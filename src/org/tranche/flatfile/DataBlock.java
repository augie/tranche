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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import org.tranche.annotations.Fix;
import org.tranche.commons.Debuggable;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.exceptions.UnexpectedEndOfDataBlockException;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;

/**
 * <p>Represents a block of data. Might have more than one file in it.</p>
 * <p>Note that the following critical activities are synchronized on the DataBlock object:</p>
 * <ul>
 *   <li>Adding bytes to data block</li>
 *   <li>Checking whether data block has bytes</li>
 *   <li>Getting bytes from data block</li>
 *   <li>Deleting block</li>
 *   <li>Getting hashes in data block</li>
 * </ul>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
@Fix(problem = "OutOfMemory due to greater than 450K array of subBlocks instantiated, almost all with 256 null elements. Took around 400MB or more memory.", solution = "Lazily instantiate subBlocks, since only accessed when needed. If servers have a lot of disk space, need substantially more than 512MB.", day = 15, month = 8, year = 2008, author = "Bryan Smith")
public class DataBlock extends Debuggable implements Comparable {

    public static BigHash HASH_LENGTH_ZERO = new BigHash(new byte[0]);
    /**
     * <p>The maximum DataBlock size, in bytes.</p>
     */
    public static final int MAX_BLOCK_SIZE = 100 * 1024 * 1024;
    /**
     * <p>The amount of headers per file.</p>
     * <p>Each header's size in bytes is equal to BigHash.HASH_LENGTH + 1 + 1 + 4 + 4. Every new block will start with this many headers. To get the blocks size, you must also include all the data in the block.</p>
     */
    public static final int HEADERS_PER_FILE = 1000;    // keep the file reference
    /**
     * <p>The two-letter file name of the DataBlock.</p>
     */
    String filename;
    /**
     * <p>The DataDirectoryConfiguration object to which this DataBlock instance belongs.</p>
     */
    DataDirectoryConfiguration ddc;
    // keep the sub-directories
    private DataBlock[] subBlocks = null;    // flags for meta-data and data
    /**
     * <p>The flag representing a data chunk.</p>
     */
    static final byte DATA = 1;
    /**
     * <p>The flag representing a meta data chunk.</p>
     */
    static final byte META_DATA = 2;    // status flags
    /**
     * <p>The status flag representing an okay chunk.</p>
     */
    static final byte STATUS_OK = 0;
    /**
     * <p>The status flag representing a deleted chunk.</p>
     */
    static final byte STATUS_DELETED = 1;
    /**
     * <p>Maximum wasted space allowed before this block resizes itself.</p>
     */
    private final int MAX_WASTED_SPACE_ALLOWED = 1024 * 1024 * 5;
    /**
     * <p>Used to read the header of the file to see what is in it one entry at a time.</p>
     * <p>This is the size of an entry in bytes. Each header is hash + (byte) type + (byte) status + int (offset in block) + int (size)</p>
     */
    static final int bytesPerEntry = (BigHash.HASH_LENGTH + 1 + 1 + 4 + 4);
    /**
     * <p>Used to read in the entire header of the file. This is the total header size in bytes.</p>
     */
    static final int bytesToRead = bytesPerEntry * HEADERS_PER_FILE;
    /**
     * <p>Need a reference back to DBU to repair files</p>
     */
    private final DataBlockUtil dbu;

    /**
     * 
     * @param filename
     * @param ddc
     * @param dbu
     */
    public DataBlock(String filename, DataDirectoryConfiguration ddc, DataBlockUtil dbu) {
        this.filename = filename;
        this.ddc = ddc;
        this.dbu = dbu;
    }

    /**
     * <p>Returns true if this DataBlock is a directory. (I.e., it has children DataBlock instances.)</p>
     * @return
     */
    public final boolean isDirectory() {
        // fall back on checking the file
        return new File(ddc.getDirectoryFile().getAbsolutePath() + filename).isDirectory();
    }

    /**
     * <p>Get absolute path to underlying file for DataBlock. No guarentee if regular file or directory.</p>
     * @return The absolute path to the file
     */
    public final String getAbsolutePath() {
        return new File(ddc.getDirectoryFile().getAbsolutePath(), filename).getAbsolutePath();
    }

    /**
     * <p>Returns true if this file is half-way through a merge.</p>
     * @return
     */
    public final boolean isMerging() {
        return new File(ddc.getDirectoryFile().getAbsolutePath() + filename + ".merge").exists();
    }

    /**
     * <p>Return the size in bytes of the underlying file for the DataBlock instance.</p>
     * @return
     */
    public final long length() {
        return new File(ddc.getDirectoryFile().getAbsolutePath() + filename).length();
    }

    /**
     * <p>Returns the underlying file for the DataBlock. Might be a merge file (.merge) or the normal file/directory.</p>
     * @return
     */
    private final File getRegularOrMergeFile() {
        // first attempt to find the merge file
        File merge = new File(ddc.getDirectoryFile().getAbsolutePath() + filename + ".merge");
        if (merge.exists()) {
            return merge;
        }
        // fall back on regular file
        File regular = new File(ddc.getDirectoryFile().getAbsolutePath() + filename);
        return regular;
    }

    /**
     * <p>Returns the list of either data or meta-data hashes stored in this block. If true is passed as a parameter, only meta-data is returned. Otherwise only data hashes are returned.</p>
     * @param isMetaData
     * @return
     * @throws java.lang.Exception
     */
    public synchronized final List<BigHash> getHashes(boolean isMetaData) throws Exception {
        // if is a directory and no merge file, pass down the chain
        if (isDirectory() && !isMerging()) {
            return new ArrayList(0);
        }

//        // read the header of the file to see what is in it
//        // each header is hash + (byte) type + (byte) status + int (offset in block) + int (size)
//        int bytesPerEntry = (BigHash.HASH_LENGTH + 1 + 1 + 4 + 4);
//        int bytesToRead = bytesPerEntry * HEADERS_PER_FILE;
        // buffer that amount
        byte[] buf = new byte[bytesToRead];

        // list of hashes
        ArrayList<BigHash> hashesToReturn = new ArrayList();

        File blockFile = getRegularOrMergeFile();
        // If file not exist, return empty list of hashes
        if (!blockFile.exists()) {
            return hashesToReturn;        // read from the file
        }
        RandomAccessFile ras = new RandomAccessFile(blockFile, "r");
        try {
            // convert the boolean to meta-data or data bit
            final byte isMetaDataByte = isMetaData ? META_DATA : DATA;

            // get the complete header
            fillWithBytes(buf, ras, blockFile.getAbsolutePath(), "Reading in header to get hashes for " + (isMetaData ? "meta data" : "data") + ".");
            // check for the hash
            for (int i = 0; i < HEADERS_PER_FILE; i++) {
                // calc the offset
                int offset = i * bytesPerEntry;
                // parse the entry parts: hash, type, status, offset, size
                BigHash h = BigHash.createFromBytes(buf, offset);
                byte type = buf[offset + BigHash.HASH_LENGTH];
                byte status = buf[offset + BigHash.HASH_LENGTH + 1];
                int o = buf[BigHash.HASH_LENGTH + offset + 2] << 24 | (buf[BigHash.HASH_LENGTH + offset + 2 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 2 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 2 + 3] & 0xff);
                int s = buf[BigHash.HASH_LENGTH + offset + 6] << 24 | (buf[BigHash.HASH_LENGTH + offset + 6 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 6 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 6 + 3] & 0xff);

                // break out of the loop if the entry size is zero
                if (s == 0 && o == 0) {
                    break;
                    // if not the same hash, continue
                }
                if (status != STATUS_OK || type != isMetaDataByte) {
                    continue;                // add the hash to the list
                    // be sure to ditch the reference to the big array via .toByteArray()
                }
                hashesToReturn.add(BigHash.createFromBytes(h.toByteArray()));
            }

            return hashesToReturn;
        } finally {
            ras.close();
        }
    }

    /**
     * <p>Uses cached information to get chunk.</p>
     * @param o Offset of chunk
     * @param s Size of chunk
     * @return Bytes for chunk
     * @throws java.lang.Exception
     */
    public synchronized final byte[] getBytes(final int o, final int s) throws Exception {
        // read from the file
        File rasFile = getRegularOrMergeFile();

        // if the file doesn't exist, throw a FNF exception
        if (!rasFile.exists()) {
            // if here, the file doesn't exist
            throw new FileNotFoundException("Bytes don't exist on this server for data block file; could not find file from getBytes(int,int): " + rasFile.getAbsolutePath());
        }

        if (rasFile.isDirectory()) {
            return null;
        }
        RandomAccessFile ras = new RandomAccessFile(rasFile, "r");

        try {
            byte[] content = new byte[s];
            // seek to the right spot
            ras.seek(o);
            // load the buffer
            fillWithBytes(content, ras, rasFile.getAbsolutePath(), "Reading in chunk based on cached offset and size to return.");

            // return the data
            return content;
        } finally {
            ras.close();
        }
    }

    /**
     * <p>Returns the bytes (chunk) representing by the hash, or throws a FileNotFoundException if not found.</p>
     * @param hash
     * @param isMetaData
     * @return
     * @throws java.lang.Exception
     */
    public synchronized final byte[] getBytes(BigHash hash, boolean isMetaData) throws Exception {
        // if is a directory and no merge file, pass down the chain
        if (isDirectory() && !isMerging()) {
            return ddc.dbu.getDataBlockToAddChunk(hash).getBytes(hash, isMetaData);
        }

//        // read the header of the file to see what is in it
//        // each header is hash + (byte) type + (byte) status + int (offset in block) + int (size)
//        int bytesPerEntry = (BigHash.HASH_LENGTH + 1 + 1 + 4 + 4);
//        int bytesToRead = bytesPerEntry * HEADERS_PER_FILE;
        // buffer that amount
        byte[] buf = new byte[bytesToRead];

        // convert the boolean to meta-data or data bit
        final byte isMetaDataByte = isMetaData ? META_DATA : DATA;

        // read from the file
        File rasFile = getRegularOrMergeFile();

        // if the file doesn't exist, throw a FNF exception
        if (!rasFile.exists()) {
            // if here, the file doesn't exist
            throw new FileNotFoundException("Bytes don't exist on this server for data block file; could not find file from getBytes(BigHash,boolean): " + rasFile.getAbsolutePath());
        }

        if (rasFile.isDirectory()) {
            int stopHere = 0;
        }
        RandomAccessFile ras = new RandomAccessFile(rasFile, "r");

        try {
            // get the complete header
            fillWithBytes(buf, ras, rasFile.getAbsolutePath(), "Reading header to get " + (isMetaData ? "meta data" : "data") + " chunk.");
            // check for the hash
            int entryNumber = 0;
            for (int i = 0; i < HEADERS_PER_FILE; i++) {
                // Update so know how many read
                entryNumber = i;
                // calc the offset
                int offset = i * bytesPerEntry;
                // parse the entry parts: hash, type, status, offset, size
                BigHash entryHash = BigHash.createFromBytes(buf, offset);
                byte type = buf[offset + BigHash.HASH_LENGTH];
                byte status = buf[offset + BigHash.HASH_LENGTH + 1];
                int entryOffset = buf[BigHash.HASH_LENGTH + offset + 2] << 24 | (buf[BigHash.HASH_LENGTH + offset + 2 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 2 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 2 + 3] & 0xff);
                int chunkSize = buf[BigHash.HASH_LENGTH + offset + 6] << 24 | (buf[BigHash.HASH_LENGTH + offset + 6 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 6 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 6 + 3] & 0xff);

                // break out of the loop if the entry size is zero
                if (chunkSize == 0 && entryOffset == 0 && !entryHash.equals(HASH_LENGTH_ZERO)) {
                    break;
                }
                // if not the same hash, continue
                if (status != STATUS_OK || !entryHash.equals(hash) || type != isMetaDataByte) {
                    continue;
                }

                // if here, we have the same hash. return the bytes
                // buffer the bytes -- don't share since the server is multi-threaded
                byte[] content = new byte[chunkSize];
                // seek to the right spot
                ras.seek(entryOffset);
                // load the buffer
                fillWithBytes(content, ras, rasFile.getAbsolutePath(), "Reading in " + (isMetaData ? "meta data" : "data") + " chunk to return.");

                // return the data
                return content;
            }

            // if here, the file doesn't exist
            throw new FileNotFoundException("Bytes don't exist on this server for data block file; read total of " + entryNumber + " entries before giving up:" + rasFile.getAbsolutePath() + " [" + hash + "]");
        } finally {
            ras.close();
        }
    }

    /**
     * <p>Check whether the bytes (chunk) represented by the hash exist in this DataBlock instance.</p>
     * @param hash
     * @param isMetaData
     * @return
     * @throws java.lang.Exception
     */
    public synchronized final boolean hasBytes(BigHash hash, boolean isMetaData) throws Exception {
        // if is a directory and no merge file, pass down the chain
        if (isDirectory() && !isMerging()) {
            return ddc.dbu.getDataBlockToAddChunk(hash).hasBytes(hash, isMetaData);
        }
        // read from the file
        File rasFile = getRegularOrMergeFile();
        // if it doesn't exist, return false
        if (!rasFile.exists()) {
            return false;
        }

//        // read the header of the file to see what is in it
//        // each header is hash + (byte) type + (byte) status + int (offset in block) + int (size)
//        int bytesPerEntry = (BigHash.HASH_LENGTH + 1 + 1 + 4 + 4);
//        int bytesToRead = bytesPerEntry * HEADERS_PER_FILE;
        // buffer that amount
        byte[] buf = new byte[bytesToRead];

        // convert the boolean to meta-data or data bit
        final byte isMetaDataByte = isMetaData ? META_DATA : DATA;

        RandomAccessFile ras = new RandomAccessFile(rasFile, "r");

        try {
            // get the complete header
            fillWithBytes(buf, ras, rasFile.getAbsolutePath(), "Reading in header while checking if has " + (isMetaData ? "meta data" : "data") + " chunk.");
        } finally {
            ras.close();
        }

        // check for the hash
        for (int i = 0; i < HEADERS_PER_FILE; i++) {
            // calc the offset
            int offset = i * bytesPerEntry;
            // parse the entry parts: hash, type, status, offset, size
            BigHash h = BigHash.createFromBytes(buf, offset);
            byte type = buf[offset + BigHash.HASH_LENGTH];
            byte status = buf[offset + BigHash.HASH_LENGTH + 1];
            int o = buf[BigHash.HASH_LENGTH + offset + 2] << 24 | (buf[BigHash.HASH_LENGTH + offset + 2 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 2 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 2 + 3] & 0xff);
            int s = buf[BigHash.HASH_LENGTH + offset + 6] << 24 | (buf[BigHash.HASH_LENGTH + offset + 6 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 6 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 6 + 3] & 0xff);

            // break out of the loop if the entry size is zero
            if (s == 0 && o == 0) {
                break;
            }
            // if not the same hash, continue
            if (status != STATUS_OK || !h.equals(hash) || type != isMetaDataByte) {
                continue;
            }

            // Found bytes, create cache entry to speed up future short-term operations
            try {
                if (this.dbu.isUseCache()) {
                    DataBlockCacheEntry e = DataBlockCacheEntry.create(hash, DataBlock.this, o, s);
                    this.dbu.getDataBlockCache().add(e, isMetaData);
                }
            } catch (Exception e) {
//                System.err.println(e.getClass().getSimpleName() + " occurred while creating cache entry: " + e.getMessage());
            }

            // return the data
            return true;
        }

        // if here, the file doesn't exist
        return false;
    }

    /**
     * <p>Add a chunk to this DataBlock.</p>
     * @param hash
     * @param isMetaData
     * @param bytes
     * @throws java.lang.Exception
     */
    public synchronized final void addBytes(BigHash hash, boolean isMetaData, byte[] bytes) throws Exception {
        addBytes(hash, isMetaData, bytes, 1);
    }

    /**
     * <p>Add a chunk to this DataBlock.</p>
     * @param hash
     * @param isMetaData
     * @param bytes
     * @param recursionCount The number of times this has been recursively called while waiting for DataBlock to merge. (After a certain number of times, this will fail and throw an exception.)
     * @throws java.lang.Exception
     */
    public synchronized final void addBytes(BigHash hash, boolean isMetaData, byte[] bytes, int recursionCount) throws Exception {

        // read from the file
        final String blockPath = ddc.getDirectoryFile().getAbsolutePath() + filename;

        // Need a stopping point to prevent waiting far too long
        if (recursionCount >= 100 && isMerging()) {
            throw new Exception("Cannot add bytes; still merging, tried " + recursionCount + " times for " + blockPath);
        }

        // if we're merging, pass to the next block -- merge started after sync wait
        if (isMerging()) {
            Thread.sleep(50);
            ddc.dbu.getDataBlockToAddChunk(hash).addBytes(hash, isMetaData, bytes, recursionCount + 1);
            return;
        }

        // buffer the entire data block header
        byte[] buf = new byte[bytesToRead];

        // lazy load the file
        lazyCreateFile(buf);

        // track the bytes used and bytes wasted
        long bytesUsed = 0;
        long bytesWasted = 0;

        // convert the boolean to meta-data or data bit
        final byte isMetaDataByte = isMetaData ? META_DATA : DATA;

        // track the last valid offset. start at the end of the header
        int nextValidOffset = bytesToRead;

        RandomAccessFile ras = new RandomAccessFile(blockPath, "rw");
        try {
            // get the complete header
            fillWithBytes(buf, ras, blockPath, "Reading in header for data block to add " + (isMetaData ? "meta data" : "data") + " chunk.");

            // Used for troubleshooting
            int totalEntriesRead = 0;

            // check for the hash
            for (int i = 0; i < HEADERS_PER_FILE; i++) {

                totalEntriesRead++;

                // calc the offset
                int offset = i * bytesPerEntry;
                // parse the entry parts: hash, type, status, offset, size
                BigHash h = BigHash.createFromBytes(buf, offset);
                byte type = buf[offset + BigHash.HASH_LENGTH];
                byte status = buf[offset + BigHash.HASH_LENGTH + 1];

                /**
                 * - Note that offset (o) and size (s) are 32-bit integers, not 64-bit longs.
                 * - Note also that the last three bytes are ANDed with 0xff so that they are not signed (otherwise, potentially negative). See: http://www.velocityreviews.com/forums/t137952-bitmask-amp-graphics-question.html
                 */
                int o = buf[BigHash.HASH_LENGTH + offset + 2] << 24 | (buf[BigHash.HASH_LENGTH + offset + 2 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 2 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 2 + 3] & 0xff);
                int s = buf[BigHash.HASH_LENGTH + offset + 6] << 24 | (buf[BigHash.HASH_LENGTH + offset + 6 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 6 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 6 + 3] & 0xff);

                // if the same hash, handle specially
                if (h.equals(hash)) {
                    // Flag it as deleted. Splits will ignore
                    if (type == isMetaDataByte) {
                        // seek to the flag's position
                        ras.seek(i * bytesPerEntry + BigHash.HASH_LENGTH + 1);
                        // write the new byte
                        ras.write(STATUS_DELETED);

                        // flag as wasted space
                        status = STATUS_DELETED;
                    }
                }

                // increment the used space if the status is OK
                if (status == STATUS_OK) {
                    bytesUsed += s;
                } // if the status isn't OK, flag as wasted space'
                else {
                    bytesWasted += s;
                }

                // keep going until a free node is found
                if (o != 0) {
                    // update the offset for writing
                    nextValidOffset = o + s;
                    continue;
                }

                // if here, the entry is ready for being used
                // seek to the right spot to write the bytes
                ras.seek(nextValidOffset);
                // write the data
                ras.write(bytes);

                // write the header info last in case the operation is interrupted
                // buffer everything so that there is only one write operation
                byte[] headerBuf = new byte[bytesPerEntry];
                // copy over the big hash
                System.arraycopy(hash.toByteArray(), 0, headerBuf, 0, BigHash.HASH_LENGTH);
                // copy over the type
                headerBuf[BigHash.HASH_LENGTH] = isMetaDataByte;
                // copy over the status
                headerBuf[BigHash.HASH_LENGTH + 1] = STATUS_OK;
                // copy over the offset
                headerBuf[BigHash.HASH_LENGTH + 2] = (byte) (nextValidOffset >> 24);
                headerBuf[BigHash.HASH_LENGTH + 2 + 1] = (byte) (nextValidOffset >> 16);
                headerBuf[BigHash.HASH_LENGTH + 2 + 2] = (byte) (nextValidOffset >> 8);
                headerBuf[BigHash.HASH_LENGTH + 2 + 3] = (byte) (nextValidOffset);
                // copy over the size
                headerBuf[BigHash.HASH_LENGTH + 6] = (byte) (bytes.length >> 24);
                headerBuf[BigHash.HASH_LENGTH + 6 + 1] = (byte) (bytes.length >> 16);
                headerBuf[BigHash.HASH_LENGTH + 6 + 2] = (byte) (bytes.length >> 8);
                headerBuf[BigHash.HASH_LENGTH + 6 + 3] = (byte) (bytes.length);
                // do the final write of the header information for this entry
                ras.seek(offset);
                ras.write(headerBuf);

                // adjust used disk space
                ddc.adjustUsedSpace(bytes.length);

                // adjust bytes used for this block
                bytesUsed += bytes.length;

                // if the data block still below the size limit and the number of files limit, return
                // also force a resize if too much data is wasted
                boolean tooManyBytes = ras.length() > DataBlock.MAX_BLOCK_SIZE;
                boolean tooManyHeaders = i >= DataBlock.HEADERS_PER_FILE - 1;
                boolean tooMuchWastedSpace = bytesWasted > MAX_WASTED_SPACE_ALLOWED;
                if (!tooMuchWastedSpace && !tooManyBytes && !tooManyHeaders) {
                    return;
                }

                // If not count wasted space, still too many bytes?
                boolean tooManyBytesAdjusted = (ras.length() - bytesWasted) > DataBlock.MAX_BLOCK_SIZE;

                // flag for if the block should create sub-blocks are be cleaned up and kept as a single block
                boolean dontSplitBlock = tooMuchWastedSpace && !tooManyHeaders && !tooManyBytesAdjusted;

                // helper method to clean up the data block
                cleanUpDataBlock(dontSplitBlock);

                // break out
                return;
            } // For every header in DataBlock

            // If here, the file doesn't exist. Try to split the block. If already doing, will
            // safely return.
            //
            // Shouldn't get here, since should be cleaned up, but has!
            try {
                cleanUpDataBlock(false);
            } catch (Exception ex) {
                System.err.println(ex.getClass().getSimpleName() + " while cleaning up data block (recursionCount=" + recursionCount + "): " + ex.getMessage());
                ex.printStackTrace(System.err);
            }

            // Try up to three times, then move on: too long, don't want hold up client connection
            if (recursionCount <= 3) {
                ddc.dbu.getDataBlockToAddChunk(hash).addBytes(hash, isMetaData, bytes, recursionCount + 1);
            } else {
                throw new Exception("Can't write bytes to this block. Block is full! and recursionCount is " + recursionCount + ": " + blockPath + " <total entries read: " + totalEntriesRead + ", size of file: " + ras.length() + ">");
            }
        } finally {
            ras.close();
        }
    }

    /**
     * <p>Lazily create the underlying file with enough space for the header.</p>
     * @param buf
     * @throws java.lang.Exception
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    private void lazyCreateFile(final byte[] buf) throws Exception, FileNotFoundException, IOException {
        // make/check the file
        File file = new File(ddc.getDirectoryFile().getAbsolutePath() + filename);
        if (!file.exists()) {
            // check that the parent directory exists
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                boolean madeDirs = parent.mkdirs();
                if (!madeDirs) {
                    throw new Exception("Can't make required parent directories: " + file.getAbsolutePath());
                }
            }
            // make the file
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(buf);
            } finally {
                IOUtil.safeClose(fos);
            }

            // increment the ddc size
            ddc.adjustUsedSpace(buf.length);
        }
    }

    /**
     * <p>Helper method to clean up a data block. Relies on a variable that signifies if the block should be split or not. Either way the block might be moved to a different disk.</p>
     * @param dontSplitBlock
     * @throws java.lang.Exception
     */
    final synchronized void cleanUpDataBlock(boolean dontSplitBlock) throws Exception {
        // get a reference to the file that should be renamed
        File normalFile = new File(ddc.getDirectoryFile().getAbsolutePath() + filename);
        // if the file doesn't exist anymore, skip
        if (!normalFile.exists()) {
            return;
        }

        // buffer the entire data block header
        byte[] buf = new byte[bytesToRead];

        // lazy load the file in case it doesn't exist.
        lazyCreateFile(buf);

        // conditionally make a directory based on if the bytes to keep is more than the block size after accounting for wasted space
        if (!dontSplitBlock) {
            // rename the file
            File backupFile = new File(ddc.getDirectoryFile().getAbsolutePath() + filename + ".merge");
            // try to rename the file
            boolean renamedFile = normalFile.renameTo(backupFile);
            // if we can't rename, fall back on the old file
            if (!renamedFile) {
                throw new Exception("Can't expand data block! Reverting to old block. Existing files " + renamedFile + " exists: " + backupFile.exists() + "; " + normalFile + " exists: " + normalFile.exists());
            }
            // if in test case, purposely fail at this point
            if (ddc.dbu.purposelyFailCleanUp) {
                throw new Exception("Purposely failed for testing post .backup file creation.");
            }
            // make the dir
            boolean madeDirectory = normalFile.mkdir();
            if (!madeDirectory) {
                try {
                    IOUtil.renameFallbackCopy(backupFile, normalFile);
                } finally {
                    throw new Exception("Can't expand data block! Reverting to old block.");
                }
            }

            // add to the queue, don't wait for it to finish
            ddc.dbu.mergeQueue.put(new DataBlockToMerge(backupFile, ddc));
        } /**
         * "otherwise, add to the slow queue of merge" --Jayson
         * "This condition occurs if just cleaning up wasted space, and have not reached the maximum
         *  number of headers yet" --Bryan
         */
        else {
            // rename the file
            File backupFile = new File(ddc.getDirectoryFile().getAbsolutePath() + filename + ".backup");
            // try to rename the file
            boolean renamedFile = normalFile.renameTo(backupFile);
            // if we can't rename, fall back on the old file
            if (!renamedFile) {
                throw new Exception("Can't expand data block! Reverting to old block. Existing files " + renamedFile + " exists: " + backupFile.exists() + "; " + normalFile + " exists: " + normalFile.exists());
            }
            // if in test case, purposely fail at this point
            if (ddc.dbu.purposelyFailCleanUp) {
                throw new Exception("Purposely failed for testing post .backup file creation.");
            }

            // the size to decrement
            long sizeToDecrement = backupFile.length();
            // merge the data back in to the b-tree
            ddc.dbu.mergeOldDataBlock(backupFile, buf);
            // decrement the bytes used
            ddc.adjustUsedSpace(-sizeToDecrement);
        }
    }

    /**
     * <p>Delete the bytes (chunk) from this DataBlock based on hash.</p>
     * @param hash
     * @param isMetaData
     * @throws java.lang.Exception
     */
    public synchronized final void deleteBytes(BigHash hash, boolean isMetaData) throws Exception {
        // if is a directory and no merge file, pass down the chain
        if (isDirectory() && !isMerging()) {
            ddc.dbu.getDataBlockToAddChunk(hash).deleteBytes(hash, isMetaData);
            return;
        }

        // check for the file
        File rasFile = getRegularOrMergeFile();
        if (!rasFile.exists()) {
            return;
        }

//        // read the header of the file to see what is in it
//        // each header is hash + (byte) type + (byte) status + int (offset in block) + int (size)
//        int bytesPerEntry = (BigHash.HASH_LENGTH + 1 + 1 + 4 + 4);
//        int bytesToRead = bytesPerEntry * HEADERS_PER_FILE;
        // buffer that amount
        byte[] buf = new byte[bytesToRead];

        // make sure that the file exists
        lazyCreateFile(buf);

        // convert the boolean to meta-data or data bit
        final byte isMetaDataByte = isMetaData ? META_DATA : DATA;

        // track the last valid offset. start at the end of the header
        int nextValidOffset = bytesToRead;
        // read from the file
        RandomAccessFile ras = new RandomAccessFile(rasFile, "rw");
        try {
            // get the complete header
            fillWithBytes(buf, ras, rasFile.getAbsolutePath(), "Reading in headers for data block to delete a " + (isMetaData ? "meta data" : "data") + " chunk.");
            // check for the hash
            for (int i = 0; i < HEADERS_PER_FILE; i++) {
                // calc the offset
                int offset = i * bytesPerEntry;
                // parse the entry parts: hash, type, status, offset, size
                BigHash h = BigHash.createFromBytes(buf, offset);
                byte type = buf[offset + BigHash.HASH_LENGTH];
                byte status = buf[offset + BigHash.HASH_LENGTH + 1];
                int o = buf[BigHash.HASH_LENGTH + offset + 2] << 24 | (buf[BigHash.HASH_LENGTH + offset + 2 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 2 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 2 + 3] & 0xff);
                int s = buf[BigHash.HASH_LENGTH + offset + 6] << 24 | (buf[BigHash.HASH_LENGTH + offset + 6 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 6 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 6 + 3] & 0xff);

                // if the same hash, handle specially
                if (h.equals(hash) && type == isMetaDataByte && status != STATUS_DELETED) {
                    // seek to the flag's position
                    ras.seek(i * bytesPerEntry + BigHash.HASH_LENGTH + 1);
                    // write the new byte
                    ras.write(STATUS_DELETED);
                    // return
                    return;
                }

                // keep going until a free node is found
                if (o == 0) {
                    break;
                }
            }

            // if here, the file doesn't exist -- don't throw an exception
        } finally {
            ras.close();
        }
    }

    /**
     * <p>Helper method to ensure that the RAS reads all of the bytes desired.</p>
     * @param buf A byte buffer to hold the data. The random access file's data will be transfered to filled this buffer.
     * @param ras Random access file for the data block.
     * @param blockFilePath The path to the file accessed by the RandomAccessFile, ras. Used for error messages.
     * @throws java.lang.Exception If any I/O errors occur, or if cannot fill the entire buffer with data from RandomAccessFile (could be corrupted data block).
     */
    static final void fillWithBytes(final byte[] buf, final RandomAccessFile ras, String blockFilePath, String description) throws Exception {
        int bytesRead = 0;
        while (bytesRead != buf.length) {
            int read = ras.read(buf, bytesRead, buf.length - bytesRead);
            // check for EOF
            if (read == -1) {
                throw new UnexpectedEndOfDataBlockException("EOF reached and expected more bytes! For data block at: " + blockFilePath + " <" + description + ">");
            }
            bytesRead += read;
        }
    }

    /**
     * <p>Compare underlying files for DataBlock instances.</p>
     * @param o
     * @return
     */
    public final int compareTo(Object o) {
        DataBlock db = (DataBlock) o;
        return filename.compareTo(db.filename);
    }

    /**
     * <p>Return the subblocks for this DataBlock.</p>
     * @return
     */
    protected final DataBlock[] getSubBlocks() {
        // Lazy-load. This will avoid an OutOfMemoryError since won't access
        // subBlocks until needed!
        synchronized (DataBlock.this) {
            if (subBlocks == null) {
                subBlocks = new DataBlock[256];
            }
        }
        return subBlocks;
    }

    /**
     * <p>The data block moves itself. This synchronized method places the data block in a new DataDirectoryConfiguration.</p>
     * <p>The intended use for this method is to balance a server's data across data directories.</p>
     * @param newDDC
     * @return
     * @throws java.lang.Exception
     */
    protected final synchronized boolean moveToDataDirectoryConfiguration(DataDirectoryConfiguration newDDC) throws Exception {

        // Immediately bail if same DDC already at
        if (newDDC.equals(this.ddc)) {
            return false;
        }

        boolean moved = false;

        final File srcFile = getRegularOrMergeFile();
        final long srcBytes = srcFile.length();

        // Don't recursively copy. Can be expensive across filesystems.
        if (this.isDirectory()) {
            throw new AssertionFailedException("Trying to move data block, is a directory (not allowed): " + srcFile.getAbsolutePath());
        }

        try {

            if (!srcFile.exists()) {
                throw new AssertionFailedException("Trying to move data block, doesn't exist: " + srcFile.getAbsolutePath());
            }

            // If new data directory doesn't exist, try to create. If cannot, throw exception.
            if (!newDDC.getDirectoryFile().exists() || !newDDC.getDirectoryFile().isDirectory()) {
                newDDC.getDirectoryFile().mkdirs();
                if (!newDDC.getDirectoryFile().exists()) {
                    throw new Exception("Trying to move data block from source<" + srcFile.getAbsolutePath() + "> to destination DDC, but cannot create DDC data directory: " + newDDC.getDirectoryFile().getAbsolutePath());
                }
                if (!newDDC.getDirectoryFile().isDirectory()) {
                    throw new Exception("Trying to move data block from source<" + srcFile.getAbsolutePath() + "> to destination DDC, but cannot use DDC because it is not a directory: " + newDDC.getDirectoryFile().getAbsolutePath());
                }
            }

            // May not move if DDC is at its limit
            if (newDDC.getActualSize() >= newDDC.getSizeLimit()) {
                return false;
            }

            // Create destination file. If this is a merge file, end with .merge
            final String regularOrMergeFileName = this.filename + (srcFile.getName().endsWith(".merge") ? ".merge" : "");
            final File destFile = new File(newDDC.getDirectory(), regularOrMergeFileName);

            if (destFile.exists()) {
                throw new AssertionFailedException("Want to move data block <" + srcFile.getAbsolutePath() + "> to new destination<" + destFile.getAbsolutePath() + ">, but destination exists.");
            }

            // Make sure parent file exists for data block
            destFile.getParentFile().mkdirs();
            moved = srcFile.renameTo(destFile);

            // Note that might be problematic across data directories on different
            // filesystems, so attempt to copy if rename fails.
            if (!moved && !destFile.exists()) {
                IOUtil.copyFile(srcFile, destFile);
                final long destBytes = destFile.length();

                if (destBytes == srcBytes) {
                    IOUtil.safeDelete(srcFile);

                    if (srcFile.exists()) {
                        // Don't delete either: might be in an odd state
                        throw new Exception("Could not delete source data block<" + srcFile.getAbsolutePath() + "> file after moving to destination<" + destFile.getAbsolutePath() + ">.");
                    }

                    moved = true;
                } else {
                    throw new Exception("After trying to move data block, size<" + srcBytes + "> or src data directory<" + srcFile.getAbsolutePath() + "> doesn't match size<" + destBytes + "> of destination directory<" + destFile.getAbsolutePath() + ">.");
                }
            }

            return moved;
        } finally {
            if (moved) {
                // Move bytes count from old DDC to new DDC
                this.ddc.adjustUsedSpace(-srcBytes);
                newDDC.adjustUsedSpace(+srcBytes);

                // Replace DDC refrenece
                this.ddc = newDDC;
            }
        }
    }
}
