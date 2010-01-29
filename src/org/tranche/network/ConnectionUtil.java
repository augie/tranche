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
package org.tranche.network;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.tranche.ConfigureTranche;
import org.tranche.TrancheServer;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.span.HashSpan;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.routing.RoutingTrancheServer;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;

/**
 * <p>Manages the Tranche server connections for the local JVM.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ConnectionUtil {

    private static boolean debug = false;
    private static final ConnectionsKeepAliveThread keepAliveThread = new ConnectionsKeepAliveThread();
    private static final Map<String, Connection> connectionMap = new HashMap<String, Connection>();
    private static final Set<ConnectionListener> listeners = new HashSet<ConnectionListener>();
    private static Thread lazyLoad = new Thread("Lazy Load Connection Util") {

        @Override
        public void run() {
            keepAliveThread.start();
            NetworkUtil.waitForStartup();
            // listen to the modifications in the master status table
            NetworkUtil.getStatus().addListener(new StatusTableListener() {

                public void rowsAdded(StatusTableEvent ste) {
                    adjustConnections();
                }

                public void rowsUpdated(StatusTableEvent ste) {
                    boolean adjustConnections = false;
                    for (String host : ste.getHosts()) {
                        // if this change affects the connectivity
                        if (ste.affectedConnectivity(host)) {
                            StatusTableRow str = NetworkUtil.getStatus().getRow(host);
                            if (isConnected(host) && str.isOnline()) {
                                adjustConnection(str.getHost(), str.getPort(), str.isSSL());
                            } else {
                                adjustConnections = true;
                            }
                        }
                        // adjust connections if hash spans changed
                        if (ste.affectedHashSpans(host)) {
                            adjustConnections = true;
                        }
                    }
                    // when a server is offline, we need a recalculation of servers to which we connect
                    if (adjustConnections) {
                        adjustConnections();
                    }
                }

                public void rowsRemoved(StatusTableEvent ste) {
                    adjustConnections();
                }
            });
        }
    };


    static {
        lazyLoad.setDaemon(true);
        lazyLoad.start();
    }

    /**
     * <p>This class cannot be instantiated.</p>
     */
    private ConnectionUtil() {
    }

    /**
     * <p>Cleans up a string.</p>
     * @param string A string
     * @return The cleaned up string
     */
    private static String normalize(String string) {
        return string.trim().toLowerCase();
    }

    /**
     * <p>Returns whether there is a connection open to the server with the given host name.</p>
     * @param host A host name
     * @return Whether there is a connection open to the server with the given host name
     */
    public static boolean isConnected(String host) {
        Connection connection = null;
        synchronized (connectionMap) {
            connection = connectionMap.get(normalize(host));
        }
        if (connection == null) {
            return false;
        }
        RemoteTrancheServer rts = connection.getRemoteTrancheServer();
        return rts != null && !rts.isClosed();
    }

    /**
     * 
     * @param host
     * @return
     */
    public static boolean isLocked(String host) {
        return getConnection(host).isLocked();
    }

    /**
     * <p>Gets the number of open connections.</p>
     * @return The number of open connections
     */
    public static int size() {
        synchronized (connectionMap) {
            return connectionMap.size();
        }
    }

    /**
     * <p>Returns the host names for the servers with which there is an open connection.</p>
     * @return A collection of host names for the servers with which there is an open connection
     */
    public static Collection<String> getConnectedHosts() {
        List<String> list = new LinkedList<String>();
        synchronized (connectionMap) {
            list.addAll(connectionMap.keySet());
        }
        return Collections.unmodifiableCollection(list);
    }

    /**
     * <p>Returns the rows for the servers with which there is an open connection.</p>
     * @return A collection of rows for the servers with which there is an open connection.
     */
    public static Collection<StatusTableRow> getConnectedRows() {
        Set<StatusTableRow> rows = new HashSet<StatusTableRow>();
        for (String host : getConnectedHosts()) {
            rows.add(NetworkUtil.getStatus().getRow(host));
        }
        return Collections.unmodifiableCollection(rows);
    }

    /**
     * <p>Returns the URLs for the servers with which there is an open connection.</p>
     * @return A collection of URLs for the servers with which there is an open connection.
     */
    public static Collection<String> getConnectedURLs() {
        Set<String> URLs = new HashSet<String>();
        for (String host : getConnectedHosts()) {
            URLs.add(NetworkUtil.getStatus().getRow(host).getURL());
        }
        return Collections.unmodifiableCollection(URLs);
    }

    /**
     * <p>Gets the connection to a Tranche server.</p>
     * <p>Returns NULL if there is no connection with the Tranche server.</p>
     * @param row A row representing a Tranche server.
     * @return A connection to a Tranche server
     */
    public static TrancheServer get(StatusTableRow row) {
        return getHost(row.getHost());
    }

    /**
     * <p>Gets the connection to the Tranche server with the given host name.</p>
     * <p>Returns NULL if there is no connection with the Tranche server.</p>
     * @param host A host name
     * @return A connection to a Tranche server. NULL if there is no connection with the Tranche server
     */
    public static TrancheServer getHost(String host) {
        host = normalize(host);
        // get
        if (isConnected(host)) {
            return getConnection(host).getTrancheServer();
        }
        return null;
    }

    /**
     * <p>Forces a connection on the Tranche server represented by the given row.</p>
     * @param row A row representing a Tranche server
     * @param locked
     * @return A connection to a Tranche server
     * @throws java.lang.Exception
     */
    public static TrancheServer connect(StatusTableRow row, boolean locked) throws Exception {
        return connectURL(row.getURL(), locked);
    }

    /**
     * <p>Forces a connection on the Tranche server with the given host name.</p>
     * @param host A host name
     * @param locked
     * @return A connection to a Tranche server
     * @throws java.lang.Exception
     */
    public static TrancheServer connectHost(String host, boolean locked) throws Exception {
        return connectURL(NetworkUtil.getStatus().getRow(host).getURL(), locked);
    }

    /**
     *
     * @param host
     * @param port
     * @param secure
     * @param locked
     * @return
     * @throws java.lang.Exception
     */
    public static TrancheServer connect(String host, int port, boolean secure, boolean locked) throws Exception {
        return connectURL(IOUtil.createURL(host, port, secure), locked);
    }

    /**
     * <p>Forces a connection on the Tranche server represented by the given URL.</p>
     * @param url A Tranche server URL
     * @param locked
     * @return A connection to a Tranche server
     * @throws java.lang.Exception
     */
    public static TrancheServer connectURL(String url, boolean locked) throws Exception {
        if (url == null) {
            throw new IOException("Can't pass a null URL.");
        }

        String originalHost = IOUtil.parseHost(url);
        // Is there a test mapping for this host? If so, connect to that url
        if (TestUtil.isTestingManualNetworkStatusTable() && TestUtil.getServerTestURL(originalHost) != null) {
            url = TestUtil.getServerTestURL(originalHost);
        }
        String host = IOUtil.parseHost(url);
        int port = IOUtil.parsePort(url);
        if (port == 0) {
            return null;
        }
        boolean secure = IOUtil.parseSecure(url);

        TrancheServer ts = null;
        // check previous connection params
        if (isConnected(originalHost)) {
            RemoteTrancheServer rts = getConnection(originalHost).getRemoteTrancheServer();
            // check the connection params
            if (!rts.isClosed() && rts.getPort() == port && rts.isSecure() == secure) {
                ts = rts;
                if (locked) {
                    getConnection(originalHost).lock();
                }
            }
        }
        if (ts == null) {
            // create a new connection
            debugOut("Creating connection to " + url);
            ts = new RemoteTrancheServer(host, port, secure);

            if (!TestUtil.isTesting()) {
                // ping to be sure there is a connection
                final TrancheServer verifyTS = ts;
                final Exception[] exception = {new TimeoutException()};
                Thread t = new Thread("Verify connection with " + host) {

                    @Override
                    public void run() {
                        for (int i = 0; i < 10; i++) {
                            try {
                                verifyTS.ping();
                                exception[0] = null;
                                break;
                            } catch (Exception e) {
                                exception[0] = e;
                            }
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
                int millisToWait = ConfigureTranche.getInt(ConfigureTranche.PROP_SERVER_TIMEOUT);
                if (millisToWait > 0) {
                    debugOut("Waiting " + millisToWait + " milliseconds for verification of connection with " + host);
                    t.join(millisToWait);
                    if (t.isAlive()) {
                        try {
                            t.interrupt();
                        } catch (Exception e) {
                            debugErr(e);
                        }
                    }
                } else {
                    debugOut("Waiting indefinitely milliseconds for verification of connection with " + host);
                    t.join();
                }
                if (exception.length > 0 && exception[0] != null) {
                    debugErr(exception[0]);
                    throw exception[0];
                }
            }

            debugOut("Connection made with " + url);

            // it's OK to kill the old connection now
            int lockCount = 0;
            TrancheServer oldTrancheServer = null;
            if (isConnected(originalHost)) {
                oldTrancheServer = getConnection(originalHost).getTrancheServer();
                lockCount = getConnection(originalHost).getLockCount();
            }
            debugOut("Caching the new connection with " + url);

            // cache the new connection
            synchronized (connectionMap) {
                connectionMap.put(originalHost, new Connection(ts, locked));
            }
            if (lockCount > 0) {
                for (int i = 0; i < lockCount; i++) {
                    getConnection(originalHost).lock();
                }
            }
            debugOut("Firing event for a connection made with " + url);

            // killing the old tranche server
            if (oldTrancheServer != null) {
                debugOut("Killing the old connection with " + originalHost);
                IOUtil.safeClose(oldTrancheServer);
            }

            // fire the event
            fireConnectionMade(host, IOUtil.parsePort(url), IOUtil.parseSecure(url));
        }
        return ts;
    }

    /**
     * <p>Interprets an exception as it occurred during communication with a Tranche server.</p>
     * <p>Take care to ensure that only exceptions having to do with the communication with the Tranche server are being reported.</p>
     * @param row A row
     * @param e An exception
     */
    public static void reportException(StatusTableRow row, Exception e) {
        try {
            reportExceptionHost(row.getHost(), e);
        } catch (Exception ex) {
            debugErr(ex);
        }
    }

    /**
     * <p>Interprets an exception as it occurred during communication with a Tranche server.</p>
     * <p>Take care to ensure that only exceptions having to do with the communication with the Tranche server are being reported.</p>
     * @param URL A Tranche URL
     * @param e An exception
     */
    public static void reportExceptionURL(String URL, Exception e) {
        try {
            reportExceptionHost(IOUtil.parseHost(URL), e);
        } catch (Exception ex) {
            debugErr(ex);
        }
    }

    /**
     * <p>Interprets an exception as it occurred during communication with a Tranche server.</p>
     * <p>Take care to ensure that only exceptions having to do with the communication with the Tranche server are being reported.</p>
     * @param host A host name
     * @param e An exception
     */
    public static void reportExceptionHost(String host, Exception e) {
        try {
            host = normalize(host);

            // make sure the exception is reported before continuing
            if (isConnected(host) && !getConnection(host).reportException(e)) {
                return;
            }
            debugErr(e);
            // connection exceptions mean the server is absolutely offlikne
            if (e instanceof ConnectException || e instanceof NoRouteToHostException) {
                flagOffline(host);
            } // timeout exceptions mean that the server is no longer responding for some reason (transmission error, offline, etc)
            else if (e instanceof TimeoutException) {
                if (isConnected(host)) {
                    // three timeouts in a row = offline
                    if (getConnection(host).getExceptionCount() >= 3) {
                        for (int i = getConnection(host).getExceptionCount() - 3; i < getConnection(host).getExceptionCount(); i++) {
                            if (!(getConnection(host).getException(i) instanceof TimeoutException)) {
                                break;
                            }
                            if (i == getConnection(host).getExceptionCount() - 1) {
                                flagOffline(host);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        }
    }

    /**
     * <p>Must FIRST safely force the closure of the connection.</p>
     * <p>Sets a server as offline in the network status.</p>
     * @param host A host name
     */
    public static void flagOffline(String host) {
        safeForceClose(host, "Flagged offline");
        // flag the server offline
        StatusTableRow row = NetworkUtil.getStatus().getRow(host);
        StatusTableRow newRow;
        if (row != null) {
            newRow = row.clone();
        } else {
            newRow = new StatusTableRow(host);
            newRow.setName(host);
            newRow.setGroup(StatusTableRow.DEFAULT_GROUP);
        }
        newRow.setIsOnline(false);
        NetworkUtil.updateRow(newRow);
    }

    /**
     * 
     * @param host
     */
    public static void lockConnection(String host) {
        if (isConnected(host)) {
            getConnection(host).lock();
        }
    }

    /**
     *
     * @param host
     */
    public static void unlockConnection(String host) {
        if (isConnected(host)) {
            getConnection(host).unlock();
        }
    }

    /**
     * 
     * @param host
     * @return
     */
    public static Connection getConnection(String host) {
        synchronized (connectionMap) {
            return connectionMap.get(host);
        }
    }

    /**
     * <p>Reports that a process is done using a Tranche server connection.</p>
     * @param row A status row
     */
    public static void safeClose(StatusTableRow row) {
        safeCloseHost(row.getHost());
    }

    /**
     * <p>Reports that a process is done using a Tranche server connection.</p>
     * @param url A Tranche URL
     */
    public static void safeCloseURL(String url) {
        try {
            safeCloseHost(IOUtil.parseHost(url));
        } catch (Exception e) {
            debugErr(e);
        }
    }

    /**
     * <p>Reports that a process is done using a Tranche server connection.</p>
     * @param host A host name
     */
    public static void safeCloseHost(String host) {
        if (isConnected(host) && !isLocked(host)) {
            IOUtil.safeClose(getConnection(host).getTrancheServer());
        }
    }

    /**
     * <p>Closes the connection immediately.</p>
     * @param host A host name
     * @param reason An arbitrary message explaining why called. Useful for troubleshooting.
     */
    public static void safeForceClose(String host, String reason) {
        // maybe not?
        if (!isConnected(host)) {
            return;
        }

        debugOut("Forcing closed " + host + ", reason: " + reason);

        // kill the server
        IOUtil.safeClose(getConnection(host).getTrancheServer());

        // remove
        synchronized (connectionMap) {
            connectionMap.remove(host);
        }

        // notify
        fireConnectionDropped(host);
    }

    /**
     * 
     * @param reason
     */
    public static void safeForceCloseAll(String reason) {
        for (String host : getConnectedHosts()) {
            safeForceClose(host, reason);
        }
    }

    /**
     * <p>Updates the connection to use the given set of connection parameters.</p>
     * <p>When some connection parameters change, need to kill the old connection and open a new one.</p>
     * @param host A host name
     * @param port A port number
     * @param ssl Whether the server communicates over SSL
     */
    protected static void adjustConnection(String host, int port, boolean ssl) {
        host = normalize(host);
        // not conected? stop
        if (!isConnected(host)) {
            return;
        }
        debugOut("Adjusting the connection for " + host + ".");
        // open the new connection
        try {
            connect(host, port, ssl, isLocked(host));
        } catch (Exception e) {
            reportExceptionHost(host, e);
        }
    }

    /**
     * <p>Establishes the connections for the client and the server to work with.</p>
     * <p>For the client, this means connecting to a full hash span with read/write priveleges for all hashes.</p>
     * <p>For a data server, this means connecting to the servers from which to perform updates to the network status table, the servers with overlapping hash spans, and also a full hash span of servers (for maintenance of sticky chunks.)</p>
     * <p>For a routing server, this means connecting to the servers from which to perform update to the network status table and the servers being routed to.</p>
     * <p>If connections have already been made, will try to make as few changes as possible to that pool of existing connections.</p>
     */
    public synchronized static void adjustConnections() {
        debugOut("Adjusting connections");
        try {
            // track the connections determined necessary
            Set<StatusTableRow> requiredConnections = new HashSet<StatusTableRow>();
            // the local instance is just a client
            if (NetworkUtil.getLocalServer() == null) {

                debugOut("Getting a full hash span.");
                // repository must be x servers in size before connecting only to a full hash span
                int threshold = ConfigureTranche.getInt(ConfigureTranche.PROP_CONNECTION_FULL_HASH_SPAN_THRESHOLD);
                Set<StatusTableRow> onlineRows = new HashSet<StatusTableRow>();
                for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                    if (!row.isOnline()) {
                        continue;
                    }
                    onlineRows.add(row);
                }
                if (onlineRows.size() > threshold) {
                    // calculate a full hash span -- seeding with the currently connected rows
                    requiredConnections.addAll(StatusTableRow.calculateFullHashSpan(getConnectedRows(), NetworkUtil.getStatus().getRows()));
                } else {
                    requiredConnections.addAll(onlineRows);
                }
                // need to keep the update connection
                if (requiredConnections.isEmpty()) {
                    // use an existing connection
                    if (size() != 0) {
                        requiredConnections.add(NetworkUtil.getStatus().getRow(getConnectedHosts().toArray(new String[0])[0]));
                    } else {
                        // need to make a connection with a random row
                        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                            if (!row.isOnline() && row.isCore()) {
                                continue;
                            }
                            requiredConnections.add(row);
                            break;
                        }
                    }
                }
            } else {
                // start with the servers from which to get network status updates
                debugOut("Determining status table row ranges.");
                // have the server status update process modify its own ranges based on the network
                ServerStatusUpdateProcess.adjustStatusTableRowRanges();
                // connect to all the servers to which we are supposed to be connected to for updates
                for (StatusTableRowRange range : ServerStatusUpdateProcess.getStatusTableRowRanges()) {
                    requiredConnections.add(NetworkUtil.getStatus().getRow(range.getConnectionHost()));
                }
                // connect to all the non-core servers we are supposed to be connected to for individual updates
                for (StatusTableRowRange range : ServerStatusUpdateProcess.getNonCoreServersToUpdate()) {
                    requiredConnections.add(NetworkUtil.getStatus().getRow(range.getConnectionHost()));
                }

                if (NetworkUtil.getLocalServer().getTrancheServer() instanceof FlatFileTrancheServer) {
                    debugOut("Local server is a FlatFileTrancheServer");

                    // always connect with the local server
                    debugOut("Connecting to the local data server.");
                    requiredConnections.add(NetworkUtil.getLocalServerRow());

                    // next get all servers with overlapping hash spans
                    debugOut("Getting all servers with overlapping hash spans");
                    Map<Collection<HashSpan>, String> overlappingHashSpanHosts = new HashMap<Collection<HashSpan>, String>();
                    debugOut("Starting to check servers with overlapping hash spans.");
                    for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                        debugOut("Starting to check for overlapping hashspans with " + row.getURL());
                        // do not connect to self, offline servers, non-data servers, and non-core servers for overlapping hash spans
                        if (row.isLocalServer() || !row.isOnline() || !row.isCore() || !row.isDataStore()) {
                            continue;
                        }
                        debugOut("Checking for overlapping hashspans with " + row.getURL());
                        // determine if the servers have overlapping hashspans
                        rowLoop:
                        for (HashSpan hs1 : row.getHashSpans()) {
                            for (HashSpan hs2 : NetworkUtil.getLocalServerRow().getHashSpans()) {
                                if (hs1.overlaps(hs2)) {
                                    overlappingHashSpanHosts.put(row.getHashSpans(), row.getHost());
                                    requiredConnections.add(row);
                                    break rowLoop;
                                }
                            }
                        }
                    }

                    // tell the flat file tranche server to work with the given servers for updating
                    ((FlatFileTrancheServer) NetworkUtil.getLocalServer().getTrancheServer()).setHostsToUseWithOverlappingHashSpans(overlappingHashSpanHosts);
                    // working with sticky chunks means that data servers must connect with a full hash span
                    Collection<StatusTableRow> fullHashSpanSeedRows = new HashSet<StatusTableRow>(requiredConnections);
                    // also seed with the current connections
                    fullHashSpanSeedRows.addAll(getConnectedRows());
                    // calculate a full hash span
                    requiredConnections.addAll(StatusTableRow.calculateFullHashSpan(fullHashSpanSeedRows, NetworkUtil.getStatus().getRows()));
                } else if (NetworkUtil.getLocalServer().getTrancheServer() instanceof RoutingTrancheServer) {
                    debugOut("Local server is a RoutingTrancheServer");
                    // connect to all servers being routed to
                    Collection<String> hosts = ((RoutingTrancheServer) NetworkUtil.getLocalServer().getTrancheServer()).getManagedServers();
                    for (String host : hosts) {
                        requiredConnections.add(NetworkUtil.getStatus().getRow(host));
                    }
                }
            }

            // connect if not already connected - connect 5 at a time
            Set<Thread> threads = new HashSet<Thread>();
            StatusTableRow[] requiredConnectionsArray = requiredConnections.toArray(new StatusTableRow[0]);
            for (int i = 0; i < requiredConnections.size(); i++) {
                final StatusTableRow row = requiredConnectionsArray[i];
                // connect if not already connected -- reporting exceptions may readjust connections again if there is a problem
                if (!isConnected(row.getHost())) {
                    Thread t = new Thread("Connecting to " + row.getHost()) {

                        @Override
                        public void run() {
                            try {
                                connect(row, false);
                            } catch (Exception e) {
                                debugErr(e);
                                reportException(row, e);
                            }
                        }
                    };
                    t.setDaemon(true);
                    threads.add(t);
                }
                if (i + 1 == requiredConnections.size() || (i + 1) % 5 == 0) {
                    for (Thread t : threads) {
                        t.start();
                    }
                    for (Thread t : threads) {
                        t.join();
                    }
                    threads.clear();
                }
            }

            debugOut("Killing unnecessary connections");
            // kill the unneeded connections
            Set<String> toKill = new HashSet<String>();
            for (String host : getConnectedHosts()) {
                boolean found = false;
                for (StatusTableRow row : requiredConnections) {
                    if (row.getHost().equals(host)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    toKill.add(host);
                }
            }
            for (String host : toKill) {
                safeCloseHost(host);
            }
        } catch (Exception e) {
            debugErr(e);
        }
    }

    /**
     * <p>Adds a connection listener.</p>
     * @param l A connection listener
     */
    public static void addListener(ConnectionListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    /**
     * <p>Removes the given connection listener.</p>
     * @param l A connection listener
     */
    public static void removeListener(ConnectionListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    /**
     *
     * @return
     */
    public static Collection<ConnectionListener> getListeners() {
        List<ConnectionListener> list = new LinkedList<ConnectionListener>();
        synchronized (listeners) {
            list.addAll(listeners);
        }
        return Collections.unmodifiableCollection(listeners);
    }

    /**
     * <p>Notifies listeners that a connection has been made to a Tranche server with the given connection parameters.</p>
     * @param host A host name
     */
    private static void fireConnectionMade(String host, int port, boolean ssl) {
        ConnectionEvent event = new ConnectionEvent(host, port, ssl);
        for (ConnectionListener listener : getListeners()) {
            try {
                listener.connectionMade(event);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Notifies listeners that a connection has been dropped.</p>
     * @param host A host name
     */
    private static void fireConnectionDropped(String host) {
        ConnectionEvent event = new ConnectionEvent(host);
        for (ConnectionListener listener : getListeners()) {
            try {
                listener.connectionDropped(event);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        ConnectionUtil.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static final boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static final void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(ConnectionUtil.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
