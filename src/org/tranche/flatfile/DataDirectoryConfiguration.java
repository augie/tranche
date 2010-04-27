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
import org.tranche.commons.TextUtil;
import org.tranche.time.TimeUtil;

/**
 * <p>A simple cap on the directories to use and the amount of data to put in them. Size is noted in bytes.</p>
 * @author Jayson
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class DataDirectoryConfiguration implements Comparable {

    // Set to true to turn on debugging
    private static final boolean isDebug = false;    // the directory
    private String directory;
    private long sizeLimit;
    /**
     * <p>Represents the maximum size of a data directory.</p>
     * <p>Note this value is ignored, as the maximum size is whether the administrator sets in the DataDirectoryConfiguration.</p>
     * @deprecated 
     */
    public static final long MAX_SIZE = -1;    // Amount of actual data already in directory. Regardless of min block sizes on filesystem.
    private long actualSize = 0;    // the parent DataBlockUtil
    /**
     * <p>A reference to the DataBlockUtil for getting information about data available to the server.</p>
     */
    DataBlockUtil dbu = null;

    /**
     * 
     * @param directory
     * @param sizeLimit
     */
    public DataDirectoryConfiguration(final String directory, long sizeLimit) {
        setDirectory(directory);
        setSizeLimit(sizeLimit);
    }

    /**
     * <p>Get the path to the directory represented by this DataDirectoryConfiguration object.</p>
     * @return
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * <p>Get the file for the directory represented by this DataDirectoryConfiguration object.</p>
     * @return
     */
    public File getDirectoryFile() {
        return new File(directory);
    }

    /**
     * <p>Set the directory represented by this DataDirectoryConfiguration object.</p>
     * @param directory
     */
    public void setDirectory(String directory) {
        
        // Fixes bug. Don't let directory end with file separator.
        while (directory.endsWith("/") || directory.endsWith("\\")) {
            directory = directory.substring(0, directory.length()-1);
        }
        
        // if a new directory if set, reset state info
        if (!directory.equals(this.directory)) {
            this.directory = directory;
            this.actualSize = 0;
        }
    }

    /**
     * <p>Get the administrator-defined size limit for the DataDirectoryConfiguration.</p>
     * @return
     */
    public long getSizeLimit() {
        return sizeLimit;
    }

    /**
     * <p>Set the size limit for the DataDirectoryConfiguration.</p>
     * @param sizeLimit
     */
    public void setSizeLimit(long sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Override()
    public boolean equals(Object obj) {

        if (obj instanceof DataDirectoryConfiguration) {
            DataDirectoryConfiguration ddc = (DataDirectoryConfiguration) obj;
            // return if the directories are the same
            return directory.equals(ddc.getDirectory());
        }

        return obj.equals(DataDirectoryConfiguration.this);
    }

    @Override()
    public int hashCode() {
        return directory.hashCode();
    }

    public int compareTo(Object o) {
        DataDirectoryConfiguration ddc = (DataDirectoryConfiguration) o;
        return this.getDirectory().compareTo(ddc.getDirectory());
    }
    /**
     * <p>If true, actual bytes used has overflowed.</p>
     */
    private boolean overflowBeenDetected = false;

    /**
     * <p>Adjust disk space used both actual bytes and estimated real disk space based on the filesystem's minimum block size.</p>
     * @param adjustment Positive or negative value. E.g., if a KB was deleted, adjustment would be -1024
     */
    public synchronized void adjustUsedSpace(long adjustment) {
        final long prevSize = this.actualSize;
        // inc up the actually used bytes
        this.actualSize += adjustment;
        printTracer("Adjusted space from " + prevSize + " -> " + this.actualSize + " (limit=" + this.sizeLimit + ") for " + this.directory);

        // Detect overflow. Requires positive number.
        if (!isOverflowBeenDetected() && adjustment > 0) {

            // Should not be less than previous size if adjustment is positive
            // unless overflowed.
            if (this.actualSize < prevSize) {
                System.err.println();
                System.err.println("///////////////////////////////////////////////////////////////////////////////");
                System.err.println("///// DETECTED OVERFLOW FOR DDC: " + this.getDirectory());
                System.err.println("  At: " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
                System.err.println();
                System.err.println("  Prev actual used bytes: " + prevSize);
                System.err.println("  Curr actual used bytes: " + this.actualSize);
                System.err.println("  Adjustment size:        " + adjustment);
                System.err.println();
                System.err.println("  Directory limit:        " + this.getSizeLimit());
                System.err.println();
                System.err.println("///////////////////////////////////////////////////////////////////////////////");
                System.err.println();

                overflowBeenDetected = true;
            }
        }
    }

    /**
     * <p>Returns true if there is enough space to store file.</p>
     * @param size The size of the file being added.
     */
    public boolean canStoreData(long size) {
        return this.sizeLimit >= size + getActualSize();
    }

    /**
     * <p>Get the size, in bytes, of actual data stored in this directory.</p>
     * @return
     */
    public long getActualSize() {
        return actualSize;
    }

    /**
     * <p>Prints out tracers if debug is set to true.</p>
     * @param msg
     */
    private void printTracer(String msg) {
        if (isDebug) {
            System.out.println("DATA_DIRECTORY_CONFIGURATION> " + msg);
        }
    }

    @Override()
    public DataDirectoryConfiguration clone() {
        DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(this.getDirectory(), this.getSizeLimit());
        ddc.actualSize = this.getActualSize();
        ddc.dbu = this.dbu;
        return ddc;
    }

    /**
     * <p>True if the variable keeping track of the actual bytes used has overflowed.</p>
     * @return
     */
    public boolean isOverflowBeenDetected() {
        return overflowBeenDetected;
    }
}
