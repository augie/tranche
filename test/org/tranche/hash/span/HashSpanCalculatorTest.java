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
package org.tranche.hash.span;

import org.tranche.util.*;
import org.tranche.exceptions.TodoException;

/**
 * Test out the hash span calculator, asserting expected proportions and other conditions.
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class HashSpanCalculatorTest extends TrancheTestCase {
   
    public void testEven() throws Exception {
        // need to update to use new connection scheme
        if (true) {
            throw new TodoException();
        }
//        // Temporary storage
//        File tmpDir = TempFileUtil.createTemporaryDirectory();
//
//        List<FlatFileTrancheServer> ffservers = new ArrayList();
//        List<Server> servers = new ArrayList();
//        List<TrancheServer> rservers = new ArrayList();
//
//        UserZipFile userZip = null;
//
//        try {
//            userZip = createUser("test", "test", "delete.zip.encrypted", true);
//
//            assertTrue("Tmp dir should exist.", tmpDir.exists());
//
//            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//            // Set up servers
//            File ff;
//            for (int i = 0; i < 3; i++) {
//
//                ff = new File(tmpDir, "server" + i);
//                ff.mkdirs();
//
//                ffservers.add(new FlatFileTrancheServer(ff));
//                ffservers.get(i).getConfiguration().addUser(userZip);
//
//                servers.add(new Server(ffservers.get(i), DevUtil.getDefaultTestPort() + i));
//                servers.get(i).start();
//
//                rservers.add(IOUtil.connect("tranche://127.0.0.1:" + servers.get(i).getPort()));
//            }
//
//            for (TrancheServer ts : rservers) {
//                assertTrue("Server should be online.", ServerUtil.isServerOnline(IOUtil.createURL(ts)));
//            }
//
//            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//            // Use hash span calculator
//            HashSpanCalculator calc = new HashSpanCalculator(userZip.getCertificate());
//
//            assertEquals("Should be 1 replication set.", 1, calc.getReplications());
//
//            calc.setReplications(1);
//
//            assertEquals("Should still be 1 replication set.", 1, calc.getReplications());
//
//            for (TrancheServer ts : rservers) {
//                calc.addServer(IOUtil.createURL(ts));
//            }
//
//            // Get map of urls and hash spans
//            Map<String, Set<HashSpan>> spans = calc.calculateSpans();
//
//            // Build a list of just the sets of hash spans...
//            List<Set<HashSpan>> spansSets = new ArrayList();
//
//            for (TrancheServer ts : rservers) {
//                spansSets.add(spans.get(IOUtil.createURL(ts)));
//            }
//
//            // We know that each server only getting one hash span. Just get hash spans.
//            List<HashSpan> spansList = new ArrayList();
//
//            for (Set<HashSpan> nextSet : spansSets) {
//                for (HashSpan span : nextSet.toArray(new HashSpan[0])) {
//                    spansList.add(span);
//                }
//            }
//            assertEquals("Should be 3 spans.", 3, spansList.size());
//
//            byte expectedSize = (byte) ((Byte.MAX_VALUE + Math.abs(Byte.MIN_VALUE)) / 3);
//
//            // Calculate sizes
//            for (HashSpan next : spansList) {
//
//                byte first = next.getFirst().toByteArray()[0];
//                byte last = next.getLast().toByteArray()[0];
//
//                assertTrue("Each span should be roughly the same size.", expectedSize - 1 <= Math.abs(first - last) && expectedSize + 1 >= Math.abs(first - last));
//            }
//
//        } catch (Exception e) {
//            fail(e.getMessage());
//        } finally {
//
//            if (ffservers != null) {
//                for (FlatFileTrancheServer ffserver : ffservers) {
//                    IOUtil.safeClose(ffserver);
//                }
//            }
//            if (servers != null) {
//                for (Server server : servers) {
//                    IOUtil.safeClose(server);
//                }
//            }
//            if (rservers != null) {
//                for (TrancheServer rserver : rservers) {
//                    IOUtil.safeClose(rserver);
//                }
//            }
//            if (userZip != null && userZip.getFile().exists()) {
//                IOUtil.safeDelete(userZip.getFile());
//            }
//            if (tmpDir != null) {
//                IOUtil.recursiveDeleteWithWarning(tmpDir);
//            }
//        }

    }
     // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    // Simple test for uneven hash spans for three servers.
    public void testUneven() {
        // need to update to use new connection scheme
        if (true) {
            throw new TodoException();
        }
//
//        // Temporary storage
//        File tmpDir = null;
//
//        // Sizes for ffservers. Directly affects hash span size
//        final int[] storage = {1024, 1024 * 2, 1024 * 3};
//
//        List<FlatFileTrancheServer> ffservers = new ArrayList();
//        List<Server> servers = new ArrayList();
//        List<TrancheServer> rservers = new ArrayList();
//
//        UserZipFile userZip = null;
//
//        try {
//            userZip = createUser("test", "test", "delete.zip.encrypted", true);
//            tmpDir = TempFileUtil.createTemporaryDirectory();
//
//            assertTrue("Tmp dir should exist.", tmpDir.exists());
//
//            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//            // Set up servers
//            File ff;
//            for (int i = 0; i < 3; i++) {
//
//                ff = new File(tmpDir, "server" + i);
//                ff.mkdirs();
//
//                ffservers.add(new FlatFileTrancheServer(ff));
//                ffservers.get(i).getConfiguration().addUser(userZip);
//
//                // Different storage sizes for each server
//                DataDirectoryConfiguration conf = new DataDirectoryConfiguration(ff.getPath(), storage[i]);
//                Set confSet = new HashSet();
//                confSet.add(conf);
//                ffservers.get(i).getConfiguration().setDataDirectories(confSet);
//
//                servers.add(new Server(ffservers.get(i), DevUtil.getDefaultTestPort() + i));
//                servers.get(i).start();
//
//                rservers.add(IOUtil.connect("tranche://127.0.0.1:" + servers.get(i).getPort()));
//            }
//
//            for (TrancheServer ts : rservers) {
//                assertTrue("Server should be online.", ServerUtil.isServerOnline(IOUtil.createURL(ts)));
//            }
//
//            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//            // Use hash span calculator
//            HashSpanCalculator calc = new HashSpanCalculator(userZip.getCertificate(), 1);
//            assertEquals("Should be 1 replication set.", 1, calc.getReplications());
//
//            for (TrancheServer ts : rservers) {
//                calc.addServer(IOUtil.createURL(ts));
//            }
//
//            Map<String, Set<HashSpan>> spans = calc.calculateSpans();
//
//            // Get list of hash span sets
//            List<Set<HashSpan>> listOfSets = new ArrayList();
//
//            for (TrancheServer ts : rservers) {
//                listOfSets.add(spans.get(IOUtil.createURL(ts)));
//            }
//
//            // Extract hash spans. In this case, 1 span per server
//            List<HashSpan> spansList = new ArrayList();
//
//            for (Set<HashSpan> nextSet : listOfSets) {
//                for (HashSpan nextSpan : nextSet.toArray(new HashSpan[0])) {
//                    spansList.add(nextSpan);
//                }
//            }
//
//            assertEquals("Should be 3 spans.", 3, spansList.size());
//
//            // Calculate sizes
//            for (int i = 0; i < spansList.size(); i++) {
//
//                int first = spansList.get(i).getFirst().toByteArray()[0];
//                int last = spansList.get(i).getLast().toByteArray()[0];
//                int actualSize = Math.abs(last - first);
//
//                // Expected size is proportional to storage size
//                int expectedSize = (Byte.MAX_VALUE + Math.abs(Byte.MIN_VALUE)) / 6;
//                expectedSize *= (i + 1);
//
//                System.out.println("DEBUG> #"+i+", actual="+actualSize+", expected="+expectedSize);
//
//                assertTrue("Each span should be proportional to the total size. actualSize - 2 {"+(actualSize - 2)+"} <= expectedSize {"+expectedSize+"} && actualSize + 2 {"+(actualSize + 2)+"} >= expectedSize {"+expectedSize+"}", actualSize - 2 <= expectedSize && actualSize + 2 >= expectedSize);
//            }
//
//            // Test percentages
//            for (int i = 0; i < 3; i++) {
//
//                double percentage = calc.getServerPercentage(IOUtil.createURL(rservers.get(i)));
//                double lowerBound = (double) storage[i] / (double) (1024 * 6) - .01;
//                double upperBound = (double) storage[i] / (double) (1024 * 6) + .01;
//
//                assertTrue("Percentages should be darn close to actual proportion.", percentage < upperBound && percentage > lowerBound);
//            }
//
//            // Add 1000 hashes, verify approx. ratios
//            BigHash nextHash;
//            int countUrl1 = 0, countUrl2 = 0, countUrl3 = 0;
//            int totalCount = 0; // For the unlikely case of crossover
//            for (int i = 0; i < 1000; i++) {
//                byte[] someBytes = new byte[64];
//                RandomUtil.getBytes(someBytes);
//                nextHash = new BigHash(someBytes);
//
//                // It's possible, though extremely unlikely, that some hash
//                // may be in multiple spans
//                Set<HashSpan> set1 = spans.get(IOUtil.createURL(rservers.get(0)));
//                Set<HashSpan> set2 = spans.get(IOUtil.createURL(rservers.get(1)));
//                Set<HashSpan> set3 = spans.get(IOUtil.createURL(rservers.get(2)));
//
//                for (HashSpan hashSpan : set1) {
//                    if (hashSpan.contains(nextHash)) {
//                        countUrl1++;
//                        totalCount++;
//                    }
//                }
//                for (HashSpan hashSpan : set2) {
//                    if (hashSpan.contains(nextHash)) {
//                        countUrl2++;
//                        totalCount++;
//                    }
//                }
//                for (HashSpan hashSpan : set3) {
//                    if (hashSpan.contains(nextHash)) {
//                        countUrl3++;
//                        totalCount++;
//                    }
//                }
//            }
//
//            // Dynamically figure out storage ratios and match against
//            // count. Should be within 3%.
//            int totalSize = 0;
//            for (int size : storage) {
//                totalSize += size;
//            }
//            double expectedProp1 = (double) storage[0] / totalSize;
//            double expectedProp2 = (double) storage[1] / totalSize;
//            double expectedProp3 = (double) storage[2] / totalSize;
//
//            double realProp1 = (double) countUrl1 / totalCount;
//            double realProp2 = (double) countUrl2 / totalCount;
//            double realProp3 = (double) countUrl3 / totalCount;
//
//            assertTrue("Expecting certain proportion."+String.valueOf(expectedProp1 - .03)+" <= "+String.valueOf(realProp1)+" && "+String.valueOf(realProp1)+" <= "+String.valueOf(expectedProp1 + .03), expectedProp1 - .03 <= realProp1 && realProp1 <= expectedProp1 + .03);
//            assertTrue("Expecting certain proportion: "+String.valueOf(expectedProp2 - .03)+" <= "+String.valueOf(realProp2)+" && "+String.valueOf(realProp2)+" <= "+String.valueOf(expectedProp2 + .03), expectedProp2 - .03 <= realProp2 && realProp2 <= expectedProp2 + .03);
//            assertTrue("Expecting certain proportion."+String.valueOf(expectedProp3 - .03)+" <= "+String.valueOf(realProp3)+" && "+String.valueOf(realProp3)+" <= "+String.valueOf(expectedProp3 + .03), expectedProp3 - .03 <= realProp3 && realProp3 <= expectedProp3 + .03);
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//            e.printStackTrace(System.err);
//            fail(e.getMessage());
//        } finally {
//
//            if (ffservers != null) {
//                for (FlatFileTrancheServer ffserver : ffservers) {
//                    IOUtil.safeClose(ffserver);
//                }
//            }
//            if (servers != null) {
//                for (Server server : servers) {
//                    IOUtil.safeClose(server);
//                }
//            }
//            if (rservers != null) {
//                for (TrancheServer rserver : rservers) {
//                    IOUtil.safeClose(rserver);
//                }
//            }
//            if (userZip != null && userZip.getFile().exists()) {
//                IOUtil.safeDelete(userZip.getFile());
//            }
//            if (tmpDir != null) {
//                IOUtil.recursiveDeleteWithWarning(tmpDir);
//            }
//        }

    }
    
    public void testFull() {
        // need to update to use new connection scheme
        if (true) {
            throw new TodoException();
        }
//
//        // Temporary storage
//        File tmpDir = null;
//
//        // Sizes for ffservers. Shouldn't matter after asking for 4 replications.
//        final int[] storage = {1024, 1024 * 2, 1024 * 3};
//
//        List<FlatFileTrancheServer> ffservers = new ArrayList();
//        List<Server> servers = new ArrayList();
//        List<TrancheServer> rservers = new ArrayList();
//
//        UserZipFile userZip = null;
//
//        try {
//
//            userZip = createUser("test", "test", "delete.zip.encrypted", true);
//            tmpDir = TempFileUtil.createTemporaryDirectory();
//
//            assertTrue("Tmp dir should exist.", tmpDir.exists());
//
//            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//            // Set up servers
//            File ff;
//            for (int i = 0; i < 3; i++) {
//
//                ff = new File(tmpDir, "server" + i);
//                ff.mkdirs();
//
//                ffservers.add(new FlatFileTrancheServer(ff));
//                ffservers.get(i).getConfiguration().addUser(userZip);
//
//                // Different storage sizes for each server. Again, shouldn't matter.
//                DataDirectoryConfiguration conf = new DataDirectoryConfiguration(ff.getPath(), storage[i]);
//                Set confSet = new HashSet();
//                confSet.add(conf);
//                ffservers.get(i).getConfiguration().setDataDirectories(confSet);
//
//                servers.add(new Server(ffservers.get(i), DevUtil.getDefaultTestPort() + i));
//                servers.get(i).start();
//
//                rservers.add(IOUtil.connect("tranche://127.0.0.1:" + servers.get(i).getPort()));
//            }
//
//            for (TrancheServer ts : rservers) {
//                assertTrue("Server should be online.", ServerUtil.isServerOnline(IOUtil.createURL(ts)));
//            }
//
//            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//            // Use hash span calculator
//            HashSpanCalculator calc = new HashSpanCalculator(userZip.getCertificate(), 4);
//            assertEquals("Should be 4 replications set.", 4, calc.getReplications());
//
//            for (TrancheServer ts : rservers) {
//                calc.addServer(IOUtil.createURL(ts));
//            }
//
//            Map<String, Set<HashSpan>> spans = calc.calculateSpans();
//
//            // Get list of hash span sets
//            List<Set<HashSpan>> listOfSets = new ArrayList();
//
//            for (TrancheServer ts : rservers) {
//                listOfSets.add(spans.get(IOUtil.createURL(ts)));
//            }
//
//            // Extract hash spans. In this case, 1 span per server
//            List<HashSpan> spansList = new ArrayList();
//
//            for (Set<HashSpan> nextSet : listOfSets) {
//                for (HashSpan nextSpan : nextSet.toArray(new HashSpan[0])) {
//                    spansList.add(nextSpan);
//                }
//            }
//            assertEquals("Should be 3 spans.", 3, spansList.size());
//
//            // Each server should have full hash spans
//            for (int i = 0; i < spansList.size(); i++) {
//
//                BigHash first = spansList.get(i).getFirst();
//                BigHash last = spansList.get(i).getLast();
//
//                assertEquals("Each hash span should cover entire range, checking first", first, HashSpan.FIRST);
//                assertEquals("Each hash span should cover entire range, checking last", last, HashSpan.LAST);
//            }
//
//        } catch (Exception e) {
//            fail(e.getMessage());
//        } finally {
//
//            if (ffservers != null) {
//                for (FlatFileTrancheServer ffserver : ffservers) {
//                    IOUtil.safeClose(ffserver);
//                }
//            }
//            if (servers != null) {
//                for (Server server : servers) {
//                    IOUtil.safeClose(server);
//                }
//            }
//            if (rservers != null) {
//                for (TrancheServer rserver : rservers) {
//                    IOUtil.safeClose(rserver);
//                }
//            }
//            if (userZip != null && userZip.getFile().exists()) {
//                IOUtil.safeDelete(userZip.getFile());
//            }
//            if (tmpDir != null) {
//                IOUtil.recursiveDeleteWithWarning(tmpDir);
//            }
//        }

    }
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    // Try out different permutation for num. servers & num. repetitions
    public void test1() {
        testConditions(3, 2);
    }

    public void test2() {
        testConditions(6, 4);
    }

    public void test3() {
        testConditions(10, 2);
    }

    public void test4() {
        testConditions(10, 9);
    }

    public void test5() {
        testConditions(7, 3);
    }
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    // Helper method. Try out different num server and replications
    public void testConditions(int numServers, int numReplications) {
        // need to update to use new connection scheme
        if (true) {
            throw new TodoException();
        }
//
//        // Temporary storage
//        File tmpDir = null;
//
//        final String HOST = "tranche://127.0.0.1";
//
//        List<String> urls = new ArrayList();
//        List<FlatFileTrancheServer> ffservers = new ArrayList();
//        List<Server> servers = new ArrayList();
//        List<TrancheServer> rservers = new ArrayList();
//
//        UserZipFile userZip = null;
//
//        try {
//
//            userZip = createUser("test", "test", "delete.zip.encrypted", true);
//            tmpDir = TempFileUtil.createTemporaryDirectory();
//
//            assertTrue("Tmp dir should exist.", tmpDir.exists());
//
//            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//            // Set up servers
//            File ff;
//            for (int i = 0; i < numServers; i++) {
//
//                ff = new File(tmpDir, "server" + i);
//                ff.mkdirs();
//
//                // Use default data directory sizes
//                ffservers.add(new FlatFileTrancheServer(ff));
//                ffservers.get(i).getConfiguration().addUser(userZip);
//
//                servers.add(new Server(ffservers.get(i), DevUtil.getDefaultTestPort() + i));
//                servers.get(i).start();
//
//                urls.add(HOST + ":" + servers.get(i).getPort());
//
//                rservers.add(IOUtil.connect(urls.get(i)));
//            }
//
//            for (String url : urls) {
//                assertTrue("Server should be online.", ServerUtil.isServerOnline(url));            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//            // Use hash span calculator
//            }
//            HashSpanCalculator calc = new HashSpanCalculator(userZip.getCertificate(), numReplications);
//            assertEquals("Should be " + numReplications + " replications set.", numReplications, calc.getReplications());
//
//            // Add servers
//            calc.addServers(urls);
//
//            Map<String, Set<HashSpan>> spans = calc.calculateSpans();
//
//            // Get list of hash span sets
//            List<Set<HashSpan>> listOfSets = new ArrayList();
//
//            for (String url : urls) {
//                listOfSets.add(spans.get(url));            // Expected size is proportional to storage size
//            }
//            int expectedSize = numReplications * (Byte.MAX_VALUE + Math.abs(Byte.MIN_VALUE)) / numServers;
//
//            for (Set<HashSpan> nextSet : listOfSets) {
//
//                // The trick is that every hash span won't be the same size,
//                // but every collection of hash spans should.
//
//                if (nextSet.size() == 1) {
//
//                    HashSpan nextSpan = nextSet.toArray(new HashSpan[0])[0];
//
//                    int first = nextSpan.getFirst().toByteArray()[0];
//                    int last = nextSpan.getLast().toByteArray()[0];
//                    int actualSize = Math.abs(last - first);
//
//                    assertTrue("Each span should be proportional to the total size.", actualSize - numServers <= expectedSize && actualSize + numServers >= expectedSize);
//                } else {
//
//                    HashSpan span1 = nextSet.toArray(new HashSpan[0])[0];
//                    HashSpan span2 = nextSet.toArray(new HashSpan[0])[1];
//
//                    int first1 = span1.getFirst().toByteArray()[0];
//                    int last1 = span1.getLast().toByteArray()[0];
//
//                    int actualSize1 = Math.abs(last1 - first1);
//
//                    int first2 = span2.getFirst().toByteArray()[0];
//                    int last2 = span2.getLast().toByteArray()[0];
//
//                    int actualSize2 = Math.abs(last2 - first2);
//
//                    int actualSize = actualSize1 + actualSize2;
//
//                    assertTrue("Each set of spans should be proportional to the total size.", actualSize - numServers <= expectedSize && actualSize + numServers >= expectedSize);
//
//                }
//
//            }
//
//            // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//            // Generate a handful of hashes, make sure fit right number of hash spans
//
//            // First, build an array list of hash spans. Don't care anymore about
//            // the server to which each span belongs
//            List<HashSpan> spansList = new ArrayList();
//
//            for (Set<HashSpan> nextSet : listOfSets) {
//                for (HashSpan next : nextSet.toArray(new HashSpan[0])) {
//                    spansList.add(next);
//                }
//            }
//            byte[] randomHashBytes;
//            BigHash randomHash;
//
//            for (int i = 0; i < 100; i++) {
//
//                int count = 0; // <-- How many hash spans which the random hash falls under
//
//                randomHashBytes = Utils.makeRandomData(BigHash.HASH_LENGTH);
//                randomHash = BigHash.createFromBytes(randomHashBytes);
//
//                for (HashSpan nextSpan : spansList) {
//                    if (nextSpan.contains(randomHash)) {
//                        count++;
//                    }
//                }
//                assertTrue("Case " + i + " should replicate at least " + numReplications + " times.", count == numReplications);
//            }
//        } catch (Exception e) {
//            fail(e.getMessage());
//        } finally {
//
//            if (ffservers != null) {
//                for (FlatFileTrancheServer ffserver : ffservers) {
//                    IOUtil.safeClose(ffserver);
//                }
//            }
//            if (servers != null) {
//                for (Server server : servers) {
//                    IOUtil.safeClose(server);
//                }
//            }
//            if (rservers != null) {
//                for (TrancheServer rserver : rservers) {
//                    IOUtil.safeClose(rserver);
//                }
//            }
//            if (userZip != null && userZip.getFile().exists()) {
//                IOUtil.safeDelete(userZip.getFile());
//            }
//            if (tmpDir != null) {
//                IOUtil.recursiveDeleteWithWarning(tmpDir);
//            }
//        }
    }
}
