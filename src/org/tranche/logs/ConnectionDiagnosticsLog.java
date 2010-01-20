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
package org.tranche.logs;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tranche.time.TimeUtil;
import org.tranche.util.Text;

/**
 * <p>A log to help understand and diagnose connections to servers.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ConnectionDiagnosticsLog {

    private final long start;
    private final String description;
    private final Map<String, ServerRecord> serverRecords;
    private final List<ExceptionRecord> exceptionRecords;

    /**
     * Create a new log with description of log contents.
     * @param description An arbitrary description of why being used. Can be null.
     */
    public ConnectionDiagnosticsLog(String description) {
        this.start = TimeUtil.getTrancheTimestamp();
        this.description = description;
        this.serverRecords = new HashMap();
        this.exceptionRecords = new ArrayList();
    }

    /**
     * Add the url, delta from beginning of connection, and description of message
     * to the log.
     * @param url
     * @param delta
     * @param description
     */
    public void logServerRequest(String url, long delta, String description) {
        ServerRecord serverRecord = null;
        synchronized (this.serverRecords) {
            serverRecord = this.serverRecords.get(url);

            // Lazily create record
            if (serverRecord == null) {
                serverRecord = new ServerRecord();
                this.serverRecords.put(url, serverRecord);
            }
        }

        synchronized (serverRecord) {
            Request thisRequest = new Request(delta, description);

            // Is this the longest request yet?
            if (serverRecord.longestRequest == null) {
                serverRecord.longestRequest = thisRequest;
            } else {
                if (thisRequest.getDelta() > serverRecord.longestRequest.getDelta()) {
                    serverRecord.longestRequest = thisRequest;
                }
            }

            // Is this the shortest request yet?
            if (serverRecord.shortestRequest == null) {
                serverRecord.shortestRequest = thisRequest;
            } else {
                if (thisRequest.getDelta() < serverRecord.shortestRequest.getDelta()) {
                    serverRecord.shortestRequest = thisRequest;
                }
            }

            // Log aggregate information 
            AggregateRequest aggregateRequest = serverRecord.serverAggregates.get(description);

            // If aggregate request doesn't exist, create it
            if (aggregateRequest == null) {
                aggregateRequest = new AggregateRequest();
                serverRecord.serverAggregates.put(description, aggregateRequest);
            }

            aggregateRequest.totalTime += delta;
            aggregateRequest.totalCount++;
        }
    }

    /**
     * Add exception, timestamp, and string to the log.
     * @param ex
     * @param timestamp
     * @param description
     */
    public void logException(Exception ex, long timestamp, String description) {
        synchronized (this.exceptionRecords) {
            this.exceptionRecords.add(new ExceptionRecord(ex, timestamp, description));
        }
    }

    /**
     * <p>Print out a summary of logged information, including useful averages 
     * and ranges. Can call at anytime.</p>
     * <p>Prints to standard out.</p>
     */
    public void printSummary() {
        printSummary(System.out);
    }

    /**
     * <p>Print out a summary of logged information, including useful averages
     * and ranges. Can call at anytime.</p>
     * @param out PrintStream to write out summary
     */
    public void printSummary(PrintStream out) {

        long stop = TimeUtil.getTrancheTimestamp();

        out.println();
        out.println(">>>>>>>>>>>>>>>>>>>>>>> Start: connection diagnostics report <<<<<<<<<<<<<<<<<<<<<<<");
        out.println();
        out.println("Current time: " + Text.getFormattedDate(stop));
        out.println("Started log:  " + Text.getFormattedDate(start));
        out.println("Ellapsed:     " + Text.getPrettyEllapsedTimeString(stop - start));
        out.println();

        synchronized (this.serverRecords) {

            long totalAverageRequest = 0;

            // For each server, print out relevant information
            for (final String url : this.serverRecords.keySet()) {
                final ServerRecord serverRecord = this.serverRecords.get(url);

                out.println();
                out.println("Server:       " + url + " (Started: " + Text.getFormattedDate(serverRecord.start) + ")");
                out.println("------------------------------------------------------------------------------");
                out.println("      Shortest request: " + Text.getPrettyEllapsedTimeString(serverRecord.shortestRequest.getDelta()));
                out.println("                   -> : " + serverRecord.shortestRequest.getDescription());
                out.println("       Longest request: " + Text.getPrettyEllapsedTimeString(serverRecord.longestRequest.getDelta()));
                out.println("                   -> : " + serverRecord.longestRequest.getDescription());
                out.println();

                long avgServerRequest = 0;
                for (String aggregateDescription : serverRecord.serverAggregates.keySet()) {
                    AggregateRequest a = serverRecord.serverAggregates.get(aggregateDescription);
                    out.println();
                    out.println("  Request description : " + aggregateDescription + " (" + a.totalCount + " request" + (a.totalCount == 1 ? "" : "s") + ")");
                    long avgTime = (long) ((double) a.totalTime / (double) a.totalCount);
                    out.println("            Avg. time : " + Text.getPrettyEllapsedTimeString(avgTime));

                    avgServerRequest += avgTime;
                }

                avgServerRequest = (long) ((double) avgServerRequest / (double) serverRecord.serverAggregates.size());
                out.println();
                out.println("  Avg. server request : " + Text.getPrettyEllapsedTimeString(avgServerRequest));

                totalAverageRequest += avgServerRequest;
            }

            totalAverageRequest = (long) ((double) totalAverageRequest / (double) this.serverRecords.size());

            // Print out aggregate averages
            out.println();
            out.println(" TOTAL AVERAGE REQUEST TIME FOR ALL SERVERS: " + Text.getPrettyEllapsedTimeString(totalAverageRequest));
        }

        synchronized (this.exceptionRecords) {

            out.println();
            out.println("Exceptions:  (" + this.exceptionRecords.size() + ")");
            out.println("------------------------------------------------------------------------------");

            // Print out details of exceptions
            for (ExceptionRecord er : this.exceptionRecords) {
                out.println("      " + Text.getFormattedDate(er.getTimestamp()) + ": " + er.getException().getClass().getSimpleName() + " with message <" + er.getException().getMessage() + ">: " + er.getDescription());
            }
        }

        out.println();
        out.println(">>>>>>>>>>>>>>>>>>>>>>> End: connection diagnostics report <<<<<<<<<<<<<<<<<<<<<<<");
        out.println();
    }

    /**
     * 
     */
    private class ServerRecord {

        protected final long start;
        /**
         * <p>Keep track of shortest and longest request.</p>
         */
        protected Request shortestRequest,  longestRequest;
        /**
         * <p>Map used to store aggregate information about servers' requests
         * so can later produce averages and other statistics.</p>
         * 
         * <p>String:              A consistent description of the request</p>
         * <p>AggregateRequest:    How keep track of times ellapsed and number
         *                      of times type of request happened</p>
         */
        protected Map<String, AggregateRequest> serverAggregates;

        private ServerRecord() {
            start = TimeUtil.getTrancheTimestamp();
            shortestRequest = null;
            longestRequest = null;
            serverAggregates = new HashMap();
        }
    }

    /**
     * 
     */
    private class Request {

        private final long delta;
        private final String description;

        private Request(long delta, String description) {
            this.delta = delta;
            this.description = description;
        }

        public long getDelta() {
            return delta;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Used to calculate averages.
     */
    private class AggregateRequest {

        /**
         * Add times to do something.
         */
        protected long totalTime = 0;
        /**
         * Add number of times something done.
         */
        protected long totalCount = 0;
    }

    /**
     * 
     */
    private class ExceptionRecord {

        private final Exception exception;
        private final long timestamp;
        private final String description;

        ExceptionRecord(Exception ex, long timestamp, String description) {
            this.exception = ex;
            this.timestamp = timestamp;
            this.description = description;
        }

        public Exception getException() {
            return exception;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getDescription() {
            return description;
        }
    }
}
