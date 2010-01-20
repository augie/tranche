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
package org.tranche.gui.project;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import org.tranche.gui.GenericTable;
import org.tranche.gui.IconRenderer;
import org.tranche.gui.util.GUIUtil;
import org.tranche.hash.BigHash;
import org.tranche.project.ProjectSummary;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith <bryanesmith at gmail dot com>
 */
public class ProjectsTable extends GenericTable implements ClipboardOwner {

    public ProjectsTableModel model = new ProjectsTableModel(this);
    private ProjectsPopupMenu menu = new ProjectsPopupMenu(this);

    public ProjectsTable() {
        setModel(model);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // the maximum width of the project size and file count columns
        getColumnModel().getColumn(1).setMaxWidth(20);
        getColumnModel().getColumn(2).setMaxWidth(20);
        getColumnModel().getColumn(3).setMaxWidth(150);
        getColumnModel().getColumn(4).setMaxWidth(150);
        getColumnModel().getColumn(5).setMaxWidth(150);
        getColumnModel().getColumn(6).setMaxWidth(200);

        // Icon renderer for the icon column (encrypted projects)
        getColumnModel().getColumn(1).setCellRenderer(new IconRenderer("/org/tranche/gui/image/system-lock-screen-16x16.gif"));
        getColumnModel().getColumn(2).setCellRenderer(new IconRenderer("/org/tranche/gui/image/user-trash-full-16x16.gif"));

        // for popup menu
        addMouseListener(new ProjectPopupListener(menu));
    }

    public Collection<BigHash> getSelectedHashes() {
        Set<BigHash> hashes = new HashSet<BigHash>();
        int[] rows = getSelectedRows();
        for (int index = rows.length - 1; index >= 0; index--) {
            hashes.add(model.getHash(rows[index]));
        }
        return Collections.unmodifiableCollection(hashes);
    }

    public Collection<ProjectSummary> getSelected() {
        Set<ProjectSummary> summaries = new HashSet<ProjectSummary>();
        int[] rows = getSelectedRows();
        for (int index = rows.length - 1; index >= 0; index--) {
            summaries.add(model.getRow(rows[index]));
        }
        return Collections.unmodifiableCollection(summaries);
    }

    public ProjectsPopupMenu getMenu() {
        return menu;
    }

    public void copySelected() {
        for (BigHash hash : getSelectedHashes()) {
            GUIUtil.copyToClipboard(hash.toString(), null);
        }
    }

    public void showSelected() {
        for (BigHash hash : getSelectedHashes()) {
            GUIUtil.showHash(hash, this, null);
        }
    }

    public void showSelectedInfo() {
        for (ProjectSummary ps : getSelected()) {
            GUIUtil.showMoreInfo(ps, null);
        }
    }

    public void openSelected() {
        for (BigHash hash : getSelectedHashes()) {
            GUIUtil.openProjectToolGUI(hash, null);
        }
    }

    public void downloadSelected() {
        for (ProjectSummary ps : getSelected()) {
            GUIUtil.openGetFileToolWizard(ps.hash, ps.uploader, ps.uploadTimestamp, null, null);
        }
    }

    @Override
    public ProjectsTableModel getModel() {
        return model;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        String returnVal = null;
        if (row >= 0) {
            int column = columnAtPoint(e.getPoint());
            ProjectSummary ps = model.getRow(row);
            boolean showMetaInfo = !ps.isEncrypted || (ps.isEncrypted && ps.shareMetaDataIfEncrypted);

            // if the column is the signers of the project, then show
            // all of their names as the tool tip text
            if (column == getColumnModel().getColumnIndex(ProjectsTableModel.COLUMN_UPLOADER) && ps.uploader != null && showMetaInfo) {
                returnVal = ps.uploader;
            } else if (column == 1) {
                if (ps.isEncrypted) {
                    returnVal = "Encrypted";
                } else {
                    returnVal = "Unencrypted";
                }
            } else if (column == 2) {
                if (ps.isHidden) {
                    returnVal = "Hidden";
                } else {
                    returnVal = "Visible";
                }
            } // otherwise, show the description
            else if (showMetaInfo) {
                returnVal = model.getDescription(row);
            }
        }

        // if the text for the tool tip text is too long, shorten it.
        if (returnVal != null) {
            if (returnVal.length() > 80) {
                returnVal = returnVal.substring(0, 80) + "...";
            } else if (returnVal.equals("")) {
                returnVal = null;
            }
        }

        return returnVal == null ? null : returnVal;
    }

    private class ProjectPopupListener extends MouseAdapter {

        private JPopupMenu menu;

        public ProjectPopupListener(JPopupMenu menu) {
            this.menu = menu;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }

        private void showPopup(MouseEvent e) {
            boolean isTimeToPopup = e.isPopupTrigger() || (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK;
            // if this is a double-click, open the selected project
            if (e.getClickCount() == 2) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        openSelected();
                    }
                };
                t.setDaemon(true);
                t.start();
            } // if this is a right-click, show the popup
            else if (isTimeToPopup) {
                menu.show(e.getComponent(), e.getX(), e.getY());
            } else if ((e.getModifiersEx() & MouseEvent.MOUSE_RELEASED) == MouseEvent.MOUSE_RELEASED) {
                menu.hide();
            }
        }
    }

    // the following are for the clipboardowner abstract
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
