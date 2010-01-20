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

/**
 * <p>Encapsulation of an outgoing message.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class OutGoingBytes {

    /**
     * <p>The ID of the callback.</p>
     */
    public long id;
    /**
     * <p>The bytes to send.</p>
     */
    public byte[] bytes;
    public String key;
    public static String PING = "[ping]";
    public static String GET_DATA = "[get data]";
    public static String GET_META_DATA = "[get meta]";
    public static String SET_DATA = "[set data]";
    public static String SET_META = "[set meta]";
    public static String HAS_DATA = "[has data]";
    public static String HAS_META_DATA = "[has meta]";
    public static String DELETE_META = "[delete meta]";
    public static String DELETE_DATA = "[delete data]";
    public static String NONCE = "[nonce]";
    public static String GET_DATA_HASHES = "[get data hashes]";
    public static String GET_META_HASHES = "[get meta hashes]";
    public static String GET_PROJECT_HASHES = "[get project hashes]";
    public static String REGISTER_SERVER = "[register server]";
    public static String GET_CONFIGURATION = "[get config]";
    public static String SET_CONFIGURATION = "[set config]";
    public static String GET_NETWORK_STATUS = "[get network status]";
    public static String GET_ACTIVITY_LOG_ENTRIES = "[get activity log entries]";
    public static String GET_ACTIVITY_LOG_ENTRIES_COUNT = "[get activity log entries count]";
    public static String CLOSE = "[CLOSE]";
    public static String SHUTDOWN = "[shutdown]";
    
    /**
     * 
     * @param id
     * @param bytes
     * @param key
     */
    public OutGoingBytes(long id, byte[] bytes, String key) {
        this.id = id;
        this.bytes = bytes;
        this.key = key;
    }
}
