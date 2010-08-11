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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.tranche.commons.ConfigurationUtil;
import org.tranche.commons.DebugUtil;
import org.tranche.network.NetworkUtil;
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
public class ConfigureTranche extends ConfigurationUtil {

    /**
     * <p>The general category.</p>
     */
    public static final String CATEGORY_GENERAL = "GENERAL";
    /**
     * <p>A comma-separated list of email addresses to which email notifications will be sent.</p>
     */
    public static final String PROP_ADMIN_EMAIL_ACCOUNTS = "admin.emails";
    /**
     * <p>The number of servers at which only a single full hash span of servers are connected to.</p>
     */
    public static final String PROP_CONNECTION_FULL_HASH_SPAN_THRESHOLD = "connection.full.hashspan.threshold";
    /**
     * <p>The number of milliseconds between sending a ping to each connected server to keep the socket alive.</p>
     */
    public static final String PROP_KEEP_ALIVE_INTERVAL = "connection.keep.alive.interval";
    /**
     * <p>The number of timeout exceptions to allow in a row before flagging a server offline.</p>
     */
    public static final String PROP_CONNECTION_TIMEOUTS = "connection.timeout.exceptions";
    /**
     * <p>The email address for users to contact.</p>
     */
    public static final String PROP_CONTACT_EMAIL = "contact.email";
    /**
     * <p>The URL where users can contact the administrators.</p>
     */
    public static final String PROP_CONTACT_URL = "contact.url";
    /**
     * <p>The description of the repository.</p>
     */
    public static final String PROP_DESCRIPTION = "description";
    /**
     * <p>The URL to HTTP POST email requests.</p>
     */
    public static final String PROP_EMAIL_URL = "email.url";
    /**
     * <p>The URL for the repository web site.</p>
     */
    public static final String PROP_HOME_URL = "home.url";
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
     * <p>The name of the repository.</p>
     */
    public static final String PROP_NAME = "name";
    /**
     * <p>The Tranche hash of the project cache. The project cache URL will be tried first.</p>
     */
    public static final String PROP_PROJECT_CACHE_HASH = "project.cache.hash";
    /**
     * <p>The URL of the project cache.</p>
     */
    public static final String PROP_PROJECT_CACHE_URL = "project.cache.url";
    /**
     * <p>The URL where a user can go to publish their data sets.</p>
     */
    public static final String PROP_PUBLISH_PASSPHRASE_URL = "project.publish.url";
    /**
     * <p>The URL of the data set's descriptive information.</p>
     */
    public static final String PROP_DATA_URL = "project.url";
    /**
     * <p>The number of replications required for each chunk of data.</p>
     */
    public static final String PROP_REPLICATIONS = "replications";
    /**
     * <p>The URL for users to sign up for the repository.</p>
     */
    public static final String PROP_SIGN_UP_URL = "signup.url";
    /**
     * <p>Servers that are offline and do not update within this time get removed from the table.</p>
     */
    public static final String PROP_DEFUNCT_SERVER_THRESHOLD = "status.defunct.threshold";
    /**
     * <p>The number of milliseconds between updating the network status table on a client.</p>
     */
    public static final String PROP_STATUS_UPDATE_CLIENT_FREQUENCY = "status.update.client.frequency";
    /**
     * <p>The number of milliseconds between updating the network status table on a server.</p>
     */
    public static final String PROP_STATUS_UPDATE_SERVER_FREQUENCY = "status.update.server.frequency";
    /**
     * <p>The number of servers in a network status table group. Used in server status propagation.</p>
     */
    public static final String PROP_STATUS_UPDATE_SERVER_GROUPING = "status.update.server.grouping";
    /**
     * <p>The URL where users can log in to get their valid user zip files.</p>
     */
    public static final String PROP_USER_LOG_IN_URL = "user.login.url";
    /**
     * <p>The URL where an updated configuration file can be found. The update will occur at regular intervals at about the same time on all clients and servers. If the location for certificate files has changed, they will be reloaded from the new location.</p>
     */
    public static final String PROP_UPDATE_CONFIG_URL = "update.conf.url";
    /**
     * <p>The number of milliseconds between updating the configuration.</p>
     */
    public static final String PROP_UPDATE_CONFIG_INTERVAL = "update.conf.interval";
    /**
     * <p>The core servers category.</p>
     */
    public static final String CATEGORY_CORE_SERVERS = "CORE SERVERS";
    /**
     * <p>The banned servers category.</p>
     */
    public static final String CATEGORY_BANNED_SERVERS = "BANNED SERVERS";
    /**
     * <p>The certificates category.</p>
     */
    public static final String CATEGORY_CERTIFICATES = "CERTIFICATES";
    /**
     * <p>The location of the public certificate for the administrator (all priveleges).</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_ADMIN = "security.certificate.admin";
    /**
     * <p>The location of the public certificate for the anonymous user.</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_ANONYMOUS = "security.certificate.anon";
    /**
     * <p>The location of the public certificate for the auto-certificate user (read, write).</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_AUTOCERT = "security.certificate.autocert";
    /**
     * <p>The location of the public certificate for the email user.</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_EMAIL = "security.certificate.email";
    /**
     * <p>The location of the public certificate for the read-only user.</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_READ = "security.certificate.read";
    /**
     * <p>The location of the public certificate for the standard user (read, write, delete).</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_USER = "security.certificate.user";
    /**
     * <p>The location of the public certificate for the write-only user.</p>
     */
    public static final String PROP_SECURITY_CERTIFICATE_WRITE = "security.certificate.write";
    /**
     * <p>The location of the private key for the anonymous user.</p>
     */
    public static final String PROP_SECURITY_KEY_ANONYMOUS = "security.key.anon";
    /**
     * <p>The location of the private key for the email user.</p>
     */
    public static final String PROP_SECURITY_KEY_EMAIL = "security.key.email";
    /**
     * <p>The logging category.</p>
     */
    public static final String CATEGORY_LOGGING = "LOGGING";
    /**
     * <p>The URL to which the software will register failed downloads.</p>
     */
    public static final String PROP_LOG_DOWNLOAD_FAILURE = "log.download.failure.url";
    /**
     * <p>The URL to which the software will register downloads.</p>
     */
    public static final String PROP_LOG_DOWNLOAD_URL = "log.download.url";
    /**
     * <p>The URL to which the software will submit error logs.</p>
     */
    public static final String PROP_LOG_ERROR_URL = "log.error.url";
    /**
     * <p>The URL to which the software will submit server logs.</p>
     */
    public static final String PROP_LOG_SERVER_URL = "log.server.url";
    /**
     * <p>The URL to which the software will register upload failures.</p>
     */
    public static final String PROP_LOG_UPLOAD_FAILURE_URL = "log.upload.failure.url";
    /**
     * <p>The URL to which the software will register uploads.</p>
     */
    public static final String PROP_LOG_UPLOAD_URL = "log.upload.url";
    /**
     * <p>The server category.</p>
     */
    public static final String CATEGORY_SERVER = "SERVER";
    /**
     * <p>The maximum number of clients that can concurrently connect to a server.</p>
     */
    public static final String PROP_SERVER_CLIENTS_MAX = "server.clients.max";
    /**
     * <p>The URL of the updated server configuration attributes.</p>
     */
    public static final String PROP_SERVER_CONFIG_ATTR_URL = "server.config.attributes.url";
    /**
     * <p>The starting location of the directory that contains all the server configuration and runtime files.</p>
     */
    public static final String PROP_SERVER_DIRECTORY = "server.directory";
    /**
     * <p>The maximum number of milliseconds before a request that is being kept alive is timed out.</p>
     */
    public static final String PROP_SERVER_KEEP_ALIVE_TIMEOUT = "server.keepalive.timeout";
    /**
     * <p>The number of milliseconds between sending offline server notification emails.</p>
     */
    public static final String PROP_SERVER_OFFLINE_NOTIFICATION_INTERVAL = "server.offline.notification.interval";
    /**
     * <p>The starting port number for a server.</p>
     */
    public static final String PROP_SERVER_PORT = "server.port";
    /**
     * <p>The maximum number of requests a client can have outstanding on a server.</p>
     */
    public static final String PROP_SERVER_QUEUE_SIZE = "server.queue.size";
    /**
     * <p>The amount of time that may pass before reregistering the local server with connected servers in the server status update process.</p>
     */
    public static final String PROP_SERVER_TIME_BETWEEN_REGISTRATIONS = "server.server.registration.time";
    /**
     * <p>The number of requests a single server can have executed simultaneously on a server.</p>
     */
    public static final String PROP_SERVER_SERVER_SIMULTANEOUS_REQUESTS = "server.server.simultaneous.requests";
    /**
     * <p>The starting boolean for whether a server should communicate over SSL.</p>
     */
    public static final String PROP_SERVER_SSL = "server.ssl";
    /**
     * <p>The maximum number of milliseconds before a request to a server times out if it doesn't send back a feep-alive signal.</p>
     */
    public static final String PROP_SERVER_TIMEOUT = "server.timeout";
    /**
     * <p>The number of requests a single user can have executed simultaneously on a server.</p>
     */
    public static final String PROP_SERVER_USER_SIMULTANEOUS_REQUESTS = "server.user.simultaneous.requests";
    /**
     * <p>The network time servers category.</p>
     */
    public static final String CATEGORY_NETWORK_TIME_SERVERS = "NETWORK TIME SERVERS";
    /**
     * <p>The number of milliseconds the actual timestamp can deviate from the expected timestamp before resetting the network time.</p>
     */
    public static final String PROP_TIME_CHANGE_CHECK_DEVIATION = "time.change.check.deviation";
    /**
     * <p>The number of milliseconds between checking if the system time has been changed significantly.</p>
     */
    public static final String PROP_TIME_CHANGE_CHECK_INTERVAL = "time.change.check.interval";
    /**
     * <p>The number of milliseconds between forcing a reset of the network time.</p>
     */
    public static final String PROP_TIME_UPDATE_INTERVAL = "time.update.interval";
    /**
     * <p>The timeout per NT server request.</p>
     */
    public static final String PROP_TIME_UPDATE_TIMEOUT = "time.update.timeout";
    /**
     * 
     */
    public static final String DEFAULT_CONFIG_FILE_LOCATION = "/org/tranche/default.conf";
    private static Map<String, String> attributesCache = null;
    private static long lastAttributeCacheTimestampModulusValue = -1;
    private static final long attributesCacheTimestampModulus = 10000000;
    private static boolean loaded = false, updated = false;

