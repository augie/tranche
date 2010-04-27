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
package org.tranche.network;

import org.tranche.commons.DebugUtil;
import org.tranche.commons.RandomUtil;
import org.tranche.util.TestUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class StatusTableRowRangeTest extends NetworkPackageTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        DebugUtil.setDebug(StatusTableRowRange.class, true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        DebugUtil.setDebug(StatusTableRowRange.class, false);
    }

    public void testCreate() throws Exception {
        TestUtil.printTitle("StatusTableRowRangeTest:testCreate()");
        
        // vars
        String str1 = RandomUtil.getString(10);
        String str2 = RandomUtil.getString(10);

        // create range
        StatusTableRowRange strr = new StatusTableRowRange(str1, str2);

        // verify
        assertEquals(str1, strr.getFrom());
        assertEquals(str2, strr.getTo());
    }
}
