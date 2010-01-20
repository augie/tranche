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

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.tranche.gui.GenericTextField;
import org.tranche.gui.FilterHelpPanel;
import org.tranche.gui.GenericButton;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.gui.LazyLoadAllSlowStuffAfterGUIRenders;
import org.tranche.gui.LazyLoadable;
import org.tranche.gui.Styles;
import org.tranche.gui.util.GUIUtil;

/**
 * The main, middle panel of the GUI. Contains the tab panel containing the panels
 * for each operation (e.g., managing uploads, downloads, servers, data sets, etc.)
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class MainPanel extends JPanel implements LazyLoadable {

    public ProjectsPanel dataSetsPanel = new ProjectsPanel();
    public DownloadsPanel downloadsPanel = new DownloadsPanel();
    public UploadsPanel uploadsPanel = new UploadsPanel();
    public ServersPanel serversPanel = new ServersPanel();
    private JPanel currentPanel = dataSetsPanel;
    /**
     * The tabs code is highly dynamic since a module can be used as a tab. An
     * installed module can be installed in front of default tabs or behind.
     * Front> Installed tabs annotated to go at front
     * Middle> Default panels
     * Last> Installed tabs not annotated to go at front
     */
    // Collection of installed tabs (from modules)
    private Map<String, GenericTab> installedFrontTabs = new HashMap();
    private Map<String, GenericTab> installedBackTabs = new HashMap();
    // Relative index of default tabs depends installed tabs at front
    private static final int RELATIVE_INDEX_PROJECTS = 0, RELATIVE_INDEX_DOWNLOADS = 1, RELATIVE_INDEX_UPLOADS = 2, RELATIVE_INDEX_SERVERS = 3, RELATIVE_INDEX_USERS = 4;
    // If add any default tabs (i.e., not installed tabs from module), update
    private static final int NUM_DEFAULT_TABS = 4;
    final public JTabbedPane tabbedPane = new JTabbedPane();
    final private JTextField filterTextField = new GenericTextField();
    final private JButton filterButton = new GenericButton("   Filter Data Sets   ");
    final private JButton clearFilterButton = new GenericButton(" View All ");
    final private JButton filterHelpButton = new GenericButton(" ? ");

    public MainPanel() {
        // add the tabs
        installDefaultTabs();

        setBackground(Color.WHITE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // add the filter
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridBagLayout());
        {
            GridBagConstraints gbc2 = new GridBagConstraints();

            gbc2.fill = GridBagConstraints.HORIZONTAL;
            gbc2.insets = new Insets(0, 0, 0, 5);
            gbc2.weightx = 1;
            filterTextField.setBackground(Styles.COLOR_BACKGROUND);
            filterTextField.setBorder(Styles.BORDER_BLACK_1);
            filterPanel.add(filterTextField, gbc2);

            gbc2.fill = GridBagConstraints.NONE;
            gbc2.weightx = 0;
            filterButton.setFont(Styles.FONT_11PT_BOLD);
            filterButton.setBorder(Styles.BORDER_BLACK_1);
            filterPanel.add(filterButton, gbc2);

            clearFilterButton.setFont(Styles.FONT_11PT_BOLD);
            clearFilterButton.setBorder(Styles.BORDER_BLACK_1);
            determineClearFilterButtonIsEnabled();
            filterPanel.add(clearFilterButton, gbc2);

            gbc2.insets = new Insets(0, 0, 0, 0);
            filterHelpButton.setFont(Styles.FONT_11PT);
            filterHelpButton.setBorder(Styles.BORDER_BLACK_1);
            filterPanel.add(filterHelpButton, gbc2);
        }
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1;
        filterPanel.setBackground(getBackground());
        add(filterPanel, gbc);

        // add the tabbed pane
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weighty = 1;
        add(tabbedPane, gbc);

        // need to listen for these changes as soon as the gui becomes visible
        tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                Thread t = new Thread("Main Panel Tab Changed") {

                    @Override
                    public void run() {
                        tabChanged();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        tabChanged();

        // lazy load!
        LazyLoadAllSlowStuffAfterGUIRenders.add(this);
    }

    private void tabChanged() {
        // depending on which of the tabs is selected, set the table panel
        if (tabbedPane.getSelectedIndex() == getProjectsTabIndex()) {
            filterTextField.setText(dataSetsPanel.getFilter());
            filterButton.setText("   Filter Data Sets   ");
            GUIUtil.getAdvancedGUI().leftPanel.setContextMenu(dataSetsPanel.menu);
        } else if (tabbedPane.getSelectedIndex() == getDownloadsTabIndex()) {
            filterTextField.setText(downloadsPanel.getFilter());
            filterButton.setText("   Filter Downloads   ");
            GUIUtil.getAdvancedGUI().leftPanel.setContextMenu(downloadsPanel.menu);
        } else if (tabbedPane.getSelectedIndex() == getUploadsTabIndex()) {
            filterTextField.setText(uploadsPanel.getFilter());
            filterButton.setText("   Filter Uploads     ");
            GUIUtil.getAdvancedGUI().leftPanel.setContextMenu(uploadsPanel.menu);
        } else if (tabbedPane.getSelectedIndex() == getServersTabIndex()) {
            filterTextField.setText(serversPanel.getFilter());
            filterButton.setText("   Filter Servers     ");
            GUIUtil.getAdvancedGUI().leftPanel.setContextMenu(serversPanel.menu);
        }
    }

    final public synchronized void lazyLoad() {
        filterHelpButton.setToolTipText("Help with using filter");
        filterHelpButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        GenericPopupFrame popup = new GenericPopupFrame("Using the Filter", new FilterHelpPanel());
                        popup.setSizeAndCenter(FilterHelpPanel.RECOMMENDED_DIMENSION);
                        popup.setVisible(true);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });

        // non-immediately visible styles on the filter
        filterTextField.setFont(Styles.FONT_12PT);
        filterTextField.setToolTipText("The regular expression of what you want to look at.");
        filterTextField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        setFilter(filterTextField.getText());
                        determineClearFilterButtonIsEnabled();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });

        // non-immediately visible config for filter button
        filterButton.setToolTipText("The regular expression of what you want to look at.");
        filterButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        setFilter(filterTextField.getText());
                        determineClearFilterButtonIsEnabled();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });

        clearFilterButton.setToolTipText("Remove the filter on the projects/files shown.");
        clearFilterButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        setFilter("");
                        filterTextField.setText("");
                        filterTextField.setCaretPosition(0);
                        determineClearFilterButtonIsEnabled();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
    }

    /**
     * Helper method to remove all tabs from the tabbed pane
     */
    private void removeTabsBeforeReinstall() {
        tabbedPane.removeAll();
    }

    /**
     * Helper method to add front installed tabs.
     */
    private void installFrontTabs() {
        try {
            int index = 0;
            GenericTab nextTabPanel;
            for (String label : installedFrontTabs.keySet()) {

                nextTabPanel = installedFrontTabs.get(label);

                // Set the new tab index
                nextTabPanel.setTabIndex(index);

                // Add the tab
                tabbedPane.add(label, (JPanel) nextTabPanel);

                try {
                    // Default icon for all installed modules
                    tabbedPane.setIconAt(index, new ImageIcon(ImageIO.read(MainPanel.class.getResourceAsStream("/org/tranche/gui/image/installed.gif"))));
                } catch (IOException ex) {
                    System.out.println("Could not add icon for installed item: " + ex.getMessage());
                }

                index++;
            }
        } catch (Exception e) {
        }
    }

    /**
     * Helper method to add back installed tabs
     */
    private void installBackTabs() {
        try {
            int index = installedFrontTabs.size() + NUM_DEFAULT_TABS;
            GenericTab nextTabPanel;
            for (String label : installedBackTabs.keySet()) {

                nextTabPanel = installedBackTabs.get(label);

                // Set the new tab index
                nextTabPanel.setTabIndex(index);

                // Add the tab
                tabbedPane.add(label, (JPanel) nextTabPanel);

                try {
                    // Default icon for all installed modules
                    tabbedPane.setIconAt(index, new ImageIcon(ImageIO.read(MainPanel.class.getResourceAsStream("/org/tranche/gui/image/installed.gif"))));
                } catch (IOException ex) {
                    System.out.println("Could not add icon for installed item: " + ex.getMessage());
                }

                index++;
            }
        } catch (Exception e) {
        }
    }

    /**
     * Since the default tabs may be behind installed tabs, method will check.
     */
    private void installDefaultTabs() {
        try {
            tabbedPane.add("Data Sets", dataSetsPanel);
            tabbedPane.add("Downloads", downloadsPanel);
            tabbedPane.add("Uploads", uploadsPanel);
            tabbedPane.add("Servers", serversPanel);

            // set the colors of the right two tabs
            tabbedPane.setBackgroundAt(getServersTabIndex(), Color.LIGHT_GRAY);
            tabbedPane.setForegroundAt(getServersTabIndex(), Color.DARK_GRAY);

            // set the look and behavior of the tabbed pane
            tabbedPane.setFocusable(false);

            // set the icons
            tabbedPane.setIconAt(getProjectsTabIndex(), new ImageIcon(ImageIO.read(MainPanel.class.getResourceAsStream("/org/tranche/gui/image/folder-22x22.gif"))));
            tabbedPane.setIconAt(getDownloadsTabIndex(), new ImageIcon(ImageIO.read(MainPanel.class.getResourceAsStream("/org/tranche/gui/image/downloads.gif"))));
            tabbedPane.setIconAt(getUploadsTabIndex(), new ImageIcon(ImageIO.read(MainPanel.class.getResourceAsStream("/org/tranche/gui/image/uploads.gif"))));
            tabbedPane.setIconAt(getServersTabIndex(), new ImageIcon(ImageIO.read(MainPanel.class.getResourceAsStream("/org/tranche/gui/image/network-server-22x22.gif"))));
        } catch (Exception e) {
            System.out.println("Exception installing default tabs: " + e.getMessage());
        }
    }

    /**
     * Used to install a tab from a tranche module.
     */
    public void installTab(final String label, final GenericTab tabPanel, final boolean isPlacedInFront) {
        try {
            // Put tab in proper collection
            if (isPlacedInFront) {
                MainPanel.this.installedFrontTabs.put(label, tabPanel);
            } else {
                MainPanel.this.installedBackTabs.put(label, tabPanel);
            }

            // Remove all the tabs from tab panel first for fresh install
            removeTabsBeforeReinstall();

            // Install the tabs in order (uses collections containing tabs)
            installFrontTabs();
            installDefaultTabs();
            installBackTabs();

            System.out.println("Added \"" + label + "\" tab at index " + tabPanel.getTabIndex() + ".");

            // Set the tab to newly-installed tab
            setTab(tabPanel.getTabPanel());
        } catch (Exception e) {
        }
    }

    /**
     * Uninstall all installed tabs.
     */
    public void uninstallTabs() {
        try {
            removeTabsBeforeReinstall();

            // Clear the collections of installed tabs first
            installedBackTabs.clear();
            installedFrontTabs.clear();

            // Only install the default tabs
            installDefaultTabs();

            // Set to projects tab
            setTab(dataSetsPanel);
        } catch (Exception e) {
        }
    }

    /**
     * Helper method. Returns all installed tabs.
     */
    public Set<GenericTab> getAllInstalledTabs() {
        Set<GenericTab> allInstalledTabs = new HashSet();
        allInstalledTabs.addAll(this.installedFrontTabs.values());
        allInstalledTabs.addAll(this.installedBackTabs.values());

        return allInstalledTabs;
    }

    /**
     * Set whether an installed tab is enabled or not.
     */
    public void setInstalledTabEnabled(final String label, final boolean isVisible) {
        Thread t = new Thread("Set tab enabled thread") {

            @Override
            public void run() {
                try {
                    Set<GenericTab> allInstalledTabs = getAllInstalledTabs();

                    for (GenericTab tab : allInstalledTabs) {
                        if (tab.getLabel().equals(label)) {
                            // May not be currently set, only if index is not -1
                            if (tab.getTabIndex() > -1) {
                                tabbedPane.setEnabledAt(tab.getTabIndex(), isVisible);
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                }
            }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        SwingUtilities.invokeLater(t);
    }

    public String getFilter() {
        return filterTextField.getText();
    }

    public boolean isFilterSet() {
        return !filterTextField.getText().trim().equals("");
    }

    /**
     * Enables/disabled clear filter button based on state.
     */
    private void determineClearFilterButtonIsEnabled() {
        // Only offer clear filter if one is set
        if (filterTextField.getText().trim().equals("")) {
            clearFilterButton.setEnabled(false);
        } else {
            clearFilterButton.setEnabled(true);
        }
    }

    public void setFilter(String filter) {
        if (currentPanel.equals(dataSetsPanel)) {
            dataSetsPanel.setFilter(filter);
        } else if (currentPanel.equals(downloadsPanel)) {
            downloadsPanel.setFilter(filter);
        } else if (currentPanel.equals(uploadsPanel)) {
            uploadsPanel.setFilter(filter);
        } else if (currentPanel.equals(serversPanel)) {
            serversPanel.setFilter(filter);
        }
    }

    public void setTab(final JPanel panel) {
        Thread t = new Thread("Set Main Panel Tab") {

            @Override
            public void run() {
                // set the new tab
                for (Component tab : tabbedPane.getComponents()) {
                    if (tab == panel) {
                        tabbedPane.setSelectedComponent(tab);
                        break;
                    }
                }
            }
        };
        t.setDaemon(true);
        SwingUtilities.invokeLater(t);
    }

    public int getProjectsTabIndex() {
        return this.installedFrontTabs.size() + RELATIVE_INDEX_PROJECTS;
    }

    public int getDownloadsTabIndex() {
        return this.installedFrontTabs.size() + RELATIVE_INDEX_DOWNLOADS;
    }

    public int getUploadsTabIndex() {
        return this.installedFrontTabs.size() + RELATIVE_INDEX_UPLOADS;
    }

    public int getServersTabIndex() {
        return this.installedFrontTabs.size() + RELATIVE_INDEX_SERVERS;
    }
}
