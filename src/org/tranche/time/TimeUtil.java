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
package org.tranche.time;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.tranche.ConfigureTranche;
import org.tranche.util.DebugUtil;
import org.tranche.util.TestUtil;

/**
 * <p>Timekeeper for the repository. All parties need to be guaranteed to be working off the same timestamp.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public final class TimeUtil {

    private static boolean debug = false;
    public static final TimeZone TRANCHE_TIMEZONE = TimeZone.getTimeZone("America/New_York");
    private static long lastRemainderUpdateTrancheConfig = -1,  timestampAuthorityLastChecked = -1,  offset = 0;
    private static Thread offsetUpdateThread = new Thread("Time Management") {

        @Override
        public void run() {
            while (true) {
                try {
                    if (TestUtil.isTesting()) {
                        return;
                    }
                    long timeBetweenChangeChecks = ConfigureTranche.getLong(ConfigureTranche.PROP_TIME_CHANGE_CHECK_INTERVAL);
                    debugOut("Time betwen change checks: " + timeBetweenChangeChecks);
                    long timeAcceptableDeviation = ConfigureTranche.getLong(ConfigureTranche.PROP_TIME_CHANGE_CHECK_DEVIATION);
                    debugOut("Acceptable deviation: " + timeAcceptableDeviation);
                    long timeBetwenForcedUpdates = ConfigureTranche.getLong(ConfigureTranche.PROP_TIME_UPDATE_INTERVAL);
                    debugOut("Time betwen forced updates: " + timeBetwenForcedUpdates);
                    if (timeBetweenChangeChecks == 0) {
                        // can't let the thread run away
                        Thread.sleep(30000);
                    } else if (timeBetweenChangeChecks > 0 && timeAcceptableDeviation > 0) {
                        long timestampLastChecked = System.currentTimeMillis();
                        Thread.sleep(timeBetweenChangeChecks);
                        long timestampNow = System.currentTimeMillis();
                        // if the expected current timestamp is off by more than the acceptable deviation
                        if (Math.abs(timestampNow - timestampLastChecked - timeBetweenChangeChecks) > timeAcceptableDeviation) {
                            debugOut("Forcing an offset update because of a system time change.");
                            // the local time was changed -- recalculate the offset
                            updateOffset();
                        } // it's been too long
                        else if (System.currentTimeMillis() - timestampAuthorityLastChecked >= timeBetwenForcedUpdates) {
                            debugOut("Forcing offset update.");
                            updateOffset();
                        }
                    } else if (timeBetwenForcedUpdates > 0 && System.currentTimeMillis() - timestampAuthorityLastChecked >= timeBetwenForcedUpdates) {
                        debugOut("Forcing offset update.");
                        updateOffset();
                    }

                    // also use this thread to update the network configuration
                    long confUpdateInterval = ConfigureTranche.getLong(ConfigureTranche.PROP_UPDATE_CONFIG_INTERVAL);
                    if (confUpdateInterval > 0) {
                        long remainderUpdateTrancheConfig = getTrancheTimestamp() % confUpdateInterval;
                        if (remainderUpdateTrancheConfig < lastRemainderUpdateTrancheConfig) {
                            debugOut("Updating network configuration.");
                            ConfigureTranche.update();
                        }
                        lastRemainderUpdateTrancheConfig = remainderUpdateTrancheConfig;
                    } else if (lastRemainderUpdateTrancheConfig != -1) {
                        lastRemainderUpdateTrancheConfig = -1;
                    }
                } catch (Exception e) {
                    debugErr(e);
                }
            }
        }
    };


    static {
        // make sure everybody is on the same time zone
        TimeZone.setDefault(TRANCHE_TIMEZONE);
        // start another thread to update from the authority
        offsetUpdateThread.setDaemon(true);
        offsetUpdateThread.setPriority(Thread.MAX_PRIORITY);
        offsetUpdateThread.start();
    }

    /**
     * Cannot instantiate.
     */
    private TimeUtil() {
    }

    /**
     * 
     */
    public synchronized static final void updateOffset() {
        // do not update if testing
        if (TestUtil.isTesting()) {
            timestampAuthorityLastChecked = System.currentTimeMillis();
            return;
        }
        debugOut("Updating offset.");
        List<String> servers = ConfigureTranche.getNetworkTimeServers();
        // shuffle the servers each time we go for an offset, which should not happen often
        Collections.shuffle(servers);
        // try all the servers
        boolean success = false;
        for (String address : servers) {
            try {
                offset = getOffset(address);
                timestampAuthorityLastChecked = System.currentTimeMillis();
                success = true;
                break;
            } catch (Exception e) {
                debugErr(e);
            }
        }
        if (!success) {
            debugOut("Could not update time offset from any network time servers.");
            if (timestampAuthorityLastChecked == -1) {
                throw new RuntimeException("Could not set time offset from any network time servers.");
            }
        }
        debugOut("Done updating offset.");
    }

    /**
     * 
     * @param address
     * @return
     * @throws java.net.UnknownHostException
     * @throws java.io.IOException
     */
    public static final long getOffset(String address) throws UnknownHostException, IOException {
        NTPUDPClient ntpClient = new NTPUDPClient();
        try {
            InetAddress inet = InetAddress.getByName(address);
            debugOut("Updating offset from " + inet.getHostAddress() + ".");
            int timeout = ConfigureTranche.getInt(ConfigureTranche.PROP_TIME_UPDATE_TIMEOUT);
            if (timeout > 0) {
                ntpClient.setDefaultTimeout(timeout);
            }
            TimeInfo time = ntpClient.getTime(inet);
            time.computeDetails();
            debugOut("Returned Offset: " + time.getOffset());
            debugOut("Network Delay: " + time.getDelay());
            return time.getOffset();
        } finally {
            ntpClient.close();
        }
    }

    /**
     * 
     * @return
     */
    public synchronized static final long getTrancheTimestamp() {
        // startup
        if (timestampAuthorityLastChecked == -1) {
            updateOffset();
        }
        return System.currentTimeMillis() - offset;
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        TimeUtil.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static final boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static final void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(TimeUtil.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param line
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
