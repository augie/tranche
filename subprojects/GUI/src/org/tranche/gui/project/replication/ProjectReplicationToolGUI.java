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
package org.tranche.gui.project.replication;

import org.tranche.gui.user.SignInUserButton;
import org.tranche.gui.server.ServersPanel;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import org.tranche.exceptions.TodoException;
import org.tranche.hash.BigHash;
import org.tranche.project.ProjectReplicationTool;
import org.tranche.time.TimeUtil;
import org.tranche.util.Text;
import org.tranche.users.UserZipFile;

/**
 * <p>Tool replicates one or more projects' chunks across selected servers.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ProjectReplicationToolGUI implements ClipboardOwner {

    /**
     * <p>Turn tracers on and off.</p>
     */
    private static final boolean isDebug = true;
    private boolean isRunning = false;

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     * TOOL VARIABLES
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     */
    private static final String title = "Project Replication Tool";
    private static final String description = "Replicates one or more projects across selected servers.";
    private static final Dimension size = new Dimension(600, 650);
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * TOOL COMPONENTS
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     */
    private final JPanel rootPanel;
    private final GenericPopupFrame frame;
    private final JButton executeButton;
    private final SignInUserButton userButton;
    private final AddProjectPanel addProjectPanel;
    private final JTextField replicationsTextField;
    private final ProjectReplicationToolGUIProgressBar progressBar;
    private final JButton clearProjectsButton;
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * MENU COMPONENTS
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     */
    private final JMenuBar menuBar;
    private final JMenu serversMenu;
    private final JMenuItem serversToReadMenuItem;
    private final JMenuItem serversToWriteMenuItem;
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * MENU COMPONENTS
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     */
    private final ServersPanel serversToReadPanel,  serversToWritePanel;
    private final GenericFrame serversToReadFrame,  serversToWriteFrame;
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * TABLE COMPONENTS
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     */
    private final JTable projectsTable;
    private final ProjectsTableModel projectsTableModel;
    /*
     * LISTENERS
     */
    private final ExecutabilityCandidateListener executableListener;

    /**
     * <p>Constructor.</p>
     * @param serversToRead Potential servers to use to read data from. Use can change. Can contain same servers as serversToWrite.
     * @param serversToWrite Potential servers to write data to. Use can change. Can contain same servers as serversToRead.
     */
    public ProjectReplicationToolGUI(final List<String> serversToRead, final List<String> serversToWrite) {
        // Instantiate the components
        rootPanel = new JPanel();
        frame = new GenericPopupFrame(title, rootPanel);
        executeButton = new GenericRoundedButton("Run tool");
        userButton = new SignInUserButton();

        // Instantiate menu components
        menuBar = new GenericMenuBar();
        serversMenu = new GenericMenu("Servers Menu");
        serversToReadMenuItem = new GenericMenuItem("Servers to Check for Copies (read)");
        serversToWriteMenuItem = new GenericMenuItem("Servers to Receive Any Replications (write)");
        replicationsTextField = new JTextField("3");
        progressBar = new ProjectReplicationToolGUIProgressBar();
        clearProjectsButton = new GenericButton("Clear projects");

        // Instantiate table components
        projectsTable = new JTable();
        projectsTableModel = new ProjectsTableModel(projectsTable, ProjectReplicationToolGUI.this);
        projectsTable.setModel(projectsTableModel);

        ProjectPopupMenu popup = new ProjectPopupMenu(projectsTable, projectsTableModel, ProjectReplicationToolGUI.this);
        GenericPopupListener popupListener = new GenericPopupListener(popup);
        popupListener.setDisplayMethod(GenericPopupListener.RIGHT_OR_DOUBLE_CLICK);
        projectsTable.addMouseListener(popupListener);

        // Instantiate listeners
        executableListener = new ExecutabilityCandidateListener(executeButton, userButton, projectsTableModel);

        addProjectPanel = new AddProjectPanel(ProjectReplicationToolGUI.this, executableListener);

        // Servers panels -- one for reading (downloading), one for injecting
        serversToReadPanel = new ServersPanel(serversToRead);
        serversToReadFrame = new GenericFrame();

        serversToReadFrame.setTitle("Servers to Check for Copies (read)");
        serversToReadFrame.setSize(500, 300);
        serversToReadFrame.setLayout(new BorderLayout());
        serversToReadFrame.add(serversToReadPanel, BorderLayout.CENTER);

        serversToWritePanel = new ServersPanel(serversToWrite);
        serversToWriteFrame = new GenericFrame();

        serversToWriteFrame.setTitle("Servers to Receive Any Replications (write)");
        serversToWriteFrame.setSize(500, 300);
        serversToWriteFrame.setLayout(new BorderLayout());
        serversToWriteFrame.add(serversToWritePanel, BorderLayout.CENTER);

        // Build the GUI
        _buildListeners();
        _buildGUI();
    }

    /**
     * <p>Build listeners for components</p>
     */
    private void _buildListeners() {

        clearProjectsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread("Clear projects thread") {

                    @Override()
                    public void run() {
                        int selection = GenericOptionPane.showConfirmDialog(
                                frame,
                                "Really clear all projects from tool?",
                                "Really clear?",
                                JOptionPane.YES_NO_OPTION);

                        if (selection == JOptionPane.NO_OPTION) {
                            return;
                        }

                        Thread t = new Thread("Clear projects thread internal") {

                            @Override()
                            public void run() {
                                projectsTableModel.clearProjects();
                            }
                        };
                        t.setDaemon(true);
                        t.setPriority(Thread.MIN_PRIORITY);
                        SwingUtilities.invokeLater(t);
                    }
                };
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
            }
        });

        serversToReadMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                serversToReadFrame.setVisible(true);
            }
        });

        serversToWriteMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                serversToWriteFrame.setVisible(true);
            }
        });

        userButton.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
