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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ArrayBlockingQueue;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class PassphraseFrame extends GenericFrame implements ActionListener {

    private JLabel titleLabel = new GenericLabel("Enter Passphrase:"), lockIconLabel = new GenericLabel("");
    private JTextArea description = new GenericTextArea("Enter your passphrase in the text box below. When finished, click the button or press enter.");
    private JPasswordField passwordField = new JPasswordField();
    private JButton enterButton = new GenericButton("Enter");
    private ArrayBlockingQueue<String> abq = new ArrayBlockingQueue(1);
    private boolean cannotBeBlank = false;

    public PassphraseFrame() {
        // return null when the window is closed
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    if (cannotBeBlank) {
                        abq.put(null);
                    } else {
                        abq.offer("");
                    }
                } catch (Exception ee) {
                }
            }
        });

        if (cannotBeBlank) {
            enterButton.setEnabled(!getCurrentPassphrase().equals(""));
        }
        addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                if (cannotBeBlank) {
                    enterButton.setEnabled(!getCurrentPassphrase().equals(""));
                }
            }
        });

        setTitle("Enter Passphrase");
        setSize(new Dimension(300, 190));
        setPreferredSize(new Dimension(300, 190));
        setLayout(new GridBagLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridy = 0;
        titleLabel.setFont(Styles.FONT_TITLE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 0));
        add(titleLabel, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 1;
        gbc.weighty = 2;
        description.setFont(Styles.FONT_12PT);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBackground(Styles.COLOR_BACKGROUND);
        description.setFocusable(false);
        description.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        add(description, gbc);

        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridheight = 2;
        gbc.gridwidth = 1;
        try {
            lockIconLabel.setIcon(new ImageIcon(ImageIO.read(PassphraseFrame.class.getResourceAsStream("/org/tranche/gui/image/lock.gif"))));
        } catch (Exception e) {
        }
        lockIconLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.BLACK));
        add(lockIconLabel, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 2;
        gbc.gridheight = 1;
        gbc.gridx = 1;
        passwordField.addActionListener(this);
        passwordField.setBorder(Styles.BORDER_NONE);
        add(passwordField, gbc);

        gbc.weighty = 2;
        gbc.gridy = 3;
        try {
            enterButton.setIcon(new ImageIcon(ImageIO.read(PassphraseFrame.class.getResourceAsStream("/org/tranche/gui/image/lock_yellow.gif"))));
            enterButton.setIconTextGap(15);
        } catch (Exception e) {
        }
        enterButton.addActionListener(this);
        enterButton.setBorder(Styles.BORDER_BUTTON_TOP);
        add(enterButton, gbc);
    }

    /**
     * 
     * @return
     * @throws RuntimeException
     */
    public String getPassphrase() throws RuntimeException {
        try {
            return abq.take();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Can't get passphrase.");
        }
    }
    /**
     * 
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    String currentPassphrase = getCurrentPassphrase();
                    String trimmedPassphrase = currentPassphrase.trim();
                    if (trimmedPassphrase.length() < currentPassphrase.length()) {
                        int pressedButton = GenericOptionPane.showConfirmDialog(PassphraseFrame.this, "The passphrase you have entered contains extra space at the beginning or end.\nDo you want to remove this whitespace?", "Whitespace Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (pressedButton == JOptionPane.YES_OPTION) {
                            passwordField.setText(trimmedPassphrase);
                            currentPassphrase = trimmedPassphrase;
                        }
                    }
                    if (cannotBeBlank && currentPassphrase.equals("")) {
                        abq.put(null);
                    } else {
                        abq.add(currentPassphrase);
                    }
                    // hide this frame
                    dispose();
                } catch (Exception ee) {
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void addKeyListener(KeyListener listener) {
        passwordField.addKeyListener(listener);
    }

    public String getCurrentPassphrase() {
        if (passwordField.getPassword() == null) {
            return "";
        } else {
            return new String(passwordField.getPassword());
        }
    }

    public void setDescription(String descriptionText) {
        description.setText(descriptionText);
    }

    public void setCannotBeBlank(boolean cannotBeBlank) {
        this.cannotBeBlank = cannotBeBlank;
        enterButton.setEnabled(!(cannotBeBlank ^ !getCurrentPassphrase().equals("")));
    }
}
