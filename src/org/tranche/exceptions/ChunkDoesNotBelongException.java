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
 * <p>If a chunk does not belong to Tranche server (data or routing server if none of data servers match), this is thrown.</p>
 * <p>Note that RemoteUtil will serialize/deserialize stable table entry for server if this is thrown, so client will get updated information. Hence, this should be used consistently!</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ChunkDoesNotBelongException extends RuntimeException {

    public static final String MESSAGE = "The chunk does not belong to the server to which you tried to set it.";

    /**
     * <p>If a chunk does not belong to Tranche server (data or routing server if none of data servers match), this is thrown.</p>
     * <p>Note that RemoteUtil will serialize/deserialize stable table entry for server if this is thrown, so client will get updated information. Hence, this should be used consistently!</p>
     */
    public ChunkDoesNotBelongException() {
        super(MESSAGE);
    }
}
