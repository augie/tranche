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

import org.tranche.hash.BigHash;

/**
 * <p>An individual entry in the DataBlockUtil cache, which can be turned on and off.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class DataBlockCacheEntry {

    /**
     * <p>The hash representing the data chunk.</p>
     */
    protected final BigHash chunkHash;
    /**
     * <p>Which DataBlock contains the chunk.</p>
     */
    protected final DataBlock dataBlock;
    /**
     * <p>The offset, in bytes, of where the chunk exists in the DataBlock.</p>
     */
    protected final int offset;
    /**
     * <p>The size, in bytes, of the chunk.</p>
     */
    protected final int size;

    /**
     * 
     * @param chunkHash
     * @param dataBlock
     * @param offset
     * @param size
     */
    private DataBlockCacheEntry(final BigHash chunkHash, final DataBlock dataBlock, final int offset, final int size) {
        this.chunkHash = chunkHash;
        this.dataBlock = dataBlock;
        this.offset = offset;
        this.size = size;
    }

    /**
     * <p>Create an entry for the cache.</p>
     * @param chunkHash
     * @param dataBlock
     * @param offset Offset of chunk in payload of DataBlock
     * @return
     */
    public static DataBlockCacheEntry create(final BigHash chunkHash, final DataBlock dataBlock, final int offset, final int size) {
        return new DataBlockCacheEntry(chunkHash, dataBlock, offset, size);
    }

    @Override()
    public int hashCode() {
        return chunkHash.hashCode();
    }

    @Override()
    public boolean equals(Object h) {
        if (h instanceof BigHash) {
            BigHash hash = (BigHash) h;
            if (hash.equals(this.chunkHash)) {
                return true;
            } else {
                return false;
            }
        } else if (h instanceof DataBlockCacheEntry) {
            DataBlockCacheEntry e = (DataBlockCacheEntry) h;
            if (e.chunkHash.equals(this.chunkHash)) {
                return true;
            } else {
                return false;
            }
        }
        return h.equals(DataBlockCacheEntry.this);
    }
}
