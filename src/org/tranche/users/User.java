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
package org.tranche.users;

import org.tranche.hash.Base16;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import org.tranche.commons.Debuggable;

/**
 * <p>Represents a Tranche user.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class User extends Debuggable implements Comparable {

    /**
     * <p>Flag for whether the User can set data.</p>
     */
    public static final int CAN_SET_DATA = (int) Math.pow(2, 0);
    /**
     * <p>Flag for whether the User can set meta data.</p>
     */
    public static final int CAN_SET_META_DATA = (int) Math.pow(2, 1);
    /**
     * <p>Flag for whether the User can delete data.</p>
     */
    public static final int CAN_DELETE_DATA = (int) Math.pow(2, 2);
    /**
     * <p>Flag for whether the User can delete meta data.</p>
     */
    public static final int CAN_DELETE_META_DATA = (int) Math.pow(2, 3);
    /**
     * <p>Space at 4-5 for what used to be delete sticky data and meta data</p>
     */
    /**
     * <p>Flag for whether the User can get configuration.</p>
     */
    public static final int CAN_GET_CONFIGURATION = (int) Math.pow(2, 6);
    /**
     * <p>Flag for whether the User can set configuration.</p>
     */
    public static final int CAN_SET_CONFIGURATION = (int) Math.pow(2, 7);
    /**
     * <p>Flag for User version 1.0.</p>
     */
    public static final int VERSION_ONE = (int) Math.pow(2, 8);
    // flags 8-31 are reserved
    /**
     * <p>Flag to allow user to do everything, like an admin.</p>
     */
    public static final int ALL_PRIVILEGES = CAN_SET_DATA | CAN_SET_META_DATA | CAN_DELETE_DATA | CAN_DELETE_META_DATA | CAN_GET_CONFIGURATION | CAN_SET_CONFIGURATION;
    /**
     * <p>User can do nothing.</p>
     */
    public static final int NO_PRIVILEGES = 0;
    // X.509 certificate representing the user
    private X509Certificate certificate = null;
    // flags representing this user's permissions
    private int flags = VERSION_ONE;

    /**
     * <p>Get the certificate.</p>
     * @return
     */
    public X509Certificate getCertificate() {
        return certificate;
    }

    /**
     * <p>Set the certificate</p>
     * @param certificate
     */
    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    /**
     * <p>Get the flags.</p>
     * @return
     */
    public int getFlags() {
        return flags;
    }

    /**
     * <p>Set the flags.</p>
     * @param flags
     */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /**
     * <p>The permission flag for whether this user can set data.</p>
     * @return
     */
    public boolean canSetData() {
        return (getFlags() & User.CAN_SET_DATA) > 0;
    }

    /**
     * <p>The permission flag for whether this user can set meta data.</p>
     * @return
     */
    public boolean canSetMetaData() {
        return (getFlags() & User.CAN_SET_META_DATA) > 0;
    }

    /**
     * <p>The permission flag for whether this user can delete data.</p>
     * @return
     */
    public boolean canDeleteData() {
        return (getFlags() & User.CAN_DELETE_DATA) > 0;
    }

    /**
     * <p>The permission flag for whether this user can delete meta data.</p>
     * @return
     */
    public boolean canDeleteMetaData() {
        return (getFlags() & User.CAN_DELETE_META_DATA) > 0;
    }

    /**
     * <p>The permission flag for whether this user can get configuration.</p>
     * @return
     */
    public boolean canGetConfiguration() {
        return (getFlags() & User.CAN_GET_CONFIGURATION) > 0;
    }

    /**
     * <p>The permission flag for whether this user can set configuration.</p>
     * @return
     */
    public boolean canSetConfiguration() {
        return (getFlags() & User.CAN_SET_CONFIGURATION) > 0;
    }

    /**
     * <p>The flag for whether the User object version is 1.0.</p>
     * @return
     */
    public boolean isVersionOne() {
        return (getFlags() & User.VERSION_ONE) > 0;
    }

    /**
     * <p>Compare the certificates between User objects.</p>
     * @param o
     * @return
     */
    public int compareTo(Object o) {
        try {
            User u = (User) o;
            String a = Base16.encode(getCertificate().getEncoded());
            String b = Base16.encode(u.getCertificate().getEncoded());
            return a.compareTo(b);
        } catch (CertificateEncodingException ex) {
            throw new RuntimeException("Can't encode certificates!", ex);
        }
    }

    /**
     * <p>Compare two user objects. To be equal, they must be the same certificate and have the same permissions.</p>
     * @param u
     * @return
     */
    public boolean equals(User u) {
        return compareTo(u) == 0 && flags == u.getFlags();
    }
}
