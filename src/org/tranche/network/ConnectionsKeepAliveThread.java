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
import org.tranche.commons.DebuggableThread;
import org.tranche.commons.ThreadUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ConnectionsKeepAliveThread extends DebuggableThread {

    /**
     * 
     */
    public ConnectionsKeepAliveThread() {
        setName("Keep Connections Alive");
        setDaemon(true);
    }

    /**
     * 
     */
    @Override()
    public void run() {
        while (true) {
            try {
                int sleepCount = ConfigureTranche.getInt(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_KEEP_ALIVE_INTERVAL);
                if (sleepCount > 0) {
                    ThreadUtil.sleep(sleepCount);
                    for (String host : ConnectionUtil.getConnectedHosts()) {
                        try {
                            // Application-level activity so socket timeout not triggered.
                            ConnectionUtil.getConnection(host).getRemoteTrancheServer().ping();
                        } catch (Exception e) {
                            debugErr(e);
                            ConnectionUtil.reportExceptionHost(host, e);
                        }
                    }
                } else {
                    ThreadUtil.sleep(10000);
                }
            } catch (Exception e) {
                debugErr(e);
            }
        }
    }
}
