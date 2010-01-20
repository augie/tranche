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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *This class services the next socket from the queue. From the HTTP request it looksup
 * the appropriate AbstractHttpHandler. It then initializes the state of the handler. Currently
 * on HTTP-GET is supported.
 * @author TPapoulias
 */
public class SocketHandler extends Thread implements IVerbosity, IFileResources {
    /*input stream from browser */

    BufferedReader is;
    /*output stream to browser*/
    PrintStream os;
    /* The client socket serviced by this object*/
    Socket clientSocket = null;
    /*The thread number assigned by the calling object to this object*/
    int threadNumber = Integer.MIN_VALUE;
    /*Flag for controlling the main loop of this thread. To terminate the thread a calling object
    should set this flag to false. When a thread is in its run state the flag is set to true.*/
    boolean isRunning = false;
    /*Flag to capture if handle() method has finished. If true handle() has not exited. If false
    handle() has exited.*/
    boolean isWorking = false;
    /*Reference to the queue  to look for sockets to service. */
    Queue queue = null;
    /*A map of the HTTP requests for this handler. Currently only HTTP-GET is implemented.*/
    HashMap<String, AbstractHttpHandler> methodMap = new HashMap<String, AbstractHttpHandler>();
    /*The handler responsible for the HTTP-GET request*/
    AbstractHttpHandler getHandler = new GetHandler();

    /**
     * Constructor
     * @param queue     Reference to the queue  to look for sockets to service
     * @param threadNumber      The thread number assigned by the calling object to this object
     */
    public SocketHandler(Queue queue, int threadNumber) {
        this.queue = queue;
        this.threadNumber = threadNumber;
        setName("HTTP Hanlder Thread " + threadNumber);

        methodMap.put(HttpConstant.METHOD_GET.getName(), getHandler);
    }

    /**
     *Drops connections if this object is about to be deleted.
     */
    public void finilize() {
        dropConnection();
    }

    /**
     * The main loop of this thread is acomplished.  The next socket from the queue
     * is retrieved and a call to  handle() is made to accomplish the request.
     */
    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
            try {
                clientSocket = queue.getNextSocket();
                if (isRunning) {
                    isWorking = true;
                    handle();
                    isWorking = false;
                }
            } catch (SocketTimeoutException ex) {
                Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
                try {
                    sendError(HttpConstant.STATUS_REQUEST_TIMEOUT, "" + clientSocket.getSoTimeout() + " ms");
                    Formatter.printToErrLn(SocketHandler.class, "run", "Request Time Out Occured", "" + clientSocket.getSoTimeout() + " ms");
                    dropConnection();
                } catch (IOException ex1) {
                    Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex1);
                    Formatter.printToErrLn(SocketHandler.class, "run", "Error Sending Request Timed Out Page", "");
                }
            } catch (IOException ex) {
                Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {

                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if (DEBUG) {
            Formatter.printToOutLn("Thread[" + threadNumber + "] is shutting down");
        }
    }

    /**
     *  Getter method for isWorking attribute. Calling objects can assess if this thread is
     * actively handling a socket request.
     * @return
     */
    public boolean isWorking() {
        return isWorking;
    }

    /**
     * Sets the isRunning variable to false. Used by calling objects to kill this thread.
     */
    public void killThread() {
        if (DEBUG) {
            Formatter.printToOutLn("Shutting Down Handler " + getName());
        }
        isRunning = false;
    }

    /**
     * Cleans up any streams to the sockets and sets the working state to false.
     */
    public void dropConnection() {
        if (os != null) {
            os.flush();
            os.close();
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException ex) {
                Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException ex) {
                Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        is = null;
        os = null;
        clientSocket = null;
        isWorking = false;
    }
    /*
     * Handles the socket request. The typical structure of a HTTP request is...
    generic-message = start-line
     *(message-header CRLF)
    CRLF
    [ message-body ]
    start-line      = Request-Line | Status-Line
     *
     * This method parses the above request to determine the request name and then looks
     * up the appropriate AbstractHttpHandler for this named request. Initializes the appropriate
     * AbstractHttpHandler object state.
     */

    private void handle() throws IOException {
        String request = "";

        is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        os = new PrintStream(clientSocket.getOutputStream());

        if (ECHO) {
            is.mark(READ_AHEAD);
        }
        request = is.readLine();

        if (request == null || request.length() == 0) {
            if (DEBUG) {
                Formatter.printToOutLn("Socket Died.");
            }
            return;
        }

        //request into HTTP method, resource name, HTTP version       
        StringTokenizer st = new StringTokenizer(request);
        if (st.countTokens() != 3) {
            sendError(HttpConstant.ERROR_REQUEST_NOT_UNDERSTOOD, request);
            dropConnection();
            return;
        }

        String requestCode = st.nextToken();
        String requestName = st.nextToken();
        String httpVersion = st.nextToken();

        //Read headers up to the CRLF line where the body begins
        HashMap<String, String> map = new HashMap<String, String>();
        String headerLine = null;

        while ((headerLine = is.readLine()) != null &&
                headerLine.length() != 0) {
            headerLine = headerLine.trim();
            int index = headerLine.indexOf(":");
            if (index == -1) {
                Formatter.printToErrLn(this.getClass(), "handle", "Invalid Header", headerLine);
                continue;
            }
            String s[] = new String[2];
            s[0] = headerLine.substring(0, index);
            s[1] = headerLine.substring(index + 1);
            map.put(s[0].trim(), s[1].trim());
        }

        //requestCode is either a GET or a HEAD or POST
        AbstractHttpHandler httpHandler = null;

        httpHandler = methodMap.get(requestCode.toUpperCase());

        if (httpHandler == null) {//most likely no handlers for this request.... But possible an invalid request
            sendError(HttpConstant.SERVICE_UNAVAILABLE, requestCode);
            return;
        }

        //update the current method handler
        httpHandler.setIn(is);
        httpHandler.setOut(os);
        httpHandler.setHeaderPV(map);
        httpHandler.setRequestCode(requestCode);
        httpHandler.setRequestName(requestName);
        httpHandler.setHttpVersion(httpVersion);

        httpHandler.handle();

        if (ECHO) {
            is.reset();
            printStream(is);
        }

        dropConnection();
    }

    /**
     * Helper method for reporting any errors. It reports errors to the browser and stderr.
     * @param errorNumber       The HTTP error number.
     * @param errorDetails          A description of the error
     * @throws java.io.IOException
     */
    protected void sendError(HttpConstant errorNumber, String errorDetails) throws IOException {
        Http.printError(os, errorNumber, errorDetails);
    }

    /**
     * Helper method that writes the contents of a stream to the stdout.
     * @param is    The stream from which errors to be written out are found.
     * @throws java.io.IOException
     */
    private void printStream(BufferedReader is) throws IOException {
        String line = null;
        while ((line = is.readLine()) != null) {
            System.out.println(line);
        }
    }
}
