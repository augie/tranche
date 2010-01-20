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
package org.tranche.remote;

import java.io.IOException;

/**
 * <p>A special IOException where the token received is unknown/unexpected.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class UnexpectedTokenException extends IOException {

    /**
     * @param   token       the token received 
     * @param   expected    the expected string used to form the message for the exception
     */
    public UnexpectedTokenException(String token, String expected) {
        // Yuck. Call to super(...) must be first, but need to intelligently create substring...
        super("Unexpected Token. Expected '" + expected + "' (with length of " + expected.length() + "). Received '" + (token.length() > 10 ? token.substring(0, 10) + "..." : token) + "' (with length of " + token.length() + ")");
    }
}
