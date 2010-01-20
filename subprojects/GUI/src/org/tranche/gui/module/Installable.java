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

/**
 * Any GUI element that works with the mediator needs to implement this interface.
 * @author Bryan E. Smith
 */
public interface Installable {
    
    /**
     * Install a particular action in the GUI.
     */
    public void installAction(SelectableModuleAction action);
    
    /**
     * Clear all installed actions.
     */
    public void clearActions();
    
    /**
     * Negotiate visibility/selectability of module.
     */
    public void negotateActionStatus(SelectableModuleAction action, boolean isEnabled);
}
