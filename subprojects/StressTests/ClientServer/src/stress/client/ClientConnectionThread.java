/*
 * ClientConnectionThread.java
 *
 * Created on October 5, 2007, 10:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package stress.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.tranche.FileEncoding;
import org.tranche.TrancheServer;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.hash.BigHash;
import org.tranche.hash.DiskBackedBigHashList;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.meta.MetaDataUtil;
import org.tranche.network.ConnectionUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.security.Signature;
import org.tranche.time.TimeUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.OperatingSystemUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.Text;
import stress.util.Logger;
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
     * If you change, please save (comment out) old options =)
     */
    public static final String JVM_OPTIONS = "-Xms512m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError";
    private String ip = null;
    private StressTest test = null;

    /** Creates a new instance of ClientConnectionThread */
    public ClientConnectionThread(String ip, StressTest test) {
        super("Stress client thread for " + ip);
        this.ip = ip;
        this.test = test;
    }

    private String getTrancheURL() throws Exception {
        return "tranche://" + ip + ":" + Configuration.getTranchePort();
    }

    @Override()
    public void run() {
        try {
            if (test.isRandomlyDeleteChunks()) {
                testAddingDeletingRawChunks();
            } else {
                testUsingSeparateJVMAFTGFT();
            }
        } catch (Exception ex) {
            System.out.println("Exception in client thread: " + ex.getMessage() + ", caught at: " + Text.getFormattedDate(System.currentTimeMillis()));

            // Log the exception
            Logger.setTestFailed();
        } finally {
        }
    }

    private void testUsingSeparateJVMAFTGFT() throws Exception {

        String command = "java " + JVM_OPTIONS + " -jar " + Configuration.getClientJARPath() + " " + Configuration.getStressTestBinaryDir().getAbsolutePath() + " " + test.getFiles() + " " + test.getMaxFileSize() + " " + this.getTrancheURL() + " " + test.getMinProjectSize() + " " + test.getMaxProjectSize() + " " + test.isUseBatch();

        // Drop down to the OS to start a new JVM
        int status = -1;

        if (Configuration.getOutAndErrRedirectFile() == null) {
            System.out.println("Running command: " + command);
            status = OperatingSystemUtil.executeExternalCommand(command);
        } else {
            System.out.println("Running command with redirect<" + Configuration.getOutAndErrRedirectFile() + ">: " + command);
            status = OperatingSystemUtil.executeUnixCommandWithOutputRedirect(command, Configuration.getOutAndErrRedirectFile());
        }

        printTracer("Client process returned status code of " + status);

        // Everything went okay
        if (status == 0) {
            // For now, do nothing
        } // Uh-oh
        else {
            throw new Exception("Return status of " + status + ", test in separate JVM failed!");
        }
    }

    private void testAddingDeletingRawChunks() throws Exception {
        printTracer("Starting test using raw chunks additions/deletions.");

        DiskBackedBigHashList addedDataHashes = null;
        DiskBackedBigHashList addedMetaHashes = null;
        DiskBackedBigHashList deletedDataHashes = null;
        DiskBackedBigHashList deletedMetaHashes = null;

        // We're going to figure out how many hashes to add
        long dataToAdd = test.getFiles() * test.getMaxFileSize();

        long dataAdded = 0;

        TrancheServer rserver = null;

        try {
            
            ConnectionUtil.lockConnection(ip);

            rserver = ConnectionUtil.getHost(ip);
            if (rserver == null) {
                throw new RuntimeException("Did not connection for host<" + ip + ">. Is this the correct host?");
            }

            try {
                addedDataHashes = new DiskBackedBigHashList();
                addedMetaHashes = new DiskBackedBigHashList();
                deletedDataHashes = new DiskBackedBigHashList();
                deletedMetaHashes = new DiskBackedBigHashList();
            } catch (Exception ex) {
                // Weird disk backed behavior, to fix. For now, show must go on. Try again.
                if (addedDataHashes == null) {
                    addedDataHashes = new DiskBackedBigHashList();
                }
                if (addedMetaHashes == null) {
                    addedMetaHashes = new DiskBackedBigHashList();
                }
                if (deletedDataHashes == null) {
                    deletedDataHashes = new DiskBackedBigHashList();
                }
                if (deletedMetaHashes == null) {
                    deletedMetaHashes = new DiskBackedBigHashList();
                }
            }
            Random r = new Random();

            while (dataAdded < dataToAdd) {

                byte[] bytes = null;
                BigHash hash;
                // Upload five chunks
                for (int i = 0; i < 5; i++) {

                    // If true, data. Else meta.
                    if (r.nextBoolean()) {

                        // Random chunk of size b/w 16KB and 1MB
                        bytes = new byte[r.nextInt(1024 * 1024 - 16 * 1024 + 1) + 16 * 1024];
                        r.nextBytes(bytes);
                        hash = new BigHash(bytes);

                        IOUtil.setData(
                                rserver,
                                StressTestUtil.getCertificate(),
                                StressTestUtil.getPrivateKey(),
                                hash,
                                bytes);
                        addedDataHashes.add(hash);
                    } else {

                        bytes = getRandomMetaDataBytes();
                        hash = new BigHash(bytes);

                        IOUtil.setMetaData(
                                rserver,
                                StressTestUtil.getCertificate(),
                                StressTestUtil.getPrivateKey(),
                                false,
                                hash,
                                bytes);
                        addedMetaHashes.add(hash);
                    }

                    // Keeps track of how much data left to add
                    dataAdded += bytes.length;

                    // Increment total project size on log
                    Logger.addFileToTotalSize(bytes.length);
                } // Add five chunks

                // Delete two chunks
                for (int i = 0; i < 2; i++) {
                    // If true, data. Else meta
                    if (r.nextBoolean() && addedDataHashes.size() > 0) {
                        int index = r.nextInt(addedDataHashes.size());
                        if (index >= addedDataHashes.size()) {
                            index = addedDataHashes.size() - 1;
                        }
                        hash = addedDataHashes.get(index);

                        // Data may already be deleted, okay...
                        IOUtil.deleteData(
                                rserver,
                                StressTestUtil.getCertificate(),
                                StressTestUtil.getPrivateKey(),
                                hash);

                        deletedDataHashes.add(hash);

                    } else if (addedMetaHashes.size() > 0) {
                        int index = r.nextInt(addedMetaHashes.size());
                        if (index >= addedMetaHashes.size()) {
                            index = addedMetaHashes.size() - 1;
                        }
                        hash = addedMetaHashes.get(index);

                        // MEta may already be deleted, okay...
                        IOUtil.deleteMetaData(
                                rserver,
                                StressTestUtil.getCertificate(),
                                StressTestUtil.getPrivateKey(),
                                hash);

                        deletedMetaHashes.add(hash);
                    }
                } // Delete two chunks

            } // Until added right amount of data

            for (final BigHash h : addedDataHashes) {
                final BigHash[] hArr = {h};
                if (deletedDataHashes.contains(h)) {
                    // Better NOT be there
                    if (rserver.hasData(hArr)[0]) {
                        System.err.println("Uh-oh, data should have been deleted: " + h);
                        throw new Exception("Uh-oh, data should have been deleted: " + h);
                    }
                } else {
                    // Better be there
                    if (!rserver.hasData(hArr)[0]) {
                        System.err.println("Uh-oh, data should be available: " + h);
                        throw new Exception("Uh-oh, data should be available: " + h);
                    }
                }
            }

            for (final BigHash h : addedMetaHashes) {
                final BigHash[] hArr = {h};
                if (deletedMetaHashes.contains(h)) {
                    // Better NOT be there
                    if (rserver.hasMetaData(hArr)[0]) {
                        System.err.println("Uh-oh, meta should have been deleted: " + h);
                        throw new Exception("Uh-oh, meta should have been deleted: " + h);
                    }
                } else {
                    // Better be there
                    if (!rserver.hasMetaData(hArr)[0]) {
                        System.err.println("Uh-oh, meta should be available: " + h);
                        throw new Exception("Uh-oh, meta should be available: " + h);
                    }
                }
            }
        } finally {
            if (addedDataHashes != null) {
                addedDataHashes.destroy();
            }
            if (addedMetaHashes != null) {
                addedMetaHashes.destroy();
            }
            if (deletedDataHashes != null) {
                deletedDataHashes.destroy();
            }
            if (deletedMetaHashes != null) {
                deletedMetaHashes.destroy();
            }
            IOUtil.safeClose(rserver);
            printTracer("Finished test using raw chunks additions/deletions.");
            ConnectionUtil.unlockConnection(ip);
        }
    }

    private static void printTracer(String msg) {
        if (isDebug) {
            System.out.println("CLIENT_CONNECTION_THREAD> " + msg);
        }
    }

    /**
     *
     */
    public static MetaData getRandomMetaData() throws Exception {
        Random r = new Random();
        return getRandomMetaData(r.nextInt(5) + 1);
    }

    public static MetaData getRandomMetaData(int uploaders) throws Exception {
        // make some random data less than one MB
        byte[] data = new byte[RandomUtil.getInt(DataBlockUtil.ONE_MB)];
        RandomUtil.getBytes(data);
        ByteArrayInputStream bais = null;

        // create the meta data
        MetaData md = new MetaData();
        for (int i = 0; i < uploaders; i++) {
            // uploader
            Signature signature = null;
            ArrayList<FileEncoding> encodings = new ArrayList<FileEncoding>();
            Map<String, String> properties = new HashMap<String, String>();
            ArrayList<MetaDataAnnotation> annotations = new ArrayList<MetaDataAnnotation>();

            // make the signature
            try {
                bais = new ByteArrayInputStream(data);
                byte[] sigBytes = SecurityUtil.sign(bais, SecurityUtil.getAnonymousKey());
                String algorithm = SecurityUtil.getSignatureAlgorithm(SecurityUtil.getAnonymousKey());
                signature = new Signature(sigBytes, algorithm, SecurityUtil.getAnonymousCertificate());
            } finally {
                IOUtil.safeClose(bais);
            }

            // make encoding
            encodings.add(new FileEncoding(FileEncoding.NONE, getRandomBigHash()));

            // set the basic properties
            properties.put(MetaData.PROP_NAME, RandomUtil.getString(10));
            properties.put(MetaData.PROP_TIMESTAMP_UPLOADED, String.valueOf(TimeUtil.getTrancheTimestamp()));

            // add the uploader
            md.addUploader(signature, encodings, properties, annotations);

            // add parts
            ArrayList<BigHash> parts = new ArrayList<BigHash>();
            for (int j = 0; j < RandomUtil.getInt(50); j++) {
                parts.add(getRandomBigHash());
            }
            md.setParts(parts);
        }
        return md;
    }

    /**
     * Creates a random BigHash. Uses 1MB chunk of random data to generate.
     * @return
     */
    public static BigHash getRandomBigHash() {
        return getRandomBigHash(1024 * 1024);
    }

    public static BigHash getRandomBigHash(int dataSize) {
        byte[] data = new byte[dataSize];
        RandomUtil.getBytes(data);
        return new BigHash(data);
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
        int nameSize = r.nextInt(10) + 5;
        for (int i = 0; i < nameSize; i++) {
            char nextChar = (char) (65 + r.nextInt(26));

            // Uppercase it randomly
            if (r.nextBoolean()) {
                nextChar += 32;
            }
            buffer.append(nextChar);
        }

        // Now add a file extension... maybe
        switch (r.nextInt(5)) {
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
        int size = 1024 + r.nextInt(1024 * 1024 - 1024);

        byte[] data = new byte[size];
        r.nextBytes(data);

        return new BigHash(data);
    }
}
