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
import javax.swing.JPanel;

import javax.swing.JTextArea;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class TextPanel extends JPanel {

    public TextPanel(String text) {
        JTextArea area = new GenericTextArea(text);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setFont(Styles.FONT_12PT_MONOSPACED);
        GenericScrollPane pane = new GenericScrollPane(area);
        setLayout(new BorderLayout());
        add(pane, BorderLayout.CENTER);
    }
}
