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

import org.tranche.security.SecurityUtil;
import org.tranche.security.EasySSLProtocolSocketFactory;
import org.tranche.util.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.net.ssl.SSLException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.tranche.ConfigureTranche;

/**
 * <p>A utility for creating and using user zip files.</p>
 * @author Bryan E. Smith
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class UserZipFileUtil {

    private static File usersDirectory;

    static {
        Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
        try {
            usersDirectory = PersistentServerFileUtil.getPersistentDirectory("users");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
    <p>Log in. Sends an HTTP POST to the Log In URL to retrieve a valid UserZipFile.</p>
     * @param username
     * @param passphrase
     * @return
     * @throws org.tranche.users.InvalidSignInException
     * @throws java.lang.Exception
     */
    public static UserZipFile getUserZipFile(String username, String passphrase) throws InvalidSignInException, Exception {
        // check local directory
        File userZipFileFile = new File(usersDirectory, username + ".zip.encrypted");
        File userEmailFile = new File(usersDirectory, username + "-email");
        if (usersDirectory != null) {
            try {
                if (userZipFileFile.exists() && userEmailFile.exists()) {
                    UserZipFile uzf = new UserZipFile(userZipFileFile);
                    try {
                        uzf.setPassphrase(passphrase);
                        if (uzf.getCertificate() == null) {
                            uzf.setPassphrase(SecurityUtil.SHA1(passphrase));
                        }
                    } catch (Exception e) {
                        uzf.setPassphrase(SecurityUtil.SHA1(passphrase));
                    }
                    uzf.setEmail(new String(IOUtil.getBytes(userEmailFile)));
                    if (uzf.getCertificate() != null && uzf.isValid()) {
                        return uzf;
                    }
                }
            } catch (Exception e) {
            }
        }
        // go to web site
        String url = ConfigureTranche.get(ConfigureTranche.PROP_USER_LOG_IN_URL);
        if (url == null || url.equals("")) {
            return null;
        }
        HttpClient hc = new HttpClient();
        PostMethod pm = new PostMethod(url);
        try {
            // set up the posting parameters
            ArrayList buf = new ArrayList();
            buf.add(new NameValuePair("name", username));
            buf.add(new NameValuePair("password", passphrase));

            // set the request body
            pm.setRequestBody((NameValuePair[]) buf.toArray(new NameValuePair[0]));

            int returnCode = 0;
            try {
                returnCode = hc.executeMethod(pm);
            } catch (SSLException ssle) {
                throw ssle;
            } catch (IOException ioe) {
                throw new Exception("Could not connect. Please try again later. "+ioe.getClass().getSimpleName()+": "+ioe.getMessage());
            }

            // if not 200 (OK), there is a scripting or connection problem
            if (returnCode != 200) {
                if (returnCode == 506) {
                    throw new InvalidSignInException();
                } else {
                    throw new Exception("There was a problem logging in. Please try again later. (status=" + returnCode + ")");
                }
            } else {
                // wait for headers to load
                ThreadUtil.safeSleep(250);

                // try to get the email
                String email = null;
                for (Header header : pm.getResponseHeaders("User Email")) {
                    if (header.getName().equals("User Email")) {
                        email = header.getValue();
                    }
                }

                // get the log in success
                for (Header header : pm.getResponseHeaders("Log In Success")) {
                    if (header.getName().equals("Log In Success")) {
                        if (header.getValue().equals("true")) {
                            if (!userZipFileFile.exists()) {
                                userZipFileFile.createNewFile();
                            }

                            // read the response (a user zip file)
                            FileOutputStream fos = null;
                            InputStream is = null;
                            try {
                                fos = new FileOutputStream(userZipFileFile);
                                is = pm.getResponseBodyAsStream();
                                byte[] buffer = new byte[1000];
                                for (int bytesRead = is.read(buffer); bytesRead > -1; bytesRead = is.read(buffer)) {
                                    fos.write(buffer, 0, bytesRead);
                                }
                            } finally {
                                IOUtil.safeClose(fos);
                                IOUtil.safeClose(is);
                            }

                            if (!userEmailFile.exists()) {
                                userEmailFile.createNewFile();
                            }

                            if (email != null) {
                                try {
                                    fos = new FileOutputStream(userEmailFile);
                                    fos.write(email.getBytes());
                                } finally {
                                    IOUtil.safeClose(fos);
                                }
                            }

                            UserZipFile userZipFile = new UserZipFile(userZipFileFile);
                            try {
                                userZipFile.setPassphrase(passphrase);
                                if (userZipFile.getCertificate() == null) {
                                    userZipFile.setPassphrase(SecurityUtil.SHA1(passphrase));
                                }
                            } catch (Exception e) {
                                userZipFile.setPassphrase(SecurityUtil.SHA1(passphrase));
                            }
                            userZipFile.setEmail(email);

                            // make sure the passphrase was correct - should always be
                            if (userZipFile.getCertificate() == null) {
                                throw new Exception("There was a problem loading your user zip file.");
                            }

                            // check not yet valid
                            if (userZipFile.isNotYetValid()) {
                                throw new Exception("The user zip file retrieved is not yet valid.");
                            }

                            // check expired
                            if (userZipFile.isExpired()) {
                                throw new Exception("The user zip file retrieved is expired.");
                            }

                            return userZipFile;
                        }
                    }
                }

                // fallen through when login failed
                for (Header header : pm.getResponseHeaders("User Approved")) {
                    if (header.getName().equals("User Approved")) {
                        if (header.getValue().equals("true")) {
                            throw new Exception("The password you provided is incorrect.");
                        } else if (header.getValue().equals("false")) {
                            throw new Exception("Your account has not yet been approved.");
                        }
                    }
                }

                throw new InvalidSignInException();
            }
        } finally {
            pm.releaseConnection();
        }
    }
}
