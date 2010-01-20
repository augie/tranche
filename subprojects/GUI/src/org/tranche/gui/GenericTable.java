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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GenericTable extends JTable {

    private TableHeaderRenderer headerRenderer = new TableHeaderRenderer();

    public GenericTable() {
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFocusable(false);
        setBorder(BorderFactory.createEmptyBorder());
        getTableHeader().setDefaultRenderer(headerRenderer);
        getTableHeader().setBackground(Color.GRAY);
        getColumnModel().addColumnModelListener(new TableColumnModelListener() {

            private int startMoved = -1;

            public void columnSelectionChanged(ListSelectionEvent event) {
            }

            public void columnMarginChanged(ChangeEvent event) {
            }

            public void columnMoved(final TableColumnModelEvent event) {
                if (startMoved == event.getToIndex()) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            headerRenderer.setPressedColumn(event.getToIndex());
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                    startMoved = -1;
                } else {
                    startMoved = event.getToIndex();
                }
            }

            public void columnRemoved(TableColumnModelEvent event) {
            }

            public void columnAdded(TableColumnModelEvent event) {
            }
        });
    }

    public GenericTable(SortableTableModel model, int mode) {
        this();
        setModel(model);
        setSelectionMode(mode);
    }

    public void setModel(final SortableTableModel model) {
        super.setModel(model);
        getTableHeader().addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            JTableHeader header = (JTableHeader) e.getSource();
                            int column = header.columnAtPoint(e.getPoint());
                            if (column == headerRenderer.pressedColumn) {
                                headerRenderer.reverseDirection();
                            }
                            headerRenderer.setPressedColumn(column);
                            model.sort(headerRenderer.pressedColumn);
                            model.repaintTable();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            }
        });
        // make sure to repaint when the model has changed
        model.addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                Thread t = new Thread("Repaint Table") {

                    @Override()
                    public void run() {
                        repaint();
                        Container c = GenericTable.this.getParent();
                        if (c != null) {
                            c.repaint();
                        }
                    }
                };
                t.setDaemon(true);
                SwingUtilities.invokeLater(t);
            }
        });
    }

    // renderer for each row with alternating row colors
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);

        try {
            if (isCellSelected(row, column) == false) {
                c.setBackground(colorForRow(row));
                c.setForeground(UIManager.getColor("Table.foreground"));
            } else {
                c.setBackground(UIManager.getColor("Table.selectionBackground"));
                c.setForeground(UIManager.getColor("Table.selectionForeground"));
            }
        } catch (NullPointerException npe) {
        }
        return c;
    }

    protected Color colorForRow(int row) {
        return (row % 2 == 0) ? getBackground() : Styles.COLOR_EXTRA_LIGHT_BLUE;
    }

    public boolean getDirection() {
        return headerRenderer.getDirection();
    }

    public TableHeaderRenderer getHeaderRenderer() {
        return headerRenderer;
    }

    public void setPressedColumn(int column) {
        headerRenderer.setPressedColumn(column);
        Thread t = new Thread("Repaint Table") {

            @Override
            public void run() {
                repaint();
                getTableHeader().repaint();
            }
        };
        t.setDaemon(true);
        SwingUtilities.invokeLater(t);
    }

    public int getPressedColumn() {
        return headerRenderer.getPressedColumn();
    }

    public boolean isSorted() {
        return getPressedColumn() >= 0;
    }
}
