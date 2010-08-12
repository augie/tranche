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

import org.tranche.gui.add.UploadSummary;
import org.tranche.gui.util.GUIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.Styles;
import org.tranche.gui.add.UploadPool;
import org.tranche.gui.add.UploadPoolEvent;
import org.tranche.gui.add.UploadPoolListener;
import org.tranche.gui.add.UploadsTable;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith <bryanesmith at gmail dot com>
 */
public class UploadsPanel extends JPanel {

    protected LeftMenu menu = new UploadsMenu();
    private UploadsTable table = new UploadsTable();

    public UploadsPanel() {
        setLayout(new BorderLayout());
        GenericScrollPane pane = new GenericScrollPane(table);
        pane.setBackground(Color.GRAY);
        pane.setBorder(Styles.BORDER_NONE);
        add(pane, BorderLayout.CENTER);

        // add the selection listener
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                Thread t = new Thread("Upload Panel Selection Changed") {

                    @Override
                    public void run() {
                        menu.updateSelection();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });

        UploadPool.addListener(new UploadPoolListener() {

            public void uploadAdded(UploadPoolEvent upe) {
                menu.updateSelection();
            }

            public void uploadUpdated(UploadPoolEvent upe) {
                menu.updateSelection();
            }

            public void uploadRemoved(UploadPoolEvent upe) {
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

    private class UploadsMenu extends LeftMenu {

        private JButton copyHashMenuItem = LeftMenu.createLeftMenuButton("Copy Hash");
        private JButton showHashMenuItem = LeftMenu.createLeftMenuButton("Show Hash");
        private JButton pauseMenuItem = LeftMenu.createLeftMenuButton("Pause");
        private JButton resumeMenuItem = LeftMenu.createLeftMenuButton("Resume");
        private JButton stopMenuItem = LeftMenu.createLeftMenuButton("Stop");
        private JButton removeMenuItem = LeftMenu.createLeftMenuButton("Remove");
        private JButton monitorMenuItem = LeftMenu.createLeftMenuButton("Monitor");
        private JButton showErrorsMenuItem = LeftMenu.createLeftMenuButton("Show Errors");
        private JButton retryMenuItem = LeftMenu.createLeftMenuButton("Retry");
        private JButton saveReceiptMenuItem = LeftMenu.createLeftMenuButton("Save Receipt");
        private JButton emailReceiptMenuItem = LeftMenu.createLeftMenuButton("Email Receipt");

        public UploadsMenu() {
            copyHashMenuItem.setToolTipText("Copy the Tranche Hash for the selected upload.");
            copyHashMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : table.getSelected()) {
                                us.copyHash();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(copyHashMenuItem);

            showHashMenuItem.setToolTipText("Show the Tranche Hash for the selected upload.");
            showHashMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : table.getSelected()) {
                                us.showHash();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(showHashMenuItem);

            pauseMenuItem.setToolTipText("Pauses the selected uploads.");
            pauseMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : table.getSelected()) {
                                us.pause();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(pauseMenuItem);

            resumeMenuItem.setToolTipText("Resumes the selected uploads.");
            resumeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : table.getSelected()) {
                                us.resume();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(resumeMenuItem);

            stopMenuItem.setToolTipText("Stops the selected uploads.");
            stopMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : table.getSelected()) {
                                us.stop();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(stopMenuItem);

            removeMenuItem.setToolTipText("Removes the selected uploads.");
            removeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : table.getSelected()) {
                                UploadPool.remove(us);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(removeMenuItem);

            monitorMenuItem.setToolTipText("Monitor selected upload.");
            monitorMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : table.getSelected()) {
                                us.showMonitor(GUIUtil.getAdvancedGUI());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(monitorMenuItem);

            showErrorsMenuItem.setToolTipText("Shows why the upload failed.");
            showErrorsMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Show Errors") {

                        @Override
                        public void run() {
                            for (UploadSummary us : table.getSelected()) {
                                us.showErrorFrame(GUIUtil.getAdvancedGUI());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(showErrorsMenuItem);

            retryMenuItem.setToolTipText("Starts a new instance of a failed upload.");
            retryMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Retry") {

                        @Override
                        public void run() {
                            for (UploadSummary us : table.getSelected()) {
                                try {
                                    if (us.isFinished() && us.getStatus().equals(UploadSummary.STATUS_FAILED)) {
                                        us.retry();
                                    }
                                } catch (Exception e) {
                                    new ErrorFrame().show(e, GUIUtil.getAdvancedGUI());
                                }
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(retryMenuItem);

            saveReceiptMenuItem.setToolTipText("Save a receipt for this upload.");
            saveReceiptMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : table.getSelected()) {
                                us.saveReceipt(GUIUtil.getAdvancedGUI());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(saveReceiptMenuItem);

            emailReceiptMenuItem.setToolTipText("Email a receipt for this upload.");
            emailReceiptMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            for (UploadSummary us : table.getSelected()) {
                                us.showEmailFrame(GUIUtil.getAdvancedGUI());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(emailReceiptMenuItem);

            pauseMenuItem.setEnabled(false);
            resumeMenuItem.setEnabled(false);
            stopMenuItem.setEnabled(false);
            removeMenuItem.setEnabled(false);
            showHashMenuItem.setEnabled(false);
            copyHashMenuItem.setEnabled(false);
            monitorMenuItem.setEnabled(false);
            showErrorsMenuItem.setEnabled(false);
            retryMenuItem.setEnabled(false);
            saveReceiptMenuItem.setEnabled(false);
            emailReceiptMenuItem.setEnabled(false);
        }

        @Override
        public void updateSelection() {
            // possible null pointer exceptions
            try {
                int rows[] = table.getSelectedRows();
                boolean failed = false, allFinished = true, anyUploading = false, anyPaused = false;
                if (table != null) {
                    for (int row : rows) {
                        // catch possible null pointer exceptions
                        try {
                            UploadSummary us = table.getUploadsTableModel().getRow(row);
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
                        } catch (Exception e) {
                        }
                    }
                }

                pauseMenuItem.setEnabled(rows.length > 0 && anyUploading);
                resumeMenuItem.setEnabled(rows.length > 0 && anyPaused);
                stopMenuItem.setEnabled(rows.length > 0 && !allFinished);
                removeMenuItem.setEnabled(rows.length > 0 && allFinished);
                monitorMenuItem.setEnabled(rows.length > 0);
                showHashMenuItem.setEnabled(rows.length == 1 && table.getUploadsTableModel().getRow(rows[0]).getReport().getHash() != null);
                copyHashMenuItem.setEnabled(showHashMenuItem.isEnabled());
                showErrorsMenuItem.setEnabled(failed);
                retryMenuItem.setEnabled(failed);
                saveReceiptMenuItem.setEnabled(showHashMenuItem.isEnabled());
                emailReceiptMenuItem.setEnabled(showHashMenuItem.isEnabled());
            } catch (Exception e) {
            }
        }
    }
}
