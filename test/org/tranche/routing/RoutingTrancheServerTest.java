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
package org.tranche.routing;

import java.util.HashSet;
import java.util.Set;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.exceptions.ChunkDoesNotBelongException;
import org.tranche.exceptions.NoHostProvidedException;
import org.tranche.exceptions.NoMatchingServersException;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.flatfile.NonceMap;
import org.tranche.network.ConnectionUtil;
import org.tranche.server.*;
import org.tranche.hash.*;
import org.tranche.hash.span.HashSpan;
import org.tranche.logs.activity.*;
import org.tranche.network.StatusTable;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class RoutingTrancheServerTest extends TrancheTestCase {

    final static String HOST1 = "ardvark.org";
    final static String HOST2 = "batman.org";
    final static String HOST3 = "catwoman.org";
    final static String HOST4 = "darwin.edu";
    final static String HOST5 = "edgar.com";
    final static String HOST6 = "friday.gov";
    final static String[] ALL_HOSTS = {HOST1, HOST2, HOST3, HOST4, HOST5};
    final boolean wasTestingTargetHashSpan;
    final boolean wasTestingHealingThread;

    public RoutingTrancheServerTest() {
        wasTestingTargetHashSpan = TestUtil.isTestingTargetHashSpan();
        wasTestingHealingThread = TestUtil.isTestingHashSpanFixingThread();
    }

    @Override()
    public void setUp() {
        TestUtil.setTestingTargetHashSpan(false);
        TestUtil.setTestingHashSpanFixingThread(false);
    }

    @Override()
    public void tearDown() {
        TestUtil.setTestingTargetHashSpan(wasTestingTargetHashSpan);
        TestUtil.setTestingHashSpanFixingThread(wasTestingHealingThread);
    }
    /**
     * <p>Very simple test: should be able to determine which servers are managed.</p>
     * @throws java.lang.Exception
     */
    public void testCanSetDataServersToManage() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testCanSetDataServersToManage");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST6, 1505, "127.0.0.1", true, true, false));


        try {
            testNetwork.start();

            RoutingTrancheServer routingServer = testNetwork.getRoutingTrancheServer(HOST6);
            Configuration routingConfig = routingServer.getConfiguration();

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

        } finally {
            testNetwork.stop();
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testSetConfiguration() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testSetConfiguration");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            String name = "foo";
            String value = "bar";
            routingConfig.setValue(name, value);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            routingConfig = routingServer.getConfiguration();
            assertEquals("Expecting certain key/value pair in configuration.", value, routingServer.getConfiguration().getValue(name));

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    } // testSetConfiguration

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testGetDataFailsOutsideHashSpan() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testGetDataFailsOutsideHashSpan");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // ----------------------------------------------------------------------------------
            // Want three narrow hash spans 
            // ----------------------------------------------------------------------------------

            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);

            // Add the hash span to server
            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);

            Configuration config = ffts.getConfiguration();
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(new HashSpan(startingHash, endingHash));
            config.setHashSpans(hashSpans);
            config.setTargetHashSpans(hashSpans);

            assertEquals("Should be one hash span.", 1, hashSpans.size());
            assertEquals("Should be one hash span.", 1, config.getHashSpans().size());

            ffts.setConfiguration(config);

            assertEquals("Should be one hash span.", 1, ffts.getConfiguration().getHashSpans().size());

            // Build next hash span
            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);

            // Add the hash span to server
            ffts = testNetwork.getFlatFileTrancheServer(HOST2);

            config = ffts.getConfiguration();
            hashSpans = new HashSet();
            hashSpans.add(new HashSpan(startingHash, endingHash));
            config.setHashSpans(hashSpans);
            config.setTargetHashSpans(hashSpans);

            assertEquals("Should be one hash span.", 1, hashSpans.size());

            ffts.setConfiguration(config);

            assertEquals("Should be one hash span.", 1, ffts.getConfiguration().getHashSpans().size());

            // Build next hash span
            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);

            // Add the hash span to server
            ffts = testNetwork.getFlatFileTrancheServer(HOST3);

            config = ffts.getConfiguration();
            hashSpans = new HashSet();
            hashSpans.add(new HashSpan(startingHash, endingHash));
            config.setHashSpans(hashSpans);

            assertEquals("Should be one hash span.", 1, hashSpans.size());

            ffts.setConfiguration(config);

            assertEquals("Should be one hash span.", 1, ffts.getConfiguration().getHashSpans().size());

            // Create a hash that will not appear in above hash spans
            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);

            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());
            for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans()) {
                assertFalse("Shouldn't contain hash: " + endingHash, hs.contains(endingHash));
            }

            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());
            for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans()) {
                assertFalse("Shouldn't contain hash: " + endingHash, hs.contains(endingHash));
            }

            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());
            for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans()) {
                assertFalse("Shouldn't contain hash: " + endingHash, hs.contains(endingHash));
            }

            // ----------------------------------------------------------------------------------
            // Ask for chunk. Should get NoMatchingServersException
            // ----------------------------------------------------------------------------------
            PropagationReturnWrapper prw = IOUtil.getData(routingServer, endingHash, false);

            assertEquals("Only expecting one server.", 1, prw.getErrors().size());
            assertEquals("Expecting certain exception.", PropagationExceptionWrapper.class, prw.getErrors().toArray(new PropagationExceptionWrapper[0])[0].getClass());
        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testGetData() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testGetData");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Might be a little overlap. However, want to make sure every hash is covered.
            for (int i = 0; i < 100; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                int coveredCount = 0;
                if (firstHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (secondHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (thirdHashSpan.contains(h)) {
                    coveredCount++;
                }

                assertNotSame("Better be at least one hash span that covers.", 0, coveredCount);
            }

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create a random data chunk. Put on appropriate server
            // -------------------------------------------------------------------------------------------
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            if (firstHashSpan.contains(hash)) {
                IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST1), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
            } else if (secondHashSpan.contains(hash)) {
                IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST2), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
            } else if (thirdHashSpan.contains(hash)) {
                IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST3), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
            } else {
                fail("Could not find a hash span that covers chunk: " + hash);
            }

            PropagationReturnWrapper wrapper = IOUtil.getData(routingServer, hash, false);
