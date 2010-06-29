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
package org.tranche.get;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Collection;
import org.tranche.commons.TextUtil;
import org.tranche.server.PropagationExceptionWrapper;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class CommandLineGetFileToolListener implements GetFileToolListener {

    private static NumberFormat nf = NumberFormat.getInstance();

    static {
        nf.setGroupingUsed(true);
        nf.setMaximumFractionDigits(3);
    }
    private final GetFileTool gft;
    private final PrintStream out;
    private int chunkCount = 0;

    public CommandLineGetFileToolListener(GetFileTool gft, PrintStream out) {
        this.gft = gft;
        this.out = out;
    }

    /**
     * <p>Notification of any other event occurence.</p>
     * @param msg
     */
    public void message(String msg) {
        out.println(msg);
    }

    /**
     * <p>Notification that a meta data chunk is starting to be downloaded.</p>
     * @param event
     */
    public void startedMetaData(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a meta data chunk is trying to be downloaded from a server.</p>
     * @param event
     */
    public void tryingMetaData(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a meta data chunk could not be downloaded.</p>
     * @param event
     */
    public void failedMetaData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        out.println("Meta data chunk failed: " + event.getFileHash());
    }

    /**
     * <p>Notification that a meta data chunk has been downloaded.</p>
     * @param event
     */
    public void finishedMetaData(GetFileToolEvent event) {
    }

    /**
     * <p>Notificatin that a data chunk is starting to be downloaded.</p>
     * @param event
     */
    public void startedData(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk is trying to be downloaded from a server.</p>
     * @param event
     */
    public void tryingData(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk could not be downloaded.</p>
     * @param event
     */
    public void failedData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        out.println("Data chunk from file <name: " + event.getFileName() + "; hash:" + event.getFileHash() + "> failed: " + event.getChunkHash());
    }

    /**
     * <p>Notification that a data chunk has been downloaded.</p>
     * @param event
     */
    public void finishedData(GetFileToolEvent event) {
        // only show for every 10 chunks
        if (chunkCount == 10) {
            chunkCount = 0;
            out.println(nf.format(gft.getTimeEstimator().getPercentDone()) + "% done, " + getTimeLeftString());
        } else {
            chunkCount++;
            return;
        }
    }

    /**
     * <p>Notification that a file is starting to be downloaded.</p>
     * @param event
     */
    public void startingFile(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a file download has started.</p>
     * @param event
     */
    public void startedFile(GetFileToolEvent event) {
        out.println("Downloading file: " + event.getFileName() + ", hash: " + event.getFileHash());
    }

    /**
     * <p>Notification that a file downloaded has been skipped.</p>
     * @param event
     */
    public void skippedFile(GetFileToolEvent event) {
        out.println(nf.format(gft.getTimeEstimator().getPercentDone()) + "% done, " + getTimeLeftString() + ", skipped: " + event.getFileName());
    }

    /**
     * <p>Notification that a file download has finished.</p>
     * @param event
     */
    public void finishedFile(GetFileToolEvent event) {
        String downloaded = "";
        if (event.getFileName() != null) {
            downloaded = ", downloaded: " + event.getFileName();
        }
        out.println(nf.format(gft.getTimeEstimator().getPercentDone()) + "% done, " + getTimeLeftString() + downloaded);
    }

    /**
     * <p>Notification that a file download has failed.</p>
     * @param event
     */
    public void failedFile(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        if (event.getFileName() == null) {
            out.println("Failed to download file: " + event.getFileHash());
        } else {
            out.println("Failed to download file: " + event.getFileName() + ", hash: " + event.getFileHash());
        }
        for (PropagationExceptionWrapper e : exceptions) {
            out.println(e.exception.getClass().getName() + ": " + e.exception.getMessage());
        }
    }

    /**
     * <p>Notification that a directory download is starting.</p>
     * @param event
     */
    public void startingDirectory(GetFileToolEvent event) {
        out.println("Starting " + event.getFileHash());
    }

    /**
     * <p>Notification that a directory download has started.</p>
     * @param event
     */
    public void startedDirectory(GetFileToolEvent event) {
        out.println("Downloading " + event.getFileHash() + ", " + TextUtil.formatBytes(gft.getBytesToDownload()));
    }

    /**
     * <p>Notification that a directory download has finished.</p>
     * @param event
     */
    public void finishedDirectory(GetFileToolEvent event) {
        out.println("Finished in " + TextUtil.formatTimeLength(gft.getTimeEstimator().getTimeRunning()));
    }

    /**
     * <p>Notification that a directory download has failed.</p>
     * @param event
     */
    public void failedDirectory(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        out.println("Failed to download " + event.getFileHash());
    }

    /**
     *
     * @return
     */
    public String getTimeLeftString() {
        String string = "time left: ";
        if (gft.getTimeEstimator().getHours() != 0) {
            string = string + gft.getTimeEstimator().getHours() + "h ";
        }
        if (gft.getTimeEstimator().getHours() != 0 || gft.getTimeEstimator().getMinutes() != 0) {
            string = string + gft.getTimeEstimator().getMinutes() + "m ";
        }
        return string + gft.getTimeEstimator().getSeconds() + "s";
    }
}
