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
package org.tranche.gui.add;

import org.tranche.gui.*;
import org.tranche.gui.util.GUIUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.tranche.add.AddFileTool;
import org.tranche.add.AddFileToolEvent;
import org.tranche.add.AddFileToolListener;
import org.tranche.commons.TextUtil;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.commons.ThreadUtil;

/**
 * A spiffy progress bar that shows both overall progress.
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class AddFileToolProgressBar extends JPanel implements AddFileToolListener {

    private AddFileTool aft;
    public int errorCount = 0;
    private BufferedImage buffer;
    private RepaintThread repaintThread = new RepaintThread();

    /**
     * 
     * @param aft
     */
    public AddFileToolProgressBar(AddFileTool aft) {
        this.aft = aft;
        setPreferredSize(new Dimension(300, 20));
        setMinimumSize(getPreferredSize());
        setBackground(Styles.COLOR_PROGRESS_BAR_BACKGROUND);
        setForeground(Color.BLACK);
        repaintThread.setDaemon(true);
        repaintThread.start();
    }

    /**
     *
     * @throws java.lang.Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            repaintThread.close();
        } finally {
            super.finalize();
        }
    }

    /**
     * 
     * @param count
     */
    public void incrementErrorCount(int count) {
        this.errorCount += count;
    }

    /**
     * 
     * @param msg
     */
    public void message(String msg) {
    }

    /**
     * <p>Notification that a meta data chunk is starting to be uploaded.</p>
     * @param event
     */
    public void startedMetaData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a meta data chunk is trying to be uploaded to a server.</p>
     * @param event
     */
    public void tryingMetaData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a meta data chunk has been uploaded to a server.</p>
     * @param event
     */
    public void uploadedMetaData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a meta data chunk could not be uploaded.</p>
     * @param event
     * @param exceptions
     */
    public void failedMetaData(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        errorCount += exceptions.size();
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a meta data chunk has been uploaded.</p>
     * @param event
     */
    public void finishedMetaData(AddFileToolEvent event) {
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a data chunk is about to be checked for on-line replications.</p>
     * @param event
     */
    public void startingData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk is starting to be uploaded.</p>
     * @param event
     */
    public void startedData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk is trying to be uploaded to a server.</p>
     * @param event
     */
    public void tryingData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk has been uploaded to a server.</p>
     * @param event
     */
    public void uploadedData(AddFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk could not be uploaded.</p>
     * @param event
     * @param exceptions
     */
    public void failedData(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        errorCount += exceptions.size();
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a data chunk has been uploaded.</p>
     * @param event
     */
    public void finishedData(AddFileToolEvent event) {
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a file upload has started.</p>
     * @param event
     */
    public void startedFile(AddFileToolEvent event) {
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a file upload has finished.</p>
     * @param event
     */
    public void finishedFile(AddFileToolEvent event) {
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a file upload has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedFile(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        errorCount += exceptions.size();
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a directory upload has started.</p>
     * @param event
     */
    public void startedDirectory(AddFileToolEvent event) {
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a directory upload has finished.</p>
     * @param event
     */
    public void finishedDirectory(AddFileToolEvent event) {
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a directory upload has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedDirectory(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        errorCount += exceptions.size();
        repaintThread.requestPaint();
    }

    /**
     * 
     * @param graphics
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        // if the buffer size isn't good anymore, remake it
        if (buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight()) {
            buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        }

        // update the progress
        double progress = 0;
        int pixelsWide = 0;
        if (aft.getTimeEstimator() != null) {
            progress = aft.getTimeEstimator().getPercentDone();
            pixelsWide = (int) (buffer.getWidth() * progress / 100);
        }

        // make the string
        String string = "";
        if (aft == null) {
            string = "Waiting to start...";
        } else if (!aft.isExecuting()) {
            if (errorCount == 0) {
                string = "Finished";
            } else {
                string = "Failed";
            }
            pixelsWide = buffer.getWidth();
        } else {
            if (progress == 100) {
                string = "Finishing...";
            } else {
                string = GUIUtil.floatFormat.format(Math.abs(progress)) + "%; ";
                if (aft.isPaused()) {
                    string = string + "Paused";
                } else if (aft.getTimeEstimator() != null) {
                    if (aft.getTimeEstimator().getHours() > 5000) {
                        string = string + "Calculating...";
                    } else {
                        string = string + aft.getTimeEstimator().getTimeLeftString();
                    }
                } else {
                    string = string + "Calculating...";
                }
            }
            string = string + "; " + TextUtil.formatBytes(aft.getBytesUploaded()) + " / " + TextUtil.formatBytes(aft.getBytesToUpload());
        }

        // pick the font
        Font[] fonts = new Font[]{Styles.FONT_10PT};
        // get the desired size of the component
        int desiredWidth = getWidth(), desiredHeight = getHeight();
        // find the largest font that'll fit
        Font fontToUse = null;
        for (int i = 0; i < fonts.length; i++) {
            fontToUse = fonts[i];
            // get the size of the string painted
            Rectangle2D bounds = graphics.getFontMetrics(fontToUse).getStringBounds(string, graphics);
            if (bounds.getWidth() <= desiredWidth && bounds.getHeight() <= desiredHeight) {
                break;
            }
        }

        // anti-alias
        Graphics2D g = (Graphics2D) buffer.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // paint the background with progress
        g.setColor(getBackground());
        g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());

        Color fillColor;
        if (!aft.isExecuting()) {
            if (errorCount == 0) {
                fillColor = new Color(0.7f, 1, 0.7f);
            } else {
                fillColor = new Color(1, 0.7f, 0.7f);
            }
        } else {
            float redComponent = 1;
            try {
                redComponent -= (0.3f * (progress / 100));
            } catch (Exception e) {
            }
            fillColor = new Color(redComponent, 1, 0.7f);
        }
        g.setColor(fillColor);
        g.fillRect(0, 0, pixelsWide, buffer.getHeight());

        // set the color
        if (isEnabled()) {
            g.setColor(Styles.COLOR_PROGRESS_BAR_TEXT_ENABLED);
        } else {
            g.setColor(Styles.COLOR_PROGRESS_BAR_TEXT_DISABLED);
        }
        // draw the sub string
        g.setColor(getForeground());
        g.setFont(fontToUse);
        Rectangle2D subStringBounds = g.getFontMetrics(fontToUse).getStringBounds(string, g);
        g.drawString(string, (int) (buffer.getWidth() / 2 - subStringBounds.getWidth() / 2), (int) (buffer.getHeight() / 2 + subStringBounds.getHeight() / 2) - 1);

        // draw the scaled image
        graphics.drawImage(buffer, 0, 0, null);
    }

    private class RepaintThread extends Thread {

        private boolean closed = false,  paintRequested = false;

        public RepaintThread() {
            setName("Add File Tool Progress Bar Repaint Thread");
        }

        @Override
        public void run() {
            while (!closed) {
                repaint();
                ThreadUtil.sleep(500);
            }
            repaint();
        }

        public void close() {
            closed = true;
        }

        public void requestPaint() {
            paintRequested = true;
        }

        private synchronized void repaint() {
            if (!paintRequested) {
                return;
            }
            paintRequested = false;
            try {
                SwingUtilities.invokeLater(new Thread("Add File Tool Progress Bar Repaint") {

                    @Override
                    public void run() {
                        AddFileToolProgressBar.this.validate();
                        AddFileToolProgressBar.this.repaint();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
