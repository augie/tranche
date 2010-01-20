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
package org.tranche.flatfile;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.hash.BigHash;

/**
 * <p>Used to cache the location of a chunk.</p>
 * <p>The idea is that hasData or hasMetaData is used before get or set. If cache results, won't have to read in header from data block.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class DataBlockCache {

    /**
     * <p>The number of entries to keep in memory for data or meta data chunks.</p>
     */
    public static final int CACHE_ENTRY_SIZE = 250;
    private final List<DataBlockCacheEntry> dataEntriesList;
    private final Map<BigHash, DataBlockCacheEntry> dataEntriesMap;
    private final List<DataBlockCacheEntry> metaDataEntriesList;
    private final Map<BigHash, DataBlockCacheEntry> metaDataEntriesMap;
    private final Object dataCacheLock = new Object(),  metaDataCacheLock = new Object();

    /**
     * 
     */
    public DataBlockCache() {
        dataEntriesList = new LinkedList();
        dataEntriesMap = new HashMap();
        metaDataEntriesList = new LinkedList();
        metaDataEntriesMap = new HashMap();
    }
    
    /**
     * <p>Add a DataBlockCacheEntry to the cache. Removes oldest entries if maximum size is exceeded.</p>
     * @param e
     * @param isMetaData
     */
    public void add(final DataBlockCacheEntry e, final boolean isMetaData) {
        if (!isMetaData) {
            synchronized (dataCacheLock) {
                // Add to data cache
                dataEntriesList.add(e);
                dataEntriesMap.put(e.chunkHash, e);

                // Prune cache if necessary
                while (dataEntriesList.size() > CACHE_ENTRY_SIZE) {
                    DataBlockCacheEntry remove = dataEntriesList.remove(0);
                    dataEntriesMap.remove(remove);
                }
            }
        } else {
            synchronized (metaDataCacheLock) {
                // Add to meta cache
                metaDataEntriesList.add(e);
                metaDataEntriesMap.put(e.chunkHash, e);

                // Prune cache if necessary
                while (metaDataEntriesList.size() > CACHE_ENTRY_SIZE) {
                    DataBlockCacheEntry remove = metaDataEntriesList.remove(0);
                    metaDataEntriesMap.remove(remove);
                }
            }
        }
    }

    /**
     * <p>Returns the DataBlockCacheEntry from the cache if exists, or null if not there.</p>
     * @param hash
     * @param isMetaData
     * @return
     * @throws org.tranche.exceptions.AssertionFailedException
     * @throws java.lang.Exception
     */
    public DataBlockCacheEntry get(BigHash hash, boolean isMetaData) throws AssertionFailedException, Exception {
        if (!isMetaData) {
            synchronized (dataCacheLock) {
                // Quickly assert that collections are synchronized
                if (dataEntriesList.size() != dataEntriesMap.size()) {
                    throw new AssertionFailedException("dataEntriesList.size()<" + dataEntriesList.size() + "> != dataEntriesSet.size()<" + dataEntriesMap.size() + "> in DataBlockCache.add");
                }

                return dataEntriesMap.get(hash);
            }
        } else {
            synchronized (metaDataCacheLock) {
                // Quickly assert that collections are synchronized
                if (metaDataEntriesList.size() != metaDataEntriesMap.size()) {
                    throw new AssertionFailedException("metaDataEntriesList.size()<" + metaDataEntriesList.size() + "> != metaDataEntriesSet.size()<" + metaDataEntriesMap.size() + "> in DataBlockCache.add");
                }

                return metaDataEntriesMap.get(hash);
            }
        }
    }

    /**
     * <p>Clear out the cache resources.</p>
     */
    public void clear() {
        synchronized (dataCacheLock) {
            dataEntriesList.clear();
            dataEntriesMap.clear();
        }
        synchronized (metaDataCacheLock) {
            metaDataEntriesList.clear();
            metaDataEntriesMap.clear();
        }
    }

    /**
     * <p>Remove a specific DataBlockCacheEntry from the cache, if there. Otherwise, do nothing.</p>
     * @param h
     * @param isMetaData
     */
    public void remove(BigHash h, boolean isMetaData) {
        if (!isMetaData) {
            synchronized (dataCacheLock) {
                DataBlockCacheEntry e = dataEntriesMap.remove(h);
                if (e != null) {
                    dataEntriesList.remove(e);
                }
            }
        } else {
            synchronized (metaDataCacheLock) {
                DataBlockCacheEntry e = metaDataEntriesMap.remove(h);
                if (e != null) {
                    metaDataEntriesList.remove(e);
                }
            }
        }
    }
}
