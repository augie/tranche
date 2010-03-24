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
package org.tranche.gui.get;

import org.tranche.gui.*;
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
import org.tranche.get.GetFileTool;
import org.tranche.get.GetFileToolEvent;
import org.tranche.get.GetFileToolListener;
import org.tranche.gui.util.GUIUtil;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.util.Text;
import org.tranche.util.ThreadUtil;

/**
 * <p>A simple progress bar for showing download status.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class GetFileToolProgressBar extends JPanel implements GetFileToolListener {

    private GetFileTool gft;
    public int errorCount = 0;
    private BufferedImage buffer;
    private RepaintThread repaintThread = new RepaintThread();

    /**
     * 
     * @param gft
     */
    public GetFileToolProgressBar(GetFileTool gft) {
        this.gft = gft;
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
     * <p>Notification of any other event occurence.</p>
     * @param msg
     */
    public void message(String msg) {
    }

    /**
     * <p>Notification that a meta data chunk is starting to be downloaded.</p>
     * @param event
     */
    public void startedMetaData(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a meta data chunk is trying to be downloaded from a server.</p>
     * @param event
     */
    public void tryingMetaData(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a meta data chunk could not be downloaded.</p>
     * @param event
     * @param exceptions
     */
    public void failedMetaData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        errorCount += exceptions.size();
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a meta data chunk has been downloaded.</p>
     * @param event
     */
    public void finishedMetaData(GetFileToolEvent event) {
        repaintThread.requestPaint();
    }

    /**
     * <p>Notificatin that a data chunk is starting to be downloaded.</p>
     * @param event
     */
    public void startedData(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk is trying to be downloaded from a server.</p>
     * @param event
     */
    public void tryingData(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a data chunk could not be downloaded.</p>
     * @param event
     * @param exceptions
     */
    public void failedData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        errorCount += exceptions.size();
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a data chunk has been downloaded.</p>
     * @param event
     */
    public void finishedData(GetFileToolEvent event) {
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a file is starting to be downloaded.</p>
     * @param event
     */
    public void startingFile(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a file download has started.</p>
     * @param event
     */
    public void startedFile(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a file downloaded has been skipped.</p>
     * @param event
     */
    public void skippedFile(GetFileToolEvent event) {
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a file download has finished.</p>
     * @param event
     */
    public void finishedFile(GetFileToolEvent event) {
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a file download has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedFile(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        errorCount += exceptions.size();
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a directory download is starting.</p>
     * @param event
     */
    public void startingDirectory(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a directory download has started.</p>
     * @param event
     */
    public void startedDirectory(GetFileToolEvent event) {
    }

    /**
     * <p>Notification that a directory download has finished.</p>
     * @param event
     */
    public void finishedDirectory(GetFileToolEvent event) {
        repaintThread.requestPaint();
    }

    /**
     * <p>Notification that a directory download has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedDirectory(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
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
        if (gft.getTimeEstimator() != null) {
            progress = gft.getTimeEstimator().getPercentDone();
            pixelsWide = (int) (buffer.getWidth() * progress / 100);
        }

        // make the string
        String string = "";
        if (gft == null) {
            string = "Waiting to start...";
        } else if (!gft.isExecuting()) {
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
                if (gft.isPaused()) {
                    string = string + "Paused";
                } else if (gft.getTimeEstimator() != null) {
                    if (gft.getTimeEstimator().getHours() > 5000) {
                        string = string + "Calculating...";
                    } else {
                        string = string + gft.getTimeEstimator().getTimeLeftString();
                    }
                } else {
                    string = string + "Calculating...";
                }
            }
            string = string + "; " + Text.getFormattedBytes(gft.getBytesDownloaded()) + " / " + Text.getFormattedBytes(gft.getBytesToDownload());
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
        if (!gft.isExecuting()) {
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
        //g.setColor(Styles.COLOR_PROGRESS_BAR_FOREGROUND);
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
            setName("Get File Tool Progress Bar Repaint Thread");
        }

        @Override
        public void run() {
            while (!closed) {
                repaint();
                ThreadUtil.safeSleep(500);
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
                SwingUtilities.invokeLater(new Thread("Get File Tool Progress Bar Repaint") {

                    @Override
                    public void run() {
                        GetFileToolProgressBar.this.validate();
                        GetFileToolProgressBar.this.repaint();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
