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

import org.tranche.gui.user.SignInPanel;
import org.tranche.gui.user.SignInFrame;
import org.tranche.gui.project.EncryptedProjectsFrame;
import org.tranche.gui.get.CSVDownloadPanel;
import org.tranche.gui.hash.BigHashHelperPanel;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.user.CreateUserZipFileFrame;
import org.tranche.gui.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.tranche.gui.module.Installable;
import org.tranche.gui.module.SelectableModuleAction;
import org.tranche.gui.server.ServerConfigurationFrame;
import org.tranche.time.TimeUtil;
import org.tranche.users.UserZipFile;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class TopPanel extends JPanel implements Installable {

    public ServerConfigurationFrame serverConfigurationFrame = new ServerConfigurationFrame();
    public EncryptedProjectsFrame encryptedProjectsFrame = new EncryptedProjectsFrame();
    private JButton preferences = new TopMenu("Preferences"), help = new TopMenu("Help"), advancedTools = new TopMenu("Advanced Tools"), signIn = new TopButton("Sign In");
    public JLabel userLabel = new GenericLabel(""), separator = new GenericLabel("|");
    public JButton logOut = new TopButton("Log Out");
    public JMenuItem userPreferencesMenuItem = new TopMenuItem("Open Preferences"), localServerMenuItem = new TopMenuItem("Server Configuration"), helpMenuItem = new TopMenuItem("FAQ and Contact Us"), aboutMenuItem = new TopMenuItem("About Tranche"), CSVDownloadMenuItem = new TopMenuItem("Download from CSV File"), hashHelperItem = new TopMenuItem("Hash"), encryptedProjectsMenuItem = new TopMenuItem("Passphrase Manager"), createNewUserMenuItem = new TopMenuItem("Create New User"), troubleshootingInformationMenuItem = new TopMenuItem("Troubleshooting information");//, proxySettingsMenuItem = new TopMenuItem("Proxy Settings"), moduleManagerItem = new TopMenuItem("Module Manager");
    private JPopupMenu advancedMenu = new JPopupMenu();
    private SignInFrame logInFrame = new SignInFrame();

    public TopPanel() {
        // set the layout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // set the look
        setBackground(Color.DARK_GRAY);
        setBorder(BorderFactory.createMatteBorder(0, 0, 5, 0, Styles.COLOR_TRIM));

        // add the preferences menu
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 8, 0, 0);
        add(preferences, gbc);
        add(help, gbc);

        gbc.insets = new Insets(0, 8, 0, 0);
        gbc.weightx = 1;
        add(advancedTools, gbc);

        // add a separator
        separator.setForeground(Color.WHITE);
        separator.setFont(Styles.FONT_10PT_BOLD);

        // add the right buttons, only the create user and the sign in are added initially
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(Styles.FONT_11PT_BOLD);
        userLabel.setVisible(false);
        gbc.insets = new Insets(6, 0, 0, 6);
        add(userLabel, gbc);
        gbc.insets = new Insets(4, 0, 0, 0);
        separator.setVisible(false);
        add(separator, gbc);
        gbc.insets = new Insets(0, 0, 0, 8);
        add(signIn, gbc);
        logOut.setVisible(false);
        add(logOut, gbc);

        // create the preferences sub-menu
        JPopupMenu preferencesMenu = new JPopupMenu();
        preferencesMenu.add(userPreferencesMenuItem);
        preferencesMenu.add(localServerMenuItem);

        // the the help sub-menu
        JPopupMenu helpMenu = new JPopupMenu();
        helpMenu.add(helpMenuItem);
        helpMenu.add(hashHelperItem);
//        helpMenu.add(proxySettingsMenuItem);
        helpMenu.add(aboutMenuItem);
        helpMenu.add(troubleshootingInformationMenuItem);

        // Add default advanced options.
        addDefaultAdvancedOptions();

        // set the mouse listeners for these buttons
        preferences.addMouseListener(new TopMenuListener(preferences, preferencesMenu));
        help.addMouseListener(new TopMenuListener(help, helpMenu));
        advancedTools.addMouseListener(new TopMenuListener(advancedTools, advancedMenu));

        // set the actions for these buttons
        createNewUserMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override()
                    public void run() {
                        // launch the create user tool
                        CreateUserZipFileFrame frame = new CreateUserZipFileFrame();
                        frame.setLocationRelativeTo(TopPanel.this.getParent());
                        frame.setVisible(true);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        signIn.setMnemonic('l');
        signIn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        try {
                            if (logInFrame == null) {
                                logInFrame = new SignInFrame();
                            }
                            logInFrame.setLocationRelativeTo(GUIUtil.getAdvancedGUI());
                            logInFrame.setVisible(true);

                            // getUser() in loginFrame pauses until the user is returned
                            UserZipFile uzf = logInFrame.getPanel().getUser();
                            logInFrame.dispose();
                            // will return immediately
                            GUIUtil.setUserEmail(logInFrame.getPanel().getEmail());
                            logInFrame = null;
                            if (uzf != SignInPanel.BLANK_USER_ZIP_FILE) {
                                setUser(uzf);
                            } else {
                                setUser(null);
                            }
                        } catch (Exception e) {
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        logOut.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        closeUser();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });

        // add the left menu listeners
        userPreferencesMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override()
                    public void run() {
                        // Build and display the serverConfigurationFrame
                        GUIUtil.getPreferencesFrame().setLocationRelativeTo(GUIUtil.getAdvancedGUI());
                        GUIUtil.getPreferencesFrame().setVisible(true);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
//        proxySettingsMenuItem.addActionListener(new ActionListener() {
//
//            public void actionPerformed(ActionEvent e) {
//                Thread t = new Thread() {
//
//                    @Override()
//                    public void run() {
//                        GUIUtil.displayURL("https://trancheproject.org/help/webstart-proxies.html");
//                    }
//                };
//                t.setDaemon(true);
//                t.start();
//            }
//        });
        localServerMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override()
                    public void run() {
                        serverConfigurationFrame.setLocationRelativeTo(GUIUtil.getAdvancedGUI());
                        serverConfigurationFrame.setVisible(true);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });

        helpMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        // Build and display the popup
                        GenericPopupFrame popup = new GenericPopupFrame("Help", new HelpPanel());
                        popup.setSizeAndCenter(HelpPanel.RECOMMENDED_DIMENSION);
                        popup.setVisible(true);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        aboutMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        GenericPopupFrame popup = new GenericPopupFrame("About", new AboutPanel());
                        popup.setSizeAndCenter(AboutPanel.RECOMMENDED_DIMENSION);
                        popup.setVisible(true);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        encryptedProjectsMenuItem.setToolTipText("Opens a frame where you can input and save your Tranche passwords.");
        encryptedProjectsMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                encryptedProjectsFrame.setLocationRelativeTo(GUIUtil.getAdvancedGUI());
                encryptedProjectsFrame.setVisible(true);
            }
        });
        CSVDownloadMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        CSVDownloadPanel dpanel = new CSVDownloadPanel();
                        GenericPopupFrame popup = new GenericPopupFrame("Download from CSV File", dpanel);
                        dpanel.frame = popup;
                        popup.setSizeAndCenter(CSVDownloadPanel.RECOMMENDED_DIMENSION);
                        popup.setVisible(true);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        troubleshootingInformationMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        TroubleshootingInformationPanel tp = new TroubleshootingInformationPanel();
                        GenericPopupFrame popup = new GenericPopupFrame(TroubleshootingInformationPanel.TITLE, tp);
                        popup.setSizeAndCenter(TroubleshootingInformationPanel.RECOMMENDED_DIMENSION);
                        popup.setVisible(true);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        
