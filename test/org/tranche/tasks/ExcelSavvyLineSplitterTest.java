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
package org.tranche.tasks;

import org.tranche.util.TrancheTestCase;

/**
 * Tests parsing the most commonly found types of Excel formatted .CSV data that 
 * normally screw up a String.split(",") invocation.
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class ExcelSavvyLineSplitterTest extends TrancheTestCase {

    public void testSimpleLineSplitting() throws Exception {
        // the line of interest
        String line = "a,b,c,d";
        // split the line
        String[] parts = ExcelSavvyLineSplitter.split(line);
        assertEquals("Expected 4 parts.", 4, parts.length);
        assertEquals(parts[0], "a");
        assertEquals(parts[1], "b");
        assertEquals(parts[2], "c");
        assertEquals(parts[3], "d");
    }

    public void testQuotedLineSplitting() throws Exception {
        // the line of interest
        String line = "\"a\",\"b\",\"c\",\"d\"";
        // split the line
        String[] parts = ExcelSavvyLineSplitter.split(line);
        assertEquals("Expected 4 parts.", 4, parts.length);
        assertEquals(parts[0], "a");
        assertEquals(parts[1], "b");
        assertEquals(parts[2], "c");
        assertEquals(parts[3], "d");
    }

    public void testQuotedSometimesLineSplitting() throws Exception {
        // the line of interest
        String line = "\"a\",b,c,\"d\"";
        // split the line
        String[] parts = ExcelSavvyLineSplitter.split(line);
        assertEquals("Expected 4 parts.", 4, parts.length);
        assertEquals(parts[0], "a");
        assertEquals(parts[1], "b");
        assertEquals(parts[2], "c");
        assertEquals(parts[3], "d");
    }

    public void testQuoteInQuotedSometimesLineSplitting() throws Exception {
        // the line of interest
        String line = "\"a\"\"\",b,c,\"d\"";
        // split the line
        String[] parts = ExcelSavvyLineSplitter.split(line);
        assertEquals("Expected 4 parts.", 4, parts.length);
        assertEquals(parts[0], "a\"");
        assertEquals(parts[1], "b");
        assertEquals(parts[2], "c");
        assertEquals(parts[3], "d");
    }

    public void testCommaInQuotedSometimesLineSplitting() throws Exception {
        // the line of interest
        String line = "\"a,\",b,\",c\",\"d\"";
        // split the line
        String[] parts = ExcelSavvyLineSplitter.split(line);
        assertEquals("Expected 4 parts.", 4, parts.length);
        assertEquals(parts[0], "a,");
        assertEquals(parts[1], "b");
        assertEquals(parts[2], ",c");
        assertEquals(parts[3], "d");
    }

    public void testBeginningWithSpaceSplitting() throws Exception {
        // the line of interest
        String line = " a, b, c, d";
        // split the line
        String[] parts = ExcelSavvyLineSplitter.split(line);
        assertEquals("Expected 4 parts.", 4, parts.length);
        assertEquals(parts[0], "a");
        assertEquals(parts[1], "b");
        assertEquals(parts[2], "c");
        assertEquals(parts[3], "d");
    }
}
