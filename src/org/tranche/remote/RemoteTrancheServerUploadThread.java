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

import java.io.DataOutputStream;
import org.tranche.commons.DebuggableThread;
import org.tranche.network.ConnectionUtil;
import org.tranche.time.TimeUtil;

/**
 * <p>The thread to be used to send messages to a server.</p>
 */
public class RemoteTrancheServerUploadThread extends DebuggableThread {

    private final RemoteTrancheServer rts;

    /**
     * 
     * @param rts
     */
    public RemoteTrancheServerUploadThread(RemoteTrancheServer rts) {
        setDaemon(true);
        this.rts = rts;
    }

    /**
     * <p>Runs the thread.</p>
     */
    @Override()
    public void run() {
        while (!rts.isClosed()) {
            OutGoingBytes ogb = null;
            try {
                if (rts.dataSocket == null) {
                    try {
                        synchronized (this) {
                            wait(2000);
                        }
                    } catch (Exception e) {
                        debugErr(e);
                    }
                    continue;
                }
                // check for connection
                if (!rts.dataSocket.isConnected() || rts.dataSocket.isClosed() || rts.dataSocket.isInputShutdown() || rts.dataSocket.isOutputShutdown()) {
                    throw new ReconnectException("");
                }

                // pull from the queue
                synchronized (rts.outgoingQueue) {
                    if (!rts.outgoingQueue.isEmpty()) {
                        ogb = rts.outgoingQueue.removeFirst();
                        rts.outgoingQueue.notify();
                    }
                }
                if (ogb == null) {
                    try {
                        synchronized (this) {
                            wait(2000);
                        }
                    } catch (Exception e) {
                        debugErr(e);
                    }
                    continue;
                }
                debugOut("ID: " + ogb.id + "; Bytes: " + ogb.bytes.length);

                rts.fireStartedUploadingBytes(ogb.id, ogb.bytes.length);
                debugOut("Writing ID #" + ogb.id);

                // shouldn't have to synchronized, but will anyhow
                DataOutputStream dos = rts.getDataOutputStream();
                // write the start byte
                dos.write(RemoteTrancheServer.OK_BYTE);
                // write out the id
                dos.writeLong(ogb.id);
                // write the length
                dos.writeInt(ogb.bytes.length);
                // write the data
                dos.write(ogb.bytes);
                // flush for luck
                rts.flushOutputStreams();

                debugOut("Finished writing ID #" + ogb.id);
                rts.fireFinishedUploadingBytes(ogb.id, ogb.bytes.length, ogb.bytes.length);

                // update last used
                rts.setTimeLastUsed(TimeUtil.getTrancheTimestamp());
                // notify the download thread
                this.rts.setLastTimeNotify(TimeUtil.getTrancheTimestamp());
                synchronized (rts.downloadThread) {
                    rts.downloadThread.notify();
                }
            } catch (NullPointerException npe) {
                debugErr(npe);
            } catch (ReconnectException re) {
                // attempt to reconnect
                try {
                    rts.reconnect();
                } catch (Exception ee) {
                    ConnectionUtil.reportExceptionHost(rts.getHost(), ee);
                }
            } catch (Exception e) {
                if (ogb != null) {
                    rts.fireFailedUploadingBytes(ogb.id);
                }
                debugErr(e);
                // attempt to reconnect
                try {
                    rts.reconnect();
                } catch (Exception ee) {
                    ConnectionUtil.reportExceptionHost(rts.getHost(), ee);
                }
            }
        }
    }
}
