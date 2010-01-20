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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.hash.span.HashSpanCollection;
import org.tranche.util.DevUtil;
import org.tranche.util.RandomUtil;
import org.tranche.util.TestUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class StatusTableRowTest extends NetworkPackageTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        StatusTableRow.setDebug(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        StatusTableRow.setDebug(false);
    }

    public void testRecreate() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testRecreate()");
        
        // create
        StatusTableRow str1 = NetworkRandomUtil.createRandomStatusTableRow();

        // recreate
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        str1.serialize(oos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        StatusTableRow str = new StatusTableRow(ois);

        // verify
        assertEquals(str1, str);
    }
    
    public void testRecreateWithTargetHashSpan() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testRecreateWithTargetHashSpan()");
        
        // create
        StatusTableRow str1 = NetworkRandomUtil.createRandomStatusTableRow(true);

        // recreate
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        str1.serialize(oos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        StatusTableRow str = new StatusTableRow(ois);

        // verify
        assertEquals(str1, str);
    }

    public void testHost() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testHost()");

        // variables
        String host = "host";

        // set
        StatusTableRow str = new StatusTableRow(host);

        // verify
        assertEquals(host, str.getHost());

        // verify
        assertEquals(host, str.getHost());
    }

    public void testName() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testName()");

        // variables
        String name = "name";

        // set
        StatusTableRow str = new StatusTableRow(RandomUtil.getString(10));
        str.setName(name);

        // verify
        assertEquals(name, str.getName());

        // verify
        assertEquals(name, str.getName());
    }

    public void testGroup() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testGroup()");

        // variables
        String group = "group";

        // set
        StatusTableRow str = new StatusTableRow(RandomUtil.getString(10));
        str.setGroup(group);

        // verify
        assertEquals(group, str.getGroup());

        // verify
        assertEquals(group, str.getGroup());
    }

    public void testPort() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testPort()");

        // variables
        int port = 500;

        // set
        StatusTableRow str = new StatusTableRow(RandomUtil.getString(10));
        str.setPort(port);

        // verify
        assertEquals(port, str.getPort());
    }

    public void testIsSSL() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testIsSSL()");

        // variables
        boolean ssl = true;

        // set
        StatusTableRow str = new StatusTableRow(RandomUtil.getString(10));
        str.setIsSSL(ssl);

        // verify
        assertEquals(ssl, str.isSSL());
    }

    public void testIsOnline() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testIsOnline()");

        // variables
        boolean online = true;

        // set
        StatusTableRow str = new StatusTableRow(RandomUtil.getString(10));
        str.setIsOnline(online);

        // verify
        assertEquals(online, str.isOnline());
    }

    public void testIsReadable() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testIsReadable()");

        // variables
        boolean readable = true;

        // set
        StatusTableRow str = new StatusTableRow(RandomUtil.getString(10));
        str.setIsReadable(readable);

        // verify
        assertEquals(readable, str.isReadable());
    }

    public void testIsWritable() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testIsWritable()");

        // variables
        boolean writeable = true;

        // set
        StatusTableRow str = new StatusTableRow(RandomUtil.getString(10));
        str.setIsWritable(writeable);

        // verify
        assertEquals(writeable, str.isWritable());
    }

    public void testIsDataStore() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testIsDataStore()");

        // variables
        boolean dataStore = true;

        // set
        StatusTableRow str = new StatusTableRow(RandomUtil.getString(10));
        str.setIsDataStore(dataStore);

        // verify
        assertEquals(dataStore, str.isDataStore());
    }

    public void testHashSpans() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testHashSpans()");

        // variables
        Set<HashSpan> hashSpans = DevUtil.createRandomHashSpanSet(10);

        // set
        StatusTableRow str = new StatusTableRow(RandomUtil.getString(10));
        str.setHashSpans(hashSpans);

        // verify
        assertEquals(hashSpans.size(), str.getHashSpans().size());

        // verify
        HashSpanCollection.areEqual(hashSpans, str.getHashSpans());
    }
    
    public void testTargetHashSpan() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testTargetHashSpan()");

        // variables
        HashSpan targetHashSpan = DevUtil.createRandomHashSpan();
        Set<HashSpan> targetHashSpans = new HashSet();
        targetHashSpans.add(targetHashSpan);

        // set
        StatusTableRow str = new StatusTableRow(RandomUtil.getString(10));
        
        str.setTargetHashSpans(targetHashSpans);

        // verify
