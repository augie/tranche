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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.tranche.time.TimeUtil;
import org.tranche.util.DevUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author Adam Giacobbe - agiac@umich.edu
 */
public class DiskBackedBigHashSetTest extends TrancheTestCase {

    /**
     * <p>Simple test adds some items, makes sure that duplicates don't impact size.</p>
     * @throws java.lang.Exception
     */
    public void testActsAsSet() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testActsAsSet()");
        DiskBackedBigHashSet dset = null;
        final int sizeToAdd = 100;
        try {
            dset = new DiskBackedBigHashSet();
            dset.setTestBufferSize(25);

            Set<BigHash> addedHashes = new HashSet();

            for (int i = 0; i < sizeToAdd; i++) {
                BigHash h = DevUtil.getRandomBigHash();
                if (!addedHashes.contains(h)) {
                    dset.add(h);
                    addedHashes.add(h);
                } else {
                    // In unlikely scenario created same BigHash, set back counter to re-run
                    i--;
                    continue;
                }
            }

            assertEquals("Expecting HashSet to be correct size.", sizeToAdd, addedHashes.size());
            assertEquals("Expecting SimpleDiskBackedBigHashSet to be correct size.", sizeToAdd, dset.size());

            // Re-add the hashes. Shouldn't change size.
            for (BigHash h : addedHashes) {
                dset.add(h);
            }
            assertEquals("Expecting SimpleDiskBackedBigHashSet to be correct size.", sizeToAdd, dset.size());

        } finally {
            if (dset != null) {
                dset.close();
            }
        }
    }

    /**
     * <p>This is a speed test. Checking two modes:</p>
     * <ul>
     *   <li>Flush mode: before critical operations, flush out memory items to disk for accuracy.</li>
     *   <li>Other mode: don't flush until reaches critical size.</li>
     * </ul>
     * @throws java.lang.Exception
     */
    public void testSpeedDifferenceBetweenBufferAndWrite() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testSpeedDifferenceBetweenBufferAndWrite()");
        DiskBackedBigHashSet setNoFlush = null,
                setFlush = null;

        try {
            setNoFlush = new DiskBackedBigHashSet();
            setNoFlush.setAutoWriteBeforeCriticalOperation(false);
            setNoFlush.setTestBufferSize(100);

            setFlush = new DiskBackedBigHashSet();
            setFlush.setAutoWriteBeforeCriticalOperation(true);
            setFlush.setTestBufferSize(100);

            List<BigHash> hashes = new ArrayList();

            // Create 1000 hashes
            for (int i = 0; i < 1000; i++) {
                BigHash h = DevUtil.getRandomBigHash();
                if (!hashes.contains(h)) {
                    hashes.add(h);
                } else {
                    i--;
                    continue;
                }
            }

            assertEquals("Expecting 1000 hashes.", 1000, hashes.size());

            /**
             * Test auto write
             */
            long start = TimeUtil.getTrancheTimestamp();
            for (BigHash h : hashes) {
                setFlush.add(h);

                // Check size
                setFlush.size();
            }
            final long setFlushTime = TimeUtil.getTrancheTimestamp() - start;

            /**
             * Test not auto write
             */
            start = TimeUtil.getTrancheTimestamp();
            for (BigHash h : hashes) {
                setNoFlush.add(h);

                // Check size
                setNoFlush.size();
            }
            final long setNoFlushTime = TimeUtil.getTrancheTimestamp() - start;

            System.out.println("Time with auto-flush...... " + Text.getPrettyEllapsedTimeString(setFlushTime));
            System.out.println("Time without auto-flush... " + Text.getPrettyEllapsedTimeString(setNoFlushTime));

            assertTrue("Expecting non-auto-flush mode to be quicker<no auto=" + Text.getPrettyEllapsedTimeString(setNoFlushTime) + ",auto=" + Text.getPrettyEllapsedTimeString(setFlushTime) + ">", setFlushTime > setNoFlushTime);

        } finally {
            if (setNoFlush != null) {
                setNoFlush.close();
            }
            if (setFlush != null) {
                setFlush.close();
            }
        }
    }

    /**
     * Add some hashes, then delete them.
     * @throws java.lang.Exception
     */
    public void testDelete() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testDelete()");
        DiskBackedBigHashSet dset = null;

        try {
            dset = new DiskBackedBigHashSet();
            dset.setTestBufferSize(25);

            Set<BigHash> hashesToKeep = new HashSet();
            Set<BigHash> hashesToDelete = new HashSet();

            // Add some hashes
            for (int i = 0; i < 500; i++) {
                BigHash h = DevUtil.getRandomBigHash();
                if (!hashesToKeep.contains(h) && !hashesToDelete.contains(h)) {
                    dset.add(h);

                    if (i < 100) {
                        hashesToDelete.add(h);
                    } else {
                        hashesToKeep.add(h);
                    }
                } else {
                    i--;
                    continue;
                }
            }

            assertEquals("Expecting certain number of entries.", 500, dset.size());

            for (BigHash h : hashesToDelete) {
                dset.delete(h);
            }

            assertEquals("Expecting certain number of entries.", 500 - hashesToDelete.size(), dset.size());
            for (BigHash h : hashesToDelete) {
                assertFalse("Better not contain.", dset.contains(h));
            }

        } finally {
            if (dset != null) {
                dset.close();
            }
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testAddDeleteThenReAdd() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testAddDeleteThenReAdd()");
        DiskBackedBigHashSet setFlush = null;

        try {

            setFlush = new DiskBackedBigHashSet();
            setFlush.setAutoWriteBeforeCriticalOperation(true);
            setFlush.setTestBufferSize(10);

            List<BigHash> hashes = new ArrayList();

            // Create 30 hashes
            for (int i = 0; i < 30; i++) {
                BigHash h = DevUtil.getRandomBigHash();
                if (!hashes.contains(h)) {
                    setFlush.add(h);
                    hashes.add(h);
                } else {
                    i--;
                    continue;
                }
            }

            assertEquals("Expecting 30 hashes.", 30, setFlush.size());
            assertEquals("Expecting 30 hashes.", 30, hashes.size());
            BigHash randomHash = hashes.get(RandomUtil.getInt(hashes.size()));

            setFlush.delete(randomHash);

            assertFalse("Shouldn't contain hash.", setFlush.contains(randomHash));

            setFlush.add(randomHash);

            assertTrue("Should contain hash.", setFlush.contains(randomHash));

            assertEquals("Expecting 30 hashes.", 30, setFlush.size());

        } finally {
            if (setFlush != null) {
                setFlush.close();
            }
        }
    }

    public void testSimplyAddAndVerifySmall() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testSimplyAddAndVerifySmall()");
        testSimplyAddAndVerify(1000);
    }

    public void testSimplyAddAndVerifyMedium() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testSimplyAddAndVerifyMedium()");
        testSimplyAddAndVerify(10000);
    }

    private void testSimplyAddAndVerify(int size) throws Exception {

        Set<BigHash> hashes = new HashSet();
        DiskBackedBigHashSet dset = null;

        try {
            dset = new DiskBackedBigHashSet();
            dset.setTestBufferSize(size / 10);

            while (hashes.size() < size) {
                BigHash h = DevUtil.getRandomBigHash();

                if (hashes.contains(h)) {
                    continue;
                }

                dset.add(h);
                hashes.add(h);
            }

            assertEquals("Expecting certain size.", size, dset.size());
            assertEquals("Expecting certain size.", size, hashes.size());

            for (BigHash h : hashes) {
                assertTrue("Better contain hash.", dset.contains(h));
            }
        } finally {
            if (dset != null) {
                dset.close();
            }
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testDeleteHahesNotContainedDoesNotChangeSize() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testDeleteHahesNotContainedDoesNotChangeSize()");
        Set<BigHash> hashes = new HashSet();

        int oldSize = 0;

        DiskBackedBigHashSet dset = null;

        final int hashesToAdd = 50;

        try {
            dset = new DiskBackedBigHashSet();
            dset.setAutoWriteBeforeCriticalOperation(true);
            dset.setTestBufferSize(hashesToAdd / 10);

            while (hashes.size() < hashesToAdd) {
                BigHash h = DevUtil.getRandomBigHash();

                if (hashes.contains(h)) {
                    continue;
                }

                dset.add(h);
                hashes.add(h);
            }

            oldSize = dset.size();

//            while (hashes.size() < hashesToAdd*2) {
            for (int i = 0; i < hashesToAdd; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                if (hashes.contains(h)) {
                    continue;
                }

                dset.delete(h);
            }

            assertEquals("Should still be a certain size.", hashesToAdd, dset.size());

        } finally {
            if (dset != null) {
                dset.close();
            }
        }
    }

    public void testSizeNeverLessThanZeroWithAutoWrite() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testSizeNeverLessThanZeroWithAutoWrite()");
        testSizeNeverLessThanZero(true);
    }

    public void testSizeNeverLessThanZeroWithoutAutoWrite() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testSizeNeverLessThanZeroWithoutAutoWrite()");
        testSizeNeverLessThanZero(false);
    }

    /**
     * 
     * @param setAutoWrite
     * @throws java.lang.Exception
     */
    private void testSizeNeverLessThanZero(boolean setAutoWrite) throws Exception {

        DiskBackedBigHashSet dset = null;

        try {
            dset = new DiskBackedBigHashSet();
            dset.setTestBufferSize(10);
            dset.setAutoWriteBeforeCriticalOperation(setAutoWrite);

            for (int i = 0; i < 20; i++) {
                BigHash h = DevUtil.getRandomBigHash();

                dset.delete(h);

                assertEquals("Size better be zero.", 0, dset.size());
            }


        } finally {
            if (dset != null) {
                dset.close();
            }
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testGet() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testGet()");
        Set<BigHash> hashes = new HashSet();

        DiskBackedBigHashSet dset = null;

        final int batchSize = 10;
        final int hashesToAdd = 50 * batchSize;

        try {

            dset = new DiskBackedBigHashSet();
            dset.setTestBufferSize(hashesToAdd / 10);

            while (hashes.size() < hashesToAdd) {
                BigHash h = DevUtil.getRandomBigHash();

                if (hashes.contains(h)) {
                    continue;
                }

                dset.add(h);
                hashes.add(h);
            }

            // Copy of the set. Remove each as we find them below...
            Set<BigHash> checkSet = new HashSet();
            checkSet.addAll(hashes);

            assertEquals("Expecting both collections to be same size.", checkSet.size(), dset.size());

            int ptr;
            for (ptr = 0; ptr < hashesToAdd; ptr += batchSize) {
                List<BigHash> dsetHashes = dset.get(ptr, batchSize);

                assertEquals("Expecting certain size subset.", batchSize, dsetHashes.size());

                checkSet.removeAll(dsetHashes);
            }

            assertEquals("Expect pointer to end at hashesToAdd", hashesToAdd, ptr);
            assertEquals("Should have emptied the check set.", 0, checkSet.size());
        } finally {
            if (dset != null) {
                dset.close();
            }
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testGetFailsIfPastBounds() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testGetFailsIfPastBounds()");

        Set<BigHash> hashes = new HashSet();

        DiskBackedBigHashSet dset = null;

        int batchSize = 10;
        int hashesToTry = 5 * batchSize;

        try {

            dset = new DiskBackedBigHashSet();
            dset.setTestBufferSize(hashesToTry / 10);

            while (hashes.size() < hashesToTry) {
                BigHash h = DevUtil.getRandomBigHash();

                if (hashes.contains(h)) {
                    continue;
                }

                dset.add(h);
                hashes.add(h);
            }

            // Shouldn't return anything -- request is out of bounds
            List<BigHash> outOfBoundsList = dset.get(hashesToTry, batchSize);
            assertEquals("Should be empty.", 0, outOfBoundsList.size());

        } finally {
            if (dset != null) {
                dset.close();
            }
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testContainsTrueAndFalse() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testContainsTrueAndFalse()");
        Set<BigHash> hashesInDSet = new HashSet(),
                hashesNotInDSet = new HashSet();

        DiskBackedBigHashSet dset = null;
        int hashesToTry = 50;

        try {

            dset = new DiskBackedBigHashSet();
            dset.setTestBufferSize(hashesToTry / 10);

            while (hashesInDSet.size() < hashesToTry) {
                BigHash h = DevUtil.getRandomBigHash();
                if (hashesInDSet.contains(h)) {
                    continue;
                }
                dset.add(h);
                hashesInDSet.add(h);
            }

            while (hashesNotInDSet.size() < hashesToTry) {
                BigHash h = DevUtil.getRandomBigHash();
                if (hashesNotInDSet.contains(h)) {
                    continue;
                }
                hashesNotInDSet.add(h);
            }

            assertEquals("Expecting certain size.", hashesToTry, dset.size());
            assertEquals("Expecting certain size.", hashesToTry, hashesInDSet.size());
            assertEquals("Expecting certain size.", hashesToTry, hashesNotInDSet.size());

            for (BigHash h : hashesInDSet) {
                assertTrue("Should contain", dset.contains(h));
            }
            for (BigHash h : hashesNotInDSet) {
                assertFalse("Should not contain", dset.contains(h));
            }

        } finally {
            if (dset != null) {
                dset.close();
            }
        }
    }

    public void testDeleteBeforeAddHasNoImpact() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testDeleteBeforeAddHasNoImpact()");
        testDeleteBeforeAddHasNoImpact(false);
    }

    public void testDeleteBeforeAddHasNoImpactAuto() throws Exception {
        TestUtil.printTitle("DiskBackedBigHashSetTest:testDeleteBeforeAddHasNoImpactAuto()");
        testDeleteBeforeAddHasNoImpact(true);
    }

    /**
     * 
     * @param autoWrite
     * @throws java.lang.Exception
     */
    private void testDeleteBeforeAddHasNoImpact(boolean autoWrite) throws Exception {
        DiskBackedBigHashSet dset = null;
        int hashesToTry = 50;

        try {

            dset = new DiskBackedBigHashSet();
            dset.setAutoWriteBeforeCriticalOperation(autoWrite);
            dset.setTestBufferSize(hashesToTry / 10);

            for (int i = 0; i < hashesToTry; i++) {
                BigHash h = DevUtil.getRandomBigHash();
                if (dset.contains(h)) {
                    i--;
                    continue;
                }

                dset.delete(h);
                dset.add(h);
            }
            assertEquals("Expecting certain size collection.", hashesToTry, dset.size());

        } finally {
            if (dset != null) {
                dset.close();
            }
        }
    }
}