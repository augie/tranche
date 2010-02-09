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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.tranche.ConfigureTranche;
import org.tranche.TrancheServer;
import org.tranche.security.Signature;
import org.tranche.hash.BigHash;
import org.tranche.configuration.Configuration;
import org.tranche.configuration.ConfigurationUtil;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.exceptions.NoHostProvidedException;
import org.tranche.exceptions.UnresponsiveServerException;
import org.tranche.util.IOUtil;
import org.tranche.servers.ServerCallbackEvent;
import org.tranche.servers.ServerEvent;
import org.tranche.servers.ServerMessageDownEvent;
import org.tranche.servers.ServerMessageUpEvent;
import org.tranche.logs.activity.Activity;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.StatusTable;
import org.tranche.server.DeleteDataItem;
import org.tranche.server.DeleteMetaDataItem;
import org.tranche.server.GetActivityLogEntriesCountItem;
import org.tranche.server.GetActivityLogEntriesItem;
import org.tranche.server.GetConfigurationItem;
import org.tranche.server.GetDataHashesItem;
import org.tranche.server.GetDataItem;
import org.tranche.server.GetMetaDataHashesItem;
import org.tranche.server.GetMetaDataItem;
import org.tranche.server.GetNetworkStatusItem;
import org.tranche.server.GetProjectHashesItem;
import org.tranche.server.HasDataItem;
import org.tranche.server.HasMetaDataItem;
import org.tranche.server.NonceItem;
import org.tranche.server.PingItem;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.server.RegisterServerItem;
import org.tranche.server.SetConfigurationItem;
import org.tranche.server.SetDataItem;
import org.tranche.server.SetMetaDataItem;
import org.tranche.server.ShutdownItem;
import org.tranche.time.TimeUtil;
import org.tranche.util.AssertionUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.Text;

