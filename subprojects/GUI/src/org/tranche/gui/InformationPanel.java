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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class InformationPanel extends JPanel {

    private JPanel displayPanel = new JPanel();
    private final LinkedList<String> names = new LinkedList<String>(),  values = new LinkedList<String>();

    public InformationPanel() {
        setBackground(Styles.COLOR_PANEL_BACKGROUND);
        setLayout(new BorderLayout());
        setBorder(Styles.BORDER_BLACK_1);
        displayPanel.setBackground(Color.WHITE);
        displayPanel.setLayout(new GridBagLayout());

        GenericScrollPane scrollPane = new GenericScrollPane(displayPanel);
        scrollPane.setBorder(Styles.BORDER_NONE);
        scrollPane.setHorizontalScrollBarPolicy(GenericScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        Thread refresh = new Thread("Refresh Information Widget") {

            @Override
            public void run() {
                displayPanel.removeAll();

                Color color = Color.WHITE;
                Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);

                GridBagConstraints gbc = new GridBagConstraints();
                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.insets = new Insets(0, 0, 0, 0);
                synchronized (InformationPanel.this) {
                    for (String name : names) {
                        if (values.get(names.indexOf(name)) == null) {
                            continue;
                        }

                        gbc.gridwidth = GridBagConstraints.RELATIVE;
                        gbc.weightx = 0;
                        JLabel title = new GenericLabel(name + ":");
                        title.setBorder(border);
                        title.setOpaque(true);
                        title.setBackground(color);
                        displayPanel.add(title, gbc);

                        gbc.gridwidth = GridBagConstraints.REMAINDER;
                        gbc.weightx = 1;
                        JTextArea description = new DisplayTextArea(values.get(names.indexOf(name)));
                        description.setBorder(border);
                        description.setOpaque(true);
                        description.setBackground(color);
                        displayPanel.add(description, gbc);

                        if (color == Color.WHITE) {
                            color = Styles.COLOR_EXTRA_LIGHT_BLUE;
                        } else {
                            color = Color.WHITE;
                        }
                    }
                }

                // put in some padding
                gbc.fill = GridBagConstraints.BOTH;
                gbc.gridheight = GridBagConstraints.REMAINDER;
                gbc.insets = new Insets(0, 0, 0, 0);
                gbc.weightx = 2;
                gbc.weighty = 2;
                displayPanel.add(Box.createVerticalStrut(1), gbc);

                validate();
                repaint();
            }
        };
        refresh.setDaemon(true);
        try {
            SwingUtilities.invokeLater(refresh);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean contains(String name) {
        return names.contains(name);
    }

    public synchronized void set(String name, String value) {
        if (name != null && value != null) {
            if (contains(name)) {
                remove(name);
            }
            names.add(name);
            values.add(value);
            refresh();
        }
    }

    public synchronized void remove(String name) {
        if (names.indexOf(name) >= 0) {
            values.remove(names.indexOf(name));
        }
        names.remove(name);
        refresh();
    }
}
