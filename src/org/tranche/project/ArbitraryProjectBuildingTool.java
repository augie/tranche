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
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.tranche.add.AddFileTool;
import org.tranche.annotations.Todo;
import org.tranche.annotations.TodoList;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.hash.DiskBackedBigHashSet;
import org.tranche.meta.MetaData;
import org.tranche.project.ProjectFile;
import org.tranche.project.ProjectFileUtil;
import org.tranche.project.DiskBackedProjectFilePartSet;
import org.tranche.project.ProjectFilePart;
import org.tranche.time.TimeUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.Text;

/**
 * <p>Create an arbitrary project from existing files on the network.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
@TodoList({
    @Todo(desc = "Need to be able to encrypt project", day = 15, month = 8, year = 2008, author = "Bryan"),
    @Todo(desc = "Need to be able to add to set a passphrase for each file hash added perchance need to decrypt", day = 15, month = 8, year = 2008, author = "Bryan"),
    @Todo(desc = "", day = 15, month = 8, year = 2008, author = "Bryan"),
    @Todo(desc = "Inefficient: same project file will be downloaded and recreated multiple times if multiple files from same project. Could cache or reorder files so can use same project file?", day = 15, month = 8, year = 2008, author = "Bryan")
})
public class ArbitraryProjectBuildingTool {

    private final DiskBackedProjectFilePartSet pfpSet;
    private final DiskBackedProjectFilePartSet projectFilePartsToAdd;
    private final DiskBackedBigHashSet fileHashesFromProjectsToAdd;
    private final DiskBackedBigHashSet projectHashesFromProjectsToAdd;
    private final DiskBackedBigHashSet individualFilesToAdd;
    private String title;
    private String description;
    private final X509Certificate cert;
    private final PrivateKey key;
    private List<String> serversToUse;
    private int serversToUploadTo = -1;
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

        title = "Project packaged on " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp());
        description = "Default description for a project packaged by the ArbitraryProjectBuildingTool.";
    }

    /**
     * <p>Add a file that should be added to the project. This file should exist already on network.</p>
     * @param pfp An existing ProjectFilePart. Could use hash, but if already have this, can save a little time.
     * @return True if added, false otherwise.
     */
    public boolean addFileFromProject(ProjectFilePart pfp) {
        return projectFilePartsToAdd.add(pfp);
    }

    /**
     * <p>Adds a file hash to the project hash.</p>
     * @param projectHash
     * @param fileHash
     * @return
     */
    public boolean addFileFromProject(BigHash projectHash, BigHash fileHash) {
        projectHashesFromProjectsToAdd.add(projectHash);
        fileHashesFromProjectsToAdd.add(fileHash);
        return true;
    }

    /**
     * <p>Add a file that should be added to the project. This file should exist already on network.</p>
     * @param hash The BigHash for the file. Will go to network to get MetaData for file. If have ProjectFilePart, use instead to save time.
     * @return True if added, false otherwise.
     */
    public boolean addIndividualFile(BigHash hash) {
        individualFilesToAdd.add(hash);
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
             * Step 1: Note we have some ProjectFilePart's (PFP's) added by user. Need
             * to modify so use this designated directory name for project.
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

            // Assert that size of files matches number of inputted files requested
            if (this.pfpSet.size() != (PFP_COUNT + SINGLE_FILE_COUNT + FILE_PROJECT_COUNT)) {
                throw new AssertionFailedException("The total size of files to include<" + this.pfpSet.size() + "> is not equal to PFP_COUNT<" + PFP_COUNT + "> + SINGLE_FILE_COUNT<" + SINGLE_FILE_COUNT + "> + FILE_PROJECT_COUNT<" + FILE_PROJECT_COUNT + ">. Please file bug report.");
            }

            /**
             * Step 4. Create the project file and upload.
             */
            ProjectFile pf = new ProjectFile();
            pf.setName(getTitle());
            pf.setDescription(getDescription());
            pf.setParts(pfpSet);

            // Upload file
            AddFileTool aft = new AddFileTool();
            aft.setUserCertificate(cert);
            aft.setUserPrivateKey(key);
            for (String host : this.serversToUse) {
                aft.addServerToUse(host);
            }

            // write the project file
            File tempProjectFile = null;
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            try {
                tempProjectFile = TempFileUtil.createTemporaryFile();
                // write the data out to a file
                fos = new FileOutputStream(tempProjectFile);
                bos = new BufferedOutputStream(fos);
                ProjectFileUtil.write(pf, bos);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                throw e;
            } finally {
                IOUtil.safeDelete(tempProjectFile);
                IOUtil.safeClose(bos);
                IOUtil.safeClose(fos);
            }

            return aft.execute().getHash();
        } finally {
            projectFilePartsToAdd.destroy();
            fileHashesFromProjectsToAdd.close();
            projectHashesFromProjectsToAdd.close();
            individualFilesToAdd.close();

            // Close off any ProjectFile's in cache
            for (ProjectFileCacheItem cacheItem : this.projectFileCache) {
                IOUtil.safeClose(cacheItem.pf);
            }
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
     * <p>Serialize a ProjectFile to disk.</p>
     * @param pf ProjectFile object to write
     * @param file File to hold the data
     * @throws java.lang.IOException
     */
    public static void writeProjectFile(ProjectFile pf, File file) throws IOException {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            ProjectFileUtil.write(pf, bos);
        } finally {
            IOUtil.safeClose(bos);
        }
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
     * <p>The number of replications. Uses same contract as AddFileTool.</p>
     * @return
     */
    public int getServersToUploadTo() {
        return serversToUploadTo;
    }

    /**
     * <p>The number of replications. Uses same contract as AddFileTool.</p>
     * @param numServersToUse
     */
    public void setServersToUploadTo(int numServersToUse) {
        this.serversToUploadTo = numServersToUse;
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
            gft.setServersToUse(serversToUse);
            gft.setHash(projectHash);
            gft.setSaveFile(f);
            gft.getFile();

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

        /**
         * 
         * @param pf
         * @param hash
         */
        public ProjectFileCacheItem(ProjectFile pf, BigHash hash) {
            this.hash = hash;
            this.pf = pf;
        }
    }
}
