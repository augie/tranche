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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single module in Tranche.
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class TrancheModule {

    public String name;
    public String description;
    public boolean isEnabled;
    public boolean isInstalled;
    public Class moduleClass;
    private List<SelectableModuleAction> selectableActions;
    public final static String FIELD_DELIMITER = "<MOD_PART>";

    public TrancheModule(String name, String description, boolean isEnabled, boolean isInstalled, Class moduleClass) {
        this.name = name;
        this.description = description;
        this.isEnabled = isEnabled;
        this.isInstalled = isInstalled;
        this.moduleClass = moduleClass;
        this.selectableActions = new ArrayList();
    }

    public String toString() {

        StringBuffer buffer = new StringBuffer();

        buffer.append(name + FIELD_DELIMITER + description + FIELD_DELIMITER + isEnabled + FIELD_DELIMITER + isInstalled + FIELD_DELIMITER + this.moduleClass.getName());

        for (SelectableModuleAction action : this.selectableActions) {
            buffer.append(this.FIELD_DELIMITER + action.toString());
        }

        return buffer.toString();
    }

    public static TrancheModule createFromString(String str) throws ClassNotFoundException {

        String[] fields = str.split(FIELD_DELIMITER);

        String name = fields[0].trim();
        String description = fields[1].trim();
        Boolean isEnabled = Boolean.valueOf(fields[2].trim());
        Boolean isInstalled = Boolean.valueOf(fields[3].trim());
        Class moduleClass = Class.forName(fields[4].trim());

        return new TrancheModule(name, description, isEnabled, isInstalled, moduleClass);
    }

    /**
     * Returns name from record created by toString
     */
    public static String extractNameFromString(String str) {
        String[] fields = str.split(FIELD_DELIMITER);
        return fields[0].trim();
    }

    /**
     * Returns isEnabled value from record created by toString
     */
    public static boolean extractIsEnabledFromString(String str) {
        String[] fields = str.split(FIELD_DELIMITER);
        return Boolean.valueOf(fields[2].trim());
    }

    /**
     * Returns true if module represents same module as this class.
     */
    public boolean sameModuleClass(TrancheModule module) {
        return module.name.equals(this.name) && module.moduleClass.equals(this.moduleClass);
    }

    /**
     * Returns a list of all selectable actions in the module.
     */
    public List<SelectableModuleAction> getSelectableActions() {
        return selectableActions;
    }

    /**
     * Add a selectable action (annotation method) to the list.
     */
    public boolean addSelectableAction(SelectableModuleAction action) {
        return this.selectableActions.add(action);
    }
}
