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

import SevenZip.LzmaAlone;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarOutputStream;

/**
 * <p>Helper methods for using various compression encodings. Includes methods that use temporary files and methods that don't.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class CompressionUtil {

    /**
     * 
     * @param directory
     * @return
     * @throws java.io.IOException
     */
    public static final File tgzCompress(File directory) throws IOException {
        File file = tarCompress(directory);
        file = gzipCompress(file);
        File renameTo = new File(file.getAbsolutePath().replace(".gzip", ".tgz"));
        IOUtil.renameFallbackCopy(file, renameTo);
        return renameTo;
    }

    /**
     * 
     * @param directory
     * @return
     * @throws java.io.IOException
     */
    public static final File tbzCompress(File directory) throws IOException {
        File file = tarCompress(directory);
        file = bzip2Compress(file);
        File renameTo = new File(file.getAbsolutePath().replace(".bzip2", ".tbz"));
        IOUtil.renameFallbackCopy(file, renameTo);
        return renameTo;
    }

    /**
     * 
     * @param directory
     * @return
     * @throws java.io.IOException
     */
    public static final File tarCompress(File directory) throws IOException {
        File file = null;
        FileOutputStream fos = null;
        TarOutputStream tos = null;
        try {
            file = TempFileUtil.createTemporaryFile(".tar");
            fos = new FileOutputStream(file);
            tos = new TarOutputStream(fos);
            for (File subFile : directory.listFiles()) {
                TarEntry ze = new TarEntry(subFile.getName());
                byte[] bytes = IOUtil.getBytes(subFile);
                ze.setSize(bytes.length);
                tos.putNextEntry(ze);
                tos.write(bytes);
                tos.closeEntry();
            }
        } finally {
            IOUtil.safeClose(tos);
            IOUtil.safeClose(fos);
        }
        return file;
    }

    /**
     * 
     * @param file
     * @param directoryName
     * @return
     * @throws java.io.IOException
     */
    public static final File tarDecompress(File file, String directoryName) throws IOException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        TarInputStream tis = null;
        File directory = null;
        try {
            directory = TempFileUtil.createTemporaryDirectory(directoryName);
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            tis = new TarInputStream(bis);
            // read the entries
            for (TarEntry te = tis.getNextEntry(); te != null; te = tis.getNextEntry()) {
                if (te.isDirectory()) {
                    continue;
                }
                // make a temp file
                File tempFile = new File(directory, te.getName());
                FileOutputStream fos = null;
                BufferedOutputStream bos = null;
                try {
                    fos = new FileOutputStream(tempFile);
                    bos = new BufferedOutputStream(fos);
                    IOUtil.getBytes(tis, bos);
                } finally {
                    IOUtil.safeClose(bos);
                    IOUtil.safeClose(fos);
                }
            }
        } finally {
            IOUtil.safeClose(tis);
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
        }
        return directory;
    }

    /**
     * 
     * @param directory
     * @return
     * @throws java.io.IOException
     */
    public static final File zipCompress(File directory) throws IOException {
        File file = null;
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            file = TempFileUtil.createTemporaryFile(".zip");
            fos = new FileOutputStream(file);
            zos = new ZipOutputStream(fos);
            for (File subFile : directory.listFiles()) {
                ZipEntry ze = new ZipEntry(subFile.getName());
                zos.putNextEntry(ze);
                zos.write(IOUtil.getBytes(subFile));
                zos.flush();
            }
        } finally {
            IOUtil.safeClose(zos);
            IOUtil.safeClose(fos);
        }
        return file;
    }

    /**
     * 
     * @param file
     * @return
     * @throws java.io.IOException
     */
    public static final File zipDecompress(File file, String directoryName) throws IOException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ZipInputStream zis = null;
        File directory = null;
        try {
            directory = TempFileUtil.createTemporaryDirectory(directoryName);
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            zis = new ZipInputStream(bis);
            // read the entries
            for (ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {
                // only upload if it is a file
                if (ze.isDirectory()) {
                    continue;
                }
                // make a temp file
                File tempFile = new File(directory, ze.getName());
                tempFile.createNewFile();
                FileOutputStream fos = null;
                BufferedOutputStream bos = null;
                try {
                    fos = new FileOutputStream(tempFile);
                    bos = new BufferedOutputStream(fos);
                    IOUtil.getBytes(zis, bos);
                } finally {
                    IOUtil.safeClose(bos);
                    IOUtil.safeClose(fos);
                }
            }
        } finally {
            IOUtil.safeClose(zis);
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
        }
        return directory;
    }

    /**
     * <p>GZIPs the in memory and returns the compressed bytes. Avoids any time penalties associated with making/using files.</p>
     * @param dataBytes
     * @param padding
     * @return
     * @throws java.io.IOException
     */
    public static final byte[] gzipCompress(byte[] dataBytes, byte[] padding) throws IOException {
        // transfer the file
        ByteArrayInputStream fis = null;
        ByteArrayOutputStream fos = null;
        GZIPOutputStream gos = null;
        try {
            // make the streams
            fis = new ByteArrayInputStream(dataBytes);
            fos = new ByteArrayOutputStream();
            gos = new GZIPOutputStream(fos);
            // write out the content
            IOUtil.getBytes(fis, gos);
            gos.write(padding);
        } finally {
            IOUtil.safeClose(fis);
            IOUtil.safeClose(gos);
            IOUtil.safeClose(fos);
        }
        // return the gzip'd content
        return fos.toByteArray();
    }

    /**
     * <p>Decompresses the input bytes assuming that it is GZIP'd. Completely avoids the use of temporary files, which saves associated time.</p>
     * @param dataBytes
     * @return
     * @throws java.io.IOException
     */
    public static final byte[] gzipDecompress(byte[] dataBytes) throws IOException {
        // transfer the file
        ByteArrayInputStream fis = null;
        ByteArrayOutputStream fos = null;
        GZIPInputStream gis = null;
        try {
            // make the streams
            fis = new ByteArrayInputStream(dataBytes);
            fos = new ByteArrayOutputStream();
            gis = new GZIPInputStream(fis);
            // write out the content
            IOUtil.getBytes(gis, fos);
        } finally {
            IOUtil.safeClose(gis);
            IOUtil.safeClose(fis);
            IOUtil.safeClose(fos);
        }
        // return the gzip'd content
        return fos.toByteArray();
    }

    /**
     * <p>GZIPs the input file and returns a pointer to a file that is GZIP compressed.</p>
     * @param input
     * @return
     * @throws java.io.IOException
     */
    public static final File gzipCompress(File input) throws IOException {
        // make a temp file
        File gzip = TempFileUtil.createTemporaryFile(".gzip");
        // transfer the file
        FileInputStream fis = null;
        FileOutputStream fos = null;
        GZIPOutputStream gos = null;
        try {
            // make the streams
            fis = new FileInputStream(input);
            fos = new FileOutputStream(gzip);
            gos = new GZIPOutputStream(fos);
            // write out the content
            IOUtil.getBytes(fis, gos);
        } finally {
            IOUtil.safeClose(fis);
            IOUtil.safeClose(gos);
            IOUtil.safeClose(fos);
        }
        // return the gzip'd content
        return gzip;
    }

    /**
     * <p>Decompresses the input file assuming that it is GZIP'd.</p>
     * @param input
     * @return
     * @throws java.io.IOException
     */
    public static final File gzipDecompress(File input) throws IOException {
        // make a temp file
        File gzip = TempFileUtil.createTemporaryFile();
        // transfer the file
        FileInputStream fis = null;
        FileOutputStream fos = null;
        GZIPInputStream gis = null;
        try {
            // make the streams
            fis = new FileInputStream(input);
            fos = new FileOutputStream(gzip);
            gis = new GZIPInputStream(fis);
            // write out the content
            IOUtil.getBytes(gis, fos);
        } finally {
            IOUtil.safeClose(gis);
            IOUtil.safeClose(fis);
            IOUtil.safeClose(fos);
        }
        return gzip;
    }

    /**
     * <p>bzip2 compresses the input file and returns a reference to the compressed file.</p>
     * @param input
     * @return
     * @throws java.io.IOException
     */
    public static final File bzip2Compress(File input) throws IOException {
        // make a temp file
        File compressed = TempFileUtil.createTemporaryFile(".bzip2");
        // transfer the file
        FileInputStream fis = null;
        FileOutputStream fos = null;
        CBZip2OutputStream gos = null;
        try {
            // make the streams
            fis = new FileInputStream(input);
            fos = new FileOutputStream(compressed);
            gos = new CBZip2OutputStream(fos);
            // write out the content
            IOUtil.getBytes(fis, gos);
        } finally {
            IOUtil.safeClose(fis);
            IOUtil.safeClose(gos);
            IOUtil.safeClose(fos);
        }
        // return the gzip'd content
        return compressed;
    }

    /**
     * <p>Decompresses the input file assuming that it is bzip2 compressed.</p>
     * @param input
     * @return
     * @throws java.io.IOException
     */
    public static final File bzip2Decompress(File input) throws IOException {
        // make a temp file
        File decompressed = TempFileUtil.createTemporaryFile();
        // transfer the file
        FileInputStream fis = null;
        FileOutputStream fos = null;
        CBZip2InputStream gis = null;
        try {
            // make the streams
            fis = new FileInputStream(input);
            fos = new FileOutputStream(decompressed);
            gis = new CBZip2InputStream(fis);
            // write out the content
            IOUtil.getBytes(gis, fos);
        } finally {
            IOUtil.safeClose(gis);
            IOUtil.safeClose(fis);
            IOUtil.safeClose(fos);
        }
        // return the gzip'd content
        return decompressed;
    }

    /**
     * <p>Decompresses the input file assuming that it is LZMA compressed.</p>
     * @param input
     * @return
     * @throws java.lang.Exception
     */
    public static final File lzmaDecompress(File input) throws Exception {
        // make a temp file
        File decompressed = TempFileUtil.createTemporaryFile();
        // decompress using LZMA
        LzmaAlone.main(new String[]{"d", input.getAbsolutePath(), decompressed.getAbsolutePath()});
        return decompressed;
    }

    /**
     * <p>Decompresses the input bytes assuming that it is LZMA compressed.</p>
     * @param bytes
     * @return
     * @throws java.lang.Exception
     */
    public static final byte[] lzmaDecompress(byte[] bytes) throws Exception {
        // file for storing the encoded bytes
        File tempFile = TempFileUtil.createTemporaryFile();
        try {
            FileOutputStream fos = new FileOutputStream(tempFile);
            try {
                fos.write(bytes);
            } finally {
                IOUtil.safeClose(fos);
            }
            // get the bytes back in memory
            return IOUtil.getBytes(lzmaDecompress(tempFile));
        } finally {
            IOUtil.safeDelete(tempFile);
        }
    }

    /**
     * <p>Compresses the input file using LZMA.</p>
     * @param input
     * @return
     * @throws java.lang.Exception
     */
    public static final File lzmaCompress(File input) throws Exception {
        // make a temp file
        File compressed = TempFileUtil.createTemporaryFile(".lzma");

        // compress using LZMA
        LzmaAlone.main(new String[]{"e", input.getCanonicalPath(), compressed.getCanonicalPath()});

        // return the gzip'd content
        return compressed;
    }
}
