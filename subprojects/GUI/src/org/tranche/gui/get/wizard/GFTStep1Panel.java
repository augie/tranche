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
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.hash.BigHashHelperPanel;
import org.tranche.gui.GenericButton;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.gui.GenericRoundedButton;
import org.tranche.gui.PassphrasePool;
import org.tranche.project.ProjectSummaryCache;
import org.tranche.gui.Styles;
import org.tranche.gui.get.SelectUploaderPanel;
import org.tranche.gui.get.SelectUploaderPanelListener;
import org.tranche.gui.hash.BigHashInput;
import org.tranche.gui.hash.BigHashInputListener;
import org.tranche.gui.hash.BigHashNotFoundPanel;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class GFTStep1Panel extends GenericWizardPanel {

    private GetFileToolWizard wizard;
    private BigHashInput hashInput = new BigHashInput();
    private SelectUploaderPanel uploaderPanel = new SelectUploaderPanel();
    private GenericButton selectButton = new GenericButton("Find Hash");
    private GenericRoundedButton searchButton = new GenericRoundedButton("Find Similar Hashes");
    private JLabel infoLabel = new GenericLabel("Please enter a hash.");
    private final Object checkForPassphraseLock = new Object();
    private String passphraseText = "Please enter a hash.";
    protected JPasswordField passphrase = new JPasswordField() {

        @Override
        public void setEnabled(boolean b) {
            super.setEnabled(b);
            if (!b) {
                setBorder(BorderFactory.createLineBorder(Color.GRAY));
                setBackground(Styles.COLOR_EXTRA_LIGHT_GRAY);
                if (wizard.summary.getGetFileTool().getHash() != null) {
                    passphraseText = "No passphrase required.";
                } else {
                    passphraseText = "Please enter a hash.";
                }
            } else {
                setBorder(BorderFactory.createLineBorder(Color.BLACK));
                setBackground(Color.WHITE);
            }
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            // if disabled, paint a string saying so
            if (!isEnabled()) {
                if (passphraseText != null) {
                    g.drawString(passphraseText, 5, getHeight() - 5);
                }
            }
        }
    };

    public GFTStep1Panel(GetFileToolWizard wizard) {
        super();
        this.wizard = wizard;

        // launch the project cache util to load in the background
        Thread t = new Thread() {

            @Override()
            public void run() {
                selectButton.setEnabled(false);
                ProjectSummaryCache.waitForStartup();
                selectButton.setEnabled(true);
            }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        setBackground(Styles.COLOR_PANEL_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // add the file name field
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 0, 10);
        add(new GenericLabel("Hash:"), gbc);

        // end the line with the text field
        gbc.weightx = 1;
        gbc.insets = new Insets(10, 0, 0, 10);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(hashInput, gbc);

        // passphrase label
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 0, 10);
        add(new GenericLabel("Passphrase:"), gbc);

        // passphrase field
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.insets = new Insets(10, 0, 10, 10);
        passphrase.setBorder(Styles.BORDER_BLACK_1);
        passphrase.setEnabled(false);
        add(passphrase, gbc);
        gbc.insets = new Insets(0, 10, 10, 10);
        searchButton.setVisible(false);
        add(searchButton, gbc);

        // Information widget will take remaining space
        gbc.weightx = 1;
        gbc.weighty = 10;
        gbc.insets = new Insets(0, 10, 5, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        uploaderPanel.setVisible(false);
        uploaderPanel.addListener(new SelectUploaderPanelListener() {

            public void uploaderChanged(final String uploaderName, final long uploadTimestamp, final String uploadRelativePathInDataSet) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        GFTStep1Panel.this.wizard.summary.getGetFileTool().setUploaderName(uploaderName);
                        GFTStep1Panel.this.wizard.summary.getGetFileTool().setUploadTimestamp(uploadTimestamp);
                        GFTStep1Panel.this.wizard.summary.getGetFileTool().setUploadRelativePath(uploadRelativePathInDataSet);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        add(uploaderPanel, gbc);
        infoLabel.setFont(Styles.FONT_16PT);
        infoLabel.setHorizontalAlignment(JLabel.CENTER);
        infoLabel.setVisible(true);
        add(infoLabel, gbc);

        searchButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        BigHashNotFoundPanel panel = BigHashNotFoundPanel.getPanel(GFTStep1Panel.this.wizard.summary.getGetFileTool().getHash().toString(), hashInput.getTextArea());
                        GenericPopupFrame popup = new GenericPopupFrame(BigHashNotFoundPanel.TITLE, panel);
                        popup.setSizeAndCenter(BigHashNotFoundPanel.RECOMMENDED_DIMENSION);
                        popup.setVisible(true);
                        panel.setParentFrame(popup);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        hashInput.addListener(new BigHashInputListener() {

            public void hashChanged(final BigHash hash) {
                GFTStep1Panel.this.wizard.summary.getGetFileTool().setHash(hash);
                if (hash != null) {
                    // make the right components available
                    uploaderPanel.setVisible(false);
                    infoLabel.setVisible(true);
                    // wait til we check out this hash
                    GFTStep1Panel.this.wizard.setEnableNextButtonNow(false);
                    passphrase.setEnabled(false);
                    searchButton.setVisible(false);
                    Thread checkForPassphraseThread = new Thread() {

                        @Override()
                        public void run() {
                            synchronized (checkForPassphraseLock) {
                                // reset the color of the info label
                                infoLabel.setForeground(Color.BLACK);
                                try {
                                    // check for a passphrase. also downloads the meta data.
                                    MetaData metaData = GFTStep1Panel.this.wizard.summary.getGetFileTool().getMetaData();
                                    GFTStep1Panel.this.wizard.summary.getGetFileTool().setUploaderName(metaData.getSignature().getUserName());
                                    GFTStep1Panel.this.wizard.summary.getGetFileTool().setUploadTimestamp(metaData.getTimestampUploaded());
                                    GFTStep1Panel.this.wizard.summary.getGetFileTool().setUploadRelativePath(metaData.getRelativePathInDataSet());
                                    uploaderPanel.setMetaData(hash, metaData);
                                    passphrase.setEnabled(metaData.isEncrypted() && !metaData.isPublicPassphraseSet());
                                    if (passphrase.isEnabled()) {
                                        // if the in cache, use it
                                        if (PassphrasePool.contains(hash)) {
                                            passphrase.setText(PassphrasePool.get(hash));
                                        }
                                    } else {
                                        passphraseText = "No passphrase required.";
                                    }

                                    // make the right components available
                                    uploaderPanel.setVisible(true);
                                    infoLabel.setVisible(false);

                                    // enable the next button
                                    GFTStep1Panel.this.wizard.setEnableNextButtonNow(true);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    infoLabel.setText(e.getMessage());
                                    setPassphraseInfoText(e.getMessage());
                                    searchButton.setVisible(true);
                                }
                            }
                        }
                    };
                    checkForPassphraseThread.setDaemon(true);
                    checkForPassphraseThread.start();
                } else {
                    // make the right components available
                    uploaderPanel.setVisible(false);
                    infoLabel.setVisible(true);
                    searchButton.setVisible(false);
                    GFTStep1Panel.this.wizard.setEnableNextButtonNow(false);
                    passphrase.setEnabled(false);
                    setPassphraseInfoText("Please enter a hash.");
                }
            }

            public void textChanged(String text) {
                if (text.equals("")) {
                    infoLabel.setForeground(Color.BLACK);
                    infoLabel.setText("Please enter a hash.");
                } else {
                    infoLabel.setForeground(Styles.COLOR_DARKER_RED);
                    try {
                        BigHash.createHashFromString(text);
                        infoLabel.setText("Loading...");
                    } catch (Exception e) {
                        try {
                            infoLabel.setText("Bad Hash: " + BigHashHelperPanel.getProblemWithHash(text));
                        } catch (Exception ee) {
                            infoLabel.setText("Bad Hash: Unknown problem.");
                        }
                    }
                }
            }
        });
    }

    public void setHash(BigHash hash) {
        hashInput.setHash(hash);
    }

    public void setPassphrase(String passphrase) {
        this.passphrase.setText(passphrase);
        this.passphrase.validate();
        this.passphrase.repaint();
    }

    private void setPassphraseInfoText(String text) {
        passphraseText = text;
        passphrase.validate();
        passphrase.repaint();
    }

    public void selectUploader(String uploaderName, Long uploadTimestamp, String relativePath) {
        uploaderPanel.selectStartingUploader(uploaderName, uploadTimestamp, relativePath);
    }

    public void onLoad() {
    }

    public boolean onNext() {
        // if we need a passphrsae, make sure there is one
        if (passphrase.isEnabled()) {
            String pTrim = String.valueOf(passphrase.getPassword()).trim();
            if (pTrim.length() != passphrase.getPassword().length) {
                int pressedButton = GenericOptionPane.showConfirmDialog(wizard, "The passphrase you have entered contains extra space at the beginning or end.\nDo you want to remove this whitespace?", "Whitespace Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (pressedButton == JOptionPane.YES_OPTION) {
                    passphrase.setText(pTrim);
                }
            }
            if (pTrim.equals("")) {
                GenericOptionPane.showMessageDialog(wizard, "Please enter a passphrase to continue.", "Enter a Passphrase", JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
            wizard.summary.getGetFileTool().setPassphrase(pTrim);
        }
        return true;
    }

    public boolean onPrevious() {
        return false;
    }
}
