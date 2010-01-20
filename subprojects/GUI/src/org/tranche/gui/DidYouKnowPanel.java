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
package org.tranche.gui;

import org.tranche.gui.util.GUIUtil;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/*
 * @author James "Augie" Hill - augman85@gmail.com
 */
import javax.swing.SwingUtilities;

public class DidYouKnowPanel extends JPanel {

    public static final int DEFAULT_WIDTH = 450,  DEFAULT_HEIGHT = 120,  DEFAULT_WAIT_TIME = 30;
    private int showingMessage = 0;
    private boolean isPaused = false;
    private ImageIcon pauseIcon,  resumeIcon,  lightBulbIcon;
    private JButton secondsButton = new JButton(String.valueOf(DEFAULT_WAIT_TIME)),  backButton = new JButton("Previous"),  forwardButton = new JButton("Next"),  urlButton = new JButton();
    private DisplayTextArea displayTextArea = new DisplayTextArea();
    private LinkedList<DidYouKnowMessage> messageList = new LinkedList<DidYouKnowMessage>();
    private Thread messageSwitchingThread;

    // add some default messages


    {
        List<DidYouKnowMessage> messageSet = new ArrayList<DidYouKnowMessage>();
        messageSet.add(new DidYouKnowMessage("Have you seen the Tranche Project homepage?", "http://tranche.proteomecommons.org", "Tranche Project Homepage"));
        messageSet.add(new DidYouKnowMessage("Check out the Tranche Project Users Group on Google Groups to be able to read FAQ's or ask your own questions.", "http://groups.google.com/group/proteomecommons-tranche-users?lnk=srg&hl=en", "Tranche Project User Group"));
        messageSet.add(new DidYouKnowMessage("Tranche supports the Science Commons CC0 license for uploads."));

        // randomize the messages into the message list
        Random generator = new Random();
        int originalSetSize = messageSet.size();
        for (int i = 0; i < originalSetSize; i++) {
            messageList.add(messageSet.remove(generator.nextInt(messageSet.size())));
        }
    }

