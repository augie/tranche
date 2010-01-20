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

import org.tranche.security.Signature;
import org.tranche.hash.BigHash;

/**
 * Encapsulates any log entry. Used to queue entries for write and for reading log.
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class LogEntryQueueItem {

    /**
     * Keys for the action being logged
     */
    public final static byte NONE = -1,  SET_DATA = 0,  SET_META_DATA = 1,  SET_CONFIG = 2,  GET_DATA = 3,  GET_META_DATA = 4,  GET_CONFIG = 5,  GET_NONCE = 6;
    
    /**
     * IP address
     */
    public String ip = null;
    
    /**
     * Log entry signature
     */
    public Signature sig = null;
    
    /**
     * Log entry signature in Base64
     */
    public String sigBase64 = null;
    
    /**
     * BigHash for log entry
     */
    public BigHash hash = null;
    
    /**
     * Log action
     */
    public byte action = NONE;
    
    /**
     * Timestamp of log entry
     */
    public long timestamp = -1;
    
    /**
     * Size of log entry
     */
    public long size = -1;
}
