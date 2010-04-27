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
package org.tranche.server.logs;

import java.io.*;
import org.tranche.security.Signature;
import org.tranche.hash.Base64;
import org.tranche.hash.BigHash;

/**
 * Handles the complicated work of translating to and from bytes for 
 * transmission using a checksum.
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class LogEntry {

    private byte[] timestamp;
    private byte action;
    /**
     * Bytes for IP address. 4 bytes for IPv4, 16 bytes for IPv6.
     */
    private byte[] IP = null;
    private byte[] hash;
    /**
     * This is the byte array representation of the signature
     */
    private byte[] signatureMD5;
    /**
     * Length of all the data (as a byte array) in bytes.
     */
    private long length = 0;

    /**
     * Internal factory method.
     */
    private static LogEntry createEntry(InternalActionFlag iflag, long timestamp, String ip, BigHash hash, byte[] sigMD5Bytes) throws Exception {
        LogEntry entry = new LogEntry();

        // Timestamp bytes
        entry.timestamp = LogUtil.convertLongToBytes(timestamp);
        entry.length += entry.timestamp.length;

        // Hash bytes (conditional)
        if (hash != null) {
            entry.hash = hash.toByteArray();
            entry.length += entry.hash.length;
        } else {
            entry.hash = null;
        }

        // Signature (conditional)
        if (sigMD5Bytes != null) {
            entry.signatureMD5 = sigMD5Bytes;
            entry.length += entry.signatureMD5.length;
        } else {
            entry.signatureMD5 = null;
        }

        // IP version byte (See ActionByte)
        byte ipVersion = LogUtil.getIPVersion(ip);

        // The IP bytes
        switch (ipVersion) {
            case ActionByte.IPv4:
                entry.IP = LogUtil.getIPv4Bytes(ip);
                entry.length += entry.IP.length;
                break;
            case ActionByte.IPv6:
                entry.IP = LogUtil.getIPv6Bytes(ip);
                entry.length += entry.IP.length;
                break;
            default:
                throw new UnsupportedOperationException("Don't recognize internal IP version representation: " + ipVersion);
        }

        // The action flag (See ActionByte)
        entry.action = iflag.getActionForIPVersion(ipVersion);
        entry.length += 1;

        return entry;
    }

    /**
     * Create a log entry.
     * @param data An array of bytes without the first long value, number of bytes, attached.
     */
    public static LogEntry createFromBytes(byte[] data) throws Exception {

        // Only some actions require a hash or sig
        byte[] hashBytes = new byte[BigHash.HASH_LENGTH];
        byte[] sigMD5Bytes = null;

        // Wrap the bytes in a input stream
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        // Keep track of how many bytes remain
        long remaining = data.length;

        try {

            // Read timestamp
            long timestamp = dis.readLong();
            remaining -= (Long.SIZE / 8);

            // Read the action byte
            byte action = dis.readByte();
            remaining -= 1;

            // Based on byte, need to determine:
            // 1. IP version to read IP
            // 2. Which parameters will be in the stream to read
            byte ipVersion = Byte.MAX_VALUE;

            if (action < 0) {
                ipVersion = ActionByte.IPv4;
            } else {
                ipVersion = ActionByte.IPv6;
            }

            // Read in the IP bytes
            byte[] ipBytes = null;
            if (ipVersion == ActionByte.IPv4) {
                ipBytes = new byte[4];
                remaining -= 4;
            } else {
                ipBytes = new byte[16];
                remaining -= 16;
            }
            dis.readFully(ipBytes);

            // Build the request
            switch (action) {

                case ActionByte.IPv4_SetData:
                case ActionByte.IPv6_SetData:

                    // Read hash
                    dis.readFully(hashBytes);
                    remaining -= hashBytes.length;

                    // Read sig
                    sigMD5Bytes = new byte[(int) remaining];
                    dis.readFully(sigMD5Bytes);

                    // Build entry
                    return LogEntry.createEntry(
                            new InternalActionFlag(InternalActionFlag.SET_DATA),
                            timestamp,
                            LogUtil.getIPString(ipBytes),
                            BigHash.createFromBytes(hashBytes),
                            sigMD5Bytes);

                // - - - - - - - - - - - - -

                case ActionByte.IPv4_SetMeta:
                case ActionByte.IPv6_SetMeta:

                    // Read hash
                    dis.readFully(hashBytes);
                    remaining -= hashBytes.length;

                    // Read sig
                    sigMD5Bytes = new byte[(int) remaining];
                    dis.readFully(sigMD5Bytes);

                    // Build entry
                    return LogEntry.createEntry(
                            new InternalActionFlag(InternalActionFlag.SET_META),
                            timestamp,
                            LogUtil.getIPString(ipBytes),
                            BigHash.createFromBytes(hashBytes),
                            sigMD5Bytes);

                // - - - - - - - - - - - - -

                case ActionByte.IPv4_SetConfig:
                case ActionByte.IPv6_SetConfig:

                    // Read sig
                    sigMD5Bytes = new byte[(int) remaining];
                    dis.readFully(sigMD5Bytes);

                    // Build entry
                    return LogEntry.createEntry(
                            new InternalActionFlag(InternalActionFlag.SET_CONF),
                            timestamp,
                            LogUtil.getIPString(ipBytes),
                            null,
                            sigMD5Bytes);

                // - - - - - - - - - - - - -

                case ActionByte.IPv4_GetData:
                case ActionByte.IPv6_GetData:

                    // Read hash
                    dis.readFully(hashBytes);
                    remaining -= hashBytes.length;

                    // Build entry
                    return LogEntry.createEntry(
                            new InternalActionFlag(InternalActionFlag.GET_DATA),
                            timestamp,
                            LogUtil.getIPString(ipBytes),
                            BigHash.createFromBytes(hashBytes),
                            null);

                // - - - - - - - - - - - - -

                case ActionByte.IPv4_GetMeta:
                case ActionByte.IPv6_GetMeta:

                    // Read hash
                    dis.readFully(hashBytes);
                    remaining -= hashBytes.length;

                    // Build entry
                    return LogEntry.createEntry(
                            new InternalActionFlag(InternalActionFlag.GET_META),
                            timestamp,
                            LogUtil.getIPString(ipBytes),
                            BigHash.createFromBytes(hashBytes),
                            null);

                // - - - - - - - - - - - - -

                case ActionByte.IPv4_GetConfig:
                case ActionByte.IPv6_GetConfig:

                    // Build entry
                    return LogEntry.createEntry(
                            new InternalActionFlag(InternalActionFlag.GET_CONF),
                            timestamp,
                            LogUtil.getIPString(ipBytes),
                            null,
                            null);

                // - - - - - - - - - - - - -

                case ActionByte.IPv4_GetNonce:
                case ActionByte.IPv6_GetNonce:

                    // Build entry
                    return LogEntry.createEntry(
                            new InternalActionFlag(InternalActionFlag.GET_NONCE),
                            timestamp,
                            LogUtil.getIPString(ipBytes),
                            null,
                            null);

                // - - - - - - - - - - - - -

                default:
                    throw new Exception("Shouldn't get here");
            }
        } finally {
            dis.close();
        }
    }

    /**
     * Return the length, in bytes, of the entry, including the checksum.
     * @return
     */
    public long length() {
        return this.length;
    }

    /**
     * Create a log entry with set data.
     * @param timestamp
     * @param ip
     * @param hash
     * @param sig
     * @return
     * @throws java.lang.Exception
     */
    public static LogEntry logSetData(long timestamp, String ip, BigHash hash, Signature sig) throws Exception {
        return LogEntry.createEntry(new InternalActionFlag(InternalActionFlag.SET_DATA),
                timestamp,
                ip,
                hash,
                LogUtil.createCertificateMD5(sig.getCert()));
    }

    /**
     * Create a log entry with meta data.
     * @param timestamp
     * @param ip
     * @param hash
     * @param sig
     * @return
     * @throws java.lang.Exception
     */
    public static LogEntry logSetMetaData(long timestamp, String ip, BigHash hash, Signature sig) throws Exception {
        return LogEntry.createEntry(new InternalActionFlag(InternalActionFlag.SET_META),
                timestamp,
                ip,
                hash,
                LogUtil.createCertificateMD5(sig.getCert()));
    }

    /**
     * Create a log entry with configuration data.
     * @param timestamp
     * @param ip
     * @param sig
     * @return
     * @throws java.lang.Exception
     */
    public static LogEntry logSetConfiguration(long timestamp, String ip, Signature sig) throws Exception {
        return LogEntry.createEntry(new InternalActionFlag(InternalActionFlag.SET_CONF),
                timestamp,
                ip,
                null,
                LogUtil.createCertificateMD5(sig.getCert()));
    }

    /**
     * Return a new log entry of the current log entry's data.
     * @param timestamp
     * @param ip
     * @param hash
     * @return
     * @throws java.lang.Exception
     */
    public static LogEntry logGetData(long timestamp, String ip, BigHash hash) throws Exception {
        return LogEntry.createEntry(new InternalActionFlag(InternalActionFlag.GET_DATA),
                timestamp,
                ip,
                hash,
                null);
    }

    /**
     * Return a new log entry of the current log entry's meta data.
     * @param timestamp
     * @param ip
     * @param hash
     * @return
     * @throws java.lang.Exception
     */
    public static LogEntry logGetMetaData(long timestamp, String ip, BigHash hash) throws Exception {
        return LogEntry.createEntry(new InternalActionFlag(InternalActionFlag.GET_META),
                timestamp,
                ip,
                hash,
                null);
    }

    /**
     * Return a new log entry with the current log entry's configuration data.
     * @param timestamp
     * @param ip
     * @return
     * @throws java.lang.Exception
     */
    public static LogEntry logGetConfiguration(long timestamp, String ip) throws Exception {
        return LogEntry.createEntry(new InternalActionFlag(InternalActionFlag.GET_CONF),
                timestamp,
                ip,
                null,
                null);
    }

    /**
     * Return a new log entry with the current log enty's nonce data.
     * @param timestamp
     * @param ip
     * @return
     * @throws java.lang.Exception
     */
    public static LogEntry logGetNonce(long timestamp, String ip) throws Exception {
        return LogEntry.createEntry(new InternalActionFlag(InternalActionFlag.GET_NONCE),
                timestamp,
                ip,
                null,
                null);
    }

    /**
     *  Serialized byte array representation of a log entry.
     * @return
     * @throws java.lang.Exception
     */
    public byte[] toByteArray() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            // Write timestamp
            baos.write(this.timestamp);
            // Write action
            baos.write(this.getAction());
            // Write IP address
            baos.write(this.IP);
            // Write hash (iff exists)
            if (this.hash != null) {
                baos.write(this.hash);
            }
            // Write signature (iff exists)
            if (this.signatureMD5 != null) {
                baos.write(this.signatureMD5);
            }

            baos.flush();
            return baos.toByteArray();
        } finally {
            baos.close();
        }
    }

    /**
     * Return simple string representation of log entry.
     * @return
     */
    @Override()
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        try {
            switch (this.action) {
                case ActionByte.IPv4_SetData:
                case ActionByte.IPv6_SetData:
                    buffer.append("Set data {");
                    break;
                case ActionByte.IPv4_SetMeta:
                case ActionByte.IPv6_SetMeta:
                    buffer.append("Set meta {");
                    break;
                case ActionByte.IPv4_SetConfig:
                case ActionByte.IPv6_SetConfig:
                    buffer.append("Set config {");
                    break;
                case ActionByte.IPv4_GetData:
                case ActionByte.IPv6_GetData:
                    buffer.append("Get data {");
                    break;
                case ActionByte.IPv4_GetMeta:
                case ActionByte.IPv6_GetMeta:
                    buffer.append("Get meta {");
                    break;
                case ActionByte.IPv4_GetConfig:
                case ActionByte.IPv6_GetConfig:
                    buffer.append("Get config {");
                    break;
                case ActionByte.IPv4_GetNonce:
                case ActionByte.IPv6_GetNonce:
                    buffer.append("Get nonce {");
                    break;
                default:
                    throw new RuntimeException("Unrecognized action: " + this.action);
            }

            buffer.append("\n");
            buffer.append("  timestamp=" + this.getTimestamp());
            buffer.append("\n");
            buffer.append("  ip=" + this.getClientIP());
            buffer.append("\n");

            if (this.hash != null) {
                buffer.append("  hash=" + this.getHash());
                buffer.append("\n");
            }
            if (this.signatureMD5 != null) {
                buffer.append("  sig=" + Base64.encodeBytes(this.getSignatureMD5()));
                buffer.append("\n");
            }

            buffer.append("}");
            buffer.append("\n");
        } catch (Exception ex) {
            buffer.append("Exception: " + ex.getMessage());
            buffer.append("\n");
            ex.printStackTrace(System.err);
        }
        return buffer.toString();
    }

    public byte getAction() {
        return action;
    }

    /**
     * Retrieve log entry timestamp.
     * @return
     * @throws java.lang.Exception
     */
    public long getTimestamp() throws Exception {
        return LogUtil.convertBytesToLong(this.timestamp);
    }

    /**
     * Retrieve the client's IP address as a string.
     * @return
     * @throws java.lang.Exception
     */
    public String getClientIP() throws Exception {
        return LogUtil.getIPString(this.IP);
    }

    /**
     * Retrieve BigHash.
     * @return
     * @throws java.lang.Exception
     */
    public BigHash getHash() throws Exception {
        if (this.hash == null) {
            return null;
        }

        return BigHash.createFromBytes(this.hash);
    }

    /**
     * Retrieve MD5 signature.
     * @return
     * @throws java.lang.Exception
     */
    public byte[] getSignatureMD5() throws Exception {
        return this.signatureMD5;
    }
}

/**
 * Used to pass info to factory method.
 * Use separate class for clarity sake: easily confused with ActionByte.
 */
class InternalActionFlag {

    /**
     * Used by factory method
     */
    protected static final byte SET_DATA = 1,
            SET_META = 2,
            SET_CONF = 3,
            GET_DATA = 4,
            GET_META = 5,
            GET_CONF = 6,
            GET_NONCE = 7;
    /**
     * Flag indicator for internal action.
     */
    public byte flag;

    /**
     * Create an InternalActionFlag using a byte flag from this class.
     * @param flag E.g., InternalActionFlag.GET_META
     */
    public InternalActionFlag(byte flag) {
        this.flag = flag;
    }

    /**
     * Returns the ActionByte representation of the action using the value of 
     * this message and the IP version byte in ActionByte.
     * @param IPVersionByte E.g., ActionByte.IPv4
     */
    public byte getActionForIPVersion(byte IPVersionByte) {
        return (byte) (this.flag + IPVersionByte);
    }
}
