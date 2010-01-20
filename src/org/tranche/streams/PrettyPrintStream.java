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
package org.tranche.streams;

import java.io.PrintStream;
import java.io.StringWriter;

/**
 * <p>Outputs text in a formatted way. Settings are maximum number of characters per line and left padding.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class PrettyPrintStream extends PrintStream {
    
    private StringWriter buffer = new StringWriter();
    private int maxCharactersPerLine = 80;
    private int padding = 0;

    /**
     * 
     * @param out
     */
    public PrettyPrintStream(PrintStream out) {
        super(out);
    }
    
    /**
     * <p>Sets the number of spaces to indent the left side of the text.</p>
     * @param size
     */
    public void setPadding(int size) {
        this.padding = size;
    }

    /**
     * <p>Returns the number of spaces to indent the left side of the text.</p>
     * @return
     */
    public int getPadding() {
        return padding;
    }

    /**
     * <p>Sets the maximum number of characters to be shown per line.</p>
     * @param maxCharactersPerLine
     */
    public void setMaxCharactersPerLine(int maxCharactersPerLine) {
        this.maxCharactersPerLine = maxCharactersPerLine;
    }

    /**
     * <p>Returns the setting of the maximum number of characters per line.</p>
     * @return
     */
    public int getMaxCharactersPerLine() {
        return maxCharactersPerLine;
    }

    /**
     * <p>Prints a line.</p>
     * @param string
     */
    public void println(String string) {
        // buffer the text
        buffer.write(string);
        // write out the line keeping limits in mind.
        // split in to words
        String[] split = buffer.toString().split(" ");
        // clear the buffer
        buffer = new StringWriter();
        int count = 0;
        boolean isFirstLine = true;
        for (int i = 0; i < split.length; i++) {
            // check if the word fits
            if (count + split[i].length() < maxCharactersPerLine || (count == 0 && split[i].length() >= maxCharactersPerLine)) {
                // pad if needed
                if (!isFirstLine && count == 0) {
                    for (int j = 0; j < padding; j++) {
                        super.print(" ");
                        count++;
                    }
                }
                // inc characters printed count
                count += split[i].length();
                // print the character
                super.print(split[i]);
                // write the newline
                if (count < maxCharactersPerLine) {
                    super.print(" ");
                    count++;
                } else {
                    super.println();
                    count = 0;
                    isFirstLine = false;
                }
                continue;
            }
            // toggle off first line
            isFirstLine = false;
            // break the line
            super.println();
            // else inc back
            i--;
            count = 0;
        }
        super.println();
    }

    /**
     * <p>Prints a string.</p>
     * @param string
     */
    public void print(String string) {
        // split by any inserted newlines
        String[] split = string.split("\\\n");
        for (int i = 0; i < split.length - 1; i++) {
            println(split[i]);
        }
        // write the last bit without assuming new-line
        buffer.write(split[split.length - 1]);
    }

    /**
     * <p>Prints a blank line.</p>
     */
    public void println() {
        println("");
    }
}
