/*
 *    Copyright 2005 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except dataInputStream compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to dataInputStream writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stress.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.tranche.Signature;
import org.tranche.TrancheServer;
import org.tranche.configuration.Configuration;
import org.tranche.configuration.ConfigurationUtil;
import org.tranche.exceptions.OutOfDiskSpaceException;
import org.tranche.hash.BigHash;
import org.tranche.remote.BooleanCallback;
import org.tranche.remote.CheckForErrorCallback;
import org.tranche.remote.GetBytesCallback;
import org.tranche.remote.GetHashesCallback;
import org.tranche.remote.GetKnownServersCallback;
import org.tranche.remote.OutGoingBytes;
import org.tranche.remote.RemoteCallback;
import org.tranche.remote.RemoteCallbackRegistry;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.server.Server;
import org.tranche.servers.ServerInfo;
import org.tranche.servers.ServerUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.Text;

/**
 * Handles client socket connection to the server.
 * @author Jayson
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class StressRemoteTrancheServer extends RemoteTrancheServer implements TrancheServer {
    // abstract OK byte to keep sync in order
    public static final byte OK_BYTE = 0;
    
    // socket and IO for the data transmission
    Socket dataSocket = null;
    InputStream inputStream = null;
    DataInputStream dataInputStream = null;
    OutputStream outputStream = null;
    DataOutputStream dataOutputStream = null;
    
    // flag for closed state
    boolean closed = false;
    
    // IP address of the server being used
    String host;
    // the port to connect on
    int port;
    
    // if ssl is being used or not
    private boolean secure = false;
    
    // max attempts
    private static int maxRetries = 3;
    
    // buffer of outgoing tasks
    Map<Long, RemoteCallback> outgoingSent = Collections.synchronizedMap(new HashMap());
    List<OutGoingBytes> outgoingQueue = Collections.synchronizedList(new LinkedList());
    
    // buffer all the nonces
    private ArrayList<byte[]> nonces = new ArrayList();
    
    // the threads
    Thread uploadThread = null;
    Thread downloadThread = null;
    
    public StressRemoteTrancheServer(String host, int port) throws Exception {
        this(host, port, false);
    }
    
    public StressRemoteTrancheServer(final String host, final int port, boolean secure) throws Exception {
        super(host,port, secure);
        System.out.println("Closing off parent upload/download threads, ignore.");
        super.close();
        System.out.println("Starting StressRemoteTrancheServer");
        
        // set the ip and port
        this.host = host;
        this.port = port;
        this.secure = secure;
        
        // connect if needed
        reconnect();
        
        // time since last used
        final long[] timeSinceLastUsed = new long[]{System.currentTimeMillis()};
        
        // kick off the upload/download threads
        uploadThread = new Thread() {
           
            public void run() {
//                System.out.println("RTS Started Upload Thread: "+port);
//                try {
                while (!closed || outgoingQueue.size() > 0) {
                    // send up data
                    if (outgoingQueue.size() > 0) {
                        OutGoingBytes ogb = outgoingQueue.remove(0);
                        
                        // try and initial upload of the data
                        try {
//                            System.out.println("Sending Bytes: isOutputShutdown(): "+dataSocket.isOutputShutdown()+", isClosed(): "+dataSocket.isClosed());
                            
                            if (System.currentTimeMillis() - timeSinceLastUsed[0] >= Server.SERVER_TIMEOUT_IN_MILLISECONDS-1000) {
                                throw new Exception("Server timed out!");
                            }
//                                System.out.println("Uploading: "+ogb.id);
                            // write the start byte
                            dataOutputStream.write(OK_BYTE);
                            // write out hte id
                            dataOutputStream.writeLong(ogb.id);
                            // write the length
                            dataOutputStream.writeInt(ogb.bytes.length);
                            // write the data
                            dataOutputStream.write(ogb.bytes);
                            // flush for luck
                            dataOutputStream.flush();
                            outputStream.flush();
                            
                            // update last used
                            timeSinceLastUsed[0] = System.currentTimeMillis();
                            
                            // notify the download thread
                            synchronized (downloadThread) {
                                downloadThread.notify();
                            }
                            // skip past the retry
                            continue;
                        } catch (Exception ioe) {
//                            ioe.printStackTrace(System.out);
                            
                            // if is it closed, break
                            if (closed) break;
                            
                            // Reconnect. This could simply be a closed socket due to server-side timeout.
                            reallyClose(ogb.id);
                            
                            // a brief pause before reconnecting
                            Thread.yield();
                            
                            // attempt to reconnect to the server
                            try {
                                reconnect();
                            }
                            // if we can't reconnect, notify all waiting items
                            catch (Exception ce) {
                                // can't reconnect, no point
                                notifyPreviousCallbacks(Long.MAX_VALUE,"Cannot reconnect to server.");
                                break;
                            }
                        }
                        
                        // try to upload once more -- assuming reconnect was successful
                        try {
                            // write the start byte
                            dataOutputStream.write(OK_BYTE);
                            // write out hte id
                            dataOutputStream.writeLong(ogb.id);
                            // write the length
                            dataOutputStream.writeInt(ogb.bytes.length);
                            // write the data
                            dataOutputStream.write(ogb.bytes);
                            // flush for luck
                            dataOutputStream.flush();
                            outputStream.flush();
                            
                            // update last used
                            timeSinceLastUsed[0] = System.currentTimeMillis();
                            
                            // notify the download thread
                            synchronized (downloadThread) {
                                downloadThread.notify();
                            }
                        }
                        // if any exception is thrown on the second try, tear down the resource and notify the waiting item
                        catch (IOException ioe) {
//                            ioe.printStackTrace();
                            // Reconnect. This could simply be a closed socket due to server-side timeout.
                            reallyClose(Long.MAX_VALUE);
                            break;
                        }
                    }
                    // otherwise wait a litte
                    else {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException ex) {
                            // Another thread is notifying this thread to die
                            break;
                        }
                    }
                } // While open or items left to process
                System.out.println(IOUtil.createURL(StressRemoteTrancheServer.this) + " upload thread exiting");
            }
        };
        uploadThread.setName("Upload Thread: "+IOUtil.createURL(this));
        uploadThread.setDaemon(true);
        uploadThread.start();
        
        // kick off the download thread
        downloadThread = new Thread() {
            public void run() {
//                System.out.println("RTS Started Download Thread: "+port);
//                try {
                try {
                    long id = -1;
                    while (!closed || outgoingSent.size() > 0) {
                        // wait for a notification
                        synchronized (this) {
                            // wait if empty
                            if (outgoingSent.size() == 0) {
                                wait();
                            }
                        }
                        
                        // get the response
                        try {
                            // check for the ok byte
                            int okCheck = dataInputStream.read();
                            // check for the OK byte
                            if (okCheck != OK_BYTE) {
                                throw new IOException("Broken transmission sequence!");
                            }
                            // get the id of the action
                            id = dataInputStream.readLong();
//                            System.out.println("Downloaded: "+id);
                            // figure out how many bytes are in the response
                            int bytesToRead = dataInputStream.readInt();
                            // buffer the bytes
                            byte[] buffer = new byte[bytesToRead];
                            int bytesRead = 0;
                            while (bytesRead < bytesToRead) {
                                bytesRead += dataInputStream.read(buffer, bytesRead, buffer.length - bytesRead);
                            }
                            // remove the pending item
                            RemoteCallback ra = outgoingSent.remove(id);
                            RemoteCallbackRegistry.unregister(ra);
                            
                            // If null, was removed (probably timeout related)
                            if (ra == null)
                                continue;
                            
                            // invoke the callback
                            ra.callback(buffer);
                            
                            // purge anything with less of an id
                            notifyPreviousCallbacks(id,"Purging old requests.");
                        } catch (NullPointerException npe) {
                            System.out.println("Caught null pointer from " + IOUtil.createURL(StressRemoteTrancheServer.this));
                            // Null pointers likely from disk operations, purge callbacks
                            notifyPreviousCallbacks(id+1,"Caught a NullPointerException, likey from disk operations?");
                        } catch (Exception ioe) {
                            // Bad connection, try a new one
                            reallyClose(Long.MAX_VALUE);
                            try {
                                reconnect();
                            } catch (Exception ex) {
                                // Continue, upload thread should repair
                            }
                        }
                    } // While open or items left
                }
                // Interrupt exceptions are expected
                catch (InterruptedException iex) { /* Just ending the thread */ }
                // Other exceptions unexpected
                catch (Exception ex) {
                    System.err.println("Caught unexpected exception: " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
                
                finally {
                    // Interrupt the upload thread to prevent further uploads (bad for servers -- client not service input stream)
                    uploadThread.interrupt();
                    
                    // Purge callbacks so nothings blocks indefinitely
                    Thread.yield();
                    notifyPreviousCallbacks(Long.MAX_VALUE,"Download thread is exitting!");
                    
                    System.out.println(IOUtil.createURL(StressRemoteTrancheServer.this) + " download thread exiting: "+port);
                }
            }
        };
        downloadThread.setName("Download Thread: "+IOUtil.createURL(this));
        downloadThread.setDaemon(true);
        downloadThread.start();
    }
    
    /**
     * connect if needed
     */
    private boolean wasConnected = false;
    
    /**
     * Intelligently connects or reconnects to server.
     */
    @Override()
    protected synchronized void reconnect() throws IOException {
        
        // Make a new connection. No new outgoing items until done!
        synchronized (outgoingQueue) {
            if (!secure) {
                // make a plain socket -- no SSL
                dataSocket = SocketFactory.getDefault().createSocket(host, port);
            } else {
                // make an SSL socket and set it
                SSLSocket ssls = (SSLSocket)SSLSocketFactory.getDefault().createSocket(host, port);
                ssls.setEnabledCipherSuites(ssls.getSupportedCipherSuites());
                dataSocket = ssls;
            }
            
            // disable naggle's algorithm
            dataSocket.setTcpNoDelay(false);
            
            // get the streams
            inputStream = dataSocket.getInputStream();
            outputStream = dataSocket.getOutputStream();
            dataInputStream = new DataInputStream(inputStream);
            dataOutputStream = new DataOutputStream(outputStream);
        }
        
        if (wasConnected) {
            System.out.println("Reconnected to " + IOUtil.createURL(this));
            wasConnected = true;
        }
    }
    
    /**
     * Ends all pending callbacks with ids before a certain id.
     * @param id End all callbacks before this id.
     */
    protected void notifyPreviousCallbacks(long id,String reasonMsg) {
        // purge anything with less of an id
        synchronized (outgoingSent) {
            RemoteCallback[] waitingActions = outgoingSent.values().toArray(new RemoteCallback[0]);
            for (RemoteCallback waitingAction : waitingActions) {
                // check that the id is less and that the outgoing has been sent
                if (waitingAction.id < id) {
                    System.err.println("Purging Missed Action: "+waitingAction.id+", class: "+waitingAction.getClass().getSimpleName()+", "+IOUtil.createURL(this) +" at "+Text.getFormattedDate(System.currentTimeMillis()));
                    System.err.println("  - Reason: "+reasonMsg);
                    RemoteCallbackRegistry.unregister(waitingAction);
                    synchronized (waitingAction) {
                        waitingAction.purge();
                    }
                    // remove the notified action -- the normal thread won't
                    outgoingSent.remove(waitingAction.id);
                }
            }
        }
    }
    
    public int getPort() {
        return port;
    }
    public String getHost(){
        return host;
    }
    
    public byte[] getData(BigHash hash) throws Exception {
        return getDataInternal(hash).getBytes();
    }
    
    public byte[] getMetaData(BigHash hash) throws Exception {
        return getMetaDataInternal(hash).getBytes();
    }
    
    public boolean hasMetaData(BigHash hash) throws Exception {
        return hasMetaDataInternal(hash).getResponse();
    }
    
    public boolean hasData(BigHash hash) throws Exception {
        return hasDataInternal(hash).getResponse();
    }
    
    public byte[] getNonce() throws Exception {
        long start = System.currentTimeMillis();
        
        byte[] nonce = getNonceInternal().getBytes();
        
        int latency = (int)(System.currentTimeMillis() - start);
        submitLatencyNonce(latency);
        
        return nonce;
    }
    
    public void setData(BigHash hash, byte[] data, Signature sig, byte[] nonce) throws Exception {
        setDataInternal(hash, data, sig, nonce).checkForError();
    }
    
    public void setMetaData(BigHash hash, byte[] data, Signature sig, byte[] nonce) throws Exception {
        setMetaDataInternal(hash, data, sig, nonce).checkForError();
    }
    
    public void setRemoteData(String url, BigHash hash, Signature sig, byte[] nonce) throws Exception {
        setRemoteDataInternal(url, hash, sig, nonce).checkForError();
    }
    
    public void setRemoteMetaData(String url, BigHash hash, Signature sig, byte[] nonce) throws Exception {
        setRemoteMetaDataInternal(url, hash, sig, nonce).checkForError();
    }
    
    public BigHash[] getDataHashes(BigInteger offset, BigInteger length) throws Exception {
        return getDataHashesInternal(offset, length).getHashes();
    }
    
    public BigHash[] getMetaDataHashes(BigInteger offset, BigInteger length) throws Exception {
        return getMetaDataHashesInternal(offset, length).getHashes();
    }
    
    public void deleteData(BigHash hash, Signature sig, byte[] nonce) throws Exception {
        deleteDataInternal(hash, sig, nonce).checkForError();
    }
    
    public void deleteMetaData(BigHash hash, Signature sig, byte[] nonce) throws Exception {
        deleteMetaDataInternal(hash, sig, nonce).checkForError();
    }
    
    public void registerServer(String url) throws Exception {
        registerServerInternal(url).checkForError();
    }
    
    public Configuration getConfiguration(Signature sig, byte[] nonce) throws Exception {
        byte[] config = getConfigurationInternal(sig, nonce).getBytes();
        // return the configuration
        return ConfigurationUtil.read(new ByteArrayInputStream(config));
    }
    
    public void setConfiguration(byte[] data, Signature sig, byte[] nonce) throws Exception {
        setConfigurationInternal(data, sig, nonce).checkForError();
    }
    
    public String[] getKnownServers(long offset, long length) throws Exception {
        return getKnownServersInternal(offset, length).getKnownServers();
    }
    /**
     *Remote protocol for sending project hashes.
     */
    public BigHash[] getProjectHashes(BigInteger offset, BigInteger length) throws Exception {
        return getProjectHashesInternal(offset, length).getHashes();
    }
    
