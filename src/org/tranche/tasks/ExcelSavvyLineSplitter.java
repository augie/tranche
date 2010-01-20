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

import java.util.ArrayList;

/**
 * <p>The all too commonly used line splitting logic for handling data exported from Excel or OpenOffice in .CSV format.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ExcelSavvyLineSplitter {

    /**
     * <p>Splits a line from a .CSV file exported from Excel into the expect original text.</p>
     * <p>+1 practical</p>
     * @param str
     * @return
     */
    public static String[] split(String str) {
        // split the line in excel friendly fashion
        ArrayList<String> itemBuffer = new ArrayList();
        for (int i = 0; i < str.length(); i++) {
            StringBuffer buf = new StringBuffer();
            // flag for if the content is quoted
            boolean quoted = false;

            // If starts with space, ignore
            if (str.charAt(i) == ' ') {
                i++;
            }

            // if the entry starts with a quote. handle it
            if (i < str.length() && str.charAt(i) == '"') {
                quoted = true;
                i++;
            }
            // parse up to a comma
            while (i < str.length() && (str.charAt(i) != ',' || quoted)) {
                // if quoted, check for the end quote
                if (str.charAt(i) == '"') {
                    if (i + 1 < str.length() && str.charAt(i + 1) == '"') {
                        buf.append('"');
                        i++;
                        i++;
                        continue;
                    }
                    // if quoted, break
                    if (quoted) {
                        quoted = false;
                        i++;
                        break;
                    }
                }
                buf.append(str.charAt(i));
                i++;
            }
            itemBuffer.add(buf.toString());

            // If the last item is a comma, add a blank entry to buffer
            if (i == str.length() - 1 && str.charAt(i) == ',') {
                itemBuffer.add("");
            }
        }
        return itemBuffer.toArray(new String[0]);
    }
}
