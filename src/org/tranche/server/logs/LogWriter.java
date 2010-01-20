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
 * Class for creating a simple log writer that writes log entries out to a file.
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class LogWriter {
    
    private File logFile;
    private DataOutputStream dos;
    
    /**
     * Create a log writer with a file to write log entries out to.
     * @param logFile
     * @throws java.lang.Exception
     */
    public LogWriter(File logFile) throws Exception {
        this.logFile = logFile;
        this.dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(logFile)));
    }
    
    /**
     * Write an a LogEntry to the log.
     * @param entry
     * @throws java.lang.Exception
     */
    public void writeEntry(LogEntry entry) throws Exception {
        
        // First, build array for payload
        byte[] payload = entry.toByteArray();
        
        // Next, get checksum for payload
        byte checksum = LogUtil.createXORChecksum(payload);
        
        // Calculate the size (includes the checksum and data)
        long size = 1+entry.length();
        
        // Write the size
        this.dos.writeLong(size);
        
        // Write the checksum
        this.dos.writeByte(checksum);
        
        // Write the data
        this.dos.write(payload);
        this.dos.flush();
    }
    
    /**
     * Closes the logging stream
     * @throws java.lang.Exception
     */
    public void close() throws Exception {
        if (this.dos != null) {
            this.dos.flush();
            this.dos.close();
        }
    }
}
