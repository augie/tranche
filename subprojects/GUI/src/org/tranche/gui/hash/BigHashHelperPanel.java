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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.tranche.hash.BigHash;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class BigHashHelperPanel extends JPanel {

    private final String DEFAULT_RESULT_TEXT = " ";
    private final static String base16Alphabet = "0123456789abcdef";
    private final static String base64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    private DisplayTextArea resultText = new DisplayTextArea(DEFAULT_RESULT_TEXT);
    public static final Dimension RECOMMENDED_DIMENSION = new Dimension(500, 280);

    public BigHashHelperPanel() {
        setBackground(Styles.COLOR_PANEL_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(5, 10, 0, 10);
        gbc.weightx = 1;
        JLabel yourTrancheHash = new GenericLabel(" Test your hash:");
        yourTrancheHash.setFont(Styles.FONT_14PT_BOLD);
        add(yourTrancheHash, gbc);

        gbc.insets = new Insets(3, 20, 0, 20);
        final JTextArea hashTextArea = new GenericTextArea();
        hashTextArea.setFont(Styles.FONT_11PT);
        hashTextArea.setBorder(Styles.BORDER_NONE);
        hashTextArea.setColumns(20);
        hashTextArea.setRows(1);
        hashTextArea.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                String hashStr = String.valueOf(hashTextArea.getText() + e.getKeyChar()).trim();
                if (hashStr.equals("")) {
                    resultText.setForeground(Color.BLACK);
                    resultText.setText(DEFAULT_RESULT_TEXT);
                } else {
                    try {
                        resultText.setForeground(Styles.COLOR_DARKER_RED);
                        resultText.setText(getProblemWithHash(hashStr));
                    } catch (Exception ee) {
                        resultText.setForeground(Styles.COLOR_DARKER_GREEN);
                        resultText.setText(ee.getMessage());
                    }
                }
            }
        });
        GenericScrollPane hashScrollPane = new GenericScrollPane(hashTextArea);
        hashScrollPane.setBorder(Styles.BORDER_BLACK_1);
        hashScrollPane.setHorizontalScrollBarPolicy(GenericScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        hashScrollPane.setVerticalScrollBarPolicy(GenericScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(hashScrollPane, gbc);

        gbc.insets = new Insets(5, 20, 0, 20);
        resultText.setFont(Styles.FONT_12PT_BOLD);
        add(resultText, gbc);

        gbc.insets = new Insets(5, 10, 0, 20);
        DisplayTextArea didYouText1 = new DisplayTextArea(" Did you ...");
        didYouText1.setFont(Styles.FONT_14PT_BOLD);
        didYouText1.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        add(didYouText1, gbc);

        gbc.insets = new Insets(5, 40, 10, 20);
        DisplayTextArea didYouText2 = new DisplayTextArea("... copy and paste the entire Hash? It may span multiple lines of text.\n... clear your browser's cache?\n... try to select your data set from the \"Find Hash\" list?");
        didYouText2.setFont(Styles.FONT_12PT);
        add(didYouText2, gbc);

        // add padding to the bottom
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weighty = 1;
        add(Box.createVerticalStrut(1), gbc);
    }

    /*
     *  Throws an exception when there is nothing wrong with the given hash.
     */
    public static String getProblemWithHash(String hashStr) throws Exception {
        try {
            BigHash.createHashFromString(hashStr);
        } catch (Exception ee) {
            if (hashStr.length() == 104) {
                String badCharacters = "";
                for (char c : hashStr.toCharArray()) {
                    if (!base64Alphabet.contains(String.valueOf(c))) {
                        badCharacters = badCharacters + " '" + c + "' ";
                    }
                }
                return "Invalid Character(s):  " + badCharacters;
            } else if (hashStr.length() == 152) {
                String badCharacters = "";
                for (char c : hashStr.toCharArray()) {
                    if (!base16Alphabet.contains(String.valueOf(c))) {
                        badCharacters = badCharacters + " '" + c + "' ";
                    }
                }
                return "Invalid Character(s):  " + badCharacters;
            } else {
                return "Invalid (Wrong length; Your hash: " + hashStr.length() + "; Should be: 104 or 152)";
            }
        }
        //152, 104
        if (hashStr.length() == 104) {
            throw new RuntimeException("Valid (Base-64)");
        } else {
            throw new RuntimeException("Valid (Base-16)");
        }
    }
}
