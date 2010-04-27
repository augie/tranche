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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.GenericTextField;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.gui.Styles;
import org.tranche.hash.BigHash;
import org.tranche.security.SecurityUtil;
import org.tranche.security.Signature;
import org.tranche.server.logs.LogEntry;
import org.tranche.server.logs.LogWriter;
import org.tranche.time.TimeUtil;
import org.tranche.commons.RandomUtil;
import org.tranche.commons.TextUtil;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class RandomLogGeneratorPanel extends JPanel {

    /**
     * Average ms/byte for log creation.
     */
    private static final double estimatedMSPerByte = (double) (90 * 1000) / (double) (1024 * 1024);
    final public static Dimension RECOMMENDED_DIMENSION = new Dimension(500, 390);
    final private JTextField // The size of the log file in bytes
            logSizeField, // The time delta for the file in milliseconds
            logDurationField, // The number of client IPs to generate and use
            logClientsField, // Time offset so can make logs in past or future
            logTimeOffsetField;
    final private JButton createLogButton;

    /**
     * Make it stand-alone.
     */
    public static void main(String[] args) {
        GenericPopupFrame popup = new GenericPopupFrame("Simple Server Random Binary Log Generator", new RandomLogGeneratorPanel());
        popup.setSizeAndCenter(RandomLogGeneratorPanel.RECOMMENDED_DIMENSION);
        popup.setVisible(true);
    }

    /**
     * Make a single log file.
     * @param destDir The directory to hold the file. Since logs have standard names, you just choose the directory.
     * @param logSize The size, in bytes, of the log file. Larger files have more entries.
     * @param timeDelta The duration, in milliseconds, for the log. Generally choose an hour, which is 3,600,000
     * @param timeOffset Time, in milliseconds, to offset the log's time. Negative will create past logs, positive will create future logs.
     * @param clients Number of clients to generate.
     * @return The File representing the generated log.
     */
    public static File generateLog(
            File destDir,
            long logSize,
            long timeDelta,
            long timeOffset,
            int clients) throws Exception {

        // Start and finish of log period
//        long finish = TimeUtil.getTrancheTimestamp()+timeOffset;
//        long start = finish - timeDelta;

        long start = TimeUtil.getTrancheTimestamp() + timeOffset;
        long finish = start + timeDelta;

        System.out.println("The log file will start on " + TextUtil.getFormattedDate(start) + " and finish on " + TextUtil.getFormattedDate(finish));

        // Create the log
        String logName = String.valueOf(finish + 1000) + ".log";
        File logFile = new File(destDir, logName);
        logFile.createNewFile();

        LogWriter writer = null;
        try {
            // Create the writer
            writer = new LogWriter(logFile);

            // Get the IPs
            List<String> IPs = generateClientIPs(clients);

            long size = 0;
            LogEntry next;
            while (size < logSize) {
                next = generateRandomEntry(IPs, start, finish);
                writer.writeEntry(next);
                size += next.length();
            }

            return logFile;
        } finally {
            // Reset the chance for subsequent runs
            chance = Integer.MIN_VALUE;
            chanceIsDownload = true;

            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * Some common and random values.
     */
    private void setSomeDefaults() {
        // Set the duration to 3,600,000. User can change
        logDurationField.setText("3,600,000");

        // Set time offset to 0. User can change.
        logTimeOffsetField.setText("0");

        // Set somewhere between 1 and 25 clients
        int randomClientCount = 1 + RandomUtil.getInt(25);
        logClientsField.setText(String.valueOf(randomClientCount));

        // Set somewhere between 64KB and 256KB log file
        int randomLogSize = 64 * 1024 + RandomUtil.getInt(192 * 1024 + 1);
        logSizeField.setText(String.valueOf(randomLogSize));
    }

    public RandomLogGeneratorPanel() {

        // Init the components
        logSizeField = new GenericTextField();
        logDurationField = new GenericTextField();
        this.logTimeOffsetField = new GenericTextField();
        this.logClientsField = new GenericTextField();
        this.createLogButton = new JButton("Create log file");

        // Set the look
        setBackground(Styles.COLOR_BACKGROUND);
        setLayout(new GridBagLayout());

        int row = 0;
        final int MARGIN = 10;

        GridBagConstraints gbc = new GridBagConstraints();

        // Size of log in bytes
        {
            JLabel l = new GenericLabel("Size of log (in bytes):");
            l.setFont(Styles.FONT_12PT_BOLD);
            l.setBorder(Styles.UNDERLINE_BLACK);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN * 1.5), MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(l, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.logSizeField, gbc);
        }

        // Time delta for log in millis
        {
            JLabel l = new GenericLabel("Duration of log (in milliseconds):");
            l.setFont(Styles.FONT_12PT_BOLD);
            l.setBorder(Styles.UNDERLINE_BLACK);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN * 1.5), MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(l, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.logDurationField, gbc);
        }

        // Time offset field
        {
            JLabel l = new GenericLabel("Time offset for past/future logs (-/+ in milliseconds):");
            l.setFont(Styles.FONT_12PT_BOLD);
            l.setBorder(Styles.UNDERLINE_BLACK);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN * 1.5), MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(l, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.logTimeOffsetField, gbc);
        }

        // The number of client IP addresses
        {
            JLabel l = new GenericLabel("Number of unique client IPs:");
            l.setFont(Styles.FONT_12PT_BOLD);
            l.setBorder(Styles.UNDERLINE_BLACK);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN * 1.5), MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(l, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.logClientsField, gbc);
        }

        // Button to generate
        {
            this.createLogButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        public void run() {
                            generateLog();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN * 2), MARGIN, (int) (MARGIN * 2), MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.createLogButton, gbc);
        }
        // Generate some default values...
        setSomeDefaults();
    }

    /**
     *
     */
    private void generateLog() {

        String oldLabel = this.createLogButton.getText();

        this.createLogButton.setText("Please wait...");
        this.createLogButton.setEnabled(false);
        this.createLogButton.repaint();

        long timeDelta, logSize, timeOffset;
        int clients;
        try {
            // Read in user values. If NumberFormatException, do it now
            // Note: all numbers can contain commas, so strip out
            String sizeStr = this.logSizeField.getText();
            sizeStr = sizeStr.replaceAll(",", "");

            String timeStr = this.logDurationField.getText();
            timeStr = timeStr.replaceAll(",", "");

            String clientsStr = this.logClientsField.getText();
            clientsStr = clientsStr.replaceAll(",", "");

            String timeOffsetStr = this.logTimeOffsetField.getText();
            timeOffsetStr = timeOffsetStr.replaceAll(",", "");

            // Parse out values from the strings
            logSize = Long.parseLong(sizeStr);
            timeDelta = Long.parseLong(timeStr);
            clients = Integer.parseInt(clientsStr);

            if (timeOffsetStr.startsWith("+")) {
                timeOffsetStr = timeOffsetStr.substring(1);
            }
            timeOffset = Long.parseLong(timeOffsetStr);

            // Get the estimated time for 1.33GHz PPC
            double estimatedMS = Math.abs(logSize * estimatedMSPerByte);
            int c = GenericOptionPane.showConfirmDialog(
                    RandomLogGeneratorPanel.this.getParent(),
                    "This would take approximately " + TextUtil.formatTimeLength((long) estimatedMS) + " on a 1.33GHz PPC. Continue?",
                    "Continue?",
                    JOptionPane.YES_NO_OPTION);

            if (c != JOptionPane.YES_OPTION) {
                // Bail
                return;
            }

            // Ask user where to put the log
            JFileChooser chooser = GUIUtil.makeNewFileChooser();
            chooser.setMultiSelectionEnabled(false);
            chooser.setDialogTitle("Chooser directory in which to place log file");
            chooser.setFileSelectionMode(chooser.DIRECTORIES_ONLY);

            int r = chooser.showSaveDialog(RandomLogGeneratorPanel.this.getParent());

            if (r != chooser.APPROVE_OPTION) {
                // bail
                return;
            }

            File logFile = generateLog(chooser.getSelectedFile(), logSize, timeDelta, timeOffset, clients);

            GenericOptionPane.showMessageDialog(
                    RandomLogGeneratorPanel.this.getParent(),
                    "The file is located at " + logFile.getAbsolutePath(),
                    "Log Created",
                    JOptionPane.INFORMATION_MESSAGE);

            // Generate some new default values...
            setSomeDefaults();

        } catch (Exception ex) {
            ErrorFrame ef = new ErrorFrame();
            ef.show(ex, RandomLogGeneratorPanel.this.getParent());
        } finally {
            this.createLogButton.setText(oldLabel);
            this.createLogButton.setEnabled(true);
            this.createLogButton.repaint();
        }
    }

    /**
     *
     */
    private static LogEntry generateRandomEntry(List<String> IPs, long start, long finish) throws Exception {
        // First, get the time b/w start and finish
        long timestamp = start + RandomUtil.getInt((int) (finish - start) + 1);

        // Pick an IP address
        String IP = IPs.get(RandomUtil.getInt(IPs.size()));

        // Generate a random big hash for data between 512 bytes and 16 KB

        byte[] hashBytes = new byte[512 + RandomUtil.getInt(15872 + 1)];
        RandomUtil.getBytes(hashBytes);
        BigHash hash = new BigHash(hashBytes);

        LogEntry entry = null;

        // Get the next action. Note the method is weighted, perferring
        // certain actions over others
        switch (getNextAction()) {
            case 0:
                entry = LogEntry.logGetConfiguration(timestamp, IP);
                break;
            case 1:
                entry = LogEntry.logGetData(timestamp, IP, hash);
                break;
            case 2:
                entry = LogEntry.logGetMetaData(timestamp, IP, hash);
                break;
            case 3:
                entry = LogEntry.logGetNonce(timestamp, IP);
                break;
            case 4:
                entry = LogEntry.logSetConfiguration(timestamp, IP, getSignature());
                break;
            case 5:
                entry = LogEntry.logSetData(timestamp, IP, hash, getSignature());
                break;
            case 6:
                entry = LogEntry.logSetMetaData(timestamp, IP, hash, getSignature());
                break;
        }
        return entry;
    }

    /**
     * Get next action. Note this is weighted, perferring some actions
     * over others. The heuristics are odd and are intended to replicate,
     * to some real degree, user activity.
     */
    private static int getNextAction() throws Exception {
        lazyLoadChances();

        // First, one third chance of nonce (action #3)
        if (RandomUtil.getInt(3) == 0) {
            return 3;        // Generate a preference for download over upload or vice versa.
        }
        if (chanceIsDownload) {
            // Roll the die!
            if (RandomUtil.getInt(chance) == 0) {
                // Data or meta data?
                if (RandomUtil.getBoolean()) {
                    return 1; // Get data
                } else {
                    return 2; // Get meta
                }
            }

        } else {
            // Roll the die!
            if (RandomUtil.getInt(chance) == 0) {
                // Data or meta data?
                if (RandomUtil.getBoolean()) {
                    return 5; // Set data
                } else {
                    return 6; // Set meta
                }
            }
        }

        // Fallback on pseudo-random
        return RandomUtil.getInt(7);
    }
    private static int chance = Integer.MIN_VALUE;
    private static boolean chanceIsDownload = true;

    private static void lazyLoadChances() {
        if (chance == Integer.MIN_VALUE) {
            // First, generate the chance scalar
            // Lower will be a higher chance
            chance = 1 + RandomUtil.getInt(4);

            // Now choose whether download or upload, giving download
            // the preference
            if (RandomUtil.getInt(4) == 0) {
                chanceIsDownload = false;            // Cute. Let's see what we got
            }
            String partial = "1 out of " + chance + " chance preference for";

            if (chanceIsDownload) {
                partial += " download.";
            } else {
                partial += " upload.";
            }
            System.out.println(partial);
        }
    }

    /**
     * Create a very illegitimate signature.
     */
    private static Signature getSignature() throws Exception {
        byte[] bytes = new byte[256];
        RandomUtil.getBytes(bytes);
        return new Signature(bytes, "fake", SecurityUtil.getAdminCertificate());
    }

    /**
     *
     */
    private static List<String> generateClientIPs(int clients) {
        List<String> ips = new LinkedList();

        StringBuffer buffer;
        byte[] ipBytes;
        while (ips.size() < clients) {

            buffer = new StringBuffer();

            // Get IP bytes
            ipBytes = new byte[4];
            RandomUtil.getBytes(ipBytes);

            // Generate the next IPv4 address
            for (byte i = 0; i < 4; i++) {
                int n = ipBytes[i] + Math.abs(Byte.MIN_VALUE);
                buffer.append(n);
                if (i < 3) {
                    buffer.append(".");
                }
            }

            // Add it. If duplicate, does nothing.
            if (!ips.contains(buffer.toString())) {
                ips.add(buffer.toString());
            }
        }

        return ips;
    }
}
