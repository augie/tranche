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
import java.awt.Component;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class TableHeaderRenderer implements TableCellRenderer {

    public int pressedColumn = -1;
    public boolean direction = false;
    private Icon arrowUp,  arrowDown;

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        // get the icons
        try {
            arrowUp = new ImageIcon(ImageIO.read(TableHeaderRenderer.class.getResourceAsStream("/org/tranche/gui/image/arrowUp.gif")));
            arrowDown = new ImageIcon(ImageIO.read(TableHeaderRenderer.class.getResourceAsStream("/org/tranche/gui/image/arrowDown.gif")));
        } catch (Exception e) {
            // do nothing
        }

        // create the label
        JLabel b = new GenericLabel((value == null) ? "" : value.toString().toUpperCase(), null, JLabel.LEFT);
        b.setHorizontalTextPosition(JLabel.LEFT);
        b.setForeground(Color.WHITE);
        b.setFont(Styles.FONT_11PT);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.WHITE), BorderFactory.createEmptyBorder(3, 4, 3, 3)));
        b.setIconTextGap(2);

        if (column == pressedColumn) {
            if (direction) {
                b.setIcon(arrowDown);
            } else {
                b.setIcon(arrowUp);
            }
        }

        return b;
    }

    public boolean getDirection() {
        return direction;
    }

    public int getPressedColumn() {
        return pressedColumn;
    }

    public void setPressedColumn(int column) {
        pressedColumn = column;
    }

    public void reverseDirection() {
        direction = !direction;
    }
}
