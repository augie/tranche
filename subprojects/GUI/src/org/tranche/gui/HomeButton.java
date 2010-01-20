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
import org.tranche.gui.advanced.AdvancedGUI;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class HomeButton extends GenericMenuBarButton {

    public HomeButton(final ErrorFrame ef, final Component relativeTo) {
        setToolTipText("Open the Tranche Tool");
        try {
            setIcon(new ImageIcon(ImageIO.read(HomeButton.class.getResourceAsStream("/org/tranche/gui/image/go-home-16x16.gif"))));
        } catch (Exception e) {
        }
        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                // flag that this button has been pressed
                if (GUIUtil.getAdvancedGUI() != null) {
                    GUIUtil.getAdvancedGUI().setLocationRelativeTo(relativeTo);
                    GUIUtil.getAdvancedGUI().setVisible(true);
                    return;
                }

                Thread t = new Thread() {

                    @Override
                    public void run() {
                        IndeterminateProgressBar progress = new IndeterminateProgressBar("Opening Tranche Tool");
                        progress.setLocationRelativeTo(relativeTo);
                        progress.start();
                        try {
                            AdvancedGUI.openNewInstance(new String[0]);
                            GUIUtil.getAdvancedGUI().setDefaultCloseOperation(GenericFrame.HIDE_ON_CLOSE);
                        } catch (Exception e) {
                            progress.stop();
                            ef.show(e, relativeTo);
                        } finally {
                            progress.stop();
                        }
                        HomeButton.this.setVisible(false);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        setOpaque(false);
    }
}
