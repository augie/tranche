/*
 * ClientConnectionThread.java
 *
 * Created on October 5, 2007, 10:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package stress.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Random;
import org.tranche.TrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.hash.DiskBackedHashList;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataUtil;
import org.tranche.util.FileEncoding;
import org.tranche.util.IOUtil;
import org.tranche.util.OperatingSystemUtil;
import org.tranche.util.Text;
import stress.util.Logger;
import stress.util.StressIOUtil;
import stress.util.StressTest;
import stress.util.StressTestUtil;

/**
 * Represents one client connection to the test server
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class ClientConnectionThread extends Thread {
    
    /**
     *
     */
    public static final boolean isDebug = false;
    
    /**
     * If file to redirect output, set here. If none, set to null.
     */
    public static final File clientOutputRedirect = new File("/home/tranche/Desktop/stress-test-client.out-and-err");
    
    /**
     * If you change, please save (comment out) old options =)
     */
    public static final String JVM_OPTIONS = "-Xms512m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError";
    
    private String ip = null;
    private StressTest test = null;
    
    /** Creates a new instance of ClientConnectionThread */
    public ClientConnectionThread(String ip, StressTest test) {
        super("Stress client thread for "+ip);
        this.ip = ip;
        this.test = test;
    }
    
    private String getTrancheURL() throws Exception {
        return "tranche://"+ip+":"+Configuration.getTranchePort();
    }
    @Override
    public void run() {
        try {
            
            if (test.isRandomlyDeleteChunks()) {
                testAddingDeletingRawChunks();
            } else {
                testUsingSeparateJVMAFTGFT();
            }
        } catch (Exception ex) {
            System.out.println("Exception in client thread: "+ex.getMessage()+", caught at: "+Text.getFormattedDate(System.currentTimeMillis()));
            
            // Log the exception
            Logger.setTestFailed();
        } finally {
            
        }
    }
    
    private void testUsingSeparateJVMAFTGFT() throws Exception {
        
        String command = "java "+JVM_OPTIONS+" -jar "+Configuration.getClientJARPath()+" "+test.getFiles()+" "+test.getMaxFileSize()+" "+this.getTrancheURL()+" "+test.getMinProjectSize()+" "+test.getMaxProjectSize();
        
//        // Add redirect if one
//        if (clientOutputRedirect != null) {
//            command += " &>> "+clientOutputRedirect;
//        }
//        
//        printTracer(command);
//        System.out.println("Running command: "+command);
        
        // Drop down to the OS to start a new JVM
        int status = -1;
                
        if (clientOutputRedirect == null) {
            System.out.println("Running command: "+command);
            status = OperatingSystemUtil.executeExternalCommand(command);
        } else {
            System.out.println("Running command with redirect<"+clientOutputRedirect.getAbsolutePath()+">: "+command);
            status = OperatingSystemUtil.executeUnixCommandWithOutputRedirect(command,clientOutputRedirect);
        }
    
        printTracer("Client process returned status code of "+status);
        
        // Everything went okay
        if (status == 0) {
            // For now, do nothing
        } 
        
        // Uh-oh
        else {
            throw new Exception("Return status of "+status+", test in separate JVM failed!");
        }
    }
    
