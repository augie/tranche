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
package org.tranche.add;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.FileEncoding;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.configuration.ServerModeFlag;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.exceptions.NoOnlineServersException;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.get.CommandLineGetFileToolListener;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.license.License;
import org.tranche.meta.MetaData;
import org.tranche.project.ProjectFile;
import org.tranche.util.DevUtil;
import org.tranche.get.GetFileToolReport;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.project.ProjectFilePart;
import org.tranche.security.SecurityUtil;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.users.MakeUserZipFileTool;
import org.tranche.users.User;
import org.tranche.users.UserZipFile;
import org.tranche.util.CompressionUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;
import org.tranche.util.ThreadUtil;
import org.tranche.util.TrancheTestCase;
import org.tranche.util.Utils;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class AddFileToolTest extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        AddFileTool.setDebug(true);
        GetFileTool.setDebug(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        AddFileTool.setDebug(false);
        GetFileTool.setDebug(false);
    }

    /**
     * <p>The first two servers have sufficient space, but the last doesn't.</p>
     * @throws java.lang.Exception
     */
    public void testOneServerOutOfSpace() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testOneServerOutOfSpace()");

        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

        File testDir = null;

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull(ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull(ffts3);

            final int chunksToAdd = 2;
            final int sizeLimit = 1024 * 1024 * chunksToAdd + 1024 * 512;

            assertEquals("Expecting one data directory.", 1, ffts1.getConfiguration().getDataDirectories().size());
            for (DataDirectoryConfiguration ddc : ffts1.getConfiguration().getDataDirectories()) {
                ddc.setSizeLimit(sizeLimit + 1024 * 1024 * 10);
            }

            assertEquals("Expecting one data directory.", 1, ffts2.getConfiguration().getDataDirectories().size());
            for (DataDirectoryConfiguration ddc : ffts2.getConfiguration().getDataDirectories()) {
                ddc.setSizeLimit(sizeLimit + 1024 * 1024 * 10);
            }

            assertEquals("Expecting one data directory.", 1, ffts3.getConfiguration().getDataDirectories().size());
            for (DataDirectoryConfiguration ddc : ffts3.getConfiguration().getDataDirectories()) {
                ddc.setSizeLimit(sizeLimit);
            }

            Set<BigHash> hashes = new HashSet();

            // Fill up the data directories. If try to add more after this, will get exceptions.
            for (int i = 0; i < chunksToAdd; i++) {
                byte[] chunk = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(chunk);

                assertFalse("Collection shouldn't contain yet.", hashes.contains(hash));
                hashes.add(hash);

                final BigHash[] hashArr = {hash};

                assertFalse(ffts1.hasData(hashArr)[0]);
                assertFalse(ffts2.hasData(hashArr)[0]);
                assertFalse(ffts3.hasData(hashArr)[0]);
                ffts1.getDataBlockUtil().addData(hash, chunk);
                ffts2.getDataBlockUtil().addData(hash, chunk);
                ffts3.getDataBlockUtil().addData(hash, chunk);
                assertTrue(ffts1.hasData(hashArr)[0]);
                assertTrue(ffts2.hasData(hashArr)[0]);
                assertTrue(ffts3.hasData(hashArr)[0]);
            }

            {
                byte[] chunk = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(chunk);

                assertFalse("Collection shouldn't contain yet.", hashes.contains(hash));
                hashes.add(hash);

                final BigHash[] hashArr = {hash};

                assertFalse(ffts1.hasData(hashArr)[0]);
                ffts1.getDataBlockUtil().addData(hash, chunk);
            }

            {
                byte[] chunk = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(chunk);

                assertFalse("Collection shouldn't contain yet.", hashes.contains(hash));
                hashes.add(hash);

                final BigHash[] hashArr = {hash};

                assertFalse(ffts2.hasData(hashArr)[0]);
                ffts2.getDataBlockUtil().addData(hash, chunk);
            }

            try {
                byte[] chunk = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(chunk);

                assertFalse("Collection shouldn't contain yet.", hashes.contains(hash));
                hashes.add(hash);

                final BigHash[] hashArr = {hash};

                assertFalse(ffts3.hasData(hashArr)[0]);
                ffts3.getDataBlockUtil().addData(hash, chunk);

                fail("Should be out of disk space and throw an exception.");
            } catch (Exception e) { /* expected */ }

            testDir = DevUtil.createTestProject(3, 1024 * 512, 1024 * 1024);

            // set up add file tool
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.setUseUnspecifiedServers(false);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(testDir);

            // upload
            AddFileToolReport uploadReport = aft.execute();

            for (PropagationExceptionWrapper pfe : uploadReport.getFailureExceptions()) {
                assertTrue("Should have something to do with being out of space, instead: " + pfe.exception.getMessage(), pfe.exception.getMessage().toLowerCase().contains("out of space"));
                assertEquals(HOST3, pfe.host);
            }

            assertFalse("Should not have failed.", uploadReport.isFailed());

        } finally {
            testNetwork.stop();

            IOUtil.recursiveDelete(testDir);
        }
    }

    public void testReuse() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testReuse()");
        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

        File testDir1 = null, testDir2 = null;

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull(ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull(ffts3);

            testDir1 = DevUtil.createTestProject(3, 1024 * 512, 1024 * 1024);

            // set up add file tool
            final AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.setUseUnspecifiedServers(false);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(testDir1);

            AddFileToolReport report1 = aft.execute();

            assertTrue("Should be finished.", report1.isFinished());
            assertNotNull("Should have hash.", report1.getHash());


            testDir2 = DevUtil.createTestProject(2, 1024 * 512, 1024 * 1024);

            aft.setFile(testDir2);
            AddFileToolReport report2 = aft.execute();

            System.err.println("DEBUG> Did report2 fail?: " + report2.isFailed());
            System.err.println("DEBUG> report2 errors: " + report2.getFailureExceptions().size());

            for (PropagationExceptionWrapper pfe : report2.getFailureExceptions()) {
                System.err.println("DEBUG>  - " + pfe.exception.getClass().getSimpleName() + " <" + pfe.host + ">: " + pfe.exception.getMessage());
            }

            assertTrue("Should be finished.", report2.isFinished());
            assertNotNull("Should have hash.", report2.getHash());

            assertNotSame("Hashes should be different.", report1.getHash(), report2.getHash());

            File verifyDir1 = null;

            // Download first data set
            try {
                verifyDir1 = TempFileUtil.createTemporaryDirectory("verifyDir1");

                File verifyDir = verifyDir1;
                File testDir = testDir1;

                GetFileTool gft = new GetFileTool();
                gft.setHash(report1.getHash());
                gft.setSaveFile(verifyDir1);
                GetFileToolReport uploadReport = gft.getDirectory();

                assertTrue("Should be finished.", uploadReport.isFinished());
                assertFalse("Shouldn't have failed.", uploadReport.isFailed());

                assertNotSame("Directories should be different.", verifyDir, testDir.getParentFile());

                Text.printRecursiveDirectoryStructure(testDir);
                Text.printRecursiveDirectoryStructure(verifyDir);

                // This is the actual data set directory we downloaded.
                File matchingDir = verifyDir1.listFiles()[0];

                assertTrue("Directories should be same.", areDirectoriesSame(testDir1, matchingDir));
                assertFalse("Directories should not be same.", areDirectoriesSame(testDir2, matchingDir));
            } finally {
                IOUtil.recursiveDelete(verifyDir1);
            }

            File verifyDir2 = null;

            // Download first data set
            try {
                verifyDir2 = TempFileUtil.createTemporaryDirectory("verifyDir2");

                GetFileTool gft = new GetFileTool();
                gft.setHash(report2.getHash());
                gft.setSaveFile(verifyDir2);
                GetFileToolReport uploadReport = gft.getDirectory();

                assertTrue("Should be finished.", uploadReport.isFinished());
                assertFalse("Shouldn't have failed.", uploadReport.isFailed());

                assertNotSame("Directories should be different.", verifyDir2, testDir2.getParentFile());

                Text.printRecursiveDirectoryStructure(testDir2);
                Text.printRecursiveDirectoryStructure(verifyDir2);

                // This is the actual data set directory we downloaded.
                File matchingDir = verifyDir2.listFiles()[0];

                assertTrue("Directories should be same.", areDirectoriesSame(testDir2, matchingDir));
                assertFalse("Directories should not be same.", areDirectoriesSame(testDir1, matchingDir));
            } finally {
                IOUtil.recursiveDelete(verifyDir2);
            }

        } finally {
            testNetwork.stop();
            IOUtil.recursiveDelete(testDir1);
            IOUtil.recursiveDelete(testDir2);
        }
    }

    public void testExplodeTAR() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testExplodeTAR()");
        File upload = DevUtil.createTestProject(10, 1, DataBlockUtil.ONE_MB / 2);
        File compressedUpload = CompressionUtil.tarCompress(upload);
        File renameTo = new File(TempFileUtil.createTemporaryDirectory(), compressedUpload.getName());
        IOUtil.renameFallbackCopy(compressedUpload, renameTo);
        testFile(renameTo.getParentFile(), "title", "description", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, false, true, upload);
    }

    public void testExplodeZIP() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testExplodeZIP()");
        File upload = DevUtil.createTestProject(10, 1, DataBlockUtil.ONE_MB / 2);
        File compressedUpload = CompressionUtil.zipCompress(upload);
        File renameTo = new File(TempFileUtil.createTemporaryDirectory(), compressedUpload.getName());
        IOUtil.renameFallbackCopy(compressedUpload, renameTo);
        testFile(renameTo.getParentFile(), "title", "description", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, false, true, upload);
    }

    public void testExplodeTGZ() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testExplodeTGZ()");
        File upload = DevUtil.createTestProject(10, 1, DataBlockUtil.ONE_MB / 2);
        File compressedUpload = CompressionUtil.tgzCompress(upload);
        File renameTo = new File(TempFileUtil.createTemporaryDirectory(), compressedUpload.getName());
        IOUtil.renameFallbackCopy(compressedUpload, renameTo);
        testFile(renameTo.getParentFile(), "title", "description", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, false, true, upload);
    }

    public void testExplodeTBZ() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testExplodeTBZ()");
        File upload = DevUtil.createTestProject(10, 1, DataBlockUtil.ONE_MB / 2);
        File compressedUpload = CompressionUtil.tbzCompress(upload);
        File renameTo = new File(TempFileUtil.createTemporaryDirectory(), compressedUpload.getName());
        IOUtil.renameFallbackCopy(compressedUpload, renameTo);
        testFile(renameTo.getParentFile(), "title", "description", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, false, true, upload);
    }

    /**
     * <p>Three servers that fail every once in a while. Tool should still work.</p>
     * @throws java.lang.Exception
     */
    public void testAllServersFailingIntermittently() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testAllServersFailingIntermittently()");
        final double failureProbability = 0.05;

        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForFailingDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, failureProbability));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForFailingDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET, failureProbability));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForFailingDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET, failureProbability));
        File testDir = null, verifyDir = null;
        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull(ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull(ffts3);

            assertTrue("Should be online: " + HOST1, NetworkUtil.getStatus().getRow(HOST1).isOnline());
            assertTrue("Should be online: " + HOST2, NetworkUtil.getStatus().getRow(HOST2).isOnline());
            assertTrue("Should be online: " + HOST3, NetworkUtil.getStatus().getRow(HOST3).isOnline());

            testDir = DevUtil.createTestProject(10, 1024 * 512, 1024 * 1024);

            Set<String> hosts = new HashSet();
            hosts.add(HOST1);
            hosts.add(HOST2);
            hosts.add(HOST3);

            // set up add file tool
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.setUseUnspecifiedServers(false);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(testDir);

            // upload
            AddFileToolReport uploadReport = aft.execute();

            System.err.println("DEBUG> Upload tool had " + uploadReport.getFailureExceptions().size() + " failure(s).");
            for (PropagationExceptionWrapper pew : uploadReport.getFailureExceptions()) {
                System.err.println("DEBUG>  - " + pew.exception.getClass().getSimpleName() + "<" + pew.host + ">: " + pew.exception.getMessage());
            }

            assertFalse("Shouldn't have failed.", uploadReport.isFailed());

            verifyDir = TempFileUtil.createTemporaryDirectory();

            GetFileTool gft = new GetFileTool();
            gft.setHash(uploadReport.getHash());
            gft.setSaveFile(verifyDir);
            gft.addServersToUse(hosts);
            gft.setUseUnspecifiedServers(false);
            GetFileToolReport downloadReport = gft.getDirectory();

            System.err.println("Download tool had " + downloadReport.getFailureExceptions().size() + " failure(s) [test: three failing servers at " + failureProbability + "].");
            for (PropagationExceptionWrapper pew : downloadReport.getFailureExceptions()) {
                System.err.println("    * " + pew.exception.getClass().getSimpleName() + "<" + pew.host + ">: " + pew.exception.getMessage());
            }

            assertTrue("Should be finished.", downloadReport.isFinished());
            assertFalse("Shouldn't have failed.", downloadReport.isFailed());

            // This is the actual data set directory we downloaded.
            File matchingDir = verifyDir.listFiles()[0];

            assertTrue("Directories should be same.", areDirectoriesSame(testDir, matchingDir));

        } finally {
            testNetwork.stop();
            IOUtil.recursiveDelete(testDir);
            IOUtil.recursiveDelete(verifyDir);
        }
    }

    /**
     * <p>One server fails around 10% of time. Tool should still work.</p>
     * @throws java.lang.Exception
     */
    public void testOneServerFailingIntermittently() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testOneServerFailingIntermittently()");

        final double failureProbability = 0.1;

        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForFailingDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET, failureProbability));
        File testDir = null, verifyDir = null;
        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull(ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull(ffts3);

            assertTrue("Should be online: " + HOST1, NetworkUtil.getStatus().getRow(HOST1).isOnline());
            assertTrue("Should be online: " + HOST2, NetworkUtil.getStatus().getRow(HOST2).isOnline());
            assertTrue("Should be online: " + HOST3, NetworkUtil.getStatus().getRow(HOST3).isOnline());

            testDir = DevUtil.createTestProject(10, 1024 * 512, 1024 * 1024);

            Set<String> hosts = new HashSet();
            hosts.add(HOST1);
            hosts.add(HOST2);
            hosts.add(HOST3);

            // set up add file tool
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.setUseUnspecifiedServers(false);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(testDir);

            // upload
            AddFileToolReport uploadReport = aft.execute();

            System.err.println("DEBUG> Upload tool had " + uploadReport.getFailureExceptions().size() + " failure(s).");
            for (PropagationExceptionWrapper pew : uploadReport.getFailureExceptions()) {
                System.err.println("DEBUG>  - " + pew.exception.getClass().getSimpleName() + "<" + pew.host + ">: " + pew.exception.getMessage());
            }

            assertFalse("Shouldn't have failed.", uploadReport.isFailed());

            verifyDir = TempFileUtil.createTemporaryDirectory();

            GetFileTool gft = new GetFileTool();
            gft.setHash(uploadReport.getHash());
            gft.setSaveFile(verifyDir);
            gft.addServersToUse(hosts);
            gft.setUseUnspecifiedServers(false);
            GetFileToolReport downloadReport = gft.getDirectory();

            System.err.println("Download tool had " + downloadReport.getFailureExceptions().size() + " failure(s) [test: one failing server at " + failureProbability + "].");
            for (PropagationExceptionWrapper pew : downloadReport.getFailureExceptions()) {
                System.err.println("    * " + pew.exception.getClass().getSimpleName() + "<" + pew.host + ">: " + pew.exception.getMessage());
            }

            assertTrue("Should be finished.", downloadReport.isFinished());
            assertFalse("Shouldn't have failed.", downloadReport.isFailed());

            // This is the actual data set directory we downloaded.
            File matchingDir = verifyDir.listFiles()[0];

            assertTrue("Directories should be same.", areDirectoriesSame(testDir, matchingDir));

        } finally {
            testNetwork.stop();
            IOUtil.recursiveDelete(testDir);
            IOUtil.recursiveDelete(verifyDir);
        }
    }

    /**
     * ================================================================================
     * START: Uncomment block
     * ================================================================================
     */
    public void testFailureAllServersOutOfSpace() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testFailureAllServersOutOfSpace()");

        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

        File testDir = null;

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull(ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull(ffts3);

            final int chunksToAdd = 2;
            final int sizeLimit = 1024 * 1024 * chunksToAdd + 1024 * 512;

            assertEquals("Expecting one data directory.", 1, ffts1.getConfiguration().getDataDirectories().size());
            for (DataDirectoryConfiguration ddc : ffts1.getConfiguration().getDataDirectories()) {
                ddc.setSizeLimit(sizeLimit);
            }

            assertEquals("Expecting one data directory.", 1, ffts2.getConfiguration().getDataDirectories().size());
            for (DataDirectoryConfiguration ddc : ffts2.getConfiguration().getDataDirectories()) {
                ddc.setSizeLimit(sizeLimit);
            }

            assertEquals("Expecting one data directory.", 1, ffts3.getConfiguration().getDataDirectories().size());
            for (DataDirectoryConfiguration ddc : ffts3.getConfiguration().getDataDirectories()) {
                ddc.setSizeLimit(sizeLimit);
            }

            Set<BigHash> hashes = new HashSet();

            // Fill up the data directories. If try to add more after this, will get exceptions.
            for (int i = 0; i < chunksToAdd; i++) {
                byte[] chunk = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(chunk);

                assertFalse("Collection shouldn't contain yet.", hashes.contains(hash));
                hashes.add(hash);

                final BigHash[] hashArr = {hash};

                assertFalse(ffts1.hasData(hashArr)[0]);
                assertFalse(ffts2.hasData(hashArr)[0]);
                assertFalse(ffts3.hasData(hashArr)[0]);
                ffts1.getDataBlockUtil().addData(hash, chunk);
                ffts2.getDataBlockUtil().addData(hash, chunk);
                ffts3.getDataBlockUtil().addData(hash, chunk);
                assertTrue(ffts1.hasData(hashArr)[0]);
                assertTrue(ffts2.hasData(hashArr)[0]);
                assertTrue(ffts3.hasData(hashArr)[0]);
            }

            try {
                byte[] chunk = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(chunk);

                assertFalse("Collection shouldn't contain yet.", hashes.contains(hash));
                hashes.add(hash);

                final BigHash[] hashArr = {hash};

                assertFalse(ffts1.hasData(hashArr)[0]);
                ffts1.getDataBlockUtil().addData(hash, chunk);

                fail("Should be out of disk space and throw an exception.");
            } catch (Exception e) { /* expected */ }

            try {
                byte[] chunk = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(chunk);

                assertFalse("Collection shouldn't contain yet.", hashes.contains(hash));
                hashes.add(hash);

                final BigHash[] hashArr = {hash};

                assertFalse(ffts2.hasData(hashArr)[0]);
                ffts2.getDataBlockUtil().addData(hash, chunk);

                fail("Should be out of disk space and throw an exception.");
            } catch (Exception e) { /* expected */ }

            try {
                byte[] chunk = DevUtil.createRandomDataChunk1MB();
                BigHash hash = new BigHash(chunk);

                assertFalse("Collection shouldn't contain yet.", hashes.contains(hash));
                hashes.add(hash);

                final BigHash[] hashArr = {hash};

                assertFalse(ffts3.hasData(hashArr)[0]);
                ffts3.getDataBlockUtil().addData(hash, chunk);

                fail("Should be out of disk space and throw an exception.");
            } catch (Exception e) { /* expected */ }

            testDir = DevUtil.createTestProject(3, 1024 * 512, 1024 * 1024);

            // set up add file tool
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.setUseUnspecifiedServers(false);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(testDir);
//            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));

            // upload
            AddFileToolReport uploadReport = aft.execute();
            assertTrue("Should have failed.", uploadReport.isFailed());

            for (PropagationExceptionWrapper pfe : uploadReport.getFailureExceptions()) {
                assertTrue("Should have something to do with being out of space, instead: " + pfe.exception.getMessage(), pfe.exception.getMessage().toLowerCase().contains("out of space"));
            }

        } finally {
            testNetwork.stop();

            IOUtil.recursiveDelete(testDir);
        }
    }

    public void testHashSpanEnforcement() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testHashSpanEnforcement()");
        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        Set<HashSpan>[] sets = getThreeEvenlySplitHashSpanSets();

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, sets[0]));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, sets[1]));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, sets[2]));

        File testDir = null;

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull(ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull(ffts3);

            // Assert hash spans for server 1
            assertEquals("Should have only one hash span.", 1, ffts1.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[0].toArray(new HashSpan[0])[0], ffts1.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts1.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[0].toArray(new HashSpan[0])[0], ffts1.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one hash span.", 1, ffts2.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[1].toArray(new HashSpan[0])[0], ffts2.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts2.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[1].toArray(new HashSpan[0])[0], ffts2.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one hash span.", 1, ffts3.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[2].toArray(new HashSpan[0])[0], ffts3.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts3.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[2].toArray(new HashSpan[0])[0], ffts3.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            testDir = DevUtil.createTestProject(10, 1024 * 512, 1024 * 1024);

            // set up add file tool
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.setUseUnspecifiedServers(false);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(testDir);

            // upload
            AddFileToolReport uploadReport = aft.execute();

            assertFalse("Shouldn't have failed.", uploadReport.isFailed());

            {
                FlatFileTrancheServer ffts = ffts1;
                int setsIndex = 0;

                // To prove hash spans distinct
                FlatFileTrancheServer other1 = ffts2;
                FlatFileTrancheServer other2 = ffts3;

                assertEquals("Should have only one hash span.", 1, ffts.getConfiguration().getHashSpans().size());
                assertEquals("Should know which hash span has.", sets[setsIndex].toArray(new HashSpan[0])[0], ffts.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

                assertEquals("Should have only one target hash span.", 1, ffts.getConfiguration().getTargetHashSpans().size());
                assertEquals("Should know which target hash span has.", sets[setsIndex].toArray(new HashSpan[0])[0], ffts.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

                BigHash[] dataHashes = ffts.getDataHashes(BigInteger.ZERO, BigInteger.valueOf(100));
                assertTrue("Should be at least one hash.", dataHashes.length > 0);
                for (BigHash hash : dataHashes) {
                    boolean found = false;
                    for (HashSpan span : ffts.getConfiguration().getHashSpans()) {
                        if (span.contains(hash)) {
                            found = true;
                            break;
                        }
                    }
                    assertTrue("Hash should be in a hash span.", found);

                    found = false;
                    for (HashSpan span : other1.getConfiguration().getHashSpans()) {
                        if (span.contains(hash)) {
                            found = true;
                            break;
                        }
                    }
                    assertFalse("Hash should not be in a hash span.", found);

                    found = false;
                    for (HashSpan span : other2.getConfiguration().getHashSpans()) {
                        if (span.contains(hash)) {
                            found = true;
                            break;
                        }
                    }
                    assertFalse("Hash should not be in a hash span.", found);
                }
            }

            {
                FlatFileTrancheServer ffts = ffts2;
                int setsIndex = 1;

                // To prove hash spans distinct
                FlatFileTrancheServer other1 = ffts3;
                FlatFileTrancheServer other2 = ffts1;

                assertEquals("Should have only one hash span.", 1, ffts.getConfiguration().getHashSpans().size());
                assertEquals("Should know which hash span has.", sets[setsIndex].toArray(new HashSpan[0])[0], ffts.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

                assertEquals("Should have only one target hash span.", 1, ffts.getConfiguration().getTargetHashSpans().size());
                assertEquals("Should know which target hash span has.", sets[setsIndex].toArray(new HashSpan[0])[0], ffts.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

                BigHash[] dataHashes = ffts.getDataHashes(BigInteger.ZERO, BigInteger.valueOf(100));
                assertTrue("Should be at least one hash.", dataHashes.length > 0);
                for (BigHash hash : dataHashes) {
                    boolean found = false;
                    for (HashSpan span : ffts.getConfiguration().getHashSpans()) {
                        if (span.contains(hash)) {
                            found = true;
                            break;
                        }
                    }
                    assertTrue("Hash should be in a hash span.", found);

                    found = false;
                    for (HashSpan span : other1.getConfiguration().getHashSpans()) {
                        if (span.contains(hash)) {
                            found = true;
                            break;
                        }
                    }
                    assertFalse("Hash should not be in a hash span.", found);

                    found = false;
                    for (HashSpan span : other2.getConfiguration().getHashSpans()) {
                        if (span.contains(hash)) {
                            found = true;
                            break;
                        }
                    }
                    assertFalse("Hash should not be in a hash span.", found);
                }
            }

            {
                FlatFileTrancheServer ffts = ffts3;
                int setsIndex = 2;

                // To prove hash spans distinct
                FlatFileTrancheServer other1 = ffts1;
                FlatFileTrancheServer other2 = ffts2;

                assertEquals("Should have only one hash span.", 1, ffts.getConfiguration().getHashSpans().size());
                assertEquals("Should know which hash span has.", sets[setsIndex].toArray(new HashSpan[0])[0], ffts.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

                assertEquals("Should have only one target hash span.", 1, ffts.getConfiguration().getTargetHashSpans().size());
                assertEquals("Should know which target hash span has.", sets[setsIndex].toArray(new HashSpan[0])[0], ffts.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

                BigHash[] dataHashes = ffts.getDataHashes(BigInteger.ZERO, BigInteger.valueOf(100));
                assertTrue("Should be at least one hash.", dataHashes.length > 0);
                for (BigHash hash : dataHashes) {
                    boolean found = false;
                    for (HashSpan span : ffts.getConfiguration().getHashSpans()) {
                        if (span.contains(hash)) {
                            found = true;
                            break;
                        }
                    }
                    assertTrue("Hash should be in a hash span.", found);


                    found = false;
                    for (HashSpan span : other1.getConfiguration().getHashSpans()) {
                        if (span.contains(hash)) {
                            found = true;
                            break;
                        }
                    }
                    assertFalse("Hash should not be in a hash span.", found);

                    found = false;
                    for (HashSpan span : other2.getConfiguration().getHashSpans()) {
                        if (span.contains(hash)) {
                            found = true;
                            break;
                        }
                    }
                    assertFalse("Hash should not be in a hash span.", found);
                }
            }

        } finally {
            testNetwork.stop();

            IOUtil.recursiveDelete(testDir);
        }
    }

    /**
     * <p>Three core servers splitting full hash span, three non-core servers splitting full hash span.</p>
     * <p>Should produce proper non-core servers when given hash.</p>
     * @throws java.lang.Exception
     */
    public void testGetNonCoreServersToUploadTo() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testGetNonCoreServersToUploadTo()");
        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";
        final String HOST4 = "donuts.org";
        final String HOST5 = "elephant.net";
        final String HOST6 = "friday.org";

        Set<HashSpan>[] sets = getThreeEvenlySplitHashSpanSets();

        TestNetwork testNetwork = new TestNetwork();

        // Core servers
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, sets[0]));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, sets[1]));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, sets[2]));

        // Non-core servers
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", false, true, false, sets[0]));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", false, true, false, sets[1]));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST6, 1505, "127.0.0.1", false, true, false, sets[2]));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);

            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull(ts4);

            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull(ts5);

            TrancheServer ts6 = ConnectionUtil.connectHost(HOST6, true);
            assertNotNull(ts6);

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull(ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull(ffts3);

            FlatFileTrancheServer ffts4 = testNetwork.getFlatFileTrancheServer(HOST4);
            assertNotNull(ffts4);

            FlatFileTrancheServer ffts5 = testNetwork.getFlatFileTrancheServer(HOST5);
            assertNotNull(ffts5);

            FlatFileTrancheServer ffts6 = testNetwork.getFlatFileTrancheServer(HOST6);
            assertNotNull(ffts6);

            // Assert hash spans for server 1 (none)
            assertEquals("Should have only one hash span.", 1, ffts1.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[0].toArray(new HashSpan[0])[0], ffts1.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts1.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[0].toArray(new HashSpan[0])[0], ffts1.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            // Assert hash spans for server 2 (none)
            assertEquals("Should have only one hash span.", 1, ffts2.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[1].toArray(new HashSpan[0])[0], ffts2.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts2.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[1].toArray(new HashSpan[0])[0], ffts2.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            // Assert hash spans for server 3 (none)
            assertEquals("Should have only one hash span.", 1, ffts3.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[2].toArray(new HashSpan[0])[0], ffts3.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts3.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[2].toArray(new HashSpan[0])[0], ffts3.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            // Assert hash spans for server 4 (non-core)
            assertEquals("Should have only one hash span.", 1, ffts4.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[0].toArray(new HashSpan[0])[0], ffts4.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts4.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[0].toArray(new HashSpan[0])[0], ffts4.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            // Assert hash spans for server 5 (non-core)
            assertEquals("Should have only one hash span.", 1, ffts5.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[1].toArray(new HashSpan[0])[0], ffts5.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts5.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[1].toArray(new HashSpan[0])[0], ffts5.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            // Assert hash spans for server 6 (non-core)
            assertEquals("Should have only one hash span.", 1, ffts6.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[2].toArray(new HashSpan[0])[0], ffts6.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts6.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[2].toArray(new HashSpan[0])[0], ffts6.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            // set up add file tool
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.addServerToUse(HOST4);
            aft.addServerToUse(HOST5);
            aft.addServerToUse(HOST6);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());

            assertEquals("Should know how many servers using.", 6, aft.getServersToUse().size());

            final int hashesToCheck = 100;

            for (int i = 0; i < hashesToCheck; i++) {
                BigHash hash = DevUtil.getRandomBigHash();

                Collection<String> hostsToUse = aft.getNonCoreServersToUploadTo(hash);
                assertEquals("Should always be one server to use.", 1, hostsToUse.size());
                String host = hostsToUse.toArray(new String[0])[0];

                assertTrue("Should be one of three non-core servers, but instead: " + host, host.equals(HOST4) || host.equals(HOST5) || host.equals(HOST6));

                FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(host);
                boolean isFound = false;
                for (HashSpan span : ffts.getConfiguration().getHashSpans()) {
                    if (span.contains(hash)) {
                        isFound = true;
                        break;
                    }
                }
                assertTrue("Hash Should be in hash span.", isFound);
            }
        } finally {
            testNetwork.stop();
        }
    }

    /**
     * <p>Three core servers splitting full hash span, three non-core servers splitting full hash span.</p>
     * <p>Should produce proper core servers when given hash.</p>
     * @throws java.lang.Exception
     */
    public void testGetCoreServersToUploadTo() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testGetCoreServersToUploadTo()");
        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";
        final String HOST4 = "donuts.org";
        final String HOST5 = "elephant.net";
        final String HOST6 = "friday.org";

        Set<HashSpan>[] sets = getThreeEvenlySplitHashSpanSets();

        TestNetwork testNetwork = new TestNetwork();

        // Core servers
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, sets[0]));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, sets[1]));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, sets[2]));

        // Non-core servers
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", false, true, false, sets[0]));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST5, 1504, "127.0.0.1", false, true, false, sets[1]));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST6, 1505, "127.0.0.1", false, true, false, sets[2]));

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);

            TrancheServer ts4 = ConnectionUtil.connectHost(HOST4, true);
            assertNotNull(ts4);

            TrancheServer ts5 = ConnectionUtil.connectHost(HOST5, true);
            assertNotNull(ts5);

            TrancheServer ts6 = ConnectionUtil.connectHost(HOST6, true);
            assertNotNull(ts6);

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull(ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull(ffts3);

            FlatFileTrancheServer ffts4 = testNetwork.getFlatFileTrancheServer(HOST4);
            assertNotNull(ffts4);

            FlatFileTrancheServer ffts5 = testNetwork.getFlatFileTrancheServer(HOST5);
            assertNotNull(ffts5);

            FlatFileTrancheServer ffts6 = testNetwork.getFlatFileTrancheServer(HOST6);
            assertNotNull(ffts6);

            // Assert hash spans for server 1 (none)
            assertEquals("Should have only one hash span.", 1, ffts1.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[0].toArray(new HashSpan[0])[0], ffts1.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts1.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[0].toArray(new HashSpan[0])[0], ffts1.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            // Assert hash spans for server 2 (none)
            assertEquals("Should have only one hash span.", 1, ffts2.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[1].toArray(new HashSpan[0])[0], ffts2.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts2.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[1].toArray(new HashSpan[0])[0], ffts2.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            // Assert hash spans for server 3 (none)
            assertEquals("Should have only one hash span.", 1, ffts3.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[2].toArray(new HashSpan[0])[0], ffts3.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts3.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[2].toArray(new HashSpan[0])[0], ffts3.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            // Assert hash spans for server 4 (non-core)
            assertEquals("Should have only one hash span.", 1, ffts4.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[0].toArray(new HashSpan[0])[0], ffts4.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts4.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[0].toArray(new HashSpan[0])[0], ffts4.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            // Assert hash spans for server 5 (non-core)
            assertEquals("Should have only one hash span.", 1, ffts5.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[1].toArray(new HashSpan[0])[0], ffts5.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts5.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[1].toArray(new HashSpan[0])[0], ffts5.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            // Assert hash spans for server 6 (non-core)
            assertEquals("Should have only one hash span.", 1, ffts6.getConfiguration().getHashSpans().size());
            assertEquals("Should know which hash span has.", sets[2].toArray(new HashSpan[0])[0], ffts6.getConfiguration().getHashSpans().toArray(new HashSpan[0])[0]);

            assertEquals("Should have only one target hash span.", 1, ffts6.getConfiguration().getTargetHashSpans().size());
            assertEquals("Should know which target hash span has.", sets[2].toArray(new HashSpan[0])[0], ffts6.getConfiguration().getTargetHashSpans().toArray(new HashSpan[0])[0]);

            // set up add file tool
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.addServerToUse(HOST4);
            aft.addServerToUse(HOST5);
            aft.addServerToUse(HOST6);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());

            assertEquals("Should know how many servers using.", 6, aft.getServersToUse().size());

            final int hashesToCheck = 100;

            for (int i = 0; i < hashesToCheck; i++) {
                BigHash hash = DevUtil.getRandomBigHash();

                Collection<String> hostsToUse = aft.getCoreServersToUploadTo(hash);
                assertEquals("Should always be one server to use.", 1, hostsToUse.size());
                String host = hostsToUse.toArray(new String[0])[0];

                assertTrue("Should be one of three core servers, but instead: " + host, host.equals(HOST1) || host.equals(HOST2) || host.equals(HOST3));

                FlatFileTrancheServer ffts = testNetwork.getFlatFileTrancheServer(host);
                boolean isFound = false;
                for (HashSpan span : ffts.getConfiguration().getHashSpans()) {
                    if (span.contains(hash)) {
                        isFound = true;
                        break;
                    }
                }
                assertTrue("Hash Should be in hash span.", isFound);
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testLockedVariablesOnExecute() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testLockedVariablesOnExecute()");
        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

        File testDir = null;

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull(ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull(ffts3);

            testDir = DevUtil.createTestProject(3, 1024 * 512, 1024 * 1024);

            // set up add file tool
            final AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.setUseUnspecifiedServers(false);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(testDir);

            final AddFileToolReport[] reportArr = new AddFileToolReport[1];
            final boolean[] isRunning = {false};

            Thread t = new Thread("AFT upload") {

                @Override()
                public void run() {
                    synchronized (isRunning) {
                        isRunning[0] = true;
                    }
                    reportArr[0] = aft.execute();
                    synchronized (isRunning) {
                        isRunning[0] = false;
                    }
                }
            };
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();

            Thread.yield();
            Thread.sleep(250);

            final long start = System.currentTimeMillis();

            int loopCount = 0;

            // Give thread 60 seconds to join
            while (t.isAlive() && System.currentTimeMillis() - start < 60 * 1000) {

                try {

                    aft.addServerToUse(HOST1);
                    // Sleep if unlocked by execute not exitted
                    Thread.sleep(100);
                    if (isRunning[0]) {
                        fail("Should have thrown exception--aft is locked.");
                    }
                } catch (Exception e) { /* Expected */ }

                try {
                    aft.setUseUnspecifiedServers(false);
                    // Sleep if unlocked by execute not exitted
                    Thread.sleep(100);
                    if (isRunning[0]) {
                        fail("Should have thrown exception--aft is locked.");
                    }
                } catch (Exception e) { /* Expected */ }

                try {
                    aft.setUserCertificate(DevUtil.getDevAuthority());
                    // Sleep if unlocked by execute not exitted
                    Thread.sleep(100);
                    if (isRunning[0]) {
                        fail("Should have thrown exception--aft is locked.");
                    }
                } catch (Exception e) { /* Expected */ }

                try {
                    aft.setUserCertificate(DevUtil.getDevAuthority());
                    // Sleep if unlocked by execute not exitted
                    Thread.sleep(100);
                    if (isRunning[0]) {
                        fail("Should have thrown exception--aft is locked.");
                    }
                } catch (Exception e) { /* Expected */ }

                try {
                    aft.setFile(testDir);
                    // Sleep if unlocked by execute not exitted
                    Thread.sleep(100);
                    if (isRunning[0]) {
                        fail("Should have thrown exception--aft is locked.");
                    }
                } catch (Exception e) { /* Expected */ }

                loopCount++;
            }

            assertTrue("Should be a minimum number of loops, but instead found: " + loopCount, loopCount > 0);
            assertNotNull(reportArr[0]);
            assertFalse("Should pass no problems.", reportArr[0].isFailed());

        } finally {
            testNetwork.stop();

            IOUtil.recursiveDelete(testDir);
        }
    }

    public void testStop() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testStop()");
        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

        File testDir = null;

        try {
            testNetwork.start();

            TrancheServer ts1 = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(ts1);

            TrancheServer ts2 = ConnectionUtil.connectHost(HOST2, true);
            assertNotNull(ts2);

            TrancheServer ts3 = ConnectionUtil.connectHost(HOST3, true);
            assertNotNull(ts3);

            FlatFileTrancheServer ffts1 = testNetwork.getFlatFileTrancheServer(HOST1);
            assertNotNull(ffts1);

            FlatFileTrancheServer ffts2 = testNetwork.getFlatFileTrancheServer(HOST2);
            assertNotNull(ffts2);

            FlatFileTrancheServer ffts3 = testNetwork.getFlatFileTrancheServer(HOST3);
            assertNotNull(ffts3);

            testDir = DevUtil.createTestProject(3, 1024 * 512, 1024 * 1024);

            // set up add file tool
            final AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.setUseUnspecifiedServers(false);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(testDir);

            final AddFileToolReport[] reportArr = new AddFileToolReport[1];
            final boolean[] isRunning = {false};

            Thread t = new Thread("AFT upload") {

                @Override()
                public void run() {
                    synchronized (isRunning) {
                        isRunning[0] = true;
                    }
                    reportArr[0] = aft.execute();
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

            assertTrue("Should be executing.", aft.isExecuting());

            aft.stop();

            t.join(60 * 1000);

            assertFalse("Thread shouldn't be running.", t.isAlive());
            assertTrue("Since it was stopped, should be finished.", reportArr[0].isFinished());
            assertNull("Shouldn't have a hash.", reportArr[0].getHash());

        } finally {
            testNetwork.stop();

            IOUtil.recursiveDelete(testDir);
        }
    }

    public void testFailureDeleteFileDuringUpload() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testFailureDeleteFileDuringUpload()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            int fileCount = 100;
            final File uploadFile = DevUtil.createTestProject(fileCount, 1, DataBlockUtil.ONE_MB * 2);

            // add the data
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));
            aft.setFile(uploadFile);

            Thread t = new Thread() {

                @Override
                public void run() {
                    ThreadUtil.safeSleep(5000);
                    IOUtil.recursiveDeleteWithWarning(uploadFile);
                }
            };
            t.setDaemon(true);
            t.start();

            AddFileToolReport uploadReport = aft.execute();
            assertTrue(uploadReport.isFailed());
            assertTrue(uploadReport.isFinished());
            assertNull(uploadReport.getHash());
            // one file will fail
            assertTrue(uploadReport.getFailureExceptions().size() >= 1);
            Iterator<PropagationExceptionWrapper> iterator = uploadReport.getFailureExceptions().iterator();
            assertTrue(iterator.next().exception instanceof FileNotFoundException);
        } finally {
            testNetwork.stop();
        }
    }

    public void testEmptyDirectory() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testEmptyDirectory()");

        AddFileToolReport uploadReport = null;
        File upload = null;
        try {
            upload = TempFileUtil.createTemporaryDirectory("name.directory");
            uploadReport = testFailure(upload, "title", "descrption", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
        } finally {
            IOUtil.safeDelete(upload);
        }

        // assert on exception
        assertEquals(1, uploadReport.getFailureExceptions().size());
        assertTrue(uploadReport.getFailureExceptions().iterator().next().exception instanceof Exception);
    }

    public void testNullFile() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testNullFile()");

        AddFileToolReport uploadReport = testFailure(null, "title", "descrption", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

        // assert on exception
        assertEquals(1, uploadReport.getFailureExceptions().size());
        assertTrue(uploadReport.getFailureExceptions().iterator().next().exception instanceof NullPointerException);
    }

    public void testExpiredCertificate() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testExpiredCertificate()");

        // create a certificate that expires one day ago
        MakeUserZipFileTool userTool = new MakeUserZipFileTool();
        userTool.setValidDays(-1);
        userTool.setUserFile(TempFileUtil.createTemporaryFile());
        UserZipFile uzf = userTool.makeCertificate();
        uzf.setFlags(SecurityUtil.getAdmin().getFlags());

        AddFileToolReport uploadReport = testFailure("title", "descrption", uzf.getCertificate(), uzf.getPrivateKey());

        // assert on exception
        assertEquals(1, uploadReport.getFailureExceptions().size());
        assertTrue(uploadReport.getFailureExceptions().iterator().next().exception instanceof CertificateExpiredException);
    }

    public void testNotYetValidCertificate() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testNotYetValidCertificate()");

        // create a certificate that expires one day ago
        MakeUserZipFileTool userTool = new MakeUserZipFileTool();
        userTool.setValidDays(new Date(TimeUtil.getTrancheTimestamp() + 100000000), 1);
        userTool.setUserFile(TempFileUtil.createTemporaryFile());
        UserZipFile uzf = userTool.makeCertificate();
        uzf.setFlags(SecurityUtil.getAdmin().getFlags());

        AddFileToolReport uploadReport = testFailure("title", "descrption", uzf.getCertificate(), uzf.getPrivateKey());

        // assert on exception
        assertEquals(1, uploadReport.getFailureExceptions().size());
        assertTrue(uploadReport.getFailureExceptions().iterator().next().exception instanceof CertificateNotYetValidException);
    }

    public void testNullCertificate() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testNullCertificate()");

        AddFileToolReport uploadReport = testFailure("title", "descrption", null, DevUtil.getDevPrivateKey());

        // assert on exception
        assertEquals(1, uploadReport.getFailureExceptions().size());
        assertTrue(uploadReport.getFailureExceptions().iterator().next().exception instanceof NullPointerException);
    }

    public void testNullPrivateKey() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testNullPrivateKey()");

        AddFileToolReport uploadReport = testFailure("title", "descrption", null, DevUtil.getDevPrivateKey());

        // assert on exception
        assertEquals(1, uploadReport.getFailureExceptions().size());
        assertTrue(uploadReport.getFailureExceptions().iterator().next().exception instanceof NullPointerException);
    }

    public void testNullDescription() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testNullDescription()");

        AddFileToolReport uploadReport = testFailure("title", null, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());

        // assert on exception
        assertEquals(1, uploadReport.getFailureExceptions().size());
        assertTrue(uploadReport.getFailureExceptions().iterator().next().exception instanceof NullPointerException);
    }

    public void testUserReadOnly() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testUserReadOnly()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        UserZipFile user = DevUtil.makeNewUser();
        user.setFlags(User.CAN_GET_CONFIGURATION);
        HashSet<User> userSet = new HashSet<User>();
        userSet.add(user);
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, userSet));
        try {
            testNetwork.start();

            File uploadFile = Utils.makeTempFile(Utils.makeRandomData(10000));

            // add the data
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.setUserCertificate(user.getCertificate());
            aft.setUserPrivateKey(user.getPrivateKey());
            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));
            aft.setFile(uploadFile);

            AddFileToolReport uploadReport = aft.execute();
            assertTrue(uploadReport.isFailed());
            assertTrue(uploadReport.isFinished());
            assertNull(uploadReport.getHash());
            // one file will fail
            assertEquals(1, uploadReport.getFailureExceptions().size());
            Iterator<PropagationExceptionWrapper> iterator = uploadReport.getFailureExceptions().iterator();
            assertTrue(iterator.next().exception instanceof GeneralSecurityException);
        } finally {
            testNetwork.stop();
        }
    }

    public void testMetaDataAnnotations() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testMetaDataAnnotations()");
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            File upload = TempFileUtil.createTempFileWithName("name.file");
            DevUtil.createTestFile(upload, DataBlockUtil.ONE_MB / 2);

            // set up add file tool
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));

            // variables
            aft.setTitle("title");
            aft.setDescription("description");
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(upload);

            List<MetaDataAnnotation> annotations = new LinkedList<MetaDataAnnotation>();
            for (int i = 0; i < 5; i++) {
                annotations.add(new MetaDataAnnotation(RandomUtil.getString(10), RandomUtil.getString(10)));
            }
            aft.setMetaDataAnnotations(annotations);

            // upload
            AddFileToolReport uploadReport = aft.execute();

            // verify
            assertTrue(uploadReport.isFinished());
            assertFalse(uploadReport.isFailed());
            assertNotNull(uploadReport.getHash());

            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(uploadReport.getHash());
            MetaData metaData = gft.getMetaData();

            assertEquals(5, metaData.getAnnotations().size());
            for (int i = 0; i < annotations.size(); i++) {
                assertEquals(annotations.get(0).getName(), metaData.getAnnotations().get(0).getName());
                assertEquals(annotations.get(0).getValue(), metaData.getAnnotations().get(0).getValue());
                assertEquals(annotations.get(1).getName(), metaData.getAnnotations().get(1).getName());
                assertEquals(annotations.get(1).getValue(), metaData.getAnnotations().get(1).getValue());
                assertEquals(annotations.get(2).getName(), metaData.getAnnotations().get(2).getName());
                assertEquals(annotations.get(2).getValue(), metaData.getAnnotations().get(2).getValue());
                assertEquals(annotations.get(3).getName(), metaData.getAnnotations().get(3).getName());
                assertEquals(annotations.get(3).getValue(), metaData.getAnnotations().get(3).getValue());
                assertEquals(annotations.get(4).getName(), metaData.getAnnotations().get(4).getName());
                assertEquals(annotations.get(4).getValue(), metaData.getAnnotations().get(4).getValue());
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testDefaults() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testDefaults()");
        AddFileTool aft = new AddFileTool();
        assertEquals(AddFileTool.DEFAULT_COMPRESS, aft.isCompress());
        assertEquals(AddFileTool.DEFAULT_DATA_ONLY, aft.isDataOnly());
        assertEquals(AddFileTool.DEFAULT_EXPLODE_BEFORE_UPLOAD, aft.isExplodeBeforeUpload());
        assertEquals(AddFileTool.DEFAULT_SHOW_META_DATA_IF_ENCRYPTED, aft.isShowMetaDataIfEncrypted());
        assertEquals(AddFileTool.DEFAULT_THREADS, aft.getThreadCount());
        assertEquals(AddFileTool.DEFAULT_USE_UNSPECIFIED_SERVERS, aft.isUsingUnspecifiedServers());
    }

    public void testDataOnly() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testDataOnly()");
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            int fileCount = 20;

            File upload = DevUtil.createTestProject(fileCount, 1, DataBlockUtil.ONE_MB / 2);

            // set up add file tool
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(upload);
            aft.setDataOnly(true);

            // upload
            AddFileToolReport uploadReport = aft.execute();

            // verify
            assertTrue(uploadReport.isFinished());
            assertFalse(uploadReport.isFailed());
            assertNull(uploadReport.getHash());

            // 0 meta data, 1 data chunk
            for (TestServerConfiguration testServer : testNetwork.getServerConfigs()) {
                assertEquals(0, testNetwork.getFlatFileTrancheServer(testServer.ostensibleHost).getMetaDataHashes(BigInteger.valueOf(0), BigInteger.valueOf(fileCount * 2)).length);
                assertEquals(fileCount, testNetwork.getFlatFileTrancheServer(testServer.ostensibleHost).getDataHashes(BigInteger.valueOf(0), BigInteger.valueOf(fileCount * 2)).length);
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testSticky() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testSticky()");
        String HOST1 = "server1.com";
        String HOST2 = "server2.com";
        String HOST3 = "server3.com";
        String HOST4 = "server4.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST4, 1503, "127.0.0.1", true, true, false, new HashSet<HashSpan>(), DevUtil.DEV_USER_SET));
        try {
            // set up network
            testNetwork.start();

            File upload = TempFileUtil.createTempFileWithName("name.file");
            DevUtil.createTestFile(upload, DataBlockUtil.ONE_MB / 2);

            // set up add file tool
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.addStickyServer(HOST4);
            aft.setUseUnspecifiedServers(false);
            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));

            // variables
            aft.setTitle("title");
            aft.setDescription("description");
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(upload);

            // upload
            AddFileToolReport uploadReport = aft.execute();

            // verify
            assertTrue(uploadReport.isFinished());
            assertFalse(uploadReport.isFailed());
            if (uploadReport.getHash() == null) {
                fail("Hash is null.");
            }

            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST4);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(uploadReport.getHash());
            MetaData metaData = gft.getMetaData();
            for (TestServerConfiguration testServer : testNetwork.getServerConfigs()) {
                IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(testServer.ostensibleHost), uploadReport.getHash());
                for (BigHash chunkHash : metaData.getParts()) {
                    IOUtil.hasData(testNetwork.getFlatFileTrancheServer(testServer.ostensibleHost), chunkHash);
                }
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testPause() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testPause()");
        String HOST1 = "server1.com";
        // set up a dummy network
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            File upload = DevUtil.createTestProject(50, 1, DataBlockUtil.ONE_MB * 5);

            // set up add file tool
            final AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.setUseUnspecifiedServers(false);
            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));

            // variables
            aft.setTitle("title");
            aft.setDescription("description");
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setLicense(null);
            aft.setFile(upload);

            // perform upload
            aft.execute();

            final List<AddFileToolEvent> eventsFired = addEventListener(aft);
            final boolean[] failed = new boolean[1];
            failed[0] = false;
            Thread t = new Thread() {

                @Override
                public void run() {
                    ThreadUtil.safeSleep(10000);
                    aft.setPause(true);
                    int eventsFiredCount = eventsFired.size();
                    ThreadUtil.safeSleep(15000);
                    int eventsFiredCount2 = eventsFired.size();
                    aft.setPause(false);
                    failed[0] = eventsFiredCount != eventsFiredCount2;
                }
            };
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();

            // validate
            if (failed[0]) {
                fail("Did not pause.");
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testNoServersToUploadTo() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testNoServersToUploadTo()");

        File upload = TempFileUtil.createTempFileWithName("name.file");
        DevUtil.createTestFile(upload, DataBlockUtil.ONE_MB / 2);

        System.out.println("----------------------------------------------------------------------");
        System.out.println("STARTING 1: No servers at all");
        System.out.println("----------------------------------------------------------------------");
        long start = System.currentTimeMillis();

        // no servers at all
        try {
            TestNetwork testNetwork = new TestNetwork();
            try {
                testNetwork.start();
                // upload
                AddFileToolReport uploadReport = testUpload(new HashSet<String>(), upload, "title", "description", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, true, false);

                // verify
                assertTrue(uploadReport.isFinished());
                assertTrue(uploadReport.isFailed());
                assertNull(uploadReport.getHash());
                // assert on exception
                assertEquals(1, uploadReport.getFailureExceptions().size());
                assertTrue(uploadReport.getFailureExceptions().iterator().next().exception instanceof Exception);
            } finally {
                testNetwork.stop();
            }
        } finally {
            System.out.println("----------------------------------------------------------------------");
            System.out.println("FINISH 1: No servers at all -- " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
            System.out.println("----------------------------------------------------------------------");
        }

        System.out.println("----------------------------------------------------------------------");
        System.out.println("STARTING 2: None writable");
        System.out.println("----------------------------------------------------------------------");
        start = System.currentTimeMillis();

        // none writable
        try {
            String HOST1 = "server1.com";
            // set up a dummy network
            TestNetwork testNetwork = new TestNetwork();
            testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
            try {
                testNetwork.start();
                StatusTableRow row = NetworkUtil.getStatus().getRow(HOST1);
                Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
                config.addUser(DevUtil.getDevUser());
                config.addHashSpan(HashSpan.FULL);
                config.addTargetHashSpan(HashSpan.FULL);
                config.setValue(ConfigKeys.SERVER_MODE_FLAG_ADMIN, String.valueOf(ServerModeFlag.CAN_READ));
                testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
                testNetwork.getFlatFileTrancheServer(HOST1).saveConfiguration();
                row.update(config);
                Set<StatusTableRow> rows = new HashSet<StatusTableRow>();
                rows.add(row);
                NetworkUtil.getStatus().setRows(rows);
                Set<String> hosts = new HashSet<String>();
                hosts.add(HOST1);

                // upload
                AddFileToolReport uploadReport = testUpload(hosts, upload, "title", "description", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, true, false);

                // verify
                assertTrue(uploadReport.isFinished());
                assertTrue(uploadReport.isFailed());
                assertNull(uploadReport.getHash());
                // assert on exception
                assertEquals(1, uploadReport.getFailureExceptions().size());
                assertTrue(uploadReport.getFailureExceptions().iterator().next().exception instanceof NoOnlineServersException);
            } finally {
                testNetwork.stop();
            }
        } finally {
            System.out.println("----------------------------------------------------------------------");
            System.out.println("FINISH 2: None writable -- " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
            System.out.println("----------------------------------------------------------------------");
        }

        System.out.println("----------------------------------------------------------------------");
        System.out.println("STARTING 3: No target hash spans");
        System.out.println("----------------------------------------------------------------------");
        start = System.currentTimeMillis();

        // no target hash spans
        try {
            String HOST1 = "server1.com";
            // set up a dummy network
            TestNetwork testNetwork = new TestNetwork();
            testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false));
            try {
                // set up network
                testNetwork.start();
                StatusTableRow row = NetworkUtil.getStatus().getRow(HOST1);
                Configuration config = testNetwork.getFlatFileTrancheServer(HOST1).getConfiguration();
                config.addUser(DevUtil.getDevUser());
                testNetwork.getFlatFileTrancheServer(HOST1).setConfiguration(config);
                row.update(config);
                Set<StatusTableRow> rows = new HashSet<StatusTableRow>();
                rows.add(row);
                NetworkUtil.getStatus().setRows(rows);
                Set<String> hosts = new HashSet<String>();
                hosts.add(HOST1);

                // upload
                AddFileToolReport uploadReport = testUpload(hosts, upload, "title", "description", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, true, false);

                // verify
                assertTrue(uploadReport.isFinished());
                assertTrue(uploadReport.isFailed());
                assertNull(uploadReport.getHash());
                // assert on exception
                assertEquals(1, uploadReport.getFailureExceptions().size());
                assertTrue(uploadReport.getFailureExceptions().iterator().next().exception instanceof NoOnlineServersException);
            } finally {
                testNetwork.stop();
            }
        } finally {
            System.out.println("----------------------------------------------------------------------");
            System.out.println("FINISH 3: No target hash spans -- " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
            System.out.println("----------------------------------------------------------------------");
        }

        System.out.println("----------------------------------------------------------------------");
        System.out.println("STARTING 4: No online");
        System.out.println("----------------------------------------------------------------------");
        start = System.currentTimeMillis();

        // no online
        try {
            String HOST1 = "server1.com";
            // set up a dummy network
            TestNetwork testNetwork = new TestNetwork();
            testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, false, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
            try {
                // set up network
                testNetwork.start();
                Set<String> hosts = new HashSet<String>();
                hosts.add(HOST1);

                // upload
                AddFileToolReport uploadReport = testUpload(hosts, upload, "title", "description", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, true, false);

                // verify
                assertTrue(uploadReport.isFinished());
                assertTrue(uploadReport.isFailed());
                assertNull(uploadReport.getHash());
                // assert on exception
                assertEquals(1, uploadReport.getFailureExceptions().size());
                assertTrue(uploadReport.getFailureExceptions().iterator().next().exception instanceof NoOnlineServersException);
            } finally {
                testNetwork.stop();
            }
        } finally {
            System.out.println("----------------------------------------------------------------------");
            System.out.println("FINISH 4: No online -- " + Text.getPrettyEllapsedTimeString(System.currentTimeMillis() - start));
            System.out.println("----------------------------------------------------------------------");
        }
    }

    public AddFileToolReport testFailure(String title, String description, X509Certificate certificate, PrivateKey privateKey) throws Exception {
        File upload = TempFileUtil.createTempFileWithName("name.file");
        DevUtil.createTestFile(upload, DataBlockUtil.ONE_MB / 2);
        return testFailure(upload, title, description, certificate, privateKey);
    }

    public AddFileToolReport testFailure(File upload, String title, String description, X509Certificate certificate, PrivateKey privateKey) throws Exception {
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            Set<String> hosts = new HashSet<String>();
            hosts.add(HOST1);

            // upload
            AddFileToolReport uploadReport = testUpload(hosts, upload, title, description, certificate, privateKey, null, null, true, false);

            // verify
            assertTrue(uploadReport.isFinished());
            assertTrue(uploadReport.isFailed());
            assertNull(uploadReport.getHash());

            return uploadReport;
        } finally {
            testNetwork.stop();
        }
    }

    public void testFileSmall() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testFileSmall()");
        testFile(DataBlockUtil.ONE_MB / 2, "title", "descrption", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, true, false, null);
    }

    public void testFileEncrypted() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testFileEncrypted()");
        testFile(DataBlockUtil.ONE_MB / 2, "title", "descrption", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, "passphrase", true, false, null);
    }

    public void testNoneEncoding() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testNoneEncoding()");

        // vars
        int size = DataBlockUtil.ONE_MB;
        String title = "title";
        String description = "descrption";
        X509Certificate certificate = DevUtil.getDevAuthority();
        PrivateKey privateKey = DevUtil.getDevPrivateKey();
        License license = null;
        String passphrase = null;
        boolean compress = false;
        boolean explode = false;
        File upload = null;
        String HOST1 = "server1.com";

        // set up a dummy network
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            Set<String> hosts = new HashSet<String>();
            hosts.add(HOST1);

            upload = TempFileUtil.createTempFileWithName("name.file");
            DevUtil.createTestFile(upload, size);

            // upload
            AddFileToolReport uploadReport = testUpload(hosts, upload, title, description, certificate, privateKey, license, passphrase, compress, explode);

            // verify
            assertTrue(uploadReport.isFinished());
            assertFalse(uploadReport.isFailed());
            if (uploadReport.getHash() == null) {
                fail("Hash is null.");
            }
            assertEquals(DataBlockUtil.ONE_MB, uploadReport.getBytesUploaded());
            GetFileTool gft = new GetFileTool();
            gft.setHash(uploadReport.getHash());
            assertEquals(1, gft.getMetaData().getParts().size());

            // try to download
            verifyUpload(upload, uploadReport.getHash(), hosts, title, description, license, passphrase, compress, explode, upload);
            verifyProjectFile(upload, uploadReport.getHash(), hosts, title, description, license, passphrase, explode, upload);
        } finally {
            testNetwork.stop();
        }
    }

    public void testFileBig() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testFileBig()");
        testFile(DataBlockUtil.ONE_MB * 10, "title", "descrption", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, true, false, null);
    }

    public void testExplodeGZIP() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testExplodeGZIP()");
        File upload = DevUtil.createTestFile(1, DataBlockUtil.ONE_MB / 2);
        File compressedUpload = CompressionUtil.gzipCompress(upload);
        File renameTo = new File(TempFileUtil.createTemporaryDirectory(), compressedUpload.getName());
        IOUtil.renameFallbackCopy(compressedUpload, renameTo);
        testFile(renameTo, "title", "description", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, false, true, upload);
    }

    public void testExplodeBZIP2() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testExplodeBZIP2()");
        File upload = DevUtil.createTestFile(1, DataBlockUtil.ONE_MB / 2);
        File compressedUpload = CompressionUtil.bzip2Compress(upload);
        File renameTo = new File(TempFileUtil.createTemporaryDirectory(), compressedUpload.getName());
        IOUtil.renameFallbackCopy(compressedUpload, renameTo);
        testFile(renameTo, "title", "description", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, false, true, upload);
    }

    public void testExplodeLZMA() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testExplodeLZMA()");
        File upload = DevUtil.createTestFile(1, DataBlockUtil.ONE_MB / 2);
        File compressedUpload = CompressionUtil.lzmaCompress(upload);
        File renameTo = new File(TempFileUtil.createTemporaryDirectory(), compressedUpload.getName());
        IOUtil.renameFallbackCopy(compressedUpload, renameTo);
        testFile(renameTo, "title", "description", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, false, true, upload);
    }

    public void testDirectoryOne() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testDirectoryOne()");
        testDirectory(1, "title", "description", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, true, false);
    }

    public void testDirectorySmall() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testDirectorySmall()");
        testDirectory(5, "title", "descrption", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, true, false);
    }

