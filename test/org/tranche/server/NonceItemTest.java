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
import java.util.HashSet;
import java.util.Set;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.flatfile.NonceMap;
import org.tranche.hash.span.HashSpan;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.util.DevUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class NonceItemTest extends TrancheTestCase {

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
        TestUtil.printTitle("NonceItemTest:testDoAction()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();
            Server s = testNetwork.getServer(HOST1);

            int count = 25;
            Set <StatusTableRow> rows = new HashSet<StatusTableRow>();
            rows.add(new StatusTableRow(s));
            NetworkUtil.updateRows(rows);
            String[] hosts = new String[1];
            hosts[0] = s.getHostName();

            // create the item
            NonceItem item = new NonceItem(s);

            // create the bytes
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            NonceItem.writeRequest(false, count, hosts, request);

            // execute
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            item.doAction(new ByteArrayInputStream(request.toByteArray()), response, "localhost");

            // verify
            ByteArrayInputStream in = new ByteArrayInputStream(response.toByteArray());
            assertEquals(Token.OK_STRING, RemoteUtil.readLine(in));
            PropagationReturnWrapper wrapper = PropagationReturnWrapper.createFromBytes(RemoteUtil.readDataBytes(in));
            byte[][][] responseData = (byte[][][]) wrapper.getReturnValueObject();
            assertEquals(1, responseData.length);
            assertEquals(count, responseData[0].length);
            for (int i = 0; i < count; i++) {
                assertEquals(NonceMap.NONCE_BYTES, responseData[0][i].length);
            }
            assertEquals(0, wrapper.getErrors().size());
        } finally {
            testNetwork.stop();
        }
    }
}
