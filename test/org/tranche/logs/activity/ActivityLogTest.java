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

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import org.tranche.TrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.network.ConnectionUtil;
import org.tranche.time.TimeUtil;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ActivityLogTest extends TrancheTestCase {

    final static int TEST_REPLICATIONS = 1;

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.setTestingActivityLogs(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtil.setTestingActivityLogs(false);
    }

    public void testReadAndWrite() throws Exception {
        TestUtil.printTitle("ActivityLogTest:testReadAndWrite()");
        for (int i = 0; i < TEST_REPLICATIONS; i++) {
            System.out.println("**************************************************************************");
            System.out.println("*** testReadAndWrite #" + i + " of " + TEST_REPLICATIONS);
            System.out.println("**************************************************************************");
            readAndWrite();
        }
    }

    /**
     * <p>Most time in this test is spent making signatures, so don't worry about the performance without checking average times for read and write.</p>
     * @throws java.lang.Exception
     */
    public void readAndWrite() throws Exception {
        File tmpDir = null;
        ActivityLog log = null;

        try {
            tmpDir = TempFileUtil.createTemporaryDirectory("testReadAndWrite");
            log = new ActivityLog(tmpDir);

//            Text.printRecursiveDirectoryStructure(tmpDir);

            // Must be 25 or greater or test will fail
            final int toAdd = 30;

            List<Activity> testAdd = new ArrayList(toAdd);
            List<Activity> testControl = new ArrayList(25);

            int total = toAdd + 25;
            for (int i = 0; i < total; i++) {
                Activity a = ActivityLogUtilTest.getRandomActivity();
//                System.out.println("DEBUG> Got random activity #" + String.valueOf(i+1) + " of "+" total.");
                if (i < toAdd) {
                    testAdd.add(a);
                } else {
                    testControl.add(a);
                }
            }

            int count = 0;
            for (Activity a : testAdd) {
                count++;
                log.write(a);
            }

            final long startingTimestamp = testAdd.get(0).getTimestamp();
            final long finishingTimestamp = testAdd.get(testAdd.size() - 1).getTimestamp();

            assertEquals("Should know last recorded timestamp.", finishingTimestamp, log.getLastRecordedTimestamp());

            List<Activity> readActivities = log.read(startingTimestamp, finishingTimestamp, Integer.MAX_VALUE, Activity.ANY);
            assertEquals("Expecting same number of activities.", testAdd.size(), readActivities.size());
            assertEquals("Expecting same number of activities.", testAdd.size(), log.getActivityCount(startingTimestamp, finishingTimestamp, Activity.ANY));

            for (int i = 0; i < readActivities.size(); i++) {
                assertEquals("Expecting read same as wrote.", testAdd.get(i), readActivities.get(i));
            }

            count = 0;
            for (Activity a : testAdd) {
                count++;
                assertTrue("Should find count #" + count + " of " + testAdd.size() + ".", log.contains(a.getTimestamp(), a.getAction(), a.getHash()));
            }

            count = 0;
            for (Activity a : testControl) {
                count++;
                assertFalse("Should not find count #" + count + " of " + testControl.size() + ".", log.contains(a.getTimestamp(), a.getAction(), a.getHash()));
            }

            /**
             * Interesting scenario: shouldn't find anything since starting with timestamp greater than last written
             * activity timestamp
             */
            readActivities = log.read(finishingTimestamp + 1, Long.MAX_VALUE, Integer.MAX_VALUE, Activity.ANY);
            assertEquals("Expecting nothing was found.", 0, readActivities.size());
            assertEquals("Expecting nothing was found.", 0, log.getActivityCount(finishingTimestamp + 1, Long.MAX_VALUE, Activity.ANY));

            // Starting at earlier timetstamp, read in everything after that
            for (int index = 0; index < testAdd.size(); index++) {
                Activity a = testAdd.get(index);
                int expectedReads = testAdd.size() - index;

                // Quickly check to see if any previous has same timestamp. If so,
                // will add extra to results.
                int j = index - 1;
                while (j >= 0 && testAdd.get(j).getTimestamp() == a.getTimestamp()) {
                    expectedReads++;
                    j--;
                }
                readActivities = log.read(a.getTimestamp(), Long.MAX_VALUE, Integer.MAX_VALUE, Activity.ANY);
                assertEquals("Expecting " + expectedReads + " activities logged starting with " + a.getTimestamp() + " (i=" + index + ").", expectedReads, readActivities.size());
                assertEquals("Expecting " + expectedReads + " activities logged starting with " + a.getTimestamp() + " (i=" + index + ").", expectedReads, log.getActivityCount(a.getTimestamp(), Long.MAX_VALUE, Activity.ANY));
            }

            // Now let's limit using ending timestamp
            count = 0;
            for (int index = testAdd.size() - 1; index >= 0; index--) {
                Activity a = testAdd.get(index);
                int expectedReads = testAdd.size() - count;

                count++;

                // Quickly check to see if next timestamp has same. If so, we'll see more
                int j = index + 1;
                while (j <= testAdd.size() - 1 && testAdd.get(j).getTimestamp() == a.getTimestamp()) {
                    expectedReads++;
                    j++;
                }
                readActivities = log.read(Long.MIN_VALUE, a.getTimestamp(), Integer.MAX_VALUE, Activity.ANY);
                assertEquals("Expecting " + expectedReads + " activities logged ending with " + a.getTimestamp() + " (i=" + index + ").", expectedReads, readActivities.size());
                assertEquals("Expecting " + expectedReads + " activities logged ending with " + a.getTimestamp() + " (i=" + index + ").", expectedReads, log.getActivityCount(Long.MIN_VALUE, a.getTimestamp(), Activity.ANY));
            }

            // Let's limit based on arbitrary number
            readActivities = log.read(startingTimestamp, finishingTimestamp, 8, Activity.ANY);
            assertEquals("Expecting 8 entries.", 8, readActivities.size());

            readActivities = log.read(startingTimestamp, finishingTimestamp, 1, Activity.ANY);
            assertEquals("Expecting 1 entries.", 1, readActivities.size());

            readActivities = log.read(startingTimestamp, finishingTimestamp, 25, Activity.ANY);
            assertEquals("Expecting 25 entries.", 25, readActivities.size());

            // Lastly, make sure throws exception if parameters are illegal
            try {
                log.read(startingTimestamp, startingTimestamp - 1, 50, Activity.ANY);
                fail("Should have thrown exception since starting timestamp later than ending timestamp.");
            } catch (Exception expected) { /* This is expected */ }

            // Let's try getting all of a certain kind using the mask
            int dataCount = 0;
            int metaDataCount = 0;
            int setCount = 0;
            int deleteCount = 0;
            int deleteDataCount = 0;
            int deleteMetaDataCount = 0;
            int setDataCount = 0;
            int setMetaDataCount = 0;
            for (Activity activity : testAdd) {
                switch (activity.getAction()) {
                    case Activity.DELETE_DATA:
                        deleteCount++;
                        deleteDataCount++;
                        dataCount++;
                        break;
                    case Activity.DELETE_META_DATA:
                        deleteCount++;
                        deleteMetaDataCount++;
                        metaDataCount++;
                        break;
                    case Activity.SET_DATA:
                        setCount++;
                        setDataCount++;
                        dataCount++;
                        break;
                    case Activity.SET_META_DATA:
                        setCount++;
                        setMetaDataCount++;
                        metaDataCount++;
                        break;
                    default:
                        fail("Unrecognized activity byte: " + activity.getAction());
                }
            }

            assertEquals("Expecting certain number of entries.", dataCount, log.getActivityCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.CHUNK_DATA));
            assertEquals("Expecting certain number of entries.", metaDataCount, log.getActivityCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.CHUNK_META_DATA));
            assertEquals("Expecting certain number of entries.", deleteCount, log.getActivityCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ACTION_DELETE));
            assertEquals("Expecting certain number of entries.", setCount, log.getActivityCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ACTION_SET));
            assertEquals("Expecting certain number of entries.", deleteDataCount, log.getActivityCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.DELETE_DATA));
            assertEquals("Expecting certain number of entries.", deleteMetaDataCount, log.getActivityCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.DELETE_META_DATA));
            assertEquals("Expecting certain number of entries.", setDataCount, log.getActivityCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.SET_DATA));
            assertEquals("Expecting certain number of entries.", setMetaDataCount, log.getActivityCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.SET_META_DATA));

            System.out.println("    Avg. time writing: " + Text.getPrettyEllapsedTimeString(log.getAvgWriteTimeInMillis()));
            System.out.println("    Avg. time reading: " + Text.getPrettyEllapsedTimeString(log.getAvgReadTimeInMillis()));
        } finally {
            try {
                log.close();
            } catch (Exception nope) { /* nope */ }
            IOUtil.recursiveDeleteWithWarning(tmpDir);
            IOUtil.safeDelete(log.getActivitiesLogFile());
            IOUtil.safeDelete(log.getBackupActivitiesLogFile());
            IOUtil.safeDelete(log.getBackupSignaturesFile());
            IOUtil.safeDelete(log.getBackupSignaturesIndexFile());
            IOUtil.safeDelete(log.getSignaturesFile());
            IOUtil.safeDelete(log.getSignaturesIndexFile());
        }
    }

    public void testSignatureIndexEntryCorruptedCannotRecover() throws Exception {
        TestUtil.printTitle("ActivityLogTest:testSignatureIndexEntryCorruptedCannotRecover()");
        for (int i = 0; i < TEST_REPLICATIONS; i++) {
            System.out.println("**************************************************************************");
            System.out.println("*** testSignatureIndexEntryCorruptedCannotRecover #" + i + " of " + TEST_REPLICATIONS);
            System.out.println("**************************************************************************");
            signatureIndexEntryCorruptedCannotRecover();
        }
    }

    public void signatureIndexEntryCorruptedCannotRecover() throws Exception {
        testSignatureIndexEntryCorrupted(false);
    }

    public void testSignatureIndexEntryCorruptedCanRecover() throws Exception {
        TestUtil.printTitle("ActivityLogTest:testSignatureIndexEntryCorruptedCanRecover()");
        for (int i = 0; i < TEST_REPLICATIONS; i++) {
            System.out.println("**************************************************************************");
            System.out.println("*** testSignatureIndexEntryCorruptedCanRecover #" + i + " of " + TEST_REPLICATIONS);
            System.out.println("**************************************************************************");
            signatureIndexEntryCorruptedCanRecover();
        }
    }

    public void signatureIndexEntryCorruptedCanRecover() throws Exception {
        testSignatureIndexEntryCorrupted(true);
    }

    private void testSignatureIndexEntryCorrupted(boolean isRecoverable) throws Exception {
        File activityLogFile = null, signatureIndexFile = null, signaturesFile = null;
        try {
            activityLogFile = TempFileUtil.createTemporaryFile();
            signatureIndexFile = TempFileUtil.createTemporaryFile();
            signaturesFile = TempFileUtil.createTemporaryFile();

            int activitySize = 1 + RandomUtil.getInt(12);

//            System.out.println("DEBUG> Using " + activitySize + " activities.");

            addActivitiesToLogsForTest(true, activitySize, activityLogFile, signatureIndexFile, signaturesFile);

            ActivityLog log = null;

            try {
                log = new ActivityLog(activityLogFile, signatureIndexFile, signaturesFile);
                assertEquals("Expecting certain size log.", activitySize, log.getActivityLogEntriesCount());
            } finally {
                log.close();
            }

            // Break the signatures index file somewhere in the last entry.
            long breakingStart = signatureIndexFile.length() - SignatureIndexEntry.SIZE + 1;
            int breakSpace = SignatureIndexEntry.SIZE - 1;


            long breakPoint = breakingStart + RandomUtil.getInt(breakSpace);

            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(signatureIndexFile, "rw");
                raf.setLength(breakPoint);
            } finally {
                try {
                    raf.close();
                } catch (Exception nope) { /* nope */ }
            }

            // Recoverable unless last signature broken.
            if (!isRecoverable) {
                // Let's break the last signature. Let's just destroy one of last ten bytes
                long signaturesBreakPoint = signaturesFile.length() - (1 + RandomUtil.getInt(10));

                try {
                    raf = new RandomAccessFile(signaturesFile, "rw");
                    raf.setLength(signaturesBreakPoint);
                } finally {
                    try {
                        raf.close();
                    } catch (Exception nope) { /* nope */ }
                }
            }

            // Load the log and see what happens...
            int expectedEntries = -1;
            try {
                log = new ActivityLog(activityLogFile, signatureIndexFile, signaturesFile);

                if (!isRecoverable) {
                    expectedEntries = activitySize - 1;
                    int foundEntries = (int) (signatureIndexFile.length() / SignatureIndexEntry.SIZE);
                    assertEquals("In non-recoverable test, expecting certain number entries.", expectedEntries, foundEntries);
                    assertEquals("Expecting certain size log for unrecoverable scenario.", activitySize - 1, log.getActivityLogEntriesCount());
                } else {
                    expectedEntries = activitySize;
                    int foundEntries = (int) (signatureIndexFile.length() / SignatureIndexEntry.SIZE);
                    assertEquals("In recoverable test, expecting certain number entries.", expectedEntries, foundEntries);
                    assertEquals("Expecting certain size log for recoverable scenario.", activitySize, log.getActivityLogEntriesCount());
                }
            } finally {
                log.close();
            }

            // Add some more to make sure nothing was corrupted
            int additionalActivitySize = 1 + RandomUtil.getInt(12);

//            System.out.println("DEBUG> Adding " + additionalActivitySize + " more activities.");

            List<Activity> additionalActivities = addActivitiesToLogsForTest(false, additionalActivitySize, activityLogFile, signatureIndexFile, signaturesFile);

            // Load the log and see what happens...
            try {
                log = new ActivityLog(activityLogFile, signatureIndexFile, signaturesFile);
                assertEquals("Expecting certain number of activity logs.", expectedEntries + additionalActivitySize, log.getActivityLogEntriesCount());

                for (Activity a : additionalActivities) {
                    assertTrue("Should contain activities.", log.contains(a.getTimestamp(), a.getAction(), a.getHash()));
                }
            } finally {
                log.close();
            }
        } finally {
            IOUtil.safeDelete(activityLogFile);
            IOUtil.safeDelete(signatureIndexFile);
            IOUtil.safeDelete(signaturesFile);
        }
    }

    /**
     * <p>If signature bytes are corrupted, tests that they are removed properly.</p>
     * @throws java.lang.Exception
     */
    public void testSignatureBytesCorrupted() throws Exception {
        for (int i = 0; i < TEST_REPLICATIONS; i++) {
            System.out.println("**************************************************************************");
            System.out.println("*** testSignatureBytesCorrupted #" + i + " of " + TEST_REPLICATIONS);
            System.out.println("**************************************************************************");
            signatureBytesCorrupted();
        }
    }

    public void signatureBytesCorrupted() throws Exception {
        File activityLogFile = null, signatureIndexFile = null, signaturesFile = null;
        try {
            activityLogFile = TempFileUtil.createTemporaryFile();
            signatureIndexFile = TempFileUtil.createTemporaryFile();
            signaturesFile = TempFileUtil.createTemporaryFile();

            int activitySize = 1 + RandomUtil.getInt(12);

//            System.out.println("DEBUG> Using " + activitySize + " activities.");

            List<Activity> activities = addActivitiesToLogsForTest(true, activitySize, activityLogFile, signatureIndexFile, signaturesFile);

            ActivityLog log = null;
            try {
                log = new ActivityLog(activityLogFile, signatureIndexFile, signaturesFile);
                assertEquals("Expecting certain size log.", activitySize, log.getActivityLogEntriesCount());
            } finally {
                log.close();
            }

            final int lastSignatureLength = activities.get(activities.size() - 1).getSignature().toByteArray().length;

            // Break the signatures file at some point in the last entry
            final long snipPointFromEnd = 1 + RandomUtil.getInt(lastSignatureLength - 2);

            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(signaturesFile, "rw");
                raf.setLength(signaturesFile.length() - snipPointFromEnd);
            } finally {
                try {
                    raf.close();
                } catch (Exception nope) { /* nope */ }
            }

            // Load the log and see what happens...
            try {
                log = new ActivityLog(activityLogFile, signatureIndexFile, signaturesFile);
                assertEquals("Expecting certain number of activity logs.", activitySize - 1, log.getActivityLogEntriesCount());
            } finally {
                log.close();
            }

            // Add some more to make sure nothing was corrupted
            int additionalActivitySize = 1 + RandomUtil.getInt(12);

//            System.out.println("DEBUG> Adding " + additionalActivitySize + " more activities.");

            List<Activity> additionalActivities = addActivitiesToLogsForTest(false, additionalActivitySize, activityLogFile, signatureIndexFile, signaturesFile);

            // Load the log and see what happens...
            try {
                log = new ActivityLog(activityLogFile, signatureIndexFile, signaturesFile);
                assertEquals("Expecting certain number of activity logs.", activitySize - 1 + additionalActivitySize, log.getActivityLogEntriesCount());

                for (Activity a : additionalActivities) {
                    assertTrue("Should contain activities.", log.contains(a.getTimestamp(), a.getAction(), a.getHash()));
                }
            } finally {
                log.close();
            }
        } finally {
            IOUtil.safeDelete(activityLogFile);
            IOUtil.safeDelete(signatureIndexFile);
            IOUtil.safeDelete(signaturesFile);
        }
    }

    /**
     * <p>If activity log bytes are corrupted, tests that they are removed properly.</p>
     * @throws java.lang.Exception
     */
    public void testActivityLogBytesCorrupted() throws Exception {
        for (int i = 0; i < TEST_REPLICATIONS; i++) {
            System.out.println("**************************************************************************");
            System.out.println("*** testActivityLogBytesCorrupted #" + i + " of " + TEST_REPLICATIONS);
            System.out.println("**************************************************************************");
            activityLogBytesCorrupted();
        }
    }

    public void activityLogBytesCorrupted() throws Exception {
        File activityLogFile = null, signatureIndexFile = null, signaturesFile = null;
        try {
            activityLogFile = TempFileUtil.createTemporaryFile();
            signatureIndexFile = TempFileUtil.createTemporaryFile();
            signaturesFile = TempFileUtil.createTemporaryFile();

            int activitySize = 1 + RandomUtil.getInt(12);

//            System.out.println("DEBUG> Using " + activitySize + " activities.");

            addActivitiesToLogsForTest(true, activitySize, activityLogFile, signatureIndexFile, signaturesFile);

            ActivityLog log = null;
            try {
                log = new ActivityLog(activityLogFile, signatureIndexFile, signaturesFile);
                assertEquals("Expecting certain size log.", activitySize, log.getActivityLogEntriesCount());
            } finally {
                log.close();
            }

            // Break the signatures file at some point in the last entry
            final long snipPointFromEnd = 1 + RandomUtil.getInt(ActivityLogEntry.SIZE - 2);

            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(activityLogFile, "rw");
                long snipAt = raf.length() - snipPointFromEnd;
                raf.setLength(snipAt);
            } finally {
                try {
                    raf.close();
                } catch (Exception nope) { /* nope */ }
            }

            // Load the log and see what happens...
            try {
                log = new ActivityLog(activityLogFile, signatureIndexFile, signaturesFile);
                assertEquals("Expecting certain number of activity logs.", activitySize - 1, log.getActivityLogEntriesCount());
            } finally {
                log.close();
            }

            // Add some more to make sure nothing was corrupted
            int additionalActivitySize = 1 + RandomUtil.getInt(12);

//            System.out.println("DEBUG> Adding " + additionalActivitySize + " more activities.");

            List<Activity> additionalActivities = addActivitiesToLogsForTest(false, additionalActivitySize, activityLogFile, signatureIndexFile, signaturesFile);

            // Load the log and see what happens...
            try {
                log = new ActivityLog(activityLogFile, signatureIndexFile, signaturesFile);
                assertEquals("Expecting certain number of activity logs.", activitySize - 1 + additionalActivitySize, log.getActivityLogEntriesCount());

                for (Activity a : additionalActivities) {
                    assertTrue("Should contain activities.", log.contains(a.getTimestamp(), a.getAction(), a.getHash()));
                }
            } finally {
                log.close();
            }

        } finally {
            IOUtil.safeDelete(activityLogFile);
            IOUtil.safeDelete(signatureIndexFile);
            IOUtil.safeDelete(signaturesFile);
        }
    }

    /**
     * <p>Simplify tests that corrupt logs by generating data.</p>
     * @param isPast If should use past activities. Else, use future.
     * @param activityLogFile
     * @param signatureIndexFile
     * @param signaturesFile
     */
    private List<Activity> addActivitiesToLogsForTest(boolean isPast, int entries, File activityLogFile, File signatureIndexFile, File signaturesFile) throws Exception {

        ActivityLog log = null;
        List<Activity> activities = new ArrayList(entries);
        try {
            log = new ActivityLog(activityLogFile, signatureIndexFile, signaturesFile);

            for (int i = 0; i < entries; i++) {
                Activity activity = ActivityLogUtilTest.getRandomActivity();
                activities.add(activity);
            }

            for (Activity activity : activities) {
                log.write(activity);
            }

            return activities;
        } finally {
            try {
                log.close();
            } catch (Exception e) { /* nope */ }
        }
    }

    public void testCanGetEntriesFromServer() throws Exception {
        long start = TimeUtil.getTrancheTimestamp();

        String HOST1 = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET, DevUtil.DEV_USER_SET));
        try {
            testNetwork.start();

            ActivityLog log = testNetwork.getFlatFileTrancheServer(HOST1).getActivityLog();
            assertEquals("Log should be empty.", 0, log.getActivityLogEntriesCount());

            TrancheServer rserver = ConnectionUtil.connectHost(HOST1, true);
            assertNotNull(rserver);

            int dataCount = 0;
            int metaDataCount = 0;
            int setCount = 0;
            int deleteCount = 0;
            int deleteDataCount = 0;
            int deleteMetaDataCount = 0;
            int setDataCount = 0;
            int setMetaDataCount = 0;

            // Three random activities
            byte activity1 = Byte.MAX_VALUE, activity2 = Byte.MAX_VALUE, activity3 = Byte.MAX_VALUE;
            BigHash hash1 = null, hash2 = null, hash3 = null;

            for (int i = 0; i < 3; i++) {
                byte activity = Byte.MAX_VALUE;
                BigHash hash = null;
                int randomInt = RandomUtil.getInt(2);
                switch (randomInt) {
                    case 0:
                        activity = Activity.SET_DATA;
                        byte[] dataChunk = DevUtil.createRandomDataChunk1MB();
                        hash = new BigHash(dataChunk);
                        dataCount++;
                        setCount++;
                        setDataCount++;
                        IOUtil.setData(rserver, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, dataChunk);
                        break;
                    case 1:
                        activity = Activity.SET_META_DATA;
                        byte[] metaDataChunk = DevUtil.createRandomMetaDataChunk();
                        hash = DevUtil.getRandomBigHash();
                        metaDataCount++;
                        setCount++;
                        setMetaDataCount++;
                        IOUtil.setMetaData(rserver, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, metaDataChunk);
                        break;
                    default:
                        throw new Exception("Unrecognized randomInt=" + randomInt + " (Should be either 0 or 1)");
                }

                if (i == 0) {
                    activity1 = activity;
                    hash1 = hash;
                } else if (i == 1) {
                    activity2 = activity;
                    hash2 = hash;
                } else if (i == 2) {
                    activity3 = activity;
                    hash3 = hash;
                } else {
                    throw new Exception("Unrecognized run i=" + i + " (expected 0 <= i < 3)");
                }
            }

            // For luck
            Thread.sleep(2000);
            long stop = TimeUtil.getTrancheTimestamp();

            int count = rserver.getActivityLogEntriesCount(start, stop, Activity.ANY);
            assertEquals("Expecting certain number of log entries.", 3, count);

            Activity[] activities = rserver.getActivityLogEntries(start, stop, Integer.MAX_VALUE, Activity.ANY);
            assertEquals("Expecting certain number of returned activites.", 3, activities.length);

            assertEquals("Should know all returned activity bytes.", activity1, activities[0].getAction());
            assertEquals("Should know all returned activity bytes.", activity2, activities[1].getAction());
            assertEquals("Should know all returned activity bytes.", activity3, activities[2].getAction());

            assertEquals("Should know all returned BigHash values.", hash1, activities[0].getHash());
            assertEquals("Should know all returned BigHash values.", hash2, activities[1].getHash());
            assertEquals("Should know all returned BigHash values.", hash3, activities[2].getHash());

            // Let's delete the first thing!
            boolean isMetaData = Activity.isMetaData(activity1);
            byte activity4 = Byte.MAX_VALUE;
            if (isMetaData) {
                activity4 = Activity.DELETE_META_DATA;
                IOUtil.deleteMetaData(rserver, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash1);
                metaDataCount++;
                deleteCount++;
                deleteMetaDataCount++;
            } else {
                activity4 = Activity.DELETE_DATA;
                IOUtil.deleteData(rserver, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash1);
                dataCount++;
                deleteCount++;
                deleteDataCount++;
            }

            // For luck
            Thread.sleep(2000);
            stop = TimeUtil.getTrancheTimestamp();

            count = rserver.getActivityLogEntriesCount(start, stop, Activity.ANY);
            assertEquals("Expecting certain number of log entries.", 4, count);

            activities = rserver.getActivityLogEntries(start, stop, Integer.MAX_VALUE, Activity.ANY);
            assertEquals("Expecting certain number of returned activites.", 4, activities.length);

            assertEquals("Should know all returned activity bytes.", activity1, activities[0].getAction());
            assertEquals("Should know all returned activity bytes.", activity2, activities[1].getAction());
            assertEquals("Should know all returned activity bytes.", activity3, activities[2].getAction());
            assertEquals("Should know all returned activity bytes.", activity4, activities[3].getAction());

            assertEquals("Should know all returned BigHash values.", hash1, activities[0].getHash());
            assertEquals("Should know all returned BigHash values.", hash2, activities[1].getHash());
            assertEquals("Should know all returned BigHash values.", hash3, activities[2].getHash());
            assertEquals("Should know all returned BigHash values.", hash1, activities[3].getHash());

            assertEquals("Expecting certain number of entries.", deleteDataCount, rserver.getActivityLogEntriesCount(start, stop, Activity.DELETE_DATA));
            assertEquals("Expecting certain number of entries.", deleteMetaDataCount, rserver.getActivityLogEntriesCount(start, stop, Activity.DELETE_META_DATA));
            assertEquals("Expecting certain number of entries.", setDataCount, rserver.getActivityLogEntriesCount(start, stop, Activity.SET_DATA));
            assertEquals("Expecting certain number of entries.", setMetaDataCount, rserver.getActivityLogEntriesCount(start, stop, Activity.SET_META_DATA));
            assertEquals("Expecting certain number of entries.", dataCount, rserver.getActivityLogEntriesCount(start, stop, Activity.CHUNK_DATA));
            assertEquals("Expecting certain number of entries.", metaDataCount, rserver.getActivityLogEntriesCount(start, stop, Activity.CHUNK_META_DATA));
            assertEquals("Expecting certain number of entries.", deleteCount, rserver.getActivityLogEntriesCount(start, stop, Activity.ACTION_DELETE));
            assertEquals("Expecting certain number of entries.", setCount, rserver.getActivityLogEntriesCount(start, stop, Activity.ACTION_SET));

            // Should be equivelent to use Long.MIN_VALUE as starting timestamp and
            // Long.MAX_VALUE as ending timestamp. However, there were problems with the log
            // doing this properly. Make sure doesn't break.
            assertEquals("Expecting certain number of entries.", deleteDataCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.DELETE_DATA));
            assertEquals("Expecting certain number of entries.", deleteMetaDataCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.DELETE_META_DATA));
            assertEquals("Expecting certain number of entries.", setDataCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.SET_DATA));
            assertEquals("Expecting certain number of entries.", setMetaDataCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.SET_META_DATA));
            assertEquals("Expecting certain number of entries.", dataCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.CHUNK_DATA));
            assertEquals("Expecting certain number of entries.", metaDataCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.CHUNK_META_DATA));
            assertEquals("Expecting certain number of entries.", deleteCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ACTION_DELETE));
            assertEquals("Expecting certain number of entries.", setCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.ACTION_SET));

            // Actually get logs and verify count
            Activity[] verifyActivities = null;

            verifyActivities = rserver.getActivityLogEntries(start, stop, Integer.MAX_VALUE, Activity.DELETE_DATA);
            assertEquals("Expecting certain number of entries.", deleteDataCount, verifyActivities.length);
            verifyActivities = rserver.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, Activity.DELETE_DATA);
            assertEquals("Expecting certain number of entries.", deleteDataCount, verifyActivities.length);

