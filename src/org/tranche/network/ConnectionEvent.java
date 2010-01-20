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
package org.tranche.network;

import org.tranche.time.TimeUtil;

/**
 * <p>Wraps the information describing a connection modification.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ConnectionEvent {

    /**
     * <p>The value for the port when the event represents a dropped connection.</p>
     */
    public static final int DROPPED_PORT = 0;
    /**
     * <p>The SSL value for when the event represents a dropped connection.</p>
     */
    public static final boolean DROPPED_SSL = false;
    private final String host;
    private final int port;
    private final boolean ssl;
    private final long timestamp = TimeUtil.getTrancheTimestamp();

    /**
     * @param host A host name
     * @param port A port number
     * @param ssl Whether the server communicates over SSL
     */
    public ConnectionEvent(String host, int port, boolean ssl) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
    }

    /**
     * @param host A host name
     */
    public ConnectionEvent(String host) {
        this(host, DROPPED_PORT, DROPPED_SSL);
    }

    /**
     * <p>Gets the host name of the server.</p>
     * @return The host name of the server.
     */
    public String getHost() {
        return host;
    }

    /**
     * <p>Returns the port number over which the server communicates.</p>
     * @return The port number over which the server communicates.
     */
    public int getPort() {
        return port;
    }

    /**
     * <p>Returns whether the host in question communicates over SSL.</p>
     * @return Whether the host in question communicates over SSL
     */
    public boolean isSSL() {
        return ssl;
    }

    /**
     * <p>Gets the timestamp the event occurred.</p>
     * @return The timestamp the event occurred.
     */
    public long getTimestamp() {
        return timestamp;
    }
}
