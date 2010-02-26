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

import org.tranche.hash.Base16;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.tranche.time.TimeUtil;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TrancheTestCase;

/**
 * Tests UserZipFile and UserZipFileUtil
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class UserZipFileTest extends TrancheTestCase {

    public void testUserZipFileEncrypted() throws Exception {
        File dir = null;
        try {
            dir = TempFileUtil.createTemporaryDirectory();
            // get cert/key
            X509Certificate cert = DevUtil.getDevAuthority();
            PrivateKey key = DevUtil.getDevPrivateKey();

            // make up a passphrase
            String passphrase = "The passphrase is...";

            // make a user
            File unencrypted = new File(dir, "dev-user.zip.encrypted");
            UserZipFile uzf = new UserZipFile(unencrypted);
            uzf.setCertificate(cert);
            uzf.setPrivateKey(key);
            uzf.setPassphrase(passphrase);
            // save it
            uzf.saveTo(unencrypted);

            // confirm that the file isn't a regular ZIP file
            FileInputStream fis = new FileInputStream(unencrypted);
            ZipInputStream zis = new ZipInputStream(fis);
            try {
                fis = new FileInputStream(unencrypted);
                zis = new ZipInputStream(fis);
                ZipEntry ze = zis.getNextEntry();
                if (ze != null) {
                    fail("Expected the ZIP file to be encrypted.");
                }
            } finally {
                IOUtil.safeClose(zis);
                IOUtil.safeClose(fis);
            }

            // check the values
            UserZipFile uzf2 = new UserZipFile(unencrypted);
            uzf2.setPassphrase(passphrase);
            assertEquals("Exepcted the same cert.", Base16.encode(uzf.getCertificate().getEncoded()), Base16.encode(uzf2.getCertificate().getEncoded()));
            assertEquals("Exepcted the same key.", Base16.encode(uzf.getPrivateKey().getEncoded()), Base16.encode(uzf2.getPrivateKey().getEncoded()));
        } finally {
            IOUtil.recursiveDeleteWithWarning(dir);
        }
    }

    public void testCreateAdmin() throws Exception {
        testUserZipFileUtil(true);
    }

    public void testCreateUser() throws Exception {
        testUserZipFileUtil(false);
    }

    public void testUserZipFileUtil(boolean isAdmin) throws Exception {
        final File tmpFile = TempFileUtil.createTemporaryFile();
        final String name = "Text User";
        final String pw = "supersecret";

        UserZipFile uzf = DevUtil.createUser(name, pw, tmpFile.getAbsolutePath(), isAdmin, false);

        assertEquals("Names should match", pw, uzf.getPassphrase());
        assertEquals("Should be same file", tmpFile, uzf.getFile());
        assertEquals("Only admins set config", isAdmin, uzf.canSetConfiguration());
    }

    public void testCertNotValidYet() throws Exception {
        File file1 = null, file2 = null;
        try {
            file1 = TempFileUtil.createTemporaryFile(".zip.encrypted");
            file2 = TempFileUtil.createTemporaryFile(".zip.encrypted");

            // Date for tommorow. Shouldn't be valid.
            Date d1 = new Date(TimeUtil.getTrancheTimestamp() + 1000 * 60 * 60 * 24);

            // Date for now. Better be valid.
            Date d2 = new Date(TimeUtil.getTrancheTimestamp());

            String password = "test_pw";
            String name = "test";
            String organization = "Tranche";

            MakeUserZipFileTool maker1 = new MakeUserZipFileTool();
            maker1.setName(name);
            maker1.setPassphrase(password);
            maker1.setSaveFile(file1);
            maker1.setValidDays(d1, 3);

            UserZipFile uzf1 = maker1.makeCertificate();
            assertTrue("Shouldn't be valid yet.", uzf1.isNotYetValid());

            MakeUserZipFileTool maker2 = new MakeUserZipFileTool();
            maker2.setName(name);
            maker2.setPassphrase(password);
            maker2.setSaveFile(file2);
            maker2.setValidDays(d2, 3);

            UserZipFile uzf2 = maker2.makeCertificate();
            assertFalse("Should be valid immediately.", uzf2.isNotYetValid());
        } finally {
            IOUtil.safeDelete(file1);
            IOUtil.safeDelete(file2);
        }
    }

    public void testCertExpired() throws Exception {
        File file1 = null, file2 = null;
        try {
            file1 = TempFileUtil.createTemporaryFile(".zip.encrypted");
            file2 = TempFileUtil.createTemporaryFile(".zip.encrypted");
            String password = "test_pw";
            String name = "test";
            String organization = "Tranche";

            MakeUserZipFileTool maker1 = new MakeUserZipFileTool();
            maker1.setName(name);
            maker1.setPassphrase(password);
            maker1.setSaveFile(file1);
            // Date for four days ago. Shouldn't be valid if 3-day pass.
            maker1.setValidDays(new Date(TimeUtil.getTrancheTimestamp() - 4 * 1000 * 60 * 60 * 24), 3);

            UserZipFile uzf1 = maker1.makeCertificate();
            assertTrue("Should be expired.", uzf1.isExpired());
            assertFalse("Should be expired.", uzf1.isValid());

            MakeUserZipFileTool maker2 = new MakeUserZipFileTool();
            maker2.setName(name);
            maker2.setPassphrase(password);
            maker2.setSaveFile(file2);
            // Date for now. Better be valid.
            maker2.setValidDays(new Date(TimeUtil.getTrancheTimestamp()), 3);

            UserZipFile uzf2 = maker2.makeCertificate();
            assertFalse("Should not be expired.", uzf2.isExpired());
            assertTrue("Should not be expired.", uzf2.isValid());
        } finally {
            IOUtil.safeDelete(file1);
            IOUtil.safeDelete(file2);
        }
    }

    public void testGetNameFromCert() throws Exception {
        File file1 = null, file2 = null;
        try {
            file1 = TempFileUtil.createTemporaryFile(".zip.encrypted");
            file2 = TempFileUtil.createTemporaryFile(".zip.encrypted");

            String password = "test_pw";

            MakeUserZipFileTool maker1 = new MakeUserZipFileTool();
            maker1.setName("Bryan Smith");
            maker1.setPassphrase(password);
            maker1.setValidDays(1);
            maker1.setSaveFile(file1);

            UserZipFile uzf1 = maker1.makeCertificate();
            assertEquals("Expecting certain name.", "Bryan Smith", uzf1.getUserNameFromCert());

            MakeUserZipFileTool maker2 = new MakeUserZipFileTool();
            maker2.setName("Augie Hill");
            maker2.setPassphrase(password);
            maker2.setValidDays(1);
            maker2.setSaveFile(file2);

            UserZipFile uzf2 = maker2.makeCertificate();
            assertEquals("Expecting certain name.", "Augie Hill", uzf2.getUserNameFromCert());

        } finally {
            IOUtil.safeDelete(file1);
            IOUtil.safeDelete(file2);
        }
    }

    public void testUserAllPrivilegesFlag() throws Exception {
        UserZipFile user = DevUtil.makeNewUser(RandomUtil.getString(10), User.ALL_PRIVILEGES);
        
        user.setFlags(User.NO_PRIVILEGES);
        assertFalse("User shouldn't be able to do anything.", user.canDeleteData());
        assertFalse("User shouldn't be able to do anything.", user.canDeleteMetaData());
        assertFalse("User shouldn't be able to do anything.", user.canGetConfiguration());
        assertFalse("User shouldn't be able to do anything.", user.canSetConfiguration());
        assertFalse("User shouldn't be able to do anything.", user.canSetData());
        assertFalse("User shouldn't be able to do anything.", user.canSetMetaData());

        user.setFlags(User.ALL_PRIVILEGES);
        assertTrue("User should be able to do anything.", user.canDeleteData());
        assertTrue("User should be able to do anything.", user.canDeleteMetaData());
        assertTrue("User should be able to do anything.", user.canGetConfiguration());
        assertTrue("User should be able to do anything.", user.canSetConfiguration());
        assertTrue("User should be able to do anything.", user.canSetData());
        assertTrue("User should be able to do anything.", user.canSetMetaData());
    }
}
