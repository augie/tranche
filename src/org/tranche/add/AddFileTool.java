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
package org.tranche.add;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.tranche.ConfigureTranche;
import org.tranche.security.Signature;
import org.tranche.TrancheServer;
import org.tranche.exceptions.NoOnlineServersException;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.hash.BigHash;
import org.tranche.hash.BigHashMaker;
import org.tranche.hash.span.HashSpan;
import org.tranche.license.License;
import org.tranche.license.LicenseUtil;
import org.tranche.logs.LogUtil;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.project.ProjectFile;
import org.tranche.project.ProjectFileUtil;
import org.tranche.project.ProjectFilePart;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.streams.AESEncodingStream;
import org.tranche.streams.GZIPEncodingStream;
import org.tranche.streams.WrappedOutputStream;
import org.tranche.time.TimeUtil;
import org.tranche.timeestimator.ContextualTimeEstimator;
import org.tranche.timeestimator.TimeEstimator;
import org.tranche.users.UserZipFile;
import org.tranche.users.UserZipFileUtil;
import org.tranche.util.DebugUtil;
import org.tranche.FileEncoding;
import org.tranche.Tertiary;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.exceptions.UnknownArgumentException;
import org.tranche.get.GetFileTool;
import org.tranche.network.MultiServerRequestStrategy;
import org.tranche.network.StatusTable;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.util.CompressionUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;
import org.tranche.util.ThreadUtil;

