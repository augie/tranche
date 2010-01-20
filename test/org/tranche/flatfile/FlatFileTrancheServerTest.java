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
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.tranche.TrancheServer;
import org.tranche.TrancheServerTest;
import org.tranche.add.AddFileTool;
import org.tranche.add.CommandLineAddFileToolListener;
import org.tranche.configuration.Configuration;
import org.tranche.exceptions.NoHostProvidedException;
import org.tranche.exceptions.TodoException;
import org.tranche.hash.BigHash;
import org.tranche.security.SecurityUtil;
import org.tranche.security.Signature;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.users.User;
import org.tranche.users.UserZipFile;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.Utils;

/**
 *<p>A suite of tests that ensure the FlatFileDistributedFileSystem class is working as expected.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class FlatFileTrancheServerTest extends TrancheServerTest {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        FlatFileTrancheServer.setDebug(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        FlatFileTrancheServer.setDebug(false);
    }

    public void testGetActivityLogEntries() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testGetActivityLogEntries()");
        throw new TodoException();
    }

    public void testGetActivityLogEntriesCount() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testGetActivityLogEntriesCount()");
        throw new TodoException();
    }

    public void testGetConfiguration() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testGetConfiguration()");
        throw new TodoException();
    }

    public void testSetData() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testSetData()");

        FlatFileTrancheServer ffdfs = null;
        try {
            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            ffdfs.getConfiguration().addUser(DevUtil.getDevUser());
            ffdfs.saveConfiguration();
            TrancheServerTest.testSetData(ffdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
        } finally {
            IOUtil.safeClose(ffdfs);
        }
    }

    public void testSetMetaData() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testSetMetaData()");

        FlatFileTrancheServer ffdfs = null;
        try {
            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            ffdfs.getConfiguration().addUser(DevUtil.getDevUser());
            ffdfs.saveConfiguration();
            TrancheServerTest.testSetMetaData(ffdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
        } finally {
            IOUtil.safeClose(ffdfs);
        }
    }

    public void testAddBigMetaData() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testAddBigMetaData()");

        FlatFileTrancheServer ffdfs = null;
        try {
            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            ffdfs.getConfiguration().addUser(DevUtil.getDevUser());
            ffdfs.saveConfiguration();
            TrancheServerTest.testSetBigMetaData(ffdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
        } finally {
            IOUtil.safeClose(ffdfs);
        }
    }

    public void testSetConfiguration() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testSetConfiguration()");

        FlatFileTrancheServer ffdfs = null;
        try {
            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            ffdfs.getConfiguration().addUser(DevUtil.getDevUser());
            ffdfs.saveConfiguration();
            TrancheServerTest.testSetConfiguration(ffdfs, DevUtil.getDevUser(), DevUtil.getDevPrivateKey());
        } finally {
            IOUtil.safeClose(ffdfs);
        }
    }

    public void testDeleteData() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testDeleteData()");

        FlatFileTrancheServer ffdfs = null;
        try {
            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            ffdfs.getConfiguration().addUser(DevUtil.getDevUser());
            ffdfs.saveConfiguration();
            TrancheServerTest.testDeleteData(ffdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
        } finally {
            IOUtil.safeClose(ffdfs);
        }
    }

    public void testDeleteMetaData() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testDeleteMetaData()");

        FlatFileTrancheServer ffdfs = null;
        try {
            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            ffdfs.getConfiguration().addUser(DevUtil.getDevUser());
            ffdfs.saveConfiguration();
            TrancheServerTest.testDeleteMetaData(ffdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
        } finally {
            IOUtil.safeClose(ffdfs);
        }
    }

    public void testDeleteMetaDataUploader() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testDeleteMetaDataUploader()");

        FlatFileTrancheServer ffdfs = null;
        try {
            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            ffdfs.getConfiguration().addUser(DevUtil.getDevUser());
            ffdfs.saveConfiguration();
            TrancheServerTest.testDeleteMetaDataUploader(ffdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
        } finally {
            IOUtil.safeClose(ffdfs);
        }
    }

    public void testDefaultHashSpans() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testDefaultHashSpans()");

        FlatFileTrancheServer ffts = null;
        try {
            ffts = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            assertEquals("Not expecting any hash spans by default", 0, ffts.getConfiguration().getHashSpans().size());
        } finally {
            IOUtil.safeClose(ffts);
        }
    }

    public void testExpiredUser() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testExpiredUser()");

        FlatFileTrancheServer ffts = null;
        try {
            ffts = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());

            File adminUserFile = TempFileUtil.createTemporaryFile();
            UserZipFile admin = DevUtil.createUser("admin", "admin", adminUserFile.getPath(), true, false);

            // Create a signed user, expired
            File signedUserFile = TempFileUtil.createTemporaryFile();
            UserZipFile user = DevUtil.createSignedUser("test", "test", signedUserFile.getPath(), admin.getCertificate(), admin.getPrivateKey(), true, true);

            ffts.getConfiguration().addUser(user);

            // Create and try to set random data
            byte[] data = Utils.makeRandomData(512);

            PropagationReturnWrapper wrapper = IOUtil.setData(ffts, user.getCertificate(), user.getPrivateKey(), BigHash.createFromBytes(data), data);
            assertTrue(wrapper.isAnyErrors());
            assertEquals(1, wrapper.getErrors().size());
            if (!(wrapper.getErrors().iterator().next().exception instanceof GeneralSecurityException)) {
                fail("Expected a GeneralSecurityException");
            }

            assertFalse("Server shouldn't have data set by user with expired certificate.", IOUtil.hasData(ffts, BigHash.createFromBytes(data)));
        } finally {
            IOUtil.safeClose(ffts);
        }
    }

    public void testBadChunksEmpty1MBSmallBatch() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBadChunksEmpty1MBSmallBatch()");
        testFFTSHandlesBadChunks(10, 10, true, true, false);
    }

    public void testBadChunksRandom1MBSmallBatch() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBadChunksRandom1MBSmallBatch()");
        testFFTSHandlesBadChunks(10, 10, true, false, false);
    }

    public void testBadChunksEmptySmallBatch() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBadChunksEmptySmallBatch()");
        testFFTSHandlesBadChunks(10, 10, false, true, false);
    }

    public void testBadChunksEmpty1MBSmallBatchWithFalseSizes() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBadChunksEmpty1MBSmallBatchWithFalseSizes()");
        testFFTSHandlesBadChunks(10, 10, true, true, true);
    }

    public void testBadChunksRandom1MBSmallBatchWithFalseSizes() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBadChunksRandom1MBSmallBatchWithFalseSizes()");
        testFFTSHandlesBadChunks(10, 10, true, false, true);
    }

    public void testBadChunksEmptySmallBatchWithFalseSizes() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBadChunksEmptySmallBatchWithFalseSizes()");
        testFFTSHandlesBadChunks(10, 10, false, true, true);
    }

    public void testBadChunksRandomSmallBatchWithFalseSizes() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBadChunksRandomSmallBatchWithFalseSizes()");
        testFFTSHandlesBadChunks(10, 10, false, false, true);
    }

    private void testFFTSHandlesBadChunks(int numDataChunks, int numMetaChunks, boolean useExactly1MBChunks, boolean isUseEmptyBytes, boolean isTestFalseSizes) throws Exception {
        FlatFileTrancheServer ffts = null;
        try {
            File fftsDir = TempFileUtil.createTemporaryDirectory();
            ffts = new FlatFileTrancheServer(fftsDir);
            ffts.getConfiguration().addUser(DevUtil.getDevUser());
            ffts.saveConfiguration();

            List<BigHash> dataChunks = new ArrayList<BigHash>();
            for (int i = 0; i < numDataChunks; i++) {
                byte[] goodChunk = null, badChunk = null;
                if (useExactly1MBChunks) {
                    goodChunk = new byte[1024 * 1024];
                    badChunk = new byte[1024 * 1024];
                } else {
                    int size = RandomUtil.getInt(1024 * 1024 - 1) + 1;
                    assertTrue("Size should be greater than zero.", size > 0);
                    assertTrue("Size should be 1MB or less.", size <= 1024 * 1024);
                    goodChunk = new byte[size];
                    badChunk = new byte[size];
                }
                RandomUtil.getBytes(goodChunk);
                BigHash hash = new BigHash(goodChunk);

                // If already contains, skip
                if (dataChunks.contains(hash)) {
                    i--;
                    continue;
                }

                /**
                 * If testing bad size, make bad chunk different size than good chunk
                 */
                if (isTestFalseSizes) {
                    int size = goodChunk.length;
                    int attempts = 0;
                    while (size > 1024 * 1024 || size == goodChunk.length) {
                        // Roll die to add or subtract
                        if (RandomUtil.getBoolean()) {
                            size = Math.abs(goodChunk.length - RandomUtil.getInt(goodChunk.length));
                        } else {
                            size = goodChunk.length + RandomUtil.getInt(1024 * 1024 - goodChunk.length + 1);
                        }

                        attempts++;

                        // Don't allow to go crazy
                        if (attempts > 1000000) {
                            fail("Couldn't change size. Why!?!");
                        }
                    }
                    badChunk = new byte[size];
                }

                // What to do with bad chunk?
                if (!isUseEmptyBytes) {
                    RandomUtil.getBytes(badChunk);
                }

                // Assert bytes are different only if same size. Otherwise, guarenteed
                // to be different.
                if (badChunk.length == goodChunk.length) {
                    boolean difference = false;
                    for (int j = 0; j < badChunk.length; j++) {
                        if (badChunk[j] != goodChunk[j]) {
                            difference = true;
                            break;
                        }
                    }
                    assertTrue("Better be a difference!", difference);
                }

                dataChunks.add(hash);

                // Flip a coin. If true, use good. Else use bad.
                if (RandomUtil.getBoolean()) {
                    PropagationReturnWrapper wrapper = IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, goodChunk);
                    assertFalse(wrapper.isAnyErrors());
                    assertTrue(IOUtil.hasData(ffts, hash));
                } else {
                    PropagationReturnWrapper wrapper = IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, badChunk);
                    assertTrue(wrapper.isAnyErrors());
                    assertFalse(IOUtil.hasData(ffts, hash));
                }
            }

            List<BigHash> metaChunks = new ArrayList<BigHash>();
            for (int i = 0; i < numMetaChunks; i++) {
                byte[] goodChunk = null, badChunk = null;
                // Create the good chunk
                goodChunk = DevUtil.createRandomMetaDataChunk();
                if (useExactly1MBChunks) {
                    badChunk = new byte[1024 * 1024];
                } else {
                    badChunk = new byte[goodChunk.length];
                }

                // The hash is arbitrary for any meta data on real network.
                // Just go ahead and use the bytes above.
                BigHash hash = new BigHash(goodChunk);

                // If already contains, skip
                if (metaChunks.contains(hash)) {
                    i--;
                    continue;
                }

                /**
                 * If testing bad size, make bad chunk different size than good chunk
                 */
                if (isTestFalseSizes) {
                    int size = goodChunk.length;
                    int attempts = 0;
                    while (size > 1024 * 1024 || size == goodChunk.length) {
                        // Roll die to add or subtract
                        if (RandomUtil.getBoolean()) {
                            size = Math.abs(goodChunk.length - RandomUtil.getInt(goodChunk.length));
                        } else {
                            size = goodChunk.length + RandomUtil.getInt(1024 * 1024 - goodChunk.length + 1);
                        }

                        attempts++;

                        // Don't allow to go crazy
                        if (attempts > 1000000) {
                            fail("Couldn't change size. Why!?!");
                        }
                    }
                    badChunk = new byte[size];
                }

                // What to do with bad chunk?
                if (!isUseEmptyBytes) {
                    RandomUtil.getBytes(badChunk);
                }

                // Assert bytes are different only if same size. Otherwise, guaranteed to be different.
                if (badChunk.length == goodChunk.length) {
                    boolean difference = false;
                    for (int j = 0; j < badChunk.length; j++) {
                        if (badChunk[j] != goodChunk[j]) {
                            difference = true;
                            break;
                        }
                    }
                    assertTrue("Better be a difference!", difference);
                }

                metaChunks.add(hash);

                // Flip a coin
                if (RandomUtil.getBoolean()) {
                    PropagationReturnWrapper wrapper = IOUtil.setMetaData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, goodChunk);
                    assertFalse(wrapper.isAnyErrors());
                    assertTrue(IOUtil.hasMetaData(ffts, hash));
                } else {
                    PropagationReturnWrapper wrapper = IOUtil.setMetaData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, badChunk);
                    assertTrue(wrapper.isAnyErrors());
                    assertFalse(IOUtil.hasMetaData(ffts, hash));
                }
            }
        } finally {
            IOUtil.safeClose(ffts);
        }
    }

    public void testConfigurationChangesAreImmediateSmall() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testConfigurationChangesAreImmediateSmall()");
        testConfigurationChangesAreImmediate(25);
    }

    public void testConfigurationChangesAreImmediateMedium() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testConfigurationChangesAreImmediateMedium()");
        testConfigurationChangesAreImmediate(50);
    }

    // Takes way too long...
