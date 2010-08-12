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
package org.tranche.gui.add;

import java.awt.Component;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.tranche.add.AddFileTool;
import org.tranche.add.AddFileToolAdapter;
import org.tranche.add.AddFileToolEvent;
import org.tranche.add.AddFileToolReport;
import org.tranche.add.AddFileToolUtil;
import org.tranche.gui.EmailFrame;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.add.monitor.UploadMonitor;
import org.tranche.gui.project.ProjectPool;
import org.tranche.gui.util.GUIUtil;
import org.tranche.license.License;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.project.ProjectSummary;
import org.tranche.remote.RemoteUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.users.UserCertificateUtil;
import org.tranche.commons.ThreadUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class UploadSummary implements ClipboardOwner {

    public static final String STATUS_STARTING = "Starting";
    public static final String STATUS_QUEUED = "Queued";
    public static final String STATUS_UPLOADING = "Uploading";
    public static final String STATUS_PAUSED = "Paused";
    public static final String STATUS_FINISHED = "Finished";
    public static final String STATUS_FAILED = "Failed";
    public static final String STATUS_STOPPED = "Stopped";
    public static final int DEFAULT_INT_VALUE = -1;
    public static final int LATEST_VERSION = 2;
    private int version = LATEST_VERSION;
    private final Map<String, Map<String, Integer>> actionMap = new HashMap<String, Map<String, Integer>>();
    private String status = STATUS_STARTING;
    private final AddFileTool addFileTool;
    private AddFileToolReport report;
    private AddFileToolProgressBar progressBar;
    private boolean isStarted = false;
    private Long timeStarted = null;
    private AddFileToolReportFrame reportGUI = null;
    private ErrorFrame errorFrame = null;
    private UploadMonitor monitor = null;
    private Thread uploadThread = new Thread("Upload Thread") {

        @Override
        public void run() {
            try {
                clearActionCount();
                addFileTool.addListener(new AddFileToolAdapter() {

                    private void handleAction(AddFileToolEvent event) {
                        synchronized (actionMap) {
                            if (!actionMap.containsKey(AddFileToolEvent.staticGetTypeString(event.getType()))) {
                                actionMap.put(AddFileToolEvent.staticGetTypeString(event.getType()), new HashMap<String, Integer>());
                            }
                            if (!actionMap.get(AddFileToolEvent.staticGetTypeString(event.getType())).containsKey(AddFileToolEvent.staticGetActionString(event.getAction()))) {
                                actionMap.get(AddFileToolEvent.staticGetTypeString(event.getType())).put(AddFileToolEvent.staticGetActionString(event.getAction()), 0);
                            }
                            int value = actionMap.get(AddFileToolEvent.staticGetTypeString(event.getType())).get(AddFileToolEvent.staticGetActionString(event.getAction()));
                            value++;
                            actionMap.get(AddFileToolEvent.staticGetTypeString(event.getType())).put(AddFileToolEvent.staticGetActionString(event.getAction()), value);
                        }
                    }

                    @Override
                    public void startedMetaData(AddFileToolEvent event) {
                        handleAction(event);
                    }

                    @Override
                    public void tryingMetaData(AddFileToolEvent event) {
                        handleAction(event);
                    }

                    @Override
                    public void uploadedMetaData(AddFileToolEvent event) {
                        handleAction(event);
                    }

                    @Override
                    public void failedMetaData(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                        handleAction(event);
                        UploadPool.set(UploadSummary.this);
                    }

                    @Override
                    public void finishedMetaData(AddFileToolEvent event) {
                        handleAction(event);
                    }

                    @Override
                    public void startingData(AddFileToolEvent event) {
                        handleAction(event);
                    }

                    @Override
                    public void startedData(AddFileToolEvent event) {
                        handleAction(event);
                    }

                    @Override
                    public void tryingData(AddFileToolEvent event) {
                        handleAction(event);
                    }

                    @Override
                    public void uploadedData(AddFileToolEvent event) {
                        handleAction(event);
                    }

                    @Override
                    public void failedData(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                        handleAction(event);
                        UploadPool.set(UploadSummary.this);
                    }

                    @Override
                    public void finishedData(AddFileToolEvent event) {
                        handleAction(event);
                        UploadPool.set(UploadSummary.this);
                    }

                    @Override
                    public void startedFile(AddFileToolEvent event) {
                        handleAction(event);
                    }

                    @Override
                    public void finishedFile(AddFileToolEvent event) {
                        handleAction(event);
                    }

                    @Override
                    public void failedFile(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                        handleAction(event);
                        UploadPool.set(UploadSummary.this);
                    }

                    @Override
                    public void startedDirectory(AddFileToolEvent event) {
                        handleAction(event);
                    }

                    @Override
                    public void finishedDirectory(AddFileToolEvent event) {
                        handleAction(event);
                        UploadPool.set(UploadSummary.this);
                    }

                    @Override
                    public void failedDirectory(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                        handleAction(event);
                        UploadPool.set(UploadSummary.this);
                    }
                });
                if (timeStarted == null) {
                    timeStarted = TimeUtil.getTrancheTimestamp();
                }
                report = addFileTool.execute(timeStarted);
                progressBar.incrementErrorCount(report.getFailureExceptions().size());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (report != null) {
                if (report.isFailed()) {
                    setStatus(STATUS_FAILED);
                    showErrorFrame(null);
                } else {
                    setStatus(STATUS_FINISHED);
                    if (!addFileTool.isDataOnly()) {
                        showReportGUI(null);
                        ProjectSummary ps = new ProjectSummary(report.getHash(), addFileTool.getTitle(), addFileTool.getDescription(), addFileTool.getSize(), addFileTool.getFileCount(), report.getTimestampStart(), UserCertificateUtil.readUserName(addFileTool.getUserCertificate()), addFileTool.isShowMetaDataIfEncrypted());
                        ps.isEncrypted = addFileTool.getPassphrase() != null;
                        ProjectPool.set(ps);
                    }
                }
            } else {
                setStatus(STATUS_FAILED);
            }
        }
    };

    public UploadSummary(AddFileTool addFileTool) {
        this.addFileTool = addFileTool;
        progressBar = new AddFileToolProgressBar(addFileTool);
        addFileTool.addListener(progressBar);
    }

    public UploadSummary(InputStream in) throws Exception {
        addFileTool = new AddFileTool();
        progressBar = new AddFileToolProgressBar(addFileTool);
        addFileTool.addListener(progressBar);
        deserialize(in);
    }

    public UploadSummary(UploadSummary summary) throws Exception {
        // parameters
        timeStarted = summary.getTimeStarted();
        addFileTool = new AddFileTool();
        progressBar = new AddFileToolProgressBar(addFileTool);
        addFileTool.addListener(progressBar);
        addFileTool.setFile(summary.getAddFileTool().getFile());
        addFileTool.setTitle(summary.getAddFileTool().getTitle());
        addFileTool.setDescription(summary.getAddFileTool().getDescription());
        addFileTool.setUserCertificate(summary.getAddFileTool().getUserCertificate());
        addFileTool.setUserPrivateKey(summary.getAddFileTool().getUserPrivateKey());
        addFileTool.setPassphrase(summary.getAddFileTool().getPassphrase());
        addFileTool.setCompress(summary.getAddFileTool().isCompress());
        addFileTool.setDataOnly(summary.getAddFileTool().isDataOnly());
        addFileTool.setExplodeBeforeUpload(summary.getAddFileTool().isExplodeBeforeUpload());
        addFileTool.setShowMetaDataIfEncrypted(summary.getAddFileTool().isShowMetaDataIfEncrypted());
        addFileTool.setUseUnspecifiedServers(summary.getAddFileTool().isUsingUnspecifiedServers());
        addFileTool.setThreadCount(summary.getAddFileTool().getThreadCount());
        addFileTool.addConfirmationEmails(summary.getAddFileTool().getConfirmationEmails());
        addFileTool.addServersToUse(summary.getAddFileTool().getServersToUse());
        addFileTool.addStickyServers(summary.getAddFileTool().getStickyServers());
        for (MetaDataAnnotation mda : summary.getAddFileTool().getMetaDataAnnotations()) {
            addFileTool.addMetaDataAnnotation(mda);
        }
        if (summary.getAddFileTool().getLicense() != null) {
            addFileTool.setLicense(summary.getAddFileTool().getLicense());
        }
    }

    public void showReportGUI(Component relativeTo) {
        if (reportGUI == null) {
            reportGUI = new AddFileToolReportFrame(this);
        }
        if (relativeTo == null) {
            GUIUtil.centerOnScreen(reportGUI);
        } else {
            reportGUI.setLocationRelativeTo(relativeTo);
        }
        reportGUI.setVisible(true);
    }

    public void showErrorFrame(Component relativeTo) {
        if (errorFrame == null) {
            errorFrame = new ErrorFrame();
        }
        if (report != null) {
            errorFrame.setPropagated(report.getFailureExceptions());
        }
        if (relativeTo == null) {
            GUIUtil.centerOnScreen(errorFrame);
        } else {
            errorFrame.setLocationRelativeTo(relativeTo);
        }
        errorFrame.setVisible(true);
    }

    public void showMonitor(Component relativeTo) {
        if (monitor == null) {
            monitor = new UploadMonitor(this);
        }
        if (relativeTo == null) {
            GUIUtil.centerOnScreen(monitor);
        } else {
            monitor.setLocationRelativeTo(relativeTo);
        }
        monitor.setVisible(true);
        monitor.start();
    }

    public void showEmailFrame(Component relativeTo) {
        EmailFrame emailFrame = new EmailFrame(AddFileToolUtil.getEmailReceiptSubject(report), AddFileToolUtil.getEmailReceiptMessage(report));
        if (relativeTo == null) {
            GUIUtil.centerOnScreen(monitor);
        } else {
            emailFrame.setLocationRelativeTo(relativeTo);
        }
        emailFrame.setVisible(true);
    }

    public void saveReceipt(final Component relativeTo) {
        final JFileChooser chooser = GUIUtil.makeNewFileChooser();
        Thread t = new Thread("Save Receipt") {

            @Override()
            public void run() {
                chooser.setAcceptAllFileFilterUsed(false);
                chooser.setMultiSelectionEnabled(false);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setDialogTitle("Save Upload Receipt");
                chooser.setFileFilter(new FileFilter() {

                    private String description = "Text Files";

                    public boolean accept(File pathname) {
                        try {
                            if (pathname.getName().endsWith(".txt")) {
                                return true;
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }
                    }

                    public String getDescription() {
                        return description;
                    }
                });
                int result = chooser.showSaveDialog(relativeTo);

                // Chose file and saved
                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                File saveToBuilder = chooser.getSelectedFile();

                // make sure the file ends with ".txt"
                if (!saveToBuilder.getName().endsWith(".txt")) {
                    saveToBuilder = new File(saveToBuilder.getParentFile(), saveToBuilder.getName() + ".txt");
                }

                File saveTo = saveToBuilder;
                try {
                    PrintWriter printer = new PrintWriter(new FileOutputStream(saveTo));
                    printer.print(AddFileToolUtil.getEmailReceiptMessage(getReport()));
                    printer.close();
                    GenericOptionPane.showMessageDialog(relativeTo, "Your receipt is saved at <" + saveTo.getPath() + ">.");
                } catch (FileNotFoundException ex) {
                    GenericOptionPane.showMessageDialog(relativeTo, "ERROR: Could not save to <" + saveTo.getPath() + ">. Please try again or cancel." + "\n" + ex.getMessage());
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    public void copyHash() {
        if (report != null && report.getHash() != null) {
            GUIUtil.copyToClipboard(report.getHash().toString(), this);
        }
    }

    public void showHash() {
        if (report != null && report.getHash() != null) {
            GUIUtil.showHash(report.getHash(), this, null);
        }
    }

    public AddFileTool getAddFileTool() {
        return addFileTool;
    }

    public AddFileToolProgressBar getProgressBar() {
        return progressBar;
    }

    public Long getTimeStarted() {
        return timeStarted;
    }

    public AddFileToolReport getReport() {
        return report;
    }

    public void clearActionCount() {
        synchronized (actionMap) {
            actionMap.clear();
        }
    }

    public int getActionCount(String type, String action) {
        synchronized (actionMap) {
            if (actionMap.get(type) != null) {
                if (actionMap.get(type).get(action) != null) {
                    return actionMap.get(type).get(action);
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        }
    }

    public void waitForFinish() {
        // wait for start
        while (!isStarted()) {
            ThreadUtil.sleep(500);
        }
        // wait for finish
        while (!isFinished()) {
            try {
                uploadThread.join();
            } catch (Exception e) {
            }
        }
    }

    public void waitForFinish(long millis) {
        if (millis <= 0) {
            return;
        }
        long waited = 0;
        // wait for start
        while (!isStarted() && waited < millis) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
            waited += 100;
        }
        // wait for finish
        while (!isFinished() && waited < millis) {
            long start = TimeUtil.getTrancheTimestamp();
            try {
                uploadThread.join(millis - waited);
            } catch (Exception e) {
            }
            waited += TimeUtil.getTrancheTimestamp() - start;
        }
    }

    public String getStatus() {
        return status;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public boolean isStarting() {
        return status.equals(STATUS_STARTING);
    }

    public boolean isUploading() {
        return status.equals(STATUS_UPLOADING);
    }

    public boolean isComplete() {
        return status.equals(STATUS_FINISHED);
    }

    public boolean isPaused() {
        return status.equals(STATUS_PAUSED);
    }

    public boolean isQueued() {
        return status.equals(STATUS_QUEUED);
    }

    public boolean isFinished() {
        return status.equals(STATUS_FINISHED) || status.equals(STATUS_FAILED) || status.equals(STATUS_STOPPED);
    }

    public void setStatus(String status) {
        this.status = status;
        // notify the download pool of a significant change
        UploadPool.set(this);
    }

    public void start() {
        // only start if the download is not already finished and is paused
        if (isFinished()) {
            return;
        }
        // don't want thread state exceptions
        if (!isStarted()) {
            isStarted = true;
            // make sure it isn't paused
            if (addFileTool != null) {
                addFileTool.setPause(false);
            }
            // start the download
            uploadThread.start();
        }
        // notify
        setStatus(STATUS_UPLOADING);
    }

    public void queue() {
        // only queue if the download is not already finished and is paused
        if (isFinished()) {
            return;
        }
        // notify
        setStatus(STATUS_QUEUED);
    }

    public void resume() {
        // only resume if the download is not already finished and is paused
        if (isFinished()) {
            return;
        }
        // make sure to start if not started
        if (!isStarted()) {
            start();
        } else {
            // resume the gft directly
            addFileTool.setPause(false);
            // notify
            setStatus(STATUS_UPLOADING);
        }
    }

    public void pause() {
        // only pause if the download is not already finished
        if (isFinished()) {
            return;
        }
        // pause the gft directly
        addFileTool.setPause(true);
        // notify
        setStatus(STATUS_PAUSED);
    }

    public void stop() {
        // only stop if the download is not already finished
        if (isFinished()) {
            return;
        }
        // stop the gft directly
        addFileTool.stop();
        // also interrupt the download thread
        uploadThread.interrupt();
        // notify
        setStatus(STATUS_STOPPED);
    }

    public void retry() throws Exception {
        // only failed
        if (!isFinished() || !getStatus().equals(STATUS_FAILED)) {
            return;
        }
        // start a new equivalent upload
        UploadPool.set(new UploadSummary(this));
    }

    public void serialize(OutputStream out) throws Exception {
        RemoteUtil.writeInt(version, out);
        if (version == 1) {
            serializeVersionOne(out);
        } else if (version == 2) {
            serializeVersionTwo(out);
        } else {
            throw new Exception("Unrecognized upload summary version: " + version);
        }
    }

    private void serializeVersionOne(OutputStream out) throws Exception {
        // status
        RemoteUtil.writeLine(status, out);
        // action map
        RemoteUtil.writeInt(actionMap.size(), out);
        for (String key : actionMap.keySet()) {
            RemoteUtil.writeLine(key, out);
            RemoteUtil.writeInt(actionMap.get(key).size(), out);
            for (String key2 : actionMap.get(key).keySet()) {
                RemoteUtil.writeLine(key2, out);
                RemoteUtil.writeInt(actionMap.get(key).get(key2), out);
            }
        }
        // report
        RemoteUtil.writeBoolean(report != null, out);
        if (report != null) {
            report.serialize(out);
        }
        // parameters
        RemoteUtil.writeLine(addFileTool.getFile().getAbsolutePath(), out);
        RemoteUtil.writeLine(addFileTool.getTitle(), out);
        RemoteUtil.writeLine(addFileTool.getDescription(), out);
        RemoteUtil.writeData(addFileTool.getUserCertificate().getEncoded(), out);
        RemoteUtil.writeData(addFileTool.getUserPrivateKey().getEncoded(), out);
        RemoteUtil.writeLine(addFileTool.getPassphrase(), out);
        RemoteUtil.writeBoolean(addFileTool.isCompress(), out);
        RemoteUtil.writeBoolean(addFileTool.isDataOnly(), out);
        RemoteUtil.writeBoolean(addFileTool.isExplodeBeforeUpload(), out);
        RemoteUtil.writeBoolean(addFileTool.isShowMetaDataIfEncrypted(), out);
        RemoteUtil.writeBoolean(addFileTool.isUsingUnspecifiedServers(), out);
        RemoteUtil.writeInt(addFileTool.getThreadCount(), out);
        RemoteUtil.writeInt(addFileTool.getConfirmationEmails().size(), out);
        for (String email : addFileTool.getConfirmationEmails()) {
            RemoteUtil.writeLine(email, out);
        }
        RemoteUtil.writeInt(addFileTool.getServersToUse().size(), out);
        for (String host : addFileTool.getServersToUse()) {
            RemoteUtil.writeLine(host, out);
        }
        RemoteUtil.writeInt(addFileTool.getStickyServers().size(), out);
        for (String host : addFileTool.getStickyServers()) {
            RemoteUtil.writeLine(host, out);
        }
        RemoteUtil.writeInt(addFileTool.getMetaDataAnnotations().size(), out);
        for (MetaDataAnnotation mda : addFileTool.getMetaDataAnnotations()) {
            RemoteUtil.writeLine(mda.toString(), out);
        }
        RemoteUtil.writeBoolean(addFileTool.getLicense() != null, out);
        if (addFileTool.getLicense() != null) {
            addFileTool.getLicense().serialize(out);
        }
    }

    private void serializeVersionTwo(OutputStream out) throws Exception {
        // status
        RemoteUtil.writeLine(status, out);
        // action map
        RemoteUtil.writeInt(actionMap.size(), out);
        for (String key : actionMap.keySet()) {
            RemoteUtil.writeLine(key, out);
            RemoteUtil.writeInt(actionMap.get(key).size(), out);
            for (String key2 : actionMap.get(key).keySet()) {
                RemoteUtil.writeLine(key2, out);
                RemoteUtil.writeInt(actionMap.get(key).get(key2), out);
            }
        }
        // report
        RemoteUtil.writeBoolean(report != null, out);
        if (report != null) {
            report.serialize(out);
        }
        // parameters
        RemoteUtil.writeLong(timeStarted, out);
        RemoteUtil.writeLine(addFileTool.getFile().getAbsolutePath(), out);
        RemoteUtil.writeLine(addFileTool.getTitle(), out);
        RemoteUtil.writeLine(addFileTool.getDescription(), out);
        RemoteUtil.writeData(addFileTool.getUserCertificate().getEncoded(), out);
        RemoteUtil.writeData(addFileTool.getUserPrivateKey().getEncoded(), out);
        RemoteUtil.writeLine(addFileTool.getPassphrase(), out);
        RemoteUtil.writeBoolean(addFileTool.isCompress(), out);
        RemoteUtil.writeBoolean(addFileTool.isDataOnly(), out);
        RemoteUtil.writeBoolean(addFileTool.isExplodeBeforeUpload(), out);
        RemoteUtil.writeBoolean(addFileTool.isShowMetaDataIfEncrypted(), out);
        RemoteUtil.writeBoolean(addFileTool.isUsingUnspecifiedServers(), out);
        RemoteUtil.writeInt(addFileTool.getThreadCount(), out);
        RemoteUtil.writeInt(addFileTool.getConfirmationEmails().size(), out);
        for (String email : addFileTool.getConfirmationEmails()) {
            RemoteUtil.writeLine(email, out);
        }
        RemoteUtil.writeInt(addFileTool.getServersToUse().size(), out);
        for (String host : addFileTool.getServersToUse()) {
            RemoteUtil.writeLine(host, out);
        }
        RemoteUtil.writeInt(addFileTool.getStickyServers().size(), out);
        for (String host : addFileTool.getStickyServers()) {
            RemoteUtil.writeLine(host, out);
        }
        RemoteUtil.writeInt(addFileTool.getMetaDataAnnotations().size(), out);
        for (MetaDataAnnotation mda : addFileTool.getMetaDataAnnotations()) {
            RemoteUtil.writeLine(mda.toString(), out);
        }
        RemoteUtil.writeBoolean(addFileTool.getLicense() != null, out);
        if (addFileTool.getLicense() != null) {
            addFileTool.getLicense().serialize(out);
        }
    }

    public void deserialize(InputStream in) throws Exception {
        version = RemoteUtil.readInt(in);
        if (version == 1) {
            deserializeVersionOne(in);
        } else if (version == 2) {
            deserializeVersionTwo(in);
        } else {
            throw new Exception("Unrecognized upload summary version: " + version);
        }
    }

    private void deserializeVersionOne(InputStream in) throws Exception {
        // status
        status = RemoteUtil.readLine(in);
        // action map
        int actionMapSize = RemoteUtil.readInt(in);
        for (int i = 0; i < actionMapSize; i++) {
            String key = RemoteUtil.readLine(in);
            actionMap.put(key, new HashMap<String, Integer>());
            int actionMapInnerSize = RemoteUtil.readInt(in);
            for (int j = 0; j < actionMapInnerSize; j++) {
                String key2 = RemoteUtil.readLine(in);
                int value = RemoteUtil.readInt(in);
                actionMap.get(key).put(key2, value);
            }
        }
        // report
        boolean reportNotNull = RemoteUtil.readBoolean(in);
        if (reportNotNull) {
            report = new AddFileToolReport(in);
            if (report.isFailed()) {
                progressBar.errorCount++;
            }
        }
        // parameters
        addFileTool.setFile(new File(RemoteUtil.readLine(in)));
        addFileTool.setTitle(RemoteUtil.readLine(in));
        addFileTool.setDescription(RemoteUtil.readLine(in));
        addFileTool.setUserCertificate(SecurityUtil.getCertificate(RemoteUtil.readDataBytes(in)));
        addFileTool.setUserPrivateKey(SecurityUtil.getPrivateKey(RemoteUtil.readDataBytes(in)));
        addFileTool.setPassphrase(RemoteUtil.readLine(in));
        addFileTool.setCompress(RemoteUtil.readBoolean(in));
        addFileTool.setDataOnly(RemoteUtil.readBoolean(in));
        addFileTool.setExplodeBeforeUpload(RemoteUtil.readBoolean(in));
        addFileTool.setShowMetaDataIfEncrypted(RemoteUtil.readBoolean(in));
        addFileTool.setUseUnspecifiedServers(RemoteUtil.readBoolean(in));
        addFileTool.setThreadCount(RemoteUtil.readInt(in));
        int emailCount = RemoteUtil.readInt(in);
        for (int i = 0; i < emailCount; i++) {
            addFileTool.addConfirmationEmail(RemoteUtil.readLine(in));
        }
        int serversCount = RemoteUtil.readInt(in);
        for (int i = 0; i < serversCount; i++) {
            addFileTool.addServerToUse(RemoteUtil.readLine(in));
        }
        int stickyServersCount = RemoteUtil.readInt(in);
        for (int i = 0; i < stickyServersCount; i++) {
            addFileTool.addStickyServer(RemoteUtil.readLine(in));
        }
        int metaDataAnnotationCount = RemoteUtil.readInt(in);
        for (int i = 0; i < metaDataAnnotationCount; i++) {
            addFileTool.addMetaDataAnnotation(MetaDataAnnotation.createFromString(RemoteUtil.readLine(in)));
        }
        boolean licenseNotNull = RemoteUtil.readBoolean(in);
        if (licenseNotNull) {
            addFileTool.setLicense(new License(in));
        }
    }

    private void deserializeVersionTwo(InputStream in) throws Exception {
        // status
        status = RemoteUtil.readLine(in);
        // action map
        int actionMapSize = RemoteUtil.readInt(in);
        for (int i = 0; i < actionMapSize; i++) {
            String key = RemoteUtil.readLine(in);
            actionMap.put(key, new HashMap<String, Integer>());
            int actionMapInnerSize = RemoteUtil.readInt(in);
            for (int j = 0; j < actionMapInnerSize; j++) {
                String key2 = RemoteUtil.readLine(in);
                int value = RemoteUtil.readInt(in);
                actionMap.get(key).put(key2, value);
            }
        }
        // report
        boolean reportNotNull = RemoteUtil.readBoolean(in);
        if (reportNotNull) {
            report = new AddFileToolReport(in);
        }
        // parameters
        timeStarted = RemoteUtil.readLong(in);
        addFileTool.setFile(new File(RemoteUtil.readLine(in)));
        addFileTool.setTitle(RemoteUtil.readLine(in));
        addFileTool.setDescription(RemoteUtil.readLine(in));
        addFileTool.setUserCertificate(SecurityUtil.getCertificate(RemoteUtil.readDataBytes(in)));
        addFileTool.setUserPrivateKey(SecurityUtil.getPrivateKey(RemoteUtil.readDataBytes(in)));
        addFileTool.setPassphrase(RemoteUtil.readLine(in));
        addFileTool.setCompress(RemoteUtil.readBoolean(in));
        addFileTool.setDataOnly(RemoteUtil.readBoolean(in));
        addFileTool.setExplodeBeforeUpload(RemoteUtil.readBoolean(in));
        addFileTool.setShowMetaDataIfEncrypted(RemoteUtil.readBoolean(in));
        addFileTool.setUseUnspecifiedServers(RemoteUtil.readBoolean(in));
        addFileTool.setThreadCount(RemoteUtil.readInt(in));
        int emailCount = RemoteUtil.readInt(in);
        for (int i = 0; i < emailCount; i++) {
            addFileTool.addConfirmationEmail(RemoteUtil.readLine(in));
        }
        int serversCount = RemoteUtil.readInt(in);
        for (int i = 0; i < serversCount; i++) {
            addFileTool.addServerToUse(RemoteUtil.readLine(in));
        }
        int stickyServersCount = RemoteUtil.readInt(in);
        for (int i = 0; i < stickyServersCount; i++) {
            addFileTool.addStickyServer(RemoteUtil.readLine(in));
        }
        int metaDataAnnotationCount = RemoteUtil.readInt(in);
        for (int i = 0; i < metaDataAnnotationCount; i++) {
            addFileTool.addMetaDataAnnotation(MetaDataAnnotation.createFromString(RemoteUtil.readLine(in)));
        }
        boolean licenseNotNull = RemoteUtil.readBoolean(in);
        if (licenseNotNull) {
            addFileTool.setLicense(new License(in));
        }
    }

    // the following are for the clipboardowner abstract
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
