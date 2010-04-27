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
package org.tranche.gui.log;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.tranche.commons.TextUtil;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.GenericTextArea;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.Styles;
import org.tranche.server.logs.ActionByte;
import org.tranche.server.logs.LogEntry;
import org.tranche.server.logs.LogReader;

/**
 * A very simple binary log file reader. Very simple.
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class SimpleLogReaderPanel extends JPanel {

    final public static Dimension RECOMMENDED_DIMENSION = new Dimension(500, 500);
    final private JButton loadLogButton;
    final JLabel logFileNameLabel;
    final JLabel logFileDateLabel;
    final JLabel logFileNumEntriesLabel;
    final JLabel logFileUploadedLabel;
    final JLabel logFileDownloadedLabel;
    final JLabel logFileNumClientsLabel;
    final JTextArea logFileDataArea;
    /**
     * Every time file loaded, set file here.
     */
    private File logFile = null;

    /**
     * Make this stand-alone.
     */
    public static void main(String[] args) {
        GenericPopupFrame popup = new GenericPopupFrame("Simple Server Binary Log File Reader", new SimpleLogReaderPanel());
        popup.setSizeAndCenter(SimpleLogReaderPanel.RECOMMENDED_DIMENSION);
        popup.setVisible(true);
    }

    /** Creates a new instance of NewClass */
    public SimpleLogReaderPanel() {

        // Initialize components
        loadLogButton = new JButton("Load log file...");
        logFileNameLabel = new GenericLabel("* File: No file selected.");
        logFileDateLabel = new GenericLabel("* Date: No file selected.");
        logFileNumEntriesLabel = new GenericLabel("* Entries: No file selected.");
        logFileNumClientsLabel = new GenericLabel("* Clients: No file selected.");
        this.logFileUploadedLabel = new GenericLabel("* Uploaded: No file selected.");
        this.logFileDownloadedLabel = new GenericLabel("* Downloaded: No file selected.");
        logFileDataArea = new GenericTextArea();

        // Set the look
        setBackground(Styles.COLOR_BACKGROUND);
        setLayout(new GridBagLayout());

        int row = 0;
        final int MARGIN = 10;

        GridBagConstraints gbc = new GridBagConstraints();

        // Open log file
        {
            this.loadLogButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    loadFile();
                }
            });

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.loadLogButton, gbc);
        }

        // Show meta information
        {
            // File name
            this.logFileNameLabel.setFont(Styles.FONT_12PT_BOLD);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.logFileNameLabel, gbc);

            // File date
            this.logFileDateLabel.setFont(Styles.FONT_12PT_BOLD);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN * .25), MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.logFileDateLabel, gbc);

            // File entries
            this.logFileNumEntriesLabel.setFont(Styles.FONT_12PT_BOLD);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN * .25), MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.logFileNumEntriesLabel, gbc);

            // Uploaded
            this.logFileUploadedLabel.setFont(Styles.FONT_12PT_BOLD);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN * .25), MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.logFileUploadedLabel, gbc);

            // Downloaded
            this.logFileDownloadedLabel.setFont(Styles.FONT_12PT_BOLD);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN * .25), MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.logFileDownloadedLabel, gbc);

            // Clients
            this.logFileNumClientsLabel.setFont(Styles.FONT_12PT_BOLD);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN * .25), MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.logFileNumClientsLabel, gbc);
        }

        // Show information
        {
            // Area should be non-editable and wrap
            this.logFileDataArea.setEditable(false);
            this.logFileDataArea.setLineWrap(true);
//            this.logFileDataArea.setBorder(Styles.BORDER_BLACK_1);
            this.logFileDataArea.setBackground(Color.WHITE);
            this.logFileDataArea.setFont(Styles.FONT_10PT_MONOSPACED);
            this.logFileDataArea.setText("No file loaded.");
            this.logFileDataArea.setWrapStyleWord(true);
            this.logFileDataArea.setBorder(Styles.BORDER_NONE);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
//            gbc.fill = GridBagConstraints.VERTICAL;
            gbc.insets = new Insets((int) (MARGIN * 1.5), MARGIN, (int) (MARGIN * 1.5), MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            GenericScrollPane scroller = new GenericScrollPane(this.logFileDataArea);
            scroller.setBorder(Styles.BORDER_NONE);
            this.add(scroller, gbc);
        }
    }

    /**
     *
     */
    private void loadFile() {

        JFileChooser chooser = GUIUtil.makeNewFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(chooser.FILES_ONLY);
        int result = chooser.showOpenDialog(SimpleLogReaderPanel.this.getParent());

        if (result != chooser.APPROVE_OPTION) {
            // Bail
            return;
        }

        this.logFile = chooser.getSelectedFile();
        parseLogFile(this.logFile);
    }

    /**
     *
     */
    private void parseLogFile(final File file) {
        final String oldButtonLabel = this.loadLogButton.getText();
        Thread t = new Thread() {

            public void run() {

                // Change button label
                loadLogButton.setText("Reading, please wait...");
                loadLogButton.setEnabled(false);
                loadLogButton.repaint();

                // Keep track of aggregate information
                int entriesCount = 0;
                long downloaded = 0;
                long uploaded = 0;

                // Holds client IP addresses
                Set<String> clients = new HashSet();

                LogReader reader = null;

                try {
                    // Clear out old data
                    logFileDataArea.setText("");

                    reader = new LogReader(file);
                    LogEntry next;
                    while (reader.hasNext()) {
                        next = reader.next();
                        entriesCount++;

                        // Build the entry string
                        StringBuffer buffer = new StringBuffer();
                        buffer.append(entriesCount + ". ");

                        byte action = next.getAction();

                        switch (action) {
                            case ActionByte.IPv4_GetConfig:
                            case ActionByte.IPv6_GetConfig:
                                buffer.append("GET CONFIG: ");
                                break;
                            case ActionByte.IPv4_GetData:
                            case ActionByte.IPv6_GetData:
                                buffer.append("GET DATA: ");
                                downloaded += next.getHash().getLength();
                                break;
                            case ActionByte.IPv4_GetMeta:
                            case ActionByte.IPv6_GetMeta:
                                buffer.append("GET META: ");
                                downloaded += next.getHash().getLength();
                                break;
                            case ActionByte.IPv4_GetNonce:
                            case ActionByte.IPv6_GetNonce:
                                buffer.append("GET NONCE: ");
                                downloaded += 8;
                                break;
                            case ActionByte.IPv4_SetConfig:
                            case ActionByte.IPv6_SetConfig:
                                buffer.append("SET CONFIG: ");
                                break;
                            case ActionByte.IPv4_SetData:
                            case ActionByte.IPv6_SetData:
                                uploaded += next.getHash().getLength();
                                buffer.append("SET DATA: ");
                                break;
                            case ActionByte.IPv4_SetMeta:
                            case ActionByte.IPv6_SetMeta:
                                uploaded += next.getHash().getLength();
                                buffer.append("SET META: ");
                                break;
                        }

                        String ip = next.getClientIP();
                        buffer.append(ip);
                        clients.add(ip);

                        logFileDataArea.append(buffer.toString() + "\n");
                    }

                    // Set the file name
                    logFileNameLabel.setText("* File: " + file.getAbsolutePath());

                    // Set the date
                    try {
                        long timestamp = Long.parseLong(file.getName().substring(0, file.getName().indexOf(".")));
                        logFileDateLabel.setText("* Date: " + TextUtil.getFormattedDate(timestamp));
                    } catch (Exception fex) {
                        logFileDateLabel.setText("* Date: not found (should be in file name)");
                    }

                    // Set the number of entries
                    logFileNumEntriesLabel.setText("* Entries: " + entriesCount);

                    // Set uploaded and downloaded
                    logFileUploadedLabel.setText("* Uploaded: " + TextUtil.formatBytes(uploaded));
                    logFileDownloadedLabel.setText("* Downloaded: " + TextUtil.formatBytes(downloaded));
                    logFileNumClientsLabel.setText("* Clients: " + clients.size());
                } catch (Exception ex) {
                    ErrorFrame ef = new ErrorFrame();
                    ef.show(ex, SimpleLogReaderPanel.this.getParent());
                } finally {

                    // Close streams
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception ignore) {
                        }
                    }

                    // Reset button so can use again
                    loadLogButton.setText(oldButtonLabel);
                    loadLogButton.setEnabled(true);

                    // Done, update GUI
                    SimpleLogReaderPanel.this.repaint();
                }
            }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
}
