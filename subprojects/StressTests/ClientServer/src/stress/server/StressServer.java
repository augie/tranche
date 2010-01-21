/*
 * StressServer.java
 *
 * Created on October 5, 2007, 4:19 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package stress.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import org.tranche.configuration.Configuration;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.flatfile.logs.DataBlockUtilLogger;
import org.tranche.flatfile.logs.DiskBackedTransactionLog;
import org.tranche.hash.span.HashSpan;
import org.tranche.logs.SimpleLog;
import org.tranche.server.Server;
import org.tranche.users.User;
import org.tranche.util.IOUtil;
import org.tranche.util.PersistentFileUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;
import stress.StressTestConfig;
import stress.util.StressTestUtil;

/**
 * Starts and stops test servers at clients request, deleting all files at end of each test.
 * Communicates with RemoteStressServer using simple protocol.
 * @author Bryan Smith <bryanesmith at gmail dot com>
 * @author Adam Giacobbe <agiac@umich.edu>
 */
public class StressServer {
    // Turn on logging. Expensive.
    public static final boolean isLogging = false;
    /**
     * Only one StressServer at a time
     */
    private static boolean isServerRunning = false;
    /**
     * Flag for StressServer to shut down
     */
    private static boolean isShutDown = false;
    /**
     * Only one FlatFileTrancheServer/Server combo at a time.
     */
    private static boolean isTestRunning = false;
    /**
     *
     */
    private static DataBlockUtilLogger logger = null;
    private static FlatFileTrancheServer ffserver = null;
    private static Server server = null;
    private static ServerSocket ss = null;

    /**
     * Entry point for the server.
     */
    public static void main(String[] args) {
        try {

            StressTestConfig.load();

            // Go ahead and load so if fails, fails early
            StressTestUtil.loadEarly();

            // ---------------------------------------------------------------------
            // This pauses indefinitely. Need to determine why. Causes server
            // to be read only.
            // ---------------------------------------------------------------------
            TestUtil.setTesting(true);
            TestUtil.setTestingServerStartupThread(false);

            startServer();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }

    }
    /**
     * <p>The number of clients currently connected.</p>
     */
    private static int numberConnections = 0;
    private static final Object numberConnectionsLock = new Object();
    private static boolean isUseDataBlockCache = DataBlockUtil.DEFAULT_USE_CACHE;

    /**
     * Start the server, but only once.
     */
    private static void startServer() throws Exception {
        if (isServerRunning) {
            return;
        }
        isServerRunning = true;

        try {
            // Server socket with no timeout, tests take a while
            ss = new ServerSocket(ServerConfiguration.STRESS_SERVER_PORT);
            ss.setSoTimeout(0);

            System.out.println("-- --- --- STARTING STRESS SERVER :" + ServerConfiguration.STRESS_SERVER_PORT + " --- --- --");

            SERVER:
            while (!isShutDown) {
                Socket s = null;
                BufferedReader reader = null;
                BufferedWriter writer = null;
                try {
                    // Wait for next connection
                    s = ss.accept();
                    reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

                    // Get the request item
                    String request = reader.readLine();

                    System.out.println("SERVER> received message: " + request);

                    // Handle request
                    if (request.equals(Protocol.REQUEST_START_TEST.trim())) {
                        startTest();
                    } else if (request.equals(Protocol.REQUEST_STOP_TEST.trim())) {
                        stopTest();
                    } else if (request.equals(Protocol.REQUEST_DISCONNECT.trim())) {
                        close();
                    } else if (request.equals(Protocol.REQUEST_USE_DATA_BLOCK_CACHE.trim())) {
                        isUseDataBlockCache = true;
                        // Why is this? FFTS shouldn't be running yet, and will use
                        // isUseDataBlockCache to set this value
                        if (ffserver != null) {
                            DataBlockUtil dbu = ffserver.getDataBlockUtil();
                            if (dbu != null) {
                                dbu.setUseCache(true);
                            }
                        }
                    } else if (request.equals(Protocol.REQUEST_NO_DATA_BLOCK_CACHE.trim())) {
                        isUseDataBlockCache = false;
                        // Why is this? FFTS shouldn't be running yet, and will use
                        // isUseDataBlockCache to set this value
                        if (ffserver != null) {
                            DataBlockUtil dbu = ffserver.getDataBlockUtil();
                            if (dbu != null) {
                                dbu.setUseCache(false);
                            }
                        }
                    } else {
                        // Whoops, bad request
                        System.err.println("SERVER> Bad request: " + request);
                        writer.write(Protocol.RESPONSE_ERROR);
                        writer.flush();
                        continue SERVER;
                    }

                    writer.write(Protocol.RESPONSE_OK);
                    writer.flush();
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    ex.printStackTrace(System.err);
                    writer.write(Protocol.RESPONSE_ERROR);
                    writer.flush();
                } finally {
                    // Each socket is for one transaction only. Performance is not an issue.
                    IOUtil.safeClose(writer);
                    IOUtil.safeClose(reader);
                    if (s != null) {
                        s.close();
                    }
                }
            } // While server is not shut down
        } finally {

            // To be safe... stop test is a safe close operation
            stopTest();

            // Reset flags... subsequent uses from same instance (e.g., unit tests) should work fine
            resetFlags();

            // Stress server shutting down
            if (ss != null) {
                ss.close();
            }
            System.out.println("-- --- --- STRESS SERVER EXITING --- --- --");

        }
    } // startServer()

