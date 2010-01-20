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
package org.tranche.scripts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import org.tranche.ConfigureTranche;
import org.tranche.exceptions.TodoException;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.scripts.ScriptsUtil.ChunkType;
import org.tranche.util.IOUtil;
import org.tranche.time.TimeUtil;
import org.tranche.util.Text;
import org.tranche.users.UserZipFile;

/**
 * <p>Injects data from a data drive to Tranche network.</p>
 * @author Bryan E. Smith
 */
public class InjectDataIntoTrancheNetwork {

    /**
     * Size of batch to handle at a time
     */
    final static int hashBatchSize = 500;
    /**
     * Keep track of progress for handling chunks
     */
    static int dataCount = 0,  metaCount = 0;
    /**
     * Keep some over all stats for user's knowledge
     */
    static int noReplications = 0,  totalChunksInjected = 0;
    /**
     * Calculate totals so progress information can be converted to percentages
     */
    static int dataChunkTotal = 0,  metaChunkTotal = 0;
    /**
     * Queue up chunks without sufficient replications
     */
    static final ArrayBlockingQueue<ChunkToInject> chunksToInjectQueue = new ArrayBlockingQueue(10000);
    /**
     * Producer
     */
    static final ChunkCheckThread[] dataChunkCheckThreads = new ChunkCheckThread[3];
    /**
     * Producer
     */
    static final ChunkCheckThread[] metaChunkCheckThreads = new ChunkCheckThread[3];
    /**
     * Consumer
     */
    static final ChunkInjectionThread[] injectionThreads = new ChunkInjectionThread[3];
    /**
     * List of servers to use
     */
    static final List<String> serversToUse = new ArrayList<String>();
    /**
     * Only allow one to run per JVM at a time
     */
    static private boolean isRunning = false;

