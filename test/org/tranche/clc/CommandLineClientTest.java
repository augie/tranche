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
package org.tranche.clc;

import org.tranche.util.*;
import org.tranche.util.IOUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import org.tranche.flatfile.FlatFileTrancheServer;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class CommandLineClientTest extends TrancheTestCase {
    
    /**
     * Doesn't really test anything per say. Needs a customized inputstream that'll let us feed one command at a time. Then we can start the server, check that it is on-line, stop the server, and verify that it is off-line.
     */
    public void testCommandLineClient() throws Exception {
        File dir = TempFileUtil.createTemporaryDirectory();
        
        // start up the command line client
        FlatFileTrancheServer ffdfs = null;
        try {
            // set up the dfs
            ffdfs = new FlatFileTrancheServer(dir.getCanonicalPath());
            
            // buffer some commands
            String commands = new String("server command=stop\nserver command=start\nserver command=stop\n");
            ByteArrayInputStream bais = new ByteArrayInputStream(commands.getBytes());
            
            // start up a new command-line client
            TrancheServerCommandLineClient clc = new TrancheServerCommandLineClient(ffdfs, bais, System.out);
            // start the client
            clc.start();
            // join it
            System.out.println("Starting command-line client.");
            clc.join();
            System.out.println("Command-line client exited.");
        } finally {
            IOUtil.safeClose(ffdfs);
        }
    }
}
