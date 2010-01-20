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

import org.tranche.exceptions.TodoException;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class NonceItemTest extends TrancheTestCase {

    public void testToDo() throws Exception {
        throw new TodoException("Froze the dev tests.");
    }

//    public void testDoAction() throws Exception {
//        FlatFileTrancheServer ffts = null;
//        File dir = null;
//        Server s = null;
//        try {
//            // create a local server
//            dir = TempFileUtil.createTemporaryDirectory();
//            ffts = new FlatFileTrancheServer(dir);
//            ffts.getConfiguration().addUser(DevUtil.getDevUser());
//            s = new Server(ffts, 1500);
//            s.start();
//
//            // wait for startup
//            ffts.waitToLoadExistingDataBlocks();
//            s.waitForStartup(1000);
//
//            int count = 25;
//            Set <StatusTableRow> rows = new HashSet<StatusTableRow>();
//            rows.add(new StatusTableRow(s));
//            NetworkUtil.getStatus().setRows(rows);
//            String[] hosts = new String[1];
//            hosts[0] = s.getHostName();
//
//            // create the item
//            NonceItem item = new NonceItem(s);
//
//            // create the bytes
//            ByteArrayOutputStream request = new ByteArrayOutputStream();
//            NonceItem.writeRequest(false, count, hosts, request);
//
//            // execute
//            ByteArrayOutputStream response = new ByteArrayOutputStream();
//            item.doAction(new ByteArrayInputStream(request.toByteArray()), response, "localhost");
//
//            // verify
//            ByteArrayInputStream in = new ByteArrayInputStream(response.toByteArray());
//            assertEquals(Token.OK_STRING, RemoteUtil.readLine(in));
//            PropagationReturnWrapper wrapper = PropagationReturnWrapper.createFromBytes(RemoteUtil.readDataBytes(in));
//            byte[][][] responseData = (byte[][][]) wrapper.getReturnValueObject();
//            assertEquals(1, responseData.length);
//            assertEquals(count, responseData[0].length);
//            for (int i = 0; i < count; i++) {
//                assertEquals(NonceMap.NONCE_BYTES, responseData[0][i].length);
//            }
//            assertEquals(0, wrapper.getErrors().size());
//        } finally {
//            IOUtil.safeClose(s);
//            IOUtil.safeClose(ffts);
//            IOUtil.recursiveDelete(dir);
//        }
//    }
}
