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
package org.tranche.logs.activity;

/**
 * <p>Wrapper class for data that is held in the signature index file for activity log.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class SignatureIndexEntry {

    public final int signatureIndex;
    public final long signatureOffset;
    public final int signatureLen;

    public SignatureIndexEntry(final int signatureIndex, final long signatureOffset, final int signatureLen) {
        this.signatureIndex = signatureIndex;
        this.signatureOffset = signatureOffset;
        this.signatureLen = signatureLen;
    }
    
    @Override()
    public int hashCode() {
        return signatureIndex;
    }
    
    @Override()
    public boolean equals(Object o) {
        if (o instanceof SignatureIndexEntry) {
            SignatureIndexEntry e = (SignatureIndexEntry)o;
            return e.signatureIndex == this.signatureIndex && e.signatureLen == this.signatureLen && e.signatureOffset == this.signatureOffset;
        }
        return false;
    }
    
    @Override()
    public String toString() {
        return "SignatureIndexEntry: index=" + signatureIndex + "; length=" + signatureLen + "; offset=" + signatureOffset;
    }
}