//        assertEquals(targetHashSpan, str.getTargetHashSpan());
        assertEquals(targetHashSpans.size(), str.getTargetHashSpans().size());
        for (HashSpan hs : targetHashSpans) {
            assertTrue("Should contain.", str.getTargetHashSpans().contains(hs));
        }
        
        for (HashSpan hs : str.getTargetHashSpans()) {
            assertTrue("Should contain.", targetHashSpans.contains(hs));
        }
    }

    public void testCalculateFullHashSpan() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testCalculateFullHashSpan()");

        // positive
        {
            // single row with one hash span
            {
                Collection<StatusTableRow> rows = new HashSet<StatusTableRow>();
                StatusTableRow row = new StatusTableRow(RandomUtil.getString(10));
                row.setIsOnline(true);
                row.setIsReadable(true);
                row.setIsWritable(true);
                // make this a core server
                Collection<String> startupServerURLs = new HashSet<String>();
                startupServerURLs.add(row.getURL());
                NetworkUtil.setStartupServerURLs(startupServerURLs);
                Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));
                row.setHashSpans(hashSpans);
                rows.add(row);

                StatusTableRow[] fullHashSpan = StatusTableRow.calculateFullHashSpan(rows).toArray(new StatusTableRow[0]);

                // verify
                assertEquals(1, fullHashSpan.length);
                assertEquals(fullHashSpan[0], row);
            }
            // single row with multiple hash spans
            {
                // run this one 3 times -- had random failures
                for (int i = 0; i < 3; i++) {
                    Collection<StatusTableRow> rows = new HashSet<StatusTableRow>();
                    StatusTableRow row = new StatusTableRow(RandomUtil.getString(10));
                    row.setIsOnline(true);
                    row.setIsReadable(true);
                    row.setIsWritable(true);
                    // make this a core server
                    Collection<String> startupServerURLs = new HashSet<String>();
                    startupServerURLs.add(row.getURL());
                    NetworkUtil.setStartupServerURLs(startupServerURLs);
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    BigHash middleHash = DevUtil.getRandomBigHash();
                    hashSpans.add(new HashSpan(HashSpan.FIRST, middleHash.getNext()));
                    hashSpans.add(new HashSpan(middleHash.getPrevious(), HashSpan.LAST));
                    hashSpans.add(new HashSpan(DevUtil.getRandomBigHash(), DevUtil.getRandomBigHash()));
                    row.setHashSpans(hashSpans);
                    rows.add(row);

                    StatusTableRow[] fullHashSpan = StatusTableRow.calculateFullHashSpan(rows).toArray(new StatusTableRow[0]);

                    // verify
                    assertEquals(1, fullHashSpan.length);
                    assertEquals(fullHashSpan[0], row);
                }
            }
            // multiple rows with one hash span
            {
                // two possibilities -- either the first one or the second two
                Collection<StatusTableRow> rows = new HashSet<StatusTableRow>();
                StatusTableRow row1 = new StatusTableRow(RandomUtil.getString(10));
                row1.setIsOnline(true);
                row1.setIsReadable(true);
                row1.setIsWritable(true);
                // make this a core server
                Collection<String> startupServerURLs = new HashSet<String>();
                startupServerURLs.add(row1.getURL());
                {
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));
                    row1.setHashSpans(hashSpans);
                    rows.add(row1);
                }
                StatusTableRow row2 = new StatusTableRow(RandomUtil.getString(10));
                row2.setIsOnline(true);
                row2.setIsReadable(true);
                row2.setIsWritable(true);
                startupServerURLs.add(row2.getURL());
                StatusTableRow row3 = new StatusTableRow(RandomUtil.getString(10));
                row3.setIsOnline(true);
                row3.setIsReadable(true);
                row3.setIsWritable(true);
                startupServerURLs.add(row3.getURL());
                BigHash middleHash = DevUtil.getRandomBigHash();
                {
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    hashSpans.add(new HashSpan(HashSpan.FIRST, middleHash));
                    row2.setHashSpans(hashSpans);
                    rows.add(row2);
                }
                {
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    hashSpans.add(new HashSpan(middleHash.getNext(), HashSpan.LAST));
                    row3.setHashSpans(hashSpans);
                    rows.add(row3);
                }
                NetworkUtil.setStartupServerURLs(startupServerURLs);

                StatusTableRow[] fullHashSpan = StatusTableRow.calculateFullHashSpan(rows).toArray(new StatusTableRow[0]);

                // verify one or the other
                if (!(fullHashSpan.length == 1 || fullHashSpan.length == 2)) {
                    fail("Full hash span should be achieved with one or two rows.");
                }
                if (fullHashSpan.length == 1) {
                    assertEquals(row1, fullHashSpan[0]);
                } else if (fullHashSpan.length == 2) {
                    if (fullHashSpan[0].getHost().equals(row2.getHost())) {
                        assertEquals(row2, fullHashSpan[0]);
                        assertEquals(row3, fullHashSpan[1]);
                    } else {
                        assertEquals(row3, fullHashSpan[0]);
                        assertEquals(row2, fullHashSpan[1]);
                    }
                }
            }
            // multiple rows with multiple hash spans
            {
                // two possibilities -- either the first one or the second two
                Collection<StatusTableRow> rows = new HashSet<StatusTableRow>();
                StatusTableRow row1 = new StatusTableRow(RandomUtil.getString(10));
                row1.setIsOnline(true);
                row1.setIsReadable(true);
                row1.setIsWritable(true);
                // make this a core server
                Collection<String> startupServerURLs = new HashSet<String>();
                startupServerURLs.add(row1.getURL());
                {
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    BigHash middleHash = DevUtil.getRandomBigHash();
                    hashSpans.add(new HashSpan(HashSpan.FIRST, middleHash.getNext()));
                    hashSpans.add(new HashSpan(middleHash.getPrevious(), HashSpan.LAST));
                    hashSpans.add(new HashSpan(DevUtil.getRandomBigHash(), DevUtil.getRandomBigHash()));
                    row1.setHashSpans(hashSpans);
                    rows.add(row1);
                }
                StatusTableRow row2 = new StatusTableRow(RandomUtil.getString(10));
                row2.setIsOnline(true);
                row2.setIsReadable(true);
                row2.setIsWritable(true);
                startupServerURLs.add(row2.getURL());
                StatusTableRow row3 = new StatusTableRow(RandomUtil.getString(10));
                row3.setIsOnline(true);
                row3.setIsReadable(true);
                row3.setIsWritable(true);
                startupServerURLs.add(row3.getURL());
                BigHash middleHash = DevUtil.getRandomBigHash();
                {
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    BigHash subMiddleHash = DevUtil.getRandomBigHash();
                    hashSpans.add(new HashSpan(HashSpan.FIRST, subMiddleHash));
                    hashSpans.add(new HashSpan(subMiddleHash.getPrevious(), middleHash));
                    row2.setHashSpans(hashSpans);
                    rows.add(row2);
                }
                {
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    BigHash subMiddleHash = DevUtil.getRandomBigHash();
                    hashSpans.add(new HashSpan(middleHash, subMiddleHash));
                    hashSpans.add(new HashSpan(subMiddleHash.getNext(), HashSpan.LAST));
                    row3.setHashSpans(hashSpans);
                    rows.add(row3);
                }
                NetworkUtil.setStartupServerURLs(startupServerURLs);

                StatusTableRow[] fullHashSpan = StatusTableRow.calculateFullHashSpan(rows).toArray(new StatusTableRow[0]);

                // verify one or the other
                if (!(fullHashSpan.length == 1 || fullHashSpan.length == 2)) {
                    fail("Full hash span should be achieved with one or two rows.");
                }
                if (fullHashSpan.length == 1) {
                    assertEquals(row1, fullHashSpan[0]);
                } else if (fullHashSpan.length == 2) {
                    if (fullHashSpan[0].getHost().equals(row2.getHost())) {
                        assertEquals(row2, fullHashSpan[0]);
                        assertEquals(row3, fullHashSpan[1]);
                    } else {
                        assertEquals(row3, fullHashSpan[0]);
                        assertEquals(row2, fullHashSpan[1]);
                    }
                }
            }
        }
    }

    public void testCalculateFullHashSpanWithSeeds() throws Exception {
        TestUtil.printTitle("StatusTableRowTest:testCalculateFullHashSpanWithSeeds()");

        // positive
        {
            // make sure always contains the seeds
            {
                for (int i = 0; i < 5; i++) {
                    Collection<StatusTableRow> seeds = new HashSet<StatusTableRow>();
                    BigHash middleHash = DevUtil.getRandomBigHash();
                    StatusTableRow seed = new StatusTableRow(RandomUtil.getString(10));
                    seed.setIsOnline(true);
                    seed.setIsReadable(true);
                    seed.setIsWritable(true);
                    // make this a core server
                    Collection<String> startupServerURLs = new HashSet<String>();
                    startupServerURLs.add(seed.getURL());
                    {
                        Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                        hashSpans.add(new HashSpan(HashSpan.FIRST, middleHash.getNext()));
                        seed.setHashSpans(hashSpans);
                    }
                    seeds.add(seed);

                    Collection<StatusTableRow> rows = new HashSet<StatusTableRow>();
                    {
                        StatusTableRow row = new StatusTableRow(RandomUtil.getString(10));
                        row.setIsOnline(true);
                        row.setIsReadable(true);
                        row.setIsWritable(true);
                        startupServerURLs.add(row.getURL());
                        Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                        hashSpans.add(new HashSpan(middleHash, HashSpan.LAST));
                        row.setHashSpans(hashSpans);
                        seeds.add(row);
                    }
                    // some more random rows
                    for (int j = 0; j < 10; j++) {
                        StatusTableRow row = new StatusTableRow(RandomUtil.getString(10));
                        row.setIsOnline(true);
                        row.setIsReadable(true);
                        row.setIsWritable(true);
                        startupServerURLs.add(row.getURL());
                        Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                        for (int k = 0; k < 5; k++) {
                            hashSpans.add(new HashSpan(DevUtil.getRandomBigHash(), DevUtil.getRandomBigHash()));
                        }
                        row.setHashSpans(hashSpans);
                        seeds.add(row);
                    }
                    NetworkUtil.setStartupServerURLs(startupServerURLs);

                    Collection<StatusTableRow> fullHashSpan = StatusTableRow.calculateFullHashSpan(seeds, rows);

                    // verify
                    boolean contains = false;
                    for (StatusTableRow row : fullHashSpan) {
                        if (row.equals(seed)) {
                            contains = true;
                            break;
                        }
                    }

                    if (!contains) {
                        fail("Full hash span collection did not contain seed row.");
                    }
                }
            }
            // single row with one hash span
            {
                Collection<StatusTableRow> seeds = new HashSet<StatusTableRow>();
                StatusTableRow row = new StatusTableRow(RandomUtil.getString(10));
                row.setIsOnline(true);
                row.setIsReadable(true);
                row.setIsWritable(true);
                // make this a core server
                Collection<String> startupServerURLs = new HashSet<String>();
                startupServerURLs.add(row.getURL());
                NetworkUtil.setStartupServerURLs(startupServerURLs);
                Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));
                row.setHashSpans(hashSpans);
                seeds.add(row);

                StatusTableRow[] fullHashSpan = StatusTableRow.calculateFullHashSpan(seeds, null).toArray(new StatusTableRow[0]);

                // verify
                assertEquals(1, fullHashSpan.length);
                assertEquals(fullHashSpan[0], row);
            }
            // single row with multiple hash spans
            {
                // run this one 3 times -- had random failures
                for (int i = 0; i < 3; i++) {
                    Collection<StatusTableRow> seeds = new HashSet<StatusTableRow>();
                    StatusTableRow row = new StatusTableRow(RandomUtil.getString(10));
                    row.setIsOnline(true);
                    row.setIsReadable(true);
                    row.setIsWritable(true);
                    // make this a core server
                    Collection<String> startupServerURLs = new HashSet<String>();
                    startupServerURLs.add(row.getURL());
                    NetworkUtil.setStartupServerURLs(startupServerURLs);
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    BigHash middleHash = DevUtil.getRandomBigHash();
                    hashSpans.add(new HashSpan(HashSpan.FIRST, middleHash.getNext()));
                    hashSpans.add(new HashSpan(middleHash.getPrevious(), HashSpan.LAST));
                    hashSpans.add(new HashSpan(DevUtil.getRandomBigHash(), DevUtil.getRandomBigHash()));
                    row.setHashSpans(hashSpans);
                    seeds.add(row);

                    StatusTableRow[] fullHashSpan = StatusTableRow.calculateFullHashSpan(seeds, null).toArray(new StatusTableRow[0]);

                    // verify
                    assertEquals(1, fullHashSpan.length);
                    assertEquals(fullHashSpan[0], row);
                }
            }
            // multiple rows with one hash span
            {
                // two possibilities -- either the first one or the second two
                Collection<StatusTableRow> seeds = new HashSet<StatusTableRow>();
                StatusTableRow row1 = new StatusTableRow(RandomUtil.getString(10));
                row1.setIsOnline(true);
                row1.setIsReadable(true);
                row1.setIsWritable(true);
                // make this a core server
                Collection<String> startupServerURLs = new HashSet<String>();
                startupServerURLs.add(row1.getURL());
                {
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    hashSpans.add(new HashSpan(HashSpan.FIRST, HashSpan.LAST));
                    row1.setHashSpans(hashSpans);
                    seeds.add(row1);
                }
                StatusTableRow row2 = new StatusTableRow(RandomUtil.getString(10));
                row2.setIsOnline(true);
                row2.setIsReadable(true);
                row2.setIsWritable(true);
                startupServerURLs.add(row2.getURL());
                StatusTableRow row3 = new StatusTableRow(RandomUtil.getString(10));
                row3.setIsOnline(true);
                row3.setIsReadable(true);
                row3.setIsWritable(true);
                startupServerURLs.add(row3.getURL());
                BigHash middleHash = DevUtil.getRandomBigHash();
                {
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    hashSpans.add(new HashSpan(HashSpan.FIRST, middleHash));
                    row2.setHashSpans(hashSpans);
                    seeds.add(row2);
                }
                {
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    hashSpans.add(new HashSpan(middleHash.getNext(), HashSpan.LAST));
                    row3.setHashSpans(hashSpans);
                    seeds.add(row3);
                }
                NetworkUtil.setStartupServerURLs(startupServerURLs);

                StatusTableRow[] fullHashSpan = StatusTableRow.calculateFullHashSpan(seeds, null).toArray(new StatusTableRow[0]);

                // verify one or the other
                if (!(fullHashSpan.length == 1 || fullHashSpan.length == 2)) {
                    fail("Full hash span should be achieved with one or two rows.");
                }
                if (fullHashSpan.length == 1) {
                    assertEquals(row1, fullHashSpan[0]);
                } else if (fullHashSpan.length == 2) {
                    if (fullHashSpan[0].getHost().equals(row2.getHost())) {
                        assertEquals(row2, fullHashSpan[0]);
                        assertEquals(row3, fullHashSpan[1]);
                    } else {
                        assertEquals(row3, fullHashSpan[0]);
                        assertEquals(row2, fullHashSpan[1]);
                    }
                }
            }
            // multiple rows with multiple hash spans
            {
                // two possibilities -- either the first one or the second two
                Collection<StatusTableRow> seeds = new HashSet<StatusTableRow>();
                StatusTableRow row1 = new StatusTableRow(RandomUtil.getString(10));
                row1.setIsOnline(true);
                row1.setIsReadable(true);
                row1.setIsWritable(true);
                // make this a core server
                Collection<String> startupServerURLs = new HashSet<String>();
                startupServerURLs.add(row1.getURL());
                {
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    BigHash middleHash = DevUtil.getRandomBigHash();
                    hashSpans.add(new HashSpan(HashSpan.FIRST, middleHash.getNext()));
                    hashSpans.add(new HashSpan(middleHash.getPrevious(), HashSpan.LAST));
                    hashSpans.add(new HashSpan(DevUtil.getRandomBigHash(), DevUtil.getRandomBigHash()));
                    row1.setHashSpans(hashSpans);
                    seeds.add(row1);
                }
                StatusTableRow row2 = new StatusTableRow(RandomUtil.getString(10));
                row2.setIsOnline(true);
                row2.setIsReadable(true);
                row2.setIsWritable(true);
                startupServerURLs.add(row2.getURL());
                StatusTableRow row3 = new StatusTableRow(RandomUtil.getString(10));
                row3.setIsOnline(true);
                row3.setIsReadable(true);
                row3.setIsWritable(true);
                startupServerURLs.add(row3.getURL());
                BigHash middleHash = DevUtil.getRandomBigHash();
                {
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    BigHash subMiddleHash = DevUtil.getRandomBigHash();
                    hashSpans.add(new HashSpan(HashSpan.FIRST, subMiddleHash));
                    hashSpans.add(new HashSpan(subMiddleHash.getPrevious(), middleHash));
                    row2.setHashSpans(hashSpans);
                    seeds.add(row2);
                }
                {
                    Collection<HashSpan> hashSpans = new HashSet<HashSpan>();
                    BigHash subMiddleHash = DevUtil.getRandomBigHash();
                    hashSpans.add(new HashSpan(middleHash, subMiddleHash));
                    hashSpans.add(new HashSpan(subMiddleHash.getNext(), HashSpan.LAST));
                    row3.setHashSpans(hashSpans);
                    seeds.add(row3);
                }
                NetworkUtil.setStartupServerURLs(startupServerURLs);

                StatusTableRow[] fullHashSpan = StatusTableRow.calculateFullHashSpan(seeds, null).toArray(new StatusTableRow[0]);

                // verify one or the other
                if (!(fullHashSpan.length == 1 || fullHashSpan.length == 2)) {
                    fail("Full hash span should be achieved with one or two rows.");
                }
                if (fullHashSpan.length == 1) {
                    assertEquals(row1, fullHashSpan[0]);
                } else if (fullHashSpan.length == 2) {
                    if (fullHashSpan[0].getHost().equals(row2.getHost())) {
                        assertEquals(row2, fullHashSpan[0]);
                        assertEquals(row3, fullHashSpan[1]);
                    } else {
                        assertEquals(row3, fullHashSpan[0]);
                        assertEquals(row2, fullHashSpan[1]);
                    }
                }
            }
        }
    }
}
