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
package org.tranche.gui.server.monitor;

import org.tranche.gui.monitor.StatusPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericMenu;
import org.tranche.gui.GenericMenuBar;
import org.tranche.gui.GenericMenuItem;
import org.tranche.gui.monitor.Monitor;
import org.tranche.servers.ServerCallbackEvent;
import org.tranche.servers.ServerEvent;
import org.tranche.remote.RemoteTrancheServerListener;
import org.tranche.servers.ServerMessageDownEvent;
import org.tranche.servers.ServerMessageUpEvent;
import org.tranche.servers.ServerUtil;
import org.tranche.time.TimeUtil;
import org.tranche.util.Text;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServerMonitor extends Monitor {

    private String host = null;
    private RemoteTrancheServerListener serverListener = null;
    private GeneralPanel general;
    private CallbackPanel callback = new CallbackPanel();
    private UploadPanel upload = new UploadPanel();
    private DownloadPanel download = new DownloadPanel();
    private StatusPanel status;

    public ServerMonitor(String host) {
        this.host = host;
        serverListener = new RemoteTrancheServerListener() {

            public void serverConnect(ServerEvent se) {
                handleServerEvent(se);
            }

            public void serverBanned(ServerEvent se) {
                handleServerEvent(se);
            }

            public void serverUnbanned(ServerEvent se) {
                handleServerEvent(se);
            }

            public void upMessageCreated(ServerMessageUpEvent smue) {
                handleServerMessageUpEvent(smue);
            }

            public void upMessageStarted(ServerMessageUpEvent smue) {
                handleServerMessageUpEvent(smue);
            }

            public void upMessageSent(ServerMessageUpEvent smue) {
                handleServerMessageUpEvent(smue);
            }

            public void upMessageFailed(ServerMessageUpEvent smue) {
                handleServerMessageUpEvent(smue);
            }

            public void downMessageStarted(ServerMessageDownEvent smde) {
                handleServerMessageDownEvent(smde);
            }

            public void downMessageProgress(ServerMessageDownEvent smde) {
                handleServerMessageDownEvent(smde);
            }

            public void downMessageCompleted(ServerMessageDownEvent smde) {
                handleServerMessageDownEvent(smde);
            }

            public void downMessageFailed(ServerMessageDownEvent smde) {
                handleServerMessageDownEvent(smde);
            }

            public void requestCreated(ServerCallbackEvent sce) {
                handleServerCallbackEvent(sce);
            }

            public void requestFulfilled(ServerCallbackEvent sce) {
                handleServerCallbackEvent(sce);
            }

            public void requestFailed(ServerCallbackEvent sce) {
                handleServerCallbackEvent(sce);
            }
        };

        // start putting together the gui
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Server Monitor: " + host);
        setLayout(new BorderLayout());
        setSize(600, 400);

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
                        fileChooser.showSaveDialog(ServerMonitor.this);
                        File selectedFile = fileChooser.getSelectedFile();
                        if (selectedFile != null) {
                            try {
                                log.saveLogFileTo(selectedFile);
                            } catch (Exception ee) {
                                GenericOptionPane.showMessageDialog(ServerMonitor.this, "There was a problem saving the log file.", "Could Not Save", JOptionPane.ERROR_MESSAGE);
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
        tabbedPane.add(callback);
        tabbedPane.add(upload);
        tabbedPane.add(download);
        tabbedPane.add(log);
        add(tabbedPane, BorderLayout.CENTER);

        // put the status panel on the bottom
        status = new StatusPanel(this);
        add(status, BorderLayout.SOUTH);
    }

    public String getHost() {
        return host;
    }

    private void setStatus(String status) {
        this.status.setStatus(status);
    }

    private void handleServerEvent(ServerEvent se) {
        log.addMessage(se.toString());
        if (se.getType() == se.AUTO_BAN || se.getType() == se.AUTO_UNBAN || se.getType() == se.USER_BAN || se.getType() == se.USER_UNBAN) {
            setStatus("Idle");
        } else if (se.getType() == se.CONNECT) {
            setStatus("Connecting");
        } else {
            setStatus("Unknown");
        }
    }

    private void handleServerMessageUpEvent(ServerMessageUpEvent smue) {
        upload.handleServerMessageUpEvent(smue);
        log.addMessage(smue.toString());
        if (smue.getType() == smue.TYPE_CREATED) {
            setStatus("Waiting to upload data from callback #" + smue.getCallbackId());
        } else if (smue.getType() == smue.TYPE_STARTED) {
            setStatus("Uploading data from callback #" + smue.getCallbackId());
        } else if (smue.getType() == smue.TYPE_COMPLETED) {
            String moreThanOne = "";
            setStatus("Waiting for response from server for ? callback" + moreThanOne + ".");
        } else {
            setStatus("Unknown");
        }
    }

    private void handleServerMessageDownEvent(ServerMessageDownEvent smde) {
        download.handleServerMessageDownEvent(smde);
        log.addMessage(smde.toString());
        if (smde.getType() == smde.TYPE_STARTED || smde.getType() == smde.TYPE_PROGRESS) {
            setStatus("Downloading response from server for callback #" + smde.getCallbackId());
        } else if (smde.getType() == smde.TYPE_COMPLETED) {
            setStatus("Idle");
        } else {
            setStatus("Unknown");
        }
    }

    private void handleServerCallbackEvent(ServerCallbackEvent sce) {
        callback.handleServerCallbackEvent(sce);
        log.addMessage(sce.toString());
        if (sce.getType() == sce.TYPE_CREATED) {
            setStatus("Waiting to upload bytes for callback #" + sce.getId());
        } else if (sce.getType() == sce.TYPE_FULFILLED || sce.getType() == sce.TYPE_FAILED) {
            setStatus("Idle");
        } else {
            setStatus("Unknown");
        }
    }

    /**
     * <p>If not already running, will begin listening to the communications between client and server.</p>
     */
    public void start() {
        if (!isListening()) {
            log.start();
            log.addMessage("Listening to communication with " + host + " at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            ServerUtil.addServerListener(host, serverListener);
            setListening(true);
            general.start();
            setStatus("  Idle");
        }
    }

    /**
     * <p>Pausing the server monitor will stop the monitor from listening to the server communication.</p>
     */
    public void stop() {
        if (isListening()) {
            ServerUtil.removeServerListener(host, serverListener);
            setListening(false);
            log.addMessage("Stopped listening to communication with " + host + " at " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
            general.stop();
            setStatus("  NOT LISTENING");
            log.stop();
        }
    }
}
