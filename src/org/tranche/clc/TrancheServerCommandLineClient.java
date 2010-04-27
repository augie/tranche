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
package org.tranche.clc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.tranche.TrancheServer;
import org.tranche.streams.PrettyPrintStream;
import org.tranche.annotations.Todo;
import org.tranche.annotations.TodoList;
import org.tranche.commons.DebuggableThread;
import org.tranche.server.Server;

/**
 * <p>
 * This is a simple command line client that can be used with any TrancheServer implementation. The abstraction lets you
 * control either a remote or local implementation through the same interface.
 * </p>
 *
 * <p>
 * The DistributedFileSystemCommandLineClient class is headless. You can run
 * it from any command prompt. It is primarily tested on Windows and Linux.
 * </p>
 *
 * This class handles launching the appropriate client (e.g. remote connection, local file system, etc.).
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class TrancheServerCommandLineClient extends DebuggableThread {

    private InputStream in;
    private InputStreamReader isr;
    private BufferedReader br;
    private PrettyPrintStream out;
    protected Map<String, TrancheServerCommandLineClientItem> items = new HashMap<String, TrancheServerCommandLineClientItem>();
    private TrancheServer dfs;
    private boolean run = true;

    /**
     * Retrieve maximum characters per line.
     * @return  The maximum characters per line in the print stream.
     *
     */
    public int getMaxLineLength() {
        return out.getMaxCharactersPerLine();
    }

    /**
     * Set the maximum characters allowed per line.
     * param    length      The maximum line length for the print stream.
     *
     */
    public void setMaxLineLength(int length) {
        out.setMaxCharactersPerLine(length);
    }

    /**
     * 
     * @param item
     * @return
     */
    public TrancheServerCommandLineClientItem getItem(String item) {
        return items.get(item);
    }

    /**
     * <p>
     * The basic constructor for the command line client. It assumes that there
     * is an implementation of TrancheServer to control, an
     * input stream for commands, and an outputstream for results.
     * </p>
     * @param   dfs     The DistributedFileSystem implementation that will be manipulated by this code.
     * @param   in      the input source for text commands
     * @param   out     The output where text is sent.
     */
    public TrancheServerCommandLineClient(TrancheServer dfs, InputStream in, PrintStream out) {
        this.dfs = dfs;

        // set the thread's name
        this.setName("Command Line Client");

        this.in = in;
        this.isr = new InputStreamReader(in);
        this.br = new BufferedReader(isr);

        // redirect all output through the pretty-print
        this.out = new PrettyPrintStream(out);

        // add the items
        addItem(new HelpItem(dfs, this));
        addItem(new KillItem(dfs, this));
        addItem(new AddAdministratorItem(dfs, this));
        addItem(new ServerItem(dfs, this));
        addItem(new ConfigurationItem(dfs, this));
        addItem(new UserZipFileItem(dfs, this));
    }

    /**
     * Adds a new command line command to this client.
     * @param   dfsclci     the command line item to add
     */
    public void addItem(TrancheServerCommandLineClientItem dfsclci) {
        this.items.put(dfsclci.getName(), dfsclci);
    }

    /**
     * 
     * @param port
     * @param secure
     */
    public Server startServer(int port, boolean secure) {
        // start the server
        ServerItem clci = (ServerItem) items.get("server");
        // start the server
        clci.preDoAction("server command=start port=" + port + " secure=" + secure, new BufferedReader(new InputStreamReader(in)), out);

        return clci.getServer();
    }

    /**
     * <p>
     * This is a command line client that is designed to manipulate any
     * implementation of TrancheServer.
     * </p>
     *
     * <p>
     * This class is also extensible, allowing custom keywords and actions to
     * be plugged in by sub-classes.
     * </p>
     */
    @TodoList({
        @Todo(desc = "Create an \"ignore exception in this block\" utility -- avoid \"empty block\" warnings and demonstrate intent"),
        @Todo(desc = "Refactor this method in to smaller pieces")
    })
    @Override()
    public void run() {
        // calc the largest item name
        int largestCommandLength = 0;
        for (TrancheServerCommandLineClientItem dfsclci : items.values()) {
            if (dfsclci.getName().length() > largestCommandLength) {
                largestCommandLength = dfsclci.getName().length();
            }
        }

        StringWriter sw = new StringWriter();
        // execute commands forever
        out.println("Tranche server command line client started. \"?\" lists commands.");
        while (isRun()) {
            try {
                // check, if it is System.in
                if (in.equals(System.in)) {
                    // support MSDOS native command prompt input (damn native libraries)
                    if (System.getProperty("noAutoReboot") != null) {
                        sw.write(br.readLine());
                    } // for proper auto-reboot, stay in Java land
                    else {
                        // only get when available
                        while (isRun()) {
                            try {
                                if (in.available() > 0) {
                                    int c = in.read();
                                    if (c == '\n') {
                                        break;
                                    }
                                    sw.write((char) c);
                                }
                                // sleep some
                                try {
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                }
                            } // catch "illegal seek" exceptions
                            catch (Exception e) {

                                // -------------------------------------------------------------------------------
                                // WHEN ADMIN DISCONNECTS FROM REMOTE SERVER (E.G., SSH)...
                                //
                                //   Every read on the input stream will throw an "illegal seek" exception,
                                //   including available()! This results in huge amounts of exception messages.
                                //   (e.g., on Mar 8 2010, 5.2GB on Japan, which ran out of space, killing the
                                //   server.)
                                //
                                //   Don't print exception messages here!
                                // -------------------------------------------------------------------------------

                                try {
                                    Thread.sleep(500);
                                } catch (Exception ee) {
                                }
                            }
                        }
                        if (!isRun()) {
                            // kill the app
                            return;
                        }
                    }
                } // else treat remote stream's specially
                else {
                    // read in the option
                    for (int c = in.read(); c != '\n'; c = in.read()) {
                        if (c == -1) {
                            return;
                        }
                        sw.write((char) c);
                    }
                }

                // get the command
                String command = sw.toString().trim();

                sw = new StringWriter();

                // keep only the good parameters
                String[] commandParts = TrancheServerCommandLineClientItem.splitCommandLine(command);

                // check for help
                if (command.equalsIgnoreCase("?") || command.equals("")) {
                    // padding between command and description
                    int pad = 2;
                    // set the spacing to the appropriate length
                    out.setPadding(largestCommandLength + pad);
                    // set the headers
                    out.print("  Command");
                    for (int i = 0; i < largestCommandLength - "Command".length(); i++) {
                        out.print(" ");
                    }
                    // print a spacer line
                    out.println("Description");
                    for (int i = 0; i < out.getMaxCharactersPerLine(); i++) {
                        out.print("-");
                    }
                    out.println("");

                    // sort the commands alphabetically
                    TrancheServerCommandLineClientItem[] list = items.values().toArray(new TrancheServerCommandLineClientItem[0]);
                    Arrays.sort(list);

                    // list the items
                    for (TrancheServerCommandLineClientItem item : list) {
                        out.print(item.getName());

                        // add appropriate spaces
                        for (int i = 0; i < largestCommandLength - item.getName().length(); i++) {
                            out.print(" ");
                        }

                        // pad between commnand and description
                        for (int i = 0; i < pad; i++) {
                            out.print(" ");
                        }
                        out.println(item.getDescription());
                        out.println("");
                    }

                    // reset padding
                    out.setPadding(0);

                    // show help
                    continue;
                }

                // try to find the option
                TrancheServerCommandLineClientItem dfsclci = null;
                for (String itemName : items.keySet()) {
                    if (itemName.equals(commandParts[0])) {
                        dfsclci = items.get(itemName);
                        break;
                    }
                }

                // fall back on finding partial matches
                if (dfsclci == null) {
                    for (String itemName : items.keySet()) {
                        if (itemName.startsWith(commandParts[0])) {
                            dfsclci = items.get(itemName);
                            break;
                        }
                    }
                }
                // if found, use and continue
                if (dfsclci != null) {
                    // execute the action
                    dfsclci.preDoAction(command, new BufferedReader(new InputStreamReader(in)), out);
                    out.flush();
                    continue;
                }

                // fall back on invalid
                out.println(command + " is not a valid command.");
            } catch (Exception e) {
                debugErr(e);
            }
        }
    }

    /**
     * Set if this object is runnablble.
     * @param   run     the flag for running
     */
    public void setRun(boolean run) {
        this.run = run;
        // interrupt the thread
        if (!in.equals(System.in)) {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Return true if this object is running; false otherwise.
     * @return  <code>true</code> if the flag for running is enabled;
     *          <code>false</code> otherwise
     */
    public boolean isRun() {
        return run;
    }
}
