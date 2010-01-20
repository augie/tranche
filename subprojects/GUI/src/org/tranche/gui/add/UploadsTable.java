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
import org.tranche.gui.util.GUIUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class UploadsTable extends GenericTable {

    private UploadsTableModel model;
    private UploadsPopupMenu popupMenu = new UploadsPopupMenu();

    public UploadsTable() {
        super(new UploadsTableModel(), ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        model = (UploadsTableModel) getModel();
        model.setTable(this);

        getColumnModel().getColumn(1).setMaxWidth(70);
        getColumnModel().getColumn(1).setPreferredWidth(60);
        getColumnModel().getColumn(2).setMaxWidth(70);
        getColumnModel().getColumn(2).setPreferredWidth(60);
        getColumnModel().getColumn(3).setMaxWidth(80);
        getColumnModel().getColumn(3).setPreferredWidth(80);
        getColumnModel().getColumn(4).setMaxWidth(100);
        getColumnModel().getColumn(4).setPreferredWidth(80);
        getColumnModel().getColumn(5).setMaxWidth(300);
        getColumnModel().getColumn(5).setPreferredWidth(250);
        // set the last column as a progress bar
        getColumnModel().getColumn(5).setCellRenderer(new TableCellRenderer() {

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return (AddFileToolProgressBar) value;
            }
        });

        // create the popup menu
        addMouseListener(new GenericPopupListener(popupMenu));
    }

    public UploadsTableModel getUploadsTableModel() {
        return model;
    }

    public String getFilter() {
        return model.getFilter();
    }

    public void setFilter(String filter) {
        model.setFilter(filter);
    }

    public Collection<UploadSummary> getSelected() {
        Set<UploadSummary> summaries = new HashSet<UploadSummary>();
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
            if (model.getRow(row).getAddFileTool().getDescription() != null) {
                description = model.getRow(row).getAddFileTool().getDescription().trim();
            }
            if (description != null) {
                if (description.length() > 80) {
                    description = description.substring(0, 80) + "...";
                } else if (description.equals("")) {
                    description = null;
                }
            }
        }
        return description;
    }

    private class UploadsPopupMenu extends JPopupMenu {

        private JMenuItem copyHashMenuItem = new JMenuItem("Copy Hash");
        private JMenuItem showHashMenuItem = new JMenuItem("Show Hash");
        private JMenuItem pauseMenuItem = new JMenuItem("Pause");
        private JMenuItem resumeMenuItem = new JMenuItem("Resume");
        private JMenuItem stopMenuItem = new JMenuItem("Stop");
        private JMenuItem removeMenuItem = new JMenuItem("Remove");
        private JMenuItem showErrorsMenuItem = new JMenuItem("Show Errors");
        private JMenuItem monitorMenuItem = new JMenuItem("Monitor");
        private JMenuItem emailReceiptMenuItem = new JMenuItem("Email Receipt");

        public UploadsPopupMenu() {
            super();
            setBorder(Styles.BORDER_BLACK_1);

            pauseMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : getSelected()) {
                                us.pause();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            resumeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : getSelected()) {
                                us.resume();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            stopMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : getSelected()) {
                                us.stop();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            removeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : getSelected()) {
                                UploadPool.remove(us);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            copyHashMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : getSelected()) {
                                us.copyHash();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            showHashMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : getSelected()) {
                                us.showHash();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            showErrorsMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Show Errors") {

                        @Override
                        public void run() {
                            for (UploadSummary us : getSelected()) {
                                us.showErrorFrame(GUIUtil.getAdvancedGUI());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            monitorMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : getSelected()) {
                                us.showMonitor(GUIUtil.getAdvancedGUI());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            emailReceiptMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : getSelected()) {
                                us.showEmailFrame(GUIUtil.getAdvancedGUI());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });

            addPopupMenuListener(new PopupMenuListener() {

                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    // possible null pointer exception
                    try {
                        int rows[] = getSelectedRows();
                        boolean failed = false, allFinished = true, anyUploading = false, anyPaused = false;
                        for (int row : rows) {
                            UploadSummary us = model.getRow(row);
                            if (us.getStatus().equals(UploadSummary.STATUS_FAILED)) {
                                failed = true;
                            }
                            if (us.isUploading()) {
                                anyUploading = true;
                            }
                            if (us.isPaused()) {
                                anyPaused = true;
                            }
                            if (!us.isFinished()) {
                                allFinished = false;
                            }
                        }

                        pauseMenuItem.setEnabled(rows.length > 0 && anyUploading);
                        resumeMenuItem.setEnabled(rows.length > 0 && anyPaused);
                        stopMenuItem.setEnabled(rows.length > 0 && !allFinished);
                        removeMenuItem.setEnabled(rows.length > 0 && allFinished);
                        showHashMenuItem.setEnabled(rows.length == 1 && model.getRow(rows[0]).getReport().getHash() != null);
                        copyHashMenuItem.setEnabled(showHashMenuItem.isEnabled());
                        showErrorsMenuItem.setEnabled(failed);
                        monitorMenuItem.setEnabled(rows.length > 0);
                        emailReceiptMenuItem.setEnabled(showHashMenuItem.isEnabled());
                    } catch (Exception ee) {
                    }
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });

            add(copyHashMenuItem);
            add(showHashMenuItem);
            add(pauseMenuItem);
            add(resumeMenuItem);
            add(stopMenuItem);
            add(removeMenuItem);
            add(showErrorsMenuItem);
            add(monitorMenuItem);
            add(emailReceiptMenuItem);
        }
    }
}
