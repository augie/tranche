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
package org.tranche.gui.add.wizard;

import org.tranche.gui.wizard.*;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.tranche.gui.DisplayTextArea;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.license.License;
import org.tranche.gui.Styles;
import org.tranche.gui.TextPanel;

/**
 * <p>User selects license and encryption</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class AFTStep2Panel extends GenericWizardPanel {

    private AddFileToolWizard wizard;
    private JRadioButton cc0Button = new JRadioButton(License.CC0.getTitle()),  customButton = new JRadioButton("Custom License");
    private ButtonGroup group = new ButtonGroup();

    public AFTStep2Panel(AddFileToolWizard wizard) {
        this.wizard = wizard;
        ActionListener l = new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                Thread t = new Thread() {

                    @Override()
                    public void run() {
                        if (e.getSource().equals(cc0Button)) {
                            cc0Button.setFont(Styles.FONT_14PT_BOLD);
                            customButton.setFont(Styles.FONT_14PT);
                            cc0Button.setSelected(true);
                            AFTStep2Panel.this.wizard.summary.getAddFileTool().setLicense(License.CC0);
                        } else if (e.getSource().equals(customButton)) {
                            cc0Button.setFont(Styles.FONT_14PT);
                            customButton.setFont(Styles.FONT_14PT_BOLD);
                            customButton.setSelected(true);
                            AFTStep2Panel.this.wizard.summary.getAddFileTool().setLicense(new License("Custom License", "", "", false));
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        };

        setBackground(Styles.COLOR_PANEL_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // Add buttons with options
        {
            gbc.gridwidth = 1;
            gbc.weightx = 1.0;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 10, 0, 10);
            group.add(cc0Button);
            cc0Button.setBackground(getBackground());
            cc0Button.setFocusable(false);
            cc0Button.setFont(Styles.FONT_14PT);
            cc0Button.addActionListener(l);
            cc0Button.setSelected(true);
            cc0Button.setFont(Styles.FONT_14PT_BOLD);
            add(cc0Button, gbc);
            cc0Button.doClick();

            // License text button
            JButton ccoButton = new JButton("View license agreement");
            ccoButton.setFont(Styles.FONT_10PT_BOLD);
            ccoButton.setBorder(Styles.UNDERLINE_MAROON);
            ccoButton.setForeground(Styles.COLOR_MAROON);
            ccoButton.setBackground(Styles.COLOR_PANEL_BACKGROUND);
            ccoButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        // Show popup with text
                        JPanel licensePanel = new TextPanel(License.CC0.getDescription());
                        GenericPopupFrame popup = new GenericPopupFrame(License.CC0.getTitle(), licensePanel);
                        popup.setSizeAndCenter(new Dimension(700, 500));
                        popup.setVisible(true);
                    } catch (Exception ex) {
                        ErrorFrame ef = new ErrorFrame();
                        ef.set(ex);
                        ef.show();
                    }
                }
            });
            gbc.gridwidth = GridBagConstraints.NONE;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(16, 0, 0, 10);
            add(ccoButton, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 35, 0, 10);
            add(new DisplayTextArea(License.CC0.getShortDescription()), gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1.0;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 10, 0, 10);
            group.add(customButton);
            customButton.setBackground(getBackground());
            customButton.setFocusable(false);
            customButton.setFont(Styles.FONT_14PT);
            customButton.addActionListener(l);
            add(customButton, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 35, 10, 10);
            add(new DisplayTextArea("Apply licensing terms of your choice."), gbc);
        }

        // add padding
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 5;
        gbc.weighty = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(Box.createVerticalStrut(1), gbc);
    }

    public void onLoad() {
    }

    public boolean onNext() {
        return true;
    }

    public boolean onPrevious() {
        return true;
    }
}