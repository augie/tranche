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
import org.tranche.util.IOUtil;

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
     * outputFilePath Path to destination file for all logged information.
     */
    private static File outputFile = null;
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
    public static File stressTestSourceDirectory = null;

    private static void lazyLoad() throws Exception {
        if (isLazyLoaded) {
            return;
        }
        isLazyLoaded = true;
        
        
        
        loadClientConfig();

        stressTestConfigFile = new File(stressTestSourceDirectory, "files/stress_test.conf");

    }

    public static void loadClientConfig() throws Exception {
        
        File clientConf = new File(stressTestSourceDirectory, "files/client.conf");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(clientConf));
            int variableLoadCount = 0;
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
                
              
                if (variableLoadCount == 4) {
                    break;
                } else if (nextLine.startsWith("SetEnviornment")) {
                    setEnviornment = nextLine.substring(nextLine.indexOf("=") + 1);
                    setEnviornment = setEnviornment.trim();
                 
                } else if (nextLine.trim().startsWith("Environment "+ setEnviornment) && nextLine.trim().endsWith("{")) {
                    in.mark(100);
                    
                    while ((nextLine = in.readLine()) != null) {
                        nextLine = nextLine.trim();
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
                          
                            variableLoadCount++;
                        } else if (tempDirectory == null && nextLine.startsWith("tempDirectory")) {
                            tempNextLine = nextLine.substring(nextLine.indexOf("=") + 1);
                            tempNextLine = tempNextLine.trim();
                         
                            tempDirectory = new File(tempNextLine);
                            variableLoadCount++;
                        } else if (outputFile == null && nextLine.startsWith("outputFile")) {
                            tempNextLine = nextLine.substring(nextLine.indexOf("=") + 1);
                            tempNextLine = tempNextLine.trim();
                        
                            outputFile = new File(tempNextLine);
                            variableLoadCount++;
                        } else if (clientJARPath == null && nextLine.startsWith("clientJARPath")) {
                            clientJARPath = nextLine.substring(nextLine.indexOf("=") + 1);
                            clientJARPath = clientJARPath.trim();
                        
                            
                            variableLoadCount++;
                        } else if (nextLine.endsWith("}") && (variableLoadCount != 4)) {
                     
                            in.reset();
                        } else if (variableLoadCount == 4) {
                            break; 
                        } else {
                            throw new RuntimeException("Missing a value in the client.conf file!! ");
                        }
                    }
                } else if (nextLine.startsWith("Environment") && nextLine.endsWith("{")) {
                 
                    while (!nextLine.trim().endsWith("}")) {
                        
                        nextLine = in.readLine();
                         
                    }
                } else {
                    throw new RuntimeException("What does \"" + nextLine + "\" mean?, error in line " + line);
                }
            }

        } finally {
            IOUtil.safeClose(in);
        }
        
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

    public static File getOutputFile() throws Exception {
        lazyLoad();
        return outputFile;
    }

    public static String getServerIP() throws Exception {
        lazyLoad();
        return serverIP;
    }

    public static File getSourceCodeDir() throws Exception {
        return stressTestSourceDirectory;
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