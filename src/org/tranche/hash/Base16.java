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
package org.tranche.hash;

import java.io.StringWriter;

/**
 * 
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public final class Base16 {

    /**
     * Encode an array of bytes into its Base16 ASCII character string equivalent.
     * @param bytes
     * @return String encoded from the array of bytes
     */
    public static final String encode(byte[] bytes) {
        StringWriter name = new StringWriter();
        for (int i = 0; i < bytes.length; i++) {
            int b = 0xf & (bytes[i] >> 4);
            name.write(encodeByte((byte) b));
            int a = 0xf & bytes[i];
            name.write(encodeByte((byte) a));
        }
        return name.toString();
    }

    /**
     * Decode a string into its Base16 byte representation equivalent.
     * @param string
     * @return Array of bytes decoded from string
     */
    public static final byte[] decode(String string) {
        byte[] bytes = new byte[string.length() / 2];
        for (int i = 0; i < string.length() - 1; i += 2) {
            char a = string.charAt(i);
            char b = string.charAt(i + 1);
            // make a byte
            bytes[i / 2] = (byte) ((decodeCharacter(a) << 4) | decodeCharacter(b));
        }
        return bytes;
    }

    /**
     * Decode a single ASCII character encoded in Base16 to its byte represenation.
     * @param c
     * @return Byte representation of the ASCII character being decoded.
     */
    public static final byte decodeCharacter(char c) {
        switch (c) {
            case '0':
                return (byte) 0;
            case '1':
                return (byte) 1;
            case '2':
                return (byte) 2;
            case '3':
                return (byte) 3;
            case '4':
                return (byte) 4;
            case '5':
                return (byte) 5;
            case '6':
                return (byte) 6;
            case '7':
                return (byte) 7;
            case '8':
                return (byte) 8;
            case '9':
                return (byte) 9;
            case 'a':
                return (byte) 10;
            case 'b':
                return (byte) 11;
            case 'c':
                return (byte) 12;
            case 'd':
                return (byte) 13;
            case 'e':
                return (byte) 14;
            case 'f':
                return (byte) 15;
            default:
                throw new RuntimeException("Character must be 0-9 or a-f.");
        }
    }

    /**
     * Encode a single byte into its Base16 equivalent ASCII represenation.
     * @param b
     * @return ASCII character representation of the byte being encoded.
     */
    public static final char encodeByte(byte b) {
        switch (b) {
            case (byte) 0:
                return '0';
            case (byte) 1:
                return '1';
            case (byte) 2:
                return '2';
            case (byte) 3:
                return '3';
            case (byte) 4:
                return '4';
            case (byte) 5:
                return '5';
            case (byte) 6:
                return '6';
            case (byte) 7:
                return '7';
            case (byte) 8:
                return '8';
            case (byte) 9:
                return '9';
            case (byte) 10:
                return 'a';
            case (byte) 11:
                return 'b';
            case (byte) 12:
                return 'c';
            case (byte) 13:
                return 'd';
            case (byte) 14:
                return 'e';
            case (byte) 15:
                return 'f';
            default:
                throw new RuntimeException("Can't have a byte outside the range 0-15.");
        }
    }
}
