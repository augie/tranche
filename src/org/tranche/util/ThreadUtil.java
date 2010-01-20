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
package org.tranche.util;

import java.util.Collection;
import org.tranche.time.TimeUtil;

/**
 * <p>A utility for using threads.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ThreadUtil {

    /**
     * <p>Utility method to wait a maximum amount of milliseconds for the collection of threads to complete before interrupting them. The threads must have already been started</p>
     * @param threads
     * @param wait Time in milliseconds to wait for the collection of threads.
     */
    public static void wait(Collection<Thread> threads, long wait) {
        // wait
        for (Thread t : threads) {
            while (wait > 0 && t.isAlive()) {
                long startWait = TimeUtil.getTrancheTimestamp();
                // wait for a maximum amount of time
                try {
                    t.join(wait);
                } catch (Exception e) {
                }
                // reduce remaining wait time by the actual amount of time used
                wait -= (TimeUtil.getTrancheTimestamp() - startWait);
            }
        }
        // interrupt any that are still working
        for (Thread t : threads) {
            if (t.isAlive()) {
                try {
                    t.interrupt();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * <p>Utility method to wait a maximum amount of milliseconds for a thread to join.</p>
     * @param t
     * @param wait
     */
    public static void wait(Thread t, long wait) {
        while (wait > 0 && t.isAlive()) {
            long startWait = TimeUtil.getTrancheTimestamp();
            // wait for a maximum amount of time
            try {
                t.join(wait);
            } catch (Exception e) {
            }
            // reduce remaining wait time by the actual amount of time used
            wait -= (TimeUtil.getTrancheTimestamp() - startWait);
        }
        if (t.isAlive()) {
            // kill the thread
            try {
                t.interrupt();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 
     * @param sleep
     */
    public static void safeSleep(long sleep) {
        while (sleep > 0) {
            long startSleep = TimeUtil.getTrancheTimestamp();
            // sleep for maximum amount of time
            try {
                Thread.sleep(sleep);
            } catch (Exception e) {
            }
            // reduce remaining sleep timeby the actual amount of time slept
            long endSleep = TimeUtil.getTrancheTimestamp();
            // local time changed... do not want to wait forever
            if (endSleep < startSleep) {
                return;
            }
            sleep -= (endSleep - startSleep);
        }
    }
}
