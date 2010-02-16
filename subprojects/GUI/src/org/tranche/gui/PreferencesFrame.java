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
package org.tranche.gui;

import org.tranche.gui.util.GUIUtil;
import org.tranche.util.PreferencesUtil;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import org.tranche.ConfigureTranche;
import org.tranche.LocalDataServer;
import org.tranche.gui.get.DownloadPool;
import org.tranche.gui.add.UploadPool;

/**
 * Panel meant for use with GenericPopupFrame.
 * @author Bryan Smith <bryanesmith at gmail.com>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class PreferencesFrame extends GenericFrame implements ActionListener, KeyListener {

    private GeneralPreferencesPanel generalPanel = new GeneralPreferencesPanel();
    private DownloadPreferencesPanel downloadPanel = new DownloadPreferencesPanel();
    private UploadPreferencesPanel uploadPanel = new UploadPreferencesPanel();
    private ServerPreferencesPanel serverPanel = new ServerPreferencesPanel();
    private LocalServerPreferencesPanel localServerPanel = new LocalServerPreferencesPanel();
    private ErrorFrame ef = new ErrorFrame();
    private GenericRoundedButton defaultsButton = new GenericRoundedButton("Load Defaults"), cancelButton = new GenericRoundedButton("Cancel"), applyButton = new GenericRoundedButton("Apply Changes");
    private JTabbedPane pane = new JTabbedPane();
    /**
     * <p>Recommended size for this popup panel when used with a generic popup frame.</p>
     */
    public static final Dimension RECOMMENDED_DIMENSION = new Dimension(575, 400);
    private static final int MARGIN_WIDTH = 10;
    private static final Border MARGIN = BorderFactory.createEmptyBorder(MARGIN_WIDTH, MARGIN_WIDTH, MARGIN_WIDTH, MARGIN_WIDTH);
    public static final short SHOW_GENERAL = 0;
    public static final short SHOW_DOWNLOADS = 1;
    public static final short SHOW_UPLOADS = 2;
    public static final short SHOW_SERVERS = 3;
    public static final short SHOW_LOCAL_SERVERS = 4;
    public static final short SHOW_PROXY_SETTINGS = 5;

    public PreferencesFrame() {
        this(SHOW_GENERAL);
    }

    public PreferencesFrame(short show) {
        // load preferences
        try {
            setTitle("Preferences");
            setSize(PreferencesFrame.RECOMMENDED_DIMENSION);
            setBackground(Styles.COLOR_PANEL_BACKGROUND);
            setLayout(new BorderLayout());

            pane.setFocusable(false);
            pane.add(generalPanel);
            pane.add(downloadPanel);
            pane.add(uploadPanel);
            pane.add(serverPanel);
            pane.add(localServerPanel);
            add(pane, BorderLayout.CENTER);

            // show the right tab
            show(show);

            JPanel optionsPanel = new JPanel();
            optionsPanel.setBackground(Styles.COLOR_PANEL_BACKGROUND);
            optionsPanel.setLayout(new GridBagLayout());
            optionsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(0, 5, 0, 5);

                defaultsButton.setFont(Styles.FONT_11PT_BOLD);
                defaultsButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Thread t = new Thread() {

                            @Override()
                            public void run() {
                                int selected = GenericOptionPane.showConfirmDialog(PreferencesFrame.this,
                                        "Are you sure you want to load defaults preferences?\nThis will overwrite all of your preferences.",
                                        "Are you sure?",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE);
                                if (selected == JOptionPane.YES_OPTION) {
                                    try {
                                        PreferencesUtil.clear();
                                        generalPanel.loadPreferences();
                                        downloadPanel.loadPreferences();
                                        uploadPanel.loadPreferences();
                                        serverPanel.loadPreferences();
                                        localServerPanel.loadPreferences();
                                        checkForChanges();
                                    } catch (Exception ex) {
                                        ef.show(ex, PreferencesFrame.this);
                                    }
                                }
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                    }
                });
                optionsPanel.add(defaultsButton, gbc);

                cancelButton.setFont(Styles.FONT_11PT_BOLD);
                cancelButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Thread t = new Thread() {

                            @Override()
                            public void run() {
                                try {
                                    generalPanel.loadPreferences();
                                    downloadPanel.loadPreferences();
                                    uploadPanel.loadPreferences();
                                    serverPanel.loadPreferences();
                                    localServerPanel.loadPreferences();
                                    PreferencesFrame.this.setVisible(false);
                                } catch (Exception ex) {
                                    ef.show(ex, PreferencesFrame.this);
                                }
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                    }
                });
                optionsPanel.add(cancelButton, gbc);

                applyButton.setFont(Styles.FONT_11PT_BOLD);
                applyButton.setEnabled(false);
                applyButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Thread t = new Thread() {

                            @Override()
                            public void run() {
                                try {
                                    generalPanel.validatePreferences();
                                    downloadPanel.validatePreferences();
                                    uploadPanel.validatePreferences();
                                    serverPanel.validatePreferences();
                                    localServerPanel.validatePreferences();
                                    generalPanel.applyChanges();
                                    downloadPanel.applyChanges();
                                    uploadPanel.applyChanges();
                                    serverPanel.applyChanges();
                                    localServerPanel.applyChanges();
                                    PreferencesUtil.save();
                                    applyButton.setEnabled(false);
                                    PreferencesFrame.this.setVisible(false);
                                } catch (Exception ex) {
                                    ef.show(ex, PreferencesFrame.this);
                                }
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                    }
                });
                optionsPanel.add(applyButton, gbc);
            }
            add(optionsPanel, BorderLayout.SOUTH);

            loadPreferences();
        } catch (Exception e) {
            ef.show(e, PreferencesFrame.this);
        }
    }
    private Thread checkForChangesThread = new Thread() {

        @Override()
        public void run() {
            checkForChanges();
        }
    };

    {
        checkForChangesThread.setDaemon(true);
    }

    public void loadPreferences() {
        generalPanel.loadPreferences();
        downloadPanel.loadPreferences();
        uploadPanel.loadPreferences();
        serverPanel.loadPreferences();
        localServerPanel.loadPreferences();
    }

    public void actionPerformed(ActionEvent e) {
        checkForChangesThread.run();
    }

    public void keyTyped(KeyEvent e) {
        checkForChangesThread.run();
    }

    public void keyPressed(KeyEvent e) {
        checkForChangesThread.run();
    }

    public void keyReleased(KeyEvent e) {
        checkForChangesThread.run();
    }

    private void checkForChanges() {
        applyButton.setEnabled(generalPanel.isChanged() || downloadPanel.isChanged() || uploadPanel.isChanged() || serverPanel.isChanged() || localServerPanel.isChanged() /* || proxySettingsPanel.isChanged() */);
    }

    public void show(short show) {
        try {
            pane.setSelectedIndex(show);
        } catch (Exception e) {
            // catch bad show values
        }
    }

    private class GeneralPreferencesPanel extends JPanel {

        private GenericCheckBox useHashComplete = new GenericCheckBox("Open Hash Auto-Completion Window");

        public GeneralPreferencesPanel() {
            setName("General");
            setBorder(MARGIN);
            setBackground(Styles.COLOR_PANEL_BACKGROUND);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;

            GenericLabel hashAutoCompleteLabel = new GenericLabel("Hash Auto-Completion");
            hashAutoCompleteLabel.setFont(Styles.FONT_14PT_BOLD);
            gbc.insets = new Insets(6, 0, 0, 0);
            add(hashAutoCompleteLabel, gbc);

            DisplayTextArea hashAutoCompleteDescription = new DisplayTextArea("We keep a list of Tranche projects in the background. As you are " +
                    "typing a Tranche hash, we can pop up a window with a list of projects that match what you are typing. Typing as few as 3 " +
                    "characters will give you a unique project.");
            gbc.insets = new Insets(1, 5, 0, 0);
            add(hashAutoCompleteDescription, gbc);

            // hash auto complete checkbox
            useHashComplete.setBackground(getBackground());
            useHashComplete.addActionListener(PreferencesFrame.this);
            gbc.insets = new Insets(3, 5, 0, 0);
            add(useHashComplete, gbc);

            // padding to the bottom
            gbc.weighty = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridheight = GridBagConstraints.REMAINDER;
            add(Box.createVerticalStrut(1), gbc);
        }

        public void validatePreferences() throws Exception {
        }

        public void loadPreferences() {
            useHashComplete.setSelected(PreferencesUtil.getBoolean(ConfigureTrancheGUI.PROP_AUTO_COMPLETE_HASH));
        }

        public void applyChanges() throws Exception {
            PreferencesUtil.set(ConfigureTrancheGUI.PROP_AUTO_COMPLETE_HASH, String.valueOf(useHashComplete.isSelected()), false);
        }

        public boolean isChanged() {
            try {
                return useHashComplete.isSelected() != PreferencesUtil.getBoolean(ConfigureTrancheGUI.PROP_AUTO_COMPLETE_HASH);
            } catch (Exception e) {
                return true;
            }
        }
    }

    private class DownloadPreferencesPanel extends JPanel {

        private GenericTextField downloadLocationField = new GenericTextField(), downloadPoolSizeField = new GenericTextField();

        public DownloadPreferencesPanel() {
            setName("Downloads");
            setBorder(MARGIN);
            setBackground(Styles.COLOR_PANEL_BACKGROUND);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;

            GenericLabel downloadLocationLabel = new GenericLabel("Download Location");
            downloadLocationLabel.setFont(Styles.FONT_14PT_BOLD);
            add(downloadLocationLabel, gbc);

            DisplayTextArea downloadDescription = new DisplayTextArea("Set a default download location for projects. " +
                    "You will still be prompted upon starting a download. " +
                    "This will make it faster to select download locations when using the download tool.");
            gbc.insets = new Insets(1, 5, 0, 0);
            add(downloadDescription, gbc);

            downloadLocationField.setFont(Styles.FONT_11PT);
            downloadLocationField.addKeyListener(PreferencesFrame.this);
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            gbc.weightx = 2;
            gbc.insets = new Insets(3, 5, 0, 0);
            add(downloadLocationField, gbc);

            GenericButton downloadLocationButton = new GenericButton("Change");
            downloadLocationButton.setFont(Styles.FONT_10PT);
            downloadLocationButton.setBorder(BorderFactory.createCompoundBorder(Styles.BORDER_BLACK_1, BorderFactory.createEmptyBorder(0, 4, 0, 4)));
            downloadLocationButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final JFileChooser fc = GUIUtil.makeNewFileChooser();
                    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    try {
                        File previousDir = new File(downloadLocationField.getText());
                        if (previousDir != null && previousDir.exists()) {
                            fc.setCurrentDirectory(previousDir);
                        }
                    } catch (Exception ex) {
                    }

                    // do not hold up the dispatch thread
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            int returnVal = fc.showOpenDialog(PreferencesFrame.this);

                            if (returnVal == FileChooser.APPROVE_OPTION) {
                                downloadLocationField.setText(fc.getSelectedFile().getAbsolutePath());
                                checkForChanges();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            gbc.weightx = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            add(downloadLocationButton, gbc);

            GenericLabel poolSizeLabel = new GenericLabel("Pool Size");
            poolSizeLabel.setFont(Styles.FONT_14PT_BOLD);
            gbc.insets = new Insets(6, 0, 0, 0);
            gbc.weightx = 1;
            add(poolSizeLabel, gbc);

            DisplayTextArea poolSizesDescription = new DisplayTextArea("The pool size refers to number of concurrent downloads. " +
                    "Larger pool size can make the most of network latency but requires more memory and processor resources. " +
                    "Setting to 0 makes the pool size unlimited.");
            gbc.insets = new Insets(1, 5, 0, 0);
            add(poolSizesDescription, gbc);

            JPanel downloadPoolSizePanel = new JPanel();
            downloadPoolSizePanel.setBackground(getBackground());
            downloadPoolSizePanel.setLayout(new GridBagLayout());
            {
                GridBagConstraints gbc2 = new GridBagConstraints();
                gbc2.fill = GridBagConstraints.HORIZONTAL;

                GenericLabel downloadLabel = new GenericLabel("Download pool size: ");
                gbc2.gridwidth = GridBagConstraints.RELATIVE;
                downloadPoolSizePanel.add(downloadLabel, gbc2);

                downloadPoolSizeField.addKeyListener(PreferencesFrame.this);
                gbc2.insets = new Insets(0, 5, 0, 0);
                gbc2.weightx = 2;
                gbc2.gridwidth = GridBagConstraints.REMAINDER;
                downloadPoolSizePanel.add(downloadPoolSizeField, gbc2);
            }
            gbc.insets = new Insets(3, 5, 0, 0);
            add(downloadPoolSizePanel, gbc);

            // padding to the bottom
            gbc.weighty = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridheight = GridBagConstraints.REMAINDER;
            add(Box.createVerticalStrut(1), gbc);
        }

        public void validatePreferences() throws Exception {
            int downloadPoolSize = Integer.valueOf(downloadPoolSizeField.getText());
            if (downloadPoolSize < 0) {
                throw new Exception("Download pool size must be a positive number.");
            }
        }

        public void loadPreferences() {
            downloadLocationField.setText(PreferencesUtil.get(PreferencesUtil.PREF_DOWNLOAD_FILE));
            downloadPoolSizeField.setText(PreferencesUtil.get(ConfigureTrancheGUI.PROP_DOWNLOAD_POOL_SIZE));
        }

        public void applyChanges() throws Exception {
            PreferencesUtil.set(PreferencesUtil.PREF_DOWNLOAD_FILE, downloadLocationField.getText(), false);
            PreferencesUtil.set(ConfigureTrancheGUI.PROP_DOWNLOAD_POOL_SIZE, downloadPoolSizeField.getText(), false);
            // set the changes to the upload pool
            DownloadPool.setDownloadPoolSize(Integer.valueOf(downloadPoolSizeField.getText()));
        }

        public boolean isChanged() {
            try {
                return !downloadLocationField.getText().equals(PreferencesUtil.get(PreferencesUtil.PREF_DOWNLOAD_FILE)) ||
                        !downloadPoolSizeField.getText().equals(PreferencesUtil.get(ConfigureTrancheGUI.PROP_DOWNLOAD_POOL_SIZE));
            } catch (Exception e) {
                return true;
            }
        }
    }

    private class UploadPreferencesPanel extends JPanel {

        private GenericTextField uploadPoolSizeField = new GenericTextField();

        public UploadPreferencesPanel() {
            setName("Uploads");
            setBorder(MARGIN);
            setBackground(Styles.COLOR_PANEL_BACKGROUND);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;

            GenericLabel poolSizeLabel = new GenericLabel("Pool Size");
            poolSizeLabel.setFont(Styles.FONT_14PT_BOLD);
            gbc.insets = new Insets(0, 0, 0, 0);
            add(poolSizeLabel, gbc);
            DisplayTextArea poolSizesDescription = new DisplayTextArea("The pool size refers to number of concurrent uploads. " +
                    "Larger pool size can make the most of network latency but requires more memory and processor resources." +
                    "Setting to 0 makes the pool size unlimited.");
            gbc.insets = new Insets(1, 5, 0, 0);
            add(poolSizesDescription, gbc);

            // Add upload pool size
            GenericLabel uploadLabel = new GenericLabel("Upload pool size: ");
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            gbc.insets = new Insets(3, 5, 0, 0);
            gbc.weightx = 0;
            add(uploadLabel, gbc);

            uploadPoolSizeField.addKeyListener(PreferencesFrame.this);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 2;
            gbc.insets = new Insets(3, 5, 0, 0);
            add(uploadPoolSizeField, gbc);

            // padding to the bottom
            gbc.weighty = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridheight = GridBagConstraints.REMAINDER;
            add(Box.createVerticalStrut(1), gbc);
        }

        public void validatePreferences() throws Exception {
            int uploadPoolSize = Integer.valueOf(uploadPoolSizeField.getText());
            if (uploadPoolSize < 0) {
                throw new Exception("Upload pool size must be a positive number.");
            }
        }

        public void loadPreferences() {
            uploadPoolSizeField.setText(PreferencesUtil.get(ConfigureTrancheGUI.PROP_UPLOAD_POOL_SIZE));
        }

        public void applyChanges() throws Exception {
            PreferencesUtil.set(ConfigureTrancheGUI.PROP_UPLOAD_POOL_SIZE, uploadPoolSizeField.getText(), false);
            // set the changes to the upload pool
            UploadPool.setUploadPoolSize(Integer.valueOf(uploadPoolSizeField.getText()));
        }

        public boolean isChanged() {
            try {
                return !uploadPoolSizeField.getText().equals(PreferencesUtil.get(ConfigureTrancheGUI.PROP_UPLOAD_POOL_SIZE));
            } catch (Exception e) {
                return true;
            }
        }
    }

    private class ServerPreferencesPanel extends JPanel {

        public ServerPreferencesPanel() {
            setName("Servers");
            setBorder(MARGIN);
            setBackground(Styles.COLOR_PANEL_BACKGROUND);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;

            // padding to the bottom
            gbc.weighty = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridheight = GridBagConstraints.REMAINDER;
            add(Box.createVerticalStrut(1), gbc);
        }

        public void validatePreferences() throws Exception {
        }

        public void loadPreferences() {
        }

        public void applyChanges() throws Exception {
        }

        public boolean isChanged() {
            return false;
        }
    }

    private class LocalServerPreferencesPanel extends JPanel {

        private GenericCheckBox ssl = new GenericCheckBox("Use Secure Sockets Layer (SSL)");
        private GenericTextField portField = new GenericTextField(), rootDirField = new GenericTextField();

        public LocalServerPreferencesPanel() {
            setName("Local Server");
            setBorder(MARGIN);
            setBackground(Styles.COLOR_PANEL_BACKGROUND);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(0, 0, 0, 0);
            gbc.weightx = 1;
            gbc.weighty = 0;
            GenericLabel directoryLabel = new GenericLabel("Root Directory");
            directoryLabel.setFont(Styles.FONT_14PT_BOLD);
            add(directoryLabel, gbc);

            gbc.gridwidth = 1;
            gbc.insets = new Insets(3, 5, 0, 0);
            gbc.weightx = 0;
            add(new GenericLabel("Directory: "), gbc);

            gbc.weightx = 5;
            gbc.insets = new Insets(3, 5, 0, 0);
            add(rootDirField, gbc);

            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 0;
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
                            fc.showOpenDialog(PreferencesFrame.this);
                            // check for a submitted file
                            File selectedFile = fc.getSelectedFile();
                            if (selectedFile == null) {
                                return;
                            }
                            rootDirField.setText(selectedFile.getAbsolutePath());
                            checkForChanges();
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

            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(10, 0, 0, 0);
            GenericLabel portLabel = new GenericLabel("Port Number");
            portLabel.setFont(Styles.FONT_14PT_BOLD);
            add(portLabel, gbc);

            gbc.insets = new Insets(1, 5, 0, 0);
            add(new DisplayTextArea("The port to which your server should bind. Each port can only be used by one computer program at a time."), gbc);

            gbc.gridwidth = GridBagConstraints.RELATIVE;
            gbc.insets = new Insets(3, 5, 0, 0);
            gbc.weightx = 0;
            add(new GenericLabel("Port: "), gbc);

            portField.addKeyListener(PreferencesFrame.this);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 2;
            gbc.insets = new Insets(3, 5, 0, 0);
            add(portField, gbc);

            GenericLabel sslLabel = new GenericLabel("SSL");
            sslLabel.setFont(Styles.FONT_14PT_BOLD);
            gbc.insets = new Insets(10, 0, 0, 0);
            add(sslLabel, gbc);

            gbc.insets = new Insets(1, 5, 0, 0);
            add(new DisplayTextArea("Using secure socket layers (ssl) for your local server will ensure that communication with your server cannot be listened to by a third party. Using SSL slows down your server slightly."), gbc);

            // auto server banning
            ssl.setBackground(getBackground());
            ssl.addActionListener(PreferencesFrame.this);
            gbc.insets = new Insets(3, 5, 0, 0);
            add(ssl, gbc);

            // padding to the bottom
            gbc.weighty = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridheight = GridBagConstraints.REMAINDER;
            add(Box.createVerticalStrut(1), gbc);
        }

        public void validatePreferences() throws Exception {
            int port = Integer.valueOf(portField.getText());
            if (port < 1) {
                throw new Exception("Port must be a positive number.");
            }
        }

        public void loadPreferences() {
            rootDirField.setText(PreferencesUtil.get(ConfigureTranche.PROP_SERVER_DIRECTORY));
            ssl.setSelected(PreferencesUtil.getBoolean(ConfigureTranche.PROP_SERVER_SSL));
            portField.setText(PreferencesUtil.get(ConfigureTranche.PROP_SERVER_PORT));
        }

        public void applyChanges() throws Exception {
            PreferencesUtil.set(ConfigureTranche.PROP_SERVER_SSL, String.valueOf(ssl.isSelected()), false);
            PreferencesUtil.set(ConfigureTranche.PROP_SERVER_DIRECTORY, rootDirField.getText(), false);
            PreferencesUtil.set(ConfigureTranche.PROP_SERVER_PORT, portField.getText(), false);
        }

        public boolean isChanged() {
            try {
                return !rootDirField.getText().equals(String.valueOf(PreferencesUtil.get(ConfigureTranche.PROP_SERVER_DIRECTORY))) ||
                        ssl.isSelected() != PreferencesUtil.getBoolean(ConfigureTranche.PROP_SERVER_SSL) ||
                        !portField.getText().equals(PreferencesUtil.get(ConfigureTranche.PROP_SERVER_PORT));
            } catch (Exception e) {
                return true;
            }
        }
    }
}
