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
package org.tranche.license;

import org.tranche.util.TrancheTestCase;

/**
 * 
 */
public class LicenseTest extends TrancheTestCase {

    public void testToString() {
        License instance = new License("My License", "Short Description", "Description", false);
        String expResult = "My License";
        String result = instance.toString();
        assertEquals(expResult, result);

        instance = new License("My License", "Short Description", "Description", false);
        expResult = "My License";
        result = instance.toString();
        assertEquals(expResult, result);
    }

    public void testGetShortDescription() {
        License instance = new License("My License", "Short Description", "Description", false);
        String expResult = "Short Description";
        String result = instance.getShortDescription();
        assertEquals(expResult, result);
    }

    public void testSetShortDescription() {
        String shortDescription = "New Short Description";
        License instance = new License("My License", "Short Description", "Description", false);
        instance.setShortDescription(shortDescription);
        assertEquals(shortDescription, instance.getShortDescription());
    }

    public void testGetTitle() {
        License instance = new License("My License", "Short Description", "Description", false);
        String expResult = "My License";
        String result = instance.getTitle();
        assertEquals(expResult, result);
    }

    public void testSetTitle() {
        String title = "New My License";
        License instance = new License("My License", "Short Description", "Description", false);
        instance.setTitle(title);
        assertEquals(title, instance.getTitle());
    }

    public void testGetDescription() {
        License instance = new License("My License", "Short Description", "Description", false);
        String expResult = "Description";
        String result = null;
        try {
            result = instance.getDescription();
        } catch (Exception e) {
            fail("Exception thrown in getting description.");
        }
        assertEquals(expResult, result);
    }

    public void testSetDescription() {
        String description = "New Description";
        License instance = new License("My License", "Short Description", "Description", false);
        instance.setDescription(description);
        try {
            assertEquals(description, instance.getDescription());
        } catch (Exception e) {
            fail("Exception thrown in getting description.");
        }
    }
}