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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.BindException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import org.tranche.gui.util.GUIUtil;
import org.tranche.logs.LogUtil;
import org.tranche.server.PropagationExceptionWrapper;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ErrorFrame extends GenericFrame {

    // whether or not to show the details of the exceptions
    private boolean showDetails = false;
    private Set<Exception> exceptions;
    private JTextArea textArea = new GenericTextArea();
    private final JTextArea comment = new GenericTextArea();

    public ErrorFrame() {
        this(new HashSet<Exception>());
    }

    public ErrorFrame(Collection<PropagationExceptionWrapper> collection) {
        super("Error");
        setPropagated(collection);
        init();
    }

    public ErrorFrame(Exception exception) {
        super("Error");
        exceptions = new HashSet<Exception>();
        exceptions.add(exception);
        init();
    }

    public ErrorFrame(Set<Exception> exceptions) {
        super("Error");
        this.exceptions = exceptions;
        init();
    }

    public void init() {
        setSize(500, 300);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(GenericFrame.DISPOSE_ON_CLOSE);

        // the error notification message and icon
        {
            Icon errorIcon = null;
            try {
                errorIcon = new ImageIcon(ImageIO.read(ErrorFrame.class.getResourceAsStream("/org/tranche/gui/image/error.gif")));
            } catch (Exception e) {
                e.printStackTrace();
            }
            JLabel errorLabel = new GenericLabel(" There was an error.", errorIcon, JLabel.LEFT);
            errorLabel.setFont(Styles.FONT_TITLE);
            add(errorLabel, BorderLayout.NORTH);
        }

        // the text area
        {
            textArea.setBackground(getBackground());
            textArea.setEditable(false);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setText("The following errors occurred:\n\n");
            textArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            for (Exception exception : exceptions) {
                textArea.append("  -  " + exception.getMessage() + "\n");
            }
            GenericScrollPane pane = new GenericScrollPane(textArea);
            pane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.BLACK));
            pane.setVerticalScrollBar(new GenericScrollBar());
            add(pane, BorderLayout.CENTER);
        }

        // panel for buttons
        {
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            {
                JPanel commentPanel = new JPanel();
                commentPanel.setLayout(new BorderLayout());
                commentPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));

                JLabel commentLabel = new GenericLabel("Comment: ");
                commentLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                commentPanel.add(commentLabel, BorderLayout.WEST);

                comment.setWrapStyleWord(true);
                comment.setLineWrap(true);
                comment.setText("Please leave a comment describing what occurred.");
                comment.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                comment.setRows(2);
                comment.setFont(Styles.FONT_12PT_MONOSPACED);
                comment.addCaretListener(new CaretListener() {

                    public void caretUpdate(CaretEvent ce) {
                        if (comment.getText().equals("Please leave a comment describing what occurred.")) {
                            comment.setText("");
                        }
                    }
                });

                GenericScrollPane pane = new GenericScrollPane(comment);
                pane.setVerticalScrollBar(new GenericScrollBar());
                pane.setHorizontalScrollBar(new GenericScrollBar(GenericScrollBar.HORIZONTAL));
                pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                commentPanel.add(pane, BorderLayout.CENTER);

                gbc.fill = GridBagConstraints.BOTH;
                gbc.weightx = 1;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                panel.add(commentPanel, gbc);
            }

            JButton doNothingButton = new GenericButton("Do Nothing");
            doNothingButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(10, 0, 10, 0);
            panel.add(doNothingButton, gbc);

            JButton sendErrorButton = new GenericButton("Send Error Report");
            sendErrorButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    ErrorFrame.this.setVisible(false);
                    Thread t = new Thread("Log Errors") {

                        @Override
                        public void run() {
                            String commentString = comment.getText();
                            if (commentString.equals("Please leave a comment describing what occurred.")) {
                                commentString = "";
                            }

                            // spawn a thread to report the error
                            LogUtil.logError(exceptions, commentString, GUIUtil.getUser());
                            dispose();
                        }
                    };
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.start();
                }
            });
            panel.add(sendErrorButton, gbc);

            final JButton showDetailsButton = new GenericButton("Show Detailed View");
            showDetailsButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            if (!ErrorFrame.this.showDetails) {
                                showDetailsButton.setText("Show Simple View");
                            } else {
                                showDetailsButton.setText("Show Detailed View");
                            }
                            setDetails(!ErrorFrame.this.showDetails);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            panel.add(showDetailsButton, gbc);

            add(panel, BorderLayout.SOUTH);
        }
    }

    public void show(Component c) {
        if (c != null) {
            // wait until the GUI is visible
            int count = 0;
            while (!c.isShowing()) {
                try {
                    if (count >= 5) {
                        break;
                    }

                    wait(500);
                    count++;
                } catch (Exception e) {
                    // don't care
                }
            }
            if (c.isShowing()) {
                setLocation(c.getLocationOnScreen());
            }
        } else {
            GUIUtil.centerOnScreen(this);
        }
        setVisible(true);
    }

    public void show(Exception exception, Component c) {
        set(exception);
        show(c);
    }

    public void show(Set<Exception> exceptions, Component c) {
        set(exceptions);
        show(c);
    }

    public void add(Exception exception) {
        exceptions.add(exception);
        textArea.append("  -  " + exception.getMessage() + "\n");
    }

    public void add(PropagationExceptionWrapper pew) {
        exceptions.add(pew.exception);
        textArea.append(" - " + pew.toString() + "\n");
    }

    public Set<Exception> getExceptions() {
        return exceptions;
    }

    private Exception formatExceptionMessage(Exception exception) {
        if (exception.getClass().equals(NullPointerException.class)) {
            NullPointerException nullPointerException = new NullPointerException("There was a programming error. Please report this error so it can be fixed.");
            nullPointerException.setStackTrace(exception.getStackTrace());
            return nullPointerException;
        } else if (exception.getClass().equals(ArrayIndexOutOfBoundsException.class)) {
            ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException = new ArrayIndexOutOfBoundsException("There was a programming error. Please report this error so it can be fixed.");
            arrayIndexOutOfBoundsException.setStackTrace(exception.getStackTrace());
            return arrayIndexOutOfBoundsException;
        } else if (exception.getClass().equals(BindException.class)) {
            BindException bindException = new BindException("The port you are trying to bind to is already in use. Change the port and try again. Standard Tranche ports are 443 and 1045.");
            bindException.setStackTrace(exception.getStackTrace());
            return bindException;
        } else {
            return exception;
        }
    }

    public void set(Exception exception) {
        exceptions.clear();
        textArea.setText("The following errors occurred:\n\n");
        comment.setText("Please leave a comment describing what occurred.");
        add(formatExceptionMessage(exception));
    }

    public void set(Set<Exception> exceptions) {
        this.exceptions.clear();
        textArea.setText("The following errors occurred:\n\n");
        comment.setText("Please leave a comment describing what occurred.");
        for (Exception e : exceptions) {
            add(formatExceptionMessage(e));
        }
    }

    public void setPropagated(Collection<PropagationExceptionWrapper> exceptions) {
        this.exceptions.clear();
        textArea.setText("The following errors occurred:\n\n");
        comment.setText("Please leave a comment describing what occurred.");
        for (PropagationExceptionWrapper e : exceptions) {
            add(e);
        }
    }

    public void set(Set<Exception> exceptions, boolean showDetails) {
        this.exceptions = exceptions;
        this.showDetails = showDetails;
        textArea.setText("The following errors occurred:\n\n");
        if (!showDetails) {
            for (Exception exception : exceptions) {
                textArea.append("  -  " + exception.getMessage() + '\n');
            }
        } else {
            for (Exception exception : exceptions) {
                textArea.append(exception.getClass().toString() + ": " + exception.getMessage() + '\n');
                for (StackTraceElement trace : exception.getStackTrace()) {
                    textArea.append(trace.toString() + '\n');
                }
                textArea.append("\n");
            }
        }
        comment.setText("Please leave a comment describing what occurred.");
    }

    public void setDetails(boolean showDetails) {
        set(this.exceptions, showDetails);
    }

    public int count() {
        return exceptions.size();
    }
}
