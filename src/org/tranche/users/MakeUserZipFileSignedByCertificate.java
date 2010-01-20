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
package org.tranche.users;

import org.tranche.security.SecurityUtil;
import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * <p>Given a certificate and a private key, create a user signed by the two.</p>
 * <p>This script is especially helpful when user files for network expire, and need to create new user files from certificates.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class MakeUserZipFileSignedByCertificate {

    private static void printUsage() {
        System.err.println();
        System.err.println("USAGE");
        System.err.println();
        System.err.println("DESCRIPTION");
        System.err.println("    Given a certificate and a private key, create a user signed by the two. This script is especially helpful when user files for network expire, and need to create new user files from certificates.");
        System.err.println();
        System.err.println("PARAMETERS");
        System.err.println("    -c, --cert          String      The path to the certificate to sign the new user");
        System.err.println("    -k, --key           String      The path to the key to sign the new user");
        System.err.println("    -o, --out           String      The path to the new user file");
        System.err.println("    -n, --name          String      The name you want to appear as the signer");
        System.err.println("    -p, --password      String      The password for the new file");
        System.err.println();
        System.err.println("RETURN CODES");
        System.err.println("    0: Exitted normally");
        System.err.println("    1: Unknown problem, see standard error");
        System.err.println("    2: Unrecognized parameter");
        System.err.println("    3: Missing required parameter");
        System.err.println();
    }

    public static void main(String[] args) {
        String certPath = null;
        String keyPath = null;
        String destPath = null;
        String name = null;
        String password = null;

        try {
            for (int i = 0; i < args.length; i += 2) {
                if (args[i].trim().equals("-c") || args[i].trim().equals("--cert")) {
                    certPath = args[i + 1];
                } else if (args[i].trim().equals("-k") || args[i].trim().equals("--key")) {
                    keyPath = args[i + 1];
                } else if (args[i].trim().equals("-o") || args[i].trim().equals("--out")) {
                    destPath = args[i + 1];
                } else if (args[i].trim().equals("-n") || args[i].trim().equals("--name")) {
                    name = args[i + 1];
                } else if (args[i].trim().equals("-p") || args[i].trim().equals("--password")) {
                    password = args[i + 1];
                } else {
                    System.err.println("Unrecognized parameter: " + args[i]);
                    printUsage();
                    System.exit(2);
                }
            }

            if (certPath == null) {
                System.err.println("Missing required parameter: path to signing certificate");
                printUsage();
                System.exit(3);
            }
            if (keyPath == null) {
                System.err.println("Missing required parameter: path to signing key");
                printUsage();
                System.exit(3);
            }
            if (destPath == null) {
                System.err.println("Missing required parameter: path to output file");
                printUsage();
                System.exit(3);
            }
            if (name == null) {
                System.err.println("Missing required parameter: name");
                printUsage();
                System.exit(3);
            }
            if (password == null) {
                System.err.println("Missing required parameter: password");
                printUsage();
                System.exit(3);
            }

            File privateKeyFile = new File(keyPath);
            File certificateFile = new File(certPath);
            File outputFile = new File(destPath);

            PrivateKey key = SecurityUtil.getPrivateKey(privateKeyFile);
            X509Certificate cert = SecurityUtil.getCertificate(certificateFile);

            // create the tool to use
            MakeUserZipFileTool tool = new MakeUserZipFileTool();
            // default network certs are valid for 1000 years
            tool.setValidDays(365000);

            tool.setSignerCertificate(cert);
            tool.setSignerPrivateKey(key);
            tool.setName(name);
            tool.setPassphrase(password);
            tool.setUserFile(outputFile);

            UserZipFile uzf = tool.makeCertificate();

            if (uzf != null) {
                System.out.println("File created: " + uzf.getFile().getAbsolutePath());
                System.exit(0);
            } else {
                throw new Exception("Not sure why UserZipFile is null. Did tool fail?");
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage();
            System.exit(1);
        }
    }
}