    public DidYouKnowPanel() {
        setLayout(new BorderLayout());
        setBackground(Styles.COLOR_PANEL_BACKGROUND);

        // load the images into the panels and buttons
        try {
            lightBulbIcon = new ImageIcon(ImageIO.read(DidYouKnowPanel.class.getResourceAsStream("/org/tranche/gui/image/dialog-information-32x32.gif")));
            backButton.setIcon(new ImageIcon(ImageIO.read(DidYouKnowPanel.class.getResourceAsStream("/org/tranche/gui/image/media-seek-backward-16x16.gif"))));
            forwardButton.setIcon(new ImageIcon(ImageIO.read(DidYouKnowPanel.class.getResourceAsStream("/org/tranche/gui/image/media-seek-forward-16x16.gif"))));
            urlButton.setIcon(new ImageIcon(ImageIO.read(DidYouKnowPanel.class.getResourceAsStream("/org/tranche/gui/image/internet-web-browser-16x16.gif"))));
            pauseIcon = new ImageIcon(ImageIO.read(DidYouKnowPanel.class.getResourceAsStream("/org/tranche/gui/image/pause.gif")));
            secondsButton.setIcon(pauseIcon);
            resumeIcon = new ImageIcon(ImageIO.read(DidYouKnowPanel.class.getResourceAsStream("/org/tranche/gui/image/play.gif")));
        } catch (Exception e) {
        }

        // put all the regular stuff in a main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Styles.COLOR_TRANSPARENT);
        mainPanel.setLayout(new BorderLayout());
        {
            // create and add the did you know panel
            JLabel didYouKnowLabel = new GenericLabel("Did you know...");
            didYouKnowLabel.setFont(Styles.FONT_28PT_TIMES_BOLD);
            didYouKnowLabel.setIcon(lightBulbIcon);
            didYouKnowLabel.setIconTextGap(5);
            mainPanel.add(didYouKnowLabel, BorderLayout.NORTH);

            // create the panel with the text
            JPanel textPanel = new JPanel();
            textPanel.setBackground(Styles.COLOR_TRANSPARENT);
            textPanel.setLayout(new BorderLayout());
            {
                textPanel.add(displayTextArea, BorderLayout.CENTER);
                textPanel.add(urlButton, BorderLayout.SOUTH);
            }
            mainPanel.add(textPanel, BorderLayout.CENTER);

            // all the buttons at the bottom
            JPanel buttonPanel = new JPanel();
            buttonPanel.setBackground(Styles.COLOR_TRANSPARENT);
            buttonPanel.setLayout(new BorderLayout());
            {
                buttonPanel.add(backButton, BorderLayout.WEST);
                buttonPanel.add(secondsButton, BorderLayout.CENTER);
                buttonPanel.add(forwardButton, BorderLayout.EAST);
            }
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        }
        add(mainPanel, BorderLayout.CENTER);

        // create and add the right image panel
        //add(rightImagePanel, BorderLayout.EAST);

        // set the first message to be shown
        displayTextArea.setText(messageList.getFirst().message);
        if (messageList.getFirst().urlName.equals("")) {
            urlButton.setText(messageList.getFirst().url);
        } else {
            urlButton.setText(messageList.getFirst().urlName);
        }
        urlButton.setToolTipText(messageList.getFirst().url);
        if (urlButton.getText().equals("")) {
            urlButton.setVisible(false);
        } else {
            urlButton.setVisible(true);
        }

        // start up the thread for changing the messages
        messageSwitchingThread = new Thread("Did You Know Message Switcher") {

            int value;

            @Override
            public void run() {
                while (messageList.size() > 0) {
                    if (!isPaused) {
                        value = Integer.valueOf(secondsButton.getText());
                        if (value == 0) {
                            if (showingMessage == messageList.size() - 1) {
                                showingMessage = 0;
                            } else {
                                showingMessage++;
                            }
                            displayTextArea.setText(messageList.get(showingMessage).message);
                            if (!messageList.get(showingMessage).urlName.equals("")) {
                                urlButton.setText(messageList.get(showingMessage).urlName);
                            } else {
                                urlButton.setText(messageList.get(showingMessage).url);
                            }
                            if (urlButton.getText().equals("")) {
                                urlButton.setVisible(false);
                            } else {
                                urlButton.setVisible(true);
                            }
                            value = 30;
                        } else {
                            value--;
                        }
                        secondsButton.setText(String.valueOf(value));
                        repaintNow();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                }
            }
        };
        messageSwitchingThread.setDaemon(true);
        messageSwitchingThread.setPriority(Thread.MIN_PRIORITY);
        messageSwitchingThread.start();

        // set the look and feel
        {
            displayTextArea.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 10));
            displayTextArea.setFont(Styles.FONT_11PT_ITALIC);
            displayTextArea.setBackground(getBackground());
            displayTextArea.setOpaque(true);

            urlButton.setIconTextGap(5);
            urlButton.setFocusable(false);
            urlButton.setBackground(getBackground());
            urlButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));

            secondsButton.setFocusable(false);
            secondsButton.setBackground(getBackground());
            secondsButton.setBorder(Styles.BORDER_NONE);
            secondsButton.setIconTextGap(10);
            secondsButton.setFont(Styles.FONT_11PT);
            secondsButton.setHorizontalTextPosition(SwingConstants.LEADING);

            backButton.setFocusable(false);
            backButton.setBackground(getBackground());
            backButton.setFont(Styles.FONT_11PT);
            backButton.setBorder(Styles.BORDER_NONE);
            backButton.setIconTextGap(5);

            forwardButton.setHorizontalTextPosition(SwingConstants.LEADING);
            forwardButton.setFocusable(false);
            forwardButton.setBackground(getBackground());
            forwardButton.setBorder(Styles.BORDER_NONE);
            forwardButton.setFont(Styles.FONT_11PT);
            forwardButton.setIconTextGap(5);
        }

        // set the behavior for the buttons
        secondsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                setPaused(!isPaused);

            }
        });
        forwardButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (showingMessage == messageList.size() - 1) {
                    showingMessage = 0;
                } else {
                    showingMessage++;
                }
                displayTextArea.setText(messageList.get(showingMessage).message);
                if (!messageList.get(showingMessage).urlName.equals("")) {
                    urlButton.setText(messageList.get(showingMessage).urlName);
                } else {
                    urlButton.setText(messageList.get(showingMessage).url);
                }
                if (urlButton.getText().equals("")) {
                    urlButton.setVisible(false);
                } else {
                    urlButton.setVisible(true);
                }
                secondsButton.setText(String.valueOf(DEFAULT_WAIT_TIME));
            }
        });
        backButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (showingMessage == 0) {
                    showingMessage = messageList.size() - 1;
                } else {
                    showingMessage--;
                }
                displayTextArea.setText(messageList.get(showingMessage).message);
                if (!messageList.get(showingMessage).urlName.equals("")) {
                    urlButton.setText(messageList.get(showingMessage).urlName);
                } else {
                    urlButton.setText(messageList.get(showingMessage).url);
                }
                if (urlButton.getText().equals("")) {
                    urlButton.setVisible(false);
                } else {
                    urlButton.setVisible(true);
                }
                secondsButton.setText(String.valueOf(DEFAULT_WAIT_TIME));
            }
        });
        urlButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        GUIUtil.displayURL(messageList.get(showingMessage).url);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
    }

    public void repaintNow() {
        if (SwingUtilities.isEventDispatchThread()) {
            DidYouKnowPanel.super.paintImmediately(getBounds());
        } else {
            Thread t = new Thread("Repaint Did You Know Panel") {

                @Override
                public void run() {
                    DidYouKnowPanel.super.paintImmediately(getBounds());
                }
            };
            t.setDaemon(true);
            try {
                SwingUtilities.invokeLater(t);
            } catch (Exception e) {
            }
        }
    }

    public void setPaused(boolean isPaused) {
        this.isPaused = isPaused;
        if (isPaused) {
            secondsButton.setIcon(resumeIcon);
        } else {
            secondsButton.setIcon(pauseIcon);
        }
    }

    public void addMessage(String message) {
        addMessage(message, null);
    }

    public void addMessage(String message, String url) {
        addMessage(message, url, null);
    }

    public void addMessage(String message, String url, String urlName) {
        messageList.add(new DidYouKnowMessage(message, url, urlName));
    }

    public class DidYouKnowMessage {

        public String message = "",  url = "",  urlName = "";

        public DidYouKnowMessage(String message) {
            this(message, "");
        }

        public DidYouKnowMessage(String message, String url) {
            this(message, url, "");
        }

        public DidYouKnowMessage(String message, String url, String urlName) {
            if (message != null) {
                this.message = message;
            }
            if (url != null) {
                this.url = url;
            }
            if (urlName != null) {
                this.urlName = urlName;
            }
        }
    }
}
