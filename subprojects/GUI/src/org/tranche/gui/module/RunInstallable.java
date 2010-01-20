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
 * <p>Any modules implementing this will run their install method when the JAR is loaded.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public interface RunInstallable {

    /**
     * <p>Install will be called every time the module is loaded, not just the first time.</p>
     * <p>All modules are loaded at the beginning of every new instance of the GUI, so check before installing anything new.</p>
     * @return False if and only if the Tranche module should be discarded (not installed), true otherwise.
     */
    public boolean install() throws Exception;

    /**
     * <p>Called when the JAR is uninstalled using the module manager. If removing anything, be nice to the user and prompt!</p>
     */
    public void uninstall() throws Exception;
}
