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

import java.io.File;
import java.io.FileInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.tranche.exceptions.TodoException;
import org.tranche.get.GetFileTool;
import org.tranche.gui.ConfigureTrancheGUI;
import org.tranche.gui.util.GUIUtil;
import org.tranche.meta.MetaDataCache;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 * Utility that reads in modules, detects selectable actions, etc.
 * @author Bryan Smith <bryanesmith at gmail.com>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class TrancheModulesUtil {

    private static final boolean isDebug = false;
    /**
     * <p>Modules to ignore because they are deprecated, etc.</p>
     * <p>Note they should be deleted from client; haven't addressed how to do this best yet.</p>
     */
    private static final String[] DEPRECATED_MODULE_NAMES = {
        "AdvancedSearchModule.jar",
        "ProteomeCommons.org-Tags.jar"
    };
    /**
     * Disk location for modules.
     */
    public static final File MODULE_DIRECTORY = new File(GUIUtil.getGUIDirectory(), "modules");
    private static final Set<BigHash> modulesToLoadOnStartup = new HashSet<BigHash>();
    private static List<TrancheModule> modules = new ArrayList();
    /**
     * All modules that have install hooks (e.g., native binaries, additional JARs) are queued up.
     */
    private static List<TrancheModule> installRequiredQueue = new ArrayList();
    /**
     * <p>When reading preferences, class may not be available yet (hasn't been discoverd from module jar).</p>
     * <p>Store enabled values here, and apply after read from disk.</p>
     */
    private static final Map<String, Boolean> isEnabledFlagQueue = new HashMap();
    /**
     * Used for lazy-loading as well as reloading modules.
     */
    private static boolean loaded = false;
    private static boolean wasLoadedOnce = false;
    /**
     * Maps TrancheModules to JAR files
     */
    private static Map<String, String> modulesJARMap = new HashMap();
    private static boolean isMostRecentInstalled = true;

    /**
     * Adds any missing modules:
     * 1. Default
     * 2. From directory.
     */
    private static synchronized void lazyload() {

        // If reloaded, variable will be set to false
        if (loaded || GUIUtil.getAdvancedGUI() == null) {
            return;
        }
        try {

            // read from the configuration
            String startupModules = ConfigureTrancheGUI.get(ConfigureTrancheGUI.CATEGORY_GUI, ConfigureTrancheGUI.PROP_STARTUP_MODULES);
            if (startupModules != null && !startupModules.equals("")) {
                for (String hashString : startupModules.split(",")) {
                    try {
                        modulesToLoadOnStartup.add(BigHash.createHashFromString(hashString));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // If somethings are not installed correctly, will need to reload modules
            // when done.
            boolean isReloadWhenFinished = false;

            TrancheModule t1 = null;

            // Load any installed modules
            if (!MODULE_DIRECTORY.exists()) {
                MODULE_DIRECTORY.mkdirs();
            }

            for (BigHash hash : TrancheModulesUtil.modulesToLoadOnStartup) {
                GetFileTool gft = new GetFileTool();
                try {
                    if (true) {
                        throw new TodoException();
                    }
//                    // Check for new version
//                    BigHash newHash = Widgets.getNewVersion(hash);
//                    if (newHash != null) {
//                        hash = newHash;
//                    }

                    MetaData metaData = MetaDataCache.get(hash, true);
                    gft.setHash(hash);

                    File saveAs = new File(MODULE_DIRECTORY, metaData.getName());

                    // Delete the old version, but only once during instance
                    final boolean exists = saveAs.exists();
                    if (exists && !wasLoadedOnce) {
                        saveAs.delete();
                        gft.setSaveFile(saveAs);
                        gft.getFile();
                        System.err.println("NOTE: Replaced existing module: " + saveAs.getAbsolutePath());
                    } else if (!exists) {
                        gft.setSaveFile(saveAs);
                        gft.getFile();
                        System.err.println("NOTE: Downloaded module: " + saveAs.getAbsolutePath());
                    } else {
                        System.err.println("NOTE: Not going to delete the following module since might have been recently added or was already replaced: " + saveAs.getAbsolutePath());
                    }

                } catch (Exception e) {
                    if (isDebug) {
                        e.printStackTrace();
                    }
                }
            }

            printTracer("Lazying loading modules from " + MODULE_DIRECTORY.getAbsolutePath());
            JAR:
            for (File jar : MODULE_DIRECTORY.listFiles()) {
                // Skip if not a jar
                if (!jar.getName().endsWith("jar")) {
                    continue JAR;
                }

                // Check to see whether in ignore collection
                for (String ignore : DEPRECATED_MODULE_NAMES) {
                    final boolean matches = ignore.equals(jar.getName());
                    printTracer("... comparing jar to ignore<" + ignore + "> to jar <" + jar.getName() + ">, match?: " + matches);
                    if (matches) {
                        // Delete it!
                        printTracer("Found deprecated module, attempting to delete. (If appears again next time tool ran, delete failed.)");
                        IOUtil.safeDelete(jar);
                        continue JAR;
                    }
                }

                printTracer("");
                printTracer("==============================================================");
                printTracer("Loading module: " + jar.getAbsolutePath());
                printTracer("==============================================================");
                printTracer("");

                /**
                 * URLClassLoader does NOT release resources once loaded. To solve this, hack
                 * is to copy JAR to temp directory and load from that. That way, a JAR
                 * can be deleted from the modules directory, even if it is still live in the temp
                 * directory.
                 *
                 * This behavior only happens on Win32 since the OS protects from deleting a file
                 * with an open handle.
                 */
                File jarCopy = TempFileUtil.createTemporaryFile(".jar");

                URLClassLoader ucl = null;

                FileInputStream fis = null;
                JarInputStream jis = null;
                URL jarURL = null;
                try {
                    // Copy the JAR to temp so can at least remove JAR from modules dir in Win32
                    IOUtil.copyFile(jar, jarCopy);

                    printTracer("Copied jar to: " + jarCopy.getAbsolutePath());

                    fis = new FileInputStream(jarCopy);
                    jis = new JarInputStream(fis);
                    jarURL = jarCopy.toURI().toURL();

                    // Instantiate so can get class loader
                    TrancheModulesUtil instantiation = new TrancheModulesUtil();

                    ucl = new URLClassLoader(new URL[]{jarURL}, instantiation.getClass().getClassLoader());
                    for (JarEntry je = jis.getNextJarEntry(); je != null; je = jis.getNextJarEntry()) {
                        // only look for class files
                        if (!je.getName().endsWith(".class")) {
                            printTracer("Skipping jar entry that's not a class: " + je.getName());
                            continue;                    // load the class
                        }
                        String className = je.getName().substring(0, je.getName().length() - 6).replace("/", ".");
                        Class c = ucl.loadClass(className);

                        printTracer("Found class: " + className);

                        // No modules for inner classes (but loaded into system class loader)
                        if (c.isAnonymousClass()) {
                            continue;
                        }

                        String name = "No name specified";
                        String description = "No description specified";

                        Annotation annotation = c.getAnnotation(TrancheModuleAnnotation.class);

                        if (annotation != null) {
                            TrancheModuleAnnotation a = (TrancheModuleAnnotation) annotation;
                            name = a.name();
                            description = a.description();
                        } // Not a module if no annotations (but loaded anyhow)
                        else {
//                        printTracer("Skipping class without an annotation...");
                            continue;
                        }

                        printTracer("Found module with name: " + name + "; description: " + description);

                        t1 = new TrancheModule(name, description, true, true, c);

                        // Get method annotations
                        for (Method m : c.getMethods()) {

                            TrancheMethodAnnotation methodAnnotation = m.getAnnotation(TrancheMethodAnnotation.class);
                            LeftMenuAnnotation leftMenuAnnotation = m.getAnnotation(LeftMenuAnnotation.class);
                            PopupMenuAnnotation popupMenuAnnotation = m.getAnnotation(PopupMenuAnnotation.class);
                            TabAnnotation tabAnnotation = m.getAnnotation(TabAnnotation.class);
                            AdvancedToolsAnnotation advAnnotation = m.getAnnotation(AdvancedToolsAnnotation.class);

                            // Will be added if found
                            SelectableModuleAction moduleAction = null;

                            if (methodAnnotation != null) {
                                moduleAction = new SelectableModuleAction(
                                        name,
                                        m,
                                        methodAnnotation.fileExtension(),
                                        methodAnnotation.mdAnnotation(),
                                        methodAnnotation.selectionMode(),
                                        methodAnnotation.label(),
                                        methodAnnotation.description());
                            } // Next, required
                            else {
                                continue;
                            }

                            // Get the gui policy. Really only matters for selectable actions in menus (i.e., not tabs)
                            GUIPolicy policy = GUIPolicy.extractPolicy(methodAnnotation, leftMenuAnnotation, popupMenuAnnotation);
                            moduleAction.setGUIPolicy(policy);

                            t1.addSelectableAction(moduleAction);

                            // If its a tab, set tab information
                            if (tabAnnotation != null) {
                                moduleAction.setIsTab(true);
                                moduleAction.setIsFrontTab(tabAnnotation.isPlacedInFront());
                            }

                            // If it is an advanced tool, set information
                            if (advAnnotation != null) {
                                moduleAction.setIsAdvancedTool(true);
                            }

                            // Install the action
                            GUIMediatorUtil.installAction(moduleAction);

                            // Map the module name to the absolute path of jar
                            modulesJARMap.put(t1.name, jar.getAbsolutePath());
                        }

                        // Will only add if not included already
                        addModule(t1);

                        // Check interfaces for hooks
                        if (isClassImplementInterface(t1.moduleClass, RunInstallable.class)) {
                            printTracer("Found instabllable (with installation hooks), queuing...");
                            installRequiredQueue.add(t1);
                        }
                    }
                } catch (Exception e) {
                    /* Print warning, but skip class */
                    System.err.println("Exception installing module " + jar.getAbsolutePath() + ": " + e.getMessage());
                    e.printStackTrace(System.err);
                } finally {
                    IOUtil.safeClose(fis);
                    IOUtil.safeClose(jis);

                    // Attempt to remove Jar from temp dir. Will fail on Windows due to file protection.
                    IOUtil.safeDelete(jarCopy);
                }
            } // Installing JARs

            // Update flags/status for modules
            updateModuleFlags();

            // Ask mediator to update GUI
            for (TrancheModule module : modules) {
                GUIMediatorUtil.negotiateModuleStatus(module);
            }

            // Run any hooks for modules

            // Run install hook for all RunInstallable
            Class mainClass;
            for (TrancheModule moduleWithInstallHook : installRequiredQueue) {
                mainClass = moduleWithInstallHook.moduleClass;

                boolean isInterface = isClassImplementInterface(mainClass, RunInstallable.class);
                if (isInterface) {
                    try {
                        RunInstallable installInterface = (RunInstallable) mainClass.newInstance();
                        printTracer("Installing module with installable interface: " + moduleWithInstallHook.name);
                        boolean keepModuleInstalled = installInterface.install();

                        if (!keepModuleInstalled) {

                            // Perchance there is some clean up work
                            installInterface.uninstall();

                            printTracer("  -> Running uninstall hook (flagged to uninstall) for " + moduleWithInstallHook.name);

                            // Remove the JAR
                            File JAR = TrancheModulesUtil.getJARContainingModule(moduleWithInstallHook.name);
                            IOUtil.safeDelete(JAR);

                            // Going to have to reload all modules when done
                            isReloadWhenFinished = true;

                            // TODO Add functionality to mediator util to remove actions?
                            // Simpler to delete JAR and reload modules...

                            // Remove from in-memory list
                            modules.remove(moduleWithInstallHook);
                            isMostRecentInstalled = false;
                        }
                    } catch (Exception ex) {
                        System.out.println("Exception installing module " + moduleWithInstallHook.name + ": " + ex.getMessage());
                        ex.printStackTrace();
                        isMostRecentInstalled = false;
                    }
                } // Defensive programming
                else {
                    throw new RuntimeException("Programming error. " + mainClass.toString() + " is not an instance of RunInstallable. Please report this.");
                }
            } // For each module with install hook, invoke install
            installRequiredQueue.clear();

            // If items were not installed, reload modules. The items not installed will not
            // be loaded since their JARs were deleted.
            if (isReloadWhenFinished) {
                TrancheModulesUtil.reloadModules();
            }
        } finally {
            loaded = true;
            wasLoadedOnce = true;
        }
    }

    /**
     * <p>Returns true only if there were no problems loading the most recently added module.</p>
     */
    public static boolean wasMostRecentModuleLoadedCorrectly() {

        boolean lastCheck = isMostRecentInstalled;

        // Assume installed correctly until there is a problem
        isMostRecentInstalled = true;

        return lastCheck;
    }

    /**
     * Force modules to load.
     */
    public static synchronized void loadModules() {
        lazyload();
    }

    /**
     * <p>Runs any uninstall hooks. Only call if really uninstalling module, not just shutting down or disabling.</p>
     * @param moduleName The actual module's name.
     */
    public static synchronized void runAnyUninstallHooks(String moduleName) throws Exception {
        TrancheModule m = TrancheModulesUtil.getModuleByName(moduleName);

        if (m != null && isClassImplementInterface(m.moduleClass, RunInstallable.class)) {
            RunInstallable ri = (RunInstallable) m.moduleClass.newInstance();
            ri.uninstall();
        }
    }

    /**
     * Useful for checking whether a class implements an interface using reflection.
     * @param c The class to check
     * @param i The interface. Must be an interface.
     * @return Returns true if a class implements a particular interface, else returns false
     */
    private static boolean isClassImplementInterface(Class c, Class i) {
        if (c != null) {
            Class[] interfaces = c.getInterfaces();
            for (Class _i : interfaces) {
                if (_i.equals(i)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Reload modules.
     */
    public static synchronized void reloadModules() {
        modules = new ArrayList();
        GUIMediatorUtil.uninstallActions();
        loaded = false;
        lazyload();
    }

    /**
     * Sets flags for modules so that may/may not be used based on preferences.
     */
    private static synchronized void updateModuleFlags() {
        boolean allModulesLoaded = true;
        // For each queued isEnabled value, apply
        for (String name : isEnabledFlagQueue.keySet()) {
            TrancheModule module = getModuleByName(name);

            if (module == null) {
                allModulesLoaded = false;
                continue;
            }

            module.isEnabled = isEnabledFlagQueue.get(name);
        }

        // Keep it simple. Only clear if all modules loaded.
        if (allModulesLoaded) {
            isEnabledFlagQueue.clear();
        }
    }

    /**
     * Sets the module list. All modules in util's memory are released. Also invokes negotiation of modules' action with GUI.
     */
    public synchronized static void setModules(List<TrancheModule> newModules) {
        modules = newModules;
        updateModuleFlags();

        // Update the GUI
        for (TrancheModule module : newModules) {
            GUIMediatorUtil.negotiateModuleStatus(module);
        }
    }

    /**
     * Adds a module if doesn't exist. Also checks whether there are any hooks that need executed.
     */
    public static synchronized boolean addModule(TrancheModule module) {
        if (!containsModule(module)) {
            modules.add(module);

            printTracer("Adding module " + module.name);
            return true;
        }

        printTracer("Already contains module " + module.name + ", nothing to do.");

        return false;
    }

    public static List<TrancheModule> getModules() {
        lazyload();
        return modules;
    }

    public static boolean containsModule(TrancheModule t1) {
        return getSameModule(t1) != null;
    }

    /**
     * Returns module from memory that matches a module. Returns null if not found.
     */
    public static TrancheModule getSameModule(TrancheModule t1) {
        for (TrancheModule module : modules) {
            if (module.sameModuleClass(t1)) {
                return module;
            }
        }
        return null;
    }

    /**
     * Return module by name. If not found, returns null.
     */
    public static TrancheModule getModuleByName(String name) {
        for (TrancheModule module : modules) {
            if (module.name.equals(name)) {
                return module;
            }
        }
        return null;
    }

    /**
     * Store isEnabled value from preferences if class not available yet. When available, will be applied.</p>
     */
    public static synchronized void queueIsEnabledValue(String name, boolean isEnabled) {
//        synchronized(isEnabledFlagQueue) {
        isEnabledFlagQueue.put(name, isEnabled);
//        }
    }

    /**
     * Returns the path to the JAR containing a certain module by name.
     * @param moduleName The exact name of the module
     * @return The jar path or null if not found
     */
    public static String getJARPathContainingModule(String moduleName) {
        return modulesJARMap.get(moduleName);
    }

    /**
     * Returns the path to the JAR containing a certain module by name.
     * @param moduleName The exact name of the module
     * @return The JAR file or null if not found
     */
    public static File getJARContainingModule(String moduleName) {
        String path = getJARPathContainingModule(moduleName);
        if (path == null) {
            return null;
        }
        return new File(path);
    }

    /**
     *
     */
    private static void printTracer(String msg) {
        if (isDebug) {
            System.out.println("TRANCHE_MODULES_UTIL> " + msg);
        }
    }
}