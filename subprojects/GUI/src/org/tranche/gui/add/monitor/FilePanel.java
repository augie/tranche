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
public class FilePanel extends JPanel {

    public static final int MAX_NUM_ROWS = 50;
    private FileTableModel model = new FileTableModel();
    private GenericTable table;

    public FilePanel() {
        setName("Files");
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

    public void handleFileEvent(AddFileToolEvent event) {
        model.handleFileEvent(event);
    }

    private class FileTableModel extends SortableTableModel {

        private final String[] headers = new String[]{"Name", "Hash", "Started", "Finished", "Status"};
        private final Map<BigHash, FileRow> rows = new HashMap<BigHash, FileRow>();
        private final LinkedList<BigHash> order = new LinkedList<BigHash>();
        private final LinkedList<BigHash> orderReceived = new LinkedList<BigHash>();

        public void handleFileEvent(AddFileToolEvent event) {
            boolean isNew = true;
            synchronized (rows) {
                isNew = !rows.containsKey(event.getFileHash());
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

                FileRow fr = new FileRow(event);
                synchronized (rows) {
                    rows.put(fr.hash, fr);
                }
                synchronized (order) {
                    order.add(fr.hash);
                }
                synchronized (orderReceived) {
                    orderReceived.add(fr.hash);
                }
            } else {
                FileRow fr;
                synchronized (rows) {
                    fr = rows.get(event.getFileHash());
                    fr.handleFileEvent(event);
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

        public FileRow getRow(int row) {
            BigHash hash;
            synchronized (order) {
                hash = order.get(row);
            }
            synchronized (rows) {
                return rows.get(hash);
            }
        }

        public int getRowOf(FileRow fr) {
            synchronized (order) {
                return order.indexOf(fr);
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
                FileRow fr = getRow(row);
                if (fr == null) {
                    return null;
                }
                switch (column) {
                    case 0:
                        return fr.fileName;
                    case 1:
                        return fr.hash.toString();
                    case 2:
                        return String.valueOf(fr.timeStarted);
                    case 3:
                        return String.valueOf(fr.timeFinished);
                    case 4:
                        return fr.status;
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
                    FileRow fr1, fr2;
                    synchronized (rows) {
                        fr1 = rows.get((BigHash) o1);
                        fr2 = rows.get((BigHash) o2);
                    }
                    if (column == table.getColumnModel().getColumnIndex("Name")) {
                        return fr1.fileName.compareTo(fr2.fileName.toLowerCase());
                    } else if (column == table.getColumnModel().getColumnIndex("Hash")) {
                        return fr1.hash.toString().compareTo(fr2.hash.toString().toLowerCase());
                    } else if (column == table.getColumnModel().getColumnIndex("Started")) {
                        if (fr1.timeStarted == fr2.timeStarted) {
                            return 0;
                        } else if (fr1.timeStarted > fr2.timeStarted) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Finished")) {
                        if (fr1.timeFinished == fr2.timeFinished) {
                            return 0;
                        } else if (fr1.timeFinished > fr2.timeFinished) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex("Status")) {
                        return fr1.status.toLowerCase().compareTo(fr2.status.toLowerCase());
                    } else {
                        return 0;
                    }
                } else {
                    return 1;
                }
            }
        }
    }

    private class FileRow {

        public BigHash hash;
        public long timeStarted = 0;
        public long timeFinished = 0;
        public String fileName = "";
        public String status = "";

        public FileRow(AddFileToolEvent event) {
            handleFileEvent(event);
        }

        public void handleFileEvent(AddFileToolEvent event) {
            hash = event.getFileHash();
            if (event.getAction() == AddFileToolEvent.ACTION_STARTING || event.getAction() == AddFileToolEvent.ACTION_STARTED) {
                timeStarted = event.getTimestamp();
                if (event.getFileName() != null && !event.getFileName().equals("")) {
                    fileName = event.getFileName();
                }
            } else if (event.getAction() == AddFileToolEvent.ACTION_FAILED || event.getAction() == AddFileToolEvent.ACTION_FINISHED) {
                timeFinished = event.getTimestamp();
            }
            status = event.getActionString();
        }
    }
}
