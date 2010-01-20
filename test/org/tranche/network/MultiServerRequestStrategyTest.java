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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class MultiServerRequestStrategyTest extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.setTestingManualNetworkStatusTable(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.setTestingManualNetworkStatusTable(false);
    }

    final static StatusTableRow[] testRowsAllOnline = {
        new StatusTableRow("ardvark", 443, false, true),
        new StatusTableRow("batman", 443, false, true),
        new StatusTableRow("catwoman", 443, false, true),
        new StatusTableRow("donkey.kong", 1500, false, true),
        new StatusTableRow("eeyor", 443, false, true),
        new StatusTableRow("freddy.kruger", 443, true, true),
        new StatusTableRow("gunter.grass", 1500, true, true),
        new StatusTableRow("hero", 1500, false, true),
        new StatusTableRow("invisible.man", 443, false, true),
        new StatusTableRow("jack.kerouac", 443, false, true),
        new StatusTableRow("karate", 443, false, true),
        new StatusTableRow("ligand.binding.site", 443, false, true),
    };
    final static StatusTableRow[] testRowsSomeOffline = {
        new StatusTableRow("ardvark", 443, false, true),
        new StatusTableRow("batman", 443, false, true),
        new StatusTableRow("catwoman", 443, false, false),
        new StatusTableRow("donkey.kong", 1500, false, true),
        new StatusTableRow("eeyor", 443, false, true),
        new StatusTableRow("freddy.kruger", 443, true, true),
        new StatusTableRow("gunter.grass", 1500, true, false),
        new StatusTableRow("hero", 1500, false, false),
        new StatusTableRow("invisible.man", 443, false, true),
        new StatusTableRow("jack.kerouac", 443, false, true),
        new StatusTableRow("karate", 443, false, true),
        new StatusTableRow("ligand.binding.site", 443, false, false),
    };

    /**
     * <p>Helper method.</p>
     * <p>Since we have test flag on to manually control, set the table and perform a few quick assertions.</p>
     * @return
     */
    private StatusTable getTestTableAllOnline() {

        StatusTable table = NetworkUtil.getStatus();
        table.clear();

        for (StatusTableRow row : testRowsAllOnline) {
            table.setRow(row);
        }

        assertEquals("Expecting certain number of rows.", testRowsAllOnline.length, table.getRows().size());

        List<StatusTableRow> foundRows = table.getRows();
        List<String> foundURLs = table.getURLs();

        // Set the core servers
        NetworkUtil.setStartupServerURLs(foundURLs);

        for (int i = 0; i < testRowsAllOnline.length; i++) {

            // Test row
            StatusTableRow testRow = testRowsAllOnline[i];
            StatusTableRow foundRow = foundRows.get(i);
            assertEquals("Should be equal.", testRow.getHost(), foundRow.getHost());
            assertEquals("Should be equal.", testRow.getPort(), foundRow.getPort());
            assertEquals("Should be equal.", testRow.isOnline(), foundRow.isOnline());
            assertEquals("Should be equal.", testRow.isSSL(), foundRow.isSSL());

            // Test url
            String testURL = IOUtil.createURL(testRow.getHost(), testRow.getPort(), testRow.isSSL());
            String foundURL = foundURLs.get(i);
            assertEquals("Should be equal.", testURL, foundURL);
        }
        return table;
    }

    /**
     * <p>Helper method.</p>
     * <p>Since we have test flag on to manually control, set the table and perform a few quick assertions.</p>
     * @return
     */
    private StatusTable getTestTableSomeOffline() {

        StatusTable table = NetworkUtil.getStatus();
        table.clear();

        for (StatusTableRow row : testRowsSomeOffline) {
            table.setRow(row);
        }

        assertEquals("Expecting certain number of rows.", testRowsSomeOffline.length, table.getRows().size());

        List<StatusTableRow> foundRows = table.getRows();
        List<String> foundURLs = table.getURLs();

        // Set the core servers
        NetworkUtil.setStartupServerURLs(foundURLs);

        for (int i = 0; i < testRowsSomeOffline.length; i++) {

            // Test row
            StatusTableRow testRow = testRowsSomeOffline[i];
            StatusTableRow foundRow = foundRows.get(i);
            assertEquals("Should be equal.", testRow.getHost(), foundRow.getHost());
            assertEquals("Should be equal.", testRow.getPort(), foundRow.getPort());
            assertEquals("Should be equal.", testRow.isOnline(), foundRow.isOnline());
            assertEquals("Should be equal.", testRow.isSSL(), foundRow.isSSL());

            // Test url
            String testURL = IOUtil.createURL(testRow.getHost(), testRow.getPort(), testRow.isSSL());
            String foundURL = foundURLs.get(i);
            assertEquals("Should be equal.", testURL, foundURL);
        }
        return table;
    }

