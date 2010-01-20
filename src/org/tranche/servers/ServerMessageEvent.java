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
package org.tranche.servers;

import org.tranche.util.*;

/**
 * <p>Used in server listeners to describe the communications between client and server.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServerMessageEvent extends ServerEvent {

    private long callbackId = -1;
    
    /**
     * 
     * @param url
     * @param callbackId
     */
    public ServerMessageEvent(String url, long callbackId) {
        super(url, ServerEvent.MESSAGE);
        this.callbackId = callbackId;
    }
    
    /**
     * <p>Returns the callback ID assigned to this message.</p>
     * @return
     */
    public long getCallbackId() {
        return callbackId;
    }
    
    /**
     * <p>Returns a human-readable message that describes this event.</p>
     * @return
     */
    @Override()
    public String toString() {
        return "Callback #: " + callbackId + ";  Date: " + Text.getFormattedDate(getTimestamp()) + ";";
    }
    
}
