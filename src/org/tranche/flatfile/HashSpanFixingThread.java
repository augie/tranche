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
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.tranche.commons.Tertiary;
import org.tranche.TrancheServer;
import org.tranche.configuration.Configuration;
import org.tranche.configuration.ConfigKeys;
import org.tranche.commons.DebuggableThread;
import org.tranche.commons.TextUtil;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.exceptions.UnexpectedEndOfDataBlockException;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.util.IOUtil;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataUtil;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.MultiServerRequestStrategy;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTable;
import org.tranche.network.StatusTableRow;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.util.TestUtil;

/**
 * <p>"Heals" a server's data and meta-data based on the configured hash span.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class HashSpanFixingThread extends DebuggableThread {

    /**
     * <p>Static references to the activities this thread will undertake</p>
     */
    public static final byte ACTIVITY_NOTHING = 0, ACTIVITY_HEALING = 1, ACTIVITY_DELETING = 2, ACTIVITY_DOWNLOADING = 3, ACTIVITY_BALANCING = 4;
    /**
     * <p>a reference to the current activity handled by this thread.</p>
     */
    private byte currentActivity = ACTIVITY_NOTHING;
    /**
     * <p>Keep track of times spent doing things.</p>
     */
    private long timeSpentDoingNothing = 0, timeSpentHealing = 0, timeSpentDeleting = 0, timeSpentDownloading = 0, timeSpentBalancing = 0;
    /**
     * Instead of core, use these!
     */
    private static Set<String> hostsToUse = new HashSet<String>();
    private final FlatFileTrancheServer ffts;
    private Configuration config;
    private boolean isClosed = false;
    /**
     * <p>Time in milliseconds to sleep when not allowed to run before retrying.</p>
     */
    private static long timeToSleepIfNotAllowedToRun = 60 * 1000;
    /**
     * 
     */
    private final HashSpanFixingThreadPolicyDecider hashSpanFixingThreadPolicyDecider;
    /**
     * <p>The number of chunks to download before going on to next task.</p>
     * <p>Keep this low! If configuration should change, want to get new information ASAP.</p>
     */
    public static final int DEFAULT_BATCH_SIZE_FOR_CHUNKS_TO_DOWNLOAD = 50;
    /**
     * <p>The number of chunks to check to see whether needs replaced, and to replace if need be, before going on to next task.</p>
     * <p>Keep this low! If configuration should change, want to get new information ASAP.</p>
     */
    public static final int DEFAULT_BATCH_SIZE_FOR_CHUNKS_TO_HEAL = 10;
    /**
     * <p>The number of chunks to check whether can be deleted, and to delete if can, before going on to next task.</p>
     * <p>Keep this low! If configuration should change, want to get new information ASAP.</p>
     */
    public static final int DEFAULT_BATCH_SIZE_FOR_CHUNKS_TO_DELETE = 5;
    /**
     * <p>The number of DataBlocks to balance between DataDirectoryConfiguration objects before going on to next task.</p>
     * <p>Keep this low! If configuration should change, want to get new information ASAP.</p>
     */
    public static final int DEFAULT_BATCH_SIZE_FOR_BLOCKS_TO_BALANCE = 2;
    /**
     * <p>Default value for number of milliseconds to pause between operations in healing thread.</p>
     * <p>A low value allows thread to operate quickly, a high value slows it down to free up resources.</p>
     */
    public static final long DEFAULT_MILLIS_PAUSE_BETWEEN_OPERATIONS = 100;
    private long timestampLastGetConfiguration = TimeUtil.getTrancheTimestamp();
    // the closing flag
    private boolean waitIfRequested = true;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long dataCopied = 0;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long dataSkipped = 0;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long metaDataCopied = 0;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long metaDataSkipped = 0;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long dataRepaired = 0;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long metaDataRepaired = 0;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long dataNotRepaired = 0;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long metaDataNotRepaired = 0;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long dataLocalChunkThrewException = 0;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long metaLocalDataChunkThreadException = 0;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long localRepairCompleteIterations = 0;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long localDeleteCompleteIterations = 0;
    /**
     * <p>One of many status variables for the FlatFileTrancheServer to use to report progress of this thread.</p>
     */
    protected long iterationCount = 0;
    /**
     * <p>Used to do local verification one cheap batch at a time.</p>
     */
    protected long localVerifyIteration = 0;
    /**
     * <p>Used to do local deletion one cheap batch at a time.</p>
     */
    protected long localDeleteIteration = 0;
    /**
     * <p>Get the total number of data chunks deleted.</p>
     */
    protected long dataDeleted = 0;
    /**
     * <p>Get the total number of meta data chunks deleted.</p>
     */
    protected long metaDataDeleted = 0;
    /**
     * <p>The current server being checked by this thread to work on this server.</p>
     */
    protected String serverBeingIndexed = "Unknown";
    /**
     * <p>A description of the current information used to produce a report for remote administration.</p>
     */
    private String currentDownloadDescription = null;
    /**
     * <p>A description of the current information used to produce a report for remote administration.</p>
     */
    private String currentDeletionDescription = null;
    /**
     * <p>A description of the current information used to produce a report for remote administration.</p>
     */
    private String currentHealingDescription = null;
    /**
     * <p>Current score for performance of the healing thread.</p>
     * <p>There are several internal heuristics for determining the scores, but basically scores are incremented when activities are productive. In some contexts (e.g., low disk space), some activities are more highly weighted (e.g., deleting unnecessary space).</p>
     */
    private long currentPerformanceScore = 0;
    /**
     * 
     */
    private static final String DELETE_DATA_SAVE_FILENAME = "delete-data.healingthread";
    /**
     * 
     */
    private static final String DELETE_META_DATA_SAVE_FILENAME = "delete-meta-data.healingthread";
    /**
     * 
     */
    private static final String DOWNLOAD_DATA_INDEX_SAVE_FILENAME = "download-data.index.healingthread";
    /**
     * 
     */
    private static final String DOWNLOAD_META_DATA_INDEX_SAVE_FILENAME = "download-meta-data.index.healingthread";
    /**
     * 
     */
    private static final String DOWNLOAD_SERVER_SAVE_FILENAME = "download-data.server.healingthread";
    /**
     * 
     */
    private static final String REPAIR_DATA_SAVE_FILENAME = "repair-data.healingthread";
    /**
     * 
     */
    private static final String REPAIR_META_DATA_SAVE_FILENAME = "repair-meta-data.healingthread";
    /**
     *
     */
    private static final String BAD_DATA_CHUNK_SAVE_FILENAME = "bad-data-chunk.healingthread";
    /**
     *
     */
    private static final String BAD_META_DATA_CHUNK_SAVE_FILENAME = "bad-meta-data-chunk.healingthread";

    /**
     * <p>The state of the healing thread:</p>
     * <ul>
     *      <li>Unknown: the state hasn't been determined yet</li>
     *      <li>Low: Disk usage is low</li>
     *      <li>Normal: Disk usage is normal</li>
     *      <li>High: One or more drives is at 90% use</li>
     * </ul>
     */
    private enum ThreadState {

        UNKNOWN, LOW, NORMAL, HIGH
    };

    /**
     * <p>Used to control logic of main method from helper methods.</p>
     */
    enum ReturnType {

        VOID, CONT_CORE_SERVERS, CONT_OUTER_LOOP, EXIT
    };
    /**
     * <p>The state of the healing thread:</p>
     * <ul>
     *      <li>Unknown: the state hasn't been determined yet</li>
     *      <li>Low: Disk usage is low</li>
     *      <li>Normal: Disk usage is normal</li>
     *      <li>High: One or more drives is at 90% use</li>
     * </ul>
     */
    private ThreadState currentThreadState = ThreadState.UNKNOWN;

    /**
     * <p>Get the number of replications on other servers before chunk deleted on this server. A policy.
     * @return
     * @deprecated This may be used by testing, but should not be used in production. Instead, set the appropriate value in the server's Configuration object.
     */
    public int getRequiredReplicationsToDelete() {
        return this.getTotalRequiredRepsToDelete(this.ffts.getConfiguration(false));
    }

    /**
     * <p>Set the number of required replications of chunk on other servers before deletion. Default is 3.</p>
     * <p>For example, if the required replications were 3, and 3+ servers have the chunk and this server has the chunk out of its hash span, the server will delete it.</p>
     * <p>However, if the required reps were 5, this server won't delete the chunk even though it is out of its hash span.</p>
     * <p>Note this operation blocks until the current iteration is complete.</p>
     * @param num
     * @param cert
     * @param key
     * @throws java.lang.Exception
     * @deprecated This may be used by testing, but should not be used in production. Instead, set the appropriate value in the server's Configuration object.
     */
    public void setRequiredReplicationsToDelete(int num, X509Certificate cert, PrivateKey key) throws Exception {
        Configuration c = this.ffts.getConfiguration(false);
        c.setValue(ConfigKeys.HASHSPANFIX_NUMBER_REPS_REQUIRED_DELETE, String.valueOf(num));

        IOUtil.setConfiguration(this.ffts, c, cert, key);
    }

    /**
     * 
     * @param ffts
     */
    public HashSpanFixingThread(FlatFileTrancheServer ffts) {
        this.ffts = ffts;
        // set as daemon
        setDaemon(true);
        // low priority
        setPriority(Thread.MIN_PRIORITY);
        // set the name
        setName("Hash Span Fixing Thread");

        // Create the decider, which should help balance activities and avoid redundancy
        this.hashSpanFixingThreadPolicyDecider = new HashSpanFixingThreadPolicyDecider(ffts, HashSpanFixingThread.this);
    }

    /**
     * <p>Blocks until next iteration. Mostly useful for tests or to determine whether affects have kicked in yet.</p>
     * @throws java.lang.InterruptedException
     */
    public void waitForIteration() throws InterruptedException {
        // only block if notifyAll() will later be called
        if (waitIfRequested) {
            synchronized (this) {
                wait();
            }
        }
    }

    /**
     * 
     */
    @Override()
    public void run() {
        try {

            // Let the project finding thread fire off. This helps with tests,
            // and gives the thread some material with which to work.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException nope) { /* Continue */ }

            // Set the activity so can know how long things are taking and so
            // users can see current activity in configuration
            setCurrentActivity(HashSpanFixingThread.ACTIVITY_NOTHING);

            // loop forever
            OUTER_LOOP:
            while (!ffts.isClosed() && !isClosed && (!TestUtil.isTesting() || TestUtil.isTestingHashSpanFixingThread())) {
                // inc the iteration count
                this.iterationCount++;

                // Under no exception should this stop in core server
                try {

                    // notify of a complete iteration
                    synchronized (this) {
                        notifyAll();
                    }

                    // If explicitly closed, break out
                    if (ffts.isClosed() || isClosed) {
                        return;
                    }

                    // Grab configuration. Record timestamp so can easily check if any changes later.
                    config = ffts.getConfiguration(false);
                    timestampLastGetConfiguration = TimeUtil.getTrancheTimestamp();

                    // Check to see whether should run
                    if (!isAllowedToRun(config)) {
                        Thread.sleep(getTimeToSleepIfNotAllowedToRun());
                        continue;
                    }

                    // Check for special option, and if present, use those servers!
                    if (config.hasKey(ConfigKeys.HASHSPANFIX_SERVER_HOSTS_TO_USE)) {
                        try {
                            String[] hosts = config.getValue(ConfigKeys.HASHSPANFIX_SERVER_HOSTS_TO_USE).split(",");

                            clearServerHostsToUse();
                            for (String host : hosts) {
                                addServerHostToUse(host.trim());
                            }
                        } catch (Exception ex) {
                            debugOut("Problem parsing option " + ConfigKeys.HASHSPANFIX_SERVER_HOSTS_TO_USE + ":" + ex.getMessage());
                            debugOut("Clearing servers, going to use core servers instead...");
                            clearServerHostsToUse();
                        }
                    }

                    // No matter what, start with these activities. That way, if using servers but have nothing,
                    // they will execut anyhow
                    checkForChunksToRepair();
                    checkForChunksToDelete();
                    checkForDataDirectoriesToBalance();

                    // if no hash spans are configured
                    if (getLocalServerTargetHashSpans().isEmpty()) {
                        continue OUTER_LOOP;
                    }

                    // the array of servers to use
                    String[] coreServerHosts = getHostsToUse();

                    // Was there a server we were previously working on?
                    String lastServer = getDownloadServer();
                    int lastServerIndex = 0;

                    if (lastServer != null) {
                        FIND_INDEX:
                        for (int i = 0; i < coreServerHosts.length; i++) {
                            if (coreServerHosts[i].equals(lastServer.trim())) {
                                lastServerIndex = i;
                                break FIND_INDEX;
                            }
                        }
                    }

                    // loop through all of the other servers and try to grab content
                    CORE_SERVERS:
                    for (int serverIndex = lastServerIndex; serverIndex < coreServerHosts.length; serverIndex++) {
                        final String nextHost = coreServerHosts[serverIndex];

                        // Save the current server so can resume after restart
                        setDownloadServer(nextHost);
                        try {

                            // If verifyHost is this local server, skip
                            if (isLocalServerHost(nextHost, config)) {
                                continue CORE_SERVERS;
                            }

                            // break out if appropriate
                            if (ffts.isClosed() || isClosed) {
                                return;
                            }

                            // Checks to see whether a server is really online
                            if (!isServerReallyOnline(nextHost)) {
                                continue CORE_SERVERS;
                            }

                            // >>> ITERATE DATA <<<
                            ReturnType returnType = checkForChunksToDownload(nextHost, false);
                            switch (returnType) {
                                case VOID:
                                    break;
                                case CONT_CORE_SERVERS:
                                    continue CORE_SERVERS;
                                case CONT_OUTER_LOOP:
                                    continue OUTER_LOOP;
                                case EXIT:
                                    return;
                            }

                            // break out if appropriate
                            if (ffts.isClosed() || isClosed) {
                                return;
                            }

                            // >>> ITERATE META DATA <<<
                            returnType = checkForChunksToDownload(nextHost, true);
                            switch (returnType) {
                                case VOID:
                                    break;
                                case CONT_CORE_SERVERS:
                                    continue CORE_SERVERS;
                                case CONT_OUTER_LOOP:
                                    continue OUTER_LOOP;
                                case EXIT:
                                    return;
                            }

                            // Since finished both data and meta data chunks, clear this information
                            clearDownloadServer();
                            setDownloadDataIndex(0);
                            setDownloadMetaDataIndex(0);

                            // break out if appropriate
                            if (ffts.isClosed() || isClosed) {
                                return;
                            }
                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                            e.printStackTrace();
                        } finally {
                            // sleep to relieve CPU
                            try {
                                sleep(1000);
                            } catch (Exception e) {
                            }
                        }
                    } // CORE SERVER LOOP

                    // Finished all servers. Make sure cleared out for next run.
                    clearDownloadServer();
                    setDownloadDataIndex(0);
                    setDownloadMetaDataIndex(0);
                } catch (Exception ex) {
                    System.err.println("Unexpected exception in HashSpanFixingThread: " + ex.getMessage());
                    ex.printStackTrace(System.err);
                } finally {
                    // sleep to relieve CPU
                    try {
                        sleep(1000);
                    } catch (Exception e) {
                    }
                }
            } // Big while loop

        } finally {

            // Set the activity so can know how long things are taking and so
            // users can see current activity in configuration
            this.setCurrentActivity(HashSpanFixingThread.ACTIVITY_NOTHING);

            // Update current activity description
            currentDownloadDescription = null;

            // Keep a reference of current server being checked
            this.serverBeingIndexed = "Unknown";

            // notify of a complete iteration
            synchronized (this) {
                // don't let any other threads wait
                waitIfRequested = false;
                // notify everyone who is listening
                notifyAll();
            }
        }
    } // run

    /**
     * <p>Check whether verifyHost is current server verifyHost.</p>
     * @param hostToCheck
     * @return
     */
    /**
     * <p>Iterate remote server's chunks (choose either data or meta data), pausing inbetween batches to perform other household tasks.</p>
     * <p>This is really part of main method's logic, but refactored for clarity.</p>
     * @param nextHost
     * @param localConfig
     * @param isMetaData
     * @return
     * @throws java.lang.Exception
     */
    private ReturnType checkForChunksToDownload(String host, boolean isMetaData) throws Exception {
        try {

            TrancheServer ts = getConnection(host);

            long lastDataIndex = -1;

            if (isMetaData) {
                lastDataIndex = getDownloadMetaDataIndex();
            } else {
                lastDataIndex = getDownloadDataIndex();
            }

            BigInteger offset = BigInteger.valueOf(lastDataIndex);
            BigInteger limit = BigInteger.valueOf(this.getBatchSizeForChunksToDownload(config));

            int count = 0;

            /**
             * Get all data or meta data chunks
             */
            BATCHES:
            for (BigHash[] chunks = (isMetaData ? ts.getMetaDataHashes(offset, limit) : ts.getDataHashes(offset, limit)); chunks.length > 0; chunks = (isMetaData ? ts.getMetaDataHashes(offset, limit) : ts.getDataHashes(offset, limit))) {

                // Checks to see whether a server is really online
                if (!isServerReallyOnline(host)) {
                    return ReturnType.CONT_CORE_SERVERS;
                }

                count++;
                if (hasConfigurationChangedCritically(config)) {
                    debugOut("1a. While checking for " + String.valueOf(isMetaData ? "meta data" : "data") + " chunks to fetch, configuration changed. Restarting.");
                    return ReturnType.CONT_OUTER_LOOP;
                }

                // Save the index perchance restart
                if (isMetaData) {
                    setDownloadMetaDataIndex(offset.longValue());
                } else {
                    setDownloadDataIndex(offset.longValue());
                }

                /**
                 * <p>Start with iteration of 0. This will slowly repair every iteration.</p>
                 * <p>Heal inbetween replication batches so network overcomes any bad IO replications.</p>
                 */
                checkForChunksToRepair();
                checkForChunksToDelete();
                checkForDataDirectoriesToBalance();

                /**
                 * <p>Only download chunks to server if can write.</p>
                 */
                if (config.canWrite()) {

                    // Keep a reference of current server being checked
                    this.serverBeingIndexed = host;

                    // Set the activity so can know how long things are taking and so
                    // users can see current activity in configuration
                    this.setCurrentActivity(HashSpanFixingThread.ACTIVITY_DOWNLOADING);

                    if (hasConfigurationChangedCritically(config)) {
                        debugOut("1b. While checking for " + String.valueOf(isMetaData ? "meta data" : "data") + " chunks to fetch, configuration changed. Restarting.");
                        return ReturnType.CONT_OUTER_LOOP;
                    }

                    // update the offset
                    offset = offset.add(BigInteger.valueOf(chunks.length));

                    // break out if appropriate
                    if (ffts.isClosed() || isClosed) {
                        return ReturnType.EXIT;
                    }

                    // Check each hash to see if it is in the configured hash span
                    Set<HashSpan> hashSpans = getLocalServerTargetHashSpans();

                    HASHES:
                    for (int i = 0; i < chunks.length; i++) {

                        final BigHash nextHash = chunks[i];

                        currentDownloadDescription = String.valueOf(isMetaData ? "meta data" : "data") + " hash " + String.valueOf(i) + " of " + String.valueOf(chunks.length);

                        // Check to see whether should pause, and do so if specified
                        pauseBetweenOperations(config);

                        if (hasConfigurationChangedCritically(config)) {
                            debugOut("2. While checking for " + String.valueOf(isMetaData ? "meta data" : "data") + " chunks in hash span, configuration changed. Restarting.");
                            return ReturnType.CONT_OUTER_LOOP;
                        }
                        for (HashSpan hs : hashSpans) {
                            // if the data should be on this box, add it
                            if (hs.contains(nextHash)) {
                                try {

                                    boolean localHasChunk = false;

                                    try {
                                        if (isMetaData) {
                                            localHasChunk = ffts.getDataBlockUtil().hasMetaData(nextHash);
                                        } else {
                                            localHasChunk = ffts.getDataBlockUtil().hasData(nextHash);
                                        }
                                    } catch (UnexpectedEndOfDataBlockException ex) {

                                        // Send in the data block for salvaging and recreation. Wait a very brief
                                        // time (3s) to allow work to fire off, then return.
                                        this.ffts.getDataBlockUtil().repairCorruptedDataBlockForChunk(nextHash, 3000, "HashSpanFixingThread: Checking if has chunk already before downloading (1, iterating remote " + String.valueOf(isMetaData ? "meta data" : "data") + " hashes)");

                                        // Rethrow the exception so logs appropriately
                                        throw ex;
                                    }

                                    // Start with data. If local server doesn't have, add.
                                    if (!localHasChunk) {
                                        if (isMetaData) {

                                            // ------------------------------------------------------------------------
                                            // I'm unsure of status of IOUtil.getMetaData and IOUtil.getData. They
                                            // accept single hash but return two-dimensional array. Subject to change.
                                            // So check for type.
                                            //                                           Bryan, Sept 24 2009
                                            // ------------------------------------------------------------------------
                                            byte[] chunk = null;
                                            PropagationReturnWrapper prw = IOUtil.getMetaData(ts, nextHash, false);

                                            if (prw.isByteArraySingleDimension()) {
                                                chunk = (byte[]) prw.getReturnValueObject();
                                            } else if (prw.isByteArrayDoubleDimension()) {
                                                chunk = ((byte[][]) prw.getReturnValueObject())[0];
                                            } else {
                                                throw new AssertionFailedException("Expected single or double dimension array, but found neither.");
                                            }

                                            // If it is a bad chunk, don't store
                                            if (!isMetaDataChunkValid(chunk)) {
                                                continue HASHES;
                                            }

                                            ffts.getDataBlockUtil().addMetaData(nextHash, chunk);

                                            debugOut("Added meta data chunk: " + nextHash);
                                            // track for server config info
                                            this.metaDataCopied++;
                                        } else {

                                            // ------------------------------------------------------------------------
                                            // I'm unsure of status of IOUtil.getMetaData and IOUtil.getData. They
                                            // accept single hash but return two-dimensional array. Subject to change.
                                            // So check for type.
                                            //                                           Bryan, Sept 24 2009
                                            // ------------------------------------------------------------------------
                                            byte[] chunk = null;
                                            PropagationReturnWrapper prw = IOUtil.getData(ts, nextHash, false);

                                            if (prw.isByteArraySingleDimension()) {
                                                chunk = (byte[]) prw.getReturnValueObject();
                                            } else if (prw.isByteArrayDoubleDimension()) {
                                                chunk = ((byte[][]) prw.getReturnValueObject())[0];
                                            } else {
                                                throw new AssertionFailedException("Expected single or double dimension array, but found neither.");
                                            }

                                            // If it is a bad chunk, don't store
                                            if (!isDataChunkValid(chunk, nextHash)) {
                                                continue HASHES;
                                            }

                                            ffts.getDataBlockUtil().addData(nextHash, chunk);

                                            debugOut("Added data chunk: " + nextHash);
                                            // track for server config info
                                            this.dataCopied++;
                                        }
                                        this.registerSuccessfulAction(HashSpanFixingThread.ACTIVITY_DOWNLOADING);
                                    } else {
                                        /**
                                         * Going to verify that the chunk is good! We have
                                         * a known problem: some data and meta data corrupted
                                         * due to Tranche 213 problems on verifyHost adapter, leading
                                         * to corrupted data that was injected to 209.
                                         *
                                         * This will really heal the network by replacing any bad data.
                                         */
                                        if (!isMetaData) {

                                            try {
                                                // -------------------------------------------------------------------------
                                                // + D + D + D + D + D + D + D + D + D + D + D + D + D + D + D + D +
                                                // -------------------------------------------------------------------------

                                                byte[] localDataChunk = ffts.getDataBlockUtil().getData(nextHash);

                                                // If chunk not found or invalid
                                                if (localDataChunk == null || !isDataChunkValid(localDataChunk, nextHash)) {
                                                    debugOut("Found a bad data chunk on local server. Going to attempt to replace...");

                                                    // Remove item for cache. May or may not be there.
                                                    if (this.ffts.getDataBlockUtil().isUseCache()) {
                                                        this.ffts.getDataBlockUtil().getDataBlockCache().remove(nextHash, false);
                                                    }

                                                    // Is this remote server's copy good?
//                                                    byte[] remoteDataBytes = (byte[]) IOUtil.getData(ts, nextHash, false).getReturnValueObject();
                                                    // ------------------------------------------------------------------------
                                                    // I'm unsure of status of IOUtil.getMetaData and IOUtil.getData. They
                                                    // accept single hash but return two-dimensional array. Subject to change.
                                                    // So check for type.
                                                    //                                           Bryan, Sept 24 2009
                                                    // ------------------------------------------------------------------------
                                                    byte[] remoteDataBytes = null;
                                                    PropagationReturnWrapper prw = IOUtil.getData(ts, nextHash, false);

                                                    if (prw.isByteArraySingleDimension()) {
                                                        remoteDataBytes = (byte[]) prw.getReturnValueObject();
                                                    } else if (prw.isByteArrayDoubleDimension()) {
                                                        remoteDataBytes = ((byte[][]) prw.getReturnValueObject())[0];
                                                    } else {
                                                        throw new AssertionFailedException("Expected single or double dimension array, but found neither.");
                                                    }

                                                    BigHash remoteVerifyHash = new BigHash(remoteDataBytes);

                                                    if (remoteVerifyHash.equals(nextHash)) {
                                                        ffts.getDataBlockUtil().deleteData(nextHash, "Healing thread: replace with valid chunk");
                                                        ffts.getDataBlockUtil().addData(nextHash, remoteDataBytes);
                                                        debugOut("... replaced data chunk!");
                                                        this.dataRepaired++;
                                                        this.registerSuccessfulAction(HashSpanFixingThread.ACTIVITY_HEALING);
                                                    } else {
                                                        debugOut("... didn't replace, remote copy bad also.");
                                                        this.dataNotRepaired++;
                                                    }

                                                } else {
                                                    // track for server config info
                                                    this.dataSkipped++;
                                                }
                                            } catch (UnexpectedEndOfDataBlockException ex) {

                                                // Send in the data block for salvaging and recreation. Wait a very brief
                                                // time (3s) to allow work to fire off, then return.
                                                this.ffts.getDataBlockUtil().repairCorruptedDataBlockForChunk(nextHash, 3000, "HashSpanFixingThread: iterating data chunks");

                                                continue;
                                            }
                                            // -------------------------------------------------------------------------
                                            // + D + D + D + D + D + D + D + D + D + D + D + D + D + D + D + D +
                                            // -------------------------------------------------------------------------

                                        } /**
                                         * Going to verify that the chunk is good! We have
                                         * a known problem: some data and meta data corrupted
                                         * due to Tranche 213 problems on verifyHost adapter, leading
                                         * to corrupted data that was injected to 209.
                                         *
                                         * This will really heal the network by replacing any bad meta data.
                                         */
                                        else if (isMetaData) {

                                            // -------------------------------------------------------------------------
                                            // + MD + MD + MD + MD + MD + MD + MD + MD + MD + MD + MD + MD + MD + MD +
                                            // -------------------------------------------------------------------------
                                            try {

                                                byte[] localMetaChunk = ffts.getDataBlockUtil().getMetaData(nextHash);

                                                // If chunk not found or invalid
                                                if (localMetaChunk == null || !isMetaDataChunkValid(localMetaChunk)) {
                                                    debugOut("Found a bad meta chunk on local server. Going to attempt to replace...");

                                                    try {

                                                        // ------------------------------------------------------------------------
                                                        // I'm unsure of status of IOUtil.getMetaData and IOUtil.getData. They
                                                        // accept single hash but return two-dimensional array. Subject to change.
                                                        // So check for type.
                                                        //                                           Bryan, Sept 24 2009
                                                        // ------------------------------------------------------------------------
                                                        byte[] remoteMetaChunk = null;
                                                        PropagationReturnWrapper prw = IOUtil.getMetaData(ts, nextHash, false);

                                                        if (prw.isByteArraySingleDimension()) {
                                                            remoteMetaChunk = (byte[]) prw.getReturnValueObject();
                                                        } else if (prw.isByteArrayDoubleDimension()) {
                                                            remoteMetaChunk = ((byte[][]) prw.getReturnValueObject())[0];
                                                        } else {
                                                            throw new AssertionFailedException("Expected single or double dimension array, but found neither.");
                                                        }

                                                        MetaData md = MetaDataUtil.read(new ByteArrayInputStream(remoteMetaChunk));

                                                        if (md == null) {
                                                            throw new Exception("No meta data found. Normally throws exception.");
                                                        }

                                                        // Everything's good, replace
                                                        ffts.getDataBlockUtil().deleteMetaData(nextHash, "Healing thread: replace with valid chunk");
                                                        ffts.getDataBlockUtil().addMetaData(nextHash, remoteMetaChunk);
                                                        debugOut("... replaced meta chunk");

                                                        this.metaDataRepaired++;
                                                        this.registerSuccessfulAction(HashSpanFixingThread.ACTIVITY_HEALING);
                                                    } catch (Exception ex) {
                                                        debugOut("... didn't replace, remote copy bad also.");
                                                        this.metaDataNotRepaired++;
                                                    }
                                                }

                                                // Everything good, log for server config info
                                                this.metaDataSkipped++;
                                            } catch (UnexpectedEndOfDataBlockException ex) {

                                                // Send in the data block for salvaging and recreation. Wait a very brief
                                                // time (3s) to allow work to fire off, then return.
                                                this.ffts.getDataBlockUtil().repairCorruptedDataBlockForChunk(nextHash, 3000, "HashSpanFixingThread: iterating meta data chunks");

                                                continue;
                                            }
                                            // -------------------------------------------------------------------------
                                            // + MD + MD + MD + MD + MD + MD + MD + MD + MD + MD + MD + MD + MD + MD +
                                            // -------------------------------------------------------------------------
                                        }
                                    }
                                } catch (Exception ex) {
                                    System.err.println("Problem with " + String.valueOf(isMetaData ? "meta data" : "data") + " chunk: " + ex.getMessage() + ", hash: " + chunks[i]);
                                    ex.printStackTrace(System.err);
                                }
                            }
                        }
                    } // For all chunks in batch

                } // If server is not read-only

                if (ffts.isClosed() || isClosed) {
                    return ReturnType.EXIT;
                }
            } // For all known data chunks

        } finally {
            releaseConnection(host);
        }

        return ReturnType.VOID;
    } // iterateChunksOnHost

    /**
     * <p>Helper method to determine whether specific verifyHost is local.</p>
     * @param hostToCheck
     * @param config
     * @return
     */
    private boolean isLocalServerHost(String hostToCheck, Configuration config) {
        String localServerUrl = config.getValue(ConfigKeys.URL);
        if (localServerUrl != null) {
            String localHost = IOUtil.parseHost(localServerUrl);
            return localHost.equals(hostToCheck);
        }

        return false;
    }

    /**
     * <p>Returns all core servers that should receive chunk.</p>
     * <p>Server should receive chunk if:</p>
     * <ul>
     *   <li>Has target hash span that covers</li>
     *   <li>Doesn't have target hash span but has hash span that covers</li>
     * </ul>
     * @param h
     * @return
     */
    public String[] getHostsToReceiveChunk(BigHash h) {
        Set<String> hostsSet = new HashSet();

        ROWS:
        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {

            if (!row.isCore() || !row.isOnline() || !row.isWritable()) {
                continue ROWS;
            }

            // If any target hash spans, use those; otherwise, use hash spans
            for (HashSpan hs : row.getTargetHashSpans()) {
                if (hs.contains(h)) {
                    hostsSet.add(row.getHost());
                    continue ROWS;
                }
            }
        }

        return hostsSet.toArray(new String[0]);
    }

    /**
     * <p>Get a connected verifyHost, preferring those that include in hash span, but defaulting to random server. Uses following precedence:</p>
     * <ul>
     *   <li>If server contains in target hash span</li>
     *   <li>If server contains in normal hash span</li>
     *   <li>Random connected server</li>
     * </ul>
     * @param h
     * @return
     */
    public String getConnectedHost(BigHash h) {

        // Try to find verifyHost with target hash span. Efficient since helps balance
        // network if verifyHost doesn't have chunk yet.
        HOSTS:
        for (String connectedHost : ConnectionUtil.getConnectedHosts()) {
            StatusTableRow row = NetworkUtil.getStatus().getRow(connectedHost);
            if (row == null) {
                continue HOSTS;
            }
            if (!row.isOnline()) {
                continue HOSTS;
            }
            for (HashSpan hs : row.getTargetHashSpans()) {
                if (hs.contains(h)) {
                    return connectedHost;
                }
            }
        }

        // Try to find verifyHost with correct hash span
        HOSTS:
        for (String connectedHost : ConnectionUtil.getConnectedHosts()) {

            StatusTableRow row = NetworkUtil.getStatus().getRow(connectedHost);
            if (row == null) {
                continue HOSTS;
            }
            if (!row.isOnline()) {
                continue HOSTS;
            }
            for (HashSpan hs : row.getHashSpans()) {
                if (hs.contains(h)) {
                    return connectedHost;
                }
            }
        }

        // If verifyHost not found, just pick a connected verifyHost at random
        HOSTS:
        for (String connectedHost : ConnectionUtil.getConnectedHosts()) {

            StatusTableRow row = NetworkUtil.getStatus().getRow(connectedHost);
            if (row == null) {
                continue HOSTS;
            }
            if (!row.isOnline()) {
                continue HOSTS;
            }
            return connectedHost;
        }

        return null;
    }

    /**
     * <p>Get servers. Might be core or test depending on environment.</p>
     * @return
     * @throws java.lang.Exception
     */
    public String[] getHostsToUse() throws Exception {
        String[] hostsArr = null;

        /**
         * If user/admin set servers to use, use those
         */
        if (hostsToUse.size() > 0) {
            hostsArr = hostsToUse.toArray(new String[0]);
        } /**
         * Normally, we just use the core servers
         */
        else {
            StatusTable table = NetworkUtil.getStatus();
            Set<String> hosts = new HashSet();
            for (StatusTableRow row : table.getRows()) {
                if (row.isCore()) {
                    hosts.add(row.getHost());
                }
            }
            hostsArr = hosts.toArray(new String[0]);
        }

        // Put in a set to make sure no redundant values.
        Set<String> hostsSet = new HashSet();

        for (String host : hostsArr) {
            hostsSet.add(host);
        }

        if (hostsArr.length != hostsSet.size()) {
            System.err.println("HASH_SPAN_FIXING_THREAD> Must be a redundant value: array has " + hostsArr.length + " items, but set has " + hostsSet.size() + ". Going to use the set. Note this could have been causing problems before with deletions if same server counted multiple times.");
        }

        return hostsSet.toArray(new String[0]);
    }

    /**
     * <p>Check to see whether a server is really online. Uses multiple heuristics.</p>
     * @param verifyHost
     * @return
     */
    private boolean isServerReallyOnline(final String host) {
        try {
            return NetworkUtil.getStatus().getRow(host).isOnline();
        } catch (NullPointerException npe) { /* nope */ }
        return false;
    }

    /**
     * 
     * @return
     */
    private Set<HashSpan> getLocalServerHashSpans() {
        StatusTableRow row = null;

        // If testing, multiple local server's. Find the correct one.
        if (TestUtil.isTesting()) {
            String hostName = ffts.getHost();
            row = NetworkUtil.getStatus().getRow(hostName);
        }

        if (row == null) {
            row = NetworkUtil.getLocalServerRow();
        }

        Set<HashSpan> hashSpans = new HashSet();
        hashSpans.addAll(row.getHashSpans());

        return hashSpans;
    }

    /**
     * 
     * @return
     */
    private Set<HashSpan> getLocalServerTargetHashSpans() {
        StatusTableRow row = null;

        // If testing, multiple local server's. Find the correct one.
        if (TestUtil.isTesting()) {
            String hostName = ffts.getHost();
            row = NetworkUtil.getStatus().getRow(hostName);
        }

        if (row == null) {
            row = NetworkUtil.getLocalServerRow();
        }

        Set<HashSpan> hashSpans = new HashSet();
        hashSpans.addAll(row.getTargetHashSpans());

        return hashSpans;
    }

    /**
     * <p>Returns target hash spans, and defaults to regular hash spans.</p>
     * @param verifyHost
     * @return
     */
    private Set<HashSpan> getHashSpans(String host) {
        StatusTableRow row = NetworkUtil.getStatus().getRow(host);
        Set<HashSpan> hashSpans = new HashSet();

        if (row != null) {
            if (row.getTargetHashSpans().size() > 0) {
                hashSpans.addAll(row.getTargetHashSpans());
            } else {
                hashSpans.addAll(row.getHashSpans());
            }
        }

        return hashSpans;
    }

    /**
     * <p>Reports exceptions and handles any exceptions.</p>
     * @param verifyHost
     * @param ex
     */
    private void reportException(String host, Exception ex) {
        try {
            ConnectionUtil.reportException(NetworkUtil.getStatus().getRow(host), ex);
        } catch (NullPointerException npe) { /* nope */ }
    }

    /**
     * <p>Helper method to determine if data chunk is valid.</p>
     * @param chunk
     * @param hash
     * @return
     */
    private boolean isDataChunkValid(byte[] chunk, BigHash hash) {
        BigHash verifyHash = new BigHash(chunk);
        return verifyHash.equals(hash);
    }

    /**
     * <p>Helper method to determine if meta data chunk is valid.</p>
     * @param chunk
     * @return
     */
    private boolean isMetaDataChunkValid(byte[] chunk) {

        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(chunk);
            MetaData md = MetaDataUtil.read(bais);

            if (md == null) {
                throw new Exception("Meta data is null; should have thrown exception.");
            }
            return true;
        } catch (Exception nope) { /* nope */ } finally {
            IOUtil.safeClose(bais);
        }
        return false;
    }

    /**
     * <p>This verifies a batch of hashes on a server. Intended for a server without a HashSpan so can repair existing data.</p>
     * @param config
     */
    private void checkForChunksToRepair() {

        /**
         * <p>Must be able to read and write to server to repair. Why?:</p>
         * <ul>
         *   <li>Write: often flagged due to disk problems. Don't want to trigger.</li>
         *   <li>Read: don't want to update timestamp in log for chunk might be deleting soon (see ServerStartupThread).</li>
         * </ul>
         */
        if (!this.config.canRead() || !this.config.canWrite()) {
            try {
                Thread.sleep(this.getPauseBetweenOperations(this.config));
            } catch (Exception nope) { /* nope */ }
            return;
        }

        BigInteger batchSizes = BigInteger.valueOf(this.getBatchSizeForChunksToHeal(lastConfig));

        // If config changed, wait
        try {
            ffts.waitToLoadExistingDataBlocks();
        } catch (Exception ex) {
            // Nope, move on
        }

        try {

            // Set the activity so can know how long things are taking and so
            // users can see current activity in configuration
            this.setCurrentActivity(HashSpanFixingThread.ACTIVITY_HEALING);

            debugOut("repairLocalChunks() for " + IOUtil.createURL(ffts) + ", using " + getHostsToUse().length + " other servers to repair.");

            long dataChunkIndex = getRepairDataIndex();
            BigInteger bigOffset = BigInteger.valueOf(dataChunkIndex);

            BigHash[] dataHashes = ffts.getDataHashes(bigOffset, batchSizes);

            // If completed all chunks, then restart. We'll know its complete
            // if we get an empty batch back at current offset.
            if (dataHashes.length == 0 && !bigOffset.equals(BigInteger.ZERO)) {
                bigOffset = BigInteger.ZERO;
                dataHashes = ffts.getDataHashes(bigOffset, batchSizes);
            }

            debugOut("Found " + dataHashes.length + " data hashes.");

            int count = 0;
            for (BigHash h : dataHashes) {

                count++;

                // Set the description of the current activity
                currentHealingDescription = "Data hash " + String.valueOf(count) + " of " + String.valueOf(dataHashes.length);

                // Check to see whether should pause, and do so if specified
                pauseBetweenOperations(config);

                if (hasConfigurationChangedCritically(config)) {
                    debugOut("A. While looking for data to heal, configuration changed. Restarting.");
                    return;
                }

                try {

                    byte[] localDataChunk = null;

                    try {
                        if (IOUtil.hasData(ffts, h)) {
                            localDataChunk = ffts.getDataBlockUtil().getData(h);
                        } else {
                            continue;
                        }
                    } catch (UnexpectedEndOfDataBlockException ex) {

                        // Send in the data block for salvaging and recreation. Wait a very brief
                        // time (3s) to allow work to fire off, then return.
                        this.ffts.getDataBlockUtil().repairCorruptedDataBlockForChunk(h, 3000, "HashSpanFixingThread: Getting data chunk to see whether needs to be repaired");

                        // Rethrow the exception so logs appropriately
                        throw ex;
                    }

                    if (!isDataChunkValid(localDataChunk, h)) {
                        debugOut("Found bad chunk in repairLocalChunks");

                        // Remove item for cache. May or may not be there.
                        if (this.ffts.getDataBlockUtil().isUseCache()) {
                            this.ffts.getDataBlockUtil().getDataBlockCache().remove(h, false);
                        }

                        boolean isReplaced = false;

                        // Find a connected verifyHost, preferrably with chunk in hash span
                        String host = getConnectedHost(h);

                        try {

                            if (host == null) {
                                throw new AssertionFailedException("No host was found!?!");
                            }

                            TrancheServer ts = getConnection(host);

                            BigHash[] singleHashArr = {h};
                            PropagationReturnWrapper prw = ts.getData(singleHashArr, true);

                            if (!prw.isByteArrayDoubleDimension()) {
                                throw new AssertionFailedException("Expecting 2 dimensional array, but wasn't found.");
                            }

                            byte[][] returnedArr = (byte[][]) prw.getReturnValueObject();

                            if (returnedArr.length != 1) {
                                throw new AssertionFailedException("Expecting 1 item in array, instead found: " + returnedArr.length);
                            }

                            byte[] remoteChunk = returnedArr[0];

                            if (isDataChunkValid(remoteChunk, h)) {

                                ffts.getDataBlockUtil().deleteData(h, "Healing thread: repairLocalChunks");
                                ffts.getDataBlockUtil().addData(h, remoteChunk);

                                debugOut("... replacing with remote chunk");
                                isReplaced = true;
                            }

                        } catch (AssertionFailedException afe) {
                            // Ignore for now
                        } catch (Exception ex) {
//                            reportException(verifyHost, ex);
                            // Could be a remote or local problem, so cannot report
                        } finally {
                            releaseConnection(host);
                        }

                        if (isReplaced) {
                            this.dataRepaired++;
                            this.registerSuccessfulAction(HashSpanFixingThread.ACTIVITY_HEALING);
                        } else {
                            safeAddLineToSaveFile(HashSpanFixingThread.BAD_DATA_CHUNK_SAVE_FILENAME, h.toBase64String());
                            this.dataNotRepaired++;
                        }
                    }

                } catch (Exception ex) {
                    System.err.println("Unexpected error when repairing local data chunks: " + ex.getMessage());
                    ex.printStackTrace(System.err);
                    dataLocalChunkThrewException++;
                }
            }

            // Save the state of the data chunks
            setRepairDataIndex(bigOffset.longValue() + dataHashes.length);

            // Keep a reference of current server being checked
            this.serverBeingIndexed = "Unknown";

            long metaDataChunkIndex = getRepairMetaDataIndex();
            bigOffset = BigInteger.valueOf(metaDataChunkIndex);

            BigHash[] metaHashes = ffts.getMetaDataHashes(bigOffset, batchSizes);

            // If completed all chunks, then restart. We'll know its complete
            // if we get an empty batch back at current offset.
            if (metaHashes.length == 0 && !bigOffset.equals(BigInteger.ZERO)) {
                bigOffset = BigInteger.ZERO;
                metaHashes = ffts.getMetaDataHashes(bigOffset, batchSizes);
            }

            debugOut("Found " + metaHashes.length + " meta data hashes.");

            count = 0;
            for (BigHash h : metaHashes) {

                count++;

                // Set the description of the current activity
                currentHealingDescription = "Meta data hash " + String.valueOf(count) + " of " + String.valueOf(metaHashes.length);

                // Check to see whether should pause, and do so if specified
                pauseBetweenOperations(config);

                if (hasConfigurationChangedCritically(config)) {
                    debugOut("B. While looking for meta data to heal, configuration changed. Restarting.");
                    return;
                }
                try {
                    byte[] localMetaChunk = null;

                    try {
                        if (IOUtil.hasMetaData(ffts, h)) {
                            localMetaChunk = ffts.getDataBlockUtil().getMetaData(h);
                        } else {
                            continue;
                        }
                    } catch (UnexpectedEndOfDataBlockException ex) {

                        // Send in the data block for salvaging and recreation. Wait a very brief
                        // time (3s) to allow work to fire off, then return.
                        this.ffts.getDataBlockUtil().repairCorruptedDataBlockForChunk(h, 3000, "HashSpanFixingThread: Getting meta data chunk to see whether needs to be repaired");

                        // Rethrow the exception so logs appropriately
                        throw ex;
                    }

                    if (!isMetaDataChunkValid(localMetaChunk)) {
                        debugOut("Found bad chunk in repairLocalChunks");

                        // Remove item for cache. May or may not be there.
                        if (this.ffts.getDataBlockUtil().isUseCache()) {
                            this.ffts.getDataBlockUtil().getDataBlockCache().remove(h, true);
                        }

                        boolean isReplaced = false;

                        // Find a connected verifyHost, preferrably with chunk in hash span
                        String host = getConnectedHost(h);

                        try {

                            if (host == null) {
                                throw new AssertionFailedException("No host was found!?!");
                            }

                            TrancheServer ts = getConnection(host);

                            BigHash[] singleHashArr = {h};
                            PropagationReturnWrapper prw = ts.getMetaData(singleHashArr, true);

                            if (!prw.isByteArrayDoubleDimension()) {
                                throw new AssertionFailedException("Expecting 2 dimensional array, but wasn't found.");
                            }

                            byte[][] returnedArr = (byte[][]) prw.getReturnValueObject();

                            if (returnedArr.length != 1) {
                                throw new AssertionFailedException("Expecting 1 item in array, instead found: " + returnedArr.length);
                            }

                            byte[] remoteChunk = returnedArr[0];

                            if (isMetaDataChunkValid(remoteChunk)) {

                                ffts.getDataBlockUtil().deleteMetaData(h, "Healing thread: repairLocalChunks");
                                ffts.getDataBlockUtil().addMetaData(h, remoteChunk);

                                debugOut("... replacing with remote chunk");
                                isReplaced = true;
                            }

                        } catch (Exception ex) {
                            debugOut("Problems with remote server in repairLocalChunks: " + host);
                        } finally {
                            releaseConnection(host);
                        }

                        if (isReplaced) {
                            this.metaDataRepaired++;
                            this.registerSuccessfulAction(HashSpanFixingThread.ACTIVITY_HEALING);
                        } else {
                            safeAddLineToSaveFile(HashSpanFixingThread.BAD_META_DATA_CHUNK_SAVE_FILENAME, h.toBase64String());
                            this.metaDataNotRepaired++;
                        }
                    }
                } catch (Exception ex) {
                    debugOut("Unexpected error when repairing local meta data chunks: " + ex.getMessage());
                    ex.printStackTrace(System.err);
                    metaLocalDataChunkThreadException++;
                }
            }

            // Save the state of the meta data chunks
            setRepairMetaDataIndex(bigOffset.longValue() + metaHashes.length);

            localVerifyIteration++;

            // Check now to see whether need to reset by seeing whether data chunks.
            BigInteger testOffset = BigInteger.valueOf(localVerifyIteration * batchSizes.longValue());
            BigHash[] checkingOffset = ffts.getDataHashes(testOffset, batchSizes);

            // Nothing found, at end. Reset.
            if (checkingOffset.length < 1) {
                localVerifyIteration = 0;

                // Need to know if finished local chunks!
                localRepairCompleteIterations++;
            }
        } catch (Exception ex) {
            debugOut("Unexpected error when repairing local chunks in HashSpanFixingThread: " + ex.getMessage());
            ex.printStackTrace(System.err);
        } finally {
            // Set the activity so can know how long things are taking and so
            // users can see current activity in configuration
            this.setCurrentActivity(HashSpanFixingThread.ACTIVITY_NOTHING);

            // Keep a reference of current server being checked
            this.serverBeingIndexed = "Unknown";

            // Set the description of the current activity
            currentHealingDescription = null;
        }
    } // repairLocalChunks

    /**
     * 
     */
    private void checkForChunksToDelete() {

        // No deleting if no authority to set on other servers.
        final X509Certificate authCert = ffts.getAuthCert();
        final PrivateKey authKey = ffts.getAuthPrivateKey();

        if (authCert == null || authKey == null) {
            return;
        }

        /**
         * <p>Must be able to read to server to delete. Why?:</p>
         * <ul>
         *   <li>Write: often flagged due to disk problems, which we don't want to trigger.</li>
         *   <li>Read: this logic also replicates across network. Don't want to replicate chunks that might be deleted. (see: ServerStartupThread)</li>
         * </ul>
         */
        if (!this.config.canRead() || !this.config.canWrite()) {
            try {
                Thread.sleep(this.getPauseBetweenOperations(this.config));
            } catch (Exception nope) { /* nope */ }
            return;
        }

        // If config changed, wait
        try {
            ffts.waitToLoadExistingDataBlocks();
        } catch (Exception ex) {
            // Nope, move on
        }

        // Check whether configuration allows deleting
        if (!isAllowedToDelete(config)) {
            debugOut("Not allowed to delete, returning...");
            return;
        }

        try {
            // Set the activity so can know how long things are taking and so
            // users can see current activity in configuration
            setCurrentActivity(HashSpanFixingThread.ACTIVITY_DELETING);

            BigInteger batchSizes = BigInteger.valueOf(this.getBatchSizeForChunksToDelete(config));

            long metaDataIndex = getDeleteMetaDataIndex();
            BigInteger bigOffset = BigInteger.valueOf(metaDataIndex);

            try {
                int count = 0;
                BigHash[] metaDataHashes = ffts.getMetaDataHashes(bigOffset, batchSizes);

                if (metaDataHashes.length == 0 && !bigOffset.equals(BigInteger.ZERO)) {
                    bigOffset = BigInteger.ZERO;
                    metaDataHashes = ffts.getMetaDataHashes(bigOffset, batchSizes);
                }

                META:
                for (BigHash metaDataHash : metaDataHashes) {

                    count++;
                    // Set the description of the current activity
                    currentDeletionDescription = "Meta data chunk " + String.valueOf(count) + " of " + String.valueOf(metaDataHashes.length);

                    // Check to see whether should pause, and do so if specified
                    pauseBetweenOperations(config);

                    if (hasConfigurationChangedCritically(config)) {
                        debugOut("5. While checking for meta data outside hash span, configuration changed. Restarting.");
                        return;
                    }

                    if (isStickyChunk(metaDataHash, true)) {
                        continue META;
                    }

                    // Check if belongs on server
                    for (HashSpan span : getLocalServerTargetHashSpans()) {

                        // If contained in span or is sticky element
                        if (span.contains(metaDataHash)) {
                            continue META;
                        }
                    }

                    byte[] metaData = null;

                    try {
                        metaData = ffts.getDataBlockUtil().getMetaData(metaDataHash);

                        if (metaData == null) {
                            throw new AssertionFailedException("Since chunk is null, should have thrown exception.");
                        }

                        // Make sure its valid
                        ByteArrayInputStream bais = null;

                        try {
                            bais = new ByteArrayInputStream(metaData);
                            MetaData md = MetaDataUtil.read(bais);

                            if (md == null) {
                                throw new NullPointerException("Meta data object null, should have thrown exception.");
                            }
                        } finally {
                            IOUtil.safeClose(bais);
                        }

                    } catch (UnexpectedEndOfDataBlockException uex) {
                        this.ffts.getDataBlockUtil().repairCorruptedDataBlockForChunk(metaDataHash, 3000, "HashSpanFixingThread: Finding meta data to replicate and delete.");
                        continue META;
                    } catch (Exception ex) {
                        System.err.println(ex.getClass().getSimpleName() + " occurred while retrieving meta data hash from local server: " + ex.getMessage());
                        ex.printStackTrace(System.err);
                        continue META;
                    }

                    // Set to network. Propogate to all relevant servers
                    String[] hostsArr = getHostsToReceiveChunk(metaDataHash);
                    Set<String> hostsSet = new HashSet();

                    for (String host : hostsArr) {
                        if (!isLocalServerHost(host, config)) {
                            hostsSet.add(host);
                        }
                    }

                    // Warn if nothing found.
                    if (hostsArr.length == 0) {
                        System.err.println("Warning: found 0 servers for meta data chunk: " + metaDataHash);
                        continue;
                    }

                    // No warning. If true, the only server was local server.
                    if (hostsSet.size() == 0) {
                        continue;
                    }

                    Collection<MultiServerRequestStrategy> strategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hostsSet, Tertiary.DONT_CARE, Tertiary.TRUE);

                    if (strategies.size() == 0) {
                        System.err.println("Warning: couldn't find strategy to connect to servers!?!");
                        continue META;
                    }

                    MultiServerRequestStrategy strategy = strategies.toArray(new MultiServerRequestStrategy[0])[0];

                    boolean isDeleted = false;

                    try {
                        TrancheServer ts = getConnection(strategy.getHostReceivingRequest());

                        PropagationReturnWrapper prw = IOUtil.setMetaData(ts, authCert, authKey, true, metaDataHash, metaData, hostsArr);

//                        // Let's calculate the replications
//                        int reps = hostsSet.size();
//
//                        ERRORS:
//                        for (PropagationExceptionWrapper pew : prw.getErrors()) {
//                            // It's okay if chunk already exists...
//                            if (pew.exception instanceof ChunkAlreadyExistsSecurityException) {
//                                continue ERRORS;
//                            }
//                            // ... otherwise, assume server doesn't have
//                            reps--;
//                        }
//
//                        final boolean isEnoughToDelete = reps >= this.getTotalRequiredRepsToDelete(config);

                        // Let's count the reps
                        int reps = 0;

                        for (String verifyHost : hostsArr) {
                            try {
                                TrancheServer verifyTs = getConnection(verifyHost);
                                if (IOUtil.hasMetaData(verifyTs, metaDataHash)) {
                                    reps++;
                                }
                            } finally {
                                releaseConnection(verifyHost);
                            }
                        }

                        final boolean isEnoughToDelete = reps >= this.getTotalRequiredRepsToDelete(config);

                        if (isEnoughToDelete) {
                            this.ffts.getDataBlockUtil().deleteMetaData(metaDataHash, "Not in hash span; was replicated.");
                            isDeleted = true;
                        }

                    } catch (Exception e) {
                        reportException(strategy.getHostReceivingRequest(), e);
                        continue META;
                    } finally {
                        releaseConnection(strategy.getHostReceivingRequest());
                    }

                    if (isDeleted) {
                        debugOut("... deleted meta data chunk: " + metaDataHash);
                        // track for showing in server config
                        this.metaDataDeleted++;
                        this.registerSuccessfulAction(HashSpanFixingThread.ACTIVITY_DELETING);
                    }

                } // For each meta data

                setDeleteMetaDataIndex(bigOffset.longValue() + metaDataHashes.length);

                // Keep a reference of current server being checked
                this.serverBeingIndexed = "Unknown";

                long dataIndex = getDeleteDataIndex();
                bigOffset = BigInteger.valueOf(dataIndex);

                BigHash[] dataHashes = ffts.getDataHashes(bigOffset, batchSizes);

                if (dataHashes.length == 0 && !bigOffset.equals(BigInteger.ZERO)) {
                    bigOffset = BigInteger.ZERO;
                    dataHashes = ffts.getDataHashes(bigOffset, batchSizes);
                }

                count = 0;
                DATA:
                for (BigHash dataHash : dataHashes) {

                    count++;
                    // Set the description of the current activity
                    currentDeletionDescription = "Data chunk " + String.valueOf(count) + " of " + String.valueOf(dataHashes.length);

                    // Check to see whether should pause, and do so if specified
                    pauseBetweenOperations(config);

                    if (hasConfigurationChangedCritically(config)) {
                        debugOut("7. While checking for data outside hash span, configuration changed. Restarting.");
                        return;
                    }

                    if (isStickyChunk(dataHash, false)) {
                        continue DATA;
                    }

                    // Check if belongs on server
                    for (HashSpan span : getLocalServerTargetHashSpans()) {
                        // If in hash span or is a sticky element
                        if (span.contains(dataHash)) {
                            continue DATA;
                        }
                    }

                    byte[] data = null;

                    try {
                        data = ffts.getDataBlockUtil().getData(dataHash);

                        if (data == null) {
                            throw new AssertionFailedException("Since chunk is null, should have thrown exception.");
                        }

                        // Make sure its valid
                        BigHash verifyHash = new BigHash(data);
                        if (!verifyHash.equals(dataHash)) {
                            throw new Exception("Hashes don't match");
                        }

                    } catch (UnexpectedEndOfDataBlockException uex) {
                        this.ffts.getDataBlockUtil().repairCorruptedDataBlockForChunk(dataHash, 3000, "HashSpanFixingThread: Finding meta data to replicate and delete.");
                        continue DATA;
                    } catch (Exception ex) {
                        System.err.println(ex.getClass().getSimpleName() + " occurred while retrieving data hash from local server: " + ex.getMessage());
                        ex.printStackTrace(System.err);
                        continue DATA;
                    }

                    // Set to network. Propogate to all relevant servers
                    String[] hostsArr = getHostsToReceiveChunk(dataHash);
                    Set<String> hostsSet = new HashSet();

                    for (String host : hostsArr) {
                        if (!isLocalServerHost(host, config)) {
                            hostsSet.add(host);
                        }
                    }

                    // Warn if nothing found.
                    if (hostsArr.length == 0) {
                        System.err.println("Warning: found 0 servers for data chunk: " + dataHash);
                        continue;
                    }

                    // No warning. If true, the only server was local server.
                    if (hostsSet.size() == 0) {
                        continue;
                    }

                    Collection<MultiServerRequestStrategy> strategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hostsSet, Tertiary.DONT_CARE, Tertiary.TRUE);

                    if (strategies.size() == 0) {
                        System.err.println("Warning: couldn't find strategy to connect to servers!?!");
                        continue DATA;
                    }

                    MultiServerRequestStrategy strategy = strategies.toArray(new MultiServerRequestStrategy[0])[0];

                    boolean isDeleted = false;

                    try {
                        TrancheServer ts = getConnection(strategy.getHostReceivingRequest());

                        PropagationReturnWrapper prw = IOUtil.setData(ts, authCert, authKey, dataHash, data, hostsArr);

//                        // Let's calculate the replications
//                        int reps = hostsSet.size();
//
//                        ERRORS:
//                        for (PropagationExceptionWrapper pew : prw.getErrors()) {
//                            // It's okay if chunk already exists...
//                            if (pew.exception instanceof ChunkAlreadyExistsSecurityException) {
//                                continue ERRORS;
//                            }
//                            // ... otherwise, assume server doesn't have
//                            reps--;
//                        }
//
//                        final boolean isEnoughToDelete = reps >= this.getTotalRequiredRepsToDelete(config);

                        // Let's count the reps
                        int reps = 0;

                        for (String verifyHost : hostsArr) {
                            try {
                                TrancheServer verifyTs = getConnection(verifyHost);
                                if (IOUtil.hasData(verifyTs, dataHash)) {
                                    reps++;
                                }
                            } finally {
                                releaseConnection(verifyHost);
                            }
                        }

                        final boolean isEnoughToDelete = reps >= this.getTotalRequiredRepsToDelete(config);

                        if (isEnoughToDelete) {
                            this.ffts.getDataBlockUtil().deleteData(dataHash, "Not in hash span; was replicated.");
                            isDeleted = true;
                        }

                    } catch (Exception e) {
                        reportException(strategy.getHostReceivingRequest(), e);
                        continue DATA;
                    } finally {
                        releaseConnection(strategy.getHostReceivingRequest());
                    }

                    if (isDeleted) {
                        debugOut("... deleted data chunk: " + dataHash);
                        // track for showing in server config
                        this.dataDeleted++;
                        this.registerSuccessfulAction(HashSpanFixingThread.ACTIVITY_DELETING);
                    }

                } // For each data chunk

                setDeleteDataIndex(bigOffset.longValue() + dataHashes.length);
            } catch (Exception e) {
                e.printStackTrace();
            }

            localDeleteIteration++;

            // Check now to see whether need to reset by seeing whether data chunks.
            BigInteger testOffset = BigInteger.valueOf(localDeleteIteration * batchSizes.longValue());
            BigHash[] checkingDataOffset = ffts.getDataHashes(testOffset, batchSizes);
            BigHash[] checkingMetaOffset = ffts.getMetaDataHashes(testOffset, batchSizes);
            // Nothing found, at end. Reset.
            if (checkingDataOffset.length < 1 && checkingMetaOffset.length < 1) {
                localDeleteIteration = 0;

                // Need to know if finished local chunks!
                localDeleteCompleteIterations++;
            }

        } catch (Exception ex) {
            System.err.println("Unexpected exception while looking for chunks to delete: " + ex.getMessage());
            ex.printStackTrace(System.err);
        } finally {
            // Set the activity so can know how long things are taking and so
            // users can see current activity in configuration
            this.setCurrentActivity(HashSpanFixingThread.ACTIVITY_NOTHING);

            // Keep a reference of current server being checked
            this.serverBeingIndexed = "Unknown";

            // Set the description of the current activity
            currentDeletionDescription = null;
        }
    } // checkForChunksToDelete

    /**
     * <p>Checks if enough disparity among data directories, and balances if there is.</p>
     * <p>Moves up to n data blocks, where n is defined by attribute value controlling the batch size for balancing data directories.</p>
     */
    private void checkForDataDirectoriesToBalance() {
        /**
         * <p>Must be able to write to server to balance. Why?:</p>
         * <ul>
         *   <li>Write: Often flagged due to disk problems. Don't want to trigger.</li>
         * </ul>
         */
        if (!this.config.canWrite()) {
            try {
                Thread.sleep(this.getPauseBetweenOperations(this.config));
            } catch (Exception nope) { /* nope */ }
            return;
        }

        // If config changed, wait. This is especially important because the two activities
        // will mess each other up
        try {
            ffts.waitToLoadExistingDataBlocks();
        } catch (Exception ex) {
            // Nope, move on
        }

        try {
            // Set the activity so can know how long things are taking and so
            // users can see current activity in configuration
            this.setCurrentActivity(HashSpanFixingThread.ACTIVITY_BALANCING);

            final int numberDataBlocksToBalance = getBatchSizeForDataBlocksToBalance(config);
            for (int i = 0; i < numberDataBlocksToBalance; i++) {

                // Check to see whether should pause, and do so if specified
                pauseBetweenOperations(config);

                if (hasConfigurationChangedCritically(config)) {
                    debugOut("While checking for data directories to balance, configuration changed. Restarting.");
                    return;
                }

                if (!this.isAllowedToBalance(config)) {
                    return;
                }

                final long prevMoved = this.ffts.getDataBlockUtil().getTotalDataBlocksMovedWhileBalancing();

                final boolean wasAnythingMoved = this.ffts.getDataBlockUtil().balanceDataDirectoryConfigurations();

                if (wasAnythingMoved) {
                    debugOut("Moved total of " + String.valueOf(this.ffts.getDataBlockUtil().getTotalDataBlocksMovedWhileBalancing() - prevMoved) + " data blocks while balancing.");
                    this.registerSuccessfulAction(HashSpanFixingThread.ACTIVITY_BALANCING);
                }

                Thread.yield();
            }
        } catch (Exception ex) {
            System.err.println("Exception occurred while balancing data directories<" + ex.getClass().getSimpleName() + ">: " + ex.getMessage());
            ex.printStackTrace(System.err);
        } finally {
            // Set the activity so can know how long things are taking and so
            // users can see current activity in configuration
            this.setCurrentActivity(HashSpanFixingThread.ACTIVITY_NOTHING);
        }
    }
    private Configuration lastConfig = null;

    /**
     * <p>Check to see if a chunk belongs to a sticky project.</p>
     * @param hash
     * @param isMetaData
     * @return
     * @throws java.lang.Exception
     */
    private boolean isStickyChunk(BigHash hash, boolean isMetaData) throws Exception {
        // TODO> After safe delete ("doubly-linked" chunk headers in data block), implement
        return false;
    }
    private Set<String> hostsToRelease = new HashSet();

    /**
     * <p>Get the connection.</p>
     * <p>Make sure you call releaseConnection. Any locks will be released.</p>
     * @param verifyHost
     * @return
     * @throws java.lang.Exception
     */
    private TrancheServer getConnection(String host) throws Exception {
        boolean isConnected = ConnectionUtil.isConnected(host);

        if (isConnected) {
            return ConnectionUtil.getHost(host);
        } else {
            hostsToRelease.add(host);
            return ConnectionUtil.connectHost(host, true);
        }
    }

    /**
     * <p>Any locks will be released.</p>
     * @param verifyHost
     */
    private void releaseConnection(String host) {
        if (hostsToRelease.contains(host)) {
            hostsToRelease.remove(host);
            ConnectionUtil.unlockConnection(host);
            ConnectionUtil.safeCloseHost(host);
        }
    }

    /**
     * <p>Returns true if the server's Configuration has critical changes since we last got updated.</p>
     * <p>If not, updates configuration and returns false.</p>
     * <p>Note that critical changes are very expensive, so detecting them as opposed to small changes can save some duplicated effort.</p>
     * @param config
     * @return True if a critical change requires restart, false if can continue.
     */
    private boolean hasConfigurationChangedCritically(Configuration config) {

        // If changed so that server is not allowed to run, then we have a critical
        // change.
        if (!isAllowedToRun(config)) {
            return true;
        }

        /*
         * First check for timestamp change. If not, we know there wasn't a 
         * critical change.
         * 
         * However, just because timestamp changed doesn't mean critical. Later,
         * might change this value depending on other circumstances, such as
         * if hash spans are the same and other future possible criteria.
         */
        boolean wasCriticallyChanged = this.timestampLastGetConfiguration < this.ffts.getLastTimeConfigurationWasSet();

        /**
         * If changed, let's see if not critical. If not, can save a lot of duplicated work.
         */
        if (wasCriticallyChanged) {

            final Configuration updatedConfig = ffts.getConfiguration(false);
            final long updatedTimestamp = TimeUtil.getTrancheTimestamp();

            // Copy to avoid concurrent modification exceptions. Might be synchronized,
            // so also don't want to hold lock

            // >>> Verify hash spans <<<
            boolean isHashSpansChanged = false;
            {
                Set<HashSpan> currentHashSpans = new HashSet();
                currentHashSpans.addAll(config.getHashSpans());
                Set<HashSpan> newHashSpans = new HashSet();
                newHashSpans.addAll(updatedConfig.getHashSpans());

                // Check size. If different, we know its different.
                if (currentHashSpans.size() != newHashSpans.size()) {
                    isHashSpansChanged = true;
                }

                // Check whether new hash spans contain every hash span in current collection
                for (HashSpan span : currentHashSpans) {
                    if (isHashSpansChanged) {
                        break;
                    }
                    if (!newHashSpans.contains(span)) {
                        isHashSpansChanged = true;
                    }
                }

                // Check whether old hash spans contain every hash span in new collection
                for (HashSpan span : newHashSpans) {
                    if (isHashSpansChanged) {
                        break;
                    }
                    if (!currentHashSpans.contains(span)) {
                        isHashSpansChanged = true;
                    }
                }
            }

            // >>> Verify target hash spans <<<
            boolean isTargetHashSpansChanged = false;
            {
                Set<HashSpan> currentTargetHashSpans = new HashSet();
                currentTargetHashSpans.addAll(config.getTargetHashSpans());
                Set<HashSpan> newTargetHashSpans = new HashSet();
                newTargetHashSpans.addAll(updatedConfig.getTargetHashSpans());

                // Check size. If different, we know its different.
                if (currentTargetHashSpans.size() != newTargetHashSpans.size()) {
                    isTargetHashSpansChanged = true;
                }

                // Check whether new hash spans contain every hash span in current collection
                for (HashSpan span : currentTargetHashSpans) {
                    if (isTargetHashSpansChanged) {
                        break;
                    }
                    if (!newTargetHashSpans.contains(span)) {
                        isTargetHashSpansChanged = true;
                    }
                }

                // Check whether old hash spans contain every hash span in new collection
                for (HashSpan span : newTargetHashSpans) {
                    if (isTargetHashSpansChanged) {
                        break;
                    }
                    if (!currentTargetHashSpans.contains(span)) {
                        isTargetHashSpansChanged = true;
                    }
                }
            }

            // Check whether old hash spans contain every hash span in new collection

            /*
             * Right now, if hash span has not changed, we don't consider the
             * changes to be critical.
             * 
             * If not critical, update the configs reference and timestamp. If
             * critical, no need -- when loop restarts, these will be changed.
             */
            if (!isHashSpansChanged && !isTargetHashSpansChanged) {

                // Update reference
                config = updatedConfig;
                timestampLastGetConfiguration = updatedTimestamp;

                // Flag changes as not critical.
                wasCriticallyChanged = false;
            }
        }

        if (wasCriticallyChanged) {
            debugOut("There was a critical change to the configuration noticed at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
        }

        return wasCriticallyChanged;
    }

    /**
     * <p>Does nothing. Deprecated. Tests associated with 'concurrent' replication (injecting data and meta data chunks with same hash at same time) should be removed.</p>
     * @param isTesting boolean input
     * @deprecated Doesn't do anything. Logic to handle data and meta data chunks removed in favor of cleaner logic
     */
    public void setTestingConcurrentReplication(boolean isTesting) {
        // Nothing
    }
    /**
     * <p>Cache values so know when change.</p>
     */
    private boolean lastAllowedToRun = ConfigKeys.DEFAULT_SHOULD_HEALING_THREAD_RUN;

    /**
     * <p>This represents a parameter that is set in FlatFileTrancheServer's Configuration, and can be remotely changed. See param and return fields of this Javadoc for more information.</p>
     * @param conf The most recent Configuration from FlatFileTrancheServer.
     * @return True if should run, false if should suspend. Never stops thread -- just waits for true.
     */
    public boolean isAllowedToRun(Configuration conf) {

        boolean isRun = ConfigKeys.DEFAULT_SHOULD_HEALING_THREAD_RUN;

        try {
            isRun = Boolean.valueOf(conf.getValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN));
        } catch (Exception nope) { /* Skip -- either none or parse error, fallback on default */ }

        if (isRun != lastAllowedToRun) {
            debugOut("Changing allowed to run from " + lastAllowedToRun + " to " + isRun);
            lastAllowedToRun = isRun;
        }

        return isRun;
    }
    /**
     * <p>Cache values so know when change.</p>
     */
    private boolean lastAllowedToDelete = ConfigKeys.DEFAULT_SHOULD_HEALING_THREAD_DELETE;

    /**
     * <p>This represents a parameter that is set in FlatFileTrancheServer's Configuration, and can be remotely changed. See param and return fields of this Javadoc for more information.</p>
     * @param conf The most recent Configuration from FlatFileTrancheServer.
     * @return True if thread is allowed to delete, false if not.
     */
    public boolean isAllowedToDelete(Configuration conf) {

        boolean isDelete = ConfigKeys.DEFAULT_SHOULD_HEALING_THREAD_DELETE;

        try {
            isDelete = Boolean.valueOf(conf.getValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_DELETE));
        } catch (Exception nope) { /* Skip -- either none or parse error, fallback on default */ }

        if (isDelete != lastAllowedToDelete) {
            debugOut("Changing allowed deletion from " + lastAllowedToDelete + " to " + isDelete);
            lastAllowedToDelete = isDelete;
        }

        return isDelete;
    }
    /**
     * <p>Cache values so know when change.</p>
     */
    private boolean lastAllowedToBalance = ConfigKeys.DEFAULT_SHOULD_HEALING_THREAD_BALANCE_DATA_BLOCKS;

    /**
     * <p>This represents a parameter that is set in FlatFileTrancheServer's Configuration, and can be remotely changed. See param and return fields of this Javadoc for more information.</p>
     * @param conf The most recent Configuration from FlatFileTrancheServer.
     * @return True if thread is allowed to balance data blocks, false if not.
     */
    public boolean isAllowedToBalance(Configuration conf) {

        boolean isBalance = ConfigKeys.DEFAULT_SHOULD_HEALING_THREAD_BALANCE_DATA_BLOCKS;

        try {
            isBalance = Boolean.valueOf(conf.getValue(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_BALANCE));
        } catch (Exception nope) { /* Skip -- either none or parse error, fallback on default */ }

        if (isBalance != lastAllowedToBalance) {
            debugOut("Changing allowed balance from " + lastAllowedToBalance + " to " + isBalance);
            lastAllowedToBalance = isBalance;
        }

        return isBalance;
    }
    /**
     * <p>Cache values so know when change.</p>
     */
    private int lastTotalRequiredRepsToDelete = ConfigKeys.DEFAULT_NUMBER_TOTAL_REPS_REQUIRED_DELETE;

    /**
     * <p>This represents a parameter that is set in FlatFileTrancheServer's Configuration, and can be remotely changed. See param and return fields of this Javadoc for more information.</p>
     * @param conf The most recent Configuration from FlatFileTrancheServer.
     * @return Number of total replications on all other servers required before deleting a chunk not in current hash span.
     */
    public int getTotalRequiredRepsToDelete(Configuration conf) {

        int size = ConfigKeys.DEFAULT_NUMBER_TOTAL_REPS_REQUIRED_DELETE;

        try {
            size = Integer.valueOf(conf.getValue(ConfigKeys.HASHSPANFIX_NUMBER_REPS_REQUIRED_DELETE));
            // Woah, this must always be at least one, or else everything is deleted
            if (size < 1) {
                size = 1;
            }
        } catch (Exception nope) { /* Skip -- either none or parse error, fallback on default */ }

        if (size != lastTotalRequiredRepsToDelete) {
            debugOut("Changing total number of reps required before deletion from " + lastTotalRequiredRepsToDelete + " to " + size);
            lastTotalRequiredRepsToDelete = size;
        }

        return size;
    }
    /**
     * <p>Cache values so know when change.</p>
     */
    private int lastRequiredRepsInHashSpanToDelete = ConfigKeys.DEFAULT_NUMBER_TOTAL_REPS_IN_HASH_SPAN_REQUIRED_DELETE;

    /**
     * <p>This represents a parameter that is set in FlatFileTrancheServer's Configuration, and can be remotely changed. See param and return fields of this Javadoc for more information.</p>
     * @param conf The most recent Configuration from FlatFileTrancheServer.
     * @return Total number of replications required on other servers with the appropriate hash span before deleting chunk on this server that is not in appropriate hash span.
     */
    public int getRequiredRepsInHashSpanToDelete(Configuration conf) {

        int size = ConfigKeys.DEFAULT_NUMBER_TOTAL_REPS_IN_HASH_SPAN_REQUIRED_DELETE;

        try {
            size = Integer.valueOf(conf.getValue(ConfigKeys.HASHSPANFIX_NUMBER_REPS_IN_HASH_SPAN_REQUIRED_DELETE));
        } catch (Exception nope) { /* Skip -- either none or parse error, fallback on default */ }

        if (size != lastRequiredRepsInHashSpanToDelete) {
            debugOut("Changing number of reps required in hash span before deletion from " + lastRequiredRepsInHashSpanToDelete + " to " + size);
            lastRequiredRepsInHashSpanToDelete = size;
        }

        return size;
    }
    /**
     * <p>Cache values so know when change.</p>
     */
    private double lastMinimumPercentageDifferenceToBalance = ConfigKeys.DEFAULT_REQUIRED_PERCENTAGE_DIFFERENCE_TO_BALANCE_DATA_DIRECTORIES;

    /**
     * <p>The required difference in percentage between the data directories with least and most available space before transfering.</p>
     * <p>This only matters if healing thread is set to auto balance.</p>
     * <p>Note that verifyHost the calculation is determined:</p>
     * <ol>
     *   <li><strong>Find data directory with most available space</strong>: Must have at least 1GB of space available (the size of 10 full DataBlock's). Calculate percentage of its available space is used.</li>
     *   <li><strong>Find data directory with least available space</strong>: Calculate percentage of its available space is used. Must meet user-defined minimum used percentage of its space. </li>
     *   <li><strong>Subtract the percentage difference of the least from the most</strong>: if difference meets required minimum difference, the data will be moved from one to the other.
     * </ol>
     * @param conf
     * @return The percentage, e.g., 15.0, required. Note should always be a positive value less than 100.0. (If user-defined value not in range, uses default.)
     */
    public double getRequiredPercentageDifferenceToBalanceDataDirectories(Configuration conf) {

        double percentageDifference = ConfigKeys.DEFAULT_REQUIRED_PERCENTAGE_DIFFERENCE_TO_BALANCE_DATA_DIRECTORIES;

        try {
            percentageDifference = Double.valueOf(conf.getValue(ConfigKeys.HASHSPANFIX_REQUIRED_PERCENTAGE_DIFFERENCE_TO_BALANCE_DATA_DIRECTORIES));
        } catch (Exception nope) { /* Skip -- either none or parse error, fallback on default */ }

        // Ignore if out of range
        if (percentageDifference < 0.0 || percentageDifference > 100.00) {
            return ConfigKeys.DEFAULT_REQUIRED_PERCENTAGE_DIFFERENCE_TO_BALANCE_DATA_DIRECTORIES;
        }

        if (lastMinimumPercentageDifferenceToBalance != percentageDifference) {
            debugOut("Changing minimum percentage differences from " + lastMinimumPercentageDifferenceToBalance + " to " + percentageDifference);
            lastMinimumPercentageDifferenceToBalance = percentageDifference;
        }

        return percentageDifference;
    }
    /**
     * <p>Cache values so know when change.</p>
     */
    private double lastMinimumPercentageForMostUsedDataDirectoryToBalance = ConfigKeys.DEFAULT_REQUIRED_PERCENTAGE_FOR_MOST_USED_DATA_DIRECTORY_BEFORE_BALANCE;

    /**
     * <p>The minimum percentage of its available space used by a data directory before it will be a candidate for balancing. This sets a threshold before data will be moved away toward other data blocks.</p>
     * <p>This only matters if healing thread is set to auto balance.</p>
     * <p>Note that this does not guarentee that data will be moved to other data directories. Three additional criteria must be met:</p>
     * <ul>
     *   <li>Balancing must be allowed</li>
     *   <li>Another data directory must have at least 1GB free space and</li>
     *   <li>the same data directory above must have a percentage difference with the candidate data directory that exceeds the minimum percentage difference rule.</li>
     * </ul>
     * <p>Note should always be a positive value less than 1.0.</p>
     * @param conf
     * @return The percentage, e.g., 60.0, required. Note should always be a positive value less than 100.0. (If user-defined value not in range, uses default.)
     */
    public double getRequiredPercentageForMostUsedDataDirectoryToBalance(Configuration conf) {

        double percentageDifference = ConfigKeys.DEFAULT_REQUIRED_PERCENTAGE_FOR_MOST_USED_DATA_DIRECTORY_BEFORE_BALANCE;

        try {
            percentageDifference = Double.valueOf(conf.getValue(ConfigKeys.HASHSPANFIX_REQUIRED_PERCENTAGE_FOR_MOST_USED_DATA_DIRECTORY_BEFORE_BALANCE));
        } catch (Exception nope) { /* Skip -- either none or parse error, fallback on default */ }

        // Ignore if out of range
        if (percentageDifference < 0.0 || percentageDifference > 100.00) {
            return ConfigKeys.DEFAULT_REQUIRED_PERCENTAGE_FOR_MOST_USED_DATA_DIRECTORY_BEFORE_BALANCE;
        }

        if (lastMinimumPercentageForMostUsedDataDirectoryToBalance != percentageDifference) {
            debugOut("Changing mimimum percentage of space for most used data directory from " + lastMinimumPercentageForMostUsedDataDirectoryToBalance + " to " + percentageDifference);
            lastMinimumPercentageForMostUsedDataDirectoryToBalance = percentageDifference;
        }

        return percentageDifference;
    }
    /**
     * <p>Cache values so know when change.</p>
     */
    private int lastBatchSizeForChunksToDownload = HashSpanFixingThread.DEFAULT_BATCH_SIZE_FOR_CHUNKS_TO_DOWNLOAD;

    /**
     * <p>The number of chunks to inspect for potential download at a time.</p>
     * <p>The following three items compete for single-thread's time:</p>
     * <ol>
     *   <li>Downloading chunks in a server's hash span</li>
     *   <li>Deleting chunks not in a server's hash span (if they have sufficient replications)</li>
     *   <li>Healing chunks that do not verify according to their hash</li>
     * </ol>
     * <p>By default, they have the same batch sizes. However, they might be adjusted to emphasize certain activities.</p>
     * @param conf
     * @return
     */
    public int getBatchSizeForChunksToDownload(Configuration conf) {
        int size = HashSpanFixingThread.DEFAULT_BATCH_SIZE_FOR_CHUNKS_TO_DOWNLOAD;
        try {
            size = Integer.valueOf(conf.getValue(ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_DOWNLOAD_CHUNKS));
        } catch (Exception nope) {
        }

        if (size != lastBatchSizeForChunksToDownload) {
            debugOut("Changing downloading batch size from " + lastBatchSizeForChunksToDownload + " to " + size);
            lastBatchSizeForChunksToDownload = size;
        }

        return size;
    }
    /**
     * <p>Cache values so know when change.</p>
     */
    private int lastBatchSizeForChunksToDelete = HashSpanFixingThread.DEFAULT_BATCH_SIZE_FOR_CHUNKS_TO_DELETE;

    /**
     * <p>The number of chunks to inspect for potential deletion at a time.</p>
     * <p>The following three items compete for single-thread's time:</p>
     * <ol>
     *   <li>Downloading chunks in a server's hash span</li>
     *   <li>Deleting chunks not in a server's hash span (if they have sufficient replications)</li>
     *   <li>Healing chunks that do not verify according to their hash</li>
     * </ol>
     * <p>By default, they have the same batch sizes. However, they might be adjusted to emphasize certain activities.</p>
     * @param conf
     * @return
     */
    public int getBatchSizeForChunksToDelete(Configuration conf) {
        int size = HashSpanFixingThread.DEFAULT_BATCH_SIZE_FOR_CHUNKS_TO_DELETE;
        try {
            size = Integer.valueOf(conf.getValue(ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_DELETE_CHUNKS));
        } catch (Exception nope) {
        }

        if (size != lastBatchSizeForChunksToDelete) {
            debugOut("Changing deleting batch size from " + lastBatchSizeForChunksToDelete + " to " + size);
            lastBatchSizeForChunksToDelete = size;
        }

        return size;
    }
    /**
     * <p>Cache values so know when change.</p>
     */
    private int lastBatchSizeForChunksToHeal = HashSpanFixingThread.DEFAULT_BATCH_SIZE_FOR_CHUNKS_TO_HEAL;

    /**
     * <p>The number of chunks to inspect for repair work at a time.</p>
     * <p>The following three items compete for single-thread's time:</p>
     * <ol>
     *   <li>Downloading chunks in a server's hash span</li>
     *   <li>Deleting chunks not in a server's hash span (if they have sufficient replications)</li>
     *   <li>Healing chunks that do not verify according to their hash</li>
     * </ol>
     * <p>By default, they have the same batch sizes. However, they might be adjusted to emphasize certain activities.</p>
     * @param conf
     * @return
     */
    public int getBatchSizeForChunksToHeal(Configuration conf) {

        int size = HashSpanFixingThread.DEFAULT_BATCH_SIZE_FOR_CHUNKS_TO_HEAL;
        try {
            size = Integer.valueOf(conf.getValue(ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_HEAL_CHUNKS));
        } catch (Exception nope) { /* Incorrect value */ }

        if (size != lastBatchSizeForChunksToHeal) {
            debugOut("Changing healing batch size from " + lastBatchSizeForChunksToHeal + " to " + size);
            lastBatchSizeForChunksToHeal = size;
        }

        return size;
    }
    /**
     * <p>Cache values so know when change.</p>
     */
    private int lastBatchSizeForDataBlocksToBalance = HashSpanFixingThread.DEFAULT_BATCH_SIZE_FOR_BLOCKS_TO_BALANCE;

    /**
     * <p>Get the number of DataBlock objects to balance between DataDirectoryConfiguration objects to check and balance before going on to the next task.</p>
     * @param conf
     * @return
     */
    public int getBatchSizeForDataBlocksToBalance(Configuration conf) {

        int size = HashSpanFixingThread.DEFAULT_BATCH_SIZE_FOR_BLOCKS_TO_BALANCE;
        try {
            size = Integer.valueOf(conf.getValue(ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_BALANCE_DIRECTORIES));
        } catch (Exception nope) { /* Incorrect value */ }

        if (size != lastBatchSizeForDataBlocksToBalance) {
            debugOut("Changing balancing batch size from " + lastBatchSizeForDataBlocksToBalance + " to " + size);
            lastBatchSizeForDataBlocksToBalance = size;
        }

        return size;
    }
    private long lastPauseInMillis = HashSpanFixingThread.DEFAULT_MILLIS_PAUSE_BETWEEN_OPERATIONS;

    /**
     * <p>Pause amount of time based on configuration attributes.</p>
     * @param conf
     * @throws java.lang.Exception
     */
    private void pauseBetweenOperations(Configuration conf) throws Exception {
        long pause = getPauseBetweenOperations(conf);

        // Yield because it is polite
        if (pause > 0) {
            Thread.sleep(pause);
        }
    }

    /**
     * <p>Get pause to relieve resources based on configuration options.</p>
     * @param conf
     * @return
     * @throws java.lang.Exception
     */
    public long getPauseBetweenOperations(Configuration conf) throws Exception {
        long pause = HashSpanFixingThread.DEFAULT_MILLIS_PAUSE_BETWEEN_OPERATIONS;
        try {
            pause = Long.parseLong(conf.getValue(ConfigKeys.HASHSPANFIX_PAUSE_MILLIS));
        } catch (Exception nope) { /* Incorrect value */ }

        if (pause != lastPauseInMillis) {
            debugOut("Changing pause in healing thread between operations from " + lastPauseInMillis + " to " + pause);
            lastPauseInMillis = pause;
        }

        return pause;
    }

    /**
     * <p>Add a server to use. If any servers are add, the healing thread will stop using core server and will use these instead.</p>
     * <p>To use core servers again, you will have to clear the added servers to use. See clearServersToUse().</p>
     * @param verifyHost
     * @return True if added, false otherwise. Generally, will only return false if already added.
     */
    public boolean addServerHostToUse(String host) {
        boolean added = hostsToUse.add(host);

        if (added) {
            debugOut("Added server to use: " + host);
        }

        return added;
    }

    /**
     * <p>If specified alternative servers to use, remove one.</p>
     * @param verifyHost
     * @return
     */
    public boolean removeServerHostToUse(String host) {
        boolean removed = hostsToUse.remove(host);

        if (removed) {
            debugOut("Removed server to use: " + host);
        }

        return removed;
    }

    /**
     * <p>Clear out all servers added to use. This will cause the HashSpanFixingThread to use core servers again until more servers are added.</p>
     */
    public void clearServerHostsToUse() {
        hostsToUse.clear();
    }
    /**
     * <p>This variable is used to keep track of when activities started. This way, we will know how long each activity ran during this instace.</p>
     */
    private long lastActivityStartTimestamp = TimeUtil.getTrancheTimestamp();

    /**
     * <p>Set the current activity that the healing thread is doing.</p>
     * <p>Though single-threaded, synchronizing to assert that this value is accurate.</p>
     * @param activity
     */
    private void setCurrentActivity(byte activity) {

        // Before updating current activity, increment appropriate time for last activity
        switch (this.currentActivity) {
            case HashSpanFixingThread.ACTIVITY_DELETING:
                this.timeSpentDeleting += (TimeUtil.getTrancheTimestamp() - lastActivityStartTimestamp);
                break;
            case HashSpanFixingThread.ACTIVITY_DOWNLOADING:
                this.timeSpentDownloading += (TimeUtil.getTrancheTimestamp() - lastActivityStartTimestamp);
                break;
            case HashSpanFixingThread.ACTIVITY_HEALING:
                this.timeSpentHealing += (TimeUtil.getTrancheTimestamp() - lastActivityStartTimestamp);
                break;
            case HashSpanFixingThread.ACTIVITY_BALANCING:
                this.timeSpentBalancing += (TimeUtil.getTrancheTimestamp() - lastActivityStartTimestamp);
                break;
            case HashSpanFixingThread.ACTIVITY_NOTHING:
                this.timeSpentDoingNothing += (TimeUtil.getTrancheTimestamp() - lastActivityStartTimestamp);
                break;
        }

        // Start the clock for the current activity
        lastActivityStartTimestamp = TimeUtil.getTrancheTimestamp();

        // Update
        this.currentActivity = activity;
    }

    /**
     * <p>Find out what the server is doing.</p>
     * @return A byte representing an activity. See the static variables starting with ACTIVTY_
     */
    public String getCurrentActivity() {
        String currentActivityStr = null;
        switch (this.currentActivity) {
            case HashSpanFixingThread.ACTIVITY_NOTHING:
                currentActivityStr = "Other (starting up or house cleaning work)";
                break;
            case HashSpanFixingThread.ACTIVITY_DELETING:
                currentActivityStr = "Checking for chunks to delete";
                if (this.currentDeletionDescription != null) {
                    currentActivityStr += " (" + this.currentDeletionDescription + ")";
                }
                break;
            case HashSpanFixingThread.ACTIVITY_DOWNLOADING:
                currentActivityStr = "Checking for chunks to download";
                if (this.currentDownloadDescription != null) {
                    currentActivityStr += " (" + this.currentDownloadDescription + ")";
                }
                break;
            case HashSpanFixingThread.ACTIVITY_HEALING:
                currentActivityStr = "Checking for corrupted chunks to heal";
                if (this.currentHealingDescription != null) {
                    currentActivityStr += " (" + this.currentHealingDescription + ")";
                }
                break;
            case HashSpanFixingThread.ACTIVITY_BALANCING:
                currentActivityStr = "Balancing data directories (Total moved data blocks: " + this.ffts.getDataBlockUtil().getTotalDataBlocksMovedWhileBalancing() + ")";
                break;
            default:
                currentActivityStr = "Unknown";
                break;
        }

        return currentActivityStr;
    }

    /**
     * <p>Get time, in milliseconds, spent in household cleaning or waiting for startup.</p>
     * @return 
     */
    public long getTimeSpentDoingNothing() {
        long time = timeSpentDoingNothing;
        // If currently doing this activity, increment time by run time of this
        // current activity.
        if (this.currentActivity == HashSpanFixingThread.ACTIVITY_NOTHING) {
            time += (TimeUtil.getTrancheTimestamp() - lastActivityStartTimestamp);
        }
        return time;
    }

    /**
     * <p>Get time, in milliseconds, spent healing local data chunks.</p>
     * @return
     */
    public long getTimeSpentHealing() {
        long time = timeSpentHealing;
        // If currently doing this activity, increment time by run time of this
        // current activity.
        if (this.currentActivity == HashSpanFixingThread.ACTIVITY_HEALING) {
            time += (TimeUtil.getTrancheTimestamp() - lastActivityStartTimestamp);
        }
        return time;
    }

    /**
     * <p>Get time, in milliseconds, spent deleting inappropriate chunks on server.</p>
     * @return
     */
    public long getTimeSpentDeleting() {
        long time = timeSpentDeleting;
        // If currently doing this activity, increment time by run time of this
        // current activity.
        if (this.currentActivity == HashSpanFixingThread.ACTIVITY_DELETING) {
            time += (TimeUtil.getTrancheTimestamp() - lastActivityStartTimestamp);
        }
        return time;
    }

    /**
     * <p>Get time, in milliseconds, spent downloading new data that belongs on this server from other servers.</p>
     * @return
     */
    public long getTimeSpentDownloading() {
        long time = timeSpentDownloading;
        // If currently doing this activity, increment time by run time of this
        // current activity.
        if (this.currentActivity == HashSpanFixingThread.ACTIVITY_DOWNLOADING) {
            time += (TimeUtil.getTrancheTimestamp() - lastActivityStartTimestamp);
        }
        return time;
    }

    /**
     * <p>Get time, in milliseconds, spent balancing the data directories.</p>
     * @return
     */
    public long getTimeSpentBalancing() {
        long time = timeSpentBalancing;
        // If currently doing this activity, increment time by run time of this
        // current activity.
        if (this.currentActivity == HashSpanFixingThread.ACTIVITY_BALANCING) {
            time += (TimeUtil.getTrancheTimestamp() - lastActivityStartTimestamp);
        }
        return time;
    }

    /**
     * <p>Builds a string suitable for human viewing with percentage of the time of this thread doing "nothing" (loading or household cleaning).</p>
     * @return
     */
    public String getEstimatedPercentageTimeSpentDoingNothing() {
        return getEstimatedPercentageTimeSpent(HashSpanFixingThread.ACTIVITY_NOTHING);
    }

    /**
     * <p>Builds a string suitable for human viewing with percentage of the time of this thread doing "healing" (looking for corrupt chunks to fix).</p>
     * @return
     */
    public String getEstimatedPercentageTimeSpentHealing() {
        return getEstimatedPercentageTimeSpent(HashSpanFixingThread.ACTIVITY_HEALING);
    }

    /**
     * <p>Builds a string suitable for human viewing with percentage of the time of this thread doing "download" (looking for new chunks to download that belong to this server's hash span).</p>
     * @return
     */
    public String getEstimatedPercentageTimeSpentDownloading() {
        return getEstimatedPercentageTimeSpent(HashSpanFixingThread.ACTIVITY_DOWNLOADING);
    }

    /**
     * <p>Builds a string suitable for human viewing with percentage of the time of this thread doing "deleting" (looking for chunks to delete that don't belong on this server with sufficient replicates).</p>
     * @return
     */
    public String getEstimatedPercentageTimeSpentDeleting() {
        return getEstimatedPercentageTimeSpent(HashSpanFixingThread.ACTIVITY_DELETING);
    }

    /**
     * <p>Builds a string suitable for human viewing with percentage of the time of this thread "balancing"</p>
     * @return
     */
    public String getEstimatedPercentageTimeSpentBalancing() {
        return getEstimatedPercentageTimeSpent(HashSpanFixingThread.ACTIVITY_BALANCING);
    }

    /**
     * <p>Internal method for building percentage strings for the various activities.</p>
     * @param activity
     * @return
     */
    private String getEstimatedPercentageTimeSpent(byte activity) {

        // If activity not found, this default message will not change
        String percentageStr = "Cannot calculate percentage";

        // Find the denominator
        long nothingTime = getTimeSpentDoingNothing();
        long deletingTime = getTimeSpentDeleting();
        long healingTime = getTimeSpentHealing();
        long downloadingTime = getTimeSpentDownloading();
        long balancingTime = getTimeSpentBalancing();
        long totalTime = nothingTime + deletingTime + healingTime + downloadingTime;

        if (totalTime == 0) {
            percentageStr = "No activity yet";
        } else {
            double percentage = 0.0;
            switch (activity) {
                case HashSpanFixingThread.ACTIVITY_DELETING:
                    percentage = 100.0 * (double) deletingTime / (double) totalTime;
                    break;
                case HashSpanFixingThread.ACTIVITY_DOWNLOADING:
                    percentage = 100.0 * (double) downloadingTime / (double) totalTime;
                    break;
                case HashSpanFixingThread.ACTIVITY_HEALING:
                    percentage = 100.0 * (double) healingTime / (double) totalTime;
                    break;
                case HashSpanFixingThread.ACTIVITY_NOTHING:
                    percentage = 100.0 * (double) nothingTime / (double) totalTime;
                    break;
                case HashSpanFixingThread.ACTIVITY_BALANCING:
                    percentage = 100.0 * (double) balancingTime / (double) totalTime;
                    break;

                // If the activity is unknown, don't even try to guess time
                default:
                    return percentageStr;
            }

            // Make the percentage
            DecimalFormat twoPlaces = new DecimalFormat("###.00");
            percentageStr = twoPlaces.format(percentage) + "%";
        }

        return percentageStr;
    }

    /**
     * <p>Any time "healing" thread (HashSpanFixingThread) does something successful, adds to score. However, the weights of the activities are contextually determined.</p>
     * @param activity
     */
    private void registerSuccessfulAction(byte activity) {

        double percentagesSum = 0.0;
        int percentagesCount = 0;

        // Update the state
        CHECK_DDCS:
        for (DataDirectoryConfiguration ddc : this.config.getDataDirectories()) {
            double percentage = 100.0 * (double) ddc.getActualSize() / (double) ddc.getSizeLimit();

            // If percentage for any disk ever exceeds 90%, we're in low disk space mode!
            if (percentage > 90.0) {
                this.currentThreadState = ThreadState.HIGH;
                percentagesCount = -1;
                break CHECK_DDCS;
            } else {
                percentagesCount++;
                percentagesSum += percentage;
            }
        }

        if (percentagesCount > 0) {
            double percentage = percentagesSum / percentagesCount;
            if (percentage < 30.0) {
                this.currentThreadState = ThreadState.LOW;
            } else if (percentage > 80.0) {
                this.currentThreadState = ThreadState.HIGH;
            } else {
                this.currentThreadState = ThreadState.NORMAL;
            }
        }

        switch (activity) {
            case HashSpanFixingThread.ACTIVITY_DELETING:

                if (this.currentThreadState == ThreadState.HIGH) {
                    this.currentPerformanceScore += 10;
                } else {
                    this.currentPerformanceScore += 1;
                }

                break;
            case HashSpanFixingThread.ACTIVITY_DOWNLOADING:

                if (this.currentThreadState == ThreadState.LOW) {
                    this.currentPerformanceScore += 2;
                } else {
                    this.currentPerformanceScore += 1;
                }

                break;
            case HashSpanFixingThread.ACTIVITY_HEALING:
                if (this.currentThreadState == ThreadState.LOW) {
                    this.currentPerformanceScore += 2;
                } else {
                    this.currentPerformanceScore += 1;
                }
                break;
            case HashSpanFixingThread.ACTIVITY_NOTHING:
                // Worthless
                break;
            case HashSpanFixingThread.ACTIVITY_BALANCING:
                if (this.currentThreadState == ThreadState.HIGH) {
                    this.currentPerformanceScore += 10;
                } else {
                    this.currentPerformanceScore += 1;
                }
                break;
            default:
                System.err.println("Unknown action registered, skipping: " + activity);
                return;
        }
    }

    /**
     * <p>Current score for performance of the healing thread.</p>
     * <p>There are several internal heuristics for determining the scores, but basically scores are incremented when activities are productive. In some contexts (e.g., low disk space), some activities are more highly weighted (e.g., deleting unnecessary space).</p>
     * @return
     */
    public long getCurrentPerformanceScore() {
        return currentPerformanceScore;
    }

    /**
     * <p>The state of the healing thread:</p>
     * <ul>
     *      <li>Unknown: the state hasn't been determined yet</li>
     *      <li>Low: Disk usage is low</li>
     *      <li>Normal: Disk usage is normal</li>
     *      <li>High: One or more drives is at 90% use</li>
     * </ul>
     * @return
     */
    public String getCurrentThreadState() {
        switch (currentThreadState) {
            case UNKNOWN:
                return "Unknown";
            case LOW:
                return "Low";
            case NORMAL:
                return "Normal";
            case HIGH:
                return "High";
        }
        return "Error";
    }

    /**
     * 
     * @return
     * @throws java.io.IOException
     */
    public long getDeleteDataIndex() throws IOException {
        return getIndexFromSaveFile(DELETE_DATA_SAVE_FILENAME);
    }

    /**
     * 
     * @return
     * @throws java.io.IOException
     */
    public long getDeleteMetaDataIndex() throws IOException {
        return getIndexFromSaveFile(DELETE_META_DATA_SAVE_FILENAME);
    }

    /**
     * 
     * @return
     * @throws java.io.IOException
     */
    public long getDownloadDataIndex() throws IOException {
        return getIndexFromSaveFile(DOWNLOAD_DATA_INDEX_SAVE_FILENAME);
    }

    /**
     * 
     * @return
     * @throws java.io.IOException
     */
    public long getDownloadMetaDataIndex() throws IOException {
        return getIndexFromSaveFile(DOWNLOAD_META_DATA_INDEX_SAVE_FILENAME);
    }

    /**
     * 
     * @return
     * @throws java.io.IOException
     */
    public long getRepairDataIndex() throws IOException {
        return getIndexFromSaveFile(REPAIR_DATA_SAVE_FILENAME);
    }

    /**
     * 
     * @return
     * @throws java.io.IOException
     */
    public long getRepairMetaDataIndex() throws IOException {
        return getIndexFromSaveFile(REPAIR_META_DATA_SAVE_FILENAME);
    }

    /**
     * <p>Read the index from the save file.</p>
     * @param name The simple name (not the path) of the file we want to read
     * @return
     */
    private long getIndexFromSaveFile(String name) throws IOException {
        long index = 0;

        synchronized (name) {
            File file = new File(ffts.getHomeDirectory(), name);

            // If file doesn't exist yet, starting from beginning
            if (!file.exists()) {
                return 0;
            }

            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(file));
                index = Long.parseLong(in.readLine());
            } catch (NumberFormatException nfe) {
                return 0;
            } finally {
                IOUtil.safeClose(in);
            }
        }

        return index;
    }

    /**
     * 
     * @return
     * @throws java.io.IOException
     */
    public String getDownloadServer() throws IOException {
        return getStringFromSaveFile(DOWNLOAD_SERVER_SAVE_FILENAME);
    }

    /**
     * <p>Read a string from a save file.</p>
     * @param name
     * @return
     * @throws java.io.IOException
     */
    private String getStringFromSaveFile(String name) throws IOException {
        String str = null;

        synchronized (name) {
            File file = new File(ffts.getHomeDirectory(), name);

            // If file doesn't exist yet, starting from beginning
            if (!file.exists()) {
                return null;
            }

            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(file));
                str = in.readLine();
            } finally {
                IOUtil.safeClose(in);
            }
        }

        return str;
    }

    /**
     * 
     * @param index
     * @throws java.io.IOException
     */
    private void setDeleteDataIndex(long index) throws IOException {
        saveIndexToSaveFile(DELETE_DATA_SAVE_FILENAME, index);
    }

    /**
     * 
     * @param index
     * @throws java.io.IOException
     */
    private void setDeleteMetaDataIndex(long index) throws IOException {
        saveIndexToSaveFile(DELETE_META_DATA_SAVE_FILENAME, index);
    }

    /**
     * 
     * @param index
     * @throws java.io.IOException
     */
    private void setDownloadDataIndex(long index) throws IOException {
        saveIndexToSaveFile(DOWNLOAD_DATA_INDEX_SAVE_FILENAME, index);
    }

    /**
     * 
     * @param index
     * @throws java.io.IOException
     */
    private void setDownloadMetaDataIndex(long index) throws IOException {
        saveIndexToSaveFile(DOWNLOAD_META_DATA_INDEX_SAVE_FILENAME, index);
    }

    /**
     * 
     * @param index
     * @throws java.io.IOException
     */
    private void setRepairDataIndex(long index) throws IOException {
        saveIndexToSaveFile(REPAIR_DATA_SAVE_FILENAME, index);
    }

    /**
     * 
     * @param index
     * @throws java.io.IOException
     */
    private void setRepairMetaDataIndex(long index) throws IOException {
        saveIndexToSaveFile(REPAIR_META_DATA_SAVE_FILENAME, index);
    }

    /**
     * 
     * @param name
     * @param index
     * @return
     * @throws java.io.IOException
     */
    private void saveIndexToSaveFile(String name, long index) throws IOException {
        synchronized (name) {
            File file = new File(ffts.getHomeDirectory(), name);

            // Always clobber
            BufferedWriter out = null;
            try {
                out = new BufferedWriter(new FileWriter(file, false));
                out.write(String.valueOf(index));
                out.newLine();
                out.flush();
            } finally {
                IOUtil.safeClose(out);
            }
        }
    }

    /**
     * 
     * @param verifyHost
     * @throws java.io.IOException
     */
    private void setDownloadServer(String host) throws IOException {
        saveStringToSaveFile(DOWNLOAD_SERVER_SAVE_FILENAME, host);
    }

    /**
     * 
     */
    private void clearDownloadServer() {
        synchronized (DOWNLOAD_SERVER_SAVE_FILENAME) {
            File file = new File(ffts.getHomeDirectory(), DOWNLOAD_SERVER_SAVE_FILENAME);
            IOUtil.safeDelete(file);
        }
    }

    /**
     * 
     * @param name
     * @param str
     * @throws java.io.IOException
     */
    private void saveStringToSaveFile(String name, String str) throws IOException {
        synchronized (name) {
            File file = new File(ffts.getHomeDirectory(), name);

            // Always clobber
            BufferedWriter out = null;
            try {
                out = new BufferedWriter(new FileWriter(file, false));
                out.write(str);
                out.newLine();
                out.flush();
            } finally {
                IOUtil.safeClose(out);
            }
        }
    }

    /**
     *
     * @param name
     * @param line
     */
    private void safeAddLineToSaveFile(String name, String line) {
        try {
            synchronized (name) {
                File file = new File(ffts.getHomeDirectory(), name);

                // Always append
                BufferedWriter out = null;
                try {
                    out = new BufferedWriter(new FileWriter(file, true));
                    out.write(line);
                    out.newLine();
                    out.flush();
                } finally {
                    IOUtil.safeClose(out);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + " occurred while trying to add line<" + line + "> to file<" + name + "> at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()) + ": " + e.getMessage());
        }
    }

    /**
     * <p>Close the thread down. Frees associated resources.</p>
     */
    public void close() {
        isClosed = true;
        this.hashSpanFixingThreadPolicyDecider.close();
    }

    /**
     * <p>Time in milliseconds to sleep when not allowed to run before retrying.</p>
     * @return
     */
    public static long getTimeToSleepIfNotAllowedToRun() {
        return timeToSleepIfNotAllowedToRun;
    }

    /**
     * <p>Time in milliseconds to sleep when not allowed to run before retrying.</p>
     * @param time
     */
    public static void setTimeToSleepIfNotAllowedToRun(long time) {
        timeToSleepIfNotAllowedToRun = time;
    }
}
