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
package org.tranche.gui.add.wizard;

import java.awt.Color;
import org.tranche.gui.wizard.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.GenericTextArea;
import org.tranche.gui.GenericTextField;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.GenericButton;
import org.tranche.gui.GenericCheckBox;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.user.SignInUserButton;
import org.tranche.gui.RandomPassphraseButton;
import org.tranche.util.PreferencesUtil;
import org.tranche.gui.Styles;
import org.tranche.util.Text;
import org.tranche.users.UserZipFile;

/**
 * Step 1: Show upload parameters
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class AFTStep1Panel extends GenericWizardPanel {

    private AddFileToolWizard wizard;
    public JTextArea description = new GenericTextArea();
    public JTextField fileText = new GenericTextField(), title = new GenericTextField();
    public SignInUserButton userButton = new SignInUserButton();
    public EncryptionPanel encryptionPanel;
    public long totalFiles = 0, totalFileSize = 0;
    public final Set<String> uploadStructures = new HashSet<String>();

    public AFTStep1Panel(AddFileToolWizard wizard) {
        this.wizard = wizard;

        setBackground(Styles.COLOR_PANEL_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        final JLabel estimatedUploadInformation = new GenericLabel("");

        // for selecting the file to upload
        {
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(10, 5, 3, 3);
            JLabel name = new GenericLabel("Upload:");
            name.setToolTipText("Select a file or directory to upload.");
            add(name, gbc);

            // display the value
            gbc.gridwidth = 1;
            gbc.weightx = 1;
            gbc.insets = new Insets(10, 3, 3, 5);
            fileText.setToolTipText("This is the file or directory that will be uploaded.");
            fileText.setEnabled(false);
            add(fileText, gbc);

            // add a button to select a file
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 0;
            gbc.insets = new Insets(10, 3, 3, 5);
            JButton fileButton = new GenericButton("   Select   ");
            fileButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final JFileChooser fc = new JFileChooser();
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            // query for a file
                            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

                            // make the last seleected upload file the default
                            try {
                                File lastUpload = new File(PreferencesUtil.get(PreferencesUtil.PREF_UPLOAD_LOCATION));
                                if (lastUpload.exists()) {
                                    fc.setSelectedFile(lastUpload);
                                }
                            } catch (Exception ee) {
                            }

                            fc.showOpenDialog(AFTStep1Panel.this.getParent());

                            // check for a submitted file
                            File selectedFile = fc.getSelectedFile();
                            if (selectedFile == null) {
                                return;
                            }

                            // check against the upload structure
                            if (isFileInUploadStructure(selectedFile)) {
                                // notify the user and ask them if they want to change their upload
                                int returnValue = GenericOptionPane.showConfirmDialog(AFTStep1Panel.this.getParent(), "The file you selected is within a known file structure.\nWould you rather upload " + getTopStructureFile(selectedFile).getAbsolutePath() + "?", "Upload File", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (returnValue == JOptionPane.YES_OPTION) {
                                    selectedFile = getTopStructureFile(selectedFile);
                                }
                            }

                            // save the upload file location
                            try {
                                PreferencesUtil.set(PreferencesUtil.PREF_UPLOAD_LOCATION, selectedFile.getAbsolutePath());
                                PreferencesUtil.save();
                            } catch (Exception ee) {
                                ee.printStackTrace();
                            }

                            // set the appropriate string
                            fileText.setText(selectedFile.toString());

                            // update the label information
                            estimatedUploadInformation.setText("  Calculating size...");
                            // iterate over all of the files
                            ArrayList<File> filesToCount = new ArrayList();
                            filesToCount.add(selectedFile);
                            totalFiles = 0;
                            totalFileSize = 0;
                            while (filesToCount.size() > 0) {
                                File fileToCount = filesToCount.remove(0);
                                if (fileToCount.isDirectory()) {
                                    File[] filesToAdd = fileToCount.listFiles();
                                    for (File fileToAdd : filesToAdd) {
                                        filesToCount.add(fileToAdd);
                                    }
                                } else {
                                    totalFiles++;
                                    totalFileSize += fileToCount.length();
                                    // if more than 100, update information
                                    if (totalFiles % 100 == 0) {
                                        estimatedUploadInformation.setText("  " + GUIUtil.integerFormat.format(totalFiles) + " files, " + Text.getFormattedBytes(totalFileSize) + " of data");
                                    }
                                }
                            }
                            // set the estimated upload information
                            estimatedUploadInformation.setText("  " + GUIUtil.integerFormat.format(totalFiles) + " files, " + Text.getFormattedBytes(totalFileSize) + " of data");
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            fileButton.setBorder(Styles.BORDER_BLACK_1_PADDING_SIDE_2);
            fileButton.setFont(Styles.FONT_10PT);
            fileButton.setToolTipText("Click to select the file or directory to upload.");
            add(fileButton, gbc);
        }

        // a panel that shows the summary for files
        {
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.gridx = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(0, 3, 10, 5);
            estimatedUploadInformation.setFont(Styles.FONT_12PT);
            estimatedUploadInformation.setToolTipText("Tentative information regarding the number of files and and size of data.");
            add(estimatedUploadInformation, gbc);
        }

        // for adding the user information
        {
            gbc.gridx = GridBagConstraints.RELATIVE;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(3, 5, 3, 5);
            JLabel name = new GenericLabel("User:");
            add(name, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.insets = new Insets(3, 3, 3, 5);
            add(userButton, gbc);
        }

        // make the title
        {
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(3, 5, 3, 5);
            JLabel label = new GenericLabel("Title:");
            add(label, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.insets = new Insets(3, 3, 3, 5);
            add(title, gbc);
        }

        // make the description
        {
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.insets = new Insets(10, 5, 2, 5);
            JLabel label = new GenericLabel("Description:");
            add(label, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.insets = new Insets(0, 5, 5, 5);
            description.setWrapStyleWord(true);
            description.setLineWrap(true);
            // add as a scroll pane
            add(new GenericScrollPane(description), gbc);
        }

        // Add the encryption panel
        {
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 10, 10, 10);
            encryptionPanel = new EncryptionPanel();
            encryptionPanel.setBackground(getBackground());
            encryptionPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.DARK_GRAY));
            add(encryptionPanel, gbc);
        }
    }

    public void setUserZipFile(UserZipFile user) {
        userButton.setUser(user);
    }

    public void setUploadStructures(Collection<String> uploadStructures) {
        this.uploadStructures.clear();
        this.uploadStructures.addAll(uploadStructures);
    }

    public void onLoad() {
    }

    public boolean onNext() {
        // check that the user entered something in the field
        if (fileText.getText() == null || fileText.getText().trim().equals("")) {
            GenericOptionPane.showMessageDialog(AFTStep1Panel.this.getParent(),
                    "Please specify a file or directory to upload.",
                    "Nothing to upload",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        // check that the name matches a real file
        File fileToUpload = new File(fileText.getText().trim());
        if (!fileToUpload.exists()) {
            GenericOptionPane.showMessageDialog(AFTStep1Panel.this.getParent(),
                    "Could not find upload file or directory",
                    "Upload not found",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        wizard.summary.getAddFileTool().setFile(fileToUpload);

        // check for a title/description if this is a directory upload
        if (description.getText().trim().equals("") || title.getText().trim().equals("")) {
            // pop up a window asking if they really want to skip title/description
            GenericOptionPane.showMessageDialog(AFTStep1Panel.this.getParent(),
                    "Please provide a title and description for your upload.",
                    "Please offer a title and description",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        wizard.summary.getAddFileTool().setDescription(description.getText());
        wizard.summary.getAddFileTool().setTitle(title.getText());

        // check for the public certificate
        if (userButton.getUser() == null) {
            GenericOptionPane.showMessageDialog(AFTStep1Panel.this.getParent(),
                    "Please load a user certificate.",
                    "No user set",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        wizard.summary.getAddFileTool().setUserCertificate(userButton.getUser().getCertificate());
        wizard.summary.getAddFileTool().setUserPrivateKey(userButton.getUser().getPrivateKey());

        // check passphrases
        if (encryptionPanel.encryptBox.isSelected()) {
            String[] passphrases = encryptionPanel.getPassphrases();
            String passphrase1 = passphrases[0];
            String passphrase2 = passphrases[1];

            // If password blank, tell user
            if (passphrase1 == null || passphrase1.equals("")) {
                GenericOptionPane.showMessageDialog(
                        AFTStep1Panel.this.getParent(),
                        "If you choose to encrypt your upload, you must provide a passphrase.\nPlease set and confirm a passphrase.",
                        "Must provide passphrase",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // If don't match, tell user
            if (!passphrase1.equals(passphrase2)) {
                GenericOptionPane.showMessageDialog(
                        AFTStep1Panel.this.getParent(),
                        "The passphrases you provided do not match.",
                        "Passphrases don't match",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // remove white space?
            String trimPassphrase = passphrases[0].trim();
            if (trimPassphrase.length() != passphrases[0].length()) {
                GenericOptionPane.showMessageDialog(wizard, "The passphrase you have entered contains extra space at the beginning or end.\nIt will be trimmed.", "Whitespace Warning", JOptionPane.WARNING_MESSAGE);
                encryptionPanel.password1.setText(trimPassphrase);
                encryptionPanel.password2.setText(trimPassphrase);
            }

            // Set the passphrase
            wizard.summary.getAddFileTool().setPassphrase(trimPassphrase);
            wizard.summary.getAddFileTool().setShowMetaDataIfEncrypted(encryptionPanel.isShareMetaIfEncrypted.isSelected());
        }

        return true;
    }

    public boolean onPrevious() {
        return false;
    }

    public boolean isFileInUploadStructure(File file) {
        String path = file.getAbsolutePath();

        // test the directories the file is in
        while (path.contains("\\")) {
            for (String structure : uploadStructures) {
                if (structure.equals("\\" + path)) {
                    return true;
                }
            }
            path = path.substring(path.indexOf("\\") + 1);
        }

        // test just the file
        for (String structure : uploadStructures) {
            if (structure.equals("\\" + path)) {
                return true;
            }
        }

        return false;
    }

    public File getTopStructureFile(File file) {
        String path = file.getAbsolutePath();

        // test the directories the file is in
        String shortestStructure = null;
        while (path.contains("\\")) {
            for (String structure : uploadStructures) {
                if (structure.equals("\\" + path)) {
                    if (shortestStructure == null) {
                        shortestStructure = structure;
                    } else {
                        if (structure.length() < shortestStructure.length()) {
                            shortestStructure = structure;
                        }
                    }
                }
            }
            path = path.substring(path.indexOf("\\") + 1);
        }

        // test just the file
        for (String structure : uploadStructures) {
            if (structure.equals("\\" + path)) {
                if (shortestStructure == null) {
                    shortestStructure = structure;
                } else {
                    if (structure.length() < shortestStructure.length()) {
                        shortestStructure = structure;
                    }
                }
            }
        }

        if (shortestStructure == null) {
            return null;
        } else {
            return new File(file.getAbsolutePath().replace(shortestStructure, ""));
        }
    }

    /**
     * <p>Simplifies management of passwords or other options.</p>
     * <p>Note that this is used in different contexts and will change as needed.</p>
     */
    public class EncryptionPanel extends JPanel {

        private JPasswordField password1 = new JPasswordField(), password2 = new JPasswordField();
        private JLabel password1Label = new GenericLabel("Passphrase:"), password2Label = new GenericLabel("Confirm Passphrase:");
        public GenericCheckBox encryptBox = new GenericCheckBox("Encrypt"), isShareMetaIfEncrypted = new GenericCheckBox("Display descriptive information even though encrypted");
        public RandomPassphraseButton random = new RandomPassphraseButton(password1, password2);

        private EncryptionPanel() {
            setPassphrasesVisible(false);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 1;
            gbc.weightx = 0.1;
            gbc.weighty = 0;
            gbc.insets = new Insets(5, 0, 0, 0);
            encryptBox.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    setPassphrasesVisible(encryptBox.isSelected());
                }
            });
            add(encryptBox, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            isShareMetaIfEncrypted.setToolTipText("Allow descriptive information to be shown while encrypted. By default, most information describing an upload is hidden until it is made public.");
            add(isShareMetaIfEncrypted, gbc);

            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(8, 0, 0, 10);
            gbc.weightx = 0;
            add(password1Label, gbc);

            gbc.gridwidth = 1;
            gbc.weightx = 1;
            add(password1, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(8, 0, 0, 0);
            gbc.weightx = 0;
            add(random, gbc);

            gbc.gridwidth = 1;
            gbc.insets = new Insets(5, 0, 0, 10);
            gbc.weightx = 0;
            add(password2Label, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(5, 0, 0, 0);
            gbc.weightx = 1;
            add(password2, gbc);
        }

        public String[] getPassphrases() {
            String[] passphrases = new String[2];
            passphrases[0] = password1.getText();
            passphrases[1] = password2.getText();
            return passphrases;
        }

        private void setPassphrasesVisible(boolean visible) {
            encryptBox.setSelected(visible);
            isShareMetaIfEncrypted.setVisible(visible);
            random.setVisible(visible);
            password1.setVisible(visible);
            password2.setVisible(visible);
            password1Label.setVisible(visible);
            password2Label.setVisible(visible);
        }
    }
}
