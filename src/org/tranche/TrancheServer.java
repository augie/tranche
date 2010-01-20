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
package org.tranche;

import org.tranche.security.Signature;
import java.math.BigInteger;
import org.tranche.hash.BigHash;
import org.tranche.configuration.Configuration;
import org.tranche.logs.activity.Activity;
import org.tranche.network.StatusTable;
import org.tranche.remote.Token;
import org.tranche.server.PropagationReturnWrapper;

/**
 * <p>This is the abstract interface for a node in the distributed file system. The get() methods are intended for public use however, the add() and delete() methods may only be invoked by a recognized authority.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public abstract class TrancheServer {

    public static final String BAD_NONCE = Token.SECURITY_ERROR + " Nonce doesn't match. Might be a replay.",  BAD_SIG = Token.SECURITY_ERROR + " Signature doesn't match. Ignoring request.";

    /**
     * <p>Ping the server. Handled by Server object which wraps this object.</p>
     * @return              Token.OK
     * @throws java.lang.Exception
     */
    public abstract void ping() throws Exception;

    /**
     * 
     * @param startHost
     * @param endHost
     * @return
     * @throws java.lang.Exception
     */
    public abstract StatusTable getNetworkStatusPortion(String startHost, String endHost) throws Exception;

    /**
     * <p>Get batch of nonces from selected server hosts.</p>
     * @param hosts
     * @param count
     * @return
     * @throws java.lang.Exception
     */
    public abstract PropagationReturnWrapper getNonces(String[] hosts, int count) throws Exception;


    /**
     * @param   hashes      the BigHash array of data chunks
     * @return              boolean array
     * @throws  Exception   if any exception occurs
     */
    public abstract boolean[] hasData(BigHash[] hashes) throws Exception;

    /**
     *
     * @param hashes
     * @param propagateRequest Whether the server should propagate the request across the network.
     * @return
     * @throws java.lang.Exception
     */
    public abstract PropagationReturnWrapper getData(BigHash[] hashes, boolean propagateRequest) throws Exception;

    /**
     * <p>Set data and replicate to specified hosts.</p>
     * @param hash
     * @param data
     * @param sig
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public abstract PropagationReturnWrapper setData(BigHash hash, byte[] data, Signature sig, String[] hosts) throws Exception;

    /**
     * <p>Delete data chunk from selected servers.</p>
     * @param hash
     * @param sigs
     * @param nonces
     * @param hosts
     * @throws java.lang.Exception
     */
    public abstract PropagationReturnWrapper deleteData(BigHash hash, Signature[] sigs, byte[][] nonces, String[] hosts) throws Exception;

    /**
     * @param   hashes      the BigHash array of meta data chunks
     * @return              boolean array
     * @throws  Exception   if any exception occurs
     */
    public abstract boolean[] hasMetaData(BigHash[] hashes) throws Exception;

    /**
     * 
     * @param hashes
     * @param propagateRequest
     * @return
     * @throws java.lang.Exception
     */
    public abstract PropagationReturnWrapper getMetaData(BigHash[] hashes, boolean propagateRequest) throws Exception;

    /**
     * <p>Set meta data and replicate to specified hosts.</p>
     * @param merge
     * @param hash
     * @param data
     * @param sig
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public abstract PropagationReturnWrapper setMetaData(boolean merge, BigHash hash, byte[] data, Signature sig, String[] hosts) throws Exception;

    /**
     * @param hash
     * @param sigs
     * @param nonces
     * @param hosts
     * @throws java.lang.Exception
     */
    public abstract PropagationReturnWrapper deleteMetaData(BigHash hash, String uploaderName, Long uploadTimestamp, String relativePathInDataSet, Signature[] sigs, byte[][] nonces, String[] hosts) throws Exception;

    /**
     * @param   offset      the offset
     * @param   length      the length
     * @return              an array of data hashes
     * @throws  Exception   if any exception occurs
     */
    public abstract BigHash[] getDataHashes(BigInteger offset, BigInteger length) throws Exception;

    /**
     * @param   offset      the offset
     * @param   length      the length
     * @return              an array of meta data hashes
     * @throws  Exception   if any exception occurs
     */
    public abstract BigHash[] getMetaDataHashes(BigInteger offset, BigInteger length) throws Exception;

    /**
     * Returns a list of all projects that this server has.
     *
     * @param   offset      the offset
     * @param   length      the length
     * @return              an array of project hashes
     * @throws  Exception   if any exception occurs
     */
    public abstract BigHash[] getProjectHashes(BigInteger offset, BigInteger length) throws Exception;

    /**
     * @param   sig         the signature
     * @param   nonce       the nonce bytes
     * @return              the configuration
     * @throws  Exception   if any exception occurs
     */
    public abstract Configuration getConfiguration(Signature sig, byte[] nonce) throws Exception;

    /**
     * Sets a new configuration for the server. Must have appropriate permission to do so.
     *
     * @param   data        The configuration saved as an array of bytes. Use ConfigurationUtil to generate
     *                      the appropriate bytes.
     * @param   sig         A signature for the data.
     * @param   nonce       The nonce used from the server.
     * @throws  Exception   If invalid configuration format exceptions or security exceptions occur.
     */
    public abstract void setConfiguration(byte[] data, Signature sig, byte[] nonce) throws Exception;

    /**
     * Registers the given URL as a known server. The server chooses if or if not it will remember this URL.
     *
     * @param   url         the URL to register (e.g. tranche://proteomecommons.org:443 )
     * @throws  Exception   if any exception occurs
     */
    public abstract void registerServer(String url) throws Exception;

    /**
     * <p>Request that a server shut down. Intended for remote administration of servers.</p>
     * @param sig
     * @param nonce
     * @throws java.lang.Exception
     */
    public abstract void requestShutdown(Signature sig, byte[] nonce) throws Exception;

    /**
     * <p>Get all activity log entries from within a time period.</p>
     * @param startTimestamp Timestamp from which to include activities (inclusive)
     * @param finishTimestamp Timestamp to which to include activities (inclusive)
     * @param limit Limit number of activity entries to return
     * @param mask Used to match activity bytes. If all the bits in the mask are included in the activity byte, then the activity will be included. For example, if the mask is Activity.ACTION_TYPE_SET, then all logs that have this bit set (set data and set meta data) will be included. However, if Activity.SET_DATA is the mask, then only set data actions will match (since there are two bits set in the mask, making it more restrictive). 0 is least restrictive (restricts nothing) and 0xFF is most restrictive (restricts everything but itself).
     * @return
     */
    public abstract Activity[] getActivityLogEntries(long startTimestamp, long finishTimestamp, int limit, byte mask) throws Exception;

    /**
     * <p>Returns a count of all activity log entries from within a time period.</p>
     * @param startTimestamp Timestamp from which to include activities (inclusive)
     * @param stopTimestamp Timestamp to which to include activities (inclusive)
     * @param mask Used to match activity bytes. If all the bits in the mask are included in the activity byte, then the activity will be included. For example, if the mask is Activity.ACTION_TYPE_SET, then all logs that have this bit set (set data and set meta data) will be included. However, if Activity.SET_DATA is the mask, then only set data actions will match (since there are two bits set in the mask, making it more restrictive). 0 is least restrictive (restricts nothing) and 0xFF is most restrictive (restricts everything but itself).
     * @return
     * @throws java.lang.Exception
     */
    public abstract int getActivityLogEntriesCount(long startTimestamp, long stopTimestamp, byte mask) throws Exception;

    /**
     * <p>Closes the server and shuts down any associated resources.</p>
     */
    public abstract void close();

    /**
     * <p>Returns the host name of the server.</p>
     * @return
     */
    public abstract String getHost();
}