/**
 * <p>A helper utility that adds single files or directories (a.k.a. "data sets" or "projects") of files to the repository.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class AddFileTool {

    private static boolean debug = false;
    /**
     * Default parameters
     */
    public static int DEFAULT_THREADS = 14;
    public static int DEFAULT_MINIMUM_THREADS = 10;
    public static int DEFAULT_DATA_QUEUE_SIZE = 20;
    public static int DEFAULT_META_DATA_QUEUE_SIZE = 25;
    public static int DEFAULT_SIZE_FILE_ENCODING_BUFFER = 1024;
    public static boolean DEFAULT_USE_UNSPECIFIED_SERVERS = true;
    public static boolean DEFAULT_EXPLODE_BEFORE_UPLOAD = false;
    public static boolean DEFAULT_COMPRESS = false;
    public static boolean DEFAULT_DATA_ONLY = false;
    public static boolean DEFAULT_SHOW_SUMMARY = false;
    public static boolean DEFAULT_SHOW_META_DATA_IF_ENCRYPTED = false;
    public static boolean DEFAULT_EMAIL_ON_FAILURE = false;
    public static boolean DEFAULT_USE_PERFORMANCE_LOG = false;
    /**
     * Startup runtime parameters
     */
    private static final boolean START_VALUE_PAUSED = false;
    private static final boolean START_VALUE_STOPPED = false;
    private static final String START_VALUE_TITLE = null;
    private static final String START_VALUE_DESCRIPTION = "";
    private static final File START_VALUE_FILE = null;
    private static final int START_VALUE_FILE_COUNT = 0;
    private static final long START_VALUE_SIZE = 0;
    private static final byte[] START_VALUE_PADDING = new byte[0];
    private static final TimeEstimator START_VALUE_TIME_ESTIMATOR = null;
    private static final ProjectFile START_VALUE_PROJECT_FILE = null;
    /**
     * User parameters
     */
    private File file = START_VALUE_FILE;
    private boolean compress = DEFAULT_COMPRESS, dataOnly = DEFAULT_DATA_ONLY, explodeBeforeUpload = DEFAULT_EXPLODE_BEFORE_UPLOAD, showMetaDataIfEncrypted = DEFAULT_SHOW_META_DATA_IF_ENCRYPTED, useUnspecifiedServers = DEFAULT_USE_UNSPECIFIED_SERVERS, emailOnFailure = DEFAULT_EMAIL_ON_FAILURE, sendPerformanceInfo = DEFAULT_USE_PERFORMANCE_LOG;
    private License license;
    private X509Certificate userCertificate;
    private PrivateKey userPrivateKey;
    private String title = START_VALUE_TITLE, description = START_VALUE_DESCRIPTION, passphrase;
    private final List<MetaDataAnnotation> metaDataAnnotations = new ArrayList<MetaDataAnnotation>();
    private final Set<String> emailConfirmationSet = new HashSet<String>(), serverHostUseSet = new HashSet<String>(), serverHostStickySet = new HashSet<String>();
    /**
     * Runtime parameters
     */
    private boolean paused = START_VALUE_PAUSED, stopped = START_VALUE_STOPPED;
    /**
     * Statistics, reporting variables, listeners
     */
    private final Set<AddFileToolListener> listeners = new HashSet<AddFileToolListener>();
    // handles all information relevant to the progress of the download
    private TimeEstimator timeEstimator = START_VALUE_TIME_ESTIMATOR;
    /**
     * Internal variables
     */
    private boolean projectFileAddedToStack = false;
    private ProjectFile projectFile = START_VALUE_PROJECT_FILE;
    private int threadCount = DEFAULT_THREADS, fileCount = START_VALUE_FILE_COUNT;
    private long size = START_VALUE_SIZE;
    byte[] padding = START_VALUE_PADDING;
    private boolean locked = false;

    /**
     * 
     * @return
     */
    public boolean isExecuting() {
        return isLocked();
    }

    /**
     * 
     * @return
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * 
     */
    public void throwExceptionIfLocked() {
        if (locked) {
            throw new RuntimeException("The variables for this tool are currently locked.");
        }
    }

    /**
     * <p>Sets whether the files should be compressed before upload.</p>
     * @param compress Whether the files should be compressed before upload.
     */
    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    /**
     * <p>Gets whether the files should be compressed before upload.</p>
     * @return Whether the files should be compressed before upload.
     */
    public boolean isCompress() {
        return compress;
    }

    /**
     * <p>Sets whether to try to decompress/unpack a compressed or bundled archive before uploading it.</p>
     * @param explodeBeforeUpload Whether to try to decompress/unpack a compressed or bundled archive before uploading it.
     */
    public void setExplodeBeforeUpload(boolean explodeBeforeUpload) {
        this.explodeBeforeUpload = explodeBeforeUpload;
    }

    /**
     * <p>Gets whether to try to decompress/unpack a compressed or bundled archive before uploading it.</p>
     */
    public boolean isExplodeBeforeUpload() {
        return explodeBeforeUpload;
    }

    /**
     * <p>Sets whether only the data chunks will be uploaded.</p>
     * @param dataOnly Whether only the data chunks will be uploaded.
     */
    public void setDataOnly(boolean dataOnly) {
        throwExceptionIfLocked();
        this.dataOnly = dataOnly;
    }

    /**
     * <p>Gets whether only the data chunks will be uploaded.</p>
     * @return Whether only the data chunks will be uploaded.
     */
    public boolean isDataOnly() {
        return dataOnly;
    }

    /**
     * 
     * @param emailOnFailure
     */
    public void setEmailOnFailure(boolean emailOnFailure) {
        this.emailOnFailure = emailOnFailure;
    }

    /**
     * 
     * @return
     */
    public boolean isEmailOnFailure() {
        return emailOnFailure;
    }

    /**
     * 
     * @param showMetaDataIfEncrypted
     */
    public void setShowMetaDataIfEncrypted(boolean showMetaDataIfEncrypted) {
        this.showMetaDataIfEncrypted = showMetaDataIfEncrypted;
    }

    /**
     *
     * @return
     */
    public boolean isShowMetaDataIfEncrypted() {
        return showMetaDataIfEncrypted;
    }

    /**
     * <p>Sets the license to be attached to the upload.</p>
     * @param license The license to be attached to the upload.
     */
    public void setLicense(License license) {
        throwExceptionIfLocked();
        this.license = license;
    }

    /**
     * <p>Gets the license to be attached to the upload.</p>
     * @return The license to be attached to the upload.
     */
    public License getLicense() {
        return license;
    }

    /**
     * <p>Sets the certificate for the uploading user.</p>
     * @param userCertificate The certificate for the uploading user.
     */
    public void setUserCertificate(X509Certificate userCertificate) {
        throwExceptionIfLocked();
        this.userCertificate = userCertificate;
    }

    /**
     * <p>Gets the certificate for the uploading user.</p>
     * @return The certificate for hte uploading user.
     */
    public X509Certificate getUserCertificate() {
        return userCertificate;
    }

    /**
     * <p>Sets the private key for the uploading user.</p>
     * @param userPrivateKey The private key for the uploading user.
     */
    public void setUserPrivateKey(PrivateKey userPrivateKey) {
        throwExceptionIfLocked();
        this.userPrivateKey = userPrivateKey;
    }

    /**
     * <p>Gets the private key for the uploading user.</p>
     * @return The private key for the uploading user.
     */
    public PrivateKey getUserPrivateKey() {
        return userPrivateKey;
    }

    /**
     * <p>Sets the title of the upload.</p>
     * @param title The title of the upload.
     */
    public void setTitle(String title) {
        throwExceptionIfLocked();
        this.title = title;
    }

    /**
     * <p>Gets the title of the upload.</p>
     * @return The title of the upload.
     */
    public String getTitle() {
        return title;
    }

    /**
     * <p>Sets the description of the upload.</p>
     * @param description The description of the upload.
     */
    public void setDescription(String description) {
        throwExceptionIfLocked();
        this.description = description;
    }

    /**
     * <p>Gets the description of the upload.</p>
     * @return The description of the upload.
     */
    public String getDescription() {
        return description;
    }

    /**
     * <p>Sets the passphrase with which to encrypt the data.</p>
     * @param passphrase The passphrase with which to encrypt the data.
     */
    public void setPassphrase(String passphrase) {
        throwExceptionIfLocked();
        debugOut("Setting the passphrase to: " + passphrase);
        this.passphrase = passphrase;
        if (this.passphrase != null) {
            this.padding = new BigHash(passphrase.getBytes()).toByteArray();
        } else {
            this.padding = START_VALUE_PADDING;
        }
    }

    /**
     * <p>Unsets the passphrase -- the upload will not be encrypted.</p>
     */
    public void clearPassphrase() {
        setPassphrase(null);
    }

    /**
     * <p>Gets the passphrase that will be used to encrypt the data.</p>
     * @return The passphrase that will be used to encrypt the data.
     */
    public String getPassphrase() {
        return passphrase;
    }

    /**
     * <p>Sets the meta data annotations to be added to the data set's project file meta data.</p>
     * @param annotations The meta data annotations.
     */
    public void setMetaDataAnnotations(Collection<MetaDataAnnotation> annotations) {
        this.metaDataAnnotations.clear();
        this.metaDataAnnotations.addAll(annotations);
    }

    /**
     * <p>Adds the given meta data annotation to the meta data annotations.</p>
     * @param annotation A meta data annotation.
     */
    public void addMetaDataAnnotation(MetaDataAnnotation annotation) {
        this.metaDataAnnotations.add(annotation);
    }

    /**
     * <p>Clears the meta data annotations.</p>
     */
    public void clearMetaDataAnnotations() {
        this.metaDataAnnotations.clear();
    }

    /**
     * <p>Gets the meta data annotations to be added to the data set's project file meta data.</p>
     * @return The meta data annotations to be added to the data set's project file meta data.
     */
    public Collection<MetaDataAnnotation> getMetaDataAnnotations() {
        return metaDataAnnotations;
    }

    /**
     * <p>Adds </p>
     * @param email
     * @return
     */
    public boolean addConfirmationEmail(String email) {
        synchronized (emailConfirmationSet) {
            return emailConfirmationSet.add(email);
        }
    }

    /**
     *
     * @param emails
     * @return
     */
    public boolean addConfirmationEmails(Collection<String> emails) {
        synchronized (emailConfirmationSet) {
            return emailConfirmationSet.addAll(emails);
        }
    }

    /**
     * 
     * @param email
     * @return
     */
    public boolean removeConfirmationEmail(String email) {
        synchronized (emailConfirmationSet) {
            return emailConfirmationSet.remove(email);
        }
    }

    /**
     * 
     * @param emails
     * @return
     */
    public boolean removeConfirmationEmails(Collection<String> emails) {
        synchronized (emailConfirmationSet) {
            return emailConfirmationSet.removeAll(emails);
        }
    }

    /**
     * 
     * @return
     */
    public Collection<String> getConfirmationEmails() {
        synchronized (emailConfirmationSet) {
            return Collections.unmodifiableCollection(emailConfirmationSet);
        }
    }

    /**
     * <p>Adds the host name of a server to which data should be stuck.</p>
     * @param serverHostName The host name of a server.
     * @return Whether the host name was added to the list.
     */
    public boolean addStickyServer(String serverHostName) {
        throwExceptionIfLocked();
        synchronized (serverHostStickySet) {
            return serverHostStickySet.add(serverHostName);
        }
    }

    /**
     * <p>Adds a collection of server host names to which data should be stuck.</p>
     * @param serverHostNames A collection of host names.
     * @return Whether the host names were added to the list.
     */
    public boolean addStickyServers(Collection<String> serverHostNames) {
        throwExceptionIfLocked();
        synchronized (serverHostStickySet) {
            return serverHostStickySet.addAll(serverHostNames);
        }
    }

    /**
     * <p>Removes the server host name from the list of sticky servers.</p>
     * @param serverHostName The host name of a server.
     * @return Whether the host name was removed from the list.
     */
    public boolean removeStickyServer(String serverHostName) {
        throwExceptionIfLocked();
        synchronized (serverHostStickySet) {
            return serverHostStickySet.remove(serverHostName);
        }
    }

    /**
     * <p>Removes a collection of host names from the list of sticky servers.</p>
     * @param serverHostNames A collection of server host names.
     * @return Whether the host names were removed from the list.
     */
    public boolean removeStickyServers(Collection<String> serverHostNames) {
        throwExceptionIfLocked();
        synchronized (serverHostStickySet) {
            return serverHostStickySet.removeAll(serverHostNames);
        }
    }

    /**
     * <p>Gets the collection of server host names to which data should be stuck.</p>
     * @return A collection of server host names.
     */
    public Collection<String> getStickyServers() {
        synchronized (serverHostStickySet) {
            return Collections.unmodifiableCollection(serverHostStickySet);
        }
    }

    /**
     *
     * @param serverHostNames
     * @return
     */
    public boolean setStickyServers(Collection<String> serverHostNames) {
        throwExceptionIfLocked();
        synchronized (serverHostStickySet) {
            serverHostStickySet.clear();
            return serverHostStickySet.addAll(serverHostNames);
        }
    }

    /**
     * <p>Adds the host name of a server to be used during upload.</p>
     * @param serverHostName The host name of a server.
     * @return Whether the host name was added to the list.
     */
    public boolean addServerToUse(String serverHostName) {
        throwExceptionIfLocked();
        synchronized (serverHostUseSet) {
            return serverHostUseSet.add(serverHostName);
        }
    }

    /**
     * <p>Adds a collection of server host names to be used during upload.</p>
     * @param serverHostNames A collection of host names to be used during upload.
     * @return Whether the host names were added to the list.
     */
    public boolean addServersToUse(Collection<String> serverHostNames) {
        throwExceptionIfLocked();
        synchronized (serverHostUseSet) {
            return serverHostUseSet.addAll(serverHostNames);
        }
    }

    /**
     * 
     * @param serverHostNames
     * @return
     */
    public boolean setServersToUse(Collection<String> serverHostNames) {
        throwExceptionIfLocked();
        synchronized (serverHostUseSet) {
            serverHostUseSet.clear();
            return serverHostUseSet.addAll(serverHostNames);
        }
    }

    /**
     * <p>Removes the server host name from the list of servers to be used.</p>
     * @param serverHostName The host name of a server.
     * @return Whether the host name was removed from the list.
     */
    public boolean removeServerToUse(String serverHostName) {
        throwExceptionIfLocked();
        synchronized (serverHostUseSet) {
            return serverHostUseSet.remove(serverHostName);
        }
    }

    /**
     * <p>Removes a collection of host names from the list of servers to be used.</p>
     * @param serverHostNames A collection of server host names.
     * @return Whether the host names were removed from the list.
     */
    public boolean removeServersToUse(Collection<String> serverHostNames) {
        throwExceptionIfLocked();
        synchronized (serverHostUseSet) {
            return serverHostUseSet.removeAll(serverHostNames);
        }
    }

    /**
     * <p>Gets the collection of server host names to be used.</p>
     * @return A collection of server host names.
     */
    public Collection<String> getServersToUse() {
        synchronized (serverHostUseSet) {
            return Collections.unmodifiableCollection(serverHostUseSet);
        }
    }

    /**
     * <p>Sets whether servers other than the ones specified should be used.</p>
     * @param useUnspecifiedServers Whether servers other than the ones specified should be used.
     */
    public void setUseUnspecifiedServers(boolean useUnspecifiedServers) {
        throwExceptionIfLocked();
        this.useUnspecifiedServers = useUnspecifiedServers;
    }

    /**
     * <p>Whether servers other than the ones specified should be used.</p>
     * @return Whether servers other than the ones specified should be used.
     */
    public boolean isUsingUnspecifiedServers() {
        return useUnspecifiedServers;
    }

    /**
     * <p></p>
     * @param sendPerformanceInfo
     */
    public void setSendPerformanceInfo(boolean sendPerformanceInfo) {
        this.sendPerformanceInfo = sendPerformanceInfo;
    }

    /**
     * <p></p>
     * @return
     */
    public boolean isSendPerformanceInfo() {
        return sendPerformanceInfo;
    }

    /**
     * <p>Set whether the upload is paused.</p>
     * @param paused Whether the upload is paused.
     */
    public void setPause(boolean paused) {
        if (this.paused != paused) {
            if (paused) {
                fire("Pausing");
            } else {
                fire("Resuming");
                synchronized (AddFileTool.this) {
                    notifyAll();
                }
            }
        }
        this.paused = paused;
    }

    /**
     * <p>Gets whether the upload is paused.</p>
     * @return Whether the upload is paused.
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * <p>Will sleep the current thread until the upload is no longer paused.</p>
     */
    private void waitHereOnPause() {
        while (paused) {
            // break point
            if (stopped) {
                return;
            }
            synchronized (AddFileTool.this) {
                try {
                    wait();
                } catch (Exception e) {
                    debugErr(e);
                }
            }
        }
    }

    /**
     * <p>Stops the upload.</p>
     */
    public void stop() {
        this.stopped = true;
    }

    /**
     * 
     * @return
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * <p>Adds a listener.</p>
     * @param l A listener.
     * @return Whether the listener was added.
     */
    public boolean addListener(AddFileToolListener l) {
        synchronized (listeners) {
            return listeners.add(l);
        }
    }

    /**
     * <p>Removes a listener.</p>
     * @param l A listener.
     * @return Whether the listener was removed.
     */
    public boolean removeListener(AddFileToolListener l) {
        synchronized (listeners) {
            return listeners.remove(l);
        }
    }

    /**
     * 
     * @return
     */
    public Collection<AddFileToolListener> getListeners() {
        synchronized (listeners) {
            return Collections.unmodifiableCollection(listeners);
        }
    }

    /**
     * <p>Gets the number of bytes that are being uploaded.</p>
     * @return The number of bytes that are being uploaded.
     */
    public long getBytesToUpload() {
        if (timeEstimator != null) {
            return timeEstimator.getTotalBytes();
        } else {
            return 0;
        }
    }

    /**
     * <p>Gets the number of bytes that have been uploaded.</p>
     * @return The number of bytes that have been uploaded.
     */
    public long getBytesUploaded() {
        if (timeEstimator != null) {
            return timeEstimator.getBytesDone();
        } else {
            return 0;
        }
    }

    /**
     * <p>Gets the time to upload object.</p>
     * @return The time to upload object.
     */
    public TimeEstimator getTimeEstimator() {
        return timeEstimator;
    }

    /**
     * <p>Sets the number of threads to be used.</p>
     * @param threadCount The number of threads to be used.
     */
    public void setThreadCount(int threadCount) {
        throwExceptionIfLocked();
        if (threadCount < DEFAULT_MINIMUM_THREADS) {
            threadCount = DEFAULT_MINIMUM_THREADS;
        }
        this.threadCount = threadCount;
    }

    /**
     * <p>Gets the number of threads to be used.</p>
     * @return The number of threads to be used.
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * 
     * @return
     */
    public File getFile() {
        return file;
    }

    /**
     *
     * @return
     */
    public int getFileCount() {
        return fileCount;
    }

    /**
     * 
     * @return
     */
    public long getSize() {
        return size;
    }

    /**
     * 
     * @param file
     */
    public void setFile(File file) {
        throwExceptionIfLocked();
        // reset?
        if (file != null && (this.file == null || !this.file.equals(file))) {
            stopped = START_VALUE_STOPPED;
            paused = START_VALUE_PAUSED;
            timeEstimator = START_VALUE_TIME_ESTIMATOR;
            fileCount = START_VALUE_FILE_COUNT;
            size = START_VALUE_SIZE;
            projectFile = START_VALUE_PROJECT_FILE;
            projectFileAddedToStack = false;
            // set the file
            this.file = file;
            // determine the size of the upload
            fire("Assessing the size of the upload.");
            try {
                LinkedList<File> stack = new LinkedList<File>();
                stack.add(file);
                while (!stack.isEmpty()) {
                    File f = stack.removeFirst();
                    if (f.isDirectory()) {
                        for (File ff : f.listFiles()) {
                            // add to the front to make this depth-first/memory efficient
                            stack.addFirst(ff);
                        }
                    } else {
                        size += f.length() + padding.length;
                        fileCount++;
                    }
                }
            } catch (Exception e) {
                debugErr(e);
            }
        }
    }

    /**
     * <p>Returns relative name of file in root dir. Used by ProjectFile to store location in project, which is used by GetFileTool when downloading a project.</p>
     * @param root
     * @param dataFile
     * @return
     * @throws java.io.IOException
     */
    private static final String relativeName(File root, File dataFile) throws IOException {
        // if the execute is a single file and there are attached files, put all files
        //  into a subdirectory with the name of the single file
        if (!root.isDirectory()) {
            return root.getName() + "/" + dataFile.getName();
        }

        String rootString = root.getCanonicalPath();
        String fileString = dataFile.getCanonicalPath();

        // if equal, return the name
        if (rootString.equals(fileString)) {
            return dataFile.getName();
        }

        return root.getName() + "/" + fileString.substring(rootString.length() + 1).replaceAll("\\\\", "/");
    }

    /**
     * <p>Used internally to establish and lock connections with servers that will be used.</p>
     * @throws java.lang.Exception
     */
    protected void setUpConnections() throws Exception {
        NetworkUtil.waitForStartup();
        setUpConnections(getServersToUse());
        setUpConnections(getStickyServers());
    }

    /**
     * <p>Used internally to establish and lock connections with servers that will be used.</p>
     * @param serverHostNames A collection of server host names.
     * @throws java.lang.Exception
     */
    private void setUpConnections(Collection<String> serverHostNames) throws Exception {
        for (String host : serverHostNames) {
            if (ConnectionUtil.isConnected(host)) {
                ConnectionUtil.lockConnection(host);
            } else {
                try {
                    ConnectionUtil.connectHost(host, true);
                } catch (Exception e) {
                    ConnectionUtil.reportExceptionHost(host, e);
                    debugErr(e);
                    fire("Could not connect to server " + host + ".");
                }
            }
        }
    }

    /**
     * <p>Used internally to unlock connections made during set-up.</p>
     */
    protected void tearDownConnections() {
        debugOut("Tearing down connections.");
        for (String host : getServersToUse()) {
            ConnectionUtil.unlockConnection(host);
        }
        for (String host : getStickyServers()) {
            ConnectionUtil.unlockConnection(host);
        }
    }

    /**
     * <p>Used internally to validate the variables used during an upload.</p>
     * @throws java.lang.Exception
     */
    private void validateVariables() throws Exception {
        try {
            // check variables
            if (file == null) {
                throw new NullPointerException("File to upload is null.");
            }
            if (!file.exists()) {
                throw new FileNotFoundException("Could not find the file to upload.");
            }
            if (file.isDirectory() && (file.list() == null || file.list().length == 0)) {
                throw new Exception("Upload directory is empty.");
            }
            if (title == null || title.equals("")) {
                title = file.getName();
            }
            if (description == null) {
                throw new NullPointerException("Description is null.");
            }
            if (userCertificate == null) {
                throw new NullPointerException("User certificate is null.");
            }
            userCertificate.checkValidity(new Date(TimeUtil.getTrancheTimestamp()));
            if (userPrivateKey == null) {
                throw new NullPointerException("User private key is null.");
            }
            // check servers
            boolean serversEmpty = false, stickyServersEmpty = false;
            serversEmpty = getServersToUse().isEmpty();
            stickyServersEmpty = getStickyServers().isEmpty();
            // would result in no actions being taken
            if (serversEmpty && stickyServersEmpty && !useUnspecifiedServers) {
                throw new Exception("No servers to use.");
            }
            // must have connections
            if (ConnectionUtil.getConnectedHosts().size() == 0) {
                throw new NoOnlineServersException("There are no servers to which you can upload. Make sure servers are online and writable.");
            } else {
                // must at least one writable, online server
                boolean writable = false, online = false, targetHashSpans = false, core = false;
                for (StatusTableRow row : ConnectionUtil.getConnectedRows()) {
                    writable = writable || row.isWritable();
                    online = online || row.isOnline();
                    targetHashSpans = targetHashSpans || !row.getTargetHashSpans().isEmpty();
                    core = core || row.isCore();
                }
                if (!writable) {
                    throw new NoOnlineServersException("There are no writable servers to which you can upload.");
                }
                if (!online) {
                    throw new NoOnlineServersException("There are no online servers to which you can upload.");
                }
                if (!targetHashSpans) {
                    throw new NoOnlineServersException("There are no servers with hash spans to which you can upload.");
                }
                if (!core) {
                    throw new NoOnlineServersException("There are no core servers online to which you can upload.");
                }
            }
        } catch (Exception e) {
            fire(e.getClass().getName() + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * <p>Gets a collection of hosts of core writable servers to which a chunk with the given hash should be uploaded.</p>
     * @param hash
     * @return
     */
    protected Collection<String> getCoreServersToUploadTo(BigHash hash) {
        List<String> hosts = new LinkedList<String>();
        StatusTable table = NetworkUtil.getStatus().clone();
        // add all servers with the hash in their target hash spans
        for (StatusTableRow row : table.getRows()) {
            if (!(row.isWritable() && row.isCore() && row.isOnline())) {
                continue;
            }
            for (HashSpan span : row.getTargetHashSpans()) {
                if (span.contains(hash)) {
                    if (!hosts.contains(row.getHost())) {
                        hosts.add(row.getHost());
                    }
                    break;
                }
            }
        }
        // add all the sticky servers
        for (String host : getStickyServers()) {
            StatusTableRow row = table.getRow(host);
            if (row != null && !hosts.contains(host) && row.isWritable() && row.isCore() && row.isOnline()) {
                if (!hosts.contains(host)) {
                    hosts.add(host);
                }
            }
        }
        return Collections.unmodifiableCollection(hosts);
    }

    /**
     * <p>Gets a collection of hosts of non-core writable servers to which one should upload a chunk with the given hash.</p>
     * @param hash
     * @return
     */
    protected Collection<String> getNonCoreServersToUploadTo(BigHash hash) {
        Set<String> hosts = new HashSet<String>();
        StatusTable table = NetworkUtil.getStatus().clone();
        // add all servers with the hash in their target hash spans
        for (StatusTableRow row : table.getRows()) {
            if (!row.isWritable() || row.isCore() || !row.isOnline()) {
                continue;
            }
            for (HashSpan span : row.getTargetHashSpans()) {
                if (span.contains(hash)) {
                    hosts.add(row.getHost());
                    break;
                }
            }
        }
        // add all the sticky servers
        for (String host : getStickyServers()) {
            StatusTableRow row = table.getRow(host);
            if (row != null && !hosts.contains(host) && row.isWritable() && !row.isCore() && row.isOnline()) {
                hosts.add(host);
            }
        }
        return Collections.unmodifiableCollection(hosts);
    }

    /**
     * 
     * @return
     */
    public AddFileToolReport execute() {
        return execute(TimeUtil.getTrancheTimestamp());
    }

    /**
     *
     * @param startTimestamp
     * @return
     */
    public AddFileToolReport execute(long startTimestamp) {
        throwExceptionIfLocked();

        final AddFileToolReport report = new AddFileToolReport(startTimestamp, title, description, passphrase != null, showMetaDataIfEncrypted, null);
        addListener(new AddFileToolReportListener(report));
        File licenseFile = null;
        AddFileToolPerformanceLog performanceLog = null;
        if (isSendPerformanceInfo()) {
            try {
                performanceLog = new AddFileToolPerformanceLog();
                addListener(performanceLog);
            } catch (Exception e) {
                debugErr(e);
            }
        }
        try {
            // lock the variables in place
            locked = true;
            setUpConnections();
            validateVariables();
            try {
                timeEstimator = new ContextualTimeEstimator(0, size, 0, fileCount);
                if (file.isDirectory()) {
                    fireStartedDirectory(file);
                }
                if (file.isDirectory() || license != null) {
                    projectFile = new ProjectFile();
                }

                // create the license file
                if (license != null) {
                    licenseFile = TempFileUtil.createTemporaryFile();
                    // do not want a gazillion of exactly the same file belonging to a gazillion data sets
                    Map<String, String> notes = new HashMap<String, String>();
                    notes.put("Title", title);
                    LicenseUtil.buildLicenseFile(licenseFile, license, new LinkedList<String>(), notes);
                    timeEstimator.addTotalBytes(licenseFile.length());
                    timeEstimator.addTotalFiles(1);
                }

                // set the project file variables
                if (projectFile != null) {
                    projectFile.setName(title);
                    projectFile.setDescription(description);
                }

                // what to upload + order
                LinkedList<FileToUpload> fileStack = new LinkedList<FileToUpload>();
                if (projectFile != null) {
                    fileStack.addLast(new FileToUpload(relativeName(file, file), file, padding, false, false));
                } else {
                    fileStack.addLast(new FileToUpload(file.getName(), file, padding, true, false));
                }
                if (licenseFile != null) {
                    String relativeName = "";
                    if (file.isDirectory()) {
                        // make sure we don't write over another file
                        boolean found;
                        int count = 1;
                        do {
                            String proposedName = LicenseUtil.RECOMMENDED_LICENSE_FILE_NAME;
                            if (count > 1) {
                                proposedName = count + "-" + LicenseUtil.RECOMMENDED_LICENSE_FILE_NAME;
                            }
                            found = false;
                            for (String fileName : file.list()) {
                                if (fileName.toLowerCase().equals(proposedName.toLowerCase())) {
                                    found = true;
                                    count++;
                                    break;
                                }
                                relativeName = file.getName() + "/" + proposedName;
                            }
                        } while (found);
                    } else {
                        relativeName = file.getName() + "/" + LicenseUtil.RECOMMENDED_LICENSE_FILE_NAME;
                    }
                    fileStack.addLast(new FileToUpload(relativeName, licenseFile, padding, false, true));
                }

                // create some threads
                Set<FileEncodingThread> fileThreads = new HashSet<FileEncodingThread>();
                Set<DataUploadingThread> dataThreads = new HashSet<DataUploadingThread>();
                Set<MetaDataUploadingThread> metaThreads = new HashSet<MetaDataUploadingThread>();
                int threadsToStart = threadCount;
                PriorityBlockingQueue<DataChunk> dataChunkQueue = new PriorityBlockingQueue<DataChunk>(DEFAULT_DATA_QUEUE_SIZE);
                PriorityBlockingQueue<MetaChunk> metaChunkQueue = new PriorityBlockingQueue<MetaChunk>(DEFAULT_META_DATA_QUEUE_SIZE);

                // create the upload threads
                for (int i = 0; i < 2; i++) {
                    fileThreads.add(new FileEncodingThread(fileThreads, dataThreads, metaThreads, fileStack, dataChunkQueue, report.getTimestampStart()));
                    threadsToStart--;
                }
                if (!dataOnly) {
                    for (int i = 0; i < Math.ceil(threadsToStart / 2) + 1; i++) {
                        metaThreads.add(new MetaDataUploadingThread(fileThreads, dataThreads, metaThreads, metaChunkQueue));
                        threadsToStart--;
                    }
                }
                for (int i = 0; i < threadsToStart; i++) {
                    dataThreads.add(new DataUploadingThread(fileThreads, dataThreads, metaThreads, dataChunkQueue, metaChunkQueue));
                    threadsToStart--;
                }
                // start the threads
                for (FileEncodingThread fileThread : fileThreads) {
                    fileThread.start();
                }
                for (DataUploadingThread dataThread : dataThreads) {
                    dataThread.start();
                }
                for (MetaDataUploadingThread metaThread : metaThreads) {
                    metaThread.start();
                }
                // wait for the file encoding threads to finish
                for (FileEncodingThread fileThread : fileThreads) {
                    fileThread.waitForFinish();
                    // check for primary flie hash
                    boolean isSetHash = !fileThread.stopped && !dataOnly && fileThread.primaryFileHash != null;
                    if (isSetHash) {
                        report.setHash(fileThread.primaryFileHash);
                    }
                }
                // wait for the chunk uploads to finish
                for (DataUploadingThread dataThread : dataThreads) {
                    dataThread.waitForFinish();
                }
                for (MetaDataUploadingThread metaThread : metaThreads) {
                    metaThread.waitForFinish();
                }
                // check for failures
                if (file.isDirectory()) {
                    if (report.isFailed()) {
                        fireFailedDirectory(file, report.getFailureExceptions());
                    } else {
                        fireFinishedDirectory(file);
                    }
                }
            } catch (Exception e) {
                if (file.isDirectory()) {
                    fireFailedDirectory(file, e);
                }
                debugErr(e);
            }
        } catch (Exception e) {
            report.addFailureException(new PropagationExceptionWrapper(e));
            debugErr(e);
        } finally {
            tearDownConnections();
            IOUtil.safeDelete(licenseFile);
            report.setTimestampEnd(TimeUtil.getTrancheTimestamp());
            if (timeEstimator != null) {
                report.setBytesUploaded(timeEstimator.getBytesDone());
                report.setFilesUploaded(timeEstimator.getFilesDone());
            }
            // need to be sure the reported hash is null when there is a failure
            if (report.isFailed()) {
                for (PropagationExceptionWrapper pew : report.getFailureExceptions()) {
                    debugErr(pew.exception);
                }
                report.setHash(null);
            }
            // register our upload only if there were no failures
            if (!dataOnly) {
                if (!report.isFailed() && !TestUtil.isTesting()) {
                    AddFileToolUtil.registerUpload(this, report);
                    if (!emailConfirmationSet.isEmpty()) {
                        AddFileToolUtil.emailReceipt(emailConfirmationSet.toArray(new String[0]), report);
                    }
                    if (ConfigureTranche.getAdminEmailAccounts().length > 0) {
                        AddFileToolUtil.emailReceipt(ConfigureTranche.getAdminEmailAccounts(), report);
                    }
                } else if (report.isFailed() && !TestUtil.isTesting()) {
                    if (emailOnFailure && !emailConfirmationSet.isEmpty()) {
                        AddFileToolUtil.emailFailureNotice(emailConfirmationSet.toArray(new String[0]), this, report);
                    }
                    if (ConfigureTranche.getAdminEmailAccounts().length > 0) {
                        AddFileToolUtil.emailFailureNotice(ConfigureTranche.getAdminEmailAccounts(), this, report);
                    }
                }
            }
            if (performanceLog != null) {
                removeListener(performanceLog);
            }
            locked = false;
        }
        return report;
    }

    /**
     * <p>Notifies listeners that meta data is being started.</p>
     * @param hash The hash of the meta data.
     */
    private void fireStartedMetaData(BigHash hash) {
        fire(new AddFileToolEvent(AddFileToolEvent.ACTION_STARTED, AddFileToolEvent.TYPE_METADATA, hash));
    }

    /**
     * <p>Notifies listeners that meta data is trying to be uploaded.</p>
     * @param hash The hash of the meta data.
     * @param serverHost The host name of the server.
     */
    private void fireTryingMetaData(BigHash hash, String serverHost) {
        fire(new AddFileToolEvent(AddFileToolEvent.ACTION_TRYING, AddFileToolEvent.TYPE_METADATA, hash, serverHost));
    }

    /**
     * <p>Notifies listeners that meta data has been uploaded.</p>
     * @param hash The hash of the meta data.
     * @param serverHost The host name of the server.
     */
    private void fireUploadedMetaData(BigHash hash, String serverHost) {
        fire(new AddFileToolEvent(AddFileToolEvent.ACTION_UPLOADED, AddFileToolEvent.TYPE_METADATA, hash, serverHost));
    }

    /**
     * <p>Notifies listeners that a meta data could not be uploaded.</p>
     * @param file
     * @param hash The hash of the meta data.
     * @param propagationExceptions
     */
    private void fireFailedMetaData(File file, BigHash hash, Collection<PropagationExceptionWrapper> exceptions) {
        fireFailure(new AddFileToolEvent(AddFileToolEvent.ACTION_FAILED, AddFileToolEvent.TYPE_METADATA, file, hash), exceptions);
    }

    /**
     * <p>Notifies listeners that a meta data was uploaded.</p>
     * @param metaData The meta data that was uploaded.
     * @param hash The hash of the meta data.
     */
    private void fireFinishedMetaData(MetaData metaData, BigHash hash) {
        if (!metaData.isProjectFile() && timeEstimator != null) {
            timeEstimator.update(timeEstimator.getBytesDone(), timeEstimator.getTotalBytes() - hash.getLength() + metaData.getEncodings().get(metaData.getEncodings().size() - 1).getHash().getLength(), timeEstimator.getFilesDone() + 1, timeEstimator.getTotalFiles());
        }
        fire(new AddFileToolEvent(AddFileToolEvent.ACTION_FINISHED, AddFileToolEvent.TYPE_METADATA, hash));
    }

    /**
     * <p>Notifies listeners that a data chunk is going to be uploaded.</p>
     * @param fileName
     * @param fileHash The hash of the file to which the data chunk belongs.
     * @param chunkHash The hash of the data chunk.
     */
    private void fireStartedData(String fileName, File file, BigHash chunkHash) {
        fire(new AddFileToolEvent(AddFileToolEvent.ACTION_STARTED, AddFileToolEvent.TYPE_DATA, fileName, chunkHash, file));
    }

    /**
     * <p>Notifies listeners that a data chunk is trying to be uploaded.</p>
     * @param fileName
     * @param fileHash The hash of the file to which the data chunk belongs.
     * @param chunkHash The hash of the data chunk.
     * @param serverHost The host name of the server.
     */
    private void fireTryingData(String fileName, File file, BigHash chunkHash, String serverHost) {
        fire(new AddFileToolEvent(AddFileToolEvent.ACTION_TRYING, AddFileToolEvent.TYPE_DATA, fileName, file, serverHost, chunkHash));
    }

    /**
     * <p>Notifies listeners that a data chunk has been uploaded.</p>
     * @param fileName
     * @param fileHash The hash of the file to which the data chunk belongs.
     * @param chunkHash The hash of the data chunk.
     * @param serverHost The host name of the server.
     */
    private void fireUploadedData(String fileName, File file, BigHash chunkHash, String serverHost) {
        fire(new AddFileToolEvent(AddFileToolEvent.ACTION_UPLOADED, AddFileToolEvent.TYPE_DATA, fileName, file, serverHost, chunkHash));
    }

    /**
     * <p>Notifies listeners that a data chunk could not be uploaded.</p>
     * @param fileName
     * @param fileHash The hash of the file to which the data chunk belongs.
     * @param chunkHash The hash of the data chunk.
     * @param propagationExceptions
     */
    private void fireFailedData(String fileName, File file, BigHash chunkHash, Collection<PropagationExceptionWrapper> exceptions) {
        fireFailure(new AddFileToolEvent(AddFileToolEvent.ACTION_FAILED, AddFileToolEvent.TYPE_DATA, fileName, chunkHash, file), exceptions);
    }

    /**
     * <p>Notifies listeners that a data chunk could not be uploaded.</p>
     * @param fileName
     * @param fileHash The hash of the file to which the data chunk belongs.
     * @param chunkHash The hash of the data chunk.
     */
    private void fireFinishedData(String fileName, File file, BigHash chunkHash) {
        if (file != null && !file.getName().equals(ProjectFile.DEFAULT_PROJECT_FILE_NAME) && timeEstimator != null) {
            timeEstimator.update(timeEstimator.getBytesDone() + chunkHash.getLength(), timeEstimator.getTotalBytes(), timeEstimator.getFilesDone(), timeEstimator.getTotalFiles());
        }
        fire(new AddFileToolEvent(AddFileToolEvent.ACTION_FINISHED, AddFileToolEvent.TYPE_DATA, fileName, chunkHash, file));
    }

    /**
     * <p>Notifies listeners that a file has been started.</p>
     * @param hash The hash of the file.
     * @param relativeName The relative name of the file in the data set.
     */
    private void fireStartedFile(File file, String relativeName) {
        fire(new AddFileToolEvent(AddFileToolEvent.ACTION_STARTED, AddFileToolEvent.TYPE_FILE, relativeName, file));
    }

    /**
     * <p>Notifies listeners that a file could not be uploaded.</p>
     * @param hash The hash of the file.
     * @param propagationExceptions
     */
    private void fireFailedFile(File file, String relativeName, Collection<PropagationExceptionWrapper> exceptions) {
        fireFailure(new AddFileToolEvent(AddFileToolEvent.ACTION_FAILED, AddFileToolEvent.TYPE_FILE, relativeName, file), exceptions);
    }

    /**
     * <p>Notifies listeners that a file was uploaded.</p>
     * @param hash The hash of the file.
     */
    private void fireFinishedFile(File file, String relativeName, BigHash fileHash) {
        fire(new AddFileToolEvent(AddFileToolEvent.ACTION_FINISHED, AddFileToolEvent.TYPE_FILE, relativeName, file, fileHash));
    }

    /**
     * <p>Notifies listeners that a directory upload has started.</p>
     * @param hash The directory file.
     */
    private void fireStartedDirectory(File file) {
        fire(new AddFileToolEvent(AddFileToolEvent.ACTION_STARTED, AddFileToolEvent.TYPE_DIRECTORY, file));
    }

    /**
     * <p>Notifies listeners that a directory upload has finished.</p>
     * @param hash The hash of the directory.
     */
    private void fireFinishedDirectory(File file) {
        fire(new AddFileToolEvent(AddFileToolEvent.ACTION_FINISHED, AddFileToolEvent.TYPE_DIRECTORY, file));
    }

    /**
     * 
     * @param file
     * @param exception
     */
    private void fireFailedDirectory(File file, Exception exception) {
        Set<PropagationExceptionWrapper> exceptions = new HashSet<PropagationExceptionWrapper>();
        exceptions.add(new PropagationExceptionWrapper(exception));
        fireFailure(new AddFileToolEvent(AddFileToolEvent.ACTION_FAILED, AddFileToolEvent.TYPE_DIRECTORY, file), exceptions);
    }

    /**
     * <p>Notifies listeners that a directory upload has failed.</p>
     * @param hash The hash of the directory.
     * @param propagationExceptions
     */
    private void fireFailedDirectory(File file, Collection<PropagationExceptionWrapper> exceptions) {
        fireFailure(new AddFileToolEvent(AddFileToolEvent.ACTION_FAILED, AddFileToolEvent.TYPE_DIRECTORY, file), exceptions);
    }

    /**
     * <p>Fires the given event.</p>
     * @param event An event.
     */
    private void fire(AddFileToolEvent event) {
        // break point
        if (stopped) {
            return;
        }
        waitHereOnPause();
        for (AddFileToolListener l : getListeners()) {
            try {
                if (event.getType() == AddFileToolEvent.TYPE_METADATA) {
                    if (event.getAction() == AddFileToolEvent.ACTION_STARTED) {
                        l.startedMetaData(event);
                    } else if (event.getAction() == AddFileToolEvent.ACTION_TRYING) {
                        l.tryingMetaData(event);
                    } else if (event.getAction() == AddFileToolEvent.ACTION_UPLOADED) {
                        l.uploadedMetaData(event);
                    } else if (event.getAction() == AddFileToolEvent.ACTION_FINISHED) {
                        l.finishedMetaData(event);
                    }
                } else if (event.getType() == AddFileToolEvent.TYPE_DATA) {
                    if (event.getAction() == AddFileToolEvent.ACTION_STARTED) {
                        l.startedData(event);
                    } else if (event.getAction() == AddFileToolEvent.ACTION_TRYING) {
                        l.tryingData(event);
                    } else if (event.getAction() == AddFileToolEvent.ACTION_UPLOADED) {
                        l.uploadedData(event);
                    } else if (event.getAction() == AddFileToolEvent.ACTION_FINISHED) {
                        l.finishedData(event);
                    }
                } else if (event.getType() == AddFileToolEvent.TYPE_FILE) {
                    if (event.getAction() == AddFileToolEvent.ACTION_STARTED) {
                        l.startedFile(event);
                    } else if (event.getAction() == AddFileToolEvent.ACTION_FINISHED) {
                        l.finishedFile(event);
                    }
                } else if (event.getType() == AddFileToolEvent.TYPE_DIRECTORY) {
                    if (event.getAction() == AddFileToolEvent.ACTION_STARTED) {
                        l.startedDirectory(event);
                    } else if (event.getAction() == AddFileToolEvent.ACTION_FINISHED) {
                        l.finishedDirectory(event);
                    }
                }
            } catch (Exception e) {
                debugErr(e);
            }
        }
    }

    /**
     * 
     * @param event
     * @param propagationExceptions
     */
    private void fireFailure(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        // break point
        if (stopped) {
            return;
        }
        waitHereOnPause();
        for (AddFileToolListener l : getListeners()) {
            try {
                if (event.getType() == AddFileToolEvent.TYPE_METADATA) {
                    if (event.getAction() == AddFileToolEvent.ACTION_FAILED) {
                        l.failedMetaData(event, exceptions);
                    }
                } else if (event.getType() == AddFileToolEvent.TYPE_DATA) {
                    if (event.getAction() == AddFileToolEvent.ACTION_FAILED) {
                        l.failedData(event, exceptions);
                    }
                } else if (event.getType() == AddFileToolEvent.TYPE_FILE) {
                    if (event.getAction() == AddFileToolEvent.ACTION_FAILED) {
                        l.failedFile(event, exceptions);
                    }
                } else if (event.getType() == AddFileToolEvent.TYPE_DIRECTORY) {
                    if (event.getAction() == AddFileToolEvent.ACTION_FAILED) {
                        l.failedDirectory(event, exceptions);
                    }
                }
            } catch (Exception e) {
                debugErr(e);
            }
        }
    }

    /**
     * <p>Fires a message.</p>
     * @param message A message.
     */
    private void fire(String message) {
        for (AddFileToolListener l : getListeners()) {
            try {
                l.message(message);
            } catch (Exception e) {
                debugErr(e);
            }
        }
    }

    /**
     * <p>Print out command-line usage.</p>
     */
    private static void printUsage() {
        System.err.println();
        System.err.println("USAGE");
        System.err.println("    [FLAGS / PARAMETERS] <FILE/DIRECTORY TO UPLOAD>");
        System.err.println();
        System.err.println("DESCRIPTION");
        System.err.println("    Upload the file or directory at <FILE/DIRECTORY TO UPLOAD>.");
        System.err.println();
        System.err.println("MEMORY ALLOCATION");
        System.err.println("    To allocate 512 MB of memory to the process, you should use the JVM option: java -Xmx512m");
        System.err.println();
        System.err.println("USER FILE AND SECURITY");
        System.err.println("    If you are not an approved user, visit " + ConfigureTranche.get(ConfigureTranche.PROP_SIGN_UP_URL) + " to apply.");
        System.err.println();
        System.err.println("    If you have an approved account, use your login information or go to " + ConfigureTranche.get(ConfigureTranche.PROP_HOME_URL) + " to download a user file.");
        System.err.println();
        System.err.println("LICENSE AGGREEMENT");
        System.err.println("    A license agreement can be uploaded with your files. Uploading without a license puts the files into the public domain. Right now, the only built-in license is the Creative Commons Zero waiver (CC0). For more information, use the --describecczero parameter.");
        System.err.println();
        System.err.println("    You can provide a custom license. This can be as simple as providing contact information, or as complete as a legal license provided by a lawyer, institution or third party.");
        System.err.println();
        System.err.println("PRINT AND EXIT FLAGS");
        System.err.println("    Use one of these to print some information and exit. Usage: java -jar <JAR> [PRINT AND EXIT FLAG]");
        System.err.println();
        System.err.println("    -C, --describecczero        Print the Creative Commons Zero (CC0) waiver and exit.");
        System.err.println("    -h, --help                  Print usage and exit.");
        System.err.println("    -V, --version               Print version number and exit.");
        System.err.println();
        System.err.println("REQUIRED PARAMETERS");
        System.err.println("  LOGIN: PLEASE SELECT ONE OF THE FOLLOWING OPTIONS:");
        System.err.println();
        System.err.println("   OPTION 1: USER ZIP FILE");
        System.err.println("    -z,  --userzipfile          Values: any two strings.  The location of your user zip file and the password (e.g., \"-z /path/to/Augie.zip.encrypted notmypassword\").");
        System.err.println("   OPTION 2: HTTP LOGIN");
        System.err.println("    -L,  --login                Values: any two strings.  The username and password for your log in (e.g., \"-L Augie notmypassword\").");
        System.err.println();
        System.err.println("RECOMMENDED PARAMETERS");
        System.err.println("    These parameters make your files more useful by providing information for users who are browsing.");
        System.err.println();
        System.err.println("    -d, --description           Values: any string.       Brief description of upload.");
        System.err.println("    -D, --descriptionfile       Values: any string.       Brief description of upload (in a plain-text file, specify path to file).");
        System.err.println("    -t, --title                 Values: any string.       Brief title.");
        System.err.println("    -T, --titlefile             Values: any string.       Brief title (in a plain-text file, specify path to file).");
        System.err.println();
        System.err.println("OPTIONAL OUTOUT & RUNTIME PARAMETERS");
        System.err.println("    By default, only the hash for the upload is printed to standard output, and errors printed to standard err. These options modify this behavior.");
        System.err.println();
        System.err.println("    -a, --allhashes             Values: none.             Print out hashes for each file to standard out. Will be printed as /path/to/file\tfile_hash, with each file output on a separate line. Easily parsed: for each line, split on whitespace and verify two columns and that second is a hash. More on this feature in section HASHES OUTPUT.");
        System.err.println("    -E, --email                 Values: email address.    Send a confirmation email when upload is finished. For more than one email, use multiple separate flags, e.g., -E jane@domain.org -E john@domain.org");
        System.err.println("    -g, --debug                 Values: none.             Prints out useful debugging information.");
        System.err.println("    -S, --showsummary           Values: none.             Prints out a summary of time and speed to standard error upon completion of a project upload.");
        System.err.println("    -v, --verbose               Values: none.             Print out progress information for all files. Intended for human agents, and not automated tool. Will produce a lot of standard output, not easily parsed.");
        System.err.println();
        System.err.println("OPTIONAL PARAMETERS");
        System.err.println("    We recommend you use the default values, which are adjusted for best performance and stability.");
        System.err.println();
        System.err.println("    -A, --annotation            Values: any two strings.  Adds an annotation to the meta data. (e.g., '-A \"My Name\" Augie').");
        System.err.println("    -c, --agreecczero           Value:  none.             Agree to the Creative Commons Zero waiver. For more information, use the --describecczero parameter.");
        System.err.println("    -e, --passphrase            Value:  any string.       Used to encrypt the upload. Data will only be available to users with this password.");
        System.err.println("    -f, --useunspecified        Value:  true or false.    Whether to use unspecified servers for the upload. Specify servers to use with the \"-V, --server\" and \"-I, --sticky\" parameters. Default value is " + AddFileTool.DEFAULT_USE_UNSPECIFIED_SERVERS + ".");
        System.err.println("    -F, --performance           Value:  true or false.    Monitors performance of tool and connections and emails to development team. Default value is " + DEFAULT_USE_PERFORMANCE_LOG + ".");
        System.err.println("    -I, --sticky                Value:  any string.       Specify the host name of a server to which the files should be stuck. If want to specify more than one, use flag multiple times (e.g., \"-I 141.214.241.100 -I 100.100.100.100\").");
        System.err.println("    -l, --customlicense         Value:  any string.       Provide custom custom license information.");
        System.err.println("    -m, --tempdir               Value:  any string.       Path to use for temporary directory instead of default. Default is based on different heuristics for operating system. Default value is " + TempFileUtil.getTemporaryDirectory() + ".");
        System.err.println("    -M, --customlicensefile     Value:  any string.       Provide custom custom license information (in a plain-text file, specify path to file).");
        System.err.println("    -N, --threads               Value:  number.           The number of threads to allow for upload. Default value is " + AddFileTool.DEFAULT_THREADS + ".");
        System.err.println("    -O, --compress              Value:  true or false.    Compress the files as they are uploaded. Tranche currently uses the GZIP algorithm to perform compression. Default value is " + AddFileTool.DEFAULT_COMPRESS + ".");
        System.err.println("    -R, --server                Value:  any string.       Specify the host name of a preferred server. If want to specify more than one, use flag multiple times (e.g., \"-V 141.214.241.100 -V 100.100.100.100\").");
        System.err.println("    -w, --showencinfo           Value:  true or false.    If your upload is encrypted, setting this to true will make the title and description available in the meta data. Default value is " + AddFileTool.DEFAULT_SHOW_META_DATA_IF_ENCRYPTED + ".");
        System.err.println("    -x, --explode               Value:  true or false.    Explode and decompress archives before uploading. Supported formats are GZIP, LZMA, BZIP2, TAR, ZIP, TAR-GZIP, and TAR-BZIP2. Default value is " + AddFileTool.DEFAULT_EXPLODE_BEFORE_UPLOAD + ".");
        System.err.println("    -y, --dataonly              Value:  true or false.    Upload only the data chunks. This can be used to quickly increase the replication of data chunks. Default value is " + AddFileTool.DEFAULT_DATA_ONLY + ".");
        System.err.println();
        System.err.println("HASHES OUTPUT");
        System.err.println("    The flag \"--allhashes\" or \"-a\" will print out all file hashes to standard output (stdout). Each uploaded file will result in a single line printed in the following format:");
        System.err.println();
        System.err.println("        /path/to/file\tfiles_hash");
        System.err.println();
        System.err.println("    Should you be uploading a directory, the final hash (which is used to download the entire upload) is also printed to standard output as the very last line of output. To help with parsing, the following line will appear after all file hash entries and before the final hash:");
        System.err.println();
        System.err.println("        --- Upload Completed ---");
        System.err.println();
        System.err.println("    If the \"all hashes\" feature is enabled, the output will have the following form:");
        System.err.println();
        System.err.println("        /path/to/file[1]\tfile[1]_hash");
        System.err.println("        /path/to/file[2]\tfile[2]_hash");
        System.err.println("        ...");
        System.err.println("        /path/to/file[N-1]\tfile[N-1]_hash");
        System.err.println("        /path/to/file[N]\tfile[N]_hash");
        System.err.println("        --- Upload Completed ---");
        System.err.println("        hash");
        System.err.println();
        System.err.println("    If the \"all hashes\" feature is disabled, the only output will be the upload hash on a single line.");
        System.err.println();
        System.err.println("RETURN CODES");
        System.err.println("    0: Program exited normally (e.g., upload succeeded, help displayed, etc.)");
        System.err.println("    1: Unknown error.");
        System.err.println("    2: Missing required argument(s).");
        System.err.println("    3: Problem with argument(s).");
        System.err.println("    4: File to upload not found.");
        System.err.println("    5: Connection issues.");
        System.err.println("    6: Could not open user file.");
        System.err.println("    7: User file expired.");
        System.err.println("    8: User login failed.");
        System.err.println();
    }

    /**
     * <p>Command-line interface to tool. Use -h or --help to see usage.</p>
     * @param args The set of arguments to be used when parsing the program.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        try {
            if (args.length == 0) {
                printUsage();
                if (!TestUtil.isTesting()) {
                    System.exit(2);
                } else {
                    return;
                }
            }

            // flags
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-g") || args[i].equals("--debug")) {
                    DebugUtil.setDebug(true);
                    setDebug(true);
                } else if (args[i].equals("-h") || args[i].equals("--help")) {
                    printUsage();
                    if (!TestUtil.isTesting()) {
                        System.exit(0);
                    } else {
                        return;
                    }
                } else if (args[i].equals("-C") || args[i].equals("--describecczero")) {
                    System.out.println("=========================== Create Commons Zero (CC0) Waiver ===========================");
                    System.out.println();
                    System.out.println(License.CC0.getDescription());
                    System.out.println();
                    if (!TestUtil.isTesting()) {
                        System.exit(0);
                    } else {
                        return;
                    }
                } else if (args[i].equals("-n") || args[i].equals("--buildnumber") || args[i].equals("--build") || args[i].equals("-V") || args[i].equals("--version")) {
                    System.out.println("Tranche uploader, build #@buildNumber");
                    if (!TestUtil.isTesting()) {
                        System.exit(0);
                    } else {
                        return;
                    }
                }
            }

            // configure
            try {
                ConfigureTranche.load(args);
            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                debugErr(e);
                if (!TestUtil.isTesting()) {
                    System.exit(2);
                } else {
                    return;
                }
            }

            AddFileTool aft = new AddFileTool();
            boolean showSummary = DEFAULT_SHOW_SUMMARY;
            try {
                for (int i = 1; i < args.length - 1; i += 2) {

                    String arg = args[i];

                    if (arg.equals("-g") || arg.equals("--debug")) {
                        i--;
                    } else if (arg.equals("-S") || arg.equals("--showsummary")) {
                        showSummary = true;
                        i--;
                    } else if (arg.equals("-a") || arg.equals("--allhashes")) {
                        aft.addListener(new AddFileToolAdapter() {

                            @Override
                            public void finishedFile(AddFileToolEvent event) {
                                System.out.println(event.getFileName() + "\t" + event.getFileHash());
                            }

                            @Override
                            public void finishedDirectory(AddFileToolEvent event) {
                                System.out.println("--- Upload Completed ---");
                            }
                        });
                        i--;
                    } else if (arg.equals("-v") || arg.equals("--verbose")) {
                        aft.addListener(new CommandLineAddFileToolListener(aft, System.out));
                        i--;
                    } else if (arg.equals("-u") || arg.equals("--user")) {
                        System.err.println("WARNING: The use of -u, --user has been deprecated. Use -z, --userzipfile instead.");
                    } else if (arg.equals("-p") || arg.equals("--userpassword")) {
                        System.err.println("WARNING: The use of -p, --userpassword has been deprecated. Use -z, --userzipfile instead.");
                    } else if (arg.equals("-z") || arg.equals("--userzipfile")) {
                        try {
                            UserZipFile user = new UserZipFile(new File(args[i + 1]));
                            user.setPassphrase(args[i + 2]);
                            i++;
                            aft.setUserCertificate(user.getCertificate());
                            aft.setUserPrivateKey(user.getPrivateKey());
                        } catch (Exception e) {
                            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                            LogUtil.logError(e);
                            if (!TestUtil.isTesting()) {
                                System.exit(6);
                            } else {
                                throw e;
                            }
                        }
                    } else if (arg.equals("-U") || arg.equals("--username")) {
                        System.err.println("WARNING: The use of -U, --username has been deprecated. Use -L, --login instead.");
                    } else if (arg.equals("-P") || arg.equals("--password")) {
                        System.err.println("WARNING: The use of -P, --password has been deprecated. Use -L, --login instead.");
                    } else if (arg.equals("-L") || arg.equals("--login")) {
                        try {
                            String name = args[i + 1];
                            String password = args[i + 2];
                            UserZipFile user = UserZipFileUtil.getUserZipFile(name, password);
                            i++;
                            aft.setUserCertificate(user.getCertificate());
                            aft.setUserPrivateKey(user.getPrivateKey());
                        } catch (Exception e) {
                            System.err.println("Check login information, " + e.getClass().getSimpleName() + ": " + e.getMessage());
                            e.printStackTrace(System.err);
                            LogUtil.logError(e);
                            if (!TestUtil.isTesting()) {
                                System.exit(8);
                            } else {
                                throw e;
                            }
                        }
                    } else if (arg.equals("-E") || arg.equals("--email")) {
                        aft.addConfirmationEmail(args[i + 1]);
                    } else if (arg.equals("-V") || arg.equals("--server")) {
                        aft.addServerToUse(args[i + 1]);
                    } else if (arg.equals("-I") || arg.equals("--sticky")) {
                        aft.addStickyServer(args[i + 1]);
                    } else if (arg.equals("-d") || arg.equals("--description")) {
                        aft.setDescription(args[i + 1]);
                    } else if (arg.equals("-D") || arg.equals("--descriptionfile")) {
                        FileInputStream fis = null;
                        ByteArrayOutputStream baos = null;
                        try {
                            fis = new FileInputStream(args[i + 1]);
                            baos = new ByteArrayOutputStream();
                            IOUtil.getBytes(fis, baos);
                            aft.setDescription(new String(baos.toByteArray()));
                        } finally {
                            IOUtil.safeClose(baos);
                            IOUtil.safeClose(fis);
                        }
                    } else if (arg.equals("-t") || arg.equals("--title")) {
                        aft.setTitle(args[i + 1]);
                    } else if (arg.equals("-T") || arg.equals("--titlefile")) {
                        FileInputStream fis = null;
                        ByteArrayOutputStream baos = null;
                        try {
                            fis = new FileInputStream(args[i + 1]);
                            baos = new ByteArrayOutputStream();
                            IOUtil.getBytes(fis, baos);
                            aft.setTitle(new String(baos.toByteArray()));
                        } finally {
                            IOUtil.safeClose(baos);
                            IOUtil.safeClose(fis);
                        }
                    } else if (arg.equals("-c") || arg.equals("--agreecczero")) {
                        aft.setLicense(License.CC0);
                        i--;
                    } else if (arg.equals("-l") || arg.equals("--customlicense")) {
                        aft.setLicense(new License("Custom License", "", args[i + 1], false));
                    } else if (arg.equals("-M") || arg.equals("--customlicensefile")) {
                        FileInputStream fis = null;
                        ByteArrayOutputStream baos = null;
                        try {
                            fis = new FileInputStream(args[i + 1]);
                            baos = new ByteArrayOutputStream();
                            IOUtil.getBytes(fis, baos);
                            aft.setLicense(new License("Custom License", "", new String(baos.toByteArray()), false));
                        } finally {
                            IOUtil.safeClose(baos);
                            IOUtil.safeClose(fis);
                        }
                    } else if (arg.equals("-e") || arg.equals("--encryptionpassword") || arg.equals("--passphrase")) {
                        aft.setPassphrase(args[i + 1]);
                    } else if (arg.equals("-b") || arg.equals("--usebatch") || arg.equals("--batch")) {
                        System.err.println("WARNING: The use of -b, --usebatch, --batch has been deprecated. It currently does nothing, and the options may be removed in future versions of AddFileTool.");
                    } else if (arg.equals("-N") || arg.equals("--threads")) {
                        aft.setThreadCount(Integer.parseInt(args[i + 1]));
                    } else if (arg.equals("-r") || arg.equals("--remotereplication")) {
                        System.err.println("WARNING: The use of -r, --remotereplication has been deprecated. It currently does nothing, and the options may be removed in future versions of AddFileTool.");
                    } else if (arg.equals("-H") || arg.equals("--enforcehashspans")) {
                        System.err.println("WARNING: The use of -H, --enforcehashspans has been deprecated. It currently does nothing, and the options may be removed in future versions of AddFileTool.");
                    } else if (arg.equals("-q") || arg.equals("--requiredreps")) {
                        System.err.println("WARNING: The use of -q, --requiredreps has been deprecated. It currently does nothing, and the options may be removed in future versions of AddFileTool.");
                    } else if (arg.equals("--register")) {
                        System.err.println("WARNING: The use of --register has been deprecated. It currently does nothing, and the options may be removed in future versions of AddFileTool.");
                    } else if (arg.equals("-x") || arg.equals("--explode")) {
                        aft.setExplodeBeforeUpload(Boolean.parseBoolean(args[i + 1]));
                    } else if (arg.equals("-O") || arg.equals("--compress")) {
                        aft.setCompress(Boolean.parseBoolean(args[i + 1]));
                    } else if (arg.equals("-y") || arg.equals("--dataonly")) {
                        aft.setDataOnly(Boolean.parseBoolean(args[i + 1]));
                    } else if (arg.equals("-w") || arg.equals("--showencinfo")) {
                        aft.setShowMetaDataIfEncrypted(Boolean.parseBoolean(args[i + 1]));
                    } else if (arg.equals("-f") || arg.equals("--useunspecified")) {
                        aft.setUseUnspecifiedServers(Boolean.parseBoolean(args[i + 1]));
                    } else if (arg.equals("-m") || arg.equals("--tempdir")) {
                        TempFileUtil.setTemporaryDirectory(new File(args[i + 1]));
                    } else if (arg.equals("-A") || arg.equals("--annotation")) {
                        aft.addMetaDataAnnotation(new MetaDataAnnotation(args[i + 1], args[i + 2]));
                        i++;
                    } else if (arg.equals("-H") || arg.equals("--proxyhost")) {
                        System.err.println("WARNING: The use of -H, --proxyhost has been deprecated.");
                    } else if (arg.equals("-X") || arg.equals("--proxyport")) {
                        System.err.println("WARNING: The use of -X, --proxyport has been deprecated.");
                    } else if (arg.equals("-F") || arg.equals("--performance")) {
                        try {
                            aft.setSendPerformanceInfo(Boolean.parseBoolean(args[i + 1]));
                        } catch (Exception e) {
                            throw new UnknownArgumentException("Expect a true/false value for " + arg + ", received " + args[i + 1]);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                debugErr(e);
                if (!TestUtil.isTesting()) {
                    System.exit(3);
                } else {
                    throw e;
                }
            }

            // perform upload
            try {
                aft.setFile(new File(args[args.length - 1]));
                AddFileToolReport report = aft.execute(TimeUtil.getTrancheTimestamp());
                if (report.isFailed()) {
                    Set<Exception> exceptions = new HashSet<Exception>();
                    for (PropagationExceptionWrapper pew : report.getFailureExceptions()) {
                        System.err.println("  " + pew.toString());
                        exceptions.add(pew.exception);
                    }
                    LogUtil.logError(exceptions);
                } else {
                    System.out.println(report.getHash());
                    if (showSummary) {
                        System.err.println("Total upload time: " + Text.getPrettyEllapsedTimeString(report.getTimeToFinish()));
                        System.err.println("Total bytes uploaded: " + Text.getFormattedBytes(report.getBytesUploaded()));
                    }
                }
            } catch (Exception e) {
                System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                LogUtil.logError(e);
                if (!TestUtil.isTesting()) {
                    if (e instanceof FileNotFoundException) {
                        System.exit(4);
                    } else {
                        System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                        e.printStackTrace(System.err);
                        System.exit(1);
                    }
                } else {
                    throw e;
                }
            }

            // exit
            if (!TestUtil.isTesting()) {
                System.exit(0);
            } else {
                return;
            }
        } catch (Exception e) {
            debugErr(e);
            if (!TestUtil.isTesting()) {
                System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(1);
            } else {
                throw e;
            }
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static void setDebug(boolean debug) {
        AddFileTool.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    protected static void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(AddFileTool.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    protected static void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }

    private class FileToUpload {

        public String fileName;
        public String relativeName;
        public String absoluteFilePath;
        public final byte[] padding;
        public final boolean isPrimaryFile;
        public final boolean isProjectFile;
        public final ProjectFile projectFile;
        public final boolean isLicenseFile;
        public boolean failed = false;
        private int dataChunksUploaded = 0;
        private int dataChunksToUpload = 0;

        public FileToUpload(String relativeName, File file, byte[] padding, boolean isPrimaryFile, boolean isLicenseFile) {
            this.relativeName = relativeName;
            this.absoluteFilePath = file.getAbsolutePath();
            this.fileName = file.getName();
            this.padding = padding;
            this.isPrimaryFile = isPrimaryFile;
            this.isProjectFile = false;
            this.projectFile = null;
            this.isLicenseFile = isLicenseFile;
        }

        public FileToUpload(ProjectFile projectFile, byte[] padding) {
            this.relativeName = "Project File";
            this.absoluteFilePath = null;
            this.fileName = ProjectFile.DEFAULT_PROJECT_FILE_NAME;
            this.padding = padding;
            this.isPrimaryFile = true;
            this.isProjectFile = true;
            this.projectFile = projectFile;
            this.isLicenseFile = false;
        }

        public File getFile() {
            if (absoluteFilePath == null && isProjectFile) {
                File file = TempFileUtil.createTemporaryFile();
                FileOutputStream fos = null;
                BufferedOutputStream bos = null;
                try {
                    fos = new FileOutputStream(file);
                    bos = new BufferedOutputStream(fos);
                    ProjectFileUtil.write(projectFile, bos);
                    absoluteFilePath = file.getAbsolutePath();
                } catch (Exception e) {
                    debugErr(e);
                } finally {
                    IOUtil.safeClose(bos);
                    IOUtil.safeClose(fos);
                }
            }
            return new File(absoluteFilePath);
        }

        public void setDataChunksToUpload(int dataChunksToUpload) {
            this.dataChunksToUpload = dataChunksToUpload;
        }

        public int getDataChunksToUpload() {
            return dataChunksToUpload;
        }

        public int getDataChunksUploaded() {
            return dataChunksUploaded;
        }

        public void incrementDataChunksUploaded() {
            dataChunksUploaded++;
        }

        public void fail() {
            failed = true;
        }

        public boolean isFailed() {
            return failed;
        }
    }

    private final class DataChunk implements Comparable<DataChunk> {

        public final MetaChunk metaChunk;
        public final BigHash hash;
        public byte[] bytes;
        public short serversTried = 0;

        public DataChunk(MetaChunk metaChunk, BigHash hash) {
            this.metaChunk = metaChunk;
            this.hash = hash;
        }

        /**
         *
         * @param o
         * @return
         */
        public final int compareTo(DataChunk c) {
            if (serversTried < c.serversTried) {
                return 1;
            } else if (serversTried > c.serversTried) {
                return -1;
            }
            return 0;
        }

        /**
         *
         */
        public final void incrementServersTried() {
            serversTried++;
        }
    }

    private final class MetaChunk implements Comparable<MetaChunk> {

        public final FileToUpload fileToUpload;
        public final MetaData metaData;
        public BigHash hash;
        public final ArrayList<BigHash> chunkHashes = new ArrayList<BigHash>();
        public short serversTried = 0;

        public MetaChunk(FileToUpload fileToUpload) {
            this.fileToUpload = fileToUpload;
            this.metaData = new MetaData();
        }

        /**
         * <p>Need to merge with the existing meta data on the network if it exists in case we will be setting to a server that does not already have the meta data on it.</p>
         */
        public final void mergeFromNetwork() {
            long startTime = TimeUtil.getTrancheTimestamp();
            try {
                MetaData metaDataFromNetwork = null;
                GetFileTool getFileTool = new GetFileTool();
                getFileTool.setHash(hash);
                getFileTool.setSuppressFailedChunkOutput(true);
                try {
                    metaDataFromNetwork = getFileTool.getMetaData();
                } catch (Exception e) {
                }
                if (metaDataFromNetwork == null) {
                    return;
                }
                if (!metaDataFromNetwork.containsUploader(metaData.getSignature().getUserName(), metaData.getTimestampUploaded(), metaData.getRelativePathInDataSet())) {
                    metaDataFromNetwork.addUploader(metaData.getSignature(), (ArrayList) metaData.getEncodings(), metaData.getProperties(), (ArrayList) metaData.getAnnotations());
                }
                metaDataFromNetwork.setParts((ArrayList) metaData.getParts());
            } catch (Exception e) {
                debugErr(e);
            }
            debugOut("Time spent merging meta data from network: " + (TimeUtil.getTrancheTimestamp() - startTime));
        }

        /**
         *
         * @param o
         * @return
         */
        public final int compareTo(MetaChunk c) {
            if (serversTried < c.serversTried) {
                return 1;
            } else if (serversTried > c.serversTried) {
                return -1;
            }
            return 0;
        }

        /**
         *
         */
        public final void incrementServersTried() {
            serversTried++;
        }
    }

    private final class FileEncodingThread extends Thread {

        private final byte[] buffer = new byte[DEFAULT_SIZE_FILE_ENCODING_BUFFER];
        private final Set<FileEncodingThread> fileThreads;
        private final Set<DataUploadingThread> dataThreads;
        private final Set<MetaDataUploadingThread> metaThreads;
        private int emptyCount = 0;
        private boolean started = false, finished = false, stopped = false;
        private final LinkedList<FileToUpload> fileStack;
        private final PriorityBlockingQueue<DataChunk> dataChunkQueue;
        public BigHash primaryFileHash;
        private final long startTimestamp;

        public FileEncodingThread(Set<FileEncodingThread> fileThreads, Set<DataUploadingThread> dataThreads, Set<MetaDataUploadingThread> metaThreads, LinkedList<FileToUpload> fileStack, PriorityBlockingQueue<DataChunk> dataChunkQueue, long startTimestamp) {
            setName("File Encoding Thread");
            setDaemon(true);
            this.fileThreads = fileThreads;
            this.dataThreads = dataThreads;
            this.metaThreads = metaThreads;
            this.fileStack = fileStack;
            this.dataChunkQueue = dataChunkQueue;
            this.startTimestamp = startTimestamp;
        }

        /**
         *
         */
        public final void halt() {
            debugOut("Halting file encoding thread.");
            stopped = true;
        }

        /**
         *
         * @return
         */
        public synchronized final void waitForFinish() {
            debugOut("Waiting for file encoding thread to finish.");
            while (!started || !finished) {
                try {
                    wait();
                } catch (Exception e) {
                    debugErr(e);
                }
            }
        }

        /**
         *
         * @param fileToUpload
         * @param e
         */
        private final void failFile(FileToUpload fileToUpload, Exception e) {
            Set<PropagationExceptionWrapper> exceptions = new HashSet<PropagationExceptionWrapper>();
            exceptions.add(new PropagationExceptionWrapper(e));
            failFile(fileToUpload, exceptions);
        }

        /**
         *
         * @param fileToUpload
         * @param propagationExceptions
         */
        private final void failFile(FileToUpload fileToUpload, Collection<PropagationExceptionWrapper> exceptions) {
            if (fileToUpload != null && !fileToUpload.failed) {
                fileToUpload.failed = true;
                fireFailedFile(fileToUpload.getFile(), fileToUpload.relativeName, exceptions);
            }
            for (FileEncodingThread thread : fileThreads) {
                thread.halt();
            }
            for (MetaDataUploadingThread thread : metaThreads) {
                thread.halt();
            }
            for (DataUploadingThread thread : dataThreads) {
                thread.halt();
            }
        }

        /**
         * 
         */
        @Override
        public void run() {
            started = true;
            try {
                DOWHILE:
                do {
                    // get the next file
                    FileToUpload fileToUpload = null;
                    synchronized (fileStack) {
                        if (!fileStack.isEmpty()) {
                            emptyCount = 0;
                            fileToUpload = fileStack.removeFirst();
                        } else {
                            emptyCount++;
                            // wait until it's time to submit the project file
                            if (emptyCount > 3 && !projectFileAddedToStack && projectFile != null && !dataOnly && projectFile.getParts().size() == timeEstimator.getTotalFiles()) {
                                projectFileAddedToStack = true;
                                fileStack.addLast(new FileToUpload(projectFile, padding));
                                continue;
                            } else if (emptyCount <= 3) {
                                ThreadUtil.safeSleep(250);
                                continue;
                            }
                            debugOut("File stack is empty.");
                            break;
                        }
                    }
                    debugOut("File taken from stack: " + fileToUpload.relativeName);
                    if (fileToUpload.getFile().isDirectory()) {
                        for (File subFile : fileToUpload.getFile().listFiles()) {
                            synchronized (fileStack) {
                                fileStack.addFirst(new FileToUpload(relativeName(file, subFile), subFile, fileToUpload.padding, false, false));
                            }
                            if (stopped || AddFileTool.this.stopped) {
                                break DOWHILE;
                            }
                        }
                    } else {
                        try {
                            // if exploding, check for compressed files
                            if (isExplodeBeforeUpload()) {
                                String fileName = fileToUpload.getFile().getName().toLowerCase();
                                if (fileName.endsWith(".zip")) {
                                    // need to allow for the explosion of single-file uploads
                                    if (file.isDirectory()) {
                                        // subtract the file's size from bytes to upload
                                        timeEstimator.subtractTotalBytes(fileToUpload.getFile().length());
                                        timeEstimator.subtractTotalFiles(1);
                                        // untar
                                        File directory = CompressionUtil.zipDecompress(fileToUpload.getFile(), fileToUpload.getFile().getName().substring(0, fileToUpload.getFile().getName().length() - 4));
                                        // put all the contents of the tar at the head of the line
                                        for (File subFile : directory.listFiles()) {
                                            // inc bytes to upload appropriately
                                            timeEstimator.addTotalBytes(subFile.length());
                                            timeEstimator.addTotalFiles(1);
                                            // add to the stack
                                            synchronized (fileStack) {
                                                fileStack.addFirst(new FileToUpload(fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 4) + "/" + subFile.getName(), subFile, fileToUpload.padding, false, false));
                                            }
                                        }
                                        // do not upload this file
                                        continue;
                                    }
                                } else if (fileName.endsWith(".gzip") || fileName.endsWith(".gz") || fileName.endsWith(".tgz")) {
                                    // subtract the file's size from bytes to upload
                                    timeEstimator.subtractTotalBytes(fileToUpload.getFile().length());
                                    // make up an appropriate relative name
                                    if (fileName.endsWith(".gzip")) {
                                        fileToUpload.relativeName = fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 5);
                                        fileToUpload.fileName = fileToUpload.fileName.substring(0, fileToUpload.fileName.length() - 5);
                                    } else if (fileName.endsWith(".gz")) {
                                        fileToUpload.relativeName = fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 3);
                                        fileToUpload.fileName = fileToUpload.fileName.substring(0, fileToUpload.fileName.length() - 3);
                                    } else if (fileName.endsWith(".tgz")) {
                                        fileToUpload.relativeName = fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 4);
                                        fileToUpload.relativeName = fileToUpload.relativeName + ".tar";
                                        fileToUpload.fileName = fileToUpload.fileName.substring(0, fileToUpload.fileName.length() - 4);
                                        fileToUpload.fileName = fileToUpload.fileName + ".tar";
                                        fileName = fileName.substring(0, fileName.length() - 4);
                                        fileName = fileName + ".tar";
                                    }
                                    // make a temp file
                                    fileToUpload.absoluteFilePath = CompressionUtil.gzipDecompress(fileToUpload.getFile()).getAbsolutePath();
                                    // inc bytes to upload appropriately
                                    timeEstimator.addTotalBytes(fileToUpload.getFile().length());
                                } else if (fileName.endsWith(".bzip") || fileName.endsWith(".bzip2") || fileName.endsWith(".bz") || fileName.endsWith(".bz2") || fileName.endsWith(".tbz2") || fileName.endsWith(".tbz")) {
                                    // subtract the file's size from bytes to upload
                                    timeEstimator.subtractTotalBytes(fileToUpload.getFile().length());
                                    // make up an appropriate relative name
                                    if (fileName.endsWith(".bzip2")) {
                                        fileToUpload.relativeName = fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 6);
                                        fileToUpload.fileName = fileToUpload.fileName.substring(0, fileToUpload.fileName.length() - 6);
                                    } else if (fileName.endsWith(".bzip")) {
                                        fileToUpload.relativeName = fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 5);
                                        fileToUpload.fileName = fileToUpload.fileName.substring(0, fileToUpload.fileName.length() - 5);
                                    } else if (fileName.endsWith(".bz2")) {
                                        fileToUpload.relativeName = fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 4);
                                        fileToUpload.fileName = fileToUpload.fileName.substring(0, fileToUpload.fileName.length() - 4);
                                    } else if (fileName.endsWith(".bz")) {
                                        fileToUpload.relativeName = fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 3);
                                        fileToUpload.fileName = fileToUpload.fileName.substring(0, fileToUpload.fileName.length() - 3);
                                    } else if (fileName.endsWith(".tbz2")) {
                                        fileToUpload.relativeName = fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 5);
                                        fileToUpload.relativeName = fileToUpload.relativeName + ".tar";
                                        fileToUpload.fileName = fileToUpload.fileName.substring(0, fileToUpload.fileName.length() - 5);
                                        fileToUpload.fileName = fileToUpload.fileName + ".tar";
                                    } else if (fileName.endsWith(".tbz")) {
                                        fileToUpload.relativeName = fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 4);
                                        fileToUpload.relativeName = fileToUpload.relativeName + ".tar";
                                        fileToUpload.fileName = fileToUpload.fileName.substring(0, fileToUpload.fileName.length() - 4);
                                        fileToUpload.fileName = fileToUpload.fileName + ".tar";
                                        fileName = fileName.substring(0, fileName.length() - 4);
                                        fileName = fileName + ".tar";
                                    }
                                    // make a temp file
                                    fileToUpload.absoluteFilePath = CompressionUtil.bzip2Decompress(fileToUpload.getFile()).getAbsolutePath();
                                    // inc bytes to upload appropriately
                                    timeEstimator.addTotalBytes(fileToUpload.getFile().length());
                                } else if (fileName.endsWith(".lzma") || fileName.endsWith(".lz")) {
                                    // subtract the file's size from bytes to upload
                                    timeEstimator.subtractTotalBytes(fileToUpload.getFile().length());
                                    // make up an appropriate relative name
                                    if (fileName.endsWith(".lzma")) {
                                        fileToUpload.relativeName = fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 5);
                                        fileToUpload.fileName = fileToUpload.fileName.substring(0, fileToUpload.fileName.length() - 5);
                                    } else if (fileName.endsWith(".lz")) {
                                        fileToUpload.relativeName = fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 3);
                                        fileToUpload.fileName = fileToUpload.fileName.substring(0, fileToUpload.fileName.length() - 3);
                                    }
                                    // make a temp file
                                    fileToUpload.absoluteFilePath = CompressionUtil.lzmaDecompress(fileToUpload.getFile()).getAbsolutePath();
                                    // inc bytes to upload appropriately
                                    timeEstimator.addTotalBytes(fileToUpload.getFile().length());
                                }
                                // if exploding, check for tar compression
                                // might be falling through from a GZIP, LZMA, or BZIP2 decompression of a tarred file
                                if (fileName.endsWith(".tar")) {
                                    // need to allow for the explosion of single-file uploads
                                    if (file.isDirectory()) {
                                        // subtract the file's size from bytes to upload
                                        timeEstimator.subtractTotalBytes(fileToUpload.getFile().length());
                                        timeEstimator.subtractTotalFiles(1);
                                        // untar
                                        File directory = CompressionUtil.tarDecompress(fileToUpload.getFile(), fileToUpload.getFile().getName().substring(0, fileToUpload.getFile().getName().length() - 4));
                                        // put all the contents of the tar at the head of the line
                                        for (File subFile : directory.listFiles()) {
                                            // inc bytes to upload appropriately
                                            timeEstimator.addTotalBytes(subFile.length());
                                            timeEstimator.addTotalFiles(1);
                                            // add to the stack
                                            synchronized (fileStack) {
                                                fileStack.addFirst(new FileToUpload(fileToUpload.relativeName.substring(0, fileToUpload.relativeName.length() - 4) + "/" + subFile.getName(), subFile, fileToUpload.padding, false, false));
                                            }
                                        }
                                        // do not upload this file
                                        continue;
                                    }
                                }
                            }

                            // stopped
                            if (stopped || AddFileTool.this.stopped) {
                                break DOWHILE;
                            }

                            // start the file
                            fireStartedFile(fileToUpload.getFile(), fileToUpload.relativeName);

                            // make a meta data chunk
                            MetaChunk metaChunk = new MetaChunk(fileToUpload);

                            // chunk the file
                            FileInputStream fis = null;
                            BufferedInputStream bis = null;
                            ChunkQueueingStream cqs = null;
                            AESEncodingStream aes = null;
                            GZIPEncodingStream gzip = null;
                            WrappedOutputStream nos = null;
                            try {
                                LinkedList<OutputStream> hashStreams = new LinkedList<OutputStream>();

                                // set up the sreams
                                fis = new FileInputStream(fileToUpload.getFile());
                                bis = new BufferedInputStream(fis);
                                cqs = new ChunkQueueingStream(this, dataChunkQueue, metaChunk);
                                OutputStream currentStream = cqs;
                                if (passphrase != null) {
                                    debugOut("Encoding file with passphrase: " + passphrase);
                                    aes = new AESEncodingStream(passphrase, currentStream);
                                    currentStream = aes;
                                    hashStreams.addFirst(aes);
                                }
                                if (compress) {
                                    gzip = new GZIPEncodingStream(currentStream);
                                    currentStream = gzip;
                                    hashStreams.addFirst(gzip);
                                }

                                // calculate the original hash
                                BigHashMaker noneBigHashMaker = new BigHashMaker();
                                nos = new WrappedOutputStream(currentStream, noneBigHashMaker);
                                hashStreams.addFirst(nos);

                                // write all of the data through the stream chain
                                for (int bytesRead = bis.read(buffer); bytesRead != -1; bytesRead = bis.read(buffer)) {
                                    // break point
                                    if (stopped || AddFileTool.this.stopped) {
                                        break DOWHILE;
                                    }
                                    nos.write(buffer, 0, bytesRead);
                                }
                                // if padding is used, add it -- always added to the end of files
                                if (padding.length != 0) {
                                    nos.write(padding);
                                }

                                // Don't safe close -- propagationExceptions are legitimate concerns
                                nos.flush();
                                nos.close();

                                // set the # of data chunks to upload
                                metaChunk.fileToUpload.setDataChunksToUpload(cqs.dataChunksQueued);

                                // make the file encoding objects
                                ArrayList<FileEncoding> encodings = new ArrayList<FileEncoding>();
                                BigHash fileHash = BigHash.createFromBytes(noneBigHashMaker.finish());
                                metaChunk.hash = fileHash;
                                encodings.add(new FileEncoding(FileEncoding.NONE, fileHash));
                                if (gzip != null) {
                                    encodings.add(new FileEncoding(FileEncoding.GZIP, gzip.getHash()));
                                }
                                if (aes != null) {
                                    encodings.add(new FileEncoding(FileEncoding.AES, aes.getHash()));
                                }

                                // set up the meta data
                                Map<String, String> properties = new HashMap<String, String>();
                                properties.put(MetaData.PROP_NAME, fileToUpload.fileName);
                                properties.put(MetaData.PROP_TIMESTAMP_UPLOADED, String.valueOf(startTimestamp));
                                properties.put(MetaData.PROP_TIMESTAMP_FILE, String.valueOf(fileToUpload.getFile().lastModified()));
                                if (!metaChunk.fileToUpload.isPrimaryFile) {
                                    properties.put(MetaData.PROP_PATH_IN_DATA_SET, fileToUpload.relativeName);
                                }

                                // create a signature for the file
                                Signature signature = new Signature(SecurityUtil.sign(fileToUpload.getFile(), userPrivateKey), SecurityUtil.getSignatureAlgorithm(userPrivateKey), userCertificate);

                                // annotations
                                ArrayList<MetaDataAnnotation> annotations = new ArrayList<MetaDataAnnotation>();
                                if (metaChunk.fileToUpload.isPrimaryFile) {
                                    annotations.addAll(metaDataAnnotations);
                                }

                                // special case of the project file
                                if (fileToUpload.isProjectFile) {
                                    metaChunk.metaData.setIsProjectFile(true);
                                    // make the title and description publicly available if it is OK
                                    if (passphrase == null || (passphrase != null && showMetaDataIfEncrypted)) {
                                        properties.put(MetaData.PROP_DATA_SET_NAME, fileToUpload.projectFile.getName());
                                        properties.put(MetaData.PROP_DATA_SET_DESCRIPTION, fileToUpload.projectFile.getDescription());
                                    }
                                    // calculate final size and save to meta data
                                    long size = 0;
                                    for (ProjectFilePart pfp : fileToUpload.projectFile.getParts()) {
                                        size += pfp.getPaddingAdjustedLength();
                                    }
                                    properties.put(MetaData.PROP_DATA_SET_SIZE, String.valueOf(size));
                                    properties.put(MetaData.PROP_DATA_SET_FILES, String.valueOf(fileToUpload.projectFile.getParts().size()));
                                }

                                // Add sticky servers to meta data object
                                synchronized (serverHostStickySet) {
                                    for (String stickyServer : serverHostStickySet) {
                                        // Add just the host information (IP address) since port / SSL status might changes
                                        annotations.add(new MetaDataAnnotation(MetaDataAnnotation.PROP_STICKY_SERVER_HOST, IOUtil.parseHost(stickyServer)));
                                    }
                                }

                                // add the uploader to the meta data
                                metaChunk.metaData.addUploader(signature, encodings, properties, annotations);

                                // set the license hash in the project file
                                if (fileToUpload.isLicenseFile) {
                                    projectFile.setLicenseHash(fileHash);
                                }

                                // set primary file hash
                                if (fileToUpload.isPrimaryFile) {
                                    primaryFileHash = fileHash;
                                }

                                // add this file to the project file
                                if (projectFile != null && !fileToUpload.isProjectFile) {
                                    projectFile.addPart(new ProjectFilePart(fileToUpload.relativeName, fileHash, fileToUpload.padding));
                                }
                            } finally {
                                IOUtil.safeClose(cqs);
                                IOUtil.safeClose(gzip);
                                IOUtil.safeClose(aes);
                                IOUtil.safeClose(bis);
                                IOUtil.safeClose(fis);
                            }
                        } catch (Exception e) {
                            failFile(fileToUpload, e);
                            debugErr(e);
                        }
                    }
                } while (!stopped && !AddFileTool.this.stopped);
            } catch (Exception e) {
                debugErr(e);
            } finally {
                finished = true;
                synchronized (FileEncodingThread.this) {
                    notifyAll();
                }
            }
        }
    }

    private final class ChunkQueueingStream extends OutputStream {

        private FileEncodingThread thread;
        private byte[] buffer = new byte[DataBlockUtil.ONE_MB];
        private byte[] buf = new byte[1];
        private int bufferOffset = 0;
        private final PriorityBlockingQueue<DataChunk> dataChunkQueue;
        private final MetaChunk metaChunk;
        public int dataChunksQueued = 0;

        public ChunkQueueingStream(FileEncodingThread thread, PriorityBlockingQueue<DataChunk> dataChunkQueue, MetaChunk metaChunk) {
            this.thread = thread;
            this.dataChunkQueue = dataChunkQueue;
            this.metaChunk = metaChunk;
        }

        /**
         * 
         * @throws java.lang.OutOfMemoryError
         */
        private final void queueBufferContents() throws OutOfMemoryError {
            if (bufferOffset > 0) {
                // create a data chunk
                DataChunk chunk = new DataChunk(metaChunk, new BigHash(buffer, bufferOffset));

                // need to copy the bytes
                chunk.bytes = new byte[bufferOffset];
                System.arraycopy(buffer, 0, chunk.bytes, 0, bufferOffset);

                long startTime = TimeUtil.getTrancheTimestamp();
                // wait until there is room
                while (dataChunkQueue.size() >= DEFAULT_DATA_QUEUE_SIZE) {
                    // break point
                    if (thread.stopped || AddFileTool.this.stopped) {
                        return;
                    }
                    ThreadUtil.safeSleep(100);
                }
                // data chunks queued
                dataChunksQueued++;
                debugOut("Queueing " + chunk.hash + " (total " + dataChunksQueued + ")");
                // add the to the list of chunks for the meta data
                metaChunk.chunkHashes.add(chunk.hash);
                // add chunk to the queue
                dataChunkQueue.put(chunk);
                debugOut("Time spent waiting for room in the data queue: " + (TimeUtil.getTrancheTimestamp() - startTime));
                // reset the offset
                bufferOffset = 0;
            }
        }

        /**
         * <p>Write bytes to network.</p>
         * @param b
         * @param off
         * @param len
         * @throws java.io.IOException
         */
        @Override()
        public final void write(byte[] b, int off, int len) throws IOException {
            for (int i = 0; i < len; i++) {
                buffer[bufferOffset] = b[i];
                bufferOffset++;
                // if full, upload the chunk
                if (bufferOffset >= buffer.length) {
                    queueBufferContents();
                }
            }
        }

        /**
         * <p>Write bytes to network.</p>
         * @param b
         * @throws java.io.IOException
         */
        @Override()
        public final void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        /**
         * 
         * @param b
         * @throws java.io.IOException
         */
        @Override
        public final void write(int b) throws IOException {
            buf[0] = (byte) b;
            write(buf);
        }

        /**
         * 
         * @throws java.io.IOException
         */
        @Override()
        public final void flush() throws IOException {
        }

        /**
         * 
         * @throws java.io.IOException
         */
        @Override()
        public final void close() throws IOException {
            queueBufferContents();
        }
    }

    private final class DataUploadingThread extends Thread {

        private final Set<FileEncodingThread> fileThreads;
        private final Set<DataUploadingThread> dataThreads;
        private final Set<MetaDataUploadingThread> metaThreads;
        private boolean started = false, finished = false, stopWhenFinished = false, stopped = false;
        private final PriorityBlockingQueue<DataChunk> dataChunkQueue;
        private final PriorityBlockingQueue<MetaChunk> metaChunkQueue;

        public DataUploadingThread(Set<FileEncodingThread> fileThreads, Set<DataUploadingThread> dataThreads, Set<MetaDataUploadingThread> metaThreads, PriorityBlockingQueue<DataChunk> dataChunkQueue, PriorityBlockingQueue<MetaChunk> metaChunkQueue) {
            setName("Data Uploading Thread");
            setDaemon(true);
            this.fileThreads = fileThreads;
            this.dataThreads = dataThreads;
            this.metaThreads = metaThreads;
            this.dataChunkQueue = dataChunkQueue;
            this.metaChunkQueue = metaChunkQueue;
        }

        /**
         *
         */
        public final void halt() {
            debugOut("Halting data uploading thread.");
            stopped = true;
        }

        /**
         *
         * @return
         */
        public synchronized final void waitForFinish() {
            stopWhenFinished = true;
            while (!started || !finished) {
                try {
                    wait();
                } catch (Exception e) {
                    debugErr(e);
                }
            }
        }

        /**
         * 
         * @param dataChunk
         * @param host
         * @param hosts
         * @return
         * @throws Exception
         */
        private final Collection<PropagationExceptionWrapper> upload(DataChunk dataChunk, TrancheServer ts, String[] hosts) throws Exception {
            debugOut("Starting to upload chunk to " + ts.getHost() + ": " + dataChunk.hash);
            fireTryingData(dataChunk.metaChunk.fileToUpload.relativeName, dataChunk.metaChunk.fileToUpload.getFile(), dataChunk.hash, ts.getHost());
            long startTime = TimeUtil.getTrancheTimestamp();
            PropagationReturnWrapper wrapper = IOUtil.setData(ts, userCertificate, userPrivateKey, dataChunk.hash, dataChunk.bytes, hosts);
            for (PropagationExceptionWrapper pew : wrapper.getErrors()) {
                debugOut(pew.toString());
            }
            debugOut("Time spent uploading a data chunk: " + (TimeUtil.getTrancheTimestamp() - startTime));
            return wrapper.getErrors();
        }

        /**
         *
         * @param chunk
         * @param e
         */
        private final void failChunk(DataChunk chunk, Exception e) {
            debugErr(e);
            Set<PropagationExceptionWrapper> exceptions = new HashSet<PropagationExceptionWrapper>();
            exceptions.add(new PropagationExceptionWrapper(e));
            failChunk(chunk, exceptions);
        }

        /**
         *
         * @param metaChunk
         * @param propagationExceptions
         */
        private final void failChunk(DataChunk chunk, Collection<PropagationExceptionWrapper> exceptions) {
            // fail the file
            if (chunk.metaChunk.fileToUpload != null && !chunk.metaChunk.fileToUpload.failed) {
                chunk.metaChunk.fileToUpload.failed = true;
                fireFailedFile(chunk.metaChunk.fileToUpload.getFile(), chunk.metaChunk.fileToUpload.relativeName, exceptions);
            }
            fireFailedData(chunk.metaChunk.fileToUpload.relativeName, chunk.metaChunk.fileToUpload.getFile(), chunk.hash, exceptions);
            for (FileEncodingThread thread : fileThreads) {
                thread.halt();
            }
            for (MetaDataUploadingThread thread : metaThreads) {
                thread.halt();
            }
            for (DataUploadingThread thread : dataThreads) {
                thread.halt();
            }
        }

        /**
         *
         */
        @Override
        public void run() {
            started = true;
            try {
                DataChunk dataChunk = null;
                DOWHILE:
                do {
                    // get the next data chunk
                    long startTime = TimeUtil.getTrancheTimestamp();
                    do {
                        dataChunk = dataChunkQueue.poll(500, TimeUnit.MILLISECONDS);
                    } while (dataChunk == null && !stopWhenFinished && !stopped && !AddFileTool.this.stopped);
                    debugOut("Time spent waiting for a data chunk: " + (TimeUtil.getTrancheTimestamp() - startTime));
                    debugOut("Data chunk queue size: " + dataChunkQueue.size());

                    // break point
                    if (stopped || AddFileTool.this.stopped) {
                        break;
                    }

                    // if this file failed to upload, do not bother to upload
                    if (dataChunk == null || dataChunk.metaChunk.fileToUpload.isFailed()) {
                        debugOut("Data chunk is null or the file is failed.");
                        continue;
                    }

                    try {
                        fireStartedData(dataChunk.metaChunk.fileToUpload.relativeName, dataChunk.metaChunk.fileToUpload.getFile(), dataChunk.hash);

                        // get servers
                        Collection<String> coreHosts = getCoreServersToUploadTo(dataChunk.hash);
                        Collection<String> nonCoreHosts = getNonCoreServersToUploadTo(dataChunk.hash);
                        if (coreHosts.isEmpty() && nonCoreHosts.isEmpty()) {
                            throw new Exception("No servers to which to upload.");
                        }

                        // upload to core using the multi server request strategy
                        Collection<MultiServerRequestStrategy> strategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(coreHosts, Tertiary.DONT_CARE, Tertiary.TRUE);
                        if (strategies.isEmpty()) {
                            throw new Exception("Failed to find an upload strategy for " + dataChunk.hash.toString());
                        }
                        debugOut(strategies.size() + " strategies found.");

                        Set<String> toUploadCoreHosts = new HashSet<String>(coreHosts);
                        Set<String> uploadedCoreHosts = new HashSet<String>();
                        for (MultiServerRequestStrategy strategy : strategies) {
                            // break point
                            if (stopped || AddFileTool.this.stopped) {
                                break DOWHILE;
                            }
                            // upload
                            try {
                                TrancheServer ts = ConnectionUtil.connectHost(strategy.getHostReceivingRequest(), true);
                                if (ts == null) {
                                    throw new Exception("Could not connect to " + strategy.getHostReceivingRequest() + " to verify the upload of data " + dataChunk.hash.toString().substring(0, 5) + "...");
                                }
                                try {
                                    upload(dataChunk, ts, toUploadCoreHosts.toArray(new String[0]));
                                } catch (Exception e) {
                                    debugErr(e);
                                    ConnectionUtil.reportExceptionHost(strategy.getHostReceivingRequest(), e);
                                } finally {
                                    ConnectionUtil.unlockConnection(strategy.getHostReceivingRequest());
                                }
                            } catch (Exception e) {
                                debugErr(e);
                            }
                            // verify
                            Collection<String> toUploadCoreHostsCopy = new HashSet<String>(toUploadCoreHosts);
                            for (String host : toUploadCoreHostsCopy) {
                                // break point
                                if (stopped || AddFileTool.this.stopped) {
                                    break DOWHILE;
                                }
                                try {
                                    TrancheServer ts = ConnectionUtil.connectHost(host, true);
                                    if (ts == null) {
                                        throw new Exception("Could not connect to " + host + " to verify the upload of data " + dataChunk.hash.toString().substring(0, 5) + "...");
                                    }
                                    try {
                                        if (IOUtil.hasData(ts, dataChunk.hash)) {
                                            uploadedCoreHosts.add(host);
                                            toUploadCoreHosts.remove(host);
                                            fireUploadedData(dataChunk.metaChunk.fileToUpload.relativeName, dataChunk.metaChunk.fileToUpload.getFile(), dataChunk.hash, host);
                                        }
                                    } catch (Exception e) {
                                        debugErr(e);
                                        ConnectionUtil.reportExceptionHost(host, e);
                                    } finally {
                                        ConnectionUtil.unlockConnection(host);
                                    }
                                } catch (Exception e) {
                                    debugErr(e);
                                }
                            }
                            // have we uploaded enough copies?
                            if (uploadedCoreHosts.size() >= ConfigureTranche.getInt(ConfigureTranche.PROP_REPLICATIONS) || toUploadCoreHosts.isEmpty()) {
                                break;
                            }
                        }

                        // upload to non-core hosts directly
                        Set<String> uploadedNonCoreHosts = new HashSet<String>();
                        for (String host : nonCoreHosts) {
                            // break point
                            if (stopped || AddFileTool.this.stopped) {
                                break DOWHILE;
                            }
                            try {
                                TrancheServer ts = ConnectionUtil.connectHost(host, true);
                                if (ts == null) {
                                    throw new Exception("Could not connect to " + host + ".");
                                }
                                try {
                                    upload(dataChunk, ts, new String[]{host});
                                    if (IOUtil.hasData(ts, dataChunk.hash)) {
                                        debugOut("Verified upload of data chunk " + dataChunk.hash + " to " + ts.getHost() + ".");
                                        uploadedNonCoreHosts.add(host);
                                        fireUploadedData(dataChunk.metaChunk.fileToUpload.relativeName, dataChunk.metaChunk.fileToUpload.getFile(), dataChunk.hash, host);
                                    }
                                } catch (Exception e) {
                                    debugErr(e);
                                    ConnectionUtil.reportExceptionHost(host, e);
                                } finally {
                                    ConnectionUtil.unlockConnection(host);
                                }
                            } catch (Exception e) {
                                debugErr(e);
                                ConnectionUtil.reportExceptionHost(host, e);
                            }
                        }

                        // checks
                        int reps = ConfigureTranche.getInt(ConfigureTranche.PROP_REPLICATIONS);
                        if (uploadedCoreHosts.size() < reps) {
                            throw new Exception("Data uploaded to " + uploadedCoreHosts.size() + " core servers, but required number is " + reps);
                        }
                        if (nonCoreHosts.size() != uploadedNonCoreHosts.size()) {
                            throw new Exception("Data uploaded to " + uploadedNonCoreHosts.size() + " non-core servers, but expected number is " + nonCoreHosts.size());
                        }

                        fireFinishedData(dataChunk.metaChunk.fileToUpload.relativeName, dataChunk.metaChunk.fileToUpload.getFile(), dataChunk.hash);
                        // if this is the last data chunk, submit the meta chunk to the upload queue
                        dataChunk.metaChunk.fileToUpload.incrementDataChunksUploaded();
                        if (dataChunk.metaChunk.fileToUpload.getDataChunksToUpload() != 0 && dataChunk.metaChunk.fileToUpload.getDataChunksUploaded() >= dataChunk.metaChunk.fileToUpload.getDataChunksToUpload()) {
                            debugOut("Submitting meta data chunk to queue.");

                            // sleep until the file encoding thread is finished with this file (last action is to set the uploader)
                            while (dataChunk.metaChunk.metaData.getUploaderCount() == 0 && !dataChunk.metaChunk.fileToUpload.isFailed()) {
                                // break point
                                if (stopped || AddFileTool.this.stopped) {
                                    break DOWHILE;
                                }
                                debugOut("Waiting for file upload thread to finish with meta data.");
                                ThreadUtil.safeSleep(100);
                            }

                            debugOut("Time spent waiting for the file encoding thread to finish with this file: " + (TimeUtil.getTrancheTimestamp() - startTime));
                            // add the chunks uploaded
                            dataChunk.metaChunk.metaData.setParts(dataChunk.metaChunk.chunkHashes);
                            // queue the meta data chunk
                            if (!dataOnly) {
                                long startTime2 = TimeUtil.getTrancheTimestamp();
                                // wait until there is room
                                while (metaChunkQueue.size() >= DEFAULT_META_DATA_QUEUE_SIZE) {
                                    // break point
                                    if (stopped || AddFileTool.this.stopped) {
                                        break DOWHILE;
                                    }
                                    debugOut("Waiting for room in meta data queue.");
                                    ThreadUtil.safeSleep(100);
                                }
                                metaChunkQueue.put(dataChunk.metaChunk);
                                debugOut("Time spent waiting to submit to the meta data queue: " + (TimeUtil.getTrancheTimestamp() - startTime2));
                            }
                        }
                    } catch (Exception e) {
                        failChunk(dataChunk, e);
                    }
                } while (!(dataChunk == null && dataChunkQueue.isEmpty() && stopWhenFinished) && !stopped && !AddFileTool.this.stopped);
            } catch (Exception e) {
                debugErr(e);
            } finally {
                finished = true;
                synchronized (DataUploadingThread.this) {
                    notifyAll();
                }
            }
        }
    }

    private final class MetaDataUploadingThread extends Thread {

        private final Set<FileEncodingThread> fileThreads;
        private final Set<DataUploadingThread> dataThreads;
        private final Set<MetaDataUploadingThread> metaThreads;
        private boolean started = false, finished = false, stopWhenFinished = false, stopped = false;
        private final PriorityBlockingQueue<MetaChunk> metaChunkQueue;

        public MetaDataUploadingThread(Set<FileEncodingThread> fileThreads, Set<DataUploadingThread> dataThreads, Set<MetaDataUploadingThread> metaThreads, PriorityBlockingQueue<MetaChunk> metaChunkQueue) {
            setName("Meta Data Uploading Thread");
            setDaemon(true);
            this.fileThreads = fileThreads;
            this.dataThreads = dataThreads;
            this.metaThreads = metaThreads;
            this.metaChunkQueue = metaChunkQueue;
        }

        /**
         *
         */
        public final void halt() {
            debugOut("Halting meta data uploading thread.");
            stopped = true;
        }

        /**
         *
         * @return
         */
        public synchronized final void waitForFinish() {
            stopWhenFinished = true;
            while (!started || !finished) {
                try {
                    wait();
                } catch (Exception e) {
                    debugErr(e);
                }
            }
        }

        /**
         * 
         * @param metaChunk
         * @param metaDataBytes
         * @param ts
         * @param hosts
         * @return
         * @throws Exception
         */
        private final Collection<PropagationExceptionWrapper> upload(MetaChunk metaChunk, byte[] metaDataBytes, TrancheServer ts, String[] hosts) throws Exception {
            debugOut("Starting to upload meta chunk to " + ts.getHost() + ": " + metaChunk.hash);
            try {
                fireTryingMetaData(metaChunk.hash, ts.getHost());
                long startTime = TimeUtil.getTrancheTimestamp();
                PropagationReturnWrapper wrapper = IOUtil.setMetaData(ts, userCertificate, userPrivateKey, true, metaChunk.hash, metaDataBytes, hosts);
                for (PropagationExceptionWrapper pew : wrapper.getErrors()) {
                    debugOut(pew.toString());
                }
                debugOut("Time spent uploading meta data chunk: " + (TimeUtil.getTrancheTimestamp() - startTime));
                return wrapper.getErrors();
            } finally {
                ConnectionUtil.unlockConnection(ts.getHost());
            }
        }

        /**
         * 
         * @param metaChunk
         * @param e
         */
        private final void failChunk(MetaChunk metaChunk, Exception e) {
            debugErr(e);
            Set<PropagationExceptionWrapper> exceptions = new HashSet<PropagationExceptionWrapper>();
            exceptions.add(new PropagationExceptionWrapper(e));
            failChunk(metaChunk, exceptions);
        }

        /**
         * 
         * @param metaChunk
         * @param propagationExceptions
         */
        private final void failChunk(MetaChunk metaChunk, Collection<PropagationExceptionWrapper> exceptions) {
            // fail the file
            if (metaChunk.fileToUpload != null && !metaChunk.fileToUpload.failed) {
                metaChunk.fileToUpload.failed = true;
                fireFailedFile(metaChunk.fileToUpload.getFile(), metaChunk.fileToUpload.relativeName, exceptions);
            }
            fireFailedMetaData(metaChunk.fileToUpload.getFile(), metaChunk.hash, exceptions);
            for (FileEncodingThread thread : fileThreads) {
                thread.halt();
            }
            for (MetaDataUploadingThread thread : metaThreads) {
                thread.halt();
            }
            for (DataUploadingThread thread : dataThreads) {
                thread.halt();
            }
        }

        /**
         *
         */
        @Override
        public void run() {
            started = true;
            try {
                MetaChunk metaChunk = null;
                DOWHILE:
                do {
                    // get the next data chunk
                    do {
                        metaChunk = metaChunkQueue.poll(500, TimeUnit.MILLISECONDS);
                    } while (metaChunk == null && !stopWhenFinished && !stopped && !AddFileTool.this.stopped);

                    // break point
                    if (stopped || AddFileTool.this.stopped) {
                        break DOWHILE;
                    }

                    // if this file failed or nothing was returned
                    if (metaChunk == null || metaChunk.fileToUpload.isFailed()) {
                        continue;
                    }

                    // is this the project file meta data? make sure it is uploaded as the last chunk
                    if (metaChunk.metaData.isProjectFile()) {
                        if (!metaChunkQueue.isEmpty()) {
                            // make sure it's put at the end of the queue
                            metaChunk.serversTried = 0;
                            metaChunkQueue.put(metaChunk);
                            continue;
                        } else {
                            // data threads done?
                            boolean allDataThreadsDone = true;
                            for (DataUploadingThread t : dataThreads) {
                                if (!t.finished) {
                                    allDataThreadsDone = false;
                                }
                            }
                            if (!allDataThreadsDone) {
                                // break point
                                if (stopped || AddFileTool.this.stopped) {
                                    break DOWHILE;
                                }
                                // sleep for data to upload
                                ThreadUtil.safeSleep(250);
                                // make sure it's put at the end of the queue
                                metaChunk.serversTried = 0;
                                metaChunkQueue.put(metaChunk);
                                continue;
                            }
                        }
                    }

                    try {
                        fireStartedMetaData(metaChunk.hash);

                        // get servers
                        Collection<String> coreHosts = getCoreServersToUploadTo(metaChunk.hash);
                        Collection<String> nonCoreHosts = getNonCoreServersToUploadTo(metaChunk.hash);
                        if (coreHosts.isEmpty() && nonCoreHosts.isEmpty()) {
                            throw new Exception("No servers to which to upload.");
                        }

                        // download the meta data from the network (might exist)
                        metaChunk.mergeFromNetwork();
                        // convert the metadata object to bytes
                        byte[] metaDataBytes = metaChunk.metaData.toByteArray();

                        // upload to core using the multi server request strategy
                        Collection<MultiServerRequestStrategy> strategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(coreHosts, Tertiary.DONT_CARE, Tertiary.TRUE);
                        if (strategies.isEmpty()) {
                            throw new AssertionFailedException("Failed to find strategy.");
                        }
                        debugOut(strategies.size() + " strategies found.");

                        Set<String> toUploadCoreHosts = new HashSet<String>(coreHosts);
                        Set<String> uploadedCoreHosts = new HashSet<String>();
                        for (MultiServerRequestStrategy strategy : strategies) {
                            // break point
                            if (stopped || AddFileTool.this.stopped) {
                                break DOWHILE;
                            }
                            // upload
                            try {
                                TrancheServer ts = ConnectionUtil.connectHost(strategy.getHostReceivingRequest(), true);
                                if (ts == null) {
                                    throw new Exception("Could not connect to " + strategy.getHostReceivingRequest() + ".");
                                }
                                try {
                                    upload(metaChunk, metaDataBytes, ts, toUploadCoreHosts.toArray(new String[0]));
                                } catch (Exception e) {
                                    debugErr(e);
                                    ConnectionUtil.reportExceptionHost(strategy.getHostReceivingRequest(), e);
                                } finally {
                                    ConnectionUtil.unlockConnection(strategy.getHostReceivingRequest());
                                }
                            } catch (Exception e) {
                                debugErr(e);
                            }
                            // verify
                            Collection<String> toUploadCoreHostsCopy = new HashSet<String>(toUploadCoreHosts);
                            for (String host : toUploadCoreHostsCopy) {
                                // break point
                                if (stopped || AddFileTool.this.stopped) {
                                    break DOWHILE;
                                }
                                try {
                                    TrancheServer ts = ConnectionUtil.connectHost(host, true);
                                    if (ts == null) {
                                        throw new Exception("Could not connect to " + host + ".");
                                    }
                                    try {
                                        if (IOUtil.hasMetaData(ts, metaChunk.hash)) {
                                            uploadedCoreHosts.add(host);
                                            toUploadCoreHosts.remove(host);
                                            fireUploadedMetaData(metaChunk.hash, host);
                                        }
                                    } catch (Exception e) {
                                        debugErr(e);
                                        ConnectionUtil.reportExceptionHost(host, e);
                                    } finally {
                                        ConnectionUtil.unlockConnection(host);
                                    }
                                } catch (Exception e) {
                                    debugErr(e);
                                }
                            }
                            // have we uploaded enough copies?
                            if (uploadedCoreHosts.size() >= ConfigureTranche.getInt(ConfigureTranche.PROP_REPLICATIONS) || toUploadCoreHosts.isEmpty()) {
                                break;
                            }
                        }

                        // upload to non-core hosts directly
                        Set<String> uploadedNonCoreHosts = new HashSet<String>();
                        for (String host : nonCoreHosts) {
                            // break point
                            if (stopped || AddFileTool.this.stopped) {
                                break DOWHILE;
                            }
                            try {
                                TrancheServer ts = ConnectionUtil.connectHost(host, true);
                                if (ts == null) {
                                    throw new Exception("Could not connect to " + host + ".");
                                }
                                try {
                                    upload(metaChunk, metaDataBytes, ts, new String[] {host});
                                    if (IOUtil.hasMetaData(ts, metaChunk.hash)) {
                                        debugOut("Verified upload of meta data " + metaChunk.hash + " to " + ts.getHost());
                                        uploadedNonCoreHosts.add(host);
                                        fireUploadedMetaData(metaChunk.hash, host);
                                    }
                                } catch (Exception e) {
                                    debugErr(e);
                                    ConnectionUtil.reportExceptionHost(host, e);
                                } finally {
                                    ConnectionUtil.unlockConnection(host);
                                }
                            } catch (Exception e) {
                                debugErr(e);
                                ConnectionUtil.reportExceptionHost(host, e);
                            }
                        }
                        
                        // checks
                        int reps = ConfigureTranche.getInt(ConfigureTranche.PROP_REPLICATIONS);
                        if (uploadedCoreHosts.size() < reps) {
                            throw new Exception("Meta data uploaded to " + uploadedCoreHosts.size() + " core servers, but required number is " + reps);
                        }
                        if (nonCoreHosts.size() != uploadedNonCoreHosts.size()) {
                            throw new Exception("Meta data upload to " + uploadedNonCoreHosts.size() + " non-core servers, but expected number is " + nonCoreHosts.size());
                        }

                        fireFinishedMetaData(metaChunk.metaData, metaChunk.hash);
                        fireFinishedFile(metaChunk.fileToUpload.getFile(), metaChunk.fileToUpload.relativeName, metaChunk.hash);
                    } catch (Exception e) {
                        failChunk(metaChunk, e);
                    }
                } while (!(metaChunk == null && metaChunkQueue.isEmpty() && stopWhenFinished) && !stopped && !AddFileTool.this.stopped);
            } catch (Exception e) {
                debugErr(e);
            } finally {
                finished = true;
                synchronized (MetaDataUploadingThread.this) {
                    notifyAll();
                }
            }
        }
    }
}
