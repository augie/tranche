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
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Hashtable;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.tranche.security.SecurityUtil;
import org.tranche.time.TimeUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.TestUtil;

/**
 * <p>This is a helper class that makes user files, i.e. X.509 certificates and assocated private keys. The information is saved in a password protected ZIP file.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class MakeUserZipFileTool {

    private static boolean debug = false;
    private String name, passphrase;
    private Date startDate;
    private long validDays;
    private X509Certificate signerCertificate;
    private PrivateKey signerPrivateKey;
    private File saveFile;

    /**
     * <p>Executes the creation of the UserZipFile.</p>
     * @return
     * @throws java.lang.NullPointerException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.NoSuchProviderException
     * @throws java.security.SignatureException
     * @throws java.security.InvalidKeyException
     */
    public UserZipFile makeCertificate() throws NullPointerException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, InvalidKeyException {
        // checks
        if (name == null) {
            throw new NullPointerException("Name is not set.");
        }
        if (passphrase == null) {
            throw new NullPointerException("Passphrase is not set.");
        }
        if (saveFile == null) {
            throw new RuntimeException("Save location is not set.");
        }

        // execute
        SecurityUtil.lazyLoad();
        // make up a new RSA keypair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(1024);

        // key pair
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // make a new certificate
        Hashtable attrs = new Hashtable();
        attrs.put(X509Principal.CN, name);

        // Serialnumber is random bits, where random generator is initialized with Date.getTime()
        byte[] serno = new byte[8];
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(TimeUtil.getTrancheTimestamp());
        random.nextBytes(serno);
        BigInteger sn = new java.math.BigInteger(serno).abs();

        // make the principle
        X509Principal principal = new X509Principal(attrs);

        //generate cert
        X509V3CertificateGenerator gen = new X509V3CertificateGenerator();
        gen.setSerialNumber(sn);
        // use the give issuer if appropriate
        if (signerCertificate != null && signerPrivateKey != null) {
            gen.setIssuerDN((X509Principal) signerCertificate.getSubjectDN());
        } else {
            gen.setIssuerDN(principal);
        }
        gen.setNotBefore(startDate);
        gen.setNotAfter(new Date(startDate.getTime() + (validDays * Long.valueOf("86400000"))));
        gen.setSubjectDN(principal);
        gen.setSignatureAlgorithm("SHA1WITHRSA");
        gen.setPublicKey(publicKey);

        // make the certificate
        X509Certificate cert = null;
        if (signerCertificate != null && signerPrivateKey != null) {
            cert = gen.generateX509Certificate(getSignerPrivateKey());
        } else {
            cert = gen.generateX509Certificate(privateKey);
        }

        // make the user file
        UserZipFile uzf = new UserZipFile(saveFile);
        uzf.setCertificate(cert);
        uzf.setPrivateKey(privateKey);
        uzf.setPassphrase(passphrase);
        uzf.saveTo(saveFile);

        // return the user
        return uzf;
    }

    /**
     * <p>Returns the CN value that will be used on the X.509 certificate.</p>
     * @return The value that will be used for the CN value of the X.509 certificate.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Sets the CN value that will be used for the X.509 certificate.</p>
     * @param name The CN value to use for the X.509 certificate.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>Returns the number of days that the user's X.509 certificate should be considered valid.</p>
     * @return The number of days that the user's X.509 certificate should be considered valid.
     */
    public long getValidDays() {
        return validDays;
    }

    /**
     * <p>Sets the number of days that ther user's X.509 certificate should be considered valid.</p>
     * @param validDays The number of days that ther user's X.509 certificate should be considered valid.
     */
    public void setValidDays(long validDays) {
        setValidDays(new Date(TimeUtil.getTrancheTimestamp()), validDays);
    }

    /**
     * <p>Sets the number of days that ther user's X.509 certificate should be considered valid.</p>
     * @param startDate The date the X.509 certificate begins being considered valid.
     * @param validDays The number of days that ther user's X.509 certificate should be considered valid.
     */
    public void setValidDays(Date startDate, long validDays) {
        this.startDate = startDate;
        this.validDays = validDays;
    }

    /**
     * <p>Returns the passphrase that was used when AES 256 bit encrypting the file.</p>
     * @return The passphrase that was used when AES 256 bit encrypting the file.
     */
    public String getPassphrase() {
        return passphrase;
    }

    /**
     * <p>Sets the passphrase to use for AES 256 bit encryption.</p>
     * @param passphrase The passphrase to use for AES 256 bit encryption.
     */
    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    /**
     * <p>Returns a File object representing the file on the local disk that contains the user information. This file is normally a ZIP archive that has been AES 256 bit encrypted.</p>
     * @return A File object that represents the file that contains the user's information.
     */
    public File getSaveFile() {
        return saveFile;
    }

    /**
     * <p>A method to get the file that the user zip file will be saved to. This can be any file on the local filesystem.</p>
     * @param saveFile
     */
    public void setSaveFile(File saveFile) {
        this.saveFile = saveFile;
    }

    /**
     * <p>Gets the signer's certificates.</p>
     * @return
     */
    public X509Certificate getSignerCertificate() {
        return signerCertificate;
    }

    /**
     * <p>Sets the signer's certificate.</p>
     * @param signerCertificate
     */
    public void setSignerCertificate(X509Certificate signerCertificate) {
        this.signerCertificate = signerCertificate;
    }

    /**
     * <p>Gets the signer's private key.</p>
     * @return
     */
    public PrivateKey getSignerPrivateKey() {
        return signerPrivateKey;
    }

    /**
     * <p>Sets the signer's private key.</p>
     * @param signerPrivateKey
     */
    public void setSignerPrivateKey(PrivateKey signerPrivateKey) {
        this.signerPrivateKey = signerPrivateKey;
    }

    /**
     *
     */
    private static void printUsage() {
        System.out.println();
        System.out.println("USAGE");
        System.out.println("    [FLAGS / PARAMETERS] <NAME> <PASSPHRASE> <VALID DAYS> <FILE>");
        System.out.println();
        System.out.println("DESCRIPTION");
        System.out.println("    Creates a user zip file with the user name <NAME> and passphrase <PASSPHRASE> that is valid for <VALID DAYS> days and saves it to <FILE>.");
        System.out.println();
        System.out.println("PRINT AND EXIT FLAGS");
        System.out.println("    Use one of these to print some information and exit. Usage: java -jar <JAR> [PRINT AND EXIT FLAG]");
        System.out.println();
        System.out.println("    -h, --help          Print usage and exit.");
        System.out.println("    -V, --version       Print version number and exit.");
        System.out.println();
        System.out.println("OUTPUT FLAGS");
        System.out.println("    -d, --debug         If you have problems, you can use this option to print debugging information. These will help use solve problems if you can repeat your problem with this flag on.");
        System.out.println();
        System.out.println("PARAMETERS");
        System.out.println("    -s, --signer        Values: two strings.        Optional. A user zip file and its passphrase used to sign the new user zip file.");
        System.out.println();
        System.out.println("RETURN CODES");
        System.out.println("    0: Exited normally");
        System.out.println("    1: Unknown error");
        System.out.println("    2: Problem with argument(s)");
        System.out.println("    3: Known error");
        System.out.println();
    }

    /**
     * 
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String args[]) throws Exception {
        try {
            if (args.length == 0) {
                printUsage();
                if (!TestUtil.isTesting()) {
                    System.exit(2);
                } else {
                    return;
                }
            }

            // flags first
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-d") || args[i].equals("--debug")) {
                    DebugUtil.setDebug(true);
                    setDebug(true);
                    UserZipFile.setDebug(true);
                } else if (args[i].equals("-h") || args[i].equals("--help")) {
                    printUsage();
                    if (!TestUtil.isTesting()) {
                        System.exit(0);
                    } else {
                        return;
                    }
                } else if (args[i].equals("-n") || args[i].equals("--buildnumber") || args[i].equals("-V") || args[i].equals("--version")) {
                    System.out.println("Tranche, build #@buildNumber");
                    if (!TestUtil.isTesting()) {
                        System.exit(0);
                    } else {
                        return;
                    }
                } else if (args[i].equals("-s") || args[i].equals("--signer")) {
                    i += 2;
                }
            }

            MakeUserZipFileTool tool = new MakeUserZipFileTool();

            // parameters next
            for (int i = 0; i < args.length - 4; i++) {
                if (args[i].equals("-s") || args[i].equals("--signer")) {
                    try {
                        UserZipFile signer = new UserZipFile(new File(args[i + 1]));
                        signer.setPassphrase(args[i + 2]);
                        tool.setSignerCertificate(signer.getCertificate());
                        tool.setSignerPrivateKey(signer.getPrivateKey());
                        i += 2;
                    } catch (Exception e) {
                        System.out.println("ERROR: Could not load signer.");
                        debugErr(e);
                        if (!TestUtil.isTesting()) {
                            System.exit(2);
                        } else {
                            return;
                        }
                    }
                }
            }

            // read the args
            tool.setName(args[args.length - 4]);
            tool.setPassphrase(args[args.length - 3]);
            try {
                tool.setValidDays(Integer.valueOf(args[args.length - 2]));
            } catch (Exception e) {
                System.out.println("ERROR: Invalid value for # of valid days.");
                debugErr(e);
                if (!TestUtil.isTesting()) {
                    System.exit(2);
                } else {
                    return;
                }
            }
            tool.setSaveFile(new File(args[args.length - 1]));

            // execute
            tool.makeCertificate();

            if (!TestUtil.isTesting()) {
                System.exit(0);
            } else {
                return;
            }
        } catch (Exception e) {
            debugErr(e);
            if (!TestUtil.isTesting()) {
                System.exit(1);
            } else {
                return;
            }
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        MakeUserZipFileTool.debug = debug;


    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static final boolean isDebug() {
        return debug;


    }

    /**
     *
     * @param line
     */
    private static final void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(MakeUserZipFileTool.class.getName() + "> " + line);
        }


    }

    /**
     *
     * @param e
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);

        }
    }
}
