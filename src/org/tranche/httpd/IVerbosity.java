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
package org.tranche.httpd;

/**
 * Helper interface for mimicking compile directives. Implementing classes that would like to
 * implement different levels of verbocity to the stdout can surround the relavent code blocks
 * with an if statement e.g. if(DEBUG){ System.out.print("Socket timed out");}. 
 * Different levels of verbocity are possible by using or declaring different DEBUG flags.
 * @author TPapoulias
 */
interface IVerbosity {

    /**
     * Debug flag reserved for midium level of stdout vebrocity.
     */
    static final boolean DEBUG = true;
    /**
     * Debug flag reserver for a higher level of stdout vebrocity.
     */
    static final boolean DEBUG_VERBOSE = false;
    /**
     * Flag for debugging an input stream. Usually used to activate a block of code that
     * marks an input stream.
     */
    static final boolean ECHO = false;
    /**
     * Size to be used in marking an input stream.
     */
    static final int READ_AHEAD = (int) (1024 * 1E6);
}
