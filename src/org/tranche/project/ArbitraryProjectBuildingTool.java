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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tranche.ConfigureTranche;
import org.tranche.FileEncoding;
import org.tranche.TrancheServer;
import org.tranche.annotations.Todo;
import org.tranche.annotations.TodoList;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.get.GetFileTool;
import org.tranche.get.GetFileToolReport;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.license.LicenseUtil;
import org.tranche.logs.ConnectionDiagnosticsLog;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTable;
import org.tranche.network.StatusTableRow;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.remote.RemoteTrancheServerPerformanceListener;
import org.tranche.security.SecurityUtil;
import org.tranche.security.Signature;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.users.UserZipFile;
import org.tranche.users.UserZipFileUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.Text;

/**
 * <p>Create an arbitrary project from existing files on the network.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
@TodoList({
    @Todo(desc = "Need to be able to encrypt project", day = 15, month = 8, year = 2008, author = "Bryan")
})
public class ArbitraryProjectBuildingTool {

    public static boolean isPrintServerPerformanceSummary() {
        return isPrintServerPerformanceSummary;
    }

    public static void setIsPrintServerPerformanceSummary(boolean aIsPrintServerPerformanceSummary) {
        isPrintServerPerformanceSummary = aIsPrintServerPerformanceSummary;
    }

    private final DiskBackedProjectFilePartSet pfpSet;
    private final Set<String> paths = new HashSet();
    private final Map<ProjectFilePart, BigHash> projectFilePartsToAdd;
    private final Map<BigHash, BigHash> filesFromProjectsToAdd;
    private final Set<BigHash> individualFilesToAdd;
    private final Set<BigHash> dataSetsToAdd;
    private String title = "No title was specified";
    private String description = "No description was specified";
    private final X509Certificate cert;
    private final PrivateKey key;
    private List<String> serversToUse;
    private final String directoryName;
    private List<ProjectFileCacheItem> projectFileCache = new LinkedList();
    private static final int MAX_CACHE_SIZE = 20;
    private static final boolean DEFAULT_EXCLUDE_LICENSES_FROM_DATA_SETS = true;
    private static boolean isExcludeLicensesFromDataSets = DEFAULT_EXCLUDE_LICENSES_FROM_DATA_SETS;
    
    private static boolean DEFAULT_PRINT_SERVER_PERFORMANCE_SUMMARY = false;
    private static boolean isPrintServerPerformanceSummary = DEFAULT_PRINT_SERVER_PERFORMANCE_SUMMARY;
    
    private final Map<String, RemoteTrancheServerPerformanceListener> rtsListenerMap;
    private final ConnectionDiagnosticsLog rtsDiagnosticsLog;

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {

        try {

            ConfigureTranche.load(args);

            // Replace args to simplify logic
            List<String> newArgs = new ArrayList(args.length - 1);
            for (int i = 1; i < args.length; i++) {
                newArgs.add(args[i]);
            }

            args = newArgs.toArray(new String[0]);

            String username = null, password = null;
            String userFilePath = null, userFilePassword = null;
            String directoryPath = null;

            final String[] validArgs = {
                "-u", "-p", "-U", "-P", "-D", "-t", "-d", "-h", "-H", "-s"
            };
            final Set<String> validArgsSet = new HashSet();
            for (String validArg : validArgs) {
                validArgsSet.add(validArg);
            }

            // Get user file first
            for (int i = 0; i < args.length; i += 2) {
                String name = args[i];
                String value = args[i + 1];

                if (name.equals("-u")) {
                    username = value;
                } else if (name.equals("-p")) {
                    password = value;
                } else if (name.equals("-U")) {
                    userFilePath = value;
                } else if (name.equals("-P")) {
                    userFilePassword = value;
                } else if (name.equals("-D")) {
                    directoryPath = value;
                } else if (name.equals("-h")) {
                    // This argument has two parameters, but skip for now
                    i++;
                } else if (!validArgsSet.contains(name)) {
                    System.err.println("Unrecognized parameter: " + name);
                    printUsage(System.err);
                    System.exit(1);
                }
            }

            // Verify: authentication
            UserZipFile uzf = null;

            try {
                if (username != null && password != null) {
                    uzf = UserZipFileUtil.getUserZipFile(username, password);
                } else if (userFilePath != null && userFilePassword != null) {
                    uzf = new UserZipFile(new File(userFilePath));
                    uzf.setPassphrase(userFilePassword);

                    // Trigger an error early if passphrase incorrect
                    uzf.getPrivateKey();
                } else {
                    throw new Exception("Did not include enough information to authenticate. Must include one of the two options. See USAGE for more information.");
                }
            } catch (Exception e) {
                System.err.println("Cannot authenticate -- " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
                printUsage(System.err);
                System.exit(1);
            }

            // Verify: directory path
            if (directoryPath == null) {
                System.err.println("You must provide a directory path. See USAGE for more information.");
                printUsage(System.err);
                System.exit(1);
            }

            ArbitraryProjectBuildingTool tool = new ArbitraryProjectBuildingTool(uzf.getCertificate(), uzf.getPrivateKey(), directoryPath);

            for (int i = 0; i < args.length; i += 2) {
                String name = args[i];
                String value = args[i + 1];

                if (name.equals("-t")) {
                    tool.setTitle(value);
                } else if (name.equals("-d")) {
                    tool.setDescription(value);
                } else if (name.equals("-h")) {

                    BigHash fileHash = BigHash.createHashFromString(value);
                    BigHash dataSetHash = BigHash.createHashFromString(args[i + 2]);

                    tool.addFileFromProject(dataSetHash, fileHash);

                    // This argument has two parameters
                    i++;

                } else if (name.equals("-H")) {
                    BigHash dataSetHash = BigHash.createHashFromString(value);
                    tool.addDataSet(dataSetHash);
                } else if (name.equals("-s")) {
                    BigHash dataSetHash = BigHash.createHashFromString(value);
                    tool.addDataSet(dataSetHash);
                } else if (!validArgsSet.contains(name)) {
                    System.err.println("Unrecognized parameter: " + name);
                    printUsage(System.err);
                    System.exit(1);
                }
            }

            BigHash hash = tool.run();

            if (hash == null) {
                throw new NullPointerException("Tool didn't throw an exception, but didn't return a hash.");
            }

            System.out.println(hash);

        } catch (Exception e) {
            System.err.println("UNKNOWN ERROR -- " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage(System.err);
            System.exit(2);
        }
    }

    /**
     * 
     * @param out
     */
    private static void printUsage(PrintStream out) {
        out.println();
        out.println("DESCRIPTION");
        out.println("   Combines one or more files into a single data set.");
        out.println();
        out.println("AUTHENTICATION PARAMETERS");
        out.println("   -u      String        ProteomeCommons.org Tranche user name.");
        out.println("   -p      String        ProteomeCommons.org Tranche user's password.");
        out.println("OR");
        out.println("   -U      String        Path to user file.");
        out.println("   -P      String        User file's password.");
        out.println();
        out.println("Also, the tool will fail if at least one file is not provided. See PARAMETERS for more details.");
        out.println();
        out.println("PARAMETERS");
        out.println();
        out.println("These are optional unless noted otherwide with the caveat that at least one file must be included. Also, must provide authentication. See AUTHENTICATION PARAMETERS for more details.");
        out.println();
        out.println("   -D      String        Directory name (required). Must provide a name for directory to include files. E.g., my-data-set, my-data-set/, stuff, stuff/, etc., are all valid examples.");
        out.println("   -t      String        Title for new data set.");
        out.println("   -d      String        Description for new data set.");
        out.println("   -h      hash1 hash2   File hash (hash1) from an unencrypted data set (hash2). Both hashes must be included to help determine license information.");
        out.println("   -H      hash          Includes every file from an unecrypted data set. (This will " + (isExcludeLicensesFromDataSets ? "exclude" : "include") + " license files)");
        out.println("   -s      true/false    Prints out a summary of server activity at end. If having performance issues, please set this to true and send the developers all the output with a brief description of what you are doing.");
        out.println();
        out.println("RETURN CODES");
        out.println("   0: Exited normally");
        out.println("   1: Missing parameters or problem with parameters");
        out.println("   2: Unknown error (see standard error)");
        out.println();
    }

    /**
     * <p>Instantiate.</p>
     * @param cert Certificate of user who can write to network
     * @param key Key for user who can write to network
     * @param directoryName The name to be used for the directory holding all the files. (Title and description can be set using setters, and are different.)
     */
    public ArbitraryProjectBuildingTool(X509Certificate cert, PrivateKey key, String directoryName) {

        // Set the directory name to use, but make sure it end with a file separator first
        this.directoryName = directoryName.endsWith(File.separator) ? directoryName : directoryName + File.separator;

        this.cert = cert;
        this.key = key;
        this.serversToUse = new ArrayList();
        pfpSet = new DiskBackedProjectFilePartSet();
        projectFilePartsToAdd = new HashMap();
        individualFilesToAdd = new HashSet();
        filesFromProjectsToAdd = new HashMap();
        dataSetsToAdd = new HashSet();
        rtsListenerMap = new HashMap();
        rtsDiagnosticsLog = new ConnectionDiagnosticsLog("Server activity when aggregating tools");

        title = "Project packaged on " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp());
        description = "Default description for a project packaged by the ArbitraryProjectBuildingTool.";
    }

    /**
     * <p>Add a file that should be added to the project. This file should exist already on network.</p>
     * @param projectHash
     * @param pfp An existing ProjectFilePart. Could use chunkHash, but if already have this, can save a little time.
     * @return True if added, false otherwise.
     */
    public boolean addFileFromProject(BigHash projectHash, ProjectFilePart pfp) {
        synchronized (projectFilePartsToAdd) {
            projectFilePartsToAdd.put(pfp, projectHash);
            return true;
        }
    }

    /**
     * <p>Adds a file chunkHash to the project chunkHash.</p>
     * @param projectHash
     * @param fileHash
     * @return
     */
    public boolean addFileFromProject(BigHash projectHash, BigHash fileHash) {
        synchronized (filesFromProjectsToAdd) {
            filesFromProjectsToAdd.put(fileHash, projectHash);
        }
        return true;
    }

    /**
     * <p>Add a file that should be added to the project. This file should exist already on network and must not be part of a data set.</p>
     * @param chunkHash The BigHash for the file. Will go to network to get MetaData for file. If have ProjectFilePart, use instead to save time.
     * @return True if added, false otherwise.
     */
    public boolean addIndividualFile(BigHash hash) {
        synchronized (individualFilesToAdd) {
            individualFilesToAdd.add(hash);
        }
        return true;
    }

    /**
     * <p>Add an entire data. This tool will include all its files with other files you specify.</p>
     * <p>The data set should exist on the network.</p>
     * @param hash The BigHash for the data set.
     * @return
     */
    public boolean addDataSet(BigHash hash) {
        synchronized (this.dataSetsToAdd) {
            this.dataSetsToAdd.add(hash);
        }

        return true;
    }

    /**
     * <p>After setting all the files, run to create the project from existing files on the network.</p>
     * @return BigHash for the newly-created project.
     * @throws java.lang.Exception Any exception will stop the tool. This partially ensures the integrity of the project. Note that can succeed even if data chunks are missing. If ProjectFilePart objects were directly added, could succeed even with missing meta data.
     */
    public BigHash run() throws Exception {

        NetworkUtil.waitForStartup();

        Map<String, BigHash> parentLicenseMap = new HashMap();
        File tmpFile = null;

        try {
            /*
             * Step 1: Note we have some ProjectFilePart's (PFP's) added by user.
             */
            for (ProjectFilePart pfp : this.projectFilePartsToAdd.keySet()) {

                final BigHash projectHash = this.projectFilePartsToAdd.get(key);

                String relativeName = this.directoryName + pfp.getRelativeName();

                // Update relative name
                pfp.setRelativeName(relativeName);

                parentLicenseMap.put(relativeName, projectHash);

                if (paths.contains(relativeName)) {
                    throw new Exception("More than one file included with the same path, which is not permitted: " + relativeName);
                }
                paths.add(relativeName);

                // Add to collection
                this.pfpSet.add(pfp);
            }

            /*
             * Step 2: Gether PFP's for files from projects.
             */

            for (final BigHash fileHash : this.filesFromProjectsToAdd.keySet()) {
                final BigHash projectHash = this.filesFromProjectsToAdd.get(fileHash);

                ProjectFile pf = getProjectFile(projectHash);

                ProjectFilePart matchingPfp = null;

                // Find the PFP
                for (ProjectFilePart pfp : pf.getParts()) {
                    if (pfp.getHash().equals(fileHash)) {
                        matchingPfp = pfp;
                        break;
                    }
                }

                if (matchingPfp == null) {
                    throw new Exception("Could not find file<" + fileHash.toString().substring(0, 12) + "...> in project<" + projectHash.toString().substring(0, 12) + "...>. If you think this is an error, contact us.");
                }

                // Update relative name
                final String relativeName = this.directoryName + matchingPfp.getRelativeName();
                matchingPfp.setRelativeName(relativeName);

                parentLicenseMap.put(relativeName, projectHash);

                if (paths.contains(relativeName)) {
                    throw new Exception("More than one file included with the same path, which is not permitted: " + relativeName);
                }
                paths.add(relativeName);

                // Add to collection
                this.pfpSet.add(matchingPfp);
            }


            /*
             * Step 3: Add any individual files (not from projects), and add
             */
            for (BigHash h : this.individualFilesToAdd) {
                GetFileTool gft = new GetFileTool();
                gft.setHash(h);
                MetaData md = gft.getMetaData();

                // How to handle padding? For now, don't support already encrypted
                // files, so nothing. When support encrypted files on the network,
                // do same thing as AFT: use passphrase, get bytes!
                byte[] padding = new byte[0];

                // Create the ProjectFilePart
                final String relativeName = this.directoryName + md.getName();
                ProjectFilePart pfp = new ProjectFilePart(relativeName, h, padding);

                parentLicenseMap.put(relativeName, h);

                if (paths.contains(relativeName)) {
                    throw new Exception("More than one file included with the same path, which is not permitted: " + relativeName);
                }
                paths.add(relativeName);

                // Add to collection
                this.pfpSet.add(pfp);
            }

            /*
             * Step 4: Add entire data sets
             */
            for (final BigHash projectHash : dataSetsToAdd) {

                ProjectFile pf = getProjectFile(projectHash);

                // Find the PFP
                String directory = null;
                for (ProjectFilePart pfp : pf.getParts()) {

                    // Skip if not including licenses
                    if (isExcludeLicensesFromDataSets && pfp.getRelativeName().equals(LicenseUtil.RECOMMENDED_LICENSE_FILE_NAME)) {
                        continue;
                    }

                    // Find the directory name
                    if (directory == null) {
                        String path = pfp.getRelativeName();
                        if (path.contains("/")) {
                            directory = path.substring(0, path.indexOf("/"));
                        } else if (path.contains("\\")) {
                            directory = path.substring(0, path.indexOf("\\"));
                        }
                    }

                    // Update the path
                    final String relativeName = this.directoryName + pfp.getRelativeName();

                    // Adjust relative name so in same path
                    pfp.setRelativeName(relativeName);
                    this.pfpSet.add(pfp);

                    if (paths.contains(relativeName)) {
                        throw new Exception("More than one file included with the same path, which is not permitted: " + relativeName);
                    }
                    paths.add(relativeName);


                }

                // Only add if any files were included
                if (directory != null) {
                    final String relativeName = this.directoryName + directory;
                    parentLicenseMap.put(relativeName, projectHash);
                }
            }

            // Assert: at least one pfp
            if (pfpSet.size() == 0) {
                throw new RuntimeException("You did not specify any files or data sets to include. Tool requires at least one file.");
            }

            // Assert: no files with exact same path

            // Add: license file
            {
                tmpFile = TempFileUtil.createTempFileWithName(LicenseUtil.RECOMMENDED_LICENSE_FILE_NAME);

                LicenseUtil.buildLicenseMultiLicenseExplanationFile(tmpFile, parentLicenseMap);

                final BigHash licenseHash = new BigHash(tmpFile);

                final String relativePath = this.directoryName + tmpFile.getName();

                Map<String, String> properties = new HashMap<String, String>();
                properties.put(MetaData.PROP_NAME, tmpFile.getName());
                properties.put(MetaData.PROP_TIMESTAMP_UPLOADED, String.valueOf(TimeUtil.getTrancheTimestamp()));
                properties.put(MetaData.PROP_TIMESTAMP_FILE, String.valueOf(tmpFile.lastModified()));
                properties.put(MetaData.PROP_PATH_IN_DATA_SET, relativePath);

                // create a signature for the file
                Signature signature = new Signature(SecurityUtil.sign(tmpFile, this.key), SecurityUtil.getSignatureAlgorithm(this.key), this.cert);

                MetaData licenseMD = new MetaData();
                licenseMD.setIsProjectFile(false);
                ArrayList<FileEncoding> encodings = new ArrayList<FileEncoding>();
                encodings.add(new FileEncoding(FileEncoding.NONE, licenseHash));

                final ArrayList<MetaDataAnnotation> metaDataAnnotations = new ArrayList<MetaDataAnnotation>();

                licenseMD.addUploader(signature, encodings, properties, metaDataAnnotations);

                uploadDataChunks(tmpFile, licenseMD);
                uploadMetaData(licenseMD, licenseHash);

                // How to handle padding? For now, don't support already encrypted
                // files, so nothing. When support encrypted files on the network,
                // do same thing as AFT: use passphrase, get bytes!
                byte[] padding = new byte[0];

                // Create the ProjectFilePart
                ProjectFilePart pfp = new ProjectFilePart(relativePath, licenseHash, padding);

                // Add to collection
                this.pfpSet.add(pfp);
            }

            /**
             * Step 4. Create the project file and upload.
             */
            ProjectFile pf = new ProjectFile();
            pf.setName(getTitle());
            pf.setDescription(getDescription());
            pf.setParts(pfpSet);

            return uploadEverything(pf);
        } finally {
            
            if (isPrintServerPerformanceSummary()) {
                rtsDiagnosticsLog.printSummary(System.err);
            }

            // Close off any ProjectFile's in cache
            for (ProjectFileCacheItem cacheItem : this.projectFileCache) {
                IOUtil.safeClose(cacheItem.pf);
            }

            IOUtil.safeDelete(tmpFile);
        }
    }
    
    /**
     * 
     * @param host
     */
    private void lazyAttachRemoteTrancheServerListener(String host) {

        // Won't always be a host
        if (host == null) {
            return;
        }

        synchronized (rtsListenerMap) {
            RemoteTrancheServerPerformanceListener l = rtsListenerMap.get(host);

            if (l == null) {

                TrancheServer ts = ConnectionUtil.getHost(host);

                if (ts != null) {

                    // Note: If ConnectionUtil.getHost(String) returns RemoteTrancheServer only,
                    //       then the method should not return TrancheServer. Right now,
                    //       assume its not safe to typecast without checking. However,
                    //       not sure how to handle non-RemoteTrancheServer items
                    if (ts instanceof RemoteTrancheServer) {

                        RemoteTrancheServer rts = (RemoteTrancheServer) ts;

                        l = new RemoteTrancheServerPerformanceListener(rtsDiagnosticsLog, host, 1);
                        rts.addListener(l);
                        rtsListenerMap.put(host, l);
                    } else {
                        System.err.println("WARNING (in " + this.getClass().getName() + "): Tranche server not a RemoteTrancheServer. Please notify developers so can fix.");
                    }
                }

            }
        }
    }

    /**
     * <p>Does the following:</p>
     * <ul>
     *  <li>Uploads the license file data and meta data chunks.</p>
     *  <li>Uploads the ProjectFile data and meta data chunks.</p>
     * </ul>
     * @param pf
     * @return
     * @throws java.lang.Exception
     */
    private BigHash uploadEverything(ProjectFile pf) throws Exception {

        File tempProjectFile = null;

        try {
            tempProjectFile = TempFileUtil.createTemporaryFile(".pf");

            FileOutputStream fos = null;
            BufferedOutputStream bos = null;

            try {
                // write the data out to a file
                fos = new FileOutputStream(tempProjectFile);
                bos = new BufferedOutputStream(fos);
                ProjectFileUtil.write(pf, bos);
            } finally {
                IOUtil.safeClose(bos);
                IOUtil.safeClose(fos);
            }

            final ArrayList<MetaDataAnnotation> metaDataAnnotations = new ArrayList<MetaDataAnnotation>();

            // What's the new data set chunkHash?
            final BigHash projectHash = new BigHash(tempProjectFile);

            // make the file encoding objects
            ArrayList<FileEncoding> encodings = new ArrayList<FileEncoding>();
            encodings.add(new FileEncoding(FileEncoding.NONE, projectHash));

            // set up the meta data
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(MetaData.PROP_NAME, tempProjectFile.getName());
            properties.put(MetaData.PROP_TIMESTAMP_UPLOADED, String.valueOf(TimeUtil.getTrancheTimestamp()));
            properties.put(MetaData.PROP_TIMESTAMP_FILE, String.valueOf(tempProjectFile.lastModified()));

            // create a signature for the file
            Signature signature = new Signature(SecurityUtil.sign(tempProjectFile, this.key), SecurityUtil.getSignatureAlgorithm(this.key), this.cert);

            // ------------------------------------------------------------------------------------
            //  If add support to encrypted resulting ProjectFile, will need to allow user
            //  to decide whether to hide this information
            // ------------------------------------------------------------------------------------
            if (true) {
                properties.put(MetaData.PROP_DATA_SET_NAME, pf.getName());
                properties.put(MetaData.PROP_DATA_SET_DESCRIPTION, pf.getDescription());
            }
            // calculate final size and save to meta data
            long size = 0;
            for (ProjectFilePart pfp : pf.getParts()) {
                size += pfp.getPaddingAdjustedLength();
            }
            properties.put(MetaData.PROP_DATA_SET_SIZE, String.valueOf(size));
            properties.put(MetaData.PROP_DATA_SET_FILES, String.valueOf(pf.getParts().size()));

            MetaData md = new MetaData();
            md.setIsProjectFile(true);
            md.addUploader(signature, encodings, properties, metaDataAnnotations);

            uploadDataChunks(tempProjectFile, md);
            uploadMetaData(md, projectHash);

            return projectHash;

        } finally {
            IOUtil.safeDelete(tempProjectFile);
        }
    }

    /**
     * 
     * @param licenseMD
     */
    private void uploadMetaData(MetaData md, BigHash mdHash) throws Exception {
        uploadChunk(md.toByteArray(), mdHash, true);
    }

    /**
     * 
     * @param file
     */
    private void uploadDataChunks(File file, MetaData md) throws Exception {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));

            int MB = 1024 * 1024;

            byte[] chunk = IOUtil.readBytes(MB, bis);

            while (chunk.length > 0) {
                BigHash chunkHash = new BigHash(chunk);
                uploadChunk(chunk, chunkHash, false);
                md.addPart(chunkHash);
                chunk = IOUtil.readBytes(MB, bis);
            }
        } finally {
            IOUtil.safeClose(bis);
        }
    }

    /**
     * 
     * @return
     */
    private Collection<String> getHostsForDownloads() {
        return getHosts(null, false);
    }

    /**
     * 
     * @param hash
     * @return
     */
    private List<String> getHostsToUploadTo(BigHash hash) {
        return getHosts(hash, true);
    }

    /**
     * 
     * @param hash
     * @param isUpload
     * @return
     */
    private List<String> getHosts(BigHash hash, boolean isUpload) {
        List<String> hosts = new LinkedList();

        StatusTable table = NetworkUtil.getStatus().clone();

        for (StatusTableRow row : table.getRows()) {

            // Shouldn't be duplicates, but ensure
            if (hosts.contains(row.getHost())) {
                continue;
            }

            // If user specified hosts to use and this wasn't one, don't use
            if (serversToUse.size() > 0 && !serversToUse.contains(row.getHost())) {
                continue;
            }

            // Server must be core server and online
            if (!(row.isCore() && row.isOnline())) {
                continue;
            }

            // If this is an upload, server must be writable
            if (isUpload && !row.isWritable()) {
                continue;
            }

            // If user specified hash, must match hash span. Else, add unconditionally.
            if (hash != null) {
                for (HashSpan span : row.getTargetHashSpans()) {
                    if (span.contains(hash)) {
                        hosts.add(row.getHost());
                        break;
                    }
                }
            } else {
                hosts.add(row.getHost());
                break;
            }
        }

        return hosts;
    }

    /**
     * 
     * @param chunk
     * @param chunkHash
     * @param isMetaData
     */
    private void uploadChunk(byte[] chunk, BigHash hash, boolean isMetaData) throws Exception {

        final int requiredCopies = Integer.parseInt(ConfigureTranche.get(ConfigureTranche.PROP_REPLICATIONS));

        int copies = 0;

        List<String> hosts = getHostsToUploadTo(hash);

        if (hosts.size() < requiredCopies) {
            throw new Exception("Not enough hosts to upload " + (isMetaData ? "meta" : "") + " data chunk with sufficient copies. " + requiredCopies + " required, but " + copies + " hosts available: " + hash);
        }

        final int MAX_ATTEMPTS = 3;

        // Since we are going to verify sequentially, just upload sequentially. More important to keep
        // simple rather than optimizing performance since so little to upload and this tool will be
        // changed a lot over time
        for (String host : hosts) {
            
            lazyAttachRemoteTrancheServerListener(host);

            ATTEMPTS:
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

                TrancheServer ts = null;
                try {
                    
                    ts = ConnectionUtil.connectHost(host, true);
                    if (ts == null) {
                        throw new Exception("Could not connect to " + host);
                    }

                    if (isMetaData) {
                        // Assume might need to merge, so don't check for existance
                        PropagationReturnWrapper prw = IOUtil.setMetaData(ts, cert, key, true, hash, chunk);

                        // Throw any exceptions
                        for (PropagationExceptionWrapper pew : prw.getErrors()) {
                            System.err.println("A problem has occurred while setting meta data for server: "+host);
                            throw pew.exception;
                        }
                    } else {
                        if (!IOUtil.hasData(ts, hash)) {
                            PropagationReturnWrapper prw = IOUtil.setData(ts, cert, key, hash, chunk);

                            // Throw any exceptions
                            for (PropagationExceptionWrapper pew : prw.getErrors()) {
                                System.err.println("A problem has occurred while setting data for server: "+host);
                                throw pew.exception;
                            }
                        }
                    }

                    // Success!
                    copies++;
                    break ATTEMPTS;
                } catch (Exception e) {
                    System.err.println(e.getClass().getSimpleName() + " <attempt #" + attempt + " of " + MAX_ATTEMPTS + ">: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    ConnectionUtil.unlockConnection(host);
                }
            }
        }

        if (copies < requiredCopies) {
            throw new Exception("Did not upload enough copies of " + (isMetaData ? "meta" : "") + " data chunk. " + requiredCopies + " required, but " + copies + " succeeded for: " + hash);
        }

    }

    /**
     * <p>This title for the new project.</p>
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * <p>This title for the new project.</p>
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * <p>The description for the new proejct.</p>
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * <p>The description for the new proejct.</p>
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * <p>Add a server to use.</p>
     * <p>If no server urls added, will use core servers for network.</p>
     * <p>If servers added, will use those instead.</p>
     * <p>Uses same contract as AddFileTool.</p>
     * @param host
     */
    public void addServerToUse(String host) {
        this.serversToUse.add(host);
    }

    /**
     * <p>Returns a ProjectFile from cache or downloads (and adds to cache), then returns.</p>
     * <p>
     * @param projectHash
     * @return
     */
    private synchronized ProjectFile getProjectFile(BigHash projectHash) throws Exception {

        try {
            // Check cache first
            for (ProjectFileCacheItem pfci : projectFileCache) {
                if (pfci.hash.equals(projectHash)) {
                    return pfci.pf;
                }
            }

            ProjectFile pf = null;

            // Nope, download
            File f = null;
            try {
                f = TempFileUtil.createTemporaryFile(".pf");

                GetFileTool gft = new GetFileTool();
                gft.setServersToUse(getHostsForDownloads());
                gft.setHash(projectHash);
                gft.setSaveFile(f);
                GetFileToolReport gftr = gft.getFile();

                if (gftr.isFailed()) {
                    System.err.println("The download of project file <" + projectHash + "> failed with the following " + gftr.getFailureExceptions().size() + " error(s):");
                    for (PropagationExceptionWrapper pew : gftr.getFailureExceptions()) {
                        System.err.println("    * " + pew.exception.getMessage() + " <" + pew.host + ">: " + pew.exception.getMessage());
                    }
                    throw new Exception("Download of project file <" + projectHash + "> failed with " + gftr.getFailureExceptions().size() + " exception(s).");
                }

                BufferedInputStream bis = null;
                try {
                    bis = new BufferedInputStream(new FileInputStream(f));
                    pf = ProjectFileUtil.read(bis);
                } finally {
                    IOUtil.safeClose(bis);
                }
            } finally {
                IOUtil.safeDelete(f);
            }

            if (pf == null) {
                throw new AssertionFailedException("Project file is null, should have thrown an exception. Please file bug report.");
            }

            // Add to cache. If already at cache size limit, remove head (oldest)
            while (projectFileCache.size() >= MAX_CACHE_SIZE) {
                ProjectFileCacheItem tmp = projectFileCache.remove(0);
                IOUtil.safeClose(tmp.pf);
            }
            projectFileCache.add(new ProjectFileCacheItem(pf, projectHash));

            return pf;
        } catch (Exception e) {
            System.err.println("Problem with ProjectFile for data set <"+projectHash+">: " + e.getMessage());
            throw e;
        }
    }

    /**
     * <p>Used for queuing up ProjectFile's in cache.</p>
     */
    private class ProjectFileCacheItem {

        private final ProjectFile pf;
        private final BigHash hash;

        public ProjectFileCacheItem(ProjectFile pf, BigHash hash) {
            this.hash = hash;
            this.pf = pf;
        }
    }
}
