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
import org.tranche.security.Signature;
import org.tranche.configuration.ConfigurationUtil;
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
public class SetConfigurationItemTest extends TrancheTestCase {

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
        TestUtil.printTitle("SetConfigurationItemTest:testDoAction()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();
            Server s = testNetwork.getServer(HOST1);
            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);

            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                ConfigurationUtil.write(ffts.getConfiguration(), baos);
            } finally {
                IOUtil.safeClose(baos);
            }
            byte[] configBytes = baos.toByteArray();
            byte[] nonce = IOUtil.getNonce(ffts);

            // make the bytes
            byte[] bytes = new byte[configBytes.length + nonce.length];
            System.arraycopy(configBytes, 0, bytes, 0, configBytes.length);
            System.arraycopy(nonce, 0, bytes, configBytes.length, nonce.length);
            // sign the bytes
            String algorithm = SecurityUtil.getSignatureAlgorithm(DevUtil.getDevPrivateKey());
            byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(bytes), DevUtil.getDevPrivateKey(), algorithm);
            // make a proper signature object
            Signature signature = new Signature(sig, algorithm, DevUtil.getDevAuthority());

            // create the item
            SetConfigurationItem item = new SetConfigurationItem(s);

            // create the bytes
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            SetConfigurationItem.writeRequest(false, configBytes, signature, nonce, request);

            // execute
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            item.doAction(new ByteArrayInputStream(request.toByteArray()), response, "localhost");

            // verify
            assertEquals(Token.OK_STRING, RemoteUtil.readLine(new ByteArrayInputStream(response.toByteArray())));
        } finally {
            testNetwork.stop();
        }
    }
}
