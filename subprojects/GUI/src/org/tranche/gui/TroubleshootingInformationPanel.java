/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tranche.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.tranche.gui.util.GUIUtil;
import org.tranche.logs.LogUtil;

/**
 * <p>Information for developers.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class TroubleshootingInformationPanel extends JPanel {

    private int MARGIN = 10;
    public static final Dimension RECOMMENDED_DIMENSION = new Dimension(700, 500);
    public static final String TITLE = "Troubleshooting information";

    public TroubleshootingInformationPanel() {
        setBackground(Styles.COLOR_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Title
        JLabel troubleshootingLabel = new GenericLabel(TITLE);
        troubleshootingLabel.setFont(Styles.FONT_14PT_BOLD);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(MARGIN * 2, MARGIN, 0, MARGIN);
        add(troubleshootingLabel, gbc);

        // Brief information
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
        add(new DisplayTextArea("When contacting developers or support about issues with Tranche, please include the following information."), gbc);

        final String troubleshootingMsg = getTroubleshootingInformation();

        // Copy to clipboard
        GenericRoundedButton copyButton = new GenericRoundedButton("Copy to clipboard");
        copyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        GUIUtil.copyToClipboard(troubleshootingMsg, null);
                        GenericOptionPane.showMessageDialog(
                                TroubleshootingInformationPanel.this.getParent(),
                                "Copied to clipboard. You can now paste the troubleshooting information in an email or text editor.",
                                "Copied troubleshooting information",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        copyButton.setToolTipText("Copies troubleshooting information to system clipboard so you can paste it.");
        add(copyButton, gbc);

        // Text 
        {
            JTextArea textArea = new JTextArea(troubleshootingMsg);
            textArea.setFont(Styles.FONT_12PT);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setBackground(Color.WHITE);
            textArea.setBorder(Styles.BORDER_BLACK_1);
            textArea.setMargin(new Insets(5, 5, 5, 5));

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);

            gbc.insets = new Insets(MARGIN, MARGIN, MARGIN * 2, MARGIN);
            add(textArea, gbc);
        }
    }

    private static String getTroubleshootingInformation() {
        return LogUtil.getTroubleshootingInformation();
    }
}
