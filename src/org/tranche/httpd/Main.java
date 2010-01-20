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

import java.util.Scanner;

/**
 *
 * @author TPapoulias
 */
public class Main {

    static final int TIME_OUT = 3600 * 1000;// one hour in milliseconds
    int timeout = TIME_OUT;
    Httpd httpd = null;

    /**
     * A new httpd server is launched.
     */
    public Main() {

        //(port,backLog,queueLength,minNumThreads,maxNumThreads)
        httpd = new Httpd(80, 10, 50, 10, 10);
        httpd.setTimeout(timeout);
        httpd.startServer();

    }

    /**
     * Helper method for stopping the server.
     */
    private void stopServer() {
        if (httpd != null) {
            httpd.stopServer();
        }
    }

    /**
     * Starts up a new httpd server and waits to receive a "q" from the stdin. Upon reception
     * it kills the server and the program terminates.
     * @param args
     */
    public static void main(String[] args) {

        Main main = new Main();

        Scanner scanner = new Scanner(System.in);

        String line = null;

        while (true) {
            line = scanner.nextLine();
            if (line.toLowerCase().trim().equals("q")) {
                main.stopServer();
                break;
            }
        }

        System.exit(0);
    }
}
