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

import java.nio.ByteBuffer;
import org.bouncycastle.crypto.digests.*;

/**
 * Class that actually creates the BigHash used for identifying files/projects. 
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public final class BigHashMaker {

    // make the hash functions
    private MD5Digest md5Digest = new MD5Digest();
    private SHA1Digest sha1Digest = new SHA1Digest();
    private SHA256Digest sha256Digest = new SHA256Digest();
    // the total length
    private long totalLength = 0;
    // previously this was an 'int' -- arg....
    // we'll have to hack in a fall-back check that
    // that uses an int rollover.
    // int totalLengthBroken = 0;

    /**
     * Update BigHash with a given array of bytes at a particular offset and 
     * length.
     * @param bytes
     * @param offset
     * @param length
     */
    public final void update(byte[] bytes, int offset, int length) {
        // write out the bytes to the hash functions
        md5Digest.update(bytes, offset, length);
        sha1Digest.update(bytes, offset, length);
        sha256Digest.update(bytes, offset, length);
        // track the total length
        totalLength += length;
    }

    /**
     * Create the BigHash return bytes of the BigHash.
     * @return
     */
    public final byte[] finish() {
        // make the buffer for the bytes
        byte[] bytes = new byte[BigHash.HASH_LENGTH];

        // get the hash values
        md5Digest.doFinal(bytes, BigHash.MD5_OFFSET);
        sha1Digest.doFinal(bytes, BigHash.SHA1_OFFSET);
        sha256Digest.doFinal(bytes, BigHash.SHA256_OFFSET);

        // set the length
        ByteBuffer bb = ByteBuffer.wrap(bytes, BigHash.LENGTH_OFFSET, BigHash.LENGTH_LENGTH);
        bb.putLong(totalLength);

        // return the big hash
        return bytes;
    }
}
