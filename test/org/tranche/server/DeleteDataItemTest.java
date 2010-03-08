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
import java.io.File;
import org.tranche.security.Signature;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class DeleteDataItemTest extends TrancheTestCase {

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
        TestUtil.printTitle("DeleteDataItemTest:testDoAction()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();
            Server s = testNetwork.getServer(HOST1);
            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);

            // create fake data chunk
            byte[] bytes = DevUtil.createRandomDataChunk1MB();
            BigHash hash = new BigHash(bytes);
            PropagationReturnWrapper pew = IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, bytes);
            assertFalse(pew.isAnyErrors());

            // create the item
            DeleteDataItem item = new DeleteDataItem(s);

            byte[][] nonces = new byte[1][];
            byte[] nonce = IOUtil.getNonce(ffts);
            nonces[0] = nonce;
            // sign the nonce + hash
            byte[] hashBytes = hash.toByteArray();
            byte[] sigBytes = new byte[nonce.length + hash.toByteArray().length];
            System.arraycopy(nonce, 0, sigBytes, 0, nonce.length);
            System.arraycopy(hashBytes, 0, sigBytes, nonce.length, hashBytes.length);
            String algorithm = SecurityUtil.getSignatureAlgorithm(DevUtil.getDevPrivateKey());
            byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(sigBytes), DevUtil.getDevPrivateKey(), algorithm);
            // make a proper signature object
            Signature[] signatures = new Signature[1];
            Signature signature = new Signature(sig, algorithm, DevUtil.getDevAuthority());
            signatures[0] = signature;

            // create the bytes
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            DeleteDataItem.writeRequest(false, hash, new String[] {HOST1}, signatures, nonces, request);

            // execute
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            item.doAction(new ByteArrayInputStream(request.toByteArray()), response, "localhost");

            // verify
            ByteArrayInputStream in = new ByteArrayInputStream(response.toByteArray());
            assertEquals(Token.OK_STRING, RemoteUtil.readLine(in));
            PropagationReturnWrapper wrapper = PropagationReturnWrapper.createFromBytes(RemoteUtil.readDataBytes(in));
            assertEquals(0, wrapper.getErrors().size());
            assertFalse(IOUtil.hasData(ffts, hash));
        } finally {
            testNetwork.stop();
        }
    }
}
