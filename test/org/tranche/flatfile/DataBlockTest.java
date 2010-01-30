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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.tranche.annotations.Todo;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.flatfile.logs.DataBlockUtilLog;
import org.tranche.hash.*;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataUtil;
import org.tranche.users.UserZipFile;
import org.tranche.util.*;

/**
 * Tests that the DataBlock object correctly stores and retrieves data.
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class DataBlockTest extends TrancheTestCase {

    public void testLogDeletions() throws Exception {
        TestUtil.printTitle("DataBlockTest:testLogDeletions()");
        testLogDeletions(true);
    }

    public void testNoLogDeletions() throws Exception {
        TestUtil.printTitle("DataBlockTest:testNoLogDeletions()");
        testLogDeletions(false);
    }

    public void testLogDeletions(boolean shouldLog) throws Exception {
        FlatFileTrancheServer ffts = null;
        File dataDir = null;
        try {
            dataDir = TempFileUtil.createTemporaryDirectory("testLogDeletions" + (shouldLog ? "Logging" : "NotLogging"));
            ffts = new FlatFileTrancheServer(dataDir);

            // Disable configuration caching
            ffts.setConfigurationCacheTime(0);

            ffts.getConfiguration().setValue(ConfigKeys.DATABLOCK_LOG_DATA_CHUNK_DELETIONS, String.valueOf(shouldLog));
            ffts.getConfiguration().setValue(ConfigKeys.DATABLOCK_LOG_META_DATA_CHUNK_DELETIONS, String.valueOf(shouldLog));

            DataBlockUtil dbu = ffts.getDataBlockUtil();

            // Keep these numbers even or the tests won't work!
            int totalDataToAdd = 10;
            int totalMetaDataToAdd = 10;

            Set<BigHash> dataChunksKept = new HashSet();
            Set<BigHash> dataChunksDeleted = new HashSet();

            Set<BigHash> metaDataChunksKept = new HashSet();
            Set<BigHash> metaDataChunksDeleted = new HashSet();

            // Add data
            for (int i = 0; i < totalDataToAdd; i++) {
                byte[] bytes = DevUtil.createRandomDataChunkVariableSize();
                BigHash hash = new BigHash(bytes);

                if (i % 2 == 0) {
                    if (dataChunksKept.contains(hash)) {
                        i--;
                        continue;
                    }

                    dbu.addData(hash, bytes);
                    dataChunksKept.add(hash);
                } else {
                    if (dataChunksDeleted.contains(hash)) {
                        i--;
                        continue;
                    }

                    dbu.addData(hash, bytes);
                    dataChunksDeleted.add(hash);
                }
            }

            // Add meta data
            for (int i = 0; i < totalMetaDataToAdd; i++) {
                byte[] bytes = DevUtil.createRandomMetaDataChunk();
                BigHash hash = DevUtil.getRandomBigHash();

                if (i % 2 == 0) {
                    if (metaDataChunksKept.contains(hash)) {
                        i--;
                        continue;
                    }

                    dbu.addMetaData(hash, bytes);
                    metaDataChunksKept.add(hash);
                } else {
                    if (metaDataChunksDeleted.contains(hash)) {
                        i--;
                        continue;
                    }

                    dbu.addMetaData(hash, bytes);
                    metaDataChunksDeleted.add(hash);
                }
            }

            // Delete appropriate data
            for (BigHash h : dataChunksDeleted) {
                dbu.deleteData(h, "test");
            }

            // Delete appropriate meta data
            for (BigHash h : metaDataChunksDeleted) {
                dbu.deleteMetaData(h, "test");
            }

            for (BigHash h : dataChunksKept) {
                assertTrue("Should have data chunk.", dbu.hasData(h));
            }
            for (BigHash h : dataChunksDeleted) {
                assertFalse("Should not have data chunk.", dbu.hasData(h));
            }
            for (BigHash h : metaDataChunksKept) {
                assertTrue("Should have meta data chunk.", dbu.hasMetaData(h));
            }
            for (BigHash h : metaDataChunksDeleted) {
                assertFalse("Should not have meta data chunk.", dbu.hasMetaData(h));
            }

            File dataLog = dbu.getDataDeletionLog();
            File metaDataLog = dbu.getMetaDataDeletionLog();

            // Shut down so can read files
            IOUtil.safeClose(ffts);

            if (!shouldLog) {
                assertFalse("Data log should not exist; not logging.", dataLog.exists());
                assertFalse("Meta data log should not exist; not logging.", metaDataLog.exists());
            } else {
                assertTrue("Data log should exist.", dataLog.exists());
                assertTrue("Data log should exist.", metaDataLog.exists());

                Set<BigHash> foundDeletedDataChunks = new HashSet();

                BufferedReader in = null;
                try {
                    in = new BufferedReader(new FileReader(dataLog));

                    String line = null;
                    while ((line = in.readLine()) != null) {
                        String[] tokens = line.split(",");
                        if (tokens.length == 0) {
                            continue;
                        }

                        // Hash should be last
                        BigHash hash = BigHash.createHashFromString(tokens[tokens.length - 1]);
                        foundDeletedDataChunks.add(hash);
                    }
                } finally {
                    IOUtil.safeClose(in);
                }

                for (BigHash h : dataChunksKept) {
                    assertFalse("Log should not contain any reference to non-deleted chunk.", foundDeletedDataChunks.contains(h));
                }
                for (BigHash h : dataChunksDeleted) {
                    assertTrue("Log should contain reference to deleted chunk.", foundDeletedDataChunks.contains(h));
                }

                Set<BigHash> foundDeletedMetaDataChunks = new HashSet();

                try {
                    in = new BufferedReader(new FileReader(metaDataLog));

                    String line = null;
                    while ((line = in.readLine()) != null) {
                        String[] tokens = line.split(",");
                        if (tokens.length == 0) {
                            continue;
                        }

                        // Hash should be last
                        BigHash hash = BigHash.createHashFromString(tokens[tokens.length - 1]);
                        foundDeletedMetaDataChunks.add(hash);
                    }
                } finally {
                    IOUtil.safeClose(in);
                }

                for (BigHash h : metaDataChunksKept) {
                    assertFalse("Log should not contain any reference to non-deleted chunk.", foundDeletedMetaDataChunks.contains(h));
                }
                for (BigHash h : metaDataChunksDeleted) {
                    assertTrue("Log should contain reference to deleted chunk.", foundDeletedMetaDataChunks.contains(h));
                }
            }

        } finally {
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDelete(dataDir);
        }
    }

    /**
     * <p>Shows that chunk of zero bytes doesn't mess up data block.</p>
     */
    public void testZeroBytesChunk() throws Exception {
        TestUtil.printTitle("DataBlockTest:testZeroBytesChunk()");
        File dir = null;
        FlatFileTrancheServer ffts = null;
        try {
            dir = TempFileUtil.createTemporaryDirectory("testZeroBytesChunk");
            ffts = new FlatFileTrancheServer(dir);

            DataBlockUtil dbu = ffts.getDataBlockUtil();

            byte[] emptyChunk = new byte[0];
            BigHash emptyChunkHash = new BigHash(emptyChunk);

            String startsWith = emptyChunkHash.toString().substring(0, 2);

            Set<BigHash> hashes = new HashSet();

            // Add a few chunks first
            for (int i = 0; i < 15; i++) {
                byte[] dataChunk = DevUtil.createRandomDataChunkStartsWith(startsWith);
                BigHash dataChunkHash = new BigHash(dataChunk);

                if (hashes.contains(dataChunkHash)) {
                    i--;
                    continue;
                }
                dbu.addData(dataChunkHash, dataChunk);
                hashes.add(dataChunkHash);
            }

            // Now add empty chunk
            assertFalse("Better not contain empty chunk already.", dbu.hasData(emptyChunkHash));
            dbu.addData(emptyChunkHash, emptyChunk);
            hashes.add(emptyChunkHash);

            // Verify empty chunk is there and is zero bytes
            assertTrue("Better contain empty chunk.", dbu.hasData(emptyChunkHash));
            byte[] emptyChunkVerify = dbu.getData(emptyChunkHash);
            assertEquals("Chunk better be zero bytes.", 0, emptyChunkVerify.length);

            // Now add a few more chunks
            for (int i = 0; i < 15; i++) {
                byte[] dataChunk = DevUtil.createRandomDataChunkStartsWith(startsWith);
                BigHash dataChunkHash = new BigHash(dataChunk);

                if (hashes.contains(dataChunkHash)) {
                    i--;
                    continue;
                }
                dbu.addData(dataChunkHash, dataChunk);
                hashes.add(dataChunkHash);
            }

            // Verify empty chunk is there and is zero bytes
            assertTrue("Better contain empty chunk.", dbu.hasData(emptyChunkHash));
            emptyChunkVerify = dbu.getData(emptyChunkHash);
            assertEquals("Chunk better be zero bytes.", 0, emptyChunkVerify.length);

            // Make sure everything is there and can get
            for (BigHash h : hashes) {
                assertTrue("Better have data chunk: " + h, dbu.hasData(h));
                byte[] dataChunk = dbu.getData(h);
                BigHash hash = new BigHash(dataChunk);
                assertEquals("Hashes should match for: " + h, h, hash);
            }

            DataBlock db = dbu.getDataBlockToGetChunk(emptyChunkHash, false);
            db.cleanUpDataBlock(false);

            Thread.sleep(1000);

            // Show file system: should be multiple datablocks
            Text.printRecursiveDirectoryStructure(dir);

            // Verify empty chunk is there and is zero bytes
            assertTrue("Better contain empty chunk.", dbu.hasData(emptyChunkHash));
            emptyChunkVerify = dbu.getData(emptyChunkHash);
            assertEquals("Chunk better be zero bytes.", 0, emptyChunkVerify.length);

            // Make sure everything is there and can get
            for (BigHash h : hashes) {
                assertTrue("Better have data chunk: " + h, dbu.hasData(h));
                byte[] dataChunk = dbu.getData(h);
                BigHash hash = new BigHash(dataChunk);
                assertEquals("Hashes should match for: " + h, h, hash);
            }
        } finally {
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDelete(dir);
        }
    }

    /**
     * Tests general read/write of the data block.
     */
    public void testNonSplittingWriteAndRead() throws Exception {
        TestUtil.printTitle("DataBlockTest:testNonSplittingWriteAndRead()");
        File dir = TempFileUtil.createTemporaryDirectory();
        try {
            // make up some random data
            ArrayList<byte[]> randomData = new ArrayList();
            for (int i = 0; i < 10; i++) {
                randomData.add(Utils.makeRandomData((int) (Math.random() * DataBlockUtil.ONE_MB)));
            }
            // make up some random meta-data
            ArrayList<byte[]> randomMetaData = new ArrayList();
            for (int i = 0; i < 10; i++) {
                randomMetaData.add(Utils.makeRandomData((int) (Math.random() * 2024)));
            }
            // add in a random big meta data (>1MB)
            randomMetaData.add(DevUtil.createRandomBigMetaDataChunk());

            // make the directory configuration
            DataDirectoryConfiguration ddc1 = new DataDirectoryConfiguration(dir.getAbsolutePath() + File.separator + "1", Long.MAX_VALUE);
            DataDirectoryConfiguration ddc2 = new DataDirectoryConfiguration(dir.getAbsolutePath() + File.separator + "2", Long.MAX_VALUE);
            DataDirectoryConfiguration ddc3 = new DataDirectoryConfiguration(dir.getAbsolutePath() + File.separator + "3", Long.MAX_VALUE);
            DataBlockUtil dbu = new DataBlockUtil();
            dbu.add(ddc1);
            dbu.add(ddc2);
            dbu.add(ddc3);

            // add some random data
            for (byte[] data : randomData) {
                BigHash hash = new BigHash(data);
                dbu.addData(hash, data);
                assertTrue("Better have data", dbu.hasData(hash));
            }
            // add some random meta-data
            for (byte[] data : randomMetaData) {
                BigHash hash = new BigHash(data);
                dbu.addMetaData(hash, data);
                assertTrue("Better have meta data", dbu.hasMetaData(hash));
            }
            // read it back out
            for (byte[] data : randomData) {
                BigHash hash = new BigHash(data);
                byte[] readData = dbu.getData(hash);
                // assert that they are the same
                BigHash check = new BigHash(readData);
                assertEquals("Expected data to be of same size.", data.length, readData.length);
                assertEquals(hash.toString(), check.toString());
            }
            // read the meta-data back out
            for (byte[] data : randomMetaData) {
                BigHash hash = new BigHash(data);
                byte[] readData = dbu.getMetaData(hash);
                // assert that they are the same
                BigHash check = new BigHash(readData);
                assertEquals("Expected data to be of same size.", data.length, readData.length);
                assertEquals(hash.toString(), check.toString());
            }

            // now test on a fresh instance -- i.e. rebooted server
            {
                DataBlockUtil dbu2 = new DataBlockUtil();
                dbu2.add(ddc2);
                dbu2.add(ddc1);
                dbu2.add(ddc3);
                // read it back out
                for (byte[] data : randomData) {
                    BigHash hash = new BigHash(data);
                    byte[] readData = dbu2.getData(hash);
                    // assert that they are the same
                    BigHash check = new BigHash(readData);
                    assertEquals("Expected data to be of same size.", data.length, readData.length);
                    assertEquals(hash.toString(), check.toString());
                }
                // read the meta-data back out
                for (byte[] data : randomMetaData) {
                    BigHash hash = new BigHash(data);
                    byte[] readData = dbu2.getMetaData(hash);
                    // assert that they are the same
                    BigHash check = new BigHash(readData);
                    assertEquals("Expected data to be of same size.", data.length, readData.length);
                    assertEquals(hash.toString(), check.toString());
                }
            }

            // now test on a fresh broken instance of the b-tree -- i.e. rebooted server with some sort of incomplete file transfer
            {
                // purposely duplicate one of the blocks in the tree
                File dirToDup = ddc1.getDirectoryFile();
                for (File temp : dirToDup.listFiles()) {
                    if (temp.isFile()) {
                        // copy the file
                        byte[] data = IOUtil.getBytes(temp);
                        // write the same block to a different part of the tree
                        FileOutputStream fos = new FileOutputStream(new File(ddc2.getDirectory(), temp.getName()));
                        fos.write(data);
                        fos.flush();
                        fos.close();
                        // stop, just one file
                        break;
                    }
                }

                // make a new data block util.
                DataBlockUtil dbu2 = new DataBlockUtil();
                dbu2.add(ddc2);
                dbu2.add(ddc1);
                dbu2.add(ddc3);
                // read it back out
                for (byte[] data : randomData) {
                    BigHash hash = new BigHash(data);
                    byte[] readData = dbu2.getData(hash);
                    // assert that they are the same
                    BigHash check = new BigHash(readData);
                    assertEquals("Expected data to be of same size.", data.length, readData.length);
                    assertEquals(hash.toString(), check.toString());
                }
                // read the meta-data back out
                for (byte[] data : randomMetaData) {
                    BigHash hash = new BigHash(data);
                    byte[] readData = dbu2.getMetaData(hash);
                    // assert that they are the same
                    BigHash check = new BigHash(readData);
                    assertEquals("Expected data to be of same size.", data.length, readData.length);
                    assertEquals(hash.toString(), check.toString());
                }
            }

        } finally {
            IOUtil.recursiveDelete(dir);
        }
    }

    /**
     * Tests that a same named data/meta-data collision will properly keep both entries. It would cause problems if either were deleted.
     */
    public void testSameHasMetaDataAndData() throws Exception {
        TestUtil.printTitle("DataBlockTest:testSameHasMetaDataAndData()");
        File dir = TempFileUtil.createTemporaryDirectory();
        try {
            // make up some random data
            byte[] data = Utils.makeRandomData((int) (Math.random() * DataBlockUtil.ONE_MB));
            byte[] metaData = Utils.makeRandomData((int) (Math.random() * 2024));

            // make the directory configuration
            DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(dir.getAbsolutePath(), Long.MAX_VALUE);
            DataBlockUtil dbu = new DataBlockUtil();
            dbu.add(ddc);

            BigHash sameHash = new BigHash(data);
            // add to the block with the same hash
            dbu.addData(sameHash, data);
            dbu.addMetaData(sameHash, metaData);

            // check the data
            byte[] dataCheck = dbu.getData(sameHash);
            BigHash dataCheckHash = new BigHash(dataCheck);
            assertEquals("Expected same size.", data.length, dataCheck.length);
            assertEquals("Expected same hash.", sameHash.toString(), dataCheckHash.toString());

            // check the meta-data
            byte[] metaDataCheck = dbu.getMetaData(sameHash);
            BigHash metaDataHash = new BigHash(metaData);
            BigHash metaDataCheckHash = new BigHash(metaDataCheck);
            assertEquals("Expected same size.", metaData.length, metaDataCheck.length);
            assertEquals("Expected same hash.", metaDataHash.toString(), metaDataCheckHash.toString());
        } finally {
            IOUtil.recursiveDelete(dir);
        }
    }

    /**
     * Test that the delete functionality works and files are still accessible. Also tests the clean up code that saves space after lots of deletes occur.
     */
    public void testDelete() throws Exception {
        TestUtil.printTitle("DataBlockTest:testDelete()");
        File dir = TempFileUtil.createTemporaryDirectory();
        try {
            // make up some random data
            ArrayList<byte[]> randomData = new ArrayList();
            ArrayList<byte[]> randomDataToDelete = new ArrayList();
            for (int i = 0; i < 10; i++) {
                randomData.add(Utils.makeRandomData((int) (Math.random() * DataBlockUtil.ONE_MB)));
                randomDataToDelete.add(Utils.makeRandomData((int) (Math.random() * DataBlockUtil.ONE_MB)));
            }

            // make the directory configuration
            DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(dir.getAbsolutePath(), Long.MAX_VALUE);
            DataBlockUtil dbu = new DataBlockUtil();
            dbu.add(ddc);

            // add the data
            for (int i = 0; i < randomData.size(); i++) {
                BigHash hash = new BigHash(randomData.get(i));
                dbu.addData(hash, randomData.get(i));
            }
            for (int i = 0; i < randomDataToDelete.size(); i++) {
                BigHash hash = new BigHash(randomDataToDelete.get(i));
                dbu.addData(hash, randomDataToDelete.get(i));
            }

            // check all normal data
            for (byte[] data : randomData) {
                BigHash hash = new BigHash(data);
                DataBlock block = dbu.getDataBlockToAddChunk(hash);
                byte[] readData = block.getBytes(hash, false);
                // assert that they are the same
                BigHash check = new BigHash(readData);
                assertEquals("Expected data to be of same size.", data.length, readData.length);
                assertEquals(hash.toString(), check.toString());
            }

            // check all data to delete
            for (byte[] data : randomDataToDelete) {
                BigHash hash = new BigHash(data);
                DataBlock block = dbu.getDataBlockToAddChunk(hash);
                byte[] readData = block.getBytes(hash, false);
                // assert that they are the same
                BigHash check = new BigHash(readData);
                assertEquals("Expected data to be of same size.", data.length, readData.length);
                assertEquals(hash.toString(), check.toString());
            }

            // delete all of the items to delete and check that they are deleted
            for (byte[] data : randomDataToDelete) {
                BigHash hash = new BigHash(data);
                DataBlock block = dbu.getDataBlockToAddChunk(hash);
                block.deleteBytes(hash, false);
                // now try to read the bytes
                try {
                    byte[] readData = block.getBytes(hash, false);
                    fail("Expected bytes not to be found.");
                } catch (Exception e) {
                    // noop -- supposed to throw
                }
            }

            //////////////////////////////////////////////////////
            // this last chunk of code tests that the clean up functionality works as expected
            //////////////////////////////////////////////////////

            // count up the size of the old files
            long preCleanupSize = 0;
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    fail("Expected no data block splits!");
                }
                preCleanupSize += file.length();
            }

            // clean up the data blocks with deleted data
            for (byte[] data : randomDataToDelete) {
                BigHash hash = new BigHash(data);
                DataBlock block = dbu.getDataBlockToAddChunk(hash);
                block.cleanUpDataBlock(true);
            }
            // check that deleted files don't exist (still)
            for (byte[] data : randomDataToDelete) {
                BigHash hash = new BigHash(data);
                DataBlock block = dbu.getDataBlockToAddChunk(hash);
                // now try to read the bytes
                try {
                    byte[] readData = block.getBytes(hash, false);
                    fail("Expected bytes not to be found.");
                } catch (Exception e) {
                    // noop -- supposed to throw
                }
            }
            // check that all of the non-deleted data is still present
            for (byte[] data : randomData) {
                BigHash hash = new BigHash(data);
                DataBlock block = dbu.getDataBlockToAddChunk(hash);
                byte[] readData = block.getBytes(hash, false);
                // assert that they are the same
                BigHash check = new BigHash(readData);
                assertEquals("Expected data to be of same size.", data.length, readData.length);
                assertEquals(hash.toString(), check.toString());
            }

            // count the file size post cleanup
            long postCleanupSize = 0;
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    fail("Expected no data block splits!");
                }
                postCleanupSize += file.length();
            }
            // assert the space has been saved
            assertTrue("Expected cleanup to save space.", postCleanupSize < preCleanupSize);


        } finally {
            IOUtil.recursiveDelete(dir);
        }
    }

    /**
     * Tests the code that handles file splitting.
     */
    public void testSplitAndAutoFix() throws Exception {
        TestUtil.printTitle("DataBlockTest:testSplitAndAutoFix()");
        File dir = TempFileUtil.createTemporaryDirectory();
        DataBlockUtilLog logger = null;
        try {

            // make the directory configuration
            DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(dir.getAbsolutePath(), Long.MAX_VALUE);
            DataBlockUtil dbu = new DataBlockUtil();
            dbu.add(ddc);

            // Turn logging on. Free test for log functionality on split
            DataBlockUtil.setIsLogging(true);
            logger = DataBlockUtil.getLogger();

            // keep track of fake hash names and real hashes
            ArrayList<BigHash> contrivedHashes = new ArrayList();
            ArrayList<BigHash> realHashes = new ArrayList();

            int bytesWritten = 0;
            int maxFileSize = 100 * 1024;
            while (bytesWritten <= DataBlock.MAX_BLOCK_SIZE * 2) {
                // make some random data
                byte[] randomData = Utils.makeRandomData((int) (Math.random() * maxFileSize));
                // check that the hash starts with 'aa'
                BigHash hash = new BigHash(randomData);
                // skip if not right hash -- takes a while!
                // manually set the hash to have 0x01 for teh first two bytes -- seems to work well
                byte[] hashBytes = hash.toByteArray();
                byte[] copy = new byte[hashBytes.length];
                System.arraycopy(hashBytes, 0, copy, 0, copy.length);
                // tweak the hash to go in to the same block
                copy[0] = 0;
                copy[1] = 1;
                BigHash fakeHash = BigHash.createFromBytes(copy);
                // add to the block
                dbu.addData(fakeHash, randomData);
                bytesWritten += randomData.length;

                // add to the lists
                contrivedHashes.add(fakeHash);
                realHashes.add(hash);

//                System.out.println(bytesWritten +" out of "+DataBlock.MAX_BLOCK_SIZE);
            }

            // expect to find one file that is a directory
            String[] checkForDir = dir.list();
            assertTrue(2 == checkForDir.length || 1 == checkForDir.length);
            // at least one directory
            boolean atLeastOneDirectory = false;
            for (File f : dir.listFiles()) {
                if (f.isDirectory()) {
                    atLeastOneDirectory = true;
                }
            }
            assertTrue(atLeastOneDirectory);

            // check for more than one split file and that all are files
            {
                int fileCount = 0;
                ArrayList<File> filesToCount = new ArrayList();
                filesToCount.add(dir);
                while (!filesToCount.isEmpty()) {
                    File fileToCount = filesToCount.remove(0);
                    if (fileToCount.isDirectory()) {
                        File[] files = fileToCount.listFiles();
                        for (File file : files) {
                            filesToCount.add(file);
                        }
                    } // tally normal files
                    else {
                        fileCount++;
                    }
                }
                assertTrue("Expected more than one split block files.", 1 < fileCount);
            }

            // check that all of the data is in the tree
            for (int i = 0; i < contrivedHashes.size(); i++) {
                // get the bytes
                byte[] data = dbu.getData(contrivedHashes.get(i));
                // check the hash
                BigHash check = new BigHash(data);
                // hashes should be the same
                assertTrue("Expected the same hash.", check.equals(realHashes.get(i)));
            }


            // purposely move one of the blocks
            {
                DataBlock block = null;
                for (int i = 0; i < contrivedHashes.size(); i++) {
                    block = dbu.getDataBlockToAddChunk(contrivedHashes.get(i));
                    if (block.getHashes(false).size() > 0) {
                        break;
                    }
                }
                dbu.purposelyFailMerge = 0;
                // force a clean up
                try {
                    block.cleanUpDataBlock(true);
                    fail("Expected a failure!");
                } catch (Exception e) {
                    // we're ok, expected exception
                }
                dbu.purposelyFailMerge = Integer.MAX_VALUE;
            }

            // check that at least one file is now missing.
            ArrayList<BigHash> missingData = new ArrayList();
            for (int i = 0; i < contrivedHashes.size(); i++) {
                // get the bytes
                try {
                    dbu.getData(contrivedHashes.get(i));
                } catch (FileNotFoundException e) {
                    missingData.add(contrivedHashes.get(i));
                }
            }
            assertTrue("Expected at least one hash to be missing from the tree.", missingData.size() > 0);

            // find the toadd.xxxx file
            File toAddFile = null;
            ArrayList<File> filesToCount = new ArrayList();
            filesToCount.add(dir);
            while (!filesToCount.isEmpty()) {
                File fileToCount = filesToCount.remove(0);
                if (fileToCount.isDirectory()) {
                    File[] files = fileToCount.listFiles();
                    for (File file : files) {
                        filesToCount.add(file);
                    }
                } // tally normal files
                else {
                    // check for non-standard files
                    if (fileToCount.getName().endsWith(".backup")) {
                        if (toAddFile != null) {
                            fail("More than one 'toadd' file was found!?");
                        }
                        toAddFile = fileToCount;
                    }
                }
            }
            // assert that a 'toadd' file was found
            assertNotNull(toAddFile);


            // now fix the tree and get on with things
            dbu.mergeOldDataBlock(toAddFile);

            // check that all of the data is in the tree
            for (int i = 0; i < contrivedHashes.size(); i++) {
                // get the bytes
                byte[] data = dbu.getData(contrivedHashes.get(i));
                // check the hash
                BigHash check = new BigHash(data);
                // hashes should be the same
                assertTrue("Expected the same hash.", check.equals(realHashes.get(i)));
            }

            // Check logger information
            assertTrue("Should be some run time.", logger.getRuntime() > 0);
            assertTrue("Should be some time spent merging.", logger.getTimeSpentMerging() > 0);
            assertTrue("Time spent merging should be less than total log time.", logger.getRuntime() > logger.getTimeSpentMerging());
        } finally {
            IOUtil.recursiveDelete(dir);
            // Shouldn't matter
            if (logger != null) {
                logger.close();
            }
            DataBlockUtil.setIsLogging(false);
        }
    }

    /**
     * Makes a big b-tree across several directories and tests that the merging functionality works.
     */
    public void testAutoFixWithMultipleDirectories() throws Exception {
        TestUtil.printTitle("DataBlockTest:testAutoFixWithMultipleDirectories()");
        File dir = TempFileUtil.createTemporaryDirectory();
        try {
            // make several directories
            File[] dataDirectories = new File[4];
            for (int i = 0; i < dataDirectories.length; i++) {
                dataDirectories[i] = new File(dir, Integer.toString(i));
            }

            // make data directory configs
            DataDirectoryConfiguration[] configs = new DataDirectoryConfiguration[dataDirectories.length];
            for (int i = 0; i < configs.length; i++) {
                configs[i] = new DataDirectoryConfiguration(dataDirectories[i].getAbsolutePath(), Long.MAX_VALUE);
            }

            // make the directory configuration
            DataBlockUtil dbu = new DataBlockUtil();
            for (int i = 0; i < configs.length; i++) {
                dbu.add(configs[i]);
            }

            // keep track of fake hash names and real hashes
            ArrayList<BigHash> contrivedHashes = new ArrayList();
            ArrayList<BigHash> realHashes = new ArrayList();

            // ensure four splits
            long totalEntries = DataBlock.HEADERS_PER_FILE * 10;
            for (int i = 0; i < totalEntries; i++) {
                // make some random data
                byte[] randomData = Utils.makeRandomData((int) (Math.random() * 10 * 1024));
                // check that the hash starts with 'aa'
                BigHash hash = new BigHash(randomData);
                // skip if not right hash -- takes a while!
                // manually set the hash to have 0x01 for teh first two bytes -- seems to work well
                byte[] hashBytes = hash.toByteArray();
                byte[] copy = new byte[hashBytes.length];
                System.arraycopy(hashBytes, 0, copy, 0, copy.length);
                // tweak the hash to go in to the same block
                copy[0] = 0;
                copy[1] = (byte) (i % 2);
                BigHash fakeHash = BigHash.createFromBytes(copy);
                // add to the block
                dbu.addData(fakeHash, randomData);

                // add to the lists
                contrivedHashes.add(fakeHash);
                realHashes.add(hash);
            }

            // check for more than one split file and that all are files
            {
                int fileCount = 0;
                ArrayList<File> filesToCount = new ArrayList();
                for (int i = 0; i < dataDirectories.length; i++) {
                    filesToCount.add(dataDirectories[i]);
                }
                while (!filesToCount.isEmpty()) {
                    File fileToCount = filesToCount.remove(0);
                    if (fileToCount.isDirectory()) {
                        File[] files = fileToCount.listFiles();
                        for (File file : files) {
                            filesToCount.add(file);
                        }
                    } // tally normal files
                    else {
                        fileCount++;
                    }
                }
                assertTrue("Expected more than one split block files.", 1 < fileCount);
            }

            // check that all of the data is in the tree
            for (int i = 0; i < contrivedHashes.size(); i++) {
                // get the bytes
                byte[] data = dbu.getData(contrivedHashes.get(i));
                // check the hash
                BigHash check = new BigHash(data);
                // hashes should be the same
                assertTrue("Expected the same hash.", check.equals(realHashes.get(i)));
            }


            // create a new DataBlock
            DataBlockUtil dbu2 = new DataBlockUtil();
            for (int i = 0; i < configs.length; i++) {
                dbu2.add(configs[i]);
            }

            // check that all of the data is in the tree
            for (int i = 0; i < contrivedHashes.size(); i++) {
                // get the bytes
                byte[] data = dbu2.getData(contrivedHashes.get(i));
                // check the hash
                BigHash check = new BigHash(data);
                // hashes should be the same
                assertTrue("Expected the same hash.", check.equals(realHashes.get(i)));
            }
        } finally {
            IOUtil.recursiveDelete(dir);
        }
    }

    @Todo(desc = "Why duplicate test?", day = 0, month = 0, year = 0, author = "Unknown")
    /**
     * Makes a big b-tree across several directories and tests that the merging functionality works.
     */
    public void testAutoFixWithMultipleDirectories2() throws Exception {
        TestUtil.printTitle("DataBlockTest:testAutoFixWithMultipleDirectories2()");
        File dir = TempFileUtil.createTemporaryDirectory();

        // use a predictable random
        Random random = new Random(1);

        try {
            // make several directories
            File[] dataDirectories = new File[4];
            for (int i = 0; i < dataDirectories.length; i++) {
                dataDirectories[i] = new File(dir, Integer.toString(i));
            }

            // make a tranche server
            FlatFileTrancheServer ffts = new FlatFileTrancheServer(dir);
            Configuration config = ffts.getConfiguration();
            Set<DataDirectoryConfiguration> ddcs = config.getDataDirectories();
            // make data directory configs
            for (int i = 0; i < dataDirectories.length; i++) {
                ddcs.add(new DataDirectoryConfiguration(dataDirectories[i].getAbsolutePath(), Long.MAX_VALUE));
            }
            // add the dev user as a trusted
            UserZipFile uzf = DevUtil.createUser("foo", "bar", new File(dir, "user.temp").getAbsolutePath(), true, false);
            config.addUser(uzf);

            // keep track of fake hash names and real hashes
            ArrayList<BigHash> contrivedHashes = new ArrayList();
            ArrayList<BigHash> realHashes = new ArrayList();

            // ensure four splits
            long totalEntries = DataBlock.HEADERS_PER_FILE * 10;
            for (int i = 0; i < totalEntries; i++) {
                // make some random data
//                byte[] randomData = Utils.makeRandomData((int)(Math.random()*10*1024));
                byte[] randomData = new byte[(int) (Math.random() * 10 * 1024)];
                random.nextBytes(randomData);
                // check that the hash starts with 'aa'
                BigHash hash = new BigHash(randomData);
                // skip if not right hash -- takes a while!
                // manually set the hash to have 0x01 for teh first two bytes -- seems to work well
                byte[] hashBytes = hash.toByteArray();
                byte[] copy = new byte[hashBytes.length];
                System.arraycopy(hashBytes, 0, copy, 0, copy.length);
                // tweak the hash to go in to the same block
                copy[0] = 0;
                copy[1] = (byte) (i % 2);
                BigHash fakeHash = BigHash.createFromBytes(copy);
                // add to the block
                IOUtil.setData(ffts, uzf.getCertificate(), uzf.getPrivateKey(), fakeHash, randomData);

                // add to the lists
                contrivedHashes.add(fakeHash);
                realHashes.add(hash);
            }

            // check for more than one split file and that all are files
            {
                System.out.println("B-tree structure.");
                int fileCount = 0;
                ArrayList<File> filesToCount = new ArrayList();
                for (int i = 0; i < dataDirectories.length; i++) {
                    filesToCount.add(dataDirectories[i]);
                }
                while (!filesToCount.isEmpty()) {
                    File fileToCount = filesToCount.remove(0);
                    if (fileToCount.isDirectory()) {
                        File[] files = fileToCount.listFiles();
                        for (File file : files) {
                            filesToCount.add(file);
                        }
                    } // tally normal files
                    else {
                        fileCount++;
                    }
                }
                assertTrue("Expected more than one split block files.", 1 < fileCount);
            }

            // check that all of the data is in the tree
            for (int i = 0; i < contrivedHashes.size(); i++) {
                // get the bytes
                Object o = IOUtil.getData(ffts, contrivedHashes.get(i), false).getReturnValueObject();
                
                byte[] data = null;
                
                if (o instanceof byte[]) {
                    data = (byte[])o;
                } else if (o instanceof byte[][]) {
                    data = ((byte[][])o)[0];
                } else {
                    fail("Expected return object to be type byte[] or byte[][], but wasn't.");
                }
                // check the hash
                BigHash check = new BigHash(data);
                // hashes should be the same
                assertTrue("Expected the same hash.", check.equals(realHashes.get(i)));
            }

            // close the server
            ffts.close();


            // create a new server
            FlatFileTrancheServer ffts2 = new FlatFileTrancheServer(dir);
            // wait for the project finding thread
            ffts2.waitToLoadExistingDataBlocks();

            // check that all of the data is in the tree
            for (int i = 0; i < contrivedHashes.size(); i++) {
                // get the bytes
                byte[] data = (byte[]) IOUtil.getData(ffts2, contrivedHashes.get(i), false).getReturnValueObject();
                // check the hash
                BigHash check = new BigHash(data);
                // hashes should be the same
                assertTrue("Expected the same hash.", check.equals(realHashes.get(i)));
            }
            ffts2.close();

            // create a new server
            FlatFileTrancheServer ffts3 = new FlatFileTrancheServer(dir);
            // don't wait for the project finding thread

            // check that all of the data is in the tree
            for (int i = 0; i < contrivedHashes.size(); i++) {
                try {
                    // get the bytes
                    byte[] data = (byte[]) IOUtil.getData(ffts3, contrivedHashes.get(i), false).getReturnValueObject();
                    // check the hash
                    BigHash check = new BigHash(data);
                    // hashes should be the same
                    assertTrue("Expected the same hash.", check.equals(realHashes.get(i)));
                } catch (FileNotFoundException e) {
                    // get the bytes
                    byte[] data = (byte[]) IOUtil.getData(ffts3, contrivedHashes.get(i), false).getReturnValueObject();
                }
            }

        } finally {
            IOUtil.recursiveDelete(dir);
        }
    }

    /**
     * <p>Want to test that appropriately has/gets data and meta data. Also negatively test.</p>
     */
    public void testHasAndGetDataAndMetaData() throws Exception {
        TestUtil.printTitle("DataBlockTest:testHasAndGetDataAndMetaData()");
        Set<BigHash> dataHashes = new HashSet<BigHash>(),
                metaHashes = new HashSet<BigHash>();

        FlatFileTrancheServer ffserver = null;

        UserZipFile uzf = null;

        File topDir = null,
                oneDir = null,
                twoDir = null,
                threeDir = null;
        DataBlockUtil dbu = null;

        try {

            // Set up small directories
            topDir = TempFileUtil.createTemporaryDirectory("testHasAndGetDataAndMetaData");
            oneDir = new File(topDir, "one");
            oneDir.mkdirs();

            twoDir = new File(topDir, "two");
            twoDir.mkdirs();

            threeDir = new File(topDir, "three");
            threeDir.mkdirs();

            assertTrue("Directory should exist: one", oneDir.exists());
            assertTrue("Directory should exist: two", twoDir.exists());
            assertTrue("Directory should exist: three", threeDir.exists());

            // Create users
            uzf = DevUtil.createUser("foo", "bar", TempFileUtil.createTemporaryFile(".zip.encrypted").getAbsolutePath(), true, false);

            // Create server and grab its config
            ffserver = new FlatFileTrancheServer(topDir);
            Configuration config = ffserver.getConfiguration();

            // Add auhorized user
            config.addUser(uzf);

            // Add new data directory configs so has multiple dirs
            Set<DataDirectoryConfiguration> ddcs = config.getDataDirectories();
            ddcs.add(new DataDirectoryConfiguration(oneDir.getAbsolutePath(), Long.MAX_VALUE));
            ddcs.add(new DataDirectoryConfiguration(twoDir.getAbsolutePath(), Long.MAX_VALUE));
            ddcs.add(new DataDirectoryConfiguration(threeDir.getAbsolutePath(), Long.MAX_VALUE));

            assertEquals("Expecting 4 ddcs: one default + 3 added", 4, config.getDataDirectories().size());

            dbu = ffserver.getDataBlockUtil();

            assertNotNull("Should return a DataBlockUtil without problems, but null!", dbu);

            int size = RandomUtil.getInt(1024 * 512) + 1024;

            // Create one hundred meta
            for (int i = 0; i < 100; i++) {

                byte[] meta = new byte[size];
                RandomUtil.getBytes(meta);

                BigHash h = new BigHash(meta);

                // If already created this chunk, what a coincidence!
                if (metaHashes.contains(h)) {
                    i--;
                    continue;
                }
                metaHashes.add(h);

                assertFalse("Should not have meta data!", dbu.hasMetaData(h));
                dbu.addMetaData(h, meta);
                assertTrue("Should have meta data!", dbu.hasMetaData(h));

                byte[] getMeta = dbu.getMetaData(h);
                assertNotNull("Should not have trouble retrieving meta data.", getMeta);
                assertEquals("Meta data should be the same length.", meta.length, getMeta.length);

                for (int j = 0; j < meta.length; j++) {
                    assertEquals("Meta data should be the same.", meta[j], getMeta[j]);
                }
            }

            // Create one hundred data
            for (int i = 0; i < 100; i++) {

                byte[] data = new byte[size];
                RandomUtil.getBytes(data);

                BigHash h = new BigHash(data);

                // If already created this chunk, what a coincidence!
                if (dataHashes.contains(h)) {
                    i--;
                    continue;
                }
                dataHashes.add(h);

                assertFalse("Should not have data!", dbu.hasData(h));
                dbu.addData(h, data);
                assertTrue("Should have data!", dbu.hasData(h));
                assertNotNull("Should not have trouble retrieving data!", dbu.getData(h));
            }

            // Make sure still there!
            for (BigHash h : metaHashes) {
                assertTrue("Should have meta data!", dbu.hasMetaData(h));
                assertNotNull("Should not have trouble retrieving meta data!", dbu.getMetaData(h));
            }
            for (BigHash h : dataHashes) {
                assertTrue("Should have data!", dbu.hasData(h));
                assertNotNull("Should not have trouble retrieving data!", dbu.getData(h));
            }

        } finally {
            IOUtil.safeClose(ffserver);
            IOUtil.safeDelete(uzf.getFile());
            IOUtil.recursiveDelete(topDir);
            dbu.close();
        }
    }

    public void testBadChunksEmpty1MBSmallBatch() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksEmpty1MBSmallBatch()");
        testDataBlockHandlesBadChunks(50, 50, true, true, false);
    }

    public void testBadChunksRandom1MBSmallBatch() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksRandom1MBSmallBatch()");
        testDataBlockHandlesBadChunks(50, 50, true, false, false);
    }

    public void testBadChunksEmptySmallBatch() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksEmptySmallBatch()");
        testDataBlockHandlesBadChunks(50, 50, false, true, false);
    }

    public void testBadChunksRandomSmallBatch() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksRandomSmallBatch()");
        testDataBlockHandlesBadChunks(50, 50, false, false, false);
    }

    public void testBadChunksEmpty1MBSmallBatchWithFalseSizes() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksEmpty1MBSmallBatchWithFalseSizes()");
        testDataBlockHandlesBadChunks(50, 50, true, true, true);
    }

    public void testBadChunksRandom1MBSmallBatchWithFalseSizes() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksRandom1MBSmallBatchWithFalseSizes()");
        testDataBlockHandlesBadChunks(50, 50, true, false, true);
    }

    public void testBadChunksEmptySmallBatchWithFalseSizes() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksEmptySmallBatchWithFalseSizes()");
        testDataBlockHandlesBadChunks(50, 50, false, true, true);
    }

    public void testBadChunksRandomSmallBatchWithFalseSizes() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksRandomSmallBatchWithFalseSizes()");
        testDataBlockHandlesBadChunks(50, 50, false, false, true);
    }

    // these scenarios are really long time - comment out for standard testing
    public void testBadChunksEmpty1MBMediumBatch() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksEmpty1MBMediumBatch()");
        testDataBlockHandlesBadChunks(100, 100, true, true, false);
    }

    public void testBadChunksRandom1MBMediumBatch() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksRandom1MBMediumBatch()");
        testDataBlockHandlesBadChunks(100, 100, true, false, false);
    }

    public void testBadChunksEmptyMediumBatch() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksEmptyMediumBatch()");
        testDataBlockHandlesBadChunks(100, 100, false, true, false);
    }

    public void testBadChunksRandomMediumBatch() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksRandomMediumBatch()");
        testDataBlockHandlesBadChunks(100, 100, false, false, false);
    }

    public void testBadChunksEmpty1MBLargeBatch() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksEmpty1MBLargeBatch()");
        testDataBlockHandlesBadChunks(500, 500, true, true, false);
    }

    public void testBadChunksEmpty1MBLargeBatchWithFalseSizes() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBadChunksEmpty1MBLargeBatchWithFalseSizes()");
        testDataBlockHandlesBadChunks(500, 500, true, true, true);
    }

    /**
     * <p>This helper method allows many permutations. Pay attention to the parameters.</p>
     * <p>This test has three DataBlockUtil's. One will have only good meta and data chunks. The others will have some good and some bad.</p>
     * <p>Bad chunks mean that they don't match their hash if data or are not valid meta data chunks. There are two type of bad chunks. See isUseEmptyBytes.</p>
     * @param numDataChunks Number of data chunks to test.
     * @param numMetaChunks Number of meta chunks to test.
     * @param useExactly1MBChunks If true, every test chunk is exactly 1MB. Other, randomly between 1 byte and 1MB, inclusive.
     * @param isUseEmptyBytes If true, the bad chunk will be an initialized but untouched byte array (should be all zeros). Others might be random bytes. This will help simulate different IO error possibilities.
     * @param isTestFalseSizes will change the size of the bad chunk before submitting.
     */
    private void testDataBlockHandlesBadChunks(int numDataChunks, int numMetaChunks, boolean useExactly1MBChunks, boolean isUseEmptyBytes, boolean isTestFalseSizes) throws Exception {
        File dataDir1 = null, dataDir2 = null, dataDir3 = null;

        // Each DataBlockUtil gets one DataDirectoryConfiguration with one
        // data directory. So simple.
        DataBlockUtil dbu1 = new DataBlockUtil();
        DataBlockUtil dbu2 = new DataBlockUtil();
        DataBlockUtil dbu3 = new DataBlockUtil();

        try {
            dataDir1 = TempFileUtil.createTemporaryDirectory();
            dataDir2 = TempFileUtil.createTemporaryDirectory();
            dataDir3 = TempFileUtil.createTemporaryDirectory();

            assertTrue("Directory should exist.", dataDir1.exists());
            assertTrue("Directory should exist.", dataDir2.exists());
            assertTrue("Directory should exist.", dataDir3.exists());

            assertFalse("Should be different directories.", dataDir1.getAbsolutePath().equals(dataDir2.getAbsolutePath()));
            assertFalse("Should be different directories.", dataDir2.getAbsolutePath().equals(dataDir3.getAbsolutePath()));
            assertFalse("Should be different directories.", dataDir3.getAbsolutePath().equals(dataDir1.getAbsolutePath()));

            DataDirectoryConfiguration ddc1 = new DataDirectoryConfiguration(dataDir1.getAbsolutePath(), Long.MAX_VALUE);
            DataDirectoryConfiguration ddc2 = new DataDirectoryConfiguration(dataDir2.getAbsolutePath(), Long.MAX_VALUE);
            DataDirectoryConfiguration ddc3 = new DataDirectoryConfiguration(dataDir3.getAbsolutePath(), Long.MAX_VALUE);

            dbu1.add(ddc1);
            dbu2.add(ddc2);
            dbu3.add(ddc3);

            List<BigHash> dataChunks = new ArrayList<BigHash>();
            List<BigHash> metaChunks = new ArrayList<BigHash>();

            /**
             * <p>Add the data chunks. See method JavaDoc for description.</p>
             */
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

                // Add good data chunk to dbu1
                dbu1.addData(hash, goodChunk);

                // Flip a coin
                if (RandomUtil.getBoolean()) {
                    dbu2.addData(hash, goodChunk);
                    dbu3.addData(hash, badChunk);
                } else {
                    dbu2.addData(hash, badChunk);
                    dbu3.addData(hash, goodChunk);
                }
            }

            /**
             * <p>Add the meta chunks. See method JavaDoc for description.</p>
             */
            for (int i = 0; i < numMetaChunks; i++) {
                byte[] goodChunk = null, badChunk = null;
                if (useExactly1MBChunks) {
                    goodChunk = DevUtil.createRandomMetaDataChunk();
                    badChunk = new byte[1024 * 1024];
                } else {
                    // Create the good chunk
                    goodChunk = DevUtil.createRandomMetaDataChunk();
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

                metaChunks.add(hash);

                // Add good data chunk to dbu1
                dbu1.addMetaData(hash, goodChunk);

                // Flip a coin
                if (RandomUtil.getBoolean()) {
                    dbu2.addMetaData(hash, goodChunk);
                    dbu3.addMetaData(hash, badChunk);
                } else {
                    dbu2.addMetaData(hash, badChunk);
                    dbu3.addMetaData(hash, goodChunk);
                }
            }

            /**
             * How many data chunks do each DataBlockUtil report?
             */
            long numDataChunksReported1 = dbu1.dataHashes.size();
            long numMetaChunksReported1 = dbu1.metaDataHashes.size();

            long numDataChunksReported2 = dbu2.dataHashes.size();
            long numMetaChunksReported2 = dbu2.metaDataHashes.size();

            long numDataChunksReported3 = dbu3.dataHashes.size();
            long numMetaChunksReported3 = dbu3.metaDataHashes.size();

            System.out.println("DBU #1 reports " + numDataChunksReported1 + " data chunks, " + numMetaChunksReported1 + " meta data chunks.");
            System.out.println("DBU #2 reports " + numDataChunksReported2 + " data chunks, " + numMetaChunksReported2 + " meta data chunks.");
            System.out.println("DBU #3 reports " + numDataChunksReported3 + " data chunks, " + numMetaChunksReported3 + " meta data chunks.");

            /**
             * Do an actual count of above
             */
            long numDataChunksCounted1 = 0;
            long numMetaChunksCounted1 = 0;

            long numDataChunksVerified1 = 0;
            long numMetaChunksVerified1 = 0;

            long numDataChunksCounted2 = 0;
            long numMetaChunksCounted2 = 0;

            long numDataChunksVerified2 = 0;
            long numMetaChunksVerified2 = 0;

            long numDataChunksCounted3 = 0;
            long numMetaChunksCounted3 = 0;

            long numDataChunksVerified3 = 0;
            long numMetaChunksVerified3 = 0;

            for (BigHash h : dataChunks) {
                if (dbu1.hasData(h)) {
                    numDataChunksCounted1++;

                    byte[] data = dbu1.getData(h);

                    try {
                        BigHash verifyHash = new BigHash(data);
                        if (verifyHash.equals(h)) {
                            numDataChunksVerified1++;
                        }
                    } catch (Exception ex) { /* Nope */ }
                }
                if (dbu2.hasData(h)) {
                    numDataChunksCounted2++;

                    byte[] data = dbu2.getData(h);

                    try {
                        BigHash verifyHash = new BigHash(data);
                        if (verifyHash.equals(h)) {
                            numDataChunksVerified2++;
                        }
                    } catch (Exception ex) { /* Nope */ }
                }
                if (dbu3.hasData(h)) {
                    numDataChunksCounted3++;

                    byte[] data = dbu3.getData(h);

                    try {
                        BigHash verifyHash = new BigHash(data);
                        if (verifyHash.equals(h)) {
                            numDataChunksVerified3++;
                        }
                    } catch (Exception ex) { /* Nope */ }
                }
            }

            for (BigHash h : metaChunks) {
                if (dbu1.hasMetaData(h)) {
                    numMetaChunksCounted1++;

                    byte[] meta = dbu1.getMetaData(h);

                    try {
                        MetaData md = MetaDataUtil.read(new ByteArrayInputStream(meta));
                        if (md != null) {
                            numMetaChunksVerified1++;
                        }
                    } catch (Exception ex) { /* Nope */ }
                }
                if (dbu2.hasMetaData(h)) {
                    numMetaChunksCounted2++;

                    byte[] meta = dbu2.getMetaData(h);

                    try {
                        MetaData md = MetaDataUtil.read(new ByteArrayInputStream(meta));
                        if (md != null) {
                            numMetaChunksVerified2++;
                        }
                    } catch (Exception ex) { /* Nope */ }
                }
                if (dbu3.hasMetaData(h)) {
                    numMetaChunksCounted3++;

                    byte[] meta = dbu3.getMetaData(h);

                    try {
                        MetaData md = MetaDataUtil.read(new ByteArrayInputStream(meta));
                        if (md != null) {
                            numMetaChunksVerified3++;
                        }
                    } catch (Exception ex) { /* Nope */ }
                }
            }

            System.out.println("Found DBU #1 " + numDataChunksCounted1 + " data chunks, " + numMetaChunksCounted1 + " meta data chunks.");
            System.out.println("Found DBU #2 " + numDataChunksCounted2 + " data chunks, " + numMetaChunksCounted2 + " meta data chunks.");
            System.out.println("Found DBU #3 " + numDataChunksCounted3 + " data chunks, " + numMetaChunksCounted3 + " meta data chunks.");

            System.out.println("Verified DBU #1 " + numDataChunksVerified1 + " data chunks, " + numMetaChunksVerified1 + " meta data chunks.");
            System.out.println("Verified DBU #2 " + numDataChunksVerified2 + " data chunks, " + numMetaChunksVerified2 + " meta data chunks.");
            System.out.println("Verified DBU #3 " + numDataChunksVerified3 + " data chunks, " + numMetaChunksVerified3 + " meta data chunks.");

            // Now make assertions
            assertEquals("Expecting certain reported data chunks for DBU #1", dataChunks.size(), numDataChunksReported1);
            assertEquals("Expecting certain counted data chunks for DBU #1", dataChunks.size(), numDataChunksCounted1);

            assertEquals("Expecting certain reported meta chunks for DBU #1", metaChunks.size(), numMetaChunksReported1);
            assertEquals("Expecting certain counted meta chunks for DBU #1", metaChunks.size(), numMetaChunksCounted1);

            assertEquals("Expecting certain reported data chunks for DBU #2", dataChunks.size(), numDataChunksReported2);
            assertEquals("Expecting certain counted data chunks for DBU #2", dataChunks.size(), numDataChunksCounted2);

            assertEquals("Expecting certain reported meta chunks for DBU #2", metaChunks.size(), numMetaChunksReported2);
            assertEquals("Expecting certain counted meta chunks for DBU #2", metaChunks.size(), numMetaChunksCounted2);

            assertEquals("Expecting certain reported data chunks for DBU #3", dataChunks.size(), numDataChunksReported3);
            assertEquals("Expecting certain counted data chunks for DBU #3", dataChunks.size(), numDataChunksCounted3);

            assertEquals("Expecting certain reported meta chunks for DBU #3", metaChunks.size(), numMetaChunksReported3);
            assertEquals("Expecting certain counted meta chunks for DBU #3", metaChunks.size(), numMetaChunksCounted3);

            assertEquals("Expecting same number of veriable data chunks b/w DBU #1 and comb. of #2 and #3.", numDataChunksVerified1, numDataChunksVerified2 + numDataChunksVerified3);
            assertEquals("Expecting same number of veriable meta chunks b/w DBU #1 and comb. of #2 and #3.", numMetaChunksVerified1, numMetaChunksVerified2 + numMetaChunksVerified3);
        } finally {
            IOUtil.recursiveDeleteWithWarning(dataDir1);
            IOUtil.recursiveDeleteWithWarning(dataDir2);
            IOUtil.recursiveDeleteWithWarning(dataDir3);
            dbu1.close();
            dbu2.close();
            dbu3.close();
        }
    }

    /**
     * This test does add, has, get, and delete operations with a meta data larger than 1MB
     */
    public void testBigMetaData() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBigMetaData()");

        File home = null;
        DataBlockUtil dbu = null;
        try {
            home = TempFileUtil.createTemporaryDirectory();
            DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(home.getAbsolutePath(), Long.MAX_VALUE);

            dbu = new DataBlockUtil();
            dbu.add(ddc);

            byte[] metaBytes = DevUtil.createRandomBigMetaDataChunk();

            // bytes should be more than 1MB
            assertTrue("Expect meta data to be longer than 1MB.", metaBytes.length > DataBlockUtil.ONE_MB);

            BigHash metaHash = new BigHash(metaBytes);
            assertFalse("Expecting meta data to not yet exist.", dbu.hasMetaData(metaHash));

            // add the meta data to the data block
            dbu.addMetaData(metaHash, metaBytes);

            // does it have the meta data?
            assertTrue("Expecting meta data to exist.", dbu.hasMetaData(metaHash));
            // can we get the meta data?
            assertNotNull("Expecting meta data to exist.", dbu.getMetaData(metaHash));

            // can we delete the meta data?
            dbu.deleteMetaData(metaHash, "testing");
            assertFalse("Expecting meta data to not exist anymore.", dbu.hasMetaData(metaHash));
        } finally {
            IOUtil.recursiveDeleteWithWarning(home);
            dbu.close();
        }
    }

    public void testDeleteAndReAddWithDifferentBytes() throws Exception {
        TestUtil.printTitle("DataBlockTest:testDeleteAndReAddWithDifferentBytes()");
        testDeleteAndReAdd(true);
    }

    public void testDeleteAndReAddWithSameBytes() throws Exception {
        TestUtil.printTitle("DataBlockTest:testDeleteAndReAddWithSameBytes()");
        testDeleteAndReAdd(false);
    }

    /**
     * <p>Want to make sure that can delete and readd a chunk without problems.</p>
     */
    public void testDeleteAndReAdd(boolean useDifferentBytesWhenReAdding) throws Exception {
        // We want to pound the same data block and keep it active
        String prefix = RandomUtil.getString(4);

        // Create a BigHash to test
        BigHash toDeleteAndReAddData = DevUtil.getRandomBigHashStartsWith(prefix);
        BigHash toDeleteAndReAddMeta = DevUtil.getRandomBigHashStartsWith(prefix);

        // Make sure different
        while (toDeleteAndReAddData.equals(toDeleteAndReAddMeta)) {
            toDeleteAndReAddMeta = DevUtil.getRandomBigHashStartsWith(prefix);
        }

        DataBlockUtil dbu = new DataBlockUtil();
        File home = null;
        try {
            home = TempFileUtil.createTemporaryDirectory();
            DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(home.getAbsolutePath(), Long.MAX_VALUE);

            dbu.add(ddc);

            /**
             * Step 1 of 6: Add
             */
            byte[] dataBytes = DevUtil.createRandomDataChunk(1024 * 1024);
            byte[] metaBytes = DevUtil.createRandomMetaDataChunk();

            dbu.addData(toDeleteAndReAddData, dataBytes);
            dbu.addMetaData(toDeleteAndReAddMeta, metaBytes);

            assertTrue("Expecting data to exist.", dbu.hasData(toDeleteAndReAddData));
            assertNotNull("Expecting data to exist.", dbu.getData(toDeleteAndReAddData));

            assertTrue("Expecting meta data to exist.", dbu.hasMetaData(toDeleteAndReAddMeta));
            assertNotNull("Expecting meta data to exist.", dbu.getMetaData(toDeleteAndReAddMeta));

            /**
             * Step 2 of 6: Delete
             */
            dbu.deleteData(toDeleteAndReAddData, "testing");
            dbu.deleteMetaData(toDeleteAndReAddMeta, "testing");

            assertFalse("Expecting data to not exist.", dbu.hasData(toDeleteAndReAddData));
            assertFalse("Expecting meta data to exist.", dbu.hasMetaData(toDeleteAndReAddMeta));

            // Getting data should either throw an exception or return null
            try {
                byte[] data = dbu.getData(toDeleteAndReAddData);
                if (data != null) {
                    fail("Expected data to not be available");
                }
            } catch (Exception ex) { /* nope */ }

            // Getting data should either throw an exception or return null
            try {
                byte[] data = dbu.getMetaData(toDeleteAndReAddMeta);
                if (data != null) {
                    fail("Expected data to not be available");
                }
            } catch (Exception ex) { /* nope */ }

            /**
             * Step 3 of 6: Add
             */
            if (useDifferentBytesWhenReAdding) {
                dataBytes = DevUtil.createRandomDataChunk(1024 * 1024);
                metaBytes = DevUtil.createRandomMetaDataChunk();
            }

            dbu.addData(toDeleteAndReAddData, dataBytes);
            dbu.addMetaData(toDeleteAndReAddMeta, metaBytes);

            assertTrue("Expecting data to exist.", dbu.hasData(toDeleteAndReAddData));
            assertNotNull("Expecting data to exist.", dbu.getData(toDeleteAndReAddData));

            assertTrue("Expecting meta data to exist.", dbu.hasMetaData(toDeleteAndReAddMeta));
            assertNotNull("Expecting meta data to exist.", dbu.getMetaData(toDeleteAndReAddMeta));

            /**
             * Step 4 of 6: Delete
             */
            dbu.deleteData(toDeleteAndReAddData, "testing");
            dbu.deleteMetaData(toDeleteAndReAddMeta, "testing");

            assertFalse("Expecting data to not exist.", dbu.hasData(toDeleteAndReAddData));
            assertFalse("Expecting meta data to exist.", dbu.hasMetaData(toDeleteAndReAddMeta));

            // Getting data should either throw an exception or return null
            try {
                byte[] data = dbu.getData(toDeleteAndReAddData);
                if (data != null) {
                    fail("Expected data to not be available");
                }
            } catch (Exception ex) { /* nope */ }

            // Getting data should either throw an exception or return null
            try {
                byte[] data = dbu.getMetaData(toDeleteAndReAddMeta);
                if (data != null) {
                    fail("Expected data to not be available");
                }
            } catch (Exception ex) { /* nope */ }

            /**
             * Step 5 of 6: Add
             */
            if (useDifferentBytesWhenReAdding) {
                dataBytes = DevUtil.createRandomDataChunk(1024 * 1024);
                metaBytes = DevUtil.createRandomMetaDataChunk();
            }

            dbu.addData(toDeleteAndReAddData, dataBytes);
            dbu.addMetaData(toDeleteAndReAddMeta, metaBytes);

            assertTrue("Expecting data to exist.", dbu.hasData(toDeleteAndReAddData));
            assertNotNull("Expecting data to exist.", dbu.getData(toDeleteAndReAddData));

            assertTrue("Expecting meta data to exist.", dbu.hasMetaData(toDeleteAndReAddMeta));
            assertNotNull("Expecting meta data to exist.", dbu.getMetaData(toDeleteAndReAddMeta));

            /**
             * Step 6 of 6: Delete
             */
            dbu.deleteData(toDeleteAndReAddData, "testing");
            dbu.deleteMetaData(toDeleteAndReAddMeta, "testing");

            assertFalse("Expecting data to not exist.", dbu.hasData(toDeleteAndReAddData));
            assertFalse("Expecting meta data to exist.", dbu.hasMetaData(toDeleteAndReAddMeta));

            // Getting data should either throw an exception or return null
            try {
                byte[] data = dbu.getData(toDeleteAndReAddData);
                if (data != null) {
                    fail("Expected data to not be available");
                }
            } catch (Exception ex) { /* nope */ }

            // Getting data should either throw an exception or return null
            try {
                byte[] data = dbu.getMetaData(toDeleteAndReAddMeta);
                if (data != null) {
                    fail("Expected data to not be available");
                }
            } catch (Exception ex) { /* nope */ }
        } finally {
            if (home != null) {
                IOUtil.recursiveDelete(home);
            }
            dbu.close();
        }
    }

    /**
     * <p>Demonstrates DataBlock.moveToDataDirectoryConfiguration works.</p>
     */
    public void testMoveDataBlockToNewDataDirectoryConfiguration() throws Exception {
        TestUtil.printTitle("DataBlockTest:testMoveDataBlockToNewDataDirectoryConfiguration()");

        final DataBlockUtil dbu = new DataBlockUtil();

        File dataDir1 = null, dataDir2 = null;

        try {
            dataDir1 = TempFileUtil.createTemporaryDirectory();
            dataDir2 = TempFileUtil.createTemporaryDirectory();

            assertFalse("Directories should not be equal.", dataDir1.getAbsolutePath().equals(dataDir2.getAbsolutePath()));

            final DataDirectoryConfiguration ddc1 = new DataDirectoryConfiguration(dataDir1.getAbsolutePath(), Long.MAX_VALUE);
            final DataDirectoryConfiguration ddc2 = new DataDirectoryConfiguration(dataDir2.getAbsolutePath(), Long.MAX_VALUE);

            assertEquals("Expecting accurate bytes.", 0, ddc1.getActualSize());
            assertEquals("Expecting accurate bytes.", 0, ddc2.getActualSize());

            dbu.add(ddc1);

            assertEquals("Expecting one data directory configuration.", 1, dbu.getDataDirectoryConfigurations().size());
            assertEquals("Expecting data directory to be empty.", 0, ddc1.getDirectoryFile().list().length);

            // Two chunks with same hash prefix so end up in same data block.
            final byte[] dataChunk = DevUtil.createRandomDataChunkStartsWith("ab");
            final BigHash dataHash = new BigHash(dataChunk);

            final byte[] metaChunk = DevUtil.createRandomMetaDataChunk();
            final BigHash metaHash = DevUtil.getRandomBigHashStartsWith("ab");

            dbu.addData(dataHash, dataChunk);
            dbu.addMetaData(metaHash, metaChunk);

            assertTrue("Should have data hash.", dbu.hasData(dataHash));
            assertTrue("Should have meta hash.", dbu.hasMetaData(metaHash));

            assertEquals("Expecting one data block in data directory.", 1, ddc1.getDirectoryFile().list().length);
            assertEquals("Expecting accurate bytes.", dataChunk.length + metaChunk.length + DataBlock.HEADERS_PER_FILE * DataBlock.bytesPerEntry, ddc1.getActualSize());

            dbu.add(ddc2);

            assertEquals("Expecting two data directory configurations.", 2, dbu.getDataDirectoryConfigurations().size());
            assertEquals("Expecting data directory to be empty.", 0, ddc2.getDirectoryFile().list().length);

            // Get the data block. Should be in DDC #1
            final DataBlock db = dbu.getDataBlockToAddChunk(dataHash);
            assertEquals("Data block should know it is in DDC #1.", ddc1, db.ddc);

            final boolean moved = db.moveToDataDirectoryConfiguration(ddc2);
            assertTrue("Expecting data directory to be moved.", moved);

            assertEquals("Data block should know it is in DDC #2 now.", ddc2, db.ddc);
            assertEquals("Expecting data directory to be empty.", 0, ddc1.getDirectoryFile().list().length);
            assertEquals("Expecting one data block in data directory.", 1, ddc2.getDirectoryFile().list().length);
            assertEquals("Expecting accurate bytes.", 0, ddc1.getActualSize());
            assertEquals("Expecting accurate bytes.", dataChunk.length + metaChunk.length + DataBlock.HEADERS_PER_FILE * DataBlock.bytesPerEntry, ddc2.getActualSize());

        } finally {
            IOUtil.recursiveDeleteWithWarning(dataDir1);
            IOUtil.recursiveDeleteWithWarning(dataDir2);
            dbu.close();
        }
    }

    /**
     * <p>Demonstrates DataBlock.moveToDataDirectoryConfiguration fails if DataDirectoryConfiguration is full.</p>
     * @throws java.lang.Exception
     */
    public void testMoveDataBlockToNewDataDirectoryConfigurationFailsIfFull() throws Exception {
        TestUtil.printTitle("DataBlockTest:testMoveDataBlockToNewDataDirectoryConfigurationFailsIfFull()");

        final DataBlockUtil dbu = new DataBlockUtil();
        File dataDir1 = null, dataDir2 = null;

        try {
            dataDir1 = TempFileUtil.createTemporaryDirectory();
            dataDir2 = TempFileUtil.createTemporaryDirectory();

            assertFalse("Directories should not be equal.", dataDir1.getAbsolutePath().equals(dataDir2.getAbsolutePath()));

            final DataDirectoryConfiguration ddc1 = new DataDirectoryConfiguration(dataDir1.getAbsolutePath(), 2 * 1024 * 1024);
            final DataDirectoryConfiguration ddc2 = new DataDirectoryConfiguration(dataDir2.getAbsolutePath(), 2 * 1024 * 1024);

            assertEquals("Expecting accurate bytes.", 0, ddc1.getActualSize());
            assertEquals("Expecting accurate bytes.", 0, ddc2.getActualSize());

            dbu.add(ddc1);

            assertEquals("Expecting one data directory configuration.", 1, dbu.getDataDirectoryConfigurations().size());
            assertEquals("Expecting data directory to be empty.", 0, ddc1.getDirectoryFile().list().length);

            // Two chunks with same hash prefix so end up in same data block.
            final byte[] dataChunk = DevUtil.createRandomDataChunkStartsWith("ab");
            final BigHash dataHash = new BigHash(dataChunk);

            final byte[] metaChunk = DevUtil.createRandomMetaDataChunk();
            final BigHash metaHash = DevUtil.getRandomBigHashStartsWith("ab");

            dbu.addData(dataHash, dataChunk);
            dbu.addMetaData(metaHash, metaChunk);

            assertTrue("Should have data hash.", dbu.hasData(dataHash));
            assertTrue("Should have meta hash.", dbu.hasMetaData(metaHash));

            assertEquals("Expecting one data block in data directory.", 1, ddc1.getDirectoryFile().list().length);
            assertEquals("Expecting accurate bytes.", dataChunk.length + metaChunk.length + DataBlock.HEADERS_PER_FILE * DataBlock.bytesPerEntry, ddc1.getActualSize());

            // Simulate full. This is what project finding thread does.
            ddc2.adjustUsedSpace(1024 * 1024 * 2);

            dbu.add(ddc2);

            assertEquals("Expecting two data directory configurations.", 2, dbu.getDataDirectoryConfigurations().size());
            assertEquals("Expecting data directory to be empty.", 0, ddc2.getDirectoryFile().list().length);

            // Get the data block. Should be in DDC #1
            final DataBlock db = dbu.getDataBlockToAddChunk(dataHash);
            assertEquals("Data block should know it is in DDC #1.", ddc1, db.ddc);

            final boolean moved = db.moveToDataDirectoryConfiguration(ddc2);
            assertFalse("Should not move: DDC is at its limit.", moved);

            assertEquals("Data block should know it is still in DDC #1.", ddc1, db.ddc);
            assertEquals("Expecting data directory to be empty.", 0, ddc2.getDirectoryFile().list().length);
            assertEquals("Expecting one data block in data directory.", 1, ddc1.getDirectoryFile().list().length);
            assertEquals("Expecting accurate bytes.", dataChunk.length + metaChunk.length + DataBlock.HEADERS_PER_FILE * DataBlock.bytesPerEntry, ddc1.getActualSize());
        } finally {
            IOUtil.recursiveDeleteWithWarning(dataDir1);
            IOUtil.recursiveDeleteWithWarning(dataDir2);
            dbu.close();
        }
    }

    /**
     * <p>Demonstrates DataBlock.moveToDataDirectoryConfiguration creates the destination data directory if not exist.</p>
     */
    public void testMoveDataBlockToNewDataDirectoryConfigurationCreatesDirectory() throws Exception {
        TestUtil.printTitle("DataBlockTest:testMoveDataBlockToNewDataDirectoryConfigurationCreatesDirectory()");

        final DataBlockUtil dbu = new DataBlockUtil();
        File dataDir1 = null, dataDir2 = null;

        try {
            dataDir1 = TempFileUtil.createTemporaryDirectory();
            dataDir2 = TempFileUtil.createTemporaryDirectory();

            assertFalse("Directories should not be equal.", dataDir1.getAbsolutePath().equals(dataDir2.getAbsolutePath()));

            final DataDirectoryConfiguration ddc1 = new DataDirectoryConfiguration(dataDir1.getAbsolutePath(), Long.MAX_VALUE);
            final DataDirectoryConfiguration ddc2 = new DataDirectoryConfiguration(dataDir2.getAbsolutePath(), Long.MAX_VALUE);

            assertEquals("Expecting accurate bytes.", 0, ddc1.getActualSize());
            assertEquals("Expecting accurate bytes.", 0, ddc2.getActualSize());

            // Delete the second DDC. It doesn't exist any longer.
            IOUtil.recursiveDeleteWithWarning(dataDir2);
            assertFalse("Data directory #2 should not longer exist.", dataDir2.exists());

            dbu.add(ddc1);

            assertEquals("Expecting one data directory configuration.", 1, dbu.getDataDirectoryConfigurations().size());
            assertEquals("Expecting data directory to be empty.", 0, ddc1.getDirectoryFile().list().length);

            // Two chunks with same hash prefix so end up in same data block.
            final byte[] dataChunk = DevUtil.createRandomDataChunkStartsWith("ab");
            final BigHash dataHash = new BigHash(dataChunk);

            final byte[] metaChunk = DevUtil.createRandomMetaDataChunk();
            final BigHash metaHash = DevUtil.getRandomBigHashStartsWith("ab");

            dbu.addData(dataHash, dataChunk);
            dbu.addMetaData(metaHash, metaChunk);

            assertTrue("Should have data hash.", dbu.hasData(dataHash));
            assertTrue("Should have meta hash.", dbu.hasMetaData(metaHash));

            assertEquals("Expecting one data block in data directory.", 1, ddc1.getDirectoryFile().list().length);
            assertEquals("Expecting accurate bytes.", dataChunk.length + metaChunk.length + DataBlock.HEADERS_PER_FILE * DataBlock.bytesPerEntry, ddc1.getActualSize());

            // Add the second data directory. It does not exist.
            dbu.add(ddc2);

            assertEquals("Expecting two data directory configurations.", 2, dbu.getDataDirectoryConfigurations().size());

            // Get the data block. Should be in DDC #1
            final DataBlock db = dbu.getDataBlockToAddChunk(dataHash);
            assertEquals("Data block should know it is in DDC #1.", ddc1, db.ddc);

            final boolean moved = db.moveToDataDirectoryConfiguration(ddc2);
            assertTrue("Expecting data directory to be moved.", moved);

            assertEquals("Data block should know it is in DDC #2 now.", ddc2, db.ddc);
            assertEquals("Expecting data directory to be empty.", 0, ddc1.getDirectoryFile().list().length);
            assertEquals("Expecting one data block in data directory.", 1, ddc2.getDirectoryFile().list().length);
            assertEquals("Expecting accurate bytes.", 0, ddc1.getActualSize());
            assertEquals("Expecting accurate bytes.", dataChunk.length + metaChunk.length + DataBlock.HEADERS_PER_FILE * DataBlock.bytesPerEntry, ddc2.getActualSize());

        } finally {
            IOUtil.recursiveDeleteWithWarning(dataDir1);
            IOUtil.recursiveDeleteWithWarning(dataDir2);
            dbu.close();
        }
    }

    /**
     * <p>Demonstrates DataBlock.moveToDataDirectoryConfiguration fails if cannot use DDC.</p>
     * @throws java.lang.Exception
     */
    public void testMoveDataBlockToNewDataDirectoryConfigurationFailsIfCannotUseDDC() throws Exception {
        TestUtil.printTitle("DataBlockTest:testMoveDataBlockToNewDataDirectoryConfigurationFailsIfCannotUseDDC()");

        final DataBlockUtil dbu = new DataBlockUtil();
        File dataDir1 = null, dataDir2 = null;
        try {
            dataDir1 = TempFileUtil.createTemporaryDirectory();
            dataDir2 = TempFileUtil.createTemporaryDirectory();

            assertFalse("Directories should not be equal.", dataDir1.getAbsolutePath().equals(dataDir2.getAbsolutePath()));

            final DataDirectoryConfiguration ddc1 = new DataDirectoryConfiguration(dataDir1.getAbsolutePath(), Long.MAX_VALUE);
            final DataDirectoryConfiguration ddc2 = new DataDirectoryConfiguration(dataDir2.getAbsolutePath(), Long.MAX_VALUE);

            assertEquals("Expecting accurate bytes.", 0, ddc1.getActualSize());
            assertEquals("Expecting accurate bytes.", 0, ddc2.getActualSize());

            dbu.add(ddc1);

            assertEquals("Expecting one data directory configuration.", 1, dbu.getDataDirectoryConfigurations().size());
            assertEquals("Expecting data directory to be empty.", 0, ddc1.getDirectoryFile().list().length);

            // Two chunks with same hash prefix so end up in same data block.
            final byte[] dataChunk = DevUtil.createRandomDataChunkStartsWith("ab");
            final BigHash dataHash = new BigHash(dataChunk);

            final byte[] metaChunk = DevUtil.createRandomMetaDataChunk();
            final BigHash metaHash = DevUtil.getRandomBigHashStartsWith("ab");

            dbu.addData(dataHash, dataChunk);
            dbu.addMetaData(metaHash, metaChunk);

            assertTrue("Should have data hash.", dbu.hasData(dataHash));
            assertTrue("Should have meta hash.", dbu.hasMetaData(metaHash));

            assertEquals("Expecting one data block in data directory.", 1, ddc1.getDirectoryFile().list().length);
            assertEquals("Expecting accurate bytes.", dataChunk.length + metaChunk.length + DataBlock.HEADERS_PER_FILE * DataBlock.bytesPerEntry, ddc1.getActualSize());

            // Add the second data directory.
            dbu.add(ddc2);

            // Delete the second DDC. It doesn't exist any longer.
            IOUtil.recursiveDeleteWithWarning(dataDir2);
            assertFalse("Data directory #2 should not longer exist.", dataDir2.exists());

            final boolean created = dataDir2.createNewFile();
            assertTrue("Should have created file.", created);
            assertTrue("Data directory should actually be a file now.", dataDir2.isFile());

            assertEquals("Expecting two data directory configurations.", 2, dbu.getDataDirectoryConfigurations().size());

            // Get the data block. Should be in DDC #1
            final DataBlock db = dbu.getDataBlockToAddChunk(dataHash);
            assertEquals("Data block should know it is in DDC #1.", ddc1, db.ddc);

            try {
                final boolean moved = db.moveToDataDirectoryConfiguration(ddc2);
                fail("Should have throw exception, data directory is actually a file. (Was move successful?: " + moved + ")");
            } catch (Exception ex) { /* Expected */ }

            assertEquals("Data block should know it is in DDC #1 still.", ddc1, db.ddc);
            assertEquals("Expecting one data block in data directory.", 1, ddc1.getDirectoryFile().list().length);
            assertEquals("Expecting accurate bytes.", dataChunk.length + metaChunk.length + DataBlock.HEADERS_PER_FILE * DataBlock.bytesPerEntry, ddc1.getActualSize());
            assertEquals("Expecting accurate bytes.", 0, ddc2.getActualSize());
        } finally {
            IOUtil.recursiveDeleteWithWarning(dataDir1);
            IOUtil.recursiveDeleteWithWarning(dataDir2);
            dbu.close();
        }
    }

    /**
     *          <--- [START] Don't uncomment unless want to add feature: --->
     *             <--- Support multiple instances same data block --->
     *///    /**
