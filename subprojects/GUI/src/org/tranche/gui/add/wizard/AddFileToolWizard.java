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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import org.tranche.add.AddFileTool;
import org.tranche.add.AddFileToolListener;
import org.tranche.add.AddFileToolPerformanceLog;
import org.tranche.gui.AnnotationFrame;
import org.tranche.gui.ConfigureTrancheGUI;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.GenericCheckBoxMenuItem;
import org.tranche.gui.GenericMenu;
import org.tranche.gui.GenericMenuBar;
import org.tranche.gui.GenericMenuItem;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.HomeButton;
import org.tranche.gui.ImagePanel;
import org.tranche.gui.LazyLoadAllSlowStuffAfterGUIRenders;
import org.tranche.gui.PreferencesFrame;
import org.tranche.gui.add.UploadSummary;
import org.tranche.gui.server.ServersFrame;
import org.tranche.gui.wizard.GenericWizard;
import org.tranche.gui.wizard.GenericWizardStep;
import org.tranche.time.TimeUtil;
import org.tranche.users.InvalidSignInException;
import org.tranche.users.UserZipFile;
import org.tranche.users.UserZipFileUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.Text;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class AddFileToolWizard extends GenericWizard {

    private static boolean debug = false;
    private AddFileToolWizardMenuBar menuBar = new AddFileToolWizardMenuBar();
    protected AnnotationFrame annotationFrame = new AnnotationFrame();
    protected ServersFrame serversFrame,  stickyServersFrame;
    protected ErrorFrame ef = new ErrorFrame();
    protected UploadSummary summary = new UploadSummary(new AddFileTool());
    protected AFTStep1Panel step1Panel = new AFTStep1Panel(this);
    protected AFTStep2Panel step2Panel = new AFTStep2Panel(this);
    protected AFTStep3Panel step3Panel = new AFTStep3Panel(this);
    protected AFTStep4Panel step4Panel = new AFTStep4Panel(this);

    public AddFileToolWizard() {
        this(null);
    }

    public AddFileToolWizard(Collection<String> servers) {
        super("Upload Tool", new Dimension(750, 675));

        // change close behavior depending on whether it was opened from the advanced gui
        if (GUIUtil.getAdvancedGUI() != null) {
            setDefaultCloseOperation(GenericWizard.DISPOSE_ON_CLOSE);
        } else {
            setDefaultCloseOperation(GenericWizard.EXIT_ON_CLOSE);
        }

        JPanel cclp = null;
        try {
            cclp = new ImagePanel(ImageIO.read(AddFileToolWizard.this.getClass().getResource("/org/tranche/gui/image/cc.gif")));
        } catch (IOException ex) {
        }

        GenericWizardStep step1 = new GenericWizardStep("Upload Parameters", step1Panel);
        step1.setButtonLabel("Upload Parameters");
        addStep(step1);

        GenericWizardStep step2 = new GenericWizardStep("Select License", step2Panel);
        step2.setButtonLabel("Select License");
        step2.setIcon(cclp);
        addStep(step2);

        GenericWizardStep step3 = new GenericWizardStep("License Agreement", step3Panel);
        step3.setButtonLabel("License Agreement");
        step3.setIcon(cclp);
        addStep(step3);

        if (GUIUtil.getAdvancedGUI() == null) {
            GenericWizardStep step4 = new GenericWizardStep("Uploading...", step4Panel);
            step4.setButtonLabel("Upload");
            step4.setIsBackDisabled(true);
            addStep(step4);
            setFinalButtonLabel("Stop Upload and Close Window");
        } else {
            setFinalButtonLabel("Begin Upload");
        }

        serversFrame = new ServersFrame("Servers", servers);
        serversFrame.setIconImage(getIconImage());
        stickyServersFrame = new ServersFrame("Sticky Servers", servers);
        stickyServersFrame.setIconImage(getIconImage());
        annotationFrame.setIconImage(getIconImage());

        // set the menu
        setMenuBar(menuBar);

        // paint
        refresh();
    }

    public void setUser(UserZipFile userZipFile) {
        step1Panel.userButton.setUser(userZipFile);
    }

    public class AddFileToolWizardMenuBar extends GenericMenuBar {

        public HomeButton homeButton = new HomeButton(ef, AddFileToolWizard.this);
        public GenericMenu optionsMenu = new GenericMenu("Options");
        public GenericMenu uploadParametersMenu = new GenericMenu("Parameters", true);
        private GenericMenu logsMenu = new GenericMenu("Logs");
        public GenericMenu helpMenu = new GenericMenu("Help");
        public GenericMenuItem serversMenuItem = new GenericMenuItem("Servers");
        public GenericMenuItem stickyServersMenuItem = new GenericMenuItem("Sticky Servers");
        public GenericMenuItem annotationsMenuItem = new GenericMenuItem("Annotations");
        public GenericCheckBoxMenuItem uploadDataOnlyCheckBoxItem = new GenericCheckBoxMenuItem("Data Only", AddFileTool.DEFAULT_DATA_ONLY);
        public GenericCheckBoxMenuItem compressCheckBoxItem = new GenericCheckBoxMenuItem("Compress", AddFileTool.DEFAULT_COMPRESS);
        public GenericCheckBoxMenuItem explodeCheckBoxItem = new GenericCheckBoxMenuItem("Explode", AddFileTool.DEFAULT_EXPLODE_BEFORE_UPLOAD);
        public GenericCheckBoxMenuItem sendEmailOnFailureCheckBoxItem = new GenericCheckBoxMenuItem("Send Email On Failure", AddFileTool.DEFAULT_EMAIL_ON_FAILURE);
        public GenericCheckBoxMenuItem useUnspecifiedServersCheckBoxItem = new GenericCheckBoxMenuItem("Use Unspecified Servers", AddFileTool.DEFAULT_USE_UNSPECIFIED_SERVERS);
        public GenericCheckBoxMenuItem usePerformanceLogCheckBoxItem = new GenericCheckBoxMenuItem("Use Performance Log", AddFileTool.DEFAULT_USE_PERFORMANCE_LOG);
        public GenericMenuItem preferencesMenuItem = new GenericMenuItem("Preferences");
        private GenericMenuItem monitorMenuItem = new GenericMenuItem("Monitor");
        public GenericMenuItem helpWebpageMenuItem = new GenericMenuItem("Help Page");

        public AddFileToolWizardMenuBar() {
            homeButton.setVisible(GUIUtil.getAdvancedGUI() == null);
            homeButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            add(homeButton);

            {
                serversMenuItem.setMnemonic('s');
                serversMenuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Thread t = new Thread() {

                            @Override
                            public void run() {
                                serversFrame.setLocationRelativeTo(AddFileToolWizard.this);
                                serversFrame.setVisible(true);
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                    }
                });
                optionsMenu.add(serversMenuItem);

                stickyServersMenuItem.setMnemonic('t');
                stickyServersMenuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Thread t = new Thread("Launch sticky servers frame") {

                            @Override()
                            public void run() {
                                stickyServersFrame.setLocationRelativeTo(AddFileToolWizard.this);
                                stickyServersFrame.setVisible(true);
                            }
                        };
                        t.setDaemon(true);
                        t.setPriority(Thread.MIN_PRIORITY);
                        t.start();
                    }
                });
                optionsMenu.add(stickyServersMenuItem);

                annotationsMenuItem.setMnemonic('a');
                annotationsMenuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Thread t = new Thread() {

                            @Override
                            public void run() {
                                annotationFrame.setLocationRelativeTo(AddFileToolWizard.this);
                                annotationFrame.setVisible(true);
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                    }
                });
                optionsMenu.add(annotationsMenuItem);


                {
                    uploadDataOnlyCheckBoxItem.setMnemonic('d');
                    uploadParametersMenu.add(uploadDataOnlyCheckBoxItem);
                    uploadDataOnlyCheckBoxItem.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent event) {
                            summary.getAddFileTool().setDataOnly(uploadDataOnlyCheckBoxItem.isSelected());
                        }
                    });

                    compressCheckBoxItem.setMnemonic('c');
                    uploadParametersMenu.add(compressCheckBoxItem);
                    compressCheckBoxItem.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent event) {
                            summary.getAddFileTool().setCompress(compressCheckBoxItem.isSelected());
                        }
                    });

                    explodeCheckBoxItem.setMnemonic('e');
                    uploadParametersMenu.add(explodeCheckBoxItem);
                    explodeCheckBoxItem.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent event) {
                            summary.getAddFileTool().setExplodeBeforeUpload(explodeCheckBoxItem.isSelected());
                        }
                    });

                    sendEmailOnFailureCheckBoxItem.setMnemonic('m');
                    uploadParametersMenu.add(sendEmailOnFailureCheckBoxItem);
                    sendEmailOnFailureCheckBoxItem.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent event) {
                            summary.getAddFileTool().setEmailOnFailure(sendEmailOnFailureCheckBoxItem.isSelected());
                        }
                    });

                    useUnspecifiedServersCheckBoxItem.setMnemonic('u');
                    uploadParametersMenu.add(useUnspecifiedServersCheckBoxItem);
                    useUnspecifiedServersCheckBoxItem.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent event) {
                            summary.getAddFileTool().setUseUnspecifiedServers(useUnspecifiedServersCheckBoxItem.isSelected());
                        }
                    });

                    usePerformanceLogCheckBoxItem.setMnemonic('l');
                    uploadParametersMenu.add(usePerformanceLogCheckBoxItem);
                    usePerformanceLogCheckBoxItem.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent event) {
                            final boolean isUse = usePerformanceLogCheckBoxItem.isSelected();
                            boolean containsPerformanceLog = false;

                            // If already have one, don't attach another
                            for (AddFileToolListener aftl : summary.getAddFileTool().getListeners()) {
                                if (aftl instanceof AddFileToolPerformanceLog) {
                                    containsPerformanceLog = true;
                                    break;
                                }
                            }

                            // If use but don't have one
                            if (isUse && !containsPerformanceLog) {
                                try {
                                    File logFile = TempFileUtil.createTempFileWithName("aft-performance-gui-" + Text.getFormattedDateSimple(TimeUtil.getTrancheTimestamp()) + ".log");
                                    AddFileToolPerformanceLog log = new AddFileToolPerformanceLog(logFile);
                                    summary.getAddFileTool().addListener(log);
                                } catch (Exception e) {
                                    System.err.println(e.getClass() + " occured while trying to attach performance log as listener: " + e.getMessage());
                                    e.printStackTrace(System.err);
                                }
                            }

                            // If have one but don't want one, remove
                            if (!isUse && containsPerformanceLog) {
                                // Perchance there is more than one
                                Set<AddFileToolListener> listenersToRemove = new HashSet();
                                
                                // Identify the performance log(s)
                                for (AddFileToolListener aftl : summary.getAddFileTool().getListeners()) {
                                    if (aftl instanceof AddFileToolPerformanceLog) {
                                        listenersToRemove.add(aftl);
                                    }
                                }
                                
                                // Remove the performance log(s)
                                for (AddFileToolListener aftl : listenersToRemove) {
                                    summary.getAddFileTool().removeListener(aftl);
                                }
                            }
                        }
                    });
                }
                uploadParametersMenu.setMnemonic('l');
                optionsMenu.add(uploadParametersMenu);

                JSeparator separator = new JSeparator();
                separator.setForeground(Color.BLACK);
                optionsMenu.add(separator);

                preferencesMenuItem.setMnemonic('p');
                preferencesMenuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Thread t = new Thread() {

                            @Override
                            public void run() {
                                try {
                                    GUIUtil.getPreferencesFrame().show(PreferencesFrame.SHOW_UPLOADS);
                                    GUIUtil.getPreferencesFrame().setLocationRelativeTo(GUIUtil.getAdvancedGUI());
                                    GUIUtil.getPreferencesFrame().setVisible(true);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                    }
                });
                optionsMenu.add(preferencesMenuItem);

                optionsMenu.setMnemonic('o');
                add(optionsMenu);
            }

            {
                monitorMenuItem.setMnemonic('m');
                monitorMenuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Thread t = new Thread() {

                            @Override
                            public void run() {
                                if (!(summary.getAddFileTool().isExecuting() || summary.isFinished())) {
                                    GenericOptionPane.showMessageDialog(AddFileToolWizard.this, "The upload must be started before the monitor can be opened.", "Could Not Open Monitor", JOptionPane.WARNING_MESSAGE);
                                    return;
                                }
                                summary.showMonitor(AddFileToolWizard.this);
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                    }
                });
                logsMenu.add(monitorMenuItem);

                logsMenu.setMnemonic('l');
                add(logsMenu);
            }

            {
                helpWebpageMenuItem.setMnemonic('w');
                helpWebpageMenuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Thread t = new Thread() {

                            @Override
                            public void run() {
                                GUIUtil.displayURL("https://trancheproject.org/users/uploading.jsp");
                            }
                        };
                        t.start();
                    }
                });
                helpMenu.add(helpWebpageMenuItem);

                helpMenu.setMnemonic('h');
                add(helpMenu);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // configure Tranche network
        ConfigureTrancheGUI.load(args);

        // gui
        AddFileToolWizard aftw = new AddFileToolWizard();
        GUIUtil.centerOnScreen(aftw);
        aftw.setVisible(true);

        // read in arguments
        Set<Exception> exceptions = new HashSet<Exception>();
        for (int i = 1; i < args.length; i += 2) {
            try {
                if (args[i].equals("-t") || args[i].equals("--title")) {
                    aftw.step1Panel.title.setText(args[i + 1]);
                } else if (args[i].equals("-d") || args[i].equals("--description")) {
                    aftw.step1Panel.description.setText(args[i + 1]);
                } else if (args[i].equals("-L") || args[i].equals("--login")) {
                    try {
                        aftw.setUser(UserZipFileUtil.getUserZipFile(args[i + 1], args[i + 2]));
                    } catch (InvalidSignInException e) {
                        GenericOptionPane.showMessageDialog(aftw, e.getMessage(), "Could Not Sign In", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        i++;
                    }
                } else if (args[i].equals("-A") || args[i].equals("--annotation")) {
                    aftw.annotationFrame.getPanel().add(args[i + 1], args[i + 2]);
                    i++;
                }
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            aftw.ef.show(exceptions, aftw);
        }

        // lazy load slow stuff
        LazyLoadAllSlowStuffAfterGUIRenders.lazyLoad();
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        AddFileToolWizard.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static final boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    protected static final void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(AddFileToolWizard.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    protected static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
