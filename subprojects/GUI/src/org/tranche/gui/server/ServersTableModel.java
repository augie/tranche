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
package org.tranche.gui.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.tranche.gui.SortableTableModel;
import org.tranche.network.ConnectionEvent;
import org.tranche.network.ConnectionListener;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableEvent;
import org.tranche.network.StatusTableListener;
import org.tranche.network.StatusTableRow;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServersTableModel extends SortableTableModel {

    public static final String COLUMN_USE = "Use";
    public static final String COLUMN_NAME = "Name";
    public static final String COLUMN_URL = "URL";
    public static final String COLUMN_GROUP = "Group";
    public static final String COLUMN_ONLINE = "";
    public static final String COLUMN_CONNECTED = "";
    private static final String[] HEADERS = new String[]{COLUMN_USE, COLUMN_NAME, COLUMN_URL, COLUMN_GROUP, COLUMN_ONLINE, COLUMN_CONNECTED};
    private Pattern filter = Pattern.compile("");
    private ServersTable table;
    private boolean showUse = true;
    private final List<String> filteredHosts = new ArrayList<String>();
    private final Set<String> selectedHosts = new HashSet<String>();

    public ServersTableModel() {
        NetworkUtil.getStatus().addListener(new StatusTableListener() {

            public void rowsAdded(StatusTableEvent event) {
                for (String host : event.getHosts()) {
                    if (filter(host)) {
                        if (contains(host)) {
                            update(host);
                        } else {
                            add(host);
                        }
                    }
                }
            }

            public void rowsUpdated(StatusTableEvent event) {
                for (String host : event.getHosts()) {
                    if (filter(host)) {
                        if (contains(host)) {
                            update(host);
                        } else {
                            add(host);
                        }
                    } else {
                        remove(host);
                    }
                }
            }

            public void rowsRemoved(StatusTableEvent event) {
                for (String host : event.getHosts()) {
                    remove(host);
                }
            }
        });
        ConnectionUtil.addListener(new ConnectionListener() {

            public void connectionMade(ConnectionEvent event) {
                update(event.getHost());
            }

            public void connectionDropped(ConnectionEvent event) {
                update(event.getHost());
            }
        });
        // make sure network is loaded
        NetworkUtil.lazyLoad();
    }

    public boolean getShowUseColumn() {
        return showUse;
    }

    public void setShowUseColumn(boolean showUse) {
        this.showUse = showUse;
    }

    public void setTable(ServersTable serversTable) {
        this.table = serversTable;
    }

    public void clickUse(int row) {
        String host = get(row).getHost();
        synchronized (selectedHosts) {
            if (selectedHosts.contains(host)) {
                selectedHosts.remove(host);
            } else {
                selectedHosts.add(host);
            }
        }
    }

    public Collection<String> getSelectedHosts() {
        List<String> list = new LinkedList<String>();
        synchronized (selectedHosts) {
            list.addAll(selectedHosts);
        }
        return Collections.unmodifiableCollection(list);
    }

    public Collection<String> getFilteredHosts() {
        List<String> list = new LinkedList<String>();
        synchronized (filteredHosts) {
            list.addAll(filteredHosts);
        }
        return Collections.unmodifiableCollection(list);
    }

    public void setSelectedHosts(Collection<String> selectedHosts) {
        if (selectedHosts != null) {
            synchronized (this.selectedHosts) {
                this.selectedHosts.clear();
                this.selectedHosts.addAll(selectedHosts);
            }
        }
    }

    public boolean contains(String host) {
        synchronized (filteredHosts) {
            return filteredHosts.contains(host);
        }
    }

    public void add(String host) {
        synchronized (filteredHosts) {
            if (!filteredHosts.contains(host)) {
                filteredHosts.add(host);
            }
        }
        // sort the table
        resort();
        // get the row of the added project
        int row = getRowOf(host);
        if (row >= 0) {
            repaintRowInserted(row);
        }
    }

    public void update(String host) {
        int startRow = getRowOf(host);
        // sort the table
        resort();
        int newRow = getRowOf(host);
        // should we repaint (and lose selections)?
        if (startRow != newRow) {
            repaintRowDeleted(startRow);
            repaintRowInserted(newRow);
        } else {
            repaintRow(startRow);
        }
    }

    public void remove(String host) {
        int row = getRowOf(host);
        synchronized (filteredHosts) {
            filteredHosts.remove(host);
        }
        if (row >= 0) {
            repaintRowDeleted(row);
        }
    }

    public StatusTableRow get(int filteredIndex) {
        if (getRowCount() > filteredIndex && filteredIndex >= 0) {
            return NetworkUtil.getStatus().getRow(getHost(filteredIndex));
        } else {
            return null;
        }
    }

    public int getRowOf(String host) {
        synchronized (filteredHosts) {
            return filteredHosts.indexOf(host);
        }
    }

    public int getRowCount() {
        synchronized (filteredHosts) {
            return filteredHosts.size();
        }
    }

    public String getName(int filteredIndex) {
        return get(filteredIndex).getName();
    }

    public String getURL(int filteredIndex) {
        return get(filteredIndex).getURL();
    }

    public String getHost(int filteredIndex) {
        synchronized (filteredHosts) {
            return filteredHosts.get(filteredIndex);
        }
    }

    public String getGroup(int filteredIndex) {
        return get(filteredIndex).getGroup();
    }

    public Boolean isUse(int filteredIndex) {
        String host = get(filteredIndex).getHost();
        synchronized (selectedHosts) {
            return selectedHosts.contains(host);
        }
    }

    public boolean isConnected(int filteredIndex) {
        return ConnectionUtil.isConnected(get(filteredIndex).getHost());
    }

    public boolean isOnline(int filteredIndex) {
        return get(filteredIndex).isOnline();
    }

    private boolean filter(String host) {
        if (filter.matcher(host.toLowerCase()).find()) {
            return true;
        } else {
            return false;
        }
    }

    public int getColumnCount() {
        int count = HEADERS.length;
        if (!showUse) {
            count--;
        }
        return count;
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
        if (!showUse) {
            column++;
        }
        if (column < HEADERS.length) {
            return HEADERS[column];
        } else {
            return "";
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return (showUse && column == 0 && get(row).isOnline());
    }

    public Object getValueAt(int row, int column) {
        try {
            if (!showUse) {
                column++;
            }
            switch (column) {
                case 0:
                    return isUse(row);
                case 1:
                    return getName(row);
                case 2:
                    return getURL(row);
                case 3:
                    return getGroup(row);
                case 4:
                    return isOnline(row);
                case 5:
                    return isConnected(row);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public String getFilter() {
        return filter.pattern();
    }

    public void setFilter(String filter) {
        this.filter = Pattern.compile(filter.toLowerCase());
        Thread t = new Thread() {

            @Override
            public void run() {
                // filter
                Collection<String> hosts = NetworkUtil.getStatus().getHosts();
                synchronized (filteredHosts) {
                    filteredHosts.clear();
                    for (String host : hosts) {
                        if (filter(host)) {
                            filteredHosts.add(host);
                        }
                    }
                }
                // sort
                resort();
                // repaint
                repaintTable();

            }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public void sort(int column) {
        if (column >= getColumnCount() || column < 0) {
            return;
        }
        table.setPressedColumn(column);
        synchronized (filteredHosts) {
            Collections.sort(filteredHosts, new RowComparator(column));
        }
    }

    public void resort() {
        sort(table.getPressedColumn());
    }

    private class RowComparator implements Comparator<String> {

        private int column;

        public RowComparator(int column) {
            this.column = column;
        }

        public int compare(String o1, String o2) {
            if (table.getDirection()) {
                String temp = o1;
                o1 = o2;
                o2 = temp;
            }

            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return 1;
            } else {
                int row1Index = getRowOf(o1);
                int row2Index = getRowOf(o2);
                if (showUse && column == table.getColumnModel().getColumnIndex(COLUMN_USE)) {
                    if (isUse(row1Index) == true && isUse(row2Index) == false) {
                        return 1;
                    } else if (isUse(row1Index) == false && isUse(row2Index) == true) {
                        return -1;
                    } else {
                        return 0;
                    }
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_NAME)) {
                    return getName(row1Index).toLowerCase().compareTo(getName(row2Index).toLowerCase());
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_URL)) {
                    return getURL(row1Index).toLowerCase().compareTo(getURL(row2Index).toLowerCase());
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_GROUP)) {
                    return getGroup(row1Index).toLowerCase().compareTo(getGroup(row2Index).toLowerCase());
                } else if ((showUse && column == 4) || (!showUse && column == 3)) {
                    if (isOnline(row1Index) == true && isOnline(row2Index) == false) {
                        return 1;
                    } else if (isOnline(row1Index) == false && isOnline(row2Index) == true) {
                        return -1;
                    } else {
                        return 0;
                    }
                } else if ((showUse && column == 5) || (!showUse && column == 4)) {
                    if (isConnected(row1Index) == true && isConnected(row2Index) == false) {
                        return 1;
                    } else if (isConnected(row1Index) == false && isConnected(row2Index) == true) {
                        return -1;
                    } else {
                        return 0;
                    }
                } else {
                    return 0;
                }
            }
        }
    }
}
