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
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class OperatingSystemUtilTest extends TrancheTestCase {
    
    public void testRunNativeCommand() throws Exception {
        OperatingSystem os = OperatingSystem.getCurrentOS();
        
        String cmd = "ls";
        if (os.isMSWindows()) {
            cmd = "dir";
        }
        
        int exitCode = OperatingSystemUtil.executeExternalCommand(cmd);
        assertEquals("Should have no problems listing files in directory.",0,exitCode);
    }
    
    public void testRedirectForUnix() throws Exception {
        
        File tmpRedirect = TempFileUtil.createTemporaryFile(".txt");
        
        try {
            // If test running on Windows, it's a negative test
            if (OperatingSystem.getCurrentOS().isMSWindows()) {
                try {
                    OperatingSystemUtil.executeUnixCommandWithOutputRedirect("java -version",tmpRedirect);
                    fail("Should have thrown an exception, Windows isn't supported");
                } catch(Exception nope) {/* expected */}
            }
            // Else it's a postive test
            else {
                long initialSize = tmpRedirect.length();
                OperatingSystemUtil.executeUnixCommandWithOutputRedirect("java -version",tmpRedirect);
                long redirectSize = tmpRedirect.length();
                assertTrue("Expecting file to be larger.",initialSize < redirectSize);
            }
        } finally {
            IOUtil.safeDelete(tmpRedirect);
        }
    }
    
}
