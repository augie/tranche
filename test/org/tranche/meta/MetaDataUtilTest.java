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
package org.tranche.meta;

import org.tranche.commons.RandomUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.FileEncoding;
import org.tranche.util.*;
import org.tranche.hash.BigHash;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.tranche.commons.DebugUtil;
import org.tranche.security.Signature;
import org.tranche.hash.Base16;
import org.tranche.project.ProjectFile;
import org.tranche.time.TimeUtil;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class MetaDataUtilTest extends TrancheTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        DebugUtil.setDebug(MetaDataUtil.class, true);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        DebugUtil.setDebug(MetaDataUtil.class, false);
    }

    public void testVersionOne() throws Exception {
        TestUtil.printTitle("testVersionOne()");

        MetaData md = createMetaData(1, true);

        // testing version one
        md.setVersion(MetaData.VERSION_ONE);

        // start without GZIP
        md.setGZIPCompress(false);

        // serialize the file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MetaDataUtil.write(md, baos);

        // check that the meta-data checks out
        checkMetaData(md, baos, true);

        //check that GZIP would shrink it
        md.setGZIPCompress(true);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        MetaDataUtil.write(md, compressed);

        // check that the meta-data checks out
        checkMetaData(md, compressed, true);
    }

    public void testVersionTwo() throws Exception {
        TestUtil.printTitle("testVersionTwo()");

        MetaData md = createMetaData(1, true);

        // testing version one
        md.setVersion(MetaData.VERSION_TWO);

        // start without GZIP
        md.setGZIPCompress(false);

        // serialize the file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MetaDataUtil.write(md, baos);

        // check that the meta-data checks out
        checkMetaData(md, baos, true);

        //check that GZIP would shrink it
        md.setGZIPCompress(true);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        MetaDataUtil.write(md, compressed);

        // check that the meta-data checks out
        checkMetaData(md, compressed, true);
    }

    public void testVersionThree() throws Exception {
        TestUtil.printTitle("testVersionThree()");

        MetaData md = createMetaData(1, true);

        // testing version one
        md.setVersion(MetaData.VERSION_THREE);

        // start without GZIP
        md.setGZIPCompress(false);

        // serialize the file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MetaDataUtil.write(md, baos);

        // check that the meta-data checks out
        checkMetaData(md, baos, true);

        //check that GZIP would shrink it
        md.setGZIPCompress(true);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        MetaDataUtil.write(md, compressed);

        // check that the meta-data checks out
        checkMetaData(md, compressed, true);
    }

    public void testVersionFour() throws Exception {
        TestUtil.printTitle("testVersionFour()");

        MetaData md = createMetaData(2 + RandomUtil.getInt(5), true);

        // testing version one
        md.setVersion(MetaData.VERSION_FOUR);

        // there is no GZIP compression in version four -- caused problems with multiple part sets
        md.setGZIPCompress(false);

        // serialize the file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MetaDataUtil.write(md, baos);

        // check that the meta-data checks out
        checkMetaData(md, baos, true);

        //check that GZIP would shrink it
        md.setGZIPCompress(true);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        MetaDataUtil.write(md, compressed);

        // check that the meta-data checks out
        checkMetaData(md, compressed, true);
    }

    public void testFixProjectFileBit() throws Exception {
        testAdjustLimitedLifeBit(MetaData.VERSION_ONE);
        testAdjustLimitedLifeBit(MetaData.VERSION_TWO);
        testAdjustLimitedLifeBit(MetaData.VERSION_THREE);
    }

    /**
     * <p>Fixes the problem that occurred where the project file bit was not set.</p>
     * @param version
     * @throws java.lang.Exception
     */
    public void testFixProjectFileBit(String version) throws Exception {
        TestUtil.printTitle("testFixProjectFileBit(" + version + ")");
        MetaData md = createMetaData(1, true);
        md.setProperty(MetaData.PROP_NAME, ProjectFile.OLD_PROJECT_FILE_NAME);
        md.setVersion(version);

        // serialize the file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MetaDataUtil.write(md, baos);
        MetaData md2 = MetaDataUtil.read(new ByteArrayInputStream(baos.toByteArray()));

        // check if limited life bit is still set -- should not be
        assertTrue(md.isProjectFile());
        assertFalse(md2.isProjectFile());
    }

    public void testAdjustLimitedLifeBit() throws Exception {
        testAdjustLimitedLifeBit(MetaData.VERSION_ONE);
        testAdjustLimitedLifeBit(MetaData.VERSION_TWO);
        testAdjustLimitedLifeBit(MetaData.VERSION_THREE);
    }

    public void testAdjustLimitedLifeBit(String version) throws Exception {
        TestUtil.printTitle("testAdjustLimitedLifeBit(" + version + ")");
        MetaData md = createMetaData(2, true);
        md.setVersion(version);
        md.setIsLimitedLife(true);

        // serialize the file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MetaDataUtil.write(md, baos);
        MetaData md2 = MetaDataUtil.read(new ByteArrayInputStream(baos.toByteArray()));

        // check if limited life bit is still set -- should not be
        assertTrue(md.isLimitedLife());
        assertFalse(md2.isLimitedLife());
    }

    public void testAdjustAnnotationsPriorToVersionFour() throws Exception {
        testAdjustAnnotationsPriorToVersionFour(MetaData.VERSION_ONE);
        testAdjustAnnotationsPriorToVersionFour(MetaData.VERSION_TWO);
        testAdjustAnnotationsPriorToVersionFour(MetaData.VERSION_THREE);
    }

    /**
     * <p>This functionality needs to stick around until all meta data are version four or later.</p>
     * @param version
     * @throws java.lang.Exception
     */
    public void testAdjustAnnotationsPriorToVersionFour(String version) throws Exception {
        TestUtil.printTitle("testAdjustAnnotationsPriorToVersionFour(" + version + ")");
        MetaData md = createMetaData(1, false);
        md.setVersion(version);

        // add the annotations to be moved
        String currentTimestampString = String.valueOf(TimeUtil.getTrancheTimestamp());
        md.addAnnotation(MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP, currentTimestampString);
        md.addAnnotation(MetaDataAnnotation.FILE_LAST_MODIFIED_TIMESTAMP, currentTimestampString);
        BigHash oldVersion = DevUtil.getRandomBigHash();
        md.addAnnotation(MetaDataAnnotation.PROP_OLD_VERSION, oldVersion.toString());
        BigHash newVersion = DevUtil.getRandomBigHash();
        md.addAnnotation(MetaDataAnnotation.PROP_NEW_VERSION, newVersion.toString());
        md.addAnnotation(MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getName(), "true");
        md.addAnnotation(MetaDataAnnotation.PROP_DELETE_OLD_VERSION, "");
        md.addAnnotation(MetaDataAnnotation.PROP_DELETE_NEW_VERSION, "");
        String serverHost = "hostname";
        md.addAnnotation(MetaDataAnnotation.PROP_STICKY_SERVER_URL, "tranche://" + serverHost + ":1");
        md.addAnnotation(MetaDataAnnotation.PROP_DELETED, "true");
        md.addAnnotation(MetaDataAnnotation.PROP_UNDELETED, "");
        md.addAnnotation(MetaDataAnnotation.PROJECT_ANNOTATED_MDA, "");

        // serialize the file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MetaDataUtil.write(md, baos);
        MetaData md2 = MetaDataUtil.read(new ByteArrayInputStream(baos.toByteArray()));

        // test the changes
        // each of the following should have been deleted
        assertEquals(0, md2.getAnnotationsWithName(MetaDataAnnotation.PROJECT_ANNOTATED_MDA).size());
        assertEquals(0, md2.getAnnotationsWithName(MetaDataAnnotation.PROP_UNDELETED).size());
        assertEquals(0, md2.getAnnotationsWithName(MetaDataAnnotation.PROP_DELETE_NEW_VERSION).size());
        assertEquals(0, md2.getAnnotationsWithName(MetaDataAnnotation.PROP_DELETE_OLD_VERSION).size());
        // each of the following should have been moved to properties structure
        // file update timestamp
        assertEquals(0, md2.getAnnotationsWithName(MetaDataAnnotation.FILE_LAST_MODIFIED_TIMESTAMP).size());
        assertEquals(currentTimestampString, String.valueOf(md2.getTimestampFileModified()));
        assertTrue(md2.getProperties().containsKey(MetaData.PROP_TIMESTAMP_FILE));
        assertEquals(currentTimestampString, md2.getProperties().get(MetaData.PROP_TIMESTAMP_FILE));
        // new version
        assertEquals(0, md2.getAnnotationsWithName(MetaDataAnnotation.PROP_NEW_VERSION).size());
        assertEquals(newVersion.toString(), md2.getNextVersion().toString());
        assertTrue(md2.getProperties().containsKey(MetaData.PROP_VERSION_NEXT));
        assertEquals(newVersion.toString(), md2.getProperties().get(MetaData.PROP_VERSION_NEXT));
        // old version
        assertEquals(0, md2.getAnnotationsWithName(MetaDataAnnotation.PROP_OLD_VERSION).size());
        assertEquals(oldVersion.toString(), md2.getPreviousVersion().toString());
        assertTrue(md2.getProperties().containsKey(MetaData.PROP_VERSION_PREVIOUS));
        assertEquals(oldVersion.toString(), md2.getProperties().get(MetaData.PROP_VERSION_PREVIOUS));
        // hidden
        assertEquals(0, md2.getAnnotationsWithName(MetaDataAnnotation.PROP_DELETED).size());
        assertEquals(true, md2.isHidden());
        assertTrue(md2.getProperties().containsKey(MetaData.PROP_HIDDEN));
        assertEquals("true", md2.getProperties().get(MetaData.PROP_HIDDEN));
        // share meta data if encrypted
        assertEquals(0, md2.getAnnotationsWithName(MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getName()).size());
        assertEquals(true, md2.shareMetaDataIfEncrypted());
        assertTrue(md2.getProperties().containsKey(MetaData.PROP_SHARE_INFO_IF_ENCRYPTED));
        assertEquals("true", md2.getProperties().get(MetaData.PROP_SHARE_INFO_IF_ENCRYPTED));
        // each of the following should have been moved to the AES file encoding properties
        assertEquals(0, md2.getAnnotationsWithName(MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP).size());
        assertTrue(md2.getEncodings().get(md2.getEncodings().size() - 1).getProperties().containsKey(FileEncoding.PROP_TIMESTAMP_PUBLISHED));
        assertEquals(currentTimestampString, md2.getEncodings().get(md2.getEncodings().size() - 1).getProperty(FileEncoding.PROP_TIMESTAMP_PUBLISHED));
        // sticky server
        assertEquals(0, md2.getAnnotationsWithName(MetaDataAnnotation.PROP_STICKY_SERVER_URL).size());
        assertEquals(1, md2.getAnnotationsWithName(MetaDataAnnotation.PROP_STICKY_SERVER_HOST).size());
        assertEquals(serverHost, md2.getStickyServers().toArray(new String[0])[0]);
        assertEquals(serverHost, md2.getAnnotationsWithName(MetaDataAnnotation.PROP_STICKY_SERVER_HOST).toArray(new MetaDataAnnotation[0])[0].getValue());
    }

    private MetaData createMetaData(int uploaderCount, boolean allProperties) throws Exception {
        MetaData md = new MetaData();

        // make some uploaders
        for (int i = 0; i < uploaderCount; i++) {
            addUploader(md, allProperties);
        }

        return md;
    }

    private void addUploader(MetaData md, boolean allProperties) throws Exception {
        // signature
        byte[] bytes = Utils.makeRandomData(10);
        X509Certificate cert = DevUtil.getDevAuthority();
        String algorithm = SecurityUtil.getSignatureAlgorithm(cert.getPublicKey());
        Signature signature = new Signature(bytes, algorithm, cert);

        // make the default encoding
        byte[] normalHashData = Utils.makeRandomData(1000);
        BigHash normalHash = new BigHash(normalHashData);
        FileEncoding normal = new FileEncoding(FileEncoding.NONE, normalHash);
        // make the compression encoding
        byte[] compressionHashData = Utils.makeRandomData(1000);
        BigHash compressionHash = new BigHash(compressionHashData);
        FileEncoding compression = new FileEncoding(FileEncoding.GZIP, compressionHash);
        // make the encryption encoding
        byte[] encryptionHashData = Utils.makeRandomData(1000);
        BigHash encryptionHash = new BigHash(encryptionHashData);
        FileEncoding encryption = new FileEncoding(FileEncoding.AES, encryptionHash);
        encryption.getProperties().setProperty(FileEncoding.PROP_PASSPHRASE, "The passphrase is....");
        encryption.getProperties().setProperty("foo", "bar");

        // put them in an array list
        ArrayList<FileEncoding> encodings = new ArrayList();
        encodings.add(normal);
        encodings.add(compression);
        encodings.add(encryption);

        // properties
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(MetaData.PROP_NAME, "I am a name.");
        properties.put(MetaData.PROP_TIMESTAMP_UPLOADED, String.valueOf(TimeUtil.getTrancheTimestamp()));
        if (allProperties) {
            properties.put(MetaData.PROP_MIME_TYPE, "text/plain");
            properties.put(MetaData.PROP_DATA_SET_DESCRIPTION, "notblank" + RandomUtil.getString(RandomUtil.getInt(15)));
            properties.put(MetaData.PROP_DATA_SET_NAME, "notblank" + RandomUtil.getString(RandomUtil.getInt(15)));
            properties.put(MetaData.PROP_DATA_SET_FILES, String.valueOf(RandomUtil.getInt(1024 * 1024)));
            properties.put(MetaData.PROP_DATA_SET_SIZE, String.valueOf(RandomUtil.getInt(1024 * 1024)));
            properties.put(MetaData.PROP_SHARE_INFO_IF_ENCRYPTED, String.valueOf(RandomUtil.getBoolean()));
            properties.put(MetaData.PROP_HIDDEN, String.valueOf(RandomUtil.getBoolean()));
            properties.put(MetaData.PROP_TIMESTAMP_FILE, String.valueOf(RandomUtil.getInt(Integer.MAX_VALUE)));
            properties.put(MetaData.PROP_PATH_IN_DATA_SET, "notblank" + RandomUtil.getString(RandomUtil.getInt(15)));
            properties.put(MetaData.PROP_VERSION_NEXT, DevUtil.getRandomBigHash().toString());
            properties.put(MetaData.PROP_VERSION_PREVIOUS, DevUtil.getRandomBigHash().toString());
        }

        // make up some annotations
        ArrayList<MetaDataAnnotation> annotations = new ArrayList();
        for (int i = 0; i < 10; i++) {
            annotations.add(new MetaDataAnnotation(RandomUtil.getString(10), RandomUtil.getString(10)));
        }

        // add uploader
        md.addUploader(signature, encodings, properties, annotations);

        // make some big hashes
        for (int j = 0; j < RandomUtil.getInt(50); j++) {
            md.addPart(DevUtil.getRandomBigHash());
        }
    }

    public static void checkMetaData(MetaData md, MetaData md2, boolean checkModifiedTimestamp) throws Exception {
        // compare uploader-independent values
        assertEquals("Expect the reading of the latest version (not version one).", false, md2.isVersionOne());
        assertEquals("Expect the rading of the latest version.", MetaData.VERSION_LATEST, md2.getVersion());
        assertEquals(md.isGZIPCompressed(), md2.isGZIPCompressed());
        assertEquals(md.isLimitedLife(), md2.isLimitedLife());
        assertEquals(md.isProjectFile(), md2.isProjectFile());
        if (!md.isVersionOne()) {
            if (checkModifiedTimestamp) {
                assertEquals(md.getLastModifiedTimestamp(), md2.getLastModifiedTimestamp());
            }
        }
        assertEquals(md.getUploaderCount(), md2.getUploaderCount());
        // check the parts map size -- parts were checked in uploader comparison
        assertEquals(md.getAllParts().size(), md2.getAllParts().size());

        // compare the uploaders
        for (int i = 0; i < md.getUploaderCount(); i++) {
            md.selectUploader(i);
            md2.selectUploader(i);

            // expect the same uploader
            assertEquals("Same algorithm name expected.", md.getSignature().getAlgorithm(), md2.getSignature().getAlgorithm());
            assertEquals("Same cert expected.", Base16.encode(md.getSignature().getCert().getEncoded()), Base16.encode(md2.getSignature().getCert().getEncoded()));

            // compare the encodings
            assertEquals("Same number of encodings expected.", md.getEncodings().size(), md2.getEncodings().size());
            for (int j = 0; j < md.getEncodings().size(); j++) {
                FileEncoding fe = md.getEncodings().get(j);
                Properties props = fe.getProperties();
                boolean found = false;
                for (int k = 0; k < md2.getEncodings().size(); k++) {
                    FileEncoding fe2 = md2.getEncodings().get(k);
                    Properties props2 = fe2.getProperties();
                    if (fe.getName().equals(fe2.getName()) && fe.getHash().equals(fe2.getHash()) && props.size() == props2.size()) {
                        found = true;
                        for (Object o : props.keySet()) {
                            String s = (String) o;
                            if (!props.getProperty(s).equals(props2.getProperty(s))) {
                                found = false;
                            }
                        }
                        if (found) {
                            break;
                        }
                    }
                }
                if (!found) {
                    fail("Equivalent encoding not found.");
                }
            }

            // properties
            assertEquals(md.isHidden(), md2.isHidden());
            assertEquals(md.isMimeType(), md2.isMimeType());
            assertEquals(md.isEncrypted(), md2.isEncrypted());
            assertEquals(md.isPublicPassphraseSet(), md2.isPublicPassphraseSet());
            assertEquals(md.getNextVersion(), md2.getNextVersion());
            assertEquals(md.getPreviousVersion(), md2.getPreviousVersion());
            // looking for equivalence
//            assertEquals(md.getTimestampFileModified(), md2.getTimestampFileModified());
            if (!(md.getVersion().equals(MetaData.VERSION_ONE) || md.getVersion().equals(MetaData.VERSION_TWO))) {
                assertEquals(md.getDataSetName(), md2.getDataSetName());
                assertEquals(md.getDataSetDescription(), md2.getDataSetDescription());
                assertEquals(md.getDataSetSize(), md2.getDataSetSize());
                assertEquals(md.getDataSetFiles(), md2.getDataSetFiles());
                if (!md.getVersion().equals(MetaData.VERSION_THREE)) {
                    assertEquals(md.getRelativePathInDataSet(), md2.getRelativePathInDataSet());
                }
            }
            assertEquals(md.getName(), md2.getName());
            assertEquals(md.getMimeType(), md2.getMimeType());
            // only check if there is a passphrase
            if (md.isEncrypted()) {
                assertEquals(md.getPublicPassphrase(), md2.getPublicPassphrase());
            }

            // check parts
            assertEquals(md.getParts().size(), md2.getParts().size());
            for (int j = 0; j < md.getParts().size(); j++) {
                assertEquals(md.getParts().get(j).toString(), md2.getParts().get(j).toString());
            }
        }
    }

    public static void checkMetaData(MetaData md, ByteArrayOutputStream baos, boolean checkModifiedTimestamp) throws Exception {
        // read the data back in
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        checkMetaData(md, MetaDataUtil.read(bais), checkModifiedTimestamp);
    }
}
