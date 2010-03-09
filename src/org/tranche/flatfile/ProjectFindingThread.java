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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.tranche.exceptions.UnexpectedEndOfDataBlockException;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataUtil;
import org.tranche.time.TimeUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;

/**
 * <p>Loads information about available data, along with corrupted DataBlock files, from disk. This runs when a server starts up.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ProjectFindingThread extends Thread {

    private static boolean debug = false;
    private final FlatFileTrancheServer ffts;
    /**
     * Tell this thread to stop. Happens if launching a new thread.
     */
    private boolean stopped = false;
    private int successes = 0,  failures = 0;
    private long metaLoaded = 0,  metaFailures = 0,  dataLoaded = 0;
    private long totalDataBlockMismatch = 0;

    /**
     * 
     * @param ffts
     */
    public ProjectFindingThread(FlatFileTrancheServer ffts) {
        setName("Project Finding Thread");
        this.ffts = ffts;
    }

    @Override()
    public void run() {
        /*
         * Check to see if testing with thread off. If so, return, but print a
         * conspicuous notice.
         */
        if (TestUtil.isTestingTurnOffProjectFindingThread()) {
            debugOut("");
            debugOut("********************************************************************");
            debugOut("******* WARNING: The project find thread has been turned OFF *******");
            debugOut("*** The TestUtil has been set to prevent this thread from running");
            debugOut("*** while testing. At: " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            debugOut("********************************************************************");
            debugOut("");

            // Simulate a running thread
            try {
                Thread.sleep(100);
            } catch (InterruptedException nope) { /* Move on, going to exit anyhow */ }

            this.ffts.doneLoadingDataBlocks = true;

            // Bail
            return;
        }


        final long start = TimeUtil.getTrancheTimestamp();

        try {

            // Reset all the FFTS values
            resetFlatFileTrancheServer();

            debugOut("Thread starting...");

            // Copy to new collection to avoid ConcurrentModificationExceptions while running (likely -- runs a while)
            Set<DataDirectoryConfiguration> ddcs = new HashSet();
            ddcs.addAll(ffts.getConfiguration().getDataDirectories());

            loadDataBlocks(ffts.getDataBlockUtil(), ddcs, this, ffts);

        } catch (Exception e) {
            debugErr(e);
        } finally {

            // Don't do anything in finally block if stopped; abandon
            if (this.isStopped()) {
                return;
            }

            /* Print out useful information.
             * Should be redirected to log. Very helpful for server-side troubleshooting.
             */
            if (!TestUtil.isTesting()) {
                debugOut("");
                debugOut("FlatFileTrancheServer finished loading data, took: " + Text.getPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp() - start));
                debugOut(" - Home directory: " + this.ffts.getHomeDirectory().getAbsolutePath());

                // DDC information
                final int dataDirectoryCount = this.ffts.getConfiguration().getDataDirectories().size();
                debugOut(" - " + dataDirectoryCount + " data " + (dataDirectoryCount == 1 ? "directory" : "directories") + ":");
                for (DataDirectoryConfiguration ddc : this.ffts.getConfiguration().getDataDirectories()) {
                    debugOut("   * " + ddc.getDirectory() + " (limit=" + Text.getFormattedBytes(ddc.getSizeLimit()) + ", used=" + Text.getFormattedBytes(ddc.getActualSize()) + ")");
                }

                // Hash spans information
                final int hashSpanCount = this.ffts.getConfiguration().getHashSpans().size();
                debugOut(" - " + hashSpanCount + " hash " + (hashSpanCount == 1 ? "span" : "spans") + (hashSpanCount > 0 ? ":" : ""));
                for (HashSpan s : this.ffts.getConfiguration().getHashSpans()) {
                    debugOut("  * Start:  " + s.getFirst());
                    debugOut("    Finish: " + s.getLast());
                }

                // Loaded chunk information
                debugOut(" - " + successes + " data blocks loaded successfully, " + failures + " data blocks unexpectedly caused an exception.");
                debugOut(" - " + dataLoaded + " data chunks were loaded, " + metaLoaded + " meta data chunks were loaded, " + metaFailures + " meta data chunks caused an exception.");
                debugOut("");

                // Advanced "debug" information
                debugOut("Other debugging information:");
                debugOut(" - " + totalDataBlockMismatch + " DataDirectoryConfiguration mismatches while iterating through directories finding blocks and chunks.");

                // Corrupted chunk information
                final DataBlockUtil dbu = this.ffts.getDataBlockUtil();
                final long corruptedDataBlockCount = dbu.getCorruptedDataBlockCount();
                debugOut(" - " + corruptedDataBlockCount + " corrupted data " + (corruptedDataBlockCount == 1 ? "block" : "blocks") + (corruptedDataBlockCount > 0 ? ":" : ""));
                if (corruptedDataBlockCount > 0) {
                    debugOut("   * " + dbu.getCorruptedDataBlockHeaderCount() + " corrupted block(s) in header, " + dbu.getCorruptedDataBlockBodyCount() + " corrupted block(s) in body (payload).");
                    debugOut("   * Chunks in corrupted DataBlocks:");
                    debugOut("     a. Salvaged:   " + dbu.getSalvagedChunksFromCorruptedDataBlockCount());
                    debugOut("     b. Downloaded: " + dbu.getDownloadedChunksFromCorruptedDataBlockCount());
                    debugOut("     c. Lost:       " + dbu.getLostChunksFromCorruptedDataBlockCount());
                }
                debugOut("");
            }
        }

        // handle the queue of files to merge
        while (!this.ffts.isClosed() && !this.isStopped()) {
            // try to get a item to use
            DataBlockToMerge dbtm = null;
            try {
                dbtm = this.ffts.getDataBlockUtil().mergeQueue.poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // noop
            }
            if (dbtm == null) {
                continue;
            }

            // try the merge
            try {
                // the size to decrement
                long sizeToDecrement = dbtm.fileToMerge.length();
                /**
                 * Before committing any information to FFTS or DataBlockUtil,
                 * check to see whether should stop
                 */
                if (this.isStopped()) {
                    break;
                }

                // handle the merge
                try {
                    this.ffts.getDataBlockUtil().mergeOldDataBlock(dbtm.fileToMerge);
                } catch (UnexpectedEndOfDataBlockException ex) {

                    // Send in the data block for salvaging and recreation
                    this.ffts.getDataBlockUtil().repairCorruptedDataBlock(dbtm.fileToMerge, "ProjectFindingThread: merging old data block (2, indefinite merging)");

                    // Rethrow the exception so logs appropriately
                    throw ex;
                }
                // decrement the bytes used

                /**
                 * Before committing any information to FFTS or DataBlockUtil,
                 * check to see whether should stop
                 */
                if (this.isStopped()) {
                    break;
                }

                dbtm.ddc.adjustUsedSpace(-sizeToDecrement);
            } catch (Exception e) {
                // noop
            }
        }
        debugOut("Exitting. Was stopped?: " + this.isStopped());
    }

    /**
     * <p>Clears out FlatFileTrancheServer values so can calculate, again if need be.</p>
     * @throws java.lang.Exception
     */
    private void resetFlatFileTrancheServer() throws Exception {
        this.ffts.doneLoadingDataBlocks = false;
        this.ffts.clearKnownProjects();
    // DataBlockUtil reset in FlatFileTrancheServer
    }

    /**
     * <p>Returns true if this has been stopped.</p>
     * @return
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * <p>Set whether this thread is stopped or not.</p>
     * <p>Note that setting this stopped to false after the thread has stopped does nothing.</p>
     * @param stopped
     */
    public void setStopped(boolean stopped) {
        debugOut("Stopping");
        this.stopped = stopped;
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        ProjectFindingThread.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static final boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static final void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(ProjectFindingThread.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }

    /**
     * <p>Depth-first traversal of data blocks to repair and merge old data blocks.</p>
     * <p>This method interface is intended for tests and scripts. Production servers should not invoke this, but will actually run the ProjectFindingThread, which will in turn invoke this method.</p>
     * @param dbu Must not be null (required)
     */
    public static void loadDataBlocks(DataBlockUtil dbu) {
        loadDataBlocks(dbu, dbu.getDataDirectoryConfigurations(), null, null);
    }

    /**
     * <p>Depth-first traversal of data blocks to repair, merge old data blocks and gather statistics.</p>
     * <p>This method interface is intended for tests and scripts. Production servers should not invoke this, but will actually run the ProjectFindingThread, which will in turn invoke this method.</p>
     * @param ffts Must not be null (required)
     */
    public static void loadDataBlocks(FlatFileTrancheServer ffts) {
        Set<DataDirectoryConfiguration> ddcs = new HashSet();
        ddcs.addAll(ffts.getConfiguration().getDataDirectories());
        loadDataBlocks(ffts.getDataBlockUtil(), ddcs, null, ffts);
    }

    /**
     * <p>Depth-first traversal of data blocks to repair, merge old data blocks and gather statistics.</p>
     * <p>Note that some some parameters are required while some are not.</p>
     * @param dbu Must not be null (required)
     * @param ddcs Must not be null (required)
     * @param thisThread Can be null
     * @param ffts Can be null
     */
    private static void loadDataBlocks(DataBlockUtil dbu, Set<DataDirectoryConfiguration> ddcs, ProjectFindingThread thisThread, FlatFileTrancheServer ffts) {
        try {

            // iterate through all of the data directory configurations
            for (DataDirectoryConfiguration ddc : ddcs) {
                final long startDDC = TimeUtil.getTrancheTimestamp();

                debugOut("Searching for data in " + ddc.getDirectory());

                /**
                 * Before committing any information to FFTS or DataBlockUtil,
                 * check to see whether should stop
                 */
                if (thisThread != null && thisThread.isStopped()) {
                    debugOut("Stopped");
                    break;
                }

                // use a stack to handle all directories
                List<String> filenames = new LinkedList();
                filenames.add(ddc.getDirectory());

                // loop over all entries
                while (!filenames.isEmpty()) {
                    String filename = filenames.remove(filenames.size() - 1);

                    debugOut("File name: " + filename);

                    /**
                     * Before committing any information to FFTS or DataBlockUtil,
                     * check to see whether should stop
                     */
                    if (thisThread != null && thisThread.isStopped()) {
                        debugOut("Stopped");
                        break;
                    }

                    try {
                        File file = new File(filename);
                        debugOut("File path: " + file.getAbsolutePath());
                        if (file.isDirectory()) {
                            for (String moreFileName : file.list()) {
                                debugOut("Adding file to stack: " + moreFileName);
                                filenames.add(file.getAbsolutePath() + File.separator + moreFileName);
                            }
                        } else {
                            // if this is a .backup or .toadd file, merge it!
                            if (file.getName().contains(".")) {
                                // add the old data
                                String blockName = filename.substring(ddc.getDirectory().length());
                                blockName = blockName.split("\\.")[0];

                                /**
                                 * Before committing any information to FFTS or DataBlockUtil,
                                 * check to see whether should stop
                                 */
                                if (thisThread != null && thisThread.isStopped()) {
                                    debugOut("Stopped");
                                    break;
                                }

                                try {
                                    // add the data!
                                    dbu.mergeOldDataBlock(new File(filename));
                                } catch (UnexpectedEndOfDataBlockException ex) {

                                    // Send in the data block for salvaging and recreation
                                    dbu.repairCorruptedDataBlock(new File(filename), "ProjectFindingThread: merging old data block (1, startup)");

                                    // Rethrow the exception so logs appropriately
                                    throw ex;
                                }
                                // skip loading normally
                                continue;
                            }

                            // load the existing block normally
                            // add all meta-data hashes and check the files for project files
                            String trimmedFileName = filename.substring(ddc.getDirectory().length());
                            debugOut("Trimmed File Name: " + trimmedFileName);

                            if (trimmedFileName == null || trimmedFileName.trim().equals("")) {
                                debugOut("Trimmed file name is empty for " + filename + ", skipping...");
                                continue;
                            }

                            /**
                             * Before committing any information to FFTS or DataBlockUtil,
                             * check to see whether should stop
                             */
                            if (thisThread != null && thisThread.isStopped()) {
                                debugOut("Stopped");
                                break;
                            }

                            DataBlock dataBlock = dbu.getDataBlock(trimmedFileName);

                            if (dataBlock == null) {
                                System.err.println("Could not find data block for " + trimmedFileName + ", skipping...");
                                System.err.flush();
                                continue;
                            }

                            // The found DDC should match the currently iterated DDC. If not, increment
                            // count that will be printed to standard out later.
                            if (thisThread != null && !ddc.equals(dataBlock.ddc)) {
                                thisThread.totalDataBlockMismatch++;
                            }

                            /**
                             * Need to add the size back, or the DataDirectoryConfiguration size limit
                             * won't be honored.
                             */
                            dataBlock.ddc.adjustUsedSpace(file.length());

                            List<BigHash> metaDataHashes = null;

                            try {
                                metaDataHashes = dataBlock.getHashes(true);
                            } catch (UnexpectedEndOfDataBlockException ex) {

                                // Send in the data block for salvaging and recreation
                                dbu.repairCorruptedDataBlock(new File(filename), "ProjectFindThread: getting meta data hashes from data block");

                                // Rethrow the exception so logs appropriately
                                throw ex;
                            }
                            debugOut("# Meta Data Hashes: " + metaDataHashes.size());

                            /**
                             * Before committing any information to FFTS or DataBlockUtil,
                             * check to see whether should stop
                             */
                            if (thisThread != null && thisThread.isStopped()) {
                                debugOut("Stopped");
                                break;
                            }

                            // get a reference to the DataBlockUtil
                            for (BigHash metaDataHash : metaDataHashes) {

                                debugOut("Checking meta data " + metaDataHash);

                                /**
                                 * Before committing any information to FFTS or DataBlockUtil,
                                 * check to see whether should stop
                                 */
                                if (thisThread != null && thisThread.isStopped()) {
                                    debugOut("Stopped");
                                    break;
                                }
                                // add to the meta-data list kept internally
                                dbu.metaDataHashes.add(metaDataHash);

                                byte[] metaBytes = null;
                                try {
                                    metaBytes = dbu.getMetaData(metaDataHash);
                                } catch (UnexpectedEndOfDataBlockException ex) {

                                    // Send in the data block for salvaging and recreation
                                    dbu.repairCorruptedDataBlock(new File(filename), "ProjectFindThread: getting meta data chunk to look for project files");

                                    // Rethrow the exception so logs appropriately
                                    throw ex;
                                }

                                if (metaBytes == null) {
                                    System.err.println("Returned null for meta bytes while loading meta data! Why!?!");
                                    System.err.flush();
                                    continue;
                                }

                                // read the meta-data to check for project files
                                try {
                                    MetaData md = MetaDataUtil.read(new ByteArrayInputStream(metaBytes));
                                    if (md.isProjectFile()) {
                                        debugOut("ProjectFile found: " + metaDataHash);
                                        /**
                                         * Before committing any information to FFTS or DataBlockUtil,
                                         * check to see whether should stop
                                         */
                                        if (thisThread != null && thisThread.isStopped()) {
                                            debugOut("Stopped");
                                            break;
                                        }

                                        // add the hash for the project file
                                        if (ffts != null) {
                                            ffts.addKnownProject(metaDataHash);
                                        }
                                    }

                                    if (ffts != null && ffts.isStickyMetaDataForThisServer(md) && md.isProjectFile()) {
                                        ffts.getConfiguration().addStickyProject(metaDataHash);
                                    }

                                    // Only counts as loaded if makes it here
                                    if (thisThread != null) {
                                        thisThread.metaLoaded++;
                                    }
                                } catch (Exception metaEx) {
                                    debugErr(metaEx);
                                    if (thisThread != null) {
                                        thisThread.metaFailures++;
                                        System.err.println("Meta data exception #" + thisThread.metaFailures + " while loading meta data in ProjectFindingThread <" + metaEx.getClass().getName() + ">: " + metaEx.getMessage());
                                    }
                                // Don't print stack trace. Message above is brief but contains enough info.
                                // Might be a lot of these!
                                }
                            }

                            /**
                             * Before committing any information to FFTS or DataBlockUtil,
                             * check to see whether should stop
                             */
                            if (thisThread != null && thisThread.isStopped()) {
                                debugOut("Stopped");
                                break;
                            }

                            // add all data hashes and check the files for project files
                            List<BigHash> dataHashes = dbu.getDataBlock(filename.substring(ddc.getDirectory().length())).getHashes(false);
                            for (BigHash dataHash : dataHashes) {

                                /**
                                 * Before committing any information to FFTS or DataBlockUtil,
                                 * check to see whether should stop
                                 */
                                if (thisThread != null && thisThread.isStopped()) {
                                    debugOut("Stopped");
                                    break;
                                }

                                // add to the meta-data list kept internally
                                dbu.dataHashes.add(dataHash);
                                if (thisThread != null) {
                                    thisThread.dataLoaded++;
                                }
                            }
                            if (thisThread != null) {
                                thisThread.successes++;
                            }
                        } // Found a datablock

                    } // handle unexpected exceptions while loading data/meta-data caches
                    catch (Exception e) {
                        if (thisThread != null) {
                            thisThread.failures++;
                        }
                        debugErr(e);
                    }
                }

                debugOut("Finished " + ddc.getDirectory() + " at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + ", DDC took " + Text.getPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp() - startDDC) + " to scan in ProjectFindingThread.");
            } // Go through each DDC looking for data/meta data

            /**
             * Before committing any information to FFTS or DataBlockUtil,
             * check to see whether should stop
             */
            if (thisThread != null && thisThread.isStopped()) {
                debugOut("Stopped");
                return;
            }
        } finally {
            // flag done load data blocks
            if (ffts != null) {
                ffts.doneLoadingDataBlocks = true;
            }
        }
    } // loadDataBlocks
}
