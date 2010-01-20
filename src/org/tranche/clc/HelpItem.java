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

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.Iterator;
import org.tranche.TrancheServer;
import org.tranche.streams.PrettyPrintStream;

/**
 * This is a generic help item. It will show a description and list any 
 * parameters that are available for a command.
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class HelpItem extends TrancheServerCommandLineClientItem {

    /**
     * @param   dfs     the Tranche server received
     * @param   dfsclc  the Tranche server command line client received
     */
    public HelpItem(TrancheServer dfs, TrancheServerCommandLineClient dfsclc) {
        super(dfs, dfsclc, "help", "Shows details, including attributes, about a given command.");
        addAttribute("command", "The command that you'd like help with.", true);
    }

    /**
     * Parse from the input buffer the required information to fill the help item.
     * @param   in      the buffered reader
     * @param   out     the print stream
     */
    public void doAction(BufferedReader in, PrintStream out) {
        try {
            // find the command
            String command = getParameter("command");
            for (Iterator it = tsclc.items.keySet().iterator(); it.hasNext();) {
                String itemName = (String) it.next();
                TrancheServerCommandLineClientItem item = (TrancheServerCommandLineClientItem) tsclc.items.get(itemName);

                // check the name
                if (item.getName().startsWith(command)) {
                    // show the info
                    out.println("Help for command: " + item.getName());
                    printSpacerLine(out);
                    out.println("Description:");
                    printSpacerLine(out);
                    out.println("  " + item.getDescription());
                    printSpacerLine(out);

                    // show info about all the attributes
                    if (item.attributes.size() > 0) {
                        out.println("Attributes: ");
                        // get the length of the longest attribute
                        int longestAttribute = 0;
                        // iterate over the attributes
                        for (Iterator ai = item.attributes.keySet().iterator(); ai.hasNext();) {
                            String key = (String) ai.next();
                            if (key.length() > longestAttribute) {
                                longestAttribute = key.length();
                            }
                        }

                        // set the pad between name and description
                        int pad = 4;
                        // set the spacer
                        if (out instanceof PrettyPrintStream) {
                            PrettyPrintStream pps = (PrettyPrintStream) out;
                            pps.setPadding(longestAttribute + pad + 2);
                        }

                        // iterate over the attributes
                        for (Iterator ai = item.attributes.keySet().iterator(); ai.hasNext();) {
                            String key = (String) ai.next();
                            TrancheServerCommandLineClientItemAttribute attribute = (TrancheServerCommandLineClientItemAttribute) item.attributes.get(key);
                            out.print("  " + attribute.getName());
                            // pad accordingly
                            for (int i = 0; i < longestAttribute - attribute.getName().length() + pad; i++) {
                                out.print(" ");
                            }
                            if (attribute.isRequired()) {
                                out.print("[Required]");
                            }
                            out.println(attribute.getDescription());
                            out.println();
                        }

                        // reset the spacer
                        if (out instanceof PrettyPrintStream) {
                            PrettyPrintStream pps = (PrettyPrintStream) out;
                            pps.setPadding(0);
                        }
                        printSpacerLine(out);

                    }
                }
            }
        } catch (Exception e) {
        }
    }
}