    /**
     * <p>Injects data from a data drive to Tranche network. Expects DataBlock-format Tranche data chunks.</p>
     * <p>Will put chunks on random servers. Will report any injections, up to three copies.</p>
     *
     * @param args Expecting following runtime arguments in this order:
     * <ol>
     *   <li>Path to your network configuration file</li>
     *   <li>Path to data directory with chunks to add</li>
     *   <li>Path to user file (zip, encrypted)</li>
     *   <li>Passphrase for user file</li>
     *   <li>Path to output file</li>
     *   <li>Path to file to log upload exceptions</li>
     *   <li>Path to file for replication failures</li>
     * </ol>
     *
     * <p>The following are optional arguments, appearing after the aforementioned required parameters:</p>
     * <ul>
     *  <li>ban:tranche_url To ban a specific server. E.g., ban:tranche://141.214.182.211:443</li>
     *  <li>use:tranche_url To limit use to 1+ servers. E.g., use:tranche://141.214.182.211:443 use:tranche://141.214.182.211:443 will cause the script to only use those two servers.</li>
     *  <li>ddc:/path/to/additional/data/dir To add additional data directories. ddc is a reference to DataDirectoryConfiguration</li>
     * </ul>
     */
    public static void main(String[] args) {

        if (isRunning) {
            System.err.println("An instance is already running in this JVM. Bailing.");
            printUsage();
            System.exit(1);
        }

        // configure Tranche
        ConfigureTranche.load(args);

        File dataRoot = null, userFile = null, outputFile = null, chunkInjectEsceptionFile = null, chunkInjectFailureFile = null;
        UserZipFile uzf = null;

        try {

            if (args.length < 7) {
                throw new Exception("Does not contain required parameters. Expecting minimum of 6, found " + args.length);
            }

            isRunning = true;

            dataRoot = new File(args[1]);
            if (dataRoot == null || !dataRoot.exists()) {
                throw new Exception("Problem loading data root, cannot find: " + dataRoot.getAbsolutePath());
            }

            userFile = new File(args[2]);
            if (userFile == null || !userFile.exists()) {
                throw new Exception("Problem loading user file, cannot find: " + userFile.getAbsolutePath());
            }

            String passphrase = args[3].trim();

            outputFile = new File(args[4]);
            outputFile.createNewFile();
            if (outputFile == null || !outputFile.exists()) {
                throw new Exception("Problem loading output file, cannot find: " + outputFile.getAbsolutePath());
            }

            chunkInjectEsceptionFile = new File(args[5]);
            chunkInjectEsceptionFile.createNewFile();
            if (chunkInjectEsceptionFile == null || !chunkInjectEsceptionFile.exists()) {
                throw new Exception("Problem loading output injection chunk exception file, cannot find: " + chunkInjectEsceptionFile.getAbsolutePath());
            }

            chunkInjectFailureFile = new File(args[6]);
            chunkInjectFailureFile.createNewFile();
            if (chunkInjectFailureFile == null || !chunkInjectFailureFile.exists()) {
                throw new Exception("Problem loading output injection chunk failure file, cannot find: " + chunkInjectFailureFile.getAbsolutePath());
            }

            uzf = new UserZipFile(userFile);
            uzf.setPassphrase(passphrase);

            final List<File> additionalDataDirectories = new ArrayList<File>();

            /**
             * Look for optional runtime arguments
             */
            if (args.length > 6) {

                System.out.println("");
                System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
                System.out.println("=-=-=     STARTING OPTIONAL PARAMETERS     =-=-=");
                System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
                System.out.println("");

                /**
                 * Look for optional arg ban:url, which will ban the server
                 */
                for (int i = 0; i < args.length; i++) {
                    String nextArg = args[i].trim();

                    if (nextArg.toLowerCase().startsWith("use:")) {
                        String url = nextArg.substring(4);
                        System.out.println("- ADDING SERVER TO USE: " + url);
                        serversToUse.add(url);
                    }

                    if (nextArg.toLowerCase().startsWith("ddc:")) {
                        String path = nextArg.substring(4);
                        System.out.println("- ADDING ANOTHER DDC PATH: " + path);
                        additionalDataDirectories.add(new File(path));
                    }
                }
            } // If optional args

            System.out.println("");
            System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
            System.out.println("");

            injectDataToNetwork(dataRoot, uzf, outputFile, chunkInjectEsceptionFile, chunkInjectFailureFile, additionalDataDirectories);
        } catch (Exception ex) {
            System.err.println("Problem starting script: " + ex.getMessage());
            ex.printStackTrace(System.err);
            printUsage();
            System.exit(1);
        } finally {
            isRunning = false;
        }

        System.exit(0);
    }

