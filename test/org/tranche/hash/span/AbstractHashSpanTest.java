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
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class AbstractHashSpanTest extends TrancheTestCase {

    public void testFullHashSpan() throws Exception {
        AbstractHashSpan ahs = new AbstractHashSpan(HashSpan.FULL);
        assertEquals(ahs.getFirst(), HashSpan.FIRST);
        assertEquals(ahs.getLast(), HashSpan.LAST);
        assertEquals(ahs.getAbstractionFirst(), AbstractHashSpan.ABSTRACTION_FIRST);
        assertEquals(ahs.getAbstractionLast(), AbstractHashSpan.ABSTRACTION_LAST);
    }

    public void testValueOf() throws Exception {
        // min
        BigHash min = AbstractHashSpan.valueOf(AbstractHashSpan.ABSTRACTION_FIRST);
        assertEquals(HashSpan.FIRST, min);
        // max
        BigHash max = AbstractHashSpan.valueOf(AbstractHashSpan.ABSTRACTION_LAST);
        assertEquals(HashSpan.LAST, max);
    }

}
