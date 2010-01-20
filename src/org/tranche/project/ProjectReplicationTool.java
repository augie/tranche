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

import org.tranche.configuration.Configuration;
import org.tranche.hash.BigHash;
import org.tranche.get.GetFileTool;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import org.tranche.TrancheServer;
import org.tranche.hash.span.HashSpan;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataUtil;
import org.tranche.network.ConnectionUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>Replicates chunks for one or more projects to select servers.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ProjectReplicationTool implements ProjectReplicationToolListener {

    private X509Certificate cert;
    private PrivateKey key;
    private BigHash projectHash = null;
    private String passphrase = null;
    private Collection<String> serversToRead;
    private Collection<String> serversToWrite;
    private final Set<ProjectReplicationToolListener> listeners;
    private int numberRequiredReplications = 3;
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     * Consumer/Producer multithreading variables
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     */
    private final int QUEUE_CAPACITY = 1000;
    private final int NUMBER_REPLICATION_DAEMONS = 5;
    private ArrayBlockingQueue<FileToReplicate> filesToReplicate;
    private FileReplicationDaemon[] fileReplicationDaemon = new FileReplicationDaemon[NUMBER_REPLICATION_DAEMONS];
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     * Counters
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     */
    private long sizeOfProject = 0;
    private long sizeAlreadyHandled = 0;
    private long failedDataChunkCount = 0,  failedMetaDataChunkCount = 0;
    private long skippedDataChunkCount = 0,  skippedMetaDataChunkCount = 0;
    private long successDataChunkCount = 0,  successMetaDataChunkCount = 0;

    /**
     * 
     * @param cert
     * @param key
     */
    public ProjectReplicationTool(X509Certificate cert, PrivateKey key) {
        this(cert, key, null, null);
    }

    /**
     * 
     * @param cert
     * @param key
     * @param serversToRead
     * @param serversToWrite
     */
    public ProjectReplicationTool(X509Certificate cert, PrivateKey key, Collection<String> serversToRead, Collection<String> serversToWrite) {
        this.cert = cert;
        this.key = key;
        if (serversToRead != null) {
            this.serversToRead = serversToRead;
        }
        if (serversToWrite != null) {
            this.serversToWrite = serversToWrite;
        }
        listeners = new HashSet();
    }

    /**
     * <p>Start replication.</p>
     * @throws java.lang.Exception
     */
    public void execute() throws Exception {

        // Reset all counters perchance tool is reused
        _reset();

        // If not enough servers to write to to get desired reps, bring reps count down
        if (this.getServersToWrite().size() < this.numberRequiredReplications) {
            System.out.println("Setting required reps from " + this.numberRequiredReplications + " to " + this.getServersToWrite().size() + ": not enough servers to write to.");
            this.numberRequiredReplications = this.getServersToWrite().size();
        }

        // Fire off the daemons
        for (int i = 0; i < fileReplicationDaemon.length; i++) {
            fileReplicationDaemon[i].start();
        }

        // Fire started
        this.fireReplicationStarted(ProjectReplicationTool.this);

        try {
            GetFileTool gft = new GetFileTool();
            if (serversToRead != null) {
                gft.setServersToUse(serversToRead);
                gft.setUseUnspecifiedServers(false);
            }
            gft.setHash(getHash());
            if (getPassphrase() != null && !getPassphrase().trim().equals("")) {
                gft.setPassphrase(getPassphrase());
            }

            MetaData md = gft.getMetaData();

            if (md.isProjectFile()) {
                replicateProject(md);
            } else {
                // Calculate size
                calculateSizeOfFile(md);
                replicateSingleFile(getHash(), md);
            }

            // If there were failures, throw an exception
            if (this.failedDataChunkCount > 0 || this.failedMetaDataChunkCount > 0) {
                throw new Exception("Some of chunks failed (data failed: " + this.failedDataChunkCount + ", meta failed: " + this.failedMetaDataChunkCount + ")");
            }

            // Request daemons to stop when queue is empty
            for (int i = 0; i < fileReplicationDaemon.length; i++) {
                fileReplicationDaemon[i].setRun(false);
            }

            // Block until daemons are finished
            for (int i = 0; i < fileReplicationDaemon.length; i++) {
                fileReplicationDaemon[i].join();
            }

            this.fireReplicationFinished();
        } catch (Exception ex) {
            this.fireReplicationFailed();
            throw ex;
        } finally {
            // Make sure daemons where requested to stop when queue is empty
            for (int i = 0; i < fileReplicationDaemon.length; i++) {
                fileReplicationDaemon[i].setRun(false);
            }
        }
    }

    /**
     * <p>Internal method to reset counter variables perchance tool is reused.</p>
     */
    private void _reset() {
        sizeOfProject = 0;
        sizeAlreadyHandled = 0;
        failedDataChunkCount = 0;
        failedMetaDataChunkCount = 0;
        skippedDataChunkCount = 0;
        skippedMetaDataChunkCount = 0;
        successDataChunkCount = 0;
        successMetaDataChunkCount = 0;

        // Create threading vars
        filesToReplicate = new ArrayBlockingQueue(QUEUE_CAPACITY);

        // Fire off the daemons
        for (int i = 0; i < fileReplicationDaemon.length; i++) {
            fileReplicationDaemon[i] = new FileReplicationDaemon(i, ProjectReplicationTool.this, filesToReplicate);
        }
    }

    /**
     * <p>Used to replicate a single-file upload OR a file within a project. When replicating project, this will be called many times.</p>
     * @param fileHash
     * @param fileMetaData
     * @throws java.lang.Exception
     */
    protected void replicateSingleFile(BigHash fileHash, MetaData fileMetaData) throws Exception {

        boolean isProblem = false;

        // Start by replicating meta data
        if (!this.injectMetaDataChunk(fileHash)) {
            isProblem = true;
        }

        for (BigHash h : fileMetaData.getParts()) {
            // Replicate next data chunk
            if (!this.injectDataChunk(h)) {
                isProblem = true;
            }
        }

        // Fire off appropriate listener for file
        if (isProblem) {
            this.fireFileFailed(fileHash);
        } else {
            this.fireFileFinished(fileHash);
        }
    }

    /**
     * <p>Helper method to replicate the project.</p>
     * @param projectMetaData
     * @throws java.lang.Exception
     */
    protected void replicateProject(MetaData projectMetaData) throws Exception {

        File projectFile = null;

        try {

            projectFile = TempFileUtil.createTemporaryFile(".pf");

            GetFileTool gft = new GetFileTool();
            if (serversToRead != null) {
                gft.setServersToUse(serversToRead);
                gft.setUseUnspecifiedServers(false);
            }
            gft.setHash(this.getHash());
            if (this.getPassphrase() != null && !this.getPassphrase().trim().equals("")) {
                gft.setPassphrase(this.getPassphrase());
            }

            gft.setSaveFile(projectFile);
            gft.getFile();

            if (!projectFile.exists()) {
                throw new Exception("Project file was not downloaded.");
            }

            ProjectFile pf = null;

            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(projectFile));
                pf = ProjectFileUtil.read(bis);
            } finally {
                if (bis != null) {
                    IOUtil.safeClose(bis);
                }
            }

            if (pf == null) {
                throw new Exception("Failed to parse project file.");
            }

            // Calculate size
            calculateSizeOfProject(projectFile.length(), pf);

            // Start the injection by injecting project meta data
            injectMetaDataChunk(this.getHash());

            // Inject the project file's data chunks
            for (BigHash h : projectMetaData.getParts()) {
                injectDataChunk(h);
            }

            // For every file in project
            for (ProjectFilePart pfp : pf.getParts()) {

                // Queue up file so can handle multiple files concurrently
                FileToReplicate fileToReplicate = new FileToReplicate(pfp.getHash());

                boolean added = false;
                // Repeatedly try to add until successful
                while (!added) {
                    synchronized (this.filesToReplicate) {
                        added = this.filesToReplicate.offer(fileToReplicate);
                    }
                    if (!added) {
                        Thread.yield();
                        Thread.sleep(50);
                    }
                }

                boolean isProblem = false;

                // Inject the file's meta data
                if (!injectMetaDataChunk(pfp.getHash())) {
                    isProblem = true;
                }

                GetFileTool fileGFT = new GetFileTool();
                if (serversToRead != null) {
                    fileGFT.setServersToUse(serversToRead);
                    fileGFT.setUseUnspecifiedServers(false);
                }
                fileGFT.setHash(pfp.getHash());
                if (this.getPassphrase() != null && !this.getPassphrase().trim().equals("")) {
                    fileGFT.setPassphrase(this.getPassphrase());
                }

                MetaData md = fileGFT.getMetaData();
                for (BigHash h : md.getParts()) {
                    // Inject the file's next data chunk
                    if (!injectDataChunk(h)) {
                        isProblem = true;
                    }
                }

                // Fire off appropriate listener for file
                if (isProblem) {
                    this.fireFileFailed(pfp.getHash());
                } else {
                    this.fireFileFinished(pfp.getHash());
                }
            }

        } finally {
            IOUtil.safeDelete(projectFile);
        }
    }

    /**
     * <p>Helper method to calculate the size of file.</p>
     * @param md
     * @throws java.lang.Exception
     */
    private void calculateSizeOfFile(MetaData md) throws Exception {
        // Calculate size of all data chunks
        for (BigHash dataHash : md.getParts()) {
            this.sizeOfProject += dataHash.getLength();
        }
    }

    /**
     * <p>Helper method to calculate the size of project.</p>
     * @param projectFileSize
     * @param pf
     * @throws java.lang.Exception
     */
    private void calculateSizeOfProject(long projectFileSize, ProjectFile pf) throws Exception {

        // Calculate size of all data chunks. 
        for (ProjectFilePart pfp : pf.getParts()) {
            GetFileTool gft = new GetFileTool();
            if (serversToRead != null) {
                gft.setServersToUse(serversToRead);
                gft.setUseUnspecifiedServers(false);
            }
            gft.setHash(pfp.getHash());
            if (this.getPassphrase() != null && !this.getPassphrase().trim().equals("")) {
                gft.setPassphrase(this.getPassphrase());
            }

            MetaData md = gft.getMetaData();
            for (BigHash dataHash : md.getParts()) {
                this.sizeOfProject += dataHash.getLength();
            }
        }

        // Add last so doesn't appear like finished before it is
        this.sizeOfProject += projectFileSize;
    }

    /**
     * <p>Helper method to inject data chunk.</p>
     * @param h
     */
    private boolean injectDataChunk(BigHash h) {
        return injectChunk(h, false);
    }

    /**
     * <p>Helper method to inject meta data chunk.</p>
     * @param h
     */
    private boolean injectMetaDataChunk(BigHash h) {
        return injectChunk(h, true);
    }

    /**
     * <p>Helper method to inject chunk.</p>
     * @param h
     * @param isMetaData
     */
    private boolean injectChunk(final BigHash chunkHash, final boolean isMetaData) {
        try {
            // First, need to get a copy of data. If can't, failed
            byte[] bytes = null;

            for (String host : getServersToWrite()) {
                TrancheServer ts = null;
                try {
                    ts = ConnectionUtil.connectHost(host, true);

                    /*
                     * If meta data...
                     */
                    if (isMetaData) {
                        bytes = ((byte[][]) IOUtil.getMetaData(ts, chunkHash, false).getReturnValueObject())[0];

                        // Verify. If fails, discard
                        InputStream is = null;
                        try {
                            is = new ByteArrayInputStream(bytes);
                            MetaData md = MetaDataUtil.read(is);

                            if (md == null) {
                                throw new Exception("Meta data is null, should have thrown exception.");
                            }
                        } catch (Exception bad) {
                            bytes = null;
                        } finally {
                            IOUtil.safeClose(is);
                        }
                    } /*
                     * If data...
                     */ else {
                        bytes = ((byte[][]) IOUtil.getData(ts, chunkHash, false).getReturnValueObject())[0];

                        // Verify the data. If not correct, discard.
                        BigHash verifyHash = new BigHash(bytes);
                        if (!verifyHash.equals(chunkHash)) {
                            bytes = null;
                        }
                    }

                    if (bytes != null) {
                        break;
                    }

                } catch (Exception ex) {
                    /* nope */
                } finally {
                    ConnectionUtil.unlockConnection(host);
                    IOUtil.safeClose(ts);
                }
            }

            // If bytes not found, fire correct fail and bail
            if (bytes == null) {
                throw new Exception("Bytes not found");
            }

            // Pick some servers to inject to.
            List<String> serversInHashSpan = new ArrayList();
            List<String> serversOutOfHashSpan = new ArrayList();

            int copiesFoundOnWriteServers = 0;

            for (String url : this.getServersToWrite()) {
                TrancheServer ts = null;
                try {
                    ts = ConnectionUtil.connectURL(url, false);

                    /*
                     * Is meta data...
                     */
                    if (isMetaData) {
                        if (IOUtil.hasMetaData(ts, chunkHash)) {
                            copiesFoundOnWriteServers++;
                        } else {
                            if (isInHashSpan(chunkHash, url)) {
                                serversInHashSpan.add(url);
                            } else {
                                serversOutOfHashSpan.add(url);
                            }
                        }
                    } /*
                     * Is data...
                     */ else {
                        if (IOUtil.hasData(ts, chunkHash)) {
                            copiesFoundOnWriteServers++;
                        } else {
                            if (isInHashSpan(chunkHash, url)) {
                                serversInHashSpan.add(url);
                            } else {
                                serversOutOfHashSpan.add(url);
                            }
                        }
                    }

                } catch (Exception ex) {
                    /* nope, try other servers */
                } finally {
                    IOUtil.safeClose(ts);
                }
            } // Find servers without chunks

            // If found enough copies, skip
            if (copiesFoundOnWriteServers >= this.getNumberRequiredReplications()) {
                if (isMetaData) {
                    this.fireMetaDataChunkSkipped(chunkHash);
                } else {
                    this.fireDataChunkSkipped(chunkHash);
                }
                return true;
            }

            // Build list of servers with hash spans first
            List<String> serversToInjectTo = new ArrayList();
            Collections.shuffle(serversInHashSpan);
            Collections.shuffle(serversOutOfHashSpan);
            serversToInjectTo.addAll(serversInHashSpan);
            serversToInjectTo.addAll(serversOutOfHashSpan);

            // We'll increment this until we get needed copies
            int totalCopies = copiesFoundOnWriteServers;

            for (String url : serversToInjectTo) {

                TrancheServer ts = null;
                try {
                    ts = ConnectionUtil.connectURL(url, false);

                    if (isMetaData) {
                        IOUtil.setMetaData(ts, cert, key, false, chunkHash, bytes);
                    } else {
                        IOUtil.setData(ts, cert, key, chunkHash, bytes);
                    }

                    totalCopies++;

                    if (totalCopies >= this.numberRequiredReplications) {
                        break;
                    }
                } catch (Exception ex) {
                    /* nope, continue */
                } finally {
                    IOUtil.safeClose(ts);
                }
            }

            if (totalCopies >= this.numberRequiredReplications) {
                if (isMetaData) {
                    this.fireMetaDataChunkReplicated(chunkHash);
                } else {
                    this.fireDataChunkReplicated(chunkHash);
                }
            } else {
                throw new Exception("Insufficient replications. Need total of " + this.numberRequiredReplications + ", ended up with " + totalCopies);
            }

            return true;
        } catch (Exception ex) {
            if (isMetaData) {
                this.fireMetaDataChunkFailed(chunkHash);
            } else {
                this.fireDataChunkFailed(chunkHash);
            }
            return false;
        }
    } // injectChunk
    /**
     * 
     */
    private Map<String, Configuration> serverConfigurationCache = null;

    /**
     * <p>Get the Configuration object for a server.</p>
     * @param url
     * @return
     * @throws java.lang.Exception
     */
    private Configuration getServerConfiguration(String url) throws Exception {
        if (serverConfigurationCache == null) {
            serverConfigurationCache = new HashMap<String, Configuration>();
        }

        Configuration c = serverConfigurationCache.get(url);

        // If configuration not found, get and cache
        if (c == null) {
            TrancheServer ts = null;
            try {
                ts = ConnectionUtil.connectURL(url, false);
                c = IOUtil.getConfiguration(ts, cert, key);
                serverConfigurationCache.put(url, c);
            } finally {
                IOUtil.safeClose(ts);
            }
        }

        return c;
    }

    /**
     * <p>Helper method to check whether chunk belongs in hash span for server.</p>
     * @param h
     * @param url
     * @return
     */
    private boolean isInHashSpan(BigHash h, String url) throws Exception {
        Configuration c = getServerConfiguration(url);

        for (HashSpan hs : c.getHashSpans()) {
            if (hs.contains(h)) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>Get the certificate used by the tool.</p>
     * @return
     */
    public X509Certificate getCert() {
        return cert;
    }

    /**
     * <p>Set the certificate used by the tool.</p>
     * @param cert
     */
    public void setCert(X509Certificate cert) {
        this.cert = cert;
    }

    /**
     * <p>Get the private key used by the tool.</p>
     * @return
     */
    public PrivateKey getKey() {
        return key;
    }

    /**
     * <p>Set the private key used by the tool.</p>
     * @param key
     */
    public void setKey(PrivateKey key) {
        this.key = key;
    }

    /**
     * <p>Get the project hash used by the tool.</p>
     * @return
     */
    public BigHash getHash() {
        return projectHash;
    }

    /**
     * <p>Set the project hash used by the tool.</p>
     * @param hash
     */
    public void setHash(BigHash hash) {
        this.projectHash = hash;
    }

    /**
     * <p>Get passphrase used by the tool.</p>
     * @return
     */
    public String getPassphrase() {
        return passphrase;
    }

    /**
     * <p>Set passphrase used by the tool.</p>
     * @param passphrase
     */
    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    /**
     * <p>Add a listener to the tool.</p>
     * @param l
     * @return
     */
    public boolean addProjectReplicationToolListener(ProjectReplicationToolListener l) {
        return this.listeners.add(l);
    }

    /**
     * <p>Clear out listeners used by the tool.</p>
     */
    public void clearProjectReplicationToolListeners() {
        this.listeners.clear();
    }

    /**
     * <p>Event fired when replication started.</p>
     * @param tool
     */
    public void fireReplicationStarted(ProjectReplicationTool tool) {
        for (ProjectReplicationToolListener l : this.listeners) {
            l.fireReplicationStarted(tool);
        }
    }

    /**
     * <p>Event fired when replication failed.</p>
     */
    public void fireReplicationFailed() {
        for (ProjectReplicationToolListener l : this.listeners) {
            l.fireReplicationFailed();
        }
    }

    /**
     * <p>Event fired when replication finished.</p>
     */
    public void fireReplicationFinished() {
        for (ProjectReplicationToolListener l : this.listeners) {
            l.fireReplicationFinished();
        }
    }

    /**
     * <p>Event fired when data chunk replicated.</p>
     * @param h
     */
    public void fireDataChunkReplicated(BigHash h) {

        successDataChunkCount++;

        for (ProjectReplicationToolListener l : this.listeners) {
            l.fireDataChunkReplicated(h);
        }

        // Increment the size
        synchronized (this.listeners) {
            this.sizeAlreadyHandled += h.getLength();
        }
    }

    /**
     * <p>Event fired when meta data chunk replicated.</p>
     * @param h
     */
    public void fireMetaDataChunkReplicated(BigHash h) {

        successMetaDataChunkCount++;

        for (ProjectReplicationToolListener l : this.listeners) {
            l.fireMetaDataChunkReplicated(h);
        }
    }

    /**
     * <p>Event fired when data chunk skipped.</p>
     * @param h
     */
    public void fireDataChunkSkipped(BigHash h) {

        skippedDataChunkCount++;

        for (ProjectReplicationToolListener l : this.listeners) {
            l.fireDataChunkSkipped(h);
        }

        // Increment the size
        synchronized (this.listeners) {
            this.sizeAlreadyHandled += h.getLength();
        }
    }

    /**
     * <p>Event fired when meta data chunk skipped.</p>
     * @param h
     */
    public void fireMetaDataChunkSkipped(BigHash h) {

        skippedMetaDataChunkCount++;

        for (ProjectReplicationToolListener l : this.listeners) {
            l.fireMetaDataChunkSkipped(h);
        }
    }

    /**
     * <p>Event fired when data chunk failed.</p>
     * @param h
     */
    public void fireDataChunkFailed(BigHash h) {

        failedDataChunkCount++;

        for (ProjectReplicationToolListener l : this.listeners) {
            l.fireDataChunkFailed(h);
        }

        // Increment the size
        synchronized (this.listeners) {
            this.sizeAlreadyHandled += h.getLength();
        }
    }

    /**
     * <p>Event fired when meta data chunk failed.</p>
     * @param h
     */
    public void fireMetaDataChunkFailed(BigHash h) {

        failedMetaDataChunkCount++;

        for (ProjectReplicationToolListener l : this.listeners) {
            l.fireMetaDataChunkFailed(h);
        }
    }

    /**
     * <p>Event fired when file failed.</p>
     * @param h
     */
    public void fireFileFailed(BigHash h) {
        for (ProjectReplicationToolListener l : this.listeners) {
            l.fireFileFailed(h);
        }
    }

    /**
     * <p>Event fired when file finished.</p>
     * @param h
     */
    public void fireFileFinished(BigHash h) {
        for (ProjectReplicationToolListener l : this.listeners) {
            l.fireFileFinished(h);
        }
    }

    /**
     * <p>Get the size of the project being replicated.</p>
     * @return
     */
    public long getSizeOfProject() {
        return sizeOfProject;
    }

    /**
     * <p>Get the size of data already replicated.</p>
     * @return
     */
    public long getSizeAlreadyHandled() {
        return sizeAlreadyHandled;
    }

    /**
     * <p>Get the failed data chunk count.</p>
     * @return
     */
    public long getFailedDataChunkCount() {
        return failedDataChunkCount;
    }

    /**
     * <p>Get the failed meta data chunk count.</p>
     * @return
     */
    public long getFailedMetaDataChunkCount() {
        return failedMetaDataChunkCount;
    }

    /**
     * <p>Get the skipped data chunk count.</p>
     * @return
     */
    public long getSkippedDataChunkCount() {
        return skippedDataChunkCount;
    }

    /**
     * <p>Get the skipped meta data chunk count.</p>
     * @return
     */
    public long getSkippedMetaDataChunkCount() {
        return skippedMetaDataChunkCount;
    }

    /**
     * <p>Get the count of successful data chunk replications.</p>
     * @return
     */
    public long getSuccessDataChunkCount() {
        return successDataChunkCount;
    }

    /**
     * <p>Get the count of successful meta data chunk replications.</p>
     * @return
     */
    public long getSuccessMetaDataChunkCount() {
        return successMetaDataChunkCount;
    }

    /**
     * <p>Get the required replication count.</p>
     * @return
     */
    public int getNumberRequiredReplications() {
        return numberRequiredReplications;
    }

    /**
     * <p>Set the required replication count.</p>
     * @param numberRequiredReplications
     */
    public void setNumberRequiredReplications(int numberRequiredReplications) {
        this.numberRequiredReplications = numberRequiredReplications;
    }

    /**
     * <p>Get the servers from which to read.</p>
     * @return
     */
    public Collection<String> getServersToRead() {
        return serversToRead;
    }

    /**
     * <p>Set the servers from which to read.</p>
     * @param serversToRead
     */
    public void setServersToRead(List<String> serversToRead) {
        this.serversToRead = serversToRead;
    }

    /**
     * <p>Get the servers to which to write.</p>
     * @return
     */
    public Collection<String> getServersToWrite() {
        return serversToWrite;
    }

    /**
     * <p>Set the servers to which to write.</p>
     * @param serversToWrite
     */
    public void setServersToWrite(List<String> serversToWrite) {
        this.serversToWrite = serversToWrite;
    }
}

/**
 * <p>Encapsulate a single file that has some work to do.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
class FileToReplicate {

    final private BigHash hash;

    /**
     * 
     * @param hash
     */
    public FileToReplicate(BigHash hash) {
        this.hash = hash;
    }

    /**
     * <p>Get the hash for the file to replicate.</p>
     * @return
     */
    public BigHash getHash() {
        return hash;
    }
}

