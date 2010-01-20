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

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
abstract public class SortableTableModel extends AbstractTableModel {

    public abstract void sort(int column);

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void repaintTable() {
        Thread t = new Thread() {

            @Override
            public void run() {
                fireTableDataChanged();
            }
        };
        t.setDaemon(true);
        SwingUtilities.invokeLater(t);
    }

    public void repaintRow(final int row) {
        if (row >= getRowCount()) {
            return;
        }
        Thread t = new Thread("Repaint Row " + row) {

            @Override
            public void run() {
                fireTableRowsUpdated(row, row);
            }
        };
        t.setDaemon(true);
        SwingUtilities.invokeLater(t);
    }

    public void repaintRowInserted(final int row) {
        if (row >= getRowCount()) {
            return;
        }
        Thread t = new Thread("Repaint Row Inserted " + row) {

            @Override
            public void run() {
                fireTableRowsInserted(row, row);
            }
        };
        t.setDaemon(true);
        SwingUtilities.invokeLater(t);
    }

    public void repaintRowDeleted(final int row) {
        Thread t = new Thread("Repaint Row Deleted " + row) {

            @Override
            public void run() {
                fireTableRowsDeleted(row, row);
            }
        };
        t.setDaemon(true);
        SwingUtilities.invokeLater(t);
    }
}
