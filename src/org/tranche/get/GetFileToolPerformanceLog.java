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
package org.tranche.get;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.tranche.ConfigureTranche;
import org.tranche.TrancheServer;
import org.tranche.commons.TextUtil;
import org.tranche.hash.BigHash;
import org.tranche.logs.ConnectionDiagnosticsLog;
import org.tranche.network.ConnectionUtil;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.remote.RemoteTrancheServerPerformanceListener;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.util.CompressionUtil;
import org.tranche.util.EmailUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>Used to help troubleshoot performance issues for client.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class GetFileToolPerformanceLog implements GetFileToolListener {

    private final Map<String, RemoteTrancheServerPerformanceListener> rtsListenerMap;
    private static final Object classLock = new Object();
    private static int classInstances = 0;
    private final int objId;
    private final ConnectionDiagnosticsLog rtsDiagnosticsLog,  gftDiagnosticsLog;
    private final File file;
    private final PrintStream out;
    private final long start;
    private final Map<BigHash, ChunkWrapper> dataChunks,  metaDataChunks;

    /**
     * 
     * @throws IOException
     * @throws FileNotFoundException
     */
    public GetFileToolPerformanceLog() throws IOException, FileNotFoundException {
        // Set the objId equal to the number of pre-existing logs, then increment
        synchronized (classLock) {
            objId = classInstances++;
        }
        rtsListenerMap = new HashMap();
        rtsDiagnosticsLog = new ConnectionDiagnosticsLog("RemoteTrancheServer diagnostics log");
        this.file = TempFileUtil.createTempFileWithName("gft-performance-" + TimeUtil.getTrancheTimestamp() + ".log");
        this.start = TimeUtil.getTrancheTimestamp();

        gftDiagnosticsLog = new ConnectionDiagnosticsLog("GetFileTool diagnostics log");

        this.dataChunks = new HashMap();
        this.metaDataChunks = new HashMap();

        this.out = new PrintStream(new FileOutputStream(this.file));
    }

    /**
     * <p>Notification of any other event occurence.</p>
     * @param msg
     */
    public void message(String msg) {
        synchronized (this.out) {
            this.out.println(TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()) + ": " + msg);
        }
    }

    /**
     * <p>Notification that a meta data chunk is starting to be downloaded.</p>
     * @param event
     */
    public void startedMetaData(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());

        try {
            synchronized (this.metaDataChunks) {
                this.metaDataChunks.put(event.getChunkHash(), new ChunkWrapper(event.getTimestamp(), ChunkType.Meta, event.getChunkHash()));
            }
        } catch (NullPointerException npe) {
            System.err.println(npe.getClass().getSimpleName() + ": " + npe.getMessage());
            npe.printStackTrace(System.err);
        }
    }

    /**
     * <p>Notification that a meta data chunk is trying to be downloaded from a server.</p>
     * @param event
     */
    public void tryingMetaData(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a meta data chunk could not be downloaded.</p>
     * @param event
     * @param exceptions
     */
    public void failedMetaData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        lazyAttachRemoteTrancheServerListener(event.getServer());

        final String desc = "Meta data (failed)";

        synchronized (this.metaDataChunks) {
            ChunkWrapper chunk = this.metaDataChunks.remove(event.getChunkHash());

            if (chunk != null) {
                final long delta = TimeUtil.getTrancheTimestamp() - chunk.started;

                this.gftDiagnosticsLog.logServerRequest(event.getServer(), delta, desc);
            }
        }

        for (PropagationExceptionWrapper pew : exceptions) {
            this.gftDiagnosticsLog.logException(pew.exception, event.getTimestamp(), desc);
        }
    }

    /**
     * <p>Notification that a meta data chunk has been downloaded.</p>
     * @param event
     */
    public void finishedMetaData(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());

        synchronized (this.metaDataChunks) {
            ChunkWrapper chunk = this.metaDataChunks.remove(event.getChunkHash());

            if (chunk != null) {
                final long delta = TimeUtil.getTrancheTimestamp() - chunk.started;
                final String desc = "Meta data (success)";

                this.gftDiagnosticsLog.logServerRequest(event.getServer(), delta, desc);
            }
        }
    }

    /**
     * <p>Notificatin that a data chunk is starting to be downloaded.</p>
     * @param event
     */
    public void startedData(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());

        try {
            synchronized (this.dataChunks) {
                this.dataChunks.put(event.getChunkHash(), new ChunkWrapper(event.getTimestamp(), ChunkType.Data, event.getChunkHash()));
            }
        } catch (NullPointerException npe) {
            System.err.println(npe.getClass().getSimpleName() + ": " + npe.getMessage());
            npe.printStackTrace(System.err);
        }
    }

    /**
     * <p>Notification that a data chunk is trying to be downloaded from a server.</p>
     * @param event
     */
    public void tryingData(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a data chunk could not be downloaded.</p>
     * @param event
     * @param exceptions
     */
    public void failedData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        lazyAttachRemoteTrancheServerListener(event.getServer());

        final String desc = "Data (failed)";

        synchronized (this.dataChunks) {
            ChunkWrapper chunk = this.dataChunks.remove(event.getChunkHash());

            if (chunk != null) {
                final long delta = TimeUtil.getTrancheTimestamp() - chunk.started;

                this.gftDiagnosticsLog.logServerRequest(event.getServer(), delta, desc);
            }
        }

        for (PropagationExceptionWrapper pew : exceptions) {
            this.gftDiagnosticsLog.logException(pew.exception, event.getTimestamp(), desc);
        }
    }

    /**
     * <p>Notification that a data chunk has been downloaded.</p>
     * @param event
     */
    public void finishedData(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());

        synchronized (this.dataChunks) {
            ChunkWrapper chunk = this.dataChunks.remove(event.getChunkHash());

            if (chunk != null) {
                final long delta = TimeUtil.getTrancheTimestamp() - chunk.started;
                final String desc = "Data (success)";

                this.gftDiagnosticsLog.logServerRequest(event.getServer(), delta, desc);
            }
        }
    }

    /**
     * <p>Notification that a file is starting to be downloaded.</p>
     * @param event
     */
    public void startingFile(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a file download has started.</p>
     * @param event
     */
    public void startedFile(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a file downloaded has been skipped.</p>
     * @param event
     */
    public void skippedFile(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a file download has finished.</p>
     * @param event
     */
    public void finishedFile(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a file download has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedFile(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
        
        for (PropagationExceptionWrapper pew : exceptions) {
            this.gftDiagnosticsLog.logException(pew.exception, event.getTimestamp(), "File (failed)");
        }
    }

    /**
     * <p>Notification that a directory download is starting.</p>
     * @param event
     */
    public void startingDirectory(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a directory download has started.</p>
     * @param event
     */
    public void startedDirectory(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a directory download has finished.</p>
     * @param event
     */
    public void finishedDirectory(GetFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());

        this.message("Finished, success");
        
        // Finalize logging.
        this.finish();
    }

    /**
     * <p>Notification that a directory download has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedDirectory(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
        
        this.message("Finished, failure");
        
        for (PropagationExceptionWrapper pew : exceptions) {
            this.gftDiagnosticsLog.logException(pew.exception, event.getTimestamp(), "Directory (failed)");
        }

        // Finalize logging.
        this.finish();
    }

    /**
     * 
     * @param host
     */
    private void lazyAttachRemoteTrancheServerListener(String host) {

        // Won't always be a host
        if (host == null) {
            return;
        }

        synchronized (rtsListenerMap) {
            RemoteTrancheServerPerformanceListener l = rtsListenerMap.get(host);

            if (l == null) {

                TrancheServer ts = ConnectionUtil.getHost(host);

                if (ts != null) {

                    // Note: If ConnectionUtil.getHost(String) returns RemoteTrancheServer only,
                    //       then the method should not return TrancheServer. Right now,
                    //       assume its not safe to typecast without checking. However,
                    //       not sure how to handle non-RemoteTrancheServer items
                    if (ts instanceof RemoteTrancheServer) {

                        RemoteTrancheServer rts = (RemoteTrancheServer) ts;

                        l = new RemoteTrancheServerPerformanceListener(rtsDiagnosticsLog, host, objId);
                        rts.addListener(l);
                        rtsListenerMap.put(host, l);
                    } else {
                        System.err.println("WARNING (in " + this.getClass().getName() + "): Tranche server not a RemoteTrancheServer. Please notify developers so can fix.");
                    }
                }

            }
        }
    }

    /**
     * 
     */
    private enum ChunkType {
        Data, Meta
    }

    /**
     * 
     */
    private class ChunkWrapper {

        final long started;
        final ChunkType type;
        final BigHash hash;

        ChunkWrapper(long started, ChunkType type, BigHash hash) {
            this.started = started;
            this.type = type;
            this.hash = hash;
        }
    }

    /**
     * 
     * @return
     */
    public File getFile() {
        return file;
    }
    private static boolean isFinished = false;

    /**
     * <p>Write summary, flush and close resource.</p>
     */
    private void finish() {

        if (isFinished) {
            return;
        }
        isFinished = true;

        // Write summary
        this.gftDiagnosticsLog.printSummary(this.out);
        this.rtsDiagnosticsLog.printSummary(this.out);

        try {
            this.out.flush();
        } catch (Exception e) { /* */ }
        try {
            this.out.close();
        } catch (Exception e) { /* */ }

        this.sendEmailAndDeleteFile();
    }

    /**
     * 
     */
    private void sendEmailAndDeleteFile() {
        File zippedFile = null;
        try {
            String subject = "[" + ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_NAME) + "] Download Performance Log @ " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp());
            String message = "See attached file.";

            zippedFile = CompressionUtil.zipCompress(this.getFile());
            EmailUtil.sendEmailHttp(subject, ConfigureTranche.getAdminEmailAccounts(), message, zippedFile);

            IOUtil.safeDelete(zippedFile);
            IOUtil.safeDelete(this.getFile());
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getClass().getSimpleName() + " while emailing GFT performance log: " + e.getMessage());

            // Suggest send zip file (if available); else, use uncompressed file
            File toSend = this.getFile();
            if (zippedFile != null && zippedFile.exists()) {
                toSend = zippedFile;
            }
            System.err.println("NOTE: Please send the following file to Tranche development team: " + toSend.getAbsolutePath());
        }
    }
}
