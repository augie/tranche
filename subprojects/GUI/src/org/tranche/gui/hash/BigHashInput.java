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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import org.tranche.gui.ConfigureTrancheGUI;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.GenericTextArea;
import org.tranche.gui.Styles;
import org.tranche.hash.Base64Util;
import org.tranche.hash.BigHash;
import org.tranche.util.PreferencesUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class BigHashInput extends JPanel {

    private final Set<BigHashInputListener> listeners = new HashSet<BigHashInputListener>();
    private final GenericTextArea textArea = new GenericTextArea();
    private final GenericScrollPane scrollPane = new GenericScrollPane(textArea);
    private final HashSuggestionWindow suggestionWindow = new HashSuggestionWindow(textArea);
    private BigHash hash = null;

    public BigHashInput() {
        textArea.setColumns(40);
        textArea.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
        textArea.setBackground(Styles.COLOR_BACKGROUND_LIGHT);
        textArea.setToolTipText("Enter the hash of the data set that you would like to download.");

        scrollPane.setBorder(Styles.BORDER_BLACK_1);
        scrollPane.setMinimumSize(new Dimension(50, 35));
        scrollPane.setHorizontalScrollBarPolicy(GenericScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(GenericScrollPane.VERTICAL_SCROLLBAR_NEVER);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        // add a listener to clean up text according to RFC 4648
        // http://tools.ietf.org/html/rfc4648#section-4
        textArea.addCaretListener(new CaretListener() {

            private String lastString = "";

            public void caretUpdate(CaretEvent e) {
                if (hash != null && hash.toString().equals(textArea.getText().trim())) {
                    return;
                }
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        // make sure the suggestion window is closed
                        suggestionWindow.setVisible(false);

                        // clean up the text to be Base64 friendly -- handle copy/paste from various sources
                        String hashString = Base64Util.cleanUpBase64(textArea.getText().trim());

                        // assume Base64
                        if (hashString.length() <= 106 && hashString.length() >= 102) {
                            // Hack: should be 104 characters. Add padding...
                            if (hashString.length() != 104) {
                                // If only 102, add ==
                                if (hashString.length() == 102) {
                                    hashString = hashString + "==";
                                }
                                // If only 101, add =
                                if (hashString.length() == 103) {
                                    hashString = hashString + "=";
                                }
                                // If more than two equals signs at end, remove
                                if (hashString.length() > 104) {
                                    hashString = hashString.substring(0, 102) + "==";
                                }
                            }
                        }

                        // PATCH: PRIDE has hashes in URL's link to this tool with the format:
                        //        [BASE 16 HASH]/path/to/some/file
                        if (hashString.length() > BigHash.HASH_STRING_LENGTH_BASE16 && hashString.charAt(BigHash.HASH_STRING_LENGTH_BASE16) == '/') {
                            // Simply try to create BigHash from base16-encoded version
                            hashString = hashString.substring(0, BigHash.HASH_STRING_LENGTH_BASE16);
                        }

                        // PATCH: PRIDE _MIGHT_ have hashes in URL's link to this tool with the format:
                        //        [BASE 64 HASH]/path/to/some/file
                        if (hashString.length() > BigHash.HASH_STRING_LENGTH_BASE64 && hashString.charAt(BigHash.HASH_STRING_LENGTH_BASE64) == '/') {
                            // Simply try to create BigHash from base64-encoded version
                            hashString = hashString.substring(0, BigHash.HASH_STRING_LENGTH_BASE64);
                        }

                        BigHash prevHash = hash;
                        try {
                            hash = BigHash.createHashFromString(hashString);
                        } catch (Exception ee) {
                            hash = null;
                        }

                        // reset the text to the user's input
                        if (!textArea.getText().equals(hashString)) {
                            textArea.setText(hashString);
                        }
                        if (!lastString.equals(hashString)) {
                            lastString = hashString;
                            fireTextChanged(hashString);
                            if (!hashString.equals("") && hash == null) {
                                boolean showAutoComplete = PreferencesUtil.getBoolean(ConfigureTrancheGUI.PROP_AUTO_COMPLETE_HASH);
                                if (showAutoComplete) {
                                    suggestionWindow.setSize(getWidth(), suggestionWindow.getHeight());
                                    Point p = getLocationOnScreen();
                                    suggestionWindow.setLocation((int) (p.getX()), (int) (p.getY() + textArea.getHeight()));
                                    suggestionWindow.refresh();
                                }
                            }
                        }

                        // fire event
                        if (!((prevHash == null && hash == null) || (prevHash != null && hash != null && prevHash.equals(hash)))) {
                            fireHashChanged(hash);
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
    }

    public GenericTextArea getTextArea() {
        return textArea;
    }

    public void setHash(BigHash hash) {
        textArea.setText(hash.toString());
        fireTextChanged(hash.toString());
        fireHashChanged(hash);
    }

    public BigHash getHash() {
        return hash;
    }

    private void fireHashChanged(BigHash hash) {
        synchronized (listeners) {
            for (BigHashInputListener l : listeners) {
                l.hashChanged(hash);
            }
        }
    }

    private void fireTextChanged(String text) {
        synchronized (listeners) {
            for (BigHashInputListener l : listeners) {
                l.textChanged(text);
            }
        }
    }

    public void addListener(BigHashInputListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }
}
