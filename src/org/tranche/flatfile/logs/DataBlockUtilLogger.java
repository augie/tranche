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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.tranche.hash.BigHash;

/**
 * <p>Add logs to logger, which is invoked. Logs are like event listeners which conform to same interface as logger.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class DataBlockUtilLogger implements DataBlockUtilLog {
    
    /**
     * <p>Holds collection of logs to an interface.</p>
     */
    List<DataBlockUtilLog> logs;
    
    /**
     * 
     */
    private DataBlockUtilLogger() {
        logs = new LinkedList();
        
        // Add aggregate log for free
        logs.add(new InMemoryAggregateLog());
    }
    
    private static DataBlockUtilLogger singleton = null;
    
    /**
     * <p>Static access to singleton log.</p>
     * @return
     */
    public static synchronized DataBlockUtilLogger getLogger() {
        if (singleton == null)
            singleton = new DataBlockUtilLogger();
        
        return singleton;
    }
    
    /**
     * <p>Clear all logs from the logger. Will need to manually add logs if want,
     * including the aggregate in-memory log.</p>
     */
    public synchronized void clearLogs() {
        logs.clear();
    }

    /**
     * <p>Add a log that conforms to DataBlockUtilLog interface.</p>
     * @param log
     */
    public void addLog(DataBlockUtilLog log) {
        logs.add(log);
    }

    /**
     * <p>Returns an unmodifiable list of logs attached to logger.</p>
     * @return
     */
    public List<DataBlockUtilLog> getLogs() {
        return Collections.unmodifiableList(logs);
    }
    
    /**
     * <p>Log the start of a has chunk activity.</p>
     * @param hash
     */
    public void logHasStarted(BigHash hash) {
        for (DataBlockUtilLog log : logs) {
            log.logHasStarted(hash);
        }
    }
    
    /**
     * <p>Log has a chunk.</p>
     * @param hash
     */
    public void logHasTrue(BigHash hash) {
        for (DataBlockUtilLog log : logs) {
            log.logHasTrue(hash);
        }
    }
    
    /**
     * <p>Log does not have a chunk.</p>
     * @param hash
     */
    public void logHasFalse(BigHash hash) {
        for (DataBlockUtilLog log : logs) {
            log.logHasFalse(hash);
        }
    }
    
    /**
     * <p>Log start of a get request.</p>
     * @param hash
     */
    public void logGetStarted(BigHash hash) {
        for (DataBlockUtilLog log : logs) {
            log.logGetStarted(hash);
        }
    }
    
    /**
     * <p>Log start of a set request.</p>
     * @param hash
     */
    public void logSetStarted(BigHash hash) {
        for (DataBlockUtilLog log : logs) {
            log.logSetStarted(hash);
        }
    }
    
    /**
     * <p>Log start of merge.</p>
     * @param datablock
     */
    public void logMergeStart(File datablock) {
        for (DataBlockUtilLog log : logs) {
            log.logMergeStart(datablock);
        }
    }
    
    /**
     * <p>Log stop of merge.</p>
     * @param datablock
     */
    public void logMergeFinish(File datablock) {
        for (DataBlockUtilLog log : logs) {
            log.logMergeFinish(datablock);
        }
    }
    
    /**
     * <p>Log successful get request.</p>
     * @param hash
     */
    public void logGetBlockSucceed(BigHash hash) {
        for (DataBlockUtilLog log : logs) {
            log.logGetBlockSucceed(hash);
        }
    }
    
    /**
     * <p>Log failed get request.</p>
     * @param hash
     */
    public void logGetBlockFailed(BigHash hash) {
        for (DataBlockUtilLog log : logs) {
            log.logGetBlockFailed(hash);
        }
    }
    
    /**
     * <p>Log successful set request.</p>
     * @param hash
     */
    public void logSetBlockSucceed(BigHash hash) {
        for (DataBlockUtilLog log : logs) {
            log.logSetBlockSucceed(hash);
        }
    }
    
    /**
     * <p>Log failed set request.</p>
     * @param hash
     */
    public void logSetBlockFailed(BigHash hash) {
        for (DataBlockUtilLog log : logs) {
            log.logSetBlockFailed(hash);
        }
    }
    
    /**
     * <p>Close off resources associated with this log.</p>
     * @throws java.lang.Exception
     */
    public void close() throws Exception {
        for (DataBlockUtilLog log : logs) {
            log.close();
        }
    }
    
    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getTimeSpentMerging() {
        return logs.get(0).getTimeSpentMerging();
    }
    
    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getRuntime() {
        return logs.get(0).getRuntime();
    }
    
    /**
     * Uses the default log. For more information, call logs directly.
     */
    @Override()
    public String toString() {
        return logs.get(0).toString();
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getBlockGetSuccesses() {
        return logs.get(0).getBlockGetSuccesses();
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getBlockGetFailures() {
        return logs.get(0).getBlockGetFailures();
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getBlockSetSuccesses() {
        return logs.get(0).getBlockSetSuccesses();
    }

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getBlockSetFailures() {
        return logs.get(0).getBlockSetFailures();
    }
}