/**
 * <p>Handles client socket connection to the server.</p>
 * @author Jayson
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class RemoteTrancheServer extends TrancheServer {

    private static boolean debug = false;
    /**
     * <p>The required difference between the last satisfied callback id and all others before purged.</p>
     * <p>For example, say a callback with id of 5 was just satisfied. Should we purge outstanding callback with id of 3?</p>
     * <p>If this value is 1 or 2, then yes. (0 and 1 are the same since no two callbacks have same id.) However, for anything else, will not purge.</p>
     * <p>This value is experimental -- if less connection problems, then this number should be high.</p>
     */
    private final static int REQUIRED_CALLBACK_ID_DIFFERENCE_BEFORE_PURGE = 50;
    /**
     * <p>Absolute limit for all batch sizes. Likely to change.</p>
     */
    public final static int BATCH_HAS_LIMIT = 100;
    /**
     * <p>Limit for batch-get requests.</p>
     */
    public final static int BATCH_GET_LIMIT = 25;
    /**
     * <p>Limit for batch-set requests.</p>
     */
    public final static int BATCH_SET_LIMIT = 10;
    /**
     * <p>Limit for batch-nonce requests.</p>
     */
    public final static int BATCH_NONCE_LIMIT = BATCH_GET_LIMIT;
    /**
     * <p>Abstract OK byte to keep sync in order</p>
     */
    public static final byte OK_BYTE = 0;
    /**
     *
     */
    public static final long DEFAULT_RESPONSE_TIMEOUT = ConfigureTranche.getLong(ConfigureTranche.PROP_SERVER_TIMEOUT);
    /**
     * 
     */
    public static final int DEFAULT_MAX_TRIES = 3;
    /**
     * <p>The number of milliseconds before a response is decidedly not going to be returned.</p>
     */
    public static long responseTimeout = DEFAULT_RESPONSE_TIMEOUT;
    /**
     * <p>The number of times in a row that a response from a server times out before the server is decidedly offline.</p>
     */
    private static int maxTries = DEFAULT_MAX_TRIES;
    // returns the incrementing id
    private static long incrementingId = 0;
    /**
     * <p>If synchronize on RemoteTrancheServer.this, deadlock!</p>
     */
    private static final Object nextIdLock = new Object();
    private static boolean isMultiSocketTest = false;
    /**
     * <p>Time of last response from server</p>
     */
    private long timeLastServerResponse = TimeUtil.getTrancheTimestamp();
    // socket and IO for the data transmission
    protected Socket dataSocket = null;
    private InputStream inputStream = null;
    private BufferedInputStream bufferedInputStream = null;
    private DataInputStream dataInputStream = null;
    private OutputStream outputStream = null;
    private BufferedOutputStream bufferedOutputStream = null;
    private DataOutputStream dataOutputStream = null;
    // flag for closed state
    private boolean closed = false;
    // IP address of the server being used
    private String host;
    // the port to connect on
    private int port;
    // if ssl is being used or not
    private boolean secure = false;
    /**
     * <p>Buffer of outgoing tasks</p>
     */
    protected final Map<Long, RemoteCallback> outgoingSent = new HashMap<Long, RemoteCallback>();
    protected final LinkedList<OutGoingBytes> outgoingQueue = new LinkedList<OutGoingBytes>();
    private final long[] timeLastWakeUp = new long[]{TimeUtil.getTrancheTimestamp(), TimeUtil.getTrancheTimestamp()};
    private final long[] timeLastNotify = new long[]{TimeUtil.getTrancheTimestamp(), TimeUtil.getTrancheTimestamp()};
    private long timeLastUsed = TimeUtil.getTrancheTimestamp();
    private long lastReconnectedTimestamp = 0;
    private int connectCount = 0;
    protected final RemoteTrancheServerDownloadThread downloadThread = new RemoteTrancheServerDownloadThread(this);
    protected final RemoteTrancheServerUploadThread uploadThread = new RemoteTrancheServerUploadThread(this);
    private final List<RemoteTrancheServerListener> listeners = new LinkedList<RemoteTrancheServerListener>();

    /**
     * 
     * @param host
     * @param port
     * @param secure
     * @throws java.lang.Exception
     */
    public RemoteTrancheServer(String host, int port, boolean secure) throws Exception {
        this.host = host;
        this.port = port;
        this.secure = secure;
        reconnect();
        uploadThread.setName("Remote Tranche Server Upload Thread: " + host);
        uploadThread.start();
        downloadThread.setName("Remote Tranche Server Download Thread: " + host);
        downloadThread.start();
    }

    /**
     * <p>Intelligently reconnect or connect to the remote Tranche server.</p>
     * @throws java.io.IOException
     */
    protected synchronized void reconnect() throws IOException {
        connectCount++;
        fireNewConnection();
        if (connectCount == 1) {
            debugOut("Attempting initial connection to " + IOUtil.createURL(this));
        } else {
            debugOut("Attempting a reconnect to " + IOUtil.createURL(this));
        }

        if (lastReconnectedTimestamp != 0 && (lastReconnectedTimestamp + 10) >= TimeUtil.getTrancheTimestamp()) {
            debugOut("Will not reconnect within 10ms of last reconnect, at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            return;
        }

        // tear down existing input
        IOUtil.safeClose(inputStream);
        IOUtil.safeClose(bufferedInputStream);
        IOUtil.safeClose(dataInputStream);
        // tear down existing output
        IOUtil.safeClose(outputStream);
        IOUtil.safeClose(bufferedOutputStream);
        IOUtil.safeClose(dataOutputStream);

        // don't close firewalled connections
        try {
            dataSocket.shutdownInput();
        } catch (Exception e) {
        }
        try {
            dataSocket.shutdownOutput();
        } catch (Exception e) {
        }
        try {
            dataSocket.close();
        } catch (Exception e) {
        }

        // Purge all previous callbacks. They are no longer valid.
        notifyPreviousCallbacks(Long.MAX_VALUE, "Server is reconnecting");

        if (!secure) {
            dataSocket = SocketFactory.getDefault().createSocket(host, port);
        } else {
            // make an SSL socket and set it
            SSLSocket ssls = (SSLSocket) SSLSocketFactory.getDefault().createSocket(host, port);
            ssls.setEnabledCipherSuites(ssls.getSupportedCipherSuites());
            dataSocket = ssls;
        }

        // disable nagle's algorithm
        dataSocket.setTcpNoDelay(false);

        // setup the input streams
        inputStream = dataSocket.getInputStream();
        bufferedInputStream = new BufferedInputStream(inputStream);
        dataInputStream = new DataInputStream(bufferedInputStream);
        // setup the output streams
        outputStream = dataSocket.getOutputStream();
        bufferedOutputStream = new BufferedOutputStream(outputStream);
        dataOutputStream = new DataOutputStream(bufferedOutputStream);

        if (lastReconnectedTimestamp != 0) {
            debugOut("Reconnected to " + IOUtil.createURL(this));
        } else {
            debugOut("Connected to " + IOUtil.createURL(this));
        }

        // Update the reconnect timestamp to avoid subsequent, waiting reconnects
        lastReconnectedTimestamp = TimeUtil.getTrancheTimestamp();

        // Make sure threads know this is being used!!!
        setTimeLastUsed(TimeUtil.getTrancheTimestamp());
    }

    /**
     * 
     * @return
     */
    protected DataOutputStream getDataOutputStream() {
        return dataOutputStream;
    }

    /**
     *
     * @return
     */
    protected DataInputStream getDataInputStream() {
        return dataInputStream;
    }

    /**
     * <p>Add a callback to outgoing and register it with timeout registry.outgoingSent</p>
     * @param id
     * @param callback
     * @throws java.lang.Exception
     */
    private void addCallback(long id, RemoteCallback callback) throws Exception {
        synchronized (outgoingSent) {
            outgoingSent.put(id, callback);
        }
        RemoteCallbackRegistry.register(callback, RemoteTrancheServer.this);
    }

    /**
     * 
     * @return
     */
    public int countOutstandingRequests() {
        synchronized (outgoingSent) {
            return outgoingSent.size();
        }
    }

    /**
     * 
     * @param ogb
     * @throws java.lang.Exception
     */
    private void addToQueue(OutGoingBytes ogb) throws Exception {
        synchronized (outgoingQueue) {
            while (!closed && outgoingQueue.size() >= ConfigureTranche.getInt(ConfigureTranche.PROP_SERVER_QUEUE_SIZE)) {
                outgoingQueue.wait(1000);
            }
            if (closed) {
                return;
            }
            outgoingQueue.addLast(ogb);
        }
        synchronized (uploadThread) {
            uploadThread.notify();
        }
    }

    /**
     * <p>Ask RTS to purge callback. Must have matchign copy of callback or will not purge.</p>
     * @param id
     * @param callback
     * @param reason
     * @return
     * @throws java.lang.Exception
     */
    public boolean purge(long id, RemoteCallback callback, String reason) throws Exception {
        synchronized (outgoingSent) {
            RemoteCallback callbackVerify = this.outgoingSent.get(id);
            if (callbackVerify == null || !callbackVerify.equals(callback)) {
                return false;
            }
            this.outgoingSent.remove(id);
        }

        RemoteCallbackRegistry.unregister(callback);
        callback.purge(reason);

        return true;
    }

    /**
     * <p>Ends all pending callbacks with ids before a certain id.</p>
     * @param id
     * @param reasonMsg
     */
    protected void notifyPreviousCallbacks(long id, String reasonMsg) {
        synchronized (outgoingSent) {
            RemoteCallback[] waitingActions = outgoingSent.values().toArray(new RemoteCallback[0]);
            for (RemoteCallback waitingAction : waitingActions) {
                // check that the id is less
                final long cutoffId = id - Math.abs(REQUIRED_CALLBACK_ID_DIFFERENCE_BEFORE_PURGE);
                if (closed || waitingAction.getID() <= cutoffId) {
                    RemoteCallbackRegistry.unregister(waitingAction);
                    synchronized (waitingAction) {
                        debugOut("Purging callback for " + IOUtil.createURL(RemoteTrancheServer.this) + ": id=" + waitingAction.getID() + "; isComplete=" + waitingAction.isComplete() + "; Reason = " + reasonMsg);
                        waitingAction.purge(reasonMsg);
                    }
                    // remove the notified action -- the normal thread won't
                    outgoingSent.remove(waitingAction.getID());
                }
            }
        }
    }

    /**
     * <p>Send the output.</p>
     */
    protected void flushOutputStreams() {
        try {
            dataOutputStream.flush();
        } catch (Exception e) {
            debugErr(e);
        }
        try {
            bufferedOutputStream.flush();
        } catch (Exception e) {
            debugErr(e);
        }
        try {
            outputStream.flush();
        } catch (Exception e) {
            debugErr(e);
        }
    }

    /**
     * <p>Gest the port number of the remote tranche server.</p>
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * <p>Gets the host name of the remote tranche server.</p>
     * @return
     */
    public String getHost() {
        return host;
    }

    /**
     * <p>Asks whether the remote tranche server has the given list of meta data chunks.</p>
     * @param hashes
     * @return
     * @throws java.lang.Exception
     */
    public boolean[] hasMetaData(BigHash[] hashes) throws Exception {
        if (hashes.length > BATCH_HAS_LIMIT) {
            throw new RuntimeException("Maximum of " + BATCH_HAS_LIMIT + " hashes allowed in a batchHasMetaData call. Found " + hashes.length);
        }
        boolean[] response = null;
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                response = hasMetaDataInternal(hashes).getResponse();
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        return response;
    }

    public BooleanArrayCallback hasMetaDataInternal(BigHash[] hashes) throws Exception {
        // buffer up a nonce request
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HasMetaDataItem.writeRequest(true, hashes, baos);
        // write the id
        long id = RemoteTrancheServer.getNextId();
        // make the output
        OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.HAS_META_DATA);
        // make the callback
        BooleanArrayCallback hdc = new BooleanArrayCallback(id, this, "Batch has meta data chunk (" + hashes.length + " chunks).");
        // add the callback to the queue
        addCallback(id, hdc);
        // add the bytes to send
        addToQueue(ogb);
        // notify the listeners
        fireCreatedBytesToUpload(id, ogb.bytes.length);
        return hdc;
    }

    /**
     * <p>Asks whether the remote tranche server has the given list of data chunks.</p>
     * @param hashes
     * @return
     * @throws java.lang.Exception
     */
    public boolean[] hasData(BigHash[] hashes) throws Exception {
        if (hashes.length > BATCH_HAS_LIMIT) {
            throw new RuntimeException("Maximum of " + BATCH_HAS_LIMIT + " hashes allowed in a batchHasData call.");
        }
        boolean[] response = null;
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                response = hasDataInternal(hashes).getResponse();
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        return response;
    }

    /**
     * 
     * @param hashes
     * @return
     * @throws java.lang.Exception
     */
    public BooleanArrayCallback hasDataInternal(BigHash[] hashes) throws Exception {
        // buffer up a nonce request
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HasDataItem.writeRequest(true, hashes, baos);
        // write the id
        long id = RemoteTrancheServer.getNextId();
        // make the output
        OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.HAS_DATA);
        // make the callback
        BooleanArrayCallback hdc = hdc = new BooleanArrayCallback(id, this, "Batch has data chunk (" + hashes.length + " chunks).");
        // add the callback to the queue
        addCallback(id, hdc);
        // add the bytes to send
        addToQueue(ogb);
        // notify the listeners
        fireCreatedBytesToUpload(id, ogb.bytes.length);
        return hdc;
    }

    /**
     * 
     * @return
     * @throws java.lang.Exception
     */
    public StatusTable getNetworkStatus() throws Exception {
        return getNetworkStatusPortion(null, null);
    }

    /**
     *
     * @param startHost
     * @param endHost
     * @return
     * @throws java.lang.Exception
     */
    public StatusTable getNetworkStatusPortion(String startHost, String endHost) throws Exception {
        // try maxTries times
        StatusTable response = null;
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                debugOut("Request Network Status: " + startHost + ", " + endHost);

                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GetNetworkStatusItem.writeRequest(true, startHost, endHost, baos);
                // write out the id
                long id = RemoteTrancheServer.getNextId();
                // write out the bytes
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.GET_NETWORK_STATUS);
                // make the callback
                StatusTableCallback callback = new StatusTableCallback(id, this, "Get the network status.");
                // queue up the bytes and the callback
                addCallback(id, callback);
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                response = callback.getResponse();
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        return response;
    }

    /**
     * <p>Pings the remote tranche server.</p>
     * @throws java.lang.Exception
     */
    public void ping() throws Exception {
        try {
            // buffer up a nonce request
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PingItem.writeRequest(true, baos);
            // write the id
            long id = RemoteTrancheServer.getNextId();
            // make the output
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.PING);
            // make the callback
            CheckForErrorCallback cfec = new CheckForErrorCallback(id, this, "Ping.");
            // add the callback to the queue
            addCallback(id, cfec);
            // add the bytes to send
            addToQueue(ogb);
            // notify the listeners
            fireCreatedBytesToUpload(id, ogb.bytes.length);
            // waits for the response from the server
            cfec.checkForError();
        } catch (Exception e) {
            debugErr(e);
            ConnectionUtil.reportExceptionHost(host, e);
            throw e;
        }
    }

    /**
     * <p>Sends a request for a shutdown to the remote tranche server.</p>
     * @param sig
     * @param nonce
     * @return
     * @throws java.lang.Exception
     */
    public void requestShutdown(Signature sig, byte[] nonce) throws Exception {
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ShutdownItem.writeRequest(true, sig, nonce, baos);
                // write out the id
                long id = RemoteTrancheServer.getNextId();
                // write out the bytes
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.SHUTDOWN);
                // make the callback
                CheckForErrorCallback cfec = new CheckForErrorCallback(id, this, "Checking for error after requesting server shutdown.");
                // queue up the bytes and the callback
                addCallback(id, cfec);
                addToQueue(ogb);
                cfec.checkForError();
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * <p>Returns a list of data hashes from the remote tranche server.</p>
     * @param offset
     * @param length
     * @return
     * @throws java.lang.Exception
     */
    public BigHash[] getDataHashes(BigInteger offset, BigInteger length) throws Exception {
        BigHash[] response = null;
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GetDataHashesItem.writeRequest(true, offset.longValue(), length.longValue(), baos);
                // write out the id
                long id = RemoteTrancheServer.getNextId();
                // write out the bytes
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.GET_DATA_HASHES);
                // make the callback
                GetHashesCallback ghc = new GetHashesCallback(id, this, "Requesting " + length.toString() + " data hashes.");
                // queue up the bytes and the callback
                addCallback(id, ghc);
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                response = ghc.getHashes();
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        return response;
    }

    /**
     * <p>Returns a list of meta data hashes from the remote tranche server.</p>
     * @param offset
     * @param length
     * @return
     * @throws java.lang.Exception
     */
    public BigHash[] getMetaDataHashes(BigInteger offset, BigInteger length) throws Exception {
        BigHash[] response = null;
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GetMetaDataHashesItem.writeRequest(true, offset.longValue(), length.longValue(), baos);
                // write out the id
                long id = RemoteTrancheServer.getNextId();
                // write out the bytes
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.GET_META_HASHES);
                // make the callback
                GetHashesCallback ghc = new GetHashesCallback(id, this, "Requesting " + length.toString() + " meta data hashes.");
                // queue up the bytes and the callback
                addCallback(id, ghc);
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                response = ghc.getHashes();
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        return response;
    }

    /**
     * <p>Register a tranche server with the remote tranche server.</p>
     * @param url
     * @throws java.lang.Exception
     */
    public void registerServer(String url) throws Exception {
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                RegisterServerItem.writeRequest(true, url, baos);
                // write out the id
                long id = RemoteTrancheServer.getNextId();
                // write out the bytes
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.REGISTER_SERVER);
                // make the callback
                CheckForErrorCallback cfec = new CheckForErrorCallback(id, this, "Check for error after registering server: " + url);
                // queue up the bytes and the callback
                addCallback(id, cfec);
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                cfec.checkForError();
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * <p>Gets the configuration from the remote tranche server.</p>
     * @param sig
     * @param nonce
     * @return
     * @throws java.lang.Exception
     */
    public Configuration getConfiguration(Signature sig, byte[] nonce) throws Exception {
        Configuration response = null;
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GetConfigurationItem.writeRequest(true, nonce, sig, baos);
                // write the id
                long id = RemoteTrancheServer.getNextId();
                // make the output
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.GET_CONFIGURATION);
                // make the callback
                GetBytesCallback gbc = new GetBytesCallback(id, this, "Getting configuration.");
                // add the callback to the queue
                addCallback(id, gbc);
                // add the bytes to send
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                byte[] config = gbc.getBytes();
                // return the configuration
                response = ConfigurationUtil.read(new ByteArrayInputStream(config));
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        return response;
    }

    /**
     * <p>Sets the configuration to the remote tranche server.</p>
     * @param data
     * @param sig
     * @param nonce
     * @throws java.lang.Exception
     */
    public void setConfiguration(byte[] data, Signature sig, byte[] nonce) throws Exception {
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                SetConfigurationItem.writeRequest(true, data, sig, nonce, baos);
                // write out the id
                long id = RemoteTrancheServer.getNextId();
                // write out the bytes
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.SET_CONFIGURATION);
                // make the callback
                CheckForErrorCallback cfec = new CheckForErrorCallback(id, this, "Checking for error after setting configuration.");
                // queue up the bytes and the callback
                addCallback(id, cfec);
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                cfec.checkForError();
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * <p>Remote protocol for sending project hashes.</p>
     * @param offset
     * @param length
     * @return
     * @throws java.lang.Exception
     */
    public BigHash[] getProjectHashes(BigInteger offset, BigInteger length) throws Exception {
        BigHash[] response = null;
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GetProjectHashesItem.writeRequest(true, offset.longValue(), length.longValue(), baos);
                // write out the id
                long id = RemoteTrancheServer.getNextId();
                // write out the bytes
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.GET_PROJECT_HASHES);
                // make the callback
                GetHashesCallback ghc = new GetHashesCallback(id, this, "Getting project hashes (offset=" + offset + ", length=" + length + ").");
                // queue up the bytes and the callback
                addCallback(id, ghc);
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                response = ghc.getHashes();
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        return response;
    }

    /**
     * <p>Gets a list of data chunks corresponding to the given list of hashes from the remote tranche server.</p>
     * @param hashes
     * @param propagateRequest
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper getData(BigHash[] hashes, boolean propagateRequest) throws Exception {
        if (hashes.length > BATCH_GET_LIMIT) {
            throw new RuntimeException("Maximum of " + BATCH_GET_LIMIT + " hashes allowed in a batchGetData call, but found " + hashes.length + ".");
        }
        PropagationReturnWrapper wrapper = null;
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GetDataItem.writeRequest(true, hashes, propagateRequest, baos);
                // write the id
                long id = RemoteTrancheServer.getNextId();
                // make the output
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.GET_DATA);
                // make the callback
                GetBytesCallback gbc = new GetBytesCallback(id, this, "Batch get data (" + hashes.length + " chunks).");
                // add the callback to the queue
                addCallback(id, gbc);
                // add the bytes to send
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);

                // read the returned object
                wrapper = PropagationReturnWrapper.createFromBytes(gbc.getBytes());

                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        return wrapper;
    }

    /**
     * <p>Gets a list of meta data chunks corresponding to the given list of hashes from the remote tranche server.</p>
     * @param hashes
     * @param propagateRequest
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper getMetaData(BigHash[] hashes, boolean propagateRequest) throws Exception {
        if (hashes.length > BATCH_GET_LIMIT) {
            throw new RuntimeException("Maximum of " + BATCH_GET_LIMIT + " hashes allowed in a batchGetMetaData call.");
        }
        PropagationReturnWrapper wrapper = null;
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GetMetaDataItem.writeRequest(true, hashes, propagateRequest, baos);
                // write the id
                long id = RemoteTrancheServer.getNextId();
                // make the output
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.GET_META_DATA);
                // make the callback
                GetBytesCallback gbc = new GetBytesCallback(id, this, "Batch get meta data (" + hashes.length + " chunks).");
                // add the callback to the queue
                addCallback(id, gbc);
                // add the bytes to send
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                wrapper = PropagationReturnWrapper.createFromBytes(gbc.getBytes());
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        return wrapper;
    }

    /**
     * <p>Get all activity log entries from within a time period.</p>
     * @param startTimestamp Timestamp from which to include activities (inclusive)
     * @param finishTimestamp Timestamp to which to include activities (inclusive)
     * @param limit Limit number of activity entries to return
     * @param mask Used to match activity bytes. If all the bits in the mask are included in the activity byte, then the activity will be included. For example, if the mask is Activity.ACTION_TYPE_SET, then all logs that have this bit set (set data and set meta data) will be included. However, if Activity.SET_DATA is the mask, then only set data actions will match (since there are two bits set in the mask, making it more restrictive). 0 is least restrictive (restricts nothing) and 0xFF is most restrictive (restricts everything but itself).
     * @return
     */
    public Activity[] getActivityLogEntries(long startTimestamp, long finishTimestamp, int limit, byte mask) throws Exception {
        Activity[] activities = null;
        for (int k = 0; k < getMaxTries(); k++) {
            ByteArrayInputStream bais = null;
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GetActivityLogEntriesItem.writeRequest(true, startTimestamp, finishTimestamp, limit, mask, baos);
                // write the id
                long id = RemoteTrancheServer.getNextId();
                // make the output
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.GET_ACTIVITY_LOG_ENTRIES);
                // make the callback
                ActivityArrayCallback aac = new ActivityArrayCallback(id, this, "Get log entries(from: " + startTimestamp + ", to: " + finishTimestamp + ", limit: " + limit + ").");
                // add the callback to the queue
                addCallback(id, aac);
                // add the bytes to send
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                activities = aac.getResponse();
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            } finally {
                IOUtil.safeClose(bais);
            }
        }
        return activities;
    }

    /**
     * <p>Returns a count of all activity log entries from within a time period.</p>
     * @param startTimestamp Timestamp from which to include activities (inclusive)
     * @param stopTimestamp Timestamp to which to include activities (inclusive)
     * @param mask Used to match activity bytes. If all the bits in the mask are included in the activity byte, then the activity will be included. For example, if the mask is Activity.ACTION_TYPE_SET, then all logs that have this bit set (set data and set meta data) will be included. However, if Activity.SET_DATA is the mask, then only set data actions will match (since there are two bits set in the mask, making it more restrictive). 0 is least restrictive (restricts nothing) and 0xFF is most restrictive (restricts everything but itself).
     * @return
     * @throws java.lang.Exception
     */
    public int getActivityLogEntriesCount(long startTimestamp, long stopTimestamp, byte mask) throws Exception {
        int response = -1;
        for (int k = 0; k < getMaxTries(); k++) {
            ByteArrayInputStream bais = null;
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GetActivityLogEntriesCountItem.writeRequest(true, startTimestamp, stopTimestamp, mask, baos);
                // write the id
                long id = RemoteTrancheServer.getNextId();
                // make the output
                byte[] byteArray = baos.toByteArray();
                OutGoingBytes ogb = new OutGoingBytes(id, byteArray, OutGoingBytes.GET_ACTIVITY_LOG_ENTRIES_COUNT);
                // make the callback
                IntegerCallback ic = new IntegerCallback(id, this, "Get log entries count(from: " + startTimestamp + ", to: " + stopTimestamp + ").");
                // add the callback to the queue
                addCallback(id, ic);
                // add the bytes to send
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                response = ic.getResponse();
                break;
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            } finally {
                IOUtil.safeClose(bais);
            }
        }
        return response;
    }

    /**
     * <p>If synchronize on RemoteTrancheServer.this, deadlock!</p>
     * @return
     */
    private static long getNextId() {
        synchronized (nextIdLock) {
            incrementingId++;
        }
        return incrementingId;
    }

    /**
     * <p>Used for stress testing only.</p>
     * @param isMultiSocketTest
     */
    public static void setMultiSocketTest(boolean isMultiSocketTest) {
        RemoteTrancheServer.isMultiSocketTest = isMultiSocketTest;
    }

    /**
     * <p>Tears down the TCP connection to the remote server and releases the associated resources. If another method is invoked by this RemoteDistributedFileSystem, the TCP connection will automatically be re-established. i.e. this method is a way to terminate lingering TCP connections that likly won't be needed.</p>
     */
    public synchronized void close() {
        debugOut("Closing remote tranche server " + getHost());

        // Check some conditions before closing down
        // Don't close socket if is test
        if (isMultiSocketTest) {
            return;
        }
        // skip if closed
        if (closed) {
            return;
        }

        // flag as closed
        closed = true;

        Thread t = new Thread("Try to shutdown server politely") {

            @Override()
            public void run() {
                // otherwise try to politely tear down the connection
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    baos.write(Token.CLOSE);
                    // out going bytes
                    long id = RemoteTrancheServer.getNextId();
                    OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.CLOSE);
                    addToQueue(ogb);
                    // notify the listeners
                    fireCreatedBytesToUpload(id, ogb.bytes.length);
                } catch (Exception e) {
                }
            }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        // Give it a few seconds
        try {
            t.join(1000);
        } catch (Exception e) { /* Move on */ }

        // close things down
        IOUtil.safeClose(dataInputStream);
        IOUtil.safeClose(bufferedInputStream);
        IOUtil.safeClose(dataOutputStream);
        IOUtil.safeClose(bufferedOutputStream);

        // don't close firewalled connections
        try {
            dataSocket.shutdownInput();
        } catch (Exception e) {
        }
        try {
            dataSocket.shutdownOutput();
        } catch (Exception e) {
        }
        try {
            dataSocket.close();
        } catch (Exception e) {
        }

        // Purge all previous callbacks. They are no longer valid.
        notifyPreviousCallbacks(Long.MAX_VALUE, "Server is closed.");
    }

    /**
     * <p>Does this server use SSL?</p>
     * @return
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * 
     * @return
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * <p>Returns whether the URL for this server is the same as the URL for the given server.</p>
     * @param obj
     * @return
     */
    @Override()
    public boolean equals(Object obj) {
        // specially compare to other rdfs
        if (obj instanceof RemoteTrancheServer) {
            RemoteTrancheServer a = (RemoteTrancheServer) obj;
            return IOUtil.createURL(this).equals(IOUtil.createURL(a));
        }
        return super.equals(obj);
    }

    /**
     * <p>Returns the hash code of the URL for this server.</p>
     * @return
     */
    @Override()
    public int hashCode() {
        return IOUtil.createURL(this).hashCode();
    }

    /**
     * <p>Time in milliseconds since last server response.</p>
     * @return
     */
    public long getMillisSinceLastServerResponse() {
        return TimeUtil.getTrancheTimestamp() - this.getTimeLastServerResponse();
    }

    /**
     * <p>Submit the latency of the chunk to server. Tries once then quits.</p>
     * @param latency
     */
    public void submitLatencyChunk(int latency) {
    }

    /**
     * <p>Submit the latency of the nonce to server. Tries once then quits.</p>
     * @param latency
     */
    public void submitLatencyNonce(int latency) {
    }

    /**
     * <p>Gets the time this server was last communicated with.</p>
     * @return
     */
    public long getTimeLastUsed() {
        return timeLastUsed;
    }

    /**
     * <p>Sets the time this server was last communicated with.</p>
     * @param timeLastUsed
     */
    public void setTimeLastUsed(long timeLastUsed) {
        this.timeLastUsed = timeLastUsed;
    }

    /**
     * <p>Used to track timestamps of last time client download thread resumed after a wait. Used for detecting timeouts and thread synchronization issues.</p>
     * @param timestamp
     */
    public void setLastTimeWakeUp(long timestamp) {
        timeLastWakeUp[1] = timeLastWakeUp[0];
        timeLastWakeUp[0] = timestamp;
    }

    /**
     * <p>Used to track timestamps of last time client download thread resumed after a wait. Used for detecting timeouts and thread synchronization issues.</p>
     * @param index
     * @return
     */
    public long getLastTimeWakeUp(int index) {
        return timeLastWakeUp[index];
    }

    /**
     * <p>Used to track timestamps of last time client sent request to server. Used for detecting timeouts.</p>
     * @param timestamp
     */
    public void setLastTimeNotify(long timestamp) {
        timeLastNotify[1] = timeLastNotify[0];
        timeLastNotify[0] = timestamp;
    }

    /**
     * <p>Used to track timestamps of last time client sent request to server. Used for detecting timeouts.</p>
     * @param index
     * @return
     */
    public long getLastTimeNotify(int index) {
        return timeLastNotify[index];
    }

    /**
     * <p>Returns the UNIX timetsamp of the last server response.</p>
     * @return The timestamp of the server's last message received.
     */
    public long getTimeLastServerResponse() {
        return timeLastServerResponse;
    }

    /**
     * <p>Set the timestamp of the server's last message received.</p>
     * @param timeLastServerResponse
     */
    public void setTimeLastServerResponse(long timeLastServerResponse) {
        this.timeLastServerResponse = timeLastServerResponse;
    }

    /**
     * <p>Replace the list of server listeners with the given list.</p>
     * @param listeners
     */
    public void setListeners(List<RemoteTrancheServerListener> listeners) {
        synchronized (listeners) {
            this.listeners.clear();
            this.listeners.addAll(listeners);
        }
    }

    /**
     * <p>Adds the given server listener from the list of server listeners.</p>
     * @param l
     */
    public void addListener(RemoteTrancheServerListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    /**
     * <p>Removes the given server listener from the list of server listeners.</p>
     * @param l
     */
    public void removeListener(RemoteTrancheServerListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    /**
     * <p>Removes the given server listeners from the list of server listeners.</p>
     * @param listeners
     */
    public void removeListeners(List<RemoteTrancheServerListener> listeners) {
        synchronized (listeners) {
            this.listeners.removeAll(listeners);
        }
    }

    /**
     * <p>Removes all server listeners.</p>
     */
    public void clearListeners() {
        synchronized (listeners) {
            listeners.clear();
        }
    }

    /**
     * <p>Returns the list of server listeners.</p>
     * @return
     */
    public Collection<RemoteTrancheServerListener> getListeners() {
        List<RemoteTrancheServerListener> list = new LinkedList<RemoteTrancheServerListener>();
        synchronized (listeners) {
            list.addAll(listeners);
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * <p>Notify the listeners that a new connection has been made to the server.</p>
     */
    protected void fireNewConnection() {
        ServerEvent se = new ServerEvent(IOUtil.createURL(this), ServerEvent.CONNECT);
        for (RemoteTrancheServerListener l : getListeners()) {
            try {
                l.serverConnect(se);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Notify the listeners that bytes have been downloaded from the server.</p>
     * @param callbackId
     * @param bytesToDownload
     */
    protected void fireStartedDownloadingBytes(long callbackId, long bytesToDownload) {
        ServerMessageDownEvent smde = new ServerMessageDownEvent(IOUtil.createURL(this), callbackId, ServerMessageDownEvent.TYPE_STARTED, bytesToDownload);
        for (RemoteTrancheServerListener l : getListeners()) {
            try {
                l.downMessageStarted(smde);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Notify the listeners that bytes have been downloaded from the server.</p>
     * @param callbackId
     * @param bytesToDownload
     * @param bytesDownloaded
     */
    protected void fireUpdateDownloadingBytes(long callbackId, long bytesToDownload, long bytesDownloaded) {
        ServerMessageDownEvent smde = new ServerMessageDownEvent(IOUtil.createURL(this), callbackId, ServerMessageDownEvent.TYPE_PROGRESS, bytesToDownload, bytesDownloaded);
        for (RemoteTrancheServerListener l : getListeners()) {
            try {
                l.downMessageProgress(smde);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Notify the listeners that bytes have been downloaded from the server.</p>
     * @param callbackId
     * @param bytesToDownload
     * @param bytesDownloaded
     */
    protected void fireFinishedDownloadingBytes(long callbackId, long bytesToDownload, long bytesDownloaded) {
        ServerMessageDownEvent smde = new ServerMessageDownEvent(IOUtil.createURL(this), callbackId, ServerMessageDownEvent.TYPE_COMPLETED, bytesToDownload, bytesDownloaded);
        for (RemoteTrancheServerListener l : getListeners()) {
            try {
                l.downMessageCompleted(smde);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Notify the listeners that bytes have failed to be downloaded from the server.</p>
     * @param callbackId
     */
    protected void fireFailedDownloadingBytes(long callbackId) {
        ServerMessageDownEvent smde = new ServerMessageDownEvent(IOUtil.createURL(this), callbackId, ServerMessageDownEvent.TYPE_FAILED);
        for (RemoteTrancheServerListener l : getListeners()) {
            try {
                l.downMessageFailed(smde);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Notify the listeners that bytes have been created to go to the server.</p>
     * @param callbackId
     * @param bytesToUpload
     */
    protected void fireCreatedBytesToUpload(long callbackId, long bytesToUpload) {
        ServerMessageUpEvent smue = new ServerMessageUpEvent(IOUtil.createURL(this), callbackId, ServerMessageUpEvent.TYPE_CREATED, bytesToUpload);
        for (RemoteTrancheServerListener l : getListeners()) {
            try {
                l.upMessageCreated(smue);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Notify the listeners that bytes are starting to be uploaded to the server.</p>
     * @param callbackId
     * @param bytesToUpload
     */
    protected void fireStartedUploadingBytes(long callbackId, long bytesToUpload) {
        ServerMessageUpEvent smue = new ServerMessageUpEvent(IOUtil.createURL(this), callbackId, ServerMessageUpEvent.TYPE_STARTED, bytesToUpload);
        for (RemoteTrancheServerListener l : getListeners()) {
            try {
                l.upMessageStarted(smue);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Notify the listeners that bytes have been uploaded to the server.</p>
     * @param callbackId
     * @param bytesToUpload
     * @param bytesUploaded
     */
    protected void fireFinishedUploadingBytes(long callbackId, long bytesToUpload, long bytesUploaded) {
        ServerMessageUpEvent smue = new ServerMessageUpEvent(IOUtil.createURL(this), callbackId, ServerMessageUpEvent.TYPE_COMPLETED, bytesToUpload, bytesUploaded);
        for (RemoteTrancheServerListener l : getListeners()) {
            try {
                l.upMessageSent(smue);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Notify the listeners that bytes could not be uploaded to the server.</p>
     * @param callbackId
     */
    protected void fireFailedUploadingBytes(long callbackId) {
        ServerMessageUpEvent smue = new ServerMessageUpEvent(IOUtil.createURL(this), callbackId, ServerMessageUpEvent.TYPE_FAILED);
        for (RemoteTrancheServerListener l : getListeners()) {
            try {
                l.upMessageFailed(smue);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Notify server listeners that a new callback has been created.</p>
     * @param rc
     */
    protected void fireCreatedCallback(RemoteCallback rc) {
        ServerCallbackEvent sce = new ServerCallbackEvent(IOUtil.createURL(this), rc.getID(), ServerCallbackEvent.TYPE_CREATED, rc.getName(), rc.getDescription());
        for (RemoteTrancheServerListener l : getListeners()) {
            try {
                l.requestCreated(sce);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Notify server listeners that a server request has been fulfilled.</p>
     * @param rc
     */
    protected void fireFulfilledCallback(RemoteCallback rc) {
        ServerCallbackEvent sce = new ServerCallbackEvent(IOUtil.createURL(this), rc.getID(), ServerCallbackEvent.TYPE_FULFILLED, rc.getName(), rc.getDescription());
        for (RemoteTrancheServerListener l : getListeners()) {
            try {
                l.requestFulfilled(sce);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Notify server listeners that a server request failed.</p>
     * @param rc
     */
    protected void fireFailedCallback(RemoteCallback rc) {
        ServerCallbackEvent sce = new ServerCallbackEvent(IOUtil.createURL(this), rc.getID(), ServerCallbackEvent.TYPE_FAILED, rc.getName(), rc.getDescription());
        for (RemoteTrancheServerListener l : getListeners()) {
            try {
                l.requestFailed(sce);
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Returns the maximum number of attempts before failing.</p>
     * @return
     */
    public static int getMaxTries() {
        return maxTries;
    }

    /**
     * <p>Sets the maximum number of connection attempts before failing.</p>
     * @param maxRetries
     */
    public static void setMaxRetries(int maxRetries) {
        RemoteTrancheServer.maxTries = maxRetries;
    }

    /**
     * 
     * @param milliseconds
     */
    public static void setResponseTimeout(long milliseconds) {
        responseTimeout = milliseconds;
    }

    /**
     * 
     * @return
     */
    public static long getResponseTimeout() {
        return responseTimeout;
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static void setDebug(boolean debug) {
        RemoteTrancheServer.debug = debug;
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
    protected static void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(RemoteTrancheServer.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    protected static void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }

    /**
     * <p>Set data and replicate to specified hosts.</p>
     * @param hash
     * @param data
     * @param sig
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper setData(BigHash hash, byte[] data, Signature sig, String[] hosts) throws Exception {

        // Hosts are required
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hash);
        AssertionUtil.assertNoNullValues(sig);
        AssertionUtil.assertNoNullValues(data);
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);

        // possible transmission errors
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                SetDataItem.writeRequest(true, hash, hosts, data, sig, baos);
                // write out the id
                long id = RemoteTrancheServer.getNextId();
                // write out the bytes
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.SET_DATA);
                // make the callback
                GetBytesCallback cfec = new GetBytesCallback(id, this, "Checking for error after setting data chunk (propagated).");
                // queue up the bytes and the callback
                addCallback(id, cfec);
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                return PropagationReturnWrapper.createFromBytes(cfec.getBytes());
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        // Whoops! Should not get here -- either succceeded or threw exception.
        throw new AssertionFailedException("Tried " + getMaxTries() + ", but failed to perform request. Should have thrown exception.");
    }

    /**
     * <p>Set meta data and replicate to specified hosts.</p>
     * @param merge
     * @param hash
     * @param data
     * @param sig
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper setMetaData(boolean merge, BigHash hash, byte[] data, Signature sig, String[] hosts) throws Exception {

        // Hosts are required
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hash);
        AssertionUtil.assertNoNullValues(sig);
        AssertionUtil.assertNoNullValues(data);
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);

        // possible transmission errors
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                SetMetaDataItem.writeRequest(true, merge, hash, hosts, data, sig, baos);
                // write out the id
                long id = RemoteTrancheServer.getNextId();
                // write out the bytes
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.SET_DATA);
                // make the callback
                GetBytesCallback cfec = new GetBytesCallback(id, this, "Checking for error after setting meta data chunk (propagated).");
                // queue up the bytes and the callback
                addCallback(id, cfec);
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                return PropagationReturnWrapper.createFromBytes(cfec.getBytes());
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        // Whoops! Should not get here -- either succceeded or threw exception.
        throw new AssertionFailedException("Tried " + getMaxTries() + ", but failed to perform request. Should have thrown exception.");
    }

    /**
     * <p>Get batch of nonces from selected server hosts.</p>
     * @param hosts
     * @param count
     * @return bytes[host index][nonce index][nonce bytes]. So, val[1][3][] returns the fourth nonce for second server. If want to reference all nonce for server 2: val[1]
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper getNonces(String[] hosts, int count) throws Exception {

        // Hosts are required
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);

        if (count < 0) {
            throw new Exception("Must request 0 or more nonces, instead requested " + count);
        }
        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer up a nonce request
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                NonceItem.writeRequest(true, count, hosts, baos);
                // write the id
                long id = RemoteTrancheServer.getNextId();
                // make the output
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.NONCE);
                // make the callback
                GetBytesCallback gbc = new GetBytesCallback(id, this, "Batch get nonce (" + count + " requested).");
                // add the callback to the queue
                addCallback(id, gbc);
                // add the bytes to send
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                return PropagationReturnWrapper.createFromBytes(gbc.getBytes());
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        // Whoops! Should not get here -- either succceeded or threw exception.
        throw new AssertionFailedException("Tried " + getMaxTries() + ", but failed to perform request. Should have thrown exception.");
    }

    /**
     * <p>Delete data chunk from selected servers.</p>
     * @param hash
     * @param sigs
     * @param nonces
     * @param hosts
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper deleteData(BigHash hash, Signature[] sigs, byte[][] nonces, String[] hosts) throws Exception {

        // Hosts are required
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hash);
        AssertionUtil.assertNoNullValues(sigs);
        AssertionUtil.assertNoNullValues(nonces);
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);

        // Assert: |nonces| = |hosts|
        if (nonces.length != hosts.length) {
            throw new AssertionFailedException("Should be one nonce for each hosts. Instead, nonces.length<" + nonces.length + "> != hosts.length<" + hosts.length + ">");
        }

        // Assert: |sigs| = |hosts|
        if (sigs.length != hosts.length) {
            throw new AssertionFailedException("Should be one signature for each hosts. Instead, sigs.length<" + sigs.length + "> != hosts.length<" + hosts.length + ">");
        }

        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DeleteDataItem.writeRequest(true, hash, hosts, sigs, nonces, baos);
                // write out the id
                long id = RemoteTrancheServer.getNextId();
                // write out the bytes
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.DELETE_DATA);
                // make the callback
                GetBytesCallback cfec = new GetBytesCallback(id, this, "Check for error after deleting data chunk (propagated).");
                // queue up the bytes and the callback
                addCallback(id, cfec);
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                return PropagationReturnWrapper.createFromBytes(cfec.getBytes());
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        // Whoops! Should not get here -- either succceeded or threw exception.
        throw new AssertionFailedException("Tried " + getMaxTries() + ", but failed to perform request. Should have thrown exception.");
    }

    /**
     * 
     * @param hash
     * @param uploaderName
     * @param uploadTimestamp
     * @param relativePathInDataSet
     * @param sigs
     * @param nonces
     * @param hosts
     * @throws java.lang.Exception
     */
    public PropagationReturnWrapper deleteMetaData(BigHash hash, String uploaderName, Long uploadTimestamp, String relativePathInDataSet, Signature[] sigs, byte[][] nonces, String[] hosts) throws Exception {

        // Hosts are required
        if (hosts == null || hosts.length == 0) {
            throw new NoHostProvidedException();
        }

        // Check parameters for assumptions
        AssertionUtil.assertNoNullValues(hash);
        AssertionUtil.assertNoNullValues(sigs);
        AssertionUtil.assertNoNullValues(nonces);
        AssertionUtil.assertNoNullValues(hosts);
        AssertionUtil.assertNoDuplicateObjects(hosts);

        // Assert: |nonces| = |hosts|
        if (nonces.length != hosts.length) {
            throw new AssertionFailedException("Should be one nonce for each hosts. Instead, nonces.length<" + nonces.length + "> != hosts.length<" + hosts.length + ">");
        }

        // Assert: |sigs| = |hosts|
        if (sigs.length != hosts.length) {
            throw new AssertionFailedException("Should be one signature for each hosts. Instead, sigs.length<" + sigs.length + "> != hosts.length<" + hosts.length + ">");
        }

        for (int k = 0; k < getMaxTries(); k++) {
            try {
                // buffer the bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DeleteMetaDataItem.writeRequest(true, hash, uploaderName, uploadTimestamp, relativePathInDataSet, hosts, sigs, nonces, baos);
                // write out the id
                long id = RemoteTrancheServer.getNextId();
                // write out the bytes
                OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray(), OutGoingBytes.DELETE_DATA);
                // make the callback
                GetBytesCallback cfec = new GetBytesCallback(id, this, "Check for error after deleting meta data chunk (propagated).");
                // queue up the bytes and the callback
                addCallback(id, cfec);
                addToQueue(ogb);
                // notify the listeners
                fireCreatedBytesToUpload(id, ogb.bytes.length);
                return PropagationReturnWrapper.createFromBytes(cfec.getBytes());
            } catch (Exception e) {
                ConnectionUtil.reportExceptionHost(host, e);
                if (e instanceof TimeoutException || (e instanceof IOException && !(e instanceof UnresponsiveServerException))) {
                    if (k == getMaxTries() - 1) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        // Whoops! Should not get here -- either succceeded or threw exception.
        throw new AssertionFailedException("Tried " + getMaxTries() + ", but failed to perform request. Should have thrown exception.");
    }
}