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
import java.io.File;
import java.io.PrintStream;
import org.tranche.TrancheServer;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.streams.PrettyPrintStream;
import org.tranche.users.UserCertificateUtil;
import org.tranche.users.UserZipFile;

/**
 * <p>Sets the user zip file for the local server.s</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class UserZipFileItem extends TrancheServerCommandLineClientItem {

    private FlatFileTrancheServer ts;

    public UserZipFileItem(TrancheServer ts, TrancheServerCommandLineClient tsclc) {
        super(ts, tsclc, "user", "Manages the server's user zip file.");
        addAttribute("command", "The command to run. Type 'user help' to see a list of the possible commands.", true);
        addAttribute("file", "The location of the user zip file.");
        addAttribute("passphrase", "The passphrase to unlock the user zip file");
        this.ts = (FlatFileTrancheServer) ts;
    }

    public void doAction(BufferedReader in, PrintStream out) {
        try {
            // required parameter
            String command = getParameter("command");
            // optional parameters
            String fileString = getParameter("file");
            String passphraseString = getParameter("passphrase");

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
                out.println("clear              Clears the user zip file.");
                out.println();
                out.println("show               Shows the currently set user zip file.");
                out.println();
                out.println("set                Sets the user zip file.");
                out.println("  file             The location of the user zip file on the local file system.");
                out.println("  passphrase       The passphrase that unlocks the given user zip file.");
                // set spacing
                if (out instanceof PrettyPrintStream) {
                    PrettyPrintStream pps = (PrettyPrintStream) out;
                    pps.setPadding(0);
                }
                out.println();
            } else if (command.equals("clear")) {
                try {
                    ts.setAuthCert(null);
                    ts.setAuthPrivateKey(null);
                    out.println("User zip file cleared.");
                } catch (Exception e) {
                    out.println("Error: " + e.getMessage());
                }
            } else if (command.equals("show")) {
                if (ts.getAuthCert() == null) {
                    out.println("User zip file is not set.");
                } else {
                    out.println("User zip file set to: " + UserCertificateUtil.readUserName(ts.getAuthCert()));
                    out.println(" issued by:  " + UserCertificateUtil.readIssuerName(ts.getAuthCert()));
                }
            } else if (command.equals("set")) {
                try {
                    UserZipFile userZipFile = new UserZipFile(new File(fileString.trim()));
                    userZipFile.setPassphrase(passphraseString.trim());
                    ts.setAuthCert(userZipFile.getCertificate());
                    ts.setAuthPrivateKey(userZipFile.getPrivateKey());
                    out.println("User zip file successfully loaded: " + userZipFile.getUserNameFromCert());
                } catch (Exception e) {
                    out.println("Error: " + e.getMessage());
                }
            } else {
                out.println("Unrecognized command \"" + command + "\". Ignoring.");
            }
        } catch (Exception e) {
            out.println("Couldn't execute user command.");
        }
    }
}
