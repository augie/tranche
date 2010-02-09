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

import java.io.DataInputStream;
import org.tranche.ConfigureTranche;
import org.tranche.network.ConnectionUtil;
import org.tranche.util.IOUtil;
import org.tranche.time.TimeUtil;

/**
 * <p>The thread to be used to receive messages from a server.</p>
 */
public class RemoteTrancheServerDownloadThread extends Thread {

    private final RemoteTrancheServer rts;

    /**
     * 
     * @param rts
     */
    public RemoteTrancheServerDownloadThread(RemoteTrancheServer rts) {
        setDaemon(true);
        this.rts = rts;
    }

    /**
     * <p>Runs the download thread.</p>
     */
    @Override()
    public void run() {
        while (!rts.isClosed()) {
            long id = -1;
            try {
                // wait for a notification
                boolean isEmpty = false;
                synchronized (rts.outgoingSent) {
                    isEmpty = rts.outgoingSent.isEmpty();
                }
                if (isEmpty) {
                    try {
                        synchronized (this) {
                            wait(5000);
                        }
                        continue;
                    } catch (Exception e) {
                        RemoteTrancheServer.debugErr(e);
                    }
                }

                // Keep track of last two times woke up from wait.
                this.rts.setLastTimeWakeUp(TimeUtil.getTrancheTimestamp());

                RemoteTrancheServer.debugOut("Server " + rts.getHost() + "; Released block on download thread");

                DataInputStream dis = rts.getDataInputStream();
                // check for the ok byte
                int okCheck = dis.read();
                // check for the OK byte
                if (okCheck != RemoteTrancheServer.OK_BYTE) {
                    throw new Exception("Server " + rts.getHost() + "; Broken transmission sequence! Expected '" + RemoteTrancheServer.OK_BYTE + "', but found '" + okCheck + "'");
                }

                // get the id of the action
                id = dis.readLong();
                // figure out how many bytes are in the response
                int bytesToRead = dis.readInt();
                // buffer the bytes
                byte[] buffer = new byte[bytesToRead];

                rts.fireStartedDownloadingBytes(id, bytesToRead);

                // download
                int bytesRead = 0;
                while (bytesRead < bytesToRead) {
                    int val = dis.read(buffer, bytesRead, buffer.length - bytesRead);
                    if (val == -1) {
                        throw new Exception("Closed IO? Returned " + val);
                    }
                    bytesRead += val;
                    // notify the listeners
                    rts.fireUpdateDownloadingBytes(id, bytesToRead, bytesRead);
                }

                RemoteTrancheServer.debugOut("Server " + IOUtil.createURL(rts) + "; Reading Bytes: finished; ID: " + id + "; Bytes: " + buffer.length);

                // Update time of last server response
                rts.setTimeLastServerResponse(TimeUtil.getTrancheTimestamp());
                rts.setTimeLastUsed(TimeUtil.getTrancheTimestamp());

                // is this a keep alive signal?
                if (buffer.length == Token.KEEP_ALIVE.length && new String(buffer).trim().equals(Token.KEEP_ALIVE_STRING.trim())) {
                    RemoteCallback rc = null;
                    synchronized (rts.outgoingSent) {
                        rc = rts.outgoingSent.get(id);
                    }
                    // If null, was removed (probably timeout related)
                    if (rc == null) {
                        continue;
                    }
                    // has this been kept alive too long?
                    long timeout = ConfigureTranche.getLong(ConfigureTranche.PROP_SERVER_KEEP_ALIVE_TIMEOUT);
                    RemoteTrancheServer.debugOut("Keep-alive timeout: " + timeout);
                    if (timeout == 0 || TimeUtil.getTrancheTimestamp() - rc.getTimeStarted() < timeout) {
                        RemoteTrancheServer.debugOut("Keeping alive " + rts.getHost());
                        rc.keepAlive();
                        continue;
                    } else {
                        RemoteTrancheServer.debugOut("Keep-alive timed out: " + rts.getHost());
                        // remove
                        synchronized (rts.outgoingSent) {
                            rts.outgoingSent.remove(id);
                        }
                        // unregister the callback
                        RemoteCallbackRegistry.unregister(rc);
                        // wake up threads waiting and the item will time out
                        rc.notifyTimedOut();
                        continue;
                    }
                }

                // remove the pending item
                rts.fireFinishedDownloadingBytes(id, buffer.length, buffer.length);

                RemoteCallback ra = null;
                synchronized (rts.outgoingSent) {
                    ra = rts.outgoingSent.remove(id);
                }

                // unregister the callback
                RemoteCallbackRegistry.unregister(ra);

                // If null, was removed (probably timeout related)
                if (ra == null) {
                    continue;
                }

                // invoke the callback
                ra.callback(buffer);
            } catch (NullPointerException npe) {
                RemoteTrancheServer.debugErr(npe);
            } catch (InterruptedException ie) {
                RemoteTrancheServer.debugErr(ie);
            } catch (Exception e) {
                if (id != -1) {
                    rts.fireFailedDownloadingBytes(id);
                }
                RemoteTrancheServer.debugErr(e);
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
