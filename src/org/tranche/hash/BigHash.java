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
import java.io.*;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * <p>A utility class for making and representing all of the hash information required for files on Tranche.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public final class BigHash implements Comparable<BigHash> {

    /**
     * MD5 hash length in bytes.
     */
    public static final int MD5_LENGTH = 128 / 8; // 16 bytes
    /**
     * SHA-1 hash length in bytes.
     */
    public static final int SHA1_LENGTH = 160 / 8; // 20 bytes
    /**
     * SHA-256 hash length in bytes.
     */
    public static final int SHA256_LENGTH = 256 / 8; // 32 bytes
    /**
     * Length of bytes represented by BigHash.
     */
    public static final int LENGTH_LENGTH = 64 / 8; // 8 bytes
    /**
     * Offset of the beginning of the MD5 portion of the BigHash.
     */
    public static final int MD5_OFFSET = 0;
    /**
     * Offset of the beginning of the SHA-1 portion of the BigHash.
     */
    public static final int SHA1_OFFSET = MD5_LENGTH;
    /**
     * Offset of the beginning of the SHA-256 portion of the BigHash.
     */
    public static final int SHA256_OFFSET = MD5_LENGTH + SHA1_LENGTH;
    /**
     * Offset of the beggingin of the Length portion of the BigHash.
     */
    public static final int LENGTH_OFFSET = MD5_LENGTH + SHA1_LENGTH + SHA256_LENGTH;
    /**
     * <p>The exact size of the hash. Made up of:</p>
     * <ul>
     *   <li>MD5 length: 16 bytes</li>
     *   <li>SHA1 length: 20 bytes</li>
     *   <li>SHA256 length: 32 bytes</li>
     *   <li>Length of file length: 8 bytes</li>
     * </ul>
     * <p>Hence this constant's value is 76 bytes, the sum of its components.</p>
     */
    public static final int HASH_LENGTH = MD5_LENGTH + SHA1_LENGTH + SHA256_LENGTH + LENGTH_LENGTH;
    /**
     * BigHash length in Base16.
     */
    public static final int HASH_STRING_LENGTH_BASE16 = 152;
    /**
     * BigHash length in Base64.
     */
    public static final int HASH_STRING_LENGTH_BASE64 = 104;
    /**
     * The bytes for this hash
     */
    private byte[] bytes;
    private int offset = 0;

    private BigHash() {
    }

    /**
     * Construct a BigHash object from an array of bytes.
     * @param bytes
     */
    public BigHash(byte[] bytes) {
        this(new ByteArrayInputStream(bytes), bytes.length);
    }

    /**
     * Construct a BigHash object from an array of bytes using a length that 
     * starts from the begnning of the array.
     * @param bytes
     * @param length
     */
    public BigHash(byte[] bytes, int length) {
        this(new ByteArrayInputStream(bytes, 0, length), length, new byte[0]);
    }

    /**
     * <p>Construct a BigHash object from an array of bytes and padding.</p>
     * @param bytes
     * @param padding
     */
    public BigHash(byte[] bytes, byte[] padding) {
        this(bytes, bytes.length, padding);
    }

    /**
     * Construct a BigHash object from an array of bytes using a length and 
     * and additional padding.
     * @param bytes
     * @param length
     * @param padding
     */
    public BigHash(byte[] bytes, int length, byte[] padding) {
        this(new ByteArrayInputStream(bytes, 0, length), length, padding);
    }

    /**
     * Construct a BigHash object from a file.
     * @param f
     * @throws java.io.FileNotFoundException
     */
    public BigHash(File f) throws FileNotFoundException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            init(fis, f.length(), new byte[0]);
        } finally {
            IOUtil.safeClose(fis);
        }
    }

    /**
     * Construct a BigHash object from a file with some additional padding.
     * @param f
     * @param padding
     * @throws java.io.FileNotFoundException
     */
    public BigHash(File f, byte[] padding) throws FileNotFoundException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            init(fis, f.length(), padding);
        } finally {
            IOUtil.safeClose(fis);
        }
    }

    /**
     * Construct a BigHash object from a stream of a specified length.
     * @param is
     * @param length
     */
    public BigHash(InputStream is, long length) {
        init(is, length, new byte[0]);
    }

    /**
     * Construct a BigHash object from a stream of specified length with some
     * additional padding.
     * @param is
     * @param length
     * @param padding
     */
    public BigHash(InputStream is, long length, byte[] padding) {
        init(is, length, padding);
    }

    /**
     * TODO: Need to provide internal documentation
     * @param is
     * @param length
     * @param padding
     * @throws java.lang.RuntimeException
     */
    private final void init(final InputStream is, final long length, byte[] padding) throws RuntimeException {
        // make the hash functions
        BigHashMaker bhm = new BigHashMaker();

        // hash
        byte[] buf = new byte[10000];
        int maxToRead = buf.length;
        // bounds check the max read count
        if (maxToRead > length) {
            maxToRead = (int) length;
        }
        long totalRead = 0;
        try {
            for (int bytesRead = is.read(buf, 0, maxToRead); totalRead < length && bytesRead != -1; bytesRead = is.read(buf, 0, maxToRead)) {
                // tally the bytes read
                totalRead += bytesRead;
                // update the maxToRead
                if (length - totalRead < maxToRead) {
                    maxToRead = (int) (length - totalRead);
                }
                // write out the bytes to the hash functions
                bhm.update(buf, 0, bytesRead);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Can't read bytes required for the hash!");
        }

        // add the padding -- this is to dynamically calc hashes that purposely avoid hash collisions or encryption
        bhm.update(padding, 0, padding.length);

        // finish and set the bytes
        bytes = bhm.finish();
    }

    /**
     * Return MD5 portion of BigHash.
     * @return
     */
    public final ByteBuffer getMD5() {
        return ByteBuffer.wrap(bytes, offset + MD5_OFFSET, MD5_LENGTH);
    }

    /**
     * Return SHA-1 portion of BigHash.
     * @return
     */
    public final ByteBuffer getSHA1() {
        return ByteBuffer.wrap(bytes, offset + SHA1_OFFSET, SHA1_LENGTH);
    }

    /**
     * Return SHA-256 portion of BigHash.
     * @return
     */
    public final ByteBuffer getSHA256() {
        return ByteBuffer.wrap(bytes, offset + SHA256_OFFSET, SHA256_LENGTH);
    }

    /**
     * Another method for getting BigHash length used for hashes prior to 
     * 5/1/2008.
     * @return
     */
    public final long getLength() {
        // unfortunately this hack is needed to fix hashes prior to 5/1/2008
        // Files of size more than 2GB had their size represented as a int
        // not a long. Thus, file size rollover.
        //
        // Note: Functionally this bug doesn't break Tranche at all. Old
        // uploads will consistently validated as long as you account for
        // the int rollover. Ask dev list for details.
        Long rolloverLengthFix = rolloverLengthFixes.get(this);
        if (rolloverLengthFix != null) {
            return rolloverLengthFix;
        }

        // manually make the long
        long l = 0;
        l |= (bytes[offset + LENGTH_OFFSET + 0] & 0xFF);
        l <<= 8;
        l |= (bytes[offset + LENGTH_OFFSET + 1] & 0xFF);
        l <<= 8;
        l |= (bytes[offset + LENGTH_OFFSET + 2] & 0xFF);
        l <<= 8;
        l |= (bytes[offset + LENGTH_OFFSET + 3] & 0xFF);
        l <<= 8;
        l |= (bytes[offset + LENGTH_OFFSET + 4] & 0xFF);
        l <<= 8;
        l |= (bytes[offset + LENGTH_OFFSET + 5] & 0xFF);
        l <<= 8;
        l |= (bytes[offset + LENGTH_OFFSET + 6] & 0xFF);
        l <<= 8;
        l |= (bytes[offset + LENGTH_OFFSET + 7] & 0xFF);

        return l;
    }

    /**
     * Return a byte array representation of the BigHash.
     * @return
     */
    public final byte[] toByteArray() {
        // if the offset is zero, return the whole array
        if (offset == 0 && bytes.length == HASH_LENGTH) {
            return bytes;
        }
        // else dup the array
        byte[] copy = new byte[HASH_LENGTH];
        System.arraycopy(bytes, offset, copy, 0, HASH_LENGTH);
        return copy;
    }

    /**
     * <p>Gets the next BigHash in the BigHash universe (this+1).</p>
     * @return
     */
    public final BigHash getNext() {
        byte[] next = new byte[bytes.length];
        for (int i = bytes.length - 1; i >= 0; i--) {
            if ((int) bytes[i] == Byte.MAX_VALUE) {
                next[i] = Byte.MIN_VALUE;
            } else {
                next[i] = (byte) ((int) bytes[i] + 1);
                for (int j = i - 1; j >= 0; j--) {
                    next[j] = bytes[j];
                }
                break;
            }
        }
        return BigHash.createFromBytes(next);
    }

    /**
     * <p>Gets the previous BigHash in the BigHash universe (this - 1).</p>
     * @return
     */
    public final BigHash getPrevious() {
        byte[] previous = new byte[bytes.length];
        for (int i = bytes.length - 1; i >= 0; i--) {
            if ((int) bytes[i] == Byte.MIN_VALUE) {
                previous[i] = Byte.MAX_VALUE;
            } else {
                previous[i] = (byte) ((int) bytes[i] - 1);
                for (int j = i - 1; j >= 0; j--) {
                    previous[j] = bytes[j];
                }
                break;
            }
        }
        return BigHash.createFromBytes(previous);
    }

    /**
     * 
     * @param hash
     */
    public final void add(BigHash hash) {
        boolean carry = false;
        for (int i = bytes.length - 1; i >= 0; i--) {
            int add = 256 + (int) hash.bytes[i] + (int) bytes[i];
            if (carry) {
                add++;
                carry = false;
            }
            if (add >= 256) {
                carry = true;
                add -= 256;
            }
            bytes[i] = (byte) (add - 128);
        }
    }

    /**
     * 
     * @return
     */
    @Override
    public BigHash clone() {
        byte[] newBytes = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            newBytes[i] = bytes[i];
        }
        return BigHash.createFromBytes(newBytes);
    }

    /**
     * Similar to standard compareTo() methods, this compares to determine the 
     * equality of two BigHash objects.
     * @param bh
     * @return
     */
    public int compareTo(final BigHash bh) {
        // use final variables for speed
        final byte[] b1 = bh.bytes;
        final int o1 = bh.offset;
        final byte[] b2 = bytes;
        final int o2 = offset;

        // compare by hash bytes
        for (int i = 0; i < HASH_LENGTH; i++) {
            // check less
            if ((int) b2[o2 + i] < (int) b1[o1 + i]) {
                return -1;
            }
            // check more
            if ((int) b2[o2 + i] > (int) b1[o1 + i]) {
                return 1;
            }
        }

        // fall back on equal
        return 0;
    }

    /**
     * Create a BigHash object from a set of bytes.
     * @param bytes
     * @return
     */
    public static final BigHash createFromBytes(byte[] bytes) {
        return createFromBytes(bytes, 0);
    }

    /**
     * Create a BigHash object from a set of bytes with a given offset where to 
     * begin calculation of the hash.
     * @param bytes 
     * @param offset 
     * @return
     */
    public static final BigHash createFromBytes(byte[] bytes, int offset) {
        if (bytes.length - offset < HASH_LENGTH) {
            throw new RuntimeException("A hash requires exactly " + HASH_LENGTH + " bytes. You provided " + bytes.length);
        }

        // make a new, already initialized hash
        BigHash hash = new BigHash();
        hash.bytes = bytes;
        hash.offset = offset;

        // return a newly made hash
        return hash;
    }

    /**
     * Return a Base64 encoded representation of the BigHash as a string.
     * @return
     */
    @Override()
    public final String toString() {
        return toBase64String();
    }
    
    /**
     * Return a Base64 encoded representation of the BigHash as a string.
     * @return
     */
    public final String toBase64String() {
        return Base64.encodeBytes(bytes, offset, HASH_LENGTH, Base64.DONT_BREAK_LINES);
    }
    
    /**
     * Return a Base16 encoded representation of the BigHash as a string.
     * @return
     */
    public final String toBase16String() {
        return Base16.encode(bytes);
    }

    /**
     * 
     * @return
     */
    public final String toWebSafeString() {
        String string = toString();
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (Exception e) {
            return string;
        }
    }

    /**
     * A customization of the equals() method to check BigHash objects. This method gets invoked alot because BigHash objects are the primary identifier of data on Tranche.
     */
    @Override()
    public final boolean equals(Object obj) {
        // assume it is a BigHash. Type checking has a 10% ish speed hit
        BigHash bh = (BigHash) obj;
        // declaring these final nets 20% speedup?
        final byte[] bb = bh.bytes;
        final int bo = bh.offset;
        final byte[] b = bytes;
        final int o = offset;
        // manually unrolling loop has a marginal improvement
        if (bb[bo + 0] != b[o + 0]) {
            return false;
        }
        if (bb[bo + 1] != b[o + 1]) {
            return false;
        }
        if (bb[bo + 2] != b[o + 2]) {
            return false;
        }
        if (bb[bo + 3] != b[o + 3]) {
            return false;
        }
        if (bb[bo + 4] != b[o + 4]) {
            return false;
        }
        if (bb[bo + 5] != b[o + 5]) {
            return false;
        }
        if (bb[bo + 6] != b[o + 6]) {
            return false;
        }
        if (bb[bo + 7] != b[o + 7]) {
            return false;
        }
        if (bb[bo + 8] != b[o + 8]) {
            return false;
        }
        if (bb[bo + 9] != b[o + 9]) {
            return false;
        }
        if (bb[bo + 10] != b[o + 10]) {
            return false;
        }
        if (bb[bo + 11] != b[o + 11]) {
            return false;
        }
        if (bb[bo + 12] != b[o + 12]) {
            return false;
        }
        if (bb[bo + 13] != b[o + 13]) {
            return false;
        }
        if (bb[bo + 14] != b[o + 14]) {
            return false;
        }
        if (bb[bo + 15] != b[o + 15]) {
            return false;
        }
        if (bb[bo + 16] != b[o + 16]) {
            return false;
        }
        if (bb[bo + 17] != b[o + 17]) {
            return false;
        }
        if (bb[bo + 18] != b[o + 18]) {
            return false;
        }
        if (bb[bo + 19] != b[o + 19]) {
            return false;
        }
        if (bb[bo + 20] != b[o + 20]) {
            return false;
        }
        if (bb[bo + 21] != b[o + 21]) {
            return false;
        }
        if (bb[bo + 22] != b[o + 22]) {
            return false;
        }
        if (bb[bo + 23] != b[o + 23]) {
            return false;
        }
        if (bb[bo + 24] != b[o + 24]) {
            return false;
        }
        if (bb[bo + 25] != b[o + 25]) {
            return false;
        }
        if (bb[bo + 26] != b[o + 26]) {
            return false;
        }
        if (bb[bo + 27] != b[o + 27]) {
            return false;
        }
        if (bb[bo + 28] != b[o + 28]) {
            return false;
        }
        if (bb[bo + 29] != b[o + 29]) {
            return false;
        }
        if (bb[bo + 30] != b[o + 30]) {
            return false;
        }
        if (bb[bo + 31] != b[o + 31]) {
            return false;
        }
        if (bb[bo + 32] != b[o + 32]) {
            return false;
        }
        if (bb[bo + 33] != b[o + 33]) {
            return false;
        }
        if (bb[bo + 34] != b[o + 34]) {
            return false;
        }
        if (bb[bo + 35] != b[o + 35]) {
            return false;
        }
        if (bb[bo + 36] != b[o + 36]) {
            return false;
        }
        if (bb[bo + 37] != b[o + 37]) {
            return false;
        }
        if (bb[bo + 38] != b[o + 38]) {
            return false;
        }
        if (bb[bo + 39] != b[o + 39]) {
            return false;
        }
        if (bb[bo + 40] != b[o + 40]) {
            return false;
        }
        if (bb[bo + 41] != b[o + 41]) {
            return false;
        }
        if (bb[bo + 42] != b[o + 42]) {
            return false;
        }
        if (bb[bo + 43] != b[o + 43]) {
            return false;
        }
        if (bb[bo + 44] != b[o + 44]) {
            return false;
        }
        if (bb[bo + 45] != b[o + 45]) {
            return false;
        }
        if (bb[bo + 46] != b[o + 46]) {
            return false;
        }
        if (bb[bo + 47] != b[o + 47]) {
            return false;
        }
        if (bb[bo + 48] != b[o + 48]) {
            return false;
        }
        if (bb[bo + 49] != b[o + 49]) {
            return false;
        }
        if (bb[bo + 50] != b[o + 50]) {
            return false;
        }
        if (bb[bo + 51] != b[o + 51]) {
            return false;
        }
        if (bb[bo + 52] != b[o + 52]) {
            return false;
        }
        if (bb[bo + 53] != b[o + 53]) {
            return false;
        }
        if (bb[bo + 54] != b[o + 54]) {
            return false;
        }
        if (bb[bo + 55] != b[o + 55]) {
            return false;
        }
        if (bb[bo + 56] != b[o + 56]) {
            return false;
        }
        if (bb[bo + 57] != b[o + 57]) {
            return false;
        }
        if (bb[bo + 58] != b[o + 58]) {
            return false;
        }
        if (bb[bo + 59] != b[o + 59]) {
            return false;
        }
        if (bb[bo + 60] != b[o + 60]) {
            return false;
        }
        if (bb[bo + 61] != b[o + 61]) {
            return false;
        }
        if (bb[bo + 62] != b[o + 62]) {
            return false;
        }
        if (bb[bo + 63] != b[o + 63]) {
            return false;
        }
        if (bb[bo + 64] != b[o + 64]) {
            return false;
        }
        if (bb[bo + 65] != b[o + 65]) {
            return false;
        }
        if (bb[bo + 66] != b[o + 66]) {
            return false;
        }
        if (bb[bo + 67] != b[o + 67]) {
            return false;
        }
        if (bb[bo + 68] != b[o + 68]) {
            return false;
        }
        if (bb[bo + 69] != b[o + 69]) {
            return false;
        }
        if (bb[bo + 70] != b[o + 70]) {
            return false;
        }
        if (bb[bo + 71] != b[o + 71]) {
            return false;
        }
        if (bb[bo + 72] != b[o + 72]) {
            return false;
        }
        if (bb[bo + 73] != b[o + 73]) {
            return false;
        }
        if (bb[bo + 74] != b[o + 74]) {
            return false;
        }
        if (bb[bo + 75] != b[o + 75]) {
            return false;
        }
        // if all are the same, return true
        return true;
    }

    /**
     * A customization of the hashCode() method to check BigHash objects. This method gets invoked alot because BigHash objects are the primary identifier of data on Tranche.
     */
    @Override()
    public int hashCode() {
        // manually make the hash code for speed
        int hashCode = 0;
        // use final variables for speedup
        final int o = offset;
        final byte[] b = bytes;
        // could unroll the loop, but it has only marginal improvements
        for (int i = 0; i < HASH_LENGTH; i++) {
            hashCode += b[i + o] << (i % 24);
        }
        return hashCode;
    }

    /**
     *Create the hash from either a Base16 or Base64 encoded string.
     */
    public static final BigHash createHashFromString(String hash) throws RuntimeException {
        // trim/clean up
        hash = hash.trim();

        // Replace the string if a redirect is set up
        if (hashRedirects.containsKey(hash)) {
            hash = hashRedirects.get(hash);
        }

        // 152 characters means that it is base16 encoded
        if (hash.length() == HASH_STRING_LENGTH_BASE16) {
            return BigHash.createFromBytes(Base16.decode(hash));
        }
        // 105 characters means that it is base64 encoded
        if (hash.length() == HASH_STRING_LENGTH_BASE64) {
            return BigHash.createFromBytes(Base64.decode(hash));
        }
        throw new RuntimeException("The hash is " + hash.length() + " characters and must be a string encoded in either Base16 (152 characters) or Base64 (104 characters). Hash: " + hash);
    }
    /**
     * <p>Whenever need to redirect a hash to another, do so here. Any hash string represents by a key in this map will be replaced with corresponding value.</p>
     */
    private static final HashMap<String, String> hashRedirects = new HashMap();


    static {
        hashRedirects.put("a71802e118162da0fd9ca55a6de8c388", "AWXqKEC1LvBfHARUYaPnVjZLTzKqzA6cVMOb8AMY2BV/Vy+FXMz9hFaYu5vYzcQrhqoGlxLkvkg4tVikTePo84Pr8E4AAAAAAAWROQ==");
        hashRedirects.put("741378e339eaf3d99b190cc13069f178", "CdYB8G4DZ5iGnd5egGe/YA/QW89VPNKMcm3bc1gLZ60xab1ehEYuDSkm5bQ+bA8dhvT7x2UY7zH6VJaJPHjLkUGaru4AAAAAAAVmTQ==");
        hashRedirects.put("Ye7HBf/vC3i3c05OHA21i08S9KPIZ1r8DKmTa+LdGAaW6k592uSBYPW2pTwIOV8aB5n+IeTwBFfYMvbJoF7MR15Z4rkAAAAAAAnz5A==", "2BXGD7lv5nQkHGdDW2zQYq6tkjI36CS8XVXt37axQgR7GQgKa6SQqe/pICtzOCE3FPqL9GBHbjhJCv7FHhi8x/crMlYAAAAAAE/7fg==");
        hashRedirects.put("QcXNm2XVMyR675rAMZyydhAaGRM6fN/11+8YYBfo646RlKsVgz0fHTC673/qrAxrLo9Fb1w3x/empWCUalhr5thWqqcAAAAAADTSLA==", "2BXGD7lv5nQkHGdDW2zQYq6tkjI36CS8XVXt37axQgR7GQgKa6SQqe/pICtzOCE3FPqL9GBHbjhJCv7FHhi8x/crMlYAAAAAAE/7fg==");
        hashRedirects.put("bfdb830238efee3ab8d2ac9faec2f0e2", "2BXGD7lv5nQkHGdDW2zQYq6tkjI36CS8XVXt37axQgR7GQgKa6SQqe/pICtzOCE3FPqL9GBHbjhJCv7FHhi8x/crMlYAAAAAAE/7fg==");
        hashRedirects.put("xYL5Ru3UbW04u37tL1r7gllGf0lMlpVu8L6Zky+eoKljmsBdLYSSwFx+GWYJgsa0TUbQk68s3ffv/BzgmQR2h1hZepkAAAAAAAAXaw==", "V9p/cM2hJGffgeZmhP2LRHtd+ZGE/7HtHBuRb7p6xEwor2nbQc4QBBPL5MA966hw6oFrYSTJtP3msZT2QSdv2RxyypMAAAAAAAAXUg==");
        hashRedirects.put("maCRskqLF7H3taISfA7z/Zct1Qu9/qj/25sirqdbS0TCT50ac95+rdBRvgZtRi09HRFE/LZbCQPyh1c+cQj1JfgErKsAAAAAAAF3Nw==", "wSYbGj7DUs0PIKSNy/z/+8U/n3hVq7ae3JfxTTqdePyfcs+aSTxcTZrjEurPK8XaVusJYae01fMXt4OFq1jOu1uOiTEAAAAAAAF3/Q==");
        hashRedirects.put("OVA5KAP19qU19aIx2rcS1KVZkt9fvBJXp35WNjYNQ6eQCictzH5EkGgUpTreh5LxkyzpJsXrJPxMWuUz1vb8Xmw4ZHAAAAAAAAjIsg==", "/UwqBgUnS22TKwBmXksyoOIusrkP5RfLoqDJNA4J37H9udVGUXqORBTjY7xL9BJieYHsZEFr+HW+qJ52suARHJqsfU4AAAAAAAjNBw==");
        hashRedirects.put("T/hhBwkvrvPQ7+CNDUxYl8rpbK9GDrciziGt62q+FY+VBVEEhD/UpH3Uy/Lc7ftjn4jXgUThhP8YywNI9eZrRSn1Sh0AAAAAAAAHkQ==", "ckJRlKWVSyB6kSYrOqk18IGhMhUPgBZ0ez364E/eVSNiUZpY1kWBaslksciXVl224o7GGKmiTFEoXPW61WOmMWusqtMAAAAAAAAIkw==");
        hashRedirects.put("Ulv/yTYTaaHin5Tv4InpsgoUY1uTJQtdoLRi9HbdtypXqztv+BiVE/wZieBkqu6d3kU20Vyejo0HYCfswgwiGyPHQPAAAAAAAAOhng==", "6GcGToTYY4ZtZgaSDEIuiJoLyHQ6m/XnY22lVYFcYDVyCmIKQBsD3Bx4w6NSAsh02fOO0ZjGDtF3L5xFYD1dkZocrBgAAAAAAAPX2A==");
        hashRedirects.put("7yTfoEXt2j0sQF/3Yv1WnlkHMIu80PT6VGA6WbqRzwLYrh13cBPN5smA+EKh0zN0j1k8RUTtavt7HGj0lJMQdKa4l9YAAAAAAOv5fQ==", "dLhx6Jb6Q4f0R5QxFCLXKsY1FBzu9Bj1irD4D2xS/dV94p2zOfEBOiFI7e9/9HvKvvZl8u6ivxE+8Yhjy1ojJEQXt6IAAAAAAOv6BQ==");
        hashRedirects.put("pimW6PSkwbsDAeIhC5rl5ycX2hqksjqI+7CNMXDNgVaWtMBx9r7hqkq6ifWSPZkszBao3Yg5NcU/5dH8qrhoo1JPicAAAAAAAJ4PaQ==", "P3d2NELmQoSua7TiJHDf96FSwYSrqcxv3t5Ul6M+y8mcnly0akuiALzjtlSgESnITvCzL9vZ1H2u3qR6mOdPBsmU4lwAAAAAAJ4P8Q==");
        hashRedirects.put("wj1xQ7zQcSXO6e06gPxPQyJGBB0xI1eATfNFsxFRwk40F+p4U8TnQXWlqSKXIbFdqe5Nfsv7DrP4slJdgSSe82nt8gYAAAAAAKoakg==", "+sm+alrjwSnZfc3njj+5eVX61EAADKMctQm+I/LlQ1NqnJ6+ndrGGjC18nZdSdmpi/gUGSqI5I9gAeAEFuHPz6/ADY4AAAAAAKobGg==");
        hashRedirects.put("6FqkyAzUBo2lm2aXR1rgO5CP9ISwAnugdm5Tmoc09wDC61kCMn1Q/Rraz0P0PuuIGjHuhFyF7vPVEJ6Nhd4IfVQFkRQAAAAAAZQHFQ==", "u9c3+UEY+XwjDuEgsiL9VnSxQbpQaTduB4OYS2J9U/njxTLQeKW5VwZXaIh+NIuIXBlivmEcNRi2k1MA9V6CZzGBBJcAAAAAAZQHnQ==");
    }
    // *** This is only a bug fix ***
    // Unfortunately we had to make a compromise to make some Tranche uploads
    // prior to 5/1/2008 work with a properly calculated Tranche hash. The
    // old uploads used an int to represent file size when they were supposed
    // to use a long. Fortunately this was
    // easy to fix for all new uploads. Unfortunately, the table below is required
    // to make a seamless fix that returns what you'd expect for file sizes
    // according to the BigHash definition.
    //
    // Again, the table below is not needed for new uploads. Only files that are
    // more than 2GB in size and that were uploaded prior to 5/1/2008
    private static final HashMap<BigHash, Long> rolloverLengthFixes = new HashMap();


    static {
        rolloverLengthFixes.put(BigHash.createHashFromString("d21awe+G3mopUFp/Slm6aivjpYIM7cipDSHpeDWEUHSrdHVTYDfEs8Ma540qiTvVnCPPIVk8Syc56e7UtOJR9sm8sjoAAAAAER43Ww=="), Long.parseLong("4582160219"));
        rolloverLengthFixes.put(BigHash.createHashFromString("kYKm2Ovmd8+620KmMdOpynCp3QGwGee9CsvXPPSA8upXTfLE8mnQyBIzzebLP3luZLK9gCfN0y2JFkzUG7PJ3qNx5CYAAAAABNioMQ=="), Long.parseLong("4376274993"));
        rolloverLengthFixes.put(BigHash.createHashFromString("3OEc6/7J9B3ANjUIiHgoE/AJjo1ScKKVBZbpsmtvy3an2gCU4W8SDNinaI40HviLc1lTpAwIXncOc0ooqRO4ZnV1mCcAAAAAHMQ+1Q=="), Long.parseLong("4777590485"));
        rolloverLengthFixes.put(BigHash.createHashFromString("CLSnoZvRleCx0qdaG/xtstla8q65jaXiP/IgHyz9x2DtQkO59xZtqZCn84e7SxuWlG/g5m0/aWVzLv/Aq7iE2/CV3BYAAAAAIWn1wQ=="), Long.parseLong("4855559617"));
        rolloverLengthFixes.put(BigHash.createHashFromString("ENa46FMh238oR3A49XFmQheXXgvHdouA/bv+bRYFJOSpRpMBgFmF8bAC5ubUEsudVtAgPkhqyHV+N21Q0JPKEBufN5QAAAAAHUTnqg=="), Long.parseLong("4786022314"));
        rolloverLengthFixes.put(BigHash.createHashFromString("OHNdtYzsjvzcyQK8miT4jyDr7M8ktKb6/dWK6n14u2meQVFj/LGMBH+ZUbaPc3cWq6A7PWTesqlDdBxxSMBU3FHymv8AAAAAG3siyA=="), Long.parseLong("4756021960"));
        rolloverLengthFixes.put(BigHash.createHashFromString("kfl8zB0obE3jifufvQI+P7Wfbrbmyid7u0VAlVIa0fQbApCbPNcE5cFDZ226/Z/UDffneBc0PCiX0jYRRPzu3W/Ss8wAAAAAC+6bug=="), Long.parseLong("4495154106"));
        rolloverLengthFixes.put(BigHash.createHashFromString("XO5WLBoV9sPjdkwU2r2JoTXaBUD9DLieCEjcwffUE95R/UWQ26iCUVGzKO8hfSR/tA6cpU0OVjyQYjdU7Kya4ZS+yXAAAAAAK0UhAw=="), Long.parseLong("5020918019"));
        rolloverLengthFixes.put(BigHash.createHashFromString("CdGl46I67NTFgRGXCksJn/qWEMpHr0MvrUa+reZBCjXyVZxzSuEJbvbZ+u738SzQEOIvkpIfnbxEogn3JhuC5KA2K44AAAAAIyy+Uw=="), Long.parseLong("4885102163"));
        rolloverLengthFixes.put(BigHash.createHashFromString("7Wbu0rKPtaiYoMhapa8miWkGd9PhhHN5FJyQsk3txTiU54+jxN6oz0TqsbRFYbQz0Jp2zZ4FwwIvg6v/W+Xy2Fg3z9oAAAAAINLTWg=="), Long.parseLong("4845654874"));
        rolloverLengthFixes.put(BigHash.createHashFromString("kRbDLXMfBlYbY3BLgO/gu5/2FtcbsuHQL7PU/Uyp8wVblktJdN/Wl4DadcLPFcdi8gTC0MOB4uG2Swa7aX8AP8CDKoUAAAAAKyIuGQ=="), Long.parseLong("5018627609"));
        rolloverLengthFixes.put(BigHash.createHashFromString("QLWa70JSeJU9NnM0Z8YfEGZwIg5x/2LpwNXbTUawANi09cBZbxFy0Pk3Ah+bBSakTFQA0QqsV3ObHmOccvKi1c1yNi8AAAAABIoTHA=="), Long.parseLong("4371125020"));
        rolloverLengthFixes.put(BigHash.createHashFromString("vrasqI20XlDZEQ5+T6LUq3672/JLIYpSNfrMUsvWfdYtZJfDyLWd77Pzte87NlwD0bIkW4uclWfk+kjZXoYT8K1ZursAAAAALO6mdg=="), Long.parseLong("5048804982"));
        rolloverLengthFixes.put(BigHash.createHashFromString("INIU+4iRFKjnxhnK4LjN4jOsP25n54t5nn6clHmL4eHZzgIee4L7L8jZJnFHi3wRI7ermFC2ISKvD3y0j9zvziXVkRIAAAAAKY0Z9Q=="), Long.parseLong("4992080373"));
        rolloverLengthFixes.put(BigHash.createHashFromString("EPhsZfJDAFj5Jq5e2csrdjO7c2gcvELPRPc4sOgk7uiWe/M9P7Bgvq57QVN7UGGpgziN8VevPSi276XuSF8yVCNLLQMAAAAACN1Efw=="), Long.parseLong("4443686015"));
        rolloverLengthFixes.put(BigHash.createHashFromString("0h8eQL6i5b81tno3a4p/mfeA5gzKhD6OENzYg+WmijdryjUsahEar3ThaEvGHkjUQ5XYyve5pxbo/9eMNsQbkq4Z7ncAAAAANWDegg=="), Long.parseLong("5190508162"));
        rolloverLengthFixes.put(BigHash.createHashFromString("IRBrXjvvM5lx5K6D4GcYW4oi6UmWY5kMrh4PTrvO2FrxpZseMKQIZ9JLuMQatY/s+Y1MtS+2TsRAd8W2AZInTfqJTxoAAAAAE9l/1Q=="), Long.parseLong("4627988437"));
        rolloverLengthFixes.put(BigHash.createHashFromString("sBt8mdoj7FJ4q9PxyHx/JQo1Png2miWgAW8lWRpXim44ITB6YWN0AFjsxEri+FiuFXN9qfKzuYH7LGdIGDhK1gnJ20sAAAAAR1qvVw=="), Long.parseLong("5492092759"));
        rolloverLengthFixes.put(BigHash.createHashFromString("ehXot5drzY0r5Ls7T29T2LBZngFJF2jbV39WW+PBrWFI4YUE1F0NOdj9CUOI3XfUnNDk5+BYLS2abgWPN8YIiV0vjpwAAAAAOO3CBw=="), Long.parseLong("5250073095"));
        rolloverLengthFixes.put(BigHash.createHashFromString("TXLYKHlJ/Ei2zs6BXeIodC/VlFYzK/IknP6Yg4nKwSv32t9zx/Eh8VCRfNx8rd97U4Ov20+e1LIU5vQ8Ej2YFqK6fpgAAAAARsj5gg=="), Long.parseLong("5482543490"));
        rolloverLengthFixes.put(BigHash.createHashFromString("VWqH4chDjyKmf3F7jVA5ECBiB2K1LKr3CxLU2Sd7UQRV3RvnSWqbNWyc5HGG3TFBjAZJ8z4ktR4Pp084jdmYVP29+D0AAAAARt3v3A=="), Long.parseLong("5483917276"));
        rolloverLengthFixes.put(BigHash.createHashFromString("JjhTXf4/pPAyGtDOGsvXKMBMgj8F3QFmltdeUksoG2mkqbxHKNhMDrdHXNdJILMBdhfbyYspOECWDJzJxLso9KYIjpYAAAAAEiuruA=="), Long.parseLong("4599819192"));
        rolloverLengthFixes.put(BigHash.createHashFromString("1zzB9MTuIkNYJgY5EsaTt1n8ythRoa2BBTQSinvY0vT9eccLEiHhvj9kTEOjWMmJZFL7gXxXSIy8Lv6FWV8bI6xPknEAAAAASIyejQ=="), Long.parseLong("5512142477"));
        rolloverLengthFixes.put(BigHash.createHashFromString("hygr5FaP2mwBc/uYbowQDnB7nB/ZPLIP3fg/ZYuslV4b0VrPf5CyW2dJW7kbdLh839XQ0iJFfyRBgrzalWt4e8w2Pc0AAAAASRpPNA=="), Long.parseLong("5521428276"));
        rolloverLengthFixes.put(BigHash.createHashFromString("W5/C1n0exNwhGXTld/bPuCja2oS3h+Uqq4ZgxX3+4671vpavrGSYG22Gsky8YOV1e6kDb/ezyjh/4ILPjD4WSLAvlI0AAAAAFiVH0g=="), Long.parseLong("4666509266"));
        rolloverLengthFixes.put(BigHash.createHashFromString("ZOWHxKYq19PeNKJao4A1S3NF/ul0uFdr38H5rcNDtxdhD4YGWGtr8OTYLu+VFrdEUOAzVD2dbWbGd++Tcxa8EvtlP3oAAAAAGlghig=="), Long.parseLong("4736950666"));
        rolloverLengthFixes.put(BigHash.createHashFromString("rOpDSei4zb0l+t0pMfm/9mnUrNOLRyUbZ7klB+5xRbI4nyZZ7TSeguiwr7oCnHoAZLkoTCVIiKoAe7yB2xLHz6oYCcsAAAAAObUz0g=="), Long.parseLong("5263143890"));
        rolloverLengthFixes.put(BigHash.createHashFromString("QBB7AJ6Va2oQki05/+Oa5RmvrgdUPxxiTHgrA1kBQVa9DP1jYWirFQCeaIqDgKy+LdVb14/2JAXsYvlzXYmGoC2Q234AAAAARV5WYA=="), Long.parseLong("5458777696"));
        rolloverLengthFixes.put(BigHash.createHashFromString("zyzlgIpW/EbBwHIyx/C/Se69LZN9wNDHrfHiDqaSXj/fT4TLElJEWFNnnDT6TUhL4/D4zzut3l6Pyj7PdjyYd9zwAesAAAAAIfcMLg=="), Long.parseLong("4864805934"));
        rolloverLengthFixes.put(BigHash.createHashFromString("dxS1fF5LllHsZtFv5lgwRoLpNYf9vrNBCSDTEQ5mFuLf0ABPL5ZbZEz8l0cNiZZObh2sGe28SlaoyI7msQmoHJYSFe8AAAAAB83OXA=="), Long.parseLong("4425895516"));
        rolloverLengthFixes.put(BigHash.createHashFromString("4lNGjj0dgz7c3oRSjwvNXi0D/M3WjmOwWpp5syjlkPwJ0Qn8nqIClogAwtTKrN+R39G1o0qyFYWo7w7hpXwrNcfhQw0AAAAAKRNLmg=="), Long.parseLong("4984097690"));
        rolloverLengthFixes.put(BigHash.createHashFromString("dP37i9P24IdY8E63Vrvsh5fjF6ygC1BIxyGHChaicFXP62xDY9ERZQpnETJLhNjP+6ffD+MU1mwF0XLdPxWzDswrnowAAAAAPIno/g=="), Long.parseLong("5310638334"));
        rolloverLengthFixes.put(BigHash.createHashFromString("ArAUoFhpc6LffVqBXSdZRfV4rRHNAdPHNtwASBP0iEV0SL8x8DKV3ICENx9+dird/+1nvbT73ox89zRUtq6U1knvLXEAAAAAGNCuYg=="), Long.parseLong("4711296610"));
        rolloverLengthFixes.put(BigHash.createHashFromString("bV2P+yS3UkOcsiQoZ/LB3pQslvQfht60KyjK92/Sf5OCh8Yu4topxJWHyYrnPjRSSqFZ2yPA0WVbKb0K1sUtnbOtLI8AAAAAKH2CWg=="), Long.parseLong("4974281306"));
        rolloverLengthFixes.put(BigHash.createHashFromString("B0gRggTkRRwbKxc4LljwYwMuXnSsM/ax3NhMm840vW/TSMB2UFMkB9PMDq6KOgLuHbxo14ChJX4uNdspLL1qU2E4mqgAAAAARQi7Qw=="), Long.parseLong("5453167427"));
        rolloverLengthFixes.put(BigHash.createHashFromString("P7zfXR79p+LicknxAcwNLqc1JU9gfKN1D5nWCYw8Gyf2USyT1wayV8bWpnSbLZwuu3K6FaTjiMDvsNTYueVu4Kb7s/cAAAAAJVo6ow=="), Long.parseLong("4921637539"));
        rolloverLengthFixes.put(BigHash.createHashFromString("C2XjpDJYt+7aHG16cjYgqTNYBJ1WadVZUp1VWusF+J2W8Uh8lcw7UV4EURmF0w8bJCr6jcQABYBWqwt4ex0Zg7TlLwoAAAAAHZojOQ=="), Long.parseLong("4791608121"));
        rolloverLengthFixes.put(BigHash.createHashFromString("+xXKq2sZMGq8QdvUGEiAM/uw+mtgk3qHnmvjwOU87hKRdHZaQDZ7wc7CLvp5Wp3Q8uvea+k/bN+5Gb9Gw+6SyEwZim0AAAAAPieRUQ=="), Long.parseLong("5337747793"));
        rolloverLengthFixes.put(BigHash.createHashFromString("J8m8v5rwkiBbMGt1UOoJ5CsMIOj4emUdTOvWCO9fkaD82bta8Ejt4fsbQRgBAPoUxqx5eh+xtcXUc34Nmd/3lsdzT1UAAAAAGG7XMA=="), Long.parseLong("4704884528"));
        rolloverLengthFixes.put(BigHash.createHashFromString("QoJR47qE6kieC6YQVTGndKLluVrPbyzD1VfKfaNVl+W+6wHckMU7yOjhZDyVOuA/8fvNa2iJfKzQkOqSp7eYcKaT0A0AAAAAHXq8PQ=="), Long.parseLong("4789550141"));
        rolloverLengthFixes.put(BigHash.createHashFromString("wrhIoEo2x69TzMD1tzx1P3ShWfQbO4JGZ1jZGo46yz/LvnSpn4TyUhvizGx6xdP+/GL8xXIoKZd3s1qQZguVGQHAtRoAAAAAMuqA5Q=="), Long.parseLong("5149196517"));
        rolloverLengthFixes.put(BigHash.createHashFromString("KA3CRvHTMOqUhA47osZ2FqnUtEwGLMGS6T6fZA8xwIxoaMGbkGf00Q4i+EN3A+MehUvBL/icc6ypRLq6OOqutDNWC6AAAAAAHBUYBA=="), Long.parseLong("4766111748"));
        rolloverLengthFixes.put(BigHash.createHashFromString("CVHTvpisNEllVan6cGjsEru7GYwXbyXg5U9IJcE5hbw4vgCE8BNENVTAvjLPKX3AX5nbVzorzXpZfiuYMFzYDs3LzU4AAAAAHsCUuQ=="), Long.parseLong("4810904761"));
        rolloverLengthFixes.put(BigHash.createHashFromString("erRovc76RSI83yloiv1Iwc63MPswbJLO7YP+KSU54D3wCj66deHCG+OJLXPR7BdydtJBk+DRTXnD6p/2rWimN7kJGuMAAAAAFJY0Uw=="), Long.parseLong("4640355411"));
        rolloverLengthFixes.put(BigHash.createHashFromString("GIWemFg46eVwYe0ax0BidK04JtJjS+PFkKkawZSIHy7P+EYgxvMX7eHNtNhuVUdXv7hFtITPVqfBcL/TxssMjip3598AAAAAK+lQmA=="), Long.parseLong("5031678104"));
        rolloverLengthFixes.put(BigHash.createHashFromString("N/b2IYl5CO6Y8vZxeBRXHQZH5iq6oUd3U2j2ntN83Z68+HuveFNe7FqayfcQQ72QAeycx3gwusq7vp+WzNoHN9ZFGIIAAAAAH/vAdg=="), Long.parseLong("4831559798"));
        rolloverLengthFixes.put(BigHash.createHashFromString("KdpNz4VI9ACJjBNr5Vi80Oq0mucgZWaH9tvjL6UDmuC4U/iqvdDfc5U6LEKlO4taNS6t30hMp5e9aUnMcc2hp9rDc1gAAAAAGuB5Qg=="), Long.parseLong("4745886018"));
        rolloverLengthFixes.put(BigHash.createHashFromString("DA5rvuwqFLv8+vnArX1gO2NJKMDrFXm6g+/3703TpUyQZ46aBObRCvFwcqGPELydZlhm15djgpGEs+oJJ8Cf4bnxLDYAAAAADHZVag=="), Long.parseLong("4504049002"));
        rolloverLengthFixes.put(BigHash.createHashFromString("Stt5hO8pUZ7dO0l/5ZTTQajD0XvTvCjBmS5dunjgM07SWD8CepIF/yDDZ2uxyIWUQJI9gpaoviysBehRQOqhPaeBQV8AAAAAMZW6hQ=="), Long.parseLong("5126863493"));
        rolloverLengthFixes.put(BigHash.createHashFromString("Ez4Uzh3pznwf3sl9LGcme8rpSaO5sL4SXslYHWv6G54VEDhOibIhT+aZGfcsVCuuNPMDTJrT2O9/b13JEoEIz6090SUAAAAAJ+hTOg=="), Long.parseLong("4964504378"));
        rolloverLengthFixes.put(BigHash.createHashFromString("FtDFmTHSJYkpvWK6X3sgKrQxU0Trck/Fg7q985k2NwGovlnaWFb3V8bVPJUYlSHtdAiPKSCkwxyj25qElOoIh8O/2xQAAAAADDK6pg=="), Long.parseLong("4499618470"));
        rolloverLengthFixes.put(BigHash.createHashFromString("zN0xpRLde8wGuYJ0/HRVZNs4xYzSXpn6R6t0y6QXtGbnkkNgDnVMPE1wi9aYqVENMXj49YxdeaciQ/hGQRNVY0hV7EgAAAAALpkctQ=="), Long.parseLong("5076753589"));
        rolloverLengthFixes.put(BigHash.createHashFromString("cH+oQBwJPWPzyQ+cI/VxFe3widCqa9x8ELariKSOQAvdrYrg+/Y68Ryl4JCblpOMZ0ALMx71lpsyF/1tBQYaOz1kAOAAAAAAII8FJQ=="), Long.parseLong("4841211173"));
        rolloverLengthFixes.put(BigHash.createHashFromString("2kEcEq9k8G/YKZqvkddhhc/HSW7wXmHCCLRHNFSvtK530qbK+L9IWDL2rCSKjl8rKNSiPu4e6vX7BH2mN/TZOEsfgigAAAAAO/uByw=="), Long.parseLong("5301305803"));
        rolloverLengthFixes.put(BigHash.createHashFromString("UKmJkNQzQZPdcOgk2jJJ5X2bfA3fqQYpPgGjCgm8h2GgSrVPAgJca8fg5KJ4dhFIs/9qRFZ09fQYj5yUpE0SBuRwR+oAAAAAHk0PzA=="), Long.parseLong("4803334092"));
        rolloverLengthFixes.put(BigHash.createHashFromString("LYcMV5lJ0+7e3491W0IMf9dFHQB9u4c7YjnaZ/QMBQcnwu0qxDvPIsdheWNzRDiiCY3nuJySAnuWqjr3np2xttgshpwAAAAAO0Is8g=="), Long.parseLong("5289159922"));
        rolloverLengthFixes.put(BigHash.createHashFromString("/f3KQd7RDxmAlqFqk8q1Ccw+j/lDV9mxxg1+RLLAKb0EOtHlk7FqY8bIgapP8M9kPGvNePdwW2VPrx9CglJHb8ACanoAAAAAI2eTFQ=="), Long.parseLong("4888957717"));
        rolloverLengthFixes.put(BigHash.createHashFromString("Zn31ClU7h0PS7LGAwidXWGDN4CFgL0Z9EkyG9oY3yXUCvSztQGeZD+OEJ+uB/bbdamD5TnaXXRpWbLs4SwdZqR+sjc0AAAAAG5lBVQ=="), Long.parseLong("4757995861"));
        rolloverLengthFixes.put(BigHash.createHashFromString("uNucD7dfBheDpcs7VPEvnLsS6rB3OUFSKopsA0NTgODz1fV1xkC48xvcEkfqFgK1RuybB40QRsN9TwmT7QIlRALQTSsAAAAAIp/Vsw=="), Long.parseLong("4875867571"));
        rolloverLengthFixes.put(BigHash.createHashFromString("CO5BVqN/VdmVqnIzD4elF11FhMFyAY79tp4ZCHYweyXhZjKOT5l07PoHDbkvuVozs2V4imK+urPgY96btWCe2ftkGlYAAAAAGl1WEg=="), Long.parseLong("4737291794"));
        rolloverLengthFixes.put(BigHash.createHashFromString("nmdLjJHIrj+KKcWRZQMf5CFqgN64XmuE3UFrCOzG1EW35mBttb0/ZmCwRIGQKktPdhb/U1xkiC9iqOZ/4Hy4Ki2H3BQAAAAAIlnL6w=="), Long.parseLong("4871277547"));
        rolloverLengthFixes.put(BigHash.createHashFromString("IOxmsuPfhJ6lP99vBIRJp9ZAChj/PN+3hbLsBHsj0JRjgcJhFFOn2DSoheTuB16fGFgJSyAzdma6qEfkwcbZLowxJUoAAAAAF9LiNA=="), Long.parseLong("4694663732"));
        rolloverLengthFixes.put(BigHash.createHashFromString("kICp1esVoNbyw0jGusgGrSAjAHZJW173bD6AlejSYyFCrvk9+3hG0xw3diNM01qj3tgn1Q20fys99Fsxg4iGh2WuFPUAAAAAFZGHIA=="), Long.parseLong("4656826144"));
        rolloverLengthFixes.put(BigHash.createHashFromString("oAv/YDeKuDqc2vyf8kyj7UXnh59YOkt7zdYTlIQj1l5xloY97jcX45sKuXNOBIzx7j5JnOZVffGgKAbFO4CG9NVzsEoAAAAAId98AQ=="), Long.parseLong("4863261697"));
        rolloverLengthFixes.put(BigHash.createHashFromString("BLyD1YNFl2xYGq5ytF7WwqpghQEebw/EgNChoAHZD8YppfFjv0baXFpGPBncilIZ+1bjYloTYowZL7kFFccRp/7WjLMAAAAAKj2Fvw=="), Long.parseLong("5003642303"));
        rolloverLengthFixes.put(BigHash.createHashFromString("e705i0Hmd2qrGnCtOTCWycuNYq14idHcJ35rcBTvAxOTcZTIBorasTxbsydvqrFvL1COk4YRfMsP3Nmmh1+L9qNGpycAAAAAFb28Lw=="), Long.parseLong("4659723311"));
        rolloverLengthFixes.put(BigHash.createHashFromString("4c8ATZW2thHb/qyPwjIw/23QfSSO844WUg9nbjjVrcKT/k28bx3TzkGkVwUWRoeJuKST+3HWBL3tbQH/7m2JC6MMZmkAAAAAI1aZ9w=="), Long.parseLong("4887845367"));
        rolloverLengthFixes.put(BigHash.createHashFromString("r+e+Q/y+BTlZN5xyMY/rDPaN2tuAyIKQqnTqkFFVMHBwe4as6aUnfytDwlpoxzVPYtIe5f+tosEUHRhCJaVy4e1E0X0AAAAAB3pG2Q=="), Long.parseLong("4420421337"));
        rolloverLengthFixes.put(BigHash.createHashFromString("uTAW52bRzwbmmcwrWSKlIzSTXd5qmrrBnmkyaVJ0AR5eyUJpd5sAPuIJyeBDv8ukl704WRL1A0e/bHPiem1mRvV6bWQAAAAAIrwjXg=="), Long.parseLong("4877722462"));
        rolloverLengthFixes.put(BigHash.createHashFromString("tcymw77oOaOhGfiAnBCE0DfQyQeTtH0F3EZRhB1ia3bFWmXk9WaivBVqH4TXgHcz1rFD4JUisE42GKXgl+xXH1679UsAAAAANuR7wA=="), Long.parseLong("5215910848"));
        rolloverLengthFixes.put(BigHash.createHashFromString("imt07nfullYUvcflZ1SEnnD5fVCYxuWfqxbyiXkDQbf/IRGTKI+Djnf1Wo0Py4Lh3nyN07VLq+8RzyUQBYgAU0fcGy4AAAAANC5TJA=="), Long.parseLong("5170418468"));
        rolloverLengthFixes.put(BigHash.createHashFromString("gPabAEgZMB+wM/V41Y4DZhvzncOyHGy3neIVyFdIrIXYzftdkYrSJNHHspm980OazujrsNMtv80+7x9LK77DXAzhp1wAAAAAJN8cig=="), Long.parseLong("4913568906"));
        rolloverLengthFixes.put(BigHash.createHashFromString("2hcV4kmndTllYBdBfZ4R5fIFLQAK/nao288RjW6VXpmi86eKIYJl4cBaJzWK325WMHqAdb4KYsEwSS/BWLDoKXbs5JwAAAAAOtNVKQ=="), Long.parseLong("5281895721"));
        rolloverLengthFixes.put(BigHash.createHashFromString("hHjQBWRbbPY15Aw2vWf1KwxrT7NWtMfXt4DuWdNtRwu/HBrKjhxQ0Ln3Kj9jRTGISf9IvJWeOsUX+ES+tcNpzh4tnj8AAAAAHjJ8Ww=="), Long.parseLong("4801592411"));
        rolloverLengthFixes.put(BigHash.createHashFromString("RQo6wEEcqdPhVm8XAp857Z6WN2ej5s/Ca4KKs1xEfq4TspZHRYKf6F3XfntzxKv9fChNBUFJmbvem5Teycpw6Jxts10AAAAAPVn7kw=="), Long.parseLong("5324274579"));
        rolloverLengthFixes.put(BigHash.createHashFromString("15hW/X8oT67zigGVR+g8VtmynQ0kngDFsr1vCDH9KybUt9v/j1+xBWaavM23ngwagSF+ufeuDBq1/ETTpSVhDcVe6uoAAAAALLvA2g=="), Long.parseLong("5045469402"));
        rolloverLengthFixes.put(BigHash.createHashFromString("gm2sRHDMnoaOIEhY+S5gGnz5IPxLi74+O1qCLzNt/bNvWZCBf61qsqfypwh2MTUi6FOSQqaRJ7eAHn+CBpnnyhnjvgQAAAAAGZnehw=="), Long.parseLong("4724481671"));
        rolloverLengthFixes.put(BigHash.createHashFromString("LeM4E3gOARtRW4clr2EV8zySLX2fajWVoHU3U4/XpUOy+xelmOIQvxaMLVyTdSHna5wnN0WE4fvL4/b/z1VuaJyUhJcAAAAAAZ0s8A=="), Long.parseLong("4322045168"));
    }
}
