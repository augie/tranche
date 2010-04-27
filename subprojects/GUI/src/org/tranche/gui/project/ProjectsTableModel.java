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
package org.tranche.gui.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Pattern;
import org.tranche.commons.TextUtil;
import org.tranche.gui.SortableTableModel;
import org.tranche.gui.user.UserZipFileEvent;
import org.tranche.gui.user.UserZipFileListener;
import org.tranche.gui.util.GUIUtil;
import org.tranche.hash.BigHash;
import org.tranche.project.ProjectSummary;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith <bryanesmith at gmail dot com>
 */
public class ProjectsTableModel extends SortableTableModel {

    public static final String COLUMN_TITLE = "Title";
    public static final String COLUMN_ENCRYPTED = "";
    public static final String COLUMN_HIDDEN = "";
    public static final String COLUMN_SIZE = "Size";
    public static final String COLUMN_FILES = "Files";
    public static final String COLUMN_DATE = "Date";
    public static final String COLUMN_UPLOADER = "Uploader";
    private final String[] headers = new String[]{COLUMN_TITLE, COLUMN_ENCRYPTED, COLUMN_HIDDEN, COLUMN_SIZE, COLUMN_FILES, COLUMN_DATE, COLUMN_UPLOADER};
    private final ArrayList<ProjectSummary> filteredProjects = new ArrayList<ProjectSummary>();
    private Pattern filter = Pattern.compile("");
    private ProjectsTable table;

    public ProjectsTableModel(ProjectsTable table) {
        this.table = table;
        ProjectPool.addListener(new ProjectPoolListener() {

            public void projectAdded(ProjectPoolEvent ppe) {
                if (filter(ppe.getProjectSummary())) {
                    addRow(ppe.getProjectSummary());
                }
            }

            public void projectUpdated(ProjectPoolEvent ppe) {
                if (contains(ppe.getProjectSummary())) {
                    if (filter(ppe.getProjectSummary())) {
                        updateRow(ppe.getProjectSummary());
                    } else {
                        removeRow(ppe.getProjectSummary());
                    }
                } else if (filter(ppe.getProjectSummary())) {
                    addRow(ppe.getProjectSummary());
                }
            }

            public void projectRemoved(ProjectPoolEvent ppe) {
                removeRow(ppe.getProjectSummary());
            }
        });

        // listen for changes to the user
        GUIUtil.addUserZipFileListener(new UserZipFileListener() {

            public void userSignedIn(UserZipFileEvent uzfe) {
                refilter();
            }

            public void userSignedOut(UserZipFileEvent uzfe) {
                refilter();
            }
        });
    }

    public void setTable(ProjectsTable table) {
        this.table = table;
    }

    public void addRow(ProjectSummary ps) {
        // add the project's bighash
        synchronized (filteredProjects) {
            if (!filteredProjects.contains(ps)) {
                filteredProjects.add(ps);
            }
        }
        // sort the table
        resort();
        // get the row of the added project
        int row = getRowOf(ps);
        if (row >= 0) {
            repaintRowInserted(row);
        }
    }

    public void updateRow(ProjectSummary ps) {
        int startRow = getRowOf(ps);
        // sort the table
        resort();
        int newRow = getRowOf(ps);
        // should we repaint (and lose selections)?
        if (startRow != newRow) {
            repaintRowDeleted(startRow);
            repaintRowInserted(newRow);
        } else {
            repaintRow(startRow);
        }
    }

    public void removeRow(ProjectSummary ps) {
        int row = getRowOf(ps);
        synchronized (filteredProjects) {
            filteredProjects.remove(ps);
        }
        if (row >= 0) {
            repaintRowDeleted(row);
        }
    }

    public boolean contains(ProjectSummary ps) {
        synchronized (filteredProjects) {
            return filteredProjects.contains(ps);
        }
    }

