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

import javax.swing.JFrame;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class EmailFrame extends GenericPopupFrame {

    private EmailPanel emailPanel = null;

    public EmailFrame(String emailSubject, String emailText) {
        super("Send Email", new EmailPanel(emailSubject, emailText), false);
        emailPanel = (EmailPanel) super.getComponent();
        emailPanel.setDisposeOnSend(this);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(550, 400);
    }

    public EmailPanel getPanel() {
        return emailPanel;
    }
}
