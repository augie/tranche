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
package org.tranche.servers;

import org.tranche.remote.RemoteTrancheServerListener;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tranche.LocalDataServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.network.ConnectionEvent;
import org.tranche.network.ConnectionListener;
import org.tranche.network.ConnectionUtil;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.util.DebugUtil;

/**
 * <p>A lazy-loading utility class that helps track which servers are currently on-line.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ServerUtil {

    private static boolean debug = false;
    // listen to what's happening with the servers
    private final static Map<String, List<RemoteTrancheServerListener>> perUrlServerListeners = new HashMap<String, List<RemoteTrancheServerListener>>();
    private final static List<RemoteTrancheServerListener> allUrlServerListeners = new ArrayList<RemoteTrancheServerListener>();
    private static String overrideHost = null;


    static {
        ConnectionUtil.addListener(new ConnectionListener() {

            public void connectionMade(ConnectionEvent event) {
                for (RemoteTrancheServerListener l : getServerListeners(event.getHost())) {
                    ((RemoteTrancheServer) ConnectionUtil.getHost(event.getHost())).addListener(l);
                }
            }

            public void connectionDropped(ConnectionEvent event) {
            }
        });
    }

    /**
     * <p>Cannot instantiate.</p>
     */
    private ServerUtil() {
    }

    /**
     * <p>Adds a server listener to listen to all server communication.</p>
     * @param l
     */
    public static void addServerListener(RemoteTrancheServerListener l) {
        // add to the cached remote tranche servers
        for (String host : ConnectionUtil.getConnectedHosts()) {
            try {
                ((RemoteTrancheServer) ConnectionUtil.getHost(host)).addListener(l);
            } catch (Exception e) {
                debugErr(e);
            }
        }
        synchronized (allUrlServerListeners) {
            // add to our list of servers
            allUrlServerListeners.add(l);
        }
    }

    /**
     * <p>Adds a servers listener to listen only to the Tranche server with the given host name.</p>
     * @param host
     * @param l
     * @return
     */
    public static boolean addServerListener(String host, RemoteTrancheServerListener l) {
        // check if the remote tranche server is already cached
        if (ConnectionUtil.isConnected(host)) {
            try {
                ((RemoteTrancheServer) ConnectionUtil.getHost(host)).addListener(l);
            } catch (Exception e) {
                debugErr(e);
            }
        }
        synchronized (perUrlServerListeners) {
            if (perUrlServerListeners.get(host) == null) {
                perUrlServerListeners.put(host, new ArrayList<RemoteTrancheServerListener>());
            }
            return perUrlServerListeners.get(host).add(l);
        }
    }

    /**
     * <p>Returns the server listeners that are listening to the Tranche server with the given URL.</p>
     * @param url
     * @return A copy of the list of servers for a given URL.
     */
    public static List<RemoteTrancheServerListener> getServerListeners(String url) {
        List<RemoteTrancheServerListener> list = new ArrayList<RemoteTrancheServerListener>();
        synchronized (perUrlServerListeners) {
            if (perUrlServerListeners.get(url) != null) {
                for (RemoteTrancheServerListener l : perUrlServerListeners.get(url)) {
                    list.add(l);
                }
            }
        }
        synchronized (allUrlServerListeners) {
            for (RemoteTrancheServerListener l : allUrlServerListeners) {
                list.add(l);
            }
        }
        return list;
    }

    /**
     * <p>Removes the given server listener from the set of server listeners for the Tranche server with the given URL.</p>
     * @param host
     * @param l
     * @return
     */
    public static boolean removeServerListener(String host, RemoteTrancheServerListener l) {
        // remove from the cached remote tranche server
        if (ConnectionUtil.isConnected(host)) {
            try {
                ((RemoteTrancheServer) ConnectionUtil.getHost(host)).removeListener(l);
            } catch (Exception e) {
                debugErr(e);
            }
        }
        synchronized (perUrlServerListeners) {
            if (perUrlServerListeners.get(host) == null) {
                return false;
            }
            return perUrlServerListeners.get(host).remove(l);
        }
    }

    /**
     * <p>Removes all Tranche server listeners from the Tranche server with the given URL.</p>
     * @param host
     */
    public static void clearServerListeners(String host) {
        // clear listeners off the cached remote tranche server
        if (ConnectionUtil.isConnected(host)) {
            try {
                ((RemoteTrancheServer) ConnectionUtil.getHost(host)).clearListeners();
            } catch (Exception e) {
                debugErr(e);
            }
        }
        synchronized (perUrlServerListeners) {
            if (perUrlServerListeners.get(host) == null) {
                return;
            }
            perUrlServerListeners.get(host).clear();
        }
    }

    /**
     * Returns the IP addresss of this Tranche server. IP6 is largely ignored right now and will likely result in odd behavior.
     * @return Returns the IP address of this Tranche server.
     */
    public static String getHostName() {
        if (overrideHost != null) {
            return overrideHost;
        }

        try {
            // get the host -- no IPv6 for now?
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        // skip local host
                        String test = address.getHostAddress();
                        if (test.startsWith("127.0.0.1")) {
                            continue;
                        }
                        // return the first address found -- this won't work well for multi-addressed interfaces
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            throw new RuntimeException("Problem getting host name", ex);
        }
        return "127.0.0.1";
    }

    /**
     * 
     * @param host
     */
    public static void setHostName(String host) {
        overrideHost = host;
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        ServerUtil.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static final boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static final void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(ServerUtil.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param line
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
