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
import org.tranche.commons.DebugUtil;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.server.Server;
import org.tranche.servers.ServerUtil;
import org.tranche.users.UserZipFile;
import org.tranche.util.TestUtil;

/**
 * <p>There can be only one local data server.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - besmit@umich.edu
 */
public class LocalDataServer {

    private static FlatFileTrancheServer ffts;
    private static Server server;
    private static File rootDir;
    private static int port;
    private static boolean ssl;
    private static UserZipFile userZipFile;
    private static TrancheServerCommandLineClient client;

    /**
     *
     */
    private LocalDataServer() {
    }

    /**
     *
     * @return
     */
    public static FlatFileTrancheServer getFlatFileTrancheServer() {
        return ffts;
    }

    /**
     *
     * @return
     */
    public static Server getServer() {
        return server;
    }

    /**
     *
     * @return
     */
    public static boolean isServerRunning() {
        if (server == null) {
            return false;
        }
        return server.isAlive();
    }

    /**
     *
     * @return
     */
    public static File getRootDirectory() {
        return rootDir;
    }

    /**
     *
     * @param rootDir
     */
    public static void setRootDirectory(File rootDir) {
        LocalDataServer.rootDir = rootDir;
    }

    /**
     *
     * @return
     */
    public static int getPort() {
        return port;
    }

    /**
     *
     * @param port
     */
    public static void setPort(int port) {
        LocalDataServer.port = port;
    }

    /**
     *
     * @return
     */
    public static boolean isSSL() {
        return ssl;
    }

    /**
     *
     * @param ssl
     */
    public static void setSSL(boolean ssl) {
        LocalDataServer.ssl = ssl;
    }

    /**
     *
     * @return
     */
    public static UserZipFile getUserZipFile() {
        return userZipFile;
    }

    /**
     *
     * @param userZipFile
     */
    public static void setUserZipFile(UserZipFile userZipFile) {
        LocalDataServer.userZipFile = userZipFile;
    }

    /**
     *
     * @throws Exception
     */
    private static void setUpFFTS() throws Exception {
        if (!rootDir.exists()) {
            throw new IOException("Root directory does not exist: " + rootDir.getAbsolutePath());
        }
        if (!rootDir.canWrite()) {
            throw new IOException("Root directory is not writable: " + rootDir.getAbsolutePath());
        }
        ffts = new FlatFileTrancheServer(rootDir);
        if (userZipFile != null) {
            ffts.setAuthCert(userZipFile.getCertificate());
            ffts.setAuthPrivateKey(userZipFile.getPrivateKey());
        }
    }

    /**
     *
     * @throws Exception
     */
    public static void start() throws Exception {
        if (isServerRunning()) {
            throw new Exception("Server is running.");
        }
        if (port < 1) {
            throw new Exception("Port number must be positive.");
        }

        setUpFFTS();
        server = new Server(ffts, port, ssl);
        server.start();
    }

    /**
     *
     * @throws Exception
     */
    public static void stop() throws Exception {
        server.setRun(false);
        ffts = null;
    }

