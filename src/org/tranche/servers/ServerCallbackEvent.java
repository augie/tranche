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

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServerCallbackEvent extends ServerEvent {

    /**
     * <p>Code for a createion event.</p>
     */
    public static final int TYPE_CREATED = 0;
    /**
     * <p>Code for a completed event.</p>
     */
    public static final int TYPE_FULFILLED = 1;
    /**
     * <p>Code for a failure event.</p>
     */
    public static final int TYPE_FAILED = 2;
    private static final String STRING_BLANK = "";
    private static final String STRING_CREATED = "Created";
    private static final String STRING_FULFILLED = "Fulfilled";
    private static final String STRING_FAILED = "Failed";
    private static final String STRING_UNKNOWN = "Unknown";
    // for this instance
    private int type = -1;
    private long id = -1;
    private String name;
    private String description;

    /**
     * 
     * @param url
     * @param id
     * @param type
     */
    public ServerCallbackEvent(String url, long id, int type) {
        super(url, ServerEvent.CALLBACK);
        this.id = id;
        this.type = type;
    }

    /**
     * 
     * @param url
     * @param id
     * @param type
     * @param name
     * @param description
     */
    public ServerCallbackEvent(String url, long id, int type, String name, String description) {
        this(url, id, type);
        this.name = name;
        this.description = description;
    }

    /**
     * <p>Returns the ID for the callback this event represents.</p>
     * @return
     */
    public long getId() {
        return id;
    }

    /**
     * <p>Returns the type code for this event.</p>
     * @return
     */
    @Override
    public int getType() {
        return type;
    }

    /**
     * <p>Returns the name of the callback this represents.</p>
     * @return
     */
    public String getName() {
        if (name == null) {
            return STRING_BLANK;
        }
        return name;
    }

    /**
     * <p>Returns the description of the callback this represents.</p>
     * @return
     */
    public String getDescription() {
        if (description == null) {
            return STRING_BLANK;
        }
        return description;
    }

    /**
     * <p>Returns the human-readable type.</p>
     * @return
     */
    @Override
    public String getTypeString() {
        switch (type) {
            case TYPE_CREATED:
                return STRING_CREATED;
            case TYPE_FULFILLED:
                return STRING_FULFILLED;
            case TYPE_FAILED:
                return STRING_FAILED;
            default:
                return STRING_UNKNOWN;
        }
    }

    /**
     * <p>A human-readable description of this event.</p>
     * @return
     */
    @Override
    public String toString() {
        return "Callback #: " + id + ";  Date: " + TextUtil.getFormattedDate(getTimestamp()) + ";  Type: " + getTypeString() + ";  Name: " + getName() + ";";
    }
}
