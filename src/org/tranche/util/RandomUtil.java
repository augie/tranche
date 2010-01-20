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
package org.tranche.util;

import java.util.Random;

/**
 * <p>Allows for quick and easy access to random information.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class RandomUtil {

    private static final Random random = new Random();

    /**
     * 
     */
    private RandomUtil() {
    }
    
    /**
     * <p>From JavaDoc: Returns the next pseudorandom, uniformly distributed double value between 0.0 and 1.0 from this random number generator's sequence.</p>
     * @return
     */
    public static double getDouble() {
        return random.nextDouble();
    }

    /**
     * 
     * @return
     */
    public static final int getInt() {
        return random.nextInt();
    }

    /**
     *
     * @param max
     * @return
     */
    public static final int getInt(int max) {
        if (max == 0) {
            return 0;
        }
        return random.nextInt(max);
    }

    /**
     *
     * @return
     */
    public static final long getLong() {
        return random.nextLong();
    }

    /**
     *
     * @param bytes
     */
    public static final void getBytes(byte[] bytes) {
        random.nextBytes(bytes);
    }

    /**
     *
     * @return
     */
    public static final byte getByte() {
        return (byte) (random.nextInt(256) - 128);
    }

    /**
     *
     * @return
     */
    public static final boolean getBoolean() {
        return random.nextBoolean();
    }

    /**
     *
     * @param length
     * @return
     */
    public static final String getString(int length) {
        String str = "";
        for (int i = 0; i < length; i++) {
            str = str + getChar();
        }
        return str;
    }

    /**
     * 
     * @return
     */
    public static final char getChar() {
        return (char) (getInt((int) 'Z' - (int) 'A') + (int) 'A');
    }
}
