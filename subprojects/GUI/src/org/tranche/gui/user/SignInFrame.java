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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import org.tranche.ConfigureTranche;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SignInFrame extends GenericPopupFrame {

    private SignInPanel logInPanel;

    public SignInFrame() {
        super("Sign In", new SignInPanel());
        logInPanel = (SignInPanel) super.getComponent();
        int height = 300;
        String signUpURL = ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_SIGN_UP_URL);
        if (signUpURL == null || signUpURL.equals("")) {
            height -= 50;
        }
        setSize(400, height);
        setDefaultCloseOperation(GenericFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        if (logInPanel.abq.peek() == null) {
                            logInPanel.abq.offer(SignInPanel.BLANK_USER_ZIP_FILE);
                        }
                    }
                };
                t.start();
            }
        });
    }

    public SignInPanel getPanel() {
        return logInPanel;
    }
}
