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
package org.tranche.hash;

import java.io.StringWriter;

/**
 * A helper class for working with Base64 encoded text. More prominently this 
 * class will clean up text copied and pasted from e-mails and text in to a 
 * respectable Tranche hash.
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public final class Base64Util {

    private static final String base64Characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    /**
     * Removes non-Base64 characters from an arbitrary string, including 
     * whitespace.
     * @param text
     * @return A string consisting of only Base64 characters.
     */
    public static final String cleanUpBase64(String text) {
        // if null, return
        if (text == null) {
            return "";
        }
        
        // the base64 characters
        char[] base64Chars = base64Characters.toCharArray();
        
        // make up a simple hash map to quickly analyze characters
        boolean[] hashMap = new boolean[255];
        for (char c : base64Chars) {
            hashMap[c] = true;
        }
        // buffer the final string
        StringWriter sw = new StringWriter();
        char[] textChars = text.toCharArray();
        for (char c : textChars) {
            // if a valid char, use it -- otherwise skip
            if (hashMap[c]) sw.write(c);
        }
        
        // return the final string
        return sw.toString();
    }
}
