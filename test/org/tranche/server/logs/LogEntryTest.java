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
import java.io.File;
import java.util.Iterator;
import org.tranche.security.SecurityUtil;
import org.tranche.security.Signature;
import org.tranche.hash.BigHash;
import org.tranche.time.TimeUtil;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.commons.RandomUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class LogEntryTest extends TrancheTestCase {

//    /**
//     * Create some entries, read them back.
//     */
//    public void testCreateEntries() throws Exception {
//        
//        List<LogEntry> entries = new ArrayList();
//        
//        // Use same signature for each action
//        Signature sig = getFauxSignature();
//        
//        long setDataTimestamp = TimeUtil.getTrancheTimestamp();
//        String setDataIP = "127.0.0.1";
//        BigHash setDataHash = createBigHashForProjectOfSize(getSmallProjectSize());
//        
//        LogEntry setDataEntry = LogEntry.logSetData(
//                setDataTimestamp,
//                setDataIP,
//                setDataHash,
//                sig);
//        entries.add(setDataEntry);
//        
//        // Sleep between 5 and 50 ms.
//        long sleep = 5 + r.nextInt(50);
//        Thread.sleep(sleep);
//        
//        long setMetaTimestamp = TimeUtil.getTrancheTimestamp();
//        String setMetaIP = "255.255.255.255";
//        BigHash setMetaHash = createBigHashForProjectOfSize(getSmallProjectSize());
//        
//        LogEntry setMetaEntry = LogEntry.logSetMetaData(
//                setMetaTimestamp,
//                setMetaIP,
//                setMetaHash,
//                sig);
//        entries.add(setMetaEntry);
//        
//        // Sleep between 5 and 50 ms.
//        sleep = 5 + r.nextInt(50);
//        Thread.sleep(sleep);
//        
//        long setConfigTimestamp = TimeUtil.getTrancheTimestamp();
//        String setConfigIP = "0.0.0.0";
//        
//        LogEntry setConfigEntry = LogEntry.logSetConfiguration(
//                setConfigTimestamp,
//                setConfigIP,
//                sig);
//        entries.add(setConfigEntry);
//        
//        // Sleep between 5 and 50 ms.
//        sleep = 5 + r.nextInt(50);
//        Thread.sleep(sleep);
//        
//        long getDataTimestamp = TimeUtil.getTrancheTimestamp();
//        String getDataIP = "123.145.167.189";
//        BigHash getDataHash = createBigHashForProjectOfSize(getSmallProjectSize());
//        
//        LogEntry getDataEntry = LogEntry.logGetData(
//                getDataTimestamp,
//                getDataIP,
//                getDataHash);
//        entries.add(getDataEntry);
//        
//        // Sleep between 5 and 50 ms.
//        sleep = 5 + r.nextInt(50);
//        Thread.sleep(sleep);
//        
//        long getMetaTimestamp = TimeUtil.getTrancheTimestamp();
//        String getMetaIP = setDataIP;
//        BigHash getMetaHash = createBigHashForProjectOfSize(getSmallProjectSize());
//        
//        LogEntry getMetaEntry = LogEntry.logGetMetaData(
//                getMetaTimestamp,
//                getMetaIP,
//                getMetaHash);
//        entries.add(getMetaEntry);
//        
//        // Sleep between 5 and 50 ms.
//        sleep = 5 + r.nextInt(50);
//        Thread.sleep(sleep);
//        
//        long getConfigTimestamp = TimeUtil.getTrancheTimestamp();
//        String getConfigIP = "1.2.3.4";
//        
//        LogEntry getConfigEntry = LogEntry.logGetConfiguration(
//                getConfigTimestamp,
//                getConfigIP);
//        entries.add(getConfigEntry);
//        
//        // Sleep between 5 and 50 ms.
//        sleep = 5 + r.nextInt(50);
//        Thread.sleep(sleep);
//        
//        long getNonceTimestamp = TimeUtil.getTrancheTimestamp();
//        String getNonceIP = setMetaIP;
//        
//        LogEntry getNonceEntry = LogEntry.logGetNonce(
//                getNonceTimestamp,
//                getNonceIP);
//        entries.add(getNonceEntry);
//        
//        File logFile = TempFileUtil.createTemporaryFile(".log_entry_test.log");
//        LogWriter writer = null;
//        LogReader reader = null;
//        try {
//            writer = new LogWriter(logFile);
//            
//            // Write them all out to file
//            long length = 0;
//            for (LogEntry e : entries) {
//                length+=e.length();
//                writer.writeEntry(e);
//            }
//            writer.close();
//            
//            reader = new LogReader(logFile);
//            LogEntry next;
//            int count = 0;
//            
//            // Keep it simple, represent the MD5 as a string
//            byte[] sigMD5 = LogUtil.createCertificateMD5(sig.getCert());
//            String sig64 = Base64.encodeBytes(sigMD5);
//            
//            while (reader.hasNext()) {
//                next = reader.next();
//                
//                // Invoke toString -- make sure no NullPointerException
//                next.toString();
//                
//                // This is the tedious part of the test... make sure right
//                // entries with right values in right order
//                switch(count) {
//                    
//                    case 0:
//                        // Set data
//                        assertEquals("no match.",ActionByte.IPv4_SetData,next.getAction());
//                        assertEquals("no match.",setDataTimestamp,next.getTimestamp());
//                        assertEquals("no match.",setDataIP,next.getClientIP());
//                        assertEquals("no match.",setDataHash,next.getHash());
//                        assertEquals("no match.",sig64,Base64.encodeBytes(next.getSignatureMD5()));
//                        break;
//                        
//                    case 1:
//                        // Set meta
//                        assertEquals("no match.",ActionByte.IPv4_SetMeta,next.getAction());
//                        assertEquals("no match.",setMetaTimestamp,next.getTimestamp());
//                        assertEquals("no match.",setMetaIP,next.getClientIP());
//                        assertEquals("no match.",setMetaHash,next.getHash());
//                        assertEquals("no match.",sig64,Base64.encodeBytes(next.getSignatureMD5()));
//                        break;
//                        
//                        
//                    case 2:
//                        // Set config
//                        assertEquals("no match.",ActionByte.IPv4_SetConfig,next.getAction());
//                        assertEquals("no match.",setConfigTimestamp,next.getTimestamp());
//                        assertEquals("no match.",setConfigIP,next.getClientIP());
//                        assertNull("no match.",next.getHash());
//                        assertEquals("no match.",sig64,Base64.encodeBytes(next.getSignatureMD5()));
//                        break;
//                        
//                    case 3:
//                        // Get data
//                        assertEquals("no match.",ActionByte.IPv4_GetData,next.getAction());
//                        assertEquals("no match.",getDataTimestamp,next.getTimestamp());
//                        assertEquals("no match.",getDataIP,next.getClientIP());
//                        assertEquals("no match.",getDataHash,next.getHash());
//                        assertNull("no match.",next.getSignatureMD5());
//                        break;
//                        
//                    case 4:
//                        // Get meta
//                        assertEquals("no match.",ActionByte.IPv4_GetMeta,next.getAction());
//                        assertEquals("no match.",getMetaTimestamp,next.getTimestamp());
//                        assertEquals("no match.",getMetaIP,next.getClientIP());
//                        assertNull("no match.",next.getSignatureMD5());
//                        break;
//                        
//                    case 5:
//                        // Get config
//                        assertEquals("no match.",ActionByte.IPv4_GetConfig,next.getAction());
//                        assertEquals("no match.",getConfigTimestamp,next.getTimestamp());
//                        assertEquals("no match.",getConfigIP,next.getClientIP());
//                        assertNull("no match.",next.getHash());
//                        assertNull("no match.",next.getSignatureMD5());
//                        break;
//                        
//                    case 6:
//                        // Get nonce
//                        assertEquals("no match.",ActionByte.IPv4_GetNonce,next.getAction());
//                        assertEquals("no match.",getNonceTimestamp,next.getTimestamp());
//                        assertEquals("no match.",getNonceIP,next.getClientIP());
//                        assertNull("no match.",next.getHash());
//                        assertNull("no match.",next.getSignatureMD5());
//                        break;
//                }
//                count++;
//                
//            } // Verify entries from file
//            
//            // Make sure it read in seven entries
//            assertEquals("Expecting 7 entries were read in, instead found "+count,7,count);
//            
//        } finally {
//            if (writer != null) {
//                writer.close();
//            }
//            if (reader != null) {
//                reader.close();
//            }
//            IOUtil.safeDelete(logFile);
//        }
//    }
    /**
     * Going to write a file. Then we'll intentionally destroy an entry
     * and make sure they other entries are fine!
     */
    public void testChecksumNegative() throws Exception {

        String ip = "127.127.127.127";
        LogEntries entries = new LogEntries();

        // Build up one of each entry
        entries.add(LogEntry.logGetData(TimeUtil.getTrancheTimestamp(), ip, DevUtil.getRandomBigHash(123)));
        entries.add(LogEntry.logGetMetaData(TimeUtil.getTrancheTimestamp(), ip, DevUtil.getRandomBigHash(456)));
        entries.add(LogEntry.logGetConfiguration(TimeUtil.getTrancheTimestamp(), ip));
        entries.add(LogEntry.logGetNonce(TimeUtil.getTrancheTimestamp(), ip));
        entries.add(LogEntry.logSetConfiguration(TimeUtil.getTrancheTimestamp(), ip, DevUtil.getBogusSignature()));
        entries.add(LogEntry.logSetData(TimeUtil.getTrancheTimestamp(), ip, DevUtil.getRandomBigHash(789), DevUtil.getBogusSignature()));
        entries.add(LogEntry.logSetMetaData(TimeUtil.getTrancheTimestamp(), ip, DevUtil.getRandomBigHash(012), DevUtil.getBogusSignature()));

        assertEquals("Expecting 7 entries.", 7, entries.size());

        LogWriter writer = null;
        LogReader reader = null;
        File logFile = TempFileUtil.createTemporaryFile(".log-entry-test.log");
        File badLogFile = null;
        try {

            writer = new LogWriter(logFile);
            Iterator<LogEntry> it = entries.iterator();
            while (it.hasNext()) {
                writer.writeEntry(it.next());
            }
            writer.close();

            // Let's verify the file
            int count = 0;
            reader = new LogReader(logFile);
            while (reader.hasNext()) {
                reader.next();
                count++;
            }
            reader.close();

            assertEquals("Expecting 7 entries, found " + count, 7, count);

            // Now, let's add a single byte somewhere in the middle of the file
            badLogFile = TempFileUtil.createTemporaryFile("log-entry-test.bad.log");

            byte[] bytes = IOUtil.getBytes(logFile);

            // Pick an early bad byte -- how about in the middle of first hash!
//            int positionForBadByte = (int)Math.round((float)bytes.length/2.0) + 30;
            int positionForBadByte = 10;


            byte[] badBytes = new byte[bytes.length];

            System.arraycopy(bytes, 0, badBytes, 0, positionForBadByte);

            byte badByte = 0;
            while (badByte == 0 && badByte == bytes[positionForBadByte]) {
                badByte = (byte) (RandomUtil.getInt(256) - 128);
            }

            badBytes[positionForBadByte] = badByte;
            System.arraycopy(bytes, positionForBadByte + 1, badBytes, positionForBadByte + 1, bytes.length - positionForBadByte - 1);

            // This doesn't prove anything!'
            assertEquals("Expecting same num bytes", bytes.length, badBytes.length);

            IOUtil.setBytes(badBytes, badLogFile);

            // Let's read it. One entry should be skipped, other preserved!
            reader = new LogReader(badLogFile);
            count = 0;
            while (reader.hasNext()) {
                LogEntry trash = reader.next();
                count++;
            }
            assertEquals("Expecting 6 entries, found " + count, 6, count);

        } finally {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            IOUtil.safeDelete(logFile);
            if (badLogFile != null) {
                IOUtil.safeDelete(badLogFile);
            }
        }
    }

    /**
     * This signature is good for nothing.
     */
    private Signature getFauxSignature() throws Exception {
        DevUtil.getDevUser();
        String algorithm = SecurityUtil.getSignatureAlgorithm(DevUtil.getDevPrivateKey());
        byte[] bytes = new byte[256];
        RandomUtil.getBytes(bytes);

        byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(bytes), DevUtil.getDevPrivateKey(), algorithm);

        return new Signature(sig, algorithm, DevUtil.getDevAuthority());
    }

    private int getSmallProjectSize() {
        return RandomUtil.getInt(1024) + 256;
    }

    private BigHash createBigHashForProjectOfSize(int size) {
        byte[] data = new byte[size];
        RandomUtil.getBytes(data);
        return new BigHash(data);
    }
}
