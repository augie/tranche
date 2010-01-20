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
package org.tranche.flatfile;

import java.util.HashMap;
import java.util.Map;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.remote.RemoteCallback;
import org.tranche.remote.RemoteCallbackPurgeListener;
import org.tranche.remote.RemoteCallbackRegistry;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.util.IOUtil;

/**
 * <p>Object will decide how to interpret events to control the HashSpanFixingThread.</p>
 * <p>There will be safety switches in place to allow human administrator to lock values.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class HashSpanFixingThreadPolicyDecider implements RemoteCallbackPurgeListener {

    private final FlatFileTrancheServer ffts;
    private final HashSpanFixingThread healingThread;
    private final Map<String, Integer> remoteCallbacksMap;

    /**
     * 
     * @param ffts
     * @param healingThread
     */
    public HashSpanFixingThreadPolicyDecider(FlatFileTrancheServer ffts, HashSpanFixingThread healingThread) {
        this.ffts = ffts;
        this.healingThread = healingThread;

        this.remoteCallbacksMap = new HashMap<String, Integer>();
        
        // Register this decider wherever necessary
        _registerThis();
    }

    /**
     * <p>Register this utility with the remote callback registry.</p>
     */
    private void _registerThis() {
        RemoteCallbackRegistry.addRemoteCallbackPurgeListener(HashSpanFixingThreadPolicyDecider.this);
    }
    
    public void close() {
        RemoteCallbackRegistry.removeRemoteCallbackPurgeListener(HashSpanFixingThreadPolicyDecider.this);
    }
    
    /**
     * <p>Fired when a callback is purged.</p>
     * @param callback
     * @param server
     * @param createdTimestamp
     */
    public void fireCallbackPurged(RemoteCallback callback, RemoteTrancheServer server, long createdTimestamp) {
        String callbackMessage = null;
        try {
            final String url = IOUtil.createURL(server);
            synchronized (remoteCallbacksMap) {
                Integer count = remoteCallbacksMap.get(url);
                if (count == null) {
                    remoteCallbacksMap.put(url, Integer.valueOf(1));
                } else {
                    remoteCallbacksMap.put(url, count + 1);
                }

                // Quick calculate total and top two servers with most callbacks
                int totalCallbacks = 0;
                String topUrl1 = null;
                int topCount1 = 0;
                String topUrl2 = null;
                int topCount2 = 0;

                for (String nextUrl : remoteCallbacksMap.keySet()) {
                    int nextCount = remoteCallbacksMap.get(nextUrl);

                    if (nextCount > topCount1) {
                        // Knock top to second
                        topUrl2 = topUrl1;
                        topCount2 = topCount1;

                        // Set top count
                        topUrl1 = nextUrl;
                        topCount1 = nextCount;
                    } else if (nextCount > topCount2) {
                        // Set second
                        topUrl2 = nextUrl;
                        topCount2 = nextCount;
                    }

                    // Increment total count of callbacks
                    totalCallbacks += nextCount;
                }

                callbackMessage = "Total of " + totalCallbacks + " purged callbacks (";

                // Must be at least one. Is there two?
                if (topCount2 > 0) {
                    callbackMessage += topCount1 + " for " + topUrl1 + ", " + topCount2 + " for " + topUrl2;
                } else {
                    callbackMessage += topCount1 + " for " + topUrl1;
                }

                callbackMessage += ")";
            }

            // Set the message to config
            if (callbackMessage != null) {
                Configuration c = ffts.getConfiguration();
                c.setValue(ConfigKeys.REMOTE_SERVER_CALLBACKS, callbackMessage);
            }
        } catch (Exception nope) {
            printNotice("An exception of type " + nope.getClass().getSimpleName() + " occurred while handling purged callback: " + nope.getMessage());
        }
    } // fireCallbackPurged

    /**
     * <p>Print a notice to standard output.</p>
     * @param msg
     */
    private static void printNotice(String msg) {
        System.out.println("HashSpanFixingThreadPolicyDecider: " + msg);
    }
}
