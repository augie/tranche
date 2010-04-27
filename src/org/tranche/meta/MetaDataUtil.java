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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.tranche.security.Signature;
import org.tranche.hash.Base64;
import org.tranche.hash.BigHash;
import org.tranche.project.ProjectFile;
import org.tranche.remote.RemoteUtil;
import org.tranche.time.TimeUtil;
import org.tranche.FileEncoding;
import org.tranche.commons.DebugUtil;
import org.tranche.util.IOUtil;

/**
 * <p>Utility for working with meta data. objects.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class MetaDataUtil {

    /**
     * 
     */
    private MetaDataUtil() {
    }

    /**
     * <p>A helper method to read a serialized MetaData object. This does *not* use Java's object serialization mechanism in order to enable versioned support across compiles of code and the ability to use split-files in non-Java programs.</p>
     * @param is The InputStream to read from.
     * @throws java.io.IOException Should any exception occur.
     * @return The unserialized MetaData object.
     */
    public static final MetaData read(InputStream is) throws Exception {
        DebugUtil.debugOut(MetaDataUtil.class, "Reading");
        // make a new MetaData object
        MetaData md = new MetaData();

        // first get the status big
        md.setFlags(RemoteUtil.readInt(is));

        // set the version
        if (md.isVersionOne()) {
            md.setVersion(MetaData.VERSION_ONE);
            // read as version one
            readVersionOneBody(md, is);
        } else {
            // read the version
            md.setVersion(RemoteUtil.readLine(is));
            // read version two
            if (md.getVersion().equals(MetaData.VERSION_TWO)) {
                readVersionTwoBody(md, is);
            } else if (md.getVersion().equals(MetaData.VERSION_THREE)) {
                readVersionThreeBody(md, is);
            } else if (md.getVersion().equals(MetaData.VERSION_FOUR)) {
                readVersionFourBody(md, is);
            } else {
                throw new RuntimeException("Invalid version.");
            }
        }

        // make modifications to versions prior to version four
        if (md.getVersion().equals(MetaData.VERSION_ONE) || md.getVersion().equals(MetaData.VERSION_TWO) || md.getVersion().equals(MetaData.VERSION_THREE)) {
            // need to fix annotations, properties from before version four
            List<MetaDataAnnotation> toRemove = new LinkedList<MetaDataAnnotation>();
            for (MetaDataAnnotation annotation : md.getAnnotations()) {
                // published timestamp moved to the file encoding properties
                if (annotation.getName().equals(MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP)) {
                    // make sure the AES encodings have the value
                    for (FileEncoding encoding : md.getEncodings()) {
                        if (encoding.getName().equals(FileEncoding.AES)) {
                            encoding.getProperties().setProperty(FileEncoding.PROP_TIMESTAMP_PUBLISHED, annotation.getValue());
                        }
                    }
                    toRemove.add(annotation);
                } // file updated timestamp moved to meta data properties
                else if (annotation.getName().equals(MetaDataAnnotation.FILE_LAST_MODIFIED_TIMESTAMP)) {
                    md.setProperty(MetaData.PROP_TIMESTAMP_FILE, annotation.getValue());
                    toRemove.add(annotation);
                } // previous upload version moved to meta data properties
                else if (annotation.getName().equals(MetaDataAnnotation.PROP_OLD_VERSION)) {
                    md.setProperty(MetaData.PROP_VERSION_PREVIOUS, annotation.getValue());
                    toRemove.add(annotation);
                } // next upload version moved to meta data properties
                else if (annotation.getName().equals(MetaDataAnnotation.PROP_NEW_VERSION)) {
                    md.setProperty(MetaData.PROP_VERSION_NEXT, annotation.getValue());
                    toRemove.add(annotation);
                } // share meta data if encrypted moved to meta data properties
                else if (annotation.getName().equals(MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getName())) {
                    md.setProperty(MetaData.PROP_SHARE_INFO_IF_ENCRYPTED, annotation.getValue());
                    toRemove.add(annotation);
                } // remove this annotation
                else if (annotation.getName().equals(MetaDataAnnotation.PROP_DELETE_OLD_VERSION)) {
                    toRemove.add(annotation);
                } // remove this annotation
                else if (annotation.getName().equals(MetaDataAnnotation.PROP_DELETE_NEW_VERSION)) {
                    toRemove.add(annotation);
                } // rename sticky server URL to sticky server host
                else if (annotation.getName().equals(MetaDataAnnotation.PROP_STICKY_SERVER_URL)) {
                    annotation.setName(MetaDataAnnotation.PROP_STICKY_SERVER_HOST);
                    try {
                        annotation.setValue(IOUtil.parseHost(annotation.getValue()));
                    } catch (Exception e) {
                        annotation.setValue(annotation.getValue());
                    }
                } // move deleted meta data annotation to hidden in properties
                else if (annotation.getName().equals(MetaDataAnnotation.PROP_DELETED)) {
                    md.setProperty(MetaData.PROP_HIDDEN, annotation.getValue());
                    toRemove.add(annotation);
                } // remove this annotation
                else if (annotation.getName().equals(MetaDataAnnotation.PROP_UNDELETED)) {
                    toRemove.add(annotation);
                } // remove this annotation
                else if (annotation.getName().equals(MetaDataAnnotation.PROJECT_ANNOTATED_MDA)) {
                    toRemove.add(annotation);
                }
            }
            for (MetaDataAnnotation annotation : toRemove) {
                md.removeAnnotation(annotation);
            }
            // update project file bit
            // TODO: do a read and write of all meta data to fix all the meta data
            // bug fix - meta data were written without the project file flag set for all data sets uploaded between November 2006 and June 2007, then remove this
            if (md.getName().equals(ProjectFile.OLD_PROJECT_FILE_NAME) && !md.isProjectFile()) {
                md.setIsProjectFile(true);
            }
        }

        // after reading in the meta data, update to the most recent version
        md.setVersion(MetaData.VERSION_LATEST);

        // no longer working with expiration timestamps as of version four
        if (md.isLimitedLife()) {
            md.setIsLimitedLife(false);
        }

        return md;
    }

    private static final void readVersionOneBody(MetaData md, InputStream is) throws Exception {
        DebugUtil.debugOut(MetaDataUtil.class, "Reading version one.");
        md.setVersion(MetaData.VERSION_ONE);

        // if GZIP'd, decompress on-the-fly
        if (md.isGZIPCompressed()) {
            is = new GZIPInputStream(is);
        }

        // uploader
        Signature signature = null;
        ArrayList<FileEncoding> encodings = new ArrayList<FileEncoding>();
        Map<String, String> properties = new HashMap<String, String>();
        ArrayList<MetaDataAnnotation> annotations = new ArrayList<MetaDataAnnotation>();
        ArrayList<BigHash> parts = new ArrayList<BigHash>();

        // get the timestamp
        long uploadTimestamp = RemoteUtil.readLong(is);
        properties.put(MetaData.PROP_TIMESTAMP_UPLOADED, String.valueOf(uploadTimestamp));
        md.setLastModifiedTimestamp(uploadTimestamp);

        // check if an expiration timestamp should exist
        if (md.isLimitedLife()) {
            // throw away the expiration timestamp
            RemoteUtil.readLong(is);
        }

        // read the name
        properties.put(MetaData.PROP_NAME, RemoteUtil.readLine(is));

        // conditionally read the MIME type
        if (md.isMimeType()) {
            properties.put(MetaData.PROP_MIME_TYPE, RemoteUtil.readLine(is));
        }

        // read in the encodings
        int numberOfEncodings = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfEncodings; i++) {
            // get the name
            String name = RemoteUtil.readLine(is);
            // get all the properties
            int propertiesSize = Integer.parseInt(RemoteUtil.readLine(is));
            Properties props = new Properties();
            for (int j = 0; j < propertiesSize; j++) {
                String propName = RemoteUtil.readLine(is);
                String propValue = null;
                // specially handle hashes
                if (propName.equals(FileEncoding.PROP_HASH)) {
                    byte[] bytes = RemoteUtil.readDataBytes(is);
                    propValue = Base64.encodeBytes(bytes, 0, bytes.length, Base64.DONT_BREAK_LINES);
                } else {
                    propValue = RemoteUtil.readLine(is);
                }
                // add to the properties
                props.setProperty(propName, propValue);
            }
            // make the encoding
            encodings.add(new FileEncoding(name, props));
        }

        // read in all of the hashes
        int numberOfParts = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfParts; i++) {
            parts.add(BigHash.createFromBytes(RemoteUtil.readDataBytes(is)));
        }
        if (!encodings.isEmpty()) {
            md.setParts(encodings.get(encodings.size() - 1).getHash(), parts);
        }

        // read in all the uploader signatures
        int numberOfSignatures = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfSignatures; i++) {
            signature = RemoteUtil.readSignature(is);
            break;
        }

        // read in all the annotations
        int numberOfAnnotations = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfAnnotations; i++) {
            String value = RemoteUtil.readLine(is);
            String name = RemoteUtil.readLine(is);
            annotations.add(new MetaDataAnnotation(name, value));
        }

        // add the uploader
        md.addUploader(signature, encodings, properties, annotations);
    }

    private static final void readVersionTwoBody(MetaData md, InputStream is) throws Exception {
        DebugUtil.debugOut(MetaDataUtil.class, "Reading version two.");

        // if GZIP'd, decompress on-the-fly
        if (md.isGZIPCompressed()) {
            is = new GZIPInputStream(is);
        }

        // uploader
        Signature signature = null;
        ArrayList<FileEncoding> encodings = new ArrayList<FileEncoding>();
        Map<String, String> properties = new HashMap<String, String>();
        ArrayList<MetaDataAnnotation> annotations = new ArrayList<MetaDataAnnotation>();
        ArrayList<BigHash> parts = new ArrayList<BigHash>();

        // get the timestamp
        long uploadTimestamp = RemoteUtil.readLong(is);
        properties.put(MetaData.PROP_TIMESTAMP_UPLOADED, String.valueOf(uploadTimestamp));

        // set last modified timestamp
        md.setLastModifiedTimestamp(RemoteUtil.readLong(is));

        // check if an expiration timestamp should exist
        if (md.isLimitedLife()) {
            // throw away the expiration timestamp
            RemoteUtil.readLong(is);
        }

        // read the name
        properties.put(MetaData.PROP_NAME, RemoteUtil.readLine(is));

        // conditionally read the MIME type
        if (md.isMimeType()) {
            properties.put(MetaData.PROP_MIME_TYPE, RemoteUtil.readLine(is));
        }

        // read in the encodings
        int numberOfEncodings = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfEncodings; i++) {
            // get the name
            String name = RemoteUtil.readLine(is);
            // get all the properties
            int propertiesSize = Integer.parseInt(RemoteUtil.readLine(is));
            Properties props = new Properties();
            for (int j = 0; j < propertiesSize; j++) {
                String propName = RemoteUtil.readLine(is);
                String propValue = null;
                // specially handle hashes
                if (propName.equals(FileEncoding.PROP_HASH)) {
                    byte[] bytes = RemoteUtil.readDataBytes(is);
                    propValue = Base64.encodeBytes(bytes, 0, bytes.length, Base64.DONT_BREAK_LINES);
                } else {
                    propValue = RemoteUtil.readLine(is);
                }
                // add to the properties
                props.setProperty(propName, propValue);
            }
            // make the encoding
            encodings.add(new FileEncoding(name, props));
        }

        // read in all of the hashes
        int numberOfParts = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfParts; i++) {
            parts.add(BigHash.createFromBytes(RemoteUtil.readDataBytes(is)));
        }
        if (!encodings.isEmpty()) {
            md.setParts(encodings.get(encodings.size() - 1).getHash(), parts);
        }

        // read in all the uploader signatures
        int numberOfSignatures = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfSignatures; i++) {
            if (signature == null) {
                signature = RemoteUtil.readSignature(is);
            }
        }

        // read in all the annotations
        int numberOfAnnotations = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfAnnotations; i++) {
            String value = RemoteUtil.readLine(is);
            String name = RemoteUtil.readLine(is);
            annotations.add(new MetaDataAnnotation(name, value));
        }

        // add the uploader
        md.addUploader(signature, encodings, properties, annotations);
    }

    private static final void readVersionThreeBody(MetaData md, InputStream is) throws Exception {
        DebugUtil.debugOut(MetaDataUtil.class, "Reading version three.");
        // if GZIP'd, decompress on-the-fly
        if (md.isGZIPCompressed()) {
            is = new GZIPInputStream(is);
        }

        // uploader
        Signature signature = null;
        ArrayList<FileEncoding> encodings = new ArrayList<FileEncoding>();
        Map<String, String> properties = new HashMap<String, String>();
        ArrayList<MetaDataAnnotation> annotations = new ArrayList<MetaDataAnnotation>();
        ArrayList<BigHash> parts = new ArrayList<BigHash>();

        // get the timestamp
        long uploadTimestamp = RemoteUtil.readLong(is);
        properties.put(MetaData.PROP_TIMESTAMP_UPLOADED, String.valueOf(uploadTimestamp));

        // set last modified timestamp
        md.setLastModifiedTimestamp(RemoteUtil.readLong(is));

        // check if an expiration timestamp should exist
        if (md.isLimitedLife()) {
            // throw away the expiration timestamp
            RemoteUtil.readLong(is);
        }

        // read the name
        properties.put(MetaData.PROP_NAME, RemoteUtil.readLine(is));

        // conditionally read the MIME type
        if (md.isMimeType()) {
            properties.put(MetaData.PROP_MIME_TYPE, RemoteUtil.readLine(is));
        }

        // read in the encodings
        int numberOfEncodings = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfEncodings; i++) {
            // get the name
            String name = RemoteUtil.readLine(is);
            // get all the properties
            int propertiesSize = Integer.parseInt(RemoteUtil.readLine(is));
            Properties props = new Properties();
            for (int j = 0; j < propertiesSize; j++) {
                String propName = RemoteUtil.readLine(is);
                String propValue = null;
                // specially handle hashes
                if (propName.equals(FileEncoding.PROP_HASH)) {
                    byte[] bytes = RemoteUtil.readDataBytes(is);
                    propValue = Base64.encodeBytes(bytes, 0, bytes.length, Base64.DONT_BREAK_LINES);
                } else {
                    propValue = RemoteUtil.readLine(is);
                }
                // add to the properties
                props.setProperty(propName, propValue);
            }
            // make the encoding
            encodings.add(new FileEncoding(name, props));
        }

        // read in all of the hashes
        int numberOfParts = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfParts; i++) {
            parts.add(BigHash.createFromBytes(RemoteUtil.readDataBytes(is)));
        }
        if (!encodings.isEmpty()) {
            md.setParts(encodings.get(encodings.size() - 1).getHash(), parts);
        }

        // read in all the uploader signatures
        int numberOfSignatures = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfSignatures; i++) {
            if (signature == null) {
                signature = RemoteUtil.readSignature(is);
            }
        }

        // read in all the annotations
        int numberOfAnnotations = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfAnnotations; i++) {
            String value = RemoteUtil.readLine(is);
            String name = RemoteUtil.readLine(is);
            annotations.add(new MetaDataAnnotation(name, value));
        }

        // read all the properties
        int numberOfProperties = Integer.parseInt(RemoteUtil.readLine(is));
        for (int i = 0; i < numberOfProperties; i++) {
            String name = RemoteUtil.readLine(is);
            String value = RemoteUtil.readLine(is);
            properties.put(name, value);
        }

        // add the uploader
        md.addUploader(signature, encodings, properties, annotations);
    }

    private static final void readVersionFourBody(MetaData md, InputStream is) throws Exception {
        DebugUtil.debugOut(MetaDataUtil.class, "Reading version four.");

        // set last modified timestamp
        md.setLastModifiedTimestamp(RemoteUtil.readLong(is));
        DebugUtil.debugOut(MetaDataUtil.class, " Last modified timestamp: " + md.getLastModifiedTimestamp());

        // parts
        int numPartSets = RemoteUtil.readInt(is);
        DebugUtil.debugOut(MetaDataUtil.class, " Part set count: " + numPartSets);
        for (int i = 0; i < numPartSets; i++) {
            BigHash finalEncodingHash = RemoteUtil.readBigHash(is);
            DebugUtil.debugOut(MetaDataUtil.class, "  Part set " + i + ": " + finalEncodingHash);
            int numParts = RemoteUtil.readInt(is);
            DebugUtil.debugOut(MetaDataUtil.class, "  Part count: " + numParts);
            ArrayList<BigHash> parts = new ArrayList<BigHash>();
            for (int j = 0; j < numParts; j++) {
                BigHash partHash = RemoteUtil.readBigHash(is);
                DebugUtil.debugOut(MetaDataUtil.class, "   Part " + j + ": " + partHash);
                parts.add(partHash);
            }
            md.setParts(finalEncodingHash, parts);
        }

        // uploaders
        int numUploaders = RemoteUtil.readInt(is);
        for (int i = 0; i < numUploaders; i++) {
            // uploader
            Signature signature = null;
            ArrayList<FileEncoding> encodings = new ArrayList<FileEncoding>();
            Map<String, String> properties = new HashMap<String, String>();
            ArrayList<MetaDataAnnotation> annotations = new ArrayList<MetaDataAnnotation>();

            // read the signature
            signature = RemoteUtil.readSignature(is);

            // get the encodings
            int numEncodings = RemoteUtil.readInt(is);
            for (int j = 0; j < numEncodings; j++) {
                // get the name
                String name = RemoteUtil.readLine(is);
                // get all the properties
                int propertiesSize = RemoteUtil.readInt(is);
                Properties props = new Properties();
                for (int k = 0; k < propertiesSize; k++) {
                    String propName = RemoteUtil.readLine(is);
                    String propValue = null;
                    // specially handle hashes
                    if (propName.equals(FileEncoding.PROP_HASH)) {
                        byte[] bytes = RemoteUtil.readDataBytes(is);
                        propValue = Base64.encodeBytes(bytes, 0, bytes.length, Base64.DONT_BREAK_LINES);
                    } else {
                        propValue = RemoteUtil.readLine(is);
                    }
                    // add to the properties
                    props.setProperty(propName, propValue);
                }
                // make the encoding
                encodings.add(new FileEncoding(name, props));
            }

            // get the properties
            int numberOfProperties = RemoteUtil.readInt(is);
            for (int j = 0; j < numberOfProperties; j++) {
                String name = RemoteUtil.readLine(is);
                String value = RemoteUtil.readLine(is);
                properties.put(name, value);
            }

            // read in all the annotations
            int numberOfAnnotations = RemoteUtil.readInt(is);
            for (int j = 0; j < numberOfAnnotations; j++) {
                String value = RemoteUtil.readLine(is);
                String name = RemoteUtil.readLine(is);
                annotations.add(new MetaDataAnnotation(name, value));
            }

            md.addUploader(signature, encodings, properties, annotations);
        }
    }

    /**
     * <p>Writes a MetaData object to the given OutputStream. This method does *not* use Java's object serialization mechanism in order to let non-Java code use them.</p>
     * @param md The MetaData object to serialize.
     * @param out The OutputStream to serialize the file to.
     * @throws java.io.IOException Should any exception occur.
     */
    public static final void write(MetaData md, OutputStream out) throws Exception {
        DebugUtil.debugOut(MetaDataUtil.class, "Writing.");
        // update the last modified timestamp
        md.setLastModifiedTimestamp(TimeUtil.getTrancheTimestamp());

        // storing info in meta data as version four -- need to move annotation, property info based on previous versions
        // mostly used for testing purposes
        if (md.getVersion().equals(MetaData.VERSION_ONE) || md.getVersion().equals(MetaData.VERSION_TWO) || md.getVersion().equals(MetaData.VERSION_THREE)) {
            // next version
            if (md.getNextVersion() != null) {
                md.addAnnotation(MetaDataAnnotation.PROP_NEW_VERSION, md.getNextVersion().toString());
            }
            // previous version
            if (md.getPreviousVersion() != null) {
                md.addAnnotation(MetaDataAnnotation.PROP_OLD_VERSION, md.getPreviousVersion().toString());
            }
            // hidden
            if (md.isHidden()) {
                md.addAnnotation(MetaDataAnnotation.PROP_DELETED, "true");
            }
            // share info if encrypted
            if (md.shareMetaDataIfEncrypted()) {
                md.addAnnotation(MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getName(), MetaDataAnnotation.SHARE_META_DATA_IF_ENCRYPTED_ANNOTATION.getValue());
            }
            // file last modified timestamp
            if (md.getTimestampFileModified() >= 0) {
                md.addAnnotation(MetaDataAnnotation.FILE_LAST_MODIFIED_TIMESTAMP, String.valueOf(md.getTimestampFileModified()));
            }

            // check encodings properties
            for (FileEncoding encoding : md.getEncodings()) {
                if (encoding.getName().equals(FileEncoding.AES)) {
                    for (Object o : encoding.getProperties().keySet()) {
                        String propertyName = (String) o;
                        if (propertyName.equals(FileEncoding.PROP_TIMESTAMP_PUBLISHED)) {
                            md.addAnnotation(MetaDataAnnotation.PROP_PUBLISHED_TIMESTAMP, encoding.getProperty(FileEncoding.PROP_TIMESTAMP_PUBLISHED));
                        }
                    }
                    encoding.getProperties().remove(FileEncoding.PROP_TIMESTAMP_PUBLISHED);
                }
            }

            // need to fix annotations, properties from before version four
            List<MetaDataAnnotation> toRemove = new LinkedList<MetaDataAnnotation>();
            for (MetaDataAnnotation annotation : md.getAnnotations()) {
                // rename sticky server URL to sticky server host
                if (annotation.getName().equals(MetaDataAnnotation.PROP_STICKY_SERVER_HOST)) {
                    annotation.setName(MetaDataAnnotation.PROP_STICKY_SERVER_URL);
                }
            }
            for (MetaDataAnnotation annotation : toRemove) {
                md.removeAnnotation(annotation);
            }
        }

        // write
        if (md.isVersionOne()) {
            writeVersionOne(md, out);
        } else {
            // write out
            if (md.getVersion().equals(MetaData.VERSION_TWO)) {
                writeVersionTwo(md, out);
            } else if (md.getVersion().equals(MetaData.VERSION_THREE)) {
                writeVersionThree(md, out);
            } else if (md.getVersion().equals(MetaData.VERSION_FOUR)) {
                writeVersionFour(md, out);
            } else {
                throw new RuntimeException("Invalid version.");
            }
        }
    }

    private static final void writeVersionOne(MetaData md, OutputStream out) throws Exception {
        DebugUtil.debugOut(MetaDataUtil.class, "Writing version one.");
        writeHeader(md, out);

        // if the GZIP flag is set, GZIP compress
        GZIPOutputStream gos = null;
        if (md.isGZIPCompressed()) {
            gos = new GZIPOutputStream(out);
            out = gos;
        }

        // write out the timestamp
        RemoteUtil.writeLong(md.getTimestampUploaded(), out);

        // conditionally write the expiration
        if (md.isLimitedLife()) {
            // no longer support expiration
            RemoteUtil.writeLong(0, out);
        }

        // write out the name
        RemoteUtil.writeLine(md.getName(), out);

        // conditionally write out the mime-type
        if (md.isMimeType()) {
            RemoteUtil.writeLine(md.getMimeType(), out);
        }

        // serialize the encodings
        List<FileEncoding> encodings = md.getEncodings();
        // write out the number of encodings
        RemoteUtil.writeLine(Integer.toString(encodings.size()), out);
        for (FileEncoding fe : encodings) {
            // write out the length of the name
            RemoteUtil.writeLine(fe.getName(), out);
            // write out the properties
            Properties props = fe.getProperties();
            Set keys = props.keySet();
            String[] sortedKeys = (String[]) keys.toArray(new String[0]);
            Arrays.sort(sortedKeys);
            RemoteUtil.writeLine(Integer.toString(props.size()), out);
            for (Object key : sortedKeys) {
                String k = (String) key;
                String value = props.getProperty(k);
                // write out the key
                RemoteUtil.writeLine(k, out);
                // if it is the hash, specially handle it
                if (k.equals(FileEncoding.PROP_HASH)) {
                    RemoteUtil.writeData(Base64.decode(value), out);
                } else {
                    // write key/value
                    RemoteUtil.writeLine(value, out);
                }
            }
        }

        // write out the parts
        List<BigHash> fileParts = md.getParts();
        RemoteUtil.writeLine(Integer.toString(fileParts.size()), out);
        for (BigHash hash : fileParts) {
            RemoteUtil.writeData(hash.toByteArray(), out);
        }

        // write the signature
        RemoteUtil.writeLine("1", out);
        RemoteUtil.writeSignature(md.getSignature(), out);

        // write the annotation links
        Collection<MetaDataAnnotation> annotations = md.getAnnotations();
        RemoteUtil.writeLine(Integer.toString(annotations.size()), out);
        for (MetaDataAnnotation mda : annotations) {
            RemoteUtil.writeLine(mda.getValue(), out);
            RemoteUtil.writeLine(mda.getName(), out);
        }

        // flush
        out.flush();
        // finish GZIP
        if (gos != null) {
            gos.finish();
        }
    }

    private static final void writeVersionTwo(MetaData md, OutputStream out) throws Exception {
        DebugUtil.debugOut(MetaDataUtil.class, "Writing version two.");
        writeHeader(md, out);

        // if the GZIP flag is set, GZIP compress
        GZIPOutputStream gos = null;
        if (md.isGZIPCompressed()) {
            gos = new GZIPOutputStream(out);
            out = gos;
        }

        // write out the timestamp
        RemoteUtil.writeLong(md.getTimestampUploaded(), out);
        RemoteUtil.writeLong(md.getLastModifiedTimestamp(), out);

        // conditionally write the expiration
        if (md.isLimitedLife()) {
            // no longer support expiration
            RemoteUtil.writeLong(0, out);
        }

        // write out the name
        RemoteUtil.writeLine(md.getName(), out);

        // conditionally write out the mime-type
        if (md.isMimeType()) {
            RemoteUtil.writeLine(md.getMimeType(), out);
        }

        // serialize the encodings
        List<FileEncoding> encodings = md.getEncodings();
        // write out the number of encodings
        RemoteUtil.writeLine(Integer.toString(encodings.size()), out);
        for (FileEncoding fe : encodings) {
            // write out the length of the name
            RemoteUtil.writeLine(fe.getName(), out);
            // write out the properties
            Properties props = fe.getProperties();
            Set keys = props.keySet();
            String[] sortedKeys = (String[]) keys.toArray(new String[0]);
            Arrays.sort(sortedKeys);
            RemoteUtil.writeLine(Integer.toString(props.size()), out);
            for (Object key : sortedKeys) {
                String k = (String) key;
                String value = props.getProperty(k);
                // write out the key
                RemoteUtil.writeLine(k, out);
                // if it is the hash, specially handle it
                if (k.equals(FileEncoding.PROP_HASH)) {
                    RemoteUtil.writeData(Base64.decode(value), out);
                } else {
                    // write key/value
                    RemoteUtil.writeLine(value, out);
                }
            }
        }

        // write out the parts
        List<BigHash> fileParts = md.getParts();
        RemoteUtil.writeLine(Integer.toString(fileParts.size()), out);
        for (BigHash hash : fileParts) {
            RemoteUtil.writeData(hash.toByteArray(), out);
        }

        // write the signature
        RemoteUtil.writeLine("1", out);
        RemoteUtil.writeSignature(md.getSignature(), out);

        // write the annotation links
        Collection<MetaDataAnnotation> annotations = md.getAnnotations();
        RemoteUtil.writeLine(Integer.toString(annotations.size()), out);
        for (MetaDataAnnotation mda : annotations) {
            RemoteUtil.writeLine(mda.getValue(), out);
            RemoteUtil.writeLine(mda.getName(), out);
        }

        // flush
        out.flush();
        // finish GZIP
        if (gos != null) {
            gos.finish();
        }
    }

    private static final void writeVersionThree(MetaData md, OutputStream out) throws Exception {
        DebugUtil.debugOut(MetaDataUtil.class, "Writing version three.");
        writeHeader(md, out);

        // if the GZIP flag is set, GZIP compress
        GZIPOutputStream gos = null;
        if (md.isGZIPCompressed()) {
            gos = new GZIPOutputStream(out);
            out = gos;
        }

        // write out the timestamp
        RemoteUtil.writeLong(md.getTimestampUploaded(), out);
        RemoteUtil.writeLong(md.getLastModifiedTimestamp(), out);

        // conditionally write the expiration
        if (md.isLimitedLife()) {
            // no longer support expiration
            RemoteUtil.writeLong(0, out);
        }

        // write out the name
        RemoteUtil.writeLine(md.getName(), out);

        // conditionally write out the mime-type
        if (md.isMimeType()) {
            RemoteUtil.writeLine(md.getMimeType(), out);
        }

        // serialize the encodings
        List<FileEncoding> encodings = md.getEncodings();
        // write out the number of encodings
        RemoteUtil.writeLine(Integer.toString(encodings.size()), out);
        for (FileEncoding fe : encodings) {
            // write out the length of the name
            RemoteUtil.writeLine(fe.getName(), out);
            // write out the properties
            Properties props = fe.getProperties();
            Set keys = props.keySet();
            String[] sortedKeys = (String[]) keys.toArray(new String[0]);
            Arrays.sort(sortedKeys);
            RemoteUtil.writeLine(Integer.toString(props.size()), out);
            for (Object key : sortedKeys) {
                String k = (String) key;
                String value = props.getProperty(k);
                // write out the key
                RemoteUtil.writeLine(k, out);
                // if it is the hash, specially handle it
                if (k.equals(FileEncoding.PROP_HASH)) {
                    RemoteUtil.writeData(Base64.decode(value), out);
                } else {
                    // write key/value
                    RemoteUtil.writeLine(value, out);
                }
            }
        }

        // write out the parts
        List<BigHash> fileParts = md.getParts();
        RemoteUtil.writeLine(Integer.toString(fileParts.size()), out);
        for (BigHash hash : fileParts) {
            RemoteUtil.writeData(hash.toByteArray(), out);
        }

        // write the signature
        RemoteUtil.writeLine("1", out);
        RemoteUtil.writeSignature(md.getSignature(), out);

        // write the annotation links
        Collection<MetaDataAnnotation> annotations = md.getAnnotations();
        RemoteUtil.writeLine(Integer.toString(annotations.size()), out);
        for (MetaDataAnnotation mda : annotations) {
            RemoteUtil.writeLine(mda.getValue(), out);
            RemoteUtil.writeLine(mda.getName(), out);
        }

        // write out the properties
        Map<String, String> properties = md.getProperties();
        RemoteUtil.writeLine(Integer.toString(properties.size()), out);
        for (String name : properties.keySet()) {
            RemoteUtil.writeLine(name, out);
            RemoteUtil.writeLine(properties.get(name), out);
        }

        // flush
        out.flush();
        // finish GZIP
        if (gos != null) {
            gos.finish();
        }
    }

    private static final void writeVersionFour(MetaData md, OutputStream out) throws Exception {
        DebugUtil.debugOut(MetaDataUtil.class, "Writing version four.");
        writeHeader(md, out);

        // write out the last modified timestamp
        RemoteUtil.writeLong(md.getLastModifiedTimestamp(), out);

        // write the parts
        Map<BigHash, List<BigHash>> allParts = md.getAllParts();
        RemoteUtil.writeInt(allParts.size(), out);
        for (BigHash finalEncodingHash : allParts.keySet()) {
            RemoteUtil.writeBigHash(finalEncodingHash, out);
            RemoteUtil.writeInt(allParts.get(finalEncodingHash).size(), out);
            for (BigHash partHash : allParts.get(finalEncodingHash)) {
                RemoteUtil.writeBigHash(partHash, out);
            }
        }

        // uploaders
        RemoteUtil.writeInt(md.getUploaderCount(), out);
        for (int i = 0; i < md.getUploaderCount(); i++) {
            // select uploader
            md.selectUploader(i);

            // read the signature
            RemoteUtil.writeSignature(md.getSignature(), out);

            // encodings
            RemoteUtil.writeInt(md.getEncodings().size(), out);
            for (FileEncoding fe : md.getEncodings()) {
                // write out the length of the name
                RemoteUtil.writeLine(fe.getName(), out);
                // write out the properties
                Properties props = fe.getProperties();
                Set keys = props.keySet();
                String[] sortedKeys = (String[]) keys.toArray(new String[0]);
                Arrays.sort(sortedKeys);
                RemoteUtil.writeInt(props.size(), out);
                for (Object key : sortedKeys) {
                    String k = (String) key;
                    String value = props.getProperty(k);
                    // write out the key
                    RemoteUtil.writeLine(k, out);
                    // if it is the hash, specially handle it
                    if (k.equals(FileEncoding.PROP_HASH)) {
                        RemoteUtil.writeData(Base64.decode(value), out);
                    } else {
                        // write key/value
                        RemoteUtil.writeLine(value, out);
                    }
                }
            }

            // properties
            RemoteUtil.writeInt(md.getProperties().size(), out);
            for (String name : md.getProperties().keySet()) {
                RemoteUtil.writeLine(name, out);
                RemoteUtil.writeLine(md.getProperties().get(name), out);
            }

            // read in all the annotations
            RemoteUtil.writeInt(md.getAnnotations().size(), out);
            for (MetaDataAnnotation annotation : md.getAnnotations()) {
                RemoteUtil.writeLine(annotation.getValue(), out);
                RemoteUtil.writeLine(annotation.getName(), out);
            }
        }

        // flush
        out.flush();
    }

    private static final void writeHeader(MetaData md, OutputStream out) throws Exception {
        // determine isMimeType -- easier to do here than in new version of meta data
        md.setIsMimeType(md.getMimeType() != null);

        // write the flags before anything else
        RemoteUtil.writeInt(md.getFlags(), out);
        out.flush();

        // write version if not version one
        if (!md.isVersionOne()) {
            // write the version number
            RemoteUtil.writeLine(md.getVersion(), out);
            out.flush();
        }
    }
}
