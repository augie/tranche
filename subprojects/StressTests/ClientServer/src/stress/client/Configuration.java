/*
 * Configuration.java
 *
 * Created on November 28, 2007, 8:13 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package stress.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import org.tranche.util.IOUtil;
import stress.StressTestConfig;

/**
 * <p>Holds all configuration parameters.</p>
 * <p>If you want to run the tests on a computer, you must first create an Environment variable, then set it.</p>
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 * @author Adam Giacobbe <agiac@umich.edu>
 */
public class Configuration {

    // Feel free to change above!
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Please don't change below unless you have a good reason!
    private static boolean isLazyLoaded = false;
    /**
     *     
     */
    private static final int tranchePort = 1500;
    /**
     *
     */
    private static final int stressPort = 1600;
    /**
     * Don't set to true unless you want to hammer server with multiple
     * computers. Please set back to false. Uncommon.
     */
    private static final boolean isMultiMode = false;
    /**
     * tempDirPath Path to where you want temp files to go. Usually a drive with plenty of room.
     */
    private static File tempDirectory = null;
    /**
     * Path to destination file for all logged information.
     */
    private static File resultsFile = null;
    /**
     * Path for redirected standard out and err
     */
    private static File outAndErrRedirectFile = null;
    /**
     * serverIP The server's IP address (not Trance URL! Just IP)
     */
    private static String serverIP = null;
    /**
     * The file for  stress_test.conf
     */
    private static File stressTestConfigFile = null;
    /**
     * clientJARPath The path to TrancheSocketStressTestClient.jar
     */
    private static String clientJARPath = null;
    
    /**
     * The path file for TrancheStressTest
     */
    private static File stressTestBinaryDirectory = null;

    /**
     * <p>Lazy load from ServerClient</p>
     * @throws java.lang.Exception
     */
    private static void lazyLoad() throws Exception {
        if (isLazyLoaded) {
            return;
        }
        _lazyLoad(StressTestConfig.class.getResource("/").getPath());
    }
    
    /**
     * <p>Offers a thread from separate JVM a way to read configuration.</p>
     * @param sourceDirectory
     * @throws java.lang.Exception
     */
    public static void loadConfigurationForClientInSeparateJVM(String path) throws Exception {
        if (isLazyLoaded) {
            return;
        }
        _lazyLoad(path);
    }
    
