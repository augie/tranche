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
 * <p>Represents an error report request.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class CheckForErrorCallback extends RemoteCallback {

    public static final String NAME = "CheckForErrorCallback";

    /**
     * @param   rts                     the remote tranche server received
     * @param   id                      Unique Identification for the Callback
     * @throws  InterruptedException    if an interrupted exception occurs
     */
    public CheckForErrorCallback(long id, RemoteTrancheServer rts, String description) throws InterruptedException {
        super(id, rts, NAME, description);
    }

    /**
     * <p>Returns the server response.</p>
     * @throws java.lang.Exception
     */
    public void checkForError() throws Exception {
        waitForCallback();
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
                // see if the response was OK
                RemoteUtil.checkOK(bais);
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
     * @param out
     * @throws java.lang.Exception
     */
    public static void writeResponse(OutputStream out) throws Exception {
        RemoteUtil.writeLine(Token.OK_STRING, out);
    }
}
