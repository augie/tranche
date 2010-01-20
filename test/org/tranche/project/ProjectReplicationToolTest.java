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
package org.tranche.project;

import org.tranche.util.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.tranche.exceptions.TodoException;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ProjectReplicationToolTest extends TrancheTestCase {

    /**
     * <p>Has a negative value for reps.</p>
     * @throws java.lang.Exception
     */
    public void testNegativeReps() throws Exception {
        // need to update to use new connection scheme
        if (true) {
            throw new TodoException();
        }
//
//        // We're testing. Don't want healing, so avoid testing core servers.
//        TestUtil.setTesting(true);
//        TestUtil.setTestUsingFakeCoreServers(false);
//        TestUtil.setTestingHashSpanFixingThread(false);
//
//        File upload = null;
//        File uploadDecoy = null;
//
//        final FlatFileTrancheServer[] fftss = new FlatFileTrancheServer[5];
//        final Server[] ss = new Server[fftss.length];
//        final String[] urls = new String[fftss.length];
//        final TrancheServer[] tss = new TrancheServer[fftss.length];
//
//        File dataDirectoryRoot = null;
//
//         String passphraseDecoy = null;
//
//        // The indices of server's roles
//        final int READ_INDEX = 0,  WRITE_1_INDEX = 1,  WRITE_2_INDEX = 2,  WRITE_3_INDEX = 3,  CONTROL_INDEX = 4;
//
//        try {
//            dataDirectoryRoot = TempFileUtil.createTemporaryDirectory();
//
//            // Create and start the servers
//            for (int i = 0; i < fftss.length; i++) {
//                File dataDir = new File(dataDirectoryRoot, String.valueOf(i));
//                fftss[i] = new FlatFileTrancheServer(dataDir);
//                fftss[i].getConfiguration().addUser(DevUtil.getDevUser());
//
//                ss[i] = new Server(fftss[i], 1500 + i);
//                ss[i].start();
//
//                urls[i] = "tranche://127.0.0.1:" + String.valueOf(1500 + i);
//                tss[i] = IOUtil.connect(urls[i]);
//            }
//
//            // Let these servers be the test servers
//            ServerUtil.setTestServers(urls);
//
//            // Make sure servers are online
//            for (String url : urls) {
//                assertTrue("Servers should be online: " + url, ServerUtil.isServerOnline(url));
//            }
//
//            // Create the upload
//                upload = TempFileUtil.createTemporaryFile(".projectReplicationToolTest");
//                DevUtil.createTestFile(upload, 32 * 1024, 5 * 1024 * 1024);
//
//
////       uploadDecoy = new File("C:\\Users\\Cornbread\\Desktop\\testProjs\\test (2)");
//
//            // Upload to the read server
//            AddFileTool aft = new AddFileTool(DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//            aft.setServersToUploadTo(1);
//            aft.addServerURL(urls[READ_INDEX]);
//            BigHash hash = aft.addFile(upload);
//
//
//
//            assertTrue("Should have at least one meta data.", fftss[READ_INDEX].getDataBlockUtil().metaDataHashes.size() > 0);
//            assertTrue("Should have at least one data.", fftss[READ_INDEX].getDataBlockUtil().dataHashes.size() > 0);
//
//  //          final int totalMetaDataChunks = fftss[READ_INDEX].getDataBlockUtil().getMetaData(hash).length;
////            final int totalDataChunks = fftss[READ_INDEX].getDataBlockUtil().getData(hash).length;
//
//            assertEquals("Should not have at least one meta data.", 0, fftss[WRITE_1_INDEX].getDataBlockUtil().metaDataHashes.size());
//            assertEquals("Should not have at least one data.", 0, fftss[WRITE_1_INDEX].getDataBlockUtil().dataHashes.size());
//
//            assertEquals("Should not have at least one meta data.", 0, fftss[WRITE_2_INDEX].getDataBlockUtil().metaDataHashes.size());
//            assertEquals("Should not have at least one data.", 0, fftss[WRITE_2_INDEX].getDataBlockUtil().dataHashes.size());
//
//            assertEquals("Should not have at least one meta data.", 0, fftss[WRITE_3_INDEX].getDataBlockUtil().metaDataHashes.size());
//            assertEquals("Should not have at least one data.", 0, fftss[WRITE_3_INDEX].getDataBlockUtil().dataHashes.size());
//
//            assertEquals("Should not have at least one meta data.", 0, fftss[CONTROL_INDEX].getDataBlockUtil().metaDataHashes.size());
//            assertEquals("Should not have at least one data.", 0, fftss[CONTROL_INDEX].getDataBlockUtil().dataHashes.size());
//
//            List<String> readServers = new ArrayList();
//            List<String> writeServers = new ArrayList();
//
//            readServers.add(urls[READ_INDEX]);
//
//            writeServers.add(urls[WRITE_1_INDEX]);
//            writeServers.add(urls[WRITE_2_INDEX]);
//            writeServers.add(urls[WRITE_3_INDEX]);
//
//            ProjectReplicationTool repTool = new ProjectReplicationTool(DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), readServers, writeServers);
//            repTool.setNumberRequiredReplications(-1);
//            repTool.setHash(hash);
//
//            ProjectReplicationToolListener l = new CommandLineProjectReplicationToolListener();
//            repTool.addProjectReplicationToolListener(l);
//
//            repTool.execute();
//
////            assertEquals("Should have at least one meta data.", totalMetaDataChunks, fftss[READ_INDEX].getDataBlockUtil().metaDataHashes.size());
////            assertEquals("Should have at least one data.", totalDataChunks, fftss[READ_INDEX].getDataBlockUtil().dataHashes.size());
//
//             Set<BigHash> relevantMetaChunks = new HashSet();
//             Set<BigHash> relevantDataChunks = new HashSet();
//
//             final String[] readUrls = new String[1];
//
//             readUrls[0] = urls[READ_INDEX];
//
//
//               relevantMetaChunks = getAllChunksForSingleFile(hash, readUrls, passphraseDecoy, true);
//               relevantDataChunks = getAllChunksForSingleFile(hash, readUrls, passphraseDecoy, false);
//
//
//
//             for(BigHash uploadHash : relevantMetaChunks) {
//                 assertTrue("Should have meta data", fftss[READ_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//                 assertFalse("Should have meta data.", fftss[WRITE_1_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//                 assertFalse("Should have meta data.", fftss[WRITE_2_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//                 assertFalse("Should have meta data.", fftss[WRITE_3_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//                 assertFalse("Should not have meta data.", fftss[CONTROL_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//
//                }
//             for(BigHash uploadHash : relevantDataChunks) {
//                 assertTrue("Should have data", fftss[READ_INDEX].getDataBlockUtil().hasData(uploadHash));
//                 assertFalse("Should have data.", fftss[WRITE_1_INDEX].getDataBlockUtil().hasData(uploadHash));
//                 assertFalse("Should have data.", fftss[WRITE_2_INDEX].getDataBlockUtil().hasData(uploadHash));
//                 assertFalse("Should have data.", fftss[WRITE_3_INDEX].getDataBlockUtil().hasData(uploadHash));
//                 assertFalse("Should not have data.", fftss[CONTROL_INDEX].getDataBlockUtil().hasData(uploadHash));
//            }
//
//
//     //       assertEquals("Should have at least one meta data.", totalMetaDataChunks, fftss[READ_INDEX].getDataBlockUtil().metaDataHashes.size());
//     //       assertEquals("Should have at least one data.", totalDataChunks, fftss[READ_INDEX].getDataBlockUtil().dataHashes.size());
//
//
//
//        } finally {
//            IOUtil.recursiveDelete(upload);
//            IOUtil.recursiveDelete(uploadDecoy);
//
//            for (TrancheServer ts : tss) {
//                IOUtil.safeClose(ts);
//            }
//
//            for (Server s : ss) {
//                IOUtil.safeClose(s);
//            }
//
//            for (FlatFileTrancheServer ffts : fftss) {
//                IOUtil.safeClose(ffts);
//            }
//
//            IOUtil.recursiveDelete(dataDirectoryRoot);
//        }
//
//        String[] noUrls = {};
//        ServerUtil.setTestServers(noUrls);
    }

    
    
    /**
     * <p>Uses helper method to test the replication of a project.</p>
     * @throws java.lang.Exception
     */
    public void testSimpleReplicatesProject() throws Exception {
        testSimpleReplicates(false);
    }

    /**
     * <p>Uses helper method to test the replication of a single file.</p>
     * @throws java.lang.Exception
     */
    public void testSimpleReplicatesSingleFile() throws Exception {
        testSimpleReplicates(true);
    }
    
    
    public void testSkipAlreadyReplicatedChunks(boolean isSingleFileTest) throws Exception {
        // need to update to use new connection scheme
        if (true) {
            throw new TodoException();
        }
//
//        // We're testing. Don't want healing, so avoid testing core servers.
//        TestUtil.setTesting(true);
//        TestUtil.setTestUsingFakeCoreServers(false);
//        TestUtil.setTestingHashSpanFixingThread(false);
//
//        File upload = null;
//        File uploadDecoy = null;
//
//        final FlatFileTrancheServer[] fftss = new FlatFileTrancheServer[5];
//        final Server[] ss = new Server[fftss.length];
//        final String[] urls = new String[fftss.length];
//        final TrancheServer[] tss = new TrancheServer[fftss.length];
//
//        File dataDirectoryRoot = null;
//
//         String passphraseDecoy = null;
//
//        // The indices of server's roles
//        final int READ_INDEX = 0,  WRITE_1_INDEX = 1,  WRITE_2_INDEX = 2,  WRITE_3_INDEX = 3,  CONTROL_INDEX = 4;
//
//        try {
//            dataDirectoryRoot = TempFileUtil.createTemporaryDirectory();
//
//            // Create and start the servers
//            for (int i = 0; i < fftss.length; i++) {
//                File dataDir = new File(dataDirectoryRoot, String.valueOf(i));
//                fftss[i] = new FlatFileTrancheServer(dataDir);
//                fftss[i].getConfiguration().addUser(DevUtil.getDevUser());
//
//                ss[i] = new Server(fftss[i], 1500 + i);
//                ss[i].start();
//
//                urls[i] = "tranche://127.0.0.1:" + String.valueOf(1500 + i);
//                tss[i] = IOUtil.connect(urls[i]);
//            }
//
//            // Let these servers be the test servers
//            ServerUtil.setTestServers(urls);
//
//            // Make sure servers are online
//            for (String url : urls) {
//                assertTrue("Servers should be online: " + url, ServerUtil.isServerOnline(url));
//            }
//
//            // Create the upload
//            if (isSingleFileTest) {
//                upload = TempFileUtil.createTemporaryFile(".projectReplicationToolTest");
//                DevUtil.createTestFile(upload, 32 * 1024, 5 * 1024 * 1024);
//                uploadDecoy = TempFileUtil.createTemporaryFile(".projectReplicationToolDecoyTest");
//                DevUtil.createTestFile(uploadDecoy, 32 * 1024, 5 * 1024 * 1024);
//
//            } else {
//                upload = DevUtil.createTestProject(3, 32 * 1024, 2 * 1024 * 1024);
//                Text.printRecursiveDirectoryStructure(upload);
//                uploadDecoy = DevUtil.createTestProject(3, 32 * 1024, 2 * 1024 * 1024);
//                Text.printRecursiveDirectoryStructure(uploadDecoy);
//      }
//
////       uploadDecoy = new File("C:\\Users\\Cornbread\\Desktop\\testProjs\\test (2)");
//
//            // Upload to the read server
//            AddFileTool aft = new AddFileTool(DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//            aft.setServersToUploadTo(1);
//            aft.addServerURL(urls[READ_INDEX]);
//            BigHash hash = aft.addFile(upload);
//            aft = new AddFileTool(DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//            aft.setServersToUploadTo(1);
//            aft.addServerURL(urls[READ_INDEX]);
//            BigHash hashDecoy = aft.addFile(uploadDecoy);
//
//
//            assertTrue("Should have at least one meta data.", fftss[READ_INDEX].getDataBlockUtil().metaDataHashes.size() > 0);
//            assertTrue("Should have at least one data.", fftss[READ_INDEX].getDataBlockUtil().dataHashes.size() > 0);
//
//  //          final int totalMetaDataChunks = fftss[READ_INDEX].getDataBlockUtil().getMetaData(hash).length;
////            final int totalDataChunks = fftss[READ_INDEX].getDataBlockUtil().getData(hash).length;
//
//            assertEquals("Should not have at least one meta data.", 0, fftss[WRITE_1_INDEX].getDataBlockUtil().metaDataHashes.size());
//            assertEquals("Should not have at least one data.", 0, fftss[WRITE_1_INDEX].getDataBlockUtil().dataHashes.size());
//
//            assertEquals("Should not have at least one meta data.", 0, fftss[WRITE_2_INDEX].getDataBlockUtil().metaDataHashes.size());
//            assertEquals("Should not have at least one data.", 0, fftss[WRITE_2_INDEX].getDataBlockUtil().dataHashes.size());
//
//            assertEquals("Should not have at least one meta data.", 0, fftss[WRITE_3_INDEX].getDataBlockUtil().metaDataHashes.size());
//            assertEquals("Should not have at least one data.", 0, fftss[WRITE_3_INDEX].getDataBlockUtil().dataHashes.size());
//
//            assertEquals("Should not have at least one meta data.", 0, fftss[CONTROL_INDEX].getDataBlockUtil().metaDataHashes.size());
//            assertEquals("Should not have at least one data.", 0, fftss[CONTROL_INDEX].getDataBlockUtil().dataHashes.size());
//
//            List<String> readServers = new ArrayList();
//            List<String> writeServers = new ArrayList();
//
//            readServers.add(urls[READ_INDEX]);
//
//            writeServers.add(urls[WRITE_1_INDEX]);
//            writeServers.add(urls[WRITE_2_INDEX]);
//            writeServers.add(urls[WRITE_3_INDEX]);
//
//            ProjectReplicationTool repTool = new ProjectReplicationTool(DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), readServers, writeServers);
//            repTool.setNumberRequiredReplications(3);
//            repTool.setHash(hash);
//
//            ProjectReplicationToolListener l = new CommandLineProjectReplicationToolListener();
//            repTool.addProjectReplicationToolListener(l);
//
//            repTool.execute();
//            repTool.execute();
//
//
////            assertEquals("Should have at least one meta data.", totalMetaDataChunks, fftss[READ_INDEX].getDataBlockUtil().metaDataHashes.size());
////            assertEquals("Should have at least one data.", totalDataChunks, fftss[READ_INDEX].getDataBlockUtil().dataHashes.size());
//
//             Set<BigHash> relevantMetaChunks = new HashSet();
//             Set<BigHash> relevantDataChunks = new HashSet();
//
//             final String[] readUrls = new String[1];
//
//             readUrls[0] = urls[READ_INDEX];
//
//             if(isSingleFileTest) {
//               relevantMetaChunks = getAllChunksForSingleFile(hashDecoy, readUrls, passphraseDecoy, true);
//               relevantDataChunks = getAllChunksForSingleFile(hashDecoy, readUrls, passphraseDecoy, false);
//
//             }else {
//               relevantMetaChunks = getAllChunksForProject(hashDecoy, readUrls, passphraseDecoy, true);
//               relevantDataChunks = getAllChunksForProject(hashDecoy, readUrls, passphraseDecoy, false);
//
//             }
//
//             for(BigHash decoyHash : relevantMetaChunks) {
//                    assertTrue("Should have meta data", fftss[READ_INDEX].getDataBlockUtil().hasMetaData(decoyHash));
//                    assertFalse("Should not have meta data for Decoy.", fftss[WRITE_1_INDEX].getDataBlockUtil().hasMetaData(decoyHash));
//                    assertFalse("Should not have meta data for Decoy.", fftss[WRITE_2_INDEX].getDataBlockUtil().hasMetaData(decoyHash));
//                    assertFalse("Should not have meta data for Decoy.", fftss[WRITE_3_INDEX].getDataBlockUtil().hasMetaData(decoyHash));
//                }
//             for(BigHash decoyHash : relevantDataChunks) {
//                    assertTrue("Should have data", fftss[READ_INDEX].getDataBlockUtil().hasData(decoyHash));
//                    assertFalse("Should not have data for Decoy.", fftss[WRITE_1_INDEX].getDataBlockUtil().hasData(decoyHash));
//                    assertFalse("Should not have data for Decoy.", fftss[WRITE_2_INDEX].getDataBlockUtil().hasData(decoyHash));
//                    assertFalse("Should not have data for Decoy.", fftss[WRITE_3_INDEX].getDataBlockUtil().hasData(decoyHash));
//                }
//
//
//             if(isSingleFileTest) {
//               relevantMetaChunks = getAllChunksForSingleFile(hash, readUrls, passphraseDecoy, true);
//               relevantDataChunks = getAllChunksForSingleFile(hash, readUrls, passphraseDecoy, false);
//
//             }else {
//               relevantMetaChunks = getAllChunksForProject(hash, readUrls, passphraseDecoy, true);
//               relevantDataChunks = getAllChunksForProject(hash, readUrls, passphraseDecoy, false);
//
//             }
//
//             for(BigHash uploadHash : relevantMetaChunks) {
//                 assertTrue("Should have meta data", fftss[READ_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//                 assertTrue("Should have meta data.", fftss[WRITE_1_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//                 assertTrue("Should have meta data.", fftss[WRITE_2_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//                 assertTrue("Should have meta data.", fftss[WRITE_3_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//                 assertFalse("Should not have meta data.", fftss[CONTROL_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//
//                }
//             for(BigHash uploadHash : relevantDataChunks) {
//                 assertTrue("Should have data", fftss[READ_INDEX].getDataBlockUtil().hasData(uploadHash));
//                 assertTrue("Should have data.", fftss[WRITE_1_INDEX].getDataBlockUtil().hasData(uploadHash));
//                 assertTrue("Should have data.", fftss[WRITE_2_INDEX].getDataBlockUtil().hasData(uploadHash));
//                 assertTrue("Should have data.", fftss[WRITE_3_INDEX].getDataBlockUtil().hasData(uploadHash));
//                 assertFalse("Should not have data.", fftss[CONTROL_INDEX].getDataBlockUtil().hasData(uploadHash));
//            }
//
//
//     //       assertEquals("Should have at least one meta data.", totalMetaDataChunks, fftss[READ_INDEX].getDataBlockUtil().metaDataHashes.size());
//     //       assertEquals("Should have at least one data.", totalDataChunks, fftss[READ_INDEX].getDataBlockUtil().dataHashes.size());
//
//
//
//        } finally {
//            IOUtil.recursiveDelete(upload);
//            IOUtil.recursiveDelete(uploadDecoy);
//
//            for (TrancheServer ts : tss) {
//                IOUtil.safeClose(ts);
//            }
//
//            for (Server s : ss) {
//                IOUtil.safeClose(s);
//            }
//
//            for (FlatFileTrancheServer ffts : fftss) {
//                IOUtil.safeClose(ffts);
//            }
//
//            IOUtil.recursiveDelete(dataDirectoryRoot);
//        }
//
//        String[] noUrls = {};
//        ServerUtil.setTestServers(noUrls);
    }

    
    
    /**
     * <p>Simple test ensures that replicator works and that it honors the list of servers to read from and write to.</p>
     * <ol>
     *   <li>Start five servers: one to upload directly to and read from, three to inject to, and one as a control</li>
     *   <li>Upload project to the read server</li>
     *   <li>Run replicator with three desired reps to the three write servers</li>
     *   <li>Verify that the three servers have a copy of every data and meta data chunk, and that forth has none.
     * </ol>
     */
    
    private void testSimpleReplicates(boolean isSingleFileTest) throws Exception {
        // need to update to use new connection scheme
        if (true) {
            throw new TodoException();
        }
//
//        // We're testing. Don't want healing, so avoid testing core servers.
//        TestUtil.setTesting(true);
//        TestUtil.setTestUsingFakeCoreServers(false);
//        TestUtil.setTestingHashSpanFixingThread(false);
//
//        File upload = null;
//        File uploadDecoy = null;
//
//        final FlatFileTrancheServer[] fftss = new FlatFileTrancheServer[5];
//        final Server[] ss = new Server[fftss.length];
//        final String[] urls = new String[fftss.length];
//        final TrancheServer[] tss = new TrancheServer[fftss.length];
//
//        File dataDirectoryRoot = null;
//
//         String passphraseDecoy = null;
//
//        // The indices of server's roles
//        final int READ_INDEX = 0,  WRITE_1_INDEX = 1,  WRITE_2_INDEX = 2,  WRITE_3_INDEX = 3,  CONTROL_INDEX = 4;
//
//        try {
//            dataDirectoryRoot = TempFileUtil.createTemporaryDirectory();
//
//            // Create and start the servers
//            for (int i = 0; i < fftss.length; i++) {
//                File dataDir = new File(dataDirectoryRoot, String.valueOf(i));
//                fftss[i] = new FlatFileTrancheServer(dataDir);
//                fftss[i].getConfiguration().addUser(DevUtil.getDevUser());
//
//                ss[i] = new Server(fftss[i], 1500 + i);
//                ss[i].start();
//
//                urls[i] = "tranche://127.0.0.1:" + String.valueOf(1500 + i);
//                tss[i] = IOUtil.connect(urls[i]);
//            }
//
//            // Let these servers be the test servers
//            ServerUtil.setTestServers(urls);
//
//            // Make sure servers are online
//            for (String url : urls) {
//                assertTrue("Servers should be online: " + url, ServerUtil.isServerOnline(url));
//            }
//
//            // Create the upload
//            if (isSingleFileTest) {
//                upload = TempFileUtil.createTemporaryFile(".projectReplicationToolTest");
//                DevUtil.createTestFile(upload, 32 * 1024, 5 * 1024 * 1024);
//                uploadDecoy = TempFileUtil.createTemporaryFile(".projectReplicationToolDecoyTest");
//                DevUtil.createTestFile(uploadDecoy, 32 * 1024, 5 * 1024 * 1024);
//
//            } else {
//                upload = DevUtil.createTestProject(3, 32 * 1024, 2 * 1024 * 1024);
//                Text.printRecursiveDirectoryStructure(upload);
//                uploadDecoy = DevUtil.createTestProject(3, 32 * 1024, 2 * 1024 * 1024);
//                Text.printRecursiveDirectoryStructure(uploadDecoy);
//      }
//
////       uploadDecoy = new File("C:\\Users\\Cornbread\\Desktop\\testProjs\\test (2)");
//
//            // Upload to the read server
//            AddFileTool aft = new AddFileTool(DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//            aft.setServersToUploadTo(1);
//            aft.addServerURL(urls[READ_INDEX]);
//            BigHash hash = aft.addFile(upload);
//            aft = new AddFileTool(DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//            aft.setServersToUploadTo(1);
//            aft.addServerURL(urls[READ_INDEX]);
//            BigHash hashDecoy = aft.addFile(uploadDecoy);
//
//
//            assertTrue("Should have at least one meta data.", fftss[READ_INDEX].getDataBlockUtil().metaDataHashes.size() > 0);
//            assertTrue("Should have at least one data.", fftss[READ_INDEX].getDataBlockUtil().dataHashes.size() > 0);
//
//  //          final int totalMetaDataChunks = fftss[READ_INDEX].getDataBlockUtil().getMetaData(hash).length;
////            final int totalDataChunks = fftss[READ_INDEX].getDataBlockUtil().getData(hash).length;
//
//            assertEquals("Should not have at least one meta data.", 0, fftss[WRITE_1_INDEX].getDataBlockUtil().metaDataHashes.size());
//            assertEquals("Should not have at least one data.", 0, fftss[WRITE_1_INDEX].getDataBlockUtil().dataHashes.size());
//
//            assertEquals("Should not have at least one meta data.", 0, fftss[WRITE_2_INDEX].getDataBlockUtil().metaDataHashes.size());
//            assertEquals("Should not have at least one data.", 0, fftss[WRITE_2_INDEX].getDataBlockUtil().dataHashes.size());
//
//            assertEquals("Should not have at least one meta data.", 0, fftss[WRITE_3_INDEX].getDataBlockUtil().metaDataHashes.size());
//            assertEquals("Should not have at least one data.", 0, fftss[WRITE_3_INDEX].getDataBlockUtil().dataHashes.size());
//
//            assertEquals("Should not have at least one meta data.", 0, fftss[CONTROL_INDEX].getDataBlockUtil().metaDataHashes.size());
//            assertEquals("Should not have at least one data.", 0, fftss[CONTROL_INDEX].getDataBlockUtil().dataHashes.size());
//
//            List<String> readServers = new ArrayList();
//            List<String> writeServers = new ArrayList();
//
//            readServers.add(urls[READ_INDEX]);
//
//            writeServers.add(urls[WRITE_1_INDEX]);
//            writeServers.add(urls[WRITE_2_INDEX]);
//            writeServers.add(urls[WRITE_3_INDEX]);
//
//            ProjectReplicationTool repTool = new ProjectReplicationTool(DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), readServers, writeServers);
//            repTool.setNumberRequiredReplications(3);
//            repTool.setHash(hash);
//
//            ProjectReplicationToolListener l = new CommandLineProjectReplicationToolListener();
//            repTool.addProjectReplicationToolListener(l);
//
//            repTool.execute();
//
//
//
////            assertEquals("Should have at least one meta data.", totalMetaDataChunks, fftss[READ_INDEX].getDataBlockUtil().metaDataHashes.size());
////            assertEquals("Should have at least one data.", totalDataChunks, fftss[READ_INDEX].getDataBlockUtil().dataHashes.size());
//
//             Set<BigHash> relevantMetaChunks = new HashSet();
//             Set<BigHash> relevantDataChunks = new HashSet();
//
//             final String[] readUrls = new String[1];
//
//             readUrls[0] = urls[READ_INDEX];
//
//             if(isSingleFileTest) {
//               relevantMetaChunks = getAllChunksForSingleFile(hashDecoy, readUrls, passphraseDecoy, true);
//               relevantDataChunks = getAllChunksForSingleFile(hashDecoy, readUrls, passphraseDecoy, false);
//
//             }else {
//               relevantMetaChunks = getAllChunksForProject(hashDecoy, readUrls, passphraseDecoy, true);
//               relevantDataChunks = getAllChunksForProject(hashDecoy, readUrls, passphraseDecoy, false);
//
//             }
//
//             for(BigHash decoyHash : relevantMetaChunks) {
//                    assertTrue("Should have meta data", fftss[READ_INDEX].getDataBlockUtil().hasMetaData(decoyHash));
//                    assertFalse("Should not have meta data for Decoy.", fftss[WRITE_1_INDEX].getDataBlockUtil().hasMetaData(decoyHash));
//                    assertFalse("Should not have meta data for Decoy.", fftss[WRITE_2_INDEX].getDataBlockUtil().hasMetaData(decoyHash));
//                    assertFalse("Should not have meta data for Decoy.", fftss[WRITE_3_INDEX].getDataBlockUtil().hasMetaData(decoyHash));
//                }
//             for(BigHash decoyHash : relevantDataChunks) {
//                    assertTrue("Should have data", fftss[READ_INDEX].getDataBlockUtil().hasData(decoyHash));
//                    assertFalse("Should not have data for Decoy.", fftss[WRITE_1_INDEX].getDataBlockUtil().hasData(decoyHash));
//                    assertFalse("Should not have data for Decoy.", fftss[WRITE_2_INDEX].getDataBlockUtil().hasData(decoyHash));
//                    assertFalse("Should not have data for Decoy.", fftss[WRITE_3_INDEX].getDataBlockUtil().hasData(decoyHash));
//                }
//
//
//             if(isSingleFileTest) {
//               relevantMetaChunks = getAllChunksForSingleFile(hash, readUrls, passphraseDecoy, true);
//               relevantDataChunks = getAllChunksForSingleFile(hash, readUrls, passphraseDecoy, false);
//
//             }else {
//               relevantMetaChunks = getAllChunksForProject(hash, readUrls, passphraseDecoy, true);
//               relevantDataChunks = getAllChunksForProject(hash, readUrls, passphraseDecoy, false);
//
//             }
//
//             for(BigHash uploadHash : relevantMetaChunks) {
//                 assertTrue("Should have meta data", fftss[READ_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//                 assertTrue("Should have meta data.", fftss[WRITE_1_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//                 assertTrue("Should have meta data.", fftss[WRITE_2_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//                 assertTrue("Should have meta data.", fftss[WRITE_3_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//                 assertFalse("Should not have meta data.", fftss[CONTROL_INDEX].getDataBlockUtil().hasMetaData(uploadHash));
//
//                }
//             for(BigHash uploadHash : relevantDataChunks) {
//                 assertTrue("Should have data", fftss[READ_INDEX].getDataBlockUtil().hasData(uploadHash));
//                 assertTrue("Should have data.", fftss[WRITE_1_INDEX].getDataBlockUtil().hasData(uploadHash));
//                 assertTrue("Should have data.", fftss[WRITE_2_INDEX].getDataBlockUtil().hasData(uploadHash));
//                 assertTrue("Should have data.", fftss[WRITE_3_INDEX].getDataBlockUtil().hasData(uploadHash));
//                 assertFalse("Should not have data.", fftss[CONTROL_INDEX].getDataBlockUtil().hasData(uploadHash));
//            }
//
//
//     //       assertEquals("Should have at least one meta data.", totalMetaDataChunks, fftss[READ_INDEX].getDataBlockUtil().metaDataHashes.size());
//     //       assertEquals("Should have at least one data.", totalDataChunks, fftss[READ_INDEX].getDataBlockUtil().dataHashes.size());
//
//
//
//        } finally {
//            IOUtil.recursiveDelete(upload);
//            IOUtil.recursiveDelete(uploadDecoy);
//
//            for (TrancheServer ts : tss) {
//                IOUtil.safeClose(ts);
//            }
//
//            for (Server s : ss) {
//                IOUtil.safeClose(s);
//            }
//
//            for (FlatFileTrancheServer ffts : fftss) {
//                IOUtil.safeClose(ffts);
//            }
//
//            IOUtil.recursiveDelete(dataDirectoryRoot);
//        }
//
//        String[] noUrls = {};
//        ServerUtil.setTestServers(noUrls);
    }

    private Set<BigHash> getAllMetaDataChunksForProject(BigHash hash, String[] urls, String passphrase) throws Exception {
        return getAllChunksForProject(hash, urls, passphrase, true);
    }

    private Set<BigHash> getAllDataChunksForProject(BigHash hash, String[] urls, String passphrase) throws Exception {
        return getAllChunksForProject(hash, urls, passphrase, false);
    }

    private Set<BigHash> getAllChunksForProject(BigHash hash, String[] urls, String passphrase, boolean isMetaData) throws Exception {
        // need to update to use new connection scheme
        if (true) {
            throw new TodoException();
        }
        return null;
//        // Will contain either meta data or data chunk hashes, depending on value of isMetaData
//        Set<BigHash> relevantChunks = new HashSet();
//
//        GetFileTool gft = null;
//
//        List<String> servers = new ArrayList();
//        for (String url : urls) {
//            servers.add(url);
//        }
//
//        if (isMetaData) {
//            // Do we want the project's meta data for project file?...
//            relevantChunks.add(hash);
//        } else {
//
//            // Need MetaData for project file (one meta data chunk)
//            gft = new GetFileTool();
//            gft.setServersToUse(servers);
//            gft.setHash(hash);
//            if (passphrase != null && !passphrase.trim().equals("")) {
//                gft.setPassphrase(passphrase);
//            }
//
//            MetaData md = gft.getMetaData();
//
//            // ... or do we want the data chunks for the project file!?!
//            relevantChunks.addAll(md.getParts());
//        }
//
//        // Need ProjectFile (one or more data chunks)
//        File projectFile = null;
//
//        try {
//
//            projectFile = TempFileUtil.createTemporaryFile(".pf");
//
//            gft = new GetFileTool();
//            gft.setServersToUse(servers);
//            gft.setHash(hash);
//            if (passphrase != null && !passphrase.trim().equals("")) {
//                gft.setPassphrase(passphrase);
//            }
//
//            gft.getFile(projectFile);
//
//            if (!projectFile.exists()) {
//                throw new Exception("Project file was not downloaded.");
//            }
//
//            ProjectFile pf = null;
//
//            BufferedInputStream bis = null;
//            try {
//                bis = new BufferedInputStream(new FileInputStream(projectFile));
//                pf = ProjectFileUtil.read(bis);
//            } finally {
//                if (bis != null) {
//                    IOUtil.safeClose(bis);
//                }
//            }
//
//            if (pf == null) {
//                throw new Exception("Failed to parse project file.");
//            }
//
//            // Need to iterate ProjectFileParts to get (i) one meta data chunks and (ii) one or more data chunks
//            for (ProjectFilePart pfp : pf.getParts()) {
//                if (isMetaData) {
//                    // If meta data desired, just grab hashes for project files' meta data chunks
//                    relevantChunks.add(pfp.getHash());
//                } else {
//                    // If data desired, first grab meta data objects, and copy data chunks from there.
//                    gft = new GetFileTool();
//                    gft.setServersToUse(servers);
//                    gft.setHash(pfp.getHash());
//                    if (passphrase != null && !passphrase.trim().equals("")) {
//                        gft.setPassphrase(passphrase);
//                    }
//
//                    MetaData individualFileMetaData = gft.getMetaData();
//
//                    // This will be the one or more data chunks for the file
//                    relevantChunks.addAll(individualFileMetaData.getParts());
//                }
//            }
//
//        } finally {
//            IOUtil.safeDelete(projectFile);
//        }
//
//        return relevantChunks;
    }
    
    private Set<BigHash> getAllChunksForSingleFile(BigHash hash, String[] urls, String passphrase, boolean isMetaData) throws Exception {
             // Will contain either meta data or data chunk hashes, depending on value of isMetaData
        Set<BigHash> relevantChunks = new HashSet();

        GetFileTool gft = null;

        List<String> servers = new ArrayList();
        for (String url : urls) {
            servers.add(url);
        }

        if (isMetaData) {
            // Do we want the project's meta data for project file?...
            relevantChunks.add(hash);
        } else {

            // Need MetaData for project file (one meta data chunk)
            gft = new GetFileTool();
            gft.setServersToUse(servers);
            gft.setHash(hash);
            if (passphrase != null && !passphrase.trim().equals("")) {
                gft.setPassphrase(passphrase);
            }

            MetaData md = gft.getMetaData();

            // ... or do we want the data chunks for the project file!?!
            relevantChunks.addAll(md.getParts());
        }

        return relevantChunks;
    }

    private Set<BigHash> getAllMetaDataChunksForSingleFile(BigHash hash, String[] urls, String passphrase) throws Exception {
        return getAllChunksForSingleFile(hash, urls, passphrase, true);
    }

    private Set<BigHash> getAllDataChunksForSingleFile(BigHash hash, String[] urls, String passphrase) throws Exception {
        return getAllChunksForSingleFile(hash, urls, passphrase, false);
    }
    
    
    
}
 