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

import java.util.HashMap;
import java.util.Map;
import org.tranche.flatfile.DataBlock;

/**
 * <p>A library of attribute key names to use in the server's configuration.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ConfigKeys {

    private static final Permissions CAN_READ_EDIT_DELETE = new Permissions(true, true, true);
    private static final Permissions CAN_READ_EDIT = new Permissions(true, false, true);
    private static final Permissions CAN_READ = new Permissions(false, false, true);
    private static final Permissions NO_PERMISSIONS = new Permissions(false, false, false);
    /**
     * <p>The version of code the server is running.</p>
     */
    public static final String BUILD_NUMBER = "buildNumber";
    /**
     * <p>True if TargetHashSpanThread is busy running.</p>
     */
    public static final String TARGET_HASH_SPAN_THREAD_WORKING = "targetHashSpanThread: working";
    /**
     * <p>Any messages from TargetHashSpanThread about its progress. Totally arbitrary.</p>
     */
    public static final String TARGET_HASH_SPAN_THREAD_MESSAGE = "targetHashSpanThread: message";
    /**
     * <p>The default value for config option to allow healing thread to run.</p>
     * <p>If  set to false by user in configuration, the thread will suspend until 
     * set back to true.</p>
     */
    public static final boolean DEFAULT_SHOULD_HEALING_THREAD_RUN = true;
    /**
     * 
     */
    public static final boolean DEFAULT_SHOULD_SERVER_STARTUP_THREAD_RUN = true;
    /**
     * <p>The default value for config option to allow healing thread to delete
     * chunks with sufficient total replications and sufficient replications
     * in appropriate hash spans.</p>
     * <p>If set to false by user in configuration,
     * the thread will not perform any deletes.</p>
     */
    public static final boolean DEFAULT_SHOULD_HEALING_THREAD_DELETE = true;
    /**
     * <p>The default value for config option that specified the number of
     * replications required for a chunk to be deleted.</p>
     * <p>A chunk will only be deleted if it is out of its hash span and
     * there are enough copies on the network (specified by this variable). Furthermore,
     * there must also be enough copies in the appropriate hash span, which
     * is specified in DEFAULT_NUMBER_TOTAL_REPS_IN_HASH_SPAN_REQUIRED_DELETE</p>
     */
    public static final int DEFAULT_NUMBER_TOTAL_REPS_REQUIRED_DELETE = 3;
    /**
     * <p>The default value for config option that specifies the number of replications
     * that are in the correct hash span (i.e., how many other servers have this
     * chunk right now that have the chunk's hash in their hash span).</p>
     * <p>A chunk will only be deleted if it is out of its hash span and there
     * are enough total copies on the network (see DEFAULT_NUMBER_TOTAL_REPS_REQUIRED_DELETE),
     * along with enough copies in the appropriate hash span.</p>
     */
    public static final int DEFAULT_NUMBER_TOTAL_REPS_IN_HASH_SPAN_REQUIRED_DELETE = 1;
    /**
     * <p>Should the healing thread bother balancing the data blocks? This is the default value.</p>
     */
    public static final boolean DEFAULT_SHOULD_HEALING_THREAD_BALANCE_DATA_BLOCKS = false;
   
    /**
     * <p>Default for for variable: When moving, data block must have this much free space before a move is considered.</p>
     */
    public static final int DEFAULT_MIN_SIZE_AVAILABLE_IN_TARGET_DATABLOCK_BEFORE_BALANCE = 10 * DataBlock.getMaxBlockSize();
    /**
     * <p>The required difference in percentage between the data directories with least and most available space before transfering.</p>
     * <p>This only matters if healing thread is set to auto balance.</p>
     * <p>Note that host the calculation is determined:</p>
     * <ol>
     *   <li><strong>Find data directory with most available space</strong>: Must have at least 1GB of space available (the size of 10 full DataBlock's). Calculate percentage of its available space is used.</li>
     *   <li><strong>Find data directory with least available space</strong>: Calculate percentage of its available space is used. Must meet user-defined minimum used percentage of its space. </li>
     *   <li><strong>Subtract the percentage difference of the least from the most</strong>: if difference meets required minimum difference, the data will be moved from one to the other.
     * </ol>
     * <p>Note should always be a positive value less than 100.00. Anything else will be ignored by healing thread.</p>
     */
    public static final double DEFAULT_REQUIRED_PERCENTAGE_DIFFERENCE_TO_BALANCE_DATA_DIRECTORIES = 15.0;
    /**
     * <p>The minimum percentage of its available space used by a data directory before it will be a candidate for balancing. This sets a threshold before data will be moved away toward other data blocks.</p>
     * <p>This only matters if healing thread is set to auto balance.</p>
     * <p>Note that this does not guarentee that data will be moved to other data directories. Three additional criteria must be met:</p>
     * <ul>
     *   <li>Balancing must be allowed</li>
     *   <li>Another data directory must have at least 1GB free space and</li>
     *   <li>the same data directory above must have a percentage difference with the candidate data directory that exceeds the minimum percentage difference rule.</li>
     * </ul>
     * <p>Note should always be a positive value less than 100.00. Anything else will be ignored by healing thread.</p>
     */
    public static final double DEFAULT_REQUIRED_PERCENTAGE_FOR_MOST_USED_DATA_DIRECTORY_BEFORE_BALANCE = 60.0;    
    /**
     * <p>Whether or not should record meta data chunk deletions by default.</p>
     */
    public static final boolean DEFAULT_LOG_META_DATA_CHUNK_DELETIONS = true;
    /**
     * <p>Whether or not should record data chunk deletions by default.</p>
     */
    public static final boolean DEFAULT_LOG_DATA_CHUNK_DELETIONS = true;
    /**
     * 
     */
    public static final boolean DEFAULT_PROPAGATE_ALLOW_GET_DATA = true;
    public static final boolean DEFAULT_PROPAGATE_ALLOW_GET_META_DATA = true;
    /**
     * *****************      ---> KEYS <---      *****************
     */
    /**
     * <p>Used for testing GUI to make sure hidden items are not displayed.</p>
     */
    public static final String TEST_NO_DISPLAY = "testNoDisplay";
    /**
     * <p>The URL for the local server.</p>
     */
    public static final String URL = "serverURL";
    /**
     * <p>The name for the local server.</p>
     */
    public static final String NAME = "Tranche:Server Name";
    /**
     * <p>The group for the local server.</p>
     */
    public static final String GROUP = "Tranche:Group Name";
    /**
     * <p></p>
     */
    public static final String CURRENTLY_CONNECTED_USERS = "currentlyConnectedUsers";
    /**
     * <p>A comma-separately list of servers to trust.</p>
     * <p>This is useful when a core server was added to network, but have not been able to push servers list yet.</p>
     */
    public static final String SERVER_EXCEPTION_LIST_HOSTS_TO_TRUST = "coreServer: exceptionListServerHostsToTrust";
    /**
     * <p>A comma-separately list of servers to not trust.</p>
     * <p>This is useful when a core server is problematic and should no longer be trusted by this server.</p>
     */
    public static final String SERVER_EXCEPTION_LIST_HOSTS_TO_NOT_TRUST = "coreServer: exceptionListServerHostsToNotTrust";
    /**
     * <p>If set to "true", this server will always have highest priority in servers the client uploads to.</p>
     */
    public static final String MIRROR_EVERY_UPLOAD = "coreServer: mirrorEveryUpload";
    /**
     * <p>An ip/hostname that manually overrides </p>
     */
    public static final String SERVER_HOST_OVERRIDE = "coreServer: hostOverride";
    /**
     * <p>Set flag byte for server to signify what permissions clients have regarding reading, writing, etc.</p>
     */
    public static final String SERVER_MODE_FLAG_SYSTEM = "coreServer: serverModeFlag (system)";
    /**
     * <p>Human-readable information about server mode.</p>
     * <p>Note that this is not a modifiable attribute; it is set automatically and cannot be changed. To change the mode, see SERVER_MODE_FLAG.</p>
     */
    public static final String SERVER_MODE_DESCRIPTION_SYSTEM = "coreServer: serverMode (system)";
    /**
     * <p>Set flag byte for server to signify what permissions clients have regarding reading, writing, etc.</p>
     */
    public static final String SERVER_MODE_FLAG_ADMIN = "coreServer: serverModeFlag (admin)";
    /**
     * <p>Human-readable information about server mode.</p>
     * <p>Note that this is not a modifiable attribute; it is set automatically and cannot be changed. To change the mode, see SERVER_MODE_FLAG.</p>
     */
    public static final String SERVER_MODE_DESCRIPTION_ADMIN = "coreServer: serverMode (admin)";
    /**
     * <p>If set to "true", this server will be read-only.</p>
     * @deprecated Use SERVER_MODE_FLAG instead.
     */
    public static final String IS_SERVER_READ_ONLY = "coreServer: isServerReadOnly";
    /**
     * <p>If set to "true", this server will be write-only.</p>
     * @deprecated Use SERVER_MODE_FLAG instead.
     */
    public static final String IS_SERVER_WRITE_ONLY = "coreServer: isServerWriteOnly";
    /**
     * 
     */
    public static final String HASHSPANFIX_INDEX_DELETE_DATA = "hashSpanFix: indexDeleteData";
    /**
     * 
     */
    public static final String HASHSPANFIX_INDEX_DELETE_META_DATA = "hashSpanFix: indexDeleteMetaData";
    /**
     * 
     */
    public static final String HASHSPANFIX_INDEX_DOWNLOAD_DATA = "hashSpanFix: indexDownloadData";
    /**
     * 
     */
    public static final String HASHSPANFIX_INDEX_DOWNLOAD_META_DATA = "hashSpanFix: indexDownloadMetaData";
    /**
     * 
     */
    public static final String HASHSPANFIX_INDEX_REPAIR_DATA = "hashSpanFix: indexRepairData";
    /**
     * 
     */
    public static final String HASHSPANFIX_INDEX_REPAIR_META_DATA = "hashSpanFix: indexRepairMetaData";
    /**
     * <p>The weighted score of how much work the healing thread has done.</p>
     */
    public static final String HASHSPANFIX_PERFORMANCE_SCORE = "hashSpanFix: PerformanceScore";
    /**
     * <p>The heuristic-driven estimate of current disk usage for server.</p>
     */
    public static final String HASHSPANFIX_PERCEIVED_DISK_USE_STATE = "hashSpanFix: PerceivedDiskUseState";
    /**
     * <p>A comma-separated list of tranche servers to use in the hash span fixing thread.</p>
     */
    public static final String HASHSPANFIX_SERVER_HOSTS_TO_USE = "hashSpanFix: serverHostsToUse";
    
    /**
     * <p>Default key for configuration: if set to false, healing thread suspends until true.</p>
     */
    public static final String HASHSPANFIX_SHOULD_HEALING_THREAD_RUN = "hashSpanFix: AllowRun";
    /**
     * <p>Default key for configuration: if set to false, healing thread won't delete anything until true.</p>
     */
    public static final String HASHSPANFIX_SHOULD_HEALING_THREAD_DELETE = "hashSpanFix: AllowDelete";
    /**
     * <p>Default key for configuration: if true, the healing thread will balance the data between data directories. Else will not.</p>
     */
    public static final String HASHSPANFIX_SHOULD_HEALING_THREAD_BALANCE = "hashSpanFix: AllowBalance";
    public static final String HASHSPANFIX_REQUIRED_MIN_USED_BYTES_IN_MAX_DATABLOCK_TO_BALANCE_DATA_DIRECTORIES = "hashSpanFix: RequiredMinimumBytesUsedBeforeBalance";
    /**
     * <p>The required difference in percentage between the data directories with least and most available space before transfering.</p>
     * <p>This only matters if healing thread is set to auto balance.</p>
     * <p>Note that host the calculation is determined:</p>
     * <ol>
     *   <li><strong>Find data directory with most available space</strong>: Must have at least 1GB of space available (the size of 10 full DataBlock's). Calculate percentage of its available space is used.</li>
     *   <li><strong>Find data directory with least available space</strong>: Calculate percentage of its available space is used. Must meet user-defined minimum used percentage of its space. </li>
     *   <li><strong>Subtract the percentage difference of the least from the most</strong>: if difference meets required minimum difference, the data will be moved from one to the other.
     * </ol>
     * <p>Note should always be a positive value less than 100.00. Any range out of value will be ignored in favor of default.</p>
     */
    public static final String HASHSPANFIX_REQUIRED_PERCENTAGE_DIFFERENCE_TO_BALANCE_DATA_DIRECTORIES = "hashSpanFix: RequiredPercentageDifferenceToBalanceDataDirectories";
    /**
     * <p>The minimum percentage of its available space used by a data directory before it will be a candidate for balancing. This sets a threshold before data will be moved away toward other data blocks.</p>
     * <p>This only matters if healing thread is set to auto balance.</p>
     * <p>Note that this does not guarentee that data will be moved to other data directories. Three additional criteria must be met:</p>
     * <ul>
     *   <li>Balancing must be allowed</li>
     *   <li>Another data directory must have at least 1GB free space and</li>
     *   <li>the same data directory above must have a percentage difference with the candidate data directory that exceeds the minimum percentage difference rule.</li>
     * </ul>
     * <p>Note should always be a positive value less than 100.00.  Any range out of value will be ignored in favor of default.</p>
     */
    public static final String HASHSPANFIX_REQUIRED_PERCENTAGE_FOR_MOST_USED_DATA_DIRECTORY_BEFORE_BALANCE = "hashSpanFix: RequiredPercentageForMostUsedDataDirectoryBeforeBalance";
    /**
     * <p></p>
     */
    public static final String HASHSPANFIX_TOTAL_DATABLOCKS_MOVED_TO_BALANCE = "hashSpanFix: TotalDataBlocksMovedToBalance";
    /**
     * <p>Default key for configuration: set integer for total number of replications across network before performing a delete.</p>
     */
    public static final String HASHSPANFIX_NUMBER_REPS_REQUIRED_DELETE = "hashSpanFix: NumTotalRepsRequiredForDelete";
    /**
     * <p>Default key for configuration: set integer for total number of replications across network in an appropriate hash span before performing a delete.</p>
     */
    public static final String HASHSPANFIX_NUMBER_REPS_IN_HASH_SPAN_REQUIRED_DELETE = "hashSpanFix: NumRepsInHashSpanRequiredForDelete";
    /**
     * <p>Default key for configuration: how many chunks to download at a time.</p>
     */
    public static final String HASHSPANFIX_BATCH_SIZE_FOR_DOWNLOAD_CHUNKS = "hashSpanFix: BatchSizeForDownloadChunks";
    /**
     * <p>Default key for configuration: how many chunks to check for delete at a time.</p>
     */
    public static final String HASHSPANFIX_BATCH_SIZE_FOR_DELETE_CHUNKS = "hashSpanFix: BatchSizeForDeleteChunks";
    /**
     * <p>Default key for configuration: how many chunks to check for replication at a time.</p>
     */
    public static final String HASHSPANFIX_BATCH_SIZE_FOR_HEAL_CHUNKS = "hashSpanFix: BatchSizeForHealChunks";
    /**
     * <p>Default key for configuration: how many data blocks to balance at a time.</p>
     */
    public static final String HASHSPANFIX_BATCH_SIZE_FOR_BALANCE_DIRECTORIES = "hashSpanFix: BatchSizeForBalanceDirectories";
    /**
     * <p>What the hash span fixing thread is currently doing.</p>
     */
    public static final String HASHSPANFIX_CURRENT_HASH_SPAN_FIXING_THREAD_ACTIVITY = "hashSpanFix: CurrentActivity";
    /**
     * <p>How much time the hash span fixing thread has spent doing nothing.</p>
     */
    public static final String HASHSPANFIX_TIME_SPENT_DOING_NOTHING = "hashSpanFix: TimeSpentDoingNothing";
    /**
     * <p>How much time the hash span fixing thread has spent deleting.</p>
     */
    public static final String HASHSPANFIX_TIME_SPENT_DELETING = "hashSpanFix: TimeSpentDeleting";
    /**
     * <p>How much time the hash span fixing thread has spent healing.</p>
     */
    public static final String HASHSPANFIX_TIME_SPENT_HEALING = "hashSpanFix: TimeSpentHealing";
    /**
     * <p>How much time the hash span fixing thread has spent balancing data directories.</p>
     */
    public static final String HASHSPANFIX_TIME_SPENT_BALANCING = "hashSpanFix: TimeSpentBalancing";
    /**
     * <p>How much time the hash span fixing thread has spent downloading.</p>
     */
    public static final String HASHSPANFIX_TIME_SPENT_DOWNLOADING = "hashSpanFix: TimeSpentDownloading";
    /**
     * <p>Number of data chunks copied to the local server by the hash span fixing thread.</p>
     */
    public static final String HASHSPANFIX_DATA_COPIED = "hashSpanFix: DataCopied";
    /**
     * <p>Number of data chunks passed over by the hash span fixing thread.</p>
     */
    public static final String HASHSPANFIX_DATA_SKIPPED = "hashSpanFix: DataSkipped";
    /**
     * <p>Number of data chunks deleted by the hash span fixing thread.</p>
     */
    public static final String HASHSPANFIX_DATA_DELETED = "hashSpanFix: DataDeleted";
    /**
     * <p>Number of data chunks repaired by the hash span fixing thread.</p>
     */
    public static final String HASHSPANFIX_DATA_REPAIRED = "hashSpanFix: DataRepaired";
    /**
     * <p>Number of data chunks that could not be repaired by the hash span fixing thread.</p>
     */
    public static final String HASHSPANFIX_DATA_NOT_REPAIRED = "hashSpanFix: DataNotRepaired";
    /**
     * <p>Number of times an exception was thrown by the hash span fixing thread while working with data chunks.</p>
     */
    public static final String HASHSPANFIX_DATA_THREW_EXCEPTION = "hashSpanFix: DataLocalChunkThrewException";
    /**
     * <p>Number of meta data chunks copied to the local server by the hash span fixing thread.</p>
     */
    public static final String HASHSPANFIX_META_COPIED = "hashSpanFix: MetaDataCopied";
    /**
     * <p>Number of meta data chunks passed over by the hash span fixing thread.</p>
     */
    public static final String HASHSPANFIX_META_SKIPPED = "hashSpanFix: MetaDataSkipped";
    /**
     * <p>Number of meta data chunks deleted by the hash span fixing thread.</p>
     */
    public static final String HASHSPANFIX_META_DELETED = "hashSpanFix: MetaDataDeleted";
    /**
     * <p>Number of meta data chunks repaired by the hash span fixing thread.</p>
     */
    public static final String HASHSPANFIX_META_REPAIRED = "hashSpanFix: MetaDataRepaired";
    /**
     * <p>Number of meta data chunks that could not be repaired by the hash span fixing thread.</p>
     */
    public static final String HASHSPANFIX_META_NOT_REPAIRED = "hashSpanFix: MetaDataNotRepaired";
    /**
     * <p>Number of times an exception was thrown by the hash span fixing thread while working with meta data chunks.</p>
     */
    public static final String HASHSPANFIX_META_THREW_EXCEPTION = "hashSpanFix: MetaDataLocalChunkThrewException";
    /**
     * <p>Number of hash span fixing thread iterations that have occurred since server startup.</p>
     */
    public static final String HASHSPANFIX_IT_COUNT = "hashSpanFix: GlobalIterationCount";
    /**
     * <p>The server the hash span fixing thread is currently being worked on.</p>
     */
    public static final String HASHSPANFIX_SERVER_BEING_CHECKED = "hashSpanFix: GlobalServerBeingChecked";
    /**
     * <p>Number of hash span fixing thread repair iterations that have occurred since server startup.</p>
     */
    public static final String HASHSPANFIX_REPAIR_ITS = "hashSpanFix: GlobalLocalRepairCompleteIterations";
    /**
     * <p>Number of hash span fixing thread delete iterations that have occurred since server startup.</p>
     */
    public static final String HASHSPANFIX_DELETE_ITS = "hashSpanFix: GlobalLocalDeleteCompleteIterations";
    /**
     * <p>Free RAM on the server.</p>
     */
    public static final String FREE_MEMORY = "freeMemory";
    /**
     * <p>Total RAM on the server.</p>
     */
    public static final String TOTAL_MEMORY = "totalMemory";
    /**
     * <p>Number of data chunks on the server.</p>
     */
    public static final String DATABLOCK_KNOWN_DATA = "dataBlockUtil: KnownDataFileCount";
    /**
     * <p>Number of meta data chunks on the server.</p>
     */
    public static final String DATABLOCK_KNOWN_META = "dataBlockUtil: KnownMetaDataFileCount";
    /**
     *
     */
    public static final String DATABLOCK_STORE_DATABLOCK_REFERENCES = "dataBlockUtil: IsStoreDataBlockReferences";
    /**
     * <p>Whether or not should record data chunk deletions.</p>
     */
    public static final String DATABLOCK_LOG_DATA_CHUNK_DELETIONS = "dataBlockUtil: logDataChunkDeletions";
    /**
     * <p>Whether or not should record meta data chunk deletions.</p>
     */
    public static final String DATABLOCK_LOG_META_DATA_CHUNK_DELETIONS = "dataBlockUtil: logMetaDataChunkDeletions";
    /**
     * <p>Number of project meta data on the server.</p>
     */
    public static final String DATABLOCK_KNOWN_PROJECTS = "flatFileTrancheServer: KnownProjectCount";
    /**
     * <p>Configuration key for maximum number of nonce maps.</p>
     */
    public static final String NONCE_MAP_MAX_SIZE = "nonceMap: MaximumSize";
    /**
     * <p>How many data blocks are waiting to merge?</p>
     */
    public static final String DATABLOCK_MERGE_QUEUE_SIZE = "dataBlockUtil: MergeQueueSize";
    /**
     * <p>Total number of merged data blocks. Might not be same as success + failures if error during finally block.</p>
     */
    public static final String DATABLOCK_TOTAL_MERGED = "dataBlockUtil: MergedDataBlocksTotal";
    /**
     * <p>Total number of successfully merged data blocks.</p>
     */
    public static final String DATABLOCK_SUCCESS_MERGED = "dataBlockUtil: MergedDataBlocksSuccesses";
    /**
     * <p>Total number of failures while merging data blocks.</p>
     */
    public static final String DATABLOCK_FAIL_MERGED = "dataBlockUtil: MergedDataBlocksFailures";
    /**
     * <p>Human-readable aggregate size of all space allotted to DDCs.</p>
     */
    public static final String TOTAL_SIZE = "flatFileTrancheServer: TotalSize";
    /**
     * <p>Human-readable aggregate size of all space allotted to DDCs.</p>
     */
    public static final String TOTAL_SIZE_USED = "flatFileTrancheServer: TotalSizeUsed";
    /**
     * <p>Human-readable aggregate size of all space allotted to DDCs.</p>
     */
    public static final String TOTAL_SIZE_UNUSED = "flatFileTrancheServer: TotalSizeAvailable";
    /**
     * <p>Time to internally cache configuration used when FlatFileTrancheServer.getConfiguration called.</p>
     * <p>Updating configuration is an expensive process, and caching saves time.</p>
     */
    public static final String GET_CONFIGURATION_CACHE_TIME = "flatFileTrancheServer: GetConfigurationCacheTimeInMillis";
    /**
     * <p>Set a time to pause, in millis, after every single operation in the healing thread.</p>
     */
    public static final String HASHSPANFIX_PAUSE_MILLIS = "hashSpanFix: PauseInMillisAfterEachOperation";
    /**
     * <p>Keeps track of server-side callbacks. These will generally happen on the healing thread, though there could be other possibilities.</p>
     */
    public static final String REMOTE_SERVER_CALLBACKS = "remoteTrancheServer: ServerSideCallbacks";
    /**
     * <p>Whether the hash span fixing thread should fix corrupt data blocks.</p>
     */
    public static final String CORRUPTED_DB_ALLOWED_TO_FIX = "corruptedDataBlock: AllowedToFixCorruptedDataBlock";
    /**
     * <p>Number of corrupted data blocks.</p>
     */
    public static final String CORRUPTED_DB_COUNT = "corruptedDataBlock: CorruptedDataBlockCount";
    /**
     * <p>Number of fixed chunks as a result of corrupt data blocks.</p>
     */
    public static final String CORRUPTED_DB_SALVAGED_CHUNK_COUNT = "corruptedDataBlock: SalvagedChunksFromCorruptedBlockCount";
    /**
     * <p>Number of downloaded chunks as a result of corrupt data blocks.</p>
     */
    public static final String CORRUPTED_DB_DOWNLOADED_CHUNK_COUNT = "corruptedDataBlock: DownloadedChunksFromCorruptedBlockCount";
    /**
     * <p>Number of lost data chunks as a result of corrupt data blocks.</p>
     */
    public static final String CORRUPTED_DB_LOST_CHUNK_COUNT = "corruptedDataBlock: LostChunksFromCorruptedDataBlockCount";
    /**
     * <p>Number of discovered corrupt data block headers.</p>
     */
    public static final String CORRUPTED_DB_IN_HEADER_COUNT = "corruptedDataBlock: CorruptedDataBlockHeaderCount";
    /**
     * <p>Number of discovered corrupt data block bodies.</p>
     */
    public static final String CORRUPTED_DB_IN_BODY_COUNT = "corruptedDataBlock: CorruptedDataBlockBodyCount";
    public static final String REQUESTS_COUNT_GET_NONCE = "requestCount: GetNonce";
    public static final String REQUESTS_COUNT_GET_NONCES = "requestCount: GetNonces";
    public static final String REQUESTS_COUNT_GET_DATA_HASHES = "requestCount: GetDataHashes";
    public static final String REQUESTS_COUNT_GET_META_DATA_HASHES = "requestCount: GetMetaDataHashes";
    public static final String REQUESTS_COUNT_GET_HASHES = "requestCount: GetHashes";
    public static final String REQUESTS_COUNT_GET_DATA = "requestCount: GetData";
    public static final String REQUESTS_COUNT_HAS_DATA = "requestCount: HasData";
    public static final String REQUESTS_COUNT_BATCH_HAS_DATA = "requestCount: BatchHasData";
    public static final String REQUESTS_COUNT_HAS_META_DATA = "requestCount: HasMetaData";
    public static final String REQUESTS_COUNT_BATCH_HAS_META_DATA = "requestCount: BatchHasMetaData";
    public static final String REQUESTS_COUNT_GET_META_DATA = "requestCount: GetMetaData";
    public static final String REQUESTS_COUNT_SET_DATA = "requestCount: SetData";
    public static final String REQUESTS_COUNT_BATCH_SET_DATA = "requestCount: BatchSetData";
    public static final String REQUESTS_COUNT_BATCH_SET_META_DATA = "requestCount: BatchSetMetaData";
    public static final String REQUESTS_COUNT_BATCH_GET_DATA = "requestCount: BatchGetData";
    public static final String REQUESTS_COUNT_BATCH_GET_META_DATA = "requestCount: BatchGetMetaData";
    public static final String REQUESTS_COUNT_SET_META_DATA = "requestCount: SetMetaData";
    public static final String REQUESTS_COUNT_DELETE_META_DATA = "requestCount: DeleteMetaData";
    public static final String REQUESTS_COUNT_DELETE_DATA = "requestCount: DeleteData";
    public static final String REQUESTS_COUNT_GET_CONFIGURATION = "requestCount: GetConfiguration";
    public static final String REQUESTS_COUNT_SET_CONFIGURATION = "requestCount: SetConfiguration";
    public static final String REQUESTS_COUNT_GET_PROJECT_HASHES = "requestCount: GetProjectHashes";
    public static final String REQUESTS_COUNT_GET_KNOWN_SERVERS = "requestCount: GetKnownServers";
    /**
     * <p>Turn off propagation of certain requests server side.</p>
     * <p>Be careful! The client-side logic for propagation is tricky, so make sure whatever you turn off won't break the client logic. (E.g., turn off propagation for setting a data chunk, will the AddFileTool still work correctly?)</p>
     */
    public static final String PROPAGATE_ALLOW_GET_DATA = "propagate: AllowGetData";
    public static final String PROPAGATE_ALLOW_GET_META_DATA = "propagate: AllowGetMetaData";
    /**
     * ServerStartupThread variables
     */
    public static final String SERVER_STARTUP_THREAD_STATUS = "serverStartupThread: status";
    /**
     * <p>Prevent the startup thread from doing anything.</p>
     */
    public static final String SERVER_STARTUP_THREAD_ALLOW_RUN = "serverStartupThread: AllowRun";
    /**
     * RoutingTrancheServer variables
     */
    public static final String ROUTING_DATA_SERVERS_HOST_LIST = "routingTrancheServer: serversHostNameList";
    /**
     * <p>Whether the hash span fixing thread should repair corrupted data blocks.</p>
     */
    public static final boolean DEFAULT_SHOULD_REPAIR_CORRUPTED_DATA_BLOCKS = true;
    /**
     * <p>Keeps a boolean for keys to state whether they are editable by the administrator.</p>
     */
    private static Map<String, Permissions> permissions = new HashMap<String, Permissions>();
    

    static {

        /***********************************************************************
         *              <--- Global server variables -->
         **********************************************************************/
        permissions.put(SERVER_EXCEPTION_LIST_HOSTS_TO_NOT_TRUST, CAN_READ_EDIT);
        permissions.put(SERVER_EXCEPTION_LIST_HOSTS_TO_TRUST, CAN_READ_EDIT);
        permissions.put(MIRROR_EVERY_UPLOAD, CAN_READ_EDIT);
        permissions.put(IS_SERVER_READ_ONLY, CAN_READ_EDIT_DELETE);
        permissions.put(SERVER_MODE_DESCRIPTION_SYSTEM, CAN_READ);
        permissions.put(SERVER_MODE_FLAG_SYSTEM, CAN_READ);
        permissions.put(SERVER_MODE_DESCRIPTION_ADMIN, CAN_READ);
        permissions.put(SERVER_MODE_FLAG_ADMIN, CAN_READ_EDIT);
        permissions.put(IS_SERVER_WRITE_ONLY, CAN_READ_EDIT);

        /***********************************************************************
         *              <--- Server's information (memory, URL, users, etc.) -->
         **********************************************************************/
        permissions.put(CURRENTLY_CONNECTED_USERS, CAN_READ);
        permissions.put(URL, CAN_READ);
        permissions.put(FREE_MEMORY, CAN_READ);
        permissions.put(TOTAL_MEMORY, CAN_READ);

        /***********************************************************************
         *              <--- Available/unavailable disk space attributes -->
         **********************************************************************/
        permissions.put(TOTAL_SIZE, CAN_READ);
        permissions.put(TOTAL_SIZE_USED, CAN_READ);
        permissions.put(TOTAL_SIZE_UNUSED, CAN_READ);

        /***********************************************************************
         *              <--- DataBlock -->
         **********************************************************************/
        permissions.put(DATABLOCK_KNOWN_DATA, CAN_READ);
        permissions.put(DATABLOCK_KNOWN_META, CAN_READ);
        permissions.put(DATABLOCK_KNOWN_PROJECTS, CAN_READ);
        permissions.put(DATABLOCK_MERGE_QUEUE_SIZE, CAN_READ);
        permissions.put(DATABLOCK_FAIL_MERGED, CAN_READ);
        permissions.put(DATABLOCK_SUCCESS_MERGED, CAN_READ);
        permissions.put(DATABLOCK_TOTAL_MERGED, CAN_READ);
        permissions.put(DATABLOCK_LOG_DATA_CHUNK_DELETIONS, CAN_READ_EDIT);
        permissions.put(DATABLOCK_LOG_META_DATA_CHUNK_DELETIONS, CAN_READ_EDIT);
        permissions.put(DATABLOCK_STORE_DATABLOCK_REFERENCES, CAN_READ_EDIT);

        // >>> Corruption in data block <<<
        permissions.put(CORRUPTED_DB_ALLOWED_TO_FIX, CAN_READ_EDIT);
        permissions.put(CORRUPTED_DB_COUNT, CAN_READ);
        permissions.put(CORRUPTED_DB_SALVAGED_CHUNK_COUNT, CAN_READ);
        permissions.put(CORRUPTED_DB_DOWNLOADED_CHUNK_COUNT, CAN_READ);
        permissions.put(CORRUPTED_DB_LOST_CHUNK_COUNT, CAN_READ);
        permissions.put(CORRUPTED_DB_IN_HEADER_COUNT, CAN_READ);
        permissions.put(CORRUPTED_DB_IN_BODY_COUNT, CAN_READ);

        /***********************************************************************
         *              <--- HashSpanFixingThread -->
         **********************************************************************/        // >>> Reporting variables <<<
        permissions.put(HASHSPANFIX_DATA_COPIED, CAN_READ);
        permissions.put(HASHSPANFIX_DATA_SKIPPED, CAN_READ);
        permissions.put(HASHSPANFIX_DATA_DELETED, CAN_READ);
        permissions.put(HASHSPANFIX_DATA_REPAIRED, CAN_READ);
        permissions.put(HASHSPANFIX_DATA_NOT_REPAIRED, CAN_READ);
        permissions.put(HASHSPANFIX_DATA_THREW_EXCEPTION, CAN_READ);
        permissions.put(HASHSPANFIX_META_COPIED, CAN_READ);
        permissions.put(HASHSPANFIX_META_SKIPPED, CAN_READ);
        permissions.put(HASHSPANFIX_META_DELETED, CAN_READ);
        permissions.put(HASHSPANFIX_META_REPAIRED, CAN_READ);
        permissions.put(HASHSPANFIX_META_NOT_REPAIRED, CAN_READ);
        permissions.put(HASHSPANFIX_META_THREW_EXCEPTION, CAN_READ);
        permissions.put(HASHSPANFIX_IT_COUNT, CAN_READ);
        permissions.put(HASHSPANFIX_SERVER_BEING_CHECKED, CAN_READ);
        permissions.put(HASHSPANFIX_REPAIR_ITS, CAN_READ);
        permissions.put(HASHSPANFIX_DELETE_ITS, CAN_READ);
        permissions.put(HASHSPANFIX_CURRENT_HASH_SPAN_FIXING_THREAD_ACTIVITY, CAN_READ);
        permissions.put(HASHSPANFIX_TIME_SPENT_DOING_NOTHING, CAN_READ);
        permissions.put(HASHSPANFIX_TIME_SPENT_DELETING, CAN_READ);
        permissions.put(HASHSPANFIX_TIME_SPENT_HEALING, CAN_READ);
        permissions.put(HASHSPANFIX_TIME_SPENT_DOWNLOADING, CAN_READ);
        permissions.put(ConfigKeys.HASHSPANFIX_TIME_SPENT_BALANCING, CAN_READ);
        permissions.put(HASHSPANFIX_PERFORMANCE_SCORE, CAN_READ);
        permissions.put(HASHSPANFIX_PERCEIVED_DISK_USE_STATE, CAN_READ);
        permissions.put(HASHSPANFIX_INDEX_DELETE_DATA,CAN_READ);
        permissions.put(HASHSPANFIX_INDEX_DELETE_META_DATA,CAN_READ);
        permissions.put(HASHSPANFIX_INDEX_DOWNLOAD_DATA,CAN_READ);
        permissions.put(HASHSPANFIX_INDEX_DOWNLOAD_META_DATA,CAN_READ);
        permissions.put(HASHSPANFIX_INDEX_REPAIR_DATA,CAN_READ);
        permissions.put(HASHSPANFIX_INDEX_REPAIR_META_DATA,CAN_READ);

        // >>> HashSpanFixingThread global controls <<<
        permissions.put(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_RUN, CAN_READ_EDIT);
        permissions.put(ConfigKeys.HASHSPANFIX_PAUSE_MILLIS, CAN_READ_EDIT);

        // >>> Batch sizes: controlling how to use resources
        permissions.put(ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_DELETE_CHUNKS, CAN_READ_EDIT);
        permissions.put(ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_DOWNLOAD_CHUNKS, CAN_READ_EDIT);
        permissions.put(ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_HEAL_CHUNKS, CAN_READ_EDIT);
        permissions.put(ConfigKeys.HASHSPANFIX_BATCH_SIZE_FOR_BALANCE_DIRECTORIES, CAN_READ_EDIT);

        // >>> Deleting chunks <<<
        permissions.put(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_DELETE, CAN_READ_EDIT);
        permissions.put(ConfigKeys.HASHSPANFIX_NUMBER_REPS_REQUIRED_DELETE, CAN_READ_EDIT);
        permissions.put(ConfigKeys.HASHSPANFIX_NUMBER_REPS_IN_HASH_SPAN_REQUIRED_DELETE, CAN_READ_EDIT);

        // >>> Balancing data directories <<<
        permissions.put(ConfigKeys.HASHSPANFIX_SHOULD_HEALING_THREAD_BALANCE, CAN_READ_EDIT);
        permissions.put(ConfigKeys.HASHSPANFIX_REQUIRED_PERCENTAGE_DIFFERENCE_TO_BALANCE_DATA_DIRECTORIES, CAN_READ_EDIT);
        permissions.put(ConfigKeys.HASHSPANFIX_REQUIRED_PERCENTAGE_FOR_MOST_USED_DATA_DIRECTORY_BEFORE_BALANCE, CAN_READ_EDIT);
        permissions.put(ConfigKeys.HASHSPANFIX_TOTAL_DATABLOCKS_MOVED_TO_BALANCE, CAN_READ_EDIT);
        
        // TargetHashSpanThread
        permissions.put(ConfigKeys.TARGET_HASH_SPAN_THREAD_MESSAGE, CAN_READ);
        permissions.put(ConfigKeys.TARGET_HASH_SPAN_THREAD_WORKING, CAN_READ);

        /***********************************************************************
         *              <--- Other FFTS-related items -->
         **********************************************************************/
        permissions.put(ConfigKeys.NONCE_MAP_MAX_SIZE, CAN_READ_EDIT);
        permissions.put(ConfigKeys.GET_CONFIGURATION_CACHE_TIME, CAN_READ_EDIT);

        /***********************************************************************
         *              <--- RemoteTrancheServer -->
         **********************************************************************/
        permissions.put(REMOTE_SERVER_CALLBACKS, CAN_READ);

        /***********************************************************************
         *              <--- Simply used for testing in GUI -->
         **********************************************************************/
        permissions.put(TEST_NO_DISPLAY, NO_PERMISSIONS);

        /***********************************************************************
         *              <!--- Request counts -->
         **********************************************************************/
        permissions.put(REQUESTS_COUNT_GET_NONCE, CAN_READ);
        permissions.put(REQUESTS_COUNT_GET_NONCES, CAN_READ);
        permissions.put(REQUESTS_COUNT_GET_DATA_HASHES, CAN_READ);
        permissions.put(REQUESTS_COUNT_GET_META_DATA_HASHES, CAN_READ);
        permissions.put(REQUESTS_COUNT_GET_HASHES, CAN_READ);
        permissions.put(REQUESTS_COUNT_GET_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_HAS_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_BATCH_HAS_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_HAS_META_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_BATCH_HAS_META_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_GET_META_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_SET_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_BATCH_SET_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_BATCH_SET_META_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_BATCH_GET_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_BATCH_GET_META_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_SET_META_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_DELETE_META_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_DELETE_DATA, CAN_READ);
        permissions.put(REQUESTS_COUNT_GET_CONFIGURATION, CAN_READ);
        permissions.put(REQUESTS_COUNT_SET_CONFIGURATION, CAN_READ);
        permissions.put(REQUESTS_COUNT_GET_PROJECT_HASHES, CAN_READ);
        permissions.put(REQUESTS_COUNT_GET_KNOWN_SERVERS, CAN_READ);
        
        /***********************************************************************
         *              <!--- ServerStartupThread variables -->
         **********************************************************************/
        permissions.put(SERVER_STARTUP_THREAD_STATUS, CAN_READ);
        permissions.put(SERVER_STARTUP_THREAD_ALLOW_RUN, CAN_READ_EDIT);
        
        /***********************************************************************
         *              <!--- Propagation variables -->
         **********************************************************************/ 
        permissions.put(PROPAGATE_ALLOW_GET_DATA, CAN_READ_EDIT);
        permissions.put(PROPAGATE_ALLOW_GET_META_DATA, CAN_READ_EDIT);
        /***********************************************************************
         *              <!--- RoutingTrancheServer variables -->
         **********************************************************************/
        permissions.put(ROUTING_DATA_SERVERS_HOST_LIST, CAN_READ_EDIT);
    }

    /**
     * <p>Whether a configuration attribute should be editable by the administrator. For example, status attributes like free memory should not be editable.</p>
     * @param key
     * @return
     */
    public static boolean isEditable(String key) {

        // Don't let edit bytes used for DDC... wait, need to be able to delete these things if for old ddcs!
        if (key.startsWith("actualBytesUsed:") || key.startsWith("actualPercentageUsed:") || key.startsWith("actualBytesUsedOverflow:")) {
            return true;
        }

        // default is to be editable
        if (!permissions.containsKey(key)) {
            return true;
        }
        // try to get the value
        try {
            return permissions.get(key).canEdit;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * <p>Whether a configuration attribute should be deletable by the administrator. For example, status attributes like free memory should not be deletable.</p>
     * @param key
     * @return
     */
    public static boolean isDeletable(String key) {

        // Don't let edit bytes used for DDC... wait, need to be able to delete these things if for old ddcs!
        if (key.startsWith("actualBytesUsed:") || key.startsWith("actualPercentageUsed:") || key.startsWith("actualBytesUsedOverflow:")) {
            return true;
        }

        // default is to be editable
        if (!permissions.containsKey(key)) {
            return true;
        }
        // try to get the value
        try {
            return permissions.get(key).canDelete;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * <p>Whether a configuration attribute should be readable. This is not a secure way to hide sensative information, but can set something to be not readable if doesn't need to be displayed.</p>
     * @param key
     * @return
     */
    public static boolean isReadable(String key) {

        // Don't let edit bytes used for DDC
        if (key.startsWith("actualBytesUsed:") || key.startsWith("actualPercentageUsed:") || key.startsWith("actualBytesUsedOverflow:")) {
            return true;
        }

        // default is to be editable
        if (!permissions.containsKey(key)) {
            return true;
        }
        // try to get the value
        try {
            return permissions.get(key).canRead;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Encapsulate permissions.
     */
    private static class Permissions {

        public final boolean canEdit,  canRead,  canDelete;

        public Permissions(boolean canEdit, boolean canDelete, boolean canRead) {
            this.canDelete = canDelete;
            this.canEdit = canEdit;
            this.canRead = canRead;
        }
    }
}