    /**
     * <p>Helper method to inject data to the Tranche network.</p>
     * @param directory
     * @param user
     * @param outputFile
     * @param chunkInjectExceptionFile
     * @param chunkInjectFailureFile
     * @param additionalDataDirectories
     * @throws java.lang.Exception
     */
    private static void injectDataToNetwork(File directory, UserZipFile user, File outputFile, File chunkInjectExceptionFile, File chunkInjectFailureFile, List<File> additionalDataDirectories) throws Exception {
        System.out.println("injectDataToNetwork(" + directory.getAbsolutePath() + "," + user.getFile().getAbsolutePath() + ")");

        final long start = TimeUtil.getTrancheTimestamp();

        FlatFileTrancheServer ffts = null;

        BufferedWriter outputFileWriter = null, chunkExceptionFileWriter = null, chunkFailureFileWriter = null;
        try {
            // Create writer. It's clobberin' time!
            outputFileWriter = new BufferedWriter(new FileWriter(outputFile, true));
            chunkExceptionFileWriter = new BufferedWriter(new FileWriter(chunkInjectExceptionFile, false));

            chunkExceptionFileWriter.write("\"Data or meta\",\"Required replications count\",\"Found replications count\",\"URL of server for failed replication\",\"Date\",\"Exception message\",\"Chunk hash\"");
            chunkExceptionFileWriter.newLine();
            chunkExceptionFileWriter.flush();

            chunkFailureFileWriter = new BufferedWriter(new FileWriter(chunkInjectFailureFile, false));

            // "Expected replications", "Found replications", "Type", "Hash"
            chunkFailureFileWriter.write("\"Expected replications\", \"Found replications\", \"Data or meta\", \"Chunk hash\"");
            chunkFailureFileWriter.newLine();
            chunkFailureFileWriter.flush();

            // Wrap data directory in FlatFileTrancheServer. Let it do
            // all the work.
            ffts = new FlatFileTrancheServer(directory.getAbsolutePath());
            user.setFlags(UserZipFile.CAN_GET_CONFIGURATION | UserZipFile.CAN_SET_CONFIGURATION | UserZipFile.CAN_SET_DATA | UserZipFile.CAN_SET_META_DATA);
            ffts.getConfiguration().getUsers().add(user);

            // Add all additional data directory configurations!
            if (additionalDataDirectories.size() > 0) {
                for (File additionalDataDirectory : additionalDataDirectories) {
                    DataDirectoryConfiguration nextDDC = new DataDirectoryConfiguration(additionalDataDirectory.getAbsolutePath(), Long.MAX_VALUE);
                    ffts.getConfiguration().getDataDirectories().add(nextDDC);
                }

                // Close and re-opened so information is saved
                IOUtil.setConfiguration(ffts, ffts.getConfiguration(), user.getCertificate(), user.getPrivateKey());
                ffts.close();
                ffts = new FlatFileTrancheServer(directory.getAbsolutePath());
            }

            final long startupStart = TimeUtil.getTrancheTimestamp();

            System.out.println(">>> Loading servers.");

            System.out.println(">>> Loading data from disk.");
            ffts.waitToLoadExistingDataBlocks();

            System.out.println("Finished loading servers and data, took " + Text.getPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp() - startupStart));
            System.out.println();
            System.out.println("Writing output to file: " + outputFile.getAbsolutePath());

            // Get data chunk count
            {
                System.out.println(">>> Getting data chunk count...");
                BigInteger offset = BigInteger.ZERO;
                BigInteger limit = BigInteger.valueOf(hashBatchSize);

                dataChunkTotal = ffts.getDataBlockUtil().dataHashes.size();
//                // Start with server's known data chunks.
//                for (BigHash[] chunks = ffts.getDataHashes(offset, limit); chunks.length > 0; chunks = ffts.getDataHashes(offset, limit)) {
//                    dataChunkTotal+=chunks.length;
//                }
                System.out.println("... total of " + dataChunkTotal + " chunks loaded.");
            }

            // Get meta chunk count
            {
                BigInteger offset = BigInteger.ZERO;
                BigInteger limit = BigInteger.valueOf(hashBatchSize);
                System.out.println(">>> Getting meta data chunk count...");

                metaChunkTotal = ffts.getDataBlockUtil().metaDataHashes.size();
                // Start with server's known meta data chunks.
//                for (BigHash[] chunks = ffts.getMetaDataHashes(offset, limit); chunks.length > 0; chunks = ffts.getMetaDataHashes(offset, limit)) {
//                    metaChunkTotal+=chunks.length;
//                }
                System.out.println("... total of " + metaChunkTotal + " meta data chunks loaded.");
            }

            // Fire up producers
            for (int i = 0; i < dataChunkCheckThreads.length; i++) {
                dataChunkCheckThreads[i] = new ChunkCheckThread(ffts, user, false, outputFileWriter, i, dataChunkCheckThreads.length);
                dataChunkCheckThreads[i].start();
            }

            for (int i = 0; i < metaChunkCheckThreads.length; i++) {
                metaChunkCheckThreads[i] = new ChunkCheckThread(ffts, user, true, outputFileWriter, i, metaChunkCheckThreads.length);
                metaChunkCheckThreads[i].start();
            }

            // Fire up consumers
            for (int i = 0; i < injectionThreads.length; i++) {
                injectionThreads[i] = new ChunkInjectionThread(ffts, user, chunksToInjectQueue, chunkExceptionFileWriter, chunkFailureFileWriter);
                injectionThreads[i].start();
            }

            try {
                // Let everything start!
                Thread.sleep(5000);
            } catch (InterruptedException ie) { /* nope */ }

            // Join all the consumers
            for (int i = 0; i < dataChunkCheckThreads.length; i++) {
                while (dataChunkCheckThreads[i].isAlive()) {
                    dataChunkCheckThreads[i].join(1000);
                }
            }
            for (int i = 0; i < metaChunkCheckThreads.length; i++) {
                while (metaChunkCheckThreads[i].isAlive()) {
                    metaChunkCheckThreads[i].join(1000);
                }
            }

            System.out.println("Waiting for injection threads to complete chunk uploads at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));

            // Since all the consumers are joined, tell the producers to stop
            for (int i = 0; i < injectionThreads.length; i++) {
                injectionThreads[i].setStopped(true);
            }

            for (int i = 0; i < injectionThreads.length; i++) {
                while (injectionThreads[i].isAlive()) {
                    injectionThreads[i].join(1000);
                }
            }

        } finally {
            if (ffts != null) {
                IOUtil.safeClose(ffts);
            }

            if (outputFileWriter != null) {
                IOUtil.safeClose(outputFileWriter);
            }

            System.out.println("\n\n=============== SUMMARY ===============");
            System.out.println("* Tool ran for: " + Text.getPrettyEllapsedTimeString(TimeUtil.getTrancheTimestamp() - start));
            System.out.println("* Found total " + dataCount + " data chunks and " + metaCount + " meta chunks on disk");
            System.out.println("* Found " + noReplications + " chunks on disk with no replications on network");
            System.out.println("* Injected total of " + totalChunksInjected + " chunk replications to network (each chunks could account for up to three replications)");
            System.out.println();
            System.out.println("Output saved to file: " + outputFile.getAbsolutePath());
            System.out.println();

            reset();
        }
    }

