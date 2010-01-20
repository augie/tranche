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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.tranche.util.EmailUtil;
import org.tranche.util.Text;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class EmailPanel extends JPanel implements ActionListener {

    private DisplayTextArea emailSubjectArea = new DisplayTextArea(),  emailTextArea = new DisplayTextArea();
    private GenericTextField addressField = new GenericTextField();
    private JFrame disposeOnSend;

    public EmailPanel(String emailSubject, String emailText) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(10, 10, 0, 10);
        gbc.weightx = 1;
        gbc.weighty = 0;
        add(new GenericLabel("Email address(es): "), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 0, 0, 10);
        gbc.weightx = 2;
        addressField.addActionListener(this);
        add(addressField, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 1;
        gbc.insets = new Insets(10, 10, 0, 15);
        add(new GenericLabel("Subject:"), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 0, 0, 10);
        emailSubjectArea.setFont(Styles.FONT_12PT_BOLD);
        emailSubjectArea.setText(emailSubject);
        add(emailSubjectArea, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 1;
        gbc.insets = new Insets(10, 10, 0, 15);
        add(new GenericLabel("Text:"), gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 0, 0, 10);
        gbc.weighty = 1;
        emailTextArea.setFont(Styles.FONT_10PT_BOLD);
        emailTextArea.setText(emailText);
        add(new GenericScrollPane(emailTextArea), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weighty = 0;
        GenericRoundedButton emailButton = new GenericRoundedButton("Send Email");
        emailButton.addActionListener(this);
        add(emailButton, gbc);
    }

    public void setDisposeOnSend(JFrame disposeOnSend) {
        this.disposeOnSend = disposeOnSend;
    }

    public void actionPerformed(ActionEvent e) {
        Thread t = new Thread() {

            @Override
            public void run() {
                String address = addressField.getText();
                // Alert that no email address was provided
                if (address == null || address.trim().equals("")) {
                    GenericOptionPane.showMessageDialog(EmailPanel.this.getParent(), "You must provide an email address to continue.");
                    return;
                }
                // send email
                try {
                    EmailUtil.sendEmail(emailSubjectArea.getText(), address.split(","), emailTextArea.getText());
                    if (disposeOnSend != null) {
                        disposeOnSend.dispose();
                    }
                    GenericOptionPane.showMessageDialog(EmailPanel.this.getParent(), "An email was sent to <" + address + ">.");
                } catch (Exception ex) {
                    GenericOptionPane.showMessageDialog(EmailPanel.this.getParent(), "ERROR: Could not send message to <" + address + ">. Please try again. If the this fails again, you could save your message instead." + Text.getNewLine() + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }
}
