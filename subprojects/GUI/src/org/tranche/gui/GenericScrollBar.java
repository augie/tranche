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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GenericScrollBar extends JScrollBar {

    public GenericScrollBar() {
        super();
        setUI(new GenericScrollBarUI());
    }

    public GenericScrollBar(int orientation) {
        super(orientation);
        setUI(new GenericScrollBarUI());
    }

    private class GenericScrollBarUI extends BasicScrollBarUI {

        // this draws scroller
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            g.setColor(Color.GRAY);
            g.fill3DRect((int) thumbBounds.getX(), (int) thumbBounds.getY(), (int) thumbBounds.getWidth(), (int) thumbBounds.getHeight(), true);
        }

        // this draws scroller background
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(Styles.COLOR_EXTRA_LIGHT_GRAY);
            g.fillRect((int) trackBounds.getX(), (int) trackBounds.getY(), (int) trackBounds.getWidth(), (int) trackBounds.getHeight());
            g.setColor(Color.GRAY);
            g.drawRect((int) trackBounds.getX(), (int) trackBounds.getY() - 1, (int) trackBounds.getWidth() - 1, (int) trackBounds.getHeight() + 1);
        }

        // and methods creating scrollbar buttons
        protected JButton createDecreaseButton(int orientation) {
            JButton button = super.createDecreaseButton(orientation);
            button.setBackground(Styles.COLOR_EXTRA_LIGHT_GRAY);
            button.setForeground(Color.WHITE);
            button.setBorder(Styles.BORDER_GRAY_1);
            return button;
        }

        protected JButton createIncreaseButton(int orientation) {
            JButton button = super.createIncreaseButton(orientation);
            button.setBackground(Styles.COLOR_EXTRA_LIGHT_GRAY);
            button.setForeground(Color.WHITE);
            button.setBorder(Styles.BORDER_GRAY_1);
            return button;
        }
    }
}
