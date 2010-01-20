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

/**
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class OperatingSystemTest extends TrancheTestCase {
    
    public void testManyOperatingSystems() throws Exception {
        // Known
        OperatingSystem testWinXP = OperatingSystem.testOS("Windows XP");
        OperatingSystem testWinNT = OperatingSystem.testOS("Windows NT");
        OperatingSystem testWin2000 = OperatingSystem.testOS("Windows 2000");
        OperatingSystem testWin98 = OperatingSystem.testOS("Windows 98");
        OperatingSystem testWin95 = OperatingSystem.testOS("Windows 95");
        
        OperatingSystem testMacOS = OperatingSystem.testOS("Mac OS");
        OperatingSystem testLinux = OperatingSystem.testOS("Linux");
        OperatingSystem testFreeBSD = OperatingSystem.testOS("FreeBSD");
        
        // Unknown
        OperatingSystem testOther1 = OperatingSystem.testOS("OS/2");
        OperatingSystem testOther2 = OperatingSystem.testOS("MPE/iX");
        OperatingSystem testOther3 = OperatingSystem.testOS("HP UX");
        OperatingSystem testOther4 = OperatingSystem.testOS("garba.ge");
        
        // Not found
        OperatingSystem testNotFound1 = OperatingSystem.testOS(null);
        OperatingSystem testNotFound2 = OperatingSystem.testOS("");
        
        // Throw them all is a collection
        OperatingSystem[] testOSArray = {
            testWinXP, testWinNT, testWin2000, testWin98, testWin95,
            testMacOS, testLinux, testFreeBSD,
            testOther1, testOther2, testOther3, testOther4,
            testNotFound1, testNotFound2
        };
        
        OperatingSystem[] windowsOSArray = {
            testWinXP, testWinNT, testWin2000, testWin98, testWin95
        };
        
        OperatingSystem[] nonWindowsOSArray = {
            testMacOS, testLinux, testFreeBSD,
            testOther1, testOther2, testOther3, testOther4,
            testNotFound1, testNotFound2
        };
        
        assertEquals("no match.",testOSArray.length,(windowsOSArray.length+nonWindowsOSArray.length));
        
        for (OperatingSystem win : windowsOSArray) {
            assertTrue("Should be a win box.",win.isMSWindows());
        }
        for (OperatingSystem win : nonWindowsOSArray) {
            assertFalse("Should NOT be a win box.",win.isMSWindows());
        }
        
        int windowsCount = 0;
        int nonWindowsCount = 0;
        int notFoundCount = 0;
        int otherCount = 0;
        
        // Test equality against each!!!
        for (OperatingSystem next : testOSArray) {
            // Count windows/non-windows
            if (next.isMSWindows()) {
                windowsCount++;
            } else {
                nonWindowsCount++;
            }
            // Count other
            if (next.equals(OperatingSystem.OTHER)) {
                otherCount++;
            }
            // Count not found
            if (next.equals(OperatingSystem.NO_INFORMATION_FOUND)) {
                notFoundCount++;
            }
            
            String shouldNotThrowException = next.toString();
            
            int matchCount = 0;
            for (OperatingSystem os : testOSArray) {
                boolean isMatch = next.equals(os);
                if (isMatch) {
                    matchCount++;
                }
            }
            
            if (next.equals(OperatingSystem.OTHER)) {
                assertEquals("Should match the 4 others",4,matchCount);
            }
            else if (next.equals(OperatingSystem.NO_INFORMATION_FOUND)) {
                assertEquals("no match.",2,matchCount);
            }
            else {
                assertEquals("no match.",1,matchCount);
            }
        }
        
        assertEquals("no match.",windowsOSArray.length,windowsCount);
        assertEquals("no match.",nonWindowsOSArray.length,nonWindowsCount);
        assertEquals("no match.",2,notFoundCount);
        assertEquals("no match.",4,otherCount);
    }
    
    public void testPrintCurrentOperatingSystem() throws Exception {
        System.out.println("Operating system: "+OperatingSystem.getCurrentOS());
    }
    
    public void testEqualityOfOperatingSystemInstance() throws Exception {
        OperatingSystem os1 = OperatingSystem.testOS("SuperOS");
        OperatingSystem os2 = OperatingSystem.testOS("SuperOS");
        assertTrue(os1.equals(os2));
        
        String random = new String("SuperOS");
        assertFalse(os1.equals(random));
    }
}
