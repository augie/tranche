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

import org.tranche.hash.Base16;
import org.tranche.users.MakeUserZipFileTool;
import org.tranche.users.UserZipFile;
import java.io.File;
import java.io.FileOutputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TrancheTestCase;

/**
 * JUnit tests for the SecurityUtil class. Tests basic encryption and hashing code for the DFS project.
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class SecurityUtilTest extends TrancheTestCase {

    /**
     * Tests the 256 bit AES encryption with PKCS5 padding by encrypting and decrypting several different sets of data with varying lengths and various passphrases.
     * @throws java.lang.Exception No exceptions are handled.
     */
    public void testEncryptionAndDecryption() throws Exception {
        // try many different passphrases
        String[] passphrases = {
            "The passphrase is...",
            "The passphrase is... ",
            "Another passphrase",
            "A really really really really really really really really really really really really really really long passphrase."
        };
        // try lots of different sizes
        for (int i = 0; i < 20; i++) {
            // make some random data
            byte[] data = new byte[(int) (DataBlockUtil.ONE_MB * Math.random())];
            RandomUtil.getBytes(data);
            // check the data
            testEncryptionAndDecryption(data, passphrases[i % passphrases.length]);
        }
    }

    /**
     * Helper method to test encryption of data using a given passphrase and a default salt/iteration.
     * @param data byte[] of data to encrypt.
     * @param passphrase The passphrase to use during encryption.
     * @throws java.lang.Exception No exceptions are handled.
     */
    public void testEncryptionAndDecryption(byte[] data, String passphrase) throws Exception {
        // global check
        byte[] fileBasedBytes = null;

        // test using the file based methods -- i.e. what we use on large files
        {
            // make the data file
            File file = new File("temp.data");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            IOUtil.safeClose(fos);

            // encrypt the data file
            File encrypted = SecurityUtil.encryptDiskBacked(passphrase, file);

//            int expectedLength = (int)(file.length()/16+1)*16;
            // calculate the remainder
            int remainder = encrypted.length() % 16 > 1 ? 1 : 0;
            int expectedLength = (int) (encrypted.length() / 16 + remainder) * 16;
            assertEquals("Encrypted file's length should be", expectedLength, encrypted.length());

            // make sure that the files aren't the same
            byte[] decryptedBytes = IOUtil.getBytes(file);
            byte[] encryptedBytes = IOUtil.getBytes(encrypted);
            // update the bytes
//            fileBasedEncryption = b;

//        System.out.println("a: "+new String(Base16.decode(a)));
//        System.out.println("b: "+new String(Base16.decode(b)));

            // assert that they aren't the same
            if (checkBytes(decryptedBytes, encryptedBytes)) {
                assertTrue("Data should have matched.", false);
            }

            // store the bytes
            File decrypted = SecurityUtil.decryptDiskBacked(passphrase, encrypted);

            // read the decrypted bytes
            fileBasedBytes = IOUtil.getBytes(decrypted);
            // assert that they are the same
            if (!checkBytes(decryptedBytes, fileBasedBytes)) {
                assertTrue("Data should have matched.", false);
            }
        }

        // test using the in-memory based methods -- i.e. what we use on small files to avoid the overhead associated with handling small files
        if (data.length < 1024 * 1024) {
            // encrypt the bytes
            byte[] encrypted = SecurityUtil.encryptInMemory(passphrase, data);
            // calculate the remainder
            int remainder = encrypted.length % 16 > 1 ? 1 : 0;
            int expectedLength = (int) (encrypted.length / 16 + remainder) * 16;
            assertEquals("Encrypted file's length should be", expectedLength, encrypted.length);

            // bytes shouldn't match
            if (checkBytes(encrypted, data)) {
                assertTrue("Data should have matched.", false);
            }

            // decrypted bytes should match
            byte[] decrypted = SecurityUtil.decryptInMemory(passphrase, encrypted);
            if (!checkBytes(decrypted, data)) {
                assertTrue("Data should have matched.", false);
            }

            // check that the file-based decrypted bytes match the in-memory based
            if (!checkBytes(decrypted, fileBasedBytes)) {
                assertTrue("Data should have matched.", false);
            }
        }
    }

    private boolean checkBytes(final byte[] decrypted, final byte[] data) {
        // if the lengths aren't equal, return false
        if (decrypted.length != data.length) {
            return false;
        }
        // check all bytes
        for (int i = 0; i < data.length; i++) {
            if (data[i] != decrypted[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 
     */
    public void testIsCertificateSignedByDefaultCerts() throws Exception {
        assertTrue("Certificate should be signed by default certs: admin", SecurityUtil.isCertificateSignedByDefaultCerts(SecurityUtil.getAdminCertificate()));
        assertTrue("Certificate should be signed by default certs: auto cert", SecurityUtil.isCertificateSignedByDefaultCerts(SecurityUtil.getAutoCertCertificate()));
        assertTrue("Certificate should be signed by default certs: read only", SecurityUtil.isCertificateSignedByDefaultCerts(SecurityUtil.getReadOnlyCertificate()));
        assertTrue("Certificate should be signed by default certs: user", SecurityUtil.isCertificateSignedByDefaultCerts(SecurityUtil.getUserCertificate()));
        assertTrue("Certificate should be signed by default certs: write only", SecurityUtil.isCertificateSignedByDefaultCerts(SecurityUtil.getWriteOnlyCertificate()));

        UserZipFile user1 = null, user2 = null;
        try {
            // First user will be signed by self
            MakeUserZipFileTool maker1 = new MakeUserZipFileTool();
            maker1.setName("Bryan");
            maker1.setPassphrase("Smith");
            maker1.setSaveFile(TempFileUtil.createTemporaryFile(".zip.encrypted"));
            maker1.setValidDays(1);
            user1 = maker1.makeCertificate();
            user1.setPassphrase("Smith");

            assertNotNull("User should exist", user1);
            assertFalse("User should not be detected to be signed by default certs", SecurityUtil.isCertificateSignedByDefaultCerts(user1.getCertificate()));

            // Second user will be signed by first user
            MakeUserZipFileTool maker2 = new MakeUserZipFileTool();
            maker2.setName("Augie");
            maker2.setPassphrase("Hill");
            maker2.setSaveFile(TempFileUtil.createTemporaryFile(".zip.encrypted"));
            maker2.setValidDays(1);
            maker2.setSignerCertificate(user1.getCertificate());
            maker2.setSignerPrivateKey(user1.getPrivateKey());
            user2 = maker2.makeCertificate();
            user2.setPassphrase("Hill");

            assertNotNull("User should exist", user2);
            assertFalse("User should not be detected to be signed by default certs", SecurityUtil.isCertificateSignedByDefaultCerts(user2.getCertificate()));

        } finally {
            if (user1 != null) {
                IOUtil.safeDelete(user1.getFile());
            }
            if (user2 != null) {
                IOUtil.safeDelete(user2.getFile());
            }
        }
    }

    /**
     * <p>Shows that admin certificate can be serialized and deserialized, and that equals admin cert. Not equal different cert.</p>
     * @throws java.lang.Exception
     */
    public void testCertificateSerializedAndDeserialized() throws Exception {
        X509Certificate cert = SecurityUtil.getAdminCertificate();
        X509Certificate otherCert = SecurityUtil.getAnonymousCertificate();
        byte[] certBytes = cert.getEncoded();

        X509Certificate certVerify = SecurityUtil.getCertificate(certBytes);
        assertTrue("hould be the same.", cert.equals(certVerify));
        assertFalse("Should not be the same.", otherCert.equals(certVerify));
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testKeySerializedAndDerialized() throws Exception {
        PrivateKey key = SecurityUtil.getAnonymousKey();

        byte[] keyBytes = key.getEncoded();

        PrivateKey verifyKey = SecurityUtil.getPrivateKey(keyBytes);
        assertTrue("Should be the same.", verifyKey.equals(key));
    }
}