/**
 * <p>Consumes ChunkToReplicate objects and processes them.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
class FileReplicationDaemon extends Thread {

    private final ProjectReplicationTool replicationTool;
    private final ArrayBlockingQueue<FileToReplicate> filesToReplicateQueue;
    private boolean run = true;
    private final int id;

    /**
     * 
     * @param id
     * @param replicationTool
     * @param filesToReplicateQueue
     */
    public FileReplicationDaemon(final int id, ProjectReplicationTool replicationTool, ArrayBlockingQueue<FileToReplicate> filesToReplicateQueue) {
        super("File injection daemon");
        this.id = id;
        this.replicationTool = replicationTool;
        this.filesToReplicateQueue = filesToReplicateQueue;
        setDaemon(true);
        setPriority(Thread.MIN_PRIORITY);
    }

    @Override()
    public void run() {
        // Continue until flagged done and empty
        while (run || filesToReplicateQueue.size() > 0) {
            FileToReplicate fileToReplicate = null;
            synchronized (filesToReplicateQueue) {
                fileToReplicate = filesToReplicateQueue.poll();
            }

            if (fileToReplicate == null) {
                try {
                    Thread.sleep(50);
                } catch (Exception nope) { /* whatever */ }
                continue;
            }

            try {
                // Need the file meta data
                GetFileTool gft = new GetFileTool();
                gft.setServersToUse(this.replicationTool.getServersToRead());
                gft.setHash(fileToReplicate.getHash());
                if (this.replicationTool.getPassphrase() != null && !this.replicationTool.getPassphrase().trim().equals("")) {
                    gft.setPassphrase(this.replicationTool.getPassphrase());
                }

                MetaData md = gft.getMetaData();

                // Call the method to replicate the file
                this.replicationTool.replicateSingleFile(fileToReplicate.getHash(), md);

            } catch (Exception ex) {
                this.replicationTool.fireFileFailed(fileToReplicate.getHash());
            }
        }
    }

    /**
     * <p>Flag for whether tool should continue to run.</p>
     * @return
     */
    public boolean isRun() {
        return run;
    }

    /**
     * <p>Flag for whether tool should continue to run.</p>
     * @param run
     */
    public void setRun(boolean run) {
        this.run = run;
    }
} // ChunkReplicationDaemon

