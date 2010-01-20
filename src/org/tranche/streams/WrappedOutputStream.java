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
package org.tranche.streams;

import org.tranche.hash.BigHashMaker;
import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.SignatureException;

/**
 * <p>A wrapper class that lets the AddFielTool upload data on-the-fly without requiring temporary files when downloading. This greatly speeds up uploads and significantly reduces disk access times.</p>
 */
public class WrappedOutputStream extends OutputStream {

    private BigHashMaker bhm;
    private OutputStream out;
    private java.security.Signature signature = null;
    private byte[] signatureBytes = null;
    private boolean closed = false;

    /**
     * <p>Creates a wrapped output stream that will automatically create an appropriate BigHash from the bytes streamed through it.</p>
     * @param out The OutputStream to write bytes to. This wrapper will pass all bytes.
     * @param bhm The BigHashMaker that will be updated as bytes are written to this class.
     */
    public WrappedOutputStream(OutputStream out, BigHashMaker bhm) {
        this.bhm = bhm;
        this.out = out;
    }

    /**
     * 
     * @param out 
     * @param bhm 
     * @param algorithm 
     * @param key 
     */
    public WrappedOutputStream(OutputStream out, BigHashMaker bhm, String algorithm, PrivateKey key) {
        this(out, bhm);
        try {
            // get something that can sign
            signature = java.security.Signature.getInstance(algorithm, "BC");
            // init the signer
            signature.initSign(key);
        } catch (Exception ex) {
            throw new RuntimeException("Can't make signature!", ex);
        }
    }

    /**
     * Writes using a buffer and respecting offset and length.
     * @param buf 
     * @param off 
     * @param len 
     * @throws java.io.IOException 
     */
    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        // update the hash
        bhm.update(buf, off, len);
        // write to the parent stream
        out.write(buf, off, len);

        // optionally update the signature
        if (signature != null) {
            try {
                signature.update(buf, off, len);
            } catch (SignatureException ex) {
                throw new RuntimeException("Can't make signature!", ex);
            }
        }
    }

    /**
     * <p>Writes a complete buffer of bytes.</p>
     * @param b 
     * @throws java.io.IOException 
     */
    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }
    byte[] tempBuf = new byte[1];

    /**
     * <p>Writes a single byte out a time. Not recommended for speed.</p>
     * @param b 
     * @throws java.io.IOException 
     */
    public void write(int b) throws IOException {
        tempBuf[0] = (byte) b;
        write(tempBuf);
    }

    /**
     * <p>Flushes any bytes being buffered. This class doesn't not buffer bytes from the wrapped OutputStream.</p>
     * @throws java.io.IOException 
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * <p>Closes the wrapped OutputStream.</p>
     * @throws java.io.IOException 
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        // close underlying stream
        out.close();
        // make the signature
        if (signature != null) {
            try {
                signatureBytes = signature.sign();
            } catch (SignatureException ex) {
                throw new RuntimeException("Can't make signature!", ex);
            }
        }
    }

    /**
     * <p>Returns the appropriate bytes to represent a signature for this file.</p>
     * @return The appropriate bytes to represent a signature for this file.
     */
    public byte[] getSignatureBytes() {
        return signatureBytes;
    }
}
