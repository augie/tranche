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
package org.tranche.tasks;

import org.tranche.util.*;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.tranche.Tertiary;
import org.tranche.TrancheServer;
import org.tranche.configuration.Configuration;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.exceptions.TodoException;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.meta.MetaData;
import org.tranche.network.*;
import org.tranche.security.SecurityUtil;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;

/**
 * <p>Utility methods to accomplish specific tasks on network.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class TaskUtil {

    /**
     * <p>This needs to be a global value, not a static class variable.</p>
     */
    private static final int REQUIRED_COPIES_FOR_DELETE = 2;

    /**
     * <p>Returns Configuration for a server with specified host.</p>
     * <p>Note that this will temporarily connect to a server if not already connected; however, if already connected to server, will simply use that connection.</p>
     * @param host
     * @return
     * @throws java.lang.Exception
     */
    public static final Configuration getConfiguration(String host) throws Exception {
        Configuration c = null;
        try {
            TrancheServer remoteServer = ConnectionUtil.connectHost(host, true);
            if (remoteServer != null) {
                try {
                    c = IOUtil.getConfiguration(remoteServer, SecurityUtil.getAnonymousCertificate(), SecurityUtil.getAnonymousKey());
                    if (c == null) {
                        throw new NullPointerException("Server<" + host + "> didn't return a configuration.");
                    }
                } finally {
                    ConnectionUtil.unlockConnection(host);
                }
            }
        } catch (Exception e) {
            ConnectionUtil.reportExceptionHost(host, e);
        }
        return c;
    }

    /**
     * 
     * @param hash
     * @param uploaderName
     * @param cert
     * @param key
     * @param uploadTimestamp
     * @param relativePathInDataSet
     * @throws java.lang.Exception
     */
    public static void deleteMetaData(BigHash hash, String uploaderName, X509Certificate cert, PrivateKey key, Long uploadTimestamp, String relativePathInDataSet) throws Exception {
        deleteMetaData(hash, uploaderName, cert, key, uploadTimestamp, relativePathInDataSet, System.out);
    }

    public static void deleteMetaData(BigHash hash, String uploaderName, X509Certificate cert, PrivateKey key, Long uploadTimestamp, String relativePathInDataSet, PrintStream out) throws Exception {

        GetFileTool gft = new GetFileTool();
        gft.setHash(hash);
        MetaData metaData = gft.getMetaData();

        /**
         * Deletions with many uploaders must be turned off for now. The activity 
         * logs do not currently account for which uploader within a meta data has
         * been deleted. Because of this, a server coming online and asking what has
         * occurred during its downtime will only see that the meta data was deleted.
         * This means that any many data deletions will result in the full meta 
         * data being deleted from the server instead of just an individual 
         * uploader within the meta data.
         *  Augie 11/17/2009
         */
        if (metaData.getUploaderCount() > 1) {
            throw new TodoException();
        }
        // uncomment when the above is fixed
//        if (!metaData.containsUploader(uploaderName, uploadTimestamp, relativePathInDataSet)) {
//            throw new Exception("Uploader does not exist.");
//        }
        // remove when the above is fixed
        uploaderName = null;
        uploadTimestamp = null;
        relativePathInDataSet = null;

        Set<String> offlineHosts = new HashSet();
        Set<String> hostsToDeleteFrom = new HashSet();
        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
            if (row.isCore() && !row.isOnline()) {
                offlineHosts.add(row.getHost());
            }
            if (row.isOnline()) {
                hostsToDeleteFrom.add(row.getHost());
            }
        }

        if (hostsToDeleteFrom.size() == 0) {
            throw new Exception("Could not delete since could not find any hosts to use.");
        }

        Collection<MultiServerRequestStrategy> strategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hostsToDeleteFrom, Tertiary.DONT_CARE, Tertiary.DONT_CARE);
        if (strategies.size() == 0) {
            throw new Exception("Could not find a strategy to propogate to " + hostsToDeleteFrom.size() + " server(s); cannot delete.");
        }

        out.println("Host(s) to use: " + hostsToDeleteFrom.size());
        for (String host : hostsToDeleteFrom) {
            out.println("   * " + host);
        }
        out.println();
        out.println("Offline host(s): " + offlineHosts.size());
        for (String host : offlineHosts) {
            out.println("   * " + host);
        }
        out.println();

        // Delete the meta data
        Set<String> deletedFrom = new HashSet<String>();
        STRATEGIES:
        for (MultiServerRequestStrategy strategy : strategies) {
            out.println("Executing next strategy:");
            out.println(strategy.toString());
            try {
                TrancheServer ts = ConnectionUtil.connectHost(strategy.getHostReceivingRequest(), true);
                if (ts == null) {
                    throw new Exception("Could not connect to " + strategy.getHostReceivingRequest());
                }
                try {
                    PropagationReturnWrapper prw = IOUtil.deleteMetaData(ts, cert, key, uploaderName, uploadTimestamp, relativePathInDataSet, hash, hostsToDeleteFrom.toArray(new String[0]));

                    Set<String> thisDeletedFrom = new HashSet<String>(hostsToDeleteFrom);
                    out.println("Total errors: " + prw.getErrors().size());
                    for (PropagationExceptionWrapper pew : prw.getErrors()) {
                        if (pew.host != null) {
                            thisDeletedFrom.remove(pew.host);
                        }
                        out.println("       * " + pew.exception.getClass().getSimpleName() + "<" + pew.host + ">: " + pew.exception.getMessage());
                        pew.exception.printStackTrace(System.err);
                    }
                    deletedFrom.addAll(thisDeletedFrom);
                    hostsToDeleteFrom.removeAll(thisDeletedFrom);
                    if (hostsToDeleteFrom.isEmpty()) {
                        break;
                    }
                } finally {
                    ConnectionUtil.unlockConnection(strategy.getHostReceivingRequest());
                }
            } catch (Exception e) {
                out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(out);
            }
            out.println();
        }

        out.println("Deleted the meta data from the following servers:");
        for (String host : deletedFrom) {
            out.println(" " + host);
        }
    }

    /**
     * 
     * @param hash
     * @param passphrase
     * @param uploaderName
     * @param cert
     * @param key
     * @param uploadTimestamp
     * @param relativePathInDataSet
     * @throws java.lang.Exception
     */
    public static void publishPassphrase(BigHash hash, String passphrase, String uploaderName, X509Certificate cert, PrivateKey key, long uploadTimestamp, String relativePathInDataSet) throws Exception {
        publishPassphrase(hash, passphrase, uploaderName, cert, key, uploadTimestamp, relativePathInDataSet, System.out);
    }

    /**
     * 
     * @param hash
     * @param passphrase
     * @param uploaderName
     * @param cert
     * @param key
     * @param uploadTimestamp
     * @param relativePathInDataSet
     * @param out
     * @throws java.lang.Exception
     */
    public static void publishPassphrase(BigHash hash, String passphrase, String uploaderName, X509Certificate cert, PrivateKey key, long uploadTimestamp, String relativePathInDataSet, PrintStream out) throws Exception {
        // Set to anything with proper hash span
        final Set<String> writableHostsToUse = new HashSet(),  writableHostsWithoutHashSpan = new HashSet(),  readOnlyHosts = new HashSet(),  offlineHosts = new HashSet();
        ROWS:
        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {

            final String host = row.getHost();
            boolean isInHashSpan = false;

            HASH_SPANS:
            for (HashSpan hs : row.getHashSpans()) {
                if (hs.contains(hash)) {
                    isInHashSpan = true;
                    break HASH_SPANS;
                }
            }

            if (!row.isOnline()) {
                offlineHosts.add(host);
            } else if (row.isWritable()) {
                if (isInHashSpan) {
                    writableHostsToUse.add(host);
                } else {
                    writableHostsWithoutHashSpan.add(host);
                }
            } else if (!row.isWritable()) {
                readOnlyHosts.add(host);
            } else {
                throw new RuntimeException("Assertion failed: unknown server status for " + row.getHost() + " <is online: " + row.isOnline() + ", is writable: " + row.isWritable() + ">");
            }
        }

        if (writableHostsToUse.size() == 0) {
            throw new Exception("Could not publish since could not find any hosts to use.");
        }

        Collection<MultiServerRequestStrategy> updateStrategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(writableHostsToUse, Tertiary.DONT_CARE, Tertiary.DONT_CARE);
        if (updateStrategies.size() == 0) {
            throw new Exception("Could not find a strategy to propogate to " + writableHostsToUse.size() + " server(s); cannot delete.");
        }

        out.println("Host(s) to use (writable, has appropriate hash span): " + writableHostsToUse.size());
        for (String host : writableHostsToUse) {
            out.println("   * " + host);
        }
        out.println();
        out.println("Other host(s): ");
        out.println("   - Writable (no appropriate hash span): " + writableHostsWithoutHashSpan.size());
        for (String host : writableHostsWithoutHashSpan) {
            out.println("   * " + host);
        }
        out.println();
        out.println("   - Read-only: " + readOnlyHosts.size());
        for (String host : readOnlyHosts) {
            out.println("   * " + host);
        }
        out.println();
        out.println("   - Offline: " + offlineHosts.size());
        for (String host : offlineHosts) {
            out.println("   * " + host);
        }
        out.println();

        // Assert: 
        if (!isDisjoint(writableHostsToUse, writableHostsWithoutHashSpan)) {
            throw new AssertionFailedException("Expecting two sets to be disjoint, but weren't");
        }
        // Assert: 
        if (!isDisjoint(writableHostsToUse, readOnlyHosts)) {
            throw new AssertionFailedException("Expecting two sets to be disjoint, but weren't");
        }

        GetFileTool gft = new GetFileTool();
        gft.setHash(hash);
        MetaData metaData = gft.getMetaData();

        // Select the appropriate uploader
        out.println("Number of uploaders in meta data: " + metaData.getUploaderCount());

        boolean found = selectUploaderInMetaData(metaData, uploaderName, relativePathInDataSet, uploadTimestamp);
        if (!found) {
            throw new Exception("Could not find user information in meta data, so cannot delete.");
        }

        metaData.setPublicPassphrase(passphrase);

        byte[] metaDataBytes = metaData.toByteArray();

        Set<String> publishedTo = new HashSet<String>();
        STRATEGIES:
        for (MultiServerRequestStrategy strategy : updateStrategies) {
            out.println("Starting next strategy:");
            out.println(strategy.toString());
            try {
                TrancheServer ts = null;

                try {
                    ts = ConnectionUtil.connectHost(strategy.getHostReceivingRequest(), true);
                    if (ts == null) {
                        throw new NullPointerException("Could not connect to " + strategy.getHostReceivingRequest());
                    }
                    PropagationReturnWrapper prw = IOUtil.setMetaData(ts, cert, key, false, hash, metaDataBytes, writableHostsToUse.toArray(new String[0]));
                    Set<String> thisPublishedTo = new HashSet<String>(writableHostsToUse);
                    out.println("Total errors: " + prw.getErrors().size());

                    // Print exception messages, and removes hosts
                    for (PropagationExceptionWrapper pew : prw.getErrors()) {
                        out.println("       * " + pew.exception.getClass().getSimpleName() + "<" + pew.host + ">: " + pew.exception.getMessage());
                        pew.exception.printStackTrace(System.err);

                        if (pew.host != null) {
                            thisPublishedTo.remove(pew.host);
                        }
                    }

                    publishedTo.addAll(thisPublishedTo);
                    writableHostsToUse.removeAll(thisPublishedTo);
                    if (writableHostsToUse.isEmpty()) {
                        break;
                    }
                } finally {
                    ConnectionUtil.unlockConnection(strategy.getHostReceivingRequest());
                }
            } catch (Exception e) {
                out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(out);
            }
            out.println();
        }

        out.println("Published passphrase to the following " + publishedTo.size() + " server(s):");
        for (String host : publishedTo) {
            out.println("   * " + host);
        }

        out.println();

        if (publishedTo.size() < REQUIRED_COPIES_FOR_DELETE) {
            throw new Exception("Failed to update minimum number of copies. Required: " + REQUIRED_COPIES_FOR_DELETE + ", Updated: " + publishedTo.size());
        }

        // Assert: all servers that received chunk are not in writable set any longer
        if (!isDisjoint(writableHostsToUse, publishedTo)) {
            throw new AssertionFailedException("Expecting two sets to be disjoint, but weren't");
        }

        out.println("Checking for meta data to delete from non-writable servers or servers without appropriate hash span.");

        // Delete chunks from all all servers that didn't get a copy (as long as online)

        // ------------------------------------------------------------------------
        // NOTE: Cannot rely on the status table to know which servers are writable, which aren't. It
        //       counted several read-only servers as writable. 
        // ------------------------------------------------------------------------
        Set<String> deleteFromHosts = new HashSet();
        deleteFromHosts.addAll(writableHostsWithoutHashSpan);
        deleteFromHosts.addAll(readOnlyHosts);
        deleteFromHosts.addAll(writableHostsToUse);

        final int MAX_ATTEMPTS = 3;

        // ------------------------------------------------------------------------
        // NOTE: Requires user can delete on server. If not, will get error messages.
        //
        // It is important to check whether chunk exists first. If not, we'll
        // get unnecessary security exceptions for servers regardless of whether
        // they have the chunk or not.
        //
        // If a user is trying to delete, cannot delete and yet the chunk exists,
        // we need an exception. Hence, we connect to each server in turn and
        // incur the overhead.
        // ------------------------------------------------------------------------
        for (String host : deleteFromHosts) {
            ATTEMPTS:
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                TrancheServer ts = null;

                try {
                    ts = ConnectionUtil.connectHost(host, true);
                    if (ts == null) {
                        throw new NullPointerException("Could not connect to " + host);
                    }

                    boolean deleted = false;
                    if (IOUtil.hasMetaData(ts, hash)) {
                        PropagationReturnWrapper prw = IOUtil.deleteMetaData(ts, cert, key, hash);
                        out.println("Attempting (#" + attempt + ") to delete from " + host);
                        deleted = true;

                        // If any problems, assume the worse and try again. If worked anyhow, we'll break
                        // out fine anyhow.
                        if (prw.isAnyErrors()) {
                            // Throw any exceptions found. Easier to handle this logic in one place.
                            for (PropagationExceptionWrapper pew : prw.getErrors()) {
                                throw pew.exception;
                            }
                        }

                        // If still has chunk, we have a problem
                        if (IOUtil.hasMetaData(ts, hash)) {
                            throw new Exception("Should have deleted from server, but still has meta data chunk.");
                        }
                    }

                    out.println("Finished checking for meta data to delete from " + host + " <attempt #" + attempt + ">. Deleted?: " + (deleted ? "yes" : "no"));
                    break ATTEMPTS;
                } catch (Exception e) {
                    out.println(e.getClass().getSimpleName() + " occured while attempting (#" + attempt + " of " + MAX_ATTEMPTS + ") to find meta data to delete from " + host + ": " + e.getMessage());
                    e.printStackTrace(out);

                    // Don't waste time -- the user cannot delete, so throw exception immediately
                    if (e instanceof GeneralSecurityException) {
                        throw e;
                    }

                    // Otherwise, only throw exception if tried enough times
                    if (attempt == MAX_ATTEMPTS) {
                        throw e;
                    }
                } finally {
                    ConnectionUtil.unlockConnection(host);
                }
            } // delete from host
        } // attempts (to delete)
    }

    /**
     * <p>Determines
     * @param setA
     * @param setB
     * @return
     */
    private static boolean isDisjoint(Set setA, Set setB) {

        for (Object objA : setA) {
            if (setB.contains(objA)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 
     * @param metaData
     * @param uploaderName
     * @param relativePathInDataSet
     * @param uploadTimestamp
     * @return
     */
    private static boolean selectUploaderInMetaData(MetaData metaData, String uploaderName, String relativePathInDataSet, long uploadTimestamp) {
        for (int i = 0; i < metaData.getUploaderCount(); i++) {
            metaData.selectUploader(i);
            String nextUniqueName = metaData.getSignature().getUserName();
            long nextTimestamp = metaData.getTimestampUploaded();
            String nextRelativeNameInDataSet = metaData.getRelativePathInDataSet();

            boolean isSameName = nextUniqueName.trim().equals(uploaderName.trim());
            boolean isSameRelativePath = (nextRelativeNameInDataSet == null && relativePathInDataSet == null) || (nextRelativeNameInDataSet != null && relativePathInDataSet != null && nextRelativeNameInDataSet.equals(relativePathInDataSet));
            boolean isSameTimestamp = nextTimestamp == uploadTimestamp;

            if (isSameName && isSameRelativePath && isSameTimestamp) {
                return true;
            }
        }

        return false;
    }
}