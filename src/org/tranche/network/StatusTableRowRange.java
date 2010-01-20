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
package org.tranche.network;

import org.tranche.ConfigureTranche;
import org.tranche.time.TimeUtil;
import org.tranche.util.DebugUtil;

/**
 * <p>Status table row ranges are used in the status table update process for servers. They are used to help propagate status updates across the network.</p>
 * <p>The range represents all host names from the "From" host (inclusive) up to the "To" host (exclusive.)</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class StatusTableRowRange extends Object {

    private static boolean debug = false;
    private String fromHost,  connectionHost,  toHost;
    private boolean registeredWithConnectionHost = false;
    private long registrationTimestamp = 0;

    /**
     * <p>Default constructor</p>
     * @param fromHost The first host in the range.
     * @param toHost The last host in the range.
     */
    protected StatusTableRowRange(String fromHost, String toHost) {
        setFrom(fromHost);
        setTo(toHost);
        // default connection host is the fromHost
        setConnectionHost(fromHost);
    }

    /**
     * <p>Gets the host of the Tranche server from which this range of network status table rows should be requested.</p>
     * @return The host that should be connected to for this range.
     */
    protected String getConnectionHost() {
        return connectionHost;
    }

    /**
     * <p>Sets the host of the Tranche server from which this range of network status table rows should be requested.</p>
     * @param connectionHost The host that should be connected to for this range.
     */
    protected void setConnectionHost(String connectionHost) {
        if (this.connectionHost == null || !this.connectionHost.equals(connectionHost)) {
            this.connectionHost = connectionHost;
            setRegistrationStatus(false);
        }
    }

    /**
     * <p>Returns whether the connection host has been registered with.</p>
     * @return Whether the connection host has been registered with.
     */
    protected boolean isRegisteredWithConnectionHost() {
        int timeBetweenRegistrations = ConfigureTranche.getInt(ConfigureTranche.PROP_SERVER_TIME_BETWEEN_REGISTRATIONS);
        if (timeBetweenRegistrations > 0 && TimeUtil.getTrancheTimestamp() - registrationTimestamp > timeBetweenRegistrations) {
            return true;
        }
        return registeredWithConnectionHost;
    }

    /**
     * <p>Sets the flag for whether the connection host has been registered with.</p>
     * @param registeredWithConnectionHost A flag for whether the connection host has been registered with.
     */
    protected void setRegistrationStatus(boolean registeredWithConnectionHost) {
        if (registeredWithConnectionHost) {
            registrationTimestamp = TimeUtil.getTrancheTimestamp();
        }
        this.registeredWithConnectionHost = registeredWithConnectionHost;
    }

    /**
     * <p>Returns the first host name in the range.</p>
     * @return A host name.
     */
    protected String getFrom() {
        return fromHost;
    }

    /**
     * <p>Sets the first host name in the range.</p>
     * @param fromHost A host name.
     */
    protected void setFrom(String fromHost) {
        this.fromHost = fromHost;
    }

    /**
     * <p>Returns the last host name in the range.</p>
     * @return A host name.
     */
    protected String getTo() {
        return toHost;
    }

    /**
     * <p>Sets the last host name in the range.</p>
     * @param toHost A host name.
     */
    protected void setTo(String toHost) {
        this.toHost = toHost;
    }

    @Override()
    public String toString() {
        return fromHost + " through " + toHost + " (connected to: " + connectionHost + ")";
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static void setDebug(boolean debug) {
        StatusTableRowRange.debug = debug;
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
    private static void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(StatusTableRowRange.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param line
     */
    private static void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
