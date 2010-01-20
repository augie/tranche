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

import java.awt.Component;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.advanced.GenericTab;
import org.tranche.gui.advanced.ProjectsPanel;
import org.tranche.hash.BigHash;
import org.tranche.util.DebugUtil;

/**
 * A utilities that mediates between tranche modules and the GUI.
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class GUIMediatorUtil {

    private static boolean debug = false;
    private static Installable projectsInstaller = null;
    private static Installable topInstaller = null;
    private static boolean loaded = false;

    private static void lazyload() {
        // May be invoke by free-standing app
        if (GUIUtil.getAdvancedGUI() == null) {
            return;
        }

        if (loaded) {
            return;
        }
        loaded = true;

        projectsInstaller = GUIUtil.getAdvancedGUI().mainPanel.dataSetsPanel;
        topInstaller = GUIUtil.getAdvancedGUI().topPanel;
    }

    /**
     * Installs an action in the GUI.
     */
    public static void installAction(SelectableModuleAction action) {
        // May be invoke by free-standing app
        if (GUIUtil.getAdvancedGUI() == null) {
            return;
        }
        debugOut("Installing action: " + action.label + ", from module: " + action.moduleName);

        lazyload();

        GUIPolicy policy = action.getGUIPolicy();

        if (policy.isForProjects()) {
            projectsInstaller.installAction(action);
        }

        // Only install tab if module is enabled
        if (action.isTab()) {
            try {
                // Get the panel and wrap it
                JPanel panel = (JPanel) action.getMethod().invoke(null, new Object[0]);
                GenericTab tab = new GenericTab(GUIUtil.getAdvancedGUI().mainPanel, panel, action.label, action.description);
                GUIUtil.getAdvancedGUI().mainPanel.installTab(action.label, tab, action.isIsFrontTab());
            } catch (Exception ex) {
                ErrorFrame ef = new ErrorFrame();
                ef.show(ex, GUIUtil.getAdvancedGUI());
            }
        }

        if (action.isAdvancedTool()) {
            topInstaller.installAction(action);
        }
    }

    /**
     * Clears the actions from the GUI.
     */
    public static void uninstallActions() {
        debugOut("Uninstalling all actions...");
        lazyload();
        projectsInstaller.clearActions();
        topInstaller.clearActions();
        GUIUtil.getAdvancedGUI().mainPanel.uninstallTabs();
    }

    /**
     * Updates visibility of all modules based on whether or not enabled.
     */
    public static void enableInstalledModules(JPanel panel, Map<JComponent, SelectableModuleAction> map, int fileCount) {
        for (JComponent component : map.keySet()) {
            enableInstalledModule(panel, component, map.get(component), fileCount);
        }
    }

    /**
     * Mediates the visibility and availablility of component.
     */
    public static void enableInstalledModule(Component panel, JComponent component, SelectableModuleAction moduleAction, int fileCount) {
        lazyload();
        GUIPolicy policy = moduleAction.getGUIPolicy();

        // Shouldn't be visible if parent module isn't enabled
        boolean isEnabled = moduleAction.getParentModule().isEnabled;
        component.setVisible(isEnabled);

        if (!isEnabled) {
            return;        // Automatically enable if should be by default
        }
        if (GUIMediatorUtil.isActionEnabledByDefault(moduleAction)) {
            component.setEnabled(true);
            return;
        }

        // Nothing if no files selected
        if (fileCount == 0) {
            component.setEnabled(false);
            return;
        } // Single file
        else if (fileCount == 1) {

            if (!policy.anyNumberOfFiles() && !policy.singleFileOnly()) {
                component.setEnabled(false);
                return;
            }

            // TODO Use meta data to filter

            // If gets here, good
            component.setEnabled(true);
        } // Muleiple file
        else {

            if (!policy.anyNumberOfFiles() && !policy.multiFileOnly()) {
                component.setEnabled(false);
                return;
            }

            // TODO Use meta data to filter

            // If gets here, good
            component.setEnabled(true);
        }
    }

    /**
     * If a module is disabled, all the GUI elements should immediately disappear.
     */
    public static void negotiateModuleStatus(TrancheModule module) {

        // May be invoke by free-standing app
        if (GUIUtil.getAdvancedGUI() == null) {
            return;
        }
        lazyload();

        for (SelectableModuleAction action : module.getSelectableActions()) {

            // Negotiate contextual menus
            projectsInstaller.negotateActionStatus(action, module.isEnabled);
            topInstaller.negotateActionStatus(action, module.isEnabled);

            // Negotiate tabs. Enable/disable depending on whether module is enabled.
            if (action.isTab()) {
                // Main panel holds references to tabs
                GUIUtil.getAdvancedGUI().mainPanel.setInstalledTabEnabled(action.label, module.isEnabled);
            }
        }
    }

    /**
     * Uses inspection to determine method parameters, and gathers information from GUI.
     */
    public static void executeAction(SelectableModuleAction action, JComponent panel) throws Exception {
        lazyload();
        Parameters params = new Parameters(action, panel);
        action.execute(params.getParameters());
    }

    /**
     * Check whether an action should be selectable by default. Uses very simple heuristics.
     * @param action The selectable action
     * @return True if selectable action should be enable by default, else false
     */
    public static boolean isActionEnabledByDefault(SelectableModuleAction action) {
        lazyload();
//        return action.getGUIPolicy().noFileOnly() || action.getGUIPolicy().anyNumberOfFiles();
        return action.getGUIPolicy().noFileOnly();
    }

    /**
     * Uses inspection to extract the parameter information for the action.
     */
    static class Parameters {

        private SelectableModuleAction action;
        private Object[] params;
        private JComponent panel;

        public Parameters(SelectableModuleAction action, JComponent panel) {
            this.action = action;
            this.params = null;
            this.panel = panel;

            build();
        }

        private void build() {

            Method m = this.action.getMethod();

            Class[] paramClasses = m.getParameterTypes();

            // No parameters
            if (paramClasses.length == 0) {
                params = new Object[0];
                return;
            }

            // List of hashes
            if (paramClasses[0].getSimpleName().contains("List")) {

                params = new Object[1];
                List<BigHash> hashes;

                // Projects
                if (this.panel instanceof ProjectsPanel) {
                    ProjectsPanel bpp = (ProjectsPanel) panel;
                    hashes = new ArrayList();
                } // Unknown
                else {
                    hashes = new ArrayList();
                }

                params[0] = hashes;
                return;
            }

            // Map of names and hashes
            if (paramClasses[0].getSimpleName().contains("Map")) {
                params = new Object[1];
                Map<String, BigHash> hashesAndNames = new HashMap();

                if (this.panel instanceof ProjectsPanel) {
                    ProjectsPanel bpp = (ProjectsPanel) panel;
                }

                params[0] = hashesAndNames;
                return;
            }

            if (paramClasses[0].getSimpleName().contains("BigHash")) {
                params = new Object[1];
                BigHash hash = null;

                if (this.panel instanceof ProjectsPanel) {
                    ProjectsPanel bpp = (ProjectsPanel) panel;
                }

                params[0] = hash;
                return;
            }
        }

        public Object[] getParameters() {
            return this.params;
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        GUIMediatorUtil.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static final boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static final void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(GUIMediatorUtil.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}

