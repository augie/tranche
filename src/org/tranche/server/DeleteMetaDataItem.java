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
package org.tranche.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tranche.security.Signature;
import org.tranche.hash.BigHash;
import org.tranche.TrancheServer;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.exceptions.PropagationFailedException;
import org.tranche.exceptions.PropagationUnfulfillableHostException;
import org.tranche.exceptions.ServerIsOfflineException;
import org.tranche.exceptions.TrancheProtocolException;
import org.tranche.flatfile.NonceMap;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.MultiServerRequestStrategy;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.remote.GetBytesCallback;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class DeleteMetaDataItem extends ServerItem {

    /**
     * @param   server  the server received
     */
    public DeleteMetaDataItem(Server server) {
        super(Token.DELETE_META_STRING, server);
    }

    /**
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public final void doAction(InputStream in, OutputStream out, String clientIP) throws Exception {
        final BigHash hash;
        final String uploaderName,  relativePathInDataSet;
        final long uploaderTimestamp;
        final Map<String, HostSignatureNonceWrapper> hostMap = new HashMap<String, HostSignatureNonceWrapper>();
        try {
            hash = RemoteUtil.readBigHash(in);
            // whether this is an individual uploader deletion
            if (RemoteUtil.readBoolean(in)) {
                uploaderName = RemoteUtil.readLine(in);
                uploaderTimestamp = RemoteUtil.readLong(in);
                if (RemoteUtil.readBoolean(in)) {
                    relativePathInDataSet = RemoteUtil.readLine(in);
                } else {
                    relativePathInDataSet = null;
                }
            } else {
                uploaderName = null;
                relativePathInDataSet = null;
                uploaderTimestamp = -1;
            }
            // Get all the host-specific information
            int targetHostCount = RemoteUtil.readInt(in);
            for (int i = 0; i < targetHostCount; i++) {
                String nextHost = RemoteUtil.readLine(in);
                Signature nextSig = RemoteUtil.readSignature(in);
                byte[] nextNonce = RemoteUtil.readBytes(NonceMap.NONCE_BYTES, in);
                hostMap.put(nextHost, new HostSignatureNonceWrapper(nextHost, nextSig, nextNonce));
            }
        } catch (Exception e) {
            throw new TrancheProtocolException();
        }

        // Store all exceptions. They will be returned to caller. (Note that the caller
        // is often another server, which will return to caller, etc... propagation recursive.)
        final Set<PropagationExceptionWrapper> exceptions = new HashSet();
        try {
            if (hostMap.containsKey(server.getHostName())) {
                HostSignatureNonceWrapper thisServerWrapper = hostMap.remove(server.getHostName());
                byte[][] nonces = new byte[1][0];
                nonces[0] = thisServerWrapper.nonce;
                try {
                    exceptions.addAll(server.getTrancheServer().deleteMetaData(hash, uploaderName, uploaderTimestamp, relativePathInDataSet, new Signature[]{thisServerWrapper.sig}, nonces, new String[]{server.getHostName()}).getErrors());
                } catch (Exception e) {
                    exceptions.add(new PropagationExceptionWrapper(e, server.getHostName()));
                }
            }
            if (!hostMap.isEmpty()) {
                // Build the strategy for this server. (There's a unit test that shows this is deterministic and, furthermore,
                // removing this host from the the hostsSet does not impact the strategy.)
                final MultiServerRequestStrategy strategy = MultiServerRequestStrategy.create(server.getHostName(), hostMap.keySet());

                // All unfulfillable requests are exceptions. If server is offline, client should have checked.
                for (String unfulfillableHost : strategy.getUnfulfillableHosts()) {
                    exceptions.add(new PropagationExceptionWrapper(new PropagationUnfulfillableHostException(), unfulfillableHost));
                }

                // Propagate out the requests to direct connections using strategy
                List<Thread> threads = new LinkedList<Thread>();
                for (final String hostToContact : strategy.getPartitionsMap().keySet()) {
                    Thread t = new Thread(hostToContact) {

                        @Override
                        public void run() {
                            try {
                                // check for offline server
                                StatusTableRow row = NetworkUtil.getStatus().getRow(hostToContact);
                                if (!row.isOnline()) {
                                    throw new ServerIsOfflineException(hostToContact);
                                }
                                TrancheServer ts = ConnectionUtil.connectHost(hostToContact, true);
                                if (ts == null) {
                                    throw new ServerIsOfflineException(hostToContact);
                                }
                                try {
                                    String[] hostsSubset = strategy.getPartitionsMap().get(hostToContact).toArray(new String[0]);
                                    byte[][] noncesSubsetArr = new byte[hostsSubset.length][];
                                    Signature[] sigsSubsetArr = new Signature[hostsSubset.length];

                                    // Build up above arrays using supplied information. Just a matter of matching
                                    // target host names with associated nonce and signature.
                                    for (int i = 0; i < hostsSubset.length; i++) {
                                        HostSignatureNonceWrapper nextWrapper = hostMap.get(hostsSubset[i]);
                                        if (nextWrapper == null) {
                                            throw new AssertionFailedException("Could not find the HostSignatureNonceWrapper for \"" + hostsSubset[i] + "\".");
                                        }

                                        // Set the sig+host+nonce triplet
                                        noncesSubsetArr[i] = nextWrapper.nonce;
                                        sigsSubsetArr[i] = nextWrapper.sig;
                                    }

                                    exceptions.addAll(ts.deleteMetaData(hash, uploaderName, uploaderTimestamp, relativePathInDataSet, sigsSubsetArr, noncesSubsetArr, hostsSubset).getErrors());
                                } finally {
                                    ConnectionUtil.unlockConnection(hostToContact);
                                }
                            } catch (Exception e) {
                                exceptions.add(new PropagationExceptionWrapper(e, hostToContact));

                                // Uh-oh. Don't know whether other servers were set or not! Throwing an exception like this
                                // probably means I/O exception, so likely that everything was lost. Assume worst.
                                for (String nextFailedHost : strategy.getPartitionsMap().get(hostToContact)) {
                                    if (nextFailedHost.equals(hostToContact)) {
                                        continue;
                                    }
                                    exceptions.add(new PropagationExceptionWrapper(new PropagationFailedException(), nextFailedHost));
                                }
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                    threads.add(t);
                }
                // wait for the threads to stop
                for (Thread t : threads) {
                    try {
                        t.join();
                    } catch (Exception e) {
                        exceptions.add(new PropagationExceptionWrapper(e, t.getName()));
                    }
                }
            }
        } catch (Exception e) {
            exceptions.add(new PropagationExceptionWrapper(e, server.getHostName()));
            for (String nextFailedHost : hostMap.keySet()) {
                if (nextFailedHost.equals(server.getHostName())) {
                    continue;
                }
                exceptions.add(new PropagationExceptionWrapper(new PropagationFailedException(), nextFailedHost));
            }
        }
        GetBytesCallback.writeResponse(new PropagationReturnWrapper(exceptions).toByteArray(), out);
    }

    /**
     * 
     * @param writeHeader
     * @param hash
     * @param hosts
     * @param signatures
     * @param nonces
     * @param out
     * @throws java.lang.Exception
     */
    public final static void writeRequest(boolean writeHeader, BigHash hash, String uploaderName, Long uploadTimestamp, String relativePathInDataSet, String[] hosts, Signature[] signatures, byte[][] nonces, OutputStream out) throws Exception {
        if (writeHeader) {
            RemoteUtil.writeBytes(Token.DELETE_META, out);
        }

        RemoteUtil.writeBigHash(hash, out);
        if (uploaderName != null && uploadTimestamp != null) {
            RemoteUtil.writeBoolean(true, out);
            RemoteUtil.writeLine(uploaderName, out);
            RemoteUtil.writeLong(uploadTimestamp, out);
            if (relativePathInDataSet != null) {
                RemoteUtil.writeBoolean(true, out);
                RemoteUtil.writeLine(relativePathInDataSet, out);
            } else {
                RemoteUtil.writeBoolean(false, out);
            }

        } else {
            RemoteUtil.writeBoolean(false, out);
        }

        RemoteUtil.writeInt(hosts.length, out);
        for (int i = 0; i <
                hosts.length; i++) {
            RemoteUtil.writeLine(hosts[i], out);
            RemoteUtil.writeSignature(signatures[i], out);
            RemoteUtil.writeBytes(nonces[i], out);
        }
    }
}
