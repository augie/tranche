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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import org.tranche.TrancheServer;
import org.tranche.exceptions.TrancheProtocolException;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.meta.MetaDataUtil;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.remote.GetBytesCallback;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.routing.RoutingTrancheServer;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;

/**
 * <p>Handles the request for a list of meta data chunks.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetMetaDataItem extends ServerItem {

    /**
     * @param   server  the server received
     */
    public GetMetaDataItem(Server server) {
        super(Token.GET_META_STRING, server);
    }

    /**
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public final void doAction(InputStream in, OutputStream out, String clientIP) throws Exception {
        BigHash[] hashes;
        boolean propagateRequest;
        try {
            hashes = RemoteUtil.readBigHashArray(in);
            propagateRequest = RemoteUtil.readBoolean(in);
        } catch (Exception e) {
            throw new TrancheProtocolException();
        }

        // verify the number of hashes is less than or equal to the allowed amount
        if (hashes.length > RemoteTrancheServer.BATCH_GET_LIMIT) {
            throw new Exception("Limit exceeded for number of items in a batch request. Requested items: " + hashes.length + ", Maximum Allowed: " + RemoteTrancheServer.BATCH_GET_LIMIT);
        }

        // execute
        byte[][] metaDataBytes = null;
        Set<PropagationExceptionWrapper> exceptionSet = new HashSet<PropagationExceptionWrapper>();
        try {
            PropagationReturnWrapper thisServerWrapper = server.getTrancheServer().getMetaData(hashes, propagateRequest);
            metaDataBytes = (byte[][]) thisServerWrapper.getReturnValueObject();
            exceptionSet.addAll(thisServerWrapper.getErrors());
            // propagate?
            if (propagateRequest && !(server.getTrancheServer() instanceof RoutingTrancheServer)) {
                for (int i = 0; i < hashes.length; i++) {
                    // got the chunk locally
                    if (metaDataBytes[i] != null) {
                        continue;
                    }
                    // check the local target hash spans
                    boolean shouldWriteLocal = false;
                    if (NetworkUtil.getLocalServerRow() != null) {
                        for (HashSpan hashSpan : NetworkUtil.getLocalServerRow().getTargetHashSpans()) {
                            if (hashSpan.contains(hashes[i])) {
                                shouldWriteLocal = NetworkUtil.getLocalServerRow().isWritable();
                                break;
                            }
                        }
                    }
                    // does not exist on this server -- try others
                    for (StatusTableRow row : ConnectionUtil.getConnectedRows()) {
                        try {
                            // online, core, readable, not this server
                            if (!(row.isOnline() && row.isCore() && row.isReadable() && !row.getHost().equals(server.getHostName()))) {
                                continue;
                            }
                            
                            // in hash span for server?
                            boolean inHashSpan = false;
                            for (HashSpan hashSpan : row.getHashSpans()) {
                                if (hashSpan.contains(hashes[i])) {
                                    inHashSpan = true;
                                    break;
                                }
                            }
                            if (!inHashSpan) {
                                continue;
                            }

                            PropagationReturnWrapper wrapper = null;

                            // If testing, cannot propagate using RTS since might get queued behind subsequent client
                            // requests, and hence never get satisfied (resulting in time out)
                            if (TestUtil.isTestingManualNetworkStatusTable()) {
                                String actualTestURL = TestUtil.getServerTestURL(row.getHost());
                                FlatFileTrancheServer ffts = TestUtil.getFFTSForURL(actualTestURL);
                                if (ffts == null) {
                                    throw new Exception("Couldn't find (test) FFTS for: " + actualTestURL);
                                }
                                wrapper = ffts.getMetaData(new BigHash[]{hashes[i]}, false);
                            } else {
                                TrancheServer ts = ConnectionUtil.connect(row, true);
                                if (ts == null) {
                                    break;
                                }
                                try {
                                    wrapper = IOUtil.getMetaData(ts, hashes[i], false);
                                } finally {
                                    ConnectionUtil.unlockConnection(row.getHost());
                                }
                            }

                            exceptionSet.addAll(wrapper.getErrors());
                            metaDataBytes[i] = IOUtil.get1DBytes(wrapper);
                            if (metaDataBytes[i] == null) {
                                continue;
                            }

                            ByteArrayInputStream bais = null;
                            try {
                                bais = new ByteArrayInputStream(metaDataBytes[i]);
                                if (MetaDataUtil.read(bais) == null) {
                                    throw new NullPointerException("Meta data object was null; should have thrown exception.");
                                }
                            } catch (Exception e) {
                                metaDataBytes[i] = null;
                                continue;
                            } finally {
                                IOUtil.safeClose(bais);
                            }

                            if (shouldWriteLocal) {
                                ((FlatFileTrancheServer) server.getTrancheServer()).getDataBlockUtil().addMetaData(hashes[i], metaDataBytes[i]);
                            }

                            break;
                        } catch (Exception e) {
                            exceptionSet.add(new PropagationExceptionWrapper(e, row.getHost()));
                            ConnectionUtil.reportException(row, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            exceptionSet.add(new PropagationExceptionWrapper(e, server.getHostName()));
        }
        GetBytesCallback.writeResponse(new PropagationReturnWrapper(exceptionSet, metaDataBytes).toByteArray(), out);
        // log
        server.getSubmitter().logBatchGetMetaData(hashes, clientIP);
    }

    /**
     * 
     * @param writeHeader
     * @param hashes
     * @param propagateRequest
     * @param out
     * @throws java.lang.Exception
     */
    public final static void writeRequest(boolean writeHeader, BigHash[] hashes, boolean propagateRequest, OutputStream out) throws Exception {
        if (writeHeader) {
            RemoteUtil.writeBytes(Token.GET_META, out);
        }
        RemoteUtil.writeBigHashArray(hashes, out);
        RemoteUtil.writeBoolean(propagateRequest, out);
    }
}
