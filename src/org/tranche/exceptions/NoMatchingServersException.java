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
 * <p>Used when cannot find a server to handle the request. The only use case at the moment is when a RoutingTrancheServer receives a get or set request for a chunk that does not fall into any of its data servers' hash spans.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class NoMatchingServersException extends IOException {
    public static final String MESSAGE = "Could not find a matching server for your request.";
    public NoMatchingServersException() {
        super(MESSAGE);
    }
}
