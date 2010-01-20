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

import org.tranche.configuration.Configuration;
import org.tranche.hash.BigHash;

/**
 * <p>Listener for important actions involving FlatFileTrancheServer.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public interface FlatFileTrancheServerListener {

    /**
     * <p>Fired when data chunk is added.</p>
     * @param hash
     */
    public void dataChunkAdded(BigHash hash);

    /**
     * <p>Fired when meta data chunk is added.</p>
     * @param hash
     */
    public void metaDataChunkAdded(BigHash hash);

    /**
     * <p>Fired when data chunk is deleted.</p>
     * @param hash
     */
    public void dataChunkDeleted(BigHash hash);

    /**
     * <p>Fired when meta data chunk is deleted.</p>
     * @param hash
     */
    public void metaDataChunkDeleted(BigHash hash);

    /**
     * <p>Fired when configuration is set.</p>
     * @param config
     */
    public void configurationSet(Configuration config);
}