//     * <p>Tests that two DataBlock's with the exact same file path can be found in two different data directories.</p>
//     * <p>THIS FAILS. CURRENTLY DO NOT SUPPORT.</p>
//     * @throws java.lang.Exception
//     */
//    public void testFindsDataBlocksInDifferentDirectoriesWithSameName() throws Exception {
//        DataBlockUtil dbu = new DataBlockUtil();
//        File dataDir1 = null, dataDir2 = null;
//        
//        try {
//            dataDir1 = TempFileUtil.createTemporaryDirectory();
//            dataDir2 = TempFileUtil.createTemporaryDirectory();
//            assertFalse("Directories must be different.", dataDir1.equals(dataDir2));
//            
//            final DataDirectoryConfiguration ddc1 = new DataDirectoryConfiguration(dataDir1.getAbsolutePath(), Long.MAX_VALUE);
//            final DataDirectoryConfiguration ddc2 = new DataDirectoryConfiguration(dataDir2.getAbsolutePath(), Long.MAX_VALUE);
//            
//            /**
//             * Add data and meta data to DDC #1
//             */
//            dbu.add(ddc1);
//            final byte[] dataChunk1 = DevUtil.createRandomDataChunkStartsWith("1f");
//            final BigHash dataHash1 = new BigHash(dataChunk1);
//            
//            final byte[] metaChunk1 = DevUtil.createRandomBigMetaDataChunk();
//            final BigHash metaHash1 = DevUtil.getRandomBigHashStartsWith("1f");
//            
//            dbu.addData(dataHash1, dataChunk1);
//            dbu.addMetaData(metaHash1, metaChunk1);
//            
//            assertTrue("Should have chunk.", dbu.hasData(dataHash1));
//            assertTrue("Should have chunk.", dbu.hasMetaData(metaHash1));
//            
//            dbu.close();
//            
//            /**
//             * Add data and meta data to DDC #2
//             */
//            dbu = new DataBlockUtil();
//            dbu.add(ddc2);
//            final byte[] dataChunk2 = DevUtil.createRandomDataChunkStartsWith("1f");
//            final BigHash dataHash2 = new BigHash(dataChunk2);
//            
//            final byte[] metaChunk2 = DevUtil.createRandomBigMetaDataChunk();
//            final BigHash metaHash2 = DevUtil.getRandomBigHashStartsWith("1f");
//            
//            assertFalse("Must have distinct chunks.", dataHash1.equals(dataHash2));
//            assertFalse("Must have distinct chunks.", metaHash1.equals(metaHash2));
//            
//            assertFalse("Must have distinct chunks.", dataHash1.equals(metaHash2));
//            assertFalse("Must have distinct chunks.", metaHash1.equals(dataHash2));
//            
//            dbu.addData(dataHash2, dataChunk2);
//            dbu.addMetaData(metaHash2, metaChunk2);
//            
//            assertTrue("Should have chunk.", dbu.hasData(dataHash2));
//            assertTrue("Should have chunk.", dbu.hasMetaData(metaHash2));
//            assertFalse("Should not have chunk.", dbu.hasData(dataHash1));
//            assertFalse("Should not have chunk.", dbu.hasMetaData(metaHash1));
//            
//            dbu.add(ddc1);
//            
//            // Should have all chunks
//            assertTrue("Should have chunk.", dbu.hasData(dataHash2));
//            assertTrue("Should have chunk.", dbu.hasMetaData(metaHash2));
//            assertTrue("Should have chunk.", dbu.hasData(dataHash1));
//            assertTrue("Should have chunk.", dbu.hasMetaData(metaHash1));
//            
//            assertEquals("Expect DataBlock in certain DDC.", ddc1, dbu.getDataBlock(dataHash1).ddc);
//            assertEquals("Expect DataBlock in certain DDC.", ddc1, dbu.getDataBlock(dataHash1).ddc);
//            assertEquals("Expect DataBlock in certain DDC.", ddc2, dbu.getDataBlock(dataHash2).ddc);
//            assertEquals("Expect DataBlock in certain DDC.", ddc2, dbu.getDataBlock(dataHash2).ddc);
//            
//        } finally {
//            IOUtil.recursiveDeleteWithWarning(dataDir1);
//            IOUtil.recursiveDeleteWithWarning(dataDir2);
//            dbu.close();
//        }
//    }
    /**
     *          <--- [END] Don't uncomment unless want to add feature: --->
     *             <--- Support multiple instances same data block --->
     */
