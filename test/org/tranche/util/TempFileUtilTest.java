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
package org.tranche.util;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class TempFileUtilTest extends TrancheTestCase {

    /**
     * Tests capability of temp directory to create files with a specific name.
     */
    public void testCreateTempFilesWithSpecificNames() throws Exception {
        Set<File> toDelete = new HashSet();
        int numberOfGenericTempFiles = 20;

        // Simulate authentic conditions by first create some temp files
        for (int i = 0; i < numberOfGenericTempFiles; i++) {
            toDelete.add(TempFileUtil.createTemporaryFile());
        }

        // Create a file with a specific
        File f1 = TempFileUtil.createTempFileWithName("license.txt");
        toDelete.add(f1);

        // Shouldn't have any problems creating another license.txt
        File anotherf1 = TempFileUtil.createTempFileWithName("license.txt");
        assertTrue("Should exist.", f1.exists());
        assertTrue("Should exist.", anotherf1.exists());
        assertEquals("Both files should have same base name.", f1.getName(), anotherf1.getName());

        // Throw in a few for kicks. No exceptions.
        toDelete.add(TempFileUtil.createTempFileWithName("license.txt"));
        toDelete.add(TempFileUtil.createTempFileWithName("license.txt"));
        toDelete.add(TempFileUtil.createTempFileWithName("license.txt"));
        toDelete.add(TempFileUtil.createTempFileWithName("license.txt"));
        toDelete.add(TempFileUtil.createTempFileWithName("license.txt"));

        // Let's create a couple more
        File f2 = TempFileUtil.createTempFileWithName("favorite-hash-list");
        toDelete.add(f2);
        File f3 = TempFileUtil.createTempFileWithName("gui.preferences");
        toDelete.add(f3);

        assertTrue("Better exist.", f1.exists());
        assertEquals("Expecting certain name.", "license.txt", f1.getName());

        assertTrue("Better exist.", f2.exists());
        assertEquals("Expecting certain name.", "favorite-hash-list", f2.getName());

        assertTrue("Better exist.", f3.exists());
        assertEquals("Expecting certain name.", "gui.preferences", f3.getName());
    }

    public void testCreateTempDirectories() {
        File tmpDir0 = TempFileUtil.createTemporaryDirectory();
        File tmpDir1 = TempFileUtil.createTemporaryDirectory();
        File tmpDir2 = TempFileUtil.createTemporaryDirectory();
        File tmpDir3 = TempFileUtil.createTemporaryDirectory();

        File buildDir0 = TempFileUtil.createTemporaryDirectory("build");
        File buildDir1 = TempFileUtil.createTemporaryDirectory("build");

        File cacheDir0 = TempFileUtil.createTemporaryDirectory("cache");
        File cacheDir1 = TempFileUtil.createTemporaryDirectory("cache");
        File cacheDir2 = TempFileUtil.createTemporaryDirectory("cache");

        assertEquals("Expecting certain directory name, instead found: " + tmpDir0.getName(), "dir", tmpDir0.getName());
        assertEquals("Expecting certain directory name, instead found: " + tmpDir1.getName(), "dir1", tmpDir1.getName());
        assertEquals("Expecting certain directory name, instead found: " + tmpDir2.getName(), "dir2", tmpDir2.getName());
        assertEquals("Expecting certain directory name, instead found: " + tmpDir3.getName(), "dir3", tmpDir3.getName());

        assertEquals("Expecting certain directory name", "build", buildDir0.getName());
        assertEquals("Expecting certain directory name", "build1", buildDir1.getName());

        assertEquals("Expecting certain directory name", "cache", cacheDir0.getName());
        assertEquals("Expecting certain directory name", "cache1", cacheDir1.getName());
        assertEquals("Expecting certain directory name", "cache2", cacheDir2.getName());
    }
}
