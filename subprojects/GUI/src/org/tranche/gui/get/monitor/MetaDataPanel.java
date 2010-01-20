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
package org.tranche.gui.get.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import org.tranche.get.GetFileToolEvent;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.GenericTable;
import org.tranche.gui.SortableTableModel;
import org.tranche.gui.Styles;
import org.tranche.hash.BigHash;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class MetaDataPanel extends JPanel {

    public static final int MAX_NUM_ROWS = 50;
    private MetaDataTableModel model = new MetaDataTableModel();
    private GenericTable table;

    public MetaDataPanel() {
        setName("Meta Data");
        setLayout(new BorderLayout());

        // create the table
        table = new GenericTable(model, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION) {

            public String getToolTipText(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                String returnVal = null;
                if (row >= 0) {
                    MetaDataRow mdr = model.getRow(row);
                    if (mdr.servers.size() > 0) {
                        returnVal = mdr.getServersList();
                    }
                }

                // if the text for the tool tip text is too long, shorten it.
                if (returnVal != null) {
                    if (returnVal.length() > 100) {
                        returnVal = returnVal.substring(0, 100) + "...";
                    } else if (returnVal.equals("")) {
                        returnVal = null;
                    }
                }

                return returnVal == null ? null : returnVal;
            }
        };

        // create and add a scroll pane containing the table
        GenericScrollPane scrollPane = new GenericScrollPane(table);
        scrollPane.setBackground(Color.GRAY);
        scrollPane.setHorizontalScrollBarPolicy(GenericScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(Styles.BORDER_NONE);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void handleMetaDataEvent(GetFileToolEvent agfte) {
        model.handleMetaDataEvent(agfte);
    }

    private class MetaDataTableModel extends SortableTableModel {

        private final String[] headers = new String[]{"Hash", "Started", "Finished", "Tries", "Servers", "Status"};
        private final Map<BigHash, MetaDataRow> rows = new HashMap<BigHash, MetaDataRow>();
        private final LinkedList<BigHash> order = new LinkedList<BigHash>();
        private final LinkedList<BigHash> orderReceived = new LinkedList<BigHash>();

        public void handleMetaDataEvent(GetFileToolEvent agfte) {
            boolean isNew = true;
            synchronized (rows) {
                isNew = !rows.containsKey(agfte.getFileHash());
            }
            if (isNew) {
                // make some space if need be
                synchronized (rows) {
                    while (rows.size() > MAX_NUM_ROWS) {
                        BigHash hash;
                        synchronized (orderReceived) {
                            hash = orderReceived.removeFirst();
                        }
                        synchronized (order) {
                            order.remove(hash);
                        }
                        rows.remove(hash);
                    }
                }

                MetaDataRow mdr = new MetaDataRow(agfte);
                synchronized (rows) {
                    rows.put(mdr.hash, mdr);
                }
                synchronized (order) {
                    order.add(mdr.hash);
                }
                synchronized (orderReceived) {
                    orderReceived.add(mdr.hash);
                }
            } else {
                MetaDataRow mdr;
                synchronized (rows) {
                    mdr = rows.get(agfte.getFileHash());
                    mdr.handleMetaDataEvent(agfte);
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

        public MetaDataRow getRow(int row) {
            BigHash hash;
            synchronized (order) {
                hash = order.get(row);
            }
            synchronized (rows) {
                return rows.get(hash);
            }
        }

        public int getRowOf(MetaDataRow mdr) {
            synchronized (order) {
                return order.indexOf(mdr);
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
                MetaDataRow mdr = getRow(row);
                if (mdr == null) {
                    return null;
                }
                switch (column) {
                    case 0:
                        return mdr.hash.toString();
                    case 1:
                        return String.valueOf(mdr.timeStarted);
                    case 2:
                        return String.valueOf(mdr.timeFinished);
                    case 3:
                        return String.valueOf(mdr.tries);
                    case 4:
                        return mdr.getServersList();
                    case 5:
                        return mdr.status;
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
                } else if (o1 instanceof BigHash && o2 instanceof BigHash) {
                    MetaDataRow mdr1, mdr2;
                    synchronized (rows) {
                        mdr1 = rows.get((BigHash) o1);
                        mdr2 = rows.get((BigHash) o2);
                    }
                    if (column == table.getColumnModel().getColumnIndex("Hash")) {
                        return mdr1.hash.toString().compareTo(mdr2.hash.toString().toLowerCase());
                    } else if (column == table.getColumnModel().getColumnIndex("Started")) {
                        if (mdr1.timeStarted == mdr2.timeStarted) {
                            return 0;
                        } else if (mdr1.timeStarted > mdr2.timeStarted) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Finished")) {
                        if (mdr1.timeFinished == mdr2.timeFinished) {
                            return 0;
                        } else if (mdr1.timeFinished > mdr2.timeFinished) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Tries")) {
                        if (mdr1.tries == mdr2.tries) {
                            return 0;
                        } else if (mdr1.tries > mdr2.tries) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Servers")) {
                        if (mdr1.servers.size() == mdr2.servers.size()) {
                            return 0;
                        } else if (mdr1.servers.size() > mdr2.servers.size()) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Status")) {
                        return mdr1.status.toLowerCase().compareTo(mdr2.status.toLowerCase());
                    } else {
                        return 0;
                    }
                } else {
                    return 1;
                }
            }
        }
    }

    private class MetaDataRow {

        public BigHash hash;
        public long timeStarted = 0;
        public long timeFinished = 0;
        public int tries = 0;
        public List<String> servers = new ArrayList<String>();
        public String status = "";

        public MetaDataRow(GetFileToolEvent agfte) {
            handleMetaDataEvent(agfte);
        }

        public void handleMetaDataEvent(GetFileToolEvent agfte) {
            hash = agfte.getFileHash();
            if (agfte.getAction() == agfte.ACTION_STARTING || agfte.getAction() == agfte.ACTION_STARTED) {
                timeStarted = agfte.getTimestamp();
            } else if (agfte.getAction() == agfte.ACTION_FAILED || agfte.getAction() == agfte.ACTION_FINISHED || agfte.getAction() == agfte.ACTION_SKIPPED) {
                timeFinished = agfte.getTimestamp();
            } else if (agfte.getAction() == agfte.ACTION_TRYING) {
                tries++;
            }
            if (agfte.getServer() != null && !agfte.getServer().equals("")) {
                if (!servers.contains(agfte.getServer())) {
                    servers.add(agfte.getServer());
                }
            }
            status = agfte.getActionString();
        }

        public String getServersList() {
            String servers = "";
            for (String url : this.servers) {
                if (servers.equals("")) {
                    servers = servers + url;
                } else {
                    servers = servers + ", " + url;
                }
            }
            return servers;
        }
    }
}
