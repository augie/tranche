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

import org.tranche.gui.*;
import org.tranche.project.ProjectSummary;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import org.tranche.ConfigureTranche;
import org.tranche.gui.project.ProjectPool;
import org.tranche.hash.BigHash;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class BigHashNotFoundPanel extends JPanel {

    private static boolean isDebug = false;
    final public static Dimension RECOMMENDED_DIMENSION = new Dimension(500, 390);
    final public static String TITLE = "Data was not found";
    /**
     * This is the hash string. We will see whether we can find a few matches.
     */
    private String hashStrWithoutData;
    /**
     * This is the text field for caller. If user selects a suggested hash, set that hash back.
     */
    private JTextArea callbackTextField;
    /**
     * 
     */
    private List<BigHash> suggestedHashes;
    /**
     * 
     */
    private JProgressBar progressBar;
    /**
     * 
     */
    private JTextArea statusText;
    /**
     * <p>Any hash within this distance will be recommended.</p>
     */
    public static long LEVENSHTEIN_DINSTANCE_CUTOFF = 40;
    public static long SIMILAR_STRING_ORDERED_CUTOFF = 40;
    public static long SIMILAR_STRING_UNORDERED_CUTOFF = 40;
    /**
     * All big hash suggestions (close matches) go here.
     */
    private GenericScrollPane suggestionsScroll;
    /**
     * All big hash suggestions (close matches) go here.
     */
    private JPanel suggestionsPanel;
    private JFrame parentFrame = null;
    private static BigHashNotFoundPanel singleton = null;

    /**
     * 
     * @param hashStrWithoutData
     * @param callbackField
     */
    private BigHashNotFoundPanel(String hashStrWithoutData, JTextArea callbackTextField) {

        this.hashStrWithoutData = hashStrWithoutData;
        this.callbackTextField = callbackTextField;
        this.suggestedHashes = new ArrayList<BigHash>();
        this.suggestionsScroll = new GenericScrollPane();
        this.suggestionsPanel = new JPanel();

        // Set up the progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        // Set up status text. Changes as projects are found.
        statusText = new JTextArea();
        statusText.setEditable(false);
        statusText.setWrapStyleWord(true);
        statusText.setLineWrap(true);
        statusText.setFont(Styles.FONT_12PT_BOLD);
        statusText.setBackground(Styles.COLOR_BACKGROUND);

        _findLevenshteinDistanceForEveryHash();
        _layout();
    }

    /**
     * 
     * @param hashStrWithoutData
     * @param callbackTextField
     * @return
     */
    public static synchronized BigHashNotFoundPanel getPanel(String hashStrWithoutData, JTextArea callbackTextField) {
        if (singleton == null || !singleton.isDisplayable()) {
            singleton = new BigHashNotFoundPanel(hashStrWithoutData, callbackTextField);
        }
        return singleton;
    }

    /**
     * 
     */
    private void _findLevenshteinDistanceForEveryHash() {

        Thread t = new Thread("Find Levenshtein distance for each hash thread.") {

            public void run() {
//                final long start = TimeUtil.getTrancheTimestamp();
//                printTracer("Started calculating distances at " + TextUtil.getFormattedDate(start));
//
//                setWaitingForProjectsToLoad();
//                ProjectSummaryCache.waitForStartup();
//
//                final long totalProjectCount = ProjectSummaryCache.getProjects().size();
//
//                printTracer("Found total of " + totalProjectCount + " projects...");
//
//                //
//                long currentProjectCount = 0;
//                for (ProjectSummary project : ProjectSummaryCache.getProjects()) {
//                    currentProjectCount++;
//
//                    // Check for disposed popup
//                    if (isDisposed()) {
//                        return;
//                    }
//
//                    if (project.projectHash != null) {
//                        final long distanceStart = TimeUtil.getTrancheTimestamp();
//
//                        String nextProjectHashStr = project.projectHash.toString();
//
//                        int strDifference = Text.getCharacterDifferenceBetweenStrings(nextProjectHashStr, hashStrWithoutData);
//
//                        printTracer("Finished hash #" + currentProjectCount + ", found difference char count of " + strDifference + ", took: " + TextUtil.formatTime(TimeUtil.getTrancheTimestamp() - distanceStart));
//                        setStartedMatchingSimilarStringsOrdered(currentProjectCount, totalProjectCount);
//
//                        // Add the hash with its score if meets cut-off
//                        if (strDifference <= BigHashNotFoundPanel.SIMILAR_STRING_ORDERED_CUTOFF && !suggestedHashes.contains(project.projectHash)) {
//
//                            // Add to GUI
//                            addRecommendedHash(project.projectHash);
//
//                            // Store in list
//                            suggestedHashes.add(project.projectHash);
//                        }
//                    }
//                }
//                printTracer("Finished looking for most similar hashes, took: " + TextUtil.formatTime(TimeUtil.getTrancheTimestamp() - start));
//
//                //
//                currentProjectCount = 0;
//                for (ProjectSummary project : ProjectSummaryCache.getProjects()) {
//                    currentProjectCount++;
//
//                    // Check for disposed popup
//                    if (isDisposed()) {
//                        return;
//                    }
//
//                    if (project.projectHash != null) {
//                        final long distanceStart = TimeUtil.getTrancheTimestamp();
//
//                        String nextProjectHashStr = project.projectHash.toString();
//
//                        int strDifference = Text.getCharacterCountDifferenceBetweenBase64Strings(
//                                nextProjectHashStr, hashStrWithoutData);
//
//                        printTracer("Finished hash #" + currentProjectCount + ", found difference char count of " + strDifference + ", took: " + TextUtil.formatTime(TimeUtil.getTrancheTimestamp() - distanceStart));
//                        setStartedMatchingSimilarStringsUnordered(currentProjectCount, totalProjectCount);
//
//                        // Add the hash with its score if meets cut-off
//                        if (strDifference <= BigHashNotFoundPanel.SIMILAR_STRING_UNORDERED_CUTOFF && !suggestedHashes.contains(project.projectHash)) {
//
//                            // Add to GUI
//                            addRecommendedHash(project.projectHash);
//
//                            // Store in list
//                            suggestedHashes.add(project.projectHash);
//                        }
//                    }
//                }
//                printTracer("Finished looking for most similar hashes, took: " + TextUtil.formatTime(TimeUtil.getTrancheTimestamp() - start));
//
//                //
//                currentProjectCount = 0;
//                for (ProjectSummary project : ProjectSummaryCache.getProjects()) {
//                    currentProjectCount++;
//
//                    // Check for disposed popup
//                    if (isDisposed()) {
//                        return;
//                    }
//
//                    if (project.projectHash != null) {
//                        final long distanceStart = TimeUtil.getTrancheTimestamp();
//                        long distance = Text.getLevenshteinDistance(hashStrWithoutData, project.projectHash.toString());
//
//                        printTracer("Finished hash #" + currentProjectCount + ", found a distance of " + distance + ", took: " + TextUtil.formatTime(TimeUtil.getTrancheTimestamp() - distanceStart));
//                        setStartedCalculatingDistances(currentProjectCount, totalProjectCount);
//
//                        // Add the hash with its score if meets cut-off
//                        if (distance <= BigHashNotFoundPanel.LEVENSHTEIN_DINSTANCE_CUTOFF && !suggestedHashes.contains(project.projectHash)) {
//
//                            // Add to GUI
//                            addRecommendedHash(project.projectHash);
//
//                            // Store in list
//                            suggestedHashes.add(project.projectHash);
//                        }
//                    }
//                }
//
//                printTracer("Finished calculating distances, took: " + TextUtil.formatTime(TimeUtil.getTrancheTimestamp() - start));
//
//                setFinishedFindingMatches();
//                checkForNoSuggestedHashes();
//                printTracer("Finished finding suggested hashes, took: " + TextUtil.formatTime(TimeUtil.getTrancheTimestamp() - start));
            }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
    boolean wasVisible = false;

    /**
     * <p>An interesting way to discover if user has closed this popup panel</p>
     */
    public boolean isDisposed() {
        if (this.getParentFrame() != null) {

            if (wasVisible && !this.getParentFrame().isDisplayable()) {
                return true;
            } // Must be visible once before "disposed"
            else if (this.getParentFrame().isVisible()) {
                wasVisible = true;
            }
        }

        return false;
    }

    /**
     * 
     */
    public void _layout() {

        // Set the look
        setBackground(Styles.COLOR_BACKGROUND);
        setLayout(new GridBagLayout());

        int row = 0;
        final int MARGIN = 10;

        GridBagConstraints gbc = new GridBagConstraints();

        // Add description
        {
            JTextArea description = new JTextArea("The data for the hash you provided was not found on the network at this time. This could be because the hash in not correct, or that the data is currently unavailable due to technical issues.\n\nThis tool will try three algorithms (in order of their speed) to recommend similar hashes. This may take several minutes.");
            description.setEditable(false);
            description.setWrapStyleWord(true);
            description.setLineWrap(true);
            description.setFont(Styles.FONT_12PT);
            description.setBackground(Styles.COLOR_BACKGROUND);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN), MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(description, gbc);
        }

        // Add the status text.
        {
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN * 2), MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.statusText, gbc);
        }

        // Add progress bar. This will be set invisible when done looking for projects.
        {
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN), MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            this.add(this.progressBar, gbc);
        }

        // Add scroll panel
        {
            // Set the look and layout for scroll panel
            this.suggestionsPanel.setBackground(Color.WHITE);
            this.suggestionsPanel.setLayout(new GridBagLayout());

            // DEBUG!
            this.suggestionsPanel.setBackground(Color.WHITE);

            this.suggestionsScroll.setViewportView(this.suggestionsPanel);

            // Conditionally add the scoll. If doesn't exist, will push content down.
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (MARGIN), MARGIN, MARGIN, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row
            this.add(suggestionsScroll, gbc);
        }
    }

    private void setWaitingForProjectsToLoad() {
        this.statusText.setText("Looking for similar hashes. Waiting for project information to load...");
        this.progressBar.setIndeterminate(true);
    }

    private void setStartedMatchingSimilarStringsOrdered(long currentProject, long totalProjects) {
        this.statusText.setText("Step 1 of 3: Looking for the most similar hashes. Checked " + currentProject + " of " + totalProjects + " projects.");

        int progressValue = (int) Math.round(100 * ((double) currentProject / (double) totalProjects));
        printTracer(currentProject + " of " + totalProjects + " done, " + progressValue + "% complete.");
        this.progressBar.setIndeterminate(false);
        this.progressBar.setValue(progressValue);
    }

    private void setStartedMatchingSimilarStringsUnordered(long currentProject, long totalProjects) {
        this.statusText.setText("Step 2 of 3: Looking for other similar hashes. Checked " + currentProject + " of " + totalProjects + " projects.");

        int progressValue = (int) Math.round(100 * ((double) currentProject / (double) totalProjects));
        printTracer(currentProject + " of " + totalProjects + " done, " + progressValue + "% complete.");
        this.progressBar.setIndeterminate(false);
        this.progressBar.setValue(progressValue);
    }

    private void setStartedCalculatingDistances(long currentProject, long totalProjects) {
        this.statusText.setText("Step 3 of 3: Looking for any remaining similar hashes. Checked " + currentProject + " of " + totalProjects + " projects.");

        int progressValue = (int) Math.round(100 * ((double) currentProject / (double) totalProjects));
        printTracer(currentProject + " of " + totalProjects + " done, " + progressValue + "% complete.");
        this.progressBar.setIndeterminate(false);
        this.progressBar.setValue(progressValue);
    }

    private void setFinishedFindingMatches() {
        this.statusText.setText("Finished looking for suggested projects.");

        this.progressBar.setIndeterminate(false);
        this.progressBar.setValue(100);
    }
    private static final int RECOMMENDED_MARGIN = 5;
    private static int RECOMMENDED_ROW = 0;
    private static final Component strut = Box.createVerticalStrut(1);

    private void addRecommendedHash(BigHash hash) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets((int) (RECOMMENDED_MARGIN), RECOMMENDED_MARGIN, 0, RECOMMENDED_MARGIN);
        gbc.gridx = 0; // <-- Cell
        gbc.gridy = RECOMMENDED_ROW++; // <-- Row

        JPanel suggestionPanel = getSuggestionPanelForHash(hash, callbackTextField, parentFrame);

        // Remove the strut if already there
        this.suggestionsPanel.remove(strut);

        // Add the panel
        this.suggestionsPanel.add(suggestionPanel, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridx = 0; // <-- Cell
        gbc.gridy = RECOMMENDED_ROW++; // <-- Row

        // Add the strut to force content flush top
        this.suggestionsPanel.add(strut, gbc);

        this.suggestionsPanel.invalidate();
        this.suggestionsPanel.validate();
        this.suggestionsPanel.repaint();

        printTracer("ADDED SUGGESTED HASH: " + hash.toString());
    }

    private void checkForNoSuggestedHashes() {
        if (this.suggestedHashes.size() == 0) {
            printTracer("DIDN'T FIND ANY MATCHING HASHES!");

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets((int) (RECOMMENDED_MARGIN), RECOMMENDED_MARGIN, 0, RECOMMENDED_MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = RECOMMENDED_ROW++; // <-- Row

            JTextArea description = new JTextArea("Unfortunately, we could not find a close enough match. Please verify the hash, and if the data is unavailable, contact us at " + ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_CONTACT_EMAIL) + " and offer us details about the suspected missing data.");
            description.setEditable(false);
            description.setWrapStyleWord(true);
            description.setLineWrap(true);
            description.setFont(Styles.FONT_12PT);
            description.setBackground(Styles.COLOR_BACKGROUND);

            this.suggestionsPanel.add(description, gbc);

            this.suggestionsPanel.invalidate();
            this.suggestionsPanel.validate();
            this.suggestionsPanel.repaint();
        }
    }

    private static JPanel getSuggestionPanelForHash(final BigHash hash, final JTextArea callbackArea, final JFrame popupFrame) {

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets((int) (RECOMMENDED_MARGIN + 8), RECOMMENDED_MARGIN, 0, RECOMMENDED_MARGIN);
        gbc.gridx = 0; // <-- Cell
        gbc.gridy = 0; // <-- Row

        String title = hash.toString().substring(0, 50) + "...";

        ProjectSummary project = ProjectPool.get(hash, null, null);
        if (project != null && project.title != null) {
            if (project.title.length() < 55) {
                title = project.title;
            } else {
                title = project.title.substring(0, 55) + "...";
            }
        }

        JTextArea titleArea = new JTextArea(title);
        titleArea.setEditable(false);
        titleArea.setWrapStyleWord(true);
        titleArea.setLineWrap(true);
        titleArea.setFont(Styles.FONT_10PT_MONOSPACED);
        titleArea.setBackground(Color.WHITE);

        panel.add(titleArea, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.25;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets((int) (RECOMMENDED_MARGIN), RECOMMENDED_MARGIN, 0, RECOMMENDED_MARGIN);
        gbc.gridx = 1; // <-- Cell

        JButton chooseHashButton = new JButton("Use this hash");
        chooseHashButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (callbackArea != null) {
                    callbackArea.setText(hash.toString());
                }
                printTracer("User selected suggested hash: " + hash.toString());

                Thread t = new Thread("") {

                    /**
                     * 
                     */
                    @Override()
                    public void run() {
                        // If parent frame set, give user option to dispose
                        if (popupFrame != null) {
                            int n = GenericOptionPane.showConfirmDialog(
                                    popupFrame,
                                    "Hash successfully set. Close this window (will stop search for more hashes)?",
                                    "Hash set, stop search?",
                                    JOptionPane.YES_NO_OPTION);

                            if (n == JOptionPane.YES_OPTION) {
                                popupFrame.dispose();
                            }
                        } else {
                            GenericOptionPane.showMessageDialog(null, "The selected hash was set.\n\nYou may close the search window at any time, but the tool will stop searching for more suggestions.", "Hash successfully set", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                };
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
            }
        });
        chooseHashButton.setFont(Styles.FONT_10PT_BOLD);
        panel.add(chooseHashButton, gbc);

        return panel;
    }

    private static void printTracer(String message) {
        if (isDebug) {
            System.out.println("BIG_HASH_NOT_FOUND_PANEL> " + message);
        }
    }

    public JFrame getParentFrame() {
        return parentFrame;
    }

    public void setParentFrame(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }
}
