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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.remote.RemoteUtil;
import org.tranche.util.IOUtil;

/**
 * <p>Since propagation must return information about which servers fail, we need to wrap return types and exceptions together.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class PropagationReturnWrapper implements Serializable {

    public static final int VERSION_ONE = 1;
    public static final int VERSION_LATEST = VERSION_ONE;
    public static final byte FLAG_VOID = 1 << 0;
    public static final byte FLAG_BOOLEAN = 1 << 1;
    public static final byte FLAG_BYTE_1D = 1 << 2;
    public static final byte FLAG_BYTE_2D = 1 << 3;
    public static final byte FLAG_BYTE_3D = 1 << 4;
    private int version = VERSION_LATEST;
    private final Set<PropagationExceptionWrapper> propagationExceptionWrapperSet;
    private final Object returnObj;

    /**
     * <p>Since propagation must return information about which servers fail, we need to wrap return types and exceptions together.</p>
     * <p>Use this constructor when method is void return type.</p>
     * @param set Set of exceptions. Is usually empty.
     */
    public PropagationReturnWrapper(Set<PropagationExceptionWrapper> set) {
        this(set, null);
    }

    /**
     * <p>Since propagation must return information about which servers fail, we need to wrap return types and exceptions together.</p>
     * <p>Use this constructor when method returns a value. (Use other constructor if void).</p>
     * <p>Do not set a null value if there is a return type. If method returns a null value, create an empty array or throw a NullPointerException.</p>
     * @param set Set of exceptions. Is usually empty.
     * @param returnObj If null, then treated as void; otherwise, this is the return object of type Boolean, byte[], byte[][], or byte[][][]
     */
    public PropagationReturnWrapper(Set<PropagationExceptionWrapper> set, Object returnObj) {
        this.propagationExceptionWrapperSet = set;
        this.returnObj = returnObj;
    }

    /**
     *
     * @param in
     * @throws IOException
     */
    public PropagationReturnWrapper(InputStream in) throws IOException {
        version = RemoteUtil.readInt(in);
        
        // set of exceptions
        propagationExceptionWrapperSet = new HashSet<PropagationExceptionWrapper>();
        int size = RemoteUtil.readInt(in);
        for (int i = 0; i < size; i++) {
            propagationExceptionWrapperSet.add(new PropagationExceptionWrapper(in));
        }

        // flag
        byte flag = RemoteUtil.readByte(in);

        // return object
        switch (flag) {
            case FLAG_VOID:
                returnObj = null;
                break;
            case FLAG_BOOLEAN:
                returnObj = RemoteUtil.readBoolean(in);
                break;
            case FLAG_BYTE_1D:
                returnObj = RemoteUtil.readDataBytes(in);
                break;
            case FLAG_BYTE_2D:
                returnObj = RemoteUtil.read2dData(in);
                break;
            case FLAG_BYTE_3D:
                returnObj = RemoteUtil.read3dData(in);
                break;
            default:
                throw new AssertionFailedException("Unrecognized flag: " + flag);
        }
    }

    /**
     * <p>Returns true if there were any errors while propogating request.</p>
     * @return
     */
    public boolean isAnyErrors() {
        return propagationExceptionWrapperSet.size() > 0;
    }

    /**
     * <p>Returns collection of errors that occurred while propogating request.</p>
     * @return
     * @see #isAnyErrors() 
     */
    public Set<PropagationExceptionWrapper> getErrors() {
        return propagationExceptionWrapperSet;
    }

    /**
     * <p>Returns true if and only if method is void.</p>
     * @return
     */
    public boolean isVoid() {
        return returnObj == null;
    }

    /**
     * <p>Returns true if and only if method return boolean value.</p>
     * @return
     */
    public boolean isBoolean() {
        return returnObj instanceof Boolean;
    }

    /**
     * <p>Returns true if and only if return type of byte[]</p>
     * @return
     */
    public boolean isByteArraySingleDimension() {
        return returnObj instanceof byte[];
    }

    /**
     * <p>Returns true if and only if return type of byte[][]</p>
     * @return
     */
    public boolean isByteArrayDoubleDimension() {
        return returnObj instanceof byte[][];
    }

    /**
     * <p>Returns true if and only if return type of byte[][][]</p>
     * @return
     */
    public boolean isByteArrayTripleDimension() {
        return returnObj instanceof byte[][][];
    }

    /**
     * <p>Returns the returned value from method. Note you will have to type case this value.</p>
     * @return
     */
    public Object getReturnValueObject() {
        return returnObj;
    }

    /**
     * 
     * @return
     */
    private byte getFlag() {
        if (isVoid()) {
            return FLAG_VOID;
        } else if (isBoolean()) {
            return FLAG_BOOLEAN;
        } else if (isByteArraySingleDimension()) {
            return FLAG_BYTE_1D;
        } else if (isByteArrayDoubleDimension()) {
            return FLAG_BYTE_2D;
        } else if (isByteArrayTripleDimension()) {
            return FLAG_BYTE_3D;
        } else {
            throw new AssertionFailedException("Do not recognize the return object type: " + String.valueOf(returnObj == null ? "null" : returnObj.getClass().getSimpleName()));
        }
    }

    public void serialize(OutputStream out) throws IOException {
        RemoteUtil.writeInt(version, out);
        if (version == VERSION_ONE) {
            serializeVersionOne(out);
        }
    }

    public void serializeVersionOne(OutputStream out) throws IOException {
        // set of exceptions
        RemoteUtil.writeInt(propagationExceptionWrapperSet.size(), out);
        for (PropagationExceptionWrapper wrapper : propagationExceptionWrapperSet) {
            wrapper.serialize(out);
        }

        // return type flag
        byte flag = getFlag();
        RemoteUtil.writeByte(flag, out);

        // return object. How written depends on the flag
        switch (flag) {
            case FLAG_VOID:
                break;
            case FLAG_BOOLEAN:
                RemoteUtil.writeBoolean((Boolean) returnObj, out);
                break;
            case FLAG_BYTE_1D:
                RemoteUtil.writeData((byte[]) returnObj, out);
                break;
            case FLAG_BYTE_2D:
                RemoteUtil.write2dData((byte[][]) returnObj, out);
                break;
            case FLAG_BYTE_3D:
                RemoteUtil.write3dData((byte[][][]) returnObj, out);
                break;
            default:
                throw new AssertionFailedException("Unrecognized flag: " + flag);
        }
    }

    /**
     * <p>Useful for troubleshooting.</p>
     * @return
     */
    @Override()
    public String toString() {
        if (isVoid()) {
            return "PropagationReturnWrapper: void, exceptions: " + this.propagationExceptionWrapperSet.size();
        } else if (isBoolean()) {
            return "PropagationReturnWrapper: boolean <" + (Boolean) this.returnObj + ">, exceptions: " + this.propagationExceptionWrapperSet.size();
        } else if (isByteArraySingleDimension()) {
            int len1 = ((byte[]) this.returnObj).length;
            return "PropagationReturnWrapper: byte[" + len1 + "], exceptions: " + this.propagationExceptionWrapperSet.size();
        } else if (isByteArrayDoubleDimension()) {
            int len1 = ((byte[][]) this.returnObj).length;
            return "PropagationReturnWrapper: byte[" + len1 + "][], exceptions: " + this.propagationExceptionWrapperSet.size();
        } else if (isByteArrayTripleDimension()) {
            int len1 = ((byte[][][]) this.returnObj).length;
            return "PropagationReturnWrapper: byte[" + len1 + "][][], exceptions: " + this.propagationExceptionWrapperSet.size();
        } else {
            throw new AssertionFailedException("Do not recognize the return object type: " + String.valueOf(returnObj == null ? "null" : returnObj.getClass().getSimpleName()));
        }
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            serialize(baos);
            return baos.toByteArray();
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    /**
     * 
     * @param bytes
     * @return
     * @throws IOException
     */
    public static PropagationReturnWrapper createFromBytes(byte[] bytes) throws IOException {
       ByteArrayInputStream bais = null;
       try {
           bais = new ByteArrayInputStream(bytes);
           return new PropagationReturnWrapper(bais);
       } finally {
           IOUtil.safeClose(bais);
       }
    }
}
