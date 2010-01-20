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
package org.tranche.gui.project;

import org.tranche.project.ProjectSummary;
import org.tranche.gui.util.GUIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.GenericTextField;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GenericButton;
import org.tranche.gui.GenericFileFilter;
import org.tranche.gui.GenericFrame;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericMenu;
import org.tranche.gui.GenericMenuBar;
import org.tranche.gui.GenericMenuItem;
import org.tranche.gui.GenericPopupListener;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.GenericTable;
import org.tranche.gui.PassphrasePool;
import org.tranche.gui.PassphrasePoolListener;
import org.tranche.gui.PassphraseFrame;
import org.tranche.gui.SortableTableModel;
import org.tranche.gui.Styles;
import org.tranche.hash.BigHash;
import org.tranche.security.SecurityUtil;
import org.tranche.util.IOUtil;

/**
 * Frame for setting encrypted projects and associated passphrases.
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith <bryanesmith at gmail dot com>
 */
public class EncryptedProjectsFrame extends GenericFrame implements WindowListener {

    private EncryptedProjectsMenuBar epmb = new EncryptedProjectsMenuBar();
    private EncrypedProjectsPopupMenu eppm = new EncrypedProjectsPopupMenu();
    private GenericTable encProjectsTable;
    private EncryptedProjectsTableModel encProjectsModel = new EncryptedProjectsTableModel();
    // Callbacks used to return information about encrypted projects when frame closes.
    private List<EncryptedProjectsCallback> callbacks = new ArrayList();
    // error frame
    private ErrorFrame ef = new ErrorFrame();

