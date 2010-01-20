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
package org.tranche.gui.get.wizard;

import org.tranche.gui.hash.BigHashHelperPanel;
import org.tranche.gui.util.GUIUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import org.tranche.get.GetFileTool;
import org.tranche.gui.ConfigureTrancheGUI;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GenericCheckBoxMenuItem;
import org.tranche.gui.GenericMenu;
import org.tranche.gui.GenericMenuBar;
import org.tranche.gui.GenericMenuItem;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.gui.HomeButton;
import org.tranche.gui.LazyLoadAllSlowStuffAfterGUIRenders;
import org.tranche.gui.PreferencesFrame;
import org.tranche.gui.get.DownloadSummary;
import org.tranche.gui.server.ServersFrame;
import org.tranche.gui.wizard.GenericWizard;
import org.tranche.gui.wizard.GenericWizardStep;
import org.tranche.hash.BigHash;
import org.tranche.users.InvalidSignInException;
import org.tranche.users.UserZipFileUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class GetFileToolWizard extends GenericWizard {

    protected GetFileToolWizardMenuBar menuBar = new GetFileToolWizardMenuBar();
    protected ServersFrame serversFrame;
    protected ErrorFrame ef = new ErrorFrame();
    protected DownloadSummary summary = new DownloadSummary(new GetFileTool());
    protected GFTStep1Panel step1Panel = new GFTStep1Panel(this);
    protected GFTStep2Panel step2Panel = new GFTStep2Panel(this);
    protected GFTStep3Panel step3Panel = new GFTStep3Panel(this);
    protected GFTStep4Panel step4Panel = new GFTStep4Panel(this);

    public GetFileToolWizard() {
        this(null);
    }

    public GetFileToolWizard(Collection<String> servers) {
        super("Get File Tool Wizard", new Dimension(700, 530));

        // change close behavior depending on whether it was opened from the advanced gui
        if (GUIUtil.getAdvancedGUI() != null) {
            setDefaultCloseOperation(GenericWizard.DISPOSE_ON_CLOSE);
        } else {
            setDefaultCloseOperation(GenericWizard.EXIT_ON_CLOSE);
        }

        GenericWizardStep step1 = new GenericWizardStep("Download Parameters", step1Panel);
        step1.setButtonLabel("Download Parameters");
        step1.setIsNextDisabled(true);
        addStep(step1);

        GenericWizardStep step2 = new GenericWizardStep("Select Files", step2Panel);
        step2.setButtonLabel("Select Files");
        addStep(step2);

        GenericWizardStep step3 = new GenericWizardStep("Save Location", step3Panel);
        step3.setButtonLabel("Save Location");
        addStep(step3);

        // Only add if free-standing tool
        if (GUIUtil.getAdvancedGUI() == null) {
            GenericWizardStep step4 = new GenericWizardStep("Downloading...", step4Panel);
            step4.setButtonLabel("Download");
            step4.setIsBackDisabled(true);
            addStep(step4);
            setFinalButtonLabel("Cancel Download and Close");
        } else {
            setFinalButtonLabel("Download");
        }

        // create server frame
        serversFrame = new ServersFrame("Servers", servers);
        serversFrame.setIconImage(getIconImage());

        // set the menu bar for the wizard
        setMenuBar(menuBar);

        refresh();
    }

    public void setHash(BigHash hash) {
        summary.getGetFileTool().setHash(hash);
        step1Panel.setHash(hash);
    }

    public void setRegex(String regEx) {
        summary.getGetFileTool().setRegEx(regEx);
        step2Panel.setRegEx(regEx);
    }

    public void setValidate(boolean validate) {
        summary.getGetFileTool().setValidate(validate);
        menuBar.validateMenuItem.setSelected(validate);
    }

    public void setPassphrase(String passphrase) {
        summary.getGetFileTool().setPassphrase(passphrase);
        step1Panel.setPassphrase(passphrase);
    }

    public void selectUploader(String uploaderName, long uploadTimestamp, String relativePath) throws Exception {
        summary.getGetFileTool().setUploaderName(uploaderName);
        summary.getGetFileTool().setUploadTimestamp(uploadTimestamp);
        summary.getGetFileTool().setUploadRelativePath(relativePath);
        step1Panel.selectUploader(uploaderName, uploadTimestamp, relativePath);
    }

    public class GetFileToolWizardMenuBar extends GenericMenuBar {

        public HomeButton homeButton = new HomeButton(ef, GetFileToolWizard.this);
        private GenericMenu optionsMenu = new GenericMenu("Options");
        private GenericMenu logsMenu = new GenericMenu("Logs");
        private GenericMenu helpMenu = new GenericMenu("Help");
        private GenericMenuItem serversMenuItem = new GenericMenuItem("Servers");
        private GenericMenu parametersMenu = new GenericMenu("Parameters", true);
        protected GenericCheckBoxMenuItem validateMenuItem = new GenericCheckBoxMenuItem("Validate", GetFileTool.DEFAULT_VALIDATE);
        protected GenericCheckBoxMenuItem continueOnFailureCheckBoxItem = new GenericCheckBoxMenuItem("Continue On Failure", GetFileTool.DEFAULT_CONTINUE_ON_FAILURE);
        protected GenericCheckBoxMenuItem useUnspecifiedServersCheckBoxItem = new GenericCheckBoxMenuItem("Use Unspecified Servers", GetFileTool.DEFAULT_USE_UNSPECIFIED_SERVERS);
        private GenericMenuItem preferencesMenuItem = new GenericMenuItem("Preferences");
        private GenericMenuItem monitorMenuItem = new GenericMenuItem("Monitor");
        private GenericMenuItem hashHelperMenuItem = new GenericMenuItem("Tranche Hash Helper");

        public GetFileToolWizardMenuBar() {
            homeButton.setVisible(GUIUtil.getAdvancedGUI() == null);
            homeButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            add(homeButton);

            serversMenuItem.setMnemonic('s');
            serversMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            serversFrame.setLocationRelativeTo(GetFileToolWizard.this);
                            serversFrame.setVisible(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            optionsMenu.add(serversMenuItem);

            {
                validateMenuItem.setMnemonic('v');
                parametersMenu.add(validateMenuItem);
                continueOnFailureCheckBoxItem.setMnemonic('c');
                parametersMenu.add(continueOnFailureCheckBoxItem);
                useUnspecifiedServersCheckBoxItem.setMnemonic('u');
                parametersMenu.add(useUnspecifiedServersCheckBoxItem);
            }
            parametersMenu.setMnemonic('p');
            optionsMenu.add(parametersMenu);

            JSeparator separator = new JSeparator();
            separator.setForeground(Color.BLACK);
            optionsMenu.add(separator);

            preferencesMenuItem.setMnemonic('r');
            preferencesMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            GUIUtil.getPreferencesFrame().show(PreferencesFrame.SHOW_DOWNLOADS);
                            GUIUtil.getPreferencesFrame().setLocationRelativeTo(GUIUtil.getAdvancedGUI());
                            GUIUtil.getPreferencesFrame().setVisible(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            optionsMenu.add(preferencesMenuItem);

            optionsMenu.setMnemonic('o');
            add(optionsMenu);

            monitorMenuItem.setMnemonic('m');
            monitorMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            if (!(summary.getGetFileTool().isExecuting() || summary.isFinished())) {
                                GenericOptionPane.showMessageDialog(GetFileToolWizard.this, "The download must be started before the monitor can be opened.", "Could Not Open Monitor", JOptionPane.WARNING_MESSAGE);
                                return;
                            }
                            summary.showMonitor(GetFileToolWizard.this);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });

            logsMenu.setMnemonic('l');
            logsMenu.add(monitorMenuItem);
            add(logsMenu);

            hashHelperMenuItem.setMnemonic('t');
            hashHelperMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            GenericPopupFrame popup = new GenericPopupFrame("Tranche Hash Helper", new BigHashHelperPanel());
                            popup.setSizeAndCenter(BigHashHelperPanel.RECOMMENDED_DIMENSION);
                            popup.setVisible(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });

            helpMenu.setMnemonic('h');
            helpMenu.add(hashHelperMenuItem);
            add(helpMenu);
        }
    }

    public static void main(String[] args) throws Exception {
        // configure Tranche network
        ConfigureTrancheGUI.load(args);

        GetFileToolWizard wizard = new GetFileToolWizard();
        GUIUtil.centerOnScreen(wizard);
        wizard.setVisible(true);

        Set<Exception> exceptions = new HashSet<Exception>();
        for (int i = 1; i < args.length; i += 2) {
            try {
                if (args[i].equals("-h") || args[i].equals("--fileName") || args[i].equals("--hash")) {
                    wizard.setHash(BigHash.createHashFromString(args[i + 1]));
                } else if (args[i].equals("-p") || args[i].equals("--passphrase")) {
                    wizard.setPassphrase(args[i + 1]);
                } else if (args[i].equals("-a") || args[i].equals("--validate")) {
                    wizard.setValidate(Boolean.valueOf(args[i + 1]));
                } else if (args[i].equals("-r") || args[i].equals("--regex")) {
                    wizard.setRegex(args[i + 1]);
                } else if (args[i].equals("-L") || args[i].equals("--login")) {
                    try {
                        GUIUtil.setUser(UserZipFileUtil.getUserZipFile(args[i + 1], args[i + 2]));
                    } catch (InvalidSignInException e) {
                        GenericOptionPane.showMessageDialog(wizard, e.getMessage(), "Could Not Sign In", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        i++;
                    }
                }
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            wizard.ef.show(exceptions, wizard);
        }

        LazyLoadAllSlowStuffAfterGUIRenders.lazyLoad();
    }
}
