package org.tranche.network;

import org.tranche.ConfigureTranche;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.time.TimeUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.ThreadUtil;

/**
 *
 * @author James A Hill
 */
public class ConnectionsKeepAliveThread extends Thread {

    private static boolean debug = false;

    public ConnectionsKeepAliveThread() {
        setName("Keep Connections Alive");
        setDaemon(true);
    }

    @Override()
    public void run() {
        // always on while the system is running
        while (true) {
            try {
                int conditionalPingInterval = ConfigureTranche.getInt(ConfigureTranche.PROP_KEEP_ALIVE_INTERVAL);
                ThreadUtil.safeSleep(conditionalPingInterval);
                for (String host : ConnectionUtil.getConnectedHosts()) {
                    try {
                        RemoteTrancheServer ts = ConnectionUtil.getConnection(host).getRemoteTrancheServer();
                        if (TimeUtil.getTrancheTimestamp() - ts.getTimeLastUsed() > conditionalPingInterval) {
                            // Application-level activity so socket timeout not triggered.
                            ts.ping();
                        }
                    } catch (Exception e) {
                        debugErr(e);
                        ConnectionUtil.reportExceptionHost(host, e);
                    }
                }
            } catch (Exception e) {
                debugErr(e);
            }
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        ConnectionsKeepAliveThread.debug = debug;
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
            DebugUtil.printOut(ConnectionsKeepAliveThread.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
