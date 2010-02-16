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

import org.tranche.gui.util.GUIUtil;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.tranche.util.EmailUtil;

/**
 * Panel meant for use with GenericPopupFrame.
 * @author Bryan Smith <bryanesmith at gmail.com>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class HelpPanel extends JPanel {

    // Used for achieving similar margins
    private int MARGIN = 10;
    private JTextField subjectField, fromField;
    private JTextArea messageArea;
    private JButton sendEmailButton;
    /**
     * <p>Recommended size for this popup panel when used with a generic popup frame.</p>
     */
    public static final Dimension RECOMMENDED_DIMENSION = new Dimension(400, 500);

    public HelpPanel() {
        setBackground(Styles.COLOR_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Tranche FAQ
        {
            JLabel header = new GenericLabel("FAQ");
            header.setFont(Styles.FONT_14PT_BOLD);
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN * 2, MARGIN, 0, MARGIN);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 0;
            add(header, gbc);

            JTextArea faqDescription = new GenericTextArea("A list of frequently asked questions is available on our web site.");
            faqDescription.setFont(Styles.FONT_12PT);
            faqDescription.setEditable(false);
            faqDescription.setLineWrap(true);
            faqDescription.setWrapStyleWord(true);
            faqDescription.setBackground(Styles.COLOR_BACKGROUND);
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            add(faqDescription, gbc);

            JButton faqButton = new GenericRoundedButton("Tranche FAQ");
            faqButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            GUIUtil.displayURL("https://trancheproject.org/faq.jsp");
                        }
                    };
                    t.start();
                }
            });
            add(faqButton, gbc);
        }

        // Email form
        {
            JLabel header = new GenericLabel("Contact Us");
            header.setFont(Styles.FONT_14PT_BOLD);
            gbc.insets = new Insets(MARGIN * 2, MARGIN, 0, MARGIN);
            add(header, gbc);

            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            add(new DisplayTextArea("If you have any questions, you can contact us by filling in the following form."), gbc);

            JLabel subjectLabel = new GenericLabel("Subject: ");
            subjectLabel.setFont(Styles.FONT_12PT_BOLD);
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, 0);
            add(subjectLabel, gbc);

            subjectField = new GenericTextField();
            gbc.insets = new Insets(MARGIN - 3, MARGIN, 0, MARGIN);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            add(subjectField, gbc);

            JLabel fromLabel = new GenericLabel("Your Email: ");
            fromLabel.setFont(Styles.FONT_12PT_BOLD);
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, 0);
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            add(fromLabel, gbc);

            fromField = new GenericTextField();
            gbc.insets = new Insets(MARGIN - 3, MARGIN, 0, MARGIN);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            add(fromField, gbc);

            JLabel messageLabel = new GenericLabel("Message: ");
            messageLabel.setFont(Styles.FONT_12PT_BOLD);
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            add(messageLabel, gbc);

            messageArea = new GenericTextArea();
            messageArea.setBorder(Styles.BORDER_BLACK_1);
            messageArea.setLineWrap(true);
            messageArea.setWrapStyleWord(true);
            gbc.insets = new Insets(5, MARGIN, 0, MARGIN);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weighty = 2;
            add(messageArea, gbc);

            sendEmailButton = new GenericRoundedButton("Submit message...");
            sendEmailButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            String subject = subjectField.getText().trim();
                            String from = fromField.getText().trim();
                            String message = messageArea.getText().trim();

                            // Make sure there is a subject, from and message
                            if (subject.equals("") || from.equals("") || message.equals("")) {
                                GenericOptionPane.showMessageDialog(
                                        HelpPanel.this,
                                        "Please provide a subject, an email address, and a message.",
                                        "Problem",
                                        JOptionPane.WARNING_MESSAGE);
                                return;
                            }

                            // Send email, error frame for any exception
                            try {
                                String[] recipients = new String[2];
                                recipients[0] = from;
                                recipients[1] = "proteomecommons-tranche-dev@googlegroups.com";

                                EmailUtil.sendEmail(subject, recipients, message);
                            } catch (Exception ex) {
                                ErrorFrame eframe = new ErrorFrame();
                                eframe.show(ex, HelpPanel.this);
                                return;
                            }

                            // Confirm delivery
                            GenericOptionPane.showMessageDialog(HelpPanel.this,
                                    "You message has been sent. You should get a response shortly.",
                                    "Message Sent",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    };
                    t.start();
                }
            });
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weighty = 0;
            add(sendEmailButton, gbc);
        }

        {
            JLabel header = new GenericLabel("Check Your Message Status");
            header.setFont(Styles.FONT_14PT_BOLD);
            gbc.insets = new Insets(MARGIN * 2, MARGIN, 0, MARGIN);
            add(header, gbc);

            JButton userGroupButton = new GenericRoundedButton("Tranche User Group");
            userGroupButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            GUIUtil.displayURL("http://groups.google.com/group/proteomecommons-tranche-users?lnk=srg&hl=en");
                        }
                    };
                    t.start();
                }
            });
            gbc.insets = new Insets(MARGIN, MARGIN, MARGIN * 2, MARGIN);
            add(userGroupButton, gbc);
        }
    }
}
