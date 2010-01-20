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
package org.tranche.configuration;

/**
 * <p>Simple byte flags for server mode (permissions such as read, write). Mode information applies to all clients, and must be enforced.</p>
 * <p>Note that this is a byte. Hence:</p>
 * <ul>
 *  <li>Eight bits for storing up to eight distinct flags. (Right now, only 'read' and 'write'.)</li>
 *  <li>Up to 256 ways to combine the 'elementary' distinct flags above into compound flags. Right now, there four: none, read-only, write-only, and read/write.</li>
 * </ul>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ServerModeFlag {
    
    /*---------------------------------------------------------------------------------------
     * DISTINCT FLAGS
     ---------------------------------------------------------------------------------------*/
    /**
     * <p>Flag: server data can be read by clients, but cannot write data to server.</p>
     */
    public static final byte CAN_READ = (1 << 0);
    /**
     * <p>Flag: clients can write data to server, but cannot read it.</p>
     */
    public static final byte CAN_WRITE = (1 << 1);
    
    /*---------------------------------------------------------------------------------------
     * COMPOSITE FLAGS (COMBINE ELEMENTARY FLAGS)
     ---------------------------------------------------------------------------------------*/
    /**
     * <p>Flag: clients cannot read nor write data from nor to server.</p>
     */
    public static final byte NONE = 0;
    /**
     * <p>Flag: clients can write and read data to and from server.</p>
     */
    public static final byte CAN_READ_WRITE = CAN_READ + CAN_WRITE;

    /**
     * <p>Convert server mode flag byte to String representation.</p>
     * @param flag
     * @return
     */
    public static final String toString(byte flag) {
        switch (flag) {
            case NONE:
                return "none";
            case CAN_READ:
                return "can read";
            case CAN_WRITE:
                return "can write";
            case CAN_READ_WRITE:
                return "can read and write";
            default:
                throw new RuntimeException("Unrecognized flag: " + flag);
        }
    }
    
    /**
     * <p>Based on flag byte, can client read data from server?</p>
     * @param flag
     * @return Ture if and only if read bit is set in flag
     */
    public static boolean canRead(byte flag) {
        return (flag & CAN_READ) == CAN_READ;
    }
    
    /**
     * <p>Based on flag byte, can client write data to server?</p>
     * @param flag
     * @return True if and only if write bit is set in flag
     */
    public static boolean canWrite(byte flag) {
        return (flag & CAN_WRITE) == CAN_WRITE;
    }
}
