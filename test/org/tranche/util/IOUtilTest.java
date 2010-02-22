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
package org.tranche.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import org.tranche.hash.BigHash;

/**
 *
 * @author Bryan
 */
public class IOUtilTest extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testURLParser() throws Exception {
        TestUtil.printTitle("IOUtilTest:testURLParser()");

        String URL1 = "http://www.proteomecommons.org:80";

        assertEquals("http://", IOUtil.parseProtocol(URL1));
        assertEquals("www.proteomecommons.org", IOUtil.parseHost(URL1));
        assertEquals(80, IOUtil.parsePort(URL1));

        String URL2 = "tranche://127.0.0.1:1500";

        assertEquals("tranche://", IOUtil.parseProtocol(URL2));
        assertEquals("127.0.0.1", IOUtil.parseHost(URL2));
        assertEquals(1500, IOUtil.parsePort(URL2));

        String URL3 = "ssh+tranche://123.123.123.1:773";

        assertEquals("ssh+tranche://", IOUtil.parseProtocol(URL3));
        assertEquals("123.123.123.1", IOUtil.parseHost(URL3));
        assertEquals(773, IOUtil.parsePort(URL3));
    }

    public void testReadWriteSafeDeleteCopy() throws Exception {
        TestUtil.printTitle("IOUtilTest:testReadWriteSafeDeleteCopy()");

        FileWriter writer = null;
        BufferedReader reader = null;

        File temp1 = null, temp1Copy = null;
        try {
            temp1 = TempFileUtil.createTemporaryFile(".txt");
            temp1Copy = TempFileUtil.createTemporaryFile(".txt.copy");
            writer = new FileWriter(temp1);

            // Write some data to it
            String poemLine1 = "I traded two spoons\\For a fork and a knife.",
                    poemLine2 = "I'm not a good poet\\So this doesn't rhyme.";
            IOUtil.writeLine(poemLine1, writer);
            IOUtil.writeLine(poemLine2, writer);

            IOUtil.safeClose(writer);

            // Read it back
            reader = new BufferedReader(new FileReader(temp1));
            assertEquals(poemLine1, IOUtil.readLine(reader));
            assertEquals(poemLine2, IOUtil.readLine(reader));

            IOUtil.safeClose(reader);

            // Copy the file
            IOUtil.copyFile(temp1, temp1Copy);

            // Read the copy
            reader = new BufferedReader(new FileReader(temp1Copy));
            assertEquals(poemLine1, IOUtil.readLine(reader));
            assertEquals(poemLine2, IOUtil.readLine(reader));

            // Shouldn't complain about safe deletes and nulls
            IOUtil.safeDelete(null);
            IOUtil.safeDelete(new File("/path/to/madness/ladfskjldjkfaslkadsjfldfaslkjdfadsfdas"));

        } finally {
            IOUtil.safeClose(writer);
            IOUtil.safeClose(reader);

            IOUtil.safeDelete(temp1);
            IOUtil.safeDelete(temp1Copy);
        }
    }

    /**
     *
     */
    public void testRecursiveCopyFailsOnEmptySrc() throws Exception {
        TestUtil.printTitle("IOUtilTest:testRecursiveCopyFailsOnEmptySrc()");

        File doesNotExist = TempFileUtil.createTemporaryDirectory();
        File dest = TempFileUtil.createTemporaryDirectory();

        if (doesNotExist.exists()) {
            IOUtil.recursiveDelete(doesNotExist);
        }

        if (dest.exists()) {
            IOUtil.recursiveDelete(dest);
        }

        try {
            IOUtil.recursiveCopyFiles(doesNotExist, dest);

            // Make sure files weren't made
            assertFalse("Should not exist.", doesNotExist.exists());
            assertFalse("Should not exist.", dest.exists());

            // Shouldn't get here, exception should be thrown, don't know why here...'
            fail("Should throw exception, no file exists.");
        } catch (Exception ex) {
            // Expected
        }
    }

    /**
     *
     */
    public void testRecursiveCopySucceedsSingleFile() throws Exception {
        TestUtil.printTitle("IOUtilTest:testRecursiveCopySucceedsSingleFile()");

        File singleFile = TempFileUtil.createTemporaryFile(".recursive-delete");
        File destFile = TempFileUtil.createTemporaryFile("file.tmp.recursive-delete-copy");

        try {
            // Put some content in singleFile
            byte[] data = new byte[RandomUtil.getInt(1024 * 1024 - 1024) + 1024];
            RandomUtil.getBytes(data);
            IOUtil.setBytes(data, singleFile);

            if (destFile.exists()) {
                IOUtil.safeDelete(destFile);
            }

            assertTrue("Better exist.", singleFile.exists());
            assertFalse("Better not exist.", destFile.exists());

            IOUtil.recursiveCopyFiles(singleFile, destFile);

            assertTrue("Better exist.", singleFile.exists());
            assertTrue("Better exist: " + destFile.getAbsolutePath(), destFile.exists());

            assertEquals("Files should have same size.", singleFile.length(), destFile.length());
        } finally {
            IOUtil.safeDelete(singleFile);
            IOUtil.safeDelete(destFile);
        }
    }

    /**
     *
     */
    public void testRecursiveCopySucceedsEmptyDirectory() throws Exception {
        TestUtil.printTitle("IOUtilTest:testRecursiveCopySucceedsEmptyDirectory()");

        File src = TempFileUtil.createTemporaryDirectory();
        File dest = TempFileUtil.createTemporaryDirectory();
        try {
            if (src.exists()) {
                IOUtil.recursiveDelete(src);
            }
            if (dest.exists()) {
                IOUtil.recursiveDelete(dest);
            }
            assertFalse("Better not exist.", src.exists());
            assertFalse("Better not exist.", dest.exists());

            src.mkdirs();

            assertTrue("Better exist.", src.exists());

            IOUtil.recursiveCopyFiles(src, dest);
            assertTrue("Better exist.", src.exists());
            assertTrue("Better exist.", dest.exists());

            assertEquals("Better be empty.", 0, src.list().length);
            assertEquals("Better be empty.", 0, dest.list().length);
        } finally {
            IOUtil.recursiveDelete(src);
            IOUtil.recursiveDelete(dest);
        }
    }

    /**
     *
     */
    public void testRecursiveCopySucceeds() throws Exception {
        TestUtil.printTitle("IOUtilTest:testRecursiveCopySucceeds()");

        File src = TempFileUtil.createTemporaryDirectory();
        File dest = TempFileUtil.createTemporaryDirectory();
        try {
            if (src.exists()) {
                IOUtil.recursiveDelete(src);
            }
            if (dest.exists()) {
                IOUtil.recursiveDelete(dest);
            }
            assertFalse("Better not exist.", src.exists());
            assertFalse("Better not exist.", dest.exists());

            src.mkdirs();

            assertTrue("Better exist.", src.exists());

            // Add a subdir to src
            File subdir1 = new File(src, "subdir1");
            subdir1.mkdirs();

            assertTrue("Better exist.", subdir1.exists());

            // Add a sibling file
            File file1 = new File(src, "file1");
            file1.createNewFile();

            assertTrue("Better exist.", file1.exists());

            byte[] data = new byte[RandomUtil.getInt(1024 * 1024 - 1024) + 1024];
            RandomUtil.getBytes(data);

            // Put some bytes
            IOUtil.setBytes(data, file1);

            // Make a file in the subdir
            File file2 = new File(subdir1, "file2");
            file2.createNewFile();

            assertTrue("Better exist.", file2.exists());

            data = new byte[RandomUtil.getInt(1024 * 1024 - 1024) + 1024];
            RandomUtil.getBytes(data);

            // Put some bytes
            IOUtil.setBytes(data, file2);

            // Create a subdir in the subdir. It'll remain empty.
            File subdir2 = new File(subdir1, "subdir2");
            subdir2.mkdirs();

            assertTrue("Better exist.", subdir2.exists());

            // Assert file structure
            assertEquals("Expecting two files.", 2, src.list().length);
            assertEquals("Expecting two files.", 2, subdir1.list().length);
            assertEquals("Better be empty.", 0, subdir2.list().length);

            // Perform copy
            IOUtil.recursiveCopyFiles(src, dest);

            // For my benefit, print to std out
            Text.printRecursiveDirectoryStructure(src);
            Text.printRecursiveDirectoryStructure(dest);

            assertTrue("Better exist.", src.exists());
            assertTrue("Better exist.", dest.exists());

            assertEquals("Better be empty.", 2, src.list().length);
            assertEquals("Better be empty.", 2, dest.list().length);

        } finally {
            IOUtil.recursiveDelete(src);
            IOUtil.recursiveDelete(dest);
        }
    }

    public void testSerialization() throws Exception {
        TestUtil.printTitle("IOUtilTest:testSerialization()");

        // test byte
        {
            byte expected = (byte)RandomUtil.getInt();

            // write
            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                IOUtil.writeByte(expected, baos);
            } finally {
                IOUtil.safeClose(baos);
            }

            // read
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(baos.toByteArray());
                assertEquals(expected, IOUtil.readByte(bais));
            } finally {
                IOUtil.safeClose(bais);
            }
        }

        // test bytes
        {
            byte[] expected = new byte[RandomUtil.getInt(10)+1];
            RandomUtil.getBytes(expected);

            // write
            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                IOUtil.writeBytes(expected, baos);
            } finally {
                IOUtil.safeClose(baos);
            }
            assertEquals(expected.length, baos.size());

            // read
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(baos.toByteArray());
                byte[] response = IOUtil.readBytes(expected.length, bais);
                for (int i = 0; i < expected.length; i++) {
                    assertEquals(expected[i], response[i]);
                }
            } finally {
                IOUtil.safeClose(bais);
            }
        }

        // test data
        {
            byte[] expected = new byte[RandomUtil.getInt(10)+1];
            RandomUtil.getBytes(expected);

            // write
            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                IOUtil.writeData(expected, baos);
            } finally {
                IOUtil.safeClose(baos);
            }

            // read
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(baos.toByteArray());
                byte[] response = IOUtil.readDataBytes(bais);
                for (int i = 0; i < expected.length; i++) {
                    assertEquals(expected[i], response[i]);
                }
            } finally {
                IOUtil.safeClose(bais);
            }

            // null
            expected = null;
            
            // write
            baos = null;
            try {
                baos = new ByteArrayOutputStream();
                IOUtil.writeData(expected, baos);
            } finally {
                IOUtil.safeClose(baos);
            }

            // read
            bais = null;
            try {
                bais = new ByteArrayInputStream(baos.toByteArray());
                byte[] response = IOUtil.readDataBytes(bais);
                assertNull(response);
            } finally {
                IOUtil.safeClose(bais);
            }
        }

        // test int
        {
            int expected = RandomUtil.getInt();

            // write
            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                IOUtil.writeInt(expected, baos);
            } finally {
                IOUtil.safeClose(baos);
            }

            // read
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(baos.toByteArray());
                assertEquals(expected, IOUtil.readInt(bais));
            } finally {
                IOUtil.safeClose(bais);
            }
        }

        // test long
        {
            long expected = RandomUtil.getLong();

            // write
            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                IOUtil.writeLong(expected, baos);
            } finally {
                IOUtil.safeClose(baos);
            }

            // read
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(baos.toByteArray());
                assertEquals(expected, IOUtil.readLong(bais));
            } finally {
                IOUtil.safeClose(bais);
            }
        }

        // test boolean
        {
            boolean expected = RandomUtil.getBoolean();

            // write
            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                IOUtil.writeBoolean(expected, baos);
            } finally {
                IOUtil.safeClose(baos);
            }

            // read
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(baos.toByteArray());
                assertEquals(expected, IOUtil.readBoolean(bais));
            } finally {
                IOUtil.safeClose(bais);
            }
        }

        // test hash
        {
            BigHash expected = DevUtil.getRandomBigHash();

            // write
            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                IOUtil.writeBigHash(expected, baos);
            } finally {
                IOUtil.safeClose(baos);
            }

            // read
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(baos.toByteArray());
                assertEquals(expected, IOUtil.readBigHash(bais));
            } finally {
                IOUtil.safeClose(bais);
            }

            expected = null;

            // write
            baos = null;
            try {
                baos = new ByteArrayOutputStream();
                IOUtil.writeBigHash(expected, baos);
            } finally {
                IOUtil.safeClose(baos);
            }

            // read
            bais = null;
            try {
                bais = new ByteArrayInputStream(baos.toByteArray());
                assertEquals(expected, IOUtil.readBigHash(bais));
            } finally {
                IOUtil.safeClose(bais);
            }
        }
    }
}
