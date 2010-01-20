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

import org.tranche.hash.BigHash;

/**
 * <p>Abstraction for a chunk of a project file. This class keeps the name and the hash for that chunk.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class ProjectFilePart implements Comparable {

    private String relativeName;
    private BigHash hash;
    private byte[] padding = new byte[0];

    /**
     * 
     * @param relativeName
     * @param hash
     * @param padding
     */
    public ProjectFilePart(String relativeName, BigHash hash, byte[] padding) {
        this.setRelativeName(relativeName);
        this.setHash(hash);
        this.padding = padding;
    }

    /**
     * <p>Compares the relative names of this to the given project file part.</p>
     * @param o
     * @return
     */
    public int compareTo(Object o) {
        return getRelativeName().compareTo(((ProjectFilePart)o).getRelativeName());
    }

    /**
     * <p>Gets the relative name of this file.</p>
     * @return
     */
    public String getRelativeName() {
        return relativeName;
    }

    /**
     * <p>Sets the relative name of this file.</p>
     * @param relativeName
     */
    public void setRelativeName(String relativeName) {
        this.relativeName = relativeName;
    }

    /**
     * <p>Returns the hash of this file.</p>
     * @return
     */
    public BigHash getHash() {
        return hash;
    }

    /**
     * <p>Sets the hash of this file.</p>
     * @param hash
     */
    public void setHash(BigHash hash) {
        this.hash = hash;
    }

    /**
     * <p>Returns whether the project file part names are the same.</p>
     * @param obj
     * @return
     */
    @Override()
    public boolean equals(Object obj) {
        if (obj instanceof ProjectFilePart) {
            return relativeName.equals(((ProjectFilePart) obj).getRelativeName());
        }
        return super.equals(obj);
    }

    /**
     * <p>Gets the hash code based on the relative name.</p>
     * @return
     */
    @Override()
    public int hashCode() {
        return getRelativeName().hashCode();
    }

    /**
     * <p>Returns the length of the file after padding has been removed.</p>
     * @return
     */
    public long getPaddingAdjustedLength() {
        return hash.getLength() - padding.length;
    }

    /**
     * <p>Gets the padding length.</p>
     * @return
     */
    public int getPaddingLength() {
        return padding.length;
    }

    /**
     * <p>Getts the padding on the file.</p>
     * @return
     */
    public byte[] getPadding() {
        return padding;
    }

    /**
     * <p>Sets the padding on the file.</p>
     * @param padding
     */
    public void setPadding(byte[] padding) {
        this.padding = padding;
    }
}