    static {
        // load the default values
        try {
            DebugUtil.debugOut(ConfigureTranche.class, "Loading default configuration from " + DEFAULT_CONFIG_FILE_LOCATION);
            ConfigurationUtil.loadDefaults(DEFAULT_CONFIG_FILE_LOCATION);
        } catch (Exception e) {
            DebugUtil.debugErr(ConfigureTranche.class, e);
        }
    }

    /**
     * 
     */
    protected ConfigureTranche() {
    }

    /**
     * <p>Returns a string array of administrator email addresses.</p>
     * @return
     */
    public static final String[] getAdminEmailAccounts() {
        String accounts = get(CATEGORY_GENERAL, PROP_ADMIN_EMAIL_ACCOUNTS);
        if (accounts != null && !accounts.equals("")) {
            return accounts.split(",");
        } else {
            return new String[0];
        }
    }

    /**
     * 
     */
    public static void waitForStartup() {
        synchronized (ConfigureTranche.class) {
            while (!loaded && !TestUtil.isTesting()) {
                try {
                    ConfigureTranche.class.wait();
                } catch (Exception e) {
                    DebugUtil.debugErr(ConfigureTranche.class, e);
                }
            }
        }
    }

    /**
     * 
     * @param args
     * @throws IOException
     */
    public static void load(String[] args) throws IOException {
        if (args.length > 0) {
            load(args[0]);
        } else {
            load("");
        }
    }

