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
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility method for writting the HTTP protocol.
 * @author TPapoulias
 */
public class Http implements IVerbosity, IFileResources {

    private static final ResourceBundle messages = ResourceBundle.getBundle(HTTP_RESPONSE_MESSAGES_PROP_FILE, Locale.US);

    public static synchronized final void print(PrintStream os, HttpConstant status, HttpConstant contentType, byte[] content) throws IOException {
        String message;
        try {
            message = messages.getString(status.getName());
        } catch (MissingResourceException ex) {
            Logger.getLogger(Http.class.getName()).log(Level.SEVERE, null, ex);
            Formatter.printToErrLn(Http.class, "printSimpleHeader", "Missing Resource File For Http Error Messages", "HttpResponseMessage");
            return;
        }
        // Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
        String s = Formatter.createStatusLine(status, message);
        os.println(s);
        s = Formatter.createPvFormat(HttpConstant.CONTENT_TYPE, contentType);
        os.println(s);
        s = Formatter.createPvFormat(HttpConstant.CONTENT_LENGTH, content.length);
        os.println(s);
        os.println("");
        //[message-body]
        os.write(content);
    }

    public static synchronized final void print(PrintStream os, HttpConstant contentType, byte[] content) throws IOException {
        print(os, HttpConstant.STATUS_OK, contentType, content);
    }

    /**
     * Method for reporting to the browser an error
     * @param os
     * @param errorNumber
     * @param errorDetails
     * @throws java.io.IOException
     */
    public static synchronized final void printError(PrintStream os, HttpConstant errorNumber, String errorDetails) throws IOException {
        String title;
        try {
            title = messages.getString(errorNumber.getName());
        } catch (MissingResourceException ex) {
            Logger.getLogger(Http.class.getName()).log(Level.SEVERE, null, ex);
            Formatter.printToErrLn(Http.class, "printSimpleHeader", "Missing Resource File For Http Error Messages", "HttpResponseMessage");
            return;
        }
        print(os, errorNumber, HttpConstant.CONTENT_TYPE_TEXT_HTML, GetHandler.get("/error.html?code=" + errorNumber.getName() + "&title=" + title + "&msg=" + errorDetails));
    }
}
