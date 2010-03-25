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
package org.tranche.hash;

import org.tranche.util.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Splitting some of functionality from Jayson's SimpleDiskBackedBigHashList to a Set so that both classes behave in their expected manner.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class DiskBackedBigHashSet {

    // the number of files buffered
    public long filesBuffered = 0;
    // how many to buffer in memory
    public static final int BUFFER_IN_MEMORY = 10000;
    private static final int READ_AND_WRITE_BUFFER = 1000;
    // two buffers to recycle for fast IO
    private byte[] readBuffer = new byte[BigHash.HASH_LENGTH * READ_AND_WRITE_BUFFER];
    private byte[] writeBuffer = new byte[BigHash.HASH_LENGTH * READ_AND_WRITE_BUFFER];
    // the buffer of items to add
    Set<BigHash> toAdd = new HashSet();
    // the buffer of items to delete
    Set<BigHash> toDelete = new HashSet();
    // the file that stores the rest on disk
    File file;
    private int diskCount;
    private boolean isTempFile = false;
    /**
     * <p>If set to true, flushes buffer to disk before critical operations. False is faster (old way), but can be inaccurate under some circumstance.</p>
     */
    private boolean autoWriteBeforeCriticalOperation = true;
    /**
     * If testing, want to be able to set lower BUFFER_IN_MEMORY.
     */
    private int testBufferSize = -1;

    /**
     * <p>Build a instance using a temp file.</p>
     */
    public DiskBackedBigHashSet() {
        diskCount = 0;
        file = TempFileUtil.createTemporaryFile(".sdbbhs");
        isTempFile = true;
    }

    /**
     * <p>Provide a file used to store records on disk.</p>
     * <p>Primary use case is when information needs to persist across instances.</p>
     */
    public DiskBackedBigHashSet(File fileForRecords) {
        diskCount = 0;
        file = fileForRecords;
        isTempFile = false;
    }

    /**
     * <p>Clears out all the hashes.</p>
     * @throws java.lang.Exception Since must delete and create files.
     */
    public void clear() throws Exception {
        this.toAdd.clear();
        this.toDelete.clear();

        IOUtil.safeDelete(this.file);
        this.file.createNewFile();

        filesBuffered = 0;
        diskCount = 0;

        if (!this.file.exists()) {
            throw new Exception("Could not create file for list at: " + this.file.getAbsolutePath());
        }
    }
    // the add method

    public synchronized final void add(BigHash hash) {
        /**
         * If in delete buffer, remove it. Else, try to add it.
         */
        boolean removed = toDelete.remove(hash);
        boolean added = toAdd.add(hash);

        if (added || removed) {
            filesBuffered++;
        }

        checkBuffers();
    }

    public synchronized final void delete(BigHash hash) {

        boolean removed = toAdd.remove(hash);
        boolean added = toDelete.add(hash);

        if (added || removed) {
            filesBuffered--;
        }

        checkBuffers();
    }

    /**
     * <p>Note: this might return an inaccurate result under two conditions:</p>
     * <ul>
     *   <li>Too high if toAdd contains hashes already in the disk-backed portion of set and not flushed.</li>
     *   <li>Too low if toDelete contains hashes not in the disk-backed portion of set and not flushed.</li>
     * </ul>
     * <p>The result will be accurate is autoWriteBeforeCriticalOperation is set to true.</p>
     * @return Number of BigHash's in set
     */
    public synchronized int size() {
        boolean isAllowedEstimate = !autoWriteBeforeCriticalOperation;
        return size(isAllowedEstimate);
    }

    /**
     * <p>Offers caller more choice than size(). Caller decides whether estimate is good enough, which can really save some time.</p>
     * @param isAllowEstimate If true, skip expensive operations that guarentee exact size. Estimates are not reliable, but can be useful if an estimate is appropriate.
     * @return The size of the collection, either actual or estimate.
     */
    public synchronized int size(boolean isAllowEstimate) {

        // If not allowed to estimate, do expensive operation.
        if (!isAllowEstimate) {
            checkBuffers(true);
        }

        int size = diskCount + toAdd.size() - toDelete.size();

        if (size < 0) {
            return 0;
        }

        return size;
    }

    public synchronized final boolean contains(BigHash hash) throws Exception {

        // If in toDelete, cannot be true
        if (toDelete.contains(hash)) {
            return false;

        }
        if (toAdd.contains(hash)) {
            return true;
        }

        // open up the IO streams
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);

            // buffer the size of one hash
            byte[] buffer = new byte[BigHash.HASH_LENGTH];

            int debug = 0;

            // Not found in-memory, check disk
            for (int bytesRead = bis.read(buffer); bytesRead != -1 && bytesRead == buffer.length; bytesRead = bis.read(buffer)) {
                BigHash test = BigHash.createFromBytes(buffer);
                int compare = test.compareTo(hash);
                debug++;
                // if same, end at true
                if (compare == 0) {
                    return true;
                // if greater, keep going
                }
                if (compare < 0) {
                    continue;
                } // otherwise, stop. the hash isn't in this list
                else {
                    return false;
                }
            }
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
        }
        return false;
    }

    /**
     * 
     * @param offset
     * @param limit
     * @return
     * @throws java.lang.Exception
     */
    public synchronized final List get(long offset, long limit) throws Exception {

        /**
         * Check to see whether should write out buffers first
         */
        if (autoWriteBeforeCriticalOperation) {
            checkBuffers(true);
        }

        // figure out the seek location
        long seekLocation = (long) BigHash.HASH_LENGTH * offset;

        // buffer the entries to return
        ArrayList<BigHash> hashes = new ArrayList();

        RandomAccessFile raf = null;

        try {
            //
            raf = new RandomAccessFile(file, "rw");

            // seek the raf to the right place
            if (seekLocation < raf.length()) {
                // seek to the right spot
                raf.seek(seekLocation);

                // loop until there are no more bytes
                for (int bytesRead = raf.read(readBuffer); bytesRead != -1 && hashes.size() < limit; bytesRead = raf.read(readBuffer)) {
                    // round out to a hash
                    if (bytesRead % BigHash.HASH_LENGTH != 0) {
                        int moreBytesRequired = BigHash.HASH_LENGTH - (bytesRead % BigHash.HASH_LENGTH);
                        while (moreBytesRequired > 0) {
                            int newBytesRead = raf.read(readBuffer, bytesRead, moreBytesRequired);
                            moreBytesRequired -= newBytesRead;
                            bytesRead += newBytesRead;
                        }
                    }
                    // how many hashes to handle
                    int hashCount = bytesRead / BigHash.HASH_LENGTH;
                    // add all of the new hashes
                    for (int i = 0; i < hashCount && hashes.size() < limit; i++) {
                        // dupe the hash
                        byte[] dupe = new byte[BigHash.HASH_LENGTH];
                        System.arraycopy(readBuffer, i * BigHash.HASH_LENGTH, dupe, 0, BigHash.HASH_LENGTH);
                        BigHash hash = BigHash.createFromBytes(dupe);
                        // add if the has isn't deleted
                        if (!toDelete.contains(hash)) {
                            hashes.add(hash);
                        }
                    }
                }
            } // Done reading from disk

            // fall back on returning the non-deleted items from memory
            BigHash[] toAddArray = toAdd.toArray(new BigHash[0]);

            /**
             * What's the seek location? If greater than raf.length(), use that because we won't
             * want everything in memory. Otherwise, use the length of the file!
             */
            int start = (int) ((seekLocation - raf.length()) / BigHash.HASH_LENGTH);
            if (start < 0) {
                start = 0;
            }

            // otherwise, try to return unsaved items
            for (int i = start; start >= 0 && i < toAddArray.length && hashes.size() < limit; i++) {
                BigHash hash = toAddArray[i];
                if (!toDelete.contains(hash)) {
                    hashes.add(hash);
                }
            }

        } finally {
            if (raf != null) {
                raf.close();
            }
        }

        // return the in-memory buffer
        return hashes;
    }

    /**
     * 
     */
    private synchronized final void checkBuffers() {
        checkBuffers(false);
    }

    /**
     * 
     * @param flush
     */
    private synchronized final void checkBuffers(boolean flush) {

        /**
         * If nothing to do, exit unconditionally 
         */
        if (toAdd.size() == 0 && toDelete.size() == 0) {
            return;
        }

        long buffer = BUFFER_IN_MEMORY;
        if (this.testBufferSize > 0) {
            buffer = this.testBufferSize;
        }

        // if too many items are in memory, TCB
        if (toAdd.size() + toDelete.size() >= buffer || flush) {
            // reset the count
            filesBuffered = 0;

            RandomAccessFile raf = null;

            try {
                // shut down the raf
                //raf.close();
                // rename the old file
                File backup = TempFileUtil.createTemporaryFile("sdbbhs.backup");
                IOUtil.renameFallbackCopy(file, backup);

                // initialize the two readers
                RandomAccessFile original = new RandomAccessFile(backup, "r");
                try {
                    raf = new RandomAccessFile(file, "rw");

                    // get the buffers and sort them
                    BigHash[] add = toAdd.toArray(new BigHash[0]);
                    Arrays.sort(add);

                    // Update count on disk
                    diskCount = 0;

                    // indexes for the arrays
                    int addIndex = 0;

                    // use the write buffer
                    int writeBufferIndex = 0;

                    // copy the files contents
                    for (int bytesRead = original.read(readBuffer); bytesRead != -1; bytesRead = original.read(readBuffer)) {
                        // round out to a hash
                        if (bytesRead % BigHash.HASH_LENGTH != 0) {
                            int moreBytesRequired = BigHash.HASH_LENGTH - (bytesRead % BigHash.HASH_LENGTH);
                            while (moreBytesRequired > 0) {
                                int newBytesRead = original.read(readBuffer, bytesRead, moreBytesRequired);
                                moreBytesRequired -= newBytesRead;
                                bytesRead += newBytesRead;
                            }
                        }
                        // How many hashes to handle in this batch
                        int hashCount = bytesRead / BigHash.HASH_LENGTH;
                        // add all of the new hashes
                        for (int i = 0; i < hashCount; i++) {
                            // make the big hash
                            BigHash hash = BigHash.createFromBytes(readBuffer, BigHash.HASH_LENGTH * i);

                            // burn identical hash copies
                            while (addIndex < add.length && hash.compareTo(add[addIndex]) == 0) {
                                addIndex++;
                            }

                            // write out every hash that is before this hash
                            while (addIndex < add.length && add[addIndex].compareTo(hash) < 0) {
                                if (!toDelete.contains(add[addIndex])) {
                                    // check/flush write buffer
                                    if (writeBufferIndex >= READ_AND_WRITE_BUFFER) {
                                        raf.write(writeBuffer);
                                        writeBufferIndex = 0;
                                    }
                                    // copy to the write buffer
                                    System.arraycopy(add[addIndex].toByteArray(), 0, writeBuffer, BigHash.HASH_LENGTH * writeBufferIndex, BigHash.HASH_LENGTH);
                                    writeBufferIndex++;
                                    diskCount++;

                                    // inc the number of files buffered
                                    filesBuffered++;
                                }
                                addIndex++;
                            }

                            // write out this hash
                            if (!toDelete.contains(hash)) {
                                // check/flush write buffer
                                if (writeBufferIndex >= READ_AND_WRITE_BUFFER) {
                                    raf.write(writeBuffer);
                                    writeBufferIndex = 0;
                                }
                                // copy to the write buffer
                                System.arraycopy(hash.toByteArray(), 0, writeBuffer, BigHash.HASH_LENGTH * writeBufferIndex, BigHash.HASH_LENGTH);
                                writeBufferIndex++;
                                diskCount++;

                                // inc the number of files buffere
                                filesBuffered++;
                            }
                        } // For each hash in current batch

                    } // For all hash batches from RandomAccessFile

                    // write out the rest -- can't forget about these
                    while (addIndex < add.length) {
                        // check/flush write buffer
                        if (writeBufferIndex >= READ_AND_WRITE_BUFFER) {
                            raf.write(writeBuffer);
                            writeBufferIndex = 0;
                        }
                        // copy to the write buffer
                        System.arraycopy(add[addIndex].toByteArray(), 0, writeBuffer, BigHash.HASH_LENGTH * writeBufferIndex, BigHash.HASH_LENGTH);
                        writeBufferIndex++;
                        diskCount++;

                        // move to the next item to add
                        addIndex++;
                    }

                    // flush the write buffer
                    raf.write(writeBuffer, 0, writeBufferIndex * BigHash.HASH_LENGTH);
                } finally {
                    IOUtil.safeClose(original);
                    // clean up the temp file
                    IOUtil.safeDelete(backup);
                }

            } catch (Exception e) {
                e.printStackTrace(System.err);
                throw new RuntimeException("Can't manage disk-backed hash list!", e);
            } finally {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (Exception ex) {
                        System.err.println("Can't close RAF: " + ex.getMessage());
                        ex.printStackTrace(System.err);
                    }
                }
            }

            // purge the in-memory
            toAdd.clear();
            toDelete.clear();
        }
    }

    /**
     * <p>Close off resources.</p>
     * <p>If collection uses a temporary file, delete. Else flush contents for next use.</p>
     */
    public synchronized final void close() {
        if (isTempFile) {
            // If using temp storage, delete
            IOUtil.safeDelete(file);
        } else {
            // If using persistent storage, flush
            checkBuffers(true);
        }
    }

    /**
     * 
     * @return
     */
    public int getTestBufferSize() {
        return testBufferSize;
    }

    /**
     * 
     * @param testBufferSize
     */
    public void setTestBufferSize(int testBufferSize) {
        this.testBufferSize = testBufferSize;
    }

    /**
     * <p>Converts contents to array. Note: the purpose of this collection is to disk back to avoid tremendous memory overhead. Only use if know collection will fit in memory.</p>
     * @return
     * @throws java.lang.Exception
     */
    public BigHash[] toArray() throws Exception {

        List<BigHash> fullBatch = this.get(0, this.size());

        return fullBatch.toArray(new BigHash[0]);
    }

    /**
     * <p>If true, will write buffers to disk before the following operations:</p>
     * <ul>
     *   <li>Calculating size</li>
     *   <li>Using get method</li>
     * </ul>
     * @return True if writing buffers to disk before critical operations, false otherwise
     */
    public boolean isAutoWriteBeforeCriticalOperation() {
        return autoWriteBeforeCriticalOperation;
    }

    /**
     * <p>If true, will write buffers to disk before the following operations:</p>
     * <ul>
     *   <li>Calculating size</li>
     *   <li>Using get method</li>
     * </ul>
     * @param autoWriteBeforeCriticalOperation True if writing buffers to disk before critical operations, false otherwise
     */
    public void setAutoWriteBeforeCriticalOperation(boolean autoWriteBeforeCriticalOperation) {
        this.autoWriteBeforeCriticalOperation = autoWriteBeforeCriticalOperation;
    }
}
