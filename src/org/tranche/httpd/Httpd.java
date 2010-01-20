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

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ServerSocketFactory;

/**
 *The httpd thread configures the server socket and the queue and waits for connections. Upon reception it
 * adds the connecting socket to a queue where it will be serviced by and AbstractHttpHandler
 *object.
 * @author TPapoulias
 */
public class Httpd extends Thread implements IVerbosity {

    /*The one and only HTTP port*/
    int port = 8080;
    /*Size of requests the server socket should manage.*/
    int backLog = 0;
    /*Flag indicating if this thread is running. Calling objects can set this flag to false to stop this thread.*/
    boolean isRunning = false;
    /*The server socket waiting for connections*/
    ServerSocket serverSocket = null;
    /*The queue where incoming socket requests will be placed.*/
    Queue queue = null;
    /*The timeout on a socket*/
    int timeout = 0;

    /**
     * Constructor
     * @param port      The port to setup the server socket
     * @param backLog   The size of connection to be managed by the server socket itself
     * @param queueLength   The max size of the Queue
     * @param minNumThreads     The min number of SocketHandling threads active
     * @param maxNumThreads     The max number of SocketHandling threads active
     */
    public Httpd(int port,
            int backLog,
            int queueLength,
            int minNumThreads,
            int maxNumThreads) {

        this.port = port;
        this.backLog = backLog;
        queue = new Queue(queueLength, minNumThreads, maxNumThreads);
    }

    /**
     * If this object is destroyed shutdowns and connections that are still open.
     */
    public void finilize() {
        if (isRunning) {
            queue.shutdown();
        }
    }

    /**
     * Setter for the timeout of any open connections
     * @param timeout
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     *All the work for starting up the server is done here. A server socket is created
     * that listens to a port and backlog.
     * @return
     */
    public boolean startServer() {
        try {
            ServerSocketFactory ssf = ServerSocketFactory.getDefault();
            serverSocket = ssf.createServerSocket(port, backLog);
            start();
        } catch (IOException ex) {
            Logger.getLogger(Httpd.class.getName()).log(Level.SEVERE, null, ex);
            Formatter.printToErrLn(this.getClass(), "startServer", "Could not open port ", "" + port);
            return false;
        }
        return true;
    }

    /**
     * Implementation of the run method of the Thread. As long as the isRunning flag is true
     * the server will wait for a connection and add it to the queue of connections for further
     * handling.
     */
    @Override
    public void run() {
        if (DEBUG) {
            Formatter.printToOutLn("Started, listening on port: " + port);
        }

        isRunning = true;

        while (isRunning) {
            Socket s = null;

            try {
                s = serverSocket.accept();
                s.setSoTimeout(timeout);

                if (DEBUG) {
                    InetAddress addr = s.getInetAddress();
                    Formatter.printToOutLn("Received a new connection from (" + addr.getHostAddress() + "): " + addr.getHostName());
                }

                this.queue.add(s);
            } catch (QueueException ex) {
                Logger.getLogger(Httpd.class.getName()).log(Level.SEVERE, null, ex);

                if (s == null)//some paranoia may be good
                {
                    continue;
                }

                PrintStream os = null;

                try {
                    os = new PrintStream(s.getOutputStream());
                    Http.printError(os, HttpConstant.STATUS_REQUEST_TIMEOUT, "The service is busy.");
                } catch (IOException ex1) {
                    Logger.getLogger(Httpd.class.getName()).log(Level.SEVERE, null, ex1);
                } finally {
                    int qSize = queue.queue.size();
                    int maxQLength = queue.maxQLength;

                    if (qSize > maxQLength) {
                        Formatter.printToErrLn(Httpd.class, "run", "Unable To Add Socket Into Queue ", "Q Full");
                    } else {
                        Formatter.printToErrLn(Httpd.class, "run", "Unable To Add Socket Into Queue ", "Unknown Reason");
                    }
                    os.flush();
                    os.close();
                    try {
                        s.close();
                    } catch (IOException ex1) {
                        Logger.getLogger(Httpd.class.getName()).log(Level.SEVERE, null, ex1);
                    } finally {
                        os = null;
                        s = null;
                    }
                }
            } catch (SocketException se) {
                if (isRunning) {
                    se.printStackTrace();
                }
            } catch (IOException ex) {
                Logger.getLogger(Httpd.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (DEBUG) {
            Formatter.printToOutLn("Shutting Http Sever Down...");
        }

        queue.shutdown();
    }

    /**
     * Method for stopping the server. Sets the isRunning flag to false and closes the connections.
     */
    public void stopServer() {
        isRunning = false;
        try {
            serverSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(Httpd.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Getter method for the port attribute.
     * @return  the port number
     */
    public int getPort() {
        return port;
    }
}
