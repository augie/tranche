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
package org.tranche.gui;

import org.tranche.gui.util.GUIUtil;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Panel meant for use with GenericPopupFrame.
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class AboutPanel extends JPanel {

    // Used for achieving similar margins
    private int MARGIN = 10;
    /**
     * <p>Recommended size for this popup panel when used with a generic popup frame.</p>
     */
    public static final Dimension RECOMMENDED_DIMENSION = new Dimension(450, 285);

    public AboutPanel() {
        setBackground(Styles.COLOR_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel aboutLabel = new GenericLabel("About Tranche");
        aboutLabel.setFont(Styles.FONT_14PT_BOLD);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(MARGIN * 2, MARGIN, 0, MARGIN);
        add(aboutLabel, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
        add(new DisplayTextArea("Tranche is a free and open source distributed file repository, the primary goal of which is to simplify the sharing and dissemination of data."), gbc);

        JButton aboutButton = new GenericRoundedButton("More About Tranche");
        aboutButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        GUIUtil.displayURL("https://trancheproject.org/about.jsp");
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
        add(aboutButton, gbc);

        // Add description about netowork
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(MARGIN * 2, MARGIN, 0, MARGIN);
        add(new DisplayTextArea("Tranche features a core network of servers that host the data. Anyone can add their own server to a network, but a server must be trusted to become part of the core network."), gbc);

        // padding
        gbc.weighty = 2.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(Box.createVerticalStrut(1), gbc);
    }
}
