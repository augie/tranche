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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.security.cert.X509Certificate;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.tranche.time.TimeUtil;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.util.Text;

/**
 * Utility class for server logging mechanisms.
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class LogUtil {

    /**
     * Turn on to get useful debugging statements.
     */
    private static final boolean isDebug = false;

    /**
     * Get the IP version for an address
     * @param ip The string representing the IP address
     * @return The ActionByte representation of the IP version.
     */
    public static byte getIPVersion(String ip) {

        // Check whether IPv4
        if (ip.contains(".")) {
            String[] tokens = ip.split("[.]");
            if (tokens.length == 4) {
                return ActionByte.IPv4;
            }
        } // Check whether IPv6
        else if (ip.contains(":")) {
            String[] tokens = ip.split(":");
            // Variable since can concat running zeros
            if (tokens.length >= 2) {
                return ActionByte.IPv6;
            }
        }

        // Unsupported
        throw new RuntimeException("IP address not supported: " + ip);
    }

    /**
     * Return bytes for an IPv4 address string.
     * @param ip
     * @return
     */
    public static byte[] getIPv4Bytes(String ip) {
        // Format: 0.0.0.0 .. 255.255.255.255
        String[] octets = ip.split("[.]");

        byte[] bytes = new byte[4];

        for (int i = 0; i < bytes.length; i++) {
            // Parse int since bytes are unsigned, 128 and above would
            // throw NumberFormatException
            int next = Integer.parseInt(octets[i]);
            // Adjust to correct byte range (not absolute)
            bytes[i] = (byte) (Byte.MIN_VALUE + next);
        }

        return bytes;
    }

    /**
     * Returns bytes for an IPv6 address string.
     * @param ip
     * @return
     */
    public static byte[] getIPv6Bytes(String ip) {
        throw new UnsupportedOperationException("Add support for IPv6 in LogEntry!");
    }

    /**
     * Retrieve IP address as a formatted string.
     * @param IPBytes
     * @return
     * @throws java.lang.Exception
     */
    public static String getIPString(byte[] IPBytes) throws Exception {
        StringBuffer ip = new StringBuffer();

        if (IPBytes.length == 4) {
            for (int i = 0; i < IPBytes.length; i++) {

                // Convert to unsigned int
                int next = IPBytes[i] + Math.abs(Byte.MIN_VALUE);

                ip.append(next);
                if (i < IPBytes.length - 1) {
                    ip.append(".");
                }
            }
        } else if (IPBytes.length == 16) {
            throw new UnsupportedOperationException("Implement IPv6.");
        } else {
            throw new Exception("Unrecognized IP version for " + IPBytes.length + ". Expecting 4 bytes (IPv4) or 16 bytes (IPv6).");
        }
        return ip.toString();
    }

    /**
     * Generate a MD5 certificate.
     * @param cert
     * @return
     * @throws java.lang.Exception
     */
    public static byte[] createCertificateMD5(X509Certificate cert) throws Exception {
        return SecurityUtil.hash(cert.getEncoded(), "md5");
    }

    /**
     * Convert long to bytes and return the byte array representation.
     * @param value
     * @return
     * @throws java.lang.Exception
     */
    public static byte[] convertLongToBytes(long value) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeLong(value);
        dos.flush();

        try {
            return baos.toByteArray();
        } finally {
            dos.close();
        }
    }

    /**
     * Convert bytes to longs and return long representation.
     * @param longVal
     * @return
     * @throws java.lang.Exception
     */
    public static long convertBytesToLong(byte[] longVal) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(longVal);
        DataInputStream dis = new DataInputStream(bais);

        try {
            return dis.readLong();
        } finally {
            dis.close();
        }
    }

    /**
     * Create a single-byte XOR checksum for data.
     * @param data The data that requires a checksum.
     * @return A byte representing the checksum.
     */
    public static byte createXORChecksum(byte[] data) {
        byte checksum = 0;
        for (byte b : data) {
            checksum ^= b;
        }
        return checksum;
    }

    /**
     * Validate a checksum byte for data
     * @param checksum A single-byte XOR checksum that needs validated
     * @param data The data for which the checksum was created.
     * @return True if validates, false otherwise.
     */
    public static boolean validateXORChecksum(byte checksum, byte[] data) {
        byte matchingChecksum = createXORChecksum(data);

        return checksum == matchingChecksum;
    }

    /**
     *
     */
    private static void printTracer(String msg) {
        if (isDebug) {
            System.out.println("LOG_UTIL> " + msg);
        }
    }

    /**
     * Upload a file to a server.
     * @return True if server returns HTTP code stating already added, false otherwise.
     */
    public static boolean uploadLogFile(File logFile, String fromServerIP, String POST_URL) throws Exception {
        return uploadLogFile(logFile, fromServerIP, POST_URL, true);
    }

    /**
     * Upload a log file to server.
     * @param isCompleteHour True only if an entire hour has passed.
     * @return True if server returns HTTP code stating already added, false otherwise.
     */
    public static boolean uploadLogFile(File logFile, String fromServerIP, String POST_URL, boolean isCompleteHour) throws Exception {

        boolean isAlreadyAdded = false;

        // If url doesn't start with http, see if it is local dir.
        if (!POST_URL.startsWith("http:")) {
            File localDir = new File(POST_URL);

            if (localDir.exists() && localDir.isDirectory()) {
                File copy = new File(localDir, logFile.getName());
                copy.createNewFile();
                IOUtil.copyFile(logFile, copy);

                printTracer("Log copied to " + copy.getAbsolutePath());
                // Doesn't matter, test
                return true;
            }
        }

        int tryCount = 0;
        boolean tryAgain = true;

        while (tryAgain) {
            try {

                // Multipart POST
                PostMethod filePost = new PostMethod(POST_URL);

                FilePart filePart = new FilePart("log", logFile);

                Part[] parts = {
                    new StringPart("server", fromServerIP),
                    new StringPart("isCompleteHour", String.valueOf(isCompleteHour)),
                    filePart
                };

                printTracer("Uploading log (server=" + fromServerIP + ",isCompleteHour=" + isCompleteHour + ") at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));

                filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost.getParams()));
                HttpClient client = new HttpClient();
                int status = client.executeMethod(filePost);

                // Status code of 200 means that the server log was already processed. 
                // 202 means successfully queued.

                if (status == 200) {
                    isAlreadyAdded = true;
                }

                if (status < 400) {
                    tryAgain = false;
                }
                if (status >= 400) {
                    throw new UploadFailedException("Couldn't post log to server, HTTP response code: " + status);
                }

            } catch (UploadFailedException lfx) {
                // rethrow
                throw lfx;
            } catch (Exception ex) {
                tryCount++;
                if (tryCount > 3) {
                    // Give up
                    tryAgain = false;
                }
            }
        } // while try again...

        return isAlreadyAdded;
    } // uploadLogFile
} // LogUtil
