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
package org.tranche.hash;

import org.tranche.util.TrancheTestCase;

/**
 * Tests the base 16 encoding.
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class Base16Test extends TrancheTestCase {

    public void testBase16Conversion() throws Exception {
        String sig = "0123456789abcdef";
        byte[] bytes = Base16.decode(sig);
        assertEquals("String to Base16 back to String should be identical.", sig, Base16.encode(bytes));
    }
}
