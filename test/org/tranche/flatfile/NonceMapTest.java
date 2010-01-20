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
package org.tranche.flatfile;

import java.io.File;
import java.util.ArrayList;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class NonceMapTest extends TrancheTestCase {

    public void testSingleNonce() {
        TestUtil.printTitle("NonceMapTest:testSingleNonce()");

        FlatFileTrancheServer ffts = null;
        File home = null;
        try {

            // Create ffts
            home = TempFileUtil.createTemporaryDirectory();
            ffts = new FlatFileTrancheServer(home);

            NonceMap nm = new NonceMap(ffts);
            byte[] nonce = nm.newNonce();
            assertTrue("Expected nonce to exist.", nm.contains(nonce));
            assertEquals("NonceMap should have one element.", 1, nm.size());

            // remove it
            nm.remove(nonce);
            assertFalse("Expected nonce to not exist.", nm.contains(nonce));
            assertEquals("NonceMap should be empty.", 0, nm.size());
        } finally {
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDelete(home);
        }
    }

    public void testTooManyNonces() throws Exception {
        TestUtil.printTitle("NonceMapTest:testTooManyNonces()");
        FlatFileTrancheServer ffts = null;
        File home = null;
        try {

            // Create ffts
            home = TempFileUtil.createTemporaryDirectory();
            ffts = new FlatFileTrancheServer(home);

            ArrayList<byte[]> nonces = new ArrayList();
            NonceMap nm = new NonceMap(ffts);
            for (int i = 0; i <= nm.getMaximumSize(); i++) {
                nonces.add(nm.newNonce());
            }

            // check that the expected nonce is gone
            assertFalse("Expected this nonce is gone.", nm.contains(nonces.get(0)));

            // assert all of the others are there
            for (int i = 1; i < nonces.size(); i++) {
                assertTrue("Expected nonce  " + i + " to exist.", nm.contains(nonces.get(i)));
            }
        } finally {
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDelete(home);
        }
    }

    public void testSetAndGetSizes() throws Exception {
        TestUtil.printTitle("NonceMapTest:testSetAndGetSizes()");
        FlatFileTrancheServer ffts = null;
        File home = null;
        try {

            // Create ffts
            home = TempFileUtil.createTemporaryDirectory();
            ffts = new FlatFileTrancheServer(home);
            ffts.getConfiguration().getUsers().add(DevUtil.getDevUser());

            NonceMap nm = new NonceMap(ffts);

            nm.setMaximumSize(NonceMap.DEFAULT_MAX_NONCES, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
            assertEquals("Shouldn't have problem setting size of NonceMap.", nm.getMaximumSize(), NonceMap.DEFAULT_MAX_NONCES);

            nm.setMaximumSize(25, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
            assertEquals("Shouldn't have problem setting size of NonceMap.", nm.getMaximumSize(), 25);

            nm.setMaximumSize(NonceMap.DEFAULT_MAX_NONCES, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
            assertEquals("Shouldn't have problem setting size of NonceMap.", nm.getMaximumSize(), NonceMap.DEFAULT_MAX_NONCES);

            assertEquals("NonceMap should be empty.", 0, nm.size());
        } finally {
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDelete(home);
        }
    }
}