    /**
     * <p>Reset the resources used by this script.</p>
     */
    private static void reset() {
        dataCount = 0;
        metaCount = 0;
        noReplications = 0;
        totalChunksInjected = 0;
        dataChunkTotal = 0;
        metaChunkTotal = 0;
    }

    /**
     * <p>Print out the usage.</p>
     */
    private static void printUsage() {
        System.err.println("");
        System.err.println("USAGE:");
        System.err.println("  java -jar ThisJar.jar <path to data root> <path to user zip file> <user zip file passphrase> <path to output file> <path to failed chunks output file> [<optional>, ...]");
        System.err.println("");
        System.err.println("Required parameters:");
        System.err.println("  - <path to data root>: The directory containing a Tranche data directory (i.e., the parent of a \"data\" directory.");
        System.err.println("  - <path to user zip file>: Full path to a Tranche user zip file.");
        System.err.println("  - <user zip file passphrase>: The passphrase to the above user zip file.");
        System.err.println("  - <path to output file>: Full path to a file containing replication information about every chunk. This can get quite large -- perhaps hundreds of MB!");
        System.err.println("  - <path to chunks exception output file>: Full path to a new CSV file for any exceptions replications information. Must not already exist.");
        System.err.println("  - <path to chunks failed to meet minimum reps>: Full path to a new CSV file for any chunks that were not replication enough times");
        System.err.println("");
        System.err.println("Optional parameters:");
        System.err.println("  - ban:tranche_url To ban a specific server. E.g., ban:tranche://141.214.182.211:443");
        System.err.println("  - use:tranche_url To limit use to 1+ servers. E.g., use:tranche://141.214.182.211:443 use:tranche://141.214.182.211:443 will cause the script to only use those two servers.");
        System.err.println("  - ddc:/path/to/additional/data/dir To add additional data directories. \"ddc\" is a reference to DataDirectoryConfiguration.");
        System.err.println("");
        System.err.flush();
    }

    /**
     * <p>Thread to check for chunks.</p>
     */
    static class ChunkCheckThread extends Thread {

