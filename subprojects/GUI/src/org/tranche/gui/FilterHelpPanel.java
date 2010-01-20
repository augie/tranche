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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class FilterHelpPanel extends JPanel {

    final public static Dimension RECOMMENDED_DIMENSION = new Dimension(450, 310);
    final JLabel projectsLabel = new Subheader("Filtering Data Sets");

    public FilterHelpPanel() {

        setBackground(Color.WHITE);
        setLayout(new GridBagLayout());

        int row = 0;
        final int MARGIN = 10;

        GridBagConstraints gbc = new GridBagConstraints();

        {
            String p = "You can use the filter to limit the data sets shown, which can save time when searching.";

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            add(new Paragraph(p), gbc);
        }

        // Filtering projects
        {
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN * 2, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            add(new Subheader("Filtering Data Sets"), gbc);

            String p1 = "Say you are looking for our RSS project, and you know the project name contains\"RSS\" somewhere. You could set the filter to help find the project:";

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            add(new Paragraph(p1), gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            add(new Codeblock("RSS"), gbc);

            String p2 = "You can also use wildcards. Say you know the project was named something like \"NCI [...] Data\". You could set the filter:";

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            add(new Paragraph(p2), gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0; // <-- Cell
            gbc.gridy = row++; // <-- Row

            add(new Codeblock("NCI .* Data"), gbc);
        }
    }

    private class Subheader extends JLabel {

        public Subheader(String str) {
            super(str);
            setFont(Styles.FONT_14PT_BOLD);
            setForeground(Styles.COLOR_TRIM);
            setBorder(Styles.UNDERLINE_MAROON);
        }
    }

    private class Paragraph extends JTextArea {

        public Paragraph(String str) {
            super(str);
            setFont(Styles.FONT_12PT);
            setEditable(false);
            setOpaque(false);
            setBorder(Styles.BORDER_NONE);
            setWrapStyleWord(true);
            setLineWrap(true);
        }
    }

    private class Codeblock extends JTextArea {

        public Codeblock(String str) {
            super(str);
            setFont(Styles.FONT_10PT_MONOSPACED);
            setEditable(false);
            setOpaque(true);
            setBorder(Styles.BORDER_BLACK_1);
            setBackground(Styles.COLOR_BACKGROUND_LIGHT);
            setWrapStyleWord(true);
            setLineWrap(true);
        }
    }
}
