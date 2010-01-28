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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.ConfigureTranche;
import org.tranche.TrancheServer;
import org.tranche.routing.RoutingTrancheServer;
import org.tranche.routing.RoutingTrancheServerListener;
import org.tranche.server.GetNetworkStatusItem;
import org.tranche.server.Server;
import org.tranche.time.TimeUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.TestUtilListener;
import org.tranche.util.ThreadUtil;

/**
 * <p>Manages the representation of the Tranche network.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class NetworkUtil {

    private static boolean debug = false;
    /**
     * <p>The software comes with a list of Tranche servers to begin contact with the network.</p>
     */
    private static final Set<String> startupServerURLs = new HashSet<String>();
    /**
     * <p>Data structure for server information.</p>
     */
    private static final StatusTable masterStatusTable = new StatusTable();
    /**
     * <p>Used in the lazy-load process.</p>
     */
    private static boolean startedLoadingNetwork = false,  finishedLoadingNetwork = false;
    /**
     * <p>Need to modify behavior depending on whether there is a server running on this JVM.</p>
     */
    private static Server localServer;
    private static ServerStatusUpdateProcess serverUpdateProcess;
    private static ClientStatusUpdateProcess clientUpdateProcess;
    private static final RoutingTrancheServerListener routingTrancheServerListener = new RoutingTrancheServerListener() {

        public void dataServersAdded(String[] hosts) {
            if (!TestUtil.isTestingManualNetworkStatusTable()) {
                ConnectionUtil.adjustConnections();
            }
        }

        public void dataServersRemoved(String[] hosts) {
            if (!TestUtil.isTestingManualNetworkStatusTable()) {
                ConnectionUtil.adjustConnections();
            }
        }
    };


    static {
        // This listener allows the NetworkUtil to automatically update itself when test conditions change.
        TestUtil.addListener(new TestUtilListener() {

            /**
             * <p>Fired when a change was made to TestUtil.setTestingManualNetworkStatusTable.</p>
             */
            public void changedTestingManualNetworkStatusTable() {
                // If testing manual network status table, use a dummy table.
                if (TestUtil.isTestingManualNetworkStatusTable()) {
                    masterStatusTable.clear();
                    stopUpdateProcess();
                    startedLoadingNetwork = true;
                    finishedLoadingNetwork = true;
                } else {
                    // Return to normal. Set new table, and set lazy-load variable to false. Then call process to update.
                    masterStatusTable.clear();
                    startedLoadingNetwork = false;
                    finishedLoadingNetwork = false;
                }
            }
        });
        // Add ConnectUtil listener
        ConnectionUtil.addListener(new ConnectionAdapter() {

            @Override
            public void connectionMade(ConnectionEvent event) {

                // Don't update when testing
                if (TestUtil.isTestingManualNetworkStatusTable()) {
                    return;
                }

                debugOut("Connection made: " + event.getHost());
                // is this not in the status table?
                StatusTableRow row = getStatus().getRow(event.getHost());
                if (row == null) {
                    // add it for reference
                    row = new StatusTableRow(event.getHost());
                    // yep, online
                    row.setIsOnline(true);
                    row.setIsSSL(event.isSSL());
                    row.setPort(event.getPort());
                    updateRow(row);
                } else {
                    // update the connection parameters
                    StatusTableRow newRow = row.clone();
                    newRow.setUpdateTimestamp(TimeUtil.getTrancheTimestamp());
                    newRow.setIsOnline(true);
                    newRow.setPort(event.getPort());
                    newRow.setIsSSL(event.isSSL());
                    updateRow(newRow);
                }
            }
        });
    }

    /**
     * <p>This class cannot be instantiated.</p>
     */
    private NetworkUtil() {
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
     * <p>Clears all the startup server URL's and adds the ones from the given collection of server URL's.</p>
     * @param serverURLs A collection of Tranche server URL's
     */
    public static void setStartupServerURLs(Collection<String> serverURLs) {
        synchronized (startupServerURLs) {
            startupServerURLs.clear();
        }
        for (String serverURL : serverURLs) {
            if (serverURL == null) {
                continue;
            }
            String normalizedURL = normalize(serverURL);
            debugOut("Adding startup server URL: " + normalizedURL);
            synchronized (startupServerURLs) {
                startupServerURLs.add(normalizedURL);
            }
        }
    }

    /**
     * <p>Returns the collection of startup server URL's.</p>
     * @return A collection of startup server URL's
     */
    public static Collection<String> getStartupServerURLs() {
        List<String> list = new LinkedList<String>();
        synchronized (startupServerURLs) {
            list.addAll(startupServerURLs);
        }
        return Collections.unmodifiableCollection(list);
    }

    /**
     * <p>Returns whether the given host name is within the list of startup Tranche server URL's.</p>
     * @param host A host name
     * @return Whether the given host name is within the list of startup Tranche server URL's
     */
    public static boolean isStartupServer(String host) {
        try {
            host = normalize(host);
            for (String serverURL : getStartupServerURLs()) {
                if (IOUtil.parseHost(serverURL).equals(host)) {
                    return true;
                }
            }
        } catch (Exception e) {
            debugErr(e);
        }
        return false;
    }

    /**
     * <p>Gets at most [size] number of startup server URL's at random. Ignores all servers with URL's in [ignoreServers].</p>
     * @param size The maximum number of startup server URL's to return
     * @param ignoreServers The servers not to return
     * @return A collection of Tranche server URL's
     */
    private static Collection<String> getRandomStartupServerURLs(int size, Collection<String> ignoreServers) {
        debugOut("Getting random startup server URLs.");
        // generate a list of servers from which to choose at random
        List<String> serversToPickFrom = new ArrayList<String>();
        for (String server : getStartupServerURLs()) {
            if (!ignoreServers.contains(server)) {
                serversToPickFrom.add(server);
            }
        }
        // choose from the servers at random
        Set<String> servers = new HashSet<String>();
        for (int i = 0; i < size && !serversToPickFrom.isEmpty(); i++) {
            servers.add(serversToPickFrom.remove(RandomUtil.getInt(serversToPickFrom.size())));
        }
        debugOut("Returning random startup server URLs.");
        return Collections.unmodifiableSet(servers);
    }

    /**
     * 
     */
    public static void reload() {
        startedLoadingNetwork = false;
        finishedLoadingNetwork = false;
        lazyLoad();
    }

    /**
     * <p>Waits until the network is loaded.</p>
     */
    public static void waitForStartup() {
        lazyLoad();
        // make sure the network is finished loading
        while (!finishedLoadingNetwork) {
            ThreadUtil.safeSleep(500);
        }
        debugOut("Finished waiting for startup.");
        debugOut(getStatus().toString());
    }

    /**
     * <p>Loads the network status from one of the startup servers and calls the ConnectionUtil.adjustConnections() method.</p>
     * @throws java.lang.Exception
     */
    public synchronized static void lazyLoad() {
        if (startedLoadingNetwork || TestUtil.isTestingManualNetworkStatusTable()) {
            debugOut("Returning loadNetwork (startedLoadingNetwork = " + startedLoadingNetwork + ", TestUtil.isTestingManualNetworkStatusTable() = " + TestUtil.isTestingManualNetworkStatusTable() + ")");
            return;
        }
        startedLoadingNetwork = true;
        Thread t = new Thread("Lazy Load Network") {

            @Override
            public void run() {
                ConfigureTranche.waitForStartup();
                debugOut("Starting to load the network.");
                try {
                    // keep track of the servers tried
                    Set<String> serversTried = new HashSet<String>();
                    debugOut("# startup server URLs: " + getStartupServerURLs().size());
                    WHILE:
                    while (serversTried.size() < getStartupServerURLs().size()) {
                        // go to three random startup servers to request the network status
                        Collection<String> serversToAsk = getRandomStartupServerURLs(4, serversTried);
                        // dummy check
                        if (serversToAsk.isEmpty()) {
                            break;
                        }
                        final Boolean[] success = {false, false, false, false};
                        Set<Thread> threads = new HashSet<Thread>();
                        FOR:
                        for (int i = 0; i < serversToAsk.size(); i++) {
                            // get the URL from which to request
                            final String serverURL = serversToAsk.toArray(new String[0])[i];
                            final int j = i;
                            // we tried this server
                            serversTried.add(serverURL);
                            // do not get from local server
                            if (NetworkUtil.getLocalServer() != null) {
                                try {
                                    if (IOUtil.parseHost(serverURL).equals(NetworkUtil.getLocalServer().getHostName())) {
                                        continue FOR;
                                    }
                                } catch (Exception e) {
                                }
                            }
                            Thread t = new Thread("Getting Network Status From " + serverURL) {

                                @Override
                                public void run() {
                                    debugOut("Trying to get the network status from " + serverURL);
                                    // catch connection errors
                                    try {
                                        TrancheServer ts = ConnectionUtil.connectURL(serverURL, false);
                                        if (ts != null) {
                                            StatusTable table = ts.getNetworkStatusPortion(GetNetworkStatusItem.RETURN_ALL, GetNetworkStatusItem.RETURN_ALL);
                                            table.removeDefunctRows();
                                            updateRows(table.getRows());
                                            success[j] = true;
                                            debugOut("Retreived the network status table from " + serverURL);
                                        }
                                    } catch (Exception e) {
                                        debugErr(e);
                                        ConnectionUtil.reportExceptionURL(serverURL, e);
                                    }
                                }
                            };
                            t.setDaemon(true);
                            t.start();
                            threads.add(t);
                        }
                        for (Thread t : threads) {
                            try {
                                t.join(5000);
                            } catch (Exception e) {
                                debugErr(e);
                            }
                        }
                        for (boolean s : success) {
                            if (s) {
                                break WHILE;
                            }
                        }
                    }
                    // adjust the connections
                    ConnectionUtil.adjustConnections();
                    // make sure the process for updating the network status is started
                    startUpdateProcess();
                } finally {
                    // made attempt, wake up waiting threads
                    finishedLoadingNetwork = true;
                    debugOut("Finished loading the network.");
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    /**
     * <p>Stops the opposite (client/server) process if it is running.</p>
     */
    private static void startUpdateProcess() {
        if (TestUtil.isTestingManualNetworkStatusTable()) {
            return;
        }
        if (localServer != null) {
            if (clientUpdateProcess != null) {
                // stop the client process
                if (clientUpdateProcess.isAlive()) {
                    debugOut("Killed the client updating process.");
                    clientUpdateProcess.safeStop();
                }
                // null the client process
                clientUpdateProcess = null;
            }
            // start a new server process?
            if (serverUpdateProcess == null || !serverUpdateProcess.isAlive()) {
                debugOut("Starting a new server updating process.");
                serverUpdateProcess = new ServerStatusUpdateProcess();
                serverUpdateProcess.start();
            }
        } else {
            if (serverUpdateProcess != null) {
                // stop the server process
                if (serverUpdateProcess.isAlive()) {
                    debugOut("Killed the server updating process.");
                    serverUpdateProcess.safeStop();
                }
                // null the server process
                serverUpdateProcess = null;
            }
            // start a new client process?
            if (clientUpdateProcess == null || !clientUpdateProcess.isAlive()) {
                debugOut("Starting a new client updating process.");
                clientUpdateProcess = new ClientStatusUpdateProcess();
                clientUpdateProcess.start();
            }
        }
    }

    /**
     * <p>Stops and starts the running update process.</p>
     */
    public static void restartUpdateProcess() {
        if (TestUtil.isTestingManualNetworkStatusTable()) {
            return;
        }
        debugOut("Restarting the update process.");
        // an old server process is running
        if (serverUpdateProcess != null) {
            // stop the server process
            if (serverUpdateProcess.isAlive()) {
                debugOut("Killed the client updating process.");
                serverUpdateProcess.safeStop();
            }
            // null the server process
            serverUpdateProcess = null;
        }
        // an old client process is running
        if (clientUpdateProcess != null) {
            // stop the client process
            if (clientUpdateProcess.isAlive()) {
                debugOut("Killed the server updating process.");
                clientUpdateProcess.safeStop();
            }
            // null the client process
            clientUpdateProcess = null;
        }
        startUpdateProcess();
    }

    /**
     * 
     */
    private static void stopUpdateProcess() {
        if (serverUpdateProcess != null) {
            serverUpdateProcess.safeStop();
            serverUpdateProcess = null;
        }
        if (clientUpdateProcess != null) {
            clientUpdateProcess.safeStop();
            clientUpdateProcess = null;
        }
    }

    /**
     * <p>Returns the master status table for this JVM.</p>
     * @return The status table for this JVM
     */
    public static StatusTable getStatus() {
        return masterStatusTable;
    }

    /**
     * <p>Updates the master status table with the given rows except when trying to update the local server row.</p>
     * <p>If the local server row needs to be updated use NetworkUtil.getLocalServerRow().update(NetworkUtil.getLocalServer()) method.</p>
     * @param row A status row
     */
    public static void updateRow(StatusTableRow row) {
        debugOut("Updating a row in the master status table.");
        // ignore the local server
        if (NetworkUtil.getLocalServerRow() != null && row.getHost().equals(NetworkUtil.getLocalServerRow().getHost())) {
            return;
        }
        debugOut(" Setting row: " + row.getHost());
        // check for port, secure in startup server list
        if (row.getPort() == 0) {
            for (String url : getStartupServerURLs()) {
                try {
                    String host = IOUtil.parseHost(url);
                    if (row.getHost().equals(normalize(host))) {
                        row.setPort(IOUtil.parsePort(url));
                        row.setIsSSL(IOUtil.parseSecure(url));
                    }
                } catch (Exception e) {
                }
            }
        }
        // update the master status table
        masterStatusTable.setRow(row);
    }

    /**
     * <p>Updates the master status table with the given rows except when trying to update the local server row.</p>
     * <p>If the local server row needs to be updated use NetworkUtil.getLocalServerRow().update(NetworkUtil.getLocalServer()) method.</p>
     * @param rows A collection of status rows
     */
    public static void updateRows(Collection<StatusTableRow> rows) {
        debugOut("Updating rows in the master status table.");
        Collection<StatusTableRow> rowsToSet = new HashSet<StatusTableRow>();
        for (StatusTableRow row : rows) {
            // ignore the local server
            if (NetworkUtil.getLocalServerRow() != null && row.getHost().equals(NetworkUtil.getLocalServerRow().getHost())) {
                continue;
            }
            debugOut(" Setting row: " + row.getHost());
            rowsToSet.add(row);
        }
        // update the master status table
        masterStatusTable.setRows(rowsToSet);
    }

    /**
     * <p>Returns the Tranche server running on this JVM, if there is one.</p>
     * @return The Tranche server running on this JVM, if there is one
     */
    public static Server getLocalServer() {
        return localServer;
    }

    /**
     * <p>Returns the status row for the Tranche server on this JVM, if there is one. Return NULL if there is no Trache server running on the local JVM.</p>
     * @return The status row for the Tranche server on this JVM
     */
    public static StatusTableRow getLocalServerRow() {
        if (localServer == null) {
            return null;
        }
        return masterStatusTable.getRow(localServer.getHostName());
    }

    /**
     * <p>Called by the Server upon shutdown.</p>
     */
    public static void clearLocalServer() {
        // Don't mess with test
        if (TestUtil.isTestingManualNetworkStatusTable()) {
            return;
        }

        debugOut("Cleared the local server.");

        // kill a connection if there is one
        StatusTableRow str = getLocalServerRow();
        if (str != null) {
            ConnectionUtil.safeForceClose(str.getHost(), "NetworkUtil.clearLocalServer invoked");
        }
        // remove the listener for the routing server
        if (localServer != null) {
            if (localServer.getTrancheServer() instanceof RoutingTrancheServer) {
                ((RoutingTrancheServer) localServer.getTrancheServer()).removeListener(routingTrancheServerListener);
            }
            // null
            localServer = null;
        }

        // may need to adjust the processes that are running to update the local status
        startUpdateProcess();
    }

    /**
     * <p>Sets the Tranche server for this JVM.</p>
     * <p>Will set the row in the network status table and load the network, if necessary.</p>
     * @param localServer A local Tranche server
     */
    public static void setLocalServer(Server localServer) {
        debugOut("Set the local server.");
        // set the server
        NetworkUtil.localServer = localServer;
        try {
            masterStatusTable.setRow(new StatusTableRow(localServer));
        } catch (Exception e) {
            debugErr(e);
        }
        boolean isFirstLoad = !startedLoadingNetwork;
        // set the listener for the routing server
        if (localServer.getTrancheServer() instanceof RoutingTrancheServer) {
            ((RoutingTrancheServer) localServer.getTrancheServer()).addListener(routingTrancheServerListener);
        }
        // load the network
        lazyLoad();
        // if the first load, loadNetwork() will do thiss
        if (!isFirstLoad) {
            // may need to adjust the processes that are running to update the local status
            startUpdateProcess();
        }
        // make sure server is registered
        for (StatusTableRowRange range : ServerStatusUpdateProcess.getStatusTableRowRanges()) {
            range.setRegistrationStatus(false);
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        NetworkUtil.debug = debug;
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
            DebugUtil.printOut(NetworkUtil.class.getName() + "> " + line);
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
