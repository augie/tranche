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
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import org.tranche.TrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataUtil;
import org.tranche.network.ConnectionUtil;
import org.tranche.servers.ServerUtil;
import org.tranche.time.TimeUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.Text;

/**
 * <p>Thread replaces any missing chunks from a corrupted data block. Reads requests for corrupted DataBlock objects from a queue.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ReplaceCorruptedDataBlockThread extends Thread {

    /**
     * 
     */
    private final static boolean isDebug = false;
    /**
     * 
     */
    private final ArrayBlockingQueue<ChunkToRetrieve> chunksQueue;
    /**
     * <p>Maximum size before allowed queue blocks.</p>
     */
    public final static int MAX_QUEUE_SIZE = 5000;
    /**
     * 
     */
    private final DataBlockUtil dbu;
    /**
     * 
     */
    private boolean stop = false;
//    /**
//     * 
//     */
//    private static ReplaceCorruptedDataBlockThread singleton = null;
//    /**
//     * 
//     */
//    private static final Object singletonLock = new Object();
    /**
     * An identifier to make sure unique thread.
     */
    private final String id;

//    /**
//     * <p>Get singleton instance of thread. Will discard previous singleton if DataBlockUtil changes.</p>
//     * @param dbu
//     * @return
//     */
//    public static final ReplaceCorruptedDataBlockThread getInstance(DataBlockUtil dbu) {
//
//        final boolean nullSingleton = (singleton == null);
//        boolean o = false;
//        if (!nullSingleton) {
//            o = !singleton.dbu.equals(dbu);
//        }
//        final boolean outdatedDBU = o;
//        
//        synchronized (singletonLock) {
//            if (nullSingleton || outdatedDBU) {
//                if (nullSingleton) {
//                    printTracer("Created singleton at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
//                } else if (outdatedDBU) {
//                    printTracer("Replace previous singleton because DBU outdated at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
//                } else {
//                    printTracer("Not sure why instantiated singleton at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
//                }
//                singleton = new ReplaceCorruptedDataBlockThread(dbu);
//            }
//        }
//
//        return singleton;
//    }
    /**
     * Private constructor. This is a singleton.
     * @param dataHashes List of data hashes that need replaced immediately.
     * @param metaHashes List of meta data hashes that need replaced immediately.
     */
    protected ReplaceCorruptedDataBlockThread(DataBlockUtil dbu) {
        super("Download missing chunks from corrupted DataBlock thread");

        this.id = String.valueOf(TimeUtil.getTrancheTimestamp());

        this.dbu = dbu;
        this.chunksQueue = new ArrayBlockingQueue(MAX_QUEUE_SIZE);

        setDaemon(true);
        setPriority(Thread.MIN_PRIORITY);
    }
    /**
     * 
     */
    private boolean isCurrentlyServicing = false;

    @Override()
    public void run() {
//
//        // If stopped, must work through queue.
//        while (!isStop() || !isQueueEmpty()) {
//
//            try {
//                ChunkToRetrieve chunkToRetrieve = null;
//
//                synchronized (chunksQueue) {
//                    chunkToRetrieve = chunksQueue.poll();
//                    isCurrentlyServicing = true;
//                }
//
//                if (chunkToRetrieve == null) {
//                    isCurrentlyServicing = false;
//                    Thread.yield();
//                    Thread.sleep(500);
//                    continue;
//                }
//
//                printTracer("Serving " + (chunkToRetrieve.isIsMetaData() ? "meta data" : "data") + " chunk to retrieve: " + chunkToRetrieve.getHash().toString().substring(0, 12) + "...");
//
//                /* If is a data chunk.
//                 */
//                if (!chunkToRetrieve.isIsMetaData()) {
//
//                    // Need to keep track of what is found and what isn't
//                    boolean chunkFound = false;
//
//                    // If chunk doesn't verify, keep copy. We prefer a valid
//                    // copy, but we'll grab an invalid copy and try to replace
//                    // it in the healing thread
//                    byte[] lastQuestionableChunk = null;
//
//                    // Use every server at our disposal!
//                    for (String url : getServersToUse()) {
//                        TrancheServer ts = null;
//                        try {
//
//                            // Give server up to half second plus yields to connect.
//                            // Don't want to miss opportunity to replace server.
//                            int attempt = 0;
//                            while (!ServerUtil.isServerOnline(url) && attempt < 10) {
//                                Thread.sleep(50);
//                                Thread.yield();
//                                attempt++;
//                            }
//
//                            ts = ConnectionUtil.connectURL(url, false);
//
//                            // No point wasting time to see if has. Just
//                            // try to get. Catch exceptions and move on.
//                            byte[] chunk = (byte[]) IOUtil.getData(ts, chunkToRetrieve.getHash(), false).getReturnValueObject();
//
//                            if (chunk == null) {
//                                throw new Exception("Should have thrown an exception anyhow.");
//                            }
//
//                            BigHash verifyHash = new BigHash(chunk);
//
//                            // If doesn't verify, keep a reference. Might
//                            // use it if not find a good copy
//                            if (!verifyHash.equals(chunkToRetrieve.getHash())) {
//                                lastQuestionableChunk = chunk;
//                                throw new Exception("Chunk didn't verify, try next.");
//                            }
//
//                            // We found the hash
//                            this.dbu.addData(chunkToRetrieve.getHash(), chunk);
//                            chunkFound = true;
//                            break;
//
//                        } catch (Exception ex) {
//                            // Nope, next...
//                        } finally {
//                            IOUtil.safeClose(ts);
//                        }
//                    }
//
//                    // Use unverified chunk if didn't find good copy
//                    if (!chunkFound && lastQuestionableChunk != null) {
//                        try {
//                            this.dbu.addData(chunkToRetrieve.getHash(), lastQuestionableChunk);
//                            chunkFound = true;
//                        } catch (Exception ex) {
//                            // Nothing to do, behavior on this level subject to change.
//                            // The chunk simply wasn't found, and the healing thread
//                            // should catch down the road.
//                        }
//                    }
//
//                    if (chunkFound) {
//                        this.dbu.incrementDownloadedChunksFromCorruptedDataBlockCount();
//                        printTracer("Successfully retrieved " + (chunkToRetrieve.isIsMetaData() ? "meta data" : "data") + " chunk: " + chunkToRetrieve.getHash().toString().substring(0, 12) + "...");
//                    } else {
//                        this.dbu.incrementLostChunksFromCorruptedDataBlockCount();
//                        printTracer("Failed to retrieve " + (chunkToRetrieve.isIsMetaData() ? "meta data" : "data") + " chunk: " + chunkToRetrieve.getHash().toString().substring(0, 12) + "...");
//                    }
//                } /* Handle meta data chunk
//                 */ else {
//                    // Need to keep track of what is found and what isn't
//                    boolean chunkFound = false;
//
//                    // If chunk doesn't verify, keep copy. We prefer a valid
//                    // copy, but we'll grab an invalid copy and try to replace
//                    // it in the healing thread
//                    byte[] lastQuestionableChunk = null;
//
//                    // Use every server at our disposal!
//                    for (String url : getServersToUse()) {
//                        TrancheServer ts = null;
//                        try {
//                            ts = ConnectionUtil.connectURL(url, false);
//
//                            // No point wasting time to see if has. Just
//                            // try to get. Catch exceptions and move on.
//                            byte[] chunk = (byte[]) IOUtil.getMetaData(ts, chunkToRetrieve.getHash(), false).getReturnValueObject();
//
//                            if (chunk == null) {
//                                throw new Exception("Should have thrown an exception anyhow.");
//                            }
//
//                            boolean verified = false;
//
//                            InputStream bais = null;
//                            try {
//                                bais = new ByteArrayInputStream(chunk);
//                                MetaData md = MetaDataUtil.read(bais);
//
//                                if (md == null) {
//                                    throw new Exception("Should have thrown an exception anyhow.");
//                                }
//
//                                verified = true;
//                            } catch (Exception ex) {
//                                // Nope, wasn't verified.
//                            } finally {
//                                IOUtil.safeClose(bais);
//                            }
//
//                            // If doesn't verify, keep a reference. Might
//                            // use it if not find a good copy
//                            if (!verified) {
//                                lastQuestionableChunk = chunk;
//                                throw new Exception("Chunk didn't verify, try next.");
//                            }
//
//                            // We found the hash
//                            this.dbu.addMetaData(chunkToRetrieve.getHash(), chunk);
//                            chunkFound = true;
//                            break;
//
//                        } catch (Exception ex) {
//                            // Nope, next...
//                        } finally {
//                            IOUtil.safeClose(ts);
//                        }
//                    }
//
//                    // Use unverified chunk if didn't find good copy
//                    if (!chunkFound && lastQuestionableChunk != null) {
//                        try {
//                            this.dbu.addMetaData(chunkToRetrieve.getHash(), lastQuestionableChunk);
//                            chunkFound = true;
//                        } catch (Exception ex) {
//                            // Nothing to do, behavior on this level subject to change.
//                            // The chunk simply wasn't found, and the healing thread
//                            // should catch down the road.
//                        }
//                    }
//
//                    if (chunkFound) {
//                        this.dbu.incrementDownloadedChunksFromCorruptedDataBlockCount();
//                        printTracer("Successfully retrieved " + (chunkToRetrieve.isIsMetaData() ? "meta data" : "data") + " chunk: " + chunkToRetrieve.getHash().toString().substring(0, 12) + "...");
//
//                    } else {
//                        this.dbu.incrementLostChunksFromCorruptedDataBlockCount();
//                        printTracer("Failed to retrieve " + (chunkToRetrieve.isIsMetaData() ? "meta data" : "data") + " chunk: " + chunkToRetrieve.getHash().toString().substring(0, 12) + "...");
//
//                    }
//                } // Retrieve if meta data chunk
//            } catch (Exception ex) {
//                System.err.println("*** An unexpected exception occurred in the thread that replaces corrupted data block chunks<" + ex.getClass().getSimpleName() + ">: " + ex.getMessage() + " ***");
//                ex.printStackTrace(System.err);
//            }
//        } // while should run
//
//        // Make sure flagged off
//        isCurrentlyServicing = false;
    }

    /**
     * <p>Synchronizes on the collection.</p>
     * @return True if queue to retrieve is empty, false otherwise.
     */
    private boolean isQueueEmpty() {
        boolean isEmpty = false;

        synchronized (chunksQueue) {
            isEmpty = chunksQueue.isEmpty();
        }
        return isEmpty;
    }

    /**
     * <p>Synchronizes on the collection.</p>
     * @return The number of elements in the queue.
     */
    private int getQueueSize() {
        int queueSize = -1;

        synchronized (chunksQueue) {
            queueSize = chunksQueue.size();
        }

        return queueSize;
    }

