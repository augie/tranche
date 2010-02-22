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
package org.tranche.gui.get;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.tranche.gui.LazyLoadAllSlowStuffAfterGUIRenders;
import org.tranche.gui.LazyLoadable;
import org.tranche.gui.project.ProjectPool;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.PersistentServerFileUtil;
import org.tranche.util.ThreadUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class DownloadCache implements LazyLoadable {

    private static boolean debug = false;
    private static File file;
    private static final Object fileLock = new Object();
    private static boolean saving = false;
    private static Thread lazyLoadThread = new Thread("Lazy Load Download Cache") {

        @Override
        public void run() {
            ProjectPool.waitForStartup();
            try {
                file = PersistentServerFileUtil.getPersistentFile("downloads");
                
                // -----------------------------------------------------------------
                // Fix: used to have a downloads directory in same location,
                //      so need to check. If so, create a new file.
                //
                //      The alternative would be to attempt to move the 
                //      directory, which is generally a bad idea (user might
                //      have symbolic links; permissions issues; very large).
                // -----------------------------------------------------------------
                if (file.exists() && file.isDirectory()) {
                    file = PersistentServerFileUtil.getPersistentFile("downloads-cache");
                }
                
                debugOut("Loading download cache: " + file.getAbsolutePath());
            } catch (Exception e) {
                debugErr(e);
            }
            // read
            synchronized (fileLock) {
                FileInputStream fis = null;
                BufferedInputStream in = null;
                try {
                    fis = new FileInputStream(file);
                    in = new BufferedInputStream(fis);
                    while (in.available() > 0) {
                        try {
                            debugOut("Loading dowload summary.");
                            DownloadSummary ds = new DownloadSummary(in);
                            debugOut("Loaded download summary.");
                            if (ds.getStatus().equals(DownloadSummary.STATUS_DOWNLOADING)) {
                                ds.pause();
                                ds.resume();
                                debugOut("Paused download.");
                            }
                            DownloadPool.set(ds);
                            debugOut("Set download summary.");
                        } catch (Exception e) {
                            debugErr(e);
                        }
                    }
                } catch (Exception e) {
                    debugErr(e);
                } finally {
                    IOUtil.safeClose(in);
                    IOUtil.safeClose(fis);
                }
            }
            // listen for changes to the download pool
            DownloadPool.addListener(new DownloadPoolListener() {

                public void downloadAdded(DownloadPoolEvent event) {
                    save();
                }

                public void downloadUpdated(DownloadPoolEvent event) {
                    save();
                }

                public void downloadRemoved(DownloadPoolEvent event) {
                    save();
                }
            });
        }
    };

    /**
     *
     */
    public DownloadCache() {
        // needed for lazy loading after the GUI renders
        lazyLoadThread.setDaemon(true);
        LazyLoadAllSlowStuffAfterGUIRenders.add(this);
    }

    /**
     *
     */
    public void lazyLoad() {
        lazyLoadThread.start();
    }

    /**
     * 
     */
    private synchronized static void save() {
        if (saving) {
            return;
        }
        saving = true;
        Thread t = new Thread("Saving Download Cache") {

            @Override
            public void run() {
                ThreadUtil.safeSleep(2500);
                // save
                synchronized (fileLock) {
                    FileOutputStream fos = null;
                    BufferedOutputStream out = null;
                    try {
                        fos = new FileOutputStream(file);
                        out = new BufferedOutputStream(fos);
                        for (DownloadSummary ds : DownloadPool.getAll()) {
                            try {
                                ds.serialize(out);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        IOUtil.safeClose(out);
                        IOUtil.safeClose(fos);
                    }
                }
                saving = false;
            }
        };
        t.start();
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        DownloadCache.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static final boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static final void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(DownloadCache.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param e
     */
    private static final void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
