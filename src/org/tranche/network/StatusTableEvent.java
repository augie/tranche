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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.tranche.time.TimeUtil;

/**
 * <p>Wraps the information describing a status table modification.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class StatusTableEvent {

    /**
     * <p>The default value for whether this event affects the connectivity of the server.</p>
     */
    public static final boolean DEFAULT_AFFECTS_CONNECTIVITY = true;
    public static final boolean DEFAULT_AFFECTS_HASH_SPANS = false;
    // Map<Host, Affects Connectivity>
    private final Map<String, Boolean> affectsConnectivity;
    // Map<Host, Affects Hash Spans>
    private final Map<String, Boolean> affectsHashSpans;
    private final long timestamp = TimeUtil.getTrancheTimestamp();

    /**
     * @param host A host name
     * @param affectsConnectivity Whether this event affects the connectivity of the server
     * @param affectsHashSpans Whether this even affects the hash spans of the server
     */
    public StatusTableEvent(String host, boolean affectsConnectivity, boolean affectsHashSpans) {
        this.affectsConnectivity = new HashMap<String, Boolean>();
        this.affectsConnectivity.put(host, affectsConnectivity);
        this.affectsHashSpans = new HashMap<String, Boolean>();
        this.affectsHashSpans.put(host, affectsHashSpans);
    }

    /**
     * @param host A host name
     */
    public StatusTableEvent(String host) {
        this(host, DEFAULT_AFFECTS_CONNECTIVITY, DEFAULT_AFFECTS_HASH_SPANS);
    }

    /**
     * @param hosts Host names to whether the event affects the connectivity of the server
     * @param affectsHashSpans Host names to whether the event affects the hash spans of the server
     */
    public StatusTableEvent(Map<String, Boolean> hosts, Map<String, Boolean> affectsHashSpans) {
        this.affectsConnectivity = hosts;
        this.affectsHashSpans = affectsHashSpans;
    }

    /**
     * <p></p>
     * @param hosts
     */
    public StatusTableEvent(Collection<String> hosts) {
        this.affectsConnectivity = new HashMap<String, Boolean>();
        this.affectsHashSpans = new HashMap<String, Boolean>();
        for (String host : hosts) {
            this.affectsConnectivity.put(host, DEFAULT_AFFECTS_CONNECTIVITY);
            this.affectsHashSpans.put(host, DEFAULT_AFFECTS_HASH_SPANS);
        }
    }

    /**
     * <p>Returns a map of host names to whether the event affects the connectivity of the server.</p>
     * @return A map of host names to whether the event affects the connectivity of the server
     */
    public Set<String> getHosts() {
        return affectsConnectivity.keySet();
    }

    /**
     * <p></p>
     * @param host
     * @return
     */
    public boolean affectedConnectivity(String host) {
        return affectsConnectivity.get(host);
    }

    /**
     * <p></p>
     * @param host
     * @return
     */
    public boolean affectedHashSpans(String host) {
        return affectsHashSpans.get(host);
    }

    /**
     * <p>Returns the timestamp the event occurred.</p>
     * @return The timestamp the event occurred.
     */
    public long getTimestamp() {
        return timestamp;
    }
}