//    /**
//     * <p>Twelve servers on network, all online. Request each server. Should already know how this works out!</p>
//     * @throws java.lang.Exception
//     */
//    public void testRequestForEveryServerAllOnline() throws Exception {
//
//        StatusTable table = getTestTableAllOnline();
//
//        Set<String> allServerHosts = new HashSet();
//        allServerHosts.addAll(table.getHosts());
//
//        // Used to find best solution(s)
//        Collection<MultiServerRequestStrategy> strategies = new HashSet();
//
//        // Create strategry from each server's perspective. Make sure partitioning is complete and not redundant.
//        for (String host : table.getHosts()) {
//            MultiServerRequestStrategy s = MultiServerRequestStrategy.create(host, allServerHosts);
//
//            // Add this strategy to complete collection
//            strategies.add(s);
//
//            assertEquals("All hosts should be available.", 0, s.getUnfulfillableHosts().size());
//            assertTrue("Expected that depth maximum of three, instead: " + s.getDepth(), s.getDepth() <= 3);
//
//            List<String> hostsCoveredByStrategy = new LinkedList();
//            for (String key : s.getPartitionsMap().keySet()) {
//                hostsCoveredByStrategy.addAll(s.getPartitionsMap().get(key));
//            }
//            hostsCoveredByStrategy.add(s.getHostReceivingRequest());
//            assertEquals("Should have same elements are table--no duplicates!", table.getHosts().size(), hostsCoveredByStrategy.size());
//            for (String next : table.getHosts()) {
//                assertTrue("Verifying contents equivalent.", hostsCoveredByStrategy.contains(next));
//            }
//            for (String next : hostsCoveredByStrategy) {
//                assertTrue("Verifying contents equivalent.", table.getHosts().contains(next));
//            }
//        }
//
//        Collection<MultiServerRequestStrategy> bestStrategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(allServerHosts);
//        int bestDepth = bestStrategies.toArray(new MultiServerRequestStrategy[0])[0].getDepth();
////        System.out.println("DEBUG> Best strategy count: " + bestStrategies.size() + " (Depth: " + bestDepth + ")");
//
//        // Let's verify the above is correct.
//        int bestDepthVerified = MultiServerRequestStrategy.INFINITE_DEPTH;
//        for (MultiServerRequestStrategy s : strategies) {
//            if (bestDepthVerified > s.getDepth()) {
//                bestDepthVerified = s.getDepth();
//            }
//        }
//
//        assertEquals("Should be same depths--verifying results.", bestDepth, bestDepthVerified);
//
//        Collection<MultiServerRequestStrategy> bestStrategiesVerified = new HashSet();
//        for (MultiServerRequestStrategy s : strategies) {
//            if (s.getDepth() == bestDepthVerified) {
//                bestStrategiesVerified.add(s);
//            }
//        }
//
//        assertEquals("Should be same sets.", bestStrategies.size(), bestStrategiesVerified.size());
//
//        for (MultiServerRequestStrategy s : bestStrategies) {
//            assertTrue("Should be same sets, but didn't find strategy for: " + s.getHostReceivingRequest(), bestStrategiesVerified.contains(s));
//        }
//
//        for (MultiServerRequestStrategy s : bestStrategiesVerified) {
//            assertTrue("Should be same sets.", bestStrategies.contains(s));
//        }
//    }

//    /**
//     * <p>Twelve servers on network, all online. Request some servers. Should already know how this works out!</p>
//     * @throws java.lang.Exception
//     */
//    public void testRequestForSomeServersAllOnline() throws Exception {
//
//        StatusTable table = getTestTableAllOnline();
//
//        // We want six online servers. Nothing should be offline.
//        List<String> allOnlineServers = new LinkedList();
//
//        for (String host : table.getHosts()) {
//            if (table.getRow(host).isOnline()) {
//                allOnlineServers.add(host);
//            } else {
//                fail("Nothing should be offline, but found: " + host);
//            }
//        }
//
//        Collections.shuffle(allOnlineServers);
//
//        Set<String> someServerHosts = new HashSet();
//
//        for (int i = 0; i < 6; i++) {
//            someServerHosts.add(allOnlineServers.get(i));
//        }
//
//        assertEquals("Should be six servers.", 6, someServerHosts.size());
//
//        // Used to find best solution(s)
//        Collection<MultiServerRequestStrategy> strategies = new HashSet();
//
//        // Create strategry from each server's perspective. Make sure partitioning is complete and not redundant.
//        for (String host : table.getHosts()) {
//            MultiServerRequestStrategy s = MultiServerRequestStrategy.create(host, someServerHosts);
//
//            // Add this strategy to complete collection
//            strategies.add(s);
//
//            assertEquals("All hosts should be available.", 0, s.getUnfulfillableHosts().size());
//            assertTrue("Expected that depth maximum of five, instead: " + s.getDepth(), s.getDepth() <= 5);
//        }
//
//        Collection<MultiServerRequestStrategy> bestStrategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(someServerHosts);
//        System.out.println("Best strategy count: " + bestStrategies.size() + " (Depth: " + bestStrategies.toArray(new MultiServerRequestStrategy[0])[0].getDepth() + ")");
//    }

