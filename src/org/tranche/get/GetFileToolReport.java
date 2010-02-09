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
package org.tranche.get;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.tranche.remote.RemoteUtil;
import org.tranche.server.PropagationExceptionWrapper;
import org.tranche.time.TimeUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class GetFileToolReport implements Serializable {

    public static final int VERSION_ONE = 1;
    public static final int VERSION_LATEST = VERSION_ONE;
    private int version = VERSION_LATEST;
    private long timestampStart = TimeUtil.getTrancheTimestamp(),  timestampEnd = -1,  bytesDownloaded = -1,  filesDownloaded = -1;
    private List<PropagationExceptionWrapper> failureExceptions = new LinkedList<PropagationExceptionWrapper>();

    /**
     * 
     */
    public GetFileToolReport() {
    }

    /**
     *
     * @param is
     * @throws java.io.IOException
     */
    public GetFileToolReport(InputStream is) throws IOException {
        deserialize(is);
    }

    /**
     *
     * @return
     */
    public int getVersion() {
        return version;
    }

    /**
     *
     * @param version
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     *
     * @return
     */
    public long getTimestampStart() {
        return timestampStart;
    }

    /**
     *
     * @return
     */
    public long getTimestampEnd() {
        return timestampEnd;
    }

    /**
     *
     * @return
     */
    public long getTimeToFinish() {
        return timestampEnd - timestampStart;
    }

    /**
     *
     * @param endTimestamp
     */
    protected void setTimestampEnd(long endTimestamp) {
        this.timestampEnd = endTimestamp;
    }

    /**
     *
     * @return
     */
    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    /**
     *
     * @param bytesDownloaded
     */
    public void setBytesDownloaded(long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
    }

    /**
     *
     * @return
     */
    public long getFilesDownloaded() {
        return filesDownloaded;
    }

    /**
     *
     * @param filesDownloaded
     */
    public void setFilesDownloaded(long filesDownloaded) {
        this.filesDownloaded = filesDownloaded;
    }

    /**
     *
     * @return
     */
    public boolean isFailed() {
        return !failureExceptions.isEmpty();
    }

    /**
     *
     * @return
     */
    public boolean isFinished() {
        return timestampEnd != -1;
    }

    /**
     *
     * @return
     */
    public List<PropagationExceptionWrapper> getFailureExceptions() {
        return failureExceptions;
    }

    /**
     *
     * @param failureException
     */
    protected void addFailureException(PropagationExceptionWrapper failureException) {
        this.failureExceptions.add(failureException);
    }

    /**
     *
     * @param failureExceptions
     */
    protected void addFailureExceptions(Collection<PropagationExceptionWrapper> failureExceptions) {
        this.failureExceptions.addAll(failureExceptions);
    }

    /**
     * <p>Outputs the values of this row to the output stream.</p>
     * @param out The output stream.
     * @throws java.io.IOException
     */
    public void serialize(OutputStream out) throws IOException {
        serialize(VERSION_LATEST, out);
    }

    /**
     * <p>Outputs the values of this row in the structure defined by the given version to the given output stream.</p>
     * @param out The output stream
     * @throws IOException
     */
    protected void serialize(int version, OutputStream out) throws IOException {
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
    private void serializeVersionOne(OutputStream out) throws IOException {
        RemoteUtil.writeLong(timestampStart, out);
        RemoteUtil.writeLong(timestampEnd, out);
        RemoteUtil.writeLong(bytesDownloaded, out);
        RemoteUtil.writeLong(filesDownloaded, out);
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
    protected void deserialize(InputStream in) throws IOException {
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
    protected void deserializeVersionOne(InputStream in) throws IOException {
        timestampStart = RemoteUtil.readLong(in);
        timestampEnd = RemoteUtil.readLong(in);
        bytesDownloaded = RemoteUtil.readLong(in);
        filesDownloaded = RemoteUtil.readLong(in);
        failureExceptions = new LinkedList<PropagationExceptionWrapper>();
        int eCount = RemoteUtil.readInt(in);
        for (int i = 0; i < eCount; i++) {
            failureExceptions.add(new PropagationExceptionWrapper(in));
        }
    }
}
