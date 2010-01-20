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
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataUtil;
import org.tranche.remote.RemoteUtil;
import org.tranche.remote.Token;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetProjectHashesItemTest extends TrancheTestCase {

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

            // create fake data chunks
            int numChunks = 20;
            for (int i = 0; i < numChunks; i++) {
                MetaData md = DevUtil.createRandomMetaData();
                md.setIsProjectFile(true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                MetaDataUtil.write(md, baos);
                byte[] bytes = baos.toByteArray();
                BigHash hash = new BigHash(bytes);
                IOUtil.setMetaData(ffts, DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, bytes);
            }

            // wait for startup
            ffts.waitToLoadExistingDataBlocks();
            s.waitForStartup(1000);

            // create the item
            GetProjectHashesItem item = new GetProjectHashesItem(s);

            // all
            {
                // create the bytes
                ByteArrayOutputStream request = new ByteArrayOutputStream();
                GetProjectHashesItem.writeRequest(false, 0, numChunks, request);

                // execute
                ByteArrayOutputStream response = new ByteArrayOutputStream();
                item.doAction(new ByteArrayInputStream(request.toByteArray()), response, "localhost");

                // verify
                ByteArrayInputStream in = new ByteArrayInputStream(response.toByteArray());
                assertEquals(Token.OK_STRING, RemoteUtil.readLine(in));
                BigHash[] responseHashes = RemoteUtil.readBigHashArray(in);
                assertEquals(numChunks, responseHashes.length);
            }

            // part
            {
                // create the bytes
                ByteArrayOutputStream request = new ByteArrayOutputStream();
                GetProjectHashesItem.writeRequest(false, numChunks / 2, numChunks, request);

                // execute
                ByteArrayOutputStream response = new ByteArrayOutputStream();
                item.doAction(new ByteArrayInputStream(request.toByteArray()), response, "localhost");

                // verify
                ByteArrayInputStream in = new ByteArrayInputStream(response.toByteArray());
                assertEquals(Token.OK_STRING, RemoteUtil.readLine(in));
                BigHash[] responseHashes = RemoteUtil.readBigHashArray(in);
                assertEquals(numChunks / 2, responseHashes.length);
            }
        } finally {
            IOUtil.safeClose(s);
            IOUtil.safeClose(ffts);
            IOUtil.recursiveDelete(dir);
        }
    }
}
