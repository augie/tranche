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
package org.tranche.project;

import org.tranche.hash.BigHash;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class DiskBackedProjectFilePartSetTest extends TrancheTestCase {

    /**
     * Shows two things.
     *  1. The set works with buffer/disk
     *  2. The items end up sorted
     */
    public void testDiskBackedProjectFilePartSet() throws Exception {
        // shrink the limit
        int maxBuffer = 10;
        int partsToMake = 21;

        // make up some parts -- more than the limit
        ProjectFilePart[] parts = new ProjectFilePart[partsToMake];
        // make all of the parts -- random, don't sort
        for (int i = 0; i < partsToMake; i++) {
            ProjectFilePart pfp = new ProjectFilePart("name" + Math.random(), new BigHash(new String("bytes" + i + "more" + Math.random()).getBytes()), new byte[0]);
            parts[i] = pfp;
        }

        // make a disk-backed set
        DiskBackedProjectFilePartSet set = new DiskBackedProjectFilePartSet();
        set.setMaxBufferSize(maxBuffer);
        for (ProjectFilePart pfp : parts) {
            set.add(pfp);
        }

        // should be the right size
        assertEquals("Set is the right size.", parts.length, set.size());

        // sort the parts to check that the disk-backed set did this
        Arrays.sort(parts);
        int index = 0;
        // check that they are all there
        for (ProjectFilePart pfp : set) {
            assertTrue("Expected parts to match.", pfp.compareTo(parts[index]) == 0);
            index++;
        }
        // the index should be correct
        assertEquals("Expected the index to be " + partsToMake, partsToMake, index);
    }

    public void testIterator() throws Exception {
        // shrink the limit
        int maxBuffer = 1000;
        int partsToMake = 2500;

        // make up some parts -- more than the limit
        ProjectFilePart[] parts = new ProjectFilePart[partsToMake];
        // make all of the parts -- random, don't sort
        for (int i = 0; i < partsToMake; i++) {
            ProjectFilePart pfp = new ProjectFilePart("name" + Math.random(), new BigHash(new String("bytes" + i + "more" + Math.random()).getBytes()), new byte[0]);
            parts[i] = pfp;
        }

        // make a disk-backed set
        DiskBackedProjectFilePartSet set = new DiskBackedProjectFilePartSet();
        set.setMaxBufferSize(maxBuffer);
        for (ProjectFilePart pfp : parts) {
            set.add(pfp);
        }

        // get all of them back
        ArrayList<ProjectFilePart> buffer = new ArrayList();
        for (ProjectFilePart pfp : parts) {
            buffer.add(pfp);
        }

        // should be the right size
        assertEquals("Set is the right size.", parts.length, set.size());
        assertEquals("Set is the right size.", parts.length, buffer.size());

        // sort the parts to check that the disk-backed set did this
        Arrays.sort(parts);
        Collections.sort(buffer);
        int index = 0;
        // check that they are all there
        for (ProjectFilePart pfp : set) {
            assertTrue("Expected parts to match.", pfp.compareTo(parts[index]) == 0);
            assertTrue("Expected parts to match.", pfp.compareTo(buffer.get(index)) == 0);
            index++;
        }
        // the index should be correct
        assertEquals("Expected the index to be " + partsToMake, partsToMake, index);
    }
}
