/*
 * StressClient.java
 *
 * Created on October 6, 2007, 8:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package stress.client;

import org.tranche.logs.SimpleLog;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;
import stress.StressTestConfig;
import stress.server.RemoteStressServer;
import stress.util.Logger;
import stress.util.StressTest;
import stress.util.StressTestSuite;
import stress.util.StressTestUtil;

/**
 * Entry point for client-side of stress test.
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 * @author Adam Giacobbe <agiac@umich.edu>
 */
public class StressClient {

    private static final boolean IS_PRINT_NETWORK_UTIL_TRACERS = false;    // Yuck, a hack to determine which JVM is running =(
    public static boolean isStressClientRunning = false;
    private static RemoteStressServer stressServer = null;

    /**
     * Entry point for the client.
     */
    public static void main(String[] args) throws Exception {

        NetworkUtil.setDebug(IS_PRINT_NETWORK_UTIL_TRACERS);
        DebugUtil.setDebug(IS_PRINT_NETWORK_UTIL_TRACERS);

        isStressClientRunning = true;
        StressTestConfig.load();
        
        System.out.println("Stress test user:");
        System.out.println("    - Valid: "+StressTestUtil.getUserZipFile().isValid());
        System.out.println("        * Not valid before: "+StressTestUtil.getCertificate().getNotBefore());
        System.out.println("    - Expired: "+StressTestUtil.getUserZipFile().isExpired());
        System.out.println("        * Not valid after: "+StressTestUtil.getCertificate().getNotAfter());
        
        TestUtil.setTestingManualNetworkStatusTable(false);

        final String url = "tranche://" + Configuration.getServerIP() + ":" + Configuration.getTranchePort();

        try {

            if (Configuration.getResultsFile().exists()) {
                System.err.println("The output file already exists: " + Configuration.getResultsFile().getAbsolutePath());
                System.err.println("Please move the file, then restart...");
                System.exit(1);
            }

            // Prime the config reader, make sure everything okay
            StressTestSuite.hasNext();

            // Connect to the server
            if (stressServer == null) {
                stressServer = new RemoteStressServer(Configuration.getServerIP());
            }

            // Flag as multi-socket test so socket not closed
            RemoteTrancheServer.setMultiSocketTest(true);

            // We're testing, so allow things like server tracker to die
//            TestUtil.setTesting(true);

            while (StressTestSuite.hasNext()) {

                StressTest test = StressTestSuite.getNext();

                // Note that the server must know what to do. Tell it now!
                if (test.isUseDataBlockCache()) {
                    StressClient.getStressServer().turnOnDataBlockCache();
                } else {
                    StressClient.getStressServer().turnOffDataBlockCache();
                }

                // Start test
                getStressServer().startTest();

                // Wait now so doesn't impact test performance
                Thread waitThread = new Thread("Limited wait thread") {

                    @Override
                    public void run() {
                        NetworkUtil.waitForStartup();
                    }
                };
                waitThread.setDaemon(true);
                waitThread.setPriority(Thread.MIN_PRIORITY);

                waitThread.start();

                // Ten seconds, then move on. Script blocked before for a week!
                waitThread.join(1000 * 60);

                System.out.println("Finished waiting for startup... (server IP: " + Configuration.getServerIP() + ")");

                System.out.println("Number of rows in status table: " + NetworkUtil.getStatus().size());
                for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                    System.out.println("    - " + row.getURL()+" (online: "+row.isOnline()+")");
                }

                System.out.println();
                System.out.println("Number of startup URLs: " + NetworkUtil.getStartupServerURLs().size());
                for (String startupURL : NetworkUtil.getStartupServerURLs()) {
                    System.out.println("    - " + startupURL);
                }

                boolean isOnline = NetworkUtil.getStatus().getRow(Configuration.getServerIP()).isOnline();

//                boolean isOnline = false;
//                try {
//                    isOnline = NetworkUtil.getStatus().getRow(Configuration.getServerIP()).isOnline();
//                } catch (NullPointerException npe) {
//                    // nope
//                }
                System.out.println("Server \"" + url + "\": " + isOnline);

                Logger.startTest(test);

                // May flag offline, flag online
//                ServerUtil.setServerOnlineStatus(url, true);

                System.out.println("Starting test with " + test.getClientCount() + " clients at " + Text.getFormattedDate(System.currentTimeMillis()) + ".");

                System.out.println(test.toString());

//                ThreadGroup clients = new ThreadGroup("Stress clients thread group");

                // Sleep a bit, make sure server is ready...
                System.out.println("  Sleep a bit to make sure server is ready...");
                Thread.sleep(1000 * 30);

                Thread[] clientThreads = new Thread[test.getClientCount()];

                for (int i = 0; i < test.getClientCount(); i++) {
                    clientThreads[i] = new ClientConnectionThread(
                            Configuration.getServerIP(),
                            test);
                    clientThreads[i].start();
                }

                // Join threads and log the stats
                for (Thread t : clientThreads) {
                    if (t != null && t.isAlive()) {
                        t.join();
                    }
                }

                Logger.stopTest();

                // Stop test (if not multi-client mode)
                if (!Configuration.isMultiMode()) {
                    getStressServer().stopTest();
                }

                // Flush contents of log
                SimpleLog.setEnabled(false);

                // Clear cache, which will tear down Tranche sockets
//                IOUtil.purgeAndCloseConnectionsForTest();

                // Give server chance to catch up, let client-side threads die...
                int sleep = 60 * 1000;
                System.out.println("Test completed, sleeping for " + Text.getPrettyEllapsedTimeString(sleep) + ". Started sleep at " + Text.getFormattedDate(System.currentTimeMillis()));
                Thread.sleep(sleep);
                IOUtil.recursiveDeleteWithWarning(Configuration.getTempDirectory());
            }

        } catch (Exception e) {
            System.out.println("An error has occured, client stopping: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            try {
                Logger.close();
            } catch (Exception ex) { /* Nope */ ex.printStackTrace(System.err);
            }

            // Only close if single-user mode
            if (!Configuration.isMultiMode() && getStressServer() != null) {
                try {
                    getStressServer().close();
                } catch (Exception ex) {
                    System.out.println("Cannot tear down test server: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            isStressClientRunning = false;
            System.out.println("~ Fin. So long! ~");
            System.exit(0);
        }
    }

    public static RemoteStressServer getStressServer() throws Exception {
        if (stressServer == null) {
            stressServer = new RemoteStressServer(Configuration.getServerIP());
        }
        return stressServer;
    }
}
