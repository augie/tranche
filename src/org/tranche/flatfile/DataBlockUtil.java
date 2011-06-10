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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import org.tranche.annotations.Fix;
import org.tranche.configuration.ConfigKeys;
import org.tranche.commons.DebugUtil;
import org.tranche.commons.TextUtil;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.exceptions.UnexpectedEndOfDataBlockException;
import org.tranche.flatfile.logs.DataBlockUtilLogger;
import org.tranche.hash.Base16;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;
import org.tranche.hash.DiskBackedBigHashSet;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataUtil;
import org.tranche.time.TimeUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>Stores config information in memory, and holds lists of available hashes and resources on disk.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
@Fix(problem = "Corrupted DataBlock files, almost certainly due to server process interrupted rudely. These represented dead ends in b-tree, as they would always fail when adding or getting chunks.", solution = "Detect corrupted datablock in DataBlock.fillBytes, and throw as UnexpectedEndOfDataBlockException. Selectly catch and add to DataBlockUtil.repairCorruptedDataBlock, which will salvage.", day = 8, month = 8, year = 2008, author = "Bryan Smith")
public class DataBlockUtil {

    public static final boolean DEFAULT_STORE_DATA_BLOCK_REFERENCES = false;
    /**
     * <p>When moving, data block must have this much free space before a move is considered.</p>
     */
//    private int minSizeAvailableInTargetDataBlockBeforeBalance = DEFAULT_MIN_SIZE_AVAILABLE_IN_TARGET_DATABLOCK_BEFORE_BALANCE;
    /**
     * <p>The default value of whether the DataBlock's cache should be used.</p>
     */
    public static final boolean DEFAULT_USE_CACHE = false;
    /**
     * <p>If set to true, will use cache to (hopefully) speed up operations.</p>
     */
    private boolean isUseCache = DEFAULT_USE_CACHE;
    /**
     * <p>If set to true, turns on tracers for balancing.</p>
     */
    private static final boolean isDebugBalancing = false;
    /**
     * <p>If set to true, logs aggregate data about DataBlocks.</p>
     */
    private static boolean isLogging = false;
    
    public static int DEFAULT_MAX_CHUNK_SIZE = 1024 * 1024;
    private static int maxChunkSize = DEFAULT_MAX_CHUNK_SIZE;

    /**
     * <p>The default file chunk.</p>
     * @deprecated Use getMaxChunkSize()
     */
    private static int ONE_MB = DEFAULT_MAX_CHUNK_SIZE;    // Keep track of how many data blocks were successfully merged
    private long successMergeDataBlock = 0;    // Keep track of how many data blocks failed to merge
    private long failedMergeDataBlock = 0;    // Keep track of total. Might be different than sum of success and fail if error in finally block
    private long totalMergeDataBlock = 0;    // 
    private long corruptedDataBlockCount = 0;    // 
    private long salvagedChunksFromCorruptedDataBlockCount = 0;    // 
    private long downloadedChunksFromCorruptedDataBlockCount = 0;    // 
    private long lostChunksFromCorruptedDataBlockCount = 0;    //
    private long corruptedDataBlockHeaderCount = 0;    // 
    private long corruptedDataBlockBodyCount = 0;    // keep track of what directories can be used
    private ArrayList<DataDirectoryConfiguration> ddcs = new ArrayList();
    /**
     * <p>The disk-backed list of data chunk hashes. Used when client requesting data chunks.</p>
     */
    public final DiskBackedBigHashSet dataHashes = new DiskBackedBigHashSet();
    /**
     * <p>The disk-backed list of meta data chunk hashes. Used when client requesting meta data chunks.</p>
     */
    public final DiskBackedBigHashSet metaDataHashes = new DiskBackedBigHashSet();    // keep a tree of data blocks
    private final DataBlock[] dataBlocks = new DataBlock[256];
    /**
     * <p>Variable to simulate serveral test conditions: if true, this will cause the code to fail when it makes the .backup file.</p>
     */
    boolean purposelyFailCleanUp = false;
    /**
     * <p>Variable to simulate serveral test conditions: the number of entries at which the merge operation should purposely fail.</p>
     */
    int purposelyFailMerge = Integer.MAX_VALUE;
    /**
     * <p>Queue for merging blocks -- the are handled by a background thread in FFTS.</p>
     */
    ArrayBlockingQueue<DataBlockToMerge> mergeQueue = new ArrayBlockingQueue(10000);
    private final ReplaceCorruptedDataBlockThread replaceCorruptedDataBlocksThread;
    private final FlatFileTrancheServer ffts;
    private final DataBlockCache cache;
    /**
     * <p>Total number of data blocks moved when balancing.</p>
     */
    private long dataBlocksBalanced = 0;
    /**
     * 
     */
    public DataBlockUtil() {
        this(null);
    }

    /**
     * 
     * @param ffts
     */
    public DataBlockUtil(FlatFileTrancheServer ffts) {

        this.ffts = ffts;

        // Fire off thread that helps repair corrupted data blocks
        this.replaceCorruptedDataBlocksThread = new ReplaceCorruptedDataBlockThread(DataBlockUtil.this);
        this.replaceCorruptedDataBlocksThread.start();

        // Make sure can get data/meta data hashes when need them
        this.dataHashes.setAutoWriteBeforeCriticalOperation(true);
        this.metaDataHashes.setAutoWriteBeforeCriticalOperation(true);

        this.cache = new DataBlockCache();
    }

    /**
     * <p>Close off any resources.</p>
     */
    public void close() {
        this.replaceCorruptedDataBlocksThread.setStop(true);

        try {
            if (this.dataDeletionWriter != null) {
                synchronized (this.dataDeletionWriter) {
                    this.dataDeletionWriter.flush();
                    this.dataDeletionWriter.close();
                }
            }
        } catch (Exception e) { /* nope */ }

        try {
            if (this.metaDataDeletionWriter != null) {
                synchronized (this.metaDataDeletionWriter) {
                    this.metaDataDeletionWriter.flush();
                    this.metaDataDeletionWriter.close();
                }
            }
        } catch (Exception e) { /* nope */ }
        try {
            if (this.corruptedDataBlockWriter != null) {
                synchronized (this.corruptedDataBlockWriter) {
                    this.corruptedDataBlockWriter.flush();
                    this.corruptedDataBlockWriter.close();
                }
            }
        } catch (Exception e) { /* nope */ }
    }

