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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import org.tranche.time.TimeUtil;

/**
 * Utility methods for interacting with the operating system.
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class OperatingSystemUtil {

    private static boolean debug = false;

    /**
     * <p>Run an external app using Runtime. Should work for Windows XP and other OS.</p>
     * <p>Note that you should just type the command as if at a prompt. Do not include "cmd \c " prefix for Windows.
     * @param command Just the application name and paramters.
     * @return The exit status code.
     * @throws java.lang.Exception
     */
    public static int executeExternalCommand(String command) throws Exception {
        // First, see whether if Windows.
        OperatingSystem thisOS = OperatingSystem.getCurrentOS();

        boolean isModernWin =
                thisOS.equals(OperatingSystem.WINDOWS_XP) ||
                thisOS.equals(OperatingSystem.WINDOWS_2000) ||
                thisOS.equals(OperatingSystem.WINDOWS_NT);

        boolean isOldWin =
                thisOS.equals(OperatingSystem.WINDOWS_95) ||
                thisOS.equals(OperatingSystem.WINDOWS_98);


        if (isModernWin) {
            command = "cmd.exe /c " + command;
        } else if (isOldWin) {
            command = "command.com /c " + command;
        }

        debugOut(command);

        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(command);

        CommandStreamHandler errHandler = new CommandStreamHandler(proc.getErrorStream(), StreamType.STD_ERR);
        CommandStreamHandler outHandler = new CommandStreamHandler(proc.getInputStream(), StreamType.STD_OUT);

        errHandler.start();
        outHandler.start();

        int exitCode = proc.waitFor();
        debugOut("Process running \"" + command + "\" exited with value of " + exitCode + " at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));

        return exitCode;
    }

    /**
     * <p>Run an external app using Runtime in Unix, and redirect all output.</p>
     * @param command Just the application name and paramters.
     * @param outputFile The files for all output: standard error and output. All output is appended.
     * @return The exit status code.
     * @throws java.lang.Exception
     */
    public static int executeUnixCommandWithOutputRedirect(String command, File outputFile) throws Exception {
        // First, see whether if Windows.
        OperatingSystem thisOS = OperatingSystem.getCurrentOS();

        if (thisOS.isMSWindows()) {
            throw new Exception("This command does not support MS Windows. Use OperatingSystemUtil.executeExternalCommand");
        }
        String[] commandArray = {
            "/bin/sh",
            "-c",
            command + " >> " + outputFile.getAbsolutePath() + " 2>> " + outputFile.getAbsolutePath()
        };

        debugOut(command);

        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(commandArray);


        CommandStreamHandler errHandler = new CommandStreamHandler(proc.getErrorStream(), StreamType.STD_ERR);
        CommandStreamHandler outHandler = new CommandStreamHandler(proc.getInputStream(), StreamType.STD_OUT);

        errHandler.start();
        outHandler.start();

        int exitCode = proc.waitFor();
        debugOut("Process running \"" + command + "\" exited with value of " + exitCode + ".");

        return exitCode;
    }

    /**
     * <p>Handles streams from command dropped to Operating System. If streams are not read, then Java app may freeze!</p>
     * <p>Use stream type to either redirect to System.out, System.err or throw away ("bit bucket").</p>
     */
    private static class CommandStreamHandler extends Thread {

        private final StreamType type;
        private final InputStream in;

        private CommandStreamHandler(InputStream in, StreamType type) {
            this.in = in;
            this.type = type;
        }

        @Override()
        public void run() {
            BufferedReader r = new BufferedReader(new InputStreamReader(this.in));
            try {
                String nextLine;
                while ((nextLine = r.readLine()) != null) {
                    if (this.type.equals(StreamType.STD_ERR)) {
                        System.err.println(nextLine);
                    } else if (this.type.equals(StreamType.STD_OUT)) {
                        System.out.println(nextLine);
                    } else if (this.type.equals(StreamType.BIT_BUCKET)) {
                        // Do nothing, the String led a full life
                    }
                }
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } finally {
                try {
                    // Attempt to close resource? Avoids non-response application if fails?
                    r.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /**
     * <p>Used to redirect output from process to print stream or bit bucket.</p>
     */
    static class StreamType {

        public static final StreamType STD_ERR = new StreamType((byte) 0);
        public static final StreamType STD_OUT = new StreamType((byte) 1);
        public static final StreamType BIT_BUCKET = new StreamType((byte) 2);
        private byte type;

        private StreamType(byte type) {
            this.type = type;
        }

        @Override()
        public boolean equals(Object o) {
            if (o instanceof StreamType) {
                StreamType s = (StreamType) o;
                return (s.type == this.type);
            }

            return o.equals(this);
        }
    }

    /**
     * <p>Safely escape file so that name doesn't have illegal characters for any operating system, but using Windows illegal character list to escape characters.</p>
     * @param file
     * @return
     */
    public static File escapeFileName(File file) {
        String filename = file.getName();
        File parent = file.getParentFile();

        filename = filename.replaceAll(Pattern.quote("<"), "_");
        filename = filename.replaceAll(Pattern.quote(">"), "_");
        filename = filename.replaceAll(Pattern.quote(":"), "_");
        filename = filename.replaceAll(Pattern.quote("\""), "_");
        filename = filename.replaceAll(Pattern.quote("/"), "_");
        filename = filename.replaceAll(Pattern.quote("\\"), "_");
        filename = filename.replaceAll(Pattern.quote("|"), "_");
        filename = filename.replaceAll(Pattern.quote("?"), "_");
        filename = filename.replaceAll(Pattern.quote("*"), "_");

        File f = new File(parent, filename);
        return f;
    }

    /**
     * <p>Safely escape file so that name doesn't have illegal characters for Windows.</p>
     * @param file
     * @return
     */
    public static File escapeFileNameForWindows(File file) {
        if (OperatingSystem.getCurrentOS().isMSWindows()) {
            return escapeFileName(file);
        }

        return file;
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        OperatingSystemUtil.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static final boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static final void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(OperatingSystemUtil.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
