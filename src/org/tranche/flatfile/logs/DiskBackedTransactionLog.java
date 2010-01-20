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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import org.tranche.hash.BigHash;
import org.tranche.time.TimeUtil;
import org.tranche.util.Text;

/**
 * Logs every transaction. Useful for stress testing, but will negatively impact performance due to transaction writes.
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class DiskBackedTransactionLog implements DataBlockUtilLog {

    private File logFile;
    private long start;
    private long aggregateTimeMerging;
    private Map<File, Long> mergeLogs;
    private Map<BigHash, Long> logGets;
    private Map<BigHash, Long> logSets;
    private Map<BigHash, Long> logHas;
    private long blockGetSuccesses;
    private long blockGetFailures;
    private long blockSetSuccesses;
    private long blockSetFailures;
    private Writer writer;
    private static final int hashSubstrLen = 10;

    /**
     * 
     * @param logFile
     * @throws java.lang.Exception
     */
    public DiskBackedTransactionLog(File logFile) throws Exception {
        this.logFile = logFile;
        restart();
    }

    /**
     * <p>Reset resources.</p>
     * @throws java.lang.Exception
     */
    public synchronized void restart() throws Exception {
        start = TimeUtil.getTrancheTimestamp();
        aggregateTimeMerging = 0;
        blockGetSuccesses = 0;
        blockGetFailures = 0;
        blockSetSuccesses = 0;
        blockSetFailures = 0;
        mergeLogs = new HashMap();
        logGets = new HashMap();
        logSets = new HashMap();
        logHas = new HashMap();
        writer = new BufferedWriter(new FileWriter(this.logFile));
    }

    /**
     * <p>Return the file used by the log.</p>
     * @return
     */
    public synchronized File getLogFile() {
        return logFile;
    }

    /**
     * <p>Log start of merge.</p>
     * @param datablock
     */
    public synchronized void logMergeStart(File datablock) {
        long start = TimeUtil.getTrancheTimestamp();
        mergeLogs.put(datablock, start);
        try {
            writer.write("START MERGE " + Text.getFormattedDate(start) + " <" + datablock.getAbsolutePath() + ">\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * <p>Log stop of merge.</p>
     * @param datablock
     */
    public synchronized void logMergeFinish(File datablock) {
        long start = mergeLogs.remove(datablock);
        long finish = TimeUtil.getTrancheTimestamp();
        try {
            aggregateTimeMerging += (finish - start);
            writer.write("FINISH MERGE " + Text.getFormattedDate(start) + " <" + datablock.getAbsolutePath() + "> (DELTA=" + Text.getPrettyEllapsedTimeString(finish - start) + ")\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * <p>Log the start of a has chunk activity.</p>
     * @param hash
     */
    public void logHasStarted(BigHash hash) {

        // May have called self during merge if dated block, so ignore
        if (logHas.containsKey(hash)) {
            return;
        }

        logHas.put(hash, TimeUtil.getTrancheTimestamp());

        try {
            writer.write("HAS STARTED " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " <" + hash.toString().substring(0, hashSubstrLen) + "...>\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * <p>Log has a chunk.</p>
     * @param hash
     */
    public void logHasTrue(BigHash hash) {
        try {
            long delta = TimeUtil.getTrancheTimestamp() - logHas.remove(hash);
            writer.write("HAS TRUE " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " <" + hash.toString().substring(0, hashSubstrLen) + "...> Took: " + Text.getPrettyEllapsedTimeString(delta) + "\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * <p>Log does not have a chunk.</p>
     * @param hash
     */
    public void logHasFalse(BigHash hash) {
        try {
            long delta = TimeUtil.getTrancheTimestamp() - logHas.remove(hash);
            writer.write("HAS FALSE " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " <" + hash.toString().substring(0, hashSubstrLen) + "...> Took: " + Text.getPrettyEllapsedTimeString(delta) + "\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * <p>Log start of a get request.</p>
     * @param hash
     */
    public synchronized void logGetStarted(BigHash hash) {

        // Don't put as a get if in "has" already
        if (logHas.containsKey(hash)) {
            return;
        }

        // Don't put as a get if in "set" already
        if (logSets.containsKey(hash)) {
            return;
        }

        // May have called self during merge if dated block, so ignore
        if (logGets.containsKey(hash)) {
            return;
        }

        logGets.put(hash, TimeUtil.getTrancheTimestamp());

        try {
            writer.write("GET STARTED " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " <" + hash.toString().substring(0, hashSubstrLen) + "...>\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * <p>Log successful get request.</p>
     * @param hash
     */
    public synchronized void logGetBlockSucceed(BigHash hash) {

        // Do nothing if not in logGets -- it was a "has" lookup or "set"
        if (!logGets.containsKey(hash)) {
            return;
        }

        try {
            long delta = TimeUtil.getTrancheTimestamp() - logGets.remove(hash);
            blockGetSuccesses++;
            writer.write("GET OK " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " <" + hash.toString().substring(0, hashSubstrLen) + "...> Took: " + Text.getPrettyEllapsedTimeString(delta) + "\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * <p>Log failed get request.</p>
     * @param hash
     */
    public synchronized void logGetBlockFailed(BigHash hash) {

        try {
            long delta = TimeUtil.getTrancheTimestamp() - logGets.remove(hash);
            blockGetFailures++;
            writer.write("GET FAILED " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " <" + hash.toString().substring(0, hashSubstrLen) + "...> Took: " + Text.getPrettyEllapsedTimeString(delta) + "\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * <p>Log start of a set request.</p>
     * @param hash
     */
    public synchronized void logSetStarted(BigHash hash) {

        // May have called self during merge if dated block, so ignore
        if (logSets.containsKey(hash)) {
            return;
        }

        logSets.put(hash, TimeUtil.getTrancheTimestamp());

        try {
            writer.write("SET STARTED " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " <" + hash.toString().substring(0, hashSubstrLen) + "...>\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * <p>Log successful set request.</p>
     * @param hash
     */
    public synchronized void logSetBlockSucceed(BigHash hash) {

        try {
            long delta = TimeUtil.getTrancheTimestamp() - logSets.remove(hash);
            blockSetSuccesses++;
            writer.write("SET OK " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " <" + hash.toString().substring(0, hashSubstrLen) + "...> Took: " + Text.getPrettyEllapsedTimeString(delta) + "\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * <p>Log failed set request.</p>
     * @param hash
     */
    public synchronized void logSetBlockFailed(BigHash hash) {

        try {
            long delta = TimeUtil.getTrancheTimestamp() - logSets.remove(hash);
            blockSetFailures++;
            writer.write("SET FAILED " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " <" + hash.toString().substring(0, hashSubstrLen) + "...> Took: " + Text.getPrettyEllapsedTimeString(delta) + "\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
    public synchronized String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("Disk-backed transaction log for DataBlockUtil:\n");
        buffer.append(" - Total run time: " + Text.getPrettyEllapsedTimeString(getRuntime()) + "\n");
        buffer.append(" - Total merge time: " + Text.getPrettyEllapsedTimeString(getTimeSpentMerging()) + "\n");
        buffer.append(" - Set blocks: successes=>" + getBlockSetSuccesses() + ", failures=>" + getBlockSetFailures() + "\n");
        buffer.append(" - Get blocks: successes=>" + getBlockGetSuccesses() + ", failures=>" + getBlockGetFailures() + "\n");

        return buffer.toString();
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public synchronized long getBlockGetSuccesses() {
        return blockGetSuccesses;
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public synchronized long getBlockGetFailures() {
        return blockGetFailures;
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public synchronized long getBlockSetSuccesses() {
        return blockSetSuccesses;
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public synchronized long getBlockSetFailures() {
        return blockSetFailures;
    }

    /**
     * <p>Close off resources associated with this log.</p>
     * @throws java.lang.Exception
     */
    public synchronized void close() throws Exception {
        try {
            writer.flush();
        } catch (Exception ex) { /* might already be closed */ }
        writer.close();
    }
}
