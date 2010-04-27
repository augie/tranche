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
package org.tranche.remote;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.tranche.commons.Tertiary;
import org.tranche.TrancheServer;
import org.tranche.TrancheServerTest;
import org.tranche.configuration.Configuration;
import org.tranche.commons.DebugUtil;
import org.tranche.exceptions.NoHostProvidedException;
import org.tranche.exceptions.PropagationUnfulfillableHostException;
import org.tranche.exceptions.TodoException;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.flatfile.NonceMap;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.logs.activity.Activity;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.MultiServerRequestStrategy;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.commons.RandomUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;

/**
 * <p>Testing to see if the functionality specified in the TrancheServer interface holds up when exposed remotely.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Brian Maso
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class RemoteTrancheServerTest extends TrancheServerTest {

    /**
     * <p>Used to try to replicate sporadic failures. Set to 1 to run every test only once.</p>
     */
    static final int TEST_COUNT = 1;
    private final boolean wasTestingHealingThread = TestUtil.isTestingHashSpanFixingThread();

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.setTestingHashSpanFixingThread(false);
        DebugUtil.setDebug(RemoteTrancheServer.class, true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.setTestingHashSpanFixingThread(wasTestingHealingThread);
        DebugUtil.setDebug(RemoteTrancheServer.class, false);
    }
    /**
     * Create a set of table rows. Will be used for each test
     */
    static final String HOST1 = "ardvark.org";
    static final String HOST2 = "batman.org";
    static final String HOST3 = "catwoman.org";
    static final String HOST4 = "darwin.edu";
    static final String HOST5 = "edgar.com";

    public void testGetData() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetData()");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {

            testNetwork.start();

            // Create data chunk
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST1, ts1);
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts1, hash));

            String[] hosts = {HOST1};
            PropagationReturnWrapper prwSet = IOUtil.setData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);
            assertTrue("Should be void.", prwSet.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash));

            // Get the chunk
            final BigHash[] hashArr = {hash};
            PropagationReturnWrapper prwGet = ts1.getData(hashArr, false);
            assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
            byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

            assertEquals("Shouldn't have any errors.", 0, prwGet.getErrors().size());
            assertEquals("Should know number of bytes returned.", chunk.length, bytes.length);

            BigHash verifyHash = new BigHash(bytes);
            assertEquals("Should generate correct hash.", hash, verifyHash);

        } finally {
            testNetwork.stop();
        }
    }

    public void testGetDataPropagated() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetDataPropagated()");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        try {

            testNetwork.start();

            // Create data chunk
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            {
                String host = HOST1;
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().clear();
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().clear();
                assertEquals("Shouldn't have a hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Shouldn't have a target hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addHashSpan(HashSpan.FULL);
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addTargetHashSpan(HashSpan.FULL);
                assertEquals("Should have a (full) hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Should have a (full) target hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
            }
            {
                String host = HOST2;
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().clear();
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().clear();
                assertEquals("Shouldn't have a hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Shouldn't have a target hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addHashSpan(HashSpan.FULL);
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addTargetHashSpan(HashSpan.FULL);
                assertEquals("Should have a (full) hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Should have a (full) target hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
            }
            {
                String host = HOST3;
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().clear();
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().clear();
                assertEquals("Shouldn't have a hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Shouldn't have a target hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addHashSpan(HashSpan.FULL);
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addTargetHashSpan(HashSpan.FULL);
                assertEquals("Should have a (full) hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Should have a (full) target hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
            }
            {
                String host = HOST4;
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().clear();
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().clear();
                assertEquals("Shouldn't have a hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Shouldn't have a target hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addHashSpan(HashSpan.FULL);
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addTargetHashSpan(HashSpan.FULL);
                assertEquals("Should have a (full) hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Should have a (full) target hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
            }
            {
                String host = HOST5;
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().clear();
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().clear();
                assertEquals("Shouldn't have a hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Shouldn't have a target hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addHashSpan(HashSpan.FULL);
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addTargetHashSpan(HashSpan.FULL);
                assertEquals("Should have a (full) hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Should have a (full) target hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
            }

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST1, ts1);
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts1, hash));

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST2, ts2);
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts2, hash));

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST3, ts3);
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts3, hash));

            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST4, ts4);
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts4, hash));

            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST5, ts5);
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts5, hash));

            testNetwork.updateNetwork();

            // First test: set to three. All three should have.
            String[] hosts = {HOST1, HOST3, HOST5};
            PropagationReturnWrapper prwSet = IOUtil.setData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);
            assertTrue("Should be void.", prwSet.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts3, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts5, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts2, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts4, hash));

            // Get the chunk from server #1
            {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts1.getData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Shouldn't have any errors.", 0, prwGet.getErrors().size());
                assertEquals("Should know number of bytes returned.", chunk.length, bytes.length);

                BigHash verifyHash = new BigHash(bytes);
                assertEquals("Should generate correct hash.", hash, verifyHash);
            }

            // Get the chunk from server #3
            {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts3.getData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Shouldn't have any errors.", 0, prwGet.getErrors().size());
                assertEquals("Should know number of bytes returned.", chunk.length, bytes.length);

                BigHash verifyHash = new BigHash(bytes);
                assertEquals("Should generate correct hash.", hash, verifyHash);
            }

            // Get the chunk from server #5
            {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts5.getData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Shouldn't have any errors.", 0, prwGet.getErrors().size());
                assertEquals("Should know number of bytes returned.", chunk.length, bytes.length);

                BigHash verifyHash = new BigHash(bytes);
                assertEquals("Should generate correct hash.", hash, verifyHash);
            }

            // Get the chunk from server #2 should fail if not propogate
            try {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts2.getData(hashArr, false);

                assertNotSame("Should have errors.", 0, prwGet.getErrors().size());
            } catch (Exception e) {
                System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }

            // Get the chunk from server #4 should fail if not propogate
            try {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts4.getData(hashArr, false);

                assertNotSame("Should have errors.", 0, prwGet.getErrors().size());
            } catch (Exception e) {
                System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }

            // Get the chunk from server #2 with propogation, and verify that stores copy
            {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts2.getData(hashArr, true);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertNotNull("Chunk shouldn't be null.", bytes);

                for (PropagationExceptionWrapper pew : prwGet.getErrors()) {
                    System.out.println(pew.toString());
                }

                assertEquals("Should know number of bytes returned.", hash.getLength(), bytes.length);

                BigHash verifyHash = new BigHash(bytes);
                assertEquals("Should generate correct hash.", hash, verifyHash);

                assertTrue("Should have data now.", IOUtil.hasData(ts2, hash));
            }

            // Get the chunk from server #4 with propogation, and verify that stores copy
            {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts4.getData(hashArr, true);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertNotNull("Chunk shouldn't be null.", bytes);

                for (PropagationExceptionWrapper pew : prwGet.getErrors()) {
                    System.out.println(pew.toString());
                }

                assertEquals("Should know number of bytes returned.", hash.getLength(), bytes.length);

                BigHash verifyHash = new BigHash(bytes);
                assertEquals("Should generate correct hash.", hash, verifyHash);

                assertTrue("Should have data now.", IOUtil.hasData(ts4, hash));
            }

            // Everything should have chunk now
            assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts2, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts3, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts4, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts5, hash));
        } finally {
            testNetwork.stop();
        }
    }

    public void testGetMetaDataMerge() throws Exception {
        testGetMetaData(true);
    }

    public void testGetMetaDataNoMerge() throws Exception {
        testGetMetaData(false);
    }

    public void testGetMetaData(boolean isMerge) throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetData(" + isMerge + ")");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {

            testNetwork.start();

            // Create data chunk
            byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST1, ts1);
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts1, hash));

            String[] hosts = {HOST1};
            PropagationReturnWrapper prwSet = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), isMerge, hash, chunk, hosts);
            assertTrue("Should be void.", prwSet.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash));

            // Get the chunk
            final BigHash[] hashArr = {hash};
            PropagationReturnWrapper prwGet = ts1.getMetaData(hashArr, false);
            assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
            byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

            assertEquals("Shouldn't have any errors.", 0, prwGet.getErrors().size());
            assertEquals("Should know number of bytes returned.", chunk.length, bytes.length);
            for (int i = 0; i < chunk.length; i++) {
                assertEquals("Bytes should be equal (#" + i + ")", chunk[i], bytes[i]);
            }

        } finally {
            testNetwork.stop();
        }
    }

    public void testGetMetaDataPropagatedMerge() throws Exception {
        testGetMetaDataPropagated(true);
    }

    public void testGetMetaDataPropagatedNoMerge() throws Exception {
        testGetMetaDataPropagated(false);
    }

    public void testGetMetaDataPropagated(boolean isMerge) throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetMetaDataPropagated(" + isMerge + ")");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));

        try {

            testNetwork.start();

            // Create data chunk
            final byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            {
                String host = HOST1;
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().clear();
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().clear();
                assertEquals("Shouldn't have a hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Shouldn't have a target hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addHashSpan(HashSpan.FULL);
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addTargetHashSpan(HashSpan.FULL);
                assertEquals("Should have a (full) hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Should have a (full) target hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
            }
            {
                String host = HOST2;
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().clear();
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().clear();
                assertEquals("Shouldn't have a hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Shouldn't have a target hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addHashSpan(HashSpan.FULL);
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addTargetHashSpan(HashSpan.FULL);
                assertEquals("Should have a (full) hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Should have a (full) target hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
            }
            {
                String host = HOST3;
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().clear();
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().clear();
                assertEquals("Shouldn't have a hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Shouldn't have a target hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addHashSpan(HashSpan.FULL);
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addTargetHashSpan(HashSpan.FULL);
                assertEquals("Should have a (full) hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Should have a (full) target hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
            }
            {
                String host = HOST4;
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().clear();
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().clear();
                assertEquals("Shouldn't have a hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Shouldn't have a target hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addHashSpan(HashSpan.FULL);
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addTargetHashSpan(HashSpan.FULL);
                assertEquals("Should have a (full) hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Should have a (full) target hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
            }
            {
                String host = HOST5;
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().clear();
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().clear();
                assertEquals("Shouldn't have a hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Shouldn't have a target hash span.", 0, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addHashSpan(HashSpan.FULL);
                testNetwork.getFlatFileTrancheServer(host).getConfiguration().addTargetHashSpan(HashSpan.FULL);
                assertEquals("Should have a (full) hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getHashSpans().size());
                assertEquals("Should have a (full) target hash span.", 1, testNetwork.getFlatFileTrancheServer(host).getConfiguration().getTargetHashSpans().size());
            }

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST1, ts1);
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts1, hash));

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST2, ts2);
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts2, hash));

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST3, ts3);
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts3, hash));

            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST4, ts4);
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts4, hash));

            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST5, ts5);
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts5, hash));

            testNetwork.updateNetwork();

            // First test: set to three. All three should have.
            String[] hosts = {HOST1, HOST3, HOST5};
            PropagationReturnWrapper prwSet = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), isMerge, hash, chunk, hosts);
            assertTrue("Should be void.", prwSet.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts3, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts5, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts2, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts4, hash));

            // Get the chunk from server #1
            {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts1.getMetaData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Should know number of bytes returned.", chunk.length, bytes.length);

                for (int i = 0; i < chunk.length; i++) {
                    assertEquals("Expecting same byte (#" + i + ")", chunk[i], bytes[i]);
                }
            }

            // Get the chunk from server #3
            {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts3.getMetaData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Should know number of bytes returned.", chunk.length, bytes.length);

                for (int i = 0; i < chunk.length; i++) {
                    assertEquals("Expecting same byte (#" + i + ")", chunk[i], bytes[i]);
                }
            }

            // Get the chunk from server #5
            {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts5.getMetaData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Should know number of bytes returned.", chunk.length, bytes.length);

                for (int i = 0; i < chunk.length; i++) {
                    assertEquals("Expecting same byte (#" + i + ")", chunk[i], bytes[i]);
                }
            }

            // Get the chunk from server #2 should fail if not propogate
            try {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts2.getMetaData(hashArr, false);

                assertNotSame("Should have errors.", 0, prwGet.getErrors().size());
            } catch (Exception e) {
                System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }

            // Get the chunk from server #4 should fail if not propogate
            try {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts4.getMetaData(hashArr, false);

                assertNotSame("Should have errors.", 0, prwGet.getErrors().size());
            } catch (Exception e) {
                System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }

            // Get the chunk from server #2 with propogation, and verify that stores copy
            {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts2.getMetaData(hashArr, true);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertNotNull("Chunk shouldn't be null.", bytes);

                assertEquals("Should know number of bytes returned.", chunk.length, bytes.length);

                for (int i = 0; i < chunk.length; i++) {
                    assertEquals("Expecting same byte (#" + i + ")", chunk[i], bytes[i]);
                }

                assertTrue("Should have data now.", IOUtil.hasMetaData(ts2, hash));
            }

            // Get the chunk from server #4 with propogation, and verify that stores copy
            {
                final BigHash[] hashArr = {hash};
                PropagationReturnWrapper prwGet = ts4.getMetaData(hashArr, true);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertNotNull("Chunk shouldn't be null.", bytes);

                assertEquals("Should know number of bytes returned.", chunk.length, bytes.length);

                for (int i = 0; i < chunk.length; i++) {
                    assertEquals("Expecting same byte (#" + i + ")", chunk[i], bytes[i]);
                }

                assertTrue("Should have data now.", IOUtil.hasMetaData(ts4, hash));
            }

            // Everything should have chunk now
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts2, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts3, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts4, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts5, hash));
        } finally {
            testNetwork.stop();
        }
    }

    public void testGetActivityLogEntries() throws Exception {

        final boolean wasTestingLogs = TestUtil.isTestingActivityLogs();

        TestUtil.setTestingActivityLogs(true);

        TestUtil.printTitle("RemoteTrancheServerTest:testGetActivityLogEntries()");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST1, ts1);

            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.ANY).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.ACTION_ANY).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.ACTION_SET).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.ACTION_DELETE).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.ACTION_REPLACE).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.SET_DATA).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.SET_META_DATA).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.DELETE_DATA).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.DELETE_META_DATA).length);

            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            String[] hosts = {HOST1};
            PropagationReturnWrapper prwSet = IOUtil.setData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);
            assertTrue("Should be void.", prwSet.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash));

            assertEquals("Shouldn't be any activities of this type.", 1, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.ANY).length);
            assertEquals("Shouldn't be any activities of this type.", 1, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.ACTION_ANY).length);
            assertEquals("Shouldn't be any activities of this type.", 1, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.ACTION_SET).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.ACTION_DELETE).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.ACTION_REPLACE).length);
            assertEquals("Shouldn't be any activities of this type.", 1, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.SET_DATA).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.SET_META_DATA).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.DELETE_DATA).length);
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.DELETE_META_DATA).length);

            Activity[] activities = ts1.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, 100, Activity.ANY);
            assertEquals("Should only be one.", 1, activities.length);
            assertEquals("Should have correct hash.", hash, activities[0].getHash());
            assertTrue("Should be data.", activities[0].isData());
            assertFalse("Should be meta data.", activities[0].isMetaData());
        } finally {
            testNetwork.stop();
            TestUtil.setTestingActivityLogs(wasTestingLogs);
        }
    }

    public void testGetActivityLogEntriesCount() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetActivityLogEntriesCount()");
        final boolean wasTestingLogs = TestUtil.isTestingActivityLogs();

        TestUtil.setTestingActivityLogs(true);
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST1, ts1);

            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ANY));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ACTION_ANY));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ACTION_SET));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ACTION_DELETE));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ACTION_REPLACE));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.SET_DATA));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.SET_META_DATA));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.DELETE_DATA));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.DELETE_META_DATA));

            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            String[] hosts = {HOST1};
            PropagationReturnWrapper prwSet = IOUtil.setData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);
            assertTrue("Should be void.", prwSet.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash));

            assertEquals("Shouldn't be any activities of this type.", 1, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ANY));
            assertEquals("Shouldn't be any activities of this type.", 1, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ACTION_ANY));
            assertEquals("Shouldn't be any activities of this type.", 1, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ACTION_SET));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ACTION_DELETE));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ACTION_REPLACE));
            assertEquals("Shouldn't be any activities of this type.", 1, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.SET_DATA));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.SET_META_DATA));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.DELETE_DATA));
            assertEquals("Shouldn't be any activities of this type.", 0, ts1.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.DELETE_META_DATA));
        } finally {
            testNetwork.stop();
            TestUtil.setTestingActivityLogs(wasTestingLogs);
        }
    }

    public void testGetConfiguration() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetConfiguration()");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST1, ts1);

            Configuration config = IOUtil.getConfiguration(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
            assertNotNull("Configuration shouldn't be null.", config);

            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);

            config = ffts.getConfiguration();
            config.setValue("bryan", "smith");

            ffts.setConfiguration(config);
            ffts.saveConfiguration();

            config = IOUtil.getConfiguration(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
            assertTrue("Should have key.", config.hasKey("bryan"));
            assertEquals("Should have correct config.", "smith", config.getValue("bryan"));

            config = ffts.getConfiguration();
            config.removeKeyValuePair("bryan");

            ffts.setConfiguration(config);
            ffts.saveConfiguration();

            config = IOUtil.getConfiguration(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
            assertFalse("Shouldn't have key any longer.", config.hasKey("bryan"));

        } finally {
            testNetwork.stop();
        }
    }

    public void testGetDataHashes() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetDataHashes()");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {

            testNetwork.start();

            // Create data chunk
            byte[] chunk1 = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash1 = new BigHash(chunk1);

            byte[] chunk2 = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash2 = new BigHash(chunk2);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST1, ts1);

            String[] hosts = {HOST1};

            BigHash[] dataHashes = ts1.getDataHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Shouldn't be any hashes yet.", 0, dataHashes.length);

            // Set chunk #1 and verify
            {

                PropagationReturnWrapper prwSet = IOUtil.setData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash1, chunk1, hosts);
                assertTrue("Should be void.", prwSet.isVoid());
                assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
                assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash1));

                final BigHash[] hashArr = {hash1};
                PropagationReturnWrapper prwGet = ts1.getData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Shouldn't have any errors.", 0, prwGet.getErrors().size());
                assertEquals("Should know number of bytes returned.", chunk1.length, bytes.length);

                BigHash verifyHash = new BigHash(bytes);
                assertEquals("Should generate correct hash.", hash1, verifyHash);
            }

            dataHashes = ts1.getDataHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Should be one hash.", 1, dataHashes.length);
            assertEquals("Should know hash.", hash1, dataHashes[0]);

            // Set chunk #2 and verify
            {
                PropagationReturnWrapper prwSet = IOUtil.setData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash2, chunk2, hosts);
                assertTrue("Should be void.", prwSet.isVoid());
                assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
                assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash1));

                final BigHash[] hashArr = {hash2};
                PropagationReturnWrapper prwGet = ts1.getData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Shouldn't have any errors.", 0, prwGet.getErrors().size());
                assertEquals("Should know number of bytes returned.", chunk2.length, bytes.length);

                BigHash verifyHash = new BigHash(bytes);
                assertEquals("Should generate correct hash.", hash2, verifyHash);
            }

            dataHashes = ts1.getDataHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Should be one hash.", 2, dataHashes.length);

            boolean isOkay1 = dataHashes[0].equals(hash1) && dataHashes[1].equals(hash2);
            boolean isOkay2 = dataHashes[1].equals(hash1) && dataHashes[0].equals(hash2);

            assertTrue("One of two scenarios should be true.", isOkay1 || isOkay2);

        } finally {
            testNetwork.stop();
        }
    }

    public void testGetMetaDataHashesMerge() throws Exception {
        testGetMetaDataHashes(true);
    }

    public void testGetMetaDataHashesNoMerge() throws Exception {
        testGetMetaDataHashes(false);
    }

    public void testGetMetaDataHashes(boolean isMerge) throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetMetaDataHashes()");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {

            testNetwork.start();

            // Create data chunk
            byte[] chunk1 = DevUtil.createRandomBigMetaDataChunk();
            BigHash hash1 = DevUtil.getRandomBigHash();

            byte[] chunk2 = DevUtil.createRandomBigMetaDataChunk();
            BigHash hash2 = DevUtil.getRandomBigHash();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST1, ts1);

            String[] hosts = {HOST1};

            BigHash[] metaHashes = ts1.getMetaDataHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Shouldn't be any hashes yet.", 0, metaHashes.length);

            // Set chunk #1 and verify
            {

                PropagationReturnWrapper prwSet = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), isMerge, hash1, chunk1, hosts);
                assertTrue("Should be void.", prwSet.isVoid());
                assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
                assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash1));

                final BigHash[] hashArr = {hash1};
                PropagationReturnWrapper prwGet = ts1.getMetaData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Shouldn't have any errors.", 0, prwGet.getErrors().size());
                assertEquals("Should know number of bytes returned.", chunk1.length, bytes.length);

                for (int i = 0; i < bytes.length; i++) {
                    assertEquals("Bytes should be equal (#" + i + ")", chunk1[i], bytes[i]);
                }
            }

            metaHashes = ts1.getMetaDataHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Should be one hash.", 1, metaHashes.length);
            assertEquals("Should know hash.", hash1, metaHashes[0]);

            // Set chunk #2 and verify
            {
                PropagationReturnWrapper prwSet = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), isMerge, hash2, chunk2, hosts);
                assertTrue("Should be void.", prwSet.isVoid());
                assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
                assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash1));

                final BigHash[] hashArr = {hash2};
                PropagationReturnWrapper prwGet = ts1.getMetaData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Shouldn't have any errors.", 0, prwGet.getErrors().size());
                assertEquals("Should know number of bytes returned.", chunk2.length, bytes.length);

                for (int i = 0; i < bytes.length; i++) {
                    assertEquals("Bytes should be equal (#" + i + ")", chunk2[i], bytes[i]);
                }
            }

            metaHashes = ts1.getMetaDataHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Should be two hashes.", 2, metaHashes.length);

            boolean isOkay1 = metaHashes[0].equals(hash1) && metaHashes[1].equals(hash2);
            boolean isOkay2 = metaHashes[1].equals(hash1) && metaHashes[0].equals(hash2);

            assertTrue("One of two scenarios should be true.", isOkay1 || isOkay2);

        } finally {
            testNetwork.stop();
        }
    }

    public void testGetProjectHashes() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetProjectHashes()");

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {

            final boolean isMerge = false;

            testNetwork.start();

            byte[] chunk1 = DevUtil.createRandomMetaDataChunk(RandomUtil.getInt(5) + 1, true);
            BigHash hash1 = DevUtil.getRandomBigHash();

            byte[] chunk2 = DevUtil.createRandomMetaDataChunk(RandomUtil.getInt(5) + 1, true);
            BigHash hash2 = DevUtil.getRandomBigHash();

            // This last one is not a project file meta data, so should not be a project hash
            byte[] chunk3 = DevUtil.createRandomMetaDataChunk(RandomUtil.getInt(5) + 1, false);
            BigHash hash3 = DevUtil.getRandomBigHash();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST1, ts1);

            String[] hosts = {HOST1};

            BigHash[] projectHashes = ts1.getProjectHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Shouldn't be any hashes yet.", 0, projectHashes.length);

            // Set chunk #1 and verify
            {

                PropagationReturnWrapper prwSet = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), isMerge, hash1, chunk1, hosts);
                assertTrue("Should be void.", prwSet.isVoid());
                assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
                assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash1));

                final BigHash[] hashArr = {hash1};
                PropagationReturnWrapper prwGet = ts1.getMetaData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Shouldn't have any errors.", 0, prwGet.getErrors().size());
                assertEquals("Should know number of bytes returned.", chunk1.length, bytes.length);

                for (int i = 0; i < bytes.length; i++) {
                    assertEquals("Bytes should be equal (#" + i + ")", chunk1[i], bytes[i]);
                }
            }

            projectHashes = ts1.getProjectHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Should be one hash.", 1, projectHashes.length);
            assertEquals("Should know hash.", hash1, projectHashes[0]);

            // Set chunk #2 and verify
            {
                PropagationReturnWrapper prwSet = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), isMerge, hash2, chunk2, hosts);
                assertTrue("Should be void.", prwSet.isVoid());
                assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
                assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash1));

                final BigHash[] hashArr = {hash2};
                PropagationReturnWrapper prwGet = ts1.getMetaData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Shouldn't have any errors.", 0, prwGet.getErrors().size());
                assertEquals("Should know number of bytes returned.", chunk2.length, bytes.length);

                for (int i = 0; i < bytes.length; i++) {
                    assertEquals("Bytes should be equal (#" + i + ")", chunk2[i], bytes[i]);
                }
            }

            projectHashes = ts1.getProjectHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Should be two hashes.", 2, projectHashes.length);

            boolean isOkay1 = projectHashes[0].equals(hash1) && projectHashes[1].equals(hash2);
            boolean isOkay2 = projectHashes[1].equals(hash1) && projectHashes[0].equals(hash2);

            assertTrue("One of two scenarios should be true.", isOkay1 || isOkay2);

            // Set chunk #2 and verify
            {
                PropagationReturnWrapper prwSet = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), isMerge, hash3, chunk3, hosts);
                assertTrue("Should be void.", prwSet.isVoid());
                assertEquals("Shouldn't be any errors.", 0, prwSet.getErrors().size());
                assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash1));

                final BigHash[] hashArr = {hash3};
                PropagationReturnWrapper prwGet = ts1.getMetaData(hashArr, false);
                assertTrue("Should be 2d array.", prwGet.isByteArrayDoubleDimension());
                byte[] bytes = ((byte[][]) prwGet.getReturnValueObject())[0];

                assertEquals("Shouldn't have any errors.", 0, prwGet.getErrors().size());
                assertEquals("Should know number of bytes returned.", chunk3.length, bytes.length);

                for (int i = 0; i < bytes.length; i++) {
                    assertEquals("Bytes should be equal (#" + i + ")", chunk3[i], bytes[i]);
                }
            }

            projectHashes = ts1.getProjectHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Should still be two hashes.", 2, projectHashes.length);

            isOkay1 = projectHashes[0].equals(hash1) && projectHashes[1].equals(hash2);
            isOkay2 = projectHashes[1].equals(hash1) && projectHashes[0].equals(hash2);

            assertTrue("One of two scenarios should be true.", isOkay1 || isOkay2);

        } finally {
            testNetwork.stop();
        }
    }

    public void testRegisterServer() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testRegisterServer()");
        throw new TodoException();
        // causes a freeze on the dev box
