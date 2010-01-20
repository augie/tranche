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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Abstracts a comma-separated value file. Use the reader to build.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class CSVFile {
    /**
     * Represents the CSV file. Not used by class, but can be used by reader/writer.
     */
    private File file;
    /**
     * Ordered list of header items
     */
    private List<String> legend;
    /**
     * Ordered list of records
     */
    private List<Map<String, String>> records;

    /**
     * @param   file        the file received 
     * @param   legend      the list of keys received 
     * @param   records     the records received
     * 
     */
    public CSVFile(File file, List<String> legend, List<Map<String, String>> records) {
        this.file = file;
        this.legend = legend;
        this.records = records;
    }

    /**
     * <p>Returnst the CSV file.</p>
     * 
     * @return  the file
     * 
     */
    public File getFile() {
        return file;
    }

    /**
     * <p>Returns the list of keys in order of appearance in the file.</p>
     * 
     * @return  the list of keys in order of appearance in the file
     * 
     */
    public List<String> getLegend() {
        return legend;
    }

    /**
     * <p>Returns a record found in a row. The rows are zero-indexed starting with the first valid record and incremented by subsequent valid records.</p>
     * 
     * @param   index   the index of the desired record
     * @return          the record found at the given index
     *
     */
    public Map<String, String> getRecord(int index) {
        return this.getRecords().get(index);
    }

    /**
     * <p>Remove the record with the given index.</p>
     * 
     * @param index
     * @return
     */
    public Map<String, String> removeRecord(int index) {
        return this.getRecords().remove(index);
    }

    /**
     * <p>Appends a record to CSVFile representation. To store, you must rewrite the file using the writer.</p>
     * 
     * @param   record  the record
     * @return          <code>true</code> if the record is appended;
     *                  <code>false</code> otherwise
     *
     */
    public boolean appendRecord(Map<String, String> record) {
        return this.records.add(record);
    }

    /**
     * <p>Returns an entry within a particular record using a key. The list of keys are available as the legend.</p>
     *
     * @param   index   the index of the record seeked
     * @param   key     the key used to retrieve an entry within the seeked record
     * @return          the entry with the given key within the seeked record
     *
     */
    public Object getEntry(int index, String key) {
        return this.getRecords().get(index).get(key);
    }

    /**
     * <p>Returns the records of the CSV file.</p>
     * 
     * @return  the list of records
     * 
     */
    public List<Map<String, String>> getRecords() {
        return records;
    }

    /**
     * <p>Returns a clone (deep copy). Useful for:</p>
     * <ul>
     *   <li>Testing</li>
     *   <li>Keeping copy of old CSV.</li>
     * </ul>
     * 
     * @return
     */
    public CSVFile clone() {
        List<String> legendClone = new ArrayList();
        List<Map<String, String>> recordsClone = new ArrayList();
        File fileClone = new File(this.file.getAbsolutePath());

        // Clone header
        for (String colHeader : this.getLegend()) {
            legendClone.add(new String(colHeader));        // Clone records
        }
        Map<String, String> recordClone;
        for (Map<String, String> record : this.getRecords()) {
            recordClone = new HashMap();
            for (String key : record.keySet()) {
                recordClone.put(new String(key), new String(record.get(key)));
            }

            recordsClone.add(recordClone);
        }

        return new CSVFile(fileClone, legendClone, recordsClone);
    }

    /**
     * <p>Prints the CSV file to the given output stream.</p>
     * 
     * @param   out     the output stream
     *
     */
    public void printContents(PrintStream out) {

        // Header
        out.print("H: ");
        for (String key : this.legend) {
            out.print(key + '\t');
        }
        out.println();

        // Body
        for (int i = 0; i < this.records.size(); i++) {

            Map<String, String> record = this.records.get(i);
            out.print(i + ": ");

            for (String key : this.legend) {
                out.print(record.get(key) + '\t');
            }
            out.println();
        }

        out.flush();
    }

    /**
     * <p>Returns whether this CSV File is the same as the given CSV File.</p>
     * 
     * @param o
     * @return
     */
    public boolean equals(Object o) {

        if (o instanceof CSVFile) {

            CSVFile obj = ((CSVFile) o);

            // First, test the numbers
            if (obj.getRecords().size() != this.getRecords().size()) {
                return false;
            }
            if (obj.getLegend().size() != this.getLegend().size()) {
                return false;
            }

            // Make sure same column headers
            for (int i = 0; i < obj.getLegend().size(); i++) {
                if (!obj.getLegend().get(i).equals(this.getLegend().get(i))) {
                    return false;
                }
            }

            // Make sure same entries
            for (int i = 0; i < obj.getRecords().size(); i++) {
                Map<String, String> objEntry = obj.getRecord(i);
                Map<String, String> entry = this.getRecord(i);

                // Each entry must be same size
                if (objEntry.size() != entry.size()) {
                    return false;
                }

                // Verify entry contents
                for (String key : objEntry.keySet()) {
                    if (!objEntry.get(key).equals(this.getEntry(i, key))) {
                        return false;
                    }
                }
            }

            return true;
        }

        return o.equals(this);
    }
}
