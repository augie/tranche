/*
 *    Copyright 2005 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tranche.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import org.tranche.ConfigureTranche;
import org.tranche.security.Signature;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.ServerModeFlag;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.flatfile.NonceMap;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.network.NetworkUtil;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.routing.RoutingTrancheServer;
import org.tranche.security.SecurityUtil;
import org.tranche.server.logs.LogSubmitter;
import org.tranche.util.IOUtil;
import org.tranche.servers.ServerUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.EmailUtil;
import org.tranche.util.TestUtil;

/**
 * <p>This provides a framework for executing a server that uses the custom XML protocol designed for p2p. Each command in the protocol is broken out in to its own sub-class, similar to the strategy used by the command line client.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class Server extends Thread {

    private static boolean debug = false;
    /**
     * <p>The maximum amount of incoming data allowed buffered in memory. Protect against a DoS attack.</p>
     * <p>Right now, them maximum size is setting meta data, so based on it's maximum value plus a MB of elbow room.</p>
     */
    public static final long DEFAULT_MAX_REQUEST_SIZE = MetaData.SIZE_MAX + BigHash.HASH_LENGTH + SecurityUtil.SIGNATURE_BUFFER_SIZE + NonceMap.NONCE_BYTES + (1024 * 1024);
    /**
     * <p>Maximum number of concurrent connections allowed</p>
     */
    public static final int DEFAULT_MAX_CONCURRENT_USERS = 1000;
    /**
     * <p>Timeout information</p>
     */
    public static final int SERVER_TIMEOUT_IN_MILLISECONDS = 180 * 1000;
    /**
     * 
     */
    public static final int SERVER_TIMEOUT_TICKS = 3;
    private static BufferedWriter redirectOutputWriter = null;
    private static File redirectOutputFile = null;
    private final TrancheServer dfs;
    private final Map<String, ServerItem> items = Collections.synchronizedMap(new HashMap<String, ServerItem>());
    private final List<ServerWorkerThread> workers = Collections.synchronizedList(new ArrayList<ServerWorkerThread>());
    private ServerSocket socket;
    private LogSubmitter submitter;
    private int port;
    private boolean ssl,  stopped = false,  isShuttingDown = false;
    private int rejectedUsers = 0,  maxConcurrentUsers = DEFAULT_MAX_CONCURRENT_USERS;
    private long maxRequestSize = DEFAULT_MAX_REQUEST_SIZE;
    private ServerStartupThread serverStartupThread;

    /**
     * <p>Creates a new server that uses the Tranche protocol using a socket bound to the specified port.</p>
     * @param dfs The underlying TrancheServer to use.
     * @param port The port that the server's socket should be bound to.
     * @throws java.lang.Exception All exceptions are thrown.
     */
    public Server(TrancheServer dfs, int port) throws Exception {
        // set the variables of inters
        this.dfs = dfs;
        this.port = port;
        this.ssl = Boolean.valueOf(ConfigureTranche.PROP_SERVER_SSL);
        setName("Tranche Server: " + this.port + ", ssl: " + this.ssl);

        init();
    }

    /**
     * <p>Creates a new server that uses the Tranche protocol optionally tunneled through SSL and using a socket bound to the specified port.</p>
     * @param dfs The underlying TrancheServer to use.
     * @param port The port that the server's socket should be bound to.
     * @param ssl true if the Tranche protocol should be piped through SSL. false if not.
     * @throws java.lang.Exception All exceptions are thrown.
     */
    public Server(TrancheServer dfs, int port, boolean ssl) throws Exception {
        // set the variables of inters
        this.dfs = dfs;
        this.port = port;
        this.ssl = ssl;
        setName("Tranche Server: " + this.port + ", ssl: " + this.ssl);

        init();
    }

    /**
     * <p>Initialize variables and build supporting objects.</p>
     * @param specifiedHost
     * @throws java.lang.Exception
     */
    private void init() throws Exception {
        debugOut("Initializing server: " + getURL());
        // add the items
        addItem(new DeleteDataItem(this));
        addItem(new DeleteMetaDataItem(this));
        addItem(new GetActivityLogEntriesCountItem(this));
        addItem(new GetActivityLogEntriesItem(this));
        addItem(new GetConfigurationItem(this));
        addItem(new GetDataHashesItem(this));
        addItem(new GetDataItem(this));
        addItem(new GetMetaDataHashesItem(this));
        addItem(new GetMetaDataItem(this));
        addItem(new GetNetworkStatusItem(this));
        addItem(new GetProjectHashesItem(this));
        addItem(new HasDataItem(this));
        addItem(new HasMetaDataItem(this));
        addItem(new NonceItem(this));
        addItem(new PingItem(this));
        addItem(new RegisterServerItem(this));
        addItem(new RejectedRequestItem(this));
        addItem(new SetConfigurationItem(this));
        addItem(new SetDataItem(this));
        addItem(new SetMetaDataItem(this));
        addItem(new ShutdownItem(this));

        // if secure make an SSL socket
        if (isSSL()) {
            // open a secure connection
            SSLServerSocketFactory serverFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            // make the server socket support all known cipher suites
            SSLServerSocket serverSocket = (SSLServerSocket) serverFactory.createServerSocket(getPort());
            serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());
            // set the socket
            socket = serverSocket;
        } else {
            // if not secure, make a plain old TCP socket
            socket = ServerSocketFactory.getDefault().createServerSocket(getPort());
        }

        // dump the server URL to the FFTS config -- needed by HashSpanFixingThread
        if (dfs instanceof FlatFileTrancheServer) {
            // manually set the server URL information
            FlatFileTrancheServer ffts = (FlatFileTrancheServer) dfs;
            ffts.getConfiguration().setValue(ConfigKeys.URL, getURL());
        } else if (dfs instanceof RoutingTrancheServer) {
            // manually set the server URL information
            RoutingTrancheServer routingTrancheServer = (RoutingTrancheServer) dfs;
            routingTrancheServer.getConfiguration().setValue(ConfigKeys.URL, getURL());
        }

        // URL changed -- also change log submitter
        this.submitter = LogSubmitter.getSubmitter(getURL());

        // Start the thread that verified integrity of server data and attempts to get any
        // newly-added data
        if (!TestUtil.isTesting() || TestUtil.isTestingServerStartupThread()) {
            serverStartupThread = new ServerStartupThread(Server.this, dfs);
            serverStartupThread.setPriority(Thread.MIN_PRIORITY);
            serverStartupThread.setDaemon(true);
            serverStartupThread.start();
        } else {
            try {
                if (dfs instanceof FlatFileTrancheServer) {
                    FlatFileTrancheServer ffts = (FlatFileTrancheServer) dfs;
                    ffts.getConfiguration().setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.CAN_READ_WRITE));
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }

            serverStartupThread = null;
        }
    }

    /**
     * 
     * @return
     */
    public String getURL() {
        return getURL(this.getHostName());
    }

    /**
     * <p>Used so can use getHostName() with test without StackOverflowError.</p>
     * @param localHostName
     * @return
     */
    private String getURL(String localHostName) {
        String URL = "";
        if (ssl) {
            URL = URL + "ssl+";
        }

        // Don't call getHost() or test conditions in that method will cause a stack overflow error
        // (since uses this method to find correct test host)
        URL = URL + "tranche://" + localHostName + ":" + getPort();
        return URL;
    }

    /**
     * 
     * @return
     */
    public LogSubmitter getSubmitter() {
        return submitter;
    }

    /**
     * 
     * @param item
     */
    private void addItem(ServerItem item) {
        items.put(item.getName(), item);
    }

    /**
     *
     * @param command
     * @return
     */
    public ServerItem getItem(String command) {
        return items.get(command);
    }

    /**
     * 
     * @return
     */
    public TrancheServer getTrancheServer() {
        return dfs;
    }

    /**
     * 
     * @return
     */
    public int getConnectedUsers() {
        return this.workers.size();
    }

    /**
     *
     * @return
     */
    public int getRejectedUsers() {
        return rejectedUsers;
    }

    /**
     *
     * @return
     */
    public int getMaxConcurrentUsers() {
        return maxConcurrentUsers;
    }

    /**
     *
     * @param maxConcurrentUsers
     */
    public void setMaxConcurrentUsers(int maxConcurrentUsers) {
        this.maxConcurrentUsers = maxConcurrentUsers;
    }

    /**
     *
     * @return
     */
    public long getMaxRequestSize() {
        return maxRequestSize;
    }

    /**
     *
     * @param maxRequestSize
     */
    public void setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    /**
     *
     * @return
     */
    public boolean isSSL() {
        return ssl;
    }

    /**
     * <p>Returns the port that the Tranche server is bound to.</p>
     * @return The port that the Tranche server is bound to.
     */
    public int getPort() {
        return port;
    }

    /**
     * <p>Determine this server's host name.</p>
     * @return The IP address or host name, as found in internet interface.
     */
    public String getHostName() {

        // Check if testing and, if so, if redirected
        if (TestUtil.isTestingManualNetworkStatusTable()) {
            Map<String, String> hostToURLMapCopy = TestUtil.getServerToHostTestMap();

            // Use any known IP address as well as localhost IP
            String thisURL = getURL(ServerUtil.getHostName());
            String localHostURL = getURL("127.0.0.1");

            // See if our URL matches if in map
            for (String nextHost : hostToURLMapCopy.keySet()) {
                String nextURL = hostToURLMapCopy.get(nextHost);

                boolean equals = nextURL != null && (nextURL.equals(thisURL) || nextURL.equals(localHostURL));

                if (equals) {
                    return nextHost;
                }
            }
        }

        // Use this instead of stored host value perchance hostname/ip overwritten in Configuration attributes
        if (dfs instanceof FlatFileTrancheServer) {
            return ((FlatFileTrancheServer)dfs).getHost();
        } else {
            return ServerUtil.getHostName();
        }
    }

    /**
     * 
     * @return
     */
    public boolean isCore() {
        return NetworkUtil.isStartupServer(getHostName());
    }

    /**
     * 
     * @param swt
     */
    public void removeWorker(ServerWorkerThread swt) {
        workers.remove(swt);
    }

    /**
     *
     * @return
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * 
     * @param sig
     * @param nonce
     * @return
     * @throws java.lang.Exception
     */
    public synchronized void requestShutdown(Signature sig, byte[] nonce) throws Exception {
        if (isShuttingDown) {
            return;
        }
        dfs.requestShutdown(sig, nonce);
        IOUtil.safeClose(this);
    }

    /**
     * <p>Toggles the state of this server. false turns the server off. true turns the server on.</p>
     * @param run false if the server should be taken off-line. true if it should be kept on-line.
     */
    public void setRun(boolean run) {
        // if it is false, nuke the server
        if (!run) {
            // Flag socket-acception loop to stop
            stopped = true;
            try {
                // synchronize on a permanent object
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                debugErr(e);
            }

            // close all worker threads
            for (ServerWorkerThread swt : workers) {
                try {
                    swt.getSocket().close();
                } catch (IOException ex) {
                }
            }

            // Close the log submitter. It must finish all pending writes.
            submitter.close();

            try {

                // ---------------------------------------------------------------------------------
                // July 14 2009:
                //   If close the Server object, should not close underlying servers! This server
                //   only wraps the underlying server, which is otherwise independent.
                //
                //   This is important because Server might be restarted (e.g., when changing port)
                // ---------------------------------------------------------------------------------

//                // safe close the tranche server
//                IOUtil.safeClose(dfs);

                // kill all of the working threads
                for (Thread t : workers) {
                    try {
                        // nicely wait a second
                        t.join(1000);
                        // forcefully interrupt it
                        if (t.isAlive()) {
                            t.interrupt();
                        }
                    } catch (Exception e) {
                    }
                }
                workers.clear();
            } catch (Exception e) {
            }

            // If output file, close
            if (redirectOutputWriter != null) {
                IOUtil.safeClose(redirectOutputWriter);
            }

            // notify the network util
            NetworkUtil.clearLocalServer();
        }
    }

    /**
     * Override of the run method. Turns the server on.
     */
    @Override()
    public void run() {
        debugOut("Starting server on port :" + getPort() + "; maximum of " + getMaxConcurrentUsers() + " concurrent users; timeout tick in millis: " + String.valueOf(SERVER_TIMEOUT_IN_MILLISECONDS / SERVER_TIMEOUT_TICKS));
        try {
            Socket clientSocket = null;
            // accept connections
            int successiveFailureCount = 0;
            // set the local server -- also starts the updating of the network status
            NetworkUtil.setLocalServer(Server.this);
            while (!isStopped()) {
                try {
                    debugOut("Server " + IOUtil.createURL(getHostName(), getPort(), isSSL()) + "; Accepting requests");
                    clientSocket = this.socket.accept();
                    debugOut("Server " + IOUtil.createURL(getHostName(), getPort(), isSSL()) + "; Request accepted");
                } catch (IOException ex) {
                    successiveFailureCount++;

                    // Shutdown if excessive. Obvious network issues or server socket is dead.
                    if (successiveFailureCount > 50) {
                        // Close the server socket
                        this.socket.close();

                        // send the email
                        EmailUtil.safeSendEmail("Tranche: Server " + getURL() + " Auto-Shutdown", ConfigureTranche.getAdminEmailAccounts(), "Server at " + IOUtil.createURL(dfs) + " failed trying to establish " + successiveFailureCount + " consecutive socket connections and shut itself down. This will happen if the server socket shuts down unexpectedly or there are horrendous network issues.");

                        // Exit gracefully
                        stopped = true;
                    }
                    // Problems connecting, wait for next connection
                    // Prevent CPU from going nuts on successive connection attempts
                    Thread.yield();
                    continue;
                }

                // Make sure there is actually a socket
                if (clientSocket == null) {
                    continue;
                }
                // Reset the count
                successiveFailureCount = 0;

                // final ref
                final Socket s = clientSocket;

                // set the timeout
                s.setSoTimeout(SERVER_TIMEOUT_IN_MILLISECONDS / SERVER_TIMEOUT_TICKS);

                // If already maximum number users, close off w/ message
                if (this.getConnectedUsers() > getMaxConcurrentUsers()) {
                    s.getOutputStream().write(Token.REJECTED_CONNECTION);
                    s.getOutputStream().flush();
                    s.close();
                    rejectedUsers++;
                    continue;
                }

                // try spawning a new worker thread
                try {
                    // spawn a thread to handle this
                    ServerWorkerThread pswt = new ServerWorkerThread(s, this);
                    // add to the set of workers
                    workers.add(pswt);
                    // start it
                    pswt.start();
                    // purge old workers
                    for (int i = 0; i < workers.size(); i++) {
                        ServerWorkerThread worker = workers.get(i);
                        if (!worker.isAlive()) {
                            workers.remove(worker);
                        }
                    }
                } catch (Exception e) {
                    debugErr(e);
                    // try to send the error back to the user -- too many threads
                    try {
                        OutputStream out = s.getOutputStream();
                        RemoteUtil.writeError("Can't connect. Too many users.", out);
                        IOUtil.safeClose(out);
                    } catch (Exception ignore) {
                    }
                }
            }
        } catch (Exception e) {
            debugErr(e);
        } finally {
            debugOut("*** Server instance listening on port " + getPort() + " is exiting. ***");
            debugOut("A total of " + getRejectedUsers() + " connections were rejected by process.");
        }
    }

    /**
     * <p>Ensures that the socket is closed.</p>
     * @throws java.lang.Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        setRun(false);
    }

    /**
     * <p>Returns the ServerStartupThread object used by Server.</p>
     * @return Null if not set (if TestUtil.isTesting() true and not testing ServerStartupThread); otherwise, returns the ServerStartupThread
     */
    public ServerStartupThread getServerStartupThread() {
        return serverStartupThread;
    }

    /**
     * <p>Wait for startup thread to finish. If not running, returns immediately.</p>
     * @param maxTimeToWaitForStartup
     */
    public void waitForStartup(long maxTimeToWaitForStartup) {
        if (serverStartupThread != null) {
            serverStartupThread.waitForStartupToComplete(maxTimeToWaitForStartup);
        }
    }

    /**
     *
     * @return
     */
    public static File getRedirectOutputFile() {
        return redirectOutputFile;
    }

    /**
     *
     * @param redirectOutputFile
     */
    public static void setRedirectOutputFile(File redirectOutputFile) {
        Server.redirectOutputFile = redirectOutputFile;
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static void setDebug(boolean debug) {
        Server.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    protected static void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(Server.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    protected static void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}