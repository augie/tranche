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

import org.tranche.gui.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;
import org.tranche.ConfigureTranche;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class LogoPanel extends JPanel {

    public LogoPanel() {
        setBackground(Color.WHITE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // add the growing logo
        add(new TrancheLogoPanel(), gbc);

        JPanel fillerPanel1 = new JPanel();
        fillerPanel1.setBackground(Color.WHITE);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 5);
        add(fillerPanel1, gbc);

        JPanel tranchePanel = new JPanel();
        {
            // set the background color
            tranchePanel.setBackground(Color.WHITE);

            // set the layout
            tranchePanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc2 = new GridBagConstraints();

            // create the individual labels
            JLabel trancheTool = new GenericLabel("Tranche");
            trancheTool.setFont(new Font("Times New Roman", Font.PLAIN, 38));
            JLabel buildNumber = new GenericLabel("(Build #@buildNumber)");
            buildNumber.setFont(new Font("Times New Roman", Font.PLAIN, 15));
            JLabel networkName = new GenericLabel(ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_NAME));
            networkName.setFont(new Font("Times New Roman", Font.BOLD, 16));
            networkName.setForeground(Styles.COLOR_DARK_RED);

            // I got null pointers on this. What should be the fallback behavior? Why did
            // ProteomeCommons.org Tranche project built on machine give null?
            String urlText = "";
            if (ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_HOME_URL) != null) {
                urlText = ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_HOME_URL).replace("http://", "").replace("https://", "");
            }
            JLabel url = new GenericLabel(urlText);

            url.setForeground(Color.DARK_GRAY);
            url.setFont(new Font("Times New Roman", Font.BOLD, 14));

            // create and add the first row
            {
                JPanel panel = new JPanel();
                panel.setLayout(new GridBagLayout());
                GridBagConstraints gbc3 = new GridBagConstraints();
                panel.setBackground(tranchePanel.getBackground());
                gbc3.anchor = gbc3.SOUTHWEST;
                gbc3.insets = new Insets(0, 0, 0, 10);
                panel.add(trancheTool, gbc3);
                gbc3.insets = new Insets(0, 0, 7, 0);
                panel.add(buildNumber, gbc3);

                gbc2.anchor = gbc2.SOUTHWEST;
                gbc2.gridy = 0;
                tranchePanel.add(panel, gbc2);
            }

            // create and add the next row
            {
                JPanel panel = new JPanel();
                panel.setLayout(new GridBagLayout());
                GridBagConstraints gbc3 = new GridBagConstraints();
                panel.setBorder(Styles.BORDER_NONE);
                panel.setBackground(tranchePanel.getBackground());
                gbc3.insets = new Insets(0, 0, 0, 0);
                panel.add(networkName, gbc3);

                gbc2.gridy = 1;
                tranchePanel.add(panel, gbc2);
            }

            // add the url
            gbc2.gridy = 2;
            gbc2.gridwidth = GridBagConstraints.REMAINDER;
            tranchePanel.add(url, gbc2);
        }
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        add(tranchePanel, gbc);

        JPanel fillerPanel2 = new JPanel();
        fillerPanel2.setBackground(Color.WHITE);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 5);
        add(fillerPanel2, gbc);

        JTextArea billboard = new GenericTextArea(ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_DESCRIPTION));
        billboard.setLineWrap(true);
        billboard.setWrapStyleWord(true);
        billboard.setEditable(false);
        billboard.setFont(Styles.FONT_11PT_BOLD);
        billboard.setForeground(Color.DARK_GRAY);
        billboard.setBorder(new LineBorder(Color.WHITE, 10, true));
        billboard.setBackground(Color.WHITE);

        GenericScrollPane pane = new GenericScrollPane(billboard);
        pane.setBorder(Styles.BORDER_NONE);
        pane.setVerticalScrollBarPolicy(GenericScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pane.setHorizontalScrollBarPolicy(GenericScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gbc.insets = new Insets(10, 20, 10, 10);
        gbc.weightx = 3;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        add(pane, gbc);
    }
}