//        moduleManagerItem.addActionListener(new ActionListener() {
//
//            public void actionPerformed(ActionEvent e) {
//                Thread t = new Thread() {
//
//                    @Override
//                    public void run() {
//                        // Build and display the popup
//                        ModulePanel mpanel = new ModulePanel(TrancheModulesUtil.getModules());
//                        GenericPopupFrame popup = new GenericPopupFrame("Module Manager", mpanel);
//                        mpanel.setFrame(popup);
//                        popup.setSizeAndCenter(ModulePanel.RECOMMENDED_DIMENSION);
//                        popup.setVisible(true);
//                    }
//                };
//                t.setDaemon(true);
//                t.start();
//            }
//        });
        hashHelperItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        GenericPopupFrame popup = new GenericPopupFrame("Tranche Hash Helper", new BigHashHelperPanel());
                        popup.setSizeAndCenter(BigHashHelperPanel.RECOMMENDED_DIMENSION);
                        popup.setVisible(true);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });

        // set the mouse listeners for these buttons
        signIn.addMouseListener(new TopButtonListener(signIn));
        logOut.addMouseListener(new TopButtonListener(logOut));
    }

    /**
     * Add the default options to the Advanced Tools menu
     */
    private void addDefaultAdvancedOptions() {
        advancedMenu.add(encryptedProjectsMenuItem);
        advancedMenu.add(createNewUserMenuItem);
        advancedMenu.add(CSVDownloadMenuItem);
//        advancedMenu.add(moduleManagerItem);
    }

    /**
     * Called by GUiMediatorUtil to install an action from a module.
     */
    public void installAction(final SelectableModuleAction action) {
//        // Add to in-memory list of installed actions
//        this.installedActions.add(action);

        // Create a button
        JMenuItem menuItem = new TopMenuItem(action.label);
        menuItem.setToolTipText(action.description);
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread("Run module argument thread (from TopPanel)") {

                    @Override
                    public void run() {
                        try {
                            // Better not take any arguments!
                            action.execute(new Object[0]);
                        } catch (Exception ex) {
                            ex.printStackTrace(System.err);
                            GUIUtil.getAdvancedGUI().ef.show(ex, GUIUtil.getAdvancedGUI());
                        }
                    }
                };
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
            }
        });

        // Add to popup menu
        this.advancedMenu.add(menuItem);
    }

    /**
     * Clears all the actions installed from modules.
     */
    public void clearActions() {

        // Remove all
        this.advancedMenu.removeAll();

        // Re-add the defaults
        addDefaultAdvancedOptions();

//        // Clear in-memory list
//        this.installedActions.clear();
    }

    /**
     * Negotiate visibility/selectability of module.
     */
    public void negotateActionStatus(SelectableModuleAction action, boolean isEnabled) {
        Component c;
        JMenuItem m;
        for (int i = 0; i < this.advancedMenu.getComponentCount(); i++) {
            c = this.advancedMenu.getComponent(i);
            if (c instanceof JMenuItem) {
                m = (JMenuItem) c;
                if (m.getText().equals(action.label)) {
                    m.setVisible(isEnabled);
                    return;
                }
            }
        }
    }

    public UserZipFile getUser() {
        return GUIUtil.getUser();
    }

    public void loadUser() {
        final JFileChooser jfc = GUIUtil.makeNewFileChooser();
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    setUser(GUIUtil.promptForUserFile(jfc, GUIUtil.getAdvancedGUI()));
                } // Exceptions aren't typed uniquely, parse message
                catch (Exception e) {

                    // Incorrect passphrase message
                    if (e.getMessage().contains("encoding") || e.getMessage().contains("passphrase")) {
                        GenericOptionPane.showMessageDialog(
                                GUIUtil.getAdvancedGUI(),
                                "Invalid password. Please try again.",
                                "Invalid Password",
                                JOptionPane.WARNING_MESSAGE);
                    } // Expired user message
                    else if (e.getMessage().contains("expired")) {
                        GenericOptionPane.showMessageDialog(
                                GUIUtil.getAdvancedGUI(),
                                "The user file you are using is expired. Please obtain a new one.",
                                "User File Expired",
                                JOptionPane.WARNING_MESSAGE);
                    } // Fall back on standard error serverConfigurationFrame
                    else {
                        GUIUtil.getAdvancedGUI().ef.show(e, GUIUtil.getAdvancedGUI());
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    public void setUser(final UserZipFile uzf) throws Exception {
        // if null, finish
        if (uzf == null) {
            GUIUtil.setUser(null);
            return;
        }

        // check the date of the certificate
        try {
            uzf.getCertificate().checkValidity(new Date(TimeUtil.getTrancheTimestamp()));
        } catch (CertificateExpiredException cee) {
            throw new CertificateExpiredException("That user file is expired. Please obtain a new one.");
        } catch (CertificateNotYetValidException cnyve) {
            throw new CertificateNotYetValidException("That user file is not yet valid.");
        }

        GUIUtil.setUser(uzf);

        // set the user label
        userLabel.setText(uzf.getUserNameFromCert());
        // possibly put email
        if (GUIUtil.getUserEmail() != null) {
            userLabel.setText(userLabel.getText() + " (" + GUIUtil.getUserEmail() + ")");
        }

        // show that the user is loaded
        signIn.setVisible(false);
        userLabel.setVisible(true);
        separator.setVisible(true);
        logOut.setVisible(true);
    }

    public void closeUser() {
        // unset the user
        GUIUtil.setUser(null);

        // show that no user is loaded
        signIn.setVisible(true);
        userLabel.setVisible(false);
        separator.setVisible(false);
        logOut.setVisible(false);
    }

    /**
     * Message popups up if no user set.
     * @param msg Message to show if no user.
     * @return True if user set, false if not.
     */
    public boolean checkForUser(String msg) {

        if (GUIUtil.getUser() == null) {

            GenericOptionPane.showMessageDialog(
                    GUIUtil.getAdvancedGUI(),
                    msg,
                    "Cannot perform action",
                    JOptionPane.WARNING_MESSAGE);

            return false;
        }

        return true;
    }

    private class TopMenuItem extends JMenuItem {

        public TopMenuItem(String title) {
            super(title);
            setFont(Styles.FONT_12PT);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }
    }

    private class TopMenu extends JButton {

        public TopMenu(String title) {
            super(title);
            setBackground(Color.DARK_GRAY);
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
            setFocusable(false);
        }

        @Override
        public void paint(Graphics g) {
            Color save = null;
            if (getModel().isArmed() && getModel().isPressed()) {
                save = UIManager.getColor("Button.select");
                UIManager.put("Button.select", getBackground());
            }
            super.paint(g);
            if (save != null) {
                UIManager.put("Button.select", save);
            }
        }
    }

    private class TopButton extends JButton {

        public TopButton(String title) {
            super(title);
            setFont(Styles.FONT_11PT);
            setBackground(Color.DARK_GRAY);
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(12, 8, 7, 8));
            setFocusable(false);
        }

        @Override
        public void paint(Graphics g) {
            Color save = null;
            if (getModel().isArmed() && getModel().isPressed()) {
                save = UIManager.getColor("Button.select");
                UIManager.put("Button.select", getBackground());
            }
            super.paint(g);
            if (save != null) {
                UIManager.put("Button.select", save);
            }
        }
    }

    private class TopButtonListener extends MouseAdapter {

        JButton component;

        public TopButtonListener(JButton component) {
            this.component = component;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            component.setBackground(Styles.COLOR_TRIM);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            component.setBackground(Color.DARK_GRAY);
        }
    }

    private class TopMenuListener extends MouseAdapter {

        private JButton component;
        private JPopupMenu menu;

        public TopMenuListener(final JButton component, JPopupMenu menu) {
            this.component = component;
            this.menu = menu;
            menu.setBorder(Styles.BORDER_BLACK_1);
            menu.setBackground(Color.GRAY);
            menu.addPopupMenuListener(new PopupMenuListener() {

                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    component.setBackground(Styles.COLOR_TRIM);
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    component.setBackground(Color.DARK_GRAY);
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            showPopup(e);
        }

        private void showPopup(final MouseEvent e) {
            menu.show(e.getComponent(), 0, component.getLocation().y + component.getHeight());
        }
    }
}
