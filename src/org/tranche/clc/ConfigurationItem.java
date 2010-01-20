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

import java.util.HashSet;
import java.util.Set;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.hash.span.HashSpan;
import org.tranche.configuration.Configuration;
import org.tranche.configuration.ServerModeFlag;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.flatfile.ServerConfiguration;
import org.tranche.hash.span.AbstractHashSpan;
import org.tranche.logs.LogUtil;
import org.tranche.streams.PrettyPrintStream;
import org.tranche.users.User;
import org.tranche.users.UserCertificateUtil;

/**
 * <p>Method to encapsulate the action of printing out information from a server's Configuration object.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ConfigurationItem extends TrancheServerCommandLineClientItem {

    private FlatFileTrancheServer ts;

    /**
     * @param   ts   the flat file DFS received
     * @param   clc  the DFS command line client received
     */
    public ConfigurationItem(TrancheServer ts, TrancheServerCommandLineClient clc) {
        super(ts, clc, "configuration", "Manipulates the server's configuration parameters.");
        addAttribute("command", "The command to run. Type 'configuration help' to see a list of the possible commands.", true);
        addAttribute("name", "The name given to the server.");
        addAttribute("group", "The name of the group to which the server belongs.");
        addAttribute("mode", "Valid values include: none, read, write, read-write.");
        addAttribute("hashspans", "The number of hash spans to be entered.");
        this.ts = (FlatFileTrancheServer)ts;
    }

    /** 
     * <p>Show information about the Configuration.</p>
     * @param   in      the input buffered reader 
     * @param   out     the ouput print stream
     */
    public void doAction(java.io.BufferedReader in, java.io.PrintStream out) {
        try {
            // required parameter
            String command = getParameter("command");
            // optional parameters
            String nameString = getParameter("name");
            String groupString = getParameter("group");
            String modeString = getParameter("mode");
            String hashSpanString = getParameter("hashspans");

            // check for help
            if (command.equals("help")) {
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
                out.println("show               Shows all the configuration parameters.");
                out.println();
                out.println("update             Shows all the configuration parameters.");
                out.println("  name             Optional. The name given to the server.");
                out.println("  group            Optional. The name of the group to which the server belongs.");
                out.println("  mode             Optional. Valid values include: none, read, write, read-write.");
                out.println("  hashspans        Optional. The number of hash spans to input.");
                // set spacing
                if (out instanceof PrettyPrintStream) {
                    PrettyPrintStream pps = (PrettyPrintStream) out;
                    pps.setPadding(0);
                }
                out.println();
            } // if it is start
            else if (command.equals("show")) {
                // get the config
                Configuration config = ts.getConfiguration();

                // list the version
                out.println("-------------------------------------------------------------------------------");
                out.println("---------------------- Configuration Information ------------------------------");
                out.println("-------------------------------------------------------------------------------");
                out.println("");

                out.println("Data Directories:");
                out.println("");
                Set<DataDirectoryConfiguration> ddcs = config.getDataDirectories();
                for (DataDirectoryConfiguration ddc : ddcs) {
                    out.println("  Directory: " + ddc.getDirectory());
                    out.println("    size limit: " + ddc.getSizeLimit());
                    out.println("    actual size: " + ddc.getActualSize());
                }
                out.println("");

                out.println("Server Configurations:");
                out.println("");
                Set<ServerConfiguration> scs = config.getServerConfigs();
                for (ServerConfiguration sc : scs) {
                    out.println("  ServerConfiguration:");
                    out.println("    host name: " + sc.getHostName());
                    out.println("    port: " + sc.getPort());
                    out.println("    type: " + sc.getType());
                }
                out.println("");

                out.println("Hash Spans:");
                out.println("");
                for (HashSpan hs : config.getHashSpans()) {
                    out.println("  HashSpan:");
                    out.println("    first: " + hs.getFirst());
                    out.println("    last: " + hs.getLast());
                }
                out.println("");

                out.println("Target Hash Spans:");
                out.println("");
                for (HashSpan hs : config.getTargetHashSpans()) {
                    out.println("  HashSpan:");
                    out.println("    first: " + hs.getFirst());
                    out.println("    last: " + hs.getLast());
                }
                out.println("");

                out.println("Recognized Users:");
                out.println("");
                Set<User> users = config.getUsers();
                for (User u : users) {
                    out.println("  X.509 Subject DN: " + UserCertificateUtil.readUserName(u.getCertificate()));
                    out.println("    can add data: " + u.canSetData());
                    out.println("    can add meta-data: " + u.canSetMetaData());
                    out.println("    can delete data: " + u.canDeleteData());
                    out.println("    can delete meta-data: " + u.canDeleteMetaData());
                    out.println("    can get configuration: " + u.canGetConfiguration());
                    out.println("    can set configuration: " + u.canSetConfiguration());
                }
                out.println("");

                out.println("Key-Value Pairs:");
                out.println("");
                out.println("  #: " + config.numberKeyValuePairs());
                out.println("");
                Set<String> keys = config.getValueKeys();
                for (String key : keys) {
                    out.println("  " + key + ": " + config.getValue(key));
                }
                out.println("");

                out.println("-------------------------------------------------------------------------------");
            } else if (command.equals("update")) {
                // get the config
                Configuration config = ts.getConfiguration().clone();

                // read only
                boolean changeMade = false;
                if (nameString != null) {
                    try {
                        config.setValue(ConfigKeys.NAME, nameString);
                        changeMade = true;
                    } catch (Exception e) {
                    }
                }
                if (groupString != null) {
                    try {
                        config.setValue(ConfigKeys.GROUP, groupString);
                        changeMade = true;
                    } catch (Exception e) {
                    }
                }

                if (modeString != null) {
                    try {
                        if (modeString.toLowerCase().equals("read")) {
                            config.setValue(ConfigKeys.SERVER_MODE_FLAG_ADMIN, String.valueOf(ServerModeFlag.CAN_READ));
                        } else if (modeString.toLowerCase().equals("write")) {
                            config.setValue(ConfigKeys.SERVER_MODE_FLAG_ADMIN, String.valueOf(ServerModeFlag.CAN_WRITE));
                        } else if (modeString.toLowerCase().equals("read-write")) {
                            config.setValue(ConfigKeys.SERVER_MODE_FLAG_ADMIN, String.valueOf(ServerModeFlag.CAN_READ_WRITE));
                        } else if (modeString.toLowerCase().equals("none")) {
                            config.setValue(ConfigKeys.SERVER_MODE_FLAG_ADMIN, String.valueOf(ServerModeFlag.NONE));
                        } else {
                            throw new Exception("Unrecognized parameter value: " + modeString);
                        }
                        changeMade = true;
                    } catch (Exception e) {
                        out.println("Invalid value given for \"mode\" parameter. Type \"configuration help\" for more information.");
                    }
                }
                if (hashSpanString != null) {
                    try {
                        int hashSpanCount = Integer.valueOf(hashSpanString);
                        out.println("Input " + hashSpanCount + " hash spans:");
                        Set<HashSpan> hashSpans = new HashSet<HashSpan>();
                        for (int i = 0; i < hashSpanCount; i++) {
                            try {
                                out.println(" Hash " + i + " start hash (0-65536): ");
                                out.flush();
                                int first = Integer.valueOf(in.readLine().trim());
                                out.println(" Hash " + i + " end hash (0-65536): ");
                                out.flush();
                                int last = Integer.valueOf(in.readLine().trim());
                                hashSpans.add(new AbstractHashSpan(first, last));
                            } catch (Exception e) {
                                out.println("Invalid integer entered.");
                            }
                        }
                        config.setTargetHashSpans(hashSpans);
                        changeMade = true;
                    } catch (Exception e) {
                        out.println("Invalid value given for \"hashspan\" parameter. Type \"configuration help\" for more information.");
                    }
                }

                if (changeMade) {
                    // save change
                    ts.setConfiguration(config);
                    out.println("Configuration changes saved.");
                } else {
                    out.println("No change made.");
                }
            } else {
                out.println("Unrecognized command \"" + command + "\". Ignoring.");
            }
        } catch (Exception e) {
            out.println("Couldn't execute configuration command.");
        }
    }
}
