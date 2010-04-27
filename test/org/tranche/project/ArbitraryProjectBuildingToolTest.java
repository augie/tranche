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
package org.tranche.project;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.ConfigureTranche;
import org.tranche.add.AddFileTool;
import org.tranche.add.AddFileToolReport;
import org.tranche.get.GetFileTool;
import org.tranche.get.GetFileToolReport;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.license.LicenseUtil;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ArbitraryProjectBuildingToolTest extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testProjectPositiveSpecifyServers() throws Exception {
        TestUtil.printTitle("ArbitraryProjectBuildingToolTest:testProjectPositiveSpecifyServers()");

        testProjectPositive(true);
    }
    
    public void testProjectPositiveDontSpecifyServers() throws Exception {
        TestUtil.printTitle("ArbitraryProjectBuildingToolTest:testProjectPositiveDontSpecifyServers()");
        testProjectPositive(false);
    }

    /**
     * <p>Make sure the tool works:</p>
     * <ul>
     *   <li>Create a single server.</li>
     *   <li>Upload three projects, each with two files.</li>
     *   <li>Use two files (from two different projects) and bundle as a project.</li>
     *   <li>Verify there are four projects.</li>
     * </ul>
     */
    public void testProjectPositive(boolean isSpecifyServers) throws Exception {
        
        final String oldReplicationsVal = ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_REPLICATIONS);

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {

            ConfigureTranche.set(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_REPLICATIONS, String.valueOf(1));

            testNetwork.start();

            File testDir1 = DevUtil.createTestProject(2, 1024, 1024 * 512);
            File testDir2 = DevUtil.createTestProject(2, 1024, 1024 * 512);
            File testDir3 = DevUtil.createTestProject(2, 1024, 1024 * 512);

            File singleFileUpload = TempFileUtil.createTempFileWithName("test-upload");
            DevUtil.createTestFile(singleFileUpload, 1024, 1024 * 512);

            assertTrue("Project must exist.", testDir1.exists());
            assertTrue("Project must exist.", testDir2.exists());
            assertTrue("Project must exist.", testDir3.exists());

            assertFalse("Must be distinct directories.", testDir1.getAbsolutePath().equals(testDir2.getAbsolutePath()));
            assertFalse("Must be distinct directories.", testDir2.getAbsolutePath().equals(testDir3.getAbsolutePath()));
            assertFalse("Must be distinct directories.", testDir3.getAbsolutePath().equals(testDir1.getAbsolutePath()));

            AddFileTool aft = new AddFileTool();
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.setFile(testDir1);
            AddFileToolReport report1 = aft.execute();
            assertSuccess(report1);
            BigHash hash1 = report1.getHash();

            aft = new AddFileTool();
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.setFile(testDir2);
            AddFileToolReport report2 = aft.execute();
            assertSuccess(report2);
            BigHash hash2 = report2.getHash();

            aft = new AddFileTool();
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.setFile(testDir3);
            AddFileToolReport report3 = aft.execute();
            assertSuccess(report3);
            BigHash hash3 = report3.getHash();

            aft = new AddFileTool();
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.setFile(singleFileUpload);
            AddFileToolReport report4 = aft.execute();
            assertSuccess(report4);
            BigHash hash4 = report4.getHash();

            // Grab project file from second project and third projects.
            ProjectFile pf2 = null, pf3 = null;
            File f = TempFileUtil.createTempFileWithName(".pf");

            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash2);
            gft.setSaveFile(f);
            GetFileToolReport gftReport2 = gft.getFile();
            assertSuccess(gftReport2);

            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(f));
                pf2 = ProjectFileUtil.read(bis);
            } finally {
                IOUtil.safeClose(bis);
                IOUtil.safeDelete(f);
            }

            f = TempFileUtil.createTempFileWithName(".pf");

            gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash3);
            gft.setSaveFile(f);
            GetFileToolReport gftReport3 = gft.getFile();
            assertSuccess(gftReport3);

            try {
                bis = new BufferedInputStream(new FileInputStream(f));
                pf3 = ProjectFileUtil.read(bis);
            } finally {
                IOUtil.safeClose(bis);
                IOUtil.safeDelete(f);
            }

            assertNotNull("Project file for project #2 must not be null.", pf2);
            assertNotNull("Project file for project #3 must not be null.", pf3);

            // Let's grab two file names
            Iterator<ProjectFilePart> it2 = pf2.getParts().iterator();
            Iterator<ProjectFilePart> it3 = pf3.getParts().iterator();

            ProjectFilePart firstFileToUse = it2.next();
            ProjectFilePart secondFileToUse = it3.next();

            // Make sure files have distinct names. Shouldn't happen, but could.
            while (firstFileToUse.getRelativeName().substring(firstFileToUse.getRelativeName().lastIndexOf(File.separator)).equals(secondFileToUse.getRelativeName().substring(secondFileToUse.getRelativeName().lastIndexOf(File.separator)))) {
                secondFileToUse = it3.next();
            }

            assertNotNull("First ProjectFilePart must not be null.", firstFileToUse);
            assertNotNull("Second ProjectFilePart must not be null.", secondFileToUse);
            assertFalse("ProjectFilePart's must not have same relative names.", firstFileToUse.getRelativeName().equals(secondFileToUse.getRelativeName()));

            // Create the tool.
            ArbitraryProjectBuildingTool apbt = new ArbitraryProjectBuildingTool(DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), "just-a-test");

            if (isSpecifyServers) {
                apbt.addServerToUse(HOST1);            
            }
            
            // Let's mix it up. Use one ProjectFilePart and one hash
            apbt.addFileFromProject(hash2, firstFileToUse.getHash());
            apbt.addFileFromProject(hash2, secondFileToUse);
            apbt.addIndividualFile(hash4);

            BigHash projectHash = apbt.run();

            assertNotNull("Better be hash for created file.", projectHash);

            File tmpDir = TempFileUtil.createTemporaryDirectory();

            // Let download it to make sure
            gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setSaveFile(tmpDir);
            gft.setHash(projectHash);

            GetFileToolReport gftr = gft.getDirectory();
            assertSuccess(gftr);

            TestUtil.printRecursiveDirectoryStructure(tmpDir);

            // Recursively find files
            List<File> filesToCheck = new LinkedList();
            List<File> regularFilesFound = new ArrayList();

            filesToCheck.add(tmpDir);

            while (filesToCheck.size() > 0) {
                File nextFile = filesToCheck.remove(0);
                if (nextFile.isDirectory()) {
                    for (File depthFirstFile : nextFile.listFiles()) {
                        filesToCheck.add(0, depthFirstFile);
                    }
                } else if (!nextFile.getName().equals(LicenseUtil.RECOMMENDED_LICENSE_FILE_NAME)) {
                    regularFilesFound.add(nextFile);
                }
            }
            
            // These are the regular files that were downloaded
            final File[] downloadedFiles = regularFilesFound.toArray(new File[0]);

            assertEquals("Expecting three files, which we selected earlier.", 3, downloadedFiles.length);
            assertFalse("Names should not match.", downloadedFiles[0].getName().equals(downloadedFiles[1].getName()));
            assertFalse("Names should not match.", downloadedFiles[1].getName().equals(downloadedFiles[2].getName()));
            assertFalse("Names should not match.", downloadedFiles[2].getName().equals(downloadedFiles[0].getName()));

            // Let's get simple file name from ProjectFilePart's
            String assertFileName1 = firstFileToUse.getRelativeName().substring(firstFileToUse.getRelativeName().lastIndexOf(File.separator) + 1);
            String assertFileName2 = secondFileToUse.getRelativeName().substring(secondFileToUse.getRelativeName().lastIndexOf(File.separator) + 1);
            String assertFileName3 = singleFileUpload.getName();

            assertNotNull("Names must not be null. Needed to make sure we downloaded right files.", assertFileName1);
            assertNotNull("Names must not be null. Needed to make sure we downloaded right files.", assertFileName2);
            assertNotNull("Names must not be null. Needed to make sure we downloaded right files.", assertFileName3);

            assertFalse("Names must not be equal.", assertFileName1.equals(assertFileName2));
            assertFalse("Names must not be equal.", assertFileName2.equals(assertFileName3));
            assertFalse("Names must not be equal.", assertFileName3.equals(assertFileName1));

            boolean firstDownloadedFileMatches = downloadedFiles[0].getName().equals(assertFileName1) || downloadedFiles[0].getName().equals(assertFileName2) || downloadedFiles[0].getName().equals(assertFileName3);
            boolean secondDownloadedFileMatches = downloadedFiles[1].getName().equals(assertFileName1) || downloadedFiles[1].getName().equals(assertFileName2) || downloadedFiles[1].getName().equals(assertFileName3);
            boolean thirdDownloadedFileMatches = downloadedFiles[2].getName().equals(assertFileName1) || downloadedFiles[2].getName().equals(assertFileName2) || downloadedFiles[2].getName().equals(assertFileName3);

            assertTrue("First downloaded file<" + downloadedFiles[0].getName() + "> doesn't match one of the files uploaded <" + assertFileName1 + " or " + assertFileName2 + " or " + assertFileName3 + ">", firstDownloadedFileMatches);
            assertTrue("Second downloaded file<" + downloadedFiles[1].getName() + "> doesn't match one of the files uploaded <" + assertFileName1 + " or " + assertFileName2 + " or " + assertFileName3 + ">", secondDownloadedFileMatches);
            assertTrue("Third downloaded file<" + downloadedFiles[2].getName() + "> doesn't match one of the files uploaded <" + assertFileName1 + " or " + assertFileName2 + " or " + assertFileName3 + ">", thirdDownloadedFileMatches);
        } finally {
            testNetwork.stop();
            ConfigureTranche.set(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_REPLICATIONS, oldReplicationsVal);
        }
    }
    
    /**
     * <p>Make sure the tool works:</p>
     * <ul>
     *   <li>Create a single server.</li>
     *   <li>Upload three projects, each with two files.</li>
     *   <li>Use one data set and one file.</li>
     *   <li>Verify there are four projects.</li>
     * </ul>
     */
    public void testProjectWithDataSetAndSingleFile() throws Exception {
        
        TestUtil.printTitle("ArbitraryProjectBuildingToolTest:testProjectWithDataSetAndSingleFile()");
        
        final String oldReplicationsVal = ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_REPLICATIONS);

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {

            ConfigureTranche.set(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_REPLICATIONS, String.valueOf(1));

            testNetwork.start();

            File testDir1 = DevUtil.createTestProject(2, 1024, 1024 * 512);
            File testDir2 = DevUtil.createTestProject(2, 1024, 1024 * 512);
            File testDir3 = DevUtil.createTestProject(2, 1024, 1024 * 512);

            File singleFileUpload = TempFileUtil.createTempFileWithName("test-upload");
            DevUtil.createTestFile(singleFileUpload, 1024, 1024 * 512);

            assertTrue("Project must exist.", testDir1.exists());
            assertTrue("Project must exist.", testDir2.exists());
            assertTrue("Project must exist.", testDir3.exists());

            assertFalse("Must be distinct directories.", testDir1.getAbsolutePath().equals(testDir2.getAbsolutePath()));
            assertFalse("Must be distinct directories.", testDir2.getAbsolutePath().equals(testDir3.getAbsolutePath()));
            assertFalse("Must be distinct directories.", testDir3.getAbsolutePath().equals(testDir1.getAbsolutePath()));

            AddFileTool aft = new AddFileTool();
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.setFile(testDir1);
            AddFileToolReport report1 = aft.execute();
            assertSuccess(report1);
            BigHash hash1 = report1.getHash();

            TestUtil.printRecursiveDirectoryStructure(testDir1);
            
            aft = new AddFileTool();
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.setFile(testDir2);
            AddFileToolReport report2 = aft.execute();
            assertSuccess(report2);
            BigHash hash2 = report2.getHash();

            aft = new AddFileTool();
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.setFile(testDir3);
            AddFileToolReport report3 = aft.execute();
            assertSuccess(report3);
            BigHash hash3 = report3.getHash();

            aft = new AddFileTool();
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.setFile(singleFileUpload);
            AddFileToolReport report4 = aft.execute();
            assertSuccess(report4);
            BigHash hash4 = report4.getHash();

            // Grab project file from third project.
            ProjectFile pf = null;
            File f = TempFileUtil.createTempFileWithName(".pf");

            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash2);
            gft.setSaveFile(f);
            GetFileToolReport gftReport1 = gft.getFile();
            assertSuccess(gftReport1);

            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(f));
                pf = ProjectFileUtil.read(bis);
            } finally {
                IOUtil.safeClose(bis);
                IOUtil.safeDelete(f);
            }

            assertNotNull("Project file for project must not be null.", pf);

            // Let's grab one file names
            Iterator<ProjectFilePart> it3 = pf.getParts().iterator();

            ProjectFilePart firstFileToUse = it3.next();

            assertNotNull("First ProjectFilePart must not be null.", firstFileToUse);

            // Create the tool.
            ArbitraryProjectBuildingTool apbt = new ArbitraryProjectBuildingTool(DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), "just-a-test");
            
            // Let's mix it up. Use one data set and one hash
            apbt.addFileFromProject(hash2, firstFileToUse.getHash());
            apbt.addDataSet(hash1);

            BigHash projectHash = apbt.run();

            assertNotNull("Better be hash for created file.", projectHash);

            File tmpDir = TempFileUtil.createTemporaryDirectory();

            // Let download it to make sure
            gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setSaveFile(tmpDir);
            gft.setHash(projectHash);

            GetFileToolReport gftr = gft.getDirectory();
            assertSuccess(gftr);

            TestUtil.printRecursiveDirectoryStructure(tmpDir);

            // Recursively find files
            List<File> filesToCheck = new LinkedList();
            List<File> regularFilesFound = new ArrayList();

            filesToCheck.add(tmpDir);
            
            Set<String> expectedFileNames = new HashSet();
            expectedFileNames.add(firstFileToUse.getRelativeName().substring(firstFileToUse.getRelativeName().lastIndexOf(File.separator)));
            
            for (File nextF: testDir1.listFiles()) {
                expectedFileNames.add(nextF.getName());
            }
            
            assertEquals("Expecting three files.",3,expectedFileNames.size());

            while (filesToCheck.size() > 0) {
                File nextFile = filesToCheck.remove(0);
                if (nextFile.isDirectory()) {
                    for (File depthFirstFile : nextFile.listFiles()) {
                        filesToCheck.add(0, depthFirstFile);
                    }
                } else if (!nextFile.getName().equals(LicenseUtil.RECOMMENDED_LICENSE_FILE_NAME)) {
                    assertTrue("File should be expected.", expectedFileNames.contains(nextFile.getName()));
                    regularFilesFound.add(nextFile);
                }
            }
            
            assertEquals("Expecting three files plus license.",  3, regularFilesFound.size());
        } finally {
            testNetwork.stop();
            ConfigureTranche.set(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_REPLICATIONS, oldReplicationsVal);
        }
    }
}