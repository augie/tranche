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
 * <p>Representation of a message being received from a server.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServerMessageDownEvent extends ServerMessageEvent {

    /**
     * <p>Code for a started download message.</p>
     */
    public static final int TYPE_STARTED = 1;
    /**
     * <p>Code for progress on a download message.</p>
     */
    public static final int TYPE_PROGRESS = 2;
    /**
     * <p>Code for a completed download message.</p>
     */
    public static final int TYPE_COMPLETED = 3;
    /**
     * <p>Code for a failed download message.</p>
     */
    public static final int TYPE_FAILED = 4;
    private static final String STRING_STARTED = "Started";
    private static final String STRING_PROGRESS = "Progress";
    private static final String STRING_COMPLETED = "Completed";
    private static final String STRING_FAILED = "Failed";
    private static final String STRING_UNKNOWN = "Unknown";
    // for this instance
    private int type = -1;
    private long bytesToDownload = 0;
    private long bytesDownloaded = 0;

    /**
     * 
     * @param url
     * @param callbackId
     * @param type
     */
    public ServerMessageDownEvent(String url, long callbackId, int type) {
        super(url, callbackId);
        this.type = type;
    }

    /**
     * 
     * @param url
     * @param callbackId
     * @param type
     * @param bytesToDownload
     */
    public ServerMessageDownEvent(String url, long callbackId, int type, long bytesToDownload) {
        this(url, callbackId, type);
        this.bytesToDownload = bytesToDownload;
    }
    
    /**
     * 
     * @param url
     * @param callbackId
     * @param type
     * @param bytesToDownload
     * @param bytesDownloaded
     */
    public ServerMessageDownEvent(String url, long callbackId, int type, long bytesToDownload, long bytesDownloaded) {
        this(url, callbackId, type, bytesToDownload);
        this.bytesDownloaded = bytesDownloaded;
    }
    
    /**
     * <p>Returns the type code.</p>
     * @return
     */
    @Override()
    public int getType() {
        return type;
    }
    
    /**
     * <p>Returns the type in a human-readable format.</p>
     * @return
     */
    @Override()
    public String getTypeString() {
        switch (type) {
            case TYPE_STARTED:
                return STRING_STARTED;
            case TYPE_PROGRESS:
                return STRING_PROGRESS;
            case TYPE_COMPLETED:
                return STRING_COMPLETED;
            case TYPE_FAILED:
                return STRING_FAILED;
            default:
                return STRING_UNKNOWN;
        }
    }
    
    /**
     * <p>Gets the number of bytes that are to be downloaded.</p>
     * @return
     */
    public long getBytesToDownload() {
        return bytesToDownload;
    }
    
    /**
     * <p>Gets the number of bytes that have been downloaded.</p>
     * @return
     */
    public long getBytesDownloaded() {
        return bytesDownloaded;
    }
    
    /**
     * <p>Gets a human-readable description of this event.</p>
     * @return
     */
    @Override()
    public String toString() {
        return super.toString() + "  Type: " + getTypeString() + ";  Bytes to Download: " + getBytesToDownload() + ";  Bytes Downloaded: " + getBytesDownloaded() + ";";
    }
}
