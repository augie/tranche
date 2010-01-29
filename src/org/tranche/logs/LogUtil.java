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
package org.tranche.logs;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.tranche.ConfigureTranche;
import org.tranche.network.*;
import org.tranche.time.TimeUtil;
import org.tranche.users.UserZipFile;
import org.tranche.util.EmailUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.Text;

/**
 * Utility code to help send logs to the appropriate groups.
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class LogUtil {

    /**
     * Helper method to log an exception to the Tranche error log.
     * @param e
     */
    public static void logError(Exception e) {
        logError(e, "", null);
    }

    /**
     * 
     * @param e
     */
    public static void logError(Set<Exception> e) {
        logError(e, "", null);
    }

    /**
     * 
     * @param e
     * @param description
     * @param uzf
     */
    public static void logError(Exception e, String description, UserZipFile uzf) {
        Set<Exception> exceptions = new HashSet<Exception>();
        exceptions.add(e);
        logError(exceptions, description, uzf);
    }

    /**
     * Helper method to log an exception to the Tranche error log.
     * @param e
     * @param description
     * @param uzf
     */
    public static void logError(Set<Exception> exceptions, String description, UserZipFile uzf) {
        // skip all errors originating from JUnit
        for (Exception e : exceptions) {
            StackTraceElement[] stes = e.getStackTrace();
            for (StackTraceElement ste : stes) {
                if (ste.getClassName().equals("junit.framework.TestCase")) {
                    return;
                }
            }
        }

        final StringBuffer descriptionBuffer = new StringBuffer(),  exceptionBuffer = new StringBuffer(),  userBuffer = new StringBuffer();

        // Build: messageBuffer
        if (description != null && !description.trim().equals("")) {
            descriptionBuffer.append(description);
        } else {
            descriptionBuffer.append("");
        }

        // Build: exceptionBuffer
        exceptionBuffer.append("Exceptions: " + exceptions.size() + " exceptions to report" + Text.getNewLine());
        for (Exception e : exceptions) {
            exceptionBuffer.append(e.getClass().getName() + ": " + e.getMessage() + Text.getNewLine());
            for (StackTraceElement ste : e.getStackTrace()) {
                exceptionBuffer.append("    " + ste + Text.getNewLine());
            }
            exceptionBuffer.append(Text.getNewLine());
        }
        exceptionBuffer.append(Text.getNewLine());

        // Build: userBuffer
        if (uzf == null) {
            userBuffer.append("");
        } else {
            userBuffer.append(uzf.getUserNameFromCert() + " <" + uzf.getEmail() + ">");
        }

        final String serversDump = getServersDump();
        final String threadDump = getThreadDump();
        final String memoryDump = getEnvironmentDump();

        final String submitURL = ConfigureTranche.get(ConfigureTranche.PROP_LOG_ERROR_URL);
        if (submitURL != null && !submitURL.trim().equals("")) {
            Thread t = new Thread("Submit error thread") {

                @Override
                public void run() {
                    HttpClient c = new HttpClient();
                    PostMethod pm = new PostMethod(submitURL);
                    try {
                        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
                        pairs.add(new NameValuePair("Exception", exceptionBuffer.toString()));
                        pairs.add(new NameValuePair("Servers", serversDump));
                        pairs.add(new NameValuePair("Threads", threadDump));
                        pairs.add(new NameValuePair("Memory", memoryDump));
                        pairs.add(new NameValuePair("UserInfo", userBuffer.toString()));
                        pairs.add(new NameValuePair("UserComment", descriptionBuffer.toString()));

                        // create the pair array
                        NameValuePair[] pairArray = new NameValuePair[pairs.size()];
                        for (int i = 0; i < pairs.size(); i++) {
                            pairArray[i] = pairs.get(i);
                        }

                        // set the values
                        pm.setRequestBody(pairArray);

                        // execute the method
                        int statusCode = c.executeMethod(pm);

                        // If registrar failed to send email, do so here...
                        if (statusCode < 200 || statusCode >= 300) {
                            throw new Exception("Failed to register, returned HTTP status code: " + statusCode);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        pm.releaseConnection();
                    }
                }
            };
            t.start();
        }

        // Send all the information to the appropriate email addresses
        if (ConfigureTranche.getAdminEmailAccounts().length > 0) {
            File tempFile = null;
            try {
                String subject = "[" + ConfigureTranche.get(ConfigureTranche.PROP_NAME) + "] Error @ " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp());
                StringBuffer msg = new StringBuffer();
                msg.append("User comments:\n");
                msg.append(descriptionBuffer + "\n\n\n");
                msg.append("Submitted by: " + userBuffer + "\n\n\n");
                msg.append(exceptionBuffer + "\n");
                tempFile = getTroubleshootingInformationFile();
                try {
                    EmailUtil.sendEmail(subject, ConfigureTranche.getAdminEmailAccounts(), msg.toString(), tempFile);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            } finally {
                IOUtil.safeDelete(tempFile);
            }
        }
    }

    /**
     * 
     * @return
     */
    public static final String getEnvironmentDump() {
        StringBuffer buf = new StringBuffer();
        buf.append("Build: @buildNumber" + Text.getNewLine());
        try {
            buf.append("Internet host/address: " + java.net.InetAddress.getLocalHost() + Text.getNewLine());
        } catch (Exception e) {
        }
        Runtime rt = Runtime.getRuntime();
        long fm = rt.freeMemory();
        long tm = rt.totalMemory();
        long mm = rt.maxMemory();
        buf.append("Memory: ");
        buf.append(Text.getNewLine());
        buf.append("    Free: " + Text.getFormattedBytes(fm));
        buf.append(Text.getNewLine());
        buf.append("    Used: " + Text.getFormattedBytes(tm - fm));
        buf.append(Text.getNewLine());
        buf.append("    Total: " + Text.getFormattedBytes(tm));
        buf.append(Text.getNewLine());
        buf.append("    Maximum:" + Text.getFormattedBytes(mm));
        buf.append(Text.getNewLine() + Text.getNewLine() + Text.getNewLine());
        return buf.toString();
    }

    /**
     * 
     * @return
     */
    public static final String getServersDump() {
        StatusTable table = NetworkUtil.getStatus().clone();
        int onlineCount = 0, onlineNonCoreServers = 0, connectedCount = 0;
        for (StatusTableRow row : table.getRows()) {
            if (row.isOnline()) {
                if (row.isCore()) {
                    onlineCount++;
                } else {
                    onlineNonCoreServers++;
                }
            }
            if (ConnectionUtil.isConnected(row.getHost())) {
                connectedCount++;
            }
        }
        StringBuffer buf = new StringBuffer();
        buf.append("Servers: " + onlineCount + " online core servers, " + onlineNonCoreServers + " other online servers" + Text.getNewLine());
        buf.append(table.toString() + Text.getNewLine() + Text.getNewLine() + Text.getNewLine());
        buf.append("Connections: " + connectedCount + " connections" + Text.getNewLine() + Text.getNewLine());
        for (StatusTableRow row : table.getRows()) {
            if (ConnectionUtil.isConnected(row.getHost())) {
                buf.append("  " + row.getName() + " (" + row.getHost() + ")" + Text.getNewLine());
            }
        }
        buf.append(Text.getNewLine() + Text.getNewLine());
        return buf.toString();
    }

    /**
     * 
     * @return
     */
    public static final String getThreadDump() {
        Map<Thread, StackTraceElement[]> threadInfo = Thread.getAllStackTraces();
        StringBuffer buf = new StringBuffer();
        buf.append("Thread dump: " + threadInfo.size() + " threads");
        buf.append(Text.getNewLine() + Text.getNewLine());
        for (Thread t : threadInfo.keySet()) {
            StackTraceElement[] ste = threadInfo.get(t);
            String daemonMsg = t.isDaemon() ? "daemon" : "non-daemon";
            String aliveMsg = t.isAlive() ? "alive" : "non-alive";
            buf.append("    * " + t.getName() + " (priority: " + t.getPriority() + ", " + daemonMsg + ", " + aliveMsg + ", state: " + t.getState() + ") ");
            buf.append(Text.getNewLine());

            for (int i = 0; i < ste.length; i++) {
                buf.append("        " + ste[i].toString());
                buf.append(Text.getNewLine());
            }

            buf.append(Text.getNewLine());
        }
        buf.append(Text.getNewLine() + Text.getNewLine());
        return buf.toString();
    }

    /**
     * 
     * @return
     */
    public static final String getTroubleshootingInformation() {
        StringBuffer buf = new StringBuffer();
        buf.append(getEnvironmentDump());
        buf.append(getServersDump());
        buf.append(getThreadDump());
        return buf.toString();
    }

    /**
     * 
     * @return
     */
    public static final File getTroubleshootingInformationFile() {
        File tempFile = null;
        try {
            File tempDir = TempFileUtil.createTemporaryDirectory();
            tempFile = new File(tempDir, "troubleshooting-info.txt");
            tempFile.createNewFile();
            FileWriter fw = null;
            try {
                fw = new FileWriter(tempFile);
                fw.write(getTroubleshootingInformation());
                fw.flush();
            } finally {
                IOUtil.safeClose(fw);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tempFile;
    }
}
