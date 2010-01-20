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
package org.tranche.util;

import java.io.PrintStream;

/**
 * <p>General utility for handling output and errors.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class DebugUtil {

    private static boolean debug = false;
    private static PrintStream err = System.err,  out = System.out;

    /**
     * <p>This class cannot be instantiated.</p>
     */
    private DebugUtil() {
    }

    /**
     * <p>Sets the PrintStream for writing error information.</p>
     * @param err The PrintStream to which all error information should be written.
     */
    public synchronized static final void setErr(PrintStream err) {
        if (err == null) {
            throw new RuntimeException("PrintStream cannot be null.");
        }
        DebugUtil.err = err;
    }

    /**
     * <p>Gets the PrintStream for writing error information.</p>
     * @return The PrintStream to which all error information is written.
     */
    public synchronized static final PrintStream getErr() {
        return err;
    }

    /**
     * <p>Sets the PrintStream for writing output information.</p>
     * @param out The PrintStream to which all output information should be written.
     */
    public synchronized static final void setOut(PrintStream out) {
        if (out == null) {
            throw new RuntimeException("PrintStream cannot be null.");
        }
        DebugUtil.out = out;
    }

    /**
     * <p>Gets the PrintStream for writing output information.</p>
     * @return The PrintStream to which all output information is written.
     */
    public synchronized static final PrintStream getOut() {
        return out;
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public synchronized static final void setDebug(boolean debug) {
        DebugUtil.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public synchronized static final boolean isDebug() {
        return debug;
    }

    /**
     * <p>If isDebug(), writes the given string to getOut().</p>
     * @param line String to write to getOut()
     */
    public synchronized static final void printOut(String line) {
        if (debug) {
            out.println(line);
        }
    }

    /**
     * <p>If isDebug(), writes the given string to getErr().</p>
     * @param line String to write to getErr()
     */
    public synchronized static final void printErr(String line) {
        if (debug) {
            err.println(line);
        }
    }

    /**
     * <p>If isDebug(), prints the given exception's stack trace to getErr().</p>
     * @param e Exception to report
     */
    public synchronized static final void reportException(Exception e) {
        if (debug) {
            e.printStackTrace(err);
        }
    }
}
