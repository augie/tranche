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

import org.tranche.commons.RandomUtil;
import java.io.File;
import org.tranche.exceptions.TodoException;
import org.tranche.flatfile.DataBlockUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class CompressionUtilTest extends TrancheTestCase {

    public void testGZIP() throws Exception {
        TestUtil.printTitle("CompressionUtil:testGZIP()");
        File file = DevUtil.createTestFile(1, DataBlockUtil.getMaxChunkSize() / 2);
        File compressedFile = CompressionUtil.gzipCompress(file);
        File decompressedFile = CompressionUtil.gzipDecompress(compressedFile);
        TestUtil.assertFilesAreEquivalent(file, decompressedFile);
    }

    public void testLZMA() throws Exception {
        TestUtil.printTitle("CompressionUtil:testLZMA()");
        File file = DevUtil.createTestFile(1, DataBlockUtil.getMaxChunkSize() / 2);
        File compressedFile = CompressionUtil.lzmaCompress(file);
        File decompressedFile = CompressionUtil.lzmaDecompress(compressedFile);
        TestUtil.assertFilesAreEquivalent(file, decompressedFile);
    }

    public void testBZIP2() throws Exception {
        TestUtil.printTitle("CompressionUtil:testBZIP2()");
        File file = DevUtil.createTestFile(1, DataBlockUtil.getMaxChunkSize() / 2);
        File compressedFile = CompressionUtil.bzip2Compress(file);
        File decompressedFile = CompressionUtil.bzip2Decompress(compressedFile);
        TestUtil.assertFilesAreEquivalent(file, decompressedFile);
    }

    public void testTAR() throws Exception {
        TestUtil.printTitle("CompressionUtil:testTAR()");
        throw new TodoException();
    }

    public void testTGZ() throws Exception {
        TestUtil.printTitle("CompressionUtil:testTGZ()");
        throw new TodoException();
    }

    public void testTBZ() throws Exception {
        TestUtil.printTitle("CompressionUtil:testTBZ()");
        throw new TodoException();
    }

    public void testZipSingleFile() throws Exception {
        File original = null, zipped = null, unzippedDir = null;

        try {
            original = TempFileUtil.createTemporaryFile();

            final int size = 1024 * 1024 - 1024;

            DevUtil.createTestFile(original, size);
            
            assertEquals("Expecting file of certain size.", size, original.length());
            
            zipped = CompressionUtil.zipCompress(original);
            
            AssertionUtil.assertBytesDifferent(IOUtil.getBytes(original), IOUtil.getBytes(zipped));
            
            unzippedDir = CompressionUtil.zipDecompress(zipped, "testZipSingleFile_unzip");
            assertEquals("Expecting one file unzipped.", 1, unzippedDir.list().length);
            
            AssertionUtil.assertBytesSame(IOUtil.getBytes(original), IOUtil.getBytes(unzippedDir.listFiles()[0]));
        } finally {
            IOUtil.safeDelete(original);
            IOUtil.safeDelete(zipped);
            IOUtil.recursiveDeleteWithWarning(unzippedDir);
        }
    }

    public void testZipDirectory() throws Exception {

        File original = null, zipped = null, unzippedDir = null;

        try {
            
            final int numFilesInProject = RandomUtil.getInt(9) + 1;
            final int minFileSize = 1024;
            final int maxFileSize = 1024 * 1024 - 1024;
            original = DevUtil.createTestProject(numFilesInProject, minFileSize, maxFileSize);
            
            assertEquals("Expecting certain number of children.", numFilesInProject, original.list().length);
            
            zipped = CompressionUtil.zipCompress(original);
            
            assertNotNull(zipped);
            assertTrue("Zipped file should exist.", zipped.exists());
            
            unzippedDir = CompressionUtil.zipDecompress(zipped, "testZipDirectory-unzip");
            
            AssertionUtil.assertSame(original, unzippedDir);
            
        } finally {
            IOUtil.recursiveDeleteWithWarning(original);
            IOUtil.safeDelete(zipped);
            IOUtil.recursiveDeleteWithWarning(unzippedDir);
        }
    }
}
