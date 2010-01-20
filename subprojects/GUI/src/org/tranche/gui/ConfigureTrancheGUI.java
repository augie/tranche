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
package org.tranche.gui;

import java.io.BufferedInputStream;
import java.io.InputStream;
import org.tranche.ConfigureTranche;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;

/**
 * <p>This is here to load a Tranche network's configuration information, including servers and certificates.</p>
 * <p>Additionally loads the graphical user inerface configuration.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ConfigureTrancheGUI extends ConfigureTranche {

    private static boolean debug = false;
    /**
     * <p></p>
     */
    public static final String CATEGORY_GUI = "[GUI]";
    /**
     * <p></p>
     */
    public static final String PROP_MODULE_LIST_URL = "module.list.url";
    /**
     * <p></p>
     */
    public static final String PROP_MODULE_LIST_HASH = "module.list.hash";
    /**
     * <p></p>
     */
    public static final String PROP_STARTUP_MODULES = "module.list.start";
    /**
     * <p></p>
     */
    public static final String PROP_UPLOAD_POOL_SIZE = "upload.pool.size";
    /**
     * <p></p>
     */
    public static final String DEFAULT_UPLOAD_POOL_SIZE = "1";
    /**
     * <p></p>
     */
    public static final String PROP_DOWNLOAD_POOL_SIZE = "download.pool.size";
    /**
     * <p></p>
     */
    public static final String DEFAULT_DOWNLOAD_POOL_SIZE = "1";
    /**
     * <p></p>
     */
    public static final String PROP_AUTO_COMPLETE_HASH = "hash.auto_complete";
    /**
     * <p></p>
     */
    public static final String DEFAULT_AUTO_COMPLETE_HASH = "true";
    /**
     * <p></p>
     */
    public static final String PROP_SWT = "swt";
    /**
     * <p></p>
     */
    public static final String DEFAULT_SWT = "false";

    static {
        set(PROP_UPLOAD_POOL_SIZE, DEFAULT_UPLOAD_POOL_SIZE);
        set(PROP_DOWNLOAD_POOL_SIZE, DEFAULT_DOWNLOAD_POOL_SIZE);
        set(PROP_AUTO_COMPLETE_HASH, DEFAULT_AUTO_COMPLETE_HASH);
        set(PROP_SWT, DEFAULT_SWT);
    }

    /**
     * <p>Class cannot be instantiated.</p>
     */
    private ConfigureTrancheGUI() {
    }

    /**
     * <p>Safely loads the network configuration from the passed in arguments.</p>
     * <p>Takes only the first argument from the array and passes it to the ConfigureTrancheGUI.load(String)
     * method to load the network configuration and the graphical user interface configuation.</p>
     * @param args
     */
    public static void load(String[] args) {
        try {
            load(args[0]);
        } catch (Exception e) {
            // likely no arguments given
        }
    }

    /**
     * <p>Takes the location to the configuration file that contains this Tranche network's configuration information and the graphical user interface information.</p>
     * <p>The file can be in the JAR package, on the local file system, or on the Internet. The program will try to find the file in that same order and will use the first one it finds.</p>
     * @param configFile
     */
    public static void load(String configFile) {
        // load the network configuration
        ConfigureTranche.load(configFile);

        // load the gui configuration
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = openStreamToFile(configFile);
            String readingCategory = null;
            String line = null;
            while ((line = readLineIgnoreComments(is)) != null) {
                try {
                    if (line.startsWith("[")) {
                        if (line.equals(CATEGORY_GUI)) {
                            readingCategory = CATEGORY_GUI;
                        } else {
                            readingCategory = null;
                        }
                    } else if (readingCategory == null) {
                        continue;
                    } else if (readingCategory.equals(CATEGORY_GUI)) {
                        String name = line.substring(0, line.indexOf('=')).trim().toLowerCase();
                        String value = line.substring(line.indexOf('=') + 1).trim();
                        set(name, value);
                    }
                } catch (Exception e) {
                    debugErr(e);
                }
            }
        } catch (Exception e) {
            debugErr(e);
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(is);
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static void setDebug(boolean debug) {
        ConfigureTrancheGUI.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(ConfigureTrancheGUI.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param line
     */
    private static void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
