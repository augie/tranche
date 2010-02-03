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
package org.tranche;

import java.io.File;
import java.io.IOException;
import org.tranche.clc.TrancheServerCommandLineClient;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.server.Server;
import org.tranche.servers.ServerUtil;
import org.tranche.users.UserZipFile;
import org.tranche.util.PreferencesUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>There can be only one local data server.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class LocalDataServer {

    private static FlatFileTrancheServer ffts;
    private static Server server;
    private static File rootDir = new File(PreferencesUtil.get(ConfigureTranche.PROP_SERVER_DIRECTORY));
    private static int port = Integer.valueOf(PreferencesUtil.get(ConfigureTranche.PROP_SERVER_PORT));
    private static boolean ssl = Boolean.valueOf(PreferencesUtil.get(ConfigureTranche.PROP_SERVER_SSL));
    private static UserZipFile userZipFile = null;
    private static TrancheServerCommandLineClient client;

    private LocalDataServer() {
    }

    public static FlatFileTrancheServer getFlatFileTrancheServer() {
        return ffts;
    }

    public static Server getServer() {
        return server;
    }

    public static boolean isServerRunning() {
        if (server == null) {
            return false;
        }
        return server.isAlive();
    }

    public static File getRootDirectory() {
        return rootDir;
    }

    public static void setRootDirectory(File rootDir) {
        LocalDataServer.rootDir = rootDir;
    }

    public static int getPort() {
        return port;
    }

    public static void setPort(int port) {
        LocalDataServer.port = port;
    }

    public static boolean isSSL() {
        return ssl;
    }

    public static void setSSL(boolean ssl) {
        LocalDataServer.ssl = ssl;
    }

    public static UserZipFile getUserZipFile() {
        return userZipFile;
    }

    public static void setUserZipFile(UserZipFile userZipFile) {
        LocalDataServer.userZipFile = userZipFile;
    }

    private static void setUpFFTS() {
        ffts = new FlatFileTrancheServer(rootDir);
        if (userZipFile != null) {
            ffts.setAuthCert(userZipFile.getCertificate());
            ffts.setAuthPrivateKey(userZipFile.getPrivateKey());
        }
    }

    public static void start() throws Exception {
        if (isServerRunning()) {
            throw new Exception("Server is running.");
        }
        if (!rootDir.exists()) {
            throw new IOException("Root directory does not exist.");
        }
        if (!rootDir.canWrite()) {
            throw new IOException("Root directory is not writable.");
        }
        if (port < 1) {
            throw new Exception("Port number must be positive.");
        }

        setUpFFTS();
        server = new Server(ffts, port, ssl);
        server.start();

        // save the preferences
        PreferencesUtil.set(ConfigureTranche.PROP_SERVER_PORT, String.valueOf(port), false);
        PreferencesUtil.set(ConfigureTranche.PROP_SERVER_SSL, String.valueOf(ssl), false);
        PreferencesUtil.set(ConfigureTranche.PROP_SERVER_DIRECTORY, rootDir.getAbsolutePath(), false);
        PreferencesUtil.save();
    }

    public static void stop() throws Exception {
        server.setRun(false);
        ffts = null;
    }

    public static void printUsage() {
        System.out.println();
        System.out.println("USAGE");
        System.out.println("    [OPTIONS]");
        System.out.println();
        System.out.println("DESCRIPTION");
        System.out.println("    Runs a Tranche data sserver.");
        System.out.println();
        System.out.println("MEMORY ALLOCATION");
        System.out.println("    You should use the JVM option: -Xmx512m");
        System.out.println();
        System.out.println("    The allocates 512m of memory for the tool. You can adjust this amount (e.g., you don't have 512MB available memory or you want to allocate more.)");
        System.out.println();
        System.out.println("OUTPUT FLAGS");
        System.out.println("    -b, --buildnumber           Value: none.                    Print version number and exit. All other arguments will be ignored.");
        System.out.println("    -h, --help                  Value: none.                    Print usage and exit. All other arguments will be ignored.");
        System.out.println("    -u, --usage                 Value: none.                    Print usage and exit. All other arguments will be ignored.");
        System.out.println("    -v, --version               Value: none.                    Print version number and exit. All other arguments will be ignored.");
        System.out.println();
        System.out.println("STANDARD PARAMETERS");
        System.out.println("    -H, --host                  Value: any string.              The host name / IP address by which the server will be known.");
        System.out.println("    -d, --directory             Value: any string.              The file system location where all server configuration files will be located.");
        System.out.println("    -p, --port                  Value: positive integer.        The port number to which the server will be bound.");
        System.out.println("    -s, --ssl                   Value: true/false.              Whether the server should operate over SSL connections.");
        System.out.println("    -z, --userzipfile           Values: any two strings.        The file system location of the user zip file for the server and the passphrase to unlock it.");
        System.out.println();
        System.out.println("RETURN CODES");
        System.out.println("    To check the return code for a process in UNIX bash System.out, use $? special variable. If non-zero, check standard error for messages.");
        System.out.println();
        System.out.println("    0:     Program exited normally (e.g., download succeeded, help displayed, etc.)");
        System.out.println("    1:     Unknown error.");
        System.out.println();
    }

    public static void main(String[] args) {
        ConfigureTranche.load(args);

        // printing usage
        if (args.length < 1) {
            printUsage();
            System.exit(0);
        }
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-h") || args[i].equals("--help") || args[i].equals("-u") || args[i].equals("--usage")) {
                printUsage();
                System.exit(0);
            } else if (args[i].equals("-b") || args[i].equals("--buildnumber") || args[i].equals("-v") || args[i].equals("--version")) {
                System.out.println("Tranche, build #@buildNumber");
                System.exit(0);
            }
        }

        // read the arguments
        for (int i = 1; i < args.length; i += 2) {
            if (args[i].equals("-H") || args[i].equals("--host")) {
                ServerUtil.setHostName(args[i + 1]);
            } else if (args[i].equals("-d") || args[i].equals("--directory")) {
                rootDir = new File(args[i + 1]);
            } else if (args[i].equals("-p") || args[i].equals("--port")) {
                try {
                    port = Integer.valueOf(args[i + 1]);
                } catch (Exception e) {
                    System.err.println("Invalid port value: " + args[i + 1]);
                }
            } else if (args[i].equals("-s") || args[i].equals("--ssl")) {
                try {
                    ssl = Boolean.valueOf(args[i + 1]);
                } catch (Exception e) {
                    System.err.println("Invalid SSL value: " + args[i + 1]);
                }
            } else if (args[i].equals("-z") || args[i].equals("--userzipfile")) {
                try {
                    UserZipFile user = new UserZipFile(new File(args[i + 1]));
                    user.setPassphrase(args[i + 2]);
                    i++;
                    userZipFile = user;
                } catch (Exception e) {
                    System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }

        // set up the local server
        setUpFFTS();

        // interact using a client -- will also start a server around the FFTS
        client = new TrancheServerCommandLineClient(ffts, System.in, System.out);
        server = client.startServer(port, ssl);
        client.run();
    }
}