    /**
     * <p>Waits for any tasks that must be completed to ensure all DataBlock's on filesystem are in an expected state.</p>
     * <p>This includes any task such as repaired broken DataBlock's, which can take a while.</p>
     * @param maxTimeToWaitMillis The maximum amount of time to wait before returning
     * @throws java.lang.InterruptedException
     */
    public void waitUntilFinished(final long maxTimeToWaitMillis) throws InterruptedException {
        Thread t = new Thread("DataBlockUtil.waitUtilFinished thread, started at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp())) {

            @Override()
            public void run() {
                /*
                 * Add anything here that should block when waiting before closing down thread.
                 * Don't worry how long they block; this thread will be interrupted when time
                 *   is up.
                 */
                try {
                    replaceCorruptedDataBlocksThread.waitForQueueToEmpty(maxTimeToWaitMillis);
                } catch (InterruptedException ex) { /* Nope, time to stop */ }
            }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        t.join(maxTimeToWaitMillis);
        t.interrupt();
    }

    /**
     * <p>Add a DataDirectoryConfiguration object to the DataBlockUtil so that its directory can be used by the server.</p>
     * @param ddc
     */
    public final synchronized void add(DataDirectoryConfiguration ddc) {
        add(ddc, "No reason was offered.");
    }

    /**
     * <p>Add a DataDirectoryConfiguration object to the DataBlockUtil so that its directory can be used by the server.</p>
     * @param ddc
     * @param reason A brief description of why was called
     */
    public final synchronized void add(DataDirectoryConfiguration ddc, String reason) {
        // add the config
        if (!ddcs.contains(ddc)) {
            // set the appropriate parent DataBlockUtil reference. not static so that more than one can be in the same JVM
            ddc.dbu = this;
            ddcs.add(ddc);
            DebugUtil.debugOut(DataBlockUtil.class, "Add data directory configuration: " + ddc.getDirectory() + " <reason: " + reason + ">");
        } else {
            DebugUtil.debugOut(DataBlockUtil.class, "Ha-ha! Tried to a DDC that I already had!: " + ddc.getDirectory() + " <reason: " + reason + ">");
        }
    }

    /**
     * <p>Returns a copy of all the DataDirectoryConfiguration objs in memory. Modifying this collection does not affect the DataBlockUtil.</p>
     * @return
     */
    public final synchronized Set getDataDirectoryConfigurations() {
        Set<DataDirectoryConfiguration> dirs = new HashSet();
        dirs.addAll(ddcs);
        return dirs;
    }

    /**
     * <p>Remove a DataDirectoryConfiguration from the server's DataBlockUtil, along w/ associated hashes. Very expensive operation.</p>
     * @param ddc
     * @return False if nothing done.
     * @throws java.lang.Exception
     */
    public final synchronized boolean remove(DataDirectoryConfiguration ddc) throws Exception {
//        Set<DataDirectoryConfiguration> singleItemSet = new HashSet();
//        singleItemSet.add(ddc);
//        removeAll(singleItemSet);
        return false;
    }

    /**
     * <p>Removes set of DataDirectoryConfiguration objects from server's DataBlockUtil, along w/ associated hashes. Very expensive operation.</p>
     * @param configsToRemove
     * @return
     * @throws java.lang.Exception
     */
    public final synchronized boolean removeAll(Set<DataDirectoryConfiguration> configsToRemove) throws Exception {        // For now, does nothing...
//        // Users will be blocked for a while while removing data blocks.
//        synchronized(ddcs) {
//
//            // Remove from list of configs
//            ddcs.removeAll(configsToRemove);
//
//            DataBlock next;
//            Iterator<DataBlock> it = dataBlocks.iterator();
//            while(it.hasNext()) {
//                next = it.next();
//                if (configsToRemove.contains(next.ddc)) {
//                    removeHashesAssociatedWithDatablock(next);
//                    it.remove();
//                }
//            } // Removing data blocks
//        }
        return false;
    }

    /**
     * <p>Helper method to get the file/dir that matches the given hash.</p>
     * @param hash
     * @return
     */
    protected final DataBlock getDataBlockToAddChunk(BigHash hash) {
        // get the bytes of the hash
        byte[] hashBytes = hash.toByteArray();

        // make a string
        StringBuffer sb = new StringBuffer();

        // go byte by byte until you find an appropriate file
        DataBlock[] blocks = this.dataBlocks;
        for (int i = 0; i < hashBytes.length; i++) {
            // append to the string
            sb.append(File.separator + Base16.encode(new byte[]{hashBytes[i]}));

            // get the index
            final int offset = 0xff & hashBytes[i];
            // get the right block
            DataBlock match = blocks[offset];

            // if null, return null
            if (match == null) {
                // return a new block
                match = getDataBlock(sb.toString());
            }

            // if it is a directory and not merging, step down the tree and look for another match
            if (match.isDirectory()) {
                // update the blocks reference
                blocks = match.getSubBlocks();
                // get the next set
                continue;
            }

            // if it is a data containing file, return the block
            return match;
        }
        throw new RuntimeException("Too many data blocks on the server!? Should never be here.");
    }

    /**
     * <p>Finds or creates the appropriate data block for a particular hash.</p>
     * @param bh
     * @param isMetaData
     * @return
     * @throws java.lang.Exception
     */
    protected final DataBlock getDataBlockToGetChunk(BigHash bh, boolean isMetaData) throws Exception {
        // get the bytes of the hash
        byte[] hashBytes = bh.toByteArray();

        // make a string
        StringBuffer sb = new StringBuffer();

        // go byte by byte until you find an appropriate file
        DataBlock[] blocks = this.dataBlocks;
        for (int i = 0; i < hashBytes.length; i++) {
            // append to the string
            sb.append(File.separator + Base16.encode(new byte[]{hashBytes[i]}));

            // get the index
            final int offset = 0xff & hashBytes[i];
            // get the right block
            DataBlock match = blocks[offset];

            // if null, return null
            if (match == null) {
                // return a new block
                match = getDataBlock(sb.toString());
            }

            // if it is a directory and not merging, step down the tree and look for another match
            if (match.isDirectory()) {
                // if merging, check for the bytes before moving down a node
                if (match.isMerging() && match.hasBytes(bh, isMetaData)) {
                    // check if the blocks match
                    return match;
                }
                // update the blocks reference
                blocks = match.getSubBlocks();
                // get the next set
                continue;
            }

            // check if the block has the data
            return match;
        }
        throw new RuntimeException("Too many data blocks on the server!? Should never be here.");
    }

    /**
     * <p>Internally loads data blocks, and is the only synchronization point required because it modifies the tree of DataBlocks.</p>
     * @param test
     * @return
     */
    final synchronized DataBlock getDataBlock(String test) {
        // get the block
        DataBlock[] blocks = dataBlocks;
        DataBlock toReturn = null;
        // keep track of the overall name
        StringBuffer buildName = new StringBuffer();
        // step through
        for (int i = 1; i < test.length(); i += 3) {
            // Assert that array of blocks is not null. Helps check logic to 
            // not invoke DataBlock.getSubBlocks unnecessarily
            if (blocks == null) {
                throw new AssertionFailedException("DataBlock[] blocks is null; i=" + i + "; test=" + test);
            }

            // get subset of string
            StringBuffer sb = new StringBuffer();
            sb.append(test.charAt(i));
            sb.append(test.charAt(i + 1));
            // append to the overall name
            buildName.append(File.separator + sb.toString());
            // switch to number
            int offset = 0xff & Base16.decode(sb.toString())[0];
            // set the reference
            toReturn = blocks[offset];
            // update the blocks
            if (toReturn == null && i <= test.length() - 2) {
                // search for existing blocks
                DataBlock findOrCreate = searchForExistingDataBlock(buildName.toString());
                if (isStoreDataBlockReferences()) {
                    blocks[offset] = findOrCreate; // Only stored if variable set
                }
                // set the block reference
                toReturn = findOrCreate;
            }
            // DataBlock.getSubBlocks lazily creates the DataBlock array
            // so let's not call it if we don't have to. Leads to premature
            // creation of subblocks, which has (reliably) resulted in OutOfMemoryError
            // in stress tests
            if (i + 3 < test.length()) {
                // update the blocks
                blocks = toReturn.getSubBlocks();
            } else {
                // This is used to assert there is no programmer error
                blocks = null;
            }
        }

        // return the loaded data block
        return toReturn;
    }

    /**
     * <p>Searchs for any DataBlock belonging to a particular DDC, and returns first found.</p>
     * @param ddc
     * @return DataBlock residing in a DDC
     */
    private final DataBlock getDataBlockForDataDirectoryConfiguration(DataDirectoryConfiguration ddc) {

        final long start = TimeUtil.getTrancheTimestamp();
        boolean found = false;
        try {

            List<DataBlock> dataBlocksToSearch = new ArrayList();

            // NOTE: this will do nothing when not storing references to DataBlocks in
            //       this.dataBlocks as currently implemented.
            //
            //       Either:
            //         - Reimplement to work when isStoreDataBlockReferences is false
            //         - DataBlock balancing off when isStoreDataBlockReferences is false
            for (DataBlock db : this.dataBlocks) {
                dataBlocksToSearch.add(db);
            }

            // Depth first search through b-tree
            while (dataBlocksToSearch.size() > 0) {
                DataBlock db = dataBlocksToSearch.remove(dataBlocksToSearch.size() - 1);

                // This could be a synchronization issue or a bug, but might return null.
                if (db == null) {
                    continue;
                }
                if (db.isDirectory()) {
                    for (DataBlock subDb : db.getSubBlocks()) {
                        dataBlocksToSearch.add(subDb);
                    }
                } else {
                    if (db.ddc.equals(ddc)) {
                        found = true;
                        return db;
                    }
                }
            }

            // Did not find a suitable DataBlock
            return null;

        } finally {
            printTracerBalancing("Time to find a DataBlock<found=" + found + "> for DDC<" + ddc.getDirectoryFile().getAbsolutePath() + ">: " + TextUtil.formatTimeLength(TimeUtil.getTrancheTimestamp() - start));
        }


    }

    /**
     * <p>Internal method for finding a DataBlock for the particular test string representing the path of the data block.</p>
     * @param test
     * @return
     */
    private final DataBlock searchForExistingDataBlock(String test) {

        DebugUtil.debugOut(DataBlockUtil.class, "Searching for data block<" + test + ">: " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));

        // find the directory with the most space
        ArrayList<File> existingFiles = new ArrayList();
        ArrayList<File> existingDirectories = new ArrayList();
        // the configurations
        ArrayList<DataDirectoryConfiguration> existingFilesDDCs = new ArrayList();
        ArrayList<DataDirectoryConfiguration> existingDirectoriesDDCs = new ArrayList();
        // the directory with the most space
        DataDirectoryConfiguration mostSpace = this.ddcs.get(0);

        int iteration = 0;
        for (DataDirectoryConfiguration ddc : ddcs) {

            DebugUtil.debugOut(DataBlockUtil.class, "Checking DDC: " + ddc.getDirectory() + ", iteration: " + iteration);
            iteration++;

            // first check if this ddc already has the block
            File checkFile = new File(ddc.getDirectoryFile(), test);
            if (checkFile.exists()) {
                // buffer the existing file
                if (checkFile.isFile()) {
                    existingFiles.add(checkFile);
                    existingFilesDDCs.add(ddc);
                    DebugUtil.debugOut(DataBlockUtil.class, "  - Found regular file: " + checkFile.getAbsolutePath() + ", iteration: " + iteration);
                } else {
                    existingDirectories.add(checkFile);
                    existingDirectoriesDDCs.add(ddc);
                    DebugUtil.debugOut(DataBlockUtil.class, "  - Found directory: " + checkFile.getAbsolutePath() + ", iteration: " + iteration);
                }
            }

            long ddcRemainingSize = ddc.getSizeLimit() - ddc.getActualSize();
            long mostRemainingSize = mostSpace.getSizeLimit() - mostSpace.getActualSize();

            // check if more space exists in this ddc
            if (ddcRemainingSize > mostRemainingSize) {
                mostSpace = ddc;
            }
        }

        DebugUtil.debugOut(DataBlockUtil.class, "Total exiting files <" + existingFiles.size() + ">, total existing directories <" + existingDirectories.size() + ">, test <" + test + ">");

        // start with a null data block
        DataBlock block = null;

        // if it is only one, use it as the block
        if (existingFiles.size() <= 1 && existingDirectories.size() == 0) {
            // get the only directory
            if (existingFilesDDCs.size() > 0) {
                mostSpace = existingFilesDDCs.get(0);
            }
            // make a block for the directory with the most space
            block = new DataBlock(test, mostSpace, DataBlockUtil.this);

            // return the new block
            return block;
        }

        // make the directory if needed
        if (existingDirectories.size() > 0) {
            // look for the directory with the merge file
            for (int i = 0; i < existingDirectories.size(); i++) {
                // get the only directory
                mostSpace = existingDirectoriesDDCs.get(i);
                // make a block for the directory with the most space
                block = new DataBlock(test, mostSpace, DataBlockUtil.this);
                // if merging, keep -- otherwise, just keep the last
                if (block.isMerging()) {
                    break;
                }
            }
        }

        // this is a check to make sure that the tree is in the expected shape, e.g. no duplicate leaf nodes
        // this should never normally happen! It is just a logical bit of code to prevent most any error in the b-tree
        if (existingFiles.size() > 0) {

            DebugUtil.debugOut(DataBlockUtil.class, "Uh-oh! Found " + existingFiles.size() + " existing file(s). Need to merge. Here are the files:");

            for (File existingFile : existingFiles) {
                DebugUtil.debugOut(DataBlockUtil.class, "  - " + existingFile.getAbsolutePath());
            }

            // keep track of the moved files
            ArrayList<File> filesToMerge = new ArrayList();
            // move all of the files to a different name
            for (File toMove : existingFiles) {
                // create the file handle to rename
                File newName = new File(toMove.getParent(), toMove.getName() + "." + TimeUtil.getTrancheTimestamp() + "-" + filesToMerge.size());
                boolean renameSuccessful = toMove.renameTo(newName);
                if (!renameSuccessful) {
                    throw new RuntimeException("Can't auto-fix file " + toMove + ". Check that appropriate permissions exist.");
                }
                // add the file
                filesToMerge.add(newName);
            }

            // make a new block
            if (block == null) {
                block = new DataBlock(test, mostSpace, DataBlockUtil.this);
            }

            // merge all of the renamed files
            for (File toMerge : filesToMerge) {
                try {
                    mergeOldDataBlock(toMerge);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }


        // fall back on making a new block
        // make a block for the directory with the most space
        if (block == null) {
            throw new RuntimeException("Can't find or make data block!? B-tree is in unknown state.");
        }

        // return the new block
        return block;
    }

    /**
     * <p>Synchronized access to setting data chunk bytes.</p>
     * @param nameHashToAdd
     * @param content
     * @throws java.lang.Exception
     */
    public final void addData(BigHash nameHashToAdd, byte[] content) throws Exception {

        // Started log for action
        if (isLogging) {
            getLogger().logSetStarted(nameHashToAdd);
        }

        try {
            // check if the blocks match
            DataBlock block = this.getDataBlockToAddChunk(nameHashToAdd);

            // Block with most available space should have space
            long available = block.ddc.getSizeLimit() - block.ddc.getActualSize();
            if (available - content.length < 0) {
                throw new IOException("Out of space: Cannot find a directory with available disk space for " + nameHashToAdd.toString().substring(0, 8) + "...");
            }

            // add the bytes
            block.addBytes(nameHashToAdd, false, content);

            // add to the disk-backed list
            dataHashes.add(nameHashToAdd);

            //
            if (isLogging) {
                getLogger().logSetBlockSucceed(nameHashToAdd);
            }

        } catch (Exception ex) {
            //
            if (isLogging) {
                getLogger().logSetBlockFailed(nameHashToAdd);
            }
            throw ex;
        }
    }

    /**
     * <p>Synchronized access to setting meta data chunk bytes.</p>
     * @param nameHashToAdd
     * @param content
     * @throws java.lang.Exception
     */
    public final void addMetaData(BigHash nameHashToAdd, byte[] content) throws Exception {

        // Started log for action
        if (isLogging) {
            getLogger().logSetStarted(nameHashToAdd);
        }

        try {
            // check if the blocks match
            DataBlock block = this.getDataBlockToAddChunk(nameHashToAdd);

            // Block with most available space should have space
            long available = block.ddc.getSizeLimit() - block.ddc.getActualSize();
            if (available - content.length < 0) {
                throw new IOException("Out of space: Cannot find a directory with available disk space for " + nameHashToAdd.toString().substring(0, 8) + "...");
            }

            // add the meta-data bytes
            block.addBytes(nameHashToAdd, true, content);

            // add to the list
            metaDataHashes.add(nameHashToAdd);

            //
            if (isLogging) {
                getLogger().logSetBlockSucceed(nameHashToAdd);
            }
        } catch (Exception ex) {
            //
            if (isLogging) {
                getLogger().logSetBlockFailed(nameHashToAdd);
            }
            throw ex;
        }
    }

    /**
     * <p>Get data chunk bytes based on hash.</p>
     * @param bh
     * @return
     * @throws java.lang.Exception
     */
    public final byte[] getData(BigHash bh) throws Exception {

        // Check to see whether using cache
        if (isUseCache()) {
            try {
                DataBlockCacheEntry e = getDataBlockCache().get(bh, false);
                if (e != null && e.dataBlock != null) {
                    byte[] data = e.dataBlock.getBytes(e.offset, e.size);

                    // Need to verify that data has not moved
                    if (new BigHash(data).equals(bh)) {
                        return data;
                    }
                }
            } catch (Exception ex) {
//                System.err.println(ex.getClass().getSimpleName() + " occurred while using cache to get data chunk: " + ex.getMessage());
            }
        }

        // Get the bytes from the data block
        return getDataBlockToGetChunk(bh, false).getBytes(bh, false);
    }

    /**
     * <p>Check whether have data chunk bytes exist based on hash.</p>
     * @param bh
     * @return
     * @throws java.lang.Exception
     */
    public final boolean hasData(BigHash bh) throws Exception {

        // Check to see whether using cache
        if (isUseCache()) {
            try {
                DataBlockCacheEntry e = getDataBlockCache().get(bh, false);
                // Don't check for existence -- if in cache, it has recently existed.
                // If deleted, it was removed, so exists by deduction.
                if (e != null && e.dataBlock != null) {
                    return true;
                }
            } catch (Exception ex) {
//                System.err.println(ex.getClass().getSimpleName() + " occurred while using cache to check if has data chunk: " + ex.getMessage());
            }
        }

        // check if the block has the data
        return getDataBlockToGetChunk(bh, false).hasBytes(bh, false);
    }

    /**
     * <p>Check whether meta data chunk bytes exist based on hash.</p>
     * @param bh
     * @return
     * @throws java.lang.Exception
     */
    public final boolean hasMetaData(BigHash bh) throws Exception {

        // Check to see whether using cache
        if (isUseCache()) {
            try {
                DataBlockCacheEntry e = getDataBlockCache().get(bh, true);
                // Don't check for existence -- if in cache, it has recently existed.
                // If deleted, it was removed, so exists by deduction.
                if (e != null && e.dataBlock != null) {
                    return true;
                }
            } catch (Exception ex) {
//                System.err.println(ex.getClass().getSimpleName() + " occurred while using cache to check if has meta chunk: " + ex.getMessage());
            }
        }

        // check if the block has the data
        return getDataBlockToGetChunk(bh, true).hasBytes(bh, true);
    }

    /**
     * <p>Get meta data chunk bytes based on hash.</p>
     * @param bh
     * @return
     * @throws java.lang.Exception
     */
    public final byte[] getMetaData(BigHash bh) throws Exception {

        // Check to see whether using cache
        if (isUseCache()) {
            try {
                DataBlockCacheEntry e = getDataBlockCache().get(bh, true);
                if (e != null && e.dataBlock != null) {
                    byte[] bytes = e.dataBlock.getBytes(e.offset, e.size);

                    // Need to verify that data has not moved
                    InputStream in = null;
                    try {
                        in = new ByteArrayInputStream(bytes);
                        MetaData md = MetaDataUtil.read(in);

                        if (md != null) {
                            return bytes;
                        }
                    } finally {
                        IOUtil.safeClose(in);
                    }
                }
            } catch (Exception ex) {
                //System.err.println(ex.getClass().getSimpleName() + " occurred while using cache to get meta chunk: " + ex.getMessage());
            }
        }

        // check if the block has the data
        return getDataBlockToGetChunk(bh, true).getBytes(bh, true);
    }

    /**
     * <p>Delete a data chunk based on hash.</p>
     * @param bh
     * @param desc A brief and arbitrary description of why deleting. This is logged. If not sure, just explain who the calling method is.
     * @throws java.lang.Exception
     */
    public final void deleteData(BigHash bh, String desc) throws Exception {

        getDataBlockToGetChunk(bh, false).deleteBytes(bh, false);

        // delete from disk-backed list of hashes
        dataHashes.delete(bh);
        // Clear cache. Might contain deleted entry.
        if (isUseCache()) {
            getDataBlockCache().remove(bh, false);
        }

        logDeletion(bh, desc, false);
    }

    /**
     * <p>Delete a meta data chunk based on hash.</p>
     * @param bh
     * @param desc A brief and arbitrary description of why deleting. This is logged. If not sure, just explain who the calling method is.
     * @throws java.lang.Exception
     */
    public final synchronized void deleteMetaData(BigHash bh, String desc) throws Exception {

        getDataBlockToGetChunk(bh, true).deleteBytes(bh, true);

        // delete from disk-backed list of hashes
        metaDataHashes.delete(bh);
        // Clear cache. Might contain deleted entry.
        if (isUseCache()) {
            getDataBlockCache().remove(bh, true);
        }

        logDeletion(bh, desc, true);
    }
    private BufferedWriter dataDeletionWriter = null, metaDataDeletionWriter = null, corruptedDataBlockWriter = null;

    /**
     * 
     * @param path
     * @param foundMsg
     */
    private void logCorruptedDataBlock(String path, String msg) {
        try {
            final String date = TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp());

            lazyLoadCorruptedDataBlockWriter();

            corruptedDataBlockWriter.write(date + ", \"" + path + "\", \"" + msg + "\"" + "\n");
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + " occurred while trying to log corrupted data block: " + e.getMessage() + "; " + "path: " + path + "; msg: " + msg);
            e.printStackTrace(System.err);
        }
    }

