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
package org.tranche.configuration;

import org.tranche.users.User;
import org.tranche.flatfile.ServerConfiguration;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.tranche.ConfigureTranche;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.security.SecurityUtil;
import org.tranche.users.UserCertificateUtil;

/**
 * <p>Configuration for a server, kept in memory</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E Smith <bryanesmith at gmail dot com>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class Configuration {

    /**
     * <p>Flag for version one.</p>
     */
    public static final int VERSION_ONE = (int) Math.pow(2, 0);
    /**
     * <p>Flag for version two. Changes include:</p>
     * <ul>
     *   <li>Support for target hash spans.</li>
     *   <li>Remove certificates from ConfigurationUtil.read/write for hash spans.</li>
     * </ul>
     */
    public static final int VERSION_TWO = (int) Math.pow(2, 1);
    public static final int SIZE_MAX_HASH_SPANS = 10;
    public static final String DEFAULT_GROUP = "Other";
    /**
     * <p>Version value.</p>
     */
    private int flags = VERSION_TWO;
    // the list of directories for storing data
    private Set<DataDirectoryConfiguration> dataDirectories = new TreeSet();
    // the list of trusted authorities
    private Set<User> users = new TreeSet();
    // server configurations
    private Set<ServerConfiguration> serverConfigs = new TreeSet();
    // spans of hashes signed by a particular user that this server attempts to keep a copy of
    private Set<HashSpan> hashSpans = new TreeSet();
    private Set<HashSpan> targetHashSpans = new TreeSet();
    // project's that this server attempt to keep a copy of
    private Set<BigHash> stickyProjects = new TreeSet();
    // just for convenience. don't serialize this
    private Map<String, String> valueMap = new HashMap();

    /* -----------------------------------------------------------------------------
     *  Deadlock has happened on servers. Need to use separate (non-static) locks 
     *  on everything to prevent potential complicated deadlocks.
     * -----------------------------------------------------------------------------
     */
    private final Object dataDirectoriesLock = new Object();
    private final Object usersLock = new Object();
    private final Object serverConfigurationLock = new Object();
    private final Object flagsLock = new Object();
    private final Object hashSpanLock = new Object();
    private final Object targetHashSpanLock = new Object();
    private final Object stickyProjectsLock = new Object();
    private final Object valueMapLock = new Object();

    /**
     * <p>Gets the data directories on this server.</p>
     * @return
     */
    public Set<DataDirectoryConfiguration> getDataDirectories() {
        synchronized (dataDirectoriesLock) {
            return dataDirectories;
        }
    }

    /**
     * <p>Sets the data directories for this server.</p>
     * @param dataDirectories
     */
    public void setDataDirectories(Set<DataDirectoryConfiguration> dataDirectories) {
        synchronized (dataDirectoriesLock) {
            this.dataDirectories = dataDirectories;
        }
    }

    /**
     * <p>Adds the given data directory to the set of data directories on this server.</p>
     * @param dataDirectory
     */
    public void addDataDirectory(DataDirectoryConfiguration dataDirectory) {
        synchronized (dataDirectoriesLock) {
            dataDirectories.add(dataDirectory);
        }
    }

    /**
     * <p>Removes the given data directory from the set of data directories on this server.</p>
     * @param dataDirectory
     */
    public void removeDataDirectory(DataDirectoryConfiguration dataDirectory) {
        synchronized (dataDirectoriesLock) {
            dataDirectories.remove(dataDirectory);
        }
    }

    /**
     * <p>Removes all the data directories from this server.</p>
     */
    public void clearDataDirectories() {
        synchronized (dataDirectoriesLock) {
            dataDirectories.clear();
        }
    }

    /**
     * <p>Gets the set of users on the server.</p>
     * @return
     */
    public Set<User> getUsers() {
        synchronized (usersLock) {
            return users;
        }
    }

    /**
     * <p>Sets the given users on the server.</p>
     * @param users
     */
    public void setUsers(Set<User> users) {
        synchronized (usersLock) {
            this.users = users;
        }
    }

    /**
     * <p>Adds the given user to the server.</p>
     * @param user
     */
    public void addUser(User user) {
        synchronized (usersLock) {
            users.add(user);
        }
    }

    /**
     * <p>Removes the given user from the server.</p>
     * @param user
     */
    public void removeUser(User user) {
        synchronized (usersLock) {
            users.remove(user);
        }
    }

    /**
     * <p>Clears the users from this server.</p>
     */
    public void clearUsers() {
        synchronized (usersLock) {
            users.clear();
        }
    }

    /**
     * <p>Gets the server configurations on this server.</p>
     * @return
     */
    public Set<ServerConfiguration> getServerConfigs() {
        synchronized (serverConfigurationLock) {
            return serverConfigs;
        }
    }

    /**
     * <p>Sets the server configurations on this server.</p>
     * @param serverConfigs
     */
    public void setServerConfigs(Set<ServerConfiguration> serverConfigs) {
        synchronized (serverConfigurationLock) {
            this.serverConfigs = serverConfigs;
        }
    }

    /**
     * <p>Adds the given server configuration to the list of server configurations on this server.</p>
     * @param serverConfig
     */
    public void addServerConfig(ServerConfiguration serverConfig) {
        synchronized (serverConfigurationLock) {
            serverConfigs.add(serverConfig);
        }
    }

    /**
     * <p>Removes the given server configuration to the list of server configurations on this server.</p>
     * @param serverConfig
     */
    public void removeServerConfig(ServerConfiguration serverConfig) {
        synchronized (serverConfigurationLock) {
            serverConfigs.remove(serverConfig);
        }
    }

    /**
     * <p>Clears the server configurations from this server.</p>
     */
    public void clearServerConfigs() {
        synchronized (serverConfigurationLock) {
            serverConfigs.clear();
        }
    }

    /**
     * <p>Gets the flags on this configuration.</p>
     * @return
     */
    public int getFlags() {
        synchronized (flagsLock) {
            return flags;
        }
    }

    /**
     * <p>Sets the flags for this configuration.</p>
     * @param flags
     */
    public void setFlags(int flags) {
        synchronized (flagsLock) {
            this.flags = flags;
        }
    }

    /**
     * <p>Returns the flag for whether the configuration is first version.</p>
     * @return
     */
    public boolean isVersionOne() {
        synchronized (flagsLock) {
            boolean is = (this.getFlags() & Configuration.VERSION_ONE) == Configuration.VERSION_ONE;
            return is;
        }
    }

    /**
     * <p>Returns the flag for whether the configuration is second version.</p>
     * @return
     */
    public boolean isVersionTwo() {
        synchronized (flagsLock) {
            boolean is = (this.getFlags() & Configuration.VERSION_TWO) == Configuration.VERSION_TWO;
            return is;
        }
    }

    /**
     * <p>Checks the supplied flag for whether the configuration is first version.</p>
     * @return
     */
    public static boolean isVersionOne(int flagVal) {
        return (flagVal & Configuration.VERSION_ONE) == Configuration.VERSION_ONE;
    }

    /**
     * <p>Checks the supplied flag for whether the configuration is second version.</p>
     * @return
     */
    public static boolean isVersionTwo(int flagVal) {
        return (flagVal & Configuration.VERSION_TWO) == Configuration.VERSION_TWO;
    }

    /**
     * <p>Get the User that corresponds to the given certificate.</p>
     * @param cert
     * @return
     */
    public User getRecognizedUser(X509Certificate cert) {
        // try each user
        User[] us = this.getUsers().toArray(new User[0]);

        // Check user directly first. This way, user's can have specific permissions!
        for (User u : us) {
            // Check for matching certs. Checks name first, then bytes.
            if (SecurityUtil.certificateNamesMatch(UserCertificateUtil.readUserName(u.getCertificate()), UserCertificateUtil.readUserName(cert))) {

                // Encoded bytes for certs. Only gets here if names matched.
                byte[] cBytes = null;
                byte[] uBytes = null;

                try {
                    cBytes = cert.getEncoded();
                    uBytes = u.getCertificate().getEncoded();
                } catch (Exception ex) {
                    // No fault tolerance
                    continue;
                }

                // Check the encoded bytes for direct match
                if (cBytes.length != uBytes.length) {
                    continue;
                }
                for (int i = 0; i < cBytes.length; i++) {
                    if (cBytes[i] != uBytes[i]) {
                        continue;
                    }
                }

                return u;
            }
        }

        for (User u : us) {
            // Check the issuers, using whatever is their default permissions. This is far more common.
            if (SecurityUtil.certificateNamesMatch(UserCertificateUtil.readUserName(u.getCertificate()), UserCertificateUtil.readIssuerName(cert))) {
                // check the issuer
                try {
                    cert.verify(u.getCertificate().getPublicKey());
                    return u;
                } catch (Exception e) {
                    // noop -- bad signatures
                    e.printStackTrace(System.err);
                }
            }
        }

        // if no user matches, try to see if it is signed by anyone
        return null;
    }

    /**
     * <p>Gets the hash spans for this server.</p>
     * @return
     */
    public Set<HashSpan> getHashSpans() {
        synchronized (hashSpanLock) {
            return hashSpans;
        }
    }

    /**
     * <p>Sets the hash spans for this server.</p>
     * @param hashSpans
     * @throws java.lang.Exception
     */
    public void setHashSpans(Set<HashSpan> hashSpans) throws Exception {
        if (hashSpans.size() > SIZE_MAX_HASH_SPANS) {
            throw new Exception("Maximum number of hash spans allowed is " + SIZE_MAX_HASH_SPANS);
        }
        synchronized (hashSpanLock) {
            this.hashSpans = hashSpans;
        }
    }

    /**
     * <p>Adds the given hash span to this server.</p>
     * @param hashSpan
     * @throws java.lang.Exception
     */
    public void addHashSpan(HashSpan hashSpan) throws Exception {
        if (hashSpans.size() == SIZE_MAX_HASH_SPANS) {
            throw new Exception("Maximum number of hash spans allowed is " + SIZE_MAX_HASH_SPANS);
        }
        synchronized (hashSpanLock) {
            hashSpans.add(hashSpan);
        }
    }

    /**
     * <p>Removes the given hash span from this server.</p>
     * @param hashSpan
     */
    public void removeHashSpan(HashSpan hashSpan) {
        synchronized (hashSpanLock) {
            hashSpans.remove(hashSpan);
        }
    }

    /**
     * <p>Removes all hash spans from this server.</p>
     */
    public void clearHashSpans() {
        synchronized (hashSpanLock) {
            hashSpans.clear();
        }
    }

    /**
     * <p>Gets the target hash spans for this server.</p>
     * @return
     */
    public Set<HashSpan> getTargetHashSpans() {
        synchronized (targetHashSpanLock) {
            return targetHashSpans;
        }
    }

    /**
     * <p>Sets the target hash spans for this server.</p>
     * @param newTargetHashSpans
     * @throws java.lang.Exception
     */
    public void setTargetHashSpans(Set<HashSpan> newTargetHashSpans) throws Exception {
        if (targetHashSpans.size() > SIZE_MAX_HASH_SPANS) {
            throw new Exception("Maximum number of target hash spans allowed is " + SIZE_MAX_HASH_SPANS);
        }
        synchronized (targetHashSpanLock) {
            this.targetHashSpans = newTargetHashSpans;
        }
    }

    /**
     * <p>Adds the given target hash span to this server.</p>
     * @param targetHashSpan
     * @throws java.lang.Exception
     */
    public void addTargetHashSpan(HashSpan targetHashSpan) throws Exception {
        if (targetHashSpans.size() == SIZE_MAX_HASH_SPANS) {
            throw new Exception("Maximum number of target hash spans allowed is " + SIZE_MAX_HASH_SPANS);
        }
        synchronized (targetHashSpanLock) {
            targetHashSpans.add(targetHashSpan);
        }
    }

    /**
     * <p>Removes the given hash span from this server.</p>
     * @param targetHashSpan
     */
    public void removeTargetHashSpan(HashSpan targetHashSpan) {
        synchronized (targetHashSpanLock) {
            targetHashSpans.remove(targetHashSpan);
        }
    }

    /**
     * <p>Removes all hash spans from this server.</p>
     */
    public void clearTargetHashSpans() {
        synchronized (targetHashSpanLock) {
            targetHashSpans.clear();
        }
    }

    /**
     * <p>Gets the sticky projects for this server.</p>
     * @return
     */
    public Set<BigHash> getStickyProjects() {
        synchronized (stickyProjectsLock) {
            return stickyProjects;
        }
    }

    /**
     * <p>Makes the projects with the given hashes as sticky projects to this servers.</p>
     * @param stickyProjects
     */
    public void setStickyProjects(Set<BigHash> stickyProjects) {
        synchronized (stickyProjectsLock) {
            this.stickyProjects = stickyProjects;
        }
    }

    /**
     * <p>Make the project with the given hash as a sticky to this server.</p>
     * @param projectHash
     */
    public void addStickyProject(BigHash projectHash) {
        synchronized (stickyProjectsLock) {
            if (!stickyProjects.contains(projectHash)) {
                stickyProjects.add(projectHash);
            }
        }
    }

    /**
     * <p>Remove the sticky project with the given hash from this server.</p>
     * @param projectHash
     */
    public void removeStickyProject(BigHash projectHash) {
        synchronized (stickyProjectsLock) {
            stickyProjects.remove(projectHash);
        }
    }

    /**
     * <p>Remove all sticky projects from this server.</p>
     */
    public void clearStickyProjects() {
        synchronized (stickyProjectsLock) {
            stickyProjects.clear();
        }
    }

    /**
     * 
     * @return
     */
    public String getName() {
        synchronized (valueMapLock) {
            return valueMap.get(ConfigKeys.NAME);
        }
    }

    /**
     *
     * @return
     */
    public String getGroup() {
        synchronized (valueMapLock) {
            if (valueMap.containsKey(ConfigKeys.GROUP)) {
                return valueMap.get(ConfigKeys.GROUP);
            }
        }
        return DEFAULT_GROUP;
    }

    /**
     * <p>Is configuration set to read-only?</p>
     * @return
     * @deprecated Use canRead() instead.
     * @see #canRead() 
     * @see #canWrite() 
     */
    public boolean isReadOnly() {
        String readOnly = getValue(ConfigKeys.IS_SERVER_READ_ONLY);
        if (readOnly == null) {
            return false;
        }
        return Boolean.valueOf(readOnly);
    }

    /**
     * <p>Is configuration set to write-only?</p>
     * @return
     * @deprecated Use canWrite() instead.
     * @see #canRead() 
     * @see #canWrite() 
     */
    public boolean isWriteOnly() {
        String writeOnly = getValue(ConfigKeys.IS_SERVER_WRITE_ONLY);
        if (writeOnly == null) {
            return false;
        }
        return Boolean.valueOf(writeOnly);
    }

    /**
     * <p>Returns whether server mode flags are set to allow clients to read server's data.</p>
     * @return
     */
    public boolean canRead() {
        try {
            // Note that both flags must permit the action
            boolean system = ServerModeFlag.canRead(Byte.valueOf(getValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM)));
            boolean admin = ServerModeFlag.canRead(Byte.valueOf(getValue(ConfigKeys.SERVER_MODE_FLAG_ADMIN)));
            return system && admin;
        } catch (Exception e) {
        }
        // If there is an exception, should probably assume can read since
        // it is normally allowed
        return true;
    }

    /**
     * <p>Returns whether server mode flags are set to allow clients to write data to server.</p>
     * @return
     */
    public boolean canWrite() {
        try {
            // Note that both flags must permit the action
            boolean system = ServerModeFlag.canWrite(Byte.valueOf(getValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM)));
            boolean admin = ServerModeFlag.canWrite(Byte.valueOf(getValue(ConfigKeys.SERVER_MODE_FLAG_ADMIN)));
            return system && admin;
        } catch (Exception e) {
        }
        // If there is an exception, should probably assume can write since
        // it is normally allowed
        return true;
    }

    /**
     * <p>Add a key/value pair to local server's configuration.</p>
     * <p>Has no impact on server-wide configuration attributes. However, if set key is equal to key in server-wide configuration attributes, then local value is selected over network-wide value.</p>
     * @param key
     * @param value
     */
    public void setValue(String key, String value) {
        synchronized (valueMapLock) {
            if (this.valueMap == null) {
                this.valueMap = new HashMap();
            }
            this.valueMap.put(key, value);
        }
    }

    /**
     * <p>Retrieves value from configuration attributes. Looks for value first in local server attributes, and if not found, checks network-wide attributes.</p>
     * @param key
     * @return Null if not found; otherwise, retruns value found in local or network-wide attributes
     */
    public String getValue(String key) {
        String value = null;

        // First check whether server's configuration has value. If does, use that!
        synchronized (valueMapLock) {
            if (this.valueMap != null) {
                value = this.valueMap.get(key);
            }
        }

        // If configuration doesn't have, use network-wide values
        if (value == null) {
            value = ConfigureTranche.getServerConfigurationAttributes().get(key);
        }

        return value;
    }

    /**
     * <p>Returns set of keys from local and network-wide attributes.</p>
     * @return
     */
    public Set<String> getValueKeys() {
        return getValueKeys(true);
    }

    /**
     * <p>Returns set of keys for attributes.</p>
     * @param includeNetworkKeys If true, will also include keys from network-wide attributes; otherwise, they will be excluded
     * @return
     */
    public Set<String> getValueKeys(boolean includeNetworkKeys) {

        // Make a copy to avoid problems
        Set<String> valueKeys = new HashSet();

        // Add in-memory values
        synchronized (valueMapLock) {
            valueKeys.addAll(this.valueMap.keySet());
        }

        // Add network values
        if (includeNetworkKeys) {
            valueKeys.addAll(ConfigureTranche.getServerConfigurationAttributes().keySet());
        }

        return Collections.unmodifiableSet(valueKeys);
    }

    /**
     * <p>Returns total number of attribute pairs, including local and network-wide.</p>
     * <p>If an attribute pair is found in both, it will only be counted once, since only one value will be used (the local value).</p>
     * @return
     */
    public int numberKeyValuePairs() {
        return getValueKeys(true).size();
    }

    /**
     * <p>Returns total number of attribute pairs.</p>
     * @param includeNetworkKeys If true, will also include keys from network-wide attributes; otherwise, they will be excluded
     * @return
     */
    public int numberKeyValuePairs(boolean includeNetworkKeys) {
        return getValueKeys(includeNetworkKeys).size();
    }

    /**
     * <p>Checks whether key is found in local or network-wide configuration attributes.</p>
     * @param key
     * @return
     */
    public boolean hasKey(String key) {
        return hasKey(key, true);
    }

    /**
     * <p>Checks whether key is found in network-wide configuration attributes.</p>
     * @param key
     * @param includeNetworkKeys If true, will also include keys from network-wide attributes; otherwise, they will be excluded
     * @return
     */
    public boolean hasKey(String key, boolean includeNetworkKeys) {
        return getValueKeys(includeNetworkKeys).contains(key);
    }

    /**
     * <p>Removes a key/value pair from local server configuration attributes. Has no effect if not present.</p>
     * <p>This has no impact on server-wide attributes. However, if remove a value that overrides network value, then the network value will be used.</p>
     * @param key
     * @return
     */
    public boolean removeKeyValuePair(String key) {

        if (!this.hasKey(key)) {
            return false;
        }
        synchronized (valueMapLock) {
            this.valueMap.remove(key);
        }

        return true;
    }

    /**
     * <p>Create a deep copy of the configuration so in-memory copies aren't shared.</p>
     * <p>Useful if want a separate copy of Configuration to edit without editting the original.</p>
     * @return
     */
    @Override()
    public Configuration clone() {
        Configuration c = new Configuration();

        // Copy flag
        synchronized (flagsLock) {
            c.setFlags(getFlags());
        }

        // Copy Set of DataDirectoryConfiguration
        synchronized (dataDirectoriesLock) {
            Set<DataDirectoryConfiguration> ddcsCopy = new HashSet();
            for (DataDirectoryConfiguration ddc : getDataDirectories()) {
                ddcsCopy.add(ddc.clone());
            }
            c.setDataDirectories(ddcsCopy);
        }

        // Copy Set of HashSpan
        synchronized (hashSpanLock) {
            try {
                Set<HashSpan> hashSpansCopy = new HashSet();
                for (HashSpan span : this.getHashSpans()) {
                    hashSpansCopy.add(span.clone());
                }
                c.setHashSpans(hashSpansCopy);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Copy Set of target HashSpan
        synchronized (targetHashSpanLock) {
            try {
                Set<HashSpan> targetHashSpansCopy = new HashSet();
                for (HashSpan span : this.getTargetHashSpans()) {
                    targetHashSpansCopy.add(span.clone());
                }
                c.setTargetHashSpans(targetHashSpansCopy);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Copy Set of ServerConfiguration
        synchronized (serverConfigurationLock) {
            Set<ServerConfiguration> serverConfigurationsCopy = new HashSet();
            for (ServerConfiguration serverConfiguration : this.getServerConfigs()) {
                serverConfigurationsCopy.add(serverConfiguration.clone());
            }
            c.setServerConfigs(serverConfigurationsCopy);
        }

        // Copy Set of sticky project hashes
        synchronized (stickyProjectsLock) {
            Set<BigHash> stickyProjectsCopy = new HashSet();
            for (BigHash h : this.getStickyProjects()) {
                stickyProjectsCopy.add(h);
            }
            c.setStickyProjects(stickyProjectsCopy);
        }

        // Copy Set of users
        synchronized (usersLock) {
            Set<User> usersCopy = new HashSet();
            for (User u : this.getUsers()) {
                usersCopy.add(u);
            }
            c.setUsers(usersCopy);
        }

        // Copy Map of name-value pairs
        synchronized (valueMapLock) {
            for (String key : this.getValueKeys(false)) {
                c.setValue(key, this.getValue(key));
            }
        }

        return c;
    }
}
