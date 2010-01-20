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
package org.tranche.network;

/**
 * <p>Blank implementation of a listener for the status table modifications.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class StatusTableAdapter implements StatusTableListener {

    /**
     * <p>Event fired when a row is added to the status table.</p>
     * <p>Blank implementation.</p>
     * @param event Holds the information describing the event.
     */
    public void rowsAdded(StatusTableEvent event) {
    }

    /**
     * <p>Event fired when a row in the status table is updated.</p>
     * <p>Blank implementation.</p>
     * @param event Holds the information describing the event.
     */
    public void rowsUpdated(StatusTableEvent event) {
    }

    /**
     * <p>Event fired when a row is removed from the status table.</p>
     * <p>Blank implementation.</p>
     * @param event Holds the information describing the event.
     */
    public void rowsRemoved(StatusTableEvent event) {
    }
}
