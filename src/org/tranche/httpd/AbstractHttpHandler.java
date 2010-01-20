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
import java.io.PrintStream;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Abstract class for handling HTTP requests.
 *All classes handling a specific HTTP requests e.g. GET, will derive from this class. 
 * @author Takis Papoulias 
 */
public abstract class AbstractHttpHandler implements IHttpHandler, IVerbosity, IFileResources {

    /*The input stream from the browser*/
    protected BufferedReader is = null;
    /*The output stream to the browser*/
    protected PrintStream os = null;
    /*A parameter value pair map of the browser request*/
    protected HashMap<String, String> pvMap = null;

    /*Http request code send by the browser*/
    protected String requestCode = "";
    /*Http request name send by the browser*/
    protected String requestName = "";
    /*Supported http version by the browser*/
    protected String httpVersion = "";

    /**
     * Helper method for sending an error message for the specified locale.
     * @param errorNumber
     * @param errorDetails
     * @throws java.io.IOException
     */
    protected void sendError(HttpConstant errorNumber, String errorDetails) throws IOException {
        Http.printError(os, errorNumber, errorDetails);
    }

    /**
     * 
     * @param contentType
     * @param content
     * @throws java.io.IOException
     */
    protected void send(HttpConstant contentType, byte[] content) throws IOException {
        Http.print(os, contentType, content);
    }

    /**
     * Setter method for the output stream to the browser.
     * @param os
     */
    public void setOut(PrintStream os) {
        this.os = os;
    }

    /**
     * Setter method for the input stream to the browser.
     * @param br
     */
    public void setIn(BufferedReader br) {
        this.is = br;
    }

    /**
     * Setter method for the param value pair send by the browser.
     * @param paramValMap       Map containing the param value pair
     */
    public void setHeaderPV(HashMap<String, String> paramValMap) {
        this.pvMap = paramValMap;
    }

    /**
     * The request code send by the browser.
     * @param rc    The request code
     */
    public void setRequestCode(String rc) {
        requestCode = rc;
    }

    /**
     * The request name send by the browser
     * @param rn    The request name
     */
    public void setRequestName(String rn) {
        requestName = rn;
    }

    /**
     * The browsers http version
     * @param ver   The http version
     */
    public void setHttpVersion(String ver) {
        httpVersion = ver;
    }
}
