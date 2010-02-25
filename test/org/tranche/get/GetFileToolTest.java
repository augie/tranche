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
package org.tranche.get;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tranche.ConfigureTranche;
import org.tranche.FileEncoding;
import org.tranche.TrancheServer;
import org.tranche.add.AddFileTool;
import org.tranche.add.AddFileToolReport;
import org.tranche.add.CommandLineAddFileToolListener;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.util.DevUtil;
import org.tranche.hash.span.HashSpan;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.meta.MetaDataUtil;
import org.tranche.network.ConnectionUtil;
import org.tranche.project.ProjectFile;
import org.tranche.project.ProjectFileUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.security.Signature;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.users.UserCertificateUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;
import org.tranche.util.ThreadUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetFileToolTest extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        AddFileTool.setDebug(true);
        GetFileTool.setDebug(true);
        ConfigureTranche.set(ConfigureTranche.PROP_REPLICATIONS, "1");
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        AddFileTool.setDebug(false);
        GetFileTool.setDebug(false);
        ConfigureTranche.set(ConfigureTranche.PROP_REPLICATIONS, ConfigureTranche.DEFAULT_REPLICATIONS);
    }

    /**
     * There are two servers with a full hash span.
     * A file is uploaded only to the first server and downloaded only from the second.
     * The second should download the data from the first server and send it to the client.
     * @throws java.lang.Exception
     */
    public void testGetFileThroughServer() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testGetFileThroughServer()");

        String HOST0 = "server1.com";
        String HOST1 = "server2.com";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST0, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));

        //create temp file
        File upload = TempFileUtil.createTempFileWithName("name.file");
        DevUtil.createTestFile(upload, 125);
        try {
            testNetwork.start();

            //Upload test file
            AddFileToolReport upFile = uploadTest(upload, HOST0, null);
            BigHash _hash = upFile.getHash();

            //download test file
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(_hash);
            gft.setSaveFile(upload);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            GetFileToolReport downFile = gft.getFile();

            assertFalse(downFile.isFailed());
            assertNotNull(downFile);
            assertEquals(upFile.getBytesUploaded(), downFile.getBytesDownloaded());

        } finally {
            testNetwork.stop();
        }
    }

    /**
     * Tests the GetFileTool.getConnections method behaves properly.
     * @see GetFileTool#getConnections(org.tranche.hash.BigHash)
     * @throws java.lang.Exception
     */
    public void testGetConnections() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testGetConnections()");
        //TODO: can specify readable or writeable connections in server config test???
        //TODO: should test different spans???

        int tot = 10; //total number of possible test servers
        int rs = RandomUtil.getInt(tot); //actual number of servers to allocate

        //create host names
        String[] hosts = new String[rs];
        for (int i = 0; i < rs; i++) {
            hosts[i] = "server" + i + ".com";
        }

        TestNetwork testNetwork = new TestNetwork();

        //add configurations for hosts
        for (int i = 0; i < rs; i++) {
            int n = 1500 + i;
            testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, hosts[i], n, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        }

        //create temp file
        File upload = TempFileUtil.createTempFileWithName("name.file");
        DevUtil.createTestFile(upload, 125);


        try {
            testNetwork.start();

            //Upload test file
            int upHost = RandomUtil.getInt(tot);
            AddFileToolReport upFile = uploadTest(upload, hosts[upHost], null);
            BigHash _hash = upFile.getHash();

            //download test file
            GetFileTool gft = new GetFileTool();
            Collection h = Arrays.asList(hosts);
            gft.addServersToUse(h);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(_hash);
            gft.setSaveFile(upload);
            Collection connections = gft.getConnections(_hash);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            GetFileToolReport downFile = gft.getFile();

            assertFalse(downFile.isFailed());
            assertNotNull(downFile);
            //TODO: is this the correct test or should I be testing
            //something else too ???
            assertEquals(connections.size(), rs);
        } finally {
            testNetwork.stop();
        }

    }

    /**
     * Tests that two files with the same contents in a data set can be downloaded as expected.
     * @throws java.lang.Exception
     */
    public void testMultipleSameFileInSameDataSet() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testMultipleSameFileInSameDataSet()");

        int tot = 20; //total # of files

        //create directory with duplicate files
        File directory = TempFileUtil.createTemporaryDirectory();
        File nextFile, duplicateFile;
        OutputStream out = null, dup = null;
        for (int i = 0, j = i + 1; i < tot; i += 2, j += 2) {
            nextFile = new File(directory, i + ".tmp");
            duplicateFile = new File(directory, j + ".tmp");
            try {
                out = new FileOutputStream(nextFile);
                dup = new FileOutputStream(duplicateFile);
                // Want files that are at least 16K
                int size = RandomUtil.getInt((4 * 1024 * 1024) - (1024 * 16) + 1) + (1024 * 16);
                byte[] bytes = new byte[size];

                RandomUtil.getBytes(bytes);
                out.write(bytes);
                dup.write(bytes);

            } finally {
                IOUtil.safeClose(out);
                IOUtil.safeClose(dup);
            }
        }

        //Test Network
        String HOST0 = "server0.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST0, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));


        try {
            testNetwork.start();

            //Upload Test Directory
            AddFileToolReport upDir = uploadTest(directory, HOST0, null);
            BigHash _hash = upDir.getHash();

            //download test file
            File downDir = TempFileUtil.createTemporaryDirectory();

            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST0);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(_hash);
            gft.setSaveFile(downDir);
            gft.setRegEx("0.tmp$");
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            GetFileToolReport downFile = gft.getDirectory();

            assertFalse(downFile.isFailed());
            assertNotNull(downFile);

            // validate
            System.out.println("Printing uploaded directory structure.");
            Text.printRecursiveDirectoryStructure(directory);
            System.out.println("");
            System.out.println("Printing downloaded directory structure.");
            Text.printRecursiveDirectoryStructure(downDir);
            assertEquals(2, new File(downDir, directory.getName()).listFiles().length);


        } finally {
            testNetwork.stop();
        }
    }

    /**
     * Tests that when all servers fail occasionally, a file can still be downloaded.
     * @throws java.lang.Exception
     */
    public void testAllServersFailingIntermittently() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testAllServersFailingIntermittently()");

        //create host names
        String[] hosts = new String[5];
        for (int i = 0; i < 5; i++) {
            hosts[i] = "server" + i + ".com";
        }

        TestNetwork testNetwork = new TestNetwork();

        //add configurations for hosts
        double failureRate = .2;
        for (int i = 0; i < 5; i++) {
            int n = 1500 + i;
            testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForFailingDataServer(443, hosts[i], n, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET, failureRate));
            failureRate = failureRate + .2;
        }

        //create temp file
        File upload = TempFileUtil.createTempFileWithName("name.file");
        DevUtil.createTestFile(upload, 125);


        try {
            testNetwork.start();

            //Upload test file
            AddFileToolReport upFile = uploadTest(upload, hosts[0], null);
            BigHash _hash = upFile.getHash();

            //download test file
            GetFileTool gft = new GetFileTool();
            Collection h = Arrays.asList(hosts);
            gft.addServersToUse(h);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(_hash);
            gft.setSaveFile(upload);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            GetFileToolReport downFile = gft.getFile();

            assertFalse(downFile.isFailed());
            assertNotNull(downFile);
            assertEquals(upFile.getBytesUploaded(), downFile.getBytesDownloaded());

        } finally {
            testNetwork.stop();
        }
    }

    /**
     * Test that when one server fails occasionally, a file can still be downloaded.
     * @throws java.lang.Exception
     */
    public void testOneServerFailingIntermittently() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testOneServerFailingIntermittently()");

        //create host names
        String HOST0 = "server0.com";

        TestNetwork testNetwork = new TestNetwork();

        //add configurations for host
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForFailingDataServer(443, HOST0, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET, .1));

        //create temp file
        File upload = TempFileUtil.createTempFileWithName("name.file");
        DevUtil.createTestFile(upload, 125);


        try {
            testNetwork.start();

            //Upload test file
            AddFileToolReport upFile = uploadTest(upload, HOST0, null);
            BigHash _hash = upFile.getHash();

            //download test file
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST0);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(_hash);
            gft.setSaveFile(upload);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            GetFileToolReport downFile = gft.getFile();

            assertFalse("Download Failed.", downFile.isFailed());
            assertNotNull("Download File Empty.", downFile);
            assertEquals(upFile.getBytesUploaded(), downFile.getBytesDownloaded());

        } finally {
            testNetwork.stop();
        }
    }

    /**
     * Works the same as AddFileToolTest.testStop
     * Stop the execution of the download in the middle and make sure it does not finish.
     * @throws java.lang.Exception
     */
    public void testStop() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testStop()");
        final String HOST0 = "aardvark.org";
        final String HOST1 = "bryan.com";
        final String HOST2 = "cherry.gov";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST0, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

        //create temp file
        File upload = TempFileUtil.createTempFileWithName("name.file");
        DevUtil.createTestFile(upload, 100 * DataBlockUtil.ONE_MB);

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST0, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts3);

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST0);
            assertNotNull(ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull(ffts3);

            AddFileToolReport upFile = uploadTest(upload, HOST0, null);
            BigHash _hash = upFile.getHash();

            //download test file
            final GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST0);
            gft.addServerToUse(HOST1);
            gft.addServerToUse(HOST2);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(_hash);
            gft.setSaveFile(upload);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));

            final GetFileToolReport[] downFile = new GetFileToolReport[1];
            final boolean[] isRunning = {false};

            Thread t = new Thread("gftDownload") {

                @Override()
                public void run() {
                    synchronized (isRunning) {
                        isRunning[0] = true;
                    }
                    downFile[0] = gft.getFile();
                    synchronized (isRunning) {
                        isRunning[0] = false;
                    }
                }
            };
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();

            Thread.yield();
            Thread.sleep(500);

            assertTrue("Should be executing.", gft.isExecuting());

            gft.stop();

            t.join(60 * 1000);

            assertFalse("Thread shouldn't be running.", t.isAlive());
            assertTrue("Since it was stopped, should be finished.", downFile[0].isFinished());


        } finally {
            testNetwork.stop();

            IOUtil.recursiveDelete(upload);
        }
    }

    public void testGetMetaData() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testGetMetaData()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // make a meta data and sign it
            MetaData metaData = DevUtil.createRandomMetaData();
            BigHash hash = null;
            for (FileEncoding fe : metaData.getEncodings()) {
                if (fe.getName().equals(FileEncoding.NONE)) {
                    hash = fe.getHash();
                    break;
                }
            }
            byte[] metaDataBytes = null;
            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                MetaDataUtil.write(metaData, baos);
                metaDataBytes = baos.toByteArray();
            } finally {
                IOUtil.safeClose(baos);
            }

            // set the meta data
            PropagationReturnWrapper wrapper = IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST1), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, metaDataBytes);
            assertFalse(wrapper.isAnyErrors());

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            MetaData downloadedMetaData = gft.getMetaData();

            // verify
            assertNotNull(downloadedMetaData);
            // simple equality comparison
            assertEquals(metaData.getTimestampUploaded(), downloadedMetaData.getTimestampUploaded());
        } finally {
            testNetwork.stop();
        }
    }

    public void testGetProjectFile() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testGetProjectFile()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // create test project
            File uploadFile = DevUtil.createTestProject(RandomUtil.getInt(10) + 2, 1, DataBlockUtil.ONE_MB * 2);
            BigHash hash = uploadTest(uploadFile, HOST1, null).getHash();

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            ProjectFile downloadedProjectFile = gft.getProjectFile();

            // verify
            assertNotNull(downloadedProjectFile);
            // validate the hash
            byte[] projectFileBytes = null;
            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                ProjectFileUtil.write(downloadedProjectFile, baos);
                projectFileBytes = baos.toByteArray();
            } finally {
                IOUtil.safeClose(baos);
            }
            assertEquals(hash, new BigHash(projectFileBytes));
        } finally {
            testNetwork.stop();
        }
    }

    public void testGetFileInMemory() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testGetFileInMemory()");
        testGetFileWithSize(false, RandomUtil.getInt(DataBlockUtil.ONE_MB));
    }

    public void testGetFileDiskBacked() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testGetFileDiskBacked()");
        testGetFileWithSize(false, DataBlockUtil.ONE_MB * 10);
    }

    public void testGetFileEncryptedInMemory() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testGetFileEncryptedInMemory()");
        testGetFileWithSize(true, RandomUtil.getInt(DataBlockUtil.ONE_MB));
    }

    public void testGetFileEncryptedDiskBacked() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testGetFileEncryptedDiskBacked()");
        testGetFileWithSize(true, DataBlockUtil.ONE_MB * 10);
    }

    public void testGetFileWithSize(boolean encrypted, int size) throws Exception {
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // create test project
            File uploadFile = TempFileUtil.createTemporaryFile();
            DevUtil.createTestFile(uploadFile, size);
            String passphrase = null;
            if (encrypted) {
                passphrase = RandomUtil.getString(10);
            }
            BigHash hash = uploadTest(uploadFile, HOST1, passphrase).getHash();

            // verify upload
            assertTrue(IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST1), hash));

            //
            File downloadDirectory = TempFileUtil.createTemporaryDirectory();
            File downloadFile = new File(downloadDirectory, uploadFile.getName());

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            gft.setSaveFile(downloadDirectory);
            if (encrypted) {
                gft.setPassphrase(passphrase);
            }
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            GetFileToolReport report = gft.getFile();

            // verify
            assertFalse(report.isFailed());
            // validate the hash
            assertEquals(new BigHash(uploadFile), new BigHash(downloadFile));
        } finally {
            testNetwork.stop();
        }
    }

    public void testGetDirectory() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testGetDirectory()");
        testGetDirectory(false, false);
    }

    public void testGetDirectoryBatch() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testGetDirectoryBatch()");
        testGetDirectory(true, false);
    }

    public void testGetDirectoryEncrypted() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testGetDirectoryEncrypted()");
        testGetDirectory(true, true);
    }

    public void testGetDirectoryBatchEncrypted() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testGetDirectoryBatchEncrypted()");
        testGetDirectory(true, true);
    }

    public void testGetDirectory(boolean batch, boolean encrypted) throws Exception {
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // create test project
            File uploadFile = DevUtil.createTestProject(RandomUtil.getInt(10) + 2, 1, DataBlockUtil.ONE_MB * 2);
            String passphrase = null;
            if (encrypted) {
                passphrase = RandomUtil.getString(15);
            }
            BigHash hash = uploadTest(uploadFile, HOST1, passphrase).getHash();

            //
            File downloadDir = TempFileUtil.createTemporaryDirectory();

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            gft.setBatch(batch);
            gft.setSaveFile(downloadDir);
            if (encrypted) {
                gft.setPassphrase(passphrase);
            }
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            gft.getDirectory();

            // validate
            System.out.println("Printing uploaded directory structure.");
            Text.printRecursiveDirectoryStructure(uploadFile);
            System.out.println("");
            System.out.println("Printing downloaded directory structure.");
            Text.printRecursiveDirectoryStructure(downloadDir);
            TestUtil.assertDirectoriesEquivalent(uploadFile, new File(downloadDir, uploadFile.getName()));
        } finally {
            testNetwork.stop();
        }
    }

    public void testRegularExpression() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testRegularExpression()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // create test project
            File uploadFile = DevUtil.createTestProject(20, 1, DataBlockUtil.ONE_MB * 2);
            BigHash hash = uploadTest(uploadFile, HOST1, null).getHash();

            //
            File downloadDir = TempFileUtil.createTemporaryDirectory();

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            gft.setSaveFile(downloadDir);
            // files are named 0.tmp through 19.tmp -- this will download 2 of them
            gft.setRegEx("0.tmp$");
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            gft.getDirectory();

            // validate
            System.out.println("Printing uploaded directory structure.");
            Text.printRecursiveDirectoryStructure(uploadFile);
            System.out.println("");
            System.out.println("Printing downloaded directory structure.");
            Text.printRecursiveDirectoryStructure(downloadDir);
            assertEquals(2, new File(downloadDir, uploadFile.getName()).listFiles().length);
        } finally {
            testNetwork.stop();
        }
    }

    public void testFailureFileRandom() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testFailureFileRandom()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // create a fake project
            BigHash hash = DevUtil.getRandomBigHash();

            //
            File download = TempFileUtil.createTemporaryDirectory();

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            gft.setSaveFile(download);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            List<GetFileToolEvent> eventsFired = addEventListener(gft);
            GetFileToolReport report = gft.getFile();

            // verify
            assertTrue(report.isFinished());
            assertTrue(report.isFailed());
            assertEquals(GetFileToolEvent.ACTION_FAILED, eventsFired.get(eventsFired.size() - 1).getAction());
            assertEquals(GetFileToolEvent.TYPE_FILE, eventsFired.get(eventsFired.size() - 1).getType());
        } finally {
            testNetwork.stop();
        }
    }

    public void testFailureDirectoryRandom() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testFailureDirectoryRandom()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // create a fake project
            BigHash hash = DevUtil.getRandomBigHash();

            //
            File download = TempFileUtil.createTemporaryDirectory();

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            gft.setSaveFile(download);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            List<GetFileToolEvent> eventsFired = addEventListener(gft);
            GetFileToolReport report = gft.getDirectory();

            // verify
            assertTrue(report.isFinished());
            assertTrue(report.isFailed());
            assertEquals(GetFileToolEvent.ACTION_STARTING, eventsFired.get(0).getAction());
            assertEquals(GetFileToolEvent.TYPE_DIRECTORY, eventsFired.get(0).getType());
            assertEquals(GetFileToolEvent.ACTION_FAILED, eventsFired.get(eventsFired.size() - 1).getAction());
            assertEquals(GetFileToolEvent.TYPE_DIRECTORY, eventsFired.get(eventsFired.size() - 1).getType());
        } finally {
            testNetwork.stop();
        }
    }

    public void testFailureDirectoryMissingDataChunk() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testFailureDirectoryMissingDataChunk()");
        testFailureDirectoryMissingDataChunk(true);
    }

    public void testFailureDirectoryMissingDataChunkDoNotContinue() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testFailureDirectoryMissingDataChunkDoNotContinue()");
        testFailureDirectoryMissingDataChunk(false);
    }

    public void testFailureDirectoryMissingDataChunk(boolean continueOnFailure) throws Exception {
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // create a fake project
            int fileCount = 100;
            File upload = DevUtil.createTestProject(fileCount, 1, DataBlockUtil.ONE_MB * 2);
            BigHash hash = uploadTest(upload, HOST1, null).getHash();

            // get the list of data chunks
            BigHash[] dataHashes = testNetwork.getFlatFileTrancheServer(HOST1).getDataHashes(BigInteger.ZERO, BigInteger.valueOf(10));
            int filesToKill = 10, filesKilled = 0;
            for (BigHash dataHash : dataHashes) {
                if (dataHash.getLength() > (DataBlockUtil.ONE_MB / 2)) {
                    IOUtil.deleteData(testNetwork.getFlatFileTrancheServer(HOST1), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHash);
                    filesKilled++;
                    if (filesKilled == filesToKill) {
                        break;
                    }
                }
            }

            //
            File download = TempFileUtil.createTemporaryDirectory();

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            gft.setSaveFile(download);
            gft.setContinueOnFailure(continueOnFailure);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            List<GetFileToolEvent> eventsFired = addEventListener(gft);
            GetFileToolReport report = gft.getDirectory();

            // verify
            assertTrue(report.isFinished());
            assertTrue(report.isFailed());
            if (continueOnFailure) {
                assertEquals(fileCount, new File(download, upload.getName()).listFiles().length);
            } else {
                // should have stopped the download of some other files
                if (new File(download, upload.getName()).exists()) {
                    assertTrue(new File(download, upload.getName()).listFiles().length < fileCount - filesToKill);
                }
            }
            // events
            assertEquals(GetFileToolEvent.ACTION_STARTING, eventsFired.get(0).getAction());
            assertEquals(GetFileToolEvent.TYPE_DIRECTORY, eventsFired.get(0).getType());
            assertEquals(GetFileToolEvent.ACTION_FAILED, eventsFired.get(eventsFired.size() - 1).getAction());
            assertEquals(GetFileToolEvent.TYPE_DIRECTORY, eventsFired.get(eventsFired.size() - 1).getType());
        } finally {
            testNetwork.stop();
        }
    }

    public void testFailureDirectoryMissingMetaChunk() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testFailureDirectoryMissingMetaChunk()");
        testFailureDirectoryMissingMetaChunk(true);
    }

    public void testFailureDirectoryMissingMetaChunkDoNotContinue() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testFailureDirectoryMissingMetaChunkDoNotContinue()");
        testFailureDirectoryMissingMetaChunk(false);
    }

    public void testFailureDirectoryMissingMetaChunk(boolean continueOnFailure) throws Exception {
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // create a fake project
            int fileCount = 100;
            File upload = DevUtil.createTestProject(fileCount, 1, DataBlockUtil.ONE_MB * 2);
            BigHash hash = uploadTest(upload, HOST1, null).getHash();

            // get the list of data chunks
            BigHash[] dataHashes = testNetwork.getFlatFileTrancheServer(HOST1).getMetaDataHashes(BigInteger.ZERO, BigInteger.valueOf(10));
            int filesToKill = 10, filesKilled = 0;
            for (BigHash metaHash : dataHashes) {
                if (metaHash.equals(hash)) {
                    continue;
                }
                IOUtil.deleteMetaData(testNetwork.getFlatFileTrancheServer(HOST1), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), metaHash);
                filesKilled++;
                if (filesKilled == filesToKill) {
                    break;
                }
            }

            //
            File download = TempFileUtil.createTemporaryDirectory();

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            gft.setSaveFile(download);
            gft.setContinueOnFailure(continueOnFailure);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            List<GetFileToolEvent> eventsFired = addEventListener(gft);
            GetFileToolReport report = gft.getDirectory();

            // verify
            assertTrue(report.isFinished());
            assertTrue(report.isFailed());
            if (continueOnFailure) {
                assertEquals(fileCount - filesToKill, new File(download, upload.getName()).listFiles().length);
            } else {
                // should have stopped the download of some other files
                if (new File(download, upload.getName()).exists()) {
                    assertTrue(new File(download, upload.getName()).listFiles().length < fileCount - filesToKill);
                }
            }
            // events
            assertEquals(GetFileToolEvent.ACTION_STARTING, eventsFired.get(0).getAction());
            assertEquals(GetFileToolEvent.TYPE_DIRECTORY, eventsFired.get(0).getType());
            assertEquals(GetFileToolEvent.ACTION_FAILED, eventsFired.get(eventsFired.size() - 1).getAction());
            assertEquals(GetFileToolEvent.TYPE_DIRECTORY, eventsFired.get(eventsFired.size() - 1).getType());
        } finally {
            testNetwork.stop();
        }
    }

    public void testMultipleUploader() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testMultipleUploader()");
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // create test project
            File uploadFile = TempFileUtil.createTemporaryFile();
            DevUtil.createTestFile(uploadFile, DataBlockUtil.ONE_MB / 2);
            File uploadFile2 = TempFileUtil.createTemporaryFile();
            IOUtil.copyFile(uploadFile, uploadFile2);
            AddFileToolReport uploadReport1 = uploadTest(uploadFile, HOST1, null);
            BigHash hash = uploadReport1.getHash();
            assertNotNull(hash);
            AddFileToolReport uploadReport2 = uploadTest(uploadFile2, HOST1, null);
            BigHash hash2 = uploadReport2.getHash();
            assertNotNull(hash2);

            // make sure the hashes are the same
            assertEquals(hash, hash2);

            //
            File downloadDir = TempFileUtil.createTemporaryDirectory();

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            gft.setSaveFile(downloadDir);
            gft.setUploaderName(UserCertificateUtil.readUserName(DevUtil.getDevAuthority()));
            gft.setUploadTimestamp(uploadReport1.getTimestampStart());
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
            MetaData metaData = gft.getMetaData();

            // verify uploaders
            assertEquals(2, metaData.getUploaderCount());

            // ambiguity should not be an issue
            GetFileToolReport report = gft.getFile();
            assertFalse(report.isFailed());
            assertTrue(report.isFinished());

            // set the uploader name -- should stil be able download
            gft.setUploaderName(DevUtil.getDevUser().getUserNameFromCert());
            GetFileToolReport report2 = gft.getFile();
            assertFalse(report2.isFailed());
            assertTrue(report2.isFinished());
            assertEquals(0, report2.getFailureExceptions().size());
        } finally {
            testNetwork.stop();
        }
    }

    public void testValidateChunk() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testValidateChunk()");
        // create some bytes
        byte[] bytes = DevUtil.createRandomDataChunk1MB();
        BigHash validHash = new BigHash(bytes);

        // test
        GetFileTool gft = new GetFileTool();
        gft.setValidate(true);
        assertTrue(gft.validateChunk(validHash, bytes));
        assertFalse(gft.validateChunk(DevUtil.getRandomBigHash(), bytes));
        gft.setValidate(false);
        assertTrue(gft.validateChunk(DevUtil.getRandomBigHash(), bytes));
    }

    public void testValidateFile() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testValidateFile()");

        File tempFile = TempFileUtil.createTemporaryFile();
        DevUtil.createTestFile(tempFile, 1, DataBlockUtil.ONE_MB * 2);
        BigHash expectedHash = new BigHash(tempFile);
        MetaData metaData = new MetaData();
        // uploader
        Signature signature = null;
        ArrayList<FileEncoding> encodings = new ArrayList<FileEncoding>();
        Map<String, String> properties = new HashMap<String, String>();
        ArrayList<MetaDataAnnotation> annotations = new ArrayList<MetaDataAnnotation>();
        // make the signature
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(tempFile);
            byte[] sigBytes = SecurityUtil.sign(fis, DevUtil.getDevPrivateKey());
            String algorithm = SecurityUtil.getSignatureAlgorithm(DevUtil.getDevPrivateKey());
            signature = new Signature(sigBytes, algorithm, DevUtil.getDevAuthority());
        } finally {
            IOUtil.safeClose(fis);
        }
        // make encoding
        encodings.add(new FileEncoding(FileEncoding.NONE, DevUtil.getRandomBigHash()));
        // set the basic properties
        properties.put(MetaData.PROP_NAME, RandomUtil.getString(10));
        properties.put(MetaData.PROP_TIMESTAMP_UPLOADED, String.valueOf(TimeUtil.getTrancheTimestamp()));
        // add the uploader
        metaData.addUploader(signature, encodings, properties, annotations);

        GetFileTool gft = new GetFileTool();
        gft.setValidate(true);
        gft.validateFile(expectedHash, metaData, tempFile, new byte[0]);
        // bad hash
        boolean exceptionThrown = false;
        try {
            gft.validateFile(DevUtil.getRandomBigHash(), metaData, tempFile, new byte[0]);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            fail("Exception not thrown.");
        }
        // bad signature
        exceptionThrown = false;
        try {
            gft.validateFile(DevUtil.getRandomBigHash(), DevUtil.createRandomMetaData(), tempFile, new byte[0]);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            fail("Exception not thrown.");
        }

        // should not throw exception
        gft.setValidate(false);
        gft.validateFile(DevUtil.getRandomBigHash(), metaData, tempFile, new byte[0]);
        gft.validateFile(DevUtil.getRandomBigHash(), DevUtil.createRandomMetaData(), tempFile, new byte[0]);
    }

    public void testDownloadData() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testDownloadData()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // upload a data chunk
            byte[] bytes = DevUtil.createRandomDataChunk1MB();
            BigHash hash = new BigHash(bytes);
            IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST1), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, bytes);

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));

            // not meant to be used in this way -- need to set up and tear down connections to test downloadData method
            try {
                gft.setUpConnections();

                // exists
                byte[] downloadedBytes = gft.downloadData(DevUtil.getRandomBigHash(), hash);
                assertNotNull(downloadedBytes);
                assertEquals(hash, new BigHash(downloadedBytes));

                // does not exist
                downloadedBytes = gft.downloadData(DevUtil.getRandomBigHash(), DevUtil.getRandomBigHash());
                assertNull(downloadedBytes);
            } finally {
                gft.tearDownConnections();
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testEvents() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testEvents()");
        testEvents(false, false);
    }

    public void testEventsBatch() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testEventsBatch()");
        testEvents(true, false);
    }

    public void testEventsEncrypted() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testEventsEncrypted()");
        testEvents(false, true);
    }

    public void testEventsBatchEncrypted() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testEventsBatchEncrypted()");
        testEvents(true, true);
    }

    public void testEvents(boolean batch, boolean encrypted) throws Exception {
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // create test project
            int fileCount = RandomUtil.getInt(10) + 10;
            File upload = DevUtil.createTestProject(fileCount, 1, DataBlockUtil.ONE_MB);
            BigHash hash = uploadTest(upload, HOST1, null).getHash();

            //
            File downloadDir = TempFileUtil.createTemporaryDirectory();

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            gft.setSaveFile(downloadDir);
            List<GetFileToolEvent> eventsFired = addEventListener(gft);
            gft.getDirectory();

            // validate
            assertFalse(eventsFired.isEmpty());
            // started directory
            assertEquals(GetFileToolEvent.ACTION_STARTING, eventsFired.get(0).getAction());
            assertEquals(GetFileToolEvent.TYPE_DIRECTORY, eventsFired.get(0).getType());
            // started metadata
            assertEquals(GetFileToolEvent.ACTION_STARTED, eventsFired.get(1).getAction());
            assertEquals(GetFileToolEvent.TYPE_METADATA, eventsFired.get(1).getType());
            assertEquals(GetFileToolEvent.ACTION_TRYING, eventsFired.get(2).getAction());
            assertEquals(GetFileToolEvent.TYPE_METADATA, eventsFired.get(2).getType());
            assertEquals(GetFileToolEvent.ACTION_FINISHED, eventsFired.get(3).getAction());
            assertEquals(GetFileToolEvent.TYPE_METADATA, eventsFired.get(3).getType());
            // getting the project file
            assertEquals(GetFileToolEvent.ACTION_STARTING, eventsFired.get(4).getAction());
            assertEquals(GetFileToolEvent.TYPE_FILE, eventsFired.get(4).getType());
            assertEquals(GetFileToolEvent.ACTION_STARTED, eventsFired.get(5).getAction());
            assertEquals(GetFileToolEvent.TYPE_FILE, eventsFired.get(5).getType());
            // getting the data chunk
            assertEquals(GetFileToolEvent.ACTION_STARTED, eventsFired.get(6).getAction());
            assertEquals(GetFileToolEvent.TYPE_DATA, eventsFired.get(6).getType());
            assertEquals(GetFileToolEvent.ACTION_TRYING, eventsFired.get(7).getAction());
            assertEquals(GetFileToolEvent.TYPE_DATA, eventsFired.get(7).getType());
            assertEquals(GetFileToolEvent.ACTION_FINISHED, eventsFired.get(8).getAction());
            assertEquals(GetFileToolEvent.TYPE_DATA, eventsFired.get(8).getType());
            // finished project file
            assertEquals(GetFileToolEvent.ACTION_FINISHED, eventsFired.get(9).getAction());
            assertEquals(GetFileToolEvent.TYPE_FILE, eventsFired.get(9).getType());
            // started directory
            assertEquals(GetFileToolEvent.ACTION_STARTED, eventsFired.get(10).getAction());
            assertEquals(GetFileToolEvent.TYPE_DIRECTORY, eventsFired.get(10).getType());
            // count the rest
            int startedMetaData = 0, tryingMetaData = 0, finishedMetaData = 0, startedChunk = 0, tryingChunk = 0, finishedChunk = 0, startingFile = 0, startedFile = 0, finishedFile = 0;
            for (int i = 11; i < eventsFired.size() - 1; i++) {
                GetFileToolEvent event = eventsFired.get(i);
                if (event.getType() == GetFileToolEvent.TYPE_METADATA) {
                    if (event.getAction() == GetFileToolEvent.ACTION_STARTED) {
                        startedMetaData++;
                    } else if (event.getAction() == GetFileToolEvent.ACTION_TRYING) {
                        tryingMetaData++;
                    } else if (event.getAction() == GetFileToolEvent.ACTION_FINISHED) {
                        finishedMetaData++;
                    }
                } else if (event.getType() == GetFileToolEvent.TYPE_DATA) {
                    if (event.getAction() == GetFileToolEvent.ACTION_STARTED) {
                        startedChunk++;
                    } else if (event.getAction() == GetFileToolEvent.ACTION_TRYING) {
                        tryingChunk++;
                    } else if (event.getAction() == GetFileToolEvent.ACTION_FINISHED) {
                        finishedChunk++;
                    }
                } else if (event.getType() == GetFileToolEvent.TYPE_FILE) {
                    if (event.getAction() == GetFileToolEvent.ACTION_STARTING) {
                        startingFile++;
                    } else if (event.getAction() == GetFileToolEvent.ACTION_STARTED) {
                        startedFile++;
                    } else if (event.getAction() == GetFileToolEvent.ACTION_FINISHED) {
                        finishedFile++;
                    }
                }
            }
            assertEquals(fileCount, startedChunk);
            assertEquals(fileCount, tryingChunk);
            assertEquals(fileCount, finishedChunk);
            assertEquals(fileCount, startedMetaData);
            assertEquals(fileCount, tryingMetaData);
            assertEquals(fileCount, finishedMetaData);
            assertEquals(fileCount, startingFile);
            assertEquals(fileCount, startedFile);
            assertEquals(fileCount, finishedFile);
            // finished directory last
            assertEquals(GetFileToolEvent.ACTION_FINISHED, eventsFired.get(eventsFired.size() - 1).getAction());
            assertEquals(GetFileToolEvent.TYPE_DIRECTORY, eventsFired.get(eventsFired.size() - 1).getType());
        } finally {
            testNetwork.stop();
        }
    }

    public void testPause() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testPause()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // create test project
            int fileCount = 100;
            File upload = DevUtil.createTestProject(fileCount, 1, DataBlockUtil.ONE_MB);
            BigHash hash = uploadTest(upload, HOST1, null).getHash();

            //
            File downloadDir = TempFileUtil.createTemporaryDirectory();

            // download the meta data
            final GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            gft.setSaveFile(downloadDir);
            final List<GetFileToolEvent> eventsFired = addEventListener(gft);
            final boolean[] failed = new boolean[1];
            failed[0] = false;
            Thread t = new Thread() {

                @Override
                public void run() {
                    ThreadUtil.safeSleep(10000);
                    gft.setPause(true);
                    int eventsFiredCount = eventsFired.size();
                    ThreadUtil.safeSleep(15000);
                    int eventsFiredCount2 = eventsFired.size();
                    gft.setPause(false);
                    failed[0] = eventsFiredCount != eventsFiredCount2;
                }
            };
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();
            gft.getDirectory();

            // validate
            if (failed[0]) {
                fail("Did not pause.");
            }
        } finally {
            testNetwork.stop();
        }
    }
    
    public void testPublishPassphrase() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testPublishPassphrase()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // make a test project
            File uploadDir = DevUtil.createTestProject(RandomUtil.getInt(10) + 2, 1, DataBlockUtil.ONE_MB * 2);
            // use a random passphrase
            String passphrase = RandomUtil.getString(10);
            BigHash hash = uploadTest(uploadDir, HOST1, passphrase).getHash();

            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(hash);
            MetaData md = gft.getMetaData().clone();
            assertTrue("Expect project to be encrypted", md.isEncrypted());
            assertFalse("Expect public passphrase to not be set.", md.isPublicPassphraseSet());

            // publish the passphrase and resend to server
            md.setPublicPassphrase(passphrase);
            IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST1), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, md.toByteArray());

            // reset
            gft.setHash(DevUtil.getRandomBigHash());
            gft.setHash(hash);
            MetaData md2 = gft.getMetaData();
            assertTrue("Expect project to be encrypted", md2.isEncrypted());
            assertTrue("Expect public passphrase to be set.", md.isPublicPassphraseSet());

            // download the upload dir to the download dir
            File downloadDir = TempFileUtil.createTemporaryDirectory();
            gft.setSaveFile(downloadDir);
            gft.getDirectory();

            // validate
            System.out.println("Printing uploaded directory structure.");
            Text.printRecursiveDirectoryStructure(uploadDir);
            System.out.println("");
            System.out.println("Printing downloaded directory structure.");
            Text.printRecursiveDirectoryStructure(downloadDir);
            TestUtil.assertDirectoriesEquivalent(uploadDir, new File(downloadDir, uploadDir.getName()));
        } finally {
            testNetwork.stop();
        }
    }

    public void testReuse() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testReuse()");
        testReuse(false, false);
    }

    public void testReuseEncrypted() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testReuseEncrypted()");
        testReuse(true, false);
    }

    public void testReuseBatch() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testReuseBatch()");
        testReuse(false, true);
    }

    public void testReuseBatchEncrypted() throws Exception {
        TestUtil.printTitle("GetFileToolTest:testReuseBatchEncrypted()");
        testReuse(true, true);
    }

    public void testReuse(boolean encrypted, boolean batch) throws Exception {
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            // create test project
            String passphrase = null;
            if (encrypted) {
                passphrase = RandomUtil.getString(15);
            }
            File upload = DevUtil.createTestProject(20, 1, DataBlockUtil.ONE_MB * 2);
            BigHash hash = uploadTest(upload, HOST1, passphrase).getHash();
            String passphrase2 = null;
            if (encrypted) {
                passphrase2 = RandomUtil.getString(15);
            }
            File upload2 = DevUtil.createTestProject(20, 1, DataBlockUtil.ONE_MB * 2);
            BigHash hash2 = uploadTest(upload2, HOST1, passphrase2).getHash();

            //
            File downloadDir = TempFileUtil.createTemporaryDirectory();
            File downloadDir2 = TempFileUtil.createTemporaryDirectory();

            // download the meta data
            GetFileTool gft = new GetFileTool();
            gft.setValidate(true);
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setBatch(batch);
            gft.addListener(new CommandLineGetFileToolListener(gft, System.out));

            // download
            gft.setHash(hash);
            if (encrypted) {
                gft.setPassphrase(passphrase);
            }
            gft.setSaveFile(downloadDir);
            GetFileToolReport gftr = gft.getDirectory();
            assertFalse(gftr.isFailed());
            assertTrue(gftr.isFinished());

            // validate
            System.out.println("Printing uploaded directory structure.");
            Text.printRecursiveDirectoryStructure(upload);
            System.out.println("");
            System.out.println("Printing downloaded directory structure.");
            Text.printRecursiveDirectoryStructure(downloadDir);
            TestUtil.assertDirectoriesEquivalent(upload, new File(downloadDir, upload.getName()));

            // download 2
            if (encrypted) {
                gft.setPassphrase(passphrase2);
            }
            gft.setHash(hash2);
            gft.setSaveFile(downloadDir2);
            GetFileToolReport gftr2 = gft.getDirectory();

            assertFalse(gftr2.isFailed());
            assertTrue(gftr2.isFinished());

            // validate
            System.out.println("Printing uploaded directory structure.");
            Text.printRecursiveDirectoryStructure(upload2);
            System.out.println("");
            System.out.println("Printing downloaded directory structure.");
            Text.printRecursiveDirectoryStructure(downloadDir2);
            TestUtil.assertDirectoriesEquivalent(upload2, new File(downloadDir2, upload2.getName()));
        } finally {
            testNetwork.stop();
        }
    }

    public List<GetFileToolEvent> addEventListener(GetFileTool gft) {
        final ArrayList<GetFileToolEvent> eventsFired = new ArrayList<GetFileToolEvent>();
        gft.addListener(new GetFileToolListener() {

            public void message(String msg) {
            }

            public void startedMetaData(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void tryingMetaData(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void failedMetaData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                eventsFired.add(event);
            }

            public void finishedMetaData(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void startedData(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void tryingData(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void failedData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                eventsFired.add(event);
            }

            public void finishedData(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void startingFile(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void startedFile(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void skippedFile(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void finishedFile(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void failedFile(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                eventsFired.add(event);
            }

            public void startingDirectory(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void startedDirectory(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void finishedDirectory(GetFileToolEvent event) {
                eventsFired.add(event);
            }

            public void failedDirectory(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                eventsFired.add(event);
            }
        });
        return eventsFired;
    }

    public AddFileToolReport uploadTest(File upload, String host, String passphrase) throws Exception {
        AddFileTool aft = new AddFileTool();
        aft.addListener(new CommandLineAddFileToolListener(aft, System.out));
        aft.setUserCertificate(DevUtil.getDevAuthority());
        aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
        aft.addServerToUse(host);
        aft.setUseUnspecifiedServers(false);
        aft.setTitle(RandomUtil.getString(20));
        aft.setDescription(RandomUtil.getString(20));
        if (passphrase != null) {
            aft.setPassphrase(passphrase);
        }
        aft.setFile(upload);

        AddFileToolReport aftReport = aft.execute();
        assertNotNull(aftReport.getHash());
        assertFalse(aftReport.isFailed());
        assertTrue(aftReport.isFinished());

        return aftReport;
    }
}
