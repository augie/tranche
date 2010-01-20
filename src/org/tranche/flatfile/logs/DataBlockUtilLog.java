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
import org.tranche.hash.BigHash;

/**
 * <p>An interface for any log object used by the DataBlockUtil.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public interface DataBlockUtilLog {
    
    /**
     * <p>Log start of merge.</p>
     * @param datablock
     */
    public void logMergeStart(File datablock);
    
    /**
     * <p>Log stop of merge.</p>
     * @param datablock
     */
    public void logMergeFinish(File datablock);
    
    /**
     * <p>Log the start of a has chunk activity.</p>
     * @param hash
     */
    public void logHasStarted(BigHash hash);
    
    /**
     * <p>Log has a chunk.</p>
     * @param hash
     */
    public void logHasTrue(BigHash hash);
    
    /**
     * <p>Log does not have a chunk.</p>
     * @param hash
     */
    public void logHasFalse(BigHash hash);
    
    /**
     * <p>Log start of a get request.</p>
     * @param hash
     */
    public void logGetStarted(BigHash hash);
    
    /**
     * <p>Log start of a set request.</p>
     * @param hash
     */
    public void logSetStarted(BigHash hash);
    
    /**
     * <p>Log successful get request.</p>
     * @param hash
     */
    public void logGetBlockSucceed(BigHash hash);
    
    /**
     * <p>Log failed get request.</p>
     * @param hash
     */
    public void logGetBlockFailed(BigHash hash);
    
    /**
     * <p>Log successful set request.</p>
     * @param hash
     */
    public void logSetBlockSucceed(BigHash hash);
    
    /**
     * <p>Log failed set request.</p>
     * @param hash
     */
    public void logSetBlockFailed(BigHash hash);
    
    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getTimeSpentMerging();
    
    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getRuntime();

    @Override()
    public String toString();

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getBlockGetSuccesses();

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getBlockGetFailures();

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getBlockSetSuccesses();

    /**
     * <p>Uses the default log. For more information, call logs directly.</p>
     * @return
     */
    public long getBlockSetFailures();
    
    /**
     * <p>Close off resources associated with this log.</p>
     * @throws java.lang.Exception
     */
    public void close() throws Exception;
}
