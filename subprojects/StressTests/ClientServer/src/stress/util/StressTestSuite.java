/*
 * TestSuite.java
 *
 * Created on October 6, 2007, 9:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package stress.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.tranche.add.AddFileTool;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.util.IOUtil;
import stress.client.Configuration;
import stress.client.StressClient;

/**
 * Reads in the stress test configuration file, verifies contents. Acts as an iterator, allowing client to get each StressTest to run.
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 * @author Adam  Giacobbe <agiac@umich.edu>
 */
public class StressTestSuite {

    // This configuration file. Read in to create test suite!
    // Collection of stress test.
    private static List<StressTest> tests = new ArrayList();
    private static boolean isLazyLoaded = false;

    /**
     * Loads the test suite from file.
     */
    private static void lazyLoad() throws Exception {
        if (isLazyLoaded) {
            return;
        }
        isLazyLoaded = true;

        final File stressTestConfig = Configuration.getStressTestConfigFile();

        // Read in and parse conf file, creating collection of stress tests
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(stressTestConfig));

            String nextLine;
            boolean isReadingInTest = false;
            int line = 0;
            StressTest next = null;
            while ((nextLine = in.readLine()) != null) {
                line++;

                if (nextLine.contains("/*")) {
                    while (!nextLine.contains("*/")) {
                        nextLine = in.readLine();
                    }

                    // Fix input so include anything after long comment
                    nextLine = nextLine.substring(nextLine.indexOf("*/") + 2);
                }

                // Throw away comments
                if (nextLine.startsWith("#")) {
                    continue;
                }
                int pos = nextLine.indexOf("#");

                if (pos != -1) {
                    nextLine = nextLine.substring(0, pos);
                }

                // Throw away all white space on left and right
                nextLine = nextLine.trim();

                if (nextLine.equals("")) {
                    continue;
                }
                if (nextLine.startsWith("test") && nextLine.endsWith("{")) {
                    if (isReadingInTest) {
                        throw new RuntimeException("Already reading in a test, error on line " + line);
                    }
                    isReadingInTest = true;
                    next = new StressTest();

                } else if (nextLine.equals("}")) {
                    if (!isReadingInTest) {
                        throw new RuntimeException("No test for closing \"}\", error on line " + line);
                    }
                    if (!next.isCorrect()) {
                        throw new RuntimeException("Problem with test ending on line " + line + ". Make sure has all mandatory properties.");
                    }
                    isReadingInTest = false;
                    tests.add(next);

                } else if (nextLine.startsWith("use_batch")) {
                    String[] tokens = nextLine.split("=");
//                    boolean useBatch = AddFileTool.DEFAULT_USE_BATCH;
                    boolean useBatch = false;
                    try {
                        useBatch = Boolean.parseBoolean(tokens[1].trim());
                    } catch (Exception ex) {
                        throw new Exception("Can't parse boolean value \"" + tokens[1] + "\", line " + line);
                    }
                    next.setUseBatch(useBatch);
                } else if (nextLine.startsWith("client_count")) {
                    String[] tokens = nextLine.split("=");
                    int count = 0;
                    try {
                        count = Integer.parseInt(tokens[1].trim());
                    } catch (NumberFormatException ex) {
                        throw new NumberFormatException("Can't parse number \"" + tokens[1] + "\", line " + line);
                    }
                    next.setClientCount(count);
                } else if (nextLine.startsWith("files")) {
                    String[] tokens = nextLine.split("=");
                    int count = 0;
                    try {
                        count = Integer.parseInt(tokens[1].trim());
                    } catch (NumberFormatException ex) {
                        throw new NumberFormatException("Can't parse number \"" + tokens[1] + "\", line " + line);
                    }
                    next.setFiles(count);
                } else if (nextLine.startsWith("max_file_size")) {
                    String[] tokens = nextLine.split("=");
                    long size = 0;
                    try {
                        size = Long.parseLong(tokens[1].trim());
                    } catch (NumberFormatException ex) {
                        throw new NumberFormatException("Can't parse number \"" + tokens[1] + "\", line " + line);
                    }
                    next.setMaxFileSize(size);
                } else if (nextLine.startsWith("min_project_size")) {
                    String[] tokens = nextLine.split("=");
                    long size = 0;
                    try {
                        size = Long.parseLong(tokens[1].trim());
                    } catch (NumberFormatException ex) {
                        throw new NumberFormatException("Can't parse number \"" + tokens[1] + "\", line " + line);
                    }
                    next.setMinProjectSize(size);
                } else if (nextLine.startsWith("max_project_size")) {
                    String[] tokens = nextLine.split("=");
                    long size = 0;
                    try {
                        size = Long.parseLong(tokens[1].trim());
                    } catch (NumberFormatException ex) {
                        throw new NumberFormatException("Can't parse number \"" + tokens[1] + "\", line " + line);
                    }
                    next.setMaxProjectSize(size);
                } else if (nextLine.startsWith("delete_chunks_randomly")) {
                    String[] tokens = nextLine.split("=");
                    boolean shouldDelete = false;
                    try {
                        shouldDelete = Boolean.parseBoolean(tokens[1].trim());
                        next.setIsRandomlyDeleteChunks(shouldDelete);
                    } catch (Exception ex) {
                        throw new IOException("Exception parsing delete_files parameter in test suite configuration! What is " + shouldDelete);
                    }

                } else if (nextLine.startsWith("use_dbu_cache")) {
                    String[] tokens = nextLine.split("=");
                    boolean useCache = DataBlockUtil.DEFAULT_USE_CACHE;
                    try {
                        useCache = Boolean.parseBoolean(tokens[1].trim());
                        next.setUseDataBlockCache(useCache);
                    } catch(Exception ex) {
                        // Could be an I/O error or bad parse!
                        throw ex;
                    }
                }else {
                    throw new RuntimeException("What does \"" + nextLine + "\" mean?, error in line " + line);
                }

            } // Reading in/parsing config file

            // Make sure user closed off last test
            if (isReadingInTest) {
                throw new RuntimeException("No closing bracket for last test, error on line " + line);
            }
        } finally {
            IOUtil.safeClose(in);
        }
    }

    /**
     * Returns true if more tests remain, else returns false.
     */
    public static boolean hasNext() throws Exception {
        lazyLoad();
        return !tests.isEmpty();
    }

    /**
     * Returns next test. Use hasNext() before to determine whether or not there is a next test.
     */
    public static StressTest getNext() throws Exception {
        lazyLoad();

        StressTest next = null;
        if (hasNext()) {
            next = tests.remove(0);
        }

        return next;
    }
}
