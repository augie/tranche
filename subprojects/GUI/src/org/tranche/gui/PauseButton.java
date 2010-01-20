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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public abstract class PauseButton extends GenericButton {

    private static final String PAUSE = "Pause";
    private static final String RESUME = "Resume";

    public PauseButton() {
        try {
            final ImageIcon pauseIcon = new ImageIcon(ImageIO.read(PauseButton.class.getResourceAsStream("/org/tranche/gui/image/pause.gif")));
            final ImageIcon resumeIcon = new ImageIcon(ImageIO.read(PauseButton.class.getResourceAsStream("/org/tranche/gui/image/play.gif")));
            setIcon(pauseIcon);
            setToolTipText(PAUSE);
            addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            if (getToolTipText().equals(PAUSE)) {
                                setIcon(resumeIcon);
                                setToolTipText(RESUME);
                                onPause();
                            } else {
                                setIcon(pauseIcon);
                                setToolTipText(PAUSE);
                                onResume();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public abstract void onPause();

    public abstract void onResume();
}
