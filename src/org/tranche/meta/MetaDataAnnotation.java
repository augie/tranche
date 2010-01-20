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
package org.tranche.meta;

import org.tranche.util.Text;

/**
 * <p>Represents a name/value pair associated with meta-data.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class MetaDataAnnotation {

    /**
     * <p>Unique namespace identifier for Tranche annotations.</p>
     */
    private static final String NAMESPACE = "Tranche:";
    /**
     * <p></p>
     * @deprecated Use PROP_STICKY_SERVER_HOST
     */
    public static final String PROP_STICKY_SERVER_URL = "Sticky Server URL";
    /**
     * <p>Name of the meta data annotation that notes the host name for a server on which a file should be stuck.</p>
     */
    public static final String PROP_STICKY_SERVER_HOST = NAMESPACE + "Sticky Server Host";
    /**
     * <p>Name of the meta data annotation that notes when the file was last modified.</p>
     * @deprecated Moved into meta data properties, name of MetaData.PROP_TIMESTAMP_FILE
     */
    public static final String FILE_LAST_MODIFIED_TIMESTAMP = "File Last Modified Timestamp";
    /**
     * <p>Name of a "Deleted" meta data annotation. Really used for hiding data sets.</p>
     * @deprecated Moved to meta data properties, name of MetaData.PROP_HIDDEN
     */
    public static final String PROP_DELETED = "Deleted";
    /**
     * <p>Name of an "Undeleted" meta data annotation.</p>
     * @deprecated No longer in use.
     */
    public static final String PROP_UNDELETED = "Undeleted";
    /**
     * <p>Name of a "Publish Passphrase Timestamp" meta data annotation.</p>
     * @deprecated Moved to AES file encoding properties, name of FileEncoding.PROP_TIMESTAMP_PUBLISHED
     */
    public static final String PROP_PUBLISHED_TIMESTAMP = "Publish Passphrase Timestamp";
    /**
     * <p>Name of an "Old Version" meta data annotation.</p>
     * @deprecated Moved to meta data properties, name of MetaData.PROP_VERSION_PREVIOUS
     */
    public static final String PROP_OLD_VERSION = NAMESPACE + "Old Version";
    /**
     * <p>Name of a "Delete Old Version" meta data annotation.</p>
     * @deprecated No longer in use.
     */
    public static final String PROP_DELETE_OLD_VERSION = NAMESPACE + "Delete Old Version";
    /**
     * <p>Name of a "New Version" meta data annotation.</p>
     * @deprecated Moved to meta data properties, name of MetaData.PROP_VERSION_NEXT
     */
    public static final String PROP_NEW_VERSION = NAMESPACE + "New Version";
    /**
     * <p>Name of a "Delete New Version" meta data annotation.</p>
     * @deprecated No longer in use.
     */
    public static final String PROP_DELETE_NEW_VERSION = NAMESPACE + "Delete New Version";
    /**
     * <p>Meta-data annotation name flagging that project has been annotated.</p>
     * @deprecated No longer in use.
     */
    public static final String PROJECT_ANNOTATED_MDA = NAMESPACE + "ProteomeCommons.org Tranche Annotation";
    /**
     * <p>Meta-data annotation name flagging that file is a peaklist.</p>
     * @deprecated Move to usage location.
     */
    public static final String IS_PEAKLIST_MDA = NAMESPACE + "Peaklist";
    /**
     * <p>Meta-data annotation name for an output file annotation.</p>
     * @deprecated Move to usage location.
     */
    public static final String IS_OUT_MDA = NAMESPACE + "Output";
    /**
     * <p>Meta-data annotation value for sequest output file.</p>
     * @deprecated Move to usage location.
     */
    public static final String SEQUEST_OUT_MDVALUE = "Sequest";
    /**
     * <p>Meta-data annotation value flagging a peaklist as XTandem.</p>
     * @deprecated Move to usage location.
     */
    public static final String PKL_IS_XTANDEM = "XTandem";
    /**
     * <p>Annotation used with encrypted projects if user permits sharing meta data while encrypted.</p>
     * @deprecated Moved to meta data properties, name of MetaData.PROP_SHARE_INFO_IF_ENCRYPTED
     */
    public static final MetaDataAnnotation SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION = new MetaDataAnnotation(NAMESPACE + "Share Meta Data If Encrypted", "true");
    /**
     * <p>The delimiter between the name and value for the annotation.</p>
     */
    public static final String DELIMITER = "<<MDA>>";
    private String name, value;

    /**
     * @param name
     * @param value
     */
    public MetaDataAnnotation(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * @param value
     */
    public MetaDataAnnotation(String value) {
        this.value = value;
    }

    /**
     * <p>Returns the value.</p>
     * @return
     */
    public final String getValue() {
        return value;
    }

    /**
     * <p>Sets the value.</p>
     * @param value
     */
    public final void setValue(String value) {
        this.value = value;
    }

    /**
     * <p>Returns the name.</p>
     * @return
     */
    public final String getName() {
        return name;
    }

    /**
     * <p>Sets the name.</p>
     * @param name
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * <p>Returns the string representation.</p>
     * @return
     */
    @Override
    public String toString() {
        return Text.tokenizeNewlines(this.name + DELIMITER + this.value);
    }

    /**
     * <p>Create from a string.</p>
     * @param string
     * @return
     */
    public static final MetaDataAnnotation createFromString(String string) {
        String[] items = string.split(Text.detokenizeNewlines(DELIMITER));
        return new MetaDataAnnotation(items[0], items[1]);
    }

    /**
     * <p>Compare two meta data annotation objects.</p>
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof MetaDataAnnotation) {
            MetaDataAnnotation mda = (MetaDataAnnotation) o;
            // looks funny because that will handle nulls
            return (mda.name == this.name || mda.name.equals(this.name)) && (mda.value == this.value || mda.value.equals(this.value));
        }
        return o.equals(this);
    }
}
