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
package org.tranche.gui.add;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.tranche.gui.LazyLoadAllSlowStuffAfterGUIRenders;
import org.tranche.gui.LazyLoadable;
import org.tranche.gui.user.UserZipFileEvent;
import org.tranche.gui.user.UserZipFileListener;
import org.tranche.gui.util.GUIUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.users.UserZipFile;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.PersistentFileUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.ThreadUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class UploadCache implements LazyLoadable {

    private static boolean debug = false;
    private static File file;
    private static final Object fileLock = new Object();
    private static boolean saving = false;
    public static Thread lazyLoadThread = new Thread("Lazy Load Upload Cache") {

        @Override
        public void run() {
            // add a listener for user changes
            GUIUtil.addUserZipFileListener(new UserZipFileListener() {

                public void userSignedIn(UserZipFileEvent event) {
                    try {
                        // load the uploads from the cache
                        file = PersistentFileUtil.getPersistentFile("uploads-" + event.getUserZipFile().getUserNameFromCert());
                        debugOut("Loading upload cache: " + file.getAbsolutePath());

                        // read the file
                        synchronized (fileLock) {
                            FileInputStream fis = null;
                            BufferedInputStream bis = null;
                            try {
                                fis = new FileInputStream(SecurityUtil.decrypt(event.getUserZipFile().getPassphrase(), file));
                                bis = new BufferedInputStream(fis);
                                // read all the preferences from the file
                                while (bis.available() > 0) {
                                    try {
                                        debugOut("Loading upload summary.");
                                        UploadSummary us = new UploadSummary(bis);
                                        debugOut("Loaded upload summary.");
                                        if (us.getStatus().equals(UploadSummary.STATUS_UPLOADING)) {
                                            us.pause();
                                            //us.resume();
                                            debugOut("Paused upload.");
                                        }
                                        UploadPool.set(us);
                                        debugOut("Set upload summary.");
                                    } catch (Exception e) {
                                        debugErr(e);
                                    }
                                }
                            } catch (Exception e) {
                                debugErr(e);
                            } finally {
                                IOUtil.safeClose(bis);
                                IOUtil.safeClose(fis);
                            }
                        }
                    } catch (Exception e) {
                        debugErr(e);
                    }
                }

                public void userSignedOut(UserZipFileEvent event) {
                    file = null;
                }
            });
            UploadPool.addListener(new UploadPoolListener() {

                public void uploadAdded(UploadPoolEvent event) {
                    save();
                }

                public void uploadUpdated(UploadPoolEvent event) {
                    save();
                }

                public void uploadRemoved(UploadPoolEvent event) {
                    save();
                }
            });
        }
    };

    /**
     *
     */
    public UploadCache() {
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
    public synchronized static void save() {
        if (saving) {
            return;
        }
        final UserZipFile uzf = GUIUtil.getUser();
        if (uzf == null) {
            return;
        }
        saving = true;
        Thread t = new Thread("Saving Upload Cache") {

            @Override
            public void run() {
                ThreadUtil.safeSleep(2500);
                // save
                synchronized (fileLock) {
                    FileOutputStream fos = null;
                    BufferedOutputStream out = null;
                    File tempFile = TempFileUtil.createTemporaryFile();
                    try {
                        fos = new FileOutputStream(tempFile);
                        out = new BufferedOutputStream(fos);
                        for (UploadSummary us : UploadPool.getAll()) {
                            us.serialize(out);
                        }

                        // close the output stream
                        IOUtil.safeClose(out);
                        IOUtil.safeClose(fos);

                        // encrypt the file
                        File encryptedFile = SecurityUtil.encrypt(uzf.getPassphrase(), tempFile);
                        IOUtil.copyFile(encryptedFile, file);
                    } catch (Exception e) {
                    } finally {
                        IOUtil.safeClose(out);
                        IOUtil.safeClose(fos);
                        IOUtil.safeDelete(tempFile);
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
        UploadCache.debug = debug;
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
            DebugUtil.printOut(UploadCache.class.getName() + "> " + line);
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
