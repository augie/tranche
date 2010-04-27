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
import org.tranche.hash.BigHash;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.TreeSet;
import org.tranche.commons.DebugUtil;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.hash.span.HashSpan;
import org.tranche.remote.RemoteUtil;
import org.tranche.security.SecurityUtil;

/**
 * <p>A utility class to read/write configuration files.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith <bryanesmith at gmail dot com>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ConfigurationUtil {

    /**
     * <p>Returns a configuration object from an input stream.</p>
     * @param is
     * @return Configuration
     */
    public static Configuration read(InputStream is) throws Exception {
        // first get the status big
        int flags = RemoteUtil.readInt(is);
        // get the int for the version -- throw an exception if it isn't 1n
        Configuration config = null;
        if (Configuration.isVersionOne(flags)) {
            config = readVersionOne(is, flags);
        } else if (Configuration.isVersionTwo(flags)) {
            config = readVersionTwo(is, flags);
        } else {
            throw new RuntimeException("Unsupported configuration data flags: " + flags);
        }
        config.setFlags(Configuration.VERSION_TWO);
        return config;
    }

    private static Configuration readVersionOne(InputStream is, int flags) throws Exception {
        DebugUtil.debugOut(ConfigurationUtil.class, "Reading version one.");
        // make a new MetaData object
        Configuration config = new Configuration();
        config.setFlags(flags);

        // read all of the users
        int numberOfUsers = RemoteUtil.readInt(is);
        TreeSet<User> users = new TreeSet();
        for (int i = 0; i < numberOfUsers; i++) {
            try {
                // get the flags
                int userFlags = RemoteUtil.readInt(is);
                // get the certificate
                byte[] encodedCertificate = RemoteUtil.readDataBytes(is);
                X509Certificate cert = SecurityUtil.getCertificate(encodedCertificate);

                // make a new user
                User user = new User();
                user.setFlags(userFlags);
                user.setCertificate(cert);
                users.add(user);
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }
        config.setUsers(users);

        // read all of the directory configurations
        TreeSet<DataDirectoryConfiguration> dataDirectories = new TreeSet();
        int numberOfDataDirectories = RemoteUtil.readInt(is);
        for (int i = 0; i < numberOfDataDirectories; i++) {
            try {
                // get the name
                String file = RemoteUtil.readLine(is);
                // get the size limit
                long sizeLimit = RemoteUtil.readLong(is);

                // make the config
                DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(file, sizeLimit);
                // add to the set
                dataDirectories.add(ddc);
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }
        config.setDataDirectories(dataDirectories);

        // read in all of the hashes
        TreeSet<ServerConfiguration> serverConfigurations = new TreeSet();
        int numberOfServerConfigurations = RemoteUtil.readInt(is);
        for (int i = 0; i < numberOfServerConfigurations; i++) {
            try {
                // get the name of the type
                String type = RemoteUtil.readLine(is);
                // get the port
                int port = RemoteUtil.readInt(is);
                // get the host name
                String hostName = RemoteUtil.readLine(is);

                // make the config
                ServerConfiguration sc = new ServerConfiguration(type, port, hostName);
                // add to the set
                serverConfigurations.add(sc);
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }
        config.setServerConfigs(serverConfigurations);

        // read in all of the hash-spans this server handles
        Set<HashSpan> hashSpans = config.getHashSpans();
        int numberOfHashSpans = RemoteUtil.readInt(is);
        for (int i = 0; i < numberOfHashSpans; i++) {
            try {
                // make in to objects
                BigHash firstHash = BigHash.createFromBytes(RemoteUtil.readDataBytes(is));
                BigHash lastHash = BigHash.createFromBytes(RemoteUtil.readDataBytes(is));
                // add to the set
                hashSpans.add(new HashSpan(firstHash, lastHash));
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }

        // read in all of the projects that this server handles
        Set<BigHash> stickyProjects = config.getStickyProjects();
        int numberOfStickyProjects = RemoteUtil.readInt(is);
        for (int i = 0; i < numberOfStickyProjects; i++) {
            try {
                // get the starting hash
                byte[] hashBytes = RemoteUtil.readDataBytes(is);
                // make in to objects
                BigHash hash = BigHash.createFromBytes(hashBytes);
                // add to the set
                stickyProjects.add(hash);
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }

        // try to get the number of name/value pairs -- this wasn't in original protocol, thus try/catch
        int numberKeyValuePairs = -1;
        try {
            numberKeyValuePairs = RemoteUtil.readInt(is);
        } catch (Exception e) {
            // no config elements, return so
            return config;
        }

        // parse all of the name/value pairs
        String nextKey, nextValue;
        for (int i = 0; i < numberKeyValuePairs; i++) {
            try {
                // first line is the key
                nextKey = RemoteUtil.readLine(is);
                // second line is the value
                nextValue = RemoteUtil.readLine(is);
                // add to the config
                config.setValue(nextKey, nextValue);
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }

        // return the configuration
        return config;
    }

    private static Configuration readVersionTwo(InputStream is, int flags) throws Exception {
        DebugUtil.debugOut(ConfigurationUtil.class, "Reading version two.");
        // make a new MetaData object
        Configuration config = new Configuration();
        config.setFlags(flags);

        // read all of the users
        int numberOfUsers = RemoteUtil.readInt(is);
        TreeSet<User> users = new TreeSet();
        for (int i = 0; i < numberOfUsers; i++) {
            try {
                // get the flags
                int userFlags = RemoteUtil.readInt(is);
                // get the certificate
                byte[] encodedCertificate = RemoteUtil.readDataBytes(is);
                X509Certificate cert = SecurityUtil.getCertificate(encodedCertificate);

                // make a new user
                User user = new User();
                user.setFlags(userFlags);
                user.setCertificate(cert);
                users.add(user);
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }
        config.setUsers(users);

        // read all of the directory configurations
        TreeSet<DataDirectoryConfiguration> dataDirectories = new TreeSet();
        int numberOfDataDirectories = RemoteUtil.readInt(is);
        for (int i = 0; i < numberOfDataDirectories; i++) {
            try {
                // get the name
                String file = RemoteUtil.readLine(is);
                // get the size limit
                long sizeLimit = RemoteUtil.readLong(is);

                // make the config
                DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(file, sizeLimit);
                // add to the set
                dataDirectories.add(ddc);
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }
        config.setDataDirectories(dataDirectories);

        // read in all of the hashes
        TreeSet<ServerConfiguration> serverConfigurations = new TreeSet();
        int numberOfServerConfigurations = RemoteUtil.readInt(is);
        for (int i = 0; i < numberOfServerConfigurations; i++) {
            try {
                // get the name of the type
                String type = RemoteUtil.readLine(is);
                // get the port
                int port = RemoteUtil.readInt(is);
                // get the host name
                String hostName = RemoteUtil.readLine(is);

                // make the config
                ServerConfiguration sc = new ServerConfiguration(type, port, hostName);
                // add to the set
                serverConfigurations.add(sc);
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }
        config.setServerConfigs(serverConfigurations);

        // read in all of the hash-spans this server handles
        Set<HashSpan> hashSpans = config.getHashSpans();
        int numberOfHashSpans = RemoteUtil.readInt(is);
        DebugUtil.debugOut(ConfigurationUtil.class, "Hash spans: " + numberOfHashSpans);
        for (int i = 0; i < numberOfHashSpans; i++) {
            try {
                // make in to objects
                BigHash firstHash = BigHash.createFromBytes(RemoteUtil.readDataBytes(is));
                BigHash lastHash = BigHash.createFromBytes(RemoteUtil.readDataBytes(is));
                // add to the set
                HashSpan hashSpan = new HashSpan(firstHash, lastHash);
                hashSpans.add(hashSpan);
                DebugUtil.debugOut(ConfigurationUtil.class, hashSpan.toString());
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }

        // read in all of the target hash-spans this server handles
//        Set<HashSpan> targetHashSpans = config.getTargetHashSpans();
        int numberOfTargetHashSpans = RemoteUtil.readInt(is);
        for (int i = 0; i < numberOfTargetHashSpans; i++) {
            try {
                // make in to objects
                BigHash firstHash = BigHash.createFromBytes(RemoteUtil.readDataBytes(is));
                BigHash lastHash = BigHash.createFromBytes(RemoteUtil.readDataBytes(is));
                // add to the set
//                targetHashSpans.add(new HashSpan(firstHash, lastHash));
                config.addTargetHashSpan(new HashSpan(firstHash, lastHash));
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }

        // read in all of the projects that this server handles
        Set<BigHash> stickyProjects = config.getStickyProjects();
        int numberOfStickyProjects = RemoteUtil.readInt(is);
        for (int i = 0; i < numberOfStickyProjects; i++) {
            try {
                // get the starting hash
                byte[] hashBytes = RemoteUtil.readDataBytes(is);
                // make in to objects
                BigHash hash = BigHash.createFromBytes(hashBytes);
                // add to the set
                stickyProjects.add(hash);
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }

        // try to get the number of name/value pairs -- this wasn't in original protocol, thus try/catch
        int numberKeyValuePairs = -1;
        try {
            numberKeyValuePairs = RemoteUtil.readInt(is);
        } catch (Exception e) {
            // no config elements, return so
            return config;
        }

        // parse all of the name/value pairs
        String nextKey, nextValue;
        for (int i = 0; i < numberKeyValuePairs; i++) {
            try {
                // first line is the key
                nextKey = RemoteUtil.readLine(is);
                // second line is the value
                nextValue = RemoteUtil.readLine(is);
                // add to the config
                config.setValue(nextKey, nextValue);
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigurationUtil.class, e);
            }
        }

        // return the configuration
        return config;
    }

    /**
     * <p>Writes a SplitFile object to the given OutputStream. This method does *not* use Java's object serialization mechanism in order to let non-Java code use them.</p>
     * @param config 
     * @param out The OutputStream to serialize the file to.
     * @throws java.io.IOException Should any exception occur.
     */
    public static void write(Configuration config, OutputStream out) throws Exception {
        // write the flags before anything else
        RemoteUtil.writeInt(config.getFlags(), out);
        // write depending on version
        if (config.isVersionOne()) {
            writeVersionOne(config, out);
        } else if (config.isVersionTwo()) {
            writeVersionTwo(config, out);
        } else {
            throw new RuntimeException("Unsupported configuration data flags: " + config.getFlags());
        }
    }

    private static void writeVersionOne(Configuration config, OutputStream out) throws Exception {
        DebugUtil.debugOut(ConfigurationUtil.class, "Writing version one.");
        // write out the users
        User[] users = config.getUsers().toArray(new User[0]);
        // write out the length
        RemoteUtil.writeInt(users.length, out);
        for (User user : users) {
            // write the flags
            RemoteUtil.writeInt(user.getFlags(), out);
            // write the certificate
            RemoteUtil.writeData(user.getCertificate().getEncoded(), out);
        }


        // write out the data directories
        DataDirectoryConfiguration[] ddcs = config.getDataDirectories().toArray(new DataDirectoryConfiguration[0]);
        // write the number of configurations
        RemoteUtil.writeInt(ddcs.length, out);
        for (DataDirectoryConfiguration ddc : ddcs) {
            // write the filename
            RemoteUtil.writeLine(ddc.getDirectory(), out);
            // write the size limit
            RemoteUtil.writeLong(ddc.getSizeLimit(), out);
        }

        // write out the configurations
        ServerConfiguration[] scs = config.getServerConfigs().toArray(new ServerConfiguration[0]);
        RemoteUtil.writeInt(scs.length, out);
        for (ServerConfiguration sc : scs) {
            RemoteUtil.writeLine(sc.getType(), out);
            RemoteUtil.writeInt(sc.getPort(), out);
            RemoteUtil.writeLine(sc.getHostName(), out);
        }

        // write out the hash spans
        HashSpan[] fileSpans = config.getHashSpans().toArray(new HashSpan[0]);
        RemoteUtil.writeInt(fileSpans.length, out);
        for (HashSpan fileSpan : fileSpans) {
            RemoteUtil.writeData(fileSpan.getFirst().toByteArray(), out);
            RemoteUtil.writeData(fileSpan.getLast().toByteArray(), out);
        }

        // write out the sticky projects
        BigHash[] stickyProjects = config.getStickyProjects().toArray(new BigHash[0]);
        RemoteUtil.writeInt(stickyProjects.length, out);
        for (BigHash stickyProject : stickyProjects) {
            RemoteUtil.writeData(stickyProject.toByteArray(), out);
        }

        // Write out key/value pairs
        int numPairs = config.numberKeyValuePairs();
        RemoteUtil.writeInt(numPairs, out);

        if (numPairs > 0) {
            Set<String> keys = config.getValueKeys();
            for (String nextKey : keys) {
                RemoteUtil.writeLine(nextKey, out);
                RemoteUtil.writeLine(config.getValue(nextKey), out);
            }
        }

        // flush
        out.flush();
    }

    private static void writeVersionTwo(Configuration config, OutputStream out) throws Exception {
        DebugUtil.debugOut(ConfigurationUtil.class, "Writing version two.");
        // write out the users
        User[] users = config.getUsers().toArray(new User[0]);
        // write out the length
        RemoteUtil.writeInt(users.length, out);
        for (User user : users) {
            // write the flags
            RemoteUtil.writeInt(user.getFlags(), out);
            // write the certificate
            RemoteUtil.writeData(user.getCertificate().getEncoded(), out);
        }


        // write out the data directories
        DataDirectoryConfiguration[] ddcs = config.getDataDirectories().toArray(new DataDirectoryConfiguration[0]);
        // write the number of configurations
        RemoteUtil.writeInt(ddcs.length, out);
        for (DataDirectoryConfiguration ddc : ddcs) {
            // write the filename
            RemoteUtil.writeLine(ddc.getDirectory(), out);
            // write the size limit
            RemoteUtil.writeLong(ddc.getSizeLimit(), out);
        }

        // write out the configurations
        ServerConfiguration[] scs = config.getServerConfigs().toArray(new ServerConfiguration[0]);
        RemoteUtil.writeInt(scs.length, out);
        for (ServerConfiguration sc : scs) {
            RemoteUtil.writeLine(sc.getType(), out);
            RemoteUtil.writeInt(sc.getPort(), out);
            RemoteUtil.writeLine(sc.getHostName(), out);
        }

        // write out the hash spans
        HashSpan[] fileSpans = config.getHashSpans().toArray(new HashSpan[0]);
        RemoteUtil.writeInt(fileSpans.length, out);
        for (HashSpan fileSpan : fileSpans) {
            RemoteUtil.writeData(fileSpan.getFirst().toByteArray(), out);
            RemoteUtil.writeData(fileSpan.getLast().toByteArray(), out);
        }

        // write out the target hash spans
        HashSpan[] targetHashSpans = config.getTargetHashSpans().toArray(new HashSpan[0]);
        RemoteUtil.writeInt(targetHashSpans.length, out);
        for (HashSpan fileSpan : targetHashSpans) {
            RemoteUtil.writeData(fileSpan.getFirst().toByteArray(), out);
            RemoteUtil.writeData(fileSpan.getLast().toByteArray(), out);
        }

        // write out the sticky projects
        BigHash[] stickyProjects = config.getStickyProjects().toArray(new BigHash[0]);
        RemoteUtil.writeInt(stickyProjects.length, out);
        for (BigHash stickyProject : stickyProjects) {
            RemoteUtil.writeData(stickyProject.toByteArray(), out);
        }

        // Write out key/value pairs
        int numPairs = config.numberKeyValuePairs();
        RemoteUtil.writeInt(numPairs, out);

        if (numPairs > 0) {
            Set<String> keys = config.getValueKeys();
            for (String nextKey : keys) {
                RemoteUtil.writeLine(nextKey, out);
                RemoteUtil.writeLine(config.getValue(nextKey), out);
            }
        }

        // flush
        out.flush();
    }
}
