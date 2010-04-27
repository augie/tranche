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
package org.tranche.network;

import java.util.Collection;
import org.tranche.commons.DebugUtil;
import org.tranche.hash.span.HashSpan;
import org.tranche.hash.span.HashSpanCollection;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class NetworkPackageTestCase extends TrancheTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        DebugUtil.setDebug(NetworkUtil.class, true);
        DebugUtil.setDebug(ConnectionUtil.class, true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        DebugUtil.setDebug(NetworkUtil.class, false);
        DebugUtil.setDebug(ConnectionUtil.class, false);
    }

    public void assertEquals(StatusTableRow str, String host, String name, String group, int port, long updateTimestamp, boolean ssl, boolean online, boolean readOnly, boolean writeOnly, boolean dataStore, Collection<HashSpan> hashSpans) throws Exception {
        if (str == null) {
            fail("Object is null");
        }
        assertEquals(host, str.getHost());
        assertEquals(name, str.getName());
        assertEquals(group, str.getGroup());
        assertEquals(port, str.getPort());
        assertEquals(updateTimestamp, str.getUpdateTimestamp());
        assertEquals(ssl, str.isSSL());
        assertEquals(online, str.isOnline());
        assertEquals(readOnly, str.isReadable());
        assertEquals(writeOnly, str.isWritable());
        assertEquals(dataStore, str.isDataStore());
        assertEquals(hashSpans.size(), str.getHashSpans().size());
        if (!HashSpanCollection.areEqual(hashSpans, str.getHashSpans())) {
            fail("Hash span collections are inequal.");
        }
    }
}