//            assertTrue("Should be a single-dimensional byte array.", wrapper.isByteArraySingleDimension());
            assertTrue("Should be a two-dimensional byte array.", wrapper.isByteArrayDoubleDimension());
            assertEquals("Should be no errors.", 0, wrapper.getErrors().size());
            byte[] returnedBytes = ((byte[][]) wrapper.getReturnValueObject())[0];
            assertEquals("Should be correct length.", chunk.length, returnedBytes.length);

            for (int i = 0; i < chunk.length; i++) {
                assertEquals("Bytes should be equal at index #" + i, chunk[i], returnedBytes[i]);
            }

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    } // testGetData

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testGetDataFailsChunkNotAvailable() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testGetDataFailsChunkNotAvailable");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Might be a little overlap. However, want to make sure every hash is covered.
            for (int i = 0; i < 100; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                int coveredCount = 0;
                if (firstHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (secondHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (thirdHashSpan.contains(h)) {
                    coveredCount++;
                }

                assertNotSame("Better be at least one hash span that covers.", 0, coveredCount);
            }

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create a random data chunk. Put on appropriate server
            // -------------------------------------------------------------------------------------------

            BigHash hash = DevUtil.getRandomBigHash();

            boolean matched = false;

            if (firstHashSpan.contains(hash)) {
                matched = true;
            } else if (secondHashSpan.contains(hash)) {
                matched = true;
            } else if (thirdHashSpan.contains(hash)) {
                matched = true;
            }

            if (!matched) {
                fail("Hash not in one of three hash spans!?!: " + hash);
            }

            PropagationReturnWrapper prw = IOUtil.getData(routingServer, hash, false);

            assertTrue("Should be at least one error", prw.getErrors().size() > 0);

            // If test, should say void (since object was null) Let's just verify byte array is null
            byte[] chunk = ((byte[][]) prw.getReturnValueObject())[0];
            assertNull("Since chunk doesn't exist, should be null.", chunk);

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    } // testGetDataFailsChunkNotAvailable

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testGetMetaDataFailsOutsideHashSpan() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testGetMetaDataFailsOutsideHashSpan");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // ----------------------------------------------------------------------------------
            // Want three narrow hash spans 
            // ----------------------------------------------------------------------------------

            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);

            // Add the hash span to server
            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);

            Configuration config = ffts.getConfiguration();
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(new HashSpan(startingHash, endingHash));
            config.setHashSpans(hashSpans);

            assertEquals("Should be one hash span.", 1, hashSpans.size());
            assertEquals("Should be one hash span.", 1, config.getHashSpans().size());

            ffts.setConfiguration(config);

            assertEquals("Should be one hash span.", 1, ffts.getConfiguration().getHashSpans().size());

            // Build next hash span
            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);

            // Add the hash span to server
            ffts = testNetwork.getFlatFileTrancheServer(HOST2);

            config = ffts.getConfiguration();
            hashSpans = new HashSet();
            hashSpans.add(new HashSpan(startingHash, endingHash));
            config.setHashSpans(hashSpans);

            assertEquals("Should be one hash span.", 1, hashSpans.size());

            ffts.setConfiguration(config);

            assertEquals("Should be one hash span.", 1, ffts.getConfiguration().getHashSpans().size());

            // Build next hash span
            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);

            // Add the hash span to server
            ffts = testNetwork.getFlatFileTrancheServer(HOST3);

            config = ffts.getConfiguration();
            hashSpans = new HashSet();
            hashSpans.add(new HashSpan(startingHash, endingHash));
            config.setHashSpans(hashSpans);

            assertEquals("Should be one hash span.", 1, hashSpans.size());

            ffts.setConfiguration(config);

            assertEquals("Should be one hash span.", 1, ffts.getConfiguration().getHashSpans().size());

            // Create a hash that will not appear in above hash spans
            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);

            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());
            for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans()) {
                assertFalse("Shouldn't contain hash: " + endingHash, hs.contains(endingHash));
            }

            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());
            for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans()) {
                assertFalse("Shouldn't contain hash: " + endingHash, hs.contains(endingHash));
            }

            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());
            for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans()) {
                assertFalse("Shouldn't contain hash: " + endingHash, hs.contains(endingHash));
            }

            // ----------------------------------------------------------------------------------
            // Ask for chunk. Should get NoMatchingServersException
            // ----------------------------------------------------------------------------------
            PropagationReturnWrapper prw = IOUtil.getMetaData(routingServer, endingHash, false);

            assertEquals("Only expecting one server.", 1, prw.getErrors().size());
            assertEquals("Expecting certain exception.", PropagationExceptionWrapper.class, prw.getErrors().toArray(new PropagationExceptionWrapper[0])[0].getClass());

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testGetMetaDataFailsChunkNotAvailable() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testGetMetaDataFailsChunkNotAvailable");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Might be a little overlap. However, want to make sure every hash is covered.
            for (int i = 0; i < 100; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                int coveredCount = 0;
                if (firstHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (secondHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (thirdHashSpan.contains(h)) {
                    coveredCount++;
                }

                assertNotSame("Better be at least one hash span that covers.", 0, coveredCount);
            }

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create a random data chunk. Put on appropriate server
            // -------------------------------------------------------------------------------------------

            BigHash hash = DevUtil.getRandomBigHash();

            boolean matched = false;

            if (firstHashSpan.contains(hash)) {
                matched = true;
            } else if (secondHashSpan.contains(hash)) {
                matched = true;
            } else if (thirdHashSpan.contains(hash)) {
                matched = true;
            }

            if (!matched) {
                fail("Hash not in one of three hash spans!?!: " + hash);
            }

            PropagationReturnWrapper prw = IOUtil.getMetaData(routingServer, hash, false);

            assertTrue("Should be at least one error", prw.getErrors().size() > 0);

            // If test, should say void (since object was null) Let's just verify byte array is null
            byte[] chunk = ((byte[][]) prw.getReturnValueObject())[0];
            assertNull("Since chunk doesn't exist, should be null.", chunk);

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    } // testGetMetaDataFailsChunkNotAvailable

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testGetMetaData() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testGetMetaData");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Might be a little overlap. However, want to make sure every hash is covered.
            for (int i = 0; i < 100; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                int coveredCount = 0;
                if (firstHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (secondHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (thirdHashSpan.contains(h)) {
                    coveredCount++;
                }

                assertNotSame("Better be at least one hash span that covers.", 0, coveredCount);
            }

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create a random data chunk. Put on appropriate server
            // -------------------------------------------------------------------------------------------
            byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            if (firstHashSpan.contains(hash)) {
                IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST1), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                assertTrue("Should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
            } else if (secondHashSpan.contains(hash)) {
                IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST2), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                assertTrue("Should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
            } else if (thirdHashSpan.contains(hash)) {
                IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST3), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                assertTrue("Should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
            } else {
                fail("Could not find a hash span that covers chunk: " + hash);
            }

            PropagationReturnWrapper wrapper = IOUtil.getMetaData(routingServer, hash, false);
            assertTrue("Should be a two-dimensional byte array.", wrapper.isByteArrayDoubleDimension());
            assertEquals("Should be no errors.", 0, wrapper.getErrors().size());
            byte[] returnedBytes = ((byte[][]) wrapper.getReturnValueObject())[0];
            assertEquals("Should be correct length.", chunk.length, returnedBytes.length);

            for (int i = 0; i < chunk.length; i++) {
                assertEquals("Bytes should be equal at index #" + i, chunk[i], returnedBytes[i]);
            }

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    } // testGetMetaData

    public void testGetConfiguration() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testGetConfiguration");
        
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());
            routingServer.setConfiguration(routingConfig);

            Configuration verifyConfiguration = IOUtil.getConfiguration(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Configuration objects should be equal.", routingConfig, verifyConfiguration);
        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    } // testGetConfiguration

    public void testBatchGetData() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testBatchGetData");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Might be a little overlap. However, want to make sure every hash is covered.
            for (int i = 0; i < 100; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                int coveredCount = 0;
                if (firstHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (secondHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (thirdHashSpan.contains(h)) {
                    coveredCount++;
                }

                assertNotSame("Better be at least one hash span that covers.", 0, coveredCount);
            }

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create five small chunks and put them on the appropriate server
            // -------------------------------------------------------------------------------------------
            Set<BigHash> hashes = new HashSet();
            for (int i = 0; i < 5; i++) {
                byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
                BigHash hash = new BigHash(chunk);

                // Highly unlikely
                if (hashes.contains(hash)) {
                    continue;
                }
                hashes.add(hash);

                if (firstHashSpan.contains(hash)) {
                    IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST1), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                    assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else if (secondHashSpan.contains(hash)) {
                    IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST2), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else if (thirdHashSpan.contains(hash)) {
                    IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST3), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else {
                    fail("Could not find a hash span that covers chunk: " + hash);
                }
            }

            PropagationReturnWrapper wrapper = routingServer.getData(hashes.toArray(new BigHash[0]), false);
            assertTrue("Should be a two-dimensional byte array.", wrapper.isByteArrayDoubleDimension());
            assertEquals("Should be no errors.", 0, wrapper.getErrors().size());
            byte[][] returnedBytes = (byte[][]) wrapper.getReturnValueObject();

            for (int i = 0; i < returnedBytes.length; i++) {
                byte[] chunk = returnedBytes[i];
                BigHash verifyHash = new BigHash(chunk);
                assertTrue("Should have hash.", hashes.contains(verifyHash));
            }

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    public void testBatchGetDataFailsChunksNotAvailable() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testBatchGetDataFailsChunksNotAvailable");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Might be a little overlap. However, want to make sure every hash is covered.
            for (int i = 0; i < 100; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                int coveredCount = 0;
                if (firstHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (secondHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (thirdHashSpan.contains(h)) {
                    coveredCount++;
                }

                assertNotSame("Better be at least one hash span that covers.", 0, coveredCount);
            }

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create five small chunks and put them on the appropriate server
            // -------------------------------------------------------------------------------------------
            final int availableChunkCount = RandomUtil.getInt(10) + 1;
            Set<BigHash> hashes = new HashSet();
            Set<BigHash> availableHashes = new HashSet(0);
            Set<BigHash> unavailableHashes = new HashSet(0);
            for (int i = 0; i < availableChunkCount; i++) {
                // 50K maximum
                byte[] chunk = DevUtil.createRandomDataChunk(RandomUtil.getInt(1024 * 50) + 1);
                BigHash hash = new BigHash(chunk);

                // Highly unlikely
                if (hashes.contains(hash)) {
                    continue;
                }
                hashes.add(hash);
                availableHashes.add(hash);

                if (firstHashSpan.contains(hash)) {
                    IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST1), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                    assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else if (secondHashSpan.contains(hash)) {
                    IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST2), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else if (thirdHashSpan.contains(hash)) {
                    IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST3), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else {
                    fail("Could not find a hash span that covers chunk: " + hash);
                }
            }

            final int unavailableChunkCount = RandomUtil.getInt(10) + 1;
            int count = 0;

            // Add certain number of chunks not available
            while (count < unavailableChunkCount) {
                // 50K maximum
                byte[] chunk = DevUtil.createRandomDataChunk(RandomUtil.getInt(1024 * 50) + 1);
                BigHash hash = new BigHash(chunk);

                // Highly unlikely
                if (hashes.contains(hash)) {
                    continue;
                }
                hashes.add(hash);
                unavailableHashes.add(hash);
                count++;
            }

            assertEquals("Expecting certain number of unavailable chunks.", unavailableChunkCount, unavailableHashes.size());
            assertEquals("Expecting certain number of available chunks.", availableChunkCount, availableHashes.size());
            assertEquals("Expecting certain number of total hashes.", unavailableChunkCount + availableChunkCount, hashes.size());

            final BigHash[] hashesArr = hashes.toArray(new BigHash[0]);
            PropagationReturnWrapper wrapper = routingServer.getData(hashesArr, false);
            assertTrue("Should be a two-dimensional byte array.", wrapper.isByteArrayDoubleDimension());
            assertTrue("Must be at least one error.", 1 <= wrapper.getErrors().size());

            for (PropagationExceptionWrapper exWrap : wrapper.getErrors()) {
                assertFalse("Shouldn't be a ChunkDoesNotBelongException since all the hash spans cover every hash.", exWrap.exception instanceof ChunkDoesNotBelongException);
            }

            byte[][] returnedBytes = (byte[][]) wrapper.getReturnValueObject();

            for (int i = 0; i < returnedBytes.length; i++) {
                byte[] chunk = returnedBytes[i];
                BigHash hash = hashesArr[i];

                // If chunk is null, find the corresponding has. It should be unavailable
                if (chunk == null) {
                    assertTrue("Should have hash.", unavailableHashes.contains(hash));
                } else {

                    BigHash verifyHash = new BigHash(chunk);
                    assertTrue("Should have hash.", availableHashes.contains(verifyHash));
                    assertEquals("Should be equal.", hash, verifyHash);
                }
            }

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    public void testBatchGetDataFailsOutsideHashSpans() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testBatchGetDataFailsOutsideHashSpans");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // ----------------------------------------------------------------------------------
            // Want three narrow hash spans 
            // ----------------------------------------------------------------------------------

            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);

            // Add the hash span to server
            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);

            Configuration config = ffts.getConfiguration();
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(new HashSpan(startingHash, endingHash));
            config.setHashSpans(hashSpans);

            assertEquals("Should be one hash span.", 1, hashSpans.size());
            assertEquals("Should be one hash span.", 1, config.getHashSpans().size());

            ffts.setConfiguration(config);

            assertEquals("Should be one hash span.", 1, ffts.getConfiguration().getHashSpans().size());

            // Build next hash span
            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);

            // Add the hash span to server
            ffts = testNetwork.getFlatFileTrancheServer(HOST2);

            config = ffts.getConfiguration();
            hashSpans = new HashSet();
            hashSpans.add(new HashSpan(startingHash, endingHash));
            config.setHashSpans(hashSpans);

            assertEquals("Should be one hash span.", 1, hashSpans.size());

            ffts.setConfiguration(config);

            assertEquals("Should be one hash span.", 1, ffts.getConfiguration().getHashSpans().size());

            // Build next hash span
            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);

            // Add the hash span to server
            ffts = testNetwork.getFlatFileTrancheServer(HOST3);

            config = ffts.getConfiguration();
            hashSpans = new HashSet();
            hashSpans.add(new HashSpan(startingHash, endingHash));
            config.setHashSpans(hashSpans);

            assertEquals("Should be one hash span.", 1, hashSpans.size());

            ffts.setConfiguration(config);

            assertEquals("Should be one hash span.", 1, ffts.getConfiguration().getHashSpans().size());

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create five small chunks and put them on the appropriate server
            // -------------------------------------------------------------------------------------------
            final int availableChunkCount = RandomUtil.getInt(10) + 1;
            Set<BigHash> hashes = new HashSet();
            Set<BigHash> availableHashes = new HashSet(0);
            Set<BigHash> unavailableHashes = new HashSet(0);
            while (availableHashes.size() < availableChunkCount) {
                // 50K maximum
                byte[] chunk = DevUtil.createRandomDataChunk(RandomUtil.getInt(1024 * 50) + 1);
                BigHash hash = new BigHash(chunk);

                // Highly unlikely
                if (hashes.contains(hash)) {
                    continue;
                }

                // Skip chunk unless fits in one of the hash spans!
                boolean contains = false;
                FlatFileTrancheServer fftsToUse = null;
                for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans()) {
                    if (hs.contains(hash)) {
                        contains = true;
                        fftsToUse = testNetwork.getFlatFileTrancheServer(HOST1);
                        break;
                    }
                }
                if (!contains) {
                    for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans()) {
                        if (hs.contains(hash)) {
                            contains = true;
                            fftsToUse = testNetwork.getFlatFileTrancheServer(HOST2);
                            break;
                        }
                    }
                }
                if (!contains) {
                    for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans()) {
                        if (hs.contains(hash)) {
                            contains = true;
                            fftsToUse = testNetwork.getFlatFileTrancheServer(HOST3);
                            break;
                        }
                    }
                }

                // Keep going until create enough chunks that are covered
                if (!contains) {
                    continue;
                }

                hashes.add(hash);
                availableHashes.add(hash);
                IOUtil.setData(fftsToUse, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                if (fftsToUse == testNetwork.getFlatFileTrancheServer(HOST1)) {
                    assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else if (fftsToUse == testNetwork.getFlatFileTrancheServer(HOST2)) {
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else if (fftsToUse == testNetwork.getFlatFileTrancheServer(HOST3)) {
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else {
                    fail("Could not find a hash span that covers chunk: " + hash);
                }
            }

            final int unavailableChunkCount = RandomUtil.getInt(10) + 1;
            int count = 0;

            // Add certain number of chunks not available
            CREATE_UNAVAILABLE_CHUNKS:
            while (count < unavailableChunkCount) {
                // 50K maximum
                byte[] chunk = DevUtil.createRandomDataChunk(RandomUtil.getInt(1024 * 50) + 1);
                BigHash hash = new BigHash(chunk);

                // Highly unlikely
                if (hashes.contains(hash)) {
                    continue;
                }

                // Skip chunk if in a hash span. Want a hash that isn't covered.
                for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans()) {
                    if (hs.contains(hash)) {
                        continue CREATE_UNAVAILABLE_CHUNKS;
                    }
                }
                for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans()) {
                    if (hs.contains(hash)) {
                        continue CREATE_UNAVAILABLE_CHUNKS;
                    }
                }
                for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans()) {
                    if (hs.contains(hash)) {
                        continue CREATE_UNAVAILABLE_CHUNKS;
                    }
                }

                hashes.add(hash);
                unavailableHashes.add(hash);
                count++;
            }

            assertEquals("Expecting certain number of unavailable chunks.", unavailableChunkCount, unavailableHashes.size());
            assertEquals("Expecting certain number of available chunks.", availableChunkCount, availableHashes.size());
            assertEquals("Expecting certain number of total hashes.", unavailableChunkCount + availableChunkCount, hashes.size());

            final BigHash[] hashesArr = hashes.toArray(new BigHash[0]);
            PropagationReturnWrapper wrapper = routingServer.getData(hashesArr, false);
            assertTrue("Should be a two-dimensional byte array.", wrapper.isByteArrayDoubleDimension());
            assertEquals("Should know the number of errors.", unavailableHashes.size(), wrapper.getErrors().size());

            for (PropagationExceptionWrapper pew : wrapper.getErrors()) {
                System.out.println("DEBUG> "+pew.exception.getClass().getSimpleName()+"<"+pew.host+">: "+pew.exception.getMessage());
                pew.exception.printStackTrace(System.out);
            }
            
            int countNoMatchingServers = 0;
            for (PropagationExceptionWrapper exWrap : wrapper.getErrors()) {
                if (exWrap.exception instanceof NoMatchingServersException) {
                    countNoMatchingServers++;
                } 
            }
            assertEquals("Expecting certain number of unavailable chunks.", unavailableHashes.size(), countNoMatchingServers);

            byte[][] returnedBytes = (byte[][]) wrapper.getReturnValueObject();

            for (int i = 0; i < returnedBytes.length; i++) {
                byte[] chunk = returnedBytes[i];
                BigHash hash = hashesArr[i];

                // If chunk is null, find the corresponding has. It should be unavailable
                if (chunk == null) {
                    assertTrue("Should have hash.", unavailableHashes.contains(hash));
                } else {

                    BigHash verifyHash = new BigHash(chunk);
                    assertTrue("Should have hash.", availableHashes.contains(verifyHash));
                    assertEquals("Should be equal.", hash, verifyHash);
                }
            }

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    public void testBatchGetMetaDataFailsChunksNotAvailable() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testBatchGetMetaDataFailsChunksNotAvailable");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Might be a little overlap. However, want to make sure every hash is covered.
            for (int i = 0; i < 100; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                int coveredCount = 0;
                if (firstHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (secondHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (thirdHashSpan.contains(h)) {
                    coveredCount++;
                }

                assertNotSame("Better be at least one hash span that covers.", 0, coveredCount);
            }

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create five small chunks and put them on the appropriate server
            // -------------------------------------------------------------------------------------------
            final int availableChunkCount = RandomUtil.getInt(10) + 1;
            Set<BigHash> hashes = new HashSet();
            Set<BigHash> availableHashes = new HashSet(0);
            Set<BigHash> unavailableHashes = new HashSet(0);
            for (int i = 0; i < availableChunkCount; i++) {

                byte[] chunk = DevUtil.createRandomMetaDataChunk();
                BigHash hash = DevUtil.getRandomBigHash();

                // Highly unlikely
                if (hashes.contains(hash)) {
                    continue;
                }
                hashes.add(hash);
                availableHashes.add(hash);

                if (firstHashSpan.contains(hash)) {
                    IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST1), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                    assertTrue("Should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else if (secondHashSpan.contains(hash)) {
                    IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST2), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                    assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertTrue("Should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else if (thirdHashSpan.contains(hash)) {
                    IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST3), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                    assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertTrue("Should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else {
                    fail("Could not find a hash span that covers chunk: " + hash);
                }
            }

            final int unavailableChunkCount = RandomUtil.getInt(10) + 1;

            // Add certain number of chunks not available
            while (unavailableHashes.size() < unavailableChunkCount) {

                BigHash hash = DevUtil.getRandomBigHash();

                // Highly unlikely
                if (hashes.contains(hash)) {
                    continue;
                }
                hashes.add(hash);
                unavailableHashes.add(hash);
            }

            assertEquals("Expecting certain number of unavailable chunks.", unavailableChunkCount, unavailableHashes.size());
            assertEquals("Expecting certain number of available chunks.", availableChunkCount, availableHashes.size());
            assertEquals("Expecting certain number of total hashes.", unavailableChunkCount + availableChunkCount, hashes.size());

            final BigHash[] hashesArr = hashes.toArray(new BigHash[0]);
            PropagationReturnWrapper wrapper = routingServer.getMetaData(hashesArr, false);
            assertTrue("Should be a two-dimensional byte array.", wrapper.isByteArrayDoubleDimension());
            assertTrue("Must be at least one error.", 1 <= wrapper.getErrors().size());

            for (PropagationExceptionWrapper exWrap : wrapper.getErrors()) {
                assertFalse("Shouldn't be a ChunkDoesNotBelongException since all the hash spans cover every hash.", exWrap.exception instanceof ChunkDoesNotBelongException);
            }

            byte[][] returnedBytes = (byte[][]) wrapper.getReturnValueObject();

            for (int i = 0; i < returnedBytes.length; i++) {
                byte[] chunk = returnedBytes[i];
                BigHash hash = hashesArr[i];

                // If chunk is null, find the corresponding has. It should be unavailable
                if (chunk == null) {
                    assertTrue("Should have hash.", unavailableHashes.contains(hash));
                } else {
                    assertTrue("Should have hash.", availableHashes.contains(hash));
                }
            }

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    public void testBatchGetMetaDataFailsOutsideHashSpans() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testBatchGetMetaDataFailsOutsideHashSpans");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // ----------------------------------------------------------------------------------
            // Want three narrow hash spans 
            // ----------------------------------------------------------------------------------

            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);

            // Add the hash span to server
            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);

            Configuration config = ffts.getConfiguration();
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(new HashSpan(startingHash, endingHash));
            config.setHashSpans(hashSpans);

            assertEquals("Should be one hash span.", 1, hashSpans.size());
            assertEquals("Should be one hash span.", 1, config.getHashSpans().size());

            ffts.setConfiguration(config);

            assertEquals("Should be one hash span.", 1, ffts.getConfiguration().getHashSpans().size());

            // Build next hash span
            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);

            // Add the hash span to server
            ffts = testNetwork.getFlatFileTrancheServer(HOST2);

            config = ffts.getConfiguration();
            hashSpans = new HashSet();
            hashSpans.add(new HashSpan(startingHash, endingHash));
            config.setHashSpans(hashSpans);

            assertEquals("Should be one hash span.", 1, hashSpans.size());

            ffts.setConfiguration(config);

            assertEquals("Should be one hash span.", 1, ffts.getConfiguration().getHashSpans().size());

            // Build next hash span
            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];

            for (int i = 0; i < startingHash.toByteArray().length; i++) {
                endingHashBytes[i] = (byte) (startingHash.toByteArray()[i] + 1);
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);

            // Add the hash span to server
            ffts = testNetwork.getFlatFileTrancheServer(HOST3);

            config = ffts.getConfiguration();
            hashSpans = new HashSet();
            hashSpans.add(new HashSpan(startingHash, endingHash));
            config.setHashSpans(hashSpans);

            assertEquals("Should be one hash span.", 1, hashSpans.size());

            ffts.setConfiguration(config);

            assertEquals("Should be one hash span.", 1, ffts.getConfiguration().getHashSpans().size());

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create five small chunks and put them on the appropriate server
            // -------------------------------------------------------------------------------------------
            final int availableChunkCount = RandomUtil.getInt(10) + 1;
            Set<BigHash> hashes = new HashSet();
            Set<BigHash> availableHashes = new HashSet(0);
            Set<BigHash> unavailableHashes = new HashSet(0);
            while (availableHashes.size() < availableChunkCount) {

                BigHash hash = DevUtil.getRandomBigHash();

                // Highly unlikely
                if (hashes.contains(hash)) {
                    continue;
                }

                // Skip chunk unless fits in one of the hash spans!
                boolean contains = false;
                FlatFileTrancheServer fftsToUse = null;
                for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans()) {
                    if (hs.contains(hash)) {
                        contains = true;
                        fftsToUse = testNetwork.getFlatFileTrancheServer(HOST1);
                        break;
                    }
                }
                if (!contains) {
                    for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans()) {
                        if (hs.contains(hash)) {
                            contains = true;
                            fftsToUse = testNetwork.getFlatFileTrancheServer(HOST2);
                            break;
                        }
                    }
                }
                if (!contains) {
                    for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans()) {
                        if (hs.contains(hash)) {
                            contains = true;
                            fftsToUse = testNetwork.getFlatFileTrancheServer(HOST3);
                            break;
                        }
                    }
                }

                // Keep going until create enough chunks that are covered
                if (!contains) {
                    continue;
                }

                byte[] chunk = DevUtil.createRandomMetaDataChunk();

                hashes.add(hash);
                availableHashes.add(hash);
                IOUtil.setMetaData(fftsToUse, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                if (fftsToUse == testNetwork.getFlatFileTrancheServer(HOST1)) {
                    assertTrue("Should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else if (fftsToUse == testNetwork.getFlatFileTrancheServer(HOST2)) {
                    assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertTrue("Should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else if (fftsToUse == testNetwork.getFlatFileTrancheServer(HOST3)) {
                    assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                    assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                    assertTrue("Should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
                } else {
                    fail("Could not find a hash span that covers chunk: " + hash);
                }
            }

            final int unavailableChunkCount = RandomUtil.getInt(10) + 1;

            // Add certain number of chunks not available
            CREATE_UNAVAILABLE_CHUNKS:
            while (unavailableHashes.size() < unavailableChunkCount) {

                BigHash hash = DevUtil.getRandomBigHash();

                // Highly unlikely
                if (hashes.contains(hash)) {
                    continue;
                }

                // Skip chunk if in a hash span. Want a hash that isn't covered.
                for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans()) {
                    if (hs.contains(hash)) {
                        continue CREATE_UNAVAILABLE_CHUNKS;
                    }
                }
                for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans()) {
                    if (hs.contains(hash)) {
                        continue CREATE_UNAVAILABLE_CHUNKS;
                    }
                }
                for (HashSpan hs : testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans()) {
                    if (hs.contains(hash)) {
                        continue CREATE_UNAVAILABLE_CHUNKS;
                    }
                }

                hashes.add(hash);
                unavailableHashes.add(hash);
            }

            assertEquals("Expecting certain number of unavailable chunks.", unavailableChunkCount, unavailableHashes.size());
            assertEquals("Expecting certain number of available chunks.", availableChunkCount, availableHashes.size());
            assertEquals("Expecting certain number of total hashes.", unavailableChunkCount + availableChunkCount, hashes.size());

            final BigHash[] hashesArr = hashes.toArray(new BigHash[0]);
            PropagationReturnWrapper wrapper = routingServer.getMetaData(hashesArr, false);
            assertTrue("Should be a two-dimensional byte array.", wrapper.isByteArrayDoubleDimension());
            assertEquals("Should know the number of errors.", unavailableHashes.size(), wrapper.getErrors().size());

            int countNoMatchingServers = 0;
            for (PropagationExceptionWrapper exWrap : wrapper.getErrors()) {
                if (exWrap.exception instanceof NoMatchingServersException) {
                    countNoMatchingServers++;
                }
            }
            assertEquals("Expecting certain number of unavailable chunks.", unavailableHashes.size(), countNoMatchingServers);

            byte[][] returnedBytes = (byte[][]) wrapper.getReturnValueObject();

            for (int i = 0; i < returnedBytes.length; i++) {
                byte[] chunk = returnedBytes[i];
                BigHash hash = hashesArr[i];

                // If chunk is null, find the corresponding has. It should be unavailable
                if (chunk == null) {
                    assertTrue("Should have hash.", unavailableHashes.contains(hash));
                } else {
                    assertTrue("Should have hash.", availableHashes.contains(hash));
                }
            }

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    /**
     * @throws java.lang.Exception
     */
    public void testGetNetworkStatus() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testGetNetworkStatus");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();

            StatusTable st = routingServer.getNetworkStatusPortion(GetNetworkStatusItem.RETURN_ALL, GetNetworkStatusItem.RETURN_ALL);
            assertNotNull("Better not be null.", st);

            assertEquals("Expecting five rows.", 5, st.getRows().size());

            assertTrue("Should have.", st.contains(HOST1));
            assertTrue("Should have.", st.contains(HOST2));
            assertTrue("Should have.", st.contains(HOST3));
            assertTrue("Should have.", st.contains(HOST4));
            assertTrue("Should have.", st.contains(HOST5));

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    } // testGetNetworkStatus

    /**
     * @throws java.lang.Exception
     */
    public void testGetNetworkStatusPortion() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testGetNetworkStatusPortion");
        
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();

            // The final value is exclusive, so expecting {HOST2, HOST3, HOST4}
            StatusTable st = routingServer.getNetworkStatusPortion(HOST2, HOST5);
            assertNotNull("Better not be null.", st);

            assertEquals("Expecting three rows.", 3, st.getRows().size());
            assertTrue("Should have.", st.contains(HOST2));
            assertTrue("Should have.", st.contains(HOST3));
            assertTrue("Should have.", st.contains(HOST4));
            assertFalse("Shouldn't have.", st.contains(HOST5));
            assertFalse("Shouldn't have.", st.contains(HOST1));
        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    } // testGetNetworkStatusPortion

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testHasData() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testHasData");
        
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Might be a little overlap. However, want to make sure every hash is covered.
            for (int i = 0; i < 100; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                int coveredCount = 0;
                if (firstHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (secondHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (thirdHashSpan.contains(h)) {
                    coveredCount++;
                }

                assertNotSame("Better be at least one hash span that covers.", 0, coveredCount);
            }

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create a random data chunk. Put on appropriate server
            // -------------------------------------------------------------------------------------------
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            assertFalse("Routing server shouldn't have chunk yet.", IOUtil.hasData(routingServer, hash));

            if (firstHashSpan.contains(hash)) {
                IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST1), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
            } else if (secondHashSpan.contains(hash)) {
                IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST2), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
            } else if (thirdHashSpan.contains(hash)) {
                IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST3), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                assertTrue("Should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
            } else {
                fail("Could not find a hash span that covers chunk: " + hash);
            }

            assertTrue("Routing server should have chunk.", IOUtil.hasData(routingServer, hash));

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    } // testHasData

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testHasMetaData() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testHasMetaData");
        
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Might be a little overlap. However, want to make sure every hash is covered.
            for (int i = 0; i < 100; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                int coveredCount = 0;
                if (firstHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (secondHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (thirdHashSpan.contains(h)) {
                    coveredCount++;
                }

                assertNotSame("Better be at least one hash span that covers.", 0, coveredCount);
            }

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create a random data chunk. Put on appropriate server
            // -------------------------------------------------------------------------------------------
            byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            assertFalse("Routing server shouldn't have chunk yet.", IOUtil.hasMetaData(routingServer, hash));

            if (firstHashSpan.contains(hash)) {
                IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST1), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                assertTrue("Should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
            } else if (secondHashSpan.contains(hash)) {
                IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST2), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                assertTrue("Should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
            } else if (thirdHashSpan.contains(hash)) {
                IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST3), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST2), hash));
                assertTrue("Should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST3), hash));
            } else {
                fail("Could not find a hash span that covers chunk: " + hash);
            }

            assertTrue("Routing server should have chunk.", IOUtil.hasMetaData(routingServer, hash));

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testSetData() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testSetData");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create a random data chunk. Put on appropriate server
            // -------------------------------------------------------------------------------------------
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            assertFalse("Routing server shouldn't have chunk yet.", IOUtil.hasData(routingServer, hash));

            IOUtil.setData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

            assertTrue("Routing server should have chunk.", IOUtil.hasData(routingServer, hash));

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testSetMetaData() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testSetMetaData");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = new RoutingTrancheServer();
            Configuration routingConfig = routingServer.getConfiguration();
            routingConfig.getUsers().add(DevUtil.getDevUser());

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create a random data chunk. Put on appropriate server
            // -------------------------------------------------------------------------------------------
            byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            assertFalse("Routing server shouldn't have chunk yet.", IOUtil.hasMetaData(routingServer, hash));

            IOUtil.setMetaData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

            assertTrue("Routing server should have chunk.", IOUtil.hasMetaData(routingServer, hash));

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testSetDataPropagated() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testSetDataPropagated");
        
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();
            
            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);
            
            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);
            
            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);
            
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull(ts4);
            
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull(ts5);

            routingServer = testNetwork.getRoutingTrancheServer(HOST5);
            assertNotNull("Shouldn't be null.", routingServer);

            Configuration routingConfig = routingServer.getConfiguration();

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Host #4 will be a full hash span
            config = testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration();
            hashSpans = new HashSet();
            hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST4).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration().getHashSpans().size());

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create a random data chunk. Put on appropriate server
            // -------------------------------------------------------------------------------------------
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            assertFalse("Routing server shouldn't have chunk yet.", IOUtil.hasData(routingServer, hash));
            assertFalse("Server shouldn't have chunk yet.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST4), hash));

            String[] hosts = {HOST4, HOST5};

            PropagationReturnWrapper prw = IOUtil.setData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);

            for (PropagationExceptionWrapper pew : prw.getErrors()) {
                System.out.println("DEBUG> "+pew.exception.getClass().getSimpleName()+"<"+pew.host+">: "+pew.exception.getMessage());
                pew.exception.printStackTrace(System.out);
            }
            
            assertEquals("Shouldn't be any exceptions.", 0, prw.getErrors().size());
            assertTrue("Should be void.", prw.isVoid());

            assertTrue("Server should have chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST4), hash));
            assertTrue("Routing server should have chunk.", IOUtil.hasData(routingServer, hash));

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testSetMetaDataPropagated() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testSetMetaDataPropagated");
        
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();
            
            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);
            
            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);
            
            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);
            
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull(ts4);
            
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull(ts5);

            routingServer = testNetwork.getRoutingTrancheServer(HOST5);
            assertNotNull("Shouldn't be null.", routingServer);

            Configuration routingConfig = routingServer.getConfiguration();

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Host #4 will be a full hash span
            config = testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration();
            hashSpans = new HashSet();
            hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST4).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration().getHashSpans().size());

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create a random data chunk. Put on appropriate server
            // -------------------------------------------------------------------------------------------
            byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            assertFalse("Routing server shouldn't have chunk yet.", IOUtil.hasMetaData(routingServer, hash));
            assertFalse("Server shouldn't have chunk yet.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST4), hash));

            String[] hosts = {HOST4, HOST5};

            PropagationReturnWrapper prw = IOUtil.setMetaData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk, hosts);

            assertEquals("Shouldn't be any exceptions.", 0, prw.getErrors().size());
            assertTrue("Should be void.", prw.isVoid());

            assertTrue("Server should have chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST4), hash));
            assertTrue("Routing server should have chunk.", IOUtil.hasMetaData(routingServer, hash));

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    public void testBatchHasData() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testBatchHasData");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = testNetwork.getRoutingTrancheServer(HOST5);
            assertNotNull("Shouldn't be null.", routingServer);

            Configuration routingConfig = routingServer.getConfiguration();

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Host #4 will be a full hash span
            config = testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration();
            hashSpans = new HashSet();
            hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST4).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration().getHashSpans().size());

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create some random chunks. Put on appropriate server
            // -------------------------------------------------------------------------------------------
            Set<BigHash> addedChunks = new HashSet();

            final int chunksToAdd = RandomUtil.getInt(10) + 1;
            final int chunksToNotAdd = RandomUtil.getInt(10) + 1;

            for (int i = 0; i < chunksToAdd; i++) {

                byte[] chunk = DevUtil.createRandomDataChunk(RandomUtil.getInt(1024 * 16) + 1);
                BigHash hash = new BigHash(chunk);

                // Make sure novel. Shouldn't happen.
                if (addedChunks.contains(hash)) {
                    i--;
                    continue;
                }

                assertFalse("Routing server shouldn't have chunk yet.", IOUtil.hasData(routingServer, hash));

                IOUtil.setData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                addedChunks.add(hash);

                assertTrue("Routing server should have chunk.", IOUtil.hasData(routingServer, hash));
            }

            assertEquals("Should know how many hashes in collection.", chunksToAdd, addedChunks.size());

            Set<BigHash> notAddedChunks = new HashSet();
            for (int i = 0; i < chunksToNotAdd; i++) {
                BigHash hash = DevUtil.getRandomBigHash();

                // Highly unlikely
                if (addedChunks.contains(hash) || notAddedChunks.contains(hash)) {
                    i--;
                    continue;
                }

                notAddedChunks.add(hash);
            }

            assertEquals("Should know how many hashes in collection.", chunksToNotAdd, notAddedChunks.size());

            Set<BigHash> allChunks = new HashSet();
            allChunks.addAll(addedChunks);
            allChunks.addAll(notAddedChunks);

            BigHash[] hashes = allChunks.toArray(new BigHash[0]);
            boolean[] hasVals = routingServer.hasData(hashes);

            assertEquals("Returned result array should be same length as parameters.", hashes.length, hasVals.length);

            for (int i = 0; i < hasVals.length; i++) {
                boolean has = hasVals[i];
                BigHash hash = hashes[i];

                if (has) {
                    assertTrue("Should contain.", addedChunks.contains(hash));
                    assertFalse("Shouldn't contain.", notAddedChunks.contains(hash));
                } else {
                    assertTrue("Should contain.", notAddedChunks.contains(hash));
                    assertFalse("Shouldn't contain.", addedChunks.contains(hash));
                }
            }

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    public void testBatchHasMetaData() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testBatchHasMetaData");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            routingServer = testNetwork.getRoutingTrancheServer(HOST5);
            assertNotNull("Shouldn't be null.", routingServer);

            Configuration routingConfig = routingServer.getConfiguration();

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));

            // Create three hash spans and assign to servers
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Host #4 will be a full hash span
            config = testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration();
            hashSpans = new HashSet();
            hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST4).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration().getHashSpans().size());

            testNetwork.updateNetwork();

            // -------------------------------------------------------------------------------------------
            // Create some random chunks. Put on appropriate server
            // -------------------------------------------------------------------------------------------
            Set<BigHash> addedChunks = new HashSet();

            final int chunksToAdd = RandomUtil.getInt(10) + 1;
            final int chunksToNotAdd = RandomUtil.getInt(10) + 1;

            for (int i = 0; i < chunksToAdd; i++) {

                BigHash hash = DevUtil.getRandomBigHash();

                // Make sure novel. Shouldn't happen.
                if (addedChunks.contains(hash)) {
                    i--;
                    continue;
                }

                byte[] chunk = DevUtil.createRandomBigMetaDataChunk();

                assertFalse("Routing server shouldn't have chunk yet.", IOUtil.hasMetaData(routingServer, hash));

                IOUtil.setMetaData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                addedChunks.add(hash);

                assertTrue("Routing server should have chunk.", IOUtil.hasMetaData(routingServer, hash));
            }

            assertEquals("Should know how many hashes in collection.", chunksToAdd, addedChunks.size());

            Set<BigHash> notAddedChunks = new HashSet();
            for (int i = 0; i < chunksToNotAdd; i++) {
                BigHash hash = DevUtil.getRandomBigHash();

                // Highly unlikely
                if (addedChunks.contains(hash) || notAddedChunks.contains(hash)) {
                    i--;
                    continue;
                }

                notAddedChunks.add(hash);
            }

            assertEquals("Should know how many hashes in collection.", chunksToNotAdd, notAddedChunks.size());

            Set<BigHash> allChunks = new HashSet();
            allChunks.addAll(addedChunks);
            allChunks.addAll(notAddedChunks);

            BigHash[] hashes = allChunks.toArray(new BigHash[0]);
            boolean[] hasVals = routingServer.hasMetaData(hashes);

            assertEquals("Returned result array should be same length as parameters.", hashes.length, hasVals.length);

            for (int i = 0; i < hasVals.length; i++) {
                boolean has = hasVals[i];
                BigHash hash = hashes[i];

                if (has) {
                    assertTrue("Should contain.", addedChunks.contains(hash));
                    assertFalse("Shouldn't contain.", notAddedChunks.contains(hash));
                } else {
                    assertTrue("Should contain.", notAddedChunks.contains(hash));
                    assertFalse("Shouldn't contain.", addedChunks.contains(hash));
                }
            }

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    public void testGetNonce() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testGetNonce");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            RoutingTrancheServer routingServer = testNetwork.getRoutingTrancheServer(HOST1);
            assertNotNull(routingServer);
            byte[] nonce = IOUtil.getNonce(routingServer);

            assertNotNull("Should not be null.", nonce);
            assertEquals("Should have correct number of bytes.", NonceMap.NONCE_BYTES, nonce.length);
        } finally {
            testNetwork.stop();
        }
    }

    public void testGetNoncePropagatedSome() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testGetNoncePropagatedSome");
        getNoncePropagated(true);
    }

    public void testGetNoncePropagatedAll() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testGetNoncePropagatedAll");
        getNoncePropagated(false);
    }

    public void getNoncePropagated(boolean isUseSome) throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        try {

            testNetwork.start();

            // Connect. Since tests all come from same host, must connect -- or some servers will not be accessible
            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);

            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull(ts4);

            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull(ts5);

            RoutingTrancheServer routingServer = testNetwork.getRoutingTrancheServer(HOST5);
            assertNotNull("Shouldn't be null.", routingServer);

            Configuration routingConfig = routingServer.getConfiguration();

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));

            if (isUseSome) {
                String managedServers = HOST1 + "," + HOST2;
                routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

                IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

                assertEquals("Should know how many managed servers.", 2, routingServer.getManagedServers().size());
                assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
                assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
                assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
                assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            } else {
                String managedServers = HOST1 + "," + HOST2 + "," + HOST3 + "," + HOST4;
                routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

                IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

                assertEquals("Should know how many managed servers.", 4, routingServer.getManagedServers().size());
                assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
                assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
                assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
                assertTrue("Host should be managed.", routingServer.isManagedServer(HOST4));
            }

            final int count = 1;
            PropagationReturnWrapper returnWrapper = routingServer.getNonces(ALL_HOSTS, count);

            for (PropagationExceptionWrapper pew : returnWrapper.getErrors()) {
                System.out.println("DEBUG> " + pew.exception.getClass().getSimpleName() + ": " + pew.exception.getMessage());
            }

            assertTrue("Should know return type.", returnWrapper.isByteArrayTripleDimension());

            assertEquals("Shouldn't be any errors.", 0, returnWrapper.getErrors().size());

            byte[][][] nonces = ((byte[][][]) returnWrapper.getReturnValueObject());

            assertEquals("Should know how many hosts returned nonces.", ALL_HOSTS.length, nonces.length);


            for (int host = 0; host < ALL_HOSTS.length; host++) {
                for (int copy = 0; copy < count; copy++) {
                    assertEquals("Should know how many bytes each nonce contains.", NonceMap.NONCE_BYTES, nonces[host][copy].length);
                }
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testGetNonces() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testGetNonces");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();
            RoutingTrancheServer routingServer = testNetwork.getRoutingTrancheServer(HOST1);
            final int count = RandomUtil.getInt(10) + 1;
            byte[][] nonces = IOUtil.getNonces(routingServer, count);

            assertNotNull("Should not be null.", nonces);
            assertEquals("Should know how many nonces got back.", count, nonces.length);
            for (int i = 0; i < count; i++) {
                assertEquals("Each nonce have correct number of bytes.", NonceMap.NONCE_BYTES, nonces[i].length);
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testGetNoncesPropagatedSome() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testGetNoncesPropagatedSome");
        getNoncesPropagated(true);
    }

    public void testGetNoncesPropagatedAll() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testGetNoncesPropagatedAll");
        getNoncesPropagated(false);
    }

    public void getNoncesPropagated(boolean isUseSome) throws Exception {
                
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            // Connect. Since tests all come from same host, must connect -- or some servers will not be accessible
            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);

            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull(ts4);

            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull(ts5);

            routingServer = testNetwork.getRoutingTrancheServer(HOST5);
            assertNotNull("Shouldn't be null.", routingServer);

            Configuration routingConfig = routingServer.getConfiguration();

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));

            if (isUseSome) {
                String managedServers = HOST1 + "," + HOST2;
                routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

                IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

                assertEquals("Should know how many managed servers.", 2, routingServer.getManagedServers().size());
                assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
                assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
                assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
                assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            } else {
                String managedServers = HOST1 + "," + HOST2 + "," + HOST3 + "," + HOST4;
                routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

                IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

                assertEquals("Should know how many managed servers.", 4, routingServer.getManagedServers().size());
                assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
                assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
                assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
                assertTrue("Host should be managed.", routingServer.isManagedServer(HOST4));
            }

            int count = RandomUtil.getInt(10) + 1;

            PropagationReturnWrapper returnWrapper = routingServer.getNonces(ALL_HOSTS, count);

            assertTrue("Should know return type.", returnWrapper.isByteArrayTripleDimension());

            for (PropagationExceptionWrapper pew : returnWrapper.getErrors()) {
                System.out.println("DEBUG> " + pew.exception.getClass().getSimpleName() + ": " + pew.exception.getMessage());
            }

            assertEquals("Shouldn't be any errors.", 0, returnWrapper.getErrors().size());

            byte[][][] nonces = (byte[][][]) returnWrapper.getReturnValueObject();

            assertEquals("Should know how many hosts returned nonces.", ALL_HOSTS.length, nonces.length);

            for (int i = 0; i < ALL_HOSTS.length; i++) {
                assertEquals("Should know how many nonces each host returns.", count, nonces[i].length);
                for (int j = 0; j < count; j++) {
                    assertEquals("Should know how many bytes each nonce contains.", NonceMap.NONCE_BYTES, nonces[i][j].length);
                }
            }

        } finally {
            testNetwork.stop();
        }
    }

    public void testDeleteDataWithAuthoritySet() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testDeleteDataWithAuthoritySet");
        testDelete(true, false);
    }

    public void testDeleteDataWithoutAuthoritySet() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testDeleteDataWithoutAuthoritySet");
        try {
            testDelete(false, false);
            fail("Should have thrown an exception since routing server cannot delete from data servers without an authority.");
        } catch (Exception e) {
            // Expected
        }
    }

    public void testDeleteMetaDataWithAuthoritySet() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testDeleteMetaDataWithAuthoritySet");
        testDelete(true, true);
    }

    public void testDeleteMetaDataWithoutAuthoritySet() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testDeleteMetaDataWithoutAuthoritySet");
        try {
            testDelete(false, true);
            fail("Should have thrown an exception since routing server cannot delete from data servers without an authority.");
        } catch (Exception e) {
            // Expected
        }
    }

    /**
     * 
     * @param isSetAuthority If set to true, should pass. If set to false, should throw exception!
     * @throws java.lang.Exception
     */
    public void testDelete(boolean isSetAuthority, boolean isMetaData) throws Exception {

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST6, 1505, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            TrancheServer rts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(rts1);

            TrancheServer rts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(rts2);

            TrancheServer rts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(rts3);

            TrancheServer rts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull(rts4);

            TrancheServer rts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull(rts5);

            TrancheServer rts6 = ConnectionUtil.connectHost(HOST6, true);
            assertNotNull(rts6);

            routingServer = testNetwork.getRoutingTrancheServer(HOST6);

//            if (isSetAuthority) {
//                routingServer.setAuthCert(DevUtil.getDevAuthority());
//                routingServer.setAuthPrivateKey(DevUtil.getDevPrivateKey());
//            }

            if (!isSetAuthority) {
                routingServer.setAuthCert(null);
                routingServer.setAuthPrivateKey(null);
            }

            Configuration routingConfig = routingServer.getConfiguration();

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST6));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST6));

            // Set users
            testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().addUser(DevUtil.getRoutingTrancheServerUser());
            testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().addUser(DevUtil.getRoutingTrancheServerUser());
            testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().addUser(DevUtil.getRoutingTrancheServerUser());

            // ---------------------------------------------------------------------------------------------
            // Create three partial hash spans and assign to managed servers. The other two servers get full
            // hash spans
            // ---------------------------------------------------------------------------------------------
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            config.setTargetHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
//            testNetwork.getFlatFileTrancheServer(HOST1).saveConfiguration();
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());
            assertEquals("Should know hash span.", firstHashSpan, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            config.setTargetHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
//            testNetwork.getFlatFileTrancheServer(HOST2).saveConfiguration();
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());
            assertEquals("Should know hash span.", secondHashSpan, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            config.setTargetHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
//            testNetwork.getFlatFileTrancheServer(HOST3).saveConfiguration();
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());
            assertEquals("Should know hash span.", thirdHashSpan, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            // Might be a little overlap. However, want to make sure every hash is covered.
            for (int i = 0; i < 100; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                int coveredCount = 0;
                if (firstHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (secondHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (thirdHashSpan.contains(h)) {
                    coveredCount++;
                }

                assertNotSame("Better be at least one hash span that covers.", 0, coveredCount);
            }

            // Create full hash span
            HashSpan full = new HashSpan(HashSpan.FIRST, HashSpan.LAST);
            hashSpans = new HashSet();
            hashSpans.add(full);

            // Set full hash span to server four
            config = testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration();
            config.setHashSpans(hashSpans);
            config.setTargetHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST4).setConfiguration(config);
//            testNetwork.getFlatFileTrancheServer(HOST4).saveConfiguration();
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration().getHashSpans().size());
            assertEquals("Should know hash span.", full, testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);
            // Set full hash span to server five
            config = testNetwork.getFlatFileTrancheServer(HOST5).getConfiguration();
            config.setHashSpans(hashSpans);
            config.setTargetHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST5).setConfiguration(config);
