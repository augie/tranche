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

import java.awt.Cursor;
import java.util.Set;
import org.tranche.project.ProjectFilePart;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public abstract class ProjectFolderViewPanel extends ProjectViewPanel {

    private ProjectFolder currentFolder,  projectRoot;

    public ProjectFolderViewPanel() {
    }

    @Override
    public void setParts(Set<ProjectFilePart> parts) {
        super.setParts(parts);
        // set up the project file parts tree
        setUpPartsTree();
    }

    public synchronized void setUpPartsTree() {
        if (ProjectFolderViewPanel.this.getParts() != null) {
            // set up the project folder structure
            projectRoot = new ProjectFolder("");
            setCurrentFolder(projectRoot);

            // for all the parts
            for (ProjectFilePart part : getFilteredParts()) {

                ProjectFolder folder = projectRoot;
                String partName = part.getRelativeName();

                // go through the relative name making folders when necessary
                while (partName.contains("/")) {
                    String folderName = partName.substring(0, partName.indexOf("/"));
                    if (!folder.containsFolder(folderName)) {
                        ProjectFolder newFolder = new ProjectFolder(folderName, folder);
                        folder.addFolder(newFolder);
                        folder = newFolder;
                    } else {
                        folder = folder.getFolder(folderName);
                    }
                    partName = partName.substring(partName.indexOf("/") + 1);
                }

                // add the part to the furthest folder down the line
                folder.addPart(part);
            }

            for (ProjectPanelSelectionListener listener : getSelectionListeners()) {
                listener.fireSelectionChanged();
            }
        }
    }

    @Override
    public void setFilter(String filterText) {
        super.setFilter(filterText);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setUpPartsTree();
        setCursor(Cursor.getDefaultCursor());
    }

    public ProjectFolder getCurrentFolder() {
        return currentFolder;
    }

    public void setCurrentFolder(ProjectFolder currentFolder) {
        this.currentFolder = currentFolder;
    }

    public ProjectFolder getRootFolder() {
        return projectRoot;
    }

    @Override
    public synchronized void clear() {
        super.clear();
        currentFolder = null;
        projectRoot = null;
    }
}
