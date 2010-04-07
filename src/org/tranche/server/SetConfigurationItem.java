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
import org.tranche.security.Signature;
import org.tranche.flatfile.NonceMap;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;

/**
 * <p>Handles requests to save server configuration.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SetConfigurationItem extends ServerItem {

    /**
     * @param   server  the server
     */
    public SetConfigurationItem(Server server) {
        super(Token.SET_CONFIGURATION_STRING, server);
    }

    /**
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public void doAction(InputStream in, OutputStream out, String clientIP) throws Exception {
        byte[] data;
        Signature sig;
        byte[] nonce;
        try {
            data = RemoteUtil.readDataBytes(in);
            sig = RemoteUtil.readSignature(in);
            nonce = RemoteUtil.readBytes(NonceMap.NONCE_BYTES, in);
        } catch (Exception e) {
            throw new TrancheProtocolException();
        }

        // execute the set
        server.getTrancheServer().setConfiguration(data, sig, nonce);
        RemoteUtil.writeLine(Token.OK_STRING, out);
    }

    /**
     * 
     * @param writeHeader
     * @param bytes
     * @param signature
     * @param nonce
     * @param out
     * @throws java.lang.Exception
     */
    public static void writeRequest(boolean writeHeader, byte[] bytes, Signature signature, byte[] nonce, OutputStream out) throws Exception {
        if (writeHeader) {
            RemoteUtil.writeBytes(Token.SET_CONFIGURATION, out);
        }
        RemoteUtil.writeData(bytes, out);
        RemoteUtil.writeSignature(signature, out);
        RemoteUtil.writeBytes(nonce, out);
    }
}
