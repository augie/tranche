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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tranche.ConfigureTranche;
import org.tranche.hash.span.HashSpanCollection;
import org.tranche.remote.RemoteUtil;
import org.tranche.time.TimeUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;

/**
 * <p>Represents a set of servers. Each server is represented by a row in the table.</p>
 * <p>The table keeps the servers sorted alphabetically by host name.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class StatusTable extends Object implements Serializable {

    private static boolean debug = false;
    /**
     * <p>The value for the first version of the object.</p>
     */
    public static final int VERSION_ONE = 1;
    /**
     * <p>The value for the latest version of the object.</p>
     */
    public static final int VERSION_LATEST = VERSION_ONE;
    /**
     * <p>For listening to the changes in the table.</p>
     */
    private final Set<StatusTableListener> listeners = new HashSet<StatusTableListener>();
    private int version = VERSION_LATEST;
    private final Map<String, StatusTableRow> map = new HashMap<String, StatusTableRow>();
    private final List<StatusTableRow> list = new ArrayList<StatusTableRow>();
    private final List<String> hostList = new ArrayList<String>();
    private final List<String> urlList = new ArrayList<String>();
    private final Object modLock = new Object();

    /**
     * <p>Default constructor.</p>
     */
    public StatusTable() {
    }

    /**
     * @param rows A collection of rows
     */
    public StatusTable(Collection<StatusTableRow> rows) {
        setVersion(VERSION_ONE);
        setRows(rows);
    }

    /**
     * <p>Deserializes the table based on the given stream.</p>
     * @param in The stream containing a serialized row
     * @throws java.io.IOException
     */
    public StatusTable(InputStream in) throws IOException {
        deserialize(in);
    }

    /**
     * <p>Gets the version.</p>
     * @return The version
     */
    public int getVersion() {
        return version;
    }

    /**
     * <p>Sets the version.</p>
     * @param version The new version
     */
    protected void setVersion(int version) {
        this.version = version;
    }

    /**
     * <p>Removes all the rows from the table.</p>
     */
    protected void clear() {
        synchronized (modLock) {
            synchronized (map) {
                map.clear();
            }
            synchronized (list) {
                list.clear();
            }
            synchronized (hostList) {
                hostList.clear();
            }
            synchronized (urlList) {
                urlList.clear();
            }
        }
    }

    /**
     * <p>Removes rows that are offline, not connected, and haven't been updated in <i>x</i> number of milliseconds.</p>
     */
    protected void removeDefunctRows() {
        long threshold = ConfigureTranche.getLong(ConfigureTranche.PROP_DEFUNCT_SERVER_THRESHOLD);
        Set<String> toRemove = new HashSet<String>();
        for (StatusTableRow row : getRows()) {
            if (!row.isOnline() && !row.isCore() && !ConnectionUtil.isConnected(row.getHost()) && TimeUtil.getTrancheTimestamp() - row.getUpdateTimestamp() > threshold) {
                toRemove.add(row.getHost());
            }
        }
        removeRows(toRemove);
    }

    /**
     * <p>Sets the rows of the table. The rows are sorted alphabetically by host name.</p>
     * <p>Will overwrite rows of the same host names.</p>
     * <p>Does not clear the previous rows.</p>
     * @param rows A collection of rows
     */
    protected void setRows(Collection<StatusTableRow> rows) {
        Set<String> addedHosts = new HashSet<String>();
        Map<String, Boolean> updatedHosts = new HashMap<String, Boolean>();
        Map<String, Boolean> updatedHashSpans = new HashMap<String, Boolean>();
        for (StatusTableRow row : rows) {
            boolean isNew = !contains(row.getHost());
            // the stored row is more recent than the one being given
            if (!isNew && getRow(row.getHost()).getUpdateTimestamp() > row.getUpdateTimestamp()) {
                continue;
            }
            boolean isUpdated = !isNew && !getRow(row.getHost()).equals(row);
            boolean affectsConnectivity = isUpdated && (!getRow(row.getHost()).getURL().equals(row.getURL()) || getRow(row.getHost()).isOnline() != row.isOnline());
            boolean affectsHashSpans = isUpdated && !HashSpanCollection.areEqual(getRow(row.getHost()).getHashSpans(), row.getHashSpans());
            synchronized (modLock) {
                synchronized (map) {
                    map.put(row.getHost(), row);
                }
                if (isNew) {
                    int index = -1;
                    synchronized (hostList) {
                        hostList.add(row.getHost());
                        Collections.sort(hostList);
                        index = hostList.indexOf(row.getHost());
                    }
                    synchronized (list) {
                        list.add(index, row);
                    }
                    synchronized (urlList) {
                        urlList.add(index, row.getURL());
                    }
                    addedHosts.add(row.getHost());
                    debugOut("Added server: " + row.getHost());
                } else if (isUpdated) {
                    // update the list
                    int index = -1;
                    synchronized (hostList) {
                        index = hostList.indexOf(row.getHost());
                    }
                    synchronized (list) {
                        list.remove(index);
                        list.add(index, row);
                    }
                    synchronized (urlList) {
                        urlList.remove(index);
                        urlList.add(index, row.getURL());
                    }
                    updatedHosts.put(row.getHost(), affectsConnectivity);
                    updatedHashSpans.put(row.getHost(), affectsHashSpans);
                    debugOut("Updated server: " + row.getHost() + " (affects connectivity? " + affectsConnectivity + ", affects hash spans? " + affectsHashSpans + ")");
                }
            }
        }
        // added servers
        if (!addedHosts.isEmpty()) {
            fireRowsAdded(addedHosts);
        }
        // updated servers
        if (!updatedHosts.isEmpty()) {
            fireRowsUpdated(updatedHosts, updatedHashSpans);
        }
    }

    /**
     * <p>Sets the row in the table. The rows are sorted alphabetically by host name.</p>
     * <p>Will overwrite rows of the same host names.</p>
     * <p>If setting many rows, use setRows(Collection<StatusTableRow>) instead.</p>
     * @param row A row
     */
    protected void setRow(StatusTableRow row) {
        boolean isNew = !contains(row.getHost());
        // the stored row is more recent than the one being given
        if (!isNew && getRow(row.getHost()).isOnline() == row.isOnline() && getRow(row.getHost()).getUpdateTimestamp() > row.getUpdateTimestamp()) {
            return;
        }
        boolean isUpdated = !isNew && !getRow(row.getHost()).equals(row);
        boolean affectsConnectivity = isUpdated && (!getRow(row.getHost()).getURL().equals(row.getURL()) || getRow(row.getHost()).isOnline() != row.isOnline());
        boolean affectsHashSpans = isUpdated && !HashSpanCollection.areEqual(getRow(row.getHost()).getHashSpans(), row.getHashSpans());
        synchronized (modLock) {
            synchronized (map) {
                map.put(row.getHost(), row);
            }
            if (isNew) {
                int index = -1;
                synchronized (hostList) {
                    hostList.add(row.getHost());
                    Collections.sort(hostList);
                    index = hostList.indexOf(row.getHost());
                }
                synchronized (list) {
                    list.add(index, row);
                }
                synchronized (urlList) {
                    urlList.add(index, row.getURL());
                }
                debugOut("Added server: " + row.getHost());
            } else if (isUpdated) {
                // update the list
                int index = -1;
                synchronized (hostList) {
                    index = hostList.indexOf(row.getHost());
                }
                synchronized (list) {
                    list.remove(index);
                    list.add(index, row);
                }
                synchronized (urlList) {
                    urlList.remove(index);
                    urlList.add(index, row.getURL());
                }
                debugOut("Updated server: " + row.getHost() + " (affects connectivity? " + affectsConnectivity + ", affects hash spans? " + affectsHashSpans + ")");
            }
        }
        // fire events outside modification lock
        if (isNew) {
            fireRowAdded(row.getHost());
        } else if (isUpdated) {
            fireRowUpdated(row.getHost(), affectsConnectivity, affectsHashSpans);
        }
    }

    /**
     * <p>Removes the rows with host names within the given collection</p>
     * @param hosts A collection of host names
     */
    protected void removeRows(Collection<String> hosts) {
        Set<String> removedHosts = new HashSet<String>();
        for (String host : hosts) {
            if (!contains(host)) {
                continue;
            }
            StatusTableRow str = null;
            synchronized (modLock) {
                synchronized (map) {
                    str = map.remove(host);
                }
                if (str != null) {
                    synchronized (list) {
                        list.remove(str);
                    }
                    synchronized (hostList) {
                        hostList.remove(host);
                    }
                    synchronized (urlList) {
                        urlList.remove(str.getURL());
                    }
                    removedHosts.add(host);
                }
            }
        }
        if (!removedHosts.isEmpty()) {
            fireRowsRemoved(removedHosts);
        }
    }

    /**
     * <p>Removes the rows with the given host name.</p>
     * <p>Removing many rows, use removeRows(Collection<String>) instead.</p>
     * @param host
     */
    protected void removeRow(String host) {
        if (!contains(host)) {
            return;
        }
        StatusTableRow str = null;
        synchronized (modLock) {
            synchronized (map) {
                str = map.remove(host);
            }
            if (str != null) {
                synchronized (list) {
                    list.remove(str);
                }
                synchronized (hostList) {
                    hostList.remove(host);
                }
                synchronized (urlList) {
                    urlList.remove(str.getURL());
                }
            }
        }
        fireRowsRemoved(host);
    }

    /**
     * <p>Gets the number of rows in the table.</p>
     * @return The number of rows in the table
     */
    public int size() {
        synchronized (list) {
            return list.size();
        }
    }

    /**
     * <p>Returns whether there are no rows in the table.</p>
     * @return Whether there are no rows in the table.
     */
    public boolean isEmpty() {
        synchronized (list) {
            return list.isEmpty();
        }
    }

    /**
     * <p>Returns the rows in the table. They will be sorted in alphabetical order by host name.</p>
     * @return The rows in the table.
     */
    public List<StatusTableRow> getRows() {
        List<StatusTableRow> rows = new LinkedList<StatusTableRow>();
        synchronized (list) {
            rows.addAll(list);
        }
        return Collections.unmodifiableList(rows);
    }

    /**
     * <p>Gets all of the rows that have host names between startHost (inclusive) and endHost (exclusive.)</p>
     * @param startHost Return rows that have host names alphabetically after or equal to this host name.
     * @param endHost Return rows that have host names alphabetically before this host name.
     * @return A list of objects that represent server status.
     */
    public List<StatusTableRow> getRows(String startHost, String endHost) {
        List<StatusTableRow> returnList = new LinkedList<StatusTableRow>();
        int startIndex = -1, endIndex = 0;
        for (int i = 0; i < size(); i++) {
            if (startIndex == -1 && getHost(i).compareTo(startHost) >= 0) {
                startIndex = i;
            }
            if (getHost(i).compareTo(endHost) <= 0) {
                endIndex = i;
            }
        }
        if (startIndex == -1) {
            startIndex = 0;
        }
        if (startIndex >= endIndex) {
            for (int i = startIndex; i < size(); i++) {
                returnList.add(getRow(getHost(i)));
            }
            for (int i = 0; i < endIndex; i++) {
                returnList.add(getRow(getHost(i)));
            }
        } else if (startIndex < endIndex) {
            for (int i = startIndex; i < endIndex; i++) {
                returnList.add(getRow(getHost(i)));
            }
        }
        return Collections.unmodifiableList(returnList);
    }

    /**
     * <p>Returns a status table containing the rows between the two given host names.</p>
     * @param startHost A host name
     * @param endHost A host name
     * @return A status table
     */
    public StatusTable getStatus(String startHost, String endHost) {
        return new StatusTable(getRows(startHost, endHost));
    }

    /**
     * <p>Gets the row with the given host name.</p>
     * @param host A host name.
     * @return The row with the given host name. NULL if the row does not exist.
     */
    public StatusTableRow getRow(String host) {
        synchronized (map) {
            return map.get(host);
        }
    }

    /**
     * <p>Returns whether a row with the given host name is contained within the table.</p>
     * @param host A host name
     * @return Whether a row with the given host name is contained within the table
     */
    public boolean contains(String host) {
        synchronized (map) {
            return map.containsKey(host);
        }
    }

    /**
     * 
     * @param i
     * @return
     */
    public String getHost(int i) {
        synchronized (hostList) {
            return hostList.get(i);
        }
    }

    /**
     * <p>Returns the list of hosts.</p>
     * @return The list of hosts
     */
    public List<String> getHosts() {
        List<String> hosts = new LinkedList<String>();
        synchronized (hostList) {
            hosts.addAll(hostList);
        }
        return Collections.unmodifiableList(hosts);
    }

    /**
     * <p>Returns the list of URLs.</p>
     * @return The list of URLs
     */
    public List<String> getURLs() {
        List<String> list = new LinkedList<String>();
        synchronized (urlList) {
            list.addAll(urlList);
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * 
     * @return
     * @throws java.io.IOException
     */
    public byte[] toByteArray() throws IOException {
        return toByteArray(StatusTable.VERSION_LATEST, StatusTableRow.VERSION_LATEST);
    }

    /**
     * 
     * @return
     * @throws java.io.IOException
     */
    public byte[] toByteArray(int tableVersion, int rowVersion) throws IOException {
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            serialize(tableVersion, rowVersion, baos);
            return baos.toByteArray();
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    /**
     * <p>Outputs the values of this row to the output stream.</p>
     * @param out The output stream.
     * @throws java.io.IOException
     */
    public void serialize(OutputStream out) throws IOException {
        serialize(version, StatusTableRow.VERSION_LATEST, out);
    }

    /**
     * <p>Write to the output stream in the given table and row versions.</p>
     * @param out The output stream
     * @throws java.io.IOException
     */
    public void serialize(int tableVersion, int rowVersion, OutputStream out) throws IOException {
        RemoteUtil.writeInt(version, out);
        if (version == VERSION_ONE) {
            serializeVersionOne(rowVersion, out);
        } else {
            throw new IOException("Unrecognized version");
        }
    }

    /**
     * <p>Outputs the values in the structure defined by version one to the given output stream.</p>
     * @param out The output stream
     * @throws java.io.IOException
     */
    protected void serializeVersionOne(int rowVersion, OutputStream out) throws IOException {
        RemoteUtil.writeInt(size(), out);
        for (StatusTableRow str : getRows()) {
            str.serialize(rowVersion, out);
        }
    }

    /**
     * <p>Sets the table based on the given serialized table.</p>
     * @param in An input stream containing a serialized table
     * @throws IOException
     */
    protected void deserialize(InputStream in) throws IOException {
        setVersion(RemoteUtil.readInt(in));
        if (getVersion() == VERSION_ONE) {
            deserializeVersionOne(in);
        } else {
            throw new IOException("Unrecognized version: " + getVersion());
        }
    }

    /**
     * <p>Sets the values of the row based the given version one serialized table.</p>
     * @param in An input stream containing a version one serialized table
     * @throws java.io.IOException
     */
    protected void deserializeVersionOne(InputStream in) throws IOException {
        int rowCount = RemoteUtil.readInt(in);
        Set<StatusTableRow> rows = new HashSet<StatusTableRow>();
        for (int i = 0; i < rowCount; i++) {
            rows.add(new StatusTableRow(in));
        }
        setRows(rows);
    }

    /**
     * <p>Creates a duplicate table.</p>
     * @return A duplicate table
     */
    @Override
    public StatusTable clone() {
        return new StatusTable(getRows());
    }

    /**
     * <p>Evaluates the given object against this one.</p>
     * @param o A StatusTable object
     * @return Whether the given object and this one are equivalent.
     */
    @Override
    public boolean equals(Object o) {
        StatusTable st = (StatusTable) o;
        return version == st.getVersion() &&
                StatusTableRow.areEqual(getRows(), st.getRows());
    }

    /**
     * <p>Prints out the values of the attributes in the row.</p>
     * @return The printed value of the row
     */
    @Override
    public String toString() {
        String out = "Network Status Table (" + size() + ")";
        for (StatusTableRow row : getRows()) {
            out = out + "\n  " + row.toString();
        }
        return out;
    }

    /**
     * <p>Adds the given listener to the list of listeners.</p>
     * @param l A listener
     */
    public void addListener(StatusTableListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    /**
     * <p>Removes the given listener from the list of listeners.</p>
     * @param l A listener
     */
    public void removeListener(StatusTableListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    /**
     *
     * @return
     */
    public Collection<StatusTableListener> getListeners() {
        List<StatusTableListener> list = new LinkedList<StatusTableListener>();
        synchronized (listeners) {
            list.addAll(listeners);
        }
        return Collections.unmodifiableCollection(list);
    }

    /**
     * <p>Fires the event for having added rows.</p>
     * @param hosts A collection of host names
     */
    private void fireRowsAdded(Collection<String> hosts) {
        StatusTableEvent spe = new StatusTableEvent(hosts);
        for (StatusTableListener listener : getListeners()) {
            try {
                listener.rowsAdded(spe);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Fires the event for having added a row.</p>
     * @param host A host name
     */
    private void fireRowAdded(String host) {
        StatusTableEvent spe = new StatusTableEvent(host);
        for (StatusTableListener listener : getListeners()) {
            try {
                listener.rowsAdded(spe);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Fires the event for having added a collection of rows.</p>
     * @param hosts A collection of host names
     */
    private void fireRowsRemoved(Collection<String> hosts) {
        StatusTableEvent spe = new StatusTableEvent(hosts);
        for (StatusTableListener listener : getListeners()) {
            try {
                listener.rowsRemoved(spe);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Fires the event for having added a row.</p>
     * @param host A host name
     */
    private void fireRowsRemoved(String host) {
        StatusTableEvent spe = new StatusTableEvent(host);
        for (StatusTableListener listener : getListeners()) {
            try {
                listener.rowsRemoved(spe);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Fires an event for having updated a map of hosts.</p>
     * @param hosts A map of hosts to whether connectivity was affected
     * @param affectsHashSpans A map of hosts to whether hash spans were affected
     */
    public void fireRowsUpdated(Map<String, Boolean> hosts, Map<String, Boolean> affectsHashSpans) {
        StatusTableEvent spe = new StatusTableEvent(hosts, affectsHashSpans);
        for (StatusTableListener l : getListeners()) {
            try {
                l.rowsUpdated(spe);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Fires an event for having updated a row.</p>
     * @param host The host name
     * @param affectsConnectivity Whether the update changes the connectivity
     */
    public void fireRowUpdated(String host, boolean affectsConnectivity, boolean affectsHashSpans) {
        StatusTableEvent spe = new StatusTableEvent(host, affectsConnectivity, affectsHashSpans);
        for (StatusTableListener l : getListeners()) {
            try {
                l.rowsUpdated(spe);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static void setDebug(boolean debug) {
        StatusTable.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(StatusTable.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param line
     */
    private static void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
