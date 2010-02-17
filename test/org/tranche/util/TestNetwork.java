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
package org.tranche.util;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTable;
import org.tranche.network.StatusTableRow;
import org.tranche.routing.RoutingTrancheServer;
import org.tranche.server.Server;

/**
 * <p></p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class TestNetwork {

    private Set<TestServerConfiguration> serverConfigs;
    private Map<TestServerConfiguration, FlatFileTrancheServer> fftsMap;
    private Map<TestServerConfiguration, RoutingTrancheServer> routingMap;
    private Map<TestServerConfiguration, Server> serverMap;
    private Collection<String> prevStartupServers = null;
    private Set<File> toDelete;
    private boolean isRunning = false;

    public TestNetwork() {
        serverConfigs = new HashSet();
        fftsMap = new HashMap();
        serverMap = new HashMap();
        routingMap = new HashMap();
        toDelete = new HashSet();
    }

    /**
     * <p>Add server to network.</p>
     * <p>Note that you will get a runtime exception if the network is running. To add more servers, stop network first.</p>
     * @param config
     * @return
     */
    public synchronized boolean addTestServerConfiguration(TestServerConfiguration config) {

        if (isRunning) {
            throw new RuntimeException("Cannot add test server configurations while running. Stop network before adding servers.");
        }

        return this.serverConfigs.add(config);
    }

    /**
     * <p>Starts network. Will start servers. Make sure to stop network to shutdown resources.</p>
     * @throws java.lang.Exception
     */
    public synchronized void start() throws Exception {

        if (isRunning) {
            throw new RuntimeException("Test network is already running.");
        }
        isRunning = true;
        
        TestUtil.clearFFTSForURL();

        TestUtil.setTesting(true);
        TestUtil.setTestingManualNetworkStatusTable(true);
        prevStartupServers = NetworkUtil.getStartupServerURLs();

        Set<StatusTableRow> rows = new HashSet<StatusTableRow>();
        NetworkUtil.updateRows(rows);

        // -----------------------------------------------------------------------
        // STEP 1: Add the host-to-url map entries
        // -----------------------------------------------------------------------
        for (TestServerConfiguration conf : this.serverConfigs) {
            TestUtil.addServerToHostTestMap(conf.ostensibleHost, conf.getActualURL());
        }

        // -----------------------------------------------------------------------
        // STEP 2: Set to be startup servers. Note some might not be core servers!
        // -----------------------------------------------------------------------
        Set<String> coreServers = new HashSet();
        for (TestServerConfiguration conf : this.serverConfigs) {
            if (conf.isCoreServer) {
                coreServers.add(conf.getOstensibleURL());
            }
        }
        NetworkUtil.setStartupServerURLs(coreServers);

        // -----------------------------------------------------------------------
        // STEP 3: Start up each server--but only if online!
        // -----------------------------------------------------------------------
        try {
            for (TestServerConfiguration conf : this.serverConfigs) {
                TrancheServer wrappedServer;
                StatusTableRow row = conf.getStatusTableRow();

                // Build: FFTS
                if (conf.serverFlag == TestServerConfiguration.DATA_SERVER_FLAG) {
                    File dir = TempFileUtil.createTemporaryDirectory();
                    toDelete.add(dir);

                    FlatFileTrancheServer ffts = new FlatFileTrancheServer(dir.getAbsolutePath(), DevUtil.getFFTSAuthority(), DevUtil.getFFTSPrivateKey());
                    ffts.getConfiguration().addUser(DevUtil.getDevUser());
                    ffts.getConfiguration().addUser(DevUtil.getFFTSUser());
                    ffts.getConfiguration().addUser(DevUtil.getRoutingTrancheServerUser());
                    ffts.getConfiguration().setHashSpans(conf.hashSpans);
                    ffts.getConfiguration().setTargetHashSpans(conf.hashSpans);

                    // Sporadic issues with server having wrong host name cached in getHost(). This should solve.
                    ffts.getConfiguration().setValue(ConfigKeys.URL, conf.getOstensibleURL());
                    ffts.saveConfiguration();
                    row.update(ffts.getConfiguration());

                    this.fftsMap.put(conf, ffts);
                    
                    TestUtil.setFFTSForURL(conf.getActualURL(), ffts);

                    wrappedServer = ffts;
                } // Build: routing server
                else if (conf.serverFlag == TestServerConfiguration.ROUTING_SERVER_FLAG) {
                    RoutingTrancheServer routing = new RoutingTrancheServer(DevUtil.getRoutingTrancheServerAuthority(), DevUtil.getRoutingTrancheServerPrivateKey());
                    routing.getConfiguration().addUser(DevUtil.getDevUser());
                    routing.getConfiguration().addUser(DevUtil.getFFTSUser());
                    routing.getConfiguration().addUser(DevUtil.getRoutingTrancheServerUser());
                    routing.getConfiguration().setHashSpans(conf.hashSpans);
                    routing.getConfiguration().setTargetHashSpans(conf.hashSpans);

                    // Sporadic issues with server having wrong host name cached in getHost(). This should solve.
                    routing.getConfiguration().setValue(ConfigKeys.URL, conf.getOstensibleURL());
                    routing.saveConfiguration();
                    row.update(routing.getConfiguration());

                    this.routingMap.put(conf, routing);

                    wrappedServer = routing;
                } else if (conf.serverFlag == TestServerConfiguration.FAILING_DATA_SERVER_FLAG) {
                    File dir = TempFileUtil.createTemporaryDirectory();
                    toDelete.add(dir);

                    FailingFlatFileTrancheServer ffts = new FailingFlatFileTrancheServer(dir.getAbsolutePath(), DevUtil.getFFTSAuthority(), DevUtil.getFFTSPrivateKey(), conf.failingProbability);
                    ffts.getConfiguration().addUser(DevUtil.getDevUser());
                    ffts.getConfiguration().addUser(DevUtil.getFFTSUser());
                    ffts.getConfiguration().addUser(DevUtil.getRoutingTrancheServerUser());
                    ffts.getConfiguration().setHashSpans(conf.hashSpans);
                    ffts.getConfiguration().setTargetHashSpans(conf.hashSpans);

                    // Sporadic issues with server having wrong host name cached in getHost(). This should solve.
                    ffts.getConfiguration().setValue(ConfigKeys.URL, conf.getOstensibleURL());
                    ffts.saveConfiguration();
                    row.update(ffts.getConfiguration());
                    
                    TestUtil.setFFTSForURL(conf.getActualURL(), ffts);

                    this.fftsMap.put(conf, ffts);

                    wrappedServer = ffts;
                } else {
                    throw new RuntimeException("Unrecognized flag: " + conf.serverFlag);
                }

                // Build: Server
                Server s = new Server(wrappedServer, conf.actualPort, conf.isSSL);
                if (conf.isOnline) {
                    s.start();
                }
                this.serverMap.put(conf, s);
                rows.add(row);
            }
        } finally {
            NetworkUtil.updateRows(rows);
        }
    }

    /**
     * <p>Stops network. Tears down resources associated with servers.</p>
     * @throws java.lang.Exception
     */
    public synchronized void stop() {
        // Don't do anything if not running
        if (!isRunning) {
            return;
        }
        
        TestUtil.clearFFTSForURL();

        // Shut down all online servers
        for (TestServerConfiguration conf : this.serverConfigs) {
            // Shutdown: RTS
            ConnectionUtil.safeForceClose(conf.ostensibleHost, "Test network stopping.");

            // Shutdown: Server
            IOUtil.safeClose(this.serverMap.get(conf));

            // Shutdown: FFTS
            IOUtil.safeClose(this.fftsMap.get(conf));
        }

        // Return to previous configuration
        if (prevStartupServers != null) {
            NetworkUtil.setStartupServerURLs(prevStartupServers);
        }
        TestUtil.clearServerToHostTestMap();
        TestUtil.setTestingManualNetworkStatusTable(false);

        // Recursively delete home directories
        for (File f : toDelete) {
            IOUtil.recursiveDelete(f);
        }

        // Clear maps so can GC
        this.fftsMap.clear();
        this.serverMap.clear();
        this.serverConfigs.clear();

        // Any configuration files?
        File parentFile = PersistentFileUtil.getPersistentDirectory();
        File configFile = new File(parentFile, "configuration");
        if (configFile.exists()) {
            IOUtil.safeDelete(configFile);
        }

        suspendedHosts.clear();
    }

    /**
     * <p>Returns the running server (generated by this class) for ostensible host. Don't create your own servers!</p>
     * <p>Note this will throw an exception if network is not running. Do not call if server is not running.</p>
     * <p>Hint: Use ConnectionUtil.getHost(ostensibleHost) to get RemoteTrancheServer connection.</p>
     * @param ostensibleHost
     * @return
     */
    public RoutingTrancheServer getRoutingTrancheServer(String ostensibleHost) {
        if (!isRunning) {
            throw new RuntimeException("Cannot return server since network is not running.");
        }

        for (TestServerConfiguration conf : this.serverConfigs) {
            if (conf.ostensibleHost.equals(ostensibleHost)) {
                try {
                    return this.routingMap.get(conf);
                } finally {
                }
            }
        }

        return null;
    }

    public Collection<TestServerConfiguration> getServerConfigs() {
        return serverConfigs;
    }

    /**
     * <p>Returns the running server (generated by this class) for ostensible host. Don't create your own servers!</p>
     * <p>Note this will throw an exception if network is not running. Do not call if server is not running.</p>
     * <p>Hint: Use ConnectionUtil.getHost(ostensibleHost) to get RemoteTrancheServer connection.</p>
     * @param ostensibleHost
     * @return
     */
    public FlatFileTrancheServer getFlatFileTrancheServer(String ostensibleHost) {

        if (!isRunning) {
            throw new RuntimeException("Cannot return server since network is not running.");
        }

        for (TestServerConfiguration conf : this.serverConfigs) {
            if (conf.ostensibleHost.equals(ostensibleHost)) {
                return this.fftsMap.get(conf);
            }
        }
        return null;
    }

    /**
     * <p>Returns the running server (generated by this class) for ostensible host. Don't create your own servers!</p>
     * <p>Note this will throw an exception if network is not running. Do not call if server is not running.</p>
     * <p>Hint: Use ConnectionUtil.getHost(ostensibleHost) to get RemoteTrancheServer connection.</p>
     * @param ostensibleHost
     * @return
     */
    public Server getServer(String ostensibleHost) {

        if (!isRunning) {
            throw new RuntimeException("Cannot return server since network is not running.");
        }

        for (TestServerConfiguration conf : this.serverConfigs) {
            if (conf.ostensibleHost.equals(ostensibleHost)) {
                return this.serverMap.get(conf);
            }
        }
        return null;
    }

    public TestServerConfiguration getTestServerConfiguration(String host) {
        for (TestServerConfiguration conf : this.serverConfigs) {
            if (conf.ostensibleHost.equals(host)) {
                return conf;
            }
        }

        return null;
    }

    public void updateNetwork() throws Exception {
        StatusTable table = NetworkUtil.getStatus();

        for (StatusTableRow row : table.getRows()) {
            Configuration config = null;

            if (this.getFlatFileTrancheServer(row.getHost()) != null) {
                config = this.getFlatFileTrancheServer(row.getHost()).getConfiguration();
            } else if (this.getRoutingTrancheServer(row.getHost()) != null) {
                config = this.getRoutingTrancheServer(row.getHost()).getConfiguration();
            }

            row.update(config);
        }
    }
    private Map<String, SuspendedHost> suspendedHosts = new HashMap();

    /**
     * <p>Temporarily suspend a server (i.e., take it offline).</p>
     * @param host
     */
    public synchronized void suspendServer(String host) throws Exception {
        if (suspendedHosts.containsKey(host)) {
            return;
        }
        if (!this.getTestServerConfiguration(host).isOnline) {
            return;
        }
        suspendedHosts.put(host, new SuspendedHost(host));
    }

    /**
     * <p>Resume a suspended server.</p>
     * @param host
     */
    public synchronized void resumeServer(String host) throws Exception {
        if (!suspendedHosts.containsKey(host)) {
            System.err.println("WARNING: Nothing to resume for host<"+host+">");
            return;
        }

        SuspendedHost sh = suspendedHosts.get(host);
        sh.resume();
        suspendedHosts.remove(host);
    }

    /**
     * When instantiate, suspends.
     */
    class SuspendedHost {

        final TestServerConfiguration conf;
        final File homeDir;

        SuspendedHost(String h) throws Exception {

            try {
            conf = getTestServerConfiguration(h);
            if (conf == null) {
                throw new Exception("Could not find test server configuration.");
            }

            // Suspend server
            IOUtil.safeClose(TestNetwork.this.getServer(h));

            switch (this.conf.serverFlag) {
                case TestServerConfiguration.DATA_SERVER_FLAG:
                    FlatFileTrancheServer ffts = TestNetwork.this.fftsMap.get(conf);
                    this.homeDir = ffts.getHomeDirectory();
                    IOUtil.safeClose(ffts);
                    break;
                case TestServerConfiguration.ROUTING_SERVER_FLAG:
                    RoutingTrancheServer routing = TestNetwork.this.routingMap.get(conf);
                    this.homeDir = null;
                    IOUtil.safeClose(routing);
                    break;
                default:
                    throw new Exception("Unrecognized server type: " + this.conf.serverFlag);
            }

            StatusTableRow newRow = new StatusTableRow(conf.ostensibleHost, conf.ostensiblePort, conf.isSSL, false);
            addRowToNetworkStatusTable(newRow);
            } catch (Exception e) {
                System.err.println(e.getClass().getSimpleName()+" occurred while suspending "+h+": "+e.getMessage());
                throw e;
            }
        }

        /**
         * <p>Helper method.</p>
         * @param newRow
         */
        private void addRowToNetworkStatusTable(StatusTableRow newRow) {
            Collection<StatusTableRow> allRows = new HashSet();

            // Assert row isn't there (perchance things change)
            for (StatusTableRow next : NetworkUtil.getStatus().getRows()) {
                if (!next.getHost().equals(newRow.getHost())) {
                    allRows.add(next);
                }
            }

            allRows.add(newRow);

            NetworkUtil.updateRows(allRows);
        }

        /**
         * 
         * @throws java.lang.Exception
         */
        public void resume() throws Exception {

            TrancheServer wrappedServer = null;

            try {

                if (this.conf.serverFlag == TestServerConfiguration.DATA_SERVER_FLAG) {
                    FlatFileTrancheServer ffts = new FlatFileTrancheServer(homeDir.getAbsolutePath(), DevUtil.getFFTSAuthority(), DevUtil.getFFTSPrivateKey());
                    ffts.getConfiguration().addUser(DevUtil.getDevUser());
                    ffts.getConfiguration().addUser(DevUtil.getFFTSUser());
                    ffts.getConfiguration().addUser(DevUtil.getRoutingTrancheServerUser());
                    ffts.getConfiguration().setHashSpans(conf.hashSpans);
                    ffts.getConfiguration().setTargetHashSpans(conf.hashSpans);

                    StatusTableRow row = NetworkUtil.getStatus().getRow(conf.ostensibleHost);

                    // Sporadic issues with server having wrong host name cached in getHost(). This should solve.
                    ffts.getConfiguration().setValue(ConfigKeys.URL, conf.getOstensibleURL());
                    row.update(ffts.getConfiguration());

                    TestNetwork.this.fftsMap.put(conf, ffts);

                    wrappedServer = ffts;
                } else if (this.conf.serverFlag == TestServerConfiguration.ROUTING_SERVER_FLAG) {
                    RoutingTrancheServer routing = new RoutingTrancheServer(DevUtil.getRoutingTrancheServerAuthority(), DevUtil.getRoutingTrancheServerPrivateKey());
                    routing.getConfiguration().addUser(DevUtil.getDevUser());
                    routing.getConfiguration().addUser(DevUtil.getFFTSUser());
                    routing.getConfiguration().addUser(DevUtil.getRoutingTrancheServerUser());
                    routing.getConfiguration().setHashSpans(conf.hashSpans);
                    routing.getConfiguration().setTargetHashSpans(conf.hashSpans);

                    StatusTableRow row = NetworkUtil.getStatus().getRow(conf.ostensibleHost);

                    // Sporadic issues with server having wrong host name cached in getHost(). This should solve.
                    routing.getConfiguration().setValue(ConfigKeys.URL, conf.getOstensibleURL());
                    row.update(routing.getConfiguration());

                    TestNetwork.this.routingMap.put(conf, routing);

                    wrappedServer = routing;
                } else {
                    throw new Exception("Unrecognized server type: " + this.conf.serverFlag);
                }

                Server s = new Server(wrappedServer, conf.actualPort, conf.isSSL);
                s.start();
                TestNetwork.this.serverMap.put(conf, s);

                StatusTableRow newRow = new StatusTableRow(conf.ostensibleHost, conf.ostensiblePort, conf.isSSL, true);
                addRowToNetworkStatusTable(newRow);
            } finally {
            }
        }

        @Override()
        public int hashCode() {
            return this.conf.ostensibleHost.hashCode();
        }

        @Override()
        public boolean equals(Object o) {
            if (o instanceof SuspendedHost) {
                SuspendedHost s = (SuspendedHost) o;
                if (!s.conf.ostensibleHost.equals(this.conf.ostensibleHost)) {
                    return false;
                }
                if (s.conf.serverFlag != this.conf.serverFlag) {
                    return false;
                }
                return true;
            }
            return false;
        }
    }
}
