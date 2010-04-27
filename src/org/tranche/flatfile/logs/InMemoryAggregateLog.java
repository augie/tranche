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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.tranche.commons.TextUtil;
import org.tranche.hash.BigHash;
import org.tranche.time.TimeUtil;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class InMemoryAggregateLog implements DataBlockUtilLog {

    private long start;
    private long aggregateTimeMerging;
    private Map<File, Long> mergeLogs;
    private long blockGetSuccesses;
    private long blockGetFailures;
    private long blockSetSuccesses;
    private long blockSetFailures;

    /**
     * 
     */
    public InMemoryAggregateLog() {
        restart();
    }

    /**
     * Resets the log. Since static singleton, only way to get new log.
     */
    public synchronized void restart() {
        start = TimeUtil.getTrancheTimestamp();
        aggregateTimeMerging = 0;
        blockGetSuccesses = 0;
        blockGetFailures = 0;
        blockSetSuccesses = 0;
        blockSetFailures = 0;
        mergeLogs = new HashMap();
    }

    /**
     * <p>Log start of merge.</p>
     * @param datablock
     */
    public synchronized void logMergeStart(File datablock) {
        mergeLogs.put(datablock, TimeUtil.getTrancheTimestamp());
    }

    /**
     * <p>Log stop of merge.</p>
     * @param datablock
     */
    public synchronized void logMergeFinish(File datablock) {
        long mergeStart = mergeLogs.remove(datablock);
        aggregateTimeMerging += (TimeUtil.getTrancheTimestamp() - mergeStart);
    }

    /**
     * <p>Log the start of a has chunk activity.</p>
     * @param hash
     */
    public void logHasStarted(BigHash hash) {
        // Nothing for now
    }

    /**
     * <p>Log has a chunk.</p>
     * @param hash
     */
    public void logHasTrue(BigHash hash) {
        // Nothing for now
    }

    /**
     * <p>Log does not have a chunk.</p>
     * @param hash
     */
    public void logHasFalse(BigHash hash) {
        // Nothing for now
    }

    /**
     * <p>Log start of a get request.</p>
     * @param hash
     */
    public synchronized void logGetStarted(BigHash hash) {
        // Nothing for now
    }

    /**
     * <p>Log start of a set request.</p>
     * @param hash
     */
    public synchronized void logSetStarted(BigHash hash) {
        // Nothing for now
    }

    /**
     * <p>Log successful get request.</p>
     * @param hash
     */
    public synchronized void logGetBlockSucceed(BigHash hash) {
        blockGetSuccesses++;
    }

    /**
     * <p>Log failed get request.</p>
     * @param hash
     */
    public synchronized void logGetBlockFailed(BigHash hash) {
        blockGetFailures++;
    }

    /**
     * <p>Log successful set request.</p>
     * @param hash
     */
    public synchronized void logSetBlockSucceed(BigHash hash) {
        blockSetSuccesses++;
    }

    /**
     * <p>Log failed set request.</p>
     * @param hash
     */
    public synchronized void logSetBlockFailed(BigHash hash) {
        blockSetFailures++;
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public synchronized long getTimeSpentMerging() {
        return aggregateTimeMerging;
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public synchronized long getRuntime() {
        return TimeUtil.getTrancheTimestamp() - start;
    }

    @Override()
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("In-memory aggregate log for DataBlockUtil:\n");
        buffer.append(" - Total run time: " + TextUtil.formatTimeLength(getRuntime()) + "\n");
        buffer.append(" - Total merge time: " + TextUtil.formatTimeLength(getTimeSpentMerging()) + "\n");
        buffer.append(" - Set blocks: successes=>" + getBlockSetSuccesses() + ", failures=>" + getBlockSetFailures() + "\n");
        buffer.append(" - Get blocks: successes=>" + getBlockGetSuccesses() + ", failures=>" + getBlockGetFailures() + "\n");

        return buffer.toString();
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getBlockGetSuccesses() {
        return blockGetSuccesses;
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getBlockGetFailures() {
        return blockGetFailures;
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getBlockSetSuccesses() {
        return blockSetSuccesses;
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getBlockSetFailures() {
        return blockSetFailures;
    }

    /**
     * <p>Close off resources associated with this log.</p>
     * @throws java.lang.Exception
     */
    public void close() throws Exception {
        // Nothing
    }
}
