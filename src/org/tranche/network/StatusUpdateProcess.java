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
package org.tranche.network;

/**
 * <p>A thread for updating the network status.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public abstract class StatusUpdateProcess extends Thread {

    private Exception caughtException;
    protected boolean isRunning = true;

    /**
     * <p>Cannot be instantiated by any class outside the network package.</p>
     */
    protected StatusUpdateProcess() {
    }

    /**
     * <p>Returns the exception that was thrown to end the status update process.</p>
     * @return The exception that was thrown to end the status update process.
     */
    public Exception getException() {
        return caughtException;
    }

    /**
     * <p>Sets the exception that was thrown to end the status update process.</p>
     * @param caughtException The exception that was thrown to end the status update process.
     */
    protected void setException(Exception caughtException) {
        this.caughtException = caughtException;
    }

    /**
     * <p>Interrupts the thread, catching any exceptions that may be thrown.</p>
     */
    public void safeStop() {
        isRunning = false;
    }
}
