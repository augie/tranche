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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.tranche.util.IOUtil;

/**
 *<p>A helper class that extends FileOutputStream and automatically deletes the file that it is reading once the close() method is invoked.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class DeleteFileOnExitFileInputStream extends FileInputStream {

    // reference the underlying file
    private File file;

    /**
     * <p>Creates a new FileOutputStream that uses the File specified to read from. When this object is closed it will automatically delete the underlying file.</p>
     * @param file The File to read from.
     * @throws java.io.FileNotFoundException If the specified file does not exist.
     */
    public DeleteFileOnExitFileInputStream(File file) throws FileNotFoundException {
        super(file);
        this.file = file;
    }

    /**
     * <p>Closes the parent FileOutputStream then deletes the file that was read from.</p>
     * @throws java.io.IOException Should any IOException occur.
     */
    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            IOUtil.safeDelete(file);
        }
    }
}
