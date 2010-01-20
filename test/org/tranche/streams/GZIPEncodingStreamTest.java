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
package org.tranche.streams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;
import org.tranche.hash.BigHash;
import org.tranche.util.CompressionUtil;
import org.tranche.util.TrancheTestCase;
import org.tranche.util.Utils;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class GZIPEncodingStreamTest extends TrancheTestCase {

    public void testGZIPStreamEncodingVersusTempFile() throws Exception {
        byte[] bytes = Utils.makeRandomData(100000);
        // compress
        byte[] compressed = CompressionUtil.gzipCompress(bytes, new byte[0]);
        BigHash bytesHash = new BigHash(compressed);
        // check against known output
        ByteArrayOutputStream gzipb = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(gzipb);
        gzip.write(bytes);
        gzip.flush();
        gzip.finish();
        BigHash goodHash = new BigHash(gzipb.toByteArray());
        // compress with the stream
        byte[] buf = new byte[123];
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPEncodingStream ges = new GZIPEncodingStream(baos);
        for (int bytesRead = bais.read(buf, 0, buf.length); bytesRead != -1; bytesRead = bais.read(buf, 0, buf.length)) {
            ges.write(buf, 0, bytesRead);
        }
        ges.flush();
        ges.close();
        // finish
        BigHash streamHash = ges.getHash();
        // make sure the byte based compress ends up as expected
        assertEquals(bytesHash, goodHash);
        // make sure that the stream matches the bytes
        assertEquals(bytesHash, streamHash);
    }
}
