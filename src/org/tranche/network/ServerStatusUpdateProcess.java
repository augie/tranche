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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.tranche.ConfigureTranche;
import org.tranche.TrancheServer;
import org.tranche.logs.LogUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.server.GetNetworkStatusItem;
import org.tranche.server.Server;
import org.tranche.time.TimeUtil;
import org.tranche.util.EmailUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;
import org.tranche.commons.ThreadUtil;

/**
 * <p>The network status update process used when there is a local server.</p>
 * <p>The server update process consists of contacting a single server in a range of servers.</p>
 * <p>This process is the cornerstone of the status propagation scheme that was implemented to reduce the number of connections that were needed to be held by servers and clients.</p>
 * <p>To protected against malicious attack, only core servers can be trusted to return the status of other servers. Non-core servers must be contacted individually to determine their status.</p>
 * <p>The status table keeps all the rows within it sorted alphabetically by host name. Starting at the local host (if it's a core server), the core servers are split up into groups of a configurable size.</p>
 * <p>Each of the groups is called a range. Within this range, the first server that is not the local server and is online will be designated the "connection host", or the one from which the updates for that server should be requested.</p>
 * <p>Core servers with non-core servers alphabetically after them and before another core server must request the status of each of those servers individually.</p>
 * <p>It is with this orderly scheme that we can be sure the information will be propagated in an orderly manner.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ServerStatusUpdateProcess extends StatusUpdateProcess {

    private static final Set<StatusTableRowRange> ranges = new HashSet<StatusTableRowRange>(), nonCoreServersToUpdate = new HashSet<StatusTableRowRange>();
    private static long offlineServerNotificationTimestamp = 0;

    /**
     * <p>Default constructor</p>
     */
    protected ServerStatusUpdateProcess() {
        setName("Server Status Update Process");
        setDaemon(true);
    }

    /**
     * <p>Starts the process.</p>
     */
    @Override
    public void run() {
        debugOut("Starting the process.");
        // keep going
        while (isRunning) {
            try {
                ThreadUtil.sleep(ConfigureTranche.getInt(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_STATUS_UPDATE_SERVER_FREQUENCY));

                if (NetworkUtil.getLocalServer() != null && NetworkUtil.getLocalServerRow() != null) {
                    // update the local server info -- do not let a problem here stop the updates
                    try {
                        NetworkUtil.getLocalServerRow().update(NetworkUtil.getLocalServer());
                    } catch (Exception e) {
                        debugErr(e);
                    }
                } else {
                    if (NetworkUtil.getLocalServer() == null) {
                        debugOut("Local server is null.");
                    } else if (NetworkUtil.getLocalServerRow() == null) {
                        debugOut("Local server row is null.");
                    }
                }

                debugOut("Starting to perform an update.");

                // If testing manual network status table, then don't do anything!
                if (TestUtil.isTestingManualNetworkStatusTable()) {
                    continue;
                }
                final StatusTable networkStatusTable = NetworkUtil.getStatus().clone();

                // Internet outage -- try to reload the network
                if (networkStatusTable.size() > 1) {
                    boolean connectedToAnyOtherThanLocal = false;
                    for (String host : ConnectionUtil.getConnectedHosts()) {
                        if (NetworkUtil.getLocalServerRow().getHost().equals(host)) {
                            continue;
                        }
                        connectedToAnyOtherThanLocal = true;
                        break;
                    }
                    if (!connectedToAnyOtherThanLocal) {
                        debugOut("Not connected to anything but the local server. Reloading network.");
                        NetworkUtil.reload();
                        continue;
                    }
                    // check status table row ranges
//                    if (getStatusTableRowRanges().size() == 0) {
//                        ConnectionUtil.adjustConnections();
//                        continue;
//                    }
                }

                // check in the same way as the client
                debugOut("Starting to perform an update.");

                for (final StatusTableRow row : networkStatusTable.getRows()) {

                    if (NetworkUtil.isBannedServer(row.getHost()) || !row.isOnline() || row.isLocalServer()) {
                        continue;
                    }

                    Thread t = new Thread() {

                        @Override()
                        public void run() {
                            debugOut("Trying to update network status from: " + row.getHost());
                            try {
                                TrancheServer ts = ConnectionUtil.connect(row, true);
                                if (ts != null) {
                                    StatusTable table = ts.getNetworkStatusPortion(GetNetworkStatusItem.RETURN_ALL, GetNetworkStatusItem.RETURN_ALL);
                                    table.removeDefunctRows();
                                    table.getRow(row.getHost()).update(IOUtil.getConfiguration(ts, SecurityUtil.getAnonymousCertificate(), SecurityUtil.getAnonymousKey()));
                                    for (StatusTableRow returnedRow : table.getRows()) {
                                        if (returnedRow.getHost().equals(row.getHost()) | !networkStatusTable.contains(returnedRow.getHost())) {
                                            NetworkUtil.updateRow(returnedRow);
                                        }
                                    }
                                    ts.registerServer(NetworkUtil.getLocalServerRow().getURL());
                                }
                            } catch (Exception e) {
                                debugErr(e);
                                ConnectionUtil.reportException(row, e);
                            } finally {
                                ConnectionUtil.unlockConnection(row.getHost());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.start();
                    Thread.yield();
                }

                // alternative to the above
                {
//                // perform the update from ranges
//                for (final StatusTableRowRange range : getStatusTableRowRanges()) {
//                    try {
//                        final TrancheServer ts = ConnectionUtil.connectHost(range.getConnectionHost(), true);
//                        // this connection died at some point
//                        if (ts == null) {
//                            throw new Exception("Could not establish connection with " + range.getConnectionHost() + " for range from " + range.getFrom() + " to " + range.getTo());
//                        }
//                        try {
//                            debugOut("Updating from " + range.getConnectionHost() + " with range " + range.getFrom() + " to " + range.getTo());
//                            try {
//                                StatusTable table = ts.getNetworkStatusPortion(range.getFrom(), range.getTo());
//                                // reregister this server if the status table says the local server is offline
//                                try {
//                                    for (StatusTableRow row : table.getRows()) {
//                                        debugOut("Is " + row.getHost() + " the local server and offline? " + (row.getHost().equals(NetworkUtil.getLocalServerRow().getHost()) && !row.isOnline()));
//                                        if (row.getHost().equals(NetworkUtil.getLocalServerRow().getHost()) && !row.isOnline()) {
//                                            range.setRegistrationStatus(false);
//                                            break;
//                                        }
//                                    }
//                                } catch (Exception e) {
//                                    debugErr(e);
//                                }
//                                table.removeDefunctRows();
//                                NetworkUtil.updateRows(table.getRows());
//                            } catch (Exception e) {
//                                debugErr(e);
//                                ConnectionUtil.reportExceptionHost(range.getConnectionHost(), e);
//                            }
//                            // policy: need to register once with each server we connect to for status update info
//                            //  because of the possibility of servers going offline during the time after being registered with
//                            //  and before propogating that registration
//                            if (!range.isRegisteredWithConnectionHost()) {
//                                debugOut("Registering local server with " + range.getConnectionHost());
//                                try {
//                                    ts.registerServer(NetworkUtil.getLocalServerRow().getURL());
//                                    range.setRegistrationStatus(true);
//                                    debugOut("Successfully registered local server with " + range.getConnectionHost());
//                                } catch (Exception e) {
//                                    debugErr(e);
//                                    ConnectionUtil.reportExceptionHost(range.getConnectionHost(), e);
//                                }
//                            }
//                        } finally {
//                            ConnectionUtil.unlockConnection(range.getConnectionHost());
//                        }
//                    } catch (Exception e) {
//                        debugErr(e);
//                        ConnectionUtil.reportExceptionHost(range.getConnectionHost(), e);
//                    }
//                }
//                // perform the update from non-core servers that are listed immediately after the local server
//                for (final StatusTableRowRange range : getNonCoreServersToUpdate()) {
//                    try {
//                        TrancheServer ts = ConnectionUtil.connectHost(range.getConnectionHost(), true);
//                        if (ts == null) {
//                            throw new Exception("Could not establish connection with non-core server " + range.getFrom());
//                        }
//                        try {
//                            debugOut("Updating from non-core server " + range.getConnectionHost());
//                            StatusTable table = ts.getNetworkStatusPortion(range.getFrom(), range.getTo());
//                            table.removeDefunctRows();
//                            NetworkUtil.updateRows(table.getRows());
//                        } finally {
//                            ConnectionUtil.unlockConnection(range.getConnectionHost());
//                        }
//                    } catch (Exception e) {
//                        debugErr(e);
//                        ConnectionUtil.reportExceptionHost(range.getConnectionHost(), e);
//                    }
//                }
                }
                // the online server at the top of the list is responsible for notifying the administration team that servers are offline
                try {
                    String topOnlineHost = "";
                    for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                        if (row.isOnline() && row.isCore()) {
                            topOnlineHost = row.getHost();
                            break;
                        }
                    }
                    // some time between emails
                    long interval = ConfigureTranche.getLong(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_OFFLINE_NOTIFICATION_INTERVAL);
                    if (topOnlineHost.equals(NetworkUtil.getLocalServerRow().getHost()) && interval > 0 && TimeUtil.getTrancheTimestamp() - offlineServerNotificationTimestamp > interval) {
                        offlineServerNotificationTimestamp = TimeUtil.getTrancheTimestamp();
                        // check the number of core servers that are offline
                        Set<StatusTableRow> offlineCoreServerSet = new HashSet<StatusTableRow>();
                        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
                            if (!row.isOnline() && row.isCore()) {
                                offlineCoreServerSet.add(row);
                            }
                        }
                        // send an email if there's something noteworthy
                        if (ConfigureTranche.getAdminEmailAccounts().length > 0 && offlineCoreServerSet.size() > 0) {
                            String message = offlineCoreServerSet.size() + " core servers are offline:" + "\n" + "\n";
                            for (StatusTableRow row : offlineCoreServerSet) {
                                message = message + "  " + row.getName() + " (" + row.getHost() + ")" + "\n";
                            }
                            EmailUtil.safeSendEmail("[" + ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_NAME) + "] " + offlineCoreServerSet.size() + " Core Servers Offline", ConfigureTranche.getAdminEmailAccounts(), message);
                        }
                    }
                } catch (Exception e) {
                    LogUtil.logError(e);
                }
                // check the status table for servers to clear
                NetworkUtil.getStatus().removeDefunctRows();
            } catch (Exception e) {
                debugErr(e);
            }
        }
    }

    /**
     * <p>Gets the row ranges.</p>
     * @return The row ranges
     */
    public synchronized static final Collection<StatusTableRowRange> getStatusTableRowRanges() {
        return new HashSet<StatusTableRowRange>(ranges);
    }

    /**
     * <p>Gets the non-core servers that the local server will update individually.</p>
     * @return
     */
    public synchronized static final Collection<StatusTableRowRange> getNonCoreServersToUpdate() {
        return new HashSet<StatusTableRowRange>(nonCoreServersToUpdate);
    }

    /**
     * <p>Updates the status table row ranges based on the status of the network.</p>
     * <p>Also determines the non-core servers the local server should be getting updates from.</p>
     */
    protected synchronized static final void adjustStatusTableRowRanges() {
        // restart ranges
        ranges.clear();
        nonCoreServersToUpdate.clear();

        Server localServer = NetworkUtil.getLocalServer();

        // Should this be an exception?
        if (localServer == null) {
            return;
        }

        // Get information for local server.
        ServerStatusTablePerspective info = getServerStatusTablePerspectiveForServer(localServer.getHostName());

        // Note that this is fine even if aren't any (e.g., just one server, etc.)
        ranges.addAll(info.coreServers);
        nonCoreServersToUpdate.addAll(info.nonCoreServers);
    }

    /**
     * <p>Get the status table information from the perspective of specified host. This could be the local host or a remote one.</p>
     * @param host The host for the server from whose perspective we want to see the network
     * @return
     */
    public static final ServerStatusTablePerspective getServerStatusTablePerspectiveForServer(String host) {

        List<StatusTableRow> rows = NetworkUtil.getStatus().getRows();
        ServerStatusTablePerspective info = new ServerStatusTablePerspective(host);

        // can't be just one server. Return empty information.
        if (rows.size() == 1 && rows.get(0).getHost().equals(host)) {
            return info;
        }

        List<StatusTableRow> coreRows = new ArrayList<StatusTableRow>();
        // where to start?
        int startIndex = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getHost().equals(host)) {
                startIndex = i;
                break;
            }
        }
        // Sort the rows so starting at appropriate index ('target' host)
        for (int i = 1; i < rows.size(); i++) {

            // Get the offset
            int offset = startIndex + i;

            // Make sure that we start at zero after reaching end
            offset = offset % rows.size();

            // get the row
            StatusTableRow row = rows.get(offset);

            // this row must be a core server
            if (!row.isCore()) {
                // add all non-core servers immediately after the local server to the ranges of servers to update individually
                if (coreRows.isEmpty()) {
                    StatusTableRow nextRow = null;
                    // if last
                    if (i + 1 == rows.size()) {
                        if (startIndex != -1) {
                            nextRow = rows.get(startIndex);
                        }
                    } else {
                        nextRow = rows.get((i + 1 + startIndex) % rows.size());
                    }
                    // dummy check
                    if (nextRow != null) {
                        info.addNonCoreServerRowRange(new StatusTableRowRange(row.getHost(), nextRow.getHost()));
                    }
                }
                continue;
            }
            // loops at the end of the rows structure -- adds each row only once
            coreRows.add(row);
        }
        // add the target server to the front of the list of core rows
        if (startIndex != -1) {
            coreRows.add(0, rows.get(startIndex));
        }
        // now go through the rows creating ranges
        for (int i = 0; i < coreRows.size();) {
            if (i + ConfigureTranche.getInt(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_STATUS_UPDATE_SERVER_GROUPING) >= coreRows.size()) {
                StatusTableRowRange range = new StatusTableRowRange(coreRows.get(i).getHost(), coreRows.get(0).getHost());
                for (int j = i; j < coreRows.size(); j++) {
                    // if the current server is the local host or is offline, connect to the next one
                    if (range.getConnectionHost().equals(host) || !coreRows.get(j).isOnline()) {
                        range.setConnectionHost(coreRows.get((j + 1) % coreRows.size()).getHost());
                    } else {
                        break;
                    }
                }
                // if we are connected to the last in the range and it is the local host or offline, don't bother adding to the ranges
                if (!(range.getConnectionHost().equals(coreRows.get(0).getHost()) && (range.getConnectionHost().equals(host) || !coreRows.get(0).isOnline()))) {
                    info.addCoreServerRowRange(range);
                }
                i = coreRows.size();
            } else {
                StatusTableRowRange range = new StatusTableRowRange(coreRows.get(i).getHost(), coreRows.get(i + ConfigureTranche.getInt(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_STATUS_UPDATE_SERVER_GROUPING)).getHost());
                for (int j = i; j < i + ConfigureTranche.getInt(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_STATUS_UPDATE_SERVER_GROUPING); j++) {
                    // if the current server is the local host or is offline, connect to the next one
                    if (range.getConnectionHost().equals(host) || !coreRows.get(j).isOnline()) {
                        range.setConnectionHost(coreRows.get(j + 1).getHost());
                    } else {
                        break;
                    }
                }
                // if we are connected to the last in the range and it is the local host or offline, don't bother adding to the ranges
                if (!(range.getConnectionHost().equals(coreRows.get(i + ConfigureTranche.getInt(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_STATUS_UPDATE_SERVER_GROUPING)).getHost()) && (range.getConnectionHost().equals(host) || !coreRows.get(i + ConfigureTranche.getInt(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_STATUS_UPDATE_SERVER_GROUPING)).isOnline()))) {
                    info.addCoreServerRowRange(range);
                }
                i += ConfigureTranche.getInt(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_STATUS_UPDATE_SERVER_GROUPING);
            }
        }

        return info;
    }
}