//    /**
//     * Might take up to 30 min.
//     */
//    public void testConfigurationChangesAreImmediateLarge() throws Exception {
//        TestUtil.printTitle("FlatFileTrancheServerTest:testConfigurationChangesAreImmediateLarge()");
//        testConfigurationChangesAreImmediate(250);
//    }

    public void testConfigurationChangesAreImmediate(int chunks) throws Exception {
        FlatFileTrancheServer ffts = null;
        try {
            ffts = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            ffts.getConfiguration().addUser(DevUtil.getDevUser());
            // We never want to use the FFTS home directory for data
            ffts.getConfiguration().clearDataDirectories();
            ffts.saveConfiguration();

            TrancheServerTest.testConfigurationChangesAreImmediate(ffts, DevUtil.getDevUser(), chunks);
        } finally {
            IOUtil.safeClose(ffts);
        }
    }

    /**
     * <p>Tests that can use DDCs when others are full.</p>
     * <p>NOTE: The larger the test, more likely will fail until DataBlockUtil is fixed to intelligently swap DataDirectoryConfigurations. This is a known problem.</p>
     */
    public void testOtherDDCsWhenOneIsFullSmall() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testOtherDDCsWhenOneIsFullSmall()");
        testOtherDDCsWhenOneIsFull(3);
    }

    /**
     * <p>Tests that can use DDCs when others are full.</p>
     * <p>NOTE: The larger the test, more likely will fail until DataBlockUtil is fixed to intelligently swap DataDirectoryConfigurations. This is a known problem.</p>
     * @throws java.lang.Exception
     */
    public void testOtherDDCsWhenOneIsFullMedium() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testOtherDDCsWhenOneIsFullMedium()");
        testOtherDDCsWhenOneIsFull(25);
    }

    /**
     * <p>Need to make sure that a full DDC is not an issue if there are others.</p>
     */
    public void testOtherDDCsWhenOneIsFull(int numChunksPerDirectory) throws Exception {

        System.out.println("");
        System.out.println("");
        System.out.println("testOtherDDCsWhenOneIsFull(" + numChunksPerDirectory + ")");
        System.out.println("");
        System.out.println("");

        /**
         * Need to calculate close size so last chunk is out of space. This will be tricky under certain conditions.
         *
         * This worked for three chunks:
         * 1024 * 1024 * 3 + 256 * 1024
         */        // Step 1 of 3: Calculate overhead. Max of 256 data blocks (unless split)
        long numDataBlocks = numChunksPerDirectory;
        if (numDataBlocks > 256) {
            numDataBlocks = 256;
        }

        // Step 2 of 3: calculate the overhead
        long overhead = numDataBlocks * 86000;

        // Step 3 of 3: calculate the needed size
        final long requiredDDCSizes = 1024 * 1024 * numChunksPerDirectory + overhead;

        FlatFileTrancheServer ffts = null;
        File home = null;

        File ddcDir1 = null, ddcDir2 = null, ddcDir3 = null;
        DataDirectoryConfiguration ddc1 = null, ddc2 = null, ddc3 = null;

        List<BigHash> hashes = new ArrayList<BigHash>();

        try {
            home = TempFileUtil.createTemporaryDirectory();
            ffts = new FlatFileTrancheServer(home);

            // Set up user and remove the original data directory.
            // Going to set the configuration so changes take place
            Configuration c = ffts.getConfiguration();
            c.addUser(DevUtil.getDevUser());
            c.clearDataDirectories();

            ddcDir1 = TempFileUtil.createTemporaryDirectory();
            ddcDir2 = TempFileUtil.createTemporaryDirectory();
            ddcDir3 = TempFileUtil.createTemporaryDirectory();

            assertFalse("Each directory must be distinct.", ddcDir1.equals(ddcDir2));
            assertFalse("Each directory must be distinct.", ddcDir2.equals(ddcDir3));
            assertFalse("Each directory must be distinct.", ddcDir3.equals(ddcDir1));

            ddc1 = new DataDirectoryConfiguration(ddcDir1.getAbsolutePath(), requiredDDCSizes);
            ddc2 = new DataDirectoryConfiguration(ddcDir2.getAbsolutePath(), requiredDDCSizes);
            ddc3 = new DataDirectoryConfiguration(ddcDir3.getAbsolutePath(), requiredDDCSizes);

            /**
             * Add the first DDC. Fill it up, assert it is full.
             */
            System.out.println("");
            System.out.println("================================================");
            System.out.println("=== STEP 1: FILL UP FIRST DDC");
            System.out.println("================================================");
            System.out.println("");
            c.addDataDirectory(ddc1);
            assertEquals("Expecting one DDC.", 1, c.getDataDirectories().size());

            IOUtil.setConfiguration(ffts, c, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            // Should have no trouble adding three, but the fourth better throw an exception
            while (hashes.size() != numChunksPerDirectory) {
                byte[] data = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(data);

                // Unlikely, but should check
                if (hashes.contains(hash)) {
                    continue;
                }

                try {
                    IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, data);
                    hashes.add(hash);
                } catch (Exception ex) {
                    System.err.println("Exception on iteration: " + hashes.size());
                    throw ex;
                }
            }

            // Just checking FFTS's configuration for DDCs
            c = ffts.getConfiguration();
            for (DataDirectoryConfiguration ddc : c.getDataDirectories()) {
                System.out.println("-> DDC: " + ddc.getDirectory() + " (" + ddc.getActualSize() + " of " + ddc.getSizeLimit() + " bytes used)");
            }

            {
                byte[] data = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(data);
                PropagationReturnWrapper wrapper = IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, data);
                assertTrue(wrapper.isAnyErrors());
            }

            System.out.println("");
            System.out.println("================================================");
            System.out.println("=== STEP 2: FILL UP SECOND DDC");
            System.out.println("================================================");
            System.out.println("");

            /**
             * Add the second DDC, keep all existing DDCs. Fill it up, assert it is full.
             */
            assertEquals("Should be one DDC.", 1, c.getDataDirectories().size());
            c.addDataDirectory(ddc2);
            assertEquals("Expecting first two DDCs.", 2, c.getDataDirectories().size());

            IOUtil.setConfiguration(ffts, c, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            Thread.yield();
            ffts.waitToLoadExistingDataBlocks();

            // Should have no trouble adding three, but the fourth better throw an exception
            while (hashes.size() != 2 * numChunksPerDirectory) {
                byte[] data = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(data);

                // Unlikely, but should check
                if (hashes.contains(hash)) {
                    continue;
                }

                IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, data);
                hashes.add(hash);
            }

            // Just checking FFTS's configuration for DDCs
            c = ffts.getConfiguration();
            for (DataDirectoryConfiguration ddc : c.getDataDirectories()) {
                System.out.println("-> DDC: " + ddc.getDirectory() + " (" + ddc.getActualSize() + " of " + ddc.getSizeLimit() + " bytes used)");
            }

            {
                byte[] data = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(data);

                PropagationReturnWrapper wrapper = IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, data);
                assertTrue(wrapper.isAnyErrors());
            }

            /**
             * Add the last DDC, keep all existing DDCs. Fill it up, assert it is full.
             */
            System.out.println("");
            System.out.println("================================================");
            System.out.println("=== STEP 3: FILL UP THIRD (AND FINAL) DDC");
            System.out.println("================================================");
            System.out.println("");
            c.addDataDirectory(ddc3);
            assertEquals("Expecting all DDCs.", 3, c.getDataDirectories().size());

            IOUtil.setConfiguration(ffts, c, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            Thread.yield();
            ffts.waitToLoadExistingDataBlocks();

            // Should have no trouble adding three, but the fourth better throw an exception
            while (hashes.size() != 3 * numChunksPerDirectory) {
                byte[] data = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(data);

                // Unlikely, but should check
                if (hashes.contains(hash)) {
                    continue;
                }

                IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, data);
                hashes.add(hash);
            }

            // Just checking FFTS's configuration for DDCs
            c = ffts.getConfiguration();
            for (DataDirectoryConfiguration ddc : c.getDataDirectories()) {
                System.out.println("-> DDC: " + ddc.getDirectory() + " (" + ddc.getActualSize() + " of " + ddc.getSizeLimit() + " bytes used)");
            }

            {
                byte[] data = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(data);

                PropagationReturnWrapper wrapper = IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, data);
                assertTrue(wrapper.isAnyErrors());
            }

            /**
             * Assert has all the data
             */
            for (BigHash h : hashes) {
                assertTrue("Expect has all the data", IOUtil.hasData(ffts, h));
            }
            assertEquals("Expecting an exact number of data chunks.", hashes.size(), ffts.getDataBlockUtil().dataHashes.size());

        } finally {
            IOUtil.safeClose(ffts);
        }
    }

    public void testUserCannotOverwriteMetaData() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testUserCannotOverwriteMetaData()");
        FlatFileTrancheServer ffts = null;
        try {
            ffts = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            UserZipFile user = DevUtil.makeNewUser();
            user.setFlags(User.CAN_GET_CONFIGURATION | User.CAN_SET_CONFIGURATION | User.CAN_SET_DATA | User.CAN_SET_META_DATA);
            ffts.getConfiguration().addUser(user);
            ffts.saveConfiguration();

            TrancheServerTest.testUserCannotOverwriteMetaData(ffts, user);
        } finally {
            IOUtil.safeClose(ffts);
        }
    }

    /**
     * Tests the functionality of batch has data and meta data functions
     */
    public void testHasDataAndMeta() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testHasDataAndMeta()");

        FlatFileTrancheServer ffserver = null;
        try {
            ffserver = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            ffserver.getConfiguration().addUser(DevUtil.getDevUser());
            ffserver.saveConfiguration();
            TrancheServerTest.testHasDataAndMeta(ffserver, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
        } finally {
            IOUtil.safeClose(ffserver);
        }
    }

    public void testBatchOfNegative1Nonces() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBatchOfNegative1Nonces()");
        testBatchNonces(-1);
    }

    public void testBatchOf0Nonces() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBatchOf0Nonces()");
        testBatchNonces(0);
    }

    public void testBatchOf1Nonces() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBatchOf1Nonces()");
        testBatchNonces(1);
    }

    public void testBatchOf5Nonces() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBatchOf5Nonces()");
        testBatchNonces(5);
    }

    public void testBatchOf100Nonces() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBatchOf100Nonces()");
        testBatchNonces(100);
    }

    public void testBatchNonces(int count) throws Exception {
        FlatFileTrancheServer ffserver = null;
        try {
            ffserver = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            TrancheServerTest.testGetNonces(ffserver, count);
        } finally {
            IOUtil.safeClose(ffserver);
        }
    }

    public void testBatchOfSize1GetData() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBatchOfSize1GetData()");
        testBatchGetData(1);
    }

    public void testBatchOfSize5GetData() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBatchOfSize5GetData()");
        testBatchGetData(5);
    }

    public void testBatchOfSize25GetData() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBatchOfSize25GetData()");
        testBatchGetData(25);
    }

    private void testBatchGetData(int count) throws Exception {
        FlatFileTrancheServer ffdfs = null;
        try {
            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            ffdfs.getConfiguration().getUsers().add(DevUtil.getDevUser());
            ffdfs.saveConfiguration();
            TrancheServerTest.testGetChunks(ffdfs, DevUtil.getDevUser(), count, false);
        } finally {
            IOUtil.safeClose(ffdfs);
        }
    }

    public void testBatchOfSize1GetMetaData() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBatchOfSize1GetMetaData()");
        testBatchGetMetaData(1);
    }

    public void testBatchOfSize5GetMetaData() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBatchOfSize5GetMetaData()");
        testBatchGetMetaData(5);
    }

    public void testBatchOfSize25GetMetaData() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testBatchOfSize25GetMetaData()");
        testBatchGetMetaData(25);
    }

    public void testBatchGetMetaData(int count) throws Exception {
        FlatFileTrancheServer ffdfs = null;
        try {
            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            ffdfs.getConfiguration().getUsers().add(DevUtil.getDevUser());
            ffdfs.saveConfiguration();
            TrancheServerTest.testGetChunks(ffdfs, DevUtil.getDevUser(), count, true);
        } finally {
            IOUtil.safeClose(ffdfs);
        }
    }

    public void testRequestShutdown() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testRequestShutdown()");
        FlatFileTrancheServer ffts = null;
        try {
            ffts = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());

            // Create a non-admin user
            File userFile = TempFileUtil.createTemporaryFile(".zip.encrypted");
            final UserZipFile uzf = DevUtil.createUser("bryan", "smith", userFile.getAbsolutePath(), false, false);

            ffts.getConfiguration().getUsers().add(DevUtil.getDevUser());
            ffts.getConfiguration().getUsers().add(uzf);

            // Get a nonce and sign it for admin
            byte[] nonce = IOUtil.getNonce(ffts);
            String algorithm = SecurityUtil.getSignatureAlgorithm(DevUtil.getDevPrivateKey());
            byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(nonce), DevUtil.getDevPrivateKey(), algorithm);

            // make a proper signature object
            Signature signature = new Signature(sig, algorithm, DevUtil.getDevAuthority());

            // shut down
            ffts.requestShutdown(signature, nonce);

            // Get a nonce and sign it for non-admin
            nonce = IOUtil.getNonce(ffts);
            algorithm = SecurityUtil.getSignatureAlgorithm(uzf.getPrivateKey());
            sig = SecurityUtil.sign(new ByteArrayInputStream(nonce), uzf.getPrivateKey(), algorithm);

            // make a proper signature object
            signature = new Signature(sig, algorithm, uzf.getCertificate());

            try {
                ffts.requestShutdown(signature, nonce);
                fail("FlatFileTrancheServer should throw an exception if non-admin requests shutdown.");
            } catch (Exception expected) {
            }
        } finally {
            IOUtil.safeClose(ffts);
        }
    }

    public void testAddCorruptedDataChunk() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testAddCorruptedDataChunk()");
        FlatFileTrancheServer ffdfs = null;
        try {
            ffdfs = new FlatFileTrancheServer(TempFileUtil.createTemporaryDirectory());
            ffdfs.getConfiguration().getUsers().add(DevUtil.getDevUser());
            ffdfs.saveConfiguration();
            TrancheServerTest.testAddCorruptedDataChunk(ffdfs, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
        } finally {
            IOUtil.safeClose(ffdfs);
        }
    }

    /**
     * Test that FFTS correctly reconstructs the list of known data hashes and meta-data hashes when a server is shut down and restarted.
     */
    public void testDataAndMetaDataHashListPersistence() throws Exception {
        TestUtil.printTitle("FlatFileTrancheServerTest:testDataAndMetaDataHashListPersistence()");

        final int dataChunksToMake = 100;
        final int metaDataChunksToMake = 100;

        // randomly make up the sizes
        final int maxDataSize = 1024;
        ArrayList<byte[]> dataChunks = new ArrayList();
        ArrayList<byte[]> metaDataChunks = new ArrayList();
        // make the data
        for (int i = 0; i < dataChunksToMake; i++) {
            byte[] randomData = new byte[(int) (1 + Math.random() * maxDataSize)];
            RandomUtil.getBytes(randomData);
            dataChunks.add(randomData);
        }

        // make the meta-data
        for (int i = 0; i < metaDataChunksToMake; i++) {
            metaDataChunks.add(DevUtil.createRandomMetaDataChunk());
        }

        // keep a list of the hashes
        BigHash[] sortedDataHashes = new BigHash[dataChunks.size()];
        BigHash[] sortedMetaDataHashes = new BigHash[metaDataChunks.size()];

        File fftsDir = TempFileUtil.createTemporaryDirectory();

        // make up a FFTS to initially test adding/getting hashes
        {
            FlatFileTrancheServer ffts = null;
            try {
                ffts = new FlatFileTrancheServer(fftsDir);
                ffts.getConfiguration().addUser(DevUtil.getDevUser());
                ffts.saveConfiguration();

                // add the data chunks
                for (int i = 0; i < dataChunks.size(); i++) {
                    sortedDataHashes[i] = new BigHash(dataChunks.get(i));
                    IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), sortedDataHashes[i], dataChunks.get(i));
                }
                // add the meta-data chunks
                for (int i = 0; i < metaDataChunks.size(); i++) {
                    sortedMetaDataHashes[i] = new BigHash(metaDataChunks.get(i));
                    IOUtil.setMetaData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, sortedMetaDataHashes[i], metaDataChunks.get(i));
                }

                // check that all exist
                {
                    int dataHashCount = 0;
                    for (int hashesRead = ffts.getDataHashes(BigInteger.valueOf(dataHashCount), BigInteger.valueOf(100)).length; hashesRead > 0; hashesRead = ffts.getDataHashes(BigInteger.valueOf(dataHashCount), BigInteger.valueOf(100)).length) {
                        dataHashCount += hashesRead;
                    }
                    // assert that it is the right size
                    assertEquals("Exepected " + dataChunksToMake + " data hashes.", dataChunksToMake, dataHashCount);
                }
                // check that all meta-data exist
                {
                    int metaDataHashCount = 0;
                    for (int hashesRead = ffts.getMetaDataHashes(BigInteger.valueOf(metaDataHashCount), BigInteger.valueOf(100)).length; hashesRead > 0; hashesRead = ffts.getMetaDataHashes(BigInteger.valueOf(metaDataHashCount), BigInteger.valueOf(100)).length) {
                        metaDataHashCount += hashesRead;
                    }
                    // assert that it is the right size
                    assertEquals("Exepected " + metaDataChunksToMake + " meta-data hashes.", metaDataChunksToMake, metaDataHashCount);
                }
            } finally {
                IOUtil.safeClose(ffts);
            }
        }

        // sort the hashes
        Arrays.sort(sortedDataHashes);
        Arrays.sort(sortedMetaDataHashes);

        // remake a FFTS and check that the cached data is loaded back in to memory
        {
            FlatFileTrancheServer ffts = null;
            try {
                ffts = new FlatFileTrancheServer(fftsDir);

                // wait to load any old data -- wait up to 30 seconds
                System.out.println("*** Waiting for project finding thread to complete. ***");
                ffts.waitToLoadExistingDataBlocks();
                System.out.println("*** Finished project finding thread ***");

                // check that all exist
                {
                    ArrayList<BigHash> dataHashesBuffer = new ArrayList();
                    for (BigHash[] buf = ffts.getDataHashes(BigInteger.ZERO, BigInteger.valueOf(100)); buf.length > 0; buf = ffts.getDataHashes(BigInteger.valueOf(dataHashesBuffer.size()), BigInteger.valueOf(100))) {
                        for (int i = 0; i < buf.length; i++) {
                            dataHashesBuffer.add(buf[i]);
                        }
                    }
                    BigHash[] dataHashes = dataHashesBuffer.toArray(new BigHash[0]);
                    // assert that it is the right size
                    assertEquals("Exepected " + dataChunksToMake + " data hashes.", dataChunksToMake, dataHashes.length);
                    // sort and check data hashes
                    Arrays.sort(dataHashes);
                    for (int i = 0; i < dataHashes.length; i++) {
                        assertEquals(sortedDataHashes[i], dataHashes[i]);
                    }
                }

                // check that all meta-data hashes still exist and are the same
                {
                    ArrayList<BigHash> metaDataHashesBuffer = new ArrayList();
                    for (BigHash[] buf = ffts.getMetaDataHashes(BigInteger.ZERO, BigInteger.valueOf(100)); buf.length > 0; buf = ffts.getMetaDataHashes(BigInteger.valueOf(metaDataHashesBuffer.size()), BigInteger.valueOf(100))) {
                        for (int i = 0; i < buf.length; i++) {
                            metaDataHashesBuffer.add(buf[i]);
                        }
                    }
                    // convert to an array
                    BigHash[] metaDataHashes = metaDataHashesBuffer.toArray(new BigHash[0]);
                    // assert that it is the right size
                    assertEquals("Exepected " + metaDataChunksToMake + " meta-data hashes.", metaDataChunksToMake, metaDataHashes.length);
                    // sort and check meta-data hashes
                    Arrays.sort(metaDataHashes);
                    for (int i = 0; i < metaDataHashes.length; i++) {
                        assertEquals(sortedMetaDataHashes[i], metaDataHashes[i]);
                    }
                }
            } finally {
                IOUtil.safeClose(ffts);
            }
        }
    }

    /**
     * Tests that the project file index is appropriately kept by the server including when new projects are added and when a server is rebooted.
     */
    public void testProjectFileIndex() {
        // need to update to use new connection scheme
        if (true) {
            throw new TodoException();
        }
//        File dir = null;
//        File directory1 = null;
//        File directory2 = null;
//
//        try {
//            dir = TempFileUtil.createTemporaryDirectory();
//            // make the project's directory'
//            directory1 = createProjectDirectory(dir, "project-1");
//            directory2 = createProjectDirectory(dir, "project-2");
//        }
//
//        catch (Exception ex) {
//            fail(ex.getMessage());
//        }
//
//        // set up a new ffdfs
//        FlatFileTrancheServer ffdfs = null;
//
//        // wrap the DFS using the custom server
//        Server server = null;
//        // connect to this server using a remote client
//        TrancheServer rdfs = null;
//
//        // upload the projects
//        BigHash hash1 = null;
//        BigHash hash2 = null;
//
//        try {
//            // set up a new ffdfs
//            ffdfs = new FlatFileTrancheServer(dir);
//            // register the default user
//            User user = DevUtil.getDevUser();
//
//            ffdfs.getConfiguration().addUser(user);
//
//            // wrap the DFS using the custom XML server
//            server = new Server(ffdfs, DevUtil.getDefaultTestPort());
//            server.start();
//            // connect to this server using a remote client
//            rdfs = IOUtil.connect(IOUtil.createURL("127.0.0.1", server.getPort(), false));
//
//            // upload the projects
//            hash1 = uploadProject(directory1, rdfs);
//            hash2 = uploadProject(directory2, rdfs);
//
//            // verify that the given hashes are in the list
//            assertProjectHashesExist(new BigHash[]{hash1, hash2}, rdfs);
//
//        }
//
//        catch (Exception e) {
//            fail(e.getMessage());
//        }
//
//        finally {
//            IOUtil.safeClose(rdfs);
//            IOUtil.safeClose(server);
//            IOUtil.safeClose(ffdfs);
//        }
//
//        // make a new server
//        FlatFileTrancheServer ffdfs2 = null;
//        try {
//            ffdfs2 = new FlatFileTrancheServer(dir);
//
//            // wait for the project file finding thread to complete
//            ffdfs2.waitToLoadExistingDataBlocks();
//
////            // Clobber the thread
////            int timeoutCount=0;
////            while (ffdfs2.projectFindingThread.isAlive()) {
////                ffdfs2.projectFindingThread.interrupt();
////                timeoutCount++;
////                if (timeoutCount >= 10) {
////                    break;
////                }
////            }
//
//            // verify that the given hashes are in the list
//            assertProjectHashesExist(new BigHash[]{hash1, hash2}, ffdfs2);
//        }
//
//        catch (Exception e) {
//            fail(e.getMessage());
//        }
//
//        finally {
//            ffdfs2.close();
//        }
//
//        // attempt to clean up
//        IOUtil.recursiveDeleteWithWarning(dir);
    }

    private BigHash uploadProject(final File directory1, final TrancheServer rdfs) throws IOException, Exception, GeneralSecurityException {
        AddFileTool aft = new AddFileTool();
        aft.setUserCertificate(DevUtil.getDevAuthority());
        aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
        aft.addServerToUse(rdfs.getHost());
        aft.setTitle("The title.");
        aft.setDescription("The description.");
        // register a command-line listener
        CommandLineAddFileToolListener claftl = new CommandLineAddFileToolListener(aft, System.out);
        aft.addListener(claftl);

        // check that the hashes match
        aft.setFile(directory1);
        return aft.execute().getHash();
    }

    private void assertProjectHashesExist(BigHash[] hashes, TrancheServer ts) throws Exception {
        BigHash[] projectHashes = ts.getProjectHashes(BigInteger.ZERO, BigInteger.valueOf(10));
        assertTrue("Expected " + hashes.length + " project hashes but found " + projectHashes.length + ".", projectHashes.length == hashes.length);
        // sort both
        Arrays.sort(projectHashes);
        Arrays.sort(hashes);
        for (int i = 0; i < hashes.length; i++) {
            assertTrue("Expected the hash to be the same.", projectHashes[i].compareTo(hashes[i]) == 0);
        }
    }

    private File createProjectDirectory(final File dir, String name) throws IOException {
        // make up some random content
        File directory = new File(dir, name);
        directory.mkdirs();

        // the files to add
        File[] files = new File[3];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(directory, i + ".txt");
            Utils.makeTempFile(Utils.makeRandomData((int) (10000 * Math.random())), files[i]);
        }
        return directory;
    }

    public void testSetDataNoHosts() throws Exception {

        final String HOST1 = "bryan.com";

        TestUtil.printTitle("FlatFileTrancheServerTest:testSetDataNoHosts");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts);

            final byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            final BigHash hash = new BigHash(chunk);

            final String[] hosts = {};

            // First try directly with RoutingTrancheServer
            try {

                PropagationReturnWrapper prw = IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testSetMetaDataNoHosts() throws Exception {

        final String HOST1 = "bryan.com";

        TestUtil.printTitle("FlatFileTrancheServerTest:testSetMetaDataNoHosts");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts);

            final byte[] chunk = DevUtil.createRandomMetaDataChunk(RandomUtil.getInt(5)+1, RandomUtil.getBoolean());
            final BigHash hash = DevUtil.getRandomBigHash();

            final String[] hosts = {};

            // First try directly with RoutingTrancheServer
            try {

                PropagationReturnWrapper prw = IOUtil.setMetaData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), true, hash, chunk, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testGetNoncesNoHosts() throws Exception {

        final String HOST1 = "bryan.com";

        TestUtil.printTitle("FlatFileTrancheServerTest:testGetNonces");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts);

            final String[] hosts = {};

            // First try directly with RoutingTrancheServer
            try {

                IOUtil.getNonces(ffts, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testDeleteDataNoHosts() throws Exception {

        final String HOST1 = "bryan.com";

        TestUtil.printTitle("FlatFileTrancheServerTest:testDeleteDataNoHosts");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts);

            final BigHash hash = DevUtil.getRandomBigHash();

            final String[] hosts = {};

            // First try directly with RoutingTrancheServer
            try {

                PropagationReturnWrapper prw = IOUtil.deleteData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testDeleteMetaDataNoHosts() throws Exception {

        final String HOST1 = "bryan.com";

        TestUtil.printTitle("FlatFileTrancheServerTest:testDeleteMetaDataNoHosts");
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));

        try {
            testNetwork.start();

            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts);

            final BigHash hash = DevUtil.getRandomBigHash();

            final String[] hosts = {};

            // First try directly with RoutingTrancheServer
            try {

                PropagationReturnWrapper prw = IOUtil.deleteMetaData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, hosts);

                fail("When calling RoutingTrancheServer without hosts, should try NoHostProvidedException.");
            } catch (NoHostProvidedException nhpe) {
                // Expected
            }
        } finally {
            testNetwork.stop();
        }
    }
}
