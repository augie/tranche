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
package org.tranche.timeestimator;

import org.tranche.time.TimeUtil;
import org.tranche.util.*;

/**
 * <p>Estimated remaining time depends on performance in last n seconds.</p>
 * <p>Subclass of TimeEstimator, falls back on TimeEstimator until first window completed.</p>
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class ContextualTimeEstimator extends TimeEstimator {

    private static boolean debug = false;
    protected final static int SECOND = 1000, MINUTE = 60 * SECOND, HOUR = 60 * MINUTE;
    /**
     * <p>Too short of window puts too much emphasis on immediate performance; many
     * small files will result in too large estimated remaining time.</p>
     */
    private int CONTEXT_INTERVAL = 60 * SECOND;
    // Information used to calculate performance during current context window
    private long completedAtStartOfCurrentContext = 0;
    private long timestampAtStartOfCurrentContext = TimeUtil.getTrancheTimestamp();
    // Work performed during last context window
    private long completedDuringPreviousContextWindow = -1;

    public ContextualTimeEstimator() {
    }

    /**
     * 
     * @param bytesCompleted
     * @param totalBytes
     */
    public ContextualTimeEstimator(long bytesCompleted, long totalBytes, long totalFilesCompleted, long totalFiles) {
        update(bytesCompleted, totalBytes, totalFilesCompleted, totalFiles);
    }

    /**
     * <p>Returns true when time to update</p>
     * @return
     */
    private synchronized boolean timeForUpdate() {
        boolean timeForUpdate = timestampAtStartOfCurrentContext + CONTEXT_INTERVAL <= TimeUtil.getTrancheTimestamp();
        if (timeForUpdate) {
            debugOut("Percentage done: " + getPercentDone() + "%");
            debugOut("Time running: " + Text.getPrettyEllapsedTimeString(getTimeRunning()));
            debugOut("Total work: " + totalBytes + ", Completed work: " + bytesCompleted);
            debugOut("Estimated remaining time: " + getHours() + "hr " + getMinutes() + "min " + getSeconds() + "s\n");
        }
        return timeForUpdate;
    }

    /**
     * <p>Submit new data to the time estimator. Used to update the estimator.</p>
     * @param totalBytesCompleted
     * @param totalBytes
     */
    @Override
    public synchronized void update(long totalBytesCompleted, long totalBytes, long totalFilesCompleted, long totalFiles) {
        debugOut("update called: completed " + totalBytesCompleted + " out of " + totalBytes);
        if (timeForUpdate()) {
            // Calculate transfer for previous context
            completedDuringPreviousContextWindow = totalBytesCompleted - completedAtStartOfCurrentContext;
            // Start the next time context
            timestampAtStartOfCurrentContext = TimeUtil.getTrancheTimestamp();
            completedAtStartOfCurrentContext = totalBytesCompleted;
        }
        super.update(totalBytesCompleted, totalBytes, totalFilesCompleted, totalFiles);
    }

    /**
     * <p>Helper method used to estimate remaining time in milliseconds.</p>
     * @return
     */
    private synchronized long estimatedTimeRemainingInMillis() {
        // time remaining is 0 if the work has been completed
        if (bytesCompleted >= totalBytes) {
            return 0;
        }
        // Get total remaining work
        long remainingWork = totalBytes - bytesCompleted;
        // Calculate the number of remaining context windows assuming perform
        // the same as previous window
        double estimatedRemainingContexts = (double) remainingWork / completedDuringPreviousContextWindow;
        return Math.round(estimatedRemainingContexts * CONTEXT_INTERVAL);
    }

    /**
     * <p>Get the number of hours the estimator has been running.</p>
     * @return
     */
    @Override
    public synchronized long getHours() {
        // If no info about prev performance, use TimeEstimator (first context)
        if (completedDuringPreviousContextWindow == -1) {
            return super.getHours();
        }
        // Extract hours from time remaining
        long estimatedTime = estimatedTimeRemainingInMillis();
        return estimatedTime / HOUR;
    }

    /**
     * <p>Get the number of minutes the estimator has been running.</p>
     * @return
     */
    @Override
    public synchronized long getMinutes() {
        // If no info about prev performance, use TimeEstimator (first context)
        if (completedDuringPreviousContextWindow == -1) {
            return super.getMinutes();
        }
        // Extract minutes from time remaining
        long estimatedTime = estimatedTimeRemainingInMillis();
        estimatedTime %= HOUR;
        return estimatedTime / MINUTE;
    }

    /**
     * <p>Get the number of seconds the estimator has been running.</p>
     * @return
     */
    @Override
    public synchronized long getSeconds() {
        // If no info about prev performance, use TimeEstimator (first context)
        if (completedDuringPreviousContextWindow == -1) {
            return super.getSeconds();
        }
        // Extract seconds from time remaining
        long estimatedTime = estimatedTimeRemainingInMillis();
        estimatedTime %= HOUR;
        estimatedTime %= MINUTE;
        return estimatedTime / SECOND;
    }

    /**
     * <p>Returns the length of each context window used for estimating time remaining.</p>
     * @return
     */
    public synchronized int getContextInterval() {
        return CONTEXT_INTERVAL;
    }

    /**
     * <p>Set the sample context window duration for determining progress. Every context window, data transfered is measured and used to estimate time remaining.</p>
     * <p>E.g., if set to 30 seconds, every 30 seconds a new performance measurement is completed and used to estimate the time remaining for the next 30 seconds.</p>
     * @param milliseconds The time in milliseconds of each window.
     */
    public synchronized void setContextInterval(int milliseconds) {
        this.CONTEXT_INTERVAL = milliseconds;
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static void setDebug(boolean debug) {
        ContextualTimeEstimator.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(ContextualTimeEstimator.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param line
     */
    private static void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
