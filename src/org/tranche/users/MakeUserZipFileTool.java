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
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Hashtable;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.tranche.time.TimeUtil;
import org.tranche.security.SecurityUtil;

/**
 * <p>This is a helper class that makes user files, i.e. X.509 certificates and 
 * assocated private keys. The information is saved in a password protected ZIP 
 * file. Default values are generated for everything except the file location. 
 * If you don't want to customize the user, you can simply specify a location 
 * for the file and invoke the makeCertificate() method.</p>
 * 
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class MakeUserZipFileTool {
    
    // make up a random name
    private String name = "Custom User - " + (int) (Math.random() * Integer.MAX_VALUE);
    private String organizationalUnit = "Tranche Participant";
    private String organization = "Tranche";
    private String location = "Ann Arbor";
    private String state = "Michigan";
    private String country = "US";
    private Date startDate = new Date(TimeUtil.getTrancheTimestamp());
    private long validDays = 14;    // track the signer
    private X509Certificate signerCertificate = null;
    private PrivateKey signerPrivateKey = null;    // the keypair
    private KeyPair kp = null;    // the passphrase
    private String passphrase = "";
    // the file to save to
    private File userFile = null;

    /**
     * 
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java -jar X509CertificateTool <options>\n");
            System.out.println("Options:");
            System.out.println("  name  The name that'll appear on the certificate. For websites this should be your website name.");
            return;
        }

        // use bouncy castle
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // make the tool
        MakeUserZipFileTool ct = new MakeUserZipFileTool();

        // load the parameters
        for (int i = 0; i < args.length - 1; i++) {
            // check for the name
            if (args[i].equals("--name")) {
                // get the name
                ct.setName(args[i + 1]);
            }
            // check for the name
            if (args[i].equals("--issuer")) {
                // load the issuer if appropriate
                String fileName = args[i + 1].replaceAll("\"", "");
                ct.setSignerCertificate(SecurityUtil.getCertificate(new File(fileName)));
            }
            // check for the name
            if (args[i].equals("--issuerKey")) {
                // load the issuer if appropriate
                String fileName = args[i + 1].replaceAll("\"", "");
                ct.setSignerPrivateKey(SecurityUtil.getPrivateKeyFromKeyStore(fileName, "password", "jfalkner", "password"));
            }
            // check for the key file
            if (args[i].equals("--issuerKeyFile")) {
                // load the issuer if appropriate
                String fileName = args[i + 1].replaceAll("\"", "");
                ct.setSignerPrivateKey(SecurityUtil.getPrivateKey(new File(fileName)));
            }
        }

        // make the certificate
        UserZipFile uzf = ct.makeCertificate();
//        makeFiles(cert, ct);

        System.out.println("User file saved as " + ct.getUserFile());
    }

//    public static void makeFiles(final X509Certificate cert, final X509CertificateTool ct) throws CertificateEncodingException, FileNotFoundException, IOException {
//        
//        {
//            System.out.println("Saving public certificate to 'public.certificate'");
//            // save the certificate
//            FileOutputStream pubOut = new FileOutputStream("public.certificate");
//            pubOut.write(cert.getEncoded());
//            pubOut.flush();
//            pubOut.close();
//        }
//        
//        {
//            System.out.println("Saving Base64 encoded public certificate to 'public.certificate.b64'");
//            // save the certificate
//            FileOutputStream pubOut = new FileOutputStream("public.certificate.b64");
//            pubOut.write(Base64.encodeBytes(cert.getEncoded()).getBytes());
//            pubOut.flush();
//            pubOut.close();
//        }
//        
//        System.out.println("Saving private key to 'private.key'");
//        // save the private key
//        FileOutputStream privOut = new FileOutputStream("private.key");
//        privOut.write(ct.getKp().getPrivate().getEncoded());
//        privOut.flush();
//        privOut.close();
//    }
    /**
     * <p>Executes the creation of the UserZipFile.</p>
     * @return
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.NoSuchProviderException
     * @throws java.security.SignatureException
     * @throws java.security.InvalidKeyException
     */
    public UserZipFile makeCertificate() throws NoSuchAlgorithmException, NoSuchProviderException, SignatureException, InvalidKeyException {
        // check for user file
        if (userFile == null) {
            throw new RuntimeException("Must specify a file to save the user information.");        // make up a new authority
        }
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        // make up a new RSA keypair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(1024);

        //ca key pair
        kp = keyGen.generateKeyPair();

        // set the private key
        PrivateKey privateKey = getKp().getPrivate();
        PublicKey publicKey = getKp().getPublic();
        // register bouncy castle
        Security.addProvider(new BouncyCastleProvider());

        // make a new certificate
        Hashtable attrs = new Hashtable();
        attrs.put(X509Principal.CN, getName());
        attrs.put(X509Principal.OU, getOrganizationalUnit());
        attrs.put(X509Principal.O, getOrganization());
        attrs.put(X509Principal.L, getLocation());
        attrs.put(X509Principal.ST, getState());
        attrs.put(X509Principal.C, getCountry());

        Date lastDate = new Date(startDate.getTime() + validDays * (24l * 60l * 60l * 1000l));

        // Serialnumber is random bits, where random generator is initialized with Date.getTime()
        byte[] serno = new byte[8];
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed((new Date().getTime()));
        random.nextBytes(serno);
        BigInteger sn = new java.math.BigInteger(serno).abs();

        // make the principle
        X509Principal principal = new X509Principal(attrs);

        //generate cert
        X509V3CertificateGenerator gen = new X509V3CertificateGenerator();
        gen.setSerialNumber(sn);
        // use the give issuer if appropriate
        if (getSignerCertificate() != null && getSignerPrivateKey() != null) {
            gen.setIssuerDN((X509Principal) getSignerCertificate().getSubjectDN());
        } else {
            gen.setIssuerDN(principal);
        }
        gen.setNotBefore(startDate);
        gen.setNotAfter(lastDate);
        gen.setSubjectDN(principal);
        gen.setSignatureAlgorithm("SHA1WITHRSA");
        gen.setPublicKey(publicKey);

        // make the certificate
        X509Certificate cert = null;
        if (getSignerCertificate() != null && getSignerPrivateKey() != null) {
            cert = gen.generateX509Certificate(getSignerPrivateKey());
        } else {
            cert = gen.generateX509Certificate(privateKey);
        }

        // make the user file
        UserZipFile uzf = new UserZipFile(getUserFile());
        uzf.setCertificate(cert);
        uzf.setPrivateKey(privateKey);
        uzf.setPassphrase(getPassphrase());
        uzf.saveTo(getUserFile());

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
     * <p>Gets the organizational unit.</p>
     * @return
     */
    public String getOrganizationalUnit() {
        return organizationalUnit;
    }

    /**
     * <p>Sets the certificate organizational unit.</p>
     * @param organizationalUnit
     */
    public void setOrganizationalUnit(String organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }

    /**
     * <p>Gets the certificate organization.</p>
     * @return
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * <p>Sets the certificate organization.</p>
     * @param organization
     */
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    /**
     * <p>Gets the certificate location.</p>
     * @return
     */
    public String getLocation() {
        return location;
    }

    /**
     * <p>Sets the certificate location.</p>
     * @param location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * <p>Gets the certificate state</p>
     * @return
     */
    public String getState() {
        return state;
    }

    /**
     * <p>Sets the certificate state</p>
     * @param state
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * <p>Gets the certificate country.</p>
     * @return
     */
    public String getCountry() {
        return country;
    }

    /**
     * <p>Sets the certificate country.</p>
     * @param country
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * <p>Gets the key pair.</p>
     * @return
     */
    public KeyPair getKp() {
        return kp;
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
        this.validDays = validDays;
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
    public File getUserFile() {
        return userFile;
    }

    /**
     * <p>A method to get the file that the user's information is or will be saved to. This can be any file on the local filesystem.</p>
     * @param userFile A File object representing the file with the user information.
     */
    public void setUserFile(File userFile) {
        this.userFile = userFile;
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
}