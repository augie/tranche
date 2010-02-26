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
import org.tranche.hash.BigHash;
import org.tranche.security.SecurityUtil;
import org.tranche.util.TrancheTestCase;
import org.tranche.util.Utils;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class AESEncodingStreamTest extends TrancheTestCase {

    public void testAESStreamEncodingVersusTempFile() throws Exception {
        byte[] bytes = Utils.makeRandomData(100000);
        // encrypt
        String passphrase = "The passphrase of today is ....";
        byte[] aes = SecurityUtil.encryptInMemory(passphrase, bytes);
        BigHash bytesHash = new BigHash(aes);
        // compress with the stream
        byte[] buf = new byte[123];
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AESEncodingStream aess = new AESEncodingStream(passphrase, baos);
        for (int bytesRead = bais.read(buf, 0, buf.length); bytesRead != -1; bytesRead = bais.read(buf, 0, buf.length)) {
            aess.write(buf, 0, bytesRead);
        }
        // finish
        BigHash streamHash = aess.getHash();
        // make sure that the stream matches the bytes
        assertEquals(bytesHash, streamHash);
    }
}
