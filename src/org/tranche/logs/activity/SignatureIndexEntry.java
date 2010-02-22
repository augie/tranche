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
package org.tranche.logs.activity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import org.tranche.util.IOUtil;

/**
 * <p>Wrapper class for data that is held in the signature index file for activity log.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SignatureIndexEntry implements Serializable {

    public static final int SIZE = 4 + 4 + 8;
    private int index, length;
    private long offset;

    /**
     *
     * @param index
     * @param offset
     * @param length
     */
    public SignatureIndexEntry(int index, long offset, int length) {
        this.index = index;
        this.offset = offset;
        this.length = length;
    }

    /**
     *
     * @param in
     * @throws IOException
     */
    public SignatureIndexEntry(InputStream in) throws IOException {
        deserialize(in);
    }

    /**
     *
     * @param bytes
     * @throws IOException
     */
    public SignatureIndexEntry(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(bytes);
            deserialize(bais);
        } finally {
            IOUtil.safeClose(bais);
        }
    }

    /**
     *
     * @return
     */
    public int getIndex() {
        return index;
    }

    /**
     *
     * @return
     */
    public int getLength() {
        return length;
    }

    /**
     *
     * @return
     */
    public long getOffset() {
        return offset;
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public byte[] toByteArray() throws Exception {
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            serialize(baos);
            return baos.toByteArray();
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    /**
     *
     * @param out
     * @throws IOException
     */
    protected void serialize(OutputStream out) throws IOException {
        IOUtil.writeInt(index, out);
        IOUtil.writeLong(offset, out);
        IOUtil.writeInt(length, out);
    }

    /**
     *
     * @param in
     * @throws IOException
     */
    protected void deserialize(InputStream in) throws IOException {
        index = IOUtil.readInt(in);
        offset = IOUtil.readLong(in);
        length = IOUtil.readInt(in);
    }

    /**
     *
     * @return
     */
    @Override()
    public int hashCode() {
        return index;
    }

    /**
     *
     * @param o
     * @return
     */
    @Override()
    public boolean equals(Object o) {
        if (o instanceof SignatureIndexEntry) {
            SignatureIndexEntry e = (SignatureIndexEntry) o;
            return e.index == index && e.length == length && e.offset == offset;
        }
        return false;
    }

    /**
     * 
     * @return
     */
    @Override()
    public String toString() {
        return "SignatureIndexEntry: index=" + index + "; length=" + length + "; offset=" + offset;
    }
}
