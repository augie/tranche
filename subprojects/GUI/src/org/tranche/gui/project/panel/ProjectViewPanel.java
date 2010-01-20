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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.tranche.project.ProjectFilePart;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public abstract class ProjectViewPanel extends JPanel {

    private JPopupMenu popupMenu;
    private List<ProjectPanelSelectionListener> selectionListeners;
    private Set<ProjectFilePart> parts;
    private Set<ProjectFilePart> partSelections;
    private Pattern filter = Pattern.compile("");
    private String filterText = "";

    public abstract void refresh();

    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void setPopupMenu(JPopupMenu popupMenu) {
        this.popupMenu = popupMenu;
    }

    public Set<ProjectFilePart> getSelectedParts() {
        return partSelections;
    }

    public Set<ProjectFilePart> getParts() {
        return parts;
    }

    public List<ProjectPanelSelectionListener> getSelectionListeners() {
        return selectionListeners;
    }

    public void setSelectionListeners(List<ProjectPanelSelectionListener> selectionListeners) {
        this.selectionListeners = selectionListeners;
    }

    public void clear() {
        parts = null;
        partSelections = null;
        for (ProjectPanelSelectionListener listener : selectionListeners) {
            listener.fireSelectionChanged();
        }
    }

    public void setParts(Set<ProjectFilePart> parts) {
        this.parts = parts;
        this.partSelections = new HashSet<ProjectFilePart>();
        for (ProjectPanelSelectionListener listener : selectionListeners) {
            listener.fireSelectionChanged();
        }
    }

    public boolean isSelected(ProjectFilePart part) {
        if (partSelections != null) {
            return partSelections.contains(part);
        }
        return false;
    }

    public void setSelected(ProjectFilePart part, Boolean selected) {
        if (parts != null) {
            if (selected) {
                partSelections.add(part);
            } else {
                partSelections.remove(part);
            }
            for (ProjectPanelSelectionListener listener : selectionListeners) {
                listener.fireSelectionChanged();
            }
        }
    }

    public void selectAll() {
        if (parts != null) {
            for (ProjectFilePart part : parts) {
                partSelections.add(part);
            }
            for (ProjectPanelSelectionListener listener : selectionListeners) {
                listener.fireSelectionChanged();
            }
        }
    }

    public void deselectAll() {
        if (parts != null) {
            partSelections = new HashSet<ProjectFilePart>();
            for (ProjectPanelSelectionListener listener : selectionListeners) {
                listener.fireSelectionChanged();
            }
        }
    }

    public Pattern getFilter() {
        return filter;
    }

    public String getFilterText() {
        return filterText;
    }

    public void setFilter(String filterText) {
        this.filterText = filterText.toLowerCase();
        try {
            this.filter = Pattern.compile(this.filterText);
        } catch (Exception e) {
            // do nothing
        }
    }

    public boolean filter(ProjectFilePart part) {
        if (part.getRelativeName() != null && filter.matcher(part.getRelativeName().toLowerCase()).find()) {
            return true;
        } else {
            return false;
        }
    }

    public Set<ProjectFilePart> getFilteredParts() {
        if (parts != null) {
            Set<ProjectFilePart> returnParts = new HashSet<ProjectFilePart>();
            for (ProjectFilePart part : parts) {
                if (filter(part)) {
                    returnParts.add(part);
                }
            }
            return returnParts;
        }
        return null;
    }
}
