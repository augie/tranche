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
package org.tranche.project;

import org.tranche.users.MakeUserZipFileTool;
import org.tranche.users.UserZipFile;
import org.tranche.get.CommandLineGetFileToolListener;
import org.tranche.get.GetFileTool;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import org.tranche.security.Signature;
import org.tranche.TrancheServer;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataUtil;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>A tool for saving a project from the Tranche network to a disk drive. The project is saved in chunked data form, not as actual files. To retrieve the data once created, set up a Flat File Tranche Server on the directory.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class BackupProjectTool {

    private BigHash projectHash;
    private BigInteger partitionSize;
    private File rootLocation;
    private ArrayList<PrintStream> out = new ArrayList();
    private UserZipFile authZip;
    private Signature trustedSig;
    /**
     * <p>Servers from which to backup data.</p>
     */
    public ArrayList<TrancheServer> downloadFromServers = new ArrayList();
    private ArrayList<Partition> partitions = new ArrayList();    // If the project is encrypted
    private String passphrase;

    /**
     * 
     * @param projectHash
     * @param partitionSize
     * @param rootLocation
     * @throws java.lang.Exception
     */
    public BackupProjectTool(String projectHash, BigInteger partitionSize, File rootLocation) throws Exception {
        this(projectHash, partitionSize, rootLocation, null);
    }

    /**
     * 
     * @param projectHash
     * @param partitionSize
     * @param rootLocation
     * @param passphrase
     * @throws java.lang.Exception
     */
    public BackupProjectTool(String projectHash, BigInteger partitionSize, File rootLocation, String passphrase) throws Exception {
        this.passphrase = passphrase;

        // set up the initial values
        this.projectHash = BigHash.createHashFromString(projectHash);
        this.partitionSize = partitionSize;
        this.rootLocation = rootLocation;
        out.add(System.out);

        // make sure the user specified a project hash, a valid partition size, and a location
        {
            if (projectHash.trim().equals("")) {
                throw new Exception("Please specify a project hash.");
            }

            // if there was no partition size specified, make it 0 (1 partition)
            if (partitionSize.equals("")) {
                this.partitionSize = new BigInteger("0");
            }

            // the partition size cannot be less than 1 MB
            if (partitionSize.compareTo(BigInteger.ZERO) > 0 && partitionSize.compareTo(BigInteger.valueOf(1153433)) < 0) {
                throw new Exception("Minimum partition size is 1.1 MB");
            }

            // if the root location doesn't exist, make it
            if (!rootLocation.exists()) {
                if (!rootLocation.mkdirs()) {
                    throw new RuntimeException("Can't created backup directory: " + rootLocation);
                }
            }

            // the root location must be a directory
            if (!rootLocation.isDirectory()) {
                throw new Exception("Your download location must be a directory.");
            }
        }

        // create a zip file as an administrator
        authZip = createUser("Admin", "password", "auth.zip.encrypted", true);
    }

    /**
     * <p>Executes the backup process.</p>
     * @throws java.lang.Exception
     */
    public void run() throws Exception {
        // the project file to back up
        ProjectFile pf = null;
        // the files to download
        ArrayList<BigHash> filesToDownload = new ArrayList();

        output("***** Backup " + projectHash.toString() + "*****\n");

        try {
            output("Waiting for startup.");

            // connect to the servers to download from
            for (String url : NetworkUtil.getStatus().getURLs()) {
                output("Connecting to " + url);
                downloadFromServers.add(ConnectionUtil.connectURL(url, false));
            }

            // get the project file
            {
                GetFileTool gft = new GetFileTool();
                gft.setValidate(false);
                gft.setHash(projectHash);

                // Set a passphrase if one was offered
                if (this.passphrase != null) {
                    gft.setPassphrase(this.passphrase);                // make sure the gft is using the specified servers
                }
                for (TrancheServer ts : downloadFromServers) {
                    gft.addServerToUse(IOUtil.createURL(ts));
                }

                // put the project file in the first partition
                File projectFile = TempFileUtil.createTemporaryFile();

                // get the project
                output("Downloading the project file.");
                gft.setSaveFile(projectFile);
                gft.getFile();

                // create the project file
                pf = ProjectFileUtil.read(new BufferedInputStream(new FileInputStream(projectFile)));

                IOUtil.safeDelete(projectFile);
            }

            // set up the first partition
            addPartition();

            // download the project file
            filesToDownload.add(projectHash);

            // add all the project's files to the files to download
            for (ProjectFilePart pfp : pf.getParts()) {
                filesToDownload.add(pfp.getHash());
            }

            // get all the data, including project file
            output("Starting data download.");

            MetaData md = null;
            byte[] bytesToDownload;
            for (BigHash fileHash : filesToDownload) {
                output("Getting meta data: " + fileHash.toString());

                // get the meta data
                int count = 0;
                bytesToDownload = null;
                do {
                    try {
                        bytesToDownload = (byte[])IOUtil.getMetaData(downloadFromServers.get(count++), fileHash, false).getReturnValueObject();
                    } catch (Exception e) { /* ignore failures and try the next server */ }
                } while (bytesToDownload == null && count < downloadFromServers.size());

                if (bytesToDownload == null) {
                    throw new Exception("Meta data could not be downloaded. Backup failed.");
                }

                output("Reading meta data: " + fileHash.toString());

                // create the meta data
                try {
                    md = MetaDataUtil.read(new ByteArrayInputStream(bytesToDownload));
                } catch (Exception e) { /* ignore */ }

                // add a trusted signature for validation later -- the logic here has not been modified based on multiple uploaders
                if (trustedSig == null) {
                    trustedSig = md.getSignature();
                }

                // check the partition to be used for fullness
                if (partitionSize.compareTo(BigInteger.ZERO) != 0 && partitionSize.compareTo(partitions.get(partitions.size() - 1).bytesDownloaded.add(BigInteger.valueOf((long) bytesToDownload.length))) <= 0) {
                    addPartition();
                }

                if (!IOUtil.hasMetaData(partitions.get(partitions.size() - 1).ffServer, fileHash)) {

                    // add the data to this partition
                    output("Adding meta data to partition " + partitions.size());
                    try {
                        IOUtil.setMetaData(partitions.get(partitions.size() - 1).ffServer, authZip.getCertificate(), authZip.getPrivateKey(), false, fileHash, bytesToDownload);
                    } catch (Exception e) { /* ignore when the data is already set */ }

                } else {
                    output("Skipping meta data: " + fileHash.toString());
                }

                // increment the number of bytes downloaded to this partition accordingly
                partitions.get(partitions.size() - 1).bytesDownloaded = partitions.get(partitions.size() - 1).bytesDownloaded.add(BigInteger.valueOf((long) bytesToDownload.length));

                // download all the data chunks
                output("Downloading data for file: " + fileHash.toString());
                for (BigHash chunkHash : md.getParts()) {

                    // check the partition to be used for fullness
                    if (partitionSize.compareTo(BigInteger.ZERO) != 0 && partitionSize.compareTo(partitions.get(partitions.size() - 1).bytesDownloaded.add(BigInteger.valueOf((long) chunkHash.getLength()))) <= 0) {
                        addPartition();
                    }

                    // skip chunks already downloaded
                    if (IOUtil.hasData(partitions.get(partitions.size() - 1).ffServer, chunkHash)) {
                        output("Skipping data chunk: " + chunkHash.toString());

                        // increment the number of bytes downloaded to this partition accordingly
                        partitions.get(partitions.size() - 1).bytesDownloaded = partitions.get(partitions.size() - 1).bytesDownloaded.add(BigInteger.valueOf((long) chunkHash.getLength()));

                        continue;
                    }

                    output("Downloading data chunk: " + chunkHash.toString());

                    bytesToDownload = null;
                    count = 0;
                    do {
                        try {
                            bytesToDownload = (byte[])IOUtil.getData(downloadFromServers.get(count++), chunkHash, false).getReturnValueObject();
                        } catch (Exception e) { /* ignore failures and try the next server */ }
                    } while (bytesToDownload == null && count < downloadFromServers.size());

                    if (bytesToDownload == null) {
                        throw new Exception("Data could not be found. Backup failed.");
                    }

                    output("Adding data to partition " + partitions.size());

                    // add the data to this partition
                    try {
                        IOUtil.setData(partitions.get(partitions.size() - 1).ffServer, authZip.getCertificate(), authZip.getPrivateKey(), chunkHash, bytesToDownload);
                    } catch (Exception e) { /* ignore when the data is already set */ }

                    // increment the number of bytes downloaded to this partition accordingly
                    partitions.get(partitions.size() - 1).bytesDownloaded = partitions.get(partitions.size() - 1).bytesDownloaded.add(BigInteger.valueOf((long) bytesToDownload.length));
                }
            }

            output("\n***** Download Complete. Verifying downloaded files. *****\n");

            // create the list of servers to use
            ArrayList<String> serversToUse = new ArrayList();
            for (Partition p : partitions) {
                serversToUse.add(IOUtil.createURL(p.ffServer));
            }

            try {
                // use the get file tool with validation to download the project
                GetFileTool gft = new GetFileTool();
                gft.setHash(projectHash);
                for (PrintStream ps : out) {
                    gft.addListener(new CommandLineGetFileToolListener(gft, ps));
                }
                gft.setValidate(true);
                gft.setServersToUse(serversToUse);

                // download to a temporary directory
                File tempDirectory = TempFileUtil.createTemporaryDirectory();
                gft.setSaveFile(tempDirectory);
                gft.getDirectory();

                output("Downloaded files are valid.");
            } catch (Exception e) {
                output(e.getMessage());
            } finally {
                output("\n***** Backup complete! *****");
            }
        } finally {
            for (TrancheServer ts : downloadFromServers) {
                IOUtil.safeClose(ts);
            }
            for (Partition p : partitions) {
                p.ffServer.close();
            }

            if (pf != null && pf.getParts() instanceof DiskBackedProjectFilePartSet) {
                ((DiskBackedProjectFilePartSet) pf.getParts()).destroy();
            }
        }
    }

    /**
     * <p>Add a PrintStream to collection; output statements will be printed to all PrintStream instances.</p>
     * @param out
     */
    public void addOutput(PrintStream out) {
        this.out.add(out);
    }

    /**
     * <p>Print a link to all PrintStream instances in collection.</p>
     * @param s
     */
    private void output(String s) {
        for (PrintStream ps : out) {
            ps.println(s);
        }
    }

    /**
     * <p>Adds a partition to backup; useful for partitioning all backups between disks or other storage media (e.g., CD).</p>
     * @throws java.lang.Exception
     */
    private void addPartition() throws Exception {
        output("Setting up partition " + (partitions.size() + 1));
        partitions.add(new Partition(rootLocation + "/partition" + (partitions.size() + 1) + "/"));
    }

    /**
     * <p>Create a user for use by the backup tool.</p>
     * @param username
     * @param password
     * @param filename
     * @param admin
     * @return
     * @throws java.lang.Exception
     */
    private UserZipFile createUser(String username, String password, String filename, boolean admin) throws Exception {

        MakeUserZipFileTool maker = new MakeUserZipFileTool();
        maker.setName(username);
        maker.setPassphrase(password);
        maker.setValidDays(1);

        File file = TempFileUtil.createTemporaryFile(filename);
        file.createNewFile();

        maker.setSaveFile(file);
        UserZipFile zip = (UserZipFile) maker.makeCertificate();

        // Set user permissions as admin (server needs user registered or it will
        // throw a SecurityException on attempted file upload)
        if (admin) {
            zip.setFlags((new SecurityUtil()).getAdmin().getFlags());
        }

        return zip;
    }

    /**
     * <p>Get the number of partitions required for the backup of project.</p>
     * @param pf
     * @param partitionSize
     * @return
     */
    private int getPartitions(ProjectFile pf, BigInteger partitionSize) {
        // initialize the partition size
        int num = 1;

        // if the partition size is 0, have only 1 partition
        if (partitionSize.longValue() > 0) {

            // while the size of the project is bigger than the number of partitions times the partition size
            while (pf.getSize().compareTo(partitionSize.multiply(BigInteger.valueOf((long) num))) > 0) {

                // add a partition
                num++;

            }
        }

        return num;
    }

    /**
     * <p>A partition used to store a certain amount of data.</p>
     */
    private class Partition {

        public FlatFileTrancheServer ffServer;
        public BigInteger bytesDownloaded;

        /**
         * 
         * @param dir
         * @throws java.lang.Exception
         */
        public Partition(String dir) throws Exception {

            // delete old configuration file
            if (new File(dir + "configuration").exists()) {
                new File(dir + "configuration").delete();
            }

            // create the flat file tranche server
            ffServer = new FlatFileTrancheServer(dir);

            // add this user to the list of users
            if (!ffServer.getConfiguration().getUsers().contains(authZip)) {
                ffServer.getConfiguration().addUser(authZip);
            }

            // create the big integer for tracking how much has been downloaded to this server
            //  starting size is 4096 to account for configuration file overhead
            bytesDownloaded = new BigInteger("104857");
        }
    } // Partition
}
