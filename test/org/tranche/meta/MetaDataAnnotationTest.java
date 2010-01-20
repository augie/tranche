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
package org.tranche.meta;

import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class MetaDataAnnotationTest extends TrancheTestCase {

    public void testCreate() throws Exception {
        String name = "Hello From";
        String value = "Philadelphia";
        MetaDataAnnotation mda = new MetaDataAnnotation(name, value);

        assertEquals(name, mda.getName());
        assertEquals(value, mda.getValue());
    }

    public void testCreateFromString() throws Exception {
        String name = "Hello From";
        String value = "Philadelphia";
        String toString = name + MetaDataAnnotation.DELIMITER + value;
        MetaDataAnnotation mda = MetaDataAnnotation.createFromString(toString);

        assertEquals(name, mda.getName());
        assertEquals(value, mda.getValue());
    }

    public void testSetName() throws Exception {
        String name = "Hello From";
        String value = "Philadelphia";
        MetaDataAnnotation mda = new MetaDataAnnotation(name, value);

        name = "Hello To";
        mda.setName(name);

        assertEquals(name, mda.getName());
        assertEquals(value, mda.getValue());
    }

    public void testSetValue() throws Exception {
        String name = "Hello From";
        String value = "Philadelphia";
        MetaDataAnnotation mda = new MetaDataAnnotation(name, value);

        value = "Philadelphia Convention Center";
        mda.setValue(value);

        assertEquals(name, mda.getName());
        assertEquals(value, mda.getValue());
    }

    public void testTokenizeDetokenize() throws Exception {
        MetaDataAnnotation mda1 = new MetaDataAnnotation("Hello From", "Atlanta");
        String mda1Str = mda1.toString();
        assertEquals("Should rebuild correctly.", mda1, MetaDataAnnotation.createFromString(mda1Str));

        MetaDataAnnotation mda2 = new MetaDataAnnotation("Time", "7:32 AM");
        String mda2Str = mda2.toString();
        assertEquals("Should rebuild correctly.", mda2, MetaDataAnnotation.createFromString(mda2Str));
    }

    public void testEquals() throws Exception {
        MetaDataAnnotation mda1 = new MetaDataAnnotation("Hello From", "Atlanta");
        MetaDataAnnotation mda2 = new MetaDataAnnotation("Hello From", "Atlanta");
        MetaDataAnnotation mda3 = new MetaDataAnnotation("Hello From", "Chicago");
        MetaDataAnnotation mda4 = new MetaDataAnnotation("Hello To", "Atlanta");

        assertTrue(mda1.equals(mda2));
        assertFalse(mda1.equals(mda3));
        assertFalse(mda1.equals(mda4));
    }
}
