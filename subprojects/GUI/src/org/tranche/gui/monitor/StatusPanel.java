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
package org.tranche.gui.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import org.tranche.gui.GenericLabel;
import org.tranche.gui.GenericButton;
import org.tranche.gui.Styles;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class StatusPanel extends JPanel {

    private Monitor monitor;
    private GenericLabel activity = new GenericLabel("  NOT LISTENING");
    private GenericButton pausePlayButton = new GenericButton("Stop Listening");
    private ImageIcon pauseIcon,  playIcon;

    public StatusPanel(Monitor monitor) {
        this.monitor = monitor;
        setLayout(new BorderLayout());

        // add the activity label
        activity.setFont(Styles.FONT_10PT);
        add(activity, BorderLayout.CENTER);

        // load the icons
        try {
            pauseIcon = new ImageIcon(ImageIO.read(StatusPanel.class.getResourceAsStream("/org/tranche/gui/image/pause.gif")));
            playIcon = new ImageIcon(ImageIO.read(StatusPanel.class.getResourceAsStream("/org/tranche/gui/image/play.gif")));
        } catch (Exception e) {
        }

        // pause/play button
        pausePlayButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (StatusPanel.this.monitor.isListening()) {
                    StatusPanel.this.monitor.stop();
                    pausePlayButton.setText("Start Listening");
                    pausePlayButton.setIcon(playIcon);
                } else {
                    StatusPanel.this.monitor.start();
                    pausePlayButton.setText("Stop Listening");
                    pausePlayButton.setIcon(pauseIcon);
                }
            }
        });
        pausePlayButton.setFont(Styles.FONT_10PT);
        pausePlayButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.BLACK), BorderFactory.createEmptyBorder(0, 5, 0, 5)));
        pausePlayButton.setIcon(pauseIcon);
        pausePlayButton.setBackground(Color.WHITE);
        add(pausePlayButton, BorderLayout.EAST);
    }

    public void setStatus(String status) {
        this.activity.setText("  " + status);
    }
}
