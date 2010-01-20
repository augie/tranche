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

import java.io.*;
import java.util.*;
import java.util.LinkedList;
import org.tranche.TrancheServer;

/**
 * Class that implements a Client Item for the Trancher server CLC.
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public abstract class TrancheServerCommandLineClientItem implements Comparable {

    String description;
    String name;
    TrancheServer dfs;
    TrancheServerCommandLineClient tsclc;
    HashMap attributes = new HashMap();
    // a map for all the values
    Map<String, String> values = Collections.synchronizedMap(new HashMap<String, String>());

    /** 
     * Creates a new instance of the command line client item.
     *
     * @param   dfs             The DistributedFileSystem implementation that will be
     *                          manipulated by this code. 
     * @param   dfsclc          the command line client received
     * @param   name            the command line client item name received
     * @param   description     the command line client item description received
     *
     */
    public TrancheServerCommandLineClientItem(TrancheServer dfs, TrancheServerCommandLineClient dfsclc, String name, String description) {
        this.name = name;
        this.description = description;
        this.dfs = dfs;
        this.tsclc = dfsclc;
    }

    /**
     * Retrieve DistributedFileSystem that is being handled by this CLC.
     * @return  The distributed file system manipulated by this code.
     *
     */
    public TrancheServer getTrancheServer() {
        return dfs;
    }

    /**
     * Retrieve the TracheServerCommandLineClient that is being handled by this CLC.
     * @return  the distributed file system command line client
     *
     */
    public TrancheServerCommandLineClient getTrancheServerCommandLineClient() {
        return tsclc;
    }

    /**
     * Retrieve name of CLC.
     * @return  the name of the command line client
     *
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieve description of CLC.
     * @return  the description of the command line client
     * 
     */
    public String getDescription() {
        return description;
    }

    /**
     * Populate CLC with opetions from input buffer.
     * @param   commandLine     the command line of parameters     
     * @param   in              the input buffered reader
     * @param   out             the output print stream
     *
     */
    public void preDoAction(String commandLine, BufferedReader in, PrintStream out) {
        // clear the values of all attributes
        values.clear();

        // populate the attributes
        String[] parts = splitCommandLine(commandLine);
        for (Iterator it = attributes.values().iterator(); it.hasNext();) {
            TrancheServerCommandLineClientItemAttribute attribute = (TrancheServerCommandLineClientItemAttribute) it.next();
            for (int i = 1; i < parts.length; i++) {
                // check if this is a valid param
                if (!parts[i].startsWith(attribute.getName() + "=")) {
                    continue;
                }
                // get the value
                String value = parts[i].substring(attribute.getName().length() + 1);
                values.put(attribute.getName().trim().toLowerCase(), value);
            }
        }

        // check for the required options
        for (Iterator it = attributes.values().iterator(); it.hasNext();) {
            TrancheServerCommandLineClientItemAttribute attribute = (TrancheServerCommandLineClientItemAttribute) it.next();
            // condition on required flag
            if (attribute.isRequired() && getParameter(attribute.getName()) == null) {
                out.println("You must supply a value for the \"" + attribute.getName() + "\" attribute. Type \"help " + getName() + "\" for a complete list of attributes.");
                return;
            }
        }

        // invoke doAction
        doAction(in, out);
    }

    /**
     * Retrieve parameter, asa string, given the parameter's name.
     * @param   name    the attribute's name
     * @return          the value of the named attribute
     *   
     */
    public String getParameter(String name) {
        return values.get(name);
    }

    /**
     * Abstract class for doAction.
     * @param   in      the input buffered reader
     * @param   out     the output print stream
     *
     */
    public abstract void doAction(BufferedReader in, PrintStream out);

    /**
     * Add attribute and description to CLC.
     * @param   name            the name of the attribute to be added
     * @param   description     the description of the atribute to be added
     * since                    1.0  
     */
    public void addAttribute(String name, String description) {
        attributes.put(name, new TrancheServerCommandLineClientItemAttribute(name, description));
    }

    /**
     * Add attribute, description, and marking whether it is a required attribute
     * for the CLC.
     * @param   name            the name of the attribute to be added 
     * @param   description     the description of the attribute to be added
     * @param   required        the required flag
     *
     */
    public void addAttribute(String name, String description, boolean required) {
        attributes.put(name, new TrancheServerCommandLineClientItemAttribute(name, description, required));
    }

    /**
     * Retrieve all attributes of the CLC.
     * @return  the command line client's attributes
     * 
     */
    public TrancheServerCommandLineClientItemAttribute[] getAttributes() {
        return (TrancheServerCommandLineClientItemAttribute[]) attributes.values().toArray(new TrancheServerCommandLineClientItemAttribute[attributes.size()]);
    }

    /**
     * Split the string of command line parameters into a array of of strings
     * for each option.
     * @param   commandLine     the command line of parameters 
     * @return                  the good parameters
     *
     */
    public static final String[] splitCommandLine(String commandLine) {
        // buffer all command line parts
        LinkedList<String> partBuf = new LinkedList<String>();

        // split first by quotes
        String[] quoteSplit = commandLine.split("\"");
        for (int quoteCount = 0; quoteCount < quoteSplit.length; quoteCount++) {
            // odd sections must have been quoted
            if (quoteCount % 2 == 1) {
                partBuf.add(quoteSplit[quoteCount]);
            } // non-quoted parts are treated normally
            else {
                // split to parts
                String[] optionParts = quoteSplit[quoteCount].split(" ");
                // buffer out misc whitespace
                for (int i = 0; i < optionParts.length; i++) {
                    if (!optionParts[i].trim().equals("")) {
                        partBuf.add(optionParts[i].trim());
                    }
                }
            }
        }

        // tack on equals signs to the front of parameters
        String isAttribute = null;
        LinkedList<String> finalParts = new LinkedList<String>();
        for (Iterator<String> it = partBuf.iterator(); it.hasNext();) {
            String part = it.next();
            // if it is an attribute tack on the '='
            if (isAttribute != null) {
                finalParts.add(isAttribute + part);
                isAttribute = null;
            } else {
                // handle equals
                if (part.endsWith("=")) {
                    isAttribute = part;
                    it.remove();
                } else {
                    finalParts.add(part);
                }
            }
        }

        // if there are two attributes and the second doesn't start with "command=", add it
        if (finalParts.size() >= 2) {
            if (!finalParts.get(1).toLowerCase().startsWith("command=")) {
                String part = finalParts.remove(1);
                finalParts.add(1, "command=" + part);
            }
        }

        // keep only the good parameters
        String[] optionParts = (String[]) finalParts.toArray(new String[0]);

        return optionParts;
    }

    /**
     * Compare to TrancheServerCommandLineItems for equality.
     * @param   o   the object whose name is to be compared
     * @return      the value 0 if the object's name is equal to this object's name;
     *              a value less than 0 if the object's name is lexicographically less than this object's name;
     *              a value greater than 0 if the object's name is lexicographically greater than this object's name.
     */
    public int compareTo(Object o) {
        TrancheServerCommandLineClientItem a = (TrancheServerCommandLineClientItem) o;
        return getName().compareTo(a.getName());
    }

    /**
     * Helper method to print appropriately long spacer lines.
     *
     * @param   out     the output print stream
     *   
     */
    void printSpacerLine(PrintStream out) {
        for (int i = 0; i < tsclc.getMaxLineLength() - 1; i++) {
            out.print("-");
        }
        out.println();
    }
}