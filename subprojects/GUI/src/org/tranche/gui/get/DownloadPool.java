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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.tranche.gui.ConfigureTrancheGUI;
import org.tranche.time.TimeUtil;
import org.tranche.util.PreferencesUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class DownloadPool {

    private static int downloadPoolSize = 3;
    private static final ArrayList<DownloadSummary> pool = new ArrayList<DownloadSummary>();
    private static final List<DownloadPoolListener> listeners = new ArrayList<DownloadPoolListener>();


    static {
        try {
            // Use preferences to set downloading pool size
            PreferencesUtil.waitForStartup();
            setDownloadPoolSize(PreferencesUtil.getInt(ConfigureTrancheGUI.PROP_DOWNLOAD_POOL_SIZE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void waitForAllToFinish() {
        for (DownloadSummary ds : getAll()) {
            ds.waitForFinish();
        }
    }

    public static void waitForAllToFinish(long millis) {
        long waited = 0;
        for (DownloadSummary ds : getAll()) {
            long start = TimeUtil.getTrancheTimestamp();
            ds.waitForFinish(millis - waited);
            waited += TimeUtil.getTrancheTimestamp() - start;
        }
    }

    public static void set(DownloadSummary summary) {
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
        checkForDownloadsToStart();
        checkForDownloadsToQueue();
        // notify all listening objects
        DownloadPoolEvent dpe = new DownloadPoolEvent(summary);
        for (DownloadPoolListener listener : getListeners()) {
            if (isNew) {
                listener.downloadAdded(dpe);
            } else {
                listener.downloadUpdated(dpe);
            }
        }
    }

    public static void remove(DownloadSummary summary) {
        if (summary == null) {
            return;
        }
        // put the summary
        synchronized (pool) {
            pool.remove(summary);
        }
        // name of method says it all
        checkForDownloadsToStart();
        // notify all listening objects
        DownloadPoolEvent dpe = new DownloadPoolEvent(summary);
        for (DownloadPoolListener listener : getListeners()) {
            listener.downloadRemoved(dpe);
        }
    }

    public static boolean contains(DownloadSummary ds) {
        synchronized (pool) {
            return pool.contains(ds);
        }
    }

    public static List<DownloadSummary> getAll() {
        ArrayList<DownloadSummary> returnList = new ArrayList<DownloadSummary>();
        synchronized (pool) {
            returnList.addAll(pool);
        }
        return Collections.unmodifiableList(returnList);
    }

    public static int getDownloadingCount() {
        int count = 0;
        for (DownloadSummary ds : getAll()) {
            if (ds.isDownloading()) {
                count++;
            }
        }
        return count;
    }

    public static int getQueuedCount() {
        int count = 0;
        for (DownloadSummary ds : getAll()) {
            if (ds.isQueued()) {
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

    public static void setDownloadPoolSize(int _downloadPoolSize) {
        downloadPoolSize = _downloadPoolSize;
        checkForDownloadsToStart();
    }

    public static int getDownloadPoolSize() {
        return downloadPoolSize;
    }

    public static boolean isRoomInDownloadPool() {
        return getDownloadingCount() < downloadPoolSize || downloadPoolSize == 0;
    }

    public static void checkForDownloadsToStart() {
        boolean newDownloadStarted = true;
        while (isRoomInDownloadPool() && newDownloadStarted) {
            newDownloadStarted = false;
            for (DownloadSummary ds : getAll()) {
                if (ds.isQueued() || ds.isStarting()) {
                    ds.start();
                    newDownloadStarted = true;
                    break;
                }
            }
        }
    }

    public static void checkForDownloadsToQueue() {
        for (DownloadSummary ds : getAll()) {
            if (ds.getStatus().equals(DownloadSummary.STATUS_STARTING)) {
                ds.queue();
            }
        }
    }

    public static void removeFinished() {
        for (DownloadSummary ds : getAll()) {
            if (ds.isFinished()) {
                remove(ds);
            }
        }
    }

    public static void clear() {
        for (DownloadSummary ds : getAll()) {
            if (!ds.isFinished()) {
                ds.stop();
            }
            remove(ds);
        }
    }

    public static Collection<DownloadPoolListener> getListeners() {
        List<DownloadPoolListener> list = new LinkedList<DownloadPoolListener>();
        synchronized (listeners) {
            list.addAll(listeners);
        }
        return Collections.unmodifiableCollection(list);
    }

    public static void addListener(DownloadPoolListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
}
