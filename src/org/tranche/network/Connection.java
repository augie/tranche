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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.tranche.TrancheServer;
import org.tranche.remote.RemoteTrancheServer;

/**
 * <p>Container for objects related to a Tranche server connection.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class Connection {

    private final TrancheServer trancheServer;
    private final List<Exception> exceptions = new ArrayList<Exception>();
    private int locks = 0;
    private final Object locksLock = new Object();

    /**
     * 
     * @param trancheServer
     * @param locked
     */
    public Connection(TrancheServer trancheServer, boolean locked) {
        this.trancheServer = trancheServer;
        if (locked) {
            lock();
        }
    }

    /**
     *
     * @return
     */
    public TrancheServer getTrancheServer() {
        return trancheServer;
    }

    /**
     * 
     * @return
     */
    public RemoteTrancheServer getRemoteTrancheServer() {
        return (RemoteTrancheServer) trancheServer;
    }

    /**
     *
     * @return
     */
    public List<Exception> getExceptions() {
        synchronized (exceptions) {
            return Collections.unmodifiableList(exceptions);
        }
    }

    /**
     * 
     * @param index
     * @return
     */
    public Exception getException(int index) {
        synchronized (exceptions) {
            return exceptions.get(index);
        }
    }

    /**
     * 
     * @param e
     * @return
     */
    public boolean reportException(Exception e) {
        synchronized (exceptions) {
            // no duplicates
            if (exceptions.contains(e)) {
                return false;
            }
            return exceptions.add(e);
        }
    }

    /**
     * 
     * @return
     */
    public int getExceptionCount() {
        synchronized (exceptions) {
            return exceptions.size();
        }
    }

    /**
     * 
     */
    public void clearExceptions() {
        synchronized (exceptions) {
            exceptions.clear();
        }
    }

    /**
     *
     * @return
     */
    public boolean isLocked() {
        synchronized (locksLock) {
            return locks > 0;
        }
    }

    /**
     * 
     * @return
     */
    public int getLockCount() {
        synchronized (locksLock) {
            return locks;
        }
    }

    /**
     * 
     */
    public void lock() {
        synchronized (locksLock) {
            locks++;
        }
    }

    /**
     * 
     */
    public void unlock() {
        synchronized (locksLock) {
            locks--;
        }
    }
}
