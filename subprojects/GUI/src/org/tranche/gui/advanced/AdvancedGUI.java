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
package org.tranche.gui.advanced;

import org.tranche.gui.util.GUIUtil;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import org.tranche.ConfigureTranche;
import org.tranche.commons.DebugUtil;
import org.tranche.gui.ConfigureTrancheGUI;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GenericFrame;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.LazyLoadAllSlowStuffAfterGUIRenders;
import org.tranche.gui.Styles;
import org.tranche.gui.add.UploadCache;
import org.tranche.gui.get.DownloadCache;
import org.tranche.gui.project.ProjectPool;
import org.tranche.users.InvalidSignInException;
import org.tranche.users.UserZipFile;
import org.tranche.users.UserZipFileUtil;
import org.tranche.util.TestUtil;

/**
 * The entry point for the primary GUI.
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class AdvancedGUI extends GenericFrame implements ClipboardOwner {

    public TopPanel topPanel;
    private LogoPanel logoPanel;
    public LeftPanel leftPanel;
    public BottomPanel bottomPanel;
    public MainPanel mainPanel;
    public ErrorFrame ef = new ErrorFrame();

    public AdvancedGUI() {
        super(ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_NAME));

        GUIUtil.setAdvancedGUI(AdvancedGUI.this);

        // set the default size
        setSize(900, 600);

        // close everything when this is closed
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            // We want modules to have the same permissions as the Tranche
            // application (e.g., so can launch web page in browser)
            // These jars will not be signed.
            System.setSecurityManager(null);
        } catch (SecurityException se) {
            System.err.println(se.getClass().getSimpleName() + " occurred while unsetting the security manager for modules: " + se.getMessage());
        } catch (NullPointerException npe) {
            // This was an old JVM bug, and should have been fixed.
        }

        // create the relevant panels
        topPanel = new TopPanel();
        logoPanel = new LogoPanel();
        leftPanel = new LeftPanel();
        mainPanel = new MainPanel();
        bottomPanel = new BottomPanel();

        // set the layout to a border layout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // add the top panel
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        add(topPanel, gbc);

        // add the logo panel
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0;
        logoPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.COLOR_TRIM));
        add(logoPanel, gbc);

        // add the left panel
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 0;
        gbc.weighty = 2;
        GenericScrollPane leftPanelScroller = new GenericScrollPane(leftPanel);
        leftPanelScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        leftPanelScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Dimension d = new Dimension(150, leftPanel.getHeight());
        leftPanelScroller.setSize(d);
        leftPanelScroller.setPreferredSize(d);
        leftPanelScroller.setMaximumSize(d);
        leftPanelScroller.setMinimumSize(d);
        leftPanelScroller.setBorder(Styles.BORDER_NONE);
        add(leftPanelScroller, gbc);

        // add the main panel
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 2;
        add(mainPanel, gbc);

        // add the bottom panel
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.weighty = 0;
        add(bottomPanel, gbc);
    }

    /**
     *Sets and shows the user that is signed in.
     *@param uzf The decrypted user zip file of the user you want to sign in.
     */
    public void setUser(UserZipFile uzf) {
        try {
            topPanel.setUser(uzf);
        } catch (Exception e) {
            ef.show(e, this);
        }
    }

    /**
     *@return The user that is currently logged in. If no user is logged in, null is returned.
     */
    public UserZipFile getUser() {
        return topPanel.getUser();
    }

    // the following are for the clipboardowner abstract
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }

    /**
     *
     */
    private static void printUsage() {
        System.out.println();
        System.out.println("USAGE");
        System.out.println("    [FLAGS / PARAMETERS]");
        System.out.println();
        System.out.println("DESCRIPTION");
        System.out.println("    Launches the main GUI.");
        System.out.println();
        System.out.println("MEMORY ALLOCATION");
        System.out.println("    To allocate 512 MB of memory to the process, you should use the JVM option: java -Xmx512m");
        System.out.println();
        System.out.println("PRINT AND EXIT FLAGS");
        System.out.println("    Use one of these to print some information and exit. Usage: java -jar <JAR> [PRINT AND EXIT FLAG]");
        System.out.println();
        System.out.println("    -h, --help          Print usage and exit.");
        System.out.println("    -V, --version       Print version number and exit.");
        System.out.println();
        System.out.println("FLAGS");
        System.out.println("    -d, --debug         If you have problems, you can use this option to print debugging information. These will help use solve problems if you can repeat your problem with this flag on.");
        System.out.println();
        System.out.println("PARAMETERS");
        System.out.println("    -L,  --login        Values: two strings.        The username and password for your sign in (e.g., \"-L Augie notmypassword\").");
        System.out.println();
        System.out.println("RETURN CODES");
        System.out.println("    0: Exited normally");
        System.out.println("    1: Unknown error");
        System.out.println("    2: Problem with argument(s)");
        System.out.println("    3: Known error");
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        try {
            // must have a config
            if (args.length == 0) {
                printUsage();
                if (!TestUtil.isTesting()) {
                    System.exit(2);
                } else {
                    return;
                }
            }

            // first debug
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-d") || args[i].equals("--debug")) {
                    DebugUtil.setDebug(AdvancedGUI.class, true);
                } else if (args[i].equals("-n") || args[i].equals("--buildnumber") || args[i].equals("-V") || args[i].equals("--version")) {
                    System.out.println("Tranche, build #@buildNumber");
                    if (!TestUtil.isTesting()) {
                        System.exit(0);
                    } else {
                        return;
                    }
                } else if (args[i].equals("-h") || args[i].equals("--help")) {
                    printUsage();
                    if (!TestUtil.isTesting()) {
                        System.exit(0);
                    } else {
                        return;
                    }
                } else if (args[i].equals("-L") || args[i].equals("--login")) {
                    i += 2;
                }
            }

            // configure
            try {
                ConfigureTrancheGUI.load(args);
            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                DebugUtil.debugErr(AdvancedGUI.class, e);
                if (!TestUtil.isTesting()) {
                    System.exit(2);
                } else {
                    return;
                }
            }

            openNewInstance(args);
        } catch (Exception e) {
            DebugUtil.debugErr(AdvancedGUI.class, e);
            if (!TestUtil.isTesting()) {
                System.exit(1);
            } else {
                return;
            }
        }
    }

    public static void openNewInstance(final String[] args) throws Exception {
        // set the process name
        try {
            System.setProperty("dock:name", "Tranche");
        } catch (Exception e) {
            DebugUtil.debugErr(AdvancedGUI.class, e);
        }

        // Create the GUI
        SwingUtilities.invokeAndWait(new Runnable() {

            public void run() {
                final AdvancedGUI sf = new AdvancedGUI();

                // make visible
                GUIUtil.centerOnScreen(sf);
                sf.setVisible(true);

                // legal
                Thread t = new Thread("Legal Intro") {

                    @Override
                    public void run() {
                        GenericOptionPane.showMessageDialog(sf, "WARNING: Please read before continuing and click OK if you accept.\n\n"
                                + "The purpose of Tranche is to enable legitimate scientific data sharing.\n"
                                + "By continuing to use this software, you accept and declare that you will not\n"
                                + "be using it to engage in copyright infringement or any other illegal activity.", "Legal", JOptionPane.QUESTION_MESSAGE);
                    }
                };
                t.setDaemon(true);
                t.start();

                // read in arguments
                for (int i = 1; i < args.length; i++) {
                    if (args[i].equals("-L") || args[i].equals("--login")) {
                        try {
                            sf.setUser(UserZipFileUtil.getUserZipFile(args[i + 1], args[i + 2]));
                        } catch (InvalidSignInException e) {
                            GenericOptionPane.showMessageDialog(sf, e.getMessage(), "Could Not Sign In", JOptionPane.ERROR_MESSAGE);
                            DebugUtil.debugErr(AdvancedGUI.class, e);
                        } catch (Exception e) {
                            DebugUtil.debugErr(AdvancedGUI.class, e);
                        } finally {
                            i += 2;
                        }
                    }
                }
            }
        });

        // Start lazy loading (most work in building project cache)
        Thread workerThread = new Thread("Lazy Load All Slow Stuff After GUI Renders") {

            @Override
            public void run() {
                // adds the static objects to the lazy load pool
                new DownloadCache();
                new UploadCache();
                new ProjectPool();
                LazyLoadAllSlowStuffAfterGUIRenders.lazyLoad();
            }
        };
        workerThread.setDaemon(true);
        workerThread.start();
    }
}
