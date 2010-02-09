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
package org.tranche.get;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Set;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.tranche.ConfigureTranche;
import org.tranche.hash.Base16;
import org.tranche.hash.BigHash;
import org.tranche.logs.LogUtil;
import org.tranche.project.ProjectFilePart;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.util.EmailUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.Text;

/**
 * <p>Contains methods that are commonly used along with or at several points within the GetFileTool class.</p>
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetFileToolUtil {

    /**
     * <p>Class cannot be instantiated.</p>
     */
    private GetFileToolUtil() {
    }

    /**
     * <p>Returns the regular expression that corresponds with all of the given project file parts in the set.</p>
     * @param parts
     * @return
     */
    public static String getRegex(Set<ProjectFilePart> parts) {
        String regex = "";
        // return immediately if null
        if (parts == null) {
            return regex;
        }
        // download only the parts in the set
        for (ProjectFilePart part : parts) {
            regex = regex + "^" + Text.forRegex(part.getRelativeName()) + "$|";
        }
        // clean up
        if (regex.length() > 0) {
            regex = regex.substring(0, regex.lastIndexOf("|"));
        }
        return regex;
    }

    /**
     * <p>Checks whether the given directory can be written to.</p>
     * @param directory
     * @throws java.lang.Exception
     */
    public static void testDirectoryForWritability(File directory) throws Exception {
        /**
         * Take the time to create if not exist. Shouldn't throw an exception.
         */
        if (!directory.exists()) {
            directory.mkdirs();
        }

        /**
         * If can't create, throw an exception
         */
        if (!directory.exists()) {
            throw new Exception("Cannot download to path <" + directory.getAbsolutePath() + ">. Tried to create directory, but couldn't. Please verify that you can write to this directory.");
        }
        if (!directory.canWrite()) {
            throw new Exception("Cannot download to path <" + directory.getAbsolutePath() + ">. Directory exists, but cannot write to it. Please verify that you can write to this directory.");
        }
    }

    /**
     * <p>Sends a notification via an HTTP POST to a web server that a successful download has occurred.</p>
     * <p>The URL to send the notification to must be set using the GetFileToolUtil.setURL(String url) method for this action to be performed.</p>
     * <p>A Thread with minimum priority is spawned to handle this event, so this method is asynchronous.</p>
     * @param gft
     * @param report
     */
    public static void registerDownload(final GetFileTool gft, final GetFileToolReport report) {
        if (TestUtil.isTesting()) {
            return;
        }

        final String logURL = ConfigureTranche.get(ConfigureTranche.PROP_LOG_DOWNLOAD_URL);
        if (logURL == null || logURL.equals("")) {
            return;
        }

        // Notify server of download
        try {
            HttpClient c = new HttpClient();
            PostMethod pm = new PostMethod(logURL);

            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new NameValuePair("hash", Base16.encode(gft.getHash().toByteArray())));
            params.add(new NameValuePair("downloadTime", String.valueOf(report.getTimeToFinish())));
            if (report.getBytesDownloaded() != 0) {
                params.add(new NameValuePair("uncompressedSize", String.valueOf(report.getBytesDownloaded())));
            }
            pm.setRequestBody(params.toArray(new NameValuePair[0]));

            // best effort, do not print anything
            c.executeMethod(pm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>Registers a failed download only if configured to do so.</p>
     * @param gft
     * @param report
     */
    public static void registerFailedDownload(final GetFileTool gft, final GetFileToolReport report) {
        if (TestUtil.isTesting()) {
            return;
        }

        try {
            String[] emailRecipients = ConfigureTranche.getAdminEmailAccounts();
            String subject = "[" + ConfigureTranche.get(ConfigureTranche.PROP_NAME) + "] Failed Download @ " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp());

            StringBuffer message = new StringBuffer();
            message.append("A download failed @ " + Text.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " on " + ConfigureTranche.get(ConfigureTranche.PROP_NAME) + "\n\n");
            message.append("--------------------------------------------------------------------------------------------\n");
            for (PropagationExceptionWrapper pew : report.getFailureExceptions()) {
                message.append(pew.toString() + Text.getNewLine());
                for (StackTraceElement ste : pew.exception.getStackTrace()) {
                    message.append("    " + ste + Text.getNewLine());
                }
                message.append(Text.getNewLine());
            }
            message.append("--------------------------------------------------------------------------------------------\n");
            message.append("\n");
            if (gft.getMetaData() != null && gft.getMetaData().getDataSetName() != null) {
                message.append("Title: " + gft.getMetaData().getDataSetName() + "\n");
            }
            message.append("Hash: " + gft.getHash().toString() + "\n");
            message.append("Using passphrase?: " + String.valueOf(gft.getPassphrase() != null) + "\n");


            File tempFile = null;
            try {
                tempFile = LogUtil.getTroubleshootingInformationFile();

                if (gft.getFailedChunksListener() != null) {

                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new FileWriter(tempFile, true));
                        GetFileToolFailedChunksListener failedChunksListener = gft.getFailedChunksListener();

                        // Any missing meta data chunks
                        int missingMDSize = failedChunksListener.getMissingMetaDataChunks().size();
                        if (missingMDSize > 0) {
                            writer.write("* Data set missing "+missingMDSize+" meta chunk"+(missingMDSize==1?"":"s")+":");
                            writer.newLine();
                            for (BigHash metaHash : failedChunksListener.getMissingMetaDataChunks()) {
                                writer.write("    - "+metaHash);
                                writer.newLine();
                            }
                        } else {
                            writer.write("* Data set not missing any meta data chunks.");
                            writer.newLine();
                        }
                        writer.newLine();
                        
                        // Any missing data chunks
                        int missingDataChunksSize = 0, missingFromFiles = failedChunksListener.getMissingDataChunks().size();
                        
                        if (missingFromFiles > 0) {
                            for (BigHash metaHash : failedChunksListener.getMissingDataChunks().keySet()) {
                                Set<BigHash> dataChunks = failedChunksListener.getMissingDataChunks().get(metaHash);
                                missingDataChunksSize += dataChunks.size();
                            }
                            
                            writer.write("A total of "+missingDataChunksSize+" data chunks missing in "+missingFromFiles+" files:");
                            writer.newLine();
                            
                            for (BigHash metaHash : failedChunksListener.getMissingDataChunks().keySet()) {
                                Set<BigHash> dataChunks = failedChunksListener.getMissingDataChunks().get(metaHash);
                                writer.write("    - File: "+metaHash);
                                writer.newLine();
                                
                                for (BigHash dataHash : dataChunks) {
                                    writer.write("        -> "+dataHash);
                                    writer.newLine();
                                }
                            }
                        } else {
                            writer.write("* No missing data chunks were reported. (Note: data chunks might be missing and not reported because associated meta data not found.)");
                            writer.newLine();
                        }
                        
                        writer.newLine();
                        writer.newLine();
                        
                    } finally {
                        IOUtil.safeClose(writer);
                    }
                }

                EmailUtil.sendEmail(subject, emailRecipients, message.toString(), tempFile);
            } finally {
                IOUtil.safeDelete(tempFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        final String logFailureURL = ConfigureTranche.get(ConfigureTranche.PROP_LOG_DOWNLOAD_FAILURE);
        if (logFailureURL == null || logFailureURL.equals("")) {
            return;
        }

        try {
            // make a new client
            HttpClient c = new HttpClient();
            PostMethod pm = new PostMethod(logFailureURL);

            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new NameValuePair("hash", Base16.encode(gft.getHash().toByteArray())));

            // TODO: remove these in the registration script
            params.add(new NameValuePair("username", ""));
            params.add(new NameValuePair("toolType", ""));

            // TODO: refactor registration script to allow for multiple exceptions
            String exceptionName = "";
            String exceptionMessage = "";
            String exceptionStack = "";
            if (!report.getFailureExceptions().isEmpty()) {
                PropagationExceptionWrapper e = report.getFailureExceptions().get(0);
                exceptionName = e.exception.getClass().getName();
                exceptionMessage = e.exception.getMessage();
                for (StackTraceElement ste : e.exception.getStackTrace()) {
                    exceptionStack = exceptionStack + ste.toString() + "\n";
                }
            }

            params.add(new NameValuePair("exceptionName", exceptionName == null ? "" : exceptionName));
            params.add(new NameValuePair("exceptionMessage", exceptionMessage == null ? "" : exceptionMessage));
            params.add(new NameValuePair("exceptionStack", exceptionStack == null ? "" : exceptionStack));
            params.add(new NameValuePair("buildNumber", "@buildNumber"));
            params.add(new NameValuePair("timestamp", String.valueOf(report.getTimestampEnd())));
            pm.setRequestBody(params.toArray(new NameValuePair[0]));

            // best effort, do not print anything
            c.executeMethod(pm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
