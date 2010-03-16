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
package org.tranche.flatfile;

import java.io.File;
import java.math.BigInteger;
import org.tranche.configuration.Configuration;
import org.tranche.hash.BigHash;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class DataDirectoryConfigurationTest extends TrancheTestCase {

    /**
     * <p>Make sure clone method works.</p>
     */
    public void testClone() {
        TestUtil.printTitle("DataDirectoryConfigurationTest:testClone()");

        DataDirectoryConfiguration ddc1 = null, ddc2 = null;
        File dir1 = null, dir2 = null;
        try {
            dir1 = TempFileUtil.createTemporaryDirectory();
            dir2 = TempFileUtil.createTemporaryDirectory();

            assertFalse("Directories must be distinct.", dir1.equals(dir2));

            // Make first DDC
            ddc1 = new DataDirectoryConfiguration(dir1.getAbsolutePath(), 1024 * 1024);
            System.out.println("DEBUG> " + dir1.getAbsolutePath());
            ddc1.adjustUsedSpace(5 * 1024);

            assertEquals("Expecting certain directory.", dir1, ddc1.getDirectoryFile());
            assertEquals("Expecting certain total space.", 1024 * 1024, ddc1.getSizeLimit());
            assertEquals("Expecting certain used space.", 5 * 1024, ddc1.getActualSize());

            // Make second DDC
            ddc2 = new DataDirectoryConfiguration(dir2.getAbsolutePath(), 512 * 1024);
            ddc2.adjustUsedSpace(256 * 1024);

            assertEquals("Expecting certain directory.", dir2, ddc2.getDirectoryFile());
            assertEquals("Expecting certain total space.", 512 * 1024, ddc2.getSizeLimit());
            assertEquals("Expecting certain used space.", 256 * 1024, ddc2.getActualSize());

            assertFalse("Two DDCs should not equal.", ddc1.equals(ddc2));

            DataDirectoryConfiguration ddc1Clone = ddc1.clone();

            // Should be a deep copy, not same object
            assertNotSame("Deep copy should not be same as any object.", ddc1, ddc1Clone);
            assertNotSame("Deep copy should not be same as any object.", ddc2, ddc1Clone);

            assertEquals("Should equal DDC.", ddc1, ddc1Clone);
            assertFalse("Should not equal DDC.", ddc2.equals(ddc1Clone));

            assertEquals("Expecting certain directory.", ddc1.getDirectoryFile(), ddc1Clone.getDirectoryFile());
            assertEquals("Expecting certain total space.", ddc1.getSizeLimit(), ddc1Clone.getSizeLimit());
            assertEquals("Expecting certain used space.", ddc1.getActualSize(), ddc1Clone.getActualSize());

            DataDirectoryConfiguration ddc2Clone = ddc2.clone();

            // Should be a deep copy, not same object
            assertNotSame("Deep copy should not be same as any object.", ddc1, ddc2Clone);
            assertNotSame("Deep copy should not be same as any object.", ddc2, ddc2Clone);

            assertEquals("Should equal DDC.", ddc2, ddc2Clone);
            assertFalse("Should not equal DDC.", ddc1.equals(ddc2Clone));

            assertEquals("Expecting certain directory.", ddc2.getDirectoryFile(), ddc2Clone.getDirectoryFile());
            assertEquals("Expecting certain total space.", ddc2.getSizeLimit(), ddc2Clone.getSizeLimit());
            assertEquals("Expecting certain used space.", ddc2.getActualSize(), ddc2Clone.getActualSize());

            // Let's change the clone. Should not change ddc.
            ddc1Clone.setDirectory(ddc2.getDirectory());
            ddc1Clone.setSizeLimit(3 * 1024 * 1024);
            ddc1Clone.adjustUsedSpace(2 * 1024);

            assertEquals("Expecting certain directory.", ddc2.getDirectory(), ddc1Clone.getDirectory());
            assertEquals("Expecting certain size limit.", 3 * 1024 * 1024, ddc1Clone.getSizeLimit());
            assertEquals("Expecting certain used space.", 2 * 1024, ddc1Clone.getActualSize());

            assertFalse("Should no longer be equal.", ddc1.equals(ddc1Clone));
            assertFalse("Should no longer be equal.", ddc1.getDirectory().equals(ddc1Clone.getDirectory()));
            assertFalse("Should no longer be equal.", ddc1.getSizeLimit() == ddc1Clone.getSizeLimit());
            assertFalse("Should no longer be equal.", ddc1.getActualSize() == ddc1Clone.getActualSize());
        } finally {
            IOUtil.recursiveDeleteWithWarning(dir1);
            IOUtil.recursiveDeleteWithWarning(dir2);
        }
    }

    /**
     * <p>Discovered that changing DDC to end with slash breaks the server!</p>
     * @throws java.lang.Exception
     */
    public void testPathEndsWithForwardSlash() throws Exception {
        TestUtil.printTitle("DataDirectoryConfigurationTest:testPathEndsWithForwardSlash()");
        FlatFileTrancheServer ffserver = null;

        File dir = null;

        try {

            dir = new File(TempFileUtil.createTemporaryDirectory().getAbsolutePath());
            dir.mkdirs();

            ffserver = new FlatFileTrancheServer(dir.getAbsoluteFile());
            ffserver.getConfiguration().getUsers().add(DevUtil.getDevUser());

            BigHash[] hashes = ffserver.getDataHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Expecting should report no chunks.", 0, hashes.length);

            // Let's cause trouble by adding a forward slash to end
            Configuration c = ffserver.getConfiguration();
            for (DataDirectoryConfiguration ddc : c.getDataDirectories()) {
                ddc.setDirectory(ddc.getDirectory() + "/");
//                assertTrue("Should (temporarily) end with slash.", ddc.getDirectory().endsWith("/"));
            }

            IOUtil.setConfiguration(ffserver, c, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

            // Add data. We'll use this later to see if problem fixed.
            byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
            BigHash hash = new BigHash(chunk);

            IOUtil.setData(ffserver, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);
            hashes = ffserver.getDataHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Expecting should report one chunk.", 1, hashes.length);

            IOUtil.safeClose(ffserver);

            // Make sure closed
            Thread.sleep(500);

            // Start up again
            ffserver = new FlatFileTrancheServer(dir.getAbsoluteFile());

            c = ffserver.getConfiguration();
            ffserver.waitToLoadExistingDataBlocks();

            hashes = ffserver.getDataHashes(BigInteger.ZERO, BigInteger.TEN);
            assertEquals("Expecting should report one chunk after restarting.", 1, hashes.length);

            for (DataDirectoryConfiguration ddc : c.getDataDirectories()) {
                assertFalse("Should not end with slash.", ddc.getDirectory().endsWith("/"));
            }
        } finally {
            IOUtil.safeClose(ffserver);
            IOUtil.recursiveDeleteWithWarning(dir);
        }
    }
}