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
package org.tranche.gui.get;

import org.tranche.gui.util.GUIUtil;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import org.tranche.get.GetFileTool;
import org.tranche.gui.GenericTextArea;
import org.tranche.gui.GenericButton;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.IndeterminateProgressBar;
import org.tranche.gui.Styles;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.tasks.CSVFile;
import org.tranche.tasks.CSVReader;
import org.tranche.util.PreferencesUtil;

/**
 * <p>Popup panel for uploading from a CSV file.</p>
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class CSVDownloadPanel extends JPanel {

    private int MARGIN = 10;
    private File file = null;
    // Reference to container for popups and alerts
    public JFrame frame = null;
    /**
     * <p>Recommended size for this popup panel when used with a generic popup frame.</p>
     */
    public static final Dimension RECOMMENDED_DIMENSION = new Dimension(500, 205);

    public CSVDownloadPanel() {
        setBackground(Styles.COLOR_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Aids correct word wrap
        setSize(RECOMMENDED_DIMENSION);

        // Build entire GUI
        {
            String description = "Reads in a comma-separated file and downloads all the files. ";
            JTextArea aboutDescription = new GenericTextArea(description);
            aboutDescription.setFont(Styles.FONT_12PT);
            aboutDescription.setEditable(false);
            aboutDescription.setLineWrap(true);
            aboutDescription.setWrapStyleWord(true);
            aboutDescription.setBackground(Styles.COLOR_BACKGROUND);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = 0; // <-- Row

            this.add(aboutDescription, gbc);

            final JButton fileButton = new GenericButton("Please choose a file...");
            fileButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            file = GUIUtil.userSelectFileForRead("Select CSV File", frame);

                            if (file != null) {
                                fileButton.setText(file.getAbsolutePath());
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = 1; // <-- Row

            this.add(fileButton, gbc);

            JButton downloadButton = new GenericButton("Download Data Sets");
            downloadButton.setFont(Styles.FONT_12PT_BOLD);
            downloadButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            if (file == null) {
                                GenericOptionPane.showMessageDialog(
                                        frame,
                                        "You must select a comma-separated file first.",
                                        "Please select a CSV file",
                                        JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }

                            try {
                                parseAndDownload();
                            } catch (Exception ex) {
                                GUIUtil.getAdvancedGUI().ef.show(ex, frame);
                            } finally {
                                frame.dispose();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.start();
                }
            });

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            gbc.weightx = .5;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN * 2, MARGIN, (int) (MARGIN * 1.5), MARGIN);
            gbc.gridx = 1; // <-- Cell
            gbc.gridy = 2; // <-- Row

            this.add(downloadButton, gbc);
        }
    }

    private void parseAndDownload() {

        frame.setVisible(false);
        final IndeterminateProgressBar progress = new IndeterminateProgressBar("Reading file...");
        progress.setLocationRelativeTo(frame);

        Thread t = new Thread() {

            @Override
            public void run() {
                int count = -1;
                try {
                    count = parseAndDownloadFilesFromCSVInternal(progress, CSVDownloadPanel.this.file, true);
                } catch (Exception ex) {
                    GUIUtil.getAdvancedGUI().ef.show(ex, frame);
                } finally {
                    if (count == 0) {
                        GenericOptionPane.showMessageDialog(
                                frame,
                                "No data sets were found in your CSV file.\nMake sure your fields are marked or that you selected the correct field for the hash.",
                                "No Data Sets Found",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                    progress.stop();
                    frame.dispose();
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    /**
     * Internal method. Parses CSV and downloads files using GUI.
     * @param progress An indeterminate progress bar to start when downloading files. If null, ignored.
     * @param file The CSV file to parse.
     * @param shouldPromptUserForColumns True if should ask user to identify columns if tool can't identify. If false, will throw exceptions if can't determine critical information
     */
    private static int parseAndDownloadFilesFromCSVInternal(IndeterminateProgressBar progress, File file, boolean shouldPromptUserForColumns) throws Exception {
        // If no downloads, user should know...
        int count = 0;

        // Get the file
        CSVReader reader = new CSVReader(file);
        CSVFile csv = reader.read();

        // Find the passphrase and BigHash keys
        String passphraseKey = null, hashKey = null, regexKey = null, subDirKey = null;

        for (String key : csv.getLegend()) {

            if (key.toLowerCase().contains("pass")) {
                passphraseKey = key;
            } else if (key.toLowerCase().contains("hash")) {
                hashKey = key;
            } else if (key.toLowerCase().contains("regex")) {
                regexKey = key;
            } else if (key.toLowerCase().contains("subdir")) {
                subDirKey = key;
            }
        }

        // BigHash key MUST be present
        if (hashKey == null && !shouldPromptUserForColumns) {
            throw new Exception("Could not identify the hash column. The CSV is missing the legend.");
        } else if (hashKey == null && shouldPromptUserForColumns) {

            StringCallback callback = new StringCallback();

            SelectColumnPanel selectPanel = new SelectColumnPanel("Could not determine the hash field. Please select:", csv.getLegend(), callback, "Cancel download");
            GenericPopupFrame frame = new GenericPopupFrame("Select Hash Field (Required)", selectPanel);
            frame.setSizeAndCenter(SelectColumnPanel.RECOMMENDED_DIMENSION);
            frame.setVisible(true);
            selectPanel.setFrame(frame);

            hashKey = callback.receiveString();

            // User bailed
            if (hashKey == null) {
                return -1;
            }
        }

        if (passphraseKey == null && shouldPromptUserForColumns) {

            StringCallback callback = new StringCallback();

            SelectColumnPanel selectPanel = new SelectColumnPanel("Could not determine the password field. If none, select \"Skip\"", csv.getLegend(), callback, "Skip");
            GenericPopupFrame frame = new GenericPopupFrame("Select Password Field (Optional)", selectPanel);
            frame.setSizeAndCenter(SelectColumnPanel.RECOMMENDED_DIMENSION);
            frame.setVisible(true);
            selectPanel.setFrame(frame);

            passphraseKey = callback.receiveString();
        }

        if (regexKey == null && shouldPromptUserForColumns) {

            StringCallback callback = new StringCallback();

            SelectColumnPanel selectPanel = new SelectColumnPanel("Could not determine the regular expression field. If none, select \"Skip\"", csv.getLegend(), callback, "Skip");
            GenericPopupFrame frame = new GenericPopupFrame("Select Regex Field (Optional)", selectPanel);
            frame.setSizeAndCenter(SelectColumnPanel.RECOMMENDED_DIMENSION);
            frame.setVisible(true);
            selectPanel.setFrame(frame);

            regexKey = callback.receiveString();
        }

        // If passed a progress bar, start it
        if (progress != null) {
            progress.start();
        }

        // Set view to download panel
        GUIUtil.getAdvancedGUI().mainPanel.setTab(GUIUtil.getAdvancedGUI().mainPanel.downloadsPanel);

        // Get each record
        for (Map<String, String> record : csv.getRecords()) {
            try {
                // Try to make big hash. If fail, skip line (not an entry)
                BigHash hash = null;
                try {
                    String hashStr = record.get(hashKey);

                    // If in quotes, remove
                    hashStr = stripQuotes(hashStr);

                    hash = BigHash.createHashFromString(hashStr);
                } catch (Exception e) {
                    continue;
                }

                // Don't try to get a passphrase if no column
                String passphrase = null;
                if (passphraseKey != null) {
                    passphrase = record.get(passphraseKey);

                    // If in quotes, remove
                    passphrase = stripQuotes(passphrase);
                }

                // Don't try to get regex if no column
                String regex = null;
                if (regexKey != null) {
                    regex = record.get(regexKey);
                    regex = stripQuotes(regex);

                    // If nothing, set to everything
                    if (regex.trim().equals("")) {
                        regex = "*";
                    }
                }

                // Get the subdirectory
                String subDir = null;
                if (subDirKey != null) {
                    subDir = record.get(subDirKey);
                    subDir = stripQuotes(subDir);
                }

                // error check
                if (subDir == null) {
                    subDir = PreferencesUtil.getFile(PreferencesUtil.PREF_DOWNLOAD_FILE).getAbsolutePath();
                }

                // If passphrase doesn't exist, check MetaData for public passphrase

                if (passphrase == null || passphrase.trim().equals("")) {

                    final String[] passphraseCheck = new String[1];
                    passphraseCheck[0] = null;

                    final BigHash h = hash;
                    Thread t = new Thread("") {

                        @Override
                        public void run() {
                            try {
                                GetFileTool gft = new GetFileTool();
                                gft.setHash(h);
                                MetaData md = gft.getMetaData();

                                if (md.isEncrypted()) {
                                    passphraseCheck[0] = md.getPublicPassphrase();
                                }
                            } catch (Exception skip) { /* */ }
                        }
                    };
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.setDaemon(true);
                    t.start();

                    t.join(30 * 1000);
                    t.interrupt();

                    if (passphraseCheck[0] != null) {
                        passphrase = passphraseCheck[0];
                    }
                }

                count++;
                GetFileTool gft = new GetFileTool();
                gft.setHash(hash);
                gft.setPassphrase(passphrase);
                gft.setRegEx(regex);
                gft.setSaveFile(new File(subDir));
                DownloadPool.set(new DownloadSummary(gft));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return count;
    }

    /**
     * Downloads projects from a CSV on a separate thread. Returns immediately.
     * @param file The CSV file.
     * @return The number of projects downloaded.
     */
    public static int parseAndDownloadFilesFromCSV(File file) throws Exception {
        return parseAndDownloadFilesFromCSVInternal(null, file, false);
    }

    /**
     * Helper method to remove quotes, if any
     */
    private static String stripQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }

        return str;
    }
}

/**
 * Used to allow user to select a column as a certain field.
 */
class SelectColumnPanel extends JPanel {

    private JList optionList;
    private JButton selectButton,  cancelButton;
    public static final Dimension RECOMMENDED_DIMENSION = new Dimension(300, 300);
    private static int MARGIN = 10;
    private JFrame frame;

    public SelectColumnPanel(String msg, List<String> legend, final StringCallback callback, String cancelButtonLabel) {

        // Build the list
        this.optionList = new JList(legend.toArray());
        this.optionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.optionList.setLayoutOrientation(JList.VERTICAL);
        this.optionList.setFont(Styles.FONT_12PT);
        this.optionList.setSelectedIndex(0);
        this.optionList.setDragEnabled(false);

        // Build the buttons
        this.selectButton = new JButton("Select");
        this.selectButton.setFont(Styles.FONT_12PT_BOLD);

        this.cancelButton = new JButton(cancelButtonLabel);
        this.cancelButton.setFont(Styles.FONT_12PT);

        // Set the layout
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Aids correct word wrap
        this.setSize(RECOMMENDED_DIMENSION);

        // Description
        {
            JTextArea description = new GenericTextArea(msg);
            description.setFont(Styles.FONT_12PT);
            description.setEditable(false);
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setBackground(Styles.COLOR_BACKGROUND);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = 0; // <-- Row

            this.add(description, gbc);
        }

        // List
        {
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = 1; // <-- Row

            add(new GenericScrollPane(this.optionList), gbc);
        }

        // Button
        {

            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            gbc.weightx = .5;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, (int) (MARGIN * 1.5), 0);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = 2; // <-- Row

            add(this.cancelButton, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            gbc.weightx = .5;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, (int) (MARGIN * 1.5), MARGIN);
            gbc.gridx = 1; // <-- Cell
            gbc.gridy = 2; // <-- Row

            add(this.selectButton, gbc);
        }

        // Listeners
        this.selectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                callback.sendString(optionList.getSelectedValue().toString());
                if (frame != null) {
                    frame.setVisible(false);
                    frame.dispose();
                }
            }
        });

        this.cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                callback.sendString(null);
                if (frame != null) {
                    frame.setVisible(false);
                    frame.dispose();
                }
            }
        });

    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }
} // SelectColumnPanel

class StringCallback {

    private String string;
    private boolean isFinished = false;

    /**
     * Sends the caller a string.
     */
    public void sendString(String string) {
        this.string = string;
        isFinished = true;
    }

    /**
     * A blocking method. Waits for string callback.
     */
    public String receiveString() {
        while (!isFinished) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
            }
            Thread.yield();
        }

        return string;
    }

    public boolean isFinished() {
        return isFinished;
    }
}

