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
package org.tranche.add;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Collection;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.util.Text;

/**
 * <p>A helper class that implements AddFileTool listening methods and produces data suitable for dispaly on the command prompt.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class CommandLineAddFileToolListener implements AddFileToolListener {

    private static NumberFormat nf = NumberFormat.getInstance();
    private AddFileTool aft;
    private PrintStream out;
    private int chunkCount = 0;


    static {
        nf.setGroupingUsed(true);
        nf.setMaximumFractionDigits(3);
    }

    /**
     * 
     * @param aft
     * @param out
     */
    public CommandLineAddFileToolListener(AddFileTool aft, PrintStream out) {
        this.aft = aft;
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
     * <p>Notification that a meta data chunk is starting to be uploaded.</p>
     * @param event
     */
    public void startedMetaData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a meta data chunk is trying to be uploaded to a server.</p>
     * @param event
     */
    public void tryingMetaData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a meta data chunk has been uploaded to a server.</p>
     * @param event
     */
    public void uploadedMetaData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a meta data chunk could not be uploaded.</p>
     * @param event
     * @param exceptions
     */
    public void failedMetaData(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
    }

    /**
     * <p>Notification that a meta data chunk has been uploaded.</p>
     * @param event
     */
    public void finishedMetaData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk is about to be checked for on-line replications.</p>
     * @param event
     */
    public void startingData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk is starting to be uploaded.</p>
     * @param event
     */
    public void startedData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk is trying to be uploaded to a server.</p>
     * @param event
     */
    public void tryingData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk has been uploaded to a server.</p>
     * @param event
     */
    public void uploadedData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk could not be uploaded.</p>
     * @param event
     * @param exceptions
     */
    public void failedData(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
    }

    /**
     * <p>Notification that a data chunk has been uploaded.</p>
     * @param event
     */
    public void finishedData(AddFileToolEvent event) {
        // only show for every 10 chunks
        if (chunkCount == 10) {
            chunkCount = 0;
            out.println(nf.format(aft.getTimeEstimator().getPercentDone()) + "% done, " + getTimeLeftString());
        } else {
            chunkCount++;
            return;
        }
    }

    /**
     * <p>Notification that a file upload has started.</p>
     * @param event
     */
    public void startedFile(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a file upload has finished.</p>
     * @param event
     */
    public void finishedFile(AddFileToolEvent event) {
        if (event.getFileName() != null) {
            out.println(nf.format(aft.getTimeEstimator().getPercentDone()) + "% done, " + getTimeLeftString() + ", uploaded: " + event.getFileName());
        }
    }

    /**
     * <p>Notification that a file upload has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedFile(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        out.println("Failed to add: " + event.getFileName());
        for (PropagationExceptionWrapper e : exceptions) {
            out.println(e.exception.getClass().getName() + ": " + e.exception.getMessage());
        }
    }

    /**
     * <p>Notification that a directory upload has started.</p>
     * @param event
     */
    public void startedDirectory(AddFileToolEvent event) {
        out.println("Uploading " + event.getFile().getAbsolutePath() + " (" + Text.getFormattedBytes(aft.getTimeEstimator().getTotalBytes()) + ")");
    }

    /**
     * <p>Notification that a directory upload has finished.</p>
     * @param event
     */
    public void finishedDirectory(AddFileToolEvent event) {
        out.println("Finished in " + Text.getPrettyEllapsedTimeString(aft.getTimeEstimator().getTimeRunning()));
    }

    /**
     * <p>Notification that a directory upload has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedDirectory(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        out.println("Failed to upload " + event.getFile().getAbsolutePath());
    }

    /**
     * 
     * @return
     */
    public String getTimeLeftString() {
        return "time left: " + aft.getTimeEstimator().getTimeLeftString();
    }
}
