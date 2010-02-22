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
package org.tranche.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import org.tranche.security.Signature;
import org.tranche.exceptions.ChunkAlreadyExistsSecurityException;
import org.tranche.exceptions.ChunkDoesNotBelongException;
import org.tranche.exceptions.ChunkDoesNotMatchHashException;
import org.tranche.exceptions.MetaDataIsCorruptedException;
import org.tranche.exceptions.NoHostProvidedException;
import org.tranche.exceptions.NoMatchingServersException;
import org.tranche.exceptions.PropagationFailedException;
import org.tranche.exceptions.PropagationUnfulfillableHostException;
import org.tranche.exceptions.RejectedRequestException;
import org.tranche.exceptions.ServerIsNotReadableException;
import org.tranche.exceptions.ServerIsNotWritableException;
import org.tranche.exceptions.ServerIsOfflineException;
import org.tranche.exceptions.TodoException;
import org.tranche.exceptions.TrancheProtocolException;
import org.tranche.exceptions.UnexpectedEndOfStreamException;
import org.tranche.exceptions.UnsupportedServerOperationException;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.hash.BigHash;
import org.tranche.network.ConnectionUtil;
import org.tranche.network.NetworkUtil;
import org.tranche.network.StatusTableRow;
import org.tranche.streams.DeleteFileOnExitFileInputStream;
import org.tranche.util.IOUtil;
import org.tranche.security.SecurityUtil;

