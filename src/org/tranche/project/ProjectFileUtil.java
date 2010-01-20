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

import org.tranche.project.ProjectFile;
import org.tranche.project.DiskBackedProjectFilePartSet;
import org.tranche.project.ProjectFilePart;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Set;
import org.tranche.hash.BigHash;
import org.tranche.remote.RemoteUtil;

/**
 * <p>Utilities for working with a project file.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ProjectFileUtil {

    /**
     * <p>Reads a project file object from an input stream created from a project file.</p>
     * @param is
     * @return
     * @throws java.io.IOException
     */
    public static ProjectFile read(InputStream is) throws IOException {
        ProjectFile projectFile = new ProjectFile();
        // read the version
        projectFile.setVersion(RemoteUtil.readLine(is));
        if (projectFile.getVersion().equals(ProjectFile.VERSION_ONE)) {
            projectFile = readVersionOne(projectFile, is);
        } else if (projectFile.getVersion().equals(ProjectFile.VERSION_TWO)) {
            projectFile = readVersionTwo(projectFile, is);
        } else if (projectFile.getVersion().equals(ProjectFile.VERSION_THREE)) {
            projectFile = readVersionThree(projectFile, is);
        } else if (projectFile.getVersion().equals(ProjectFile.VERSION_FOUR)) {
            projectFile = readVersionFour(projectFile, is);
        } else {
            String version = projectFile.getVersion();
            int length = projectFile.getVersion().length();
            if (length > 10) {
                version = version.substring(0, 10) + "... [length: " + length + "]";
            }
            throw new IOException("Invalid project file version " + version);
        }
        return projectFile;
    }

    private static ProjectFile readVersionOne(ProjectFile pf, InputStream is) throws IOException {
        pf.setSize(new BigInteger(RemoteUtil.readLine(is)));
        pf.setName(RemoteUtil.readLine(is));
        pf.setDescription(RemoteUtil.readLine(is));
        DiskBackedProjectFilePartSet parts = new DiskBackedProjectFilePartSet();
        String relativeName = RemoteUtil.readLine(is);
        while (relativeName != null) {
            BigHash hash = BigHash.createFromBytes(RemoteUtil.readDataBytes(is));
            // make a part
            parts.add(new ProjectFilePart(relativeName, hash, new byte[0]));
            // get the next line
            relativeName = RemoteUtil.readLine(is);
        }
        pf.setParts(parts);
        return pf;
    }

    private static ProjectFile readVersionTwo(ProjectFile pf, InputStream is) throws IOException {
        pf.setSize(new BigInteger(RemoteUtil.readLine(is)));
        pf.setName(RemoteUtil.readLine(is));
        pf.setDescription(RemoteUtil.readLine(is));
        DiskBackedProjectFilePartSet parts = new DiskBackedProjectFilePartSet();
        String relativeName = RemoteUtil.readLine(is);
        while (relativeName != null) {
            BigHash hash = BigHash.createFromBytes(RemoteUtil.readDataBytes(is));
            // get the padding as well -- needed for cases of ambiguous encryption and hash collisions
            parts.add(new ProjectFilePart(relativeName, hash, RemoteUtil.readDataBytes(is)));
            // get the next line
            relativeName = RemoteUtil.readLine(is);
        }
        pf.setParts(parts);
        return pf;
    }

    private static ProjectFile readVersionThree(ProjectFile pf, InputStream is) throws IOException {
        pf.setSize(new BigInteger(RemoteUtil.readLine(is)));
        DiskBackedProjectFilePartSet parts = new DiskBackedProjectFilePartSet();
        String relativeName = RemoteUtil.readLine(is);
        while (relativeName != null) {
            BigHash hash = BigHash.createFromBytes(RemoteUtil.readDataBytes(is));
            // get the padding as well -- needed for cases of ambiguous encryption and hash collisions
            parts.add(new ProjectFilePart(relativeName, hash, RemoteUtil.readDataBytes(is)));
            // get the next line
            relativeName = RemoteUtil.readLine(is);
        }
        pf.setParts(parts);
        return pf;
    }

    private static ProjectFile readVersionFour(ProjectFile pf, InputStream is) throws IOException {
        pf.setSize(new BigInteger(RemoteUtil.readLine(is)));
        pf.setName(RemoteUtil.readLine(is));
        pf.setDescription(RemoteUtil.readLine(is));
        String licenseHash = RemoteUtil.readLine(is);
        if (licenseHash != null) {
            pf.setLicenseHash(BigHash.createHashFromString(licenseHash));
        }
        DiskBackedProjectFilePartSet parts = new DiskBackedProjectFilePartSet();
        String relativeName = RemoteUtil.readLine(is);
        while (relativeName != null) {
            BigHash hash = RemoteUtil.readBigHash(is);
            // get the padding as well -- needed for cases of ambiguous encryption and hash collisions
            parts.add(new ProjectFilePart(relativeName, hash, RemoteUtil.readDataBytes(is)));
            // get the next line
            relativeName = RemoteUtil.readLine(is);
        }
        pf.setParts(parts);
        return pf;
    }

    /**
     * <p>Reads the parts from an input stream created from a project file.</p>
     * @param is
     * @return
     * @throws java.io.IOException
     */
    public static DiskBackedProjectFilePartSet readParts(InputStream is) throws IOException {
        ProjectFile pf = read(is);
        Set<ProjectFilePart> parts = pf.getParts();
        if (parts instanceof DiskBackedProjectFilePartSet) {
            return (DiskBackedProjectFilePartSet) parts;
        }
        throw new RuntimeException("Expecting parts to be disk-backed.");
    }

    /**
     * <p>Writes a project file object to an output stream.</p>
     * @param pf
     * @param out
     * @throws java.io.IOException
     */
    public static void write(ProjectFile pf, OutputStream out) throws IOException {
        // calculate the size immediately before writing
        pf.calculateSize();
        RemoteUtil.writeLine(pf.getVersion(), out);
        if (pf.getVersion().equals(ProjectFile.VERSION_ONE)) {
            writeVersionOne(pf, out);
        } else if (pf.getVersion().equals(ProjectFile.VERSION_TWO)) {
            writeVersionTwo(pf, out);
        } else if (pf.getVersion().equals(ProjectFile.VERSION_THREE)) {
            writeVersionThree(pf, out);
        } else if (pf.getVersion().equals(ProjectFile.VERSION_FOUR)) {
            writeVersionFour(pf, out);
        } else {
            throw new IOException("Invalid project file version " + pf.getVersion());
        }
    }

    private static void writeVersionOne(ProjectFile pf, OutputStream out) throws IOException {
        RemoteUtil.writeLine(pf.getSize().toString(), out);
        RemoteUtil.writeLine(pf.getName(), out);
        RemoteUtil.writeLine(pf.getDescription(), out);
        Set<ProjectFilePart> parts = pf.getParts();
        for (ProjectFilePart pfp : parts) {
            RemoteUtil.writeLine(pfp.getRelativeName(), out);
            RemoteUtil.writeData(pfp.getHash().toByteArray(), out);
        }
    }

    private static void writeVersionTwo(ProjectFile pf, OutputStream out) throws IOException {
        RemoteUtil.writeLine(pf.getSize().toString(), out);
        RemoteUtil.writeLine(pf.getName(), out);
        RemoteUtil.writeLine(pf.getDescription(), out);
        Set<ProjectFilePart> parts = pf.getParts();
        for (ProjectFilePart pfp : parts) {
            RemoteUtil.writeLine(pfp.getRelativeName(), out);
            RemoteUtil.writeData(pfp.getHash().toByteArray(), out);
            RemoteUtil.writeData(pfp.getPadding(), out);
        }
    }

    private static void writeVersionThree(ProjectFile pf, OutputStream out) throws IOException {
        RemoteUtil.writeLine(pf.getSize().toString(), out);
        Set<ProjectFilePart> parts = pf.getParts();
        for (ProjectFilePart pfp : parts) {
            RemoteUtil.writeLine(pfp.getRelativeName(), out);
            RemoteUtil.writeData(pfp.getHash().toByteArray(), out);
            RemoteUtil.writeData(pfp.getPadding(), out);
        }
    }

    private static void writeVersionFour(ProjectFile pf, OutputStream out) throws IOException {
        RemoteUtil.writeLine(pf.getSize().toString(), out);
        RemoteUtil.writeLine(pf.getName(), out);
        RemoteUtil.writeLine(pf.getDescription(), out);
        if (pf.getLicenseHash() != null) {
            RemoteUtil.writeLine(pf.getLicenseHash().toString(), out);
        } else {
            RemoteUtil.writeLine("", out);
        }
        Set<ProjectFilePart> parts = pf.getParts();
        for (ProjectFilePart pfp : parts) {
            RemoteUtil.writeLine(pfp.getRelativeName(), out);
            RemoteUtil.writeBigHash(pfp.getHash(), out);
            RemoteUtil.writeData(pfp.getPadding(), out);
        }
    }
}
