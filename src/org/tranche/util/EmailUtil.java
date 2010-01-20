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
package org.tranche.util;

import org.tranche.security.SecurityUtil;
import org.tranche.security.EasySSLProtocolSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.tranche.ConfigureTranche;

/**
 * <p>Send an email by posting contents to URL that will send it out.</p>
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class EmailUtil {

    static {
        // load the HTTPS protocol just once on startup
        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
    }

    /**
     * <p>Sends an email in a separate thread without bubbling out any exceptions.</p>
     * @param subject
     * @param recipients
     * @param message
     */
    public static void safeSendEmail(String subject, String[] recipients, String message) {
        safeSendEmail(subject, recipients, message, null);
    }

    /**
     * <p>Sends an email in a separate thread without bubbling out any exceptions.</p>
     * @param subject
     * @param recipients
     * @param message
     * @param attachment
     * @param wait
     */
    public static void safeSendEmail(final String subject, final String[] recipients, final String message, final File attachment) {
        try {
            sendEmail(subject, recipients, message, attachment);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>Send an email. A blocking operation.</p>
     * @param subject
     * @param recipients
     * @param message
     * @throws java.io.IOException
     */
    public static void sendEmail(String subject, String[] recipients, String message) throws IOException {
        sendEmail(subject, recipients, message, null);
    }

    /**
     * <p>Send an email. A blocking operation.</p>
     * @param subject
     * @param recipients
     * @param message
     * @param attachment
     * @throws java.io.IOException
     */
    public static void sendEmail(String subject, String[] recipients, String message, File attachment) throws IOException {
        InputStream is = null;
        try {
            // dummy check - are there recipients?
            if (recipients == null || recipients.length == 0 || recipients[0].equals("")) {
                throw new RuntimeException("No recipients specified.");
            } // is there a subject?
            else if (subject == null) {
                throw new RuntimeException("No subject specified.");
            } // is there a message?
            else if (message == null) {
                throw new RuntimeException("No message specified.");
            }

            // make a new client
            HttpClient c = new HttpClient();
            PostMethod pm = new PostMethod(ConfigureTranche.get(ConfigureTranche.PROP_EMAIL_URL));

            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new NameValuePair("network", ConfigureTranche.get(ConfigureTranche.PROP_NAME)));

            // put together the recipients
            String toStr = "";
            for (String recipient : recipients) {
                toStr = toStr + recipient + ", ";
            }
            toStr = toStr.substring(0, toStr.length() - 2);
            params.add(new NameValuePair("to", toStr));

            String toSignature = "";
            {
                ByteArrayInputStream bais = null;
                try {
                    bais = new ByteArrayInputStream(toStr.getBytes());
                    toSignature = String.valueOf(SecurityUtil.sign(bais, SecurityUtil.getEmailKey(), SecurityUtil.getEmailCertificate().getSigAlgName()));
                } catch (Exception e) {
                } finally {
                    IOUtil.safeClose(bais);
                }
            }
            params.add(new NameValuePair("toSignature", toSignature));
            String signatureAlgorithm = "";
            try {
                signatureAlgorithm = SecurityUtil.getEmailCertificate().getSigAlgName();
            } catch (Exception e) {
            }
            params.add(new NameValuePair("signatureAlgorithm", signatureAlgorithm));
            params.add(new NameValuePair("subject", subject));
            params.add(new NameValuePair("message", message));
            if (attachment != null) {
                params.add(new NameValuePair("attachment", attachment.getName()));
                is = attachment.toURL().openStream();
                pm.setRequestBody(is);
            }
            pm.setQueryString(params.toArray(new NameValuePair[]{}));

            int code = c.executeMethod(pm);
            if (code != 200) {
                if (code == 506) {
                    throw new RuntimeException("Network not found.");
                } else if (code == 507) {
                    throw new RuntimeException("Validation failed.");
                } else if (code == 508) {
                    throw new RuntimeException("To cannot be blank.");
                } else if (code == 509) {
                    throw new RuntimeException("Subject cannot be blank.");
                } else if (code == 510) {
                    throw new RuntimeException("Message cannot be blank.");
                } else if (code == 511) {
                    throw new RuntimeException("Network cannot be blank.");
                } else if (code == 512) {
                    throw new RuntimeException("To Signature cannot be blank.");
                } else if (code == 513) {
                    throw new RuntimeException("Invalid signature for To field.");
                } else if (code == 514) {
                    throw new RuntimeException("Signature algorithm cannot be blank.");
                } else {
                    throw new RuntimeException("Email could not be sent (response code = " + code + ").");
                }
            }
        } finally {
            IOUtil.safeClose(is);
        }
    }
}
