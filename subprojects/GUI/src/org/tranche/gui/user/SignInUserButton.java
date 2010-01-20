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
import org.tranche.gui.util.GUIUtil;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.tranche.users.UserZipFile;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SignInUserButton extends GenericRoundedButton implements ActionListener {

    private static final String DEFAULT_TEXT = "Sign In";
    private UserZipFile user = null;
    public Image icon = null;

    public SignInUserButton() {
        super(DEFAULT_TEXT);
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        // show the sign in frame
        final SignInFrame logInFrame = new SignInFrame();

        if (icon != null) {
            logInFrame.setIconImage(icon);
        }

        logInFrame.setLocationRelativeTo(SignInUserButton.this);
        logInFrame.setVisible(true);

        Thread t = new Thread() {

            @Override
            public void run() {
                // getUser() in loginFrame pauses until the user is returned
                UserZipFile uzf = logInFrame.getPanel().getUser();
                if (uzf != SignInPanel.BLANK_USER_ZIP_FILE) {
                    setUser(uzf);
                } else {
                    setUser(null);
                }
                // will return immediately
                GUIUtil.setUserEmail(logInFrame.getPanel().getEmail());
                // close the frame
                logInFrame.dispose();
            }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public UserZipFile getUser() {
        return user;
    }

    public void setUser(UserZipFile user) {
        this.user = user;
        if (user != null) {
            setText(user.getUserNameFromCert());
        } else {
            setText(DEFAULT_TEXT);
        }
    }
}