//        TestNetwork testNetwork = new TestNetwork();
//        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
//        // Note this second server is offline
//        final int PORT2 = 1501;
//        final boolean SSL2 = false;
//        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, PORT2, "127.0.0.1", true, false, SSL2));
//
//        try {
//
//            testNetwork.start();
//
//            assertTrue("First server should be online.", NetworkUtil.getStatus().getRow(HOST1).isOnline());
//            assertFalse("Second server should not be online.", NetworkUtil.getStatus().getRow(HOST2).isOnline());
//
//            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
//            assertNotNull("Should still have ffts, even though offline.", ffts2);
//
//            // Test network already built the server -- just need to start it
//            Server s2 = testNetwork.getServer(HOST2);
//            s2.start();
//
//            assertFalse("According to network status table, second server should not be online.", NetworkUtil.getStatus().getRow(HOST2).isOnline());
//
//            TrancheServer ts = ConnectionUtil.connectHost(HOST1, true);
//            assertNotNull("RemoteTrancheServer shouldn't be null.", ts);
//
//            ts.registerServer(IOUtil.createURL(HOST2, PORT2, SSL2));
//
//            assertTrue("Now second server should be online according to network status table.", NetworkUtil.getStatus().getRow(HOST2).isOnline());
//
//        } finally {
//            testNetwork.stop();
//        }
    }

    /**
     * <p>Tests that can set data to single server using propagation version of item.</p>
     */
    public void testSetDataPropagatedSingleServer() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testSetDataPropagatedSingleServer()");
        for (int i = 0; i < TEST_COUNT; i++) {
            setDataPropagatedSingleServer();
        }
    }

    public void setDataPropagatedSingleServer() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            // Create data chunk
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connection for host: " + HOST1, ts1);
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts1, hash));

            String[] hosts = {HOST1};
            PropagationReturnWrapper prw1 = IOUtil.setData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);
            assertTrue("Should be void.", prw1.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prw1.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can set meta data to single server using propagation version of item.</p>
     */
    public void testSetMetaDataPropagatedSingleServer() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testSetMetaDataPropagatedSingleServer()");
        for (int i = 0; i < TEST_COUNT; i++) {
            setMetaDataPropagatedSingleServer();
        }
    }

    public void setMetaDataPropagatedSingleServer() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            // Create data chunk
            byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts1, hash));

            String[] hosts = {HOST1};
            PropagationReturnWrapper prw1 = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk, hosts);
            assertTrue("Should be void.", prw1.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prw1.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can get nonces from single server using propagation version of item.</p>
     */
    public void testGetNoncesPropagatedSingleServer() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetNoncesPropagatedSingleServer()");
        for (int i = 0; i < TEST_COUNT; i++) {
            getNoncesPropagatedSingleServer();
        }
    }

    public void getNoncesPropagatedSingleServer() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);

            int noncesCount = RandomUtil.getInt(10) + 1;
            String[] hosts = {HOST1};
            PropagationReturnWrapper wrapper1 = ts1.getNonces(hosts, noncesCount);
            assertTrue("Should be true", wrapper1.isByteArrayTripleDimension());

            byte[][][] nonces = (byte[][][]) wrapper1.getReturnValueObject();
            assertEquals("Should know how many servers return nonces.", 1, nonces.length);
            assertEquals("Should know how many nonces we got back from each server.", noncesCount, nonces[0].length);
            for (int i = 0; i < noncesCount; i++) {
                assertEquals("Each nonce have proper number of bytes.", NonceMap.NONCE_BYTES, nonces[0][i].length);
            }
            assertEquals("Should have no exceptions.", 0, wrapper1.getErrors().size());
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can delete data from single server using propagation version of item.</p>
     */
    public void testDeleteDataPropagatedSingleServer() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteDataPropagatedSimple()");
        for (int i = 0; i < TEST_COUNT; i++) {
            deleteDataPropagatedSingleServer();
        }
    }

    public void deleteDataPropagatedSingleServer() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            // Create data chunk
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts1, hash));

            String[] hosts = {HOST1};
            PropagationReturnWrapper prw1 = IOUtil.setData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);
            assertTrue("Should be void.", prw1.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prw1.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash));

            PropagationReturnWrapper prw2 = IOUtil.deleteData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, hosts);
            assertTrue("Should be void.", prw2.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prw2.getErrors().size());
            assertFalse("Should not have chunk.", IOUtil.hasData(ts1, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can delete meta data from single server using propagation version of item.</p>
     */
    public void testDeleteMetaDataPropagatedSingleServer() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteMetaDataPropagatedSingleServer()");
        for (int i = 0; i < TEST_COUNT; i++) {
            deleteMetaDataPropagatedSingleServer();
        }
    }

    public void deleteMetaDataPropagatedSingleServer() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            // Create data chunk
            byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts1, hash));

            String[] hosts = {HOST1};
            PropagationReturnWrapper prw1 = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk, hosts);
            assertTrue("Should be void.", prw1.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prw1.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash));

            PropagationReturnWrapper prw2 = IOUtil.deleteMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, hosts);
            assertTrue("Should be void.", prw2.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prw2.getErrors().size());
            assertFalse("Should not have chunk.", IOUtil.hasMetaData(ts1, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can set data to multiple servers using propagation version of item.</p>
     */
    public void testSetDataPropagated() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testSetDataPropagated()");
        for (int i = 0; i < TEST_COUNT; i++) {
            setDataPropagated();
        }
    }

    public void setDataPropagated() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            Set<String> hosts = new HashSet();
            hosts.add(HOST1);
            hosts.add(HOST2);
            hosts.add(HOST3);

            Collection<MultiServerRequestStrategy> strategiesSet = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hosts, Tertiary.DONT_CARE, Tertiary.TRUE);

            assertTrue("Should be at least one appropriate strategy, instead found: " + strategiesSet.size(), strategiesSet.size() >= 1);

            MultiServerRequestStrategy[] strategiesArr = strategiesSet.toArray(new MultiServerRequestStrategy[0]);
            MultiServerRequestStrategy randomStrategy = strategiesArr[0];

            TrancheServer ts = ConnectionUtil.getHost(randomStrategy.getHostReceivingRequest());
            assertNotNull("ConnectionUtil should have connetion for host: " + randomStrategy.getHostReceivingRequest(), ts);

            // Create data chunk
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST3, ts3);

            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts3);

            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST5, ts3);

            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts1, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts2, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts3, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts4, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts5, hash));

            PropagationReturnWrapper prw1 = IOUtil.setData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts.toArray(new String[0]));

            assertTrue("Should be void.", prw1.isVoid());

            // If errors, print them
            if (prw1.getErrors().size() > 0) {
                System.err.println("==========================================================================================");
                System.err.println(" UH-OH! There was/were " + prw1.getErrors().size() + " error(s) in propagated request for test: testSetDataPropagated");
                System.err.println("==========================================================================================");

                for (PropagationExceptionWrapper pew : prw1.getErrors()) {
                    System.err.println("    * " + pew.exception.getClass().getSimpleName() + " for server \"" + pew.host + "\": " + pew.exception.getMessage());
                }
            }

            assertEquals("Shouldn't be any errors.", 0, prw1.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts2, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts3, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts4, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts5, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can set meta data to multiple servers using propagation version of item.</p>
     */
    public void testSetMetaDataPropagated() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testSetMetaDataPropagated()");
        for (int i = 0; i < TEST_COUNT; i++) {
            setMetaDataPropagated();
        }
    }

    public void setMetaDataPropagated() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            Set<String> hosts = new HashSet();
            hosts.add(HOST1);
            hosts.add(HOST2);
            hosts.add(HOST3);

            Collection<MultiServerRequestStrategy> strategiesSet = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hosts, Tertiary.DONT_CARE, Tertiary.TRUE);

            assertTrue("Should be at least one appropriate strategy, instead found: " + strategiesSet.size(), strategiesSet.size() >= 1);

            MultiServerRequestStrategy[] strategiesArr = strategiesSet.toArray(new MultiServerRequestStrategy[0]);
            MultiServerRequestStrategy randomStrategy = strategiesArr[0];

            TrancheServer ts = ConnectionUtil.getHost(randomStrategy.getHostReceivingRequest());
            assertNotNull("ConnectionUtil should have connetion for host: " + randomStrategy.getHostReceivingRequest(), ts);

            // Create data chunk
            byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST3, ts3);

            // need to connect to 4 and 5 to provide a pathway
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts4);
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST5, ts5);

            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts1, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts2, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts3, hash));

            PropagationReturnWrapper prw1 = IOUtil.setMetaData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk, hosts.toArray(new String[0]));
            assertTrue("Should be void.", prw1.isVoid());

            // If errors, print them
            if (prw1.getErrors().size() > 0) {
                System.err.println("==========================================================================================");
                System.err.println(" UH-OH! There was/were " + prw1.getErrors().size() + " error(s) in propagated request for test: testSetMetaDataPropagated");
                System.err.println("==========================================================================================");

                for (PropagationExceptionWrapper pew : prw1.getErrors()) {
                    System.err.println("    * " + pew.exception.getClass().getSimpleName() + " for server \"" + pew.host + "\": " + pew.exception.getMessage());
                }
            }

            assertEquals("Shouldn't be any errors.", 0, prw1.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts2, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts3, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can get single nonce from multiple servers using propagation version of item.</p>
     */
    public void testGetNoncePropagated() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetNoncePropagated()");
        for (int i = 0; i < TEST_COUNT; i++) {
            getNoncePropagated();
        }
    }

    public void getNoncePropagated() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            // ---------------------------------------------------------------------------
            // Straight forward: get from three hosts. Note that the strategy often picks
            //                   a server not in that "target" host collection to
            //                   start the propagated request. (See MultiServerRequestStrategy
            //                   for more details.)
            // ---------------------------------------------------------------------------

            Set<String> hosts = new HashSet();
            hosts.add(HOST1);
            hosts.add(HOST2);
            hosts.add(HOST3);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST3, ts3);

            // need to connect to 4 and 5 to provide a pathway
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts4);
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST5, ts5);

            Collection<MultiServerRequestStrategy> strategiesSet = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hosts, Tertiary.DONT_CARE, Tertiary.DONT_CARE);
            assertTrue("Should be at least one appropriate strategy, instead found: " + strategiesSet.size(), strategiesSet.size() >= 1);
            MultiServerRequestStrategy[] strategiesArr = strategiesSet.toArray(new MultiServerRequestStrategy[0]);
            MultiServerRequestStrategy randomStrategy = strategiesArr[0];

            TrancheServer ts = ConnectionUtil.getHost(randomStrategy.getHostReceivingRequest());
            assertNotNull("ConnectionUtil should have connetion for host: " + randomStrategy.getHostReceivingRequest(), ts);

            byte[][] nonces = IOUtil.getNonces(ts, hosts.toArray(new String[0]));
            assertEquals("Should be three nonces.", 3, nonces.length);
            for (int i = 0; i < 3; i++) {
                assertEquals("Should have proper number of bytes.", NonceMap.NONCE_BYTES, nonces[i].length);
            }
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can get nonces from multiple servers using propagation version of item.</p>
     */
    public void testGetNoncesPropagated() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetNoncesPropagated()");
        for (int i = 0; i < TEST_COUNT; i++) {
            getNoncesPropagated();
        }
    }

    public void getNoncesPropagated() throws Exception {
        final int nonceCount = RandomUtil.getInt(10) + 1;
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            // ---------------------------------------------------------------------------
            // Straight forward: get from three hosts. Note that the strategy often picks
            //                   a server not in that "target" host collection to
            //                   start the propagated request. (See MultiServerRequestStrategy
            //                   for more details.)
            // ---------------------------------------------------------------------------

            Set<String> hosts = new HashSet();
            hosts.add(HOST1);
            hosts.add(HOST2);
            hosts.add(HOST3);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST3, ts3);

            // need to connect to 4 and 5 to provide a pathway
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts4);
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST5, ts5);

            Collection<MultiServerRequestStrategy> strategiesSet = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hosts, Tertiary.DONT_CARE, Tertiary.DONT_CARE);
            assertTrue("Should be at least one appropriate strategy, instead found: " + strategiesSet.size(), strategiesSet.size() >= 1);

            MultiServerRequestStrategy[] strategiesArr = strategiesSet.toArray(new MultiServerRequestStrategy[0]);
            MultiServerRequestStrategy randomStrategy = strategiesArr[0];

            TrancheServer ts = ConnectionUtil.getHost(randomStrategy.getHostReceivingRequest());
            assertNotNull("ConnectionUtil should have connetion for host: " + randomStrategy.getHostReceivingRequest(), ts);

            PropagationReturnWrapper wrapper1 = ts.getNonces(hosts.toArray(new String[0]), nonceCount);

            assertTrue("Should be true", wrapper1.isByteArrayTripleDimension());

            byte[][][] nonces = (byte[][][]) wrapper1.getReturnValueObject();
            assertEquals("Should be three servers returning nonces.", 3, nonces.length);

            for (int i = 0; i < 3; i++) {
                assertEquals("Should know how many nonces each server returns.", nonceCount, nonces[i].length);
                for (int j = 0; j < nonceCount; j++) {
                    assertEquals("Each nonce should have proper number of bytes.", NonceMap.NONCE_BYTES, nonces[i][j].length);
                }
            }
            assertEquals("Should have no exceptions.", 0, wrapper1.getErrors().size());
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can delete data from multiple servers using propagation version of item.</p>
     */
    public void testDeleteDataPropagatedPartially() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteDataPropagatedPartially()");
        for (int i = 0; i < TEST_COUNT; i++) {
            deleteDataPropagatedPartially();
        }
    }

    public void deleteDataPropagatedPartially() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            Set<String> hosts = new HashSet();
            hosts.add(HOST1);
            hosts.add(HOST2);
            hosts.add(HOST3);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST3, ts3);

            // need to connect to 4 and 5 to provide a pathway
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts4);
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST5, ts5);

            Collection<MultiServerRequestStrategy> strategiesSet = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hosts, Tertiary.DONT_CARE, Tertiary.DONT_CARE);
            assertTrue("Should be at least one appropriate strategy, instead found: " + strategiesSet.size(), strategiesSet.size() >= 1);

            MultiServerRequestStrategy[] strategiesArr = strategiesSet.toArray(new MultiServerRequestStrategy[0]);
            MultiServerRequestStrategy randomStrategy = strategiesArr[0];

            TrancheServer ts = ConnectionUtil.getHost(randomStrategy.getHostReceivingRequest());
            assertNotNull("ConnectionUtil should have connetion for host: " + randomStrategy.getHostReceivingRequest(), ts);

            // Create data chunk
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts1, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts2, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts3, hash));

            PropagationReturnWrapper prw1 = IOUtil.setData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts.toArray(new String[0]));

            assertTrue("Should be void.", prw1.isVoid());

            // If errors, print them
            if (prw1.getErrors().size() > 0) {
                System.err.println("==========================================================================================");
                System.err.println(" UH-OH! There was/were " + prw1.getErrors().size() + " error(s) in propagated request for test: testSetDataPropagated");
                System.err.println("==========================================================================================");

                for (PropagationExceptionWrapper pew : prw1.getErrors()) {
                    System.err.println("    * " + pew.exception.getClass().getSimpleName() + " for server \"" + pew.host + "\": " + pew.exception.getMessage());
                }
            }

            assertEquals("Shouldn't be any errors.", 0, prw1.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts2, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts3, hash));

            String[] someHosts = {HOST1, HOST3};

            PropagationReturnWrapper prw2 = IOUtil.deleteData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, someHosts);

            assertTrue("Should be void.", prw2.isVoid());

            // If errors, print them
            if (prw2.getErrors().size() > 0) {
                System.err.println("==========================================================================================");
                System.err.println(" UH-OH! There was/were " + prw2.getErrors().size() + " error(s) in propagated request for test: testSetDataPropagated");
                System.err.println("==========================================================================================");

                for (PropagationExceptionWrapper pew : prw2.getErrors()) {
                    System.err.println("    * " + pew.exception.getClass().getSimpleName() + " for server \"" + pew.host + "\": " + pew.exception.getMessage());
                }
            }

            assertEquals("Shouldn't be any errors.", 0, prw2.getErrors().size());
            assertFalse("Should not have chunk.", IOUtil.hasData(ts1, hash));
            assertTrue("Should still have chunk.", IOUtil.hasData(ts2, hash));
            assertFalse("Should not have chunk.", IOUtil.hasData(ts3, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can delete meta data from multiple servers using propagation version of item.</p>
     */
    public void testDeleteMetaDataPropagatedPartially() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteMetaDataPropagatedPartially()");
        for (int i = 0; i < TEST_COUNT; i++) {
            deleteMetaDataPropagatedPartially();
        }
    }

    public void deleteMetaDataPropagatedPartially() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            Set<String> hosts = new HashSet();
            hosts.add(HOST1);
            hosts.add(HOST2);
            hosts.add(HOST3);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST3, ts3);

            // need to connect to 4 and 5 to provide a pathway
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts4);
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST5, ts5);

            Collection<MultiServerRequestStrategy> strategiesSet = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hosts, Tertiary.DONT_CARE, Tertiary.DONT_CARE);
            assertTrue("Should be at least one appropriate strategy, instead found: " + strategiesSet.size(), strategiesSet.size() >= 1);

            MultiServerRequestStrategy[] strategiesArr = strategiesSet.toArray(new MultiServerRequestStrategy[0]);
            MultiServerRequestStrategy randomStrategy = strategiesArr[0];

            TrancheServer ts = ConnectionUtil.getHost(randomStrategy.getHostReceivingRequest());
            assertNotNull("ConnectionUtil should have connetion for host: " + randomStrategy.getHostReceivingRequest(), ts);

            // Create data chunk
            byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts1, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts2, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts3, hash));

            PropagationReturnWrapper prw1 = IOUtil.setMetaData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk, hosts.toArray(new String[0]));

            assertTrue("Should be void.", prw1.isVoid());

            // If errors, print them
            if (prw1.getErrors().size() > 0) {
                System.err.println("==========================================================================================");
                System.err.println(" UH-OH! There was/were " + prw1.getErrors().size() + " error(s) in propagated request for test: testSetMetaDataPropagated");
                System.err.println("==========================================================================================");

                for (PropagationExceptionWrapper pew : prw1.getErrors()) {
                    System.err.println("    * " + pew.exception.getClass().getSimpleName() + " for server \"" + pew.host + "\": " + pew.exception.getMessage());
                }
            }

            assertEquals("Shouldn't be any errors.", 0, prw1.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts2, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts3, hash));

            String[] someHosts = {HOST3};

            PropagationReturnWrapper prw2 = IOUtil.deleteMetaData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, someHosts);

            assertTrue("Should be void.", prw2.isVoid());

            // If errors, print them
            if (prw2.getErrors().size() > 0) {
                System.err.println("==========================================================================================");
                System.err.println(" UH-OH! There was/were " + prw2.getErrors().size() + " error(s) in propagated request for test: testSetDataPropagated");
                System.err.println("==========================================================================================");

                for (PropagationExceptionWrapper pew : prw2.getErrors()) {
                    System.err.println("    * " + pew.exception.getClass().getSimpleName() + " for server \"" + pew.host + "\": " + pew.exception.getMessage());
                }
            }

            assertEquals("Shouldn't be any errors.", 0, prw2.getErrors().size());
            assertTrue("Should still have chunk.", IOUtil.hasMetaData(ts1, hash));
            assertTrue("Should still have chunk.", IOUtil.hasMetaData(ts2, hash));
            assertFalse("Should not have chunk.", IOUtil.hasMetaData(ts3, hash));

        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can delete all data from multiple servers using propagation version of item.</p>
     */
    public void testDeleteDataPropagatedFully() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteDataPropagatedFully()");
        for (int i = 0; i < TEST_COUNT; i++) {
            deleteDataPropagatedFully();
        }
    }

    public void deleteDataPropagatedFully() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            Set<String> hosts = new HashSet();
            hosts.add(HOST1);
            hosts.add(HOST2);
            hosts.add(HOST3);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST3, ts3);

            // need to connect to 4 and 5 to provide a pathway
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts4);
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST5, ts5);

            Collection<MultiServerRequestStrategy> strategiesSet = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hosts, Tertiary.DONT_CARE, Tertiary.DONT_CARE);
            assertTrue("Should be at least one appropriate strategy, instead found: " + strategiesSet.size(), strategiesSet.size() >= 1);

            MultiServerRequestStrategy[] strategiesArr = strategiesSet.toArray(new MultiServerRequestStrategy[0]);
            MultiServerRequestStrategy randomStrategy = strategiesArr[0];

            TrancheServer ts = ConnectionUtil.getHost(randomStrategy.getHostReceivingRequest());
            assertNotNull("ConnectionUtil should have connetion for host: " + randomStrategy.getHostReceivingRequest(), ts);

            // Create data chunk
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts1, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts2, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts3, hash));

            PropagationReturnWrapper prw1 = IOUtil.setData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts.toArray(new String[0]));
            assertTrue("Should be void.", prw1.isVoid());

            // If errors, print them
            if (prw1.getErrors().size() > 0) {
                System.err.println("==========================================================================================");
                System.err.println(" UH-OH! There was/were " + prw1.getErrors().size() + " error(s) in propagated request for test: testSetDataPropagated");
                System.err.println("==========================================================================================");

                for (PropagationExceptionWrapper pew : prw1.getErrors()) {
                    System.err.println("    * " + pew.exception.getClass().getSimpleName() + " for server \"" + pew.host + "\": " + pew.exception.getMessage());
                }
            }

            assertEquals("Shouldn't be any errors.", 0, prw1.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts2, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts3, hash));

            PropagationReturnWrapper prw2 = IOUtil.deleteData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, hosts.toArray(new String[0]));

            assertTrue("Should be void.", prw2.isVoid());

            // If errors, print them
            if (prw2.getErrors().size() > 0) {
                System.err.println("==========================================================================================");
                System.err.println(" UH-OH! There was/were " + prw2.getErrors().size() + " error(s) in propagated request for test: testSetDataPropagated");
                System.err.println("==========================================================================================");

                for (PropagationExceptionWrapper pew : prw2.getErrors()) {
                    System.err.println("    * " + pew.exception.getClass().getSimpleName() + " for server \"" + pew.host + "\": " + pew.exception.getMessage());
                }
            }

            assertEquals("Shouldn't be any errors.", 0, prw2.getErrors().size());
            assertFalse("Should not have chunk.", IOUtil.hasData(ts1, hash));
            assertFalse("Should not have chunk.", IOUtil.hasData(ts2, hash));
            assertFalse("Should not have chunk.", IOUtil.hasData(ts3, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can delete all meta data from multiple servers using propagation version of item.</p>
     */
    public void testDeleteMetaDataPropagatedFully() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteMetaDataPropagatedFully()");
        for (int i = 0; i < TEST_COUNT; i++) {
            deleteMetaDataPropagatedFully();
        }
    }

    public void deleteMetaDataPropagatedFully() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            Set<String> hosts = new HashSet();
            hosts.add(HOST1);
            hosts.add(HOST2);
            hosts.add(HOST3);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);
            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);
            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST3, ts3);
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts4);
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST5, ts5);

            Collection<MultiServerRequestStrategy> strategiesSet = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hosts, Tertiary.DONT_CARE, Tertiary.DONT_CARE);
            assertTrue("Should be at least one appropriate strategy, instead found: " + strategiesSet.size(), strategiesSet.size() >= 1);

            MultiServerRequestStrategy[] strategiesArr = strategiesSet.toArray(new MultiServerRequestStrategy[0]);
            MultiServerRequestStrategy randomStrategy = strategiesArr[0];

            TrancheServer ts = ConnectionUtil.getHost(randomStrategy.getHostReceivingRequest());
            assertNotNull("ConnectionUtil should have connetion for host: " + randomStrategy.getHostReceivingRequest(), ts);

            // Create data chunk
            byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts1, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts2, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts3, hash));

            PropagationReturnWrapper prw1 = IOUtil.setMetaData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk, hosts.toArray(new String[0]));

            assertTrue("Should be void.", prw1.isVoid());

            // If errors, print them
            if (prw1.getErrors().size() > 0) {
                System.err.println("==========================================================================================");
                System.err.println(" UH-OH! There was/were " + prw1.getErrors().size() + " error(s) in propagated request for test: testSetMetaDataPropagated");
                System.err.println("==========================================================================================");

                for (PropagationExceptionWrapper pew : prw1.getErrors()) {
                    System.err.println("    * " + pew.exception.getClass().getSimpleName() + " for server \"" + pew.host + "\": " + pew.exception.getMessage());
                }
            }

            assertEquals("Shouldn't be any errors.", 0, prw1.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts2, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts3, hash));

            PropagationReturnWrapper prw2 = IOUtil.deleteMetaData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, hosts.toArray(new String[0]));

            assertTrue("Should be void.", prw2.isVoid());

            // If errors, print them
            if (prw2.getErrors().size() > 0) {
                System.err.println("==========================================================================================");
                System.err.println(" UH-OH! There was/were " + prw2.getErrors().size() + " error(s) in propagated request for test: testSetDataPropagated");
                System.err.println("==========================================================================================");

                for (PropagationExceptionWrapper pew : prw2.getErrors()) {
                    System.err.println("    * " + pew.exception.getClass().getSimpleName() + " for server \"" + pew.host + "\": " + pew.exception.getMessage());
                }
            }

            assertEquals("Shouldn't be any errors.", 0, prw2.getErrors().size());
            assertFalse("Should not have chunk.", IOUtil.hasMetaData(ts1, hash));
            assertFalse("Should not have chunk.", IOUtil.hasMetaData(ts2, hash));
            assertFalse("Should not have chunk.", IOUtil.hasMetaData(ts3, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests the behavior when server receives request for server that is offline. Should calmly wrap exception and return.</p>
     */
    public void testSetDataPropagatedFailsWhenServerOffline() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testSetDataPropagatedFailsWhenServerOffline()");
        for (int i = 0; i < TEST_COUNT; i++) {
            setDataPropagatedFailsWhenServerOffline();
        }
    }

    public void setDataPropagatedFailsWhenServerOffline() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, false, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {

            testNetwork.start();

            // Create data chunk
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);
            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts4);
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST5, ts5);

            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts1, hash));

            // Note: this server is offline
            String[] hosts = {HOST3};

            PropagationReturnWrapper prw1 = IOUtil.setData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);

            assertTrue("Should be void.", prw1.isVoid());
            assertEquals("Should be an error.", 1, prw1.getErrors().size());
            assertFalse("Should not have data chunk since not part of target collection.", IOUtil.hasData(ts1, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests the behavior when server receives request for server that is offline. Should calmly wrap exception and return.</p>
     */
    public void testSetMetaDataPropagatedFailsWhenServerOffline() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testSetMetaDataPropagatedFailsWhenServerOffline()");
        for (int i = 0; i < TEST_COUNT; i++) {
            setMetaDataPropagatedFailsWhenServerOffline();
        }
    }

    public void setMetaDataPropagatedFailsWhenServerOffline() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, false, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);
            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);
            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST3, ts3);
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST5, ts5);

            // Create data chunk
            byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts1, hash));

            // Note: this server is offline
            String[] hosts = {HOST4};

            PropagationReturnWrapper prw1 = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk, hosts);

            assertTrue("Should be void.", prw1.isVoid());
            assertEquals("Should be an error.", 1, prw1.getErrors().size());
            assertFalse("Should not have data chunk since not part of target collection.", IOUtil.hasMetaData(ts1, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests the behavior when server receives request for server that is offline. Should calmly wrap exception and return.</p>
     */
    public void testGetNoncePropagatedFailsWhenServerOffline() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetNoncePropagatedFailsWhenServerOffline()");
        for (int i = 0; i < TEST_COUNT; i++) {
            getNoncePropagatedFailsWhenServerOffline();
        }
    }

    public void getNoncePropagatedFailsWhenServerOffline() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, false, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);
            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST3, ts3);
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts4);
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST5, ts5);

            // Note this is offline
            String[] hosts = {HOST2};

            try {
                byte[][] nonces = IOUtil.getNonces(ts1, hosts);
                assertEquals("Should only be one nonce.", 1, nonces.length);
                assertNull("Should be null (since unreachable).", nonces[0]);
            } catch (PropagationUnfulfillableHostException nope) {
                // fine
            }
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests the behavior when server receives request for server that is offline. Should calmly wrap exception and return.</p>
     */
    public void testGetNoncesPropagatedFailsWhenServerOffline() throws Exception {
        for (int i = 0; i < TEST_COUNT; i++) {
            getNoncesPropagatedFailsWhenServerOffline();
        }
    }

    public void getNoncesPropagatedFailsWhenServerOffline() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, false, false));
        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);
            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);
            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST3, ts3);
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts4);

            // Note this is offline
            String[] hosts = {HOST5};

            int noncesCount = RandomUtil.getInt(10) + 1;

            PropagationReturnWrapper wrapper1 = ts1.getNonces(hosts, noncesCount);

            assertTrue("Should be true", wrapper1.isByteArrayTripleDimension());

            byte[][][] nonces = (byte[][][]) wrapper1.getReturnValueObject();

            assertEquals("Should only be one nonce.", 1, nonces.length);
            assertEquals("Should be an exception (since unreachable).", 1, wrapper1.getErrors().size());
            assertEquals("Should not be any nonces returned.", 0, nonces[0].length);
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can get everything online even when everything else is offline.</p>
     */
    public void testSetDataPropagatedAllOnlineSucceeds() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testSetDataPropagatedAllOnlineSucceeds()");
        for (int i = 0; i < TEST_COUNT; i++) {
            setDataPropagatedAllOnlineSucceeds();
        }
    }

    public void setDataPropagatedAllOnlineSucceeds() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, false, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, false, false));
        try {
            testNetwork.start();

            // Create data chunk
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);
            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);
            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST3, ts3);

            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts1, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts2, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasData(ts3, hash));

            String[] hosts = {HOST1, HOST2, HOST3};

            PropagationReturnWrapper prw1 = IOUtil.setData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);

            assertTrue("Should be void.", prw1.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prw1.getErrors().size());

            assertTrue("Should have chunk.", IOUtil.hasData(ts1, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts2, hash));
            assertTrue("Should have chunk.", IOUtil.hasData(ts3, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can get everything online even when everything else is offline.</p>
     */
    public void testSetMetaDataPropagatedAllOnlineSucceeds() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testSetMetaDataPropagatedAllOnlineSucceeds()");
        for (int i = 0; i < TEST_COUNT; i++) {
            setMetaDataPropagatedAllOnlineSucceeds();
        }
    }

    public void setMetaDataPropagatedAllOnlineSucceeds() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, false, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, false, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, false, false));
        try {
            testNetwork.start();

            // Create data chunk
            byte[] chunk = DevUtil.createRandomMetaDataChunk();
            BigHash hash = DevUtil.getRandomBigHash();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts1);

            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts1, hash));
            assertFalse("Should not have chunk yet.", IOUtil.hasMetaData(ts4, hash));

            String[] hosts = {HOST1, HOST4};

            PropagationReturnWrapper prw1 = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk, hosts);

            assertTrue("Should be void.", prw1.isVoid());
            assertEquals("Shouldn't be any errors.", 0, prw1.getErrors().size());
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts1, hash));
            assertTrue("Should have chunk.", IOUtil.hasMetaData(ts4, hash));
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can get everything online even when everything else is offline.</p>
     */
    public void testGetNoncePropagatedAllOnlineSucceeds() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetNoncePropagatedAllOnlineSucceeds()");
        for (int i = 0; i < TEST_COUNT; i++) {
            getNoncePropagatedAllOnlineSucceeds();
        }
    }

    public void getNoncePropagatedAllOnlineSucceeds() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, false, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            String[] hosts = {HOST1, HOST2, HOST4, HOST5};

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);
            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);
            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST4, ts4);
            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST5, ts5);

            byte[][] nonces = IOUtil.getNonces(ts1, hosts);

            assertEquals("Should be four nonces.", 4, nonces.length);

            // Make sure each server returned nonce with proper bytes
            assertEquals("Should have proper number of bytes.", NonceMap.NONCE_BYTES, nonces[0].length);
            assertEquals("Should have proper number of bytes.", NonceMap.NONCE_BYTES, nonces[1].length);
            assertEquals("Should have proper number of bytes.", NonceMap.NONCE_BYTES, nonces[2].length);
            assertEquals("Should have proper number of bytes.", NonceMap.NONCE_BYTES, nonces[3].length);
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Tests that can get everything online even when everything else is offline.</p>
     * @throws java.lang.Exception
     */
    public void testGetNoncesPropagatedAllOnlineSucceeds() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testGetNoncesPropagatedAllOnlineSucceeds()");
        for (int i = 0; i < TEST_COUNT; i++) {
            getNoncesPropagatedAllOnlineSucceeds();
        }
    }

    public void getNoncesPropagatedAllOnlineSucceeds() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false));
        // Offline
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            String[] hosts = {HOST1, HOST2};

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);
            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);

            int noncesCount = RandomUtil.getInt(10) + 1;

            PropagationReturnWrapper wrapper1 = ts1.getNonces(hosts, noncesCount);
            assertTrue("Should be true", wrapper1.isByteArrayTripleDimension());

            byte[][][] nonces = (byte[][][]) wrapper1.getReturnValueObject();
            assertEquals("Should know how many servers return nonces.", 2, nonces.length);
            assertEquals("Should know how many nonces we got back from each server: " + hosts[0], noncesCount, nonces[0].length);
            assertEquals("Should know how many nonces we got back from each server: " + hosts[1], noncesCount, nonces[1].length);

            for (int i = 0; i < noncesCount; i++) {
                assertEquals("Each nonce have proper number of bytes: " + hosts[0], NonceMap.NONCE_BYTES, nonces[0][i].length);
                assertEquals("Each nonce have proper number of bytes: " + hosts[1], NonceMap.NONCE_BYTES, nonces[1][i].length);
            }
            assertEquals("Should have no exceptions.", 0, wrapper1.getErrors().size());
        } finally {
            testNetwork.stop();
        }
    }

    public void testSetDataNoHosts() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testSetDataNoHosts");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            final byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            final BigHash hash = new BigHash(chunk);

            final String[] hosts = {};

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
        TestUtil.printTitle("RemoteTrancheServerTest:testSetMetaDataNoHosts");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            final byte[] chunk = DevUtil.createRandomMetaDataChunk(RandomUtil.getInt(5) + 1, RandomUtil.getBoolean());
            final BigHash hash = DevUtil.getRandomBigHash();

            final String[] hosts = {};

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
        TestUtil.printTitle("RemoteTrancheServerTest:testGetNonces");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            final String[] hosts = {};

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
        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteDataNoHosts");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            final BigHash hash = DevUtil.getRandomBigHash();

            final String[] hosts = {};

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
        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteMetaDataNoHosts");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            final BigHash hash = DevUtil.getRandomBigHash();

            final String[] hosts = {};

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

    public void testPing() throws Exception {
        TestUtil.printTitle("RemoteTrancheServerTest:testPing()");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();
            TrancheServer ts = ConnectionUtil.connectHost(HOST1, true);
            ts.ping();
        } finally {
            testNetwork.stop();
        }
    }

