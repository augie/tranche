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
package org.tranche.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import org.tranche.security.Signature;
import org.tranche.configuration.ConfigurationUtil;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.users.User;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SetConfigurationItemTest extends TrancheTestCase {

    public void testDoAction() throws Exception {
        FlatFileTrancheServer ffts = null;
        File dir = null;
        Server s = null;
        try {
            // create a local server
            dir = TempFileUtil.createTemporaryDirectory();
            ffts = new FlatFileTrancheServer(dir);
            User devUser = DevUtil.getDevUser();
            devUser.setFlags(User.ALL_PRIVILEGES);
            ffts.getConfiguration().addUser(devUser);
            s = new Server(ffts, 1500);
            s.start();

            // wait for startup
            ffts.waitToLoadExistingDataBlocks();
            s.waitForStartup(1000);

            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                ConfigurationUtil.write(ffts.getConfiguration(), baos);
            } finally {
                IOUtil.safeClose(baos);
            }
            byte[] configBytes = baos.toByteArray();
            byte[] nonce = IOUtil.getNonce(ffts);

            // make the bytes
            byte[] bytes = new byte[configBytes.length + nonce.length];
            System.arraycopy(configBytes, 0, bytes, 0, configBytes.length);
            System.arraycopy(nonce, 0, bytes, configBytes.length, nonce.length);
            // sign the bytes
            String algorithm = SecurityUtil.getSignatureAlgorithm(DevUtil.getDevPrivateKey());
            byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(bytes), DevUtil.getDevPrivateKey(), algorithm);
            // make a proper signature object
            Signature signature = new Signature(sig, algorithm, DevUtil.getDevAuthority());

            // create the item
            SetConfigurationItem item = new SetConfigurationItem(s);

            // create the bytes
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            SetConfigurationItem.writeRequest(false, configBytes, signature, nonce, request);

            // execute
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            item.doAction(new ByteArrayInputStream(request.toByteArray()), response, "localhost");

            // verify
            assertEquals(Token.OK_STRING, RemoteUtil.readLine(new ByteArrayInputStream(response.toByteArray())));
        } finally {
            IOUtil.safeClose(s);
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDelete(dir);
        }
    }
}
