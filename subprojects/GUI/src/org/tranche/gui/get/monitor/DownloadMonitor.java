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
package org.tranche.gui.get.monitor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import org.tranche.get.GetFileToolEvent;
import org.tranche.get.GetFileToolListener;
import org.tranche.gui.get.DownloadSummary;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericMenu;
import org.tranche.gui.GenericMenuBar;
import org.tranche.gui.GenericMenuItem;
import org.tranche.gui.monitor.Monitor;
import org.tranche.gui.monitor.StatusPanel;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.util.Text;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class DownloadMonitor extends Monitor {

    private DownloadSummary ds;
    private GeneralPanel general;
    private FilePanel file = new FilePanel();
    private DataPanel chunk = new DataPanel();
    private MetaDataPanel metadata = new MetaDataPanel();
    private ServerPanel server = new ServerPanel();
    private StatusPanel status;
    private GetFileToolListener listener;

    public DownloadMonitor(DownloadSummary ds) {
        this.ds = ds;
        listener = new GetFileToolListener() {

            public void message(String msg) {
                log.addMessage(msg);
            }

            public void startedMetaData(GetFileToolEvent event) {
                handleMetaDataEvent(event);
            }

            public void tryingMetaData(GetFileToolEvent event) {
                handleMetaDataEvent(event);
            }

            public void failedMetaData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                handleMetaDataEvent(event);
            }

            public void finishedMetaData(GetFileToolEvent event) {
                handleMetaDataEvent(event);
            }

            public void startedData(GetFileToolEvent event) {
                handleDataEvent(event);
            }

            public void tryingData(GetFileToolEvent event) {
                handleDataEvent(event);
            }

            public void failedData(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                handleDataEvent(event);
            }

            public void finishedData(GetFileToolEvent event) {
                handleDataEvent(event);
            }

            public void startingFile(GetFileToolEvent event) {
                handleFileEvent(event);
            }

            public void startedFile(GetFileToolEvent event) {
                handleFileEvent(event);
            }

            public void skippedFile(GetFileToolEvent event) {
                handleFileEvent(event);
            }

            public void finishedFile(GetFileToolEvent event) {
                handleFileEvent(event);
            }

            public void failedFile(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                handleFileEvent(event);
            }

            public void startingDirectory(GetFileToolEvent event) {
                handleDirectoryEvent(event);
            }

            public void startedDirectory(GetFileToolEvent event) {
                handleDirectoryEvent(event);
            }

            public void finishedDirectory(GetFileToolEvent event) {
                handleDirectoryEvent(event);
            }

            public void failedDirectory(GetFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                handleDirectoryEvent(event);
            }
        };

        // start putting together the gui
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Download Monitor");
        setLayout(new BorderLayout());
        setSize(600, 440);

        // add a menu bar
        GenericMenuBar menuBar = new GenericMenuBar();

        // logs
        GenericMenu logMenu = new GenericMenu("Logs");
        GenericMenuItem saveLogMenuItem = new GenericMenuItem("Save Log");
        saveLogMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final JFileChooser fileChooser = GUIUtil.makeNewFileChooser();
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        fileChooser.showSaveDialog(DownloadMonitor.this);
                        File selectedFile = fileChooser.getSelectedFile();
                        if (selectedFile != null) {
                            try {
                                log.saveLogFileTo(selectedFile);
                            } catch (Exception ee) {
                                GenericOptionPane.showMessageDialog(DownloadMonitor.this, "There was a problem saving the log file.", "Could Not Save", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });

        logMenu.add(saveLogMenuItem);
        menuBar.add(logMenu);
        add(menuBar, BorderLayout.NORTH);

        // create the tabbed pane and add
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFocusable(false);
        general = new GeneralPanel(this);
        tabbedPane.add(general);
        tabbedPane.add(file);
        tabbedPane.add(chunk);
        tabbedPane.add(metadata);
        tabbedPane.add(server);
        tabbedPane.add(log);
        add(tabbedPane, BorderLayout.CENTER);

        // put the status panel on the bottom
        status = new StatusPanel(this);
        add(status, BorderLayout.SOUTH);
    }

    public DownloadSummary getDownloadSummary() {
        return ds;
    }

    private void setStatus(String status) {
        this.status.setStatus(status);
    }

    private void resetStatus() {
        int md_started = ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_METADATA), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_STARTED));
        int md_complete = ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_METADATA), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_FINISHED));
        int md_failed = ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_METADATA), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_FAILED));
        int c_started = ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_DATA), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_STARTED));
        int c_complete = ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_DATA), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_FINISHED));
        int c_failed = ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_DATA), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_FAILED));
        int f_skipped = ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_FILE), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_SKIPPED));
        int f_complete = ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_FILE), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_FINISHED));
        int f_failed = ds.getActionCount(GetFileToolEvent.staticGetTypeString(GetFileToolEvent.TYPE_FILE), GetFileToolEvent.staticGetActionString(GetFileToolEvent.ACTION_FAILED));
        setStatus("Meta Data: " + md_started + " - " + md_complete + " - " + md_failed + "   Data: " + c_started + " - " + c_complete + " - " + c_failed + "   Files: " + f_skipped + " - " + f_complete + " - " + f_failed);
    }

    private void handleMetaDataEvent(GetFileToolEvent event) {
        log.addMessage(event.toString());
        server.handleEvent(event);
        metadata.handleMetaDataEvent(event);
        resetStatus();
    }

    private void handleDataEvent(GetFileToolEvent event) {
        log.addMessage(event.toString());
        server.handleEvent(event);
        chunk.handleDataEvent(event);
        resetStatus();
    }

    private void handleFileEvent(GetFileToolEvent event) {
        log.addMessage(event.toString());
        file.handleFileEvent(event);
        resetStatus();
    }

    private void handleDirectoryEvent(GetFileToolEvent event) {
        log.addMessage(event.toString());
        if (event.getAction() == GetFileToolEvent.ACTION_FINISHED) {
            setStatus("Download Finished");
        }
        resetStatus();
    }

    /**
     * <p>If not already running, will begin listening to the communications between client and server.</p>
     */
    public void start() {
        if (!isListening()) {
            log.start();
            log.addMessage("Listening at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            ds.getGetFileTool().addListener(listener);
            setListening(true);
            resetStatus();
            general.start();
        }
    }

    /**
     * <p>Pausing the server monitor will stop the monitor from listening to the server communication.</p>
     */
    public void stop() {
        if (isListening()) {
            log.addMessage("Stopped listening at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            ds.getGetFileTool().removeListener(listener);
            setListening(false);
            setStatus("  NOT LISTENING");
            general.stop();
            log.stop();
        }
    }

    /**
     * <p>Performs a cleanup on memory and disk.</p>
     */
    @Override
    public void clean() {
        super.clean();
    }
}
