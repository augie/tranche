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
package org.tranche.server;

import org.tranche.security.Signature;

/**
 * <p>Helps organize signatures and nonces for certain propagated requests.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class HostSignatureNonceWrapper {

    public final String host;
    public final Signature sig;
    public final byte[] nonce;

    /**
     * <p>Helps organize signatures and nonces for certain propagated requests.</p>
     * @param host
     * @param sig
     * @param nonce
     */
    public HostSignatureNonceWrapper(String host, Signature sig, byte[] nonce) {
        this.host = host;
        this.sig = sig;
        this.nonce = nonce;
    }

    /**
     * 
     * @return
     */
    @Override()
    public int hashCode() {
        // Host is really only thing that matters; these aren't stored long
        return this.host.hashCode();
    }

    /**
     * <p>Tests that other host is the same host for this wrapper.</p>
     * <p>Note that equals method for class calls this method if parameter is of class String. See equals JavaDoc for this class for more information.</p>
     * @param otherHost
     * @return
     */
    public boolean isHost(String otherHost) {
        return this.host.equals(otherHost);
    }

    /**
     * <p>Tests that other wrapper has same host, signature and nonce.</p>
     * <p>Note that equals method for class calls this method if parameter is of class HostSignatureNonceWrapper. See equals JavaDoc for this class for more information.</p>
     * @param otherWrapper
     * @return
     */
    public boolean isHostSignatureNonceWrapper(HostSignatureNonceWrapper otherWrapper) {
        // Test equality of hots
        if (!otherWrapper.host.equals(this.host)) {
            return false;
        }

        // Test equality of signature
        if (!otherWrapper.sig.equals(this.sig)) {
            return false;
        }

        // Test equality of nonce
        if (otherWrapper.nonce.length != this.nonce.length) {
            return false;
        }

        for (int i = 0; i < this.nonce.length; i++) {
            if (otherWrapper.nonce[i] != this.nonce[i]) {
                return false;
            }
        }

        // If gets here, equal
        return true;
    }

    /**
     * <p>Can test for equality for any of following:</p>
     * <ul>
     *   <li>HostSignatureNonceWrapper: must match host, sig and nonce</li>
     *   <li>String (host): only needs to match host. Used for quick lookups when satisfying propagation request</li>
     * </ul>
     * @param o
     * @return
     * @see #isHost(java.lang.String) 
     * @see #isHostSignatureNonceWrapper(org.tranche.server.HostSignatureNonceWrapper) 
     */
    @Override()
    public boolean equals(Object o) {
        if (o instanceof HostSignatureNonceWrapper) {
            return isHostSignatureNonceWrapper((HostSignatureNonceWrapper) o);
        } else if (o instanceof String) {
            return isHost((String) o);
        }
        return false;
    }
}
