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
package org.tranche;

import org.tranche.hash.BigHash;
import java.util.Properties;
import org.tranche.commons.TextUtil;

/**
 * <p>Represents an encoding of a chunk. A meta data will have collection of FileEncoding so that, when downloading the chunk, the tool will know how to process it to get the original bytes.</p>
 * <p>Note that order is significant. E.g., say a chunk has the following encodings: NONE, GZIP, and AES. This means that AES decryption was last applied (so much be handled first when downloading). Each FileEncoding has a hash associated with it to verify integrity.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class FileEncoding {

    /**
     * <p>Representing a chunk's state without any encodings.</p>
     */
    public static final String NONE = "None";
    /**
     * <p>Representing a chunk's state after LZMA compression applied.</p>
     */
    public static final String LZMA = "LZMA";
    /**
     * <p>Representing a chunk's state after GZIP compression applied.</p>
     */
    public static final String GZIP = "GZIP";
    /**
     * <p>Representing a chunk's state after AES encryption applied.</p>
     */
    public static final String AES = "AES";
    /**
     * <p>Representing a chunk (or collection of chunks) state after tarballed.</p>
     * @deprecated Not in use.
     */
    public static final String TARBALL = "Tarball";
    /**
     * <p>Property key: used to get the hash value.</p>
     */
    public static final String PROP_HASH = "Hash";
    /**
     * <p>Property key: used to get the passphrase value (if set).</p>
     */
    public static final String PROP_PASSPHRASE = "Passphrase";
    /**
     * <p>Property key: used to set when the passphrase was published.</p>
     */
    public static final String PROP_TIMESTAMP_PUBLISHED = "Timestamp Published";
    private static final String DELIMITER = "<<ENC>>";
    private static final String PROP_DELIMITER = "<<PROP>>";
    private static final String NEWLINE_TOKEN = "<<NL>>";
    private String name;
    private BigHash hash;
    private Properties properties;

    /**
     * 
     * @param name
     * @param props
     */
    public FileEncoding(String name, Properties props) {
        this.name = name;
        this.setProperties(props);
    }

    /**
     * 
     * @param name
     * @param mh
     */
    public FileEncoding(String name, BigHash mh) {
        this.name = name;
        properties = new Properties();
        // set the hash
        properties.setProperty(FileEncoding.PROP_HASH, mh.toString());
    }

    /**
     * <p>Get the BigHash associated with this FileEncoding.</p>
     * @return
     */
    public BigHash getHash() {
        if (hash == null) {
            hash = BigHash.createHashFromString(getProperties().getProperty(FileEncoding.PROP_HASH));
        }
        return hash;
    }

    /**
     * <p>Get a property (value) using a key.</p>
     * @param name Property key used to return the particular property value.
     * @return
     * @see #PROP_HASH
     * @see #PROP_PASSPHRASE
     */
    public String getProperty(String name) {
        return getProperties().getProperty(name);
    }

    /**
     * <p>Set a property. This property can be later retrieved using the name (key).</p>
     * @param name
     * @param value
     * @see #PROP_HASH
     * @see #PROP_PASSPHRASE
     */
    public void setProperty(String name, String value) {
        getProperties().setProperty(name, value);
    }

    /**
     * <p>Get the name of the FileEncoding. Used to identify the type of encoding.</p>
     * @return
     * @see #NONE
     * @see #AES
     * @see #GZIP
     * @see #LZMA
     * @see #TARBALL
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Set the name of the FileEncoding. Used to identify the type of encoding.</p>
     * @param name
     * @see #NONE
     * @see #AES
     * @see #GZIP
     * @see #LZMA
     * @see #TARBALL
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>Properties object associated with FileEncoding.</p>
     * @return
     * @see #PROP_HASH
     * @see #PROP_PASSPHRASE
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * <p>Properties object associated with FileEncoding.</p>
     * @param properties
     * @see #PROP_HASH
     * @see #PROP_PASSPHRASE
     * @see #PROP_TIMESTAMP_PUBLISHED
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override()
    public boolean equals(Object o) {
        FileEncoding obj = ((FileEncoding) o);
        return obj.getName().equals(getName())
                && obj.getHash().equals(getHash())
                && obj.getProperties().size() == getProperties().size();
    }

    /**
     * <p>Create a string representation. This can be saved and later restored.</p>
     * @return
     * @see #createFromString(java.lang.String) 
     */
    @Override()
    public String toString() {
        StringBuffer props = new StringBuffer();
        for (Object key : properties.keySet()) {
            props.append(key + PROP_DELIMITER + properties.get(key) + DELIMITER);
        }
        return String.valueOf(getName() + DELIMITER + props.toString()).replace(TextUtil.TEXT_LINE_BREAK_RN, NEWLINE_TOKEN).replace(TextUtil.TEXT_LINE_BREAK_R, NEWLINE_TOKEN).replace(TextUtil.TEXT_LINE_BREAK_N, NEWLINE_TOKEN);
    }

    /**
     * <p>Create a FileEncoding object from a string.</p>
     * @param string
     * @return
     * @see #toString() 
     */
    public static FileEncoding createFromString(String string) {

        String[] items = string.replace(NEWLINE_TOKEN, TextUtil.TEXT_LINE_BREAK_N).split(DELIMITER);

        String name = items[0];
        Properties properties = new Properties();

        for (int i = 1; i < items.length; i++) {
            String[] pair = items[i].split(PROP_DELIMITER);
            properties.setProperty(pair[0], pair[1]);
        }

        return new FileEncoding(name, properties);
    }
}
