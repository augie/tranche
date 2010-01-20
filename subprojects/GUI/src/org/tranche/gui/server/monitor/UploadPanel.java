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
import org.tranche.servers.ServerMessageUpEvent;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class UploadPanel extends JPanel {

    public static final int MAX_NUM_ROWS = 50;
    private UploadTableModel model = new UploadTableModel();
    private GenericTable table;

    public UploadPanel() {
        setName("Up");
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

    public void handleServerMessageUpEvent(ServerMessageUpEvent smue) {
        model.handleServerMessageUpEvent(smue);
    }

    private class UploadTableModel extends SortableTableModel {

        private String[] headers = new String[]{"Callback", "Progress", "Size", "Started", "Finished", "Status"};
        private Map<Long, UploadRow> rows = new HashMap<Long, UploadRow>();
        private LinkedList<Long> order = new LinkedList<Long>();
        private LinkedList<Long> orderReceived = new LinkedList<Long>();

        public void handleServerMessageUpEvent(ServerMessageUpEvent smue) {
            boolean isNew = true;
            synchronized (rows) {
                isNew = !rows.containsKey(smue.getCallbackId());
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

                UploadRow dr = new UploadRow(smue);
                synchronized (rows) {
                    rows.put(smue.getCallbackId(), dr);
                }
                synchronized (order) {
                    order.add(dr.callbackId);
                }
                synchronized (orderReceived) {
                    orderReceived.add(dr.callbackId);
                }
            } else {
                UploadRow ur;
                synchronized (rows) {
                    ur = rows.get(smue.getCallbackId());
                    ur.handleServerMessageUpEvent(smue);
                }
            }
            resort();
            fireTableDataChanged();
        }

        public int getColumnCount() {
            return headers.length;
        }

        public Class getColumnClass(int c) {
            try {
                return getValueAt(0, c).getClass();
            } catch (Exception e) {
                return String.class;
            }
        }

        public String getColumnName(int column) {
            if (column < getColumnCount()) {
                return headers[column];
            } else {
                return "";
            }
        }

        public UploadRow getRow(int row) {
            long callbackId = -1;
            synchronized (order) {
                callbackId = order.get(row);
            }
            synchronized (rows) {
                return rows.get(callbackId);
            }
        }

        public int getRowOf(UploadRow dr) {
            synchronized (order) {
                return order.indexOf(dr);
            }
        }

        public int getRowCount() {
            synchronized (order) {
                return order.size();
            }
        }

        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public Object getValueAt(int row, int column) {
            try {
                UploadRow dr = getRow(row);
                if (dr == null) {
                    return null;
                }
                switch (column) {
                    case 0:
                        return String.valueOf(dr.callbackId);
                    case 1:
                        return String.valueOf(dr.bytesUploaded);
                    case 2:
                        return String.valueOf(dr.bytesToUpload);
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
                    UploadRow ur1, ur2;
                    synchronized (rows) {
                        ur1 = rows.get((Long) o1);
                        ur2 = rows.get((Long) o2);
                    }
                    if (column == table.getColumnModel().getColumnIndex("Callback")) {
                        if (ur1.callbackId == ur2.callbackId) {
                            return 0;
                        } else if (ur1.callbackId > ur2.callbackId) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Progress")) {
                        if (ur1.bytesUploaded == ur2.bytesUploaded) {
                            return 0;
                        } else if (ur1.bytesUploaded > ur2.bytesUploaded) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Size")) {
                        if (ur1.bytesToUpload == ur2.bytesToUpload) {
                            return 0;
                        } else if (ur1.bytesToUpload > ur2.bytesToUpload) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Started")) {
                        if (ur1.timeStarted == ur2.timeStarted) {
                            return 0;
                        } else if (ur1.timeStarted > ur2.timeStarted) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Finished")) {
                        if (ur1.timeFinished == ur2.timeFinished) {
                            return 0;
                        } else if (ur1.timeFinished > ur2.timeFinished) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Status")) {
                        return ur1.status.toLowerCase().compareTo(ur2.status.toLowerCase());
                    } else {
                        return 0;
                    }
                } else {
                    return 1;
                }
            }
        }
    }

    private class UploadRow {

        public long callbackId = -1;
        public long timeStarted = 0;
        public long timeFinished = 0;
        public long bytesToUpload = 0;
        public long bytesUploaded = 0;
        public String status = "Unknown";

        public UploadRow(ServerMessageUpEvent smue) {
            update(smue);
        }

        public void handleServerMessageUpEvent(ServerMessageUpEvent smue) {
            update(smue);
        }

        private void update(ServerMessageUpEvent smue) {
            this.callbackId = smue.getCallbackId();
            if (smue.getType() == smue.TYPE_CREATED) {
                this.status = "Waiting";
            } else if (smue.getType() == smue.TYPE_STARTED) {
                this.status = "Uploading";
                this.timeStarted = smue.getTimestamp();
            } else if (smue.getType() == smue.TYPE_COMPLETED) {
                this.status = "Complete";
                this.timeFinished = smue.getTimestamp();
            }
            this.bytesToUpload = smue.getBytesToUpload();
            this.bytesUploaded = smue.getBytesUploaded();
        }
    }
}
