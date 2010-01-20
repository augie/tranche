/*
 * StressClient.java
 *
 * Created on October 6, 2007, 8:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package stress.client;

import java.io.File;
import org.proteomecommons.tranche.ProteomeCommonsTrancheConfig;
import org.tranche.logs.SimpleLog;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.servers.ServerUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;
import stress.server.RemoteStressServer;
import stress.util.Logger;
import stress.util.StressIOUtil;
import stress.util.StressTest;
import stress.util.StressTestSuite;

/**
 * Entry point for client-side of stress test.
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 * @author Adam Giacobbe <agiac@umich.edu>
 */
public class StressClient {

    // Yuck, a hack to determine which JVM is running =(
    public static boolean isStressClientRunning = false;
    private static String argCombination = null;

    /**
     * Entry point for the client.
     */
    public static void main(String[] args) throws Exception {

        isStressClientRunning = true;

        ProteomeCommonsTrancheConfig.load();

        if (args.length == 1) {
            stress.client.Configuration.stressTestSourceDirectory = new File(args[0]);
        } else if (args.length > 1) {

            //Takes into consideration spaces in the link
            for (String arg : args) {
                if (argCombination != null) {
                    argCombination = argCombination + " " + arg;
                } else if (arg != null) {
                    argCombination = arg;
                }

            }
            stress.client.Configuration.stressTestSourceDirectory = new File(argCombination);

        } else {
            System.out.println("You need to provide the path to the directory that the src folder is in!");
            System.out.println("Now Exiting!");
            return;
        }

        RemoteStressServer stressServer = null;
        try {

            if (Configuration.getOutputFile().exists()) {
                System.err.println("The output file already exists: " + Configuration.getOutputFile().getAbsolutePath());
                System.err.println("Please move the file, then restart...");
                System.exit(1);
            }

            String url = "tranche://" + Configuration.getServerIP() + ":" + Configuration.getTranchePort();

            // Prime the config reader, make sure everything okay
            StressTestSuite.hasNext();

            // Connect to the server
            stressServer = new RemoteStressServer(Configuration.getServerIP());

            // Flag as multi-socket test so socket not closed
            RemoteTrancheServer.setMultiSocketTest(true);

            // We're testing, so allow things like server tracker to die
            TestUtil.setTesting(true);

            // Wait now so doesn't impact test performance
            Thread waitThread = new Thread("Limited wait thread") {

                @Override
                public void run() {
                    ServerUtil.waitForStartup();
                }
            };
            waitThread.setDaemon(true);
            waitThread.setPriority(Thread.MIN_PRIORITY);
            waitThread.start();

            // Ten seconds, then move on. Script blocked before for a week!
            waitThread.join(1000 * 10);


            while (StressTestSuite.hasNext()) {

                StressTest test = StressTestSuite.getNext();

                // Start test
                stressServer.startTest();

                boolean isOnline = ServerUtil.isServerOnline(url);
                System.out.println("Server \"" + url + "\": " + isOnline);

                Logger.startTest(test);

                // May flag offline, flag online
                ServerUtil.setServerOnlineStatus(url, true);

                System.out.println("Starting test with " + test.getClientCount() + " clients at " + Text.getFormattedDate(System.currentTimeMillis()) + ".");

                System.out.println(test.toString());

//                ThreadGroup clients = new ThreadGroup("Stress clients thread group");

                // Sleep a bit, make sure server is ready...
                System.out.println("  Sleep a bit to make sure server is ready...");
                Thread.sleep(1000 * 30);

                Thread[] clientThreads = new Thread[test.getClientCount()];

                for (int i = 0; i < test.getClientCount(); i++) {
                    clientThreads[i] = new ClientConnectionThread(
                            Configuration.getServerIP(),
                            test);
                    clientThreads[i].start();
                }

                // Join threads and log the stats
                for (Thread t : clientThreads) {
                    if (t != null && t.isAlive()) {
                        t.join();
                    }
                }

                Logger.stopTest();

                // Stop test (if not multi-client mode)
                if (!Configuration.isMultiMode()) {
                    stressServer.stopTest();
                }

                // Flush contents of log
                SimpleLog.setEnabled(false);

                // Clear cache, which will tear down Tranche sockets
                StressIOUtil.clearCache();

                // Give server chance to catch up, let client-side threads die...
                int sleep = 60 * 1000;
                System.out.println("Test completed, sleeping for " + Text.getPrettyEllapsedTimeString(sleep) + ". Started sleep at " + Text.getFormattedDate(System.currentTimeMillis()));
                Thread.sleep(sleep);
                IOUtil.recursiveDeleteWithWarning(Configuration.getTempDirectory());
            }

        } catch (Exception e) {
            System.out.println("An error has occured, client stopping: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            try {
                Logger.close();
            } catch (Exception ex) { /* Nope */ ex.printStackTrace(System.err);
            }

            // Only close if single-user mode
            if (!Configuration.isMultiMode() && stressServer != null) {
                try {
                    stressServer.close();
                } catch (Exception ex) {
                    System.out.println("Cannot tear down test server: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            isStressClientRunning = false;
            System.out.println("~ Fin. So long! ~");
            System.exit(0);
        }
    }
}
