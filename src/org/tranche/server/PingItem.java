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
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;

/**
 * <p>A simpler method for pinging the server. Simply sends back Token.OK.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class PingItem extends ServerItem {

    /**
     * @param   server  the server received
     */
    public PingItem(Server server) {
        super(Token.PING_STRING, server);
    }

    /**
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public final void doAction(InputStream in, OutputStream out, String clientIP) throws Exception {
        RemoteUtil.writeLine(Token.OK_STRING, out);
        RemoteUtil.writeLine(Token.OK_STRING, out);
    }

    /**
     * 
     * @param writeHeader
     * @param out
     * @throws java.lang.Exception
     */
    public static final void writeRequest(boolean writeHeader, OutputStream out) throws Exception {
        if (writeHeader) {
            RemoteUtil.writeBytes(Token.PING, out);
        }
    }
}
