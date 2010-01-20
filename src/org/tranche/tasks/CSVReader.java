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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;

/**
 * <p>Used to read in a CSV file.</p>
 * 
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class CSVReader {

    private File file;
    private List<String> legend;
    private List<Map<String, String>> records;

    /**
     * @param   file    the file received
     *
     */
    public CSVReader(File file) {
        this.file = file;
        this.legend = new ArrayList();
        this.records = new ArrayList();
    }

    /**
     * <p>Executes the read.</p>
     * 
     * @return              The new file
     * @throws  Exception   if any exception occurs
     *
     */
    public CSVFile read() throws Exception {

        BufferedReader in = null;

        // Just to make more useful error messages
        int line = 1;

        try {

            in = new BufferedReader(new FileReader(this.file));
            String str;

            // If not a legend, we'll need to reset the stream
            boolean isLegend = true;

            // Get legend
            while ((str = in.readLine()) != null) {

                // Skip blank entries
                if (!str.trim().equals("")) {

                    String[] keys = this.getEntries(str);

                    for (String key : keys) {
                        this.legend.add(key.trim());

                        // Best-effort attempt to determine whether there is a legend
                        try {
                            // If there is a hash in this line, no legend
                            BigHash.createHashFromString(key);

                            // If get here, there is no legend
                            isLegend = false;
                            break;
                        } catch (Exception e) { /* Nope, good, probably a legend */ }
                    }

                    // Done after first non-empty row
                    line++;
                    break;
                }

                line++;
            }

            // If no legend, need to create one
            if (!isLegend) {

                // Could use reset with mark, but line may be long...
                IOUtil.safeClose(in);
                in = new BufferedReader(new FileReader(this.file));

                this.legend.clear();

                // First entry still in memory... how many items are in it?
                int numEntries = getEntries(str).length;

                for (int i = 1; i <= numEntries; i++) {
                    this.legend.add("Column #" + i);
                }
            }

            // Get remaining rows
            while ((str = in.readLine()) != null) {

                // Skip blank entries
                if (!str.trim().equals("")) {

                    String[] items = this.getEntries(str);

                    if (items.length != this.legend.size()) {
                        throw new IOException("Wrong number of entries on line " + line + ": expecting " + this.legend.size() + ", found " + items.length);
                    }

                    // Create a map representation
                    Map<String, String> record = new HashMap();

                    for (int i = 0; i < items.length; i++) {
                        String key = this.legend.get(i);
                        String value = items[i].trim();

                        record.put(key, value);
                    }

                    this.records.add(record);
                }

                line++;
            }

        } finally {
            IOUtil.safeClose(in);
        }

        return new CSVFile(this.file, this.legend, this.records);
    }

    /**
     * <p>Breaks up a line into entries based on commas. However, commas appearing with quotes aren't used to tokenize.</p>
     * 
     * @param   the entry to split by
     * @return  the entries that constitiute the line
     *
     */
    private String[] getEntries(String entry) {
        return ExcelSavvyLineSplitter.split(entry);
    }
}
