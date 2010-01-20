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
package org.tranche.hash.span;

import org.tranche.hash.BigHash;

/**
 * <p>A type of hash span where the universe of hashes is divided into large chunks.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class AbstractHashSpan extends HashSpan {

    public static final int ABSTRACTION_FIRST = 0,  ABSTRACTION_LAST = 65536;
    private static byte[] ABSTRACTION_DISCRETE_BYTES = new byte[BigHash.HASH_LENGTH];
    private static byte[] NEGATIVE_ONE_BYTES = new byte[BigHash.HASH_LENGTH];
    public static BigHash ABSTRACTION_DISCRETE_VALUE,  NEGATIVE_ONE;
    private int first,  last;


    static {
        // 608 bits per hash
        // 2^608 / 65536 = 2^608 / 2^16 = 2^(608-16) = 2^592
        for (int i = 0; i < ABSTRACTION_DISCRETE_BYTES.length; i++) {
            ABSTRACTION_DISCRETE_BYTES[i] = Byte.MIN_VALUE;
        }
        ABSTRACTION_DISCRETE_BYTES[1] = Byte.MIN_VALUE + 1;
        ABSTRACTION_DISCRETE_VALUE = BigHash.createFromBytes(ABSTRACTION_DISCRETE_BYTES);
        for (int i = 0; i < NEGATIVE_ONE_BYTES.length; i++) {
            NEGATIVE_ONE_BYTES[i] = (byte) (Byte.MIN_VALUE - 1);
        }
        NEGATIVE_ONE = BigHash.createFromBytes(NEGATIVE_ONE_BYTES);
    }

    /**
     * 
     * @param first
     * @param last
     */
    public AbstractHashSpan(int first, int last) {
        super(valueOf(first), valueOf(last));
        if (last < first) {
            this.first = last;
            this.last = first;
        } else {
            this.first = first;
            this.last = last;
        }
    }

    /**
     * 
     * @param hashSpan
     */
    public AbstractHashSpan(HashSpan hashSpan) {
        super(hashSpan.getFirst(), hashSpan.getLast());
        // binary search for the first
        int index = ABSTRACTION_LAST / 2;
        int lastMove = index;
        while (true) {
            int compare = hashSpan.getFirst().compareTo(valueOf(index));
            if (compare == 0) {
                break;
            }
            if (lastMove == 1) {
                // must err on the side of too small
                if (compare < 0) {
                    index--;
                }
                break;
            }
            lastMove = lastMove / 2;
            if (compare < 0) {
                index -= lastMove;
            } else {
                index += lastMove;
            }
        }
        first = index;

        // binary search for the last
        index = ABSTRACTION_LAST / 2;
        lastMove = index;
        while (true) {
            int compare = hashSpan.getLast().compareTo(valueOf(index));
            if (compare == 0) {
                break;
            }
            if (lastMove == 1) {
                // must err on the side of too big
                if (compare > 0) {
                    index++;
                }
                break;
            }
            lastMove = lastMove / 2;
            if (compare < 0) {
                index -= lastMove;
            } else {
                index += lastMove;
            }
        }
        last = index;
    }

    /**
     *
     * @param first
     */
    public void setFirst(int first) {
        super.setFirst(valueOf(first));
        this.first = first;
    }

    /**
     * 
     * @return
     */
    public int getAbstractionFirst() {
        return first;
    }

    /**
     * 
     * @param last
     */
    public void setLast(int last) {
        super.setLast(valueOf(last));
        this.last = last;
    }

    /**
     *
     * @return
     */
    public int getAbstractionLast() {
        return last;
    }

    /**
     *
     * @param value
     * @return
     */
    public static BigHash valueOf(int value) {
        BigHash hash = FIRST.clone();
        for (int i = 0; i < value; i++) {
            hash.add(ABSTRACTION_DISCRETE_VALUE);
        }
        if (value != 0) {
            // subtract 1
            hash.add(NEGATIVE_ONE);
        }
        return hash;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return first + " - " + last;
    }
}
