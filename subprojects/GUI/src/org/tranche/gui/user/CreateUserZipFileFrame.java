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

import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.*;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.tranche.users.MakeUserZipFileTool;
import org.tranche.users.UserZipFile;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class CreateUserZipFileFrame extends GenericFrame {

    // text boxes for each field
    public JTextField name = new GenericTextField();
    public JTextField validDays = new GenericTextField();
    public JPasswordField passphrase = new JPasswordField();
    public JPasswordField passphraseConfirm = new JPasswordField();    // the signer certificate
    public SignInUserButton signer = new SignInUserButton();    // the button to click for making the user
    public JButton createUserButton = new GenericButton("Create User");    // the make certificate tools
    private MakeUserZipFileTool userTool = new MakeUserZipFileTool();    // a flag for testing purposes, hides the confirmation
    private boolean testing = false;
    // used when testing to set the saving location
    private File selectedFile = null;

    public CreateUserZipFileFrame() {
        super("Create New User");

        // normally dispose on a close
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // set the appropriate size
        setSize(400, 300);

        // set the layout
        setLayout(new BorderLayout());

        // set the menu bar
        {
            JMenuBar menuBar = new GenericMenuBar();
            JMenu helpMenu = new GenericMenu("Help");
            JMenuItem aboutMenuItem = new GenericMenuItem("About");
            aboutMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    DescriptionFrame gui = new DescriptionFrame("About Create User Tool", "This tool creates new user files for Tranche. The files should be saved with a '.zip.encrypted' extension. Hover over any of the fields to learn more.\n\nPermissions can be passed to the new user only if the signing user has specifically been registered with a server.");
                    // open in the default platform location
                    gui.setLocationRelativeTo(CreateUserZipFileFrame.this);
                    // show the frame
                    gui.setVisible(true);
                }
            });
            helpMenu.add(aboutMenuItem);
            menuBar.add(helpMenu);

            add(menuBar, BorderLayout.NORTH);
        }

        {
            JPanel panel = new JPanel();

            // use gridbag
            panel.setLayout(new GridBagLayout());
            // set the constraints
            GridBagConstraints gbc = new GridBagConstraints();

            // set the associated weights
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(0, 3, 0, 3);

            // set the borders
            name.setBorder(Styles.BORDER_BLACK_1);
            validDays.setBorder(Styles.BORDER_BLACK_1);

            // add a label
            JLabel userInfoLabel = new GenericLabel("User Information");
            userInfoLabel.setFont(Styles.FONT_TITLE_1);
            userInfoLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.COLOR_BORDER_DARK));
            panel.add(userInfoLabel, gbc);

            // add a label for the name
            gbc.insets = new Insets(10, 5, 2, 5);
            JPanel namePanel = new LabeledPanel("Name: ", name);
            namePanel.setToolTipText("This is the name that will appear as your Tranche user name.");
            panel.add(namePanel, gbc);
            gbc.insets = new Insets(2, 5, 2, 5);
            JPanel validDaysPanel = new LabeledPanel("Valid Days: ", validDays);
            validDaysPanel.setToolTipText("The number of days until this user file expires. Time starts expiring the moment the user file is created.");
            panel.add(validDaysPanel, gbc);

            gbc.gridwidth = GridBagConstraints.RELATIVE;
            passphrase.setBorder(Styles.BORDER_BLACK_1);
            JPanel passphrasePanel = new LabeledPanel("Passphrase: ", passphrase);
            passphrasePanel.setToolTipText("The passphrase that will be required in order to decrypt and use your user file. If you leave this blank, the passphrase will be an empty string.");
            panel.add(passphrasePanel, gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 0;
            JButton generatePassphraseButton = new RandomPassphraseButton(passphrase, passphraseConfirm);
            panel.add(generatePassphraseButton, gbc);

            gbc.weightx = 1;
            passphraseConfirm.setBorder(Styles.BORDER_BLACK_1);
            JPanel passphraseConfirmPanel = new LabeledPanel("Confirm Passphrase: ", passphraseConfirm);
            passphraseConfirmPanel.setToolTipText("Enter the same passphrase as above in order to confirm that it is what you expect.");
            panel.add(passphraseConfirmPanel, gbc);

            // remove the spacing for the title
            gbc.insets = new Insets(10, 3, 0, 3);

            // add the signer button
            JLabel signingUserLabel = new GenericLabel("Signing User (Optional)");
            signingUserLabel.setFont(Styles.FONT_TITLE_1);
            signingUserLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.COLOR_BORDER_DARK));
            panel.add(signingUserLabel, gbc);

            gbc.insets = new Insets(5, 5, 5, 5);
            panel.add(signer, gbc);

            add(panel, BorderLayout.CENTER);
        }

        {
            // add the make button
            createUserButton.setBorder(Styles.BORDER_BUTTON_TOP);
            createUserButton.setFont(Styles.FONT_TITLE);
            createUserButton.addActionListener(new CreateUserListener());
            add(createUserButton, BorderLayout.SOUTH);
        }
    }

    public static void main(String[] args) {
        // configure the Tranche network
        ConfigureTrancheGUI.load(args);

        CreateUserZipFileFrame cuf = new CreateUserZipFileFrame();
        GUIUtil.centerOnScreen(cuf);
        cuf.setVisible(true);
    }

    class CreateUserListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            final JFileChooser jfc = GUIUtil.makeNewFileChooser();
            // make the thread
            Thread t = new Thread() {

                @Override
                public void run() {
                    try {
                        // set all of the values
                        CreateUserZipFileFrame.this.userTool.setName(CreateUserZipFileFrame.this.name.getText());
                        CreateUserZipFileFrame.this.userTool.setValidDays(Long.parseLong(CreateUserZipFileFrame.this.validDays.getText()));

                        // check the passphrase
                        String pass = CreateUserZipFileFrame.this.passphrase.getText();
                        String passConfirm = CreateUserZipFileFrame.this.passphraseConfirm.getText();
                        if (!pass.equals(passConfirm)) {
                            GenericOptionPane.showMessageDialog(CreateUserZipFileFrame.this, "Passphrases don't match. Check that they are the same.");
                            return;
                        }
                        // set the passphrase
                        CreateUserZipFileFrame.this.userTool.setPassphrase(pass);

                        // check for a signer
                        UserZipFile signer = CreateUserZipFileFrame.this.signer.getUser();
                        if (signer != null) {
                            CreateUserZipFileFrame.this.userTool.setSignerCertificate(signer.getCertificate());
                            CreateUserZipFileFrame.this.userTool.setSignerPrivateKey(signer.getPrivateKey());
                        }

                        // prompt the user for a file
                        jfc.setDialogTitle("Select a location for the user file");
                        jfc.setMultiSelectionEnabled(false);
                        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        if (selectedFile != null) {
                            jfc.setSelectedFile(selectedFile);
                        } else {
                            jfc.showSaveDialog(CreateUserZipFileFrame.this);
                        }

                        // get the selected file
                        selectedFile = jfc.getSelectedFile();
                        if (selectedFile == null) {
                            return;
                        }
                        // make sure that it ends with .zip.encrypted
                        if (!selectedFile.getName().endsWith(".zip.encrypted")) {
                            selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".zip.encrypted");
                        }
                        // set the file
                        CreateUserZipFileFrame.this.userTool.setSaveFile(selectedFile);

                        // make the user
                        UserZipFile uzf = CreateUserZipFileFrame.this.userTool.makeCertificate();

                        // note that the file was made
                        if (!testing) {
                            GenericOptionPane.showMessageDialog(CreateUserZipFileFrame.this, "User file succesfully made.");
                        }

                        // dump the cert for luck
                        System.out.println(uzf.getCertificate());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            t.setDaemon(true);
            t.start();
        }
    }

    protected boolean isTesting() {
        return testing;
    }

    protected void setTesting(boolean testing) {
        this.testing = testing;
    }

    protected void setSaveLocation(File location) {
        this.selectedFile = location;
    }
}

// helper class
class LabeledPanel extends JPanel {

    JLabel label;
    JTextField text;

    public LabeledPanel(String label, JTextField text) {
        // set the layout
        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        // set up the constraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.weighty = 0;
        // fill rest with text
        gbc.insets = new Insets(0, 2, 0, 0);
        // add the label
        this.label = new GenericLabel(label);
        add(this.label, gbc);
        gbc.weightx = 1;
        add(text, gbc);

        // set the fields
        this.text = text;
    }

    @Override
    public void setToolTipText(String text) {
        // set on the super
        super.setToolTipText(text);
        // set on the sub-components
        label.setToolTipText(text);
        this.text.setToolTipText(text);
    }
}
