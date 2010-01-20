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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import org.tranche.ConfigureTranche;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;
import org.tranche.annotations.Todo;
import org.tranche.security.Signature;
import org.tranche.time.TimeUtil;

/**
 * <p>Uploads logs files to server in binary format.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
@Todo(desc = "Change to use executor for upload, queue up others on disk.", day = 0, month = 0, year = 0, author = "Unknown")
public class AutoUploadingBinaryLog implements Submittable {

    /**
     * Set to true to turn on useful tracers.
     */
    private static final boolean isDebug = false;
    /**
     * This Server's URL.
     */
    private static String serverURL;
    /**
     *
     */
    private LogEntries entries;
    /**
     *
     */
    private long start;

    /**
     *
     */
    private AutoUploadingBinaryLog(String serverURL) {
        this.serverURL = serverURL;
        entries = new LogEntries();
        start = TimeUtil.getTrancheTimestamp();
    }
    /**
     * Support multiple servers for testing or whatever.
     */
    private static Map<String, AutoUploadingBinaryLog> logManagers = new HashMap();
    /**
     * Queue that validates server has log.
     */
    private static final ArrayBlockingQueue<String> validationQueue = new ArrayBlockingQueue(100);
    /**
     * Validates logs, re-uploading if needed.
     */
    private final ValidationThread validator = new ValidationThread();
    /**
     * Stores the previously-uploaded log. Quick method to give server
     * time to process log before validating.
     */
    private String lastUploadedLog = null;

    /**
     * Factory method and registry. Creates a manager if not exist for serverURL, or retrieves logManager.
     * @param serverURL
     * @return
     */
    public static AutoUploadingBinaryLog getLogManager(String serverURL) {

        AutoUploadingBinaryLog manager = logManagers.get(serverURL);

        if (manager == null) {
            manager = new AutoUploadingBinaryLog(serverURL);
            logManagers.put(serverURL, manager);
        }

        printTracer("Set or retrieved a log manager for " + serverURL);

        return manager;
    }

    /**
     * Set data.
     * @param hash
     * @param sig
     * @param ip
     */
    public void setData(BigHash hash, Signature sig, String ip) {
        performUploadIfTime(false);
        try {
            this.entries.add(LogEntry.logSetData(TimeUtil.getTrancheTimestamp(), ip, hash, sig));
        } catch (Exception ex) {
            // Skip, shouldn't happen
            printTracer("Exception in setData: " + ex.getMessage());
        }
    }

    /**
     * Set meta data.
     * @param hash
     * @param sig
     * @param ip
     */
    public void setMetaData(BigHash hash, Signature sig, String ip) {
        performUploadIfTime(false);
        try {
            this.entries.add(LogEntry.logSetMetaData(TimeUtil.getTrancheTimestamp(), ip, hash, sig));
        } catch (Exception ex) {
            // Skip, shouldn't happen
        }
    }

    /**
     * Set configuration.
     * @param sig
     * @param ip
     */
    public void setConfiguration(Signature sig, String ip) {
        performUploadIfTime(false);
        try {
            this.entries.add(LogEntry.logSetConfiguration(TimeUtil.getTrancheTimestamp(), ip, sig));
        } catch (Exception ex) {
            // Skip, shouldn't happen
        }
    }

    /**
     * Retrieve data.
     * @param hash
     * @param ip
     */
    public void getData(BigHash hash, String ip) {
        performUploadIfTime(false);
        try {
            this.entries.add(LogEntry.logGetData(TimeUtil.getTrancheTimestamp(), ip, hash));
        } catch (Exception ex) {
            // Skip, shouldn't happen
            printTracer("Exception in getData: " + ex.getMessage());
        }
    }

    /**
     * Retrieve metadata.
     * @param hash
     * @param ip
     */
    public void getMetaData(BigHash hash, String ip) {
        performUploadIfTime(false);
        try {
            this.entries.add(LogEntry.logGetMetaData(TimeUtil.getTrancheTimestamp(), ip, hash));
        } catch (Exception ex) {
            // Skip, shouldn't happen
            printTracer("Exception in getMetaData: " + ex.getMessage());
        }
    }

    /**
     * Retrieve configuration.
     * @param ip
     */
    public void getConfiguration(String ip) {
        performUploadIfTime(false);
        try {
            this.entries.add(LogEntry.logGetConfiguration(TimeUtil.getTrancheTimestamp(), ip));
        } catch (Exception ex) {
            // Skip, shouldn't happen
            printTracer("Exception in getConfiguration: " + ex.getMessage());
        }
    }

    /**
     * Retrienve nonce.
     * @param ip
     */
    public void getNonce(String ip) {
        performUploadIfTime(false);
        try {
            this.entries.add(LogEntry.logGetNonce(TimeUtil.getTrancheTimestamp(), ip));
        } catch (Exception ex) {
            // Skip, shouldn't happen
            printTracer("Exception in getNonce: " + ex.getMessage());
        }
    }

    /**
     * Coerces the log to flush contents and upload. Useful for testing or before a shutdown. Currently not called in any production code.
     */
    public synchronized void flush() {
        performUploadIfTime(true);
    }

    /**
     * Record everything to memory and return that file. Doesn't upload file, doesn't impact contents of memory.
     * @return
     * @throws java.lang.Exception
     */
    public synchronized File writeMemoryToFile() throws Exception {
//        printTracer("writeMemoryToFile -> writing "+entries.size()+" entries...");
        return writeLogsToDisk(entries);
    }

    /**
     * Uploads if 1hr has ellapsed or if 1MB of data.
     * @param flush True to coerce upload, false to use internal heuristics
     */
    private synchronized void performUploadIfTime(final boolean flush) {

        final boolean hasHourEllapsed = hasHourEllapsed();
        final boolean has1MBAccumulated = has1MBAccumulated();

        if (flush || hasHourEllapsed || has1MBAccumulated) {
            try {
                // only do this if the log url is not null
                final String logURL = ConfigureTranche.get(ConfigureTranche.PROP_LOG_SERVER_URL);
                if (logURL == null || logURL.equals("")) {
                    return;
                }

                StringBuffer msgBuf = new StringBuffer();
                if (hasHourEllapsed) {
                    msgBuf.append(" [Hour ellapsed] ");
                }
                if (has1MBAccumulated) {
                    msgBuf.append(" [1MB accumulated] ");
                }
                if (flush) {
                    msgBuf.append(" [flush, server closing?]");
                }
                printTracer("Uploading log, reason(s): " + msgBuf.toString());
                System.out.println("Uploading server log for " + serverURL + " at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + ", reason(s): " + msgBuf.toString());

                // Swap out entries
                final LogEntries entriesToCommit = this.entries;
                this.entries = new LogEntries();
                printTracer("Total of " + entriesToCommit.size() + " entries being written...");

                // Write to disk.
                final File logFile = writeLogsToDisk(entriesToCommit);

//                printTracer("Committing entries to disk: "+logFile.getAbsolutePath());
                final String u = this.serverURL;

                // Perform upload on a thread. Up to three tries then bail.
                Thread t = new Thread("Binary log file upload thread") {

                    public void run() {
                        boolean isFinished = false;
                        byte tries = 0;

                        while (!isFinished) {
                            try {
                                // Don't all JUnit tests to log their activities
                                if (!TestUtil.isTesting()) {
                                    LogUtil.uploadLogFile(logFile, u, logURL, hasHourEllapsed);
                                }

                                isFinished = true;
                            } catch (UploadFailedException ufe) {
                                tries++;
                                if (tries >= 3) {
                                    isFinished = true;
                                }
                            } catch (Exception ex) {
                                isFinished = true;
                            } finally {
//                                // Delete log file
//                                IOUtil.safeDelete(logFile);

                                // Store path in queue. Later validate and purge.
                                synchronized (validationQueue) {

                                    lastUploadedLog = logFile.getAbsolutePath();

                                    boolean added = validationQueue.offer(logFile.getAbsolutePath());

                                    // If not added, pop oldest entry.
                                    // Failsafe to make sure bad log files don't prevent future validation.
                                    if (!added) {
                                        if (validationQueue.remainingCapacity() <= 0) {
                                            String path = validationQueue.remove();
                                            if (path != null) {
                                                File oldLog = new File(path);
                                                if (oldLog.exists()) {
                                                    IOUtil.safeDelete(oldLog);
                                                } else {
                                                    System.err.println("Cannot delete old log file, does not exist: " + oldLog.getAbsolutePath());
                                                }

                                            }
                                        }
                                        validationQueue.offer(logFile.getAbsolutePath());
                                    }
                                }
                            }
                        }
                    }
                };
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);

                // No, use an executor
                t.start();

            } catch (Exception ex) {
                // For now, skip
            } finally {
                // Update start time only if hour ellapsed.
                // If not, still want the hourly log.
                if (hasHourEllapsed) {
                    start = TimeUtil.getTrancheTimestamp();
                }

                synchronized (validator) {
                    if (!isValidationRunning) {
                        validator.start();
                        isValidationRunning = true;
                    }
                }
            }
        } // If time to upload
    } // performUploadIfTime
    boolean isValidationRunning = false;

    /**
     *
     */
    private class ValidationThread extends Thread {

        public ValidationThread() {
            setDaemon(true);
            setPriority(Thread.MIN_PRIORITY);
        }

        public void run() {
            while (true) {

                String nextLog = null;

                try {

                    // Try to remove the next log
                    synchronized (validationQueue) {
                        String log = validationQueue.peek();
                        boolean isPreviouslyUploaded = lastUploadedLog != null && lastUploadedLog.equals(log);
                        if (log != null && !isPreviouslyUploaded) {
                            nextLog = validationQueue.remove();
                        }
                    }

                    if (nextLog == null) {
                        continue;
                    }

                    File logFile = new File(nextLog);

                    if (!logFile.exists()) {
                        System.err.println("Log file at " + logFile.getAbsolutePath() + " does not exist, cannot validate.");
                        continue;
                    }

                    // only do this if the log url is not null
                    final String logURL = ConfigureTranche.get(ConfigureTranche.PROP_LOG_SERVER_URL);
                    if (logURL == null || logURL.equals("")) {
                        // if null, just delete to not take too much space
                        printTracer("Log URL not set. Deleting log.");
                        IOUtil.safeDelete(logFile);
                        continue;
                    }

                    printTracer("Validating " + logFile.getAbsolutePath());

                    // If uploaded, delete. Else re-add.
                    if (LogUtil.uploadLogFile(logFile, serverURL, logURL, true)) {
                        IOUtil.safeDelete(logFile);
                        printTracer("Validated, deleting!");
                    } else {
                        printTracer("Nope, have to revalidate...");
                        synchronized (validationQueue) {
                            if (!validationQueue.contains(nextLog)) {
                                validationQueue.offer(nextLog);
                            }
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace(System.err);

                    if (nextLog != null) {
                        synchronized (validationQueue) {
                            if (!validationQueue.contains(nextLog)) {
                                validationQueue.offer(nextLog);
                            }
                        }
                    }

                } finally {
                    try {
                        // Sleep a bit
                        Thread.sleep(1000 * 30);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    } // ValidationThread

    /**
     * Print to stdout the tracer string.
     * @param msg
     */
    private static void printTracer(String msg) {
        if (isDebug) {
            System.out.println("AUTO_UPLOADING_BINARY_LOG> " + msg);
        }
    }

    /**
     * Return true if an hour has elapsed; otherwise false.
     * @return
     */
    private boolean hasHourEllapsed() {
        return (TimeUtil.getTrancheTimestamp() - start) >= (1000 * 60 * 60);
    }

    /**
     * Return true if at least 1MB of logs have accumulated; otherwise false.
     * @return
     */
    private boolean has1MBAccumulated() {
        return this.entries.lengthInBytes() >= (1024 * 1024);
    }

    /**
     * Close binary log.
     */
    public void close() {
        // Do nothing, just close
        this.flush();
    }

    /**
     *
     */
    private File writeLogsToDisk(LogEntries entriesToWrite) throws Exception {
        File tmpLogFile = TempFileUtil.createTemporaryFile(".log");

        LogWriter writer = new LogWriter(tmpLogFile);

        try {
            Iterator<LogEntry> it = entriesToWrite.iterator();

            // Write all the logs in memory
            LogEntry next;
            while (it.hasNext()) {
                next = it.next();
                writer.writeEntry(next);
            }

            return tmpLogFile;
        } finally {
            writer.close();
        }
    }
}
