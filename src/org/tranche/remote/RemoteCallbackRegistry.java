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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.tranche.commons.TextUtil;
import org.tranche.time.TimeUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;

/**
 * <p>Register all callbacks, unregister when called back. Registry invokes an interrupt on timeout.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class RemoteCallbackRegistry {

    private static final boolean debug = false;
    /**
     * <p>Callback will be interrupted after this time in milliseconds, throwing a PurgedCallbackException.</p>
     * <p>This only happens if the server hasn't received any data for a minimal period.</p>
     * @see #serverTimeoutInMillis
     * @see #absoluteCallbackTimeoutInMillis
     */
    private static long callbackTimeoutInMillis = 1000 * 60 * 5;
    /**
     * <p>The minimum amount of time should wait for a quite server before purging a callback.</p>
     * <p>This only happens if the callback is at least a certain age.
     * @see #callbackTimeoutInMillis
     * @see #absoluteCallbackTimeoutInMillis
     */
    private static long serverTimeoutInMillis = 1000 * 60;
    /**
     * <p>The age at which any timeout is purged, regardless of whether the server is active or not. 0 (or any negative number) will disable this timeout.</p>
     * <p>This is quite different from the other heuristic, which looks at both the age of the request and the length of time since last server response.</p>
     * @see #callbackTimeoutInMillis
     * @see #serverTimeoutInMillis
     */
    private static long absoluteCallbackTimeoutInMillis = 1000 * 60 * 10;
    private static final List<CallbackEntry> entries = new ArrayList();
    private static final Set<RemoteCallbackPurgeListener> listeners = new HashSet();
    private static boolean isLazyLoaded = false;

    /**
     * <p>Add the callback to the registrar. Registrar automatically purges callbacks that are timed out based on internal heuristics.</p>
     * <p>If a callback is returned, make sure to unregister it.</p>
     * @param callback
     * @param server
     */
    public static void register(RemoteCallback callback, RemoteTrancheServer server) {
        lazyLoad();
        // Don't register test callbacks
        if (TestUtil.isTesting() && !TestUtil.isTestingTimeout()) {
            return;
        }
        synchronized (entries) {
            entries.add(new CallbackEntry(callback, server));
            printTracer("Callback registered.");
        }
    }

    /**
     * <p>Unregister a callback that has been added to the registry.</p>
     * @param callback
     */
    public static void unregister(RemoteCallback callback) {
        lazyLoad();
        synchronized (entries) {
            Iterator<CallbackEntry> it = entries.iterator();
            CallbackEntry next;
            while (it.hasNext()) {
                next = it.next();
                if (next.getCallback().equals(callback)) {
                    it.remove();
                    printTracer("Callback unregistered.");
                    return;
                }
            }
        }
    }

    /**
     * <p>The caller should be the RemoteCallback, which will only happen if it is purged.</p>
     * @param callback
     * @param server
     * @param createdTimestamp
     */
    public static synchronized void fireCallbackPurged(RemoteCallback callback, RemoteTrancheServer server, long createdTimestamp) {
        for (RemoteCallbackPurgeListener l : listeners) {
            l.fireCallbackPurged(callback, server, createdTimestamp);
        }
    }

    /**
     * <p>Add a purge listener.</p>
     * @param l
     * @return
     */
    public static synchronized boolean addRemoteCallbackPurgeListener(RemoteCallbackPurgeListener l) {
        try {
            return listeners.add(l);
        } finally {
        }
    }

    /**
     * <p>Remove the given purge listener.</p>
     * @param l
     * @return
     */
    public static synchronized boolean removeRemoteCallbackPurgeListener(RemoteCallbackPurgeListener l) {
        try {
            return listeners.remove(l);
        } finally {
        }
    }

    /**
     * <p>Remove all purge listeners.</p>
     */
    public static synchronized void clearRemoteCallbackPurgeListeners() {
        listeners.clear();
    }

    /**
     * 
     */
    private static void lazyLoad() {
        if (isLazyLoaded) {
            return;
        }
        isLazyLoaded = true;

        ScavengerThread t = new ScavengerThread();
        t.start();
    }

    /**
     * <p>Prints message to System.out.</p>
     * @param msg
     */
    private static void printTracer(String msg) {
        if (debug) {
            System.out.println("REMOTE_CALLBACK_REGISTRY> " + msg);
        }
    }

    /**
     * <p>The minimum amount of time should wait for a quite server before purging a callback.</p>
     * <p>This only happens if the callback is at least a certain age.
     * @see #callbackTimeoutInMillis
     * @see #absoluteCallbackTimeoutInMillis
     * @return
     */
    public static long getServerTimeoutInMillis() {
        return serverTimeoutInMillis;
    }

    /**
     * <p>The minimum amount of time should wait for a quite server before purging a callback.</p>
     * <p>This only happens if the callback is at least a certain age.
     * @see #callbackTimeoutInMillis
     * @see #absoluteCallbackTimeoutInMillis
     * @param aTimeoutInMillisSinceServerLastSent
     */
    public static void setServerTimeoutInMillis(long aTimeoutInMillisSinceServerLastSent) {
        serverTimeoutInMillis = aTimeoutInMillisSinceServerLastSent;
    }

    /**
     * <p>The age at which any timeout is purged, regardless of whether the server is active or not. 0 (or any negative number) will disable this timeout.</p>
     * <p>This is quite different from the other heuristic, which looks at both the age of the request and the length of time since last server response.</p>
     * @see #callbackTimeoutInMillis
     * @see #serverTimeoutInMillis
     * @return
     */
    public static long getAbsoluteCallbackTimeoutInMillis() {
        return absoluteCallbackTimeoutInMillis;
    }

    /**
     * <p>The age at which any timeout is purged, regardless of whether the server is active or not. 0 (or any negative number) will disable this timeout.</p>
     * <p>This is quite different from the other heuristic, which looks at both the age of the request and the length of time since last server response.</p>
     * @see #callbackTimeoutInMillis
     * @see #serverTimeoutInMillis
     * @param aAbsoluteTimeoutInMillis
     */
    public static void setAbsoluteCallbackTimeoutInMillis(long aAbsoluteTimeoutInMillis) {
        absoluteCallbackTimeoutInMillis = aAbsoluteTimeoutInMillis;
    }

    /**
     * <p>Callback will be interrupted after this time in milliseconds, throwing a PurgedCallbackException.</p>
     * <p>This only happens if the server hasn't received any data for a minimal period.</p>
     * @see #serverTimeoutInMillis
     * @see #absoluteCallbackTimeoutInMillis
     * @return
     */
    public static long getCallbackTimeoutInMillis() {
        return callbackTimeoutInMillis;
    }

    /**
     * <p>Callback will be interrupted after this time in milliseconds, throwing a PurgedCallbackException.</p>
     * <p>This only happens if the server hasn't received any data for a minimal period.</p>
     * @see #serverTimeoutInMillis
     * @see #absoluteCallbackTimeoutInMillis
     * @param aTimeoutInMillis
     */
    public static void setCallbackTimeoutInMillis(long aTimeoutInMillis) {
        callbackTimeoutInMillis = aTimeoutInMillis;
    }

    /**
     * <p>Thread interrupts old timeouts. One per process.</p>
     */
    private static class ScavengerThread extends Thread {

        /**
         * 
         */
        public ScavengerThread() {
            super("RemoteCallbackRegistry Scavenger Thread");
            this.setDaemon(true);
            this.setPriority(Thread.MIN_PRIORITY);
        }

        /**
         * 
         */
        @Override()
        public void run() {
            while (true) {
                try {
                    synchronized (RemoteCallbackRegistry.entries) {
                        Iterator<CallbackEntry> it = RemoteCallbackRegistry.entries.iterator();
                        CallbackEntry next;
                        while (it.hasNext()) {
                            next = it.next();
                            if (next.isTimedOut()) {
                                it.remove();
                                printTracer("Callback timed out, purged for " + IOUtil.createURL(next.server) + " (Registered " + TextUtil.getFormattedDate(next.registeredTime) + ", Purged at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()) + ")");
                            }
                        }
                    }

                    // Scavenger takes a nap
                    Thread.sleep(100);
                } catch (InterruptedException ex) { /* Nope */ } finally {
                }
            }
        }
    }

    /**
     * <p>Encapsulates the callback entry logic.</p>
     */
    private static class CallbackEntry {

        private RemoteCallback callback;
        private RemoteTrancheServer server;
        private long registeredTime;

        /**
         * 
         * @param c
         * @param s
         */
        public CallbackEntry(RemoteCallback c, RemoteTrancheServer s) {
            this.callback = c;
            this.server = s;
            this.registeredTime = TimeUtil.getTrancheTimestamp();
        }

        /**
         * <p>Returns true if timed out and interrupted, false otherwise.</p>
         * <p>Only timed out if server response and callback have both timed out.</p>
         * @return True if timed out and interrupted.
         */
        public boolean isTimedOut() {

            boolean callbackTimeout = isRegistrationEllapsed();
            boolean serverTimeout = isServerTimedOut();
            boolean absoluteTimeout = isAbsoluteTimeoutEllapsed();

            if ((callbackTimeout && serverTimeout) || absoluteTimeout) {

                String why = "not sure why...";

                if (callbackTimeout && serverTimeout) {
                    why = "Callback and server timed out";
                } else if (absoluteTimeout) {
                    why = "Absolute age of request exceeded";
                }

                final String desc = "Callback id #" + callback.getID() + "(" + callback.getName() + ") for " + IOUtil.createURL(callback.getRemoteTrancheServer()) + " timed out and purged at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()) + "; Reason: " + why;

                // Print more information if debug flag set
                printTracer(desc);
                printTracer("  - Type: " + callback.getName());
                printTracer("  - Time since registration: " + TextUtil.formatTimeLength(getRegistrationEllapsed()));
                printTracer("  - Time since server output: " + TextUtil.formatTimeLength(getServerOutputEllapsed()));
                printTracer("  - Number of registered entries: " + entries.size());

                // Print minimal information about purge -- every time out is notable
                System.err.println(desc);

                this.callback.purge("RemoteCallbackRegistry.isTimedOut purged callback due to callback and server timeout");
                return true;
            }

            return false;
        }

        protected long getRegistrationEllapsed() {
            return TimeUtil.getTrancheTimestamp() - this.registeredTime;
        }

        protected boolean isRegistrationEllapsed() {
            return getRegistrationEllapsed() > RemoteCallbackRegistry.getCallbackTimeoutInMillis();
        }

        protected boolean isAbsoluteTimeoutEllapsed() {
            if (RemoteCallbackRegistry.getAbsoluteCallbackTimeoutInMillis() <= 0) {
                return false;
            }
            return getRegistrationEllapsed() > RemoteCallbackRegistry.getAbsoluteCallbackTimeoutInMillis();
        }

        protected long getServerOutputEllapsed() {
            return this.server.getMillisSinceLastServerResponse();
        }

        protected boolean isServerTimedOut() {
            return getServerOutputEllapsed() > RemoteCallbackRegistry.getServerTimeoutInMillis();
        }

        /**
         * 
         * @return
         */
        public RemoteCallback getCallback() {
            return this.callback;
        }
    } // CallbackEntry
}
