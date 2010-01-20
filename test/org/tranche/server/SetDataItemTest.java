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
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SetDataItemTest extends TrancheTestCase {

    public void testDoAction() throws Exception {
        FlatFileTrancheServer ffts = null;
        File dir = null;
        Server s = null;
        try {
            // create a local server
            dir = TempFileUtil.createTemporaryDirectory();
            ffts = new FlatFileTrancheServer(dir);
            ffts.getConfiguration().addUser(DevUtil.getDevUser());
            s = new Server(ffts, 1500);
            s.start();

            // wait for startup
            ffts.waitToLoadExistingDataBlocks();
            s.waitForStartup(1000);

            // make a meta data to set
            byte[] bytesToUpload = DevUtil.createRandomDataChunk1MB();
            BigHash hash = new BigHash(bytesToUpload);
            String[] hosts = new String[1];
            hosts[0] = s.getHostName();
            // sign nonce + hash + data
            byte[] bytes = new byte[BigHash.HASH_LENGTH + bytesToUpload.length];
            byte[] hashBytes = hash.toByteArray();
            System.arraycopy(hashBytes, 0, bytes, 0, hashBytes.length);
            System.arraycopy(bytesToUpload, 0, bytes, hashBytes.length, bytesToUpload.length);
            // sign the bytes
            String algorithm = SecurityUtil.getSignatureAlgorithm(DevUtil.getDevPrivateKey());
            byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(bytes), DevUtil.getDevPrivateKey(), algorithm);
            // make a proper signature object
            Signature signature = new Signature(sig, algorithm, DevUtil.getDevAuthority());

            // create the item
            SetDataItem item = new SetDataItem(s);

            // create the bytes
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            SetDataItem.writeRequest(false, hash, hosts, bytesToUpload, signature, request);

            // execute
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            item.doAction(new ByteArrayInputStream(request.toByteArray()), response, "localhost");

            // verify
            ByteArrayInputStream bais = new ByteArrayInputStream(response.toByteArray());
            assertEquals(Token.OK_STRING, RemoteUtil.readLine(bais));
            PropagationReturnWrapper wrapper = PropagationReturnWrapper.createFromBytes(RemoteUtil.readDataBytes(bais));
            assertEquals(0, wrapper.getErrors().size());
            assertTrue(IOUtil.hasData(ffts, hash));
        } finally {
            IOUtil.safeClose(s);
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDelete(dir);
        }
    }
}