    /**
     *
     */
    public static void printUsage() {
        System.out.println();
        System.out.println("USAGE");
        System.out.println("    [FLAGS / PARAMETERS]");
        System.out.println();
        System.out.println("DESCRIPTION");
        System.out.println("    Runs a Tranche data server.");
        System.out.println();
        System.out.println("MEMORY ALLOCATION");
        System.out.println("    To allocate 512 MB of memory to the process, you should use the JVM option: java -Xmx512m");
        System.out.println();
        System.out.println("PRINT AND EXIT FLAGS");
        System.out.println("    Use one of these to print some information and exit. Usage: java -jar <JAR> [PRINT AND EXIT FLAG]");
        System.out.println();
        System.out.println("    -h, --help              Print usage and exit.");
        System.out.println("    -V, --version           Print version number and exit.");
        System.out.println();
        System.out.println("OUTPUT FLAGS");
        System.out.println("    -d, --debug             If you have problems, you can use this option to print debugging information. These will help use solve problems if you can repeat your problem with this flag on.");
        System.out.println();
        System.out.println("PARAMETERS");
        System.out.println("    -D, --directory         Value: string.                  The directory that contains the server configurations and runtime files.");
        System.out.println("    -H, --host              Value: string.                  The host name / IP address by which the server will be known.");
        System.out.println("    -p, --port              Value: positive integer.        The port number to which the server will be bound.");
        System.out.println("    -s, --ssl               Value: true/false.              Whether the server should operate over SSL connections.");
        System.out.println("    -z, --userzipfile       Values: two strings.            The file system location of the user zip file for the server and the passphrase to unlock it.");
        System.out.println("    -r, --datablockrefs     Value: true/false.              Whether to store datablock refs.");
        System.out.println();
        System.out.println("RETURN CODES");
        System.out.println("    0: Exited normally");
        System.out.println("    1: Unknown error");
        System.out.println("    2: Problem with argument(s)");
        System.out.println("    3: Known error");
        System.out.println();
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printUsage();
                if (!TestUtil.isTesting()) {
                    System.exit(2);
                } else {
                    return;
                }
            }

            boolean isStoreDataBlockReferences = DataBlockUtil.DEFAULT_STORE_DATA_BLOCK_REFERENCES;

