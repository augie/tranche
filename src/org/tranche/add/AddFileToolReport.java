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
package org.tranche.add;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.tranche.hash.BigHash;
import org.tranche.remote.RemoteUtil;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.time.TimeUtil;

/**
 * 
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class AddFileToolReport {

    public static final int VERSION_ONE = 1;
    public static final int VERSION_LATEST = VERSION_ONE;
    private int version = VERSION_LATEST;
    private String title,  description;
    private boolean isEncrypted,  showMetaDataIfEncrypted;
    private long timestampStart = TimeUtil.getTrancheTimestamp(),  timestampEnd = -1,  bytesUploaded = -1,  filesUploaded = -1, originalFileCount = 0,  originalBytesUploaded = 0;
    private BigHash hash;
    private List<PropagationExceptionWrapper> failureExceptions = new LinkedList<PropagationExceptionWrapper>();

    /**
     * 
     * @param timestampStart
     * @param title
     * @param description
     * @param isEncrypted
     * @param showMetaDataIfEncrypted
     */
    public AddFileToolReport(long timestampStart, String title, String description, boolean isEncrypted, boolean showMetaDataIfEncrypted, BigHash hash) {
        this.timestampStart = timestampStart;
        this.title = title;
        this.description = description;
        this.isEncrypted = isEncrypted;
        this.showMetaDataIfEncrypted = showMetaDataIfEncrypted;
        this.hash = hash;
    }

    /**
     *
     * @param is
     * @throws java.io.IOException
     */
    public AddFileToolReport(InputStream is) throws IOException {
        deserialize(is);
    }

    /**
     *
     * @return
     */
    public synchronized int getVersion() {
        return version;
    }

    /**
     *
     * @param version
     */
    public synchronized void setVersion(int version) {
        this.version = version;
    }

    /**
     *
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @return
     */
    public boolean isEncrypted() {
        return isEncrypted;
    }

    /**
     *
     * @return
     */
    public boolean isShowMetaDataIfEncrypted() {
        return showMetaDataIfEncrypted;
    }

    /**
     *
     * @return
     */
    public synchronized long getTimestampStart() {
        return timestampStart;
    }

    /**
     *
     * @return
     */
    public synchronized long getTimestampEnd() {
        return timestampEnd;
    }

    /**
     *
     * @param endTimestamp
     */
    protected synchronized void setTimestampEnd(long endTimestamp) {
        this.timestampEnd = endTimestamp;
    }

    /**
     *
     * @return
     */
    public synchronized long getOriginalFileCount() {
        return originalFileCount;
    }

    /**
     *
     * @param originalFileCount
     */
    protected synchronized void setOriginalFileCount(long originalFileCount) {
        this.originalFileCount = originalFileCount;
    }

    /**
     *
     * @return
     */
    public synchronized long getOriginalBytesUploaded() {
        return originalBytesUploaded;
    }

    /**
     *
     * @param originalBytesUploaded
     */
    protected synchronized void setOriginalBytesUploaded(long originalBytesUploaded) {
        this.originalBytesUploaded = originalBytesUploaded;
    }

    /**
     *
     * @return
     */
    public synchronized long getTimeToFinish() {
        return timestampEnd - timestampStart;
    }

    /**
     *
     * @return
     */
    public synchronized BigHash getHash() {
        return hash;
    }

    /**
     *
     * @param hash
     */
    protected synchronized void setHash(BigHash hash) {
        this.hash = hash;
    }

    /**
     *
     * @return
     */
    public synchronized long getBytesUploaded() {
        return bytesUploaded;
    }

    /**
     *
     * @param bytesUploaded
     */
    protected synchronized void setBytesUploaded(long bytesUploaded) {
        this.bytesUploaded = bytesUploaded;
    }

    /**
     *
     * @return
     */
    public synchronized long getFilesUploaded() {
        return filesUploaded;
    }

    /**
     *
     * @param filesUploaded
     */
    protected synchronized void setFilesUploaded(long filesUploaded) {
        this.filesUploaded = filesUploaded;
    }

    /**
     *
     * @return
     */
    public synchronized boolean isFailed() {
        return !failureExceptions.isEmpty();
    }

    /**
     *
     * @return
     */
    public synchronized boolean isFinished() {
        return timestampEnd != -1;
    }

    /**
     *
     * @return
     */
    public synchronized List<PropagationExceptionWrapper> getFailureExceptions() {
        return failureExceptions;
    }

    /**
     *
     * @param failureException
     */
    protected synchronized void addFailureException(PropagationExceptionWrapper failureException) {
        if (!failureExceptions.contains(failureException)) {
            failureExceptions.add(failureException);
        }
    }

    /**
     *
     * @param failureExceptions
     */
    protected synchronized void addFailureExceptions(Collection<PropagationExceptionWrapper> failureExceptions) {
        for (PropagationExceptionWrapper wrapper : failureExceptions) {
            addFailureException(wrapper);
        }
    }

    /**
     * <p>Outputs the values of this row to the output stream.</p>
     * @param out The output stream.
     * @throws java.io.IOException
     */
    public synchronized void serialize(OutputStream out) throws IOException {
        serialize(VERSION_LATEST, out);
    }

    /**
     * <p>Outputs the values of this row in the structure defined by the given version to the given output stream.</p>
     * @param out The output stream
     * @throws IOException
     */
    protected synchronized void serialize(int version, OutputStream out) throws IOException {
        if (version > VERSION_LATEST) {
            serialize(VERSION_LATEST, out);
            return;
        }
        RemoteUtil.writeInt(version, out);
        if (version == VERSION_ONE) {
            serializeVersionOne(out);
        } else {
            throw new IOException("Unrecognized version");
        }
    }

    /**
     *
     * @param out The output stream
     * @throws java.io.IOException
     */
    private synchronized void serializeVersionOne(OutputStream out) throws IOException {
        RemoteUtil.writeLong(timestampStart, out);
        RemoteUtil.writeLong(timestampEnd, out);
        RemoteUtil.writeBoolean(hash != null, out);
        if (hash != null) {
            RemoteUtil.writeBigHash(hash, out);
        }
        RemoteUtil.writeLong(bytesUploaded, out);
        RemoteUtil.writeLong(filesUploaded, out);
        RemoteUtil.writeLong(originalBytesUploaded, out);
        RemoteUtil.writeLong(originalFileCount, out);
        RemoteUtil.writeBoolean(isEncrypted, out);
        RemoteUtil.writeBoolean(showMetaDataIfEncrypted, out);
        RemoteUtil.writeLine(title, out);
        RemoteUtil.writeLine(description, out);
        RemoteUtil.writeInt(failureExceptions.size(), out);
        for (PropagationExceptionWrapper w : failureExceptions) {
            w.serialize(out);
        }
    }

    /**
     *
     * @param in An input stream containing a serialized report
     * @throws IOException
     */
    protected synchronized void deserialize(InputStream in) throws IOException {
        setVersion(RemoteUtil.readInt(in));
        if (getVersion() == VERSION_ONE) {
            deserializeVersionOne(in);
        } else {
            throw new IOException("Unrecognized version: " + getVersion());
        }
        setVersion(VERSION_LATEST);
    }

    /**
     *
     * @param in An input stream containing a version one serialized report
     * @throws java.io.IOException
     */
    protected synchronized void deserializeVersionOne(InputStream in) throws IOException {
        timestampStart = RemoteUtil.readLong(in);
        timestampEnd = RemoteUtil.readLong(in);
        boolean isHash = RemoteUtil.readBoolean(in);
        if (isHash) {
            hash = RemoteUtil.readBigHash(in);
        }
        bytesUploaded = RemoteUtil.readLong(in);
        filesUploaded = RemoteUtil.readLong(in);
        originalBytesUploaded = RemoteUtil.readLong(in);
        originalFileCount = RemoteUtil.readLong(in);
        isEncrypted = RemoteUtil.readBoolean(in);
        showMetaDataIfEncrypted = RemoteUtil.readBoolean(in);
        title = RemoteUtil.readLine(in);
        description = RemoteUtil.readLine(in);
        failureExceptions = new LinkedList<PropagationExceptionWrapper>();
        int eCount = RemoteUtil.readInt(in);
        for (int i = 0; i < eCount; i++) {
            failureExceptions.add(new PropagationExceptionWrapper(in));
        }
    }
}
