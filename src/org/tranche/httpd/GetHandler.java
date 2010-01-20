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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.tranche.remote.RemoteUtil;

/**
 * Handler for HTTP-GET. It is only able to echo back the browser request.
 * @author Takis Papoulias 
 */
public class GetHandler extends AbstractHttpHandler {

    /**
     * Handles the HTTP request. Usually called by the SocketHandler to handle a GET request.
     * @throws java.io.IOException
     */
    public void handle() throws IOException {
        if (requestName.toLowerCase().equals("/echo")) {
            Http.print(os, HttpConstant.CONTENT_TYPE_TEXT_HTML, Html.createEchoPage(os, pvMap, httpVersion, requestName, requestCode));
            return;
        }
        byte[] content = get(requestName);
        if (content == null) {
            sendError(HttpConstant.ERROR_FILE_NOT_FOUND, "The following file could not be found: " + requestName);
            return;
        }
        Http.print(os, getContentType(requestName), content);
    }

    public static HttpConstant getContentType(String requestName) {
        String extension = ".html";
        if (requestName.contains("?")) {
            requestName = requestName.substring(0, requestName.indexOf("?"));
        }
        extension = requestName.substring(requestName.lastIndexOf(".")+1);
        if (extension.equals("jnlp")) {
            return HttpConstant.CONTENT_TYPE_JNLP;
        }
        return HttpConstant.CONTENT_TYPE_TEXT_HTML;
    }

    public static byte[] get(String requestName) throws IOException {
        InputStream requestedFileInputStream = null;
        String fileLocation = "/";
        if (!requestName.contains("?")) {
            fileLocation = IFileResources.HTTP_PAGE_DIRECTORY + requestName;
        } else {
            fileLocation = IFileResources.HTTP_PAGE_DIRECTORY + requestName.substring(0, requestName.indexOf("?"));
        }
        requestedFileInputStream = GetHandler.class.getResourceAsStream(fileLocation);
        if (requestedFileInputStream == null) {
            return null;
        }
        // read the variables
        Map<String, String> variables = new HashMap<String, String> ();
        if (requestName.contains("?")) {
            String[] variablesArray = requestName.substring(requestName.indexOf("?")+1).split("&");
            for (int i = 0; i < variablesArray.length; i++) {
                if (variablesArray[0].contains("=")) {
                    variables.put(variablesArray[0].substring(0, variablesArray[0].indexOf("=")).toLowerCase(), variablesArray[0].substring(variablesArray[0].indexOf("=")+1));
                } else {
                    variables.put(variablesArray[0].toLowerCase(), "");
                }
            }
        }
        // read the file
        String responseString = "";
        while (requestedFileInputStream.available() > 0) {
            String line = RemoteUtil.readLine(requestedFileInputStream);
            if (line != null) {
                responseString += line;
            }
        }
        // replace variables
        for (String name : variables.keySet()) {
            responseString = responseString.replace("${" + name + "}", variables.get(name));
        }
        return responseString.getBytes();
    }
}
