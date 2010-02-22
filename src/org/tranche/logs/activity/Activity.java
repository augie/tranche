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
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import org.tranche.security.Signature;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;

/**
 * <p>Entry in activity log, which is used to log all requests that impact data held by Tranche server.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class Activity implements Comparable<Activity>, Serializable {

    /**
     * <p>The value used when a timestamp is not known.</p>
     * <p>A timestamp is not known until it is written. This is because all entries must be in chronological order, so timestamp is recordered when thread is writing.</p>
     */
    public static final long UNSET_TIMESTAMP = -1;
    /**
     * We reserved the four low-order bits for chunk types so can expand (up to 128 values)
     */
    public static final byte CHUNK_ANY = 0;
    public static final byte CHUNK_DATA = 1 << 0;
    public static final byte CHUNK_META_DATA = 1 << 1;
    /**
     * We reserved the four high-order bits for action types so can expand (up to 128 values)
     */
    public static final byte ACTION_ANY = 1 << 4;
    public static final byte ACTION_SET = 1 << 5;
    public static final byte ACTION_DELETE = 1 << 6;
    public static final byte ACTION_REPLACE = 3 << 4;
    /**
     * <p>Mask for all activities, regardless of chunk type (data, meta, etc?) and action.</p>
     */
    public static final byte ANY = CHUNK_ANY | ACTION_ANY;
    /**
     * <p>Action byte representing a data chunk being set.</p>
     */
    public static final byte SET_DATA = CHUNK_DATA | ACTION_SET;
    /**
     * <p>Action byte representing a meta data chunk being set.</p>
     */
    public static final byte SET_META_DATA = CHUNK_META_DATA | ACTION_SET;
    /**
     * <p>Action byte representing a data chunk being deleted.</p>
     */
    public static final byte DELETE_DATA = CHUNK_DATA | ACTION_DELETE;
    /**
     * <p>Action byte representing a meta data chunk being deleted.</p>
     */
    public static final byte DELETE_META_DATA = CHUNK_META_DATA | ACTION_DELETE;
    /**
     * <p>Action byte representing a meta data chunk being replaced.</p>
     */
    public static final byte REPLACE_META_DATA = CHUNK_META_DATA | ACTION_REPLACE;
    /**
     * <p>The timestamp that the request was received.</p>
     */
    private long timestamp;
    /**
     * <p>The action byte.</p>
     * @see #DELETE_DATA
     * @see #DELETE_META_DATA
     * @see #SET_DATA
     * @see #SET_META_DATA
     * @see #REPLACE_META_DATA
     */
    private byte action;
    /**
     * <p>Signature accompanying the logged request.</p>
     */
    private Signature signature;
    /**
     * <p>BigHash of chunk being logged.</p>
     */
    private BigHash hash;

    /**
     * <p>Used to create an Activity object that will be written to ActivityLog.</p>
     * <p>Note that there is no timestamp parameter. When an Activity is written to the activity log, the timestamp will be recorded.</p>
     * @param action
     * @param signature
     * @param hash
     * @throws java.lang.Exception
     */
    public Activity(byte action, Signature signature, BigHash hash) throws Exception {
        this(UNSET_TIMESTAMP, action, signature, hash);
    }

    /**
     * <p>Used to create an Activity object that was read from the ActivityLog.</p>
     * <p>In the other constructor, there is no timestamp parameter since the timestamp is unknown until the Activity is recorded. Here, the timestamp is known and must be supplied as a parameter.</p>
     * @param timestamp
     * @param action
     * @param signature
     * @param hash
     * @throws java.lang.Exception
     */
    public Activity(long timestamp, byte action, Signature signature, BigHash hash) throws Exception {
        this.timestamp = timestamp;
        this.action = action;
        this.signature = signature;
        this.hash = hash;
    }

    /**
     * 
     * @param in
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public Activity(InputStream in) throws IOException, GeneralSecurityException {
        deserialize(in);
    }

    /**
     *
     * @param bytes
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public Activity(byte[] bytes) throws IOException, GeneralSecurityException {
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
     * @param timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
    public Signature getSignature() {
        return signature;
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
     */
    public boolean isTimestampSet() {
        return this.timestamp != UNSET_TIMESTAMP;
    }

    /**
     *
     * @return
     */
    public boolean isMetaData() {
        return isMetaData(action);
    }

    /**
     *
     * @return
     */
    public boolean isData() {
        return isData(action);
    }

    /**
     *
     * @param actionByte
     * @return
     */
    public static boolean isMetaData(byte actionByte) {
        return (actionByte & CHUNK_META_DATA) == CHUNK_META_DATA;
    }

    /**
     *
     * @param actionByte
     * @return
     */
    public static boolean isData(byte actionByte) {
        return (actionByte & CHUNK_DATA) == CHUNK_DATA;
    }

    /**
     *
     * @return
     * @throws IOException
     * @throws CertificateEncodingException
     */
    public byte[] toByteArray() throws IOException, CertificateEncodingException {
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
     * @throws CertificateEncodingException
     */
    protected void serialize(OutputStream out) throws IOException, CertificateEncodingException {
        IOUtil.writeLong(timestamp, out);
        IOUtil.writeByte(action, out);
        IOUtil.writeBytes(hash.toByteArray(), out);
        signature.serialize(out);
    }

    /**
     *
     * @param in
     * @throws IOException
     * @throws GeneralSecurityException
     */
    protected void deserialize(InputStream in) throws IOException, GeneralSecurityException {
        timestamp = IOUtil.readLong(in);
        action = IOUtil.readByte(in);
        hash = BigHash.createFromBytes(IOUtil.readBytes(BigHash.HASH_LENGTH, in));
        signature = new Signature(in);
    }

    /**
     *
     * @return
     */
    @Override()
    public int hashCode() {
        return hash.hashCode() + action;
    }

    /**
     *
     * @param o
     * @return
     */
    @Override()
    public boolean equals(Object o) {
        if (o instanceof Activity) {
            Activity a = (Activity) o;
            return a.timestamp == this.timestamp && a.action == this.action && a.hash.equals(this.hash) && a.signature.equals(this.signature);
        }
        return false;
    }

    /**
     *
     * @param a
     * @return
     */
    public int compareTo(Activity a) {
        if (this.timestamp < a.timestamp) {
            return -1;
        } else if (this.timestamp > a.timestamp) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * 
     * @return
     */
    @Override
    public String toString() {
        return "Activity: timestamp=" + timestamp + ", action=" + action + ", hash=" + hash + ", signature=" + signature.getUserName();
    }
}
