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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.tranche.gui.Styles;
import org.tranche.gui.module.GUIMediatorUtil;
import org.tranche.gui.module.SelectableModuleAction;
import org.tranche.gui.util.GUIUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ProjectsPopupMenu extends JPopupMenu {

    private final ProjectsTable table;
    private final Map<JMenuItem, SelectableModuleAction> installedActions = new HashMap<JMenuItem, SelectableModuleAction>();
    private JMenuItem copyHashMenuItem = new JMenuItem("Copy Hash");
    private JMenuItem showHashMenuItem = new JMenuItem("Show Hash");
    private JMenuItem moreInfoMenuItem = new JMenuItem("More Info");
    private JMenuItem openMenuItem = new JMenuItem("Open");
    private JMenuItem downloadMenuItem = new JMenuItem("Download");

    public ProjectsPopupMenu(ProjectsTable table) {
        this.table = table;
        setBorder(Styles.BORDER_BLACK_1);

        // add the item listeners
        copyHashMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        ProjectsPopupMenu.this.table.copySelected();
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
                        ProjectsPopupMenu.this.table.showSelected();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        add(showHashMenuItem);

        moreInfoMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        ProjectsPopupMenu.this.table.showSelectedInfo();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        add(moreInfoMenuItem);

        openMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        ProjectsPopupMenu.this.table.openSelected();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        add(openMenuItem);

        downloadMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        ProjectsPopupMenu.this.table.downloadSelected();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        add(downloadMenuItem);

        addPopupMenuListener(new PopupMenuListener() {

            public void popupMenuCanceled(PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        try {
                            int[] rows = ProjectsPopupMenu.this.table.getSelectedRows();
                            downloadMenuItem.setEnabled(rows.length > 0);
                            moreInfoMenuItem.setEnabled(rows.length > 0);
                            openMenuItem.setEnabled(rows.length > 0);
                            copyHashMenuItem.setEnabled(rows.length == 1);
                            showHashMenuItem.setEnabled(rows.length > 0);

                            // Handle the installed modules
                            for (JMenuItem item : installedActions.keySet()) {
                                GUIMediatorUtil.enableInstalledModule(ProjectsPopupMenu.this, item, installedActions.get(item), ProjectsPopupMenu.this.table.getSelectedRowCount());
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }
        });
    }

    /**
     * Bind the action to the popup menu.
     * @param action
     */
    public void installAction(final SelectableModuleAction action) {
        JMenuItem menuItem = new JMenuItem(action.label);
        menuItem.setSelected(false);
        menuItem.setToolTipText(action.description);
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        try {
                            GUIMediatorUtil.executeAction(action, ProjectsPopupMenu.this);
                        } catch (Exception ex) {
                            GUIUtil.getAdvancedGUI().ef.show(ex, GUIUtil.getAdvancedGUI());
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        add(menuItem);
        installedActions.put(menuItem, action);
    }

    /**
     * 
     */
    public void clearActions() {
        for (JMenuItem item : installedActions.keySet()) {
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
        for (JMenuItem component : installedActions.keySet()) {
            if (installedActions.get(component).equals(action)) {
                component.setVisible(isEnabled);
            }
        }
    }
}
