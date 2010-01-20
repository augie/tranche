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

import java.util.HashSet;
import java.util.Set;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.hash.span.HashSpanCollection;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class TargetHashSpanThreadTest extends TrancheTestCase {

    private final boolean wasTestingTargetHashSpan = TestUtil.isTestingTargetHashSpan();
    private final boolean wasTestingHealingThread = TestUtil.isTestingHashSpanFixingThread();

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.setTestingTargetHashSpan(true);
        TestUtil.setTestingHashSpanFixingThread(false);
        TargetHashSpanThread.setDebug(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.setTestingTargetHashSpan(wasTestingTargetHashSpan);
        TestUtil.setTestingHashSpanFixingThread(wasTestingHealingThread);
        TargetHashSpanThread.setDebug(false);
    }

    /**
     * <p>Server with full hash span going to have target hash span of half. Make sure chunks replicated before delete.</p>
     * @throws java.lang.Exception
     */
    public void testConstriction() throws Exception {
        TestUtil.printTitle("TargetHashSpanThreadTest:testConstriction()");

        final int chunksToUse = 15;

        final String HOST1 = "aardvark.org",  HOST2 = "batman.com";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull("Should have ffts.", ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull("Should have ffts.", ffts2);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);

            assertFalse("Servers should have different host names.", ffts1.getHost().equals(ffts2.getHost()));

            ffts1.getConfiguration().setValue(ConfigKeys.HASHSPANFIX_NUMBER_REPS_IN_HASH_SPAN_REQUIRED_DELETE, String.valueOf(1));
            ffts1.getConfiguration().setValue(ConfigKeys.HASHSPANFIX_NUMBER_REPS_REQUIRED_DELETE, String.valueOf(1));

            // Just set this server's target hash span thread to recheck more frequently since going to test
            ffts1.getTargetHashSpanThread().setTimeToSleep(100);

            // Give each full hash span
            final Set<HashSpan> normalHashSpanSet = new HashSet();
            normalHashSpanSet.add(HashSpan.FULL);

            // Don't use normalHashSpanSet for target, or have bug: will TargetHashSpanThread and this test will
            // be modifying both normal and target hash spans whenever changes either (since set shared reference)
            //
            // Also, need separate collection for each server. Don't want to modify both servers just because change
            // one.
            final Set<HashSpan> targetHashSpanSet1 = new HashSet();
            final Set<HashSpan> targetHashSpanSet2 = new HashSet();
            targetHashSpanSet1.add(HashSpan.FULL);
            targetHashSpanSet2.add(HashSpan.FULL);

            ffts1.getConfiguration().setHashSpans(normalHashSpanSet);
            ffts1.getConfiguration().setTargetHashSpans(targetHashSpanSet1);
            ffts1.setConfiguration(ffts1.getConfiguration());

            ffts2.getConfiguration().setHashSpans(normalHashSpanSet);
            ffts2.getConfiguration().setTargetHashSpans(targetHashSpanSet2);
            ffts2.setConfiguration(ffts2.getConfiguration());

            testNetwork.updateNetwork();

            // Make sure configuration is correct
            assertEquals("Should only have one hash span.", 1, ffts1.getConfiguration().getHashSpans().size());
            HashSpan foundHashSpan = ffts1.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0];
            assertEquals("Should know starting hash span.", HashSpan.FIRST, foundHashSpan.getFirst());
            assertEquals("Should know ending hash span.", HashSpan.LAST, foundHashSpan.getLast());

            assertEquals("Should only have one target hash span.", 1, ffts1.getConfiguration().getTargetHashSpans().size());
            HashSpan foundTargetHashSpan = ffts1.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0];
            assertEquals("Should know starting target hash span.", HashSpan.FIRST, foundTargetHashSpan.getFirst());
            assertEquals("Should know ending target hash span.", HashSpan.LAST, foundTargetHashSpan.getLast());

            assertEquals("HashSpan and target HashSpan should be equal.", foundHashSpan, foundTargetHashSpan);

            assertEquals("Should only have one hash span.", 1, ffts2.getConfiguration().getHashSpans().size());
            foundHashSpan = ffts2.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0];
            assertEquals("Should know starting hash span.", HashSpan.FIRST, foundHashSpan.getFirst());
            assertEquals("Should know ending hash span.", HashSpan.LAST, foundHashSpan.getLast());

            assertEquals("Should only have one target hash span.", 1, ffts2.getConfiguration().getTargetHashSpans().size());
            foundTargetHashSpan = ffts2.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0];
            assertEquals("Should know starting hash span.", HashSpan.FIRST, foundTargetHashSpan.getFirst());
            assertEquals("Should know ending hash span.", HashSpan.LAST, foundTargetHashSpan.getLast());

            assertEquals("HashSpan and target HashSpan should be equal.", foundHashSpan, foundTargetHashSpan);

            // Make sure network status table is correct
            StatusTableRow row = NetworkUtil.getStatus().getRow(HOST1);
            assertEquals("Should only have one hash span.", 1, row.getHashSpans().size());
            assertEquals("Should only have one target hash span.", 1, row.getTargetHashSpans().size());

            row = NetworkUtil.getStatus().getRow(HOST2);
            assertEquals("Should only have one hash span.", 1, row.getHashSpans().size());
            assertEquals("Should only have one target hash span.", 1, row.getTargetHashSpans().size());

            // Here's the target hash span. We'll set it AFTER we set all the chunks
            byte[] middleHashBytes = new byte[BigHash.HASH_LENGTH];
            for (int i = 0; i < middleHashBytes.length; i++) {
                middleHashBytes[i] = 0;
            }
            BigHash middleHash = BigHash.createFromBytes(middleHashBytes);
            HashSpan targetHashSpan = new HashSpan(HashSpan.FIRST, middleHash);

            Set<BigHash> dataChunks = new HashSet();
            Set<BigHash> metaChunks = new HashSet();

            for (int i = 0; i < chunksToUse; i++) {
                byte[] dataChunk = DevUtil.createRandomDataChunkVariableSize();
                BigHash dataHash = new BigHash(dataChunk);

                while (dataChunks.contains(dataHash)) {
                    dataChunk = DevUtil.createRandomDataChunkVariableSize();
                    dataHash = new BigHash(dataChunk);
                }

                IOUtil.setData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHash, dataChunk);
                dataChunks.add(dataHash);

                byte[] metaChunk = DevUtil.createRandomBigMetaDataChunk();
                BigHash metaHash = DevUtil.getRandomBigHash();

                while (metaChunks.contains(metaHash)) {
                    metaChunk = DevUtil.createRandomBigMetaDataChunk();
                    metaHash = DevUtil.getRandomBigHash();
                }

                IOUtil.setMetaData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaHash, metaChunk);
                metaChunks.add(metaHash);
            }

            assertEquals("Should have correct number of hashes.", chunksToUse, dataChunks.size());
            assertEquals("Should have correct number of hashes.", chunksToUse, metaChunks.size());

            for (BigHash hash : dataChunks) {
                assertTrue("Should have.", IOUtil.hasData(ffts1, hash));
                assertFalse("Shouldn't have.", IOUtil.hasData(ffts2, hash));
            }

            for (BigHash hash : metaChunks) {
                assertTrue("Should have.", IOUtil.hasMetaData(ffts1, hash));
                assertFalse("Shouldn't have.", IOUtil.hasMetaData(ffts2, hash));
            }

            Configuration config1 = ffts1.getConfiguration();
            assertNotNull("Configuration better not be null.", config1);

            assertEquals("Should have one target hash span.", 1, config1.getTargetHashSpans().size());
            assertNotSame("Target hash span should be different than previous one.", targetHashSpan, config1.getTargetHashSpans().toArray(new HashSpan[0])[0]);

            config1.getTargetHashSpans().clear();
            config1.addTargetHashSpan(targetHashSpan);

            assertEquals("Should have one target hash span.", 1, config1.getTargetHashSpans().size());

            ffts1.setConfiguration(config1);

            // Busy wait up to a few minutes while hash span changes
            final long maxTimeToWait = 2 * 1000 * 60;
            final long start = System.currentTimeMillis();

            BUSY_WAIT:
            while (System.currentTimeMillis() - start <= maxTimeToWait) {
                config1 = ffts1.getConfiguration(false);

                Set<HashSpan> hashSpans = config1.getHashSpans();

                assertEquals("Better be only one hash span.", 1, hashSpans.size());

                foundHashSpan = hashSpans.toArray(new HashSpan[0])[0];
                if (foundHashSpan.getFirst().equals(targetHashSpan.getFirst()) && foundHashSpan.getLast().equals(targetHashSpan.getLast())) {
                    break BUSY_WAIT;
                }

                // Sleep a second
                Thread.sleep(1000);
            }

            int count = 0;
            for (BigHash hash : dataChunks) {
                count++;
                if (targetHashSpan.contains(hash)) {
                    // DBU
                    assertTrue("[ffts1, DBU] Should have data #" + count + ": " + hash, ffts1.getDataBlockUtil().hasData(hash));
                    assertFalse("[ffts2, DBU] Shouldn't have data #" + count + ": " + hash, ffts2.getDataBlockUtil().hasData(hash));

                    // IOUtil
                    assertTrue("[ffts1, IOUtil] Should have data #" + count + ": " + hash, IOUtil.hasData(ffts1, hash));
                    assertFalse("[ffts2, IOUtil] Shouldn't have data #" + count + ": " + hash, IOUtil.hasData(ffts2, hash));
                } else {
                    // DBU
                    assertFalse("[ffts1, DBU] Shouldn't have data #" + count + ": " + hash, ffts1.getDataBlockUtil().hasData(hash));
                    assertTrue("[ffts2, DBU] Should have data #" + count + ": " + hash, ffts2.getDataBlockUtil().hasData(hash));

                    // IOUtil
                    assertFalse("[ffts1, IOUtil] Shouldn't have data #" + count + ": " + hash, IOUtil.hasData(ffts1, hash));
                    assertTrue("[ffts2, IOUtil] Should have data #" + count + ": " + hash, IOUtil.hasData(ffts2, hash));
                }
            }

            count = 0;
            for (BigHash hash : metaChunks) {
                count++;
                if (targetHashSpan.contains(hash)) {
                    // DBU
                    assertTrue("[ffts1, DBU] Should have meta data #" + count + ": " + hash, ffts1.getDataBlockUtil().hasMetaData(hash));
                    assertFalse("[ffts2, DBU] Shouldn't have meta data #" + count + ": " + hash, ffts2.getDataBlockUtil().hasMetaData(hash));

                    // IOUtil
                    assertTrue("[ffts1, IOUtil] Should have meta data #" + count + ": " + hash, IOUtil.hasMetaData(ffts1, hash));
                    assertFalse("[ffts2, IOUtil] Shouldn't have meta data #" + count + ": " + hash, IOUtil.hasMetaData(ffts2, hash));
                } else {
                    // DBU
                    assertTrue("[ffts2, DBU] Should have meta data #" + count + ": " + hash, ffts2.getDataBlockUtil().hasMetaData(hash));
                    assertFalse("[ffts1, DBU] Shouldn't have meta data #" + count + ": " + hash, ffts1.getDataBlockUtil().hasMetaData(hash));

                    // IOUtil
                    assertTrue("[ffts2, IOUtil] Should have meta data #" + count + ": " + hash, IOUtil.hasMetaData(ffts2, hash));
                    assertFalse("[ffts1, IOUtil] Shouldn't have meta data #" + count + ": " + hash, IOUtil.hasMetaData(ffts1, hash));
                }
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testExpansionZeroInitialHashSpans() throws Exception {
        TestUtil.printTitle("TargetHashSpanThreadTest:testExpansionZeroInitialHashSpans()");
        Set<HashSpan> hashSpanSet = new HashSet(), targetHashSpanSet = new HashSet();
        targetHashSpanSet.add(HashSpan.FULL);
        testTargetHashSpanThread(hashSpanSet, targetHashSpanSet);
    }

    public void testExpansionSmallInitialHashSpan() throws Exception {
        TestUtil.printTitle("TargetHashSpanThreadTest:testExpansionSmallInitialHashSpan()");
        Set<HashSpan> hashSpanSet = new HashSet(), targetHashSpanSet = new HashSet();
        hashSpanSet.add(new HashSpan(HashSpan.FIRST, HashSpan.FIRST));
        targetHashSpanSet.add(HashSpan.FULL);
        testTargetHashSpanThread(hashSpanSet, targetHashSpanSet);
    }

    public void testExpansionLargeInitialHashSpan() throws Exception {
        TestUtil.printTitle("TargetHashSpanThreadTest:testExpansionSmallInitialHashSpan()");
        Set<HashSpan> hashSpanSet = new HashSet(), targetHashSpanSet = new HashSet();
        hashSpanSet.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST.getPrevious()));
        targetHashSpanSet.add(HashSpan.FULL);
        testTargetHashSpanThread(hashSpanSet, targetHashSpanSet);
    }

    public void testExpansionMultipleInitialHashSpans() throws Exception {
        TestUtil.printTitle("TargetHashSpanThreadTest:testExpansionMultipleInitialHashSpans()");
        Set<HashSpan> hashSpanSet = new HashSet(), targetHashSpanSet = new HashSet();
        hashSpanSet.add(new HashSpan(HashSpan.FIRST, HashSpan.FIRST));
        hashSpanSet.add(new HashSpan(HashSpan.LAST, HashSpan.LAST));
        targetHashSpanSet.add(HashSpan.FULL);
        testTargetHashSpanThread(hashSpanSet, targetHashSpanSet);
    }

    public void testTargetHashSpanThread(Set<HashSpan> hashSpanSet, Set<HashSpan> targetHashSpanSet) throws Exception {
        final String HOST1 = "aardvark.org";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();
            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull("Should have ffts.", ffts1);

            // Just set this server's target hash span thread to recheck more frequently since going to test
            ffts1.getTargetHashSpanThread().setTimeToSleep(100);

            Configuration config1 = ffts1.getConfiguration(false);
            assertNotNull("Configuration better not be null.", config1);

            config1.setHashSpans(hashSpanSet);
            config1.setTargetHashSpans(targetHashSpanSet);

            assertEquals(targetHashSpanSet.size(), config1.getTargetHashSpans().size());
            assertEquals(hashSpanSet.size(), config1.getHashSpans().size());

            ffts1.setConfiguration(config1);

            // Busy wait up to a few minutes while hash span changes
            final long maxTimeToWait = 2 * 1000 * 60;
            final long start = System.currentTimeMillis();

            BUSY_WAIT:
            while (System.currentTimeMillis() - start <= maxTimeToWait) {
                config1 = ffts1.getConfiguration(false);

                Set<HashSpan> hashSpans = config1.getHashSpans();
                if (hashSpans.size() > 0) {
                    break BUSY_WAIT;
                }

                // Sleep a second
                Thread.sleep(1000);
            }

            config1 = ffts1.getConfiguration(false);
            assertEquals(targetHashSpanSet.size(), config1.getHashSpans().size());
            HashSpanCollection.areEqual(config1.getHashSpans(), config1.getTargetHashSpans());
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Shows that if target hash span is removed, should reduce to nothing.</p>
     * @throws java.lang.Exception
     */
    public void testReducesToNothing() throws Exception {
        TestUtil.printTitle("TargetHashSpanThreadTest:testReducesToNothing()");

        final int chunksToUse = 15;

        final String HOST1 = "aardvark.org",  HOST2 = "batman.com";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull("Should have ffts.", ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull("Should have ffts.", ffts2);

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST1, ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("ConnectionUtil should have connetion for host: " + HOST2, ts2);

            assertFalse("Servers should have different host names.", ffts1.getHost().equals(ffts2.getHost()));

            ffts1.getConfiguration().setValue(ConfigKeys.HASHSPANFIX_NUMBER_REPS_IN_HASH_SPAN_REQUIRED_DELETE, String.valueOf(1));
            ffts1.getConfiguration().setValue(ConfigKeys.HASHSPANFIX_NUMBER_REPS_REQUIRED_DELETE, String.valueOf(1));

            // Just set this server's target hash span thread to recheck more frequently since going to test
            ffts1.getTargetHashSpanThread().setTimeToSleep(100);

            // Give each full hash span
            final Set<HashSpan> normalHashSpanSet = new HashSet();
            normalHashSpanSet.add(HashSpan.FULL);

            // Don't use normalHashSpanSet for target, or have bug: will TargetHashSpanThread and this test will
            // be modifying both normal and target hash spans whenever changes either (since set shared reference)
            //
            // Also, need separate collection for each server. Don't want to modify both servers just because change
            // one.
            final Set<HashSpan> targetHashSpanSet1 = new HashSet();
            final Set<HashSpan> targetHashSpanSet2 = new HashSet();
            targetHashSpanSet1.add(HashSpan.FULL);
            targetHashSpanSet2.add(HashSpan.FULL);

            ffts1.getConfiguration().setHashSpans(normalHashSpanSet);
            ffts1.getConfiguration().setTargetHashSpans(targetHashSpanSet1);
            ffts1.setConfiguration(ffts1.getConfiguration());

            ffts2.getConfiguration().setHashSpans(normalHashSpanSet);
            ffts2.getConfiguration().setTargetHashSpans(targetHashSpanSet2);
            ffts2.setConfiguration(ffts2.getConfiguration());

            testNetwork.updateNetwork();

            // Make sure configuration is correct
            assertEquals("Should only have one hash span.", 1, ffts1.getConfiguration().getHashSpans().size());
            HashSpan foundHashSpan = ffts1.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0];
            assertEquals("Should know starting hash span.", HashSpan.FIRST, foundHashSpan.getFirst());
            assertEquals("Should know ending hash span.", HashSpan.LAST, foundHashSpan.getLast());

            assertEquals("Should only have one target hash span.", 1, ffts1.getConfiguration().getTargetHashSpans().size());
            HashSpan foundTargetHashSpan = ffts1.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0];
            assertEquals("Should know starting target hash span.", HashSpan.FIRST, foundTargetHashSpan.getFirst());
            assertEquals("Should know ending target hash span.", HashSpan.LAST, foundTargetHashSpan.getLast());

            assertEquals("HashSpan and target HashSpan should be equal.", foundHashSpan, foundTargetHashSpan);

            assertEquals("Should only have one hash span.", 1, ffts2.getConfiguration().getHashSpans().size());
            foundHashSpan = ffts2.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0];
            assertEquals("Should know starting hash span.", HashSpan.FIRST, foundHashSpan.getFirst());
            assertEquals("Should know ending hash span.", HashSpan.LAST, foundHashSpan.getLast());

            assertEquals("Should only have one target hash span.", 1, ffts2.getConfiguration().getTargetHashSpans().size());
            foundTargetHashSpan = ffts2.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0];
            assertEquals("Should know starting hash span.", HashSpan.FIRST, foundTargetHashSpan.getFirst());
            assertEquals("Should know ending hash span.", HashSpan.LAST, foundTargetHashSpan.getLast());

            assertEquals("HashSpan and target HashSpan should be equal.", foundHashSpan, foundTargetHashSpan);

            // Make sure network status table is correct
            StatusTableRow row = NetworkUtil.getStatus().getRow(HOST1);
            assertEquals("Should only have one hash span.", 1, row.getHashSpans().size());
            assertEquals("Should only have one target hash span.", 1, row.getTargetHashSpans().size());

            row = NetworkUtil.getStatus().getRow(HOST2);
            assertEquals("Should only have one hash span.", 1, row.getHashSpans().size());
            assertEquals("Should only have one target hash span.", 1, row.getTargetHashSpans().size());

            Set<BigHash> dataChunks = new HashSet();
            Set<BigHash> metaChunks = new HashSet();

            for (int i = 0; i < chunksToUse; i++) {
                byte[] dataChunk = DevUtil.createRandomDataChunkVariableSize();
                BigHash dataHash = new BigHash(dataChunk);

                while (dataChunks.contains(dataHash)) {
                    dataChunk = DevUtil.createRandomDataChunkVariableSize();
                    dataHash = new BigHash(dataChunk);
                }

                IOUtil.setData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHash, dataChunk);
                dataChunks.add(dataHash);

                byte[] metaChunk = DevUtil.createRandomBigMetaDataChunk();
                BigHash metaHash = DevUtil.getRandomBigHash();

                while (metaChunks.contains(metaHash)) {
                    metaChunk = DevUtil.createRandomBigMetaDataChunk();
                    metaHash = DevUtil.getRandomBigHash();
                }

                IOUtil.setMetaData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaHash, metaChunk);
                metaChunks.add(metaHash);
            }

            assertEquals("Should have correct number of hashes.", chunksToUse, dataChunks.size());
            assertEquals("Should have correct number of hashes.", chunksToUse, metaChunks.size());

            for (BigHash hash : dataChunks) {
                assertTrue("Should have.", IOUtil.hasData(ffts1, hash));
                assertFalse("Shouldn't have.", IOUtil.hasData(ffts2, hash));
            }

            for (BigHash hash : metaChunks) {
                assertTrue("Should have.", IOUtil.hasMetaData(ffts1, hash));
                assertFalse("Shouldn't have.", IOUtil.hasMetaData(ffts2, hash));
            }

            Configuration config1 = ffts1.getConfiguration();
            assertNotNull("Configuration better not be null.", config1);

            assertEquals("Should have one target hash span.", 1, config1.getTargetHashSpans().size());
            config1.getTargetHashSpans().clear();

            assertEquals("Should not have a target hash span.", 0, config1.getTargetHashSpans().size());

            ffts1.setConfiguration(config1);

            // Busy wait up to a few minutes while hash span changes
            final long maxTimeToWait = 2 * 1000 * 60;
            final long start = System.currentTimeMillis();

            BUSY_WAIT:
            while (System.currentTimeMillis() - start <= maxTimeToWait) {
                config1 = ffts1.getConfiguration(false);

                if (config1.getHashSpans().size() == 0) {
                    break BUSY_WAIT;
                }

                // Sleep a second
                Thread.sleep(1000);
            }

            config1 = ffts1.getConfiguration(false);
            assertEquals("Shouldn't have a hash span.", 0, config1.getHashSpans().size());

            // Show that other server has a copy of everything but this server has nothing
            int count = 0;
            for (BigHash hash : dataChunks) {
                count++;
                // DBU
                assertFalse("[ffts1, DBU] Shouldn't have data #" + count + ": " + hash, ffts1.getDataBlockUtil().hasData(hash));
                assertTrue("[ffts2, DBU] Should have data #" + count + ": " + hash, ffts2.getDataBlockUtil().hasData(hash));

                // IOUtil
                assertFalse("[ffts1, IOUtil] Shouldn't have data #" + count + ": " + hash, IOUtil.hasData(ffts1, hash));
                assertTrue("[ffts2, IOUtil] Should have data #" + count + ": " + hash, IOUtil.hasData(ffts2, hash));

            }

            count = 0;
            for (BigHash hash : metaChunks) {
                count++;
                // DBU
                assertTrue("[ffts2, DBU] Should have meta data #" + count + ": " + hash, ffts2.getDataBlockUtil().hasMetaData(hash));
                assertFalse("[ffts1, DBU] Shouldn't have meta data #" + count + ": " + hash, ffts1.getDataBlockUtil().hasMetaData(hash));

                // IOUtil
                assertTrue("[ffts2, IOUtil] Should have meta data #" + count + ": " + hash, IOUtil.hasMetaData(ffts2, hash));
                assertFalse("[ffts1, IOUtil] Shouldn't have meta data #" + count + ": " + hash, IOUtil.hasMetaData(ffts1, hash));

            }

        } finally {
            testNetwork.stop();
        }
    }
}
