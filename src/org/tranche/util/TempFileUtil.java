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
import org.tranche.commons.DebugUtil;

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
 * @author James "Augie" Hill - augman85@gmail.com
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
    private static final int maxTempDirAttempts = 50;
    // to track temp files made
    private static int count = 0;
    // To track temp directories made!
    private static final Map<String, Integer> tmpDirCounts = new HashMap();
    // the file that the temp directory is in
    private static File tempDirectory;

    static {
        loadTemporaryDirectory();
    }

    /**
     * <p>Lock on a file to determine whether another JVM is using the temporary directory or file.</p>
     * @param lockToTry
     * @return
     */
    private static synchronized boolean tryLock(File lockToTry) {
        try {
            // make if the file doesn't exist
            if (!lockToTry.exists()) {
                if (!lockToTry.mkdirs()) {
                    return false;
                }
            }

            // try to get a lock
            File tempDirLock = new File(lockToTry, "lock.file");
            // make as a file...not a directory
            if (!tempDirLock.exists()) {
                if (!tempDirLock.createNewFile()) {
                    return false;
                }
            }

            // Try (and keep lock), or fail
            if (!canLockFile(tempDirLock, false)) {
                return false;
            }

            // otherwise, set the references an return true
            tempDirectory = lockToTry;

            // clean up the old temp directory
            recursiveDeleteSkipLockFiles(tempDirLock);

            // before returning, purge any directories that already exist
            for (int i = 1; i < maxTempDirAttempts; i++) {
                File checkExistingDir = new File(lockToTry.getAbsolutePath() + "-" + i);
                if (checkExistingDir.exists()) {
                    // try to lock it
                    File purgeDirLock = new File(checkExistingDir, "lock.file");

                    // Try (but don't keep) lock
                    if (canLockFile(purgeDirLock, true)) {
                        recursiveDeleteSkipLockFiles(checkExistingDir);
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
     * <p>Handles logic of testing a file lock on file and, if locking not implemented on file system, test an alternative lock.</p>
     * @param lockFile
     * @return
     * @throws Exception
     */
    private static synchronized boolean canLockFile(File lockFile, boolean isReleaseLockAfterTest) throws Exception {
        FileChannel fc = new RandomAccessFile(lockFile, "rw").getChannel();
        FileLock lock = null;
        try {
            lock = fc.tryLock();

            // --------------------------------------------------------------------------------------
            // Note: everything that is NOT tryLock should go in the safe try-catch block below.
            //       tryLock is the only thing allowed to throw an IOException, since this behavior
            //       indicated that file system does not permit locks, and the work-around must be
            //       used.
            // --------------------------------------------------------------------------------------
            try {
                if (lock == null) {

                    fc.close();

                    // Something else has lock, so can't secure
                    return false;
                }

                if (isReleaseLockAfterTest) {
                    lock.release();
                }
            } catch (Exception nope) { /* safe close */ }

        } catch (IOException ioe) {
            // ----------------------------------------------------------------------------------------------
            // The only IOException candidate in above try block must be tryLock!
            //
            // Happens on file system that does not support file locking. (Distributed? E.g., work/ volume on Queen Bee)
            // Instead, see whether work around lock, lock-workaround.file, exists
            // ----------------------------------------------------------------------------------------------
            File workAround = new File(lockFile.getParentFile(), "lock-workaround.file");

            if (workAround.exists()) {
                // Work around used by other process, so can't secure
                return false;
            }
            if (!workAround.createNewFile()) {
                // Can't write to directory/file, so bail
                return false;
            }

            System.out.println("WARNING: Could not open file lock, so using a work-around file \"lock\" at: " + workAround.getAbsolutePath());

            // This is normally bad b/c not guarenteed to execute and consumes memory.
            // However, since this is a single, empty file for special environments that can be
            // manually cleaned up, it is the best solution.
            workAround.deleteOnExit();
        }
        return true;
    }

    /**
     * <p>Helper method to clean up old directories.</p>
     * @param directoryToDelete
     */
    private static synchronized void recursiveDeleteSkipLockFiles(final File directoryToDelete) {
        // delete everything but the lock file
        ArrayList<File> filesToDelete = new ArrayList();
        filesToDelete.add(directoryToDelete);
        while (filesToDelete.size() > 0) {
            File fileToDelete = filesToDelete.remove(0);
            // skip the lock file
            if (fileToDelete.getName().equals("lock.file") || fileToDelete.getName().equals("lock-workaround.file")) {
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
    private synchronized static boolean tryToMakeFile(File f) {
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
    public synchronized static void setTemporaryDirectory(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IOException(directory.getAbsolutePath() + " is not a directory. Cannot set as temp directory.");
        }
        if (!directory.canWrite() || !directory.canRead()) {
            throw new IOException(directory.getAbsolutePath() + " has constricting permissions (readable?=" + directory.canRead() + ", writeable?=" + directory.canWrite() + "). Cannot set as temp directory.");
        }
        if (tempDirectory.getAbsolutePath().equals(directory.getAbsolutePath())) {
            return;
        }
        // clear out the old temp dir
        if (tempDirectory != null) {
            clear();
        }
        tempDirectory = directory;
    }

    /**
     * 
     */
    public static synchronized void loadTemporaryDirectory() {
        if (tempDirectory != null) {
            clear();
        }
        try {
            boolean success = false;
            if (PreferencesUtil.get(PreferencesUtil.PREF_TEMPORARY_FILE_DIRECTORY) != null) {
                success = loadTemporaryDirectory(new File(PreferencesUtil.get(PreferencesUtil.PREF_TEMPORARY_FILE_DIRECTORY)));
            }
            if (!success && System.getProperty(JAVA_TEMP_DIR) != null && !System.getProperty(JAVA_TEMP_DIR).equals("")) {
                success = loadTemporaryDirectory(new File(System.getProperty(JAVA_TEMP_DIR), DEFAULT_TEMP_SUB_DIR));
            }
            if (!success) {
                success = loadTemporaryDirectory(new File(System.getProperty(JAVA_USER_HOME), DEFAULT_TEMP_SUB_DIR));
            }
            if (!success && DEFAULT_TEMP_SUB_DIR.startsWith(".")) {
                success = loadTemporaryDirectory(new File(System.getProperty(JAVA_USER_HOME), DEFAULT_TEMP_SUB_DIR.substring(1)));
            }
            if (!success) {
                tempDirectory = null;
            }
        } catch (Exception e) {
            DebugUtil.debugErr(TempFileUtil.class, e);
        }
    }

    /**
     * 
     * @param startWith
     * @return
     */
    private static final boolean loadTemporaryDirectory(File startWith) {
        if (tryLock(startWith)) {
            return true;
        }
        for (int i = 1; i < maxTempDirAttempts; i++) {
            if (tryLock(new File(startWith.getParent(), startWith.getName() + "-" + i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Returns the root temporary directory.</p>
     * <p>Note that the client should not attempt to create files from this directory directly, but instead should use methods in this class.</p>
     * @return
     */
    public synchronized static String getTemporaryDirectory() {
        if (tempDirectory == null) {
            loadTemporaryDirectory();
        }
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