//    /**
//     * <p>Twelve servers on network, four are offline. Request each server. Should already know how this works out!</p>
//     * @throws java.lang.Exception
//     */
//    public void testRequestForEveryServerSomeOffline() throws Exception {
//
//        StatusTable table = getTestTableSomeOffline();
//
//        Set<String> allServerHosts = new HashSet();
//        allServerHosts.addAll(table.getHosts());
//
//        // Used to find best solution(s)
//        Collection<MultiServerRequestStrategy> strategies = new HashSet();
//
//        // Create strategry from each server's perspective. Make sure partitioning is complete and not redundant.
//        for (String host : table.getHosts()) {
//
//            MultiServerRequestStrategy s = MultiServerRequestStrategy.create(host, allServerHosts);
//
//            if (!NetworkUtil.getStatus().getRow(host).isOnline()) {
//                assertEquals("If server is offline, everything should be unfulfillable.", allServerHosts.size(), s.getUnfulfillableHosts().size());
//                continue;
//            }
//
//            assertEquals("Four hosts should be unavailable.", 4, s.getUnfulfillableHosts().size());
//            assertTrue("Expected that depth maximum of three, instead: " + s.getDepth(), s.getDepth() <= 3);
//
//            List<String> hostsCoveredByStrategy = new LinkedList();
//            for (String key : s.getPartitionsMap().keySet()) {
//                hostsCoveredByStrategy.addAll(s.getPartitionsMap().get(key));
//            }
//            hostsCoveredByStrategy.addAll(s.getUnfulfillableHosts());
//            hostsCoveredByStrategy.add(s.getHostReceivingRequest());
//            assertEquals("Should have same elements are table--no duplicates!", table.getHosts().size(), hostsCoveredByStrategy.size());
//            for (String next : table.getHosts()) {
//                assertTrue("Verifying contents equivalent.", hostsCoveredByStrategy.contains(next));
//            }
//            for (String next : hostsCoveredByStrategy) {
//                assertTrue("Verifying contents equivalent.", table.getHosts().contains(next));
//            }
//        }
//
//        Collection<MultiServerRequestStrategy> bestStrategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(allServerHosts);
////        System.out.println("DEBUG> Best strategy count: " + bestStrategies.size() + " (Depth: " + bestStrategies.toArray(new MultiServerRequestStrategy[0])[0].getDepth() + ")");
//    }

