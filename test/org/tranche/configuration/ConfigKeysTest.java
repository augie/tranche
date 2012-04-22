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
package org.tranche.configuration;

import java.io.File;
import org.tranche.add.AddFileTool;
import org.tranche.add.AddFileToolReport;
import org.tranche.add.CommandLineAddFileToolListener;
import org.tranche.commons.RandomUtil;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.flatfile.HashSpanFixingThread;
import org.tranche.get.CommandLineGetFileToolListener;
import org.tranche.get.GetFileTool;
import org.tranche.get.GetFileToolReport;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 * <p>Test that configuration attributes (keys) do what they are supposed to do.</p>
 * <p>This generally concerns server behavior. Though configuration attributes are not restricted to anything in particular (and clients use values, too), the result is to generally administrate a server with real-time policy changes.</p>
 * <p>Some ConfigKeys are tested elsewhere, including:</p>
 * <ul>
 *   <li>"corruptedDataBlock: AllowedToFixCorruptedDataBlock" (ConfigKeys.CORRUPTED_DB_ALLOWED_TO_FIX) in ReplaceCorruptedDataBlockThreadTest</li>
 *   <li>"hashSpanFix: AllowDelete" (ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_DELETE) IN HashSpanFixingThreadTest.testConfigurationAttributes</li>
 *   <li>"hashSpanFix: AllowRun" (ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN) IN HashSpanFixingThreadTest.testConfigurationAttributes</li>
 *   <li>"hashSpanFix: NumTotalRepsRequiredForDelete" (ConfigKeys.HASHSPANFIX_NUMBER_REPS_REQUIRED_DELETE) IN HashSpanFixingThreadTest.testConfigurationAttributes</li>
 * </ul>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ConfigKeysTest extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testBlockPropagationForDownloads() throws Exception {
        TestUtil.printTitle("ConfigKeysTest:testBlockPropagationForDownloads()");

        String HOST1 = "server1.com",
                HOST2 = "server2.com",
                HOST3 = "server3.com",
                HOST4 = "server4.com",
                HOST5 = "server5.com";

        final String[] hosts = {
            HOST1, HOST2, HOST3, HOST4, HOST5
        };

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, hosts[0], 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, hosts[1], 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, hosts[2], 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, hosts[3], 1503, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, hosts[4], 1504, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));

        //create temp file
        File project = null, downloadDir1 = null, downloadDir2 = null;

        try {
            testNetwork.start();


            assertEquals(hosts.length, testNetwork.getServerConfigs().size());

            // No healing, no startup, and definitely no propagation

            for (String host : hosts) {
                FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(host);
                assertNotNull(ffts);
                Configuration c = ffts.getConfiguration();
                c.setValue(ConfigKeys.PROPAGATE_ALLOW_GET_DATA, String.valueOf(false));
                c.setValue(ConfigKeys.PROPAGATE_ALLOW_GET_META_DATA, String.valueOf(false));
                c.setValue(ConfigKeys.SERVER_STARTUP_THREAD_ALLOW_RUN, String.valueOf(false));
                c.setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, String.valueOf(false));
                ffts.setConfiguration(c);
            }

            // Prove that changes took place
            for (String host : hosts) {
                FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(host);
                assertNotNull(ffts);
                Configuration c = ffts.getConfiguration();
                assertEquals(String.valueOf(false), c.getValue(ConfigKeys.PROPAGATE_ALLOW_GET_DATA));
                assertEquals(String.valueOf(false), c.getValue(ConfigKeys.PROPAGATE_ALLOW_GET_META_DATA));
                assertEquals(String.valueOf(false), c.getValue(ConfigKeys.SERVER_STARTUP_THREAD_ALLOW_RUN));
                assertEquals(String.valueOf(false), c.getValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN));
            }

            project = DevUtil.createTestProject(RandomUtil.getInt(10) + 2, 1, DataBlockUtil.getMaxChunkSize() * 2);

            //Upload test file
            AddFileTool aft = new AddFileTool();
            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.addServerToUse(HOST3); // Pick middle server
            aft.setUseUnspecifiedServers(false);
            aft.setTitle(RandomUtil.getString(20));
            aft.setDescription(RandomUtil.getString(20));
            aft.setFile(project);

            final AddFileToolReport aftReport = aft.execute();
            assertNotNull(aftReport.getHash());
            assertFalse(aftReport.isFailed());
            assertTrue(aftReport.isFinished());

            final BigHash _hash = aftReport.getHash();

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
            // TEST #1: Make sure GFT still works with propagation off
            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
            downloadDir1 = TempFileUtil.createTemporaryDirectory();

            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.addServerToUse(HOST2);
            gft.addServerToUse(HOST3);
            gft.addServerToUse(HOST4);
            gft.addServerToUse(HOST5);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(_hash);
            gft.setSaveFile(downloadDir1);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            GetFileToolReport gftReport = gft.getDirectory();

            assertNotNull(gftReport);
            assertFalse(gftReport.isFailed());
            
            final File downloadedProject1 = new File(downloadDir1, project.getName());

            TestUtil.assertDirectoriesEquivalent(project, downloadedProject1);
            
            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
            // TEST #2: Should fail if the server with data omitted
            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
            downloadDir2 = TempFileUtil.createTemporaryDirectory();

            gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.addServerToUse(HOST2);
            //gft.addServerToUse(HOST3); << SERVER WITH DATA
            gft.addServerToUse(HOST4);
            gft.addServerToUse(HOST5);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(_hash);
            gft.setSaveFile(downloadDir2);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            gftReport = gft.getDirectory();

            assertNotNull(gftReport);
            assertTrue(gftReport.isFailed());

        } finally {
            testNetwork.stop();
            if (project != null) {
                IOUtil.recursiveDeleteWithWarning(project);
            }
            if (downloadDir1 != null) {
                IOUtil.recursiveDeleteWithWarning(downloadDir1);
            }
            if (downloadDir2 != null) {
                IOUtil.recursiveDeleteWithWarning(downloadDir1);
            }
        }
    }

    /**
     * <p>Tests the following using simple setter/getter methods:</p>
     * <ul>
     *   <li>hashSpanFix: BatchSizeForDeleteChunks (ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_DELETE_CHUNKS)</li>
     *   <li>hashSpanFix: BatchSizeForDownloadChunks (ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_DOWNLOAD_CHUNKS)</li>
     *   <li>hashSpanFix: BatchSizeForReplicateChunks (ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_HEAL_CHUNKS)</li>
     *   <li>hashSpanFix: PauseInMillisAfterEachOperation (ConfigKeys.HASHSPANFIX_PAUSE_MILLIS)</li>
     * </ul>
     * @throws java.lang.Exception
     */
    public void testHashSpanFixingThreadBatchSizesAndPause() throws Exception {
        TestUtil.printTitle("ConfigKeysTest:testHashSpanFixingThreadBatchSizesAndPause()");
        FlatFileTrancheServer ffts = null;
        try {
            TestUtil.setTestingHashSpanFixingThread(true);

            File data = TempFileUtil.createTemporaryDirectory();
            ffts = new FlatFileTrancheServer(data);

            // Set ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_DELETE_CHUNKS
            ffts.getConfiguration().setValue(ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_DELETE_CHUNKS, String.valueOf(42));

            // Set ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_DOWNLOAD_CHUNKS
            ffts.getConfiguration().setValue(ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_DOWNLOAD_CHUNKS, String.valueOf(7));

            // Set ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_HEAL_CHUNKS
            ffts.getConfiguration().setValue(ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_HEAL_CHUNKS, String.valueOf(8));

            // Set ConfigKeys.HASHSPANFIX_PAUSE_MILLIS
            ffts.getConfiguration().setValue(ConfigKeys.HASHSPANFIX_PAUSE_MILLIS, String.valueOf(1024));

            // Verify above
            HashSpanFixingThread h = ffts.getHashSpanFixingThread();

            assertEquals("Expecting size for delete batch", 42, h.getBatchSizeForChunksToDelete(ffts.getConfiguration()));
            assertEquals("Expecting size for download batch", 7, h.getBatchSizeForChunksToDownload(ffts.getConfiguration()));
            assertEquals("Expecting size for healing batch", 8, h.getBatchSizeForChunksToHeal(ffts.getConfiguration()));
            assertEquals("Expecting size for pause between operations in healing thread", 1024, h.getPauseBetweenOperations(ffts.getConfiguration()));
        } finally {
            TestUtil.setTestingHashSpanFixingThread(false);
            IOUtil.safeClose(ffts);
        }
    }
}