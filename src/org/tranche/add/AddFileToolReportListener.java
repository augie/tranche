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

import java.util.Collection;
import org.tranche.server.PropagationExceptionWrapper;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class AddFileToolReportListener implements AddFileToolListener {

    private final AddFileToolReport report;

    /**
     *
     * @param report
     */
    public AddFileToolReportListener(AddFileToolReport report) {
        this.report = report;
    }

    /**
     * <p>Notification of any other event occurence.</p>
     * @param msg
     */
    public void message(String msg) {
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
        report.addFailureExceptions(exceptions);
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
        report.addFailureExceptions(exceptions);
    }

    /**
     * <p>Notification that a data chunk has been uploaded.</p>
     * @param event
     */
    public void finishedData(AddFileToolEvent event) {
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
        report.setOriginalFileCount(report.getOriginalFileCount() + 1);
        report.setOriginalBytesUploaded(report.getOriginalBytesUploaded() + event.getFile().length());
    }

    /**
     * <p>Notification that a file upload has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedFile(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        report.addFailureExceptions(exceptions);
    }

    /**
     * <p>Notification that a directory upload has started.</p>
     * @param event
     */
    public void startedDirectory(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a directory upload has finished.</p>
     * @param event
     */
    public void finishedDirectory(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a directory upload has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedDirectory(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        report.addFailureExceptions(exceptions);
    }
}
