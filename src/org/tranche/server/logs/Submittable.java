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
package org.tranche.server.logs;

import org.tranche.security.Signature;
import org.tranche.hash.BigHash;

/**
 * Interface for all classes to which the LogSubmitter can submit.
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public interface Submittable {

    /**
     * Set data for log writing.
     * @param hash
     * @param sig
     * @param ip
     */
    public void setData(BigHash hash, Signature sig, String ip);

    /**
     * Set meta data for log writing.
     * @param hash
     * @param sig
     * @param ip
     */
    public void setMetaData(BigHash hash, Signature sig, String ip);

    /**
     * Set configuration for log writing.
     * @param sig
     * @param ip
     */
    public void setConfiguration(Signature sig, String ip);

    /**
     * Retrieve data.
     * @param hash
     * @param ip
     */
    public void getData(BigHash hash, String ip);

    /**
     * Retrieve meta data.
     * @param hash
     * @param ip
     */
    public void getMetaData(BigHash hash, String ip);

    /**
     * Retrieve configuration.
     * @param ip
     */
    public void getConfiguration(String ip);

    /**
     * Retrieve nonce.
     * @param ip
     */
    public void getNonce(String ip);

    /**
     * Close file.
     */
    public void close();
}
