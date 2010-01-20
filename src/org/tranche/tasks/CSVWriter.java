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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.tranche.util.IOUtil;
import org.tranche.util.Text;

/**
 * <p>Creates a CSV file from a CSVFile representation.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class CSVWriter {

    private CSVFile csvObj;
    private String NEWLINE;

    /**
     * @param   csvObj  the cvs object received
     * 
     */
    public CSVWriter(CSVFile csvObj) {
        this.csvObj = csvObj;
        this.NEWLINE = Text.getNewLine();
    }

    /**
     * <p>Executes the write.</p>
     * 
     * @throws  Exception   if any exception occurs
     * 
     */
    public void write() throws Exception {

        BufferedWriter out = null;

        try {
            out = new BufferedWriter(new FileWriter(csvObj.getFile()));

            // Write out the legend, first line
            List<String> legend = this.csvObj.getLegend();
            for (int i = 0; i < legend.size(); i++) {

                String entry = legend.get(i);

                // If contains comma, quote
                if (entry.contains(",")) {
                    entry = '"' + entry + '"';
                }

                if (i < legend.size() - 1) {
                    out.write(entry + ",");
                } else {
                    out.write(entry + NEWLINE);
                }
            }

            // Write out the entries, one per line
            List<Map<String, String>> records = this.csvObj.getRecords();

            for (Map<String, String> record : records) {

                if (record.size() != legend.size()) {
                    throw new IOException("Mismatch between size of legend and record, legend of size " + legend.size() + ", record of size " + record.size());
                }

                for (int i = 0; i < record.size(); i++) {

                    String entry = record.get(legend.get(i));

                    // If contains comma, quote
                    if (entry.contains(",")) {
                        entry = '"' + entry + '"';
                    }

                    if (i < record.size() - 1) {
                        out.write(entry + ",");
                    } else {
                        out.write(entry + NEWLINE);
                    }
                }
            }
        } finally {
            out.flush();
            IOUtil.safeClose(out);
        }
    }
}
