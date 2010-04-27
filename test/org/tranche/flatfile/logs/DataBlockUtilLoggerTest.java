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
package org.tranche.flatfile.logs;

import org.tranche.exceptions.TodoException;
import org.tranche.util.TrancheTestCase;

/**
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class DataBlockUtilLoggerTest extends TrancheTestCase {

    /**
     * Tests logs are accurate. Also piggy-backs some simple testing to keep test run-time low.
     */
    public void testMemoryAndDiskLogActivity() throws Exception {
        // need to update to new connection scheme
        if (true) {
            throw new TodoException();
        }
//
//        TestUtil.setTesting(true);
//        TestUtil.setTestUsingFakeCoreServers(false);
//
//        Random r = new Random();
//
//        File tmpDir = TempFileUtil.createTemporaryDirectory();
//
//        File logFile = TempFileUtil.createTemporaryFile(".log");
//
//        FlatFileTrancheServer ffts = null;
//        DataBlockUtilLogger logger = null;
//        DiskBackedTransactionLog dlog = null;
//        BufferedReader reader = null;
//
//        try {
//            // Create ffts instance
//            ffts = new FlatFileTrancheServer(tmpDir);
//            ffts.getConfiguration().getUsers().add(DevUtil.getDevUser());
//
//            // Turn logging on
//            ffts.getDataBlockUtil().setIsLogging(true);
//            logger = ffts.getDataBlockUtil().getLogger();
//
//            // The in-memory aggregate is default. Additionally, adding disk
//            dlog = new DiskBackedTransactionLog(logFile);
//            ffts.getDataBlockUtil().getLogger().addLog(dlog);
//
//            // Determine activity
//            int setData = r.nextInt(100);
//            int setMeta = r.nextInt(100);
//            int getData = r.nextInt(setData+1)-1;
//            int getMeta = r.nextInt(setMeta+1)-1;
//
//            // Intentionally fail at a few things
//            int getDataFail = r.nextInt(50);
//            int getMetaFail = r.nextInt(50);
//
//            // Store hashes to make sure no collision
//            Set<BigHash> dataHashes = new HashSet(),
//                    metaHashes = new HashSet();
//
//            BigHash nextHash = null;
//            byte[] nextBytes = new byte[r.nextInt(1000)+24];
//
//            // Set data
//            for (int i=0; i<setData; i++) {
//
//                while (true) {
//                    // Get next random bytes and hash
//                    r.nextBytes(nextBytes);
//                    nextHash = new BigHash(nextBytes);
//
//                    // Verify hash is new, or restart
//                    if (dataHashes.contains(nextHash)) continue;
//
//                    IOUtil.setData(ffts,DevUtil.getDevAuthority(),DevUtil.getDevPrivateKey(),nextHash,nextBytes);
//                    dataHashes.add(nextHash);
//
//                    break;
//                }
//            }
//
//            // Set meta data
//            for (int i=0; i<setMeta; i++) {
//
//                while (true) {
//                    // Get next random bytes and hash
//                    r.nextBytes(nextBytes);
//                    nextHash = new BigHash(nextBytes);
//
//                    // Verify hash is new, or restart
//                    if (metaHashes.contains(nextHash)) continue;
//
//                    IOUtil.setMetaData(ffts,DevUtil.getDevAuthority(),DevUtil.getDevPrivateKey(),nextHash,nextBytes);
//                    metaHashes.add(nextHash);
//
//                    break;
//                }
//            }
//
//            // Get data
//            int count = 0;
//            for (BigHash h : dataHashes) {
//                ffts.getData(h);
//
//                // Break out at right time
//                count++;
//                if (count >= getData) break;
//            }
//
//            // Get meta data
//            count = 0;
//            for (BigHash h : metaHashes) {
//                ffts.getMetaData(h);
//
//                // Break out at right time
//                count++;
//                if (count >= getMeta) break;
//            }
//
//            // Get data failed
//            for (int i=0; i<getDataFail; i++) {
//                while(true) {
//
//                    // Get next random bytes and hash
//                    r.nextBytes(nextBytes);
//                    nextHash = new BigHash(nextBytes);
//
//                    // Verify hash is new, or restart
//                    if (dataHashes.contains(nextHash)) continue;
//                    try {
//                        ffts.getData(nextHash);
//                        fail("Shouldn't find data that doesn't exist.");
//                    } catch (Exception ex) { /* nope */ }
//
//                    break;
//                }
//            }
//
//            // Get meta failed
//            for (int i=0; i<getMetaFail; i++) {
//                while(true) {
//
//                    // Get next random bytes and hash
//                    r.nextBytes(nextBytes);
//                    nextHash = new BigHash(nextBytes);
//
//                    // Verify hash is new, or restart
//                    if (metaHashes.contains(nextHash)) continue;
//                    try {
//                        ffts.getMetaData(nextHash);
//                        fail("Shouldn't find meta data that doesn't exist.");
//                    } catch (Exception ex) { /* nope */ }
//
//                    break;
//                }
//            }
//
//            // Verify logged information
//            DataBlockUtilLogger log = ffts.getDataBlockUtil().getLogger();
//
//            System.out.println(log);
//
//            assertEquals("Expecting certain number set successes (in-memory).",setData+setMeta,log.getBlockSetSuccesses());
//            assertEquals("Expecting certain number get successes (in-memory).",getData+getMeta,log.getBlockGetSuccesses());
//            assertEquals("Expecting certain number get failures (in-memory). Note ffts will check for data when adding. If ffts changes, this may fail.",setData+setMeta+getMetaFail+getDataFail,log.getBlockGetFailures());
//
//            assertEquals("Expecting certain number set successes (disk).",setData+setMeta,dlog.getBlockSetSuccesses());
//            assertEquals("Expecting certain number get successes (disk).",getData+getMeta,dlog.getBlockGetSuccesses());
//            assertEquals("Expecting certain number get failures (disk). Note ffts will check for data when adding. If ffts changes, this may fail.",setData+setMeta+getMetaFail+getDataFail,dlog.getBlockGetFailures());
//
//
//            dlog.close();
//
//            System.out.println(dlog);
//
//            System.out.println("*** DUMPING CONTENTS OF DISK-BACKED DISKUTIL LOG "+TextUtil.getFormattedBytes(logFile.length())+"***");
//            reader = new BufferedReader(new FileReader(logFile));
//
//            String line;
//            int i=0;
//            while((line = reader.readLine()) != null) {
//                System.out.println(i+": "+line);
//                i++;
//            }
//
//            // Piggy-back some simple testing of logs
//            assertEquals("Expecting certain number of logs",2,logger.getLogs().size());
//
//            // Clear, shouldn't be any logs
//            logger.clearLogs();
//            assertEquals("Shouldn't be any logs.",0,logger.getLogs().size());
//
//            // Re-add in-memory aggregate log
//            logger.addLog(new InMemoryAggregateLog());
//
//        } finally {
//            IOUtil.safeClose(ffts);
//            if (logger != null)
//                logger.close();
//            IOUtil.safeClose(reader);
//            IOUtil.recursiveDeleteWithWarning(tmpDir);
//            IOUtil.safeDelete(logFile);
//        }
    }
}
