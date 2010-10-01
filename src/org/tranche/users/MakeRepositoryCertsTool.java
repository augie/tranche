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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.tranche.ConfigureTranche;
import org.tranche.commons.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.util.TestUtil;

/**
 * <p>Creates a new default set of certificates that are to be used for a Tranche repository.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class MakeRepositoryCertsTool {

    private static boolean verbose = false;

    /**
     * <p>Prints a message to System.out.</p>
     * @param msg
     */
    private static void printVerbose(String msg) {
        if (verbose) {
            System.out.println(msg);
        }
    }

    /**
     * 
     */
    private static void printUsage() {
        System.out.println();
        System.out.println("USAGE");
        System.out.println("    [FLAGS] <DIRECTORY>");
        System.out.println();
        System.out.println("DESCRIPTION");
        System.out.println("    Makes a set of user zip files to be used as the base users of a Tranche repository. These files will be saved in <DIRECTORY>.");
        System.out.println();
        System.out.println("PRINT AND EXIT FLAGS");
        System.out.println("    Use one of these to print some information and exit. Usage: java -jar <JAR> [PRINT AND EXIT FLAG]");
        System.out.println();
        System.out.println("    -h, --help         Print usage and exit.");
        System.out.println("    -V, --version      Print version number and exit.");
        System.out.println();
        System.out.println("OUTPUT FLAGS");
        System.out.println("    -d, --debug        If you have problems, you can use this option to print debugging information. These will help use solve problems if you can repeat your problem with this flag on.");
        System.out.println("    -v, --verbose      Print additional progress information.");
        System.out.println();
        System.out.println("RETURN CODES");
        System.out.println("    0: Exited normally");
        System.out.println("    1: Unknown error");
        System.out.println("    2: Problem with argument(s)");
        System.out.println("    3: Known error");
        System.out.println();
    }

    /**
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        try {
            // If no arguments, print and exit
            if (args.length == 0) {
                printUsage();
                if (!TestUtil.isTesting()) {
                    System.exit(2);
                } else {
                    return;
                }
            }

            ConfigureTranche.loadDefaults(ConfigureTranche.DEFAULT_CONFIG_FILE_LOCATION);

            // flags
            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("-d") || args[i].equals("--debug")) {
                    DebugUtil.setDebug(MakeRepositoryCertsTool.class, true);
                    DebugUtil.setDebug(MakeUserZipFileTool.class, true);
                    DebugUtil.setDebug(UserZipFile.class, true);
                } else if (args[0].equals("-n") || args[0].equals("--buildnumber") || args[0].equals("-V") || args[0].equals("--version")) {
                    System.out.println("Tranche, build #@buildNumber");
                    if (!TestUtil.isTesting()) {
                        System.exit(0);
                    } else {
                        return;
                    }
                } else if (args[0].equals("-h") || args[0].equals("--help")) {
                    printUsage();
                    if (!TestUtil.isTesting()) {
                        System.exit(0);
                    } else {
                        return;
                    }
                } else if (args[i].equals("-v") || args[i].equals("--verbose")) {
                    verbose = true;
                }
            }

            // check the destination (last argument)
            File directory = null;
            try {
                directory = new File(args[args.length - 1]);
            } catch (Exception e) {
                System.err.println("ERROR: Invalid value for output directory.");
                DebugUtil.debugErr(MakeRepositoryCertsTool.class, e);
                if (!TestUtil.isTesting()) {
                    System.exit(2);
                } else {
                    return;
                }
            }
            printVerbose("Directory to save certificates: " + directory.getAbsolutePath());
            if (!directory.exists()) {
                System.err.println("ERROR: Directory does not exist: " + directory.getAbsolutePath());
                if (!TestUtil.isTesting()) {
                    System.exit(3);
                } else {
                    return;
                }
            }

            // create the tool
            MakeUserZipFileTool tool = new MakeUserZipFileTool();
            tool.setValidDays(Long.valueOf("365000"));

            FileOutputStream fos = null;
            try {
                // make the admin cert
                {
                    printVerbose("Creating Administrator Certificate");
                    tool.setName("Admin");

                    String passphrase = SecurityUtil.generateBase64Password(15);
                    printVerbose("Administator Certificate Passphrase: " + passphrase);
                    tool.setPassphrase(passphrase);

                    File saveFile = new File(directory, "admin.zip.encrypted");
                    if (saveFile.exists()) {
                        System.err.println("ERROR: Administrator zip file file already exists: " + saveFile.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    tool.setSaveFile(saveFile);
                    UserZipFile user = tool.makeCertificate();

                    File adminCert = new File(directory, "admin.public.certificate");
                    printVerbose("Creating administrator certificate file: " + adminCert.getAbsolutePath());
                    if (adminCert.exists()) {
                        System.err.println("ERROR: Administrator certificate file already exists: " + adminCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    if (!adminCert.createNewFile()) {
                        System.err.println("ERROR: Could not create administator certificate file: " + adminCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    // write out the certificate
                    printVerbose("Writing out the administrator certificate to " + adminCert.getAbsolutePath());
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(adminCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }

                    // create a passphrase file
                    File passphrasesFile = new File(directory, "passphrases.txt");
                    printVerbose("Creating passphrases file: " + passphrasesFile.getAbsolutePath());
                    if (!passphrasesFile.createNewFile()) {
                        System.err.println("ERROR: Could not create passphrases file: " + passphrasesFile.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    // write out to the passphrases file
                    fos = new FileOutputStream(passphrasesFile);
                    fos.write(("Administator: " + passphrase + "\n").getBytes());
                }

                // make the write cert
                {
                    printVerbose("Creating Write-Only Certificate");
                    tool.setName("Write-Only");

                    String passphrase = SecurityUtil.generateBase64Password(15);
                    printVerbose("Write-Only Certificate Passphrase: " + passphrase);
                    tool.setPassphrase(passphrase);

                    File saveFile = new File(directory, "write.zip.encrypted");
                    if (saveFile.exists()) {
                        System.err.println("ERROR: Write-only zip file file already exists: " + saveFile.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    tool.setSaveFile(saveFile);
                    UserZipFile user = tool.makeCertificate();

                    // create cert file
                    File writeCert = new File(directory, "write.public.certificate");
                    printVerbose("Creating write-only certificate file: " + writeCert.getAbsolutePath());
                    if (writeCert.exists()) {
                        System.err.println("ERROR: Write-only certificate file already exists: " + writeCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    if (!writeCert.createNewFile()) {
                        System.err.println("ERROR: Could not create write-only certificate file: " + writeCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }

                    // write out the certificate
                    printVerbose("Writing out the write-only certificate to " + writeCert.getAbsolutePath());
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(writeCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }

                    fos.write(("Write-Only: " + passphrase + "\n").getBytes());
                }

                // make the read cert
                UserZipFile readUser = null;
                {
                    printVerbose("Creating Read-Only Certificate");
                    tool.setName("Read-Only");

                    String passphrase = SecurityUtil.generateBase64Password(15);
                    printVerbose("Read-Only Certificate Passphrase: " + passphrase);
                    tool.setPassphrase(passphrase);

                    File saveFile = new File(directory, "read.zip.encrypted");
                    if (saveFile.exists()) {
                        System.err.println("ERROR: Read-only zip file file already exists: " + saveFile.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    tool.setSaveFile(saveFile);
                    UserZipFile user = tool.makeCertificate();
                    readUser = user;

                    // create cert file
                    File readCert = new File(directory, "read.public.certificate");
                    printVerbose("Creating read-only certificate file: " + readCert.getAbsolutePath());
                    if (readCert.exists()) {
                        System.err.println("ERROR: Read-only certificate file already exists: " + readCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    if (!readCert.createNewFile()) {
                        System.err.println("ERROR: Could not create read-only certificate file: " + readCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }

                    // write out the certificate
                    printVerbose("Writing out the read-only certificate to " + readCert.getAbsolutePath());
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(readCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }

                    fos.write(("Read-Only: " + passphrase + "\n").getBytes());
                }

                // make the user cert
                {
                    printVerbose("Creating User Certificate");
                    tool.setName("User");

                    String passphrase = SecurityUtil.generateBase64Password(15);
                    printVerbose("User Certificate Passphrase: " + passphrase);
                    fos.write(("User: " + passphrase + "\n").getBytes());
                    tool.setPassphrase(passphrase);

                    File saveFile = new File(directory, "user.zip.encrypted");
                    if (saveFile.exists()) {
                        System.err.println("ERROR: User zip file file already exists: " + saveFile.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    tool.setSaveFile(saveFile);
                    UserZipFile user = tool.makeCertificate();

                    // create cert file
                    File userCert = new File(directory, "user.public.certificate");
                    printVerbose("Creating user certificate file: " + userCert.getAbsolutePath());
                    if (userCert.exists()) {
                        System.err.println("ERROR: User certificate file already exists: " + userCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    if (!userCert.createNewFile()) {
                        System.err.println("ERROR: Could not create user certificate file: " + userCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }

                    // write out the certificate
                    printVerbose("Writing out the user certificate to " + userCert.getAbsolutePath());
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(userCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }
                }

                // make the autocert cert
                {
                    printVerbose("Creating AutoCert Certificate");
                    tool.setName("AutoCert");

                    String passphrase = SecurityUtil.generateBase64Password(15);
                    printVerbose("AutoCert Certificate Passphrase: " + passphrase);
                    tool.setPassphrase(passphrase);

                    File saveFile = new File(directory, "autocert.zip.encrypted");
                    if (saveFile.exists()) {
                        System.err.println("ERROR: User zip file file already exists: " + saveFile.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    tool.setSaveFile(saveFile);
                    UserZipFile user = tool.makeCertificate();

                    // create cert file
                    File autocertCert = new File(directory, "autocert.public.certificate");
                    printVerbose("Creating auto certificate file: " + autocertCert.getAbsolutePath());
                    if (autocertCert.exists()) {
                        System.err.println("ERROR: Auto certificate file already exists: " + autocertCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    if (!autocertCert.createNewFile()) {
                        System.err.println("ERROR: Could not create auto certificate file: " + autocertCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }

                    // write out the certificate
                    printVerbose("Writing out the auto certificate to " + autocertCert.getAbsolutePath());
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(autocertCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }

                    fos.write(("AutoCert: " + passphrase + "\n").getBytes());
                }

                // make the anonymous cert
                {
                    printVerbose("Creating Anonymous Certificate");
                    tool.setName("Anonymous");

                    // should the anonymous certificate have read-only abilities?
                    tool.setSignerCertificate(readUser.getCertificate());
                    tool.setSignerPrivateKey(readUser.getPrivateKey());
                    printVerbose("Anonymous certificate signed by read-only certificate.");

                    String passphrase = SecurityUtil.generateBase64Password(15);
                    printVerbose("Anonymous Certificate Passphrase: " + passphrase);
                    tool.setPassphrase(passphrase);

                    File saveFile = new File(directory, "anonymous.zip.encrypted");
                    if (saveFile.exists()) {
                        System.err.println("ERROR: Anonymous zip file file already exists: " + saveFile.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    tool.setSaveFile(saveFile);
                    UserZipFile user = tool.makeCertificate();

                    // create the cert file
                    File anonCert = new File(directory, "anonymous.public.certificate");
                    printVerbose("Creating anonymous certificate file: " + anonCert.getAbsolutePath());
                    if (anonCert.exists()) {
                        System.err.println("ERROR: Anonymous certificate file already exists: " + anonCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    if (!anonCert.createNewFile()) {
                        System.err.println("ERROR: Could not create anonymous certificate file: " + anonCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    // write out the certificate
                    printVerbose("Writing out the anonymous certificate to " + anonCert.getAbsolutePath());
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(anonCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }

                    // create the key file
                    File anonKey = new File(directory, "anonymous.private.key");
                    printVerbose("Creating anonymous private key file: " + anonKey.getAbsolutePath());
                    if (anonKey.exists()) {
                        System.err.println("ERROR: Anonymous private key file already exists: " + anonKey.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    if (!anonKey.createNewFile()) {
                        System.err.println("ERROR: Could not create anonymous private key file: " + anonKey.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }

                    // write out the private key
                    printVerbose("Writing out the anonymous private key to " + anonCert.getAbsolutePath());
                    try {
                        os = new FileOutputStream(anonKey);
                        os.write(user.getPrivateKey().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }

                    fos.write(("Anonymous: " + passphrase + "\n").getBytes());
                }

                // make the email cert
                {
                    printVerbose("Creating Email Certificate");
                    tool.setName("Email");

                    String passphrase = SecurityUtil.generateBase64Password(15);
                    printVerbose("Email Certificate Passphrase: " + passphrase);
                    tool.setPassphrase(passphrase);

                    File saveFile = new File(directory, "email.zip.encrypted");
                    if (saveFile.exists()) {
                        System.err.println("ERROR: Email zip file file already exists: " + saveFile.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    tool.setSaveFile(saveFile);
                    UserZipFile user = tool.makeCertificate();

                    // create cert file
                    File emailCert = new File(directory, "email.public.certificate");
                    printVerbose("Creating email certificate file: " + emailCert.getAbsolutePath());
                    if (emailCert.exists()) {
                        System.err.println("ERROR: Email certificate file already exists: " + emailCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    if (!emailCert.createNewFile()) {
                        System.err.println("ERROR: Could not create email certificate file: " + emailCert.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }

                    // write out the certificate
                    printVerbose("Writing out the email certificate to " + emailCert.getAbsolutePath());
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(emailCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }

                    // create key file
                    File emailKey = new File(directory, "email.private.key");
                    printVerbose("Creating email private key file: " + emailKey.getAbsolutePath());
                    if (emailKey.exists()) {
                        System.err.println("ERROR: Email private key file already exists: " + emailKey.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }
                    if (!emailKey.createNewFile()) {
                        System.err.println("ERROR: Could not create email private key file: " + emailKey.getAbsolutePath());
                        if (!TestUtil.isTesting()) {
                            System.exit(3);
                        } else {
                            return;
                        }
                    }

                    // write out the private key
                    printVerbose("Writing out the email private key to " + emailKey.getAbsolutePath());
                    try {
                        os = new FileOutputStream(emailKey);
                        os.write(user.getPrivateKey().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }

                    fos.write(("Email: " + passphrase + "\n").getBytes());
                }
            } finally {
                IOUtil.safeClose(fos);
            }

            printVerbose("Certificates created in: " + directory.getAbsolutePath());
            if (!TestUtil.isTesting()) {
                System.exit(0);
            } else {
                return;
            }
        } catch (Exception e) {
            DebugUtil.debugErr(MakeRepositoryCertsTool.class, e);
            if (!TestUtil.isTesting()) {
                System.err.println(e.getClass().getSimpleName()+": "+e.getMessage());
                e.printStackTrace(System.err);
                System.exit(1);
            } else {
                return;
            }
        }
    }
}