//    /**
//     * <p>Shows that data blocks can be "split" into multiple data directories. E.g.:</p>
//     * <ul>
//     *   <li>/media/sdb1/ProteomeCommons.org-Tranche/data/ab/00</li>
//     *   <li>/media/sdf1/ProteomeCommons.org-Tranche/data/ab/01</li>
//     *   <li>/media/sda1/ProteomeCommons.org-Tranche/data/ab/02</li>
//     *   <li>Etc.</li>
//     * </ul>
//     * <p>This behavior already occurs on servers, but I need to formally demonstrate before completely the data directory balancing code.</p>
//     * @throws java.lang.Exception
//     */
//    public void testDataBlocksCanBeSplitIntoMultipleDataDirectories() throws Exception {
//        TestUtil.printTitle("DataBlockTest:testDataBlocksCanBeSplitIntoMultipleDataDirectories()");
//
//        File data1 = null, data2 = null, data3 = null;
//        DataBlockUtil dbu = new DataBlockUtil();
//
//        try {
//            data1 = TempFileUtil.createTemporaryDirectory();
//            data2 = TempFileUtil.createTemporaryDirectory();
//            data3 = TempFileUtil.createTemporaryDirectory();
//
//            assertTrue("Directory better exist.", data1.exists());
//            assertTrue("Directory better exist.", data2.exists());
//            assertTrue("Directory better exist.", data3.exists());
//            assertFalse("Directories better not equal.", data1.equals(data2));
//            assertFalse("Directories better not equal.", data2.equals(data3));
//            assertFalse("Directories better not equal.", data3.equals(data1));
//
//            // Each hash represents a data and meta data chunk
//            Set<BigHash> hashes = new HashSet();
//
//            DataDirectoryConfiguration ddc1 = new DataDirectoryConfiguration(data1.getAbsolutePath(), Long.MAX_VALUE);
//            DataDirectoryConfiguration ddc2 = new DataDirectoryConfiguration(data2.getAbsolutePath(), Long.MAX_VALUE);
//            DataDirectoryConfiguration ddc3 = new DataDirectoryConfiguration(data3.getAbsolutePath(), Long.MAX_VALUE);
//
//            // First, use only 1 data directory
//            dbu.add(ddc1);
//
//            // Want lots of splitting. Each pair is one data, one meta
//            final int numChunkPairsToAdd = 1000 * 10;
//
//            for (int i = 0; i < numChunkPairsToAdd; i++) {
//                byte[] dataChunk = DevUtil.createRandomDataChunk(1024);
//                BigHash hash = new BigHash(dataChunk);
//                byte[] hashBytes = hash.toByteArray();
//
//                byte[] metaChunk = DevUtil.createRandomMetaDataChunk();
//
//                // Actually, tweak hash's bytes:
//                //
//                if (RandomUtil.getBoolean()) {
//                    hashBytes[0] = 2;
//                } else {
//                    hashBytes[0] = 2;
//                    hashBytes[1] = 2;
//                }
//
//                // Modified hash to encourage splitting
//                hash = BigHash.createFromBytes(hashBytes);
//
//                if (hashes.contains(hash)) {
//                    i--;
//                    continue;
//                }
//
//                dbu.addData(hash, dataChunk);
//                dbu.addMetaData(hash, metaChunk);
//                hashes.add(hash);
//            }
//
//            System.out.println("1. Done adding");
//
//            // Manually merge any files
//            List<String> files = new ArrayList();
//            files.add(data1.getAbsolutePath());
//
//            while (files.size() > 0) {
//                // Grab last, depth-first
//                File next = new File(files.remove(files.size() - 1));
//                if (next.isDirectory()) {
//                    for (File f : next.listFiles()) {
//                        files.add(f.getAbsolutePath());
//                    }
//                } else {
//                    if (next.getName().endsWith(".merge")) {
//                        if (next.exists()) {
//                            System.out.println("  Merging: " + next.getAbsolutePath());
//                            dbu.mergeOldDataBlock(next);
//                        }
//                    }
//                }
//            }
//
//            System.out.println("2. Done merging");
//
//            assertEquals("Should have correct number of hashes.", numChunkPairsToAdd, hashes.size());
//            assertEquals("Should have correct number of hashes.", numChunkPairsToAdd, dbu.dataHashes.size(false));
//
//            for (BigHash h : hashes) {
//                assertTrue("Better have data chunk.", dbu.hasData(h));
//                assertTrue("Better have meta chunk.", dbu.hasMetaData(h));
//            }
//
//            System.out.println("3. Done verifying exist");
//
////            System.out.println();
////            System.out.println("////////////////////////////////////////////////////////////////////");
////            System.out.println("// Done adding " + numChunkPairsToAdd + ", here's the structure:");
////            System.out.println("////////////////////////////////////////////////////////////////////");
////            System.out.println();
////            Text.printRecursiveDirectoryStructure(data1);
//
//            // Add second directory.
//            dbu.add(ddc2);
//
//            // Now grab a handful of random data blocks. Greater than 256 guarentees
//            // that some will be data/02 and some data/02/02
//            final int dataBlocksToMove = 300;
//            int movedCount = 0;
//
//            Iterator<BigHash> hashesIt = hashes.iterator();
//            while (movedCount < dataBlocksToMove && hashesIt.hasNext()) {
//                DataBlock db = dbu.getDataBlockToAddChunk(hashesIt.next());
//                boolean moved = db.moveToDataDirectoryConfiguration(ddc2);
//
//                // Failed, perhaps already moved!
//                if (moved) {
//                    movedCount++;
//                }
//            }
//
//            System.out.println("4. Done moving");
//
////            System.out.println();
////            System.out.println("////////////////////////////////////////////////////////////////////");
////            System.out.println("//  Done moving " + dataBlocksToMove + ", here's the structure:   ");
////            System.out.println("////////////////////////////////////////////////////////////////////");
////            System.out.println();
////            Text.printRecursiveDirectoryStructure(data1);
////            Text.printRecursiveDirectoryStructure(data2);
//
//            assertEquals("Expecting certain number of data blocks moved.", dataBlocksToMove, movedCount);
//
//            for (BigHash h : hashes) {
//                assertTrue("Better have data chunk.", dbu.hasData(h));
//                assertTrue("Better have meta chunk.", dbu.hasMetaData(h));
//            }
//
//            System.out.println("5. Done verifying all chunks still there after move.");
//
//            // Make sure we can still add
//            for (int i = 0; i < numChunkPairsToAdd; i++) {
//                byte[] dataChunk = DevUtil.createRandomDataChunk(1024);
//                BigHash hash = new BigHash(dataChunk);
//                byte[] hashBytes = hash.toByteArray();
//
//                byte[] metaChunk = DevUtil.createRandomMetaDataChunk();
//
//                // Actually, tweak hash's bytes:
//                //
//                if (RandomUtil.getBoolean()) {
//                    hashBytes[0] = 2;
//                } else {
//                    hashBytes[0] = 2;
//                    hashBytes[1] = 2;
//                }
//
//                // Modified hash to encourage splitting
//                hash = BigHash.createFromBytes(hashBytes);
//
//                if (hashes.contains(hash)) {
//                    i--;
//                    continue;
//                }
//
//                dbu.addData(hash, dataChunk);
//                dbu.addMetaData(hash, metaChunk);
//                hashes.add(hash);
//            }
//
//            System.out.println("6. Done adding again");
//
//            // Manually merge any files
//            files = new ArrayList();
//            files.add(data1.getAbsolutePath());
//
//            while (files.size() > 0) {
//                // Grab last, depth-first
//                File next = new File(files.remove(files.size() - 1));
//                if (next.isDirectory()) {
//                    for (File f : next.listFiles()) {
//                        files.add(f.getAbsolutePath());
//                    }
//                } else {
//                    if (next.getName().endsWith(".merge")) {
//                        if (next.exists()) {
//                            System.out.println("  Merging: " + next.getAbsolutePath());
//                            dbu.mergeOldDataBlock(next);
//                        }
//                    }
//                }
//            }
//
//            System.out.println("7. Done merging again");
//
//            assertEquals("Should have correct number of hashes.", numChunkPairsToAdd * 2, hashes.size());
//            assertEquals("Should have correct number of hashes.", numChunkPairsToAdd * 2, dbu.dataHashes.size(false));
//
//            for (BigHash h : hashes) {
//                assertTrue("Better have data chunk.", dbu.hasData(h));
//                assertTrue("Better have meta chunk.", dbu.hasMetaData(h));
//            }
//
//            System.out.println("8. Done verifying all chunks still there after adding more.");
//
//            // Add second directory.
//            dbu.add(ddc3);
//
//            // Now grab a handful of random data blocks. Greater than 256 guarentees
//            // that some will be data/02 and some data/02/02
//            movedCount = 0;
//
//            hashesIt = hashes.iterator();
//            while (movedCount < dataBlocksToMove && hashesIt.hasNext()) {
//                DataBlock db = dbu.getDataBlockToAddChunk(hashesIt.next());
//                boolean moved = db.moveToDataDirectoryConfiguration(ddc3);
//
//                // Failed, perhaps already moved!
//                if (moved) {
//                    movedCount++;
//                }
//            }
//
//            System.out.println("9. Done moving again");
//
////            System.out.println();
////            System.out.println("////////////////////////////////////////////////////////////////////");
////            System.out.println("//  Done moving " + dataBlocksToMove + ", here's the structure:   ");
////            System.out.println("////////////////////////////////////////////////////////////////////");
////            System.out.println();
////            Text.printRecursiveDirectoryStructure(data1);
////            Text.printRecursiveDirectoryStructure(data2);
////            Text.printRecursiveDirectoryStructure(data3);
//
//            for (BigHash h : hashes) {
//                assertTrue("Better have data chunk.", dbu.hasData(h));
//                assertTrue("Better have meta chunk.", dbu.hasMetaData(h));
//            }
//        } finally {
//            dbu.close();
//            IOUtil.recursiveDeleteWithWarning(data1);
//            IOUtil.recursiveDeleteWithWarning(data2);
//            IOUtil.recursiveDeleteWithWarning(data3);
//        }
//    }

    public void testDeleteDataChunksReturnsCorrectHashes() throws Exception {
        TestUtil.printTitle("DataBlockTest:testDeleteDataChunksReturnsCorrectHashes()");
        testDeleteChunksReturnsCorrectHashes(false);
    }

    public void testDeleteMetaDataChunksReturnsCorrectHashes() throws Exception {
        TestUtil.printTitle("DataBlockTest:testDeleteMetaDataChunksReturnsCorrectHashes()");
        testDeleteChunksReturnsCorrectHashes(true);
    }

    /**
     * <p>Emulating a server that deletes a lot of its data, perhaps because doesn't have hash span. Want to make sure it reports correct hashes it has.</p>
     * @param isMetaData
     * @throws java.lang.Exception
     */
    private void testDeleteChunksReturnsCorrectHashes(boolean isMetaData) throws Exception {

        // Keep these numbers even, or test will fail!
        int hashesToUse = 226;
        int hashesToBufferInMemory = 50;

        FlatFileTrancheServer ffts = null;
        File dataDir = null;
        try {
            dataDir = TempFileUtil.createTemporaryDirectory("testDeleteChunksReturnsCorrectHashesFor" + (isMetaData ? "MetaData" : "Data"));
            ffts = new FlatFileTrancheServer(dataDir);

            ffts.getConfiguration().getUsers().add(DevUtil.getDevUser());

            DataBlockUtil dbu = ffts.getDataBlockUtil();
            dbu.dataHashes.setTestBufferSize(hashesToBufferInMemory);
            dbu.metaDataHashes.setTestBufferSize(hashesToBufferInMemory);

            List<BigHash> hashesAdded = new ArrayList();
            List<BigHash> hashesKept = new ArrayList();

            // Step 1: add a bunch of hashes
            for (int count = 0; count < hashesToUse; count++) {
                if (!isMetaData) {
                    byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
                    BigHash hash = new BigHash(chunk);

                    if (hashesAdded.contains(hash)) {
                        count--;
                        continue;
                    }

                    IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                    hashesAdded.add(hash);
                    hashesKept.add(hash);
                } else {
                    byte[] chunk = DevUtil.createRandomMetaDataChunk();
                    BigHash hash = DevUtil.getRandomBigHash();

                    if (hashesAdded.contains(hash)) {
                        count--;
                        continue;
                    }

                    IOUtil.setMetaData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                    hashesAdded.add(hash);
                    hashesKept.add(hash);
                }
            }

            // Verify hashes are there
            assertEquals("Should have added correct number of hashes.", hashesToUse, hashesAdded.size());
            for (BigHash h : hashesAdded) {
                if (!isMetaData) {
                    assertTrue("Should have data chunk.", IOUtil.hasData(ffts, h));
                } else {
                    assertTrue("Should have meta data chunk.", IOUtil.hasMetaData(ffts, h));
                }
            }

            // Verify reports hashes
            {
                BigHash[] hashesReported = null;
                // Ask for fifty extra hashes to make sure not adding anything, etc.
                if (!isMetaData) {
                    hashesReported = ffts.getDataHashes(BigInteger.ZERO, BigInteger.valueOf(hashesToUse + 50));
                } else {
                    hashesReported = ffts.getMetaDataHashes(BigInteger.ZERO, BigInteger.valueOf(hashesToUse + 50));
                }

                assertEquals("Expecting server to report correct number of hashes.", hashesToUse, hashesReported.length);
            }

            // Step 2: delete every other hash
            boolean delete = true;
            for (BigHash h : hashesAdded) {
                // Flip deletion boolean
                delete = !delete;
                if (!delete) {
                    continue;
                }

                if (!isMetaData) {
                    dbu.deleteData(h, "testing");
                } else {
                    dbu.deleteMetaData(h, "testing");
                }

                hashesKept.remove(h);
            }

            // Multiple the hashes kept by two
            assertEquals("Expecting exactly half hashes deleted.", hashesAdded.size(), hashesKept.size() * 2);

            if (!isMetaData) {
                assertEquals("Expecting a certain number of hashes.", hashesKept.size(), dbu.getDataHashes(0, hashesToUse + 50).length);
            } else {
                assertEquals("Expecting a certain number of hashes.", hashesKept.size(), dbu.getMetaDataHashes(0, hashesToUse + 50).length);
            }

            // Verify the hashes
            for (BigHash h : hashesAdded) {
                if (hashesKept.contains(h)) {
                    if (!isMetaData) {
                        assertTrue("Should have data chunk still.", IOUtil.hasData(ffts, h));
                    } else {
                        assertTrue("Should have meta data chunk still.", IOUtil.hasMetaData(ffts, h));
                    }
                } else {
                    if (!isMetaData) {
                        assertFalse("Should not have data chunk any longer.", IOUtil.hasData(ffts, h));
                    } else {
                        assertFalse("Should not have meta data chunk any longer.", IOUtil.hasMetaData(ffts, h));
                    }
                }
            }

        } finally {
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDelete(dataDir);
        }
    }

    /**
     * <p>There was a bug: anything that was balanced no longer "seen" by data block util. Need to fix.</p>
     * @throws java.lang.Exception
     */
    public void testBalancedDataBlocksCanStillBeFound() throws Exception {
        TestUtil.printTitle("DataBlockTest:testBalancedDataBlocksCanStillBeFound()");

        FlatFileTrancheServer ffts = null;
        File dataDir1 = null, dataDir2 = null;

        final boolean wasTestingHashSpanFixingThread = TestUtil.isTestingHashSpanFixingThread();
        final boolean wasTesting = TestUtil.isTesting();

        try {
            TestUtil.setTesting(true);
            TestUtil.setTestingHashSpanFixingThread(false);

            dataDir1 = TempFileUtil.createTemporaryDirectory("testBalancedDataBlocksCanStillBeFound-1");
            dataDir2 = TempFileUtil.createTemporaryDirectory("testBalancedDataBlocksCanStillBeFound-2");

            // Start with first dir. After a while, add second.
            ffts = new FlatFileTrancheServer(dataDir1);
            // Shut off configuration caching for test
            ffts.setConfigurationCacheTime(0);

            ffts.getConfiguration().getUsers().add(DevUtil.getDevUser());

            assertEquals("Expecting flat file server to start with 1 DDC.", 1, ffts.getConfiguration().getDataDirectories().size());

            // Let's set the ddc to be 6MB
            long maxSize = 6 * 1024 * 1024;
            DataDirectoryConfiguration ddc1 = ffts.getConfiguration().getDataDirectories().toArray(new DataDirectoryConfiguration[0])[0];
            ddc1.setSizeLimit(maxSize);

            Set<BigHash> dataChunks = new HashSet();
            Set<BigHash> metaDataChunks = new HashSet();

            // Let's add five data chunks and five meta data chunks
            DATA:
            for (int i = 0; i < 5; i++) {
                byte[] dataChunk = DevUtil.createRandomDataChunk1MB();
                BigHash dataHash = new BigHash(dataChunk);

                assertEquals("Expecting data chunk to be exactly 1MB", 1024 * 1024, dataChunk.length);

                if (dataChunks.contains(dataHash)) {
                    i--;
                    continue DATA;
                }

                ffts.getDataBlockUtil().addData(dataHash, dataChunk);
                dataChunks.add(dataHash);
            }
            META_DATA:
            for (int i = 0; i < 5; i++) {
                byte[] metaDataChunk = DevUtil.createRandomMetaDataChunk();
                BigHash metaDataHash = DevUtil.getRandomBigHash();

                // Keep going until get new big hash. So unlikely this will happen.
                while (metaDataChunks.contains(metaDataHash)) {
                    metaDataHash = DevUtil.getRandomBigHash();
                }

                ffts.getDataBlockUtil().addMetaData(metaDataHash, metaDataChunk);
                metaDataChunks.add(metaDataHash);
            }

            // Make sure it has all the data
            for (BigHash hash : dataChunks) {
                assertTrue("Better have data; just added it", IOUtil.hasData(ffts, hash));
            }
            for (BigHash hash : metaDataChunks) {
                assertTrue("Better have meta data; just added it", IOUtil.hasMetaData(ffts, hash));
            }

            // Add second data directory
            DataDirectoryConfiguration ddc2 = new DataDirectoryConfiguration(dataDir2.getAbsolutePath(), maxSize);
            ffts.getConfiguration().getDataDirectories().add(ddc2);

            assertEquals("Should have two data directory configurations now.", 2, ffts.getConfiguration().getDataDirectories().size());

            for (DataDirectoryConfiguration ddc : ffts.getConfiguration().getDataDirectories()) {
                assertEquals("Each ddc should have the same size limit.", maxSize, ddc.getSizeLimit());
            }

            // Configure so balances
            ffts.getConfiguration().setValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_BALANCE, String.valueOf(true));

            // To get the configuration to "hook" in DataBlockUtil, must actually set. This is a trick, but
            // always happens in real environment
            Configuration config = ffts.getConfiguration();
            IOUtil.setConfiguration(ffts, config, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            // Give time to load projects
            long timeToSleep = 1000;
            System.out.println("Sleeping: " + timeToSleep);
            Thread.sleep(timeToSleep);

            // Should be able to find data
            for (BigHash hash : dataChunks) {
                assertTrue("Better have data; added second ddc, but not balanced yet", IOUtil.hasData(ffts, hash));
//                byte[] bytes = (byte[]) IOUtil.getData(ffts, hash, false).getReturnValueObject();
                // get the bytes
                Object o = IOUtil.getData(ffts, hash, false).getReturnValueObject();
                
                byte[] data = null;
                
                if (o instanceof byte[]) {
                    data = (byte[])o;
                } else if (o instanceof byte[][]) {
                    data = ((byte[][])o)[0];
                } else {
                    fail("Expected return object to be type byte[] or byte[][], but wasn't.");
                }
                assertNotNull(data);
            }
            for (BigHash hash : metaDataChunks) {
                assertTrue("Better have meta data; added second ddc, but not balanced yet", IOUtil.hasMetaData(ffts, hash));
                byte[] bytes = (byte[]) IOUtil.getMetaData(ffts, hash, false).getReturnValueObject();
                assertNotNull(bytes);
            }

            // 
            ffts.getDataBlockUtil().setMinSizeAvailableInTargetDataBlockBeforeBalance(1024 * 1024);

            // Coerce some balancing
            for (int i = 0; i < 2; i++) {
                boolean wasBalanced = ffts.getDataBlockUtil().balanceDataDirectoryConfigurations();
//                assertTrue("Expecting that some balancing took place.", wasBalanced);
                if (!wasBalanced) {
                    Thread.sleep(250);
                }
            }

            // Make sure both ddcs have some data
            for (DataDirectoryConfiguration ddc : ffts.getConfiguration().getDataDirectories()) {
//                assertTrue("Expecting each ddc to have at least some data, but ddc doesn't have any: " + ddc.getDirectory(), 0 < ddc.getActualSize());
                System.out.println("After balancing, " + ddc.getDirectory() + ": " + ddc.getActualSize());
            }

            // Should be able to find data
            for (BigHash hash : dataChunks) {
                assertTrue("Better have data; just balanced, should still be available", IOUtil.hasData(ffts, hash));
                byte[] bytes = (byte[]) IOUtil.getData(ffts, hash, false).getReturnValueObject();
                assertNotNull(bytes);
            }
            for (BigHash hash : metaDataChunks) {
                assertTrue("Better have meta data; just balanced, should still be available", IOUtil.hasMetaData(ffts, hash));
                byte[] bytes = (byte[]) IOUtil.getMetaData(ffts, hash, false).getReturnValueObject();
                assertNotNull(bytes);
            }

        } finally {
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDelete(dataDir1);
            IOUtil.recursiveDelete(dataDir2);

            TestUtil.setTesting(wasTesting);
            TestUtil.setTestingHashSpanFixingThread(wasTestingHashSpanFixingThread);
        }
    }
}
