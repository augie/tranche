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
import org.tranche.commons.DebugUtil;
import org.tranche.security.Signature;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.span.HashSpan;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ShutdownItemTest extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        DebugUtil.setDebug(FlatFileTrancheServer.class, true);
        DebugUtil.setDebug(Server.class, true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        DebugUtil.setDebug(FlatFileTrancheServer.class, false);
        DebugUtil.setDebug(Server.class, false);
    }

    public void testDoAction() throws Exception {
        TestUtil.printTitle("ShutdownItemTest:testDoAction()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();
            Server s = testNetwork.getServer(HOST1);
            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);

            // get a nonce from the server
            byte[] nonce = IOUtil.getNonce(ffts);
            // sign the bytes
            String algorithm = SecurityUtil.getSignatureAlgorithm(DevUtil.getDevPrivateKey());
            byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(nonce), DevUtil.getDevPrivateKey(), algorithm);
            Signature signature = new Signature(sig, algorithm, DevUtil.getDevAuthority());

            // wait for startup
            ffts.waitToLoadExistingDataBlocks();
            s.waitForStartup(1000);

            // create the item
            ShutdownItem item = new ShutdownItem(s);

            // create the bytes
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            ShutdownItem.writeRequest(false, signature, nonce, request);

            // execute
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            item.doAction(new ByteArrayInputStream(request.toByteArray()), response, "localhost");

            // verify
            String responseLine = RemoteUtil.readLine(new ByteArrayInputStream(response.toByteArray()));
            assertEquals(Token.OK_STRING, responseLine);
        } finally {
            testNetwork.stop();
        }
    }
}
