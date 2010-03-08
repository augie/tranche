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
package org.tranche.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class RegisterServerItemTest extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        FlatFileTrancheServer.setDebug(true);
        Server.setDebug(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        FlatFileTrancheServer.setDebug(false);
        Server.setDebug(false);
    }

    public void testDoAction() throws Exception {
        TestUtil.printTitle("RegisterServerItemTest:testDoAction()");

        String HOST1 = "server1.com";
        String HOST2 = "server2.com";
        
        // set up a dummy network
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();

            // create the item
            RegisterServerItem itm = new RegisterServerItem(testNetwork.getServer(HOST1));

            // create the bytes
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            RegisterServerItem.writeRequest(false, testNetwork.getServer(HOST2).getURL(), request);

            // set up
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            itm.doAction(new ByteArrayInputStream(request.toByteArray()), response, "localhost");

            // verify
            assertEquals(Token.OK_STRING, RemoteUtil.readLine(new ByteArrayInputStream(response.toByteArray())));
        } finally {
            testNetwork.stop();
        }
    }
}