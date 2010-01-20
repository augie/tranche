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
package org.tranche.exceptions;

/**
 * <p>Used when functionality not implemented yet, but planned.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class TodoException extends UnsupportedOperationException {

    public final static String MESSAGE = "The operation you called has not been implemented yet.";

    /**
     * <p>Used when functionality not implemented yet, but planned.</p>
     */
    public TodoException() {
        super(MESSAGE);
    }

    /**
     * 
     * @param msg
     */
    public TodoException(String msg) {
        super(MESSAGE + " " + msg);
    }
}
