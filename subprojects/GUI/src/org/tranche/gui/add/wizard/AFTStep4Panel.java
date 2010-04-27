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

import java.awt.BorderLayout;
import java.awt.Color;
import org.tranche.gui.wizard.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.tranche.commons.TextUtil;
import org.tranche.gui.DidYouKnowPanel;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.InformationPanel;
import org.tranche.gui.PauseButton;
import org.tranche.gui.Styles;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class AFTStep4Panel extends GenericWizardPanel {

    private AddFileToolWizard wizard;
    private InformationPanel informationWidget = new InformationPanel();
    private DidYouKnowPanel didYouKnowPanel = new DidYouKnowPanel();
    private boolean loaded = false;

    public AFTStep4Panel(AddFileToolWizard wizard) {
        this.wizard = wizard;

        setBackground(Styles.COLOR_PANEL_BACKGROUND);
        setLayout(new GridBagLayout());

        // start it off as paused
        didYouKnowPanel.setPaused(true);
    }

    public void onLoad() {
        if (loaded) {
            return;
        }
        loaded = true;

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.insets = new Insets(10, 10, 10, 10);
        JPanel progressPanel = new JPanel();
        {
            progressPanel.setLayout(new BorderLayout());
            wizard.summary.getProgressBar().setBorder(BorderFactory.createMatteBorder(1, 1, 1, 0, Color.BLACK));
            progressPanel.add(wizard.summary.getProgressBar(), BorderLayout.CENTER);

            progressPanel.add(new PauseButton() {

                public void onPause() {
                    AFTStep4Panel.this.wizard.summary.getAddFileTool().setPause(true);
                }

                public void onResume() {
                    AFTStep4Panel.this.wizard.summary.getAddFileTool().setPause(false);
                }
            }, BorderLayout.EAST);
        }
        add(progressPanel, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 10;
        gbc.weighty = 10;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(0, 10, 10, 10);
        add(informationWidget, gbc);

        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 10, 20);
        add(didYouKnowPanel, gbc);

        informationWidget.set("File", wizard.summary.getAddFileTool().getFile().getAbsolutePath());
        informationWidget.set("Size", wizard.summary.getAddFileTool().getFileCount() + " files, " + TextUtil.formatBytes(wizard.summary.getAddFileTool().getSize()));
        informationWidget.set("User", wizard.step1Panel.userButton.getUser().getUserNameFromCert());
        informationWidget.set("License", wizard.summary.getAddFileTool().getLicense().getTitle());
        informationWidget.set("Title", wizard.summary.getAddFileTool().getTitle());
        informationWidget.set("Description", wizard.summary.getAddFileTool().getDescription());
        didYouKnowPanel.setPaused(false);
        Thread t = new Thread() {

            @Override
            public void run() {
                // execute the upload
                wizard.summary.start();
                wizard.summary.waitForFinish();
                if (wizard.summary.getReport().isFailed()) {
                    wizard.summary.showErrorFrame(wizard);
                } else {
                    wizard.summary.showReportGUI(wizard);
                }
                wizard.setFinalButtonLabel("Close");
            }
        };
        t.setDaemon(true);
        t.start();
    }

    public boolean onNext() {
        if (wizard.summary.getAddFileTool().isExecuting()) {
            if (JOptionPane.NO_OPTION == GenericOptionPane.showConfirmDialog(
                    GUIUtil.getAdvancedGUI(),
                    "If you close the window, the upload will stop. Really close?",
                    "Really close?",
                    JOptionPane.YES_NO_OPTION)) {
                return false;
            }
            wizard.summary.stop();
        }
        return true;
    }

    public boolean onPrevious() {
        return false;
    }
}