//            testNetwork.getFlatFileTrancheServer(HOST5).saveConfiguration();
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST5).getConfiguration().getHashSpans().size());
            assertEquals("Should know hash span.", full, testNetwork.getFlatFileTrancheServer(HOST5).getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            testNetwork.updateNetwork();

            // Create a random chunk
            byte[] chunk = null;
            BigHash hash = null;

            if (isMetaData) {
                chunk = DevUtil.createRandomMetaDataChunk();
                hash = DevUtil.getRandomBigHash();

                assertFalse("Shouldn't have chunk yet.", IOUtil.hasMetaData(routingServer, hash));
                assertFalse("Shouldn't have chunk yet.", IOUtil.hasMetaData(rts4, hash));
                assertFalse("Shouldn't have chunk yet.", IOUtil.hasMetaData(rts5, hash));

                String[] hosts = {HOST5, HOST6};
                IOUtil.setMetaData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk, hosts);

                assertTrue("Should have chunk.", IOUtil.hasMetaData(routingServer, hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(rts4, hash));
                assertTrue("Should have chunk.", IOUtil.hasMetaData(rts5, hash));

                PropagationReturnWrapper prw = IOUtil.deleteMetaData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash);

                if (prw.isAnyErrors()) {
                    String msg = "*** Terminating (meta data deletion) due to " + prw.getErrors().size() + " error(s) ***";
                    for (PropagationExceptionWrapper pew : prw.getErrors()) {
                        System.out.println("DEBUG> - " + pew.exception.getClass().getSimpleName() + "<" + pew.host + ">: " + pew.exception.getMessage());
                        pew.exception.printStackTrace(System.out);
                    }
                    throw new Exception(msg);
                }

                // Any servers not managed by routing server should be unaffected
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(rts4, hash));
                assertTrue("Should have chunk.", IOUtil.hasMetaData(rts5, hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(routingServer, hash));
            } else {
                chunk = DevUtil.createRandomDataChunkVariableSize();
                hash = new BigHash(chunk);

                assertFalse("Shouldn't have chunk yet.", IOUtil.hasData(routingServer, hash));
                assertFalse("Shouldn't have chunk yet.", IOUtil.hasData(rts4, hash));
                assertFalse("Shouldn't have chunk yet.", IOUtil.hasData(rts5, hash));

                String[] hosts = {HOST5, HOST6};
                IOUtil.setData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);

                assertTrue("Should have chunk.", IOUtil.hasData(routingServer, hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(rts4, hash));
                assertTrue("Should have chunk.", IOUtil.hasData(rts5, hash));

                PropagationReturnWrapper prw = IOUtil.deleteData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash);

                if (prw.isAnyErrors()) {
                    String msg = "*** Terminating (data deletion) due to " + prw.getErrors().size() + " error(s) ***";
                    for (PropagationExceptionWrapper pew : prw.getErrors()) {
                        System.out.println("DEBUG> - " + pew.exception.getClass().getSimpleName() + "<" + pew.host + ">: " + pew.exception.getMessage());
                        pew.exception.printStackTrace(System.out);
                    }
                    throw new Exception(msg);
                }

                // Any servers not managed by routing server should be unaffected
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(rts4, hash));
                assertTrue("Should have chunk.", IOUtil.hasData(rts5, hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(routingServer, hash));
            }

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    public void testDeleteDataPropagated() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testDeleteDataPropagated");
        deletePropagated(false);
    }

    public void testDeleteMetaDataPropagated() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testDeleteMetaDataPropagated");
        deletePropagated(true);
    }

    public void deletePropagated(boolean isMetaData) throws Exception {

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST6, 1505, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;

        try {
            testNetwork.start();

            TrancheServer rts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(rts1);

            TrancheServer rts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(rts2);

            TrancheServer rts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(rts3);

            TrancheServer rts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull(rts4);

            TrancheServer rts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull(rts5);

            TrancheServer rts6 = ConnectionUtil.connectHost(HOST6, true);
            assertNotNull(rts6);

            routingServer = testNetwork.getRoutingTrancheServer(HOST6);
            routingServer.setAuthCert(DevUtil.getDevAuthority());
            routingServer.setAuthPrivateKey(DevUtil.getDevPrivateKey());

            Configuration routingConfig = routingServer.getConfiguration();

            assertEquals("Shouldn't be any managed servers.", 0, routingServer.getManagedServers().size());
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST1));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST2));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            String managedServers = HOST1 + "," + HOST2 + "," + HOST3;
            routingConfig.setValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST, managedServers);

            IOUtil.setConfiguration(routingServer, routingConfig, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertEquals("Should know how many managed servers.", 3, routingServer.getManagedServers().size());
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST1));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST2));
            assertTrue("Host should be managed.", routingServer.isManagedServer(HOST3));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST4));
            assertFalse("Host should not be managed.", routingServer.isManagedServer(HOST5));

            // Set users
            testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().addUser(DevUtil.getRoutingTrancheServerUser());
            testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().addUser(DevUtil.getRoutingTrancheServerUser());
            testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().addUser(DevUtil.getRoutingTrancheServerUser());

            // ---------------------------------------------------------------------------------------------
            // Create three partial hash spans and assign to managed servers. The other two servers get full
            // hash spans
            // ---------------------------------------------------------------------------------------------
            BigHash startingHash = HashSpan.FIRST;
            byte[] endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = -42;
            }
            BigHash endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan firstHashSpan = new HashSpan(startingHash, endingHash);
            Set<HashSpan> hashSpans = new HashSet();
            hashSpans.add(firstHashSpan);
            Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHashBytes = new byte[startingHash.toByteArray().length];
            for (int i = 0; i < endingHashBytes.length; i++) {
                endingHashBytes[i] = 43;
            }
            endingHash = BigHash.createFromBytes(endingHashBytes);
            HashSpan secondHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(secondHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST2).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST2).getConfiguration().getHashSpans().size());

            startingHash = endingHash;
            endingHash = HashSpan.LAST;
            HashSpan thirdHashSpan = new HashSpan(startingHash, endingHash);
            hashSpans = new HashSet();
            hashSpans.add(thirdHashSpan);
            config = testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST3).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST3).getConfiguration().getHashSpans().size());

            // Might be a little overlap. However, want to make sure every hash is covered.
            for (int i = 0; i < 100; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                int coveredCount = 0;
                if (firstHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (secondHashSpan.contains(h)) {
                    coveredCount++;
                }
                if (thirdHashSpan.contains(h)) {
                    coveredCount++;
                }

                assertNotSame("Better be at least one hash span that covers.", 0, coveredCount);
            }

            // Create full hash span
            HashSpan full = new HashSpan(HashSpan.FIRST, HashSpan.LAST);
            hashSpans = new HashSet();
            hashSpans.add(full);

            // Set full hash span to server four
            config = testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST4).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST4).getConfiguration().getHashSpans().size());

            // Set full hash span to server five
            config = testNetwork.getFlatFileTrancheServer(HOST5).getConfiguration();
            config.setHashSpans(hashSpans);
            testNetwork.getFlatFileTrancheServer(HOST5).setConfiguration(config);
            assertEquals("Expecting one hash span.", 1, testNetwork.getFlatFileTrancheServer(HOST5).getConfiguration().getHashSpans().size());

            testNetwork.updateNetwork();

            // Create a random chunk
            byte[] chunk = null;
            BigHash hash = null;

            if (isMetaData) {
                chunk = DevUtil.createRandomMetaDataChunk();
                hash = DevUtil.getRandomBigHash();

                assertFalse("Shouldn't have chunk yet.", IOUtil.hasMetaData(routingServer, hash));
                assertFalse("Shouldn't have chunk yet.", IOUtil.hasMetaData(rts4, hash));
                assertFalse("Shouldn't have chunk yet.", IOUtil.hasMetaData(rts5, hash));

                String[] hostsToSet = {HOST4, HOST5, HOST6};
                IOUtil.setMetaData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk, hostsToSet);

                assertTrue("Should have chunk.", IOUtil.hasMetaData(rts4, hash));
                assertTrue("Should have chunk.", IOUtil.hasMetaData(rts5, hash));
                assertTrue("Should have chunk.", IOUtil.hasMetaData(routingServer, hash));

                String[] hostsToDelete = {HOST4, HOST6};

                IOUtil.deleteMetaData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, hostsToDelete);

                // Any servers not managed by routing server should be unaffected
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(routingServer, hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasMetaData(rts4, hash));
                assertTrue("Should have chunk.", IOUtil.hasMetaData(rts5, hash));
            } else {
                chunk = DevUtil.createRandomDataChunkVariableSize();
                hash = new BigHash(chunk);

                assertFalse("Shouldn't have chunk yet.", IOUtil.hasData(routingServer, hash));
                assertFalse("Shouldn't have chunk yet.", IOUtil.hasData(rts4, hash));
                assertFalse("Shouldn't have chunk yet.", IOUtil.hasData(rts5, hash));

                String[] hostsToSet = {HOST4, HOST5, HOST6};
                IOUtil.setData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hostsToSet);

                assertTrue("Should have chunk.", IOUtil.hasData(routingServer, hash));
                assertTrue("Should have chunk.", IOUtil.hasData(rts4, hash));
                assertTrue("Should have chunk.", IOUtil.hasData(rts5, hash));

                String[] hostsToDelete = {HOST4, HOST6};

                IOUtil.deleteData(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, hostsToDelete);

                // Any servers not managed by routing server should be unaffected
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(rts4, hash));
                assertTrue("Should have chunk.", IOUtil.hasData(rts5, hash));
                assertFalse("Shouldn't have chunk.", IOUtil.hasData(routingServer, hash));
            }

        } finally {
            IOUtil.safeClose(routingServer);
            testNetwork.stop();
        }
    }

    public void testGetActivityLog() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testGetActivityLog");
        
        RoutingTrancheServer routingServer = null;
        try {
            routingServer = new RoutingTrancheServer();

            Activity[] activities = routingServer.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.ANY);
            assertEquals("Should be zero.", 0, activities.length);
        } finally {
            IOUtil.safeClose(routingServer);
        }
    }

    public void testGetActivityLogMask() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testGetActivityLogMask");
        
        RoutingTrancheServer routingServer = null;
        try {
            routingServer = new RoutingTrancheServer();

            Activity[] activities = routingServer.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.SET_DATA);
            assertEquals("Should be zero.", 0, activities.length);
        } finally {
            IOUtil.safeClose(routingServer);
        }
    }

    public void testGetActivityLogCount() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testGetActivityLogCount");
        
        RoutingTrancheServer routingServer = null;
        try {
            routingServer = new RoutingTrancheServer();

            assertEquals("Should be zero.", 0, routingServer.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ANY));
        } finally {
            IOUtil.safeClose(routingServer);
        }
    }

    public void testGetActivityLogCountMask() throws Exception {
        
        TestUtil.printTitle("RoutingTrancheServerTest:testGetActivityLogCountMask");
        
        RoutingTrancheServer routingServer = null;
        try {
            routingServer = new RoutingTrancheServer();

            assertEquals("Should be zero.", 0, routingServer.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.SET_DATA));
        } finally {
            IOUtil.safeClose(routingServer);
        }
    }

    public void testShutdown() throws Exception {

        TestUtil.printTitle("RoutingTrancheServerTest:testShutdown");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        RoutingTrancheServer routingServer = null;
        try {
            testNetwork.start();

            routingServer = testNetwork.getRoutingTrancheServer(HOST1);
            assertNotNull("Should be a routing server at host: " + HOST1, routingServer);

            assertFalse("Shouldn't be closed.", routingServer.isClosed());

            IOUtil.requestShutdown(routingServer, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            assertTrue("Should be closed.", routingServer.isClosed());

        } finally {
            testNetwork.stop();
        }
    }
    
    /**
     * <p>What happens if set chunks to routing server that doesn't have any managed servers?</p>
     * @throws java.lang.Exception
     */
    public void testChunksToRoutingServerWithoutDataServers() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testChunksToRoutingServerWithoutDataServers");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        
        try {
            testNetwork.start();
            
            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            
            RoutingTrancheServer routing = testNetwork.getRoutingTrancheServer(HOST1);
            assertNotNull(routing);
            
            final byte[] dataChunk = DevUtil.createRandomDataChunkVariableSize();
            final BigHash dataHash = new BigHash(dataChunk);
            
            final byte[] metaChunk = DevUtil.createRandomMetaDataChunk();
            final BigHash metaHash = DevUtil.getRandomBigHash();
            
            PropagationReturnWrapper dataReturn = IOUtil.setData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHash, dataChunk);
            
            assertEquals("Expecting one exception.", 1, dataReturn.getErrors().size());
            assertEquals("Expecting certain exception.", NoMatchingServersException.class, dataReturn.getErrors().toArray(new PropagationExceptionWrapper[0])[0].exception.getClass());
            
            PropagationReturnWrapper metaReturn = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaHash, metaChunk);
            
            assertEquals("Expecting one exception.", 1, metaReturn.getErrors().size());
            assertEquals("Expecting certain exception.", NoMatchingServersException.class, metaReturn.getErrors().toArray(new PropagationExceptionWrapper[0])[0].exception.getClass());
            
        } finally {
            testNetwork.stop();
        }
    }
    
    public void testSetDataNoHosts() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testSetDataNoHosts");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            RoutingTrancheServer routing = testNetwork.getRoutingTrancheServer(HOST1);
            assertNotNull(routing);

            final byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            final BigHash hash = new BigHash(chunk);

            final String[] hosts = {};

            // First try directly with RoutingTrancheServer
            try {

                PropagationReturnWrapper prw = IOUtil.setData(routing, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
            
            // Next use RemoteTrancheServer. The exception should get thrown client-side, not wrapped!
            try {
                PropagationReturnWrapper prw = IOUtil.setData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
        } finally {
            testNetwork.stop();
        }
    }
    
    public void testSetMetaDataNoHosts() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testSetMetaDataNoHosts");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            RoutingTrancheServer routing = testNetwork.getRoutingTrancheServer(HOST1);
            assertNotNull(routing);

            final byte[] chunk = DevUtil.createRandomMetaDataChunk(RandomUtil.getInt(5)+1, RandomUtil.getBoolean());
            final BigHash hash = DevUtil.getRandomBigHash();

            final String[] hosts = {};

            // First try directly with RoutingTrancheServer
            try {

                PropagationReturnWrapper prw = IOUtil.setMetaData(routing, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), true, hash, chunk, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
            
            // Next use RemoteTrancheServer. The exception should get thrown client-side, not wrapped!
            try {
                PropagationReturnWrapper prw = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), true, hash, chunk, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
        } finally {
            testNetwork.stop();
        }
    }
    
    public void testGetNoncesNoHosts() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testGetNonces");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            RoutingTrancheServer routing = testNetwork.getRoutingTrancheServer(HOST1);
            assertNotNull(routing);

            final String[] hosts = {};

            // First try directly with RoutingTrancheServer
            try {

                IOUtil.getNonces(routing, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
            
            // Next use RemoteTrancheServer. The exception should get thrown client-side, not wrapped!
            try {
                IOUtil.getNonces(ts1, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
        } finally {
            testNetwork.stop();
        }
    }
    
    public void testDeleteDataNoHosts() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testDeleteDataNoHosts");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            RoutingTrancheServer routing = testNetwork.getRoutingTrancheServer(HOST1);
            assertNotNull(routing);

            final BigHash hash = DevUtil.getRandomBigHash();

            final String[] hosts = {};

            // First try directly with RoutingTrancheServer
            try {

                PropagationReturnWrapper prw = IOUtil.deleteData(routing, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
            
            // Next use RemoteTrancheServer. The exception should get thrown client-side, not wrapped!
            try {

                PropagationReturnWrapper prw = IOUtil.deleteData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
        } finally {
            testNetwork.stop();
        }
    }
    
    public void testDeleteMetaDataNoHosts() throws Exception {
        TestUtil.printTitle("RoutingTrancheServerTest:testDeleteMetaDataNoHosts");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForRoutingServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            RoutingTrancheServer routing = testNetwork.getRoutingTrancheServer(HOST1);
            assertNotNull(routing);

            final BigHash hash = DevUtil.getRandomBigHash();

            final String[] hosts = {};

            // First try directly with RoutingTrancheServer
            try {

                PropagationReturnWrapper prw = IOUtil.deleteMetaData(routing, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
            
            // Next use RemoteTrancheServer. The exception should get thrown client-side, not wrapped!
            try {

                PropagationReturnWrapper prw = IOUtil.deleteMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
        } finally {
            testNetwork.stop();
        }
    }
}
