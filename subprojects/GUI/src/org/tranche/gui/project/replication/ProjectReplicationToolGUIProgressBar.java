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
package org.tranche.gui.project.replication;

import org.tranche.gui.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import org.tranche.exceptions.TodoException;
import org.tranche.hash.BigHash;
import org.tranche.project.ProjectReplicationTool;
import org.tranche.project.ProjectReplicationToolListener;
import org.tranche.timeestimator.ContextualTimeEstimator;
import org.tranche.timeestimator.TimeEstimator;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ProjectReplicationToolGUIProgressBar extends JPanel implements ProjectReplicationToolListener {

    private ProjectReplicationTool replicationTool = null;
    private TimeEstimator timeEstimator = new ContextualTimeEstimator();
    // Flag any problems
    public boolean wasError = false,  uploadComplete = false;
    // count files and chunks
    public long chunkCount = 0,  fileCount = 0;
    // buffer the image
    private BufferedImage buffer = null;
    // A prefix for the message string. Allows to say what doing, etc.
    private String prefixString = null;
    private long skippedChunks = 0,  failedChunks = 0,  injectedChunks = 0;

    /**
     * 
     */
    public ProjectReplicationToolGUIProgressBar() {
        setPreferredSize(new Dimension(380, 20));
        setMinimumSize(getPreferredSize());
        setBackground(Styles.COLOR_PROGRESS_BAR_BACKGROUND);
        setForeground(Styles.COLOR_PROGRESS_BAR_FOREGROUND);
    }

    /**
     * 
     * @param graphics
     */
    @Override()
    protected void paintComponent(Graphics graphics) {
        // make some default values
        long sizeOfProject = 1, sizeAlreadyHandled = 0;
        int pixelsWide = 0;
        double progress = 0;

        // if the buffer size isn't good anymore, remake it
        if (buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight()) {
            buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        }

        // update the progress
        if (replicationTool != null) {
            // update the info
            sizeOfProject = replicationTool.getSizeOfProject();
            sizeAlreadyHandled = replicationTool.getSizeAlreadyHandled();

            // estimate the time
            timeEstimator.update(sizeAlreadyHandled, sizeOfProject, 0, 1);
        }

        // update the progress
        progress = timeEstimator.getPercentDone();
        // cap at 100% -- hide project file uploads
        if (progress > 100 || uploadComplete) {
            progress = 100;
        } else if (progress < 0 || !(progress >= 0)) {
            progress = 0;
        }

        pixelsWide = (int) (buffer.getWidth() * progress / 100);

        // make the string
        String string = "";

        if (prefixString != null) {
            string += prefixString + ": ";
        }

        if (uploadComplete) {
            if (!wasError) {
                string += "Replication Complete";
            } else {
                string += "Replication Failed";
            }
        } else if (replicationTool == null) {
            string += "Waiting to start...";
        } else {
            if (true) {
                throw new TodoException();
            }
//            if (progress == 100) {
//                string += "Finishing up...";
//            } else {
//                string += GUIUtil.floatFormat.format(Math.abs(progress)) + "%; ";
//                if (timeEstimator.getHours() < 100) {
//                    string += "Est.: " +
//                            timeEstimator.getHours() + "h " +
//                            timeEstimator.getMinutes() + "m " +
//                            timeEstimator.getSeconds() + "s";
//                } else {
//                    string += "Calculating...";
//                }
//            }
//            string += "; Files: " + GUIUtil.integerFormat.format(fileCount) + "; Chunks: Total=" + GUIUtil.integerFormat.format(chunkCount) + " (success=" + GUIUtil.integerFormat.format(this.injectedChunks) + ", skip=" + GUIUtil.integerFormat.format(this.skippedChunks) + ", fail=" + GUIUtil.integerFormat.format(this.failedChunks) + ")";
        }

        // pick the font
        Font[] fonts = new Font[]{Styles.FONT_10PT};
        // get the desired size of the component
        int desiredWidth = getWidth();
        int desiredHeight = getHeight();
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
        if (uploadComplete) {
            if (!wasError) {
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

        // draw the sub string
        g.setColor(getForeground());
        g.setFont(fontToUse);
        Rectangle2D subStringBounds = g.getFontMetrics(fontToUse).getStringBounds(string, g);
        g.drawString(string, (int) (buffer.getWidth() / 2 - subStringBounds.getWidth() / 2), (int) (buffer.getHeight() / 2 + subStringBounds.getHeight() / 2) - 1);

        // draw the scaled image
        graphics.drawImage(buffer, 0, 0, null);
    }

    /**
     * 
     * @param replicationTool
     */
    public void fireReplicationStarted(ProjectReplicationTool replicationTool) {
        this.replicationTool = replicationTool;

        // While progress bar is running, high contrast. When finished, grey.
        this.setForeground(Color.BLACK);
        repaint();
    }

    /**
     * 
     */
    public void fireReplicationFailed() {
        wasError = true;
        uploadComplete = true;
        this.setForeground(Styles.COLOR_PROGRESS_BAR_FOREGROUND);

        repaint();
    }

    /**
     * 
     */
    public void fireReplicationFinished() {
        uploadComplete = true;
        this.setForeground(Styles.COLOR_PROGRESS_BAR_FOREGROUND);

        repaint();
    }

    /**
     * 
     * @param h
     */
    public void fireDataChunkReplicated(BigHash h) {
        chunkCount++;
        this.injectedChunks++;
        repaint();
    }

    /**
     * 
     * @param h
     */
    public void fireMetaDataChunkReplicated(BigHash h) {
        chunkCount++;
        this.injectedChunks++;
        repaint();
    }

    /**
     * 
     * @param h
     */
    public void fireDataChunkSkipped(BigHash h) {
        chunkCount++;
        this.skippedChunks++;
        repaint();
    }

    /**
     * 
     * @param h
     */
    public void fireMetaDataChunkSkipped(BigHash h) {
        chunkCount++;
        this.skippedChunks++;
        repaint();
    }

    /**
     * 
     * @param h
     */
    public void fireDataChunkFailed(BigHash h) {
        chunkCount++;
        this.failedChunks++;
        repaint();
    }

    /**
     * 
     * @param h
     */
    public void fireMetaDataChunkFailed(BigHash h) {
        chunkCount++;
        this.failedChunks++;
        repaint();
    }

    /**
     * 
     * @param h
     */
    public void fireFileFailed(BigHash h) {
        wasError = true;
        fileCount++;
        repaint();
    }

    /**
     * 
     * @param h
     */
    public void fireFileFinished(BigHash h) {
        fileCount++;
        repaint();
    }

    /**
     * <p>Reset the variables so can reuse. Particularly useful when replicating multiple projects serially.</p>
     */
    public void reset() {
        this.replicationTool = null;
        this.timeEstimator = new ContextualTimeEstimator();
        this.wasError = false;
        this.uploadComplete = false;
        this.chunkCount = 0;
        this.fileCount = 0;
        skippedChunks = 0;
        failedChunks = 0;
        injectedChunks = 0;
    }

    /**
     * <p>Custom message to prefix the message string in the progress bar.</p>
     * @return
     */
    public String getPrefixString() {
        return prefixString;
    }

    /**
     * <p>Custom message to prefix the message string in the progress bar.</p>
     * @param prefixString
     */
    public void setPrefixString(String prefixString) {
        this.prefixString = prefixString;
    }
}