        private final FlatFileTrancheServer ffts;
        private final UserZipFile user;
        private final boolean isMetaData;
        private final BufferedWriter writer;
        private final boolean isServersToUse;
        private final int requiredReps;
        /**
         * This is the tricky part, so read this carefully:
         * 
         * Want to be able to have multiple threads working on data. So if three threads handling meta data,
         *   they shouldn't step on each others' heels. To do this, assign each thread a number, along with the
         *   total number of other threads doing the same task.
         *
         * This way, can split up the tasks into portion, and each will start off at different places and take a slice
         *   of the remaining duty. 
         *   - Thread one: 1-499, 1500-1999, etc.
         *   - Thread two: 500-999, 2000,2499, etc.
         *   - Thread three: 1000-1499, 2500-2999, etc.
         *
         * So,
         *       startingOffset = 0 + threadNumber * hashBatchSize
         * And next batch,
         *       start with => last offset + totalThreadCount * hashBatchSize
         *
         *   - Thread one:
         *     - First batch: startingOffset = 0 + 0 * 500 = 0
         *     - Second batch: 0 + 3 * 500 = 1500
         *   - Thread two:
         *     - First batch: startingOffset = 0 + 1 * 500 = 500
         *     - Second batch: 500 + 3 * 500 = 2000
         *   - Thread three:
         *     - First batch: startingOffset = 0 + 2 * 500 = 1000
         *     - Second batch: 1000 + 3 * 500 = 2500
         *
         * So on!
         */
        long threadNumber, totalThreadCount, startingOffset;

        /**
         *
         * @param ffserver
         * @param uzf
         * @param isMetaData
         */
        ChunkCheckThread(FlatFileTrancheServer ffts, UserZipFile user, boolean isMetaData, BufferedWriter writer, long threadNumber, long totalThreadCount) {
            this.ffts = ffts;
            this.user = user;
            this.isMetaData = isMetaData;
            this.writer = writer;

            // Determine whether there are explicit servers to use and how
            // many replications must exist before satisfied.
            this.isServersToUse = InjectDataIntoTrancheNetwork.serversToUse.size() > 0;
            if (this.isServersToUse) {
                requiredReps = InjectDataIntoTrancheNetwork.serversToUse.size();
            } else {
                requiredReps = 3;
            }

            this.threadNumber = threadNumber;
            this.totalThreadCount = totalThreadCount;

            // Calculate the starting offset for this thread!
            this.startingOffset = 0 + this.threadNumber * hashBatchSize;

            String type = null;
            if (isMetaData) {
                type = "meta data";
            } else {
                type = "data";
            }

            System.out.println("ChunkCheckThread #" + this.threadNumber + " for " + type + " starting with " + this.startingOffset);
        }

