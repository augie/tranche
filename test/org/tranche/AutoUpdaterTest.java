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
package org.tranche;

import org.tranche.exceptions.TodoException;
import org.tranche.util.*;

/**
 * NOT YET IMPLEMENTED
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class AutoUpdaterTest extends TrancheTestCase {

    public void testTodo() throws Exception {
        throw new TodoException();
    }

//    // max file sizes
//    final int maxFileSize = 10000;
//
//    public void testAutoUpdateInvalid() throws Exception {
//        // delete the directory
//        File dir = TempFileUtil.createTemporaryDirectory();
//
//        // make 2 random files
//        File a = new File(dir, "files/a");
//        Utils.makeTempFile(a, (int)(Math.random()*maxFileSize));
//        File b = new File(dir, "files/b");
//        Utils.makeTempFile(b, (int)(Math.random()*maxFileSize));
//
//        // put them in a ZIP
//        File zip = new File(dir, "CurrentVersion.zip");
//        FileOutputStream fos = null;
//        try {
//            // make the zip
//            fos = new FileOutputStream(zip);
//            ZipOutputStream zos = new ZipOutputStream(fos);
//
//            // make the first entry
//            ZipEntry zeA = new ZipEntry("a");
//            zos.putNextEntry(zeA);
//            FileInputStream fisA = new FileInputStream(a);
//            try {
//                for (int i=fisA.read();i!=-1;i=fisA.read()){
//                    zos.write(i);
//                }
//            } finally {
//                IOUtil.safeClose(fisA);
//            }
//            zos.flush();
//            zos.closeEntry();
//
//            // make the second entry
//            ZipEntry zeB = new ZipEntry("dir/b");
//            zos.putNextEntry(zeB);
//            FileInputStream fisB = new FileInputStream(b);
//            try {
//                for (int i=fisB.read();i!=-1;i=fisB.read()){
//                    zos.write(i);
//                }
//            } finally {
//                IOUtil.safeClose(fisB);
//            }
//            zos.flush();
//            zos.closeEntry();
//
//            // finish/flush
//            zos.finish();
//            zos.close();
//
//            fos.flush();
//            fos.close();
//
//            System.out.println("Zip file size: " + zip.length() + " bytes");
//
//            // now make a URL to the path
//            String zip_url = "file:///"+zip.getCanonicalPath();
//            String sig_url = "file:///"+zip.getParentFile().getCanonicalPath()+"/CurrentVersion.sha1";
//
//            // make a signature
//            String algorithm = SecurityUtil.getSignatureAlgorithm(DevUtil.getDevPrivateKey());
//            // get the signature bytes
//            byte[] sigBytes = SecurityUtil.sign(zip, DevUtil.getDevPrivateKey(), algorithm);
//            File sigFile = new File(zip.getParentFile(), "CurrentVersion.sha1");
//            FileOutputStream sigOutput = new FileOutputStream(sigFile);
//            sigOutput.write(sigBytes);
//            sigOutput.flush();
//            sigOutput.close();
//
//            // auto-update
//            AutoUpdater au = new AutoUpdater();
//            AutoUpdater.setZipFileUrl(zip_url);
//            AutoUpdater.setSignatureFileUrl(sig_url);
//            // set a different certificate to the one we signed with
//            au.setCertificate(SecurityUtil.getDefaultCertificate());
//
//            // try to update the code
//            boolean caught = false;
//            try {
//                au.updateCode(dir);
//            }
//            // catch the invalid signature exception
//            catch (GeneralSecurityException gse) {
//                caught = true;
//            }
//            // if no error was thrown, fail the test
//            if (!caught) {
//                fail("The code shouldn't accept invalid signatures.");
//            }
//
//        } finally {
//            IOUtil.safeDelete(zip);
//        }
//
//        // delete the files
//        Utils.recursiveDelete(dir);
//    }
//
//    public void testAutoUpdate() throws Exception {
//        // delete the directory
//        File dir = TempFileUtil.createTemporaryDirectory();
//        Utils.recursiveDelete(dir);
//        dir.mkdirs();
//
//        // make 2 random files
//        File a = new File(dir, "files/a");
//        Utils.makeTempFile(a, (int)(Math.random()*maxFileSize));
//        File b = new File(dir, "files/b");
//        Utils.makeTempFile(b, (int)(Math.random()*maxFileSize));
//
//        // put them in a ZIP
//        File zip = new File(dir, "CurrentVersion.zip");
//        FileOutputStream fos = null;
//        try {
//            // make the zip
//            fos = new FileOutputStream(zip);
//            ZipOutputStream zos = new ZipOutputStream(fos);
//
//            // make the first entry
//            ZipEntry zeA = new ZipEntry("a");
//            zos.putNextEntry(zeA);
//            FileInputStream fisA = new FileInputStream(a);
//            try {
//                for (int i=fisA.read();i!=-1;i=fisA.read()){
//                    zos.write(i);
//                }
//            } finally {
//                IOUtil.safeClose(fisA);
//            }
//            zos.flush();
//            zos.closeEntry();
//
//            // make the second entry
//            ZipEntry zeB = new ZipEntry("dir/b");
//            zos.putNextEntry(zeB);
//            FileInputStream fisB = new FileInputStream(b);
//            try {
//                for (int i=fisB.read();i!=-1;i=fisB.read()){
//                    zos.write(i);
//                }
//            } finally {
//                IOUtil.safeClose(fisB);
//            }
//            zos.flush();
//            zos.closeEntry();
//
//            // finish/flush
//            zos.finish();
//            zos.close();
//
//            fos.flush();
//            fos.close();
//
//
//            // now make a URL to the path
//            String zip_url = "file:///"+zip.getCanonicalPath();
//            String sig_url = "file:///"+zip.getParentFile().getCanonicalPath()+"/CurrentVersion.sha1";
//
//            // make a signature
//            String algorithm = SecurityUtil.getSignatureAlgorithm(DevUtil.getDevPrivateKey());
//            // get the signature bytes
//            byte[] sigBytes = SecurityUtil.sign(zip, DevUtil.getDevPrivateKey(), algorithm);
//            File sigFile = new File(zip.getParentFile(), "CurrentVersion.sha1");
//            FileOutputStream sigOutput = new FileOutputStream(sigFile);
//            sigOutput.write(sigBytes);
//            sigOutput.flush();
//            sigOutput.close();
//
//            // auto-update
//            AutoUpdater au = new AutoUpdater();
//            AutoUpdater.setZipFileUrl(zip_url);
//            AutoUpdater.setSignatureFileUrl(sig_url);
//            au.setCertificate(DevUtil.getDevAuthority());
//
//            // update the code to this project's directory
//            au.updateCode(dir);
//
//            // check that they match
//            assertEquals("Must have file a.", a.length(), new File(dir, "a").length());
//            assertEquals("Must have file b.", b.length(), new File(dir, "dir/b").length());
//
//        } finally {
//            IOUtil.safeDelete(zip);
//        }
//
//        // delete the files
//        Utils.recursiveDelete(dir);
//    }
//
//    public void testAutoUpdateOverwrite() throws Exception {
//        // delete the directory
//        File dir =TempFileUtil.createTemporaryDirectory();
//        Utils.recursiveDelete(dir);
//        dir.mkdirs();
//
//        // make 2 random files
//        File a = new File(dir, "files/a");
//        Utils.makeTempFile(a, (int)(Math.random()*maxFileSize));
//        File b = new File(dir, "files/b");
//        Utils.makeTempFile(b, (int)(Math.random()*maxFileSize));
//
//        // put them in a ZIP
//        File zip = new File(dir, "CurrentVersion.zip");
//        FileOutputStream fos = null;
//        try {
//            // make the zip
//            fos = new FileOutputStream(zip);
//            ZipOutputStream zos = new ZipOutputStream(fos);
//
//            // make the first entry
//            ZipEntry zeA = new ZipEntry("a");
//            zos.putNextEntry(zeA);
//            FileInputStream fisA = new FileInputStream(a);
//            try {
//                for (int i=fisA.read();i!=-1;i=fisA.read()){
//                    zos.write(i);
//                }
//            } finally {
//                IOUtil.safeClose(fisA);
//            }
//            zos.flush();
//            zos.closeEntry();
//
//            // make the second entry
//            ZipEntry zeB = new ZipEntry("dir/b");
//            zos.putNextEntry(zeB);
//            FileInputStream fisB = new FileInputStream(b);
//            try {
//                for (int i=fisB.read();i!=-1;i=fisB.read()){
//                    zos.write(i);
//                }
//            } finally {
//                IOUtil.safeClose(fisB);
//            }
//            zos.flush();
//            zos.closeEntry();
//
//            // finish/flush
//            zos.finish();
//            zos.close();
//
//            fos.flush();
//            fos.close();
//
//
//            // now make a URL to the path
//            String zip_url = "file:///"+zip.getCanonicalPath();
//            String sig_url = "file:///"+zip.getParentFile().getCanonicalPath()+"/CurrentVersion.sha1";
//
//            // make a signature
//            String algorithm = SecurityUtil.getSignatureAlgorithm(DevUtil.getDevPrivateKey());
//            // get the signature bytes
//            byte[] sigBytes = SecurityUtil.sign(zip, DevUtil.getDevPrivateKey(), algorithm);
//            File sigFile = new File(zip.getParentFile(), "CurrentVersion.sha1");
//            FileOutputStream sigOutput = new FileOutputStream(sigFile);
//            sigOutput.write(sigBytes);
//            sigOutput.flush();
//            sigOutput.close();
//
//            // write out known contents to a file
//            File existingFile = new File(dir, "a");
//            File existingFileDuplicate = new File(dir, "a.duplicate");
//            String content = "Test File Content.";
//            // write one file
//            {
//                FileWriter fw = new FileWriter(existingFile);
//                fw.write(content);
//                fw.flush();
//                fw.close();
//            }
//            // write a duplicate to compare against
//            {
//                FileWriter fw = new FileWriter(existingFileDuplicate);
//                fw.write(content);
//                fw.flush();
//                fw.close();
//            }
//
//            // test that the two files match
//            TestTools.assertMatchingContents(existingFile, existingFileDuplicate);
//
//            // auto-update
//            AutoUpdater au = new AutoUpdater();
//            au.setZipFileUrl(zip_url);
//            au.setSignatureFileUrl(sig_url);
//            au.setCertificate(DevUtil.getDevAuthority());
//
//            // update the code to this project's directory
//            au.updateCode(dir);
//
//            // check that they match
//            assertEquals("Must have file a.", a.length(), new File(dir, "a").length());
//            assertEquals("Must have file b.", b.length(), new File(dir, "dir/b").length());
//
//            // check the new files
////            File newA = new File(dir, "a");
//            File newB = new File(dir, "dir/b");
//
//            TestTools.assertMatchingContents(existingFile, a);
//            TestTools.assertMatchingContents(newB, b);
//
//        } finally {
//            IOUtil.safeDelete(zip);
//        }
//
//        // delete the files
//        Utils.recursiveDelete(dir);
//    }
//
//
////    public void testAutoUpdateBuild() throws Exception {
////        // delete the directory
////        File dir = new File(TempFileUtil.getTemporaryDirectory() + "/AutoUpdaterTest/testAutoUpdateBuild");
////        Utils.recursiveDelete(dir);
////        dir.mkdirs();
////
////        // auto-update
////        AutoUpdater au = new AutoUpdater();
////        au.setCertificate(SecurityUtil.getCertificate(new File("C:\\Documents and Settings\\Jayson\\Desktop\\backup\\clutser-keys\\ProteomeCommons.org-Data-Network\\public.certificate")));
////        au.setZipFileUrl("file:///C:\\Documents and Settings\\Jayson\\Desktop\\svn\\DFS\\build\\website\\files\\");
////
////        // update the code to this project's directory
////        au.updateCode(dir);
////
////        // delete the files
////        Utils.recursiveDelete(dir);
////    }
}
