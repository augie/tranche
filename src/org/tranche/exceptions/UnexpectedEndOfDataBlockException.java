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
 * <p>Used if expecting more bytes in data block, but read end. Almost certainly means corrupted data block.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class UnexpectedEndOfDataBlockException extends Exception {

    /**
     * 
     * @param message
     */
    public UnexpectedEndOfDataBlockException(String message) {
        super(message);
    }

    /**
     * 
     */
    public UnexpectedEndOfDataBlockException() {
        super("End of data block read, expecting more bytes.");
    }
}