    /**
     * Used to reset boolean flags. Only obvious use if for subsequent tests in JUnit.
     */
    private static void resetFlags() {
        isServerRunning = false;
        isShutDown = false;
        isTestRunning = false;
    }

    /**
     * Starts a test server. If one already running, ignore.
     */
    private static synchronized void startTest() throws Exception {

        // Adjust number of current connections
        synchronized (numberConnectionsLock) {
            numberConnections++;
        }

        // Ignore other requests if running; multi-clients will call multiple times
        if (isTestRunning) {
            System.out.println("SERVER> Test already running, ignore request to start (Total number of connections: " + numberConnections + ")");
            return;
        }


        try {
            // Create a unique log file
            if (isLogging) {
                File logFile = new File(PersistentFileUtil.getPersistentDirectory(), "SWT-" + System.currentTimeMillis() + ".log");
                logFile.createNewFile();
                SimpleLog.setLog(SimpleLog.SWT_LOG, logFile);
                SimpleLog.setEnabled(true);
                System.out.println("Setting ServerWorkerThread log: " + logFile.getAbsolutePath());
            }

//            // Prepare data directory. If can't setup, throw exception
//            File dataDirectory = new File(Configuration.DATA_DIRECTORY_PATH);
//            if (!dataDirectory.exists() && !dataDirectory.mkdirs()) {
//                // One last chance
//                if (!dataDirectory.exists())
//                    throw new RuntimeException("Can't make directory: "+Configuration.DATA_DIRECTORY_PATH);
//            }

            StressTestUtil.getServerDataDirectory().mkdirs();

            ffserver = new FlatFileTrancheServer(StressTestUtil.getServerDataDirectory());

            Configuration c = ffserver.getConfiguration();

            c.addUser(StressTestUtil.getUserZipFile());

            // Prevent deletion of files entirely. Let hash span run, though: other servers do, and this is a stress test
            c.setValue("hashSpanFix: AllowDelete ", String.valueOf(false));

            c.getHashSpans().clear();
            c.getHashSpans().add(HashSpan.FULL);

            c.getTargetHashSpans().clear();
            c.getTargetHashSpans().add(HashSpan.FULL);

            ffserver.setConfiguration(c);

            // Decide whether to use cache
            ffserver.getDataBlockUtil().setUseCache(isUseDataBlockCache);

            // Create a transactional log...
            File transactionalLog = new File(PersistentFileUtil.getPersistentDirectory(), "data-block-" + System.currentTimeMillis() + ".log");
            transactionalLog.createNewFile();

            // Turn on logging and add the transactional log
            if (isLogging) {
                DataBlockUtil.setIsLogging(true);
                logger = DataBlockUtil.getLogger();
                logger.addLog(new DiskBackedTransactionLog(transactionalLog));
                System.out.println("Added transactional log at: " + transactionalLog.getAbsolutePath());
            }

            // Add users
            Iterator<User> it = ffserver.getConfiguration().getUsers().iterator();
            while (it.hasNext()) {
                User u = it.next();
                u.setFlags(User.ALL_PRIVILEGES);
            }

            server = new Server(ffserver, ServerConfiguration.TRANCHE_SERVER_PORT);

            // Create an obvious output file for server output/error
            File outputFile = new File(PersistentFileUtil.getPersistentDirectory(), "output.txt");
            outputFile.createNewFile();
            Server.setRedirectOutputFile(outputFile);
            System.out.println("Redirecting server's output to " + outputFile.getAbsolutePath());
            server.start();

            isTestRunning = true;

            System.out.println("SERVER> Starting test, tranche server listening on :" + ServerConfiguration.TRANCHE_SERVER_PORT);
        } catch (Exception e) {

            System.err.println("Error on stress test server, bailing: " + e.getMessage());
            e.printStackTrace(System.err);

            // Whoops, close the servers
            IOUtil.safeClose(ffserver);
            IOUtil.safeClose(server);

            if (isLogging) {
                closeLogger();
            }

            throw new Exception(e.getMessage());
        }
    }

    private static synchronized void stopTest() throws Exception {

        final long shutdownStart = System.currentTimeMillis();

        // Ignore if not running. Perhaps multiple clients?
        if (!isTestRunning) {
            System.out.println("SERVER> No test running, ignoring request to stop");
            return;
        }

        // Adjust number of current connections. Synchronized so recursive delete does not interfer
        // with a recently started test. (Won't reliquish lock until directory deleted.)
        synchronized (numberConnectionsLock) {
            numberConnections--;

            if (numberConnections > 0) {
                System.out.println("SERVER> Not shutting down, still " + numberConnections + " connection" + (numberConnections > 1 ? "s" : "") + ".");
                return;
            }

            System.out.println("SERVER> Stopping test.");

            IOUtil.safeClose(server);
            IOUtil.safeClose(ffserver);

//            IOUtil.purgeAndCloseConnectionsForTest();

            // Just delete the data directory, nothing else
            IOUtil.recursiveDeleteWithWarning(new File(StressTestUtil.getServerDataDirectory(), "data"));

            if (isLogging) {
                SimpleLog.setEnabled(false);
                closeLogger();
            }

            isTestRunning = false;

            System.out.println("Took " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - shutdownStart) + " to shutdown test resources...");
        }
    }

    private static void closeLogger() {
        if (logger != null) {
            try {
                logger.close();
            } catch (Exception ex) {
                System.err.println("Problem closing logger down: " + ex.getMessage());
                ex.printStackTrace();
            }
            logger = null;
        }
    }

    private static void close() throws Exception {
        isShutDown = true;
    }
}