//            assertEquals("Expecting certain number of entries.", deleteMetaDataCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.DELETE_META_DATA));

            verifyActivities = rserver.getActivityLogEntries(start, stop, Integer.MAX_VALUE, Activity.DELETE_META_DATA);
            assertEquals("Expecting certain number of entries.", deleteMetaDataCount, verifyActivities.length);
            verifyActivities = rserver.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, Activity.DELETE_META_DATA);
            assertEquals("Expecting certain number of entries.", deleteMetaDataCount, verifyActivities.length);

//            assertEquals("Expecting certain number of entries.", setDataCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.SET_DATA));

            verifyActivities = rserver.getActivityLogEntries(start, stop, Integer.MAX_VALUE, Activity.SET_DATA);
            assertEquals("Expecting certain number of entries.", setDataCount, verifyActivities.length);
            verifyActivities = rserver.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, Activity.SET_DATA);
            assertEquals("Expecting certain number of entries.", setDataCount, verifyActivities.length);

//            assertEquals("Expecting certain number of entries.", setMetaDataCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.SET_META_DATA));

            verifyActivities = rserver.getActivityLogEntries(start, stop, Integer.MAX_VALUE, Activity.SET_META_DATA);
            assertEquals("Expecting certain number of entries.", setMetaDataCount, verifyActivities.length);
            verifyActivities = rserver.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, Activity.SET_META_DATA);
            assertEquals("Expecting certain number of entries.", setMetaDataCount, verifyActivities.length);

