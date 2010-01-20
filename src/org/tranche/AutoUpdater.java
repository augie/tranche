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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;

/**
 * <p>A tool for automatically updating the code to its current version.</p>
 * @author Jayson Falker - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class AutoUpdater {

    private X509Certificate certificate = null;

    /**
     * <p>Entry method for the code.</p>
     * @param args Any set of strings for parameters. Currently no parameters are used by this program.
     * @throws java.lang.Exception Any problem with the code is bubbled out.
     */
    public static void main(String[] args) throws Exception {
        // load configuration
        ConfigureTranche.load(args);

        // assume the local directory needs updating
        File file = new File(".");

        // check for udpates
        AutoUpdater au = new AutoUpdater();
        au.updateCode(file);
    }

    /**
     * <p>Automatically loads the default certificate.</p>
     */
    public AutoUpdater() {
        setCertificate(SecurityUtil.getDefaultCertificate());
    }

    /**
     * <p>Automatically updates the given directory with the current version of the Tranche code.</p>
     * @param homeDirectory The home directory to update with the URL.
     * @throws java.lang.Exception Any error with the code is bubbled out.
     */
    public void updateCode(File homeDirectory) throws Exception {
        System.out.println("*** Automatically Updating Tranche Code ***");

        // idiot check
        String zipFileURL = ConfigureTranche.get(ConfigureTranche.PROP_SOFTWARE_UPDATE_ZIP_URL);
        String sigFileURL = ConfigureTranche.get(ConfigureTranche.PROP_SOFTWARE_UPDATE_SIGNATURE_URL);
        if (zipFileURL == null || zipFileURL.equals("")) {
            System.out.println("Zip File URL is not set - cannot continue.");
            return;
        } else if (sigFileURL == null || sigFileURL.equals("")) {
            System.out.println("Signature File URL is not set - cannot continue.");
            return;
        } else if (certificate == null) {
            System.out.println("Certificate is not set - cannot continue.");
            return;
        }

        System.out.println("Downloading Zip File: " + zipFileURL);

        // download the ZIP
        URL currentVersion = new URL(zipFileURL);
        InputStream currentVersionInputStream = null;
        OutputStream currentVersionOutputStream = null;
        File currentVersionFile = new File(homeDirectory, "CurrentVersion.zip");
        try {
            currentVersionInputStream = currentVersion.openStream();
            System.out.println("Available to be read: " + currentVersionInputStream.available() + " bytes.");
            currentVersionOutputStream = new FileOutputStream(currentVersionFile);
            IOUtil.getBytes(currentVersionInputStream, currentVersionOutputStream);
        } finally {
            IOUtil.safeClose(currentVersionInputStream);
            IOUtil.safeClose(currentVersionOutputStream);
        }

        System.out.println("Finished. File Size: " + currentVersionFile.length() + " bytes");
        System.out.println("Downloading Signature File: " + sigFileURL);

        // download the signature
        URL currentVersionSignature = new URL(sigFileURL);
        InputStream currentVersionSignatureInputStream = null;
        InputStream fis = null;
        boolean isValid = false;
        try {
            // download the stream
            currentVersionSignatureInputStream = currentVersionSignature.openStream();
            fis = new FileInputStream(currentVersionFile);
            // check the signature
            byte[] bytes = IOUtil.getBytes(currentVersionSignatureInputStream);
            System.out.println("Finished. File Size: " + bytes.length + " bytes");
            // update the signature
            isValid = SecurityUtil.verify(fis, bytes, SecurityUtil.getSignatureAlgorithm(certificate.getPublicKey()), certificate);
        } finally {
            IOUtil.safeClose(currentVersionSignatureInputStream);
            IOUtil.safeClose(fis);
        }

        // if the signature isn't valid thrown an exception
        if (!isValid) {
            throw new GeneralSecurityException("Can't verify signature.");
        }

        // if signed correctly, extract the updated code
        FileInputStream tempIn = new FileInputStream(currentVersionFile);
        try {
            ZipInputStream zis = new ZipInputStream(tempIn);
            // unzip each entry in to the lib directory
            for (ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {
                // get the name
                String name = ze.getName();

                // make sure lib directory exists
                File fileToUpdate = new File(homeDirectory, name);
                // make any required directories
                if (ze.isDirectory()) {
                    fileToUpdate.mkdirs();
                    continue;
                } else {
                    fileToUpdate.getParentFile().mkdirs();
                }

                // make the IO stream
                FileOutputStream fosToUpdate = new FileOutputStream(fileToUpdate);
                try {
                    IOUtil.getBytes(zis, fosToUpdate);
                } finally {
                    IOUtil.safeClose(fosToUpdate);
                }
            }
            IOUtil.safeClose(zis);
        } finally {
            IOUtil.safeClose(tempIn);
        }

        System.out.println("Update Complete.");
    }

    /**
     * <p>Sets the certificate for validating the downloaded file.</p>
     * @param certificate
     */
    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    /**
     * <p>Gets the certificate used in validating the downloaded file.</p>
     * @return
     */
    public X509Certificate getCertificate() {
        return certificate;
    }
}
