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
package org.tranche.logs.activity;

import org.tranche.hash.BigHash;

/**
 *<p>Wrapper class for data that is held in the activity log file.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ActivityLogEntry {

    public final long timestamp;
    public final byte action;
    public final int signatureIndex;
    public final BigHash hash;

    public ActivityLogEntry(final long timestamp, final byte action, final int signatureIndex, final BigHash hash) {
        this.timestamp = timestamp;
        this.action = action;
        this.signatureIndex = signatureIndex;
        this.hash = hash;
    }

    @Override()
    public int hashCode() {
        // Use hash and simply increment by action. Highly unlikely any collision
        return hash.hashCode() + action;
    }
    
    @Override()
    public boolean equals(Object o) {
        if (o instanceof ActivityLogEntry) {
            ActivityLogEntry e = (ActivityLogEntry)o;
            
            return e.hash.equals(this.hash) && e.timestamp == this.timestamp && e.action == this.action && e.signatureIndex == this.signatureIndex;
        }
        return false;
    }
    
    @Override()
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("ActivityLogEntry: timestamp=" + timestamp + "; action=" + action + "; signatureIndex=" + signatureIndex + "; hash=" + hash);

        return buffer.toString();
    }
}
