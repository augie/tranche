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

import org.tranche.util.IOUtil;
import org.tranche.hash.BigHash;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.util.TreeSet;
import org.tranche.add.AddFileTool;
import org.tranche.add.CommandLineAddFileToolListener;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.get.CommandLineGetFileToolListener;
import org.tranche.get.GetFileTool;
import org.tranche.hash.Base16;
import org.tranche.hash.span.HashSpan;
import org.tranche.util.DevUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TrancheTestCase;
import org.tranche.util.Utils;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author besmit
 */
public class ProjectFileUtilTest extends TrancheTestCase {

    public void testProjectFileUtilSimple() throws Exception {
        String name = "This is the name.";
        String description = "This is the description.";
        testVersionOne(name, description);
        testVersionTwo(name, description);
        testVersionFour(name, description);
    }

    public void testProjectFileUtilComplex() throws Exception {
        String name = "This is the name\nblah.";
        String description = "This is the \\\\description\\.";
        testVersionOne(name, description);
        testVersionTwo(name, description);
        testVersionFour(name, description);
    }

    private ProjectFile setUpProjectFile(ProjectFile pf) throws Exception {
        File dir = TempFileUtil.createTemporaryDirectory();
        // make up some padding
        byte[] padding = "This is some padding...".getBytes();
        if (pf.getVersion().equals(ProjectFile.VERSION_ONE)) {
            padding = new byte[0];
        }
        // make up a project file
        long length = 0;
        TreeSet<ProjectFilePart> parts = new TreeSet();
        // make up some hashes
        byte[] data = Utils.makeRandomData(1000);
        for (int i = 0; i < (int) (Math.random() * 100); i++) {
            // make up a relative name
            String relativeName = new String("somedir/blah/file-" + i + ".txt");
            // make up a hash
            BigHash hash = new BigHash(data, padding);
            // change the bytes
            data = hash.toByteArray();
            // make up a different hash
            BigHash encoded = new BigHash(data);
            // change the bytes
            data = encoded.toByteArray();
            // make a project file part
            ProjectFilePart pfp = new ProjectFilePart(relativeName, hash, padding);

            // Test equality/non-equality of pfp
            assertFalse("Better not include pfp already yet.", parts.contains(pfp));
            parts.add(pfp);
            assertTrue("Better include pfp.", parts.contains(pfp));

            // inc the length
            length += (hash.getLength() - padding.length);
        }
        pf.setParts(parts);

        // serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ProjectFileUtil.write(pf, baos);
        // read in the project file
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ProjectFile pf2 = ProjectFileUtil.read(bais);
        // check the length
        assertEquals("Length should match.", BigInteger.valueOf(length).longValue(), pf2.getSize().longValue());
        // check parts
        assertEquals("Should have the same number of parts.", parts.size(), pf2.getParts().size());
        // check that the parts are the same
        ProjectFilePart[] p1 = pf.getParts().toArray(new ProjectFilePart[0]);
        ProjectFilePart[] p2 = pf2.getParts().toArray(new ProjectFilePart[0]);
        for (int i = 0; i < p1.length; i++) {
            assertEquals("Expected the same relative name.", p1[i].getRelativeName(), p2[i].getRelativeName());
            assertEquals("Expected the same hash.", p1[i].getHash().toString(), p2[i].getHash().toString());
            assertEquals("Expected the same padding.", Base16.encode(p1[i].getPadding()), Base16.encode(p2[i].getPadding()));
        }

        IOUtil.recursiveDeleteWithWarning(dir);
        return pf2;
    }

    public void testVersionOne(String name, String description) throws Exception {
        // make the project file
        ProjectFile pf = new ProjectFile();
        pf.setVersion(ProjectFile.VERSION_ONE);
        pf.setName(name);
        pf.setDescription(description);

        // set up
        ProjectFile pf2 = setUpProjectFile(pf);

        //verify
        assertEquals("Versions should match.", ProjectFile.VERSION_ONE, pf2.getVersion());
        assertEquals("Name should match.", name, pf2.getName());
        assertEquals("Description should match.", description, pf2.getDescription());
    }

    public void testVersionTwo(String name, String description) throws Exception {
        // make the project file
        ProjectFile pf = new ProjectFile();
        pf.setVersion(ProjectFile.VERSION_TWO);
        pf.setName(name);
        pf.setDescription(description);

        // set up
        ProjectFile pf2 = setUpProjectFile(pf);

        //verify
        assertEquals("Versions should match.", ProjectFile.VERSION_TWO, pf2.getVersion());
        assertEquals("Name should match.", name, pf2.getName());
        assertEquals("Description should match.", description, pf2.getDescription());
    }

    public void testVersionThree() throws Exception {
        // make the project file
        ProjectFile pf = new ProjectFile();
        pf.setVersion(ProjectFile.VERSION_THREE);

        // set up
        ProjectFile pf2 = setUpProjectFile(pf);

        // verify
        assertEquals("Versions should match.", ProjectFile.VERSION_THREE, pf2.getVersion());
    }

    public void testVersionFour(String name, String description) throws Exception {
        // make the project file
        ProjectFile pf = new ProjectFile();
        pf.setVersion(ProjectFile.VERSION_FOUR);
        pf.setName(name);
        pf.setDescription(description);
        pf.setLicenseHash(DevUtil.getRandomBigHash());

        // set up
        ProjectFile pf2 = setUpProjectFile(pf);

        // verify
        assertEquals("Versions should match.", ProjectFile.VERSION_FOUR, pf2.getVersion());
    }

    /**
     * After setting up a server with a project, download project. Use ProjectFileUtil to read in file.
     */
    public void testReadParts() throws Exception {
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // Upload project to server
            File tempProj = DevUtil.createTestProject(10, 1, DataBlockUtil.ONE_MB * 3);

            byte[] randomData = Utils.makeRandomData(512);
            File tempFile = new File(tempProj + "delete");
            Utils.makeTempFile(randomData, tempFile);

            AddFileTool aft = new AddFileTool();
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));
            aft.setFile(tempProj);
            BigHash projHash = aft.execute().getHash();
            assertNotNull(projHash);

            // Wipe out project, going to download again
            IOUtil.safeDelete(tempFile);

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // Get file, parse and build DiskBackedProjectFilePartSet

            // get the project file
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(projHash);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            gft.setSaveFile(tempFile);
            gft.getFile();

            assertTrue("Project should exist.", tempFile.exists());

            // read the project file's parts
            DiskBackedProjectFilePartSet projectFilePartSet = ProjectFileUtil.readParts(new ByteArrayInputStream(IOUtil.getBytes(tempFile)));

            // Make sure clear does it's job
            projectFilePartSet.clear();
            assertTrue("Just called clear, better be empty", projectFilePartSet.isEmpty());

            projectFilePartSet.destroy();
        } finally {
            testNetwork.stop();
        }
    }
}