        @Override()
        public void run() {
            // port to updated connection scheme
            if (true) {
                throw new TodoException();
            }

//
//            try {
//                BigInteger offset = BigInteger.valueOf(this.startingOffset);
//                BigInteger limit = BigInteger.valueOf(hashBatchSize);
//
//                BigHash[] chunks = null;
//
//                // Get first batch of chunks.
//                if (isMetaData) {
//                    chunks = ffts.getMetaDataHashes(offset, limit);
//                } else {
//                    chunks = ffts.getDataHashes(offset, limit);
//                }
//
//                if (chunks == null) {
//                    throw new Exception("Why did server return null chunks!!!");
//                }
//
//                // Start with server's known data chunks. Get's next batch at end of loop.
//                for (; chunks.length > 0;) {
//
//                    int percent = (int) (100.0 * (double) (dataCount + metaCount) / (dataChunkTotal + metaChunkTotal));
//
//                    if (isMetaData) {
//                        System.out.println("  -> Total of " + metaCount + " out of " + metaChunkTotal + " meta chunks processed, total of " + noReplications + " replications, entire process approximately " + percent + "% complete.");
//                    } else {
//                        System.out.println("  -> Total of " + dataCount + " out of " + dataChunkTotal + " data chunks processed, total of " + noReplications + " replications, entire process approximately " + percent + "% complete.");
//                    }
//
//                    // For each hash in batch
//                    try {
//                        for (BigHash hash : chunks) {
//
//                            ChunkType type = null;
//
//                            if (isMetaData) {
//                                metaCount++;
//                                type = ChunkType.META;
//                            } else {
//                                dataCount++;
//                                type = ChunkType.DATA;
//                            }
//
//                            List<String> serversWithChunkAlready = null;
//
//                            if (this.isServersToUse) {
//                                serversWithChunkAlready = ScriptsUtil.getServersWithChunk(hash, type, InjectDataIntoTrancheNetwork.serversToUse);
//                            } else {
//                                serversWithChunkAlready = ScriptsUtil.getServersWithChunk(hash, type);
//                            }
//                            writer.write("! " + serversWithChunkAlready.size() + " servers with " + type + " hash " + hash + Text.getNewLine());
//                            writer.flush();
//
//                            // Increment count if no replications
//                            if (serversWithChunkAlready.size() == 0) {
//                                noReplications++;
//                            }
//
//                            int reps = serversWithChunkAlready.size();
//                            if (reps < requiredReps) {
//
//                                final List<String> candidateServers = new ArrayList();
//
//                                // If certain servers to use, select those.
//                                // Otherwise, use core online servers.
//                                if (this.isServersToUse) {
//                                    candidateServers.addAll(InjectDataIntoTrancheNetwork.serversToUse);
//                                } else {
//                                    for (StatusTableRow row: NetworkUtil.getStatus().getRows()) {
//                                        candidateServers.add(row.getURL());
//                                    }
//                                }
//
//                                Collections.shuffle(candidateServers);
//
//                                final List<String> serversToInjectTo = new ArrayList();
//
//                                // Build up prioritized/randomized list of servers to which should inject data
//                                CORE_SERVERS_LOOP:
//                                for (String s : candidateServers) {
//
//                                    // Skip offline servers
//                                    if (!ServerUtil.isServerOnline(s)) {
//                                        continue CORE_SERVERS_LOOP;
//                                    }
//
//                                    IS_IT_NOT_ON_THIS_SERVER:
//                                    for (String url : serversWithChunkAlready) {
//                                        if (url.trim().equals(s.trim())) {
//                                            continue CORE_SERVERS_LOOP;
//                                        }
//                                    }
//
//                                    // Skip read-only servers
//                                    if (ServerUtil.isReadOnlyServer(s)) {
//                                        continue CORE_SERVERS_LOOP;
//                                    }
//
//                                    serversToInjectTo.add(s);
//                                }
//
//                                try {
//                                    boolean offered = false;
//
//                                    final ChunkToInject injectionWrapper = new ChunkToInject(hash, type, serversToInjectTo, requiredReps, reps);
//
//                                    // Keep offering until queue accepts
//                                    while (!offered) {
//
//                                        offered = chunksToInjectQueue.offer(injectionWrapper);
//
//                                        // This will only happen if the upload queue is full
//                                        if (!offered) {
//                                            Thread.sleep(500);
//                                            Thread.yield();
//                                        } else {
//                                            writer.write(" [queued] " + hash + " queue size: " + chunksToInjectQueue.size());
//                                            writer.flush();
//                                        }
//                                    }
//                                } catch (Exception ex) {
//                                    ex.printStackTrace(System.err);
//                                    System.err.flush();
//                                } finally {
//                                }
//
//                            } // Replications loop
//
//                        } // For each chunk to check in batch
//
//                    } catch (Exception ex) {
//                        System.err.println("Problem with checking chunk: " + ex.getMessage());
//                        ex.printStackTrace(System.err);
//                    }
//
//                    // update the offset
//                    offset = offset.add(BigInteger.valueOf(hashBatchSize * this.totalThreadCount));
//
//                    // Get next batch of chunks
//                    if (isMetaData) {
//                        chunks = ffts.getMetaDataHashes(offset, limit);
//                    } else {
//                        chunks = ffts.getDataHashes(offset, limit);
//                    }
//                } // For each batch of chunk hashes
//
//            } catch (Exception ex) {
//                System.err.println("Problem occurred in ChunkCheckThread run: " + ex.getMessage());
//                ex.printStackTrace(System.err);
//            } finally {
//                System.out.println("Exitting ChunkCheckThread...");
//            }
//
        } // Run
    } // ChunkCheckThread

    /**
     * <p>Thread to inject chunks.</p>
     */
    static class ChunkInjectionThread extends Thread {

