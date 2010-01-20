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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import org.tranche.gui.DisplayTextArea;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.InformationPanel;
import org.tranche.gui.Styles;
import org.tranche.gui.project.ProjectPool;
import org.tranche.gui.util.GUIUtil;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.project.ProjectSummary;
import org.tranche.util.Text;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SelectUploaderPanel extends JPanel {

    private final Set<SelectUploaderPanelListener> listeners = new HashSet<SelectUploaderPanelListener>();
    private final JPanel panel = new JPanel();
    private final InformationPanel infoPanel = new InformationPanel();
    private BigHash hash;
    private MetaData metaData;
    private String uploaderName,  relativePath;
    private Long uploadTimestamp;
    private int selectedUploader = 0;

    public SelectUploaderPanel() {
        setLayout(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setLayout(new GridBagLayout());

        add(new GenericScrollPane(panel), BorderLayout.WEST);
        add(infoPanel, BorderLayout.CENTER);

        refresh();
    }

    public void setMetaData(BigHash hash, MetaData metaData) {
        this.hash = hash;
        this.metaData = metaData;
        try {
            this.metaData.selectUploader(uploaderName, uploadTimestamp, relativePath);
        } catch (Exception e) {
        }
        selectedUploader = metaData.getSelectedUploader();
        refresh();
    }

    public void selectStartingUploader(String uploaderName, Long uploadTimestamp, String relativePath) {
        this.uploaderName = uploaderName;
        this.uploadTimestamp = uploadTimestamp;
        this.relativePath = relativePath;
        if (metaData != null) {
            try {
                metaData.selectUploader(uploaderName, uploadTimestamp, uploaderName);
                selectedUploader = metaData.getSelectedUploader();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        refresh();
    }

    public void refresh() {
        synchronized (panel) {
            panel.removeAll();
            if (metaData != null) {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.weightx = 1;
                for (int i = 0; i < metaData.getUploaderCount(); i++) {
                    metaData.selectUploader(i);
                    final DisplayTextArea uploader = new DisplayTextArea("Uploaded by " + metaData.getSignature().getUserName() + " on " + Text.getFormattedDate(metaData.getTimestampUploaded()));
                    uploader.setBorder(BorderFactory.createCompoundBorder(Styles.UNDERLINE_BLACK, BorderFactory.createEmptyBorder(5, 5, 5, 5)));
                    uploader.setOpaque(true);
                    uploader.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    final int uploaderIndex = i;
                    uploader.addMouseListener(new MouseAdapter() {

                        @Override
                        public void mouseClicked(MouseEvent e) {
                            Thread t = new Thread() {

                                @Override
                                public void run() {
                                    if (selectedUploader != uploaderIndex) {
                                        selectedUploader = uploaderIndex;
                                        metaData.selectUploader(selectedUploader);
                                        fireUploaderChanged(metaData.getSignature().getUserName(), metaData.getTimestampUploaded(), metaData.getRelativePathInDataSet());
                                        refresh();
                                    }
                                }
                            };
                            t.setDaemon(true);
                            t.start();
                        }
                    });
                    if (selectedUploader == i) {
                        uploader.setBackground(Styles.COLOR_SELECTED_BACKGROUND);
                        uploader.setFont(Styles.FONT_11PT_BOLD);
                        try {
                            ProjectSummary ps = ProjectPool.get(hash, uploaderName, uploadTimestamp);
                            if (ps != null) {
                                infoPanel.set("Title", ps.title);
                                infoPanel.set("Size", GUIUtil.integerFormat.format(ps.files) + " files" + ", " + Text.getFormattedBytes(ps.size));
                                infoPanel.set("Description", ps.description);
                            } else if (!metaData.isProjectFile()) {
                                if (metaData.getRelativePathInDataSet() != null) {
                                    infoPanel.set("Path", metaData.getRelativePathInDataSet());
                                }
                                infoPanel.set("Name", metaData.getName());
                                infoPanel.set("Size", Text.getFormattedBytes(hash.getLength()));
                                if (metaData.getMimeType() != null) {
                                    infoPanel.set("MIME Type", metaData.getMimeType());
                                }
                            } else {
                                if (metaData.getDataSetName() != null) {
                                    infoPanel.set("Title", metaData.getDataSetName());
                                }
                                String size = "";
                                if (metaData.getDataSetFiles() > 0) {
                                    size = size + GUIUtil.integerFormat.format(metaData.getDataSetFiles()) + " files";
                                    if (metaData.getDataSetSize() > 0) {
                                        size = size + ", ";
                                    }
                                }
                                if (metaData.getDataSetSize() > 0) {
                                    size = size + Text.getFormattedBytes(metaData.getDataSetSize());
                                }
                                if (!size.equals("")) {
                                    infoPanel.set("Size", size);
                                }
                                if (metaData.getDataSetDescription() != null) {
                                    infoPanel.set("Description", metaData.getDataSetDescription());
                                }
                            }
                            if (metaData.getNextVersion() != null) {
                                infoPanel.set("Next Version", metaData.getNextVersion().toString());
                            }
                            if (metaData.getPreviousVersion() != null) {
                                infoPanel.set("Previous Version", metaData.getPreviousVersion().toString());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        uploader.setBackground(Color.WHITE);
                        uploader.setFont(Styles.FONT_11PT);
                    }
                    panel.add(uploader, gbc);
                }
                // padding to the bottom
                gbc.weighty = 2;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.gridheight = GridBagConstraints.REMAINDER;
                panel.add(Box.createVerticalStrut(1), gbc);
            }
            validate();
            repaint();
        }
    }

    public void fireUploaderChanged(String uploaderName, long uploadTimestamp, String uploadRelativePathInDataSet) {
        synchronized (listeners) {
            for (SelectUploaderPanelListener l : listeners) {
                l.uploaderChanged(uploaderName, uploadTimestamp, uploadRelativePathInDataSet);
            }
        }
    }

    public void addListener(SelectUploaderPanelListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }
}
