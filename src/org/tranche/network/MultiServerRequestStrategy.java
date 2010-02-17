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
package org.tranche.network;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tranche.Tertiary;
import org.tranche.exceptions.AssertionFailedException;

/**
 * <p>Say a client has a request for many servers. If pick any server (randomly or using a heuristic), you can create this object to determine:</p>
 * <ol>
 *   <li>Which servers will receive subsets of the total request (called 'partitions') so that all the requests are filled efficiently</li>
 *   <li>The 'depth' of the request, which is an approximation of the efficiency.</li>
 * </ol>
 * <p>A simple strategy: if want to see best server to which to send a request, calculate this for all online servers and select the strategy with the smallest depth.</p>
 * <p>Note that a server receiving a multi-server request will also build a MultiServerRequestStategy object, using its host as the parameter. That way, it will know how to partition the request amongst servers to which it is connected.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class MultiServerRequestStrategy {

    /**
     * <p>Constant used for infinite depth--i.e., cannot solve. If depth equal to this, then discard strategy.</p>
     */
    public static final int INFINITE_DEPTH = Integer.MAX_VALUE;
    private final String hostReceivingRequest;
    private final Map<String, Set<String>> partitionsMap;
    private final Map<String, Set<Host>> hostPartitionsMap;
    private final Set<String> unfulfillable;
    private final int depth;
    private final Set<String> hostsToIncludeInRequest;

    private MultiServerRequestStrategy(String hostReceivingRequest, Map<String, Set<Host>> hostPartitionsMap, Map<String, Set<String>> partitionsMap, Collection<String> unfulfillable, Collection<String> hostsToIncludeInRequest, int depth) {
        this.hostReceivingRequest = hostReceivingRequest;
        this.partitionsMap = partitionsMap;
        this.unfulfillable = new HashSet<String>(unfulfillable);
        this.hostPartitionsMap = hostPartitionsMap;
        this.depth = depth;
        this.hostsToIncludeInRequest = new HashSet<String>(hostsToIncludeInRequest);
    }

    /**
     * <p>Finds set of strategies that have the smallest depth, which is a strong heuristic for speed.</p>
     * <p>Uses as primary host only online, core servers to which the client is connected.</p>
     * <p>Uses in the tree of connections only online, core servers.</p>
     * <p>If caller would like to select strategy based on heuristics other than depth, can call create method for desired to get desired set of strategies.</p>
     * @param serverHostsToRequest
     * @return
     */
    public static Collection<MultiServerRequestStrategy> findFastestStrategiesUsingConnectedCoreServers(Collection<String> serverHostsToRequest, Tertiary readable, Tertiary writable) {
        StatusTable table = NetworkUtil.getStatus().clone();

        List<MultiServerRequestStrategy> strategies = new LinkedList();
        List<String> coreServerHosts = new LinkedList();
        for (StatusTableRow row : table.getRows()) {
            if (row.isCore() && row.isOnline() && ConnectionUtil.isConnected(row.getHost()) && (readable.equals(Tertiary.DONT_CARE) || (readable.equals(Tertiary.TRUE) && row.isReadable()) || (readable.equals(Tertiary.FALSE) && !row.isReadable())) && (writable.equals(Tertiary.DONT_CARE) || (writable.equals(Tertiary.TRUE) && row.isWritable()) || (writable.equals(Tertiary.FALSE) && !row.isWritable()))) {
                coreServerHosts.add(row.getHost());
            }
        }
        Collections.shuffle(coreServerHosts);

        // make the strategies for each
        LinkedList<MultiServerRequestStrategy> possibleStrategies = new LinkedList();
        for (String host : coreServerHosts) {
            MultiServerRequestStrategy strategy = create(host, serverHostsToRequest);
            if (strategy.getUnfulfillableHosts().isEmpty()) {
                possibleStrategies.addFirst(strategy);
            } else {
                possibleStrategies.addLast(strategy);
            }
        }

        // Find smallest depth
        int smallestDepth = INFINITE_DEPTH;
        for (MultiServerRequestStrategy strategy : possibleStrategies) {
            if (strategy.depth < smallestDepth) {
                smallestDepth = strategy.depth;
            }
        }

        // add servers with smallest depth
        for (MultiServerRequestStrategy strategy : possibleStrategies) {
            if (strategy.depth == smallestDepth) {
                strategies.add(strategy);
            }
            if (strategy.depth < smallestDepth) {
                throw new AssertionFailedException("Smallest depth {" + smallestDepth + "} somehow greater than current depth {" + strategy.depth + "}.");
            }
        }

        // second fastest strategies -- addfiletool does not work without these
        List<MultiServerRequestStrategy> secondFastestStrategies = new LinkedList();
        for (MultiServerRequestStrategy strategy : possibleStrategies) {
            if (strategy.depth == smallestDepth + 1) {
                secondFastestStrategies.add(strategy);
            }
        }
        strategies.addAll(secondFastestStrategies);

        return strategies;
    }

    /**
     * <p>Say a client has a request for many servers. If pick any server (randomly or using a heuristic), you can create this object to determine:</p>
     * <ol>
     *   <li>Which servers will receive subsets of the total request (called 'partitions') so that all the requests are filled efficiently</li>
     *   <li>The 'depth' of the request, which is an approximation of the efficiency.</li>
     * </ol>
     * <p>A simple strategy: if want to see best server to which to send a request, calculate this for all online servers and select the strategy with the smallest depth.</p>
     * <p>Note that a server receiving a multi-server request will also build a MultiServerRequestStategy object, using its host as the parameter. That way, it will know how to partition the request amongst servers to which it is connected.</p>
     * @param hostReceivingRequest Server to which to send a request for a set of servers
     * @param serverHostsToRequest Set of all servers included in the request. Note that if hostReceivingRequest is not included in the set, then it can still be used to pass the requests on--but it won't be included in the request!</p>
     * @return The MultiServerRequestStrategy object
     */
    public static MultiServerRequestStrategy create(String hostReceivingRequest, Collection<String> serverHostsToRequest) {
        StatusTable table = NetworkUtil.getStatus().clone();

        // If server offline, everything is unreachable! Depth is infinite.
        if (!table.getRow(hostReceivingRequest).isOnline()) {
            return new MultiServerRequestStrategy(hostReceivingRequest, new HashMap(), new HashMap(), serverHostsToRequest, serverHostsToRequest, INFINITE_DEPTH);
        }

        // Get information about server
        ServerStatusTablePerspective info = ServerStatusUpdateProcess.getServerStatusTablePerspectiveForServer(hostReceivingRequest);

        // Unfulfillable. For now, this means offline or no route to host from hostReceivingRequest!
        Set<String> unfulfillable = new HashSet();
        for (String nextServerHost : serverHostsToRequest) {
            if (!table.contains(nextServerHost) || !table.getRow(nextServerHost).isOnline()) {
                unfulfillable.add(nextServerHost);
            }
        }

        // We'll need to partition these hosts. Copy parameter set contents -- assume it is unmodifiable
        Set<String> unpartitionedHosts = new HashSet();
        unpartitionedHosts.addAll(serverHostsToRequest);

        // The following will be handled below
        unpartitionedHosts.remove(hostReceivingRequest);
        unpartitionedHosts.removeAll(info.getConnectedCoreServerHosts());
        unpartitionedHosts.removeAll(unfulfillable);

        // Used to keep state while partioning. Note that we'll potentially use a server, even if it is not
        // a 'target'
        List<String> currentLevelHosts = new LinkedList();
        currentLevelHosts.addAll(info.getConnectedCoreServerHosts());

//        Set<String> nextLevelHosts = new HashSet();
        List<String> nextLevelHosts = new LinkedList();

        // This is the map that will contain information on how to properly partition the requests.
        // For every server connected to subject server, create empty collections. We'll add to this
        // as we discover how to partition the requests.
        Map<String, Set<Host>> hostMap = new HashMap();
        for (String host : info.getConnectedCoreServerHosts()) {

            if (!table.getRow(host).isOnline()) {
                continue;
            }

            Set<Host> partition = new HashSet();

            // Only add the server to the partition if it is requested! This server
            // might be used even if it isn't in the request set simply to keep the depth of
            // the search tree low.
            if (serverHostsToRequest.contains(host)) {
                partition.add(new Host(host, true, 1, hostReceivingRequest));
            } else {
                partition.add(new Host(host, false, 1, hostReceivingRequest));
            }

            hostMap.put(host, partition);
        }

        // Need to keep track of servers already processed.
        Set<String> alreadyHandledHosts = new HashSet();
        alreadyHandledHosts.add(hostReceivingRequest);
        alreadyHandledHosts.addAll(info.getConnectedCoreServerHosts());
        alreadyHandledHosts.addAll(unfulfillable);

        // Continue until everything is partitioned
        int depth = 1;
        PARTIONING:
        while (unpartitionedHosts.size() > 0) {

            depth++;
            if (depth > 1000000) {
                throw new AssertionFailedException("MultiServerRequestStrategy.partitionRequestsToServersConnectedToServer is running away; depth=" + depth);
            }

            // Shouldn't happen. Our connection algorithm works around offline servers.
            if (currentLevelHosts.size() == 0 && unpartitionedHosts.size() > 0) {

                // If true, treated as an assertion failed. If false, treated as unreachable.
                // Sometimes use true to troubleshoot, but production should be false. 
                //
                // The show must go on.
                final boolean isError = false;

                if (isError) {
                    System.err.println("*************************************************************************************************");
                    System.err.println(" ASSERTION FAILED, DID NOT INCORPORATE " + unpartitionedHosts.size() + " SERVER(S)");
                    System.err.println("   MultiServerRequestStrategy has failed to process every server, program failure.");
                    System.err.println("   Printing network status table");
                    System.err.println("*************************************************************************************************");

                    for (String host : table.getHosts()) {
                        if (unpartitionedHosts.contains(host)) {
                            System.err.println(" [FAILED] " + table.getRow(host));
                        } else {
                            System.err.println(" [SUCCESS] " + table.getRow(host));
                        }
                    }

                    // Print out the strategy for troubleshooting
                    System.err.println();
                    printStrategy(hostReceivingRequest, hostMap, unfulfillable, System.err);

                    // Quickly find the leaves
                    int maxDepth = 1;
                    for (Set<Host> hosts : hostMap.values()) {
                        for (Host h : hosts) {
                            if (h.level > maxDepth) {
                                maxDepth = h.level;
                            }
                        }
                    }

                    Set<Host> leaves = new HashSet();
                    for (Set<Host> hosts : hostMap.values()) {
                        for (Host h : hosts) {
                            if (h.level == maxDepth) {
                                leaves.add(h);
                            }
                        }
                    }

                    System.err.println();
                    System.err.println("Leaf nodes on request graph:");
                    for (Host nextHost : leaves) {
                        ServerStatusTablePerspective nextInfo = ServerStatusUpdateProcess.getServerStatusTablePerspectiveForServer(nextHost.host);
                        System.err.println(" * " + nextInfo.host + " is connected to " + nextInfo.getConnectedCoreServerHosts().size() + " server(s)");
                        for (String connectedHost : nextInfo.getConnectedCoreServerHosts()) {
                            System.err.println("    - " + connectedHost + " (already handled?: " + alreadyHandledHosts.contains(connectedHost) + ")");
                        }
                    }

                    System.err.println("*************************************************************************************************");

                    StringBuffer failedServers = new StringBuffer();
                    for (String failedServer : unpartitionedHosts) {
                        failedServers.append(failedServer + " ");
                    }

                    throw new AssertionFailedException("currentLevelHosts.size() == 0 and unpartitionedHosts.size() == " + unpartitionedHosts.size() + ": cannot complete task for " + hostReceivingRequest + ", failed servers: " + failedServers.toString());
                } else {
                    // Anything that hasn't been partitioned is unfulfillable
                    unfulfillable.addAll(unpartitionedHosts);
                    unpartitionedHosts.clear();
                }
            } // If assertion failed

            // Sort so deterministic
            Collections.sort(currentLevelHosts);

            // Order so that use target hosts before non-target hosts. This may cut down
            // on one ore more unnecessary requests.
            List<String> hostsToProcess = new LinkedList();
            for (String nextHost : currentLevelHosts) {
                if (serverHostsToRequest.contains(nextHost)) {
                    // Beginning of list
                    hostsToProcess.add(0, nextHost);
                } else {
                    // End of list
                    hostsToProcess.add(nextHost);
                }
            }

            // Process next level
            for (String nextHost : currentLevelHosts) {

                // Identify the partition to which nextHost belongs
                Set<Host> partition = hostMap.get(nextHost);

                if (partition == null) {
                    FIND_PARTITION:
                    for (String key : hostMap.keySet()) {
                        Set<Host> nextCandidatePartition = hostMap.get(key);

                        for (Host nextCandidateHost : nextCandidatePartition) {
                            if (nextCandidateHost.host.equals(nextHost)) {
                                partition = nextCandidatePartition;
                                break FIND_PARTITION;
                            }
                        }
                    }
                }

                if (partition == null) {
                    throw new AssertionFailedException("Cound't find partition for " + nextHost + " (serverHost: " + hostReceivingRequest + ")");
                }

                ServerStatusTablePerspective nextInfo = ServerStatusUpdateProcess.getServerStatusTablePerspectiveForServer(nextHost);

                List<String> connectedHosts = new ArrayList(nextInfo.getConnectedCoreServerHosts().size());
                connectedHosts.addAll(nextInfo.getConnectedCoreServerHosts());

                // Order so deterministic
                Collections.sort(connectedHosts);

                // For each server connected to the next host, add to correct partition
                for (String connectedHost : connectedHosts) {

                    // Don't process same host multiple times to avoid
                    // loops in network logic.
                    if (alreadyHandledHosts.contains(connectedHost)) {
//                        System.out.println("DEBUG>          Already handled: " + connectedHost);
                        continue;
                    }
//                    System.out.println("DEBUG>          Handling: " + connectedHost);
                    alreadyHandledHosts.add(connectedHost);

                    if (unpartitionedHosts.contains(connectedHost)) {

                        partition.add(new Host(connectedHost, true, depth, nextHost));

                        nextLevelHosts.add(connectedHost);

                        unpartitionedHosts.remove(connectedHost);
                    } else {
                        partition.add(new Host(connectedHost, false, depth, nextHost));

                        nextLevelHosts.add(connectedHost);
                    }
                }
            } // For each server in 'current level' (already paritioned, seeing who they are connected to; recursion)

            // Set the state for the next iteration
            currentLevelHosts = nextLevelHosts;
            nextLevelHosts = new LinkedList();

        } // While there are unpartitioned hosts

        // Remove any partitions of size, since confusing
        Set<String> keysToRemove = new HashSet();
        for (String key : hostMap.keySet()) {
            if (hostMap.get(key).size() == 0) {
                keysToRemove.add(key);
            }
        }

        for (String keyToRemove : keysToRemove) {
            hostMap.remove(keyToRemove);
        }

        // Now build map with every server which is not part of request removed
        Map<String, Set<String>> requestMap = new HashMap();

        for (String key : hostMap.keySet()) {
            Set<Host> partition = hostMap.get(key);
            Set<String> requestPartition = new HashSet();

            for (Host host : partition) {
                if (host.isPartOfRequest) {
                    requestPartition.add(host.host);
                }
            }

            // Don't include empty partitions. Would be requesting nothing from a server!
            if (requestPartition.size() > 0) {
                requestMap.put(key, requestPartition);
            }
        }

        return new MultiServerRequestStrategy(hostReceivingRequest, hostMap, requestMap, unfulfillable, serverHostsToRequest, depth);
    }

    /**
     * <p>The host name for the server to receive the request, per this strategy object.</p>
     * @return
     */
    public String getHostReceivingRequest() {
        return hostReceivingRequest;
    }

    /**
     * <p>This is a map containing the following:</p>
     * <ul>
     *   <li>Key set: server hosts connected to the server being sent the request</li>
     *   <li>Values: the server hosts that will sent to each of the server hosts in the key set; i.e., the partitions</li>
     * </ul>
     * <p>Say we want set of hosts in this request, H = { h1, h2, h3, ..., h12 }, |H| = 12. If sending this request to an arbitrary host, h1, this map shows how this server will partition up these requests amongst its connected servers so that the request is filled as quickly as possible.</p>
     * <p>Say our key set is this map is {h2, h3, h4}, which are servers connected directly to h1. Then, hypothetically:</p>
     * <ul>
     *   <li>h2 -> { h2, h5, h6, h7 } in the map. That means h1 should send the request for { h2, h5, h6, h7 } to h2.</li>
     *   <li>h3 -> { h8, h9, h10 }. In that case, h3 is being used even though it isn't a member of the requested server set! This is done to keep the request efficient.</li>
     *   <li>h4 -> { h4, h11, h12 }</li>
     * </ul>
     * <p>All the requests were partitioned! Note that just because h2 -> { h2, h5, h6, h7 } doesn't mean that the server is connected to all four servers. The depth is a separate issue (and is available through this object); this is enough information to know where to send the requests next.</p>
     * @return
     */
    public Map<String, Set<String>> getPartitionsMap() {
        return partitionsMap;
    }

    /**
     * <p>If this request is sent to the server host listed, this is the 'depth' of the request across the network for the requested servers when modeled as a tree.</p>
     * <p>I.e., assume the server being sent the request is the root node. The branches are the servers to which it sends the request. If they satisfy the requests, then they are leaves; otherwise, they'll branche, etc. This number is the depth of this tree.</p>
     * <p>This is a rough approximation of the efficiency--the lower the depth, the more efficient (likely) to be.</p>
     * <p>Before sending a request to a particular server, can create a number of these summary objects and select the one with lowest depth. Other heuristics might be helpful as well.</p>
     * @return
     */
    public int getDepth() {
        return depth;
    }

    @Override()
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Starting server: " + this.getHostReceivingRequest() + "; depth: " + (this.getDepth() == INFINITE_DEPTH ? "infinite (cannot solve)" : this.getDepth()));
        for (String key : partitionsMap.keySet()) {
            Set<String> associatedServers = partitionsMap.get(key);
            StringBuffer set = new StringBuffer();
            set.append("{");
            int j = 0;
            for (String as : associatedServers) {
                set.append(as);
                if (j < associatedServers.size() - 1) {
                    set.append(", ");
                }
                j++;
            }
            set.append("}");

            buf.append("    * " + key + " -> " + set);
        }
        return buf.toString();
    }

    public void printStrategy(PrintStream out) {
        printStrategy(hostReceivingRequest, hostPartitionsMap, unfulfillable, out);
    }

    private static void printStrategy(String hostReceivingRequest, Map<String, Set<Host>> hostPartitionsMap, Set<String> unfulfillable, PrintStream out) {
        out.println("********************************************************************************");
        out.println("Strategy recipient -> " + hostReceivingRequest);
        out.println("********************************************************************************");
        out.println();
        out.println("* " + hostPartitionsMap.keySet() + " connection(s):");

        // For each connected host, recursively print out the requests
        for (String connectedHost : hostPartitionsMap.keySet()) {
            out.println("   - " + connectedHost);
            Set<Host> partition = hostPartitionsMap.get(connectedHost);
            recursivelyPrintPartition(partition, connectedHost, out);
        }
        out.println();
        out.println("* " + unfulfillable.size() + " unfulfillable request(s)");
        for (String unfulfillableHost : unfulfillable) {
            out.println("   - " + unfulfillableHost);
        }

    }

    /**
     * <p>Helper method for printStrategy to print out things recursively with useful format.</p>
     * @param partition
     * @param host
     * @param out
     */
    private static void recursivelyPrintPartition(Set<Host> partition, String host, PrintStream out) {
        for (Host h : partition) {
            if (h.parentHost != null && h.parentHost.equals(host)) {
                StringBuffer padding = new StringBuffer();
                for (int i = 0; i < h.level; i++) {
                    padding.append("  ");
                }
                out.println(padding + "   - " + h.host);
                recursivelyPrintPartition(partition, h.host, out);
            }
        }
    }

    /**
     * <p>All the hosts that are unfulfillable. Right now, this means because the server is offline. There might be additional future conditions.</p>
     * @return
     */
    public Set<String> getUnfulfillableHosts() {
        return unfulfillable;
    }

    /**
     * <p>Wrapper used for creating a MultiServerRequestStrategy.</p>
     */
    private static class Host {

        final String host;
        final boolean isPartOfRequest;
        final int level;
        final String parentHost;

        Host(String host, boolean isPartOfRequest, int level, String parentHost) {
            this.host = host;
            this.isPartOfRequest = isPartOfRequest;
            this.level = level;
            this.parentHost = parentHost;
        }
    }

    @Override()
    public int hashCode() {
        // As it stands, unlikely to avoid lots of collision. However, these strategy objects
        // are not really stored for long, so shouldn't matter.
        //
        // If large collection, will slow down a little.
        int code = hostReceivingRequest.hashCode();
        return code;
    }

    /**
     * <p>Test that strategy objects are similar. Don't have to be exactly same, but must have the following in common:</p>
     * <ul>
     *   <li>Same host to contact</li>
     *   <li>Same depth</li>
     *   <li>Same unfulfillable set</li>
     *   <li>Same target sets</li>
     * </ul>
     * @param other
     * @return
     */
    public boolean equivalent(MultiServerRequestStrategy other) {
        // Test: same hostReceivingRequest
        if (!other.hostReceivingRequest.equals(this.hostReceivingRequest)) {
            return false;
        }

        // Test: same depth
        if (other.getDepth() != this.getDepth()) {
            return false;
        }

        // Test: same unfulfillable set
        if (other.getUnfulfillableHosts().size() != this.getUnfulfillableHosts().size()) {
            return false;
        }
        for (String s : other.getUnfulfillableHosts()) {
            if (!this.getUnfulfillableHosts().contains(s)) {
                return false;
            }
        }

        // Test: same target sets
        Set<String> thisTargetSet = new HashSet();
        Set<String> otherTargetSet = new HashSet();

        for (String key : other.getPartitionsMap().keySet()) {
            otherTargetSet.addAll(other.getPartitionsMap().get(key));
        }

        for (String key : this.getPartitionsMap().keySet()) {
            thisTargetSet.addAll(this.getPartitionsMap().get(key));
        }

        // Test: same unfulfillable set
        if (otherTargetSet.size() != thisTargetSet.size()) {
            return false;
        }
        for (String s : otherTargetSet) {
            if (!thisTargetSet.contains(s)) {
                return false;
            }
        }

        // If gets here, equivalent
        return true;
    }

    @Override()
    public boolean equals(Object o) {
        if (o instanceof MultiServerRequestStrategy) {
            MultiServerRequestStrategy other = (MultiServerRequestStrategy) o;

            // Equality is stochastic. Equivalence is good enough for us.
            return equivalent(other);
        }

        // If not of type MultiServerRequestStrategy, then not equal
        return false;
    }

    /**
     * <p>Returns true if request contains a particular host.</p>
     * @param host
     * @return
     */
    public boolean requestContains(String host) {
        return this.hostsToIncludeInRequest.contains(host);
    }
}