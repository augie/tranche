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
import org.tranche.hash.BigHash;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ChunkDoesNotMatchHashException extends IOException {

    public static final String MESSAGE = "The chunk does not match the associated hash. Was the chunk corrupted in memory or during transmission?";

    /**
     * <p>Create exception based on the expected hash and the found (calculated) hash.</p>
     * @param expectedHash The hash associated with the chunk.
     * @param foundHash The hash calculated based on chunks contents.
     */
    public ChunkDoesNotMatchHashException(BigHash expectedHash, BigHash foundHash) {
        super(MESSAGE + "\n   Expected: " + expectedHash + "\n   Found: " + foundHash);
    }

    /**
     * <p>Only use this constructor when re-creating a deserialized exception (i.e., creating exception from message sent from server to client).</p>
     * @param serializedMessage
     */
    public ChunkDoesNotMatchHashException(String serializedMessage) {
        super(serializedMessage);
    }
}
