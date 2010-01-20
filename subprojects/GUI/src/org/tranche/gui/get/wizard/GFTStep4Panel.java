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
package org.tranche.gui.get.wizard;

import java.awt.BorderLayout;
import java.awt.Color;
import org.tranche.gui.wizard.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.tranche.gui.DidYouKnowPanel;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.InformationPanel;
import org.tranche.gui.PauseButton;
import org.tranche.gui.Styles;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class GFTStep4Panel extends GenericWizardPanel {

    public GetFileToolWizard wizard;
    private InformationPanel infoWidget = new InformationPanel();
    private DidYouKnowPanel didYouKnowPanel = new DidYouKnowPanel();
    private boolean loaded = false;

    public GFTStep4Panel(GetFileToolWizard wizard) {
        this.wizard = wizard;
        
        setBackground(Styles.COLOR_PANEL_BACKGROUND);
        setLayout(new GridBagLayout());

        // start it off as paused
        didYouKnowPanel.setPaused(true);
    }

    public void onLoad() {
        // only load one
        if (loaded) {
            return;
        }
        loaded = true;

        GridBagConstraints gbc = new GridBagConstraints();

        // progress bar
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
                    GFTStep4Panel.this.wizard.summary.getGetFileTool().setPause(true);
                }

                public void onResume() {
                    GFTStep4Panel.this.wizard.summary.getGetFileTool().setPause(false);
                }
            }, BorderLayout.EAST);
        }
        add(progressPanel, gbc);

        // Information widget will take remaining space
        gbc.weightx = 2;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        infoWidget.setVisible(true);
        add(infoWidget, gbc);

        // add the did you know panel
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 10, 20);
        didYouKnowPanel.setPaused(false);
        add(didYouKnowPanel, gbc);

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    infoWidget.set("Hash", wizard.summary.getGetFileTool().getHash().toString());
                    infoWidget.set("Save Location", wizard.summary.getGetFileTool().getSaveFile().getAbsolutePath());
                    if (wizard.summary.getGetFileTool().getMetaData().isProjectFile()) {
                        infoWidget.set("Uploader", wizard.summary.getProjectSummary().uploader);
                        infoWidget.set("Title", wizard.summary.getProjectSummary().title);
                        infoWidget.set("Description", wizard.summary.getProjectSummary().description);
                    } else {
                        infoWidget.set("Uploader", wizard.summary.getGetFileTool().getMetaData().getSignature().getUserName());
                        infoWidget.set("Name", wizard.summary.getGetFileTool().getMetaData().getName());
                    }

                    wizard.summary.start();
                    wizard.summary.waitForFinish();
                    wizard.setFinalButtonLabel("Close");
                    wizard.repaint();
                } catch (Exception e) {
                    wizard.ef.show(e, wizard);
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    public boolean onNext() {
        if (wizard.summary.getGetFileTool().isExecuting()) {
            if (JOptionPane.NO_OPTION == GenericOptionPane.showConfirmDialog(
                    GUIUtil.getAdvancedGUI(),
                    "If you close the window, the download will stop. Really close?",
                    "Really close?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
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