//    /**
//     * <p>Twelve servers on network, four are offline. Request some servers. Should already know how this works out!</p>
//     * @throws java.lang.Exception
//     */
//    public void testRequestForSomeServersSomeOffline() throws Exception {
//
//        StatusTable table = getTestTableSomeOffline();
//
//        // We want five online and two offline
//        List<String> allOnlineServers = new LinkedList();
//        List<String> allOfflineServers = new LinkedList();
//
//        for (String host : table.getHosts()) {
//            if (table.getRow(host).isOnline()) {
//                allOnlineServers.add(host);
//            } else {
//                allOfflineServers.add(host);
//            }
//        }
//
//        Collections.shuffle(allOnlineServers);
//        Collections.shuffle(allOfflineServers);
//
//        Set<String> someServerHosts = new HashSet();
//
//        for (int i = 0; i < 5; i++) {
//            someServerHosts.add(allOnlineServers.get(i));
//        }
//        for (int i = 0; i < 2; i++) {
//            someServerHosts.add(allOfflineServers.get(i));
//        }
//
//        assertEquals("Should be seven servers.", 7, someServerHosts.size());
//
//        // Used to find best solution(s)
//        Collection<MultiServerRequestStrategy> strategies = new HashSet();
//
//        // Create strategry from each server's perspective. Make sure partitioning is complete and not redundant.
//        for (String host : table.getHosts()) {
//
//            MultiServerRequestStrategy s = MultiServerRequestStrategy.create(host, someServerHosts);
//
//            // Add this strategy to complete collection
//            strategies.add(s);
//
//            if (!NetworkUtil.getStatus().getRow(host).isOnline()) {
//                assertEquals("If server is offline, everything should be unfulfillable.", someServerHosts.size(), s.getUnfulfillableHosts().size());
//                continue;
//            }
//
//            assertEquals("Two hosts should be unavailable.", 2, s.getUnfulfillableHosts().size());
//            assertTrue("Expected that depth maximum of three, instead: " + s.getDepth(), s.getDepth() <= 3);
//        }
//
//        Collection<MultiServerRequestStrategy> bestStrategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(someServerHosts);
//        System.out.println("Best strategy count: " + bestStrategies.size() + " (Depth: " + bestStrategies.toArray(new MultiServerRequestStrategy[0])[0].getDepth() + ")");
//    }

    /**
     * <p>Need to make sure that removing the host that receives the propagated request doesn't change the strategy. Needs to be deterministic.</p>
     * @throws java.lang.Exception
     */
    public void testRemovingHostFromTargetCollectionDoesNotChangeStrategyAllOnline() throws Exception {
        StatusTable table = getTestTableAllOnline();

        Set<String> allServerHosts = new HashSet();
        allServerHosts.addAll(table.getHosts());

        int hostCount = 0;


        // Create strategry from each server's perspective. Make sure partitioning is complete and not redundant.
        for (String host : table.getHosts()) {

            assertTrue("Target collection should include this host.", allServerHosts.contains(host));
            MultiServerRequestStrategy s1 = MultiServerRequestStrategy.create(host, allServerHosts);

            Set<String> otherHosts = new HashSet();
            otherHosts.addAll(allServerHosts);
            otherHosts.remove(host);

            assertEquals("Should only be missing one server.", allServerHosts.size() - 1, otherHosts.size());
            assertFalse("Target collection should include this host.", otherHosts.contains(host));

            MultiServerRequestStrategy s2 = MultiServerRequestStrategy.create(host, otherHosts);

            assertEquals("Should be equal", s1.getDepth(), s2.getDepth());
            assertEquals("Should be equal", s1.getUnfulfillableHosts().size(), s2.getUnfulfillableHosts().size());
            assertEquals("Should be equal", s1.getPartitionsMap().size(), s2.getPartitionsMap().size());

            int keyCount = 0;

            for (String key : s1.getPartitionsMap().keySet()) {
                assertTrue("Should contain", s2.getPartitionsMap().keySet().contains(key));

                Set<String> partition1 = s1.getPartitionsMap().get(key);
                Set<String> partition2 = s2.getPartitionsMap().get(key);

                assertEquals("Should be equal for " + host + " using connected host: " + key, partition1.size(), partition2.size());

                for (String nextHost : partition1) {
                    assertTrue("Should contain for " + key, partition2.contains(nextHost));
                }

                keyCount++;
            }
            hostCount++;
        }
    }

    /**
     * <p>Need to make sure that removing the host that receives the propagated request doesn't change the strategy. Needs to be deterministic.</p>
     * @throws java.lang.Exception
     */
    public void testRemovingHostFromTargetCollectionDoesNotChangeStrategySomeOffline() throws Exception {
        StatusTable table = getTestTableSomeOffline();

        Set<String> allServerHosts = new HashSet();
        allServerHosts.addAll(table.getHosts());

        int hostCount = 0;

        // Create strategry from each server's perspective. Make sure partitioning is complete and not redundant.
        for (String host : table.getHosts()) {

            assertTrue("Target collection should include this host.", allServerHosts.contains(host));
            MultiServerRequestStrategy s1 = MultiServerRequestStrategy.create(host, allServerHosts);

            Set<String> otherHosts = new HashSet();
            otherHosts.addAll(allServerHosts);
            otherHosts.remove(host);

            assertEquals("Should only be missing one server.", allServerHosts.size() - 1, otherHosts.size());
            assertFalse("Target collection should include this host.", otherHosts.contains(host));

            MultiServerRequestStrategy s2 = MultiServerRequestStrategy.create(host, otherHosts);

            assertEquals("Should be equal", s1.getDepth(), s2.getDepth());

            // Only check if online. Otherwise, these numbers will be different since one less server to retrieve that will fail
            if (NetworkUtil.getStatus().getRow(host).isOnline()) {
                assertEquals("Should be equal", s1.getUnfulfillableHosts().size(), s2.getUnfulfillableHosts().size());
            } else {
                assertEquals("Should be equal", s1.getUnfulfillableHosts().size()-1, s2.getUnfulfillableHosts().size());
            }

            assertEquals("Should be equal", s1.getPartitionsMap().size(), s2.getPartitionsMap().size());

            int keyCount = 0;

            for (String key : s1.getPartitionsMap().keySet()) {
                assertTrue("Should contain", s2.getPartitionsMap().keySet().contains(key));

                Set<String> partition1 = s1.getPartitionsMap().get(key);
                Set<String> partition2 = s2.getPartitionsMap().get(key);

                assertEquals("Should be equal for " + host + " using connected host: " + key, partition1.size(), partition2.size());

                for (String nextHost : partition1) {
                    assertTrue("Should contain for " + key, partition2.contains(nextHost));
                }

                keyCount++;
            }
            hostCount++;
        }
    }
}
