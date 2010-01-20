Since the module system may be confusing, this README may offer help.

----------------------------------------------------
THE PROCESS OF LOADING MODULES
----------------------------------------------------
Modules are loaded using a class loader.

When tranche GUI instance is started, at some point, org.tranche.modules.TrancheModulesUtil.lazyLoad() is called. This method finds JARs from a specific directory (look in the Util for exact location), looks for a class annotation called TrancheModule, and builds a module instance.

A module itself does nothing but specify the class; if a method is annotated by SelectableModuleAction, then the action can be loaded and installed into the gui.

Note that SelectableModuleAction itself will do nothing. Further annotation(s) are required:
* LeftMenuAnnotation if the action should appear in the left menu of projects/files panels
* PopupMenuAnnotation if the action should appear as popup (right- or control-clickable from table)

A very special case, TabAnnotation, marks an action as returning a JPanel, which is wrapped and installed as a tab.

In summary:
* A MODULE is just a class that contains actions. It really doesn't do anything; it can be installed, enabled or disabled
* AN ACTION is a properly annotated method, requiring a SelectableModuleAction along with other annotations to flag where the action should go in the GUI.

----------------------------------------------------
WHAT HAPPENS IN THE GUI
----------------------------------------------------

If an action is installed and the module is enabled, the actions will be visible in the left menu and available for the popup menu. (If module is installed but not enabled, actions are not visible.)

However, whether or not the action is available depends on the number of files selected and the names of files selected. (Just like the existing left menu items and popup menu items!) If you have worked in the projects and/or files panel, this is probably already familiar to you. Selectability depends on the file filter and the policy of the action.

----------------------------------------------------
FILE FILTER VS. POLICY
----------------------------------------------------

The FILE FILTER offers a way to enable/disable action for selected files. Some module actions should only apply to certain files. If I offer an action in a module that only targets SQT files (*.sqt), I'd set the file filter so only properly-name SQT files are selectable. (If other files are selected, the user won't be able to use the module action.)

The POLICY offers a way to enable/disable action for projects vs. files and for single versus multiple versus any number of files.

----------------------------------------------------
THE PLAYERS
----------------------------------------------------
Of course, this is subject to change.

The user can install/uninstall and enable/disable modules from the Module Manager, which is jsut the ModulePanel and GenericPopupFrame. These classes are located in the same package (org.tranche.gui) as the rest of the GUI classes.

Most of the relevant code is in the modules directory. 
* GUIMediatorUtil: basically arbitrates between TrancheModulesUtil and the GUI. If the module util thinks a module should be installed, calls the mediator. The mediator is there to bridge the two entities.
* GUIPolicy: low memory device to help mediator determine if an action works w/ single file, multiple files, files versus projects. This is built from the annotation, and is a rather simple class that hopefully makes the process of enabling/visibility for actions more clear.
* GenericTab: if an action is flagged as a tab (meaning it has the TabAnnotation sig), when it is "installed" in the GUI by the mediator, it is wrapped in this. This class is derived from the tab wrappers Augie developed when building the GUI.
* Installable: if a GUI class "installs" a GUI, it implements this. (Note that this is for installing actions that are not tabs.) A quick search of org.tranche.gui will show what implements this (now, files and projects panel, subject to change). The mediator keeps a references to all Installables so can install/uninstall actions at user's whim.
* LeftMenuAnnotation: annotates an action (method) that should appear in left menu for project and/or files. A quick look at class should explain everything.
* PopupMenuAnnotation: annotates an action (method) that should appear in a popup menu (right- or control-click in table) for project and/or files. A quick look at class should explain everything.
* SelectableModuleAction: stores information and references needed by an action, including the loaded method, back-reference to module, the file filter (if a filter is set, files can be filtered), the policy, etc. This would be an important class to understand.
* TabAnnotation: just a signature that says that the action (method) returns a JPanel that should be installed as a tab. As you probably guessed by now, the mediator installs the tab.
* TrancheMethodAnnotation: flags a method as an action and stores certain information, such as label and description (used for tooltips for clickable actions or text for tabs). Information regarding policy comes from other annotations (e.g., LeftMenuAnnoation, etc.)
* TrancheModule: represents an entire module. Used mostly for enabling/disabling a group or actions. These are shown by the module manager, not the actions. Most (if not all) modules will only contain one action, though they can have zero, one or more. (My prototype example had three.)
* TrancheModuleAnnotation: module and actions will never be loaded if class is not annotated with this. Keeps module information. May be more useful down the road if more tools develop, so choose info carefully.
* TrancheModulesUtil: does most of the work regarding finding, maintaining and removing module information from system. (The mediator action installs and uninstalls from the GUI; it is generally contacted by the TrancheModulesUtil to do its work.)

In summary: The TrancheModulesUtil loads the modules, and requests that the mediator installs them in the GUI. When files are selected, GUI (actually, any Selectable) contacts the mediator to see whether should be selectable, which consults the action's file filter and policy.

GUI (Selectables/other) <-> GUIMediatorUtil <-> TrancheModulesUtil

----------------------------------------------------
SPECIAL CASES: NATIVE BINARIES NEED INSTALLED
----------------------------------------------------

If a module requires installation of anything beyond what is contained in the module JAR (e.g., native binaries or other JARs), the main module class can implement RunInstallable.

Any module with a main class implementing RunInstallable will need to implement two methods: install and uninstall. "install" will be called every time module is loaded, offering the module an opportunity to install any needed items. "uninstall" will only be called when the module is uninstalled using the Tranche module.

There are three special notes of importance about "install":
(i) Make sure that the module only installs what is needed. Since this will be ran every single time it is loaded, your code should make sure it is not installing over and over again. 
(ii) If your installable on works on certain platforms (e.g., installing a native Win32 binary), please check for the target platform. If not, offer a friendly reminder.
(iii) Note that the install method returns a boolean. Return false only if you want the module to be discarded; true will keep the module.

One important special note about "uninstall":
(i) If uninstalling files, prompt the user first. Note that the user may have installed items themselves, so be smart about this!