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
package org.tranche.security;

import org.tranche.users.User;
import org.tranche.logs.LogUtil;
import org.tranche.hash.Base64;
import org.tranche.hash.Base16;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.tranche.ConfigureTranche;
import org.tranche.exceptions.PassphraseRequiredException;
import org.tranche.users.UserCertificateUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>A helper class for handling security related tasks. You can also find helper methods here for looking up default X.509 certificates and RSA keys.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SecurityUtil {

    private static boolean debug = false;
    private static String adminCertificateLocation = "/org/tranche/test/admin.public.certificate",  writeCertificateLocation = "/org/tranche/test/write.public.certificate",  userCertificateLocation = "/org/tranche/test/user.public.certificate",  readCertificateLocation = "/org/tranche/test/read.public.certificate",  autocertCertificateLocation = "/org/tranche/test/autocert.public.certificate",  anonCertificateLocation = "/org/tranche/test/anonymous.public.certificate",  anonPrivateKeyLocation = "/org/tranche/test/anonymous.private.key",  emailCertificateLocation = "/org/tranche/test/email.public.certificate",  emailPrivateKeyLocation = "/org/tranche/test/email.private.key";
    private static boolean uninitialized = true;
    private static X509Certificate defaultCertificate = null;
    private static PrivateKey defaultKey = null;
    /**
     * <p>Size of signature in bytes, used by buffer.</p>
     */
    public static final int SIGNATURE_BUFFER_SIZE = 10000;

    /**
     * <p>Create a hash using a particular hashing algorithm.</p>
     * @param f
     * @param algorithm
     * @param buf
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] hash(File f, String algorithm, byte[] buf) throws IOException, GeneralSecurityException {
        FileInputStream fis = new FileInputStream(f);
        byte[] hash = null;

        // try to make the hash
        try {
            hash = hash(fis, algorithm, buf);
        } finally {
            fis.close();
        }

        // return the hash
        return hash;
    }

    /**
     * <p>Create a hash using a particular hashing algorithm.</p>
     * @param f
     * @param algorithm
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] hash(File f, String algorithm) throws IOException, GeneralSecurityException {
        return hash(f, algorithm, new byte[SIGNATURE_BUFFER_SIZE]);
    }

    /**
     * <p>Helper method to make MD5 hashes digestable and file-name friendly. This encoding represents every 4 bits as 0-f.</p>
     * @param bytes
     * @return
     */
    public static String encodeBytes(byte[] bytes) {
        StringWriter name = new StringWriter();
        for (int i = 0; i < bytes.length; i++) {
            int a = 0xf & bytes[i];
            name.write(encodeByte((byte) a));
            int b = 0xf & (bytes[i] >> 4);
            name.write(encodeByte((byte) b));
        }
        return name.toString();
    }

    /**
     * <p>Encodes a byte as a character. Only encodes byte if 0-15, inclusive. Throws a RuntimeException if not in aforementioned range.</p>
     * @param b
     * @return
     */
    public static char encodeByte(byte b) {
        switch (b) {
            case (byte) 0:
                return '0';
            case (byte) 1:
                return '1';
            case (byte) 2:
                return '2';
            case (byte) 3:
                return '3';
            case (byte) 4:
                return '4';
            case (byte) 5:
                return '5';
            case (byte) 6:
                return '6';
            case (byte) 7:
                return '7';
            case (byte) 8:
                return '8';
            case (byte) 9:
                return '9';
            case (byte) 10:
                return 'a';
            case (byte) 11:
                return 'b';
            case (byte) 12:
                return 'c';
            case (byte) 13:
                return 'd';
            case (byte) 14:
                return 'e';
            case (byte) 15:
                return 'f';
            default:
                throw new RuntimeException("Can't have a byte outside the range 0-15.");
        }

    }

    /**
     * <p>Generate a random base64 password of specified size.</p>
     * @param size
     * @return
     */
    public static String generateBase64Password(int size) {
        byte[] bytes = new byte[size];
        RandomUtil.getBytes(bytes);
        String encoded = Base64.encodeBytes(bytes);
        return encoded.substring(0, size);
    }

    /**
     * <p>Generate a random base64 password 20 characters long.</p>
     * @return
     */
    public static String generateBase64Password() {
        return generateBase64Password(20);
    }

    /**
     * <p>Helper method that uses the bouncycastle.org's X509 certificate generator to make a certificate for the given public/private key pair.</p>
     * @param name
     * @param pub
     * @param priv
     * @return
     * @throws java.security.GeneralSecurityException
     */
    public static Certificate createCertificate(String name, PublicKey pub, PrivateKey priv) throws GeneralSecurityException {
        lazyLoad();
        
        // make a new certificate
        X509V1CertificateGenerator gen = new X509V1CertificateGenerator();

        Hashtable attrs = new Hashtable();
        attrs.put(X509Principal.CN, name);
        attrs.put(X509Principal.OU, "Default DFS Website");
        attrs.put(X509Principal.O, "Certificate Auto-Generator");
        attrs.put(X509Principal.L, "Ann Arbor");
        attrs.put(X509Principal.ST, "Michigan");
        attrs.put(X509Principal.C, "US");

        Date firstDate = new Date();
        // Set back startdate ten minutes to avoid some problems with wrongly set clocks.
        firstDate.setTime(firstDate.getTime() - 10 * 60 * 1000);
        Date lastDate = new Date();
        // validity in days = validity*24*60*60*1000 milliseconds
        lastDate.setTime(lastDate.getTime() + (60 * (24 * 60 * 60 * 1000)));

        // Serialnumber is random bits, where random generator is initialized with Date.getTime()
        byte[] serno = new byte[8];
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed((new Date().getTime()));
        random.nextBytes(serno);
        BigInteger sn = new java.math.BigInteger(serno).abs();

        // make the principle
        X509Principal principal = new X509Principal(attrs);

        //generate cert
        gen.setSerialNumber(sn);
        gen.setIssuerDN(principal);
        gen.setNotBefore(firstDate);
        gen.setNotAfter(lastDate);
        gen.setSubjectDN(principal);
        gen.setSignatureAlgorithm("SHA1WITHRSA");
        gen.setPublicKey(pub);

        return gen.generateX509Certificate(priv);
    }

    /**
     * <p>Sign a file using a private key. Uses key's signature algorithm to sign.</p>
     * @param f
     * @param key
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(File f, PrivateKey key) throws IOException, GeneralSecurityException {
        return sign(f, key, SecurityUtil.getSignatureAlgorithm(key));
    }

    /**
     * <p>Sign a file using a private key. Uses key's signature algorithm to sign.</p>
     * @param f
     * @param key
     * @param buf
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(File f, PrivateKey key, byte[] buf) throws IOException, GeneralSecurityException {
        return sign(f, key, SecurityUtil.getSignatureAlgorithm(key), buf);
    }

    /**
     * <p>Sign a file using a private key and a specified algorithm.</p>
     * @param f
     * @param key
     * @param algorithm
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(File f, PrivateKey key, String algorithm) throws IOException, GeneralSecurityException {
        byte[] buffer = new byte[1000];
        return sign(f, key, algorithm, buffer);
    }

    /**
     * <p>Sign a file using a private key and a specified algorithm.</p>
     * @param f
     * @param key
     * @param algorithm
     * @param buffer
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(File f, PrivateKey key, String algorithm, byte[] buffer) throws IOException, GeneralSecurityException {
        FileInputStream fis = null;
        byte[] bytes = null;
        Exception exception = null;
        try {
            fis = new FileInputStream(f);
            bytes = sign(fis, key, algorithm, buffer);
        } catch (Exception e) {
            exception = e;
        } finally {
            try {
                fis.close();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        if (exception != null) {
            if (exception instanceof IOException) {
                throw (IOException) exception;
            }
            if (exception instanceof GeneralSecurityException) {
                throw (GeneralSecurityException) exception;
            }
        }
        return bytes;
    }

    /**
     * <p>Sign data from an InputStream using a private key. Uses key's signature algorithm.</p>
     * @param is
     * @param key
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(InputStream is, PrivateKey key) throws IOException, GeneralSecurityException {
        return sign(is, key, SecurityUtil.getSignatureAlgorithm(key));
    }

    /**
     * <p>Sign data from an InputStream using a private key and a specific algorithm.</p>
     * @param is
     * @param key
     * @param algorithm
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] sign(InputStream is, PrivateKey key, String algorithm) throws IOException, GeneralSecurityException {
        // make a buffer
        byte[] buf = new byte[SIGNATURE_BUFFER_SIZE];
        return sign(is, key, algorithm, buf);
    }

    /**
     * <p>Sign data from an InputStream using a private key and a specific algorithm.</p>
     * <p>After profiling this was hot-spot that dominated the AddFileTool's time. The only speed improvement to make is allowing a reusable buffer of bytes.</p>
     * @param is
     * @param key
     * @param algorithm
     * @param buf
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final byte[] sign(InputStream is, PrivateKey key, String algorithm, byte[] buf) throws IOException, GeneralSecurityException {
        // get something that can sign
        java.security.Signature sig = java.security.Signature.getInstance(algorithm);
        // init the signer
        sig.initSign(key);
        // sign the file's contents
        for (int bytesRead = is.read(buf); bytesRead > 0; bytesRead = is.read(buf)) {
            sig.update(buf, 0, bytesRead);
        }

        //return the signature
        return sig.sign();
    }

    /**
     * <p>Create a hash using a particular hashing algorithm.</p>
     * @param bytes
     * @param algorithm
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] hash(byte[] bytes, String algorithm) throws IOException, GeneralSecurityException {
        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(bytes);
            return hash(bais, algorithm);
        } finally {
            IOUtil.safeClose(bais);
        }
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    /**
     * <p>Create a hash using a particular hashing algorithm.</p>
     * @param is
     * @param algorithm
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] hash(InputStream is, String algorithm) throws IOException, GeneralSecurityException {
        return hash(is, algorithm, new byte[SIGNATURE_BUFFER_SIZE]);
    }

    /**
     * <p>Lazy load resources used by utility methods.</p>
     */
    public static void lazyLoad() {
        if (uninitialized) {
            Security.addProvider(new BouncyCastleProvider());
            uninitialized = false;
        }
    }

    /**
     * <p>Create a hash using a particular hashing algorithm.</p>
     * @param is
     * @param algorithm
     * @param buf
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] hash(InputStream is, String algorithm, byte[] buf) throws IOException, GeneralSecurityException {
        lazyLoad();
        // get something that can sign
        MessageDigest md = MessageDigest.getInstance(algorithm, "BC");
        // hash the file's contents
        for (int bytesRead = is.read(buf); bytesRead > 0; bytesRead = is.read(buf)) {
            md.update(buf, 0, bytesRead);
        }

        //return the hash bytes
        return md.digest();
    }

//     /**
//      * Helper method to sign a file using a particular keystone, alias, and password.
//      */
//     public static void sign(String file, String keystore, String keystorePassword, String alias, String aliasPassword)throws IOException, GeneralSecurityException {
//         byte[] digitalSignature = signBytes(file, keystore, keystorePassword, alias, aliasPassword);
//
//         // save the signature
//         FileOutputStream fos = new FileOutputStream(file+".sha");
//         fos.write(digitalSignature);
//         fos.flush();
//         fos.close();
//     }
//
//     public static byte[] signBytes(String file, String keystore, String keystorePassword, String alias, String aliasPassword)throws IOException, GeneralSecurityException {
//         FileInputStream fis = new FileInputStream(file);
//         byte[] sig = signBytes(fis, keystore, keystorePassword, alias, aliasPassword);
//         fis.close();
//
//         return sig;
//     }
//
//     public static byte[] signBytes(InputStream is, String keystore, String keystorePassword, String alias, String aliasPassword)throws IOException, GeneralSecurityException {
//         // get the private key
//         PrivateKey privateKey = getPrivateKeyFromKeyStore(keystore, keystorePassword, alias, aliasPassword);
//
//         // sign the jar
//         Signature sig = Signature.getInstance("SHA1withDSA");
//         sig.initSign(privateKey);
//         // buffer for speed
//         byte[] buf = new byte[512];
//         // update with jar contents
//         for (int bytesRead = is.read(buf);bytesRead>0;bytesRead=is.read(buf)){
//             sig.update(buf, 0, bytesRead);
//         }
//         // get the signature
//         byte[] digitalSignature = sig.sign();
//
//         return digitalSignature;
//     }
//
//
    /**
     * <p>Retrieve the PrivateKey (used to sign bytes) from the system keystore.</p>
     * @param keystore
     * @param keystorePassword
     * @param alias
     * @param aliasPassword
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static PrivateKey getPrivateKeyFromKeyStore(String keystore, String keystorePassword, String alias, String aliasPassword) throws IOException, GeneralSecurityException {
        FileInputStream fis = new FileInputStream(keystore);
        PrivateKey key = null;
        try {
            key = getPrivateKeyFromKeyStore(fis, keystorePassword, alias, aliasPassword);
        } catch (Exception e) {
            LogUtil.logError(e);
        }
        fis.close();

        return key;
    }

    /**
     * <p>Retrieve the PrivateKey (used to sign bytes) from the system keystore.</p>
     * @param keystore
     * @param keystorePassword
     * @param alias
     * @param aliasPassword
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static PrivateKey getPrivateKeyFromKeyStore(InputStream keystore, String keystorePassword, String alias, String aliasPassword) throws IOException, GeneralSecurityException {
        // check the signature on the jar
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(keystore, new String(keystorePassword).toCharArray());

        // get the private
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, new String(aliasPassword).toCharArray());

        // return the private key
        return privateKey;
    }
//     /**
//      * Helper method to get a private key from a KeyStore.
//      */
//     public static Certificate getCertificateFromKeyStore(String keystore, String keystorePassword, String alias) throws IOException, GeneralSecurityException {
//         // check the signature on the jar
//         KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
//         ks.load(new FileInputStream(keystore), new String(keystorePassword).toCharArray());
//
//         // get the private
//         Certificate cert = (Certificate)ks.getCertificate(alias);
//
//         // return the private key
//         return cert;
//     }
    private static X509Certificate adminCert = null,  userCert = null,  readOnlyCert = null,  writeOnlyCert = null,  autoCert = null,  anonCert = null,  emailCert = null;
    private static PrivateKey anonKey = null,  emailKey = null;

    /**
     * <p>Returns certificate with priveldges: read only</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getAnonymousCertificate() throws IOException, GeneralSecurityException {
        if (anonCert == null) {
            anonCert = getCertificate(ConfigureTranche.openStreamToFile(anonCertificateLocation));
        }
        // return the certificate
        return anonCert;
    }

    /**
     * <p>Returns key with priveldges: read only</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final PrivateKey getAnonymousKey() throws IOException, GeneralSecurityException {
        if (anonKey == null) {
            // buffer the bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = ConfigureTranche.openStreamToFile(anonPrivateKeyLocation);
            IOUtil.getBytes(is, baos);
            // close IO
            is.close();
            // return the key
            anonKey = getPrivateKey(baos.toByteArray());
        }
        return anonKey;
    }

    /**
     * <p>Returns certificate used for signing email to be sent from server.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getEmailCertificate() throws IOException, GeneralSecurityException {
        if (emailCert == null) {
            emailCert = getCertificate(ConfigureTranche.openStreamToFile(emailCertificateLocation));
        }
        // return the certificate
        return emailCert;
    }

    /**
     * <p>Returns key used for signing email to be sent from server.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final PrivateKey getEmailKey() throws IOException, GeneralSecurityException {
        if (emailKey == null) {
            // buffer the bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = ConfigureTranche.openStreamToFile(emailPrivateKeyLocation);
            IOUtil.getBytes(is, baos);
            // close IO
            is.close();
            // return the key
            emailKey = getPrivateKey(baos.toByteArray());
        }
        return emailKey;
    }

    /**
     * <p>Returns certificate with priveldges: all</p>
     * <p>Must have matching key to sign bytes.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getAdminCertificate() throws IOException, GeneralSecurityException {
        // Lazy load
        if (adminCert == null) {
            adminCert = getCertificate(ConfigureTranche.openStreamToFile(adminCertificateLocation));
        }

        return adminCert;
    }

    /**
     * <p>Returns certificate with priveldges: read, write, delete (not set configuration)</p>
     * <p>Must have matching key to sign bytes.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getUserCertificate() throws IOException, GeneralSecurityException {
        // Lazy load
        if (userCert == null) {
            userCert = getCertificate(ConfigureTranche.openStreamToFile(userCertificateLocation));
        }

        return userCert;
    }

    /**
     * <p>Returns certificate with priveldges: read only</p>
     * <p>Must have matching key to sign bytes.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getReadOnlyCertificate() throws IOException, GeneralSecurityException {
        // Lazy load
        if (readOnlyCert == null) {
            readOnlyCert = getCertificate(ConfigureTranche.openStreamToFile(readCertificateLocation));
        }

        return readOnlyCert;
    }

    /**
     * <p>Returns certificate with priveldges: write</p>
     * <p>Must have matching key to sign bytes.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getWriteOnlyCertificate() throws IOException, GeneralSecurityException {
        // Lazy load
        if (writeOnlyCert == null) {
            writeOnlyCert = getCertificate(ConfigureTranche.openStreamToFile(writeCertificateLocation));
        }
        return writeOnlyCert;
    }

    /**
     * <p>Returns certificate with priveldges: read, write (no delete, no write configuration)</p>
     * <p>Must have matching key to sign bytes.</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getAutoCertCertificate() throws IOException, GeneralSecurityException {
        // Lazy load
        if (autoCert == null) {
            autoCert = getCertificate(ConfigureTranche.openStreamToFile(autocertCertificateLocation));
        }
        return autoCert;
    }

    /**
     * <p>Get User object representing adminstrative privileges. (Cannot sign without private key.)</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final User getAdmin() throws IOException, GeneralSecurityException {
        // the user
        User user = new User();
        user.setCertificate(getAdminCertificate());
        user.setFlags(User.ALL_PRIVILEGES);
        // return the user
        return user;
    }

    /**
     * <p>Get User object representing user privileges (read, write, delete). (Cannot sign without private key.)</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final User getUser() throws IOException, GeneralSecurityException {
        // the user
        User user = new User();
        user.setCertificate(getUserCertificate());
        user.setFlags(getUserFlags());
        // return the user
        return user;
    }

    /**
     * <p>Returns integer for flags used to set for user: (super) user.</p>
     * @return
     */
    public static final int getUserFlags() {
        return User.CAN_DELETE_DATA | User.CAN_DELETE_META_DATA | User.CAN_GET_CONFIGURATION | User.CAN_SET_DATA | User.CAN_SET_META_DATA | User.VERSION_ONE;
    }

    /**
     * <p>Get User object representing read-only privileges. (Cannot sign without private key.)</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final User getReadOnly() throws IOException, GeneralSecurityException {
        // the user
        User user = new User();
        user.setCertificate(getReadOnlyCertificate());
        user.setFlags(getReadOnlyFlags());
        // return the user
        return user;
    }

    /**
     * <p>Returns integer for flags used to set for user: read-only.</p>
     * @return
     */
    public static final int getReadOnlyFlags() {
        return User.CAN_GET_CONFIGURATION | User.VERSION_ONE;
    }

    /**
     * <p>Get User object representing anonymous, read-only privileges. (Cannot sign without private key.)</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final User getAnonymous() throws IOException, GeneralSecurityException {
        User user = new User();
        user.setCertificate(getAnonymousCertificate());
        // anonymous users have the same allowance as read-only
        user.setFlags(getAnonymousFlags());
        return user;
    }

    /**
     * <p>Returns integer for flags used to set for user: anonymous.</p>
     * @return
     */
    public static final int getAnonymousFlags() {
        return User.VERSION_ONE;
    }

    /**
     * <p>Get User object representing write-only privileges. (Cannot sign without private key.)</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final User getWriteOnly() throws IOException, GeneralSecurityException {
        // the user
        User user = new User();
        user.setCertificate(getWriteOnlyCertificate());
        user.setFlags(getWriteOnlyFlags());
        // return the user
        return user;
    }

    /**
     * <p>Returns integer for flags used to set for user: write-only.</p>
     * @return
     */
    public static final int getWriteOnlyFlags() {
        return User.CAN_GET_CONFIGURATION | User.CAN_SET_DATA | User.CAN_SET_META_DATA | User.VERSION_ONE;
    }

    /**
     * <p>Get User object representing auto-certificate privileges (read, write). (Cannot sign without private key.)</p>
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final User getAutoCert() throws IOException, GeneralSecurityException {
        // the user
        User user = new User();
        user.setCertificate(getAutoCertCertificate());
        user.setFlags(getAutoCertFlags());
        // return the user
        return user;
    }

    /**
     * <p>Returns integer for flags used to set for user: auto-cert.</p>
     * @return
     */
    public static final int getAutoCertFlags() {
        return User.CAN_GET_CONFIGURATION | User.CAN_SET_DATA | User.CAN_SET_META_DATA | User.VERSION_ONE;
    }

    /**
     * <p>Returns X509Certificate object serialized into bytes.</p>
     * @param bytes
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getCertificate(byte[] bytes) throws IOException, GeneralSecurityException {
        return getCertificate(new ByteArrayInputStream(bytes));
    }

    /**
     * <p>Helper method to load an X509 certificate from an input stream.</p>
     * @param in
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static final X509Certificate getCertificate(InputStream in) throws IOException, GeneralSecurityException {
        lazyLoad();

        // make an X509 facroty
        CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");

        if (in == null) {
            throw new IOException("Certificate stream is null!");
        }

        // get the NRPP certificate
        X509Certificate cert = (X509Certificate) cf.generateCertificate(in);

        // return the certificate
        return cert;
    }

    /**
     * <p>Helper method to load an X509 certificate from a file.</p>
     * @param file
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static X509Certificate getCertificate(File file) throws IOException, GeneralSecurityException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return getCertificate(fis);
        } finally {
            IOUtil.safeClose(fis);
        }
    }

    /**
     * <p>Verify that the contents of an InputStream's bytes were signed by a certificate using a particular algorithm.</p>
     * @param is
     * @param digitalSignature
     * @param algorithm
     * @param cert
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static boolean verify(InputStream is, byte[] digitalSignature, String algorithm, Certificate cert) throws IOException, GeneralSecurityException {
        return verify(is, digitalSignature, algorithm, cert.getPublicKey());
    }

    /**
     * <p>Verify that the contents of an InputStream's bytes were signed by a public key using a particular algorithm.</p>
     * @param is
     * @param digitalSignature
     * @param algorithm
     * @param publicKey
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static boolean verify(InputStream is, byte[] digitalSignature, String algorithm, PublicKey publicKey) throws IOException, GeneralSecurityException {
        // decrypt the signature to verify
        Signature verify = Signature.getInstance(algorithm);
        verify.initVerify(publicKey);
        byte[] buf = new byte[512];
        // update with jar contents
        for (int bytesRead = is.read(buf); bytesRead > 0; bytesRead = is.read(buf)) {
            verify.update(buf, 0, bytesRead);
        }
        // return if it was valid
        return verify.verify(digitalSignature);
    }

    /**
     * <p>Encrypts a file using AES and a passphrase.</p>
     * @param passphrase
     * @param file
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static File encrypt(String passphrase, File file) throws IOException, GeneralSecurityException {
        return encrypt(passphrase, new byte[8], 1000, file);
    }

    /**
     * <p>Encrypts a file using AES and a passphrase.</p>
     * @param passphrase
     * @param salt
     * @param iterations
     * @param file
     * @return
     * @throws java.io.IOException
     */
    public static File encrypt(String passphrase, byte[] salt, int iterations, File file) throws IOException {
        // make the AES encryption engine
        AESFastEngine encrypt = new AESFastEngine();
        // make up some params
        PKCS5S2ParametersGenerator pg = new PKCS5S2ParametersGenerator();
        pg.init(passphrase.getBytes(), salt, iterations);
        CipherParameters params = pg.generateDerivedParameters(256);
        // initialize
        encrypt.init(true, params);
        int blockSize = encrypt.getBlockSize();

        // read the file and encrypt it
        File encryptedFile = TempFileUtil.createTemporaryFile();
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        java.io.BufferedOutputStream bos = null;
        try {
            // initialize streams
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            fos = new FileOutputStream(encryptedFile);
            bos = new java.io.BufferedOutputStream(fos);

            // make the buffers
            byte[] data = new byte[blockSize];
            byte[] encrypted = new byte[blockSize];

            // encrypt all the data
            int bytesRead = 0;
            for (bytesRead = bis.read(data); bytesRead == blockSize; bytesRead = bis.read(data)) {
                encrypt.processBlock(data, 0, encrypted, 0);
                // write the data
                bos.write(encrypted);
            }
            if (bytesRead == -1) {
                bytesRead = 0;
            }
            // padd the rest using method#2 recommended by PKCS#5 add x bytes with a value of x.
            int paddingLength = data.length - bytesRead;
            for (int i = bytesRead; i < data.length; i++) {
                data[i] = (byte) (0xff & paddingLength);
            }
            // process the data
            encrypt.processBlock(data, 0, encrypted, 0);
            bos.write(encrypted);

            // return the file
            return encryptedFile;
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
            IOUtil.safeClose(bos);
            IOUtil.safeClose(fos);
        }
    }

    /**
     * <p>Decrypt an AES-encrypted file using a specified passphrase.</p>
     * @param passphrase
     * @param file
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static File decrypt(String passphrase, File file) throws IOException, GeneralSecurityException {
        return decrypt(passphrase, new byte[8], 1000, file);
    }

    /**
     * <p>Decrypt an AES-encrypted file using a specified passphrase.</p>
     * @param passphrase
     * @param is
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static ByteArrayOutputStream decrypt(String passphrase, InputStream is) throws IOException, GeneralSecurityException {
        return decrypt(passphrase, new byte[8], 1000, is);
    }

    /**
     * <p>Decrypt an AES-encrypted file using a specified passphrase.</p>
     * @param passphrase
     * @param salt
     * @param iterations
     * @param is
     * @return
     * @throws java.io.IOException
     */
    public static ByteArrayOutputStream decrypt(String passphrase, byte[] salt, int iterations, InputStream is) throws IOException {
        if (passphrase == null) {
            throw new PassphraseRequiredException("Can't decrypt file. No passphrase specified.");
        }
        // make the AES encryption engine
        AESFastEngine encrypt = new AESFastEngine();
        // make up some params
        PKCS5S2ParametersGenerator pg = new PKCS5S2ParametersGenerator();
        pg.init(passphrase.getBytes(), salt, iterations);
        CipherParameters params = pg.generateDerivedParameters(256);
        // initialize
        encrypt.init(false, params);
        int blockSize = encrypt.getBlockSize();

        // make the IO
        BufferedInputStream bis = null;
        ByteArrayOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            // initialize streams
            bis = new BufferedInputStream(is);
            fos = new ByteArrayOutputStream();
            bos = new BufferedOutputStream(fos);

            // make the buffers
            byte[] data = new byte[blockSize];
            byte[] encrypted = new byte[blockSize];
            byte[] encryptedBuffer = new byte[blockSize];
            boolean firstRound = true;

            // encrypt all the data
            int offset = 0;
            for (int bytesRead = bis.read(data, offset, data.length - offset); bytesRead != -1; bytesRead = bis.read(data, offset, data.length - offset)) {
                // check for bytes read
                if (bytesRead + offset != data.length) {
                    offset += bytesRead;
                    continue;
                }
                offset = 0;

                // if not the first round, write it
                encrypt.processBlock(data, 0, encrypted, 0);
                // write the data
                if (!firstRound) {
                    bos.write(encryptedBuffer);
                    System.arraycopy(encrypted, 0, encryptedBuffer, 0, encrypted.length);
                } else {
                    System.arraycopy(encrypted, 0, encryptedBuffer, 0, encrypted.length);
                    firstRound = false;
                }
            }
            // take the last block and remove padding
            int paddingLength = (int) (0xff & encryptedBuffer[encryptedBuffer.length - 1]);
            if (paddingLength < 0 || paddingLength > encryptedBuffer.length) {
                throw new RuntimeException("Invalid CBC encoding at the end of the encrypted file! Please check that you used the correct passphrase.");
            }
            bos.write(encryptedBuffer, 0, encrypted.length - paddingLength);

            // return the file
            return fos;
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(is);
            IOUtil.safeClose(bos);
            IOUtil.safeClose(fos);
        }
    }

    /**
     * <p>Decrypt an AES-encrypted file using a specified passphrase.</p>
     * @param passphrase
     * @param salt
     * @param iterations
     * @param file
     * @return
     * @throws java.io.IOException
     */
    public static File decrypt(String passphrase, byte[] salt, int iterations, File file) throws IOException {
        // make the AES encryption engine
        AESFastEngine encrypt = new AESFastEngine();
        // make up some params
        PKCS5S2ParametersGenerator pg = new PKCS5S2ParametersGenerator();
        pg.init(passphrase.getBytes(), salt, iterations);
        CipherParameters params = pg.generateDerivedParameters(256);
        // initialize
        encrypt.init(false, params);
        int blockSize = encrypt.getBlockSize();

        // read the file and encrypt it
        File encryptedFile = TempFileUtil.createTemporaryFile();
        // make the IO
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            // initialize streams
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            fos = new FileOutputStream(encryptedFile);
            bos = new BufferedOutputStream(fos);

            // make the buffers
            byte[] data = new byte[blockSize];
            byte[] encrypted = new byte[blockSize];
            byte[] encryptedBuffer = new byte[blockSize];
            boolean firstRound = true;

            // encrypt all the data
            int offset = 0;
            for (int bytesRead = bis.read(data, offset, data.length - offset); bytesRead != -1; bytesRead = bis.read(data, offset, data.length - offset)) {
                // check for bytes read
                if (bytesRead + offset != data.length) {
                    offset += bytesRead;
                    continue;
                }
                offset = 0;

                // if not the first round, write it
                encrypt.processBlock(data, 0, encrypted, 0);
                // write the data
                if (!firstRound) {
                    bos.write(encryptedBuffer);
                    System.arraycopy(encrypted, 0, encryptedBuffer, 0, encrypted.length);
                } else {
                    System.arraycopy(encrypted, 0, encryptedBuffer, 0, encrypted.length);
                    firstRound = false;
                }
            }
            // take the last block and remove padding
            int paddingLength = (int) (0xff & encryptedBuffer[encryptedBuffer.length - 1]);
            if (paddingLength < 0 || paddingLength > encryptedBuffer.length) {
//                throw new RuntimeException("Invalid CBC encoding at the end of the encrypted file! Can't have "+paddingLength+" bytes of padding.");
                throw new RuntimeException("Invalid CBC encoding at the end of the encrypted file! Please check that you used the correct passphrase.");
            }
            bos.write(encryptedBuffer, 0, encrypted.length - paddingLength);

            // return the file
            return encryptedFile;
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
            IOUtil.safeClose(bos);
            IOUtil.safeClose(fos);
        }
    }

    /**
     * <p>Helper method to convert certificate's into unique names.</p>
     * @param cert
     * @return
     */
    public static String getMD5Name(X509Certificate cert) {
        try {
            byte[] md5 = SecurityUtil.hash(cert.getEncoded(), "md5");
            return Base16.encode(md5);
        } catch (Exception e) {
            // noop
        }
        throw new RuntimeException("Can't create MD5 name for certificate.");
    }

    /**
     * <p>Extract the signature algorithm used with specified PublicKey.</p>
     * @param key
     * @return
     */
    public static String getSignatureAlgorithm(PublicKey key) {
        String signatureAlgorithm = "SHA1withRSA";
        if (key instanceof DSAPublicKey) {
            signatureAlgorithm = "SHA1withDSA";
        }
        return signatureAlgorithm;
    }

    /**
     * <p>Extract the signature algorithm used with specified PrivateKey.</p>
     * @param key
     * @return
     */
    public static String getSignatureAlgorithm(PrivateKey key) {
        String signatureAlgorithm = "SHA1withRSA";
        if (key instanceof DSAPrivateKey) {
            signatureAlgorithm = "SHA1withDSA";
        }
        return signatureAlgorithm;
    }

    /**
     * <p>Load the PrivateKey serialized to a file.</p>
     * @param file
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static PrivateKey getPrivateKey(File file) throws IOException, GeneralSecurityException {
        SecurityUtil.lazyLoad();

        // load the private key from disk
        byte[] keyBytes = IOUtil.getBytes(file);
        // use the other helper method
        return getPrivateKey(keyBytes);
    }

    /**
     * <p>Load the PrivateKey serialized to a byte array.</p>
     * @param keyBytes
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static PrivateKey getPrivateKey(byte[] keyBytes) throws IOException, GeneralSecurityException {
        SecurityUtil.lazyLoad();

        // load the key from the bytes
        PKCS8EncodedKeySpec newkspec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
        return kf.generatePrivate(newkspec);
    }

    /**
     * <p>Retrieve the default X.509 certificate used by tool. Unless configured otherwise, this is the anonymous certificate.</p>
     * @return
     */
    public static X509Certificate getDefaultCertificate() {
        if (defaultCertificate == null) {
            try {
                defaultCertificate = getAnonymousCertificate();
            } catch (Exception e) {
            }
        }
        return defaultCertificate;
    }

    /**
     * <p>Set the default X.509 certificate used by the tool. If not specified, uses the anonymous certificate.</p>
     * @param aDefaultCertificate
     */
    public static void setDefaultCertificate(X509Certificate aDefaultCertificate) {
        defaultCertificate = aDefaultCertificate;
    }

    /**
     * <p>Retrieve the default private key used by the tool. If not specified, uses the anonymous key.</p>
     * @return
     */
    public static PrivateKey getDefaultKey() {
        if (defaultKey == null) {
            try {
                defaultKey = getAnonymousKey();
            } catch (Exception e) {
            }
        }
        return defaultKey;
    }

    /**
     * <p>Set the default private key used by the tool. If not specified, uses the anonymous key.</p>
     * @param aDefaultKey
     */
    public static void setDefaultKey(PrivateKey aDefaultKey) {
        defaultKey = aDefaultKey;
    }

    /**
     * <p>In-memory version of encryption function. This method avoids all uses of temporary files, which can save some time when handling lots of small files.</p>
     * @param passphrase
     * @param dataBytes
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] encrypt(String passphrase, byte[] dataBytes) throws IOException, GeneralSecurityException {
        return encrypt(passphrase, new byte[8], 1000, dataBytes);
    }

    /**
     * <p>In-memory version of encryption function. This method avoids all uses of temporary files, which can save some time when handling lots of small files.</p>
     * @param passphrase
     * @param salt
     * @param iterations
     * @param dataBytes
     * @return
     * @throws java.io.IOException
     */
    public static byte[] encrypt(String passphrase, byte[] salt, int iterations, byte[] dataBytes) throws IOException {
        // make the AES encryption engine
        AESFastEngine encrypt = new AESFastEngine();
        // make up some params
        PKCS5S2ParametersGenerator pg = new PKCS5S2ParametersGenerator();
        pg.init(passphrase.getBytes(), salt, iterations);
        CipherParameters params = pg.generateDerivedParameters(256);
        // initialize
        encrypt.init(true, params);
        int blockSize = encrypt.getBlockSize();

        // read the file and encrypt it
        ByteArrayInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream fos = null;
        try {
            // initialize streams
            fis = new ByteArrayInputStream(dataBytes);
            bis = new BufferedInputStream(fis);
            fos = new ByteArrayOutputStream();

            // make the buffers
            byte[] data = new byte[blockSize];
            byte[] encrypted = new byte[blockSize];

            // encrypt all the data
            int bytesRead = 0;
            for (bytesRead = bis.read(data); bytesRead == blockSize; bytesRead = bis.read(data)) {
                encrypt.processBlock(data, 0, encrypted, 0);
                // write the data
                fos.write(encrypted);
            }
            if (bytesRead == -1) {
                bytesRead = 0;
            }
            // padd the rest using method#2 recommended by PKCS#5 add x bytes with a value of x.
            int paddingLength = data.length - bytesRead;
            for (int i = bytesRead; i < data.length; i++) {
                data[i] = (byte) (0xff & paddingLength);
            }
            // process the data
            encrypt.processBlock(data, 0, encrypted, 0);
            fos.write(encrypted);

            // return the file
            return fos.toByteArray();
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
            IOUtil.safeClose(fos);
        }
    }

    /**
     * <p>In-memory version of decryption function. This method avoids all uses of temporary files, which can save some time when handling lots of small files.</p>
     * @param passphrase
     * @param dataBytes
     * @return
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static byte[] decrypt(String passphrase, byte[] dataBytes) throws IOException, GeneralSecurityException {
        return decrypt(passphrase, new byte[8], 1000, dataBytes);
    }

    /**
     * <p>In-memory version of decryption function. This method avoids all uses of temporary files, which can save some time when handling lots of small files.</p>
     * @param passphrase
     * @param salt
     * @param iterations
     * @param dataBytes
     * @return
     * @throws java.io.IOException
     */
    public static byte[] decrypt(String passphrase, byte[] salt, int iterations, byte[] dataBytes) throws IOException {
        if (passphrase == null) {
            throw new PassphraseRequiredException("Can't decrypt file. No passphrase specified.");
        }
        // make the AES encryption engine
        AESFastEngine encrypt = new AESFastEngine();
        // make up some params
        PKCS5S2ParametersGenerator pg = new PKCS5S2ParametersGenerator();
        pg.init(passphrase.getBytes(), salt, iterations);
        CipherParameters params = pg.generateDerivedParameters(256);
        // initialize
        encrypt.init(false, params);
        int blockSize = encrypt.getBlockSize();

        // make the IO
        ByteArrayInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream fos = null;
        try {
            // initialize streams
            fis = new ByteArrayInputStream(dataBytes);
            bis = new BufferedInputStream(fis);
            fos = new ByteArrayOutputStream();

            // make the buffers
            byte[] data = new byte[blockSize];
            byte[] encrypted = new byte[blockSize];
            byte[] encryptedBuffer = new byte[blockSize];
            boolean firstRound = true;

            // encrypt all the data
            int offset = 0;
            for (int bytesRead = bis.read(data, offset, data.length - offset); bytesRead != -1; bytesRead = bis.read(data, offset, data.length - offset)) {
                // check for bytes read
                if (bytesRead + offset != data.length) {
                    offset += bytesRead;
                    continue;
                }
                offset = 0;

                // if not the first round, write it
                encrypt.processBlock(data, 0, encrypted, 0);
                // write the data
                if (!firstRound) {
                    fos.write(encryptedBuffer);
                    System.arraycopy(encrypted, 0, encryptedBuffer, 0, encrypted.length);
                } else {
                    System.arraycopy(encrypted, 0, encryptedBuffer, 0, encrypted.length);
                    firstRound = false;
                }
            }
            // take the last block and remove padding
            int paddingLength = (int) (0xff & encryptedBuffer[encryptedBuffer.length - 1]);
            if (paddingLength < 0 || paddingLength > encryptedBuffer.length) {
                throw new RuntimeException("Invalid CBC encoding at the end of the encrypted file! Please check that you used the correct passphrase.");
            }
            fos.write(encryptedBuffer, 0, encrypted.length - paddingLength);

            // return the file
            return fos.toByteArray();
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
            IOUtil.safeClose(fos);
        }
    }

    /**
     * <p>Checks to see whether certificate is signed by the loaded certificates.</p>
     * <p>Primary use case is to help decide on client's side whether to perform certain network-specific actions. The content will still be validated server-side.</p>
     * @param cert The certificate to check
     * @return True if signed by the default certs, false otherwise
     * @throws java.lang.Exception
     */
    public static boolean isCertificateSignedByDefaultCerts(X509Certificate cert) throws Exception {

        Set<X509Certificate> pcCerts = new HashSet();
        pcCerts.add(SecurityUtil.getAdminCertificate());
        pcCerts.add(SecurityUtil.getAutoCertCertificate());
        pcCerts.add(SecurityUtil.getReadOnlyCertificate());
        pcCerts.add(SecurityUtil.getUserCertificate());
        pcCerts.add(SecurityUtil.getWriteOnlyCertificate());

        for (X509Certificate pcCert : pcCerts) {
            // Check the issuers, using whatever is their default permissions. This is far more common.
            if (certificateNamesMatch(UserCertificateUtil.readUserName(pcCert), UserCertificateUtil.readIssuerName(cert))) {
                // check the issuer
                try {
                    cert.verify(pcCert.getPublicKey());
                    return true;
                } catch (Exception e) {
                    // noop -- bad signatures
                    e.printStackTrace(System.err);
                }
            }
        }

        return false;
    }

    /**
     * <p>A utility method to check whether two certificate names match.</p>
     * <p>Not sufficient for security, but a fast way to check whether found a matching cert name before more expensive security checks.</p>
     * @param a
     * @param b
     * @return
     */
    public static boolean certificateNamesMatch(String a, String b) {
        String[] partsA = a.split(", *");
        Arrays.sort(partsA);
        String[] partsB = b.split(", *");
        Arrays.sort(partsB);

        if (partsA.length != partsB.length) {
            return false;
        }

        // Every field should match
        for (int i = 0; i < partsA.length; i++) {
            if (!partsA[i].equals(partsB[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>Set the administrator X.509 certificate.</p>
     * @param cert
     */
    public static void setAdminCert(X509Certificate cert) {
        adminCert = cert;
    }

    /**
     * <p>Set the file path to the administrator X.509 certificate.</p>
     * @param adminCertificateLocation
     */
    public static void setAdminCertLocation(String adminCertificateLocation) {
        if (SecurityUtil.adminCertificateLocation == null || !SecurityUtil.adminCertificateLocation.equals(adminCertificateLocation)) {
            adminCert = null;
            SecurityUtil.adminCertificateLocation = adminCertificateLocation;
            debugOut("admin: " + adminCertificateLocation);
        }
    }

    /**
     * <p>Set the user (read, write, delete) X.509 certificate.</p>
     * @param cert
     */
    public static void setUserCert(X509Certificate cert) {
        userCert = cert;
    }

    /**
     * <p>Set the file path to the user (read, write, delete) X.509 certificate.</p>
     * @param userCertificateLocation
     */
    public static void setUserCertLocation(String userCertificateLocation) {
        if (SecurityUtil.userCertificateLocation == null || !SecurityUtil.userCertificateLocation.equals(userCertificateLocation)) {
            userCert = null;
            SecurityUtil.userCertificateLocation = userCertificateLocation;
            debugOut("user: " + userCertificateLocation);
        }
    }

    /**
     * <p>Set the write-only X.509 certificate.</p>
     * @param cert
     */
    public static void setWriteCert(X509Certificate cert) {
        writeOnlyCert = cert;
    }

    /**
     * <p>Set the file path to the write-only X.509 certificate.</p>
     * @param writeCertificateLocation
     */
    public static void setWriteCertLocation(String writeCertificateLocation) {
        if (SecurityUtil.writeCertificateLocation == null || !SecurityUtil.writeCertificateLocation.equals(writeCertificateLocation)) {
            writeOnlyCert = null;
            SecurityUtil.writeCertificateLocation = writeCertificateLocation;
            debugOut("write: " + writeCertificateLocation);
        }
    }

    /**
     * <p>Set the read-only X.509 certificate.</p>
     * @param cert
     */
    public static void setReadCert(X509Certificate cert) {
        readOnlyCert = cert;
    }

    /**
     * <p>Set the file path to the read-only X.509 certificate.</p>
     * @param readCertificateLocation
     */
    public static void setReadCertLocation(String readCertificateLocation) {
        if (SecurityUtil.readCertificateLocation == null || !SecurityUtil.readCertificateLocation.equals(readCertificateLocation)) {
            readOnlyCert = null;
            SecurityUtil.readCertificateLocation = readCertificateLocation;
            debugOut("read: " + readCertificateLocation);
        }
    }

    /**
     * <p>Set the auto-certificate (read, write) X.509 certificate.</p>
     * @param cert
     */
    public static void setAutoCertCert(X509Certificate cert) {
        autoCert = cert;
    }

    /**
     * <p>Set the file path to the read-only X.509 certificate.</p>
     * @param autocertCertificateLocation
     */
    public static void setAutocertCertLocation(String autocertCertificateLocation) {
        if (SecurityUtil.autocertCertificateLocation == null || !SecurityUtil.autocertCertificateLocation.equals(autocertCertificateLocation)) {
            SecurityUtil.autoCert = null;
            SecurityUtil.autocertCertificateLocation = autocertCertificateLocation;
            debugOut("auto: " + autocertCertificateLocation);
        }
    }

    /**
     * <p>Set the anonymous, read-only X.509 certificate.</p>
     * @param cert
     */
    public static void setAnonCert(X509Certificate cert) {
        anonCert = cert;
    }

    /**
     * <p>Set the anonymous, read-only private key.</p>
     * @param key
     */
    public static void setAnonKey(PrivateKey key) {
        anonKey = key;
    }

    /**
     * <p>Set the file path to the anonymous, read-only X.509 certificate.</p>
     * @param anonCertificateLocation
     */
    public static void setAnonCertLocation(String anonCertificateLocation) {
        if (SecurityUtil.anonCertificateLocation == null || !SecurityUtil.anonCertificateLocation.equals(anonCertificateLocation)) {
            anonCert = null;
            SecurityUtil.anonCertificateLocation = anonCertificateLocation;
            debugOut("anon (cert): " + anonCertificateLocation);
        }
    }

    /**
     * <p>Set the file path to the anonymous, read-only private key.</p>
     * @param anonPrivateKeyLocation
     */
    public static void setAnonKeyLocation(String anonPrivateKeyLocation) {
        if (SecurityUtil.anonPrivateKeyLocation == null || !SecurityUtil.anonPrivateKeyLocation.equals(anonPrivateKeyLocation)) {
            anonKey = null;
            SecurityUtil.anonPrivateKeyLocation = anonPrivateKeyLocation;
            debugOut("anon (key): " + anonCertificateLocation);
        }
    }

    /**
     * <p>Set the X.509 certificate used to sign email data to be sent by server.</p>
     * @param cert
     */
    public static void setEmailCert(X509Certificate cert) {
        emailCert = cert;
    }

    /**
     * <p>Set the private key used to sign email data to be sent by server.</p>
     * @param key
     */
    public static void setEmailKey(PrivateKey key) {
        emailKey = key;
    }

    /**
     * <p>Set the file path to the certificate used for signing email to be sent by server.</p>
     * @param emailCertificateLocation
     */
    public static void setEmailCertLocation(String emailCertificateLocation) {
        if (SecurityUtil.emailCertificateLocation == null || !SecurityUtil.emailCertificateLocation.equals(emailCertificateLocation)) {
            emailCert = null;
            SecurityUtil.emailCertificateLocation = emailCertificateLocation;
        }
    }

    /**
     * <p>Set the file path to the private key used for signing email to be sent by server.</p>
     * @param emailPrivateKeyLocation
     */
    public static void setEmailKeyLocation(String emailPrivateKeyLocation) {
        if (SecurityUtil.emailPrivateKeyLocation == null || !SecurityUtil.emailPrivateKeyLocation.equals(emailPrivateKeyLocation)) {
            emailKey = null;
            SecurityUtil.emailPrivateKeyLocation = emailPrivateKeyLocation;
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        SecurityUtil.debug = debug;
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
            DebugUtil.printOut(SecurityUtil.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param line
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
