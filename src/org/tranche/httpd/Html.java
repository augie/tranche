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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Helper methods for writing HTML formatted content to the browser.
 * @author TPapoulias
 */
public class Html {

    /**
     * Helper method for creating an HTML formatted page whose content is the content of the
     * browser request.
     * @param os    Browser output stream
     * @param pvMap     A pair value map
     * @param httpVersion   The http version
     * @param requestName   The file being requested
     * @param requestCode   The name of the request e.g. GET
     * @return  The content of the page
     */
    public synchronized static final byte[] createEchoPage(PrintStream os,
            HashMap<String, String> pvMap,
            String httpVersion,
            String requestName,
            String requestCode) {

        String s = "<HTML><HEAD><TITLE>Echo Client</TITLE></HEAD><body>";
        s = s + "<h5>Request Code: " + requestCode + "</h5>" +
                "<h5>Request Name: " + requestName + "</h5>" +
                "<h5>HTTP Version: " + httpVersion + "</H5>";

        for (Iterator i = pvMap.keySet().iterator(); i.hasNext();) {
            String param = (String) i.next();
            String value = pvMap.get(param);
            s += "<H5>" + param + ":" + value + "</H5>";
        }
        s += "</body></HTML>";
        return s.getBytes();
    }
}
