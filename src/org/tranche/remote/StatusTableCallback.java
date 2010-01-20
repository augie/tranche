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
import org.tranche.network.StatusTable;
import org.tranche.util.IOUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class StatusTableCallback extends RemoteCallback {

    public static final String NAME = "StatusTableCallback";
    private static StatusTable status;

    /**
     * 
     * @param id
     * @param rts
     * @param description
     */
    public StatusTableCallback(long id, RemoteTrancheServer rts, String description) {
        super(id, rts, NAME, description);
    }

    /**
     * 
     * @return
     * @throws java.lang.Exception
     */
    public StatusTable getResponse() throws Exception {
        waitForCallback();
        return status;
    }

    /**
     * 
     * @param bytes
     */
    public void callback(byte[] bytes) {
        try {
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(bytes);
                RemoteUtil.handledError(RemoteUtil.readLine(bais), bais);
                status = new StatusTable(bais);
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
     * @param statusTable
     * @param out
     * @throws java.lang.Exception
     */
    public static void writeResponse(byte[] statusTable, OutputStream out) throws Exception {
        RemoteUtil.writeLine(Token.OK_STRING, out);
        RemoteUtil.writeBytes(statusTable, out);
    }
}
