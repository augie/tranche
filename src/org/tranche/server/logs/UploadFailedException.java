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
package org.tranche.server.logs;

import java.io.IOException;

/**
 * Signature exception to end an upload with certain status.
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class UploadFailedException extends IOException {

    /**
     * Create a standard UploadFailedException without a message.
     */
    public UploadFailedException() {
        super();
    }

    /**
     * Create a standard UploadFailedException with a message.
     * @param msg
     */
    public UploadFailedException(String msg) {
        super(msg);
    }
}
