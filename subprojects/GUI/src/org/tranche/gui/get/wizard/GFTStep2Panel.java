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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.tranche.exceptions.CouldNotFindMetaDataException;
import org.tranche.get.GetFileTool;
import org.tranche.get.GetFileToolUtil;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.GenericButton;
import org.tranche.gui.GenericCheckBox;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericTextField;
import org.tranche.gui.Styles;
import org.tranche.gui.project.panel.ProjectPanel;
import org.tranche.security.WrongPassphraseException;
import org.tranche.util.DebugUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class GFTStep2Panel extends GenericWizardPanel {

    private GetFileToolWizard wizard;
    private JPanel projectPanelProgressPanel = new JPanel(),  projectPanelPanel = new JPanel();
    private Thread projectLoadingThread = null;
    private JPanel blankPanel = new JPanel();
    private GridBagConstraints gbc = new GridBagConstraints();
    private final short[] previousState = {0};
    public JTextField regEx = new GenericTextField() {

        @Override
        public void setEnabled(boolean b) {
            super.setEnabled(b);
            if (!b) {
                setBackground(Styles.COLOR_EXTRA_LIGHT_GRAY);
                setBorder(BorderFactory.createLineBorder(Color.GRAY));
            } else {
                setBackground(Color.WHITE);
                setBorder(BorderFactory.createLineBorder(Color.BLACK));
            }
        }
    };
    public GenericCheckBox downloadAllCheckBox = new GenericCheckBox("Download everything.", true);
    public ProjectPanel projectPanel = new ProjectPanel();

    public GFTStep2Panel(GetFileToolWizard wizard) {
        this.wizard = wizard;

        setBackground(Styles.COLOR_PANEL_BACKGROUND);
        setLayout(new GridBagLayout());

        // download all check box
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(10, 10, 0, 10);
        downloadAllCheckBox.setBackground(getBackground());
        downloadAllCheckBox.setFocusable(false);
        downloadAllCheckBox.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {

                // Make sure not just a rollover; state of checkbox must change
                synchronized (previousState) {
                    // Come up with code for current state of download all checkbox.
                    short currentState = 0;
                    if (downloadAllCheckBox.isSelected()) {
                        currentState = 1;
                    } else {
                        currentState = 2;
                    }

                    // State must change (or must be first time this code block entered), otherwise return
                    if (previousState[0] == currentState) {
                        return;
                    }
                    previousState[0] = currentState;
                }

                Thread t = new Thread() {

                    @Override
                    public void run() {
                        try {
                            regEx.setEnabled(!downloadAllCheckBox.isSelected());
                            if (!downloadAllCheckBox.isSelected()) {
                                int cindex = 0;
                                for (; cindex < GFTStep2Panel.this.getComponentCount(); cindex++) {
                                    if (GFTStep2Panel.this.getComponent(cindex) == blankPanel) {
                                        break;
                                    }
                                }
                                if (cindex < GFTStep2Panel.this.getComponentCount()) {
                                    GFTStep2Panel.this.remove(blankPanel);
                                    GFTStep2Panel.this.add(projectPanelPanel, gbc, cindex);
                                    GFTStep2Panel.this.validate();
                                    GFTStep2Panel.this.repaint();
                                }

                                tryToLoadProject();
                            } else {
                                int cindex = 0;
                                for (; cindex < GFTStep2Panel.this.getComponentCount(); cindex++) {
                                    if (GFTStep2Panel.this.getComponent(cindex) == projectPanelPanel) {
                                        break;
                                    }
                                }
                                if (cindex < GFTStep2Panel.this.getComponentCount()) {
                                    GFTStep2Panel.this.remove(projectPanelPanel);
                                    GFTStep2Panel.this.add(blankPanel, gbc, cindex);
                                    GFTStep2Panel.this.validate();
                                    GFTStep2Panel.this.repaint();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        add(downloadAllCheckBox, gbc);

        // regEx label
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(10, 10, 0, 10);
        JLabel regexLabel = new GenericLabel("Filter:");
        regexLabel.setBackground(getBackground());
        regexLabel.setToolTipText("Enter the regular expression of the files you want to download, e.g. '\\.pkl'.");
        add(regexLabel, gbc);

        // regEx text field
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(10, 0, 0, 10);
        regEx.setBorder(Styles.BORDER_BLACK_1);
        regEx.setBackground(Styles.COLOR_BACKGROUND_LIGHT);
        regEx.setEnabled(false);
        regEx.setToolTipText("Enter the regular expression of the files you want to download, e.g. '\\.pkl'.");
        regEx.addCaretListener(new CaretListener() {

            public void caretUpdate(CaretEvent e) {
                // spawn in a background thread
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        updateProjectPanelRegEx();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        add(regEx, gbc);

        // project panel
        {
            // set up the panel with a progress bar
            projectPanelPanel.setLayout(new BorderLayout());
            {
                projectPanelProgressPanel.setLayout(new BorderLayout());
                {
                    JProgressBar projectPanelProgress = new JProgressBar();
                    projectPanelProgress.setBorder(Styles.BORDER_BLACK_1);
                    projectPanelProgress.setOpaque(true);
                    projectPanelProgress.setBackground(Styles.COLOR_PROGRESS_BAR_BACKGROUND);
                    projectPanelProgress.setForeground(Styles.COLOR_PROGRESS_BAR_FOREGROUND);
                    projectPanelProgress.setIndeterminate(true);
                    projectPanelProgress.setToolTipText("Trying to load the data set.");
                    projectPanelProgressPanel.add(projectPanelProgress, BorderLayout.CENTER);

                    GenericButton cancelButton = new GenericButton("  Cancel  ");
                    cancelButton.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
                    cancelButton.setFont(Styles.FONT_10PT);
                    cancelButton.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            Thread t = new Thread() {

                                @Override
                                public void run() {
                                    if (projectLoadingThread != null) {
                                        if (projectLoadingThread.isAlive()) {
                                            projectLoadingThread.stop();
                                        }
                                    }
                                }
                            };
                            t.setDaemon(true);
                            t.start();
                        }
                    });
                    projectPanelProgressPanel.add(cancelButton, BorderLayout.EAST);
                }
                projectPanelProgressPanel.setVisible(false);
                projectPanelPanel.add(projectPanelProgressPanel, BorderLayout.NORTH);
                projectPanelPanel.add(projectPanel, BorderLayout.CENTER);
            }

            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.insets = new Insets(10, 10, 5, 10);
            projectPanelPanel.setBorder(Styles.BORDER_BLACK_1);

            // start by showing the blank panel
            blankPanel.setLayout(new BorderLayout());
            JLabel allFilesLabel = new GenericLabel("All files selected.");
            allFilesLabel.setFont(Styles.FONT_12PT_MONOSPACED);
            allFilesLabel.setHorizontalAlignment(JLabel.CENTER);
            blankPanel.add(allFilesLabel, BorderLayout.CENTER);
            blankPanel.setBackground(Styles.COLOR_EXTRA_LIGHT_GRAY);
            blankPanel.setBorder(Styles.BORDER_GRAY_1);
            add(blankPanel, gbc);
        }
    }

    public void setRegEx(String regEx) {
        if (regEx == null || regEx.equals("")) {
            return;
        }
        downloadAllCheckBox.setSelected(false);
        this.regEx.setText(regEx);
        updateProjectPanelRegEx();
    }

    public void updateProjectPanelRegEx() {
        if (!projectPanel.getFilterText().equals(regEx.getText().toLowerCase())) {
            projectPanel.setFilter(regEx.getText());
            projectPanel.refresh();
        }
    }

    public void onLoad() {
        try {
            if (!wizard.summary.getGetFileTool().getMetaData().isProjectFile()) {
                downloadAllCheckBox.setText("Download is a single file.");
                downloadAllCheckBox.setSelected(true);
                downloadAllCheckBox.setEnabled(false);
            }
            tryToLoadProject();
            // produces desired results for setting the regEx in the wizard to show only selected files in the project panel
            regEx.getCaret().setDot(regEx.getText().length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tryToLoadProject() throws Exception {
        // only have one loading thread open at a time
        if ((projectLoadingThread != null && projectLoadingThread.isAlive()) || !wizard.summary.getGetFileTool().getMetaData().isProjectFile()) {
            return;
        }

        // try to set the project panel files
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    projectPanelProgressPanel.setVisible(true);
                    projectPanel.setProject(wizard.summary.getGetFileTool().getHash(), wizard.summary.getGetFileTool().getProjectFile(), wizard.summary.getGetFileTool().getMetaData());
                } catch (CouldNotFindMetaDataException e) {
                    GenericOptionPane.showMessageDialog(wizard, e.getMessage(), "Data Set Not Found", JOptionPane.ERROR_MESSAGE);
                } catch (WrongPassphraseException e) {
                    GenericOptionPane.showMessageDialog(wizard, e.getMessage(), "Wrong Passphrase", JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    projectPanel.clear();
                    GenericOptionPane.showMessageDialog(wizard, "Could not download the project file.\n\n" + e.getClass().getSimpleName() + ": " + e.getMessage(), "Data Set Not Found", JOptionPane.ERROR_MESSAGE);
                } finally {
                    projectPanelProgressPanel.setVisible(false);
                }
            }
        };
        t.setDaemon(true);
        projectLoadingThread = t;
        projectLoadingThread.start();
    }

    public boolean onNext() {
        // make the regex
        String regexStr = GetFileTool.DEFAULT_REG_EX;
        // set up the regex according to the selected files
        if (!downloadAllCheckBox.isSelected()) {
            if (projectPanel.getSelectedParts() != null && projectPanel.getSelectedParts().size() > 0) {
                regexStr = GetFileToolUtil.getRegex(projectPanel.getSelectedParts());
            } else if (regEx.getText() != null && !regEx.getText().trim().equals("")) {
                regexStr = regEx.getText();
            }
        }
        wizard.summary.getGetFileTool().setRegEx(regexStr);
        return true;
    }

    public boolean onPrevious() {
        return true;
    }
}
