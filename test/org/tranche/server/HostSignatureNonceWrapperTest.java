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

import java.util.Random;
import org.tranche.security.Signature;
import org.tranche.flatfile.NonceMap;
import org.tranche.util.DevUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class HostSignatureNonceWrapperTest extends TrancheTestCase {

    /**
     * <p>Tests when HostSignatureNonceWrapper.equals(Object o) parameter instance of HostSignatureNonceWrapper.</p>
     * @throws java.lang.Exception
     */
    public void testEqualsToHostSignatureNonceWrapper() throws Exception {
        String host1 = "141.214.182.207";
        String host2 = "141.214.182.211";
        
        byte[] nonce1 = new byte[NonceMap.NONCE_BYTES];
        byte[] nonce2 = new byte[NonceMap.NONCE_BYTES];
        
        Random r = new Random();
        r.nextBytes(nonce1);
        r.nextBytes(nonce2);
        
        // Very unlikely.
        while (areBytesEqual(nonce1, nonce2)) {
            r.nextBytes(nonce2);
        }
        
        Signature sig1 = DevUtil.getBogusSignature();
        Signature sig2 = DevUtil.getBogusSignature();
        
        // Very unlikely.
        while (sig1.equals(sig2)) {
            sig2 = DevUtil.getBogusSignature();
        }
        
        HostSignatureNonceWrapper wrapper1 = new HostSignatureNonceWrapper(host1, sig1, nonce1);
        HostSignatureNonceWrapper wrapper1Copy = new HostSignatureNonceWrapper(host1, sig1, nonce1);
        
        assertFalse("Should have different memory addresses.", wrapper1 == wrapper1Copy);
        assertEquals("Should be equal.", wrapper1, wrapper1Copy);
        
        HostSignatureNonceWrapper wrapper2 = new HostSignatureNonceWrapper(host2, sig2, nonce2);
        HostSignatureNonceWrapper wrapper2Copy = new HostSignatureNonceWrapper(host2, sig2, nonce2);
        
        assertFalse("Should have different memory addresses.", wrapper2 == wrapper2Copy);
        assertEquals("Should be equal.", wrapper2, wrapper2Copy);
        
        assertFalse("Should not be equal.", wrapper1.equals(wrapper2));
        assertFalse("Should not be equal.", wrapper2.equals(wrapper1));
        
        HostSignatureNonceWrapper wrapper1DifferentNonce = new HostSignatureNonceWrapper(host1, sig1, nonce2);
        
        assertFalse("Since nonce is different, should not be equal.", wrapper1.equals(wrapper1DifferentNonce));
        assertFalse("Since nonce is different, should not be equal.", wrapper1DifferentNonce.equals(wrapper1));
        
        HostSignatureNonceWrapper wrapper1DifferentSignature = new HostSignatureNonceWrapper(host1, sig2, nonce1);
        
        assertFalse("Since signature is different, should not be equal.", wrapper1.equals(wrapper1DifferentSignature));
        assertFalse("Since signature is different, should not be equal.", wrapper1DifferentSignature.equals(wrapper1));
    }
    
    private boolean areBytesEqual(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            return false;
        }
        
        for (int i=0; i<b1.length; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * <p>Tests when HostSignatureNonceWrapper.equals(Object o) parameter instance of String.</p>
     * @throws java.lang.Exception
     */
    public void testEqualsToHostString() throws Exception {
        String host1 = "141.214.182.207";
        String host2 = "141.214.182.211";
        
        byte[] nonce1 = new byte[NonceMap.NONCE_BYTES];
        
        Random r = new Random();
        r.nextBytes(nonce1);
        
        Signature sig1 = DevUtil.getBogusSignature();
        
        HostSignatureNonceWrapper wrapper1 = new HostSignatureNonceWrapper(host1, sig1, nonce1);
        
        assertTrue("Should be equal.", wrapper1.equals(host1));
        assertFalse("Should not be equal.", wrapper1.equals(host2));
    }
}
