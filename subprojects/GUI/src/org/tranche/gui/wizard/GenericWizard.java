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
package org.tranche.gui.wizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.GenericFrame;
import org.tranche.gui.GenericRoundedButton;
import org.tranche.gui.ImagePanel;
import org.tranche.gui.Styles;
import org.tranche.gui.TrancheLogoPanel;
import org.tranche.time.TimeUtil;

/**
 * <p>Create a wizard that can be used for any task.</p>
 * <p>You can add GenericWizardSteps to create a wizard.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GenericWizard extends GenericFrame {

    private static final boolean isDebug = false;
    private int currentStep = 0;
    private boolean isNextAvailable = false;
    private LinkedList<GenericWizardStep> steps = new LinkedList<GenericWizardStep>();
    /**
     * <ul>
     *  <li>bannerLogo: Long space available over the title.</li>
     *  <li>additionalIcon: Roughtly-size of fractal, space to west of fractal.</li>
     * </ul>
     */
    private JPanel bannerLogo,  additionalIcon;
    private TrancheLogoPanel trancheLogo = new TrancheLogoPanel();


    {
        trancheLogo.lazyLoad();
    }
    private ImagePanel blankIconPanel = null;
    private String title;
    private String wizardTitle = "";
    private String logoUrl = null;
    private String logoToolTipText = null;
    private String finalButtonLabel = "Close the " + this.wizardTitle;
    private JButton prevButton = new GenericRoundedButton("Previous..."),  nextButton = new GenericRoundedButton("Next...");
    private JMenuBar menuBar = null;

    /**
     *
     * @param title The title for the wizard.
     * @param dimension The dimension for the wizard.
     */
    public GenericWizard(String title, Dimension dimension) {
        this(title, dimension, null);
    }

    /**
     *
     * @param title The title for the wizard.
     * @param dimension The dimension for the wizard.
     * @param menuBar An optional menu bar for the wizard
     */
    public GenericWizard(String title, Dimension dimension, JMenuBar menuBar) {
        super(title);
        this.wizardTitle = title;
        this.finalButtonLabel = "Close the " + this.wizardTitle;
        this.title = title;
        this.menuBar = menuBar;

        try {
            blankIconPanel = new ImagePanel(ImageIO.read(GenericWizard.class.getResourceAsStream("/org/tranche/gui/image/blank.gif")));
        } catch (Exception e) {
        }

        // prepare buttons for activity
        prevButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    public void run() {
                        if (steps.get(currentStep).getPanel().onPrevious()) {

                            currentStep--;
                            printTracer("Previous button clicked, at step #" + currentStep);

                            // Safely go back. Shouldn't be able to go back to negative index.
                            refresh();
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        prevButton.setFont(Styles.FONT_16PT);
        nextButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    public void run() {
                        try {
                            if (steps.get(currentStep).getPanel().onNext()) {

                                currentStep++;
                                printTracer("Next button clicked, at step #" + currentStep);

                                // If next step, move to it
                                if (currentStep < steps.size()) {
                                    refresh();
                                } else {
                                    // We're at the end, dispose
                                    dispose();

                                    // What's the policy? If set to exit, do that
                                    if (getDefaultCloseOperation() == JFrame.EXIT_ON_CLOSE) {
                                        System.exit(0);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        nextButton.setFont(Styles.FONT_16PT_BOLD);

        // If there's an advanced gui, just close. Else exit.
        if (GUIUtil.getAdvancedGUI() != null) {
            setDefaultCloseOperation(GenericFrame.DISPOSE_ON_CLOSE);
        } else {
            setDefaultCloseOperation(GenericFrame.EXIT_ON_CLOSE);
        }

        // Size and position
        setSize(dimension);
        setPreferredSize(dimension);
        setMinimumSize(dimension);

        // center on screen by default
        GUIUtil.centerOnScreen(this);
        // set the look and feel
        setLayout(new BorderLayout());
    }

    public void nextStep(int forwardStepCount) {
        for (int i = 1; i <= forwardStepCount && i <= steps.size(); i++) {
            int step = currentStep;
            nextStep();

            // wait for the step forward to occur - max at 1 second
            long startTime = TimeUtil.getTrancheTimestamp();
            while (currentStep < step + 1 && TimeUtil.getTrancheTimestamp() - startTime < 1000) {
                try {
                    Thread.sleep(20);
                } catch (Exception e) {
                }
            }
        }
    }

    public void nextStep() {
        nextButton.doClick();
    }

    public void setLogoUrl(String url) {
        logoUrl = url;
        refresh();
    }

    public void setLogoToolTipText(String toolTipText) {
        logoToolTipText = toolTipText;
        refresh();
    }

    public void refresh() {
        if (steps.size() > 0 && currentStep < steps.size()) {
            prepareStep();
        }

        getContentPane().removeAll();

        // make a panel consisting of the menu bar and the masthead with images
        JPanel topPanel = new JPanel();
        topPanel.setBackground(Color.WHITE);
        topPanel.setLayout(new BorderLayout());
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Styles.COLOR_TRIM));
        {
            // add the menu bar
            if (menuBar != null) {
                topPanel.add(menuBar, BorderLayout.NORTH);
            }

            JPanel topImagesPanel = new JPanel();
            topImagesPanel.setLayout(new BorderLayout());
            {
                // add the logo panel
                JPanel logoPanel = new JPanel();
                logoPanel.setLayout(new BorderLayout());
                {
                    if (bannerLogo != null) {
                        if (logoUrl != null) {
                            bannerLogo.addMouseListener(new MouseAdapter() {

                                public void mouseClicked(MouseEvent e) {
                                    Thread t = new Thread() {

                                        public void run() {
                                            GUIUtil.displayURL(logoUrl);
                                        }
                                    };
                                    t.setDaemon(true);
                                    t.start();
                                }

                                public void mouseEntered(MouseEvent e) {
                                    setCursor(Cursor.HAND_CURSOR);
                                }

                                public void mouseExited(MouseEvent e) {
                                    setCursor(Cursor.DEFAULT_CURSOR);
                                }
                            });
                        }
                        if (logoToolTipText != null) {
                            bannerLogo.setToolTipText(logoToolTipText);
                        }
                        logoPanel.add(bannerLogo, BorderLayout.CENTER);
                    } else {
                        JPanel panel = new JPanel();
                        panel.setBackground(Color.WHITE);
                        logoPanel.add(panel, BorderLayout.CENTER);
                    }

                    // Put text label headers in a panel and add panel
                    {
                        JPanel headerPanel = new JPanel();
                        headerPanel.setLayout(new GridBagLayout());
                        GridBagConstraints gbc = new GridBagConstraints();

                        JLabel stepsLabel = new GenericLabel("Step " + (currentStep + 1) + " of " + steps.size());
                        stepsLabel.setBackground(Color.WHITE);
                        stepsLabel.setOpaque(true);
                        stepsLabel.setFont(Styles.FONT_16PT_BOLD);
                        stepsLabel.setForeground(Color.LIGHT_GRAY);
                        stepsLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

                        gbc.weightx = 1;
                        gbc.weighty = 1;
                        gbc.fill = GridBagConstraints.BOTH;
                        gbc.gridwidth = GridBagConstraints.RELATIVE;
                        gbc.gridx = 0;
                        gbc.gridy = 0;

                        headerPanel.add(stepsLabel, gbc);

                        JLabel titleLabel = new GenericLabel(getTitle());
                        titleLabel.setBackground(Color.WHITE);
                        titleLabel.setOpaque(true);
                        titleLabel.setForeground(Color.BLACK);
                        titleLabel.setFont(Styles.FONT_24PT_BOLD);
                        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 2, 0));

                        gbc.weightx = 1;
                        gbc.weighty = 0;
                        gbc.fill = GridBagConstraints.HORIZONTAL;
                        gbc.gridwidth = GridBagConstraints.RELATIVE;
                        gbc.gridx = 0;
                        gbc.gridy = 1;

                        headerPanel.add(titleLabel, gbc);

                        logoPanel.add(headerPanel, BorderLayout.SOUTH);
                    }
                }
                topImagesPanel.add(logoPanel, BorderLayout.CENTER);

                // Add the Tranche fractal logo and other logo (if one)
                JPanel iconsPanel = new JPanel();
                iconsPanel.setLayout(new BorderLayout());
                {
                    if (additionalIcon != null) {
                        // Wrap icon so can set an inset (padding)
                        JPanel iconWrapper = GUIUtil.wrapComponentInPanel(additionalIcon, 0, 8, 0, 0);
                        iconWrapper.setBackground(Color.WHITE);
                        iconsPanel.add(iconWrapper, BorderLayout.WEST);
                    } else {
                        // Wrap icon so can set an inset (padding)
                        JPanel iconWrapper = GUIUtil.wrapComponentInPanel(blankIconPanel, 0, 8, 0, 0);
                        iconWrapper.setBackground(Color.WHITE);
                        iconsPanel.add(iconWrapper, BorderLayout.WEST);
                    }
                    // Wrap Tranche fractal logo so can set an inset (padding)
                    JPanel trancheLogoWrapper = GUIUtil.wrapComponentInPanel(trancheLogo, 0, 8, 0, 8);
                    trancheLogoWrapper.setBackground(Color.WHITE);
                    iconsPanel.add(trancheLogoWrapper, BorderLayout.CENTER);
                }
                topImagesPanel.add(iconsPanel, BorderLayout.EAST);
            }
            topPanel.add(topImagesPanel, BorderLayout.CENTER);
        }
        add(topPanel, BorderLayout.NORTH);

        // add the current step
        if (steps.size() > 0 && currentStep < steps.size()) {
            add(steps.get(currentStep).getPanel(), BorderLayout.CENTER);
        } else {
            add(new JPanel(), BorderLayout.CENTER);
        }

        // add the buttons on the bottom
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(Styles.COLOR_PANEL_BACKGROUND);
        bottomPanel.setLayout(new GridBagLayout());
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(5, 5, 5, 2);
            gbc.weightx = 0.5;
            gbc.weighty = 1;
            bottomPanel.add(prevButton, gbc);

            // Add a spacer strut
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets = new Insets(0, 0, 0, 0);
            gbc.weightx = 0;
            gbc.weighty = 0;

            bottomPanel.add(Box.createVerticalStrut(50), gbc);

            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(5, 2, 5, 5);
            gbc.weightx = 0.5;
            gbc.weighty = 1;
            bottomPanel.add(nextButton, gbc);
        }

        add(bottomPanel, BorderLayout.SOUTH);

        validate();
        repaint();
    }

    public void setMenuBar(JMenuBar menuBar) {
        this.menuBar = menuBar;
        refresh();
    }

    /**
     * Add a step to the wizard to the end.
     */
    public void addStep(GenericWizardStep step) {
        addStep(steps.size(), step);
    }

    /**
     * Add a step to the wizard at a specified position.
     * @param position
     * @param step
     */
    public void addStep(int position, GenericWizardStep step) {
        steps.add(position, step);

        // Doubly-link to previous step if one
        if (position < this.steps.size() - 1) {
            step.setNext(steps.get(position + 1));
            steps.get(position + 1).setPrevious(step);
        }

        // Double-link to next step if one
        if (position != 0) {
            step.setPrevious(steps.get(position - 1));
            steps.get(position - 1).setNext(step);
        }

        printTracer("Added \"" + step.getTitle() + "\" at position #" + position);
    }

    public GenericWizardStep getStep(int step) {
        if (step < 1 || step > steps.size()) {
            return null;
        }
        return steps.get(step - 1);
    }

    public void setLogo(JPanel logo) {
        this.bannerLogo = logo;
        refresh();
    }

    /**
     * Returns the title of the wizard.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title of the wizard.
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
        super.setTitle(title);
    }

    /**
     *
     */
    private void prepareStep() {
        // Set the title
        setTitle(steps.get(currentStep).getTitle());

        printTracer("step.isNextDisabled for \"" + steps.get(currentStep).getTitle() + "\": " + steps.get(currentStep).isNextDisabled());
        printTracer("step.getNext() = " + steps.get(currentStep).getNext());

        // Set next button disabled and label
        nextButton.setEnabled(!steps.get(currentStep).isNextDisabled());
        //nextButton.setVisible(!steps.get(currentStep).isNextDisabled());

        // Special case: if at end of tasks, label should be different
        if (currentStep == steps.size() - 1) {
            nextButton.setText(getFinalButtonLabel());
        } else {
            printTracer("Setting next label to: " + steps.get(currentStep).getNext().getButtonLabel());
            nextButton.setText("Continue to " + steps.get(currentStep).getNext().getButtonLabel());
        }

        boolean isPrevDisabled = steps.get(currentStep).isBackDisabled() || currentStep == 0;

        // Set prev button disabled and label
        prevButton.setEnabled(!isPrevDisabled);
        prevButton.setVisible(!isPrevDisabled);

        if (!isPrevDisabled && steps.get(currentStep).getPrevious() != null) {
            printTracer("Setting prev label to: " + steps.get(currentStep).getPrevious().getButtonLabel());
            prevButton.setText("Return to " + steps.get(currentStep).getPrevious().getButtonLabel());
        }

        // See if next is selectable by default
        if (!steps.get(currentStep).isNextAvailableByDefault()) {
            nextButton.setEnabled(false);
        }

        // Are the icons different for step?
        if (steps.get(currentStep).getIcon() != null) {
            additionalIcon = steps.get(currentStep).getIcon();
        } else {
            additionalIcon = null;
        }

        // Call the onLoad hook
        Thread t = new Thread("onLoad hook") {

            public void run() {
                steps.get(currentStep).getPanel().onLoad();
            }
        };
        t.setDaemon(true);
        t.start();
    }

    /**
     * Returns true when "next" is an option for the client.
     */
    public boolean isNextAvailable() {
        return isNextAvailable;
    }

    /**
     * Should be called within a step when "next" button is available.
     * The author of the step is responsible to set this function.
     * @param isNextAvailable
     */
    public void setIsNextAvailable(boolean isNextAvailable) {
        // Enable or disable the button
        if (isNextAvailable) {
            nextButton.setEnabled(isNextAvailable);
        }
        this.isNextAvailable = isNextAvailable;
    }

    /**
     * Enable and disable next button at will.
     * Note that if next is disabled by default, you need to enable it at some point.
     * @param enable
     */
    public void setEnableNextButtonNow(boolean enable) {
        this.nextButton.setEnabled(enable);
        steps.get(currentStep).setIsNextDisabled(!enable);
        printTracer("Enabled next button: " + enable);
    }

    private static void printTracer(String msg) {
        if (isDebug) {
            System.out.println("GENERIC_WIZARD> " + msg);
        }
    }

    public String getWizardTitle() {
        return wizardTitle;
    }

    public String getFinalButtonLabel() {
        return finalButtonLabel;
    }

    public void setFinalButtonLabel(String finalButtonLabel) {
        this.finalButtonLabel = finalButtonLabel;
        refresh();
    }
}
