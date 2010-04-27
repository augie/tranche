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
package org.tranche.flatfile;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.commons.Tertiary;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.commons.DebuggableThread;
import org.tranche.commons.TextUtil;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.exceptions.NoMatchingServersException;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.hash.span.HashSpanCollection;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.MultiServerRequestStrategy;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;
import org.tranche.commons.ThreadUtil;

/**
 * <p>Thread that monitors for target hash spans and starts moving everything off this server not in the target hash spans.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class TargetHashSpanThread extends DebuggableThread {

    private boolean closed = false;
    private final FlatFileTrancheServer ffts;
    private int minRequiredCopies = ConfigKeys.DEFAULT_NUMBER_TOTAL_REPS_IN_HASH_SPAN_REQUIRED_DELETE;
    private int timeToSleep = 5000;

    /**
     * 
     * @param ffts
     */
    public TargetHashSpanThread(FlatFileTrancheServer ffts) {
        setName("Target Hash Span Thread");
        setPriority(Thread.MIN_PRIORITY);
        this.ffts = ffts;
    }

    /**
     *
     */
    @Override()
    public void run() {
        RUN:
        while (!shouldStop()) {
            try {
                Configuration config = ffts.getConfiguration(false);

                // If continued, make sure set value to false
                try {
                    config.setValue(ConfigKeys.TARGET_HASH_SPAN_THREAD_WORKING, String.valueOf(false));
                } catch (Exception e) { /* nope */ }

                try {
                    config.removeKeyValuePair(ConfigKeys.TARGET_HASH_SPAN_THREAD_MESSAGE);
                } catch (Exception e) { /* nope */ }

                // they are the same
                if (HashSpanCollection.areEqual(config.getHashSpans(), config.getTargetHashSpans())) {
                    debugOut("Hash spans and target hash spans are equivalent.");
                    continue;
                }

                // check for larger target
                if (config.getHashSpans().size() == 0 && config.getTargetHashSpans().size() > 0) {
                    debugOut("There are 0 hash spans and > 0 target hash spans.");
                    config.setHashSpans(new HashSet<HashSpan>(config.getTargetHashSpans()));
                    ffts.setConfiguration(config);
                    continue;
                }
                // if true, the target hash spans encompass the hash spans
                List<HashSpan> allHashSpans = new LinkedList<HashSpan>();
                allHashSpans.addAll(config.getHashSpans());
                allHashSpans.addAll(config.getTargetHashSpans());
                if (HashSpanCollection.areEqual(allHashSpans, config.getTargetHashSpans())) {
                    debugOut("Target hash spans encompass the hash spans.");
                    config.setHashSpans(new HashSet<HashSpan>(config.getTargetHashSpans()));
                    ffts.setConfiguration(config);
                    continue;
                }

                // must have authorization to move off data
                if (ffts.getAuthCert() == null || ffts.getAuthPrivateKey() == null) {
                    debugOut("Server not authorized to set data on the repository.");
                    continue;
                }

                if (!TestUtil.isTesting()) {
                    debugOut("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_");
                    debugOut(" Found " + this.ffts.getConfiguration().getTargetHashSpans().size() + " target hash span(s) at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
                    for (HashSpan targetHashSpan : this.ffts.getConfiguration().getTargetHashSpans()) {
                        debugOut("    Start: " + targetHashSpan.getFirst());
                        debugOut("    End:   " + targetHashSpan.getLast());
                    }
                    debugOut(" Found " + this.ffts.getConfiguration().getHashSpans().size() + " hash span(s) at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
                    for (HashSpan hashSpan : this.ffts.getConfiguration().getHashSpans()) {
                        debugOut("    Start: " + hashSpan.getFirst());
                        debugOut("    End:   " + hashSpan.getLast());
                    }
                    debugOut("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_");
                }

                // Set value in FFTS config so users know we are using target hash span thread right now
                try {
                    config.setValue(ConfigKeys.TARGET_HASH_SPAN_THREAD_WORKING, String.valueOf(true));
                } catch (Exception e) { /* nope */ }

                try {
                    ffts.getConfiguration(false).setValue(ConfigKeys.TARGET_HASH_SPAN_THREAD_MESSAGE, "Started (" + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()) + ")");
                } catch (Exception e) { /* nope */ }

                final Set<HashSpan> startingTargetHashSpans = new HashSet();
                for (HashSpan hs : ffts.getConfiguration(false).getTargetHashSpans()) {
                    startingTargetHashSpans.add(hs);
                }

                // Fail up to three times, then throw exception!
                int maxRetries = 3;
                long injected = -1;

                DATA:
                for (int i = 1; i <= maxRetries; i++) {

                    // Update required copies
                    try {
                        minRequiredCopies = Integer.parseInt(this.ffts.getConfiguration().getValue(ConfigKeys.HASHSPANFIX_NUMBER_REPS_IN_HASH_SPAN_REQUIRED_DELETE));
                    } catch (Exception nope) { /* continue */ }

                    try {
                        injected = injectAndRemoveDataChunks(startingTargetHashSpans);
                        break DATA;
                    } catch (NoMatchingServersException ne) {
                        // Cannot find an appropriate hash span for chunk on network. Continue and hope that changes later
                        debugOut("Could not find a hash span on the network at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " for data chunk: " + ne.getMessage());
                        ne.printStackTrace(System.err);
                        continue RUN;
                    } catch (Exception e) {
                        if (i == maxRetries) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                // If target hash spans were modified again, just continue outside loop... this will verify if need
                // to run again
                if (!HashSpanCollection.areEqual(startingTargetHashSpans, ffts.getConfiguration(false).getTargetHashSpans())) {
                    debugOut("WARNING: Restarting since target hash spans have changed.");
                    continue RUN;
                }

                // Make sure should still be running
                if (shouldStop()) {
                    debugOut("WARNING: Stopping since a terminal condition has been met.");
                    break RUN;
                }

                if (!TestUtil.isTesting()) {
                    debugOut("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_");
                    debugOut(" STEP 1 of 3: Finished injecting data (total: " + injected + ") at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
                    debugOut("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_");
                }

                injected = -1;

                META:
                for (int i = 1; i <= maxRetries; i++) {

                    // Update required copies
                    try {
                        minRequiredCopies = Integer.parseInt(this.ffts.getConfiguration().getValue(ConfigKeys.HASHSPANFIX_NUMBER_REPS_IN_HASH_SPAN_REQUIRED_DELETE));
                    } catch (Exception nope) { /* continue */ }

                    try {
                        injected = injectAndRemoveMetaDataChunks(startingTargetHashSpans);
                        break META;
                    } catch (NoMatchingServersException ne) {
                        // Cannot find an appropriate hash span for chunk on network. Continue and hope that changes later
                        debugOut("Could not find a hash span on the network at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " for meta data chunk: " + ne.getMessage());
                        ne.printStackTrace(System.err);
                        continue RUN;
                    } catch (Exception e) {
                        if (i == maxRetries) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                // If target hash spans were modified again, just continue outside loop... this will verify if need
                // to run again
                if (!HashSpanCollection.areEqual(startingTargetHashSpans, ffts.getConfiguration(false).getTargetHashSpans())) {
                    debugOut("WARNING: Restarting since target hash spans have changed.");
                    continue RUN;
                }

                // Make sure should still be running
                if (shouldStop()) {
                    debugOut("WARNIGN: Stopping since a terminal condition has been met.");
                    break RUN;
                }

                if (!TestUtil.isTesting()) {
                    debugOut("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_");
                    debugOut(" STEP 2 of 3: Finished injecting meta data (total: " + injected + ") at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
                    debugOut("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_");
                }

                // Remove the target hash span and update the hash span
                config = this.ffts.getConfiguration(false);

                Set<HashSpan> newHashSpans = new HashSet();

                for (HashSpan targetHashSpan : config.getTargetHashSpans()) {
                    newHashSpans.add(targetHashSpan);
                }

                try {
                    config.setHashSpans(newHashSpans);
                    ffts.setConfiguration(config);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if (!TestUtil.isTesting()) {
                    debugOut("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_");
                    debugOut(" STEP 3 of 3: Updated configuration at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()));
                    debugOut("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_");
                }
            } catch (Exception e) {
                debugErr(e);
            } finally {
                try {
                    ffts.getConfiguration(false).setValue(ConfigKeys.TARGET_HASH_SPAN_THREAD_WORKING, String.valueOf(false));
                } catch (Exception e) { /* nope */ }
                try {
                    ffts.getConfiguration(false).removeKeyValuePair(ConfigKeys.TARGET_HASH_SPAN_THREAD_MESSAGE);
                } catch (Exception e) { /* nope */ }
                ThreadUtil.sleep(getTimeToSleep());
            }
        }
    }

    /**
     * 
     */
    public void close() {
        closed = true;
    }

    /**
     * <p>Helper method to manage logic of whether should continue.</p>
     * @return
     */
    private boolean shouldStop() {
        return closed || (TestUtil.isTesting() && !TestUtil.isTestingTargetHashSpan());
    }

    /**
     *
     * @param startingTargetHashSpans
     * @return
     * @throws java.lang.Exception
     */
    private long injectAndRemoveDataChunks(Set<HashSpan> startingTargetHashSpans) throws Exception {
        return injectAndRemoveChunks(false, startingTargetHashSpans);
    }

    /**
     *
     * @param startingTargetHashSpans
     * @return
     * @throws java.lang.Exception
     */
    private long injectAndRemoveMetaDataChunks(Set<HashSpan> startingTargetHashSpans) throws Exception {
        return injectAndRemoveChunks(true, startingTargetHashSpans);
    }

    /**
     * 
     * @param isMetaData
     * @param startingTargetHashSpans
     * @return
     * @throws java.lang.Exception
     */
    private long injectAndRemoveChunks(boolean isMetaData, Set<HashSpan> startingTargetHashSpans) throws Exception {

        long injected = 0;

        final BigInteger batchSize = BigInteger.valueOf(25);
        BigInteger offset = BigInteger.ZERO;

        BigHash[] hashes = null;

        if (isMetaData) {
            hashes = this.ffts.getMetaDataHashes(offset, batchSize);
        } else {
            hashes = this.ffts.getDataHashes(offset, batchSize);
        }

        while (hashes != null && hashes.length > 0) {

            try {
                ffts.getConfiguration(false).setValue(ConfigKeys.TARGET_HASH_SPAN_THREAD_MESSAGE, "Injecting " + offset.toString() + " through " + offset.add(batchSize).toString() + " of " + (isMetaData ? "meta data" : "data") + " chunks (" + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()) + ")");
            } catch (Exception e) { /* nope */ }

            if (!HashSpanCollection.areEqual(startingTargetHashSpans, ffts.getConfiguration(false).getTargetHashSpans())) {
                return injected;
            }
            if (shouldStop()) {
                return injected;
            }

            HASHES:
            for (BigHash hash : hashes) {

                boolean targetContains = false;
                CHECK_TARGET_HASH_SPANS:
                for (HashSpan targetHashSpan : this.ffts.getConfiguration(false).getTargetHashSpans()) {
                    if (targetHashSpan.contains(hash)) {
                        targetContains = true;
                        break CHECK_TARGET_HASH_SPANS;
                    }
                }

                // If target hash spans contain, just continue
                if (targetContains) {
                    continue;
                }

                byte[] chunk = null;
                if (isMetaData) {

                    // If the hash set is wrong, continue
                    if (!IOUtil.hasMetaData(ffts, hash)) {
                        continue HASHES;
                    }

                    chunk = ffts.getDataBlockUtil().getMetaData(hash);
                } else {

                    // If the hash set is wrong, continue
                    if (!IOUtil.hasData(ffts, hash)) {
                        continue HASHES;
                    }

                    chunk = ffts.getDataBlockUtil().getData(hash);
                }

                if (chunk == null) {
                    throw new AssertionFailedException(String.valueOf(isMetaData ? "Meta data" : "Data") + " chunk not found, though server says it has: " + hash);
                }

                // Find all the servers that should have, checking any available
                // hash span(s) or target hash span
                Set<String> serversShouldHave = new HashSet();
                for (StatusTableRow row : NetworkUtil.getStatus().getRows()) {

                    // Make sure server is online and is core
                    if (row.isOnline() && row.isCore() && row.isWritable()) {

                        boolean contains = false;

                        // When writing, we only care about target hash span!
                        NEXT_TARGET_SERVER_HASH_SPANS:
                        for (HashSpan hs : row.getTargetHashSpans()) {
                            if (hs.contains(hash)) {
                                contains = true;
                                break NEXT_TARGET_SERVER_HASH_SPANS;
                            }
                        }

                        if (contains) {
                            if (!row.getHost().equals(this.ffts.getHost())) {
                                serversShouldHave.add(row.getHost());
                            }
                        }
                    }
                }

                // If couldn't find server that should have hash
                if (serversShouldHave.size() == 0) {
                    throw new NoMatchingServersException();
                }

                // Set the chunk to these servers propagated
                Collection<MultiServerRequestStrategy> bestStrategies = MultiServerRequestStrategy.findFastestStrategiesUsingConnectedCoreServers(serversShouldHave, Tertiary.DONT_CARE, Tertiary.TRUE);

                // Assert more than one. Should be since already asserted that at least one server should support chunk.
                if (bestStrategies.size() == 0) {
                    throw new AssertionFailedException("Should be matching strategies, but weren't any.");
                }

                // Just pick any strategy from set of best strategies
                MultiServerRequestStrategy strategy = bestStrategies.toArray(new MultiServerRequestStrategy[0])[0];

                TrancheServer server = ConnectionUtil.getHost(strategy.getHostReceivingRequest());

                // Assert connection exists already
                if (server == null) {
                    throw new AssertionFailedException(strategy.getHostReceivingRequest() + " is supposedly a connected server, but ConnectionUtil returned null when requesting connection.");
                }

                String[] serversShouldHaveArr = serversShouldHave.toArray(new String[0]);

                if (isMetaData) {

                    PropagationReturnWrapper returnWrapper = IOUtil.setMetaData(server, ffts.getAuthCert(), ffts.getAuthPrivateKey(), true, hash, chunk, serversShouldHaveArr);

                    Set<PropagationExceptionWrapper> unacceptableExceptions = new HashSet();

                    // Interpolate how many servers have the hash
                    int nowHasCount = serversShouldHaveArr.length - returnWrapper.getErrors().size();
                    if (nowHasCount < 0) {
                        nowHasCount = 0;
                    }

                    // Verify that each exception is acceptable.
                    for (PropagationExceptionWrapper pew : returnWrapper.getErrors()) {
                        if (!isExceptionAcceptable(pew)) {
                            unacceptableExceptions.add(pew);
                        } else {
                            // Acceptable exceptions imply server has hash
                            nowHasCount++;
                        }
                    }

                    // Make sure there are enough copies or fail.
                    if (nowHasCount < this.minRequiredCopies) {

                        debugOut("Found total of " + unacceptableExceptions.size() + " exception while trying to inject chunk to network so can delete to reach target hash span at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()) + ". (Note: Failed injection. Insufficient copies on network. Requires: " + this.minRequiredCopies + ", instead: " + nowHasCount + " for: " + hash + ")");
                        for (PropagationExceptionWrapper pew : unacceptableExceptions) {
                            debugOut("    * Found unacceptable " + pew.exception.getClass().getSimpleName() + ": " + pew.exception.getMessage());
                        }

                        throw new Exception("Insufficient copies on network. Requires: " + this.minRequiredCopies + ", instead: " + nowHasCount + " for: " + hash);
                    }

                    this.ffts.getDataBlockUtil().deleteMetaData(hash, "Deleting to reach target hash span");

                    injected++;
                } else {
                    PropagationReturnWrapper returnWrapper = IOUtil.setData(server, ffts.getAuthCert(), ffts.getAuthPrivateKey(), hash, chunk, serversShouldHaveArr);

                    for (PropagationExceptionWrapper pew : returnWrapper.getErrors()) {
                        debugOut("    - " + pew.exception.getClass().getSimpleName() + " <" + pew.host + ">: " + pew.exception.getMessage());
                    }

                    Set<PropagationExceptionWrapper> unacceptableExceptions = new HashSet();

                    // Interpolate how many servers have the hash
                    int nowHasCount = serversShouldHaveArr.length - returnWrapper.getErrors().size();
                    if (nowHasCount < 0) {
                        nowHasCount = 0;
                    }

                    // Verify that each exception is acceptable.
                    for (PropagationExceptionWrapper pew : returnWrapper.getErrors()) {
                        if (!isExceptionAcceptable(pew)) {
                            unacceptableExceptions.add(pew);
                        } else {
                            // Acceptable exceptions imply server has hash
                            nowHasCount++;
                        }
                    }

                    // Make sure there are enough copies or fail.
                    if (nowHasCount < this.minRequiredCopies) {
                        debugOut("Found total of " + unacceptableExceptions.size() + " exception while trying to inject chunk to network so can delete to reach target hash span at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()) + ". (Note: Failed injection. Insufficient copies on network. Requires: " + this.minRequiredCopies + ", instead: " + nowHasCount + " for: " + hash + ")");
                        for (PropagationExceptionWrapper pew : unacceptableExceptions) {
                            debugOut("    * Found unacceptable " + pew.exception.getClass().getSimpleName() + ": " + pew.exception.getMessage());
                        }
                        throw new Exception("Insufficient copies on network. Requires: " + this.minRequiredCopies + ", instead: " + nowHasCount + " for: " + hash);
                    }

                    this.ffts.getDataBlockUtil().deleteData(hash, "Deleting to reach target hash span");

                    injected++;
                }
            }

            // Update batch of hashes
            offset = offset.add(batchSize);

            if (isMetaData) {
                hashes = this.ffts.getDataHashes(offset, batchSize);
            } else {
                hashes = this.ffts.getMetaDataHashes(offset, batchSize);
            }
        }

        return injected;
    }

    /**
     * 
     * @param pew
     * @return
     */
    private boolean isExceptionAcceptable(PropagationExceptionWrapper pew) {
        if (pew.exception.getClass().getSimpleName().equals("ChunkAlreadyExistsSecurityException")) {
            return true;
        }
        return false;
    }

    /**
     * <p>Time, in milliseconds, to sleep when no target hash span found.</p>
     * @return
     */
    public int getTimeToSleep() {
        return timeToSleep;
    }

    /**
     * <p>Time, in milliseconds, to sleep when no target hash span found.</p>
     * @param timeToSleep
     */
    public void setTimeToSleep(int timeToSleep) {
        this.timeToSleep = timeToSleep;
    }
}
