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

/**
 * <p>Representation of a message being sent to a server.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServerMessageUpEvent extends ServerMessageEvent {

    /**
     * <p>Represents a created upload message.</p>
     */
    public static final int TYPE_CREATED = 0;
    /**
     * <p>Represents a started upload message</p>
     */
    public static final int TYPE_STARTED = 1;
    /**
     * <p>Represents a completed upload message</p>
     */
    public static final int TYPE_COMPLETED = 2;
    /**
     * <p>Represents a failed upload message.</p>
     */
    public static final int TYPE_FAILED = 3;
    private static final String STRING_CREATED = "Created";
    private static final String STRING_STARTED = "Started";
    private static final String STRING_COMPLETED = "Completed";
    private static final String STRING_FAILED = "Failed";
    private static final String STRING_UNKNOWN = "Unknown";
    // for this instance
    private int type = -1;
    private long bytesToUpload = 0;
    private long bytesUploaded = 0;
    
    /**
     * 
     * @param url
     * @param callbackId
     * @param type
     */
    public ServerMessageUpEvent(String url, long callbackId, int type) {
        super(url, callbackId);
        this.type = type;
    }
    
    /**
     * 
     * @param url
     * @param callbackId
     * @param type
     * @param bytesToUpload
     */
    public ServerMessageUpEvent(String url, long callbackId, int type, long bytesToUpload) {
        this(url, callbackId, type);
        this.bytesToUpload = bytesToUpload;
    }
    
    /**
     * 
     * @param url
     * @param callbackId
     * @param type
     * @param bytesToUpload
     * @param bytesUploaded
     */
    public ServerMessageUpEvent(String url, long callbackId, int type, long bytesToUpload, long bytesUploaded) {
        this(url, callbackId, type, bytesToUpload);
        this.bytesUploaded = bytesUploaded;
    }
    
    /**
     * <p>Returns the type of action this event represents.</p>
     * @return
     */
    @Override()
    public int getType() {
        return type;
    }
    
    /**
     * <p>Returns the human-readable type of action this event represents.</p>
     * @return
     */
    @Override()
    public String getTypeString() {
        switch (type) {
            case TYPE_CREATED:
                return STRING_CREATED;
            case TYPE_STARTED:
                return STRING_STARTED;
            case TYPE_COMPLETED:
                return STRING_COMPLETED;
            case TYPE_FAILED:
                return STRING_FAILED;
            default:
                return STRING_UNKNOWN;
        }
    }
    
    /**
     * <p>Returns the number of bytes expected to be uploaded.</p>
     * @return
     */
    public long getBytesToUpload() {
        return bytesToUpload;
    }
    
    /**
     * <p>Returns the number of bytes that have been uploaded.</p>
     * @return
     */
    public long getBytesUploaded() {
        return bytesUploaded;
    }
    
    /**
     * <p>Gets a human-readable printout of what this event represents.</p>
     * @return
     */
    @Override()
    public String toString() {
        return super.toString() + "  Type: " + getTypeString() + ";  Bytes to Upload: " + getBytesToUpload() + ";  Bytes Uploaded: " + getBytesUploaded() + ";";
    }
    
}
