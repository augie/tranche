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

import java.util.Collection;
import org.tranche.server.PropagationExceptionWrapper;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public interface GetFileToolListener {

    /**
     * <p>Notification of any other event occurence.</p>
     * @param msg
     */
    public void message(String msg);

    /**
     * <p>Notification that a meta data chunk is starting to be downloaded.</p>
     * @param event
     */
    public void startedMetaData(GetFileToolEvent event);

    /**
     * <p>Notification that a meta data chunk is trying to be downloaded from a server.</p>
     * @param event
     */
    public void tryingMetaData(GetFileToolEvent event);

    /**
     * <p>Notification that a meta data chunk could not be downloaded.</p>
     * @param event
     * @param exceptions
     */
    public void failedMetaData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions);

    /**
     * <p>Notification that a meta data chunk has been downloaded.</p>
     * @param event
     */
    public void finishedMetaData(GetFileToolEvent event);

    /**
     * <p>Notificatin that a data chunk is starting to be downloaded.</p>
     * @param event
     */
    public void startedData(GetFileToolEvent event);

    /**
     * <p>Notification that a data chunk is trying to be downloaded from a server.</p>
     * @param event
     */
    public void tryingData(GetFileToolEvent event);

    /**
     * <p>Notification that a data chunk could not be downloaded.</p>
     * @param event
     * @param exceptions
     */
    public void failedData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions);

    /**
     * <p>Notification that a data chunk has been downloaded.</p>
     * @param event
     */
    public void finishedData(GetFileToolEvent event);

    /**
     * <p>Notification that a file is starting to be downloaded.</p>
     * @param event
     */
    public void startingFile(GetFileToolEvent event);

    /**
     * <p>Notification that a file download has started.</p>
     * @param event
     */
    public void startedFile(GetFileToolEvent event);

    /**
     * <p>Notification that a file downloaded has been skipped.</p>
     * @param event
     */
    public void skippedFile(GetFileToolEvent event);

    /**
     * <p>Notification that a file download has finished.</p>
     * @param event
     */
    public void finishedFile(GetFileToolEvent event);

    /**
     * <p>Notification that a file download has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedFile(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions);

    /**
     * <p>Notification that a directory download is starting.</p>
     * @param event
     */
    public void startingDirectory(GetFileToolEvent event);

    /**
     * <p>Notification that a directory download has started.</p>
     * @param event
     */
    public void startedDirectory(GetFileToolEvent event);

    /**
     * <p>Notification that a directory download has finished.</p>
     * @param event
     */
    public void finishedDirectory(GetFileToolEvent event);

    /**
     * <p>Notification that a directory download has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedDirectory(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions);
}
