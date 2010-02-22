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
package org.tranche.flatfile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.network.*;
import org.tranche.server.*;
import org.tranche.meta.*;
import org.tranche.util.*;
/**
 * Tests the thread that pushes MD/data chunks to server w/ proper hash span.
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith <bryanesmith at gmail dot com>
 */
public class HashSpanFixingThreadTest extends TrancheTestCase {

    private final boolean wasHealingThreadRunning;

    public HashSpanFixingThreadTest() {
        wasHealingThreadRunning = TestUtil.isTestingHashSpanFixingThread();
        HashSpanFixingThread.setTimeToSleepIfNotAllowedToRun(500);
    }

    @Override()
    public void setUp() {
        TestUtil.setTestingHashSpanFixingThread(true);
    }

    @Override()
    public void tearDown() {
        TestUtil.setTestingHashSpanFixingThread(wasHealingThreadRunning);
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) { /* */ }
    }
    /**
     * Create a set of table rows. Will be used for each test
     */
    final static String HOST1 = "ardvark.org";
    final static String HOST2 = "batman.org";
    final static String HOST3 = "catwoman.org";
    final static String HOST4 = "darwin.edu";
    final static String HOST5 = "edgar.com";
    final static String HOST6 = "friday.com";
    /**
     * <p>If need to duplicate a sporadic problem, increase the number. Else, leave at 1 to run each test once.</p>
     */
    final static int RUN_EACH_TEST_COUNT = 1;
    /**
     * <p>Number of iterations to wait (in each test) for healing thread to do its job.</p>
     * <p>Ideally, one; however, test network must update and recognize updates. Try adjusting this number.</p>
     * <p>A good goal would be 2, as healing thread might already be in middle of iteration (so can't be 1).</p>
     */
