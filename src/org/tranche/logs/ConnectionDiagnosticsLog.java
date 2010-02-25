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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.tranche.time.TimeUtil;
import org.tranche.util.Text;

/**
 * <p>A log to help understand and diagnose connections to servers.</p>
 * <p>All output is thread safe.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ConnectionDiagnosticsLog {

    private final long start;
    private final Map<String, ServerRecord> serverRecords;
    private final List<ExceptionRecord> exceptionRecords;
    private final Map<String, List<SocketActivityWrapper>> socketActivityMap;
    private final String description;

    /**
     * <p>Create a new log.</p>
     * <p>All output is thread safe.</p>
     */
    public ConnectionDiagnosticsLog(String description) {
        this.start = TimeUtil.getTrancheTimestamp();
        this.serverRecords = new HashMap();
        this.exceptionRecords = new ArrayList();
        this.socketActivityMap = new HashMap();
        this.description = description;
    }

    /**
     * Add the host, delta from beginning of connection, and description of message
     * to the log.
     * @param host
     * @param delta
     * @param description
     */
    public void logServerRequest(String host, long delta, String description) {

        if (host == null || host.trim().equals("")) {
            host = "unknown";
        }

        ServerRecord serverRecord = null;
        synchronized (this.serverRecords) {
            serverRecord = this.serverRecords.get(host);

            // Lazily create record
            if (serverRecord == null) {
                serverRecord = new ServerRecord();
                this.serverRecords.put(host, serverRecord);
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
     * 
     * @param host
     */
    public void logConnection(String host, long timestamp) {
        logSocketActivity(host, timestamp, SocketActivity.Connect);
    }

    /**
     * 
     * @param host
     */
    public void logBanned(String host, long timestamp) {
        logSocketActivity(host, timestamp, SocketActivity.Banned);
    }

    /**
     * 
     * @param host
     */
    public void logUnbanned(String host, long timestamp) {
        logSocketActivity(host, timestamp, SocketActivity.Unbanned);
    }

    /**
     * 
     * @param host
     * @param activity
     */
    private void logSocketActivity(String host, long timestamp, SocketActivity activity) {
        if (host == null || host.trim().equals("")) {
            host = "unknown";
        }

        synchronized (this.socketActivityMap) {
            List<SocketActivityWrapper> activityList = this.socketActivityMap.get(host);

            // Lazily add the list to the map
            if (activityList == null) {
                activityList = new LinkedList();
                this.socketActivityMap.put(host, activityList);
            }

            activityList.add(new SocketActivityWrapper(activity, timestamp));
        }
    }

    /**
     * <p>Print out a summary of logged information, including useful averages
     * and ranges. Can call at anytime.</p>
     * @param out PrintStream to write out summary
     */
    public void printSummary(PrintStream out) {

        long stop = TimeUtil.getTrancheTimestamp();
        Map<String, Integer> requestsCountMap = new HashMap();
        long totalRequests = 0;
        for (final String host : this.serverRecords.keySet()) {
            int requestsCountForHost = 0;
            final ServerRecord serverRecord = this.serverRecords.get(host);
            for (String aggregateDescription : serverRecord.serverAggregates.keySet()) {
                AggregateRequest a = serverRecord.serverAggregates.get(aggregateDescription);

                requestsCountForHost += a.totalCount;
            }
            requestsCountMap.put(host, requestsCountForHost);
            totalRequests += requestsCountForHost;
        }

        out.println();
        out.println(">>>>>>>>>>>>>>>>>>>>>>> Start: " + this.description + " <<<<<<<<<<<<<<<<<<<<<<<");
        out.println();
        out.println("Current time:  " + Text.getFormattedDate(stop));
        out.println("Started log:   " + Text.getFormattedDate(start));
        out.println("Ellapsed:      " + Text.getPrettyEllapsedTimeString(stop - start));
        out.println("Total requests: " + totalRequests);
        out.println();

        synchronized (this.serverRecords) {

            long totalAverageRequest = 0;

            // For each server, print out relevant information
            for (final String host : this.serverRecords.keySet()) {
                final ServerRecord serverRecord = this.serverRecords.get(host);

                println(out);
                println(out, "Server:       " + host + " (Started: " + Text.getFormattedDate(serverRecord.start) + ")");
                println(out, "------------------------------------------------------------------------------");
                println(out, "        Total requests: " + requestsCountMap.get(host));
                println(out, "      Shortest request: " + Text.getPrettyEllapsedTimeString(serverRecord.shortestRequest.getDelta()));
                println(out, "                   -> : " + serverRecord.shortestRequest.getDescription());
                println(out, "       Longest request: " + Text.getPrettyEllapsedTimeString(serverRecord.longestRequest.getDelta()));
                println(out, "                   -> : " + serverRecord.longestRequest.getDescription());
                println(out);

                long avgServerRequest = 0;
                for (String aggregateDescription : serverRecord.serverAggregates.keySet()) {
                    AggregateRequest a = serverRecord.serverAggregates.get(aggregateDescription);
                    println(out);
                    println(out, "  Request description : " + aggregateDescription + " (" + a.totalCount + " request" + (a.totalCount == 1 ? "" : "s") + ")");
                    long avgTime = (long) ((double) a.totalTime / (double) a.totalCount);
                    println(out, "            Avg. time : " + Text.getPrettyEllapsedTimeString(avgTime));

                    avgServerRequest += avgTime;
                }

                avgServerRequest = (long) ((double) avgServerRequest / (double) serverRecord.serverAggregates.size());
                println(out);
                println(out, "  Avg. server request : " + Text.getPrettyEllapsedTimeString(avgServerRequest));

                List<SocketActivityWrapper> socketActivityList = this.socketActivityMap.get(host);

                println(out);
                println(out, "  Socket activity");
                if (socketActivityList == null) {
                    println(out, "            No socket reconnects nor bans/unbans were logged.");
                } else {
                    int connections = 0, bans = 0, unbans = 0;

                    for (SocketActivityWrapper saw : socketActivityList) {
                        if (saw.activity.equals(SocketActivity.Connect)) {
                            connections++;
                            println(out, "                   Connected -> : " + Text.getFormattedDate(saw.timestamp));
                        } else if (saw.activity.equals(SocketActivity.Banned)) {
                            bans++;
                            println(out, "                   Banned -> : " + Text.getFormattedDate(saw.timestamp));
                        } else if (saw.activity.equals(SocketActivity.Unbanned)) {
                            unbans++;
                            println(out, "                   Unbanned -> : " + Text.getFormattedDate(saw.timestamp));
                        }
                    }
                    println(out, "            Total connections: " + connections);
                    println(out, "            Total bans: " + bans);
                    println(out, "            Total unbans: " + unbans);
                }

                totalAverageRequest += avgServerRequest;
            }

            totalAverageRequest = (long) ((double) totalAverageRequest / (double) this.serverRecords.size());

            // Print out aggregate averages
            println(out);
            println(out, " TOTAL AVERAGE REQUEST TIME FOR ALL SERVERS: " + Text.getPrettyEllapsedTimeString(totalAverageRequest));
        }

        synchronized (this.exceptionRecords) {

            println(out);
            println(out, "Exceptions:  (" + this.exceptionRecords.size() + ")");
            println(out, "------------------------------------------------------------------------------");

            // Print out details of exceptions
            for (ExceptionRecord er : this.exceptionRecords) {
                println(out, "      " + Text.getFormattedDate(er.getTimestamp()) + ": " + er.getException().getClass().getSimpleName() + " with message <" + er.getException().getMessage() + ">: " + er.getDescription());
            }
        }

        println(out);
        println(out, ">>>>>>>>>>>>>>>>>>>>>>> End: " + description + " <<<<<<<<<<<<<<<<<<<<<<<");
        println(out);
    }

    /**
     * 
     */
    private class ServerRecord {

        protected final long start;
        /**
         * <p>Keep track of shortest and longest request.</p>
         */
        protected  Request shortestRequest,   longestRequest ;
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
     * @param out
     */
    private static void println(PrintStream out) {
        synchronized (out) {
            out.println();
        }
    }

    /**
     * 
     * @param out
     * @param line
     */
    private static void println(PrintStream out, String line) {
        synchronized (out) {
            out.println(line);
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

    /**
     * 
     */
    enum SocketActivity {

        Connect, Banned, Unbanned
    }

    /**
     * 
     */
    class SocketActivityWrapper {

        final long timestamp;
        final SocketActivity activity;

        public SocketActivityWrapper(SocketActivity activity, long timestamp) {
            this.timestamp = timestamp;
            this.activity = activity;
        }
    }
}
