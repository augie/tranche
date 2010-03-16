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

import org.tranche.ConfigureTranche;
import org.tranche.TrancheServer;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.server.GetNetworkStatusItem;
import org.tranche.util.DebugUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.ThreadUtil;

/**
 * <p>The network status update process used when there is no local server.</p>
 * <p>The client update process only consists of contacting a single server and getting the network from it.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ClientStatusUpdateProcess extends StatusUpdateProcess {

    private static boolean debug = false;
    private static String lastCheckedHost;

    /**
     * <p>Default constructor</p>
     */
    protected ClientStatusUpdateProcess() {
        setName("Client Status Update Process");
        setDaemon(true);
    }

    /**
     * <p>Starts the process.</p>
     */
    @Override
    public void run() {
        debugOut("Started the process");

        while (isRunning) {
//            updateUsingStatusTablePortions();
            updateDirectly();

            ThreadUtil.safeSleep(ConfigureTranche.getInt(ConfigureTranche.PROP_STATUS_UPDATE_CLIENT_FREQUENCY));
        }
    }

    /**
     * 
     */
    private void updateDirectly() {

        // If testing manual network status table, then don't do anything!
        if (TestUtil.isTestingManualNetworkStatusTable()) {
            return;
        }

        debugOut("Starting to perform an update.");

        // Internet outage -- try to reload the network
        if (NetworkUtil.getStatus().size() > 1 && ConnectionUtil.size() == 0) {
            NetworkUtil.reload();
            return;
        }

        final int MAX_ATTEMPTS = 3;

        HOSTS:
        for (final String host : NetworkUtil.getStatus().getHosts()) {

            ATTEMPTS:
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

                final boolean[] isCompleted = {false};

                Thread t = new Thread() {

                    @Override()
                    public void run() {
                        debugOut("Trying to update network status from last checked: " + host);
                        TrancheServer ts = null;
                        try {
                            ts = ConnectionUtil.getHost(host);
                            // server connection no longer in pool
                            if (ts != null) {
                                ConnectionUtil.lockConnection(host);
                                StatusTable table = ts.getNetworkStatusPortion(host, host);

                                StatusTableRow row = table.getRow(host);

                                if (row == null) {
                                    throw new AssertionFailedException("Expect to find row for host, instead null.");
                                }

                                NetworkUtil.getStatus().setRow(row);
                                isCompleted[0] = true;
                            }
                        } catch (Exception e) {
                            debugErr(e);
                        } finally {
                            ConnectionUtil.unlockConnection(host);
                        }
                    }
                };
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();

                try {
                    // Give it a little time only
                    t.join(5 * 1000);
                } catch (Exception e) {
                    // nope
                }

                if (t.isAlive() && !isCompleted[0]) {
                    t.interrupt();
                }

                // Success, continue
                if (isCompleted[0]) {
                    continue HOSTS;
                }

                // Did if fail? Flag it as offline
                if (!isCompleted[0] && attempt == MAX_ATTEMPTS) {
                    StatusTableRow row = NetworkUtil.getStatus().getRow(host);
                    if (row == null && row.isOnline()) {
                        ConnectionUtil.flagOffline(host);
                    }
                }
            }
        } // attempt
    } // updateDirectly

    /**
     * 
     */
    private void updateUsingStatusTablePortions() {
        try {


            // If testing manual network status table, then don't do anything!
            if (TestUtil.isTestingManualNetworkStatusTable()) {
                return;
            }

            debugOut("Starting to perform an update.");

            // Internet outage -- try to reload the network
            if (NetworkUtil.getStatus().size() > 1 && ConnectionUtil.size() == 0) {
                NetworkUtil.reload();
                return;
            }

            // perform the update
            boolean updated = false;
            StatusTable table = null;
            // check the same server again
            if (lastCheckedHost != null) {
                debugOut("Trying to update network status from last checked: " + lastCheckedHost);
                try {
                    // get the whole network from a single server to which we already have a connection
                    TrancheServer ts = ConnectionUtil.getHost(lastCheckedHost);
                    // server connection no longer in pool
                    if (ts != null) {
                        ConnectionUtil.lockConnection(lastCheckedHost);
                        try {
                            // update the status
                            table = ts.getNetworkStatusPortion(GetNetworkStatusItem.RETURN_ALL, GetNetworkStatusItem.RETURN_ALL);
                            updated = true;
                        } catch (Exception e) {
                            debugErr(e);
                        } finally {
                            ConnectionUtil.unlockConnection(lastCheckedHost);
                        }
                    }
                } catch (Exception e) {
                    debugErr(e);
                    ConnectionUtil.reportExceptionHost(lastCheckedHost, e);
                }
            }
            // could not get status from the same server
            if (!updated) {
                // try each of the connections
                for (String host : ConnectionUtil.getConnectedHosts()) {
                    // already tried this one
                    if (updated || (lastCheckedHost != null && host.equals(lastCheckedHost))) {
                        return;
                    }
                    debugOut("Trying to update network status from " + host);
                    try {
                        // get the whole network from a single server to which we already have a connection
                        TrancheServer ts = ConnectionUtil.getHost(host);
                        // server connection no longer in pool
                        if (ts != null) {
                            ConnectionUtil.lockConnection(host);
                            try {
                                // update the status
                                table = ts.getNetworkStatusPortion(GetNetworkStatusItem.RETURN_ALL, GetNetworkStatusItem.RETURN_ALL);
                                lastCheckedHost = host;
                                updated = true;
                            } catch (Exception e) {
                                debugErr(e);
                            } finally {
                                ConnectionUtil.unlockConnection(host);
                            }
                        }
                    } catch (Exception e) {
                        debugErr(e);
                        ConnectionUtil.reportExceptionHost(host, e);
                    }
                }
            }
            // remove defunct rows
            table.removeDefunctRows();
            // update the master table with the returned table
            NetworkUtil.updateRows(table.getRows());
            // check the status table for servers to clear
            NetworkUtil.getStatus().removeDefunctRows();
        } catch (Exception e) {
            debugErr(e);
            setException(e);
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        ClientStatusUpdateProcess.debug = debug;
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
            DebugUtil.printOut(ClientStatusUpdateProcess.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param line
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
