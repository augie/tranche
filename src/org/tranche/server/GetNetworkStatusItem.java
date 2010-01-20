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
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTable;
import org.tranche.network.StatusTableRow;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.StatusTableCallback;
import org.tranche.remote.Token;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetNetworkStatusItem extends ServerItem {

    /**
     * <p>Use this as the first host and last host to return all of the rows.</p>
     */
    public static final String RETURN_ALL = "";

    /**
     * 
     * @param server
     */
    public GetNetworkStatusItem(Server server) {
        super(Token.GET_NETWORK_STATUS_PORTION_STRING, server);
    }

    /**
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public final void doAction(InputStream in, OutputStream out, String clientIP) throws Exception {
        int tableVersion, rowVersion;
        String fromHost, toHost;
        try {
            tableVersion = RemoteUtil.readInt(in);
            rowVersion = RemoteUtil.readInt(in);
            fromHost = RemoteUtil.readLine(in);
            toHost = RemoteUtil.readLine(in);
        } catch (Exception e) {
            throw new TrancheProtocolException();
        }
        // execute
        if ((fromHost == null || fromHost.equals(GetNetworkStatusItem.RETURN_ALL)) && (toHost == null || toHost.equals(GetNetworkStatusItem.RETURN_ALL))) {
            // return the entire status table
            StatusTableCallback.writeResponse(NetworkUtil.getStatus().toByteArray(tableVersion, rowVersion), out);
        } else {
            // return a subsection of the status table
            StatusTableCallback.writeResponse(NetworkUtil.getStatus().getStatus(fromHost, toHost).toByteArray(tableVersion, rowVersion), out);
        }
    }

    /**
     * 
     * @param writeHeader
     * @param out
     * @throws java.lang.Exception
     */
    public final static void writeRequest(boolean writeHeader, OutputStream out) throws Exception {
        writeRequest(writeHeader, null, null, out);
    }

    /**
     * 
     * @param writeHeader
     * @param fromHost
     * @param toHost
     * @param out
     * @throws java.lang.Exception
     */
    public final static void writeRequest(boolean writeHeader, String fromHost, String toHost, OutputStream out) throws Exception {
        if (writeHeader) {
            RemoteUtil.writeBytes(Token.GET_NETWORK_STATUS_PORTION, out);
        }
        RemoteUtil.writeInt(StatusTable.VERSION_LATEST, out);
        RemoteUtil.writeInt(StatusTableRow.VERSION_LATEST, out);
        if (fromHost == null) {
            fromHost = RETURN_ALL;
        }
        RemoteUtil.writeLine(fromHost, out);
        if (toHost == null) {
            toHost = RETURN_ALL;
        }
        RemoteUtil.writeLine(toHost, out);
    }
}
