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
import java.nio.ByteBuffer;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ActivityLogUtil {

    /**
     * <p>Timestamp is a long.</p>
     */
    public static final byte TIMESTAMP_SIZE_IN_BYTES = 8;
    /**
     * <p>The activity includes the type of chunk (e.g., meta data) and the specific action (e.g., delete) and is a single byte.</p>
     */
    public static final byte ACTIVITY_SIZE_IN_BYTES = 1;
    /**
     * <p>The signature index is the primary key for finding the signature in the signature index file and is an integer.</p>
     */
    public static final byte SIGNATURE_INDEX_SIZE_IN_BYTES = 4;
    /**
     * <p>The signature offset is found in the signature index file and is the offset for the signature in the signature entries file (which is a random access file) and is a long.</p>
     */
    public static final byte SIGNATURE_OFFSET_SIZE_IN_BYTES = 8;
    /**
     * <p>The signature length is found in the signature index file and is the size of the signature (in bytes) in the signature entries file (which is a random access file) and is an integer.</p> 
     */
    public static final byte SIGNATURE_LENGTH_SIZE_IN_BYTES = 4;
    /**
     * 
     */
    public static final int TOTAL_ACTIVITY_ENTRY_SIZE_IN_BYTES = TIMESTAMP_SIZE_IN_BYTES + ACTIVITY_SIZE_IN_BYTES + SIGNATURE_INDEX_SIZE_IN_BYTES + BigHash.HASH_LENGTH;
    /**
     * 
     */
    public static final int TOTAL_SIGNATURE_INDEX_ENTRY_SIZE_IN_BYTES = SIGNATURE_INDEX_SIZE_IN_BYTES + SIGNATURE_OFFSET_SIZE_IN_BYTES + SIGNATURE_LENGTH_SIZE_IN_BYTES;

    public static byte[] toActivityLogByteArray(ActivityLogEntry activityLogEntry) throws IOException {
        ByteArrayOutputStream baos = null;
        try {

            baos = new ByteArrayOutputStream(TOTAL_ACTIVITY_ENTRY_SIZE_IN_BYTES);

            // Timestamp is first 8 bytes
            byte[] timestampBytes = new byte[TIMESTAMP_SIZE_IN_BYTES];
            ByteBuffer bb = ByteBuffer.wrap(timestampBytes);
            bb.putLong(activityLogEntry.timestamp);
            baos.write(timestampBytes);

            // Activity is next 1 byte
            baos.write(new byte[]{activityLogEntry.action});

            // Signature index is next 4 bytes
            byte[] signatureIndexBytes = new byte[SIGNATURE_INDEX_SIZE_IN_BYTES];
            bb = ByteBuffer.wrap(signatureIndexBytes);
            bb.putInt(activityLogEntry.signatureIndex);
            baos.write(signatureIndexBytes);

            // BigHash is remaining 78 bytes
            baos.write(activityLogEntry.hash.toByteArray());

            baos.flush();
            byte[] byteArray = baos.toByteArray();

            // Quick assertion
            if (byteArray.length != TOTAL_ACTIVITY_ENTRY_SIZE_IN_BYTES) {
                throw new AssertionFailedException("byteArray.length<" + byteArray.length + "> != TOTAL_ACTIVITY_ENTRY_SIZE_IN_BYTES<" + TOTAL_ACTIVITY_ENTRY_SIZE_IN_BYTES + ">");
            }
            return byteArray;
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    public static ActivityLogEntry fromActivityLogByteArray(byte[] activityLogEntryByteArray) throws IOException {

        if (activityLogEntryByteArray.length != TOTAL_ACTIVITY_ENTRY_SIZE_IN_BYTES) {
            throw new RuntimeException("Expecting " + TOTAL_ACTIVITY_ENTRY_SIZE_IN_BYTES + " bytes in input array, instead found: " + activityLogEntryByteArray.length);
        }

        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(activityLogEntryByteArray);

            // Timestamp is first 8 bytes
            byte[] timestampBytes = new byte[TIMESTAMP_SIZE_IN_BYTES];
            IOUtil.getBytesFully(timestampBytes, bais);
            ByteBuffer bb = ByteBuffer.wrap(timestampBytes);
            long timestamp = bb.getLong();

            // Activity is next 1 byte
            byte[] activityBytes = new byte[ACTIVITY_SIZE_IN_BYTES];
            IOUtil.getBytesFully(activityBytes, bais);
            byte activity = activityBytes[0];

            // Signature index is next 4 bytes
            byte[] signatureIndexBytes = new byte[SIGNATURE_INDEX_SIZE_IN_BYTES];
            IOUtil.getBytesFully(signatureIndexBytes, bais);
            bb = ByteBuffer.wrap(signatureIndexBytes);
            int signatureIndex = bb.getInt();

            // BigHash is remaining 78 bytes
            byte[] hashBytes = new byte[BigHash.HASH_LENGTH];
            IOUtil.getBytesFully(hashBytes, bais);
            bb = ByteBuffer.wrap(hashBytes);
            BigHash hash = BigHash.createFromBytes(hashBytes);

            return new ActivityLogEntry(timestamp, activity, signatureIndex, hash);

        } finally {
            IOUtil.safeClose(bais);
        }
    }

    public static byte[] toSignatureHeaderByteArray(SignatureIndexEntry signatureIndexEntry) throws IOException {
        ByteArrayOutputStream baos = null;

        try {

            baos = new ByteArrayOutputStream(TOTAL_SIGNATURE_INDEX_ENTRY_SIZE_IN_BYTES);

            // Signature index is first 4 bytes
            byte[] signatureIndexBytes = new byte[SIGNATURE_INDEX_SIZE_IN_BYTES];
            ByteBuffer bb = ByteBuffer.wrap(signatureIndexBytes);
            bb.putInt(signatureIndexEntry.signatureIndex);
            baos.write(signatureIndexBytes);

            // Signature offset is next 8 bytes
            byte[] signatureOffsetBytes = new byte[SIGNATURE_OFFSET_SIZE_IN_BYTES];
            bb = ByteBuffer.wrap(signatureOffsetBytes);
            bb.putLong(signatureIndexEntry.signatureOffset);
            baos.write(signatureOffsetBytes);

            // Signature length (int bytes) is last 4 bytes
            byte[] signatureLengthBytes = new byte[SIGNATURE_LENGTH_SIZE_IN_BYTES];
            bb = ByteBuffer.wrap(signatureLengthBytes);
            bb.putInt(signatureIndexEntry.signatureLen);
            baos.write(signatureLengthBytes);

            baos.flush();
            byte[] byteArray = baos.toByteArray();

            // Quick assertion
            if (byteArray.length != TOTAL_SIGNATURE_INDEX_ENTRY_SIZE_IN_BYTES) {
                throw new AssertionFailedException("byteArray.length<" + byteArray.length + "> != TOTAL_SIGNATURE_INDEX_ENTRY_SIZE_IN_BYTES<" + TOTAL_SIGNATURE_INDEX_ENTRY_SIZE_IN_BYTES + ">");
            }
            return byteArray;
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    public static SignatureIndexEntry fromSignatureHeaderByteArray(byte[] signatureHeaderByteArray) throws IOException {

        if (signatureHeaderByteArray.length != TOTAL_SIGNATURE_INDEX_ENTRY_SIZE_IN_BYTES) {
            throw new RuntimeException("Expecting " + TOTAL_SIGNATURE_INDEX_ENTRY_SIZE_IN_BYTES + " bytes in input array, instead found: " + signatureHeaderByteArray.length);
        }

        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(signatureHeaderByteArray);

            // Signature index is first 4 bytes
            byte[] signatureIndexBytes = new byte[SIGNATURE_INDEX_SIZE_IN_BYTES];
            IOUtil.getBytesFully(signatureIndexBytes, bais);
            ByteBuffer bb = ByteBuffer.wrap(signatureIndexBytes);
            int signatureIndex = bb.getInt();

            // Signature offset is next 8 bytes
            byte[] signatureOffsetBytes = new byte[SIGNATURE_OFFSET_SIZE_IN_BYTES];
            IOUtil.getBytesFully(signatureOffsetBytes, bais);
            bb = ByteBuffer.wrap(signatureOffsetBytes);
            long signatureOffset = bb.getLong();

            // Signature length (int bytes) is last 4 bytes
            byte[] signatureLengthBytes = new byte[SIGNATURE_LENGTH_SIZE_IN_BYTES];
            IOUtil.getBytesFully(signatureLengthBytes, bais);
            bb = ByteBuffer.wrap(signatureLengthBytes);
            int signatureLen = bb.getInt();

            return new SignatureIndexEntry(signatureIndex, signatureOffset, signatureLen);
        } finally {
            IOUtil.safeClose(bais);
        }
    }
}
