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
package org.tranche.clc;

import org.tranche.ConfigureTranche;
import org.tranche.TrancheServer;
import org.tranche.logs.LogUtil;
import org.tranche.server.Server;
import org.tranche.streams.PrettyPrintStream;

/**
 * <p>Used for interaction with the Tranche server through a command-line interface.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ServerItem extends TrancheServerCommandLineClientItem {

    private static Server socketServer = null;
    private TrancheServer wrappedServer;

    /**
     * <p>Returns the Server object (socket server) to which this item belongs.</p>
     * @return  the server
     * 
     */
    public static Server getServer() {
        return socketServer;
    }

    /**
     * @param   wrappedServer     the tranche server           
     * @param   dfsclc  the tranche server command line client
     */
    public ServerItem(TrancheServer wrappedServer, TrancheServerCommandLineClient dfsclc) {
        super(wrappedServer, dfsclc, "server", "Manipulates server's connectivity.");
        this.wrappedServer = wrappedServer;
        addAttribute("command", "The command to run. Type 'server help' to see a list of the possible commands.", true);
        addAttribute("port", "The port to run the server on. Leave blank to use the default port (" + ConfigureTranche.get(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_PORT) + ").");
        addAttribute("secure", "If this server should tunnel traffic via SSL.");
    }

    /**
     * @param   in      the input stream
     * @param   out     the output stream
     */
    public void doAction(java.io.BufferedReader in, java.io.PrintStream out) {
        try {
            // required parameter
            String command = getParameter("command");
            // optional parameters
            String portString = getParameter("port");
            String secureString = getParameter("secure");

            // check for help
            if (command.equals("help")) {
                String status = "Offline";
                if (socketServer != null) {
                    status = "On-line and bound to port " + socketServer.getPort();
                }
                out.println("PDA Server Status: " + status);

                // print the commands
                out.println("Commands");
                // dashed line separator
                for (int i = 0; i < getTrancheServerCommandLineClient().getMaxLineLength() - 1; i++) {
                    out.print("-");
                }
                out.println();
                // set spacing
                if (out instanceof PrettyPrintStream) {
                    PrettyPrintStream pps = (PrettyPrintStream) out;
                    pps.setPadding(10);
                }
                out.println("start      Starts the server.");
                out.println("  port     Optional. The port to which the server should bind. Default is \"" + ConfigureTranche.get(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_PORT) + "\".");
                out.println("  secure   Optional. Whether the server should run over SSL. Default is \"" + ConfigureTranche.get(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_PORT)+ "\".");
                out.println();
                out.println("update     Change the connectivity of the server.");
                out.println("  port     Optional. The to which the server should bind. Default no change.");
                out.println("  secure   Optional. Whether the server should run over SSL. Default no change.");
                out.println();
                out.println("stop      Stops the server.");
                // set spacing
                if (out instanceof PrettyPrintStream) {
                    PrettyPrintStream pps = (PrettyPrintStream) out;
                    pps.setPadding(0);
                }
                out.println();
            } // if it is start
            else if (command.equals("start")) {
                // already running?
                if (socketServer != null && !socketServer.isStopped()) {
                    out.println("Server instance is already running.");
                    return;
                }

                int port = 0;
                boolean secure = false;
                // check for custom port
                if (portString != null) {
                    try {
                        port = Integer.parseInt(portString);
                    } catch (Exception e) {
                        out.println("Invalid port specified. " + portString + " is not a valid port number.");
                        LogUtil.logError(e);
                    }
                } else {
                    try {
                        port = Integer.parseInt(ConfigureTranche.get(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_PORT));
                    } catch (Exception e) {
                        out.println("Invalid default port.");
                    }
                }
                // try to update the secure flag
                if (secureString != null) {
                    try {
                        secure = Boolean.parseBoolean(secureString);
                    } catch (Exception e) {
                        out.println("Invalid value given for \"secure\" parameter. Valid parameters are \"true\" or \"false\".");
                        LogUtil.logError(e);
                    }
                } else {
                    try {
                        secure = Boolean.parseBoolean(ConfigureTranche.get(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_SSL));
                    } catch (Exception e) {
                        out.println("Invalid default SSL.");
                    }
                }

                // If user specifies host name, then try to bind to that
                socketServer = new Server(wrappedServer, port, secure);
                // start the server
                socketServer.start();
                out.println("Tranche server started, listening at " + socketServer.getURL());
            } // update port
            else if (command.equals("update")) {
                if (socketServer == null) {
                    out.println("No server instance is running.");
                    return;
                }

                int port = socketServer.getPort();
                boolean secure = socketServer.isSSL();
                if (portString != null) {
                    try {
                        port = Integer.parseInt(portString);
                    } catch (Exception e) {
                        out.println("Invalid port specified. " + portString + " is not a valid port number.");
                        return;
                    }
                }
                // try to update the secure flag
                if (secureString != null) {
                    try {
                        secure = Boolean.parseBoolean(secureString);
                    } catch (Exception e) {
                        out.println("Invalid value given for \"secure\" parameter. Valid parameters are \"true\" or \"false\".");
                        LogUtil.logError(e);
                        return;
                    }
                }

                if (port != socketServer.getPort() || secure != socketServer.isSSL()) {
                    socketServer.setRun(false);
                    socketServer = new Server(wrappedServer, port, secure);
                    socketServer.start();
                    out.println("Server now listening at " + socketServer.getURL() + ".");
                } else {
                    out.println("No changes made.");
                }
            } // handle stop requests
            else if (command.equals("stop")) {
                if (socketServer == null) {
                    out.println("No server instance is running.");
                    return;
                }

                // terminate thread -- TODO: graceful termination would be best
                socketServer.setRun(false);
                socketServer = null;
                out.println("Server terminated.");
            } else {
                out.println("Unrecognized command \"" + command + "\". Ignoring.");
            }
        } catch (Exception e) {
            out.println("Couldn't execute server command.");
            LogUtil.logError(e);
        }
    }
}
