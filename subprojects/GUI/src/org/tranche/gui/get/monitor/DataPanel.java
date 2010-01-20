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
public class DataPanel extends JPanel {

    public static final int MAX_NUM_ROWS = 50;
    private DataTableModel model = new DataTableModel();
    private GenericTable table;

    public DataPanel() {
        setName("Data");
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

    public void handleDataEvent(GetFileToolEvent agfte) {
        model.handleDataEvent(agfte);
    }

    private class DataTableModel extends SortableTableModel {

        private final String[] headers = new String[]{"File", "Chunk", "Started", "Finished", "Tries", "Servers", "Status"};
        private final Map<String, ChunkRow> rows = new HashMap<String, ChunkRow>();
        private final LinkedList<String> order = new LinkedList<String>();
        private final LinkedList<String> orderReceived = new LinkedList<String>();

        public void handleDataEvent(GetFileToolEvent agfte) {
            boolean isNew = true;
            synchronized (rows) {
                isNew = !rows.containsKey(makeUniqueID(agfte));
            }
            if (isNew) {
                // make some space if need be
                synchronized (rows) {
                    while (rows.size() > MAX_NUM_ROWS) {
                        String uniqueId;
                        synchronized (orderReceived) {
                            uniqueId = orderReceived.removeFirst();
                        }
                        synchronized (order) {
                            order.remove(uniqueId);
                        }
                        rows.remove(uniqueId);
                    }
                }

                ChunkRow cr = new ChunkRow(agfte);
                synchronized (rows) {
                    rows.put(cr.uniqueId, cr);
                }
                synchronized (order) {
                    order.add(cr.uniqueId);
                }
                synchronized (orderReceived) {
                    orderReceived.add(cr.uniqueId);
                }
            } else {
                ChunkRow cr;
                synchronized (rows) {
                    cr = rows.get(makeUniqueID(agfte));
                }
                cr.handleChunkEvent(agfte);
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

        public ChunkRow getRow(int row) {
            String uniqueID;
            synchronized (order) {
                uniqueID = order.get(row);
            }
            synchronized (rows) {
                return rows.get(uniqueID);
            }
        }

        public int getRowOf(ChunkRow cr) {
            synchronized (order) {
                return order.indexOf(cr);
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
                ChunkRow cr = getRow(row);
                if (cr == null) {
                    return null;
                }
                switch (column) {
                    case 0:
                        return cr.fileHash.toString();
                    case 1:
                        return cr.chunkHash.toString();
                    case 2:
                        return String.valueOf(cr.timeStarted);
                    case 3:
                        return String.valueOf(cr.timeFinished);
                    case 4:
                        return String.valueOf(cr.tries);
                    case 5:
                        return cr.getServersList();
                    case 6:
                        return cr.status;
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
                    ChunkRow cr1, cr2;
                    synchronized (rows) {
                        cr1 = rows.get((String) o1);
                        cr2 = rows.get((String) o2);
                    }
                    if (column == table.getColumnModel().getColumnIndex("File")) {
                        return cr1.fileHash.toString().compareTo(cr2.fileHash.toString().toLowerCase());
                    } else if (column == table.getColumnModel().getColumnIndex("Chunk")) {
                        return cr1.chunkHash.toString().compareTo(cr2.chunkHash.toString().toLowerCase());
                    } else if (column == table.getColumnModel().getColumnIndex("Started")) {
                        if (cr1.timeStarted == cr2.timeStarted) {
                            return 0;
                        } else if (cr1.timeStarted > cr2.timeStarted) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Finished")) {
                        if (cr1.timeFinished == cr2.timeFinished) {
                            return 0;
                        } else if (cr1.timeFinished > cr2.timeFinished) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Tries")) {
                        if (cr1.tries == cr2.tries) {
                            return 0;
                        } else if (cr1.tries > cr2.tries) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Servers")) {
                        if (cr1.servers.size() == cr2.servers.size()) {
                            return 0;
                        } else if (cr1.servers.size() > cr2.servers.size()) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Status")) {
                        return cr1.status.toLowerCase().compareTo(cr2.status.toLowerCase());
                    } else {
                        return 0;
                    }
                } else {
                    return 1;
                }
            }
        }
    }

    public static String makeUniqueID(BigHash fileHash, BigHash chunkHash) {
        return fileHash.toString() + chunkHash.toString();
    }

    public static String makeUniqueID(GetFileToolEvent agfte) {
        return agfte.getFileHash().toString() + agfte.getChunkHash().toString();
    }

    private class ChunkRow {

        public String uniqueId = "";
        public BigHash chunkHash;
        public BigHash fileHash;
        public long timeStarted = 0;
        public long timeFinished = 0;
        public int tries = 0;
        public List<String> servers = new ArrayList<String>();
        public String status = "";

        public ChunkRow(GetFileToolEvent agfte) {
            handleChunkEvent(agfte);
        }

        public void handleChunkEvent(GetFileToolEvent event) {
            fileHash = event.getFileHash();
            chunkHash = event.getChunkHash();
            uniqueId = makeUniqueID(event);
            if (event.getAction() == GetFileToolEvent.ACTION_STARTING || event.getAction() == GetFileToolEvent.ACTION_STARTED) {
                timeStarted = event.getTimestamp();
            } else if (event.getAction() == GetFileToolEvent.ACTION_FAILED || event.getAction() == GetFileToolEvent.ACTION_FINISHED || event.getAction() == GetFileToolEvent.ACTION_SKIPPED) {
                timeFinished = event.getTimestamp();
            } else if (event.getAction() == GetFileToolEvent.ACTION_TRYING) {
                tries++;
            }
            if (event.getServer() != null && !event.getServer().equals("")) {
                if (!servers.contains(event.getServer())) {
                    servers.add(event.getServer());
                }
            }
            status = event.getActionString();
        }

        public String getServersList() {
            String serversList = "";
            for (String url : servers) {
                if (serversList.equals("")) {
                    serversList = serversList + url;
                } else {
                    serversList = serversList + ", " + url;
                }
            }
            return serversList;
        }
    }
}
