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
package org.tranche.get;

import org.tranche.hash.BigHash;
import org.tranche.time.TimeUtil;
import org.tranche.util.Text;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetFileToolEvent {

    /**
     * <p>Code for a meta data chunk object.</p>
     */
    public static final int TYPE_METADATA = 0;
    /**
     * <p>Code for a data chunk object.</p>
     */
    public static final int TYPE_DATA = 1;
    /**
     * <p>Code for a file object.</p>
     */
    public static final int TYPE_FILE = 2;
    /**
     * <p>Code for a directory object.</p>
     */
    public static final int TYPE_DIRECTORY = 3;
    private static final String STRING_METADATA = "Meta Data";
    private static final String STRING_DATA = "Data";
    private static final String STRING_FILE = "File";
    private static final String STRING_DIRECTORY = "Directory";
    private static final String STRING_UNKNOWN = "Unknown";
    /**
     * <p>Code for a starting action.</p>
     */
    public static final short ACTION_STARTING = 0;
    /**
     * <p>Code for a started action.</p>
     */
    public static final short ACTION_STARTED = 1;
    /**
     * <p>Code for a trying action.</p>
     */
    public static final short ACTION_TRYING = 2;
    /**
     * <p>Code for a skipping action.</p>
     */
    public static final short ACTION_SKIPPED = 3;
    /**
     * <p>Code for a finished action.</p>
     */
    public static final short ACTION_FINISHED = 4;
    /**
     * <p>Code for a failure action.</p>
     */
    public static final short ACTION_FAILED = 5;
    private static final String STRING_STARTING = "Starting";
    private static final String STRING_STARTED = "Started";
    private static final String STRING_TRYING = "Trying";
    private static final String STRING_SKIPPED = "Skipped";
    private static final String STRING_FINISHED = "Finished";
    private static final String STRING_FAILED = "Failed";
    private BigHash fileHash,  chunkHash;
    private String serverHostName,  fileName;
    private final int type;
    private final short action;
    private long timestamp = TimeUtil.getTrancheTimestamp();

    /**
     *
     * @param action
     * @param type
     */
    public GetFileToolEvent(short action, int type) {
        this.action = action;
        this.type = type;
    }

    /**
     *
     * @param action
     * @param type
     * @param fileHash
     */
    public GetFileToolEvent(short action, int type, BigHash fileHash) {
        this(action, type);
        this.fileHash = fileHash;
    }

    /**
     *
     * @param action
     * @param type
     * @param fileName
     * @param fileHash
     */
    public GetFileToolEvent(short action, int type, String fileName, BigHash fileHash) {
        this(action, type, fileHash);
        this.fileName = fileName;
    }

    /**
     * 
     * @param action
     * @param type
     * @param fileHash
     * @param serverHostName
     */
    public GetFileToolEvent(short action, int type, BigHash fileHash, String serverHostName) {
        this(action, type, fileHash);
        this.serverHostName = serverHostName;
    }

    /**
     *
     * @param action
     * @param type
     * @param fileHash
     * @param chunkHash
     */
    public GetFileToolEvent(short action, int type, BigHash fileHash, BigHash chunkHash) {
        this(action, type, fileHash);
        this.chunkHash = chunkHash;
    }

    /**
     * 
     * @param action
     * @param type
     * @param fileHash
     * @param serverHostName
     * @param chunkHash
     */
    public GetFileToolEvent(short action, int type, BigHash fileHash, String serverHostName, BigHash chunkHash) {
        this(action, type, fileHash, serverHostName);
        this.chunkHash = chunkHash;
    }

    /**
     * <p>If this event references a file, this returns the hash of the file. Otherwise, null.</p>
     * @return
     */
    public BigHash getFileHash() {
        return fileHash;
    }

    /**
     * <p>If this event references a file, this returns the name of that file. Otherwise, null.</p>
     * @return
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * <p>If this event references a data chunk, this returns that value. Otherwise, null.</p>
     * @return
     */
    public BigHash getChunkHash() {
        return chunkHash;
    }

    /**
     *
     * @return
     */
    public String getServer() {
        return serverHostName;
    }

    /**
     * <p>The type of object that this event represents.</p>
     * @return
     */
    public int getType() {
        return type;
    }

    /**
     * <p>The human-readable version of the type of object that this event represents.</p>
     * @return
     */
    public String getTypeString() {
        return staticGetTypeString(type);
    }

    /**
     * <p>The human-readable version of the type of object the given type value represents.</p>
     * @param type
     * @return
     */
    public static String staticGetTypeString(int type) {
        switch (type) {
            case TYPE_METADATA:
                return STRING_METADATA;
            case TYPE_DATA:
                return STRING_DATA;
            case TYPE_FILE:
                return STRING_FILE;
            case TYPE_DIRECTORY:
                return STRING_DIRECTORY;
            default:
                return STRING_UNKNOWN;
        }
    }

    /**
     * <p>The action that this event is reporting.</p>
     * @return
     */
    public short getAction() {
        return action;
    }

    /**
     * <p>The human-readable version of the action that this event is reporting.</p>
     * @return
     */
    public String getActionString() {
        return staticGetActionString(action);
    }

    /**
     * <p>The human-readable version of the given action value..</p>
     * @param action
     * @return
     */
    public static String staticGetActionString(short action) {
        switch (action) {
            case ACTION_STARTING:
                return STRING_STARTING;
            case ACTION_STARTED:
                return STRING_STARTED;
            case ACTION_TRYING:
                return STRING_TRYING;
            case ACTION_SKIPPED:
                return STRING_SKIPPED;
            case ACTION_FINISHED:
                return STRING_FINISHED;
            case ACTION_FAILED:
                return STRING_FAILED;
            default:
                return STRING_UNKNOWN;
        }
    }

    /**
     * <p>The UNIX timestamp of when this object was created.</p>
     * @return
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * <p>Printout of this object's information.</p>
     * @return
     */
    @Override()
    public String toString() {
        String returnStr = "Type: " + getTypeString() + ";  Action: " + getActionString() + ";  ";
        if (fileHash != null) {
            returnStr = returnStr + "File: " + fileHash.toString() + ";  ";
        }
        if (chunkHash != null) {
            returnStr = returnStr + "Chunk: " + chunkHash.toString() + ";  ";
        }
        if (serverHostName != null) {
            returnStr = returnStr + "Server: " + serverHostName + ";  ";
        }
        returnStr = returnStr + "Date: " + Text.getFormattedDate(timestamp) + ";  ";
        return returnStr;
    }

}
