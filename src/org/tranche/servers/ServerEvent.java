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
package org.tranche.servers;

import org.tranche.commons.TextUtil;
import org.tranche.time.TimeUtil;

/**
 * <p>Represents a general server event.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServerEvent {

    /**
     * <p>Code for a connection type.</p>
     */
    public static final int CONNECT = 0;
    /**
     * <p>Code for a computer banning type.</p>
     */
    public static final int AUTO_BAN = 1;
    /**
     * <p>Code for a user banning type.</p>
     */
    public static final int USER_BAN = 2;
    /**
     * <p>Code for a computer unbanning type.</p>
     */
    public static final int AUTO_UNBAN = 3;
    /**
     * <p>Code for a user unbanning type.</p>
     */
    public static final int USER_UNBAN = 4;
    /**
     * <p>Code for a message type.</p>
     */
    public static final int MESSAGE = 5;
    /**
     * <p>Code for a callback type.</p>
     */
    public static final int CALLBACK = 6;
    private static final String STRING_CONNECT = "Connect";
    private static final String STRING_AUTO_BAN = "Auto Ban";
    private static final String STRING_USER_BAN = "User Ban";
    private static final String STRING_AUTO_UNBAN = "Auto Unban";
    private static final String STRING_USER_UNBAN = "User Unban";
    private static final String STRING_MESSAGE = "Message";
    private static final String STRING_CALLBACK = "Callback";
    private static final String STRING_UNKNOWN = "Unknown";
    // info about this particular event
    private String url = null;
    private int type = -1;
    private long timestamp = TimeUtil.getTrancheTimestamp();

    /**
     * 
     * @param url
     * @param type
     */
    public ServerEvent(String url, int type) {
        this.url = url;
        this.type = type;
    }

    /**
     * <p>Returns the UNIX timestamp of occurrence for this event.</p>
     * @return
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * <p>Returns the URL for the server being referenced by this event.</p>
     * @return
     */
    public String getUrl() {
        return url;
    }

    /**
     * <p>Returns the type code for this event.</p>
     * @return
     */
    public int getType() {
        return type;
    }

    /**
     * <p>Returns the type in a human-readable format.</p>
     * @return
     */
    public String getTypeString() {
        switch (type) {
            case CONNECT:
                return STRING_CONNECT;
            case AUTO_BAN:
                return STRING_AUTO_BAN;
            case USER_BAN:
                return STRING_USER_BAN;
            case AUTO_UNBAN:
                return STRING_AUTO_UNBAN;
            case USER_UNBAN:
                return STRING_USER_UNBAN;
            case MESSAGE:
                return STRING_MESSAGE;
            case CALLBACK:
                return STRING_CALLBACK;
            default:
                return STRING_UNKNOWN;
        }
    }

    /**
     * <p>Returns a human-readable description of this event.</p>
     * @return
     */
    @Override()
    public String toString() {
        return "Type: " + getTypeString() + ";  Date: " + TextUtil.getFormattedDate(timestamp);
    }
}
