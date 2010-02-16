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
package org.tranche.gui.user;

import org.tranche.gui.*;
import org.tranche.gui.util.GUIUtil;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.cert.CertificateExpiredException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import javax.swing.ListSelectionModel;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelListener;
import org.tranche.users.UserZipFile;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GenericUsersPanel extends JPanel {

    private UsersMenuBar usersMenuBar = new UsersMenuBar();
    private UsersPopupMenu usersPopupMenu = new UsersPopupMenu();
    public UsersTableModel usersModel;
    private GenericTable usersTable;
    public ErrorFrame ef = new ErrorFrame();

    public GenericUsersPanel() {
        usersModel = new UsersTableModel(new String[]{"User Name", "File Location"});
        usersTable = new GenericTable(usersModel, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // add the popup menu
        usersTable.addMouseListener(new GenericPopupListener(usersPopupMenu));

        // set the layout
        setLayout(new BorderLayout());

        // add the menu bar
        add(usersMenuBar, BorderLayout.NORTH);

        // add the table
        GenericScrollPane pane = new GenericScrollPane(usersTable);
        pane.setBorder(Styles.BORDER_NONE);
        pane.setVerticalScrollBar(new GenericScrollBar());
        add(pane, BorderLayout.CENTER);
    }

    public void addUser(UserZipFile uzf) {
        usersModel.addRow(new UserSummary(uzf));
    }

    public Set<UserZipFile> getUsers() {
        return usersModel.getUsers();
    }

    public void addTableModelListener(TableModelListener listener) {
        usersModel.addTableModelListener(listener);
    }

    // menu bar for this panel
    class UsersMenuBar extends JMenuBar {
        // create the menus

        private JMenu usersMenu = new JMenu("Users");
        private JMenu selectionMenu = new JMenu("Selection");
        private JMenu sortByMenu = new JMenu("Sort By");
        // create the menu items
        private JMenuItem addMenuItem = new JMenuItem("Add");
        private JMenuItem removeMenuItem = new JMenuItem("Remove");
        private JMenuItem userNameMenuItem = new JMenuItem("User Name");
        private JMenuItem fileLocationMenuItem = new JMenuItem("File Location");

        public UsersMenuBar() {
            // add the listeners
            addMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final JFileChooser jfc = GUIUtil.makeNewFileChooser();
                    Thread t = new Thread() {

                        public void run() {
                            try {
                                // add the row to the table
                                usersModel.addRow(new UserSummary(GUIUtil.promptForUserFile(jfc, GenericUsersPanel.this.getParent())));
                            } catch (CertificateExpiredException cee) {
                                GenericOptionPane.showMessageDialog(GenericUsersPanel.this.getParent(), "The user file you are using is expired. Please obtain a new one.", "User File Expired", JOptionPane.WARNING_MESSAGE);
                            } catch (Exception e) {
                                if (e.getMessage().contains("encoding") || e.getMessage().contains("passphrase")) {
                                    GenericOptionPane.showMessageDialog(GenericUsersPanel.this.getParent(), "Invalid password. Please try again.", "Invalid Password", JOptionPane.WARNING_MESSAGE);
                                } else {
                                    ef.show(e, GenericUsersPanel.this.getParent());
                                }
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            removeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        public void run() {
                            int[] rows = usersTable.getSelectedRows();
                            for (int index = rows.length - 1; index >= 0; index--) {
                                usersModel.removeRow(rows[index]);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            userNameMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        public void run() {
                            usersModel.sort(usersTable.getColumnModel().getColumnIndex("User Name"));
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            fileLocationMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        public void run() {
                            usersModel.sort(usersTable.getColumnModel().getColumnIndex("File Name"));
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            selectionMenu.addMenuListener(new MenuListener() {

                public void menuSelected(MenuEvent e) {
                    if (usersTable.getSelectedRowCount() > 0) {
                        removeMenuItem.setEnabled(true);
                    } else {
                        removeMenuItem.setEnabled(false);
                    }
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuCanceled(MenuEvent e) {
                }
            });

            // set the mnemonics
            addMenuItem.setMnemonic('a');
            removeMenuItem.setMnemonic('r');
            userNameMenuItem.setMnemonic('u');
            fileLocationMenuItem.setMnemonic('f');

            // add the server menu items
            usersMenu.add(addMenuItem);

            // add the selection menu items
            selectionMenu.add(removeMenuItem);

            // add the sort by menu items
            sortByMenu.add(userNameMenuItem);
            sortByMenu.add(fileLocationMenuItem);

            // set Mnemonics
            usersMenu.setMnemonic('u');
            selectionMenu.setMnemonic('s');
            sortByMenu.setMnemonic('o');

            // add the menus
            add(usersMenu);
            add(selectionMenu);
            add(sortByMenu);
        }
    }

    // the table popup menu
    private class UsersPopupMenu extends JPopupMenu {

        // the sort by menu
        private JMenu sortByMenu = new JMenu("Sort By");
        // menu items
        private JMenuItem removeMenuItem = new JMenuItem("Remove");
        private JMenuItem userNameMenuItem = new JMenuItem("User Name");
        private JMenuItem fileLocationMenuItem = new JMenuItem("File Location");

        public UsersPopupMenu() {
            // add the listeners
            removeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        public void run() {
                            int[] rows = usersTable.getSelectedRows();
                            for (int index = rows.length - 1; index >= 0; index--) {
                                usersModel.removeRow(rows[index]);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            userNameMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        public void run() {
                            usersModel.sort(usersTable.getColumnModel().getColumnIndex("User Name"));
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            fileLocationMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        public void run() {
                            usersModel.sort(usersTable.getColumnModel().getColumnIndex("File Name"));
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });


            add(removeMenuItem);
            addSeparator();
            sortByMenu.add(userNameMenuItem);
            sortByMenu.add(fileLocationMenuItem);
            add(sortByMenu);

            addPopupMenuListener(new PopupMenuListener() {

                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    if (usersTable.getSelectedRowCount() > 0) {
                        removeMenuItem.setEnabled(true);
                    } else {
                        removeMenuItem.setEnabled(false);
                    }
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }
            });
        }
    }

    private class UsersTableModel extends SortableTableModel {

        private ArrayList<UserSummary> allUsers = new ArrayList<UserSummary>();
        private ArrayList<UserSummary> filteredUsers = new ArrayList<UserSummary>();
        private String[] headers = new String[0];
        private Pattern filter = Pattern.compile("");

        private UsersTableModel(String[] headers) {
            this.headers = headers;
        }

        public void addRow(UserSummary us) {
            allUsers.add(us);
            filteredUsers.add(us);
            sort(usersTable.getPressedColumn());
            fireTableDataChanged();
        }

        public void clear() {
            allUsers.clear();
            filteredUsers.clear();
            fireTableDataChanged();
        }

        private boolean filter(UserSummary us) {
            if (us != null && (this.filter.matcher(us.userZipFile.getUserNameFromCert().toLowerCase()).find() || this.filter.matcher(us.location.toString().toLowerCase()).find())) {
                return true;
            } else {
                return false;
            }
        }

        public int getColumnCount() {
            return headers.length;
        }

        public Class getColumnClass(int c) {
            try {
                return getValueAt(0, c).getClass();
            } catch (Exception e) {
                return String.class;
            }
        }

        public String getColumnName(int column) {
            if (column < getColumnCount()) {
                return headers[column];
            } else {
                return "";
            }
        }

        public UserSummary getRow(int filteredIndex) {
            return filteredUsers.get(filteredIndex);
        }

        public int getRowCount() {
            return filteredUsers.size();
        }

        public File getLocation(int filteredIndex) {
            return filteredUsers.get(filteredIndex).location;
        }

        public Set<UserZipFile> getUsers() {
            Set<UserZipFile> users = new HashSet<UserZipFile>();
            users.addAll(users);
            return Collections.unmodifiableSet(users);
        }

        public UserZipFile getUserZipFile(int filteredIndex) {
            return filteredUsers.get(filteredIndex).userZipFile;
        }

        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return filteredUsers.get(row).userZipFile.getUserNameFromCert();
                case 1:
                    return filteredUsers.get(row).location.toString();
                default:
                    return null;
            }
        }

        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public void removeRow(int filteredIndex) {
            UserSummary us = filteredUsers.get(filteredIndex);
            allUsers.remove(us);
            filteredUsers.remove(filteredIndex);
            fireTableRowsDeleted(filteredIndex, filteredIndex);
        }

        public void setFilter(String filter) {
            this.filter = Pattern.compile(filter.toLowerCase());

            filteredUsers.clear();

            for (UserSummary us : allUsers) {
                if (filter(us)) {
                    filteredUsers.add(us);
                }
            }

            fireTableDataChanged();
        }

        public void sort(int column) {
            usersTable.setPressedColumn(column);
            Collections.sort(allUsers, new USComparator(column));
            Collections.sort(filteredUsers, new USComparator(column));
        }

        private class USComparator implements Comparator {

            private int column;

            public USComparator(int column) {
                this.column = column;
            }

            public int compare(Object o1, Object o2) {
                if (usersTable.getDirection()) {
                    Object temp = o1;
                    o1 = o2;
                    o2 = temp;
                }

                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o1 == null) {
                    return 1;
                } else if (o1 instanceof UserSummary && o2 instanceof UserSummary) {
                    if (column == usersTable.getColumnModel().getColumnIndex("User Name")) {
                        return ((((UserSummary) o1).userZipFile).getUserNameFromCert().toLowerCase().compareTo((((UserSummary) o1).userZipFile).getUserNameFromCert().toLowerCase()));
                    } else if (column == usersTable.getColumnModel().getColumnIndex("File Name")) {
                        return (((UserSummary) o1).location.toString().toLowerCase().compareTo(((UserSummary) o1).location.toString().toLowerCase()));
                    } else {
                        return 0;
                    }
                } else {
                    return 1;
                }
            }
        }
    }

    private class UserSummary {

        public UserZipFile userZipFile;
        public File location;

        public UserSummary(UserZipFile userZipFile) {
            this.userZipFile = userZipFile;
            this.location = userZipFile.getFile();
        }
    }
}
