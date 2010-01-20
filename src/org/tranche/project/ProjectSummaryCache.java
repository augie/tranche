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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.tranche.ConfigureTranche;
import org.tranche.get.GetFileTool;
import org.tranche.get.GetFileToolReport;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.security.EasySSLProtocolSocketFactory;
import org.tranche.time.TimeUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.ThreadUtil;

/**
 * Utility to retrieve and parse project cache file, as well as manage aspects of cache.
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ProjectSummaryCache {

    static {
        // load the HTTPS protocol just once on startup
        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
    }
    private static boolean debug = false;
    private static final Set<ProjectSummary> projects = new HashSet<ProjectSummary>();
    private static boolean startedLoading = false;
    private static final Thread cacheLoadingThread = new Thread("Project Cache Loading Thread") {

        @Override
        public void run() {
            File cacheFile = TempFileUtil.getTemporaryFile();
            try {
                long start = TimeUtil.getTrancheTimestamp();
                List<BigHash> hashesToTry = getNewestProjectCacheHashes();

                debugOut("Total of " + hashesToTry.size() + " project cache hashes to try...");

                // get from the repository
                boolean haveCacheFile = false;

                String url = ConfigureTranche.get(ConfigureTranche.PROP_PROJECT_CACHE_URL);
                if (!haveCacheFile && url != null && !url.equals("")) {
                    debugOut("Loading cache from URL: " + url);
                    InputStream is = null;
                    FileWriter fw = null;
                    try {
                        fw = new FileWriter(cacheFile);
                        HttpClient hc = new HttpClient();
                        GetMethod gm = new GetMethod(url);
                        try {
                            int returnCode = hc.executeMethod(gm);
                            debugOut("HTTP Return code: " + returnCode);
                            if (returnCode == 200) {
                                haveCacheFile = true;
                                fw.write(gm.getResponseBodyAsString());
                            }
                        } catch (SSLException ssle) {
                            debugErr(ssle);
                        } catch (IOException ioe) {
                            debugErr(ioe);
                        } finally {
                            gm.releaseConnection();
                        }
                    } catch (Exception e) {
                        debugErr(e);
                    } finally {
                        IOUtil.safeClose(is);
                        IOUtil.safeClose(fw);
                    }
                }

                if (!haveCacheFile) {
                    for (BigHash h : hashesToTry) {
                        debugOut("* Trying cache: " + h);
                        GetFileTool gft = new GetFileTool();
                        gft.setHash(h);
                        gft.setSaveFile(cacheFile);
                        GetFileToolReport report = gft.getFile();
                        if (!report.isFailed()) {
                            haveCacheFile = true;
                            break;
                        } else {
                            IOUtil.safeDelete(cacheFile);
                        }
                    }
                }

                if (!haveCacheFile) {
                    throw new Exception("Unable to download a project cache file.");
                }

                readCacheFile(cacheFile);

                debugOut("Time to load cache: " + (TimeUtil.getTrancheTimestamp() - start) / 1000 + " seconds.");
                debugOut("Projects loaded from cache: " + projects.size());
            } catch (Exception e) {
                debugErr(e);
            } finally {
                IOUtil.safeDelete(cacheFile);
            }
        }
    };

    private static void readCacheFile(File cacheFile) throws Exception {
        BufferedReader in = null;
        FileReader fr = null;
        try {
            fr = new FileReader(cacheFile);
            in = new BufferedReader(fr);

            String nextLine, readName = null, readDescription = null, readType = null, uploader = null;
            long readTotalSize = 0, readFileCount = 0, readTimestamp = 0;
            BigHash readHash = null, readOldVersionHash = null, readNewVersionHash = null;
            boolean isEncrypted = false, isPublished = false, isHidden = false, shareMetaDataIfEncrypted = false;

            while ((nextLine = in.readLine()) != null) {

                try {
                    // Start next project
                    if (nextLine.startsWith("-----") || nextLine.startsWith("PROJECT")) {
                        if (readHash != null) {
                            // Add project (unless first project)
                            ProjectSummary pfs = new ProjectSummary(readHash, readName, readDescription, readTotalSize, readFileCount, readTimestamp, uploader, shareMetaDataIfEncrypted);

                            if (readName == null) {
                                pfs.title = "?";
                            }
                            if (readDescription == null) {
                                pfs.description = "";
                            }
                            if (readOldVersionHash != null) {
                                pfs.oldVersion = readOldVersionHash;
                            }
                            if (readNewVersionHash != null) {
                                pfs.newVersion = readNewVersionHash;
                            }
                            if (readType != null) {
                                pfs.type = readType;
                            }

                            // Flag so project table can handle
                            pfs.isEncrypted = isEncrypted;
                            pfs.isPublished = isPublished;
                            pfs.isHidden = isHidden;
                            pfs.shareMetaDataIfEncrypted = shareMetaDataIfEncrypted;

                            synchronized (projects) {
                                projects.add(pfs);
                            }
                        }

                        // To be safe, reset all values for next project
                        uploader = null;
                        readOldVersionHash = null;
                        readNewVersionHash = null;
                        readType = null;
                        readName = null;
                        readDescription = null;
                        readTotalSize = 0;
                        readFileCount = 0;
                        readTimestamp = 0;
                        readHash = null;
                        isEncrypted = false;
                        isPublished = false;
                        isHidden = false;
                        shareMetaDataIfEncrypted = false;
                    } else if (nextLine.startsWith("TITLE: ")) {
                        readName = nextLine.replace("TITLE: ", "").trim();
                    } else if (nextLine.startsWith("DESC: ")) {
                        readDescription = nextLine.replace("DESC: ", "").trim().replace("\\n", "\n");
                    } else if (nextLine.startsWith("SIZE: ")) {
                        readTotalSize = Long.parseLong(nextLine.replace("SIZE: ", "").trim());
                    } else if (nextLine.startsWith("FILES: ")) {
                        readFileCount = Long.parseLong(nextLine.replace("FILES: ", "").trim());
                    } else if (nextLine.startsWith("DATE: ")) {
                        readTimestamp = Long.parseLong(nextLine.replace("DATE: ", "").trim());
                    } else if (nextLine.startsWith("SIGS: ")) {
                        uploader = nextLine.replace("SIGS: ", "").trim();
                    } else if (nextLine.startsWith("HASH: ")) {
                        readHash = BigHash.createHashFromString(nextLine.replace("HASH: ", "").trim());
                    } else if (nextLine.startsWith("OLD: ")) {
                        readOldVersionHash = BigHash.createHashFromString(nextLine.replace("OLD: ", "").trim());
                    } else if (nextLine.startsWith("NEW: ") || nextLine.startsWith("NEW: ")) {
                        readNewVersionHash = BigHash.createHashFromString(nextLine.replace("NEW: ", "").trim());
                    } else if (nextLine.startsWith("ENCRYPTED: ") && nextLine.toLowerCase().contains("true")) {
                        isEncrypted = true;
                    } else if (nextLine.startsWith("ENCRYPTED: ") && nextLine.toLowerCase().contains("published")) {
                        isEncrypted = true;
                        isPublished = true;
                    } else if (nextLine.startsWith("DELETED: ") && nextLine.toLowerCase().contains("true")) {
                        isHidden = true;
                    } else if (nextLine.startsWith("TYPE: ")) {
                        readType = nextLine.replace("TYPE: ", "").trim();
                    } else if (nextLine.startsWith("SHARE MD IF ENC: ")) {
                        shareMetaDataIfEncrypted = true;
                    }
                } catch (Exception e) {
                }
            }
        } finally {
            IOUtil.safeClose(in);
            IOUtil.safeClose(fr);
        }
    }

    /**
     * Returns most recent hashes for cache (newest version).
     * @return
     */
    public static synchronized List<BigHash> getNewestProjectCacheHashes() {
        List<BigHash> newestHashes = new LinkedList<BigHash>();

        // read the hash
        BigHash newestCacheHash = null;
        try {
            newestCacheHash = BigHash.createHashFromString(ConfigureTranche.get(ConfigureTranche.PROP_PROJECT_CACHE_HASH));
            newestHashes.add(newestCacheHash);
        } catch (Exception e) {
            return Collections.unmodifiableList(newestHashes);
        }

        // start to download
        long startTime = TimeUtil.getTrancheTimestamp();

        GetFileTool gft = new GetFileTool();
        try {
            boolean go = true;
            while (go) {
                gft.setHash(newestCacheHash);
                MetaData metaData = gft.getMetaData();
                if (metaData.getNextVersion() != null) {
                    newestCacheHash = metaData.getNextVersion();
                    newestHashes.add(newestCacheHash);
                } else {
                    go = false;
                }
            }
        } catch (Exception e) {
            debugErr(e);
        }

        debugOut("Checking for new cache took " + (TimeUtil.getTrancheTimestamp() - startTime) / 1000 + "s.");
        debugOut("Newest cache hash: " + newestCacheHash);
        Collections.reverse(newestHashes);
        return Collections.unmodifiableList(newestHashes);
    }

    /**
     * 
     */
    public static void waitForStartup() {
        synchronized (cacheLoadingThread) {
            if (!startedLoading) {
                startedLoading = true;
                cacheLoadingThread.setDaemon(true);
                cacheLoadingThread.setPriority(Thread.MIN_PRIORITY);
                cacheLoadingThread.start();
            }
        }
        while (!startedLoading) {
            ThreadUtil.safeSleep(1000);
        }
        while (cacheLoadingThread.isAlive()) {
            try {
                cacheLoadingThread.join();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 
     * @return
     */
    public static Collection<ProjectSummary> getProjects() {
        List<ProjectSummary> list = new LinkedList<ProjectSummary>();
        synchronized (projects) {
            list.addAll(projects);
        }
        return Collections.unmodifiableCollection(list);
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        ProjectSummaryCache.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static final boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static final void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(ProjectSummaryCache.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
