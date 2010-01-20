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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;

/**
 * Used for creating popups that get hash span. Generic since you can set the text on the button and decide what to do with the output.
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class GenericBigHashPanel extends JPanel {

    private boolean isFinished = false;
    private BigHashInput hashInput = new BigHashInput();
    JButton returnBigHashButton;
    JFrame frame;
    /**
     * <p>Recommended dimensions.</p>
     */
    public static Dimension DIMENSION = new Dimension(600, 200);
    private static boolean onlyProjectFiles = false;

    /**
     * <p>Create a hash span panel.</p>
     * @param buttonText The text you want to appear on the button. This is probably whatever you are going to do with the BigHash.
     */
    public GenericBigHashPanel(String buttonText) {
        returnBigHashButton = new GenericButton(buttonText);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // add the file name field
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(10, 10, 0, 10);
        JLabel nameLabel = new GenericLabel("Hash:");
        nameLabel.setToolTipText("Enter the hash of the data set that you would like to download.");
        nameLabel.setBackground(Styles.COLOR_BACKGROUND);
        add(nameLabel, gbc);

        // end the line with the text field
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.insets = new Insets(10, 1, 0, 5);
        add(hashInput, gbc);

        // the download button
        gbc.insets = new Insets(10, 0, 0, 0);
        gbc.ipady = 1;
        gbc.weighty = 0;
        returnBigHashButton.setFont(Styles.FONT_TITLE);
        returnBigHashButton.setBorder(Styles.BORDER_BUTTON_TOP);
        returnBigHashButton.setToolTipText("Clicking this button will download the selected data set.");
        returnBigHashButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                Thread t = new Thread("Generic Big Hash Panel Button Press") {

                    @Override
                    public void run() {
                        if (hashInput.getHash() == null) {
                            GenericOptionPane.showMessageDialog(
                                    frame,
                                    "No valid hash has been specified.",
                                    "No Valid Hash",
                                    JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        if (onlyProjectFiles) {
                            GetFileTool gft = new GetFileTool();
                            gft.setHash(hashInput.getHash());
                            try {
                                MetaData metaData = gft.getMetaData();
                                // If only supposed to be a project and not
                                if (!metaData.isProjectFile()) {
                                    GenericOptionPane.showMessageDialog(
                                            frame,
                                            "The hash you provided is not the hash of a project file.",
                                            "Not a Project File Hash",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    return;
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                GenericOptionPane.showMessageDialog(
                                        frame,
                                        "The data set could not be found. Please make sure you provided the correct hash.",
                                        "Data Set Not Found",
                                        JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }
                        }

                        frame.dispose();
                        isFinished = true;
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        add(returnBigHashButton, gbc);
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    public void setOnlyProjectFiles(boolean aOnlyProjectFiles) {
        onlyProjectFiles = aOnlyProjectFiles;
    }

    public BigHash receiveBigHash() {
        while (!isFinished) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
            }
        }
        return hashInput.getHash();
    }
}
