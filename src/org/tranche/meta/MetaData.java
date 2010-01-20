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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tranche.security.Signature;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.hash.BigHash;
import org.tranche.time.TimeUtil;
import org.tranche.FileEncoding;
import org.tranche.util.IOUtil;

/**
 * <p>Object that represents a meta data chunk on the Tranche network.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class MetaData {

    /**
     * <p>Identifies the first meta data version.</p>
     */
    public static final String VERSION_ONE = "1";
    /**
     * <p>Identifies the second meta data version -- added last modified timestamp and version.</p>
     */
    public static final String VERSION_TWO = "2";
    /**
     * <p>Identifies the third meta data version -- added properties (project files, project size, project name, and project description.)</p>
     */
    public static final String VERSION_THREE = "3";
    /**
     * <p>Identifies the fourth meta data version -- allows for multiple uploaders and distinguishes the overlap.</p>
     */
    public static final String VERSION_FOUR = "4";
    /**
     * <p>Identifies the latest meta data version.</p>
     */
    public static final String VERSION_LATEST = VERSION_FOUR;
    /**
     * <p>Flag meaning this meta data is version 1.0</p>
     * <p>Not useful past the first version.</p>
     */
    public static final int VERSION_ONE_BIT = (int) Math.pow(2, 0);
    /**
     * <p>Flag meaning this meta data represents a directory/project.</p>
     */
    public static final int PROJECT_FILE_BIT = (int) Math.pow(2, 1);
    /**
     * <p>Flag meaning this meta data represents sticky data.</p>
     * @deprecated Making a file sticky is now done by adding a Meta Data Annotation of a server to which it should be stuck.
     */
    public static final int STICKY_DATA_BIT = (int) Math.pow(2, 2);
    /**
     * <p>Flag meaning this meta data represents stick meta data.</p>
     * @deprecated making a file sticky is now done by adding a Meta Data Annotation of a server to which it should be stuck.
     */
    public static final int STICKY_META_DATA_BIT = (int) Math.pow(2, 3);
    /**
     * <p>Flag meaning this meta data is GZIP compressed.</p>
     */
    public static final int GZIP_COMPRESSED_BIT = (int) Math.pow(2, 4);
    /**
     * <p>Flag meaning this meta data is limited life.</p>
     * @deprecated Use the property for expiration timestamp to denote when a file should expire.
     */
    public static final int LIMITED_LIFE_BIT = (int) Math.pow(2, 5);
    /**
     * <p>Flag meaning this meta data has a mime type.</p>
     * @deprecated Of no purpose since the mime type was moved into the properties structure and now depends on the uploader.
     */
    public static final int MIME_TYPE_BIT = (int) Math.pow(2, 6);
    /**
     * <p></p>
     */
    public static final String PROP_DATA_SET_SIZE = "dataset.size";
    /**
     * <p></p>
     */
    public static final String PROP_DATA_SET_FILES = "dataset.files";
    /**
     * <p></p>
     */
    public static final String PROP_DATA_SET_NAME = "dataset.name";
    /**
     * <p></p>
     */
    public static final String PROP_DATA_SET_DESCRIPTION = "dataset.description";
    /**
     * <p></p>
     */
    public static final String PROP_TIMESTAMP_UPLOADED = "timestamp.upload";
    /**
     * <p>The last modified timestamp of the file as it existed on the uploader's system.</p>
     */
    public static final String PROP_TIMESTAMP_FILE = "timestamp.file";
    /**
     * <p>The name of the file.</p>
     */
    public static final String PROP_NAME = "name";
    /**
     * <p>The relative name of the file in the data set (includes the file name.)</p>
     */
    public static final String PROP_PATH_IN_DATA_SET = "path.dataset";
    /**
     * <p></p>
     */
    public static final String PROP_MIME_TYPE = "mimetype";
    /**
     * <p>The hash of the newer version of this file or data set.</p>
     */
    public static final String PROP_VERSION_NEXT = "version.next";
    /**
     * <p>The hash of the older version of this file or data set.</p>
     */
    public static final String PROP_VERSION_PREVIOUS = "version.previous";
    /**
     *
     */
    public static final String PROP_HIDDEN = "hidden";
    /**
     *
     */
    public static final String PROP_SHARE_INFO_IF_ENCRYPTED = "encrypted.share";
    /**
     * <p>Maximum size.</p>
     */
    public static final long SIZE_MAX = 90 * DataBlockUtil.ONE_MB;
    private String version = VERSION_LATEST;
    private int flags = GZIP_COMPRESSED_BIT;
    private long lastModifiedTimestamp = TimeUtil.getTrancheTimestamp();
    private final List<MetaDataUploader> uploaders = new ArrayList<MetaDataUploader>();
    // Map<hash of final encoding, List<hash of parts that correspond to final encoding>>
    private final Map<BigHash, List<BigHash>> parts = new HashMap<BigHash, List<BigHash>>();
    private int selectedUploader = -1;

    /**
     * 
     */
    public MetaData() {
    }

    /**
     * <p>Creates a clone of the given meta data.</p>
     * @param toClone
     */
    private MetaData(MetaData toClone) {
        version = toClone.getVersion();
        flags = toClone.getFlags();
        lastModifiedTimestamp = toClone.getLastModifiedTimestamp();
        uploaders.addAll(toClone.getUploaders());
        parts.putAll(toClone.getAllParts());
        selectedUploader = toClone.getSelectedUploader();
    }

    /**
     * 
     * @return
     */
    private List<MetaDataUploader> getUploaders() {
        return Collections.unmodifiableList(uploaders);
    }

    /**
     * <p>Returns the version of this meta data.</p>
     * @return
     */
    public final String getVersion() {
        return version;
    }

    /**
     * <p>Sets the version of this meta data.</p>
     * @param version
     */
    public final void setVersion(String version) {
        if (version.equals(VERSION_ONE)) {
            flags = flags | VERSION_ONE_BIT;
        } else {
            flags = flags & (GZIP_COMPRESSED_BIT | PROJECT_FILE_BIT | STICKY_DATA_BIT | STICKY_META_DATA_BIT | LIMITED_LIFE_BIT | MIME_TYPE_BIT);
        }
        this.version = version;
    }

    /**
     * <p>Gets the flags.</p>
     * @return
     */
    public final int getFlags() {
        return flags;
    }

    /**
     * <p>Sets the flags.</p>
     * @param flags
     */
    protected final void setFlags(int flags) {
        this.flags = flags;
    }

    /**
     * <p>Returns the flag for whether this meta data is version 1.0.</p>
     * @return
     */
    public final boolean isVersionOne() {
        return (VERSION_ONE_BIT & flags) != 0;
    }

    /**
     * <p>Returns the flag for whether this meta data is GZIP compressed.</p>
     * @return
     */
    public final boolean isGZIPCompressed() {
        return (GZIP_COMPRESSED_BIT & flags) != 0;
    }

    /**
     * <p>Sets whether to GZIP compress the meta data file upon writing.</p>
     * @param GZIPcompress
     */
    public final void setGZIPCompress(boolean GZIPcompress) {
        if (GZIPcompress) {
            flags = flags | GZIP_COMPRESSED_BIT;
        } else {
            flags = flags & (VERSION_ONE_BIT | PROJECT_FILE_BIT | STICKY_DATA_BIT | STICKY_META_DATA_BIT | LIMITED_LIFE_BIT | MIME_TYPE_BIT);
        }
    }

    /**
     * <p>Returns the flag for whether this meta data is for a project file.</p>
     * @return
     */
    public final boolean isProjectFile() {
        return (PROJECT_FILE_BIT & flags) != 0;
    }

    /**
     *
     * @param isProjectFile
     */
    public final void setIsProjectFile(boolean isProjectFile) {
        if (isProjectFile) {
            flags = flags | MetaData.PROJECT_FILE_BIT;
        } else {
            flags = flags & (VERSION_ONE_BIT | STICKY_DATA_BIT | STICKY_META_DATA_BIT | GZIP_COMPRESSED_BIT | LIMITED_LIFE_BIT | MIME_TYPE_BIT);
        }
    }

    /**
     * 
     * @return
     * @deprecated No longer support expiration timestamps.
     */
    public final boolean isLimitedLife() {
        return (LIMITED_LIFE_BIT & flags) != 0;
    }

    /**
     *
     * @param isLimitedLife
     * @deprecated No longer support expiration timestamps.
     */
    protected final void setIsLimitedLife(boolean isLimitedLife) {
        if (isLimitedLife) {
            flags = flags | MetaData.LIMITED_LIFE_BIT;
        } else {
            flags = flags & (VERSION_ONE_BIT | STICKY_DATA_BIT | STICKY_META_DATA_BIT | GZIP_COMPRESSED_BIT | PROJECT_FILE_BIT | MIME_TYPE_BIT);
        }
    }

    /**
     * 
     * @return
     */
    protected final boolean isMimeType() {
        return (MIME_TYPE_BIT & flags) != 0;
    }

    /**
     * 
     * @param isMimeType
     */
    protected final void setIsMimeType(boolean isMimeType) {
        if (isMimeType) {
            flags = flags | MetaData.MIME_TYPE_BIT;
        } else {
            flags = flags & (VERSION_ONE_BIT | STICKY_DATA_BIT | STICKY_META_DATA_BIT | GZIP_COMPRESSED_BIT | PROJECT_FILE_BIT | LIMITED_LIFE_BIT);
        }
    }

    /**
     * <p>Returns the UNIX timestamp of when the meta data was last modified.</p>
     * @return
     */
    public final long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    /**
     * <p>DO NOT USE THIS METHOD. It is only meant to be used by MetaDataUtil.</p>
     * <p>Sets the UNIX timestamp of when the meta data was last modified.</p>
     * @param timestamp
     */
    protected final void setLastModifiedTimestamp(long timestamp) {
        this.lastModifiedTimestamp = timestamp;
    }

    /**
     * <p>Returns the index in the list of uploaders for the target uploader at the target timestamp with the given full path in the data set.</p>
     * @param targetUploaderName
     * @param targetUploadTimestamp
     * @param fullPath
     * @return
     * @throws org.tranche.meta.AmbiguousFileSelectionException
     */
    private synchronized final int indexOfUploader(String targetUploaderName, Long targetUploadTimestamp, String fullPathInDataSet) throws AmbiguousFileSelectionException {
        if (targetUploaderName == null) {
            throw new NullPointerException("Null uploader name.");
        }
        if (targetUploadTimestamp == null)  {
            throw new NullPointerException("Null upload timestamp.");
        }
        String targetUploadTimestampString = String.valueOf(targetUploadTimestamp);
        // where does this uploader match in the list?
        List<Integer> indicesOfUploaders = new ArrayList<Integer>();
        for (MetaDataUploader uploader : uploaders) {
            // found the uploader, timestamp, and path
            if (uploader.getSignature().getUserName().equals(targetUploaderName) &&
                    (uploader.getProperties().containsKey(PROP_TIMESTAMP_UPLOADED) && uploader.getProperties().get(PROP_TIMESTAMP_UPLOADED).equals(targetUploadTimestampString)) &&
                    (fullPathInDataSet == null || (uploader.getProperties().containsKey(PROP_PATH_IN_DATA_SET) && uploader.getProperties().get(PROP_PATH_IN_DATA_SET).equals(fullPathInDataSet)))) {
                indicesOfUploaders.add(uploaders.indexOf(uploader));
            }
        }
        // more than one match found
        if (indicesOfUploaders.size() > 1) {
            throw new AmbiguousFileSelectionException(indicesOfUploaders);
        } // none found
        else if (indicesOfUploaders.isEmpty()) {
            return -1;
        }
        // found the one
        return indicesOfUploaders.get(0);
    }

    /**
     * 
     * @param targetUploaderName
     * @param targetUploadTimestamp
     * @param fullPathInDataSet
     * @return
     * @throws AmbiguousFileSelectionException
     */
    public final boolean containsUploader(String targetUploaderName, Long targetUploadTimestamp, String fullPathInDataSet) throws AmbiguousFileSelectionException {
        return indexOfUploader(targetUploaderName, targetUploadTimestamp, fullPathInDataSet) != -1;
    }

    /**
     * 
     * @param targetUploaderName
     * @param targetUploadTimestamp
     * @param fullPathInDataSet
     * @throws java.lang.Exception
     */
    public final void selectUploader(String targetUploaderName, Long targetUploadTimestamp, String fullPathInDataSet) throws Exception {
        int index = indexOfUploader(targetUploaderName, targetUploadTimestamp, fullPathInDataSet);
        if (index == -1) {
            throw new Exception("Invalid identification combination.");
        }
        selectedUploader = index;
    }

    /**
     * 
     * @param selectedUploader
     */
    public final void selectUploader(int selectedUploader) {
        this.selectedUploader = selectedUploader;
    }

    /**
     * 
     * @return
     */
    public final int getSelectedUploader() {
        return selectedUploader;
    }

    /**
     *
     * @return
     */
    public synchronized final int getUploaderCount() {
        return uploaders.size();
    }

    /**
     * <p>In the given properties, the PROP_TIMESTAMP_UPLOADED value is overwritten by the given uploadTimestamp, and the PROP_PATH_IN_DATA_SET value is overwritten by the given fullPathInDataSet.</p>
     * <p>Selects the added uploader.</p>
     * @param signature
     * @param encodings
     * @param properties
     * @param annotations
     * @throws java.lang.Exception
     */
    public synchronized final void addUploader(Signature signature, ArrayList<FileEncoding> encodings, Map<String, String> properties, ArrayList<MetaDataAnnotation> annotations) throws Exception {
        if (indexOfUploader(signature.getUserName(), Long.valueOf(properties.get(PROP_TIMESTAMP_UPLOADED)), properties.get(PROP_PATH_IN_DATA_SET)) != -1) {
            throw new Exception("Duplicate upload identification combination.");
        }
        MetaDataUploader uploader = new MetaDataUploader(signature, encodings, properties, annotations);
        uploaders.add(uploader);
        selectedUploader = uploaders.size() - 1;
    }

    /**
     *
     * @param uploaderName
     * @param uploadTimestamp
     * @param fullPathInDataSet
     * @throws java.lang.Exception
     */
    public synchronized final void removeUploader(String uploaderName, long uploadTimestamp, String fullPathInDataSet) throws Exception {
        int index = indexOfUploader(uploaderName, uploadTimestamp, fullPathInDataSet);
        if (index == -1) {
            throw new Exception("Invalid identification combination.");
        }
        BigHash finalEncodingHash = uploaders.get(index).getEncodings().get(uploaders.get(index).getEncodings().size() - 1).getHash();
        uploaders.remove(index);
        if (selectedUploader == uploaders.size()) {
            selectedUploader--;
        }
        // possibly remove parts from map
        boolean usedMoreThanOnce = false;
        for (int i = 0; i < uploaders.size(); i++) {
            if (uploaders.get(i).getEncodings().get(uploaders.get(i).getEncodings().size() - 1).getHash().equals(finalEncodingHash)) {
                usedMoreThanOnce = true;
                break;
            }
        }
        if (!usedMoreThanOnce) {
            parts.remove(finalEncodingHash);
        }
    }

    /**
     * 
     * @return
     */
    public synchronized final Signature getSignature() {
        return uploaders.get(selectedUploader).getSignature();
    }

    /**
     * 
     * @return
     */
    public synchronized final List<FileEncoding> getEncodings() {
        try {
            return uploaders.get(selectedUploader).getEncodings();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>Returns the parts associated with the final encoding for the selected uploader.</p>
     * @return
     */
    public synchronized final List<BigHash> getParts() {
        try {
            return parts.get(getEncodings().get(getEncodings().size() - 1).getHash());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @param finalEncodingHash
     * @param parts
     */
    protected synchronized final void setParts(BigHash finalEncodingHash, ArrayList<BigHash> parts) {
        this.parts.put(finalEncodingHash, parts);
    }

    /**
     * <p>Adds the given hash to the list of part hashes for the final encoding of the currently selected uploader.</p>
     * @param partHash
     */
    public synchronized final void addPart(BigHash partHash) {
        BigHash finalEncodingHash = getEncodings().get(getEncodings().size() - 1).getHash();
        if (!parts.containsKey(finalEncodingHash)) {
            parts.put(finalEncodingHash, new ArrayList<BigHash>());
        }
        parts.get(finalEncodingHash).add(partHash);
    }

    /**
     * 
     * @param parts
     */
    public synchronized final void setParts(ArrayList<BigHash> parts) {
        this.parts.put(getEncodings().get(getEncodings().size() - 1).getHash(), parts);
    }

    /**
     *
     * @return
     */
    public synchronized final Map<BigHash, List<BigHash>> getAllParts() {
        return parts;
    }

    /**
     * 
     * @return
     */
    public synchronized final Map<String, String> getProperties() {
        return uploaders.get(selectedUploader).getProperties();
    }

    /**
     * 
     * @param name
     * @param value
     */
    protected synchronized final void setProperty(String name, String value) {
        uploaders.get(selectedUploader).getProperties().put(name, value);
    }

    /**
     * 
     * @return
     */
    public synchronized final List<MetaDataAnnotation> getAnnotations() {
        return uploaders.get(selectedUploader).getAnnotations();
    }

    /**
     * 
     * @param name
     * @return
     */
    public synchronized final Collection<MetaDataAnnotation> getAnnotationsWithName(String name) {
        List<MetaDataAnnotation> annotations = new ArrayList<MetaDataAnnotation>();
        for (MetaDataAnnotation annotation : uploaders.get(selectedUploader).getAnnotations()) {
            if (annotation.getName().equals(name)) {
                annotations.add(annotation);
            }
        }
        return Collections.unmodifiableCollection(annotations);
    }

    /**
     * 
     * @param name
     * @return
     */
    public synchronized final Collection<String> getAnnotationValuesWithName(String name) {
        List<String> values = new ArrayList<String>();
        for (MetaDataAnnotation annotation : uploaders.get(selectedUploader).getAnnotations()) {
            if (annotation.getName().equals(name)) {
                values.add(annotation.getValue());
            }
        }
        return Collections.unmodifiableCollection(values);
    }

    /**
     * 
     * @param annotationName
     * @param annotationValue
     */
    public synchronized final void addAnnotation(String annotationName, String annotationValue) {
        uploaders.get(selectedUploader).getAnnotations().add(new MetaDataAnnotation(annotationName, annotationValue));
    }

    /**
     * 
     * @param annotation
     */
    public synchronized final void removeAnnotation(MetaDataAnnotation annotation) {
        uploaders.get(selectedUploader).getAnnotations().remove(annotation);
    }

    /**
     * 
     * @param name
     * @param value
     */
    public synchronized final void removeAnnotation(String name, String value) {
        MetaDataAnnotation toRemove = null;
        for (MetaDataAnnotation annotation : uploaders.get(selectedUploader).getAnnotations()) {
            if (annotation.getName().equals(name) && annotation.getValue().equals(value)) {
                toRemove = annotation;
                break;
            }
        }
        if (toRemove != null) {
            removeAnnotation(toRemove);
        }
    }

    /**
     * 
     */
    public synchronized final void clearAnnotations() {
        uploaders.get(selectedUploader).getAnnotations().clear();
    }

    /**
     * <p>Helper method that will inspect the encodings and check that all encryptions have a passphrase.</p>
     * @return
     */
    public synchronized final boolean isEncrypted() {
        for (FileEncoding fe : uploaders.get(selectedUploader).getEncodings()) {
            if (fe.getName().equals(FileEncoding.AES)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Publish a passphrase to this meta data.</p>
     * @param passphrase
     * @throws java.lang.Exception
     */
    public synchronized final void setPublicPassphrase(String passphrase) throws Exception {
        // get the proper encoding
        FileEncoding AESFileEncoding = null;
        for (FileEncoding fe : uploaders.get(selectedUploader).getEncodings()) {
            if (fe.getName().equals(FileEncoding.AES)) {
                AESFileEncoding = fe;
                break;
            }
        }

        // make sure this meta data points to data that is actually encrypted
        if (AESFileEncoding == null) {
            throw new Exception("The data is not encrypted.");
        }

        // finish by setting the passphrase
        if (passphrase == null || passphrase.equals("")) {
            // remove the public passphrase and timestamp
            AESFileEncoding.getProperties().remove(FileEncoding.PROP_PASSPHRASE);
            AESFileEncoding.getProperties().remove(FileEncoding.PROP_TIMESTAMP_PUBLISHED);
        } else {
            // check if it's already set - problem with multiple published timestamps
            String previousPassphrase = getPublicPassphrase();
            // if the previous passphrase is not null and it is the same one as is trying to be set, return successfully
            if (previousPassphrase != null && !previousPassphrase.equals("") && previousPassphrase.equals(passphrase)) {
                return;
            }

            // set the public passphrase
            AESFileEncoding.setProperty(FileEncoding.PROP_PASSPHRASE, passphrase);
            AESFileEncoding.setProperty(FileEncoding.PROP_TIMESTAMP_PUBLISHED, String.valueOf(TimeUtil.getTrancheTimestamp()));
        }
    }

    /**
     * <p>Clears the published passphrase.</p>
     * @throws java.lang.Exception
     */
    public final void clearPublicPassphrase() throws Exception {
        setPublicPassphrase(null);
    }

    /**
     * <p>Gets the published passphrase.</p>
     * @return
     * @throws java.lang.Exception
     */
    public synchronized final String getPublicPassphrase() throws Exception {
        // get the proper encoding
        FileEncoding AESFileEncoding = null;
        for (FileEncoding fe : uploaders.get(selectedUploader).getEncodings()) {
            if (fe.getName().equals(FileEncoding.AES)) {
                AESFileEncoding = fe;
                break;
            }
        }

        // make sure this meta data points to data that is actually encrypted
        if (AESFileEncoding == null) {
            throw new Exception("The data is not encrypted.");
        }

        // check for a public passphrase
        if (AESFileEncoding.getProperty(FileEncoding.PROP_PASSPHRASE) == null) {
            return null;
        }

        return AESFileEncoding.getProperty(FileEncoding.PROP_PASSPHRASE);
    }

    /**
     * <p>Returns whether the public passphrase is set.</p>
     * @return
     */
    public final boolean isPublicPassphraseSet() {
        try {
            return getPublicPassphrase() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 
     * @return
     */
    public synchronized final String getName() {
        return uploaders.get(selectedUploader).getProperties().get(PROP_NAME);
    }

    /**
     *
     * @return
     */
    public synchronized final long getTimestampUploaded() {
        try {
            return Long.valueOf(uploaders.get(selectedUploader).getProperties().get(PROP_TIMESTAMP_UPLOADED));
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     *
     * @return
     */
    public synchronized final String getRelativePathInDataSet() {
        return uploaders.get(selectedUploader).getProperties().get(PROP_PATH_IN_DATA_SET);
    }

    /**
     *
     * @return
     */
    public synchronized final String getMimeType() {
        return uploaders.get(selectedUploader).getProperties().get(PROP_MIME_TYPE);
    }

    /**
     *
     * @param mimeType
     */
    public synchronized final void setMimeType(String mimeType) {
        uploaders.get(selectedUploader).getProperties().put(PROP_MIME_TYPE, mimeType);
    }

    /**
     *
     * @return
     */
    public synchronized final long getTimestampFileModified() {
        try {
            return Long.valueOf(uploaders.get(selectedUploader).getProperties().get(PROP_TIMESTAMP_FILE));
        } catch (Exception e) {
            return getTimestampUploaded();
        }
    }

    /**
     *
     * @param timestamp
     */
    public synchronized final void setTimestampFileModified(long timestamp) {
        uploaders.get(selectedUploader).getProperties().put(PROP_TIMESTAMP_FILE, String.valueOf(timestamp));
    }

    /**
     *
     * @return
     */
    public synchronized final BigHash getNextVersion() {
        try {
            return BigHash.createHashFromString(uploaders.get(selectedUploader).getProperties().get(PROP_VERSION_NEXT));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     *
     * @param hash
     */
    public synchronized final void setNextVersion(BigHash hash) {
        uploaders.get(selectedUploader).getProperties().put(PROP_VERSION_NEXT, hash.toString());
    }

    /**
     *
     * @return
     */
    public synchronized final BigHash getPreviousVersion() {
        try {
            return BigHash.createHashFromString(uploaders.get(selectedUploader).getProperties().get(PROP_VERSION_PREVIOUS));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     *
     * @param hash
     */
    public synchronized final void setPreviousVersion(BigHash hash) {
        uploaders.get(selectedUploader).getProperties().put(PROP_VERSION_PREVIOUS, hash.toString());
    }

    /**
     *
     * @return
     */
    public synchronized final long getDataSetFiles() {
        try {
            return Long.valueOf(uploaders.get(selectedUploader).getProperties().get(PROP_DATA_SET_FILES));
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     *
     * @param files
     */
    public synchronized final void setDataSetFiles(long files) {
        uploaders.get(selectedUploader).getProperties().put(PROP_DATA_SET_FILES, String.valueOf(files));
    }

    /**
     *
     * @return
     */
    public synchronized final long getDataSetSize() {
        try {
            return Long.valueOf(uploaders.get(selectedUploader).getProperties().get(PROP_DATA_SET_SIZE));
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     *
     * @param size
     */
    public synchronized final void setDataSetSize(long size) {
        uploaders.get(selectedUploader).getProperties().put(PROP_DATA_SET_SIZE, String.valueOf(size));
    }

    /**
     *
     * @return
     */
    public synchronized final String getDataSetName() {
        return uploaders.get(selectedUploader).getProperties().get(PROP_DATA_SET_NAME);
    }

    /**
     *
     * @param name
     */
    public synchronized final void setDataSetName(String name) {
        uploaders.get(selectedUploader).getProperties().put(PROP_DATA_SET_NAME, name);
    }

    /**
     *
     * @return
     */
    public synchronized final String getDataSetDescription() {
        return uploaders.get(selectedUploader).getProperties().get(PROP_DATA_SET_DESCRIPTION);
    }

    /**
     *
     * @param description
     */
    public synchronized final void setDataSetDescription(String description) {
        uploaders.get(selectedUploader).getProperties().put(PROP_DATA_SET_DESCRIPTION, description);
    }

    /**
     * 
     * @return
     */
    public synchronized final boolean shareMetaDataIfEncrypted() {
        return uploaders.get(selectedUploader).getProperties().containsKey(PROP_SHARE_INFO_IF_ENCRYPTED) && uploaders.get(selectedUploader).getProperties().get(PROP_SHARE_INFO_IF_ENCRYPTED).toLowerCase().trim().equals("true");
    }

    /**
     *
     * @param shareMetaDataIfEncrypted
     */
    public synchronized final void setShareMetaDataIfEncrypted(boolean shareMetaDataIfEncrypted) {
        uploaders.get(selectedUploader).getProperties().put(PROP_SHARE_INFO_IF_ENCRYPTED, String.valueOf(shareMetaDataIfEncrypted));
    }

    /**
     * <p>Whether the meta information should be shared with persons that do not have the passphrase.</p>
     * @return
     */
    public final boolean shareMetaData() {
        return !isEncrypted() || (isEncrypted() && (isPublicPassphraseSet() || shareMetaDataIfEncrypted()));
    }

    /**
     * <p>Whether the file this meta data represents should be visible in searches.</p>
     * @return
     */
    public synchronized final boolean isHidden() {
        return uploaders.get(selectedUploader).getProperties().containsKey(PROP_HIDDEN) && uploaders.get(selectedUploader).getProperties().get(PROP_HIDDEN).toLowerCase().trim().equals("true");
    }

    /**
     * <p>Sets the data this meta data represents as hidden.</p>
     * @param isHidden
     */
    public synchronized void setHidden(boolean isHidden) {
        uploaders.get(selectedUploader).getProperties().put(PROP_HIDDEN, String.valueOf(isHidden));
    }

    /**
     * <p>Returns the host names for all the servers to which uploaders have designated that the file be stuck.</p>
     * @return A collection of Tranche server host names
     */
    public synchronized final Collection<String> getAllStickyServers() {
        Set<String> stickyServers = new HashSet<String>();
        for (int i = 0; i < uploaders.size(); i++) {
            selectedUploader = 0;
            stickyServers.addAll(getStickyServers());
        }
        return Collections.unmodifiableCollection(stickyServers);
    }

    /**
     * <p>Returns the host names for servers to which this uploader has designated that the file be stuck.</p>
     * @return A collection of Tranche server host names
     */
    public final Collection<String> getStickyServers() {
        return getAnnotationValuesWithName(MetaDataAnnotation.PROP_STICKY_SERVER_HOST);
    }

    /**
     * 
     * @param host
     */
    public final void addStickyServer(String host) {
        addAnnotation(MetaDataAnnotation.PROP_STICKY_SERVER_HOST, host);
    }

    /**
     * 
     * @param host
     */
    public final void removeStickyServer(String host) {
        removeAnnotation(MetaDataAnnotation.PROP_STICKY_SERVER_HOST, host);
    }

    /**
     * 
     * @return
     */
    @Override
    public final MetaData clone() {
        return new MetaData(this);
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
            MetaDataUtil.write(this, baos);
            this.version = saveVersion;
            return baos.toByteArray();
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    /**
     *
     * @param bytes
     * @return
     * @throws java.lang.Exception
     */
    public static final MetaData createFromBytes(byte[] bytes) throws Exception {
        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(bytes);
            return MetaDataUtil.read(bais);
        } finally {
            IOUtil.safeClose(bais);
        }
    }

    private final class MetaDataUploader {

        private Signature signature;
        /**
         * list of encodings, e.g. "GZIP" and "AES"
         */
        private final List<FileEncoding> encodings;
        private final Map<String, String> properties;
        private final List<MetaDataAnnotation> annotations;

        public MetaDataUploader() {
            signature = null;
            encodings = new ArrayList<FileEncoding>();
            properties = new HashMap<String, String>();
            annotations = new ArrayList<MetaDataAnnotation>();
        }

        public MetaDataUploader(Signature signature, ArrayList<FileEncoding> encodings, Map<String, String> properties, ArrayList<MetaDataAnnotation> annotations) {
            this.signature = signature;
            this.encodings = encodings;
            this.properties = properties;
            this.annotations = annotations;
        }

        public final Signature getSignature() {
            return signature;
        }

        public final void setSignature(Signature signature) {
            this.signature = signature;
        }

        public final List<FileEncoding> getEncodings() {
            return encodings;
        }

        public final Map<String, String> getProperties() {
            return properties;
        }

        public final List<MetaDataAnnotation> getAnnotations() {
            return annotations;
        }
    }
}
