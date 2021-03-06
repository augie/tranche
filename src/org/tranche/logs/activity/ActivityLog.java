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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.cert.CertificateEncodingException;
import java.util.LinkedList;
import java.util.List;
import org.tranche.ConfigureTranche;
import org.tranche.commons.Debuggable;
import org.tranche.commons.TextUtil;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.hash.BigHash;
import org.tranche.security.Signature;
import org.tranche.time.TimeUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.PersistentServerFileUtil;
import org.tranche.util.TestUtil;

/**
 * <p>Activity log used to log all requests that impact data held by Tranche server.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ActivityLog extends Debuggable {

    public static final String DEFAULT_FILE_NAME_ACTIVITIES = ".activities";
    public static final String DEFAULT_FILE_NAME_SIGNATURES_INDEX = ".signatures-index";
    public static final String DEFAULT_FILE_NAME_SIGNATURES_ENTRIES = ".signatures-entries";
    public static final String BACKUP_SUFFIX = ".repair-backup";
    private final File activitiesLogFile, signaturesIndexFile, signaturesFile, backupActivitiesLogFile, backupSignaturesIndexFile, backupSignaturesFile;
    private RandomAccessFile activitiesLogRAF, signaturesIndexRAF, signaturesRAF;
    private final SignatureMap signatureMap;
    private boolean isClosed = false;
    private long timeWriting, writeCount, timeReading, readCount;
    private boolean hasBeenBackedUp = false;
    /**
     * <p>Needed so thread safe. Even though synchronize on individual RAF, not know which thread will get monitor next when multiple simulatenous writes.</p>
     */
    final Object writeLock = new Object();
    /**
     * <p>Only used for record keeping. Monitor is quicly returned.</p>
     */
    final Object countLock = new Object();

    /**
     * <p>Instantiate an activity log used to log all requests that impact data held by Tranche server.</p>
     * <p>This constructor will create log files in the persistent directory using the default file names.</p>
     * @see PersistentFileUtil#getPersistentDirectory() 
     * @throws java.io.IOException If log files do not exist and cannot be created or other I/O exception
     */
    public ActivityLog() throws IOException, Exception {
        this(PersistentServerFileUtil.getPersistentDirectory());
    }

    /**
     * <p>Instantiate an activity log used to log all requests that impact data held by Tranche server.</p>
     * <p>This contrustor will create log files in the specified directory using the default file names.</p>
     * @param directoryForLogs Directory 
     * @throws java.io.IOException If log files do not exist and cannot be created or other I/O exception
     */
    public ActivityLog(File directoryForLogs) throws IOException, Exception {
        this(new File(directoryForLogs, DEFAULT_FILE_NAME_ACTIVITIES), new File(directoryForLogs, DEFAULT_FILE_NAME_SIGNATURES_INDEX), new File(directoryForLogs, DEFAULT_FILE_NAME_SIGNATURES_ENTRIES));
    }

    /**
     * <p>Instantiate an activity log used to log all requests that impact data held by Tranche server.</p>
     * <p>This constructor uses specified log file locations.</p>
     * @param activitiesLogFile
     * @param signaturesIndexFile
     * @param signaturesFile
     * @throws java.io.IOException If log files do not exist and cannot be created or other I/O exception
     */
    public ActivityLog(File activitiesLogFile, File signaturesIndexFile, File signaturesFile) throws IOException, Exception {
        this.activitiesLogFile = activitiesLogFile;
        this.signaturesIndexFile = signaturesIndexFile;
        this.signaturesFile = signaturesFile;

        // Don't need to see this for every server in test
        if (!TestUtil.isTesting()) {
            debugOut("Using activities log file:   " + this.activitiesLogFile.getAbsolutePath() + " (" + (this.activitiesLogFile.exists() ? String.valueOf(this.activitiesLogFile.length()) + " bytes" : "not exist yet") + ")");
            debugOut("Using signatures index file: " + this.signaturesIndexFile.getAbsolutePath() + " (" + (this.signaturesIndexFile.exists() ? String.valueOf(this.signaturesIndexFile.length()) + " bytes" : "not exist yet") + ")");
            debugOut("Using signatures file:       " + this.signaturesFile.getAbsolutePath() + " (" + (this.signaturesFile.exists() ? String.valueOf(this.signaturesFile.length()) + " bytes" : "not exist yet") + ")");
        }

        // Instantiate objects, but don't create or check existance. These will only be created
        // if any repair work is done.
        this.backupActivitiesLogFile = new File(this.getActivitiesLogFile().getAbsolutePath() + BACKUP_SUFFIX);
        this.backupSignaturesFile = new File(this.getSignaturesFile().getAbsolutePath() + BACKUP_SUFFIX);
        this.backupSignaturesIndexFile = new File(this.getSignaturesIndexFile().getAbsolutePath() + BACKUP_SUFFIX);

        if (!this.activitiesLogFile.exists()) {
            this.activitiesLogFile.getParentFile().mkdirs();
            this.activitiesLogFile.createNewFile();
        }

        if (!this.signaturesIndexFile.exists()) {
            this.signaturesIndexFile.getParentFile().mkdirs();
            this.signaturesIndexFile.createNewFile();
        }

        if (!this.signaturesFile.exists()) {
            this.signaturesFile.getParentFile().mkdirs();
            this.signaturesFile.createNewFile();
        }

        if (!this.activitiesLogFile.exists()) {
            throw new IOException("Cannot create log file at location: " + this.getActivitiesLogFile().getAbsolutePath());
        }
        if (!this.signaturesIndexFile.exists()) {
            throw new IOException("Cannot create log file at location: " + this.getSignaturesIndexFile().getAbsolutePath());
        }
        if (!this.signaturesFile.exists()) {
            throw new IOException("Cannot create log file at location: " + this.getSignaturesFile().getAbsolutePath());
        }

        // Create the random access files
        this.openRAF();

        // Since signatures are around 800 bytes (or roughly 1KB), 1000 is around 800KB (or roughly 1MB)
        this.signatureMap = new SignatureMap(signaturesIndexRAF, signaturesRAF);

        timeWriting = 0;
        writeCount = 0;
        timeReading = 0;
        readCount = 0;

        checkFilesForCorruption();
    }

    public static void main(String[] args) throws Exception {
        ConfigureTranche.load(args);
        File activities = new File("C:/Documents and Settings/James A Hill/Desktop/.activities");
        File signatureIndex = new File("C:/Documents and Settings/James A Hill/Desktop/.signatures-index");
        File signatures = new File("C:/Documents and Settings/James A Hill/Desktop/.signatures-entries");
        ActivityLog log = null;
        try {
            log = new ActivityLog(activities, signatureIndex, signatures);
            log.setDebug(true);
            System.out.println(log.getActivityLogEntriesCount());
            List<Activity> list = log.read(0, TimeUtil.getTrancheTimestamp(), 20, Activity.ANY);
            for (Activity a : list) {
                System.out.println(a.toString());
            }
        } finally {
            log.close();
        }
    }

    /**
     * <p>Not thread safe! Make sure single thread finishes this method before start multithreaded operations on log.</p>
     * @throws java.io.IOException
     */
    private void checkFilesForCorruption() throws IOException, Exception {

        try {

            int largestIntactSignatureIndex = -1;
            long signaturesIndexLen = this.signaturesIndexRAF.length();
            long signaturesLen = this.signaturesRAF.length();
            long activityLogLen = this.activitiesLogRAF.length();
            boolean alreadyCheckedActivityLog = false;

            /**
             * Step 1: check the signatures. This requires that both have same number of entries!
             */
            {
                // Step 1a. If signatures index is corrupted, remove corrupted bytes.
                long excessSignatureIndexBytes = signaturesIndexLen % SignatureIndexEntry.SIZE;
                if (excessSignatureIndexBytes != 0) {

                    // Quick assertion to make excess less than full entry
                    if (excessSignatureIndexBytes >= SignatureIndexEntry.SIZE) {
                        throw new AssertionFailedException("excess bytes<" + excessSignatureIndexBytes + "> should be less than entry<" + SignatureIndexEntry.SIZE + ">, but aren't.");
                    }

                    System.err.println("---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
                    System.err.println("Detected corruption in signature index file<" + this.getSignaturesIndexFile().getAbsolutePath() + ">. Ended with " + excessSignatureIndexBytes + " excess bytes. Repairing...");
                    System.err.println("---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

                    // Backup logs (if not already backed up)
                    backupFiles();

                    if (signaturesIndexLen - excessSignatureIndexBytes == 0) {

                        boolean isRecoverable = false;

                        try {
                            // Last-ditch effort to salvage one signature
                            byte[] possibleSignatureBytes = new byte[(int) this.getSignaturesFile().length()];
                            this.signaturesRAF.seek(0);
                            this.signaturesRAF.readFully(possibleSignatureBytes);
                            new Signature(possibleSignatureBytes);
                            debugOut("    ... no signature indices, but was able to read in signature from signatures file. Repairable.");

                            // Prevent log clearing
                            isRecoverable = true;

                            SignatureIndexEntry sie = new SignatureIndexEntry(0, 0l, (int) getSignaturesFile().length());
                            byte[] sieBytes = sie.toByteArray();
                            this.signaturesIndexRAF.seek(0);
                            this.signaturesIndexRAF.write(sieBytes);

                            // There's only one signature!
                            largestIntactSignatureIndex = 0;

                            // Go backwards in activity log until find the correct entry. Before starting,
                            // make sure there's at least one entry or don't bother.
                            //
                            // (Note that the activity log might be corrupted. This might fix, but this
                            //  will get checked later.)
                            if (activityLogLen / ActivityLogEntry.SIZE >= 1) {

                                final long intactEntries = (long) Math.floor((double) activityLogLen / (double) ActivityLogEntry.SIZE);
                                long startingOffset = (intactEntries - 1) * ActivityLogEntry.SIZE;

                                boolean findingCorrectActivityLogEntry = true;
                                ActivityLogEntry ale = null;

                                long desiredEntries = intactEntries;

                                while (findingCorrectActivityLogEntry) {

                                    byte[] activityBytes = new byte[ActivityLogEntry.SIZE];
                                    this.activitiesLogRAF.seek(startingOffset);
                                    this.activitiesLogRAF.readFully(activityBytes);
                                    ale = new ActivityLogEntry(activityBytes);

                                    // If found a signature index that is less-equal to correct one, then stop
                                    findingCorrectActivityLogEntry = ale.getSignatureIndex() > largestIntactSignatureIndex;

                                    // Update so if loops again, next next newest entry
                                    if (findingCorrectActivityLogEntry) {
                                        startingOffset -= ActivityLogEntry.SIZE;
                                        desiredEntries--;
                                    }
                                }

                                // Note: if get here, any corruption was fixed, so won't have to check again.
                                alreadyCheckedActivityLog = true;

                                // Here's where the log file should actually end
                                long desiredActivityFileLen = desiredEntries * ActivityLogEntry.SIZE;
                                if (desiredActivityFileLen == activityLogLen) {
                                    // Nothing to do -- no changes need to be made. This should happen often, since a signature was 
                                    // lost, and almost certainly one activity log entry needs to be tossed
                                    System.err.println("    ... No changes need to be made to activity log to accomodate loss of signature.");
                                } else if (desiredActivityFileLen < activityLogLen) {
                                    System.err.println("    ... Updating activity log from " + intactEntries + " entries to " + desiredEntries + " due to lost signature.");
                                } else {
                                    throw new AssertionFailedException("Should not happen, but: desiredActivityFileLen{" + desiredActivityFileLen + "} > activityLogLen{" + activityLogLen + "}");
                                }

                                this.activitiesLogRAF.setLength(desiredActivityFileLen);
                            }

                        } catch (Exception nope) { /* Nothing to do, cannot recover */ }

                        if (!isRecoverable) {
                            // Corrupted on first entry. Simply wipe out all files since nothing else to do
                            this.signaturesIndexRAF.setLength(0);
                            this.signaturesRAF.setLength(0);
                            this.activitiesLogRAF.setLength(0);
                            System.err.println("    ... No intact signature indices found. Cleared all logs.");
                        }
                    } else {
                        // Make sure the previous entry is readable
                        final byte[] lastIntactSignatureEntry = new byte[SignatureIndexEntry.SIZE];
                        long lastIntactSignatureEntrySeek = signaturesIndexLen - excessSignatureIndexBytes - SignatureIndexEntry.SIZE;
                        this.signaturesIndexRAF.seek(lastIntactSignatureEntrySeek);
                        this.signaturesIndexRAF.readFully(lastIntactSignatureEntry);
                        SignatureIndexEntry e = new SignatureIndexEntry(lastIntactSignatureEntry);
                        largestIntactSignatureIndex = e.getIndex();

                        // If get's here, let's see whether can recover last entry. We're assuming that the 
                        // last entry was somehow corrupted in the signatures index file.
                        long lastSignatureOffset = e.getOffset() + e.getLength();
                        int lastSignatureLen = (int) (signaturesLen - lastSignatureOffset);

                        // If we find that there are signature bytes after the last know signature, then the signature
                        // header _might_ be recoverable. We'll try loading them into memory and recalculating
                        boolean recoverable = lastSignatureLen > 0;

                        // If POTENTIALLY recoverable...
                        if (recoverable) {
                            System.err.println("    ... might be recoverable.");

                            try {
                                byte[] signatureBytes = new byte[lastSignatureLen];
                                this.signaturesRAF.seek(lastSignatureOffset);
                                this.signaturesRAF.read(signatureBytes);

                                // If this throws an exception, then cannot recover
                                new Signature(signatureBytes);

                                SignatureIndexEntry sie = new SignatureIndexEntry(e.getIndex() + 1, lastSignatureOffset, lastSignatureLen);

                                System.err.println("    ... information recovered, writing to end of file (index #" + String.valueOf(e.getIndex() + 1) + ")");

                                this.signaturesIndexRAF.seek(lastIntactSignatureEntrySeek + SignatureIndexEntry.SIZE);
                                byte[] sieBytes = sie.toByteArray();
                                this.signaturesIndexRAF.write(sieBytes);

                                largestIntactSignatureIndex = sie.getIndex();
                            } catch (Exception nope) {
                                // Not recoverable
                                recoverable = false;
                            }
                        }


                        if (!recoverable) {
                            System.err.println("    ... not recoverable. Going to snip last " + excessSignatureIndexBytes + " bytes from signature index (header) file.");

                            // Truncate after last intact entry
                            long newSignatureIndexFileLength = lastIntactSignatureEntrySeek + SignatureIndexEntry.SIZE;
                            this.signaturesIndexRAF.setLength(newSignatureIndexFileLength);

                            // Find the activity log entry that 
                            if (largestIntactSignatureIndex == -1) {
                                throw new AssertionFailedException("largestIntactSignatureIndex should have been set.");
                            }

                            // Go backwards in activity log until find the correct entry. Before starting,
                            // make sure there's at least one entry or don't bother.
                            //
                            // (Note that the activity log might be corrupted. This might fix, but this
                            //  will get checked later.)
                            if (activityLogLen / ActivityLogEntry.SIZE >= 1) {

                                final long intactEntries = (long) Math.floor((double) activityLogLen / (double) ActivityLogEntry.SIZE);
                                long startingOffset = (intactEntries - 1) * ActivityLogEntry.SIZE;

                                boolean findingCorrectActivityLogEntry = true;
                                ActivityLogEntry ale = null;

                                long desiredEntries = intactEntries;

                                while (findingCorrectActivityLogEntry) {

                                    byte[] activityBytes = new byte[ActivityLogEntry.SIZE];
                                    this.activitiesLogRAF.seek(startingOffset);
                                    this.activitiesLogRAF.readFully(activityBytes);
                                    ale = new ActivityLogEntry(activityBytes);

                                    // If found a signature index that is less-equal to correct one, then stop
                                    findingCorrectActivityLogEntry = ale.getSignatureIndex() > largestIntactSignatureIndex;

                                    // Update so if loops again, next next newest entry
                                    if (findingCorrectActivityLogEntry) {
                                        startingOffset -= ActivityLogEntry.SIZE;
                                        desiredEntries--;
                                    }
                                }

                                // Note: if get here, any corruption was fixed, so won't have to check again.
                                alreadyCheckedActivityLog = true;

                                // Here's where the log file should actually end
                                long desiredActivityFileLen = desiredEntries * ActivityLogEntry.SIZE;
                                if (desiredActivityFileLen == activityLogLen) {
                                    // Nothing to do -- no changes need to be made. This should happen often, since a signature was 
                                    // lost, and almost certainly one activity log entry needs to be tossed
                                    System.err.println("    ... No changes need to be made to activity log to accomodate loss of signature.");
                                } else if (desiredActivityFileLen < activityLogLen) {
                                    System.err.println("    ... Updating activity log from " + intactEntries + " entries to " + desiredEntries + " due to lost signature.");
                                } else {
                                    throw new AssertionFailedException("Should not happen, but: desiredActivityFileLen{" + desiredActivityFileLen + "} > activityLogLen{" + activityLogLen + "}");
                                }

                                this.activitiesLogRAF.setLength(desiredActivityFileLen);
                            }
                        }
                    } // If more than one entry and need to snip
                } // Step 1a
                // Step 1b: Make sure signature bytes are there
                {
                    // Only bother if file has data
                    long updatedSignaturesIndexFileLen = this.signaturesIndexRAF.length();
                    if (updatedSignaturesIndexFileLen > 0) {
                        final byte[] lastSignatureEntry = new byte[SignatureIndexEntry.SIZE];
                        long lastSignatureEntrySeek = signaturesIndexRAF.length() - SignatureIndexEntry.SIZE;
                        this.signaturesIndexRAF.seek(lastSignatureEntrySeek);
                        this.signaturesIndexRAF.readFully(lastSignatureEntry);
                        SignatureIndexEntry e = new SignatureIndexEntry(lastSignatureEntry);

                        long expectedLength = e.getOffset() + e.getLength();
                        signaturesLen = this.signaturesRAF.length();

                        // There's only a problem if less bytes than expect. If more, means old abandoned signature
                        // bytes are there, but they'll be ignored.
                        boolean isProblem = (expectedLength) > (signaturesLen);
                        if (isProblem) {
                            System.err.println("---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
                            System.err.println("Detected corruption in signature file<" + this.getSignaturesFile().getAbsolutePath() + ">, expected " + String.valueOf(expectedLength) + " but found " + signaturesLen + ". Going to try to fix.");
                            System.err.println("---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

                            // Backup logs (if not already backed up)
                            backupFiles();

                            // If there was only one entry, just clear out
                            if (lastSignatureEntrySeek == 0) {
                                this.activitiesLogRAF.setLength(0);
                                this.signaturesIndexRAF.setLength(0);
                                this.signaturesRAF.setLength(0);
                                System.err.println("    ... No intact signatures found. Cleared all logs.");
                            } else {

                                // Need to find the first entry that is not corrupted
                                boolean foundFirstIntactEntry = false;

                                // Already checked the last entry. Let's check next-to-last
                                int nextEntryToCheck = (int) ((double) updatedSignaturesIndexFileLen / (double) SignatureIndexEntry.SIZE) - 2;
                                int entriesToSnip = 1;
                                long lengthToSnip = -1;

                                boolean noSignaturesFound = false;

                                FIND_FIRST_INTACT_ENTRY:
                                while (!foundFirstIntactEntry) {
                                    long nextSeek = nextEntryToCheck * SignatureIndexEntry.SIZE;

                                    if (nextSeek < 0) {
                                        noSignaturesFound = true;
                                        break FIND_FIRST_INTACT_ENTRY;
                                    }

                                    byte[] nextBytes = new byte[SignatureIndexEntry.SIZE];
                                    this.signaturesIndexRAF.seek(nextSeek);
                                    this.signaturesIndexRAF.readFully(nextBytes);
                                    e = new SignatureIndexEntry(nextBytes);
                                    lengthToSnip = e.getOffset() + e.getLength();

                                    foundFirstIntactEntry = (lengthToSnip) <= (signaturesLen);

                                    // If not found, back up
                                    if (!foundFirstIntactEntry) {
                                        nextEntryToCheck--;
                                        entriesToSnip++;
                                    }
                                }

                                if (noSignaturesFound) {
                                    this.activitiesLogRAF.setLength(0);
                                    this.signaturesIndexRAF.setLength(0);
                                    this.signaturesRAF.setLength(0);
                                    System.err.println("    ... No intact signatures were found. Nothing salvagable, cleared all logs.");
                                } else {
                                    System.err.println("    ... going to snip signature file at " + lengthToSnip + " (Total entries lost: " + entriesToSnip + ")");
                                    this.signaturesRAF.setLength(lengthToSnip);

                                    // Now snip the index file to match
                                    long signatureIndexFileSnip = this.getSignaturesIndexFile().length() - entriesToSnip * SignatureIndexEntry.SIZE;
                                    System.err.println("    ... snipping signature index file at " + signatureIndexFileSnip + " to match the " + entriesToSnip + " lost entr(ies)");
                                    this.signaturesIndexRAF.setLength(signatureIndexFileSnip);

                                    // Get the last intact signature entry. Going to use to snip activity log.
                                    final byte[] lastIntactSignatureEntry = new byte[SignatureIndexEntry.SIZE];
                                    long lastIntactSignatureEntrySeek = signatureIndexFileSnip - SignatureIndexEntry.SIZE;
                                    this.signaturesIndexRAF.seek(lastIntactSignatureEntrySeek);
                                    this.signaturesIndexRAF.readFully(lastIntactSignatureEntry);
                                    e = new SignatureIndexEntry(lastIntactSignatureEntry);
                                    largestIntactSignatureIndex = e.getIndex();

                                    // Go backwards in activity log until find the correct entry. Before starting,
                                    // make sure there's at least one entry or don't bother.
                                    //
                                    // (Note that the activity log might be corrupted. This might fix, but this
                                    //  will get checked later.)
                                    if (activityLogLen / ActivityLogEntry.SIZE >= 1) {

                                        final long intactEntries = (long) Math.floor((double) activityLogLen / (double) ActivityLogEntry.SIZE);
                                        long startingOffset = (intactEntries - 1) * ActivityLogEntry.SIZE;

                                        boolean findingCorrectActivityLogEntry = true;
                                        ActivityLogEntry ale = null;

                                        long desiredEntries = intactEntries;

                                        while (findingCorrectActivityLogEntry) {

                                            byte[] activityBytes = new byte[ActivityLogEntry.SIZE];
                                            this.activitiesLogRAF.seek(startingOffset);
                                            this.activitiesLogRAF.readFully(activityBytes);
                                            ale = new ActivityLogEntry(activityBytes);

                                            // If found a signature index that is less-equal to correct one, then stop
                                            findingCorrectActivityLogEntry = ale.getSignatureIndex() > largestIntactSignatureIndex;

                                            // Update so if loops again, next next newest entry
                                            if (findingCorrectActivityLogEntry) {
                                                startingOffset -= ActivityLogEntry.SIZE;
                                                desiredEntries--;
                                            }
                                        }

                                        // Note: if get here, any corruption was fixed, so won't have to check again.
                                        alreadyCheckedActivityLog = true;

                                        // Here's where the log file should actually end
                                        long desiredActivityFileLen = desiredEntries * ActivityLogEntry.SIZE;
                                        if (desiredActivityFileLen == activityLogLen) {
                                            // Nothing to do -- no changes need to be made. This should happen often, since a signature was 
                                            // lost, and almost certainly one activity log entry needs to be tossed
                                            System.err.println("    ... No changes need to be made to activity log to accomodate loss of signature.");
                                        } else if (desiredActivityFileLen < activityLogLen) {
                                            System.err.println("    ... Updating activity log from " + intactEntries + " entries to " + desiredEntries + " due to lost signature.");
                                        } else {
                                            throw new AssertionFailedException("Should not happen, but: desiredActivityFileLen{" + desiredActivityFileLen + "} > activityLogLen{" + activityLogLen + "}");
                                        }

                                        this.activitiesLogRAF.setLength(desiredActivityFileLen);
                                    }
                                }
                            } // More than one entry
                        }
                    }
                } // Step 1b
            }// Step 1
            /**
             * Step 2: Check the activity log for corruption
             */
            if (!alreadyCheckedActivityLog) {
                long excessActivityLogBytes = this.getActivitiesLogFile().length() % ActivityLogEntry.SIZE;
                if (excessActivityLogBytes != 0) {

                    if (excessActivityLogBytes >= ActivityLogEntry.SIZE) {
                        throw new AssertionFailedException("excessActivityLogBytes{" + excessActivityLogBytes + "} >= ActivityLogEntry.SIZE {" + ActivityLogEntry.SIZE + "}");
                    }

                    System.err.println("---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
                    System.err.println("Detected corruption in signature index file<" + this.getActivitiesLogFile().getAbsolutePath() + ">. File of size " + this.activitiesLogRAF.length() + " ends with " + excessActivityLogBytes + " excess bytes. Trimming file...");
                    System.err.println("---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

                    // Backup logs (if not already backed up)
                    backupFiles();

                    long snipPoint = this.activitiesLogRAF.length() - excessActivityLogBytes;
                    this.activitiesLogRAF.setLength(snipPoint);
                    System.err.println("    ... snipped to " + this.getActivityLogEntriesCount() + " entries <Size: " + TextUtil.formatBytes(snipPoint) + ">");
                }
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + " occurred while checking for corruption: " + e.getMessage());
            System.err.println("    Check standard error for more clues as to what happened. One or more logs might be corrupted and need attention.");
            e.printStackTrace(System.err);
            throw e;
        }
    }

    /**
     * <p>Open up all associated random access files.</p>
     * @throws java.io.IOException
     */
    private void openRAF() throws IOException {
        this.activitiesLogRAF = new RandomAccessFile(getActivitiesLogFile(), "rw");
        this.signaturesIndexRAF = new RandomAccessFile(getSignaturesIndexFile(), "rw");
        this.signaturesRAF = new RandomAccessFile(getSignaturesFile(), "rw");
    }

    /**
     * <p>Close down all associated random access files.</p>
     * <p>Note that you can re-open these files again by calling openRAF(). However, this will instantiate new random access files, and all monitors will synchronize on new objects. The end result is that the methods will temporarily not be thread safe.</p>
     * <p>Take home point: In general, after the ActivityLog is instantiated, better to not close the RAF unless staying closed.</p>
     * @throws java.io.IOException
     */
    private void closeRAF() throws IOException {
        IOUtil.safeClose(activitiesLogRAF);
        IOUtil.safeClose(signaturesIndexRAF);
        IOUtil.safeClose(signaturesRAF);
    }

    /**
     * <p>Backups up files, clobbering any existing backups.</p>
     * <p>Though this is synchronized, calling this will make other synchronized methods no longer thread safe. Only use this to backup from the constructor or a method called by constructor.</p>
     * @throws java.io.IOException
     */
    private synchronized void backupFiles() throws IOException {
        if (hasBeenBackedUp) {
            return;
        }
        hasBeenBackedUp = true;

        try {
            // Close random access files so not locked.
            closeRAF();

            this.getBackupActivitiesLogFile().createNewFile();
            this.getBackupSignaturesFile().createNewFile();
            this.getBackupSignaturesIndexFile().createNewFile();

            IOUtil.copyFile(this.getActivitiesLogFile(), this.getBackupActivitiesLogFile());
            IOUtil.copyFile(this.getSignaturesFile(), this.getBackupSignaturesFile());
            IOUtil.copyFile(this.getSignaturesIndexFile(), this.getBackupSignaturesIndexFile());

            System.err.println("********************************************************************************************************************************************************");
            System.err.println("** Backed up log files. You will want to save these somewhere because they will be overwritten next time the logs are repaired or a backup in made:");
            System.err.println("********************************************************************************************************************************************************");
            System.err.println("*   Activity log<" + this.getActivitiesLogFile().getAbsolutePath() + "> --> " + this.getBackupActivitiesLogFile().getAbsolutePath());
            System.err.println("*   Signature index file<" + this.getSignaturesIndexFile().getAbsolutePath() + "> --> " + this.getBackupSignaturesIndexFile().getAbsolutePath());
            System.err.println("*   Signature file<" + this.getSignaturesFile().getAbsolutePath() + "> --> " + this.getBackupSignaturesFile().getAbsolutePath());
            System.err.println("********************************************************************************************************************************************************");

        } finally {
            // Open up random access files again
            openRAF();
        }
    }

    /**
     * <p>Writes activity to various files.</p>
     * <p>Thread safe.</p>
     * @param activity
     * @throws java.io.IOException
     */
    public void write(Activity activity) throws IOException, CertificateEncodingException {

        final long start = TimeUtil.getTrancheTimestamp();

        try {
            long signatureOffset = this.signaturesRAF.length();
            byte[] signatureBytes = activity.getSignature().toByteArray();
            int signatureIndex = -1;

            // Synchronized on write lock so thread safe (i.e., entries in activity log always in chronological order)
            synchronized (writeLock) {

                // Synchronized on signatures RAF since need it's info to write to signatures index file
                synchronized (this.signaturesRAF) {
                    /**
                     * STEP 1: GET ENOUGH INFORMATION TO WRITE THE SIGNATURE INDEX FILE ENTRY AND WRITE IT
                     */
                    synchronized (this.signaturesIndexRAF) {

                        boolean isFirst = this.signaturesIndexRAF.length() == 0;
                        if (!isFirst) {
                            // Seek to last entry and read
                            this.signaturesIndexRAF.seek(this.signaturesIndexRAF.length() - SignatureIndexEntry.SIZE);
                            byte[] prevEntryBytes = new byte[SignatureIndexEntry.SIZE];
                            this.signaturesIndexRAF.readFully(prevEntryBytes);

                            // Build the entry object and grab its index. Increment to get next index
                            SignatureIndexEntry prevEntry = new SignatureIndexEntry(prevEntryBytes);
                            signatureIndex = prevEntry.getIndex() + 1;
                        } else {
                            // Zero-index
                            signatureIndex = 0;
                        }

                        // Build the bytes
                        SignatureIndexEntry entry = new SignatureIndexEntry(signatureIndex, signatureOffset, signatureBytes.length);
                        try {
                            byte[] entryBytes = entry.toByteArray();
                            // Write the entry at the end of the file
                            this.signaturesIndexRAF.seek(this.signaturesIndexRAF.length());
                            this.signaturesIndexRAF.write(entryBytes);
                        } catch (Exception e) {
                            debugErr(e);
                        }
                    }

                    /**
                     * STEP 2: WRITE SIGNATURE TO SIGNATURES FILE
                     */
                    {
                        this.signaturesRAF.seek(signatureOffset);
                        this.signaturesRAF.write(signatureBytes);
                    }
                }

                synchronized (this.activitiesLogRAF) {
                    /**
                     * STEP 3: WRITE THE ACTIVITY TO THE ACTIVITY LOG
                     */
                    final long timestamp = TimeUtil.getTrancheTimestamp();

                    // Caller might appreciate we set the correct timestamp
                    activity.setTimestamp(timestamp);

                    try {
                        ActivityLogEntry entry = new ActivityLogEntry(timestamp, activity.getAction(), signatureIndex, activity.getHash());
                        byte[] entryBytes = entry.toByteArray();
                        this.activitiesLogRAF.seek(this.activitiesLogRAF.length());
                        this.activitiesLogRAF.write(entryBytes);
                    } catch (Exception e) {
                        debugErr(e);
                    }
                }
            }
        } finally {
            long total = TimeUtil.getTrancheTimestamp() - start;
            synchronized (countLock) {
                timeWriting += total;
                writeCount++;
            }
        }
    }

    /**
     * <p>Performs search to find whether contains an entry representing parameters.</p>
     * @param timestamp
     * @param action
     * @param hash
     * @return True if found; false otherwise
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    public boolean contains(long timestamp, byte action, BigHash hash) throws IOException, Exception {
        final long start = TimeUtil.getTrancheTimestamp();
        try {
            return containsBinary(timestamp, action, hash);
        } finally {
            long total = TimeUtil.getTrancheTimestamp() - start;
            synchronized (countLock) {
                timeReading += total;
                readCount++;
            }
        }
    }

    /**
     * <p>Performs O(log n) binary search to find whether contains an entry representing parameters.</p>
     * @param timestamp
     * @param action
     * @param hash
     * @return
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    private boolean containsBinary(long timestamp, byte action, BigHash hash) throws IOException, Exception {
        synchronized (this.activitiesLogRAF) {

            final long totalLen = this.activitiesLogRAF.length();
            final int totalEntries = (int) ((double) totalLen / (double) ActivityLogEntry.SIZE);

            int checkIndex = (int) ((double) totalEntries / 2.0);
            int checkOffset = checkIndex * ActivityLogEntry.SIZE;

            int lower = 0;
            int higher = totalEntries - 1;

            while (lower <= higher) {
                byte[] entryBytes = new byte[ActivityLogEntry.SIZE];
                this.activitiesLogRAF.seek(checkOffset);
                this.activitiesLogRAF.readFully(entryBytes);
                ActivityLogEntry entry = new ActivityLogEntry(entryBytes);

                if (entry.getTimestamp() == timestamp) {
                    // Found it
                    if (entry.getAction() == action && entry.getHash().equals(hash)) {
                        return true;
                    }

                    // This one isn't it. Need to work back and forward and get everything
                    // with same timestamp
                    int testIndex = checkIndex - 1;
                    int testOffset = testIndex * ActivityLogEntry.SIZE;

                    // First, go back
                    while (true) {

                        // Cannot go back any further; at beginning of file. Not found in this direction.
                        if (testOffset < 0) {
                            break;
                        }

                        entryBytes = new byte[ActivityLogEntry.SIZE];

                        this.activitiesLogRAF.seek(testOffset);
                        this.activitiesLogRAF.readFully(entryBytes);

                        entry = new ActivityLogEntry(entryBytes);

                        // Found first activity before this that is not same timestamp
                        if (entry.getTimestamp() != timestamp) {
                            break;
                        }

                        if (entry.getAction() == action && entry.getHash().equals(hash)) {
                            return true;
                        }

                        // Move back one
                        testIndex--;
                        testOffset = testIndex * ActivityLogEntry.SIZE;
                    }

                    testIndex = checkIndex + 1;
                    testOffset = testIndex * ActivityLogEntry.SIZE;

                    // Next, go forward
                    while (true) {

                        // If cannot read in that many bytes, at end of file. Break, not found in this direction.
                        if (testOffset + ActivityLogEntry.SIZE > this.activitiesLogRAF.length()) {
                            break;
                        }

                        entryBytes = new byte[ActivityLogEntry.SIZE];
                        this.activitiesLogRAF.seek(testOffset);
                        this.activitiesLogRAF.readFully(entryBytes);
                        entry = new ActivityLogEntry(entryBytes);

                        // Found first activity before this that is not same timestamp
                        if (entry.getTimestamp() != timestamp) {
                            break;
                        }

                        if (entry.getAction() == action && entry.getHash().equals(hash)) {
                            return true;
                        }

                        // Move forward one
                        testIndex++;
                        testOffset = testIndex * ActivityLogEntry.SIZE;
                    }

                    // Checked everything with same timestamp, not there.
                    return false;
                } else if (entry.getTimestamp() > timestamp) {
                    higher = checkIndex - 1;
                } else if (entry.getTimestamp() < timestamp) {
                    lower = checkIndex + 1;
                }

                // Update index and offset
                checkIndex = (int) ((double) (lower + higher) / 2.0);
                checkOffset = checkIndex * ActivityLogEntry.SIZE;
            }

            return false;
        }
    }

    /**
     * <p>Performs O(n) linear search to find whether contains an entry representing parameters.</p>
     * @param timestamp
     * @param action
     * @param hash
     * @return
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    private boolean containsLinear(long timestamp, byte action, BigHash hash) throws IOException, Exception {
        synchronized (this.activitiesLogRAF) {
            long seek = 0;
            final long totalLen = this.activitiesLogRAF.length();

            // Read until not enough bytes left for total entry. (This indicates corruption and should have been checked)
            FIND_STARTING_INDEX:
            while (seek + ActivityLogEntry.SIZE <= totalLen) {

                byte[] entryBytes = new byte[ActivityLogEntry.SIZE];
                this.activitiesLogRAF.seek(seek);
                this.activitiesLogRAF.readFully(entryBytes);
                ActivityLogEntry entry = new ActivityLogEntry(entryBytes);

                if (entry.getTimestamp() == timestamp && entry.getAction() == action && entry.getHash().equals(hash)) {
                    return true;
                }

                // Linear speed-up (remove from log(n) lookup)
                if (entry.getTimestamp() > timestamp) {
                    return false;
                }

                // Increment seek to next entry
                seek += ActivityLogEntry.SIZE;
            }

            // Not found
            return false;
        }
    }

    /**
     * <p>Returns all activities between timestamps up until a specified limit that match the supplied mask.</p>
     * @param fromTimestamp Starting timestamp (inclusive)
     * @param toTimestamp Ending timestamp (inclusive)
     * @param limit Limit to number of entries to return. Should use Integer.MAX_VALUE if want all entries
     * @param mask Used to match activity bytes. If all the bits in the mask are included in the activity byte, then the activity will be included. For example, if the mask is Activity.ACTION_TYPE_SET, then all logs that have this bit set (set data and set meta data) will be included. However, if Activity.SET_DATA is the mask, then only set data actions will match (since there are two bits set in the mask, making it more restrictive). 0 is least restrictive (restricts nothing) and 0xFF is most restrictive (restricts everything but itself).
     * @return
     * @throws java.lang.Exception
     */
    public List<Activity> read(long fromTimestamp, long toTimestamp, int limit, byte mask) throws Exception {
        if (fromTimestamp > toTimestamp) {
            throw new RuntimeException("Illegal parameters: starting timestamp<" + fromTimestamp + "> is greater than ending timestamp<" + toTimestamp + ">.");
        }

        final long start = TimeUtil.getTrancheTimestamp();
        try {
            List<Activity> activities = new LinkedList();

            // Step 1: find where timestamp starts
            synchronized (this.activitiesLogRAF) {
                // Quickly find. Should be log(n) lookup.
                long indexToStart = getIndexToStart(fromTimestamp);

                // Couldn't find
                if (indexToStart == -1) {
                    return activities;
                }

                long seek = indexToStart * ActivityLogEntry.SIZE;
                final long totalLen = this.activitiesLogRAF.length();

                // Read until one of three conditions met
                // 1. Pass the toTimestamp date
                // 2. Exceed the limit (while statement)
                // 3. End of file (while statement)
                READING_ACTIVITIES:
                while (activities.size() < limit && seek + ActivityLogEntry.SIZE <= totalLen) {

                    this.activitiesLogRAF.seek(seek);

                    byte[] entryBytes = new byte[ActivityLogEntry.SIZE];
                    this.activitiesLogRAF.readFully(entryBytes);
                    ActivityLogEntry entry = new ActivityLogEntry(entryBytes);

                    // If passed the ending timestamp, break
                    if (entry.getTimestamp() > toTimestamp) {
                        break READING_ACTIVITIES;
                    }

                    Signature sig = this.signatureMap.getSignature(entry.getSignatureIndex());
                    Activity nextActivity = new Activity(entry.getTimestamp(), entry.getAction(), sig, entry.getHash());

                    // Only add if passes the mask. Note a 0 is least restrictive--it matches
                    // everything. 0xFF would only match if all bits in byte were set, which 
                    // won't happen in production, so it would match nothing.
                    boolean passesMask = mask == Activity.ANY || (nextActivity.getAction() & mask) == mask;
                    if (passesMask) {
                        activities.add(nextActivity);
                    }

                    // Increment seek to next entry
                    seek += ActivityLogEntry.SIZE;
                }
            }

            return activities;
        } finally {
            long total = TimeUtil.getTrancheTimestamp() - start;
            synchronized (countLock) {
                timeReading += total;
                readCount++;
            }
        }
    }

    /**
     * <p>Returns count of all activities between timestamps that match mask.</p>
     * @param fromTimestamp Starting timestamp (inclusive)
     * @param toTimestamp Ending timestamp (inclusive)
     * @param mask Used to match activity bytes. If all the bits in the mask are included in the activity byte, then the activity will be included. For example, if the mask is Activity.ACTION_TYPE_SET, then all logs that have this bit set (set data and set meta data) will be included. However, if Activity.SET_DATA is the mask, then only set data actions will match (since there are two bits set in the mask, making it more restrictive). 0 is least restrictive (restricts nothing) and 0xFF is most restrictive (restricts everything but itself).
     * @return
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    public int getActivityCount(long fromTimestamp, long toTimestamp, byte mask) throws IOException, Exception {
        if (fromTimestamp > toTimestamp) {
            throw new RuntimeException("Illegal parameters: starting timestamp<" + fromTimestamp + "> is greater than ending timestamp<" + toTimestamp + ">.");
        }

        int count = 0;
        try {

            // Step 1: find where timestamp starts
            synchronized (this.activitiesLogRAF) {
                // Quickly find. Should be log(n) lookup.
                long indexToStart = getIndexToStart(fromTimestamp);

                // Couldn't find
                if (indexToStart == -1) {
                    return count;
                }

                long seek = indexToStart * ActivityLogEntry.SIZE;
                final long totalLen = this.activitiesLogRAF.length();

                // Read until one of conditions met
                // 1. Pass the toTimestamp date
                // 3. End of file (while statement)
                READING_ACTIVITIES:
                while (seek + ActivityLogEntry.SIZE <= totalLen) {

                    this.activitiesLogRAF.seek(seek);

                    byte[] entryBytes = new byte[ActivityLogEntry.SIZE];
                    this.activitiesLogRAF.readFully(entryBytes);
                    ActivityLogEntry entry = new ActivityLogEntry(entryBytes);

                    // If passed the ending timestamp, break
                    if (entry.getTimestamp() > toTimestamp) {
                        break READING_ACTIVITIES;
                    }

                    // If gets here, entry was in range. Increment count.
                    boolean matchesMask = mask == Activity.ANY || (entry.getAction() & mask) == mask;
                    if (matchesMask) {
                        count++;                    // Increment seek to next entry
                    }
                    seek += ActivityLogEntry.SIZE;
                }
            }

            return count;
        } finally {
        }
    }

    /**
     * <p>Logarithmic lookup to find first index that starts at or after timestamp.</p>
     * @param timestamp
     * @return Index where should start, otherwise -1
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    private int getIndexToStart(long startingTimestamp) throws IOException, Exception {

        // If negative (or zero) timestamp, just start at index of 1. This ensures that
        // if tryign to get all activity entries, can use Long.MIN_VALUE
        if (startingTimestamp <= 0) {
            return 0;
        }

        synchronized (this.activitiesLogRAF) {

            final long totalLen = this.activitiesLogRAF.length();
            final int totalEntries = (int) ((double) totalLen / (double) ActivityLogEntry.SIZE);

            int checkIndex = (int) ((double) totalEntries / 2.0);
            int checkOffset = checkIndex * ActivityLogEntry.SIZE;

            int lower = 0;
            int higher = totalEntries - 1;

            while (lower <= higher) {

                byte[] entryBytes = new byte[ActivityLogEntry.SIZE];
                this.activitiesLogRAF.seek(checkOffset);
                this.activitiesLogRAF.readFully(entryBytes);
                ActivityLogEntry entry = new ActivityLogEntry(entryBytes);

                /**
                 * We matched exactly! But there might be other activities matching same timestamp, so look back
                 */
                if (entry.getTimestamp() == startingTimestamp) {

                    // Special case: if first index, this must be it!
                    if (checkIndex == 0) {
                        return checkIndex;
                    }

                    // This might not be the first timestamp that's equal. Work back until find timestamp that isn't equal
                    checkIndex--;
                    checkOffset = checkIndex * ActivityLogEntry.SIZE;

                    // First, go back
                    while (true) {
                        entryBytes = new byte[ActivityLogEntry.SIZE];
                        this.activitiesLogRAF.seek(checkOffset);
                        this.activitiesLogRAF.readFully(entryBytes);
                        entry = new ActivityLogEntry(entryBytes);

                        // Found first activity before this that is not same timestamp
                        if (entry.getTimestamp() != startingTimestamp) {
                            return checkIndex + 1;
                        }

                        // Special case: if first index, this must be it!
                        if (checkIndex == 0) {
                            return checkIndex;
                        }

                        // Move back one
                        checkIndex--;
                        checkOffset = checkIndex * ActivityLogEntry.SIZE;
                    }
                } //
                /**
                 * The timestamp we found is less than starting. However, if the next is greater, we know
                 * where to start!
                 */
                else if (entry.getTimestamp() < startingTimestamp) {

                    // Special case: if this is the last entry, then not found
                    if (checkIndex == totalEntries - 1) {
                        return -1;
                    }

                    // Check to see whether next is greater. If so, we're set!
                    checkOffset = (checkIndex + 1) * ActivityLogEntry.SIZE;
                    entryBytes = new byte[ActivityLogEntry.SIZE];
                    this.activitiesLogRAF.seek(checkOffset);
                    this.activitiesLogRAF.readFully(entryBytes);
                    entry = new ActivityLogEntry(entryBytes);

                    if (entry.getTimestamp() >= startingTimestamp) {
                        return checkIndex + 1;
                    } else {
                        lower = checkIndex + 1;
                    }
                } //
                /**
                 * The timestamp is greater than starting, so might be an index that with smaller timestamp that
                 * is still after our starting timestamp. Only way to find out is to go backwards.
                 * 
                 * However, if the entry before this is earlier than starting timestamp, we know what to do!
                 */
                else if (entry.getTimestamp() > startingTimestamp) {

                    // Special case: if this is the first entry, then found!
                    if (checkIndex == 0) {
                        return 0;
                    }

                    // Check to see whether previos is less. If so, we're set!
                    checkOffset = (checkIndex - 1) * ActivityLogEntry.SIZE;
                    entryBytes = new byte[ActivityLogEntry.SIZE];
                    this.activitiesLogRAF.seek(checkOffset);
                    this.activitiesLogRAF.readFully(entryBytes);
                    entry = new ActivityLogEntry(entryBytes);

                    if (entry.getTimestamp() < startingTimestamp) {
                        return checkIndex;
                    } else if (entry.getTimestamp() == startingTimestamp) {
                        return checkIndex - 1;
                    } else {
                        higher = checkIndex - 1;
                    }
                }

                // Update index and offset
                checkIndex = (int) ((double) (lower + higher) / 2.0);
                checkOffset = checkIndex * ActivityLogEntry.SIZE;
            }

            return -1;
        }
    }

    /**
     * <p>Average time  per entry(in milliseconds) spent writing out data.</p>
     * @return
     */
    public long getAvgWriteTimeInMillis() {
        if (writeCount == 0) {
            return 0;
        }
        return (long) ((double) timeWriting / (double) writeCount);
    }

    /**
     * <p>Average time per entry (in milliseconds) spent reading in data.</p>
     * @return
     */
    public long getAvgReadTimeInMillis() {
        if (readCount == 0) {
            return 0;
        }
        return (long) ((double) timeReading / (double) readCount);
    }

    /**
     * <p>Close down the activity log. Once closed, cannot reopen.</p>
     * <p>Thread safe.</p>
     * @throws java.io.IOException
     */
    public synchronized void close() throws IOException {
        if (isClosed) {
            return;
        }
        try {
            closeRAF();
        } finally {
            isClosed = true;
        }
    }

    public File getActivitiesLogFile() {
        return activitiesLogFile;
    }

    public File getSignaturesIndexFile() {
        return signaturesIndexFile;
    }

    public File getSignaturesFile() {
        return signaturesFile;
    }

    public File getBackupActivitiesLogFile() {
        return backupActivitiesLogFile;
    }

    public File getBackupSignaturesIndexFile() {
        return backupSignaturesIndexFile;
    }

    public File getBackupSignaturesFile() {
        return backupSignaturesFile;
    }

    /**
     * <p>Reads in signatures.</p>
     */
    private class SignatureMap {

        private final RandomAccessFile signaturesIndexRAF;
        private final RandomAccessFile signaturesRAF;

        private SignatureMap(RandomAccessFile signaturesIndexRAF, RandomAccessFile signaturesRAF) {
            this.signaturesIndexRAF = signaturesIndexRAF;
            this.signaturesRAF = signaturesRAF;
        }

        /**
         * <p>Returns Signature object based on memory. If not found, returns null.</p>
         * <p>Attempts to get from cache. If not there, gets from disk and adds to cache.</p>
         * @param signatureIndex
         * @return
         * @throws java.io.IOException
         * @throws java.lang.Exception
         */
        public Signature getSignature(int signatureIndex) throws IOException, Exception {

            Signature signature = null;

            /**
             *  Find the signature index entry. This is constant time lookup, log(k), since can calculate location!
             */
            SignatureIndexEntry entry = null;
            synchronized (this.signaturesIndexRAF) {

                long offset = signatureIndex * SignatureIndexEntry.SIZE;
                final long totalLen = this.signaturesIndexRAF.length();

                // Doesn't exist
                if (offset + SignatureIndexEntry.SIZE > totalLen) {
                    return null;
                }

                byte[] entryBytes = new byte[SignatureIndexEntry.SIZE];
                try {
                    this.signaturesIndexRAF.seek(offset);
                } catch (IOException ioe) {
                    debugOut(ioe.getClass().getSimpleName() + " occurred at seak " + offset + " for signature index " + signatureIndex + ": " + ioe.getMessage());
                    throw ioe;
                }
                this.signaturesIndexRAF.readFully(entryBytes);
                entry = new SignatureIndexEntry(entryBytes);
            }

            /**
             * Find the signature bytes and build the signature. This is also constant time O(k) since know
             * exactly where it is.
             */
            synchronized (this.signaturesRAF) {
                byte[] signatureBytes = new byte[entry.getLength()];
                this.signaturesRAF.seek(entry.getOffset());
                this.signaturesRAF.readFully(signatureBytes);
                signature = new Signature(signatureBytes);
            }
            return signature;
        }
    }

    public long getActivityLogEntriesCount() {
        synchronized (this.activitiesLogRAF) {
            return (long) Math.floor((double) this.getActivitiesLogFile().length() / (double) ActivityLogEntry.SIZE);
        }
    }

    /**
     * <p>Returns the last recorded timestamp for activity log. If not found, returns -1.</p>
     * @return -1 if no entries; last timestamp otherwise
     * @throws java.io.IOException
     */
    public long getLastRecordedTimestamp() throws IOException {
        synchronized (this.activitiesLogRAF) {
            byte[] activityBytes = new byte[ActivityLogEntry.SIZE];
            long lastEntryOffset = activitiesLogRAF.length() - ActivityLogEntry.SIZE;

            // If no entries, return -1
            if (lastEntryOffset < 0) {
                return -1;
            }

            this.activitiesLogRAF.seek(lastEntryOffset);
            this.activitiesLogRAF.readFully(activityBytes);
            ActivityLogEntry lastEntry = new ActivityLogEntry(activityBytes);
            return lastEntry.getTimestamp();
        }
    }
}
