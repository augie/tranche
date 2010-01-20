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

/**
 * Class definine the components of an action byte.
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class ActionByte {
    
    /**
     * IPv values
     */
    public static final byte 
            IPv4 = Byte.MIN_VALUE,
            IPv6 = 0;
    
    /**
     * Identifiers for IPv4 byte actions.
     */
    public static final byte
            IPv4_SetData = IPv4+1,
            IPv4_SetMeta = IPv4+2,
            IPv4_SetConfig = IPv4+3,
            IPv4_GetData = IPv4+4,
            IPv4_GetMeta = IPv4+5,
            IPv4_GetConfig = IPv4+6,
            IPv4_GetNonce = IPv4+7;
    
    /**
     * Identifiers for IPv6 byte actions.
     */
    public static final byte
            IPv6_SetData = IPv6+1,
            IPv6_SetMeta = IPv6+2,
            IPv6_SetConfig = IPv6+3,
            IPv6_GetData = IPv6+4,
            IPv6_GetMeta = IPv6+5,
            IPv6_GetConfig = IPv6+6,
            IPv6_GetNonce = IPv6+7;
}
