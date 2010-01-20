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
package org.tranche.gui.add.monitor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import org.tranche.add.AddFileToolEvent;
import org.tranche.add.AddFileToolListener;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericMenu;
import org.tranche.gui.GenericMenuBar;
import org.tranche.gui.GenericMenuItem;
import org.tranche.gui.add.UploadSummary;
import org.tranche.gui.monitor.Monitor;
import org.tranche.gui.monitor.StatusPanel;
import org.tranche.gui.util.GUIUtil;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.util.Text;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class UploadMonitor extends Monitor {

    private UploadSummary us;
    private GeneralPanel general;
    private FilePanel file = new FilePanel();
    private ChunkPanel chunk = new ChunkPanel();
    private MetaDataPanel metadata = new MetaDataPanel();
    private ServerPanel server = new ServerPanel();
    private StatusPanel status;
    private AddFileToolListener listener;

    public UploadMonitor(UploadSummary us) {
        this.us = us;
        listener = new AddFileToolListener() {

            public void message(String msg) {
                log.addMessage(msg);
            }

            public void startedMetaData(AddFileToolEvent event) {
                handleMetaDataEvent(event);
            }

            public void tryingMetaData(AddFileToolEvent event) {
                handleMetaDataEvent(event);
            }

            public void uploadedMetaData(AddFileToolEvent event) {
                handleChunkEvent(event);
            }

            public void failedMetaData(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                handleMetaDataEvent(event);
            }

            public void finishedMetaData(AddFileToolEvent event) {
                handleMetaDataEvent(event);
            }

            public void startingData(AddFileToolEvent event) {
                handleChunkEvent(event);
            }

            public void startedData(AddFileToolEvent event) {
                handleChunkEvent(event);
            }

            public void tryingData(AddFileToolEvent event) {
                handleChunkEvent(event);
            }

            public void uploadedData(AddFileToolEvent event) {
                handleChunkEvent(event);
            }

            public void failedData(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                handleChunkEvent(event);
            }

            public void finishedData(AddFileToolEvent event) {
                handleChunkEvent(event);
            }

            public void startedFile(AddFileToolEvent event) {
                handleFileEvent(event);
            }

            public void finishedFile(AddFileToolEvent event) {
                handleFileEvent(event);
            }

            public void failedFile(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                handleFileEvent(event);
            }

            public void startedDirectory(AddFileToolEvent event) {
                handleDirectoryEvent(event);
            }

            public void finishedDirectory(AddFileToolEvent event) {
                handleDirectoryEvent(event);
            }

            public void failedDirectory(AddFileToolEvent event, Collection<PropagationExceptionWrapper> exceptions) {
                handleDirectoryEvent(event);
            }
        };

        // start putting together the gui
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Upload Monitor");
        setLayout(new BorderLayout());
        setSize(600, 500);

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
                        fileChooser.showSaveDialog(UploadMonitor.this);
                        File selectedFile = fileChooser.getSelectedFile();
                        if (selectedFile != null) {
                            try {
                                log.saveLogFileTo(selectedFile);
                            } catch (Exception ee) {
                                GenericOptionPane.showMessageDialog(UploadMonitor.this, "There was a problem saving the log file.", "Could Not Save", JOptionPane.ERROR_MESSAGE);
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

    public void handleDirectoryEvent(AddFileToolEvent event) {
        log.addMessage(event.toString());
        if (event.getAction() == AddFileToolEvent.ACTION_FINISHED) {
            setStatus("Upload Finished");
        }
    }

    public void handleFileEvent(AddFileToolEvent event) {
        log.addMessage(event.toString());
        file.handleFileEvent(event);
        resetStatus();
    }

    public void handleChunkEvent(AddFileToolEvent event) {
        log.addMessage(event.toString());
        server.handleEvent(event);
        chunk.handleChunkEvent(event);
        resetStatus();
    }

    public void handleMetaDataEvent(AddFileToolEvent event) {
        log.addMessage(event.toString());
        server.handleEvent(event);
        metadata.handleMetaDataEvent(event);
        resetStatus();
    }

    /**
     * 
     * @return
     */
    public UploadSummary getUploadSummary() {
        return us;
    }

    /**
     * 
     * @param status
     */
    private void setStatus(String status) {
        this.status.setStatus(status);
    }

    /**
     * 
     */
    private void resetStatus() {
        int c_started = us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_DATA), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_STARTED));
        int c_finished = us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_DATA), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_FINISHED));
        int c_failed = us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_DATA), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_FAILED));
        int md_started = us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_METADATA), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_STARTED));
        int md_finished = us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_METADATA), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_FINISHED));
        int md_failed = us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_METADATA), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_FAILED));
        int f_finished = us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_FILE), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_FINISHED));
        int f_failed = us.getActionCount(AddFileToolEvent.staticGetTypeString(AddFileToolEvent.TYPE_FILE), AddFileToolEvent.staticGetActionString(AddFileToolEvent.ACTION_FAILED));
        setStatus("Data: " + c_started + " - " + c_finished + " - " + c_failed + "   Meta Data: " + md_started + " - " + md_finished + " - " + md_failed + "   Files: " + f_finished + " - " + f_failed);
    }

    /**
     * <p>If not already running, will begin listening to the communications between client and server.</p>
     */
    public void start() {
        if (!isListening()) {
            log.start();
            log.addMessage("Listening at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            us.getAddFileTool().addListener(listener);
            setStatus("  Idle");
            setListening(true);
            general.start();
        }
    }

    /**
     * <p>Pausing the server monitor will stop the monitor from listening to the server communication.</p>
     */
    public void stop() {
        if (isListening()) {
            log.addMessage("Stopped listening at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            us.getAddFileTool().removeListener(listener);
            setListening(false);
            setStatus("  NOT LISTENING");
            general.stop();
            log.stop();
        }
    }
}
