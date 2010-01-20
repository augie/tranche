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
package org.tranche.license;

import java.io.InputStream;
import java.io.OutputStream;
import org.tranche.remote.RemoteUtil;
import org.tranche.util.IOUtil;

/**
 * Class describing the licensing terms with title, short, and full-text descriptions.
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class License {

    /**************************************************************************
     * STATIC GLOBAL VARIABLES
     **************************************************************************/
    public static License CC0 = new License(
            "Creative Commons CC0 Waiver 1.0 Universal",
            "The person who associated a work with this document has dedicated this work to the Commons by waiving all of his or her rights to the work under copyright law and all related or neighboring legal rights he or she had in the work, to the extent allowable by law.\n\nWorks under CC0 do not require attribution. When citing the work, you should not imply endorsement by the author.\n\nOther Rights — In no way are any of the following rights affected by CC0:\n - Patent or trademark rights held by the person who associated this document with a work.\n - Rights other persons may have either in the work itself or in how the work is used, such as publicity or privacy rights.",
            "/org/tranche/license/CC0-1.0-Universal",
            true);
    /**************************************************************************
     * INSTANCE VARIABLES
     **************************************************************************/
    private int version = 1;
    private String title, shortDescription;
    /**
     * <p>Include either a description or a descriptionFile:</p>
     * <ul>
     *   <li>If description, stored in memory</li>
     *   <li>If descriptionFile, read from disk each time</li>
     * </ul>
     */
    private String description;
    /**
     * <p>Include either a description or a descriptionFile:</p>
     * <ul>
     *   <li>If description, stored in memory</li>
     *   <li>If descriptionFile, read from disk each time</li>
     * </ul>
     */
    private String descriptionFile;

    /**
     * Create a license with a title, short description, its encryption status,
     * the URL of the license, and the full-text of the license.
     * @param title
     * @param shortDescription A brief description of the license
     * @param description The full-text description of the license
     * @param descriptionIsFile Whether the description is the local file containing the description.
     */
    public License(String title, String shortDescription, String description, boolean descriptionIsFile) {
        if (title != null) {
            this.title = title;
        } else {
            this.title = "";
        }
        if (shortDescription != null) {
            this.shortDescription = shortDescription;
        } else {
            this.shortDescription = "";
        }
        if (descriptionIsFile) {
            this.descriptionFile = description;
        } else {
            this.description = description;
        }
        try {
            if (description == null && getShortDescription() == null) {
                throw new Exception("A description must be given for the license.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 
     * @param in
     * @throws Exception
     */
    public License(InputStream in) throws Exception {
        deserialize(in);
    }

    /**
     * Return the title and encryption status appended.
     * @return
     */
    @Override()
    public String toString() {
        return getTitle();
    }

    /**
     * Retrieve short description of the license.
     * @return
     */
    public String getShortDescription() {
        return shortDescription;
    }

    /**
     * Set short description of the license.
     * @param shortDescription
     */
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    /**
     * Retrieve title of license.
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set title of the license.
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * <p>The legal text or other text to describe the license.</p>
     * <p>Note this might be stored in memory or on disk. Returns null if not set.</p>
     * @return The legal text, if set, or null otherwise
     */
    public String getDescription() {
        if (description != null) {
            return description;
        } else if (this.descriptionFile != null) {
            try {
                StringBuffer buffer = new StringBuffer();

                InputStream in = null;
                try {
                    in = License.class.getResourceAsStream(this.descriptionFile);

                    byte[] bytes = new byte[256];

                    int readBytes;
                    while ((readBytes = in.read(bytes)) != -1) {

                        if (readBytes == bytes.length) {
                            buffer.append(new String(bytes));
                        } else {
                            byte[] readBytesArray = new byte[readBytes];
                            System.arraycopy(bytes, 0, readBytesArray, 0, readBytes);
                            buffer.append(new String(readBytesArray));
                        }
                    }

                    return buffer.toString();
                } finally {
                    IOUtil.safeClose(in);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // not found
        return null;
    }

    /**
     * Set the license text. If set this way, any existing legal text will be 
     * ignored in favor of this text.
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * @param out
     * @throws Exception
     */
    public void serialize(OutputStream out) throws Exception {
        RemoteUtil.writeInt(version, out);
        if (title != null) {
            RemoteUtil.writeLine(title, out);
        } else {
            RemoteUtil.writeLine("", out);
        }
        if (shortDescription != null) {
            RemoteUtil.writeLine(shortDescription, out);
        } else {
            RemoteUtil.writeLine("", out);
        }
        if (description != null) {
            RemoteUtil.writeLine(description, out);
        } else {
            RemoteUtil.writeLine("", out);
        }
        if (descriptionFile != null) {
            RemoteUtil.writeLine(descriptionFile, out);
        } else {
            RemoteUtil.writeLine("", out);
        }
    }

    /**
     * 
     * @param in
     * @throws Exception
     */
    public void deserialize(InputStream in) throws Exception {
        version = RemoteUtil.readInt(in);
        title = RemoteUtil.readLine(in);
        shortDescription = RemoteUtil.readLine(in);
        description = RemoteUtil.readLine(in);
        descriptionFile = RemoteUtil.readLine(in);
    }
}