//    final static int NUM_HEALING_THREAD_ITS_TO_WAIT = 2;
    final static int NUM_HEALING_THREAD_ITS_TO_WAIT = 3;
    
    /**
     * <p>Simple test:</p>
     * <ul>
     *   <li>Add bunch of chunks to a single server (two servers get nothing)</li>
     *   <li>Split hash span more-or-less evenly in three across servers.</li>
     *   <li>Make sure one copy in correct server.</li>
     * </ul>
     * <p>In this test, the server that originally received chunks runs healing thread, but other servers do NOT.</p>
     * @throws java.lang.Exception
     */
    public void testReplicateChunksAndDelete() throws Exception {
        final int chunks = 5;
        for (int i = 0; i < RUN_EACH_TEST_COUNT; i++) {
            System.out.println("--- --- --- --- --- testReplicateChunksAndDelete #" + String.valueOf(i + 1) + " of " + RUN_EACH_TEST_COUNT + " --- --- --- --- ---");
            replicateChunksAndDelete(chunks);
        }
    }

    private void replicateChunksAndDelete(final int chunksToAdd) throws Exception {

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            Thread.sleep(1000);

            assertTrue("Should be online: " + HOST1, NetworkUtil.getStatus().getRow(HOST1).isOnline());
            assertTrue("Should be online: " + HOST2, NetworkUtil.getStatus().getRow(HOST2).isOnline());
            assertTrue("Should be online: " + HOST3, NetworkUtil.getStatus().getRow(HOST3).isOnline());

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull("Should have found ffts for host: " + HOST1, ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull("Should have found ffts for host: " + HOST2, ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull("Should have found ffts for host: " + HOST3, ffts3);

            assertEquals("Expecting ffts to have certain name.", HOST1, ffts1.getHost());
            assertEquals("Expecting ffts to have certain name.", HOST2, ffts2.getHost());
            assertEquals("Expecting ffts to have certain name.", HOST3, ffts3.getHost());

            Server s1 = testNetwork.getServer(HOST1);
            assertNotNull("Server shouldn't be null.", s1);

            Server s2 = testNetwork.getServer(HOST2);
            assertNotNull("Server shouldn't be null.", s2);

            Server s3 = testNetwork.getServer(HOST3);
            assertNotNull("Server shouldn't be null.", s3);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("Remote server shouldn't be null.", ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("Remote server shouldn't be null.", ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("Remote server shouldn't be null.", ts3);

            Set<BigHash> dataHashes = new HashSet();
            Set<BigHash> metaHashes = new HashSet();

            for (int i = 0; i < chunksToAdd; i++) {

                // Data chunk
                byte[] dataChunk = DevUtil.createRandomDataChunkVariableSize();
                BigHash dataHash = new BigHash(dataChunk);

                if (dataHashes.contains(dataHash)) {
                    fail("Generated same data chunk twice or hash collision?");
                }

                IOUtil.setData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHash, dataChunk);
                dataHashes.add(dataHash);

                // Meta data chunk
                byte[] metaChunk = DevUtil.createRandomMetaDataChunk();
                BigHash metaHash = DevUtil.getRandomBigHash();

                if (metaHashes.contains(metaHash)) {
                    fail("Generated same meta data chunk twice or hash collision?");
                }

                IOUtil.setMetaData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaHash, metaChunk);
                metaHashes.add(metaHash);
            }

            for (BigHash h : dataHashes) {
                assertTrue("Server should have chunk", ffts1.getDataBlockUtil().hasData(h));
                assertFalse("Server should not have chunk yet", ffts2.getDataBlockUtil().hasData(h));
                assertFalse("Server should not have chunk yet", ffts3.getDataBlockUtil().hasData(h));
            }
            for (BigHash h : metaHashes) {
                assertTrue("Server should have chunk", ffts1.getDataBlockUtil().hasMetaData(h));
                assertFalse("Server should not have chunk yet", ffts2.getDataBlockUtil().hasMetaData(h));
                assertFalse("Server should not have chunk yet", ffts3.getDataBlockUtil().hasMetaData(h));
            }

            Set<HashSpan>[] spans = getThreeHashSpans();

            // Set three hash spans
            Configuration config1 = ffts1.getConfiguration();
            config1.setHashSpans(spans[0]);
            config1.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, String.valueOf(true));
            config1.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_DELETE, String.valueOf(true));
            config1.setValue(ConfigKeys.HASHSPANFIX_NUMBER_REPS_IN_HASH_SPAN_REQUIRED_DELETE, String.valueOf(1));
            config1.setValue(ConfigKeys.HASHSPANFIX_NUMBER_REPS_REQUIRED_DELETE, String.valueOf(1));
            ffts1.setConfiguration(config1);

            Configuration config2 = ffts2.getConfiguration();
            config2.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, String.valueOf(false));
            config2.setHashSpans(spans[1]);
            ffts2.setConfiguration(config2);

            Configuration config3 = ffts3.getConfiguration();
            config3.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, String.valueOf(false));
            config3.setHashSpans(spans[2]);
            ffts3.setConfiguration(config3);

            testNetwork.updateNetwork();

            // 
            for (int i = 0; i < NUM_HEALING_THREAD_ITS_TO_WAIT; i++) {
                long start = System.currentTimeMillis();
                try {
                    ffts1.getHashSpanFixingThread().waitForIteration();
                } finally {
                    System.err.println("DEBUG> Waited for iteration #" + String.valueOf(i + 1) + " of " + NUM_HEALING_THREAD_ITS_TO_WAIT + ": " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
                }
            }

            int count = 0;
            for (BigHash h : dataHashes) {
                count++;
                FlatFileTrancheServer ffts = ffts1;
                if (spansContainHash(ffts.getConfiguration().getHashSpans(), h)) {
                    assertTrue("Should have data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasData(h));
                } else {
                    assertFalse("Should not have data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasData(h));
                }
                ffts = ffts2;
                if (spansContainHash(ffts.getConfiguration().getHashSpans(), h)) {
                    assertTrue("Should have data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasData(h));
                } else {
                    assertFalse("Should not have data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasData(h));
                }
                ffts = ffts3;
                if (spansContainHash(ffts.getConfiguration().getHashSpans(), h)) {
                    assertTrue("Should have data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasData(h));
                } else {
                    assertFalse("Should not have data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasData(h));
                }
            }

            count = 0;
            for (BigHash h : metaHashes) {
                FlatFileTrancheServer ffts = ffts1;
                if (spansContainHash(ffts.getConfiguration().getHashSpans(), h)) {
                    assertTrue("Should have meta data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasMetaData(h));
                } else {
                    assertFalse("Should not have meta data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasMetaData(h));
                }
                ffts = ffts2;
                if (spansContainHash(ffts.getConfiguration().getHashSpans(), h)) {
                    assertTrue("Should have meta data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasMetaData(h));
                } else {
                    assertFalse("Should not have meta data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasMetaData(h));
                }
                ffts = ffts3;
                if (spansContainHash(ffts.getConfiguration().getHashSpans(), h)) {
                    assertTrue("Should have meta data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasMetaData(h));
                } else {
                    assertFalse("Should not have meta data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasMetaData(h));
                }
            }

        } finally {
            testNetwork.stop();
        }
    }
    
    /**
     * <p>Simple test:</p>
     * <ul>
     *   <li>Add bunch of chunks to a single server (two servers get nothing)</li>
     *   <li>Split hash span more-or-less evenly in three across servers.</li>
     *   <li>Servers without copies should grab the appropriate chunks</li>
     * </ul>
     * <p>In this test, the server that originally received chunks does NOT run healing thread, but other servers do.</p>
     * @throws java.lang.Exception
     */
    public void testGrabAppropriateChunks() throws Exception {
        final int chunks = 5;
        for (int i = 0; i < RUN_EACH_TEST_COUNT; i++) {
            System.out.println("--- --- --- --- --- testGrabAppropriateChunks #" + String.valueOf(i + 1) + " of " + RUN_EACH_TEST_COUNT + " --- --- --- --- ---");
            grabAppropriateChunks(chunks);
        }
    }

    private void grabAppropriateChunks(final int chunksToAdd) throws Exception {

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            Thread.sleep(1000);

            assertTrue("Should be online: " + HOST1, NetworkUtil.getStatus().getRow(HOST1).isOnline());
            assertTrue("Should be online: " + HOST2, NetworkUtil.getStatus().getRow(HOST2).isOnline());
            assertTrue("Should be online: " + HOST3, NetworkUtil.getStatus().getRow(HOST3).isOnline());

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull("Should have found ffts for host: " + HOST1, ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull("Should have found ffts for host: " + HOST2, ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull("Should have found ffts for host: " + HOST3, ffts3);

            assertEquals("Expecting ffts to have certain name.", HOST1, ffts1.getHost());
            assertEquals("Expecting ffts to have certain name.", HOST2, ffts2.getHost());
            assertEquals("Expecting ffts to have certain name.", HOST3, ffts3.getHost());

            Server s1 = testNetwork.getServer(HOST1);
            assertNotNull("Server shouldn't be null.", s1);

            Server s2 = testNetwork.getServer(HOST2);
            assertNotNull("Server shouldn't be null.", s2);

            Server s3 = testNetwork.getServer(HOST3);
            assertNotNull("Server shouldn't be null.", s3);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("Remote server shouldn't be null.", ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("Remote server shouldn't be null.", ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("Remote server shouldn't be null.", ts3);

            Set<BigHash> dataHashes = new HashSet();
            Set<BigHash> metaHashes = new HashSet();

            for (int i = 0; i < chunksToAdd; i++) {

                // Data chunk
                byte[] dataChunk = DevUtil.createRandomDataChunkVariableSize();
                BigHash dataHash = new BigHash(dataChunk);

                if (dataHashes.contains(dataHash)) {
                    fail("Generated same data chunk twice or hash collision?");
                }

                IOUtil.setData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHash, dataChunk);
                dataHashes.add(dataHash);

                // Meta data chunk
                byte[] metaChunk = DevUtil.createRandomMetaDataChunk();
                BigHash metaHash = DevUtil.getRandomBigHash();

                if (metaHashes.contains(metaHash)) {
                    fail("Generated same meta data chunk twice or hash collision?");
                }

                IOUtil.setMetaData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaHash, metaChunk);
                metaHashes.add(metaHash);
            }

            for (BigHash h : dataHashes) {
                assertTrue("Server should have chunk", ffts1.getDataBlockUtil().hasData(h));
                assertFalse("Server should not have chunk yet", ffts2.getDataBlockUtil().hasData(h));
                assertFalse("Server should not have chunk yet", ffts3.getDataBlockUtil().hasData(h));
            }
            for (BigHash h : metaHashes) {
                assertTrue("Server should have chunk", ffts1.getDataBlockUtil().hasMetaData(h));
                assertFalse("Server should not have chunk yet", ffts2.getDataBlockUtil().hasMetaData(h));
                assertFalse("Server should not have chunk yet", ffts3.getDataBlockUtil().hasMetaData(h));
            }

            Set<HashSpan>[] spans = getThreeHashSpans();

            // Set three hash spans. 
            //
            // Note that healing thread for first server will not run (no deleting nor injecting);
            // other servers will grab, though.
            Configuration config1 = ffts1.getConfiguration();
            config1.setHashSpans(spans[0]);
            config1.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, String.valueOf(false));
            ffts1.setConfiguration(config1);

            Configuration config2 = ffts2.getConfiguration();
            config2.setHashSpans(spans[1]);
            config2.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, String.valueOf(true));
            ffts2.setConfiguration(config2);

            Configuration config3 = ffts3.getConfiguration();
            config3.setHashSpans(spans[2]);
            config3.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, String.valueOf(true));
            ffts3.setConfiguration(config3);

            testNetwork.updateNetwork();

            // Allow do job
            for (int i = 0; i < NUM_HEALING_THREAD_ITS_TO_WAIT; i++) {
                long start = System.currentTimeMillis();
                try {
                    ffts2.getHashSpanFixingThread().waitForIteration();
                } finally {
                    System.err.println("DEBUG> Waited for iteration #" + String.valueOf(i + 1) + " of " + NUM_HEALING_THREAD_ITS_TO_WAIT + ": " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
                }
            }

            int count = 0;
            for (BigHash h : dataHashes) {
                count++;
                
                // First server should have everything since doesn't delete
                assertTrue("Should have data chunk #" + count + ": " + ffts1.getHost() + " [" + h + "]", ffts1.getDataBlockUtil().hasData(h));
                
                FlatFileTrancheServer ffts = ffts2;
                if (spansContainHash(ffts.getConfiguration().getHashSpans(), h)) {
                    assertTrue("Should have data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasData(h));
                } else {
                    assertFalse("Should not have data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasData(h));
                }
                ffts = ffts3;
                if (spansContainHash(ffts.getConfiguration().getHashSpans(), h)) {
                    assertTrue("Should have data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasData(h));
                } else {
                    assertFalse("Should not have data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasData(h));
                }
            }

            count = 0;
            for (BigHash h : metaHashes) {
                
                // First server should have everything since doesn't delete
                assertTrue("Should have meta data chunk #" + count + ": " + ffts1.getHost() + " [" + h + "]", ffts1.getDataBlockUtil().hasMetaData(h));
                
                FlatFileTrancheServer ffts = ffts2;
                if (spansContainHash(ffts.getConfiguration().getHashSpans(), h)) {
                    assertTrue("Should have meta data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasMetaData(h));
                } else {
                    assertFalse("Should not have meta data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasMetaData(h));
                }
                ffts = ffts3;
                if (spansContainHash(ffts.getConfiguration().getHashSpans(), h)) {
                    assertTrue("Should have meta data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasMetaData(h));
                } else {
                    assertFalse("Should not have meta data chunk #" + count + ": " + ffts.getHost() + " [" + h + "]", ffts.getDataBlockUtil().hasMetaData(h));
                }
            }

        } finally {
            testNetwork.stop();
        }
    }
    
    /**
     * <p>Three servers with full hash span. Test works as follows.</p>
     * <ul>
     *   <li>Create certain number of data and meta data chunks. Create good and bad copy of each.</li>
     *   <li>Set one of each good and bad copy to random server.</p>
     *   <li>Wait and verify repaired and that every server has a copy.</li>
     * </ul>
     * @throws java.lang.Exception
     */
    public void testHealChunks() throws Exception {
        final int chunksToAdd = 5;
        for (int i = 0; i < RUN_EACH_TEST_COUNT; i++) {
            System.out.println("--- --- --- --- --- testHealChunks #" + String.valueOf(i + 1) + " of " + RUN_EACH_TEST_COUNT + " --- --- --- --- ---");
            healChunks(chunksToAdd);
        }
    }
    
    private void healChunks(final int chunksToAdd) throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            Thread.sleep(1000);

            assertTrue("Should be online: " + HOST1, NetworkUtil.getStatus().getRow(HOST1).isOnline());
            assertTrue("Should be online: " + HOST2, NetworkUtil.getStatus().getRow(HOST2).isOnline());
            assertTrue("Should be online: " + HOST3, NetworkUtil.getStatus().getRow(HOST3).isOnline());

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull("Should have found ffts for host: " + HOST1, ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull("Should have found ffts for host: " + HOST2, ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull("Should have found ffts for host: " + HOST3, ffts3);

            assertEquals("Expecting ffts to have certain name.", HOST1, ffts1.getHost());
            assertEquals("Expecting ffts to have certain name.", HOST2, ffts2.getHost());
            assertEquals("Expecting ffts to have certain name.", HOST3, ffts3.getHost());

            Server s1 = testNetwork.getServer(HOST1);
            assertNotNull("Server shouldn't be null.", s1);

            Server s2 = testNetwork.getServer(HOST2);
            assertNotNull("Server shouldn't be null.", s2);

            Server s3 = testNetwork.getServer(HOST3);
            assertNotNull("Server shouldn't be null.", s3);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("Remote server shouldn't be null.", ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("Remote server shouldn't be null.", ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("Remote server shouldn't be null.", ts3);

            Set<BigHash> dataHashes = new HashSet();
            Set<BigHash> metaHashes = new HashSet();
            
            for (int i = 0; i < chunksToAdd; i++) {

                // Data chunk
                byte[] dataChunkGood = DevUtil.createRandomDataChunkVariableSize();
                byte[] dataChunkBad = DevUtil.createRandomDataChunkVariableSize();
                BigHash dataHash = new BigHash(dataChunkGood);

                if (dataHashes.contains(dataHash)) {
                    fail("Generated same data chunk twice or hash collision?");
                }

                int goodDataChunkRecipient = RandomUtil.getInt(3);
                int badDataChunkRecipient = RandomUtil.getInt(3);
                while (badDataChunkRecipient == goodDataChunkRecipient) {
                    badDataChunkRecipient = RandomUtil.getInt(3);
                }
                
                // Set good chunk
                switch (goodDataChunkRecipient) {
                    case 0:
                        ffts1.getDataBlockUtil().addData(dataHash, dataChunkGood);
                        break;
                    case 1:
                        ffts2.getDataBlockUtil().addData(dataHash, dataChunkGood);
                        break;
                    case 2:
                        ffts2.getDataBlockUtil().addData(dataHash, dataChunkGood);
                        break;
                    default:
                        fail("Unrecognized value: "+goodDataChunkRecipient);
                }
                
                // Set bad chunk
                switch (badDataChunkRecipient) {
                    case 0:
                        ffts1.getDataBlockUtil().addData(dataHash, dataChunkBad);
                        break;
                    case 1:
                        ffts2.getDataBlockUtil().addData(dataHash, dataChunkBad);
                        break;
                    case 2:
                        ffts2.getDataBlockUtil().addData(dataHash, dataChunkBad);
                        break;
                    default:
                        fail("Unrecognized value: "+badDataChunkRecipient);
                }

                // Meta data chunk
                byte[] metaChunkGood = DevUtil.createRandomMetaDataChunk();
                byte[] metaChunkBad = DevUtil.createRandomDataChunkVariableSize();
                BigHash metaHash = DevUtil.getRandomBigHash();

                if (metaHashes.contains(metaHash)) {
                    fail("Generated same meta data chunk twice or hash collision?");
                }

                int goodMetaChunkRecipient = RandomUtil.getInt(3);
                int badMetaChunkRecipient = RandomUtil.getInt(3);
                while (badMetaChunkRecipient == goodMetaChunkRecipient) {
                    badMetaChunkRecipient = RandomUtil.getInt(3);
                }
                
                // Set good chunk
                switch (goodMetaChunkRecipient) {
                    case 0:
                        ffts1.getDataBlockUtil().addMetaData(metaHash, metaChunkGood);
                        break;
                    case 1:
                        ffts2.getDataBlockUtil().addMetaData(metaHash, metaChunkGood);
                        break;
                    case 2:
                        ffts2.getDataBlockUtil().addMetaData(metaHash, metaChunkGood);
                        break;
                    default:
                        fail("Unrecognized value: "+goodMetaChunkRecipient);
                }
                
                // Set bad chunk
                switch (badMetaChunkRecipient) {
                    case 0:
                        ffts1.getDataBlockUtil().addMetaData(metaHash, metaChunkBad);
                        break;
                    case 1:
                        ffts2.getDataBlockUtil().addMetaData(metaHash, metaChunkBad);
                        break;
                    case 2:
                        ffts2.getDataBlockUtil().addMetaData(metaHash, metaChunkBad);
                        break;
                    default:
                        fail("Unrecognized value: "+badMetaChunkRecipient);
                }
            }

            Set<HashSpan> fullHashSpanSet = new HashSet();
            fullHashSpanSet.add(HashSpan.FULL);
            
            // Set three hash spans. 
            //
            // Note that healing thread for first server will not run (no deleting nor injecting);
            // other servers will grab, though.
            Configuration config1 = ffts1.getConfiguration();
            config1.setHashSpans(fullHashSpanSet);
            config1.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, String.valueOf(true));
            ffts1.setConfiguration(config1);

            Configuration config2 = ffts2.getConfiguration();
            config2.setHashSpans(fullHashSpanSet);
            config2.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, String.valueOf(true));
            ffts2.setConfiguration(config2);

            Configuration config3 = ffts3.getConfiguration();
            config3.setHashSpans(fullHashSpanSet);
            config3.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, String.valueOf(true));
            ffts3.setConfiguration(config3);

            testNetwork.updateNetwork();

            // Allow do job
            for (int i = 0; i < NUM_HEALING_THREAD_ITS_TO_WAIT; i++) {
                long start = System.currentTimeMillis();
                try {
                    ffts2.getHashSpanFixingThread().waitForIteration();
                } finally {
                    System.err.println("DEBUG> Waited for iteration #" + String.valueOf(i + 1) + " of " + NUM_HEALING_THREAD_ITS_TO_WAIT + ": " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
                }
            }

            int count = 0;
            for (BigHash h : dataHashes) {
                count++;
                
                // FFTS #1
                assertTrue("Should have data chunk #" + count + ": " + ffts1.getHost() + " [" + h + "]", ffts1.getDataBlockUtil().hasData(h));
                byte[] dataChunk = ffts1.getDataBlockUtil().getData(h);
                BigHash verifyHash = new BigHash(dataChunk);
                assertEquals("Hashes should equal.", h, verifyHash);
                
                // FFTS #2
                assertTrue("Should have data chunk #" + count + ": " + ffts2.getHost() + " [" + h + "]", ffts2.getDataBlockUtil().hasData(h));
                dataChunk = ffts2.getDataBlockUtil().getData(h);
                verifyHash = new BigHash(dataChunk);
                assertEquals("Hashes should equal.", h, verifyHash);
                
                // FFTS #3
                assertTrue("Should have data chunk #" + count + ": " + ffts3.getHost() + " [" + h + "]", ffts3.getDataBlockUtil().hasData(h));
                dataChunk = ffts3.getDataBlockUtil().getData(h);
                verifyHash = new BigHash(dataChunk);
                assertEquals("Hashes should equal.", h, verifyHash);
            }

            count = 0;
            for (BigHash h : metaHashes) {
                count++;
                
                // FFTS #1
                assertTrue("Should have meta data chunk #" + count + ": " + ffts1.getHost() + " [" + h + "]", ffts1.getDataBlockUtil().hasMetaData(h));
                byte[] metaChunk = ffts1.getDataBlockUtil().getMetaData(h);
                
                ByteArrayInputStream bais = null;
                try {
                    bais = new ByteArrayInputStream(metaChunk);
                    MetaData md = MetaDataUtil.read(bais);
                    if (md == null) {
                        fail("Meta data was null; should have thrown exception.");
                    }
                } finally {
                    IOUtil.safeClose(bais);
                }
                
                // FFTS #2
                assertTrue("Should have meta data chunk #" + count + ": " + ffts2.getHost() + " [" + h + "]", ffts2.getDataBlockUtil().hasMetaData(h));
                metaChunk = ffts2.getDataBlockUtil().getMetaData(h);
                
                bais = null;
                try {
                    bais = new ByteArrayInputStream(metaChunk);
                    MetaData md = MetaDataUtil.read(bais);
                    if (md == null) {
                        fail("Meta data was null; should have thrown exception.");
                    }
                } finally {
                    IOUtil.safeClose(bais);
                }
                
                // FFTS #3
                assertTrue("Should have meta data chunk #" + count + ": " + ffts3.getHost() + " [" + h + "]", ffts3.getDataBlockUtil().hasMetaData(h));
                metaChunk = ffts3.getDataBlockUtil().getMetaData(h);
                
                bais = null;
                try {
                    bais = new ByteArrayInputStream(metaChunk);
                    MetaData md = MetaDataUtil.read(bais);
                    if (md == null) {
                        fail("Meta data was null; should have thrown exception.");
                    }
                } finally {
                    IOUtil.safeClose(bais);
                }
            }

        } finally {
            testNetwork.stop();
        }
    }
    
    /**
     * <ul>
     *  <li>Start server with one data directory and add bunch of chunks</li>
     *  <li>Add two more data directories</li>
     *  <li>Verify balancing occurs</li>
     * </ul>
     * @throws java.lang.Exception
     */
    public void testCanBalance() throws Exception {
        for (int i = 0; i < RUN_EACH_TEST_COUNT; i++) {
            System.out.println("--- --- --- --- --- testCanBalance #" + String.valueOf(i + 1) + " of " + RUN_EACH_TEST_COUNT + " --- --- --- --- ---");
            canBalance();
        }
    }

    private void canBalance() throws Exception {
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        File dir2 = null, dir3 = null;
        try {
            testNetwork.start();

            Thread.sleep(1000);

            assertTrue("Should be online: " + HOST1, NetworkUtil.getStatus().getRow(HOST1).isOnline());

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull("Should have found ffts for host: " + HOST1, ffts1);

            assertEquals("Expecting ffts to have certain name.", HOST1, ffts1.getHost());

            Server s1 = testNetwork.getServer(HOST1);
            assertNotNull("Server shouldn't be null.", s1);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("Remote server shouldn't be null.", ts1);

            Configuration config = ffts1.getConfiguration();
            assertEquals("Expecting one data directory.", 1, config.getDataDirectories().size());

            // Set the size
            int chunkCount = 10;
            int maxSize = chunkCount * 1024 * 1024;
            File dir1 = null;
            for (DataDirectoryConfiguration ddc : config.getDataDirectories()) {
                ddc.setSizeLimit(maxSize);
                dir1 = ddc.getDirectoryFile();
            }

            assertTrue("The directory we are using must exist.", dir1.exists());

            Set<BigHash> dataHashes = new HashSet();
            Set<BigHash> metaHashes = new HashSet();

            // Need to take up at least 50% for test
            long used = 0;
            for (int i = 0; i < chunkCount; i++) {
                final int size = RandomUtil.getInt(1024 * 512) + 1024 * 512;
                byte[] dataChunk = DevUtil.createRandomDataChunk(size);
                used += dataChunk.length;
                BigHash dataHash = new BigHash(dataChunk);

                ffts1.getDataBlockUtil().addData(dataHash, dataChunk);
                dataHashes.add(dataHash);

                byte[] metaChunk = DevUtil.createRandomMetaDataChunk();
                used += metaChunk.length;
                BigHash metaHash = DevUtil.getRandomBigHash();

                ffts1.getDataBlockUtil().addMetaData(metaHash, metaChunk);
                metaHashes.add(metaHash);
            }

            assertTrue("Added more than half total size.", used > ((double) maxSize) / 2.0);
            assertTrue("Should be less than limit.", used < maxSize);

            assertTrue("Expecting used space at least certain size, but "+used+" not <= "+getBytesUsedInDirectory(dir1), used <=  getBytesUsedInDirectory(dir1));

            for (BigHash h : dataHashes) {
                assertTrue("Should have.", ffts1.getDataBlockUtil().hasData(h));
            }

            for (BigHash h : metaHashes) {
                assertTrue("Should have.", ffts1.getDataBlockUtil().hasMetaData(h));
            }

            dir2 = TempFileUtil.createTemporaryDirectory();
            dir3 = TempFileUtil.createTemporaryDirectory();
            assertNotSame("Directories should be different.", dir2, dir3);

            DataDirectoryConfiguration ddc2 = new DataDirectoryConfiguration(dir2.getAbsolutePath(), maxSize);
            DataDirectoryConfiguration ddc3 = new DataDirectoryConfiguration(dir3.getAbsolutePath(), maxSize);

            config.addDataDirectory(ddc2);
            config.addDataDirectory(ddc3);

            config.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, String.valueOf(true));
            config.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_BALANCE, String.valueOf(true));
            config.setValue(ConfigKeys.HASHSPANFIX_REQUIRED_PERCENTAGE_FOR_MOST_USED_DATA_DIRECTORY_BEFORE_BALANCE, String.valueOf(25.0));
            config.setValue(ConfigKeys.HASHSPANFIX_REQUIRED_PERCENTAGE_DIFFERENCE_TO_BALANCE_DATA_DIRECTORIES, String.valueOf(10.0));
            config.setValue(ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_BALANCE_DIRECTORIES, String.valueOf(10));
            config.setValue(ConfigKeys.HASHSPANFIX_REQUIRED_MIN_USED_BYTES_IN_MAX_DATABLOCK_TO_BALANCE_DATA_DIRECTORIES, String.valueOf(1024 * 1024));

            ffts1.setConfiguration(config);

            assertEquals("Expecting three data directory.", 3, config.getDataDirectories().size());

            testNetwork.updateNetwork();

            // Allow do job
            for (int i = 0; i < NUM_HEALING_THREAD_ITS_TO_WAIT; i++) {
                long start = System.currentTimeMillis();
                try {
                    ffts1.getHashSpanFixingThread().waitForIteration();
                } finally {
                    System.err.println("DEBUG> Waited for iteration #" + String.valueOf(i + 1) + " of " + NUM_HEALING_THREAD_ITS_TO_WAIT + ": " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
                }
            }
            Text.printRecursiveDirectoryStructure(dir1);
            Text.printRecursiveDirectoryStructure(dir2);
            Text.printRecursiveDirectoryStructure(dir3);
            
            assertNotSame("Should not be empty: "+dir1.getAbsolutePath(), 0, getBytesUsedInDirectory(dir1));
            assertNotSame("Should not be empty: "+dir2.getAbsolutePath(), 0, getBytesUsedInDirectory(dir2));
            assertNotSame("Should not be empty: "+dir3.getAbsolutePath(), 0, getBytesUsedInDirectory(dir3));
            assertTrue("Expecting used space at least certain size, but "+used+" not > "+getBytesUsedInDirectory(dir1), used > getBytesUsedInDirectory(dir1));
            assertTrue("Expecting used space at least certain size, but "+used+" not > "+getBytesUsedInDirectory(dir2), used > getBytesUsedInDirectory(dir2));
            assertTrue("Expecting used space at least certain size, but "+used+" not > "+getBytesUsedInDirectory(dir3), used > getBytesUsedInDirectory(dir3));

        } finally {
            testNetwork.stop();
            IOUtil.recursiveDelete(dir2);
            IOUtil.recursiveDelete(dir3);
        }
    }

    private static long getBytesUsedInDirectory(File dir) {
        if (!dir.exists()) {
            throw new RuntimeException("Directory doesn't exist: " + dir.getAbsolutePath());
        }
        if (!dir.isDirectory()) {
            throw new RuntimeException("Not a directory: " + dir.isDirectory());
        }
        long total = 0;
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                throw new RuntimeException("Child [" + f.getName() + "] of directory [" + dir.getAbsolutePath() + "] is also a directory; supposed to be all regular files.");
            }
            total += f.length();
        }

        return total;
    }

    /**
     * <p>Quick helper method for debugging.</p>
     * @param config
     * @param msg
     */
    private static void printHashSpans(Configuration config, String msg) {
        System.out.println(msg + ":");
        for (HashSpan hs : config.getHashSpans()) {
            System.out.println("* First: " + hs.getFirst());
            System.out.println("   Last: " + hs.getLast());
        }
    }

    /**
     * <p>Helper method to see if hash fits in set of hash spans.</p>
     * @param spans
     * @param hash
     * @return
     */
    public boolean spansContainHash(Set<HashSpan> spans, BigHash hash) {
        for (HashSpan hs : spans) {
            if (hs.contains(hash)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Helper method to get three separate hash spans that cover entire hash span.</p>
     * @return
     */
    private Set<HashSpan>[] getThreeHashSpans() {

        Set<HashSpan>[] spans = new Set[3];

        BigHash startingHash = HashSpan.FIRST;
        byte[] endingHashBytes = new byte[startingHash.toByteArray().length];

        for (int i = 0; i < startingHash.toByteArray().length; i++) {
            endingHashBytes[i] = -42;
        }
        BigHash endingHash = BigHash.createFromBytes(endingHashBytes);

        Set<HashSpan> set = new HashSet();
        set.add(new HashSpan(startingHash, endingHash));
        spans[0] = set;

        // Build next hash span
        startingHash = endingHash;
        endingHashBytes = new byte[startingHash.toByteArray().length];

        for (int i = 0; i < startingHash.toByteArray().length; i++) {
            endingHashBytes[i] = 42;
        }
        endingHash = BigHash.createFromBytes(endingHashBytes);

        set = new HashSet();
        set.add(new HashSpan(startingHash, endingHash));
        spans[1] = set;

        // Build next hash span
        startingHash = endingHash;
        endingHashBytes = new byte[startingHash.toByteArray().length];

        for (int i = 0; i < startingHash.toByteArray().length; i++) {
            endingHashBytes[i] = Byte.MAX_VALUE;
        }
        endingHash = BigHash.createFromBytes(endingHashBytes);

        set = new HashSet();
        set.add(new HashSpan(startingHash, endingHash));
        spans[2] = set;

        return spans;
    }
}