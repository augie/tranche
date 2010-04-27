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
package org.tranche.util;

import org.tranche.security.SecurityUtil;
import java.util.Collections;
import java.util.HashMap;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.tranche.commons.TextUtil;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.hash.Base16;
import org.tranche.network.NetworkUtil;

/**
 * <p>A helper class for the code to determine if it is being run in test mode or not. If the code is run in test mode, it shouldn't spawn unneeded background threads or otherwise use resources intended solely for a live environment.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class TestUtil {

    private static final List<TestUtilListener> listeners = new LinkedList<TestUtilListener>();
    private static boolean testing = false;
    private static boolean testingTimeout = false;
    private static boolean testUsingFakeCoreServers = false;
    private static boolean testingHashSpanFixingThread = false;
    private static boolean testingTurnOffProjectFindingThread = false;
    private static boolean testingServerTracker = false;
    private static boolean testingActivityLogs = false;
    private static boolean testingServerStartupThread = false;
    private static boolean testingManualNetworkStatusTable = false;
    /**
     * <p>Flag to simulate an OutOfMemoryError.</p>
     */
    public static boolean addFileToolSimulateOOM = false;
    /**
     * <p>Flag to simulate how a tool might recover from an OutOfMemoryError.</p>
     */
    public static boolean addFileToolAutoRecoverFromOOM = false;
    private static boolean addFileToolChunkFailOnce = false;
    private static boolean addFileToolChunkFailTwice = false;
    private static boolean testingTargetHashSpan = false;
    /**
     * <p>Holds a map of hosts to Tranche urls. This is done so can unit test.</p>
     * <p>Note that there is a maximum of one host per JVM. This is done so port and other information can be changed.</p>
     * <p>When test, need more than one server. This allows us to do this.</p>
     */
    private static final Map<String, String> hostToURLMap = new HashMap();
    /**
     * <p>Map of URL (e.g., tranche://127.0.0.1:1501) to associated FlatFileTrancheServer instance from a running TestNetwork.</p>
     * <p>This is needed to test propagation in tests. If server items use remote server when propagating, might end up behind another request from client (clients and servers share remote connections), and will hence time out.</p>
     */
    private static final Map<String, FlatFileTrancheServer> URLToFFTSMap = new HashMap();

    /**
     * <p>Flag to determine whether tests are running.</p>
     * @return
     */
    public static boolean isTesting() {
        return testing;
    }

    /**
     * <p>Flag to determine whether tests are running.</p>
     * @param testing
     */
    public static void setTesting(boolean testing) {
        TestUtil.testing = testing;
    }

    /**
     * <p>Flag to simulate core servers. Tests can add servers.</p>
     * @return
     */
    public static boolean isTestUsingFakeCoreServers() {
        return testUsingFakeCoreServers;
    }

    /**
     * <p>Flag to simulate core servers. Tests can add servers.</p>
     * @param aTestUsingFakeCoreServers
     */
    public static void setTestUsingFakeCoreServers(boolean aTestUsingFakeCoreServers) {
        testUsingFakeCoreServers = aTestUsingFakeCoreServers;
    }

    /**
     * <p>Can turn the HashSpanFixingThread on or off. By default, it is off (false).</p>
     * @return
     */
    public static boolean isTestingHashSpanFixingThread() {
        return testingHashSpanFixingThread;
    }

    /**
     * <p>Can turn the HashSpanFixingThread on or off. By default, it is off (false).</p>
     * @param aTestingHashSpanFixingThread
     */
    public static void setTestingHashSpanFixingThread(boolean aTestingHashSpanFixingThread) {
        testingHashSpanFixingThread = aTestingHashSpanFixingThread;
    }

    /**
     * <p>Used to turn off project-finding thread. Useful if want to test healing thread's capability for fixing broken data blocks.</p>
     * @return
     */
    public static boolean isTestingTurnOffProjectFindingThread() {
        return testingTurnOffProjectFindingThread;
    }

    /**
     * <p>Used to turn off project-finding thread. Useful if want to test healing thread's capability for fixing broken data blocks.</p>
     * @param aTestingTurnOffProjectFindingThread
     */
    public static void setTestingTurnOffProjectFindingThread(boolean aTestingTurnOffProjectFindingThread) {
        testingTurnOffProjectFindingThread = aTestingTurnOffProjectFindingThread;
    }

    /**
     * <p>If set to true, every chunk in batch uploads will fail once: fail first time, but retry should succeed.</p>
     * @return
     */
    public static boolean isAddFileToolChunkFailOnce() {
        return addFileToolChunkFailOnce;
    }

    /**
     * <p>If set to true, every chunk in batch uploads will fail once: fail first time, but retry should succeed.</p>
     * @param aAddFileToolChunkFailOnce
     */
    public static void setAddFileToolChunkFailOnce(boolean aAddFileToolChunkFailOnce) {
        addFileToolChunkFailOnce = aAddFileToolChunkFailOnce;
    }

    /**
     * <p>If set to true, every chunk in batch uploads will fail twice: fail first time, and fail retry.</p>
     * @return
     */
    public static boolean isAddFileToolChunkFailTwice() {
        return addFileToolChunkFailTwice;
    }

    /**
     * <p>If set to true, every chunk in batch uploads will fail twice: fail first time, and fail retry.</p>
     * @param aAddFileToolChunkFailTwice
     */
    public static void setAddFileToolChunkFailTwice(boolean aAddFileToolChunkFailTwice) {
        addFileToolChunkFailTwice = aAddFileToolChunkFailTwice;
    }

    /**
     * <p>If set to true, the ServerTracker in ServerUtil will run for tests.</p>
     * @return
     */
    public static boolean isTestingServerTracker() {
        return testingServerTracker;
    }

    /**
     * <p>If set to true, the ServerTracker in ServerUtil will run for tests.</p>
     * @param aTestingServerTracker
     */
    public static void setTestingServerTracker(boolean aTestingServerTracker) {
        testingServerTracker = aTestingServerTracker;
    }

    /**
     * <p>If testing timeout, want to make sure scavengar thread can operate.</p>
     * @return
     */
    public static boolean isTestingTimeout() {
        return testingTimeout;
    }

    /**
     * <p>If testing timeout, want to make sure scavengar thread can operate.</p>
     * @param aTestingTimeout
     */
    public static void setTestingTimeout(boolean aTestingTimeout) {
        testingTimeout = aTestingTimeout;
    }

    /**
     * <p>If testing, activity logs won't be recorded unless this is set.</p>
     * @return
     */
    public static boolean isTestingActivityLogs() {
        return testingActivityLogs;
    }

    /**
     * <p>If testing, activity logs won't be recorded unless this is set.</p>
     * @param aTestingActivityLogs
     */
    public static void setTestingActivityLogs(boolean aTestingActivityLogs) {
        testingActivityLogs = aTestingActivityLogs;
    }

    /**
     * <p>If testing, the Server won't start the ServerStartupThread unless this is set.</p>
     * @return
     */
    public static boolean isTestingServerStartupThread() {
        return testingServerStartupThread;
    }

    /**
     * <p>If testing, the Server won't start the ServerStartupThread unless this is set.</p>
     * @param aTestingServerStartupThread
     */
    public static void setTestingServerStartupThread(boolean aTestingServerStartupThread) {
        testingServerStartupThread = aTestingServerStartupThread;
    }

    /**
     * <p>If set to true, then the network status table is being tested. Automatic updates to the table will be disabled.</p>
     * @return
     */
    public static boolean isTestingManualNetworkStatusTable() {
        return testingManualNetworkStatusTable;
    }

    /**
     * <p>If set to true, then the network status table is being tested. Automatic updates to the table will be disabled.</p>
     * @param aTestingManualNetworkStatusTable
     */
    public static void setTestingManualNetworkStatusTable(boolean aTestingManualNetworkStatusTable) {

        boolean prevVal = testingManualNetworkStatusTable;
        testingManualNetworkStatusTable = aTestingManualNetworkStatusTable;

        // Make sure the static components are loaded from class
        NetworkUtil.lazyLoad();

        // If changed, fire any listeners
        if (prevVal != aTestingManualNetworkStatusTable) {
            synchronized (listeners) {
                for (TestUtilListener tul : listeners) {
                    tul.changedTestingManualNetworkStatusTable();
                }
            }
        }
    }

    /**
     * <p>Add TestUtilListener to TestUtil.</p>
     * @param tul
     * @return
     */
    public static boolean addListener(TestUtilListener tul) {
        synchronized (listeners) {
            return listeners.add(tul);
        }
    }

    /**
     * <p>Remove specific TestUtilListener from TestUtil.</p>
     * @param tul
     * @return
     */
    public static boolean removeListener(TestUtilListener tul) {
        synchronized (listeners) {
            return listeners.remove(tul);
        }
    }

    /**
     * <p>Clear all TestUtilListener objects.</p>
     */
    public static void clearListeners() {
        synchronized (listeners) {
            listeners.clear();
        }
    }

    /**
     * 
     * @param title
     */
    public static final void printTitle(String title) {
        System.out.println();
        System.out.println("=============================================================================");
        System.out.println("  " + title);
        System.out.println("=============================================================================");
        System.out.println();
    }

    /**
     * <p>Add a map entry for a single server.</p>
     * <p>Holds a map of hosts to Tranche urls. This is done so can unit test.</p>
     * <p>Note that there is a maximum of one host per JVM. This is done so port and other information can be changed.</p>
     * <p>When test, need more than one server. This allows us to do this.</p>
     * <p>Only works when TestUtil.isTestingManualNetworkStatusTable() returns true. For this to work, must manually create status table.</p>
     * @param host The ostensible host name
     * @param trancheURL The actual Tranche server URL
     */
    public static void addServerToHostTestMap(String host, String trancheURL) {
        synchronized (hostToURLMap) {
            hostToURLMap.put(host, trancheURL);
        }
    }

    /**
     * <p>Returns unmodifiable map.</p>
     * <p>Holds a map of hosts to Tranche urls. This is done so can unit test.</p>
     * <p>Note that there is a maximum of one host per JVM. This is done so port and other information can be changed.</p>
     * <p>When test, need more than one server. This allows us to do this.</p>
     * <p>Only works when TestUtil.isTestingManualNetworkStatusTable() returns true. For this to work, must manually create status table.</p>
     * @return
     */
    public static Map<String, String> getServerToHostTestMap() {
        synchronized (hostToURLMap) {
            return Collections.unmodifiableMap(hostToURLMap);
        }
    }

    /**
     * <p>Clears the collection.</p>
     * <p>Holds a map of hosts to Tranche urls. This is done so can unit test.</p>
     * <p>Note that there is a maximum of one host per JVM. This is done so port and other information can be changed.</p>
     * <p>When test, need more than one server. This allows us to do this.</p>
     * <p>Only works when TestUtil.isTestingManualNetworkStatusTable() returns true. For this to work, must manually create status table.</p>
     */
    public static void clearServerToHostTestMap() {
        synchronized (hostToURLMap) {
            hostToURLMap.clear();
        }
    }

    /**
     * <p>Returns a Tranche url for an (ostensible) host. If doesn't exist, returns null.</p>
     * <p>Holds a map of hosts to Tranche urls. This is done so can unit test.</p>
     * <p>Note that there is a maximum of one host per JVM. This is done so port and other information can be changed.</p>
     * <p>When test, need more than one server. This allows us to do this.</p>
     * <p>Only works when TestUtil.isTestingManualNetworkStatusTable() returns true. For this to work, must manually create status table.</p>
     * @param host
     * @return
     */
    public static String getServerTestURL(String host) {
        synchronized (hostToURLMap) {
            return hostToURLMap.get(host);
        }
    }

    /**
     * <p>Returns the ostensible host for a Tranche server given its actual url. If doesn't exist, returns null.</p>
     * <p>Holds a map of hosts to Tranche urls. This is done so can unit test.</p>
     * <p>Note that there is a maximum of one host per JVM. This is done so port and other information can be changed.</p>
     * <p>When test, need more than one server. This allows us to do this.</p>
     * <p>Only works when TestUtil.isTestingManualNetworkStatusTable() returns true. For this to work, must manually create status table.</p>
     * @param url
     * @return
     */
    public static String getOstensibleHostForURL(String url) {
        synchronized (hostToURLMap) {
            for (String key : hostToURLMap.keySet()) {
                if (hostToURLMap.get(key).equals(url)) {
                    return key;
                }
            }
        }
        return null;
    }

    /**
     * 
     * @param dir1
     * @param dir2
     * @throws java.lang.Exception
     */
    public static final void assertDirectoriesEquivalent(File dir1, File dir2) throws Exception {
        if (!dir1.exists()) {
            throw new Exception(dir1 + " does not exist.");
        }
        if (!dir2.exists()) {
            throw new Exception(dir2 + " does not exist.");
        }
        File[] downloadedFileList = dir2.listFiles();
        File[] uploadedFileList = dir1.listFiles();

        if (uploadedFileList == null) {
            throw new NullPointerException("Upload directory file list is null.");
        }

        if (downloadedFileList == null) {
            throw new NullPointerException("Download directory file list is null.");
        }

        if (uploadedFileList.length != downloadedFileList.length) {
            throw new Exception("Should be the same number of files");
        }

        for (int i = 0; i < downloadedFileList.length; i++) {
            // check that the contents are the same
            assertFilesAreEqual(uploadedFileList[i], downloadedFileList[i]);
        }
    }

    /**
     * 
     * @param fileA
     * @param fileB
     * @throws java.lang.Exception
     */
    public static void assertFilesAreEqual(File fileA, File fileB) throws Exception {
        if (!fileB.getName().equals(fileA.getName())) {
            throw new Exception("Should be the same names");
        }
        assertFilesAreEquivalent(fileA, fileB);
    }

    /**
     * 
     * @param fileA
     * @param fileB
     * @throws java.lang.Exception
     */
    public static void assertFilesAreEquivalent(File fileA, File fileB) throws Exception {
        if (!fileA.exists()) {
            throw new Exception(fileA.getAbsolutePath() + " does not exist.");
        }
        if (!fileB.exists()) {
            throw new Exception(fileB.getAbsolutePath() + " does not exist.");
        }
        if (fileB.length() != fileA.length()) {
            throw new Exception("Should be the same size. Size of " + fileA.getName() + ": " + fileA.length() + ", Size of " + fileB.getName() + ": " + fileB.length());
        }
        byte[] md5A = SecurityUtil.hash(fileA, "md5");
        byte[] md5B = SecurityUtil.hash(fileB, "md5");
        if (!Base16.encode(md5A).equals(Base16.encode(md5B))) {
            throw new Exception("Same hashes expected.");
        }
    }

    /**
     * <p>TargetHashSpanThread only runs if true.</p>
     * @return
     */
    public static boolean isTestingTargetHashSpan() {
        return testingTargetHashSpan;
    }

    /**
     * <p>TargetHashSpanThread only runs if true.</p>
     * @param aTestingTargetHashSpan
     */
    public static void setTestingTargetHashSpan(boolean aTestingTargetHashSpan) {
        testingTargetHashSpan = aTestingTargetHashSpan;
    }

    /**
     * <p>Map of URL (e.g., tranche://127.0.0.1:1501) to associated FlatFileTrancheServer instance from a running TestNetwork.</p>
     * <p>This is needed to test propagation in tests. If server items use remote server when propagating, might end up behind another request from client (clients and servers share remote connections), and will hence time out.</p>
     * @param url
     * @param ffts
     */
    public static void setFFTSForURL(String url, FlatFileTrancheServer ffts) {
        URLToFFTSMap.put(url, ffts);
    }

    /**
     * <p>Map of URL (e.g., tranche://127.0.0.1:1501) to associated FlatFileTrancheServer instance from a running TestNetwork.</p>
     * <p>This is needed to test propagation in tests. If server items use remote server when propagating, might end up behind another request from client (clients and servers share remote connections), and will hence time out.</p>
     * @param url
     * @return
     */
    public static FlatFileTrancheServer getFFTSForURL(String url) {
        FlatFileTrancheServer ffts = URLToFFTSMap.get(url);
        return ffts;
    }

    /**
     * <p>Map of URL (e.g., tranche://127.0.0.1:1501) to associated FlatFileTrancheServer instance from a running TestNetwork.</p>
     * <p>This is needed to test propagation in tests. If server items use remote server when propagating, might end up behind another request from client (clients and servers share remote connections), and will hence time out.</p>
     * @param url
     * @return
     */
    public static FlatFileTrancheServer removeFFTSForURL(String url) {
        return URLToFFTSMap.remove(url);
    }

    /**
     * <p>Map of URL (e.g., tranche://127.0.0.1:1501) to associated FlatFileTrancheServer instance from a running TestNetwork.</p>
     * <p>This is needed to test propagation in tests. If server items use remote server when propagating, might end up behind another request from client (clients and servers share remote connections), and will hence time out.</p>
     */
    public static void clearFFTSForURL() {
        URLToFFTSMap.clear();
    }

    /**
     * <p>Prints out breadth-first directory structure for humans. Nice to see files quickly.</p>
     * @param root
     */
    public static void printRecursiveDirectoryStructure(File root) {
        recursivePrintRecursiveDirectoryStructure(new File[] {root}, 0);
    }

    /**
     * <p>Prints out breadth-first directory structure for humans. Nice to see files quickly.</p>
     * @param files
     * @param indent Number of spaces to indent files/directories for each subdirectory
     */
    private static void recursivePrintRecursiveDirectoryStructure(File[] files, int indent) {

        // Build up indentation
        StringBuffer indentation = new StringBuffer();
        for (int i = 0; i < indent; i++) {
            indentation.append(" ");
        }

        // Recursively print
        for (File f : files) {
            if (f.isDirectory()) {
                System.out.println(indentation.toString() + f.getName() + "/");
                recursivePrintRecursiveDirectoryStructure(f.listFiles(), indent + 2);
            } else {
                System.out.println(indentation.toString() + f.getName() + ": " + TextUtil.formatBytes(f.length()));
            }
        }
    }
}
