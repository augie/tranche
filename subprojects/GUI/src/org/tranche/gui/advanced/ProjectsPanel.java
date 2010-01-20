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
import org.tranche.gui.*;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.tranche.gui.module.GUIMediatorUtil;
import org.tranche.gui.module.GUIPolicy;
import org.tranche.gui.module.SelectableModuleAction;
import org.tranche.gui.module.Installable;
import org.tranche.gui.project.ProjectPool;
import org.tranche.gui.project.ProjectPoolEvent;
import org.tranche.gui.project.ProjectPoolListener;
import org.tranche.gui.project.ProjectsTable;

/**
 * A panel for browsing all of the projects on the network.
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith <bryanesmith at gmail dot com>
 */
public class ProjectsPanel extends JPanel implements LazyLoadable, Installable {

    private ProjectsTable table = new ProjectsTable();
    protected LeftMenu menu = new ProjectMenu();

    public ProjectsPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // add the table in a scroll pane
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.weighty = 1;
        GenericScrollPane pane = new GenericScrollPane(table);
        pane.setBackground(Color.GRAY);
        pane.setBorder(Styles.BORDER_NONE);
        add(pane, gbc);

        // lazy load this thread
        LazyLoadAllSlowStuffAfterGUIRenders.add(this);
    }

    public void lazyLoad() {
        // add the selection listener for changing the abilities in the left menu
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        Thread t = new Thread("Project Table Selection Changed") {

                            @Override
                            public void run() {
                                menu.updateSelection();
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                    }
                };
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

    /**
     * Called by GUiMediatorUtil to install an action from a module.
     * @param action
     */
    public void installAction(SelectableModuleAction action) {
        GUIPolicy policy = action.getGUIPolicy();
        if (policy.isForProjectLeftMenu()) {
            ((ProjectMenu) menu).installAction(action);
        }
        if (policy.isForProjectPopupMenu()) {
            table.getMenu().installAction(action);
        }
    }

    /**
     * Clears all the actions installed from modules.
     */
    public void clearActions() {
        ((ProjectMenu) menu).clearActions();
        table.getMenu().clearActions();
    }

    /**
     * Change the GUI elements representing a particular action based on status.
     */
    public void negotateActionStatus(SelectableModuleAction action, boolean isEnabled) {
        ((ProjectMenu) menu).changeActionStatus(action, isEnabled);
        table.getMenu().changeActionStatus(action, isEnabled);
    }

    private class ProjectMenu extends LeftMenu {

        private final Map<JButton, SelectableModuleAction> installedActions = new HashMap<JButton, SelectableModuleAction>();
        private JButton copyHashButton = LeftMenu.createLeftMenuButton("Copy Hash");
        private JButton showHashButton = LeftMenu.createLeftMenuButton("Show Hash");
        private JButton moreInfoButton = LeftMenu.createLeftMenuButton("More Info");
        private JButton openButton = LeftMenu.createLeftMenuButton("Open");
        private JButton downloadButton = LeftMenu.createLeftMenuButton("Download");
        private JButton refreshButton = LeftMenu.createLeftMenuButton("Refresh");

        public ProjectMenu() {
            ProjectPool.addListener(new ProjectPoolListener() {

                public void projectAdded(ProjectPoolEvent ppe) {
                    updateSelection();
                }

                public void projectUpdated(ProjectPoolEvent ppe) {
                    updateSelection();
                }

                public void projectRemoved(ProjectPoolEvent ppe) {
                    updateSelection();
                }
            });

            copyHashButton.setToolTipText("Copies the hash of the selected data set.");
            copyHashButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    table.copySelected();
                }
            });
            addButton(copyHashButton);

            showHashButton.setToolTipText("Shows you the hash of the selected data set.");
            showHashButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.showSelected();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(showHashButton);

            moreInfoButton.setToolTipText("Shows you the details of the selected data sets.");
            moreInfoButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.showSelectedInfo();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(moreInfoButton);

            openButton.setToolTipText("Shows you the files within the selected data sets.");
            openButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.openSelected();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(openButton);

            downloadButton.setToolTipText("Downloads the selected data sets.");
            downloadButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            table.downloadSelected();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(downloadButton);

            updateSelection();
        }

        @Override
        public void updateSelection() {
            int selectedHashesCount = table.getSelectedHashes().size();
            copyHashButton.setEnabled(selectedHashesCount == 1);
            showHashButton.setEnabled(selectedHashesCount > 0);
            moreInfoButton.setEnabled(selectedHashesCount > 0);
            openButton.setEnabled(selectedHashesCount > 0);
            downloadButton.setEnabled(selectedHashesCount > 0);

            // Handle the installed modules
            if (selectedHashesCount > 0 && installedActions.keySet().size() > 0) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        for (JComponent item : installedActions.keySet()) {
                            try {
                                GUIMediatorUtil.enableInstalledModule(ProjectsPanel.this, item, installedActions.get(item), table.getSelectedRowCount());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        }

        /**
         * Bind the action to the popup menu.
         * @param action
         */
        public void installAction(final SelectableModuleAction action) {
            JButton button = LeftMenu.createLeftMenuButton(action.label);
            button.setSelected(false);
            button.setEnabled(GUIMediatorUtil.isActionEnabledByDefault(action));
            button.setToolTipText(action.description);
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            try {
                                GUIMediatorUtil.executeAction(action, ProjectsPanel.this);
                            } catch (Exception ex) {
                                GUIUtil.getAdvancedGUI().ef.show(ex, GUIUtil.getAdvancedGUI());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addButton(button);
            installedActions.put(button, action);
        }

        /**
         *
         */
        public void clearActions() {
            for (JButton item : installedActions.keySet()) {
                remove(item);
            }
            installedActions.clear();
        }

        /**
         * Sets a component's visibility based on whether it is enabled.
         * @param action
         * @param isEnabled
         */
        public void changeActionStatus(SelectableModuleAction action, boolean isEnabled) {
            for (JButton component : installedActions.keySet()) {
                if (installedActions.get(component).equals(action)) {
                    component.setVisible(isEnabled);
                }
            }
        }
    }
}
