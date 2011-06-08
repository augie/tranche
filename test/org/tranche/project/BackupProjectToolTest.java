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
package org.tranche.project;

import org.tranche.util.*;
import java.io.File;
import org.tranche.exceptions.TodoException;
import org.tranche.flatfile.DataBlockUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class BackupProjectToolTest extends TrancheTestCase {

    long dataSize = DataBlockUtil.getMaxChunkSize() * 2;

    public void testProjectBackup() throws Exception {
        // need to update to use new connection scheme
        if (true) {
            throw new TodoException();
        }
//        BigHash projectHash;
//        BigInteger partitionSize;
//        String rootLocation;
//
//        // the add project directory
//        File addDir = TempFileUtil.createTemporaryDirectory();
//
//        // the get project directory
//        File getDir = TempFileUtil.createTemporaryDirectory();
//
//        // the verify project directory
//        File verifyDir = TempFileUtil.createTemporaryDirectory();
//
//        // set up a new ffdfs
//        FlatFileTrancheServer ffts = null;
//
//        // wrap the DFS using the custom server
//        Server server = null;
//
//        // connect to this server using a remote client
//        TrancheServer rdfs = null;
//
//        try {
//            // create and add a project of random data
//            {
//                // make up some random content
//                File directory = new File(addDir, "project");
//                directory.mkdirs();
//
//                // the files to add
//                File[] files = new File[3];
//                for (int i=0;i<files.length;i++) {
//                    files[i] = new File(directory, i+".txt");
//                    Utils.makeTempFile(Utils.makeRandomData((int)(dataSize*Math.random())), files[i]);
//                }
//
//                // set up a new ffdfs
//                ffts = new FlatFileTrancheServer(addDir);
//
//                // register the default user
//                User user = DevUtil.getDevUser();
//
//                ffts.getConfiguration().addUser(user);
//
//                // wrap the DFS using the custom XML server
//                server = new Server(ffts, DevUtil.getDefaultTestPort());
//                server.start();
//
//                // set up the servers for the AFT
//                ServerUtil.setUseCoreServers(false);
//                ServerUtil.isServerOnline(IOUtil.createURL(server.getHostName(), server.getPort(), false));
//
//                // add the data
//                AddFileTool aft = new AddFileTool(DevUtil.getDevAuthority(), DevUtil.getDevPrivateKey());
//
//                // set the dfs
//                aft.setTitle("The title.");
//                aft.setDescription("The description.");
//
//                // register a command-line listener
//                CommandLineAddFileToolListener claftl = new CommandLineAddFileToolListener(System.out);
//                aft.addListener(claftl);
//
//                // check that the hashes match
//                projectHash = aft.addFile(directory);
//            }
//
//            // backup the project
//            {
//                // random partition size
//                partitionSize = new BigInteger(String.valueOf((long) Math.floor(dataSize + Math.random()*dataSize)));
//
//                // create the backup project tool
//                BackupProjectTool bpt = new BackupProjectTool(projectHash.toString(), partitionSize, getDir);
//
//                // use the create flat file tranche server
//                bpt.downloadFromServers.add(ffts);
//
//                // run the tool
//                bpt.run();
//            }
//
//            // get the project from the backup location
//            {
//                ArrayList <Server> servers = new ArrayList();
//                ArrayList <String> serversToUse = new ArrayList();
//
//                try {
//                    for (String partition : getDir.list()) {
//                        // set up a new ffdfs
//                        ffts = new FlatFileTrancheServer(getDir + "\\" + partition);
//
//                        // wrap the DFS using the custom XML server
//                        Server s = new Server(ffts, DevUtil.getDefaultTestPort() + servers.size() + 1);
//                        s.start();
//                        servers.add(s);
//
//                        // connect to this server using a remote client
//                        rdfs = IOUtil.connect(IOUtil.createURL(server.getHostName(), server.getPort(), false));
//                        rdfs.close();
//
//                        serversToUse.add(IOUtil.createURL(ffts));
//                    }
//
//                    // use the get file tool with validation to download the project
//                    GetFileTool gft = new GetFileTool();
//                    gft.setServersToUse(serversToUse);
//                    gft.setHash(projectHash);
//                    gft.setTrustedCertificates(new X509Certificate[]{DevUtil.getDevAuthority()});
//                    gft.setValidate(true);
//
//                    // download to a temporary directory
//                    gft.getDirectory(verifyDir);
//
//                    File projectDirectory = new File(addDir + "\\project");
//                    if (!projectDirectory.exists()) {
//                        projectDirectory.mkdirs();
//                    }
//                    File verifyDirectory = new File(verifyDir + "\\project");
//                    if (!verifyDirectory.exists()) {
//                        verifyDirectory.mkdirs();
//                    }
//
//                    // the two folders should have the same number of files
//                    assertEquals("Same number of files expected.", projectDirectory.listFiles().length, verifyDirectory.listFiles().length);
//
//                    // create a sorted list of files
//                    File[] addFiles = sortList(projectDirectory.listFiles());
//                    File[] verifyFiles = sortList(verifyDirectory.listFiles());
//
//                    InputStream in;
//                    for (int i = 0; i < addFiles.length; i++) {
//
//                        // assert true that the file names are the same
//                        assertEquals("Same file names expected.", addFiles[i].getName(), verifyFiles[i].getName());
//
//                        in = new FileInputStream(addFiles[i]);
//                        byte[] addData = new byte[in.available()];
//                        in.read(addData);
//
//                        in = new FileInputStream(verifyFiles[i]);
//                        byte[] verifyData = new byte[in.available()];
//                        in.read(verifyData);
//
//                        assertEquals("Same data length expected.", addData.length, verifyData.length);
//
//                        for (int j = 0; j < addData.length; j++) {
//                            assertEquals("Same data expected.", addData[j], verifyData[j]);
//                        }
//                    }
//                } finally {
//                    for (Server s : servers) {
//                        IOUtil.safeClose(s);
//                    }
//                }
//            }
//        } finally {
//            IOUtil.safeClose(rdfs);
//            IOUtil.safeClose(server);
//
//            // attempt to clean up
//            IOUtil.recursiveDeleteWithWarning(addDir);
//            IOUtil.recursiveDeleteWithWarning(getDir);
//            IOUtil.recursiveDeleteWithWarning(verifyDir);
//        }
    }

    private File[] sortList(File[] f) {
        File[] returnArray = f;

        // basic bubble sort for sorting by file name
        boolean isSorted;
        File tempVariable;
        int numberOfTimesLooped = 0;

        do {
            isSorted = true;

            for (int i = 1; i < f.length - numberOfTimesLooped; i++) {
                if (f[i].getName().compareTo(f[i - 1].getName()) < 0) {
                    tempVariable = f[i];
                    f[i] = f[i - 1];
                    f[i - 1] = tempVariable;

                    isSorted = false;
                }
            }

            numberOfTimesLooped++;
        } while (!isSorted);

        return returnArray;
    }
}
