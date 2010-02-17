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

import junit.framework.TestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class TrancheTestCase extends TestCase {

    private final static boolean isTesting = TestUtil.isTesting();

    @Override()
    protected void setUp() throws Exception {
        TestUtil.setTesting(true);
        DebugUtil.setDebug(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        TempFileUtil.clear();
        TestUtil.setTesting(isTesting);
        DebugUtil.setDebug(false);
    }

}
