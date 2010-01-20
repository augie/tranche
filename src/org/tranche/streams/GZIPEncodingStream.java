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

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import org.tranche.hash.BigHash;
import org.tranche.hash.BigHashMaker;

/**
 * <p>A stream to encode a stream of data in GZIP and outputting to a given output stream.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class GZIPEncodingStream extends OutputStream {
    // track a big hash
    private BigHashMaker bhm = new BigHashMaker();
    // the big hash
    private BigHash bh = null;
    // the output stream
    private WrappedOutputStream wos;
    // the gzip stream
    private GZIPOutputStream gos;
    private boolean closed = false;
    byte[] tempBuf = new byte[1];

    /**
     * 
     * @param out
     * @throws java.io.IOException
     */
    public GZIPEncodingStream(OutputStream out) throws IOException {
        wos = new WrappedOutputStream(out, bhm);
        gos = new GZIPOutputStream(wos);
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
        // write to the parent stream
        gos.write(buf, off, len);
    }

    /**
     * <p>Write bytes to the output stream.</p>
     * @param b
     * @throws java.io.IOException
     */
    @Override
    public void write(byte[] b) throws IOException {
        // write to the parent stream
        gos.write(b);
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
     * <p>Get the hash of the file output to the stream.</p>
     * @return
     */
    public BigHash getHash() {
        // flush
        try {
            gos.flush();
        } catch (Exception e) {
        }
        try {
            wos.flush();
        } catch (Exception e) {
        }
        // return the hash
        if (bh == null) {
            bh = BigHash.createFromBytes(bhm.finish());
        }
        return bh;
    }

    /**
     * <p>Flush and close the output stream.</p>
     * @throws java.io.IOException
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        gos.flush();
        try {
            gos.finish();
        } catch (Exception e) {
        }
        try {
            gos.close();
        } catch (Exception e) {
        }
        wos.flush();
        wos.close();
    }

    /**
     * <p>Flush the output stream.</p>
     * @throws java.io.IOException
     */
    @Override
    public void flush() throws IOException {
        gos.flush();
        wos.flush();
    }
}
