/*
 *    Copyright 2005 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tranche.routing;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tranche.commons.Tertiary;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.configuration.ConfigurationUtil;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.exceptions.NoHostProvidedException;
import org.tranche.exceptions.NoMatchingServersException;
import org.tranche.exceptions.PropagationFailedException;
import org.tranche.exceptions.UnsupportedServerOperationException;
import org.tranche.flatfile.NonceMap;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.logs.activity.Activity;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.MultiServerRequestStrategy;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTable;
import org.tranche.network.StatusTableRow;
import org.tranche.remote.Token;
import org.tranche.security.SecurityUtil;
import org.tranche.security.Signature;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.servers.ServerUtil;
import org.tranche.time.TimeUtil;
import org.tranche.users.User;
import org.tranche.util.AssertionUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.PersistentServerFileUtil;
import org.tranche.util.TestUtil;

/**
 * <p>Used to connect to one or more Tranche Servers ("data servers"). Redirects requests to appropriate servers.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class RoutingTrancheServer extends TrancheServer {

    private Configuration config;
    private NonceMap nonces;
    private Set<String> dataServerHosts;
    private List<RoutingTrancheServerListener> listeners;
    private final Set<String> connectedHosts;
    private X509Certificate authCert;
    private PrivateKey authPrivateKey;
    private boolean closed;

    public RoutingTrancheServer(X509Certificate cert, PrivateKey key) {
        try {
            loadConfiguration();
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + " occurred while loading configuration: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }

        nonces = new NonceMap(this);
        dataServerHosts = new HashSet();
        listeners = new LinkedList();
        connectedHosts = new HashSet();

        this.closed = false;

        this.authCert = cert;
        this.authPrivateKey = key;

        boolean authMissing = this.authCert == null || this.authPrivateKey == null;

        if (authMissing && !TestUtil.isTesting()) {
            String missingDesc = null;
            if (this.authCert == null && this.authPrivateKey == null) {
                missingDesc = "certificate and private key";
            } else if (this.authCert == null && this.authPrivateKey != null) {
                missingDesc = "certificate";
            } else {
                missingDesc = "private key";
            }

            System.err.println("~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~");
            System.err.println("WARNING: ");
            System.err.println("  The routing server is running without a trusted " + missingDesc + " set.");
            System.err.println();
            System.err.println("  Though the server can still run, it will not be able to perform deletes to the data");
            System.err.println("  servers it manages.");
            System.err.println();
            System.err.println("  If the " + missingDesc + " will be set using the API or command-line tools after ");
            System.err.println("  message is printed, then ignore this warning.");
            System.err.println("~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~");
        }
    }

    public RoutingTrancheServer() {
        this(null, null);
    }

    private void loadConfiguration() throws Exception {
        File configFile = new File(PersistentServerFileUtil.getPersistentDirectory(), "configuration");
        if (configFile.exists()) {
            // get the config bytes
            byte[] configBytes = IOUtil.getBytes(configFile);
            config = ConfigurationUtil.read(new ByteArrayInputStream(configBytes));
        } else {
            // if none exist, make a new one
            config = new Configuration();
            // add the default users
            config.addUser(SecurityUtil.getAdmin());
            config.addUser(SecurityUtil.getUser());
            config.addUser(SecurityUtil.getReadOnly());
            config.addUser(SecurityUtil.getWriteOnly());
            config.addUser(SecurityUtil.getAutoCert());

            configFile.getParentFile().mkdirs();
        }
    }

    /**
     * Sets a new configuration for the server. Must have appropriate permission to do so.
     *
     * @param   data        The configuration saved as an array of bytes. Use ConfigurationUtil to generate
     *                      the appropriate bytes.
     * @param   sig         A signature for the data.
     * @param   nonce       The nonce used from the server.
     * @throws  Exception   If invalid configuration format exceptions or security exceptions occur.
     *
     */
    public void setConfiguration(byte[] data, Signature sig, byte[] nonce) throws Exception {

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(sig);
        AssertionUtil.assertNoNullValues(data);
        AssertionUtil.assertNoNullValues(nonce);

        // Verify nonce exists and is valid
        boolean nonceValid = verifyNonce(nonce);
        if (!nonceValid) {
            throw new GeneralSecurityException(BAD_NONCE);
        }

        // param check
        if (sig == null) {
            throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid signature.");
        }
        if (nonce == null) {
            throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid nonce.");
        }
        if (data == null) {
            throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid nonce.");
        }

        // get the certificate
        User user = config.getRecognizedUser(sig.getCert());
        // if certificate can't be loaded, throw an exception
        if (user == null) {
            throw new GeneralSecurityException(Token.SECURITY_ERROR + " User is not recognized by this server.");
        }
        // check permissions
        if (!user.canSetConfiguration()) {
            throw new GeneralSecurityException(Token.SECURITY_ERROR + " User can not set the server's configuration.");
        }

        // Check that the certificate is valid
        boolean certIsValid = verifyCertificate(sig.getCert());
        if (!certIsValid) {
            throw new GeneralSecurityException("User certificate not valid after " + user.getCertificate().getNotAfter().toString());
        }

        // make the array of bytes to check
        byte[] bytes = new byte[data.length + nonce.length];
        System.arraycopy(data, 0, bytes, 0, data.length);
        System.arraycopy(nonce, 0, bytes, data.length, nonce.length);

        // redo the signature to verify it
        boolean nonceVerify = verifySignature(bytes, sig.getBytes(), sig.getCert().getPublicKey(), sig.getAlgorithm());
        if (!nonceVerify) {
            throw new GeneralSecurityException(BAD_SIG);
        }

        // Verified, perform action
        setConfiguration(ConfigurationUtil.read(new ByteArrayInputStream(data)));
    }

    public void setConfiguration(Configuration newConfig) throws Exception {

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(newConfig);

        synchronized (config) {
            // --------------------------------------------------------------------------------------------
            //  Use existing hash spans. A user cannot be allowed to change these because automatically calculated.
            // --------------------------------------------------------------------------------------------
            Set<HashSpan> correctHashSpans = this.config.getHashSpans();
            newConfig.getHashSpans().clear();
            newConfig.getHashSpans().addAll(correctHashSpans);

            // --------------------------------------------------------------------------------------------
            //  Clear out DDCs. They don't server any purpose, and are just clutter.
            // --------------------------------------------------------------------------------------------
            newConfig.getDataDirectories().clear();

            // Update reference
            this.config = newConfig;

            // Save to disk
            saveConfiguration();

            // Check for updates
            checkForDataServersUpdates();
        }
    }

    /**
     * <p>Check if needs to update data servers to which this server instance is routing.</p>
     * @throws java.lang.Exception
     */
    private void checkForDataServersUpdates() throws Exception {
        synchronized (this.config) {
            synchronized (this.dataServerHosts) {

                // Need to read in the new data servers
                Set<String> newDataServers = new HashSet();
                String newDataServersStr = this.config.getValue(ConfigKeys.ROUTING_DATA_SERVERS_HOST_LIST);
                if (newDataServersStr != null) {
                    for (String host : newDataServersStr.split(",")) {
                        newDataServers.add(host.trim());
                    }
                }

                Set<String> hostsToAdd = new HashSet();
                Set<String> hostsToRemove = new HashSet();

                // First, check if each old data servers in new list.
                // If not, then it will be removed.
                for (String host : this.dataServerHosts) {
                    if (!newDataServers.contains(host)) {
                        hostsToRemove.add(host);
                    }
                }

                // Next, check if each new data server in old list.
                // If not, then it will be added.
                for (String host : newDataServers) {
                    if (!this.dataServerHosts.contains(host)) {
                        hostsToAdd.add(host);
                    }
                }

                // Remove appropriate servers.
                this.dataServerHosts.removeAll(hostsToRemove);
                this.dataServerHosts.addAll(hostsToAdd);

                // Fire events
                fireDataServersAdded(hostsToAdd.toArray(new String[0]));
                fireDataServersRemoved(hostsToRemove.toArray(new String[0]));
            }
        }
    }

    /**
     * <p>Serialize the Configurtation data and save to disk.</p>
     *
     */
    public void saveConfiguration() {
        try {
            synchronized (this.config) {
                // save the configuration
                File backupConfig = new File(PersistentServerFileUtil.getPersistentDirectory(), "configuration.backup");
                // delete any old backups
                backupConfig.delete();
                // copy the current to the backup
                File configFile = new File(PersistentServerFileUtil.getPersistentDirectory(), "configuration");
                configFile.renameTo(backupConfig);

                // write out the new configuration
                FileOutputStream fos = null;
                BufferedOutputStream bos = null;
                try {
                    fos = new FileOutputStream(configFile);
                    bos = new BufferedOutputStream(fos);
                    ConfigurationUtil.write(config, bos);
                } catch (Exception e) {
                    // safely close
                    IOUtil.safeClose(bos);
                    IOUtil.safeClose(fos);
                    // restore the backup
                    configFile.delete();
                    backupConfig.renameTo(configFile);
                } finally {
                    IOUtil.safeClose(bos);
                    IOUtil.safeClose(fos);
                }
            }
        } finally {
        }
    }

    /**
     * @param   sig         the signature
     * @param   nonce       the nonce bytes
     * @return              the configuration
     * @throws  Exception   if any exception occurs
     *
     */
    public Configuration getConfiguration(Signature sig, byte[] nonce) throws Exception {

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(sig);
        AssertionUtil.assertNoNullValues(nonce);

        // Verify nonce exists and is valid
        boolean nonceValid = verifyNonce(nonce);
        if (!nonceValid) {
            throw new GeneralSecurityException(BAD_NONCE);
        }

        // param check
        if (sig == null) {
            throw new Exception("Must pass in a valid signature.");
        }
        if (nonce == null) {
            throw new Exception("Must pass in a valid nonce.");
        }

        if (sig.getCert() == null) {
            throw new GeneralSecurityException("Signature's certificate is null.");
        }
        // get the certificate
        User user = config.getRecognizedUser(sig.getCert());
        // if certificate can't be loaded, throw an exception
        if (user == null) {
            throw new GeneralSecurityException("User is not recognized by this server.");
        }
        // check permissions
        if (!user.canGetConfiguration()) {
            throw new GeneralSecurityException("User can not read the server's configuration.");
        }

        // Check that the certificate is valid
        boolean certIsValid = verifyCertificate(sig.getCert());
        if (!certIsValid) {
            throw new GeneralSecurityException("User certificate not valid after " + user.getCertificate().getNotAfter().toString());
        }

        // redo the signature to verify it
        boolean nonceVerify = verifySignature(nonce, sig.getBytes(), sig.getCert().getPublicKey(), sig.getAlgorithm());
        if (!nonceVerify) {
            throw new GeneralSecurityException(BAD_SIG);
        }

        // send back the configuration
        return getConfiguration();
    }

    public Configuration getConfiguration() throws Exception {
        return this.config;
    }

    /**
     * <p>Ping the server. Handled by Server object which wraps this object.</p>
     * @throws java.lang.Exception
     */
    public void ping() throws Exception {
    }

    /**
     * <p>Returns the rows in the local network status that have host names that are between the startHost (inclusive) and the endHost (exclusive.)</p>
     * @param startHost 
     * @param endHost 
     * @return
     * @throws java.lang.Exception
     */
    public StatusTable getNetworkStatusPortion(String startHost, String endHost) throws Exception {
        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(startHost);
        AssertionUtil.assertNoNullValues(endHost);
        return NetworkUtil.getStatus().getStatus(startHost, endHost);
    }

    /**
     * <p>Just returns a deep copy of servers to use. Synchronized.</p>
     * @return
     */
    public Set<String> getManagedServers() {
        Set<String> deepCopyOfServers = new HashSet();
        synchronized (this.dataServerHosts) {
            for (String host : this.dataServerHosts) {
                deepCopyOfServers.add(host);
            }
        }
        return deepCopyOfServers;
    }

    /**
     * <p>Returns true if server host is among managed host list; false otherwise.</p>
     * @param host
     * @return
     */
    public boolean isManagedServer(String host) {
        return getManagedServers().contains(host);
    }

    /**
     * <p>Returns data servers with hash span that contains hash, or returns null.</p>
     * <ul>
     *   <li>Must be currently connected</li>
     *   <li>Must be core server</li>
     *   <li>Must be online</li>
     * </ul>
     * @param hash
     * @return
     */
    private Set<String> getManagedServersThatContain(BigHash hash) throws Exception {

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hash);

        Set<String> deepCopyOfServers = getManagedServers();
        Set<String> serversThatContain = new HashSet();

        // Check all servers to see if hash span covers chunk
        HOSTS:
        for (String host : deepCopyOfServers) {
            StatusTableRow row = NetworkUtil.getStatus().getRow(host);
            for (HashSpan hs : row.getHashSpans()) {
                if (hs.contains(hash)) {
                    serversThatContain.add(host);
                    continue HOSTS;
                }
            }
        }

        return serversThatContain;
    }

    /**
     * <p>Lazily connects to data servers.</p>
     * @param host
     * @return
     */
    private TrancheServer getConnectionForDataServer(String host) throws Exception {

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(host);

        synchronized (this.connectedHosts) {
            TrancheServer ts = ConnectionUtil.getHost(host);

            // Lazily connect if not connected.
            if (ts == null) {

                // Quick assertion to find bugs
                if (this.connectedHosts.contains(host)) {
                    throw new AssertionFailedException("Set of connected hosts contains host that is not connected: " + host);
                }

                ts = ConnectionUtil.connectHost(host, true);
                this.connectedHosts.add(host);
            }

            // If connected but not locked, do so. This will prevent a connection from
            // being shut down.
            if (ts != null && !this.connectedHosts.contains(host)) {
                ConnectionUtil.lockConnection(host);
                this.connectedHosts.add(host);
            }

            return ts;
        }
    }

    /**
     * @param   hashes      the BigHash array of data chunks
     * @return              boolean array
     * @throws  Exception   if any exception occurs
     *
     */
    public boolean[] hasData(BigHash[] hashes) throws Exception {

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hashes);

        // Create a set of hashes that need to be retrieved. Make
        // a copy since we are going to modify it.
        BigHash[] hashesCopy = new BigHash[hashes.length];
        for (int i = 0; i < hashes.length; i++) {
            hashesCopy[i] = hashes[i];
        }

        // Keep an array ready to store boolean values when found
        boolean[] has = new boolean[hashes.length];
        for (int i = 0; i < has.length; i++) {
            has[i] = false;
        }

        Set<String> deepCopyOfServers = getManagedServers();


        // Check all servers to see if hash span covers chunk
        SERVERS:
        for (String host : deepCopyOfServers) {

            // Break out of loop when no more chunks to find
            boolean workRemaining = false;
            for (BigHash hash : hashesCopy) {
                if (hash != null) {
                    workRemaining = true;
                    break;
                }
            }
            if (!workRemaining) {
                break SERVERS;
            }

            TrancheServer ts = null;
            try {
                ts = getConnectionForDataServer(host);

                // Keep a map of chunks mapped to their index in the hash array
                Map<BigHash, Integer> chunkToIndexMap = new HashMap();

                HASHES_IN_HASH_SPANS:
                for (int index = 0; index < hashesCopy.length; index++) {

                    // Get the next hash to find
                    BigHash hash = hashesCopy[index];

                    // If null, means we've already handled
                    if (hash == null) {
                        continue HASHES_IN_HASH_SPANS;
                    }

                    boolean contains = false;
                    HASH_SPANS:
                    for (HashSpan hs : NetworkUtil.getStatus().getRow(host).getHashSpans()) {
                        if (hs.contains(hash)) {
                            contains = true;
                            break HASH_SPANS;
                        }
                    }

                    if (contains) {
                        chunkToIndexMap.put(hash, index);
                    }
                }

                // If we found any chunks this server should have
                if (chunkToIndexMap.size() > 0) {

                    // Get the chunks
                    BigHash[] batchOfHashes = chunkToIndexMap.keySet().toArray(new BigHash[0]);
                    boolean[] batchOfBoolean = ts.hasData(batchOfHashes);

                    if (batchOfBoolean.length < batchOfHashes.length) {
                        throw new AssertionFailedException("batchOfBoolean.length{" + batchOfBoolean.length + "} < batchOfHashes.length{" + batchOfHashes.length + "}");
                    }

                    // For each retrieved chunk, add to array of chunks. Then flag
                    // the chunk as already retrieved
                    for (int j = 0; j < batchOfBoolean.length; j++) {
                        BigHash hash = batchOfHashes[j];
                        boolean hasChunk = batchOfBoolean[j];
                        int index = chunkToIndexMap.get(hash);

                        if (hasChunk) {
                            // We found it!
                            has[index] = true;

                            // Flag the chunk as found
                            hashesCopy[index] = null;
                        }
                    }
                }
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
            } finally {
            }
        }

        return has;
    }

    /**
     * @param   hashes      the BigHash array of meta data chunks
     * @return              boolean array
     * @throws  Exception   if any exception occurs
     *
     */
    public boolean[] hasMetaData(BigHash[] hashes) throws Exception {

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hashes);

        // Create a set of hashes that need to be retrieved. Make
        // a copy since we are going to modify it.
        BigHash[] hashesCopy = new BigHash[hashes.length];
        for (int i = 0; i < hashes.length; i++) {
            hashesCopy[i] = hashes[i];
        }

        // Keep an array ready to store boolean values when found
        boolean[] has = new boolean[hashes.length];
        for (int i = 0; i < has.length; i++) {
            has[i] = false;
        }

        Set<String> deepCopyOfServers = getManagedServers();


        // Check all servers to see if hash span covers chunk
        SERVERS:
        for (String host : deepCopyOfServers) {

            // Break out of loop when no more chunks to find
            boolean workRemaining = false;
            for (BigHash hash : hashesCopy) {
                if (hash != null) {
                    workRemaining = true;
                    break;
                }
            }
            if (!workRemaining) {
                break SERVERS;
            }

            TrancheServer ts = null;
            try {
                ts = getConnectionForDataServer(host);

                // Keep a map of chunks mapped to their index in the hash array
                Map<BigHash, Integer> chunkToIndexMap = new HashMap();

                HASHES_IN_HASH_SPANS:
                for (int index = 0; index < hashesCopy.length; index++) {

                    // Get the next hash to find
                    BigHash hash = hashesCopy[index];

                    // If null, means we've already handled
                    if (hash == null) {
                        continue HASHES_IN_HASH_SPANS;
                    }

                    boolean contains = false;
                    HASH_SPANS:
                    for (HashSpan hs : NetworkUtil.getStatus().getRow(host).getHashSpans()) {
                        if (hs.contains(hash)) {
                            contains = true;
                            break HASH_SPANS;
                        }
                    }

                    if (contains) {
                        chunkToIndexMap.put(hash, index);
                    }
                }

                // If we found any chunks this server should have
                if (chunkToIndexMap.size() > 0) {

                    // Get the chunks
                    BigHash[] batchOfHashes = chunkToIndexMap.keySet().toArray(new BigHash[0]);
                    boolean[] batchOfBoolean = ts.hasMetaData(batchOfHashes);

                    if (batchOfBoolean.length < batchOfHashes.length) {
                        throw new AssertionFailedException("batchOfBoolean.length{" + batchOfBoolean.length + "} < batchOfHashes.length{" + batchOfHashes.length + "}");
                    }

                    // For each retrieved chunk, add to array of chunks. Then flag
                    // the chunk as already retrieved
                    for (int j = 0; j < batchOfBoolean.length; j++) {
                        BigHash hash = batchOfHashes[j];
                        boolean hasChunk = batchOfBoolean[j];
                        int index = chunkToIndexMap.get(hash);

                        if (hasChunk) {
                            // We found it!
                            has[index] = true;

                            // Flag the chunk as found
                            hashesCopy[index] = null;
                        }
                    }
                }
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
            } finally {
            }
        }

        return has;
    }

    /**
     * 
     * @param hashes
     * @param propagateRequest
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper getMetaData(BigHash[] hashes, boolean propagateRequest) throws Exception {
        return getChunk(hashes, propagateRequest, true);
    }

    /**
     *
     * @param hashes
     * @param propagateRequest
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper getData(BigHash[] hashes, boolean propagateRequest) throws Exception {
        return getChunk(hashes, propagateRequest, false);
    }

    /**
     * 
     * @param hashes
     * @param propagateRequest
     * @param isMetaData
     * @return
     * @throws java.lang.Exception
     */
    private PropagationReturnWrapper getChunk(BigHash[] hashes, boolean propagateRequest, boolean isMetaData) throws Exception {

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hashes);

        // Create a set of hashes that need to be retrieved. Make
        // a copy since we are going to modify it.
        BigHash[] hashesCopy = new BigHash[hashes.length];
        for (int i = 0; i < hashes.length; i++) {
            hashesCopy[i] = hashes[i];
        }

        // We will store all chunks here
        byte[][] chunks = new byte[hashes.length][];

        Set<String> deepCopyOfServers = getManagedServers();

        // Store all exceptions to return to client
        Set<PropagationExceptionWrapper> exceptions = new HashSet();

        // Check all servers to see if hash span covers chunk
        SERVERS:
        for (String host : deepCopyOfServers) {

            // Break out of loop when no more chunks to retrieve
            boolean workRemaining = false;
            for (BigHash hash : hashesCopy) {
                if (hash != null) {
                    workRemaining = true;
                    break;
                }
            }
            if (!workRemaining) {
                break SERVERS;
            }

            TrancheServer ts = null;
            try {
                ts = getConnectionForDataServer(host);

                // Keep a map of chunks mapped to their index in the hash array
                Map<BigHash, Integer> chunkToIndexMap = new HashMap();

                HASHES_IN_HASH_SPANS:
                for (int index = 0; index < hashesCopy.length; index++) {

                    // Get the next hash to find
                    BigHash hash = hashesCopy[index];

                    // If null, means we've already handled
                    if (hash == null) {
                        continue HASHES_IN_HASH_SPANS;
                    }

                    boolean contains = false;
                    HASH_SPANS:
                    for (HashSpan hs : NetworkUtil.getStatus().getRow(host).getHashSpans()) {
                        if (hs.contains(hash)) {
                            contains = true;
                            break HASH_SPANS;
                        }
                    }

                    if (contains) {
                        chunkToIndexMap.put(hash, index);
                    }
                }

                // If we found any chunks this server should have
                if (chunkToIndexMap.size() > 0) {

                    // Get the chunks
                    BigHash[] batchOfHashes = chunkToIndexMap.keySet().toArray(new BigHash[0]);
                    PropagationReturnWrapper wrapper = null;

                    if (isMetaData) {
                        wrapper = ts.getMetaData(batchOfHashes, propagateRequest);
                    } else {
                        wrapper = ts.getData(batchOfHashes, propagateRequest);
                    }

                    byte[][] batchOfChunks = (byte[][]) wrapper.getReturnValueObject();

                    // Copy over all problems. Note that there might be more than more than one exception per
                    // unavailable chunk.
                    exceptions.addAll(wrapper.getErrors());

                    if (batchOfChunks.length < batchOfHashes.length) {
                        throw new AssertionFailedException("batchOfChunks.length{" + batchOfChunks.length + "} < batchOfHashes.length{" + batchOfHashes.length + "}");
                    }

                    // For each retrieved chunk, add to array of chunks. Then flag
                    // the chunk as already retrieved
                    for (int j = 0; j < batchOfChunks.length; j++) {
                        BigHash hash = batchOfHashes[j];
                        byte[] chunk = batchOfChunks[j];

                        // Note: might fail. Skip if chunk wasn't found.
                        if (chunk == null) {
                            continue;
                        }

                        int index = chunkToIndexMap.get(hash);

                        // Add next chunk to array
                        chunks[index] = chunk;

                        // Flag the chunk as retrieved
                        hashesCopy[index] = null;
                    }
                }
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                exceptions.add(new PropagationExceptionWrapper(e, host));
            } finally {
            }
        }

        // Check if there are any chunks with matching hash spans
        for (BigHash h : hashesCopy) {
            // If hash is not null, means that it wasn't found
            if (h != null) {
                boolean contains = false;
                SERVERS:
                for (String host : deepCopyOfServers) {
                    HASH_SPANS:
                    for (HashSpan hs : NetworkUtil.getStatus().getRow(host).getHashSpans()) {
                        if (hs.contains(h)) {
                            contains = true;
                            break SERVERS;
                        }
                    }
                }
                if (!contains) {
                    exceptions.add(new PropagationExceptionWrapper(new NoMatchingServersException(), getHost()));
                }
            }
        }

        return new PropagationReturnWrapper(exceptions, chunks);
    }
    String cachedHost = null;

    public String getHost() {

        // Don't waste time synchronizing if cached
        if (cachedHost != null) {
            return cachedHost;
        }

        synchronized (this.config) {
            if (cachedHost == null) {

                // First, check whether set in configuration
                try {
                    cachedHost = IOUtil.parseHost(this.config.getValue(ConfigKeys.URL));
                } catch (Exception nope) { /* Just get the host name from interfaces */ }

                if (cachedHost == null) {
                    cachedHost = ServerUtil.getHostName();
                }
            }
        }
        return cachedHost;
    }

    /**
     * @param   offset      the offset
     * @param   length      the length
     * @return              an array of data hashes
     * @throws  Exception   if any exception occurs
     *
     */
    public BigHash[] getDataHashes(BigInteger offset, BigInteger length) throws Exception {
        throw new UnsupportedServerOperationException();
    }

    /**
     * @param   offset      the offset
     * @param   length      the length
     * @return              an array of meta data hashes
     * @throws  Exception   if any exception occurs
     *
     */
    public BigHash[] getMetaDataHashes(BigInteger offset, BigInteger length) throws Exception {
        throw new UnsupportedServerOperationException();
    }

    /**
     * Registers the given URL as a known server. The server chooses if or if not it will remember this URL.
     *
     * @param   url         the URL to register (e.g. tranche://proteomecommons.org:443 )
     * @throws  Exception   if any exception occurs
     *
     */
    public void registerServer(String url) throws Exception {
        // implemented by Server object
    }

    /**
     * Returns a list of all projects that this server has.
     *
     * @param   offset      the offset
     * @param   length      the length
     * @return              an array of project hashes
     * @throws  Exception   if any exception occurs
     *
     */
    public BigHash[] getProjectHashes(BigInteger offset, BigInteger length) throws Exception {
        throw new UnsupportedServerOperationException();
    }

    /**
     * Closes the server and shuts down any associated resources.
     *
     *
     */
    public void close() {

        // Remove the lock for all connected data servers. This will only
        // happen if lock was set.
        synchronized (this.connectedHosts) {
            for (String connectedDataServer : this.connectedHosts) {
                ConnectionUtil.unlockConnection(connectedDataServer);
            }

            this.connectedHosts.clear();
        }

        closed = true;
    }

    /**
     * <p>Request that a server shut down. Intended for remote administration of servers.</p>
     * @param sig
     * @param nonce
     * @throws java.lang.Exception
     */
    public void requestShutdown(Signature sig, byte[] nonce) throws Exception {

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(sig);
        AssertionUtil.assertNoNullValues(nonce);

        // Verify nonce exists and is valid
        boolean nonceValid = verifyNonce(nonce);
        if (!nonceValid) {
            throw new GeneralSecurityException(BAD_NONCE);
        }

        // param check
        if (sig == null) {
            throw new Exception("Must pass in a valid signature.");
        }
        if (nonce == null) {
            throw new Exception("Must pass in a valid nonce.");
        }

        if (sig.getCert() == null) {
            throw new GeneralSecurityException("Signature's certificate is null.");
        }
        // get the certificate
        User user = config.getRecognizedUser(sig.getCert());
        // if certificate can't be loaded, throw an exception
        if (user == null) {
            throw new GeneralSecurityException("User is not recognized by this server.");
        }
        // If can set configurtaion, can shut down
        if (!user.canSetConfiguration()) {
            throw new GeneralSecurityException("User can not set the server's configuration.");
        }

        // Check that the certificate is valid
        boolean certIsValid = verifyCertificate(sig.getCert());
        if (!certIsValid) {
            throw new GeneralSecurityException("User certificate not valid after " + user.getCertificate().getNotAfter().toString());
        }

        // redo the signature to verify it
        boolean nonceVerify = verifySignature(nonce, sig.getBytes(), sig.getCert().getPublicKey(), sig.getAlgorithm());
        if (!nonceVerify) {
            throw new GeneralSecurityException(BAD_SIG);
        }

        // Close the server. This is a really weird of doing this!
        close();
    }

    /**
     * <p>Get all activity log entries from within a time period.</p>
     * @param startTimestamp Timestamp from which to include activities (inclusive)
     * @param finishTimestamp Timestamp to which to include activities (inclusive)
     * @param limit Limit number of activity entries to return
     * @param mask Used to match activity bytes. If all the bits in the mask are included in the activity byte, then the activity will be included. For example, if the mask is Activity.ACTION_TYPE_SET, then all logs that have this bit set (set data and set meta data) will be included. However, if Activity.SET_DATA is the mask, then only set data actions will match (since there are two bits set in the mask, making it more restrictive). 0 is least restrictive (restricts nothing) and 0xFF is most restrictive (restricts everything but itself).
     * @return
     */
    public Activity[] getActivityLogEntries(long startTimestamp, long finishTimestamp, int limit, byte mask) throws Exception {
        // As of now, the routing server doesn't log activities (so won't need significant space). 
        // Underlying data server log, however.
        return new Activity[0];
    }

    /**
     * <p>Returns a count of all activity log entries from within a time period.</p>
     * @param startTimestamp Timestamp from which to include activities (inclusive)
     * @param stopTimestamp Timestamp to which to include activities (inclusive)
     * @param mask Used to match activity bytes. If all the bits in the mask are included in the activity byte, then the activity will be included. For example, if the mask is Activity.ACTION_TYPE_SET, then all logs that have this bit set (set data and set meta data) will be included. However, if Activity.SET_DATA is the mask, then only set data actions will match (since there are two bits set in the mask, making it more restrictive). 0 is least restrictive (restricts nothing) and 0xFF is most restrictive (restricts everything but itself).
     * @return
     * @throws java.lang.Exception
     */
    public int getActivityLogEntriesCount(long startTimestamp, long stopTimestamp, byte mask) throws Exception {
        // As of now, the routing server doesn't log activities (so won't need significant space). 
        // Underlying data server log, however.
        return 0;
    }

    /**
     * Helper method to verify nonce.
     *
     * @param   nonce   a nonce byte
     * @return          <code>true</code> if the nonce byte is verified;
     *                  <code>false</code> otherwise
     */
    private boolean verifyNonce(byte[] nonce) {

        if (!nonces.contains(nonce)) {
            return false;
        }
        // Verified, now remove nonce
        nonces.remove(nonce);
        return true;
    }

    /**
     * Helper method to verify certificate is still valid.
     *
     * @param   cert    the certificate
     * @return          <code>true</code> if the certificate is valid;
     *                  <code>false</code> otherwise
     *
     */
    private boolean verifyCertificate(X509Certificate cert) {
        // CHECK CERT AGAINST DEV AUTHORITY. The dev authority may be expired,
        // but should verify anyhow.
        try {
            cert.checkValidity(new Date(TimeUtil.getTrancheTimestamp()));
        } // Keep individual exceptions perchance we later want to allow certain
        catch (CertificateExpiredException e) {
            return false;
        } catch (CertificateNotYetValidException e) {
            return false;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Helper method to verify signatures.
     *
     * @param   bytes                       the bytes to update the signature
     * @param   sig                         the bytes to verify the signature
     * @param   key                         the signature key
     * @param   algorithm                   the signature algoritm
     * @return                              <code>true</code> if the signature is verified;
     *                                      <code>false</code> otherwise
     * @throws  GeneralSecurityException    if a general security exception occurs
     * @throws  IOException                 if an IO exception occurs
     *
     */
    private boolean verifySignature(byte[] bytes, byte[] sig, PublicKey key, String algorithm) throws GeneralSecurityException, IOException {
        java.security.Signature signature = java.security.Signature.getInstance(algorithm);
        signature.initVerify(key);
        signature.update(bytes);
        return signature.verify(sig);
    }

    /**
     * 
     * @param l
     * @return
     */
    public boolean addListener(RoutingTrancheServerListener l) {
        return this.listeners.add(l);
    }

    /**
     * 
     * @param l
     * @return
     */
    public boolean removeListener(RoutingTrancheServerListener l) {
        return this.listeners.remove(l);
    }

    /**
     * 
     */
    public void clearListeners() {
        this.listeners.clear();
    }

    /**
     * 
     * @param hosts
     */
    public void fireDataServersAdded(String[] hosts) {
        for (RoutingTrancheServerListener l : this.listeners) {
            l.dataServersAdded(hosts);
        }
    }

    /**
     * <p>Fired when a RoutingTrancheServer instance removes one or more data server.</p>
     * @param hosts
     */
    public void fireDataServersRemoved(String[] hosts) {
        for (RoutingTrancheServerListener l : this.listeners) {
            l.dataServersRemoved(hosts);
        }
    }

    /**
     * <p>Set data and replicate to specified hosts.</p>
     * @param hash
     * @param data
     * @param sig
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper setData(BigHash hash, byte[] data, Signature sig, String[] hosts) throws Exception {

        // Hosts are required
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hash);
        AssertionUtil.assertNoNullValues(sig);
        AssertionUtil.assertNoNullValues(data);
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);

        Set<String> hostsToUse = new HashSet();

        // Store all exceptions to return to client
        Set<PropagationExceptionWrapper> exceptions = new HashSet();

        // Add every host that isn't included in set of data server covering hash
        for (String host : hosts) {
            if (this.getHost() != null && this.getHost().equals(host)) {
                Set<String> matchingManagedServers = this.getManagedServersThatContain(hash);

                StringBuffer list = new StringBuffer();
                for (String matchingHost : matchingManagedServers) {
                    list.append(matchingHost + " ");
                }

                hostsToUse.addAll(matchingManagedServers);
            } else {
                hostsToUse.add(host);
            }
        }

        if (hostsToUse.size() > 0) {

            Collection<MultiServerRequestStrategy> bestStrategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hostsToUse, Tertiary.DONT_CARE, Tertiary.TRUE);

            // If no strategies, what happened!
            if (bestStrategies.size() == 0) {
                int onlineServers = 0;
                int connectedServers = 0;
                StatusTable st = NetworkUtil.getStatus();

                for (String host : st.getHosts()) {
                    if (st.getRow(host).isOnline()) {
                        onlineServers++;
                    }
                    if (ConnectionUtil.isConnected(host)) {
                        connectedServers++;
                    }
                }

                throw new Exception("Routing server failed to find a strategy to fulfill request. (Hosts to use: " + hostsToUse.size() + "; online servers: " + onlineServers + "; connected servers: " + connectedServers + ")");
            }

            MultiServerRequestStrategy bestStrategy = bestStrategies.toArray(new MultiServerRequestStrategy[0])[0];

            // Play out a strategy
            TrancheServer ts = this.getConnectionForDataServer(bestStrategy.getHostReceivingRequest());

            try {
                PropagationReturnWrapper prw = ts.setData(hash, data, sig, hostsToUse.toArray(new String[0]));
                exceptions.addAll(prw.getErrors());

            } catch (Exception e) {
                exceptions.add(new PropagationExceptionWrapper(e, bestStrategy.getHostReceivingRequest()));

                for (String host : hostsToUse) {
                    exceptions.add(new PropagationExceptionWrapper(new PropagationFailedException(), host));
                }
            }
        } else {
            exceptions.add(new PropagationExceptionWrapper(new NoMatchingServersException(), getHost()));
        }

        return new PropagationReturnWrapper(exceptions);
    }

    /**
     * 
     * @param merge
     * @param hash
     * @param data
     * @param sig
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper setMetaData(boolean merge, BigHash hash, byte[] data, Signature sig, String[] hosts) throws Exception {

        // Hosts are required
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hash);
        AssertionUtil.assertNoNullValues(sig);
        AssertionUtil.assertNoNullValues(data);
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);

        Set<String> hostsToUse = new HashSet();

        // Add every host that isn't included in set of data server covering hash
        for (String host : hosts) {
            if (this.getHost() != null && this.getHost().equals(host)) {
                Set<String> matchingManagedServers = this.getManagedServersThatContain(hash);

                StringBuffer list = new StringBuffer();
                for (String matchingHost : matchingManagedServers) {
                    list.append(matchingHost + " ");
                }

                hostsToUse.addAll(matchingManagedServers);
            } else {
                hostsToUse.add(host);
            }
        }

        // Store all exceptions to return to client
        Set<PropagationExceptionWrapper> exceptions = new HashSet();

        if (hostsToUse.size() > 0) {

            Collection<MultiServerRequestStrategy> bestStrategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hostsToUse, Tertiary.DONT_CARE, Tertiary.TRUE);

            // If no strategies, what happened!
            if (bestStrategies.size() == 0) {
                int onlineServers = 0;
                int connectedServers = 0;
                StatusTable st = NetworkUtil.getStatus();

                for (String host : st.getHosts()) {
                    if (st.getRow(host).isOnline()) {
                        onlineServers++;
                    }
                    if (ConnectionUtil.isConnected(host)) {
                        connectedServers++;
                    }
                }

                throw new Exception("Routing server failed to find a strategy to fulfill request. (Hosts to use: " + hostsToUse.size() + "; online servers: " + onlineServers + "; connected servers: " + connectedServers + ")");
            }
            MultiServerRequestStrategy bestStrategy = bestStrategies.toArray(new MultiServerRequestStrategy[0])[0];

            // Play out a strategy
            TrancheServer ts = this.getConnectionForDataServer(bestStrategy.getHostReceivingRequest());

            try {
                PropagationReturnWrapper prw = ts.setMetaData(merge, hash, data, sig, hostsToUse.toArray(new String[0]));
                exceptions.addAll(prw.getErrors());

            } catch (Exception e) {
                exceptions.add(new PropagationExceptionWrapper(e, bestStrategy.getHostReceivingRequest()));

                for (String host : hostsToUse) {
                    exceptions.add(new PropagationExceptionWrapper(new PropagationFailedException(), host));
                }
            }
        } else {
            exceptions.add(new PropagationExceptionWrapper(new NoMatchingServersException(), getHost()));
        }

        return new PropagationReturnWrapper(exceptions);
    }

    /**
     * <p>Get batch of nonces from selected server hosts.</p>
     * @param hosts
     * @param count
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper getNonces(String[] hosts, int count) throws Exception {

        // Hosts are required
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);

        Set<String> hostsToUse = new HashSet();
        boolean isThisHostIncluded = false;

        for (String host : hosts) {

            // Don't add this host. Later, we'll get a nonce for this host
            if (host.equals(this.getHost())) {
                isThisHostIncluded = true;
            } else {
                hostsToUse.add(host);
            }
        }

        // Store all exceptions to return to client
        Set<PropagationExceptionWrapper> exceptions = new HashSet();

        byte[][][] noncesToReturn = null;

        // -------------------------------------------------------------------------------
        // If there are any hosts besides this routing server
        // -------------------------------------------------------------------------------
        if (hostsToUse.size() > 0) {
            Collection<MultiServerRequestStrategy> bestStrategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hostsToUse, Tertiary.DONT_CARE, Tertiary.DONT_CARE);

            // If no strategies, what happened!
            if (bestStrategies.size() == 0) {
                int onlineServers = 0;
                int connectedServers = 0;
                StatusTable st = NetworkUtil.getStatus();

                for (String host : st.getHosts()) {
                    if (st.getRow(host).isOnline()) {
                        onlineServers++;
                    }
                    if (ConnectionUtil.isConnected(host)) {
                        connectedServers++;
                    }
                }

                throw new Exception("Routing server failed to find a strategy to fulfill request. (Hosts to use: " + hostsToUse.size() + "; online servers: " + onlineServers + "; connected servers: " + connectedServers + ")");
            }

            MultiServerRequestStrategy bestStrategy = bestStrategies.toArray(new MultiServerRequestStrategy[0])[0];

            // Play out a strategy
            TrancheServer ts = this.getConnectionForDataServer(bestStrategy.getHostReceivingRequest());

            try {

                String[] hostsArr = null;

                // If this hosts was included, it was removed. Use that set; otherwise, simply use parameter
                // to this method.
                if (isThisHostIncluded) {
                    hostsArr = hostsToUse.toArray(new String[0]);
                } else {
                    hostsArr = hosts;
                }

                PropagationReturnWrapper prw = ts.getNonces(hostsArr, count);
                exceptions.addAll(prw.getErrors());

                if (!prw.isByteArrayTripleDimension()) {
                    throw new Exception("Expected return type to be a three-dimensional byte array, but wasn't: " + String.valueOf(prw.getReturnValueObject() == null ? "void" : prw.getReturnValueObject().getClass().getSimpleName()));
                }

                byte[][][] returnedValues = (byte[][][]) prw.getReturnValueObject();

                if (returnedValues.length != hostsArr.length) {
                    throw new AssertionFailedException("returnedValues.length<" + returnedValues.length + "> != hostsArr.length<" + hostsArr.length + ">");
                }

                if (isThisHostIncluded) {

                    // Need to put together nonces to return
                    // in the order matching the hosts that were
                    // passed as a parameter to this method
                    noncesToReturn = new byte[hosts.length][count][];

                    HOSTS:
                    for (int i = 0; i < hosts.length; i++) {

                        // Here's the next host
                        String host = hosts[i];

                        /**
                         * If this host is this routing server, simply 
                         * grab the nonce. Else, need to find the matching
                         * nonce from the values returned from other server.
                         */
                        if (host.equals(this.getHost())) {
                            if (count < 0) {
                                throw new Exception("Request for nonces must be zero or greater, found " + count);
                            }
                            noncesToReturn[i] = new byte[count][NonceMap.NONCE_BYTES];
                            for (int j = 0; j < count; j++) {
                                try {
                                    noncesToReturn[i][j] = nonces.newNonce();
                                } catch (ArrayIndexOutOfBoundsException aib) {
                                    System.err.println("ArrayIndexOutOfBoundsException happened: i=" + i + ", j=" + j + ", count=" + count + ", hosts.length=" + hosts.length);
                                    throw aib;
                                }
                            }
                        } else {
                            FIND_MATCHING_NONCE:
                            for (int j = 0; j < hostsArr.length; j++) {
                                if (hostsArr[j].equals(host)) {
                                    noncesToReturn[i] = returnedValues[j];
                                    continue HOSTS;
                                }
                            }
                            throw new AssertionFailedException("Couldn't find matching nonce for host: " + host);
                        }
                    }

                } else {
                    noncesToReturn = returnedValues;
                }

            } catch (Exception e) {
                exceptions.add(new PropagationExceptionWrapper(e, bestStrategy.getHostReceivingRequest()));

                for (String host : hostsToUse) {
                    exceptions.add(new PropagationExceptionWrapper(new PropagationFailedException(), host));
                }
            }
        } // -------------------------------------------------------------------------------
        // If there are not any hosts besides this routing server
        // -------------------------------------------------------------------------------
        else {
            // Is this host included?
            if (isThisHostIncluded) {
                noncesToReturn = new byte[1][count][];
                for (int i = 0; i < count; i++) {
                    noncesToReturn[0][i] = this.nonces.newNonce();
                }
            } else {
                noncesToReturn = new byte[0][0][0];
            }
        }

        return new PropagationReturnWrapper(exceptions, noncesToReturn);
    }

    /**
     * <p>Delete data chunk from selected servers.</p>
     * @param hash
     * @param sigs
     * @param nonces
     * @param hosts
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper deleteData(BigHash hash, Signature[] sigs, byte[][] nonces, String[] hosts) throws Exception {

        // Hosts are required
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hash);
        AssertionUtil.assertNoNullValues(sigs);
        AssertionUtil.assertNoNullValues(nonces);
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);

        String methodName = "deleteData";

        // Quick assertions
        if (sigs.length != hosts.length) {
            throw new AssertionFailedException(methodName + ": sigs.length<" + sigs.length + "> != hosts.length<" + hosts.length + ">");
        }
        if (nonces.length != hosts.length) {
            throw new AssertionFailedException(methodName + ": nonces.length<" + nonces.length + "> != hosts.length<" + hosts.length + ">");
        }

        Set<String> hostsToUse = new HashSet();

        // Store all exceptions to return to client
        Set<PropagationExceptionWrapper> exceptions = new HashSet();

        // -1 means this host wasn't included; otherwise, the value is the index to use for this server index
        int thisServerIndex = -1;

        // Add every host that isn't included in set of data server covering hash
        for (int i = 0; i < hosts.length; i++) {

            String host = hosts[i];

            boolean isThisHost = this.getHost() != null && this.getHost().equals(host);

            if (!isThisHost) {
                hostsToUse.add(host);
            } else {
                thisServerIndex = i;
            }
        }

        // If this server was included, perform delete now
        if (thisServerIndex != -1) {
            try {
                Signature sig = sigs[thisServerIndex];
                byte[] nonce = nonces[thisServerIndex];

                // ---------------------------------------------------------------------------------------
                // Perform security verification (nonce, signature, etc.)
                // ---------------------------------------------------------------------------------------

                // Verify nonce exists and is valid
                boolean nonceValid = verifyNonce(nonce);
                if (!nonceValid) {
                    throw new GeneralSecurityException(BAD_NONCE);
                }

                // param check
                if (sig == null) {
                    throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid signature.");
                }
                if (hash == null) {
                    throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid hash.");
                }
                if (nonce == null) {
                    throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid nonce.");
                }

                // get the certificate
                User user = config.getRecognizedUser(sig.getCert());

                // if certificate can't be loaded, throw an exception
                if (user == null) {
                    throw new GeneralSecurityException(Token.SECURITY_ERROR + " User is not recognized by this server.");
                }

                // check permissions
                if (!user.canDeleteData()) {
                    throw new GeneralSecurityException(Token.SECURITY_ERROR + " User can not delete data.");
                }

                // Check that the certificate is valid
                boolean certIsValid = verifyCertificate(sig.getCert());
                if (!certIsValid) {
                    throw new GeneralSecurityException("User certificate not valid after " + user.getCertificate().getNotAfter().toString());
                }

                // ---------------------------------------------------------------------------------------
                // Make sure that an authority was set for the routing server so can perform deletes
                // ---------------------------------------------------------------------------------------

                X509Certificate thisRoutingServerCert = this.getAuthCert();
                PrivateKey thisRoutingServerPrivateKey = this.getAuthPrivateKey();

                if (thisRoutingServerCert == null || thisRoutingServerPrivateKey == null) {
                    throw new RuntimeException("Cannot perform any deletes from routing server unless certificate and key are set.");
                }

                // ---------------------------------------------------------------------------------------
                // Will check each server to determine if has chunk. (Alternative: check to see
                // if chunk in hash span, but then would need to change so exception not
                // thrown if chunk doesn't exist on server.)
                // ---------------------------------------------------------------------------------------

                Set<String> deepCopyOfServers = getManagedServers();

                SERVERS:
                for (String host : deepCopyOfServers) {

                    TrancheServer dataServer = null;
                    try {
                        dataServer = getConnectionForDataServer(host);

                        boolean performDelete = IOUtil.hasData(dataServer, hash);

                        if (performDelete) {
                            PropagationReturnWrapper prw = IOUtil.deleteData(dataServer, thisRoutingServerCert, thisRoutingServerPrivateKey, hash);
                            if (prw.isAnyErrors()) {
                                exceptions.addAll(prw.getErrors());
                            }
                        }

                    } catch (Exception e) {
                        ConnectionUtil.reportExceptionHost(host, e);

                        // Rethrow the exception, and let client handle. If want an option to continue anyhow,
                        // will need to use propagated delete.
                        throw e;
                    } finally {
                    }
                }
            } catch (Exception e) {
                exceptions.add(new PropagationExceptionWrapper(e, this.getHost()));
            }
        }

        // Only perform propagation if other servers as targets
        if (hostsToUse.size() > 0) {
            Collection<MultiServerRequestStrategy> bestStrategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hostsToUse, Tertiary.DONT_CARE, Tertiary.DONT_CARE);

            // If no strategies, what happened!
            if (bestStrategies.size() == 0) {
                int onlineServers = 0;
                int connectedServers = 0;
                StatusTable st = NetworkUtil.getStatus();

                for (String host : st.getHosts()) {
                    if (st.getRow(host).isOnline()) {
                        onlineServers++;
                    }
                    if (ConnectionUtil.isConnected(host)) {
                        connectedServers++;
                    }
                }

                throw new Exception("Routing server failed to find a strategy to fulfill request. (Hosts to use: " + hostsToUse.size() + "; online servers: " + onlineServers + "; connected servers: " + connectedServers + ")");
            }

            MultiServerRequestStrategy bestStrategy = bestStrategies.toArray(new MultiServerRequestStrategy[0])[0];

            // Create array for parameters
            Signature[] propagationSigs = sigs;
            byte[][] propagationNonces = nonces;
            String[] propagationHosts = hosts;

            // If this server was a target, need to copy over values to array
            if (thisServerIndex != -1) {
                propagationSigs = new Signature[sigs.length - 1];
                propagationNonces = new byte[nonces.length - 1][];
                propagationHosts = new String[hosts.length - 1];

                int count = 0;
                for (int i = 0; i < hosts.length; i++) {
                    if (i == thisServerIndex) {
                        continue;
                    }
                    propagationSigs[count] = sigs[i];
                    propagationNonces[count] = nonces[i];
                    propagationHosts[count] = hosts[i];

                    count++;
                }
            }

            // Play out a strategy
            TrancheServer ts = this.getConnectionForDataServer(bestStrategy.getHostReceivingRequest());

            try {
                PropagationReturnWrapper prw = ts.deleteData(hash, propagationSigs, propagationNonces, propagationHosts);
                exceptions.addAll(prw.getErrors());
            } catch (Exception e) {
                exceptions.add(new PropagationExceptionWrapper(e, bestStrategy.getHostReceivingRequest()));

                for (String host : hostsToUse) {
                    exceptions.add(new PropagationExceptionWrapper(new PropagationFailedException(), host));
                }
            }
        } // If any other hosts to retrieve (besides this)

        return new PropagationReturnWrapper(exceptions);
    }

    /**
     * p>Delete meta data chunk from selected servers.</p>
     * @param hash
     * @param sigs
     * @param nonces
     * @param hosts
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper deleteMetaData(BigHash hash, String uploaderName, Long uploadTimestamp, String relativePathInDataSet, Signature[] sigs, byte[][] nonces, String[] hosts) throws Exception {

        // Hosts are required
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hash);
        AssertionUtil.assertNoNullValues(sigs);
        AssertionUtil.assertNoNullValues(nonces);
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);

        String methodName = "deleteMetaData";

        // Quick assertions
        if (sigs.length != hosts.length) {
            throw new AssertionFailedException(methodName + ": sigs.length<" + sigs.length + "> != hosts.length<" + hosts.length + ">");
        }
        if (nonces.length != hosts.length) {
            throw new AssertionFailedException(methodName + ": nonces.length<" + nonces.length + "> != hosts.length<" + hosts.length + ">");
        }

        Set<String> hostsToUse = new HashSet();

        // Store all exceptions to return to client
        Set<PropagationExceptionWrapper> exceptions = new HashSet();

        // -1 means this host wasn't included; otherwise, the value is the index to use for this server index
        int thisServerIndex = -1;

        // Add every host that isn't included in set of data server covering hash
        for (int i = 0; i < hosts.length; i++) {

            String host = hosts[i];

            boolean isThisHost = this.getHost() != null && this.getHost().equals(host);

            if (!isThisHost) {
                hostsToUse.add(host);
            } else {
                thisServerIndex = i;
            }
        }

        // If this server was included, perform delete now
        if (thisServerIndex != -1) {
            try {
                Signature sig = sigs[thisServerIndex];
                byte[] nonce = nonces[thisServerIndex];
                // ---------------------------------------------------------------------------------------
                // Perform security verification (nonce, signature, etc.)
                // ---------------------------------------------------------------------------------------

                // Verify nonce exists and is valid
                boolean nonceValid = verifyNonce(nonce);
                if (!nonceValid) {
                    throw new GeneralSecurityException(BAD_NONCE);
                }

                // param check
                if (sig == null) {
                    throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid signature.");
                }
                if (hash == null) {
                    throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid hash.");
                }
                if (nonce == null) {
                    throw new Exception(Token.REQUEST_ERROR + " Must pass in a valid nonce.");
                }

                // get the certificate
                User user = config.getRecognizedUser(sig.getCert());

                // if certificate can't be loaded, throw an exception
                if (user == null) {
                    throw new GeneralSecurityException(Token.SECURITY_ERROR + " User is not recognized by this server.");
                }

                // check permissions
                if (!user.canDeleteMetaData()) {
                    throw new GeneralSecurityException(Token.SECURITY_ERROR + " User can not delete mete data.");
                }

                // Check that the certificate is valid
                boolean certIsValid = verifyCertificate(sig.getCert());
                if (!certIsValid) {
                    throw new GeneralSecurityException("User certificate not valid after " + user.getCertificate().getNotAfter().toString());
                }

                // ---------------------------------------------------------------------------------------
                // Make sure that an authority was set for the routing server so can perform deletes
                // ---------------------------------------------------------------------------------------

                X509Certificate thisRoutingServerCert = this.getAuthCert();
                PrivateKey thisRoutingServerPrivateKey = this.getAuthPrivateKey();

                if (thisRoutingServerCert == null || thisRoutingServerPrivateKey == null) {
                    throw new RuntimeException("Cannot perform any deletes from routing server unless certificate and key are set.");
                }

                // ---------------------------------------------------------------------------------------
                // Will check each server to determine if has chunk. (Alternative: check to see
                // if chunk in hash span, but then would need to change so exception not
                // thrown if chunk doesn't exist on server.)
                // ---------------------------------------------------------------------------------------

                Set<String> deepCopyOfServers = getManagedServers();

                SERVERS:
                for (String host : deepCopyOfServers) {

                    TrancheServer dataServer = null;
                    try {
                        dataServer = getConnectionForDataServer(host);

                        boolean performDelete = IOUtil.hasMetaData(dataServer, hash);

                        if (performDelete) {
                            PropagationReturnWrapper prw = IOUtil.deleteMetaData(dataServer, thisRoutingServerCert, thisRoutingServerPrivateKey, hash);
                            if (prw.isAnyErrors()) {
                                exceptions.addAll(prw.getErrors());
                            }
                        }

                    } catch (Exception e) {
                        ConnectionUtil.reportExceptionHost(host, e);

                        // Rethrow the exception, and let client handle. If want an option to continue anyhow,
                        // will need to use propagated delete.
                        throw e;
                    } finally {
                    }
                }
            } catch (Exception e) {
                exceptions.add(new PropagationExceptionWrapper(e, this.getHost()));
            }
        }

        // Only perform propagation if other servers as targets
        if (hostsToUse.size() > 0) {
            Collection<MultiServerRequestStrategy> bestStrategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(hostsToUse, Tertiary.DONT_CARE, Tertiary.DONT_CARE);

            // If no strategies, what happened!
            if (bestStrategies.size() == 0) {
                int onlineServers = 0;
                int connectedServers = 0;
                StatusTable st = NetworkUtil.getStatus();

                for (String host : st.getHosts()) {
                    if (st.getRow(host).isOnline()) {
                        onlineServers++;
                    }
                    if (ConnectionUtil.isConnected(host)) {
                        connectedServers++;
                    }
                }

                throw new Exception("Routing server failed to find a strategy to fulfill request. (Hosts to use: " + hostsToUse.size() + "; online servers: " + onlineServers + "; connected servers: " + connectedServers + ")");
            }

            MultiServerRequestStrategy bestStrategy = bestStrategies.toArray(new MultiServerRequestStrategy[0])[0];

            // Create array for parameters
            Signature[] propagationSigs = sigs;
            byte[][] propagationNonces = nonces;
            String[] propagationHosts = hosts;

            // If this server was a target, need to copy over values to array
            if (thisServerIndex != -1) {
                propagationSigs = new Signature[sigs.length - 1];
                propagationNonces = new byte[nonces.length - 1][];
                propagationHosts = new String[hosts.length - 1];

                int count = 0;
                for (int i = 0; i < hosts.length; i++) {
                    if (i == thisServerIndex) {
                        continue;
                    }
                    propagationSigs[count] = sigs[i];
                    propagationNonces[count] = nonces[i];
                    propagationHosts[count] = hosts[i];

                    count++;
                }
            }

            // Play out a strategy
            TrancheServer ts = this.getConnectionForDataServer(bestStrategy.getHostReceivingRequest());

            try {
                PropagationReturnWrapper prw = ts.deleteMetaData(hash, uploaderName, uploadTimestamp, relativePathInDataSet, propagationSigs, propagationNonces, propagationHosts);
                exceptions.addAll(prw.getErrors());

            } catch (Exception e) {
                exceptions.add(new PropagationExceptionWrapper(e, bestStrategy.getHostReceivingRequest()));

                for (String host : hostsToUse) {
                    exceptions.add(new PropagationExceptionWrapper(new PropagationFailedException(), host));
                }
            }
        } // If any other hosts to retrieve (besides this)

        return new PropagationReturnWrapper(exceptions);
    }

    /**
     * <p>By setting a certificate and private key to the routing Tranche server that can perform deletes, will be able to delete from managed data servers.</p>
     * <p>It is the responsibility of the administrator starting the routing server to secure a signed key/certificate that can perform deletes. Please contact the Tranche network administrators for more information.</p>
     * @return
     */
    public X509Certificate getAuthCert() {
        return authCert;
    }

    /**
     * <p>By setting a certificate and private key to the routing Tranche server that can perform deletes, will be able to delete from managed data servers.</p>
     * <p>It is the responsibility of the administrator starting the routing server to secure a signed key/certificate that can perform deletes. Please contact the Tranche network administrators for more information.</p>
     * @param authCert
     */
    public void setAuthCert(X509Certificate authCert) {
        this.authCert = authCert;
    }

    /**
     * <p>By setting a certificate and private key to the routing Tranche server that can perform deletes, will be able to delete from managed data servers.</p>
     * <p>It is the responsibility of the administrator starting the routing server to secure a signed key/certificate that can perform deletes. Please contact the Tranche network administrators for more information.</p>
     * @return
     */
    public PrivateKey getAuthPrivateKey() {
        return authPrivateKey;
    }

    /**
     * <p>By setting a certificate and private key to the routing Tranche server that can perform deletes, will be able to delete from managed data servers.</p>
     * <p>It is the responsibility of the administrator starting the routing server to secure a signed key/certificate that can perform deletes. Please contact the Tranche network administrators for more information.</p>
     * @param authPrivateKey
     */
    public void setAuthPrivateKey(PrivateKey authPrivateKey) {
        this.authPrivateKey = authPrivateKey;
    }

    /**
     * <p>Returns true if and only if the server has been closed.</p>
     * @return
     */
    public boolean isClosed() {
        return closed;
    }
}
