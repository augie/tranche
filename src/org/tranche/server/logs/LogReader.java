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
package org.tranche.server.logs;

import java.io.*;

/**
 * Reads in a log file, iterating through entries
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class LogReader {
    
    /**
     * Buffer any read by hasNext() so next() can access.
     */
    private LogEntry buffer = null;   
    private File log;
    private DataInputStream in;
    
    /**
     * Create a log reader opening up a log file.
     * @param log
     * @throws java.lang.Exception
     */
    public LogReader(File log) throws Exception {
        this.log = log;
        in = new DataInputStream(new FileInputStream(log));
    }
    

    /**
     * Returns true if there is another log entry to read; otherwise false.
     * @return
     * @throws java.lang.Exception
     */
    public boolean hasNext() throws Exception {
        nextInternal();
        return buffer != null;
    }
    
    /**
     * Return the next log entry from the file.
     * @return
     * @throws java.lang.Exception
     */
    public LogEntry next() throws Exception {
        if (buffer != null) {
            LogEntry swap = buffer;
            buffer = null;
            return swap;
        }
        
        nextInternal();
        return buffer;
    }
    
    
    private int stackCount = 0;
    private synchronized LogEntry nextInternal() throws Exception {
        try {
            buffer = null;
            
            // Bytes to read
            long remaining = in.readLong();
            
            // Read the checksum
            byte checksum = in.readByte();
            
            // Read the data
            byte[] bytes = new byte[(int)remaining-1];
            in.readFully(bytes);
            
            // Perform the checksum
            if (!LogUtil.validateXORChecksum(checksum,bytes)) {
                throw new ChecksumException();
            }
            
            buffer = LogEntry.createFromBytes(bytes);
            
        } catch (ChecksumException cex) {
            
            System.err.println("Found checksum exception reading server log: "+cex.getMessage());
            cex.printStackTrace(System.err);
            
            // First check number of consecutive ChecksumExceptions, friendly to stack
            stackCount++;
            if (stackCount > 100) {
                throw new IOException("Bad file, "+stackCount+" consecutive bad entries.");
            }
            
            // Throw that one away, try next
            return nextInternal();
        } catch (EOFException eof) {
            // Nothing, returns null
            buffer = null;
        } finally {
            stackCount = 0;
        }
        
        return null;
    }
    
    /**
     * Close file that the log reader has opened.
     * @throws java.lang.Exception
     */
    public void close() throws Exception {
        this.in.close();
    }
}

/**
 * Signature exception if checksum fails.
 */
class ChecksumException extends IOException {
    /**
     * Creates a checksum exception for when a checksum fails.
     */
    public ChecksumException() {
        super("The checksum does not match the payload.");
    }
}
