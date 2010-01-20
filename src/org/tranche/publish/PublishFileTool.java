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
package org.tranche.publish;

import org.tranche.exceptions.TodoException;
import org.tranche.util.DebugUtil;
import org.tranche.util.ThreadUtil;

/**
 * <p>This tool is used to publish a passphrase for an encrypted upload.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class PublishFileTool {

    private static boolean debug = false;
    /**
     * Default parameters
     */
    public static int DEFAULT_THREADS = 5;
    /**
     * Startup runtime parameters
     */
    private static final boolean START_VALUE_PAUSED = false;
    private static final boolean START_VALUE_STOPPED = false;
    /**
     * Runtime parameters
     */
    private boolean paused = START_VALUE_PAUSED,  stopped = START_VALUE_STOPPED;
    /**
     * Inernal variables
     */
    private int threadCount = DEFAULT_THREADS;
    private boolean locked = false;

    /**
     *
     * @return
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     *
     */
    public void throwExceptionIfLocked() {
        if (locked) {
            throw new RuntimeException("The variables for this tool are currently locked.");
        }
    }

    /**
     * <p>Set whether the upload is paused.</p>
     * @param paused Whether the upload is paused.
     */
    public void setPause(boolean paused) {
        this.paused = paused;
    }

    /**
     * <p>Gets whether the upload is paused.</p>
     * @return Whether the upload is paused.
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * <p>Will sleep the current thread until the upload is no longer paused.</p>
     */
    private void waitHereOnPause() {
        while (paused) {
            ThreadUtil.safeSleep(1000);
        }
    }

    /**
     * <p>Stops the upload.</p>
     */
    public void stop() {
        this.stopped = true;
    }

    /**
     *
     * @return
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     *
     * @return
     */
    public PublishFileToolReport execute() {
        if (true) {
            throw new TodoException();
        }
        return null;
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static void setDebug(boolean debug) {
        PublishFileTool.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    protected static void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(PublishFileTool.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    protected static void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
