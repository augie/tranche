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

import java.util.Locale;

/**
 * Some support for various locales. HTTP response and error messages names for resource bundles used.
 * @author Takis Papoulias 
 */
public interface IFileResources {

    /**
     * Location of the HTTP properties
     */
    public static final String HTTP_PROPERTIES_DIRECTORY = "org/tranche/httpd";
    /**
     * Location of the HTML pages
     */
    public static final String HTTP_PAGE_DIRECTORY = "/org/tranche/httpd/www";
    /**
     * The supporting locale of this compilation.
     */
    public static final Locale LOCALE = Locale.US;
    /**
     * The name of the resource bundle where response messages can be found in this locale
     */
    public static final String HTTP_RESPONSE_MESSAGES_PROP_FILE = HTTP_PROPERTIES_DIRECTORY + "/HttpResponseMessages";
}
