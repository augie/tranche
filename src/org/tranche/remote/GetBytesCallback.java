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
 * <p>Represents a byte array request.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class GetBytesCallback extends RemoteCallback {

    public static final String NAME = "GetBytesCallback";
    private byte[] bytes = null;

    /**
     * 
     * @param id
     * @param rts
     * @param description
     */
    public GetBytesCallback(long id, RemoteTrancheServer rts, String description) {
        super(id, rts, NAME, description);
    }

    /**
     * <p>Returns the server response.</p>
     * @return
     * @throws java.lang.Exception
     */
    public byte[] getBytes() throws Exception {
        waitForCallback();
        // check for null
        if (bytes == null) {
            throw new Exception("Why are bytes null? (was purged?: " + isPurged() + "; cached exception?: " + (getCachedException() != null ? getCachedException().getClass().getSimpleName() + ": " + getCachedException().getMessage() : " none") + "; is complete? :" + (this.isComplete() ? "no" : "yes") + ")");
        }
        // return the bytes
        return bytes;
    }

    /**
     * <p>Sets the response from the server.</p>
     * @param callbackBytes
     */
    public synchronized void callback(byte[] callbackBytes) {
        try {
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(callbackBytes);
                RemoteUtil.handledError(RemoteUtil.readLine(bais), bais);
                bytes = RemoteUtil.readDataBytes(bais);
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
     * @param bytes
     * @param out
     * @throws java.lang.Exception
     */
    public static void writeResponse(byte[] bytes, OutputStream out) throws Exception {
        RemoteUtil.writeLine(Token.OK_STRING, out);
        RemoteUtil.writeData(bytes, out);
    }
}