    private boolean filter(ProjectSummary ps) {
        try {
            if ((ps.title != null && (filter.matcher(ps.title.toLowerCase()).find() || filter.matcher(ps.hash.toString()).find()))) {
                boolean signedIn = false;
                if (GUIUtil.getUser() != null) {
                    signedIn = ps.uploader.equals(GUIUtil.getUser().getUserNameFromCert());
                }
                return signedIn || !ps.isHidden;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String getFilter() {
        return filter.pattern();
    }

    public void refilter() {
        setFilter(getFilter());
    }

    public void setFilter(String filter) {
        this.filter = Pattern.compile(filter.toLowerCase());

        Thread t = new Thread() {

            @Override
            public void run() {
                // there might be concurrent modification exceptions
                try {
                    // filter
                    Collection<ProjectSummary> summaries = ProjectPool.getProjectSummaries();
                    synchronized (filteredProjects) {
                        filteredProjects.clear();
                        for (ProjectSummary pf : summaries) {
                            if (filter(pf)) {
                                filteredProjects.add(pf);
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

    public ProjectSummary getRow(int filteredIndex) {
        try {
            return filteredProjects.get(filteredIndex);
        } catch (Exception e) {
            return null;
        }
    }

    public int getRowOf(ProjectSummary pf) {
        return filteredProjects.indexOf(pf);
    }

    public int getRowCount() {
        return filteredProjects.size();
    }

    public String getDescription(int filteredIndex) {
        return getRow(filteredIndex).description;
    }

    public long getFiles(int filteredIndex) {
        return getRow(filteredIndex).files;
    }

    public BigHash getHash(int filteredIndex) {
        return getRow(filteredIndex).hash;
    }

    public String getUploader(int filteredIndex) {
        return getRow(filteredIndex).uploader;
    }

    public long getSize(int filteredIndex) {
        return getRow(filteredIndex).size;
    }

    public long getTimestamp(int filteredIndex) {
        return getRow(filteredIndex).size;
    }

    public String getTitle(int filteredIndex) {
        return getRow(filteredIndex).title;
    }

    public Object getValueAt(int row, int column) {
        ProjectSummary ps = getRow(row);
        if (ps == null) {
            return null;
        }
        switch (column) {
            case 0:
                if (ps.title != null) {
                    return ps.title;
                } else {
                    return "";
                }
            case 1:
                return ps.isEncrypted && !ps.isPublished;
            case 2:
                return ps.isHidden;
            case 3:
                if (ps.size > 0) {
                    return TextUtil.formatBytes(ps.size);
                } else {
                    return "";
                }
            case 4:
                if (ps.files > 0) {
                    return GUIUtil.integerFormat.format(ps.files);
                } else {
                    return "";
                }
            case 5:
                if (ps.uploadTimestamp > 0) {
                    return new Date(ps.uploadTimestamp).toGMTString().substring(0, 11);
                } else {
                    return "";
                }
            case 6:
                return ps.uploader;
            default:
                return null;
        }
    }

    public void sort(int column) {
        if (column >= headers.length || column < 0) {
            return;
        }
        table.setPressedColumn(column);
        synchronized (filteredProjects) {
            Collections.sort(filteredProjects, new PSComparator(column));
        }
    }

    public void resort() {
        sort(table.getPressedColumn());
    }

    private class PSComparator implements Comparator {

        private int column;

        public PSComparator(int column) {
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
            } else if (o1 instanceof ProjectSummary && o2 instanceof ProjectSummary) {
                ProjectSummary ps1 = (ProjectSummary) o1;
                ProjectSummary ps2 = (ProjectSummary) o2;
                if (column == table.getColumnModel().getColumnIndex(COLUMN_TITLE)) {
                    return ps1.title.toLowerCase().compareTo(ps2.title.toLowerCase());
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_SIZE)) {
                    if (ps1.size == ps2.size) {
                        return 0;
                    } else if (ps1.size > ps2.size) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_FILES)) {
                    if (ps1.files == ps2.files) {
                        return 0;
                    } else if (ps1.files > ps2.files) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_DATE)) {
                    if (ps1.uploadTimestamp == ps2.uploadTimestamp) {
                        return 0;
                    } else if (ps1.uploadTimestamp > ps2.uploadTimestamp) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (column == table.getColumnModel().getColumnIndex(COLUMN_UPLOADER)) {
                    return ps1.uploader.toLowerCase().compareTo(ps2.uploader.toLowerCase());
                } else if (column == 1) {
                    if (ps1.isEncrypted == ps2.isEncrypted) {
                        return 0;
                    } else if (ps1.isEncrypted == true && ps2.isEncrypted == false) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (column == 2) {
                    if (ps1.isHidden == ps2.isHidden) {
                        return 0;
                    } else if (ps1.isHidden == true && ps2.isHidden == false) {
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
