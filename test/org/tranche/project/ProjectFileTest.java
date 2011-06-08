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

import java.math.BigInteger;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.hash.BigHash;
import org.tranche.util.DevUtil;
import org.tranche.commons.RandomUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ProjectFileTest extends TrancheTestCase {

    public void testVersion() throws Exception {
        ProjectFile pf = new ProjectFile();

        // default latest
        assertEquals(pf.VERSION_LATEST, pf.getVersion());

        // var
        String version = ProjectFile.VERSION_THREE;

        // set
        pf.setVersion(version);

        // verify
        assertEquals(version, pf.getVersion());
    }

    public void testName() throws Exception {
        ProjectFile pf = new ProjectFile();

        // default
        assertEquals("", pf.getName());

        // var
        String name = "name";

        // set
        pf.setName(name);

        // verify
        assertEquals(name, pf.getName());
    }

    public void testDescription() throws Exception {
        ProjectFile pf = new ProjectFile();

        // default
        assertEquals("", pf.getDescription());

        // var
        String description = "description";

        // set
        pf.setDescription(description);

        // verify
        assertEquals(description, pf.getDescription());
    }

    public void testLicenseHash() throws Exception {
        ProjectFile pf = new ProjectFile();

        // default
        assertEquals(null, pf.getLicenseHash());

        // var
        BigHash hash = DevUtil.getRandomBigHash();

        // set
        pf.setLicenseHash(hash);

        // verify
        assertEquals(hash, pf.getLicenseHash());
    }

    public void testParts() throws Exception {
        ProjectFile pf = new ProjectFile();

        // default
        assertEquals(null, pf.getParts());

        // create
        DiskBackedProjectFilePartSet parts = new DiskBackedProjectFilePartSet();
        BigInteger size = BigInteger.ZERO;
        for (int i = 0; i < 10; i++) {
            String relativeName = RandomUtil.getString(20);
            int fileSize = RandomUtil.getInt(DataBlockUtil.getMaxChunkSize() * 2);
            BigHash hash = DevUtil.getRandomBigHash(fileSize);
            // make a part
            parts.add(new ProjectFilePart(relativeName, hash, new byte[0]));
            // add size
            size = size.add(BigInteger.valueOf(fileSize));
        }

        // set
        pf.setParts(parts);

        // verify
        assertEquals(parts, pf.getParts());
    }

    /**
     * Tests equality w/ and w/o padding.
     */
    public void testProjectFilePartPadding() throws Exception {

        int dataSize = 512;
        int paddingSize = 128;

        ProjectFilePart pfpPadding, pfpPaddingSame, pfpPaddingDifferent,
                pfpNoPadding, pfpNoPaddingSame, pfpNoPaddingDifferent;

        byte[] data = new byte[dataSize];
        byte[] padding = new byte[paddingSize];
        byte[] encryptedData = new byte[dataSize + paddingSize];

        BigHash hash = null;

        // Make the padded parts. These emulate encrypted parrts.
        RandomUtil.getBytes(data);
        RandomUtil.getBytes(padding);
        System.arraycopy(data, 0, encryptedData, 0, data.length);
        System.arraycopy(padding, 0, encryptedData, data.length, padding.length);
        hash = new BigHash(encryptedData);

        pfpPadding = new ProjectFilePart("/path/to/file1", hash, padding);
        pfpPaddingSame = new ProjectFilePart("/path/to/file1", hash, padding);

        RandomUtil.getBytes(data);
        RandomUtil.getBytes(padding);
        System.arraycopy(data, 0, encryptedData, 0, data.length);
        System.arraycopy(padding, 0, encryptedData, data.length, padding.length);
        hash = new BigHash(encryptedData);
        pfpPaddingDifferent = new ProjectFilePart("/path/to/file2", hash, padding);

        assertEquals("Should be equal", pfpPadding, pfpPaddingSame);
        assertNotSame("Should not be equal", pfpPadding, pfpPaddingDifferent);

        assertEquals("Should be equal", pfpPadding.hashCode(), pfpPaddingSame.hashCode());
        assertNotSame("Should not be equal", pfpPadding.hashCode(), pfpPaddingDifferent.hashCode());

        // Test w/o padding. Emulate unencrypted parts.
        padding = new byte[0];
        RandomUtil.getBytes(data);
        hash = new BigHash(data);

        pfpNoPadding = new ProjectFilePart("/path/to/file3", hash, padding);
        pfpNoPaddingSame = new ProjectFilePart("/path/to/file3", hash, padding);

        RandomUtil.getBytes(data);
        hash = new BigHash(data);
        pfpNoPaddingDifferent = new ProjectFilePart("/path/to/file4", hash, padding);

        assertEquals("Should be equal", pfpNoPadding, pfpNoPaddingSame);
        assertNotSame("Should not be equal", pfpNoPadding, pfpNoPaddingDifferent);

        assertEquals("Should be equal", pfpNoPadding.hashCode(), pfpNoPaddingSame.hashCode());
        assertNotSame("Should not be equal", pfpNoPadding.hashCode(), pfpNoPaddingDifferent.hashCode());

        // Test lengths w/ padding removed
        assertEquals("Expecting certain size.", dataSize, pfpPadding.getPaddingAdjustedLength());
        assertEquals("Expecting certain size.", dataSize, pfpPaddingSame.getPaddingAdjustedLength());
        assertEquals("Expecting certain size.", dataSize, pfpPaddingDifferent.getPaddingAdjustedLength());
        assertEquals("Expecting certain size.", dataSize, pfpNoPadding.getPaddingAdjustedLength());
        assertEquals("Expecting certain size.", dataSize, pfpNoPaddingSame.getPaddingAdjustedLength());
        assertEquals("Expecting certain size.", dataSize, pfpNoPaddingDifferent.getPaddingAdjustedLength());

        // Test padding length
        assertEquals("Expecting certain size.", paddingSize, pfpPadding.getPaddingLength());
        assertEquals("Expecting certain size.", paddingSize, pfpPaddingSame.getPaddingLength());
        assertEquals("Expecting certain size.", paddingSize, pfpPaddingDifferent.getPaddingLength());
        assertEquals("Expecting certain size.", 0, pfpNoPadding.getPaddingLength());
        assertEquals("Expecting certain size.", 0, pfpNoPaddingSame.getPaddingLength());
        assertEquals("Expecting certain size.", 0, pfpNoPaddingDifferent.getPaddingLength());
    }
}