            // flags first
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-d") || args[i].equals("--debug")) {
                    DebugUtil.setDebug(LocalDataServer.class, true);
                    DebugUtil.setDebug(FlatFileTrancheServer.class, true);
                    DebugUtil.setDebug(Server.class, true);
                    DebugUtil.setDebug(TrancheServerCommandLineClient.class, true);
                } else if (args[i].equals("-h") || args[i].equals("--help")) {
                    printUsage();
                    if (!TestUtil.isTesting()) {
                        System.exit(0);
                    } else {
                        return;
                    }
                } else if (args[i].equals("-n") || args[i].equals("--buildnumber") || args[i].equals("-V") || args[i].equals("--version")) {
                    System.out.println("Tranche, build #@buildNumber");
                    if (!TestUtil.isTesting()) {
                        System.exit(0);
                    } else {
                        return;
                    }
                } else if (args[i].equals("-D") || args[i].equals("--directory")) {
                    i++;
                } else if (args[i].equals("-H") || args[i].equals("--host")) {
                    i++;
                } else if (args[i].equals("-p") || args[i].equals("--port")) {
                    i++;
                } else if (args[i].equals("-s") || args[i].equals("--ssl")) {
                    i++;
                } else if (args[i].equals("-z") || args[i].equals("--userzipfile")) {
                    i += 2;
                }
            }

            // configure
            try {
                ConfigureTranche.load(args);
            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                DebugUtil.debugErr(LocalDataServer.class, e);
                if (!TestUtil.isTesting()) {
                    System.exit(2);
                } else {
                    return;
                }
            }

            // set defaults
            setRootDirectory(new File(ConfigureTranche.get(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_DIRECTORY)));
            setPort(ConfigureTranche.getInt(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_PORT));
            setSSL(ConfigureTranche.getBoolean(ConfigureTranche.CATEGORY_SERVER, ConfigureTranche.PROP_SERVER_SSL));

            // parameters next
            for (int i = 1; i < args.length - 1; i++) {
                if (args[i].equals("-D") || args[i].equals("--directory")) {
                    try {
                        setRootDirectory(new File(args[i + 1]));
                    } catch (Exception e) {
                        System.err.println("ERROR: Invalid root directory value: " + args[i + 1]);
                        DebugUtil.debugErr(LocalDataServer.class, e);
                        if (!TestUtil.isTesting()) {
                            System.exit(2);
                        } else {
                            return;
                        }
                    } finally {
                        i++;
                    }
                } else if (args[i].equals("-H") || args[i].equals("--host")) {
                    try {
                        ServerUtil.setHostName(args[i + 1]);
                    } catch (Exception e) {
                        System.err.println("ERROR: Invalid host name: " + args[i + 1]);
                        DebugUtil.debugErr(LocalDataServer.class, e);
                        if (!TestUtil.isTesting()) {
                            System.exit(2);
                        } else {
                            return;
                        }
                    } finally {
                        i++;
                    }
                } else if (args[i].equals("-p") || args[i].equals("--port")) {
                    try {
                        setPort(Integer.valueOf(args[i + 1]));
                    } catch (Exception e) {
                        System.err.println("ERROR: Invalid port value: " + args[i + 1]);
                        DebugUtil.debugErr(LocalDataServer.class, e);
                        if (!TestUtil.isTesting()) {
                            System.exit(2);
                        } else {
                            return;
                        }
                    } finally {
                        i++;
                    }
                } else if (args[i].equals("-s") || args[i].equals("--ssl")) {
                    try {
                        setSSL(Boolean.valueOf(args[i + 1]));
                    } catch (Exception e) {
                        System.err.println("ERROR: Invalid SSL value: " + args[i + 1]);
                        DebugUtil.debugErr(LocalDataServer.class, e);
                        if (!TestUtil.isTesting()) {
                            System.exit(2);
                        } else {
                            return;
                        }
                    } finally {
                        i++;
                    }
                } else if (args[i].equals("-r") || args[i].equals("--datablockrefs")) {
                    try {
                        isStoreDataBlockReferences = Boolean.valueOf(args[i + 1]);
                    } catch (Exception e) {
                        System.err.println("ERROR: Invalid boolean value (isStoreDataBlockReferences): " + args[i + 1]);
                        DebugUtil.debugErr(LocalDataServer.class, e);
                        if (!TestUtil.isTesting()) {
                            System.exit(2);
                        } else {
                            return;
                        }
                    } finally {
                        i++;
                    }
                } else if (args[i].equals("-z") || args[i].equals("--userzipfile")) {
                    try {
                        UserZipFile user = new UserZipFile(new File(args[i + 1]));
                        user.setPassphrase(args[i + 2]);
                        setUserZipFile(user);
                    } catch (Exception e) {
                        System.err.println("ERROR: " + e.getMessage());
                        DebugUtil.debugErr(LocalDataServer.class, e);
                        if (!TestUtil.isTesting()) {
                            System.exit(2);
                        } else {
                            return;
                        }
                    } finally {
                        i += 2;
                    }
                }
            }

            // set up the local server
            try {
                setUpFFTS();
                setStoreDataBlockReferences(isStoreDataBlockReferences);

                // interact using a client -- will also start a server around the FFTS
                client = new TrancheServerCommandLineClient(ffts, System.in, System.out);
                server = client.startServer(port, ssl);
                client.run();
            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                DebugUtil.debugErr(LocalDataServer.class, e);
                if (!TestUtil.isTesting()) {
                    System.exit(3);
                } else {
                    return;
                }
            }

            if (!TestUtil.isTesting()) {
                System.exit(0);
            } else {
                return;
            }
        } catch (Exception e) {
            DebugUtil.debugErr(LocalDataServer.class, e);
            if (!TestUtil.isTesting()) {
                System.exit(1);
            } else {
                return;
            }
        }
    }

    /**
     *
     * @param isUsingDataBlockReferences
     */
    private static void setStoreDataBlockReferences(boolean isUsingDataBlockReferences) {
        Configuration config = getFlatFileTrancheServer().getConfiguration();
        config.setValue(ConfigKeys.DATABLOCK_STORE_DATABLOCK_REFERENCES, String.valueOf(isUsingDataBlockReferences));
        getFlatFileTrancheServer().saveConfiguration();
        System.out.println("Using datablock references?........... " + getFlatFileTrancheServer().getConfiguration().getValue(ConfigKeys.DATABLOCK_STORE_DATABLOCK_REFERENCES));
    }
}
