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

import java.security.cert.X509Certificate;
import org.tranche.hash.BigHash;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.tranche.security.Signature;
import org.tranche.time.TimeUtil;
import org.tranche.util.DevUtil;
import org.tranche.FileEncoding;
import org.tranche.commons.RandomUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.users.UserCertificateUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;
import org.tranche.util.Utils;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class MetaDataTest extends TrancheTestCase {

    public void testLastModifiedTimestamp() throws Exception {
        TestUtil.printTitle("MetaDataTest:testLastModifiedTimestamp()");

        MetaData md = new MetaData();

        // var
        long lmTimestamp = TimeUtil.getTrancheTimestamp();

        // set
        md.setLastModifiedTimestamp(lmTimestamp);

        // verify
        assertEquals(lmTimestamp, md.getLastModifiedTimestamp());
    }

    public void testVersion() throws Exception {
        TestUtil.printTitle("MetaDataTest:testVersion()");

        MetaData md = new MetaData();

        // default
        assertEquals(MetaData.VERSION_LATEST, md.getVersion());
        assertFalse(md.isVersionOne());

        // set version one
        String version = MetaData.VERSION_ONE;
        md.setVersion(version);

        // verify
        assertEquals(MetaData.VERSION_ONE, md.getVersion());
        assertTrue(md.isVersionOne());

        // set version two
        version = MetaData.VERSION_TWO;
        md.setVersion(version);

        // verify
        assertEquals(MetaData.VERSION_TWO, md.getVersion());
        assertFalse(md.isVersionOne());
    }

    public void testGZIPCompressed() throws Exception {
        TestUtil.printTitle("MetaDataTest:testGZIPCompressed()");
        MetaData md = createMetaData(RandomUtil.getString(15), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);

        // verify default
        assertTrue(md.isGZIPCompressed());

        // set
        md.setGZIPCompress(false);

        // verify
        assertFalse(md.isGZIPCompressed());

        // set
        md.setGZIPCompress(true);

        // verify
        assertTrue(md.isGZIPCompressed());
    }

    public void testIsProjectFile() throws Exception {
        TestUtil.printTitle("MetaDataTest:testIsProjectFile()");
        MetaData md = createMetaData(RandomUtil.getString(15), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);

        // verify default
        assertFalse(md.isProjectFile());

        // set
        md.setIsProjectFile(true);

        // verify
        assertTrue(md.isProjectFile());

        // set
        md.setIsProjectFile(false);

        // verify
        assertFalse(md.isProjectFile());
    }

    public void testIsLimitedLife() throws Exception {
        TestUtil.printTitle("MetaDataTest:testIsLimitedLife()");
        MetaData md = createMetaData(RandomUtil.getString(15), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);

        // verify default
        assertFalse(md.isLimitedLife());

        // set
        md.setIsLimitedLife(true);

        // verify
        assertTrue(md.isLimitedLife());

        // set
        md.setIsLimitedLife(false);

        // verify
        assertFalse(md.isLimitedLife());
    }

    public void testShareMetaDataIfEncrypted() throws Exception {
        TestUtil.printTitle("MetaDataTest:testShareMetaDataIfEncrypted()");
        MetaData md = createMetaData(RandomUtil.getString(15), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);

        // verify default
        assertFalse(md.shareMetaDataIfEncrypted());

        // set
        md.setShareMetaDataIfEncrypted(true);

        // verify
        assertTrue(md.shareMetaDataIfEncrypted());

        // set
        md.setShareMetaDataIfEncrypted(false);

        // verify
        assertFalse(md.shareMetaDataIfEncrypted());
    }

    public void testDataSetName() throws Exception {
        TestUtil.printTitle("MetaDataTest:testDataSetName()");
        MetaData md = createMetaData(RandomUtil.getString(15), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);

        // default
        assertEquals(null, md.getDataSetName());

        // must be project file
        md.setIsProjectFile(true);

        // var
        String name = "name";

        // set
        md.setDataSetName(name);

        // verify
        assertEquals(name, md.getDataSetName());

        // reset
        name = "new name";

        // set
        md.setDataSetName(name);

        // verify
        assertEquals(name, md.getDataSetName());
    }

    public void testDataSetSize() throws Exception {
        TestUtil.printTitle("MetaDataTest:testDataSetSize()");
        MetaData md = createMetaData(RandomUtil.getString(15), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);

        // default
        assertEquals(-1, md.getDataSetSize());

        // must be project file
        md.setIsProjectFile(true);

        // var
        long size = Long.valueOf(RandomUtil.getInt(1024));

        // set
        md.setDataSetSize(size);

        // verify
        assertEquals(size, md.getDataSetSize());

        // reset
        size = 1;

        // set
        md.setDataSetSize(size);

        // verify
        assertEquals(size, md.getDataSetSize());
    }

    public void testDataSetFiles() throws Exception {
        TestUtil.printTitle("MetaDataTest:testDataSetFiles()");
        MetaData md = createMetaData(RandomUtil.getString(15), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);

        // default
        assertEquals(-1, md.getDataSetFiles());

        // must be project file
        md.setIsProjectFile(true);

        // var
        int files = 1;

        // set
        md.setDataSetFiles(files);

        // verify
        assertEquals(files, md.getDataSetFiles());

        // reset
        files = 10;

        // set
        md.setDataSetFiles(files);

        // verify
        assertEquals(files, md.getDataSetFiles());
    }

    public void testDataSetDescription() throws Exception {
        TestUtil.printTitle("MetaDataTest:testDataSetDescription()");
        MetaData md = createMetaData(RandomUtil.getString(15), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);

        // default
        assertEquals(null, md.getDataSetDescription());

        // must be project file
        md.setIsProjectFile(true);

        // var
        String description = "description";

        // set
        md.setDataSetDescription(description);

        // verify
        assertEquals(description, md.getDataSetDescription());

        // reset
        description = "new description";

        // set
        md.setDataSetDescription(description);

        // verify
        assertEquals(description, md.getDataSetDescription());
    }

    public void testMimeType() throws Exception {
        TestUtil.printTitle("MetaDataTest:testMimeType()");
        MetaData md = createMetaData(RandomUtil.getString(15), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);

        // var
        String mimeType = "mime/type";
        md.setMimeType(mimeType);

        // verify
        assertEquals(mimeType, md.getMimeType());
    }

    public void testGetHash() throws Exception {
        TestUtil.printTitle("MetaDataTest:testGetHash()");

        MetaData md = createMetaData(RandomUtil.getString(15), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);

        // hash should be the the NONE hash
        BigHash expectedHash = null;
        for (FileEncoding encoding : md.getEncodings()) {
            if (encoding.getName().equals(FileEncoding.NONE)) {
                expectedHash = encoding.getHash();
                break;
            }
        }

        assertEquals(expectedHash.toString(), md.getHash().toString());
    }

    public void testStickyServers() throws Exception {
        TestUtil.printTitle("MetaDataTest:testStickyServers()");

        MetaData md = createMetaData(RandomUtil.getString(15), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);

        // should have nothing by default
        assertEquals(0, md.getStickyServers().size());

        // add some sticky server URLs
        md.addStickyServer("localhost");
        md.addStickyServer("localhost2");

        // should now have two
        Collection<String> hosts = md.getStickyServers();
        assertEquals(2, hosts.size());
        assertTrue(hosts.contains("localhost"));
        assertTrue(hosts.contains("localhost2"));

        // check annotations
        Collection<String> annotations = md.getAnnotationValuesWithName(MetaDataAnnotation.PROP_STICKY_SERVER_HOST);
        assertEquals(2, annotations.size());
        assertTrue(annotations.contains("localhost"));
        assertTrue(annotations.contains("localhost2"));
    }

    public void testHidden() throws Exception {
        TestUtil.printTitle("MetaDataTest:testHidden()");

        MetaData md = createMetaData(RandomUtil.getString(15), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);

        // not hidden by default
        assertFalse(md.isHidden());

        // hide
        md.setHidden(true);

        // now hidden
        assertTrue(md.isHidden());
        assertEquals("true", md.getProperties().get(MetaData.PROP_HIDDEN));

        // unhide
        md.setHidden(false);

        // not hidden
        assertFalse(md.isHidden());
    }

    public void testIsEncrypted() throws Exception {
        TestUtil.printTitle("MetaDataTest:testIsEncrypted()");

        MetaData md = createMetaData(RandomUtil.getString(RandomUtil.getInt(50)), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), true);
        assertTrue(md.isEncrypted());
        // not encrypted
        MetaData md2 = createMetaData(RandomUtil.getString(RandomUtil.getInt(50)), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), false);
        assertFalse(md2.isEncrypted());
    }

    public void testPublishPassphrase() throws Exception {
        TestUtil.printTitle("MetaDataTest:testPublishPassphrase()");

        MetaData md = createMetaData(RandomUtil.getString(RandomUtil.getInt(50)), TimeUtil.getTrancheTimestamp(), RandomUtil.getString(50), true);
        assertNull(md.getPublicPassphrase());
        assertFalse(md.isPublicPassphraseSet());

        // set the passphrase - could be anything
        String publicPassphrase = "passphrase";
        md.setPublicPassphrase(publicPassphrase);
        // should still be encrypted
        assertTrue(md.isEncrypted());
        assertTrue(md.isPublicPassphraseSet());
        assertEquals(md.getPublicPassphrase(), publicPassphrase);

        // there should be a published timestamp property
        int foundCount = 0;
        for (FileEncoding encoding : md.getEncodings()) {
            if (encoding.getName().equals(FileEncoding.AES)) {
                if (encoding.getProperty(FileEncoding.PROP_TIMESTAMP_PUBLISHED) != null) {
                    foundCount++;
                }
            }
        }
        assertEquals("Should be a single published timestamp annotation.", foundCount, 1);

        // that passphrase was incorrect - reset the public passphrase
        md.clearPublicPassphrase();
        assertNull(md.getPublicPassphrase());
        assertFalse(md.isPublicPassphraseSet());

        // there should be no published timestamp annotations
        for (FileEncoding encoding : md.getEncodings()) {
            if (encoding.getName().equals(FileEncoding.AES)) {
                if (encoding.getProperty(FileEncoding.PROP_TIMESTAMP_PUBLISHED) != null) {
                    fail("Should not be any published timestamp annotations.");
                }
            }
        }
    }

    public void testAddUploader() throws Exception {
        TestUtil.printTitle("MetaDataTest:testAddUploader()");

        long uploadTimestamp1 = TimeUtil.getTrancheTimestamp();
        MetaData md = createMetaData(RandomUtil.getString(50), uploadTimestamp1, null, false);

        // verify size and selected uploader
        assertEquals(1, md.getUploaderCount());
        assertEquals(uploadTimestamp1, md.getTimestampUploaded());

        // sleep to change
        Thread.sleep(50);

        // add two more uploaders
        long uploadTimestamp2 = TimeUtil.getTrancheTimestamp();
        addUploader(md, RandomUtil.getString(50), uploadTimestamp2, null, false);

        // verify size
        assertEquals(2, md.getUploaderCount());
        assertEquals(uploadTimestamp2, md.getTimestampUploaded());

        // each should be selectable -- these will throw exceptions if they are not selectable
        // verify each has corresponding parts
        md.selectUploader(UserCertificateUtil.readUserName(DevUtil.getDevAuthority()), uploadTimestamp1, null);
        assertNotNull(md.getParts());
        md.selectUploader(UserCertificateUtil.readUserName(DevUtil.getDevAuthority()), uploadTimestamp2, null);
        assertNotNull(md.getParts());
    }

    public void testRemoveUploader() throws Exception {
        TestUtil.printTitle("MetaDataTest:testRemoveUploader()");

        long uploadTimestamp1 = TimeUtil.getTrancheTimestamp();
        MetaData md = createMetaData(RandomUtil.getString(50), uploadTimestamp1, null, false);

        // verify size
        assertEquals(1, md.getUploaderCount());

        // sleep to change timestamp
        Thread.sleep(50);

        // add two more uploaders
        long uploadTimestamp2 = TimeUtil.getTrancheTimestamp();
        addUploader(md, RandomUtil.getString(50), uploadTimestamp2, null, false);

        // verify size
        assertEquals(2, md.getUploaderCount());

        // remove the first uploader
        md.removeUploader(UserCertificateUtil.readUserName(DevUtil.getDevAuthority()), uploadTimestamp1, null);

        // verify size and only second one
        assertEquals(1, md.getUploaderCount());
        assertEquals(1, md.getAllParts().size());
        assertEquals(uploadTimestamp2, md.getTimestampUploaded());
    }

    public void testSelectedUploader() throws Exception {
        TestUtil.printTitle("MetaDataTest:testSelectedUploader()");

        long uploadTimestamp1 = TimeUtil.getTrancheTimestamp();
        MetaData md = createMetaData(RandomUtil.getString(50), uploadTimestamp1, RandomUtil.getString(50), false);
        Thread.sleep(50);
        // add two more uploaders
        long uploadTimestamp2 = TimeUtil.getTrancheTimestamp();
        addUploader(md, RandomUtil.getString(50), uploadTimestamp2, RandomUtil.getString(50), false);
        Thread.sleep(50);
        long uploadTimestamp3 = TimeUtil.getTrancheTimestamp();
        addUploader(md, RandomUtil.getString(50), uploadTimestamp3, RandomUtil.getString(50), false);

        // read the signature names
        md.selectUploader(0);
        String signatureName1 = md.getSignature().getUserName();
        md.selectUploader(1);
        String signatureName2 = md.getSignature().getUserName();
        md.selectUploader(2);
        String signatureName3 = md.getSignature().getUserName();

        // select the uploader and verify signature name
        md.selectUploader(signatureName1, uploadTimestamp1, null);
        assertEquals(signatureName1, md.getSignature().getUserName());
        md.selectUploader(signatureName2, uploadTimestamp2, null);
        assertEquals(signatureName2, md.getSignature().getUserName());
        md.selectUploader(signatureName3, uploadTimestamp3, null);
        assertEquals(signatureName3, md.getSignature().getUserName());
    }

    public void testSelectedUploaderFromSameDataSet() throws Exception {
        TestUtil.printTitle("MetaDataTest:testSelectedUploaderFromSameDataSet()");

        long uploadTimestamp = TimeUtil.getTrancheTimestamp();
        String relativeLocation1 = RandomUtil.getString(50);
        String relativeLocation2 = RandomUtil.getString(50);
        MetaData md = createMetaData(RandomUtil.getString(50), uploadTimestamp, relativeLocation1, false);
        addUploader(md, RandomUtil.getString(50), uploadTimestamp, relativeLocation2, false);

        // throws an exception
        boolean exceptionThrown = false;
        try {
            md.selectUploader(UserCertificateUtil.readUserName(DevUtil.getDevAuthority()), uploadTimestamp, null);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            fail("Exception not thrown.");
        }

        // select the uploaders in turn
        md.selectUploader(UserCertificateUtil.readUserName(DevUtil.getDevAuthority()), uploadTimestamp, relativeLocation1);
        assertEquals(relativeLocation1, md.getRelativePathInDataSet());
        md.selectUploader(UserCertificateUtil.readUserName(DevUtil.getDevAuthority()), uploadTimestamp, relativeLocation2);
        assertEquals(relativeLocation2, md.getRelativePathInDataSet());
    }

    private MetaData createMetaData(String name, long timestamp, String relativeLocationInDataSet, boolean isEncrypted) throws Exception {
        MetaData md = new MetaData();

        // create an uploader
        addUploader(md, name, timestamp, relativeLocationInDataSet, isEncrypted);

        return md;
    }

    private void addUploader(MetaData md, String name, long timestamp, String relativeLocationInDataSet, boolean isEncrypted) throws Exception {
        // signature
        byte[] bytes = Utils.makeRandomData(10);
        X509Certificate cert = DevUtil.getDevAuthority();
        String algorithm = SecurityUtil.getSignatureAlgorithm(cert.getPublicKey());
        Signature signature = new Signature(bytes, algorithm, cert);

        // make encodings
        ArrayList<FileEncoding> encodings = new ArrayList();
        // make the default encoding
        byte[] normalHashData = Utils.makeRandomData(1000);
        BigHash normalHash = new BigHash(normalHashData);
        encodings.add(new FileEncoding(FileEncoding.NONE, normalHash));
        // make the compression encoding
        byte[] compressionHashData = Utils.makeRandomData(1000);
        BigHash compressionHash = new BigHash(compressionHashData);
        encodings.add(new FileEncoding(FileEncoding.GZIP, compressionHash));
        if (isEncrypted) {
            // make the encryption encoding
            byte[] encryptionHashData = Utils.makeRandomData(1000);
            BigHash encryptionHash = new BigHash(encryptionHashData);
            encodings.add(new FileEncoding(FileEncoding.AES, encryptionHash));
        }

        // properties
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(MetaData.PROP_NAME, name);
        properties.put(MetaData.PROP_TIMESTAMP_UPLOADED, String.valueOf(timestamp));
        properties.put(MetaData.PROP_PATH_IN_DATA_SET, relativeLocationInDataSet);

        // make up some annotations
        ArrayList<MetaDataAnnotation> annotations = new ArrayList();
        for (int i = 0; i < 10; i++) {
            annotations.add(new MetaDataAnnotation(RandomUtil.getString(10), RandomUtil.getString(10)));
        }

        // add uploader
        md.addUploader(signature, encodings, properties, annotations);

        // make some big hashes
        for (int i = 0; i < RandomUtil.getInt(50); i++) {
            md.addPart(DevUtil.getRandomBigHash());
        }
    }
}