//    public void testNonce() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testNonce()");
//        FlatFileTrancheServer ffdfs = null;
//        Server server = null;
//        TrancheServer rdfs = null;
//        try {
//            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort(), false);
//            server.start();
//            server.waitForStartup(1000);
//            rdfs = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//            byte[] nonce = IOUtil.getNonce(rdfs);
//            assertNotNull(nonce);
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(rdfs);
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//    }
//
//    public void testBatchOfNegative1Nonces() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testBatchOfNegative1Nonces()");
//        testBatchNonces(-1);
//    }
//
//    public void testBatchOf0Nonces() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testBatchOf0Nonces()");
//        testBatchNonces(0);
//    }
//
//    public void testBatchOf1Nonces() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testBatchOf1Nonces()");
//        testBatchNonces(1);
//    }
//
//    public void testBatchOf5Nonces() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testBatchOf5Nonces()");
//        testBatchNonces(5);
//    }
//
//    public void testBatchOf100Nonces() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testBatchOf100Nonces()");
//        testBatchNonces(100);
//    }
//
//    private void testBatchNonces(int count) throws Exception {
//        FlatFileTrancheServer ffdfs = null;
//        Server server = null;
//        TrancheServer rdfs = null;
//        try {
//            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort(), false);
//            server.start();
//            server.waitForStartup(1000);
//            rdfs = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//            TrancheServerTest.testGetNonces(rdfs, count);
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//    }
//
//    public void testBatchOfSize1GetData() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testBatchOfSize1GetData()");
//        testBatchGetData(1);
//    }
//
//    public void testBatchOfSize5GetData() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testBatchOfSize5GetData()");
//        testBatchGetData(5);
//    }
//
//    public void testBatchOfSize25GetData() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testBatchOfSize25GetData()");
//        testBatchGetData(25);
//    }
//
//    private void testBatchGetData(int count) throws Exception {
//        FlatFileTrancheServer ffdfs = null;
//        Server server = null;
//        TrancheServer rdfs = null;
//        try {
//            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffdfs.getConfiguration().getUsers().add(DevUtil.getDevUser());
//            ffdfs.saveConfiguration();
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort(), false);
//            server.start();
//            rdfs = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            TrancheServerTest.testGetChunks(rdfs, DevUtil.getDevUser(), count, false);
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//    }
//
//    public void testBatchOfSize1GetMetaData() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testBatchOfSize1GetMetaData()");
//        testBatchGetMetaData(1);
//    }
//
//    public void testBatchOfSize5GetMetaData() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testBatchOfSize5GetMetaData()");
//        testBatchGetMetaData(5);
//    }
//
//    public void testBatchOfSize25GetMetaData() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testBatchOfSize25GetMetaData()");
//        testBatchGetMetaData(25);
//    }
//
//    public void testBatchGetMetaData(int count) throws Exception {
//        FlatFileTrancheServer ffdfs = null;
//        Server server = null;
//        TrancheServer rdfs = null;
//        try {
//            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffdfs.getConfiguration().getUsers().add(DevUtil.getDevUser());
//            ffdfs.saveConfiguration();
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort(), false);
//            server.start();
//            rdfs = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            TrancheServerTest.testGetChunks(rdfs, DevUtil.getDevUser(), count, true);
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//    }
//
//    /**
//     * Tests checking whether chunk exists before and after adding chunks.
//     */
//    public void testDoesAndDoesNotHaveChunk() throws Exception {
//        FlatFileTrancheServer ffserver = null;
//        Server server = null;
//        TrancheServer rserver = null;
//        try {
//            ffserver = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffserver.getConfiguration().addUser(DevUtil.getDevUser());
//            ffserver.saveConfiguration();
//            server = new Server(ffserver, DevUtil.getDefaultTestPort());
//            server.start();
//            rserver = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            // Make a faux data chunk and meta data chunk
//            byte[] dataChunk = new byte[1024];
//            RandomUtil.getBytes(dataChunk);
//
//            byte[] metaChunk = DevUtil.createRandomMetaDataChunk();
//
//            BigHash dataHash = new BigHash(dataChunk);
//            BigHash metaHash = DevUtil.getRandomBigHash();
//
//            assertFalse("Shouldn't have data yet", IOUtil.hasData(rserver, dataHash));
//            assertFalse("Shouldn't have meta yet", IOUtil.hasMetaData(rserver, metaHash));
//
//            IOUtil.setData(rserver, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHash, dataChunk);
//            IOUtil.setMetaData(rserver, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaHash, metaChunk);
//
//            assertTrue("Should have data now", IOUtil.hasData(rserver, dataHash));
//            assertTrue("Should have meta now", IOUtil.hasMetaData(rserver, metaHash));
//        } finally {
//            IOUtil.safeClose(rserver);
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffserver);
//        }
//    }
//
//    /**
//     * <p>Create to remote tranche servers and test that the equals method.</p>
//     */
//    public void testEquals() throws Exception {
//        FlatFileTrancheServer ffserver = null;
//        Server server = null;
//        RemoteTrancheServer rts1 = null, rts2 = null;
//        try {
//            ffserver = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffserver.getConfiguration().addUser(DevUtil.getDevUser());
//
//            server = new Server(ffserver, DevUtil.getDefaultTestPort(), false);
//            server.start();
//
//            Thread.yield();
//
//            rts1 = new RemoteTrancheServer("127.0.0.1", server.getPort(), false);
//            rts2 = new RemoteTrancheServer("127.0.0.1", server.getPort(), false);
//
//            assertTrue("Expect remote tranche servers to be equal.", rts1.equals(rts2));
//        } finally {
//            IOUtil.safeClose(rts1);
//            IOUtil.safeClose(rts2);
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffserver);
//        }
//    }
//
//    public void testSetData() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testSetData()");
//        testSetData(false);
//    }
//
//    public void testSetDataSecure() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testSetDataSecure()");
//        testSetData(true);
//    }
//
//    public void testSetData(boolean secure) throws Exception {
//        FlatFileTrancheServer ffdfs = null;
//        Server server = null;
//        TrancheServer rdfs = null;
//        try {
//            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffdfs.getConfiguration().getUsers().add(DevUtil.getDevUser());
//            ffdfs.saveConfiguration();
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort(), secure);
//            server.start();
//            rdfs = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            TrancheServerTest.testSetData(rdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//    }
//
//    public void testSetMetaData() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testSetMetaData()");
//        testSetMetaData(false);
//    }
//
//    public void testSetMetaDataSecure() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testSetMetaDataSecure()");
//        testSetMetaData(true);
//    }
//
//    public void testSetMetaData(boolean secure) throws Exception {
//        FlatFileTrancheServer ffdfs = null;
//        Server server = null;
//        TrancheServer rdfs = null;
//        try {
//            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffdfs.getConfiguration().getUsers().add(DevUtil.getDevUser());
//            ffdfs.saveConfiguration();
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort(), secure);
//            server.start();
//            rdfs = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            TrancheServerTest.testSetMetaData(rdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//    }
//
//    public void testSetConfiguration() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testSetSetConfiguration()");
//        testSetSetConfiguration(false);
//    }
//
//    public void testSetSetConfigurationSecure() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testSetSetConfigurationSecure()");
//        testSetSetConfiguration(true);
//    }
//
//    public void testSetSetConfiguration(boolean secure) throws Exception {
//        FlatFileTrancheServer ffdfs = null;
//        Server server = null;
//        TrancheServer rdfs = null;
//        try {
//            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffdfs.getConfiguration().getUsers().add(DevUtil.getDevUser());
//            ffdfs.saveConfiguration();
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort(), secure);
//            server.start();
//            rdfs = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            TrancheServerTest.testSetConfiguration(rdfs, DevUtil.getDevUser(), DevUtil.getDevPrivateKey());
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//    }
//
//    public void testDeleteData() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteData()");
//        testDeleteData(false);
//    }
//
//    public void testDeleteDataSecure() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteDataSecure()");
//        testDeleteData(true);
//    }
//
//    public void testDeleteData(boolean secure) throws Exception {
//        FlatFileTrancheServer ffdfs = null;
//        Server server = null;
//        TrancheServer rdfs = null;
//        try {
//            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffdfs.getConfiguration().getUsers().add(DevUtil.getDevUser());
//            ffdfs.saveConfiguration();
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort(), secure);
//            server.start();
//            rdfs = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            TrancheServerTest.testDeleteData(rdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//    }
//
//    public void testDeleteMetaData() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteMetaData()");
//        testDeleteMetaData(false);
//    }
//
//    public void testDeleteMetaDataSecure() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteMetaDataSecure()");
//        testDeleteMetaData(true);
//    }
//
//    public void testDeleteMetaData(boolean secure) throws Exception {
//        FlatFileTrancheServer ffdfs = null;
//        Server server = null;
//        TrancheServer rdfs = null;
//        try {
//            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffdfs.getConfiguration().getUsers().add(DevUtil.getDevUser());
//            ffdfs.saveConfiguration();
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort(), secure);
//            server.start();
//            rdfs = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            TrancheServerTest.testDeleteMetaData(rdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//    }
//
//    public void testDeleteMetaDataUploader() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testDeleteMetaDataUploader()");
//        FlatFileTrancheServer ffdfs = null;
//        Server server = null;
//        TrancheServer rdfs = null;
//        try {
//            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffdfs.getConfiguration().getUsers().add(DevUtil.getDevUser());
//            ffdfs.saveConfiguration();
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort(), false);
//            server.start();
//            rdfs = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            TrancheServerTest.testDeleteMetaDataUploader(rdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//    }
//
//    public void testGetNetworkStatusPortion() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testGetNetworkStatusPortion()");
//        FlatFileTrancheServer ffdfs = null;
//        Server server = null;
//        TrancheServer rdfs = null;
//        try {
//            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort(), false);
//            server.start();
//            rdfs = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            TrancheServerTest.testGetNetworkStatusTablePortion(rdfs);
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//    }
//
//    public void testUserCannotOverwriteMetaData() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testUserCannotOverwriteMetaData()");
//        testUserCannotOverwriteMetaData(false);
//    }
//
//    public void testUserCannotOverwriteMetaDataSecure() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testUserCannotOverwriteMetaDataSecure()");
//        testUserCannotOverwriteMetaData(true);
//    }
//
//    public void testUserCannotOverwriteMetaData(boolean secure) throws Exception {
//        FlatFileTrancheServer ffts = null;
//        Server server = null;
//        TrancheServer rts = null;
//        try {
//            ffts = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            UserZipFile user = DevUtil.makeNewUser();
//            user.setFlags(User.CAN_GET_CONFIGURATION | User.CAN_SET_META_DATA);
//            ffts.getConfiguration().addUser(user);
//            ffts.saveConfiguration();
//            server = new Server(ffts, DevUtil.getDefaultTestPort(), secure);
//            server.start();
//            rts = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            TrancheServerTest.testUserCannotOverwriteMetaData(rts, user);
//        } finally {
//            IOUtil.safeClose(rts);
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffts);
//        }
//    }
//
//    public void testSetBigMetaData() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testSetBigMetaData()");
//        testSetBigMetaData(false);
//    }
//
//    public void testSetBigMetaDataSecure() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testSetBigMetaDataSecure()");
//        testSetBigMetaData(true);
//    }
//
//    public void testSetBigMetaData(boolean secure) throws Exception {
//        FlatFileTrancheServer ffdfs = null;
//        Server server = null;
//        TrancheServer rdfs = null;
//        try {
//            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffdfs.getConfiguration().getUsers().add(DevUtil.getDevUser());
//            ffdfs.saveConfiguration();
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort(), secure);
//            server.start();
//            rdfs = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            TrancheServerTest.testSetBigMetaData(rdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//    }
//
//    public void testHasDataAndMeta() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testHasDataAndMeta()");
//        FlatFileTrancheServer ffserver = null;
//        Server server = null;
//        TrancheServer rts = null;
//        try {
//            ffserver = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffserver.getConfiguration().addUser(DevUtil.getDevUser());
//            ffserver.saveConfiguration();
//            server = new Server(ffserver, DevUtil.getDefaultTestPort(), false);
//            server.start();
//            rts = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            TrancheServerTest.testHasDataAndMeta(rts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffserver);
//        }
//    }
//
//    public void testAdminCanRequestShutdown() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testAdminCanRequestShutdown()");
//        FlatFileTrancheServer ffts = null;
//        Server server = null;
//        TrancheServer ts = null;
//        try {
//            ffts = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            ffts.getConfiguration().getUsers().add(DevUtil.getDevUser());
//            ffts.saveConfiguration();
//            server = new Server(ffts, 1500, false);
//            server.start();
//            ts = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            IOUtil.requestShutdown(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//
//            // Wait several seconds
//            Thread.sleep(5000);
//
//            assertTrue("Server should be stopped.", server.isStopped());
//        } finally {
//            IOUtil.safeClose(ts);
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffts);
//        }
//    }
//
//    public void testNonAdminCannotRequestShutdown() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testNonAdminCannotRequestShutdown()");
//        FlatFileTrancheServer ffts = null;
//        Server server = null;
//        TrancheServer ts = null;
//        try {
//            ffts = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            // Create a non-admin user
//            File userFile = TempFileUtil.createTemporaryFile(".zip.encrypted");
//            UserZipFile uzf = DevUtil.createUser("bryan", "smith", userFile.getAbsolutePath(), false, false);
//            ffts.getConfiguration().getUsers().add(uzf);
//            ffts.saveConfiguration();
//            server = new Server(ffts, 1500, false);
//            server.start();
//            ts = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            try {
//                IOUtil.requestShutdown(ts, uzf.getCertificate(), uzf.getPrivateKey());
//                fail("Should have thrown an exception.");
//            } catch (Exception e) {
//            }
//
//            // Wait several seconds
//            Thread.sleep(5000);
//
//            assertFalse("Server should not be stopped.", server.isStopped());
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffts);
//        }
//    }
//
//    public void testUnrecognizedUserCannotRequestShutdown() throws Exception {
//        TestUtil.printTitle("RemoteTrancheServerTest:testUnrecognizedUserCannotRequestShutdown()");
//        FlatFileTrancheServer ffts = null;
//        Server server = null;
//        TrancheServer ts = null;
//        try {
//            ffts = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
//            // Create a non-admin user
//            File userFile = TempFileUtil.createTemporaryFile(".zip.encrypted");
//            UserZipFile uzf = DevUtil.createUser("bryan", "smith", userFile.getAbsolutePath(), false, false);
//            server = new Server(ffts, 1500, false);
//            server.start();
//            ts = ConnectionUtil.connect(server.getHostName(), server.getPort(), server.isSSL(), true);
//
//            try {
//                IOUtil.requestShutdown(ts, uzf.getCertificate(), uzf.getPrivateKey());
//                fail("Should have thrown an exception.");
//            } catch (Exception e) {
//            }
//
//            // Wait several seconds
//            Thread.sleep(5000);
//
//            assertFalse("Server should not be stopped.", server.isStopped());
//        } finally {
//            ConnectionUtil.safeForceClose(server.getHostName(), "Ending test.");
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffts);
//        }
//    }
}
