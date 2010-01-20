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
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetMetaDataItemTest extends TrancheTestCase {

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

            // create some fake data chunks
            int numHashes = RandomUtil.getInt(10) + 1;
            int bogusHashes = RandomUtil.getInt(10) + 1;
            int numRequestedHashes = numHashes + bogusHashes;
            BigHash hashes[] = new BigHash[numRequestedHashes];
            for (int i = 0; i < numHashes; i++) {
                byte[] bytes = DevUtil.createRandomMetaDataChunk();
                hashes[i] = new BigHash(bytes);
                IOUtil.setMetaData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hashes[i], bytes);
            }
            for (int i = numHashes; i < numRequestedHashes; i++) {
                hashes[i] = DevUtil.getRandomBigHash();
            }

            // create the item
            GetMetaDataItem item = new GetMetaDataItem(s);

            // create the bytes
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            GetMetaDataItem.writeRequest(false, hashes, false, request);

            // execute
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            item.doAction(new ByteArrayInputStream(request.toByteArray()), response, "localhost");

            // verify
            ByteArrayInputStream in = new ByteArrayInputStream(response.toByteArray());
            assertEquals(Token.OK_STRING, RemoteUtil.readLine(in));
            PropagationReturnWrapper responseObject = PropagationReturnWrapper.createFromBytes(RemoteUtil.readDataBytes(in));
            assertEquals(responseObject.getErrors().size(), bogusHashes);
            byte[][] responseByte2DArray = (byte[][]) responseObject.getReturnValueObject();
            assertEquals(numRequestedHashes, responseByte2DArray.length);
            for (int i = 0; i < numHashes; i++) {
                assertEquals(hashes[i], new BigHash(responseByte2DArray[i]));
            }
            for (int i = numHashes; i < numRequestedHashes; i++) {
                assertNull(responseByte2DArray[i]);
            }
        } finally {
            IOUtil.safeClose(s);
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDelete(dir);
        }
    }
}
