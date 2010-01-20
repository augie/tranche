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
package org.tranche.project;

import org.tranche.hash.BigHash;

/**
 * <p>Listener for project replication tool.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public interface ProjectReplicationToolListener {

    /**
     * <p>Event fired when replication started.</p>
     * @param replicationTool
     */
    public void fireReplicationStarted(ProjectReplicationTool replicationTool);

    /**
     * <p>Event fired when replication failed.</p>
     */
    public void fireReplicationFailed();

    /**
     * <p>Event fired when replication finished.</p>
     */
    public void fireReplicationFinished();

    /**
     * <p>Event fired when data chunk replicated.</p>
     * @param h
     */
    public void fireDataChunkReplicated(BigHash h);

    /**
     * <p>Event fired when meta data chunk replicated.</p>
     * @param h
     */
    public void fireMetaDataChunkReplicated(BigHash h);

    /**
     * <p>Event fired when data chunk skipped.</p>
     * @param h
     */
    public void fireDataChunkSkipped(BigHash h);

    /**
     * <p>Event fired when meta data chunk skipped.</p>
     * @param h
     */
    public void fireMetaDataChunkSkipped(BigHash h);

    /**
     * <p>Event fired when data chunk failed.</p>
     * @param h
     */
    public void fireDataChunkFailed(BigHash h);

    /**
     * <p>Event fired when meta data chunk failed.</p>
     * @param h
     */
    public void fireMetaDataChunkFailed(BigHash h);

    /**
     * <p>Event fired when file failed.</p>
     * @param h
     */
    public void fireFileFailed(BigHash h);

    /**
     * <p>Event fired when file finished.</p>
     * @param h
     */
    public void fireFileFinished(BigHash h);
}
