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
package org.tranche.hash.span;

import java.security.cert.X509Certificate;
import java.util.*;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.hash.BigHash;
import org.tranche.configuration.Configuration;
import org.tranche.network.ConnectionUtil;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;

/**
 * <p>Determines hash spans for list of servers based on available space.</p>
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class HashSpanCalculator {

    // Servers to calculate
    public List<String> urls;
    // Available space for each server
    public Map<String, Long> sizes;
    public Map<String, Set<HashSpan>> spans;
    // Recommended sizes
    public Map<String, Double> percentages;
    // Total available size between all servers
    private long total;
    private X509Certificate certificate;
    // Number of servers with hash spans covering the same hash
    private int replications;

    /**
     * Create a hash span calculator. You will need to add servers before 
     * performing the final calculation. If you don't specify a number of 
     * repetitions, the hash span calculator will assign each server with a 
     * unique hash span, i.e., 1 repetition.
     * @param certificate The public certificate used to retrieve information about the servers.
     */
    public HashSpanCalculator(X509Certificate certificate) {
        this(certificate, 1);
    }

    /**
     * Create a hash span calculator. You will need to add servers before 
     * performing the final calculation.
     * @param certificate The public certificate used to retrieve information about the servers.
     * @param replications The number of times you wish each file chunk to be 
     * replicated across the servers. 1 means every server will get a unique 
     * hash span, i.e., each server gets one copy of each file chunk. More 
     * replications means more redundancy and larger hash spans for each server, 
     * requiring more storage.
     */
    public HashSpanCalculator(X509Certificate certificate, int replications) {

        this.certificate = certificate;

        this.setReplications(replications);
        this.urls = new ArrayList();
        this.sizes = new HashMap();
        this.spans = new HashMap();
        this.percentages = new HashMap();
        total = 0;
    }

    /**
     * Add a server to consider in the final calculation. You can continue 
     * adding servers after calling this method.
     */
    public void addServer(String url) {
        urls.add(url);
    }

    /**
     * Add a list of servers to consider in the final calculation. You can 
     * continue adding servers after calling this method.
     */
    public void addServers(List<String> urls) {

        for (String url : urls) {
            this.urls.add(url);
        }
    }

    /**
     * <p>Calculate the hash spans based on the servers added.</p>
     * @return A map of urls with the calculated spans
     */
    public Map<String, Set<HashSpan>> calculateSpans() throws Exception {

        // Reset total. Performing new calculation.
        if (this.total != 0) {
            this.total = 0;
        }

        if (urls.size() == 0) {
            throw new RuntimeException("There are no servers selected. Please add server urls before calculating the spans.");
        }

        // Create map of associated sizes with each server
        for (String url : urls) {

            long space = getAvailableSpace(url);

            this.total += space;

            sizes.put(url, new Long(space));
        }

        // Assign spans to each server
        assignSpans();

        return spans;
    }

    /**
     * Calculates available space for a server.
     */
    private long getAvailableSpace(String url) throws Exception {

        long space = 0;

        Configuration config = IOUtil.getConfiguration(ConnectionUtil.connectURL(url, false), certificate, SecurityUtil.getAnonymousKey());

        Set<DataDirectoryConfiguration> dirs = config.getDataDirectories();

        for (DataDirectoryConfiguration next : dirs) {
            space += next.getSizeLimit();
        }

        // Scale down spaces, since they can be pretty large. Relative anyhow.
        space /= (1024);

        return space;
    }

    /**
     * Assign hash spans.
     */
    private void assignSpans() throws Exception {

        // Pointer to where next hash span should start
        BigHash startHash1 = HashSpan.FIRST;

        byte[] startBytes1, endBytes1; // Bytes for first hash span. May only be one.
        BigHash endHash1;
        String url;

        Set<HashSpan> serverSpans; // Holds hash spans for each server

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // EVERY SERVER GETS FULL HASH SPAN
        // If num replications greater than number of servers, give them all full hash span
        if (this.replications >= this.urls.size()) {

            for (String nextServer : this.urls) {

                this.percentages.put(nextServer, new Double(100));

                // Instantiate new for each URL for deep copies
                serverSpans = new TreeSet();
                serverSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));

                this.spans.put(nextServer, serverSpans);
            }

            return;
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // GIVE SERVERS APPROPRIATELY SIZED HASHSPAN(S)
        // Assign spans for every server but the last
        for (int i = 0; i < urls.size() - 1; i++) {

            // Get next server url
            url = urls.get(i);

            // Calculate percentage for hash span
            long size = sizes.get(url).longValue();
            double percent = (double) size / (double) total;

            this.percentages.put(url, new Double(percent));

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // Get start bytes
            startBytes1 = startHash1.toByteArray();

            // Calculate end hash
            endBytes1 = new byte[BigHash.HASH_LENGTH];

            // TODO Use replications to _evenly_ distribute hash spans. Do some
            // examples by hand, first.
            double length = (Byte.MAX_VALUE + Math.abs(Byte.MIN_VALUE)) * percent * this.replications;
            double assign = length + startBytes1[0];

            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // If server is getting one hash span. This happens when the assignment
            // can fit in on hash span.

            if (assign < Byte.MAX_VALUE) {

                for (int j = 0; j < endBytes1.length; j++) {
                    endBytes1[j] = (byte) assign;
                }

                endHash1 = BigHash.createFromBytes(endBytes1);

                serverSpans = new TreeSet();
                serverSpans.add(new HashSpan(startHash1, endHash1));

                // Create hash span and assign
                spans.put(url, serverSpans);

                // Update starting hash pointer.
                startHash1 = BigHash.createFromBytes(endBytes1);
            } // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // The server is getting two hash spans. Finish off current hash span
            // and create second.
            else {

                // Create a second set of start and end bytes
                byte[] startBytes2 = new byte[BigHash.HASH_LENGTH],
                        endBytes2 = new byte[BigHash.HASH_LENGTH];

                BigHash startHash2, endHash2;

                // First hash span is from startHash1 (pointer to end of last 
                // big hash) to last byte.
                // I.e., theoretically, if assign is 15, max byte is 12, start
                // is 8, then want to assign bytes (8, 12)
                for (int j = 0; j < endBytes1.length; j++) {
                    endBytes1[j] = Byte.MAX_VALUE;
                }

                endHash1 = BigHash.createFromBytes(endBytes1);

                HashSpan span1 = new HashSpan(startHash1, endHash1);

                // Calculate remaining bytes.
                double remaining = length - (Byte.MAX_VALUE - startHash1.toByteArray()[0]);
                assign = Byte.MIN_VALUE + remaining;

                // Second hash span is from first byte to assign remaining bytes
                // I.e., theoretically, if remaining bytes is 5, then
                // want to assign bytes (0, 5) to second hash span
                for (int j = 0; j < startBytes2.length; j++) {
                    startBytes2[j] = Byte.MIN_VALUE;
                }

                for (int j = 0; j < endBytes2.length; j++) {
                    endBytes2[j] = (byte) assign;
                }

                startHash2 = BigHash.createFromBytes(startBytes2);
                endHash2 = BigHash.createFromBytes(endBytes2);

                HashSpan span2 = new HashSpan(startHash2, endHash2);

                serverSpans = new LinkedHashSet(); // Keep in order
                serverSpans.add(span1);
                serverSpans.add(span2);

                // Create hash span and assign
                spans.put(url, serverSpans);

                // Update starting hash pointer.
                startHash1 = BigHash.createFromBytes(endBytes2);
            }
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // LAST SERVER MUST PICK UP THE REST. NO GAPS.
        // Last server gets remainder to make sure no hash left out due to
        // rounding errors.

        url = urls.get(urls.size() - 1);

        // Getting percentage only for records
        long size = sizes.get(url).longValue();
        double percent = (double) size / (double) total;

        this.percentages.put(url, new Double(percent));

        serverSpans = new TreeSet();
        serverSpans.add(new HashSpan(startHash1, HashSpan.LAST));

        spans.put(url, serverSpans);
    }

    /**
     * Retrieve number of replications.
     * @return
     */
    public int getReplications() {
        return replications;
    }

    /**
     * Set number of replications.
     * @param replications
     */
    public void setReplications(int replications) {
        this.replications = replications;
    }

    /**
     * After performing the calculation, you can get each server's respective percentage.
     * @param url Server url. Must have performed calculation already.
     * @return String Percentage
     */
    public double getServerPercentage(String url) {
        return this.percentages.get(url).doubleValue();
    }
}