    /**
     * 
     * @param configFile
     * @throws IOException
     */
    public static void load(String configFile) throws IOException {
        load(openStreamToFile(configFile));
    }

    /**
     * 
     * @param configFileStream
     */
    public synchronized static void load(InputStream configFileStream) {
        try {
            ConfigurationUtil.load(configFileStream);

            // distribute
            NetworkUtil.setStartupServerURLs(getList(CATEGORY_CORE_SERVERS));
            NetworkUtil.setBannedServerHosts(getList(CATEGORY_BANNED_SERVERS));
            SecurityUtil.setAdminCertLocation(get(CATEGORY_CERTIFICATES, PROP_SECURITY_CERTIFICATE_ADMIN));
            SecurityUtil.setWriteCertLocation(get(CATEGORY_CERTIFICATES, PROP_SECURITY_CERTIFICATE_WRITE));
            SecurityUtil.setUserCertLocation(get(CATEGORY_CERTIFICATES, PROP_SECURITY_CERTIFICATE_USER));
            SecurityUtil.setReadCertLocation(get(CATEGORY_CERTIFICATES, PROP_SECURITY_CERTIFICATE_READ));
            SecurityUtil.setAutocertCertLocation(get(CATEGORY_CERTIFICATES, PROP_SECURITY_CERTIFICATE_AUTOCERT));
            SecurityUtil.setAnonCertLocation(get(CATEGORY_CERTIFICATES, PROP_SECURITY_CERTIFICATE_ANONYMOUS));
            SecurityUtil.setAnonKeyLocation(get(CATEGORY_CERTIFICATES, PROP_SECURITY_KEY_ANONYMOUS));
            SecurityUtil.setEmailCertLocation(get(CATEGORY_CERTIFICATES, PROP_SECURITY_CERTIFICATE_EMAIL));
            SecurityUtil.setEmailKeyLocation(get(CATEGORY_CERTIFICATES, PROP_SECURITY_KEY_EMAIL));

            if (!updated) {
                updated = true;
                update();
            }
        } catch (Exception e) {
            DebugUtil.debugErr(ConfigureTranche.class, e);
        }

        synchronized (ConfigureTranche.class) {
            loaded = true;
            ConfigureTranche.class.notifyAll();
        }
    }

