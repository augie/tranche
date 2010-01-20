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

import org.tranche.hash.BigHash;
import org.tranche.hash.BigHashMaker;
import java.io.IOException;
import java.io.OutputStream;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;

/**
 * <p>Encrypted file output stream.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class AESEncodingStream extends OutputStream {

    // track a big hash
    private BigHashMaker bhm = new BigHashMaker();
    // the big hash
    private BigHash bh = null;
    // make the AES encryption engine
    private AESFastEngine encrypt;
    // the block size
    private int blockSize;
    // make the buffers
    private byte[] data;
    private byte[] encrypted;
    /**
     * the real output stream
     */
    private OutputStream out;
    /**
     * offsets for the current buffer
     */
    private int currentDataOffset = 0;
    private boolean closed = false;
    byte[] tempBuf = new byte[1];

    /**
     * 
     * @param passphrase
     * @param out
     * @throws java.io.IOException
     */
    public AESEncodingStream(String passphrase, OutputStream out) throws IOException {
        this(passphrase, new byte[8], 1000, out);
    }

    /**
     * 
     * @param passphrase
     * @param salt
     * @param iterations
     * @param out
     * @throws java.io.IOException
     */
    public AESEncodingStream(String passphrase, byte[] salt, int iterations, OutputStream out) throws IOException {
        // cache the real output stream
        this.out = out;

        // make the AES encryption engine
        encrypt = new AESFastEngine();
        // make up some params
        PKCS5S2ParametersGenerator pg = new PKCS5S2ParametersGenerator();
        pg.init(passphrase.getBytes(), salt, iterations);
        CipherParameters params = pg.generateDerivedParameters(256);
        // initialize
        encrypt.init(true, params);
        blockSize = encrypt.getBlockSize();
        // make the buffers
        data = new byte[blockSize];
        encrypted = new byte[blockSize];
    }

    /**
     * <p>Write to the output stream.</p>
     * @param buf
     * @param off
     * @param len
     * @throws java.io.IOException
     */
    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        // encrypt all the data
        int bytesRead = 0;
        while (len - bytesRead >= blockSize - currentDataOffset) {
            // copy over the bytes to fill up the current data buffer
            for (int i = 0; blockSize > currentDataOffset; i++) {
                data[currentDataOffset] = buf[bytesRead];
                // inc offsets
                bytesRead++;
                currentDataOffset++;
            }
            // encrypt the full buffer
            encrypt.processBlock(data, 0, encrypted, 0);
            // write the data to the real output
            out.write(encrypted);
            // update the big hash
            bhm.update(encrypted, 0, blockSize);
            // reset data offset
            currentDataOffset = 0;
        }
        // copy over the remaining bytes -- might be less than a full buffer
        for (int i = 0; len > bytesRead + i; i++) {
            data[currentDataOffset] = buf[bytesRead + i];
            // inc offset that matters
            currentDataOffset++;
        }
    }

    /**
     * <p>Write bytes to the output stream.</p>
     * @param b
     * @throws java.io.IOException
     */
    @Override
    public void write(byte[] b) throws IOException {
        // write to the parent stream
        write(b, 0, b.length);
    }

    /**
     * <p>Write a byte to the output stream.</p>
     * @param b
     * @throws java.io.IOException
     */
    public void write(int b) throws IOException {
        tempBuf[0] = (byte) b;
        write(tempBuf);
    }

    /**
     * <p>Get the hash of the file written to the output stream..</p>
     * @return
     * @throws java.io.IOException
     */
    public BigHash getHash() throws IOException {
        // return the hash
        if (bh == null) {
            // flush
            try {
                flush();
            } catch (Exception e) {
            }
            try {
                close();
            } catch (Exception e) {
            }

            // return and cache the hash
            bh = BigHash.createFromBytes(bhm.finish());
        }
        return bh;
    }

    /**
     * <p>Close the output stream.</p>
     * @throws java.io.IOException
     */
    @Override
    public void close() throws IOException {
        // check the closed flag
        if (closed) {
            return;        // flag the close
        }
        closed = true;

        // calc the final block
        int paddingLength = data.length - currentDataOffset;
        for (int i = currentDataOffset; i < data.length; i++) {
            data[i] = (byte) (0xff & paddingLength);
        }
        // process the data
        encrypt.processBlock(data, 0, encrypted, 0);
        // write the data to the real output
        out.write(encrypted);
        // update the big hash
        bhm.update(encrypted, 0, blockSize);

        // close
        out.flush();
        out.close();
    }

    /**
     * <p>Flush the output stream.</p>
     * @throws java.io.IOException
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }
}
