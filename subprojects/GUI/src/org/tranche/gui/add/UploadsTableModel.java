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
package org.tranche.gui.add;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Pattern;
import org.tranche.commons.TextUtil;
import org.tranche.gui.SortableTableModel;
import org.tranche.gui.util.GUIUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class UploadsTableModel extends SortableTableModel {

    public static final String COLUMN_TITLE = "Title";
    public static final String COLUMN_SIZE = "Size";
    public static final String COLUMN_FILES = "Files";
    public static final String COLUMN_STATUS = "Status";
    public static final String COLUMN_DATE = "Started";
    public static final String COLUMN_PROGRESS = "Progress";
    private final String[] headers = new String[]{COLUMN_TITLE, COLUMN_SIZE, COLUMN_FILES, COLUMN_STATUS, COLUMN_DATE, COLUMN_PROGRESS};
    private final ArrayList<UploadSummary> filteredUploads = new ArrayList<UploadSummary>();
    private Pattern filter = Pattern.compile("");
    private UploadsTable table;

    public UploadsTableModel() {
        UploadPool.addListener(new UploadPoolListener() {

            public void uploadAdded(UploadPoolEvent event) {
                if (filter(event.getUploadSummary())) {
                    addRow(event.getUploadSummary());
                }
            }

            public void uploadUpdated(UploadPoolEvent event) {
                if (contains(event.getUploadSummary())) {
                    if (filter(event.getUploadSummary())) {
                        updateRow(event.getUploadSummary());
                    } else {
                        removeRow(event.getUploadSummary());
                    }
                }
            }

            public void uploadRemoved(UploadPoolEvent event) {
                removeRow(event.getUploadSummary());
            }
        });
    }

    public void setTable(UploadsTable table) {
        this.table = table;
    }

    public void addRow(UploadSummary us) {
        synchronized (filteredUploads) {
            filteredUploads.add(us);
        }
        // sort the table
        resort();
        // get the row of the added project
        int row = getRowOf(us);
        if (row >= 0) {
            repaintRowInserted(row);
        }
    }

    public void updateRow(UploadSummary us) {
        int startRow = getRowOf(us);
        // sort the table
        resort();
        int newRow = getRowOf(us);
        // should we repaint (and lose selections)?
        if (startRow != newRow) {
            repaintRowDeleted(startRow);
            repaintRowInserted(newRow);
        } else {
            repaintRow(startRow);
        }
    }

    public void removeRow(UploadSummary us) {
        int row = getRowOf(us);
        synchronized (filteredUploads) {
            filteredUploads.remove(us);
        }
        if (row >= 0) {
            repaintRowDeleted(row);
        }
    }

    public boolean contains(UploadSummary us) {
        synchronized (filteredUploads) {
            return filteredUploads.contains(us);
        }
    }

    public int getColumnCount() {
        return headers.length;
    }

    public int getRowCount() {
        synchronized (filteredUploads) {
            return filteredUploads.size();
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

    public UploadSummary getRow(int filteredIndex) {
        try {
            synchronized (filteredUploads) {
                return filteredUploads.get(filteredIndex);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public int getRowOf(UploadSummary us) {
        synchronized (filteredUploads) {
            return filteredUploads.indexOf(us);
        }
    }

    public Object getValueAt(int row, int column) {
        // sometimes null pointer exceptions occur
        try {
            UploadSummary us = getRow(row);
            switch (column) {
                case 0:
                    return us.getAddFileTool().getTitle();
                case 1:
                    if (us.getAddFileTool().getSize() != UploadSummary.DEFAULT_INT_VALUE) {
                        return TextUtil.formatBytes(us.getAddFileTool().getSize());
                    } else {
                        return "";
                    }
                case 2:
                    if (us.getAddFileTool().getFileCount() != UploadSummary.DEFAULT_INT_VALUE) {
                        return GUIUtil.integerFormat.format(us.getAddFileTool().getFileCount());
                    } else {
                        return "";
                    }
                case 3:
                    return us.getStatus();
                case 4:
                    if (us.getTimeStarted() != null) {
                        return new Date(us.getTimeStarted()).toGMTString().substring(0, 11);
                    } else {
                        return "";
                    }
                case 5:
                    return us.getProgressBar();
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public boolean filter(UploadSummary us) {
        try {
            return (filter.matcher(us.getAddFileTool().getTitle().toLowerCase()).find() || filter.matcher(us.getAddFileTool().getDescription().toLowerCase()).find());
        } catch (Exception e) {
            return false;
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
                    synchronized (filteredUploads) {
                        filteredUploads.clear();
                    }
                    for (UploadSummary us : UploadPool.getAll()) {
                        if (filter(us)) {
                            synchronized (filteredUploads) {
                                filteredUploads.add(us);
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
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public void sort(int column) {
        if (column >= headers.length || column < 0) {
            return;
        }
        table.setPressedColumn(column);
        synchronized (filteredUploads) {
            Collections.sort(filteredUploads, new USComparator(column));
        }
    }

    public void resort() {
        sort(table.getPressedColumn());
    }

    private class USComparator implements Comparator {

        private int column;

        public USComparator(int column) {
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
            } else if (o1 instanceof UploadSummary && o2 instanceof UploadSummary) {

                UploadSummary us1 = (UploadSummary) o1;
                UploadSummary us2 = (UploadSummary) o2;

                if (column == table.getColumnModel().getColumnIndex(COLUMN_TITLE)) {
                    return us1.getAddFileTool().getTitle().compareTo(us2.getAddFileTool().getTitle());
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_SIZE)) {
                    if (us1.getAddFileTool().getSize() == us2.getAddFileTool().getSize()) {
                        return 0;
                    } else if (us1.getAddFileTool().getSize() > us2.getAddFileTool().getSize()) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_FILES)) {
                    if (us1.getAddFileTool().getFileCount() == us2.getAddFileTool().getFileCount()) {
                        return 0;
                    } else if (us1.getAddFileTool().getFileCount() > us2.getAddFileTool().getFileCount()) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_STATUS)) {
                    return us1.getStatus().compareTo(us2.getStatus());
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_DATE)) {
                    if (us1.getReport() != null && us2.getReport() != null) {
                        if (us1.getReport().getTimestampStart() == us2.getReport().getTimestampStart()) {
                            return 0;
                        } else if (us1.getReport().getTimestampStart() > us2.getReport().getTimestampStart()) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else {
                        if (us1.getReport() == null && us2.getReport() == null) {
                            return 0;
                        } else if (us1.getReport() != null) {
                            return 1;
                        } else {
                            return -1;
                        }
                    }
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_PROGRESS)) {
                    if (us1.getAddFileTool().getTimeEstimator() != null && us2.getAddFileTool().getTimeEstimator() != null) {
                        if (us1.getAddFileTool().getTimeEstimator().getPercentDone() == us2.getAddFileTool().getTimeEstimator().getPercentDone()) {
                            return 0;
                        } else if (us1.getAddFileTool().getTimeEstimator().getPercentDone() > us2.getAddFileTool().getTimeEstimator().getPercentDone()) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else {
                        if (us1.getAddFileTool().getTimeEstimator() == null && us2.getAddFileTool().getTimeEstimator() == null) {
                            return 0;
                        } else if (us1.getAddFileTool().getTimeEstimator() != null) {
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
