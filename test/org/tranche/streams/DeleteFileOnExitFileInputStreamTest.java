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
package org.tranche.streams;

import java.io.File;
import java.io.FileInputStream;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TrancheTestCase;
import org.tranche.util.Utils;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author besmit
 */
public class DeleteFileOnExitFileInputStreamTest extends TrancheTestCase {

    public void testDeletesFile() {
        deletesFile(256);
    }

    public void deletesFile(int size) {

        File temp = TempFileUtil.createTemporaryFile();

        try {

            byte[] randomData = Utils.makeRandomData(size);
            Utils.makeTempFile(randomData, temp);

            assertTrue("File should exist.", temp.exists());

            FileInputStream fin = new DeleteFileOnExitFileInputStream(temp);

            fin.close();

            assertFalse("File should not longer exist.", temp.exists());
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {

            // Just in case
            temp.delete();
        }

    }
}
