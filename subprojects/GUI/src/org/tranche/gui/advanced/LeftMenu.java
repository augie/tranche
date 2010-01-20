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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class LeftMenu extends JToolBar {

    private static Border emptyPaddingT4B1 = BorderFactory.createEmptyBorder(4, 0, 1, 0),  emptyPaddingT4B0 = BorderFactory.createEmptyBorder(4, 0, 0, 0);

    public LeftMenu() {
        setFloatable(false);
        setAlignmentY(JToolBar.LEFT);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Styles.COLOR_EXTRA_LIGHT_GRAY);
        setBorderPainted(false);
    }

    public void addButton(JButton button) {
        if (getComponentCount() > 0) {
            button.setBorder(emptyPaddingT4B1);
        }
        add(button);
    }

    public void updateSelection() {
    }

    /**
     * Factory method return JButton for left menu bar
     */
    public static JButton createLeftMenuButton(String label) {
        JButton button = new JButton(label);
        button.setForeground(Styles.COLOR_MAROON);
        button.setBackground(Styles.COLOR_EXTRA_LIGHT_GRAY);
        button.setBorder(Styles.BORDER_NONE);
        button.setVerticalTextPosition(AbstractButton.CENTER);
        button.setHorizontalTextPosition(AbstractButton.LEADING);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(Styles.UNDERLINE_EXTRA_LIGHT_GRAY);
        button.addMouseListener(new LeftMenuListener(button));
        return button;
    }

    /**
     * Mouse listener for events in the left menu.
     */
    private static class LeftMenuListener implements MouseListener {

        private JButton button;

        public LeftMenuListener(JButton button) {
            this.button = button;
        }

        public void mouseReleased(MouseEvent e) {
            button.setForeground(Styles.COLOR_MAROON);
            button.setBorder(emptyPaddingT4B1);
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
            button.setForeground(Styles.COLOR_MAROON);
            button.setBorder(emptyPaddingT4B1);
        }

        public void mouseEntered(MouseEvent e) {
            if (button.isEnabled()) {
                button.setForeground(Styles.COLOR_DARK_GREEN);
                button.setBorder(BorderFactory.createCompoundBorder(emptyPaddingT4B0, Styles.UNDERLINE_DARK_GREEN));
            }
        }
    }
}
