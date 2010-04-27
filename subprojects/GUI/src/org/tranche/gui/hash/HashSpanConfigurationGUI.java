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
package org.tranche.gui.hash;

import org.tranche.gui.user.SignInUserButton;
import org.tranche.gui.*;
import org.tranche.gui.server.ServersPanel;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import org.tranche.TrancheServer;
import org.tranche.exceptions.TodoException;
import org.tranche.gui.util.GUIUtil;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.hash.span.HashSpanCalculator;
import org.tranche.util.IOUtil;
import org.tranche.users.UserZipFile;

/**
 * A tool that uses the hash span calculator to allow users to configure an arbitrary number of serversInfoList.
 *
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class HashSpanConfigurationGUI extends GenericFrame implements MouseListener {

    private JTextField replicationsField; // <-- Set number of replications
    public ServersPanel serversPanel;
    private JButton calculateButton = new GenericButton("Calculate hash spans...");
    public SignInUserButton userButton = new SignInUserButton();
    public ErrorFrame ef;
    // Should this be in the Styles util?
    final int MARGIN_LEFT = 5,  MARGIN_RIGHT = 5;
    private static final String TITLE = "Network Hash Span Configuration";

    // ========================================================================
    // Build the GUI
    public HashSpanConfigurationGUI() {
        super(TITLE);
        if (true) {
            throw new TodoException();
        }
//        serversPanel = new ServersPanel();
        init();
    }

    public HashSpanConfigurationGUI(Set<String> servers) {
        super(TITLE);
        if (true) {
            throw new TodoException();
        }
//        serversPanel = new ServersPanel(servers);
        init();
    }

    public void init() {
        // set the style
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(new Dimension(400, 425));
        setBackground(Styles.COLOR_BACKGROUND);
        setLayout(new BorderLayout());

        // create the error frame
        ef = new ErrorFrame();

        // add the menu bar
        {
            JMenuBar menuBar = new GenericMenuBar();
            JMenu helpMenu = new GenericMenu("Help");
            JMenuItem aboutMenuItem = new GenericMenuItem("About This Tool");
            aboutMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    DescriptionFrame gui = new DescriptionFrame("About Hash Span Calculator", "Calculates proportional hash spans for a network of servers based on desired number of replications and available storage in each server.");
                    gui.setLocationRelativeTo(HashSpanConfigurationGUI.this);
                    gui.setVisible(true);
                }
            });
            helpMenu.add(aboutMenuItem);
            menuBar.add(helpMenu);
            add(menuBar, BorderLayout.NORTH);
        }

        // Use for a few tool tips
        final String genericToolTip = "The purpose of the tool is to create balanced hash spans. After calculating, you can accept, modify or reject the recommended hash spans.";

        // add the center panel
        {
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            // Can reuse same constraints object
            GridBagConstraints gbc = new GridBagConstraints();

            // Load user button
            {
                // Set up contraint for the button
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.PAGE_START;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(12, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 1; // <-- Row

                // Add the panel
                panel.add(userButton, gbc);
            }

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // Set number of replications

            {
                final String repsToolTip = "Number of desired replications for each file chunk. More means more redundancy, but larger storage requirements.";

                // Label
                JLabel replicationsLabel = new GenericLabel("Number of replications:");
                replicationsLabel.setToolTipText(repsToolTip);

                // Panel for label and text field
                JPanel replicationsPanel = new JPanel();
                replicationsPanel.setLayout(new GridBagLayout());
                GridBagConstraints repsConstraints = new GridBagConstraints();

                // Place the label in the panel
                repsConstraints.anchor = GridBagConstraints.NORTHWEST;
                repsConstraints.ipadx = 0;
                repsConstraints.ipady = 0;
                repsConstraints.weightx = 0;
                repsConstraints.weighty = 0;
                repsConstraints.fill = GridBagConstraints.HORIZONTAL;
                repsConstraints.gridx = 0; // <-- Cell
                repsConstraints.gridy = 0; // <-- Row

                // Little margin on top so aligns right with text field.
                // Relative to panel, not GUI
                repsConstraints.insets = new Insets(3, 0, 0, 0);

                replicationsPanel.add(replicationsLabel, repsConstraints);

                // Place the text field in the panel
                this.replicationsField = new GenericTextField();

                repsConstraints.gridx = 1; // <-- Cell
                repsConstraints.gridy = 0; // <-- Row
                repsConstraints.weightx = 1;
                repsConstraints.fill = GridBagConstraints.HORIZONTAL;
                repsConstraints.gridwidth = GridBagConstraints.REMAINDER;

                // Relative to panel, not GUI
                repsConstraints.insets = new Insets(0, MARGIN_LEFT, 0, 0);

                replicationsPanel.add(this.replicationsField, repsConstraints);

                // Now add the panel to the GUI
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(10, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 2; // <-- Row

                panel.add(replicationsPanel, gbc);
            }

            if (true) {
                throw new TodoException();
            }
//            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//            // Build up a scrollable list of serversInfoList
//            {
//                // only allow servers to be used where the loaded user has write priveleges
//                serversPanel.setWritePrivelegedUsersOnly(true);
//
//                // listen for changes to the loaded user
//                userButton.addPropertyChangeListener(new PropertyChangeListener() {
//
//                    public void propertyChange(PropertyChangeEvent evt) {
//                        if (evt.getPropertyName().equals("text")) {
//                            serversPanel.setUserZipFile(userButton.getUser());
//                        }
//                    }
//                });
//
//                gbc.gridwidth = GridBagConstraints.REMAINDER;
//                gbc.anchor = GridBagConstraints.NORTHWEST;
//                gbc.weightx = 1;
//                gbc.weighty = 1;
//                gbc.fill = GridBagConstraints.BOTH;
//                gbc.insets = new Insets(10, MARGIN_LEFT, 10, MARGIN_RIGHT);
//                gbc.gridx = 0; // <-- Cell
//                gbc.gridy = 3; // <-- Row
//
//                serversPanel.setMinimumSize(new Dimension(200, 200));
//                serversPanel.setBorder(Styles.BORDER_BLACK_1);
//
//                panel.add(serversPanel, gbc);
//            }
//            add(panel, BorderLayout.CENTER);

            // "Calculate hash spans..." button
            JPanel buttonPanel;
            {
                buttonPanel = new JPanel();
                buttonPanel.setLayout(new GridBagLayout());

                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.weightx = 1;
                gbc.weighty = 1;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.insets = new Insets(0, MARGIN_LEFT, 15, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 0; // <-- Row

                calculateButton.setFont(Styles.FONT_14PT_BOLD);
                calculateButton.setToolTipText(genericToolTip);

                buttonPanel.add(calculateButton, gbc);
            }

            add(buttonPanel, BorderLayout.SOUTH);
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Set listeners
        calculateButton.addMouseListener(this);
    }

    // ========================================================================
    /**
     * Entry point for the GUI
     */
    public static void main(String[] args) throws Exception {
        // configure Tranche network
        ConfigureTrancheGUI.load(args);

        HashSpanConfigurationGUI gui = new HashSpanConfigurationGUI();

        // show the frame
        GUIUtil.centerOnScreen(gui);
        gui.setVisible(true);
    }

    // ========================================================================
    // Listeners
    public void mouseReleased(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {

        /*
        Thread t = new Thread("") {
        public void run() {
        }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
         */

        // If calculate button
        if (e.getComponent().equals(this.calculateButton)) {

            Thread t = new Thread("Calculation thread") {

                public void run() {
                    if (true) {
                        throw new TodoException();
                    }
//                    // Build up appropriate alert with all relevant error information
//                    StringBuffer errorMsg = new StringBuffer();
//                    boolean isError = false;
//
//                    // Make sure replications is set
//                    int numReplications = -1;
//
//                    // 1. Error if number of replications not set
//                    if (replicationsField.getText().trim().equals("")) {
//                        errorMsg.append("* Must specify number of replications. E.g., if want one copy of every file chunk in network, specify 1." + "\n");
//                        isError = true;
//                    } // 2. Error if number of replications NaN
//                    else {
//
//                        try {
//                            numReplications = Integer.parseInt(replicationsField.getText());
//                        } catch (Exception numEx) {
//                            errorMsg.append("* \"" + replicationsField.getText() + "\" is not a number. Please specify a whole number, e.g., 1, 2, 3, etc." + "\n");
//                            isError = true;
//                        }
//                    }
//
//                    // 3. Error if at least not 1 server selected
//                    if (serversPanel.getSelectedServers().size() < 1) {
//                        errorMsg.append("* Must select at least one server that is online." + "\n");
//                        isError = true;
//                    }
//
//                    // 4. No user loaded
//                    if (userButton.getUser() == null) {
//                        errorMsg.append("* Must load a user." + "\n");
//                        isError = true;
//                    } // 5. User does not have permissions to set or get configuration
//                    // USER FILE DOES NOT REPORT PERMISSIONS CORRECTLY, TODO
//                    //            else if (!this.userButton.getUser().canSetConfiguration() || !this.userButton.getUser().canGetConfiguration()){
//                    //
//                    //                errorMsg.append("* User does not have configuration permissions set." + "\n");
//                    //                isError = true;
//                    //            }
//                    // 6. User is not recognized by server
//                    else if (serversPanel.getSelectedServers().size() != 0 && userButton.getUser() != null) {
//
//                        for (String server : serversPanel.getSelectedServers()) {
//
//                            ServerUtil.isServerOnline(server);
//                            Configuration config = ServerUtil.getServerInfo(server).getConfiguration();
//
//                            // If user not recognized
//                            if (config.getRecognizedUser(userButton.getUser().getCertificate()) == null) {
//                                errorMsg.append("* User is not recognized by server " + server + "." + "\n");
//                                isError = true;
//                            }
//                        }
//                    }
//
//                    // If there is an error message, display it
//                    if (isError) {
//                        //ef.show(new Exception("Error while attempting to calculate hash spans:" + "\n" + errorMsg), HashSpanConfigurationGUI.this);
//
//                        GenericOptionPane.showMessageDialog(HashSpanConfigurationGUI.this,
//                                "Error while attempting to calculate hash spans:" + "\n" + errorMsg,
//                                "Cannot calculate hash spans",
//                                JOptionPane.ERROR_MESSAGE);
//
//                        // Bail.
//                        return;
//                    }
//
//                    // Go! Confirmation GUI does the rest of the work.
//                    CalculatorGUI cGUI = new CalculatorGUI(userButton.getUser(), serversPanel.getSelectedServers(), numReplications, HashSpanConfigurationGUI.this);
//                    cGUI.performCalculation();
                }
            };
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }
    }
    // ========================================================================

    /**
     * Background thread for handling server status and known servers
     */
    // ========================================================================
    /**
     * Generates popup dialog, which calls hash span calculator, etc.
     */
    private class CalculatorGUI extends JFrame implements MouseListener {

        private UserZipFile user;
        private List<String> serversList = new ArrayList<String>();
        private int replications;
        private Map<String, Set<HashSpan>> spanMap;
        private JList spanList;
        private GenericScrollPane spanScroller;
        private JButton viewButton,  commitButton,  cancelButton;
        private DefaultListModel listModel;
        private int index; // Most recently selected item in list

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Build the GUI
        public CalculatorGUI(UserZipFile user, Collection<String> servers, int replications, HashSpanConfigurationGUI gui) {

            super("Calculate hash spans");

            this.user = user;
            this.serversList.addAll(servers);
            this.replications = replications;

            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            this.setSize(new Dimension(400, 240));
            this.setBackground(Styles.COLOR_BACKGROUND);
            this.setLayout(new GridBagLayout());
            this.setLocationRelativeTo(gui);

            // Can reuse same constraints object
            GridBagConstraints gbc = new GridBagConstraints();

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // Add span list to GUI
            {
                this.listModel = new DefaultListModel();
                this.spanList = new JList(this.listModel);
                this.spanList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

                this.spanScroller = new GenericScrollPane(this.spanList);
                this.spanScroller.setBorder(Styles.BORDER_BLACK_1);
                this.spanScroller.setPreferredSize(new Dimension(100, 100));

                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(10, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 0; // <-- Row

                this.add(this.spanScroller, gbc);
            }

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // Add view button
            {
                this.viewButton = new JButton("View or edit selected server");

                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 1; // <-- Row

                this.add(this.viewButton, gbc);
            }

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // Add buttons
            {
                this.commitButton = new JButton("Commit Spans");

                gbc.gridwidth = 1;
                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(15, MARGIN_LEFT, 10, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 2; // <-- Row

                this.add(this.commitButton, gbc);

                this.cancelButton = new JButton("Cancel");

                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(15, MARGIN_LEFT, 10, MARGIN_RIGHT);
                gbc.gridx = 1; // <-- Cell
                gbc.gridy = 2; // <-- Row

                this.add(this.cancelButton, gbc);
            }

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // Register listeners

            this.viewButton.addMouseListener(this);
            this.commitButton.addMouseListener(this);
            this.cancelButton.addMouseListener(this);
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        /**
         * Performs the calculation.
         */
        public void performCalculation() {

            this.setVisible(true);

            // Start indeterminate progress bar
            IndeterminateProgressBar progress = new IndeterminateProgressBar("Calculating hash spans...");
            progress.setLocationRelativeTo(this);
            progress.start();

            // Build a calculator
            HashSpanCalculator calculator = new HashSpanCalculator(this.user.getCertificate(), this.replications);

            // Add urls for each server
            for (String server : this.serversList) {
                calculator.addServer(server);
            }

            try {
                this.spanMap = calculator.calculateSpans();
            } catch (Exception ex) {
                ef.show(new Exception("An error has occurred. Please try again." + "\n" + ex), HashSpanConfigurationGUI.this);
                dispose();
                return;
            }

            // For each server, set the name and rough percentage
            for (String url : this.serversList) {

                double percentage = calculator.getServerPercentage(url);

                // Round to nearest percentage
                long estimate = Math.round(percentage * 100);
                percentage = (double) estimate;

                String nextHashReport = url + " (approx. " + percentage + "%)";

                this.listModel.addElement(nextHashReport);
            }

            // Done progress, dispose the progress bar
            progress.stop();
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Listeners
        public void mouseReleased(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseClicked(MouseEvent e) {

            // View and edit
            if (e.getSource().equals(this.viewButton)) {

//                Thread t = new Thread("") {
//                    public void run() {
//
//                    }
//                };
//                t.setDaemon(true);
//                t.setPriority(Thread.MIN_PRIORITY);
//                t.start();

                this.index = this.spanList.getSelectedIndex();

                // Bail if nothing selected
                if (index == -1) {
                    ef.show(new Exception("Please select a server to open the hash span(s) editor."), HashSpanConfigurationGUI.this);
                    return;
                }

                // Server info list and span list are synchronized by index.
                // I.e., each has the same server at same index

                String url = this.serversList.get(index);
                Set<HashSpan> hashSpans = this.spanMap.get(url);

                HashSpanEditor editor = new HashSpanEditor(hashSpans, url, this);

            // Editor will callback when done
            } // Set hash spans
            else if (e.getSource().equals(this.commitButton)) {

                Thread t = new Thread("Commit thread") {

                    public void run() {
                        Set<HashSpan> hashSpans;
                        TrancheServer rserver = null;

                        int successes = 0, failures = 0;

                        // For each server, set the configuration
                        for (String url : serversList) {

                            try {
                                hashSpans = spanMap.get(url);
                                if (true) {
                                    throw new TodoException();
                                }
//
//                                System.out.println("Attempting to set " + hashSpans.size() + " hash spans to " + url + ":");
//                                for (HashSpan span : hashSpans) {
//                                    System.out.println(" * " + span.getFirst().toString().substring(0, 12) + "... to " + span.getLast().toString().substring(0, 12) + "...");
//                                }
//                                rserver = IOUtil.connect(url);
//                                Configuration config = ServerUtil.getServerInfo(url).getConfiguration();
//                                config.setHashSpans(hashSpans);
//                                IOUtil.setConfiguration(rserver, config, user.getCertificate(), user.getPrivateKey());
//                                IOUtil.safeClose(rserver);
//
//                                System.out.println("Finished, set " + hashSpans.size() + " hash spans to " + url + ":");
//                                for (HashSpan span : config.getHashSpans()) {
//                                    System.out.println(" * " + span.getFirst().toString().substring(0, 12) + "... to " + span.getLast().toString().substring(0, 12) + "...");
//                                }

                                successes++;
                            } catch (Exception ex) {
                                ef.show(new Exception("An error has occurred. Please try again." + "\n" + ex), CalculatorGUI.this);
                                if (url != null) {
                                    System.err.println("Problem setting hash spans for " + url + ": " + ex.getMessage());
                                } else {
                                    System.err.println("Problem setting hash spans: " + ex.getMessage());
                                }
                                failures++;
                            } finally {
                                if (rserver != null) {
                                    IOUtil.safeClose(rserver);
                                }
                                dispose();
                            }

                        } // Foreach server
                        GenericOptionPane.showMessageDialog(CalculatorGUI.this, "Set hash spans for " + successes + " server(s), failed to set for " + failures + " servers.");
                    }
                };
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();


            } else if (e.getSource().equals(this.cancelButton)) {
                dispose();
            }
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Calback function. HashSpanEditor is returning hash span(s)
        public void setHashSpans(String url, Set<HashSpan> returnedSpans) {

            this.spanMap.put(url, returnedSpans);
            String update = url + " (Modified)";

            // Update list to mention modification
            this.listModel.set(this.index, update);
        }
    } // CalculatorGUI inner class
    // ========================================================================

    /**
     * Allows user to view and edit hash span(s) for a server
     */
    private class HashSpanEditor extends JFrame implements MouseListener {

        private Set<HashSpan> hashSpans;
        private String url;
        // For callback
        CalculatorGUI gui;
        // May be one or two hash spans, but initialize so no null pointers
        private JTextField start1 = new GenericTextField(),  end1 = new GenericTextField(),  start2 = new GenericTextField(),  end2 = new GenericTextField();
        private JButton saveButton,  cancelButton;
        private int numHashSpans = -1; // <-- Will be 1 or 2
        // Important: blocks until user is done so can return updated hash span
        private boolean isDone = false;

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Create the GUI
        public HashSpanEditor(Set<HashSpan> hashSpans, String url, CalculatorGUI gui) {

            super("Hash Span Editor");

            this.hashSpans = hashSpans;
            this.url = url;
            this.gui = gui;

            // Set mode (single or double hash spans(
            this.numHashSpans = this.hashSpans.size();

            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            this.setSize(new Dimension(600, 200));
            this.setBackground(Styles.COLOR_BACKGROUND);
            this.setLayout(new GridBagLayout());
            this.setLocationRelativeTo(gui);

            // Can reuse same constraints object
            GridBagConstraints gbc = new GridBagConstraints();

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // Set description information
            {
                String description = "";

                if (this.numHashSpans == 1) {
                    description = "There is 1 hash span for the server at " + this.url + ".";
                } else if (this.numHashSpans == 2) {
                    description = "There are two hash spans for the server at " + this.url + ".";
                }

                JTextArea descriptionArea = new GenericTextArea(description);
                descriptionArea.setLineWrap(true);
                descriptionArea.setWrapStyleWord(true);
                descriptionArea.setBackground(Color.WHITE);
                descriptionArea.setEditable(false);

                // Setup panel for border, etc.
                JPanel descriptionPanel = new JPanel();
                descriptionPanel.setBorder(Styles.BORDER_BLACK_1);
                descriptionPanel.setBackground(Styles.COLOR_BACKGROUND_LIGHT);
                descriptionPanel.setLayout(new GridBagLayout());
                GridBagConstraints descriptionConstraints = new GridBagConstraints();
                descriptionConstraints.insets = new Insets(3, 3, 3, 3);
                descriptionConstraints.gridwidth = GridBagConstraints.REMAINDER;
                descriptionConstraints.fill = GridBagConstraints.BOTH;
                descriptionConstraints.weightx = 1;
                descriptionConstraints.weighty = 1;

                // Add the description to the embedded panel
                descriptionPanel.add(descriptionArea, descriptionConstraints);

                // Set up contraint for the panel
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.PAGE_START;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(10, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 0; // <-- Row

                // Add the panel
                this.add(descriptionPanel, gbc);
            }

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // Set up first hash span
            {
                HashSpan span1 = this.hashSpans.toArray(new HashSpan[0])[0];

                this.start1 = new GenericTextField(span1.getFirst().toString());
                this.end1 = new GenericTextField(span1.getLast().toString());

                this.start1.setFont(Styles.FONT_10PT_MONOSPACED);
                this.end1.setFont(Styles.FONT_10PT_MONOSPACED);

                JLabel firstHashSpanLabel = new GenericLabel("First hash span:");

                // Set up constraints for label
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.PAGE_START;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(15, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 1; // <-- Row

                this.add(firstHashSpanLabel, gbc);

                // Set up constraints for start1
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.PAGE_START;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 2; // <-- Row

                this.add(start1, gbc);

                // Set up constraints for end1
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.PAGE_START;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 3; // <-- Row

                this.add(end1, gbc);

            // TODO Remove hash span

            }

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // Set up second hash span iff two hash spans
            if (this.numHashSpans == 2) {

                HashSpan span2 = this.hashSpans.toArray(new HashSpan[0])[1];

                this.start2 = new GenericTextField(span2.getFirst().toString());
                this.end2 = new GenericTextField(span2.getLast().toString());

                this.start2.setFont(Styles.FONT_10PT_MONOSPACED);
                this.end2.setFont(Styles.FONT_10PT_MONOSPACED);

                JLabel secondHashSpanLabel = new GenericLabel("Second hash span:");

                // Set up constraints for label
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.PAGE_START;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(15, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 4; // <-- Row

                this.add(secondHashSpanLabel, gbc);

                // Set up constraints for start1
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.PAGE_START;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 5; // <-- Row

                this.add(start2, gbc);

                // Set up constraints for end1
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.PAGE_START;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 6; // <-- Row

                this.add(end2, gbc);

            // TODO Remove hash span
            }

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // Add "Save" and "Cancel" buttons
            {
                this.saveButton = new JButton("Save");
                this.cancelButton = new JButton("Cancel");

                // Set up constraints for save button
                gbc.gridwidth = 1;
                gbc.anchor = GridBagConstraints.PAGE_START;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(10, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 0; // <-- Cell
                gbc.gridy = 7; // <-- Row

                this.add(this.saveButton, gbc);

                // Set up constraints for cancel button
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.PAGE_START;
                gbc.weightx = 1;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(10, MARGIN_LEFT, 0, MARGIN_RIGHT);
                gbc.gridx = 1; // <-- Cell
                gbc.gridy = 7; // <-- Row

                this.add(this.cancelButton, gbc);
            }

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // Register mouse listeners for buttons
            this.saveButton.addMouseListener(this);
            this.cancelButton.addMouseListener(this);

            this.setVisible(true);
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Show the dialog, wait for a response by user.
//        public Set<HashSpan> getUpdatedHashSpans() {
//
//            // Block until user is finished
//            while (!this.isDone) {
//                try {
//                    System.out.println("Sleeping...");
//                    Thread.sleep(3000);
//                } catch (InterruptedException ex) {
//                    // No thanks
//                }
//            }
//
//            this.setVisible(false);
//            this.dispose();
//            return this.hashSpans;
//        }
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // Listeners
        public void mouseReleased(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseClicked(MouseEvent e) {

            // If save
            if (e.getComponent().equals(this.saveButton)) {

                // Reset hash span
                BigHash start1 = BigHash.createHashFromString(this.start1.getText());
                BigHash end1 = BigHash.createHashFromString(this.end1.getText());

                HashSpan span1 = this.hashSpans.toArray(new HashSpan[0])[0];
                span1.setFirst(start1);
                span1.setLast(end1);

                HashSpan span2 = null;

                // There will only be a second span sometimes
                if (this.numHashSpans == 2) {

                    BigHash start2 = BigHash.createHashFromString(this.start2.getText());
                    BigHash end2 = BigHash.createHashFromString(this.end2.getText());

                    span2 = this.hashSpans.toArray(new HashSpan[0])[1];
                    span2.setFirst(start2);
                    span2.setLast(end2);
                }

                // Clear out the collection
                this.hashSpans.clear();

                // Add the span(s)
                this.hashSpans.add(span1);

                if (this.numHashSpans == 2 && span2 != null) {
                    this.hashSpans.add(span2);
                }

                this.setVisible(false);
                this.dispose();

                this.gui.setHashSpans(this.url, this.hashSpans);
                return;
            } // If cancel
            else if (e.getComponent().equals(this.cancelButton)) {

                // Do nothing... hash span is in correct state.
                this.setVisible(false);
                this.dispose();
                return;
            }

        // TODO Remove hash span #1

        // TODO Remove hash span #2

        }
    } // HashSpanEditor inner class
}
