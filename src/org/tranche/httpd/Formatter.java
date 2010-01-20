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

import java.util.Date;

/**
 * This class is a helper class to handle the formatting strings written either as an
 * HTTP response, or to stdout, or to stderr.
 *
 * @author Takis Papoulias 
 */
public class Formatter implements IVerbosity {

    /**
     * Overloaded helper method for creating a HTTP proper response for a param value pair.
     * @param param     The HTTP parameter name
     * @param value     The Http parameter value
     * @return
     */
    public static String createPvFormat(HttpConstant param, HttpConstant value) {
        String s = param.getName();
        s += HttpConstant.PARAM_VALUE_SEPARATOR.getName();
        s += HttpConstant.SEPARATOR.getName();
        s += value.getName();
        return s;
    }

    /**
     * Overloaded helper method for creating a HTTP proper response for a param value pair.
     * @param param     The HTTP parameter name
     * @param value     The Http parameter value
     * @return
     */
    public static String createPvFormat(HttpConstant param, int value) {
        String s = param.getName();
        s += HttpConstant.PARAM_VALUE_SEPARATOR.getName();
        s += HttpConstant.SEPARATOR.getName();
        s += value;
        return s;
    }

    /**
     *  Helper method for creating a properly formatted status line for the response.
     * @param statusId  The ID of the status e.g. 408 for request time out
     * @param msg   A short description of the status
     * @return
     */
    public static String createStatusLine(HttpConstant statusId, String msg) {
        String s = HttpConstant.HTTP_VERSION.getName();
        s += HttpConstant.SEPARATOR.getName();
        s += statusId.getName();
        s += HttpConstant.SEPARATOR.getName();
        s += msg;
        return s;
    }

    /**
     * Helper method that prints to stdout. Used to maintain a consistent look.
     * @param msg   The msg to print
     */
    public static void printToOutLn(String msg) {
        System.out.println("[Httpd] " + msg);
    }

    /**
     * Helper method that prints to the stderr details about a failure. To be used to describe
     * what the program was trying to accomplish when a failure occured.
     * @param caller    The class that produced the error
     * @param methodName    The method name of the class where the error originated
     * @param msg   A shortdescription of the error
     * @param data  The offending data if any
     */
    public static void printToErrLn(Class caller, String methodName, String msg, String data) {
        System.err.println("[Httpd->" + caller.getName() + "." + methodName + "()] [" + (new Date()).toString() + "] " + msg + " : " + data);
    }
}
