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
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.tranche.ConfigureTranche;
import org.tranche.FileEncoding;
import org.tranche.TrancheServer;
import org.tranche.annotations.Todo;
import org.tranche.annotations.TodoList;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.get.GetFileTool;
import org.tranche.get.GetFileToolReport;
import org.tranche.hash.BigHash;
import org.tranche.hash.DiskBackedBigHashSet;
import org.tranche.hash.span.HashSpan;
import org.tranche.license.LicenseUtil;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTable;
import org.tranche.network.StatusTableRow;
import org.tranche.security.SecurityUtil;
import org.tranche.security.Signature;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.time.TimeUtil;
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

    private final DiskBackedProjectFilePartSet pfpSet;
    private final DiskBackedProjectFilePartSet projectFilePartsToAdd;
    private final DiskBackedBigHashSet fileHashesFromProjectsToAdd;
    private final DiskBackedBigHashSet projectHashesFromProjectsToAdd;
    private final DiskBackedBigHashSet individualFilesToAdd;
    private final DiskBackedBigHashSet dataSetsToAdd;
    private String title;
    private String description;
    private final X509Certificate cert;
    private final PrivateKey key;
    private List<String> serversToUse;
    private final String directoryName;
    private List<ProjectFileCacheItem> projectFileCache = new LinkedList();
    private static final int MAX_CACHE_SIZE = 20;

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
        projectFilePartsToAdd = new DiskBackedProjectFilePartSet();
        individualFilesToAdd = new DiskBackedBigHashSet();
        fileHashesFromProjectsToAdd = new DiskBackedBigHashSet();
        projectHashesFromProjectsToAdd = new DiskBackedBigHashSet();
        dataSetsToAdd = new DiskBackedBigHashSet();

        title = "Project packaged on " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp());
        description = "Default description for a project packaged by the ArbitraryProjectBuildingTool.";
    }

    /**
     * <p>Add a file that should be added to the project. This file should exist already on network.</p>
     * @param pfp An existing ProjectFilePart. Could use chunkHash, but if already have this, can save a little time.
     * @return True if added, false otherwise.
     */
    public boolean addFileFromProject(ProjectFilePart pfp) {
        synchronized (projectFilePartsToAdd) {
            return projectFilePartsToAdd.add(pfp);
        }
    }

    /**
     * <p>Adds a file chunkHash to the project chunkHash.</p>
     * @param projectHash
     * @param fileHash
     * @return
     */
    public boolean addFileFromProject(BigHash projectHash, BigHash fileHash) {
        synchronized (projectHashesFromProjectsToAdd) {
            projectHashesFromProjectsToAdd.add(projectHash);
        }
        synchronized (fileHashesFromProjectsToAdd) {
            fileHashesFromProjectsToAdd.add(fileHash);
        }
        return true;
    }

    /**
     * <p>Add a file that should be added to the project. This file should exist already on network.</p>
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
        synchronized(this.dataSetsToAdd) {
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

        final int HASH_BATCH_SIZE = 100;

        // Used later for an assert
        final int PFP_COUNT = this.projectFilePartsToAdd.size();
        final int SINGLE_FILE_COUNT = this.individualFilesToAdd.size();
        final int FILE_PROJECT_COUNT = this.fileHashesFromProjectsToAdd.size();

        try {
            /*
             * Step 1: Note we have some ProjectFilePart's (PFP's) added by user.
             */
            for (ProjectFilePart pfp : this.projectFilePartsToAdd) {
                // Update relative name
                pfp.setRelativeName(this.directoryName + pfp.getRelativeName());

                // Add to collection
                this.pfpSet.add(pfp);
            }

            /*
             * Step 2: Gether PFP's for files from projects.
             */
            // Assert collections are of same size.
            if (projectHashesFromProjectsToAdd.size() != fileHashesFromProjectsToAdd.size()) {
                throw new AssertionFailedException("Sizes of projectHashesFromProjects<" + projectHashesFromProjectsToAdd.size() + "> and fileHashesFromProjects<" + fileHashesFromProjectsToAdd.size() + "> are not equal. Please file bug report.");
            }

            for (int i = 0; i < this.projectHashesFromProjectsToAdd.size(); i += HASH_BATCH_SIZE) {
                List<BigHash> fileHashBatch = this.fileHashesFromProjectsToAdd.get(i, HASH_BATCH_SIZE);
                List<BigHash> projectHashBatch = this.projectHashesFromProjectsToAdd.get(i, HASH_BATCH_SIZE);

                // Assert batches are of same size.
                if (projectHashBatch.size() != fileHashBatch.size()) {
                    throw new AssertionFailedException("Sizes of projectHashBatch<" + projectHashBatch.size() + "> and fileHashBatch<" + fileHashBatch.size() + "> are not equal. Please file bug report.");
                }

                for (int j = 0; j < projectHashBatch.size(); j++) {
                    final BigHash projectHash = projectHashBatch.get(j);
                    final BigHash fileHash = fileHashBatch.get(j);

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
                    matchingPfp.setRelativeName(this.directoryName + matchingPfp.getRelativeName());

                    // Add to collection
                    this.pfpSet.add(matchingPfp);
                }
            }

            /*
             * Step 3: Add any individual files (not from projects), and add
             */
            for (int i = 0; i < this.individualFilesToAdd.size(); i += HASH_BATCH_SIZE) {
                List<BigHash> fileHashBatch = this.individualFilesToAdd.get(i, HASH_BATCH_SIZE);

                for (BigHash h : fileHashBatch) {
                    GetFileTool gft = new GetFileTool();
                    gft.setHash(h);
                    MetaData md = gft.getMetaData();

                    // How to handle padding? For now, don't support already encrypted
                    // files, so nothing. When support encrypted files on the network,
                    // do same thing as AFT: use passphrase, get bytes!
                    byte[] padding = new byte[0];

                    // Create the ProjectFilePart
                    ProjectFilePart pfp = new ProjectFilePart(this.directoryName + md.getName(), h, padding);

                    // Add to collection
                    this.pfpSet.add(pfp);
                }
            }

            /*
             * Step 4: Add entire data sets
             */

            for (int i = 0; i < this.dataSetsToAdd.size(); i++) {
                List<BigHash> dataSetBatch = this.dataSetsToAdd.get(i, HASH_BATCH_SIZE);

                for (final BigHash projectHash : dataSetBatch) {

                    ProjectFile pf = getProjectFile(projectHash);


                    // Find the PFP
                    for (ProjectFilePart pfp : pf.getParts()) {
                        if (!pfp.getRelativeName().equals(LicenseUtil.RECOMMENDED_LICENSE_FILE_NAME)) {
                            this.pfpSet.add(pfp);
                        }
                    }
                }
            }
            
            // Assert: at least one pfp
            if (pfpSet.size() == 0) {
                throw new RuntimeException("You did not specify any files or data sets to include. Tool requires at least one file.");
            }
            
            // Assert: no files with exact same path

            /**
             * Step 4. Create the project file and upload.
             */
            ProjectFile pf = new ProjectFile();
            pf.setName(getTitle());
            pf.setDescription(getDescription());
            pf.setParts(pfpSet);

            return uploadEverything(pf);
        } finally {
            projectFilePartsToAdd.destroy();
            fileHashesFromProjectsToAdd.close();
            projectHashesFromProjectsToAdd.close();
            individualFilesToAdd.close();
            dataSetsToAdd.close();

            // Close off any ProjectFile's in cache
            for (ProjectFileCacheItem cacheItem : this.projectFileCache) {
                IOUtil.safeClose(cacheItem.pf);
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
     * @param md
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
                            throw pew.exception;
                        }
                    } else {
                        if (!IOUtil.hasData(ts, hash)) {
                            PropagationReturnWrapper prw = IOUtil.setData(ts, cert, key, hash, chunk);


                            // Throw any exceptions
                            for (PropagationExceptionWrapper pew : prw.getErrors()) {
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