//            assertEquals("Expecting certain number of entries.", dataCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.CHUNK_DATA));

            verifyActivities = rserver.getActivityLogEntries(start, stop, Integer.MAX_VALUE, Activity.CHUNK_DATA);
            assertEquals("Expecting certain number of entries.", dataCount, verifyActivities.length);
            verifyActivities = rserver.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, Activity.CHUNK_DATA);
            assertEquals("Expecting certain number of entries.", dataCount, verifyActivities.length);

//            assertEquals("Expecting certain number of entries.", metaDataCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.CHUNK_META_DATA));

            verifyActivities = rserver.getActivityLogEntries(start, stop, Integer.MAX_VALUE, Activity.CHUNK_META_DATA);
            assertEquals("Expecting certain number of entries.", metaDataCount, verifyActivities.length);
            verifyActivities = rserver.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, Activity.CHUNK_META_DATA);
            assertEquals("Expecting certain number of entries.", metaDataCount, verifyActivities.length);

//            assertEquals("Expecting certain number of entries.", deleteCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.DELETE));

            verifyActivities = rserver.getActivityLogEntries(start, stop, Integer.MAX_VALUE, Activity.ACTION_DELETE);
            assertEquals("Expecting certain number of entries.", deleteCount, verifyActivities.length);
            verifyActivities = rserver.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, Activity.ACTION_DELETE);
            assertEquals("Expecting certain number of entries.", deleteCount, verifyActivities.length);

//            assertEquals("Expecting certain number of entries.", setCount, rserver.getActivityLogEntriesCount(Long.MIN_VALUE, Long.MAX_VALUE, Activity.SET));

            verifyActivities = rserver.getActivityLogEntries(start, stop, Integer.MAX_VALUE, Activity.ACTION_SET);
            assertEquals("Expecting certain number of entries.", setCount, verifyActivities.length);
            verifyActivities = rserver.getActivityLogEntries(Long.MIN_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, Activity.ACTION_SET);
            assertEquals("Expecting certain number of entries.", setCount, verifyActivities.length);

        } finally {
            testNetwork.stop();
        }
    }
}
