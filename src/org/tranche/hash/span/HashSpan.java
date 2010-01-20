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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.tranche.hash.BigHash;

/**
 * <p>Represents a range of hashes.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class HashSpan implements Comparable<HashSpan> {

    /**
     * <p>Constant of the first and last possible BigHash possible.</p>
     */
    public static final BigHash FIRST,  LAST;
    private static byte[] LAST_BYTES = new byte[BigHash.HASH_LENGTH],  FIRST_BYTES = new byte[BigHash.HASH_LENGTH];


    static {
        for (int i = 0; i < FIRST_BYTES.length; i++) {
            FIRST_BYTES[i] = Byte.MIN_VALUE;
        }
        FIRST = BigHash.createFromBytes(FIRST_BYTES);
        for (int i = 0; i < LAST_BYTES.length; i++) {
            LAST_BYTES[i] = Byte.MAX_VALUE;
        }
        LAST = BigHash.createFromBytes(LAST_BYTES);
    }
    /**
     * <p>Full hash span</p>
     */
    public static final HashSpan FULL = new HashSpan(FIRST, LAST);
    private static final Set<HashSpan> fullSetInternal = new HashSet();


    static {
        fullSetInternal.add(FULL);
    }
    public static final Set<HashSpan> FULL_SET = Collections.unmodifiableSet(fullSetInternal);
    private BigHash first,  last;

    /**
     * 
     * @param first
     * @param last
     */
    public HashSpan(BigHash first, BigHash last) {
        if (first.compareTo(last) > 0) {
            setFirst(last);
            setLast(first);
        } else {
            setFirst(first);
            setLast(last);
        }
    }

    /**
     * Retrieve first BigHash portion of HashSpan.
     * @return
     */
    public BigHash getFirst() {
        return first;
    }

    /**
     * Set first BigHash portion of the HashSpan.
     * @param first
     */
    public void setFirst(BigHash first) {
        this.first = first;
    }

    /**
     * Retrieve last BigHash portion of HashSpan.
     * @return
     */
    public BigHash getLast() {
        return last;
    }

    /**
     * Set last BigHash portion of the HashSpan.
     * @param last
     */
    public void setLast(BigHash last) {
        this.last = last;
    }

    /**
     * Compare the equality (first hash) of another HashSpan.  Returns 0 if equal; anything
     * else otherwise.
     * @param hs
     * @return
     */
    public int compareTo(HashSpan hs) {
        return getFirst().compareTo(hs.getFirst());
    }

    /**
     * Returns true if has is between the first and last hashes in hash span 
     * range.
     * @param hash BigHash wish to check if in this hash span
     * @return boolean true if in range, false if not
     */
    public boolean contains(BigHash hash) {
        return getFirst().compareTo(hash) <= 0 && getLast().compareTo(hash) >= 0;
    }

    /**
     * <p>Whether any BigHash in the given HashSpan is also in this HashSpan.</p>
     * @param hashSpan
     * @return
     */
    public boolean overlaps(HashSpan hashSpan) {
        return contains(hashSpan.getFirst()) || contains(hashSpan.getLast()) || hashSpan.contains(getFirst()) || hashSpan.contains(getLast());
    }

    /**
     * <p>Whether the given hash span has an end BigHash that comes immediately before or after this HashSpan.</p>
     * @param hashSpan
     * @return
     */
    public boolean isAdjecentTo(HashSpan hashSpan) {
        return (!getLast().equals(LAST) && getLast().getNext().equals(hashSpan.getFirst())) || (!getFirst().equals(FIRST) && getFirst().getPrevious().equals(hashSpan.getLast()));
    }

    /**
     * Return the hash code of the HashSpan.
     * @return
     */
    @Override()
    public int hashCode() {
        return new String(getFirst().toString() + ":" + getLast().toString()).hashCode();
    }

    /**
     * Returns true if two hash spans are the same; false otherwise.
     * @param o hash span
     * @return boolean true if same, false otherwise
     */
    @Override()
    public boolean equals(Object o) {
        if (o instanceof HashSpan) {
            HashSpan span = (HashSpan) o;
            return getFirst().compareTo(span.getFirst()) == 0 && getLast().compareTo(span.getLast()) == 0;
        }
        return o.equals(this);
    }

    /**
     * Return a cloned copy of this HashSpan object.
     * @return
     */
    @Override
    public HashSpan clone() {
        return new HashSpan(getFirst(), getLast());
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return getFirst().toString() + ", " + getLast().toString();
    }
}
