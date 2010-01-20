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
import org.tranche.time.TimeUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.Text;

/**
 * <p>Improved messages for purged callbacks instead of sporadic "null" messages.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class PurgedCallbackException extends IOException {

    /**
     *
     * @param c
     */
    public PurgedCallbackException(RemoteCallback c) {
        super(c.getClass().getSimpleName() + " (id = " + c.getID() + ") purged from " + IOUtil.createURL(c.getRemoteTrancheServer()) + " at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " with the message: " + c.getPurgeMsg() + ".");
    }
}
