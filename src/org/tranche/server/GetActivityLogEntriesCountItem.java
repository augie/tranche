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
package org.tranche.server;

import java.io.InputStream;
import java.io.OutputStream;
import org.tranche.exceptions.TrancheProtocolException;
import org.tranche.remote.IntegerCallback;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;

/**
 * <p>Server item: get activity log entries count within a time frame.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetActivityLogEntriesCountItem extends ServerItem {

    /**
     * 
     * @param server
     */
    public GetActivityLogEntriesCountItem(Server server) {
        super(Token.GET_ACTIVITY_LOG_ENTRIES_COUNT_STRING, server);
    }

    /**
     * @param in The input stream
     * @param out The output stream
     * @param clientIP So the transaction can be logged with client IP address.
     * @throws Exception
     */
    public final void doAction(InputStream in, OutputStream out, String clientIP) throws Exception {
        long startingTimestamp, endingTimestamp;
        byte mask;
        try {
            startingTimestamp = RemoteUtil.readLong(in);
            endingTimestamp = RemoteUtil.readLong(in);
            mask = RemoteUtil.readByte(in);
        } catch (Exception e) {
            throw new TrancheProtocolException();
        }
        // execute
        IntegerCallback.writeResponse(server.getTrancheServer().getActivityLogEntriesCount(startingTimestamp, endingTimestamp, mask), out);
    }

    /**
     * 
     * @param writeHeader
     * @param startTimestamp
     * @param endTimestamp
     * @param mask
     * @param out
     * @throws java.lang.Exception
     */
    public static final void writeRequest(boolean writeHeader, long startTimestamp, long endTimestamp, byte mask, OutputStream out) throws Exception {
        if (writeHeader) {
            RemoteUtil.writeBytes(Token.GET_ACTIVITY_LOG_ENTRIES_COUNT, out);
        }
        RemoteUtil.writeLong(startTimestamp, out);
        RemoteUtil.writeLong(endTimestamp, out);
        RemoteUtil.writeByte(mask, out);
    }
}