//    public void testDirectoryBig() throws Exception {
//        TestUtil.printTitle("AddFileToolTest:testDirectoryBig()");
//        testDirectory(100, "title", "descrption", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, null, true, false);
//    }
//
    public void testDirectoryEncrypted() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testDirectoryEncrypted()");
        testDirectory(5, "title", "descrption", DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), null, "passphrase", true, false);
    }

    public void testDirectory(int fileCount, String title, String description, X509Certificate certificate, PrivateKey privateKey, License license, String passphrase, boolean compress, boolean explode) throws Exception {
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();
            Set<String> hosts = new HashSet<String>();
            hosts.add(HOST1);

            File upload = DevUtil.createTestProject(fileCount, 1, DataBlockUtil.ONE_MB * 5);

            // upload
            AddFileToolReport report = testUpload(hosts, upload, title, description, certificate, privateKey, license, passphrase, compress, explode);

            // verify
            assertTrue(report.isFinished());
            assertFalse(report.isFailed());
            if (report.getHash() == null) {
                fail("Hash is null.");
            }

            // try to download
            verifyUpload(upload, report.getHash(), hosts, title, description, license, passphrase, compress, explode, upload);
            verifyProjectFile(upload, report.getHash(), hosts, title, description, license, passphrase, explode, upload);
        } finally {
            testNetwork.stop();
        }
    }

    public void testFileNetwork() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testFileNetwork()");

        String HOST1 = "server1.com";
        String HOST2 = "server2.com";
        String HOST3 = "server3.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            File upload = TempFileUtil.createTempFileWithName("name.file");
            DevUtil.createTestFile(upload, DataBlockUtil.ONE_MB / 2);

            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.setUseUnspecifiedServers(false);
            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));

            // variables
            aft.setTitle("title");
            aft.setDescription("description");
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(upload);

            // perform upload
            AddFileToolReport uploadReport = aft.execute();

            // validate
            assertFalse(uploadReport.isFailed());

            // should all have the
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.addServerToUse(HOST2);
            gft.addServerToUse(HOST3);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(uploadReport.getHash());
            MetaData metaData = gft.getMetaData();
            for (TestServerConfiguration testServer : testNetwork.getServerConfigs()) {
                IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(testServer.ostensibleHost), uploadReport.getHash());
                for (BigHash chunkHash : metaData.getParts()) {
                    IOUtil.hasData(testNetwork.getFlatFileTrancheServer(testServer.ostensibleHost), chunkHash);
                }
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testDirectoryNetwork() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testDirectoryNetwork()");

        String HOST1 = "server1.com";
        String HOST2 = "server2.com";
        String HOST3 = "server3.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            int fileCount = 20;
            File upload = DevUtil.createTestProject(fileCount, 1, DataBlockUtil.ONE_MB * 5);

            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.setUseUnspecifiedServers(false);
            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));

            // variables
            aft.setTitle("title");
            aft.setDescription("description");
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(upload);

            // perform upload
            AddFileToolReport uploadReport = aft.execute();

            // validate
            assertFalse(uploadReport.isFailed());

            // should all have the
            GetFileTool gft = new GetFileTool();
            gft.addServerToUse(HOST1);
            gft.addServerToUse(HOST2);
            gft.addServerToUse(HOST3);
            gft.setUseUnspecifiedServers(false);
            gft.setHash(uploadReport.getHash());
            MetaData metaData = gft.getMetaData();
            ProjectFile projectFile = gft.getProjectFile();
            for (TestServerConfiguration testServer : testNetwork.getServerConfigs()) {
                IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(testServer.ostensibleHost), uploadReport.getHash());
                for (BigHash chunkHash : metaData.getParts()) {
                    IOUtil.hasData(testNetwork.getFlatFileTrancheServer(testServer.ostensibleHost), chunkHash);
                }
                for (ProjectFilePart pfp : projectFile.getParts()) {
                    IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(testServer.ostensibleHost), pfp.getHash());
                    MetaData partMetaData = MetaData.createFromBytes(((byte[][]) IOUtil.getMetaData(testNetwork.getFlatFileTrancheServer(testServer.ostensibleHost), pfp.getHash(), false).getReturnValueObject())[0]);
                    for (BigHash chunkHash : partMetaData.getParts()) {
                        IOUtil.hasData(testNetwork.getFlatFileTrancheServer(testServer.ostensibleHost), chunkHash);
                    }
                }
            }
        } finally {
            testNetwork.stop();
        }
    }

    public List<AddFileToolEvent> addEventListener(AddFileTool aft) {
        final ArrayList<AddFileToolEvent> eventsFired = new ArrayList<AddFileToolEvent>();
        aft.addListener(new AddFileToolListener() {

            public void message(String msg) {
            }

            public void startedData(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void startingData(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void tryingData(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void uploadedData(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void finishedData(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void failedData(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                eventsFired.add(event);
            }

            public void startedMetaData(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void tryingMetaData(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void uploadedMetaData(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void failedMetaData(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                eventsFired.add(event);
            }

            public void finishedMetaData(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void startedFile(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void finishedFile(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void failedFile(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                eventsFired.add(event);
            }

            public void startedDirectory(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void finishedDirectory(AddFileToolEvent event) {
                eventsFired.add(event);
            }

            public void failedDirectory(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                eventsFired.add(event);
            }
        });
        return eventsFired;
    }

    public void verifyUpload(File upload, BigHash hash, Collection<String> hosts, String title, String description, License license, String passphrase, boolean compress, boolean explode, File originalUpload) throws Exception {
        File download = TempFileUtil.createTemporaryDirectory();

        GetFileTool gft = new GetFileTool();
        gft.addServersToUse(hosts);
        gft.setUseUnspecifiedServers(false);
        gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
        gft.setHash(hash);
        gft.setContinueOnFailure(false);
        gft.setSaveFile(download);
        if (passphrase != null) {
            gft.setPassphrase(passphrase);
        }

        // check meta data
        MetaData metaData = gft.getMetaData();
        assertEquals(passphrase != null, metaData.isEncrypted());

        // verify encodings
        int expectedEncodingCount = 1;
        if (passphrase != null) {
            expectedEncodingCount++;
        }
        if (compress) {
            expectedEncodingCount++;
        }
        // none, gzip, aes
        assertEquals(expectedEncodingCount, metaData.getEncodings().size());
        Iterator<FileEncoding> encodingsIterator = metaData.getEncodings().iterator();
        assertEquals(FileEncoding.NONE, encodingsIterator.next().getName());
        if (compress) {
            assertEquals(FileEncoding.GZIP, encodingsIterator.next().getName());
        }
        if (passphrase != null) {
            assertEquals(FileEncoding.AES, encodingsIterator.next().getName());
        }

        if (upload.isDirectory() || license != null) {
            assertTrue(metaData.isProjectFile());
            assertEquals(ProjectFile.DEFAULT_PROJECT_FILE_NAME, metaData.getName());

            GetFileToolReport report = gft.getDirectory();
            // verify download
            assertTrue(report.isFinished());
            assertFalse(report.isFailed());

            if (!explode) {
                System.out.println("Printing uploaded directory structure.");
                Text.printRecursiveDirectoryStructure(upload);
                System.out.println("");
                System.out.println("Printing downloaded directory structure.");
                Text.printRecursiveDirectoryStructure(download);
                TestUtil.assertDirectoriesEquivalent(upload, new File(download, upload.getName()));
            } else {
                System.out.println("Printing uploaded directory structure.");
                Text.printRecursiveDirectoryStructure(originalUpload);
                System.out.println("");
                System.out.println("Printing downloaded directory structure.");
                Text.printRecursiveDirectoryStructure(new File(download, upload.getName()).listFiles()[0]);
                TestUtil.assertDirectoriesEquivalent(originalUpload, new File(download, upload.getName()).listFiles()[0]);
            }
        } else {
            if (explode) {
                if (upload.getName().toLowerCase().endsWith(".gzip")) {
                    assertEquals(upload.getName().substring(0, upload.getName().length() - 5), metaData.getName());
                }
            } else {
                assertEquals(upload.getName(), metaData.getName());
            }

            GetFileToolReport report = gft.getFile();
            // verify download
            assertTrue(report.isFinished());
            assertFalse(report.isFailed());

            if (!explode) {
                TestUtil.assertFilesAreEqual(upload, new File(download, upload.getName()));
            } else {
                String name = upload.getName();
                name = name.replace(".gzip", "");
                name = name.replace(".tgz", "");
                name = name.replace(".lzma", "");
                name = name.replace(".bzip2", "");
                name = name.replace(".tar", "");
                System.out.println("Checking " + new File(download, name).getAbsolutePath() + " against " + originalUpload.getAbsolutePath());
                TestUtil.assertFilesAreEquivalent(originalUpload, new File(download, name));
            }
        }
    }

    public void verifyProjectFile(File upload, BigHash hash, Collection<String> hosts, String title, String description, License license, String passphrase, boolean explode, File originalUpload) throws Exception {
        GetFileTool gft = new GetFileTool();
        gft.addServersToUse(hosts);
        gft.setUseUnspecifiedServers(false);
        gft.addListener(new CommandLineGetFileToolListener(gft, System.out));
        gft.setHash(hash);
        gft.setContinueOnFailure(false);
        if (passphrase != null) {
            gft.setPassphrase(passphrase);
        }

        ProjectFile projectFile = null;
        if (upload.isDirectory() || license != null) {
            projectFile = gft.getProjectFile();
            assertEquals(title, projectFile.getName());
            assertEquals(description, projectFile.getDescription());
            if (license == null) {
                assertNull(projectFile.getLicenseHash());
            } else {
                if (projectFile.getLicenseHash() == null) {
                    fail("License is null.");
                }
            }

            // correct file count
            int expectedFileCount = 0;
            if (upload.isDirectory()) {
                LinkedList<File> stack = new LinkedList<File>();
                if (explode) {
                    stack.add(originalUpload);
                } else {
                    stack.add(upload);
                }
                while (!stack.isEmpty()) {
                    File f = stack.removeFirst();
                    if (f.isDirectory()) {
                        for (File ff : f.listFiles()) {
                            // add to the front to make this depth-first/memory efficient
                            stack.addFirst(ff);
                        }
                    } else {
                        expectedFileCount++;
                    }
                }
            } else {
                expectedFileCount = 2;
            }
            assertEquals(expectedFileCount, projectFile.getParts().size());
        } else {
            // should fail
            boolean exceptionThrown = false;
            try {
                gft.getProjectFile();
            } catch (Exception e) {
                exceptionThrown = true;
            }
            if (!exceptionThrown) {
                fail("No exception was thrown.");
            }
        }
    }

    public AddFileToolReport testUpload(Collection<String> hosts, File upload, String title, String description, X509Certificate certificate, PrivateKey privateKey, License license, String passphrase, boolean compress, boolean explode) throws Exception {
        // set up add file tool
        AddFileTool aft = new AddFileTool();
        aft.addServersToUse(hosts);
        aft.setUseUnspecifiedServers(false);
        aft.addListener(new CommandLineAddFileToolListener(aft, System.out));

        // variables
        aft.setTitle(title);
        aft.setDescription(description);
        aft.setUserCertificate(certificate);
        aft.setUserPrivateKey(privateKey);
        aft.setLicense(license);
        aft.setFile(upload);
        if (passphrase != null) {
            aft.setPassphrase(passphrase);
        }
        aft.setCompress(compress);
        aft.setExplodeBeforeUpload(explode);

        // perform upload
        return aft.execute();
    }

    private Set<HashSpan>[] getThreeEvenlySplitHashSpanSets() throws Exception {
        Set<HashSpan>[] sets = new Set[3];
        sets[0] = new HashSet();
        sets[1] = new HashSet();
        sets[2] = new HashSet();

        final byte[] firstBytes = new byte[BigHash.HASH_LENGTH];
        for (int i = 0; i < BigHash.HASH_LENGTH; i++) {
            firstBytes[i] = -43;
        }

        final BigHash firstHash = BigHash.createFromBytes(firstBytes);

        final byte[] secondBytes = new byte[BigHash.HASH_LENGTH];
        for (int i = 0; i < BigHash.HASH_LENGTH; i++) {
            secondBytes[i] = +42;
        }

        final BigHash secondHash = BigHash.createFromBytes(secondBytes);

        sets[0].add(new HashSpan(HashSpan.FIRST, firstHash));
        sets[1].add(new HashSpan(firstHash, secondHash));
        sets[2].add(new HashSpan(secondHash, HashSpan.LAST));

        return sets;
    }

    /**
     * <p>Internal method. Used to show that two directories are identical--same name, recursively same files/directories.</p>
     * @param dir1
     * @param dir2
     * @return
     * @throws org.tranche.exceptions.AssertionFailedException
     * @throws java.lang.RuntimeException
     */
    private boolean areDirectoriesSame(File dir1, File dir2) throws AssertionFailedException, RuntimeException {
        if (!dir1.isDirectory()) {
            throw new RuntimeException("Not directory: " + dir1.getAbsolutePath());
        }
        if (!dir2.isDirectory()) {
            throw new RuntimeException("Not directory: " + dir2.getAbsolutePath());
        }

        // Must have same name
        if (!dir1.getName().equals(dir2.getName())) {
            return false;
        } else {

            // Directories should have same length
            if (dir1.list().length != dir2.list().length) {
                return false;
            }

            for (File f1 : dir1.listFiles()) {

                // Find equivalent file in other directory
                File f2 = null;
                for (File f2Candidate : dir2.listFiles()) {
                    if (f2Candidate.getName().equals(f1.getName())) {
                        f2 = f2Candidate;
                        break;
                    }
                }

                if (f2 == null) {
                    return false;
                }

                if (f1.isDirectory()) {
                    if (!f2.isDirectory()) {
                        return false;
                    }

                    if (!areDirectoriesSame(f1, f2)) {
                        return false;
                    }

                } else {

                    if (!f1.isFile()) {
                        throw new AssertionFailedException("File is not a directory, but not a file!?!: " + f1.getAbsolutePath());
                    }

                    if (!f2.isFile()) {
                        return false;
                    }

                    if (f1.length() != f2.length()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public AddFileToolReport testFile(int size, String title, String description, X509Certificate certificate, PrivateKey privateKey, License license, String passphrase, boolean compress, boolean explode, File originalUpload) throws Exception {
        File upload = TempFileUtil.createTempFileWithName("name.file");
        DevUtil.createTestFile(upload, size);
        return testFile(upload, title, description, certificate, privateKey, license, passphrase, compress, explode, originalUpload);
    }

    public AddFileToolReport testFile(File upload, String title, String description, X509Certificate certificate, PrivateKey privateKey, License license, String passphrase, boolean compress, boolean explode, File originalUpload) throws Exception {
        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            Set<String> hosts = new HashSet<String>();
            hosts.add(HOST1);

            // upload
            AddFileToolReport uploadReport = testUpload(hosts, upload, title, description, certificate, privateKey, license, passphrase, compress, explode);

            // verify
            assertTrue(uploadReport.isFinished());
            assertFalse(uploadReport.isFailed());
            if (uploadReport.getHash() == null) {
                fail("Hash is null.");
            }

            // try to download
            verifyUpload(upload, uploadReport.getHash(), hosts, title, description, license, passphrase, compress, explode, originalUpload);
            verifyProjectFile(upload, uploadReport.getHash(), hosts, title, description, license, passphrase, explode, originalUpload);
            return uploadReport;
        } finally {
            testNetwork.stop();
        }
    }

    public void testDirectoryEventsNetwork() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testDirectoryEventsNetwork()");

        String HOST1 = "server1.com";
        String HOST2 = "server2.com";
        String HOST3 = "server3.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();
            Set<String> hosts = new HashSet<String>();
            for (TestServerConfiguration testServer : testNetwork.getServerConfigs()) {
                hosts.add(testServer.ostensibleHost);
            }

            testDirectoryEvents(hosts);
        } finally {
            testNetwork.stop();
        }
    }

    public void testDirectoryEvents() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testDirectoryEvents()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();
            Set<String> hosts = new HashSet<String>();
            hosts.add(HOST1);

            testDirectoryEvents(hosts);
        } finally {
            testNetwork.stop();
        }
    }

    public void testDirectoryEvents(Collection<String> hosts) throws Exception {
        int fileCount = 20;
        File upload = DevUtil.createTestProject(20, 1, DataBlockUtil.ONE_MB / 2);

        AddFileTool aft = new AddFileTool();
        aft.addServersToUse(hosts);
        aft.setUseUnspecifiedServers(false);
        aft.addListener(new CommandLineAddFileToolListener(aft, System.out));
        List<AddFileToolEvent> eventsFired = addEventListener(aft);

        // variables
        aft.setTitle("title");
        aft.setDescription("descrption");
        aft.setUserCertificate(DevUtil.getDevAuthority());
        aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
        aft.setFile(upload);

        // perform upload
        aft.execute();

        // validate
        assertFalse(eventsFired.isEmpty());
        // started directory
        assertEquals(AddFileToolEvent.ACTION_STARTED, eventsFired.get(0).getAction());
        assertEquals(AddFileToolEvent.TYPE_DIRECTORY, eventsFired.get(0).getType());
        // count the rest
        int startedMetaData = 0, tryingMetaData = 0, finishedMetaData = 0, startedChunk = 0, tryingChunk = 0, finishedChunk = 0, startedFile = 0, finishedFile = 0;
        for (int i = 1; i < eventsFired.size() - 1; i++) {
            AddFileToolEvent event = eventsFired.get(i);
            if (event.getType() == AddFileToolEvent.TYPE_METADATA) {
                if (event.getAction() == AddFileToolEvent.ACTION_STARTED) {
                    startedMetaData++;
                } else if (event.getAction() == AddFileToolEvent.ACTION_TRYING) {
                    tryingMetaData++;
                } else if (event.getAction() == AddFileToolEvent.ACTION_FINISHED) {
                    finishedMetaData++;
                }
            } else if (event.getType() == AddFileToolEvent.TYPE_DATA) {
                if (event.getAction() == AddFileToolEvent.ACTION_STARTED) {
                    startedChunk++;
                } else if (event.getAction() == AddFileToolEvent.ACTION_TRYING) {
                    tryingChunk++;
                } else if (event.getAction() == AddFileToolEvent.ACTION_FINISHED) {
                    finishedChunk++;
                }
            } else if (event.getType() == AddFileToolEvent.TYPE_FILE) {
                if (event.getAction() == AddFileToolEvent.ACTION_STARTED) {
                    startedFile++;
                } else if (event.getAction() == AddFileToolEvent.ACTION_FINISHED) {
                    finishedFile++;
                }
            }
        }
        // +1 is for the project file
        fileCount++;
        assertEquals(fileCount, startedChunk);
        assertEquals(fileCount, tryingChunk);
        assertEquals(fileCount, finishedChunk);
        assertEquals(fileCount, startedMetaData);
        assertEquals(fileCount, tryingMetaData);
        assertEquals(fileCount, finishedMetaData);
        assertEquals(fileCount, startedFile);
        assertEquals(fileCount, finishedFile);
        // finished directory last
        assertEquals(AddFileToolEvent.ACTION_FINISHED, eventsFired.get(eventsFired.size() - 1).getAction());
        assertEquals(AddFileToolEvent.TYPE_DIRECTORY, eventsFired.get(eventsFired.size() - 1).getType());
    }

    public void testFileEvents() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testFileEvents()");

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            // set up network
            testNetwork.start();
            Set<String> hosts = new HashSet<String>();
            hosts.add(HOST1);

            testFileEvents(hosts);
        } finally {
            testNetwork.stop();
        }
    }

    public void testFileEventsNetwork() throws Exception {
        TestUtil.printTitle("AddFileToolTest:testFileEventsNetwork()");

        String HOST1 = "server1.com";
        String HOST2 = "server2.com";
        String HOST3 = "server3.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();
            Set<String> hosts = new HashSet<String>();
            for (TestServerConfiguration testServer : testNetwork.getServerConfigs()) {
                hosts.add(testServer.ostensibleHost);
            }

            testFileEvents(hosts);
        } finally {
            testNetwork.stop();
        }
    }

    public void testFileEvents(Collection<String> hosts) throws Exception {
        File upload = TempFileUtil.createTempFileWithName("name.file");
        DevUtil.createTestFile(upload, DataBlockUtil.ONE_MB / 2);

        AddFileTool aft = new AddFileTool();
        aft.addServersToUse(hosts);
        aft.setUseUnspecifiedServers(false);
        aft.addListener(new CommandLineAddFileToolListener(aft, System.out));
        List<AddFileToolEvent> eventsFired = addEventListener(aft);

        // variables
        aft.setTitle("title");
        aft.setDescription("descrption");
        aft.setUserCertificate(DevUtil.getDevAuthority());
        aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
        aft.setFile(upload);

        // perform upload
        AddFileToolReport uploadReport = aft.execute();

        // validate
        assertFalse(uploadReport.isFailed());
        assertFalse(eventsFired.isEmpty());
        for (AddFileToolEvent event : eventsFired) {
            System.out.println(event.toString());
        }
        // events
        int count = 0;
        assertEquals(AddFileToolEvent.ACTION_STARTED, eventsFired.get(count).getAction());
        assertEquals(AddFileToolEvent.TYPE_FILE, eventsFired.get(count++).getType());
        assertEquals(AddFileToolEvent.ACTION_STARTED, eventsFired.get(count).getAction());
        assertEquals(AddFileToolEvent.TYPE_DATA, eventsFired.get(count++).getType());
        assertEquals(AddFileToolEvent.ACTION_TRYING, eventsFired.get(count).getAction());
        assertEquals(AddFileToolEvent.TYPE_DATA, eventsFired.get(count++).getType());
        for (int i = 0; i < hosts.size(); i++) {
            assertEquals(AddFileToolEvent.ACTION_UPLOADED, eventsFired.get(count).getAction());
            assertEquals(AddFileToolEvent.TYPE_DATA, eventsFired.get(count++).getType());
        }
        assertEquals(AddFileToolEvent.ACTION_FINISHED, eventsFired.get(count).getAction());
        assertEquals(AddFileToolEvent.TYPE_DATA, eventsFired.get(count++).getType());
        assertEquals(AddFileToolEvent.ACTION_STARTED, eventsFired.get(count).getAction());
        assertEquals(AddFileToolEvent.TYPE_METADATA, eventsFired.get(count++).getType());
        assertEquals(AddFileToolEvent.ACTION_TRYING, eventsFired.get(count).getAction());
        assertEquals(AddFileToolEvent.TYPE_METADATA, eventsFired.get(count++).getType());
        for (int i = 0; i < hosts.size(); i++) {
            assertEquals(AddFileToolEvent.ACTION_UPLOADED, eventsFired.get(count).getAction());
            assertEquals(AddFileToolEvent.TYPE_METADATA, eventsFired.get(count++).getType());
        }
        assertEquals(AddFileToolEvent.ACTION_FINISHED, eventsFired.get(count).getAction());
        assertEquals(AddFileToolEvent.TYPE_METADATA, eventsFired.get(count++).getType());
        assertEquals(AddFileToolEvent.ACTION_FINISHED, eventsFired.get(count).getAction());
        assertEquals(AddFileToolEvent.TYPE_FILE, eventsFired.get(count++).getType());
    }
}
