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
import org.tranche.TrancheServer;
import org.tranche.exceptions.TrancheProtocolException;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTable;
import org.tranche.network.StatusTableRow;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.util.IOUtil;

/**
 * <p>Handles registering new or modified server URLs.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class RegisterServerItem extends ServerItem {

    /**
     * @param   server  the server received
     */
    public RegisterServerItem(Server server) {
        super(Token.REGISTER_SERVER_STRING, server);
    }

    /**
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public final void doAction(InputStream in, OutputStream out, String clientIP) throws Exception {
        String url;
        try {
            url = RemoteUtil.readLine(in);
        } catch (Exception e) {
            throw new TrancheProtocolException();
        }

        // parse info
        final boolean ssl = IOUtil.parseSecure(url);
        final String host = IOUtil.parseHost(url);
        final int port = IOUtil.parsePort(url);

        // is this different than what we know already?
        boolean goOn = false;
        // new host
        if (NetworkUtil.isBannedServer(host)) {
            goOn = false;
        } else if (!NetworkUtil.getStatus().contains(host)) {
            goOn = true;
        } // check the row
        else {
            StatusTableRow row = NetworkUtil.getStatus().getRow(host);
            // ssl, port, or online status
            if (row.isSSL() != ssl || row.getPort() != port || !row.isOnline()) {
                goOn = true;
            }
        }

        // different -- verify the modification
        if (goOn) {
            Thread t = new Thread("Registering " + url) {

                @Override
                public void run() {
                    // need to verify the suggestion -- the registration may be malicious
                    try {
                        TrancheServer ts = ConnectionUtil.connect(host, port, ssl, true);
                        if (ts == null) {
                            return;
                        }
                        try {
                            NetworkUtil.updateRow(ts.getNetworkStatusPortion(host, host).getRow(host));
                        } finally {
                            ConnectionUtil.unlockConnection(host);
                        }
                    } catch (Exception e) {
                        ConnectionUtil.reportExceptionHost(host, e);
                    }
                }
            };
            t.start();
        }

        // write response here to so registering server does not have to wait
        RemoteUtil.writeLine(Token.OK_STRING, out);
    }

    /**
     * 
     * @param writeHeader
     * @param url
     * @param out
     * @throws java.lang.Exception
     */
    public static final void writeRequest(boolean writeHeader, String url, OutputStream out) throws Exception {
        if (writeHeader) {
            RemoteUtil.writeBytes(Token.REGISTER_SERVER, out);
        }
        RemoteUtil.writeLine(url, out);
    }
}
