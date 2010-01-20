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
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.flatfile.HashSpanFixingThread;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
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