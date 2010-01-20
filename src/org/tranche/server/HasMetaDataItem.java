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
import org.tranche.exceptions.TrancheProtocolException;
import org.tranche.hash.BigHash;
import org.tranche.remote.BooleanArrayCallback;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;

/**
 * <p>Handles requests for lists of has meta data chunk requests.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class HasMetaDataItem extends ServerItem {

    /**
     * @param   server  the server received
     */
    public HasMetaDataItem(Server server) {
        super(Token.HAS_META_STRING, server);
    }

    /**
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public final void doAction(InputStream in, OutputStream out, String clientIP) throws Exception {
        BigHash[] hashes = null;
        try {
            hashes = RemoteUtil.readBigHashArray(in);
        } catch (Exception e) {
            throw new TrancheProtocolException();
        }
        // verify the number of hashes is less than or equal to the allowed amount
        if (hashes.length > RemoteTrancheServer.BATCH_HAS_LIMIT) {
            throw new RuntimeException("Limit exceeded for number of items in a batch request. Requested items: " + hashes.length + ", Maximum Allowed: " + RemoteTrancheServer.BATCH_HAS_LIMIT);
        }
        // perform the task
        BooleanArrayCallback.writeResponse(server.getTrancheServer().hasMetaData(hashes), out);
    }

    /**
     * 
     * @param writeHeader
     * @param hashes
     * @param out
     * @throws java.lang.Exception
     */
    public final static void writeRequest(boolean writeHeader, BigHash[] hashes, OutputStream out) throws Exception {
        if (writeHeader) {
            RemoteUtil.writeBytes(Token.HAS_META, out);
        }
        RemoteUtil.writeBigHashArray(hashes, out);
    }
}
