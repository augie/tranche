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

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.tranche.TrancheServer;
import org.tranche.configuration.ConfigKeys;
import org.tranche.configuration.Configuration;
import org.tranche.hash.Base64;
import org.tranche.routing.RoutingTrancheServer;
import org.tranche.time.TimeUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;

/**
 * <p>An abstraction to track nonces on the server-side. It will also ensure that the same nonce isn't being used twice during operation of the server.</p>
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class NonceMap {

    /**
     * Size of nonce in bytes
     */
    public static final int NONCE_BYTES = 8;
    /**
     * <p>Default maximum number of nonces to store in memory.</p>
     */
    public static final int DEFAULT_MAX_NONCES = 10000;
    /**
     * Store the nonces
     */
    private final Set<String> nonces;
    /**
     * 
     */
    private final TrancheServer trancheServer;

    public NonceMap(FlatFileTrancheServer trancheServer) {
        this.trancheServer = trancheServer;
        nonces = new LinkedHashSet<String>();
    }

    public NonceMap(RoutingTrancheServer trancheServer) {
        this.trancheServer = trancheServer;
        nonces = new LinkedHashSet<String>();
    }

    /**
     * 
     */
    private NonceMap() {
        throw new RuntimeException("Called constructor with FlatFileTrancheServer as parameter.");
    }

    /**
     * <p>Check to see whether the nonce bytes are valid.</p>
     * @param   nonce   the nonce bytes 
     * @return          <code>true</code> if the hash map contains the nonce bytes;
     *                  <code>false</code> otherwise
     *
     */
    public boolean contains(byte[] nonce) {
        // convert to a string
        String s = Base64.encodeBytes(nonce);
        // check in the hash map
        synchronized (nonces) {
            return nonces.contains(s);
        }
    }

    /**
     * <p>Create a new nonce.</p>
     * @return  nonce bytes
     * 
     */
    public byte[] newNonce() {
        // Generate nonce
        byte[] nonce = new byte[NONCE_BYTES];
        RandomUtil.getBytes(nonce);

        String nonceStr = Base64.encodeBytes(nonce);

        // nonce take already, get a different one
        int attempt = 0;
        boolean cont = false;

        synchronized (nonces) {
            cont = nonces.contains(nonceStr);
        }

        while (cont) {
            RandomUtil.getBytes(nonce);
            nonceStr = Base64.encodeBytes(nonce);
            attempt++;

            if (attempt >= 10000) {
                throw new RuntimeException("Tried " + attempt + " times to get new nonce in NonceMap.newNonce, couldn't!?!");
            }

            synchronized (nonces) {
                cont = nonces.contains(nonceStr);
            }
        }

        // Avoid deadlock by accessing here instead of within synchronized block
        final int maxSize = getMaximumSizeInternal();

        // Add nonce to collection
        synchronized (nonces) {
            nonces.add(nonceStr);

            // if there are too many, remove the first
            for (Iterator<String> it = nonces.iterator(); it.hasNext() && nonces.size() > maxSize;) {
//            nonces.remove(it.next());

                // Should be faster
                it.next();
                it.remove();
            }
        }

        return nonce;
    }
    /**
     * 
     */
    private int cachedMaximumSize = -1;
    /**
     * 
     */
    private long lastMaximumSizeUpdateTimestamp = -1;
    /**
     * 
     */
    private final static long timeEllapseBeforeUpdate = 1000 * 60 * 10;

    /**
     * <p>Caches and returns maximum size of collection. Periodically updates. Intended for quick, internal use.</p>
     * @return
     */
    private int getMaximumSizeInternal() {
        boolean isSizeSetYet = (cachedMaximumSize != -1);
        boolean isTimeForUpdate = (TimeUtil.getTrancheTimestamp() - lastMaximumSizeUpdateTimestamp >= timeEllapseBeforeUpdate);

        if (!isSizeSetYet || isTimeForUpdate) {
            try {
                cachedMaximumSize = getMaximumSize();
            } finally {
                // Make sure time updated
                lastMaximumSizeUpdateTimestamp = TimeUtil.getTrancheTimestamp();
            }
        }

        return cachedMaximumSize;
    }

    /**
     * <p>Get maximum size of NonceMap. Note this is always up to date because checks FlatFileTrancheServer's configuration, so don't call too often.</p>
     * <p>There's an private version of this method, getMethodSizeInternal, that uses a cached version to save time.</p>
     * @return
     */
    public int getMaximumSize() {
        try {
            if (this.trancheServer instanceof FlatFileTrancheServer) {
                FlatFileTrancheServer ffts = (FlatFileTrancheServer) this.trancheServer;
                return Integer.parseInt(ffts.getConfiguration().getValue(ConfigKeys.NONCE_MAP_MAX_SIZE));
            } else if (this.trancheServer instanceof RoutingTrancheServer) {
                RoutingTrancheServer rots = (RoutingTrancheServer) this.trancheServer;
                return Integer.parseInt(rots.getConfiguration().getValue(ConfigKeys.NONCE_MAP_MAX_SIZE));
            }
        } catch (Exception ex) {
        }

        // Fall back on default value
        return NonceMap.DEFAULT_MAX_NONCES;
    }

    /**
     * <p>Sets the maximum size in the Configuration for the FlatFileTrancheServer.</p>
     * @param maxSize
     * @param cert
     * @param key
     * @throws java.lang.Exception
     */
    public void setMaximumSize(final int maxSize, X509Certificate cert, PrivateKey key) throws Exception {
        // Update FlatFileTrancheServer configuration attribute
        Configuration c = null;

        if (this.trancheServer instanceof FlatFileTrancheServer) {
            FlatFileTrancheServer ffts = (FlatFileTrancheServer) this.trancheServer;
            c = ffts.getConfiguration();
        } else if (this.trancheServer instanceof RoutingTrancheServer) {
            RoutingTrancheServer rots = (RoutingTrancheServer) this.trancheServer;
            c = rots.getConfiguration();
        }

        c.setValue(ConfigKeys.NONCE_MAP_MAX_SIZE, String.valueOf(maxSize));
        IOUtil.setConfiguration(trancheServer, c, cert, key);

        // Update cached size and timestamp
        cachedMaximumSize = maxSize;
        lastMaximumSizeUpdateTimestamp = TimeUtil.getTrancheTimestamp();
    }

    /**
     * <p>Remove nonce from collection of valid nonces.</p>
     * @param   nonce   the nonce bytes to remove from the collection
     * 
     */
    public void remove(byte[] nonce) {
        // remove from the cache
        synchronized (nonces) {
            nonces.remove(Base64.encodeBytes(nonce));
        }
    }

    /**
     * <p>Total number of nonces currently in collection.</p>
     * @return  
     * 
     */
    public synchronized int size() {
        synchronized (nonces) {
            return nonces.size();
        }
    }
}
