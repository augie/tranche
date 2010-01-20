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
package org.tranche.configuration;

import org.tranche.exceptions.ServerIsNotReadableException;
import org.tranche.exceptions.ServerIsNotWritableException;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ServerModeFlagTest extends TrancheTestCase {

    public void testUnique() throws Exception {
        TestUtil.printTitle("ServerModeFlagTest:testUnique()");

        assertEquals("Should be equal.", ServerModeFlag.NONE, ServerModeFlag.NONE);
        assertNotSame("Should not be equal.", ServerModeFlag.NONE, ServerModeFlag.CAN_READ_WRITE);
        assertNotSame("Should not be equal.", ServerModeFlag.NONE, ServerModeFlag.CAN_WRITE);
        assertNotSame("Should not be equal.", ServerModeFlag.NONE, ServerModeFlag.CAN_READ);

        assertEquals("Should be equal.", ServerModeFlag.CAN_READ, ServerModeFlag.CAN_READ);
        assertNotSame("Should not be equal.", ServerModeFlag.CAN_READ, ServerModeFlag.CAN_READ_WRITE);
        assertNotSame("Should not be equal.", ServerModeFlag.CAN_READ, ServerModeFlag.CAN_WRITE);
        assertNotSame("Should not be equal.", ServerModeFlag.CAN_READ, ServerModeFlag.NONE);

        assertEquals("Should be equal.", ServerModeFlag.CAN_WRITE, ServerModeFlag.CAN_WRITE);
        assertNotSame("Should not be equal.", ServerModeFlag.CAN_WRITE, ServerModeFlag.CAN_READ_WRITE);
        assertNotSame("Should not be equal.", ServerModeFlag.CAN_WRITE, ServerModeFlag.CAN_READ);
        assertNotSame("Should not be equal.", ServerModeFlag.CAN_WRITE, ServerModeFlag.NONE);

        assertEquals("Should be equal.", ServerModeFlag.CAN_READ_WRITE, ServerModeFlag.CAN_READ_WRITE);
        assertNotSame("Should not be equal.", ServerModeFlag.CAN_READ_WRITE, ServerModeFlag.CAN_READ);
        assertNotSame("Should not be equal.", ServerModeFlag.CAN_READ_WRITE, ServerModeFlag.CAN_WRITE);
        assertNotSame("Should not be equal.", ServerModeFlag.CAN_READ_WRITE, ServerModeFlag.NONE);
    }

    /**
     * Test of toString method, of class ServerModeFlag.
     */
    public void testToString() throws Exception {
        TestUtil.printTitle("ServerModeFlagTest:testToString()");

        assertNotNull("Shouldn't be null.", ServerModeFlag.toString(ServerModeFlag.NONE));
        assertNotNull("Shouldn't be null.", ServerModeFlag.toString(ServerModeFlag.CAN_READ));
        assertNotNull("Shouldn't be null.", ServerModeFlag.toString(ServerModeFlag.CAN_READ_WRITE));
        assertNotNull("Shouldn't be null.", ServerModeFlag.toString(ServerModeFlag.CAN_WRITE));

        // I'm not asserting on string matches since toString might very well change
        System.out.println("Should say something about \"none\": " + ServerModeFlag.toString(ServerModeFlag.NONE));
        System.out.println("Should say something about \"can read\": " + ServerModeFlag.toString(ServerModeFlag.CAN_READ));
        System.out.println("Should say something about \"can read & write\": " + ServerModeFlag.toString(ServerModeFlag.CAN_READ_WRITE));
        System.out.println("Should say something about \"can write\": " + ServerModeFlag.toString(ServerModeFlag.CAN_WRITE));

        // Negative test: make sure a fake byte throws an exception
        try {
            byte fake = Byte.MAX_VALUE;
            String str = "Should have thrown an  exception for flag " + fake + ", instead: " + ServerModeFlag.toString(fake);
            fail(str);
        } catch (Exception nope) { /* Should throw an exception */ }
    }

    public void testCanRead() throws Exception {
        TestUtil.printTitle("ServerModeFlagTest:testCanRead()");

        assertTrue("Failed. Check ServerModeFlag byte values to verify mutually exclusive.", ServerModeFlag.canRead(ServerModeFlag.CAN_READ));
        assertTrue("Failed. Check ServerModeFlag byte values to verify mutually exclusive.", ServerModeFlag.canRead(ServerModeFlag.CAN_READ_WRITE));
        assertFalse("Failed. Check ServerModeFlag byte values to verify mutually exclusive.", ServerModeFlag.canRead(ServerModeFlag.NONE));
        assertFalse("Failed. Check ServerModeFlag byte values to verify mutually exclusive.", ServerModeFlag.canRead(ServerModeFlag.CAN_WRITE));
    }

    public void testCanWrite() throws Exception {
        TestUtil.printTitle("ServerModeFlagTest:testCanWrite()");
        
        assertTrue("Failed. Check ServerModeFlag byte values to verify mutually exclusive.", ServerModeFlag.canWrite(ServerModeFlag.CAN_WRITE));
        assertTrue("Failed. Check ServerModeFlag byte values to verify mutually exclusive.", ServerModeFlag.canWrite(ServerModeFlag.CAN_READ_WRITE));
        assertFalse("Failed. Check ServerModeFlag byte values to verify mutually exclusive.", ServerModeFlag.canWrite(ServerModeFlag.NONE));
        assertFalse("Failed. Check ServerModeFlag byte values to verify mutually exclusive.", ServerModeFlag.canWrite(ServerModeFlag.CAN_READ));
    }

    public void testServerWithNoneFlag() throws Exception {
        TestUtil.printTitle("ServerModeFlagTest:testServerWithNoneFlag()");

        String HOST = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        try {
            testNetwork.start();

            // Before test, add chunk. Shouldn't be issue.
            testNetwork.getFlatFileTrancheServer(HOST).getConfiguration().setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.CAN_READ_WRITE));
            testNetwork.getFlatFileTrancheServer(HOST).saveConfiguration();

            final byte[] dataChunk = DevUtil.createRandomDataChunk(1048 * 16);
            final BigHash dataHash = new BigHash(dataChunk);

            final byte[] metaDataChunk = DevUtil.createRandomMetaDataChunk();
            final BigHash metaDataHash = DevUtil.getRandomBigHash();

            IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHash, dataChunk);
            IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaDataHash, metaDataChunk);

            assertTrue("Should have data chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST), dataHash));
            // Verify bytes
            {
                byte[] verifyDataChunk = ((byte[][]) IOUtil.getData(testNetwork.getFlatFileTrancheServer(HOST), dataHash, false).getReturnValueObject())[0];
                assertEquals("Chunks should have same length.", dataChunk.length, verifyDataChunk.length);
                for (int i = 0; i < dataChunk.length; i++) {
                    assertEquals("Bytes should always match: " + i, dataChunk[i], verifyDataChunk[i]);
                }
            }

            assertTrue("Should have meta data chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST), metaDataHash));
            // Verify bytes
            {
                byte[] verifymetaDataChunk = ((byte[][]) IOUtil.getMetaData(testNetwork.getFlatFileTrancheServer(HOST), metaDataHash, false).getReturnValueObject())[0];
                assertEquals("Chunks should have same length.", metaDataChunk.length, verifymetaDataChunk.length);
                for (int i = 0; i < metaDataChunk.length; i++) {
                    assertEquals("Bytes should always match: " + i, metaDataChunk[i], verifymetaDataChunk[i]);
                }
            }

            // --------------------------------------------------------------------------------
            // --- TEST CONDITION STARTS HERE ---
            // --------------------------------------------------------------------------------
            testNetwork.getFlatFileTrancheServer(HOST).getConfiguration().setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.NONE));
            testNetwork.getFlatFileTrancheServer(HOST).saveConfiguration();

            assertFalse("Should not be able write.", testNetwork.getFlatFileTrancheServer(HOST).canWrite());
            assertFalse("Should not be able read.", testNetwork.getFlatFileTrancheServer(HOST).canRead());

            byte flagVerify = Byte.valueOf(testNetwork.getFlatFileTrancheServer(HOST).getConfiguration().getValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM));
            assertEquals("Flags should match.", ServerModeFlag.NONE, flagVerify);

            // Should NOT be able to read
            {
                PropagationReturnWrapper wrapper = IOUtil.getData(testNetwork.getFlatFileTrancheServer(HOST), dataHash, false);;
                assertTrue(wrapper.isAnyErrors());
                assertEquals(1, wrapper.getErrors().size());
                assertEquals(ServerIsNotReadableException.MESSAGE, wrapper.getErrors().iterator().next().exception.getMessage());
            }
            {
                PropagationReturnWrapper wrapper = IOUtil.getMetaData(testNetwork.getFlatFileTrancheServer(HOST), metaDataHash, false);
                assertTrue(wrapper.isAnyErrors());
                assertEquals(1, wrapper.getErrors().size());
                assertEquals(ServerIsNotReadableException.MESSAGE, wrapper.getErrors().iterator().next().exception.getMessage());
            }

            // Should NOT be able to write
            {
                byte[] chunk = DevUtil.createRandomDataChunk(1024 * 5);
                BigHash hash = new BigHash(chunk);
                PropagationReturnWrapper wrapper = IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);
                assertTrue(wrapper.isAnyErrors());
                assertEquals(1, wrapper.getErrors().size());
                assertEquals(ServerIsNotWritableException.MESSAGE, wrapper.getErrors().iterator().next().exception.getMessage());
            }
            {
                byte[] chunk = DevUtil.createRandomMetaDataChunk();
                BigHash hash = DevUtil.getRandomBigHash();
                PropagationReturnWrapper wrapper = IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);
                assertTrue(wrapper.isAnyErrors());
                assertEquals(1, wrapper.getErrors().size());
                assertEquals(ServerIsNotWritableException.MESSAGE, wrapper.getErrors().iterator().next().exception.getMessage());
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testServerWithReadFlag() throws Exception {
        TestUtil.printTitle("ServerModeFlagTest:testServerWithReadFlag()");

        String HOST = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        try {
            testNetwork.start();

            // Before test, add chunk. Shouldn't be issue.
            testNetwork.getFlatFileTrancheServer(HOST).getConfiguration().setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.CAN_READ_WRITE));
            testNetwork.getFlatFileTrancheServer(HOST).saveConfiguration();

            final byte[] dataChunk = DevUtil.createRandomDataChunk(1048 * 16);
            final BigHash dataHash = new BigHash(dataChunk);

            final byte[] metaDataChunk = DevUtil.createRandomMetaDataChunk();
            final BigHash metaDataHash = new BigHash(metaDataChunk);

            IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHash, dataChunk);
            IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaDataHash, metaDataChunk);

            assertTrue("Should have data chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST), dataHash));
            // Verify bytes
            {
                byte[] verifyDataChunk = ((byte[][]) IOUtil.getData(testNetwork.getFlatFileTrancheServer(HOST), dataHash, false).getReturnValueObject())[0];
                assertEquals("Chunks should have same length.", dataChunk.length, verifyDataChunk.length);
                for (int i = 0; i < dataChunk.length; i++) {
                    assertEquals("Bytes should always match: " + i, dataChunk[i], verifyDataChunk[i]);
                }
            }

            assertTrue("Should have meta data chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST), metaDataHash));
            // Verify bytes
            {
                byte[] verifymetaDataChunk = ((byte[][]) IOUtil.getMetaData(testNetwork.getFlatFileTrancheServer(HOST), metaDataHash, false).getReturnValueObject())[0];
                assertEquals("Chunks should have same length.", metaDataChunk.length, verifymetaDataChunk.length);
                for (int i = 0; i < metaDataChunk.length; i++) {
                    assertEquals("Bytes should always match: " + i, metaDataChunk[i], verifymetaDataChunk[i]);
                }
            }

            // --------------------------------------------------------------------------------
            // --- TEST CONDITION STARTS HERE ---
            // --------------------------------------------------------------------------------
            testNetwork.getFlatFileTrancheServer(HOST).getConfiguration().setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.CAN_READ));
            testNetwork.getFlatFileTrancheServer(HOST).saveConfiguration();

            assertFalse("Should not be able to write.", testNetwork.getFlatFileTrancheServer(HOST).canWrite());
            assertTrue("Should be able to read.", testNetwork.getFlatFileTrancheServer(HOST).canRead());

            byte flagVerify = Byte.valueOf(testNetwork.getFlatFileTrancheServer(HOST).getConfiguration().getValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM));
            assertEquals("Flags should match.", ServerModeFlag.CAN_READ, flagVerify);

            // Should be able to read
            {
                byte[] dataBytes = ((byte[][]) IOUtil.getData(testNetwork.getFlatFileTrancheServer(HOST), dataHash, false).getReturnValueObject())[0];
                assertEquals(dataHash, new BigHash(dataBytes));
            }
            {
                byte[] metaDataBytes = ((byte[][]) IOUtil.getMetaData(testNetwork.getFlatFileTrancheServer(HOST), metaDataHash, false).getReturnValueObject())[0];
                assertEquals(metaDataHash, new BigHash(metaDataBytes));
            }

            // Should NOT be able to write
            {
                byte[] chunk = DevUtil.createRandomDataChunk(1024 * 5);
                BigHash hash = new BigHash(chunk);
                PropagationReturnWrapper wrapper = IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);
                assertTrue(wrapper.isAnyErrors());
                assertEquals(1, wrapper.getErrors().size());
                assertEquals(ServerIsNotWritableException.MESSAGE, wrapper.getErrors().iterator().next().exception.getMessage());
            }
            {
                byte[] chunk = DevUtil.createRandomMetaDataChunk();
                BigHash hash = DevUtil.getRandomBigHash();
                PropagationReturnWrapper wrapper = IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);
                assertTrue(wrapper.isAnyErrors());
                assertEquals(1, wrapper.getErrors().size());
                assertEquals(ServerIsNotWritableException.MESSAGE, wrapper.getErrors().iterator().next().exception.getMessage());
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testServerWithWriteFlag() throws Exception {
        TestUtil.printTitle("ServerModeFlagTest:testServerWithWriteFlag()");

        String HOST = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        try {
            testNetwork.start();

            // Before test, add chunk. Shouldn't be issue.
            testNetwork.getFlatFileTrancheServer(HOST).getConfiguration().setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.CAN_READ_WRITE));
            testNetwork.getFlatFileTrancheServer(HOST).saveConfiguration();

            final byte[] dataChunk = DevUtil.createRandomDataChunk(1048 * 16);
            final BigHash dataHash = new BigHash(dataChunk);

            final byte[] metaDataChunk = DevUtil.createRandomMetaDataChunk();
            final BigHash metaDataHash = new BigHash(metaDataChunk);

            IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHash, dataChunk);
            IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaDataHash, metaDataChunk);

            assertTrue("Should have data chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST), dataHash));
            // Verify bytes
            {
                byte[] verifyDataChunk = ((byte[][]) IOUtil.getData(testNetwork.getFlatFileTrancheServer(HOST), dataHash, false).getReturnValueObject())[0];
                assertEquals("Chunks should have same length.", dataChunk.length, verifyDataChunk.length);
                for (int i = 0; i < dataChunk.length; i++) {
                    assertEquals("Bytes should always match: " + i, dataChunk[i], verifyDataChunk[i]);
                }
            }

            assertTrue("Should have meta data chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST), metaDataHash));
            // Verify bytes
            {
                byte[] verifymetaDataChunk = ((byte[][]) IOUtil.getMetaData(testNetwork.getFlatFileTrancheServer(HOST), metaDataHash, false).getReturnValueObject())[0];
                assertEquals("Chunks should have same length.", metaDataChunk.length, verifymetaDataChunk.length);
                for (int i = 0; i < metaDataChunk.length; i++) {
                    assertEquals("Bytes should always match: " + i, metaDataChunk[i], verifymetaDataChunk[i]);
                }
            }

            // --------------------------------------------------------------------------------
            // --- TEST CONDITION STARTS HERE ---
            // --------------------------------------------------------------------------------
            testNetwork.getFlatFileTrancheServer(HOST).getConfiguration().setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.CAN_WRITE));
            testNetwork.getFlatFileTrancheServer(HOST).saveConfiguration();

            assertTrue("Should be able to write.", testNetwork.getFlatFileTrancheServer(HOST).canWrite());
            assertFalse("Should not be able to read.", testNetwork.getFlatFileTrancheServer(HOST).canRead());

            byte flagVerify = Byte.valueOf(testNetwork.getFlatFileTrancheServer(HOST).getConfiguration().getValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM));
            assertEquals("Flags should match.", ServerModeFlag.CAN_WRITE, flagVerify);

            // Should NOT be able to read
            {
                PropagationReturnWrapper wrapper = IOUtil.getData(testNetwork.getFlatFileTrancheServer(HOST), dataHash, false);;
                assertTrue(wrapper.isAnyErrors());
                assertEquals(1, wrapper.getErrors().size());
                assertEquals(ServerIsNotReadableException.MESSAGE, wrapper.getErrors().iterator().next().exception.getMessage());
            }
            {
                PropagationReturnWrapper wrapper = IOUtil.getMetaData(testNetwork.getFlatFileTrancheServer(HOST), metaDataHash, false);
                assertTrue(wrapper.isAnyErrors());
                assertEquals(1, wrapper.getErrors().size());
                assertEquals(ServerIsNotReadableException.MESSAGE, wrapper.getErrors().iterator().next().exception.getMessage());
            }

            // Should be able to write
            {
                byte[] chunk = DevUtil.createRandomDataChunk(1024 * 5);
                BigHash hash = new BigHash(chunk);
                PropagationReturnWrapper wrapper = IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);
                assertFalse(wrapper.isAnyErrors());
                assertTrue(IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST), hash));
            }
            {
                byte[] chunk = DevUtil.createRandomMetaDataChunk();
                BigHash hash = DevUtil.getRandomBigHash();
                PropagationReturnWrapper wrapper = IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);
                assertFalse(wrapper.isAnyErrors());
                assertTrue(IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST), hash));
            }
        } finally {
            testNetwork.stop();
        }
    }

    public void testServerWithReadWriteFlag() throws Exception {
        TestUtil.printTitle("ServerModeFlagTest:testServerWithReadWriteFlag()");

        String HOST = "server1.com";
        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        try {
            testNetwork.start();

            // Before test, add chunk. Shouldn't be issue.
            testNetwork.getFlatFileTrancheServer(HOST).getConfiguration().setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.CAN_READ_WRITE));
            testNetwork.getFlatFileTrancheServer(HOST).saveConfiguration();

            final byte[] dataChunk = DevUtil.createRandomDataChunk(1048 * 16);
            final BigHash dataHash = new BigHash(dataChunk);

            final byte[] metaDataChunk = DevUtil.createRandomMetaDataChunk();
            final BigHash metaDataHash = new BigHash(metaDataChunk);

            IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), dataHash, dataChunk);
            IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, metaDataHash, metaDataChunk);

            assertTrue("Should have data chunk.", IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST), dataHash));
            // Verify bytes
            {
                byte[] verifyDataChunk = ((byte[][]) IOUtil.getData(testNetwork.getFlatFileTrancheServer(HOST), dataHash, false).getReturnValueObject())[0];
                assertEquals("Chunks should have same length.", dataChunk.length, verifyDataChunk.length);
                for (int i = 0; i < dataChunk.length; i++) {
                    assertEquals("Bytes should always match: " + i, dataChunk[i], verifyDataChunk[i]);
                }
            }

            assertTrue("Should have meta data chunk.", IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST), metaDataHash));
            // Verify bytes
            {
                byte[] verifymetaDataChunk = ((byte[][]) IOUtil.getMetaData(testNetwork.getFlatFileTrancheServer(HOST), metaDataHash, false).getReturnValueObject())[0];
                assertEquals("Chunks should have same length.", metaDataChunk.length, verifymetaDataChunk.length);
                for (int i = 0; i < metaDataChunk.length; i++) {
                    assertEquals("Bytes should always match: " + i, metaDataChunk[i], verifymetaDataChunk[i]);
                }
            }

            // --------------------------------------------------------------------------------
            // --- TEST CONDITION STARTS HERE ---
            // --------------------------------------------------------------------------------
            testNetwork.getFlatFileTrancheServer(HOST).getConfiguration().setValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM, String.valueOf(ServerModeFlag.CAN_READ_WRITE));
            testNetwork.getFlatFileTrancheServer(HOST).saveConfiguration();

            assertTrue("Should be able to write.", testNetwork.getFlatFileTrancheServer(HOST).canWrite());
            assertTrue("Should be able to read.", testNetwork.getFlatFileTrancheServer(HOST).canRead());

            byte flagVerify = Byte.valueOf(testNetwork.getFlatFileTrancheServer(HOST).getConfiguration().getValue(ConfigKeys.SERVER_MODE_FLAG_SYSTEM));
            assertEquals("Flags should match.", ServerModeFlag.CAN_READ_WRITE, flagVerify);

            // Should NOT be able to read
            {
                byte[] dataBytes = ((byte[][]) IOUtil.getData(testNetwork.getFlatFileTrancheServer(HOST), dataHash, false).getReturnValueObject())[0];
                assertEquals(dataHash, new BigHash(dataBytes));
            }
            {
                byte[] metaDataBytes = ((byte[][]) IOUtil.getMetaData(testNetwork.getFlatFileTrancheServer(HOST), metaDataHash, false).getReturnValueObject())[0];
                assertEquals(metaDataHash, new BigHash(metaDataBytes));
            }

            // Should be able to write
            {
                byte[] chunk = DevUtil.createRandomDataChunk(1024 * 5);
                BigHash hash = new BigHash(chunk);
                PropagationReturnWrapper wrapper = IOUtil.setData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), hash, chunk);
                assertFalse(wrapper.isAnyErrors());
                assertTrue(IOUtil.hasData(testNetwork.getFlatFileTrancheServer(HOST), hash));
            }
            {
                byte[] chunk = DevUtil.createRandomMetaDataChunk();
                BigHash hash = DevUtil.getRandomBigHash();
                PropagationReturnWrapper wrapper = IOUtil.setMetaData(testNetwork.getFlatFileTrancheServer(HOST), DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey(), false, hash, chunk);
                assertFalse(wrapper.isAnyErrors());
                assertTrue(IOUtil.hasMetaData(testNetwork.getFlatFileTrancheServer(HOST), hash));
            }
        } finally {
            testNetwork.stop();
        }
    }
}
