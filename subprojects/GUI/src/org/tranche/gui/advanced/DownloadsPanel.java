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
package org.tranche.gui.advanced;

import org.tranche.gui.get.DownloadSummary;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.Styles;
import org.tranche.gui.get.DownloadPool;
import org.tranche.gui.get.DownloadPoolEvent;
import org.tranche.gui.get.DownloadPoolListener;
import org.tranche.gui.get.DownloadsTable;
import org.tranche.gui.util.GUIUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith <bryanesmith at gmail dot com>
 */
public class DownloadsPanel extends JPanel {

    protected LeftMenu menu = new DownloadsMenu();
    private DownloadsTable table = new DownloadsTable();

    public DownloadsPanel() {
        // add the table
        setLayout(new BorderLayout());
        GenericScrollPane pane = new GenericScrollPane(table);
        pane.setBackground(Color.GRAY);
        pane.setBorder(Styles.BORDER_NONE);
        add(pane, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                Thread t = new Thread("Download Panel Selection Changed") {

                    @Override
                    public void run() {
                        menu.updateSelection();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });

        DownloadPool.addListener(new DownloadPoolListener() {

            public void downloadAdded(DownloadPoolEvent dpe) {
                menu.updateSelection();
            }

            public void downloadUpdated(DownloadPoolEvent dpe) {
                menu.updateSelection();
            }

            public void downloadRemoved(DownloadPoolEvent dpe) {
                menu.updateSelection();
            }
        });
    }

    public String getFilter() {
        return table.getFilter();
    }

    public void setFilter(String filter) {
        table.setFilter(filter);
    }

    private class DownloadsMenu extends LeftMenu {

        private JButton copyHashMenuItem = LeftMenu.createLeftMenuButton("Copy Hash");
        private JButton showHashMenuItem = LeftMenu.createLeftMenuButton("Show Hash");
        private JButton pauseMenuItem = LeftMenu.createLeftMenuButton("Pause");
        private JButton resumeMenuItem = LeftMenu.createLeftMenuButton("Resume");
        private JButton stopMenuItem = LeftMenu.createLeftMenuButton("Stop");
        private JButton removeMenuItem = LeftMenu.createLeftMenuButton("Remove");
        private JButton monitorMenuItem = LeftMenu.createLeftMenuButton("Monitor");
        private JButton showErrorsMenuItem = LeftMenu.createLeftMenuButton("Show Errors");

        public DownloadsMenu() {
            super();
            copyHashMenuItem.setToolTipText("Copies the Tranche Hash of the selected download.");
            copyHashMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.getDownloadsTableModel().getRow(table.getSelectedRow()).copyHash();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(copyHashMenuItem);

            showHashMenuItem.setToolTipText("Shows the Tranche Hash of the selected download.");
            showHashMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.getDownloadsTableModel().getRow(table.getSelectedRow()).showHash();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(showHashMenuItem);

            pauseMenuItem.setToolTipText("Pauses the selected downloads.");
            pauseMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Pause Selected Downloads") {

                        @Override
                        public void run() {
                            for (int row : table.getSelectedRows()) {
                                table.getDownloadsTableModel().getRow(row).pause();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(pauseMenuItem);

            resumeMenuItem.setToolTipText("Resumes the selected downloads.");
            resumeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Resume Selected Downloads") {

                        @Override
                        public void run() {
                            for (int row : table.getSelectedRows()) {
                                table.getDownloadsTableModel().getRow(row).resume();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(resumeMenuItem);

            stopMenuItem.setToolTipText("Stops and removes the selected downloads.");
            stopMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Remove Selected Downloads") {

                        @Override
                        public void run() {
                            for (int row : table.getSelectedRows()) {
                                table.getDownloadsTableModel().getRow(row).stop();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(stopMenuItem);

            removeMenuItem.setToolTipText("Remove all selected downloads.");
            removeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Remove Selected Downloads") {

                        @Override
                        public void run() {
                            for (DownloadSummary ds : table.getSelected()) {
                                DownloadPool.remove(ds);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(removeMenuItem);

            monitorMenuItem.setToolTipText("Monitor the selected download.");
            monitorMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Monitor Download") {

                        @Override
                        public void run() {
                            for (DownloadSummary ds : table.getSelected()) {
                                ds.showMonitor(GUIUtil.getAdvancedGUI());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(monitorMenuItem);

            showErrorsMenuItem.setToolTipText("Shows why the download failed.");
            showErrorsMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Show Errors") {

                        @Override
                        public void run() {
                            for (DownloadSummary ds : table.getSelected()) {
                                ds.showErrorFrame(GUIUtil.getAdvancedGUI());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(showErrorsMenuItem);

            copyHashMenuItem.setEnabled(false);
            showHashMenuItem.setEnabled(false);
            pauseMenuItem.setEnabled(false);
            resumeMenuItem.setEnabled(false);
            stopMenuItem.setEnabled(false);
            removeMenuItem.setEnabled(false);
            monitorMenuItem.setEnabled(false);
            showErrorsMenuItem.setEnabled(false);
        }

        @Override
        public void updateSelection() {
            int rows[] = table.getSelectedRows();
            boolean failed = false, allFinished = true, anyDownloading = false;
            if (table != null) {
                for (int row : rows) {
                    // catch any possible null pointer exceptions
                    try {
                        DownloadSummary ds = table.getDownloadsTableModel().getRow(row);
                        if (ds.getStatus().equals(DownloadSummary.STATUS_FAILED)) {
                            failed = true;
                        }
                        if (ds.isDownloading()) {
                            anyDownloading = true;
                        }
                        if (!ds.isFinished()) {
                            allFinished = false;
                        }
                    } catch (Exception e) {
                    }
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
    }
}
