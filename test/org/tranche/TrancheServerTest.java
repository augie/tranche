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
package org.tranche;

import org.tranche.security.Signature;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.tranche.util.DevUtil;
import org.tranche.util.Utils;
import org.tranche.hash.BigHash;
import org.tranche.configuration.Configuration;
import org.tranche.configuration.ConfigurationUtil;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.flatfile.NonceMap;
import org.tranche.hash.span.HashSpan;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataUtil;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.flatfile.ServerConfiguration;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.meta.MetaDataUtilTest;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTable;
import org.tranche.network.StatusTableRow;
import org.tranche.server.GetNetworkStatusItem;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.util.TempFileUtil;
import org.tranche.users.User;
import org.tranche.users.UserZipFile;
import org.tranche.commons.RandomUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @auther Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class TrancheServerTest extends TrancheTestCase {

    /**
     * Keep JUnit happy. This class is simply a stub that is used by FFTS and RTS tests for similar functionality.
     * @throws java.lang.Exception
     */
    public void testNothing() throws Exception {
    }

    public static final void testDeleteData(final TrancheServer dfs, X509Certificate devAuth, PrivateKey key) throws Exception {
        // get the initial number of entries in the repos
        BigHash[] hashes = dfs.getDataHashes(BigInteger.ZERO, BigInteger.valueOf(1000));

        // try deleting all the files that dev.protomecommons.org has added.
        testSetData(dfs, devAuth, key);

        // get the initial number of entries in the repos
        BigHash[] hashes2 = dfs.getDataHashes(BigInteger.ZERO, BigInteger.valueOf(1000));
        assertEquals("Should only be one more entry than initial repository.", hashes.length, hashes2.length - 1);

        // count how many delets
        int deleteCount = 0;

        // find the smallest entry part
        for (BigHash hash : hashes2) {
            // inc count
            deleteCount++;
            PropagationReturnWrapper wrapper = IOUtil.deleteData(dfs, devAuth, key, hash);
            assertFalse(wrapper.isAnyErrors());
        }

        // check that at least one thing was deleted
        if (deleteCount < 1) {
            fail("At least one item must be deleted.");
        }

        // get the initial number of entries in the repos
        BigHash[] hashesFinal = dfs.getDataHashes(BigInteger.ZERO, BigInteger.valueOf(1000));
        assertEquals("After deleting there should be no entries in the DFS.", 0, hashesFinal.length);
    }

    public static final void testDeleteMetaData(final TrancheServer dfs, X509Certificate devAuth, PrivateKey key) throws Exception {
        // get the initial number of entries in the repos
        BigHash[] hashes = dfs.getMetaDataHashes(BigInteger.ZERO, BigInteger.valueOf(1000));

        // try deleting all the files that dev.protomecommons.org has added.
        testSetMetaData(dfs, devAuth, key);

        // get the initial number of entries in the repos
        BigHash[] hashes2 = dfs.getMetaDataHashes(BigInteger.ZERO, BigInteger.valueOf(1000));
        assertEquals("Should only be one more entry than initial repository.", hashes.length, hashes2.length - 1);

        // find the smallest entry part
        int deleteCount = 0;
        for (BigHash hash : hashes2) {
            // inc count
            deleteCount++;
            PropagationReturnWrapper wrapper = IOUtil.deleteMetaData(dfs, devAuth, key, hash);
            assertFalse(wrapper.isAnyErrors());
        }

        // check that at least one thing was deleted
        assertFalse("At least one item must be deleted.", deleteCount == 0);

        // get the initial number of entries in the repos
        BigHash[] hashesFinal = dfs.getMetaDataHashes(BigInteger.ZERO, BigInteger.valueOf(1000));
        assertEquals("After deleting there should be no entries in the DFS.", 0, hashesFinal.length);
    }

    public static final void testDeleteMetaDataUploader(TrancheServer dfs, X509Certificate auth, PrivateKey key) throws Exception {
        // make up some meta-data and random data
        byte[] bytes = Utils.makeRandomData(DataBlockUtil.ONE_MB);
        BigHash hash = new BigHash(bytes);
        MetaData metaData = DevUtil.createRandomMetaData(2);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MetaDataUtil.write(metaData, baos);
        byte[] metaDataBytes = baos.toByteArray();

        // make sure of the number of uploaders
        assertEquals(2, metaData.getUploaderCount());

        // make the uri based on the MD5 hash
        PropagationReturnWrapper setWrapper = IOUtil.setMetaData(dfs, auth, key, false, hash, metaDataBytes);
        assertFalse(setWrapper.isAnyErrors());

        // the data should be on-line
        assertTrue("Meta-data should be on-line.", IOUtil.hasMetaData(dfs, hash));

        // verify that the entry is the same
        byte[] checkBytes = ((byte[][]) IOUtil.getMetaData(dfs, hash, false).getReturnValueObject())[0];
        assertNotNull("Data input stream must not be null!", checkBytes);
        DevUtil.assertBytesMatch(metaDataBytes, checkBytes);

        // delete the individual uploader
        String deletedName = metaData.getSignature().getUserName();
        long deletedTimestamp = metaData.getTimestampUploaded();
        String deletedPath = metaData.getRelativePathInDataSet();
        IOUtil.deleteMetaData(dfs, auth, key, deletedName, deletedTimestamp, deletedPath, hash);

        // the data should still be on-line
        assertTrue("Meta-data should be on-line.", IOUtil.hasMetaData(dfs, hash));

        // download the meta data
        MetaData newMetaData = MetaDataUtil.read(new ByteArrayInputStream(((byte[][]) IOUtil.getMetaData(dfs, hash, false).getReturnValueObject())[0]));

        // make sure of the number of uploaders
        assertEquals(1, newMetaData.getUploaderCount());
        if (newMetaData.getName().equals(deletedName) && newMetaData.getTimestampUploaded() == deletedTimestamp) {
            fail("Did not delete the correct uploader.");
        }
    }

    /**
     * This test is a general example of how a file can be pushed to a node. Given an instance of DistributedFileSystem, a recognized authority, and that authority's private key, any code can invoke this method to push data on to a node.
     */
    public static final void testSetData(final TrancheServer dfs, X509Certificate auth, PrivateKey key) throws Exception {
        // random data
        byte[] bytes = Utils.makeRandomData(DataBlockUtil.ONE_MB);
        BigHash hash = new BigHash(bytes);

        // make the uri based on the MD5 hash
        PropagationReturnWrapper setWrapper = IOUtil.setData(dfs, auth, key, hash, bytes);
        assertFalse(setWrapper.isAnyErrors());

        // the data should be on-line
        assertTrue("Data should be on-line.", IOUtil.hasData(dfs, hash));

        // verify that the entry is the same
        PropagationReturnWrapper getWrapper = IOUtil.getData(dfs, hash, false);
        assertFalse(getWrapper.isAnyErrors());
        assertNotNull(getWrapper.getReturnValueObject());
        DevUtil.assertBytesMatch(bytes, ((byte[][]) getWrapper.getReturnValueObject())[0]);
    }

    /**
     * This test is a general example of how a file can be pushed to a node. Given an instance of DistributedFileSystem, a recognized authority, and that authority's private key, any code can invoke this method to push data on to a node.
     */
    public static final void testSetMetaData(TrancheServer ts, X509Certificate auth, PrivateKey key) throws Exception {
        MetaData md = DevUtil.createRandomMetaData();
        MetaData md2 = DevUtil.createRandomMetaData();
        byte[] bytes = md.toByteArray();
        byte[] bytes2 = md2.toByteArray();
        BigHash hash = DevUtil.getRandomBigHash();

        // add the meta-data (merge)
        PropagationReturnWrapper setWrapper1 = IOUtil.setMetaData(ts, auth, key, true, hash, bytes);
        assertFalse(setWrapper1.isAnyErrors());

        // verify that the entry is the same
        PropagationReturnWrapper getWrapper1 = IOUtil.getMetaData(ts, hash, false);
        assertFalse(getWrapper1.isAnyErrors());
        DevUtil.assertBytesMatch(bytes, ((byte[][]) getWrapper1.getReturnValueObject())[0]);

        // add the meta-data
        PropagationReturnWrapper setWrapper2 = IOUtil.setMetaData(ts, auth, key, false, hash, bytes2);
        assertFalse(setWrapper2.isAnyErrors());

        // verify that the entry is the same
        PropagationReturnWrapper getWrapper2 = IOUtil.getMetaData(ts, hash, false);
        assertFalse(getWrapper2.isAnyErrors());
        DevUtil.assertBytesMatch(bytes2, ((byte[][]) getWrapper2.getReturnValueObject())[0]);

        // merge the meta-data
        PropagationReturnWrapper setWrapper3 = IOUtil.setMetaData(ts, auth, key, true, hash, bytes);
        assertFalse(setWrapper3.isAnyErrors());

        // manual merge
        MetaData mergedMD = new MetaData();
        mergedMD.addUploader(md2.getSignature(), new ArrayList<FileEncoding>(md2.getEncodings()), md2.getProperties(), new ArrayList<MetaDataAnnotation>(md2.getAnnotations()));
        mergedMD.setParts(new ArrayList<BigHash>(md2.getParts()));
        mergedMD.addUploader(md.getSignature(), new ArrayList<FileEncoding>(md.getEncodings()), md.getProperties(), new ArrayList<MetaDataAnnotation>(md.getAnnotations()));
        mergedMD.setParts(new ArrayList<BigHash>(md.getParts()));

        // verify that the entry is the same
        PropagationReturnWrapper getWrapper3 = IOUtil.getMetaData(ts, hash, false);
        assertFalse(getWrapper3.isAnyErrors());
        MetaData returnedMergedMetaData = MetaData.createFromBytes(((byte[][]) getWrapper3.getReturnValueObject())[0]);
        MetaDataUtilTest.checkMetaData(mergedMD, returnedMergedMetaData, false);
    }

    public static final void testSetBigMetaData(TrancheServer ts, X509Certificate auth, PrivateKey key) throws Exception {
        MetaData md = DevUtil.createRandomBigMetaData();
        MetaData md2 = DevUtil.createRandomBigMetaData();
        byte[] bytes = md.toByteArray();
        byte[] bytes2 = md2.toByteArray();
        BigHash hash = DevUtil.getRandomBigHash();

        // add the meta-data (merge)
        PropagationReturnWrapper setWrapper1 = IOUtil.setMetaData(ts, auth, key, true, hash, bytes);
        assertFalse(setWrapper1.isAnyErrors());

        // verify that the entry is the same
        PropagationReturnWrapper getWrapper1 = IOUtil.getMetaData(ts, hash, false);
        assertFalse(getWrapper1.isAnyErrors());
        DevUtil.assertBytesMatch(bytes, ((byte[][]) getWrapper1.getReturnValueObject())[0]);

        // add the meta-data
        PropagationReturnWrapper setWrapper2 = IOUtil.setMetaData(ts, auth, key, false, hash, bytes2);
        assertFalse(setWrapper2.isAnyErrors());

        // verify that the entry is the same
        PropagationReturnWrapper getWrapper2 = IOUtil.getMetaData(ts, hash, false);
        assertFalse(getWrapper2.isAnyErrors());
        DevUtil.assertBytesMatch(bytes2, ((byte[][]) getWrapper2.getReturnValueObject())[0]);

        // merge the meta-data
        PropagationReturnWrapper setWrapper3 = IOUtil.setMetaData(ts, auth, key, true, hash, bytes);
        assertFalse(setWrapper3.isAnyErrors());

        // manual merge
        MetaData mergedMD = new MetaData();
        mergedMD.addUploader(md2.getSignature(), new ArrayList<FileEncoding>(md2.getEncodings()), md2.getProperties(), new ArrayList<MetaDataAnnotation>(md2.getAnnotations()));
        mergedMD.setParts(new ArrayList<BigHash>(md2.getParts()));
        mergedMD.addUploader(md.getSignature(), new ArrayList<FileEncoding>(md.getEncodings()), md.getProperties(), new ArrayList<MetaDataAnnotation>(md.getAnnotations()));
        mergedMD.setParts(new ArrayList<BigHash>(md.getParts()));

        // verify that the entry is the same
        PropagationReturnWrapper getWrapper3 = IOUtil.getMetaData(ts, hash, false);
        assertFalse(getWrapper3.isAnyErrors());
        MetaData returnedMergedMetaData = MetaData.createFromBytes(((byte[][]) getWrapper3.getReturnValueObject())[0]);
        MetaDataUtilTest.checkMetaData(mergedMD, returnedMergedMetaData, false);
    }

    /**
     * This test is a general example of how a file can be pushed to a node. Given an instance of DistributedFileSystem, a recognized authority, and that authority's private key, any code can invoke this method to push data on to a node.
     */
    public static final void testSetConfiguration(final TrancheServer dfs, User user, PrivateKey key) throws Exception {
        // make a configuration
        Configuration config = new Configuration();

        // add a random number of data directory configurations
        for (int i = 0; i < RandomUtil.getInt(5) + 1; i++) {
            config.addDataDirectory(DevUtil.makeNewDataDirectoryConfiguration());
        }

        // set this user to the server
        config.addUser(user);
        // add a random number of users
        for (int i = 0; i < RandomUtil.getInt(20) + 1; i++) {
            config.addUser(DevUtil.makeNewUserWithRandomFlags());
        }

        // add a random number of hash spans
        for (int i = 0; i < RandomUtil.getInt(20) + 1; i++) {
            config.addHashSpan(DevUtil.makeRandomHashSpan());
        }

        // add a random number of sticky projects
        for (int i = 0; i < RandomUtil.getInt(20) + 1; i++) {
            config.addStickyProject(DevUtil.getRandomBigHash(DataBlockUtil.ONE_MB));
        }

        // add a random number of server configs
        for (int i = 0; i < RandomUtil.getInt(5) + 1; i++) {
            config.addServerConfig(DevUtil.makeNewServerConfiguration(RandomUtil.getInt(40) + 1));
        }

        // put some random keys in there
        for (int i = 0; i < RandomUtil.getInt(20) + 1; i++) {
            config.setValue(RandomUtil.getString(RandomUtil.getInt(50) + 1), RandomUtil.getString(RandomUtil.getInt(50) + 1));
        }

        // make an output stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ConfigurationUtil.write(config, baos);
        // convert to an array of bytes
        byte[] bytes = baos.toByteArray();

        // get a nonce
        byte[] nonce = IOUtil.getNonce(dfs);

        // make a sig+nonce buffer
        byte[] tosign = new byte[bytes.length + nonce.length];
        System.arraycopy(bytes, 0, tosign, 0, bytes.length);
        System.arraycopy(nonce, 0, tosign, bytes.length, nonce.length);

        // sign it
        String algorithm = SecurityUtil.getSignatureAlgorithm(key);
        byte[] sigBytes = SecurityUtil.sign(new ByteArrayInputStream(tosign), key, algorithm);
        // make it in to an object
        Signature sig = new Signature(sigBytes, algorithm, user.getCertificate());

        // set the config
        dfs.setConfiguration(bytes, sig, nonce);

        // sign another nonce for getting the configuration
        byte[] nonce2 = IOUtil.getNonce(dfs);
        // sign the second nonce
        String algorithm2 = SecurityUtil.getSignatureAlgorithm(key);
        byte[] sigBytes2 = SecurityUtil.sign(new ByteArrayInputStream(nonce2), key, algorithm2);
        // make it in to an object
        Signature sig2 = new Signature(sigBytes2, algorithm2, user.getCertificate());

        // get the configuration
        Configuration config2 = dfs.getConfiguration(sig2, nonce2);

        // check that they are equal
        assertEquals("Expected the same version.", config.isVersionOne(), config2.isVersionOne());
        assertEquals("Expected equal flags.", config.getFlags(), config2.getFlags());
        assertEquals("Expected the same number of data directories.", config.getDataDirectories().size(), config2.getDataDirectories().size());
        assertEquals("Expected the same number of users.", config.getUsers().size(), config2.getUsers().size());
        assertEquals("Expected the same number of hash spans.", config.getHashSpans().size(), config2.getHashSpans().size());
        assertEquals("Expected the same number of server configs.", config.getServerConfigs().size(), config2.getServerConfigs().size());
        assertEquals("Expected the same number of sticky projects.", config.getStickyProjects().size(), config2.getStickyProjects().size());

        // check all added ddc's are in the config
        for (DataDirectoryConfiguration ddc : config.getDataDirectories()) {
            boolean found = false;
            for (DataDirectoryConfiguration ddc2 : config2.getDataDirectories()) {
                if (ddc2.equals(ddc)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Expected all data directory configurations to be in configuration.", found);
        }

        // check all added users are in the config
        for (User user1 : config.getUsers()) {
            boolean found = false;
            for (User user2 : config2.getUsers()) {
                if (user1.equals(user2)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Expected all users to be in configuration.", found);
        }

        // check all added users are in the config
        for (HashSpan hashSpan1 : config.getHashSpans()) {
            boolean found = false;
            for (HashSpan hashSpan2 : config2.getHashSpans()) {
                if (hashSpan1.equals(hashSpan2)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Expected all hash spans to be in configuration.", found);
        }

        // check all added users are in the config
        for (ServerConfiguration serverConfig1 : config.getServerConfigs()) {
            boolean found = false;
            for (ServerConfiguration serverConfig2 : config2.getServerConfigs()) {
                if (serverConfig1.equals(serverConfig2)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Expected all hash spans to be in configuration.", found);
        }

        // check all added users are in the config
        for (BigHash hash1 : config.getStickyProjects()) {
            boolean found = false;
            for (BigHash hash2 : config2.getStickyProjects()) {
                if (hash1.equals(hash2)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Expected all sticky projects to be in configuration.", found);
        }

        // check keys and values
        for (String key1 : config.getValueKeys()) {
            boolean found = false;
            for (String key2 : config2.getValueKeys()) {
                if (key1.equals(key2) && config.getValue(key1).equals(config2.getValue(key2))) {
                    found = true;
                    break;
                }
            }
            assertTrue("Expected all keys and same values to be in configuration.", found);
        }
    }

    /**
     * Test that configuration changes are taking effect immediately.
     */
    /**
     * <p>Very long test. Asserts the following:</p>
     * <ul>
     *   <li>Users update immediately</li>
     *   <li>DataDirectoryConfigurations update immediately</li>
     *   <li>Attributes (name/value pairs) change immediately</li>
     *   <li>HashSpans change immediately.</li>
     *   <li>Correct data is loaded depending on data directory configurations.</li>
     * </ul>
     */
    public static final void testConfigurationChangesAreImmediate(TrancheServer ts, UserZipFile adminUser, int chunksToTry) throws Exception {

        /**
         * Need three directories. Going to put some data in each, and keep
         * track of which has which. When we change directories in configuration,
         * we expect it will load whatever data/meta is in its current directory/directories.
         */
        File dir1 = null, dir2 = null, dir3 = null;
        DataDirectoryConfiguration ddc1 = null, ddc2 = null, ddc3 = null;

        /**
         * Need to keep track of every chunk in each DDC! How else test
         * that configuration reloading works!!!
         */
        Set<BigHash> ddc1Data = new HashSet(), ddc1MetaData = new HashSet(), ddc2Data = new HashSet(), ddc2MetaData = new HashSet(), ddc3Data = new HashSet(), ddc3MetaData = new HashSet();

        try {
            // Set up directories, assert they are different
            dir1 = TempFileUtil.createTemporaryDirectory();
            dir2 = TempFileUtil.createTemporaryDirectory();
            dir3 = TempFileUtil.createTemporaryDirectory();

            assertTrue("Expect dir to exist.", dir1.exists());
            assertTrue("Expect dir to exist.", dir2.exists());
            assertTrue("Expect dir to exist.", dir3.exists());

            System.out.println("DDC #1: " + dir1.getAbsolutePath());
            System.out.println("DDC #2: " + dir2.getAbsolutePath());
            System.out.println("DDC #3: " + dir3.getAbsolutePath());

            assertFalse("All dirs must be distinct.", dir1.equals(dir2));
            assertFalse("All dirs must be distinct.", dir2.equals(dir3));
            assertFalse("All dirs must be distinct.", dir3.equals(dir1));

            // Set up DDCs
            ddc1 = new DataDirectoryConfiguration(dir1.getAbsolutePath(), 1000000000l);
            ddc2 = new DataDirectoryConfiguration(dir2.getAbsolutePath(), 1000000000l);
            ddc3 = new DataDirectoryConfiguration(dir3.getAbsolutePath(), 1000000000l);

            // verify there are no starting data directories - if there are, remove them before calling this test method
            Configuration c = IOUtil.getConfiguration(ts, adminUser.getCertificate(), adminUser.getPrivateKey());
            assertEquals("Should not be any data directories right now", 0, c.getDataDirectories().size());

            /**
             * Start with data directory #1. Add some data and meta data.
             */
            c.clearDataDirectories();
            c.addDataDirectory(ddc1);

            // set the configuration back to the tranche serve
            IOUtil.setConfiguration(ts, c, adminUser.getCertificate(), adminUser.getPrivateKey());

            // Now add some data
            for (int i = 0; i < chunksToTry; i++) {
                byte[] data = DevUtil.createRandomDataChunkVariableSize();
                BigHash dataHash = new BigHash(data);

                byte[] meta = DevUtil.createRandomMetaDataChunk();
                BigHash metaHash = DevUtil.getRandomBigHash(meta.length);

                // Really unlikely, but make sure didn't already create these
                if (ddc1Data.contains(dataHash) || ddc2Data.contains(dataHash) || ddc3Data.contains(dataHash)) {
                    i--;
                    continue;
                }
                if (ddc1MetaData.contains(metaHash) || ddc2MetaData.contains(metaHash) || ddc3MetaData.contains(metaHash)) {
                    i--;
                    continue;
                }

                // Keep track of which ddc has what
                ddc1Data.add(dataHash);
                ddc1MetaData.add(metaHash);

                PropagationReturnWrapper wrapper1 = IOUtil.setData(ts, adminUser.getCertificate(), adminUser.getPrivateKey(), dataHash, data);
                assertFalse(wrapper1.isAnyErrors());
                PropagationReturnWrapper wrapper2 = IOUtil.setMetaData(ts, adminUser.getCertificate(), adminUser.getPrivateKey(), false, metaHash, meta);
                assertFalse(wrapper2.isAnyErrors());
            }

            // Sleep to kick off threads
            if (ts instanceof FlatFileTrancheServer) {
                FlatFileTrancheServer ffts = (FlatFileTrancheServer) ts;
                ffts.waitToLoadExistingDataBlocks();
            } else {
                Thread.sleep(5000);
            }

            // Verify everything
            for (BigHash h : ddc1Data) {
                assertTrue("Just added this data, better have!", IOUtil.hasData(ts, h));
            }
            for (BigHash h : ddc1MetaData) {
                assertTrue("Just added this meta, better have!", IOUtil.hasMetaData(ts, h));
            }

            /**
             * Clear data directories. Use data directory #2. Add some data and meta data.
             */
            c.clearDataDirectories();
            c.addDataDirectory(ddc2);
            IOUtil.setConfiguration(ts, c, adminUser.getCertificate(), adminUser.getPrivateKey());

            // Now add some data
            for (int i = 0; i < chunksToTry; i++) {
                byte[] data = DevUtil.createRandomDataChunkVariableSize();
                BigHash dataHash = new BigHash(data);

                byte[] meta = DevUtil.createRandomMetaDataChunk();
                BigHash metaHash = DevUtil.getRandomBigHash(meta.length);

                // Really unlikely, but make sure didn't already create these
                if (ddc1Data.contains(dataHash) || ddc2Data.contains(dataHash) || ddc3Data.contains(dataHash)) {
                    i--;
                    continue;
                }
                if (ddc1MetaData.contains(metaHash) || ddc2MetaData.contains(metaHash) || ddc3MetaData.contains(metaHash)) {
                    i--;
                    continue;
                }

                // Keep track of which ddc has what
                ddc2Data.add(dataHash);
                ddc2MetaData.add(metaHash);

                PropagationReturnWrapper wrapper1 = IOUtil.setData(ts, adminUser.getCertificate(), adminUser.getPrivateKey(), dataHash, data);
                assertFalse(wrapper1.isAnyErrors());
                PropagationReturnWrapper wrapper2 = IOUtil.setMetaData(ts, adminUser.getCertificate(), adminUser.getPrivateKey(), false, metaHash, meta);
                assertFalse(wrapper2.isAnyErrors());
            }

            // Verify everything
            for (BigHash h : ddc2Data) {
                assertTrue("Just added this data, better have!", IOUtil.hasData(ts, h));
            }
            for (BigHash h : ddc2MetaData) {
                assertTrue("Just added this meta, better have!", IOUtil.hasMetaData(ts, h));
            }
            for (BigHash h : ddc1Data) {
                assertFalse("Should not have data, ddc not in use!", IOUtil.hasData(ts, h));
            }
            for (BigHash h : ddc1MetaData) {
                assertFalse("Should not have meta, ddc not in use!", IOUtil.hasMetaData(ts, h));
            }

            /**
             * Clear data directories. Use data directory #3. Add some data and meta data.
             */
            c.clearDataDirectories();
            c.addDataDirectory(ddc3);
            IOUtil.setConfiguration(ts, c, adminUser.getCertificate(), adminUser.getPrivateKey());

            // Now add some data
            for (int i = 0; i < chunksToTry; i++) {
                byte[] data = DevUtil.createRandomDataChunkVariableSize();
                BigHash dataHash = new BigHash(data);

                byte[] meta = DevUtil.createRandomMetaDataChunk();
                BigHash metaHash = DevUtil.getRandomBigHash(meta.length);

                // Really unlikely, but make sure didn't already create these
                if (ddc1Data.contains(dataHash) || ddc2Data.contains(dataHash) || ddc3Data.contains(dataHash)) {
                    i--;
                    continue;
                }
                if (ddc1MetaData.contains(metaHash) || ddc2MetaData.contains(metaHash) || ddc3MetaData.contains(metaHash)) {
                    i--;
                    continue;
                }

                // Keep track of which ddc has what
                ddc3Data.add(dataHash);
                ddc3MetaData.add(metaHash);

                IOUtil.setData(ts, adminUser.getCertificate(), adminUser.getPrivateKey(), dataHash, data);
                IOUtil.setMetaData(ts, adminUser.getCertificate(), adminUser.getPrivateKey(), false, metaHash, meta);
            }

            // Sleep to kick off threads
            if (ts instanceof FlatFileTrancheServer) {
                FlatFileTrancheServer ffts = (FlatFileTrancheServer) ts;
                ffts.waitToLoadExistingDataBlocks();
            } else {
                Thread.sleep(5000);
            }

            // Verify everything
            for (BigHash h : ddc3Data) {
                assertTrue("Just added this data, better have!", IOUtil.hasData(ts, h));
            }
            for (BigHash h : ddc3MetaData) {
                assertTrue("Just added this meta, better have!", IOUtil.hasMetaData(ts, h));
            }
            for (BigHash h : ddc1Data) {
                assertFalse("Should not have data, ddc not in use!", IOUtil.hasData(ts, h));
            }
            for (BigHash h : ddc1MetaData) {
                assertFalse("Should not have meta, ddc not in use!", IOUtil.hasMetaData(ts, h));
            }
            for (BigHash h : ddc2Data) {
                assertFalse("Should not have data, ddc not in use!", IOUtil.hasData(ts, h));
            }
            for (BigHash h : ddc2MetaData) {
                assertFalse("Should not have meta, ddc not in use!", IOUtil.hasMetaData(ts, h));
            }

            /**
             * Clear data directories. Use all directories, should have all data.
             */
            c.clearDataDirectories();
            c.addDataDirectory(ddc1);
            c.addDataDirectory(ddc2);
            c.addDataDirectory(ddc3);

            IOUtil.setConfiguration(ts, c, adminUser.getCertificate(), adminUser.getPrivateKey());

            // Sleep to kick off threads
            if (ts instanceof FlatFileTrancheServer) {
                FlatFileTrancheServer ffts = (FlatFileTrancheServer) ts;
                ffts.waitToLoadExistingDataBlocks();
            } else {
                Thread.sleep(5000);
            }

            // Verify everything
            for (BigHash h : ddc1Data) {
                assertTrue("Should have everything!", IOUtil.hasData(ts, h));
            }
            for (BigHash h : ddc1MetaData) {
                assertTrue("Should have everything!", IOUtil.hasMetaData(ts, h));
            }
            for (BigHash h : ddc2Data) {
                assertTrue("Should have everything!", IOUtil.hasData(ts, h));
            }
            for (BigHash h : ddc2MetaData) {
                assertTrue("Should have everything!", IOUtil.hasMetaData(ts, h));
            }
            for (BigHash h : ddc3Data) {
                assertTrue("Should have everything!", IOUtil.hasData(ts, h));
            }
            for (BigHash h : ddc3MetaData) {
                assertTrue("Should have everything!", IOUtil.hasMetaData(ts, h));
            }

            /**
             * Clear data directories. Use data directory #2 AND #3.
             * Data might have shuffled. Simply count data and meta to compare later.
             */
            c.clearDataDirectories();
            c.addDataDirectory(ddc2);
            c.addDataDirectory(ddc3);

            IOUtil.setConfiguration(ts, c, adminUser.getCertificate(), adminUser.getPrivateKey());

            /**
             * Finished checking data, let's try HashSpans
             */
            c.clearHashSpans();
            HashSpan span1 = DevUtil.createRandomHashSpan();
            c.addHashSpan(span1);

            IOUtil.setConfiguration(ts, c, adminUser.getCertificate(), adminUser.getPrivateKey());

            // Sleep to kick off threads
            if (ts instanceof FlatFileTrancheServer) {
                FlatFileTrancheServer ffts = (FlatFileTrancheServer) ts;
                ffts.waitToLoadExistingDataBlocks();
            } else {
                Thread.sleep(5000);
            }

            c = IOUtil.getConfiguration(ts, adminUser.getCertificate(), adminUser.getPrivateKey());

            assertEquals("Expecting 1 HashSpan.", 1, c.getHashSpans().size());
            for (HashSpan span : c.getHashSpans()) {
                assertTrue("Expecting to get the same span back!", span.equals(span1));
            }

            /**
             * Let's clear the hash spans
             */
            c.clearHashSpans();
            IOUtil.setConfiguration(ts, c, adminUser.getCertificate(), adminUser.getPrivateKey());

            // Sleep to kick off threads
            if (ts instanceof FlatFileTrancheServer) {
                FlatFileTrancheServer ffts = (FlatFileTrancheServer) ts;
                ffts.waitToLoadExistingDataBlocks();
            } else {
                Thread.sleep(5000);
            }

            c = IOUtil.getConfiguration(ts, adminUser.getCertificate(), adminUser.getPrivateKey());

            assertEquals("Expecting no HashSpan.", 0, c.getHashSpans().size());

            /**
             * Let's add a value and make sure it appears.
             */
            final String key = "Bryan's test key";
            final String value1 = "true";
            final String value2 = "false";

            c.setValue(key, value1);
            IOUtil.setConfiguration(ts, c, adminUser.getCertificate(), adminUser.getPrivateKey());

            // Sleep to kick off threads
            if (ts instanceof FlatFileTrancheServer) {
                FlatFileTrancheServer ffts = (FlatFileTrancheServer) ts;
                ffts.waitToLoadExistingDataBlocks();
            } else {
                Thread.sleep(5000);
            }

            c = IOUtil.getConfiguration(ts, adminUser.getCertificate(), adminUser.getPrivateKey());
            assertEquals("Expecting certain value back.", c.getValue(key), value1);

            // Change to second value
            c.setValue(key, value2);
            IOUtil.setConfiguration(ts, c, adminUser.getCertificate(), adminUser.getPrivateKey());

            // Sleep to kick off threads
            if (ts instanceof FlatFileTrancheServer) {
                FlatFileTrancheServer ffts = (FlatFileTrancheServer) ts;
                ffts.waitToLoadExistingDataBlocks();
            } else {
                Thread.sleep(5000);
            }

            c = IOUtil.getConfiguration(ts, adminUser.getCertificate(), adminUser.getPrivateKey());
            assertEquals("Expecting certain value back.", c.getValue(key), value2);

        } finally {
            IOUtil.recursiveDeleteWithWarning(dir1);
            IOUtil.recursiveDeleteWithWarning(dir2);
            IOUtil.recursiveDeleteWithWarning(dir3);
        }
    }

    public static final void testUserCannotOverwriteMetaData(TrancheServer ts, UserZipFile user) throws Exception {
        assertFalse("User should not be able to delete meta data.", user.canDeleteMetaData());

        byte[] firstMetaChunk = DevUtil.createRandomMetaDataChunk();
        byte[] secondMetaChunk = DevUtil.createRandomMetaDataChunk();

        // Only need one hash. Going to try to clobber with different chunk.
        BigHash firstMetaHash = DevUtil.getRandomBigHash();

        // the server should not already have this data
        assertFalse("Expect server to not already have meta data.", IOUtil.hasMetaData(ts, firstMetaHash));

        PropagationReturnWrapper wrapper1 = IOUtil.setMetaData(ts, user.getCertificate(), user.getPrivateKey(), false, firstMetaHash, firstMetaChunk);
        assertFalse(wrapper1.isAnyErrors());

        // the server should say it has this data
        assertTrue("Expect server to have meta data.", IOUtil.hasMetaData(ts, firstMetaHash));

        PropagationReturnWrapper wrapper2 = IOUtil.setMetaData(ts, user.getCertificate(), user.getPrivateKey(), false, firstMetaHash, secondMetaChunk);
        assertTrue(wrapper2.isAnyErrors());
    }

    public static final void testGetChunks(final TrancheServer ts, final UserZipFile uzf, int batchSize, boolean isMetaData) throws Exception {
        // We're going to upload 2xbatchSize, but only keep reference to batchSize so that there is some noise
        byte[][] uploadedChunks = new byte[batchSize][];
        BigHash[] uploadedChunksHashes = new BigHash[batchSize];
        for (int i = 0; i < batchSize; i++) {

            byte[] noiseChunkToUpload = null;
            BigHash noiseHashToUpload = null;

            if (isMetaData) {
                // Upload the chunks we're going to download.
                uploadedChunks[i] = DevUtil.createRandomMetaDataChunk();
                uploadedChunksHashes[i] = DevUtil.getRandomBigHash(uploadedChunks[i].length);
                IOUtil.setMetaData(ts, uzf.getCertificate(), uzf.getPrivateKey(), false, uploadedChunksHashes[i], uploadedChunks[i]);

                // Don't save reference. Just put some noise up on the server.
                noiseChunkToUpload = DevUtil.createRandomMetaDataChunk();
                noiseHashToUpload = DevUtil.getRandomBigHash(noiseChunkToUpload.length);
                // Upload the chunks we won't download. They are just noise.
                IOUtil.setMetaData(ts, uzf.getCertificate(), uzf.getPrivateKey(), false, noiseHashToUpload, noiseChunkToUpload);
            } else {
                // Upload the chunks we're going to download.
                uploadedChunks[i] = DevUtil.createRandomDataChunkVariableSize();
                uploadedChunksHashes[i] = new BigHash(uploadedChunks[i]);
                IOUtil.setData(ts, uzf.getCertificate(), uzf.getPrivateKey(), uploadedChunksHashes[i], uploadedChunks[i]);

                // Don't save reference. Just put some noise up on the server.
                noiseChunkToUpload = DevUtil.createRandomDataChunkVariableSize();
                noiseHashToUpload = new BigHash(noiseChunkToUpload);
                // Upload the chunks we won't download. They are just noise.
                IOUtil.setData(ts, uzf.getCertificate(), uzf.getPrivateKey(), noiseHashToUpload, noiseChunkToUpload);
            }
        }

        byte[][] downloadedChunks = null;
        if (isMetaData) {
            downloadedChunks = (byte[][]) ts.getMetaData(uploadedChunksHashes, false).getReturnValueObject();
        } else {
            downloadedChunks = (byte[][]) ts.getData(uploadedChunksHashes, false).getReturnValueObject();
        }

        // Should be same number of elements
        assertEquals("Should be one chunk downloaded for each hash.", uploadedChunksHashes.length, downloadedChunks.length);
        assertEquals("Should be one chunk downloaded for each chunk uploaded.", uploadedChunks.length, downloadedChunks.length);

        for (int i = 0; i < downloadedChunks.length; i++) {
            byte[] downloaded = downloadedChunks[i];
            byte[] uploaded = uploadedChunks[i];

            assertEquals("Expecting same number of uploaded and downloaded bytes for chunk.", uploaded.length, downloaded.length);
            for (int j = 0; j < downloaded.length; j++) {
                assertEquals("Expecting same data in uploaded and downloaded chunks.", uploaded[j], downloaded[j]);
            }
        }
    }

    /**
     * <p></p>
     * @param ts
     * @param count
     * @throws java.lang.Exception
     */
    public static final void testGetNonces(final TrancheServer ts, int count) throws Exception {
        if (count >= 0) {
            // use the stock methods
            byte[][] nonces = IOUtil.getNonces(ts, count);

            assertEquals("Expecting certain nonce count.", count, nonces.length);

            for (int i = 0; i < nonces.length; i++) {
                assertNotNull("Expect nonce " + i + " of " + nonces.length + " to not be null.", nonces[i]);
                assertEquals("Expect nonce " + i + " of " + nonces.length + " to be certain size.", NonceMap.NONCE_BYTES, nonces[i].length);
            }
        } else {
            try {
                byte[][] nonces = IOUtil.getNonces(ts, count);
                fail("Should have thrown an exception.");
            } catch (Exception expected) { /* Should throw exception */ }
        }
    }

    /**
     * Tests the functionality of batch has meta data and data methods
     * @throws java.lang.Exception
     */
    public static final void testHasDataAndMeta(final TrancheServer ts, X509Certificate auth, PrivateKey key) throws Exception {
        ByteArrayOutputStream baos = null;
        try {
            Map<BigHash, Boolean> dataMap = new HashMap<BigHash, Boolean>();
            Map<BigHash, Boolean> metaDataMap = new HashMap<BigHash, Boolean>();

            // make the data
            for (int i = 0; i < RandomUtil.getInt(20) + 2; i++) {
                // create some data
                byte[] data = DevUtil.createRandomDataChunk(RandomUtil.getInt(DataBlockUtil.ONE_MB));
                // make the hash
                BigHash hash = new BigHash(data);
                // should this one be uploaded?
                boolean upload = RandomUtil.getBoolean();
                if (upload) {
                    // set the data to the server
                    IOUtil.setData(ts, auth, key, hash, data);
                    // make sure it's on the server
                    assertTrue("Expected data to be on server.", IOUtil.hasData(ts, hash));
                } else {
                    // make sure it's not on the server
                    assertFalse("Expected data to not be on server.", IOUtil.hasData(ts, hash));
                }
                // add to the list of uploaded data
                dataMap.put(hash, upload);
            }

            // try the batch
            List<BigHash> dataHashes = new ArrayList<BigHash>();
            dataHashes.addAll(dataMap.keySet());
            assertEquals("Expected same number of hashes in list as in map.", dataHashes.size(), dataMap.keySet().size());

            boolean[] dataBooleans = ts.hasData(dataHashes.toArray(new BigHash[0]));
            assertNotNull("Expect returned list to not be null.", dataBooleans);
            // check the returned values
            assertEquals("Expected same list sizes.", dataHashes.size(), dataBooleans.length);
            for (int i = 0; i < dataHashes.size(); i++) {
                assertEquals("Expected returned value to be the same", dataMap.get(dataHashes.get(i)), Boolean.valueOf(dataBooleans[i]));
            }

            // make some meta data
            for (int i = 0; i < RandomUtil.getInt(20) + 2; i++) {
                // create a random meta data
                MetaData md = DevUtil.createRandomMetaData();
                // turn the md into bytes
                baos = new ByteArrayOutputStream();
                MetaDataUtil.write(md, baos);
                byte[] metaDataBytes = baos.toByteArray();
                // doesn't need to be any hash in particular we're setting this meta data for
                BigHash hash = new BigHash(metaDataBytes);
                // should this one be uploaded?
                boolean upload = RandomUtil.getBoolean();
                if (upload) {
                    // set the meta data
                    IOUtil.setMetaData(ts, auth, key, false, hash, metaDataBytes);
                    // the data should be on-line
                    assertTrue("Meta-data should be on-line.", IOUtil.hasMetaData(ts, hash));
                } else {
                    // the data should not be on-line
                    assertFalse("Meta-data should not be on-line.", IOUtil.hasMetaData(ts, hash));
                }
                // add to the uploaded list
                metaDataMap.put(hash, upload);
            }

            // try the batch
            List<BigHash> metaHashes = new ArrayList<BigHash>();
            metaHashes.addAll(metaDataMap.keySet());
            assertEquals("Expected same number of hashes in list as in map.", metaHashes.size(), metaDataMap.keySet().size());

            boolean[] metaBooleans = ts.hasMetaData(metaHashes.toArray(new BigHash[0]));
            assertNotNull("Expect returned list to not be null.", metaBooleans);
            // check the returned values
            assertEquals("Expected same list sizes.", metaHashes.size(), metaBooleans.length);
            for (int i = 0; i < metaHashes.size(); i++) {
                assertEquals("Expected returned value to be the same", metaDataMap.get(metaHashes.get(i)), Boolean.valueOf(metaBooleans[i]));
            }
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    public static final void testAddCorruptedDataChunk(TrancheServer ts, X509Certificate cert, PrivateKey pk) throws Exception {
        byte[] chunk = DevUtil.createRandomDataChunkVariableSize();
        BigHash hash = new BigHash(chunk);

        // Pick a random byte to change
        final int index = RandomUtil.getInt(chunk.length);
        final byte oldByte = chunk[index];
        byte newByte = (byte) (oldByte > Byte.MIN_VALUE ? oldByte - 1 : Byte.MAX_VALUE);
        assertNotSame("Should be different bytes!", oldByte, newByte);
        chunk[index] = newByte;

        PropagationReturnWrapper wrapper = IOUtil.setData(ts, cert, pk, hash, chunk);
        assertTrue(wrapper.isAnyErrors());
    }

    public static final void testGetNetworkStatusTablePortion(TrancheServer ts) throws Exception {
        StatusTable st = ts.getNetworkStatusPortion(GetNetworkStatusItem.RETURN_ALL, GetNetworkStatusItem.RETURN_ALL);
        assertEquals(NetworkUtil.getStatus().size(), st.size());
        List<StatusTableRow> expectedRows = NetworkUtil.getStatus().getRows();
        List<StatusTableRow> returnedRows = st.getRows();
        for (int i = 0; i < st.size(); i++) {
            assertEquals(expectedRows.get(i).getHost(), returnedRows.get(i).getHost());
        }
    }
}
