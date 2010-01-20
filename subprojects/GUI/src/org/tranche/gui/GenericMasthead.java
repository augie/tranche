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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A Tranche-styled masthead.
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class GenericMasthead extends JPanel {

    private String title;
    private JLabel label = new GenericLabel("");

    public GenericMasthead(String title) {
        setBackground(Color.DARK_GRAY);
        setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, Styles.COLOR_TRIM));
        setLayout(new BorderLayout());

        this.title = title;

        label.setText(title);
        label.setFont(Styles.FONT_14PT);
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        add(label, BorderLayout.CENTER);
    }

    public void setLabelFont(Font font) {
        label.setFont(font);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.label.setText(title);
        this.label.repaint();
    }
}