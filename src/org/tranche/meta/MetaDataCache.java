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
package org.tranche.meta;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.tranche.exceptions.TodoException;
import org.tranche.hash.BigHash;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class MetaDataCache {

    public static final int DEFAULT_SIZE_LIMIT = 50;
    private static int sizeLimit = DEFAULT_SIZE_LIMIT;
    private static final Map<BigHash, MetaData> cache = new HashMap<BigHash, MetaData>();
    private static final LinkedList<BigHash> cacheAccess = new LinkedList<BigHash>();

    /**
     * 
     */
    private MetaDataCache() {
    }

    /**
     *
     * @param hash
     * @param metaData
     */
    public synchronized static void set(BigHash hash, MetaData metaData) {
        throw new TodoException();
//        // note that this hash was accessed
//        cacheAccess.remove(hash);
//        cacheAccess.add(hash);
//        // set the meta data
//        cache.put(hash, metaData);
//        // check size limit
//        if (cacheAccess.size() > sizeLimit) {
//            // remove the entry that was longest ago used
//            cache.remove(cacheAccess.removeFirst());
//        }
    }

    /**
     * <p>Returns a clone of the meta data with the given hash.</p>
     * @param hash
     * @return
     */
    public static MetaData get(BigHash hash) {
        return get(hash, false);
    }

    /**
     * 
     * @param hash
     * @param downloadIfNotPresent
     * @return
     */
    public static MetaData get(BigHash hash, boolean downloadIfNotPresent) {
        return get(hash, downloadIfNotPresent, null);
    }

    /**
     * 
     * @param hash
     * @param downloadIfNotPresent
     * @return
     */
    public synchronized static MetaData get(BigHash hash, boolean downloadIfNotPresent, Collection<String> serversHosts) {
        throw new TodoException();
//        if (!cache.containsKey(hash) && downloadIfNotPresent) {
//            try {
//                GetFileTool gft = new GetFileTool();
//                gft.setHash(hash);
//                if (serversHosts != null) {
//                    gft.setServersToUse(new LinkedList<String>(serversHosts));
//                }
//                set(hash, gft.getMetaData());
//            } catch (Exception e) {
//            }
//        }
//        if (cache.containsKey(hash)) {
//            // note that this hash was accessed
//            cacheAccess.remove(hash);
//            cacheAccess.add(hash);
//            // get the meta data from the cache
//            return cache.get(hash).clone();
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
        MetaDataCache.sizeLimit = sizeLimit;
    }

    /**
     * 
     * @return
     */
    public static int getSizeLimit() {
        return sizeLimit;
    }
}
