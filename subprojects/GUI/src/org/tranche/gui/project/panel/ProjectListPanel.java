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
package org.tranche.gui.project.panel;

import org.tranche.gui.util.GUIUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.tranche.commons.TextUtil;

import org.tranche.gui.GenericLabel;
import org.tranche.gui.GenericPopupListener;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.Styles;
import org.tranche.project.ProjectFilePart;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ProjectListPanel extends ProjectFolderViewPanel {

    private Set<ProjectFolder> currentFolderSelections;
    private ListPanel labelPanel = new ListPanel();
    private GenericPopupListener popupListener;

    public ProjectListPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setOpaque(true);

        // put the label panel into a scrollpane
        GenericScrollPane pane = new GenericScrollPane(labelPanel, GenericScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, GenericScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setBorder(Styles.BORDER_NONE);
        pane.setBackground(Color.GRAY);
        add(pane, BorderLayout.CENTER);
    }

    public ProjectListPanel(Set<ProjectFilePart> parts) {
        this();
        setParts(parts);
    }

    @Override
    public void setPopupMenu(JPopupMenu popupMenu) {
        super.setPopupMenu(popupMenu);
        popupListener = new GenericPopupListener(popupMenu);
        popupListener.setDisplayMethod(GenericPopupListener.RIGHT_CLICK_ONLY);
    }

    @Override
    public void clear() {
        super.clear();
        currentFolderSelections = null;
        for (ProjectPanelSelectionListener listener : getSelectionListeners()) {
            listener.fireSelectionChanged();
        }
    }

    @Override
    public void setCurrentFolder(ProjectFolder currentFolder) {
        super.setCurrentFolder(currentFolder);
        currentFolderSelections = new HashSet<ProjectFolder>();
        for (ProjectPanelSelectionListener listener : getSelectionListeners()) {
            listener.fireSelectionChanged();
        }
    }

    public boolean isSelected(ProjectFolder folder) {
        if (currentFolderSelections != null) {
            return currentFolderSelections.contains(folder);
        }
        return false;
    }

    public void setSelected(ProjectFolder folder, boolean selected) {
        if (folder != null) {
            if (selected) {
                currentFolderSelections.add(folder);
            } else {
                currentFolderSelections.remove(folder);
            }
            for (ProjectPanelSelectionListener listener : getSelectionListeners()) {
                listener.fireSelectionChanged();
            }
        }
    }

    @Override
    public Set<ProjectFilePart> getSelectedParts() {
        if (getParts() != null && getCurrentFolder() != null) {
            Set<ProjectFilePart> returnParts = new HashSet<ProjectFilePart>();
            for (ProjectFilePart part : getCurrentFolder().getParts()) {
                if (isSelected(part)) {
                    returnParts.add(part);
                }
            }
            for (ProjectFolder folder : currentFolderSelections) {
                for (ProjectFilePart part : folder.getAllParts()) {
                    returnParts.add(part);
                }
            }
            return returnParts;
        }
        return null;
    }

    @Override
    public void selectAll() {
        // only select all from within the current folder
        if (getCurrentFolder() != null) {
            for (ProjectFolder folder : getCurrentFolder().getFolders()) {
                setSelected(folder, true);
            }
            for (ProjectFilePart part : getCurrentFolder().getParts()) {
                setSelected(part, true);
            }
            for (Component c : labelPanel.getComponents()) {
                if (c.getClass().equals(FolderLabel.class) || c.getClass().equals(PartLabel.class)) {
                    ((JLabel) c).setBackground(Styles.COLOR_LIGHT_BLUE);
                }
            }
            for (ProjectPanelSelectionListener listener : getSelectionListeners()) {
                listener.fireSelectionChanged();
            }
        }
    }

    @Override
    public void deselectAll() {
        // only deselect all from within the current folder - all superfolders should already be deselected
        if (getCurrentFolder() != null) {
            super.deselectAll();
            currentFolderSelections = new HashSet<ProjectFolder>();
            for (Component c : labelPanel.getComponents()) {
                if (c.getClass().equals(FolderLabel.class) || c.getClass().equals(PartLabel.class)) {
                    ((JLabel) c).setBackground(Color.WHITE);
                }
            }
            for (ProjectPanelSelectionListener listener : getSelectionListeners()) {
                listener.fireSelectionChanged();
            }
        }
    }

    public void refresh() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            labelPanel.removeAll();
            if (getCurrentFolder() != null) {
                // add the current location label
                GenericLabel label = new GenericLabel("Current Folder:  /" + getCurrentFolder().getPath() + "  (" + GUIUtil.integerFormat.format(getCurrentFolder().getFileCount()) + " files, " + TextUtil.formatBytes(getCurrentFolder().getSize()) + ")");
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
                label.setFont(Styles.FONT_12PT_MONOSPACED);
                labelPanel.add(label, labelPanel.gbc);

                if (!getCurrentFolder().equals(getRootFolder())) {
                    labelPanel.addBackFolder();
                }
                for (ProjectFolder folder : getCurrentFolder().getFolders()) {
                    labelPanel.addFolder(folder);
                }
                for (ProjectFilePart part : getCurrentFolder().getParts()) {
                    labelPanel.addPart(part);
                }
            }
            labelPanel.addPadding();
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
        validate();
        repaint();
    }

    private class ListPanel extends JPanel {

        public GridBagConstraints gbc = new GridBagConstraints();
        private Component lastClickedComponent = null;

        public ListPanel() {
            setLayout(new GridBagLayout());
            setBackground(Color.WHITE);

            // set up the gridbagconstraints
            resetGridBagConstraints();
        }

        public void resetGridBagConstraints() {
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridheight = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.insets = new Insets(0, 0, 0, 0);
            gbc.ipadx = 4;
            gbc.ipady = 4;
            gbc.weightx = 1;
            gbc.weighty = 0;
        }

        public void addBackFolder() {
            final JLabel label = new GenericLabel("Back to Parent Folder");
            label.setToolTipText("Go Back");
            label.setFont(Styles.FONT_14PT_BOLD);
            label.setBackground(Color.WHITE);
            label.setFocusable(false);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 0));
            label.setIconTextGap(5);
            try {
                label.setIcon(new ImageIcon(ImageIO.read(ProjectListPanel.this.getClass().getResourceAsStream("/org/tranche/gui/image/back-22x22.gif"))));
            } catch (Exception e) {
            }
            if (popupListener != null) {
                label.addMouseListener(popupListener);
            }
            label.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(final MouseEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            if (e.getClickCount() == 2) {
                                setCurrentFolder(getCurrentFolder().getParent());
                                deselectAll();
                                refresh();
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            add(label, gbc);
        }

        public void addFolder(final ProjectFolder folder) {
            // create the label
            final FolderLabel label = new FolderLabel(folder);

            // determine whether the folder is selected
            if (isSelected(folder)) {
                label.setBackground(Styles.COLOR_LIGHT_BLUE);
            }

            // listen for clicks
            if (popupListener != null) {
                label.addMouseListener(popupListener);
            }
            label.setToolTipText(GUIUtil.integerFormat.format(folder.getFileCount()) + " files, " + TextUtil.formatBytes(folder.getSize()));
            label.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(final MouseEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            if (e.getButton() == MouseEvent.BUTTON1) {
                                if (e.getClickCount() == 1) {
                                    boolean isSelected = isSelected(folder);
                                    if (MouseEvent.getMouseModifiersText(e.getModifiers()).contains("Shift")) {
                                        if (lastClickedComponent == null) {
                                            setSelected(folder, !isSelected);
                                            if (!isSelected) {
                                                label.setBackground(Styles.COLOR_LIGHT_BLUE);
                                            } else {
                                                label.setBackground(Color.WHITE);
                                            }
                                            lastClickedComponent = label;
                                            return;
                                        }
                                        Component[] components = labelPanel.getComponents();
                                        boolean select = false;
                                        for (int i = 0; i < components.length; i++) {
                                            if (!(components[i].getClass().equals(FolderLabel.class) || components[i].getClass().equals(PartLabel.class))) {
                                                continue;
                                            }
                                            if (components[i] == lastClickedComponent || components[i] == label) {
                                                select = !select;
                                            }
                                            if (select || (!select && (components[i] == lastClickedComponent || components[i] == label))) {
                                                ((JLabel) components[i]).setBackground(Styles.COLOR_LIGHT_BLUE);
                                                if (components[i].getClass().equals(FolderLabel.class)) {
                                                    setSelected(((FolderLabel) components[i]).getFolder(), true);
                                                } else if (components[i].getClass().equals(PartLabel.class)) {
                                                    setSelected(((PartLabel) components[i]).getPart(), true);
                                                }
                                            }
                                            if (!select && (components[i] == lastClickedComponent || components[i] == label)) {
                                                break;
                                            }
                                        }
                                    } else if (!MouseEvent.getMouseModifiersText(e.getModifiers()).contains("Ctrl")) {
                                        for (Component c : labelPanel.getComponents()) {
                                            if (c.getClass().equals(FolderLabel.class) || c.getClass().equals(PartLabel.class)) {
                                                ((JLabel) c).setBackground(Color.WHITE);
                                            }
                                        }
                                        deselectAll();
                                        setSelected(folder, !isSelected);
                                        if (!isSelected) {
                                            label.setBackground(Styles.COLOR_LIGHT_BLUE);
                                        } else {
                                            label.setBackground(Color.WHITE);
                                        }
                                        lastClickedComponent = label;
                                    } else {
                                        setSelected(folder, !isSelected);
                                        if (!isSelected) {
                                            label.setBackground(Styles.COLOR_LIGHT_BLUE);
                                        } else {
                                            label.setBackground(Color.WHITE);
                                        }
                                        lastClickedComponent = label;
                                    }
                                } else if (e.getClickCount() == 2) {
                                    setCurrentFolder(folder);
                                    refresh();
                                }
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            add(label, gbc);
        }

        public void addPart(final ProjectFilePart part) {
            // get the name
            String name = part.getRelativeName();
            if (name.contains("/")) {
                name = name.substring(name.lastIndexOf("/") + 1, name.length());
            }

            // create the label
            final PartLabel label = new PartLabel(part, name);
            if (isSelected(part)) {
                label.setBackground(Styles.COLOR_LIGHT_BLUE);
            }
            label.setToolTipText(TextUtil.formatBytes(part.getPaddingAdjustedLength()));
            if (popupListener != null) {
                label.addMouseListener(popupListener);
            }
            label.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(final MouseEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            if (e.getButton() == MouseEvent.BUTTON1) {
                                if (e.getClickCount() == 1) {
                                    boolean isSelected = isSelected(part);
                                    if (MouseEvent.getMouseModifiersText(e.getModifiers()).contains("Shift")) {
                                        if (lastClickedComponent == null) {
                                            setSelected(part, !isSelected);
                                            if (!isSelected) {
                                                label.setBackground(Styles.COLOR_LIGHT_BLUE);
                                            } else {
                                                label.setBackground(Color.WHITE);
                                            }
                                            lastClickedComponent = label;
                                            return;
                                        }
                                        Component[] components = labelPanel.getComponents();
                                        boolean select = false;
                                        for (int i = 0; i < components.length; i++) {
                                            if (!(components[i].getClass().equals(FolderLabel.class) || components[i].getClass().equals(PartLabel.class))) {
                                                continue;
                                            }
                                            if (components[i] == lastClickedComponent || components[i] == label) {
                                                select = !select;
                                            }
                                            if (select || (!select && (components[i] == lastClickedComponent || components[i] == label))) {
                                                ((JLabel) components[i]).setBackground(Styles.COLOR_LIGHT_BLUE);
                                                if (components[i].getClass().equals(FolderLabel.class)) {
                                                    setSelected(((FolderLabel) components[i]).getFolder(), true);
                                                } else if (components[i].getClass().equals(PartLabel.class)) {
                                                    setSelected(((PartLabel) components[i]).getPart(), true);
                                                }
                                            }
                                            if (!select && (components[i] == lastClickedComponent || components[i] == label)) {
                                                break;
                                            }
                                        }
                                    } else if (!MouseEvent.getMouseModifiersText(e.getModifiers()).contains("Ctrl")) {
                                        for (Component c : labelPanel.getComponents()) {
                                            if (c.getClass().equals(FolderLabel.class) || c.getClass().equals(PartLabel.class)) {
                                                ((JLabel) c).setBackground(Color.WHITE);
                                            }
                                        }
                                        deselectAll();
                                        setSelected(part, !isSelected);
                                        if (!isSelected) {
                                            label.setBackground(Styles.COLOR_LIGHT_BLUE);
                                        } else {
                                            label.setBackground(Color.WHITE);
                                        }
                                        lastClickedComponent = label;
                                    } else {
                                        setSelected(part, !isSelected);
                                        if (!isSelected) {
                                            label.setBackground(Styles.COLOR_LIGHT_BLUE);
                                        } else {
                                            label.setBackground(Color.WHITE);
                                        }
                                        lastClickedComponent = label;
                                    }
                                }
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            add(label, gbc);
        }

        public void addPadding() {
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridheight = GridBagConstraints.REMAINDER;
            gbc.ipadx = 0;
            gbc.ipady = 5;
            gbc.weighty = 10;
            JPanel panel = new JPanel();
            panel.setBackground(Color.WHITE);
            panel.setOpaque(true);
            if (popupListener != null) {
                panel.addMouseListener(popupListener);
            }
            add(panel, gbc);
            labelPanel.resetGridBagConstraints();
        }
    }

    private class FolderLabel extends GenericLabel {

        private ProjectFolder folder;

        public FolderLabel(ProjectFolder folder) {
            super(folder.getName());
            this.folder = folder;
            setFont(Styles.FONT_14PT_BOLD);
            setBackground(Color.WHITE);
            setFocusable(false);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 0));
            setIconTextGap(5);
            try {
                setIcon(new ImageIcon(ImageIO.read(FolderLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/folder-22x22.gif"))));
            } catch (Exception e) {
            }
        }

        public ProjectFolder getFolder() {
            return folder;
        }
    }

    private class PartLabel extends GenericLabel {

        private ProjectFilePart part;

        public PartLabel(ProjectFilePart part, String name) {
            super(name);
            this.part = part;
            setFont(Styles.FONT_14PT);
            setBackground(Color.WHITE);
            setFocusable(false);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 0));
            setIconTextGap(5);
            try {
                if (name.toLowerCase().endsWith(".txt") || name.toLowerCase().endsWith(".rtf") || name.toLowerCase().endsWith(".rtx") || name.toLowerCase().endsWith(".inf") || name.toLowerCase().endsWith(".ini")) {
                    setIcon(new ImageIcon(ImageIO.read(PartLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/text-x-generic-22x22.gif"))));
                } else if (name.toLowerCase().endsWith(".zip") || name.toLowerCase().endsWith(".tgz") || name.toLowerCase().endsWith(".tar") || name.toLowerCase().endsWith(".tar.gz") || name.toLowerCase().endsWith(".gzip")) {
                    setIcon(new ImageIcon(ImageIO.read(PartLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/package-x-generic-22x22.gif"))));
                } else if (name.toLowerCase().endsWith(".mp3") || name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".midi")) {
                    setIcon(new ImageIcon(ImageIO.read(PartLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/audio-x-generic-22x22.gif"))));
                } else if (name.toLowerCase().endsWith(".gif") || name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".tif") || name.toLowerCase().endsWith(".tiff") || name.toLowerCase().endsWith(".bmp")) {
                    setIcon(new ImageIcon(ImageIO.read(PartLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/image-x-generic-22x22.gif"))));
                } else if (name.toLowerCase().endsWith(".wmv") || name.toLowerCase().endsWith(".asf") || name.toLowerCase().endsWith(".mov") || name.toLowerCase().endsWith(".mp4") || name.toLowerCase().endsWith(".mpeg") || name.toLowerCase().endsWith(".qt") || name.toLowerCase().endsWith(".avi")) {
                    setIcon(new ImageIcon(ImageIO.read(PartLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/video-x-generic-22x22.gif"))));
                } else if (name.toLowerCase().endsWith(".htm") || name.toLowerCase().endsWith(".html") || name.toLowerCase().endsWith(".php") || name.toLowerCase().endsWith(".asp") || name.toLowerCase().endsWith(".jsp") || name.toLowerCase().endsWith(".css") || name.toLowerCase().endsWith(".xml") || name.toLowerCase().endsWith(".shtml")) {
                    setIcon(new ImageIcon(ImageIO.read(PartLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/text-html-22x22.gif"))));
                } else if (name.toLowerCase().endsWith(".pdf")) {
                    setIcon(new ImageIcon(ImageIO.read(PartLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/pdf-22x22.gif"))));
                } else if (name.toLowerCase().endsWith(".odt") || name.toLowerCase().endsWith(".doc")) {
                    setIcon(new ImageIcon(ImageIO.read(PartLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/x-office-document-22x22.gif"))));
                } else if (name.toLowerCase().endsWith(".odp") || name.toLowerCase().endsWith(".ppt")) {
                    setIcon(new ImageIcon(ImageIO.read(PartLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/x-office-presentation-22x22.gif"))));
                } else if (name.toLowerCase().endsWith(".ods") || name.toLowerCase().endsWith(".xls") || name.toLowerCase().endsWith(".csv")) {
                    setIcon(new ImageIcon(ImageIO.read(PartLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/x-office-spreadsheet-22x22.gif"))));
                } else if (name.toLowerCase().endsWith(".exe")) {
                    setIcon(new ImageIcon(ImageIO.read(PartLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/application-x-executable-22x22.gif"))));
                } else {
                    setIcon(new ImageIcon(ImageIO.read(PartLabel.this.getClass().getResourceAsStream("/org/tranche/gui/image/text-x-generic-template-22x22.gif"))));
                }
            } catch (Exception e) {
            }
        }

        public ProjectFilePart getPart() {
            return part;
        }
    }
}
