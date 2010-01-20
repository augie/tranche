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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.tranche.hash.BigHash;
import org.tranche.util.DevUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class HashSpanCollectionTest extends TrancheTestCase {

    public void testIsFullHashSpan() throws Exception {
        // positive
        {
            // single full
            {
                // spans
                List<HashSpan> hashSpans = new LinkedList<HashSpan>();
                hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));

                // collection
                HashSpanCollection hsc = new HashSpanCollection(hashSpans);

                // verify
                assertEquals(true, hsc.isFullHashSpan());
            }
            // multiple full
            {
                // spans
                List<HashSpan> hashSpans = new LinkedList<HashSpan>();
                hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST.getPrevious().getPrevious()));
                hashSpans.add(new HashSpan(HashSpan.LAST.getPrevious(), HashSpan.LAST));

                // collection
                HashSpanCollection hsc = new HashSpanCollection(hashSpans);

                // verify
                assertEquals(true, hsc.isFullHashSpan());
            }
        }
        // negative
        {
            // none
            {
                // spans
                List<HashSpan> hashSpans = new LinkedList<HashSpan>();

                // collection
                HashSpanCollection hsc = new HashSpanCollection(hashSpans);

                // verify
                assertEquals(false, hsc.isFullHashSpan());
            }
            // hole at start
            {
                // spans
                List<HashSpan> hashSpans = new LinkedList<HashSpan>();
                hashSpans.add(new HashSpan(HashSpan.FIRST.getNext(), HashSpan.LAST));

                // collection
                HashSpanCollection hsc = new HashSpanCollection(hashSpans);

                // verify
                assertEquals(false, hsc.isFullHashSpan());
            }
            // hole at end
            {
                // spans
                List<HashSpan> hashSpans = new LinkedList<HashSpan>();
                hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST.getPrevious()));

                // collection
                HashSpanCollection hsc = new HashSpanCollection(hashSpans);

                // verify
                assertEquals(false, hsc.isFullHashSpan());
            }
            // hole in the middle
            {
                // spans
                List<HashSpan> hashSpans = new LinkedList<HashSpan>();
                hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST.getPrevious().getPrevious().getPrevious()));
                hashSpans.add(new HashSpan(HashSpan.LAST.getPrevious(), HashSpan.LAST));

                // collection
                HashSpanCollection hsc = new HashSpanCollection(hashSpans);

                // verify
                assertEquals(false, hsc.isFullHashSpan());
            }
        }
    }

    public void testGetMissingHashSpans() throws Exception {
        // positive
        {
            // single full
            {
                // spans
                List<HashSpan> hashSpans = new LinkedList<HashSpan>();
                hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));

                // collection
                HashSpanCollection hsc = new HashSpanCollection(hashSpans);

                // verify
                assertEquals(true, hsc.getMissingHashSpans().isEmpty());
            }
            // multiple full
            {
                // spans
                List<HashSpan> hashSpans = new LinkedList<HashSpan>();
                hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST.getPrevious().getPrevious()));
                hashSpans.add(new HashSpan(HashSpan.LAST.getPrevious(), HashSpan.LAST));

                // collection
                HashSpanCollection hsc = new HashSpanCollection(hashSpans);

                // verify
                assertEquals(true, hsc.getMissingHashSpans().isEmpty());
            }
        }
        // negative
        {
            // none
            {
                // spans
                List<HashSpan> hashSpans = new LinkedList<HashSpan>();

                // collection
                HashSpanCollection hsc = new HashSpanCollection(hashSpans);

                // verify
                Collection<HashSpan> missing = hsc.getMissingHashSpans();
                assertEquals(1, missing.size());
                HashSpan[] array = missing.toArray(new HashSpan[0]);
                assertEquals(array[0].getFirst().toString(), HashSpan.FIRST.toString());
                assertEquals(array[0].getLast().toString(), HashSpan.LAST.toString());
            }
            // hole at start
            {
                // spans
                List<HashSpan> hashSpans = new LinkedList<HashSpan>();
                hashSpans.add(new HashSpan(HashSpan.FIRST.getNext(), HashSpan.LAST));

                // collection
                HashSpanCollection hsc = new HashSpanCollection(hashSpans);

                // verify
                Collection<HashSpan> missing = hsc.getMissingHashSpans();
                assertEquals(1, missing.size());
                HashSpan[] array = missing.toArray(new HashSpan[0]);
                assertEquals(HashSpan.FIRST.toString(), array[0].getFirst().toString());
                assertEquals(HashSpan.FIRST.toString(), array[0].getLast().toString());
            }
            // hole at end
            {
                // spans
                List<HashSpan> hashSpans = new LinkedList<HashSpan>();
                hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST.getPrevious()));

                // collection
                HashSpanCollection hsc = new HashSpanCollection(hashSpans);

                // verify
                Collection<HashSpan> missing = hsc.getMissingHashSpans();
                assertEquals(1, missing.size());
                HashSpan[] array = missing.toArray(new HashSpan[0]);
                assertEquals(HashSpan.LAST.toString(), array[0].getFirst().toString());
                assertEquals(HashSpan.LAST.toString(), array[0].getLast().toString());
            }
            // hole in the middle
            {
                // spans
                List<HashSpan> hashSpans = new LinkedList<HashSpan>();
                BigHash middleHash = DevUtil.getRandomBigHash();
                hashSpans.add(new HashSpan(HashSpan.FIRST, middleHash.getPrevious()));
                hashSpans.add(new HashSpan(middleHash.getNext(), HashSpan.LAST));

                // collection
                HashSpanCollection hsc = new HashSpanCollection(hashSpans);

                // verify
                Collection<HashSpan> missing = hsc.getMissingHashSpans();
                assertEquals(1, missing.size());
                HashSpan[] array = missing.toArray(new HashSpan[0]);
                assertEquals(middleHash.toString(), array[0].getFirst().toString());
                assertEquals(middleHash.toString(), array[0].getLast().toString());
            }
        }
    }

    public void testAreEqual() throws Exception {
        // positive
        {
            Collection<HashSpan> c1 = new HashSet<HashSpan>();
            Collection<HashSpan> c2 = new HashSet<HashSpan>();
            HashSpan hs1 = DevUtil.makeRandomHashSpan();
            HashSpan hs2 = hs1.clone();
            c1.add(hs1);
            c2.add(hs2);

            assertTrue(HashSpanCollection.areEqual(c1, c2));
        }
        // negative
        {
            // different size
            {
                Collection<HashSpan> c1 = new HashSet<HashSpan>();
                Collection<HashSpan> c2 = new HashSet<HashSpan>();
                c1.add(DevUtil.makeRandomHashSpan());
                c1.add(DevUtil.makeRandomHashSpan());
                c2.add(DevUtil.makeRandomHashSpan());

                assertFalse(HashSpanCollection.areEqual(c1, c2));
            }
            // different element
            {
                Collection<HashSpan> c1 = new HashSet<HashSpan>();
                Collection<HashSpan> c2 = new HashSet<HashSpan>();
                HashSpan hs1 = DevUtil.makeRandomHashSpan();
                HashSpan hs2 = DevUtil.makeRandomHashSpan();
                c1.add(hs1);
                c2.add(hs2);

                assertFalse(HashSpanCollection.areEqual(c1, c2));
            }
        }
    }

    public void testMerge() throws Exception {
        // positive
        {
            HashSpan hs1 = DevUtil.makeRandomHashSpan();
            HashSpan hs2 = new HashSpan(hs1.getFirst().getNext(), hs1.getLast().getNext());

            HashSpan merged = HashSpanCollection.merge(hs1, hs2);
            assertEquals(hs1.getFirst(), merged.getFirst());
            assertEquals(hs2.getLast(), merged.getLast());
        }
        // negative (not overlapping
        {
            HashSpan hs1 = DevUtil.makeRandomHashSpan();
            HashSpan hs2 = new HashSpan(hs1.getLast().getNext().getNext(), hs1.getLast().getNext().getNext());

            assertEquals(null, HashSpanCollection.merge(hs1, hs2));
        }
    }

    public void testMergeCollection() throws Exception {
        // merge occurs
        {
            HashSpan hs = DevUtil.makeRandomHashSpan();
            HashSpan hs2 = new HashSpan(hs.getLast().getPrevious(), DevUtil.getRandomBigHash());

            List<HashSpan> c = new LinkedList<HashSpan>();
            c.add(hs);
            c.add(hs2);

            HashSpanCollection.merge(c);
            assertEquals(1, c.size());
        }
        // no merge occurs
        {
            HashSpan hs = DevUtil.makeRandomHashSpan();
            HashSpan hs2 = new HashSpan(hs.getLast().getNext().getNext(), hs.getLast().getNext().getNext().getNext());

            List<HashSpan> c = new LinkedList<HashSpan>();
            c.add(hs);
            c.add(hs2);

            HashSpanCollection.merge(c);
            assertEquals(2, c.size());
        }
    }
}
