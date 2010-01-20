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
package org.tranche.remote;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;

/**
 * <p>Represents a hash array server request.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetHashesCallback extends RemoteCallback {

    public static final String NAME = "GetHashesCallback";
    private BigHash[] toReturn = null;

    /**
     * @param   rts                     the remote tranche server received
     * @param   id                      Unique Identification for the Callback
     * @throws  InterruptedException    if an interrupted exception occurs
     */
    public GetHashesCallback(long id, RemoteTrancheServer rts, String description) {
        super(id, rts, NAME, description);
    }

    /**
     * <p>Returns the server response.</p>
     * @return
     * @throws java.lang.Exception
     */
    public BigHash[] getHashes() throws Exception {
        waitForCallback();
        return toReturn;
    }

    /**
     * <p>Sets the response from the server.</p>
     * @param bs
     */
    public void callback(byte[] bs) {
        try {
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(bs);
                RemoteUtil.handledError(RemoteUtil.readLine(bais), bais);
                toReturn = RemoteUtil.readBigHashArray(bais);
            } finally {
                IOUtil.safeClose(bais);
            }
        } catch (Exception e) {
            setCachedException(e);
        } finally {
            notifyWaiting();
        }
    }

    /**
     * 
     * @param hashes
     * @param out
     * @throws java.lang.Exception
     */
    public static void writeResponse(BigHash[] hashes, OutputStream out) throws Exception {
        RemoteUtil.writeLine(Token.OK_STRING, out);
        RemoteUtil.writeBigHashArray(hashes, out);
    }
}
