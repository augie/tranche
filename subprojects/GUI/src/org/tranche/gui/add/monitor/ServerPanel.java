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
package org.tranche.gui.add.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import org.tranche.add.AddFileToolEvent;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.GenericTable;
import org.tranche.gui.SortableTableModel;
import org.tranche.gui.Styles;
import org.tranche.hash.BigHash;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServerPanel extends JPanel {

    public static final int MAX_NUM_ROWS = 50;
    private ServerTableModel model = new ServerTableModel();
    private GenericTable table;

    public ServerPanel() {
        setName("Servers");
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

    public void handleEvent(AddFileToolEvent event) {
        model.handleEvent(event);
    }

    private class ServerTableModel extends SortableTableModel {

        private final String[] headers = new String[]{"Server", "MD Tries", "MD Uploaded", "Chunk Tries", "Chunks Uploaded"};
        private final Map<String, ServerRow> rows = new HashMap<String, ServerRow>();
        private final LinkedList<String> order = new LinkedList<String>();
        private final LinkedList<String> orderReceived = new LinkedList<String>();

        public void handleEvent(AddFileToolEvent event) {
            if (event.getServer() == null || event.getServer().equals("")) {
                return;
            }
            boolean isNew = true;
            synchronized (rows) {
                isNew = !rows.containsKey(event.getServer());
            }
            if (isNew) {
                // make some space if need be
                synchronized (rows) {
                    while (rows.size() > MAX_NUM_ROWS) {
                        String url;
                        synchronized (orderReceived) {
                            url = orderReceived.removeFirst();
                        }
                        synchronized (order) {
                            order.remove(url);
                        }
                        rows.remove(url);
                    }
                }

                ServerRow sr = new ServerRow(event);
                synchronized (rows) {
                    rows.put(sr.host, sr);
                }
                synchronized (order) {
                    order.add(sr.host);
                }
                synchronized (orderReceived) {
                    orderReceived.add(sr.host);
                }
            } else {
                ServerRow sr;
                synchronized (rows) {
                    sr = rows.get(event.getServer());
                    sr.handleEvent(event);
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

        public ServerRow getRow(int row) {
            String url;
            synchronized (order) {
                url = order.get(row);
            }
            synchronized (rows) {
                return rows.get(url);
            }
        }

        public int getRowOf(ServerRow sr) {
            synchronized (order) {
                return order.indexOf(sr);
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
                ServerRow sr = getRow(row);
                if (sr == null) {
                    return null;
                }
                switch (column) {
                    case 0:
                        return sr.host;
                    case 1:
                        return String.valueOf(sr.metadataTries);
                    case 2:
                        return String.valueOf(sr.metadata);
                    case 3:
                        return String.valueOf(sr.chunkTries);
                    case 4:
                        return String.valueOf(sr.chunks);
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
                } else if (o1 instanceof String && o2 instanceof String) {
                    ServerRow sr1, sr2;
                    synchronized (rows) {
                        sr1 = rows.get((String) o1);
                        sr2 = rows.get((String) o2);
                    }
                    if (column == table.getColumnModel().getColumnIndex("Server")) {
                        return sr1.host.compareTo(sr2.host.toLowerCase());
                    } else if (column == table.getColumnModel().getColumnIndex("MD Tries")) {
                        if (sr1.metadataTries == sr2.metadataTries) {
                            return 0;
                        } else if (sr1.metadataTries > sr2.metadataTries) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("MD Uploaded")) {
                        if (sr1.metadata == sr2.metadata) {
                            return 0;
                        } else if (sr1.metadata > sr2.metadata) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Chunk Tries")) {
                        if (sr1.chunkTries == sr2.chunkTries) {
                            return 0;
                        } else if (sr1.chunkTries > sr2.chunkTries) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Chunks Uploaded")) {
                        if (sr1.chunks == sr2.chunks) {
                            return 0;
                        } else if (sr1.chunks > sr2.chunks) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else {
                        return 0;
                    }
                } else {
                    return 1;
                }
            }
        }
    }

    public static String makeUniqueID(String fileRelativeName, BigHash hash) {
        return fileRelativeName + hash.toString();
    }

    public static String makeUniqueID(AddFileToolEvent event) {
        return makeUniqueID(event.getFileName(), event.getChunkHash());
    }

    private class ServerRow {

        public String host;
        public int metadata = 0;
        public int metadataTries = 0;
        public int chunks = 0;
        public int chunkTries = 0;

        public ServerRow(AddFileToolEvent event) {
            handleEvent(event);
        }

        public void handleEvent(AddFileToolEvent event) {
            if (event.getServer() != null && !event.getServer().equals("")) {
                host = event.getServer();
                if (event.getType() == AddFileToolEvent.TYPE_METADATA) {
                    if (event.getAction() == AddFileToolEvent.ACTION_TRYING) {
                        metadataTries++;
                    } else if (event.getAction() == AddFileToolEvent.ACTION_UPLOADED) {
                        metadata++;
                    }
                } else if (event.getType() == AddFileToolEvent.TYPE_DATA) {
                    if (event.getAction() == AddFileToolEvent.ACTION_TRYING) {
                        chunkTries++;
                    } else if (event.getAction() == AddFileToolEvent.ACTION_UPLOADED) {
                        chunks++;
                    }
                }
            }
        }
    }
}
