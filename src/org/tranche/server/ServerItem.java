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

/**
 * <p>Abstraction of an action the server can handle.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public abstract class ServerItem {

    /**
     * <p>The name of the command.</p>
     */
    protected String commandName;
    /**
     * <p>Keeps a reference to the Server object to which this item belongs.</p>
     */
    protected Server server;

    /**
     * @param   commandName     the command name received 
     * @param   server          the server received
     */
    public ServerItem(String commandName, Server server) {
        this.commandName = commandName;
        this.server = server;
    }

    /**
     * <p>Returns the name of the command.</p>
     * @return  the command name
     */
    public String getName() {
        return commandName;
    }

    /**
     * <p>Handle the server's action.<p>
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public abstract void doAction(InputStream in, OutputStream out, String clientIP) throws Exception;
}