//                serversToReadPanel.setUserZipFile(userButton.getUser());
//                serversToWritePanel.setUserZipFile(userButton.getUser());
            }
        });

        executeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread("Put description here") {

                    @Override()
                    public void run() {
                        // Put logic here
                        try {
                            execute();
                        } catch (Exception ex) {
                            ErrorFrame ef = new ErrorFrame();
                            ef.show(ex, frame);
                        }
                    }
                };
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
            }
        });

        // If load a user, might become executable
        userButton.addActionListener(executableListener);
        userButton.addChangeListener(executableListener);

        this.projectsTableModel.addTableModelListener(executableListener);

        this.frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.frame.addWindowListener(new WindowListener() {

            public void windowActivated(WindowEvent e) {
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowDeactivated(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowOpened(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {

                if (GUIUtil.getAdvancedGUI() == null) {
                    Thread t = new Thread("Put description here") {

                        @Override()
                        public void run() {
                            // Put logic here
                            if (isRunning) {
                                int selection = GenericOptionPane.showConfirmDialog(
                                        frame,
                                        "If you close, the replicator will stop. Do you really want to close?",
                                        "Are you sure you want to close?",
                                        JOptionPane.YES_NO_OPTION);

                                if (selection == JOptionPane.NO_OPTION) {
                                    return;
                                }
                            }

                            frame.dispose();
                            System.exit(0);
                        }
                    };
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.start();

                } else {
                    frame.dispose();
                }
            }
        });
    } // _buildListeners

    /**
     * <p>Internal method that builds up the GUI components.</p>
     */
    private void _buildGUI() {
        frame.setSize(size);

        // Set layout manager for GUI
        rootPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Counter for automatic row assignment
        int row = 0;
        final int MARGIN = 10;

        // Build the menu bar
        {
            serversMenu.add(serversToReadMenuItem);
            serversMenu.add(serversToWriteMenuItem);
            menuBar.add(serversMenu);

            gbc.weightx = 1.0;
            gbc.weighty = 0.1;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.fill = GridBagConstraints.BOTH;

            rootPanel.add(menuBar, gbc);
        }

        // Description
        {
            JTextArea area = new JTextArea(description);
            area.setEditable(false);
            area.setWrapStyleWord(true);
            area.setLineWrap(true);
            area.setBackground(rootPanel.getBackground());
            area.setFont(Styles.FONT_11PT_ITALIC);

            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);

            rootPanel.add(area, gbc);
        }

        // User file
        {
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);

            rootPanel.add(userButton, gbc);
        }

        // The project info box: accepts a hash and passphrase
        {
            JLabel label = new JLabel("Add a project to replicate");
            label.setFont(Styles.FONT_14PT_BOLD);

            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2 * MARGIN, MARGIN, 4, MARGIN);

            rootPanel.add(label, gbc);

            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, MARGIN, MARGIN, MARGIN);

            rootPanel.add(addProjectPanel, gbc);
        }

        // The growing project list
        {
            JLabel label = new JLabel("Projects to replicate");
            label.setFont(Styles.FONT_14PT_BOLD);

            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2 * MARGIN, MARGIN, 4, MARGIN);

            rootPanel.add(label, gbc);

            gbc.weightx = 1.0;
            gbc.weighty = 0.4;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, MARGIN, 0, MARGIN);

            rootPanel.add(new GenericScrollPane(projectsTable), gbc);

            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, MARGIN, 0, MARGIN);

            rootPanel.add(clearProjectsButton, gbc);
        }

        // The minimum number of desired replications
        {
            JLabel label = new JLabel("Desired minimum replications");
            label.setFont(Styles.FONT_14PT_BOLD);

            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2 * MARGIN, MARGIN, 4, MARGIN);

            rootPanel.add(label, gbc);

            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, MARGIN, MARGIN, MARGIN);

            rootPanel.add(replicationsTextField, gbc);
        }

        // The progress Bar
        {
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);

            rootPanel.add(progressBar, gbc);
        }

        // The execute button
        {
            gbc.weightx = 1.0;
            gbc.weighty = 0.1;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, MARGIN, MARGIN);

            rootPanel.add(executeButton, gbc);
        }
    } // _buildGUI

    /**
     * 
     * @throws java.lang.Exception
     */
    private void execute() throws Exception {

        isRunning = true;
        long start = TimeUtil.getTrancheTimestamp();

        // Keep reference to execute button text so can change back when done
        final String buttonLabel = this.executeButton.getText();

        try {
            // Change button to show that tool is running
            this.executeButton.setEnabled(false);
            this.executeButton.setText("Running...");
            System.out.println("Executing project replication tool at " + Text.getFormattedDate(start) + ":");

            // Get number of replications
            int replications = -1;
            try {
                replications = Integer.parseInt(this.replicationsTextField.getText());
            } catch (NumberFormatException nfe) { /* nope */ }

            if (replications < 1) {
                GenericOptionPane.showMessageDialog(
                        frame,
                        "You must offer a desired minimum number of replications above zero. (If you are unsure, type the number '3'.)\n\nThe following input is not a positive number: " + this.replicationsTextField.getText(),
                        "Must specify number of replications",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            System.out.println(" * " + replications + " replications");

            // Get servers from which to read
            List<String> serversToRead = new ArrayList();
            serversToRead.addAll(this.serversToReadPanel.getSelectedHosts());

            boolean noServersToRead = false;
            if (serversToRead.size() == 0) {
                noServersToRead = true;
            }

            System.out.println(" * " + serversToRead.size() + " servers for reading:");
            for (String url : serversToRead) {
                System.out.println("   - " + url);
            }

            // Get servers from which to write
            List<String> serversToWrite = new ArrayList();
            serversToWrite.addAll(this.serversToWritePanel.getSelectedHosts());

            System.out.println(" * " + serversToWrite.size() + " servers for writing:");
            for (String url : serversToWrite) {
                System.out.println("   - " + url);
            }

            boolean noServersToWrite = false;
            if (serversToWrite.size() == 0) {
                noServersToWrite = true;
            }

            if (noServersToRead || noServersToWrite) {
                String readServers = String.valueOf(serversToRead.size()) + " ";
                readServers += (serversToRead.size() == 1 ? "server" : "servers");

                String writeServers = String.valueOf(serversToWrite.size()) + " ";
                writeServers += (serversToWrite.size() == 1 ? "server" : "servers");
                GenericOptionPane.showMessageDialog(
                        frame,
                        "There are " + readServers + " to read and " + writeServers + " to write.\n\nMust be at least one server in each pool.\n\nCheck the servers menu and select more servers.\n\nNote that this may happen right after logging in. Sometimes simply viewing the two servers lists solves the problem.",
                        "Check server pools",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get user
            UserZipFile uzf = this.userButton.getUser();
            System.out.println(" * " + uzf.getUserNameFromCert());

            // Get list of projects (hashes and passphrases)
            List<ProjectsEntry> projects = this.projectsTableModel.getProjects();
            System.out.println(" * " + projects.size() + " projects");

            int currentProjectCount = 1;
            final int totalProjectCount = projects.size();
            for (ProjectsEntry project : projects) {

                progressBar.reset();
                progressBar.setPrefixString("Project " + String.valueOf(currentProjectCount) + " of " + String.valueOf(totalProjectCount));

                System.out.println("   - Starting next project: " + project.getHash());

                ProjectReplicationTool replicationTool = new ProjectReplicationTool(uzf.getCertificate(), uzf.getPrivateKey(), serversToRead, serversToWrite);
                replicationTool.setHash(project.getHash());

                if (project.getPassphrase() != null && project.getPassphrase().trim().equals("")) {
                    replicationTool.setPassphrase(project.getPassphrase());
                }

                replicationTool.setNumberRequiredReplications(replications);
                replicationTool.addProjectReplicationToolListener(progressBar);

                replicationTool.execute();

                currentProjectCount++;
            }

        } finally {
            // Flag off running for GUI to assist user
            isRunning = false;

            // Set execute button back
            this.executeButton.setText(buttonLabel);
            executableListener.checkIfExecutable();
        }

        GenericOptionPane.showMessageDialog(
                frame,
                "Tool ran for " + Text.getPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp() - start),
                "Tool finished at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()),
                JOptionPane.INFORMATION_MESSAGE);

    }

    /**
     * <p>Launch the project replication tool, which replicates one or more projects across user-selected servers.</p>
     * @param args Ignored.
     */
    public static void main(String[] args) {
        if (true) {
            throw new TodoException();
        }
//        // configure Tranche network
//        ConfigureTrancheGUI.load(args);
//
//        System.out.println("Waiting to connect to servers...");
//        ServerUtil.waitForStartup();
//
//        List<String> serversToUse = new ArrayList();
//        for (ServerInfo info : ServerUtil.getServers(true)) {
//            serversToUse.add(info.getUrl());
//        }
//
//        ProjectReplicationToolGUI tool = new ProjectReplicationToolGUI(serversToUse, serversToUse);
//        tool.setVisible(true);
    }

    /**
     * 
     * @param visible
     */
    public void setVisible(boolean visible) {
        this.frame.setVisible(visible);
    }

    /**
     * 
     * @return
     */
    public GenericPopupFrame getFrame() {
        return frame;
    }

    /**
     * 
     * @return
     */
    public UserZipFile getUserZipFile() {
        return userButton.getUser();
    }

    /**
     * 
     * @param uzf
     */
    public void setUserZipFile(UserZipFile uzf) {
        userButton.setUser(uzf);
    }

    /**
     * 
     */
    public void addProject(BigHash hash) {
        addProject(hash, null);
    }

    /**
     * 
     * @param hash
     * @param passphrase
     */
    public void addProject(BigHash hash, String passphrase) {
        ProjectsEntry entry = new ProjectsEntry(hash, passphrase);
        projectsTableModel.addProject(entry);

        printTracer("Added project:" + Text.getNewLine() + entry.toString());
    }

    private static void printTracer(String msg) {
        if (isDebug) {
            System.out.println("PROJECT_REPLICATION_TOOL> " + msg);
        }
    }
    // the following are for the clipboardowner abstract

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
} // ProjectReplicationTool

/**
 * 
 * @author besmit
 */
class AddProjectPanel extends JPanel {

    final private JTextField hashField;
    final private JPasswordField passwordField;
    final private JButton addProjectButton;
    final private ProjectReplicationToolGUI replicationTool;
    final private ExecutabilityCandidateListener executableListener;

    public AddProjectPanel(final ProjectReplicationToolGUI replicationTool, ExecutabilityCandidateListener executableListener) {

        // Reference so can add new projects to replication tool
        this.replicationTool = replicationTool;
        this.executableListener = executableListener;

        // Instantiate components
        hashField = new JTextField();
        passwordField = new JPasswordField();
        addProjectButton = new GenericButton("Add Project to Replicate");

        _buildListeners();
        _buildGUI();
    }

    /**
     * <p>Build listeners for components</p>
     */
    private void _buildListeners() {
        addProjectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread("Put description here") {

                    @Override()
                    public void run() {
                        BigHash hash = null;
                        try {
                            hash = BigHash.createHashFromString(hashField.getText());
                        } catch (Exception ex) { /* */ }

                        if (hash == null) {
                            GenericOptionPane.showMessageDialog(
                                    replicationTool.getFrame(),
                                    "Please provide a hash for the project",
                                    "Hash required",
                                    JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        String passphrase = null;
                        try {
                            char[] chars = passwordField.getPassword();

                            if (chars.length > 0) {
                                passphrase = String.valueOf(chars);
                            }
                        } catch (Exception ex) { /* */ }

                        if (passphrase == null) {
                            int selection = GenericOptionPane.showConfirmDialog(
                                    replicationTool.getFrame(),
                                    "No passphrase was provided. Continue?\n\n(If a passphrase is not required, just click \"Yes\" to continue.)",
                                    "No passphrase, continue?",
                                    JOptionPane.YES_NO_OPTION);
                            if (selection == JOptionPane.NO_OPTION) {
                                return;
                            }
                        }

                        // Add passphrase
                        replicationTool.addProject(hash, passphrase);

                        // Clear out fields for next project
                        hashField.setText("");
                        passwordField.setText("");
                    }
                };
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();

            }
        });

        // If add project, might qualify to allow execution
        addProjectButton.addActionListener(executableListener);
    }

    public void _buildGUI() {

        // Layout
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Style
        this.setBorder(Styles.BORDER_BLACK_1);
        this.setBackground(Color.WHITE);

        // Used to calculate row
        int row = 0;

        final int MARGIN = 4;

        // Hash
        {
            JLabel hashLabel = new JLabel("Required: Project Hash");
            hashLabel.setFont(Styles.FONT_11PT_BOLD);

            gbc.weightx = 0.0;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.insets = new Insets(2 * MARGIN, MARGIN, 0, MARGIN);

            this.add(hashLabel, gbc);

            hashField.setBackground(Styles.COLOR_BACKGROUND_LIGHT);

            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);

            this.add(hashField, gbc);
        }

        // Password
        {
            JLabel passwordLabel = new JLabel("Optional: Passphrase (if encrypted)");
            passwordLabel.setFont(Styles.FONT_11PT_BOLD);

            gbc.weightx = 0.0;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.insets = new Insets(2 * MARGIN, MARGIN, 0, MARGIN);

            this.add(passwordLabel, gbc);

            passwordField.setBackground(Styles.COLOR_BACKGROUND_LIGHT);

            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);

            this.add(passwordField, gbc);
        }

        // Add project button
        {
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.insets = new Insets(5 * MARGIN, MARGIN, 2 * MARGIN, MARGIN);

            addProjectButton.setFont(Styles.FONT_11PT_BOLD);

            this.add(addProjectButton, gbc);
        }
    }
}

