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
import org.tranche.TrancheServer;

/**
 * Kill item class for killing the tranche server.
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class KillItem extends TrancheServerCommandLineClientItem {

    /**
     * Create a kill item for the CLC.  Use only for emergency use.
     * @param   dfs     the Tranche server received
     * @param   dfsclc  the Tranche server command line client received
     */
    public KillItem(TrancheServer dfs,
            TrancheServerCommandLineClient dfsclc) {
        super(dfs, dfsclc, "kill", "Emergency use only! This attempts to force the entire application to terminate, i.e. it directly invokes System.exit(). Do not use this command unless you know what you are doing. It may result in loss of data and corruption.");
    }

    /**
     * Kill's the server by exiting the process.
     * @param   in      the buffered reader
     * @param   out     the print stream
     */
    public void doAction(BufferedReader in, PrintStream out) {
        try {
            out.println("\nExiting.");
            System.exit(1);
        } catch (Exception e) {
            out.println("\nError. Can't execute command.\n");
            e.printStackTrace();
        }
    }
}