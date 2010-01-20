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

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.project.ProjectFile;
import org.tranche.project.ProjectFilePart;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ProjectPanel extends JPanel {

    // static identifiers for view types
    public static final int VIEW_TREE = 1;
    public static final int VIEW_ICONS = 2;
    public static final int VIEW_LIST = 3;
    public static final int VIEW_TILES = 4;
    public static final int VIEW_TABLE = 5;
    // local variables
    private int view;
    private BigHash hash;
    private MetaData metaData;
    private ProjectFile projectFile;
    private String filterText = "";
    private JPopupMenu popupMenu;
    // the project view panels
    private Map<Integer, ProjectViewPanel> viewPanels = new HashMap<Integer, ProjectViewPanel>();
    private List<ProjectPanelSelectionListener> selectionListeners = new ArrayList<ProjectPanelSelectionListener>();

    public ProjectPanel() {
        setLayout(new BorderLayout());
        setView(VIEW_LIST);
    }

    public ProjectPanel(BigHash hash, ProjectFile projectFile, MetaData metaData) {
        this();
        setProject(hash, projectFile, metaData);
    }

    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void setPopupMenu(JPopupMenu popupMenu) {
        this.popupMenu = popupMenu;
        viewPanels.get(view).setPopupMenu(popupMenu);
    }

    public void addSelectionListener(ProjectPanelSelectionListener listener) {
        selectionListeners.add(listener);
        for (ProjectViewPanel panel : viewPanels.values()) {
            panel.setSelectionListeners(selectionListeners);
        }
    }

    public void clear() {
        hash = null;
        projectFile = null;
        metaData = null;
        viewPanels.get(view).clear();
        viewPanels.get(view).refresh();
        validate();
        repaint();
    }

    public boolean isProjectLoaded() {
        return (projectFile != null);
    }

    public ProjectFile getProjectFile() {
        return projectFile;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setProject(BigHash hash, ProjectFile projectFile, MetaData metaData) {
        this.hash = hash;
        this.projectFile = projectFile;
        this.metaData = metaData;
        // make sure the panel is showing and parts are set
        setView(getView());
    }

    public int getView() {
        return view;
    }

    public void setView(int view) {
        this.view = view;

        // create the panel if it hasn't been created yet
        if (view == ProjectPanel.VIEW_ICONS) {
            if (viewPanels.get(view) == null) {
                viewPanels.put(view, new ProjectIconsPanel());
            }
        } else if (view == ProjectPanel.VIEW_LIST) {
            if (viewPanels.get(view) == null) {
                viewPanels.put(view, new ProjectListPanel());
            }
        } else if (view == ProjectPanel.VIEW_TABLE) {
            if (viewPanels.get(view) == null) {
                viewPanels.put(view, new ProjectTablePanel());
            }
        } else if (view == ProjectPanel.VIEW_TILES) {
            if (viewPanels.get(view) == null) {
                viewPanels.put(view, new ProjectTilesPanel());
            }
        } else if (view == ProjectPanel.VIEW_TREE) {
            if (viewPanels.get(view) == null) {
                viewPanels.put(view, new ProjectTreePanel());
            }
        }

        // set the variables in the panel
        viewPanels.get(view).setSelectionListeners(selectionListeners);
        viewPanels.get(view).setPopupMenu(popupMenu);

        // show the view panel
        removeAll();
        //add(menuBar, BorderLayout.NORTH);
        if (projectFile != null) {
            viewPanels.get(view).setParts(projectFile.getParts());
            viewPanels.get(view).setFilter(getFilterText());
        } else {
            viewPanels.get(view).clear();
        }
        viewPanels.get(view).refresh();
        add(viewPanels.get(view), BorderLayout.CENTER);
        validate();
        repaint();
    }

    public void refresh() {
        viewPanels.get(view).refresh();
    }

    public String getFilterText() {
        return filterText;
    }

    public void setFilter(String filterText) {
        this.filterText = filterText.toLowerCase();
        viewPanels.get(view).setFilter(this.filterText);
    }

    public Set<ProjectFilePart> getSelectedParts() {
        return viewPanels.get(view).getSelectedParts();
    }

    public Set<ProjectFilePart> getFilteredParts() {
        return viewPanels.get(view).getFilteredParts();
    }

    public void selectAll() {
        viewPanels.get(view).selectAll();
    }

    public void deselectAll() {
        viewPanels.get(view).deselectAll();
    }
}
