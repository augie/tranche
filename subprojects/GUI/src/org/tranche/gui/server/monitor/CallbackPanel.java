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
import org.tranche.servers.ServerCallbackEvent;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class CallbackPanel extends JPanel {

    public static final int MAX_NUM_ROWS = 50;
    private static final String BLANK = "";
    private static final String REQUESTS = "Requests";
    private CallbackTableModel model = new CallbackTableModel();
    private GenericTable table;

    public CallbackPanel() {
        setName(REQUESTS);
        setLayout(new BorderLayout());

        // create the table
        table = new GenericTable(model, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // create and add a scroll pane containing the table
        GenericScrollPane scrollPane = new GenericScrollPane(table);
        scrollPane.setBackground(Color.GRAY);
        scrollPane.setHorizontalScrollBarPolicy(scrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(Styles.BORDER_NONE);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void handleServerCallbackEvent(ServerCallbackEvent sce) {
        model.handleServerCallbackEvent(sce);
    }

    private class CallbackTableModel extends SortableTableModel {

        private static final String ID = "ID";
        private static final String NAME = "Name";
        private static final String DESCRIPTION = "Description";
        private static final String CREATED = "Created";
        private static final String FINISHED = "Finished";
        private static final String TIME_TO_FINISH = "Time To Finish";
        private static final String STATUS = "Status";
        private String[] headers = new String[]{ID, NAME, DESCRIPTION, CREATED, FINISHED, TIME_TO_FINISH, STATUS};
        private Map<Long, CallbackRow> rows = new HashMap<Long, CallbackRow>();
        private LinkedList<Long> order = new LinkedList<Long>();
        private LinkedList<Long> orderReceived = new LinkedList<Long>();

        public void handleServerCallbackEvent(ServerCallbackEvent sce) {
            boolean isNew = true;
            synchronized (rows) {
                isNew = !rows.containsKey(sce.getId());
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

                CallbackRow cr = new CallbackRow(sce);
                synchronized (rows) {
                    rows.put(sce.getId(), cr);
                }
                synchronized (order) {
                    order.add(cr.id);
                }
                synchronized (orderReceived) {
                    orderReceived.add(cr.id);
                }
            } else {
                CallbackRow cr;
                synchronized (rows) {
                    cr = rows.get(sce.getId());
                    cr.handleServerCallbackEvent(sce);
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
                return BLANK;
            }
        }

        public CallbackRow getRow(int row) {
            long callbackId = -1;
            synchronized (order) {
                callbackId = order.get(row);
            }
            synchronized (rows) {
                return rows.get(callbackId);
            }
        }

        public int getRowOf(CallbackRow cr) {
            synchronized (order) {
                return order.indexOf(cr);
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
                CallbackRow cr = getRow(row);
                if (cr == null) {
                    return null;
                }
                switch (column) {
                    case 0:
                        return String.valueOf(cr.id);
                    case 1:
                        return cr.name;
                    case 2:
                        return cr.description;
                    case 3:
                        if (cr.timeCreated == -1) {
                            return BLANK;
                        } else {
                            return String.valueOf(cr.timeCreated);
                        }
                    case 4:
                        if (cr.timeFinished == -1) {
                            return BLANK;
                        } else {
                            return String.valueOf(cr.timeFinished);
                        }
                    case 5:
                        if (cr.timeToFinish == -1) {
                            return BLANK;
                        } else {
                            return String.valueOf(cr.timeToFinish);
                        }
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
                } else if (o1 instanceof Long && o2 instanceof Long) {
                    CallbackRow cr1, cr2;
                    synchronized (rows) {
                        cr1 = rows.get((Long) o1);
                        cr2 = rows.get((Long) o2);
                    }
                    if (column == table.getColumnModel().getColumnIndex(ID)) {
                        if (cr1.id == cr2.id) {
                            return 0;
                        } else if (cr1.id > cr2.id) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex(NAME)) {
                        return cr1.name.toLowerCase().compareTo(cr2.name.toLowerCase());
                    } else if (column == table.getColumnModel().getColumnIndex(DESCRIPTION)) {
                        return cr1.description.toLowerCase().compareTo(cr2.description.toLowerCase());
                    } else if (column == table.getColumnModel().getColumnIndex(CREATED)) {
                        if (cr1.timeCreated == cr2.timeCreated) {
                            return 0;
                        } else if (cr1.timeCreated > cr2.timeCreated) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex(FINISHED)) {
                        if (cr1.timeFinished == cr2.timeFinished) {
                            return 0;
                        } else if (cr1.timeFinished > cr2.timeFinished) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex(TIME_TO_FINISH)) {
                        if (cr1.timeToFinish == cr2.timeToFinish) {
                            return 0;
                        } else if (cr1.timeToFinish > cr2.timeToFinish) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (column == table.getColumnModel().getColumnIndex(STATUS)) {
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

    private class CallbackRow {

        private static final String WAITING = "Waiting";
        public long id = -1;
        public long timeCreated = -1;
        public long timeFinished = -1;
        public long timeToFinish = -1;
        public String name;
        public String description;
        public String status;

        public CallbackRow(ServerCallbackEvent sce) {
            update(sce);
        }

        public void handleServerCallbackEvent(ServerCallbackEvent sce) {
            update(sce);
        }

        private void update(ServerCallbackEvent sce) {
            id = sce.getId();
            if (sce.getType() == sce.TYPE_CREATED) {
                timeCreated = sce.getTimestamp();
                status = WAITING;
            } else if (sce.getType() == sce.TYPE_FULFILLED || sce.getType() == sce.TYPE_FAILED) {
                timeFinished = sce.getTimestamp();
                if (timeCreated != -1) {
                    timeToFinish = timeFinished - timeCreated;
                }
                status = sce.getTypeString();
            }
            if (name == null) {
                name = sce.getName();
            }
            if (description == null) {
                description = sce.getDescription();
            }
        }
    }
}
