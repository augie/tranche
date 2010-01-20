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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.tranche.hash.Base16;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 * <p>A memory sensitive Set implementation for managing ProjectFileParts. If too many parts exist, some are stored to disk. Automatically sorted.</p>
 * @author Jayson Falker - jfalkner@umich.edu
 */
public class DiskBackedProjectFilePartSet implements Set<ProjectFilePart> {
    // the file where the parts are kept

    private File fileA;
    private File fileB;
    private boolean toggle = true;
    // an in memory buffer
    private TreeSet<ProjectFilePart> buf = new TreeSet();
    // set the max buf size
    private int maxBufferSize = 10000;
    // keep track of the size
    private int size = 0;
    private boolean isDestroyed = false;

    /**
     * <p>Returns the file that is being used.</p>
     * @return
     */
    private File getToggleFile() {
        if (toggle) {
            return fileA;
        } else {
            return fileB;
        }
    }

    /**
     * Help prevent amassing large num. temp files
     */
    @Override()
    protected void finalize() {
        destroy();
    }

    /**
     * <p>Explicity destroy the disk-backed collection. If don't, GC will garbage collect OR temp dir cleaned out when tool reran.</p>
     */
    public void destroy() {
        if (isDestroyed) {
            return;
        }
        isDestroyed = true;
        IOUtil.safeDelete(fileA);
        IOUtil.safeDelete(fileB);
    }

    /**
     * <p>Creates a new instance of DiskBackedProjectFilePartSet</p>
     */
    public DiskBackedProjectFilePartSet() {
        // make two files to use
        fileA = TempFileUtil.createTemporaryFile();
        fileB = TempFileUtil.createTemporaryFile();
    }

    /**
     * <p>Throws RuntimeException</p>
     * @param o
     * @return
     */
    public synchronized boolean contains(Object o) {
        throw new RuntimeException("Contains method not implemented.");
    }

    /**
     * <p>Throws RuntimeException</p>
     * @param o
     * @return
     */
    public synchronized boolean remove(Object o) {
        throw new RuntimeException("remove() method not implemented.");
    }

    /**
     * <p>Returns an array of project file parts.</p>
     * @param <T>
     * @param a
     * @return
     */
    public synchronized <T> T[] toArray(T[] a) {
        // fall back on teh file
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(getToggleFile());
            br = new BufferedReader(fr);
            for (String line = br.readLine(); line != null; line = br.readLine()) {
//                String[] parts = line.split("~");
//                // make a new pfp
//                ProjectFilePart pfp = new ProjectFilePart(parts[0].replace("\\~", "~"), BigHash.createHashFromString(parts[1]), Base16.decode(parts[2]));
                ProjectFilePart pfp = convertToProjectFilePart(line);
                // buffer it
                buf.add(pfp);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtil.safeClose(br);
            IOUtil.safeClose(fr);
        }
        return (T[]) buf.toArray(a);
    }

    /**
     * <p>Throws RuntimeException</p>
     * @param c
     * @return
     */
    public synchronized boolean addAll(Collection<? extends ProjectFilePart> c) {
        throw new RuntimeException("addAll() isn't implemented.");
    }

    /**
     * <p>Adds a project file part set to the list of project file part sets.</p>
     * @param o
     * @return
     */
    public synchronized boolean add(ProjectFilePart o) {
        // add one
        size++;
        // add to the buffer
        boolean added = buf.add(o);
        // if not too much, return
        if (buf.size() < maxBufferSize) {
            return added;
        }
        // save the data
        saveData();
        // return true
        return true;
    }

