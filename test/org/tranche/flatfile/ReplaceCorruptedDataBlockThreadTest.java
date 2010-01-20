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

import org.tranche.exceptions.TodoException;
import org.tranche.util.TrancheTestCase;

/**
 * <p>Tests functionality that repairs corrupted DataBlock's.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ReplaceCorruptedDataBlockThreadTest extends TrancheTestCase {

    public void testTodo() throws Exception {
        // need to update to new connection scheme
        if (true) {
            throw new TodoException();
        }
    }
//
//    /**
//     * <p>Tests that will not repair if set ConfigKey.CORRUPTED_DB_ALLOWED_TO_FIX.</p>
//     * @throws java.lang.Exception
//     */
//    public void testNoRepairCorruptedDataBlockHeader() throws Exception {
//        testCanRepairCorruptedDataBlock(true, 5, false, true);
//    }
//
//    /**
//     * <p>Tests that will not repair if set ConfigKey.CORRUPTED_DB_ALLOWED_TO_FIX.</p>
//     * @throws java.lang.Exception
//     */
//    public void testNoRepairCorruptedDataBlockBody() throws Exception {
//        testCanRepairCorruptedDataBlock(false, 5, false, true);
//    }
//
//    /**
//     * <p>Tests that repairs corrupted data block. Corruption is in the HEADER.</p>
//     * <p>This test allows use a ProjectFindingThread and HashSpanFixingThread to repair.</p>
//     * @throws java.lang.Exception
//     */
//    public void testCanRepairCorruptedDataBlockHeader() throws Exception {
//        testCanRepairCorruptedDataBlock(true, 5, false, false);
//    }
//
//    /**
//     * <p>Tests that repairs corrupted data block. Corruption is in the BODY.</p>
//     * <p>This test allows use a ProjectFindingThread and HashSpanFixingThread to repair.</p>
//     * @throws java.lang.Exception
//     */
//    public void testCanRepairCorruptedDataBlockBody() throws Exception {
//        testCanRepairCorruptedDataBlock(false, 5, false, false);
//    }
//
//    /**
//     * <p>Tests that repairs corrupted data block. Corruption is in the HEADER.</p>
//     * <p>This test allows use of HashSpanFixingThread to repair (NOT ProjectFindingThread).</p>
//     * @throws java.lang.Exception
//     */
//    public void testCanRepairCorruptedDataBlockHeaderWithoutProjectFindingThread() throws Exception {
//        testCanRepairCorruptedDataBlock(true, 5, true, false);
//    }
//
//    /**
//     * <p>Tests that repairs corrupted data block. Corruption is in the BODY.</p>
//     * <p>This test allows use of HashSpanFixingThread to repair (NOT ProjectFindingThread).</p>
//     * @throws java.lang.Exception
//     */
//    public void testCanRepairCorruptedDataBlockBodyWithoutProjectFindingThread() throws Exception {
//        testCanRepairCorruptedDataBlock(false, 5, true, false);
//    }
//
//    /**
//     * <p>Demonstrate the a corrupted data block can be fixed. Corrupted data blocks will end early, and are found when they throw an UnexpectedEndOfDataBlockException.</p>
//     * <p>I've noticed that data blocks that some servers are indefinitely corrupted, meaning that the data is not only unavailable on that server, but any hashes that would go into that data block will indefinitely result in exceptions. This effectively prevents a branch of the b-tree from growing.</p>
//     * <p>New functionality is being implemented to handle this inevitability. Corruption can happen under many circumstances (like I/O exceptions), but is most likely if a server process is rudely killed during I/O to data blocks.</p>
//     * @param isCreateCorruptionInHeader If true, will corrupt the header. Else corrupts the payload. Both should be tested separately.
//     * @param numChunks The number of chunks of data and meta data to be tested. Some additional noise chunks tested. The test will effectively test 4 * numChunks.
//     * @param turnOffProjectFindingThread If true, will turn off the project finding thread. This tests that the healing thread can fix DataBlocks, too.
//     * @param shouldTurnOffFix If true, won't repair. If false, should repair.
//     * @throws java.lang.Exception
//     */
//    public void testCanRepairCorruptedDataBlock(boolean isCreateCorruptionInHeader, int numChunks, boolean turnOffProjectFindingThread, boolean shouldTurnOffFix) throws Exception {
//        // Set to use fake servers. This will notify servers to wait for servers to be available.
//        TestUtil.setTestUsingFakeCoreServers(true);
//
//        // Conditionally turn off project finding thread. In finally block, will turn back on
//        TestUtil.setTestingTurnOffProjectFindingThread(turnOffProjectFindingThread);
//
//        // Let ServerUtil know ahead of time that we are going to use two test servers. That way,
//        // the thread that fixes corrupted data blocks will block, and ServerUtil will know when to
//        // end the block.
//        ServerUtil.setExpectedTestServersCount(2);
//
//        /* We need to test two types of corrupted data blocks:
//         * 1. End of data block in the payload portion of the data block, meaning the header is intact.
//         * 2. End of data block in the header portion of the data block, meaning the header is not intact.
//         */
//
//        System.out.println();
//        System.out.println("--- --- -- testCanRepairCorruptedDataBlock(" + isCreateCorruptionInHeader + "," + numChunks + ") -- --- ---");
//        System.out.println();
//
//        System.out.println();
//        System.out.println("=======================================================");
//        System.out.println("=== Step 1: Creating servers, adding data/meta data.");
//        System.out.println("=======================================================");
//        System.out.println();
//
//        if (numChunks > 100) {
//            throw new Exception("Please run test with less than 100. That would take entirely too long.");
//        }
//
//        // Use FlatFileTrancheServer so authentic. Better than simply mimicking DBU.
//        FlatFileTrancheServer ffts1 = null, ffts2 = null;
//
//        // Need to use Server. Originally didn't want to, but keeps instantiatign new FFTS
//        // when trying to connect in project finding thread
//        Server s1 = null, s2 = null;
//        TrancheServer rs1 = null, rs2 = null;
//
//        File dataRoot1 = null, dataRoot2 = null;
//        File tmpDataBlockFile = null;
//
//        try {
//            dataRoot1 = TempFileUtil.createTemporaryDirectory();
//            assertTrue("Directory better exist.", dataRoot1.exists());
//
//            dataRoot2 = TempFileUtil.createTemporaryDirectory();
//            assertTrue("Directory better exist.", dataRoot2.exists());
//
//            ffts1 = new FlatFileTrancheServer(dataRoot1);
//            ffts1.getConfiguration().getUsers().add(DevUtil.getDevUser());
//
//            // If not allowed to fix, set value
////            if (shouldTurnOffFix) {
//            Configuration config = ffts1.getConfiguration();
//            config.setValue(ConfigKeys.CORRUPTED_DB_ALLOWED_TO_FIX, String.valueOf(!shouldTurnOffFix));
//
//            // Set so stores value
//            IOUtil.setConfiguration(ffts1, config, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
////            }
//
//            ffts2 = new FlatFileTrancheServer(dataRoot2);
//            ffts2.getConfiguration().getUsers().add(DevUtil.getDevUser());
//
////            s1 = new Server(ffts1,1500);
////            s1.start();
////
////            s2 = new Server(ffts2,1501);
////            s2.start();
////
////            rs1 = IOUtil.connect("tranche://127.0.0.1:1500");
////            rs2 = IOUtil.connect("tranche://127.0.0.1:1501");
////
////            TestUtil.setTestUsingFakeCoreServers(true);
////            String[] testUrls = {
////                "tranche://127.0.0.1:1500", "tranche://127.0.0.1:1501"
////            };
////            ServerUtil.setTestServers(testUrls);
////
////            assertTrue("Server should be online: "+"tranche://127.0.0.1:1500",ServerUtil.isServerOnline("tranche://127.0.0.1:1500"));
////            assertTrue("Server should be online: "+"tranche://127.0.0.1:1501",ServerUtil.isServerOnline("tranche://127.0.0.1:1501"));
//
//            /* Use a prefix to create chunks that end up in the same data block.
//             * We'll create additional chunks to make sure process doesn't impact.
//             */
//            final String base64HashPrefix = RandomUtil.getString(2);
//            String base16HashPrefix = null;
//
//            tmpDataBlockFile = TempFileUtil.createTemporaryFile(base64HashPrefix);
//
//            List<BigHash> dataHashes = new ArrayList();
//            List<BigHash> metaHashes = new ArrayList();
//
//            long totalDataWrittenToFailedDataBlock = 0;
//
//            /* For each iteration, add the following four:
//             * 1. Data chunk w/ hash starting with hashPrefix.
//             * 2. Meta data chunk w/ hash starting with hashPrefix.
//             * 3. Data chunk w/ hash not starting with hashPrefix.
//             * 4. Meta data chunk w/ hash not starting with hashPrefix.
//             *
//             * Anything w/ hashPrefix will end up in the corrupted data block.
//             */
//            for (int i = 0; i < numChunks; i++) {
//                // Create data chunk start w/ prefix. Assert.
//                byte[] dataChunkInside = DevUtil.createRandomDataChunkStartsWith(base64HashPrefix);
//                BigHash dataHashInside = new BigHash(dataChunkInside);
//                assertTrue("Hash should start with prefix " + base64HashPrefix + ", but doesn't: " + dataHashInside, dataHashInside.toString().startsWith(base64HashPrefix));
//
//                String base16Hash = Base16.encode(dataHashInside.toByteArray());
//
//                // Lazy-load the base 16 hash prefix. We'll need this to work with data block file paths.
//                if (base16HashPrefix == null) {
//                    base16HashPrefix = base16Hash.substring(0, 2);
//                }
//                assertTrue("Base 16 hash must start with same prefix " + base16HashPrefix + ", but doesn't: " + base16Hash, base16Hash.startsWith(base16HashPrefix));
//
//                IOUtil.setData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHashInside, dataChunkInside);
//                IOUtil.setData(ffts2, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHashInside, dataChunkInside);
//                dataHashes.add(dataHashInside);
//
//                // Add bytes only for chunks going to failing data block
//                totalDataWrittenToFailedDataBlock += dataChunkInside.length;
//
//                // Create meta chunk start w/ prefix. Assert.
//                byte[] metaChunkInside = DevUtil.createRandomMetaDataChunk();
//                BigHash metaHashInside = DevUtil.getRandomBigHashStartsWith(base64HashPrefix);
//                assertTrue("Hash should start with prefix " + base64HashPrefix + ", but doesn't: " + metaHashInside, metaHashInside.toString().startsWith(base64HashPrefix));
//
//                base16Hash = Base16.encode(metaHashInside.toByteArray());
//                assertTrue("Base 16 hash must start with same prefix " + base16HashPrefix + ", but doesn't: " + base16Hash, base16Hash.startsWith(base16HashPrefix));
//
//                IOUtil.setMetaData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaHashInside, metaChunkInside);
//                IOUtil.setMetaData(ffts2, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaHashInside, metaChunkInside);
//                metaHashes.add(metaHashInside);
//
//                // Add bytes only for chunks going to failing data block
//                totalDataWrittenToFailedDataBlock += metaChunkInside.length;
//
//                // Create data chunk not starting w/ prefix. Assert.
//                boolean isBase16StartWithHashPrefix = true;
//                int count = 0;
//                byte[] dataChunkOutside = null;
//                BigHash dataHashOutside = null;
//
//                // Different Base64 make different Base16 more likely, but try up to a few time.
//                while (isBase16StartWithHashPrefix && count < 20) {
//                    dataChunkOutside = DevUtil.createRandomDataChunkDoesNotStartWith(base64HashPrefix);
//                    dataHashOutside = new BigHash(dataChunkOutside);
//                    assertFalse("Hash should not start with prefix " + base64HashPrefix + ", but does: " + dataHashOutside, dataHashOutside.toString().startsWith(base64HashPrefix));
//
//                    base16Hash = Base16.encode(dataHashOutside.toByteArray());
//                    isBase16StartWithHashPrefix = base16Hash.startsWith(base16HashPrefix);
//                    count++;
//                }
//                assertFalse("Base 16 hash must not start with prefix " + base16HashPrefix + ", but does: " + base16Hash, base16Hash.startsWith(base16HashPrefix));
//
//                IOUtil.setData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHashOutside, dataChunkOutside);
//                IOUtil.setData(ffts2, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHashOutside, dataChunkOutside);
//                dataHashes.add(dataHashOutside);
//
//                // Create meta chunk not starting w/ prefix. Assert.
//                byte[] metaChunkOutside = null;
//                BigHash metaHashOutside = null;
//                isBase16StartWithHashPrefix = true;
//                count = 0;
//
//                // Different Base64 make different Base16 more likely, but try up to a few time.
//                while (isBase16StartWithHashPrefix && count < 20) {
//                    metaChunkOutside = DevUtil.createRandomMetaDataChunk();
//                    metaHashOutside = DevUtil.getRandomBigHashNotStartWith(base64HashPrefix);
//                    assertFalse("Hash should not start with prefix " + base64HashPrefix + ", but does: " + metaHashOutside, metaHashOutside.toString().startsWith(base64HashPrefix));
//
//                    base16Hash = Base16.encode(metaHashOutside.toByteArray());
//                    isBase16StartWithHashPrefix = base16Hash.startsWith(base16HashPrefix);
//                    count++;
//                }
//
//                assertFalse("Base 16 hash must not start with prefix " + base16HashPrefix + ", but does: " + base16Hash, base16Hash.startsWith(base16HashPrefix));
//
//                IOUtil.setMetaData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaHashOutside, metaChunkOutside);
//                IOUtil.setMetaData(ffts2, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaHashOutside, metaChunkOutside);
//                metaHashes.add(metaHashOutside);
//            }
//
//            assertEquals("Expecting certain number of data hashes.", 2 * numChunks, dataHashes.size());
//            assertEquals("Expecting certain number of meta data hashes.", 2 * numChunks, metaHashes.size());
//
//            for (BigHash h : dataHashes) {
//                assertTrue("Should have data chunk.", IOUtil.hasData(ffts1, h));
//                assertTrue("Should have data chunk.", IOUtil.hasData(ffts2, h));
//            }
//            for (BigHash h : metaHashes) {
//                assertTrue("Should have meta chunk.", IOUtil.hasMetaData(ffts1, h));
//                assertTrue("Should have meta chunk.", IOUtil.hasMetaData(ffts2, h));
//            }
//
//            // Close down the servers. Going to do some dirty work.
//            // Note that second server doesn't necessarily need to be closed,
//            // but it is useful to restart since prints out information about
//            // data it loaded.
////            IOUtil.safeClose(rs1);
////            IOUtil.safeClose(rs2);
////            IOUtil.safeClose(s1);
////            IOUtil.safeClose(s2);
//            IOUtil.safeClose(ffts1);
//            IOUtil.safeClose(ffts2);
//
//            System.out.println();
//            System.out.println("=======================================================");
//            System.out.println("=== Step 2: Finding and corrupting data block.");
//            System.out.println("=======================================================");
//            System.out.println();
//
//            /* Here's the dirty work: time to corrupt the data block.
//             * How we corrupt it depends on the flag.
//             */
//            File dataDirectory = new File(dataRoot1, "data");
//            assertTrue("Data directory for server must exist.", dataDirectory.exists());
//
//            File dataBlockToCorrupt = new File(dataDirectory, base16HashPrefix);
//            assertTrue("Data block we are trying to corrupt must exist: " + dataBlockToCorrupt.getAbsolutePath(), dataBlockToCorrupt.exists());
//
//            // This is the size of the entire file.
//            final long dataBlockSize = dataBlockToCorrupt.length();
//
//            // This is how many bytes are in the header.
//            final long headerSize = DataBlock.bytesToRead;
//
//            // Number of bytes in header already written. Need to snip somewhere in here to damage header.
//            final long addedHeadersBytes = DataBlock.bytesPerEntry * 2 * numChunks;
//
//            // The index of the very last byte of written data
//            final long writtenBodyLastByteIndex = headerSize + totalDataWrittenToFailedDataBlock - 1;
//
//            // Calculate how many headers are intact
//            int intactHeadersTmp;
//
//            System.out.println("Data block: " + dataBlockToCorrupt.getAbsolutePath() + " (Size: " + String.valueOf(dataBlockSize) + " bytes.)");
//            System.out.println("  * Header: 0 - " + String.valueOf(headerSize - 1) + " (Written portion: 0 - " + String.valueOf(addedHeadersBytes - 1) + ")");
//            System.out.println("  * Body: " + String.valueOf(headerSize) + " - " + String.valueOf(dataBlockSize - 1) + " (Written portion: " + String.valueOf(headerSize) + " - " + String.valueOf(writtenBodyLastByteIndex) + ")");
//            long snipPoint = -1;
//            if (isCreateCorruptionInHeader) {
//
//                // Create a snip point somewhere between 100 bytes and the end of the header
//                snipPoint = (int) (RandomUtil.getInt((int) addedHeadersBytes - 50 - 100) + 100);
//
//                // The number of intact headers is all the headers that appear before snip, not the header snipped or anything after it
//                intactHeadersTmp = (int) Math.floor((double) snipPoint / (double) DataBlock.bytesPerEntry);
//
//                System.out.println("  * Going to snip in header at " + snipPoint + ", should be " + intactHeadersTmp + " intact headers.");
//                assertTrue("Snip point must be greater than 100 bytes.", snipPoint >= 100);
//                assertTrue("Snip point must be less than size of written portion of header minus 50 bytes.", snipPoint <= addedHeadersBytes - 50);
//            } else {
//
//                // Create a snip point somewhere between 100 bytes after end of header and the end of the file - 50 bytes
//                snipPoint = (int) (RandomUtil.getInt((int) (writtenBodyLastByteIndex - 50 - (100 + headerSize))) + 100 + headerSize);
//
//                System.err.println("DEBUG> CREATED SNIP POINT IN BODY AT: " + snipPoint + " (header ended at " + headerSize + ", body ended at " + writtenBodyLastByteIndex + ")");
//
//                // All headers should be intact -- we're snipping the body!
//                intactHeadersTmp = 2 * numChunks;
//
//                System.out.println("  * Going to snip in body at " + snipPoint);
//                assertTrue("Snip point must be greater than header size plus 100 bytes.", snipPoint >= headerSize + 100);
//                assertTrue("Snip point must be less than size of end of written portion of file minus 50 bytes.", snipPoint <= writtenBodyLastByteIndex - 50);
//            }
//
//            // Keep a final value of the number of intact headers. Used for when header section
//            // is snipped.
//            final int intactHeaders = intactHeadersTmp;
//
//            // Copy over file to temporary location
//            IOUtil.copyFile(dataBlockToCorrupt, tmpDataBlockFile);
//
//            // Delete data block. Going to recreate as a corrupted file.
//            IOUtil.safeDelete(dataBlockToCorrupt);
//            assertFalse("Data block must not exist.", dataBlockToCorrupt.exists());
//
//            // Recreate data block. Going to copy over only a certain number of chunks.
//            dataBlockToCorrupt.createNewFile();
//            assertTrue("Data block must exist.", dataBlockToCorrupt.exists());
//
//            BufferedOutputStream out = null;
//            BufferedInputStream in = null;
//
//            try {
//                out = new BufferedOutputStream(new FileOutputStream(dataBlockToCorrupt));
//                in = new BufferedInputStream(new FileInputStream(tmpDataBlockFile));
//
//                // Move over specified number of bytes
//                IOUtil.getBytes(out, in, snipPoint);
//            } finally {
//                IOUtil.safeClose(in);
//                IOUtil.safeClose(out);
//            }
//
//            assertEquals("Expecting data block file to be snipped to specified size.", snipPoint, dataBlockToCorrupt.length());
//
//            System.out.println();
//            System.out.println("=======================================================");
//            System.out.println("=== Step 3: Restarting servers, verifying repair.");
//            System.out.println("=======================================================");
//            System.out.println();
//
//            // Start servers back up.
//            ffts2 = new FlatFileTrancheServer(dataRoot2);
//            ffts2.getConfiguration().getUsers().add(DevUtil.getDevUser());
//
//            ffts1 = new FlatFileTrancheServer(dataRoot1);
//            ffts1.getConfiguration().getUsers().add(DevUtil.getDevUser());
//
//            s1 = new Server(ffts1, 1500);
//            s1.start();
//
//            s2 = new Server(ffts2, 1501);
//            s2.start();
//
//            rs1 = IOUtil.connect("tranche://127.0.0.1:1500");
//            rs2 = IOUtil.connect("tranche://127.0.0.1:1501");
//
//            String[] testUrls = {
//                "tranche://127.0.0.1:1500", "tranche://127.0.0.1:1501"
//            };
//
//            ServerUtil.setTestServers(testUrls);
//            ServerUtil.waitForTestServersStartup(testUrls.length);
//
//            assertTrue("Server should be online: " + "tranche://127.0.0.1:1500", ServerUtil.isServerOnline("tranche://127.0.0.1:1500"));
//            assertTrue("Server should be online: " + "tranche://127.0.0.1:1501", ServerUtil.isServerOnline("tranche://127.0.0.1:1501"));
//
//            // If turned off fixing, expect an exception when accessing data
//            if (shouldTurnOffFix) {
//                try {
//                    for (BigHash h : dataHashes) {
//                        assertTrue("Should have data chunk.", IOUtil.hasData(ffts1, h));
//                        byte[] dataChunk = (byte[])IOUtil.getData(ffts1, h, false).getReturnValueObject();
//
//                        if (dataChunk == null) {
//                            fail("Data chunk should not be null.");
//                        }
//                    }
//                    for (BigHash h : metaHashes) {
//                        assertTrue("Should have meta chunk.", IOUtil.hasMetaData(ffts1, h));
//                        byte[] metaChunk = (byte[])IOUtil.getMetaData(ffts1, h, false).getReturnValueObject();
//
//                        if (metaChunk == null) {
//                            fail("Meta data chunk should not be null.");
//                        }
//                    }
//                    fail("Should throw an exception. Repairs shouldn't occur, and methods should throw an exception.");
//                } catch (UnexpectedEndOfDataBlockException ex) {
//                    // This is expected. Bail on test -- we're done.
//                    return;
//                } catch (RuntimeException ex) {
//                    // This is expected. Bail on test -- we're done.
//                    return;
//                }
//            }
//
//            // This will wait the project finding thread if it catches the bad data block...
//            if (!turnOffProjectFindingThread) {
//                ffts1.waitToLoadExistingDataBlocks();
//                ffts2.waitToLoadExistingDataBlocks();
//
//                // Sleep a bit perchance the healing thread caught it!
//                // This should give sufficient time for something to get queued,
//                // and the wait called later will help
//                Thread.sleep(5000);
//            } else {
//                // Manually load known hashes. This is what the project finding thread
//                // does, but we want to test the healing thread's ability to repair.
//                // This will allow the healing thread to do all the work.
//                for (BigHash h : dataHashes) {
//                    ffts1.getDataBlockUtil().dataHashes.add(h);
//                    ffts2.getDataBlockUtil().dataHashes.add(h);
//                }
//                for (BigHash h : metaHashes) {
//                    ffts1.getDataBlockUtil().metaDataHashes.add(h);
//                    ffts2.getDataBlockUtil().metaDataHashes.add(h);
//                }
//
//                // Simulate the wait to allow the healing thread to start working
//                Thread.sleep(5000);
//            }
//
//            // ... and this will wait for the heal thread if it catches the bad data block!
//            ffts1.getDataBlockUtil().waitUntilFinished(30 * 1000);
//
//            // Second server didn't receive any damage. It should have everything.
//            for (BigHash h : dataHashes) {
//                assertTrue("Should have data chunk.", IOUtil.hasData(ffts2, h));
//            }
//            for (BigHash h : metaHashes) {
//                assertTrue("Should have meta chunk.", IOUtil.hasMetaData(ffts2, h));
//            }
//
//            // How much data was salvaged and repaired depends on where the damage was done.
//            if (isCreateCorruptionInHeader) {
//                /* If corrupted in header, it should recover every chunk whose header
//                 * is intact. We can calculate this with very little math.
//                 * Value is already stored in intactHeaders.
//                 */
//                // Expect the salvaged chunks (anything with intact headers) plus the number
//                // of unaffected chunks, which is 2 * numChunks (data + meta data outside the effected
//                // data block)
//                final int expectedTotalChunks = intactHeaders + 2 * numChunks;
//                int foundChunks = 0;
//                for (BigHash h : dataHashes) {
//                    if (IOUtil.hasData(ffts1, h)) {
//                        foundChunks++;
//                    }
//                }
//                for (BigHash h : metaHashes) {
//                    if (IOUtil.hasMetaData(ffts1, h)) {
//                        foundChunks++;
//                    }
//                }
//                assertEquals("Expecting a certain number of chunks to be found.", expectedTotalChunks, foundChunks);
//
//            } else {
//
//                /* If corrupted in body, everything should have been repaired. All the hashes
//                 * were there, and the server should have gone to the second server to grab
//                 * the data.
//                 */
//                int foundChunks = 0;
//                for (BigHash h : dataHashes) {
//                    if (IOUtil.hasData(ffts1, h)) {
//                        foundChunks++;
//                    }
//                }
//                for (BigHash h : metaHashes) {
//                    if (IOUtil.hasMetaData(ffts1, h)) {
//                        foundChunks++;
//                    }
//                }
//
//                int dataChunkCount = 0;
//                for (BigHash h : dataHashes) {
//                    dataChunkCount++;
//                    assertTrue("Should have data chunk #" + dataChunkCount + " of " + dataHashes.size() + ".", IOUtil.hasData(ffts1, h));
//
//                    // Verify that the bytes were repaired
//                    byte[] bytes = (byte[])IOUtil.getData(ffts1, h, false).getReturnValueObject();
//                    assertNotNull("Bytes better exist.", bytes);
//                    assertEquals("Expecting bytes to be of certain size.", h.getLength(), bytes.length);
//
//                    BigHash verifyHash = new BigHash(bytes);
//                    assertEquals("Expecting hashes to match.", h, verifyHash);
//                }
//                int metaChunkCount = 0;
//                for (BigHash h : metaHashes) {
//                    metaChunkCount++;
//                    assertTrue("Should have meta chunk #" + metaChunkCount + ".", IOUtil.hasMetaData(ffts1, h));
//
//                    // Verify that the bytes were repaired
//                    byte[] bytes = (byte[])IOUtil.getMetaData(ffts1, h, false).getReturnValueObject();
//                    assertNotNull("Bytes better exist.", bytes);
//
//                // Nothing to check. Meta data has arbitrary hash which cannot
//                // be created by meta data chunk alone.
//                }
//            }
//
//        } finally {
//
//            IOUtil.safeClose(rs1);
//            IOUtil.safeClose(rs2);
//            IOUtil.safeClose(s1);
//            IOUtil.safeClose(s2);
//            IOUtil.safeClose(ffts1);
//            IOUtil.safeClose(ffts2);
//            IOUtil.recursiveDeleteWithWarning(dataRoot1);
//            IOUtil.recursiveDeleteWithWarning(dataRoot2);
//            IOUtil.safeDelete(tmpDataBlockFile);
//
//            TestUtil.setTestingTurnOffProjectFindingThread(false);
//            IOUtil.purgeAndCloseConnectionsForTest();
//        }
//    } // testCanRepairCorruptedDataBlock(boolean)
}