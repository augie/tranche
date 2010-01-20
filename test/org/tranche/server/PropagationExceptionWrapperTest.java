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

import java.io.ByteArrayOutputStream;
import org.tranche.exceptions.PropagationFailedException;
import org.tranche.exceptions.ServerIsNotWritableException;
import org.tranche.exceptions.ServerIsOfflineException;
import org.tranche.hash.BigHash;
import org.tranche.util.DevUtil;
import org.tranche.util.IOUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class PropagationExceptionWrapperTest extends TrancheTestCase {

    public void testSerializeDeserialize() throws Exception {

        String w1Host = "bryanesmith.com";
        BigHash w1Hash = DevUtil.getRandomBigHash();
        String w1Message = "Testing #1 (contains a comma, and a colon:)";
        PropagationExceptionWrapper w1 = new PropagationExceptionWrapper(new Exception(w1Message), w1Host, w1Hash);

        ByteArrayOutputStream out1 = new ByteArrayOutputStream();

        w1.serialize(out1);
        out1.flush();
        byte[] bytes1 = out1.toByteArray();
        IOUtil.safeClose(out1);

        PropagationExceptionWrapper w1Verify = PropagationExceptionWrapper.deserialize(bytes1);
        
        assertEquals("Expect same", w1Host, w1Verify.host);
        assertEquals("Expect same", w1Hash, w1Verify.hash);
        assertEquals("Expect same", w1Message, w1Verify.exception.getMessage());
        assertEquals("Expect same", "Exception", w1Verify.exception.getClass().getSimpleName());

        String w2Host = "828productions.com";
        String w2Message = "Testing #2 :,,,,,,: Just testing\n\nsome content!~";
        PropagationExceptionWrapper w2 = new PropagationExceptionWrapper(new RuntimeException(w2Message), w2Host);
        
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();

        w2.serialize(out2);
        out2.flush();
        byte[] bytes2 = out2.toByteArray();
        IOUtil.safeClose(out2);
        
        PropagationExceptionWrapper w2Verify = PropagationExceptionWrapper.deserialize(bytes2);

        assertEquals("Expect same", w2Host, w2Verify.host);
        assertNull(w2Verify.hash);
        assertEquals("Expect same", w2Message, w2Verify.exception.getMessage());
        assertEquals("Expect same", "RuntimeException", w2Verify.exception.getClass().getSimpleName());

        String w3Host = "w3.org";
        PropagationExceptionWrapper w3 = new PropagationExceptionWrapper(new ServerIsNotWritableException(), w3Host);
        
        ByteArrayOutputStream out3 = new ByteArrayOutputStream();

        w3.serialize(out3);
        out3.flush();
        byte[] bytes3 = out3.toByteArray();
        IOUtil.safeClose(out3);
        
        PropagationExceptionWrapper w3Verify = PropagationExceptionWrapper.deserialize(bytes3);

        assertEquals("Expect same", w3Host, w3Verify.host);
        assertNull(w3Verify.hash);
        assertEquals("Expect same", ServerIsNotWritableException.MESSAGE, w3Verify.exception.getMessage());
        assertEquals("Expect same", "ServerIsNotWritableException", w3Verify.exception.getClass().getSimpleName());

    }

    /**
     * If wrapping a PropagationFailedException, make sure deserializes as just that!
     * @throws java.lang.Exception
     */
    public void testPropagationFailedExceptionIsProperlyDeserialized() throws Exception {
        PropagationExceptionWrapper w1 = new PropagationExceptionWrapper(new PropagationFailedException(), "127.0.0.1");
        
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();

        w1.serialize(out1);
        out1.flush();
        byte[] bytes1 = out1.toByteArray();
        IOUtil.safeClose(out1);
        PropagationExceptionWrapper w1Verified = PropagationExceptionWrapper.deserialize(bytes1);

        assertTrue("Deserialized exception should be instance of PropagationFailedException, but instead: " + w1Verified.exception.getClass().getSimpleName(), w1Verified.exception instanceof PropagationFailedException);
    }

    /**
     * If wrapping a ServerIsOfflineException, make sure deserializes as just that!
     * @throws java.lang.Exception
     */
    public void testServerIsOfflineExceptionIsProperlyDeserialized() throws Exception {
        String offlineHost = "141.214.182.209";
        Exception offlineException = new ServerIsOfflineException(offlineHost);

        PropagationExceptionWrapper w1 = new PropagationExceptionWrapper(offlineException, "127.0.0.1");
        
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();

        w1.serialize(out1);
        out1.flush();
        byte[] bytes1 = out1.toByteArray();
        IOUtil.safeClose(out1);
        PropagationExceptionWrapper w1Verified = PropagationExceptionWrapper.deserialize(bytes1);

        assertTrue("Deserialized exception should be instance of ServerIsOfflineException, but instead: " + w1Verified.exception.getClass().getSimpleName(), w1Verified.exception instanceof ServerIsOfflineException);
        assertEquals("Should know the exception message (at least, beginning of string).", offlineException.getMessage().substring(0, 15), w1Verified.exception.getMessage().substring(0, 15));
    }
}
