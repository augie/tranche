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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.Set;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;

/**
 * <p>This represents a special file on Tranche that encapsulates the information about a directory upload.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ProjectFile {

    /**
     * <p>The default project file name</p>
     */
    public static final String DEFAULT_PROJECT_FILE_NAME = "project.pf";
    /**
     * <p>DO NOT CHANGE -- EVER</p>
     */
    public static final String OLD_PROJECT_FILE_NAME = "project.pf";
    /**
     * <p>Identifies the first project file version.</p>
     */
    public static final String VERSION_ONE = "1";
    /**
     * <p>Identifies the second project file version.</p>
     * <p>The second version includes padding for each project file part.</p>
     */
    public static final String VERSION_TWO = "2";
    /**
     * <p>Identifies the third project file version.</p>
     * <p>The third version discludes the name and description.</p>
     */
    public static final String VERSION_THREE = "3";
    /**
     * <p>Identifies the fourth project file version.</p>
     * <p>The fourth version includes the license hash, name, and description -- need to keep these for encrypted files where the uploader does not want the title and description to be public.</p>
     */
    public static final String VERSION_FOUR = "4";
    /**
     * <p>Identifies the latest project file version.</p>
     */
    public static final String VERSION_LATEST = VERSION_FOUR;
    private String version = VERSION_LATEST,  name = "",  description = "";
    private BigHash licenseHash = null;
    private BigInteger size = BigInteger.ZERO;
    private Set<ProjectFilePart> parts = new DiskBackedProjectFilePartSet();;
    private boolean isSizeSet = false;

    /**
     * <p>Gets the version of this project file.</p>
     * @return
     */
    public String getVersion() {
        return version;
    }

    /**
     * <p>Sets the version of this project file.</p>
     * @param version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * <p>Gets the description for this project file.</p>
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * <p>Sets the description of this projet file.</p>
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * <p>Sets the name for this project file.</p>
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>Gets the name for this project file.</p>
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @param licenseHash
     */
    public void setLicenseHash(BigHash licenseHash) {
        this.licenseHash = licenseHash;
    }

    /**
     *
     * @return
     */
    public BigHash getLicenseHash() {
        return licenseHash;
    }

    /**
     * <p>Gets the size on disk in bytes of the directory that this object represents.</p>
     * @return
     */
    public BigInteger getSize() {
        return size;
    }

    /**
     *
     * @param size
     */
    protected void setSize(BigInteger size) {
        this.size = size;
        isSizeSet = true;
    }

    /**
     *
     */
    public void calculateSize() {
        if (parts == null || parts.isEmpty()) {
            if (this.size == null || !this.size.equals(BigInteger.ZERO)) {
                setSize(BigInteger.ZERO);
            }
            return;
        }
        BigInteger newSize = BigInteger.ZERO;
        for (ProjectFilePart pfp : parts) {
            newSize = newSize.add(BigInteger.valueOf(pfp.getPaddingAdjustedLength()));
        }
        setSize(newSize);
    }

    /**
     * <p>Was the method called to set the size of the data set?</p>
     * @return
     */
    public boolean isSizeSet() {
        return isSizeSet;
    }

    /**
     * <p>Gets the parts for this project file.</p>
     * @return
     */
    public synchronized Set<ProjectFilePart> getParts() {
        return parts;
    }

    /**
     * <p>Sets the parts for this project file.</p>
     * @param parts
     */
    public synchronized void setParts(Set<ProjectFilePart> parts) {
        this.parts = parts;
    }

    /**
     * <p>Adds a project file part to this project.</p>
     * @param pfp
     */
    public synchronized void addPart(ProjectFilePart pfp) {
        // add the part
        parts.add(pfp);
    }

    /**
     * <p>Destroys the disk backed project file part set.</p>
     */
    @Override()
    protected void finalize() {
        if (this.parts != null && this.parts instanceof DiskBackedProjectFilePartSet) {
            try {
                DiskBackedProjectFilePartSet s = (DiskBackedProjectFilePartSet) this.parts;
                s.destroy();
                this.parts = null;
            } catch (Exception ex) {
                System.err.println("Problem deleting disk-backed set in ProjectFile.finalize<" + ex.getClass().getName() + ">: " + ex.getMessage() + ". Please file a bug report.");
            }
        }
    }

    /**
     * <p>Closes off any resources that might be used.</p>
     */
    public void close() {
        finalize();
    }

    /**
     * 
     * @return
     * @throws java.lang.Exception
     */
    public final byte[] toByteArray() throws Exception {
        return toByteArray(version);
    }

    /**
     * 
     * @param version
     * @return
     * @throws java.lang.Exception
     */
    public final byte[] toByteArray(String version) throws Exception {
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            String saveVersion = this.version;
            this.version = version;
            ProjectFileUtil.write(this, baos);
            this.version = saveVersion;
            return baos.toByteArray();
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    /**
     * 
     * @param file
     * @return
     * @throws java.lang.Exception
     */
    public static final ProjectFile createFromFile(File file) throws Exception {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            return ProjectFileUtil.read(bis);
        } finally {
            IOUtil.safeClose(bis);
            IOUtil.safeClose(fis);
        }
    }
}
