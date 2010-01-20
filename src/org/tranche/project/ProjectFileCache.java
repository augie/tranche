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
package org.tranche.project;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.tranche.exceptions.TodoException;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;

/**
 *
 * @author James "Augie" Hill - augie@828producotions.com
 */
public class ProjectFileCache {

    public static final int DEFAULT_SIZE_LIMIT = 10;
    private static int sizeLimit = DEFAULT_SIZE_LIMIT;
    private static Map<BigHash, ProjectFile> cache = new HashMap<BigHash, ProjectFile>();
    private static LinkedList<BigHash> cacheAccess = new LinkedList<BigHash>();
    private static final GetFileTool gft = new GetFileTool();

    /**
     *
     */
    private ProjectFileCache() {
    }

    /**
     * 
     * @param hash
     * @param projectFile
     */
    public synchronized static void set(BigHash hash, ProjectFile projectFile) {
        throw new TodoException();
//        // note that this hash was accessed
//        cacheAccess.remove(hash);
//        cacheAccess.add(hash);
//        // set
//        cache.put(hash, projectFile);
//        // check size limit
//        if (cacheAccess.size() > sizeLimit) {
//            // remove the entry that was longest ago used
//            cache.remove(cacheAccess.removeFirst());
//        }
    }

    /**
     * <p>Returns the project file with the given hash.</p>
     * @param hash
     * @return
     */
    public static ProjectFile get(BigHash hash) {
        return get(hash, false);
    }

    /**
     *
     * @param hash
     * @param downloadIfNotPresent
     * @return
     */
    public static ProjectFile get(BigHash hash, boolean downloadIfNotPresent) {
        return get(hash, downloadIfNotPresent, null, null);
    }

    /**
     * 
     * @param hash
     * @param downloadIfNotPresent
     * @param passphrase
     * @return
     */
    public static ProjectFile get(BigHash hash, boolean downloadIfNotPresent, String passphrase) {
        return get(hash, downloadIfNotPresent, passphrase, null);
    }

    /**
     * 
     * @param hash
     * @param downloadIfNotPresent
     * @param serversHosts
     * @return
     */
    public static ProjectFile get(BigHash hash, boolean downloadIfNotPresent, Collection<String> serversHosts) {
        return get(hash, downloadIfNotPresent, null, serversHosts);
    }

    /**
     * 
     * @param hash
     * @param downloadIfNotPresent
     * @param serversHosts
     * @return
     */
    public synchronized static ProjectFile get(BigHash hash, boolean downloadIfNotPresent, String passphrase, Collection<String> serversHosts) {
        throw new TodoException();
//        if (!cache.containsKey(hash) && downloadIfNotPresent) {
//            try {
//                gft.setHash(hash);
//                if (serversHosts != null) {
//                    gft.setServersToUse(new LinkedList<String>(serversHosts));
//                }
//                set(hash, gft.getProjectFile());
//            } catch (Exception e) {
//            }
//        }
//        if (cache.containsKey(hash)) {
//            // note that this hash was accessed
//            cacheAccess.remove(hash);
//            cacheAccess.add(hash);
//            // get from the cache
//            return cache.get(hash);
//        } else {
//            return null;
//        }
    }

    /**
     *
     * @param hash
     * @return
     */
    public synchronized static boolean contains(BigHash hash) {
        throw new TodoException();
//        return cache.containsKey(hash);
    }

    /**
     *
     * @param sizeLimit
     */
    public static void setSizeLimit(int sizeLimit) {
        ProjectFileCache.sizeLimit = sizeLimit;
    }

    /**
     *
     * @return
     */
    public static int getSizeLimit() {
        return sizeLimit;
    }
}
