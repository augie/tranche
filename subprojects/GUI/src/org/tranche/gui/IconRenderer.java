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
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * <p>Render icon in table. The default cell renderer prints obj.toString()</p>
 * <p>Only needs to be applied to columns that may have icons.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class IconRenderer extends DefaultTableCellRenderer {

    private ImageIcon icon;

    public IconRenderer(String location) {
        icon = new ImageIcon(getClass().getResource(location));
    }

    /**
     * 
     * @param table
     * @param value
     * @param isSelected
     * @param hasFocus
     * @param row
     * @param column
     * @return
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value == null) {
            return this;
        }
        if (value.equals(Boolean.TRUE)) {
            setIcon(icon);
        } else {
            setIcon(null);
        }
        setText("");
        return this;
    }
}