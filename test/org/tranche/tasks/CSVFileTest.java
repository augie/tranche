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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TrancheTestCase;

/**
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class CSVFileTest extends TrancheTestCase {
    
    /**
     * Create a CSVFile by hand, use the writer to write it to disk, then use reader to read, and make sure the result equals the initial object.
     */
    public void testReadWriteCSV() throws Exception {
        CSVFile csv1 = createTestCSVFileObj(), csv2;
        File file = csv1.getFile();
        
        try {
            
            // Write out the CSV file
            CSVWriter writer = new CSVWriter(csv1);
            writer.write();
            
            // Now read in the CSV file
            CSVReader reader = new CSVReader(file);
            csv2 = reader.read();
            
            assertEquals("Should be 4 records.", 4, csv2.getRecords().size());
            
            List<String> firstLegend = csv1.getLegend();
            List<String> secondLegend = csv2.getLegend();
            List<Map<String,String>> firstRecords = csv1.getRecords();
            List<Map<String,String>> secondRecords = csv2.getRecords();
            
            for (int i = 0; i < firstLegend.size(); i++) {
                assertEquals("Should be same keys, " + firstLegend.get(i) + " and " + secondLegend.get(i), firstLegend.get(i), secondLegend.get(i));
            }
            
            for (int i = 0; i < firstRecords.size(); i++) {
                
                Map<String,String> record1 = firstRecords.get(i);
                Map<String,String> record2 = secondRecords.get(i);
                
                for (int j = 0; j < firstLegend.size(); j++) {
                    assertEquals("Should be same item", record1.get(firstLegend.get(j)), record2.get(secondLegend.get(j)));
                }
                
            }
        }
        
        finally {
            IOUtil.safeDelete(file);
        }
    }
    
    /**
     * Tests that clones are deep copies
     */
    public void testDeepCopyClone() {
        
        CSVFile csvObj = createTestCSVFileObj();
        CSVFile csvObjClone = csvObj.clone();
        
        assertTrue("Expect clones to be equal.",csvObj.equals(csvObjClone));
        
        // Change original, make sure clone is different
        csvObj.removeRecord(0);
        
        assertFalse("Should no longer be equal.",csvObj.equals(csvObjClone));
    }
    
    
    /**
     * Test common operations on CSV file.
     */
    public void testCommonCSVOperations() throws Exception {
        
        CSVFile csvObj = createTestCSVFileObj();
        
        // Simulate a changed CSV. Backup using clone, then add to original.
        CSVFile csvObjBackup = csvObj.clone();
        
        Map<String,String> newRecord = new HashMap();
        newRecord.put("Toy Hash", "bbbbaaaa");
        newRecord.put("Passphrase", "default");
        newRecord.put("Day of week", "Sunday");
        csvObj.appendRecord(newRecord);
        
        assertFalse("Backup should be dated.",csvObj.equals(csvObjBackup));
        
        // Test print statement to find nulls
        csvObj.printContents(DevUtil.getNullPrintStream());
    }
    
    /**
     * Helper method to create a tricky CSV file for tests.
     */
    private CSVFile createTestCSVFileObj() {
        
        File file = TempFileUtil.createTemporaryFile();
        
        // Create CSV object by hand
        List<String> legend = new ArrayList();
        List<Map<String,String>> records = new ArrayList();
        
        String key1 = "Toy Hash",
                key2 = "Passphrase",
                key3 = "Day of week";
        
        legend.add(key1);
        legend.add(key2);
        legend.add(key3);
        
        Map<String,String> map = new HashMap();
        map.put(key1, "abcDEFghi");
        map.put(key2, "super,secret");
        map.put(key3, "Monday");
        records.add(map);
        
        map = new HashMap();
        map.put(key1, "1234567890");
        map.put(key2, "");
        map.put(key3, "Tuesday,Wednesday");
        records.add(map);
        
        map = new HashMap();
        map.put(key1, "asdfjkl;");
        map.put(key2, "");
        map.put(key3, "");
        records.add(map);
        
        map = new HashMap();
        map.put(key1, "=-=-=-=-=-=-");
        map.put(key2, "alFDeMvR1");
        map.put(key3, "Saturday");
        records.add(map);
        
        return new CSVFile(file, legend, records);
    }
}