    private static void _lazyLoad(String sourceDirectory) throws Exception {
        if (isLazyLoaded) {
            return;
        }
        isLazyLoaded = true;
        
        // Set the source directory!
        stressTestBinaryDirectory = new File(sourceDirectory);
        System.out.println("1. Thinks the binary directory is at: "+stressTestBinaryDirectory.getAbsolutePath());
        
        BufferedReader in = null;
        
        try {
//            URL fileUrl = StressTestConfig.class.getResource("/files/client.conf");
//            System.out.println("2. Parsing client configuration at: "+fileUrl);
//            in = new BufferedReader(new FileReader(fileUrl.getPath()));
            
            File clientConfigurationFile = new File(getStressTestBinaryDir(), "files/client.conf");
            System.out.println("2. Parsing client configuration at: "+clientConfigurationFile.getAbsolutePath());
            in = new BufferedReader(new FileReader(clientConfigurationFile));
            
            String nextLine = null;
            int line = 0;
            String tempNextLine = null;
            String setEnviornment = null;
            while ((nextLine = in.readLine()) != null) {
                line++;
                nextLine = nextLine.trim();
                // Throw away comments
                if (nextLine.startsWith("#")) {
                    continue;
                }
                int pos = nextLine.indexOf("#");
                if (nextLine.startsWith("/*")) {
                    while (!nextLine.endsWith("*/")) {
                        nextLine = in.readLine();
                    }
                    continue;
                }
                nextLine = nextLine.trim();
                if (pos != -1) {
                    nextLine = nextLine.substring(0, pos);
                }
                
                // Ignore blank lines
                if (nextLine.equals("")) {
                    continue;
                }
                else if (nextLine.startsWith("SetEnvironment")) {
                    setEnviornment = nextLine.substring(nextLine.indexOf("=") + 1);
                    setEnviornment = setEnviornment.trim();
                } else if (nextLine.startsWith("Environment") && nextLine.contains(setEnviornment) && nextLine.endsWith("{")) {
                    READ_IN_ENVIRONMENT:
                    while (true) {
                        nextLine = in.readLine();
                        
                        // Throw away comments
                        if (nextLine.startsWith("#")) {
                            continue;
                        }
                        pos = nextLine.indexOf("#");
                        if (nextLine.startsWith("/*")) {
                            while (!nextLine.endsWith("*/")) {
                                nextLine = in.readLine();
                            }
                            continue;
                        }
                        nextLine = nextLine.trim();
                        if (pos != -1) {
                            nextLine = nextLine.substring(0, pos);
                        }
                        if (nextLine.equals("")) {
                            continue;
                        } else if (serverIP == null && nextLine.startsWith("serverIP")) {
                            serverIP = nextLine.substring(nextLine.indexOf("=") + 1);
                            serverIP = serverIP.trim();
                        } else if (tempDirectory == null && nextLine.startsWith("tempDirectory")) {
                            tempNextLine = nextLine.substring(nextLine.indexOf("=") + 1);
                            tempNextLine = tempNextLine.trim();
                            tempDirectory = new File(tempNextLine);
                        } else if (resultsFile == null && nextLine.startsWith("resultsFile")) {
                            tempNextLine = nextLine.substring(nextLine.indexOf("=") + 1);
                            tempNextLine = tempNextLine.trim();
                            resultsFile = new File(tempNextLine);
                        } else if (clientJARPath == null && nextLine.startsWith("clientJARPath")) {
                            clientJARPath = nextLine.substring(nextLine.indexOf("=") + 1);
                            clientJARPath = clientJARPath.trim();
                        } else if (outAndErrRedirectFile == null && nextLine.startsWith("outAndErrRedirectFile")) {
                            outAndErrRedirectFile = new File(nextLine.substring(nextLine.indexOf("=") + 1).trim());
                        } else if (nextLine.endsWith("}")) {
                            break READ_IN_ENVIRONMENT;
                        }
                    }
                } else if (nextLine.startsWith("Environment") && nextLine.endsWith("{")) {
                    while (!nextLine.endsWith("}")) {
                        nextLine = in.readLine();
                    }
                } else {
                    throw new RuntimeException("What does \"" + nextLine + "\" mean?, error in line " + line);
                }
            }

        } finally {
            IOUtil.safeClose(in);
        }

        URL clientConfigUrl = StressTestConfig.class.getResource("/files/stress_test.conf");
        stressTestConfigFile = new File(clientConfigUrl.getPath());
    }

    public static int getTranchePort() throws Exception {
        lazyLoad();
        return tranchePort;
    }

    public static int getStressPort() throws Exception {
        lazyLoad();
        return stressPort;
    }

    public static boolean isMultiMode() throws Exception {
        lazyLoad();
        return isMultiMode;
    }

    public static File getTempDirectory() throws Exception {
        lazyLoad();
        return tempDirectory;
    }

    public static File getResultsFile() throws Exception {
        lazyLoad();
        return resultsFile;
    }
    
    public static File getOutAndErrRedirectFile() throws Exception {
        lazyLoad();
        return outAndErrRedirectFile;
    }

    public static String getServerIP() throws Exception {
        lazyLoad();
        return serverIP;
    }

    /**
     * 
     * @return Directory where classes and other files (e.g., config, user certs, etc.) are located.</p>
     * @throws java.lang.Exception
     */
    public static File getStressTestBinaryDir() throws Exception {
        lazyLoad();
        return stressTestBinaryDirectory;
    }

    public static File getStressTestConfigFile() throws Exception {
        lazyLoad();
        return stressTestConfigFile;
    }

    public static String getClientJARPath() throws Exception {
        lazyLoad();
        return clientJARPath;
    }
    
} // Configuration