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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JFrame;

import org.tranche.gui.util.GUIUtil;

/**
 * A generic popup frame.
 * @author Bryan Smith <bryanesmith at gmail.com>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GenericPopupFrame extends GenericFrame {

    private Component component;

    /**
     * Takes in a title and panel, and constructs the popup. Automatically generates the masthead.
     * @param title The title for the popup frame
     * @param panel Any JPanel for the body of the popup
     */
    public GenericPopupFrame(String title, Component panel) {
        this(title, panel, true);
    }

    /**
     * 
     * @param title
     * @param panel
     * @param useScrollPane
     */
    public GenericPopupFrame(String title, Component panel, boolean useScrollPane) {
        super(title);

        setBackground(Styles.COLOR_BACKGROUND);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Add masthead
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridx = 0; // <-- Cell
        gbc.gridy = 0; // <-- Row
        add(new GenericMasthead(title), gbc);

        // Add panel
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridx = 0; // <-- Cell
        gbc.gridy = 1; // <-- Row

        component = panel;
        if (useScrollPane) {
            add(new GenericScrollPane(panel), gbc);
        } else {
            add(panel, gbc);
        }
    }

    /**
     * <p>Convenient method that sets size and dimensions. Keeps caller code clean.</p>
     * @param dimension A valid dimension object.
     */
    public void setSizeAndCenter(Dimension dimension) {
        setMinimumSize(dimension);
        setPreferredSize(dimension);
        setSize(dimension);
        center();
    }

    /**
     * Center an item once it is packed. If you don't want to pack(), but rather would like to set the Dimension, use setSizeAndCenter.
     */
    public void center() {
        GUIUtil.centerOnScreen(this);
    }

    /**
     * 
     * @return
     */
    public Component getComponent() {
        return component;
    }
}