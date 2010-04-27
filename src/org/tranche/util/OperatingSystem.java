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

import org.tranche.commons.Debuggable;

/**
 * <p>Encapsulates identity of operating system.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class OperatingSystem extends Debuggable {

    private static final String OTHER_TOKEN = "Match anything remaining!!!";
    /**
     * <p>Represents Windows Vista</p>
     */
    public static final OperatingSystem WINDOWS_VISTA = new OperatingSystem("Windows Vista");
    /**
     * <p>Represents Windows XP</p>
     */
    public static final OperatingSystem WINDOWS_XP = new OperatingSystem("Windows XP");
    /**
     * <p>Represents Windows NT</p>
     */
    public static final OperatingSystem WINDOWS_NT = new OperatingSystem("Windows NT");
    /**
     * <p>Represents Windows 2000</p>
     */
    public static final OperatingSystem WINDOWS_2000 = new OperatingSystem("Windows 2000");
    /**
     * <p>Represents Windows 98</p>
     */
    public static final OperatingSystem WINDOWS_98 = new OperatingSystem("Windows 98");
    /**
     * <p>Represents Windows 95</p>
     */
    public static final OperatingSystem WINDOWS_95 = new OperatingSystem("Windows 95");
    /**
     * <p>Represents Linux</p>
     */
    public static final OperatingSystem LINUX = new OperatingSystem("Linux");
    /**
     * <p>Represents FreeBSD</p>
     */
    public static final OperatingSystem FREE_BSD = new OperatingSystem("FreeBSD");
    /**
     * <p>Represents Mac OS</p>
     */
    public static final OperatingSystem MAC_OS = new OperatingSystem("Mac OS");
    /**
     * <p>Represents an unknown operating system</p>
     */
    public static final OperatingSystem OTHER = new OperatingSystem(OTHER_TOKEN);
    /**
     * <p>Represents scenario where no information about operating system was found</p>
     */
    public static final OperatingSystem NO_INFORMATION_FOUND = new OperatingSystem(null);
    /**
     * Not the most agile solution, but gets the job done. Any new OSes -- besides
     * OTHER and NOT_FOUND -- need to be added to known OS array.
     */
    private static final OperatingSystem[] knownOperatingSystems = {
        WINDOWS_XP, WINDOWS_NT, WINDOWS_2000, WINDOWS_98, WINDOWS_95,
        LINUX, MAC_OS, FREE_BSD, WINDOWS_VISTA
    };
    /**
     * Not the most agile solution, but gets the job done. Add any Windows versions here.
     */
    private static final OperatingSystem[] windowsOperatingSystems = {
        WINDOWS_VISTA, WINDOWS_XP, WINDOWS_NT, WINDOWS_2000, WINDOWS_98, WINDOWS_95
    };
    private String OS;

    /**
     * 
     * @param OS
     */
    private OperatingSystem(String OS) {
        this.OS = OS;
    }
    private static OperatingSystem thisOperatingSystem = null;

    /**
     * <p>Returns the user's operating system.</p>
     * @return
     */
    public static OperatingSystem getCurrentOS() {

        if (thisOperatingSystem == null) {
            String osName = null;
            try {
                osName = System.getProperty("os.name");
            } catch (Exception ex) {
                System.err.println("Exception: " + ex.getMessage());
            }

            if (osName == null) {
                return OperatingSystem.NO_INFORMATION_FOUND;
            }

            thisOperatingSystem = new OperatingSystem(osName);
        }

        return thisOperatingSystem;
    }

    /**
     * <p>Used for testing only.</p>
     * @param testName
     * @return
     */
    public static OperatingSystem testOS(String testName) {
        return new OperatingSystem(testName);
    }

    /**
     * <p>Returns true if a windows box, else false.</p>
     * @return
     */
    public boolean isMSWindows() {
        for (OperatingSystem win : windowsOperatingSystems) {
            if (this.equals(win)) {
                return true;
            }
        }

        return false;
    }

    @Override()
    public boolean equals(Object o) {
        if (o instanceof OperatingSystem) {
            OperatingSystem O = (OperatingSystem) o;

            // See whether found (avoid NullPointerException)
            boolean thisUnknown = this.OS == null || this.OS.trim().equals("");
            boolean objUnknown = O.OS == null || O.OS.trim().equals("");

            if (thisUnknown && objUnknown) {
                return true;
            } else if (thisUnknown && !objUnknown || !thisUnknown && objUnknown) {
                return false;
            }

            // Try to match others
            boolean thisOther = true;
            boolean objOther = true;

            for (OperatingSystem next : knownOperatingSystems) {
                if (next.OS.equals(O.OS)) {
                    objOther = false;
                }
                if (next.OS.equals(this.OS)) {
                    thisOther = false;
                }
                // Safely exit if both already found
                if (!thisOther && !objOther) {
                    break;
                }
            }

            if (thisOther && objOther) {
                return true;            // Just match the string
            }
            return (O.OS.equals(this.OS));
        }

        return o.equals(this);
    }

    @Override()
    public String toString() {
        return this.OS;
    }
}
