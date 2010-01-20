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
package org.tranche.hash;

import org.tranche.util.*;
import org.tranche.security.SecurityUtil;
import org.tranche.util.IOUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import org.tranche.hash.span.HashSpan;
import org.tranche.flatfile.DataBlockUtil;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class BigHashTest extends TrancheTestCase {

    public void testGetNext() throws Exception {
        TestUtil.printTitle("BigHashTest:testGetNext()");
        // get
        BigHash max = HashSpan.LAST;
        BigHash next = max.getNext();

        // verify
        assertEquals(next.toString(), HashSpan.FIRST.toString());
    }

    public void testGetLast() throws Exception {
        TestUtil.printTitle("BigHashTest:testGetLast()");
        // get
        BigHash max = HashSpan.FIRST;
        BigHash last = max.getPrevious();

        // verify
        assertEquals(last.toString(), HashSpan.LAST.toString());
    }

    /**
     * Verifies that the customized equals method works. This was optimized explicitly for the BigHash object.
     */
    public void testCustomEqualsMethod() throws Exception {
        TestUtil.printTitle("BigHashTest:testCustomEqualsMethod()");
        // make a 100 different hashes and test that the equals method works
        for (int i = 0; i < 100; i++) {
            byte[] bytes = Utils.makeRandomData(100);
            // same hash from the same bytes should be the same
            BigHash a = new BigHash(bytes);
            BigHash b = new BigHash(bytes);
            assertEquals(a, b);
            // convert to a string
            String base64 = a.toString();
            BigHash c = BigHash.createHashFromString(base64);
            BigHash d = BigHash.createHashFromString(base64);
            assertEquals(c, d);
            assertEquals(c, a);
            assertEquals(c, b);

            // negative test
            bytes[0]++;
            BigHash e = new BigHash(bytes);
            assertFalse(a.equals(e));
            assertFalse(b.equals(e));
            assertFalse(c.equals(e));
            assertFalse(d.equals(e));

            // check for different hash codes
            assertEquals(a.hashCode(), b.hashCode());
            assertEquals(b.hashCode(), b.hashCode());
            assertEquals(c.hashCode(), b.hashCode());
            assertEquals(d.hashCode(), b.hashCode());
            assertFalse(a.hashCode() == e.hashCode());

            // check the shared byte array case
            byte[] big = new byte[BigHash.HASH_LENGTH * 2];
            System.arraycopy(a.toByteArray(), 0, big, 0, BigHash.HASH_LENGTH);
            System.arraycopy(e.toByteArray(), 0, big, BigHash.HASH_LENGTH, BigHash.HASH_LENGTH);

            // make the shared hashes
            BigHash f = BigHash.createFromBytes(big, 0);
            BigHash g = BigHash.createFromBytes(big, BigHash.HASH_LENGTH);
            assertFalse(f.equals(g));
            assertTrue(f.hashCode() != g.hashCode());
            // check that it still matches the old
            assertEquals(a, f);
            assertEquals(e, g);
        }
    }

    public void testLongConversion() throws Exception {
        TestUtil.printTitle("BigHashTest:testLongConversion()");
        int seed = 0;
        Random rand = new Random(seed);
        for (int i = 0; i < 100; i++) {
            byte[] b = new byte[8];
            rand.nextBytes(b);
            // check conversion
            compareByteBufferLongVersusManualConversion(b);
        }
    }

    private void compareByteBufferLongVersusManualConversion(final byte[] b) {

        // wrap with a byte buffer
        ByteBuffer bb = ByteBuffer.wrap(b);
        long a = bb.getLong();

        // manually convert the long
        long l = 0;
        l |= b[0] & 0xFF;
        l <<= 8;
        l |= b[1] & 0xFF;
        l <<= 8;
        l |= b[2] & 0xFF;
        l <<= 8;
        l |= b[3] & 0xFF;
        l <<= 8;
        l |= b[4] & 0xFF;
        l <<= 8;
        l |= b[5] & 0xFF;
        l <<= 8;
        l |= b[6] & 0xFF;
        l <<= 8;
        l |= b[7] & 0xFF;

        // check that they are equals
        assertEquals("Long values should be the same.", a, l);
    }

    /**
     *Checks that a known amount of data hashes correctly.
     */
    public void testHashingByteArray() throws Exception {
        TestUtil.printTitle("BigHashTest:testHashingByteArray()");
        // max file size
        int maxFileSize = 10000;
        // make up some data
        byte[] data = new byte[(int) (Math.random() * maxFileSize)];
        RandomUtil.getBytes(data);
        // hash the bytes
        BigHash hash = new BigHash(data);
        // check the hashes
        assertEquals("Length should match.", data.length, hash.getLength());
        // check the md5
        ByteBuffer md5 = hash.getMD5();
        byte[] md5Check = SecurityUtil.hash(data, "MD5");
        DevUtil.assertBytesMatch(md5Check, md5);
        ByteBuffer sha1 = hash.getSHA1();
        byte[] sha1Check = SecurityUtil.hash(data, "SHA-1");
        DevUtil.assertBytesMatch(sha1Check, sha1);
    }

    public void testHashingFile() throws Exception {
        File dir = TempFileUtil.createTemporaryDirectory();

        // max file size
        int maxFileSize = 1000000;
        // make up some data
        File file = new File(dir, "example.file");
        Utils.makeTempFile(file, (long) (Math.random() * maxFileSize));

        // hash the bytes
        BigHash hash = new BigHash(file);
        // check the hashes
        assertEquals("Length should match.", file.length(), hash.getLength());
        // check the md5
        ByteBuffer md5 = hash.getMD5();
        byte[] md5Check = SecurityUtil.hash(file, "MD5");
        DevUtil.assertBytesMatch(md5Check, md5);
        ByteBuffer sha1 = hash.getSHA1();
        byte[] sha1Check = SecurityUtil.hash(file, "SHA-1");
        DevUtil.assertBytesMatch(sha1Check, sha1);
    }

    /**
     *Tests if the data comes from an InputStream.
     */
    public void testHashingInputStream() throws Exception {
        // max file size
        int maxFileSize = DataBlockUtil.ONE_MB;
        // make up some data
        byte[] data = new byte[(int) (Math.random() * maxFileSize)];
        RandomUtil.getBytes(data);
        // convert to a stream
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        // hash the bytes
        BigHash hash = new BigHash(bais, data.length);
        // check the hashes
        assertEquals("Length should match.", data.length, hash.getLength());
        // check the md5
        ByteBuffer md5 = hash.getMD5();
        byte[] md5Check = SecurityUtil.hash(data, "MD5");
        DevUtil.assertBytesMatch(md5Check, md5);
        ByteBuffer sha1 = hash.getSHA1();
        byte[] sha1Check = SecurityUtil.hash(data, "SHA-1");
        DevUtil.assertBytesMatch(sha1Check, sha1);
    }

    /**
     *Tests if the data comes from an InputStream.
     */
    public void testPaddedInputStream() throws Exception {
        // max file size
        int maxFileSize = DataBlockUtil.ONE_MB;
        // make up some data
        byte[] data = new byte[(int) (Math.random() * maxFileSize)];
        // make up some padding
        byte[] padding = new byte[(int) (Math.random() * maxFileSize)];
        RandomUtil.getBytes(data);
        RandomUtil.getBytes(padding);
        // convert to a stream
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        // hash the bytes
        BigHash hash = new BigHash(bais, data.length, padding);
        // check the hashes
        assertEquals("Length should match.", data.length + padding.length, hash.getLength());

        // check compared to hasing one big array
        byte[] oneBigArray = new byte[data.length + padding.length];
        System.arraycopy(data, 0, oneBigArray, 0, data.length);
        System.arraycopy(padding, 0, oneBigArray, data.length, padding.length);
        // make the hash
        BigHash oneBigHash = new BigHash(oneBigArray);
        assertTrue("Hashes should match.", hash.compareTo(oneBigHash) == 0);
    }

    /**
     *Tests if the data comes from an InputStream that has extra bytes.
     */
    public void testHashingInputStreamExtra() throws Exception {
        // max file size
        int maxFileSize = DataBlockUtil.ONE_MB;
        // make up some data
        byte[] data = new byte[(int) (Math.random() * maxFileSize)];
        RandomUtil.getBytes(data);
        // make a slightly bigger array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(data);
        baos.write((byte) 0);
        baos.write((byte) 1);
        baos.write((byte) 2);
        // convert to a stream
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        // hash the bytes
        BigHash hash = new BigHash(bais, data.length);
        // check the hashes
        assertEquals("Length should match.", data.length, hash.getLength());
        // check the md5
        ByteBuffer md5 = hash.getMD5();
        byte[] md5Check = SecurityUtil.hash(data, "MD5");
        DevUtil.assertBytesMatch(md5Check, md5);
        ByteBuffer sha1 = hash.getSHA1();
        byte[] sha1Check = SecurityUtil.hash(data, "SHA-1");
        DevUtil.assertBytesMatch(sha1Check, sha1);
    }

    public void testSerialization() throws Exception {
        File dir = TempFileUtil.createTemporaryDirectory();

        // max file size
        int maxFileSize = 10000;
        // make up some data
        byte[] data = new byte[(int) (Math.random() * maxFileSize)];
        RandomUtil.getBytes(data);
        // hash the bytes
        BigHash hash = new BigHash(data);
        // check the hashes
        assertEquals("Length should match.", data.length, hash.getLength());
        // check the md5
        ByteBuffer md5 = hash.getMD5();
        byte[] md5Check = SecurityUtil.hash(data, "MD5");
        DevUtil.assertBytesMatch(md5Check, md5);
        ByteBuffer sha1 = hash.getSHA1();
        byte[] sha1Check = SecurityUtil.hash(data, "SHA-1");
        DevUtil.assertBytesMatch(sha1Check, sha1);

        // save the hash
        File temp = new File(dir, "temp.hash");
        FileOutputStream fos = new FileOutputStream(temp);
        fos.write(hash.toByteArray());
        fos.flush();
        fos.close();

        // read the hash
        BigHash check = BigHash.createFromBytes(IOUtil.getBytes(temp));
        // assert they match
        assertEquals("Lengths should match.", hash.getLength(), check.getLength());
        DevUtil.assertBytesMatch(hash.getMD5(), check.getMD5());
        DevUtil.assertBytesMatch(hash.getSHA1(), check.getSHA1());
        DevUtil.assertBytesMatch(hash.getSHA256(), check.getSHA256());
    }

    /**
     * <p>If data chunks are empty, what happens?</p>
     * @throws java.lang.Exception
     */
    public void testEmptyChunkHash() throws Exception {
        byte[] bytes1 = new byte[0];
        BigHash hash1 = new BigHash(bytes1);

        byte[] bytes2 = new byte[1024];
        BigHash hash2 = new BigHash(bytes2);

        byte[] bytes3 = new byte[1024 * 1024];
        BigHash hash3 = new BigHash(bytes3);
    }

    /**
     * <p>Tests a DevUtil method to create BigHash with prefix. Oh boy, a test for a test utility method.</p>
     * @throws java.lang.Exception
     */
    public void testCreateRandomBigHashWithSamePrefix() throws Exception {

        // Empty scenario should not have any problems
        String prefix = "";

        BigHash hash1 = DevUtil.getRandomBigHashStartsWith(prefix);
        BigHash hash2 = DevUtil.getRandomBigHashStartsWith(prefix);

        System.out.println("Should start with: " + prefix);
        System.out.println("  Hash #1: " + hash1);
        System.out.println("  Hash #2: " + hash2);

        assertTrue("Hash should start with prefix.", hash1.toString().startsWith(prefix));
        assertTrue("Hash should start with prefix.", hash2.toString().startsWith(prefix));

        // 1 character scenario
        prefix = "1";

        hash1 = DevUtil.getRandomBigHashStartsWith(prefix);
        hash2 = DevUtil.getRandomBigHashStartsWith(prefix);

        System.out.println("Should start with: " + prefix);
        System.out.println("  Hash #1: " + hash1);
        System.out.println("  Hash #2: " + hash2);

        assertTrue("Hash should start with prefix.", hash1.toString().startsWith(prefix));
        assertTrue("Hash should start with prefix.", hash2.toString().startsWith(prefix));

        // 2 character scenario
        prefix = "a1";

        hash1 = DevUtil.getRandomBigHashStartsWith(prefix);
        hash2 = DevUtil.getRandomBigHashStartsWith(prefix);

        System.out.println("Should start with: " + prefix);
        System.out.println("  Hash #1: " + hash1);
        System.out.println("  Hash #2: " + hash2);

        assertTrue("Hash should start with prefix.", hash1.toString().startsWith(prefix));
        assertTrue("Hash should start with prefix.", hash2.toString().startsWith(prefix));

        // 4 character scenario
        prefix = "defg";

        hash1 = DevUtil.getRandomBigHashStartsWith(prefix);
        hash2 = DevUtil.getRandomBigHashStartsWith(prefix);

        System.out.println("Should start with: " + prefix);
        System.out.println("  Hash #1: " + hash1);
        System.out.println("  Hash #2: " + hash2);

        assertTrue("Hash should start with prefix.", hash1.toString().startsWith(prefix));
        assertTrue("Hash should start with prefix.", hash2.toString().startsWith(prefix));

        // 6 character scenario
        prefix = "bcdefg";

        hash1 = DevUtil.getRandomBigHashStartsWith(prefix);
        hash2 = DevUtil.getRandomBigHashStartsWith(prefix);

        System.out.println("Should start with: " + prefix);
        System.out.println("  Hash #1: " + hash1);
        System.out.println("  Hash #2: " + hash2);

        assertTrue("Hash should start with prefix.", hash1.toString().startsWith(prefix));
        assertTrue("Hash should start with prefix.", hash2.toString().startsWith(prefix));

        // 8 character scenario
        prefix = "abcdefgh";

        hash1 = DevUtil.getRandomBigHashStartsWith(prefix);
        hash2 = DevUtil.getRandomBigHashStartsWith(prefix);

        System.out.println("Should start with: " + prefix);
        System.out.println("  Hash #1: " + hash1);
        System.out.println("  Hash #2: " + hash2);

        assertTrue("Hash should start with prefix.", hash1.toString().startsWith(prefix));
        assertTrue("Hash should start with prefix.", hash2.toString().startsWith(prefix));

        // Full hash scenario
        prefix = DevUtil.getRandomBigHash().toString();

        hash1 = DevUtil.getRandomBigHashStartsWith(prefix);
        hash2 = DevUtil.getRandomBigHashStartsWith(prefix);

        assertTrue("Hash should start with prefix.", hash1.toString().startsWith(prefix));
        assertTrue("Hash should start with prefix.", hash2.toString().startsWith(prefix));

        // Negative test: scenario with prefix larger than BigHash
        prefix = "a" + DevUtil.getRandomBigHash().toString();

        try {
            hash1 = DevUtil.getRandomBigHashStartsWith(prefix);
            fail("Should have thrown an exception, prefix larger than hash!!!");
        } catch (Exception ex) { /* nope, expected */ }
    }

    public void testCreateDataChunkStartingWithPrefixOfSize1() throws Exception {
        testCreateDataChunkStartingWithPrefix(1);
    }

    public void testCreateDataChunkStartingWithPrefixOfSize2() throws Exception {
        testCreateDataChunkStartingWithPrefix(2);
    }

    public void testCreateDataChunkStartingWithPrefixOfSize3() throws Exception {
        try {
            testCreateDataChunkStartingWithPrefix(3);
            fail("Should throw an exception, max prefix size is 2.");
        } catch (Exception ex) {
            // Expected, should throw exception.
        }
    }

    public void testCreateDataChunkStartingWithPrefixOfSize4() throws Exception {
        try {
            testCreateDataChunkStartingWithPrefix(4);
            fail("Should throw an exception, max prefix size is 2.");
        } catch (Exception ex) {
            // Expected, should throw exception.
        }
    }

    /**
     * <p>Tests DevUtil method that creates data chunks whose hash would start with a specific prefix.</p>
     * @throws java.lang.Exception
     */
    public void testCreateDataChunkStartingWithPrefix(int prefixSize) throws Exception {
        String prefix = RandomUtil.getString(prefixSize);
        final int numChunksToTest = 10;
//        System.out.println();
//        System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
//        System.out.println(" Using prefix: "+prefix);
//        System.out.println("   Each of "+numChunksToTest+" chunks' hashes should start with the above prefix.");
//        System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
//        System.out.println();
//
//        final long totalStart = TimeUtil.getTrancheTimestamp();
        for (int i = 0; i < numChunksToTest; i++) {
//            final long start = TimeUtil.getTrancheTimestamp();

            // Size will be between 1024 bytes and a MB
            byte[] dataChunk = DevUtil.createRandomDataChunkStartsWith(prefix);
            BigHash nextHash = new BigHash(dataChunk);

            assertTrue("Expecting hash to start with prefix " + prefix + ", but doesn't: " + nextHash, nextHash.toString().startsWith(prefix));

//            System.out.println("  "+String.valueOf(i+1)+" of "+String.valueOf(numChunksToTest)+") Took "+Text.getPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp()-start)+" to get chunk of size "+Text.getFormattedBytes(dataChunk.length)+", hash="+nextHash);
        }

//        final long totalRuntime = TimeUtil.getTrancheTimestamp()-totalStart;
//        final double avg = (double)totalRuntime/(double)numChunksToTest;
//        System.out.println();
//        System.out.println("Test ran for "+Text.getPrettyEllapsedTimeString(totalRuntime)+" with average chunk taking "+Text.getPrettyEllapsedTimeString(avg)+" time to generate.");
    }

    public void testCreateDataChunkNotStartWithPrefixOfSize1() throws Exception {
        testCreateDataChunkNotStartWithPrefix(1);
    }

    public void testCreateDataChunkNotStartWithPrefixOfSize2() throws Exception {
        testCreateDataChunkNotStartWithPrefix(2);
    }

    public void testCreateDataChunkNotStartWithPrefixOfSize3() throws Exception {
        try {
            testCreateDataChunkNotStartWithPrefix(3);
            fail("Should throw an exception, max prefix size is 2.");
        } catch (Exception ex) {
            // Expected, should throw exception.
        }
    }

    public void testCreateDataChunkNotStartWithPrefixOfSize4() throws Exception {
        try {
            testCreateDataChunkNotStartWithPrefix(4);
            fail("Should throw an exception, max prefix size is 2.");
        } catch (Exception ex) {
            // Expected, should throw exception.
        }
    }

    /**
     * <p>Tests DevUtil method that creates data chunks whose hash will not ever start with a specific prefix.</p>
     * @throws java.lang.Exception
     */
    public void testCreateDataChunkNotStartWithPrefix(int prefixSize) throws Exception {
        String prefix = RandomUtil.getString(prefixSize);
        final int numChunksToTest = 200;
//        System.out.println();
//        System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
//        System.out.println(" Using prefix: "+prefix);
//        System.out.println("   Each of "+numChunksToTest+" chunks' hashes should NOT start with the above prefix.");
//        System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
//        System.out.println();
//
//        final long totalStart = TimeUtil.getTrancheTimestamp();
        for (int i = 0; i < numChunksToTest; i++) {
//            final long start = TimeUtil.getTrancheTimestamp();

            // Size will be between 1024 bytes and a MB
            byte[] dataChunk = DevUtil.createRandomDataChunkDoesNotStartWith(prefix);
            BigHash nextHash = new BigHash(dataChunk);

            assertFalse("Expecting hash to not start with prefix " + prefix + ", but does: " + nextHash, nextHash.toString().startsWith(prefix));

//            System.out.println("  "+String.valueOf(i+1)+" of "+String.valueOf(numChunksToTest)+") Took "+Text.getPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp()-start)+" to get chunk of size "+Text.getFormattedBytes(dataChunk.length)+", hash="+nextHash);
        }

//        final long totalRuntime = TimeUtil.getTrancheTimestamp()-totalStart;
//        final double avg = (double)totalRuntime/(double)numChunksToTest;
//        System.out.println();
//        System.out.println("Test ran for "+Text.getPrettyEllapsedTimeString(totalRuntime)+" with average chunk taking "+Text.getPrettyEllapsedTimeString(avg)+" time to generate.");
    }
}