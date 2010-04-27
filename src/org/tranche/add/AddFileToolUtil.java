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
package org.tranche.add;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.tranche.ConfigureTranche;
import org.tranche.commons.DebugUtil;
import org.tranche.commons.TextUtil;
import org.tranche.logs.LogUtil;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.time.TimeUtil;
import org.tranche.users.UserCertificateUtil;
import org.tranche.util.EmailUtil;

/**
 * <p>Utility that contains methods for the AddFileTool.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class AddFileToolUtil {

    /**
     * <p>Class cannot be instantiated.</p>
     */
    private AddFileToolUtil() {
    }

    /**
     * 
     * @param aft
     * @param report
     */
    public static void registerUpload(final AddFileTool aft, final AddFileToolReport report) {
        final String logURL = ConfigureTranche.get(ConfigureTranche.CATEGORY_LOGGING, ConfigureTranche.PROP_LOG_UPLOAD_URL);
        if (logURL == null || logURL.equals("")) {
            return;
        }

        HttpClient c = new HttpClient();
        PostMethod pm = new PostMethod(logURL);
        try {
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new NameValuePair("Title", aft.getTitle()));
            pairs.add(new NameValuePair("Description", aft.getDescription()));
            pairs.add(new NameValuePair("Timestamp", String.valueOf(report.getTimestampStart())));
            if (aft.getPassphrase() == null) {
                pairs.add(new NameValuePair("passphrase", ""));
            } else {
                pairs.add(new NameValuePair("passphrase", aft.getPassphrase()));
            }
            pairs.add(new NameValuePair("hash", report.getHash().toString()));
            pairs.add(new NameValuePair("uploadTime", String.valueOf(report.getTimeToFinish())));
            pairs.add(new NameValuePair("Submitted By", UserCertificateUtil.readUserName(aft.getUserCertificate())));
            pairs.add(new NameValuePair("Tranche:Files", String.valueOf(report.getOriginalFileCount())));
            pairs.add(new NameValuePair("Tranche:Size", String.valueOf(report.getOriginalBytesUploaded())));
            for (MetaDataAnnotation mda : aft.getMetaDataAnnotations()) {
                pairs.add(new NameValuePair(mda.getName(), mda.getValue()));
            }

            if (aft.getLicense() != null) {
                try {
                    pairs.add(new NameValuePair("licenseText", aft.getLicense().getDescription()));
                    pairs.add(new NameValuePair("licenseName", aft.getLicense().getTitle()));
                    pairs.add(new NameValuePair("licenseShortName", aft.getLicense().getShortDescription()));
                } catch (Exception ex) {
                    System.err.println(ex.getClass().getSimpleName() + " happened while including license information: " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }

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
            if (statusCode != 200 && ConfigureTranche.getAdminEmailAccounts().length != 0) {
                String message = "You are receiving this message from the AddFileTool because the the upload registrar returned an HTTP code of " + statusCode + ".\n\n"
                        + "Submitted by: " + UserCertificateUtil.readUserName(aft.getUserCertificate()) + "\n"
                        + "Hash: " + report.getHash().toString() + "\n"
                        + "Title: " + report.getTitle() + "\n"
                        + "Description: " + report.getDescription() + "\n";
                EmailUtil.safeSendEmail("[" + ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_NAME) + "] Upload Registration Failed @ " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()), ConfigureTranche.getAdminEmailAccounts(), message);
            }
        } catch (Exception e) {
            DebugUtil.debugErr(AddFileToolUtil.class, e);
        } finally {
            pm.releaseConnection();
        }
    }

    /**
     * 
     * @param recipients
     * @param report
     * @param wait
     */
    public static void emailReceipt(String[] recipients, AddFileToolReport report) {
        EmailUtil.safeSendEmail(getEmailReceiptSubject(report), recipients, getEmailReceiptMessage(report));
    }

    /**
     *
     * @param report
     * @return
     */
    public static String getEmailReceiptSubject(AddFileToolReport report) {
        return "[" + ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_NAME) + "] Upload Receipt: " + report.getTitle();
    }

    /**
     * 
     * @param report
     * @return
     */
    public static String getEmailReceiptMessage(AddFileToolReport report) {
        String message = "This is a receipt for your upload of \"" + report.getTitle() + "\" at " + TextUtil.getFormattedDate(report.getTimestampStart()) + "." + "\n" + "\n"
                + "This is the hash that represents your data:" + "\n" + "\n"
                + report.getHash().toString() + "\n" + "\n"
                + "More information about this upload:" + "\n" + "\n"
                + "          Network: " + ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_NAME) + "\n"
                + "          Title: " + report.getTitle() + "\n"
                + "          Description: " + report.getDescription() + "\n"
                + "          Uploaded: " + TextUtil.getFormattedDate(report.getTimestampStart()) + "\n";
        if (report.isEncrypted()) {
            message = message
                    + "          Encrypted - [passphrase ommitted for your protection]" + "\n";
        }
        message = message + "\n";
        if (report.isEncrypted()) {
            message = message + "Note that your data set is " + (report.isShowMetaDataIfEncrypted() ? "encrypted" : "encrypted and hidden") + ". When you want your data set to be available to the public, you will need to log in, and go to 'Manage your Tranche data'. There, you need to:" + "\n" + "\n";
            message = message
                    + "          Select your data set and choose \"Make Public\"" + "\n";
            if (!report.isShowMetaDataIfEncrypted()) {
                message = message
                        + "          Select your data set and choose \"Set as Visible\"" + "\n";
            }
            if (!report.isShowMetaDataIfEncrypted()) {
                message = message + "\n" + "Making a data set public is different from making it visible when it is hidden, and hence requires the actions noted above.";
            }
        }
        message = message + "\n" + "\n";
        if (ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_DOWNLOAD_TOOL_URL) != null && !ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_DOWNLOAD_TOOL_URL).equals("")) {
            message = message + "The following is a URL to download the data. Please do not include this URL in your citation." + "\n" + "\n"
                    + "          " + ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_DOWNLOAD_TOOL_URL) + "?fileName=" + report.getHash().toWebSafeString() + "\n" + "\n";
        }
        if (ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_DATA_URL) != null && !ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_DATA_URL).equals("")) {
            message = message + "The following is a URL to your data page, which includes all annotated information about your data set. Please do not include this URL in your citation." + "\n" + "\n"
                    + "          " + ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_DATA_URL) + "?id=" + report.getHash().toWebSafeString() + "\n" + "\n";
        }
        if (report.isEncrypted() && ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_PUBLISH_PASSPHRASE_URL) != null && !ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_PUBLISH_PASSPHRASE_URL).equals("")) {
            message = message + "Because your upload is encrypted, it is only accessible with the proper passphrase. When you want to make your data public, you can publish the passphrase. Publishing the passphrase will make the data publicly available without showing the passphrase. Here is the URL where you can publish your passphrase:" + "\n" + "\n"
                    + "          " + ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_PUBLISH_PASSPHRASE_URL) + "?hash=" + report.getHash().toWebSafeString() + "\n" + "\n";
        }
        message = message + "You should cite your data in the following manner:" + "\n" + "\n"
                + "---------------------------------------------------------------------------" + "\n"
                + "          The data associated with this manuscript may be downloaded from " + ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_NAME) + " using the following hash: " + "\n" + "\n"
                + "          " + report.getHash().toString() + "\n" + "\n"
                + "          The hash may be used to prove exactly what files were published as part of this manuscript's data set, and the hash may also be used to check that the data has not changed since publication." + "\n"
                + "---------------------------------------------------------------------------" + "\n" + "\n"
                + "We ask that you please DO NOT include any other URLs in your citation." + "\n" + "\n";
        return message;
    }

    /**
     * 
     * @param recipients
     * @param aft
     * @param report
     */
    public static void emailFailureNotice(String[] recipients, AddFileTool aft, AddFileToolReport report) {
        String subject = "[" + ConfigureTranche.get(ConfigureTranche.CATEGORY_GENERAL, ConfigureTranche.PROP_NAME) + "] Failed Upload @ " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp());
        final StringBuffer message = new StringBuffer();
        try {
            message.append("An upload failed at " + TextUtil.getFormattedDate(TimeUtil.getTrancheTimestamp()) + " due to:" + "\n" + "\n");
            for (PropagationExceptionWrapper e : report.getFailureExceptions()) {
                message.append("* " + e.exception.getClass().getName() + ": " + e.exception.getMessage() + "\n");

                for (StackTraceElement ste : e.exception.getStackTrace()) {
                    message.append("    - " + ste.toString() + "\n");
                }

                message.append("\n");
            }
            message.append("Title: " + aft.getTitle() + "\n" + "\n");
            message.append("Description: " + aft.getDescription() + "\n" + "\n");
            message.append(LogUtil.getTroubleshootingInformation() + "\n" + "\n");
            // Build up string list of admin email addresses to which user can forward error
            StringBuffer adminEmailListBuffer = new StringBuffer();
            final String[] a = ConfigureTranche.getAdminEmailAccounts();
            for (int i = 0; i < a.length; i++) {
                adminEmailListBuffer.append(a[i]);
                if (i < a.length - 1) {
                    adminEmailListBuffer.append(", ");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        EmailUtil.safeSendEmail(subject, recipients, message.toString());
    }
}
