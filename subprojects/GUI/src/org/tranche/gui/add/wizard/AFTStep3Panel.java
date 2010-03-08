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
package org.tranche.gui.add.wizard;

import org.tranche.gui.wizard.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.apache.commons.httpclient.NameValuePair;
import org.tranche.gui.add.UploadPool;
import org.tranche.gui.DisplayTextArea;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.GenericTextArea;
import org.tranche.gui.GenericCheckBox;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericScrollPane;
import org.tranche.license.License;
import org.tranche.gui.Styles;
import org.tranche.gui.TextPanel;
import org.tranche.gui.util.GUIUtil;
import org.tranche.meta.MetaDataAnnotation;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class AFTStep3Panel extends GenericWizardPanel {

    private AddFileToolWizard wizard;
    private JLabel headerLabel = new GenericLabel("");
    public CustomLicensePanel customLicensePanel = new CustomLicensePanel();
    private CC0LicensePanel cc0LicensePanel = new CC0LicensePanel();

    public AFTStep3Panel(AddFileToolWizard wizard) {
        this.wizard = wizard;

        setBackground(Styles.COLOR_PANEL_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // title
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 0, 10);
        headerLabel.setFont(Styles.FONT_18PT);
        add(headerLabel, gbc);

        // license panels
        gbc.insets = new Insets(5, 25, 15, 15);
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        add(cc0LicensePanel, gbc);
        add(customLicensePanel, gbc);
    }

    public void onLoad() {
        headerLabel.setText("You have chosen to use: " + wizard.summary.getAddFileTool().getLicense().getTitle());
        cc0LicensePanel.setVisible(wizard.summary.getAddFileTool().getLicense() == License.CC0);
        cc0LicensePanel.agreeBox1.setSelected(false);
        cc0LicensePanel.agreeBox2.setSelected(false);
        customLicensePanel.setVisible(wizard.summary.getAddFileTool().getLicense() != License.CC0);
        customLicensePanel.agreeBox.setSelected(false);
    }

    public boolean onNext() {
        if (wizard.summary.getAddFileTool().getLicense() == null) {
            return false;
        } else if (wizard.summary.getAddFileTool().getLicense() == License.CC0) {
            if (!cc0LicensePanel.agreeBox1.isSelected() || !cc0LicensePanel.agreeBox2.isSelected()) {
                GenericOptionPane.showMessageDialog(AFTStep3Panel.this.getParent(),
                        "Cannot continue. Please check the boxes indicating your agreement.",
                        "Cannot Continue",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else {
            if (customLicensePanel.customLicenseTextArea.getText().trim().equals("")) {
                GenericOptionPane.showMessageDialog(AFTStep3Panel.this.getParent(),
                        "Cannot continue. Please provide your custom license text.",
                        "Cannot Continue",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (!customLicensePanel.agreeBox.isSelected()) {
                GenericOptionPane.showMessageDialog(AFTStep3Panel.this.getParent(),
                        "Cannot continue. Please check the boxes indicating your agreement.",
                        "Cannot Continue",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            wizard.summary.getAddFileTool().getLicense().setDescription(customLicensePanel.customLicenseTextArea.getText().trim());
        }
        // set the servers
        wizard.summary.getAddFileTool().setServersToUse(wizard.serversFrame.getPanel().getSelectedHosts());
        wizard.summary.getAddFileTool().setStickyServers(wizard.stickyServersFrame.getPanel().getSelectedHosts());
        // set the meta data annotations
        for (NameValuePair pair : wizard.annotationFrame.getPanel().getNameValuePairs()) {
            wizard.summary.getAddFileTool().addMetaDataAnnotation(new MetaDataAnnotation(pair.getName(), pair.getValue()));
        }
        // add user's email address
        wizard.summary.getAddFileTool().addConfirmationEmail(GUIUtil.getUserEmail());
        // start
        UploadPool.set(wizard.summary);
        // send tot the advanced GUI
        if (GUIUtil.getAdvancedGUI() != null) {
            wizard.setVisible(false);
            GUIUtil.getAdvancedGUI().mainPanel.setTab(GUIUtil.getAdvancedGUI().mainPanel.uploadsPanel);
            return false;
        }
        return true;
    }

    public boolean onPrevious() {
        return true;
    }

    private class CC0LicensePanel extends JPanel {

        private JPanel cc0Text = new TextPanel(License.CC0.getDescription());
        public GenericCheckBox agreeBox1 = new GenericCheckBox(""), agreeBox2 = new GenericCheckBox("");

        public CC0LicensePanel() {
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 0, 0);
            add(new GenericScrollPane(cc0Text), gbc);

            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.insets = new Insets(10, 0, 0, 5);
            add(agreeBox1, gbc);

            gbc.weightx = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(10, 0, 0, 0);
            add(new DisplayTextArea("I hereby waive all copyright and related or neighboring rights together with all associated claims and causes of action with respect to this work to the extent possible under the law."), gbc);

            gbc.weightx = 0;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(10, 0, 0, 5);
            add(agreeBox2, gbc);

            gbc.insets = new Insets(10, 0, 0, 0);
            gbc.weightx = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            add(new DisplayTextArea("I have read and understand the terms and intended legal effect of CC0, and hereby voluntarily elect to apply it to this work."), gbc);
        }
    }

    public class CustomLicensePanel extends JPanel {

        public JTextArea customLicenseTextArea = new GenericTextArea();
        public GenericCheckBox agreeBox = new GenericCheckBox("");

        public CustomLicensePanel() {
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(0, 0, 3, 0);
            add(new GenericLabel("License text:"), gbc);

            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 0, 0);
            add(new GenericScrollPane(customLicenseTextArea), gbc);

            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 1;
            gbc.weighty = 0;
            gbc.weightx = 0;
            gbc.insets = new Insets(10, 0, 0, 5);
            add(agreeBox, gbc);

            gbc.weightx = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(10, 0, 0, 0);
            add(new DisplayTextArea("I assert that I am legally permitted to upload this data set with the given license."), gbc);
        }
    }
}
