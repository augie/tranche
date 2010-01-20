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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author besmit
 */
public class PrettyPrintStreamTest extends TrancheTestCase {

    public void testPrettyPrintStream() {

        File tmp = null;

        // Swallow the results, just need to make sure works
        File temp = null;

        PrettyPrintStream p = null;

        try {

            tmp = TempFileUtil.createTemporaryDirectory();
            temp = TempFileUtil.createTemporaryFile();

            p = new PrettyPrintStream(new PrintStream(new FileOutputStream(temp)));

            int padding = 2;
            int maxCharactersPerLine = 20;

            p.setPadding(padding);
            assertEquals("Just set padding.", padding, p.getPadding());

            p.setMaxCharactersPerLine(maxCharactersPerLine);
            assertEquals("Just set max characters per line.", maxCharactersPerLine, p.getMaxCharactersPerLine());

            // Print a few things.
            final String helloWorld = "Hello, World!";
            final String goodbyeWorld = "Goodbye, Cruel World!";

            p.print(helloWorld);
            p.println();

            p.print(goodbyeWorld);
            p.println();

            p.println(helloWorld);
            p.println(goodbyeWorld);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            p.close();
            IOUtil.safeDelete(temp);
            IOUtil.recursiveDeleteWithWarning(tmp);
        }

    }
}
