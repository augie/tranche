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
package org.tranche.httpd;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *This class defines the Queue in which sockets are placed temporarily by httpd. AbstractHttpHandlers
 * look here for the next socket to handle. These handlers are cratead and their threads started up
 * during construction of the class and placed in a threadPool.
 * @author TPapoulias
 */
public class Queue {

    /**
     * The queue where sockets are placed before handled. It works like a FIFO queue.
     */
    LinkedList<Socket> queue = new LinkedList<Socket>();

    /*The maximum length of the Queue.*/
    int maxQLength = 0;
    /*The minimum number of handling threads available.*/
    int minNumThreads = 0;
    /*The max number of threads available.*/
    int maxNumThreads = 0;
    /*The current number of threads active. Should be a number bounded by minNumThreads and maxNumThreads.*/
    int currentNumThreads = 0;
    /*A flag for declaring if this Queue is active. If inactive the flag is set to false and
    calling threads will receive a null when requesting the next socket to service.*/
    boolean isRunning = true;
    /*Thread pool where SocketHandler are placed.*/
    List<SocketHandler> threadPool = new ArrayList<SocketHandler>();

    /**
     * Constructor
     * @param maxQLength        The size of the queue
     * @param minNumThreads     The minimum number of SocketHandler threads active.
     * @param maxNumThreads     The maximum number of SocketHandler threads.
     */
    public Queue(int maxQLength, int minNumThreads, int maxNumThreads) {
        this.maxQLength = maxQLength;
        this.minNumThreads = minNumThreads;
        this.maxNumThreads = maxNumThreads;

        for (int i = 0; i < minNumThreads; i++) {
            SocketHandler h = new SocketHandler(this, i);
            h.start();
            threadPool.add(h);
        }
    }

    /**
     * The proper way of adding sockets to the queue. If the size of the queue is exited
     * the socket is not added and an QueueException thrown. Once the socket is added to
     * the bottom of the queue the method adds more SocketHandler threads.
     * @param socket       The socket to be added to the queue.
     * @throws org.tranche.httpd.QueueException
     */
    public synchronized void add(Socket socket) throws QueueException {

        if (queue.size() > maxQLength) {
            throw new QueueException("Queue is Full. Max size = " + maxQLength);
        }

        queue.addLast(socket);

        boolean availableThread = false;

        for (Iterator i = threadPool.iterator(); i.hasNext();) {
            SocketHandler h = (SocketHandler) i.next();
            if (!h.isWorking()) {
                availableThread = true;
                break;
            }
        }

        if (!availableThread) {
            if (currentNumThreads < maxNumThreads) {
                SocketHandler h = new SocketHandler(this, currentNumThreads++);
                h.start();
                threadPool.add(h);
            } else {
                System.err.println("[HTTPD.Queue.add()]Unable To Grow Thread Pool: Reason Pool Is Full");
            }
        }

        notifyAll();
    }

    /**
     * Method used for retrieving next item in the queue.
     * @return  The next socket in the queue awaiting service
     */
    public synchronized Socket getNextSocket() {
        while (queue.isEmpty()) {
            try {
                if (!isRunning) {
                    return null;
                }
                wait();
            } catch (InterruptedException ie) {
            }
        }
        return queue.removeFirst();
    }

    /**
     * Methods for shutting down the queue. It first sets the isRunning flag to false. Calls the
     * killThread() method of every thread in the threadpool.
     *
     */
    public synchronized void shutdown() {
        isRunning = false;
        for (Iterator i = this.threadPool.iterator(); i.hasNext();) {
            SocketHandler rt = (SocketHandler) i.next();
            rt.killThread();
        }
        notifyAll();
    }
}
