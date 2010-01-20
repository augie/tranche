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
package org.tranche.gui.get.wizard;

import org.tranche.gui.wizard.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.tranche.get.GetFileToolAdapter;
import org.tranche.get.GetFileToolEvent;
import org.tranche.gui.GenericTextArea;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.GenericButton;
import org.tranche.gui.GenericOptionPane;
import org.tranche.util.PreferencesUtil;
import org.tranche.gui.Styles;
import org.tranche.gui.get.DownloadPool;
import org.tranche.server.PropagationExceptionWrapper;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class GFTStep3Panel extends GenericWizardPanel {

    private GetFileToolWizard wizard;
    private JTextField location = new JTextField(PreferencesUtil.getFile(PreferencesUtil.PREF_DOWNLOAD_FILE).getAbsolutePath());
    private boolean startedDownload = false;

    public GFTStep3Panel(GetFileToolWizard wizard) {
        this.wizard = wizard;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Explanatory text
        {
            JTextArea downloadDescription = new GenericTextArea("Tranche will download the data set into the selected directory. As individual files are completed, they will appear in the directory.");
            downloadDescription.setFont(Styles.FONT_12PT);
            downloadDescription.setEditable(false);
            downloadDescription.setLineWrap(true);
            downloadDescription.setWrapStyleWord(true);
            downloadDescription.setBackground(getBackground());
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(10, 10, 10, 0);
            add(downloadDescription, gbc);
        }

        {
            // "Download location" label
            JLabel label = new JLabel("Download location:");
            label.setFont(Styles.FONT_12PT_BOLD);

            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(10, 10, 0, 0);
            add(label, gbc);

            // Location (path) label
            gbc.weightx = 1;
            add(location, gbc);

            // "Change" button
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            gbc.insets = new Insets(10, 10, 0, 10);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            JButton changeButton = new GenericButton("Change...");
            changeButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final JFileChooser fc = GUIUtil.makeNewFileChooser();
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                            File previousDir = new File(PreferencesUtil.get(PreferencesUtil.PREF_DOWNLOAD_FILE));
                            if (previousDir != null && previousDir.exists()) {
                                fc.setCurrentDirectory(previousDir);
                            }
                            if (fc.showOpenDialog(GFTStep3Panel.this.wizard) == JFileChooser.APPROVE_OPTION) {
                                location.setText(fc.getSelectedFile().getAbsolutePath().toString());
                                GFTStep3Panel.this.wizard.setEnableNextButtonNow(true);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            changeButton.setBorder(Styles.BORDER_BLACK_1_PADDING_SIDE_2);
            changeButton.setFont(Styles.FONT_10PT);
            changeButton.setToolTipText("Click to select the save location.");
            add(changeButton, gbc);
        }

        // Vertical strut. Competes w/ other struts for space.
        gbc.weighty = 1;
        add(Box.createVerticalStrut(1), gbc);
    }

    public void onLoad() {
        try {
            File locationFile = new File(location.getText());
            // make sure the location exists
            if (!locationFile.exists()) {
                throw new Exception("");
            }
            location.setText(locationFile.getAbsolutePath());
        } catch (Exception e) {
            location.setText("Location does not exist.");
            wizard.setEnableNextButtonNow(false);
        }
    }

    public boolean onNext() {
        try {
            /**
             * Test that directory (or parent if a normal file) is writable
             */
            File directory = new File(location.getText());

            // This condition might be met even if the download location is intended to be
            // a directory but doesn't exist. In any case, we'll just look at the parent!
            if (!directory.isDirectory()) {
                directory = directory.getParentFile();
            }

            // Make the path. Shouldn't throw exception if fails. 
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Test that directory exists
            if (!directory.exists()) {
                GenericOptionPane.showMessageDialog(
                        wizard,
                        "Tried to create directory <" + directory.getAbsolutePath() + ">, but couldn't.\n\nPlease verify that you have write permissions for the selected location, or select a different download directory.",
                        "Cannot download to selected location",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (!directory.canWrite()) {
                GenericOptionPane.showMessageDialog(
                        wizard,
                        "Cannot write to selected download directory <" + directory.getAbsolutePath() + ">.\n\nPlease verify that have write permissions for the selected location, or select a different download directory.",
                        "Cannot download to selected location",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            if (startedDownload) {
                return false;
            }
            startedDownload = true;

            // make the download summary
            wizard.summary.getGetFileTool().setSaveFile(directory);
            wizard.summary.getGetFileTool().setServersToUse(wizard.serversFrame.getPanel().getSelectedHosts());
            // menu selections
            wizard.summary.getGetFileTool().setUseUnspecifiedServers(wizard.menuBar.useUnspecifiedServersCheckBoxItem.isSelected());
            wizard.summary.getGetFileTool().setContinueOnFailure(wizard.menuBar.continueOnFailureCheckBoxItem.isSelected());
            wizard.summary.getGetFileTool().setValidate(wizard.menuBar.validateMenuItem.isSelected());

            // save the preference
            PreferencesUtil.set(PreferencesUtil.PREF_DOWNLOAD_FILE, directory.getAbsolutePath());
            // set to pool
            DownloadPool.set(wizard.summary);

            // If this is the advanced GUI, download using it
            if (GUIUtil.getAdvancedGUI() != null) {
                wizard.setVisible(false);
                GUIUtil.getAdvancedGUI().mainPanel.setTab(GUIUtil.getAdvancedGUI().mainPanel.downloadsPanel);
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean onPrevious() {
        return true;
    }
}
