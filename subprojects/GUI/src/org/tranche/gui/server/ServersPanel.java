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

import org.tranche.gui.util.GUIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.tranche.gui.GenericMenu;
import org.tranche.gui.GenericMenuBar;
import org.tranche.gui.GenericMenuItem;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.PreferencesFrame;
import org.tranche.gui.Styles;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ServersPanel extends JPanel {

    private ServersMenuBar menuBar = new ServersMenuBar();
    private ServersTable table = new ServersTable();

    public ServersPanel() {
        this(null);
    }

    public ServersPanel(Collection<String> selectedHosts) {
        // selected servers
        table.getModel().setSelectedHosts(selectedHosts);

        setLayout(new BorderLayout());

        // add the menu
        add(menuBar, BorderLayout.NORTH);

        // add the table
        GenericScrollPane pane = new GenericScrollPane(table);
        pane.setBackground(Color.GRAY);
        pane.setBorder(Styles.BORDER_NONE);
        add(pane, BorderLayout.CENTER);
    }

    public Collection<String> getSelectedHosts() {
        return table.getModel().getSelectedHosts();
    }

    public class ServersMenuBar extends GenericMenuBar {

        private JMenu optionsMenu = new GenericMenu("Options");
        private JMenuItem preferencesMenuItem = new GenericMenuItem("Preferences");
        private JMenu selectionMenu = new GenericMenu("Selection");
        private JMenuItem selectAllMenuItem = new GenericMenuItem("Select All");
        private JMenuItem deselectAllMenuItem = new GenericMenuItem("Deselect All");
        private JMenuItem useMenuItem = new GenericMenuItem("Use");
        private JMenuItem useNoneMenuItem = new GenericMenuItem("Do Not Use");
        private JMenuItem pingMenuItem = new GenericMenuItem("Ping");
        private JMenuItem monitorMenuItem = new GenericMenuItem("Monitor");

        public ServersMenuBar() {
            super();
            preferencesMenuItem.setMnemonic('o');
            preferencesMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            GUIUtil.getPreferencesFrame().show(PreferencesFrame.SHOW_SERVERS);
                            GUIUtil.getPreferencesFrame().setLocationRelativeTo(GUIUtil.getAdvancedGUI());
                            GUIUtil.getPreferencesFrame().setVisible(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            optionsMenu.add(preferencesMenuItem);

            optionsMenu.setMnemonic('o');
            add(optionsMenu);

            selectAllMenuItem.setMnemonic('s');
            selectAllMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.selectAll();
                            table.validate();
                            table.repaint();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            selectionMenu.add(selectAllMenuItem);
            deselectAllMenuItem.setMnemonic('d');
            deselectAllMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.getSelectionModel().clearSelection();
                            table.validate();
                            table.repaint();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            selectionMenu.add(deselectAllMenuItem);

            JSeparator separator = new JSeparator();
            separator.setForeground(Color.BLACK);
            selectionMenu.add(separator);

            useMenuItem.setMnemonic('u');
            useMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.getModel().setSelectedHosts(table.getSelectedHosts());
                            table.validate();
                            table.repaint();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            selectionMenu.add(useMenuItem);
            useNoneMenuItem.setMnemonic('n');
            useNoneMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            Collection<String> selectedTable = table.getSelectedHosts();
                            Collection<String> using = table.getModel().getSelectedHosts();
                            Set<String> newUsing = new HashSet<String>(using);
                            for (String host : selectedTable) {
                                newUsing.remove(host);
                            }
                            table.getModel().setSelectedHosts(newUsing);
                            table.validate();
                            table.repaint();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            selectionMenu.add(useNoneMenuItem);
            pingMenuItem.setMnemonic('p');
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
            selectionMenu.add(pingMenuItem);
            monitorMenuItem.setMnemonic('m');
            monitorMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.monitorSelected(null);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            selectionMenu.add(monitorMenuItem);

            selectionMenu.setMnemonic('s');
            add(selectionMenu);
            selectionMenu.addMenuListener(new MenuListener() {

                public void menuSelected(MenuEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            Collection<String> hosts = table.getSelectedHosts();
                            useMenuItem.setEnabled(!hosts.isEmpty());
                            useNoneMenuItem.setEnabled(!hosts.isEmpty());
                            pingMenuItem.setEnabled(!hosts.isEmpty());
                            monitorMenuItem.setEnabled(!hosts.isEmpty());
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuCanceled(MenuEvent e) {
                }
            });
        }
    }
}
