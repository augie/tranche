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

import org.tranche.gui.util.GUIUtil;
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
import org.tranche.gui.server.ServersTable;

/**
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ServersPanel extends JPanel {

    protected LeftMenu menu = new ServersMenu();
    private ServersTable table = new ServersTable(false);

    public ServersPanel() {
        // add the table
        setLayout(new BorderLayout());
        GenericScrollPane pane = new GenericScrollPane(table);
        pane.setBackground(Color.GRAY);
        pane.setBorder(Styles.BORDER_NONE);
        add(pane, BorderLayout.CENTER);

        // add the selection listener
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                Thread t = new Thread("Servers Panel Selection Changed") {

                    @Override
                    public void run() {
                        menu.updateSelection();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
    }

    public String getFilter() {
        return table.getModel().getFilter();
    }

    public void setFilter(String filter) {
        table.getModel().setFilter(filter);
    }

    private class ServersMenu extends LeftMenu {

        private JButton connectMenuItem = LeftMenu.createLeftMenuButton("Connect");
        private JButton pingMenuItem = LeftMenu.createLeftMenuButton("Ping");
        private JButton monitorMenuItem = LeftMenu.createLeftMenuButton("Monitor");
        private JButton registerServerMenuItem = LeftMenu.createLeftMenuButton("Register Server");
        private JButton shutDownMenuItem = LeftMenu.createLeftMenuButton("Shut Down");
        private JButton configureMenuItem = LeftMenu.createLeftMenuButton("Load Configuration");

        public ServersMenu() {
            // add the listeners
            connectMenuItem.setToolTipText("Connects to the selected servers.");
            connectMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.connectToSelected(GUIUtil.getAdvancedGUI());
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(connectMenuItem);

            pingMenuItem.setToolTipText("Determines download time from the selected servers.");
            pingMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.pingServers(table.getSelectedHosts().toArray(new String[0]));
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(pingMenuItem);

            monitorMenuItem.setToolTipText("Opens the Server Monitor and listens to your communication with this server.");
            monitorMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.monitorSelected(GUIUtil.getAdvancedGUI());
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(monitorMenuItem);

            configureMenuItem.setToolTipText("Opens the server configuration for the selected server.");
            configureMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            // launch the configuration window
                            GUIUtil.getAdvancedGUI().topPanel.localServerMenuItem.doClick();
                            // set the server host
                            try {
                                GUIUtil.getAdvancedGUI().topPanel.serverConfigurationFrame.getPanel().setServerHost(table.getModel().getHost(table.getSelectedRow()));
                            } catch (Exception e) {
                                // show errors relative to the open window
                                GUIUtil.getAdvancedGUI().ef.show(e, GUIUtil.getAdvancedGUI().topPanel.serverConfigurationFrame.getPanel());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(configureMenuItem);
            
            registerServerMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.registerServerOnSelected(GUIUtil.getAdvancedGUI());
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(registerServerMenuItem);
            
            shutDownMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.shutDownSelected(GUIUtil.getAdvancedGUI());
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(shutDownMenuItem);

            connectMenuItem.setEnabled(false);
            pingMenuItem.setEnabled(false);
            monitorMenuItem.setEnabled(false);
            configureMenuItem.setEnabled(false);
            registerServerMenuItem.setEnabled(false);
            shutDownMenuItem.setEnabled(false);
        }

        @Override
        public void updateSelection() {
            int rows[] = table.getSelectedRows();
            connectMenuItem.setEnabled(rows.length > 0);
            pingMenuItem.setEnabled(rows.length > 0);
            monitorMenuItem.setEnabled(rows.length > 0);
            configureMenuItem.setEnabled(rows.length == 1);
            registerServerMenuItem.setEnabled(rows.length > 0);
            shutDownMenuItem.setEnabled(rows.length > 0 && GUIUtil.getUser() != null);
        }
    }
}