        final FlatFileTrancheServer ffts;
        final UserZipFile user;
        final ArrayBlockingQueue<ChunkToInject> queue;
        private boolean stopped = false;
        private final BufferedWriter chunkExceptionFileWriter,  chunkFailureFileWriter;

        /**
         *
         * @param user
         * @param isMetaData
         * @param queue
         */
        ChunkInjectionThread(FlatFileTrancheServer ffts, UserZipFile user, ArrayBlockingQueue<ChunkToInject> queue, BufferedWriter chunkErrorFileWriter, BufferedWriter chunkFailureFileWriter) {
            this.ffts = ffts;
            this.user = user;
            this.queue = queue;
            this.chunkExceptionFileWriter = chunkErrorFileWriter;
            this.chunkFailureFileWriter = chunkFailureFileWriter;
        }

        @Override()
        public void run() {
            // port to new connection scheme
//            try {
//                // Continue until stopped and finished injecting
//                while (!stopped || !this.queue.isEmpty()) {
//
//                    ChunkToInject chunkToInject = null;
//
//                    try {
//                        synchronized (queue) {
//                            chunkToInject = queue.poll();
//                        }
//                    } catch (Exception ie) {
//                        // nope
//                    }
//
//                    if (chunkToInject == null) {
//                        try {
//                            Thread.sleep(50);
//                            Thread.yield();
//                        } catch (InterruptedException nope) {
//                        }
//
//                        continue;
//                    }
//
//                    byte[] bytesToUpload = null;
//
//                    try {
//                        if (chunkToInject.isMetaData()) {
//                            bytesToUpload = (byte[])IOUtil.getMetaData(ffts, chunkToInject.getChunkHash(), false).getReturnValueObject();
//                        } else {
//                            bytesToUpload = (byte[])IOUtil.getData(ffts, chunkToInject.getChunkHash(), false).getReturnValueObject();
//                        }
//                    } catch (Exception ex) {
//                        System.err.println("THIS MIGHT BE A SERIES DATA BLOCK UTIL ERROR:");
//                        System.err.println("Problem getting bytes: " + ex.getMessage());
//                        ex.printStackTrace(System.err);
//                        continue;
//                    }
//
//                    if (bytesToUpload == null) {
//                        System.err.println("Local bytes for chunk null, " + chunkToInject.getChunkHash().toString().substring(0, 16) + "..., " + chunkToInject.type);
//                        continue;
//                    }
//
//                    // Upload to 3 servers
//                    int reps = chunkToInject.foundReps;
//                    try {
//                        CORE_SERVERS_LOOP:
//                        for (String s : chunkToInject.getServersToInjectTo()) {
//
//                            if (!ServerUtil.isServerOnline(s)) {
//                                continue;
//                            }
//
//                            if (reps >= 3) {
//                                break CORE_SERVERS_LOOP;
//                            }
//
//                            TrancheServer rserver = null;
//
//                            try {
//                                rserver = ConnectionUtil.connectURL(s, false);
//
//                                if (chunkToInject.isMetaData()) {
//                                    IOUtil.setMetaData(rserver, user.getCertificate(), user.getPrivateKey(), false, chunkToInject.getChunkHash(), bytesToUpload);
//                                } else {
//                                    IOUtil.setData(rserver, user.getCertificate(), user.getPrivateKey(), chunkToInject.getChunkHash(), bytesToUpload);
//                                }
//
//                                // Update replications count. Will bail if enough
//                                totalChunksInjected++;
//                                reps++;
//
//                            } catch (Exception ex) {
//                                System.err.println("Problem injecting chunk " + chunkToInject.getChunkHash().toString().substring(0, 14) + "..., " + chunkToInject.getType() + ": " + ex.getMessage());
//                                ex.printStackTrace(System.err);
//
//                                synchronized (this.chunkExceptionFileWriter) {
//                                    String metaOrData = null;
//                                    if (chunkToInject.isMetaData()) {
//                                        metaOrData = "META";
//                                    } else {
//                                        metaOrData = "DATA";
//                                    }
//
//                                    this.chunkExceptionFileWriter.write("\"" + metaOrData + "\",");
//                                    this.chunkExceptionFileWriter.write(chunkToInject.getRequiredReps() + ",");
//                                    this.chunkExceptionFileWriter.write(chunkToInject.getFoundReps() + ",");
//
//                                    this.chunkExceptionFileWriter.write("\"" + s + "\",");
//
//                                    this.chunkExceptionFileWriter.write("\"" + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + "\",");
//                                    this.chunkExceptionFileWriter.write("\"" + ex.getMessage() + "\",");
//                                    this.chunkExceptionFileWriter.write("\"" + chunkToInject.getChunkHash() + "\"");
//                                    this.chunkExceptionFileWriter.newLine();
//                                    this.chunkExceptionFileWriter.flush();
//                                }
//                            }
//                        }
//
//                    } finally {
//                        // If not enough reps, output to separate file
//                        if (reps < 3) {
//                            synchronized (this.chunkFailureFileWriter) {
//                                // "Expected replications", "Found replications", "Type", "Hash"
//
//                                String metaOrData = null;
//                                if (chunkToInject.isMetaData()) {
//                                    metaOrData = "META";
//                                } else {
//                                    metaOrData = "DATA";
//                                }
//
//                                this.chunkFailureFileWriter.write("\"3\",");
//                                this.chunkFailureFileWriter.write("\"" + reps + "\",");
//                                this.chunkFailureFileWriter.write("\"" + metaOrData + "\",");
//                                this.chunkFailureFileWriter.write("\"" + chunkToInject.getChunkHash() + "\"");
//                                this.chunkFailureFileWriter.newLine();
//                                this.chunkFailureFileWriter.flush();
//                            }
//                        }
//                    }
//                }
//
//            } catch (Exception ex) {
//                System.err.println("Problem in ChunkInjectionThread: " + ex.getMessage());
//                ex.printStackTrace(System.err);
//            } finally {
//                System.out.println("Exitting ChunkInjectionThread...");
//            }
        }

