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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.tranche.TrancheServer;
import org.tranche.network.*;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.span.HashSpan;
import org.tranche.hash.*;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.commons.RandomUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ServerStartupThreadTest extends TrancheTestCase {

    private final boolean wasTestingHashSpanFixingThread = TestUtil.isTestingHashSpanFixingThread();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.setTestingActivityLogs(true);
        TestUtil.setTestingServerStartupThread(true);
        TestUtil.setTestingHashSpanFixingThread(false);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.setTestingActivityLogs(false);
        TestUtil.setTestingServerStartupThread(false);
        TestUtil.setTestingHashSpanFixingThread(wasTestingHashSpanFixingThread);
    }
    
    final String HOST1 = "bryan.com";
    final String HOST2 = "mark.gov";
    final String HOST3 = "augie.org";

    /**
     * <p>This test starts three servers: server 1, server 2, and server 3. The following happens:</p>
     * <ol>
     *   <li>Add a bunch of chunks to servers 1, 2 and 3.</li>
     *   <li>Turn server 1 offline.</li>
     *   <li>Delete 1/3 chunks from server 2 and 1/3 from server 3. That means a chunk will be deleted from one, both or neither.</li>
     *   <li>Add a few chunks to server 2 and a few different chunks to server 3.</li>
     *   <li>Turn server 1 online, and make sure it has the appropriate chunks after deletions and adds completed.</p>
     * </ol>
     * @throws java.lang.Exception
     */
    public void testServerStartupThreadPerformsDeletesAndSets() throws Exception {
        
        final boolean wasTestingActivityLogs = TestUtil.isTestingActivityLogs();
        
        TestUtil.setTestingActivityLogs(true);
        
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false));
        try {
            testNetwork.start();
            
            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            ffts1.getConfiguration().getHashSpans().add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));
            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            ffts2.getConfiguration().getHashSpans().add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));
            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            ffts3.getConfiguration().getHashSpans().add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));

            // Logs better be going and empty!
            assertEquals("Shouldn't be any activities yet.", 0, ffts1.getActivityLog().getActivityLogEntriesCount());
            assertEquals("Shouldn't be any activities yet.", 0, ffts2.getActivityLog().getActivityLogEntriesCount());
            assertEquals("Shouldn't be any activities yet.", 0, ffts3.getActivityLog().getActivityLogEntriesCount());

            TrancheServer rserver1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull("Remote server shouldn't be null.", rserver1);

            TrancheServer rserver2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull("Remote server shouldn't be null.", rserver2);

            TrancheServer rserver3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull("Remote server shouldn't be null.", rserver3);

            Set<BigHash> dataChunksToCheckFor = new HashSet();
            Set<BigHash> metaDataChunksToCheckFor = new HashSet();

            /**
             * ------------------------------------------------------------------------------------------------------------------------------
             *  STEP 1: Add a bunch of chunks to servers 1, 2 and 3.
             * ------------------------------------------------------------------------------------------------------------------------------
             */
            final int initialChunksToAdd = 18;
            for (int i = 0; i < initialChunksToAdd; i++) {
                boolean isMetaData = RandomUtil.getBoolean();
                if (isMetaData) {
                    byte[] chunk = DevUtil.createRandomMetaDataChunk();
                    BigHash hash = DevUtil.getRandomBigHash();

                    IOUtil.setMetaData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);
                    IOUtil.setMetaData(ffts2, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);
                    IOUtil.setMetaData(ffts3, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                    metaDataChunksToCheckFor.add(hash);
                } else {
                    byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
                    BigHash hash = new BigHash(chunk);

                    IOUtil.setData(ffts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);
                    IOUtil.setData(ffts2, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);
                    IOUtil.setData(ffts3, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                    dataChunksToCheckFor.add(hash);
                }
            }
            
            for (BigHash hash : dataChunksToCheckFor) {
                assertTrue("Should have.", ffts1.getDataBlockUtil().hasData(hash));
                assertTrue("Should have.", ffts2.getDataBlockUtil().hasData(hash));
                assertTrue("Should have.", ffts3.getDataBlockUtil().hasData(hash));
            }
            
            for (BigHash hash : metaDataChunksToCheckFor) {
                assertTrue("Should have.", ffts1.getDataBlockUtil().hasMetaData(hash));
                assertTrue("Should have.", ffts2.getDataBlockUtil().hasMetaData(hash));
                assertTrue("Should have.", ffts3.getDataBlockUtil().hasMetaData(hash));
            }

            assertEquals("Should know exactly how many activities there were.", initialChunksToAdd, ffts1.getActivityLog().getActivityLogEntriesCount());
            assertEquals("Should know exactly how many activities there were.", initialChunksToAdd, ffts2.getActivityLog().getActivityLogEntriesCount());
            assertEquals("Should know exactly how many activities there were.", initialChunksToAdd, ffts3.getActivityLog().getActivityLogEntriesCount());

            assertEquals("Should know how many chunks added.", initialChunksToAdd, metaDataChunksToCheckFor.size() + dataChunksToCheckFor.size());

            for (BigHash h : metaDataChunksToCheckFor) {
                assertTrue("Server should have chunk.", IOUtil.hasMetaData(rserver1, h));
                assertTrue("Server should have chunk.", IOUtil.hasMetaData(rserver2, h));
                assertTrue("Server should have chunk.", IOUtil.hasMetaData(rserver3, h));
            }

            for (BigHash h : dataChunksToCheckFor) {
                assertTrue("Server should have chunk.", IOUtil.hasData(rserver1, h));
                assertTrue("Server should have chunk.", IOUtil.hasData(rserver2, h));
                assertTrue("Server should have chunk.", IOUtil.hasData(rserver3, h));
            }

            /**
             * ------------------------------------------------------------------------------------------------------------------------------
             *  STEP 2: Turn server 1 offline.
             * ------------------------------------------------------------------------------------------------------------------------------
             */
            assertTrue("Should be online: "+HOST1, NetworkUtil.getStatus().getRow(HOST1).isOnline());
            testNetwork.suspendServer(HOST1);
            assertFalse("Shouldn't be online: "+HOST1, NetworkUtil.getStatus().getRow(HOST1).isOnline());

            /**
             * ------------------------------------------------------------------------------------------------------------------------------
             *  STEP 3: Delete 1/3 chunks from server 2 and 1/3 from server 3. That means a chunk will be deleted from one, both or neither.
             * ------------------------------------------------------------------------------------------------------------------------------
             */
            Set<BigHash> deletedDataChunks = new HashSet();
            Set<BigHash> deletedMetaDataChunks = new HashSet();

            final int chunksToDelete = (int) ((double) initialChunksToAdd / 3.0);

            List<BigHash> remainingDataChunks = new ArrayList(initialChunksToAdd);
            remainingDataChunks.addAll(dataChunksToCheckFor);
            List<BigHash> remainingMetaDataChunks = new ArrayList(initialChunksToAdd);
            remainingMetaDataChunks.addAll(metaDataChunksToCheckFor);

            for (int i = 0; i < chunksToDelete; i++) {
                // Delete something from server 2
                {
                    boolean isMetaData = RandomUtil.getBoolean();

                    // ---> CHANGE THESE VARS FOR WHATEVER SERVER <---

                    // Special cases: if no more chunks, use other type
                    if (remainingDataChunks.size() == 0) {
                        isMetaData = true;
                    } else if (remainingMetaDataChunks.size() == 0) {
                        isMetaData = false;
                    }

                    if (isMetaData) {
                        int index = RandomUtil.getInt(remainingMetaDataChunks.size());
                        BigHash hash = remainingMetaDataChunks.remove(index);

                        IOUtil.deleteMetaData(ffts2, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash);
                        IOUtil.deleteMetaData(ffts3, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash);

                        // Update collections so know what's going on when server started
                        // up again
                        if (!deletedMetaDataChunks.contains(hash)) {
                            deletedMetaDataChunks.add(hash);
                        }
                        metaDataChunksToCheckFor.remove(hash);
                    } else {
                        int index = RandomUtil.getInt(remainingDataChunks.size());
                        BigHash hash = remainingDataChunks.remove(index);

//                        System.err.println("DEBUG> DELETING --DATA-- FROM SERVER 2: " + hash);

                        IOUtil.deleteData(ffts2, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash);
                        IOUtil.deleteData(ffts3, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash);

                        // Update collections so know what's going on when server started
                        // up again
                        if (!deletedDataChunks.contains(hash)) {
                            deletedDataChunks.add(hash);
                        }
                        dataChunksToCheckFor.remove(hash);
                    }
                }
            }

            assertEquals("Should be able to anticipate.", initialChunksToAdd - chunksToDelete, remainingDataChunks.size() + remainingMetaDataChunks.size());

            assertEquals("Should know exactly how many activities there were.", initialChunksToAdd + chunksToDelete, ffts2.getActivityLog().getActivityLogEntriesCount());
            assertEquals("Should know exactly how many activities there were.", initialChunksToAdd + chunksToDelete, ffts3.getActivityLog().getActivityLogEntriesCount());

            /**
             * ------------------------------------------------------------------------------------------------------------------------------
             *  STEP 4: Add a few chunks to server 2 and a few different chunks to server 3.
             * ------------------------------------------------------------------------------------------------------------------------------
             */
            final int chunksToAdd = (int) ((double) initialChunksToAdd / 3.0);
            for (int i = 0; i < chunksToAdd; i++) {
                // Add something from server 2
                {
                    boolean isMetaData = RandomUtil.getBoolean();

                    // ---> CHANGE THESE VARS FOR WHATEVER SERVER <---
                    TrancheServer ts = ffts2;

                    if (isMetaData) {
                        byte[] chunk = DevUtil.createRandomMetaDataChunk();
                        BigHash hash = DevUtil.getRandomBigHash();

                        IOUtil.setMetaData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                        metaDataChunksToCheckFor.add(hash);
                    } else {
                        byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
                        BigHash hash = new BigHash(chunk);

                        IOUtil.setData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                        dataChunksToCheckFor.add(hash);
                    }
                }

                // Add something from server 3
                {
                    boolean isMetaData = RandomUtil.getBoolean();

                    // ---> CHANGE THESE VARS FOR WHATEVER SERVER <---
                    TrancheServer ts = ffts3;

                    if (isMetaData) {
                        byte[] chunk = DevUtil.createRandomMetaDataChunk();
                        BigHash hash = DevUtil.getRandomBigHash();

                        IOUtil.setMetaData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);

                        metaDataChunksToCheckFor.add(hash);
                    } else {
                        byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
                        BigHash hash = new BigHash(chunk);

                        IOUtil.setData(ts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);

                        dataChunksToCheckFor.add(hash);
                    }
                }
            }

            assertEquals("Should know exactly how many activities there were.", initialChunksToAdd + chunksToDelete + chunksToAdd, ffts2.getActivityLog().getActivityLogEntriesCount());
            assertEquals("Should know exactly how many activities there were.", initialChunksToAdd + chunksToDelete + chunksToAdd, ffts3.getActivityLog().getActivityLogEntriesCount());

            /**
             * ------------------------------------------------------------------------------------------------------------------------------
             *  STEP 5: Turn server 1 online, and make sure it has the appropriate chunks after deletions and adds completed.
             * ------------------------------------------------------------------------------------------------------------------------------
             */
            assertFalse("Shouldn't be online yet: "+HOST1, NetworkUtil.getStatus().getRow(HOST1).isOnline());
            testNetwork.resumeServer(HOST1);
            assertTrue("Should be online again: "+HOST1, NetworkUtil.getStatus().getRow(HOST1).isOnline());

            ServerStartupThread sst = testNetwork.getServer(HOST1).getServerStartupThread();
            assertNotNull("Startup thread should not be null.", sst);

            // Wait up to one minute
            boolean completed = sst.waitForStartupToComplete(60 * 1000);

            assertTrue("Startup thread should have completed in under a minute.", completed);

            for (BigHash h : dataChunksToCheckFor) {
                assertTrue("Should not have chunk: " + h, IOUtil.hasData(rserver1, h));
            }

            for (BigHash h : metaDataChunksToCheckFor) {
                assertTrue("Should not have chunk: " + h, IOUtil.hasMetaData(rserver1, h));
            }

            for (BigHash h : deletedDataChunks) {
                boolean hasData = IOUtil.hasData(rserver1, h);
                if (hasData) {
                    byte[] bytes = (byte[])IOUtil.getData(rserver1, h, false).getReturnValueObject();
                    System.out.println("Found data chunk shouldn't have: " + (bytes == null ? "null" : bytes.length + " bytes"));
                }
                assertFalse("Should not have chunk: " + h, hasData);
            }

            for (BigHash h : deletedMetaDataChunks) {
                boolean hasMetaData = IOUtil.hasMetaData(rserver1, h);
                if (hasMetaData) {
                    byte[] bytes = (byte[])IOUtil.getMetaData(rserver1, h, false).getReturnValueObject();
                    System.out.println("Found meta data chunk shouldn't have: " + (bytes == null ? "null" : bytes.length + " bytes"));
                }
                assertFalse("Should not have chunk: " + h, hasMetaData);
            }

        } finally {
            testNetwork.stop();
            TestUtil.setTestingActivityLogs(wasTestingActivityLogs);
        }
    }
}
