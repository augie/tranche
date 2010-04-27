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
import java.math.BigInteger;
import org.tranche.exceptions.TrancheProtocolException;
import org.tranche.remote.GetHashesCallback;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;

/**
 * <p>Handles requests for project file hashes.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetProjectHashesItem extends ServerItem {

    /**
     * @param   server  the server received
     */
    public GetProjectHashesItem(Server server) {
        super(Token.GET_PROJECT_HASHES_STRING, server);
    }

    /**
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public final void doAction(InputStream in, OutputStream out, String clientIP) throws Exception {
        BigInteger offset, length;
        try {
            offset = BigInteger.valueOf(RemoteUtil.readLong(in));
            length = BigInteger.valueOf(RemoteUtil.readLong(in));
        } catch (Exception e) {
            throw new TrancheProtocolException();
        }
        // write out the hashes
        GetHashesCallback.writeResponse(server.getTrancheServer().getProjectHashes(offset, length), out);
    }

    /**
     * 
     * @param writeHeader
     * @param offset
     * @param length
     * @param out
     * @throws java.lang.Exception
     */
    public static final void writeRequest(boolean writeHeader, long offset, long length, OutputStream out) throws Exception {
        if (writeHeader) {
            RemoteUtil.writeBytes(Token.GET_PROJECT_HASHES, out);
        }
        RemoteUtil.writeLong(offset, out);
        RemoteUtil.writeLong(length, out);
    }
}
