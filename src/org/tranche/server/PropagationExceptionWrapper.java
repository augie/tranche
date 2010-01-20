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
package org.tranche.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import org.tranche.exceptions.ChunkAlreadyExistsSecurityException;
import org.tranche.exceptions.ChunkDoesNotBelongException;
import org.tranche.exceptions.ChunkDoesNotMatchHashException;
import org.tranche.exceptions.MetaDataIsCorruptedException;
import org.tranche.exceptions.NoHostProvidedException;
import org.tranche.exceptions.NoMatchingServersException;
import org.tranche.exceptions.OutOfDiskSpaceException;
import org.tranche.exceptions.PropagationFailedException;
import org.tranche.exceptions.PropagationUnfulfillableHostException;
import org.tranche.exceptions.RejectedRequestException;
import org.tranche.exceptions.ServerIsNotReadableException;
import org.tranche.exceptions.ServerIsNotWritableException;
import org.tranche.exceptions.ServerIsOfflineException;
import org.tranche.exceptions.TodoException;
import org.tranche.exceptions.TrancheProtocolException;
import org.tranche.exceptions.UnsupportedServerOperationException;
import org.tranche.hash.BigHash;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.remote.RemoteUtil;
import org.tranche.util.IOUtil;

/**
 * <p>Associate an exception with a particular host.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class PropagationExceptionWrapper implements Serializable {

    public static final int VERSION_ONE = 1;
    public static final int VERSION_LATEST = VERSION_ONE;
    public final int version;
    public final String host;
    public final BigHash hash;
    public final Exception exception;

    /**
     * 
     * @param exception
     */
    public PropagationExceptionWrapper(Exception exception) {
        this(VERSION_LATEST, exception, null, null);
    }

    /**
     * <p>Associate an exception with a particular host.</p>
     * @param exception
     * @param host
     */
    public PropagationExceptionWrapper(Exception exception, String host) {
        this(VERSION_LATEST, exception, host, null);
    }

    /**
     * 
     * @param exception
     * @param hash
     */
    public PropagationExceptionWrapper(Exception exception, BigHash hash) {
        this(VERSION_LATEST, exception, null, hash);
    }

    /**
     * 
     * @param exception
     * @param host
     * @param hash
     */
    public PropagationExceptionWrapper(Exception exception, String host, BigHash hash) {
        this(VERSION_LATEST, exception, host, hash);
    }

    /**
     * 
     * @param version
     * @param exception
     * @param host
     * @param hash
     */
    public PropagationExceptionWrapper(int version, Exception exception, String host, BigHash hash) {
        this.version = version;
        this.exception = exception;
        this.host = host;
        this.hash = hash;
    }

    /**
     *
     * @param in
     * @throws IOException
     */
    public PropagationExceptionWrapper(InputStream in) throws IOException {
        version = RemoteUtil.readInt(in);
        if (version == VERSION_ONE) {
            host = RemoteUtil.readLine(in);
            boolean isHash = RemoteUtil.readBoolean(in);
            if (isHash) {
                hash = RemoteUtil.readBigHash(in);
            } else {
                hash = null;
            }
            String exceptionName = RemoteUtil.readLine(in);
            String exceptionMessage = RemoteUtil.readLine(in);
            if (exceptionName.equals("IOException")) {
                exception = new IOException(exceptionMessage);
            } else if (exceptionName.equals("OutOfDiskSpaceException")) {
                exception = new OutOfDiskSpaceException();
            } else if (exceptionName.equals("ChunkAlreadyExistsSecurityException")) {
                exception = new ChunkAlreadyExistsSecurityException();
            } else if (exceptionName.equals("GeneralSecurityException")) {
                exception = new GeneralSecurityException(exceptionMessage);
            } else if (exceptionName.equals("RuntimeException")) {
                exception = new RuntimeException(exceptionMessage);
            } else if (exceptionName.equals("ServerIsNotReadableException")) {
                if (host != null) {
                    // read in updated status info and commit as update to current table
                    try {
                        NetworkUtil.updateRow(new StatusTableRow(in));
                    } catch (Exception e) {
                    }
                }
                exception = new ServerIsNotReadableException();
            } else if (exceptionName.equals("ServerIsNotWritableException")) {
                if (host != null) {
                    // read in updated status info and commit as update to current table
                    try {
                        NetworkUtil.updateRow(new StatusTableRow(in));
                    } catch (Exception e) {
                    }
                }
                exception = new ServerIsNotWritableException();
            } else if (exceptionName.equals("RejectedRequestException")) {
                exception = new RejectedRequestException();
            } else if (exceptionName.equals("NoMatchingServersException")) {
                exception = new NoMatchingServersException();
            } else if (exceptionName.equals("UnsupportedServerOperationException")) {
                exception = new UnsupportedServerOperationException();
            } else if (exceptionName.equals("ChunkDoesNotMatchHashException")) {
                exception = new ChunkDoesNotMatchHashException(exceptionMessage);
            } else if (exceptionName.equals("MetaDataIsCorruptedException")) {
                exception = new MetaDataIsCorruptedException();
            } else if (exceptionName.equals("TodoException")) {
                exception = new TodoException(exceptionMessage);
            } else if (exceptionName.equals("ChunkDoesNotBelongException")) {
                if (host != null) {
                    // read in updated status info and commit as update to current table
                    try {
                        NetworkUtil.updateRow(new StatusTableRow(in));
                    } catch (Exception e) {
                    }
                }
                exception = new ChunkDoesNotBelongException();
            } else if (exceptionName.equals("PropagationFailedException")) {
                exception = new PropagationFailedException();
            } else if (exceptionName.equals("ServerIsOfflineException")) {
                exception = new ServerIsOfflineException(host);
            } else if (exceptionName.equals("PropagationUnfulfillableHostException")) {
                exception = new PropagationUnfulfillableHostException();
            } else if (exceptionName.equals("NoHostProvidedException")) {
                exception = new NoHostProvidedException();
            } else if (exceptionName.equals("TrancheProtocolException")) {
                exception = new TrancheProtocolException();
            } else {
                exception = new IOException(exceptionMessage);
            }
        } else {
            throw new IOException("Unrecognized version.");
        }
    }

    @Override
    public String toString() {
        String string = "";
        if (host != null) {
            string = string + "Host: " + host + " ";
        }
        if (hash != null) {
            string = string + "Hash: " + hash + " ";
        }
        return string + exception.getClass().getName() + ": " + exception.getMessage();
    }

    /**
     * 
     * @param bytes
     * @return
     * @throws java.io.IOException
     */
    public static PropagationExceptionWrapper deserialize(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(bytes);
            return new PropagationExceptionWrapper(bais);
        } finally {
            IOUtil.safeClose(bais);
        }
    }

    /**
     * 
     * @param out
     * @throws IOException
     */
    public void serialize(OutputStream out) throws IOException {
        RemoteUtil.writeInt(version, out);
        if (version == VERSION_ONE) {
            serializeVersionOne(out);
        } else {
            throw new IOException("Unrecognized version.");
        }
    }

    /**
     * 
     * @param out
     * @throws IOException
     */
    public void serializeVersionOne(OutputStream out) throws IOException {
        RemoteUtil.writeLine(host, out);
        boolean isHash = hash != null;
        RemoteUtil.writeBoolean(isHash, out);
        if (isHash) {
            RemoteUtil.writeBigHash(hash, out);
        }
        RemoteUtil.writeLine(exception.getClass().getSimpleName(), out);
        RemoteUtil.writeLine(exception.getMessage(), out);
        if (host != null && (exception instanceof ServerIsNotReadableException || exception instanceof ServerIsNotWritableException || exception instanceof ServerIsOfflineException)) {
            NetworkUtil.getStatus().getRow(host).serialize(out);
        }
    }
}
