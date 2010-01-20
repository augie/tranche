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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import org.tranche.gui.util.GUIUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class RandomPassphraseButton extends GenericButton implements ClipboardOwner {

    public RandomPassphraseButton(final JPasswordField passphraseField, final JPasswordField confirmPassphraseField) {
        super("Random");
        setBorder(Styles.BORDER_BLACK_1_PADDING_SIDE_2);
        setFont(Styles.FONT_10PT);
        setToolTipText("Generate a random 20 character passphrase.");
        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        String p = GUIUtil.generateRandomPassphrase(20);
                        passphraseField.setText(p);
                        confirmPassphraseField.setText(p);
                        int returnInt = GenericOptionPane.showConfirmDialog(RandomPassphraseButton.this, "Your randomly generated passphrase: " + p + "\nDo you want to copy this to your clipboard?", "Random Passphrase", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        if (returnInt == JOptionPane.YES_OPTION) {
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(p), RandomPassphraseButton.this);
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
    }

    /**
     * For the clipboardowner abstract
     * @param clipboard
     * @param contents
     */
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
