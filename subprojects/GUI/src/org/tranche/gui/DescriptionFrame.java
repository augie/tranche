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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

import javax.swing.JTextArea;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class DescriptionFrame extends GenericFrame {

    public DescriptionFrame(String title, String description) {
        super(title);

        // set the size
        setSize(400, 230);

        // set the layout
        setLayout(new GridBagLayout());

        // set the constraints
        GridBagConstraints gbc = new GridBagConstraints();

        // add a title
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 0, 8);
        JLabel titleLabel = new GenericLabel(title);
        titleLabel.setFont(Styles.FONT_TITLE);
        titleLabel.setAlignmentX(titleLabel.CENTER_ALIGNMENT);
        add(titleLabel, gbc);

        // add a descriptive text area
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.insets = new Insets(8, 8, 8, 8);
        JTextArea descriptionTextArea = new GenericTextArea(description);
        descriptionTextArea.setRows(8);
        descriptionTextArea.setMargin(new Insets(5, 5, 5, 5));
        descriptionTextArea.setFont(Styles.FONT_DEFAULT);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionTextArea.setBackground(Color.WHITE);
        descriptionTextArea.setEditable(false);
        descriptionTextArea.setBorder(BorderFactory.createLineBorder(Color.WHITE, 5));

        // add the panel
        GenericScrollPane pane = new GenericScrollPane(descriptionTextArea);
        pane.setBorder(Styles.BORDER_BLACK_1);
        pane.setVerticalScrollBar(new GenericScrollBar());
        add(pane, gbc);
    }
}
