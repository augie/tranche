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

import java.io.File;
import org.tranche.exceptions.TodoException;
import org.tranche.flatfile.DataBlockUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class CompressionUtilTest extends TrancheTestCase {

    public void testGZIP() throws Exception {
        TestUtil.printTitle("CompressionUtil:testGZIP()");
        File file = DevUtil.createTestFile(1, DataBlockUtil.ONE_MB / 2);
        File compressedFile = CompressionUtil.gzipCompress(file);
        File decompressedFile = CompressionUtil.gzipDecompress(compressedFile);
        TestUtil.assertFilesAreEquivalent(file, decompressedFile);
    }

    public void testLZMA() throws Exception {
        TestUtil.printTitle("CompressionUtil:testLZMA()");
        File file = DevUtil.createTestFile(1, DataBlockUtil.ONE_MB / 2);
        File compressedFile = CompressionUtil.lzmaCompress(file);
        File decompressedFile = CompressionUtil.lzmaDecompress(compressedFile);
        TestUtil.assertFilesAreEquivalent(file, decompressedFile);
    }

    public void testBZIP2() throws Exception {
        TestUtil.printTitle("CompressionUtil:testBZIP2()");
        File file = DevUtil.createTestFile(1, DataBlockUtil.ONE_MB / 2);
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

    public void testZIP() throws Exception {
        TestUtil.printTitle("CompressionUtil:testZIP()");
        throw new TodoException();
    }
}
