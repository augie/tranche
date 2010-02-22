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
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;

/**
 *<p>Wrapper class for data that is held in the activity log file.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ActivityLogEntry implements Serializable {

    public static final int SIZE = 8 + 1 + 4 + BigHash.HASH_LENGTH;
    private long timestamp;
    private byte action;
    private int signatureIndex;
    private BigHash hash;

    /**
     * 
     * @param timestamp
     * @param action
     * @param signatureIndex
     * @param hash
     */
    public ActivityLogEntry(long timestamp, byte action, int signatureIndex, BigHash hash) {
        this.timestamp = timestamp;
        this.action = action;
        this.signatureIndex = signatureIndex;
        this.hash = hash;
    }

    /**
     *
     * @param in
     * @throws IOException
     */
    public ActivityLogEntry(InputStream in) throws IOException {
        deserialize(in);
    }

    /**
     *
     * @param bytes
     * @throws IOException
     */
    public ActivityLogEntry(byte[] bytes) throws IOException {
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
    public long getTimestamp() {
        return timestamp;
    }

    /**
     *
     * @return
     */
    public byte getAction() {
        return action;
    }

    /**
     *
     * @return
     */
    public int getSignatureIndex() {
        return signatureIndex;
    }

    /**
     *
     * @return
     */
    public BigHash getHash() {
        return hash;
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
        IOUtil.writeLong(timestamp, out);
        IOUtil.writeByte(action, out);
        IOUtil.writeInt(signatureIndex, out);
        IOUtil.writeBytes(hash.toByteArray(), out);
    }

    /**
     * 
     * @param in
     * @throws IOException
     */
    protected void deserialize(InputStream in) throws IOException {
        timestamp = IOUtil.readLong(in);
        action = IOUtil.readByte(in);
        signatureIndex = IOUtil.readInt(in);
        hash = BigHash.createFromBytes(IOUtil.readBytes(BigHash.HASH_LENGTH, in));
    }

    /**
     *
     * @return
     */
    @Override()
    public int hashCode() {
        // Use hash and simply increment by action. Highly unlikely any collision
        return hash.hashCode() + action;
    }

    /**
     *
     * @param o
     * @return
     */
    @Override()
    public boolean equals(Object o) {
        if (o instanceof ActivityLogEntry) {
            ActivityLogEntry e = (ActivityLogEntry) o;
            return e.hash.equals(this.hash) && e.timestamp == this.timestamp && e.action == this.action && e.signatureIndex == this.signatureIndex;
        }
        return false;
    }

    /**
     *
     * @return
     */
    @Override()
    public String toString() {
        return "ActivityLogEntry: timestamp=" + timestamp + "; action=" + action + "; signatureIndex=" + signatureIndex + "; hash=" + hash;
    }
}
