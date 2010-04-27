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

import org.tranche.commons.RandomUtil;
import org.tranche.util.*;
import java.io.*;
import java.util.*;

/**
 * <p>A list that stores BigHash objects. This is a memory-friendly method that
 * commits portions of list to disk.</p>
 * 
 * <p>Every certain number of BigHash records, records
 * are committed to disk, and the number of BigHash entries in memory are always less
 * than the number of records stored in each partition file.</p>
 * 
 * <p>You can find out number of records stored per partition by calling static
 * method DiskBackedBigHashList.RECORDS_PER_PARTITION().</p>
 * 
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class DiskBackedBigHashList extends AbstractList<BigHash> {

    static String SLASH = File.separator;
    static String NL = "\n";
    // Incremented as hashes are added, decremented as hashes are removed.
    // Could be calculated by iterating partitions, but this offers a speedup.
    private int size = 0;
    // Very important... the id for the cache. Used so multiple DiskBackedHashLists
    // can exist.
    // TODO Detect collisions
    private long id;
    // Where information is stored. Each partitions has a fixed size, and are
    // stored as separate files on disk.
    private File TMP_DIR;
    private List<Partition> diskPartitions;
    // Number of hash records to store on each partition
    // Tested: The larger, the faster. However, memory requirements also go up.
    private static int RECORDS_PER_PARTITION = 5000;
    // Hashes stored in memory. If exceeds limit, committed to disk as partition.
    private List<BigHash> memory;
    private boolean isDestroyed = false;

    public DiskBackedBigHashList() throws IOException {
        // Assign a random id
        this.id = RandomUtil.getLong();

        diskPartitions = new ArrayList();
        memory = new ArrayList();
        //TMP_DIR                  = TempFileUtil.createTemporaryFile("cache-" + this.id);
        TMP_DIR = TempFileUtil.createTemporaryDirectory("cache");
        diskPartitions = new ArrayList();

        if (!TMP_DIR.mkdirs() && !TMP_DIR.exists()) {
            throw new IOException("Can't create temporary directory " + TMP_DIR.getPath());
        }
    }

    /**
     * When done with list, destroy records on disk.
     */
    @Override()
    protected void finalize() throws Throwable {
        destroy();
    }
    
    /**
     * 
     */
    public void close() {
        if (isDestroyed) {
            return;
        }
        isDestroyed = true;

        // Close off random access files
        for (Partition part : this.diskPartitions) {

            try {
                part.partitionFile.delete();
            //part.raccess.close();
            } catch (Exception e) {
                System.err.println("Problem shutting down DiskBackedHashList: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }

        // Shouldn't delete perchance other DiskBackedHashLists in use
        IOUtil.recursiveDeleteWithWarning(this.TMP_DIR);
    }

    /**
     * Explicity destroy the disk-backed collection. If don't, GC will garbage collect OR temp dir cleaned out when tool reran.
     * @deprecated Use close
     */
    public void destroy() {

        close();
    }

    /**
     * Returns number of BigHash entries stored in each disk partition.
     * @return int Number of BigHash entries per partition.
     */
    public static int recordsPerPartition() {
        return getRecordsPerPartition();
    }

    /**
     * Returns directory that holds partition files
     * @return File The directory holding partition files.
     */
    public File directory() {
        return this.TMP_DIR;
    }

    /**
     * Returns size of list.
     * @return int Size of list.
     */
    public int size() {
        return size;
    }

    /**
     * Returns number of records in memory. (The rest are on disk.)
     * @return int Number of BigHash records in memory
     */
    public int recordsInMemory() {
        return this.memory.size();
    }

    /**
     * Returns number of records in partitions on disk. (The rest are in memory.)
     * @return int Number of BigHash records on disk (in partitions)
     */
    public int recordsOnDisk() {

        return this.size - this.recordsInMemory();
    }

    /**
     * Returns number of partitions on disk.
     * @return int Number of partitions on disk.
     */
    public int paritionsOnDisk() {
        return this.diskPartitions.size();
    }

    /**
     * Returns true if this contains a given Hash
     * @param hash BigHash
     * @return true if parameter appears in list.
     */
    public synchronized boolean contains(BigHash hash) {

        // Try to find a match somewhere in collection.
        for (int i = 0; i < this.size(); i++) {
            if (this.get(i).equals(hash)) {
                return true;
            }
        }

        // No match was found
        return false;
    }

    /**
     * Append hash to list.
     * @param hash A BigHash to add to list.
     */
    @Override()
    public synchronized boolean add(BigHash hash) {

        boolean added = false;

        // Increment size
        this.size++;

        // If memory is at limit, time to commit
        if (memory.size() >= getRecordsPerPartition()) {

            // How many partitions are already in the system? That's the new
            // partition's index.
            int newIndex = this.diskPartitions.size();

            // Create new parition
            this.diskPartitions.add(new Partition(newIndex));

            // Commit memory to partition
            this.diskPartitions.get(newIndex).commit(memory);

            // Clear memory
            memory = new ArrayList();

            // Add new hash to memory
            memory.add(hash);
        } // Doesn't exceed limit, so append to memory
        else {
            memory.add(hash);
        }


        return added;
    }

    /**
     * Get BigHash at specified index.
     * @param index Index of BigHash.
     * @return BigHash The BigHash.
     */
    public synchronized BigHash get(int index) {

        // Is record on disk or in memory?
        boolean onDisk = recordOnDisk(index);

        // It's on the disk
        if (onDisk) {

            // Which partition is it on?
            int partition = index / getRecordsPerPartition();
            int relativeIndex = index % getRecordsPerPartition();

            // Get hash from partition
            return this.diskPartitions.get(partition).get(relativeIndex);
        }

        // It's in memory
        int relativeIndex = index - this.recordsOnDisk();
        return this.memory.get(relativeIndex);
    }

    /**
     * Returns true if on disk, else returns false. Throws a RuntimeException if index
     * out of bounds.
     *
     * @param index Index of hash
     * @return boolean True if on disk, false if in memory.
     */
    public boolean recordOnDisk(int index) {

        // If index out of bounds...
        if (index >= this.size) {
            throw new RuntimeException("Attempting to get " + index + " when there are currently " + this.size + " entries.");
        }

        boolean onDisk = false;

        // Only way can be on disk if the number of BigHash elements exceeds what is stored in memory
        if (this.size > this.getRecordsPerPartition()) {

            // Is on disk if index is less than index of last element on disk.
            if (this.recordsOnDisk() > index) {
                onDisk = true;
            }
        }

        return onDisk;
    }

    /**
     * CURRENTLY UNIMPLEMENTED
     * @param index Index of BigHash.
     * @return BigHash The BigHash.
     */
    @Override()
    public BigHash remove(int index) {
        throw new UnsupportedOperationException("remove() not implemented.");
    }

    /**
     * <p>Returns an iterator for list.</p>
     * @return Iterator
     */
    @Override()
    public Iterator<BigHash> iterator() {

        return new DiskBackedHashListIterator(this);
    }

    /**
     * Represents a single file, which is a virutal partition.
     */
    private class Partition {

        int partitionNumber;      // nth partition, a new partition for each commit to disk
        //File partitionDir;
        File partitionFile;       // File holding records
        //RandomAccessFile raccess; // A random access wrapper for byte file
        // Number of bytes of BigHash string
        // TODO Find dynamic method to do this. Can use Utils class, but in test
        // directory, and not sure its a good idea to reference anything in test.
        private static final int RECORD_SIZE = 104;
        StringBuffer sbuffer;

        public Partition(int partitionNumber) {

            this.partitionNumber = partitionNumber;
            this.partitionFile = new File(TMP_DIR.toString() + SLASH + partitionNumber + ".partition");

            /*
            try {
            this.raccess = new RandomAccessFile( this.partitionFile, "rw" ) ;
            }
            
            catch (FileNotFoundException e) {
            System.out.println("A FileNoteFoundException occurred with the following message: " + e.getMessage());
            }
             */

            this.sbuffer = new StringBuffer();
        }

        /**
         * Commit all contents of list to a partition.
         */
        public boolean commit(List<BigHash> memory) {

            boolean committed = true;

            // Add all hashes to string buffer
            for (BigHash nextHash : memory) {
                this.add(nextHash);
            }

            // Commit string buffer to disk
            RandomAccessFile raccess = null;
            try {
                raccess = new RandomAccessFile(this.partitionFile, "rw");
                raccess.write(this.sbuffer.toString().getBytes());
            } catch (IOException e) {
                committed = false;
                System.out.println("An IOException occurred with the following message: " + e.getMessage());
            } finally {
                try {
                    if (raccess != null) {
                        raccess.close();
                    }
                } catch (Exception ex) {
                    System.err.println("Problem shutting down RAS: " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }

            // Get rid of string buffer (memory)
            this.sbuffer = new StringBuffer();

            return committed;
        }

        /**
         * Append BigHash to end of partition. Returns false if it can't.
         */
        private void add(BigHash h) {

            // First entry doesn't need a new line character
            if (this.sbuffer.length() == 0) {
                this.sbuffer.append(h.toString());
            } // Every entry but first needs a new line
            else {
                this.sbuffer.append(h.toString());
            }

        }

        /**
         * Returns BigHash entry at specified index.
         */
        public BigHash get(int index) {

            BigHash record = null;

            RandomAccessFile raccess = null;
            // Grab record
            try {
                // Build random access file
                raccess = new RandomAccessFile(this.partitionFile, "rw");
                raccess.write(this.sbuffer.toString().getBytes());

                byte[] raw = new byte[this.RECORD_SIZE];

                raccess.seek(index * this.RECORD_SIZE);

                raccess.read(raw);

                String hash = new String(raw);

                record = BigHash.createHashFromString(hash);
            } catch (IOException e) {
                System.out.println("An IOException occurred with the following message: " + e.getMessage());
            } finally {
                try {
                    if (raccess != null) {
                        raccess.close();
                    }
                } catch (Exception ex) {
                    System.err.println("Problem shutting down RAS: " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }

            return record;
        }

        /**
         * Removes BigHash entry at specified index and returns the BigHash.
         */
        public BigHash remove(int index) {

            throw new UnsupportedOperationException("Partition.remove() not implemented.");
        }

        @Override()
        public String toString() {
            return "Record #" + this.partitionNumber + " for collection with id " + id;
        }
    }

    /**
     * Iterator class for DiskBackedBigHashList.
     */
    private class DiskBackedHashListIterator implements Iterator<BigHash> {

        private DiskBackedBigHashList dlist;
        private int position = 0;

        public DiskBackedHashListIterator(DiskBackedBigHashList dlist) {
            this.dlist = dlist;
        }

        public boolean hasNext() {
            return position < dlist.size();
        }

        public BigHash next() {
            return this.dlist.get(position++);

        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Retrieve number of records per partition.
     * @return
     */
    public static int getRecordsPerPartition() {
        return RECORDS_PER_PARTITION;
    }

    /**
     * Set number of records per partition.
     * @param aRECORDS_PER_PARTITION
     */
    public static void setRecordsPerPartition(int aRECORDS_PER_PARTITION) {
        RECORDS_PER_PARTITION = aRECORDS_PER_PARTITION;
    }
}