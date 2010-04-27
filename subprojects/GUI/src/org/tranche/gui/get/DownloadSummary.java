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
package org.tranche.gui.get;

import java.awt.Component;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import org.tranche.get.GetFileTool;
import org.tranche.get.GetFileToolEvent;
import org.tranche.get.GetFileToolListener;
import org.tranche.get.GetFileToolReport;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.get.monitor.DownloadMonitor;
import org.tranche.gui.project.ProjectPool;
import org.tranche.gui.util.GUIUtil;
import org.tranche.meta.MetaData;
import org.tranche.project.ProjectSummary;
import org.tranche.remote.RemoteUtil;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.commons.ThreadUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class DownloadSummary implements ClipboardOwner {

    public static final String STATUS_STARTING = "Starting";
    public static final String STATUS_QUEUED = "Queued";
    public static final String STATUS_DOWNLOADING = "Downloading";
    public static final String STATUS_PAUSED = "Paused";
    public static final String STATUS_STOPPED = "Stopped";
    public static final String STATUS_FINISHED = "Finished";
    public static final String STATUS_FAILED = "Failed";
    private int version = 1;
    private final GetFileTool getFileTool;
    private GetFileToolProgressBar progressBar;
    private GetFileToolReport report;
    private String status = STATUS_STARTING;
    public long filesToDownload = -1, bytesToDownload = -1;
    private final Map<String, Map<String, Integer>> actionMap = new HashMap<String, Map<String, Integer>>();
    private boolean isStarted = false;
    private ErrorFrame errorFrame = null;
    private DownloadMonitor monitor = null;
    private Thread downloadThread = new Thread("Download Thread") {

        @Override
        public void run() {
            try {
                clearActionCount();
                getFileTool.addListener(new GetFileToolListener() {

                    private void handleAction(GetFileToolEvent event) {
                        synchronized (actionMap) {
                            if (!actionMap.containsKey(GetFileToolEvent.staticGetTypeString(event.getType()))) {
                                actionMap.put(GetFileToolEvent.staticGetTypeString(event.getType()), new HashMap<String, Integer>());
                            }
                            if (!actionMap.get(GetFileToolEvent.staticGetTypeString(event.getType())).containsKey(GetFileToolEvent.staticGetActionString(event.getAction()))) {
                                actionMap.get(GetFileToolEvent.staticGetTypeString(event.getType())).put(GetFileToolEvent.staticGetActionString(event.getAction()), 0);
                            }
                            int value = actionMap.get(GetFileToolEvent.staticGetTypeString(event.getType())).get(GetFileToolEvent.staticGetActionString(event.getAction()));
                            value++;
                            actionMap.get(GetFileToolEvent.staticGetTypeString(event.getType())).put(GetFileToolEvent.staticGetActionString(event.getAction()), value);
                        }
                    }

                    public void message(String msg) {
                    }

                    public void startedMetaData(GetFileToolEvent event) {
                        handleAction(event);
                    }

                    public void tryingMetaData(GetFileToolEvent event) {
                        handleAction(event);
                    }

                    public void failedMetaData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                        handleAction(event);
                        DownloadPool.set(DownloadSummary.this);
                    }

                    public void finishedMetaData(GetFileToolEvent event) {
                        handleAction(event);
                    }

                    public void startedData(GetFileToolEvent event) {
                        handleAction(event);
                    }

                    public void tryingData(GetFileToolEvent event) {
                        handleAction(event);
                    }

                    public void failedData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                        handleAction(event);
                        DownloadPool.set(DownloadSummary.this);
                    }

                    public void finishedData(GetFileToolEvent event) {
                        handleAction(event);
                        DownloadPool.set(DownloadSummary.this);
                    }

                    public void startingFile(GetFileToolEvent event) {
                        handleAction(event);
                    }

                    public void startedFile(GetFileToolEvent event) {
                        handleAction(event);
                    }

                    public void skippedFile(GetFileToolEvent event) {
                        handleAction(event);
                        DownloadPool.set(DownloadSummary.this);
                    }

                    public void finishedFile(GetFileToolEvent event) {
                        handleAction(event);
                        DownloadPool.set(DownloadSummary.this);
                    }

                    public void failedFile(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                        handleAction(event);
                        DownloadPool.set(DownloadSummary.this);
                    }

                    public void startingDirectory(GetFileToolEvent event) {
                        handleAction(event);
                    }

                    public void startedDirectory(GetFileToolEvent event) {
                        handleAction(event);
                    }

                    public void finishedDirectory(GetFileToolEvent event) {
                        handleAction(event);
                        DownloadPool.set(DownloadSummary.this);
                    }

                    public void failedDirectory(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                        handleAction(event);
                        DownloadPool.set(DownloadSummary.this);
                    }
                });
                MetaData metaData = getFileTool.getMetaData();
                if (metaData.isProjectFile()) {
                    if (filesToDownload == -1 || bytesToDownload == -1) {
                        Thread t = new Thread() {

                            @Override
                            public void run() {
                                while (report == null) {
                                    if (getFileTool.getTimeEstimator() != null) {
                                        filesToDownload = getFileTool.getTimeEstimator().getTotalFiles();
                                        bytesToDownload = getFileTool.getTimeEstimator().getTotalBytes();
                                        if (filesToDownload > 1) {
                                            break;
                                        }
                                    }
                                    ThreadUtil.sleep(200);
                                }
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                    }
                    report = getFileTool.getDirectory();
                } else {
                    filesToDownload = 1;
                    bytesToDownload = getFileTool.getHash().getLength();
                    report = getFileTool.getFile();
                }
                if (report != null) {
                    if (report.isFinished() && !report.isFailed()) {
                        String name = "";
                        ProjectSummary ps = getProjectSummary();
                        if (ps == null || ps.title.trim().equals("")) {
                            name = getFileTool.getHash().toString().substring(0, 19) + "...";
                        } else {
                            if (ps.title.length() > 20) {
                                name = ps.title.substring(0, 19) + "...";
                            } else {
                                name = ps.title;
                            }
                        }
                        GenericOptionPane.showMessageDialog(null, "Downloaded " + name + " to:\n" + getFileTool.getSaveFile().getAbsolutePath(), "Download Finished", JOptionPane.PLAIN_MESSAGE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (report != null) {
                if (report.isFailed()) {
                    setStatus(STATUS_FAILED);
                    showErrorFrame(null);
                } else {
                    setStatus(STATUS_FINISHED);
                }
            } else {
                setStatus(STATUS_FAILED);
            }
        }
    };

    public DownloadSummary(GetFileTool getFileTool) {
        this.getFileTool = getFileTool;
        progressBar = new GetFileToolProgressBar(getFileTool);
        getFileTool.addListener(progressBar);
    }

    public DownloadSummary(InputStream in) throws Exception {
        this.getFileTool = new GetFileTool();
        progressBar = new GetFileToolProgressBar(getFileTool);
        getFileTool.addListener(progressBar);
        deserialize(in);
    }

    public void clearActionCount() {
        synchronized (actionMap) {
            actionMap.clear();
        }
    }

    public void diffActionCount(GetFileToolEvent event, int diff) {
        synchronized (actionMap) {
            if (actionMap.get(event.getTypeString()) == null) {
                actionMap.put(event.getTypeString(), new HashMap<String, Integer>());
            }
            if (actionMap.get(event.getTypeString()).get(event.getActionString()) == null) {
                actionMap.get(event.getTypeString()).put(event.getActionString(), 0);
            }
            actionMap.get(event.getTypeString()).put(event.getActionString(), actionMap.get(event.getTypeString()).get(event.getActionString()) + diff);
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
            try {
                Thread.sleep(200);
            } catch (Exception e) {
            }
        }
        // wait for finish
        while (!isFinished()) {
            try {
                downloadThread.join();
            } catch (Exception e) {
            }
        }
    }

    public void waitForFinish(long millis) {
        // return immediately in some cases
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
                downloadThread.join(millis - waited);
            } catch (Exception e) {
            }
            waited += TimeUtil.getTrancheTimestamp() - start;
        }
    }

    public void copyHash() {
        if (getFileTool != null && getFileTool.getHash() != null) {
            GUIUtil.copyToClipboard(getFileTool.getHash().toString(), this);
        }
    }

    public void showHash() {
        if (getFileTool != null && getFileTool.getHash() != null) {
            GUIUtil.showHash(getFileTool.getHash(), this, null);
        }
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
            monitor = new DownloadMonitor(this);
        }
        if (relativeTo == null) {
            GUIUtil.centerOnScreen(monitor);
        } else {
            monitor.setLocationRelativeTo(relativeTo);
        }
        monitor.setVisible(true);
        monitor.start();
    }

    public GetFileTool getGetFileTool() {
        return getFileTool;
    }

    public ProjectSummary getProjectSummary() {
        if (getFileTool == null) {
            return null;
        }
        if (!ProjectPool.contains(getFileTool.getHash())) {
            try {
                MetaData metaData = getFileTool.getMetaData();
                if (metaData.isProjectFile()) {
                    ProjectPool.set(new ProjectSummary(metaData, getFileTool.getHash()));
                }
            } catch (Exception e) {
            }
        }
        return ProjectPool.get(getFileTool.getHash(), getFileTool.getUploaderName(), getFileTool.getUploadTimestamp());
    }

    public GetFileToolReport getReport() {
        return report;
    }

    public String getStatus() {
        return status;
    }

    public GetFileToolProgressBar getProgressBar() {
        return progressBar;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public boolean isStarting() {
        return status.equals(STATUS_STARTING);
    }

    public boolean isDownloading() {
        return status.equals(STATUS_DOWNLOADING);
    }

    public boolean isComplete() {
        return status.equals(STATUS_FINISHED);
    }

    public boolean isFailed() {
        return status.equals(STATUS_FAILED);
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

    public void setStatus(String _status) {
        status = _status;
        if (status.equals(STATUS_QUEUED)) {
            progressBar.queued = true;
        } else {
            progressBar.queued = false;
        }
        if (status.equals(STATUS_PAUSED)) {
            progressBar.paused = true;
        } else {
            progressBar.paused = false;
        }
        if (status.equals(STATUS_STOPPED)) {
            progressBar.stopped = true;
        } else {
            progressBar.stopped = false;
        }
        if (status.equals(STATUS_FAILED) || status.equals(STATUS_FINISHED)) {
            progressBar.finished = true;
        } else {
            progressBar.finished = false;
        }
        // notify the download pool of a significant change
        DownloadPool.set(this);
    }

    public void start() {
        // only start if the download is not already finished and is paused
        if (isFinished()) {
            return;
        }
        // make sure the gft is not paused
        getFileTool.setPause(false);
        // don't want thread state exceptions
        if (!isStarted()) {
            isStarted = true;
            // start the download
            downloadThread.start();
        }
        // notify
        setStatus(STATUS_DOWNLOADING);
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
            getFileTool.setPause(false);
            // notify
            setStatus(STATUS_DOWNLOADING);
        }
    }

    public void pause() {
        // only pause if the download is not already finished
        if (isFinished()) {
            return;
        }
        // pause the gft directly
        getFileTool.setPause(true);
        // notify
        setStatus(STATUS_PAUSED);
    }

    public void stop() {
        // only stop if the download is not already finished
        if (isFinished()) {
            return;
        }
        // stop the gft directly
        getFileTool.stop();
        // also interrupt the download thread
        downloadThread.interrupt();
        // notify
        setStatus(STATUS_STOPPED);
    }

    public void serialize(OutputStream out) throws Exception {
        RemoteUtil.writeInt(version, out);
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
        RemoteUtil.writeBigHash(getFileTool.getHash(), out);
        RemoteUtil.writeLine(getFileTool.getPassphrase(), out);
        RemoteUtil.writeLine(getFileTool.getSaveFile().getAbsolutePath(), out);
        RemoteUtil.writeLine(getFileTool.getRegEx(), out);
        RemoteUtil.writeLine(getFileTool.getUploadRelativePath(), out);
        RemoteUtil.writeLine(getFileTool.getUploaderName(), out);
        RemoteUtil.writeLong(getFileTool.getUploadTimestamp(), out);
        RemoteUtil.writeInt(getFileTool.getThreadCount(), out);
        RemoteUtil.writeBoolean(getFileTool.isBatch(), out);
        RemoteUtil.writeBoolean(getFileTool.isContinueOnFailure(), out);
        RemoteUtil.writeBoolean(getFileTool.isUsingUnspecifiedServers(), out);
        RemoteUtil.writeBoolean(getFileTool.isValidate(), out);
        RemoteUtil.writeInt(getFileTool.getServersToUse().size(), out);
        for (String host : getFileTool.getServersToUse()) {
            RemoteUtil.writeLine(host, out);
        }
        RemoteUtil.writeLong(bytesToDownload, out);
        RemoteUtil.writeLong(filesToDownload, out);
    }

    public void deserialize(InputStream in) throws Exception {
        version = RemoteUtil.readInt(in);

        // VERSION ONE
        {
            // status
            status = RemoteUtil.readLine(in);
            if (status.equals(STATUS_QUEUED)) {
                progressBar.queued = true;
            } else {
                progressBar.queued = false;
            }
            if (status.equals(STATUS_PAUSED)) {
                progressBar.paused = true;
            } else {
                progressBar.paused = false;
            }
            if (status.equals(STATUS_STOPPED)) {
                progressBar.stopped = true;
            } else {
                progressBar.stopped = false;
            }
            if (status.equals(STATUS_FINISHED) || status.equals(STATUS_FAILED)) {
                progressBar.finished = true;
            } else {
                progressBar.finished = false;
            }
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
                report = new GetFileToolReport(in);
                if (report.isFailed()) {
                    progressBar.errorCount++;
                }
            }
            // parameters
            getFileTool.setHash(RemoteUtil.readBigHash(in));
            getFileTool.setPassphrase(RemoteUtil.readLine(in));
            getFileTool.setSaveFile(new File(RemoteUtil.readLine(in)));
            getFileTool.setRegEx(RemoteUtil.readLine(in));
            getFileTool.setUploadRelativePath(RemoteUtil.readLine(in));
            getFileTool.setUploaderName(RemoteUtil.readLine(in));
            getFileTool.setUploadTimestamp(RemoteUtil.readLong(in));
            getFileTool.setThreadCount(RemoteUtil.readInt(in));
            getFileTool.setBatch(RemoteUtil.readBoolean(in));
            getFileTool.setContinueOnFailure(RemoteUtil.readBoolean(in));
            getFileTool.setUseUnspecifiedServers(RemoteUtil.readBoolean(in));
            getFileTool.setValidate(RemoteUtil.readBoolean(in));
            int serversCount = RemoteUtil.readInt(in);
            for (int i = 0; i < serversCount; i++) {
                getFileTool.addServerToUse(RemoteUtil.readLine(in));
            }
            bytesToDownload = RemoteUtil.readLong(in);
            filesToDownload = RemoteUtil.readLong(in);
        }
    }

    // the following are for the clipboardowner abstract
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
