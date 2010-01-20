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
package org.tranche.logs;

import java.io.*;
import java.util.concurrent.ArrayBlockingQueue;
import org.tranche.util.*;

/**
 *
 */
public class SimpleLog {
    
    /**
     * ------------------------------------------------------------
     *    Need to disable for stress tests. Throwing OOM erros
     * ------------------------------------------------------------
     */
    public static final boolean IS_DISABLED = true;

    /**
     * Identifier for add file tool log.
     */
    public static final int AFT_LOG = 0;
    
    /**
     * Identifier for get file tool log.
     */
    public static final int GFT_LOG = 1;
    
    /**
     * Identifier for remote tranche server log.
     */
    public static final int RTS_LOG = 2;
    
    /**
     * Identifier for server worker thread log.
     */
    public static final int SWT_LOG = 3;
    
    /**
     * Identifier for server utility log.
     */
    public static final int ServerUtil_LOG = 4;
    private static String[] logFiles = {
        PersistentFileUtil.getPersistentDirectory() + File.separator + "AFT.log",
        PersistentFileUtil.getPersistentDirectory() + File.separator + "GFT.log",
        PersistentFileUtil.getPersistentDirectory() + File.separator + "RTS.log",
        PersistentFileUtil.getPersistentDirectory() + File.separator + "SWT.log",
        PersistentFileUtil.getPersistentDirectory() + File.separator + "ServerUtil.log"
    };    // a queue of messages to log
    private static ArrayBlockingQueue<LogUnit> queue = new ArrayBlockingQueue(1000);
    private static boolean run = false;
    private static Thread backgroundLoggingThread = new LogWriterThread();

    private static class LogWriterThread extends Thread {
    
        /**
         * Execute simple log to take log units off the queue and write their 
         * contents out to their assigned log file.
         */
        @Override()
        public void run() {
            // set the name
            setName("Logging Thread (Background)");
            while (run || !queue.isEmpty()) {
                try {
                    // wait on a log
                    LogUnit lu = queue.take();
                    // log to the right file
                    FileWriter fos = new FileWriter(logFiles[lu.log], true);
                    try {
                        fos.write(lu.buffer.toString() + "\n");
                    } finally {
                        IOUtil.safeClose(fos);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            System.out.println("Background logger exiting...");
        }
    };

    /**
     * Add a log unit to the run queue to be logged.
     * @param lu
     */
    public static final void log(LogUnit lu) {
        
        if (IS_DISABLED) {
            return;
        }
        
        try {
            if (run && backgroundLoggingThread.isAlive()) {
                queue.put(lu);
            }
        } catch (Exception e) {
            System.err.println("Can't log message! Cause: " + e.getMessage());
        }
    }
    
    /**
     * Set whether to enable or disable logging.
     * @param loggingEnabled
     */
    public static final void setEnabled(boolean loggingEnabled) {
        setEnabled(loggingEnabled, false);
    }

    /**
     * Set whether to enable logging and/or purge old log files.
     * @param loggingEnabled
     * @param purgeOldLogFiles
     */
    public static final void setEnabled(boolean loggingEnabled, boolean purgeOldLogFiles) {
        // Start logging
        run = loggingEnabled;

        // purge old files if appropriate
        if (purgeOldLogFiles) {
            for (String logFile : logFiles) {
                new File(logFile).delete();
            }
        }

        // Create a new log if enabled
        if (loggingEnabled) {
            backgroundLoggingThread = new LogWriterThread();

            // Turn on the committer thread
            backgroundLoggingThread.setDaemon(true);
            backgroundLoggingThread.setPriority(Thread.MIN_PRIORITY);
            backgroundLoggingThread.start();
        } else {
            // join the thread
            try {
                // Give up to five seconds
                backgroundLoggingThread.join(5000);
            } catch (InterruptedException e) {
                // noop
            }
        }
    }

    /**
     * Set log identifier and file location for log for simple logging.
     * @param log
     * @param fileForLog
     */
    public static void setLog(int log, File fileForLog) {
        logFiles[log] = fileForLog.getAbsolutePath();
    }
}
