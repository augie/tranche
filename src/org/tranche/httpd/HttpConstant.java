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

/**
 *HTTP ontologies are wrappered as HTTPConstants. Because these are immutable and
 * defined by the protocol this class is declared and made final and the constructor is private.
 * @author TPapoulias
 */
public final class HttpConstant {

    /*
     * Method http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1
     *        Method         = "OPTIONS"                ; Section 9.2
    | "GET"                    ; Section 9.3
    | "HEAD"                   ; Section 9.4
    | "POST"                   ; Section 9.5
    | "PUT"                    ; Section 9.6
    | "DELETE"                 ; Section 9.7
    | "TRACE"                  ; Section 9.8
    | "CONNECT"                ; Section 9.9
    | extension-method
    extension-method = token
     **/
    public static final HttpConstant METHOD_OPTIONS = new HttpConstant("OPTIONS");
    public static final HttpConstant METHOD_GET = new HttpConstant("GET");
    public static final HttpConstant METHOD_POST = new HttpConstant("POST");
    public static final HttpConstant METHOD_PUT = new HttpConstant("PUT");
    public static final HttpConstant METHOD_DELETE = new HttpConstant("DELETE");
    public static final HttpConstant METHOD_TRACE = new HttpConstant("TRACE");
    public static final HttpConstant METHOD_CONNECT = new HttpConstant("CONNECT");
    public static final HttpConstant HTTP_VERSION = new HttpConstant("HTTP/1.1");
    public static final HttpConstant SEPARATOR = new HttpConstant(" ");
    public static final HttpConstant PARAM_VALUE_SEPARATOR = new HttpConstant(":");
    public static final HttpConstant CONTENT_TYPE = new HttpConstant("Content-type");
    public static final HttpConstant CONTENT_LENGTH = new HttpConstant("Content-length");
    public static final HttpConstant CONTENT_TYPE_TEXT_HTML = new HttpConstant("text/html");
    public static final HttpConstant CONTENT_TYPE_JNLP = new HttpConstant("application/x-java-jnlp-file");
    //Status-Code  http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html
    public static final HttpConstant STATUS_OK = new HttpConstant("200");
    public static final HttpConstant STATUS_REQUEST_TIMEOUT = new HttpConstant("408");
    public static final HttpConstant SERVICE_UNAVAILABLE = new HttpConstant("503");
    //Error codes
    public static final HttpConstant ERROR_INVALID_METHOD = new HttpConstant("400");
    public static final HttpConstant ERROR_FILE_NOT_FOUND = new HttpConstant("404");
    public static final HttpConstant ERROR_REQUEST_NOT_UNDERSTOOD = new HttpConstant("444");
    //Defaults
    public static final HttpConstant DEFAULT_HOME_PAGE = new HttpConstant("/index.html");
    private final String name;

    /**
     * Constructor made private to insure immutability of the objects
     * @param name  The name of the constant
     */
    private HttpConstant(String name) {
        this.name = name;
    }

    /**
     * Getter for the name of this constant
     * @return
     */
    public final String getName() {
        return name;
    }
}
