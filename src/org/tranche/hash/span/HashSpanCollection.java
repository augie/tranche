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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.hash.BigHash;

/**
 * <p>Use to manage and make modifications to a group of HashSpans.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class HashSpanCollection extends Object {

    private Collection<HashSpan> hashSpans = Collections.synchronizedSet(new HashSet<HashSpan>());

    /**
     * <p>The default constructor.</p>
     */
    public HashSpanCollection() {
    }

    /**
     * <p>The constructor to use for starting with a group of hash spans.</p>
     * @param hashSpans The hash spans to start with.
     */
    public HashSpanCollection(Collection<HashSpan> hashSpans) {
        addAll(hashSpans);
    }

    /**
     * <p>The hash spans contained within the collection.</p>
     * @return A collection of hash spans.
     */
    public Collection<HashSpan> getHashSpans() {
        return Collections.unmodifiableCollection(hashSpans);
    }

    /**
     * <p>Adds all the hash spans in the given collection to this collection.</p>
     * @param hashSpans A collection of hash spans.
     */
    public void addAll(Collection<HashSpan> hashSpans) {
        this.hashSpans.addAll(hashSpans);
    }

    /**
     * <p>Removes all the hash spans in the given collection from this collection.</p>
     * @param hashSpans A collection of hash spans.
     */
    public void removeAll(Collection<HashSpan> hashSpans) {
        this.hashSpans.removeAll(hashSpans);
    }

    /**
     * <p>Returns a collection of hash spans that represent the ranges of hashes that are required to complete a full hash span.</p>
     * @return A collection of hash spans.
     */
    public Collection<HashSpan> getMissingHashSpans() {
        Set<HashSpan> missingHashSpans = new HashSet<HashSpan>();
        //
        if (!hashSpans.isEmpty()) {
            // keep track of merged hash spans
            List<HashSpan> workingHashSpans = new LinkedList<HashSpan>(hashSpans);
            merge(workingHashSpans);
            Collections.sort(workingHashSpans);
            // pick out the missing ranges
            {
                // first to first in merged
                if (HashSpan.FIRST.compareTo(workingHashSpans.get(0).getFirst()) < 0) {
                    missingHashSpans.add(new HashSpan(HashSpan.FIRST, workingHashSpans.get(0).getFirst().getPrevious()));
                }
                // all the middles -- already merged, so these are definitely not overlapping
                for (int i = 0; i < workingHashSpans.size() - 1; i++) {
                    missingHashSpans.add(new HashSpan(workingHashSpans.get(i).getLast().getNext(), workingHashSpans.get(i + 1).getFirst().getPrevious()));
                }
                // last in merged to last
                if (HashSpan.LAST.compareTo(workingHashSpans.get(workingHashSpans.size() - 1).getLast()) > 0) {
                    missingHashSpans.add(new HashSpan(workingHashSpans.get(workingHashSpans.size() - 1).getLast().getNext(), HashSpan.LAST));
                }
            }
        } // everything is missing
        else {
            missingHashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));
        }
        return Collections.unmodifiableCollection(missingHashSpans);
    }

    /**
     * <p>Whether the collection of hash spans together represent the full range of hashes.</p>
     * @return Whether the collection of hash spans together represent the full range of hashes.
     */
    public boolean isFullHashSpan() {
        return getMissingHashSpans().isEmpty();
    }

    /**
     * <p>Creates a clone of this hash span collection.</p>
     * @return A new HashSpanCollection object that is equivalent to this one.
     */
    @Override
    public HashSpanCollection clone() {
        return new HashSpanCollection(getHashSpans());
    }

    /**
     * <p>Returns whether the hash spans contained within this and the given are equivalent.</p>
     * @param o A hash span collection.
     * @return Whether the hash spans contained within this and the given are equivalent.
     */
    @Override
    public boolean equals(Object o) {
        HashSpanCollection c = (HashSpanCollection) o;
        return areEqual(getHashSpans(), c.getHashSpans());
    }

    /**
     * <p>Merge a collection of hash spans as much as possible</p>
     * <p>Merging consists of taking overlapping hash spans and combining them into a single hash span as much as is possible.</p>
     * @param hashSpans A list of hash spans.
     */
    public static void merge(List<HashSpan> hashSpans) {
        // quick size checks
        if (hashSpans == null || hashSpans.isEmpty() || hashSpans.size() == 1) {
            return;
        }
        // merge the list
        boolean performedMerge;
        do {
            performedMerge = false;
            // go through all of the spans
            for (int i = 0; i < hashSpans.size() - 1; i++) {
                if (hashSpans.get(i).overlaps(hashSpans.get(i + 1)) || hashSpans.get(i).isAdjecentTo(hashSpans.get(i + 1))) {
                    HashSpan hs1 = hashSpans.get(i);
                    HashSpan hs2 = hashSpans.get(i + 1);
                    hashSpans.add(merge(hs1, hs2));
                    hashSpans.remove(hs1);
                    hashSpans.remove(hs2);
                    performedMerge = true;
                    break;
                }
            }
            // all done
            if (hashSpans.size() == 1) {
                break;
            }
        } while (performedMerge);
    }

    /**
     * <p>Makes one hash span from two.</p>
     * <p>Given hash spans must overlap or be adjacent.</p>
     * @param hs1 A hash span.
     * @param hs2 A hash span.
     * @return Null if the hash spans do not overlap and are not adjacent.
     */
    public static HashSpan merge(HashSpan hs1, HashSpan hs2) {
        // verify overlap
        if (!(hs1.overlaps(hs2) || hs1.isAdjecentTo(hs2))) {
            return null;
        }
        // get ends
        BigHash first = hs1.getFirst(), last = hs1.getLast();
        if (hs2.getFirst().compareTo(first) < 0) {
            first = hs2.getFirst();
        }
        if (hs2.getLast().compareTo(last) > 0) {
            last = hs2.getLast();
        }
        return new HashSpan(first, last);
    }

    /**
     * <p>Determines the given collections of hash spans are equivalent.</p>
     * @param hashSpans1 A collection of hash spans.
     * @param hashSpans2 A collection of hash spans.
     * @return Whether the two given collections of hash spans are equivalent.
     */
    public static boolean areEqual(Collection<HashSpan> hashSpans1, Collection<HashSpan> hashSpans2) {
        // test null
        if (hashSpans1 == null && hashSpans2 == null) {
            return true;
        }
        if (hashSpans1 == null || hashSpans2 == null) {
            return false;
        }
        // merge them and test equivalence of the resulting hash spans
        List<HashSpan> hashSpansList1 = new ArrayList<HashSpan>(hashSpans1);
        merge(hashSpansList1);
        List<HashSpan> hashSpansList2 = new ArrayList<HashSpan>(hashSpans2);
        merge(hashSpansList2);
        // test size
        if (hashSpansList1.size() != hashSpansList2.size()) {
            return false;
        }
        // all hash spans should be equal
        for (int i = 0; i < hashSpansList1.size(); i++) {
            if (!hashSpansList1.get(i).equals(hashSpansList2.get(i))) {
                return false;
            }
        }
        return true;
    }
}