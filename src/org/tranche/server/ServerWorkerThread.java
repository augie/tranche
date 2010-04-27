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
package org.tranche.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.tranche.ConfigureTranche;
import org.tranche.commons.DebuggableThread;
import org.tranche.commons.TextUtil;
import org.tranche.exceptions.ChunkDoesNotBelongException;
import org.tranche.exceptions.ServerIsNotReadableException;
import org.tranche.exceptions.ServerIsNotWritableException;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.util.IOUtil;
import org.tranche.logs.LogUnit;
import org.tranche.logs.SimpleLog;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.time.TimeUtil;
import org.tranche.commons.ThreadUtil;

/**
 * <p>Handles a client request.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServerWorkerThread extends DebuggableThread {

    private static boolean isTestingKeepAlive = false;
    private static final long KEEP_ALIVE_THRESHOLD = RemoteTrancheServer.getResponseTimeout() / 10;
    private final Socket s;
    private final Server server;
    private final ArrayBlockingQueue<ServerWorkerThreadQueueItem> queue = new ArrayBlockingQueue((ConfigureTranche.getInt(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_QUEUE_SIZE) * 4) + 1);
    private final Set<Long> currentlyWorkingItems = new HashSet<Long>();
    private final Set<ServerWorkerOutputThread> outputThreads = new HashSet<ServerWorkerOutputThread>();
    private final ServerWorkerKeepAliveThread keepAliveThread = new ServerWorkerKeepAliveThread();
    private boolean run = true;
    private OutputStream os;
    private BufferedOutputStream bos;
    private DataOutputStream dos;

    /**
     * @param s The socket received
     * @param server The server received
     */
    public ServerWorkerThread(Socket s, Server server) {
        setName("Server Worker Thread");
        setDaemon(true);
        this.s = s;
        this.server = server;
    }

    /**
     * <p>Returns the socket associated with the handling of client requests..</p>
     * @return
     */
    public Socket getSocket() {
        return s;
    }

    /**
     * 
     * @param bufferID
     * @param bytes
     * @throws java.lang.Exception
     */
    private void sendOutput(long bufferID, byte[] bytes) throws Exception {
        if (dos != null) {
            debugOut("Server " + IOUtil.createURL(server.getHostName(), server.getPort(), server.isSSL()) + "; sending output (ID = " + bufferID + ", bytes = " + bytes.length + ")");
            synchronized (dos) {
                dos.write(RemoteTrancheServer.OK_BYTE);
                // send back id
                dos.writeLong(bufferID);
                // send back the size
                dos.writeInt(bytes.length);
                // send back the bytes
                dos.write(bytes);
                // flush
                dos.flush();
                bos.flush();
                os.flush();
            }
        }
    }

    private void sendError(long queueItemID, Exception e) throws Exception {
        // send back the error -- don't close the communication channel. Won't disrupt other actions
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            // close the socket
            RemoteUtil.writeError(e.getMessage(), baos);
            // optionally send the local server info
            if (e instanceof ServerIsNotReadableException || e instanceof ServerIsNotWritableException || e instanceof ChunkDoesNotBelongException) {
                NetworkUtil.getLocalServerRow().serialize(StatusTableRow.VERSION_LATEST, baos);
            }
            // send back the data
            sendOutput(queueItemID, baos.toByteArray());
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    @Override
    public void run() {
        InputStream in = null;
        BufferedInputStream bin = null;
        DataInputStream dis = null;
        try {
            // make the streams
            in = s.getInputStream();
            bin = new BufferedInputStream(in);
            dis = new DataInputStream(bin);
            os = s.getOutputStream();
            bos = new BufferedOutputStream(os);
            dos = new DataOutputStream(bos);

            // start the keep alive thread
            keepAliveThread.start();
            // create some output threads
            int outputThreadCount = ConfigureTranche.getInt(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_USER_SIMULTANEOUS_REQUESTS);
            debugOut("Host address: " + s.getInetAddress().getHostAddress() + " (Is startup server? " + NetworkUtil.isStartupServer(s.getInetAddress().getHostAddress()) + ")");
            debugOut("Host name: " + s.getInetAddress().getHostName() + " (Is startup server? " + NetworkUtil.isStartupServer(s.getInetAddress().getHostName()) + ")");
            if (NetworkUtil.isStartupServer(s.getInetAddress().getHostAddress()) || NetworkUtil.isStartupServer(s.getInetAddress().getHostName())) {
                outputThreadCount = ConfigureTranche.getInt(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_SERVER_SIMULTANEOUS_REQUESTS);
            }
            for (int i = 0; i < outputThreadCount; i++) {
                ServerWorkerOutputThread thread = new ServerWorkerOutputThread();
                thread.start();
                outputThreads.add(thread);
            }

            // handle commands
            while (!s.isClosed() && !s.isInputShutdown()) {
                LogUnit logInput = new LogUnit(SimpleLog.SWT_LOG, "Input: " + s.getInetAddress() + "; Port: " + s.getPort());
                try {
                    // get a byte
                    byte okCheck = -1;
                    try {
                        okCheck = (byte) dis.read();
                        logInput.log("OK byte recieved: " + okCheck);
                    } catch (SocketTimeoutException stoe) {
                        break;
                    }

                    // if it is -1, break
                    if (okCheck == -1) {
                        return;
                    }
                    // check for the OK byte
                    if (okCheck != RemoteTrancheServer.OK_BYTE) {
                        logInput.log("Incorrect OK byte. Transmission out of sequence.");
                        break;
                    }

                    logInput.log("Reading: started");
                    // get the id
                    long id = dis.readLong();

                    // figure out how many bytes are in this serverItem
                    int bytesToRead = dis.readInt();

                    if (bytesToRead > this.server.getMaxRequestSize()) {
                        logInput.log("Request size of " + bytesToRead + " greater than maximum of " + this.server.getMaxRequestSize() + ", rejecting.");

                        // Skip the bytes for request: rejected
                        dis.skipBytes(bytesToRead);

                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            server.getItem(Token.REJECTED_CONNECTION_STRING).doAction(new ByteArrayInputStream(new byte[0]), baos, s.getInetAddress().getHostAddress());
                            sendOutput(id, baos.toByteArray());
                        } catch (Exception e) {
                            sendError(id, e);
                        }
                    } else {
                        // queueItem the bytes
                        byte[] buffer = new byte[bytesToRead];
                        int bytesRead = 0;
                        while (bytesRead < bytesToRead) {
                            bytesRead += dis.read(buffer, bytesRead, buffer.length - bytesRead);
                        }
                        logInput.log("Reading: Finished; ID: " + id + "; Bytes: " + buffer.length + "; Queuing: started");
                        debugOut("Server " + IOUtil.createURL(server.getHostName(), server.getPort(), server.isSSL()) + "; Received request (ID = " + id + ", bytes = " + buffer.length + ")");

                        String itemName = null;
                        ByteArrayInputStream bais = null;
                        try {
                            try {
                                bais = new ByteArrayInputStream(buffer);
                                itemName = RemoteUtil.readLine(bais);
                            } finally {
                                IOUtil.safeClose(bais);
                            }
                        } catch (Exception e) {
                            debugErr(e);
                        }

                        if (itemName != null && itemName.equals(Token.PING_STRING)) {
                            try {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                server.getItem(itemName).doAction(bais, baos, s.getInetAddress().getHostAddress());
                                sendOutput(id, baos.toByteArray());
                            } catch (Exception e) {
                                sendError(id, e);
                            }
                        } else {
                            // add to the queue, blocking if full
                            queue.put(new ServerWorkerThreadQueueItem(id, buffer));
                        }
                        logInput.log("Queuing: Stopped");
                    }
                } finally {
                    SimpleLog.log(logInput);
                }
            }
        } catch (SocketException ste) {
            debugErr(ste);
            SimpleLog.log(new LogUnit(SimpleLog.SWT_LOG, "Socket Exception: Exiting Worker."));
        } catch (SocketTimeoutException stoe) {
            debugErr(stoe);
            LogUnit tol = new LogUnit(SimpleLog.SWT_LOG, "Socket Timeout: Exiting Worker.");
            SimpleLog.log(tol);
            LogUnit exception = new LogUnit(SimpleLog.SWT_LOG, "SocketTimeoutException in SWT input thread: " + stoe.getMessage());
            for (StackTraceElement ste : stoe.getStackTrace()) {
                exception.log(ste.toString());
            }
            SimpleLog.log(exception);
        } catch (Exception e) {
            debugErr(e);
            LogUnit tol = new LogUnit(SimpleLog.SWT_LOG, "Non-Socket Exception: Exiting Worker.");
            tol.log(e.getMessage());
            SimpleLog.log(tol);
        } finally {
            // flag off the run
            run = false;

            // shutdown the input
            try {
                s.shutdownInput();
            } catch (Exception e) {
            }
            IOUtil.safeClose(dis);
            IOUtil.safeClose(bin);
            IOUtil.safeClose(in);

            LogUnit lu = new LogUnit(SimpleLog.SWT_LOG, "Exiting worker main thread (input thread) for " + s.getInetAddress() + " at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            SimpleLog.log(lu);

            // turn off the output
            synchronized (outputThreads) {
                for (ServerWorkerOutputThread outputThread : outputThreads) {
                    try {
                        outputThread.join(10000);
                    } catch (InterruptedException ex) {
                    }
                    // interrupt if needed
                    if (outputThread.isAlive()) {
                        outputThread.interrupt();
                    }
                }
            }
        }
    }

    /**
     * 
     * @param isTestingKeepAlive
     */
    public static void setTestingKeepAlive(boolean isTestingKeepAlive) {
        ServerWorkerThread.isTestingKeepAlive = isTestingKeepAlive;
    }

    /**
     * <p>Helper class to associate unique identifiers with bytes.</p>
     */
    private class ServerWorkerThreadQueueItem {

        public final long id;
        public final byte[] bytes;

        /**
         * 
         * @param id
         * @param bytes
         */
        public ServerWorkerThreadQueueItem(long id, byte[] bytes) {
            debugOut("Created new queue item with ID " + id + " and size " + bytes.length);
            this.id = id;
            this.bytes = bytes;
        }
    }

    /**
     * 
     */
    private class ServerWorkerOutputThread extends Thread {

        /**
         * 
         */
        public ServerWorkerOutputThread() {
            setName("Server Worker Output Thread");
            setDaemon(true);
        }

        /**
         * 
         */
        @Override()
        public void run() {
            try {
                // while running
                while (run || queue.size() > 0) {
                    // wake up and keep alive
                    if (keepAliveThread.millisSinceLastSignal() > KEEP_ALIVE_THRESHOLD) {
                        synchronized (keepAliveThread) {
                            keepAliveThread.notify();
                        }
                        // let the keep-alive thread execute
                        Thread.yield();
                    }

                    // get the next serverItem
                    ServerWorkerThreadQueueItem queueItem = queue.poll(1000, TimeUnit.MILLISECONDS);

                    // if nothing is in the queue, continue
                    if (queueItem == null) {
                        continue;
                    }

                    // otherwise process the bytes
                    // queueItem the input and pass it to the server
                    ByteArrayInputStream bais = new ByteArrayInputStream(queueItem.bytes);

                    // read the command line
                    String line = RemoteUtil.readLine(bais);

                    // handle the remote command
                    try {
                        ServerItem serverItem = server.getItem(line);
                        if (serverItem == null) {
                            throw new Exception("Can't find command " + line);
                        }

                        debugOut("Processing server item: " + serverItem.getName());

                        // queueItem the output
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        // add to set of currently working items
                        synchronized (currentlyWorkingItems) {
                            currentlyWorkingItems.add(queueItem.id);
                        }

                        // pause here
                        if (isTestingKeepAlive) {
                            ThreadUtil.sleep(1 * RemoteTrancheServer.getResponseTimeout() + 1);
                        }

                        // send a keep-alive signal
                        sendOutput(queueItem.id, Token.KEEP_ALIVE);

                        // do the action
                        serverItem.doAction(bais, baos, s.getInetAddress().getHostAddress());

                        // remove from currently working items
                        synchronized (currentlyWorkingItems) {
                            currentlyWorkingItems.remove(queueItem.id);
                        }

                        // send back the data
                        sendOutput(queueItem.id, baos.toByteArray());

                        // check for close -- a special case of actions
                        if (line.equals(Token.CLOSE_STRING)) {
                            // return. the finally clause will safely close resources
                            return;
                        }
                    } catch (Exception e) {
                        sendError(queueItem.id, e);
                    }
                }
            } catch (Exception e) {
                debugErr(e);
                return;
            } finally {
                // flag off run
                run = false;
                // shutdown resources
                try {
                    s.shutdownInput();
                } catch (Exception e) {
                }
                try {
                    s.shutdownOutput();
                } catch (Exception e) {
                }
                // close down io
                IOUtil.safeClose(os);
                IOUtil.safeClose(bos);
                IOUtil.safeClose(dos);
                // close the socket itself
                try {
                    s.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * 
     */
    private class ServerWorkerKeepAliveThread extends Thread {

        private long lastSignalTimestamp = TimeUtil.getTrancheTimestamp();

        /**
         *
         */
        public ServerWorkerKeepAliveThread() {
            setName("Server Worker Keep Alive Thread");
            setDaemon(true);
            setPriority(Thread.MAX_PRIORITY);
        }

        /**
         * 
         * @return
         */
        public long millisSinceLastSignal() {
            return TimeUtil.getTrancheTimestamp() - lastSignalTimestamp;

        }

        /**
         *
         */
        @Override()
        public void run() {
            while (run) {
                try {
                    synchronized (this) {
                        wait(KEEP_ALIVE_THRESHOLD);
                    }

                    LinkedList<Long> ids = new LinkedList<Long>();
                    synchronized (currentlyWorkingItems) {
                        ids.addAll(currentlyWorkingItems);
                    }
                    for (ServerWorkerThreadQueueItem item : queue.toArray(new ServerWorkerThreadQueueItem[0])) {
                        ids.add(item.id);
                    }
                    for (Long id : ids) {
                        debugOut("Sending keep alive signal: " + id);
                        sendOutput(id, Token.KEEP_ALIVE);
                    }

                    debugOut("Time since last keep alive signal: " + TextUtil.formatTimeLength(TimeUtil.getTrancheTimestamp() - lastSignalTimestamp));
                    lastSignalTimestamp = TimeUtil.getTrancheTimestamp();
                } catch (Exception e) {
                    debugErr(e);
                    // flag off the run
                    run = false;

                    // shutdown the input
                    try {
                        s.shutdownInput();
                    } catch (Exception ee) {
                    }

                    // turn off the output
                    synchronized (outputThreads) {
                        for (ServerWorkerOutputThread outputThread : outputThreads) {
                            try {
                                outputThread.join(10000);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            // interrupt if needed
                            if (outputThread.isAlive()) {
                                outputThread.interrupt();
                            }
                        }
                    }
                }
            }
        }
    }
}
