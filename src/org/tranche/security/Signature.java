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
package org.tranche.security;

import java.security.cert.X509Certificate;
import org.tranche.users.UserCertificateUtil;

/**
 * <p>Abstracts the absolute minimum required for a signature. This is what the Signature interface should require, but the initial code had that funky 'getEncoding()' concept.</p>
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augie@umich.edu
 */
public class Signature {

    private byte[] bytes;
    private String algorithm;
    private X509Certificate cert;

    /**
     * @param sig           the signature's bytes received
     * @param algorithm     the signature's algorithm received
     * @param cert          the signature's certificate received 
     */
    public Signature(byte[] sig, String algorithm, X509Certificate cert) {
        this.bytes = sig;
        this.algorithm = algorithm;
        this.cert = cert;
    }

    /**
     * @return  the signature's bytes
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * @return  the signature's algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * @return  the signature's certificate
     */
    public X509Certificate getCert() {
        return cert;
    }

    /**
     * 
     * @return
     */
    public String getUserName() {
        return UserCertificateUtil.readUserName(cert);
    }

    /**
     * <p>A custom equals() method for use by the hash algorithms.</p>
     * @param   o   The object compared to this signature object.
     * @return      <code>true</code> if the object is equal to this signature object;
     *              <code>false</code> otherwise
     */
    @Override()
    public boolean equals(Object o) {
        if (o instanceof Signature) {
            Signature sig = (Signature) o;
            try {
                // check that they are the same
                return sig.getAlgorithm().equals(getAlgorithm()) &&
                        new String(sig.getCert().getEncoded()).equals(new String(getCert().getEncoded())) &&
                        new String(sig.getBytes()).equals(new String(getBytes()));
            } catch (Exception e) {
                return false;
            }
        } else {
            return super.equals(o);
        }
    }

    /**
     * <p>The custom hash code.</p>
     * @return  the custom hash code
     */
    @Override()
    public int hashCode() {
        try {
            return new String(getAlgorithm() + new String(getCert().getEncoded()) + new String(getBytes())).hashCode();
        } catch (Exception e) {
            return super.hashCode();
        }
    }
}
