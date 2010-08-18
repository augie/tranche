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
package org.tranche.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import org.tranche.ConfigureTranche;
import org.tranche.commons.DebugUtil;
import org.tranche.flatfile.FlatFileTrancheServer;

/**
 * Stores user preferences.
 * @author Bryan Smith <bryanesmith at gmail.com>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class PreferencesUtil {

    public static final File PREFERENCES_FILE = new File(FlatFileTrancheServer.getDefaultHomeDir() + File.separator + "preferences");
    public static final String PREF_DOWNLOAD_FILE = "download.file";
    public static final String PREF_TEMPORARY_FILE_DIRECTORY = "temp.dir";
    public static final String PREF_USER_FILE_LOCATION = "user.file";
    public static final String PREF_UPLOAD_LOCATION = "uploader.file";
    private static final Map<String, String> preferences = new HashMap<String, String>();
    private static boolean lazyLoadStarted = false;
    private static Thread lazyLoadThread = new Thread("Preferences Util Lazy Load") {

        @Override()
        public void run() {
            try {
                // in case the preferences file does not exist
                if (!PREFERENCES_FILE.exists()) {
                    save();
                    return;
                }

                BufferedReader in = null;
                FileReader fr = null;
                try {
                    fr = new FileReader(PREFERENCES_FILE);
                    in = new BufferedReader(fr);
                    // read all the preferences from the file
                    while (in.ready()) {
                        String key = in.readLine().trim();
                        String value = in.readLine().trim();
                        synchronized (preferences) {
                            preferences.put(key, value);
                        }
                    }
                } finally {
                    IOUtil.safeClose(fr);
                    IOUtil.safeClose(in);
                }
            } catch (Exception e) {
                DebugUtil.debugErr(PreferencesUtil.class, e);
            }
        }
    };

    /**
     * 
     */
    public static void waitForStartup() {
        lazyLoad();
        while (lazyLoadThread.isAlive() || !lazyLoadStarted) {
            try {
                lazyLoadThread.join();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 
     */
    public synchronized static void lazyLoad() {
        if (!lazyLoadStarted) {
            lazyLoadStarted = true;
            lazyLoadThread.setDaemon(true);
            lazyLoadThread.start();
        }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public synchronized static void save() {
        DebugUtil.debugOut(PreferencesUtil.class, "Saving.");
        try {
            // Delete file if exists
            PREFERENCES_FILE.delete();
            PREFERENCES_FILE.createNewFile();

            // write the file
            BufferedWriter out = null;
            FileWriter fw = null;
            try {
                fw = new FileWriter(PREFERENCES_FILE);
                out = new BufferedWriter(fw);
                // save the preferences
                for (String preferenceName : preferences.keySet()) {
                    out.write(preferenceName);
                    out.newLine();
                    out.write(preferences.get(preferenceName));
                    out.newLine();
                }
                out.flush();
                fw.flush();
            } finally {
                IOUtil.safeClose(fw);
                IOUtil.safeClose(out);
            }
        } catch (Exception e) {
            DebugUtil.debugErr(PreferencesUtil.class, e);
        }
    }

    /**
     * 
     * @param name
     * @param value
     */
    public static void set(String name, String value) {
        set(name, value, true);
    }

    /**
     * 
     * @param name
     * @param value
     * @param save
     */
    public static void set(String name, String value, boolean save) {
        waitForStartup();
        synchronized (preferences) {
            preferences.put(name, value);
        }
        if (save) {
            save();
        }
    }

    /**
     * 
     * @param name
     */
    public static void remove(String name) {
        waitForStartup();
        synchronized (preferences) {
            preferences.remove(name);
        }
        save();
    }

    /**
     *
     * @param name
     * @return String value of the given preference. <i>null</i> if preference does not exist.
     */
    public static String get(String category, String name) {
        waitForStartup();
        String value = null;
        synchronized (preferences) {
            value = preferences.get(name);
        }
        if (value == null || value.equals("")) {
            value = ConfigureTranche.get(category, name);
        }
        return value;
    }

    /**
     * 
     * @param name
     * @return String value of the given preference. <i>null</i> if preference does not exist.
     */
    public static String get(String name) {
        return get(ConfigureTranche.CATEGORY_GENERAL, name);

    }

    /**
     * 
     * @param name
     * @return
     */
    public static int getInt(String name) {
        try {
            return Integer.valueOf(get(name));
        } catch (Exception e) {
            DebugUtil.debugErr(PreferencesUtil.class, e);
        }
        return 0;
    }

    public static boolean getBoolean(String category, String name) {
        try {
            return Boolean.valueOf(get(category, name));
        } catch (Exception e) {
            DebugUtil.debugErr(PreferencesUtil.class, e);
        }
        return false;
    }

    /**
     *
     * @param name
     * @return
     */
    public static boolean getBoolean(String name) {
        try {
            return Boolean.valueOf(get(name));
        } catch (Exception e) {
            DebugUtil.debugErr(PreferencesUtil.class, e);
        }
        return false;
    }

    /**
     * 
     * @param name
     * @return
     */
    public static File getFile(String name) {
        String value = get(name);
        if (value == null) {
            return new File("");
        }
        return new File(value);
    }

    /**
     * 
     * @return
     */
    public static int getSize() {
        waitForStartup();
        synchronized (preferences) {
            return preferences.size();
        }
    }

    /**
     * 
     */
    public static void clear() {
        synchronized (preferences) {
            preferences.clear();
        }
        save();
    }
}
