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

import org.tranche.TrancheServer;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.network.ConnectionUtil;
import org.tranche.remote.RemoteCallback;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServerWorkerThreadTest extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        ServerWorkerThread.setDebug(true);
        RemoteCallback.setDebug(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        ServerWorkerThread.setDebug(false);
        RemoteCallback.setDebug(false);
    }

    public void testKeepAlive() throws Exception {
        TestUtil.printTitle("ServerWorkerThreadTest:testKeepAlive()");
        
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();
            Server s = testNetwork.getServer(HOST1);
            FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(HOST1);

            // create some fake data chunks
            int numHashes = RandomUtil.getInt(10) + 1;
            int bogusHashes = RandomUtil.getInt(10);
            int numRequestedHashes = numHashes + bogusHashes;
            BigHash hashes[] = new BigHash[numRequestedHashes];
            for (int i = 0; i < numHashes; i++) {
                byte[] bytes = DevUtil.createRandomDataChunk(DataBlockUtil.ONE_MB);
                hashes[i] = new BigHash(bytes);
                IOUtil.setData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hashes[i], bytes);
            }
            for (int i = numHashes; i < numRequestedHashes; i++) {
                hashes[i] = DevUtil.getRandomBigHash();
            }

            // connect to server and download the data
            TrancheServer ts = null;
            try {
                // connect
                ts = ConnectionUtil.connectURL(s.getURL(), false);

                // pause the action
                ServerWorkerThread.setTestingKeepAlive(true);
                // batch get data
                PropagationReturnWrapper responseObject = ts.getData(hashes, false);

                System.out.println("Returned from get data.");

                // verify
                assertEquals(responseObject.getErrors().size(), bogusHashes);
                byte[][] responseByte2DArray = (byte[][]) responseObject.getReturnValueObject();
                assertEquals(numRequestedHashes, responseByte2DArray.length);
                for (int i = 0; i < numHashes; i++) {
                    assertEquals(hashes[i], new BigHash(responseByte2DArray[i]));
                }
                for (int i = numHashes; i < numRequestedHashes; i++) {
                    assertNull(responseByte2DArray[i]);
                }
            } finally {
                IOUtil.safeClose(ts);
            }
        } finally {
            testNetwork.stop();
            ServerWorkerThread.setTestingKeepAlive(false);
        }
    }
}
