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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.TrancheServer;
import org.tranche.exceptions.PropagationFailedException;
import org.tranche.exceptions.PropagationUnfulfillableHostException;
import org.tranche.exceptions.ServerIsOfflineException;
import org.tranche.exceptions.TrancheProtocolException;
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
public class NonceItem extends ServerItem {

    /**
     * @param   server  the server received
     */
    public NonceItem(Server server) {
        super(Token.GET_NONCES_STRING, server);
    }

    /**
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public final void doAction(InputStream in, OutputStream out, String clientIP) throws Exception {
        final int requestedNonces,  count;
        final List<String> hosts;
        final List<byte[][]> nonces;
        try {
            requestedNonces = RemoteUtil.readInt(in);
            count = RemoteUtil.readInt(in);
            hosts = new ArrayList<String>(count);
            nonces = new ArrayList<byte[][]>(count);
            for (int i = 0; i < count; i++) {
                hosts.add(RemoteUtil.readLine(in));
                nonces.add(new byte[0][0]);
            }
        } catch (Exception e) {
            throw new TrancheProtocolException();
        }

        // Store all exceptions. They will be returned to caller. (Note that the caller
        // is often another server, which will return to caller, etc... propagation recursive.)
        byte[][][] returnVal = new byte[hosts.size()][0][0];
        final Set<PropagationExceptionWrapper> exceptions = new HashSet<PropagationExceptionWrapper>();
        final Set<String> hostsSet = new HashSet<String>(hosts);
        try {
            // If so, add from targets collection
            if (hostsSet.contains(server.getHostName())) {
                hostsSet.remove(server.getHostName());
                PropagationReturnWrapper wrapper = null;
                try {
                    wrapper = server.getTrancheServer().getNonces(new String[]{server.getHostName()}, requestedNonces);
                } catch (Exception e) {
                    exceptions.add(new PropagationExceptionWrapper(e, server.getHostName()));
                }
                if (wrapper != null) {
                    exceptions.addAll(wrapper.getErrors());
                    if (!wrapper.isVoid()) {
                        byte[][][] returnedNonces = (byte[][][]) wrapper.getReturnValueObject();
                        nonces.set(hosts.indexOf(server.getHostName()), returnedNonces[0]);
                    }
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
                                    String[] hostsSubset = strategy.getPartitionsMap().get(hostToContact).toArray(new String[0]);
                                    PropagationReturnWrapper prw = ts.getNonces(hostsSubset, requestedNonces);
                                    exceptions.addAll(prw.getErrors());
                                    if (!prw.isVoid()) {
                                        byte[][][] noncesSubset = (byte[][][]) prw.getReturnValueObject();
                                        for (int i = 0; i < hostsSubset.length; i++) {
                                            nonces.set(hosts.indexOf(hostsSubset[i]), noncesSubset[i]);
                                        }
                                    }
                                } finally {
                                    ConnectionUtil.unlockConnection(hostToContact);
                                }
                            } catch (Exception e) {
                                // Add to collection of exceptions.
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
            for (int i = 0; i < nonces.size(); i++) {
                returnVal[i] = nonces.get(i);
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
        GetBytesCallback.writeResponse(new PropagationReturnWrapper(exceptions, returnVal).toByteArray(), out);
    }

    /**
     * 
     * @param writeHeader
     * @param numNonces
     * @param hosts
     * @param out
     * @throws java.lang.Exception
     */
    public static final void writeRequest(boolean writeHeader, int numNonces, String[] hosts, OutputStream out) throws Exception {
        if (writeHeader) {
            RemoteUtil.writeBytes(Token.GET_NONCES, out);
        }
        RemoteUtil.writeInt(numNonces, out);
        RemoteUtil.writeInt(hosts.length, out);
        for (String host : hosts) {
            RemoteUtil.writeLine(host, out);
        }
    }
}
