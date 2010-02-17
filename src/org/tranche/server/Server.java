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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import org.tranche.ConfigureTranche;
import org.tranche.security.Signature;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.ServerModeFlag;
import org.tranche.exceptions.RejectedRequestException;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.flatfile.NonceMap;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.network.NetworkUtil;
import org.tranche.remote.RemoteUtil;
import org.tranche.routing.RoutingTrancheServer;
import org.tranche.security.SecurityUtil;
import org.tranche.server.logs.LogSubmitter;
import org.tranche.util.IOUtil;
import org.tranche.servers.ServerUtil;
import org.tranche.util.DebugUtil;
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
    private static BufferedWriter redirectOutputWriter = null;
    private static File redirectOutputFile = null;
    private final TrancheServer ts;
    private final Map<String, ServerItem> items = new HashMap<String, ServerItem>();
    private final Set<ServerWorkerThread> workers = new HashSet<ServerWorkerThread>();
    private ServerSocket socket;
    private LogSubmitter submitter;
    private boolean ssl, stopped = false, isShuttingDown = false;
    private long maxRequestSize = DEFAULT_MAX_REQUEST_SIZE;
    private int port, rejectedClients = 0;
    private ServerStartupThread serverStartupThread;

    /**
     * <p>Creates a new server that uses the Tranche protocol using a socket bound to the specified port.</p>
     * @param dfs The underlying TrancheServer to use.
     * @param port The port that the server's socket should be bound to.
     * @throws java.lang.Exception All exceptions are thrown.
     */
    public Server(TrancheServer dfs, int port) throws Exception {
        // set the variables of inters
        this.ts = dfs;
        this.port = port;
        this.ssl = ConfigureTranche.getBoolean(ConfigureTranche.PROP_SERVER_SSL);
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
        this.ts = dfs;
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

        // URL changed -- also change log submitter
        submitter = LogSubmitter.getSubmitter(getURL());

        // dump the server URL to the FFTS config -- needed by HashSpanFixingThread
        if (ts instanceof FlatFileTrancheServer) {
            // manually set the server URL information
            ((FlatFileTrancheServer) ts).getConfiguration().setValue(ConfigKeys.URL, getURL());
        } else if (ts instanceof RoutingTrancheServer) {
            // manually set the server URL information
            ((RoutingTrancheServer) ts).getConfiguration().setValue(ConfigKeys.URL, getURL());
        }

        // Start the thread that verified integrity of server data and attempts to get any
        // newly-added data
        if (!TestUtil.isTesting() || TestUtil.isTestingServerStartupThread()) {
            serverStartupThread = new ServerStartupThread(Server.this, ts);
            serverStartupThread.setDaemon(true);
            serverStartupThread.start();
        } else {
            try {
                if (ts instanceof FlatFileTrancheServer) {
                    ((FlatFileTrancheServer) ts).getConfiguration().setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.CAN_READ_WRITE));
                }
            } catch (Exception e) {
                debugErr(e);
            }
            serverStartupThread = null;
        }
    }

    /**
     * 
     * @return
     */
    public String getURL() {
        return getURL(getHostName());
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
        return ts;
    }

    /**
     * 
     * @return
     */
    public int getConnectedClients() {
        synchronized (workers) {
            return workers.size();
        }
    }

    /**
     * 
     * @return
     */
    private Collection<ServerWorkerThread> getWorkers() {
        Set<ServerWorkerThread> set = new HashSet<ServerWorkerThread>();
        synchronized (workers) {
            set.addAll(workers);
        }
        return set;
    }

    /**
     *
     * @return
     */
    public int getRejectedClients() {
        return rejectedClients;
    }

    /**
     *
     * @return
     */
    public int getMaxConcurrentClients() {
        return ConfigureTranche.getInt(ConfigureTranche.PROP_SERVER_CLIENTS_MAX);
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
        if (ts instanceof FlatFileTrancheServer) {
            return ((FlatFileTrancheServer) ts).getHost();
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
        ts.requestShutdown(sig, nonce);
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

            // close all worker threads
            for (ServerWorkerThread swt : getWorkers()) {
                try {
                    swt.getSocket().close();
                } catch (Exception e) {
                    debugErr(e);
                }
            }

            IOUtil.safeClose(socket);

            // kill all of the working threads
            for (Thread t : getWorkers()) {
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
            synchronized (workers) {
                workers.clear();
            }

            IOUtil.safeClose(redirectOutputWriter);

            // notify the network util
            NetworkUtil.clearLocalServer();

            // Close the log submitter. It must finish all pending writes.
            try {
                submitter.close();
            } catch (Exception e) {
                debugErr(e);
            }
        }
    }

    /**
     * Override of the run method. Turns the server on.
     */
    @Override()
    public void run() {
        debugOut("Starting server on port " + getPort());
        try {
            // set the local server -- also starts the updating of the network status
            NetworkUtil.setLocalServer(Server.this);
            // listen for requests
            Socket clientSocket = null;
            int successiveFailureCount = 0;
            while (!isStopped()) {
                try {
                    debugOut("Server " + IOUtil.createURL(getHostName(), getPort(), isSSL()) + "; Receiving connection request.");
                    clientSocket = socket.accept();
                } catch (IOException e) {
                    // Shutdown if excessive. Obvious network issues or server socket is dead.
                    if (++successiveFailureCount > 5) {
                        setRun(false);
                    }
                    // Prevent CPU from going nuts on successive connection attempts
                    Thread.yield();
                    continue;
                }
                successiveFailureCount = 0;

                // Make sure there is actually a socket
                if (clientSocket == null) {
                    continue;
                }

                try {
                    debugOut("Server " + IOUtil.createURL(getHostName(), getPort(), isSSL()) + "; Established connection with " + clientSocket.getLocalAddress().getHostAddress() + ".");

                    int timeout = ConfigureTranche.getInt(ConfigureTranche.PROP_SERVER_TIMEOUT);
                    if (timeout == 0) {
                        clientSocket.setSoTimeout(60000);
                    } else {
                        clientSocket.setSoTimeout(timeout);
                    }

                    // If already maximum number clients, close off w/ message
                    if (getConnectedClients() >= getMaxConcurrentClients()) {
                        throw new Exception("Maximum number of concurrent clients reached: " + getMaxConcurrentClients());
                    }

                    // spawn a thread to handle this
                    ServerWorkerThread pswt = new ServerWorkerThread(clientSocket, this);
                    // start it
                    pswt.start();
                    // add to the set of workers
                    synchronized (workers) {
                        workers.add(pswt);
                    }
                    // purge old workers
                    for (ServerWorkerThread worker : getWorkers()) {
                        if (!worker.isAlive()) {
                            synchronized (workers) {
                                workers.remove(worker);
                            }
                        }
                    }
                } catch (Exception e) {
                    debugErr(e);
                    rejectedClients++;
                    // try to send the error back to the user -- too many threads
                    OutputStream out = null;
                    try {
                        out = clientSocket.getOutputStream();
                        RemoteUtil.writeError(RejectedRequestException.MESSAGE, out);
                    } catch (Exception ex) {
                        debugErr(ex);
                    } finally {
                        IOUtil.safeClose(out);
                    }
                }
            }
        } catch (Exception e) {
            debugErr(e);
        }
        debugOut("Server is exiting.");
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
