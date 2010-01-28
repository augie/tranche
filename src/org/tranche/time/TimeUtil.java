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

import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.tranche.ConfigureTranche;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;

/**
 * <p>Timekeeper for the Tranche network. All parties need to be guaranteed to be working off the same timestamp.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public final class TimeUtil {

    private static boolean debug = false;
    public static final TimeZone TRANCHE_TIMEZONE = TimeZone.getTimeZone("America/New_York");
    public static final long UPDATE_CONFIG_TIMESTAMP_MODULUS = Long.valueOf("10000000");
    private static long lastRemainderUpdateTrancheConfig = -1;
    private static long millisecondsBetweenTimeChangeChecks = 10000,  millisecondsAcceptableTimeChangeDeviation = 1000,  millisecondsBetweenOffsetUpdates = 21600000;
    private static int millisecondsTimeOut = 5000;
    private static long timestampAuthorityLastChecked = -1,  offset = 0;
    private static NTPUDPClient ntpClient = new NTPUDPClient();
    private static List<String> networkTimeServers = new ArrayList<String>();
    private static boolean startedLoadingNetworkTimeServers = false,  finishedLoadingNetworkTimeServers = false;
    private static Thread offsetUpdateThread = new Thread("Time Management") {

        @Override
        public void run() {
            while (true) {
                try {
                    long timestampLastChecked = System.currentTimeMillis();
                    Thread.sleep(millisecondsBetweenTimeChangeChecks);
                    long timestampNow = System.currentTimeMillis();
                    // if the expected current timestamp is off by more than the acceptable deviation
                    if (Math.abs(timestampNow - timestampLastChecked - millisecondsBetweenTimeChangeChecks) > millisecondsAcceptableTimeChangeDeviation) {
                        debugOut("Forcing an offset update because of a system time change.");
                        // the local time was changed -- recalculate the offset
                        updateOffset();
                    } // it's been too long
                    else if (System.currentTimeMillis() - timestampAuthorityLastChecked >= millisecondsBetweenOffsetUpdates) {
                        debugOut("Forcing offset update.");
                        updateOffset();
                    }

                    // also use this thread to update the network configuration
                    long remainderUpdateTrancheConfig = getTrancheTimestamp() % UPDATE_CONFIG_TIMESTAMP_MODULUS;
                    if (remainderUpdateTrancheConfig < lastRemainderUpdateTrancheConfig) {
                        debugOut("Updating network configuration.");
                        ConfigureTranche.update();
                    }
                    lastRemainderUpdateTrancheConfig = remainderUpdateTrancheConfig;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };


    static {
        // make sure everybody is on the same time zone
        TimeZone.setDefault(TRANCHE_TIMEZONE);
        ntpClient.setDefaultTimeout(millisecondsTimeOut);
        // start another thread to update from the authority
        offsetUpdateThread.setDaemon(true);
        offsetUpdateThread.start();
    }

    /**
     * Cannot instantiate.
     */
    private TimeUtil() {
    }

    /**
     * 
     * @param millisecondsBetweenOffsetUpdates
     */
    public static final void setMillisecondsBetweenOffsetUpdates(long millisecondsBetweenOffsetUpdates) {
        TimeUtil.millisecondsBetweenOffsetUpdates = millisecondsBetweenOffsetUpdates;
    }

    /**
     * 
     * @return
     */
    public static final long getMillisecondsBetweenOffsetUpdates() {
        return millisecondsBetweenOffsetUpdates;
    }

    /**
     *
     * @return
     */
    public static final long getMillisecondsBetweenTimeChangeChecks() {
        return millisecondsBetweenTimeChangeChecks;
    }

    /**
     *
     * @param millisecondsBetweenChecks
     */
    public static final void setMillisecondsBetweenTimeChangeChecks(long millisecondsBetweenChecks) {
        TimeUtil.millisecondsBetweenTimeChangeChecks = millisecondsBetweenChecks;
    }

    /**
     *
     * @return
     */
    public static final long getMillisecondsAcceptableTimeChangeDeviation() {
        return millisecondsAcceptableTimeChangeDeviation;
    }

    /**
     *
     * @param millisecondsAcceptableDeviation
     */
    public static final void setMillisecondsAcceptableTimeChangeDeviation(long millisecondsAcceptableDeviation) {
        TimeUtil.millisecondsAcceptableTimeChangeDeviation = millisecondsAcceptableDeviation;
    }

    /**
     *
     */
    public static final void waitForStartup() {
        debugOut("Waiting for startup.");
        loadNetworkTimeServers();
        while (!finishedLoadingNetworkTimeServers) {
            try {
                Thread.sleep(250);
            } catch (Exception e) {
            }
        }
        debugOut("Done waiting for startup.");
    }

    /**
     * 
     */
    private synchronized static final void loadNetworkTimeServers() {
        if (startedLoadingNetworkTimeServers) {
            return;
        }
        startedLoadingNetworkTimeServers = true;
        debugOut("Loading network time servers.");
        try {
            if (TestUtil.isTesting()) {
                return;
            }
            // load the network time server IP addresses
            InputStream is = null;
            try {
                is = ConfigureTranche.openStreamToFile("/org/tranche/time/nts.conf");
                String line = null;
                while ((line = ConfigureTranche.readLineIgnoreComments(is)) != null) {
                    networkTimeServers.add(line.trim().toLowerCase());
                }
            } catch (Exception e) {
                debugErr(e);
            } finally {
                IOUtil.safeClose(is);
            }
        } finally {
            debugOut("Done loading network time servers.");
            finishedLoadingNetworkTimeServers = true;
        }
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
        waitForStartup();
        // shuffle the servers each time we go for an offset, which should not happen often
        Collections.shuffle(networkTimeServers);
        // try all the servers
        for (String address : networkTimeServers) {
            try {
                InetAddress inet = InetAddress.getByName(address);
                debugOut("Updating offset from " + inet.getHostAddress() + ".");
                TimeInfo time = ntpClient.getTime(inet);
                time.computeDetails();
                offset = time.getOffset();
                debugOut("Returned Offset: " + time.getOffset());
                debugOut("Network Delay: " + time.getDelay());
                timestampAuthorityLastChecked = System.currentTimeMillis();
                // stop if there was a success
                break;
            } catch (Exception e) {
                debugErr(e);
            }
        }
        debugOut("Done updating offset.");
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
