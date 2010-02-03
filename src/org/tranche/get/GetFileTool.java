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
package org.tranche.get;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.tranche.ConfigureTranche;
import org.tranche.FileEncoding;
import org.tranche.TrancheServer;
import org.tranche.exceptions.CantVerifySignatureException;
import org.tranche.exceptions.CouldNotFindMetaDataException;
import org.tranche.exceptions.PassphraseRequiredException;
import org.tranche.exceptions.UnknownArgumentException;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.logs.LogUtil;
import org.tranche.meta.MetaData;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTable;
import org.tranche.network.StatusTableRow;
import org.tranche.project.ProjectFile;
import org.tranche.project.ProjectFilePart;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.security.SecurityUtil;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.timeestimator.ContextualTimeEstimator;
import org.tranche.timeestimator.TimeEstimator;
import org.tranche.util.CompressionUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;
import org.tranche.util.ThreadUtil;

/**
 * <p>A tool for downloading from a Tranche repository.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class GetFileTool {

    private static boolean debug = false;
    /**
     * Default parameters
     */
    private static String TEMP_FILE_DENOTATION = ".tranche-temp.";
    public static boolean DEFAULT_BATCH = true;
    public static boolean DEFAULT_VALIDATE = false;
    public static String DEFAULT_REG_EX = ".";
    public static boolean DEFAULT_USE_UNSPECIFIED_SERVERS = true;
    public static Long DEFAULT_UPLOAD_TIMESTAMP = null;
    public static String DEFAULT_UPLOADER_NAME = null;
    public static String DEFAULT_UPLOAD_RELATIVE_PATH = null;
    public static int DEFAULT_THREADS = 8;
    public static boolean DEFAULT_SHOW_SUMMARY = false;
    public static boolean DEFAULT_CONTINUE_ON_FAILURE = true;
    public static int DEFAULT_DATA_QUEUE_SIZE = 100;
    // we don't want to use up too much memory with big meta data - so need to keep track of total size of the data
    //  this meta data references
    // limit of size on disk == 13 MB of meta data in memory, or roughly 175 GB of data references
    private final static long LIMIT_META_DATA_SIZE_ON_DISK = Long.valueOf("187904819200");
    /**
     * Startup runtime parameters
     */
    private static final boolean START_VALUE_PAUSED = false;
    private static final boolean START_VALUE_STOPPED = false;
    private static final MetaData START_VALUE_META_DATA = null;
    private static final ProjectFile START_VALUE_PROJECT_FILE = null;
    private static final boolean START_VALUE_SKIPPED_FILE = false;
    private static final TimeEstimator START_VALUE_TIME_ESTIMATOR = null;
    /**
     * User parameters
     */
    private boolean batch = DEFAULT_BATCH,  validate = DEFAULT_VALIDATE,  continueOnFailure = DEFAULT_CONTINUE_ON_FAILURE,  useUnspecifiedServers = DEFAULT_USE_UNSPECIFIED_SERVERS;
    private BigHash hash;
    private File saveTo;
    private String uploaderName = DEFAULT_UPLOADER_NAME,  uploadRelativePath = DEFAULT_UPLOAD_RELATIVE_PATH,  passphrase,  regEx = DEFAULT_REG_EX;
    private Long uploadTimestamp = DEFAULT_UPLOAD_TIMESTAMP;
    private final Set<String> serverHostUseSet = new HashSet<String>();
    private final Set<String> externalServerURLs = new HashSet();
    private int threadCount = DEFAULT_THREADS;
    /**
     * Runtime parameters
     */
    private boolean paused = START_VALUE_PAUSED,  stopped = START_VALUE_STOPPED;
    /**
     * Statistics, reporting variables, listeners
     */
    private final Set<GetFileToolListener> listeners = new HashSet<GetFileToolListener>();
    private MetaData metaData = START_VALUE_META_DATA;
    private ProjectFile projectFile = START_VALUE_PROJECT_FILE;
    // handles all information relevant to the progress of the download
    private TimeEstimator timeEstimator = START_VALUE_TIME_ESTIMATOR;
    private boolean skippedFile = START_VALUE_SKIPPED_FILE;
    /**
     * Inernal variables
     */
    private boolean locked = false;
    private Pattern regExPattern = Pattern.compile(regEx);

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
     * <p>Sets whether to download chunks in batches.</p>
     * @param batch Whether to download chunks in batches.
     */
    public void setBatch(boolean batch) {
        this.batch = batch;
    }

    /**
     * <p>Whether chunks will be downloaded in batches.</p>
     * @return Whether chunks will be downloaded in batches.
     */
    public boolean isBatch() {
        return batch;
    }

    /**
     * <p>Sets whether the download will be validated.</p>
     * <p>Validation consists of recreating hashes and signatures then comparing them to the downloaded data.</p>
     * @param validate Whether the download will be validated.
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    /**
     * <p>Sets whether the download will be validated.</p>
     * <p>Validation consists of recreating hashes and signatures then comparing them to the downloaded data.</p>
     * @return Whether the download will be validated.
     */
    public boolean isValidate() {
        return validate;
    }

    /**
     * <p>Sets whether the download should continue even if some part of it has already failed.</p>
     * @param continueOnFailure Whether the download should continue even if some part of it has already failed.
     */
    public void setContinueOnFailure(boolean continueOnFailure) {
        this.continueOnFailure = continueOnFailure;
    }

    /**
     * <p>Whether the download should continue even if some part of it has already failed.</p>
     * @return Whether the download should continue even if some part of it has already failed.
     */
    public boolean isContinueOnFailure() {
        return continueOnFailure;
    }

    /**
     * <p>Sets the hash of the data to be downloaded.</p>
     * @param hash The hash of the data to be downloaded.
     */
    public void setHash(BigHash hash) {
        // reset?
        if (this.hash == null || hash == null || !this.hash.equals(hash)) {
            metaData = START_VALUE_META_DATA;
            projectFile = START_VALUE_PROJECT_FILE;
            stopped = START_VALUE_STOPPED;
            paused = START_VALUE_PAUSED;
            skippedFile = START_VALUE_SKIPPED_FILE;
            timeEstimator = START_VALUE_TIME_ESTIMATOR;
        }
        this.hash = hash;
    }

    /**
     * <p>Gets the hash of the data to be downloaded.</p>
     * @return The hash of the data to be downloaded.
     */
    public BigHash getHash() {
        return hash;
    }

    /**
     * <p>Sets the location to which the file should be saved.</p>
     * @param saveTo The location to which the file should be saved.
     */
    public void setSaveFile(File saveTo) {
        this.saveTo = saveTo;
    }

    /**
     * <p>Gets the location to which the file should be saved.</p>
     * @return The location to which the file should be saved.
     */
    public File getSaveFile() {
        return saveTo;
    }

    /**
     * <p>Sets the name of the uploading user. Required to disambiguate between files that have been uploaded multiple times.</p>
     * @param uploaderName The name of the uploading user.
     */
    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    /**
     * <p>Gets the name of the specified uploading user.</p>
     * @return The name of the specified uploading user.
     */
    public String getUploaderName() {
        return uploaderName;
    }

    /**
     * <p>Sets the timestamp of when the data set was uploaded. Required to disambiguate between files that have been uploaded multiple times.</p>
     * @param uploadTimestamp The timestamp of when the data set was uploaded.
     */
    public void setUploadTimestamp(long uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    /**
     * <p>Gets the timestamp of when the data set was uploaded.</p>
     * @return The timestamp of when the data set was uploaded.
     */
    public Long getUploadTimestamp() {
        return uploadTimestamp;
    }

    /**
     * <p>Sets the name of the relative path of the file in the data set. Required to disambiguate between files that have been uploaded multiple times in a data set.</p>
     * @param uploadRelativePath Name of the relative path of the file in the data set.
     */
    public void setUploadRelativePath(String uploadRelativePath) {
        this.uploadRelativePath = uploadRelativePath;
    }

    /**
     * <p>Gets the name of the relative path of the file in the data set.</p>
     * @return The name of the relative path of the file in the data set.
     */
    public String getUploadRelativePath() {
        return uploadRelativePath;
    }

    /**
     * <p>Sets the passphrase to be used during decryption.</p>
     * @param passphrase The passphrase to be used during decryption.
     */
    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    /**
     * <p>Gets the passphrase to be used during decryption.</p>
     * @return The passphrase to be used during decryption.
     */
    public String getPassphrase() {
        return passphrase;
    }

    /**
     * <p>Unsets the decryption passphrase.</p>
     */
    public void clearPassphrase() {
        setPassphrase(null);
    }

    /**
     * <p>Sets the regular expression to be used to download a portion of a data set.</p>
     * @param regEx The regular expression.
     */
    public void setRegEx(String regEx) {
        this.regEx = regEx.toLowerCase();
        this.regExPattern = Pattern.compile(this.regEx);
    }

    /**
     * <p>Gets the regular expression that is used to download a portion of a data set.</p>
     * @return The regular expression.
     */
    public String getRegEx() {
        return regEx;
    }

    /**
     * <p>Unsets the regular expression.</p>
     */
    public void clearRegEx() {
        setRegEx(DEFAULT_REG_EX);
    }

    /**
     * <p>Adds the host name of a server to be used during download.</p>
     * @param serverHostName The host name of a server.
     * @return Whether the host name was added to the list.
     */
    public boolean addServerToUse(String serverHostName) {
        synchronized (serverHostUseSet) {
            return serverHostUseSet.add(serverHostName);
        }
    }

    /**
     * <p>Adds a collection of server host names to be used during download.</p>
     * @param serverHostNames A collection of host names to be used during download.
     * @return Whether the host names were added to the list.
     */
    public boolean addServersToUse(Collection<String> serverHostNames) {
        synchronized (serverHostUseSet) {
            return serverHostUseSet.addAll(serverHostNames);
        }
    }

    /**
     * <p>Removes the server host name from the list of servers to be used.</p>
     * @param serverHostName The host name of a server.
     * @return Whether the host name was removed from the list.
     */
    public boolean removeServerToUse(String serverHostName) {
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
        synchronized (serverHostUseSet) {
            return serverHostUseSet.removeAll(serverHostNames);
        }
    }

    /**
     * <p>Gets the collection of server host names to be used.</p>
     * @return A collection of server host names.
     */
    public Collection<String> getServersToUse() {
        List<String> list = new LinkedList<String>();
        synchronized (serverHostUseSet) {
            list.addAll(serverHostUseSet);
        }
        return Collections.unmodifiableCollection(list);
    }

    /**
     * <p>Adds the url of a server from an external network to be used during download.</p>
     * <p>Note that this collection does not effect the collection of server hosts to use with this network; it only permits the use of additional servers from other networks.</p>
     * @param serverURL The url of a server.
     * @return Whether the url was added to the list.
     */
    public boolean addExternalServerURLToUse(String serverURL) {
        synchronized (externalServerURLs) {
            return externalServerURLs.add(serverURL);
        }
    }

    /**
     * <p>Adds a collection of server urls from external networks to be used during download.</p>
     * <p>Note that this collection does not effect the collection of server hosts to use with this network; it only permits the use of additional servers from other networks.</p>
     * @param serverURLs A collection of urls to be used during download.
     * @return Whether the urls were added to the list.
     */
    public boolean addExternalServerURLsToUse(Collection<String> serverURLs) {
        synchronized (externalServerURLs) {
            return externalServerURLs.addAll(serverURLs);
        }
    }

    /**
     * <p>Removes the server url from the list of servers from external networks to be used.</p>
     * <p>Note that this collection does not effect the collection of server hosts to use with this network; it only permits the use of additional servers from other networks.</p>
     * @param serverURL The url of a server.
     * @return Whether the url was removed from the list.
     */
    public boolean removeExternalServerURLToUse(String serverURL) {
        synchronized (externalServerURLs) {
            return externalServerURLs.remove(serverURL);
        }
    }

    /**
     * <p>Removes a collection of host names from the list of servers to be used.</p>
     * <p>Note that this collection does not effect the collection of server hosts to use with this network; it only permits the use of additional servers from other networks.</p>
     * @param serverURLs A collection of server urls.
     * @return Whether the urls were removed from the list.
     */
    public boolean removeExternalServerURLsToUse(Collection<String> serverURLs) {
        synchronized (externalServerURLs) {
            return externalServerURLs.removeAll(serverURLs);
        }
    }

    /**
     * <p>Gets the collection of server urls to be used from separate networks.</p>
     * <p>Note that this collection does not effect the collection of server hosts to use with this network; it only permits the use of additional servers from other networks.</p>
     * @return A collection of server urls.
     */
    public Collection<String> getExternalServerURLsToUse() {
        List<String> list = new LinkedList<String>();
        synchronized (externalServerURLs) {
            list.addAll(externalServerURLs);
        }
        return Collections.unmodifiableCollection(list);
    }

    /**
     * 
     * @param serversHostUseSet
     */
    public void setServersToUse(Collection<String> serversHostUseSet) {
        synchronized (serverHostUseSet) {
            serverHostUseSet.clear();
            serverHostUseSet.addAll(serversHostUseSet);
        }
    }

    /**
     * <p>Sets whether servers other than the ones specified should be used.</p>
     * @param useUnspecifiedServers Whether servers other than the ones specified should be used.
     */
    public void setUseUnspecifiedServers(boolean useUnspecifiedServers) {
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
     * <p>Set whether the download is paused.</p>
     * @param paused Whether the download is paused.
     */
    public void setPause(boolean paused) {
        this.paused = paused;
    }

    /**
     * <p>Gets whether the download is paused.</p>
     * @return Whether the download is paused.
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * <p>Will sleep the current thread until the download is no longer paused.</p>
     */
    private void waitHereOnPause() {
        while (paused) {
            // break point
            if (isStopped()) {
                return;
            }
            ThreadUtil.safeSleep(1000);
        }
    }

    /**
     * 
     * @return
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * <p>Stops the download.</p>
     */
    public void stop() {
        stopped = true;
    }

    /**
     * <p>Adds a listener.</p>
     * @param l A listener.
     * @return Whether the listener was added.
     */
    public boolean addListener(GetFileToolListener l) {
        synchronized (listeners) {
            return listeners.add(l);
        }
    }

    /**
     * <p>Removes a listener.</p>
     * @param l A listener.
     * @return Whether the listener was removed.
     */
    public boolean removeListener(GetFileToolListener l) {
        synchronized (listeners) {
            return listeners.remove(l);
        }
    }

    /**
     * 
     * @return
     */
    public Collection<GetFileToolListener> getListeners() {
        List<GetFileToolListener> list = new LinkedList<GetFileToolListener>();
        synchronized (listeners) {
            list.addAll(listeners);
        }
        return Collections.unmodifiableCollection(list);
    }

    /**
     * <p>Gets the number of bytes that are being downloaded.</p>
     * @return The number of bytes that are being downloaded.
     */
    public long getBytesToDownload() {
        if (timeEstimator != null) {
            return timeEstimator.getTotalBytes();
        } else {
            return 0;
        }
    }

    /**
     * <p>Gets the number of bytes that have been downloaded.</p>
     * @return The number of bytes that have been downloaded.
     */
    public long getBytesDownloaded() {
        if (timeEstimator != null) {
            return timeEstimator.getBytesDone();
        } else {
            return 0;
        }
    }

    /**
     * <p>Gets the time to download object.</p>
     * @return The time to download object.
     */
    public TimeEstimator getTimeEstimator() {
        return timeEstimator;
    }

    /**
     * <p>Sets the number of threads to be used.</p>
     * @param threadCount The number of threads to be used.
     */
    public void setThreadCount(int threadCount) {
        if (threadCount < 2) {
            threadCount = 2;
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
     * <p>Used internally to validate the variables used during a download.</p>
     * @throws java.lang.Exception
     */
    private void validateVariables() throws Exception {
        long start = System.currentTimeMillis();
        // check variables
        if (hash == null) {
            throw new NullPointerException("Hash is not set.");
        }
        // would result in no actions being taken
        if (getServersToUse().isEmpty() && !useUnspecifiedServers && getExternalServerURLsToUse().isEmpty()) {
            throw new Exception("No servers to use.");
        }
        debugOut("Time spent validating variables: " + (System.currentTimeMillis() - start));
    }

    /**
     * <p>Used internally to validate the </p>
     * @throws java.lang.Exception
     */
    private void validateSpecifiedUploader() throws Exception {
        // must specify which uploader
        if (metaData.getUploaderCount() > 1 && uploaderName != null && uploadTimestamp != null) {
            metaData.selectUploader(uploaderName, uploadTimestamp, uploadRelativePath);
        }
    }

    /**
     * <p>Used internally to establish and lock connections with servers that will be used.</p>
     * @throws java.lang.Exception
     */
    protected void setUpConnections() throws Exception {
        long start = System.currentTimeMillis();
        NetworkUtil.waitForStartup();
        debugOut("Time spent waiting for network util to start up: " + (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();

        // Connect to servers on this network
        for (String host : getServersToUse()) {
            if (ConnectionUtil.isConnected(host)) {
                ConnectionUtil.lockConnection(host);
            } else {
                try {
                    ConnectionUtil.connectHost(host, true);
                } catch (Exception e) {
                    debugErr(e);
                    fire("Could not connect to server " + host + ".");
                    ConnectionUtil.reportExceptionHost(host, e);
                }
            }
        }

        // Connect to servers on external networks
        for (String url : getExternalServerURLsToUse()) {
            String host = IOUtil.parseHost(url);

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
            // Tues Jan 19th 2010: Noticed this wasn't working on shadow network,
            // though a different tool was. Each thread using GFT was blocking on 
            // Socket.connect, so trying different connection logic based on the 
            // other working tool.
            //                                                            Bryan
            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
//            if (ConnectionUtil.isConnected(host)) {
//                ConnectionUtil.lockConnection(host);
//            } else {
//                try {
//                    ConnectionUtil.connectURL(url, true);
//                } catch (Exception e) {
//                    System.err.println(e.getClass().getSimpleName() + " occurred while connecting to external server " + url + ": " + e.getMessage());
//                    e.printStackTrace(System.err);
//                    ConnectionUtil.reportExceptionURL(url, e);
//                    debugErr(e);
//                    fire("Could not connect to external server " + url + ".");
//                }
//            }

            TrancheServer ts = null;
            try {
                ts = ConnectionUtil.getHost(host);

                if (ts == null) {
                    ts = ConnectionUtil.connectURL(url, true);
                } else {
                    ConnectionUtil.lockConnection(host);
                }
            } catch (Exception e) {
                debugErr(e);
                fire("Could not connect to external server " + url + ".");
                ConnectionUtil.reportExceptionURL(url, e);
            }
        }

        // no connections? big problem
        if (ConnectionUtil.size() == 0) {
            throw new Exception("No available connections.");
        }

        debugOut("Time spent setting up connections: " + (System.currentTimeMillis() - start));
    }

    /**
     * <p>Used internally to unlock connections made during set-up.</p>
     */
    protected void tearDownConnections() {
        long start = System.currentTimeMillis();
        for (String host : getServersToUse()) {
            ConnectionUtil.unlockConnection(host);
        }
        for (String url : getExternalServerURLsToUse()) {
            ConnectionUtil.unlockConnection(IOUtil.parseHost(url));
        }
        debugOut("Time spent tearing down connections: " + (System.currentTimeMillis() - start));
    }

    /**
     * <p>Gets a collection of server host names that should be used to download a chunk with the given hash.</p>
     * <p>If noted that the tool can use servers that are not specified, will include all matching connected servers.</p>
     * <p>Servers ordered: (1) connected servers that are readable, writable, and have the hash in their hash span (2) the same as the previous, but not writable (3) other servers to use that may have sticky data.</p>
     * <p>Each of the portions of the list are internally randomized.</p>
     * @param hash A hash.
     * @return An ordered collection of server host names that should be used to download a chunk with the given hash.
     */
    protected Collection<String> getConnections(BigHash hash) {
        long start = System.currentTimeMillis();
        debugOut("Getting connections for " + hash);
        debugOut("There are " + ConnectionUtil.getConnectedRows().size() + " connections available.");
        List<String> writableHosts = new LinkedList<String>();
        List<String> nonWritableHosts = new LinkedList<String>();
        List<String> externalHosts = new LinkedList<String>();
        // add all connected servers with the hash in their hash spans
        if (useUnspecifiedServers) {
            for (StatusTableRow row : ConnectionUtil.getConnectedRows()) {
                if (!row.isReadable()) {
                    continue;
                }
                for (HashSpan span : row.getHashSpans()) {
                    if (span.contains(hash)) {
                        // prefer writable hosts (to save on propagation)
                        if (row.isWritable()) {
                            writableHosts.add(row.getHost());
                        } else {
                            nonWritableHosts.add(row.getHost());
                        }
                        break;
                    }
                }
            }
        }
        StatusTable table = NetworkUtil.getStatus().clone();
        for (String host : getServersToUse()) {
            try {
                StatusTableRow row = table.getRow(host);
                if (!writableHosts.contains(host) && !nonWritableHosts.contains(host) && row.isReadable()) {
                    if (row.isWritable()) {
                        writableHosts.add(host);
                    } else {
                        nonWritableHosts.add(host);
                    }
                }
            } catch (Exception e) {
                debugErr(e);
            }
        }
        for (String url : getExternalServerURLsToUse()) {
            try {
                String host = IOUtil.parseHost(url);
                if (ConnectionUtil.isConnected(host)) {
                    externalHosts.add(host);
                }
            } catch (Exception e) {
                debugErr(e);
            }
        }

        Collections.shuffle(writableHosts);
        Collections.shuffle(nonWritableHosts);
        Collections.shuffle(externalHosts);
        // prefer writable hosts (to save on propagation)
        List<String> hosts = new LinkedList<String>(writableHosts);
        hosts.addAll(nonWritableHosts);
        hosts.addAll(externalHosts);

        debugOut("Time spent getting connections: " + (System.currentTimeMillis() - start));

        return Collections.unmodifiableCollection(hosts);
    }

    /**
     * 
     * @param relativeName
     * @return
     * @throws java.lang.Exception
     */
    private File createTemporaryFile(String relativeName, boolean singleFile) throws Exception {
        if (relativeName == null) {
            throw new NullPointerException("Relative name is null.");
        }
        File file = null;
        File saveLocation = saveTo;
        if (singleFile) {
            if (saveLocation == null) {
                saveLocation = TempFileUtil.createTemporaryFile();
            }
            file = saveLocation;
        } else {
            if (saveLocation == null) {
                saveLocation = TempFileUtil.createTemporaryDirectory();
            }
            file = new File(saveLocation, relativeName);
        }
        if (file.getParent() != null) {
            file = new File(file.getParent(), TEMP_FILE_DENOTATION + file.getName());
        } else {
            file = new File(TempFileUtil.createTemporaryDirectory(), TEMP_FILE_DENOTATION + file.getName());
        }
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            file.createNewFile();
        }
        return file;
    }

    /**
     * <p>Gets the meta data, downloading if necessary.</p>
     * @return
     * @throws java.lang.CouldNotFindMetaDataException
     * @throws java.lang.Exception
     */
    public MetaData getMetaData() throws CouldNotFindMetaDataException, Exception {
        // don't have it?
        if (metaData == null) {
            validateVariables();
            setUpConnections();
            try {
                metaData = downloadMetaData(hash);
                if (metaData == null) {
                    throw new CouldNotFindMetaDataException(hash);
                }
                // select the proper uploader
                if (metaData.getUploaderCount() > 1 && getUploaderName() != null && getUploadTimestamp() != null) {
                    metaData.selectUploader(getUploaderName(), getUploadTimestamp(), getUploadRelativePath());
                }
                // may need to set the public passphrase for the whole data set
                if (metaData.isPublicPassphraseSet()) {
                    setPassphrase(metaData.getPublicPassphrase());
                }
            } finally {
                tearDownConnections();
            }
        }
        return metaData;
    }

    /**
     * <p>Gets the project file, downloading if necessary.</p>
     * @return The project file.
     * @throws java.lang.Exception
     */
    public ProjectFile getProjectFile() throws Exception {
        // download?
        if (projectFile == null) {
            File pf = null;
            try {
                setUpConnections();
                // do we have the meta data?
                if (metaData == null) {
                    getMetaData();
                } else {
                    validateVariables();
                }
                validateSpecifiedUploader();
                timeEstimator = new ContextualTimeEstimator(0, hash.getLength(), 0, 1);
                pf = TempFileUtil.createTemporaryFile("pf" + System.currentTimeMillis());
                // download the file
                if (passphrase != null) {
                    downloadFile(null, metaData, pf, new BigHash(passphrase.getBytes()).toByteArray());
                } else {
                    downloadFile(null, metaData, pf, new byte[0]);
                }
                // read the file
                projectFile = ProjectFile.createFromFile(pf);
            } finally {
                tearDownConnections();
                IOUtil.safeDelete(pf);
            }
        }
        return projectFile;
    }

    /**
     * <p>Download the specified file.</p>
     * @return A report describing what occurred during the download.
     */
    public GetFileToolReport getFile() {
        GetFileToolReport report = new GetFileToolReport();
        addListener(new GetFileToolReportListener(report));
        try {
            // lock the variables in place
            locked = true;
            setUpConnections();
            // download the meta data
            if (metaData == null) {
                getMetaData();
            } else {
                validateVariables();
            }
            validateSpecifiedUploader();
            // check the save file
            if (saveTo == null) {
                throw new NullPointerException("Save file is not set.");
            }
            // variables are valid -- download
            timeEstimator = new ContextualTimeEstimator(0, hash.getLength(), 0, 1);
            // download
            File file = saveTo;
            if (saveTo.exists() && saveTo.isDirectory()) {
                GetFileToolUtil.testDirectoryForWritability(saveTo);
                file = new File(saveTo, metaData.getName());
            }
            byte[] padding = new byte[0];
            if (passphrase != null) {
                padding = new BigHash(passphrase.getBytes()).toByteArray();
            }
            downloadFile(null, metaData, file, padding);
        } catch (Exception e) {
            debugErr(e);
            if (metaData == null) {
                fireFailedFile(hash, e);
            } else {
                fireFailedFile(hash, metaData.getName(), e);
            }
        } finally {
            tearDownConnections();
            report.setTimestampEnd(TimeUtil.getTrancheTimestamp());
            if (timeEstimator != null) {
                report.setFilesDownloaded(timeEstimator.getFilesDone());
                report.setBytesDownloaded(timeEstimator.getTotalBytes());
            } else {
                report.setFilesDownloaded(0);
                report.setBytesDownloaded(0);
            }
            if (report.isFailed()) {
                for (PropagationExceptionWrapper pew : report.getFailureExceptions()) {
                    debugErr(pew.exception);
                }
            }
            // register our download only if nothing was skipped during download and there were no failures
            if (!skippedFile && !report.isFailed() && !TestUtil.isTesting()) {
                GetFileToolUtil.registerDownload(this, report);
            }
            locked = false;
        }
        return report;
    }

    /**
     * <p>Download the specified directory.</p>
     * @return A report describing what occurred during the download.
     */
    public GetFileToolReport getDirectory() {
        GetFileToolReport report = new GetFileToolReport();
        addListener(new GetFileToolReportListener(report));
        try {
            // lock the variables in place
            locked = true;
            fireStartingDirectory(hash);
            setUpConnections();
            // download the project file
            if (projectFile == null) {
                getProjectFile();
            } else {
                validateVariables();
            }
            // check the save location
            if (saveTo == null) {
                throw new NullPointerException("Save file is not set.");
            }
            if (!saveTo.isDirectory()) {
                throw new Exception("Save location must be a directory.");
            }
            GetFileToolUtil.testDirectoryForWritability(saveTo);
            // make the meta chunk list
            long size = 0, files = 0;
            LinkedList<MetaChunk> metaChunkList = new LinkedList<MetaChunk>();
            for (ProjectFilePart pfp : projectFile.getParts()) {
                if (regExPattern.matcher(pfp.getRelativeName().toLowerCase()).find()) {
                    metaChunkList.add(new MetaChunk(pfp));
                    size += pfp.getPaddingAdjustedLength() + pfp.getPaddingLength();
                    files++;
                }
            }
            timeEstimator = new ContextualTimeEstimator(0, size, 0, files);
            fireStartedDirectory(hash);

            // download the directory
            PriorityBlockingQueue<DataChunk> dataChunkQueue = new PriorityBlockingQueue(DEFAULT_DATA_QUEUE_SIZE);
            Map<String, MetaChunkBatch> metaDataBatchWaitingList = new HashMap<String, MetaChunkBatch>();
            Set<DirectoryMetaDataDownloadingThread> metaThreads = new HashSet<DirectoryMetaDataDownloadingThread>();
            Set<DirectoryDataDownloadingThread> dataThreads = new HashSet<DirectoryDataDownloadingThread>();
            // start a meta data downloading thread
            for (int i = 0; i < 2; i++) {
                metaThreads.add(new DirectoryMetaDataDownloadingThread(metaChunkList, dataChunkQueue, metaDataBatchWaitingList, dataThreads, metaThreads));
            }
            // start data downloading threads
            Map<String, DataChunkBatch> dataBatchWaitingList = new HashMap<String, DataChunkBatch>();
            for (int i = 0; i < threadCount - 1; i++) {
                dataThreads.add(new DirectoryDataDownloadingThread(dataChunkQueue, dataBatchWaitingList, dataThreads, metaThreads));
            }
            // start the meta data downloading thread after creating the data downloading threads
            for (DirectoryMetaDataDownloadingThread metaThread : metaThreads) {
                metaThread.start();
            }
            for (DirectoryDataDownloadingThread dataThread : dataThreads) {
                dataThread.start();
            }
            // wait for the meta data thread to finish
            for (DirectoryMetaDataDownloadingThread metaThread : metaThreads) {
                metaThread.waitForFinish();
            }
            // tell all the data downloading threads there will be no more for them to download
            for (DirectoryDataDownloadingThread dataThread : dataThreads) {
                dataThread.waitForFinish();
            }
            // check for failures
            if (report.isFailed()) {
                fireFailedDirectory(hash, report.getFailureExceptions());
            } else {
                fireFinishedDirectory(hash);
            }
        } catch (Exception e) {
            debugErr(e);
            fireFailedDirectory(hash, e);
        } finally {
            tearDownConnections();
            report.setTimestampEnd(TimeUtil.getTrancheTimestamp());
            if (report.isFailed()) {
                for (PropagationExceptionWrapper pew : report.getFailureExceptions()) {
                    debugErr(pew.exception);
                }
            }
            if (timeEstimator != null) {
                report.setFilesDownloaded(timeEstimator.getFilesDone());
                report.setBytesDownloaded(timeEstimator.getTotalBytes());
            }
            // register our download only if nothing was skipped during download and there were no failures
            if (!TestUtil.isTesting()) {
                if (!skippedFile && !report.isFailed()) {
                    GetFileToolUtil.registerDownload(this, report);
                } else if (report.isFailed()) {
                    GetFileToolUtil.registerFailedDownload(this, report);
                }
            }
            locked = false;
        }
        return report;
    }

    /**
     * <p>Checks whether a file should be skipped. Deletes the file if it does not match the given file hash.</p>
     * @param expectedHash The expected hash of the file.
     * @param saveAs The location of where the file will be saved.
     * @param padding The bytes that were tacked on the end up the uploaded file.
     * @return True if the saveAs file exists and matches the expected hash. False and deletes the saveAs file if it does not match the expected hash.
     * @throws java.lang.Exception
     */
    private boolean skipFile(BigHash expectedHash, File saveAs, byte[] padding) throws Exception {
        debugOut("Checking for skip file: " + saveAs.getAbsolutePath());
        if (saveAs.exists()) {
            BigHash checkHash = new BigHash(saveAs, padding);
            if (checkHash.equals(expectedHash)) {
                return true;
            }
            // doesn't match, delete
            IOUtil.safeDelete(saveAs);
        }
        return false;
    }

    /**
     * <p>Used internally to download a file.</p>
     * @param part The project file part. NULL if not part of a directory download or is a project file.
     * @param metaData The meta data for the file to be downloaded.
     * @param saveAs The location to save the downloaded file.
     * @param padding The bytes that were tacked on the end up the uploaded file.
     */
    private void downloadFile(ProjectFilePart part, MetaData metaData, File saveAs, byte[] padding) {
        if (metaData.getEncodings().get(0).getHash().getLength() > DataBlockUtil.ONE_MB) {
            downloadFileDiskBacked(part, metaData, saveAs, padding);
        } else {
            downloadFileInMemory(part, metaData, saveAs, padding);
        }
    }

    /**
     * <p>Downloads a file in memory.</p>
     * @param part The project file part. NULL if not part of a directory download or is a project file.
     * @param metaData The meta data for the file to be downloaded.
     * @param saveAs The location to save the downloaded file.
     * @param padding The bytes that were tacked on the end up the uploaded file.
     */
    private void downloadFileInMemory(ProjectFilePart part, MetaData metaData, File saveAs, byte[] padding) {
        debugOut("Downloading file (in memory) " + saveAs.getName() + ". Padding length: " + padding.length);
        // none encoding hash
        BigHash fileHash = metaData.getEncodings().get(0).getHash();
        fireStartingFile(fileHash);
        try {
            // check for skip
            if (skipFile(fileHash, saveAs, padding)) {
                fireSkippedFile(fileHash, metaData, part);
            } else {
                debugOut("");
                // fire started -- should be relative name in data set, not name of file
                if (part == null) {
                    fireStartedFile(fileHash, metaData.getName());
                } else {
                    fireStartedFile(fileHash, part.getRelativeName());
                }
                // less than 1 MB, so there is only one part hash
                BigHash partHash = metaData.getParts().get(0);
                // download
                byte[] bytes = downloadData(fileHash, partHash);
                // failed to download
                if (bytes == null) {
                    throw new Exception("Could not find data chunk with hash " + partHash);
                }
                // decode
                decodeFileInMemory(part, metaData, bytes, saveAs, padding);
            }
        } catch (Exception e) {
            debugErr(e);
            fireFailedFile(fileHash, e);
            // delete what exists -- it may be invalid
            IOUtil.safeDelete(saveAs);
        }
    }

    /**
     * <p>Downloads a file to a tempory file.</p>
     * @param part The project file part. NULL if not part of a directory download or is a project file.
     * @param metaData The meta data for the file to be downloaded.
     * @param saveAs The location to save the downloaded file.
     * @param padding The bytes that were tacked on the end up the uploaded file.
     */
    private void downloadFileDiskBacked(ProjectFilePart part, MetaData metaData, File saveAs, byte[] padding) {
        debugOut("Downloading file (disk backed) " + saveAs.getName() + ". Padding length: " + padding.length);
        // none encoding hash
        BigHash fileHash = metaData.getEncodings().get(0).getHash();
        fireStartingFile(fileHash);
        try {
            // check for skip
            if (skipFile(fileHash, saveAs, padding)) {
                fireSkippedFile(fileHash, metaData, part);
            } else {
                // fire started -- should be relative name in data set, not name of file
                if (part == null) {
                    fireStartedFile(fileHash, metaData.getName());
                } else {
                    fireStartedFile(fileHash, part.getRelativeName());
                }
                // make a meta chunk
                MetaChunk metaChunk = new MetaChunk(metaData, true);
                // make a new list of data chunks
                LinkedList<DataChunk> dataChunks = new LinkedList<DataChunk>();
                long offset = 0;
                for (BigHash partHash : metaData.getParts()) {
                    dataChunks.add(new DataChunk(offset, partHash, metaChunk));
                    // update the offset
                    offset += partHash.getLength();
                }
                // create the data downloading threads
                Set<FileDataDownloadingThread> dataThreads = new HashSet<FileDataDownloadingThread>();
                for (int i = 0; i < threadCount && i < metaData.getParts().size(); i++) {
                    FileDataDownloadingThread thread = new FileDataDownloadingThread(dataChunks, dataThreads);
                    dataThreads.add(thread);
                    thread.start();
                }
                // wait for them all to stop
                for (FileDataDownloadingThread thread : dataThreads) {
                    thread.waitForFinish();
                }
                // check for failure
                for (FileDataDownloadingThread thread : dataThreads) {
                    if (!thread.exceptions.isEmpty()) {
                        throw new Exception("Could not find part of the file.");
                    }
                }
                // close the RAF
                IOUtil.safeClose(metaChunk.fileDecoding.raf);
                // decode
                decodeFileDiskBacked(part, metaData, metaChunk.fileDecoding.tempFile, saveAs, padding);
                // delete the temp file
                IOUtil.safeDelete(metaChunk.fileDecoding.tempFile);
            }
        } catch (Exception e) {
            fireFailedFile(fileHash, e);
        }
    }

    /**
     * <p>Decodes a file from downloaded bytes.</p>
     * @param part The project file part. NULL if not part of a directory download or is a project file.
     * @param metaData The meta data for the file to be downloaded.
     * @param bytes Bytes that were downloaded to decode and save.
     * @param saveAs The location to save the downloaded file.
     * @param padding The bytes that were tacked on the end up the uploaded file.
     * @throws java.lang.Exception
     */
    private void decodeFileInMemory(ProjectFilePart part, MetaData metaData, byte[] bytes, File saveAs, byte[] padding) throws Exception {
        // none encoding hash
        BigHash fileHash = metaData.getEncodings().get(0).getHash();
        debugOut("Decoding file (in memory) " + fileHash);
        // decode the file -- rewind the encodings
        List<FileEncoding> encodings = metaData.getEncodings();
        for (int i = encodings.size() - 1; i >= 0; i--) {
            FileEncoding fe = encodings.get(i);
            if (fe.getName().equals(FileEncoding.GZIP)) {
                bytes = CompressionUtil.gzipDecompress(bytes);
            } else if (fe.getName().equals(FileEncoding.LZMA)) {
                bytes = CompressionUtil.lzmaDecompress(bytes);
            } else if (fe.getName().equals(FileEncoding.AES)) {
                // if no global passphrase, use the one in the encoding
                if (passphrase == null) {
                    bytes = SecurityUtil.decrypt(fe.getProperty(FileEncoding.PROP_PASSPHRASE), bytes);
                } else {
                    debugOut("Decoding (in memory) using passphrase: " + passphrase);
                    bytes = SecurityUtil.decrypt(passphrase, bytes);
                }
            }
        }
        // lastly, trim off any padding (passphrase is added as padding to encrypted files)
        debugOut("Padding length: " + padding.length);
        if (padding.length > 0) {
            byte[] trimmedBytes = new byte[bytes.length - padding.length];
            System.arraycopy(bytes, 0, trimmedBytes, 0, trimmedBytes.length);
            bytes = trimmedBytes;
        }
        // save the decoded bytes to the expected file
        IOUtil.setBytes(bytes, saveAs);
        // validate
        validateFile(fileHash, metaData, saveAs, padding);
        // set last modified timestamp
        saveAs.setLastModified(metaData.getTimestampFileModified());
        // done
        fireFinishedFile(fileHash, part);
    }

    /**
     * <p>Decodes a file from a temporary file.</p>
     * @param part The project file part. NULL if not part of a directory download or is a project file.
     * @param metaData The meta data for the file to be downloaded.
     * @param tempFile The file to which the downloaded bytes were saved.
     * @param saveAs The location to save the downloaded file.
     * @param padding The bytes that were tacked on the end up the uploaded file.
     * @throws java.lang.Exception
     */
    private void decodeFileDiskBacked(ProjectFilePart part, MetaData metaData, File tempFile, File saveAs, byte[] padding) throws Exception {
        // none encoding hash
        BigHash fileHash = metaData.getEncodings().get(0).getHash();
        debugOut("Decoding file (disk backed) " + fileHash);
        // decode the file -- rewind the encodings
        List<FileEncoding> encodings = metaData.getEncodings();
        for (int i = encodings.size() - 1; i >= 0; i--) {
            FileEncoding fe = encodings.get(i);
            if (fe.getName().equals(FileEncoding.GZIP)) {
                tempFile = CompressionUtil.gzipDecompress(tempFile);
            } else if (fe.getName().equals(FileEncoding.LZMA)) {
                tempFile = CompressionUtil.lzmaDecompress(tempFile);
            } else if (fe.getName().equals(FileEncoding.AES)) {
                // if no global passphrase, use the one in the encoding
                if (passphrase == null) {
                    tempFile = SecurityUtil.decrypt(fe.getProperty(FileEncoding.PROP_PASSPHRASE), tempFile);
                } else {
                    debugOut("Decoding (disk backed) using passphrase: " + passphrase);
                    tempFile = SecurityUtil.decrypt(passphrase, tempFile);
                }
            }
        }
        // lastly, trim off any padding
        debugOut("Padding length: " + padding.length);
        if (padding.length > 0) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(tempFile, "rw");
                raf.setLength(tempFile.length() - padding.length);
            } catch (IOException e) {
                IOUtil.safeClose(raf);
                // fallback on manually copying -- shouldn't ever make it here
                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    fis = new FileInputStream(tempFile);
                    File trimTempFile = TempFileUtil.createTemporaryFile();
                    fos = new FileOutputStream(trimTempFile);
                    IOUtil.getBytes(fos, fis, tempFile.length() - padding.length);
                    // update the references
                    IOUtil.safeDelete(tempFile);
                    tempFile = trimTempFile;
                } catch (IOException ex) {
                    throw new RuntimeException("Can't trim the decoded file.", ex);
                } finally {
                    IOUtil.safeClose(fos);
                    IOUtil.safeClose(fis);
                }
            } finally {
                IOUtil.safeClose(raf);
            }
        }
        // validate
        validateFile(fileHash, metaData, tempFile, padding);
        // finally, rename the decoded file to the expected one
        IOUtil.renameFallbackCopy(tempFile, saveAs);
        // set last modified timestamp
        saveAs.setLastModified(metaData.getTimestampFileModified());
        // done
        fireFinishedFile(fileHash, part);
    }

    /**
     * <p>Downloads a meta data chunk.</p>
     * @param hash A hash.
     * @return A meta data object.
     */
    private MetaData downloadMetaData(BigHash hash) {
        fireStartedMetaData(hash);
        MetaData downloadMetaData = null;
        try {
            Collection<String> hosts = getConnections(hash);
            if (hosts.isEmpty()) {
                throw new Exception("No servers to use.");
            }
            Set<PropagationExceptionWrapper> exceptions = new HashSet<PropagationExceptionWrapper>();

            // ---------------------------------------------------------------------------
            // For some reason, a chunk sporadically fails validation once in a long while.
            // However, this problem is easily reproducable using stress tests. (Running
            // over night, might happen 10 or more times.) The second time, works fine.
            //
            // Since trying multiple times does not incur serious overhead (unlike failing
            // when the mistake happens), please do not remove this logic until the
            // problem is solved. When the problem is solved AND demonstrated that it is
            // solved by passing the stress tests, then we can remove this. Thu Nov 12 2009
            // ---------------------------------------------------------------------------
            ATTEMPT:
            for (int attempt = 0; attempt < 2; attempt++) {
                HOSTS:
                for (String host : hosts) {
                    fireTryingMetaData(hash, host);
                    try {
                        TrancheServer ts = ConnectionUtil.connectHost(host, true);
                        if (ts == null) {
                            debugErr(new Exception("No connection for " + host + "."));
                            continue HOSTS;
                        }
                        try {
                            long start = TimeUtil.getTrancheTimestamp();
                            PropagationReturnWrapper wrapper = IOUtil.getMetaData(ts, hash, true);
                            debugOut("Milliseconds spent waiting for get meta data response: " + (TimeUtil.getTrancheTimestamp() - start));
                            for (PropagationExceptionWrapper pew : wrapper.getErrors()) {
                                debugOut(pew.toString());
                            }
                            if (!wrapper.isVoid()) {
                                byte[] bytes = IOUtil.get1DBytes(wrapper);
                                if (bytes == null) {
                                    continue HOSTS;
                                }
                                downloadMetaData = MetaData.createFromBytes(bytes);

                                fireFinishedMetaData(downloadMetaData, hash, host);
                                break ATTEMPT;
                            } else if (wrapper.isAnyErrors()) {
                                exceptions.addAll(wrapper.getErrors());
                            }
                        } finally {
                            ConnectionUtil.unlockConnection(host);
                        }
                    } catch (Exception e) {
                        exceptions.add(new PropagationExceptionWrapper(e, host, hash));
                        ConnectionUtil.reportExceptionHost(host, e);
                    }
                }
            }
            if (downloadMetaData == null) {
                fireFailedMetaData(hash, exceptions);
            }
        } catch (Exception e) {
            debugErr(e);
            fireFailedMetaData(hash, e);
        }
        return downloadMetaData;
    }

    /**
     * <p>Downloads a data chunk.</p>
     * @param fileHash The hash of the file to which the data chunk belongs.
     * @param chunkHash The data chunk hash.
     * @return Data chunk bytes.
     */
    protected byte[] downloadData(BigHash fileHash, BigHash chunkHash) {
        debugOut("Downloading data chunk " + chunkHash);
        fireStartedChunk(fileHash, chunkHash);
        byte[] bytes = null;
        try {
            Collection<String> hosts = getConnections(chunkHash);
            if (hosts.isEmpty()) {
                throw new Exception("No servers to use.");
            }
            Set<PropagationExceptionWrapper> exceptions = new HashSet<PropagationExceptionWrapper>();

            // ---------------------------------------------------------------------------
            // For some reason, a chunk sporadically fails validation once in a long while.
            // However, this problem is easily reproducable using stress tests. (Running
            // over night, might happen 10 or more times.) The second time, works fine.
            //
            // Since trying multiple times does not incur serious overhead (unlike failing
            // when the mistake happens), please do not remove this logic until the 
            // problem is solved. When the problem is solved AND demonstrated that it is
            // solved by passing the stress tests, then we can remove this. Thu Nov 12 2009
            // ---------------------------------------------------------------------------
            ATTEMPT:
            for (int attempt = 0; attempt < 2; attempt++) {
                HOSTS:
                for (String host : hosts) {
                    fireTryingChunk(fileHash, chunkHash, host);
                    try {
                        TrancheServer ts = ConnectionUtil.connectHost(host, true);
                        if (ts == null) {
                            debugErr(new Exception("No connection for " + host + "."));
                            continue HOSTS;
                        }
                        try {
                            long start = TimeUtil.getTrancheTimestamp();
                            PropagationReturnWrapper wrapper = IOUtil.getData(ts, chunkHash, true);
                            debugOut("Milliseconds spent waiting for get meta data response: " + (TimeUtil.getTrancheTimestamp() - start));
                            for (PropagationExceptionWrapper pew : wrapper.getErrors()) {
                                debugOut(pew.toString());
                            }
                            if (!wrapper.isVoid()) {
                                bytes = IOUtil.get1DBytes(wrapper);
                                // are the bytes valid?
                                if (!validateChunk(chunkHash, bytes)) {
                                    // -----------------------------------------------------------------------------
                                    // This sometimes happens, even when server has correct copy of chunk. Why?
                                    // -----------------------------------------------------------------------------
                                    debugOut("Invalid data chunk returned by " + host + ".");
                                    bytes = null;
                                    // try the next server
                                    continue HOSTS;
                                }
                                fireFinishedChunk(fileHash, chunkHash, host);
                                break ATTEMPT;
                            } else if (wrapper.isAnyErrors()) {
                                exceptions.addAll(wrapper.getErrors());
                            }
                        } finally {
                            ConnectionUtil.unlockConnection(host);
                        }
                    } catch (Exception e) {
                        exceptions.add(new PropagationExceptionWrapper(e, host, chunkHash));
                        ConnectionUtil.reportExceptionHost(host, e);
                        debugErr(e);
                    }
                }
            }
            if (bytes == null) {
                fireFailedChunk(fileHash, chunkHash, exceptions);
            }
        } catch (Exception e) {
            fireFailedChunk(fileHash, chunkHash, e);
        }
        return bytes;
    }

    /**
     * <p>Validates a data chunk</p>
     * @param expectedHash What hash is expected.
     * @param bytes The bytes to validate.
     * @return Whether the hash of the given bytes matches the expected hash.
     */
    protected boolean validateChunk(BigHash expectedHash, byte[] bytes) {
        if (validate) {
            BigHash actualHash = new BigHash(bytes);
            if (!actualHash.equals(expectedHash)) {
                debugErr(new Exception("Invalid chunk downloaded. Expected " + expectedHash + " but received " + actualHash));
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Validates a file.</p>
     * @param expectedHash What hash is expected.
     * @param metaData The meta data of the file
     * @param file The file to be validated.
     * @param padding The bytes that were tacked on the end up the uploaded file.
     * @throws java.lang.Exception
     */
    protected void validateFile(BigHash expectedHash, MetaData metaData, File file, byte[] padding) throws Exception {
        if (validate) {
            // validate the hash
            BigHash actualHash = new BigHash(file, padding);
            if (!actualHash.equals(expectedHash)) {
                throw new Exception("Decoded file does not match the expected file.\n  Expected: " + expectedHash + "\n  Found: " + actualHash);
            }
            // validate the signature
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                // verify that it matches what is expected
                if (!SecurityUtil.verify(fis, metaData.getSignature().getBytes(), metaData.getSignature().getAlgorithm(), metaData.getSignature().getCert().getPublicKey())) {
                    throw new CantVerifySignatureException("Data downloaded is invalid.");
                }
            } finally {
                IOUtil.safeClose(fis);
            }
        }
    }

    /**
     * <p>Notifies listeners that meta data is being started.</p>
     * @param hash The hash of the meta data.
     */
    private void fireStartedMetaData(BigHash hash) {
        fire(new GetFileToolEvent(GetFileToolEvent.ACTION_STARTED, GetFileToolEvent.TYPE_METADATA, hash));
    }

    /**
     * <p>Notifies listeners that meta data is trying to be downloaded.</p>
     * @param hash The hash of the meta data.
     * @param serverHost The host name of the server.
     */
    private void fireTryingMetaData(BigHash hash, String serverHost) {
        fire(new GetFileToolEvent(GetFileToolEvent.ACTION_TRYING, GetFileToolEvent.TYPE_METADATA, hash, serverHost));
    }

    /**
     * 
     * @param hash
     * @param exception
     */
    private void fireFailedMetaData(BigHash hash, Exception exception) {
        Set<PropagationExceptionWrapper> exceptions = new HashSet<PropagationExceptionWrapper>();
        exceptions.add(new PropagationExceptionWrapper(exception, hash));
        fireFailure(new GetFileToolEvent(GetFileToolEvent.ACTION_FAILED, GetFileToolEvent.TYPE_METADATA, hash), exceptions);
    }

    /**
     * <p>Notifies listeners that a meta data could not be downloaded.</p>
     * @param hash The hash of the meta data.
     * @param exceptions
     */
    private void fireFailedMetaData(BigHash hash, Collection<PropagationExceptionWrapper> exceptions) {
        fireFailure(new GetFileToolEvent(GetFileToolEvent.ACTION_FAILED, GetFileToolEvent.TYPE_METADATA, hash), exceptions);
    }

    /**
     * <p>Notifies listeners that a meta data was downloaded.</p>
     * @param metaData The meta data that was downloaded.
     * @param hash The hash of the meta data.
     * @param serverHost The host name of the server.
     */
    private void fireFinishedMetaData(MetaData metaData, BigHash hash, String serverHost) {
        if (!metaData.isProjectFile() && timeEstimator != null) {
            timeEstimator.update(timeEstimator.getBytesDone(), timeEstimator.getTotalBytes() - hash.getLength() + metaData.getEncodings().get(metaData.getEncodings().size() - 1).getHash().getLength(), timeEstimator.getFilesDone() + 1, timeEstimator.getTotalFiles());
        }
        fire(new GetFileToolEvent(GetFileToolEvent.ACTION_FINISHED, GetFileToolEvent.TYPE_METADATA, hash, serverHost));
    }

    /**
     * <p>Notifies listeners that a data chunk is going to be downloaded.</p>
     * @param fileHash The hash of the file to which the data chunk belongs.
     * @param chunkHash The hash of the data chunk.
     */
    private void fireStartedChunk(BigHash fileHash, BigHash chunkHash) {
        fire(new GetFileToolEvent(GetFileToolEvent.ACTION_STARTED, GetFileToolEvent.TYPE_DATA, fileHash, chunkHash));
    }

    /**
     * <p>Notifies listeners that a data chunk is trying to be downloaded.</p>
     * @param fileHash The hash of the file to which the data chunk belongs.
     * @param chunkHash The hash of the data chunk.
     * @param serverHost The host name of the server.
     */
    private void fireTryingChunk(BigHash fileHash, BigHash chunkHash, String serverHost) {
        fire(new GetFileToolEvent(GetFileToolEvent.ACTION_TRYING, GetFileToolEvent.TYPE_DATA, fileHash, serverHost, chunkHash));
    }

    /**
     * 
     * @param fileHash
     * @param chunkHash
     * @param exception
     */
    private void fireFailedChunk(BigHash fileHash, BigHash chunkHash, Exception exception) {
        Set<PropagationExceptionWrapper> exceptions = new HashSet<PropagationExceptionWrapper>();
        exceptions.add(new PropagationExceptionWrapper(exception, hash));
        fireFailure(new GetFileToolEvent(GetFileToolEvent.ACTION_FAILED, GetFileToolEvent.TYPE_DATA, fileHash, chunkHash), exceptions);
    }

    /**
     * <p>Notifies listeners that a data chunk could not be downloaded.</p>
     * @param fileHash The hash of the file to which the data chunk belongs.
     * @param chunkHash The hash of the data chunk.
     * @param exceptions
     */
    private void fireFailedChunk(BigHash fileHash, BigHash chunkHash, Collection<PropagationExceptionWrapper> exceptions) {
        fireFailure(new GetFileToolEvent(GetFileToolEvent.ACTION_FAILED, GetFileToolEvent.TYPE_DATA, fileHash, chunkHash), exceptions);
    }

    /**
     * <p>Notifies listeners that a data chunk could not be downloaded.</p>
     * @param fileHash The hash of the file to which the data chunk belongs.
     * @param chunkHash The hash of the data chunk.
     * @param serverHost The host name of the server.
     */
    private void fireFinishedChunk(BigHash fileHash, BigHash chunkHash, String serverHost) {
        if (timeEstimator != null) {
            timeEstimator.update(timeEstimator.getBytesDone() + chunkHash.getLength(), timeEstimator.getTotalBytes(), timeEstimator.getFilesDone(), timeEstimator.getTotalFiles());
        }
        fire(new GetFileToolEvent(GetFileToolEvent.ACTION_FINISHED, GetFileToolEvent.TYPE_DATA, fileHash, serverHost, chunkHash));
    }

    /**
     * <p>Notifies listeners that a file is being started.</p>
     * @param hash The hash of the file.
     */
    private void fireStartingFile(BigHash hash) {
        fire(new GetFileToolEvent(GetFileToolEvent.ACTION_STARTING, GetFileToolEvent.TYPE_FILE, hash));
    }

    /**
     * <p>Notifies listeners that a file has been skipped.</p>
     * @param hash The hash of the file.
     */
    private void fireSkippedFile(BigHash hash, MetaData metaData, ProjectFilePart part) {
        if (timeEstimator != null) {
            timeEstimator.update(timeEstimator.getBytesDone() + hash.getLength(), timeEstimator.getTotalBytes(), timeEstimator.getFilesDone() + 1, timeEstimator.getTotalFiles());
        }
        // need to track whether files were skipped
        skippedFile = true;
        if (part == null) {
            fire(new GetFileToolEvent(GetFileToolEvent.ACTION_SKIPPED, GetFileToolEvent.TYPE_FILE, metaData.getName(), hash));
        } else {
            fire(new GetFileToolEvent(GetFileToolEvent.ACTION_SKIPPED, GetFileToolEvent.TYPE_FILE, part.getRelativeName(), hash));
        }
    }

    /**
     * <p>Notifies listeners that a file has been started.</p>
     * @param hash The hash of the file.
     * @param relativeName The relative name of the file in the data set.
     */
    private void fireStartedFile(BigHash hash, String relativeName) {
        fire(new GetFileToolEvent(GetFileToolEvent.ACTION_STARTED, GetFileToolEvent.TYPE_FILE, relativeName, hash));
    }

    /**
     * 
     * @param hash
     * @param exception
     */
    private void fireFailedFile(BigHash hash, Exception exception) {
        Set<PropagationExceptionWrapper> exceptions = new HashSet<PropagationExceptionWrapper>();
        exceptions.add(new PropagationExceptionWrapper(exception, hash));
        fireFailure(new GetFileToolEvent(GetFileToolEvent.ACTION_FAILED, GetFileToolEvent.TYPE_FILE, hash), exceptions);
    }

    /**
     * <p>Notifies listeners that a file could not be downloaded.</p>
     * @param hash The hash of the file.
     * @param exceptions
     */
    private void fireFailedFile(BigHash hash, Collection<PropagationExceptionWrapper> exceptions) {
        fireFailure(new GetFileToolEvent(GetFileToolEvent.ACTION_FAILED, GetFileToolEvent.TYPE_FILE, hash), exceptions);
    }

    /**
     * 
     * @param hash
     * @param fileName
     * @param exception
     */
    private void fireFailedFile(BigHash hash, String fileName, Exception exception) {
        Set<PropagationExceptionWrapper> exceptions = new HashSet<PropagationExceptionWrapper>();
        exceptions.add(new PropagationExceptionWrapper(exception, hash));
        fireFailure(new GetFileToolEvent(GetFileToolEvent.ACTION_FAILED, GetFileToolEvent.TYPE_FILE, fileName, hash), exceptions);
    }

    /**
     * <p>Notifies listeners that a file could not be downloaded.</p>
     * @param hash The hash of the file.
     * @param fileName The relative name of the file in the data set.
     * @param exceptions
     */
    private void fireFailedFile(BigHash hash, String fileName, Collection<PropagationExceptionWrapper> exceptions) {
        fireFailure(new GetFileToolEvent(GetFileToolEvent.ACTION_FAILED, GetFileToolEvent.TYPE_FILE, fileName, hash), exceptions);
    }

    /**
     * <p>Notifies listeners that a file was downloaded.</p>
     * @param hash The hash of the file.
     */
    private void fireFinishedFile(BigHash hash, ProjectFilePart part) {
        if (part == null) {
            fire(new GetFileToolEvent(GetFileToolEvent.ACTION_FINISHED, GetFileToolEvent.TYPE_FILE, hash));
        } else {
            fire(new GetFileToolEvent(GetFileToolEvent.ACTION_FINISHED, GetFileToolEvent.TYPE_FILE, part.getRelativeName(), hash));
        }
    }

    /**
     * <p>Notifies listeners that directory download is being started.</p>
     * @param hash The hash of the directory.
     */
    private void fireStartingDirectory(BigHash hash) {
        fire(new GetFileToolEvent(GetFileToolEvent.ACTION_STARTING, GetFileToolEvent.TYPE_DIRECTORY, hash));
    }

    /**
     * <p>Notifies listeners that a directory download has started.</p>
     * @param hash The hash of the directory.
     */
    private void fireStartedDirectory(BigHash hash) {
        fire(new GetFileToolEvent(GetFileToolEvent.ACTION_STARTED, GetFileToolEvent.TYPE_DIRECTORY, hash));
    }

    /**
     * <p>Notifies listeners that a directory download has finished.</p>
     * @param hash The hash of the directory.
     */
    private void fireFinishedDirectory(BigHash hash) {
        fire(new GetFileToolEvent(GetFileToolEvent.ACTION_FINISHED, GetFileToolEvent.TYPE_DIRECTORY, hash));
    }

    /**
     * 
     * @param hash
     * @param exception
     */
    private void fireFailedDirectory(BigHash hash, Exception exception) {
        Set<PropagationExceptionWrapper> exceptions = new HashSet<PropagationExceptionWrapper>();
        exceptions.add(new PropagationExceptionWrapper(exception, hash));
        fireFailure(new GetFileToolEvent(GetFileToolEvent.ACTION_FAILED, GetFileToolEvent.TYPE_DIRECTORY, hash), exceptions);
    }

    /**
     * <p>Notifies listeners that a directory download has failed.</p>
     * @param hash The hash of the directory.
     * @param exceptions
     */
    private void fireFailedDirectory(BigHash hash, Collection<PropagationExceptionWrapper> exceptions) {
        fireFailure(new GetFileToolEvent(GetFileToolEvent.ACTION_FAILED, GetFileToolEvent.TYPE_DIRECTORY, hash), exceptions);
    }

    /**
     * <p>Fires the given event.</p>
     * @param event An event.
     */
    private void fire(GetFileToolEvent event) {
        // break point
        if (isStopped()) {
            return;
        }
        waitHereOnPause();
        for (GetFileToolListener l : getListeners()) {
            try {
                if (event.getType() == GetFileToolEvent.TYPE_METADATA) {
                    if (event.getAction() == GetFileToolEvent.ACTION_STARTED) {
                        l.startedMetaData(event);
                    } else if (event.getAction() == GetFileToolEvent.ACTION_TRYING) {
                        l.tryingMetaData(event);
                    } else if (event.getAction() == GetFileToolEvent.ACTION_FINISHED) {
                        l.finishedMetaData(event);
                    }
                } else if (event.getType() == GetFileToolEvent.TYPE_DATA) {
                    if (event.getAction() == GetFileToolEvent.ACTION_STARTED) {
                        l.startedData(event);
                    } else if (event.getAction() == GetFileToolEvent.ACTION_TRYING) {
                        l.tryingData(event);
                    } else if (event.getAction() == GetFileToolEvent.ACTION_FINISHED) {
                        l.finishedData(event);
                    }
                } else if (event.getType() == GetFileToolEvent.TYPE_FILE) {
                    if (event.getAction() == GetFileToolEvent.ACTION_STARTING) {
                        l.startingFile(event);
                    } else if (event.getAction() == GetFileToolEvent.ACTION_STARTED) {
                        l.startedFile(event);
                    } else if (event.getAction() == GetFileToolEvent.ACTION_SKIPPED) {
                        l.skippedFile(event);
                    } else if (event.getAction() == GetFileToolEvent.ACTION_FINISHED) {
                        l.finishedFile(event);
                    }
                } else if (event.getType() == GetFileToolEvent.TYPE_DIRECTORY) {
                    if (event.getAction() == GetFileToolEvent.ACTION_STARTING) {
                        l.startingDirectory(event);
                    } else if (event.getAction() == GetFileToolEvent.ACTION_STARTED) {
                        l.startedDirectory(event);
                    } else if (event.getAction() == GetFileToolEvent.ACTION_FINISHED) {
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
     * @param exceptions
     */
    private void fireFailure(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        // break point
        if (isStopped()) {
            return;
        }
        waitHereOnPause();
        for (GetFileToolListener l : getListeners()) {
            try {
                if (event.getType() == GetFileToolEvent.TYPE_METADATA) {
                    if (event.getAction() == GetFileToolEvent.ACTION_FAILED) {
                        l.failedMetaData(event, exceptions);
                    }
                } else if (event.getType() == GetFileToolEvent.TYPE_DATA) {
                    if (event.getAction() == GetFileToolEvent.ACTION_FAILED) {
                        l.failedData(event, exceptions);
                    }
                } else if (event.getType() == GetFileToolEvent.TYPE_FILE) {
                    if (event.getAction() == GetFileToolEvent.ACTION_FAILED) {
                        l.failedFile(event, exceptions);
                    }
                } else if (event.getType() == GetFileToolEvent.TYPE_DIRECTORY) {
                    if (event.getAction() == GetFileToolEvent.ACTION_FAILED) {
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
        waitHereOnPause();
        for (GetFileToolListener l : getListeners()) {
            try {
                l.message(message);
            } catch (Exception e) {
                debugErr(e);
            }
        }
    }

    /**
     * <p>Command-line interface. Use -h or --help for usage information, or if using Java API, use GetFileTool.printUsage() to print to standard out.</p>
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {

        // register the bouncy castle code
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        // load Tranche configuration
        ConfigureTranche.load(args);

        // print + exit args
        for (String arg : args) {
            if (arg.equals("-n") || arg.equals("--buildnumber")) {
                System.out.println("Tranche downloader, build: @buildNumber");
                if (!TestUtil.isTesting()) {
                    System.exit(0);
                } else {
                    return;
                }
            } else if (arg.equals("-h") || arg.equals("--help")) {
                printUsage(System.out);
                if (!TestUtil.isTesting()) {
                    System.exit(0);
                } else {
                    return;
                }
            }
        }

        // if no arguments, print and exit
        if (args.length <= 2) {
            printUsage(System.out);
            if (!TestUtil.isTesting()) {
                System.exit(2);
            } else {
                return;
            }
        }

        GetFileTool gft = new GetFileTool();
        // arguments
        boolean showSummary = DEFAULT_SHOW_SUMMARY;
        try {
            // get the arguments
            for (int i = 1; i < args.length - 2; i += 2) {
                String arg = args[i];
                if (arg.equals("-d") || arg.equals("--debug")) {
                    DebugUtil.setDebug(true);
                    setDebug(true);
                    i--;
                } else if (arg.equals("-v") || arg.equals("--verbose")) {
                    gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
                    i--;
                } else if (arg.equals("-S") || arg.equals("--summary")) {
                    showSummary = true;
                    i--;
                } else if (arg.equals("-e") || arg.equals("--passphrase")) {
                    gft.setPassphrase(args[i + 1]);
                } else if (arg.equals("-i") || arg.equals("--timestamp")) {
                    try {
                        gft.setUploadTimestamp(Long.valueOf(args[i + 1]));
                    } catch (Exception e) {
                        throw new UnknownArgumentException("Expect a number value for " + arg + ", received " + args[i + 1]);
                    }
                } else if (arg.equals("-o") || arg.equals("--output")) {
                    System.err.println("WARNING: The use of -o, --output has been deprecated.");
                } else if (arg.equals("-p") || arg.equals("--path")) {
                    gft.setUploadRelativePath(args[i + 1]);
                } else if (arg.equals("-r") || arg.equals("--regex")) {
                    gft.setRegEx(args[i + 1]);
                } else if (arg.equals("-s") || arg.equals("--useunspecified")) {
                    try {
                        gft.setUseUnspecifiedServers(Boolean.parseBoolean(args[i + 1]));
                    } catch (Exception e) {
                        throw new UnknownArgumentException("Expect a true/false value for " + arg + ", received " + args[i + 1]);
                    }
                } else if (arg.equals("-u") || arg.equals("--uname")) {
                    gft.setUploaderName(args[i + 1]);
                } else if (arg.equals("-V") || arg.equals("--server")) {
                    gft.getServersToUse().add(args[i + 1]);
                } else if (arg.equals("-a") || arg.equals("--validate")) {
                    try {
                        gft.setValidate(Boolean.parseBoolean(args[i + 1]));
                    } catch (Exception e) {
                        throw new UnknownArgumentException("Expect a true/false value for " + arg + ", received " + args[i + 1]);
                    }
                } else if (arg.equals("-c") || arg.equals("--continue")) {
                    try {
                        gft.setContinueOnFailure(Boolean.parseBoolean(args[i + 1]));
                    } catch (Exception e) {
                        throw new UnknownArgumentException("Expect a true/false value for " + arg + ", received " + args[i + 1]);
                    }
                } else if (arg.equals("-b") || arg.equals("--batch")) {
                    try {
                        gft.setBatch(Boolean.parseBoolean(args[i + 1]));
                    } catch (Exception e) {
                        throw new UnknownArgumentException("Expect a true/false value for " + arg + ", received " + args[i + 1]);
                    }
                } else if (arg.equals("-m") || arg.equals("--tempdir")) {
                    TempFileUtil.setTemporaryDirectory(new File(args[i + 1]));
                } else if (arg.equals("-t") || arg.equals("--threads")) {
                    gft.setThreadCount(Integer.parseInt(args[i + 1]));
                } else if (arg.equals("-H") || arg.equals("--proxyhost")) {
                    System.err.println("WARNING: The use of -H, --proxyhost has been deprecated.");
                } else if (arg.equals("-X") || arg.equals("--proxyport")) {
                    System.err.println("WARNING: The use of -X, --proxyport has been deprecated.");
                } else {
                    throw new UnknownArgumentException("Unrecognized option: " + arg);
                }
            }

            // Set hash
            gft.setHash(BigHash.createHashFromString(args[args.length - 2]));
            // set the save location
            gft.setSaveFile(new File(args[args.length - 1]));
        } catch (Exception pae) {
            System.err.println("Problem with arguments: " + pae.getMessage());
            printUsage(System.err);
            if (!TestUtil.isTesting()) {
                System.exit(4);
            } else {
                throw pae;
            }
        }

        try {
            // perform the download
            MetaData metaData = gft.getMetaData();
            GetFileToolReport report = null;
            if (metaData != null) {
                if (metaData.isProjectFile()) {
                    report = gft.getDirectory();
                } else {
                    report = gft.getFile();
                }
            }

            // make a report
            if (report.isFailed()) {
                System.err.println("FAILURE");
                // Aware of JUnit tests calling GetFileTool.main
                if (!TestUtil.isTesting()) {
                    System.exit(3);
                } else {
                    throw new RuntimeException("Download failed.");
                }
            } else {
                // The only output: full path of downloaded project
                System.out.println(gft.getSaveFile().getAbsolutePath());
                // show summary
                if (showSummary) {
                    System.out.println("Time elapsed: " + Text.getPrettyEllapsedTimeString(report.getTimeToFinish()));
                }
                // Aware of JUnit tests calling GetFileTool.main
                if (!TestUtil.isTesting()) {
                    System.exit(0);
                } else {
                    return;
                }
            }
        } catch (PassphraseRequiredException pre) {
            System.err.println("Problem unencrypting data: " + pre.getMessage());
            if (!TestUtil.isTesting()) {
                System.exit(6);
            } else {
                throw pre;
            }
        } catch (FileNotFoundException ce) {
            System.err.println("Can't find the file:" + ce.getMessage());
            LogUtil.logError(ce);
            if (!TestUtil.isTesting()) {
                System.exit(2);
            } else {
                throw ce;
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + " with message: " + e.getMessage());
            LogUtil.logError(e);
            if (!TestUtil.isTesting()) {
                System.exit(1);
            } else {
                throw e;
            }
        }
    }

    /**
     * <p>Print out command-line usage.</p>
     * @param console
     */
    private static void printUsage(PrintStream console) {
        console.println();
        console.println("USAGE");
        console.println("    [FLAGS] <HASH> <SAVE LOCATION>");
        console.println();
        console.println("DESCRIPTION");
        console.println("    Downloads the data set with the given HASH to the SAVE LOCATION.");
        console.println();
        console.println("    If download succeeds, the only standard output is the path to the directory of the download. See RETURN CODES for the values of the return codes.");
        console.println();
        console.println("MEMORY ALLOCATION");
        console.println("    You should use the JVM option:");
        console.println();
        console.println("    java -Xmx512m");
        console.println();
        console.println("    The allocates 512m of memory for the tool. You can adjust this amount (e.g., you don't have 512MB available memory or you want to allocate more.)");
        console.println();
        console.println("OUTPUT FLAGS");
        console.println("    -d, --debug                 Value: none.           If you have problems with a download, you can use this option to print detailed information. This helps us find potential errors in the code.");
        console.println("    -h, --help                  Value: none.           Print usage and exit. All other arguments will be ignored.");
        console.println("    -n, --buildnumber           Value: none.           Print version number and exit. All other arguments will be ignored.");
        console.println("    -S, --summary               Value: none.           Prints out a summary of time and speed to standard error upon completion of a download.");
        console.println("    -v, --verbose               Value: none.           Print download progress information.");
        console.println();
        console.println("STANDARD PARAMETERS");
        console.println("    -e, --passphrase            Value: any string.     If the data set is encrypted, use this to unencrypt.");
        console.println("    -i, --timestamp             Value: any number.     Specifies the timestamp that the given file or data set was uploaded. Serves to disambiguate the data set that is to be downloaded.");
        console.println("    -p, --path                  Value: any string.     Specifies the relative path in a data set of the file that is to be downloaded. Serves to disambiguate the file that is to be downloaded.");
        console.println("    -s, --useunspecified        Value: true/false.     Whether unspecified servers should be used. If false, only the servers that you specify with the \"-V\" or \"--server\" flags will be used. Otherwise, other servers will be used, as well. Default value is " + DEFAULT_USE_UNSPECIFIED_SERVERS + ".");
        console.println("    -r, --regex                 Value: any string.     Matches a specified regular expression with the relative name of every file (as a file). Uses standard Java regular expression rules.");
        console.println("    -u, --uname                 Value: any string.     Specifies the name of the uploader for the given file or data set. Serves to disambiguate the file or data set that is to be downloaded.");
        console.println("    -V, --server                Value: any string.     Specify the host name of a preferred server. If want to specify more than one, use flag multiple times (e.g., \"-V 141.214.241.100 -V 100.100.100.100\").");
        console.println();
        console.println("ADDITIONAL PARAMETERS");
        console.println("    We recommend you use the default values, which are adjusted for best performance and stability.");
        console.println();
        console.println("    -a, --validate              Value: true/false.     Verify integrity of data and validate signed by proper users. Default value is " + DEFAULT_VALIDATE + ".");
        console.println("    -b, --batch                 Value: true/false.     Enables faster uploads by simulateously downloading small chunks of data together. Default value is " + DEFAULT_BATCH + ".");
        console.println("    -c, --continue              Value: true/false.     Upon failure, continue to download as much as possible. Default value is " + DEFAULT_CONTINUE_ON_FAILURE + ".");
        console.println("    -m, --tempdir               Value: any string.     Path to use for temporary directory instead of default. Default is based on different heuristics for OS and filesystem permissions. Default value is " + TempFileUtil.getTemporaryDirectory() + ".");
        console.println("    -t, --threads               Value: any number.     The maximum number of threads to use. Increasing may require more memory, and may result in increased CPU usage, increased bandwidth, increased disk accesses and faster downloads. Default value is " + DEFAULT_THREADS + ".");
        console.println();
        console.println("RETURN CODES");
        console.println("    To check the return code for a process in UNIX bash console, use $? special variable. If non-zero, check standard error for messages.");
        console.println();
        console.println("    0:     Program exited normally (e.g., download succeeded, help displayed, etc.)");
        console.println("    1:     Unknown error.");
        console.println("    2:     Some file was not found. Most likely, a file specified in an argument was not found.");
        console.println("    3:     Failed to download.");
        console.println("    4:     Problem with an argument.");
        console.println("    5:     Passphrase either wrong or missing.");
        console.println();
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static void setDebug(boolean debug) {
        GetFileTool.debug = debug;
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
    private static void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(GetFileTool.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param line
     */
    private static void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }

    private class FileDecoding {

        private File tempFile;
        private RandomAccessFile raf;
        private int dataChunksWrittenToRandomAccessFile = 0;
        private boolean failed = false;

        /**
         * 
         * @param tempFile
         * @param raf
         */
        public FileDecoding(File tempFile, RandomAccessFile raf) {
            this.tempFile = tempFile;
            this.raf = raf;
        }

        /**
         * 
         */
        public void fail() {
            debugOut("File decoding signalled to fail.");
            failed = true;
            IOUtil.safeClose(raf);
            IOUtil.safeDelete(tempFile);
        }

        /**
         * 
         * @return
         */
        public boolean isFailed() {
            return failed;
        }

        /**
         * 
         * @param dataChunk
         * @throws java.lang.Exception
         */
        public void processDataChunk(DataChunk dataChunk) throws Exception {
            // failed to download part of the file, don't bother going on
            if (!failed) {
                debugOut("Processing data chunk.");
                // write to the random access file?
                raf.seek(dataChunk.offset);
                raf.write(dataChunk.bytes);
                dataChunksWrittenToRandomAccessFile++;
            }
        }
    }

    private class DataChunk implements Comparable<DataChunk> {

        public final long offset;
        public final BigHash hash;
        public final MetaChunk metaChunk;
        private byte[] bytes;
        private final Set<String> serversTried = new HashSet<String>();

        /**
         *
         * @param offset
         * @param hash
         * @param metaChunk
         */
        public DataChunk(long offset, BigHash hash, MetaChunk metaChunk) {
            this.offset = offset;
            this.hash = hash;
            this.metaChunk = metaChunk;
        }

        /**
         * 
         * @return
         */
        public byte[] getBytes() {
            return bytes;
        }

        /**
         *
         * @param bytes
         */
        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        /**
         *
         * @param o
         * @return
         */
        public int compareTo(DataChunk c) {
            if (serversTried.size() < c.serversTried.size()) {
                return 1;
            } else if (serversTried.size() > c.serversTried.size()) {
                return -1;
            }
            return 0;
        }

        /**
         *
         * @param host
         */
        public void addServerTried(String host) {
            serversTried.add(host);
        }
    }

    private class MetaChunk implements Comparable<MetaChunk> {

        private ProjectFilePart part;
        private MetaData md;
        private final Set<String> serversTried = new HashSet<String>();
        public final FileDecoding fileDecoding;
        private File saveAs;

        /**
         * 
         * @param part
         * @throws java.lang.Exception
         */
        public MetaChunk(ProjectFilePart part) throws Exception {
            this.part = part;
            // only set up a file decoding object if the file is larger than 1MB
            if (part.getHash().getLength() > DataBlockUtil.ONE_MB) {
                // make a temp file
                File tempFile = createTemporaryFile(part.getRelativeName().replaceAll("[\\:*?\"<>|]", "-"), false);
                // open the temp file for writing
                RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
                // set the size
                raf.setLength(part.getHash().getLength());
                // creating the file decoding object
                fileDecoding = new FileDecoding(tempFile, raf);
            } else {
                fileDecoding = null;
            }
        }

        /**
         * 
         * @param hash
         * @param md
         * @throws java.lang.Exception
         */
        public MetaChunk(MetaData md) throws Exception {
            this(md, false);
        }

        /**
         * 
         * @param md
         * @param singleFile
         * @throws java.lang.Exception
         */
        public MetaChunk(MetaData md, boolean singleFile) throws Exception {
            this.md = md;
            // make a temp file
            File tempFile = createTemporaryFile(md.getName().replaceAll("[\\:*?\"<>|]", "-"), singleFile);
            // open the temp file for writing
            RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
            // set the size
            raf.setLength(md.getEncodings().get(md.getEncodings().size() - 1).getHash().getLength());
            // creating the file decoding object
            fileDecoding = new FileDecoding(tempFile, raf);
        }

        /**
         * 
         * @param o
         * @return
         */
        public int compareTo(MetaChunk c) {
            if (serversTried.size() < c.serversTried.size()) {
                return 1;
            } else if (serversTried.size() > c.serversTried.size()) {
                return -1;
            }
            return 0;
        }

        /**
         *
         * @param host
         */
        public void addServerTried(String host) {
            serversTried.add(host);
        }
    }

    private class DataChunkBatch {

        private ArrayList<DataChunk> list = new ArrayList<DataChunk>();
        private long size = 0;
    }

    private class MetaChunkBatch {

        private ArrayList<MetaChunk> list = new ArrayList<MetaChunk>();
        private long size = 0;
    }

    private class FileDataDownloadingThread extends Thread {

        private final LinkedList<DataChunk> dataList;
        private boolean started = false,  finished = false,  stopped = false;
        private List<PropagationExceptionWrapper> exceptions = new LinkedList<PropagationExceptionWrapper>();
        private Set<FileDataDownloadingThread> dataThreads;

        public FileDataDownloadingThread(LinkedList<DataChunk> dataList, Set<FileDataDownloadingThread> dataThreads) {
            setName("File Data Downloading Thread");
            setDaemon(true);
            this.dataList = dataList;
            this.dataThreads = dataThreads;
        }

        /**
         *
         * @return
         */
        public boolean isStarted() {
            return started;
        }

        /**
         * 
         */
        private void started() {
            started = true;
        }

        /**
         *
         * @return
         */
        public boolean isFinished() {
            return finished;
        }

        /**
         *
         */
        private void finished() {
            finished = true;
        }

        /**
         *
         */
        public void halt() {
            debugOut("Halting");
            stopped = true;
        }

        /**
         * 
         * @return
         */
        public boolean isStopped() {
            return stopped;
        }

        /**
         * 
         */
        private void haltAll() {
            // halt all the data downloading threads
            for (FileDataDownloadingThread thread : dataThreads) {
                thread.halt();
            }
        }

        /**
         * 
         * @param exception
         */
        private void addException(PropagationExceptionWrapper exception) {
            exceptions.add(exception);
        }

        /**
         *
         */
        public void waitForFinish() {
            while (!isStarted() || !isFinished()) {
                ThreadUtil.safeSleep(500);
            }
            debugOut("Exiting file data download thread.");
        }

        @Override
        public void run() {
            started();
            try {
                do {
                    DataChunk dataChunk = null;
                    try {
                        synchronized (dataList) {
                            if (!dataList.isEmpty()) {
                                dataChunk = dataList.remove();
                            }
                        }
                        if (dataChunk == null) {
                            break;
                        }
                        // downloading the chunk
                        dataChunk.setBytes(downloadData(dataChunk.metaChunk.md.getEncodings().get(dataChunk.metaChunk.md.getEncodings().size() - 1).getHash(), dataChunk.hash));
                        if (dataChunk.getBytes() == null) {
                            haltAll();
                            continue;
                        }
                        // write to the file
                        synchronized (dataChunk.metaChunk.fileDecoding) {
                            dataChunk.metaChunk.fileDecoding.processDataChunk(dataChunk);
                        }
                    } catch (Exception e) {
                        debugErr(e);
                        if (dataChunk == null) {
                            addException(new PropagationExceptionWrapper(e));
                        } else {
                            addException(new PropagationExceptionWrapper(e, dataChunk.hash));
                        }
                        haltAll();
                    }
                } while (!isStopped() && !GetFileTool.this.isStopped());
            } catch (Exception e) {
                debugErr(e);
            } finally {
                finished();
            }
        }
    }

    private class DirectoryDataDownloadingThread extends Thread {

        private final PriorityBlockingQueue<DataChunk> dataChunkQueue;
        private boolean started = false,  finished = false,  stopWhenFinished = false,  stopped = false;
        // batch data structures
        private final Map<String, DataChunkBatch> batchWaitingList;
        private Set<DirectoryDataDownloadingThread> dataThreads;
        private Set<DirectoryMetaDataDownloadingThread> metaThreads;

        /**
         *
         * @param dataChunkQueue
         */
        public DirectoryDataDownloadingThread(PriorityBlockingQueue<DataChunk> dataChunkQueue, Map<String, DataChunkBatch> batchWaitingList, Set<DirectoryDataDownloadingThread> dataThreads, Set<DirectoryMetaDataDownloadingThread> metaThreads) {
            setName("Directory Data Downloading Thread");
            setDaemon(true);
            this.dataChunkQueue = dataChunkQueue;
            this.batchWaitingList = batchWaitingList;
            this.dataThreads = dataThreads;
            this.metaThreads = metaThreads;
        }

        /**
         *
         * @return
         */
        public boolean isStarted() {
            return started;
        }

        /**
         * 
         */
        private void started() {
            started = true;
        }

        /**
         *
         * @return
         */
        public boolean isFinished() {
            return finished;
        }

        /**
         *
         */
        private void finished() {
            finished = true;
        }

        /**
         *
         */
        public void halt() {
            debugOut("Halting");
            stopped = true;
        }

        /**
         * 
         */
        private void haltAll() {
            for (DirectoryMetaDataDownloadingThread thread : metaThreads) {
                thread.halt();
            }
            for (DirectoryDataDownloadingThread thread : dataThreads) {
                thread.halt();
            }
        }

        /**
         * 
         * @return
         */
        public boolean isStopped() {
            return stopped;
        }

        /**
         *
         */
        public void waitForFinish() {
            stopWhenFinished();
            while (!isStarted() || !isFinished()) {
                ThreadUtil.safeSleep(500);
            }
            debugOut("Exiting data download thread.");
        }

        /**
         *
         */
        private void stopWhenFinished() {
            stopWhenFinished = true;
        }

        /**
         * 
         * @return
         */
        public boolean isStopWhenFinished() {
            return stopWhenFinished;
        }

        /**
         *
         * @param chunk
         * @param e
         */
        private void fireFailedChunk(DataChunk dataChunk, Exception e) {
            if (dataChunk == null) {
                debugOut("Data chunk is null?");
            } else {
                GetFileTool.this.fireFailedChunk(dataChunk.metaChunk.part.getHash(), dataChunk.hash, e);
                // fail the file
                if (dataChunk.metaChunk.fileDecoding != null) {
                    dataChunk.metaChunk.fileDecoding.fail();
                }
                fireFailedFile(dataChunk.metaChunk.part.getHash(), new Exception("Could not find part of the file."));
            }
            if (!GetFileTool.this.isContinueOnFailure()) {
                haltAll();
            }
        }

        /**
         *
         * @param chunk
         */
        private void putBackChunk(DataChunk chunk) {
            debugOut("Put back chunk " + chunk.hash);
            try {
                dataChunkQueue.put(chunk);
            } catch (Exception e) {
                fireFailedChunk(chunk, e);
            }
        }

        private String getBatchHost(DataChunk dataChunk) throws Exception {
            // add to batch waiting list
            String[] hosts = getConnections(dataChunk.hash).toArray(new String[0]);
            // no hosts to contact
            if (hosts.length == 0) {
                throw new Exception("No hosts for data chunk.");
            }
            // check if there are new hosts
            String host = null;
            for (String h : hosts) {
                if (!dataChunk.serversTried.contains(h)) {
                    host = h;
                    break;
                }
            }
            if (host == null) {
                throw new Exception("No more hosts to try for data chunk.");
            }
            return host;
        }

        private ArrayList<DataChunk> addToBatchList(DataChunk dataChunk, String host) {
            ArrayList<DataChunk> list = null;
            synchronized (batchWaitingList) {
                // add to the waiting list
                if (!batchWaitingList.containsKey(host)) {
                    batchWaitingList.put(host, new DataChunkBatch());
                }
                batchWaitingList.get(host).list.add(dataChunk);
                batchWaitingList.get(host).size += dataChunk.hash.getLength();
                // check whether it's time to download
                if (batchWaitingList.get(host).size >= DataBlockUtil.ONE_MB || batchWaitingList.get(host).list.size() == RemoteTrancheServer.BATCH_GET_LIMIT) {
                    // download from the host -- will handle all download logic and processing
                    list = batchWaitingList.remove(host).list;
                }
            }
            return list;
        }

        /**
         *
         * @param chunks
         * @param host
         */
        private void downloadBatch(ArrayList<DataChunk> chunks, String host) {
            debugOut("Downloading " + chunks.size() + " data chunks from " + host + ".");
            // fire events and build up hash array
            ArrayList<BigHash> hashes = new ArrayList<BigHash>();
            for (DataChunk chunk : chunks) {
                hashes.add(chunk.hash);
                fireTryingChunk(chunk.metaChunk.part.getHash(), chunk.hash, host);
                chunk.addServerTried(host);
            }
            // download
            try {
                TrancheServer ts = ConnectionUtil.connectHost(host, true);
                if (ts == null) {
                    throw new IOException("No connection with " + host + ".");
                }
                try {
                    long start = TimeUtil.getTrancheTimestamp();
                    PropagationReturnWrapper wrapper = ts.getData(hashes.toArray(new BigHash[0]), true);
                    debugOut("Milliseconds spent waiting for get data response (" + hashes.size() + " hashes): " + (TimeUtil.getTrancheTimestamp() - start));
                    for (PropagationExceptionWrapper pew : wrapper.getErrors()) {
                        debugOut(pew.toString());
                    }
                    if (wrapper.isVoid()) {
                        throw new NullPointerException("Null response returned by " + host + ".");
                    }
                    byte[][] dataBytesArray = IOUtil.get2DBytes(wrapper);
                    for (int i = 0; i < dataBytesArray.length; i++) {
                        try {
                            // did not exist on server/network
                            if (dataBytesArray[i] == null) {
                                putBackChunk(chunks.get(i));
                                continue;
                            }
                            // read
                            chunks.get(i).setBytes(dataBytesArray[i]);
                            // fire event
                            fireFinishedChunk(chunks.get(i).metaChunk.part.getHash(), chunks.get(i).hash, host);
                            // process
                            processData(chunks.get(i));
                        } catch (Exception e) {
                            debugErr(e);
                            putBackChunk(chunks.get(i));
                        }
                    }
                } finally {
                    ConnectionUtil.unlockConnection(host);
                }
            } catch (Exception e) {
                debugErr(e);
                ConnectionUtil.reportExceptionHost(host, e);
                for (DataChunk chunk : chunks) {
                    putBackChunk(chunk);
                }
            }
        }

        /**
         *
         */
        private void downloadRemainingBatchChunks() {
            debugOut("Downloading remaining data chunks.");
            while (true) {
                // break point
                if (isStopped() || GetFileTool.this.isStopped()) {
                    break;
                }
                String host = null;
                ArrayList<DataChunk> list = null;
                synchronized (batchWaitingList) {
                    if (batchWaitingList.isEmpty()) {
                        return;
                    }
                    host = batchWaitingList.keySet().toArray(new String[0])[0];
                    list = batchWaitingList.remove(host).list;
                }
                // download from the host -- will handle all download logic and processing
                downloadBatch(list, host);
                synchronized (dataChunkQueue) {
                    // are there chunks that could not be downloaded from this host?
                    debugOut("Data chunks left to download: " + dataChunkQueue.size());
                    for (int i = 0; i < dataChunkQueue.size(); i++) {
                        DataChunk dataChunk = dataChunkQueue.poll();
                        try {
                            String nextHost = getBatchHost(dataChunk);
                            addToBatchList(dataChunk, nextHost);
                        } catch (Exception e) {
                            fireFailedChunk(dataChunk, e);
                        }
                    }
                }
            }
        }

        /**
         *
         * @param dataChunk
         * @throws java.lang.Exception
         */
        private void processData(DataChunk dataChunk) throws Exception {
            debugOut("Processing data chunk " + dataChunk.hash);
            // writing to a random access file?
            if (dataChunk.metaChunk.part.getHash().getLength() > DataBlockUtil.ONE_MB) {
                synchronized (dataChunk.metaChunk.fileDecoding) {
                    dataChunk.metaChunk.fileDecoding.processDataChunk(dataChunk);
                    // not failed
                    if (!dataChunk.metaChunk.fileDecoding.failed) {
                        // done? decode
                        if (dataChunk.metaChunk.fileDecoding.dataChunksWrittenToRandomAccessFile == dataChunk.metaChunk.md.getParts().size()) {
                            IOUtil.safeClose(dataChunk.metaChunk.fileDecoding.raf);
                            decodeFileDiskBacked(dataChunk.metaChunk.part, dataChunk.metaChunk.md, dataChunk.metaChunk.fileDecoding.tempFile, dataChunk.metaChunk.saveAs, dataChunk.metaChunk.part.getPadding());
                            IOUtil.safeDelete(dataChunk.metaChunk.fileDecoding.tempFile);
                        }
                    }
                }
            } else {
                decodeFileInMemory(dataChunk.metaChunk.part, dataChunk.metaChunk.md, dataChunk.bytes, dataChunk.metaChunk.saveAs, dataChunk.metaChunk.part.getPadding());
            }
        }

        /**
         * 
         * @param dataChunk
         * @return
         */
        private boolean isFinished(DataChunk dataChunk) {
            boolean isForcedStop = isStopped() || GetFileTool.this.isStopped();
            boolean isExpectMore = (dataChunk != null) || !dataChunkQueue.isEmpty() || !isStopWhenFinished();
            return isForcedStop || !isExpectMore;
        }

        /**
         *
         */
        @Override
        public void run() {
            started();
            try {
                DataChunk dataChunk = null;
                do {
                    // get the next data chunk
                    do {
                        dataChunk = dataChunkQueue.poll(500, TimeUnit.MILLISECONDS);
                    } while (dataChunk == null && !stopWhenFinished && !(stopped || GetFileTool.this.stopped));
                    // break point
                    if (isStopped() || GetFileTool.this.isStopped()) {
                        break;
                    }
                    if (dataChunk == null && dataChunkQueue.isEmpty()) {
                        if (isBatch()) {
                            downloadRemainingBatchChunks();
                        }
                        continue;
                    }

                    // if this file failed to download
                    if (dataChunk.metaChunk.fileDecoding != null && dataChunk.metaChunk.fileDecoding.isFailed()) {
                        // do not bother to download this chunk
                        continue;
                    }
                    if (!isBatch()) {
                        try {
                            // download
                            dataChunk.setBytes(downloadData(dataChunk.metaChunk.part.getHash(), dataChunk.hash));
                            if (dataChunk.getBytes() == null) {
                                throw new Exception("Could not find data with hash " + dataChunk.hash);
                            }
                            // process
                            processData(dataChunk);
                        } catch (Exception e) {
                            fireFailedChunk(dataChunk, e);
                        }
                    } else {
                        try {
                            // is this the first time around?
                            if (dataChunk.serversTried.isEmpty()) {
                                fireStartedChunk(dataChunk.metaChunk.part.getHash(), dataChunk.hash);
                            }
                            String host = getBatchHost(dataChunk);
                            ArrayList<DataChunk> list = addToBatchList(dataChunk, host);
                            if (list != null) {
                                downloadBatch(list, host);
                            }
                        } catch (Exception e) {
                            fireFailedChunk(dataChunk, e);
                        }
                    }
                } while (!isFinished(dataChunk));
            } catch (Exception e) {
                debugErr(e);
            } finally {
                finished();
            }
        }
    }

    private class DirectoryMetaDataDownloadingThread extends Thread {

        private final LinkedList<MetaChunk> metaChunks;
        private final PriorityBlockingQueue<DataChunk> dataChunkQueue;
        private boolean started = false,  finished = false,  stopped = false;
        // batch data structures
        private final Map<String, MetaChunkBatch> batchWaitingList;
        private Set<DirectoryDataDownloadingThread> dataThreads;
        private Set<DirectoryMetaDataDownloadingThread> metaThreads;

        public DirectoryMetaDataDownloadingThread(LinkedList<MetaChunk> metaChunks, PriorityBlockingQueue<DataChunk> dataChunkQueue, Map<String, MetaChunkBatch> batchWaitingList, Set<DirectoryDataDownloadingThread> dataThreads, Set<DirectoryMetaDataDownloadingThread> metaThreads) {
            setName("Directory Meta Data Downloading Thread");
            setDaemon(true);
            this.metaChunks = metaChunks;
            this.dataChunkQueue = dataChunkQueue;
            this.batchWaitingList = batchWaitingList;
            this.dataThreads = dataThreads;
            this.metaThreads = metaThreads;
        }

        /**
         *
         * @return
         */
        public boolean isStarted() {
            return started;
        }

        /**
         *
         * @return
         */
        public boolean isFinished() {
            return finished;
        }

        /**
         *
         */
        public void halt() {
            stopped = true;
        }

        /**
         * 
         */
        private void haltAll() {
            for (DirectoryMetaDataDownloadingThread thread : metaThreads) {
                thread.halt();
            }
            for (DirectoryDataDownloadingThread thread : dataThreads) {
                thread.halt();
            }
        }

        /**
         *
         * @return
         */
        public boolean isStopped() {
            return stopped;
        }

        /**
         *
         * @return
         */
        public void waitForFinish() {
            while (!isStarted() || !isFinished()) {
                ThreadUtil.safeSleep(500);
            }
        }

        /**
         *
         * @param metaChunk
         * @throws java.lang.Exception
         */
        private void processMetaData(MetaChunk metaChunk) throws Exception {
            debugOut("Processing meta data for " + metaChunk.part.getHash());
            // add all the part chunks to the data queue
            long offset = 0;
            for (BigHash dataChunkHash : metaChunk.md.getParts()) {
                debugOut("Queueing chunk " + dataChunkHash);
                // wait until there is room
                while (dataChunkQueue.size() >= DEFAULT_DATA_QUEUE_SIZE) {
                    ThreadUtil.safeSleep(50);

                    if (isStopped()) {
                        return;
                    }
                }
                dataChunkQueue.put(new DataChunk(offset, dataChunkHash, metaChunk));
                offset += dataChunkHash.getLength();
            }
        }

        /**
         *
         * @param chunk
         */
        private void putBackChunk(MetaChunk chunk) {
            synchronized (metaChunks) {
                metaChunks.addFirst(chunk);
            }
        }

        /**
         *
         * @param chunk
         * @param e
         */
        private void fireFailedChunk(MetaChunk chunk, Exception e) {
            if (chunk == null) {
                debugOut("Meta chunk is null?");
            } else {
                GetFileTool.this.fireFailedMetaData(chunk.part.getHash(), e);
                fireFailedFile(chunk.part.getHash(), new Exception("Could not find the meta data of the file."));
                // fail the file
                if (chunk.fileDecoding != null) {
                    chunk.fileDecoding.fail();
                }
            }
            if (!GetFileTool.this.isContinueOnFailure()) {
                haltAll();
            }
        }

        /**
         *
         * @param chunks
         * @param host
         * @throws java.lang.Exception
         */
        private void downloadBatch(ArrayList<MetaChunk> chunks, String host) {
            debugOut("Downloading " + chunks.size() + " meta data from " + host + ".");
            // fire events and build up hash array
            ArrayList<BigHash> hashes = new ArrayList<BigHash>();
            for (MetaChunk chunk : chunks) {
                hashes.add(chunk.part.getHash());
                fireTryingMetaData(chunk.part.getHash(), host);
                chunk.addServerTried(host);
            }
            // download
            try {
                TrancheServer ts = ConnectionUtil.connectHost(host, true);
                if (ts == null) {
                    throw new IOException("No connection for " + host + ".");
                }
                try {
                    long start = TimeUtil.getTrancheTimestamp();
                    PropagationReturnWrapper wrapper = ts.getMetaData(hashes.toArray(new BigHash[0]), true);
                    debugOut("Milliseconds spent waiting for get meta data response (" + hashes.size() + " hashes): " + (TimeUtil.getTrancheTimestamp() - start));
                    for (PropagationExceptionWrapper pew : wrapper.getErrors()) {
                        debugOut(pew.toString());
                    }
                    if (wrapper.isVoid()) {
                        throw new NullPointerException("Null bytes returned by " + host + ".");
                    }
                    byte[][] metaDataBytesArray = IOUtil.get2DBytes(wrapper);
                    for (int i = 0; i < metaDataBytesArray.length; i++) {
                        try {
                            // did not exist on server/network
                            if (metaDataBytesArray[i] == null) {
                                putBackChunk(chunks.get(i));
                                continue;
                            }
                            // read
                            chunks.get(i).md = MetaData.createFromBytes(metaDataBytesArray[i]);
                            fireFinishedMetaData(chunks.get(i).md, chunks.get(i).part.getHash(), host);
                            // process
                            processMetaData(chunks.get(i));
                        } catch (Exception e) {
                            debugErr(e);
                            putBackChunk(chunks.get(i));
                        }
                    }
                } finally {
                    ConnectionUtil.unlockConnection(host);
                }
            } catch (Exception e) {
                // put back all the chunks
                for (MetaChunk chunk : chunks) {
                    putBackChunk(chunk);
                }
                debugErr(e);
                ConnectionUtil.reportExceptionHost(host, e);
            }
        }

        private String getBatchHost(MetaChunk metaChunk) throws Exception {
            // add to batch waiting list
            String[] hosts = getConnections(metaChunk.part.getHash()).toArray(new String[0]);
            // no hosts to contact
            if (hosts.length == 0) {
                throw new IOException("No hosts for meta data.");
            }
            // check if there are new hosts
            String host = null;
            for (String h : hosts) {
                if (!metaChunk.serversTried.contains(h)) {
                    host = h;
                    break;
                }
            }
            if (host == null) {
                throw new IOException("No more hosts to try for meta data chunk.");
            }
            return host;
        }

        private ArrayList<MetaChunk> addToBatchList(MetaChunk metaChunk, String host) {
            // add to the waiting list
            ArrayList<MetaChunk> list = null;
            synchronized (batchWaitingList) {
                if (!batchWaitingList.containsKey(host)) {
                    batchWaitingList.put(host, new MetaChunkBatch());
                }
                batchWaitingList.get(host).list.add(metaChunk);
                batchWaitingList.get(host).size += metaChunk.part.getHash().getLength();
                // check whether it's time to download
                if (batchWaitingList.get(host).size >= LIMIT_META_DATA_SIZE_ON_DISK || batchWaitingList.get(host).list.size() == RemoteTrancheServer.BATCH_GET_LIMIT) {
                    list = batchWaitingList.remove(host).list;
                }
            }
            return list;
        }

        /**
         *
         */
        private void downloadRemainingBatchChunks() {
            while (true) {
                // break point
                if (isStopped() || GetFileTool.this.isStopped()) {
                    break;
                }
                String host = null;
                ArrayList<MetaChunk> list = null;
                synchronized (batchWaitingList) {
                    if (batchWaitingList.isEmpty()) {
                        return;
                    }
                    host = batchWaitingList.keySet().toArray(new String[0])[0];
                    list = batchWaitingList.remove(host).list;
                }
                // download from the host -- will handle all download logic and processing
                downloadBatch(list, host);
                // are there chunks that could not be downloaded from this host?
                synchronized (metaChunks) {
                    debugOut("Meta chunks left to download: " + metaChunks.size());
                    for (int i = 0; i < metaChunks.size(); i++) {
                        MetaChunk metaChunk = metaChunks.removeFirst();
                        try {
                            String nextHost = getBatchHost(metaChunk);
                            addToBatchList(metaChunk, nextHost);
                        } catch (Exception e) {
                            fireFailedChunk(metaChunk, e);
                        }
                    }
                }
            }
        }

        /**
         *
         */
        @Override
        public void run() {
            started = true;
            try {
                do {
                    MetaChunk metaChunk = null;
                    synchronized (metaChunks) {
                        if (!metaChunks.isEmpty()) {
                            metaChunk = metaChunks.removeFirst();
                        }
                    }
                    // break point
                    if (isStopped() || GetFileTool.this.isStopped()) {
                        break;
                    }
                    if (metaChunk == null) {
                        if (isBatch()) {
                            downloadRemainingBatchChunks();
                        }
                        // did anything get put back?
                        if (metaChunks.isEmpty()) {
                            break;
                        }
                    } else {
                        try {
                            // is this the first time around?
                            if (metaChunk.serversTried.isEmpty()) {
                                fireStartingFile(metaChunk.part.getHash());
                                // check for skip
                                metaChunk.saveAs = new File(GetFileTool.this.saveTo.getAbsolutePath() + File.separator + metaChunk.part.getRelativeName());
                                if (skipFile(metaChunk.part.getHash(), metaChunk.saveAs, metaChunk.part.getPadding())) {
                                    fireSkippedFile(metaChunk.part.getHash(), metaChunk.md, metaChunk.part);
                                    continue;
                                }
                                fireStartedFile(metaChunk.part.getHash(), metaChunk.part.getRelativeName());
                            }
                            if (!isBatch()) {
                                // download the meta data
                                metaChunk.md = GetFileTool.this.downloadMetaData(metaChunk.part.getHash());
                                if (metaChunk.md == null) {
                                    throw new NullPointerException("Could not find meta data: " + metaChunk.part.getHash());
                                }
                                // process
                                processMetaData(metaChunk);
                            } else {
                                // is this the first time around?
                                if (metaChunk.serversTried.isEmpty()) {
                                    fireStartedMetaData(metaChunk.part.getHash());
                                }
                                String host = getBatchHost(metaChunk);
                                ArrayList<MetaChunk> list = addToBatchList(metaChunk, host);
                                if (list != null) {
                                    downloadBatch(list, host);
                                }
                            }
                        } catch (Exception e) {
                            fireFailedChunk(metaChunk, e);
                        }
                    }
                } while (!isStopped() && !GetFileTool.this.isStopped());
            } catch (Exception e) {
                debugErr(e);
            } finally {
                finished = true;
            }
        }
    }
}
