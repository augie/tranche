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
package org.tranche.gui.meta;

import org.tranche.gui.util.GUIUtil;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.meta.MetaDataUtil;
import org.tranche.users.UserZipFile;
import org.tranche.FileEncoding;
import org.tranche.exceptions.TodoException;
import org.tranche.gui.ConfigureTrancheGUI;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GenericButton;
import org.tranche.gui.GenericCheckBox;
import org.tranche.gui.GenericFrame;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericScrollBar;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.GenericTextField;
import org.tranche.gui.IndeterminateProgressBar;
import org.tranche.gui.user.SignInUserButton;
import org.tranche.gui.Styles;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class MetaDataEditFrame extends GenericFrame {

    private BigHash metaDataHash;
    private MetaData metaData;
    private final Set<String> servers = new HashSet<String>();
    private final SignInUserButton userPanel = new SignInUserButton();
    private final JTextField fileName = new GenericTextField();
    private final JTextField mimeType = new GenericTextField();
    // the list of all known relevant MIME types
    private final String[] possibleMIMEType = {
        "",
        "application/javascript",
        "application/msword",
        "application/pdf",
        "application/rtf",
        "application/vnd.ms-excel",
        "application/vnd.ms-powerpoint",
        "application/vnd.ms-project",
        "application/vnd.ms-works",
        "application/xhtml+xml",
        "application/xml",
        "application/x-baf",
        "application/x-bic",
        "application/x-bzip2",
        "application/x-dta",
        "application/x-fid",
        "application/x-gtar",
        "application/x-gzip",
        "application/x-java-vm",
        "application/x-java-archive",
        "application/x-lzma",
        "application/x-mgf",
        "application/x-msp",
        "application/x-mzData",
        "application/x-mzXML",
        "application/x-pkl",
        "application/x-raw",
        "application/x-t2d",
        "application/x-tar",
        "application/x-text+mgf",
        "application/x-wiff",
        "application/x-yep",
        "application/zip",
        "audio/x-wav",
        "audio/x-midi",
        "image/bmp",
        "image/gif",
        "image/jpeg",
        "image/png",
        "image/tiff",
        "image/tiff-fx",
        "text/css",
        "text/csv",
        "text/directory",
        "text/enriched",
        "text/html",
        "text/plain",
        "text/richtext",
        "text/rtf",
        "text/xml",
        "text/xml-external-parsed-entity",
        "text/x-amethyst-xml",
        "text/x-jasper-xml",
        "text/x-opal-xml",
        "text/x-tandem-xml",
        "text/x-thegpm-xml",
        "video/mpeg",
        "video/mpeg4-generic",
        "video/quicktime",
        "video/raw",
        "video/x-msvideo"
    };
    private JComboBox mimeTypeComboBox = new JComboBox(possibleMIMEType);
    private final JComboBox expirationM = new JComboBox();
    private final JComboBox expirationD = new JComboBox();
    private final JComboBox expirationY = new JComboBox();
    private final GenericCheckBox isHiddenCheckBox = new GenericCheckBox("Hidden");
    private DefaultTableModel annotationDM = new DefaultTableModel(new Object[]{"Name", "Value"}, 0);
    private JTable annotationTable = new JTable();
    private JMenuBar annotationMenuBar = new JMenuBar(),  signatureMenuBar = new JMenuBar();
    private JButton saveChangesButton = new GenericButton("Save Changes");
    private static boolean hasShownStickyServerWarning = false;
    public ErrorFrame ef = new ErrorFrame();
    private final DefaultTableModel encodingDM = new DefaultTableModel(new Object[]{"Encoding", "Hash"}, 0) {

        @Override()
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel signatureDM = new DefaultTableModel(new Object[]{"Name"}, 0) {

        @Override()
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private DefaultTableModel partDM = new DefaultTableModel(new Object[]{"Hash"}, 0) {

        @Override()
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    /**
     * <p>Used to show warning message once.</p>
     */
    private boolean hasShownPermissionsWarning = false;
    /**
     * <p>Used to show warning message once.</p>
     */
    private boolean hasShownSaveWarning = false;

    public MetaDataEditFrame() {
        super("Meta-Data Viewer/Editor");

        // Until check meta data and user, disable
        setComponentsEnabled(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(new Dimension(600, 300));
        setBackground(Styles.COLOR_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // add a title
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 10, 5, 3);
        JLabel title = new GenericLabel("Meta-Data Viewer/Editor");
        title.setFont(Styles.FONT_TITLE);
        add(title, gbc);

        // user file
        gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        userPanel.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent e) {
                setEnabledBasedOnUser();
            }
        });
        add(userPanel, gbc);

        // create the tabbed pane
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTabbedPane jtp = new JTabbedPane();
        jtp.setFocusable(false);

        // create the pane
        {
            // create the panel
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            GridBagConstraints gbc2 = new GridBagConstraints();

            // add the name label
            gbc2.anchor = GridBagConstraints.WEST;
            gbc2.weightx = 0;
            gbc2.gridwidth = 1;
            gbc2.insets = new Insets(10, 10, 4, 5);
            JLabel nameLabel = new GenericLabel("Name:");
            nameLabel.setToolTipText("The arbitrary name associated with this project.");
            nameLabel.setBackground(Styles.COLOR_BACKGROUND);
            panel.add(nameLabel, gbc2);

            // add the name text field
            gbc2.weightx = 1;
            gbc2.gridwidth = GridBagConstraints.REMAINDER;
            gbc2.fill = GridBagConstraints.HORIZONTAL;
            fileName.setBorder(Styles.BORDER_BLACK_1);
            fileName.setBackground(Styles.COLOR_BACKGROUND_LIGHT);
            fileName.setToolTipText("The arbitrary name associated with this project.");
            panel.add(fileName, gbc2);

            // add the mime type label
            gbc2.weightx = 0;
            gbc2.gridwidth = 1;
            gbc2.fill = GridBagConstraints.NONE;
            gbc2.anchor = GridBagConstraints.WEST;
            gbc2.insets = new Insets(4, 10, 4, 5);
            JLabel mimeTypeLabel = new GenericLabel("MIME Type:");
            mimeTypeLabel.setToolTipText("The MIME type of this project.");
            mimeTypeLabel.setBackground(Styles.COLOR_BACKGROUND);
            panel.add(mimeTypeLabel, gbc2);

            // add the mimeType text field
            gbc2.weightx = 1;
            gbc2.gridwidth = GridBagConstraints.RELATIVE;
            gbc2.fill = GridBagConstraints.BOTH;
            gbc2.insets = new Insets(6, 10, 6, 5);
            mimeType.setBorder(Styles.BORDER_BLACK_1);
            mimeType.setBackground(Styles.COLOR_BACKGROUND_LIGHT);
            mimeType.setToolTipText("The MIME type of this project.");
            mimeType.addKeyListener(new KeyAdapter() {

                @Override()
                public void keyReleased(KeyEvent e) {
                    Thread t = new Thread() {

                        @Override()
                        public void run() {
                            // commit all key types to the meta data
                            metaData.setMimeType(mimeType.getText());
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            panel.add(mimeType, gbc2);

            // mime type combo box
            gbc2.weightx = 0;
            gbc2.gridwidth = GridBagConstraints.REMAINDER;
            gbc2.insets = new Insets(4, 10, 4, 5);
            mimeTypeComboBox.setToolTipText("The MIME type of this project.");
            mimeTypeComboBox.addItemListener(
                    new ItemListener() {

                        public void itemStateChanged(ItemEvent e) {
                            // commit all changes when a selection is made
                            if (!e.getItem().toString().equals("Select")) {
                                mimeType.setText(e.getItem().toString());
                                metaData.setMimeType(e.getItem().toString());
                            } else {
                                mimeType.setText("");
                                metaData.setMimeType("");
                            }
                        }
                    });
            panel.add(mimeTypeComboBox, gbc2);

            // add the expiration label
            gbc2.weightx = 0;
            gbc2.gridwidth = 1;
            gbc2.fill = GridBagConstraints.NONE;
            JLabel expirationLabel = new GenericLabel("Expiration:");
            expirationLabel.setToolTipText("The expiration time of this project.");
            expirationLabel.setBackground(Styles.COLOR_BACKGROUND);
            panel.add(expirationLabel, gbc2);

            // the months, days, and years values for combo boxes
            expirationM.addItem("Month");
            for (int i = 1; i <= 12; i++) {
                expirationM.addItem(String.valueOf(i));
            }
            expirationD.addItem("Day");
            for (int i = 1; i <= 31; i++) {
                expirationD.addItem(String.valueOf(i));
            }
            expirationY.addItem("Year");
            for (int i = 1969; i <= 2069; i++) {
                expirationY.addItem(String.valueOf(i));
            }

            // expiration month combo box with a label
            gbc2.gridx = GridBagConstraints.RELATIVE;
            gbc2.gridy = 2;
            gbc2.gridwidth = 1;
            gbc2.fill = GridBagConstraints.HORIZONTAL;
            expirationM.setToolTipText("The expiration month of this project.");
            panel.add(expirationM, gbc2);

            // expiration day combo box
            expirationD.setToolTipText("The expiration day of this project.");
            panel.add(expirationD, gbc2);

            // add the expiration year combo box
            expirationY.setToolTipText("The expiration year of this project.");
            panel.add(expirationY, gbc2);

            // add the flags label
            gbc2.fill = GridBagConstraints.NONE;
            gbc2.gridx = 0;
            gbc2.gridy = 5;
            gbc2.weightx = 0;
            gbc2.gridwidth = 1;
            gbc2.anchor = GridBagConstraints.NORTHWEST;
            gbc2.insets = new Insets(4, 10, 4, 5);
            gbc2.weighty = 0;
            JLabel flagsLabel = new GenericLabel("Flags:");
            flagsLabel.setBackground(Styles.COLOR_BACKGROUND);
            panel.add(flagsLabel, gbc2);

            // all the check boxes
            JPanel flagPanel = new JPanel();
            {
                // is deleted checkbox
                isHiddenCheckBox.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        // commit the change
                        metaData.setHidden(isHiddenCheckBox.isSelected());
                    }
                });
                flagPanel.add(isHiddenCheckBox);
            }
            gbc2.anchor = GridBagConstraints.NORTHWEST;
            gbc2.fill = GridBagConstraints.HORIZONTAL;
            gbc2.gridwidth = GridBagConstraints.REMAINDER;
            gbc2.weighty = 0;
            panel.add(flagPanel, gbc2);

            // put the general in a scroll pane
            GenericScrollPane scrollPane = new GenericScrollPane(panel, GenericScrollPane.VERTICAL_SCROLLBAR_ALWAYS, GenericScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setVerticalScrollBar(new GenericScrollBar());

            // add the general panel to the tabbed pane
            jtp.addTab("General", scrollPane);
        }

        // create the pane
        {
            // create the panel
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());

            // the user cannot select a column
            annotationTable.setColumnSelectionAllowed(false);
            // the user cannot select more than one row at a time
            annotationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            // listen for when a change is made to the data
            annotationDM.addTableModelListener(new TableModelListener() {

                public void tableChanged(TableModelEvent e) {
                    // when the information has been changed, commit the changes to the meta data
                    if (e.getType() == TableModelEvent.UPDATE) {
                        MetaDataAnnotation rowData = new MetaDataAnnotation(annotationTable.getValueAt(e.getFirstRow(), 0).toString(), annotationTable.getValueAt(e.getFirstRow(), 1).toString());
                        metaData.getAnnotations().set(e.getFirstRow(), rowData);
                    }
                }
            });
            annotationTable = new JTable(annotationDM);

            // create a menu
            final JMenu editMenu = new JMenu("Edit");
            editMenu.setMnemonic('E');

            // create a menu item to add to the menu
            JMenuItem addMenuItem = new JMenuItem();
            addMenuItem.setText("Add an Annotation");
            addMenuItem.setMnemonic('A');
            addMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    // add the annotation
                    try {
                        MetaDataAnnotation rowData = new MetaDataAnnotation("", "");
                        metaData.getAnnotations().add(rowData);
                        Object[] row = {"", ""};
                        annotationDM.addRow(row);
                    } catch (Exception ee) {
                        ef.show(ee, MetaDataEditFrame.this);
                    }
                }
            });
            // add the menu item to the menu
            editMenu.add(addMenuItem);

            // create a menu item to add to the menu
            JMenuItem addStickyServerItem = new JMenuItem();
            addStickyServerItem.setText("Add Sticky Server");
            addStickyServerItem.setMnemonic('S');
            addStickyServerItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    // add the annotation
                    try {
                        if (!hasShownStickyServerWarning) {
                            GenericOptionPane.showMessageDialog(MetaDataEditFrame.this,
                                    "When adding a sticky server, only specify the host. For example, instead of:\n\ntranche://141.214.182.209:443\n\nJust specify:\n\n141.214.182.209\n\nIf you want to specify multiple servers, add as separate annotations.",
                                    "How to add a sticky server",
                                    JOptionPane.INFORMATION_MESSAGE);
                            hasShownStickyServerWarning = true;
                        }

                        MetaDataAnnotation rowData = new MetaDataAnnotation(MetaDataAnnotation.PROP_STICKY_SERVER_URL, "");
                        metaData.getAnnotations().add(rowData);
                        Object[] row = {MetaDataAnnotation.PROP_STICKY_SERVER_URL, ""};
                        annotationDM.addRow(row);
                    } catch (Exception ee) {
                        ef.show(ee, MetaDataEditFrame.this);
                    }
                }
            });
            // add the menu item to the menu
            editMenu.add(addStickyServerItem);

            // create a menu item to add to the menu
            final JMenuItem deleteMenuItem = new JMenuItem();
            deleteMenuItem.setText("Delete Annotation");
            deleteMenuItem.setMnemonic('D');
            deleteMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        // delete the selected row
                        if (annotationTable.getSelectedRowCount() > 0) {
                            metaData.getAnnotations().remove(annotationTable.getSelectedRow());
                            annotationDM.removeRow(annotationTable.getSelectedRow());
                        }
                    } catch (Exception ee) {
                        ef.show(ee, MetaDataEditFrame.this);
                    }
                }
            });
            // add the menu item to the menu
            editMenu.add(deleteMenuItem);

            editMenu.addMenuListener(
                    new MenuListener() {
                        // only allow the user to select the edit menu when a row is selected

                        public void menuSelected(MenuEvent e) {
                            if (annotationTable.getSelectedRowCount() > 0) {
                                deleteMenuItem.setEnabled(true);
                            } else {
                                deleteMenuItem.setEnabled(false);
                            }
                        }

                        public void menuDeselected(MenuEvent e) {
                        }

                        public void menuCanceled(MenuEvent e) {
                        }
                    });

            // add the menu to the menu bar
            annotationMenuBar.add(editMenu);
            // add the menu bar to the panel
            panel.add(annotationMenuBar, BorderLayout.NORTH);

            // create the scroll pane for the table and add it to the panel
            GenericScrollPane scrollPane = new GenericScrollPane(annotationTable, GenericScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, GenericScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            panel.add(scrollPane, BorderLayout.CENTER);

            // add the panel to the tabbed pane
            jtp.addTab("Annotations", panel);
        }

        // create the pane
        {
            // create the panel
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());

            // create the table with the table model
            JTable table = new JTable(encodingDM);

            // create the scroll pane for the table and add it to the panel
            GenericScrollPane scrollPane = new GenericScrollPane(table, GenericScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, GenericScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            panel.add(scrollPane, BorderLayout.CENTER);

            // add the panel to the tabbed pane
            jtp.addTab("Encodings", panel);
        }

        // create the pane
        {
            // create the panel
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());

            // create the table with the table model
            final JTable table = new JTable(signatureDM);
            // the user cannot select a column
            table.setColumnSelectionAllowed(false);
            // the user cannot select more than one row at a time
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            // create a menu
            JMenu editMenu = new JMenu("Edit");
            editMenu.setMnemonic('E');
            // add the menu to the menu bar
            signatureMenuBar.add(editMenu);
            // add the menu bar to the panel
            panel.add(signatureMenuBar, BorderLayout.NORTH);

            // create the scroll pane for the table and add it to the panel
            GenericScrollPane scrollPane = new GenericScrollPane(table, GenericScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, GenericScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            panel.add(scrollPane, BorderLayout.CENTER);

            // add the panel to the tabbed pane
            jtp.addTab("Signatures", panel);
        }

        // create the pane
        {
            // create the table with the table model
            JTable table = new JTable(partDM);

            // put the general in a scroll pane
            GenericScrollPane scrollPane = new GenericScrollPane(table, GenericScrollPane.VERTICAL_SCROLLBAR_ALWAYS, GenericScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            // add the panel to the tabbed pane
            jtp.addTab("Parts", scrollPane);
        }

        // add the tabbed pane
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        this.add(jtp, gbc);

        // save changes button
        gbc.gridy = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        saveChangesButton.addActionListener(new saveChangesButtonHandler());
        saveChangesButton.setBorder(Styles.BORDER_BUTTON_TOP);
        this.add(saveChangesButton, gbc);

        // Go ahead and check the user
        setEnabledBasedOnUser();
    }

    /**
     * <p>Enable/disable editting based on user that is logged in.</p>
     */
    private void setEnabledBasedOnUser() {
        if (metaData != null && userPanel.getUser() != null) {
            // Allow any user...
            setComponentsEnabled(true);

            // ... but warn user only once that may not be able to save permissions
            if (!hasShownPermissionsWarning) {
                GenericOptionPane.showMessageDialog(MetaDataEditFrame.this, "Only administrators can overwrite existing meta data. You can edit changes, but you might not be able to save them.\n\nYou can try to save any changes you wish to make.", "May not be able to save edits", JOptionPane.WARNING_MESSAGE);
                hasShownPermissionsWarning = true;
            }

        // --------------------------------------------------------------------
        //  The following code can limit to signer, but this would be 
        //  troublesome since:
        //     i. Users likely don't have write permissions
        //     ii. Admins should be allowed to edit, along w/ admin tools
        // --------------------------------------------------------------------

        /************************************************************************
        // Check to see that user is uploader
        for (Signature sig : metaData.getSignatures()) {
        if (userPanel.getUser().getCertificate().equals(sig.getCert())) {
        setComponentsEnabled(true);
        }
        }
         ************************************************************************/
        } else {
            setComponentsEnabled(false);
        }
    }

    /**
     * handler for the saveChangesButton presses
     */
    private class saveChangesButtonHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            Thread t = new Thread() {

                @Override()
                public void run() {
                    if (!hasShownSaveWarning) {
                        GenericOptionPane.showMessageDialog(MetaDataEditFrame.this, "This tool will attempt to save the changes using your permissions.\n\nYou will receive a summary, as well as contact information if you cannot save your edits.", "Checking your permissions...", JOptionPane.INFORMATION_MESSAGE);
                        hasShownSaveWarning = true;
                    }
                    IndeterminateProgressBar frame = new IndeterminateProgressBar("Saving meta data...");
                    frame.setLocationRelativeTo(MetaDataEditFrame.this);
                    frame.start();
                    try {
                        // create the bytestream
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        // turn the metaData into a byte stream
                        MetaDataUtil.write(metaData, baos);

                        // For every server, if it has the meta data, set it
                        List<String> savedURLs = new ArrayList();
                        List<String> failedURLs = new ArrayList();
                        Set<Exception> exceptions = new HashSet<Exception>();
                        for (String url : servers) {
                            if (true) {
                                throw new TodoException();
                            }
//                            if (ServerUtil.isServerOnline(url)) {
//                                // connect
//                                TrancheServer ts = IOUtil.connect(url);
//                                if (IOUtil.hasMetaData(ts, getMetaDataHash())) {
//                                    try {
//                                        // upload the changes
//                                        IOUtil.setMetaData(ts, getUserZipFile().getCertificate(), getUserZipFile().getPrivateKey(), false, getMetaDataHash(), baos.toByteArray());
//                                        savedURLs.add(url);
//                                    } catch (Exception ex) {
//                                        System.err.println("MetaDataEditFrame: Cannot set meta data to " + url + ": " + ex.getMessage());
//                                        failedURLs.add(url);
//                                        exceptions.add(ex);
//                                    }
//                                }
//                            }
                        }

                        // Stop the progress bar
                        frame.stop();

                        // Need to build summary message
                        StringBuffer message = new StringBuffer();
                        String title = null;
                        int type = JOptionPane.INFORMATION_MESSAGE;

                        // Was there any errors?
                        if (exceptions.size() > 0) {
                            type = JOptionPane.WARNING_MESSAGE;
                            title = "Attempted save, but error" + (exceptions.size() == 1 ? "" : "s") + " encountered";
                            message.append("A total of " + exceptions.size() + " error" + (exceptions.size() == 1 ? " was" : "s were") + " encountered while saving the data.\n\n");
                        } else {
                            title = "No errors encountered while saving changes";
                        }

                        // How many servers saved changes?
                        if (savedURLs.size() > 0) {
                            message.append("The following " + savedURLs.size() + " server" + (savedURLs.size() == 1 ? " has" : "s have") + " saved the edited meta data:\n");
                            for (String u : savedURLs) {
                                message.append("  * " + u + "\n");
                            }
                        } else {
                            message.append("No servers saved the edits to the meta data.\n");
                        }
                        message.append("\n");

                        // How many servers had problems?
                        if (failedURLs.size() > 0) {
                            message.append("The following " + failedURLs.size() + " server" + (failedURLs.size() == 1 ? "" : "s") + " did NOT save the edited meta data:\n");
                            for (String u : failedURLs) {
                                message.append("  * " + u + "\n");
                            }
                        }
                        message.append("\n");

                        GenericOptionPane.showMessageDialog(MetaDataEditFrame.this, message, title, type);

                    } catch (Exception ee) {
                        ef.show(ee, MetaDataEditFrame.this);
                    } finally {
                        frame.stop();
                    }
                }
            };
            t.setDaemon(true);
            t.start();
        }
    }

    private void setComponentsEnabled(boolean b) {
        // make sure the user can edit this meta data
//        if (b != false) {
//            for (Signature sig : metaData.getSignatures()) {
//                if (!userPanel.getUser().getCertificate().equals(sig.getCert())) {
//                    b = false;
//                }
//            }
//        }

        // set all the components editability
        userPanel.setVisible(!b);
        fileName.setEditable(b);
        mimeType.setEditable(b);
        mimeTypeComboBox.setEnabled(b);
        expirationM.setEnabled(b);
        expirationD.setEnabled(b);
        expirationY.setEnabled(b);
        isHiddenCheckBox.setEnabled(b);
        annotationTable.setEnabled(b);
        annotationMenuBar.setVisible(b);
        signatureMenuBar.setVisible(b);
        saveChangesButton.setVisible(b);
    }

    public UserZipFile getUserZipFile() {
        return userPanel.getUser();
    }

    public void setUserZipFile(UserZipFile aUserZipFile) {
        userPanel.setUser(aUserZipFile);
        setEnabledBasedOnUser();
    }

    public BigHash getMetaDataHash() {
        return metaDataHash;
    }

    public void setMetaDataHash(BigHash aMetaDataHash) throws Exception {
        metaDataHash = aMetaDataHash;

        // use the GetFileTool to download the project file
        GetFileTool gft = new GetFileTool();
        gft.setHash(aMetaDataHash);
        gft.setValidate(false);

        // if there is a specific server to use
        for (String url : servers) {
            gft.getServersToUse().add(url);
        }

        // get the meta data
        setMetaData(gft.getMetaData());
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData aMetaData) {
        metaData = aMetaData;
        fileName.setText(metaData.getName());
        mimeType.setText(metaData.getMimeType());

        isHiddenCheckBox.setSelected(metaData.isHidden());

        // add the rows to the annotation table
        for (MetaDataAnnotation mda : metaData.getAnnotations()) {
            // add the row to the table model
            annotationDM.addRow(new Object[]{mda.getName(), mda.getValue()});
        }

        // add the rows to the encoding table
        for (FileEncoding fe : metaData.getEncodings()) {
            // create the row
            Object[] row = {fe.getName(), fe.getHash().toString()};
            // add the row to the table model
            encodingDM.addRow(row);
        }

        // add the rows to the signature table
        // create the row
        Object[] row = {metaData.getSignature().getUserName()};
        // add the row to the table model
        signatureDM.addRow(row);

        // add the rows to the part table
        for (BigHash part : metaData.getParts()) {
            // create the row
            Object[] partRow = {part.toString()};
            // add the row to the table model
            partDM.addRow(partRow);
        }
    }

    public void setServers(Collection<String> servers) {
        this.servers.clear();
        this.servers.addAll(servers);
    }

    public static void main(String[] args) throws Exception {
        // configure Tranche network
        ConfigureTrancheGUI.load(args);

        // make the GUI
        MetaDataEditFrame gui = new MetaDataEditFrame();

        // show the frame
        GUIUtil.centerOnScreen(gui);
        gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gui.setVisible(true);
    }
}
