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

import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ServerConfigurationTest extends TrancheTestCase {

    public void testClone() {
        // Create two different configurations. Just like real network, here are real examples!
        ServerConfiguration wsu1Config = new ServerConfiguration(ServerConfiguration.DFS, 443, "146.9.4.103");
        ServerConfiguration dataRecovery209Config = new ServerConfiguration(ServerConfiguration.DFS, 1500, "141.214.182.209");

        assertFalse("Two configs should be different.", wsu1Config.equals(dataRecovery209Config));

        // Clone the WSU #1 example
        ServerConfiguration wsu1ConfigClone = wsu1Config.clone();

        // Show it has same info as WSU #1 example, but is an entirely different object (clone)
        assertEquals("Should have same info as config.", wsu1Config, wsu1ConfigClone);
        assertNotSame("Should have different reference since deep clone.", wsu1Config, wsu1ConfigClone);

        // Set the clone to be like 209 data recovery example
        wsu1ConfigClone.setHostName(dataRecovery209Config.getHostName());
        wsu1ConfigClone.setPort(dataRecovery209Config.getPort());
        wsu1ConfigClone.setType(dataRecovery209Config.getType());

        // Show that the clone is like 209 data recovery example
        assertEquals("Should have same info as config.", dataRecovery209Config, wsu1ConfigClone);
        assertNotSame("Should have different reference since deep clone.", dataRecovery209Config, wsu1ConfigClone);

        // Show it is no longer anything like WSU #1 example
        assertFalse("Should not have same info as config.", wsu1Config.equals(wsu1ConfigClone));
        assertNotSame("Should have different reference since deep clone.", wsu1Config, wsu1ConfigClone);

        // Show that WSU #1 example didn't change when its clone did
        assertEquals("Better not have changed.", ServerConfiguration.DFS, wsu1Config.getType());
        assertEquals("Better not have changed.", 443, wsu1Config.getPort());
        assertEquals("Better not have changed.", "146.9.4.103", wsu1Config.getHostName());
    }
}