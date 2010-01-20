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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.tranche.util.TrancheTestCase;
import org.tranche.util.Utils;

/**
 * Tests to make sure that the DiskBackedBigHashList works as expected.
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class DiskBackedBigHashListTest extends TrancheTestCase {

    /**
     * Simple test that adds a bunch of adds, makes sure partitions were created.
     */
    private void simpleAdds(int sampleSize) {
        DiskBackedBigHashList dlist = null;

        try {

            dlist = new DiskBackedBigHashList();

            assertEquals("Should be zero records.", 0, dlist.size());
            assertEquals("Shouldn't be any partitions.", 0, dlist.paritionsOnDisk());
            assertEquals("Shouldn't be any records in memory.", 0, dlist.recordsInMemory());
            assertEquals("Shouldn't be any records on disk.", 0, dlist.recordsOnDisk());

            // Add number of BigHashes equal to test parameter
            for (int i = 0; i < sampleSize; i++) {
                dlist.add(generateBigHash(512));
            }

            int numPartitions = (sampleSize - 1) / DiskBackedBigHashList.recordsPerPartition();
            int numMemory = (sampleSize - 1) % DiskBackedBigHashList.recordsPerPartition() + 1;
            int numDisk = sampleSize - numMemory;

            assertEquals("Should be " + sampleSize + " records.", sampleSize, dlist.size());
            assertEquals("Should be" + numPartitions + " partitions.", numPartitions, dlist.paritionsOnDisk());
            assertEquals("Should be" + numMemory + " records in memory.", numMemory, dlist.recordsInMemory());
            assertEquals("Should be" + numDisk + " on disk.", numDisk, dlist.recordsOnDisk());

        } catch (IOException e) {
            System.err.println("An IOException occurred with the following message: " + e.getMessage());
        } finally {
        }
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    public void testAddSmall() {
        simpleAdds(9999);
    }

    public void testAddMedium() {
        simpleAdds(20000);
    }

    public void testAddLarge() {
        simpleAdds(30001);
    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // Tests sanity of function that determines what is on disk and what isn't
    public void testDiskVersusMemory() {

        DiskBackedBigHashList dlist = null;
        int sampleSize = 20001;

        try {

            dlist = new DiskBackedBigHashList();

            assertEquals("Should be zero records.", 0, dlist.size());
            assertEquals("Shouldn't be any partitions.", 0, dlist.paritionsOnDisk());
            assertEquals("Shouldn't be any records in memory.", 0, dlist.recordsInMemory());
            assertEquals("Shouldn't be any records on disk.", 0, dlist.recordsOnDisk());

            // Add number of BigHashes equal to test parameter
            for (int i = 0; i < sampleSize; i++) {
                dlist.add(generateBigHash(512));
            }

            int numPartitions = (sampleSize - 1) / DiskBackedBigHashList.recordsPerPartition();
            int numMemory = (sampleSize - 1) % DiskBackedBigHashList.recordsPerPartition() + 1;
            int numDisk = sampleSize - numMemory;

            assertEquals("Should be " + sampleSize + " records.", sampleSize, dlist.size());
            assertEquals("Should be" + numPartitions + " partitions.", numPartitions, dlist.paritionsOnDisk());
            assertEquals("Should be" + numMemory + " records in memory.", numMemory, dlist.recordsInMemory());
            assertEquals("Should be" + numDisk + " on disk.", numDisk, dlist.recordsOnDisk());

            // Test sanity of onDisk boolean function... is it correct?
            for (int index = 0; index < numDisk; index++) {
                assertTrue("Index " + index + " should be on disk.", dlist.recordOnDisk(index));
            }

            // Make sure rest are in memory
            for (int index = numDisk; index < sampleSize; index++) {
                assertFalse("Index " + index + " should be in memory.", dlist.recordOnDisk(index));
            }

        } catch (IOException e) {
            System.err.println("An IOException occurred with the following message: " + e.getMessage());
        } finally {
        }

    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // Tests DiskBackedHashListIterator
    public void testIterator() {

        DiskBackedBigHashList dlist = null;
        List hashes = new ArrayList();

        int sampleSize = 12000; // Keep it small

        try {

            dlist = new DiskBackedBigHashList();

            assertEquals("Should be zero records.", 0, dlist.size());
            assertEquals("Shouldn't be any partitions.", 0, dlist.paritionsOnDisk());
            assertEquals("Shouldn't be any records in memory.", 0, dlist.recordsInMemory());
            assertEquals("Shouldn't be any records on disk.", 0, dlist.recordsOnDisk());

            // Add same hash to both collections
            BigHash nextHash = null;
            for (int i = 0; i < sampleSize; i++) {

                nextHash = generateBigHash(512);

                hashes.add(nextHash);
                dlist.add(nextHash);
            }

            assertEquals("Two collections should be same size.", dlist.size(), hashes.size());

            Iterator it = dlist.iterator();

            int i = 0;
            while (it.hasNext()) {
                assertEquals("Hashes should be equal.", it.next(), hashes.get(i));
                i++;
            }

        } catch (IOException e) {
            System.err.println("An IOException occurred with the following message: " + e.getMessage());
        } finally {
        }

    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // Tests that hashes get method
    public void simpleGets(int sampleSize) {

        DiskBackedBigHashList dlist = null;
        List hashes = new ArrayList();

        try {

            dlist = new DiskBackedBigHashList();

            assertEquals("Should be zero records.", 0, dlist.size());
            assertEquals("Shouldn't be any partitions.", 0, dlist.paritionsOnDisk());
            assertEquals("Shouldn't be any records in memory.", 0, dlist.recordsInMemory());
            assertEquals("Shouldn't be any records on disk.", 0, dlist.recordsOnDisk());

            // Add same hash to both collections
            BigHash nextHash = null;
            for (int i = 0; i < sampleSize; i++) {

                nextHash = generateBigHash(512);

                hashes.add(nextHash);
                dlist.add(nextHash);
            }

            assertEquals("Two collections should be same size.", dlist.size(), hashes.size());

            // Make sure they are all the same
            for (int i = 0; i < dlist.size(); i++) {
                assertEquals("Hashes should be equal.", dlist.get(i), hashes.get(i));
            }

        } catch (IOException e) {
            System.err.println("An IOException occurred with the following message: " + e.getMessage());
        } finally {
        }

    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    public void testGetSmall() {
        simpleGets(9999);
    }

    public void testGetMedium() {
        simpleGets(20000);
    }

    public void testGetLarge() {
        simpleGets(30001);
    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // Generate a random hash. Separate function perchance better way to do this.
    private BigHash generateBigHash(int size) {

        return BigHash.createFromBytes(Utils.makeRandomData(size));
    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // Tests that are used purely as time trial -- no assertions, just want
    // to see how long. (Reason: avoid java.lang.OutOfMemoryError)
    public void simpleTimeTrial(int sampleSize) {

        DiskBackedBigHashList dlist = null;

        try {

            dlist = new DiskBackedBigHashList();

            // Add same hash to both collections
            BigHash nextHash = null;

            for (int i = 0; i < sampleSize; i++) {

                nextHash = generateBigHash(512);
                dlist.add(nextHash);
            }

            // No get each hash
            for (int i = 0; i < sampleSize; i++) {
                dlist.get(i);
            }
        } catch (IOException e) {
            System.err.println("An IOException occurred with the following message: " + e.getMessage());
        } finally {
        }

    }

    // Large time trial
    // Uncomment to test time. No need to test on build farm... doesn't assert
    // anything.
//    public void testTimeTrialSmall() {
//        System.out.println("testTimeTrialSmall()");
//        simpleTimeTrial(10001);
//    }
//
//    public void testTimeTrialMedium() {
//        System.out.println("testTimeTrialMedium()");
//        simpleTimeTrial(30001);
//    }
//
//        public void testTimeTrialLarge() {
//            System.out.println("testTimeTrialLarge()");
//            simpleTimeTrial(100001);
//        }
//
//    public void testTimeTrialHumonguous() {
//        System.out.println("testTimeTrialHumonguous()");
//        simpleTimeTrial(1000001);
//    }
}
