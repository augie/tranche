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

import org.tranche.util.*;
import java.text.DecimalFormat;
import org.tranche.hash.BigHash;
import org.tranche.time.TimeUtil;

/**
 * <p>Command-line listener for the file replication tool.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class CommandLineProjectReplicationToolListener implements ProjectReplicationToolListener {

    private ProjectReplicationTool replicationTool = null;
    private long start = -1;

    /**
     * <p>Event fired when replication started.</p>
     * @param replicationTool
     */
    public void fireReplicationStarted(ProjectReplicationTool replicationTool) {
        this.replicationTool = replicationTool;
        this.start = TimeUtil.getTrancheTimestamp();
        System.out.println("Project replication tool started at " + Text.getFormattedDate(start));
    }

    /**
     * <p>Event fired when replication failed.</p>
     */
    public void fireReplicationFailed() {
        System.err.println("Project replication failed at " + Text.getFormattedDate(start) + ", ran for " + Text.getPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp() - start));
        printReplicationStats();
    }

    /**
     * <p>Event fired when replication finished.</p>
     */
    public void fireReplicationFinished() {
        System.out.println("Project replication finished at " + Text.getFormattedDate(start) + ", ran for " + Text.getPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp() - start));
        printReplicationStats();
    }

    /**
     * <p>Event fired when data chunk replicated.</p>
     * @param h
     */
    public void fireDataChunkReplicated(BigHash h) {
    }

    /**
     * <p>Event fired when meta data chunk replicated.</p>
     * @param h
     */
    public void fireMetaDataChunkReplicated(BigHash h) {
    }

    /**
     * <p>Event fired when data chunk skipped.</p>
     * @param h
     */
    public void fireDataChunkSkipped(BigHash h) {
    }

    /**
     * <p>Event fired when meta data chunk skipped.</p>
     * @param h
     */
    public void fireMetaDataChunkSkipped(BigHash h) {
    }

    /**
     * <p>Event fired when data chunk failed.</p>
     * @param h
     */
    public void fireDataChunkFailed(BigHash h) {
    }

    /**
     * <p>Event fired when meta data chunk failed.</p>
     * @param h
     */
    public void fireMetaDataChunkFailed(BigHash h) {
    }

    /**
     * <p>Event fired when file failed.</p>
     * @param h
     */
    public void fireFileFailed(BigHash h) {
        System.err.println("- " + getPercentageComplete() + " done. File failed to completely replicate: " + h.toString());
    }

    /**
     * <p>Event fired when file finished.</p>
     * @param h
     */
    public void fireFileFinished(BigHash h) {
        System.out.println("+ " + getPercentageComplete() + " done. File finished: " + h.toString());
    }

    /**
     * <p>Internal method for building string with percentage information.</p>
     * @return
     */
    private String getPercentageComplete() {
        long projectSize = this.replicationTool.getSizeOfProject();
        long handled = this.replicationTool.getSizeAlreadyHandled();

        double percentage = 0;
        if (projectSize > 0) {
            percentage = 100.0 * ((double) handled / (double) projectSize);
        }
        DecimalFormat twoPlaces = new DecimalFormat("###.00");
        return twoPlaces.format(percentage) + "% (" + handled + " out of " + projectSize + ")";
    }

    /**
     * <p>Print out replication statistics to standard output.</p>
     */
    private void printReplicationStats() {

        DecimalFormat fixedSize = new DecimalFormat("000,000,000");
        String successData = fixedSize.format(this.replicationTool.getSuccessDataChunkCount());
        String successMeta = fixedSize.format(this.replicationTool.getSuccessMetaDataChunkCount());
        String failureData = fixedSize.format(this.replicationTool.getFailedDataChunkCount());
        String failureMeta = fixedSize.format(this.replicationTool.getFailedMetaDataChunkCount());
        String skippedData = fixedSize.format(this.replicationTool.getSkippedDataChunkCount());
        String skippedMeta = fixedSize.format(this.replicationTool.getSkippedMetaDataChunkCount());
        String totalData = fixedSize.format(this.replicationTool.getSuccessDataChunkCount() + this.replicationTool.getFailedDataChunkCount() + this.replicationTool.getSkippedDataChunkCount());
        String totalMeta = fixedSize.format(this.replicationTool.getSuccessMetaDataChunkCount() + this.replicationTool.getFailedMetaDataChunkCount() + this.replicationTool.getSkippedMetaDataChunkCount());

        System.out.println("-------------------------------------------");
        System.out.println("                    DATA               META");
        System.out.println("-------------------------------------------");
        System.out.println("SUCCESS:     " + successData + "        " + successMeta);
        System.out.println("FAILURE:     " + failureData + "        " + failureMeta);
        System.out.println("SKIPPED:     " + skippedData + "        " + skippedMeta);
        System.out.println("-------------------------------------------");
        System.out.println("TOTAL:       " + totalData + "        " + totalMeta);
    }
}
