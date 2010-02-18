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

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.tranche.ConfigureTranche;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.server.Server;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.ThreadUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServerStatusUpdateProcessTest extends NetworkPackageTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        ServerStatusUpdateProcess.setDebug(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        ServerStatusUpdateProcess.setDebug(false);
    }

    public void testCalculateNonCoreServersToUpdate() throws Exception {
        TestUtil.printTitle("ServerStatusUpdateProcessTest:testCalculateNonCoreServersToUpdate()");

        FlatFileTrancheServer ffts = null;
        File dir = null;
        Server s = null;
        try {
            // fake network
            Set<StatusTableRow> unsortedRows = new HashSet<StatusTableRow>();
            for (int i = 0; i < 5 + RandomUtil.getInt(10); i++) {
                StatusTableRow row = NetworkRandomUtil.createRandomStatusTableRow();
                row.setIsOnline(true);
                unsortedRows.add(row);
            }
            NetworkUtil.updateRows(unsortedRows);

            // local server
            dir = TempFileUtil.createTemporaryDirectory("testServer");
            ffts = new FlatFileTrancheServer(dir);
            s = new Server(ffts, RandomUtil.getInt(65535), RandomUtil.getBoolean());
            s.start();

            // wait for startup
            ThreadUtil.safeSleep(5000);

            // adjust
            ServerStatusUpdateProcess.adjustStatusTableRowRanges();

            // verify -- should not include local server
            assertEquals(NetworkUtil.getStatus().getRows().size() - 1, ServerStatusUpdateProcess.getNonCoreServersToUpdate().size());
            assertEquals(0, ServerStatusUpdateProcess.getStatusTableRowRanges().size());
        } finally {
            s.setRun(false);
            IOUtil.safeClose(s);
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDeleteWithWarning(dir);
            NetworkUtil.getStatus().clear();
        }
    }

    public void testCalculateAllCoreStatusTableRowRanges() throws Exception {
        TestUtil.printTitle("ServerStatusUpdateProcessTest:testCalculateAllCoreStatusTableRowRanges()");

        // set up -- makes the test verifiable
        ConfigureTranche.set(ConfigureTranche.PROP_STATUS_UPDATE_SERVER_GROUPING, "1");

        FlatFileTrancheServer ffts = null;
        File dir = null;
        Server s = null;
        try {
            // fake network
            Set<StatusTableRow> unsortedRows = new HashSet<StatusTableRow>();
            Set<String> startupServerURLs = new HashSet<String>();
            for (int i = 0; i < 5 + RandomUtil.getInt(10); i++) {
                StatusTableRow row = NetworkRandomUtil.createRandomStatusTableRow();
                row.setIsOnline(true);
                startupServerURLs.add(row.getURL());
                unsortedRows.add(row);
            }
            NetworkUtil.setStartupServerURLs(startupServerURLs);
            NetworkUtil.updateRows(unsortedRows);

            // local server
            dir = TempFileUtil.createTemporaryDirectory("testServer");
            ffts = new FlatFileTrancheServer(dir);
            s = new Server(ffts, RandomUtil.getInt(65535), RandomUtil.getBoolean());
            s.start();

            // wait for startup
            ThreadUtil.safeSleep(5000);

            // adjust
            ServerStatusUpdateProcess.adjustStatusTableRowRanges();

            // verify -- should include local server
            assertEquals(NetworkUtil.getStatus().getRows().size(), ServerStatusUpdateProcess.getStatusTableRowRanges().size());
        } finally {
            s.setRun(false);
            IOUtil.safeClose(s);
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDeleteWithWarning(dir);
        }
    }
}
