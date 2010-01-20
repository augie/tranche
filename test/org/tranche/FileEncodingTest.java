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

import org.tranche.hash.BigHash;
import java.util.Properties;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;
import org.tranche.util.Utils;

/**
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class FileEncodingTest extends TrancheTestCase {

    public void testToStringAndBack() {
        TestUtil.printTitle("FileEncodingTest:testToStringAndBack()");
        
        // Create a test file encoding
        Properties props = new Properties();
        props.setProperty("name", "test.pkl");
        BigHash hash = new BigHash(Utils.makeRandomData(1000));
        FileEncoding fileEnc1 = new FileEncoding("None", hash);

        // Create string representation
        String fileEncString = fileEnc1.toString();

        // Back...
        FileEncoding fileEnc2 = FileEncoding.createFromString(fileEncString);

        assertEquals("Props should be same", fileEnc1, fileEnc2);
    }
}
