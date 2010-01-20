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
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Tranchetastic :)
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class TrancheLogoPanel extends JPanel implements LazyLoadable {

    // static defaults for drawing the tree
    public static final int DEFAULT_WIDTH = 150;
    public static final int DEFAULT_HEIGHT = 150;
    public static final int DEFAULT_ANGLE = 40;
    public static final int DEFAULT_SIZE = 35;
    public static final int DEFAULT_ITERATIONS = 9;
    public static final long DEFAULT_PAUSE = 200;
    public static final int DEFAULT_FADE_STEPS = 3;
    // the width and height
    private int width = DEFAULT_WIDTH, height = DEFAULT_HEIGHT;
    // used for logo
    private float angle = DEFAULT_ANGLE, size = DEFAULT_SIZE;
    private int iterations = DEFAULT_ITERATIONS;
    // the pause time between squares
    long pauseTimeInMillis = DEFAULT_PAUSE;
    // the fade steps to use -- lower is faster for renders
    int fadeSteps = 10;
    // playing with blue
    int[] reds, greens, blues, alphas;
    // array of colors
    private Color[][] colors;
    // the current image
    private BufferedImage buffer;
    private final Object bufferLock = new Object();
    // the tilted image
    private BufferedImage tiltedBuffer;
    // ratio to scale height by
    private float ratio = 110f / 150f;
    private boolean lazyLoaded = false;

    public static void main(String[] args) throws Exception {
        // draw the panel
        TrancheLogoPanel panel = new TrancheLogoPanel();
        panel.setSize(800, 800);
        panel.resetSize();

        JFrame f = new JFrame();
        f.add(panel);
        f.pack();
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // manually lazy load
        panel.lazyLoad();
    }

    public TrancheLogoPanel() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_ANGLE, DEFAULT_SIZE, DEFAULT_ITERATIONS, DEFAULT_PAUSE, DEFAULT_FADE_STEPS);
    }

    /**
     * Force this logo panel to always be the same size.
     */
    public TrancheLogoPanel(int width, int height, int angle, int size, int iterations, long pauseInMillis, int fadeSteps) {
        // why reset the size here?
        resetSize();

        // set the width and height
        this.width = width;
        this.height = height;
        this.angle = angle;
        this.size = size;
        this.iterations = iterations;
        this.pauseTimeInMillis = pauseInMillis;

        // set the base color to that given in Styles
        if (Styles.INT_TRANCHE_COLORS_TO_USE == 1) {
            setBaseColor(Styles.COLOR_TRANCHE_LOGO);
        } else {
            setColors(Styles.COLOR_TRANCHE_LOGO, Styles.COLOR_TRANCHE_LOGO2);
        }

        // set the buffered Images
        buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // the tilted image
        tiltedBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // lazy load!
        LazyLoadAllSlowStuffAfterGUIRenders.add(this);
    }

    private void adjustColors() {
        // set the colors
        colors = new Color[fadeSteps][2];
        for (int i = 0; i < colors.length; i++) {
            // make the color for each
            for (int j = 0; j < colors[i].length; j++) {
                // adjustment factors for the color channels
                int adjustRed = (i + 1) * (255 - reds[j]) / (colors.length);
                int adjustGreen = (i + 1) * (255 - greens[j]) / (colors.length);
                int adjustBlue = (i + 1) * (255 - blues[j]) / (colors.length);
                // the colors
                colors[i][j] = new Color(255 - adjustRed, 255 - adjustGreen, 255 - adjustBlue, alphas[j]);
            }
        }
    }

    public void setBaseColor(Color color) {
        reds = new int[]{color.getRed() + 70, color.getRed()};
        greens = new int[]{color.getGreen() + 70, color.getGreen()};
        blues = new int[]{color.getBlue() + 70, color.getBlue()};
        alphas = new int[]{color.getAlpha(), color.getAlpha()};
        adjustColors();
    }

    public void setColors(Color color1, Color color2) {
        reds = new int[]{color1.getRed(), color2.getRed()};
        greens = new int[]{color1.getGreen(), color2.getGreen()};
        blues = new int[]{color1.getBlue(), color2.getBlue()};
        alphas = new int[]{color1.getAlpha(), color2.getAlpha()};
        adjustColors();
    }

    public void setWidth(int width) {
        this.width = width;
        resetSize();
    }

    public void setHeight(int height) {
        this.height = height;
        resetSize();
    }

    private void resetSize() {
        setPreferredSize(new Dimension(width, (int) (height * ratio)));
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        validate();
    }

    public synchronized void lazyLoad() {
        // return immediately if this class already lazy loaded
        if (lazyLoaded) {
            return;
        }
        // flag as lazy loaded
        lazyLoaded = true;

        // make a background thread to update the pic
        Thread t = new Thread("Tranche Logo") {

            @Override
            public void run() {
                try {
                    // pause for a little bit
                    Thread.sleep(1500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                // draw that image!
                try {
                    startDrawing();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        t.start();
    }

    /**
     * Yes, Jayson paid attention in Trig.
     */
    public void startDrawing() throws Exception {
        // track iterations to know fade color
        long ticks = 0;

        // wait to draw only when the logo panel is showing
        while (!isShowing()) {
            try {
                Thread.sleep(250);
            } catch (Exception e) {
            }
        }

        // buffer to draw
        LinkedList<RectToDraw> toDraw = new LinkedList();
        // add the inital rect
        RectToDraw first = new RectToDraw();
//        first.bx = width/2;
        first.bx = 0;
        first.by = height / 2 + size / 2;
        first.cx = first.bx + size;
        first.cy = first.by;
        first.dx = first.cx;
        first.dy = first.cy - size;
        first.ax = first.bx;
        first.ay = first.dy;
        first.count = 1;
        // add it
        toDraw.add(first);

        // get the graphics
        Graphics2D g = (Graphics2D) buffer.getGraphics();
        // anti-alias!
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());

//        // optionally save the gif segments
//        int lastSplitIndex = 0;

        // draw everything
        while (!toDraw.isEmpty()) {
            try {
                // increment ticks to track color changes
                ticks++;

                // draw the rect
                RectToDraw rdt = toDraw.getFirst();
                // only remove after enough ticks
                if (ticks % colors.length == colors.length - 1) {
                    toDraw.removeFirst();
                }

//            // render the image to disk if appropriate
//            if (lastSplitIndex != rdt.count) {
//                synchronized (buffer) {
//                    File imageFile = new File("tranche-logo-"+rdt.count+".png");
//                    System.out.println("Saving as "+imageFile.getAbsolutePath());
//                    ImageIO.write(buffer, "png", imageFile);
//                    lastSplitIndex = rdt.count;
//                }
//            }

                // if we've finished drawing the whole thing, quit
                if (rdt.count > iterations) {
                    continue;
                }

                // calc the length
                float x = Math.abs(rdt.ax - rdt.bx);
                float y = Math.abs(rdt.ay - rdt.by);
                // a^2 = b^2 + c^2
                float length = (float) (Math.sqrt(x * x + y * y));

                // calc the new length (always smaller, depends on angle)
                float newLength = (float) (length / 2 / Math.cos(Math.toRadians(angle)));

                // set the color appropriately
                int colorIndex = (int) (ticks % colors.length);
                // set the right color to get the fade-in look
                g.setColor(colors[colorIndex][rdt.count % colors[colorIndex].length]);

                // draw the shape as a polygon
                Polygon p = new Polygon();
                p.addPoint((int) rdt.ax, (int) rdt.ay);
                p.addPoint((int) rdt.bx, (int) rdt.by);
                p.addPoint((int) rdt.cx, (int) rdt.cy);
                p.addPoint((int) rdt.dx, (int) rdt.dy);
                g.fillPolygon(p);

                // only add more if at the appropriate tick
                if (ticks % colors.length == colors.length - 1) {
                    // draw the rect that goes to the right
                    {
                        // make the angle
                        float newAngle = rdt.angle - angle - 180;
                        RectToDraw nrdt = new RectToDraw();
                        // we know that bx and by stay the same
                        nrdt.bx = rdt.cx;
                        nrdt.by = rdt.cy;
                        // ax and ay are calc'd using the new length and known angle
                        float aAngle = newAngle;
                        nrdt.ax = rdt.cx + (float) (newLength * Math.sin(Math.toRadians(aAngle)));
                        nrdt.ay = rdt.cy + (float) (newLength * Math.cos(Math.toRadians(aAngle)));
                        // cx and cy are based on the old
                        float cAngle = newAngle - 90;
                        nrdt.cx = rdt.cx + (float) (newLength * Math.sin(Math.toRadians(cAngle)));
                        nrdt.cy = rdt.cy + (float) (newLength * Math.cos(Math.toRadians(cAngle)));
                        // dx and dy are based on the old
                        float dAngle = newAngle - 90;
                        nrdt.dx = nrdt.ax + (float) (newLength * Math.sin(Math.toRadians(cAngle)));
                        nrdt.dy = nrdt.ay + (float) (newLength * Math.cos(Math.toRadians(cAngle)));
                        // set the new angle
                        nrdt.angle = rdt.angle - angle;
                        nrdt.count = rdt.count + 1;
                        toDraw.add(nrdt);
                    }


                    // draw the rect that goes to the left
                    {
                        // make the angle
                        float newAngle = rdt.angle + angle;
                        RectToDraw nrdt = new RectToDraw();
                        // we know that bx and by stay the same
                        nrdt.ax = rdt.dx;
                        nrdt.ay = rdt.dy;
                        // ax and ay are calc'd using the new length and known angle
                        float aAngle = newAngle;
                        nrdt.bx = rdt.dx + (float) (newLength * Math.sin(Math.toRadians(aAngle)));
                        nrdt.by = rdt.dy + (float) (newLength * Math.cos(Math.toRadians(aAngle)));
                        // cx and cy are based on the old
                        float cAngle = newAngle + 90;
                        nrdt.cx = nrdt.bx + (float) (newLength * Math.sin(Math.toRadians(cAngle)));
                        nrdt.cy = nrdt.by + (float) (newLength * Math.cos(Math.toRadians(cAngle)));
                        // dx and dy are based on the old
                        float dAngle = newAngle + 90;
                        nrdt.dx = rdt.dx + (float) (newLength * Math.sin(Math.toRadians(cAngle)));
                        nrdt.dy = rdt.dy + (float) (newLength * Math.cos(Math.toRadians(cAngle)));
                        // set the new angle
                        nrdt.angle = rdt.angle + angle;
                        nrdt.count = rdt.count + 1;
                        toDraw.add(nrdt);
                    }

                }

                // update the buffer synchronously to keep from having redraw issues
                tilt(buffer, Math.toRadians(-90));

                repaint();

                // wait some so that the pic grows over time
                try {
                    Thread.sleep(pauseTimeInMillis);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                while (!isVisible() || !isShowing()) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Tile the whole image on to its side because I didn't think to draw it straight in the first place.
     */
    public void tilt(BufferedImage image, double angle) {
        // a hack to tilt at exactly 90 degrees quickly
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                synchronized (bufferLock) {
                    tiltedBuffer.setRGB(j, width - i - 1, buffer.getRGB(i, j));
                }
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        synchronized (bufferLock) {
            g.drawImage(tiltedBuffer, 0, -(int) (width - ratio * width), null);
        }
    }

    private class RectToDraw {

        float ax = 0;
        float ay = 0;
        float bx = 10;
        float by = 0;
        float cx = 10;
        float cy = 10;
        float dx = 0;
        float dy = 10;
        float angle = 0;
        Color color = new Color(255, 255, 255, 200);
        int count = 0;
    }
}