//    /**
//     * <p>Determine context of server and returns appropriate servers.</p>
//     * @return Array of server urls to use.
//     */
//    private String[] getServersToUse() {
//        String[] serversToUse = null;
//
//        serversToUse = ServerUtil.getCoreServers().toArray(new String[0]);
//
//        return serversToUse;
//    }

    /**
     * <p>Returns value for flag that determines if the thread should stop.</p>
     * <p>Note that the thread will continue until queue is empty.</p>
     * @return True if set to stop, false otherwise
     */
    public boolean isStop() {
        return stop;
    }

    /**
     * <p>Set whether the thread should stop.</p>
     * <p>Note that the thread will continue until queue is empty.</p>
     * @param stop True if want to stop, false otherwise
     */
    public void setStop(boolean stop) {
        this.stop = stop;
    }

    /**
     * <p>Add a chunk that should be repaired from a corrupted data block.</p>
     * @param hash
     * @param isMetaData
     */
    public void addChunkToRetrieve(BigHash hash, boolean isMetaData) throws InterruptedException {
        boolean isAdded = false;
        final ChunkToRetrieve chunkToRetrieve = new ChunkToRetrieve(hash, isMetaData);

        while (!isAdded) {
            synchronized (this.chunksQueue) {
                isAdded = this.chunksQueue.add(chunkToRetrieve);
            }

            if (!isAdded) {
                Thread.yield();
                Thread.sleep(50);
            }
        }

        if (isAdded) {
            printTracer("Added " + (isMetaData ? "meta data" : "data") + " chunk to retrieve: " + hash.toString().substring(0, 12) + "...");
        }
    }

    /**
     * <p>Wait up to a certain amount of time for queue to empty. This is done to alleviate resources on server.</p>
     * @param maxTimeToWaitMillis Maximum time to wait for queue to empty in milliseconds. For example, if want to wait up to five seconds, value would be 5000.
     * @throws java.lang.InterruptedException
     */
    public void waitForQueueToEmpty(long maxTimeToWaitMillis) throws InterruptedException {
        final long start = TimeUtil.getTrancheTimestamp();
        boolean hasWaitedLongEnough = TimeUtil.getTrancheTimestamp() - start > maxTimeToWaitMillis;
        while ((!this.isQueueEmpty() || isCurrentlyServicing) && !hasWaitedLongEnough) {
            Thread.yield();
            Thread.sleep(50);
            hasWaitedLongEnough = TimeUtil.getTrancheTimestamp() - start > maxTimeToWaitMillis;
        }
        if (!this.isQueueEmpty() || hasWaitedLongEnough || isCurrentlyServicing) {
            printTracer("Waited " + Text.getPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp() - start) + " for queue to empty, but still has " + getQueueSize() + " items. <Is " + (isCurrentlyServicing ? "" : "not") + " currently servicing a chunk>");
        }
    }

    /**
     * <p>Print a tracer if a debug flag is set.</p>
     * @param message
     */
    private void printTracer(String message) {
        if (isDebug) {
            System.out.println("REPLACE_CORRUPTED_DATA_BLOCK_THREAD " + this.id + "> " + message);
        }
    }

    /**
     * <p>Wrapper for chunk so can hold in a collection.</p>
     */
    private class ChunkToRetrieve {

        private final BigHash hash;
        private final boolean isMetaData;

        ChunkToRetrieve(BigHash hash, boolean isMetaData) {
            this.hash = hash;
            this.isMetaData = isMetaData;
        }

        /**
         * <p>Get hash for chunk to retrieve.</p>
         * @return
         */
        public BigHash getHash() {
            return hash;
        }

        /**
         * <p>Returns true if is meta data.</p>
         * @return
         */
        public boolean isIsMetaData() {
            return isMetaData;
        }
    }
}
