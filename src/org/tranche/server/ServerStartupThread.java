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
package org.tranche.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.FileEncoding;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.ServerModeFlag;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.logs.activity.Activity;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.network.*;
import org.tranche.time.TimeUtil;
import org.tranche.users.User;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;

/**
 * <p>Start up tasks thread for server. Important for integrity of network: makes sure certain items which should be deleted are, and attempts to get copies of any newly added chunks.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ServerStartupThread extends Thread {

    private static boolean debug = false;
    /**
     * 
     */
    private static final String DELETE_DATA_SAVE_FILENAME = "delete-data.server-startup-thread";
    /**
     * 
     */
    private static final String DELETE_META_DATA_SAVE_FILENAME = "delete-meta-data.server-startup-thread";
    /**
     * <p>The maximum number of log entries to get at once when dealing with deletions.</p>
     * <p>Network latency is the major concern, so make high. Approximately a little over 1000 entries is around 1MB.</p>
     */
    private final static int maximumBatchSizeDelete = 2000;
    /**
     * <p>The maximum number of log entries to get at once when dealing with setting chunks.</p>
     * <p>Keep low so if fail in middle of batch, progress not lost. Also need to verify chunks not deleted, so don't want to hold deleted chunks long.</p>
     */
    private final static int maximumBatchSizeSet = 50;
    /**
     *
     */
    private final static int maximumBatchSizeReplaceMetaData = 100;
    private final Server socketServer;
    private final TrancheServer wrappedServer;
    private boolean exitted;
    private final int[] deleteCount;
    private final int[] addCount;
    private final int[] replacedMetaDataCount;

    /**
     * <p>Start up tasks thread for server. Important for integrity of network: makes sure certain items which should be deleted are, and attempts to get copies of any newly added chunks.</p>
     * @param socketServer
     * @param wrappedServer
     */
    public ServerStartupThread(Server socketServer, TrancheServer wrappedServer) {
        this.socketServer = socketServer;
        this.wrappedServer = wrappedServer;
        this.deleteCount = new int[1];
        this.deleteCount[0] = 0;
        this.addCount = new int[1];
        this.addCount[0] = 0;
        this.replacedMetaDataCount = new int[1];
        this.replacedMetaDataCount[0] = 0;
        this.exitted = false;
    }

    @Override
    public void run() {
        // need to update to new connection scheme
        final long start = TimeUtil.getTrancheTimestamp();

        // These are times took for each step to run
        long step1Time = -1, step2Time = -1, step3Time = -1, step4Time = -1, step5Time = -1, step6Time = -1, step7Time = -1;

        try {

            boolean isFlatFileTrancheServer = (wrappedServer instanceof FlatFileTrancheServer);

            if (!isFlatFileTrancheServer) {
                debugOut("Not a data server");
                return;
            }

            debugOut("Starting for data server");
            /**
             * GET SERVERS TO USE: When our new network functionality is finished, this will likely
             *                     need to change.
             */
            Set<String> hostsToUse = new HashSet();
            SERVERS_TO_USE:
            for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {

                // Don't check self!
                if (row.getHost().equals(socketServer.getHostName()) || this.wrappedServer.getHost().equals(row.getHost()) || row.isLocalServer()) {
                    continue SERVERS_TO_USE;
                }

                // If testing, need to just check port. This is because we don't know whether
                // address is 192.168 or 127.0.0.1 (the loopback address).
                if (TestUtil.isTesting() || TestUtil.isTestingServerStartupThread()) {
                    try {
                        int thisPort = socketServer.getPort();
                        int nextPort = row.getPort();
                        if (thisPort == nextPort) {
                            continue SERVERS_TO_USE;
                        }
                    } catch (Exception e) {
                        debugErr(e);
                        // Since this is a test, I'd like to know about this. Rethrow
                        throw new RuntimeException(e);
                    }
                }

                hostsToUse.add(row.getHost());
            }

            for (String host : hostsToUse) {
                debugOut("Using server: " + host);
            }

            final FlatFileTrancheServer ffts = (FlatFileTrancheServer) wrappedServer;
            final long lastRecordedActivityTimestamp = ffts.getActivityLog().getLastRecordedTimestamp();

            // ----------------------------------------------------------------------------------------------
            //  STEP 1: Data is not available yet. Make 'write-only'.
            // ----------------------------------------------------------------------------------------------
            debugOut("Step 1: ");
            final long step1Start = TimeUtil.getTrancheTimestamp();
            ffts.getConfiguration().setValue(ConfigKeys.SERVER_STARTUP_THREAD_STATUS, String.valueOf("STEP 1: Limiting server to write-only (Running: " + String.valueOf(TimeUtil.getTrancheTimestamp() - start) + ")"));
            ffts.getConfiguration().setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.CAN_WRITE));
            final long startWriteOnlyTimestamp = TimeUtil.getTrancheTimestamp();

            // Record time took to complete step
            step1Time = TimeUtil.getTrancheTimestamp() - step1Start;

            // ----------------------------------------------------------------------------------------------
            //  STEP 2: Wait for it to load data blocks
            // ----------------------------------------------------------------------------------------------
            debugOut("Step 2: ");
            final long step2Start = TimeUtil.getTrancheTimestamp();
            ffts.getConfiguration().setValue(ConfigKeys.SERVER_STARTUP_THREAD_STATUS, String.valueOf("STEP 2: Waiting for datablocks to load (Running: " + String.valueOf(TimeUtil.getTrancheTimestamp() - start) + ")"));
            try {
                ffts.waitToLoadExistingDataBlocks();
            } catch (Exception e) {
                debugErr(e);
            }

            // Record time took to complete step
            step2Time = TimeUtil.getTrancheTimestamp() - step2Start;

            // ----------------------------------------------------------------------------------------------
            //  STEP 3: Check other servers for logs, and see if anything should be deleted.
            // ----------------------------------------------------------------------------------------------
            debugOut("Step 3: ");
            long step3Start = TimeUtil.getTrancheTimestamp();

            ffts.getConfiguration().setValue(ConfigKeys.SERVER_STARTUP_THREAD_STATUS, String.valueOf("STEP 3: Waiting for startup (Running: " + String.valueOf(TimeUtil.getTrancheTimestamp() - start) + ")"));
            if (!TestUtil.isTesting()) {
                NetworkUtil.waitForStartup();
            }
            List<String> serversToCheckForDeletes = new LinkedList();

            CHECK_SERVERS_FOR_DELETES:
            for (String host : hostsToUse) {
                ffts.getConfiguration().setValue(ConfigKeys.SERVER_STARTUP_THREAD_STATUS, String.valueOf("STEP 3: Checking " + host + " for chunks to delete (Running: " + String.valueOf(TimeUtil.getTrancheTimestamp() - start) + ")"));

                boolean wasChecked = checkServerForChunksToDelete(lastRecordedActivityTimestamp, startWriteOnlyTimestamp, host, ffts, deleteCount);
                if (!wasChecked) {
                    serversToCheckForDeletes.add(host);
                }
            } // For each server, look for deletes to perform

            // Record time took to complete step
            step3Time = TimeUtil.getTrancheTimestamp() - step3Start;

            // ----------------------------------------------------------------------------------------------
            //  STEP 4: Check for replaced meta data.
            // ----------------------------------------------------------------------------------------------
            debugOut("Step 4: ");
            final long step4Start = TimeUtil.getTrancheTimestamp();

            List<String> serversToCheckForReplacedMetaData = new LinkedList();

            CHECK_SERVERS_FOR_REPLACED_META_DATA:
            for (String host : hostsToUse) {
                ffts.getConfiguration().setValue(ConfigKeys.SERVER_STARTUP_THREAD_STATUS, String.valueOf("STEP 4: Checking " + host + " for meta data to replace (Running: " + String.valueOf(TimeUtil.getTrancheTimestamp() - start) + ")"));

                boolean wasChecked = checkServerForMetaDataToReplace(lastRecordedActivityTimestamp, startWriteOnlyTimestamp, host, ffts, replacedMetaDataCount);
                if (!wasChecked) {
                    serversToCheckForReplacedMetaData.add(host);
                }
            } // For each server, look for deletes to perform

            step4Time = TimeUtil.getTrancheTimestamp() - step4Start;

            // ----------------------------------------------------------------------------------------------
            //  STEP 5: Data is now available. Unrestrict.
            // ----------------------------------------------------------------------------------------------
            debugOut("Step 5: ");
            final long step5Start = TimeUtil.getTrancheTimestamp();
            ffts.getConfiguration().setValue(ConfigKeys.SERVER_STARTUP_THREAD_STATUS, String.valueOf("STEP 5: Putting server in unrestricted mode (read/write) (Running: " + String.valueOf(TimeUtil.getTrancheTimestamp() - start) + ")"));
            ffts.getConfiguration().setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.CAN_READ_WRITE));

            // Record time took to complete step
            step5Time = TimeUtil.getTrancheTimestamp() - step5Start;

            // ----------------------------------------------------------------------------------------------
            //  STEP 6: Check other servers for newly-added chunks
            // ----------------------------------------------------------------------------------------------
            debugOut("Step 6: ");
            final long step6Start = TimeUtil.getTrancheTimestamp();

            List<String> serversToCheckForAdds = new LinkedList();

            CHECK_SERVERS_FOR_CHUNKS:
            for (String host : hostsToUse) {
                ffts.getConfiguration().setValue(ConfigKeys.SERVER_STARTUP_THREAD_STATUS, String.valueOf("STEP 6: Checking " + host + " for chunks to add (Running: " + String.valueOf(TimeUtil.getTrancheTimestamp() - start) + ")"));
                boolean wasChecked = checkServerForChunksToAdd(lastRecordedActivityTimestamp, startWriteOnlyTimestamp, host, ffts, addCount, deleteCount);

                if (!wasChecked) {
                    serversToCheckForAdds.add(host);
                }
            } // For each server, look for new chunks to add

            // Record time took to complete step
            step6Time = TimeUtil.getTrancheTimestamp() - step6Start;

            // ----------------------------------------------------------------------------------------------
            //  STEP 7: Wait for servers to come online to perform deletes and adds from them
            // ----------------------------------------------------------------------------------------------
            debugOut("Step 7: ");
            final long step7Start = TimeUtil.getTrancheTimestamp();

            while (serversToCheckForAdds.size() > 0 || serversToCheckForDeletes.size() > 0 || serversToCheckForReplacedMetaData.size() > 0) {

                ffts.getConfiguration().setValue(ConfigKeys.SERVER_STARTUP_THREAD_STATUS, String.valueOf("STEP 7: Waiting for servers to check for deletes <" + serversToCheckForDeletes.size() + "> and adds <" + serversToCheckForAdds.size() + "> and meta data to replace <" + serversToCheckForReplacedMetaData.size() + "> (Running: " + String.valueOf(TimeUtil.getTrancheTimestamp() - start) + ")"));

                // Perform: deletes
                Iterator<String> serversToCheckForDeletesIt = serversToCheckForDeletes.iterator();
                while (serversToCheckForDeletesIt.hasNext()) {
                    String host = serversToCheckForDeletesIt.next();
                    boolean wasChecked = checkServerForChunksToDelete(lastRecordedActivityTimestamp, startWriteOnlyTimestamp, host, ffts, deleteCount);
                    if (wasChecked) {
                        serversToCheckForDeletesIt.remove();
                    }
                }

                // Perform: replace
                Iterator<String> serversToCheckForReplacedMetaDataIt = serversToCheckForReplacedMetaData.iterator();
                while (serversToCheckForReplacedMetaDataIt.hasNext()) {
                    String host = serversToCheckForReplacedMetaDataIt.next();
                    boolean wasChecked = checkServerForMetaDataToReplace(lastRecordedActivityTimestamp, startWriteOnlyTimestamp, host, ffts, replacedMetaDataCount);
                    if (wasChecked) {
                        serversToCheckForReplacedMetaDataIt.remove();
                    }
                }

                // Perform: adds
                Iterator<String> serversToCheckForAddsIt = serversToCheckForAdds.iterator();
                while (serversToCheckForAddsIt.hasNext()) {
                    String host = serversToCheckForAddsIt.next();
                    boolean wasChecked = checkServerForChunksToAdd(lastRecordedActivityTimestamp, startWriteOnlyTimestamp, host, ffts, addCount, deleteCount);
                    if (wasChecked) {
                        serversToCheckForAddsIt.remove();
                    }
                }

                // Sleep a bit to wait for things to come online
                Thread.sleep(1000 * 30);
            }

            // Record time took to complete step
            step7Time = TimeUtil.getTrancheTimestamp() - step7Start;

        } catch (Exception e) {
            debugErr(e);
        } finally {
            exitted = true;
            debugOut("---------------------------------------------------------------------------------------------------------------------------");
            debugOut(" ServerStartupThread finished. Took: " + Text.getPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp() - start));
            debugOut("    * Step 1 (set server mode 'write-only') .................................... " + Text.getPrettyEllapsedTimeString(step1Time));
            debugOut("    * Step 2 (wait for datablocks to load) ..................................... " + Text.getPrettyEllapsedTimeString(step2Time));
            debugOut("    * Step 3 (find and perform missed deletions) ............................... " + Text.getPrettyEllapsedTimeString(step3Time));
            debugOut("    * Step 4 (find and replace meta data) ...................................... " + Text.getPrettyEllapsedTimeString(step4Time));
            debugOut("    * Step 5 (set server mode unrestricted) .................................... " + Text.getPrettyEllapsedTimeString(step5Time));
            debugOut("    * Step 6 (find and add missed chunks) ...................................... " + Text.getPrettyEllapsedTimeString(step6Time));
            debugOut("    * Step 7 (wait for offline servers to perform steps 3 and 5) ............... " + Text.getPrettyEllapsedTimeString(step7Time));
            debugOut(" Deletes: " + deleteCount[0] + "; Adds: " + addCount[0]);
            debugOut("---------------------------------------------------------------------------------------------------------------------------");
        }
    }

    /**
     *
     * @param hash
     * @param downloadedChunk
     * @param localServerToUpdate
     * @param user
     * @return
     * @throws Exception
     */
    public static boolean addMetaData(BigHash hash, byte[] downloadedChunk, final FlatFileTrancheServer localServerToUpdate, User user) throws Exception {
        if (IOUtil.hasMetaData(localServerToUpdate, hash)) {

            // check for existing meta data
            MetaData existingMetaData = null;
            try {
                existingMetaData = MetaData.createFromBytes(localServerToUpdate.getDataBlockUtil().getMetaData(hash));
            } catch (Exception e) {
                // Oh, this could be bad. Best thing to do is delete so that we have newest meta data.
                System.err.println("ERROR: " + e.getClass().getSimpleName() + " occurred while trying to read in meta data to replace in ServerStartupThread<" + hash + ">: " + e.getMessage());
                localServerToUpdate.getDataBlockUtil().addMetaData(hash, downloadedChunk);
                return false;
            }

            // read the new meta data
            MetaData newMetaData = null;
            try {
                newMetaData = MetaData.createFromBytes(downloadedChunk);
            } catch (Exception e) {
                // Remote meta data is corrupted. Skip it.
                return false;
            }

            MetaData mergedMetaData = existingMetaData.clone();

            // add all uploaders from the new meta data
            for (int i = 0; i < newMetaData.getUploaderCount(); i++) {
                newMetaData.selectUploader(i);
                if (!mergedMetaData.containsUploader(newMetaData.getSignature().getUserName(), newMetaData.getTimestampUploaded(), newMetaData.getRelativePathInDataSet())) {
                    // just add the uploader
                    mergedMetaData.addUploader(newMetaData.getSignature(), new ArrayList<FileEncoding>(newMetaData.getEncodings()), newMetaData.getProperties(), new ArrayList<MetaDataAnnotation>(newMetaData.getAnnotations()));
                    // set the parts
                    mergedMetaData.setParts(new ArrayList(newMetaData.getParts()));
                }
            }
            // write out the bytes of the merged meta data
            byte[] mergedMetaDataBytes = mergedMetaData.toByteArray();
            // check size
            if (mergedMetaDataBytes.length > MetaData.SIZE_MAX) {
                System.err.println("Error in ServerStartupThread (when setting meta data): mergedMetaDataBytes.length(" + mergedMetaDataBytes.length + ") > MetaData.SIZE_MAX(" + MetaData.SIZE_MAX + ")");
                return false;
            }
            // add the merged meta data
            localServerToUpdate.getDataBlockUtil().addMetaData(hash, mergedMetaDataBytes);
            return true;
        } /**
         * Server doesn't have yet. Simply add.
         */
        else {
            if (!user.canSetMetaData()) {
                return false;
            }
            localServerToUpdate.getDataBlockUtil().addMetaData(hash, downloadedChunk);
            return true;
        }
    } // addMetaData

    /**
     *
     * @param fromTimestamp
     * @param toTimestamp
     * @param host
     * @param localServerToUpdate
     * @return
     */
    public static boolean checkServerForMetaDataToReplace(final long fromTimestamp, final long toTimestamp, final String host, final FlatFileTrancheServer localServerToUpdate) {
        return checkServerForMetaDataToReplace(fromTimestamp, toTimestamp, host, localServerToUpdate, null);
    }

    /**
     *
     * @param fromTimestamp
     * @param toTimestamp
     * @param host
     * @param localServerToUpdate
     * @param metaDataUpdateCount
     * @return
     */
    public static boolean checkServerForMetaDataToReplace(final long fromTimestamp, final long toTimestamp, final String host, final FlatFileTrancheServer localServerToUpdate, final int[] metaDataUpdateCount) {
        //  Right now, there is no replacement functionality. When there is, we'll have to implement here like it is implemented
        // in FlatFileTrancheServer (so they have the same policy).

        return true;
//        long timestampToStartAt = fromTimestamp;
//        ATTEMPT:
//        for (int attempt = 0; attempt < 3; attempt++) {
//            try {
//
//                // If not online, wait a second and try again
//                StatusTableRow row = NetworkUtil.getStatus().getRow(host);
//                if (row == null || !row.isOnline()) {
//                    Thread.sleep(1000);
//                    continue ATTEMPT;
//                }
//
//                TrancheServer ts = getConnection(host);
//
//                boolean cont = true;
//
//                while (cont) {
//                    cont = false;
//
//                    Activity[] nextBatch = ts.getActivityLogEntries(timestampToStartAt, toTimestamp, maximumBatchSizeReplaceMetaData, Activity.REPLACE_META_DATA);
//
//                    // Perform replace, but only if trust signatures
//                    REPLACE:
//                    for (Activity a : nextBatch) {
//
//                        // First, make sure can load user
//                        User user = localServerToUpdate.getConfiguration().getRecognizedUser(a.signature.getCert());
//                        if (user == null) {
//                            continue REPLACE;
//                        }
//
//                        if (a.isMetaData()) {
//
//                            PropagationReturnWrapper rw = IOUtil.getMetaData(ts, a.hash, false);
//
//                            byte[] newChunk = null;
//
//                            if (rw.isByteArraySingleDimension()) {
//                                newChunk = (byte[]) rw.getReturnValueObject();
//                            } else if (rw.isByteArrayDoubleDimension()) {
//                                newChunk = ((byte[][]) rw.getReturnValueObject())[0];
//                            } else {
//                                throw new AssertionFailedException("Expecting one or two dimensional byte array, but found neither.");
//                            }
//
//                            // It's better to move on than throw an exception since server
//                            // might have other chunks we'd like.
//                            if (newChunk == null) {
//                                continue REPLACE;
//                            }
//
//                            boolean added = addMetaData(a.hash, newChunk, localServerToUpdate, user);
//                            if (added) {
//                                if (metaDataUpdateCount != null) {
//                                    metaDataUpdateCount[0]++;
//                                }
//                            }
//
//                        } else {
//                            System.err.println("Assertion failed: should be meta data, but was data. Skipping replacement: " + a.hash);
//                            continue REPLACE;
//                        }
//
//                    } // For all deletes in this batch
//
//                    // If got a full batch, means we might not be done yet. Update the starting timestamp
//                    // for the next batch
//                    if (nextBatch.length == maximumBatchSizeDelete) {
//                        cont = true;
//                        timestampToStartAt = nextBatch[nextBatch.length - 1].timestamp;
//                    }
//                } // While potential entries remaining
//
//                // If get here, no problems. Finished
//                return true;
//            } catch (Exception e) {
//                System.err.println(e.getClass().getSimpleName() + " occurred while checking for deleted chunk on " + host + " (attempt #" + attempt + ") : " + e.getMessage());
//                e.printStackTrace(System.err);
//            } finally {
//                releaseConnection(host);
//            }
//        } // For up to certain number of attempt
//
//        // Couldn't check because offline or too many problems
//        return false;
    }

    /**
     * <p>Update a local server (FlatFileTrancheServer) by checking specific server at a specific url is there any deletes to consider between two timestamps. If so, will perform deletes only if user is trusted.</p>
     * @param fromTimestamp Starting of period to check (inclusive)
     * @param toTimestamp End of period to check (inclusive)
     * @param host Host name for server to check for deletions
     * @param localServerToUpdate The FlatFileTrancheServer to check
     */
    public static boolean checkServerForChunksToDelete(final long fromTimestamp, final long toTimestamp, final String host, final FlatFileTrancheServer localServerToUpdate) {
        return checkServerForChunksToDelete(fromTimestamp, toTimestamp, host, localServerToUpdate, null);
    }

    /**
     * <p>Update a local server (FlatFileTrancheServer) by checking specific server at a specific url is there any deletes to consider between two timestamps. If so, will perform deletes only if user is trusted.</p>
     * @param fromTimestamp Starting of period to check (inclusive)
     * @param toTimestamp End of period to check (inclusive)
     * @param host Host name for server to check for deletions
     * @param localServerToUpdate The FlatFileTrancheServer to check
     * @param deleteCount An integer array of size one to count number of chunks deleted. (Use final array so can use within thread.)
     * @return
     */
    private static boolean checkServerForChunksToDelete(final long fromTimestamp, final long toTimestamp, final String host, final FlatFileTrancheServer localServerToUpdate, final int[] deleteCount) {
        long timestampToStartAt = fromTimestamp;
        ATTEMPT:
        for (int attempt = 0; attempt < 3; attempt++) {
            try {

                // If not online, wait a second and try again
                StatusTableRow row = NetworkUtil.getStatus().getRow(host);
                if (row == null || !row.isOnline()) {
                    Thread.sleep(1000);
                    continue ATTEMPT;
                }

                TrancheServer ts = getConnection(host);

                boolean cont = true;
                Set<Activity> lastBatch = new HashSet();

                while (cont) {
                    cont = false;

                    Activity[] nextDeleteBatch = ts.getActivityLogEntries(timestampToStartAt, toTimestamp, maximumBatchSizeDelete, Activity.ACTION_DELETE);

                    List<Activity> deletes = new ArrayList(maximumBatchSizeDelete);

                    // Add to list, but verify haven't handled already. After all, multiple actions could hypothetically
                    // happen in a single timestamp instance
                    for (Activity a : nextDeleteBatch) {
                        if (!lastBatch.contains(a)) {
                            deletes.add(a);
                        }
                    }

                    // Update last batch so don't duplicate our delete efforts
                    lastBatch.clear();
                    lastBatch.addAll(deletes);

                    // Perform deletes, but only if trust signatures
                    DELETES:
                    for (Activity a : deletes) {

                        // First, make sure can load user
                        User user = localServerToUpdate.getConfiguration().getRecognizedUser(a.signature.getCert());
                        if (user == null) {
                            continue DELETES;
                        }

                        if (a.isMetaData()) {

                            // Only delete if user has permission on this server
                            if (!user.canDeleteMetaData()) {
                                continue DELETES;
                            }

                            // ----------------------------------------------------------------------------------------
                            // TODO>
                            //   Meta data should not be deleted this way. The activity log should log deletes 
                            //   on user+path+timestamp basis, and remove that 'signature' only.
                            // ----------------------------------------------------------------------------------------

                            if (IOUtil.hasMetaData(localServerToUpdate, a.hash)) {

                                localServerToUpdate.getDataBlockUtil().deleteMetaData(a.hash, "ServerStartThread found deleted meta data on " + host);

                                // Don't log; creates an infinite cycles between servers!

                                if (deleteCount != null) {
                                    deleteCount[0]++;
                                }
                            }
                        } else {

                            // Only delete if user has permission on this server
                            if (!user.canDeleteData()) {
                                continue DELETES;
                            }

                            if (IOUtil.hasData(localServerToUpdate, a.hash)) {

                                localServerToUpdate.getDataBlockUtil().deleteData(a.hash, "ServerStartThread found deleted data on " + host);

                                // Don't log: creates an infinite cycles between servers!

                                if (deleteCount != null) {
                                    deleteCount[0]++;
                                }
                            }
                        }
                    } // For all deletes in this batch

                    // If got a full batch, means we might not be done yet. Update the starting timestamp
                    // for the next batch
                    if (nextDeleteBatch.length == maximumBatchSizeDelete) {
                        cont = true;
                        timestampToStartAt = nextDeleteBatch[nextDeleteBatch.length - 1].timestamp;
                    }
                } // While potential entries remaining

                // If get here, no problems. Finished
                return true;
            } catch (Exception e) {
                System.err.println(e.getClass().getSimpleName() + " occurred while checking for deleted chunk on " + host + " (attempt #" + attempt + ") : " + e.getMessage());
                e.printStackTrace(System.err);
            } finally {
                releaseConnection(host);
            }
        } // For up to certain number of attempt

        // Couldn't check because offline or too many problems
        return false;
    } // checkServerForChunksToDelete

    /**
     * <p>Update a local server (FlatFileTrancheServer) by checking specific server at a specific url is there any new chunks to consider between two timestamps. If so, will add chunks only if user is trusted and belong to hash span.</p>
     * <p>Also performs a check for scenario known as 'Startup Radiation' (pun on Hawking Radiation) where, for tiny window of time, a chunk that is about to be deleted might be "rescued" by this activity. The delete should help prevent the replication of the "rescued" chunk, but cannot fully prevent it.</p>
     * <p>The only way to prevent the replication of a deleted chunk is to make sure that deletions are complete by verify after a time period that the chunk didn't replicate. This is a problem for a deletion tool and cannot be handled here.</p>
     * @param fromTimestamp Starting of period to check (inclusive)
     * @param toTimestamp End of period to check (inclusive)
     * @param host Host name for server to check for deletions
     * @param localServerToUpdate The FlatFileTrancheServer to check
     */
    public static boolean checkServerForChunksToAdd(final long fromTimestamp, final long toTimestamp, final String host, final FlatFileTrancheServer localServerToUpdate) {
        return checkServerForChunksToAdd(fromTimestamp, toTimestamp, host, localServerToUpdate, null, null);
    }

    private static boolean checkServerForChunksToAdd(final long fromTimestamp, final long toTimestamp, final String host, final FlatFileTrancheServer localServerToUpdate, final int[] addCount, final int[] deleteCount) {
        // need to update to new connection scheme
        long timestampToStartAt = fromTimestamp;
        for (int attempt = 0; attempt < 3; attempt++) {
            boolean contAdd = true;
            try {
                TrancheServer ts = getConnection(host);

                while (contAdd) {
                    contAdd = false;

                    // Be generous with batch start perchance slight difference in times on systems. They are supposed
                    // to be synchronized, but a fudge factor will prevent certain tricky issues
                    final long batchStart = TimeUtil.getTrancheTimestamp() - 3000;

                    Activity[] newChunksBatch = ts.getActivityLogEntries(timestampToStartAt, toTimestamp, maximumBatchSizeSet, Activity.ACTION_SET);

                    // Keep a reference to everything that was added. Later, we're going to check that they
                    // weren't deleted to prevent a tricky scenario!
                    Set<Activity> addedChunks = new HashSet();

                    // Perform set, but only if trust signatures
                    ADD:
                    for (Activity a : newChunksBatch) {

                        boolean isInHashSpan = false;
                        for (HashSpan hs : localServerToUpdate.getConfiguration().getHashSpans()) {
                            if (hs.contains(a.hash)) {
                                isInHashSpan = true;
                                break;
                            }
                        }

                        if (!isInHashSpan) {
                            continue ADD;
                        }

                        // First, make sure can load user
                        User user = localServerToUpdate.getConfiguration().getRecognizedUser(a.signature.getCert());
                        if (user == null) {
                            continue ADD;
                        }

                        if (a.isMetaData()) {

                            // Only delete if user has permission on this server
                            if (!user.canSetMetaData()) {
                                continue ADD;
                            }

                            // This will happen when the chunk was deleted. Note that if it was
                            // deleted and the server has it, meant it was replaced!
                            if (!IOUtil.hasMetaData(ts, a.hash)) {
                                continue ADD;
                            }

                            PropagationReturnWrapper rw = IOUtil.getMetaData(ts, a.hash, false);

                            byte[] chunk = null;

                            if (rw.isByteArraySingleDimension()) {
                                chunk = (byte[]) rw.getReturnValueObject();
                            } else if (rw.isByteArrayDoubleDimension()) {
                                chunk = ((byte[][]) rw.getReturnValueObject())[0];
                            } else {
                                throw new AssertionFailedException("Expecting one or two dimensional byte array, but found neither.");
                            }

                            // It's better to move on than throw an exception since server
                            // might have other chunks we'd like.
                            if (chunk == null) {
                                continue ADD;
                            }

                            boolean added = addMetaData(a.hash, chunk, localServerToUpdate, user);
                            if (added) {
                                if (addCount != null) {
                                    addCount[0]++;
                                }
                            }

                            addedChunks.add(a);
                            if (addCount != null) {
                                addCount[0]++;
                            }
                        } else {
                            if (!IOUtil.hasData(localServerToUpdate, a.hash)) {

                                // Only delete if user has permission on this server
                                if (!user.canSetData()) {
                                    continue ADD;
                                }

                                // This will happen when the chunk was deleted. Note that if it was
                                // deleted and the server has it, meant it was replaced!
                                if (!IOUtil.hasData(ts, a.hash)) {
                                    continue ADD;
                                }

                                PropagationReturnWrapper rw = IOUtil.getData(ts, a.hash, false);

                                byte[] chunk = null;

                                if (rw.isByteArraySingleDimension()) {
                                    chunk = (byte[]) rw.getReturnValueObject();
                                } else if (rw.isByteArrayDoubleDimension()) {
                                    chunk = ((byte[][]) rw.getReturnValueObject())[0];
                                } else {
                                    throw new AssertionFailedException("Expecting one or two dimensional byte array, but found neither.");
                                }

                                // It's better to move on than throw an exception since server
                                // might have other chunks we'd like.
                                if (chunk == null) {
                                    continue ADD;
                                }

                                localServerToUpdate.getDataBlockUtil().addData(a.hash, chunk);
                                addedChunks.add(a);
                                if (addCount != null) {
                                    addCount[0]++;
                                }
                            }
                        }
                    } // For all deletes in this batch

                    /**
                     * -----------------------------------------------------------------------------------------
                     *
                     * 'STARTUP RADIATION'
                     *
                     * There's one scenario where we could replicate a deleted chunk, 'startup radiation'.
                     *
                     * Say user starts deletion of chunk c. Server A is still starting up, and discovers c.
                     *
                     *   a) User starts deletion of c on network. Server A doesn't have the chunk, so it isn't impacted.
                     *   b) Server A finds chunk c on server B, and adds it to self.
                     *   c) Server B has chunk c deleted.
                     *
                     * Now server A has a bad copy of the chunk.
                     *
                     * Solution: Check if chunk has been deleted off server C after maximum time it'd take for a
                     *           deletion to span the entire network, and if so, delete it.
                     *
                     * Note: There is still a very unlikely scenario where a bad chunk can be replicated: there is
                     *       a brief time window during which a server gets a copy of the chunk and it is deleted. If
                     *       another server discovers the chunk, then the deleted chunk will get replicated!
                     *
                     *       This is unavoidable, as a server must have a chunk for it to be deleted; yet if it has
                     *       the chunk, it can be replicated!
                     *
                     * The probability of the happening:
                     *   p(chunk deleted while being retrieved during startup) * p(chunk replicated during time window its available)
                     *
                     * The solution: when user deleting chunks, should check to make sure didn't escape. If escape,
                     *               re-delete and check again. Keep checking until find that chunk didn't escape.
                     *
                     * -----------------------------------------------------------------------------------------
                     */                                //
                    // *************************************************************************************************
                    // Nothing to do if nothing was added
                    boolean contCheck = addedChunks.size() > 0;

                    Set<Activity> lastBatch = new HashSet();
                    long lastCheckStart = batchStart;
                    final long lastCheckStop = TimeUtil.getTrancheTimestamp() + 3;

                    while (contCheck) {
                        contCheck = false;

                        try {
                            Activity[] nextDeleteBatch = ts.getActivityLogEntries(lastCheckStart, lastCheckStop, maximumBatchSizeDelete, Activity.ACTION_DELETE);

                            List<Activity> deletes = new ArrayList(maximumBatchSizeDelete);

                            // Add to list, but verify haven't handled already. After all, multiple actions could hypothetically
                            // happen in a single timestamp instance
                            for (Activity a : nextDeleteBatch) {
                                if (!lastBatch.contains(a)) {
                                    deletes.add(a);
                                }
                            }

                            // Update last batch so don't duplicate our delete efforts
                            lastBatch.clear();
                            lastBatch.addAll(deletes);

                            final Set<Activity> dataChunksToDelete = new HashSet();
                            final Set<Activity> metaDataChunksToDelete = new HashSet();

                            // Look for chunks that were just added that were just deleted. Make sure _deleted_ from
                            // remote server _after_ they were _added_ to remote server!
                            DELETE:
                            for (Activity delete : deletes) {

                                // User must be recognized by system
                                User user = localServerToUpdate.getConfiguration().getRecognizedUser(delete.signature.getCert());
                                if (user == null) {
                                    continue DELETE;
                                }

                                ADD:
                                for (Activity add : addedChunks) {
                                    
                                    boolean isAfter = (delete.timestamp > add.timestamp);
                                    boolean isSameType = (add.isMetaData() == delete.isMetaData());
                                    boolean isSameChunk = isAfter && isSameType && add.hash.equals(delete.hash);
                                    
                                    // Don't bother--only care about deletes for chunks after they were added!
                                    if (!isSameChunk) {
                                        continue ADD;
                                    }

                                    // ------------------------------------------------------------
                                    //  If gets here, must be deleted since was added!
                                    // ------------------------------------------------------------
                                    if (delete.isMetaData()) {

                                        // Only delete if trusted
                                        if (!user.canDeleteMetaData()) {
                                            continue DELETE;
                                        }

                                        metaDataChunksToDelete.add(delete);
                                    } else {

                                        // Only delete if trusted
                                        if (!user.canDeleteData()) {
                                            continue DELETE;
                                        }

                                        dataChunksToDelete.add(delete);
                                    }
                                }
                            } // Checking all deletes

                            // Perform deletes!
                            for (Activity a : metaDataChunksToDelete) {
                                if (IOUtil.hasMetaData(localServerToUpdate, a.hash)) {
                                    localServerToUpdate.getDataBlockUtil().deleteMetaData(a.hash, "Deleting a meta data chunk (startup radiation)");
                                    if (deleteCount != null) {
                                        deleteCount[0]++;
                                    }
                                }
                            }
                            for (Activity a : dataChunksToDelete) {
                                if (IOUtil.hasData(localServerToUpdate, a.hash)) {
                                    localServerToUpdate.getDataBlockUtil().deleteData(a.hash, "Deleting a data chunk (startup radiation)");
                                    if (deleteCount != null) {
                                        deleteCount[0]++;
                                    }
                                }
                            }

                            metaDataChunksToDelete.clear();
                            dataChunksToDelete.clear();

                            // If got a full batch, means we might not be done yet. Update the starting timestamp
                            // for the next batch
                            if (nextDeleteBatch.length == maximumBatchSizeDelete) {
                                contCheck = true;
                                timestampToStartAt = nextDeleteBatch[nextDeleteBatch.length - 1].timestamp;
                            }
                        } catch (Exception e) {
                            System.err.println(e.getClass().getSimpleName() + " occurred while checking for new chunks (attempt #" + attempt + ") : " + e.getMessage());
                            e.printStackTrace(System.err);
                        } finally {
                            IOUtil.safeClose(ts);
                        }
                    } // Checking for scenario were added a chunk that was about to be deleted

                    // *************************************************************************************************

                    // If got a full batch, means we might not be done yet. Update the starting timestamp
                    // for the next batch
                    if (newChunksBatch.length == maximumBatchSizeSet) {
                        contAdd = true;
                        lastCheckStart = newChunksBatch[newChunksBatch.length - 1].timestamp;
                    }
                } // While potential entries remaining

                // If made it here, finished!
                return true;
            } catch (Exception e) {
                debugErr(e);
            } finally {
                releaseConnection(host);
            }
        } // Attempts

        // Couldn't check because offline or too many problems
        return false;
    } // checkServerForChunksToAdd

    /**
     * <p>Wait for ServerStartupThread to finish running. Note this will only block for a maximum number of milliseconds, as set by parameter.</p>
     * @param maxTimeToWaitInMillis Maximum number of milliseconds to wait for thread to finish
     * @return True if finished; false if not finished (i.e., maximum time to wait elapsed and not complete)
     */
    public boolean waitForStartupToComplete(long maxTimeToWaitInMillis) {
        final long start = TimeUtil.getTrancheTimestamp();

        // Increments of time in which to join on thread. 
        final long blockPeriods = 250;

        while (true) {
            // Time to exit? Has maximum wait time been reached?
            if (TimeUtil.getTrancheTimestamp() - start >= maxTimeToWaitInMillis) {
                return this.exitted;
            }

            try {
                // Join for a small period of time
                this.join(blockPeriods);
            } catch (InterruptedException ie) { /* nope */ }

            // If the exitted flag is set, the thread has complete
            if (this.exitted) {
                return true;
            }
        }
    }
    private static Set<String> hostsToRelease = new HashSet();

    /**
     * <p>Get the connection.</p>
     * <p>Make sure you call releaseConnection. Any locks will be released.</p>
     * @param host
     * @return
     * @throws java.lang.Exception
     */
    private static TrancheServer getConnection(String host) throws Exception {
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
     * @param host
     */
    private static void releaseConnection(String host) {
        if (hostsToRelease.contains(host)) {
            hostsToRelease.remove(host);
            ConnectionUtil.unlockConnection(host);
            ConnectionUtil.safeCloseHost(host);
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        ServerStartupThread.debug = debug;
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
            DebugUtil.printOut(ServerStartupThread.class.getName() + "> " + line);
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
}
