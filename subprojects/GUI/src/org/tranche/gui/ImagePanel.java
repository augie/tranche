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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ImagePanel extends JPanel {

    private Image backImage = null;

    public ImagePanel(Image backImage) {
        setBackground(Styles.COLOR_TRANSPARENT);
        try {
            this.backImage = backImage;
            setPreferredSize(new Dimension(backImage.getWidth(null), backImage.getHeight(null)));
            setSize(backImage.getWidth(null), backImage.getHeight(null));
            setMaximumSize(new Dimension(backImage.getWidth(null), backImage.getHeight(null)));
            setMinimumSize(new Dimension(backImage.getWidth(null), backImage.getHeight(null)));
        } catch (Exception e) {
            throw new RuntimeException("Can't make background image.", e);
        }
    }

    public void paintComponent(Graphics g) {
        Graphics innerG = g.create();
        if (backImage == null) {
            innerG.setColor(Color.WHITE);
            innerG.fill3DRect(0, 0, getSize().width, getSize().height, true);
        } else {
            innerG.drawImage(backImage, 0, 0, getSize().width, getSize().height, this);
        }
    }

    public void setBackImage(String path) {
        ImageIcon ic = new ImageIcon(path);
        this.backImage = ic.getImage();
        repaint();
    }
}