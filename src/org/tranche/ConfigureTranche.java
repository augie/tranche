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
package org.tranche;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.network.NetworkUtil;
import org.tranche.remote.RemoteUtil;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.tasks.ExcelSavvyLineSplitter;
import org.tranche.time.TimeUtil;
import org.tranche.users.User;
import org.tranche.util.ThreadUtil;

/**
 * <p>This is here to load a Tranche network's configuration information, including servers and certificates.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ConfigureTranche {

    private static boolean debug = false;
    /**
     * <p></p>
     */
    public static final String CATEGORY_GENERAL = "[GENERAL]";
    /**
     * <p></p>
     */
    public static final String CATEGORY_CORE_SERVERS = "[CORE SERVERS]";
    /**
     * <p></p>
     */
    public static final String CATEGORY_CERTIFICATES = "[CERTIFICATES]";
    /**
     * <p></p>
     */
    public static final String CATEGORY_LOGGING = "[LOGGING]";
    /**
     * <p></p>
     */
    public static final String CATEGORY_SERVER = "[SERVER]";
    /**
     * <p>The name of this Tranche network.</p>
     */
    public static final String PROP_NAME = "name";
    /**
     * <p>The description of this Tranche network.</p>
     */
    public static final String PROP_DESCRIPTION = "description";
    /**
     * <p></p>
     */
    public static final String PROP_ADMIN_EMAIL_ACCOUNTS = "admin.emails";
    /**
     * <p>The email address for users to contact.</p>
     */
    public static final String PROP_CONTACT_EMAIL = "contact.email";
    /**
     * <p>The URL used to launch the Tranche Java Web Start application.</p>
     */
    public static final String PROP_LAUNCH_TRANCHE_URL = "launch.advanced.url";
    /**
     * <p></p>
     */
    public static final String PROP_HOME_URL = "home.url";
    /**
     * <p></p>
     */
    public static final String PROP_SIGN_UP_URL = "signup.url";
    /**
     * <p></p>
     */
    public static final String PROP_CONTACT_URL = "contact.url";
    /**
     * <p></p>
     */
    public static final String PROP_DOWNLOAD_TOOL_URL = "launch.download.url";
    /**
     * <p></p>
     */
    public static final String PROP_UPLOAD_TOOL_URL = "launch.upload.url";
    /**
     * <p></p>
     */
    public static final String PROP_PROJECT_CACHE_URL = "project.cache.url";
    /**
     * <p></p>
     */
    public static final String PROP_PROJECT_CACHE_HASH = "project.cache.hash";
    /**
     * <p></p>
     */
    public static final String PROP_DATA_URL = "project.url";
    /**
     * <p></p>
     */
    public static final String PROP_PUBLISH_PASSPHRASE_URL = "project.publish.url";
    /**
     * <p></p>
     */
    public static final String PROP_EMAIL_URL = "email.url";
    /**
     * <p></p>
     */
    public static final String DEFAULT_EMAIL_URL = "https://trancheproject.org/email/send.jsp";
    /**
     * <p></p>
     */
    public static final String PROP_USER_LOG_IN_URL = "user.login.url";
    /**
     * <p></p>
     */
    public static final String PROP_SERVERS_URL = "servers.url";
    /**
     * <p></p>
     */
    public static final String PROP_SERVER_CONFIG_ATTR_URL = "server.config.attributes.url";
    /**
     * <p></p>
     */
    public static final String PROP_SERVER_DIRECTORY = "server.directory";
    /**
     * <p></p>
     */
    public static final String DEFAULT_SERVER_DIRECTORY = "";
    /**
     * <p></p>
     */
    public static final String PROP_SERVER_TIMEOUT = "server.timeout";
    /**
     * <p></p>
     */
    public static final String DEFAULT_SERVER_TIMEOUT = "60000";
    /**
     * <p></p>
     */
    public static final String PROP_SERVER_KEEP_ALIVE_TIMEOUT = "server.keepalive.timeout";
    /**
     * <p></p>
     */
    public static final String DEFAULT_SERVER_KEEP_ALIVE_TIMEOUT = "300000";
    /**
     * <p></p>
     */
    public static final String PROP_SERVER_QUEUE_SIZE = "server.queue.size";
    /**
     * <p></p>
     */
    public static final String DEFAULT_SERVER_QUEUE_SIZE = "10";
    /**
     * <p>The number of requests a single user can have executed simultaneously on a server.</p>
     */
    public static final String PROP_SERVER_USER_SIMULTANEOUS_REQUESTS = "server.user.simultaneous.requests";
    /**
     * <p></p>
     */
    public static final String DEFAULT_SERVER_USER_SIMULTANEOUS_REQUESTS = "2";
    /**
     * <p>The number of requests a single server can have executed simultaneously on a server. Must be at >= 2.</p>
     */
    public static final String PROP_SERVER_SERVER_SIMULTANEOUS_REQUESTS = "server.server.simultaneous.requests";
    /**
     * <p></p>
     */
    public static final String DEFAULT_SERVER_SERVER_SIMULTANEOUS_REQUESTS = "10";
    /**
     * <p>The amount of time that may pass before reregistering the local server with connected servers in the server status update process.</p>
     */
    public static final String PROP_SERVER_TIME_BETWEEN_REGISTRATIONS = "server.server.registration.time";
    /**
     * <p></p>
     */
    public static final String DEFAULT_SERVER_TIME_BETWEEN_REGISTRATIONS = "3600000";
    /**
     * <p></p>
     */
    public static final String PROP_SERVER_PORT = "server.port";
    /**
     * <p></p>
     */
    public static final String DEFAULT_SERVER_PORT = "443";
    /**
     * <p></p>
     */
    public static final String PROP_SERVER_SSL = "server.ssl";
    /**
     * <p></p>
     */
    public static final String DEFAULT_SERVER_SSL = "false";
    /**
     * <p></p>
     */
    public static final String PROP_SOFTWARE_UPDATE_ZIP_URL = "update.software.zip.url";
    /**
     * <p></p>
     */
    public static final String PROP_SOFTWARE_UPDATE_SIGNATURE_URL = "update.software.signature.url";
    /**
     * <p>Where to submit error messages that users submit.</p>
     */
    public static final String PROP_LOG_ERROR_URL = "log.error.url";
    /**
     * <p></p>
     */
    public static final String PROP_LOG_SERVER_URL = "log.server.url";
    /**
     * <p></p>
     */
    public static final String PROP_LOG_UPLOAD_URL = "log.upload.url";
    /**
     * <p></p>
     */
    public static final String PROP_LOG_UPLOAD_FAILURE_URL = "log.upload.failure.url";
    /**
     * <p></p>
     */
    public static final String PROP_LOG_DOWNLOAD_URL = "log.download.url";
    /**
     * <p></p>
     */
    public static final String PROP_LOG_DOWNLOAD_FAILURE = "log.download.failure.url";
    /**
     * <p></p>
     */
    public static final String PROP_LOG_DOWNLOAD_FAILURE_EMAIL_RECIPIENTS = "log.download.failure.emails";
    /**
     * <p></p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_ADMIN = "security.certificate.admin";
    /**
     * <p></p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_WRITE = "security.certificate.write";
    /**
     * <p></p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_USER = "security.certificate.user";
    /**
     * <p></p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_READ = "security.certificate.read";
    /**
     * <p></p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_AUTOCERT = "security.certificate.autocert";
    /**
     * <p></p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_ANONYMOUS = "security.certificate.anon";
    /**
     * <p></p>
     */
    public static final String PROP_SECURITY_KEY_ANONYMOUS = "security.key.anon";
    /**
     * <p></p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_EMAIL = "security.certificate.email";
    /**
     * <p></p>
     */
    public static final String PROP_SECURITY_KEY_EMAIL = "security.key.email";
    /**
     * <p></p>
     */
    public static final String PROP_SECURITY_CERTS_URL = "security.certs.url";
    /**
     * <p></p>
     */
    public static final String PROP_STATUS_UPDATE_CLIENT_FREQUENCY = "status.update.client.frequency";
    /**
     * <p></p>
     */
    public static final String DEFAULT_STATUS_UPDATE_CLIENT_FREQUENCY = "60000";
    /**
     * <p></p>
     */
    public static final String PROP_STATUS_UPDATE_SERVER_FREQUENCY = "status.update.server.frequency";
    /**
     * <p></p>
     */
    public static final String DEFAULT_STATUS_UPDATE_SERVER_FREQUENCY = "30000";
    /**
     * <p></p>
     */
    public static final String PROP_STATUS_UPDATE_SERVER_GROUPING = "status.update.server.grouping";
    /**
     * <p></p>
     */
    public static final String DEFAULT_STATUS_UPDATE_SERVER_GROUPING = "5";
    /**
     * <p>The point at which only a single full hash span of servers are connected to.</p>
     */
    public static final String PROP_CONNECTION_FULL_HASH_SPAN_THRESHOLD = "connection.full.hashspan.threshold";
    /**
     * <p></p>
     */
    public static final String DEFAULT_CONNECTION_FULL_HASH_SPAN_THRESHOLD = "25";
    /**
     * <p></p>
     */
    public static final String PROP_KEEP_ALIVE_INTERVAL = "connection.keep.alive.interval";
    /**
     * <p></p>
     */
    public static final String DEFAULT_KEEP_ALIVE_INTERVAL = "8000";
    /**
     * <p>Servers that are offline and do not update within this time get removed from the table.</p>
     */
    public static final String PROP_DEFUNCT_SERVER_THRESHOLD = "status.defunct.threshold";
    /**
     * <p>Servers that are offline and do not update within this time get removed from the table.</p>
     */
    public static final String DEFAULT_DEFUNCT_SERVER_THRESHOLD = "604800000";
    /**
     * <p></p>
     */
    public static final String PROP_SERVER_OFFLINE_NOTIFICATION_INTERVAL = "server.offline.notification.interval";
    /**
     * <p></p>
     */
    public static final String DEFAULT_SERVER_OFFLINE_NOTIFICATION_INTERVAL = "86400000";
    private static final Properties properties = new Properties();
    private static Map<String, String> attributesCache = null;
    private static long lastAttributeCacheTimestampModulusValue = -1;
    private static final long attributesCacheTimestampModulus = 10000000;
    private static boolean loaded = false;


    static {
        set(PROP_EMAIL_URL, DEFAULT_EMAIL_URL);
        set(PROP_SERVER_DIRECTORY, DEFAULT_SERVER_DIRECTORY);
        set(PROP_SERVER_PORT, DEFAULT_SERVER_PORT);
        set(PROP_SERVER_SSL, DEFAULT_SERVER_SSL);
        set(PROP_SERVER_QUEUE_SIZE, DEFAULT_SERVER_QUEUE_SIZE);
        set(PROP_SERVER_USER_SIMULTANEOUS_REQUESTS, DEFAULT_SERVER_USER_SIMULTANEOUS_REQUESTS);
        set(PROP_SERVER_SERVER_SIMULTANEOUS_REQUESTS, DEFAULT_SERVER_SERVER_SIMULTANEOUS_REQUESTS);
        set(PROP_SERVER_TIMEOUT, DEFAULT_SERVER_TIMEOUT);
        set(PROP_SERVER_KEEP_ALIVE_TIMEOUT, DEFAULT_SERVER_KEEP_ALIVE_TIMEOUT);
        set(PROP_STATUS_UPDATE_CLIENT_FREQUENCY, DEFAULT_STATUS_UPDATE_CLIENT_FREQUENCY);
        set(PROP_STATUS_UPDATE_SERVER_FREQUENCY, DEFAULT_STATUS_UPDATE_SERVER_FREQUENCY);
        set(PROP_STATUS_UPDATE_SERVER_GROUPING, DEFAULT_STATUS_UPDATE_SERVER_GROUPING);
        set(PROP_CONNECTION_FULL_HASH_SPAN_THRESHOLD, DEFAULT_CONNECTION_FULL_HASH_SPAN_THRESHOLD);
        set(PROP_SERVER_TIME_BETWEEN_REGISTRATIONS, DEFAULT_SERVER_TIME_BETWEEN_REGISTRATIONS);
        set(PROP_KEEP_ALIVE_INTERVAL, DEFAULT_KEEP_ALIVE_INTERVAL);
        set(PROP_DEFUNCT_SERVER_THRESHOLD, DEFAULT_DEFUNCT_SERVER_THRESHOLD);
        set(PROP_SERVER_OFFLINE_NOTIFICATION_INTERVAL, DEFAULT_SERVER_OFFLINE_NOTIFICATION_INTERVAL);
    }

    /**
     * <p>Class cannot be instantiated.</p>
     */
    protected ConfigureTranche() {
    }

    /**
     * <p>Returns a string array of administrator email addresses.</p>
     * @return
     */
    public static final String[] getAdminEmailAccounts() {
        if (!get(PROP_ADMIN_EMAIL_ACCOUNTS).equals("")) {
            return get(PROP_ADMIN_EMAIL_ACCOUNTS).split(",");
        } else {
            return new String[0];
        }
    }

    /**
     * 
     * @param property
     * @return
     */
    public synchronized static final String get(String property) {
        return properties.getProperty(property, "");
    }

    /**
     * 
     * @param property
     * @return
     */
    public synchronized static final boolean getBoolean(String property) {
        try {
            return Boolean.valueOf(get(property));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 
     * @param property
     * @return
     */
    public synchronized static final int getInt(String property) {
        try {
            return Integer.valueOf(get(property));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     *
     * @param property
     * @return
     */
    public synchronized static final long getLong(String property) {
        try {
            return Long.valueOf(get(property));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 
     * @param property
     * @param value
     */
    public synchronized static final void set(String property, String value) {
        properties.setProperty(property, value.toString());
    }

    /**
     * 
     */
    public static void waitForStartup() {
        while (!loaded) {
            ThreadUtil.safeSleep(500);
        }
    }

    /**
     * <p>Safely loads the network configuration from the passed in arguments.</p>
     * <p>Takes only the first argument from the array and passes it to the ConfigureTranche.load(String) method.</p>
     * @param args
     */
    public static void load(String[] args) {
        try {
            load(args[0]);
        } catch (Exception e) {
            // likely no arguments given
        }
    }

    /**
     * <p>Takes the location to the configuration file that contains this Tranche network's configuration information.</p>
     * <p>The file can be in the JAR package, on the local file system, or on the Internet. The program will try to find the file in that same order and will use the first one it finds.</p>
     * @param configFile
     */
    public static void load(String configFile) {
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = openStreamToFile(configFile);
            String readingCategory = CATEGORY_GENERAL;
            Set<String> servers = new HashSet<String>();
            String line = null;
            while ((line = readLineIgnoreComments(is)) != null) {
                try {
                    if (line.startsWith("[")) {
                        if (line.equals(CATEGORY_GENERAL)) {
                            readingCategory = CATEGORY_GENERAL;
                        } else if (line.equals(CATEGORY_CORE_SERVERS)) {
                            readingCategory = CATEGORY_CORE_SERVERS;
                        } else if (line.equals(CATEGORY_CERTIFICATES)) {
                            readingCategory = CATEGORY_CERTIFICATES;
                        } else if (line.equals(CATEGORY_LOGGING)) {
                            readingCategory = CATEGORY_LOGGING;
                        } else if (line.equals(CATEGORY_SERVER)) {
                            readingCategory = CATEGORY_SERVER;
                        } else {
                            readingCategory = null;
                        }
                    } else if (readingCategory == null) {
                        continue;
                    } else if (readingCategory.equals(CATEGORY_GENERAL) || readingCategory.equals(CATEGORY_LOGGING) || readingCategory.equals(CATEGORY_SERVER)) {
                        String name = line.substring(0, line.indexOf('=')).trim().toLowerCase();
                        String value = line.substring(line.indexOf('=') + 1).trim();
                        set(name, value);
                    } else if (readingCategory.equals(CATEGORY_CORE_SERVERS)) {
                        servers.add(line.trim());
                    } else if (readingCategory.equals(CATEGORY_CERTIFICATES)) {
                        String name = line.substring(0, line.indexOf('=')).trim().toLowerCase();
                        String value = line.substring(line.indexOf('=') + 1).trim();
                        if (name.equals(PROP_SECURITY_CERTIFICATE_ADMIN)) {
                            SecurityUtil.setAdminCertLocation(value);
                        } else if (name.equals(PROP_SECURITY_CERTIFICATE_WRITE)) {
                            SecurityUtil.setWriteCertLocation(value);
                        } else if (name.equals(PROP_SECURITY_CERTIFICATE_USER)) {
                            SecurityUtil.setUserCertLocation(value);
                        } else if (name.equals(PROP_SECURITY_CERTIFICATE_READ)) {
                            SecurityUtil.setReadCertLocation(value);
                        } else if (name.equals(PROP_SECURITY_CERTIFICATE_AUTOCERT)) {
                            SecurityUtil.setAutocertCertLocation(value);
                        } else if (name.equals(PROP_SECURITY_CERTIFICATE_ANONYMOUS)) {
                            SecurityUtil.setAnonCertLocation(value);
                        } else if (name.equals(PROP_SECURITY_KEY_ANONYMOUS)) {
                            SecurityUtil.setAnonKeyLocation(value);
                        } else if (name.equals(PROP_SECURITY_CERTIFICATE_EMAIL)) {
                            SecurityUtil.setEmailCertLocation(value);
                        } else if (name.equals(PROP_SECURITY_KEY_EMAIL)) {
                            SecurityUtil.setEmailKeyLocation(value);
                        } else {
                            set(name, value);
                        }
                    }
                } catch (Exception e) {
                    debugErr(e);
                }
            }

            // If retrieve anything from URL, use that; otherwise, use whatever comes from configuration
            Set<String> serversFromURL = updateServers();
            if (serversFromURL.size() > 0) {
                NetworkUtil.setStartupServerURLs(serversFromURL);
            } else {
                NetworkUtil.setStartupServerURLs(servers);
            }

            // If URL, overwrite with certificates dynamically (if no URL, returns immediately)
            updateCertificates();
        } catch (Exception e) {
            debugErr(e);
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(is);
        }
        loaded = true;
    }

    /**
     * <p>Helper method to get only important configuration information by skipping comments and blank lines.</p>
     * @param is
     * @return
     */
    public static final String readLineIgnoreComments(InputStream is) throws IOException, NullPointerException {
        while (is.available() > 0) {
            String line = RemoteUtil.readLine(is);
            if (line != null) {
                line = line.trim();
            }

            // ignore comments and blanks
            if (line != null && (line.startsWith("#") || line.startsWith("//") || line.equals(""))) {
                continue;
            }

            // If is null, blank line. Note this wan't a problem
            // in Windows, which used \r\n. In Unix, \n
            // returned null based on RemoteUtil.readLine's logic
            // as of Sept. 19 2008  -- Bryan
            if (line == null) {
                continue;
            }

            return line;
        }
        return null;
    }

    /**
     * <p>Opens an input stream to a file with a nebulous location.</p>
     * <p>First tries a Java package, then tries on the file system, then tries the Internet.</p>
     * @param file
     * @return
     * @throws java.io.IOException
     */
    public static final InputStream openStreamToFile(String file) throws IOException {
        InputStream is = null;

        // is it in a local package?
        try {
            is = ConfigureTranche.class.getResourceAsStream(file);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        // is it a file on the local file system?
        if (is == null) {
            try {
                is = new File(file).toURL().openStream();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

        // is it a file on the Internet?
        if (is == null) {
            is = new URL(file).openStream();
        }

        return is;
    }

    /**
     * <p>Returns network-wide core servers from URL.</p>
     * @return
     */
    public static final Set<String> updateServers() {
        String updateURL = get(ConfigureTranche.PROP_SERVERS_URL);

        Set<String> urls = new HashSet<String>();

        // return blank set when no URL from which to update
        if (updateURL == null || updateURL.equals("")) {
            return urls;
        }

        // get the newest list of core servers
        try {
            // make a new client
            HttpClient c = new HttpClient();

            // make a post method
            PostMethod pm = new PostMethod(updateURL);

            // best effort, do not print anything
            c.executeMethod(pm);

            if (pm.getStatusCode() == 200) {
                BufferedReader br = null;
                InputStreamReader isr = null;
                try {
                    isr = new InputStreamReader(pm.getResponseBodyAsStream());
                    br = new BufferedReader(isr);
                    String line;
                    while ((line = br.readLine()) != null) {
                        urls.add(line.trim());
                    }
                } finally {
                    IOUtil.safeClose(br);
                    IOUtil.safeClose(isr);
                }
            }
        } catch (Exception e) {
            debugErr(e);
        }

        return urls;
    }

    /**
     * <p>Returns network-wide server attributes from URL.</p>
     * @return
     */
    public static Map<String, String> getServerConfigurationAttributes() {
        // all servers should update their configuration attributes at the same time
        long modulus = TimeUtil.getTrancheTimestamp() % attributesCacheTimestampModulus;
        if (lastAttributeCacheTimestampModulusValue == -1 || modulus < lastAttributeCacheTimestampModulusValue) {
            attributesCache = updateServerConfigurationAttributes();
        }
        lastAttributeCacheTimestampModulusValue = modulus;
        return attributesCache;
    }

    /**
     * <p>Internal method that retrieves attributes from URL.</p>
     * @return
     */
    private static Map<String, String> updateServerConfigurationAttributes() {
        debugOut("Updating server configuration attributes.");
        String updateURL = get(PROP_SERVER_CONFIG_ATTR_URL);

        Map<String, String> attributes = new HashMap();

        // return blank when no URL from which to update
        if (updateURL == null || updateURL.equals("")) {
            return attributes;
        }

        try {
            // make a new client
            HttpClient c = new HttpClient();

            // make a post method
            PostMethod pm = new PostMethod(updateURL);

            // best effort, do not print anything
            c.executeMethod(pm);

            if (pm.getStatusCode() == 200) {
                BufferedReader br = null;
                InputStreamReader isr = null;
                try {
                    isr = new InputStreamReader(pm.getResponseBodyAsStream());
                    br = new BufferedReader(isr);
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = ExcelSavvyLineSplitter.split(line.trim());
                        if (parts.length != 2) {
                            continue;
                        }
                        attributes.put(parts[0], parts[1]);
                    }
                } finally {
                    IOUtil.safeClose(br);
                    IOUtil.safeClose(isr);
                }
            }
        } catch (Exception e) {
            debugErr(e);
        }

        return attributes;
    }

    /**
     * <p>Loads network-wide certificates (and any relevant anoymous keys) from URL.</p>
     * @return
     */
    private static void updateCertificates() {
        String updateURL = get(PROP_SECURITY_CERTS_URL);

        // return if no URL from which to update
        if (updateURL == null || updateURL.equals("")) {
            return;
        }

        try {
            // make a new client
            HttpClient c = new HttpClient();

            // make a post method
            PostMethod pm = new PostMethod(updateURL);

            // best effort, do not print anything
            c.executeMethod(pm);

            if (pm.getStatusCode() == 200) {
                InputStream is = null;
                try {
                    is = pm.getResponseBodyAsStream();

                    final int totalToRead = RemoteUtil.readInt(is);

                    for (int i = 0; i < totalToRead; i++) {
                        int certBytesToRead = RemoteUtil.readInt(is);

                        byte[] certBytes = new byte[certBytesToRead];
                        int read = 0;
                        while (read < certBytes.length) {
                            int len = certBytes.length - read;
                            read += is.read(certBytes, read, len);
                        }

                        X509Certificate cert = SecurityUtil.getCertificate(certBytes);
                        PrivateKey key = null;

                        int flags = RemoteUtil.readInt(is);

                        byte isKey = (byte) is.read();

                        if (isKey == 0) {
                            // Nothing to do
                        } else if (isKey == 1) {
                            int keyBytesToRead = RemoteUtil.readInt(is);

                            byte[] keyBytes = new byte[keyBytesToRead];

                            read = 0;
                            while (read < keyBytes.length) {
                                int len = keyBytes.length - read;
                                read += is.read(keyBytes, read, len);
                            }

                            key = SecurityUtil.getPrivateKey(keyBytes);
                        } else {
                            throw new AssertionFailedException("Value for isKey should be 0 (false) or 1 (true), instead found: " + isKey);
                        }

                        String subjectName = cert.getSubjectX500Principal().getName();
                        String certName = null;

                        if (subjectName != null) {
                            // Break up into components
                            String[] tokens = subjectName.split(",");
                            for (String token : tokens) {
                                String[] pair = token.split("=");
                                if (pair.length != 2) {
                                    continue;
                                }
                                if (pair[0].equals("CN")) {
                                    certName = pair[1];
                                    break;
                                }
                            }
                        }

                        if (certName == null) {
                            System.err.println("WARNING: Certificate doesn't have a subject name for some warning. Will attempt to use, but should notify developers.");
                        }

                        // Admin
                        if (flags == User.ALL_PRIVILEGES) {
                            if (key != null) {
                                throw new AssertionFailedException("Should not have key for user: admin");
                            }
                            SecurityUtil.setAdminCert(cert);
                        } // User
                        else if (flags == SecurityUtil.getUserFlags()) {
                            if (key != null) {
                                throw new AssertionFailedException("Should not have key for user: user");
                            }
                            SecurityUtil.setUserCert(cert);
                        } // Read-only
                        else if (flags == SecurityUtil.getReadOnlyFlags()) {
                            if (key != null) {
                                throw new AssertionFailedException("Should not have key for user: read-only");
                            }
                            SecurityUtil.setReadCert(cert);
                        } // Anon
                        else if (flags == SecurityUtil.getAnonymousFlags()) {
                            if (key == null) {
                                throw new AssertionFailedException("Should have key for user: anonymous");
                            }
                            SecurityUtil.setAnonCert(cert);
                            SecurityUtil.setAnonKey(key);
                        } // Write-only: same flags as auto-cert, so check name
                        else if (flags == SecurityUtil.getWriteOnlyFlags() && certName != null && certName.toLowerCase().contains("write-only")) {
                            if (key != null) {
                                throw new AssertionFailedException("Should not have key for user: write-only");
                            }
                            SecurityUtil.setWriteCert(cert);
                        } // Auto-cert: same flags as write-only, so check name
                        else if (flags == SecurityUtil.getAutoCertFlags() && certName != null && certName.toLowerCase().contains("auto")) {
                            if (key != null) {
                                throw new AssertionFailedException("Should not have key for user: auto-cert");
                            }
                            SecurityUtil.setAutoCertCert(cert);
                        } else {
                            throw new AssertionFailedException("Could not find flags: " + flags);
                        }
                    }

                } finally {
                    IOUtil.safeClose(is);
                }
            }
        } catch (Exception e) {
            debugErr(e);
        }
    }

    /**
     * <p>Sets the flag for whether the output and error information should be written.</p>
     * @param debug The flag for whether the output and error information should be written.</p>
     */
    public static void setDebug(boolean debug) {
        ConfigureTranche.debug = debug;
    }

    /**
     * <p>Returns whether the output and error information is being written.</p>
     * @return Whether the output and error information is being written.
     */
    public static boolean isDebug() {
        return debug;
    }

    /**
     *
     * @param line
     */
    private static void debugOut(String line) {
        if (debug) {
            DebugUtil.printOut(ConfigureTranche.class.getName() + "> " + line);
        }
    }

    /**
     *
     * @param line
     */
    private static void debugErr(Exception e) {
        if (debug) {
            DebugUtil.reportException(e);
        }
    }
}
