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
package org.tranche.network;

import java.util.HashSet;
import java.util.Set;
import org.tranche.util.RandomUtil;
import org.tranche.util.TestUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class NetworkUtilTest extends NetworkPackageTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        NetworkUtil.setDebug(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        NetworkUtil.setDebug(false);
    }

    public void testStartupServerURLs() throws Exception {
        TestUtil.printTitle("NetworkUtilTest:testStartupServerURLs()");

        // variables
        Set<String> serverURLs = new HashSet<String>();
        for (int i = 0; i < 5 + RandomUtil.getInt(10); i++) {
            serverURLs.add(RandomUtil.getString(5 + RandomUtil.getInt(10)).trim().toLowerCase());
        }

        // sets
        NetworkUtil.setStartupServerURLs(serverURLs);

        // verify
        assertEquals(serverURLs.size(), NetworkUtil.getStartupServerURLs().size());
        for (String serverURL : NetworkUtil.getStartupServerURLs()) {
            assertEquals(true, serverURLs.contains(serverURL));
        }
    }
}
