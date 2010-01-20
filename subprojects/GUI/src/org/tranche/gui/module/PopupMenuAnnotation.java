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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method annotation that indicates that the action should be in popup menu.
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PopupMenuAnnotation {

    /**
     * <p>Indicates which popup menus the option should appear in. The following options are acceptable:</p>
     * <ul>
     *   <li>Projects</li>
     *   <li>Files</li>
     * </ul>
     *
     * <p>You can combine options. For example, if you want the module to appear in both projects and files, use "Projects|Files".</p>
     */
    String scope();
}
