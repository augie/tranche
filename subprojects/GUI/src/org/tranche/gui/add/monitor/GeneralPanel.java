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
package org.tranche.gui.add.monitor;

import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import org.tranche.add.AddFileToolEvent;
import org.tranche.commons.TextUtil;
import org.tranche.gui.add.UploadSummary;
import org.tranche.gui.monitor.InfoColumn;
import org.tranche.gui.util.GUIUtil;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.users.UserCertificateUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GeneralPanel extends JPanel {

    private UploadMonitor monitor;
    private InfoColumn left;
    private InfoColumn right;
    // thread to update 
    private Thread updateThread;

    public GeneralPanel(UploadMonitor monitor) {
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
                    while (monitor.getUploadSummary().getReport() == null || !monitor.getUploadSummary().getReport().isFinished()) {
                        update();
                        Thread.sleep(2000);
                    }
                    update();
                } catch (Exception e) {
                }
            }
        };
        updateThread.setDaemon(true);
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
        UploadSummary us = monitor.getUploadSummary();

        left.clear();
        left.put("Upload", us.getAddFileTool().getFile().getAbsolutePath());
        left.put("Title", us.getAddFileTool().getTitle());
        left.put("Description", us.getAddFileTool().getDescription());
        if (us.getAddFileTool().getLicense() == null) {
            left.put("License", "");
        } else {
            left.put("License", us.getAddFileTool().getLicense().getTitle());
        }
        left.put("Encrypted", String.valueOf(us.getAddFileTool().getPassphrase() != null && !us.getAddFileTool().getPassphrase().equals("")));
        left.put("Files", GUIUtil.integerFormat.format(us.getAddFileTool().getFileCount()));
        left.put("Size", TextUtil.formatBytes(us.getAddFileTool().getSize()));
        left.put("User", String.valueOf(UserCertificateUtil.readUserName(us.getAddFileTool().getUserCertificate())));
        left.put("", " ");
        left.put("Compress", String.valueOf(us.getAddFileTool().isCompress()));
        left.put("Data Only", String.valueOf(us.getAddFileTool().isDataOnly()));
        left.put("Explode", String.valueOf(us.getAddFileTool().isExplodeBeforeUpload()));
        left.put("Send Email On Failure", String.valueOf(us.getAddFileTool().isEmailOnFailure()));
        left.put("Use Unspecified Servers", String.valueOf(us.getAddFileTool().isUsingUnspecifiedServers()));

        if (!us.getAddFileTool().getMetaDataAnnotations().isEmpty()) {
            left.put("", " ");
            left.put("Annotations", "");
            left.put("", " ");
            for (MetaDataAnnotation mda : us.getAddFileTool().getMetaDataAnnotations()) {
                left.put(mda.getName(), mda.getValue());
            }
        }

        left.refresh();
    }

    public void updateRightColumn() {
        UploadSummary us = monitor.getUploadSummary();

        right.clear();
        right.put("Status", us.getStatus());
        if (us.getReport() != null) {
            right.put("Elapsed Time", TextUtil.formatTimeLength(us.getReport().getTimeToFinish()));
            right.put("Uploaded", TextUtil.formatBytes(us.getReport().getBytesUploaded()));
        } else if (us.getAddFileTool().getTimeEstimator() != null) {
            right.put("Time Remaining", us.getAddFileTool().getTimeEstimator().getTimeLeftString());
            right.put("Uploaded", TextUtil.formatBytes(us.getAddFileTool().getBytesUploaded()) + " / " + TextUtil.formatBytes(us.getAddFileTool().getBytesToUpload()));
        }
        right.put("", " ");
        right.put("Data", "");
        right.put("     Started", String.valueOf(us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_DATA), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_STARTED))));
        right.put("     Finished", String.valueOf(us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_DATA), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_FINISHED))));
        right.put("     Failed", String.valueOf(us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_DATA), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_FAILED))));
        right.put("Meta Data", "");
        right.put("     Started", String.valueOf(us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_METADATA), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_STARTED))));
        right.put("     Finished", String.valueOf(us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_METADATA), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_FINISHED))));
        right.put("     Failed", String.valueOf(us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_METADATA), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_FAILED))));
        right.put("Files", "");
        right.put("     Finished", String.valueOf(us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_FILE), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_FINISHED))));
        right.put("     Failed", String.valueOf(us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_FILE), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_FAILED))));
        right.refresh();
    }
}