        /**
         * <p>Check if thread has been stopped.</p>
         * @return
         */
        public boolean isStopped() {
            return stopped;
        }

        /**
         * <p>Set the thread to stop.</p>
         * @param stopped
         */
        public void setStopped(boolean stopped) {
            if (stopped) {
                synchronized (queue) {
                    System.out.println("Received signal to stop when queue is empty. Queue has " + queue.size() + " item(s) remaining to inject.");
                }
            }
            this.stopped = stopped;
        }
    } // ChunkInjectionThread

    /**
     * <p>Encapsulate a single chunk that needs uploaded.</p>
     */
    static class ChunkToInject {

        private BigHash chunkHash;
        private ChunkType type;
        private List<String> serversToInjectTo;
        private final int requiredReps,  foundReps;

        ChunkToInject(BigHash chunkHash, ChunkType type, List<String> serversToInjectTo, int requiredReps, int foundReps) {
            this.chunkHash = chunkHash;
            this.type = type;
            this.serversToInjectTo = serversToInjectTo;
            this.requiredReps = requiredReps;
            this.foundReps = foundReps;
        }

        /**
         * <p>Get hash for chunk to inject.</p>
         * @return
         */
        public BigHash getChunkHash() {
            return chunkHash;
        }

        /**
         * <p>Type of the chunk: meta or data.</p>
         * @return
         */
        public ChunkType getType() {
            return type;
        }

        /**
         * <p>List of server URLs to which to inject.</p>
         * @return
         */
        public List<String> getServersToInjectTo() {
            return serversToInjectTo;
        }

        /**
         * <p>Return true if the chunk is meta data.</p>
         * @return
         */
        public boolean isMetaData() {
            return this.type == ChunkType.META;
        }

        /**
         * <p>Get the required number of replications for this chunk.</p>
         * @return
         */
        public int getRequiredReps() {
            return requiredReps;
        }

        /**
         * <p>Get the number of replications found on the network.</p>
         * @return
         */
        public int getFoundReps() {
            return foundReps;
        }
    }
}
