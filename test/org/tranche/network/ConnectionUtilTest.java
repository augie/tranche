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

import org.tranche.exceptions.TodoException;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ConnectionUtilTest extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        ConnectionUtil.setDebug(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        ConnectionUtil.setDebug(false);
    }

    public void testConnectHost() throws Exception {
        TestUtil.printTitle("ConnectionUtilTest:testConnectHost()");
        throw new TodoException();
    }

    public void testConnectURL() throws Exception {
        TestUtil.printTitle("ConnectionUtilTest:testConnectURL()");
        throw new TodoException();
    }

    public void testConnectRow() throws Exception {
        TestUtil.printTitle("ConnectionUtilTest:testConnectRow()");
        throw new TodoException();
    }

    public void testConnect() throws Exception {
        TestUtil.printTitle("ConnectionUtilTest:testConnect()");
        throw new TodoException();
    }

    public void testFlagOffline() throws Exception {
        TestUtil.printTitle("ConnectionUtilTest:testFlagOffline()");
        throw new TodoException();
    }

}
