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
package org.tranche.server.logs;

import java.util.*;

/**
 * Keeps track of size. A simple class.
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class LogEntries {
    
    private List<LogEntry> entries;
    private long length;
    
    /**
     * Create and initialize structures to track log entries.
     */
    public LogEntries() {
        entries = new LinkedList();
    }
    
    /**
     * Add a log entry for tracking.
     * @param entry
     */
    public void add(LogEntry entry) {
        length += entry.length();
        this.entries.add(entry);
    }
    
    /**
     * Number of entries being tracked.
     * @return
     */
    public int size() {
        return entries.size();
    }
    
    /**
     * Length in bytes of entries being tracked.
     * @return
     */
    public long lengthInBytes() {
        return this.length;
    }
    
    /**
     * Return an iterator pointing to the beginning of list of entries being tracked.
     * @return
     */
    public Iterator<LogEntry> iterator() {
        return this.entries.iterator();
    }
    
    /**
     * Clear the entire list of log entries being tracked.
     */
    public void clear() {
        this.entries.clear();
    }
}