    /**
     * 
     * @param hash
     * @param isMetaData
     */
    private void logDeletion(final BigHash hash, final String reason, final boolean isMetaData) {

        try {
            final String date = TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp());

            if (!isMetaData) {

                if (!isLogDataChunkDeletions()) {
                    return;
                }

                lazyLoadDataDeletionWriter();

                synchronized (dataDeletionWriter) {
                    // Date, reason, hash
                    dataDeletionWriter.write(date + ", \"" + reason + "\", " + hash + "\n");
                }

            } else {

                if (!isLogMetaDataChunkDeletions()) {
                    return;
                }

                lazyLoadMetaDataDeletionWriter();

                synchronized (metaDataDeletionWriter) {
                    // Date, reason, hash
                    metaDataDeletionWriter.write(date + ", \"" + reason + "\", " + hash + "\n");
                }
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + " occurred while trying to log deletion: " + e.getMessage() + "; " + (isMetaData ? "meta data chunk" : "data chunk") + ": " + hash);
            e.printStackTrace(System.err);
        }
    }
    private static final Object lazyLoadLock = new Object();

    /**
     * <p>Create the log writer if not created already.</p>
     * @throws java.lang.Exception
     */
    private void lazyLoadDataDeletionWriter() throws Exception {
        synchronized (lazyLoadLock) {
            if (dataDeletionWriter != null) {
                return;
            }

            File logFile = getDataDeletionLog();

            // In append mode
            dataDeletionWriter = new BufferedWriter(new FileWriter(logFile, true));
        }
    }

    /**
     * <p>Get the data deletion log. Check for existance before reading; may not exist if not logging, or might exist even if not logging if logged in past.</p>
     * @return
     */
    public File getDataDeletionLog() {
        final String log = "deleted-data-chunks.log";
        if (this.ffts != null) {
            return new File(this.ffts.getHomeDirectory(), log);
        }

        return new File(log);
    }

    /**
     * <p>Create the log writer if not created already.</p>
     * @throws java.lang.Exception
     */
    private void lazyLoadMetaDataDeletionWriter() throws Exception {
        synchronized (lazyLoadLock) {
            if (metaDataDeletionWriter != null) {
                return;
            }

            File logFile = getMetaDataDeletionLog();

            // In append mode
            metaDataDeletionWriter = new BufferedWriter(new FileWriter(logFile, true));
        }
    }

    /**
     * <p>Create the log writer if not created already.</p>
     * @throws java.lang.Exception
     */
    private void lazyLoadCorruptedDataBlockWriter() throws Exception {
        synchronized (lazyLoadLock) {
            if (corruptedDataBlockWriter != null) {
                return;
            }

            File logFile = getCorruptedDataBlocksLog();

            // In append mode
            corruptedDataBlockWriter = new BufferedWriter(new FileWriter(logFile, true));
        }
    }

    /**
     * <p>Get the data deletion log. Check for existance before reading; may not exist if not logging, or might exist even if not logging if logged in past.</p>
     * @return
     */
    public File getCorruptedDataBlocksLog() {
        final String log = "corrupted-datablocks.log";
        if (this.ffts != null) {
            return new File(this.ffts.getHomeDirectory(), log);
        }
        return new File(log);
    }

    /**
     * <p>Get the meta data deletion log. Check for existance before reading; may not exist if not logging, or might exist even if not logging if logged in past.</p>
     * @return
     */
    public File getMetaDataDeletionLog() {
        final String log = "deleted-meta-data-chunks.log";
        if (this.ffts != null) {
            return new File(this.ffts.getHomeDirectory(), log);
        }
        return new File(log);
    }
    private boolean lastLogDataDeletions = ConfigKeys.DEFAULT_LOG_DATA_CHUNK_DELETIONS;

    /**
     * <p>Checks configuration to see whether should log data chunk deletions.</p>
     * @return
     */
    private boolean isLogDataChunkDeletions() {
        boolean shouldLog = ConfigKeys.DEFAULT_LOG_DATA_CHUNK_DELETIONS;

        try {
            shouldLog = Boolean.valueOf(this.ffts.getConfiguration().getValue(ConfigKeys.DATABLOCK_LOG_DATA_CHUNK_DELETIONS));
        } catch (Exception nope) { /* Skip -- either none or parse error */ }

        if (shouldLog != lastLogDataDeletions) {
            printNotice("Changing log data chunk delete from " + lastLogDataDeletions + " to " + shouldLog + " at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            lastLogDataDeletions = shouldLog;
        }

        return shouldLog;
    }
    private boolean lastLogMetaDataDeletions = ConfigKeys.DEFAULT_LOG_META_DATA_CHUNK_DELETIONS;

    /**
     * <p>Checks configuration to see whether should log data chunk deletions.</p>
     * @return
     */
    private boolean isLogMetaDataChunkDeletions() {
        boolean shouldLog = ConfigKeys.DEFAULT_LOG_META_DATA_CHUNK_DELETIONS;

        try {
            shouldLog = Boolean.valueOf(this.ffts.getConfiguration().getValue(ConfigKeys.DATABLOCK_LOG_META_DATA_CHUNK_DELETIONS));
        } catch (Exception nope) { /* Skip -- either none or parse error */ }

        if (shouldLog != lastLogMetaDataDeletions) {
            printNotice("Changing log data chunk delete from " + lastLogMetaDataDeletions + " to " + shouldLog + " at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            lastLogMetaDataDeletions = shouldLog;
        }

        return shouldLog;
    }

    /**
     * <p>Get the hashes associated with the data chunks on the server.</p>
     * @param offset
     * @param length
     * @return
     * @throws java.lang.Exception
     */
    public final synchronized BigHash[] getDataHashes(long offset, long length) throws Exception {
        // use the disk-backed list
        List<BigHash> hashes = dataHashes.get(offset, (int) length);
        return hashes.toArray(new BigHash[0]);
    }

    /**
     * <p>Get the hashes associated with the meta data chunks on the server.</p>
     * @param offset
     * @param length
     * @return
     * @throws java.lang.Exception
     */
    public final synchronized BigHash[] getMetaDataHashes(long offset, long length) throws Exception {
        // use the disk-backed list
        List<BigHash> hashes = metaDataHashes.get(offset, (int) length);
        return hashes.toArray(new BigHash[0]);
    }

    /**
     * <p>Merge the data block, including splitting the data block.</p>
     * @param fileToMerge
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    public final void mergeOldDataBlock(final File fileToMerge) throws FileNotFoundException, IOException, Exception {
        mergeOldDataBlock(fileToMerge, new byte[DataBlock.getBytesToRead()]);
    }

    /**
     * <p>Merge the data block, including splitting the data block.</p>
     * @param fileToMerge
     * @param buf
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    public final void mergeOldDataBlock(final File fileToMerge, final byte[] buf) throws FileNotFoundException, IOException, Exception {

        // Log merges
        if (isLogging) {
            getLogger().logMergeStart(fileToMerge);
        }

        // try to copy the data in to the new chunk(s)
        boolean successfullySplit = false;
        try {
            // split the block in to two by re-adding all of the hashes
            RandomAccessFile ras = new RandomAccessFile(fileToMerge, "r");
            try {
                // get the complete header
                DataBlock.fillWithBytes(buf, ras, fileToMerge.getAbsolutePath(), "Reading in headers to merge old data block.");
                // check for the hash
                for (int j = 0; j < DataBlock.getHeadersPerFile(); j++) {
                    // calc the offset
                    int offset = j * DataBlock.bytesPerEntry;
                    // parse the entry parts: hash, type, status, offset, size
                    BigHash h = BigHash.createFromBytes(buf, offset);
                    byte type = buf[offset + BigHash.HASH_LENGTH];
                    byte status = buf[offset + BigHash.HASH_LENGTH + 1];
                    int o = buf[BigHash.HASH_LENGTH + offset + 2] << 24 | (buf[BigHash.HASH_LENGTH + offset + 2 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 2 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 2 + 3] & 0xff);
                    int s = buf[BigHash.HASH_LENGTH + offset + 6] << 24 | (buf[BigHash.HASH_LENGTH + offset + 6 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 6 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 6 + 3] & 0xff);

                    // a purposeful failure check for the test case
                    if (purposelyFailMerge <= j) {
                        throw new Exception("Purposely failed during merge for testing.");
                    }

                    // add to the split blocks. skip if deleted, thus block splitted purges files
                    if (status == DataBlock.STATUS_DELETED) {
                        continue;                    // if at the end of the content, end
                    }
                    if (o == 0) {
                        break;                    // read the bytes
                    }
                    byte[] splitBuf = new byte[s];
                    ras.seek(o);
                    DataBlock.fillWithBytes(splitBuf, ras, fileToMerge.getAbsolutePath(), "Reading in " + (type == DataBlock.META_DATA ? "meta data" : "data") + " chunk to merge.");

                    // get the appropriate data block
                    DataBlock splitBlock = getDataBlockToAddChunk(h);
                    // add the data
                    splitBlock.addBytes(h, type == DataBlock.META_DATA, splitBuf);
                }
            } finally {
                IOUtil.safeClose(ras);

                if (isLogging) {
                    getLogger().logMergeFinish(fileToMerge);
                }
            }

            // flag as successfully split
            successfullySplit = true;
        } finally {
            totalMergeDataBlock++;
            // only delete the backup file if the split was a success
            if (successfullySplit) {

                boolean deletedMerge = fileToMerge.delete();
                if (!deletedMerge) {
                    throw new Exception("Can't remove merged file " + fileToMerge + ". Check file for delete permissions.");
                }

                // check if the directory is empty
                File parent = fileToMerge.getParentFile();
                if (parent.list().length == 0) {
                    parent.delete();
                }

                // Increment last. Can compare against total to see if errors occur during finally block.
                successMergeDataBlock++;
            } else {
                // Increment last. Can compare against total to see if errors occur during finally block.
                failedMergeDataBlock++;
            }
        }
    }

    /**
     * <p>If a data block is corrupted, will throw UnexpectedEndOfDataBlockException. This method attempts to repair the block by doing the following:</p>
     * <ol>
     *   <li>Backs file up to temporary file, and deletes corrupted data block. Server will be able to write data blocks again!</li>
     *   <li>Go on a salvaging mission. See which chunks (data or meta) are okay on the disk, and write them. Keep track of all data and meta data that is not in corrupted block.</li>
     *   <li>After done salvaging, start off thread to download chunks that are now missing.</li>
     * </ol>
     * @param forChunkWithHash Hash for chunk in the corrupted data block. (If catch an UnexpectedEndOfDataBlockException, the DataBlock is corrupted.)
     * @param maxTimeToWaitForQueueToFinish Time in milliseconds to wait for missing chunks to download. If cannot wait, specify 0, which will submit request and return. This basically a way to wait to alleviate server resources.
     * @param description
     * @throws java.lang.Exception
     * @return True if repaired, false otherwise. Won't be repaired if repairs turned off or already submitted.
     */
    public final synchronized boolean repairCorruptedDataBlockForChunk(BigHash forChunkWithHash, int maxTimeToWaitForQueueToFinish, final String description) throws Exception {
        DataBlock block = this.getDataBlockToAddChunk(forChunkWithHash);
        return repairCorruptedDataBlock(new File(block.getAbsolutePath()), maxTimeToWaitForQueueToFinish, description);
    }

    /**
     * <p>If a data block is corrupted, will throw UnexpectedEndOfDataBlockException. This method attempts to repair the block by doing the following:</p>
     * <ol>
     *   <li>Backs file up to temporary file, and deletes corrupted data block. Server will be able to write data blocks again!</li>
     *   <li>Go on a salvaging mission. See which chunks (data or meta) are okay on the disk, and write them. Keep track of all data and meta data that is not in corrupted block.</li>
     *   <li>After done salvaging, start off thread to download chunks that are now missing.</li>
     * </ol>
     * <p>Note that this method will wait up to five minutes for missing chunks to download before returning. If cannot wait, specify the maximum time to wait in the alternative signature method.</p>
     * @param dataBlockFile The path for the DataBlock's file. If not known, use a hash for a chunk in the DataBlock.
     * @param description
     * @throws java.lang.Exception
     * @return True if repaired, false otherwise. Won't be repaired if repairs turned off or already submitted.
     */
    public final synchronized boolean repairCorruptedDataBlock(final File dataBlockFile, final String description) throws Exception {
        return repairCorruptedDataBlock(dataBlockFile, 5 * 60 * 1000, description);
    }

    /**
     * <p>If a data block is corrupted, will throw UnexpectedEndOfDataBlockException. This method attempts to repair the block by doing the following:</p>
     * <ol>
     *   <li>Backs file up to temporary file, and deletes corrupted data block. Server will be able to write data blocks again!</li>
     *   <li>Go on a salvaging mission. See which chunks (data or meta) are okay on the disk, and write them. Keep track of all data and meta data that is not in corrupted block.</li>
     *   <li>After done salvaging, start off thread to download chunks that are now missing.</li>
     * </ol>
     * @param dataBlockFile The path for the DataBlock's file. If not known, use a hash for a chunk in the DataBlock.
     * @param maxTimeToWaitForQueueToFinish Time in milliseconds to wait for missing chunks to download. If cannot wait, specify 0, which will submit request and return. This basically a way to wait to alleviate server resources.
     * @param description
     * @throws java.lang.Exception
     * @return True if repaired, false otherwise. Won't be repaired if repairs turned off or already submitted.
     */
    public final synchronized boolean repairCorruptedDataBlock(final File dataBlockFile, final int maxTimeToWaitForQueueToFinish, final String description) throws Exception {

        // Check if skipped FIRST. hasBeenRepairedLately will modify the underlying
        // connections, so if out of order, won't be accurate!
        final boolean hasBeenSkipped = hasDataBlockBeenSkippedAlready(dataBlockFile);

        // Check to see if allowed. If not, exit immediately.
        if (!isAllowedToRepairCorruptedDataBlocks()) {

            // Increment count only if not skipped yet. Want an accurate count
            // of bad data blocks.
            if (!hasBeenSkipped) {
                this.incrementCorruptedDataBlockBodyCount();
            }
            return false;
        }

        // Don't check this if not allowed to repair. This modifies underlying connections,
        // so check AFTER returning if not allowed to repair
        final boolean hasBeenRepairedLately = hasDataBlockBeenRepairedLately(dataBlockFile);

        // Check to see whether repaired lately. If it has, don't repair again -- one of the threads
        // is filing a duplicate request on the same data block.
        if (hasBeenRepairedLately) {
            return false;
        }

        // Increment the count only if not skipped recently. This gives an accurate account
        // of total bad data blocks.
        if (!hasBeenRepairedLately && !hasBeenSkipped) {
            this.incrementCorruptedDataBlockCount();
        }

        String foundMsg = "Found corrupted data block <" + description + ">, going to attempt to repair.";
        System.err.println(foundMsg + ": " + dataBlockFile.getAbsolutePath());

        logCorruptedDataBlock(dataBlockFile.getAbsolutePath(), foundMsg);

        File tmpFile = null;
        try {
            // Get data block size so can make sure entirely copied.
            final long dataBlockSize = dataBlockFile.length();

            // Keep a reference to old data block path
            final String corruptedDataBlockFilePath = dataBlockFile.getAbsolutePath();

            // Find DataBlock so can synchronize while deleting.

            // Try to move to temporary file.
            tmpFile = TempFileUtil.createTempFileWithName(dataBlockFile.getName() + ".tmp");
            final boolean wasMoved = dataBlockFile.renameTo(tmpFile);

            // If file not moved due to system-dependent reasons, then simply copy over bytes.
            if (!wasMoved) {
                IOUtil.copyFile(dataBlockFile, tmpFile);
            }

            if (tmpFile.length() != dataBlockSize) {
                throw new Exception("Tried to copy data block file over to temp file, but failed. (Should copy " + dataBlockSize + " bytes, but copied " + tmpFile.length() + " bytes.)");
            }

            // Delete the file. Should synchronize on block.
            final File corruptedDataBlockFile = new File(corruptedDataBlockFilePath);
            final boolean wasDeleted = corruptedDataBlockFile.delete();

            // If not deleted and still exists, try to clobber the file.
            if (!wasDeleted && corruptedDataBlockFile.exists()) {
                FileOutputStream clobberOutputStream = null;
                try {
                    clobberOutputStream = new FileOutputStream(corruptedDataBlockFile, false);
                    byte[] nullBytes = new byte[0];
                    clobberOutputStream.write(nullBytes);
                    clobberOutputStream.flush();
                } finally {
                    IOUtil.safeClose(clobberOutputStream);
                }
            }

            // If file still exists with data, return it to where it was before and throw an exception.
            // We'll have to figure this out from the logs.
            if (corruptedDataBlockFile.exists() && corruptedDataBlockFile.length() > 0) {
                // Move temp file back.
                IOUtil.copyFile(tmpFile, corruptedDataBlockFile);

                // Throw exception so admin can take care of this.
                throw new Exception("Unable to remove corrupted data block, leaving it alone. Need to figure out why.");
            }

            // Open up the temp file. Going to read in the headers
            RandomAccessFile ras = new RandomAccessFile(tmpFile, "r");

            boolean wasHeaderCorrupted = false;
            boolean wasBodyCorrupted = false;
            int metaSalvaged = 0, dataSalvaged = 0;
            for (int i = 0; i < DataBlock.getHeadersPerFile(); i++) {
                int offset = i * DataBlock.bytesPerEntry;

                // If header fails, we are done
                byte[] nextHeader = new byte[DataBlock.bytesPerEntry];
                try {
                    ras.seek(offset);
                    int read = ras.read(nextHeader);

                    if (read != DataBlock.bytesPerEntry) {
                        throw new UnexpectedEndOfDataBlockException("End of header, must be corrupted in header.");
                    }
                } catch (UnexpectedEndOfDataBlockException ex) {
                    // Cannot do anything -- corrupted headers are hopeless.
                    // Healing thread should smooth out over time.
                    wasHeaderCorrupted = true;
                    // If header is corrupted, then the body is missing, not corrupted.
                    wasBodyCorrupted = false;
                    break;
                }

                // parse the entry parts: hash, type, status, offset, size
                BigHash chunkHash = BigHash.createFromBytes(nextHeader, 0);
                byte chunkType = nextHeader[BigHash.HASH_LENGTH];
                byte chunkStatus = nextHeader[BigHash.HASH_LENGTH + 1];
                int chunkOffset = nextHeader[BigHash.HASH_LENGTH + 2] << 24 | (nextHeader[BigHash.HASH_LENGTH + 2 + 1] & 0xff) << 16 | (nextHeader[BigHash.HASH_LENGTH + 2 + 2] & 0xff) << 8 | (nextHeader[BigHash.HASH_LENGTH + 2 + 3] & 0xff);
                int chunkSize = nextHeader[BigHash.HASH_LENGTH + 6] << 24 | (nextHeader[BigHash.HASH_LENGTH + 6 + 1] & 0xff) << 16 | (nextHeader[BigHash.HASH_LENGTH + 6 + 2] & 0xff) << 8 | (nextHeader[BigHash.HASH_LENGTH + 6 + 3] & 0xff);

                // At end, header not corrupted. No more entries.
                if (chunkOffset == 0) {
                    break;
                }

                // If not flagged as okay, skip it
                if (chunkStatus != DataBlock.STATUS_OK) {
                    continue;
                }

                try {
                    byte[] chunk = new byte[chunkSize];
                    ras.seek(chunkOffset);
                    int read = ras.read(chunk);

                    if (read != chunkSize) {
                        throw new UnexpectedEndOfDataBlockException("Failed to read chunk, must be corrupted in data portion of data block.");
                    }

                    // Add the chunk right through the DataBlockUtil.
                    if (chunkType == DataBlock.DATA) {
                        this.addData(chunkHash, chunk);
                        this.incrementSalvagedChunksFromCorruptedDataBlockCount();
                        dataSalvaged++;
                    } else if (chunkType == DataBlock.META_DATA) {
                        this.addMetaData(chunkHash, chunk);
                        this.incrementSalvagedChunksFromCorruptedDataBlockCount();
                        metaSalvaged++;
                    }

                } catch (UnexpectedEndOfDataBlockException ex) {
                    // Appears corruption is in the data area, not header.
                    // Add to list of chunks to download. Then continue
                    // to get rest of headers.
                    wasBodyCorrupted = true;
                    this.replaceCorruptedDataBlocksThread.addChunkToRetrieve(chunkHash, chunkType == DataBlock.META_DATA);
                }
            }

            if (wasHeaderCorrupted) {
                this.incrementCorruptedDataBlockHeaderCount();
            }
            if (wasBodyCorrupted) {
                this.incrementCorruptedDataBlockBodyCount();
            }

            String fixMsg = "Was corrupted in header: " + wasHeaderCorrupted + "; Was corrupted in body: " + wasBodyCorrupted + "; data chunks salvaged: " + dataSalvaged + "; meta chunks salvaged: " + metaSalvaged + ".";
            logCorruptedDataBlock(dataBlockFile.getAbsolutePath(), fixMsg);

            // Wait a bit for queue to empty. This will alleviate resources as the server
            // repairs critical data blocks.
            this.replaceCorruptedDataBlocksThread.waitForQueueToEmpty(maxTimeToWaitForQueueToFinish);

            return true;
        } finally {
            IOUtil.safeDelete(tmpFile);
        }
    } // repairCorruptedDataBlock(...)
    /**
     * <p>Keep associated timestamps with repairs to prevent DataBlocks to be repaired multiple times in quick succession.</p>
     */
    private final Map<String, Long> repairedDataBlockMap = new HashMap();
    /**
     * <p>The time, in milliseconds, that must ellapse since a repair to any given data block before will be repaired again.</p>
     */
    public static final long REQUIRED_TIME_BETWEEN_REPAIRS_ON_SAME_DATABLOCK = 1000 * 60 * 60 * 24;

    /**
     * <p>Check to see whether it is okay to repair a data block based on whether it has been recently repaired.</p>
     * <p>Uses the value of REQUIRED_TIME_BETWEEN_REPAIRS_ON_SAME_DATABLOCK to determine whether a data block's repair is recent or not.</p>
     * @param dataBlock The file that might be repaired.
     * @return True if it has been repaired lately, which means it should not be repaired. False otherwise.
     */
    private final synchronized boolean hasDataBlockBeenRepairedLately(File dataBlock) {

        final long currentTime = TimeUtil.getTrancheTimestamp();

        // Get list of entries to remove since required time has ellapsed
        List<String> entriesToRemove = new ArrayList();

        // Chunk for data blocks that can be removed
        for (String dataBlockPath : repairedDataBlockMap.keySet()) {
            long timeOfRepair = repairedDataBlockMap.get(dataBlockPath);
            if (currentTime - timeOfRepair > REQUIRED_TIME_BETWEEN_REPAIRS_ON_SAME_DATABLOCK) {
                entriesToRemove.add(dataBlockPath);
            }
        }

        // Remove any data block repair entries that have ellapsed
        for (String dataBlockPathToRemove : entriesToRemove) {
            repairedDataBlockMap.remove(dataBlockPathToRemove);
        }

        // If found, that means must wait
        boolean wasRepairedLately = repairedDataBlockMap.containsKey(dataBlock.getAbsolutePath());

        // If not found, going to repair. Add to map.
        if (!wasRepairedLately) {
            repairedDataBlockMap.put(dataBlock.getAbsolutePath(), currentTime);
        }

        // If skipped contains, remove it. This allows the data block to go bad later
        // and be recounted. 
        skippedDataBlocks.remove(dataBlock.getAbsolutePath());

        return wasRepairedLately;
    }
    /**
     * <p>Keep track of DataBlocks already "skipped" -- meaning these have already been flagged as corrupted and ignored because not enough time ellapsed since last handled.</p>
     */
    private final Set<String> skippedDataBlocks = new HashSet<String>();

    /**
     * <p>True if already skipped (meaning don't count this as an additional corrupted DataBlock -- already accounted for).</p>
     * @param dataBlock
     * @return
     */
    private final synchronized boolean hasDataBlockBeenSkippedAlready(File dataBlock) {
        boolean alreadySkipped = skippedDataBlocks.contains(dataBlock.getAbsolutePath());

        if (!alreadySkipped) {
            skippedDataBlocks.add(dataBlock.getAbsolutePath());
        }

        return alreadySkipped;
    }

    /**
     * <p>Returns true if is logging in-memory using DataBlockUtilLog.</p>
     * @return
     */
    public static final synchronized boolean isLogging() {
        return isLogging;
    }

    /**
     * <p>Set whether to log in-memory using DataBlockUtilLog.</p>
     * @param aIsLogging
     */
    public static final synchronized void setIsLogging(boolean aIsLogging) {
        isLogging = aIsLogging;
    }

    /**
     * <p>Returns singleton log for instance.</p>
     * @return
     */
    public static final synchronized DataBlockUtilLogger getLogger() {
        return DataBlockUtilLogger.getLogger();
    }

    /**
     * <p>Return the count of successfully merge DataBlock objects.</p>
     * @return
     */
    public long getSuccessMergeDataBlock() {
        return successMergeDataBlock;
    }

    /**
     * <p>Return the count of failed merges of DataBlock objects.</p>
     * @return
     */
    public long getFailedMergeDataBlock() {
        return failedMergeDataBlock;
    }

    /**
     * <p>Return the count of total merges of DataBlock objects.</p>
     * @return
     */
    public long getTotalMergeDataBlock() {
        return totalMergeDataBlock;
    }

    /**
     * <p>Return total count of corrupted DataBlock objects.</p>
     * @return
     */
    public long getCorruptedDataBlockCount() {
        return corruptedDataBlockCount;
    }

    /**
     * <p>Return the total count of chunks salvaged from corrupted DataBlock objects.</p>
     * @return
     */
    public long getSalvagedChunksFromCorruptedDataBlockCount() {
        return salvagedChunksFromCorruptedDataBlockCount;
    }

    /**
     * <p>Return the total count of chunks that were downloaded from other servers to replace missing chunks in corrupted DataBlock objects.</p>
     * @return
     */
    public long getDownloadedChunksFromCorruptedDataBlockCount() {
        return downloadedChunksFromCorruptedDataBlockCount;
    }

    /**
     * <p>Return the total count of chunk that were not salvaged from a corrupted DataBlock that was corrupted in the body.</p>
     * @return
     */
    public long getLostChunksFromCorruptedDataBlockCount() {
        return lostChunksFromCorruptedDataBlockCount;
    }

    /**
     * <p>Return the total count of DataBlock objects that were corrupted in the header.</p>
     * @return
     */
    public long getCorruptedDataBlockHeaderCount() {
        return corruptedDataBlockHeaderCount;
    }

    /**
     * <p>Return the total count of DataBlock objects that were corrupted in the body.</p>
     * @return
     */
    public long getCorruptedDataBlockBodyCount() {
        return corruptedDataBlockBodyCount;
    }

    /**
     * <p>Increment the count of corrupted DataBlock objects.</p>
     */
    protected void incrementCorruptedDataBlockCount() {
        corruptedDataBlockCount++;
    }

    /**
     * <p>Increment the number of salvaged chunks from a corrupted DataBlock.</p>
     */
    protected void incrementSalvagedChunksFromCorruptedDataBlockCount() {
        salvagedChunksFromCorruptedDataBlockCount++;
    }

    /**
     * <p>Increment the number of chunks that were downloaded to replace missing chunks in a corrupted DataBlock.</p>
     */
    protected void incrementDownloadedChunksFromCorruptedDataBlockCount() {
        downloadedChunksFromCorruptedDataBlockCount++;
    }

    /**
     * <p>Increment the number of chunks that were lost in corrupted DataBlock objects.</p>
     */
    protected void incrementLostChunksFromCorruptedDataBlockCount() {
        lostChunksFromCorruptedDataBlockCount++;
    }

    /**
     * <p>Increment the number of DataBlock objects that were corrupted in the header.</p>
     */
    protected void incrementCorruptedDataBlockHeaderCount() {
        corruptedDataBlockHeaderCount++;
    }

    /**
     * <p>Increment the number of DataBlock objects that were corrupted in the body.</p>
     */
    protected void incrementCorruptedDataBlockBodyCount() {
        corruptedDataBlockBodyCount++;
    }
    private boolean lastAllowedToRepairCorruptedDataBlocks = ConfigKeys.DEFAULT_SHOULD_REPAIR_CORRUPTED_DATA_BLOCKS;

    /**
     * <p>Returns true if allowed to repair corrupted DataBlocks. Check FFTS Configuration attribute.</p>
     * @return 
     */
    public boolean isAllowedToRepairCorruptedDataBlocks() {

        boolean shouldRepair = ConfigKeys.DEFAULT_SHOULD_REPAIR_CORRUPTED_DATA_BLOCKS;

        try {
            shouldRepair = Boolean.valueOf(this.ffts.getConfiguration().getValue(ConfigKeys.CORRUPTED_DB_ALLOWED_TO_FIX));
        } catch (Exception nope) { /* Skip -- either none or parse error */ }

        if (shouldRepair != lastAllowedToRepairCorruptedDataBlocks) {
            printNotice("Changing allowed to repair corrupted data blocks from " + lastAllowedToRepairCorruptedDataBlocks + " to " + shouldRepair + " at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            lastAllowedToRepairCorruptedDataBlocks = shouldRepair;
        }

        return shouldRepair;
    }

    /**
     * <p>Used to print out information about the DataBlock to standard out.</p>
     * @param message
     */
    private void printNotice(String message) {
        System.out.println("DATA_BLOCK_UTIL: " + message);
    }

    /**
     * <p>Balances data directory configuration usage. Prevents access while running.</p>
     * @return True if any balancing took place, false otherwise.
     * @throws java.lang.Exception
     */
    public boolean balanceDataDirectoryConfigurations() throws Exception {

        final long start = TimeUtil.getTrancheTimestamp();
        String status = null;

        try {
            // First check whether allowed, bail if not.
            if (!this.ffts.getHashSpanFixingThread().isAllowedToBalance(this.ffts.getConfiguration())) {
                status = "Not allowed to balance";
                return false;
            }

            // Don't bother if less than two data directories. Nothing to do.
            if (ddcs.size() < 2) {
                status = "Not enough DDCs: Requires 2, found " + ddcs.size();
                return false;
            }

            /**
             * Find DDC with most available space.
             * 
             * Heuristic: find DataDirectoryConfiguration with most available space and at least minimum size
             *            of 10 DataBlock available to receive data, which should be 1GB.
             */
            double mostAvailablePercentageUsed = -1;
            long mostAvailableSpace = 0;
            DataDirectoryConfiguration mostAvailableDDC = null;

            for (DataDirectoryConfiguration ddc : this.ddcs) {
                long available = ddc.getSizeLimit() - ddc.getActualSize();

                if (available > mostAvailableSpace) {
                    mostAvailableSpace = available;
                    mostAvailableDDC = ddc;
                    mostAvailablePercentageUsed = 100.0 * (double) ddc.getActualSize() / (double) ddc.getSizeLimit();
                }
            }

            // If nothing meets the criteria, bail
            if (mostAvailableDDC == null) {
                status = "mostAvailableDDC is null";
                return false;
            }
            if (mostAvailableSpace < getMinSizeAvailableInTargetDataBlockBeforeBalance()) {
                status = "mostAvailableSpace[" + mostAvailableDDC.getDirectory() + ": mostAvailableDDC.getSizeLimit()<" + mostAvailableDDC.getSizeLimit() + "> - mostAvailableDDC.getActualSize()<" + mostAvailableDDC.getActualSize() + ">] does not meet min size: requires " + getMinSizeAvailableInTargetDataBlockBeforeBalance() + " but found " + mostAvailableSpace;
                return false;
            }

            /**
             * Heuristic: find DataDirectoryConfiguration with greatest used space. Must be a certain
             *            percentage greater used space, which is user defined. (Default should be 15%.)
             */
            double leastAvailablePercentageUsed = -1;
            long leastAvailableSpace = Long.MAX_VALUE;
            DataDirectoryConfiguration leastAvailableDDC = null;

            for (DataDirectoryConfiguration ddc : this.ddcs) {
                long available = ddc.getSizeLimit() - ddc.getActualSize();

                if (available < leastAvailableSpace && available > 0) {
                    leastAvailableSpace = available;
                    leastAvailableDDC = ddc;
                    leastAvailablePercentageUsed = 100.0 * (double) ddc.getActualSize() / (double) ddc.getSizeLimit();
                }
            }

            final double requiredPercentage = this.ffts.getHashSpanFixingThread().getRequiredPercentageForMostUsedDataDirectoryToBalance(this.ffts.getConfiguration());

            // If didn't find a data directory with minimum required percentage of used space, bail
            if (leastAvailableDDC == null) {
                status = "leastAvailableDDC is null";
                return false;
            }

            if (leastAvailablePercentageUsed < requiredPercentage) {
                status = "leastAvailablePercentageUsed<" + leastAvailablePercentageUsed + ", DDC:" + leastAvailableDDC.getDirectory() + "> less than requiredPercentage<" + requiredPercentage + ">";
                return false;
            }

            final double requiredPercentageDifference = this.ffts.getHashSpanFixingThread().getRequiredPercentageDifferenceToBalanceDataDirectories(this.ffts.getConfiguration());

            // If not a great enough difference in percentage used between two, bail
            if (leastAvailablePercentageUsed - mostAvailablePercentageUsed < requiredPercentageDifference) {
                status = "leastAvailablePercentageUsed<" + leastAvailablePercentageUsed + "> - mostAvailablePercentageUsed<" + mostAvailablePercentageUsed + "> less than requiredPercentageDifference<" + requiredPercentageDifference + ">";
                return false;
            }

            final DataBlock toMove = this.getDataBlockForDataDirectoryConfiguration(leastAvailableDDC);

            if (toMove == null) {
                status = "A data block was not found. This could be a bug, so returning.";
                return false;
            }

            synchronized (toMove) {
                // Make sure not changed prior to synchronization
                if (!toMove.ddc.equals(leastAvailableDDC)) {
                    status = "The candidate DataBlock to move suddenly changed DDC prior to synchronization";
                    return false;
                }

                final boolean moved = toMove.moveToDataDirectoryConfiguration(mostAvailableDDC);

                // If moved, increment count
                if (moved) {
                    dataBlocksBalanced++;
                }

                status = "Moved<moved=" + moved + "> " + toMove.filename + "<" + toMove.length() + " bytes> from " + leastAvailableDDC.getDirectory() + " to " + mostAvailableDDC.getDirectory();
                return moved;
            }
        } finally {
            printTracerBalancing("Time to find single data block to move to balance data directories: " + TextUtil.formatTimeLength(TimeUtil.getTrancheTimestamp() - start) + (status != null ? "(" + status + ")" : ""));
        }
    }

    /**
     * <p>Print tracers particular to balancing funcitonality.</p>
     * @param foundMsg
     */
    private static void printTracerBalancing(String msg) {
        if (isDebugBalancing) {
            System.out.println("DataBlockUtil:balancing> " + msg);
        }
    }

    /**
     * 
     * @return The total number of data blocks moved while balancing. Starts at 0 for each instance of Tranche server.
     */
    public long getTotalDataBlocksMovedWhileBalancing() {
        return dataBlocksBalanced;
    }

    /**
     * <p>Get the DataBlockCache object used by this utility.</p>
     * @return
     */
    public DataBlockCache getDataBlockCache() {
        return cache;
    }

    /**
     * <p>Returns true if using cache, false otherwise.</p>
     * @return
     */
    public boolean isUseCache() {
        return isUseCache;
    }
    private boolean lastUseCache = this.isUseCache;

    /**
     * <p>Set whether should use cache.</p>
     * @param isUseCache
     */
    public void setUseCache(boolean isUseCache) {
        if (isUseCache != lastUseCache) {
            if (isUseCache) {
                System.out.println("DATA_BLOCK_CACHE$ Turning on caching at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            } else {
                System.out.println("DATA_BLOCK_CACHE$ Turning off caching at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            }
        }

        this.isUseCache = isUseCache;
    }

    /**
     * <p>When moving, data block must have this much free space before a move is considered.</p>
     * @return
     */
    public int getMinSizeAvailableInTargetDataBlockBeforeBalance() {
//        return minSizeAvailableInTargetDataBlockBeforeBalance;
        try {
            return Integer.parseInt(this.ffts.getConfiguration().getValue(ConfigKeys.HASHSPANFIX_REQUIRED_MIN_USED_BYTES_IN_MAX_DATABLOCK_TO_BALANCE_DATA_DIRECTORIES));
        } catch (Exception e) {
        }
        return ConfigKeys.DEFAULT_MIN_SIZE_AVAILABLE_IN_TARGET_DATABLOCK_BEFORE_BALANCE;
    }

    /**
     * <p>When moving, data block must have this much free space before a move is considered.</p>
     * @param aMinSizeAvailableInTargetDataBlockBeforeBalance
     */
    public void setMinSizeAvailableInTargetDataBlockBeforeBalance(int aMinSizeAvailableInTargetDataBlockBeforeBalance) {
        ffts.getConfiguration().setValue(ConfigKeys.HASHSPANFIX_REQUIRED_MIN_USED_BYTES_IN_MAX_DATABLOCK_TO_BALANCE_DATA_DIRECTORIES, String.valueOf(aMinSizeAvailableInTargetDataBlockBeforeBalance));
    }

    private boolean lastIsStoreDataBlockReferences = DEFAULT_STORE_DATA_BLOCK_REFERENCES;

    /**
     * This only sets in memory. To set permanently, set the Configuration value ConfigKeys.DATABLOCK_STORE_DATABLOCK_REFERENCES to "true".
     * @deprecated Set the value in Configuration file
     * @param store
     * @return
     */
    public void setStoreDataBlockReferences(boolean store) {
        lastIsStoreDataBlockReferences = store;
    }

    /**
     * @return the storeDataBlockReferences
     */
    public boolean isStoreDataBlockReferences() {
        boolean storeDataBlockReferences = lastIsStoreDataBlockReferences;

        try {
            storeDataBlockReferences = Boolean.valueOf(this.ffts.getConfiguration().getValue(ConfigKeys.DATABLOCK_STORE_DATABLOCK_REFERENCES));
        } catch (Exception nope) { }

        if (lastIsStoreDataBlockReferences != storeDataBlockReferences) {
            printNotice("Changed \"" + ConfigKeys.DATABLOCK_STORE_DATABLOCK_REFERENCES + "\" from "+lastIsStoreDataBlockReferences+" to "+storeDataBlockReferences);
            lastIsStoreDataBlockReferences = storeDataBlockReferences;
        }

        return storeDataBlockReferences;
    }

    /**
     * @return the ONE_MB
     */
    public static int getMaxChunkSize() {
        return maxChunkSize;
    }

    /**
     * @param aONE_MB the ONE_MB to set
     */
    public static void setMaxChunkSize(int aONE_MB) {
        maxChunkSize = aONE_MB;
    }
}
