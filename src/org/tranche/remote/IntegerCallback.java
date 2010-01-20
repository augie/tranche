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
import org.tranche.util.IOUtil;

/**
 * <p>Represents an integer server request.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class IntegerCallback extends RemoteCallback {

    public static final String NAME = "IntegerCallback";
    private int response;

    /**
     * @param   rts                     the remote tranche server received
     * @param   id                      Unique identification number for the callback
     * @throws  InterruptedException    if an interrupted exception occurs
     */
    public IntegerCallback(long id, RemoteTrancheServer rts, String description) {
        super(id, rts, NAME, description);
    }

    /**
     * <p>Returns the server response.</p>
     * @return
     * @throws java.lang.Exception
     */
    public int getResponse() throws Exception {
        waitForCallback();
        return response;
    }

    /**
     * <p>Sets the response from the server.</p>
     * @param bytes
     */
    public void callback(byte[] bytes) {
        try {
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(bytes);
                RemoteUtil.handledError(RemoteUtil.readLine(bais), bais);
                response = RemoteUtil.readInt(bais);
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
     * @param response
     * @param out
     * @throws java.lang.Exception
     */
    public static void writeResponse(int response, OutputStream out) throws Exception {
        RemoteUtil.writeLine(Token.OK_STRING, out);
        RemoteUtil.writeInt(response, out);
    }

}
