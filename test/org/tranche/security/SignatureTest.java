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
package org.tranche.security;

import org.tranche.util.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SignatureTest extends TrancheTestCase {

    public void testSignature() throws Exception {
        X509Certificate cert = null;
        try {
            cert = DevUtil.getDevAuthority();
        } catch (GeneralSecurityException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Signature sig1 = new Signature(cert.getSignature(), cert.getSigAlgName(), cert);
        Signature sig2 = new Signature(cert.getSignature(), cert.getSigAlgName(), cert);

        assertTrue("Two signatures should be equal.", sig1.equals(sig2));
        assertNotNull("Signature should return hash code.", sig1.hashCode());
    }

    public void testToAndFromSignatureByteArray() throws Exception {
        Signature sig = DevUtil.getBogusSignature();
        byte[] sigBytes = sig.toByteArray();
        Signature sigVerify = new Signature(sigBytes);
        assertEquals("Should verify", sig, sigVerify);
    }
}
