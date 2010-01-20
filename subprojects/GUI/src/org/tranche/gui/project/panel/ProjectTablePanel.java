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
package org.tranche.gui.project.panel;

import org.tranche.gui.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import org.tranche.hash.BigHash;
import org.tranche.project.ProjectFilePart;
import org.tranche.util.Text;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ProjectTablePanel extends ProjectViewPanel {

    private ProjectTableModel projectModel = new ProjectTableModel();
    private GenericTable projectTable;
    private PagePanel pagePanel = new PagePanel();
    private GenericPopupListener popupListener;

    public ProjectTablePanel() {
        setLayout(new BorderLayout());

        // create the table
        projectTable = new GenericTable(projectModel, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        projectTable.getColumnModel().getColumn(1).setMaxWidth(120);

        // mimic the table selection within the project file parts map
        projectTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        // wait for the rows to be selected
                        try {
                            wait(50);
                        } catch (Exception ee) {
                        }

                        // first deselect all old parts
                        for (ProjectFilePart part : ProjectTablePanel.this.getParts()) {
                            ProjectTablePanel.this.setSelected(part, Boolean.FALSE);
                        }
                        // set the projects as selected
                        for (int index : projectTable.getSelectedRows()) {
                            ProjectTablePanel.this.setSelected(projectModel.getRow(index), Boolean.TRUE);
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });

        // add the table within a scroll pane
        GenericScrollPane pane = new GenericScrollPane(projectTable);
        pane.setBorder(Styles.BORDER_NONE);
        pane.setBackground(Color.GRAY);
        pane.setVerticalScrollBar(new GenericScrollBar());
        pane.setVerticalScrollBarPolicy(GenericScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(pane, BorderLayout.CENTER);

        // add the popuplistener
        if (popupListener != null) {
            projectTable.addMouseListener(popupListener);
        }

        // add the page panel at the bottom
        add(pagePanel, BorderLayout.SOUTH);
    }

    public ProjectTablePanel(Set<ProjectFilePart> parts) {
        this();
        setParts(parts);
    }

    @Override
    public void setPopupMenu(JPopupMenu popupMenu) {
        super.setPopupMenu(popupMenu);
        popupListener = new GenericPopupListener(popupMenu);
        popupListener.setDisplayMethod(GenericPopupListener.RIGHT_OR_DOUBLE_CLICK);
        if (projectTable != null) {
            projectTable.addMouseListener(popupListener);
        }
    }

    @Override
    public void setParts(Set<ProjectFilePart> parts) {
        super.setParts(parts);
        if (parts != null) {
            projectModel.addRows(parts);
            pagePanel.updateComponents();
            pagePanel.setVisible(true);
            if (projectTable != null) {
                projectTable.addMouseListener(popupListener);
            }
        }
    }

    @Override
    public void setFilter(String filterText) {
        super.setFilter(filterText);
        projectModel.setFilter(filterText);
    }

    @Override
    public void clear() {
        super.clear();
        projectModel.clear();
        pagePanel.setVisible(false);
    }

    public void refresh() {
        validate();
        repaint();
    }

    @Override
    public void selectAll() {
        super.selectAll();
        projectTable.selectAll();
        refresh();
    }

    @Override
    public void deselectAll() {
        super.deselectAll();
        projectTable.clearSelection();
        refresh();
    }

    private class ProjectTableModel extends SortableTableModel {

        private ArrayList<ProjectFilePart> filteredFiles = new ArrayList<ProjectFilePart>();
        private ArrayList<ProjectFilePart> pageFiles = new ArrayList<ProjectFilePart>();
        private String[] headers = new String[]{"File Name", "Size"};

        private ProjectTableModel() {
        }

        public void addRows(Set<ProjectFilePart> set) {
            for (ProjectFilePart pfp : set) {
                if (ProjectTablePanel.this.filter(pfp)) {
                    if (!filteredFiles.contains(pfp)) {
                        filteredFiles.add(pfp);
                    }
                }
            }
            sort(projectTable.getPressedColumn());
            resetPage();
        }

        public void clear() {
            filteredFiles.clear();
            pageFiles.clear();
            pagePanel.updateComponents();
        }

        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public String getColumnName(int column) {
            if (column < getColumnCount()) {
                return headers[column];
            } else {
                return "";
            }
        }

        public BigHash getHash(int filteredIndex) {
            return pageFiles.get(filteredIndex).getHash();
        }

        public int getPartCount() {
            return ProjectTablePanel.this.getParts().size();
        }

        public int getFilteredPartCount() {
            return filteredFiles.size();
        }

        public ProjectFilePart getRow(int filteredIndex) {
            return pageFiles.get(filteredIndex);
        }

        public int getRowCount() {
            return pageFiles.size();
        }

        public long getSize(int filteredIndex) {
            return pageFiles.get(filteredIndex).getPaddingAdjustedLength();
        }

        public String getName(int filteredIndex) {
            return pageFiles.get(filteredIndex).getRelativeName();
        }

        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return pageFiles.get(row).getRelativeName();
                case 1:
                    return Text.getFormattedBytes(pageFiles.get(row).getPaddingAdjustedLength());
                default:
                    return null;
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        private void resetPage() {
            pagePanel.updateComponents();

            pageFiles.clear();

            for (int i = pagePanel.page * pagePanel.numPerPage; i < (pagePanel.page + 1) * pagePanel.numPerPage; i++) {
                if (filteredFiles.size() > i) {
                    pageFiles.add(filteredFiles.get(i));
                }
            }

            fireTableDataChanged();
        }

        public void setFilter(String filter) {
            filteredFiles.clear();

            if (ProjectTablePanel.this.getParts() != null) {
                for (ProjectFilePart pfp : ProjectTablePanel.this.getParts()) {
                    if (filter(pfp)) {
                        filteredFiles.add(pfp);
                    }
                }
                resetPage();
            }
        }

        public void sort(int column) {
            projectTable.setPressedColumn(column);
            Collections.sort(filteredFiles, new PFSComparator(column));
            resetPage();
        }

        private class PFSComparator implements Comparator {

            private int column;

            public PFSComparator(int column) {
                this.column = column;
            }

            public int compare(Object o1, Object o2) {
                if (projectTable.getDirection()) {
                    Object temp = o1;
                    o1 = o2;
                    o2 = temp;
                }

                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o1 == null) {
                    return 1;
                } else if (o1 instanceof ProjectFilePart && o2 instanceof ProjectFilePart) {
                    if (column == 0) {
                        return ((ProjectFilePart) o1).getRelativeName().toLowerCase().compareTo(((ProjectFilePart) o2).getRelativeName().toLowerCase());
                    } else if (column == 1) {
                        if (((ProjectFilePart) o1).getPaddingAdjustedLength() == ((ProjectFilePart) o2).getPaddingAdjustedLength()) {
                            return 0;
                        } else if (((ProjectFilePart) o1).getPaddingAdjustedLength() > ((ProjectFilePart) o2).getPaddingAdjustedLength()) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else {
                        return 0;
                    }
                } else {
                    return 1;
                }
            }
        }
    }

    private class PagePanel extends JPanel {
        // pertinent info

        private int page = 0;
        private int numPages = 0;
        private int numPerPage = 100;
        private JLabel numPagesLabel = new GenericLabel();
        private JTextField pageTextField = new GenericTextField();
        private JTextField perPageTextField = new GenericTextField();

        public PagePanel() {
            // set the page panel initially to invisible
            setVisible(false);

            // set the border
            setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));

            // set the layout
            GridBagLayout gbl = new GridBagLayout();
            GridBagConstraints gbc = new GridBagConstraints();
            setLayout(gbl);

            // add the number of pages label
            gbc.weightx = 1;
            gbc.insets = new Insets(3, 0, 3, 0);
            add(numPagesLabel, gbc);

            // create and add the back to start button
            gbc.weightx = 0;
            JButton startButton = new PagePanelButton("<<");
            startButton.setFont(Styles.FONT_10PT_BOLD);
            startButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    page = 0;
                    projectModel.resetPage();
                }
            });
            add(startButton, gbc);

            // create and add the back by one button
            JButton backButton = new PagePanelButton("<");
            backButton.setFont(Styles.FONT_10PT_BOLD);
            backButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (!pageTextField.getText().equals("1")) {
                        page--;
                        projectModel.resetPage();
                    }
                }
            });
            add(backButton, gbc);

            // create and add the page label
            JLabel pageLabel = new GenericLabel("  Page: ");
            add(pageLabel, gbc);

            // add the page text field
            gbc.insets = new Insets(3, 0, 3, 5);
            pageTextField.addKeyListener(new KeyListener() {

                public void keyTyped(KeyEvent e) {
                }

                public void keyPressed(KeyEvent e) {
                }

                public void keyReleased(KeyEvent e) {
                    if (!pageTextField.getText().equals("")) {
                        page = Integer.parseInt(pageTextField.getText()) - 1;
                        projectModel.resetPage();
                    }
                }
            });
            pageTextField.setColumns(3);
            pageTextField.setBorder(Styles.BORDER_NONE);
            pageTextField.setMinimumSize(new Dimension(20, perPageTextField.getHeight()));
            add(pageTextField, gbc);

            // create and add the go to next button
            gbc.insets = new Insets(3, 0, 3, 0);
            JButton nextButton = new PagePanelButton(">");
            nextButton.setFont(Styles.FONT_10PT_BOLD);
            nextButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (!pageTextField.getText().equals(String.valueOf(numPages))) {
                        page++;
                        projectModel.resetPage();
                    }
                }
            });
            add(nextButton, gbc);

            // create and add the go to end button
            JButton endButton = new PagePanelButton(">>");
            endButton.setFont(Styles.FONT_10PT_BOLD);
            endButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    page = numPages - 1;
                    projectModel.resetPage();
                }
            });
            add(endButton, gbc);

            // add the per page text field
            gbc.insets = new Insets(3, 30, 3, 0);
            perPageTextField.setColumns(3);
            perPageTextField.setMinimumSize(new Dimension(20, perPageTextField.getHeight()));
            perPageTextField.setBorder(Styles.BORDER_NONE);

            // reload the data every time a key is changed
            perPageTextField.addKeyListener(new KeyListener() {

                public void keyTyped(KeyEvent e) {
                }

                public void keyPressed(KeyEvent e) {
                }

                public void keyReleased(KeyEvent e) {
                    if (Integer.parseInt(perPageTextField.getText()) > 0) {
                        numPerPage = Integer.parseInt(perPageTextField.getText().trim());
                        projectModel.resetPage();
                    }
                }
            });
            add(perPageTextField, gbc);

            gbc.insets = new Insets(3, 5, 3, 10);
            JLabel perPageLabel = new GenericLabel(" per page ");
            add(perPageLabel, gbc);
        }

        private class PagePanelButton extends JButton {

            public PagePanelButton(String text) {
                super(text);
                setBackground(Styles.COLOR_BACKGROUND);
                setForeground(Styles.COLOR_MAROON);
                setFont(Styles.FONT_12PT_BOLD);
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                setFocusable(false);
            }
        }

        public void updateComponents() {
            // reset the number of pages
            numPages = (projectModel.getFilteredPartCount() / numPerPage) + 1;
            // if the page the user is on is more than the number of pages
            if (page >= numPages) {
                // reset the current page to the last
                page = numPages - 1;
            }
            // reset the number of pages label
            numPagesLabel.setText(numPages + " Pages");
            // reset the current page
            pageTextField.setText(String.valueOf(page + 1));
            // reset the per page text field
            perPageTextField.setText(String.valueOf(numPerPage));
        }
    }
}
