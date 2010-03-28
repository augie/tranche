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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.ConfigureTranche;
import org.tranche.configuration.Configuration;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.AbstractHashSpan;
import org.tranche.hash.span.HashSpan;
import org.tranche.hash.span.HashSpanCollection;
import org.tranche.remote.RemoteUtil;
import org.tranche.server.Server;
import org.tranche.time.TimeUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;

/**
 * <p>A row in the status table. Each row represents a server of the network.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class StatusTableRow implements Serializable {

    private static boolean debug = false;
    /**
     * <p>The value for the first version of the object.</p>
     */
    public static final int VERSION_ONE = 1;
    /**
     * <p></p>
     */
    public static final int VERSION_TWO = 2;
    /**
     * <p>The value for the latest version of the object.</p>
     */
    public static final int VERSION_LATEST = VERSION_TWO;
    /**
     * <p>The location of the SSL bit in the set of flags.</p>
     */
    public static final int SSL_BIT = (int) Math.pow(2, 0);
    /**
     * <p>The location of the Online bit in the set of flags.</p>
     */
    public static final int ONLINE_BIT = (int) Math.pow(2, 1);
    /**
     * <p>The location of the "Can Read" bit in the set of flags.</p>
     */
    public static final int CAN_READ_BIT = (int) Math.pow(2, 2);
    /**
     * <p>The location of the "Can Write" bit in the set of flags.</p>
     */
    public static final int CAN_WRITE_BIT = (int) Math.pow(2, 3);
    /**
     * <p>The location of the "Data Store" bit in the set of flags.</p>
     */
    public static final int DATA_STORE_BIT = (int) Math.pow(2, 4);
    /**
     * <p>The default value for the flags.</p>
     */
    public static final long DEFAULT_FLAGS = CAN_READ_BIT | CAN_WRITE_BIT | DATA_STORE_BIT;
    /**
     * <p>The default set of hash spans.</p>
     */
    public static final Collection<HashSpan> DEFAULT_HASH_SPANS = HashSpan.FULL_SET;
    /**
     * <p>The default set of target hash spans.</p>
     */
    public static final Collection<HashSpan> DEFAULT_TARGET_HASH_SPANS = HashSpan.FULL_SET;
    /**
     * <p>The maximum length of the name.</p>
     */
    public static final int LENGTH_MAX_NAME = 100;
    /**
     * <p>The maximum length of the group name.</p>
     */
    public static final int LENGTH_MAX_GROUP = 100;
    private final Set<HashSpan> hashSpans = new HashSet<HashSpan>(DEFAULT_HASH_SPANS), targetHashSpans = new HashSet<HashSpan>(DEFAULT_TARGET_HASH_SPANS);
    private String host, name = "", group = "";
    private int version = VERSION_LATEST, port = ConfigureTranche.getInt(ConfigureTranche.PROP_SERVER_PORT);
    private long flags = DEFAULT_FLAGS, updateTimestamp = 0, responseTimestamp = 0;

    /**
     * <p>Starts the row with a host name. All other values are the defaults.</p>
     * @param host Host name.
     */
    public StatusTableRow(String host) {
        setHost(host);
    }

    /**
     * <p>Deserializes the row based on the given stream.</p>
     * @param in The stream containing a serialized row
     * @throws java.io.IOException
     */
    public StatusTableRow(InputStream in) throws IOException {
        deserialize(in);
    }

    /**
     * <p>Constructs a row based on a local server.</p>
     * @param server A server
     */
    public StatusTableRow(Server server) {
        update(server);
    }

    /**
     * <p>Starts the row with a host name.</p>
     * @param host
     * @param port
     * @param isSSL
     */
    public StatusTableRow(String host, int port, boolean isSSL) {
        setHost(host);
        setPort(port);
        setIsSSL(isSSL);
    }

    /**
     * <p>Starts the row with a host name.</p>
     * @param host
     * @param port
     * @param isSSL
     * @param isOnline
     */
    public StatusTableRow(String host, int port, boolean isSSL, boolean isOnline) {
        setHost(host);
        setPort(port);
        setIsSSL(isSSL);
        setIsOnline(isOnline);
    }

    /**
     * <p>Updates the information in the row based on the given configuration.</p>
     * <p>This is updated by the network status table and related utilities, as well as by tests.</p>
     * @param config A configuration
     */
    public void update(Configuration config) {
        setName(config.getName());
        setGroup(config.getGroup());
        setIsReadable(config.canRead());
        setIsWritable(config.canWrite());
        // TODO after creating a routing server, change this
        setIsDataStore(true);
        setHashSpans(config.getHashSpans());
        setTargetHashSpans(config.getTargetHashSpans());
        setUpdateTimestamp(TimeUtil.getTrancheTimestamp());
    }

    /**
     * <p>Updates the information in the row based on the given local server.</p>
     * @param server A server
     */
    protected void update(Server server) {
        setHost(server.getHostName());
        setPort(server.getPort());
        setIsSSL(server.isSSL());
        setIsOnline(server.isAlive());
        // TODO after creating a routing server, change this
        setIsDataStore(true);
        // some info from the configuration
        try {
            if (server.getTrancheServer() instanceof FlatFileTrancheServer) {
                update(((FlatFileTrancheServer) server.getTrancheServer()).getConfiguration());
            } else {
                update(IOUtil.getConfiguration(server.getTrancheServer(), SecurityUtil.getAnonymousCertificate(), SecurityUtil.getAnonymousKey()));
            }
        } catch (Exception e) {
            debugErr(e);
        }
        setUpdateTimestamp(TimeUtil.getTrancheTimestamp());
    }

    /**
     *
     * @param row
     * @return Whether any information was changed.
     */
    protected boolean update(StatusTableRow row) {
        if (row == null || !row.getHost().equals(getHost())) {
            return false;
        }
        boolean changed = false;
        if (row.getUpdateTimestamp() > getUpdateTimestamp()) {
            setUpdateTimestamp(row.getUpdateTimestamp());
            changed = setPort(row.getPort()) || changed;
            changed = setIsSSL(row.isSSL()) || changed;
            changed = setIsReadable(row.isReadable()) || changed;
            changed = setIsWritable(row.isWritable()) || changed;
            changed = setIsDataStore(row.isDataStore()) || changed;
            changed = setName(row.getName()) || changed;
            changed = setGroup(row.getGroup()) || changed;
            changed = setHashSpans(row.getHashSpans()) || changed;
            changed = setTargetHashSpans(row.getTargetHashSpans()) || changed;
        }
        if (row.getResponseTimestamp() > getResponseTimestamp()) {
            setResponseTimestamp(row.getResponseTimestamp());
            changed = setIsOnline(row.isOnline()) || changed;
        } else if (!row.isOnline()) {
            changed = setIsOnline(row.isOnline()) || changed;
        }
        return changed;
    }

    /**
     * <p>Returns whether this row is a core server. A core server is defined a server that exists in the startup list of servers.</p>
     * @return Whether this row is a core server.
     */
    public boolean isCore() {
        return NetworkUtil.isStartupServer(host);
    }

    /**
     * <p>Returns whether this row is for the local server.</p>
     * @return Whether this row is for the local server
     */
    public boolean isLocalServer() {
        if (NetworkUtil.getLocalServerRow() == null) {
            return false;
        }
        return NetworkUtil.getLocalServerRow().getHost().equals(getHost());
    }

    /**
     * <p>Gets the URL of the row. Uses IOUtil.createURL(String, int, boolean).</p>
     * @return The URL
     */
    public String getURL() {
        return IOUtil.createURL(getHost(), getPort(), isSSL());
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
     * @return
     */
    protected boolean setVersion(int version) {
        if (this.version != version) {
            this.version = version;
            return true;
        }
        return false;
    }

    /**
     * <p>Gets the timestamp when the row's information was last updated.</p>
     * @return The timestamp when the row's information was last updated.
     */
    public long getUpdateTimestamp() {
        return updateTimestamp;
    }

    /**
     * <p>Sets the timestamp when the row's information was last updated.</p>
     * @param updateTimestamp The timestamp when the row's information was last updated.
     * @return
     */
    protected boolean setUpdateTimestamp(long updateTimestamp) {
        if (this.updateTimestamp != updateTimestamp) {
            this.updateTimestamp = updateTimestamp;
            return true;
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public long getResponseTimestamp() {
        return responseTimestamp;
    }

    /**
     *
     */
    public void responseReceived() {
        setResponseTimestamp(TimeUtil.getTrancheTimestamp());
        if ((ONLINE_BIT & getFlags()) == 0) {
            setIsOnline(true);
        }
    }

    /**
     * 
     * @param responseTimestamp
     * @return
     */
    protected boolean setResponseTimestamp(long responseTimestamp) {
        if (this.responseTimestamp != responseTimestamp) {
            this.responseTimestamp = responseTimestamp;
            return true;
        }
        return false;
    }

    /**
     * <p>Returns the flags object.</p>
     * @return The flags
     */
    public long getFlags() {
        return flags;
    }

    /**
     * <p>Sets the flags for the row.</p>
     * @param flags The new flags
     * @return
     */
    private boolean setFlags(long flags) {
        if (this.flags != flags) {
            this.flags = flags;
            return true;
        }
        return false;
    }

    /**
     * <p>Gets the host name.</p>
     * @return The host name
     */
    public String getHost() {
        return host;
    }

    /**
     * <p>Sets the host name.</p>
     * @param host The new host name
     * @return
     */
    protected boolean setHost(String host) {
        host = host.trim().toLowerCase();
        if (this.host == null || !this.host.equals(host)) {
            this.host = host;
            return true;
        }
        return false;
    }

    /**
     * <p>Gets the port number.</p>
     * @return The port number
     */
    public int getPort() {
        return port;
    }

    /**
     * <p>Sets the port number.</p>
     * @param port The new port number
     * @return
     */
    protected boolean setPort(int port) {
        if (this.port != port) {
            this.port = port;
            return true;
        }
        return false;
    }

    /**
     * <p>Gets the name.</p>
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Sets the name.</p>
     * @param name The new name
     * @return
     */
    protected boolean setName(String name) {
        if (name == null) {
            name = "";
        }
        name = name.trim();
        if (name.length() > LENGTH_MAX_NAME) {
            name = name.substring(0, LENGTH_MAX_NAME);
        }
        if (this.name == null || !this.name.equals(name)) {
            this.name = name;
            return true;
        }
        return false;
    }

    /**
     * <p>Gets the group name.</p>
     * @return The group name
     */
    public String getGroup() {
        return group;
    }

    /**
     * <p>Sets the group name.</p>
     * @param group The new group name
     * @return
     */
    protected boolean setGroup(String group) {
        if (group == null) {
            group = "";
        }
        group = group.trim();
        if (group.length() > LENGTH_MAX_GROUP) {
            group = group.substring(0, LENGTH_MAX_GROUP);
        }
        if (this.group == null || !this.group.equals(group)) {
            this.group = group;
            return true;
        }
        return false;
    }

    /**
     * <p>Returns whether the server communicates over SSL.</p>
     * @return Whether the server communicates over SSL
     */
    public boolean isSSL() {
        return (SSL_BIT & getFlags()) != 0;
    }

    /**
     * <p>Sets whether the server communicates over SSL.</p>
     * @param ssl Whether the server communicates over SSL
     */
    protected boolean setIsSSL(boolean ssl) {
        if (((SSL_BIT & getFlags()) != 0) != ssl) {
            if (ssl) {
                setFlags(getFlags() | SSL_BIT);
            } else {
                setFlags(getFlags() & (DATA_STORE_BIT | ONLINE_BIT | CAN_READ_BIT | CAN_WRITE_BIT));
            }
            return true;
        }
        return false;
    }

    /**
     * <p>Returns whether the server is online.</p>
     * @return Whether the server is online.
     */
    public boolean isOnline() {
        return (ONLINE_BIT & getFlags()) != 0 && !NetworkUtil.isBannedServer(host);
    }

    /**
     * <p>Sets whether the server is online.</p>
     * @param online Whether the server is online.
     * @return
     */
    protected boolean setIsOnline(boolean online) {
        if (((ONLINE_BIT & getFlags()) != 0) != online) {
            if (online) {
                setFlags(getFlags() | ONLINE_BIT);
            } else if (!ConnectionUtil.isConnected(host)) {
                setFlags(getFlags() & (SSL_BIT | DATA_STORE_BIT | CAN_READ_BIT | CAN_WRITE_BIT));
            }
            return true;
        }
        return false;
    }

    /**
     * <p>Returns whether the server allows its data to be read.</p>
     * @return Whether the server allows its data to be read.
     */
    public boolean isReadable() {
        return (CAN_READ_BIT & getFlags()) != 0;
    }

    /**
     * <p>Sets whether the server allows its data to be read.</p>
     * @param readable Whether the server allows its data to be read.
     * @return
     */
    protected boolean setIsReadable(boolean readable) {
        if (((CAN_READ_BIT & getFlags()) != 0) != readable) {
            if (readable) {
                setFlags(getFlags() | CAN_READ_BIT);
            } else {
                setFlags(getFlags() & (SSL_BIT | ONLINE_BIT | DATA_STORE_BIT | CAN_WRITE_BIT));
            }
            return true;
        }
        return false;
    }

    /**
     * <p>Gets whether the server can be written to.</p>
     * @return Whether the server can be written to.
     */
    public boolean isWritable() {
        return (CAN_WRITE_BIT & getFlags()) != 0;
    }

    /**
     * <p>Sets whether the server can be written to.</p>
     * @param writable Whether the server can be written to.
     */
    protected boolean setIsWritable(boolean writable) {
        if (((CAN_WRITE_BIT & getFlags()) != 0) != writable) {
            if (writable) {
                setFlags(getFlags() | CAN_WRITE_BIT);
            } else {
                setFlags(getFlags() & (SSL_BIT | ONLINE_BIT | CAN_READ_BIT | DATA_STORE_BIT));
            }
            return true;
        }
        return false;
    }

    /**
     * <p>Returns whether the server stores data.</p>
     * @return Whether the server stores data.
     */
    public boolean isDataStore() {
        return (DATA_STORE_BIT & getFlags()) != 0;
    }

    /**
     * <p>Sets whether the server stores data.</p>
     * @param dataStore Whether the server stores data.
     * @return
     */
    protected boolean setIsDataStore(boolean dataStore) {
        if (((DATA_STORE_BIT & getFlags()) != 0) != dataStore) {
            if (dataStore) {
                setFlags(getFlags() | DATA_STORE_BIT);
            } else {
                setFlags(getFlags() & (SSL_BIT | ONLINE_BIT | CAN_READ_BIT | CAN_WRITE_BIT));
            }
            return true;
        }
        return false;
    }

    /**
     * <p>Gets the hash spans.</p>
     * @return The hash spans
     */
    public Collection<HashSpan> getHashSpans() {
        synchronized (hashSpans) {
            return new HashSet<HashSpan>(hashSpans);
        }
    }

    /**
     * <p>Sets the hash spans.</p>
     * @param hashSpans The hash spans
     * @return
     */
    protected boolean setHashSpans(Collection<HashSpan> hashSpans) {
        if (!HashSpanCollection.areEqual(getHashSpans(), hashSpans)) {
            synchronized (this.hashSpans) {
                this.hashSpans.clear();
                this.hashSpans.addAll(hashSpans);
            }
            return true;
        }
        return false;
    }

    /**
     * <p>Gets the target hash spans.</p>
     * @return The hash spans
     */
    public Collection<HashSpan> getTargetHashSpans() {
        synchronized (targetHashSpans) {
            return new HashSet<HashSpan>(targetHashSpans);
        }
    }

    /**
     * <p>Sets the target hash spans.</p>
     * @param hashSpans The hash spans
     * @return
     */
    protected boolean setTargetHashSpans(Collection<HashSpan> hashSpans) {
        if (!HashSpanCollection.areEqual(getTargetHashSpans(), hashSpans)) {
            synchronized (this.targetHashSpans) {
                this.targetHashSpans.clear();
                this.targetHashSpans.addAll(hashSpans);
            }
            return true;
        }
        return false;
    }

    /**
     *
     * @return
     * @throws java.io.IOException
     */
    public byte[] toByteArray(int version) throws IOException {
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            serialize(version, baos);
            return baos.toByteArray();
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    /**
     * <p>Outputs the values of this row in the structure defined by the given version to the given output stream.</p>
     * @param out The output stream
     * @throws IOException
     */
    public void serialize(int version, OutputStream out) throws IOException {
        if (version > VERSION_LATEST) {
            serialize(VERSION_LATEST, out);
            return;
        }
        RemoteUtil.writeInt(version, out);
        if (version == VERSION_ONE) {
            serializeVersionOne(out);
        } else if (version == VERSION_TWO) {
            serializeVersionTwo(out);
        } else {
            throw new IOException("Unrecognized version: " + version);
        }
    }

    /**
     * <p>Outputs the values of this row in the structure defined by version one to the given output stream.</p>
     * @param out The output stream
     * @throws java.io.IOException
     */
    private void serializeVersionOne(OutputStream out) throws IOException {
        RemoteUtil.writeLine(host, out);
        RemoteUtil.writeLine(name, out);
        RemoteUtil.writeLine(group, out);
        RemoteUtil.writeInt(port, out);
        RemoteUtil.writeLong(flags, out);
        Collection<HashSpan> hashSpans = getHashSpans();
        RemoteUtil.writeInt(hashSpans.size(), out);
        for (HashSpan hs : hashSpans) {
            RemoteUtil.writeBigHash(hs.getFirst(), out);
            RemoteUtil.writeBigHash(hs.getLast(), out);
        }
        // For now, only support one target hash span. Might change later.
        Collection<HashSpan> targetHashSpans = getTargetHashSpans();
        RemoteUtil.writeInt(targetHashSpans.size(), out);
        for (HashSpan targetHashSpan : targetHashSpans) {
            RemoteUtil.writeBigHash(targetHashSpan.getFirst(), out);
            RemoteUtil.writeBigHash(targetHashSpan.getLast(), out);
        }
        RemoteUtil.writeLong(updateTimestamp, out);
    }

    /**
     * 
     * @param out
     * @throws IOException
     */
    private void serializeVersionTwo(OutputStream out) throws IOException {
        IOUtil.writeString(host, out);
        IOUtil.writeString(name, out);
        IOUtil.writeString(group, out);
        IOUtil.writeInt(port, out);
        IOUtil.writeLong(flags, out);
        Collection<HashSpan> hashSpans = getHashSpans();
        IOUtil.writeInt(hashSpans.size(), out);
        for (HashSpan hs : hashSpans) {
            IOUtil.writeBigHash(hs.getFirst(), out);
            IOUtil.writeBigHash(hs.getLast(), out);
        }
        // For now, only support one target hash span. Might change later.
        Collection<HashSpan> targetHashSpans = getTargetHashSpans();
        IOUtil.writeInt(targetHashSpans.size(), out);
        for (HashSpan targetHashSpan : targetHashSpans) {
            IOUtil.writeBigHash(targetHashSpan.getFirst(), out);
            IOUtil.writeBigHash(targetHashSpan.getLast(), out);
        }
        IOUtil.writeLong(updateTimestamp, out);
        IOUtil.writeLong(responseTimestamp, out);
    }

    /**
     * <p>Sets the values of the row based on the given serialized row.</p>
     * @param in An input stream containing a serialized row
     * @throws IOException
     */
    protected void deserialize(InputStream in) throws IOException {
        setVersion(RemoteUtil.readInt(in));
        if (getVersion() == VERSION_ONE) {
            deserializeVersionOne(in);
        } else if (getVersion() == VERSION_TWO) {
            deserializeVersionTwo(in);
        } else {
            throw new IOException("Unrecognized version: " + getVersion());
        }
        setVersion(VERSION_LATEST);
    }

    /**
     * <p>Sets the values of the row based the given version one serialized row.</p>
     * @param in An input stream containing a version one serialized row
     * @throws java.io.IOException
     */
    protected void deserializeVersionOne(InputStream in) throws IOException {
        setHost(RemoteUtil.readLine(in));
        setName(RemoteUtil.readLine(in));
        setGroup(RemoteUtil.readLine(in));
        setPort(RemoteUtil.readInt(in));
        setFlags(RemoteUtil.readLong(in));

        // Set hash spans
        Set<HashSpan> thisHashSpans = new HashSet<HashSpan>();
        int hashSpanCount = RemoteUtil.readInt(in);
        for (int i = 0; i < hashSpanCount; i++) {
            BigHash first = RemoteUtil.readBigHash(in);
            BigHash last = RemoteUtil.readBigHash(in);
            thisHashSpans.add(new HashSpan(first, last));
        }
        setHashSpans(thisHashSpans);

        // Set target hash spans
        Set<HashSpan> thisTargetHashSpans = new HashSet();
        int targetHashSpanCount = RemoteUtil.readInt(in);
        for (int i = 0; i < targetHashSpanCount; i++) {
            BigHash first = RemoteUtil.readBigHash(in);
            BigHash last = RemoteUtil.readBigHash(in);
            thisTargetHashSpans.add(new HashSpan(first, last));
        }
        setTargetHashSpans(thisTargetHashSpans);

        setUpdateTimestamp(RemoteUtil.readLong(in));
    }

    /**
     * 
     * @param in
     * @throws IOException
     */
    protected void deserializeVersionTwo(InputStream in) throws IOException {
        setHost(IOUtil.readString(in));
        setName(IOUtil.readString(in));
        setGroup(IOUtil.readString(in));
        setPort(IOUtil.readInt(in));
        setFlags(IOUtil.readLong(in));

        // Set hash spans
        Set<HashSpan> thisHashSpans = new HashSet<HashSpan>();
        int hashSpanCount = IOUtil.readInt(in);
        for (int i = 0; i < hashSpanCount; i++) {
            BigHash first = IOUtil.readBigHash(in);
            BigHash last = IOUtil.readBigHash(in);
            thisHashSpans.add(new HashSpan(first, last));
        }
        setHashSpans(thisHashSpans);

        // Set target hash spans
        Set<HashSpan> thisTargetHashSpans = new HashSet();
        int targetHashSpanCount = IOUtil.readInt(in);
        for (int i = 0; i < targetHashSpanCount; i++) {
            BigHash first = IOUtil.readBigHash(in);
            BigHash last = IOUtil.readBigHash(in);
            thisTargetHashSpans.add(new HashSpan(first, last));
        }
        setTargetHashSpans(thisTargetHashSpans);

        setUpdateTimestamp(IOUtil.readLong(in));
        setResponseTimestamp(IOUtil.readLong(in));
    }

    /**
     * <p>Evaluates all the attributes except the update timestamp of the given object against this one.</p>
     * @param o A StatusTableRow object
     * @return Whether all the attributes except the update timestamp of the given object are equal to this one.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof StatusTableRow) {
            StatusTableRow str = (StatusTableRow) o;
            return host.equals(str.getHost())
                    && name.equals(str.getName())
                    && group.equals(str.getGroup())
                    && port == str.getPort()
                    && flags == str.getFlags()
                    && HashSpanCollection.areEqual(getHashSpans(), str.getHashSpans())
                    && HashSpanCollection.areEqual(getTargetHashSpans(), str.getTargetHashSpans())
                    && updateTimestamp == str.getUpdateTimestamp()
                    && responseTimestamp == str.getResponseTimestamp();
        }
        return false;
    }

    /**
     * <p>Creates a duplication of this row.</p>
     * @return The duplicate row
     */
    @Override
    public StatusTableRow clone() {
        try {
            ByteArrayOutputStream baos = null;
            ByteArrayInputStream bais = null;
            try {
                baos = new ByteArrayOutputStream();
                serialize(version, baos);
                bais = new ByteArrayInputStream(baos.toByteArray());
                return new StatusTableRow(bais);
            } finally {
                IOUtil.safeClose(baos);
                IOUtil.safeClose(bais);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>Hashes the host name only.</p>
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return host.hashCode();
    }

    /**
     * <p>Prints out the values of the attributes in the row.</p>
     * @return The printed value of the row
     */
    @Override
    public String toString() {
        String out = "Host = \"" + host + "\", name = \"" + name + "\", group = \"" + group + "\", update time = \"" + updateTimestamp + "\", response time = \"" + responseTimestamp + "\", port = \"" + port + "\", ssl = \"" + isSSL() + "\", online = \"" + isOnline() + "\", readable = \"" + isReadable() + "\", writable = \"" + isWritable() + "\", data store = \"" + isDataStore() + "\", core = \"" + isCore() + "\", local = \"" + isLocalServer() + "\"";
        Collection<HashSpan> hashSpans = getHashSpans();
        if (!hashSpans.isEmpty()) {
            out = out + ", " + hashSpans.size() + " hash spans = ";
            boolean first = true;
            for (HashSpan hs : hashSpans) {
                if (!first) {
                    out = out + " ; ";
                } else {
                    first = false;
                }
                out = out + new AbstractHashSpan(hs).toString();
            }
        }
        Collection<HashSpan> targetHashSpans = getTargetHashSpans();
        if (!targetHashSpans.isEmpty()) {
            out = out + ", " + targetHashSpans.size() + " target hash spans = ";
            boolean first = true;
            for (HashSpan hs : targetHashSpans) {
                if (!first) {
                    out = out + " ; ";
                } else {
                    first = false;
                }
                out = out + new AbstractHashSpan(hs).toString();
            }
        }
        return out;
    }

    /**
     * <p>Returns whether the two collections of rows are equivalent.</p>
     * @param c1 A collection of rows
     * @param c2 A collection of rows
     * @return Whether the two collections of rows are equivalent
     */
    public static boolean areEqual(Collection<StatusTableRow> c1, Collection<StatusTableRow> c2) {
        // test null
        if (c1 == null && c2 == null) {
            return true;
        }
        if ((c1 == null || c2 == null) || (c1.size() != c2.size())) {
            return false;
        }
        // test them
        StatusTableRow[] array1 = c1.toArray(new StatusTableRow[0]);
        for (int i = 0; i < array1.length; i++) {
            if (!c2.contains(array1[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Given a collection of StatusTableRows, randomizes the rows and returns the first found full hash span.</p>
     * <p>It is important that the rows are randomized before finding the full hash span because the status table sorts all the rows alphabetically, and we don't want all clients connecting to the same servers.</p>
     * @param allRows The rows to use in trying to put together a full hash span.
     * @return A collection of status table rows that together make up a full hash span.
     * @throws java.lang.Exception
     */
    protected static Collection<StatusTableRow> calculateFullHashSpan(Collection<StatusTableRow> allRows) throws Exception {
        return calculateFullHashSpan(null, allRows);
    }

    /**
     * <p>Given two collections of StatusTableRows, randomizes the rows and returns the first found full hash span with read and write priveleges and core servers containing as many of the seedRows as possible.</p>
     * <p>It is important that the rows are randomized before finding the full hash span because the status table sorts all the rows alphabetically, and we don't want all clients connecting to the same servers.</p>
     * @param seedRows The function should try to keep as many of these in the full hash span as possible.
     * @param allRows All the rows to use in trying to put together a full hash span, possibly including the seed rows.
     * @return A collection of status table rows that together make up a full hash span.
     * @throws java.lang.Exception
     * TODO: allow for partial priveleges -- read on one server with a hash span plus write on another server with the same hash span
     */
    protected static Collection<StatusTableRow> calculateFullHashSpan(Collection<StatusTableRow> seedRows, Collection<StatusTableRow> allRows) throws Exception {
        // set up the rows
        StatusTableRow[] seedRowsArray = null;
        if (seedRows != null) {
            seedRowsArray = seedRows.toArray(new StatusTableRow[0]);
        } else {
            seedRowsArray = new StatusTableRow[0];
        }
        // randomize the rows -- status tables keeps the rows sorted alphabetically, and don't want all clients connecting to same servers
        StatusTableRow[] randomizedAllRowsArray;
        if (allRows != null) {
            List<StatusTableRow> allRowsList = new LinkedList<StatusTableRow>(allRows);
            Collections.shuffle(allRowsList);
            randomizedAllRowsArray = allRowsList.toArray(new StatusTableRow[0]);
        } else {
            randomizedAllRowsArray = new StatusTableRow[0];
        }
        // collection of rows to return
        List<StatusTableRow> fullHashSpanRows = new LinkedList<StatusTableRow>();
        // set up a collection of full hash spans
        HashSpanCollection collection = new HashSpanCollection();
        int seedRowCount = 0;
        // start with the seed rows
        while (!collection.isFullHashSpan() && seedRowCount < seedRowsArray.length) {
            StatusTableRow row = seedRowsArray[seedRowCount++];
            // TODO: allow for partial priveleges -- read on one server with a hash span plus write on another server with the same hash span
            if (row != null && row.isOnline() && row.isCore() && row.isReadable() && row.isWritable() && !row.getHashSpans().isEmpty()) {
                collection.addAll(row.getHashSpans());
                fullHashSpanRows.add(row);
            }
        }
        int allRowCount = 0;
        // not full yet? start adding other rows
        while (!collection.isFullHashSpan() && allRowCount < randomizedAllRowsArray.length) {
            StatusTableRow row = randomizedAllRowsArray[allRowCount++];
            // TODO: allow for partial priveleges -- read on one server with a hash span plus write on another server with the same hash span
            if (row != null && row.isOnline() && row.isCore() && row.isReadable() && row.isWritable() && !row.getHashSpans().isEmpty()) {
                collection.addAll(row.getHashSpans());
                fullHashSpanRows.add(row);
            }
        }
        // full hash span impossible -- just return what we've got
        if (!collection.isFullHashSpan()) {
            return Collections.unmodifiableCollection(fullHashSpanRows);
        }
        // trim off the unnecessary other rows
        for (int i = 0; i < allRowCount; i++) {
            StatusTableRow row = randomizedAllRowsArray[i];
            if (row != null) {
                // remove
                collection.removeAll(row.getHashSpans());
                fullHashSpanRows.remove(row);
                // no longer a full hash span
                if (!collection.isFullHashSpan()) {
                    // put back
                    collection.addAll(row.getHashSpans());
                    fullHashSpanRows.add(row);
                }
            }
        }
        // trim off the unnecessary seed rows
        for (int i = 0; i < seedRowCount; i++) {
            StatusTableRow row = seedRowsArray[i];
            if (row != null) {
                // remove
                collection.removeAll(row.getHashSpans());
                fullHashSpanRows.remove(row);
                // no longer a full hash span
                if (!collection.isFullHashSpan()) {
                    // put back
                    collection.addAll(row.getHashSpans());
                    fullHashSpanRows.add(row);
                }
            }
        }
        return Collections.unmodifiableCollection(fullHashSpanRows);
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static void setDebug(boolean debug) {
        StatusTableRow.debug = debug;
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
            DebugUtil.printOut(StatusTableRow.class.getName() + "> " + line);
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