    /**
     * <p>Saves the projects to disk.</p>
     * @throws java.lang.RuntimeException
     */
    private synchronized void saveData() throws RuntimeException {
        // if too much, serialize
        FileReader fr = null;
        BufferedReader br = null;
        // write out to the other file
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            // read from the current toggle file
            fr = new FileReader(getToggleFile());
            br = new BufferedReader(fr);
            // change files
            toggle = !toggle;
            fw = new FileWriter(getToggleFile());
            bw = new BufferedWriter(fw);
            // sort the items
            ProjectFilePart[] pfps = (ProjectFilePart[]) buf.toArray(new ProjectFilePart[0]);
            Arrays.sort(pfps);
            int pfpsIndex = 0;
            // read/copy the lines
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                // if it is empty, skip
                if (line.trim().equals("")) {
                    continue;
                // load the project file part
                }
                ProjectFilePart pfp = convertToProjectFilePart(line);
                // check which is less
                if (pfpsIndex >= pfps.length || pfp.compareTo(pfps[pfpsIndex]) <= 0) {
                    bw.write(convertToStringLine(pfp));
                } else if (pfp.compareTo(pfps[pfpsIndex]) > 0) {
                    // write all of the ones that are less
                    while (pfpsIndex < pfps.length && pfp.compareTo(pfps[pfpsIndex]) > 0) {
                        bw.write(convertToStringLine(pfps[pfpsIndex]));
                        pfpsIndex++;
                    }
                    // write out the current entry
                    bw.write(convertToStringLine(pfp));
                } else {
                    System.out.println("Skipping " + pfp.getRelativeName());
                }
            }
            // write the rest
            for (; pfpsIndex < pfps.length; pfpsIndex++) {
                bw.write(convertToStringLine(pfps[pfpsIndex]));
            }
            // clear the buffer
            buf.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // close the input
            IOUtil.safeClose(br);
            IOUtil.safeClose(fr);
            // flush the output
            try {
                bw.flush();
            } catch (Exception e) {
            }
            try {
                fw.flush();
            } catch (Exception e) {
            }
            // close the output
            IOUtil.safeClose(bw);
            IOUtil.safeClose(fw);
        }
    }

    /**
     * <p>Helper method to convert a ProjectFilePart</p>
     * @param pfp
     * @return
     */
    private String convertToStringLine(ProjectFilePart pfp) {
        return Base16.encode(pfp.getRelativeName().getBytes()) + "~" + Base16.encode(pfp.getHash().toByteArray()) + "~" + Base16.encode(pfp.getPadding()) + "~\n";
    }

    /**
     * <p>Helper method to convert a line to a ProjectFilePart</p>
     * @param line
     * @return
     */
    private ProjectFilePart convertToProjectFilePart(String line) {
        String[] parts = line.split("~");
        // reality check
        if (parts.length < 2) {
            throw new RuntimeException("Invalid serialized line \"" + line + "\"");
        }
        // get the padding if it exists
        byte[] pad = new byte[0];
        if (parts.length > 2) {
            pad = Base16.decode(parts[2]);
        // construct the file part
        }
        ProjectFilePart pfp = new ProjectFilePart(new String(Base16.decode(parts[0])), BigHash.createHashFromString(parts[1]), pad);
        return pfp;
    }

    /**
     * <p>Throws RuntimeException</p>
     * @param c
     * @return
     */
    public synchronized boolean containsAll(Collection<?> c) {
        throw new RuntimeException("containsAll() isn't implemented.");
    }

    /**
     * <p>Throws RuntimeException</p>
     * @param c
     * @return
     */
    public synchronized boolean removeAll(Collection<?> c) {
        throw new RuntimeException("removeAll() isn't implemented.");
    }

    /**
     * <p>Throws RuntimeException</p>
     * @param c
     * @return
     */
    public synchronized boolean retainAll(Collection<?> c) {
        throw new RuntimeException("retainAll() isn't implemented.");
    }

    /**
     * <p>Resets the object.</p>
     */
    public synchronized void clear() {
        // clear the buf
        buf.clear();
        // clear the file
        IOUtil.safeDelete(getToggleFile());
        // reset the size
        size = 0;
    }

    /**
     * <p>Returns whether the set does not contain any project file parts.</p>
     * @return
     */
    public synchronized boolean isEmpty() {
        return size == 0;
    }

    /**
     * <p>Returns an iterator of the set of project file parts.</p>
     * @return
     */
    public synchronized Iterator<ProjectFilePart> iterator() {
        // make a memory sensitive iterator
        Iterator diskIterator = new Iterator() {

            FileReader fr = null;
            BufferedReader br = null;
            // the buffere part
            ProjectFilePart pfp = null;

            private void lazyLoad() {
                if (fr == null) {
                    try {
                        // save the data
                        saveData();
                        // make the readers
                        fr = new FileReader(getToggleFile());
                        br = new BufferedReader(fr);
                    } catch (FileNotFoundException ex) {
                        throw new RuntimeException("Can't make iterator", ex);
                    }
                }
            }

            private ProjectFilePart useBuffer() {
                lazyLoad();
                // no part is buffered, buffer one
                if (pfp == null) {
                    try {
                        // read the next line
                        String line = br.readLine();
                        // null check
                        if (line == null) {
                            return null;
                        }
                        pfp = convertToProjectFilePart(line);
                    } catch (IOException ex) {
                        throw new RuntimeException("Can't read next", ex);
                    }
                }
                // return the part
                return pfp;
            }

            public Object next() {
                ProjectFilePart p = useBuffer();
                // reset the buffer
                pfp = null;
                // return the part
                return p;
            }

            public void remove() {
                throw new RuntimeException("Remove isn't implemented.");
            }

            public boolean hasNext() {
                boolean hasNext = useBuffer() != null;
                // auto-teardown at the end
                if (!hasNext) {
                    IOUtil.safeClose(br);
                    IOUtil.safeClose(fr);
                }
                return hasNext;
            }
        };
        // return the iterator
        return diskIterator;
    }

    /**
     * <p>Returns the number of project file parts.</p>
     * @return
     */
    public synchronized int size() {
        return size;
    }

    /**
     * <p>Returns an array of project file parts.</p>
     * @return
     */
    public synchronized Object[] toArray() {
        ProjectFilePart[] array = toArray(new ProjectFilePart[0]);
        return array;
    }

    /**
     * <p>Returns the maximum size.</p>
     * @return
     */
    public int getMaxBufferSize() {
        return maxBufferSize;
    }

    /**
     * <p>Sets the maximum buffer size.</p>
     * @param maxBufferSize
     */
    public void setMaxBufferSize(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }
}
