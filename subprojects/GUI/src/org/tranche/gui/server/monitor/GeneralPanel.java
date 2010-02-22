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
package org.tranche.gui.server.monitor;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import org.tranche.gui.monitor.InfoColumn;
import org.tranche.hash.span.AbstractHashSpan;
import org.tranche.hash.span.HashSpan;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.time.TimeUtil;
import org.tranche.util.Text;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GeneralPanel extends JPanel {

    private ServerMonitor monitor;
    private InfoColumn left;
    private InfoColumn right;
    // thread to update 
    private Thread updateThread;

    public GeneralPanel(ServerMonitor monitor) {
        this.monitor = monitor;
        left = new InfoColumn(monitor);
        right = new InfoColumn(monitor);
        setName("General");
        setLayout(new GridLayout(1, 2));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        // add the left column
        left.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.BLACK));
        add(left);

        // add the right column
        add(right);

        update();
    }

    public void start() {
        updateThread = new Thread() {

            @Override
            public void run() {
                try {
                    while (true) {
                        update();
                        Thread.sleep(2000);
                    }
                } catch (Exception e) {
                }
            }
        };
        updateThread.start();
    }

    public void stop() {
        updateThread.interrupt();
    }

    public void update() {
        updateLeftColumn();
        updateRightColumn();
    }

    public void updateLeftColumn() {
        StatusTableRow row = NetworkUtil.getStatus().getRow(monitor.getHost());

        left.clear();
        left.put("URL", row.getURL());
        left.put("Name", row.getName());
        left.put("Group", row.getGroup());
        left.put("Hash Spans", "");
        for (HashSpan hs : row.getHashSpans()) {
            AbstractHashSpan ahs = new AbstractHashSpan(hs);
            left.put("", ahs.getAbstractionFirst() + " - " + ahs.getAbstractionLast());
        }
        left.put("Target Hash Spans", "");
        for (HashSpan hs : row.getTargetHashSpans()) {
            AbstractHashSpan ahs = new AbstractHashSpan(hs);
            left.put("", ahs.getAbstractionFirst() + " - " + ahs.getAbstractionLast());
        }

        left.refresh();
    }

    public void updateRightColumn() {
        StatusTableRow row = NetworkUtil.getStatus().getRow(monitor.getHost());

        right.clear();
        right.put("Last Updated", Text.getFormattedDate(row.getUpdateTimestamp()));
        right.put("Trusted", String.valueOf(row.isCore()));
        right.put("Readable", String.valueOf(row.isReadable()));
        right.put("Writable", String.valueOf(row.isWritable()));
        right.put("Data Server", String.valueOf(row.isDataStore()));
        right.put("Online", String.valueOf(row.isOnline()));
        right.put("Connected", String.valueOf(ConnectionUtil.isConnected(row.getHost())));
        if (row.getResponseTimestamp() != 0) {
            right.put("Last Response", Text.getShortPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp() - row.getResponseTimestamp()) + " ago");
        } else {
            right.put("Last Response", "");
        }
        right.refresh();
    }
}
