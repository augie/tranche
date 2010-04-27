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
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.logs.activity.Activity;
import org.tranche.network.ConnectionUtil;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.time.TimeUtil;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetActivityLogEntriesItemTest extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.setTestingActivityLogs(true);
        DebugUtil.setDebug(FlatFileTrancheServer.class, true);
        DebugUtil.setDebug(Server.class, true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.setTestingActivityLogs(false);
        DebugUtil.setDebug(FlatFileTrancheServer.class, false);
        DebugUtil.setDebug(Server.class, false);
    }

    public void testDoAction() throws Exception {
        TestUtil.printTitle("GetActivityLogEntriesItemTest:testDoAction()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();
            Server s = testNetwork.getServer(HOST1);

            long startTimestamp = TimeUtil.getTrancheTimestamp();
            // create fake data chunk
            byte[] bytes = DevUtil.createRandomDataChunk1MB();
            BigHash hash = new BigHash(bytes);
            PropagationReturnWrapper pew = IOUtil.setData(ConnectionUtil.connectHost(HOST1, false), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, bytes);
            assertFalse(pew.isAnyErrors());
            long endTimestamp = TimeUtil.getTrancheTimestamp();

            // create the item
            GetActivityLogEntriesItem item = new GetActivityLogEntriesItem(s);

            // 
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            GetActivityLogEntriesItem.writeRequest(false, startTimestamp, endTimestamp, 10, Activity.CHUNK_DATA, request);

            // execute
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            item.doAction(new ByteArrayInputStream(request.toByteArray()), response, "localhost");

            // verify
            ByteArrayInputStream bais = new ByteArrayInputStream(response.toByteArray());
            assertEquals(Token.OK_STRING, RemoteUtil.readLine(bais));
            Activity[] activities = GetActivityLogEntriesItem.readResponse(bais);
            assertEquals(1, activities.length);
            assertEquals(Activity.SET_DATA, activities[0].getAction());
            assertEquals(hash.toString(), activities[0].getHash().toString());
            assertTrue(activities[0].isData());
        } finally {
            testNetwork.stop();
        }
    }
}
