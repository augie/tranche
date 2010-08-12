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

import org.tranche.gui.hash.GenericBigHashPanel;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.*;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith <bryanesmith at gmail dot com>
 */
public class LeftPanel extends JPanel {

    // First section -- the permanent menu
    private LeftMenu permanentMenu = new LeftMenu();
    private JButton downloadByHashButton, openByHashButton, uploadMenuItem;
    // Second section -- the contextual menu
    private LeftMenu contextMenu = new LeftMenu();
    private final GridBagConstraints savedGBC;

    public LeftPanel() {
        setBackground(Styles.COLOR_EXTRA_LIGHT_GRAY);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;

        // Permanent menu
        {
            // Create and add the "download by hash string" button
            downloadByHashButton = LeftMenu.createLeftMenuButton("New Download");
            downloadByHashButton.setToolTipText("Opens the Download Tool");
            downloadByHashButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            GUIUtil.openGetFileToolWizard(null, null, null, null, GUIUtil.getAdvancedGUI());
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            permanentMenu.addButton(downloadByHashButton);

            // Create and add the "upload" button
            uploadMenuItem = LeftMenu.createLeftMenuButton("New Upload");
            uploadMenuItem.setToolTipText("Opens the Upload Tool");
            uploadMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            GUIUtil.openAddFileToolWizard(GUIUtil.getAdvancedGUI());
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            permanentMenu.addButton(uploadMenuItem);

            // Create and add the "browse by hash string" button
            openByHashButton = LeftMenu.createLeftMenuButton("Open By Hash");
            openByHashButton.setToolTipText("Open the data set with the given hash.");
            openByHashButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    // Take wait off the dispatch thread
                    Thread h = new Thread() {

                        @Override
                        public void run() {
                            GenericBigHashPanel panel = new GenericBigHashPanel("Open Data Set");
                            panel.setOnlyProjectFiles(true);
                            GenericPopupFrame frame = new GenericPopupFrame("Open By Hash", panel);

                            panel.setFrame(frame);
                            frame.setSizeAndCenter(GenericBigHashPanel.DIMENSION);
                            frame.setMinimumSize(GenericBigHashPanel.DIMENSION);
                            frame.pack();
                            frame.setVisible(true);

                            GUIUtil.openProjectToolGUI(panel.receiveBigHash(), GUIUtil.getAdvancedGUI());
                        }
                    };
                    h.setDaemon(true);
                    h.start();
                }
            });
            permanentMenu.addButton(openByHashButton);

            // Add the bar
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 10, 0, 10);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1.0;
            gbc.weighty = 0;
            add(permanentMenu, gbc);
        }

        // Separator
        {
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(10, 0, 10, 0);
            gbc.weightx = 1;
            gbc.weighty = 0;
            JSeparator separator = new JSeparator();
            separator.setForeground(Color.DARK_GRAY);
            add(separator, gbc);
        }

        // Contextual options
        {
            // Add the bar
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 10, 0, 10);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 0;
            savedGBC = gbc;
            add(contextMenu, gbc);
        }

        // put in some padding
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 10, 10, 30);
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(Box.createVerticalStrut(1), gbc);
//        }
    }

    public void setContextMenu(final LeftMenu newContextMenu) {
        remove(contextMenu);
        validate();
        repaint();
        contextMenu = newContextMenu;
        addImpl(contextMenu, savedGBC, 2);
        validate();
        repaint();
    }
}
