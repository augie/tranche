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
package org.tranche.tasks;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.span.HashSpan;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataUtil;
import org.tranche.network.ConnectionUtil;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.util.AssertionUtil;
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
public class TaskUtilTest extends TrancheTestCase {

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testGetConfigurationByHost() throws Exception {
        TestUtil.printTitle("TaskUtilTest:testGetConfigurationByHost()");

        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        final int PORT1 = 1500;
        final int PORT2 = 1501;
        final int PORT3 = 1503;

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, PORT1, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, PORT2, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, PORT3, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

        final String URL1 = IOUtil.createURL(HOST1, PORT1, false);
        final String URL2 = IOUtil.createURL(HOST2, PORT2, false);
        final String URL3 = IOUtil.createURL(HOST3, PORT3, false);

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

            Configuration c1 = TaskUtil.getConfiguration(HOST1);
            assertNotNull(c1);
            assertEquals("Expecting proper URL from configuration.", URL1, c1.getValue(ConfigKeys.URL));

            Configuration c2 = TaskUtil.getConfiguration(HOST2);
            assertNotNull(c2);
            assertEquals("Expecting proper URL from configuration.", URL2, c2.getValue(ConfigKeys.URL));

            Configuration c3 = TaskUtil.getConfiguration(HOST3);
            assertNotNull(c3);
            assertEquals("Expecting proper URL from configuration.", URL3, c3.getValue(ConfigKeys.URL));
        } finally {
            testNetwork.stop();
        }
    }

    public void testDeleteMetaDataMultipleUploaders() throws Exception {
        TestUtil.printTitle("TaskUtilTest:testDeleteMetaDataMultipleUploaders()");

        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        final int PORT1 = 1500;
        final int PORT2 = 1501;
        final int PORT3 = 1503;

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, PORT1, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, PORT2, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, PORT3, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

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

            final int uploadersCount = RandomUtil.getInt(3) + 2;
            final byte[] originalMetaDataBytes = DevUtil.createRandomMetaDataChunk(uploadersCount);
            BigHash hash = DevUtil.getRandomBigHash();

            final MetaData originalMetaData = generateFromBytes(originalMetaDataBytes);

            String[] hostsArr = {HOST1, HOST2, HOST3};
            PropagationReturnWrapper prw = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), true, hash, originalMetaDataBytes, hostsArr);
            assertFalse("Shouldn't have any problems.", prw.isAnyErrors());

            byte[] metaBytes1 = ffts1.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesSame(originalMetaDataBytes, metaBytes1);

            byte[] metaBytes2 = ffts2.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesSame(originalMetaDataBytes, metaBytes2);

            byte[] metaBytes3 = ffts3.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesSame(originalMetaDataBytes, metaBytes3);

            // Pick a random uploader to delete
            int uploaderToRemove = RandomUtil.getInt(uploadersCount);
            originalMetaData.selectUploader(uploaderToRemove);

            String uploaderName = originalMetaData.getSignature().getUserName();
            assertNotNull(uploaderName);

            TaskUtil.deleteMetaData(hash, uploaderName, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), originalMetaData.getTimestampUploaded(), originalMetaData.getRelativePathInDataSet());

            metaBytes1 = ffts1.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesDifferent(originalMetaDataBytes, metaBytes1);
            MetaData metaData1 = generateFromBytes(metaBytes1);
            assertNotNull(metaData1);
            assertEquals("Expecting certain number of uploaders.", uploadersCount - 1, metaData1.getUploaderCount());
            for (int uploader = 0; uploader < metaData1.getUploaderCount(); uploader++) {
                metaData1.selectUploader(uploader);
                assertNotSame(uploaderName, metaData1.getSignature().getUserName());
            }

            metaBytes2 = ffts2.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesDifferent(originalMetaDataBytes, metaBytes2);
            MetaData metaData2 = generateFromBytes(metaBytes2);
            assertNotNull(metaData2);
            assertEquals("Expecting certain number of uploaders.", uploadersCount - 1, metaData2.getUploaderCount());
            for (int uploader = 0; uploader < metaData2.getUploaderCount(); uploader++) {
                metaData2.selectUploader(uploader);
                assertNotSame(uploaderName, metaData2.getSignature().getUserName());
            }

            metaBytes3 = ffts3.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesDifferent(originalMetaDataBytes, metaBytes3);
            MetaData metaData3 = generateFromBytes(metaBytes3);
            assertNotNull(metaData3);
            assertEquals("Expecting certain number of uploaders.", uploadersCount - 1, metaData3.getUploaderCount());
            for (int uploader = 0; uploader < metaData3.getUploaderCount(); uploader++) {
                metaData3.selectUploader(uploader);
                assertNotSame(uploaderName, metaData3.getSignature().getUserName());
            }

        } finally {
            testNetwork.stop();
        }
    }

    public void testDeleteMetaDataSingleUploader() throws Exception {
        TestUtil.printTitle("TaskUtilTest:testDeleteMetaDataSingleUploader");

        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        final int PORT1 = 1500;
        final int PORT2 = 1501;
        final int PORT3 = 1503;

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, PORT1, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, PORT2, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, PORT3, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

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

            final byte[] originalMetaDataBytes = DevUtil.createRandomMetaDataChunk(1);
            BigHash hash = DevUtil.getRandomBigHash();

            final MetaData originalMetaData = generateFromBytes(originalMetaDataBytes);
            assertEquals("Expecting on uploader.", 1, originalMetaData.getUploaderCount());

            String[] hostsArr = {HOST1, HOST2, HOST3};
            PropagationReturnWrapper prw = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), true, hash, originalMetaDataBytes, hostsArr);
            assertFalse("Shouldn't have any problems.", prw.isAnyErrors());

            byte[] metaBytes1 = ffts1.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesSame(originalMetaDataBytes, metaBytes1);

            byte[] metaBytes2 = ffts2.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesSame(originalMetaDataBytes, metaBytes2);

            byte[] metaBytes3 = ffts3.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesSame(originalMetaDataBytes, metaBytes3);

            // Pick a random uploader to delete
            originalMetaData.selectUploader(0);

            String uploaderName = originalMetaData.getSignature().getUserName();
            assertNotNull(uploaderName);

            TaskUtil.deleteMetaData(hash, uploaderName, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), originalMetaData.getTimestampUploaded(), originalMetaData.getRelativePathInDataSet());

            try {
                metaBytes1 = ffts1.getDataBlockUtil().getMetaData(hash);
                fail("Should have thrown exception; meta data should be deleted.");
            } catch (FileNotFoundException fnfe) {
                // expected
            }

            try {
                metaBytes2 = ffts2.getDataBlockUtil().getMetaData(hash);
                fail("Should have thrown exception; meta data should be deleted.");
            } catch (FileNotFoundException fnfe) {
                // expected
            }

            try {
                metaBytes3 = ffts3.getDataBlockUtil().getMetaData(hash);
                fail("Should have thrown exception; meta data should be deleted.");
            } catch (FileNotFoundException fnfe) {
                // expected
            }


        } finally {
            testNetwork.stop();
        }
    }

    public void testPublishPassphraseMultipleUploaders() throws Exception {
        TestUtil.printTitle("TaskUtilTest:testDeleteMetaDataMultipleUploaders()");

        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        final int PORT1 = 1500;
        final int PORT2 = 1501;
        final int PORT3 = 1503;

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, PORT1, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, PORT2, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, PORT3, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

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

            final int uploadersCount = RandomUtil.getInt(3) + 2;
            final byte[] originalMetaDataBytes = DevUtil.createRandomMetaDataChunk(uploadersCount, true, true);
            BigHash hash = DevUtil.getRandomBigHash();

            final MetaData originalMetaData = generateFromBytes(originalMetaDataBytes);

            for (int uploader = 0; uploader < uploadersCount; uploader++) {
                originalMetaData.selectUploader(uploader);
                assertNull(originalMetaData.getPublicPassphrase());
            }

            String[] hostsArr = {HOST1, HOST2, HOST3};
            PropagationReturnWrapper prw = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), true, hash, originalMetaDataBytes, hostsArr);
            assertFalse("Shouldn't have any problems.", prw.isAnyErrors());

            byte[] metaBytes1 = ffts1.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesSame(originalMetaDataBytes, metaBytes1);

            byte[] metaBytes2 = ffts2.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesSame(originalMetaDataBytes, metaBytes2);

            byte[] metaBytes3 = ffts3.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesSame(originalMetaDataBytes, metaBytes3);

            // Pick a random uploader to publish passphrase
            int uploaderToPublish = RandomUtil.getInt(uploadersCount);
            originalMetaData.selectUploader(uploaderToPublish);
            originalMetaData.setPublicPassphrase("secret");

            final String uploaderName = originalMetaData.getSignature().getUserName();
            final String path = originalMetaData.getRelativePathInDataSet();
            final long timestamp = originalMetaData.getTimestampUploaded();

            assertNotNull(uploaderName);

            for (int uploader = 0; uploader < uploadersCount; uploader++) {
                originalMetaData.selectUploader(uploader);
                final String nextUploaderName = originalMetaData.getSignature().getUserName();
                final String nextPath = originalMetaData.getRelativePathInDataSet();
                final long nextTimestamp = originalMetaData.getTimestampUploaded();

                boolean isSameName = uploaderName.equals(nextUploaderName);
                boolean isSamePath = (nextPath == null && path == null) || (nextPath != null && path != null && nextPath.equals(path));
                boolean isSameTimestamp = nextTimestamp == timestamp;

                if (isSameName && isSamePath && isSameTimestamp) {
                    assertEquals("Expecting public passphrase set correctly.", "secret", originalMetaData.getPublicPassphrase());
                } else {
                    assertNull(originalMetaData.getPublicPassphrase());
                }
            }

            TaskUtil.publishPassphrase(hash, "secret", uploaderName, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), timestamp, path);

            metaBytes1 = ffts1.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesDifferent(originalMetaDataBytes, metaBytes1);
            MetaData metaData1 = generateFromBytes(metaBytes1);
            assertNotNull(metaData1);
            assertEquals("Expecting certain number of uploaders.", uploadersCount, metaData1.getUploaderCount());
            for (int uploader = 0; uploader < uploadersCount; uploader++) {
                MetaData md = metaData1;
                md.selectUploader(uploader);
                final String nextUploaderName = md.getSignature().getUserName();
                final String nextPath = md.getRelativePathInDataSet();
                final long nextTimestamp = md.getTimestampUploaded();

                boolean isSameName = uploaderName.equals(nextUploaderName);
                boolean isSamePath = (nextPath == null && path == null) || (nextPath != null && path != null && nextPath.equals(path));
                boolean isSameTimestamp = (nextTimestamp == timestamp);

                if (isSameName && isSamePath && isSameTimestamp) {
                    assertEquals("Expecting public passphrase set correctly.", "secret", md.getPublicPassphrase());
                } else {
                    assertNull("Should be null, but found: " + md.getPublicPassphrase(), md.getPublicPassphrase());
                }
            }

            metaBytes2 = ffts2.getDataBlockUtil().getMetaData(hash);

            metaBytes3 = ffts3.getDataBlockUtil().getMetaData(hash);

        } finally {
            testNetwork.stop();
        }
    }

    public void testPublishPassphraseSingleUploader() throws Exception {
        TestUtil.printTitle("TaskUtilTest:testPublishPassphraseSingleUploader()");

        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        final int PORT1 = 1500;
        final int PORT2 = 1501;
        final int PORT3 = 1503;

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, PORT1, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, PORT2, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, PORT3, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

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

            final int uploadersCount = 1;
            final byte[] originalMetaDataBytes = DevUtil.createRandomMetaDataChunk(uploadersCount, true, true);
            BigHash hash = DevUtil.getRandomBigHash();

            final MetaData originalMetaData = generateFromBytes(originalMetaDataBytes);

            for (int uploader = 0; uploader < uploadersCount; uploader++) {
                originalMetaData.selectUploader(uploader);
                assertNull(originalMetaData.getPublicPassphrase());
            }

            String[] hostsArr = {HOST1, HOST2, HOST3};
            PropagationReturnWrapper prw = IOUtil.setMetaData(ts1, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), true, hash, originalMetaDataBytes, hostsArr);
            assertFalse("Shouldn't have any problems.", prw.isAnyErrors());

            byte[] metaBytes1 = ffts1.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesSame(originalMetaDataBytes, metaBytes1);

            byte[] metaBytes2 = ffts2.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesSame(originalMetaDataBytes, metaBytes2);

            byte[] metaBytes3 = ffts3.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesSame(originalMetaDataBytes, metaBytes3);

            // Pick a random uploader to publish passphrase
            int uploaderToPublish = RandomUtil.getInt(uploadersCount);
            originalMetaData.selectUploader(uploaderToPublish);
            originalMetaData.setPublicPassphrase("secret");

            final String uploaderName = originalMetaData.getSignature().getUserName();
            final String path = originalMetaData.getRelativePathInDataSet();
            final long timestamp = originalMetaData.getTimestampUploaded();

            assertNotNull(uploaderName);

            for (int uploader = 0; uploader < uploadersCount; uploader++) {
                originalMetaData.selectUploader(uploader);
                final String nextUploaderName = originalMetaData.getSignature().getUserName();
                final String nextPath = originalMetaData.getRelativePathInDataSet();
                final long nextTimestamp = originalMetaData.getTimestampUploaded();

                boolean isSameName = uploaderName.equals(nextUploaderName);
                boolean isSamePath = (nextPath == null && path == null) || (nextPath != null && path != null && nextPath.equals(path));
                boolean isSameTimestamp = nextTimestamp == timestamp;

                if (isSameName && isSamePath && isSameTimestamp) {
                    assertEquals("Expecting public passphrase set correctly.", "secret", originalMetaData.getPublicPassphrase());
                } else {
                    assertNull(originalMetaData.getPublicPassphrase());
                }
            }

            TaskUtil.publishPassphrase(hash, "secret", uploaderName, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), timestamp, path);

            metaBytes1 = ffts1.getDataBlockUtil().getMetaData(hash);
            AssertionUtil.assertBytesDifferent(originalMetaDataBytes, metaBytes1);
            MetaData metaData1 = generateFromBytes(metaBytes1);
            assertNotNull(metaData1);
            assertEquals("Expecting certain number of uploaders.", uploadersCount, metaData1.getUploaderCount());
            for (int uploader = 0; uploader < uploadersCount; uploader++) {
                MetaData md = metaData1;
                md.selectUploader(uploader);
                final String nextUploaderName = md.getSignature().getUserName();
                final String nextPath = md.getRelativePathInDataSet();
                final long nextTimestamp = md.getTimestampUploaded();

                boolean isSameName = uploaderName.equals(nextUploaderName);
                boolean isSamePath = (nextPath == null && path == null) || (nextPath != null && path != null && nextPath.equals(path));
                boolean isSameTimestamp = (nextTimestamp == timestamp);

                if (isSameName && isSamePath && isSameTimestamp) {
                    assertEquals("Expecting public passphrase set correctly.", "secret", md.getPublicPassphrase());
                } else {
                    assertNull("Should be null, but found: " + md.getPublicPassphrase(), md.getPublicPassphrase());
                }
            }

            metaBytes2 = ffts2.getDataBlockUtil().getMetaData(hash);

            metaBytes3 = ffts3.getDataBlockUtil().getMetaData(hash);

        } finally {
            testNetwork.stop();
        }
    }

    /**
     * 
     * @param metaDataBytes
     * @return
     * @throws java.lang.Exception
     */
    private MetaData generateFromBytes(byte[] metaDataBytes) throws Exception {
        ByteArrayInputStream bais = null;

        try {
            bais = new ByteArrayInputStream(metaDataBytes);
            return MetaDataUtil.read(bais);
        } finally {
            IOUtil.safeClose(bais);
        }
    }
}