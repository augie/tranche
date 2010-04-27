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
package org.tranche.gui.get;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;
import javax.swing.JPanel;
import org.tranche.commons.TextUtil;
import org.tranche.gui.SortableTableModel;
import org.tranche.gui.util.GUIUtil;
import org.tranche.hash.BigHash;
import org.tranche.project.ProjectSummary;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class DownloadsTableModel extends SortableTableModel {

    public static final String COLUMN_TITLE = "Title";
    public static final String COLUMN_SIZE = "Size";
    public static final String COLUMN_FILES = "Files";
    public static final String COLUMN_STATUS = "Status";
    public static final String COLUMN_PROGRESS = "Progress";
    private final String[] headers = new String[]{COLUMN_TITLE, COLUMN_SIZE, COLUMN_FILES, COLUMN_STATUS, COLUMN_PROGRESS};
    private final ArrayList<DownloadSummary> filteredDownloads = new ArrayList<DownloadSummary>();
    private Pattern filter = Pattern.compile("");
    private DownloadsTable table;

    public DownloadsTableModel() {
        DownloadPool.addListener(new DownloadPoolListener() {

            public void downloadAdded(DownloadPoolEvent event) {
                if (filter(event.getDownloadSummary())) {
                    addRow(event.getDownloadSummary());
                }
            }

            public void downloadUpdated(DownloadPoolEvent event) {
                if (contains(event.getDownloadSummary())) {
                    if (filter(event.getDownloadSummary())) {
                        updateRow(event.getDownloadSummary());
                    } else {
                        removeRow(event.getDownloadSummary());
                    }
                } else if (filter(event.getDownloadSummary())) {
                    addRow(event.getDownloadSummary());
                }
            }

            public void downloadRemoved(DownloadPoolEvent event) {
                removeRow(event.getDownloadSummary());
            }
        });
    }

    public void setTable(DownloadsTable table) {
        this.table = table;
    }

    public void addRow(DownloadSummary ds) {
        synchronized (filteredDownloads) {
            if (!filteredDownloads.contains(ds)) {
                filteredDownloads.add(ds);
            }
        }
        // sort the table
        resort();
        // get the row of the added project
        int row = getRowOf(ds);
        if (row >= 0) {
            repaintRowInserted(row);
        }
    }

    public void updateRow(DownloadSummary ds) {
        int startRow = getRowOf(ds);
        // sort the table
        resort();
        int newRow = getRowOf(ds);
        // should we repaint (and lose selections)?
        if (startRow != newRow) {
            repaintRowDeleted(startRow);
            repaintRowInserted(newRow);
        } else {
            repaintRow(startRow);
        }
    }

    public void removeRow(DownloadSummary ds) {
        int row = getRowOf(ds);
        synchronized (filteredDownloads) {
            filteredDownloads.remove(ds);
        }
        if (row >= 0) {
            repaintRowDeleted(row);
        }
    }

    private boolean filter(DownloadSummary ds) {
        try {
            boolean returnVal = false;
            ProjectSummary ps = ds.getProjectSummary();
            if (ps != null) {
                if (ps.title != null) {
                    returnVal = returnVal || filter.matcher(ps.title.toLowerCase()).find();
                } else {
                    returnVal = returnVal || filter.matcher("").find();
                }
                if (ps.description != null) {
                    returnVal = returnVal || filter.matcher(ps.description.toLowerCase()).find();
                } else {
                    returnVal = returnVal || filter.matcher("").find();
                }
                if (ps.hash != null) {
                    returnVal = returnVal || filter.matcher(ps.hash.toString().toLowerCase()).find();
                } else {
                    returnVal = returnVal || filter.matcher("").find();
                }
            } else {
                returnVal = filter.matcher("").find();
            }
            return returnVal;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean contains(DownloadSummary ds) {
        synchronized (filteredDownloads) {
            return filteredDownloads.contains(ds);
        }
    }

    public int getColumnCount() {
        return headers.length;
    }

    @Override
    public String getColumnName(int column) {
        if (column < getColumnCount()) {
            return headers[column];
        } else {
            return "";
        }
    }

    public DownloadSummary getRow(int filteredIndex) {
        try {
            synchronized (filteredDownloads) {
                return filteredDownloads.get(filteredIndex);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public int getRowOf(DownloadSummary ds) {
        synchronized (filteredDownloads) {
            return filteredDownloads.indexOf(ds);
        }
    }

    public int getRowCount() {
        synchronized (filteredDownloads) {
            return filteredDownloads.size();
        }
    }

    public long getFiles(int filteredIndex) {
        return getRow(filteredIndex).filesToDownload;
    }

    public BigHash getHash(int filteredIndex) {
        return getRow(filteredIndex).getGetFileTool().getHash();
    }

    public JPanel getProgress(int filteredIndex) {
        return getRow(filteredIndex).getProgressBar();
    }

    public String getDescription(int filteredIndex) {
        return getRow(filteredIndex).getProjectSummary().description;
    }

    public long getSize(int filteredIndex) {
        return getRow(filteredIndex).bytesToDownload;
    }

    public String getStatus(int filteredIndex) {
        return getRow(filteredIndex).getStatus();
    }

    public String getTitle(int filteredIndex) {
        return getRow(filteredIndex).getProjectSummary().title;
    }

    public Object getValueAt(int row, int column) {
        try {
            switch (column) {
                case 0:
                    return getTitle(row);
                case 1:
                    if (getSize(row) >= 0) {
                        return TextUtil.formatBytes(getSize(row));
                    } else {
                        return "";
                    }
                case 2:
                    if (getFiles(row) >= 0) {
                        return GUIUtil.integerFormat.format(getFiles(row));
                    } else {
                        return "";
                    }
                case 3:
                    return getStatus(row);
                case 4:
                    return getProgress(row);
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
                // there might be concurrent modification exceptions
                try {
                    // filter
                    Collection<DownloadSummary> summaries = DownloadPool.getAll();
                    synchronized (filteredDownloads) {
                        filteredDownloads.clear();
                        for (DownloadSummary ds : summaries) {
                            if (filter(ds)) {
                                filteredDownloads.add(ds);
                            }
                        }
                    }
                    // sort
                    resort();
                    // repaint
                    repaintTable();
                } catch (Exception e) {
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    public void sort(int column) {
        if (column >= headers.length || column < 0) {
            return;
        }
        table.setPressedColumn(column);
        synchronized (filteredDownloads) {
            Collections.sort(filteredDownloads, new DSComparator(column));
        }
    }

    public void resort() {
        sort(table.getPressedColumn());
    }

    private class DSComparator implements Comparator {

        private int column;

        public DSComparator(int column) {
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
            } else if (o1 instanceof DownloadSummary && o2 instanceof DownloadSummary) {
                DownloadSummary ds1 = (DownloadSummary) o1;
                int index1 = getRowOf(ds1);
                DownloadSummary ds2 = (DownloadSummary) o2;
                int index2 = getRowOf(ds2);
                if (column == table.getColumnModel().getColumnIndex(COLUMN_TITLE)) {
                    return getTitle(index1).toLowerCase().compareTo(getTitle(index2).toLowerCase());
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_SIZE)) {
                    if (getSize(index1) == getSize(index2)) {
                        return 0;
                    } else if (getSize(index1) > getSize(index2)) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_FILES)) {
                    if (getFiles(index1) == getFiles(index2)) {
                        return 0;
                    } else if (getFiles(index1) > getFiles(index2)) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_STATUS)) {
                    return ds1.getStatus().compareTo(ds2.getStatus());
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_PROGRESS)) {
                    if (ds1.getGetFileTool().getTimeEstimator() != null && ds2.getGetFileTool().getTimeEstimator() != null) {
                        if (ds1.getGetFileTool().getTimeEstimator().getPercentDone() == ds2.getGetFileTool().getTimeEstimator().getPercentDone()) {
                            return 0;
                        } else if (ds1.getGetFileTool().getTimeEstimator().getPercentDone() > ds2.getGetFileTool().getTimeEstimator().getPercentDone()) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else {
                        if (ds1.getGetFileTool().getTimeEstimator() == null && ds2.getGetFileTool().getTimeEstimator() == null) {
                            return 0;
                        } else if (ds1.getGetFileTool().getTimeEstimator() != null) {
                            return 1;
                        } else {
                            return -1;
                        }
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
