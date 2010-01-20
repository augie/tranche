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
import java.io.PrintStream;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.tranche.users.MakeUserZipFileTool;
import org.tranche.users.UserZipFile;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;

/**
 * <p>Creates a new default set of certificates that are to be used for a Tranche network.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class MakeDefaultCerts {

    private static boolean debug = false,  verbose = false;

    /**
     * <p>Prints a message to System.err.</p>
     * @param msg
     */
    private static void printDebug(String msg) {
        if (debug) {
            System.err.println(msg);
        }
    }

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
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        try {
            // If no arguments, print and exit
            if (args.length == 0) {
                printUsage();
                System.exit(2);
            }

            // Look for help request. If one, print and exit.
            for (String arg : args) {
                try {
                    if (arg.equals("-n") || arg.equals("--buildnumber")) {
                        System.out.println("Tranche network certificates creator, build: @buildNumber");
                        System.exit(0);
                    } else if (arg.equals("-h") || arg.equals("--help")) {
                        printUsage();
                        System.exit(0);
                    } else if (arg.equals("-d") || arg.equals("--debug")) {
                        debug = true;
                    } else if (arg.equals("-v") || arg.equals("--verbose")) {
                        verbose = true;
                    }
                } catch (Exception e) {
                    printDebug(e.getMessage());
                    System.exit(2);
                }
            }

            // create the tool to use
            MakeUserZipFileTool tool = new MakeUserZipFileTool();
            // default network certs are valid for 1000 years
            tool.setValidDays(365000);

            // read the arguments
            String organization = "Tranche Network";
            boolean anonRead = true;
            for (int i = 0; i < args.length - 1; i += 2) {
                try {
                    // do not allow blank arguments
                    if (args[i + 1] == null) {
                        continue;
                    }
                    if (args[i].equals("-c") || args[i].equals("--country")) {
                        tool.setCountry(args[i + 1]);
                    } else if (args[i].equals("-o") || args[i].equals("--organization")) {
                        organization = args[i + 1];
                        tool.setOrganization(organization);
                    } else if (args[i].equals("-u") || args[i].equals("--org-unit")) {
                        tool.setOrganizationalUnit(args[i + 1]);
                    } else if (args[i].equals("-l") || args[i].equals("--location")) {
                        tool.setLocation(args[i + 1]);
                    } else if (args[i].equals("-s") || args[i].equals("--state")) {
                        tool.setState(args[i + 1]);
                    } else if (args[i].equals("-a") || args[i].equals("--anon-read")) {
                        anonRead = Boolean.valueOf(args[i + 1]);
                    } else if (args[i].equals("-y") || args[i].equals("--days-valid")) {
                        tool.setValidDays(Integer.valueOf(args[i + 1]));
                    }
                } catch (Exception e) {
                    printDebug(e.getMessage());
                    System.exit(2);
                }
            }
            
            printVerbose("Certificates will be valid for " + tool.getValidDays() + " days.");

            // check the destination (last argument)
            File directory = new File(args[args.length - 1]);
            if (!directory.exists()) {
                printDebug("Directory does not exist.");
                System.exit(3);
            }

            // check for a subdirectory
            File subdirectory = new File(directory, "tranche-certs");
            int subdirNum = 1;
            while (subdirectory.exists() && subdirNum < 10) {
                subdirectory = new File(directory, "tranche-certs-" + subdirNum++);
            }

            // make sure we've got a new subdirectory
            if (subdirectory.exists()) {
                printDebug("Delete old tranche-certs directories and try again.");
                System.exit(3);
            }

            // create the subdirectory
            subdirectory.mkdir();

            // create a passphrase file
            File passphraseFile = new File(subdirectory, "passphrases.txt");
            passphraseFile.createNewFile();
            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(passphraseFile);

                // create the files
                File adminCert = new File(subdirectory, "admin.public.certificate");
                adminCert.createNewFile();
                File writeCert = new File(subdirectory, "write.public.certificate");
                writeCert.createNewFile();
                File readCert = new File(subdirectory, "read.public.certificate");
                readCert.createNewFile();
                File userCert = new File(subdirectory, "user.public.certificate");
                userCert.createNewFile();
                File autocertCert = new File(subdirectory, "autocert.public.certificate");
                autocertCert.createNewFile();
                File anonCert = new File(subdirectory, "anonymous.public.certificate");
                anonCert.createNewFile();
                File anonKey = new File(subdirectory, "anonymous.private.key");
                anonKey.createNewFile();
                File emailCert = new File(subdirectory, "email.public.certificate");
                emailCert.createNewFile();
                File emailKey = new File(subdirectory, "email.private.key");
                emailKey.createNewFile();

                // use bouncy castle
                Security.addProvider(new BouncyCastleProvider());

                // make the admin cert
                {
                    printVerbose("Creating Administrator Certificate");
                    tool.setName(organization + " Administrator Certificate");

                    String passphrase = SecurityUtil.generateBase64Password(10);
                    printVerbose("  Administator Certificate Passphrase: \"" + passphrase + "\"");
                    fos.write(("Administator: \"" + passphrase + "\"\n").getBytes());
                    tool.setPassphrase(passphrase);

                    tool.setUserFile(new File(subdirectory, "admin.zip.encrypted"));
                    UserZipFile user = tool.makeCertificate();

                    // write out the certificate
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(adminCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }
                }

                // make the write cert
                {
                    printVerbose("Creating Write-Only Certificate");
                    tool.setName(organization + " Write-Only Certificate");

                    String passphrase = SecurityUtil.generateBase64Password(10);
                    printVerbose("  Write-Only Certificate Passphrase: \"" + passphrase + "\"");
                    fos.write(("Write-Only: \"" + passphrase + "\"\n").getBytes());
                    tool.setPassphrase(passphrase);

                    tool.setUserFile(new File(subdirectory, "write.zip.encrypted"));
                    UserZipFile user = tool.makeCertificate();

                    // write out the certificate
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(writeCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }
                }

                // make the read cert
                UserZipFile readUser = null;
                {
                    printVerbose("Creating Read-Only Certificate");
                    tool.setName(organization + " Read-Only Certificate");

                    String passphrase = SecurityUtil.generateBase64Password(10);
                    printVerbose("  Read-Only Certificate Passphrase: \"" + passphrase + "\"");
                    fos.write(("Read-Only: \"" + passphrase + "\"\n").getBytes());
                    tool.setPassphrase(passphrase);

                    tool.setUserFile(new File(subdirectory, "read.zip.encrypted"));
                    UserZipFile user = tool.makeCertificate();
                    readUser = user;

                    // write out the certificate
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(readCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }
                }

                // make the user cert
                {
                    printVerbose("Creating User Certificate");
                    tool.setName(organization + " User Certificate");

                    String passphrase = SecurityUtil.generateBase64Password(10);
                    printVerbose("  User Certificate Passphrase: \"" + passphrase + "\"");
                    fos.write(("User: \"" + passphrase + "\"\n").getBytes());
                    tool.setPassphrase(passphrase);

                    tool.setUserFile(new File(subdirectory, "user.zip.encrypted"));
                    UserZipFile user = tool.makeCertificate();

                    // write out the certificate
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
                    tool.setName(organization + " AutoCert Certificate");

                    String passphrase = SecurityUtil.generateBase64Password(10);
                    printVerbose("  AutoCert Certificate Passphrase: \"" + passphrase + "\"");
                    fos.write(("AutoCert: \"" + passphrase + "\"\n").getBytes());
                    tool.setPassphrase(passphrase);

                    tool.setUserFile(new File(subdirectory, "autocert.zip.encrypted"));
                    UserZipFile user = tool.makeCertificate();

                    // write out the certificate
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(autocertCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }
                }

                // make the anonymous cert
                {
                    printVerbose("Creating Anonymous Certificate");
                    tool.setName(organization + " Anonymous Certificate");

                    // should the anonymous certificate have read-only abilities?
                    if (anonRead) {
                        tool.setSignerCertificate(readUser.getCertificate());
                        tool.setSignerPrivateKey(readUser.getPrivateKey());
                        printVerbose("Anonymous certificate signed by read-only certificate.");
                    }

                    String passphrase = SecurityUtil.generateBase64Password(10);
                    printVerbose("  Anonymous Certificate Passphrase: \"" + passphrase + "\"");
                    fos.write(("Anonymous: \"" + passphrase + "\"\n").getBytes());
                    tool.setPassphrase(passphrase);

                    tool.setUserFile(new File(subdirectory, "anonymous.zip.encrypted"));
                    UserZipFile user = tool.makeCertificate();

                    // write out the certificate
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(anonCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }

                    // write out the private key
                    try {
                        os = new FileOutputStream(anonKey);
                        os.write(user.getPrivateKey().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }
                }

                // make the email cert
                {
                    printVerbose("Creating Email Certificate");
                    tool.setName(organization + " Email Certificate");

                    String passphrase = SecurityUtil.generateBase64Password(10);
                    printVerbose("  Email Certificate Passphrase: \"" + passphrase + "\"");
                    fos.write(("Email: \"" + passphrase + "\"\n").getBytes());
                    tool.setPassphrase(passphrase);

                    tool.setUserFile(new File(subdirectory, "email.zip.encrypted"));
                    UserZipFile user = tool.makeCertificate();

                    // write out the certificate
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(emailCert);
                        os.write(user.getCertificate().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }

                    // write out the private key
                    try {
                        os = new FileOutputStream(emailKey);
                        os.write(user.getPrivateKey().getEncoded());
                    } finally {
                        IOUtil.safeClose(os);
                    }
                }
            } finally {
                IOUtil.safeClose(fos);
            }

            System.out.println("Certificates created in " + subdirectory.getAbsolutePath());
        } catch (Exception e) {
            printDebug(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * <p>Print out command-line usage.</p>
     */
    public static void printUsage() {
        printUsage(false);
    }

    /**
     * <p>Print out command-line usage.</p>
     * @param isError If true, usage printed to standard error. Else, printed to standard out.
     */
    private static void printUsage(boolean isError) {
        PrintStream console = System.out;

        if (isError) {
            console = System.err;
        }

        console.println();
        console.println("USAGE");
        console.println("    [OPTIONS...] <DIRECTORY>");
        console.println();
        console.println("DESCRIPTION");
        console.println("    Makes a set of Tranche user zip files to be used for starting a Tranche network.");
        console.println();
        console.println("MEMORY ALLOCATION");
        console.println("    You should use the JVM option:");
        console.println();
        console.println("    java -Xmx512m -jar <JAR> ...");
        console.println();
        console.println("    The allocates 512m of memory for the tool. You can adjust this amount (e.g., you don't have 512MB available memory or you want to allocate more.)");
        console.println();
        console.println("    The rest of this document does not mention this parameter, but it is highly advised that you use it.");
        console.println();
        console.println("OPTIONAL OUTOUT PARAMETERS");
        console.println("    -d, --debug                 Values: none.           If you have problems, you can use this option to print tracers. These will help use solve problems if you can repeat your problem with this flag on.");
        console.println("    -h, --help                  Values: none.           Print usage and exit. All other arguments will be ignored.");
        console.println("    -v, --verbose               Values: none.           Print additional progress information to standard out.");
        console.println("    -n, --buildnumber           Values: none.           Print version number and exit. All other arguments will be ignored.");
        console.println();
        console.println("OPTIONAL PARAMETERS");
        console.println("    By default, certificates are valid for 1,000 years and the anonymous certificate is signed by the read-only certificate (anybody can download from your network).");
        console.println();
        console.println("    -c, --country               Values: any string.     Country to be associated with your certificates.");
        console.println("    -o, --organization          Values: any string.     Organization to be associated with your certificates.");
        console.println("    -u, --org-unit              Values: any string.     Organizational unit to be associated with your certificates.");
        console.println("    -l, --location              Values: any string.     Location (city) to be associated with your certificates.");
        console.println("    -s, --state                 Values: any string.     State to be associated with your certificates.");
        console.println("    -a, --anon-read             Values: true or false.  If true, anybody can download from the Tranche network.");
        console.println("    -y, --days-valid            Values: integer.        The number of days for which the certificates should be valid.");
        console.println();
        console.println("RETURN CODES");
        console.println("    To check the return code for a process in bash console, use $? special variable. If non-zero, check standard error for messages.");
        console.println();
        console.println("    0:     Program exited normally (e.g., certificates created, help displayed, etc.)");
        console.println("    1:     Unknown error.");
        console.println("    2:     Problem with an argument.");
        console.println("    3:     A known error occurred. Use -d for more information.");
        console.println();
    }
}
