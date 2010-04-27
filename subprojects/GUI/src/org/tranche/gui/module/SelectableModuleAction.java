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
package org.tranche.gui.module;

import java.lang.reflect.Method;
import java.util.List;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.annotations.Todo;

/**
 * Encapsulates a single annotated method. Every TrancheModule has a collection of these.
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
@Todo(desc = "MetaDataFilter not implemented; low priority --Bryan", day = 0, month = 0, year = 0, author = "unknown")
public class SelectableModuleAction {

    public String label,  description;
    private Method method;
    String moduleName;
    /**
     * The following variables are for selectable options; See LeftMenuAnnotation and
     * PopupMenuAnnotation for more information.
     */
    private GUIPolicy policy = new GUIPolicy();
    public String fileExtension,  mdAnnotation,  selectionMode;
    private FileFilter fileFilter;
    private MetaDataFilter metaDataFilter;
    /**
     * The following variables are for tabs; See TabAnnotation for more information.
     */
    // Set to true if TabAnnotation, which is just a signature annotation.
    private boolean isTab = false;    // Set to true if is a tab that goes to front. See TabAnnotation for more informaiton.
    private boolean isFrontTab = false;
    private boolean isAdvancedTool = false;
    /**
     * The following variables are for toString(); used to write preferences concerning
     * modules to disk.
     */
    public static final String FIELD_DELIMITER = "<ACTION_PART>";
    private static final String POPUP_ANNOTATION = "POPUP_ANNOTATION:";
    private static final String LEFT_MENU_ANNOTATION = "LEFT_MENU_ANNOTATION:";

    public SelectableModuleAction(String moduleName, Method action, String fileExtension, String mdAnnotation, String selectionMode, String label, String description) {
        this.moduleName = moduleName;
        this.fileExtension = fileExtension;
        this.mdAnnotation = mdAnnotation;
        this.selectionMode = selectionMode;
        this.label = label;
        this.description = description;

        // Create the filters
        this.fileFilter = new FileFilter(this.fileExtension);
        this.metaDataFilter = new MetaDataFilter(this.mdAnnotation);

        this.method = action;
    }

    /**
     * Execute the action.
     * TODO: String for now, later will be more generic
     * TODO: Null return for now, later will need to return an object
     */
    public void execute(Object[] args) throws Exception {

        // Create an instance of containing class
//        Class c = this.method.getDeclaringClass();
//        Object o = c.newInstance();

        this.getMethod().invoke(null, args);
    }

    /**
     * Set the module's GUI policy. See GUIPolicy for more information.
     */
    public void setGUIPolicy(GUIPolicy policy) {
        this.policy = policy;
    }

    public GUIPolicy getGUIPolicy() {
        return this.policy;
    }

    /**
     * Returns the parent module of the selectable action.
     */
    public TrancheModule getParentModule() {
        return TrancheModulesUtil.getModuleByName(moduleName);
    }

    public String toString() {

        StringBuffer buffer = new StringBuffer();

        buffer.append(this.getMethod().getDeclaringClass().getName() + "->" + this.getMethod().getName() + this.FIELD_DELIMITER);
        buffer.append(this.fileExtension + this.FIELD_DELIMITER);
        buffer.append(this.mdAnnotation + this.FIELD_DELIMITER);
        buffer.append(this.selectionMode + this.FIELD_DELIMITER);
        buffer.append(this.label + this.FIELD_DELIMITER);
        buffer.append(this.description + this.FIELD_DELIMITER);
        buffer.append(this.policy);

        return buffer.toString();
    }

    public static SelectableModuleAction createFromString(String moduleName, String string) throws ClassNotFoundException, Exception {

        SelectableModuleAction action = null;

        // Fields for SelectModuleAction token, probably from a preferences file
        String[] tokens = string.split(FIELD_DELIMITER);

        // Extract the class name and method name
        String[] moduleTokens = tokens[0].split("->");

        // Try to rebuild the module
        Class module = Class.forName(moduleTokens[0]);
        Method method = null;

        // Try to find the method
        for (Method m : module.getMethods()) {
            if (m.getName().equals(moduleTokens[1])) {
                method = m;
                break;
            }
        }

        if (method == null) {
            throw new RuntimeException("Couldn't find method \"" + moduleTokens[1] + "\" in class \"" + moduleTokens[0] + "\"");
        }
        action = new SelectableModuleAction(
                moduleName,
                method,
                tokens[1],
                tokens[2],
                tokens[3],
                tokens[4],
                tokens[5]);

        GUIPolicy policy = GUIPolicy.createFromString(tokens[5]);
        action.setGUIPolicy(policy);

        return action;
    }

    /**
     * Returns true if file name meets standards.
     */
    public boolean acceptByFilename(String filename) {
        return this.fileFilter.accept(filename);
    }

    /**
     * Returns true if file name meets standards.
     */
    public boolean acceptByFilenames(List<String> filenames) {

        for (String filename : filenames) {
            if (!this.fileFilter.accept(filename)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Matches on module name and action label only.
     */
    public boolean equals(SelectableModuleAction action) {
        return this.moduleName.equals(action.moduleName) && this.label.equals(action.label);
    }

    /**
     * Returns true if meta data annotations meet standards.
     */
    public boolean acceptByMetaData(MetaData metadata) {
        return this.metaDataFilter.accept(metadata);
    }

    public Method getMethod() {
        return method;
    }

    /**
     * Returns true only if action is a tab.
     */
    public boolean isTab() {
        return isTab;
    }

    /**
     * Set to true if action is tab (if has TabAnnotation signature)
     */
    public void setIsTab(boolean isTab) {
        this.isTab = isTab;
    }

    public boolean isIsFrontTab() {
        return isFrontTab;
    }

    public void setIsFrontTab(boolean isFrontTab) {
        this.isFrontTab = isFrontTab;
    }

    /**
     * Returns true only if action is an Advanced Tool.
     */
    public boolean isAdvancedTool() {
        return isAdvancedTool;
    }

    /**
     * Set to true if action is an Advanced Tool (has the AdvancedToolsAnnotation signature)
     */
    public void setIsAdvancedTool(boolean isAdvancedTool) {
        this.isAdvancedTool = isAdvancedTool;
    }
}

/**
 * Helper class for determining whether files acceptable by action.
 */
class FileFilter {

    String extensions;

    public FileFilter(String extensions) {
        this.extensions = extensions.trim();
    }

    public boolean accept(String filename) {
        if (this.extensions.equals("*")) {
            return true;        // Get the extension for the file being filtered
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1);

        // Return true if somewhere in 
        return this.extensions.toLowerCase().contains(extension.toLowerCase());
    }
}

/**
 * Helper class for determining if meta data matches required standards.
 */
class MetaDataFilter {

    String metaDataAnnotations;

    public MetaDataFilter(String metaDataAnnotations) {
        this.metaDataAnnotations = metaDataAnnotations.trim();
    }

    public boolean accept(MetaData md) {
        if (this.metaDataAnnotations.equals("*")) {
            return true;        // Check the annotations
        }
        for (MetaDataAnnotation a : md.getAnnotations()) {

            // Split the rules on the vertical bar
            String[] rules = this.metaDataAnnotations.split("[|]");

            for (String rule : rules) {

                // Get the name and value parts of rule
                String[] parts = rule.split("->");
                String name = parts[0];
                String value = parts[1];

                // If name matches and value is wildcard
                if (name.equals(a.getName()) && value.equals("*")) {
                    return true;                // If name and value matche
                }
                if (name.equals(a.getName()) && value.equals(a.getValue())) {
                    return true;
                }
            }
        }

        return false;
    }
}
