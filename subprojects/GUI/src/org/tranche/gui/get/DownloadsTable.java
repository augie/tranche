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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableCellRenderer;
import org.tranche.gui.GenericPopupListener;
import org.tranche.gui.GenericTable;
import org.tranche.gui.Styles;
import org.tranche.project.ProjectSummary;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class DownloadsTable extends GenericTable {

    private DownloadsTableModel model;
    private DownloadsPopupMenu menu = new DownloadsPopupMenu();

    public DownloadsTable() {
        super(new DownloadsTableModel(), ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        model = (DownloadsTableModel) getModel();
        model.setTable(this);

        getColumnModel().getColumn(1).setMaxWidth(70);
        getColumnModel().getColumn(1).setPreferredWidth(60);
        getColumnModel().getColumn(2).setMaxWidth(70);
        getColumnModel().getColumn(2).setPreferredWidth(60);
        getColumnModel().getColumn(3).setMaxWidth(80);
        getColumnModel().getColumn(3).setPreferredWidth(80);
        getColumnModel().getColumn(4).setMaxWidth(300);
        getColumnModel().getColumn(4).setPreferredWidth(250);
        // set the last column as a progress bar
        getColumnModel().getColumn(4).setCellRenderer(new TableCellRenderer() {

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return (Component) value;
            }
        });

        // add the popup menu listener
        addMouseListener(new GenericPopupListener(menu));
    }

    public DownloadsTableModel getDownloadsTableModel() {
        return model;
    }

    public String getFilter() {
        return model.getFilter();
    }

    public void setFilter(String filter) {
        model.setFilter(filter);
    }

    public Collection<DownloadSummary> getSelected() {
        Set<DownloadSummary> summaries = new HashSet<DownloadSummary>();
        int[] rows = getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) {
            summaries.add(model.getRow(rows[i]));
        }
        return Collections.unmodifiableCollection(summaries);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        String description = null;
        if (row >= 0) {
            ProjectSummary ps = model.getRow(row).getProjectSummary();
            if (ps != null) {
                description = ps.description;
                if (description != null) {
                    if (description.length() > 80) {
                        description = description.substring(0, 80) + "...";
                    } else if (description.equals("")) {
                        description = null;
                    }
                }
            }
        }
        return description == null ? "" : description;
    }

    private class DownloadsPopupMenu extends JPopupMenu {

        private JMenuItem copyHashMenuItem = new JMenuItem("Copy Hash");
        private JMenuItem showHashMenuItem = new JMenuItem("Show Hash");
        private JMenuItem pauseMenuItem = new JMenuItem("Pause");
        private JMenuItem resumeMenuItem = new JMenuItem("Resume");
        private JMenuItem stopMenuItem = new JMenuItem("Stop");
        private JMenuItem removeMenuItem = new JMenuItem("Remove");
        private JMenuItem monitorMenuItem = new JMenuItem("Monitor");
        private JMenuItem showErrorsMenuItem = new JMenuItem("Show Errors");

        public DownloadsPopupMenu() {
            super();
            setBorder(Styles.BORDER_BLACK_1);

            copyHashMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            model.getRow(getSelectedRow()).copyHash();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            add(copyHashMenuItem);

            showHashMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            model.getRow(getSelectedRow()).showHash();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            add(showHashMenuItem);

            pauseMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (int row : getSelectedRows()) {
                                model.getRow(row).pause();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            add(pauseMenuItem);

            resumeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (int row : getSelectedRows()) {
                                model.getRow(row).resume();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            add(resumeMenuItem);

            stopMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (int row : getSelectedRows()) {
                                model.getRow(row).stop();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            add(stopMenuItem);

            removeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Remove Selected Downloads") {

                        @Override
                        public void run() {
                            for (DownloadSummary ds : getSelected()) {
                                DownloadPool.remove(ds);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            add(removeMenuItem);

            monitorMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Monitor Download") {

                        @Override
                        public void run() {
                            for (DownloadSummary ds : getSelected()) {
                                ds.showMonitor(DownloadsTable.this);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            add(monitorMenuItem);

            showErrorsMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Show Errors") {

                        @Override
                        public void run() {
                            for (DownloadSummary ds : getSelected()) {
                                ds.showErrorFrame(DownloadsTable.this);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            add(showErrorsMenuItem);

            addPopupMenuListener(new PopupMenuListener() {

                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    int rows[] = getSelectedRows();
                    boolean failed = false, allFinished = true, anyDownloading = false;
                    for (int row : rows) {
                        DownloadSummary ds = model.getRow(row);
                        if (ds.getStatus().equals(DownloadSummary.STATUS_FAILED)) {
                            failed = true;
                        }
                        if (ds.isDownloading()) {
                            anyDownloading = true;
                        }
                        if (!ds.isFinished()) {
                            allFinished = false;
                        }
                    }
                    copyHashMenuItem.setEnabled(rows.length == 1);
                    showHashMenuItem.setEnabled(rows.length == 1);
                    pauseMenuItem.setEnabled(rows.length > 0 && !allFinished && anyDownloading);
                    resumeMenuItem.setEnabled(rows.length > 0 && !allFinished);
                    stopMenuItem.setEnabled(rows.length > 0 && !allFinished);
                    removeMenuItem.setEnabled(rows.length > 0 && allFinished);
                    monitorMenuItem.setEnabled(rows.length > 0);
                    showErrorsMenuItem.setEnabled(failed);
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
        }
    }
}
