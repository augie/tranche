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
package org.tranche.gui.user;

import org.tranche.gui.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ArrayBlockingQueue;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.apache.commons.httpclient.protocol.Protocol;
import org.tranche.ConfigureTranche;
import org.tranche.gui.util.GUIUtil;
import org.tranche.security.EasySSLProtocolSocketFactory;
import org.tranche.users.InvalidSignInException;
import org.tranche.users.UserZipFile;
import org.tranche.users.UserZipFileUtil;
import org.tranche.util.TempFileUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SignInPanel extends JPanel implements ActionListener {

    public static final UserZipFile BLANK_USER_ZIP_FILE = new UserZipFile(TempFileUtil.createTemporaryFile());
    private JTextField userNameTextField = new GenericTextField();
    private JPasswordField passwordField = new JPasswordField();
    private String email = null;
    public ArrayBlockingQueue<UserZipFile> abq = new ArrayBlockingQueue<UserZipFile>(1);
    private ErrorFrame ef = new ErrorFrame();

    static {
        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
    }

    public SignInPanel() {
        setBackground(Styles.COLOR_PANEL_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(7, 7, 0, 0);
        gbc.weightx = 0;
        gbc.weighty = 1;
        add(new GenericLabel("Unique Name / Email:"), gbc);

        userNameTextField.addActionListener(this);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(7, 7, 0, 7);
        gbc.weightx = 1;
        add(userNameTextField, gbc);

        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(7, 7, 0, 0);
        gbc.weightx = 0;
        add(new GenericLabel("Password:"), gbc);

        passwordField.addActionListener(this);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(7, 7, 0, 7);
        gbc.weightx = 1;
        add(passwordField, gbc);

        GenericRoundedButton signInButton = new GenericRoundedButton("Sign In");
        signInButton.addActionListener(this);
        gbc.insets = new Insets(7, 7, 7, 7);
        add(signInButton, gbc);

        if (ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_SIGN_UP_URL) != null && !ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_SIGN_UP_URL).equals("")) {
            JLabel noAccountLabel = new GenericLabel("No Account?");
            noAccountLabel.setFont(Styles.FONT_14PT_BOLD);
            gbc.insets = new Insets(10, 7, 0, 7);
            add(noAccountLabel, gbc);

            GenericRoundedButton applyButton = new GenericRoundedButton("Apply For An Account");
            applyButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override()
                        public void run() {
                            GUIUtil.displayURL(ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_SIGN_UP_URL));
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            gbc.insets = new Insets(7, 7, 0, 7);
            add(applyButton, gbc);
        }

        JLabel userCertLabel = new GenericLabel("Have a User File?");
        userCertLabel.setFont(Styles.FONT_14PT_BOLD);
        gbc.insets = new Insets(10, 7, 0, 7);
        add(userCertLabel, gbc);

        GenericRoundedButton logInWithCertButton = new GenericRoundedButton("Sign In With My User Certificate");
        logInWithCertButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final JFileChooser jfc = GUIUtil.makeNewFileChooser();
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        UserZipFile uzf = null;
                        try {
                            uzf = GUIUtil.promptForUserFile(jfc, SignInPanel.this);
                        } catch (Exception e) {
                            ef.show(e, SignInPanel.this);
                        }
                        if (uzf != null) {
                            abq.offer(uzf);
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        gbc.insets = new Insets(7, 7, 7, 7);
        add(logInWithCertButton, gbc);
    }

    public String getEmail() {
        return email;
    }

    public UserZipFile getUser() {
        try {
            return abq.take();
        } catch (Exception e) {
            return BLANK_USER_ZIP_FILE;
        }
    }

    public void actionPerformed(ActionEvent e) {
        Thread t = new Thread() {

            @Override
            public void run() {

                // get the entered information
                String name = userNameTextField.getText(), password = String.valueOf(passwordField.getPassword());

                // check the required fields are filled out
                if (name == null || name.trim().equals("")) {
                    GenericOptionPane.showMessageDialog(SignInPanel.this, "\"User Name\" cannot be blank.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } else if (password == null || password.trim().equals("")) {
                    GenericOptionPane.showMessageDialog(SignInPanel.this, "\"Password\" cannot be blank.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // show the user something is going on
                IndeterminateProgressBar progressBar = new IndeterminateProgressBar("Logging In");
                progressBar.setLocationRelativeTo(SignInPanel.this);
                progressBar.start();

                try {
                    UserZipFile uzf = UserZipFileUtil.getUserZipFile(name.trim(), password.trim());
                    email = uzf.getEmail();
                    abq.offer(uzf);
                    progressBar.stop();
                } catch (InvalidSignInException e) {
                    progressBar.stop();
                    GenericOptionPane.showMessageDialog(null, e.getMessage(), "Could Not Sign In", JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    progressBar.stop();
                    ef.show(e, SignInPanel.this);
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }
}
