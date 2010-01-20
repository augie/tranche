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
import org.tranche.logs.activity.Activity;
import org.tranche.server.GetActivityLogEntriesItem;
import org.tranche.util.IOUtil;

/**
 * <p>Represents an activity array server request.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ActivityArrayCallback extends RemoteCallback {

    public static final String NAME = "ActivityArrayCallback";
    private Activity[] list = null;

    /**
     * @param   rts                     the remote tranche server received
     * @param   id                      Unique identification number for the callback
     * @throws  InterruptedException    if an interrupted exception occurs
     */
    public ActivityArrayCallback(long id, RemoteTrancheServer rts, String description) {
        super(id, rts, NAME, description);
    }

    /**
     * <p>Returns the server response.</p>
     * @return
     * @throws java.lang.Exception
     */
    public Activity[] getResponse() throws Exception {
        waitForCallback();
        return list;
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
                list = GetActivityLogEntriesItem.readResponse(bais);
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
    public static void writeResponse(Activity[] response, OutputStream out) throws Exception {
        RemoteUtil.writeLine(Token.OK_STRING, out);
        // Write out total number of entries
        RemoteUtil.writeInt(response.length, out);
        // Write out each entry
        for (int i = 0; i < response.length; i++) {
            RemoteUtil.writeData(response[i].toByteArray(), out);
        }
    }

}
