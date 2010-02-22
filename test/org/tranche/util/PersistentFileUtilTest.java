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

/**
 * @author besmit
 */
public class PersistentFileUtilTest extends TrancheTestCase {

    public void testPersistentUtil() throws Exception {

        // Get persistent dir
        File persistentDir = PersistentServerFileUtil.getPersistentDirectory();

        // Set directory
        File faux = new File("/faux/path/to/persistent/dir");
        PersistentServerFileUtil.setPersistentDirectory(faux);

        assertEquals("Just testing set.", faux, PersistentServerFileUtil.getPersistentDirectory());

        // Set back
        PersistentServerFileUtil.setPersistentDirectory(persistentDir);

        File persistent = PersistentServerFileUtil.getPersistentFile("tranche.config");

        // Persistent should be in the persistent directory
        String parent = persistent.getParent();

        assertEquals("The parent of the persistent file should be persistent dir.", parent, persistentDir);
    }
}
