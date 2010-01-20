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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tranche.security.Signature;
import org.tranche.hash.BigHash;
import org.tranche.util.TestUtil;

/**
 * Writes to log in bytes
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class LogSubmitter {

    // Separate LogSubmitter for each server IP address.
    private static Map<String, LogSubmitter> submitters = new HashMap<String, LogSubmitter>();
    // Single thread broadcasts all events to registered Submittables.
    private LoggerQueueThread lqt = null;
    // When a server registers itself with this submitter, this changes.
    private String serverURL = "127.0.0.1";

    /**
     * Factory method to create a logger.
     */
    public static LogSubmitter getSubmitter(String serverURL) {

        LogSubmitter submitter = submitters.get(serverURL);

        // If no submitter, create one and add to map
        if (submitter == null) {
            submitter = new LogSubmitter(serverURL);
            submitters.put(serverURL, submitter);
        }

        return submitter;
    }

    /**
     * Create a log submitter for submitting logs.
     * @param ip
     */
    public LogSubmitter(String ip) {
        this.serverURL = ip;

        try {
            // Create and start the log queue thread
            lqt = new LoggerQueueThread();

            // Add default submittables (i.e., default log)
            lqt.addSubmittable(AutoUploadingBinaryLog.getLogManager(serverURL));

            if (TestUtil.isTesting()) {
                // Do nothing, throw away submissions
                lqt.requestStop();
            } else {
                // Start the submission thread
                lqt.start();
            }
        } catch (Exception ex) {
        }
    }

    /**
     * Attach a submittable (i.e., another log) to the submitter. The submitter broadcasts submissions to all Submittables.
     */
    public void attachSubmittable(Submittable subscriber) {
        this.lqt.addSubmittable(subscriber);
    }

    /**
     * Releases resources used by logger and log.
     */
    public void close() {

        if (this.lqt != null) {
            this.lqt.requestStop();
            try {
                // Wait a little so new logger doesn't try to open same resource
                this.lqt.join(500);
            } catch (InterruptedException ex) { /* nope */ }
        }

        // Remove submitter from map
        submitters.remove(this.serverURL);
    }

    /**
     * Set data.
     * @param hash
     * @param sig
     * @param ip
     */
    public void logSetData(BigHash hash, Signature sig, String ip) {
        LogEntryQueueItem item = new LogEntryQueueItem();
        item.action = LogEntryQueueItem.SET_DATA;
        item.hash = hash;
        item.sig = sig;
        item.ip = ip;

        this.lqt.submit(item);
    }

    /**
     * Set meta data.
     * @param hash
     * @param sig
     * @param ip
     */
    public void logSetMetaData(BigHash hash, Signature sig, String ip) {
        LogEntryQueueItem item = new LogEntryQueueItem();
        item.action = LogEntryQueueItem.SET_META_DATA;
        item.hash = hash;
        item.sig = sig;
        item.ip = ip;

        this.lqt.submit(item);
    }

    /**
     * Set configuration data.
     * @param sig
     * @param ip
     */
    public void logSetConfiguration(Signature sig, String ip) {
        LogEntryQueueItem item = new LogEntryQueueItem();
        item.action = LogEntryQueueItem.SET_CONFIG;
        item.sig = sig;
        item.ip = ip;

        this.lqt.submit(item);
    }

    /**
     * 
     * @param hashes
     * @param ip
     */
    public void logBatchGetData(BigHash[] hashes, String ip) {
        for (BigHash hash : hashes) {
            LogEntryQueueItem item = new LogEntryQueueItem();
            item.action = LogEntryQueueItem.GET_DATA;
            item.hash = hash;
            item.ip = ip;
            this.lqt.submit(item);
        }
    }

    /**
     *
     * @param hashes
     * @param ip
     */
    public void logBatchGetMetaData(BigHash[] hashes, String ip) {
        for (BigHash hash : hashes) {
            LogEntryQueueItem item = new LogEntryQueueItem();
            item.action = LogEntryQueueItem.GET_META_DATA;
            item.hash = hash;
            item.ip = ip;
            this.lqt.submit(item);
        }
    }

    /**
     * Retrieve configuration.
     * @param ip
     */
    public void logGetConfiguration(String ip) {
        LogEntryQueueItem item = new LogEntryQueueItem();
        item.action = LogEntryQueueItem.GET_CONFIG;
        item.ip = ip;

        this.lqt.submit(item);
    }

    /**
     * Retrieve nonce.
     * @param ip
     */
    public void logGetNonce(String ip) {
        LogEntryQueueItem item = new LogEntryQueueItem();
        item.action = LogEntryQueueItem.GET_NONCE;
        item.ip = ip;

        this.lqt.submit(item);
    }

    /**
     * Handles I/O queue. Server should wait for I/O.
     */
    private class LoggerQueueThread extends Thread {

        // Flag for request to stop thread. Thread actually stops when all pending items are complete.
        private boolean isStopped = false;
        // Queue for log IO requests
        private final List<LogEntryQueueItem> queue = new ArrayList();
        // All submittables subscribe to the submitter, which broadcasts log entries.
        private final Set<Submittable> submittables = new HashSet();

        /**
         * Instantiate a low-priority daemon thread that broadcasts submissions.
         */
        protected LoggerQueueThread() {
            setName("Logger queue thread");
            setPriority(Thread.MIN_PRIORITY);
            setDaemon(true);
        }

        @Override()
        public void run() {
            while (!isStopped || !queue.isEmpty()) {
                try {

                    // If empty, take a nap. This is a very low-priority thread
                    if (this.queue.isEmpty()) {
                        Thread.sleep(1000);
                        continue;
                    }

                    // Get next item. One at time so doesn't neglect other tasks
                    // (like uploading to server)
                    LogEntryQueueItem item = null;

                    synchronized (queue) {
                        item = this.queue.remove(0);
                    }

                    switch (item.action) {
                        case LogEntryQueueItem.NONE:
                            // Nothing to do
                            break;
                        case LogEntryQueueItem.GET_CONFIG:
                            for (Submittable log : submittables) {
                                log.getConfiguration(item.ip);
                            }
                            break;
                        case LogEntryQueueItem.GET_DATA:
                            for (Submittable log : submittables) {
                                log.getData(item.hash, item.ip);
                            }
                            break;
                        case LogEntryQueueItem.GET_META_DATA:
                            for (Submittable log : submittables) {
                                log.getMetaData(item.hash, item.ip);
                            }
                            break;
                        case LogEntryQueueItem.GET_NONCE:
                            for (Submittable log : submittables) {
                                log.getNonce(item.ip);
                            }
                            break;
                        case LogEntryQueueItem.SET_CONFIG:
                            for (Submittable log : submittables) {
                                log.setConfiguration(item.sig, item.ip);
                            }
                            break;
                        case LogEntryQueueItem.SET_DATA:
                            for (Submittable log : submittables) {
                                log.setData(item.hash, item.sig, item.ip);
                            }
                            break;
                        case LogEntryQueueItem.SET_META_DATA:
                            for (Submittable log : submittables) {
                                log.setMetaData(item.hash, item.sig, item.ip);
                            }
                            break;
                        default:
                            // whoops
                            throw new RuntimeException("Unrecognized LogQueueItem action: " + item.action);
                    }

                } catch (Exception ex) {
                    // Shouldn't get here
                    System.err.println("Exception in LoggerQueueThread: " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }

            // finished, close off resources
            for (Submittable log : submittables) {
                log.close();
            }
        }

        /**
         * Submit a log entry. It will be broadcasted to all subscribers on a single thread.
         */
        public void submit(LogEntryQueueItem item) {
            if (isStopped) {
                return;
            }
            synchronized (queue) {
                queue.add(item);
            }
        }

        /**
         * Add a submittable. These listen for log events and record them using their logic.
         */
        public void addSubmittable(Submittable log) {
            this.submittables.add(log);
        }

        /**
         * Ask submittor to stop accepting submissions and to finish its pending work.
         */
        public void requestStop() {
            isStopped = true;
        }
    }
}