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
 * Annotation for tranche module method.
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TrancheMethodAnnotation {

    /**
     * <p>filter for acceptable file extensions for this module's action.</p>
     * <p>E.g., ".mgf|.pkl" will accept any file ending with ".mgf" or ".pkl"</p>
     * <p>If you don't want to restrict by file extension, just specify \"*\".</p>
     */
    String fileExtension();

    /**
     * <p>Acceptable meta data annotations.</p>
     * <p>E.g., "Tranche:Peaklist->*|Tranche:Sequest Output->*"</p>
     * <p>If you don't want to restrict by annotations, just specify \"*\".</p>
     */
    String mdAnnotation();

    /**
     * <p>One of the following options:</p>
     * <ul>
     *   <li>none: does not take any file or project arguments</li>
     *   <li>single: takes exactly 1 argument</li>
     *   <li>multiple: takes more than 1 argument</li>
     *   <li>any: takes 1 or more arguments</li>
     * </ul>
     * <p>Use case for <strong>none</strong>: You have a method that does not take any parameters. E.g., a method that launches a native application without passing it files.</p>
     * <p>Use case for <strong>any</strong>: You have a method that takes one or more hashes, files, etc. E.g., a method that converts one or more BioXML files to an HTML file explaining the experiments.</p>
     */
    String selectionMode();

    /**
     * <p>Label for interface. Should be short.</p>
     * <p>E.g., \"Peak List Viewer\"</p>
     */
    String label();

    /**
     * <p>Description for action, used as a tool tip.</p>
     * <p>E.g., \"View peak list using custom render tool.\"</p>
     */
    String description();
//    /**
//     * <p>Please one of the following paramater types:</p>
//     * <ul>
//     *   <li>List<BigHash>: List of selected big hashes.</li>
//     *   <li>Map<String,BigHash>: map between selected big hashes and file/project names.</li>
//     *   <li>None.</li>
//     * </ul>
//     */
//    String params();
}
