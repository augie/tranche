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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.tranche.network.NetworkUtil;
import org.tranche.remote.RemoteUtil;
import org.tranche.security.EasySSLProtocolSocketFactory;
import org.tranche.util.DebugUtil;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.tasks.ExcelSavvyLineSplitter;
import org.tranche.time.TimeUtil;
import org.tranche.util.TestUtil;

/**
 * <p>This is here to load a Tranche network's configuration information, including servers and certificates.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ConfigureTranche {

    private static boolean debug = false;
    /**
     * <p>The general category.</p>
     */
    public static final String CATEGORY_GENERAL = "[GENERAL]";
    /**
     * <p>The core servers category.</p>
     */
    public static final String CATEGORY_CORE_SERVERS = "[CORE SERVERS]";
    /**
     * <p>The banned servers category.</p>
     */
    public static final String CATEGORY_BANNED_SERVERS = "[BANNED SERVERS]";
    /**
     * <p>The certificates category.</p>
     */
    public static final String CATEGORY_CERTIFICATES = "[CERTIFICATES]";
    /**
     * <p>The logging category.</p>
     */
    public static final String CATEGORY_LOGGING = "[LOGGING]";
    /**
     * <p>The server category.</p>
     */
    public static final String CATEGORY_SERVER = "[SERVER]";
    /**
     * <p>The network time servers category.</p>
     */
    public static final String CATEGORY_NETWORK_TIME_SERVERS = "[NETWORK TIME SERVERS]";
    /**
     * <p>The name of the repository.</p>
     */
    public static final String PROP_NAME = "name";
    /**
     * <p>The description of the repository.</p>
     */
    public static final String PROP_DESCRIPTION = "description";
    /**
     * <p>A comma-separated list of email addresses to which email notifications will be sent.</p>
     */
    public static final String PROP_ADMIN_EMAIL_ACCOUNTS = "admin.emails";
    /**
     * <p>The email address for users to contact.</p>
     */
    public static final String PROP_CONTACT_EMAIL = "contact.email";
    /**
     * <p>The number of replications required for each chunk of data.</p>
     */
    public static final String PROP_REPLICATIONS = "replications";
    /**
     * <p>The default value of the number of replications required for each chunk of data.</p>
     */
    public static final String DEFAULT_REPLICATIONS = "3";
    /**
     * <p>The URL for the repository web site.</p>
     */
    public static final String PROP_HOME_URL = "home.url";
    /**
     * <p>The URL for users to sign up for the repository.</p>
     */
    public static final String PROP_SIGN_UP_URL = "signup.url";
    /**
     * <p>The URL where users can contact the administrators.</p>
     */
    public static final String PROP_CONTACT_URL = "contact.url";
    /**
     * <p>The URL used to launch the Tranche Java Web Start application.</p>
     */
    public static final String PROP_LAUNCH_TRANCHE_URL = "launch.advanced.url";
    /**
     * <p>The URL used to launch the Tranche Downloader Java Web Start application.</p>
     */
    public static final String PROP_DOWNLOAD_TOOL_URL = "launch.download.url";
    /**
     * <p>The URL used to launch the Tranche Uploader Java Web Start application.</p>
     */
    public static final String PROP_UPLOAD_TOOL_URL = "launch.upload.url";
    /**
     * <p>The URL of the project cache.</p>
     */
    public static final String PROP_PROJECT_CACHE_URL = "project.cache.url";
    /**
     * <p>The Tranche hash of the project cache. The project cache URL will be tried first.</p>
     */
    public static final String PROP_PROJECT_CACHE_HASH = "project.cache.hash";
    /**
     * <p>The URL of the data set's descriptive information.</p>
     */
    public static final String PROP_DATA_URL = "project.url";
    /**
     * <p>The URL where a user can go to publish their data sets.</p>
     */
    public static final String PROP_PUBLISH_PASSPHRASE_URL = "project.publish.url";
    /**
     * <p>The URL to HTTP POST email requests.</p>
     */
    public static final String PROP_EMAIL_URL = "email.url";
    /**
     * <p>The default URL to HTTP POST email requests.</p>
     */
    public static final String DEFAULT_EMAIL_URL = "https://trancheproject.org/email/send.jsp";
    /**
     * <p>The URL where users can log in to get their valid user zip files.</p>
     */
    public static final String PROP_USER_LOG_IN_URL = "user.login.url";
    /**
     * <p>The number of milliseconds between checking if the system time has been changed significantly.</p>
     */
    public static final String PROP_TIME_CHANGE_CHECK_INTERVAL = "time.change.check.interval";
    /**
     * <p>The default number of milliseconds between checking if the system time has been changed significantly.</p>
     */
    public static final String DEFAULT_TIME_CHANGE_CHECK_INTERVAL = "10000";
    /**
     * <p>The number of milliseconds the actual timestamp can deviate from the expected timestamp before resetting the network time.</p>
     */
    public static final String PROP_TIME_CHANGE_CHECK_DEVIATION = "time.change.check.deviation";
    /**
     * <p>The default number of milliseconds the actual timestamp can deviate from the expected timestamp before resetting the network time.</p>
     */
    public static final String DEFAULT_TIME_CHANGE_CHECK_DEVIATION = "1000";
    /**
     * <p>The number of milliseconds between forcing a reset of the network time.</p>
     */
    public static final String PROP_TIME_UPDATE_INTERVAL = "time.update.interval";
    /**
     * <p>The default number of milliseconds between forcing a reset of the network time.</p>
     */
    public static final String DEFAULT_TIME_UPDATE_INTERVAL = "21600000";
    /**
     * <p>The timeout per NT server request.</p>
     */
    public static final String PROP_TIME_UPDATE_TIMEOUT = "time.update.timeout";
    /**
     * <p>The default timeout per NT server request.</p>
     */
    public static final String DEFAULT_TIME_UPDATE_TIMEOUT = "10000";
    /**
     * <p>The number of milliseconds between updating the network status table on a client.</p>
     */
    public static final String PROP_STATUS_UPDATE_CLIENT_FREQUENCY = "status.update.client.frequency";
    /**
     * <p>The default number of milliseconds between updating the network status table on a client.</p>
     */
    public static final String DEFAULT_STATUS_UPDATE_CLIENT_FREQUENCY = "60000";
    /**
     * <p>The number of milliseconds between updating the network status table on a server.</p>
     */
    public static final String PROP_STATUS_UPDATE_SERVER_FREQUENCY = "status.update.server.frequency";
    /**
     * <p>The default number of milliseconds between updating the network status table on a server.</p>
     */
    public static final String DEFAULT_STATUS_UPDATE_SERVER_FREQUENCY = "30000";
    /**
     * <p>The number of servers in a network status table group. Used in server status propagation.</p>
     */
    public static final String PROP_STATUS_UPDATE_SERVER_GROUPING = "status.update.server.grouping";
    /**
     * <p>The default number of servers in a network status table group.</p>
     */
    public static final String DEFAULT_STATUS_UPDATE_SERVER_GROUPING = "5";
    /**
     * <p>The number of servers at which only a single full hash span of servers are connected to.</p>
     */
    public static final String PROP_CONNECTION_FULL_HASH_SPAN_THRESHOLD = "connection.full.hashspan.threshold";
    /**
     * <p>The default number of servers at which only a single full hash span of servers are connected to.</p>
     */
    public static final String DEFAULT_CONNECTION_FULL_HASH_SPAN_THRESHOLD = "25";
    /**
     * <p>The number of milliseconds between sending a ping to each connected server to keep the socket alive.</p>
     */
    public static final String PROP_KEEP_ALIVE_INTERVAL = "connection.keep.alive.interval";
    /**
     * <p>The default number of milliseconds between sending a ping to each connected server to keep the socket alive.</p>
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
     * <p>The URL where an updated configuration file can be found. The update will occur at regular intervals at about the same time on all clients and servers. If the location for certificate files has changed, they will be reloaded from the new location.</p>
     */
    public static final String PROP_UPDATE_CONFIG_URL = "update.conf.url";
    /**
     * <p>The number of milliseconds between updating the configuration.</p>
     */
    public static final String PROP_UPDATE_CONFIG_INTERVAL = "update.conf.interval";
    /**
     * <p>The default number of milliseconds between updating the configuration.</p>
     */
    public static final String DEFAULT_UPDATE_CONFIG_INTERVAL = "10000000";
    /**
     * <p>The URL to which the software will submit error logs.</p>
     */
    public static final String PROP_LOG_ERROR_URL = "log.error.url";
    /**
     * <p>The URL to which the software will submit server logs.</p>
     */
    public static final String PROP_LOG_SERVER_URL = "log.server.url";
    /**
     * <p>The URL to which the software will register uploads.</p>
     */
    public static final String PROP_LOG_UPLOAD_URL = "log.upload.url";
    /**
     * <p>The URL to which the software will register upload failures.</p>
     */
    public static final String PROP_LOG_UPLOAD_FAILURE_URL = "log.upload.failure.url";
    /**
     * <p>The URL to which the software will register downloads.</p>
     */
    public static final String PROP_LOG_DOWNLOAD_URL = "log.download.url";
    /**
     * <p>The URL to which the software will register failed downloads.</p>
     */
    public static final String PROP_LOG_DOWNLOAD_FAILURE = "log.download.failure.url";
    /**
     * <p>The location of the public certificate for the administrator (all priveleges).</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_ADMIN = "security.certificate.admin";
    /**
     * <p>The location of the public certificate for the write-only user.</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_WRITE = "security.certificate.write";
    /**
     * <p>The location of the public certificate for the standard user (read, write, delete).</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_USER = "security.certificate.user";
    /**
     * <p>The location of the public certificate for the read-only user.</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_READ = "security.certificate.read";
    /**
     * <p>The location of the public certificate for the auto-certificate user (read, write).</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_AUTOCERT = "security.certificate.autocert";
    /**
     * <p>The location of the public certificate for the anonymous user.</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_ANONYMOUS = "security.certificate.anon";
    /**
     * <p>The location of the private key for the anonymous user.</p>
     */
    public static final String PROP_SECURITY_KEY_ANONYMOUS = "security.key.anon";
    /**
     * <p>The location of the public certificate for the email user.</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_EMAIL = "security.certificate.email";
    /**
     * <p>The location of the private key for the email user.</p>
     */
    public static final String PROP_SECURITY_KEY_EMAIL = "security.key.email";
    /**
     * <p>The starting location of the directory that contains all the server configuration and runtime files.</p>
     */
    public static final String PROP_SERVER_DIRECTORY = "server.directory";
    /**
     * <p>The default location of the directory that contains all the server configuration and runtime files.</p>
     */
    public static final String DEFAULT_SERVER_DIRECTORY = "./";
    /**
     * <p>The starting port number for a server.</p>
     */
    public static final String PROP_SERVER_PORT = "server.port";
    /**
     * <p>The default starting port number for a server.</p>
     */
    public static final String DEFAULT_SERVER_PORT = "443";
    /**
     * <p>The starting boolean for whether a server should communicate over SSL.</p>
     */
    public static final String PROP_SERVER_SSL = "server.ssl";
    /**
     * <p>The default starting boolean for whether a server should communicate over SSL.</p>
     */
    public static final String DEFAULT_SERVER_SSL = "false";
    /**
     * <p>The maximum number of requests a client can have outstanding on a server.</p>
     */
    public static final String PROP_SERVER_QUEUE_SIZE = "server.queue.size";
    /**
     * <p>The default maximum number of requests a client can have outstanding on a server.</p>
     */
    public static final String DEFAULT_SERVER_QUEUE_SIZE = "10";
    /**
     * <p>The maximum number of milliseconds before a request to a server times out if it doesn't send back a feep-alive signal.</p>
     */
    public static final String PROP_SERVER_TIMEOUT = "server.timeout";
    /**
     * <p>The default maximum number of milliseconds before a request to a server times out if it doesn't send back a feep-alive signal.</p>
     */
    public static final String DEFAULT_SERVER_TIMEOUT = "60000";
    /**
     * <p>The maximum number of milliseconds before a request that is being kept alive is timed out.</p>
     */
    public static final String PROP_SERVER_KEEP_ALIVE_TIMEOUT = "server.keepalive.timeout";
    /**
     * <p>The default maximum number of milliseconds before a request that is being kept alive is timed out.</p>
     */
    public static final String DEFAULT_SERVER_KEEP_ALIVE_TIMEOUT = "120000";
    /**
     * <p>The URL of the updated server configuration attributes.</p>
     */
    public static final String PROP_SERVER_CONFIG_ATTR_URL = "server.config.attributes.url";
    /**
     * <p>The amount of time that may pass before reregistering the local server with connected servers in the server status update process.</p>
     */
    public static final String PROP_SERVER_TIME_BETWEEN_REGISTRATIONS = "server.server.registration.time";
    /**
     * <p>The default amount of time that may pass before reregistering the local server with connected servers in the server status update process.</p>
     */
    public static final String DEFAULT_SERVER_TIME_BETWEEN_REGISTRATIONS = "3600000";
    /**
     * <p>The number of requests a single user can have executed simultaneously on a server.</p>
     */
    public static final String PROP_SERVER_USER_SIMULTANEOUS_REQUESTS = "server.user.simultaneous.requests";
    /**
     * <p>The default number of requests a single user can have executed simultaneously on a server.</p>
     */
    public static final String DEFAULT_SERVER_USER_SIMULTANEOUS_REQUESTS = "3";
    /**
     * <p>The number of requests a single server can have executed simultaneously on a server.</p>
     */
    public static final String PROP_SERVER_SERVER_SIMULTANEOUS_REQUESTS = "server.server.simultaneous.requests";
    /**
     * <p>The default number of requests a single server can have executed simultaneously on a server.</p>
     */
    public static final String DEFAULT_SERVER_SERVER_SIMULTANEOUS_REQUESTS = "10";
    /**
     * <p>The number of milliseconds between sending offline server notification emails.</p>
     */
    public static final String PROP_SERVER_OFFLINE_NOTIFICATION_INTERVAL = "server.offline.notification.interval";
    /**
     * <p>The default number of milliseconds between sending offline server notification emails.</p>
     */
    public static final String DEFAULT_SERVER_OFFLINE_NOTIFICATION_INTERVAL = "86400000";
    private static final Properties properties = new Properties(), defaultProperties = new Properties();
    private static Map<String, String> attributesCache = null;
    private static final List<String> networkTimeServers = new LinkedList<String>();
    private static long lastAttributeCacheTimestampModulusValue = -1;
    private static final long attributesCacheTimestampModulus = 10000000;
    private static boolean loaded = false, updated = false, defaultNetworkTimeServersLoaded = false;

    static {
        // load the HTTPS protocol just once on startup
        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
        reset();
        setDefault(PROP_EMAIL_URL, DEFAULT_EMAIL_URL);
        setDefault(PROP_SERVER_DIRECTORY, DEFAULT_SERVER_DIRECTORY);
        setDefault(PROP_SERVER_PORT, DEFAULT_SERVER_PORT);
        setDefault(PROP_SERVER_SSL, DEFAULT_SERVER_SSL);
        setDefault(PROP_SERVER_QUEUE_SIZE, DEFAULT_SERVER_QUEUE_SIZE);
        setDefault(PROP_SERVER_USER_SIMULTANEOUS_REQUESTS, DEFAULT_SERVER_USER_SIMULTANEOUS_REQUESTS);
        setDefault(PROP_SERVER_SERVER_SIMULTANEOUS_REQUESTS, DEFAULT_SERVER_SERVER_SIMULTANEOUS_REQUESTS);
        setDefault(PROP_SERVER_TIMEOUT, DEFAULT_SERVER_TIMEOUT);
        setDefault(PROP_SERVER_KEEP_ALIVE_TIMEOUT, DEFAULT_SERVER_KEEP_ALIVE_TIMEOUT);
        setDefault(PROP_STATUS_UPDATE_CLIENT_FREQUENCY, DEFAULT_STATUS_UPDATE_CLIENT_FREQUENCY);
        setDefault(PROP_STATUS_UPDATE_SERVER_FREQUENCY, DEFAULT_STATUS_UPDATE_SERVER_FREQUENCY);
        setDefault(PROP_STATUS_UPDATE_SERVER_GROUPING, DEFAULT_STATUS_UPDATE_SERVER_GROUPING);
        setDefault(PROP_CONNECTION_FULL_HASH_SPAN_THRESHOLD, DEFAULT_CONNECTION_FULL_HASH_SPAN_THRESHOLD);
        setDefault(PROP_SERVER_TIME_BETWEEN_REGISTRATIONS, DEFAULT_SERVER_TIME_BETWEEN_REGISTRATIONS);
        setDefault(PROP_KEEP_ALIVE_INTERVAL, DEFAULT_KEEP_ALIVE_INTERVAL);
        setDefault(PROP_DEFUNCT_SERVER_THRESHOLD, DEFAULT_DEFUNCT_SERVER_THRESHOLD);
        setDefault(PROP_SERVER_OFFLINE_NOTIFICATION_INTERVAL, DEFAULT_SERVER_OFFLINE_NOTIFICATION_INTERVAL);
        setDefault(PROP_REPLICATIONS, DEFAULT_REPLICATIONS);
        setDefault(PROP_UPDATE_CONFIG_INTERVAL, DEFAULT_UPDATE_CONFIG_INTERVAL);
        setDefault(PROP_TIME_CHANGE_CHECK_INTERVAL, DEFAULT_TIME_CHANGE_CHECK_INTERVAL);
        setDefault(PROP_TIME_CHANGE_CHECK_DEVIATION, DEFAULT_TIME_CHANGE_CHECK_DEVIATION);
        setDefault(PROP_TIME_UPDATE_INTERVAL, DEFAULT_TIME_UPDATE_INTERVAL);
        setDefault(PROP_TIME_UPDATE_TIMEOUT, DEFAULT_TIME_UPDATE_TIMEOUT);
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
        String accounts = get(PROP_ADMIN_EMAIL_ACCOUNTS);
        if (accounts != null && !accounts.equals("")) {
            return accounts.split(",");
        } else {
            return new String[0];
        }
    }

    /**
     * 
     * @param property
     * @return
     */
    public static final boolean getBoolean(String property) {
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
    public static final int getInt(String property) {
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
    public static final long getLong(String property) {
        try {
            return Long.valueOf(get(property));
        } catch (Exception e) {
            return 0;
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
     * @param value
     */
    public synchronized static final void set(String property, String value) {
        properties.setProperty(property, value.toString());
    }

    /**
     *
     * @param property
     */
    public synchronized static final void getDefault(String property) {
        defaultProperties.getProperty(property, "");
    }

    /**
     * 
     * @param property
     * @param value
     */
    protected synchronized static final void setDefault(String property, String value) {
        defaultProperties.setProperty(property, value);
    }

    /**
     * 
     */
    private synchronized static final void reset() {
        debugOut("Clearing all properties.");
        properties.clear();
        debugOut("Setting default properties.");
        properties.putAll(defaultProperties);
    }

    /**
     * 
     */
    public synchronized static void waitForStartup() {
        while (!loaded) {
            try {
                ConfigureTranche.class.wait();
            } catch (Exception e) {
                debugErr(e);
            }
        }
    }

    /**
     * <p>Safely loads the network configuration from the passed in arguments.</p>
     * <p>Takes only the first argument from the array and passes it to the ConfigureTranche.load(String) method.</p>
     * @param args
     * @throws IOException
     */
    public static void load(String[] args) throws IOException {
        load(args[0]);
    }

    /**
     * <p>Takes the location to the configuration file that contains this Tranche network's configuration information.</p>
     * <p>The file can be in the JAR package, on the local file system, or on the Internet. The program will try to find the file in that same order and will use the first one it finds.</p>
     * @param configFile
     * @throws IOException
     */
    public static void load(String configFile) throws IOException {
        InputStream is = null;
        try {
            is = openStreamToFile(configFile);
            load(is);
        } catch (Exception e) {
            throw new IOException("Could not open repository configuration file (" + configFile + "). Configuration file location must be the first argument and can be in the JVM classpath, on the local file system, or on the Internet.");
        } finally {
            IOUtil.safeClose(is);
        }
    }

    /**
     * Takes the input stream of this Tranche network's configuration information.
     * @param configFileStream
     */
    public synchronized static void load(InputStream configFileStream) {
        try {
            reset();

            debugOut("Loading configuration");
            String readingCategory = CATEGORY_GENERAL;
            Set<String> servers = new HashSet<String>(), bannedServers = new HashSet<String>(), timeServers = new HashSet<String>();
            String line = null;
            while ((line = readLineIgnoreComments(configFileStream)) != null) {
                try {
                    debugOut("> " + line);
                    if (line.startsWith("[")) {
                        if (line.equals(CATEGORY_GENERAL)) {
                            readingCategory = CATEGORY_GENERAL;
                        } else if (line.equals(CATEGORY_CORE_SERVERS)) {
                            readingCategory = CATEGORY_CORE_SERVERS;
                        } else if (line.equals(CATEGORY_BANNED_SERVERS)) {
                            readingCategory = CATEGORY_BANNED_SERVERS;
                        } else if (line.equals(CATEGORY_CERTIFICATES)) {
                            readingCategory = CATEGORY_CERTIFICATES;
                        } else if (line.equals(CATEGORY_LOGGING)) {
                            readingCategory = CATEGORY_LOGGING;
                        } else if (line.equals(CATEGORY_SERVER)) {
                            readingCategory = CATEGORY_SERVER;
                        } else if (line.equals(CATEGORY_NETWORK_TIME_SERVERS)) {
                            readingCategory = CATEGORY_NETWORK_TIME_SERVERS;
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
                    } else if (readingCategory.equals(CATEGORY_BANNED_SERVERS)) {
                        bannedServers.add(line.trim());
                    } else if (readingCategory.equals(CATEGORY_NETWORK_TIME_SERVERS)) {
                        timeServers.add(line.trim());
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
            NetworkUtil.setStartupServerURLs(servers);
            NetworkUtil.setBannedServerHosts(bannedServers);
            synchronized (networkTimeServers) {
                networkTimeServers.clear();
                networkTimeServers.addAll(timeServers);
                defaultNetworkTimeServersLoaded = false;
            }
            debugOut("Done reading configuration.");

            if (!updated) {
                updated = true;
                update();
            }
        } catch (Exception e) {
            debugErr(e);
        }
        loaded = true;
        ConfigureTranche.class.notifyAll();
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
     * <p>Opens an input stream to a file. First tries a Java package, then tries on the file system, then tries the Internet.</p>
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
            debugErr(e);
        }

        // is it a file on the local file system?
        if (is == null) {
            try {
                is = new File(file).toURL().openStream();
            } catch (Exception e) {
                debugErr(e);
            }
        }

        // is it a file on the Internet?
        if (is == null) {
            try {
                is = new URL(file).openStream();
            } catch (Exception e) {
                debugErr(e);
            }
        }

        if (is == null) {
            throw new IOException("Could not open file: " + file);
        }

        return is;
    }

    /**
     *
     */
    public static void update() {
        String url = get(PROP_UPDATE_CONFIG_URL);
        if (url != null && !url.equals("")) {
            debugOut("Updating configuration from " + url);
            try {
                // make a new client
                HttpClient c = new HttpClient();
                // make a post method
                GetMethod gm = new GetMethod(url);
                // best effort, do not print anything
                c.executeMethod(gm);
                if (gm.getStatusCode() == 200) {
                    debugOut("Successfully obtained new configuration from " + url);
                    InputStream configFileStream = null;
                    try {
                        configFileStream = new ByteArrayInputStream(gm.getResponseBody());
                        ConfigureTranche.load(configFileStream);
                    } finally {
                        IOUtil.safeClose(configFileStream);
                    }
                }
            } catch (Exception e) {
                debugErr(e);
            }
        }
    }

    /**
     * 
     * @return
     */
    public static List<String> getNetworkTimeServers() {
        waitForStartup();
        if (countNetworkTimeServers() == 0 && !defaultNetworkTimeServersLoaded) {
            loadDefaultNetworkTimeServers();
        }
        List<String> servers = new LinkedList<String>();
        synchronized (networkTimeServers) {
            servers.addAll(networkTimeServers);
        }
        return servers;
    }

    /**
     * 
     * @return
     */
    public static int countNetworkTimeServers() {
        synchronized (networkTimeServers) {
            return networkTimeServers.size();
        }
    }

    /**
     * 
     */
    public static void loadDefaultNetworkTimeServers() {
        synchronized (networkTimeServers) {
            if (TestUtil.isTesting() || defaultNetworkTimeServersLoaded) {
                return;
            }
            debugOut("Loading default network time servers.");
            // load the network time server IP addresses
            Set<String> servers = new HashSet<String>();
            InputStream is = null;
            try {
                is = openStreamToFile("/org/tranche/time/default_nts.conf");
                String line = null;
                while ((line = readLineIgnoreComments(is)) != null) {
                    servers.add(line.trim().toLowerCase());
                }
            } catch (Exception e) {
                debugErr(e);
            } finally {
                IOUtil.safeClose(is);
                networkTimeServers.clear();
                networkTimeServers.addAll(servers);
                defaultNetworkTimeServersLoaded = true;
                debugOut("Done loading network time servers.");
            }
        }
    }

    /**
     * <p>Returns network-wide server attributes from URL.</p>
     * @return
     */
    public static Map<String, String> getServerConfigurationAttributes() {
        // all servers should update their configuration attributes at the same time
        long remainder = TimeUtil.getTrancheTimestamp() % attributesCacheTimestampModulus;
        if (lastAttributeCacheTimestampModulusValue == -1 || remainder < lastAttributeCacheTimestampModulusValue) {
            attributesCache = updateServerConfigurationAttributes();
        }
        lastAttributeCacheTimestampModulusValue = remainder;
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
            HttpClient c = new HttpClient();
            PostMethod pm = new PostMethod(updateURL);
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
