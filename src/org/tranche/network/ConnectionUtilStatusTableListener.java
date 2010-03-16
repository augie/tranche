/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tranche.network;

/**
 * <p>Listens for changes in status table on behalf of the ConnectionUtil.</p>
 * <p>Note that I moved this to a separate class because the thread dumps from jstack were not complete if a listener is anonymously declared inside a thread. =( </p>
 * @author Tranche
 */
public class ConnectionUtilStatusTableListener implements StatusTableListener {

    public void rowsAdded(StatusTableEvent ste) {
        ConnectionUtil.adjustConnections();
    }

    public void rowsUpdated(StatusTableEvent ste) {
        boolean adjustConnections = false;
        StatusTable table = NetworkUtil.getStatus().clone();
        for (String host : ste.getHosts()) {
            // if this change affects the connectivity
            if (ste.affectedConnectivity(host)) {
                StatusTableRow str = table.getRow(host);
                if (ConnectionUtil.isConnected(host) && str.isOnline()) {
                    ConnectionUtil.adjustConnection(str.getHost(), str.getPort(), str.isSSL());
                } else {
                    adjustConnections = true;
                }
            }
            // adjust connections if hash spans changed
            if (ste.affectedHashSpans(host)) {
                adjustConnections = true;
            }
        }
        // when a server is offline, we need a recalculation of servers to which we connect
        if (adjustConnections) {
            ConnectionUtil.adjustConnections();
        }
    }

    public void rowsRemoved(StatusTableEvent ste) {
        ConnectionUtil.adjustConnections();
    }
}
