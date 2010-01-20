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
package org.tranche.license;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.tranche.util.TrancheTestCase;

/**
 * 
 */
public class LicenseUtilTest extends TrancheTestCase {

    /**
     * Test of buildLicenseFile method, of class LicenseUtil.
     */
    public void testBuildLicenseFile() throws Exception {

        File outputFile = File.createTempFile("license", null);
        License license = new License("My License", "Short Description", "Description", false);
        List<String> additionalAgreements = null;
        Map<String, String> optionalNotes = null;
        File expResult = outputFile;
        File result = LicenseUtil.buildLicenseFile(outputFile, license, additionalAgreements, optionalNotes);
        assertEquals(expResult, result);
    }
}