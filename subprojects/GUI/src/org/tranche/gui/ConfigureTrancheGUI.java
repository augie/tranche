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

import java.io.IOException;
import java.io.InputStream;
import org.tranche.ConfigureTranche;
import org.tranche.commons.ConfigurationUtil;
import org.tranche.commons.DebugUtil;

/**
 * <p>Loads a Tranche network's configuration information, including servers and certificates.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ConfigureTrancheGUI extends ConfigureTranche {

    /**
     * <p>The graphical user interface category.</p>
     */
    public static final String CATEGORY_GUI = "GUI";
    /**
     * <p>The starting number of downloads that can occur at any one time.</p>
     */
    public static final String PROP_DOWNLOAD_POOL_SIZE = "download.pool.size";
    /**
     * <p>The starting boolean of whether the hash popup suggestion window should be used.</p>
     */
    public static final String PROP_AUTO_COMPLETE_HASH = "hash.auto_complete";
    /**
     * <p>The hash of the list of possible modules.</p>
     */
    public static final String PROP_MODULE_LIST_HASH = "module.list.hash";
    /**
     * <p>A comma-separated list of hashes for modules that should be loaded on startup.</p>
     */
    public static final String PROP_STARTUP_MODULES = "module.list.start";
    /**
     * <p>The URL of the list of possible modules.</p>
     */
    public static final String PROP_MODULE_LIST_URL = "module.list.url";
    /**
     * <p>The starting number of uploads that can occur at any one time.</p>
     */
    public static final String PROP_UPLOAD_POOL_SIZE = "upload.pool.size";
    /**
     *
     */
    public static final String DEFAULT_GUI_CONFIG_FILE_LOCATION = "/org/tranche/gui/default.conf";

    static {
        // load the default values
        try {
            DebugUtil.debugOut(ConfigureTrancheGUI.class, "Loading default GUI configuration from " + DEFAULT_GUI_CONFIG_FILE_LOCATION);
            ConfigurationUtil.loadDefaults(DEFAULT_GUI_CONFIG_FILE_LOCATION);
        } catch (Exception e) {
            DebugUtil.debugErr(ConfigureTrancheGUI.class, e);
        }
    }

    /**
     * 
     */
    protected ConfigureTrancheGUI() {
    }

    /**
     * <p></p>
     * @param args
     * @throws IOException
     */
    public static void load(String[] args) throws IOException {
        // this method needs to be here to load the default config file
        ConfigureTranche.load(args);
    }

    /**
     * <p></p>
     * @param configFile
     * @throws IOException
     */
    public static void load(String configFile) throws IOException {
        // this method needs to be here to load the default config file
        ConfigureTranche.load(configFile);
    }

    /**
     * <p></p>
     * @param configFileStream
     */
    public static void load(InputStream configFileStream) {
        // this method needs to be here to load the default config file
        ConfigureTranche.load(configFileStream);
    }
}
