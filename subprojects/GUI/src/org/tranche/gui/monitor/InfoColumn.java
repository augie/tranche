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
package org.tranche.gui.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedList;
import javax.swing.JPanel;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.Styles;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class InfoColumn extends JPanel {

    private Monitor monitor;
    private JPanel panel = new JPanel();
    private final LinkedList<String> names = new LinkedList<String>(),  values = new LinkedList<String>();

    public InfoColumn(Monitor monitor) {
        this.monitor = monitor;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        panel.setBackground(getBackground());
        add(panel, BorderLayout.CENTER);
        refresh();
    }

    public synchronized void put(String name, String value) {
        names.add(name);
        values.add(value);
    }

    public synchronized void clear() {
        names.clear();
        values.clear();
    }

    public synchronized void refresh() {
        JPanel newPanel = new JPanel();
        newPanel.setLayout(new GridBagLayout());
        newPanel.setBackground(getBackground());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(2, 15, 2, 15);

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i), shortValue;
            if (value.length() > 20) {
                shortValue = value.substring(0, 20) + "...";
            } else {
                shortValue = value;
            }

            String name = names.get(i), shortName;
            if (name.length() > 20) {
                shortName = name.substring(0, 20) + "...";
            } else {
                shortName = name;
            }

            String nameLabelText = "";
            if (!shortName.equals("")) {
                nameLabelText = shortName + ":";
            }

            GenericLabel nameLabel = new GenericLabel(nameLabelText);
            nameLabel.setToolTipText(name);
            nameLabel.setFont(Styles.FONT_11PT);
            nameLabel.setBackground(getBackground());
            GenericLabel valueLabel = new GenericLabel(shortValue);
            valueLabel.setToolTipText(value);
            valueLabel.setFont(Styles.FONT_11PT);
            valueLabel.setBackground(getBackground());

            gbc.gridwidth = GridBagConstraints.RELATIVE;
            newPanel.add(nameLabel, gbc);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            newPanel.add(valueLabel, gbc);
        }

        // add padding for the bottom
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weightx = 10;
        gbc.weighty = 10;
        JPanel padding = new JPanel();
        padding.setBackground(getBackground());
        newPanel.add(padding, gbc);

        remove(panel);
        panel = newPanel;
        add(newPanel, BorderLayout.CENTER);
        monitor.repaint();
    }
}
