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
 * Some simple tests to show that the Base64 cleanup code works for the ways that Tranche hashes are commonly munged.
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class Base64UtilTest extends TrancheTestCase {

    /**
     *Test if the clean up code is working properly.
     */
    public void testCleanUpCode() throws Exception {
        // the String we expect
        String expected = "siQG5oyk956YTBLsV2RhNKPy6/kQOeHZot3V6qRN4/fp3AdMnZXP2PThHuudUtBcTjA9VIeent0gT3mBv1l751pIai0AAAAAAABKZw==";
        // with whitespace \n and \t
        String whitespace = "siQG5oyk956 YTBLsV2RhNKPy6/kQOeHZ  ot3V6qRN4/fp3AdMnZXP2PThHuudUtBcTjA9VIeent0gT3mBv1l751pIai0AAAAAAABKZw==";
        // with '>' and whitespace as in a e-mail
        String email = "> siQG5oyk956YTBLsV2RhNKPy6/kQOeHZot3V6qRN4/fp3AdMnZXP2PThHuudUtBcTjA9VIeent0g\n> T3mBv1l751pIai0AAAAAAABKZw==";
        // with -- and - as if hyphenated from manuscript
        String hyphen = "siQG5oyk956YTBLsV2RhNKPy6/kQOeHZot3V6qRN-4/fp3AdMnZXP2PThHuudUtBcTjA9VIeent0g--T3mBv1l751pIai0AAAAAAABKZw==";

        // check that each case is cleaned up
        assertEquals("Expected normal hashes to stay the same.", expected, Base64Util.cleanUpBase64(expected));
        assertEquals("Expected to clean up whitespace.", expected, Base64Util.cleanUpBase64(whitespace));
        assertEquals("Expected to clean up e-mail characters.", expected, Base64Util.cleanUpBase64(email));
        assertEquals("Expected clean up hyphens.", expected, Base64Util.cleanUpBase64(hyphen));
    }
}
