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
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.tranche.Tertiary;
import org.tranche.TrancheServer;
import org.tranche.configuration.Configuration;
import org.tranche.exceptions.TodoException;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataUtil;
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
        Set<String> hostsToPublishTo = new HashSet();
        Set<String> offlineHosts = new HashSet();
        ROWS:
        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
            if (row.isOnline() && row.isWritable()) {
                HASH_SPANS:
                for (HashSpan hs : row.getHashSpans()) {
                    if (hs.contains(hash)) {
                        hostsToPublishTo.add(row.getHost());
                        break HASH_SPANS;
                    }
                }
            } else {
                offlineHosts.add(row.getHost());
            }
        }

        if (hostsToPublishTo.size() == 0) {
            throw new Exception("Could not publish since could not find any hosts to use.");
        }

        Collection<MultiServerRequestStrategy> strategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hostsToPublishTo, Tertiary.DONT_CARE, Tertiary.DONT_CARE);
        if (strategies.size() == 0) {
            throw new Exception("Could not find a strategy to propogate to " + hostsToPublishTo.size() + " server(s); cannot delete.");
        }

        out.println("Host(s) to use: " + hostsToPublishTo.size());
        for (String host : hostsToPublishTo) {
            out.println("   * " + host);
        }
        out.println();
        out.println("Offline host(s): " + offlineHosts.size());
        for (String host : offlineHosts) {
            out.println("   * " + host);
        }
        out.println();

        GetFileTool gft = new GetFileTool();
        gft.setHash(hash);
        MetaData metaData = gft.getMetaData();

        // Select the appropriate uploader
        out.println("Number of uploaders in meta data: " + metaData.getUploaderCount());
        boolean found = false;
        for (int i = 0; i < metaData.getUploaderCount(); i++) {
            metaData.selectUploader(i);
            String nextUniqueName = metaData.getSignature().getUserName();
            long nextTimestamp = metaData.getTimestampUploaded();
            String nextRelativeNameInDataSet = metaData.getRelativePathInDataSet();
            out.println(" " + nextUniqueName + ", " + nextTimestamp + ", " + nextRelativeNameInDataSet);

            boolean isSameName = nextUniqueName.trim().equals(uploaderName.trim());
            boolean isSameRelativePath = (nextRelativeNameInDataSet == null && relativePathInDataSet == null) || (nextRelativeNameInDataSet != null && relativePathInDataSet != null && nextRelativeNameInDataSet.equals(relativePathInDataSet));
            boolean isSameTimestamp = nextTimestamp == uploadTimestamp;

            if (isSameName && isSameRelativePath && isSameTimestamp) {
                found = true;
                break;
            }
        }
        out.println();

        if (!found) {
            throw new Exception("Could not find user information in meta data, so cannot delete.");
        }

        metaData.setPublicPassphrase(passphrase);

        ByteArrayOutputStream baos = null;
        byte[] metaDataBytes = null;
        try {
            baos = new ByteArrayOutputStream();
            MetaDataUtil.write(metaData, baos);
            metaDataBytes = baos.toByteArray();
        } finally {
            IOUtil.safeClose(baos);
        }

        Set<String> publishedTo = new HashSet<String>();
        STRATEGIES:
        for (MultiServerRequestStrategy strategy : strategies) {
            out.println("Starting next strategy:");
            out.println(strategy.toString());
            try {
                TrancheServer ts = ConnectionUtil.connectHost(strategy.getHostReceivingRequest(), true);
                if (ts == null) {
                    throw new NullPointerException("Could not connect to " + strategy.getHostReceivingRequest());
                }
                try {
                    PropagationReturnWrapper prw = IOUtil.setMetaData(ts, cert, key, false, hash, metaDataBytes, hostsToPublishTo.toArray(new String[0]));
                    Set<String> thisPublishedTo = new HashSet<String>(hostsToPublishTo);
                    out.println("Total errors: " + prw.getErrors().size());
                    for (PropagationExceptionWrapper pew : prw.getErrors()) {
                        if (pew.host != null) {
                            thisPublishedTo.remove(pew.host);
                        }
                        out.println("       * " + pew.exception.getClass().getSimpleName() + "<" + pew.host + ">: " + pew.exception.getMessage());
                        pew.exception.printStackTrace(System.err);
                    }
                    publishedTo.addAll(thisPublishedTo);
                    hostsToPublishTo.removeAll(thisPublishedTo);
                    if (hostsToPublishTo.isEmpty()) {
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

        out.println("Published passphrase to the following servers:");
        for (String host : publishedTo) {
            out.println(" " + host);
        }
    }
}