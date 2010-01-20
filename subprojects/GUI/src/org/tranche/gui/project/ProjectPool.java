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
package org.tranche.gui.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.gui.LazyLoadAllSlowStuffAfterGUIRenders;
import org.tranche.gui.LazyLoadable;
import org.tranche.project.ProjectSummary;
import org.tranche.project.ProjectSummaryCache;
import org.tranche.hash.BigHash;
import org.tranche.util.DebugUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ProjectPool implements LazyLoadable {

    private static boolean debug = false;
    private static final Set<ProjectSummary> pool = new HashSet<ProjectSummary>();
    private static final List<ProjectPoolListener> listeners = new ArrayList<ProjectPoolListener>();
    private static boolean lazyLoadStarted = false;
    private static Thread lazyLoadThread = new Thread("Lazy Load Project Pool") {

        @Override
        public void run() {
            try {
                lazyLoadStarted = true;
                // load all of the projects from the project cache into the pool
                ProjectSummaryCache.waitForStartup();
                // load from cache to project pool
                for (ProjectSummary ps : ProjectSummaryCache.getProjects()) {
                    try {
                        // it is in the cache and not in the pool yet
                        if (!contains(ps.hash)) {
                            // get the project from the cache and set in the pool
                            set(ps);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 
     */
    public ProjectPool() {
        // needed for lazy loading after the GUI renders
        lazyLoadThread.setDaemon(true);
        lazyLoadThread.setPriority(Thread.MIN_PRIORITY);
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
    public static void waitForStartup() {
        while (lazyLoadThread.isAlive() || !lazyLoadStarted) {
            try {
                lazyLoadThread.join();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 
     * @param summary
     */
    public static void set(ProjectSummary summary) {
        if (summary == null) {
            return;
        }
        // determine whether this is the first summary for this hash
        boolean isNew = !contains(summary);
        // put the summary
        synchronized (pool) {
            pool.add(summary);
        }
        // notify all listening objects
        if (isNew) {
            fireProjectAdded(summary);
        } else {
            fireProjectUpdated(summary);
        }
    }

    /**
     * 
     * @param hash
     * @return
     */
    public static List<ProjectSummary> get(BigHash hash) {
        List<ProjectSummary> list = new LinkedList<ProjectSummary>();
        for (ProjectSummary ps : getProjectSummaries()) {
            if (ps.hash.equals(hash)) {
                list.add(ps);
            }
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * 
     * @param hash
     * @param uploaderName
     * @param uploadTimestamp
     * @return
     */
    public static ProjectSummary get(BigHash hash, String uploaderName, Long uploadTimestamp) {
        ProjectSummary returnVal = null;
        if (uploaderName != null && uploadTimestamp != null) {
            for (ProjectSummary ps : get(hash)) {
                if (ps.uploader.equals(uploaderName) && ps.uploadTimestamp == uploadTimestamp) {
                    returnVal = ps;
                }
            }
        }
        if (contains(hash)) {
            returnVal = get(hash).get(0);
        }
        return returnVal;
    }

    /**
     * 
     * @param summary
     */
    public static void remove(ProjectSummary summary) {
        synchronized (pool) {
            pool.remove(summary);
        }
    }

    /**
     * 
     * @param hash
     * @return
     */
    public static boolean contains(BigHash hash) {
        return !get(hash).isEmpty();
    }

    /**
     * 
     * @param summary
     * @return
     */
    public static boolean contains(ProjectSummary summary) {
        synchronized (pool) {
            return pool.contains(summary);
        }
    }

    /**
     *
     * @return
     */
    public static long size() {
        synchronized (pool) {
            return pool.size();
        }
    }

    /**
     *
     * @return
     */
    public static Collection<ProjectSummary> getProjectSummaries() {
        List<ProjectSummary> list = new LinkedList<ProjectSummary>();
        synchronized (pool) {
            list.addAll(pool);
        }
        return Collections.unmodifiableCollection(list);
    }

    /**
     *
     * @return
     */
    public static Set<BigHash> getBigHashes() {
        Set<BigHash> hashes = new HashSet<BigHash>();
        for (ProjectSummary ps : getProjectSummaries()) {
            hashes.add(ps.hash);
        }
        return Collections.unmodifiableSet(hashes);
    }

    /**
     * 
     * @return
     */
    private static Collection<ProjectPoolListener> getListeners() {
        List<ProjectPoolListener> list = new LinkedList<ProjectPoolListener>();
        synchronized (listeners) {
            list.addAll(listeners);
        }
        return Collections.unmodifiableCollection(list);
    }

    /**
     *
     * @param summary
     */
    private static void fireProjectAdded(ProjectSummary summary) {
        for (ProjectPoolListener listener : getListeners()) {
            listener.projectAdded(new ProjectPoolEvent(summary));
        }
    }

    /**
     * 
     * @param summary
     */
    private static void fireProjectRemoved(ProjectSummary summary) {
        for (ProjectPoolListener listener : getListeners()) {
            listener.projectRemoved(new ProjectPoolEvent(summary));
        }
    }

    /**
     * 
     * @param summary
     */
    private static void fireProjectUpdated(ProjectSummary summary) {
        for (ProjectPoolListener listener : getListeners()) {
            listener.projectUpdated(new ProjectPoolEvent(summary));
        }
    }

    /**
     * 
     * @param listener
     */
    public static void addListener(ProjectPoolListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static final void setDebug(boolean debug) {
        ProjectPool.debug = debug;
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
            DebugUtil.printOut(ProjectPool.class.getName() + "> " + line);
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
