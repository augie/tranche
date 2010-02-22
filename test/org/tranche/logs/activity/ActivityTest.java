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
package org.tranche.logs.activity;

import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ActivityTest extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testToAndFromByteArray() throws Exception {
        TestUtil.printTitle("ActivityTest:testToAndFromByteArray()");
        
        Activity activity1 = ActivityLogUtilTest.getRandomActivity();

        byte[] activity1Bytes = activity1.toByteArray();
        Activity activity1Verify = new Activity(activity1Bytes);
        assertEquals("Activities should be equal.", activity1, activity1Verify);

        Activity activity2 = ActivityLogUtilTest.getRandomActivity();

        assertFalse("Randomly generated activities should not be equal. The odds are mind-bendingly low.", activity1.equals(activity2));

        byte[] activity2Bytes = activity2.toByteArray();
        Activity activity2Verify = new Activity(activity2Bytes);
        assertEquals("Activities should be equal.", activity2, activity2Verify);

        assertFalse("The two verification activities should not be equal.", activity1Verify.equals(activity2Verify));
        assertFalse("The two verification activities should not be equal.", activity2Verify.equals(activity1Verify));
    }
}
