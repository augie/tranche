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

import java.security.GeneralSecurityException;
import org.tranche.remote.Token;

/**
 * <p>Exception thrown only if trying to set a chunk that already exists and do not have sufficient privileges to delete.</p>
 * <p>Note that this should be a subclass of GeneralSecurityException, but because this is rethrown in RemoteUtil, needs to be either IOException or RuntimeException. Chose latter.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ChunkAlreadyExistsSecurityException extends GeneralSecurityException {

    public static final String MESSAGE = Token.SECURITY_ERROR + ": Chunk already exists and user cannot delete";

    /**
     * <p>Exception thrown only if trying to set a chunk that already exists and do not have sufficient privileges to delete.</p>
     * <p>Note that this should be a subclass of GeneralSecurityException, but because this is rethrown in RemoteUtil, needs to be either IOException or RuntimeException. Chose latter.</p>
     */
    public ChunkAlreadyExistsSecurityException() {
        super(MESSAGE);
    }
}
