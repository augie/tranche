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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class GenericRoundedButton extends JButton {

    private Set<JButton> syncedButtons;
    /**
     * If not enabled, will become grey
     */
    private Color foregroundColor = Color.BLACK;
    private static final Color disabledForegroundColor = new Color(0xAA, 0xAA, 0xAA, 0xAA);
    private static final Color enabledForegroundColor = Color.BLACK;

    public GenericRoundedButton(String title) {
        super(title);
        init();
    }

    public GenericRoundedButton() {
        super();
        init();
    }

    private void init() {
        syncedButtons = new HashSet();

        setBackground(Styles.COLOR_BACKGROUND_LIGHT);
        // give it some inner margin
        setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
        setFocusable(false);
    }

    public void setText(String text) {

        super.setText(text);

        // Find the longest string amoung synced buttons.
        // Button will pad its text accordingly
        int maxSyncedStringLength = getText().length();

        // Must check if null -- otherwise super constructor NullPointerException
        if (this.syncedButtons != null) {
            for (JButton next : this.syncedButtons) {
                int nextSize = next.getText().length();

                // Update max size if appropriate
                if (nextSize > maxSyncedStringLength) {
                    maxSyncedStringLength = nextSize;
                }
            }
        }

        // Padding is the num. of spaces to add to text
        int padding = maxSyncedStringLength - text.length();

        // Toggle spaces right, then left
        boolean isRight = true;

        // Will only add padding if text needs padding (> 0)
        for (int i = 0; i < padding; i++) {
            if (isRight) {
                text = text + " ";
            } else {
                text = " " + text;
            }

            isRight = !isRight;
        }
    }

    public void paint(Graphics g) {
        // Paint JButton parent to be color of background (transparent)
//        setBackground(getParent().getBackground());
//        setBorder(Styles.BORDER_NONE);
        this.setContentAreaFilled(false);
        this.setBorderPainted(false);

        Graphics2D g2d = (Graphics2D) g;

        // Anti-aliased
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        super.paint(g);

        // Make it grey #DDDDDD, and make it round with 1px black border
        g2d.setColor(new Color(0xDD, 0xDD, 0xDD, 0xFF));
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
        g2d.setColor(foregroundColor);
        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);

        // Determine the label size so can center it
        FontRenderContext frc = new FontRenderContext(null, false, false);
        Rectangle2D r = getFont().getStringBounds(getText(), frc);

        float xMargin = (float) (getWidth() - r.getWidth()) / 2;
        float yMargin = (float) (getHeight() - getFont().getSize()) / 2;

        // Draw the text in the center
        g2d.drawString(getText(), xMargin, (float) getFont().getSize() + yMargin);
    }

    /**
     * If want buttons to keep similar sizes, just sync them both. Rough estimate -- not exact.
     */
    public void syncTextSize(JButton button) {
        this.syncedButtons.add(button);
    }

    @Override()
    public void setEnabled(boolean isEnabled) {
        if (!isEnabled) {
            this.foregroundColor = disabledForegroundColor;
        } else {
            this.foregroundColor = enabledForegroundColor;
        }

        Thread t = new Thread("Put description here") {

            @Override()
            public void run() {
                // Put logic here
                GenericRoundedButton.this.repaint();
            }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        SwingUtilities.invokeLater(t);

        super.setEnabled(isEnabled);
    }
}
