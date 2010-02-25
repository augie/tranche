/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tranche.add;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.tranche.ConfigureTranche;
import org.tranche.TrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.logs.ConnectionDiagnosticsLog;
import org.tranche.network.*;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.remote.RemoteTrancheServerPerformanceListener;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.util.CompressionUtil;
import org.tranche.util.EmailUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.Text;

/**
 * <p>Used to help troubleshoot performance issues for client.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class AddFileToolPerformanceLog implements AddFileToolListener {

    private final Map<String, RemoteTrancheServerPerformanceListener> rtsListenerMap;
    private final static Object classLock = new Object();
    private static int classInstances = 0;
    private final int objId;
    private final ConnectionDiagnosticsLog rtsDiagnosticsLog,  aftDiagnosticsLog;
    private final File file;
    private final PrintStream out;
    private final long start;
    private final Map<BigHash, ChunkWrapper> dataChunks,  metaDataChunks;

    /**
     * 
     * @param file
     * @throws java.io.FileNotFoundException
     */
    public AddFileToolPerformanceLog(File file) throws FileNotFoundException {
        // Set the objId equal to the number of pre-existing logs, then increment
        synchronized (classLock) {
            objId = classInstances++;
        }
        rtsListenerMap = new HashMap();
        rtsDiagnosticsLog = new ConnectionDiagnosticsLog("RemoteTrancheServer diagnostics log");
        aftDiagnosticsLog = new ConnectionDiagnosticsLog("AddFileTool diagnostics log");
        this.file = file;
        this.start = TimeUtil.getTrancheTimestamp();

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
            this.out.println(Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + ": " + msg);
        }
    }

    /**
     * <p>Notification that a meta data chunk is starting to be uploaded.</p>
     * @param event
     */
    public void startedMetaData(AddFileToolEvent event) {
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
     * <p>Notification that a meta data chunk is trying to be uploaded to a server.</p>
     * @param event
     */
    public void tryingMetaData(AddFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a meta data chunk has been uploaded to a server.</p>
     * @param event
     */
    public void uploadedMetaData(AddFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a meta data chunk could not be uploaded.</p>
     * @param event
     * @param exceptions
     */
    public void failedMetaData(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        lazyAttachRemoteTrancheServerListener(event.getServer());

        final String desc = "Meta data (failed)";

        synchronized (this.metaDataChunks) {
            ChunkWrapper chunk = this.metaDataChunks.remove(event.getChunkHash());

            if (chunk != null) {
                final long delta = TimeUtil.getTrancheTimestamp() - chunk.started;

                this.aftDiagnosticsLog.logServerRequest(event.getServer(), delta, desc);
            }
        }

        for (PropagationExceptionWrapper pew : exceptions) {
            this.aftDiagnosticsLog.logException(pew.exception, event.getTimestamp(), desc);
        }
    }

    /**
     * <p>Notification that a meta data chunk has been uploaded.</p>
     * @param event
     */
    public void finishedMetaData(AddFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());

        synchronized (this.metaDataChunks) {
            ChunkWrapper chunk = this.metaDataChunks.remove(event.getChunkHash());

            if (chunk != null) {
                final long delta = TimeUtil.getTrancheTimestamp() - chunk.started;
                final String desc = "Meta data (success)";

                this.aftDiagnosticsLog.logServerRequest(event.getServer(), delta, desc);
            }
        }
    }

    /**
     * <p>Notification that a data chunk is about to be checked for on-line replications.</p>
     * @param event
     */
    public void startingData(AddFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a data chunk is starting to be uploaded.</p>
     * @param event
     */
    public void startedData(AddFileToolEvent event) {
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
     * <p>Notification that a data chunk is trying to be uploaded to a server.</p>
     * @param event
     */
    public void tryingData(AddFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a data chunk has been uploaded to a server.</p>
     * @param event
     */
    public void uploadedData(AddFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a data chunk could not be uploaded.</p>
     * @param event
     * @param exceptions
     */
    public void failedData(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
        final String desc = "Data (failed)";

        synchronized (this.dataChunks) {
            ChunkWrapper chunk = this.dataChunks.remove(event.getChunkHash());

            if (chunk != null) {
                final long delta = TimeUtil.getTrancheTimestamp() - chunk.started;

                this.aftDiagnosticsLog.logServerRequest(event.getServer(), delta, desc);
            }
        }

        for (PropagationExceptionWrapper pew : exceptions) {
            this.aftDiagnosticsLog.logException(pew.exception, event.getTimestamp(), desc);
        }
    }

    /**
     * <p>Notification that a data chunk has been uploaded.</p>
     * @param event
     */
    public void finishedData(AddFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());

        synchronized (this.dataChunks) {
            ChunkWrapper chunk = this.dataChunks.remove(event.getChunkHash());

            if (chunk != null) {
                final long delta = TimeUtil.getTrancheTimestamp() - chunk.started;
                final String desc = "Data (success)";

                this.aftDiagnosticsLog.logServerRequest(event.getServer(), delta, desc);
            }
        }
    }

    /**
     * <p>Notification that a file upload has started.</p>
     * @param event
     */
    public void startedFile(AddFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a file upload has finished.</p>
     * @param event
     */
    public void finishedFile(AddFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
    }

    /**
     * <p>Notification that a file upload has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedFile(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
        
        for (PropagationExceptionWrapper pew : exceptions) {
            this.aftDiagnosticsLog.logException(pew.exception, event.getTimestamp(), "File (failed)");
        }
    }

    /**
     * <p>Notification that a directory upload has started.</p>
     * @param event
     */
    public void startedDirectory(AddFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());


    }

    /**
     * <p>Notification that a directory upload has finished.</p>
     * @param event
     */
    public void finishedDirectory(AddFileToolEvent event) {
        lazyAttachRemoteTrancheServerListener(event.getServer());

        this.message("Finished, success.");

        // Finalize logging.
        this.finish();
    }

    /**
     * <p>Notification that a directory upload has failed.</p>
     * @param event
     * @param exceptions
     */
    public void failedDirectory(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
        lazyAttachRemoteTrancheServerListener(event.getServer());
        
        this.message("Finished, failed.");
        
        for (PropagationExceptionWrapper pew : exceptions) {
            this.aftDiagnosticsLog.logException(pew.exception, event.getTimestamp(), "Directory (failed)");
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
        this.aftDiagnosticsLog.printSummary(this.out);
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
            String subject = "AddFilePerformanceLog [" + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + "]: " + this.getFile().getName();
            String message = "See attached file.";

            zippedFile = CompressionUtil.zipCompress(this.getFile());
            EmailUtil.sendEmail(subject, ConfigureTranche.getAdminEmailAccounts(), message, zippedFile);

            IOUtil.safeDelete(zippedFile);
            IOUtil.safeDelete(this.getFile());
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getClass().getSimpleName() + " while emailing AFT performance log: " + e.getMessage());

            // Suggest send zip file (if available); else, use uncompressed file
            File toSend = this.getFile();
            if (zippedFile != null && zippedFile.exists()) {
                toSend = zippedFile;
            }
            System.err.println("NOTE: Please send the following file to Tranche development team: " + toSend.getAbsolutePath());
        }
    }
}
