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
package org.tranche.gui.advanced;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.GenericTextArea;
import org.tranche.gui.Styles;

/**
 * Wraps a module action that returns a panel so can be tabbed.
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GenericTab extends JPanel {

    // Main GUI panel
    private final MainPanel mainPanel;
    // Pointer to current panel. Will be installable panel unless swapped
    private JPanel currentPanel;
    // This is the panel holding the GUI content in this class
    public JPanel generalPanel = new JPanel();
    // Reference to panel being installed
    public JPanel installablePanel = null;
    // Index for installed tab
    private int tabIndex = -1;
    // Keep reference to label perchance we need to match
    private String label;

    public GenericTab(final MainPanel mainPanel, final JPanel installablePanel, final String title, final String description) {
        this.mainPanel = mainPanel;
        this.installablePanel = installablePanel;

        this.label = title;

        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        setLayout(new BorderLayout());

        // create and add the general panel
        {
            generalPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            JLabel titleLabel = new GenericLabel(title);
            titleLabel.setFont(Styles.FONT_TITLE);
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridheight = GridBagConstraints.RELATIVE;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 0;
            generalPanel.add(titleLabel, gbc);

            JTextArea descriptionArea = new GenericTextArea(description);
            descriptionArea.setBackground(generalPanel.getBackground());
            descriptionArea.setWrapStyleWord(true);
            descriptionArea.setLineWrap(true);
            descriptionArea.setEditable(false);
            gbc.gridheight = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(5, 10, 5, 5);
            gbc.weighty = 1;
            generalPanel.add(descriptionArea, gbc);
        }
        add(generalPanel, BorderLayout.CENTER);
        currentPanel = generalPanel;
    }

    /**
     * Returns the panel containing the tab information.
     */
    public JPanel getTabPanel() {
        return currentPanel;
    }

    /**
     * Returns the installed panel.
     */
    public JPanel getInstalledPanel() {
        return this.installablePanel;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public String getLabel() {
        return label;
    }
}