    /**
     *
     */
    public static void update() {
        String url = get(CATEGORY_GENERAL, PROP_UPDATE_CONFIG_URL);
        if (url != null && !url.equals("")) {
            DebugUtil.debugOut(ConfigureTranche.class, "Updating configuration from " + url);
            try {
                // make a new client
                HttpClient c = new HttpClient();
                // make a post method
                GetMethod gm = new GetMethod(url);
                // best effort, do not print anything
                c.executeMethod(gm);
                if (gm.getStatusCode() == 200) {
                    DebugUtil.debugOut(ConfigureTranche.class, "Successfully obtained new configuration from " + url);
                    InputStream configFileStream = null;
                    try {
                        configFileStream = new ByteArrayInputStream(gm.getResponseBody());
                        load(configFileStream);
                    } finally {
                        IOUtil.safeClose(configFileStream);
                    }
                }
            } catch (Exception e) {
                DebugUtil.debugErr(ConfigureTranche.class, e);
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
        DebugUtil.debugOut(ConfigureTranche.class, "Updating server configuration attributes.");
        String updateURL = get(CATEGORY_SERVER, PROP_SERVER_CONFIG_ATTR_URL);

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
            DebugUtil.debugErr(ConfigureTranche.class, e);
        }

        return attributes;
    }
}
