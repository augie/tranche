/*
 * Logger.java
 *
 * Created on October 7, 2007, 3:11 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package stress.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import org.tranche.util.IOUtil;
import org.tranche.util.Text;
import stress.client.Configuration;
import stress.client.StressClient;

/**
 *
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class Logger {
    
    private static File log = null;
    
    private static boolean isLazyLoaded = false;
    
    private static BufferedWriter writer = null;
    
    private static synchronized void lazyLoad() throws Exception {
        if (isLazyLoaded)
            return;
        isLazyLoaded = true;
        // Backup any existing file
        
        try {
            log = Configuration.getOutputFile();
        } catch (NoClassDefFoundError e) {
            e.printStackTrace(System.err);
            File fallbackOutputFile = new File(System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "stress-test-output.txt");
            System.err.println("Error trying to load output file path: "+e.getMessage());
            System.err.println("Falling back on hard-coded output path value: "+fallbackOutputFile);
            log = new File(fallbackOutputFile.getAbsolutePath());
        }
//        // Cheap effort to backup existing.
//        if (log.exists()) {
//            File backup = new File(log.getParentFile(),"~"+log.getName());
////            IOUtil.safeDelete(backup);
////            backup.createNewFile();
////            IOUtil.setBytes(IOUtil.getBytes(log),backup);
//            IOUtil.copyFile(log,backup);
//        }
//
//        // Make sure can write to file
//        log.createNewFile();
//        if (!log.exists()) {
//            throw new Exception("Can't create log <"+log.getAbsolutePath()+">");
//        }
        
        boolean wasCreated = false;
        
        // Note that multiple JVMs may lazy load here!
        if (!log.exists() && StressClient.isStressClientRunning) {
            
            System.out.println("Preparing log for output <"+log.getAbsolutePath()+">");
            
            // Create the file -- this is the JVM running the top-level stress test
            log.createNewFile();
            
            // uh-oh
            if (!log.exists()) {
                throw new Exception("Can't create log <"+log.getAbsolutePath()+">");
            }
            
            wasCreated = true;
        }
        
        // Don't want child JVMs to clobber log
        if (wasCreated) {
            // Make the writer
            writer = new BufferedWriter(new FileWriter(log,false));
            
            // Write out the header
            writer.write("\"Client Connections\",");
            writer.write("\"Num. Files Per Project\",");
            writer.write("\"Max. File Size\",");
            writer.write("\"Total Test Size (all projects)\",");
            writer.write("\"Performed Deletes\",");
            writer.write("\"Test Time\",");
            writer.write("\"Test Passed\"");
            writer.newLine();
            writer.flush();
        }
    }
    
    private static int numConnections = 0;
    private static int numFiles = 0;
    private static long maxFileSize = 0;
    private static long totalSize = 0;
    private static long start = 0;
    private static boolean failed = false;
    private static boolean isPerformDeletes = false;
    
    public static void startTest(StressTest test) throws Exception {
        lazyLoad();
        numConnections = test.getClientCount();
        numFiles = test.getFiles();
        maxFileSize = test.getMaxFileSize();
        isPerformDeletes = test.isRandomlyDeleteChunks();
        start = System.currentTimeMillis();
    }
    
    public static synchronized void addFileToTotalSize(int size) throws Exception {
        lazyLoad();
        totalSize += size;
    }
    
    public static void setTestFailed() {
        failed = true;
    }
    
    public static synchronized void stopTest() throws Exception {
        lazyLoad();
        
        long timeInMillis = System.currentTimeMillis()-start;
        
        System.out.println("Finished test (passed="+String.valueOf(!failed)+"), took "+Text.getPrettyEllapsedTimeString(timeInMillis));
        
        writer.write(numConnections+",");
        writer.write(numFiles+",");
        writer.write(maxFileSize+",");
        writer.write(totalSize+",");
        writer.write(String.valueOf(isPerformDeletes)+",");
        writer.write(timeInMillis+",");
        writer.write(String.valueOf(!failed));
        writer.newLine();
        writer.flush();
        
        // Reset values
        numConnections = 0;
        numFiles = 0;
        maxFileSize = 0;
        totalSize = 0;
        start = 0;
        failed = false;
    }
    
    public static synchronized void close() throws Exception {
        lazyLoad();
        System.out.println("Log available at: "+log.getAbsolutePath());
        IOUtil.safeClose(writer);
    }
}
