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

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.GenericTable;
import org.tranche.gui.SortableTableModel;
import org.tranche.gui.Styles;
import org.tranche.servers.ServerMessageDownEvent;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class DownloadPanel extends JPanel {

    public static final int MAX_NUM_ROWS = 50;
    private DownloadTableModel model = new DownloadTableModel();
    private GenericTable table;

    public DownloadPanel() {
        setName("Down");
        setLayout(new BorderLayout());

        // create the table
        table = new GenericTable(model, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // create and add a scroll pane containing the table
        GenericScrollPane scrollPane = new GenericScrollPane(table);
        scrollPane.setBackground(Color.GRAY);
        scrollPane.setHorizontalScrollBarPolicy(GenericScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(Styles.BORDER_NONE);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void handleServerMessageDownEvent(ServerMessageDownEvent smde) {
        model.handleServerMessageDownEvent(smde);
    }

    private class DownloadTableModel extends SortableTableModel {

        private final String[] headers = new String[]{"Callback", "Progress", "Size", "Started", "Finished", "Status"};
        private final Map<Long, DownloadRow> rows = new HashMap<Long, DownloadRow>();
        private final LinkedList<Long> order = new LinkedList<Long>();
        private final LinkedList<Long> orderReceived = new LinkedList<Long>();

        public void handleServerMessageDownEvent(ServerMessageDownEvent smde) {
            boolean isNew = true;
            synchronized (rows) {
                isNew = !rows.containsKey(smde.getCallbackId());
            }
            if (isNew) {
                // make some space if need be
                synchronized (rows) {
                    while (rows.size() > MAX_NUM_ROWS) {
                        long callbackId;
                        synchronized (orderReceived) {
                            callbackId = orderReceived.removeFirst();
                        }
                        synchronized (order) {
                            order.remove(callbackId);
                        }
                        rows.remove(callbackId);
                    }
                }

                DownloadRow dr = new DownloadRow(smde);
                synchronized (rows) {
                    rows.put(smde.getCallbackId(), dr);
                }
                synchronized (order) {
                    order.add(dr.callbackId);
                }
                synchronized (orderReceived) {
                    orderReceived.add(dr.callbackId);
                }
            } else {
                DownloadRow dr;
                synchronized (rows) {
                    dr = rows.get(smde.getCallbackId());
                    dr.handleServerMessageDownEvent(smde);
                }
            }
            resort();
            fireTableDataChanged();
        }

        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public Class getColumnClass(int c) {
            try {
                return getValueAt(0, c).getClass();
            } catch (Exception e) {
                return String.class;
            }
        }

        @Override
        public String getColumnName(int column) {
            if (column < getColumnCount()) {
                return headers[column];
            } else {
                return "";
            }
        }

        public DownloadRow getRow(int row) {
            long callbackId = -1;
            synchronized (order) {
                callbackId = order.get(row);
            }
            synchronized (rows) {
                return rows.get(callbackId);
            }
        }

        public int getRowOf(DownloadRow dr) {
            synchronized (order) {
                return order.indexOf(dr);
            }
        }

        public int getRowCount() {
            synchronized (order) {
                return order.size();
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public Object getValueAt(int row, int column) {
            try {
                DownloadRow dr = getRow(row);
                if (dr == null) {
                    return null;
                }
                switch (column) {
                    case 0:
                        return String.valueOf(dr.callbackId);
                    case 1:
                        return String.valueOf(dr.bytesDownloaded);
                    case 2:
                        return String.valueOf(dr.bytesToDownload);
                    case 3:
                        return String.valueOf(dr.timeStarted);
                    case 4:
                        return String.valueOf(dr.timeFinished);
                    case 5:
                        return dr.status;
                    default:
                        return null;
                }
            } catch (Exception e) {
                return null;
            }
        }

        public void sort(int column) {
            if (column >= headers.length || column < 0) {
                return;
            }
            table.setPressedColumn(column);
            synchronized (order) {
                Collections.sort(order, new SSComparator(column));
            }
        }

        public void resort() {
            sort(table.getPressedColumn());
        }

        private class SSComparator implements Comparator {

            private int column;

            public SSComparator(int column) {
                this.column = column;
            }

            public int compare(Object o1, Object o2) {
                if (table.getDirection()) {
                    Object temp = o1;
                    o1 = o2;
                    o2 = temp;
                }

                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o1 == null) {
                    return 1;
                } else if (o1 instanceof Long && o2 instanceof Long) {
                    DownloadRow dr1, dr2;
                    synchronized (rows) {
                        dr1 = rows.get((Long) o1);
                        dr2 = rows.get((Long) o2);
                    }
                    if (column == table.getColumnModel().getColumnIndex("Callback")) {
                        if (dr1.callbackId == dr2.callbackId) {
                            return 0;
                        } else if (dr1.callbackId > dr2.callbackId) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Progress")) {
                        if (dr1.bytesDownloaded == dr2.bytesDownloaded) {
                            return 0;
                        } else if (dr1.bytesDownloaded > dr2.bytesDownloaded) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Size")) {
                        if (dr1.bytesToDownload == dr2.bytesToDownload) {
                            return 0;
                        } else if (dr1.bytesToDownload > dr2.bytesToDownload) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Started")) {
                        if (dr1.timeStarted == dr2.timeStarted) {
                            return 0;
                        } else if (dr1.timeStarted > dr2.timeStarted) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Finished")) {
                        if (dr1.timeFinished == dr2.timeFinished) {
                            return 0;
                        } else if (dr1.timeFinished > dr2.timeFinished) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Status")) {
                        return dr1.status.toLowerCase().compareTo(dr2.status.toLowerCase());
                    } else {
                        return 0;
                    }
                } else {
                    return 1;
                }
            }
        }
    }

    private class DownloadRow {

        public long callbackId = -1;
        public long timeStarted = 0;
        public long timeFinished = 0;
        public long bytesToDownload = 0;
        public long bytesDownloaded = 0;
        public String status = "Unknown";

        public DownloadRow(ServerMessageDownEvent smde) {
            update(smde);
        }

        public void handleServerMessageDownEvent(ServerMessageDownEvent smde) {
            update(smde);
        }

        private void update(ServerMessageDownEvent smde) {
            this.callbackId = smde.getCallbackId();
            if (smde.getType() == smde.TYPE_STARTED) {
                this.status = "Downloading";
                this.timeStarted = smde.getTimestamp();
            } else if (smde.getType() == smde.TYPE_PROGRESS) {
                this.status = "Downloading";
            } else if (smde.getType() == smde.TYPE_COMPLETED) {
                this.status = "Complete";
                this.timeFinished = smde.getTimestamp();
            }
            this.bytesToDownload = smde.getBytesToDownload();
            this.bytesDownloaded = smde.getBytesDownloaded();
        }
    }
}
