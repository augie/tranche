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
package org.tranche.gui.server;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.tranche.TrancheServer;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.GenericButton;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.gui.GenericPopupListener;
import org.tranche.gui.GenericTable;
import org.tranche.gui.GenericTextField;
import org.tranche.gui.IconRenderer;
import org.tranche.gui.IndeterminateProgressBar;
import org.tranche.gui.server.monitor.ServerMonitor;
import org.tranche.gui.util.GUIUtil;
import org.tranche.network.ConnectionUtil;
import org.tranche.time.TimeUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.ThreadUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServersTable extends GenericTable {

    private ServersTableModel model = new ServersTableModel();
    private ServerPopupMenu serverPopupMenu = new ServerPopupMenu();

    public ServersTable() {
        this(true);
    }

    public ServersTable(boolean showUse) {
        model.setTable(this);
        model.setShowUseColumn(showUse);
        setModel(model);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        addMouseListener(new GenericPopupListener(serverPopupMenu));
        if (showUse) {
            addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(final MouseEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            if (columnAtPoint(e.getPoint()) == getColumnModel().getColumnIndex(ServersTableModel.COLUMN_USE)) {
                                model.clickUse(rowAtPoint(e.getPoint()));
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
        }
        // set up the initial contents of the table
        model.setFilter("");
    }

    private void updateColumnRendering() {
        // size of columns
        int c = 3;
        if (model.getShowUseColumn()) {
            getColumnModel().getColumn(0).setMaxWidth(20);
            getColumnModel().getColumn(0).setMinWidth(20);
            getColumnModel().getColumn(0).setWidth(20);
            c++;
        }
        getColumnModel().getColumn(c).setMaxWidth(20);
        getColumnModel().getColumn(c).setMinWidth(20);
        getColumnModel().getColumn(c++).setWidth(20);
        getColumnModel().getColumn(c).setMaxWidth(20);
        getColumnModel().getColumn(c).setMinWidth(20);
        getColumnModel().getColumn(c++).setWidth(20);

        // icons
        c -= 2;
        getColumnModel().getColumn(c++).setCellRenderer(new IconRenderer("/org/tranche/gui/image/internet-web-browser.gif"));
        getColumnModel().getColumn(c++).setCellRenderer(new IconRenderer("/org/tranche/gui/image/system-software-update.gif"));
    }

    /**
     * Something stupid is happening. This fixes it.
     * @param g
     */
    @Override
    public void paint(Graphics g) {
        updateColumnRendering();
        super.paint(g);
    }

    @Override
    public ServersTableModel getModel() {
        return model;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        String returnVal = null;
        if (row >= 0) {
            int column = columnAtPoint(e.getPoint());
            if (!model.getShowUseColumn()) {
                column++;
            } else if (column == 4) {
                if (model.isOnline(row)) {
                    returnVal = "Online";
                } else {
                    returnVal = "Offline";
                }
            } else if (column == 5) {
                if (model.isConnected(row)) {
                    returnVal = "Connected";
                } else {
                    returnVal = "Not Connected";
                }
            }
        }

        // if the text for the tool tip text is too long, shorten it.
        if (returnVal != null) {
            if (returnVal.length() > 50) {
                returnVal = returnVal.substring(0, 50) + "...";
            } else if (returnVal.equals("")) {
                returnVal = null;
            }
        }

        return returnVal == null ? null : returnVal;
    }

    public Collection<String> getSelectedHosts() {
        Set<String> servers = new HashSet<String>();
        int[] rows = getSelectedRows();
        for (int index = rows.length - 1; index >= 0; index--) {
            servers.add(model.getHost(rows[index]));
        }
        return Collections.unmodifiableCollection(servers);
    }

    public void monitorSelected(Component relativeTo) {

        final int serverCount = getSelectedHosts().size();
        if (serverCount > 1) {
            Component popupRelativeTo = relativeTo;
            if (popupRelativeTo == null) {
                popupRelativeTo = ServersTable.this;
            }
            int returnValue = GenericOptionPane.showConfirmDialog(relativeTo, "You selected to open " + serverCount + " server monitors. Continue?", "Open " + serverCount + " monitors?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (returnValue != JOptionPane.YES_OPTION) {
                return;
            }
        }

        for (String host : getSelectedHosts()) {
            ServerMonitor monitor = new ServerMonitor(host);
            if (relativeTo == null) {
                GUIUtil.centerOnScreen(monitor);
            } else {
                monitor.setLocationRelativeTo(relativeTo);
            }
            monitor.setVisible(true);
            monitor.start();
        }
    }

    public void shutDownSelected(Component relativeTo) {
        Set<Exception> exceptions = new HashSet<Exception>();
        for (String host : getSelectedHosts()) {
            try {
                TrancheServer ts = ConnectionUtil.connectHost(host, true);
                if (ts == null) {
                    throw new Exception("Could not connect to " + host);
                }
                try {
                    IOUtil.requestShutdown(ts, GUIUtil.getUser().getCertificate(), GUIUtil.getUser().getPrivateKey());
                } finally {
                    ConnectionUtil.unlockConnection(host);
                }
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            ErrorFrame ef = new ErrorFrame();
            ef.show(exceptions, relativeTo);
        } else {
            GenericOptionPane.showMessageDialog(relativeTo, "Server successfully shut down.");
        }
    }

    public void registerServerOnSelected(Component relativeTo) {
        JPanel panel = new JPanel();
        final GenericPopupFrame frame = new GenericPopupFrame("Server URL", panel);
        GenericTextField textField = new GenericTextField();
        {
            panel.setLayout(new BorderLayout());
            textField.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false);
                }
            });
            panel.add(textField, BorderLayout.CENTER);
            GenericButton button = new GenericButton("Register Server");
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false);
                }
            });
            panel.add(button, BorderLayout.SOUTH);
        }
        frame.setSize(300, 125);
        if (relativeTo == null) {
            GUIUtil.centerOnScreen(frame);
        } else {
            frame.setLocationRelativeTo(relativeTo);
        }
        frame.setVisible(true);
        while (frame.isVisible()) {
            ThreadUtil.safeSleep(250);
        }
        String url = textField.getText();
        if (url != null && !url.equals("")) {
            IndeterminateProgressBar progress = new IndeterminateProgressBar("Registering Server");
            if (relativeTo == null) {
                GUIUtil.centerOnScreen(progress);
            } else {
                progress.setLocationRelativeTo(relativeTo);
            }
            progress.start();
            Set<Exception> exceptions = new HashSet<Exception>();
            for (String host : getSelectedHosts()) {
                try {
                    TrancheServer ts = ConnectionUtil.connectHost(host, true);
                    if (ts == null) {
                        throw new Exception("Could not connect to " + host);
                    }
                    try {
                        ts.registerServer(url);
                    } finally {
                        ConnectionUtil.unlockConnection(host);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
            progress.stop();
            if (!exceptions.isEmpty()) {
                ErrorFrame ef = new ErrorFrame();
                ef.show(exceptions, relativeTo);
            } else {
                GenericOptionPane.showMessageDialog(relativeTo, "Server successfully registered.");
            }
        }
    }

    public void connectToSelected(Component relativeTo) {
        IndeterminateProgressBar progress = new IndeterminateProgressBar("Connecting");
        if (relativeTo == null) {
            GUIUtil.centerOnScreen(progress);
        } else {
            progress.setLocationRelativeTo(relativeTo);
        }
        progress.start();
        Set<Exception> exceptions = new HashSet<Exception>();
        for (String host : getSelectedHosts()) {
            try {
                TrancheServer ts = ConnectionUtil.connectHost(host, false);
                if (ts == null) {
                    throw new Exception("Could not connect to " + host);
                }
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        progress.stop();
        if (!exceptions.isEmpty()) {
            ErrorFrame ef = new ErrorFrame();
            ef.show(exceptions, relativeTo);
        }
    }

    public void pingServers(final String[] hosts) {
        if (hosts.length == 0) {
            return;
        }
        Thread t = new Thread("Ping server thread") {

            @Override
            public void run() {
                List<Thread> pingThreads = new LinkedList<Thread>();

                // Initialize to avoid null pointer exceptions
                final Integer avgTimes[] = new Integer[hosts.length];
                for (int i = 0; i < avgTimes.length; i++) {
                    avgTimes[i] = -1;
                }

                for (int i = 0; i < hosts.length; i++) {
                    final int hostNum = i;
                    Thread t = new Thread("Ping") {

                        @Override()
                        public void run() {
                            try {
                                TrancheServer ts = ConnectionUtil.connectHost(hosts[hostNum], true);
                                if (ts != null) {
                                    try {
                                        // get in as many pings as possible
                                        int avg = 0;
                                        int pings = 0;
                                        for (int i = 0; i < 3; i++) {
                                            try {
                                                long start = TimeUtil.getTrancheTimestamp();
                                                ts.ping();
                                                avg += TimeUtil.getTrancheTimestamp() - start;
                                                pings++;
                                                avgTimes[hostNum] = avg / pings;
                                            } catch (Exception e) {
                                            }
                                        }
                                    } finally {
                                        ConnectionUtil.unlockConnection(hosts[hostNum]);
                                    }
                                }
                            } catch (Exception e) {
                                ConnectionUtil.reportExceptionHost(hosts[hostNum], e);
                            }
                        }
                    };
                    pingThreads.add(i, t);
                    pingThreads.get(i).setDaemon(true);
                    pingThreads.get(i).start();
                }

                // wait for the threads to finish pinging
                ThreadUtil.wait(pingThreads, 3000);

                String text = "Ping results:\n";
                for (int i = 0; i < hosts.length; i++) {
                    text = text + "   " + hosts[i] + "   -   ";
                    if (avgTimes != null && avgTimes[i] >= 0) {
                        text = text + avgTimes[i] + " ms\n";
                    } else {
                        text = text + "No response\n";
                    }
                }

                GenericOptionPane.showMessageDialog(
                        GUIUtil.getAdvancedGUI(),
                        text,
                        "Ping Results",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };
        t.setDaemon(true);
        t.start();
    }

    private class ServerPopupMenu extends JPopupMenu {

        private JMenuItem connectMenuItem = new JMenuItem("Connect");
        private JMenuItem pingMenuItem = new JMenuItem("Ping");
        private JMenuItem monitorMenuItem = new JMenuItem("Monitor");
        private JMenuItem registerServerMenuItem = new JMenuItem("Register Server");
        private JMenuItem shutDownMenuItem = new JMenuItem("Shut Down");

        public ServerPopupMenu() {
            connectMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            connectToSelected(ServersTable.this);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            pingMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            pingServers(getSelectedHosts().toArray(new String[0]));
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            monitorMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            monitorSelected(ServersTable.this);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            registerServerMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            registerServerOnSelected(ServersTable.this);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            shutDownMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        @Override
                        public void run() {
                            shutDownSelected(ServersTable.this);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });

            add(connectMenuItem);
            add(pingMenuItem);
            add(monitorMenuItem);
            add(registerServerMenuItem);
            add(shutDownMenuItem);

            addPopupMenuListener(new PopupMenuListener() {

                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    Thread t = new Thread() {

                        @Override()
                        public void run() {
                            int rows[] = getSelectedRows();
                            connectMenuItem.setEnabled(rows.length > 0);
                            pingMenuItem.setEnabled(rows.length > 0);
                            monitorMenuItem.setEnabled(rows.length > 0);
                            registerServerMenuItem.setEnabled(rows.length > 0);
                            shutDownMenuItem.setEnabled(rows.length > 0 && GUIUtil.getUser() != null);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }
            });
        }
    }
}
