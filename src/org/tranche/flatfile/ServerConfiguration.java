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
package org.tranche.flatfile;

import org.tranche.annotations.Todo;

/**
 * <p>Store information about the Server instance in a configuration object.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class ServerConfiguration implements Comparable {
    
//    public static final String HTTPS = "HTTPS";
    
    /**
     * <p>Get the protocol: HTTP</p>
     */
    public static final String HTTP = "HTTP";
    /**
     * <p>Get the protocol: DFS</p>
     */
    public static final String DFS = "DFS";
    /**
     * <p>Get the protocol: DFS+SSL</p>
     */
    @Todo(desc="Why is this value indistinguishable from ServerConfiguration#DFS", day=17, month=2, year=2009, author="Bryan Smith")
    public static final String DFS_SSL = "DFS";
    // local references
    private int port;
    private String type, hostName;

    /**
     * 
     * @param type
     * @param port
     * @param hostName
     */
    public ServerConfiguration(String type, int port, String hostName) {
        this.type = type;
        this.port = port;
        this.setHostName(hostName);
    }

    /**
     * <p>Get the port associated with this server.</p>
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * <p>Set the port associated with this server.</p>
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * <p>Get the type (protocol) associated with this server.</p>
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * <p>Set the type (protocol) associated with this server.</p>
     * @param type
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override()
    public boolean equals(Object o) {
        if (o instanceof ServerConfiguration) {
            ServerConfiguration a = (ServerConfiguration) o;
            return a.getType().equals(getType()) && a.getPort() == getPort();
        }
        return super.equals(o);
    }

    @Override()
    public int hashCode() {
        return new String(getType() + "," + getPort()).hashCode();
    }

    /**
     * <p>Get the host name associated with this server.</p>
     * @return
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * <p>Set the host name associated with this server.</p>
     * @param hostName
     */
    public void setHostName(String hostName) {
        if (hostName == null) {
            this.hostName = "";
        } else {
            this.hostName = hostName;
        }
    }

    public int compareTo(Object o) {
        ServerConfiguration sc = (ServerConfiguration) o;
        String a = getType() + ":" + getPort() + ":" + getHostName();
        String b = sc.getType() + ":" + sc.getPort() + ":" + sc.getHostName();
        return a.compareTo(b);
    }

    /**
     * <p>Clone this object.</p>
     * @return
     */
    @Override()
    public ServerConfiguration clone() {
        return new ServerConfiguration(this.type, this.port, this.hostName);
    }
}
