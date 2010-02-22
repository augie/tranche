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

import java.io.File;
import java.io.IOException;
import org.tranche.flatfile.FlatFileTrancheServer;

/**
 * <p>Used for persistent files and directories.</p>
 * @author Bryan Smith <bryanesmith at gmail.com>
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class PersistentServerFileUtil {

    private static File persistentDirectory = new File(FlatFileTrancheServer.getDefaultHomeDir());

    /**
     * 
     */
    private PersistentServerFileUtil() {
    }

    /**
     * <p>Get a persistent directory position.</p>
     * @return
     */
    public static File getPersistentDirectory() {
        return persistentDirectory;
    }

    /**
     * <p>Set persistent directory.</p>
     * @param persistentDirectory
     */
    public static void setPersistentDirectory(File persistentDirectory) {
        PersistentServerFileUtil.persistentDirectory = persistentDirectory;
    }

    /**
     * <p>Returns a file with the given name in the persistent directory. If the file does not exist, it will be created.</p>
     * @param fileName The name of the file.
     * @return File
     * @throws java.io.IOException
     */
    public synchronized static File getPersistentFile(String fileName) throws IOException {
        if (!persistentDirectory.exists()) {
            persistentDirectory.mkdirs();
        }
        File file = new File(getPersistentDirectory(), fileName);
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Could not create file named " + fileName + ".");
        }
        return file;
    }

    /**
     * 
     * @param directoryName
     * @return
     * @throws IOException
     */
    public synchronized static File getPersistentDirectory(String directoryName) throws IOException {
        if (!persistentDirectory.exists()) {
            persistentDirectory.mkdirs();
        }
        File file = new File(getPersistentDirectory(), directoryName);
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Could not create directory named " + directoryName + ".");
        }
        return file;
    }
}
