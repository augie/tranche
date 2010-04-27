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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.tranche.gui.ConfigureTrancheGUI;
import org.tranche.time.TimeUtil;
import org.tranche.util.PreferencesUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class UploadPool {

    private static int uploadPoolSize = 3;
    private static final ArrayList<UploadSummary> pool = new ArrayList<UploadSummary>();
    private static final List<UploadPoolListener> listeners = new ArrayList<UploadPoolListener>();


    static {
        // Use preferences to set uploading pool size
        PreferencesUtil.waitForStartup();
        setUploadPoolSize(PreferencesUtil.getInt(ConfigureTrancheGUI.PROP_UPLOAD_POOL_SIZE));
    }

    public static void waitForAllToFinish() {
        for (UploadSummary us : getAll()) {
            us.waitForFinish();
        }
    }

    public static void waitForAllToFinish(long millis) {
        long waited = 0;
        for (UploadSummary us : getAll()) {
            long start = TimeUtil.getTrancheTimestamp();
            us.waitForFinish(millis - waited);
            waited += TimeUtil.getTrancheTimestamp() - start;
        }
    }

    public static void set(UploadSummary summary) {
        if (summary == null) {
            return;
        }
        // is this new?
        boolean isNew = !contains(summary);
        if (isNew) {
            // put the summary
            synchronized (pool) {
                pool.add(summary);
            }
        }
        // name of method says it all
        checkForUploadsToStart();
        checkForUploadsToQueue();
        // notify all listening objects
        UploadPoolEvent upe = new UploadPoolEvent(summary);
        for (UploadPoolListener listener : getListeners()) {
            if (isNew) {
                listener.uploadAdded(upe);
            } else {
                listener.uploadUpdated(upe);
            }
        }
    }

    public static void remove(UploadSummary summary) {
        if (summary == null) {
            return;
        }
        // put the summary
        synchronized (pool) {
            pool.remove(summary);
        }
        // name of method says it all
        checkForUploadsToStart();
        // notify all listening objects
        UploadPoolEvent upe = new UploadPoolEvent(summary);
        for (UploadPoolListener listener : getListeners()) {
            listener.uploadRemoved(upe);
        }
    }

    public static boolean contains(UploadSummary summary) {
        synchronized (pool) {
            return pool.contains(summary);
        }
    }

    public static List<UploadSummary> getAll() {
        ArrayList<UploadSummary> returnList = new ArrayList<UploadSummary>();
        synchronized (pool) {
            returnList.addAll(pool);
        }
        return returnList;
    }

    public static int getUploadingCount() {
        int count = 0;
        for (UploadSummary us : getAll()) {
            if (us.isUploading()) {
                count++;
            }
        }
        return count;
    }

    public static int getQueuedCount() {
        int count = 0;
        for (UploadSummary us : getAll()) {
            if (us.isQueued()) {
                count++;
            }
        }
        return count;
    }

    public static int size() {
        synchronized (pool) {
            return pool.size();
        }
    }

    public static void setUploadPoolSize(int _uploadPoolSize) {
        uploadPoolSize = _uploadPoolSize;
        checkForUploadsToStart();
    }

    public static int getUploadPoolSize() {
        return uploadPoolSize;
    }

    public static boolean isRoomInUploadPool() {
        return getUploadingCount() < uploadPoolSize || uploadPoolSize == 0;
    }

    public static void checkForUploadsToStart() {
        boolean newUploadStarted = true;
        while (isRoomInUploadPool() && newUploadStarted) {
            newUploadStarted = false;
            for (UploadSummary us : getAll()) {
                if (us.isQueued() || us.isStarting()) {
                    us.start();
                    newUploadStarted = true;
                    break;
                }
            }
        }
    }

    public static void checkForUploadsToQueue() {
        for (UploadSummary us : getAll()) {
            if (us.getStatus().equals(UploadSummary.STATUS_STARTING)) {
                us.queue();
            }
        }
    }

    public static void removeFinished() {
        for (UploadSummary us : getAll()) {
            if (us.isFinished()) {
                remove(us);
            }
        }
    }

    public static void clear() {
        for (UploadSummary us : getAll()) {
            if (!us.isFinished()) {
                us.stop();
            }
            remove(us);
        }
    }

    public static Collection<UploadPoolListener> getListeners() {
        synchronized (listeners) {
            return Collections.unmodifiableCollection(listeners);
        }
    }

    public static void addListener(UploadPoolListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
}
