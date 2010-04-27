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
package org.tranche.gui.add;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import org.tranche.gui.DisplayTextArea;
import org.tranche.gui.GenericRoundedButton;
import org.tranche.gui.Styles;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class AddFileToolReportPanel extends JPanel {

    private DisplayTextArea trancheHash = new DisplayTextArea();
    private GenericRoundedButton emailReceiptButton = new GenericRoundedButton("Email Receipt"), saveReceiptButton = new GenericRoundedButton("Save Receipt");

    public AddFileToolReportPanel(final UploadSummary us) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 10, 0, 10);
        gbc.weightx = 1;
        gbc.weighty = 0;
        add(new DisplayTextArea("Your upload is complete. A receipt for your upload has been emailed to you. The following is the Tranche hash that represents your upload:"), gbc);

        gbc.insets = new Insets(10, 10, 10, 10);
        trancheHash.setFont(Styles.FONT_12PT_BOLD);
        trancheHash.setText(us.getReport().getHash().toString());
        add(trancheHash, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 5, 3, 5);
        add(saveReceiptButton, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(emailReceiptButton, gbc);

        // add the ability to save the hash to a file
        saveReceiptButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                us.saveReceipt(AddFileToolReportPanel.this);
            }
        });
        // add the ability to email the hash
        emailReceiptButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override()
                    public void run() {
                        us.showEmailFrame(AddFileToolReportPanel.this);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
    }
}
