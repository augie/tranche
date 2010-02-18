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

import java.util.HashSet;
import java.util.Set;
import org.tranche.util.DebugUtil;

/**
 * <p>Encapsulates information for ServerStatusUpdateProcess so can view from perspective on any server--not just the localhost server.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ServerStatusTablePerspective {

    private static boolean debug = false;
    /**
     * <p>The host name (or IP address) for the server from which we wish to gather the status table info. (I.e. from this server's perspective.)</p>
     */
    public final String host;
    /**
     * <p>The status table row ranges for core servers.</p>
     */
    public final Set<StatusTableRowRange> coreServers;
    /**
     * <p>The status table row ranges for non-core servers.</p>
     */
    public final Set<StatusTableRowRange> nonCoreServers;

    /**
     * <p>Encapsulates information for ServerStatusUpdateProcess so can view from perspective on any server--not just the localhost server.</p>
     * @param host The host name (or IP address) for the server from which we wish to gather the status table info. (I.e. from this server's perspective.)
     */
    public ServerStatusTablePerspective(String host) {
        this.host = host;
        this.coreServers = new HashSet();
        this.nonCoreServers = new HashSet();
    }

    /**
     * <p>Returns all the core servers to which a particular server is connected.</p>
     * @return Set of hosts 
     */
    public Set<String> getConnectedCoreServerHosts() {
        Set<String> connectedHosts = new HashSet();

        synchronized (coreServers) {
            for (StatusTableRowRange row : this.coreServers) {
                connectedHosts.add(row.getConnectionHost());
            }
        }

        return connectedHosts;
    }

    /**
     * <p>Add range for non-core servers.</p>
     * @param range
     * @return
     */
    public boolean addCoreServerRowRange(StatusTableRowRange range) {
        synchronized (coreServers) {
            return coreServers.add(range);
        }
    }

    /**
     * <p>Add range for non-core servers.</p>
     * @param range
     * @return
     */
    public boolean addNonCoreServerRowRange(StatusTableRowRange range) {
        synchronized (nonCoreServers) {
            return nonCoreServers.add(range);
        }
    }

    @Override()
    public String toString() {
        StringBuffer buf = new StringBuffer();
        System.out.println("ServerStatusTablePerspective: " + host);

        // Core servers
        synchronized (coreServers) {
            if (coreServers.size() == 0) {
                System.out.println("    * 0 core servers");
            } else {
                System.out.println("    * " + coreServers.size() + " core server range(s):");
                for (StatusTableRowRange range : coreServers) {
                    System.out.println("        + " + range);
                }
            }
        }

        // Non-core servers
        synchronized (nonCoreServers) {
            if (nonCoreServers.size() == 0) {
                System.out.println("    * 0 non-core servers");
            } else {
                System.out.println("    * " + nonCoreServers.size() + " non-core server(s):");
                for (StatusTableRowRange range : nonCoreServers) {
                    System.out.println("        + " + range);
                }
            }
        }

        return buf.toString();
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        ServerStatusTablePerspective.debug = debug;
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
            DebugUtil.printOut(ServerStatusTablePerspective.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