// returns the incrementing id
    private static long incrementingId = 0;
    private static synchronized long getNextId() {
        incrementingId++;
        return incrementingId;
    }
    
    
    private GetBytesCallback getDataInternal(BigHash hash) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // write the action
        dos.write(Token.GET_DATA);
        // write the hash
        RemoteUtil.writeData(hash.toByteArray(), baos);
        // flush all bytes
        dos.flush();
        
        synchronized (outgoingQueue) {
            // write the id
            long id = StressRemoteTrancheServer.getNextId();
            // make the output
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            GetBytesCallback gbc = new GetBytesCallback(id, this);
            // add the callback to the queue
            addCallback(id,gbc);
            // add the bytes to send
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return gbc;
        }
    }
    
    private GetBytesCallback getMetaDataInternal(BigHash hash) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // write the action
        dos.write(Token.GET_META);
        // write the hash
        RemoteUtil.writeData(hash.toByteArray(), baos);
        // flush all bytes
        dos.flush();
        
        synchronized (outgoingQueue) {
            // write the id
            long id = StressRemoteTrancheServer.getNextId();
            // make the output
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            GetBytesCallback gbc = new GetBytesCallback(id, this);
            // add the callback to the queue
            addCallback(id, gbc);
            // add the bytes to send
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return gbc;
        }
    }
    
    public BooleanCallback hasDataInternal(BigHash hash) throws Exception {
        // buffer up a nonce request
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // write the action
        dos.write(Token.HAS_DATA);
        // write the hash
        RemoteUtil.writeData(hash.toByteArray(), baos);
        
        // flush all bytes
        dos.flush();
        
        synchronized (outgoingQueue) {
            // write the id
            long id = StressRemoteTrancheServer.getNextId();
            // make the output
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            BooleanCallback hdc = new BooleanCallback(id, this);
            // add the callback to the queue
            addCallback(id, hdc);
            // add the bytes to send
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return hdc;
        }
    }
    
    public BooleanCallback hasMetaDataInternal(BigHash hash) throws Exception {
        // buffer up a nonce request
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // write the action
        dos.write(Token.HAS_META);
        // write the hash
        RemoteUtil.writeData(hash.toByteArray(), baos);
        
        // flush all bytes
        dos.flush();
        
        synchronized (outgoingQueue) {
            // write the id
            long id = StressRemoteTrancheServer.getNextId();
            // make the output
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            BooleanCallback hdc = new BooleanCallback(id, this);
            // add the callback to the queue
            addCallback(id, hdc);
            // add the bytes to send
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return hdc;
        }
    }
    
    private GetBytesCallback getNonceInternal() throws Exception {
        return getNonceInternal(true);
    }
    
    private GetBytesCallback getNonceInternal(boolean useCache) throws Exception {
        // buffer up a nonce request
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // write the action
        dos.write(Token.GET_NONCE);
        // flush all bytes
        dos.flush();
        
        synchronized (outgoingQueue) {
            // write the id
            long id = StressRemoteTrancheServer.getNextId();
            // make the output
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            GetBytesCallback gbc = new GetBytesCallback(id, this);
            // add the callback to the queue
            addCallback(id, gbc);
            // add the bytes to send
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return gbc;
        }
    }
    
    private CheckForErrorCallback setDataInternal(BigHash hash, byte[] data, Signature sig, byte[] nonce) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // request delete
        dos.write(Token.ADD_DATA);
        dos.flush();
        // write the hash
        RemoteUtil.writeData(hash.toByteArray(), dos);
        // write the data
        RemoteUtil.writeData(data, dos);
        // write the signature
        RemoteUtil.writeSignature(sig, dos);
        // write the nonce
        RemoteUtil.writeData(nonce, dos);
        
        synchronized (outgoingQueue) {
            // write out the id
            long id = StressRemoteTrancheServer.getNextId();
            // write out the bytes
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            CheckForErrorCallback cfec = new CheckForErrorCallback(id, this);
            // queue up the bytes and the callback
            addCallback(id, cfec);
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return cfec;
        }
    }
    
    private CheckForErrorCallback setMetaDataInternal(BigHash hash, byte[] data, Signature sig, byte[] nonce) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // request delete
        dos.write(Token.ADD_META);
        dos.flush();
        // write the hash
        RemoteUtil.writeData(hash.toByteArray(), dos);
        // write the data
        RemoteUtil.writeData(data, dos);
        // write the signature
        RemoteUtil.writeSignature(sig, dos);
        // write the nonce
        RemoteUtil.writeData(nonce, dos);
        
        synchronized (outgoingQueue) {
            // write out the id
            long id = StressRemoteTrancheServer.getNextId();
            // write out the bytes
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            CheckForErrorCallback cfec = new CheckForErrorCallback(id, this);
            // queue up the bytes and the callback
            addCallback(id, cfec);
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return cfec;
        }
    }
    
    private CheckForErrorCallback setRemoteDataInternal(String url, BigHash hash, Signature sig, byte[] nonce) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // request delete
        dos.write(Token.ADD_DATA_REMOTE);
        dos.flush();
        // Write the URL
        RemoteUtil.writeLine(url, baos);
        // write the meta-data
        RemoteUtil.writeData(hash.toByteArray(), baos);
        // Write the sig
        RemoteUtil.writeSignature(sig, baos);
        // write the nonce
        RemoteUtil.writeData(nonce, baos);
        
        synchronized (outgoingQueue) {
            // write out the id
            long id = StressRemoteTrancheServer.getNextId();
            // write out the bytes
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            CheckForErrorCallback cfec = new CheckForErrorCallback(id, this);
            // queue up the bytes and the callback
            addCallback(id, cfec);
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return cfec;
        }
    }
    
    private CheckForErrorCallback setRemoteMetaDataInternal(String url, BigHash hash, Signature sig, byte[] nonce) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // request delete
        dos.write(Token.ADD_META_REMOTE);
        dos.flush();
        // Write the URL
        RemoteUtil.writeLine(url, baos);
        // write the meta-data
        RemoteUtil.writeData(hash.toByteArray(), baos);
        // Write the sig
        RemoteUtil.writeSignature(sig, baos);
        // write the nonce
        RemoteUtil.writeData(nonce, baos);
        
        synchronized (outgoingQueue) {
            // write out the id
            long id = StressRemoteTrancheServer.getNextId();
            // write out the bytes
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            CheckForErrorCallback cfec = new CheckForErrorCallback(id, this);
            // queue up the bytes and the callback
            addCallback(id, cfec);
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return cfec;
        }
    }
    
    public GetHashesCallback getDataHashesInternal(BigInteger offset, BigInteger length) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // request hashes
        dos.write(Token.GET_DATA_HASHES);
        RemoteUtil.writeData(offset.toByteArray(), dos);
        RemoteUtil.writeData(length.toByteArray(), dos);
        dos.flush();
        
        synchronized (outgoingQueue) {
            // write out the id
            long id = StressRemoteTrancheServer.getNextId();
            // write out the bytes
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            GetHashesCallback ghc = new GetHashesCallback(id, this);
            // queue up the bytes and the callback
            addCallback(id, ghc);
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return ghc;
        }
    }
    
    private GetHashesCallback getMetaDataHashesInternal(BigInteger offset, BigInteger length) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // request hashes
        dos.write(Token.GET_META_HASHES);
        RemoteUtil.writeData(offset.toByteArray(), dos);
        RemoteUtil.writeData(length.toByteArray(), dos);
        dos.flush();
        
        synchronized (outgoingQueue) {
            // write out the id
            long id = StressRemoteTrancheServer.getNextId();
            // write out the bytes
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            GetHashesCallback ghc = new GetHashesCallback(id, this);
            // queue up the bytes and the callback
            addCallback(id, ghc);
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return ghc;
        }
    }
    
    private CheckForErrorCallback registerServerInternal(String url) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // request delete
        dos.write(Token.ADD_SERVER);
        dos.write(url.getBytes());
        dos.write(Token.EOL);
        dos.flush();
        
        synchronized (outgoingQueue) {
            // write out the id
            long id = StressRemoteTrancheServer.getNextId();
            // write out the bytes
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            CheckForErrorCallback cfec = new CheckForErrorCallback(id, this);
            // queue up the bytes and the callback
            addCallback(id, cfec);
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return cfec;
        }
    }
    
    
    private CheckForErrorCallback deleteMetaDataInternal(BigHash hash, Signature sig, byte[] nonce) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // request delete
        dos.write(Token.DELETE_META);
        dos.flush();
        // write the meta-data
        RemoteUtil.writeData(hash.toByteArray(), baos);
        // Write the sig
        RemoteUtil.writeSignature(sig, baos);
        // write the nonce
        RemoteUtil.writeData(nonce, baos);
        
        synchronized (outgoingQueue) {
            // write out the id
            long id = StressRemoteTrancheServer.getNextId();
            // write out the bytes
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            CheckForErrorCallback cfec = new CheckForErrorCallback(id, this);
            // queue up the bytes and the callback
            addCallback(id, cfec);
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return cfec;
        }
    }
    
    private CheckForErrorCallback deleteDataInternal(BigHash hash, Signature sig, byte[] nonce) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // request delete
        dos.write(Token.DELETE_DATA);
        dos.flush();
        // write the meta-data
        RemoteUtil.writeData(hash.toByteArray(), baos);
        // Write the sig
        RemoteUtil.writeSignature(sig, baos);
        // write the nonce
        RemoteUtil.writeData(nonce, baos);
        
        synchronized (outgoingQueue) {
            // write out the id
            long id = StressRemoteTrancheServer.getNextId();
            // write out the bytes
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            CheckForErrorCallback cfec = new CheckForErrorCallback(id, this);
            // queue up the bytes and the callback
            addCallback(id, cfec);
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return cfec;
        }
    }
    
    private GetBytesCallback getConfigurationInternal(Signature sig, byte[] nonce) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // rask for the configuration
        dos.write(Token.GET_CONFIGURATION);
        // write the big hash
        RemoteUtil.writeData(nonce, dos);
        // write the signature
        RemoteUtil.writeSignature(sig, dos);
        // flush all bytes
        dos.flush();
        
        synchronized (outgoingQueue) {
            // write the id
            long id = StressRemoteTrancheServer.getNextId();
            // make the output
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            GetBytesCallback gbc = new GetBytesCallback(id, this);
            // add the callback to the queue
            addCallback(id, gbc);
            // add the bytes to send
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return gbc;
        }
    }
    
    private CheckForErrorCallback setConfigurationInternal(byte[] data, Signature sig, byte[] nonce) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // request delete
        dos.write(Token.SET_CONFIGURATION);
        dos.flush();
        // write the big other
        RemoteUtil.writeData(data, dos);
        // write the signature
        RemoteUtil.writeSignature(sig, dos);
        // write the nonce
        RemoteUtil.writeData(nonce, dos);
        dos.flush();
        
        synchronized (outgoingQueue) {
            // write out the id
            long id = StressRemoteTrancheServer.getNextId();
            // write out the bytes
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            CheckForErrorCallback cfec = new CheckForErrorCallback(id, this);
            // queue up the bytes and the callback
            addCallback(id, cfec);
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return cfec;
        }
    }
    
    public GetKnownServersCallback getKnownServersInternal(long offset, long length) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // request urls
        dos.write(Token.GET_KNOWN_SERVERS);
        // write the offset/length
        RemoteUtil.writeLong(offset, dos);
        RemoteUtil.writeLong(length, dos);
        dos.flush();
        
        synchronized (outgoingQueue) {
            // write out the id
            long id = StressRemoteTrancheServer.getNextId();
            // write out the bytes
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            GetKnownServersCallback gksc = new GetKnownServersCallback(id, this);
            // queue up the bytes and the callback
            addCallback(id, gksc);
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return gksc;
        }
    }
    
    private void throwSpecialExceptions(final Exception e) throws IOException {
        // pitch certain exceptions
        if (e != null && e.getMessage() != null && e.getMessage().contains(Token.REMOTE_REPLICATE_ERROR)) throw new IOException(e.getMessage());
        if (e != null && e.getMessage() != null && e.getMessage().contains(Token.SECURITY_ERROR)) throw new IOException(e.getMessage());
        if (e != null && e.getMessage() != null && e.getMessage().contains(Token.REQUEST_ERROR)) throw new FileNotFoundException(e.getMessage());
        if (e != null && e.getMessage() != null && e.getMessage().contains(Token.SPACE_ERROR)) throw new OutOfDiskSpaceException(e.getMessage() + " For server at " + IOUtil.createURL(this) + ".");
    }
    
    public synchronized void reallyClose(long cutoff) {
        // close things down
        IOUtil.safeClose(dataInputStream);
        IOUtil.safeClose(dataOutputStream);
        
        // don't close firewalled connections
        try { dataSocket.shutdownInput(); } catch (Exception e){}
        try { dataSocket.shutdownOutput(); } catch (Exception e){}
        try { dataSocket.close(); } catch (Exception e){}
        
        // Purge all previous callbacks. They are no longer valid.
        notifyPreviousCallbacks(cutoff,"Called reallyClose -- perhaps to reopen? Or to shutdown?");
    }
    
    /**
     *Tears down the TCP connection to the remote server and releases the associated resources. If another method is invoked by this RemoteDistributedFileSystem, the TCP connection will automatically be re-established. i.e. this method is a way to terminate lingering TCP connections that likly won't be needed.
     */
    public synchronized void close() {
        // check if this is still in the cache
        boolean hasInCache = IOUtil.remoteCache.get(IOUtil.createURL(this)) == this;
        if (hasInCache) {
            return;
        }
        
        // skip if closed
        if (closed) return;
        // flag as closed
        closed = true;
        
        // otherwise try to politely tear down the connection
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(Token.CLOSE);
            // out going bytes
            OutGoingBytes ogb = new OutGoingBytes(StressRemoteTrancheServer.getNextId(), baos.toByteArray());
            outgoingQueue.add(ogb);
        } catch (Exception e){
            // handle any exceptions
        }
        
        // wait for the upload/download threads to finish
        try {
            uploadThread.join(500);
            if (uploadThread.isAlive()) {
                synchronized (uploadThread) {
                    uploadThread.interrupt();
                }
            }
        } catch (Exception e) {}
        
        // really close
        reallyClose(Long.MAX_VALUE);
        
        try {
            downloadThread.join(500);
            if (downloadThread.isAlive()) {
                synchronized (downloadThread) {
                    downloadThread.interrupt();
                }
            }
        } catch (Exception e) {}
        
        // remove from the IOUtil cache
        IOUtil.remoteCache.remove(IOUtil.createURL(this));
    }
    
    public boolean isSecure() {
        return secure;
    }
    
    public boolean equals(Object obj) {
        // specially compare to other rdfs
        if (obj instanceof StressRemoteTrancheServer) {
            StressRemoteTrancheServer a = (StressRemoteTrancheServer)obj;
            return IOUtil.createURL(this).equals(IOUtil.createURL(a));
        }
        return super.equals(obj);
    }
    
    public int hashCode() {
        return IOUtil.createURL(this).hashCode();
    }
    
    private GetHashesCallback getProjectHashesInternal(BigInteger offset, BigInteger length) throws Exception {
        // buffer the bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // request hashes
        dos.write(Token.GET_PROJECT_HASHES);
        RemoteUtil.writeData(offset.toByteArray(), dos);
        RemoteUtil.writeData(length.toByteArray(), dos);
        dos.flush();
        
        synchronized (outgoingQueue) {
            // write out the id
            long id = StressRemoteTrancheServer.getNextId();
            // write out the bytes
            OutGoingBytes ogb = new OutGoingBytes(id, baos.toByteArray());
            // make the callback
            GetHashesCallback ghc = new GetHashesCallback(id, this);
            // queue up the bytes and the callback
            addCallback(id, ghc);
            this.outgoingQueue.add(ogb);
            
            // return the callback
            return ghc;
        }
    }
    
    public static int getMaxRetries() {
        return maxRetries;
    }
    
    public static void setMaxRetries(int aMaxAttempts) {
        maxRetries = aMaxAttempts;
    }
    
    /**
     * Submit the latency of the chunk to server. Tries once then quits.
     */
    public void submitLatencyChunk(int latency) {
        String url = IOUtil.createURL(this);
        ServerInfo info  = ServerUtil.getServerInfo(url);
        
        // In some test cases, may not get a server info obj
        if (info != null)
            info.adjustAvgLatency1MB(latency);
    }
    
    /**
     * Submit the latency of the nonce to server. Tries once then quits.
     */
    public void submitLatencyNonce(int latency) {
        String url = IOUtil.createURL(this);
        ServerInfo info = ServerUtil.getServerInfo(url);
        
        // In some test cases, may not get a server info obj
        if (info != null)
            info.adjustAvgLatencyNonce(latency);
    }
    
    /**
     * Add a callback to outgoing and register it with timeout registry.outgoingSent
     */
    private void addCallback(long id, RemoteCallback callback) {
        this.outgoingSent.put(id,callback);
        RemoteCallbackRegistry.register(callback,StressRemoteTrancheServer.this);
    }
}
