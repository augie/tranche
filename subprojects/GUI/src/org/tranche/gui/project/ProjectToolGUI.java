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

import java.awt.BorderLayout;
import org.tranche.gui.server.ServersPanel;
import org.tranche.gui.*;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.project.panel.ProjectPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.tranche.exceptions.CouldNotFindMetaDataException;
import org.tranche.get.GetFileTool;
import org.tranche.get.GetFileToolUtil;
import org.tranche.gui.get.SelectUploaderFrame;
import org.tranche.gui.get.wizard.GetFileToolWizard;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.project.ProjectFile;
import org.tranche.project.ProjectFilePart;
import org.tranche.project.ProjectSummary;
import org.tranche.users.InvalidSignInException;
import org.tranche.users.UserZipFileUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ProjectToolGUI extends GenericFrame implements ClipboardOwner {

    private ProjectSummary summary;
    private ProjectToolGUIMenuBar projectMenuBar = new ProjectToolGUIMenuBar();
    private ProjectToolGUIPopupMenu projectPopupMenu = new ProjectToolGUIPopupMenu();
    private ProjectPanel projectPanel = new ProjectPanel();
    private ErrorFrame ef = new ErrorFrame();

    public ProjectToolGUI() {
        super("Data Set Browser");
        setSize(600, 500);
        setLayout(new BorderLayout());

        add(projectMenuBar, BorderLayout.NORTH);
        projectPanel.setPopupMenu(projectPopupMenu);
        add(projectPanel, BorderLayout.CENTER);
    }

    public void open(BigHash hash, Collection<String> hosts) throws CouldNotFindMetaDataException, Exception {
        if (!ProjectPool.contains(hash)) {
            ProjectPool.set(new ProjectSummary(hash));
        }
        open(ProjectPool.get(hash).get(0), hosts);
    }

    public void open(ProjectSummary summary, Collection<String> hosts) throws CouldNotFindMetaDataException, Exception {
        IndeterminateProgressBar progress = new IndeterminateProgressBar("Loading...");
        if (isVisible()) {
            progress.setLocationRelativeTo(this);
        } else {
            GUIUtil.centerOnScreen(this);
        }
        progress.setDisposeAllowable(false);
        progress.start();
        try {
            this.summary = summary;
            GetFileTool gft = new GetFileTool();
            gft.setHash(summary.hash);
            if (hosts != null) {
                gft.setServersToUse(hosts);
            }
            MetaData metaData = gft.getMetaData();
            // select the right uploader
            if (metaData.getUploaderCount() > 1 && !summary.isMetaDataParsed()) {
                SelectUploaderFrame frame = new SelectUploaderFrame();
                frame.setMetaData(summary.hash, metaData);
                frame.setLocationRelativeTo(ProjectToolGUI.this);
                frame.setVisible(true);
                frame.waitForSelection();
            }
            if (metaData.isEncrypted() && !metaData.isPublicPassphraseSet()) {
                if (!PassphrasePool.contains(summary.hash)) {
                    PassphraseFrame pf = new PassphraseFrame();
                    pf.setLocationRelativeTo(ProjectToolGUI.this);
                    pf.setVisible(true);
                    PassphrasePool.set(summary.hash, pf.getPassphrase());
                }
                gft.setPassphrase(PassphrasePool.get(summary.hash));
            }
            if (!summary.isMetaDataParsed()) {
                summary.parseMetaData(metaData, summary.hash);
            }
            gft.setUploaderName(summary.uploader);
            gft.setUploadTimestamp(summary.uploadTimestamp);
            ProjectFile projectFile = gft.getProjectFile();
            if (!summary.isProjectFileParsed()) {
                summary.parseProjectFile(projectFile);
            }
            projectPanel.setProject(summary.hash, projectFile, metaData);
        } finally {
            progress.stop();
        }
    }

    private void download(String regex) {
        try {
            GetFileToolWizard gftw = new GetFileToolWizard();
            if (summary.isEncrypted && PassphrasePool.contains(summary.hash)) {
                gftw.setPassphrase(PassphrasePool.get(summary.hash));
            }
            gftw.setHash(summary.hash);
            gftw.selectUploader(summary.uploader, summary.uploadTimestamp, null);
            if (regex != null) {
                gftw.setRegex(regex);
            }
            gftw.setDefaultCloseOperation(GetFileToolWizard.DISPOSE_ON_CLOSE);
            gftw.setLocationRelativeTo(ProjectToolGUI.this);
            gftw.setVisible(true);
        } catch (Exception e) {
            ef.show(e, ProjectToolGUI.this);
        }
    }

    private class OpenProjectFrame extends GenericFrame implements ActionListener {

        private final JTextField hashTextField = new GenericTextField();
        private final ServersPanel serversPanel = new ServersPanel();

        public OpenProjectFrame() {
            super("Open Data Set");

            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(500, 300);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            // create and add the hash label
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            gbc.weightx = 0;
            gbc.insets = new Insets(10, 5, 0, 5);
            JLabel hashLabel = new GenericLabel("Hash:");
            hashLabel.setToolTipText("The hash of the data set you want to view.");
            add(hashLabel, gbc);

            // create and add the hash text field
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.insets = new Insets(10, 0, 0, 5);
            hashTextField.setBorder(Styles.BORDER_BLACK_1);
            hashTextField.setToolTipText("The hash of the project you want to view.");
            hashTextField.addActionListener(this);
            add(hashTextField, gbc);

            // create and add the servers label
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            gbc.weightx = 0;
            gbc.insets = new Insets(10, 5, 0, 5);
            add(new GenericLabel("Servers:"), gbc);

            // add the servers panel
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 3;
            gbc.insets = new Insets(10, 0, 0, 5);
            serversPanel.setBorder(Styles.BORDER_BLACK_1);
            add(serversPanel, gbc);

            // create and add the open project button
            gbc.anchor = GridBagConstraints.SOUTH;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weighty = 0;
            gbc.insets = new Insets(10, 0, 0, 0);
            JButton openProjectButton = new GenericButton("Open");
            openProjectButton.setBorder(Styles.BORDER_BUTTON_TOP);
            openProjectButton.addActionListener(this);
            add(openProjectButton, gbc);
        }

        public void actionPerformed(ActionEvent e) {
            Thread t = new Thread() {

                @Override
                public void run() {
                    IndeterminateProgressBar progress = new IndeterminateProgressBar("Opening...");
                    progress.setAlwaysOnTop(false);
                    progress.setLocationRelativeTo(OpenProjectFrame.this);
                    progress.start();
                    try {
                        open(BigHash.createHashFromString(hashTextField.getText().trim()), serversPanel.getSelectedHosts());
                        dispose();
                    } catch (Exception ee) {
                        ef.show(ee, OpenProjectFrame.this);
                    } finally {
                        progress.stop();
                    }
                }
            };
            t.setDaemon(true);
            t.start();
        }
    }

    private class ProjectToolGUIPopupMenu extends JPopupMenu {

        private JMenuItem selectAllMenuItem = new JMenuItem("Select All");
        private JMenuItem deselectAllMenuItem = new JMenuItem("Deselect All");
        private JMenuItem downloadSelectionMenuItem = new JMenuItem("Download");
        private JMenuItem showHashSelectionMenuItem = new JMenuItem("Show Hash");
        private JMenuItem copyHashSelectionMenuItem = new JMenuItem("Copy Hash");
        private JMenu viewMenu = new JMenu("View");
        public GenericCheckBoxMenuItem tableMenuItem = new GenericCheckBoxMenuItem("Table");
        public GenericCheckBoxMenuItem treeMenuItem = new GenericCheckBoxMenuItem("Tree");
        public GenericCheckBoxMenuItem tilesMenuItem = new GenericCheckBoxMenuItem("Tiles");
        public GenericCheckBoxMenuItem listMenuItem = new GenericCheckBoxMenuItem("List");
        public GenericCheckBoxMenuItem iconsMenuItem = new GenericCheckBoxMenuItem("Icons");

        public void uncheckViewItems() {
            tableMenuItem.setSelected(false);
            treeMenuItem.setSelected(false);
            tilesMenuItem.setSelected(false);
            listMenuItem.setSelected(false);
            iconsMenuItem.setSelected(false);
        }

        public ProjectToolGUIPopupMenu() {
            selectAllMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            projectPanel.selectAll();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            deselectAllMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            projectPanel.deselectAll();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            downloadSelectionMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            if (projectPanel.isProjectLoaded()) {
                                download(GetFileToolUtil.getRegex(projectPanel.getSelectedParts()));
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            showHashSelectionMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            try {
                                ProjectFilePart part = (ProjectFilePart) projectPanel.getSelectedParts().toArray()[0];
                                GUIUtil.showHash(part.getHash(), (ClipboardOwner) ProjectToolGUI.this, ProjectToolGUI.this);
                            } catch (Exception e) {
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            copyHashSelectionMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            try {
                                ProjectFilePart part = (ProjectFilePart) projectPanel.getSelectedParts().toArray()[0];
                                GUIUtil.copyToClipboard(part.getHash().toString(), (ClipboardOwner) ProjectToolGUI.this);
                            } catch (Exception e) {
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addPopupMenuListener(new PopupMenuListener() {

                public void popupMenuCanceled(PopupMenuEvent e) {
                }

                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    boolean isLoaded = projectPanel.isProjectLoaded(),
                            isSelectionMade = false,
                            isSingleSelection = false;
                    if (isLoaded) {
                        isSelectionMade = (projectPanel.getSelectedParts().size() > 0);
                        isSingleSelection = projectPanel.getSelectedParts().size() == 1;
                    }
                    selectAllMenuItem.setEnabled(isLoaded);
                    deselectAllMenuItem.setEnabled(isLoaded);
                    downloadSelectionMenuItem.setEnabled(isLoaded && isSelectionMade);
                    showHashSelectionMenuItem.setEnabled(isLoaded && isSelectionMade && isSingleSelection);
                    copyHashSelectionMenuItem.setEnabled(showHashSelectionMenuItem.isEnabled());
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }
            });
            tableMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            uncheckViewItems();
                            projectPanel.setView(ProjectPanel.VIEW_TABLE);
                            tableMenuItem.setSelected(true);
                            ProjectToolGUI.this.projectMenuBar.uncheckViewItems();
                            ProjectToolGUI.this.projectMenuBar.tableMenuItem.setSelected(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            treeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            uncheckViewItems();
                            projectPanel.setView(ProjectPanel.VIEW_TREE);
                            treeMenuItem.setSelected(true);
                            ProjectToolGUI.this.projectMenuBar.uncheckViewItems();
                            ProjectToolGUI.this.projectMenuBar.treeMenuItem.setSelected(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            tilesMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            uncheckViewItems();
                            projectPanel.setView(ProjectPanel.VIEW_TILES);
                            tilesMenuItem.setSelected(true);
                            ProjectToolGUI.this.projectMenuBar.uncheckViewItems();
                            ProjectToolGUI.this.projectMenuBar.tilesMenuItem.setSelected(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            listMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            uncheckViewItems();
                            projectPanel.setView(ProjectPanel.VIEW_LIST);
                            listMenuItem.setSelected(true);
                            ProjectToolGUI.this.projectMenuBar.uncheckViewItems();
                            ProjectToolGUI.this.projectMenuBar.listMenuItem.setSelected(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            iconsMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            uncheckViewItems();
                            projectPanel.setView(ProjectPanel.VIEW_ICONS);
                            iconsMenuItem.setSelected(true);
                            ProjectToolGUI.this.projectMenuBar.uncheckViewItems();
                            ProjectToolGUI.this.projectMenuBar.iconsMenuItem.setSelected(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });

            setBorder(Styles.BORDER_BLACK_1);

            add(selectAllMenuItem);
            add(deselectAllMenuItem);
            add(downloadSelectionMenuItem);
            add(showHashSelectionMenuItem);
            add(copyHashSelectionMenuItem);

            JSeparator separator = new JSeparator();
            separator.setForeground(Color.BLACK);
            add(separator);

            // add the view menu items
            {
                listMenuItem.setSelected(true);
                viewMenu.add(tableMenuItem);
                //viewMenu.add(treeMenuItem);
                //viewMenu.add(tilesMenuItem);
                viewMenu.add(listMenuItem);
            //viewMenu.add(iconsMenuItem);
            }
            add(viewMenu);
        }
    }

    private class ProjectToolGUIMenuBar extends GenericMenuBar {

        // home button - opens advanced gui
        private HomeButton homeButton = new HomeButton(ef, ProjectToolGUI.this);
        // search field and button
        private JTextField searchTextField = new GenericTextField();
        private JButton searchButton = new JButton("");
        // create the menus
        private GenericMenu dataSetMenu = new GenericMenu("Data Set");
        private GenericMenu selectionMenu = new GenericMenu("Selection");
        private GenericMenu viewMenu = new GenericMenu("View");
        // create the menu items
        private GenericMenuItem openMenuItem = new GenericMenuItem("Open");
        private GenericMenuItem closeMenuItem = new GenericMenuItem("Close");
        private GenericMenuItem moreInfoMenuItem = new GenericMenuItem("More Info");
        private GenericMenuItem downloadAllMenuItem = new GenericMenuItem("Download");
        private GenericMenuItem showHashMenuItem = new GenericMenuItem("Show Hash");
        private GenericMenuItem copyHashMenuItem = new GenericMenuItem("Copy Hash");
        private GenericMenuItem selectAllMenuItem = new GenericMenuItem("Select All");
        private GenericMenuItem deselectAllMenuItem = new GenericMenuItem("Deselect All");
        private GenericMenuItem downloadSelectionMenuItem = new GenericMenuItem("Download");
        private GenericMenuItem showHashSelectionMenuItem = new GenericMenuItem("Show Hash");
        private GenericMenuItem copyHashSelectionMenuItem = new GenericMenuItem("Copy Hash");
        private GenericCheckBoxMenuItem tableMenuItem = new GenericCheckBoxMenuItem("Table");
        private GenericCheckBoxMenuItem treeMenuItem = new GenericCheckBoxMenuItem("Tree");
        private GenericCheckBoxMenuItem tilesMenuItem = new GenericCheckBoxMenuItem("Tiles");
        private GenericCheckBoxMenuItem listMenuItem = new GenericCheckBoxMenuItem("List");
        private GenericCheckBoxMenuItem iconsMenuItem = new GenericCheckBoxMenuItem("Icons");

        private void uncheckViewItems() {
            tableMenuItem.setSelected(false);
            treeMenuItem.setSelected(false);
            tilesMenuItem.setSelected(false);
            listMenuItem.setSelected(false);
            iconsMenuItem.setSelected(false);
        }

        public ProjectToolGUIMenuBar() {
            homeButton.setVisible(GUIUtil.getAdvancedGUI() == null);
            homeButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            add(homeButton);

            // add the listeners
            openMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            OpenProjectFrame opf = new OpenProjectFrame();
                            opf.setLocationRelativeTo(ProjectToolGUI.this);
                            opf.setVisible(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            closeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            projectPanel.clear();
                            summary = null;
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            selectAllMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            projectPanel.selectAll();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            deselectAllMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            projectPanel.deselectAll();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            moreInfoMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            GUIUtil.showMoreInfo(summary, ProjectToolGUI.this);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            downloadAllMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            if (projectPanel.isProjectLoaded()) {
                                download(null);
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
                            if (summary != null) {
                                GUIUtil.showHash(summary.hash, (ClipboardOwner) ProjectToolGUI.this, ProjectToolGUI.this);
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
                            if (summary != null) {
                                GUIUtil.copyToClipboard(summary.hash.toString(), (ClipboardOwner) ProjectToolGUI.this);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            dataSetMenu.addMenuListener(new MenuListener() {

                public void menuSelected(MenuEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            boolean isLoaded = projectPanel.isProjectLoaded();
                            openMenuItem.setEnabled(!isLoaded);
                            closeMenuItem.setEnabled(isLoaded);
                            moreInfoMenuItem.setEnabled(isLoaded);
                            downloadAllMenuItem.setEnabled(isLoaded);
                            showHashMenuItem.setEnabled(isLoaded);
                            copyHashMenuItem.setEnabled(isLoaded);
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

            downloadSelectionMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            if (projectPanel.isProjectLoaded()) {
                                download(GetFileToolUtil.getRegex(projectPanel.getSelectedParts()));
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            showHashSelectionMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            try {
                                ProjectFilePart part = (ProjectFilePart) projectPanel.getSelectedParts().toArray()[0];
                                GUIUtil.showHash(part.getHash(), (ClipboardOwner) ProjectToolGUI.this, ProjectToolGUI.this);
                            } catch (Exception e) {
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            copyHashSelectionMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            try {
                                ProjectFilePart part = (ProjectFilePart) projectPanel.getSelectedParts().toArray()[0];
                                GUIUtil.copyToClipboard(part.getHash().toString(), (ClipboardOwner) ProjectToolGUI.this);
                            } catch (Exception e) {
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            selectionMenu.addMenuListener(new MenuListener() {

                public void menuSelected(MenuEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            boolean isLoaded = projectPanel.isProjectLoaded(),
                                    isSelectionMade = false,
                                    isSingleSelection = false;
                            if (isLoaded) {
                                isSelectionMade = (projectPanel.getSelectedParts().size() > 0);
                                isSingleSelection = projectPanel.getSelectedParts().size() == 1;
                            }
                            selectAllMenuItem.setEnabled(isLoaded);
                            deselectAllMenuItem.setEnabled(isLoaded);
                            downloadSelectionMenuItem.setEnabled(isLoaded && isSelectionMade);
                            showHashSelectionMenuItem.setEnabled(isLoaded && isSelectionMade && isSingleSelection);
                            copyHashSelectionMenuItem.setEnabled(showHashSelectionMenuItem.isEnabled());
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

            tableMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            uncheckViewItems();
                            projectPanel.setView(ProjectPanel.VIEW_TABLE);
                            tableMenuItem.setSelected(true);
                            ProjectToolGUI.this.projectPopupMenu.uncheckViewItems();
                            ProjectToolGUI.this.projectPopupMenu.tableMenuItem.setSelected(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            treeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            uncheckViewItems();
                            projectPanel.setView(ProjectPanel.VIEW_TREE);
                            treeMenuItem.setSelected(true);
                            ProjectToolGUI.this.projectPopupMenu.uncheckViewItems();
                            ProjectToolGUI.this.projectPopupMenu.treeMenuItem.setSelected(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            tilesMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            uncheckViewItems();
                            projectPanel.setView(ProjectPanel.VIEW_TILES);
                            tilesMenuItem.setSelected(true);
                            ProjectToolGUI.this.projectPopupMenu.uncheckViewItems();
                            ProjectToolGUI.this.projectPopupMenu.tilesMenuItem.setSelected(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            listMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            uncheckViewItems();
                            projectPanel.setView(ProjectPanel.VIEW_LIST);
                            listMenuItem.setSelected(true);
                            ProjectToolGUI.this.projectPopupMenu.uncheckViewItems();
                            ProjectToolGUI.this.projectPopupMenu.listMenuItem.setSelected(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            iconsMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            uncheckViewItems();
                            projectPanel.setView(ProjectPanel.VIEW_ICONS);
                            iconsMenuItem.setSelected(true);
                            ProjectToolGUI.this.projectPopupMenu.uncheckViewItems();
                            ProjectToolGUI.this.projectPopupMenu.iconsMenuItem.setSelected(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });

            // set the mnemonics
            {
                openMenuItem.setMnemonic('o');
                closeMenuItem.setMnemonic('c');
                moreInfoMenuItem.setMnemonic('m');
                downloadAllMenuItem.setMnemonic('d');
                showHashMenuItem.setMnemonic('h');
                copyHashMenuItem.setMnemonic('p');

                selectAllMenuItem.setMnemonic('s');
                deselectAllMenuItem.setMnemonic('d');
                downloadSelectionMenuItem.setMnemonic('o');
                showHashSelectionMenuItem.setMnemonic('h');
                copyHashSelectionMenuItem.setMnemonic('c');

                tableMenuItem.setMnemonic('t');
                treeMenuItem.setMnemonic('r');
                tilesMenuItem.setMnemonic('e');
                listMenuItem.setMnemonic('l');
                iconsMenuItem.setMnemonic('i');
            }

            // add the project menu items
            {
                dataSetMenu.add(openMenuItem);
                dataSetMenu.add(closeMenuItem);

                JSeparator separator1 = new JSeparator();
                separator1.setForeground(Color.BLACK);
                dataSetMenu.add(separator1);

                dataSetMenu.add(moreInfoMenuItem);
                dataSetMenu.add(downloadAllMenuItem);
                dataSetMenu.add(showHashMenuItem);
                dataSetMenu.add(copyHashMenuItem);
            }

            // add the selection menu items
            {
                selectionMenu.add(selectAllMenuItem);
                selectionMenu.add(deselectAllMenuItem);

                JSeparator separator1 = new JSeparator();
                separator1.setForeground(Color.BLACK);
                selectionMenu.add(separator1);

                selectionMenu.add(downloadSelectionMenuItem);

                JSeparator separator2 = new JSeparator();
                separator2.setForeground(Color.BLACK);
                selectionMenu.add(separator2);

                selectionMenu.add(showHashSelectionMenuItem);
                selectionMenu.add(copyHashSelectionMenuItem);
            }

            // add the view menu items
            {
                listMenuItem.setSelected(true);
                viewMenu.add(tableMenuItem);
                //viewMenu.add(treeMenuItem);
                //viewMenu.add(tilesMenuItem);
                viewMenu.add(listMenuItem);
            //viewMenu.add(iconsMenuItem);
            }

            // set Mnemonics
            dataSetMenu.setMnemonic('p');
            selectionMenu.setMnemonic('s');
            viewMenu.setMnemonic('v');

            // add the menus
            add(dataSetMenu);
            add(selectionMenu);
            add(viewMenu);

            // create a listener for the searc field and button
            ActionListener a = new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            projectPanel.setFilter(searchTextField.getText().trim());
                            projectPanel.refresh();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            };

            // add the search text field
            searchTextField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 1, 0, Color.BLACK), BorderFactory.createEmptyBorder(0, 5, 0, 0)));
            searchTextField.setMaximumSize(new Dimension(200, 19));
            searchTextField.setFont(new Font("Search Text Font", Font.PLAIN, 11));
            searchTextField.setToolTipText("The regular expression of the files you want to look at.");
            searchTextField.addActionListener(a);
            add(searchTextField);

            // add the search button
            searchButton.setMinimumSize(new Dimension(19, 19));
            searchButton.setSize(new Dimension(19, 19));
            searchButton.setPreferredSize(new Dimension(19, 19));
            searchButton.setMaximumSize(new Dimension(19, 19));
            searchButton.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, Color.BLACK));
            searchButton.setBackground(Color.WHITE);
            searchButton.setFont(Styles.FONT_10PT);
            searchButton.setFocusable(false);
            searchButton.setToolTipText("The regular expression of the files you want to look at.");
            try {
                searchButton.setIcon(new ImageIcon(ImageIO.read(ProjectToolGUI.this.getClass().getResourceAsStream("/org/tranche/gui/image/system-search-16x16.gif"))));
            } catch (Exception e) {
            }
            searchButton.addActionListener(a);
            add(searchButton);
        }
    }

    public static void main(final String[] args) {
        // configure Tranche network
        ConfigureTrancheGUI.load(args);

        // create the frame
        final ProjectToolGUI gui = new ProjectToolGUI();
        gui.setDefaultCloseOperation(ProjectToolGUI.EXIT_ON_CLOSE);

        // show the frame
        GUIUtil.centerOnScreen(gui);
        gui.setVisible(true);

        Thread t = new Thread() {

            @Override
            public void run() {
                Set<Exception> exceptions = new HashSet<Exception>();
                BigHash hash = null;
                String pass = null;
                for (int i = 1; i < args.length; i += 2) {
                    try {
                        if (args[i].equals("--fileName") || args[i].equals("--hash") || args[i].equals("-h")) {
                            hash = BigHash.createHashFromString(args[i + 1]);
                        } else if (args[i].equals("--passphrase") || args[i].equals("-p")) {
                            pass = args[i + 1];
                        } else if (args[i].equals("-L") || args[i].equals("--login")) {
                            try {
                                GUIUtil.setUser(UserZipFileUtil.getUserZipFile(args[i + 1], args[i + 2]));
                            } catch (InvalidSignInException e) {
                                GenericOptionPane.showMessageDialog(gui, e.getMessage(), "Could Not Sign In", JOptionPane.ERROR_MESSAGE);
                            } finally {
                                i++;
                            }
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
                if (hash != null) {
                    try {
                        if (pass != null) {
                            PassphrasePool.set(hash, pass);
                        }
                        gui.open(hash, null);
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
                if (!exceptions.isEmpty()) {
                    gui.ef.show(exceptions, gui);
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    // the following is for the clipboardowner abstract
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
