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
package org.tranche.gui.advanced;

import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.*;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.tranche.gui.get.DownloadPool;
import org.tranche.gui.get.DownloadPoolEvent;
import org.tranche.gui.get.DownloadPoolListener;
import org.tranche.gui.project.ProjectPool;
import org.tranche.gui.project.ProjectPoolEvent;
import org.tranche.gui.project.ProjectPoolListener;
import org.tranche.gui.add.UploadPool;
import org.tranche.gui.add.UploadPoolEvent;
import org.tranche.gui.add.UploadPoolListener;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableEvent;
import org.tranche.network.StatusTableRow;
import org.tranche.network.StatusTableListener;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class BottomPanel extends JPanel implements LazyLoadable {

    private JLabel text = new GenericLabel(" ");
    private final String DIVIDER = "       |      ";

    public BottomPanel() {
        // set the layout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // set the style
        setBackground(Color.DARK_GRAY);
        setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Styles.COLOR_TRIM));

        // add the text field
        text.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, Color.WHITE), BorderFactory.createEmptyBorder(3, 20, 3, 0)));
        text.setForeground(Color.WHITE);
        text.setFont(Styles.FONT_12PT_BOLD);
        text.setHorizontalAlignment(JLabel.CENTER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        add(text, gbc);

        // starters
        updateBottomText();

        // lazy load this thread
        LazyLoadAllSlowStuffAfterGUIRenders.add(this);
    }

    public void lazyLoad() {
        ProjectPool.addListener(new ProjectPoolListener() {

            public void projectAdded(ProjectPoolEvent e) {
                updateBottomText();
            }

            public void projectRemoved(ProjectPoolEvent e) {
                updateBottomText();
            }

            public void projectUpdated(ProjectPoolEvent e) {
                updateBottomText();
            }
        });
        DownloadPool.addListener(new DownloadPoolListener() {

            public void downloadAdded(DownloadPoolEvent e) {
                updateBottomText();
            }

            public void downloadUpdated(DownloadPoolEvent e) {
                updateBottomText();
            }

            public void downloadRemoved(DownloadPoolEvent e) {
                updateBottomText();
            }
        });
        UploadPool.addListener(new UploadPoolListener() {

            public void uploadAdded(UploadPoolEvent e) {
                updateBottomText();
            }

            public void uploadUpdated(UploadPoolEvent e) {
                updateBottomText();
            }

            public void uploadRemoved(UploadPoolEvent e) {
                updateBottomText();
            }
        });
        NetworkUtil.getStatus().addListener(new StatusTableListener() {

            public void rowsAdded(StatusTableEvent event) {
                updateBottomText();
            }

            public void rowsUpdated(StatusTableEvent event) {
                updateBottomText();
            }

            public void rowsRemoved(StatusTableEvent event) {
                updateBottomText();
            }
        });
    }

    private void updateBottomText() {
        String dataSetsString = "Data Sets: " + GUIUtil.integerFormat.format(ProjectPool.size());

        // for notifying that a download has completed
        String downloadsString = "Downloads: " + GUIUtil.integerFormat.format(DownloadPool.getDownloadingCount());
        if (DownloadPool.getQueuedCount() > 0) {
            downloadsString = downloadsString + "  (" + GUIUtil.integerFormat.format(DownloadPool.getQueuedCount()) + " Queued)";
        }

        // for notifying that a upload has completed
        String uploadsString = "Uploads: " + GUIUtil.integerFormat.format(UploadPool.getUploadingCount());
        if (UploadPool.getQueuedCount() > 0) {
            uploadsString = uploadsString + "  (" + GUIUtil.integerFormat.format(UploadPool.getQueuedCount()) + " Queued)";
        }

        int online = 0;
        for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {
            if (row.isOnline()) {
                online++;
            }
        }
        
        String onlineMsg = GUIUtil.integerFormat.format(online);
        String totalMsg = GUIUtil.integerFormat.format(NetworkUtil.getStatus().size());
        
        String serversString = "Servers: " + onlineMsg+" online, "+totalMsg+" total";

        text.setText(dataSetsString + DIVIDER + downloadsString + DIVIDER + uploadsString + DIVIDER + serversString);
    }
}