/**
 * <p>Utility methods for remote tranche server interaction.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class RemoteUtil {

    public static final byte BYTE_FALSE = (byte) 0;
    public static final byte BYTE_TRUE = (byte) 1;

    /**
     * <p>Returns true if input stream doesn't have any available input, else false.</p>
     *
     * @param   is              the input stream
     * @return                  <code>true</code> if the input stream does not have any available input;
     *                          <code>false</code> otherwise
     * @throws  IOException     if an input or output exception occurs 
     *
     */
    public static final boolean isAvailable(InputStream is) throws IOException {
        return is.available() > 0;
    }

    /**
     * 
     * @param booleanByte
     * @return
     * @throws java.io.IOException
     */
    public static final boolean getBoolean(byte booleanByte) throws IOException {
        if (booleanByte == RemoteUtil.BYTE_TRUE) {
            return true;
        } else if (booleanByte == RemoteUtil.BYTE_FALSE) {
            return false;
        }
        throw new IOException("Unrecognized boolean byte value: " + booleanByte);
    }

    /**
     *
     * @param byteBoolean
     * @return
     */
    public static final byte getByte(boolean byteBoolean) {
        if (byteBoolean) {
            return RemoteUtil.BYTE_TRUE;
        } else {
            return RemoteUtil.BYTE_FALSE;
        }
    }

    /**
     *
     * @param booleanArray
     * @param out
     * @throws java.io.IOException
     */
    public static final void writeBooleanArray(boolean[] booleanArray, OutputStream out) throws IOException {
        RemoteUtil.writeInt(booleanArray.length, out);
        for (int i = 0; i < booleanArray.length; i++) {
            RemoteUtil.writeBoolean(booleanArray[i], out);
        }
    }

    /**
     *
     * @param is
     * @return
     * @throws java.io.IOException
     */
    public static final boolean[] readBooleanArray(InputStream is) throws IOException {
        boolean[] booleanArray = new boolean[RemoteUtil.readInt(is)];
        for (int i = 0; i < booleanArray.length; i++) {
            booleanArray[i] = RemoteUtil.readBoolean(is);
        }
        return booleanArray;
    }

    /**
     *
     * @param hashes
     * @param os
     * @throws java.io.IOException
     */
    public static final void writeBigHashArray(BigHash[] hashes, OutputStream os) throws IOException {
        RemoteUtil.writeInt(hashes.length, os);
        for (int i = 0; i < hashes.length; i++) {
            RemoteUtil.writeBigHash(hashes[i], os);
        }
    }

    /**
     *
     * @param is
     * @return
     * @throws java.io.IOException
     */
    public static final BigHash[] readBigHashArray(InputStream is) throws IOException {
        BigHash[] hashes = new BigHash[RemoteUtil.readInt(is)];
        for (int i = 0; i < hashes.length; i++) {
            hashes[i] = readBigHash(is);
        }
        return hashes;
    }

    /**
     * 
     * @param is
     * @return
     * @throws java.io.IOException
     */
    public static final BigHash readBigHash(InputStream is) throws IOException {
        return BigHash.createFromBytes(readBytes(BigHash.HASH_LENGTH, is));
    }

    /**
     *
     * @param hash
     * @param os
     * @throws java.io.IOException
     */
    public static final void writeBigHash(BigHash hash, OutputStream os) throws IOException {
        writeBytes(hash.toByteArray(), os);
    }

    /**
     * <p>Helper method to read a single line of unencoded text</p>
     * @param   is              the input stream
     * @return                  the string equivalent of the byte array output stream                   
     * @throws  IOException     if an input or output exception occurs 
     */
    public static final String readLine(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int c = is.read(); c != '\n'; c = is.read()) {
            if (c == -1) {
                break;
            }
            baos.write((char) c);
        }
        // return the string
        String s = new String(baos.toByteArray());
        if (s == null || baos.size() == 0) {
            return null;
        }
        s = s.replace("\\n", "\n");
        s = s.replace("\\\\", "\\");
        return s;
    }

    /**
     * <p>Write a line to an output stream.</p>
     * @param   s               the string 
     * @param   out             the output stream 
     * @throws  IOException     if an input or output exception occurs
     */
    public static final void writeLine(String s, OutputStream out) throws IOException {
        // filter the string
        if (s != null) {
            s = s.replace("\\", "\\\\");
            s = s.replace("\n", "\\n");
            out.write(s.getBytes());
        }
        out.write(Token.EOL);
        out.flush();
    }

    /**
     * <p>Helper method to read data</p>
     * @param   in              the input stream
     * @return                  the byte array input stream containing the buffered data 
     * @throws  IOException     if an input or output exception occurs 
     */
    public static final InputStream bufferData(InputStream in) throws IOException {
        // get the size
        String sizeString = RemoteUtil.readLine(in);
        long size = Integer.parseInt(sizeString);
        // null bytes
        if (size == -1) {
            return null;
        } // if no bytes, handle specially
        else if (size == 0) {
            // finally burn the '\n' at the end of the data
            int endChar = in.read();
            if (endChar != '\n') {
                throw new IOException("Data should have ended after " + size + " bytes!");
            }
            // return the buffered data
            return new ByteArrayInputStream(new byte[0]);
        }

        // if more than 100000, buffer on disk
        long cutoff = DataBlockUtil.ONE_MB;
        if (size <= cutoff) {
            // keep it all in memory
            byte[] buf = new byte[(int) size];
            int maxToRead = buf.length;
            int index = 0;
            for (int bytesRead = in.read(buf, index, maxToRead); index < size; bytesRead = in.read(buf, index, maxToRead)) {

                // Hmmm... if end of stream, bytesRead will be equal to -1. Then the next read would thrown
                // an IndexOutOfBoundsException.
                // 
                // Instead, let's throw a more useful exception
                if (bytesRead < 0) {
                    throw new UnexpectedEndOfStreamException("bytesRead = " + bytesRead + "; index = " + index + "; maxToRead = " + maxToRead + "; buf.length = " + buf.length);
                }

                // inc the index
                index += bytesRead;
                // trim bytes to read appropriately
                if (size - index < maxToRead) {
                    maxToRead = (int) size - index;
                }
            }
            // finally burn the '\n' at the end of the data
            char endChar = (char) in.read();
            if (endChar != '\n') {
                throw new UnexpectedTokenException(Character.toString(endChar), "Expected \\n. Data should have ended after " + size + " bytes!");
            }
            // return the buffered data
            return new ByteArrayInputStream(buf);
        } // else save to disk
        else {
            // make a new file
            File temp = File.createTempFile("temp", "data");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(temp);
                // get all the data
                byte[] buf = new byte[(int) cutoff];
                int maxToRead = buf.length;
                int index = 0;
                for (int bytesRead = in.read(buf, 0, maxToRead); index < size; bytesRead = in.read(buf, 0, maxToRead)) {
                    // inc the index
                    index += bytesRead;
                    // trim bytes to read appropriately
                    if (size - index < maxToRead) {
                        maxToRead = (int) size - index;
                    }
                    // write the bytes
                    fos.write(buf, 0, bytesRead);
                }
            } finally {
                IOUtil.safeClose(fos);
            }
            // finally burn the '\n' at the end of the data
            char endChar = (char) in.read();
            if (endChar != Token.EOL) {
                throw new UnexpectedTokenException(Character.toString(endChar), "Expected \\n. Data should have ended after " + size + " bytes!");
            }
            // return the data
            return new DeleteFileOnExitFileInputStream(temp);
        }
    }

    /**
     * <p>Helper method for handling errors.</p>
     * @param   responseLine    the response consisting of an exception 
     * @param   in              the input stream
     * @throws  IOException     if an input or output exception occurs
     * @throws java.lang.RuntimeException if a server doesn't support an operation or another runtime exception occurs
     */
    public static final void handledError(String responseLine, InputStream in) throws IOException, RuntimeException, GeneralSecurityException {
        // check for nulls
        if (responseLine == null) {
            throw new IOException("Null response from server on thread named " + Thread.currentThread().getName());
        }
        // handle exceptions
        if (responseLine.equals(Token.ERROR_STRING)) {
            // Get the error line. (Note: line might end with newline character (if more content) or -1 end of stream.
            String errorMessage = RemoteUtil.readLine(in);

            // Some specific exceptions should be thrown
            if (errorMessage == null) {
                throw new IOException();
            } else if (errorMessage.equals(ServerIsNotReadableException.MESSAGE)) {
                // read in updated status info and commit as update to current table
                try {
                    NetworkUtil.updateRow(new StatusTableRow(in));
                } catch (Exception e) {
                }
                throw new ServerIsNotReadableException();
            } else if (errorMessage.equals(ServerIsNotWritableException.MESSAGE)) {
                // read in updated status info and commit as update to current table
                try {
                    NetworkUtil.updateRow(new StatusTableRow(in));
                } catch (Exception e) {
                }
                throw new ServerIsNotWritableException();
            } else if (errorMessage.equals(ChunkAlreadyExistsSecurityException.MESSAGE)) {
                throw new ChunkAlreadyExistsSecurityException();
            } else if (errorMessage.equals(RejectedRequestException.MESSAGE)) {
                throw new RejectedRequestException();
            } else if (errorMessage.equals(NoMatchingServersException.MESSAGE)) {
                throw new NoMatchingServersException();
            } else if (errorMessage.equals(UnsupportedServerOperationException.MESSAGE)) {
                throw new UnsupportedServerOperationException();
            } else if (errorMessage.startsWith(ChunkDoesNotMatchHashException.MESSAGE)) {
                throw new ChunkDoesNotMatchHashException(errorMessage);
            } else if (errorMessage.equals(MetaDataIsCorruptedException.MESSAGE)) {
                throw new MetaDataIsCorruptedException();
            } else if (errorMessage.equals(NoHostProvidedException.MESSAGE)) {
                throw new NoHostProvidedException();
            } else if (errorMessage.equals(TrancheProtocolException.MESSAGE)) {
                throw new TrancheProtocolException();
            } else if (errorMessage.startsWith(TodoException.MESSAGE)) {
                throw new TodoException(errorMessage.replace(TodoException.MESSAGE, ""));
            } else if (errorMessage.startsWith(ServerIsOfflineException.MESSAGE_PREFIX)) {
                // Remove host information from error message
                String host = "";
                try {
                    host = errorMessage.substring(ServerIsOfflineException.MESSAGE_PREFIX.length()).trim();
                    ConnectionUtil.flagOffline(host);
                } catch (Exception e) {
                }
                throw new ServerIsOfflineException(host);
            } else if (errorMessage.equals(PropagationFailedException.MESSAGE)) {
                throw new PropagationFailedException();
            } else if (errorMessage.equals(ChunkDoesNotBelongException.MESSAGE)) {
                // read in updated status info and commit as update to current table
                try {
                    NetworkUtil.updateRow(new StatusTableRow(in));
                } catch (Exception e) {
                }
                // Still throw exception so application can handle
                throw new ChunkDoesNotBelongException();
            } else if (errorMessage.equals(PropagationUnfulfillableHostException.MESSAGE)) {
                throw new PropagationUnfulfillableHostException();
            }

            // throw the response as an exception
            throw new IOException(errorMessage);
        }
    }

    /**
     * <p>Helper method for reading data.</p>
     * @param   is              the input stream
     * @return                  the input stream of buffer data
     * @throws  IOException     if an input or output exception occurs
     */
    public static final InputStream readData(InputStream is) throws IOException {
        try {
            // get the response line
            String response = RemoteUtil.readLine(is);
            // handle any errors
            RemoteUtil.handledError(response, is);
            // check that it is data
            if (!response.equals(Token.DATA_STRING)) {
                throw new UnexpectedTokenException(response, Token.DATA_STRING);
            }

            return RemoteUtil.bufferData(is);
        } catch (GeneralSecurityException gse) {
            throw new IOException(gse.getMessage());
        }

    }

    /**
     * <p>Helper method to return all bytes from an input stream.</p>
     * @param   bis             the input stream
     * @return                  the data bytes
     * @throws  IOException     if an input or output exception occurs
     */
    public static final byte[] readDataBytes(InputStream bis) throws IOException {
        // get the original name
        InputStream is = RemoteUtil.readData(bis);
        try {
            if (is == null) {
                return null;
            } else {
                return IOUtil.getBytes(is);
            }

        } finally {
            IOUtil.safeClose(is);
        }

    }

    /**
     * <p>Helper method for writing bytes to an output stream. Formats into two "lines", the first is the length of bytes plus Token.EOL, then the bytes plus Token.EOL.</p>
     * @param   data            the data bytes
     * @param   out             the output stream 
     * @throws  IOException     if an input or output exception occurs
     */
    public static final void writeData(byte[] data, OutputStream out) throws IOException {
        // write out the data line
        out.write(Token.DATA);
        // write out the size
        if (data == null) {
            out.write(Long.toString(-1).getBytes());
        } else {
            out.write(Long.toString(data.length).getBytes());
        }

        out.write(Token.EOL);
        if (data != null) {
            // write out the data
            out.write(data, 0, data.length);
            out.write(Token.EOL);
        }

        out.flush();
    }

    /**
     *
     * @param data
     * @param out
     * @throws IOException
     */
    public static final void write2dData(byte[][] data, OutputStream out) throws IOException {
        out.write(data.length);
        for (int i = 0; i <
                data.length; i++) {
            writeData(data[i], out);
        }

    }

    /**
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static final byte[][] read2dData(InputStream in) throws IOException {
        int length = in.read();
        byte[][] bytes = new byte[length][];
        for (int i = 0; i <
                length; i++) {
            bytes[i] = readDataBytes(in);
        }

        return bytes;
    }

    /**
     *
     * @param data
     * @param out
     * @throws IOException
     */
    public static final void write3dData(byte[][][] data, OutputStream out) throws IOException {
        out.write(data.length);
        for (int i = 0; i <
                data.length; i++) {
            out.write(data[i].length);
            for (int j = 0; j <
                    data[i].length; j++) {
                writeData(data[i][j], out);
            }

        }
    }

    /**
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static final byte[][][] read3dData(InputStream in) throws IOException {
        int length = in.read();
        byte[][][] bytes = new byte[length][][];
        for (int i = 0; i <
                length; i++) {
            int length2 = in.read();
            bytes[i] = new byte[length2][];
            for (int j = 0; j <
                    length2; j++) {
                bytes[i][j] = readDataBytes(in);
            }

        }
        return bytes;
    }

    /**
     *
     * @param bytes
     * @param out
     * @throws java.io.IOException
     */
    public static final void writeBytes(byte[] bytes, OutputStream out) throws IOException {
        out.write(bytes);
        out.flush();
    }

    /**
     *
     * @param length
     * @param in
     * @return
     * @throws java.io.IOException
     */
    public static final byte[] readBytes(int length, InputStream in) throws IOException {
        byte[] bytes = new byte[length];
        in.read(bytes);
        return bytes;
    }

    /**
     *
     * @param b
     * @param out
     * @throws java.io.IOException
     */
    public static final void writeByte(byte b, OutputStream out) throws IOException {
        out.write(b);
        out.flush();
    }

    /**
     *
     * @param in
     * @return
     * @throws java.io.IOException
     */
    public static final byte readByte(InputStream in) throws IOException {
        return (byte) in.read();
    }

    /**
     * <p>Helper method for writing a long to an output stream.</p>
     * @param   l               the long to write
     * @param   out             the output stream
     * @throws  IOException     if an input or output exception occurs
     */
    public static final void writeLong(long l, OutputStream out) throws IOException {
        byte[] bytes = new byte[8];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.putLong(l);
        // write the data
        writeData(bytes, out);
    }

    /**
     * <p>Helper method to read a long from an input stream.</p>
     * @param   is              the input stream
     * @return                  the long read from the byte buffer
     * @throws  IOException     if an input or output exception occurs
     */
    public static final long readLong(InputStream is) throws IOException {
        byte[] bytes = readDataBytes(is);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return bb.getLong();
    }

    /**
     * <p>Helper method for writing an integer to an output stream.</p>
     * @param   i               the integer to write
     * @param   out             the output stream
     * @throws  IOException     if an input or output exception occurs
     */
    public static final void writeInt(int i, OutputStream out) throws IOException {
        byte[] bytes = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.putInt(i);
        // write the data
        writeData(bytes, out);
    }

    /**
     * <p>Helper method to read an integer from an input stream.</p>
     * @param   is              the input stream
     * @return                  the int read from the byte buffer
     * @throws  IOException     if an input or output exception occurs
     */
    public static final int readInt(InputStream is) throws IOException {
        byte[] bytes = readDataBytes(is);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return bb.getInt();
    }

    /**
     * <p>Helper method for writing an integer to an output stream.</p>
     * @param   b               the boolean to write
     * @param   out             the output stream
     * @throws  IOException     if an input or output exception occurs
     */
    public static final void writeBoolean(boolean b, OutputStream out) throws IOException {
        writeByte(getByte(b), out);
    }

    /**
     * <p>Helper method to read an integer from an input stream.</p>
     * @param   is              the input stream
     * @return                  the int read from the byte buffer
     * @throws  IOException     if an input or output exception occurs
     */
    public static final boolean readBoolean(InputStream is) throws IOException {
        return getBoolean(readByte(is));
    }

    /**
     * <p>Helper method to write out an error message to the output stream.</p>
     * @param   message         the message to output as an error
     * @param   out             the output stream
     * @throws  IOException     if an input or output exception occurs
     */
    public static final void writeError(String message, OutputStream out) throws IOException {
        out.write(Token.ERROR);
        // null check
        if (message != null) {

            // Copy to new string so can use unmodified 'message' parameter below (with any relevant hooks)
            String messageCopy = message.replace("\\", "\\\\").replace("\n", "\\n");
            out.write(messageCopy.getBytes());

            // -----------------------------------------------------------------------
            // Server-side hooks for specific types of exceptions go here
            // -----------------------------------------------------------------------

            // If chunk doesn't belong on server, we also need to send updated table information
            if (message.startsWith(ChunkDoesNotBelongException.MESSAGE)) {

                // Get local row and serialize it for client
                StatusTableRow localRow = NetworkUtil.getLocalServerRow();
                ObjectOutputStream oos = new ObjectOutputStream(out);
                localRow.serialize(StatusTableRow.VERSION_LATEST, oos);
            }

        } else {
            out.write("null".getBytes());
        }

        out.write(Token.EOL);
        out.flush();
    }

    /**
     * <p>Helper method to write out a signature to an output stream.</p>
     * @param   sig         the signature
     * @param   bos         the output stream
     * @throws  Exception   if any exception occurs
     */
    public static final void writeSignature(Signature sig, OutputStream bos) throws Exception {
        // send over the algorithm
        RemoteUtil.writeData(sig.getAlgorithm().getBytes(), bos);
        // send over the signer's certificate
        RemoteUtil.writeData(sig.getCert().getEncoded(), bos);
        // send over the signature
        RemoteUtil.writeData(sig.getBytes(), bos);
        bos.flush();
    }

    /**
     * <p>Helper method to read a signature from an input stream.</p>
     * @param   in          the input stream
     * @return              the signature
     * @throws  Exception   if any exception occurs
     */
    public static final Signature readSignature(
            InputStream in) throws Exception {
        // send over the algorithm
        byte[] algorithm = RemoteUtil.readDataBytes(in);
        byte[] cert = RemoteUtil.readDataBytes(in);
        byte[] sig = RemoteUtil.readDataBytes(in);
        return new Signature(sig, new String(algorithm), SecurityUtil.getCertificate(cert));
    }

    /**
     * <p>Helper method that will throw an IOException if the input stream does not start with an Token.OK_STRING line.</p>
     * @param   in              the input stream
     * @throws  IOException     if an input or output exception occurs
     */
    public static final void checkOK(InputStream in) throws IOException, GeneralSecurityException {
        checkOK(RemoteUtil.readLine(in), in);
    }

    /**
     * <p>Helper method that will throw an IOException if the line is not an Token.OK_STRING</p>
     * @param   line            the line checked for errors
     * @param   is              the input stream
     * @throws  IOException     if an input or output exception occurs
     */
    public static final void checkOK(String line, InputStream is) throws IOException, GeneralSecurityException {
        // first check for errors
        RemoteUtil.handledError(line, is);
        // if not an error, check for OK
        if (!line.equals(Token.OK_STRING)) {
            throw new UnexpectedTokenException(line, Token.OK_STRING);
        }
    }
}
