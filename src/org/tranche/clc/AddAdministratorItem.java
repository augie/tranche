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
import org.tranche.TrancheServer;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.security.SecurityUtil;
import org.tranche.users.UserCertificateUtil;
import org.tranche.users.UserZipFile;

/**
 * Class inherited from the Tranche Server CLC Item for adding an administrator item.
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class AddAdministratorItem extends TrancheServerCommandLineClientItem {

    /**
     * Add a new administrator to the server's configuration.
     * @param   dfs     the Tranche server received
     * @param   dfsclc  the Tranche server command line client received
     */
    public AddAdministratorItem(TrancheServer dfs, TrancheServerCommandLineClient dfsclc) {
        super(dfs, dfsclc, "addAdministrator", "Adds a new administrator to the server's configuration. Helpful when setting up a new server from a headless computer.");
        addAttribute("file", "The user file to load.", true);
        addAttribute("passphrase", "The passphrase to use.", false);
    }

    /**
     * Parse out from input buffer the administrator information to fill the 
     * CLC item.
     * @param   in      the buffered reader
     * @param   out     the print stream
     */
    public void doAction(BufferedReader in, PrintStream out) {
        try {
            // find the command
            String file = getParameter("file");
            String passphrase = getParameter("passphrase");

            // check for the file
            File f = new File(file);
            if (!f.exists()) {
                out.println("User file doesn't exist: " + f + "\n\nCheck that you specified the correct file.");
            }

            // load the user file
            UserZipFile uzf = new UserZipFile(f);
            if (passphrase != null) {
                uzf.setPassphrase(passphrase);
            } else {
                uzf.setPassphrase("");
            }

            // set the user's permissions to match that of an admin
            uzf.setFlags(SecurityUtil.getAdmin().getFlags());

            // add the user
            FlatFileTrancheServer ffts = (FlatFileTrancheServer) dfs;
            ffts.getConfiguration().getUsers().add(uzf);

            // confirm that things are OK
            out.println("User \"" + UserCertificateUtil.readUserName(uzf.getCertificate()) + "\" successfully added.");
        } catch (Exception e) {
            out.println("Can't load user file. Please check that you've specified the correct file and passphrase.");
        }
    }
}