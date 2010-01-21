/*
 * TestUtil.java
 *
 * Created on October 5, 2007, 10:22 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package stress.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.tranche.hash.Base16;
import org.tranche.users.UserZipFile;
import org.tranche.users.UserZipFileUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.Text;
import stress.client.Configuration;

/**
 * <p>Common functionality.</p>
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class StressTestUtil {

    /**
     * Set to true for verbose output about memory usage.
     * Note: I put Java 6 and JVM flag to dump if OutOfMemoryError, so shouldn't need
     * unless running on Java 5 or you didn't set the JVM flag.
     */
    static final boolean MONITOR_MEMORY = false;
//    
//    /**
//     * Put paths to source code directories here, i.e., the root dir for the svn-controlled directory!
//     */
//    private static final String trancheJohnYokoSourceDir = "/home/tranche/Documents/Source Code/Stress-Tests/TrancheStressTest/";
//    private static final String bryanMacSourceDir = "/Volumes/data/Sources/java/ProteomeCommons.org/Stress-Tests/TrancheStressTest/";
//    
//    /**
//     * Choose the appropriate directory here!
//     */
//    private static final String sourceCodeDir = trancheJohnYokoSourceDir;
//    
    /**
     * User zip file information.
     */
    private static File USER_ZIP_FILE = null;
    private static boolean isLazyLoaded = false;
    private static UserZipFile uzf = null;
    private static X509Certificate cert = null;
    private static PrivateKey key = null;

    private synchronized static void lazyLoad() throws Exception {
        if (isLazyLoaded) {
            return;
        }
        isLazyLoaded = true;

        // Get user zip file
//        URL userZipFileUrl = StressTestConfig.class.getResource("/stress/tranche-certs/uzf/user.zip.encrypted");
//        USER_ZIP_FILE = new File(userZipFileUrl.getPath());
        USER_ZIP_FILE = new File(Configuration.getStressTestBinaryDir(), "stress/tranche-certs/uzf/user.zip.encrypted");

//        final String userZipFilePath = Configuration.getSourceCodeDir()+"/stress/tranche-certs/uzf/user.zip.encrypted";
//        USER_ZIP_FILE = new File(userZipFilePath);

        // Load the user file
        if (!USER_ZIP_FILE.exists()) {
            throw new RuntimeException("Can't find the user zip file: " + USER_ZIP_FILE.getAbsolutePath());
        }
        uzf = new UserZipFile(USER_ZIP_FILE);

        String password = null;

        BufferedReader reader = null;
        try {
            // Get the password
            reader = new BufferedReader(new FileReader(new File(USER_ZIP_FILE.getAbsolutePath() + ".password")));
            password = reader.readLine();
        } finally {
            IOUtil.safeClose(reader);
        }

        uzf.setPassphrase(password);

        cert = uzf.getCertificate();
        key = uzf.getPrivateKey();

        if (cert == null) {
            throw new RuntimeException("Certificate is null, check user file <" + uzf.getFile().getAbsolutePath() + ">");
        }
        if (key == null) {
            throw new RuntimeException("Private key is null, check user file <" + uzf.getFile().getAbsolutePath() + ">");        // Start up the thread to print out thread dumps
        }
        new ThreadDumpThread().start();

        if (StressTestUtil.MONITOR_MEMORY) {
            new MemoryUseMonitorThread().start();
        }
    }

    /**
     * Not necessary, but can load early.
     */
    public static void loadEarly() throws Exception {

        // Force Configuration's lazyLoad() first
        Configuration.getResultsFile();

        // Load the stress util
        lazyLoad();
    }

    /**
     * Get the test user.
     */
    public static synchronized UserZipFile getUserZipFile() throws Exception {
        lazyLoad();
        return uzf;
    }

    /**
     * Synchronized to avoid InvalidKeyExceptions on signing uploads.
     */
    public static synchronized X509Certificate getCertificate() throws Exception {
        lazyLoad();
        return cert;
    }

    /**
     * Synchronized to avoid InvalidKeyExceptions on signing uploads.
     */
    public static synchronized PrivateKey getPrivateKey() throws Exception {
        lazyLoad();
        return key;
    }
    
    /**
     * <p>Creates a new user signed by stress test user. Make sure you delete when done!</p>
     * @param username
     * @param password
     * @return
     * @throws java.lang.Exception
     */
    public static UserZipFile getNewSignedUserZipFile(String username, String password) throws Exception{
        lazyLoad();
        
        File tmp = TempFileUtil.createTemporaryFile(".zip.encrypted");
        
        return UserZipFileUtil.createSignedUser(username, password, tmp, false, getCertificate(), getPrivateKey());
    }

    /**
     * <p>Creates a test project in the temp directory. Only call if you are a client.</p>
     * @param numFiles The number of files in the project.
     * @param maxFileSize The maximum size of file
     * @return The directory holding the project.
     */
    public static File createTestProject(int numFiles, long maxFileSize) throws Exception {
        lazyLoad();
        if (numFiles < 0) {
            throw new RuntimeException("Project must have zero or more files. Found: " + numFiles);
        }
        if (maxFileSize < 1024) {
            throw new RuntimeException("Max file size must be at least 1024. Found " + maxFileSize);
        }

        // Down cast, make sure no overflow. My bad.
        int test = (int) maxFileSize;
        if (test != maxFileSize) {
            throw new RuntimeException(maxFileSize + " is too large. Cannot be above " + Integer.MAX_VALUE);        // Make sure there is a temp dir or can be created
        }
        if (!Configuration.getTempDirectory().exists() && !Configuration.getTempDirectory().mkdirs()) {
            if (!Configuration.getTempDirectory().exists()) {
                throw new RuntimeException("Cannot make temp directory <" + Configuration.getTempDirectory().getAbsolutePath() + ">");
            }
        }

        // Going to randomly create name, avoid clobbering existing project
        File project = null;
        byte[] tmpBytes = new byte[12];
        Random r = new Random();

        // Continue until we find a new project name
        while (project == null || project.exists()) {
            r.nextBytes(tmpBytes);
            project = new File(Configuration.getTempDirectory(), Base16.encode(tmpBytes));
        }

        // Create the project
        project.mkdirs();
        if (!project.exists()) {
            throw new RuntimeException("Cannot create test project <" + project + ">");        // Add files
        }
        File next;
        for (int i = 0; i < numFiles; i++) {
            next = new File(project, RandomUtil.getLong() +"."+ i + ".stress-test.txt");
            // Next file's size
            int size = 1024 + r.nextInt((int) maxFileSize - 1023);
            tmpBytes = new byte[size];
            r.nextBytes(tmpBytes);
            IOUtil.setBytes(tmpBytes, next);
            Logger.addFileToTotalSize(size);
        }

        if (project.list().length != numFiles) {
            throw new Exception("Assertion failed. Trying to make project of size " + numFiles + ", but made project of size " + project.list().length);
        }
        return project;
    }

    /**
     * Only call if you are a server.
     */
    public static File getServerDataDirectory() throws Exception {
        // Make sure there is a data dir or can be created
        if (!Configuration.getTempDirectory().exists() && !Configuration.getTempDirectory().mkdirs()) {
            if (!Configuration.getTempDirectory().exists()) {
                throw new RuntimeException("Cannot make data directory <" + Configuration.getTempDirectory().getAbsolutePath() + ">");
            }
        }
        return Configuration.getTempDirectory();
    }

    /**
     *
     */
    static class ThreadDumpThread extends Thread {

        ThreadDumpThread() {
            setPriority(Thread.MIN_PRIORITY);
            setDaemon(true);
        }

        @Override()
        public void run() {
            long start = System.currentTimeMillis();

            long timestamp;

            // Start a thread. If user types "dump", perform dump
            Thread t = new Thread("Stress Test Console Thread") {

                @Override()
                public void run() {
                    BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
                    //System.out.println("STARTING INTERACTIVE CONSOLE...");
                    String input;
                    while (true) {
                        try {
                            input = keyboard.readLine();

                            if (input == null) {
                                continue;
                            }
                            if (input.equalsIgnoreCase("dump")) {
                                dumpThreads();
                            } else {
                                System.out.println("You typed: " + input);
                                System.out.println("Known commands: dump");
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            };
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();

            while (true) {

                timestamp = System.currentTimeMillis();

                // Print out a thread dump daily
                if (timestamp > start + (1000 * 60 * 60 * 24)) {

                    dumpThreads();
                    start = System.currentTimeMillis();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }

        private void dumpThreads() {
            System.out.println("===== THREAD DUMP AT " + Text.getFormattedDate(System.currentTimeMillis()) + "======");

            Map<Thread, StackTraceElement[]> dump = Thread.getAllStackTraces();
            Set<Thread> threads = dump.keySet();

            System.out.println("*** " + threads.size() + " Threads ***");
            int count = 0;
            for (Thread t : threads) {
                count++;
                System.out.println(count + ") " + t.getName() + "(STATE = " + t.getState() + ")");


                for (StackTraceElement e : dump.get(t)) {
                    System.out.println(" " + e.toString());
                }
                System.out.println();
            }

            System.out.println("===== ===== ===== ===== ===== ===== ===== ===== ===== ===== ===");

        }
    }

    static class MemoryUseMonitorThread extends Thread {

        MemoryUseMonitorThread() {
            super("MemoryUseMonitorThread (in StressTestUtil)");
            setPriority(Thread.MIN_PRIORITY);
            setDaemon(true);
        }

        @Override()
        public void run() {
            long totalMem, freeMem, usedMem;
            while (true) {
                try {
                    Thread.sleep(120 * 1000);
                    totalMem = Runtime.getRuntime().totalMemory();
                    freeMem = Runtime.getRuntime().freeMemory();
                    usedMem = totalMem - freeMem;
                    System.out.println("At " + Text.getFormattedDate(System.currentTimeMillis()) + ": total memory=" + totalMem + ", used memory=" + usedMem + ", free memory=" + freeMem);
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }
    }
}