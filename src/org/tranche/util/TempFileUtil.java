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
package org.tranche.util;

import org.tranche.hash.BigHash;
import org.tranche.hash.Base16;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>This utility class creates a clean working temporary directory for storing files during runtime.
 * The directory is created only once at initialization, and it can be customized by overriding  Java's
 * java.io.tmpdir system property, e.g.:<p>
 *
 * <p>java -Djava.io.tempdir="/my/temp/directory" org.tranche.Main</p>
 *
 * <p>The temporary directory will not conflict with another temp dir. File locks are kept and checked when making
 *  the directory.</p>
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James A. Hill
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class TempFileUtil {

    // the Java spec defined property for the system's temp dir
    private static final String JAVA_TEMP_DIR = "java.io.tmpdir";
    // the Java spec defined property for the user's home directory
    private static final String JAVA_USER_HOME = "user.home";
    // default location that data is saved
    private static final String DEFAULT_TEMP_SUB_DIR = ".tranche-temp";
    // the max attempts to make when creating direcoties
    private static final int maxTempDirAttempts = 20;
    // to track temp files made
    private static int count = 0;
    // To track temp directories made!
    private static final Map<String, Integer> tmpDirCounts = new HashMap();
    // the file that the temp directory is in
    private static File tempDirectory;


    static {
        // load the temp directory attribute
        try {

            // check for temp dir variable
            if (System.getProperty(JAVA_TEMP_DIR) != null && !System.getProperty(JAVA_TEMP_DIR).equals("")) {

                // May change to not be a hidden directory for non-privileged Windows users
                String subdir = DEFAULT_TEMP_SUB_DIR;

                // try to lock on the 'trache-temp' directory
                if (!tryLock(new File(System.getProperty(JAVA_TEMP_DIR), subdir))) {
                    for (int i = 1; i < maxTempDirAttempts; i++) {
                        File fileToTry = new File(System.getProperty(JAVA_TEMP_DIR), subdir + "-" + i);
                        if (tryLock(fileToTry)) {
                            break;
                        }
                        // if on the last, throw exception
                        if (i == 19) {

                            // Try to use a non-hidden file.
                            // Should only happen once.
                            if (subdir.startsWith(".")) {
                                System.out.println("Cannot write to " + subdir);
                                subdir = subdir.substring(1);
                                System.out.println("Trying " + subdir);
                                i = 1;
                                continue;
                            }

                            throw new RuntimeException("Can't acquire a temporary directory lock! Tried 20 times.");
                        }
                    }
                }
            } // fall back on the default home directory for a file lock
            else {
                // May change to not be a hidden directory for non-privileged Windows users
                String subdir = DEFAULT_TEMP_SUB_DIR;

                if (!tryLock(new File(System.getProperty(JAVA_USER_HOME), subdir))) {
                    for (int i = 1; i < maxTempDirAttempts; i++) {
                        File fileToTry = new File(System.getProperty(JAVA_TEMP_DIR), subdir + "-" + i);
                        if (tryLock(fileToTry)) {
                            break;
                        }
                        // if on the last, throw exception
                        if (i == maxTempDirAttempts - 1) {

                            // Try to use a non-hidden file.
                            // Should only happen once.
                            if (subdir.startsWith(".")) {
                                System.out.println("Cannot write to " + subdir);
                                subdir = subdir.substring(1);
                                System.out.println("Trying " + subdir);
                                i = 1;
                                continue;
                            }

                            throw new RuntimeException("Can't acquire a temporary directory lock! Tried 20 times.");
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } finally {
//            System.out.println("Temporary directory: " + tempDirectory);
        }

    }

    /**
     * <p>Lock on a file to determine whether another JVM is using the temporary directory or file.</p>
     * @param lockToTry
     * @return
     */
    private static boolean tryLock(File lockToTry) {
        try {
            // make if the file doesn't exist
            if (!lockToTry.exists()) {
                if (!lockToTry.mkdirs()) {
                    return false;
                }
            }

            // try to get a lock
            File tempDir = new File(lockToTry, "lock.file");
            // make as a file...not a directory
            if (!tempDir.exists()) {
                if (!tempDir.createNewFile()) {
                    return false;
                }
            }
            FileChannel fc = new RandomAccessFile(tempDir, "rw").getChannel();
            FileLock lock = fc.tryLock();
            if (lock == null) {
                fc.close();
                return false;
            }
            // otherwise, set the references an return true
            tempDirectory = lockToTry;

            // clean up the old temp directory
            recursiveDeleteSkipLockFiles(tempDir);

            // before returning, purge any directories that already exist
            for (int i = 1; i < maxTempDirAttempts; i++) {
                File checkExistingDir = new File(lockToTry.getAbsolutePath() + "-" + i);
                if (checkExistingDir.exists()) {
                    // try to lock it
                    File purgeDir = new File(checkExistingDir, "lock.file");
                    FileChannel purgeChannel = new RandomAccessFile(purgeDir, "rw").getChannel();
                    try {
                        // try to get a lock on the file
                        FileLock purgeLock = purgeChannel.tryLock();
                        // if no lock, skip. Don't purge a directory in use
                        if (purgeLock == null) {
                            continue;
                        }
                        // release the lock
                        purgeLock.release();
                        // clean up the old temp directory
                        recursiveDeleteSkipLockFiles(purgeDir);
                    } finally {
                        purgeChannel.close();
                    }
                }
            }

            return true;
        } // fall back on false if any exceptions appear
        catch (Exception e) {
            return false;
        }
    }

    /**
     * <p>Helper method to clean up old directories.</p>
     * @param directoryToDelete
     */
    private static void recursiveDeleteSkipLockFiles(final File directoryToDelete) {
        // delete everything but the lock file
        ArrayList<File> filesToDelete = new ArrayList();
        filesToDelete.add(directoryToDelete);
        while (filesToDelete.size() > 0) {
            File fileToDelete = filesToDelete.remove(0);
            // skip the lock file
            if (fileToDelete.getName().equals("lock.file")) {
                continue;
            }
            // purge all files in directories
            if (fileToDelete.isDirectory()) {
                // check the size, remove empty
                File[] moreFilesToDelete = fileToDelete.listFiles();
                if (moreFilesToDelete.length == 0) {
                    if (!fileToDelete.delete()) {
                        System.out.println("Can't delete temp directory " + fileToDelete);
                    }
                } else {
                    // add each file individually
                    for (File moreFileToDelete : moreFilesToDelete) {
                        filesToDelete.add(moreFileToDelete);
                    }
                }
            } // purge files directly
            else {
                if (!fileToDelete.delete()) {
                    System.out.println("Can't delete temp file " + fileToDelete);
                }
            }
        }
    }

    /**
     * <p>Create a temporary file. File will end with .tmp file extension.</p>
     * @return
     */
    public synchronized static final File createTemporaryFile() {
        return createTemporaryFile(".tmp");
    }

    /**
     * <p>Create a temporary file with an arbitrary file extension.</p>
     * @param extension Any arbitrary file extension (e.g., .txt, .pf, etc.)
     * @return
     */
    public synchronized static final File createTemporaryFile(String extension) {
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }

        // Mark it as temp
        if (!extension.equals(".tmp")) {
            extension = ".tmp" + extension;
        }

        // make a file
        File f = new File(tempDirectory, "file" + count + extension);
        count++;
        if (count < 0) {
            count = 0;
        }
        while (f.exists() || !tryToMakeFile(f)) {
            f = new File(tempDirectory, "file" + count + extension);
            count++;
            if (count < 0) {
                count = 0;
            }
        }
        // return the temp file
        return f;
    }

    public synchronized static final File getTemporaryFile() {
        return getTemporaryFile(".tmp");
    }

    public synchronized static final File getTemporaryFile(String extension) {
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }

        // Mark it as temp
        if (!extension.equals(".tmp")) {
            extension = ".tmp" + extension;
        }

        // make a file
        File f = new File(tempDirectory, "file" + count + extension);
        count++;
        if (count < 0) {
            count = 0;
        }
        while (f.exists()) {
            f = new File(tempDirectory, "file" + count + extension);
            count++;
            if (count < 0) {
                count = 0;
            }
        }
        // return the temp file
        return f;
    }

    /**
     * <p>Create a temporary directory.</p>
     * @return
     */
    public static synchronized final File createTemporaryDirectory() {
        return createTemporaryDirectory("dir");
    }

    /**
     * <p>Create a temporary directory with a particular name.</p>
     * @param name
     * @return
     */
    public static synchronized final File createTemporaryDirectory(String name) {

        int nameCount = 0;
        Integer countObj = tmpDirCounts.get(name);
        if (countObj != null) {
            nameCount = countObj.intValue();
        }

        // make a file
        File f = new File(tempDirectory, name);
        if (f.exists()) {
            f = new File(tempDirectory, name + nameCount);
        }
        nameCount++;
        // stop rollovers
        if (nameCount < 0) {
            nameCount = 0;
        }
        while (f.exists() || !f.mkdirs()) {
            f = new File(tempDirectory, name + nameCount);
            nameCount++;
            // stop rollovers
            if (nameCount < 0) {
                nameCount = 0;
            }
        }

        // Update the count in the map
        tmpDirCounts.put(name, nameCount);

        // return the temp file
        return f;
    }

    /**
     * <p>Attempts to create a temporary file with a specific name. If file exists, will try to create in another location or throw an IOException so file isn't clobbered.</p>
     * @param filename Only the name of the file -- not the full path. Will be placed in the temp directory.
     * @return
     * @throws java.io.IOException
     */
    public static synchronized final File createTempFileWithName(String filename) throws IOException {
        File f = new File(tempDirectory, filename);

        int createCount = 0;
        while (f.exists() && createCount < 500) {
            File tmpSubdir = new File(tempDirectory, String.valueOf(createCount));

            // If doesn't exist, create the file
            if (!tmpSubdir.exists()) {
                tmpSubdir.mkdirs();
            }

            f = new File(tmpSubdir, filename);
            createCount++;
        }
        if (f.exists()) {
            throw new IOException("Temp file with name already exists: " + filename);
        } else if (!tryToMakeFile(f)) {
            throw new IOException("Cannot create file with name: " + filename);
        }

        return f;
    }

    /**
     * <p>Helper method to create file after creating parent directories.</p>
     * @param f
     * @return
     */
    private static boolean tryToMakeFile(File f) {
        try {
            // make the parent if needed
            File parent = f.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            // make the file
            return f.createNewFile();
        } catch (IOException ex) {
            System.err.println("Warning can't make temp file " + f);
        }
        return false;
    }

    /**
     * <p>A helper function to make files.</p>
     * <p>The file's name depends:</p>
     * <ul>
     *   <li>If file not exist in temp directory with hash, the name will be the hash</li>
     *   <li>If file exists in temp directory with hash, then the hash will be file extension so file name is unique</li>
     * </ul>
     * <p>You cannot reliably depend on file name for hash.</p>
     * @param fileHash
     * @return
     */
    public static synchronized final File createTemporaryFile(BigHash fileHash) {
        // make a file
        File f = new File(tempDirectory, Base16.encode(fileHash.toByteArray()));

        // If the file already exists, use the hash as a file extension instead of as name
        if (f.exists()) {
            return TempFileUtil.createTemporaryFile(Base16.encode(fileHash.toByteArray()));
        }

        // if it doesn't exist, make it
        if (!f.exists() || f.length() != fileHash.getLength()) {
            // if the file doesn't exist, make it
            if (!f.exists()) {
                // make the parent directory if needed
                File parentFile = f.getParentFile();
                if (parentFile != null) {
                    parentFile.mkdirs();
                }
                try {
                    // make the file
                    f.createNewFile();
                } catch (IOException ex) {
                    throw new RuntimeException("Can't make temp file " + f.getName(), ex);
                }
            }
            // make a random access file
            RandomAccessFile raf = null;
            try {
                // make the raf
                raf = new RandomAccessFile(f, "rw");
                // make the size
                raf.setLength(fileHash.getLength());
            } catch (IOException e) {
                throw new RuntimeException("Can't create file " + f, e);
            } finally {
                IOUtil.safeClose(raf);
            }
        }

        // return the temp file
        return f;
    }

    /**
     * <p>Set the temporary directory. Does some sanity checking.</p>
     * <p>Mainly used by non-GUI clients (i.e., command-line tools).</p>
     * @param directory
     * @throws java.io.IOException
     */
    public static void setTemporaryDirectory(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IOException(directory.getAbsolutePath() + " is not a directory. Cannot set as temp directory.");
        }
        if (!directory.canWrite() || !directory.canRead()) {
            throw new IOException(directory.getAbsolutePath() + " has constricting permissions (readable?=" + directory.canRead() + ", writeable?=" + directory.canWrite() + "). Cannot set as temp directory.");
        }
        tempDirectory = directory;
    }

    /**
     * <p>Returns the root temporary directory.</p>
     * <p>Note that the client should not attempt to create files from this directory directly, but instead should use methods in this class.</p>
     * @return
     */
    public static String getTemporaryDirectory() {
        return tempDirectory.getAbsolutePath();
    }

    /**
     * 
     */
    public static synchronized void clear() {
        try {
            for (File file : tempDirectory.listFiles()) {
                try {
                    if (file.isDirectory()) {
                        IOUtil.recursiveDelete(file);
                    } else {
                        IOUtil.safeDelete(file);
                    }
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }
}
