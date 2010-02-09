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

import java.util.concurrent.TimeoutException;
import org.tranche.exceptions.UnresponsiveServerException;
import org.tranche.time.TimeUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.Text;

/**
 * <p>Abstract remote callback with common functionality.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public abstract class RemoteCallback {

    private static boolean debug = false;
    /**
     * <p>Flag meaning the callback is not fulfilled.</p>
     */
    public static final long NOT_COMPLETED = -1;
    private RemoteTrancheServer rts;
    private String name, description, purgeMsg;
    private long id, timeCompleted = NOT_COMPLETED, timeStarted = TimeUtil.getTrancheTimestamp();
    private Exception cachedException;
    private final long created;
    private boolean keepAlive = false;

    /**
     * 
     * @param id
     * @param rts
     * @param name
     * @param description
     */
    public RemoteCallback(long id, RemoteTrancheServer rts, String name, String description) {
        this.id = id;
        this.rts = rts;
        this.name = name;
        this.description = description;
        this.created = TimeUtil.getTrancheTimestamp();
        // notify listeners
        rts.fireCreatedCallback(this);
    }

    /**
     * <p>Method to be implemented.</p>
     * @param bytes
     */
    public abstract void callback(byte[] bytes);

    /**
     * 
     */
    public synchronized void keepAlive() {
        debugOut("ID: " + id + "; Keep alive signal received.");
        keepAlive = true;
        notifyAll();
    }

    /**
     * <p>Caller blocks waiting for callback. Caller's execution blocked within RemoteTrancheServer.</p>
     * @throws java.lang.Exception
     */
    public synchronized void waitForCallback() throws Exception {
        // if not completed, wait
        long start = TimeUtil.getTrancheTimestamp();
        while (timeCompleted == RemoteCallback.NOT_COMPLETED && cachedException == null) {
            wait(RemoteTrancheServer.getResponseTimeout());
            // was the response timeout reset by the server?
            if (keepAlive) {
                keepAlive = false;
                continue;
            }
            // did this time out?
            if (timeCompleted == RemoteCallback.NOT_COMPLETED && cachedException == null) {
                final long finish = TimeUtil.getTrancheTimestamp();
                cachedException = new TimeoutException("Callback #" + id + " timed out. Server: " + this.rts.getHost() + ". Waited: " + Text.getPrettyEllapsedTimeString(finish - start) + " (Start: " + Text.getFormattedDate(start) + ", Finish: " + Text.getFormattedDate(finish) + ")");
            }
        }
        debugOut("Exiting wait method after " + Text.getPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp() - start) + " (ID = " + id + ", Time completed = " + timeCompleted + ", Name = " + name + ", Description = " + description + ")");
        // throw the exception if there is one
        if (cachedException != null) {
            throw cachedException;
        }
    }

    /**
     * <p>Notify caller waiting callback.</p>
     */
    public synchronized void notifyWaiting() {
        // flag as finished
        timeCompleted = TimeUtil.getTrancheTimestamp();
        // notify server listeners
        rts.fireFulfilledCallback(this);
        // notify listeners
        notifyAll();
    }

    /**
     * 
     */
    public synchronized void notifyTimedOut() {
        debugOut("ID: " + id + "; Keep-alive timed out");
        cachedException = new UnresponsiveServerException();
        notifyWaiting();
    }

    /**
     * 
     * @return
     */
    protected RemoteTrancheServer getRemoteTrancheServer() {
        return rts;
    }

    /**
     * 
     * @return
     */
    public long getID() {
        return id;
    }

    /**
     * <p>Returns whether this callback has been fulfilled.</p>
     * @return
     */
    public boolean isComplete() {
        return timeCompleted != NOT_COMPLETED;
    }

    /**
     * <p>Returns the UNIX timestamp for when the callback was fulfilled.</p>
     * @return
     */
    public long getTimeCompleted() {
        return timeCompleted;
    }

    /**
     * 
     * @return
     */
    public long getTimeStarted() {
        return timeStarted;
    }

    /**
     * <p>Purges callback, notifying waiting threads. Sends a useful exception.</p>
     * @param reason
     */
    public void purge(String reason) {
        purgeMsg = reason;
        cachedException = new PurgedCallbackException(this);
        notifyWaiting();

        // Tell the registry to fire off listeners
        RemoteCallbackRegistry.fireCallbackPurged(RemoteCallback.this, rts, this.created);
    }

    /**
     * <p>Returns whether purged. Used by callbacks for better exceptions.</p>
     * @return
     */
    protected boolean isPurged() {
        return purgeMsg != null;
    }

    /**
     * <p>Returns the message that was given for purging this callback.</p>
     * @return
     */
    public String getPurgeMsg() {
        return purgeMsg;
    }

    /**
     * <p>Returns thename of this callback.</p>
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Returns the description of this callback.</p>
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * 
     * @return
     */
    public Exception getCachedException() {
        return cachedException;
    }

    /**
     * 
     * @param e
     */
    protected void setCachedException(Exception e) {
        cachedException = e;
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static void setDebug(boolean debug) {
        RemoteCallback.debug = debug;
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
            DebugUtil.printOut(RemoteCallback.class.getName() + "> " + line);
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
}