/**
 * 
 */
class ProjectsTableModel extends AbstractTableModel {

    private final String[] columnNames = {"Hash", "Passphrase"};
    private final List<ProjectsEntry> rows = new ArrayList();
    private final JTable table;
    private final ProjectReplicationToolGUI replicationTool;

    public ProjectsTableModel(JTable table, ProjectReplicationToolGUI replicationTool) {
        this.table = table;
        this.replicationTool = replicationTool;
    }

    /**
     * 
     * @param columnIndex
     * @return
     */
    @Override()
    public Class getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return BigHash.class;
        }
        if (columnIndex == 1) {
            return String.class;
        }
        throw new RuntimeException("Only two columns, requested column index of " + columnIndex);
    }

    /**
     * 
     * @return
     */
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     * 
     * @param columnIndex
     * @return
     */
    @Override()
    public String getColumnName(int columnIndex) {

        if (columnIndex > columnNames.length) {
            throw new RuntimeException("Only two columns, requested column index of " + columnIndex);
        }

        return columnNames[columnIndex];
    }

    /**
     * 
     * @return
     */
    public int getRowCount() {
        return rows.size();
    }

    /**
     * 
     * @param rowIndex
     * @param columnIndex
     * @return
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex > getRowCount() || columnIndex > columnNames.length) {
            return null;
        }

        if (columnIndex == 0) {
            return rows.get(rowIndex).getHash();
        } else if (columnIndex == 1) {
            return rows.get(rowIndex).getDisplayablePassphraseMask();
        }

        throw new RuntimeException("Should never get here, column index: " + columnIndex);
    }

    /**
     * 
     * @param row
     * @return
     */
    public BigHash getHash(int row) {
        if (row > getRowCount()) {
            return null;
        }

        return rows.get(row).getHash();
    }

    /**
     * 
     * @param rowIndex
     * @param columnIndex
     * @return
     */
    @Override()
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /**
     * 
     * @param aValue
     * @param rowIndex
     * @param columnIndex
     */
    @Override()
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException("use addProject(ProjectsEntry)");
    }

    /**
     * 
     * @param entry
     */
    public void addProject(ProjectsEntry entry) {
        rows.add(entry);

        this.fireTableDataChanged();
    }

    public void removeSelectedProjects(int[] rowValues) {

        for (int row : rowValues) {
            System.out.print(row + " ");
        }
        System.out.println("");

        // Reverse sort so removing doesn't effect ordering
        List<Integer> orderedRows = new ArrayList();
        for (int row : rowValues) {
            orderedRows.add(row);
        }
        Collections.sort(orderedRows);

        // Remove rows, starting with higher values first
        for (int i = orderedRows.size() - 1; i >= 0; i--) {
            int row = orderedRows.get(i);
            rows.remove(row);
        }
        this.fireTableDataChanged();
    }

    public void setPassphrase(String passphrase, int row) {

        if (row >= this.getRowCount()) {
            return;
        }

        ProjectsEntry entry = this.rows.get(row);
        entry.setPassphrase(passphrase);
        this.fireTableDataChanged();
    }

    /**
     * 
     * @return
     */
    public List<ProjectsEntry> getProjects() {
        return Collections.unmodifiableList(rows);
    }

    /**
     * 
     */
    public void clearProjects() {
        rows.clear();
        this.fireTableDataChanged();
    }
}

