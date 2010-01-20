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
import org.tranche.util.DevUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author besmit
 */
public class HashSpanTest extends TrancheTestCase {

    public void testClone() throws Exception {
        // Make up an end hash. Cannot be equal to absolute last for test purposes.
        BigHash endTmp = DevUtil.getRandomBigHash();
        while (endTmp.equals(HashSpan.LAST)) {
            endTmp = DevUtil.getRandomBigHash();
        }

        final BigHash end = endTmp;
        HashSpan span = new HashSpan(HashSpan.FIRST, end);
        HashSpan clone = span.clone();

        assertEquals("Should have same info.", span, clone);
        assertNotSame("Should not have same reference.", span, clone);

        clone.setLast(HashSpan.LAST);

        assertFalse("Should not longer have same info.", span.equals(clone));
        assertNotSame("Should not have same reference.", span, clone);

        // Make sure original didn't change
        assertEquals("Original should still have same last hash.", end, span.getLast());

        String randomobject = new String("Random");
        assertFalse("Non-similarobjects should not be equal", span.equals(randomobject));
    }
}