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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.tranche.security.Signature;
import org.tranche.configuration.ConfigKeys;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.configuration.Configuration;
import org.tranche.configuration.ConfigurationUtil;
import org.tranche.exceptions.TrancheProtocolException;
import org.tranche.flatfile.NonceMap;
import org.tranche.util.IOUtil;

/**
 * <p>Handles requests for a server configuration.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetConfigurationItem extends ServerItem {

    /**
     * @param   server  the server received
     */
    public GetConfigurationItem(Server server) {
        super(Token.GET_CONFIGURATION_STRING, server);
    }

    /**
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public final void doAction(InputStream in, OutputStream out, String clientIP) throws Exception {
        byte[] nonce;
        Signature sig;
        try {
            nonce = RemoteUtil.readBytes(NonceMap.NONCE_BYTES, in);
            sig = RemoteUtil.readSignature(in);
        } catch (Exception e) {
            throw new TrancheProtocolException();
        }
        // execute it
        Configuration config = server.getTrancheServer().getConfiguration(sig, nonce);
        // add the number of currently connected users
        config.setValue(ConfigKeys.CURRENTLY_CONNECTED_USERS, Integer.toString(server.getConnectedUsers()));
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            // write out the configuration
            ConfigurationUtil.write(config, baos);
        } finally {
            IOUtil.safeClose(baos);
        }
        // write results
        RemoteUtil.writeLine(Token.OK_STRING, out);
        RemoteUtil.writeData(baos.toByteArray(), out);
        // log
        server.getSubmitter().logGetConfiguration(clientIP);
    }

    /**
     * 
     * @param writeHeader
     * @param nonce
     * @param signature
     * @param out
     * @throws java.lang.Exception
     */
    public final static void writeRequest(boolean writeHeader, byte[] nonce, Signature signature, OutputStream out) throws Exception {
        if (writeHeader) {
            RemoteUtil.writeBytes(Token.GET_CONFIGURATION, out);
        }
        RemoteUtil.writeBytes(nonce, out);
        RemoteUtil.writeSignature(signature, out);
    }
}