    public EncryptedProjectsFrame() {
        super("Passphrase Manager");

        // set it to dispose of only this window on close
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(350, 350);
        setLayout(new BorderLayout());

        // add the menu bar
        add(epmb, BorderLayout.NORTH);

        // set up the table
        encProjectsTable = new GenericTable(encProjectsModel, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        encProjectsTable.addMouseListener(new GenericPopupListener(eppm));
        GenericScrollPane scrollPane = new GenericScrollPane(encProjectsTable);
        scrollPane.setBorder(Styles.BORDER_NONE);
        scrollPane.setBackground(Color.GRAY);
        add(scrollPane, BorderLayout.CENTER);

        // window
        addWindowListener(this);
    }

    private void removeSelected() {
        if (encProjectsTable.getSelectedRowCount() == 0) {
            return;
        }
        int[] rows = encProjectsTable.getSelectedRows();
        for (int row : rows) {
            PassphrasePool.remove(encProjectsModel.getHash(row));
        }
    }

    /**
     * <p>Returns all encrypted projects with associated passphrases.</p>
     * @return A map including all hashes with associated passphrases.
     */
    public Map<BigHash, String> getEncryptedInformation() {
        return PassphrasePool.getAll();
    }

    private class EncryptedProjectsMenuBar extends GenericMenuBar {

        private JMenu encryptedProjectsMenu = new GenericMenu("Data Sets");
        private JMenu selectionMenu = new GenericMenu("Selection");
        // create the menu items
        private JMenuItem loadListMenuItem = new GenericMenuItem("Load List");
        private JMenuItem saveListMenuItem = new GenericMenuItem("Save List");
        private JMenuItem addMenuItem = new GenericMenuItem("Add");
        private JMenuItem removeMenuItem = new GenericMenuItem("Remove");

        public EncryptedProjectsMenuBar() {
            // initialize the abilities
            removeMenuItem.setEnabled(false);

            // add the listeners
            loadListMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final JFileChooser jfc = GUIUtil.makeNewFileChooser();
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            // query for a file
                            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                            jfc.setFileFilter(new GenericFileFilter(".txt.encrypted", "Encrypted Text (*.txt.encrypted)"));
                            
                            jfc.showOpenDialog(EncryptedProjectsFrame.this);

                            // check for a submitted file
                            File selectedFile = jfc.getSelectedFile();
                            if (selectedFile == null) {
                                return;
                            }

                            // ask for the passphrase
                            PassphraseFrame pf = new PassphraseFrame();
                            pf.setDescription("This list is encrypted. What is the password?");
                            pf.setLocationRelativeTo(EncryptedProjectsFrame.this);
                            pf.setVisible(true);
                            String pass = pf.getPassphrase();
                            if (pass == null || pass.equals("")) {
                                return;
                            }

                            FileReader fr = null;
                            BufferedReader br = null;
                            try {
                                File decryptedFile = SecurityUtil.decrypt(pass, selectedFile);
                                fr = new FileReader(decryptedFile);
                                br = new BufferedReader(fr);

                                // Avoid null pointer exceptions if file is empty
                                while (br.ready()) {
                                    try {
                                        // throwing away name
                                        String name = br.readLine().trim();
                                        String hash = br.readLine().trim();
                                        String passphrase = br.readLine().trim();

                                        // add this to the model
                                        PassphrasePool.set(BigHash.createHashFromString(hash), passphrase);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (Exception ee) {
                                ef.show(ee, EncryptedProjectsFrame.this);
                            } finally {
                                IOUtil.safeClose(br);
                                IOUtil.safeClose(fr);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            saveListMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final JFileChooser jfc = GUIUtil.makeNewFileChooser();
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            try {
                                jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                                jfc.setFileFilter(new GenericFileFilter(".txt.encrypted", "Encrypted Text (*.txt.encrypted)"));

                                int action = jfc.showSaveDialog(EncryptedProjectsFrame.this);

                                File selectedFile = null;
                                // If user accepts, get file. Else, show message and throw exception
                                if (action == JFileChooser.APPROVE_OPTION) {
                                    selectedFile = jfc.getSelectedFile();
                                } else {
                                    return;
                                }


                                // ask for the passphrase
                                PassphraseFrame pf = new PassphraseFrame();
                                pf.setDescription("This list will be encrypted. What should the password be?");
                                pf.setLocationRelativeTo(EncryptedProjectsFrame.this);
                                pf.setVisible(true);
                                String pass = pf.getPassphrase();
                                if (pass == null || pass.equals("")) {
                                    return;
                                }

                                // make sure the file extension is .txt.encrypted
                                String filePath = selectedFile.getCanonicalPath();
                                if (!filePath.substring(filePath.length() - 14, filePath.length()).equals(".txt.encrypted")) {
                                    filePath = filePath + ".txt.encrypted";
                                    selectedFile = new File(filePath);
                                }

                                OutputStream os = null;
                                FileOutputStream fos = null;
                                try {
                                    // determine the file contents
                                    String output = "";
                                    for (BigHash hash : PassphrasePool.getAll().keySet()) {
                                        output = output + "\n" + hash.toString() + "\n" + PassphrasePool.get(hash) + "\n";
                                    }

                                    System.out.println("----");
                                    System.out.println(output);
                                    System.out.println("----");

                                    // output the file
                                    fos = new FileOutputStream(selectedFile);
                                    os = new PrintStream(fos);
                                    os.write(SecurityUtil.encrypt(pass, output.getBytes()));

                                    // notify the user
                                    GenericOptionPane.showMessageDialog(EncryptedProjectsFrame.this, "List saved.", "Success", JOptionPane.INFORMATION_MESSAGE);
                                } catch (Exception ee) {
                                    throw new Exception("The list could not be saved.");
                                } finally {
                                    IOUtil.safeClose(fos);
                                    IOUtil.safeClose(os);
                                }
                            } catch (Exception ee) {
                                ef.show(ee, EncryptedProjectsFrame.this);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            addMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    AddEncryptedProjectFrame aepf = new AddEncryptedProjectFrame();
                    aepf.setLocationRelativeTo(EncryptedProjectsFrame.this);
                    aepf.setVisible(true);
                }
            });
            removeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            removeSelected();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            selectionMenu.addMenuListener(new MenuListener() {

                public void menuSelected(MenuEvent e) {
                    if (encProjectsTable.getSelectedRowCount() > 0) {
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
            loadListMenuItem.setMnemonic('l');
            saveListMenuItem.setMnemonic('s');
            addMenuItem.setMnemonic('a');
            removeMenuItem.setMnemonic('r');

            // add the projects menu items
            encryptedProjectsMenu.add(loadListMenuItem);
            encryptedProjectsMenu.add(saveListMenuItem);
            encryptedProjectsMenu.add(addMenuItem);

            // add the project menu items
            selectionMenu.add(removeMenuItem);

            // set Mnemonics
            encryptedProjectsMenu.setMnemonic('p');
            selectionMenu.setMnemonic('s');

            // add the menus
            add(encryptedProjectsMenu);
            add(selectionMenu);
        }
    }

    // the table popup menu
    private class EncrypedProjectsPopupMenu extends JPopupMenu {

        // menu items
        public JMenuItem removeSelectedMenuItem = new JMenuItem("Remove");

        public EncrypedProjectsPopupMenu() {
            // add the item listeners
            removeSelectedMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            removeSelected();
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            add(removeSelectedMenuItem);

            addPopupMenuListener(new PopupMenuListener() {

                public void popupMenuCanceled(PopupMenuEvent e) {
                }

                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    if (encProjectsTable.getSelectedRowCount() > 0) {
                        eppm.removeSelectedMenuItem.setEnabled(true);
                    } else {
                        eppm.removeSelectedMenuItem.setEnabled(false);
                    }
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }
            });
        }
    }

    private class AddEncryptedProjectFrame extends GenericFrame {

        private JTextField hashTextField = new GenericTextField();
        private JPasswordField passphraseTextField = new JPasswordField(), passphraseAgainTextField = new JPasswordField();

        public AddEncryptedProjectFrame() {
            super("Add Encrypted Data Set");

            // set a fixed preferred size
            setSize(new Dimension(300, 150));

            // set the default background
            setBackground(Styles.COLOR_BACKGROUND);

            // create and set the layout
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            // create and add the hash label
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = GridBagConstraints.RELATIVE;
            gbc.gridy = 1;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            gbc.weightx = 0;
            gbc.insets = new Insets(10, 5, 0, 5);
            JLabel hashLabel = new GenericLabel("Hash:");
            add(hashLabel, gbc);

            // create and add the hash text field
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.insets = new Insets(10, 0, 0, 5);
            hashTextField.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    executeAdd();
                }
            });
            add(hashTextField, gbc);

            // create and add the hash label
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.gridx = GridBagConstraints.RELATIVE;
            gbc.gridy = 2;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            gbc.weightx = 0;
            gbc.insets = new Insets(10, 5, 0, 5);
            JLabel passphraseLabel = new GenericLabel("Passphrase:");
            add(passphraseLabel, gbc);

            // create and add the hash text field
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.insets = new Insets(10, 0, 0, 5);
            passphraseTextField.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    executeAdd();
                }
            });
            add(passphraseTextField, gbc);

            // create and add the hash label
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.gridx = GridBagConstraints.RELATIVE;
            gbc.gridy = 3;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            gbc.weightx = 0;
            gbc.insets = new Insets(10, 5, 0, 5);
            JLabel passphraseAgainLabel = new GenericLabel("Passphrase Again:");
            add(passphraseAgainLabel, gbc);

            // create and add the hash text field
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.insets = new Insets(10, 0, 0, 5);
            passphraseAgainTextField.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    executeAdd();
                }
            });
            add(passphraseAgainTextField, gbc);

            // create and add the open project button
            gbc.anchor = GridBagConstraints.SOUTH;
            gbc.gridy = 4;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.gridheight = GridBagConstraints.REMAINDER;
            gbc.weighty = 1;
            gbc.insets = new Insets(5, 0, 0, 0);
            JButton addButton = new GenericButton("Add Encrypted Data Set");
            addButton.setBorder(Styles.BORDER_BUTTON_TOP);
            addButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    executeAdd();
                }
            });
            add(addButton, gbc);
        }

        private void executeAdd() {
            try {
                BigHash hash = BigHash.createHashFromString(hashTextField.getText().trim());

                // make sure the passphrases match
                if (!passphraseTextField.getText().equals(passphraseAgainTextField.getText())) {
                    throw new Exception("Your two given passphrases do not match.");
                }
                String passphrase = new String(passphraseTextField.getPassword()).trim();

                // add to the passphrase cache
                PassphrasePool.set(hash, passphrase);

                // close the window
                dispose();
            } catch (Exception e) {
                ef.show(e, this);
            }
        }
    }

    // Window events
    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        // For each listener, set encrypted project information
        for (EncryptedProjectsCallback nextCallback : callbacks) {
            nextCallback.returnHashInformation(getEncryptedInformation());
        }
        setVisible(false);
    }

    private class EncryptedProjectsTableModel extends SortableTableModel {

        private final String[] headers = new String[]{"Hash", "Passphrase"};
        private final List<BigHash> filteredList = new ArrayList<BigHash>();

        public EncryptedProjectsTableModel() {
            PassphrasePool.addListener(new PassphrasePoolListener() {

                public void passphraseAdded(BigHash hash) {
                    addRow(hash);
                }

                public void passphraseUpdated(BigHash hash) {
                    int row = getRowOf(hash);
                    if (row >= 0) {
                        resort();
                        repaintRow(row);
                    } else {
                        addRow(hash);
                    }
                }

                public void passphraseRemoved(BigHash hash) {
                    removeRow(hash);
                }
            });
            ProjectPool.addListener(new ProjectPoolListener() {

                public void projectAdded(ProjectPoolEvent ppe) {
                    projectUpdated(ppe);
                }

                public void projectUpdated(ProjectPoolEvent ppe) {
                    int row = getRowOf(ppe.getProjectSummary().hash);
                    if (row >= 0) {
                        resort();
                        repaintRow(row);
                    }
                }

                public void projectRemoved(ProjectPoolEvent ppe) {
                    if (getRowOf(ppe.getProjectSummary().hash) >= 0) {
                        PassphrasePool.remove(ppe.getProjectSummary().hash);
                    }
                }
            });
        }

        private void addRow(BigHash hash) {
            synchronized (filteredList) {
                filteredList.add(hash);
            }
            // sort the table
            resort();
            // get the row of the added project
            int row = getRowOf(hash);
            if (row >= 0) {
                repaintRowInserted(row);
            }
        }

        private void removeRow(BigHash hash) {
            int row = getRowOf(hash);
            synchronized (filteredList) {
                filteredList.remove(hash);
            }
            if (row >= 0) {
                repaintRowDeleted(row);
            }
        }

        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public String getColumnName(int column) {
            if (column < getColumnCount()) {
                return headers[column];
            } else {
                return null;
            }
        }

        public BigHash getHash(int row) {
            return filteredList.get(row);
        }

        public int getRowOf(BigHash hash) {
            return filteredList.indexOf(hash);
        }

        public int getRowCount() {
            return PassphrasePool.size();
        }

        public Object getValueAt(int row, int column) {
            ProjectSummary ps = ProjectPool.get(filteredList.get(row)).get(0);
            Object returnVal = null;
            if (getColumnName(column).equals("Hash")) {
                if (ps != null) {
                    returnVal = ps.title;
                } else {
                    returnVal = "";
                }
            } else if (getColumnName(column).equals("Passphrase")) {
                returnVal = "**********";
            }
            return returnVal;
        }

        public void resort() {
            sort(encProjectsTable.getPressedColumn());
        }

        public void sort(int column) {
            if (column < 0 || column > getColumnCount()) {
                return;
            }
            encProjectsTable.setPressedColumn(column);
            Collections.sort(filteredList, new EPComparator(column));
        }

        private class EPComparator implements Comparator {

            private int column;

            public EPComparator(int column) {
                this.column = column;
            }

            public int compare(Object o1, Object o2) {
                if (encProjectsTable.getDirection()) {
                    Object temp = o1;
                    o1 = o2;
                    o2 = temp;
                }

                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o1 == null) {
                    return 1;
                } else {
                    ProjectSummary ps1 = ProjectPool.get((BigHash) o1).get(0);
                    ProjectSummary ps2 = ProjectPool.get((BigHash) o2).get(0);
                    if (getColumnName(column).equals("Hash")) {
                        return ps1.title.compareTo(ps2.title);
                    } else {
                        return 0;
                    }
                }
            }
        }
    }
}

interface EncryptedProjectsCallback {

    /**
     * <p>Sets all information about hashes and associated passwords.</p>
     */
    public void returnHashInformation(Map<BigHash, String> encryptedInfo);
}
