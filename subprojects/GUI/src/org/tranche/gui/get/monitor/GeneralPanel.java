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
package org.tranche.gui.get.monitor;

import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import org.tranche.commons.TextUtil;
import org.tranche.get.GetFileToolEvent;
import org.tranche.gui.get.DownloadSummary;
import org.tranche.gui.monitor.InfoColumn;
import org.tranche.gui.util.GUIUtil;
import org.tranche.project.ProjectSummary;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GeneralPanel extends JPanel {

    private DownloadMonitor monitor;
    private InfoColumn left;
    private InfoColumn right;
    // thread to update 
    private Thread updateThread;

    public GeneralPanel(DownloadMonitor monitor) {
        this.monitor = monitor;
        left = new InfoColumn(monitor);
        right = new InfoColumn(monitor);
        setName("General");
        setLayout(new GridLayout(1, 2));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        // add the left column
        left.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.BLACK));
        add(left);

        // add the right column
        add(right);

        update();
    }

    public void start() {
        updateThread = new Thread() {

            @Override
            public void run() {
                try {
                    while (true) {
                        update();
                        Thread.sleep(2000);
                    }
                } catch (Exception e) {
                }
            }
        };
        updateThread.start();
    }

    public void stop() {
        updateThread.interrupt();
    }

    public void update() {
        updateLeftColumn();
        updateRightColumn();
    }

    public void updateLeftColumn() {
        DownloadSummary ds = monitor.getDownloadSummary();
        ProjectSummary ps = ds.getProjectSummary();

        left.clear();
        left.put("Hash", ds.getGetFileTool().getHash().toString());
        if (ps != null) {
            left.put("Title", ps.title);
            left.put("Description", ps.description);
        } else {
            try {
                left.put("Title", ds.getGetFileTool().getMetaData().getName());
            } catch (Exception e) {
            }
        }
        try {
            left.put("Encrypted", String.valueOf(ds.getGetFileTool().getMetaData().isEncrypted()));
        } catch (Exception e) {
        }
        left.put("", " ");
        left.put("Filter", ds.getGetFileTool().getRegEx());
        left.put("Files", GUIUtil.integerFormat.format(ds.filesToDownload));
        left.put("Size", TextUtil.formatBytes(ds.bytesToDownload));
        left.put("Passphrase Provided", String.valueOf(ds.getGetFileTool().getPassphrase() != null && !ds.getGetFileTool().getPassphrase().equals("")));
        left.put("Validate", String.valueOf(ds.getGetFileTool().isValidate()));
        left.put("Download Location", ds.getGetFileTool().getSaveFile().getAbsolutePath());
        left.refresh();
    }

    public void updateRightColumn() {
        DownloadSummary ds = monitor.getDownloadSummary();

        right.clear();
        right.put("Status", ds.getStatus());
        if (ds.getReport() != null) {
            right.put("Elapsed Time", TextUtil.formatTimeLength(ds.getReport().getTimeToFinish()));
            right.put("Downloaded", TextUtil.formatBytes(ds.getReport().getBytesDownloaded()));
        } else if (ds.getGetFileTool().getTimeEstimator() != null) {
            right.put("Time Remaining", ds.getGetFileTool().getTimeEstimator().getTimeLeftString());
            right.put("Downloaded", TextUtil.formatBytes(ds.getGetFileTool().getBytesDownloaded()) + " / " + TextUtil.formatBytes(ds.getGetFileTool().getBytesToDownload()));
        }
        right.put("", " ");
        right.put("Meta Data", "");
        right.put("     Started", String.valueOf(ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_METADATA), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_STARTED))));
        right.put("     Finished", String.valueOf(ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_METADATA), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_FINISHED))));
        right.put("     Failed", String.valueOf(ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_METADATA), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_FAILED))));
        right.put("Chunks", "");
        right.put("     Started", String.valueOf(ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_DATA), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_STARTED))));
        right.put("     Finished", String.valueOf(ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_DATA), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_FINISHED))));
        right.put("     Failed", String.valueOf(ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_DATA), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_FAILED))));
        right.put("Files", "");
        right.put("     Skipped", String.valueOf(ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_FILE), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_SKIPPED))));
        right.put("     Finished", String.valueOf(ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_FILE), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_FINISHED))));
        right.put("     Failed", String.valueOf(ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_FILE), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_FAILED))));
        right.refresh();
    }
}
