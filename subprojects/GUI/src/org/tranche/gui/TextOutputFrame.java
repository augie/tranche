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
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class TextOutputFrame extends GenericFrame {

    public final JTextArea textArea = new GenericTextArea();

    public TextOutputFrame(String title) {
        super(title);
        setSize(550, 300);

        textArea.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK));
        textArea.setLineWrap(true);
        textArea.setEditable(false);

        // add the text area
        add(new GenericScrollPane(textArea, GenericScrollPane.VERTICAL_SCROLLBAR_ALWAYS, GenericScrollPane.HORIZONTAL_SCROLLBAR_NEVER));

        // only show the most recent 10000 characters output
        textArea.addCaretListener(new CaretListener() {

            public void caretUpdate(CaretEvent event) {
                if (event.getDot() > 10000) {
                    textArea.setText(textArea.getText().substring(150));
                }
            }
        });
    }
}
