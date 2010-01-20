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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.tranche.LocalDataServer;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.gui.DisplayTextArea;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GenericButton;
import org.tranche.gui.GenericCheckBox;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericPopupListener;
import org.tranche.gui.GenericRoundedButton;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.GenericTable;
import org.tranche.gui.GenericTextArea;
import org.tranche.gui.GenericTextField;
import org.tranche.gui.IndeterminateProgressBar;
import org.tranche.gui.SortableTableModel;
import org.tranche.gui.Styles;
import org.tranche.gui.user.SignInUserButton;
import org.tranche.hash.span.AbstractHashSpan;
import org.tranche.hash.span.HashSpan;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.users.User;
import org.tranche.users.UserZipFile;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;

/**
 * <p>All the options needed to start and configure a local server or configure a remote server.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServerConfigurationPanel extends JPanel {

    private static boolean debug = false;
    // button for saving the changes
    public ErrorFrame ef = new ErrorFrame();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private final static int HORIZONTAL_MARGIN = 10;
    private LocalServerPanel localServerPanel = new LocalServerPanel();
    private ExternalServerPanel externalServerPanel = new ExternalServerPanel();
    private UsersPanel usersPanel = new UsersPanel();
    private HashSpansPanel targetHashSpansPanel = new HashSpansPanel();
    private DataDirectoriesPanel dataDirectoriesPanel = new DataDirectoriesPanel();
    private AttributesPanel attributesPanel = new AttributesPanel();
    // the loaded server host and configuration
    private String loadedServerHost;
    private Configuration configuration;
    private JLabel loadedServerLabel = new GenericLabel("No server configuration loaded.");
    private JButton saveButton;

    /**
     * <p>A hook for shutdown to cloes down resources.</p>
     */
    public void hookClosingWindow() {
        if (attributesPanel != null) {
            attributesPanel.preventDaemonThreads();
        }
    }

    public ServerConfigurationPanel() {
        setLayout(new BorderLayout());
        setBackground(Styles.COLOR_BACKGROUND);

        // add the label
        loadedServerLabel.setFont(Styles.FONT_14PT_BOLD);
        loadedServerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
        add(loadedServerLabel, BorderLayout.NORTH);

        tabbedPane.setBackground(Styles.COLOR_BACKGROUND);
        tabbedPane.setFocusable(false);
        tabbedPane.add("Local Server", localServerPanel);
        tabbedPane.add("External Server", externalServerPanel);
        tabbedPane.add("Users", new GenericScrollPane(usersPanel, GenericScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, GenericScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        tabbedPane.add("Target Hash Spans", new GenericScrollPane(targetHashSpansPanel, GenericScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, GenericScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        tabbedPane.add("Data Directories", new GenericScrollPane(dataDirectoriesPanel, GenericScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, GenericScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        tabbedPane.add("Attributes", new GenericScrollPane(attributesPanel, GenericScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, GenericScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        add(tabbedPane, BorderLayout.CENTER);
        // Add change listener for tabs. Do whatever house keeping needs done.
        tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                // The save button should be visible for any tab but first and second
                if (tabbedPane.getSelectedComponent().equals(localServerPanel) || tabbedPane.getSelectedComponent().equals(externalServerPanel)) {
                    saveButton.setVisible(false);
                } else {
                    saveButton.setVisible(true);
                }
            }
        });

        // Some tabs should not be focusable until server started
        setServerIsLoaded(LocalDataServer.isServerRunning());

        // Add save button, which shouldn't be visible for first tab
        saveButton = new GenericButton("Save Changes");
        // By default invisible. If tabs change, may be set to visible
        saveButton.setVisible(false);
        saveButton.setBorder(Styles.BORDER_BUTTON_TOP);
        add(saveButton, BorderLayout.SOUTH);
        // Add save listener. Save the configuration or report an exception.
        saveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                if (attributesPanel.isAutoUpdating()) {
                    GenericOptionPane.showMessageDialog(
                            ServerConfigurationPanel.this,
                            "To save the configuration, you must unselect the auto-update feature in the Attribute panel.\n\nThe reason is that the configuration might update on your computer while you are saving, overwriting your changes.\n\nAfter you unselect the checkbox, try saving again.",
                            "Please turn off auto-update in attributes panel first",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                save();
            }
        });
    }

    private void clearConfiguration() {
        // unset the old server config
        configuration = null;
        setServerIsLoaded(false);
    }

    public void setConfiguration(Configuration configuration) {
        debugOut("Setting configuration with " + configuration.getTargetHashSpans().size() + " target hash spans.");

        // unset the old configuration
        clearConfiguration();

        // Starting a new server, resume running
        if (attributesPanel != null) {
            attributesPanel.resumeDaemonThreads();
        }

        // Duplicate the server Configuration so don't share a memory reference.
        // This is really only an issue if editting a localhost configuration,
        // but that's important enough to clone. Pretty cheap operation anyhow.
        this.configuration = configuration.clone();

        debugOut("Set configuration with " + this.configuration.getTargetHashSpans().size() + " target hash spans.");

        // set the server configuration to on
        setServerIsLoaded(configuration != null);
    }

    /**
     * Sets focusability/visibility of GUI components, updates to GUI.
     * @param isLoaded True if a server configuration is loaded, false otherwise.
     */
    public void setServerIsLoaded(boolean isLoaded) {
        if (isLoaded) {
            usersPanel.build();
            targetHashSpansPanel.build(true);
            dataDirectoriesPanel.build();
            attributesPanel.build();
        } else {
            usersPanel.tearDown();
            targetHashSpansPanel.tearDown();
            dataDirectoriesPanel.tearDown();
            attributesPanel.tearDown();
        }
        // change the enabled value of every tab except the first two
        for (int i = 2; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setEnabledAt(i, isLoaded);
        }
    }

    /**
     * Saves all information when user selects to save information.
     */
    public void save() {
        Thread t = new Thread() {

            @Override()
            public void run() {
                try {
                    // get the current user
                    UserZipFile user = GUIUtil.getAdvancedGUI().getUser();

                    // make sure a user is loaded
                    if (user == null) {
                        GenericOptionPane.showMessageDialog(
                                ServerConfigurationPanel.this,
                                "You must load your user file before saving configuration.",
                                "Cannot save configuration",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    int verifySave = GenericOptionPane.showConfirmDialog(
                            ServerConfigurationPanel.this,
                            "Are you sure you want to save the configuration for " + loadedServerHost,
                            "Really save?",
                            JOptionPane.YES_NO_OPTION);

                    if (verifySave == JOptionPane.NO_OPTION) {
                        return;
                    }

                    debugOut("Setting configuration for " + loadedServerHost);

                    // clear the old set of users
                    configuration.clearUsers();
                    // set the users
                    for (SingleUserEmbeddedPanel p : usersPanel.getEmbeddedUserPanels()) {

                        User nextUser = p.getUser();

                        // Build up user permissions from p
                        Map<Byte, Boolean> permissions = p.getPermissions();

                        int flags = 0;

                        if (permissions.get(SingleUserEmbeddedPanel.READ_CONFIG)) {
                            flags += User.CAN_GET_CONFIGURATION;
                        }
                        if (permissions.get(SingleUserEmbeddedPanel.WRITE_CONFIG)) {
                            flags += User.CAN_SET_CONFIGURATION;
                        }
                        if (permissions.get(SingleUserEmbeddedPanel.WRITE_DATA)) {
                            flags += User.CAN_SET_DATA;
                        }
                        if (permissions.get(SingleUserEmbeddedPanel.WRITE_META)) {
                            flags += User.CAN_SET_META_DATA;
                        }
                        if (permissions.get(SingleUserEmbeddedPanel.DELETE_DATA)) {
                            flags += User.CAN_DELETE_DATA;
                        }
                        if (permissions.get(SingleUserEmbeddedPanel.DELETE_META)) {
                            flags += User.CAN_DELETE_META_DATA;
                        }

                        nextUser.setFlags(flags);
                        configuration.addUser(nextUser);
                    }

                    debugOut("... set " + usersPanel.getEmbeddedUserPanels().size() + " users.");

                    // clear the hash spans
                    configuration.clearTargetHashSpans();
                    // Set the hash spans
                    configuration.setTargetHashSpans(targetHashSpansPanel.getHashSpans());

                    debugOut("... set " + targetHashSpansPanel.getHashSpans().size() + " target hash spans.");
                    debugOut("... there are " + configuration.getTargetHashSpans().size() + " target hash spans in the configuration.");

                    // clear the directories
                    configuration.clearDataDirectories();
                    // Save the data directory information
                    configuration.setDataDirectories(dataDirectoriesPanel.getDataDirectoryConfigurations());

                    debugOut("... set " + dataDirectoriesPanel.getDataDirectoryConfigurations().size() + " data directories.");

                    // First, replace the attributes.
                    Map<String, String> pairs = attributesPanel.getNameValuePairs();
                    for (String key : pairs.keySet()) {
                        configuration.removeKeyValuePair(key);
                        configuration.setValue(key, pairs.get(key));
                    }

                    // Next, remove any attributes that were not in the pairs. That
                    // way, removed attributes are committed.
                    // Use a list then remove to avoid ConcurrentModificationException
                    List<String> attributesToRemove = new ArrayList();
                    for (String key : configuration.getValueKeys()) {
                        if (!pairs.keySet().contains(key)) {
                            attributesToRemove.add(key);
                        }
                    }
                    for (String key : attributesToRemove) {
                        configuration.removeKeyValuePair(key);
                    }

                    debugOut("... set " + attributesPanel.getNameValuePairs().size() + " attributes.");

                    // Connect to the server
                    TrancheServer ts = ConnectionUtil.connectHost(loadedServerHost, true);
                    if (ts == null) {
                        throw new Exception("Could not connect with " + loadedServerHost + ".");
                    }
                    try {
                        IOUtil.setConfiguration(ts, configuration, user.getCertificate(), user.getPrivateKey());
                        debugOut("... saved configuration to " + loadedServerHost);
                    } finally {
                        ConnectionUtil.unlockConnection(loadedServerHost);
                    }
                    // set the label to reflect the change
                    loadedServerLabel.setText("Server configuration loaded: " + loadedServerHost);
                } catch (Exception e) {
                    debugErr(e);
                    ef.show(e, ServerConfigurationPanel.this);
                    return;
                }

                debugOut(NetworkUtil.getStatus().toString());

                // notify the user of success
                GenericOptionPane.showMessageDialog(
                        ServerConfigurationPanel.this,
                        "The modified configuration was successfully saved.",
                        "Configuration Saved",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };
        // Don't close if busy!
        t.setDaemon(false);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /**
     * Sets the loaded server's host and loads the configuration of that server, if possible.
     */
    public void setServerHost(String host) throws Exception {
        loadedServerHost = host;

        // get the user
        UserZipFile user = GUIUtil.getAdvancedGUI().getUser();

        // Connect to the server
        TrancheServer ts = ConnectionUtil.connectHost(host, true);
        if (ts == null) {
            throw new Exception("Could not connect with " + host + ".");
        }
        try {
            // get the configuration
            if (GUIUtil.getAdvancedGUI().getUser() == null) {
                setConfiguration(IOUtil.getConfiguration(ts, SecurityUtil.getDefaultCertificate(), SecurityUtil.getDefaultKey()));
            } else {
                setConfiguration(IOUtil.getConfiguration(ts, user.getCertificate(), user.getPrivateKey()));
            }
        } finally {
            ConnectionUtil.unlockConnection(host);
        }
        // set the label to reflect the change
        loadedServerLabel.setText("Server configuration loaded: " + host);
    }

    /**
     * Turn server on/off, set port, SSL
     */
    private class LocalServerPanel extends JPanel {

        private SignInUserButton userButton = new SignInUserButton();
        private JTextField portField = new GenericTextField(),  rootDirField = new GenericTextField();
        private GenericCheckBox sslCheckBox = new GenericCheckBox("SSL", LocalDataServer.isSSL());
        private JButton toggleServerButton = null;
        private int MARGIN = ServerConfigurationPanel.HORIZONTAL_MARGIN;
        // Text for the toggleServerButton
        private final String START_TEXT = "Start the server",  STOP_TEXT = "Stop the server";

        public LocalServerPanel() {
            setBackground(Styles.COLOR_BACKGROUND);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, MARGIN, MARGIN);
            add(new DisplayTextArea("Start a Tranche server. Your user will be set as an administrator of the server. The \"Server User\" is the user certificate assigned to the server, with which it communicates with the rest of the network."), gbc);

            gbc.weighty = 0;

            // user
            {
                gbc.gridwidth = 1;
                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.insets = new Insets(0, MARGIN, MARGIN, 0);
                JLabel name = new GenericLabel("Server User:");
                name.setToolTipText("The user zip file that represents the server.");
                add(name, gbc);

                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(0, MARGIN, MARGIN, MARGIN);
                userButton.setToolTipText("The user zip file that represents the server.");
                if (LocalDataServer.getUserZipFile() != null) {
                    userButton.setUser(LocalDataServer.getUserZipFile());
                }
                add(userButton, gbc);
            }

            // directory
            {
                gbc.gridwidth = 1;
                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.insets = new Insets(0, MARGIN, MARGIN, 0);
                JLabel name = new GenericLabel("Directory:");
                name.setToolTipText("The root directory for the server.");
                add(name, gbc);

                // display the value
                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(0, MARGIN, MARGIN, MARGIN);
                rootDirField.setToolTipText("The root directory for the server.");
                rootDirField.setText(LocalDataServer.getRootDirectory().getAbsolutePath());
                add(rootDirField, gbc);

                // add a button to select a file
                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.insets = new Insets(0, 0, MARGIN, MARGIN);
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                JButton fileButton = new GenericButton("   Select   ");
                fileButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        final JFileChooser fc = GUIUtil.makeNewFileChooser();
                        Thread t = new Thread() {

                            @Override
                            public void run() {
                                // query for a file
                                fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                                // make the last seleected upload file the default
                                fc.setSelectedFile(LocalDataServer.getRootDirectory());
                                // open it up
                                fc.showOpenDialog(ServerConfigurationPanel.this.getParent());
                                // check for a submitted file
                                File selectedFile = fc.getSelectedFile();
                                if (selectedFile == null) {
                                    return;
                                }
                                rootDirField.setText(selectedFile.getAbsolutePath());
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                    }
                });
                fileButton.setBorder(Styles.BORDER_BLACK_1_PADDING_SIDE_2);
                fileButton.setFont(Styles.FONT_10PT);
                fileButton.setToolTipText("Click to select the root directory for the server.");
                add(fileButton, gbc);
            }

            // Port label and text field
            {
                JLabel portLabel = new GenericLabel("Port: ");
                portLabel.setFont(Styles.FONT_12PT_BOLD);

                gbc.gridwidth = 1;
                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.insets = new Insets(0, MARGIN, 0, 0);
                add(portLabel, gbc);

                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.RELATIVE;
                gbc.insets = new Insets(0, MARGIN, 0, MARGIN);
                portField.setText(String.valueOf(LocalDataServer.getPort()));
                add(portField, gbc);

                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.insets = new Insets(0, 0, 0, MARGIN);
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                sslCheckBox.setSelected(LocalDataServer.isSSL());
                add(sslCheckBox, gbc);
            }

            // Server on/off
            {
                // Button depends on whether server is on or off.
                if (LocalDataServer.isServerRunning()) {
                    this.toggleServerButton = new GenericRoundedButton(STOP_TEXT);
                } else {
                    this.toggleServerButton = new GenericRoundedButton(START_TEXT);
                }

                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(10, MARGIN, 10, MARGIN);
                add(toggleServerButton, gbc);
            }

            // Register the listener
            this.toggleServerButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        if (GUIUtil.getAdvancedGUI().getUser() == null) {
                            throw new Exception("You must sign in to start a server.");
                        }

                        // If server is running, stop it
                        if (LocalDataServer.isServerRunning()) {
                            String host = LocalDataServer.getServer().getHostName();
                            try {
                                LocalDataServer.stop();
                            } catch (Exception ex) {
                                ErrorFrame eframe = new ErrorFrame();
                                eframe.show(ex, LocalServerPanel.this);
                                return;
                            }

                            // Swap the button's text
                            toggleServerButton.setText(START_TEXT);

                            userButton.setEnabled(true);
                            rootDirField.setEnabled(true);
                            portField.setEnabled(true);
                            sslCheckBox.setEnabled(true);

                            // set the label to reflect the change
                            loadedServerLabel.setText("No server configuration loaded.");

                            // All tabs should be selectable since server running
                            if (loadedServerHost.equals(host)) {
                                loadedServerHost = null;
                                configuration = null;
                                ServerConfigurationPanel.this.setServerIsLoaded(false);
                            }
                        } else {

                            final IndeterminateProgressBar progress = new IndeterminateProgressBar("Starting server");
                            progress.setLocationRelativeTo(ServerConfigurationPanel.this);
                            progress.start();

                            Thread t = new Thread("Start local server thread") {

                                @Override()
                                public void run() {
                                    try {
                                        // set user
                                        LocalDataServer.setUserZipFile(userButton.getUser());
                                        // set directory
                                        LocalDataServer.setRootDirectory(new File(rootDirField.getText()));
                                        // Set port
                                        LocalDataServer.setPort(Integer.parseInt(portField.getText()));
                                        // Use SSL?
                                        LocalDataServer.setSSL(sslCheckBox.isSelected());
                                        // Start the server
                                        LocalDataServer.start();
                                        // get the configuration
                                        configuration = LocalDataServer.getFlatFileTrancheServer().getConfiguration();
                                        // make sure the user is an admin
                                        User user = GUIUtil.getUser();
                                        user.setFlags(User.ALL_PRIVILEGES);
                                        configuration.addUser(user);
                                        LocalDataServer.getFlatFileTrancheServer().setConfiguration(configuration);
                                        // set the configuration for this tool
                                        setConfiguration(configuration);
                                        // set the loaded configuration
                                        loadedServerHost = LocalDataServer.getServer().getHostName();
                                        // set the label to reflect the change
                                        loadedServerLabel.setText("Local server configuration loaded.");
                                        // Swap the button's text
                                        toggleServerButton.setText(STOP_TEXT);
                                        userButton.setEnabled(false);
                                        rootDirField.setEnabled(false);
                                        portField.setEnabled(false);
                                        sslCheckBox.setEnabled(false);
                                    } catch (Exception ex) {
                                        ef.show(ex, ServerConfigurationPanel.this);
                                    } finally {
                                        progress.stop();
                                    }
                                }
                            };
                            t.setDaemon(true);
                            t.start();
                        }
                    } catch (Exception ex) {
                        ef.show(ex, ServerConfigurationPanel.this);
                    }
                }
            });
        }
    }

    /**
     * Load the server information for configuration
     */
    private class ExternalServerPanel extends JPanel {

        private int MARGIN = ServerConfigurationPanel.HORIZONTAL_MARGIN;

        public ExternalServerPanel() {
            // set the layout to gridbag
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            // Description
            {
                String description = "Load the configuration of any online Tranche server.\n\nIf you have been designated the priveleges to do so, you will be able to save the edited configuration back to that Tranche server.";

                JTextArea descriptionArea = new GenericTextArea(description);
                descriptionArea.setEditable(false);
                descriptionArea.setBackground(Styles.COLOR_BACKGROUND);
                descriptionArea.setWrapStyleWord(true);
                descriptionArea.setLineWrap(true);

                gbc.fill = GridBagConstraints.BOTH;
                gbc.gridy = 0;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.insets = new Insets(MARGIN, MARGIN, MARGIN, MARGIN);
                gbc.weighty = 1;
                gbc.weightx = 1;
                add(descriptionArea, gbc);
            }

            // Load Configuration Form
            {
                gbc.gridwidth = GridBagConstraints.RELATIVE;
                gbc.gridy = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(0, MARGIN, 0, 0);
                gbc.weightx = 0;
                gbc.weighty = 0;
                add(new GenericLabel("Server:"), gbc);

                // add a text box for the server
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(0, MARGIN, 0, MARGIN);
                final JTextField serverTextField = new GenericTextField();
                final JButton loadServerButton = new GenericRoundedButton("Load Configuration");
                serverTextField.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Thread t = new Thread("Load server host thread") {

                            @Override
                            public void run() {
                                // load the server
                                loadServerButton.doClick();
                            }
                        };
                        t.setDaemon(true);
                        t.setPriority(Thread.MIN_PRIORITY);
                        t.start();
                    }
                });
                add(serverTextField, gbc);

                // add the button
                gbc.gridy = 2;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.insets = new Insets(MARGIN, MARGIN, MARGIN, MARGIN);
                loadServerButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Thread t = new Thread("Load server url thread") {

                            @Override()
                            public void run() {
                                // set the new URL and load the configuration
                                try {
                                    setServerHost(serverTextField.getText());
                                } catch (Exception ee) {
                                    ef.show(ee, ServerConfigurationPanel.this);
                                }
                            }
                        };
                        t.setDaemon(true);
                        t.setPriority(Thread.MIN_PRIORITY);
                        t.start();
                    }
                });
                add(loadServerButton, gbc);
            }
        }
    } // external server panel

    /**
     * Set userPanels permissions, etc.
     */
    private class UsersPanel extends JPanel {

        private List<SingleUserEmbeddedPanel> userPanels;
        private JButton addUserButton;

        public UsersPanel() {

            this.userPanels = new ArrayList<SingleUserEmbeddedPanel>();

            this.setBackground(Styles.COLOR_BACKGROUND);

            // Set the layout
            this.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            // Add user button
            {
                this.addUserButton = new GenericButton("Add User");

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(5, 5, 0, 5);

                this.add(this.addUserButton, gbc);
            }

            // Add listener for add user button
            this.addUserButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final JFileChooser jfc = GUIUtil.makeNewFileChooser();
                    Thread t = new Thread() {

                        @Override()
                        public void run() {
                            try {
                                addUser(jfc);
                            } catch (Exception ex) {
                                ef.show(ex, UsersPanel.this);
                            }
                        }
                    };
                    t.start();
                }
            });
        }

        /**
         * <p>Called when server turned on.</p>
         */
        public void build() {

            GridBagConstraints gbc = new GridBagConstraints();

            // Get the config. For each user, create and add the panel
            for (User user : configuration.getUsers()) {
                if (user == null) {
                    continue;
                }
                SingleUserEmbeddedPanel nextUserPanel = new SingleUserEmbeddedPanel(user);
                nextUserPanel.setBorder(Styles.BORDER_BLACK_1);
                this.userPanels.add(nextUserPanel);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(5, 5, 0, 5);

                this.add(nextUserPanel, gbc);
            }
        }

        /**
         * Called when server is turned off.
         */
        public void tearDown() {

            for (SingleUserEmbeddedPanel userPanel : this.userPanels) {
                this.remove(userPanel);
            }

            this.userPanels.clear();
        }

        /**
         * Called when user wants to add a user
         */
        public void addUser(JFileChooser jfc) throws Exception {

            // Prompt user for user to add to server
            UserZipFile user = GUIUtil.promptForUserFile(jfc, this.getParent(), false);

            if (user == null) {
                return;
            }

            // Create a SingleUserEmbeddPanel
            SingleUserEmbeddedPanel userPanel = new SingleUserEmbeddedPanel(user);

            // Add to list of userPanels
            this.userPanels.add(userPanel);

            // Add to GUI
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.insets = new Insets(5, 5, 0, 5);

            // Add after button
            add(userPanel, gbc, 1);
            validate();
            ServerConfigurationPanel.this.repaint();

            // Popup message with success message, reminder to save changes
            GenericOptionPane.showMessageDialog(
                    this.getParent(),
                    "User added. Don't forget to save changes.",
                    "User added",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        public void removeUser(User user) {
            for (SingleUserEmbeddedPanel userPanel : userPanels) {
                if (userPanel.getUser().equals(user)) {
                    userPanels.remove(userPanel);
                    remove(userPanel);
                    validate();
                    ServerConfigurationPanel.this.repaint();
                    break;
                }
            }
        }

        public List<SingleUserEmbeddedPanel> getEmbeddedUserPanels() {
            return userPanels;
        }
    }

    /**
     * Represents a single user within the user panel
     */
    private class SingleUserEmbeddedPanel extends JPanel {

        private User user;
        private String name;
        private JButton removeButton;
        public final static byte READ_CONFIG = 0,  WRITE_CONFIG = 1,  WRITE_DATA = 2,  WRITE_META = 3,  DELETE_DATA = 4,  DELETE_META = 5;
        private GenericCheckBox readConfigBox,  writeConfigBox,  writeDataBox,  writeMetaBox,  deleteDataBox,  deleteMetaBox;

        public SingleUserEmbeddedPanel(final User user) {

            this.user = user;
            this.name = user.getCertificate().getSubjectDN().toString().split("CN=")[1].split(",")[0];

            this.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            this.setBackground(Color.WHITE);

            // Create check boxes
            this.readConfigBox = new GenericCheckBox("Configuration");
            this.readConfigBox.setSelected(this.getUser().canGetConfiguration());
            this.readConfigBox.setBackground(Color.WHITE);

            this.writeConfigBox = new GenericCheckBox("Configuration");
            this.writeConfigBox.setSelected(this.getUser().canSetConfiguration());
            this.writeConfigBox.setBackground(Color.WHITE);

            this.writeDataBox = new GenericCheckBox("Data");
            this.writeDataBox.setSelected(this.getUser().canSetData());
            this.writeDataBox.setBackground(Color.WHITE);

            this.writeMetaBox = new GenericCheckBox("Meta Data");
            this.writeMetaBox.setSelected(this.getUser().canSetMetaData());
            this.writeMetaBox.setBackground(Color.WHITE);

            this.deleteDataBox = new GenericCheckBox("Data");
            this.deleteDataBox.setSelected(this.getUser().canDeleteData());
            this.deleteDataBox.setBackground(Color.WHITE);

            this.deleteMetaBox = new GenericCheckBox("Meta Data");
            this.deleteMetaBox.setSelected(this.getUser().canDeleteMetaData());
            this.deleteMetaBox.setBackground(Color.WHITE);

            // Add label
            {
                JLabel nameLabel = new GenericLabel(this.name);
                nameLabel.setFont(Styles.FONT_14PT_BOLD);
                nameLabel.setForeground(Color.WHITE);
                nameLabel.setBackground(Color.BLACK);

                // Put label in a panel to style
                JPanel namePanel = new JPanel();
                namePanel.setLayout(new GridBagLayout());
                GridBagConstraints pc = new GridBagConstraints();

                pc.fill = GridBagConstraints.HORIZONTAL;
                pc.gridwidth = GridBagConstraints.RELATIVE;
                pc.weightx = 1;
                pc.weighty = 0;
                pc.insets = new Insets(5, 5, 5, 5);

                namePanel.setBackground(Color.BLACK);
                namePanel.add(nameLabel, pc);

                pc.gridwidth = GridBagConstraints.REMAINDER;
                pc.weightx = 0;

                // Build and add the remove button
                removeButton = new GenericButton("Remove");
                removeButton.setForeground(Color.WHITE);
                removeButton.setBackground(Color.BLACK);
                removeButton.setBorder(Styles.UNDERLINE_WHITE);
                removeButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        usersPanel.removeUser(user);
                    }
                });
                namePanel.add(removeButton, pc);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(0, 0, 0, 0);

                this.add(namePanel, gbc);
            }

            // Add read row
            {
                JLabel readLabel = new GenericLabel("Read: ");
                readLabel.setFont(Styles.FONT_12PT);
                readLabel.setForeground(Color.BLACK);
                readLabel.setBackground(Styles.COLOR_BACKGROUND_LIGHT);

                // Put label in a panel to style
                JPanel readPanel = new JPanel();
                readPanel.setLayout(new GridBagLayout());
                GridBagConstraints pc = new GridBagConstraints();

                pc.fill = GridBagConstraints.HORIZONTAL;
                pc.gridwidth = GridBagConstraints.REMAINDER;
                pc.weightx = 1;
                pc.weighty = 0;
                pc.insets = new Insets(5, 5, 5, 5);

                readPanel.setBackground(Styles.COLOR_BACKGROUND_LIGHT);
                readPanel.add(readLabel, pc);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 1;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(0, 0, 0, 0);

                this.add(readPanel, gbc);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(0, 0, 0, 0);

                this.add(this.readConfigBox, gbc);
            }

            // Add write row
            {
                JLabel writeLabel = new GenericLabel("Write: ");
                writeLabel.setFont(Styles.FONT_12PT);
                writeLabel.setBackground(Styles.COLOR_BACKGROUND);
                writeLabel.setForeground(Color.BLACK);

                // Put label in a panel to style
                JPanel writePanel = new JPanel();
                writePanel.setLayout(new GridBagLayout());
                GridBagConstraints pc = new GridBagConstraints();

                pc.fill = GridBagConstraints.HORIZONTAL;
                pc.gridwidth = GridBagConstraints.REMAINDER;
                pc.weightx = 1;
                pc.weighty = 0;
                pc.insets = new Insets(5, 5, 5, 5);

                writePanel.setBackground(Styles.COLOR_BACKGROUND);
                writePanel.add(writeLabel, pc);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 1;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(0, 0, 0, 0);

                this.add(writePanel, gbc);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 1;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(0, 0, 0, 0);

                writeConfigBox.setBackground(Styles.COLOR_BACKGROUND);
                this.add(this.writeConfigBox, gbc);
                writeDataBox.setBackground(Styles.COLOR_BACKGROUND);
                this.add(this.writeDataBox, gbc);

                gbc.gridwidth = GridBagConstraints.REMAINDER;

                writeMetaBox.setBackground(Styles.COLOR_BACKGROUND);
                this.add(this.writeMetaBox, gbc);
            }

            // Add delete row
            {
                JLabel deleteLabel = new GenericLabel("Delete: ");
                deleteLabel.setFont(Styles.FONT_12PT);
                deleteLabel.setBackground(Styles.COLOR_BACKGROUND_LIGHT);
                deleteLabel.setForeground(Color.BLACK);

                // Put label in a panel to style
                JPanel deletePanel = new JPanel();
                deletePanel.setLayout(new GridBagLayout());
                GridBagConstraints pc = new GridBagConstraints();

                pc.fill = GridBagConstraints.HORIZONTAL;
                pc.gridwidth = GridBagConstraints.REMAINDER;
                pc.weightx = 1;
                pc.weighty = 0;
                pc.insets = new Insets(5, 5, 5, 5);

                deletePanel.setBackground(Styles.COLOR_BACKGROUND_LIGHT);
                deletePanel.add(deleteLabel, pc);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 1;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(0, 0, 0, 0);

                this.add(deletePanel, gbc);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 1;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(0, 0, 0, 0);

                this.add(this.deleteDataBox, gbc);

                gbc.gridwidth = GridBagConstraints.REMAINDER;

                this.add(this.deleteMetaBox, gbc);
            }
        }

        /**
         * <p>Returns the current state of the single user panel. The map uses the byte code (e.g., READ_CONFIG) as a key, which is used to retrieve a boolean value stating what is currently set.)
         */
        public Map<Byte, Boolean> getPermissions() {

            Map<Byte, Boolean> state = new HashMap();

            state.put(READ_CONFIG, this.readConfigBox.isSelected());
            state.put(WRITE_CONFIG, this.writeConfigBox.isSelected());
            state.put(WRITE_DATA, this.writeDataBox.isSelected());
            state.put(WRITE_META, this.writeMetaBox.isSelected());
            state.put(DELETE_DATA, this.deleteDataBox.isSelected());
            state.put(DELETE_META, this.deleteMetaBox.isSelected());

            return state;
        }

        public User getUser() {
            return user;
        }
    }

    /**
     * Set hash spans.
     */
    private class HashSpansPanel extends JPanel {

        private List<EmbeddedHashSpanPanel> hashSpanPanels = new ArrayList<EmbeddedHashSpanPanel>();
        private JButton addHashSpanButton = new GenericButton("Add Hash Span");
        private GridBagConstraints gbc = new GridBagConstraints();

        public HashSpansPanel() {
            setBackground(Styles.COLOR_BACKGROUND);
            setLayout(new GridBagLayout());

            // Add hash span button
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.insets = new Insets(5, 5, 0, 5);
            add(addHashSpanButton, gbc);

            // Add a strut to align items to top
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.insets = new Insets(0, 0, 0, 0);
            add(Box.createVerticalStrut(1), gbc);

            // Register listeners
            addHashSpanButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    // Create a panel with default hash span
                    AbstractHashSpan defaultSpan = new AbstractHashSpan(AbstractHashSpan.ABSTRACTION_FIRST, AbstractHashSpan.ABSTRACTION_LAST);
                    EmbeddedHashSpanPanel panel = new EmbeddedHashSpanPanel(HashSpansPanel.this, defaultSpan, hashSpanPanels.size());

                    // Add to list of panels
                    hashSpanPanels.add(panel);

                    // Add to hash spans panel
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    gbc.gridwidth = GridBagConstraints.REMAINDER;
                    gbc.weightx = 1;
                    gbc.weighty = 0;
                    gbc.insets = new Insets(5, 5, 0, 5);

                    // Set the index so everything appears before strut
                    add(panel, gbc, 1 + hashSpanPanels.indexOf(panel));
                    HashSpansPanel.this.validate();
                    ServerConfigurationPanel.this.repaint();
                }
            });
        }

        /**
         * <p>Called when server started</p>
         */
        public void build(boolean isTargetHashSpans) {
            if (isTargetHashSpans) {
                for (HashSpan nextSpan : configuration.getTargetHashSpans()) {
                    EmbeddedHashSpanPanel nextPanel = new EmbeddedHashSpanPanel(this, new AbstractHashSpan(nextSpan), hashSpanPanels.size());

                    // Add to list of panels
                    hashSpanPanels.add(nextPanel);

                    // Add to hash spans panel
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    gbc.gridwidth = GridBagConstraints.REMAINDER;
                    gbc.weightx = 1;
                    gbc.weighty = 0;
                    gbc.insets = new Insets(5, 5, 0, 5);

                    // Set the index so everything appears before strut
                    add(nextPanel, gbc, 1 + hashSpanPanels.indexOf(nextPanel));
                }
            } else {
                for (HashSpan nextSpan : configuration.getHashSpans()) {
                    EmbeddedHashSpanPanel nextPanel = new EmbeddedHashSpanPanel(this, new AbstractHashSpan(nextSpan), hashSpanPanels.size());

                    // Add to list of panels
                    hashSpanPanels.add(nextPanel);

                    // Add to hash spans panel
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    gbc.gridwidth = GridBagConstraints.REMAINDER;
                    gbc.weightx = 1;
                    gbc.weighty = 0;
                    gbc.insets = new Insets(5, 5, 0, 5);

                    // Set the index so everything appears before strut
                    add(nextPanel, gbc, 1 + hashSpanPanels.indexOf(nextPanel));
                }
            }
        }

        /**
         * <p>Called when server stopped.</p>
         */
        public void tearDown() {
            for (EmbeddedHashSpanPanel p : hashSpanPanels) {
                remove(p);
            }
            hashSpanPanels.clear();
            validate();
            ServerConfigurationPanel.this.repaint();
        }

        /**
         * <p>Removes hash span with certain index.</p>
         */
        public void removeHashSpan(EmbeddedHashSpanPanel panel) {
            hashSpanPanels.remove(panel);
            remove(panel);
            validate();
            ServerConfigurationPanel.this.repaint();
        }

        /**
         * <p>Returns all hash spans in panel's current state.</p>
         */
        public Set<HashSpan> getHashSpans() {
            Set<HashSpan> spans = new HashSet();
            for (EmbeddedHashSpanPanel panel : hashSpanPanels) {
                spans.add(panel.getHashSpan());
            }
            return spans;
        }
    }

    /**
     * One for each hash span in HashSpansPanel
     */
    private class EmbeddedHashSpanPanel extends JPanel {

        private AbstractHashSpan span;
        private int index;
        private JPanel panel;
        private JButton removeHashSpanButton;
        private JTextField firstBigHashField,  secondBigHashField;

        public EmbeddedHashSpanPanel(final HashSpansPanel panel, AbstractHashSpan span, final int index) {

            this.span = span;
            this.index = index;
            this.panel = panel;

            this.setBackground(Color.WHITE);
            this.setBorder(Styles.BORDER_BLACK_1);

            // Set the layout
            this.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            // Add span label and "Remove" button
            {
                // Use a panel so can style
                JPanel headerPanel = new JPanel();
                headerPanel.setBackground(Color.BLACK);
                headerPanel.setLayout(new GridBagLayout());
                GridBagConstraints pc = new GridBagConstraints();

                // Build and add the header label
                JLabel hashSpanLabel = new GenericLabel("Hash Span #" + index);
                hashSpanLabel.setFont(Styles.FONT_12PT_BOLD);
                hashSpanLabel.setForeground(Color.WHITE);

                pc.fill = GridBagConstraints.HORIZONTAL;
                pc.gridwidth = 1;
                pc.weightx = 1.0;
                pc.weighty = 0;
                pc.insets = new Insets(5, 5, 2, 0);
                headerPanel.add(hashSpanLabel, pc);

                // Build and add the remove button
                this.removeHashSpanButton = new GenericButton("Remove");
                this.removeHashSpanButton.setForeground(Color.WHITE);
                this.removeHashSpanButton.setBackground(Color.BLACK);
                this.removeHashSpanButton.setBorder(Styles.UNDERLINE_WHITE);

                pc.fill = GridBagConstraints.HORIZONTAL;
                pc.gridwidth = GridBagConstraints.REMAINDER;
                pc.weightx = 0;
                pc.weighty = 0;
                pc.insets = new Insets(5, 0, 5, 5);
                headerPanel.add(this.removeHashSpanButton, pc);

                // Add the panel
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(0, 0, 0, 0);

                this.add(headerPanel, gbc);
            }

            // Add first span text field
            {
                this.firstBigHashField = new GenericTextField(String.valueOf(this.span.getAbstractionFirst()));
                this.firstBigHashField.setBackground(Styles.COLOR_BACKGROUND);
                this.firstBigHashField.setFont(Styles.FONT_10PT);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(5, 5, 0, 5);

                this.add(this.firstBigHashField, gbc);
            }

            // Add second span text field
            {
                this.secondBigHashField = new GenericTextField(String.valueOf(this.span.getAbstractionLast()));
                this.secondBigHashField.setBackground(Styles.COLOR_BACKGROUND);
                this.secondBigHashField.setFont(Styles.FONT_10PT);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(5, 5, 5, 5);

                this.add(this.secondBigHashField, gbc);
            }

            // Set listener for remove
            this.removeHashSpanButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    panel.removeHashSpan(EmbeddedHashSpanPanel.this);
                }
            });
        }

        public HashSpan getHashSpan() {
            return new AbstractHashSpan(Integer.valueOf(this.firstBigHashField.getText()), Integer.valueOf(this.secondBigHashField.getText()));
        }
    }

    /**
     * Data directory information.
     */
    private class DataDirectoriesPanel extends JPanel {

        private List<EmbeddedDataDirectoryPanel> directoryPanels = new ArrayList<EmbeddedDataDirectoryPanel>();
        private JButton addDirectoryButton = new GenericButton("Add Data Directory");
        private GridBagConstraints gbc = new GridBagConstraints();

        public DataDirectoriesPanel() {
            setBackground(Styles.COLOR_BACKGROUND);
            setLayout(new GridBagLayout());

            {
                gbc.anchor = GridBagConstraints.NORTH;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(5, 5, 0, 5);

                add(addDirectoryButton, gbc);
            }

            // Add a strut to align items to top
            {
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weightx = 1;
                gbc.weighty = 1;
                gbc.insets = new Insets(0, 0, 0, 0);

                add(Box.createVerticalStrut(1), gbc);
            }

            // Register listener for button to add directory
            addDirectoryButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    // Create an embedded panel
                    DataDirectoryConfiguration dconfig = new DataDirectoryConfiguration("/Set_Path_For_New_Directory", 0);
                    EmbeddedDataDirectoryPanel panel = new EmbeddedDataDirectoryPanel(DataDirectoriesPanel.this, dconfig, directoryPanels.size());

                    // Add to list of panels
                    directoryPanels.add(panel);

                    // Add to hash spans panel
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    gbc.gridwidth = GridBagConstraints.REMAINDER;
                    gbc.weightx = 1;
                    gbc.weighty = 0;
                    gbc.insets = new Insets(5, 5, 0, 5);

                    DataDirectoriesPanel.this.add(panel, gbc, 1 + directoryPanels.indexOf(panel));
                    DataDirectoriesPanel.this.validate();
                    ServerConfigurationPanel.this.repaint();
                }
            });
        }

        /**
         * <p>Called when server started</p>
         */
        public void build() {
            for (DataDirectoryConfiguration dconfig : configuration.getDataDirectories()) {

                EmbeddedDataDirectoryPanel panel = new EmbeddedDataDirectoryPanel(this, dconfig, directoryPanels.size());

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1.0;
                gbc.weighty = 0;
                gbc.insets = new Insets(5, 5, 0, 5);

                directoryPanels.add(panel);
                add(panel, gbc, 1 + directoryPanels.indexOf(panel));
            }

            validate();
            ServerConfigurationPanel.this.repaint();
        }

        /**
         * <p>Called when server stopped.</p>
         */
        public void tearDown() {
            for (EmbeddedDataDirectoryPanel p : directoryPanels) {
                remove(p);
            }
            directoryPanels.clear();
            validate();
            ServerConfigurationPanel.this.repaint();
        }

        /**
         * <p>Removes an embedded data directory panel</p>
         */
        public void removeDirectory(EmbeddedDataDirectoryPanel panel) {
            directoryPanels.remove(panel);
            remove(panel);
            validate();
            ServerConfigurationPanel.this.repaint();
        }

        /**
         * <p>Returns set of data directory configuration.</p>
         */
        public Set<DataDirectoryConfiguration> getDataDirectoryConfigurations() {
            Set<DataDirectoryConfiguration> dconfigs = new HashSet();
            for (EmbeddedDataDirectoryPanel p : directoryPanels) {
                dconfigs.add(p.getDataDirectoryConfiguration());
            }
            return dconfigs;
        }
    }

    /**
     * Represents one data directory within the DataDirectoriesPanel
     */
    private class EmbeddedDataDirectoryPanel extends JPanel {

        private DataDirectoriesPanel panel;
        private DataDirectoryConfiguration config;
        private JTextField directoryField,  sizeField;
        private JButton removeButton;
        private int index;

        public EmbeddedDataDirectoryPanel(final DataDirectoriesPanel panel, DataDirectoryConfiguration config, int index) {

            this.panel = panel;
            this.config = config;
            this.index = index;

            setBackground(Color.WHITE);
            setBorder(Styles.BORDER_BLACK_1);

            // Set the layout
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            // Create header label and remove button
            {
                // Use a panel so can style
                JPanel headerPanel = new JPanel();
                headerPanel.setBackground(Color.BLACK);
                headerPanel.setLayout(new GridBagLayout());
                GridBagConstraints pc = new GridBagConstraints();

                // Build and add the header label
                JLabel hashSpanLabel = new GenericLabel("Directory #" + index);
                hashSpanLabel.setFont(Styles.FONT_12PT_BOLD);
                hashSpanLabel.setForeground(Color.WHITE);

                pc.fill = GridBagConstraints.HORIZONTAL;
                pc.gridwidth = 1;
                pc.weightx = 1.0;
                pc.weighty = 0;
                pc.insets = new Insets(5, 5, 2, 0);
                headerPanel.add(hashSpanLabel, pc);

                // Build and add the remove button
                this.removeButton = new GenericButton("Remove");
                this.removeButton.setForeground(Color.WHITE);
                this.removeButton.setBackground(Color.BLACK);
                this.removeButton.setBorder(Styles.UNDERLINE_WHITE);

                pc.fill = GridBagConstraints.HORIZONTAL;
                pc.gridwidth = GridBagConstraints.REMAINDER;
                pc.weightx = 0;
                pc.weighty = 0;
                pc.insets = new Insets(5, 0, 5, 5);
                headerPanel.add(this.removeButton, pc);

                // Add the panel
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(0, 0, 0, 0);

                add(headerPanel, gbc);
            }

            // Directory location
            {
                this.directoryField = new GenericTextField();
                this.directoryField.setText(this.config.getDirectory());
                this.directoryField.setBackground(Styles.COLOR_BACKGROUND);
                this.directoryField.setFont(Styles.FONT_11PT);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(5, 5, 0, 5);

                add(this.directoryField, gbc);
            }

            // Size
            {
                this.sizeField = new GenericTextField();
                this.sizeField.setText("" + this.config.getSizeLimit());
                this.sizeField.setBackground(Styles.COLOR_BACKGROUND);
                this.sizeField.setFont(Styles.FONT_11PT);

                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.insets = new Insets(5, 5, 5, 5);

                this.add(this.sizeField, gbc);
            }

            // Register remove button listener
            this.removeButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    panel.removeDirectory(EmbeddedDataDirectoryPanel.this);
                }
            });
        }

        /**
         * <p>Returns data directory configuration based on current state of panel.</p>
         */
        public DataDirectoryConfiguration getDataDirectoryConfiguration() {
            String directory = directoryField.getText().trim();
            long size = Long.parseLong(sizeField.getText().trim());
            return new DataDirectoryConfiguration(directory, size);
        }
    }
    /**
     * Only one instance allowed at a time. Updates the server's configuration every
     * few seconds.
     */
    private static Thread updateAttributesThread = null;
    /**
     * Want to show user a warning ONCE per instance to turn off auto update
     * features if editting.
     */
    private static boolean isShownUserAutoUpdateWarning = false;

    /**
     * Panel that shows key/value pairs in server's configuration. Can add, edit or remove.
     * @author Bryan Smith <bryanesmith at gmail.com>
     */
    private class AttributesPanel extends JPanel {

        private Configuration config;
        private AttributesTable table;
        private final GenericCheckBox autoUpdateCheckBox;
        private final JComboBox autoUpdateComboBox;
        private final String[] autoUpdateComboBoxOptions = {
            "Update every 5 seconds", "Update every 10 seconds", "Update every 30 seconds", "Update every 1 minute",//            "Update every 15 minutes", "Update every 1 hour" // <- If set then change, will take long time to update!
        };
        /**
         * 
         */
        private final boolean[] runDaemonThreads = {true};

        public AttributesPanel() {

            setLayout(new GridBagLayout());

            this.autoUpdateCheckBox = new GenericCheckBox("Periodically update the values in this table?");
            this.autoUpdateCheckBox.setToolTipText("If selected, you can choose how often the tool will update values in Attributes table.");
            this.autoUpdateCheckBox.setSelected(false);

            this.autoUpdateComboBox = new JComboBox(autoUpdateComboBoxOptions);
            this.autoUpdateComboBox.setSelectedIndex(1);
            this.autoUpdateComboBox.setVisible(this.autoUpdateCheckBox.isSelected());

            // Only show options if selected to show
            this.autoUpdateCheckBox.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread("Put description here") {

                        @Override()
                        public void run() {
                            autoUpdateComboBox.setVisible(autoUpdateCheckBox.isSelected());
                        }
                    };
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY);
                    SwingUtilities.invokeLater(t);

                    if (!isShownUserAutoUpdateWarning) {
                        GenericOptionPane.showMessageDialog(
                                getParent(),
                                "You will want to turn off periodic updates when editting attributes, since it could interfer with your changes.\n\nThis message will not be shown again while running this instance of the Tranche tool.",
                                "Turn off periodic updates when editting",
                                JOptionPane.INFORMATION_MESSAGE);
                        isShownUserAutoUpdateWarning = true;
                    }
                }
            });

            // Create the menu
            JMenuBar attributesMenuBar = new JMenuBar();
            JMenu attributesMenu = new JMenu("Attributes Menu");
            final JMenuItem addAttributeMenuItem = new JMenuItem("Add Attribute");
            final JMenuItem editAttributeMenuItem = new JMenuItem("Edit Attribute");

            editAttributeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override()
                        public void run() {
                            editSelectedAttribute();
                        }
                    };
                    t.start();
                }
            });

            addAttributeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override()
                        public void run() {
                            try {
                                // Popup frame to add name/value
                                AddAttributeFrame frame = new AddAttributeFrame(AttributesPanel.this);
                                frame.setVisible(true);
                            } catch (Exception ee) {
                                ErrorFrame ef = new ErrorFrame();
                                ef.show(ee, AttributesPanel.this.getParent());
                            }
                        }
                    };
                    t.start();
                }
            });

            final JMenuItem removeAttributesMenuItem = new JMenuItem("Remove Selected Attributes");
            removeAttributesMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        removeSelectedAttributes();
                    } catch (Exception ee) {
                        ErrorFrame ef = new ErrorFrame();
                        ef.show(ee, AttributesPanel.this);
                    }
                }
            });

            attributesMenu.add(addAttributeMenuItem);
            attributesMenu.add(editAttributeMenuItem);
            attributesMenu.add(removeAttributesMenuItem);

            attributesMenu.addMenuListener(new MenuListener() {

                public void menuCanceled(MenuEvent e) {
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuSelected(MenuEvent e) {
                    Thread t = new Thread() {

                        @Override()
                        public void run() {
                            addAttributeMenuItem.setEnabled(true);

                            int selectionCount = attributesPanel.table.getSelectedRowCount();

                            if (selectionCount == 1) {

                                // Doesn't have to be edittable: open in read only
                                // so user can read
                                editAttributeMenuItem.setEnabled(true);

                                // Only delete if edittable
                                removeAttributesMenuItem.setEnabled(attributesPanel.table.isSelectedDeletable());
                            } else if (selectionCount > 1) {
                                editAttributeMenuItem.setEnabled(false);

                                // Only delete if edittable
                                removeAttributesMenuItem.setEnabled(attributesPanel.table.isSelectedDeletable());
                            } else {
                                // Nothing selected
                                editAttributeMenuItem.setEnabled(false);
                                removeAttributesMenuItem.setEnabled(false);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });

            attributesMenuBar.add(attributesMenu);

            int row = 0;

            // Add the constraints for the table
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, 0, 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(attributesMenuBar, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(6, 0, 0, 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.autoUpdateCheckBox, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(6, 10, 10, 10);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.autoUpdateComboBox, gbc);

            /*
             * Add button to launch browser to view page on these attributes
             */
            JButton launchBrowserHelpButton = new JButton("Server configuration attributes help...");
            launchBrowserHelpButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    GUIUtil.displayURL("http://tranche.proteomecommons.org/users/server-configuration-options.html");
                }
            });

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(6, 10, 10, 10);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(launchBrowserHelpButton, gbc);

            /*
             * Add table
             */
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.table = new AttributesTable();
            this.table.setBorder(Styles.BORDER_BLACK_1);

            // Wrap in container. Need header.
            JPanel tableContainer = new JPanel();
            tableContainer.setLayout(new BorderLayout());
            tableContainer.add(table.getTableHeader(), BorderLayout.PAGE_START);
            tableContainer.add(table, BorderLayout.CENTER);

            this.add(tableContainer, gbc);

            // Create a popup menu
            AttributesPopupMenu popup = new AttributesPopupMenu(AttributesPanel.this);
            GenericPopupListener popupListener = new GenericPopupListener(popup);
            popupListener.setDisplayMethod(GenericPopupListener.RIGHT_CLICK_ONLY);
            table.addMouseListener(popupListener);

            // Add a strut to align items to top
            {
                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 1;
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = row++; // <-- Row
                gbc.insets = new Insets(0, 0, 0, 10);

                this.add(Box.createVerticalStrut(1), gbc);
            }
        }

        public boolean isAutoUpdating() {
            return this.autoUpdateCheckBox.isSelected();
        }

        /**
         * <p>Edit a single selected attribute's name and/or value.</p>
         */
        protected void editSelectedAttribute() {
            try {
                String name = table.getSelectedAttributeName();
                String value = table.getSelectedAttributeValue();

                if (name == null) {
                    GenericOptionPane.showMessageDialog(
                            AttributesPanel.this.getParent(),
                            "You must select an attribute row in the table first.",
                            "Please select a row",
                            JOptionPane.INFORMATION_MESSAGE);
                }

                // Popup frame to add name/value
                AddAttributeFrame frame = new AddAttributeFrame(AttributesPanel.this, name, value, table.isSelectedEdittable());
                frame.setVisible(true);
            } catch (Exception ee) {
                ErrorFrame ef = new ErrorFrame();
                ef.show(ee, AttributesPanel.this.getParent());
            }
        }

        /**
         * @return Time is millis to pause between updates, as selected by user in drop down.
         */
        protected long getMillisForSelectedAutoUpdate() {

            final long SECOND = 1000,  MINUTE = 60 * SECOND,  HOUR = 60 * MINUTE;

            String s = this.autoUpdateComboBox.getSelectedItem().toString();

            if (s.trim().equals("Update every 5 seconds")) {
                return 5 * SECOND;
            } else if (s.trim().equals("Update every 10 seconds")) {
                return 10 * SECOND;
            } else if (s.trim().equals("Update every 30 seconds")) {
                return 30 * SECOND;
            } else if (s.trim().equals("Update every 1 minute")) {
                return MINUTE;
            } else if (s.trim().equals("Update every 15 minutes")) {
                return 15 * MINUTE;
            } else if (s.trim().equals("Update every 1 hour")) {
                return HOUR;
            }

            return 10 * SECOND;
        }

        /**
         * <p>Add a key/value pair.</p>
         */
        public int addAttribute(String name, String value) {

            // If not readable, just don't add. Could use a better method
            // behind the scenes, but thisname is good enough for now.
            if (!ConfigKeys.isReadable(name)) {
                return -1;
            }

            // Update table
            int row = this.table.addAttribute(name, value);

            // Update configuration
            this.config.setValue(name, value);

            return row;
        }

        public boolean containsAttribute(String name) {
            return this.table.containsAttribute(name);
        }

        /**
         * <p>Removes all selected attributes in the table.</p>
         */
        public void removeSelectedAttributes() {

            // List of keys to remove
            List<String> removedKeys = this.table.getSelectedNames();

            // Remove keys
            for (String nextKey : removedKeys) {
                this.config.removeKeyValuePair(nextKey);
            }

            // Update table
            table.removeSelectedRows();
        }

        /**
         * Returns map of key and value pairs.
         */
        public Map<String, String> getNameValuePairs() {
            Map<String, String> map = new HashMap();

            for (String key : this.config.getValueKeys()) {
                map.put(key, this.config.getValue(key));
            }

            return map;
        }

        /**
         * <p>A hook for shutdown to cloes down resources.</p>
         */
        public void preventDaemonThreads() {
            runDaemonThreads[0] = false;
        }

        public void resumeDaemonThreads() {
            runDaemonThreads[0] = true;
        }

        /**
         * <p>Called when server started</p>
         */
        public void build() {
            setConfig(configuration);

            if (updateAttributesThread != null) {
                updateAttributesThread.interrupt();
            }

            final Configuration c = this.getConfig();
            final AttributesTable t = this.table;
            final AttributesPanel ap = AttributesPanel.this;

            updateAttributesThread = new Thread("Update server's configuration attributes thread") {

                @Override()
                public void run() {
                    while (runDaemonThreads[0]) {
                        try {

                            long pause = ap.getMillisForSelectedAutoUpdate();

                            // If selected to not auto update, pause and try again
                            if (!isAutoUpdating()) {
                                Thread.sleep(pause);
                                continue;
                            }

                            UserZipFile uzf = GUIUtil.getUser();

                            // No, table is difficult to deselect, confusing...
//                            // If the table has any selected rows, don't do anything.
//                            // User is editting.
//                            if (t.getSelectedRow() != -1) {
//                                Thread.sleep(pause);
//                                continue;
//                            }

                            X509Certificate cert = null;
                            PrivateKey key = null;

                            // Skip iteration if problems getting user
                            if (uzf == null) {
                                cert = SecurityUtil.getAnonymousCertificate();
                                key = SecurityUtil.getAnonymousKey();
                            } else {
                                cert = uzf.getCertificate();
                                key = uzf.getPrivateKey();
                            }

                            // Get server host. If can't, skip loop
                            String url = c.getValue("serverURL");

                            if (url == null) {
                                Thread.sleep(pause);
                                continue;
                            }

                            String host = IOUtil.parseHost(url);

                            TrancheServer ts = ConnectionUtil.connectHost(host, true);
                            if (ts == null) {
                                throw new Exception("Could not connect with " + url);
                            }
                            try {
                                Configuration newConfig = IOUtil.getConfiguration(ts, cert, key);

                                if (newConfig == null) {
                                    throw new Exception("Configuration was null, should have thrown an exception.");
                                }
                                ap.setConfig(newConfig);
                            } finally {
                                ConnectionUtil.unlockConnection(host);
                            }

                            Thread.sleep(pause);
                        } catch (InterruptedException ie) {
                            // Expected, stop
                            return;
                        } catch (Exception ex) {
                            // Skip, not critical
                            System.err.println(ex.getClass().getSimpleName() + " while updating: " + ex.getMessage());
                            ex.printStackTrace(System.err);
                        }
                    }
                }
            };
            updateAttributesThread.setDaemon(true);
            updateAttributesThread.setPriority(Thread.MIN_PRIORITY);
            updateAttributesThread.start();
        }

        /**
         * <p>Called when server stopped.</p>
         */
        public void tearDown() {
        }

        /**
         * <p>Table that holds attributes.</p>
         */
        private class AttributesTable extends GenericTable {

            AttributesTableModel model;

            public AttributesTable() {

                // Set the table mode
                super(new AttributesTableModel(), ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                this.model = (AttributesTableModel) super.getModel();

                super.setBackground(Styles.COLOR_BACKGROUND);
                super.setGridColor(Color.BLACK);

                try {
                    this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                    this.getColumnModel().getColumn(2).setPreferredWidth(150);
                    this.getColumnModel().getColumn(2).setMaxWidth(150);
                } catch (Exception ex) { /* nope, move on */ }
            }

            /**
             * 
             * @param name
             * @param value
             * @return The row index
             */
            public int addAttribute(String name, String value) {

                // If model contains a value already, remove. This represents
                // a set.
                this.model.removeAttribute(name);

                return this.model.addAttribute(name, value);
            }

            public boolean containsAttribute(String name) {
                return this.model.containsAttribute(name);
            }

            public String getSelectedAttributeName() {
                int selectedRow = this.getSelectedRow();
                return this.model.getSelectedAttributeName(selectedRow);
            }

            public String getSelectedAttributeValue() {
                int selectedRow = this.getSelectedRow();
                return this.model.getSelectedAttributeValue(selectedRow);
            }

            public void clear() {
                this.model.clear();
            }

            /**
             * <p>Returns all the keys that are selected.</p>
             */
            public List<String> getSelectedNames() {

                List<String> selectedKeys = new ArrayList();

                for (int nextRow : this.getSelectedRows()) {
                    selectedKeys.add(this.model.getRow(nextRow).getName());
                }

                return selectedKeys;
            }

            /**
             * 
             * @return
             */
            public AttributesTableModel getAttributesTableModel() {
                return this.model;
            }

            /**
             * <p>Returns true only if every (1 or more) row contains edittable attributes.</p>
             * @return
             */
            public boolean isSelectedEdittable() {
                for (int nextRow : this.getSelectedRows()) {
                    if (!this.model.getRow(nextRow).isEdittable()) {
                        return false;
                    }
                }

                return true;
            }

            /**
             * <p>Returns true only if every (1 or more) row contains deletable attributes.</p>
             * @return
             */
            public boolean isSelectedDeletable() {
                for (int nextRow : this.getSelectedRows()) {
                    if (!this.model.getRow(nextRow).isDeletable()) {
                        return false;
                    }
                }

                return true;
            }

            /**
             * <p>Removes all selected rows from the table.</p>
             */
            public void removeSelectedRows() {

                for (String next : this.getSelectedNames()) {
                    this.model.removeAttribute(next);
                }
            }
        }
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        /**
         * <p>Handles data shown it table.</p>
         */
        private class AttributesTableModel extends SortableTableModel {

            private String[] columnNames = {"Name", "Value", "Permissions"};
            private final List<AttributeTableRow> data = new ArrayList();

            /**
             * 
             * @return
             */
            public int getColumnCount() {
                return columnNames.length;
            }

            /**
             * 
             * @return
             */
            public int getRowCount() {
                synchronized (data) {
                    return data.size();
                }
            }

            /**
             * 
             * @param col
             * @return
             */
            @Override()
            public String getColumnName(int col) {
                synchronized (data) {
                    return columnNames[col];
                }
            }

            /**
             * 
             * @param row
             * @param col
             * @return
             */
            public Object getValueAt(int row, int col) {
                synchronized (data) {
                    // Avoid IndexOutOfBoundsException during redraws, sporadic
                    if (row > this.data.size()) {
                        return null;
                    }

                    AttributeTableRow attributeRow = this.data.get(row);

                    switch (col) {

                        case 0:
                            return attributeRow.getName();
                        case 1:
                            return attributeRow.getValue();
                        case 2:
                            StringBuffer buffer = new StringBuffer();
                            int attributeCount = 0;
                            if (attributeRow.isReadable()) {
                                if (buffer.length() > 0) {
                                    buffer.append(", ");
                                }
                                buffer.append("Read");
                                attributeCount++;
                            }
                            if (attributeRow.isEdittable()) {
                                if (buffer.length() > 0) {
                                    buffer.append(", ");
                                }
                                buffer.append("Edit");
                                attributeCount++;
                            }
                            if (attributeRow.isDeletable()) {
                                if (buffer.length() > 0) {
                                    buffer.append(", ");
                                }
                                buffer.append("Delete");
                                attributeCount++;
                            }

                            // If no permissions, say so. This should be showing anyhow!
                            if (attributeCount == 0) {
                                buffer.append("[No Permissions]");
                            }
                            // If only 1, say "____ only" so more readable
                            if (attributeCount == 1) {
                                buffer.append(" Only");
                            }

                            return buffer.toString();
                    }
                }
                throw new RuntimeException("Shouldn't get here.");
            }

            /**
             * 
             * @param c
             * @return
             */
            @Override()
            public Class getColumnClass(int c) {
                try {
                    return getValueAt(0, c).getClass();
                } catch (Exception e) {
                    return String.class;
                }
            }

            /**
             * 
             * @param name
             * @param value
             * @return The row index for newly-added item.
             */
            public int addAttribute(String name, String value) {
                synchronized (data) {
                    AttributeTableRow attributeRow = new AttributeTableRow(name, value);
                    this.data.add(attributeRow);
                    this.fireTableDataChanged();

                    return this.data.size() - 1;
                }
            }

            /**
             * 
             */
            public void clear() {
                synchronized (data) {
                    this.data.clear();
                    this.fireTableDataChanged();
                }
            }

            /**
             * 
             * @param row
             * @return
             */
            public AttributeTableRow getRow(int row) {
                synchronized (data) {
                    return this.data.get(row);
                }
            }

            /**
             * 
             * @param row
             * @return
             */
            public String getSelectedAttributeName(int row) {
                synchronized (data) {
                    if (row >= 0 && row < this.getRowCount()) {
                        return this.data.get(row).getName();
                    }

                    return null;
                }
            }

            /**
             * 
             * @param row
             * @return
             */
            public String getSelectedAttributeValue(int row) {

                synchronized (data) {
                    if (row >= 0 && row < this.getRowCount()) {
                        return this.data.get(row).getValue();
                    }

                    return null;
                }
            }

            /**
             * 
             * @param name
             * @return
             */
            public boolean containsAttribute(String name) {

                synchronized (data) {
                    for (int i = 0; i < this.getRowCount(); i++) {

                        if (this.data.get(i).getName().equals(name)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            /**
             * 
             * @param name
             */
            public void removeAttribute(String name) {

                synchronized (data) {
                    for (int i = 0; i < this.getRowCount(); i++) {

                        if (this.data.get(i).getName().equals(name)) {
                            this.data.remove(i);
                            break;
                        }
                    }
                }

                this.fireTableDataChanged();
            }

            /**
             * 
             * @param i
             */
            public void removeAttribute(int i) {
                synchronized (data) {
                    this.data.remove(i);
                }
                this.fireTableDataChanged();
            }
            /**
             * The last column preseed.
             */
            private int lastColumn = 0;
            /**
             * Was the last ordering reversed?
             */
            private boolean wasLastDown = false;

            /**
             * <p>Get the last column pressed (for reordering)
             * @return
             */
            public int getLastColumnPressed() {
                return lastColumn;
            }

            /**
             * <p>Sort the table based on the pressed column.</p>
             * @param column The column index.
             */
            @Override()
            public void sort(int column) {
                sort(column, true);
            }

            /**
             * <p>Sort the table based on the pressed column.</p>
             * @param column The column index.
             * @param checkToReverseOrder If true, check to see whether should change ordering. That is the normal behavior. False means that this isn't a real key press and used the last (as in, auto-updated, so keep same ordering.)
             */
            public void sort(final int column, final boolean checkToReverseOrder) {
                // First determine if this is supposed to reverse order
                boolean isReverseOrder = wasLastDown;

                if (checkToReverseOrder) {
                    if (lastColumn == column && !wasLastDown) {
                        isReverseOrder = true;
                    } else {
                        isReverseOrder = false;
                    }

                    // Update values so works next time
                    wasLastDown = isReverseOrder;
                    lastColumn = column;
                }

                /*
                 * Sort by: Name (key)
                 */
                if (column == 0) {
                    List<String> keys = new ArrayList();

                    synchronized (data) {
                        // Grab all the keys (names)
                        for (AttributeTableRow row : this.data) {
                            keys.add(row.getName());
                        }
                        Collections.sort(keys);

                        // If pushed a second time, reverse order
                        if (isReverseOrder) {
                            Collections.reverse(keys);
                        }

                        // Going to sort. Create necessary collections.
                        Iterator<String> keyIterator = keys.iterator();
                        List<AttributeTableRow> attributes = new ArrayList();
                        attributes.addAll(this.data);

                        // Clear out all data already in model
                        this.clear();

                        // Add back, one at a time
                        SORTED_KEYS:
                        while (keyIterator.hasNext()) {
                            String key = keyIterator.next();
                            keyIterator.remove();

                            Iterator<AttributeTableRow> attributesIterator = attributes.iterator();

                            REMAINING_ATTRIBUTES:
                            while (attributesIterator.hasNext()) {
                                AttributeTableRow attribute = attributesIterator.next();

                                if (attribute.getName().equals(key)) {
                                    this.addAttribute(attribute.getName(), attribute.getValue());
                                    attributesIterator.remove();
                                    break REMAINING_ATTRIBUTES;
                                }
                            }
                        }
                    }
                } /*
                 * Sort by: value
                 */ else if (column == 1) {
                    List<String> values = new ArrayList();

                    synchronized (data) {
                        // Grab all the values and sort
                        for (AttributeTableRow row : this.data) {
                            values.add(row.getValue());
                        }
                        Collections.sort(values);

                        // If pushed a second time, reverse order
                        if (isReverseOrder) {
                            Collections.reverse(values);
                        }

                        // Going to sort. Create necessary collections.
                        Iterator<String> valueIterator = values.iterator();
                        List<AttributeTableRow> attributes = new ArrayList();
                        attributes.addAll(this.data);

                        // Clear out all data already in model
                        this.clear();

                        // Add back, one at a time
                        SORTED_VALUES:
                        while (valueIterator.hasNext()) {
                            String value = valueIterator.next();
                            valueIterator.remove();

                            Iterator<AttributeTableRow> attributesIterator = attributes.iterator();

                            REMAINING_ATTRIBUTES:
                            while (attributesIterator.hasNext()) {
                                AttributeTableRow attribute = attributesIterator.next();

                                if (attribute.getValue().equals(value)) {
                                    this.addAttribute(attribute.getName(), attribute.getValue());
                                    attributesIterator.remove();
                                    break REMAINING_ATTRIBUTES;
                                }
                            }
                        }
                    }
                } /*
                 * Sort by: is edittable
                 */ else if (column == 2) {
                    synchronized (data) {
                        List<AttributeTableRow> rows = new ArrayList();
                        rows.addAll(this.data);

                        this.clear();

                        Iterator<AttributeTableRow> rowIterator = rows.iterator();
                        List<AttributeTableRow> remaining = new ArrayList();

                        // If reverse, we'll store edittable items as remaining
                        // and add unedittable items first. If not reverse, opposite.
                        while (rowIterator.hasNext()) {
                            AttributeTableRow row = rowIterator.next();
                            rowIterator.remove();
                            if (row.isEdittable()) {
                                if (isReverseOrder) {
                                    remaining.add(row);
                                } else {
                                    this.addAttribute(row.getName(), row.getValue());
                                }
                            } else {
                                if (!isReverseOrder) {
                                    remaining.add(row);
                                } else {
                                    this.addAttribute(row.getName(), row.getValue());
                                }
                            }
                        }

                        // Add remaining
                        for (AttributeTableRow row : remaining) {
                            this.addAttribute(row.getName(), row.getValue());
                        }
                    }
                }
                this.fireTableDataChanged();
            } // sort(int)
        } // AttributesTableModel

        /**
         * Abstracts a row for the table model.
         */
        private class AttributeTableRow {

            private String name,  value;
            private boolean edittable,  readable,  deletable;

            AttributeTableRow(String name, String value) {
                this.name = name;
                this.value = value;
                this.edittable = ConfigKeys.isEditable(name);
                this.readable = ConfigKeys.isReadable(name);
                this.deletable = ConfigKeys.isDeletable(name);
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
                this.edittable = ConfigKeys.isEditable(name);
                this.readable = ConfigKeys.isReadable(name);
                this.deletable = ConfigKeys.isDeletable(name);
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

            public boolean isEdittable() {
                return edittable;
            }

            public boolean isReadable() {
                return readable;
            }

            public boolean isDeletable() {
                return deletable;
            }
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        public Configuration getConfig() {
            return config;
        }

        public void setConfig(Configuration config) {

            this.config = config;

            // If table contains any data, clear
            this.table.clear();

//            // Create an alphabetical list of attributes
//            List<String> orderedKeys = new ArrayList<String>();
//            orderedKeys.addAll(this.config.getValueKeys());
//            Collections.sort(orderedKeys);
//
//            // Add all attributes
//            for (String key : orderedKeys) {
//
//                String value = this.config.getValue(key);
//
//                if (value == null) {
//                    value = "none";
//                }
//
//                int row = this.addAttribute(key, value);
//            }

            for (String key : config.getValueKeys()) {
                String value = this.config.getValue(key);

                if (value == null) {
                    value = "none";
                }

                this.addAttribute(key, value);
            }

            // Now sort based on whatever was last sorted. This way, auto-updates do not reorder.
            AttributesTableModel tableModel = AttributesPanel.this.table.getAttributesTableModel();
            tableModel.sort(tableModel.getLastColumnPressed(), false);
        }
    }

    /**
     * Popup that allows a user to add a name/value pair. Called from ServerConfigurationFrame in a listener.
     */
    private class AddAttributeFrame extends JFrame implements MouseListener {

        private JButton addButton,  cancelButton;
        private JTextField nameField,  valueField;
        private int MARGIN_LEFT = 10,  MARGIN_RIGHT = 10,  LABEL_MARGIN_TOP = 10,  FIELD_MARGIN_TOP = 3;
        private AttributesPanel panel;
        private final static String addLabel = "Add Attribute to Configuration";
        private final static String editLabel = "Edit Attribute in Configuration";

        public AddAttributeFrame(AttributesPanel panel) {
            this(panel, "", "", addLabel, true);
        }

        public AddAttributeFrame(AttributesPanel panel, String name, String value, boolean isEdittable) {
            this(panel, name, value, isEdittable ? editLabel : editLabel + " (Read only)", isEdittable);
        }

        private AddAttributeFrame(AttributesPanel panel, String name, String value, String label, boolean isEdittable) {

            super(label);

            this.panel = panel;

            setLayout(new GridBagLayout());

            Dimension d = new Dimension(400, 175);

            this.setSize(d);
            this.setMinimumSize(d);

            // Add the constraints for the table
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            {
                // Name label and field
                JLabel nameLabel = new GenericLabel("Name");
                nameLabel.setFont(Styles.FONT_12PT_BOLD);

                gbc.insets = new Insets(LABEL_MARGIN_TOP, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 0; // <-- Row

                this.add(nameLabel, gbc);

                this.nameField = new GenericTextField();
                gbc.insets = new Insets(FIELD_MARGIN_TOP, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 1; // <-- Row

                this.nameField.setText(name);

                this.add(this.nameField, gbc);
            }

            {
                // Value label and field
                JLabel valueLabel = new GenericLabel("Value");
                valueLabel.setFont(Styles.FONT_12PT_BOLD);

                gbc.insets = new Insets(LABEL_MARGIN_TOP, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 2; // <-- Row

                this.add(valueLabel, gbc);

                this.valueField = new GenericTextField();
                gbc.insets = new Insets(FIELD_MARGIN_TOP, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 3; // <-- Row

                this.valueField.setText(value);

                this.add(this.valueField, gbc);
            }

            {
                // Cancel and add buttons
                this.cancelButton = new GenericButton("Cancel");

                if (label.equals(editLabel)) {
                    this.addButton = new GenericButton("Edit");
                } else if (label.equals(addLabel)) {
                    this.addButton = new GenericButton("Add");
                } else {
                    // Fall back on add
                    this.addButton = new GenericButton("Add");
                }

                // If read only, don't allow!
                this.addButton.setEnabled(isEdittable);

                gbc.gridwidth = 1;
                gbc.anchor = GridBagConstraints.PAGE_START;
                gbc.weightx = .5;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(FIELD_MARGIN_TOP, MARGIN_LEFT, 10, 10);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 4; // <-- Row

                this.add(this.cancelButton, gbc);

                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.insets = new Insets(FIELD_MARGIN_TOP, 0, 10, MARGIN_RIGHT);
                gbc.gridx = 1; // <-- Cell
                gbc.gridy = 4; // <-- Row

                this.add(this.addButton, gbc);
            }

            // Listeners for buttons
            this.addButton.addMouseListener(this);
            this.cancelButton.addMouseListener(this);
        }

        @Override()
        public void setVisible(boolean visible) {

            if (visible) {
                GUIUtil.centerOnScreen(this);
            }

            super.setVisible(visible);
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
            // Cancel
            if (e.getSource().equals(cancelButton)) {
                this.setVisible(false);
                return;
            } // Add
            else if (e.getSource().equals(addButton)) {
                Thread t = new Thread() {

                    @Override()
                    public void run() {
                        // If either are empty, bail
                        if (nameField.getText().trim().equals("") || valueField.getText().trim().equals("")) {
                            GenericOptionPane.showMessageDialog(
                                    ServerConfigurationPanel.this,
                                    "You must add a name and a value.",
                                    "Cannot Add Attribute",
                                    JOptionPane.WARNING_MESSAGE);
                            return;
                        } else if (panel.containsAttribute(nameField.getText())) {
                            int c = GenericOptionPane.showConfirmDialog(
                                    ServerConfigurationPanel.this.getParent(),
                                    "An attribute with that name exists. Continue and overwrite?",
                                    "Continue?",
                                    JOptionPane.YES_NO_OPTION);

                            if (c != JOptionPane.YES_OPTION) {
                                // Bail
                                return;
                            } else {
                                panel.addAttribute(nameField.getText(), valueField.getText());
                                dispose();
                            }
                        } else {
                            panel.addAttribute(nameField.getText(), valueField.getText());
                            dispose();
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        }
    }

    /**
     * Right-click menu that allows actions on certain rows
     */
    private class AttributesPopupMenu extends JPopupMenu {

        // menu items
        private JMenuItem editMenuItem = new JMenuItem("Edit selected attribute");
        private JMenuItem deleteMenuItem = new JMenuItem("Delete selected attribute(s)");
        private final AttributesPanel attributesPanel;

        public AttributesPopupMenu(final AttributesPanel attributesPanel) {
            setBorder(Styles.BORDER_BLACK_1);

            this.attributesPanel = attributesPanel;

            editMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override()
                        public void run() {
                            attributesPanel.editSelectedAttribute();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            deleteMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override()
                        public void run() {
                            attributesPanel.removeSelectedAttributes();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });

            // All functionality common to every project part
            add(editMenuItem);
            add(deleteMenuItem);

            // Check selections
            addPopupMenuListener(new PopupMenuListener() {

                public void popupMenuCanceled(PopupMenuEvent e) {
                }

                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    Thread t = new Thread() {

                        @Override()
                        public void run() {
                            int selectionCount = attributesPanel.table.getSelectedRowCount();

                            if (selectionCount == 1) {

                                // Doesn't have to be edittable: open in read only
                                // so user can read
                                editMenuItem.setEnabled(true);

                                // Only delete if edittable
                                deleteMenuItem.setEnabled(attributesPanel.table.isSelectedDeletable());
                            } else if (selectionCount > 1) {
                                editMenuItem.setEnabled(false);

                                // Only delete if edittable
                                deleteMenuItem.setEnabled(attributesPanel.table.isSelectedDeletable());
                            } else {
                                // Nothing selected
                                editMenuItem.setEnabled(false);
                                deleteMenuItem.setEnabled(false);
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
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        ServerConfigurationPanel.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static final boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static final void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(ServerConfigurationPanel.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