/**
 * 
 */
class ProjectsEntry {

    private BigHash hash;
    private String passphrase;

    public ProjectsEntry(BigHash hash, String passphrase) {
        this.hash = hash;
        this.passphrase = passphrase;
    }

    public BigHash getHash() {
        return hash;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public String getPassphrase() {
        return this.passphrase;
    }

    public String getDisplayablePassphraseMask() {
        if (passphrase == null) {
            return "(none)";
        } else {
            char[] asterisk = new char[passphrase.length()];
            for (int i = 0; i < asterisk.length; i++) {
                asterisk[i] = '*';
            }
            return String.valueOf(asterisk);
        }
    }

    @Override()
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("HASH:       " + hash.toString() + Text.getNewLine());
        buffer.append("PASSPHRASE: " + getDisplayablePassphraseMask());

        return buffer.toString();
    }
}

/**
 * 
 * @author besmit
 */
class ProjectPopupMenu extends JPopupMenu {

    private final JMenuItem copyHashMenuItem;
    private final JMenuItem editPassphraseMenuItem;
    private final JMenuItem removeProjectsMenuItem;
    private final JTable projectsTable;
    private final ProjectsTableModel projectsModel;
    private final ProjectReplicationToolGUI replicationTool;

    public ProjectPopupMenu(final JTable projectsTable, final ProjectsTableModel projectsTableModel, final ProjectReplicationToolGUI replicationTool) {

        this.projectsTable = projectsTable;
        this.projectsModel = projectsTableModel;
        this.replicationTool = replicationTool;

        this.copyHashMenuItem = new JMenuItem("Copy hash to clipboard");
        this.editPassphraseMenuItem = new JMenuItem("Edit selected passphrase");
        this.removeProjectsMenuItem = new JMenuItem("Remove selected project(s)");

        setBorder(Styles.BORDER_BLACK_1);

        copyHashMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread("Put description here") {

                    @Override()
                    public void run() {
                        // Put logic here
                        copyHashToClipboard();
                    }
                };
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
            }
        });
        removeProjectsMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread("Put description here") {

                    @Override()
                    public void run() {
                        // Put logic here
                        removeSelectedRows();
                    }
                };
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
            }
        });
        editPassphraseMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread("Put description here") {

                    @Override()
                    public void run() {
                        // Put logic here
                        editSelectedPassphrase();
                    }
                };
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
            }
        });

        add(copyHashMenuItem);
        add(editPassphraseMenuItem);
        add(removeProjectsMenuItem);

        addPopupMenuListener(
                new PopupMenuListener() {

                    public void popupMenuCanceled(PopupMenuEvent e) {
                    }

                    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                        Thread t = new Thread("Popup visible thread") {

                            @Override()
                            public void run() {
                                try {
                                    int[] rows = projectsTable.getSelectedRows();
                                    copyHashMenuItem.setEnabled(rows.length == 1);
                                    editPassphraseMenuItem.setEnabled(rows.length == 1);
                                    removeProjectsMenuItem.setEnabled(rows.length >= 1);

                                } catch (Exception ex) {
                                    System.err.println("Problem with popup menu: " + ex.getMessage());
                                    ex.printStackTrace(System.err);
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
     * 
     */
    private void copyHashToClipboard() {
        int row = this.projectsTable.getSelectedRow();
        BigHash hash = this.projectsModel.getHash(row);
        if (hash == null) {
            return;
        }
        GUIUtil.copyToClipboard(hash.toString(), replicationTool);
    }

    /**
     * 
     */
    private void removeSelectedRows() {
        int[] rowValues = this.projectsTable.getSelectedRows();
        this.projectsModel.removeSelectedProjects(rowValues);
    }

    /**
     * 
     */
    private void editSelectedPassphrase() {
        final int row = this.projectsTable.getSelectedRow();
        String passphrase = JOptionPane.showInputDialog(replicationTool.getFrame(), "Please supply a new passphrase:");
        if (passphrase.trim().equals("")) {
            passphrase = null;
        }

        this.projectsModel.setPassphrase(passphrase, row);
    }
}

/**
 * 
 * @author besmit
 */
class ExecutabilityCandidateListener implements ActionListener, ChangeListener, TableModelListener {

    private final JButton executeButton;
    private final SignInUserButton userButton;
    private final ProjectsTableModel tableModel;

    public ExecutabilityCandidateListener(JButton executeButton, SignInUserButton userButton, ProjectsTableModel tableModel) {
        this.executeButton = executeButton;
        this.userButton = userButton;
        this.tableModel = tableModel;

        // Fire off event to determine whether executable. Might be if right variable set, but probably not.
        checkIfExecutable();
    }

    public void actionPerformed(ActionEvent e) {
        checkIfExecutable();
    }

    public void stateChanged(ChangeEvent e) {
        checkIfExecutable();
    }

    public void tableChanged(TableModelEvent e) {
        checkIfExecutable();
    }

    public void checkIfExecutable() {
        boolean isEnabled = this.userButton.getUser() != null && tableModel.getProjects().size() > 0;
        this.executeButton.setEnabled(isEnabled);
    }
}