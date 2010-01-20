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
package org.tranche.remote;

import org.tranche.servers.*;

/**
 * <p>A listener for monitoring client-server communication.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public interface RemoteTrancheServerListener {

    /**
     * <p>A notification that a server has been connected to.</p>
     * @param se
     */
    public void serverConnect(ServerEvent se);

    /**
     * <p>A notification that a server has been banned.</p>
     * @param se
     */
    public void serverBanned(ServerEvent se);

    /**
     * <p>A notification that a server has been unbanned.</p>
     * @param se
     */
    public void serverUnbanned(ServerEvent se);

    /**
     * <p>A notification that an outbound message has been created.</p>
     * @param smue
     */
    public void upMessageCreated(ServerMessageUpEvent smue);

    /**
     * <p>A notification that an outbound message has started to be sent.</p>
     * @param smue
     */
    public void upMessageStarted(ServerMessageUpEvent smue);

    /**
     * <p>A notification that an outbound message has been sent.</p>
     * @param smue
     */
    public void upMessageSent(ServerMessageUpEvent smue);

    /**
     * <p>A notification that an outbound message has failed.</p>
     * @param smue
     */
    public void upMessageFailed(ServerMessageUpEvent smue);

    /**
     * <p>A notification that an inbound message has been started.</p>
     * @param smde
     */
    public void downMessageStarted(ServerMessageDownEvent smde);

    /**
     * <p>A notification that an inbound message is being downloaded.</p>
     * @param smde
     */
    public void downMessageProgress(ServerMessageDownEvent smde);

    /**
     * <p>A notification that an inbound message has been completed.</p>
     * @param smde
     */
    public void downMessageCompleted(ServerMessageDownEvent smde);

    /**
     * <p>A notification that an inbound message has failed.</p>
     * @param smde
     */
    public void downMessageFailed(ServerMessageDownEvent smde);

    /**
     * <p>A notification that a server request has been created.</p>
     * @param sce
     */
    public void requestCreated(ServerCallbackEvent sce);

    /**
     * <p>A notification that a server request has been fulfilled by the server.</p>
     * @param sce
     */
    public void requestFulfilled(ServerCallbackEvent sce);

    /**
     * <p>A notification that a server request has failed.</p>
     * @param sce
     */
    public void requestFailed(ServerCallbackEvent sce);
}
