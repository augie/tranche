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

import java.io.IOException;

/**
 * <p>This exception represents corrupted data. Normally it means someone tried to manually change a setting in a file, or that a file conversion or update didn't complete.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class CorruptDataException extends IOException {

    /**
     * @param message   The message to be displayed when the exception is thrown.
     */
    public CorruptDataException(String message) {
        super(message);
    }
}
