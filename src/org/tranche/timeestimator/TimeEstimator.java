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

import org.tranche.commons.Debuggable;
import org.tranche.time.TimeUtil;

/**
 * <p>A utility class to easily calculate the estimated completion time for work.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class TimeEstimator extends Debuggable {

    /**
     * <p>Represented the amount of work to do.</p>
     */
    protected long totalBytes = 1;
    /**
     * <p>Represents the work done.</p>
     */
    protected long bytesCompleted = 0;
    /**
     * 
     */
    protected long totalFiles = 1;
    /**
     *
     */
    protected long filesCompleted = 0;
    /**
     * <p>The start time for the estimator.</p>
     */
    protected long startTime = TimeUtil.getTrancheTimestamp();
    /**
     * <p>The current time for the estimator.</p>
     */
    protected long currentTime = TimeUtil.getTrancheTimestamp();

    /**
     * <p>Updates the internal clock and provides a chance to update the amount of total work and the amount of work done.</p>
     * @param bytesCompleted An arbitrary long that represents the amount of work completed. Must be between 0 and Long.MAX_VALUE.
     * @param totalBytes An arbitrary long that represents the amount of work to do. Must be between 0 and Long.MAX_VALUE.
     * @param filesCompleted
     * @param totalFiles
     */
    public synchronized void update(long bytesCompleted, long totalBytes, long filesCompleted, long totalFiles) {
        // otherwise update the time
        currentTime = TimeUtil.getTrancheTimestamp();
        this.bytesCompleted = bytesCompleted;
        this.totalBytes = totalBytes;
        this.filesCompleted = filesCompleted;
        this.totalFiles = totalFiles;
    }

    /**
     * 
     * @return
     */
    public synchronized long getBytesDone() {
        return bytesCompleted;
    }

    /**
     *
     * @return
     */
    public synchronized long getTotalBytes() {
        return totalBytes;
    }

    /**
     *
     * @return
     */
    public synchronized long getFilesDone() {
        return filesCompleted;
    }

    /**
     *
     * @return
     */
    public synchronized long getTotalFiles() {
        return totalFiles;
    }

    /**
     * 
     * @param addTotalBytes
     */
    public synchronized void addTotalBytes(long addTotalBytes) {
        this.totalBytes += addTotalBytes;
    }

    /**
     * 
     * @param subtractTotalBytes
     */
    public synchronized void subtractTotalBytes(long subtractTotalBytes) {
        this.totalBytes -= subtractTotalBytes;
    }

    /**
     *
     * @param addTotalFiles
     */
    public synchronized void addTotalFiles(long addTotalFiles) {
        this.totalFiles += addTotalFiles;
    }

    /**
     * 
     * @param subtractTotalFiles
     */
    public synchronized void subtractTotalFiles(long subtractTotalFiles) {
        this.totalFiles -= subtractTotalFiles;
    }

    /**
     * <p>Returns the percentage of work complete as a number between 0 and 100, e.g. 97.23%</p>
     * @return Returns a double value that is between 0 and 100.
     */
    public synchronized double getPercentDone() {
        double percent = Math.floor(100.0 * (double) bytesCompleted / totalBytes);
        // Percent should never be over 100% or less than 0%
        if (percent > 100) {
            percent = 100;
        }
        if (percent < 0) {
            percent = 0;
        }
        return percent;
    }

    /**
     * <p>Returns the number of hours left to work. Might be more than 24.</p>
     * @return Returns a long value that is between 0 and Long.MAX_VALUE.
     */
    public synchronized long getHours() {
        // calc the file's size
        double percentDone = bytesCompleted * 100.0 / totalBytes;
        if (percentDone >= 100) {
            return 0;
        }
        // estimate time to completion -- divide by 1000 to get seconds
        long timeToCompletion = (long) ((currentTime - startTime) / (percentDone / 100) / 1000) - (currentTime - startTime) / 1000;
        // make sure the time isn't negative
        if (timeToCompletion < 0) {
            timeToCompletion = 0;
        }
        // get hours
        long hours = timeToCompletion / 60 / 60;
        return hours;
    }

    /**
     * <p>Returns the number of minutes left to work. Will be 0 through 60.</p>
     * @return Returns a minutes value that is beteen 0 and 60.
     */
    public synchronized long getMinutes() {
        // calc the file's size
        double percentDone = bytesCompleted * 100.0 / totalBytes;
        if (percentDone >= 100) {
            return 0;
        }
        // estimate time to completion -- divide by 1000 to get seconds
        long timeToCompletion = (long) ((currentTime - startTime) / (percentDone / 100) / 1000) - (currentTime - startTime) / 1000;
        // make sure the time isn't negative
        if (timeToCompletion < 0) {
            timeToCompletion = 0;
        }
        // get hours
        return timeToCompletion / 60 % 60;
    }

    /**
     * <p>Returns the number of seconds left to work. Will be 0 through 60.</p>
     * @return Returns a seconds value that is between 0 and 60.
     */
    public synchronized long getSeconds() {
        // calc the file's size
        double percentDone = bytesCompleted * 100.0 / totalBytes;
        if (percentDone >= 100) {
            return 0;
        }
        // estimate time to completion -- divide by 1000 to get seconds
        long timeToCompletion = (long) ((currentTime - startTime) / (percentDone / 100) / 1000) - (currentTime - startTime) / 1000;
        // make sure the time isn't negative
        if (timeToCompletion < 0) {
            timeToCompletion = 0;
        }
        // get hours
        return timeToCompletion % 60;
    }

    /**
     * <p>Get the total time the estimator has been running.</p>
     * @return
     */
    public synchronized long getTimeRunning() {
        return TimeUtil.getTrancheTimestamp() - startTime;
    }

    /**
     * <p>Get the time the estimator started.</p>
     * @return
     */
    public synchronized long getStartTime() {
        return startTime;
    }

    /**
     *
     * @return
     */
    public String getTimeLeftString() {
        String string = "";
        if (getHours() != 0) {
            string = string + getHours() + "h ";
        }
        if (getHours() != 0 || getMinutes() != 0) {
            string = string + getMinutes() + "m ";
        }
        return string + getSeconds() + "s";
    }
}
