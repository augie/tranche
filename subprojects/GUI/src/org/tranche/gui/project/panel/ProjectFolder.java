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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tranche.project.ProjectFilePart;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ProjectFolder {

    private ProjectFolder parent;
    private String folderName;
    private Map<String, ProjectFolder> folderMap = new HashMap<String, ProjectFolder>();
    private List<ProjectFolder> folders = new LinkedList<ProjectFolder>();
    private Map<String, ProjectFilePart> partMap = new HashMap<String, ProjectFilePart>();
    private List<ProjectFilePart> parts = new LinkedList<ProjectFilePart>();

    public ProjectFolder(String folderName) {
        this.folderName = folderName;
    }

    public ProjectFolder(String folderName, ProjectFolder parent) {
        this(folderName);
        setParent(parent);
    }

    public String getName() {
        return folderName;
    }

    public boolean containsFolder(String folderName) {
        if (folderName == null) {
            return false;
        }
        return folderMap.get(folderName) != null;
    }

    public ProjectFolder getFolder(String folderName) {
        return folderMap.get(folderName);
    }

    public List<ProjectFolder> getFolders() {
        return folders;
    }

    public void addFolder(ProjectFolder folder) {
        folderMap.put(folder.getName(), folder);
        // find the place for this folder in the sorted list and add
        if (folders.isEmpty()) {
            folders.add(folder);
        } else {
            int index = 0;
            while (folder.getName().compareToIgnoreCase(folders.get(index).getName()) > 0) {
                index++;
                if (folders.size() == index) {
                    break;
                }
            }
            if (folders.size() == index) {
                folders.add(folder);
            } else {
                folders.add(index, folder);
            }
        }
    }

    public List<ProjectFilePart> getParts() {
        return parts;
    }

    public Set<ProjectFilePart> getAllParts() {
        Set<ProjectFilePart> returnParts = new HashSet<ProjectFilePart>();
        for (ProjectFolder folder : folders) {
            for (ProjectFilePart part : folder.getAllParts()) {
                returnParts.add(part);
            }
        }
        for (ProjectFilePart part : parts) {
            returnParts.add(part);
        }
        return returnParts;
    }

    public void addPart(ProjectFilePart part) {
        partMap.put(part.getRelativeName(), part);
        // find the place for this part in the sorted list and add
        if (parts.isEmpty()) {
            parts.add(part);
        } else {
            int index = 0;
            while (part.getRelativeName().compareToIgnoreCase(parts.get(index).getRelativeName()) > 0) {
                index++;
                if (parts.size() == index) {
                    break;
                }
            }
            if (parts.size() == index) {
                parts.add(part);
            } else {
                parts.add(index, part);
            }
        }
    }

    public ProjectFolder getParent() {
        return parent;
    }

    public void setParent(ProjectFolder parent) {
        this.parent = parent;
    }

    public String getPath() {
        String path = "";
        if (parent != null && !parent.getPath().equals("")) {
            path = parent.getPath() + "/";
        }
        return path + folderName;
    }

    public long getSize() {
        long size = 0;
        for (ProjectFilePart part : parts) {
            size += part.getPaddingAdjustedLength();
        }
        for (ProjectFolder folder : folders) {
            size += folder.getSize();
        }
        return size;
    }

    public long getFileCount() {
        long files = 0;
        files += parts.size();
        for (ProjectFolder folder : folders) {
            files += folder.getFileCount();
        }
        return files;
    }

    public boolean equals(ProjectFolder folder) {
        return getPath().equals(folder.getPath());
    }
}
