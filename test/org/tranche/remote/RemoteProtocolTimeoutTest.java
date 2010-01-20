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
package org.tranche.remote;

import org.tranche.TrancheServer;
import org.tranche.hash.span.HashSpan;
import org.tranche.network.ConnectionUtil;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.ThreadUtil;
import org.tranche.util.TrancheTestCase;

/**
 * A set of tests that verify that the protocol works as expected when timeouts occur.
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class RemoteProtocolTimeoutTest extends TrancheTestCase {

    /**
     * Test what happens when a timeout occurs. Make sure that the code gracefully recovers. The RemoteTrancheServer class should attempt to retry connections up to three times.
     */
    public void testTimeout() throws Exception {
        TestUtil.printTitle("RemoteProtocolTimeoutTest:testTimeout()");

        String HOST1 = "server1.com";
        String[] hosts = {HOST1};
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            TrancheServer ts = ConnectionUtil.connectHost(HOST1, true);

            // verify the connection
            IOUtil.getNonces(ts, hosts);

            // wait until the connection surely timesout
            ThreadUtil.safeSleep(40 * 1000);

            // get another nonce
            IOUtil.getNonces(ts, hosts);
        } finally {
            testNetwork.stop();
        }
    }
}
