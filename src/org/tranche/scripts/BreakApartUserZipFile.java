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
package org.tranche.scripts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.tranche.users.UserZipFile;
import org.tranche.util.IOUtil;

/**
 * <p>Takes a User Zip File (*.zip.encrypted) and the password for that file and creates a X509Certificate file (*.public.certificate) and a PrivateKey file (*.private.key) in the same directory.</p>
 * @author James "Augie" Hill - augman85@gmail.com - augman85@gmail.com
 */
public class BreakApartUserZipFile {

    /**
     * <p>Takes a User Zip File (*.zip.encrypted) and the password for that file and creates a X509Certificate file (*.public.certificate) and a PrivateKey file (*.private.key) in the same directory.</p>
     * @param args -uzf [path to user zip file] -pass [user zip file passphrase]
     */
    public static void main(String[] args) {
        // tell them what's up
        if (args.length < 2) {
            System.out.println("Arguments:");
            System.out.println("    -uzf [path to user zip file]            Required. The path to the user zip file on the local file system.");
            System.out.println("    -pass [user zip file passphrase]      Required. The passphrase for the given user zip file.");
        }

        // read the arguments
        UserZipFile uzf = null;
        for (int i = 0; i < args.length; i += 2) {
            if (args[i].equals("-uzf")) {
                try {
                    uzf = new UserZipFile(new File(args[i + 1]));
                } catch (Exception e) {
                    System.out.println("Could not load user zip file. Ignoring.");
                }
            } else if (args[i].equals("-pass")) {
                try {
                    uzf.setPassphrase(args[i + 1]);
                } catch (Exception e) {
                    System.out.println("Problem setting user zip file passphrase. Ignoring.");
                }
            }
        }

        // dummy check
        if (uzf == null) {
            System.out.println("User Zip File not set - exiting.");
            return;
        }
        if (!uzf.getFile().getName().endsWith(".zip.encrypted")) {
            System.out.println("File is not a *.zip.encrypted file - exiting.");
            return;
        }
        if (uzf.getCertificate() == null) {
            System.out.println("Could not read certificate from user zip file - exiting.");
            return;
        }
        if (uzf.getPrivateKey() == null) {
            System.out.println("Could not read private key from user zip file - exiting.");
            return;
        }

        System.out.println("Starting to break up file...");
        System.out.println("");
        
        // break it up
        String certName = uzf.getFile().getName().replace(".zip.encrypted", "");
        File certFile = new File(uzf.getFile().getParentFile(), certName + ".public.certificate");
        File keyFile = new File(uzf.getFile().getParentFile(), certName + ".private.key");

        // write out the certificate and private key
        try {
            OutputStream os = null;
            try {
                if (certFile.exists()) {
                    certFile.delete();
                }
                certFile.createNewFile();
                os = new FileOutputStream(certFile);
                os.write(uzf.getCertificate().getEncoded());
            } finally {
                IOUtil.safeClose(os);
            }

            // write out the private key
            try {
                if (keyFile.exists()) {
                    keyFile.delete();
                }
                keyFile.createNewFile();
                os = new FileOutputStream(keyFile);
                os.write(uzf.getPrivateKey().getEncoded());
            } finally {
                IOUtil.safeClose(os);
            }
        } catch (Exception e) {
            System.out.println("Could not create files - do you have permission to write to that directory?");
            return;
        }
        
        System.out.println("Files created:");
        System.out.println("  " + certFile.getAbsolutePath());
        System.out.println("  " + keyFile.getAbsolutePath());
        System.out.println("");
        System.out.println("Operation finished.");
        System.exit(0);
    }
}
