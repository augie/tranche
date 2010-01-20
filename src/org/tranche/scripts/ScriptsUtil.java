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
package org.tranche.scripts;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tranche.TrancheServer;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.project.ProjectFile;
import org.tranche.project.ProjectFileUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>Utilities for scripts.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ScriptsUtil {

    /**
     * <p>If true, will ban servers that have met certain conditions.</p>
     */
    private static boolean isAllowBanning = true;
    /**
     * <p>If true, prints tracers.</p>
     */
    private static boolean isDebug = true;
    /**
     * <p>The cuttoff for a server. After this, it's banned from the instance if isAllowBanning set to true.</p>
     */
    private static final long FAILURE_TOKEN_CUTOFF = 20;
    /**
     * <p>The number of success tokens a server needs to decrement its failure token count.</p>
     */
    private static final long REQUIRED_SUCCESS_TOKENS_FOR_FAILURE_TOKEN_REMOVAL = 10;
    /**
     * <p>Keep track of each server's failure token counts. Can go up or down over time.</p>
     */
    private static Map<String, Long> failureTokenCounts = new HashMap<String, Long>();
    /**
     * <p>Keep track of each server's success token counts. Used to negate (decrement) failure tokens, though not necessarily evenly. See REQUIRED_SUCCESS_TOKENS_FOR_FAILURE_TOKEN_REMOVAL.</p>
     */
    private static Map<String, Long> successTokenCounts = new HashMap<String, Long>();

    /**
     * <p>The types of chunk: data, meta, project.</p>
     */
    public static enum ChunkType {

        DATA, META, PROJECT
    }

    /**
     * <p>Finds all online core servers with a particular chunk.</p>
     * @param hash The big hash of the chunk
     * @param type The ChunkType of the chunk (enumerated value), specifying whether data or meta data. Don't use PROJECT type as parameter.
     * @return
     * @throws java.lang.Exception
     */
    public static List<String> getServersWithChunk(BigHash hash, ChunkType type) throws Exception {
        List<String> coreServerUrls = new ArrayList();

        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
            if (row.isOnline()) {
                coreServerUrls.add(row.getURL());
            }
        }
        return getServersWithChunk(hash, type, coreServerUrls);
    }

    /**
     * <p>Finds all online core servers with a particular chunk.</p>
     * @param hash The big hash of the chunk
     * @param type The ChunkType of the chunk (enumerated value), specifying whether data or meta data. Don't use PROJECT type as parameter.
     * @param serversToUse List<String> of server urls to consider.
     * @return
     * @throws java.lang.Exception
     */
    public static List<String> getServersWithChunk(BigHash hash, ChunkType type, List<String> serversToUse) throws Exception {

        // Create a list to hold servers with chunks. May be any number, guesinng three for optimization.
        final List<String> urls = new ArrayList(3);

        // Ignore this. Holds urls that are confirmed to NOT have chunk
        final List<String> ignore = new ArrayList(3);

        // Shuffle to iron out latencies across samples
        Collections.shuffle(serversToUse);

        List<ServerLookupThread> queryThreads = new ArrayList();
        ServerLookupThread nextThread;
        for (String url : serversToUse) {
            nextThread = new ServerLookupThread(urls, ignore, url, hash, type);
            queryThreads.add(nextThread);
            nextThread.start();
        }

        Thread.yield();

        for (Thread t : queryThreads) {
            while (t.isAlive()) {
                try {
                    Thread.sleep(10);
                } catch (Exception ex) { /* nope  */ }
                Thread.yield();
            }
        }

        return urls;
    }

    /**
     * <p>Finds all online core servers without a particular chunk.</p>
     * @param hash The big hash of the chunk
     * @param type The ChunkType of the chunk (enumerated value), specifying whether data or meta data. Don't use PROJECT type as parameter.
     * @return
     * @throws java.lang.Exception
     */
    public static List<String> getServersWithoutChunk(BigHash hash, ChunkType type) throws Exception {
        List<String> coreServerUrls = new ArrayList();

        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
            if (row.isOnline()) {
                coreServerUrls.add(row.getURL());
            }
        }
        return getServersWithoutChunk(hash, type, coreServerUrls);
    }

    /**
     * <p>Finds all online core servers without a particular chunk.</p>
     * @param hash The big hash of the chunk
     * @param type The ChunkType of the chunk (enumerated value), specifying whether data or meta data. Don't use PROJECT type as parameter.
     * @param serversToUse List<String> of urls for servers you want to consider.
     * @return
     * @throws java.lang.Exception
     */
    public static List<String> getServersWithoutChunk(BigHash hash, ChunkType type, List<String> serversToUse) throws Exception {

        // Ignore this
        final List<String> ignore = new ArrayList(3);

        // Create a list to hold servers without chunks. May be any number, guesinng three for optimization.
        final List<String> urls = new ArrayList(3);

        // Shuffle to iron out latencies across samples
        Collections.shuffle(serversToUse);

        List<ServerLookupThread> queryThreads = new ArrayList();
        ServerLookupThread nextThread;
        for (String url : serversToUse) {
            nextThread = new ServerLookupThread(ignore, urls, url, hash, type);
            queryThreads.add(nextThread);
            nextThread.start();
        }

        Thread.yield();

        for (Thread t : queryThreads) {
            while (t.isAlive()) {
                try {
                    Thread.sleep(10);
                } catch (Exception ex) { /* nope  */ }
                Thread.yield();
            }
        }

        return urls;
    }

    /**
     * <p>Thread so concurrent lookups (network-bound activity).</p>
     * @param urls A list of urls with found hashes. Thread adds server's url if contains hash.
     * @param url The url for the server to queury
     * @param hash The BigHash for the chunk to lookup.
     * @param ChunkType
     */
    static class ServerLookupThread extends Thread {

        private List<String> hasUrls,  hasNotUrls;
        private String url;
        private BigHash hash;
        private ChunkType type;

        /**
         * 
         * @param hasUrls
         * @param hasNotUrls
         * @param url
         * @param hash
         * @param type
         */
        public ServerLookupThread(List<String> hasUrls, List<String> hasNotUrls, String url, BigHash hash, ChunkType type) {
            this.hasUrls = hasUrls;
            this.hasNotUrls = hasNotUrls;
            this.url = url;
            this.hash = hash;
            this.type = type;
        }

        @Override()
        public void run() {
            TrancheServer rserver = null;
            try {
                rserver = ConnectionUtil.connectURL(url, false);
                boolean isFound = false;
                if (type.equals(ChunkType.META) || type.equals(ChunkType.PROJECT)) {
                    if (IOUtil.hasMetaData(rserver, hash)) {
                        isFound = true;
                    }
                } else {
                    if (IOUtil.hasData(rserver, hash)) {
                        isFound = true;
                    }
                }

                if (isFound) {
                    synchronized (hasUrls) {
                        hasUrls.add(url);
                    }
                } else {
                    synchronized (hasNotUrls) {
                        hasNotUrls.add(url);
                    }
                }
                logServerSuccess(url);
            } catch (Exception ex) {
                System.err.println("Could not connect to " + url + ": " + ex.getMessage());

                // Log the exception
                logServerException(url, ex);
            } finally {
                IOUtil.safeClose(rserver);
            }
        }
    } // ServerLookupThread

    /**
     * <p>Recycles from GetFileTool. =)</p>
     * @param projectFileHash
     * @return
     * @throws java.lang.Exception
     */
    public static ProjectFile getProjectFile(BigHash projectFileHash) throws Exception {
        return getProjectFile(projectFileHash, null);
    }

    /**
     * <p>Recycles from GetFileTool. =)</p>
     * @param projectFileHash
     * @param passphrase
     * @return
     * @throws java.lang.Exception
     */
    public static ProjectFile getProjectFile(BigHash projectFileHash, String passphrase) throws Exception {
        File pf = null;

        // Parse the project file in memory
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ProjectFile projectFile = null;
        try {

            pf = TempFileUtil.createTemporaryFile(".pf");

            GetFileTool gft = new GetFileTool();
            gft.setHash(projectFileHash);
            gft.setValidate(false);
            if (passphrase != null && !passphrase.trim().equals("")) {
                gft.setPassphrase(passphrase);
            }
            gft.setSaveFile(pf);
            gft.getFile();

            // open the streams
            fis = new FileInputStream(pf);
            bis = new BufferedInputStream(fis);
            // set the project file
            projectFile = ProjectFileUtil.read(bis);

        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
            IOUtil.safeDelete(pf);
        }

        return projectFile;
    }

    /**
     * <p>Log server success. Helps automatically manager servers.</p>
     * @param url
     */
    private static void logServerSuccess(String url) {
        synchronized (successTokenCounts) {
            Long serverSuccessTokenCount = successTokenCounts.get(url);

            if (serverSuccessTokenCount != null) {
                serverSuccessTokenCount = Long.valueOf(serverSuccessTokenCount.longValue() + 1);
            } else {
                serverSuccessTokenCount = Long.valueOf(1);
            }
            successTokenCounts.put(url, serverSuccessTokenCount);

            // Note that a server may take a while to get an exception, so may
            // store up multiple redemptions (i.e., may be greater than REQUIRED_SUCCESS_TOKENS_FOR_FAILURE_TOKEN_REMOVAL)
            if (isAllowBanning && REQUIRED_SUCCESS_TOKENS_FOR_FAILURE_TOKEN_REMOVAL <= serverSuccessTokenCount) {

                synchronized (failureTokenCounts) {
                    Long serverFailureTokenCount = failureTokenCounts.get(url);

                    // Decrement count of exceptions if exists and greater than one
                    if (serverFailureTokenCount != null && serverFailureTokenCount.longValue() > 0) {

                        // Traded in n success tokens for decrement to exception count
                        // where n = successesBeforeDecrementFailure
                        final long prevServerExceptionTokenCount = serverFailureTokenCount.longValue();
                        serverFailureTokenCount = Long.valueOf(prevServerExceptionTokenCount - 1);
                        failureTokenCounts.put(url, serverFailureTokenCount);

                        printTracer("Because " + url + " had " + serverSuccessTokenCount + " success tokens, decremented failure tokens from " + prevServerExceptionTokenCount + " to " + serverFailureTokenCount);

                        // Take away enough success tokens for the trade-off, might be some remaining
                        successTokenCounts.put(url, Long.valueOf(serverSuccessTokenCount - REQUIRED_SUCCESS_TOKENS_FOR_FAILURE_TOKEN_REMOVAL));
                    }
                }
            }
        }
    }

    /**
     * <p>Log exceptions for server.</p>
     * @param url
     * @param ex
     */
    private static void logServerException(String url, Exception ex) {
        synchronized (failureTokenCounts) {
            Long serverFailureTokenCount = failureTokenCounts.get(url);

            if (serverFailureTokenCount != null) {
                serverFailureTokenCount = Long.valueOf(serverFailureTokenCount.longValue() + 1);
            } else {
                serverFailureTokenCount = Long.valueOf(1);
            }
            failureTokenCounts.put(url, serverFailureTokenCount);

            printTracer("Exception for " + url + ", failure tokens=" + serverFailureTokenCount + ", message=" + ex.getMessage());
        }
    }

    /**
     * <p>Print a tracer if debug flag is set.</p>
     * @param msg
     */
    private static void printTracer(String msg) {
        if (isDebug) {
            System.out.println("SCRIPTS_UTIL> " + msg);
        }
    }
}
