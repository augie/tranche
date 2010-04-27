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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.security.Signature;
import org.tranche.TrancheServer;
import org.tranche.exceptions.PropagationFailedException;
import org.tranche.exceptions.PropagationUnfulfillableHostException;
import org.tranche.exceptions.ServerIsOfflineException;
import org.tranche.exceptions.TrancheProtocolException;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.MultiServerRequestStrategy;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.remote.GetBytesCallback;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;

/**
 * <p>Propagated request to set data to any number of servers.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SetDataItem extends ServerItem {

    /**
     * p>Propagated request to set data to any number of servers.</p>
     * @param   server  the server received
     */
    public SetDataItem(Server server) {
        super(Token.SET_DATA_STRING, server);
    }

    /**
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public final void doAction(InputStream in, OutputStream out, String clientIP) throws Exception {
        final BigHash hash;
        final Set<String> hostsSet = new HashSet<String>();
        final byte[] data;
        final Signature sig;
        try {
            hash = RemoteUtil.readBigHash(in);
            int hostCount = RemoteUtil.readInt(in);
            for (int i = 0; i < hostCount; i++) {
                hostsSet.add(RemoteUtil.readLine(in));
            }
            data = RemoteUtil.readDataBytes(in);
            sig = RemoteUtil.readSignature(in);
        } catch (Exception e) {
            throw new TrancheProtocolException();
        }

        // Store all exceptions. They will be returned to caller. (Note that the caller
        // is often another server, which will return to caller, etc... propagation recursive.)
        final Set<PropagationExceptionWrapper> exceptions = new HashSet();
        try {
            // If this server is in the target server collection, add from targets collection
            if (hostsSet.contains(server.getHostName())) {
                hostsSet.remove(server.getHostName());
                try {
                    exceptions.addAll(server.getTrancheServer().setData(hash, data, sig, new String[]{server.getHostName()}).getErrors());
                } catch (Exception e) {
                    exceptions.add(new PropagationExceptionWrapper(e, server.getHostName()));
                }
            }

            if (!hostsSet.isEmpty()) {
                // Build the strategy for this server. (There's a unit test that shows this is deterministic and, furthermore,
                // removing this host from the the hostsSet does not impact the strategy.)
                final MultiServerRequestStrategy strategy = MultiServerRequestStrategy.create(server.getHostName(), hostsSet);

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
                                    PropagationReturnWrapper prw = null;

                                    // Cannot propagate over sockets since RTS shared between test servers and client. A multi-threaded tool (e.g., AFT)
                                    // will queue up requests before this request, and hence a timeout will occur.
                                    if (TestUtil.isTestingManualNetworkStatusTable()) {
                                        Set<String> partitionRecipients = strategy.getPartitionsMap().get(hostToContact);
                                        Set<PropagationExceptionWrapper> exceptionWrappers = new HashSet();

                                        for (String nextOstensibleHost : partitionRecipients) {
                                            final String actualTestUrl = TestUtil.getServerTestURL(nextOstensibleHost);
                                            FlatFileTrancheServer ffts = TestUtil.getFFTSForURL(actualTestUrl);

                                            if (ffts == null) {
                                                throw new Exception("Couldn't find (test) FFTS for: " + actualTestUrl);
                                            }

                                            String[] hostArr = {nextOstensibleHost};
                                            PropagationReturnWrapper nextWrapper = ffts.setData(hash, data, sig, hostArr);
                                            exceptionWrappers.addAll(nextWrapper.getErrors());
                                        }

                                        prw = new PropagationReturnWrapper(exceptionWrappers);
                                    } else {
                                        prw = ts.setData(hash, data, sig, strategy.getPartitionsMap().get(hostToContact).toArray(new String[0]));
                                    }

                                    exceptions.addAll(prw.getErrors());
                                } finally {
                                    ConnectionUtil.unlockConnection(hostToContact);
                                }
                            } catch (Exception e) {
                                exceptions.add(new PropagationExceptionWrapper(e, hostToContact));

                                // Uh-oh. Don't know whether other servers were set or not. Throwing an exception like this
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
            for (String nextFailedHost : hostsSet) {
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
     * @param bytes
     * @param signature
     * @param out
     * @throws java.lang.Exception
     */
    public static final void writeRequest(boolean writeHeader, BigHash hash, String[] hosts, byte[] bytes, Signature signature, OutputStream out) throws Exception {
        if (writeHeader) {
            RemoteUtil.writeBytes(Token.SET_DATA, out);
        }

        RemoteUtil.writeBigHash(hash, out);
        RemoteUtil.writeInt(hosts.length, out);
        for (String host : hosts) {
            RemoteUtil.writeLine(host, out);
        }

        RemoteUtil.writeData(bytes, out);
        RemoteUtil.writeSignature(signature, out);
    }
}
