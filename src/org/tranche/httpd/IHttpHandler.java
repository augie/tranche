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

/**
 *Interface for a class implementing a handler for an HTTP request. e.g. GET
 * @author Takis Papoulias 
 */
public interface IHttpHandler {

    /**
     * The details of handling a HTTP request are implemented here.
     * @throws java.io.IOException
     */
    public void handle() throws IOException;

    /**
     * Setter method for the output stream to the browser.
     * @param os
     */
    public void setOut(PrintStream os);

    /**
     * Setter method for the input stream from the browser.
     * @param br
     */
    public void setIn(BufferedReader br);

    /**
     * The param value pair that was passed by the browser.
     * @param paramValMap
     */
    public void setHeaderPV(HashMap<String, String> paramValMap);

    /**
     * Setter method for the request code from the browser.
     * @param rc
     */
    public void setRequestCode(String rc);

    /**
     * Setter method for the name of the request.
     * @param rn
     */
    public void setRequestName(String rn);

    /**
     * Setter method for the version of HTTP used by the browser.
     * @param ver
     */
    public void setHttpVersion(String ver);
}