//    private void testUsingModifiedAFT() throws Exception {
//        printTracer("Starting test using modified AFT");
//        File saveTo = null;
//        // Create the project
//        File project = StressTestUtil.createTestProject(test.getFiles(),test.getMaxFileSize());
//        
//        try {
//            // Upload the project
//            StressAddFileTool aft = new StressAddFileTool(
//                    ClientConnectionThread.this,
//                    StressTestUtil.getCertificate(),
//                    StressTestUtil.getPrivateKey());
////            AddFileTool aft = new AddFileTool(
////                    StressTestUtil.getCertificate(),
////                    StressTestUtil.getPrivateKey());
//            aft.addServerURL(getTrancheURL());
//            aft.setServersToUploadTo(1);
//            aft.setShowUploadSummary(false);
//            BigHash hash = aft.addFile(project);
//            
//            // Download the project
//            StressGetFileTool gft = new StressGetFileTool(ClientConnectionThread.this);
//            List<String>server = new ArrayList();
//            server.add(getTrancheURL());
//            gft.setServersToUse(server);
//            gft.setHash(hash);
//            gft.setShowDownloadSummary(false);
//            
//            saveTo = new File(project.getParentFile(),"~"+project.getName());
//            saveTo.mkdirs();
//            
//            gft.getDirectory(saveTo);
//            boolean isError = gft.getErrors().size() > 0;
//            
//            // Log the information
//            if (isError) {
//                Logger.setTestFailed();
//            }
//        } finally {
//            if (project != null)
//                IOUtil.recursiveDeleteWithWarning(project);
//            if (saveTo != null)
//                IOUtil.recursiveDeleteWithWarning(saveTo);
//            printTracer("Finished test using modified AFT.");
//        }
//    }
    
    private void testAddingDeletingRawChunks() throws Exception {
        printTracer("Starting test using raw chunks additions/deletions.");
        
        DiskBackedHashList addedDataHashes = null;
        DiskBackedHashList addedMetaHashes = null;
        DiskBackedHashList deletedDataHashes = null;
        DiskBackedHashList deletedMetaHashes = null;
        
        // We're going to figure out how many hashes to add
        long dataToAdd = test.getFiles() * test.getMaxFileSize();
        
        long dataAdded = 0;
        
        TrancheServer rserver = null;
        
        try {
            
            rserver = StressIOUtil.connect(getTrancheURL(),ClientConnectionThread.this);
            
            try {
                addedDataHashes = new DiskBackedHashList();
                addedMetaHashes = new DiskBackedHashList();
                deletedDataHashes = new DiskBackedHashList();
                deletedMetaHashes = new DiskBackedHashList();
            } catch (Exception ex) {
                // Weird disk backed behavior, to fix. For now, show must go on. Try again.
                if (addedDataHashes == null)
                    addedDataHashes = new DiskBackedHashList();
                if (addedMetaHashes == null)
                    addedMetaHashes = new DiskBackedHashList();
                if (deletedDataHashes == null)
                    deletedDataHashes = new DiskBackedHashList();
                if (deletedMetaHashes == null)
                    deletedMetaHashes = new DiskBackedHashList();
            }
            Random r = new Random();
            
            while (dataAdded < dataToAdd) {
                
                byte[] bytes = null;
                BigHash hash;
                // Upload five chunks
                for (int i=0; i<5;i++) {
                    
                    // If true, data. Else meta.
                    if (r.nextBoolean()) {
                        
                        // Random chunk of size b/w 16KB and 1MB
                        bytes = new byte[r.nextInt(1024*1024-16*1024+1)+16*1024];
                        r.nextBytes(bytes);
                        hash = new BigHash(bytes);
                        
                        StressIOUtil.setData(
                                rserver,
                                StressTestUtil.getCertificate(),
                                StressTestUtil.getPrivateKey(),
                                hash,
                                bytes);
                        addedDataHashes.add(hash);
                    } else {
                        
                        bytes = this.getRandomMetaDataBytes();
                        hash = new BigHash(bytes);
                        
                        StressIOUtil.setMetaData(
                                rserver,
                                StressTestUtil.getCertificate(),
                                StressTestUtil.getPrivateKey(),
                                hash,
                                bytes);
                        addedMetaHashes.add(hash);
                    }
                    
                    // Keeps track of how much data left to add
                    dataAdded+=bytes.length;
                    
                    // Increment total project size on log
                    Logger.addFileToTotalSize(bytes.length);
                } // Add five chunks
                
                // Delete two chunks
                for (int i=0; i<2;i++) {
                    // If true, data. Else meta
                    if (r.nextBoolean() && addedDataHashes.size() > 0) {
                        int index = r.nextInt(addedDataHashes.size());
                        if (index >= addedDataHashes.size()) index = addedDataHashes.size()-1;
                        hash = addedDataHashes.get(index);
                        
                        // Data may already be deleted, okay...
                        StressIOUtil.deleteData(
                                rserver,
                                StressTestUtil.getCertificate(),
                                StressTestUtil.getPrivateKey(),
                                hash);
                        
                        deletedDataHashes.add(hash);
                        
                    } else if (addedMetaHashes.size() > 0) {
                        int index = r.nextInt(addedMetaHashes.size());
                        if (index >= addedMetaHashes.size()) index = addedMetaHashes.size()-1;
                        hash = addedMetaHashes.get(index);
                        
                        // MEta may already be deleted, okay...
                        StressIOUtil.deleteMetaData(
                                rserver,
                                StressTestUtil.getCertificate(),
                                StressTestUtil.getPrivateKey(),
                                hash);
                        
                        deletedMetaHashes.add(hash);
                    }
                } // Delete two chunks
            } // Until added right amount of data
            
            for (BigHash h : addedDataHashes) {
                if (deletedDataHashes.contains(h)) {
                    // Better NOT be there
                    if (rserver.hasData(h)) {
                        System.err.println("Uh-oh, data should have been deleted: "+h);
                        throw new Exception("Uh-oh, data should have been deleted: "+h);
                    }
                } else {
                    // Better be there
                    if (!rserver.hasData(h)) {
                        System.err.println("Uh-oh, data should be available: "+h);
                        throw new Exception("Uh-oh, data should be available: "+h);
                    }
                }
            }
            
            for (BigHash h : addedMetaHashes) {
                if (deletedMetaHashes.contains(h)) {
                    // Better NOT be there
                    if (rserver.hasMetaData(h)) {
                        System.err.println("Uh-oh, meta should have been deleted: "+h);
                        throw new Exception("Uh-oh, meta should have been deleted: "+h);
                    }
                } else {
                    // Better be there
                    if (!rserver.hasMetaData(h)) {
                        System.err.println("Uh-oh, meta should be available: "+h);
                        throw new Exception("Uh-oh, meta should be available: "+h);
                    }
                }
            }
        } finally {
            if (addedDataHashes != null)
                addedDataHashes.destroy();
            if (addedMetaHashes != null)
                addedMetaHashes.destroy();
            if (deletedDataHashes != null)
                deletedDataHashes.destroy();
            if (deletedMetaHashes != null)
                deletedMetaHashes.destroy();
            IOUtil.safeClose(rserver);
            printTracer("Finished test using raw chunks additions/deletions.");
        }
    }
    
    private static void printTracer(String msg) {
        if (isDebug) {
            System.out.println("CLIENT_CONNECTION_THREAD> "+msg);
        }
    }
    
    /**
     *
     */
    public static MetaData getRandomMetaData() throws Exception {
        MetaData metaData = new MetaData();
        String[] parts = {
            getRandomFileName(),
            getRandomFileName(),
            getRandomFileName(),
            getRandomFileName()
        };
        metaData.setName(parts[parts.length-1]);
        
        // Add a none encoding
        FileEncoding none = new FileEncoding(FileEncoding.NONE, createRandomBigHash());
        metaData.addEncoding(none);
        
        return metaData;
    }
    
    /**
     *
     */
    public static byte[] getRandomMetaDataBytes() throws Exception {
        MetaData metaData = getRandomMetaData();
        
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            MetaDataUtil.write(metaData, baos);
            
            return baos.toByteArray();
        } finally {
            baos.close();
        }
    }
    
    /**
     *
     */
    public static String getRandomFileName() throws Exception {
        StringBuffer buffer = new StringBuffer();
        
        Random r = new Random();
        int nameSize = r.nextInt(10)+5;
        for (int i=0; i<nameSize; i++) {
            char nextChar = (char)(65+r.nextInt(26));
            
            // Uppercase it randomly
            if (r.nextBoolean())
                nextChar+=32;
            
            buffer.append(nextChar);
        }
        
        // Now add a file extension... maybe
        switch(r.nextInt(5)) {
            case 0:
                // Nope, unix style
                break;
            case 1:
                buffer.append(".txt");
                break;
            case 2:
                buffer.append(".bin");
                break;
            case 3:
                buffer.append(".xml");
                break;
            case 4:
                buffer.append(".dat");
                break;
        }
        
        return buffer.toString();
    }
    
    /**
     * Creates a BigHash representign a chunk of random data b/w 1KB and 1MB
     */
    public static BigHash createRandomBigHash() {
        
        Random r = new Random();
        // Chunk size b/w 1KB and 1MB
        int size = 1024 + r.nextInt(1024*1024-1024);
        
        byte[] data = new byte[size];
        r.nextBytes(data);
        
        return new BigHash(data);
    }
    
}
